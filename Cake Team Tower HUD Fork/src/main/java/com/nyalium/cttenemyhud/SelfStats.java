package com.nyalium.cttenemyhud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 自身属性面板的数据源：<b>纯客户端</b>，照抄上游 Cake Tower HUD Plus 的做法。
 *
 * <h2>原理</h2>
 * <ol>
 *   <li>客户端定时发地图自带的玩家命令 {@code /trigger ViewStats}（地图自己
 *       {@code enable} 过，任何人可用，不需要 OP）；</li>
 *   <li>地图 {@code cake_team_tower:misc/view_stats.mcfunction} 收到后，用上百条
 *       {@code tellraw @s} 把属性一条一条<b>以 chat 形式</b>发回给本人
 *       （血量、四色心、碎/粉心、🗡力量、🗡攻速、🛡防御、元素护甲…）；</li>
 *   <li>本类在 {@link net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents#ALLOW_GAME}
 *       里把这些行截下来：<b>吞掉不进聊天栏</b>，攒成面板内容；顺手按「颜色 + 心形字形」
 *       把心数解析出来（{@link #heartCounts()}），供后续头顶条复用。</li>
 * </ol>
 *
 * <h2>为什么必须靠这个</h2>
 * <p>别人的心数、敌人绝对血量都不在客户端可见的同步范围里（{@code RedHearts} 这类 dummy
 * objective 不挂在显示槽上就根本不会下发）；而 {@code ViewStats} 是唯一一条
 * 「客户端能主动问、服务端必答」的数据通道，而且<b>只能问到自己</b>——
 * 地图那 147 条 {@code tellraw} 全是 {@code @s}，没有一条发给 {@code @a}。</p>
 */
public final class SelfStats {

    /** 采集窗口：触发后这么久之内到达的 chat 都算本次 ViewStats 的输出。 */
    static final long CAPTURE_WINDOW_MS = 1500L;
    /** 一次爆发最长持续多久（地图一趟 ViewStats 是分多个 tick 发的）。 */
    static final long CAPTURE_HARD_MS = 8000L;
    /** 一次最多攒多少行（地图一趟大约 20~40 行，留点余量）。 */
    private static final int MAX_LINES = 64;

    /** 面板里显示的原始行（含颜色样式，直接交给字体渲染）。 */
    private static final List<Component> LINES = new ArrayList<>();
    /** 心的数量：1 红 2 魂 3 黑 4 蓝 5 碎 6 粉（0 位不用）。 */
    private static final int[] HEARTS = new int[7];

    private static long windowEndMs = 0L;
    private static long hardEndMs = 0L;
    private static long nextTriggerMs = 0L;
    private static boolean notStarted = false;

    private SelfStats() {}

    /** 地图 tellraw 里用的颜色（{@code ChatFormatting} 的 RGB）。 */
    static final int RED = 0xFF5555;
    static final int YELLOW = 0xFFFF55;
    static final int BLACK = 0x000000;
    static final int BLUE = 0x5555FF;
    static final int DARK_RED = 0xAA0000;
    static final int LIGHT_PURPLE = 0xFF55FF;

    // ── 触发 ──

    /** 客户端每 tick 调用：到点就发一次 {@code /trigger ViewStats}。 */
    public static void tick(net.minecraft.client.Minecraft mc) {
        if (mc == null || mc.player == null || mc.getConnection() == null) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        int seconds = cfg.selfStatsSeconds;
        if (seconds <= 0) return;                       // 0 = 关闭自动刷新
        long now = System.currentTimeMillis();
        if (now < nextTriggerMs) return;
        nextTriggerMs = now + seconds * 1000L;
        LINES.clear();
        notStarted = false;
        windowEndMs = now + CAPTURE_WINDOW_MS;
        hardEndMs = now + CAPTURE_HARD_MS;
        try {
            mc.getConnection().sendCommand("trigger ViewStats");
        } catch (Exception ignored) {
            // 断线瞬间发包失败：下一轮再来
        }
    }

    /** 断线 / 换服时清空，避免看到上一服的面板。 */
    public static void reset() {
        LINES.clear();
        java.util.Arrays.fill(HEARTS, 0);
        windowEndMs = 0L;
        hardEndMs = 0L;
        nextTriggerMs = 0L;
        notStarted = false;
    }

    // ── 采集 ──

    /**
     * 在 {@code ClientReceiveMessageEvents.ALLOW_GAME} 里调用。
     *
     * @return true = 这行属于本次 ViewStats 输出，<b>吞掉、不显示在聊天栏</b>
     */
    public static boolean onGameMessage(Component message, boolean overlay, long nowMs) {
        if (overlay || message == null) return false;
        if (nowMs > windowEndMs || nowMs > hardEndMs) return false;
        String flat = message.getString();
        if (flat == null) return false;
        if (isGameNotStarted(flat)) {
            notStarted = true;
            windowEndMs = 0L;      // 本次没数据，提前收工
            return true;
        }
        // 地图的标题/说明行是翻译键（客户端没装地图资源包时会以原文出现），
        // 图标行则可能只有一两个字符 —— 这些都要一起吞掉，否则聊天栏会刷屏。
        boolean statLine = looksLikeStat(flat) || isViewStatsLine(flat);
        boolean glyphOnly = flat.strip().length() <= 2 && !flat.isBlank();
        if (!statLine && !glyphOnly) return false;
        if (LINES.size() < MAX_LINES) LINES.add(message);
        parseHearts(message);
        // 还在发就继续延长窗口（硬上限 CAPTURE_HARD_MS）
        windowEndMs = Math.min(hardEndMs, nowMs + CAPTURE_WINDOW_MS);
        return true;
    }

    // ── 读取（面板 / 后续头顶条用） ──

    /** 面板内容（不可变视图；地图发来时已经是带样式的 Component，直接渲染即可）。 */
    public static List<Component> lines() {
        return List.copyOf(LINES);
    }

    /** 解析出来的心数：索引 1~6 = 红/魂/黑/蓝/碎/粉；全 0 表示没解析到。 */
    public static int[] heartCounts() {
        return HEARTS.clone();
    }

    /** 面板是否该画：有内容、且地图没说「游戏没开始」。 */
    public static boolean visible() {
        if (notStarted || LINES.isEmpty()) return false;
        boolean anyHeart = false;
        for (int i = 1; i < HEARTS.length; i++) {
            if (HEARTS[i] > 0) {
                anyHeart = true;
                break;
            }
        }
        return anyHeart || LINES.size() >= 2;
    }

    // ── 解析（纯函数，selfCheck 直接打） ──

    /** 心形字形：{@code ❤} 或地图字体里用于碎心/粉心的私有区字符。 */
    static boolean isHeartGlyph(char ch) {
        return ch == '\u2764' || (ch >= '\uE000' && ch <= '\uF8FF');
    }

    /**
     * 字形 + 颜色 → 心类。
     *
     * @return 1 红 2 魂 3 黑 4 蓝 5 碎 6 粉，0 = 不是心
     */
    static int heartKindOf(String glyph, int rgb) {
        boolean heart = glyph != null && glyph.indexOf('\u2764') >= 0;
        if (heart) {
            if (rgb == RED) return 1;
            if (rgb == YELLOW) return 2;
            if (rgb == BLACK) return 3;
            if (rgb == BLUE) return 4;
            return 0;
        }
        // 碎心 / 粉心用私有区字形，只能靠颜色认
        if (rgb == DARK_RED) return 5;
        if (rgb == LIGHT_PURPLE) return 6;
        return 0;
    }

    /** 取 {@code flat[idx]} 这个字形前面紧邻的数字（地图格式是「数字+字形」）。 */
    static int amountBefore(String flat, int idx) {
        if (flat == null || idx <= 0) return -1;
        int i = idx - 1;
        while (i >= 0 && flat.charAt(i) == ' ') i--;
        int end = i;
        while (i >= 0 && Character.isDigit(flat.charAt(i))) i--;
        if (i == end) return -1;
        try {
            return Integer.parseInt(flat.substring(i + 1, end + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 这一行像不像 ViewStats 的属性行（数字开头 / 带心形 / 带属性图标）。 */
    static boolean looksLikeStat(String flat) {
        if (flat == null || flat.isBlank()) return false;
        String s = flat.strip();
        if (s.isEmpty()) return false;
        if (Character.isDigit(s.charAt(0))) return true;
        if (s.indexOf('\u2764') >= 0) return true;
        for (String icon : new String[]{"\uD83D\uDDE1", "\uD83D\uDEE1", "\u2603", "\u2600", "\u2620", "\u2693"}) {
            if (s.contains(icon)) return true;
        }
        return false;
    }

    /**
     * 地图 ViewStats 的标题/说明行（{@code Misc_View_Stats_*}）。
     *
     * <p>客户端没装地图资源包时这些翻译键会以原文出现，所以按 key 前缀认；
     * 装了资源包时它们已经被翻译成中文/英文，这里也兜底认几个常见前缀词。</p>
     */
    static boolean isViewStatsLine(String flat) {
        if (flat == null) return false;
        return flat.contains("Misc_View_Stats") || flat.contains("Misc_View_Stat");
    }

    /** 地图在「游戏没开始」时回的那句（中英都要认）。 */
    static boolean isGameNotStarted(String flat) {        if (flat == null) return false;
        String s = flat.toLowerCase(java.util.Locale.ROOT);
        return s.contains("game not started") || flat.contains("游戏未开始") || flat.contains("游戏没开始");
    }

    /** 把一行的所有样式段摊平成「文本 + 每字符颜色」，再按字形扫心数。 */
    static void parseHearts(Component message) {
        StringBuilder sb = new StringBuilder();
        List<Integer> colors = new ArrayList<>();
        message.visit((style, text) -> {
            int rgb = style.getColor() != null ? style.getColor().getValue() : 0xFFFFFF;
            sb.append(text);
            int n = text.length();
            for (int i = 0; i < n; i++) colors.add(rgb);
            return Optional.empty();
        }, Style.EMPTY);
        String flat = sb.toString();
        for (int i = 0; i < flat.length(); i++) {
            char ch = flat.charAt(i);
            if (!isHeartGlyph(ch)) continue;
            int rgb = i < colors.size() ? colors.get(i) : 0xFFFFFF;
            int kind = heartKindOf(String.valueOf(ch), rgb);
            if (kind <= 0) continue;
            int amount = amountBefore(flat, i);
            if (amount >= 0) HEARTS[kind] = amount;
        }
    }
}
