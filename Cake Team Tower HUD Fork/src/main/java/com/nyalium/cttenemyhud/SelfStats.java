package com.nyalium.cttenemyhud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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
 *       把心数解析出来（{@link #heartCounts()}）。</li>
 * </ol>
 *
 * <h2>1.20.4：分类 + 双缓冲 + 过滤</h2>
 * <ul>
 *   <li><b>分类</b>：每行按地图翻译键 / 心形字形归到 {@link SelfStatsKind}，
 *       配置里可以让某一类不上屏（例如只看红心、不看🗡攻速）；</li>
 *   <li><b>提示文本不上面板</b>：{@code Misc_View_Stats_1/2}（标题、「悬停图标查看说明」）
 *       属于提示性文本，直接丢掉；客户端没装地图资源包时，含<b>未解析翻译键</b>的行也整行丢掉
 *       —— 否则面板上会出现 {@code Misc_View_Stats_7800:00:19} 这种原文；</li>
 *   <li><b>双缓冲</b>：新一轮数据先收在 {@code PENDING}，等这一趟发完才整体替换上屏内容，
 *       所以刷新时面板不会先清空再慢慢长出来（之前那一下「闪烁」就是这么来的）。</li>
 * </ul>
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

    /** 地图 tellraw 里用的颜色（{@code ChatFormatting} 的 RGB）。 */
    static final int RED = 0xFF5555;
    static final int YELLOW = 0xFFFF55;
    static final int BLACK = 0x000000;
    static final int BLUE = 0x5555FF;
    static final int DARK_RED = 0xAA0000;
    static final int LIGHT_PURPLE = 0xFF55FF;
    /** 原版 gray（{@link net.minecraft.ChatFormatting#GRAY}），「负生命」那行用它。 */
    static final int GRAY = 0xAAAAAA;

    // ── 地图翻译键（view_stats.mcfunction） ──

    static final String KEY_PREFIX = "Misc_View_Stats_";
    /** 标题 + 「悬停图标查看说明」：提示性文本，永远不上面板。 */
    static final Set<String> HINT_KEYS = Set.of(KEY_PREFIX + "1", KEY_PREFIX + "2");
    /** 游戏计时行：「The Game time is 」+ 时分秒。 */
    static final String TIME_KEY = KEY_PREFIX + "78";
    /** 键 → 分类（键与属性一一对应，见地图 view_stats.mcfunction）。 */
    private static final java.util.Map<String, SelfStatsKind> KEY_KINDS = buildKeyKinds();

    /** 面板里真正上屏的行（上一趟的完整结果）。 */
    private static final List<Line> SHOWN = new ArrayList<>();
    /** 这一趟正在收的行；收齐了才整体换到 {@link #SHOWN}。 */
    private static final List<Line> PENDING = new ArrayList<>();
    /** 心的数量：1 红 2 魂 3 黑 4 蓝 5 碎 6 粉（0 位不用）。 */
    private static final int[] HEARTS = new int[7];
    private static final int[] PENDING_HEARTS = new int[7];

    private static boolean capturing;
    private static long windowEndMs = 0L;
    private static long hardEndMs = 0L;
    private static long nextTriggerMs = 0L;
    private static boolean notStarted = false;

    private SelfStats() {}

    /** 面板上的一行：渲染用的原始 Component + 分类。 */
    static final class Line {
        final Component text;
        final SelfStatsKind kind;

        Line(Component text, SelfStatsKind kind) {
            this.text = text;
            this.kind = kind;
        }
    }

    // ── 触发 ──

    /** 客户端每 tick 调用：先把已经收齐的一趟换上屏，再看要不要发下一次。 */
    public static void tick(net.minecraft.client.Minecraft mc) {
        if (mc == null) return;
        long now = System.currentTimeMillis();
        settle(now);
        if (mc.player == null || mc.getConnection() == null) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        int seconds = cfg.selfStatsSeconds;
        if (seconds <= 0) return;                       // 0 = 关闭自动刷新
        if (now < nextTriggerMs) return;
        nextTriggerMs = now + seconds * 1000L;
        begin(now);
        try {
            mc.getConnection().sendCommand("trigger ViewStats");
        } catch (Exception ignored) {
            // 断线瞬间发包失败：下一轮再来
        }
    }

    /** 开始一趟采集：只清 PENDING，屏上的旧内容继续显示（避免闪一下空白）。 */
    private static void begin(long now) {
        PENDING.clear();
        java.util.Arrays.fill(PENDING_HEARTS, 0);
        capturing = true;
        notStarted = false;
        windowEndMs = now + CAPTURE_WINDOW_MS;
        hardEndMs = now + CAPTURE_HARD_MS;
    }

    /** 这一趟发完了（或超时）：把 PENDING 整体换到 SHOWN。空结果不覆盖旧面板。 */
    private static void settle(long now) {
        if (!capturing) return;
        if (now <= windowEndMs && now <= hardEndMs) return;
        capturing = false;
        if (PENDING.isEmpty()) return;
        SHOWN.clear();
        SHOWN.addAll(PENDING);
        System.arraycopy(PENDING_HEARTS, 0, HEARTS, 0, HEARTS.length);
    }

    /** 断线 / 换服时清空，避免看到上一服的面板。 */
    public static void reset() {
        SHOWN.clear();
        PENDING.clear();
        java.util.Arrays.fill(HEARTS, 0);
        java.util.Arrays.fill(PENDING_HEARTS, 0);
        capturing = false;
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
        if (!capturing || nowMs > windowEndMs || nowMs > hardEndMs) return false;
        String flat = message.getString();
        if (flat == null) return false;
        if (isGameNotStarted(flat)) {
            notStarted = true;
            capturing = false;
            SHOWN.clear();                 // 地图明确说「没开局」：清空，别留上一局的数字
            java.util.Arrays.fill(HEARTS, 0);
            return true;
        }

        Analyzed a = analyze(message);
        boolean glyphOnly = flat.strip().length() <= 2 && !flat.isBlank();
        boolean ours = looksLikeStat(flat) || isViewStatsLine(flat) || glyphOnly
                || a.hasKey() || a.unresolvedKey;
        if (!ours) return false;

        SelfStatsKind kind = classify(a);
        // 提示性文本（标题 / 悬停说明）不上面板；缺地图语言文件时整行原文也不上屏。
        boolean panelWorthy = kind != SelfStatsKind.HINT && !a.unresolvedKey;
        if (panelWorthy && PENDING.size() < MAX_LINES) {
            PENDING.add(new Line(message, kind));
        }
        addHearts(a, PENDING_HEARTS);
        // 还在发就继续延长窗口（硬上限 CAPTURE_HARD_MS）
        windowEndMs = Math.min(hardEndMs, nowMs + CAPTURE_WINDOW_MS);
        return true;
    }

    // ── 读取（面板用） ──

    /** 面板内容（已按配置过滤掉隐藏类；地图发来时就是带样式的 Component，直接渲染）。 */
    public static List<Component> lines() {
        Set<String> hidden = EnemyHudConfig.hiddenKinds();
        List<Component> out = new ArrayList<>(SHOWN.size());
        for (Line line : SHOWN) {
            if (hidden.contains(line.kind.id)) continue;
            out.add(line.text);
        }
        return out;
    }

    /** 不受过滤影响的完整行数（自检 / 调试用）。 */
    static int storedLineCount() {
        return SHOWN.size();
    }

    /** 某一类在上一趟里出现了几行（自检用）。 */
    static int storedLinesOf(SelfStatsKind kind) {
        int n = 0;
        for (Line line : SHOWN) {
            if (line.kind == kind) n++;
        }
        return n;
    }

    /** 解析出来的心数：索引 1~6 = 红/魂/黑/蓝/碎/粉；全 0 表示没解析到。 */
    public static int[] heartCounts() {
        return HEARTS.clone();
    }

    /** 面板是否该画：有内容、且地图没说「游戏没开始」。 */
    public static boolean visible() {
        return !notStarted && !SHOWN.isEmpty();
    }

    // ── 解析（纯函数，selfCheck 直接打） ──

    /** 一行解析出来的原始素材：文本、每字符颜色、翻译键、是否含未解析键。 */
    static final class Analyzed {
        final StringBuilder flat = new StringBuilder();
        final List<Integer> colors = new ArrayList<>();
        final Set<String> keys = new LinkedHashSet<>();
        /** true = 有翻译键没被客户端解析出来（没装地图资源包时会这样）。 */
        boolean unresolvedKey;

        String text() {
            return flat.toString();
        }

        boolean hasKey() {
            for (String key : keys) {
                if (key.startsWith(KEY_PREFIX)) return true;
            }
            return false;
        }
    }

    /** 摊平一段 Component：文本 + 每字符颜色 + 全部翻译键。 */
    static Analyzed analyze(Component message) {
        Analyzed a = new Analyzed();
        if (message == null) return a;
        message.visit((style, text) -> {
            int rgb = style.getColor() != null ? style.getColor().getValue() : 0xFFFFFF;
            a.flat.append(text);
            for (int i = 0; i < text.length(); i++) a.colors.add(rgb);
            return Optional.empty();
        }, Style.EMPTY);
        collectKeys(message, a);
        return a;
    }

    private static void collectKeys(Component component, Analyzed a) {
        if (component.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if (key != null) {
                a.keys.add(key);
                // 没解析出来时，渲染文本就是键本身（带参数时是「键 + 参数」），据此判定。
                String own;
                try {
                    own = component.plainCopy().getString();
                } catch (RuntimeException e) {
                    own = null;
                }
                if (key.startsWith(KEY_PREFIX) && own != null && own.startsWith(key)) {
                    a.unresolvedKey = true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectKeys(sibling, a);
        }
    }

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
    static boolean isGameNotStarted(String flat) {
        if (flat == null) return false;
        String s = flat.toLowerCase(Locale.ROOT);
        return s.contains("game not started") || flat.contains("游戏未开始") || flat.contains("游戏没开始");
    }

    /**
     * 一行的分类（纯函数）。
     *
     * <p>优先级：提示键 &gt; 计时键 &gt; 其余属性键 &gt; 字形/颜色兜底。</p>
     */
    static SelfStatsKind classify(Analyzed a) {
        if (a == null) return SelfStatsKind.MISC;
        for (String key : a.keys) {
            if (HINT_KEYS.contains(key)) return SelfStatsKind.HINT;
        }
        for (String key : a.keys) {
            if (TIME_KEY.equals(key)) return SelfStatsKind.TIME;
        }
        for (String key : a.keys) {
            SelfStatsKind kind = KEY_KINDS.get(key);
            if (kind != null) return kind;
        }
        return glyphKind(a.text(), a.colors);
    }

    /** 没有翻译键时的兜底：先看「❤%」，再按心形颜色，再按 🗡🛡✈✏ 这些图标。 */
    static SelfStatsKind glyphKind(String flat, List<Integer> colors) {
        if (flat == null || flat.isEmpty()) return SelfStatsKind.MISC;
        if (flat.contains("\u2764%")) return SelfStatsKind.HEALTH;
        for (int i = 0; i < flat.length(); i++) {
            char ch = flat.charAt(i);
            if (!isHeartGlyph(ch)) continue;
            int rgb = colors != null && i < colors.size() ? colors.get(i) : 0xFFFFFF;
            int kind = heartKindOf(String.valueOf(ch), rgb);
            switch (kind) {
                case 1: return SelfStatsKind.HEART_RED;
                case 2: return SelfStatsKind.HEART_SOUL;
                case 3: return SelfStatsKind.HEART_BLACK;
                case 4: return SelfStatsKind.HEART_BLUE;
                case 5: return SelfStatsKind.HEART_CRACKED;
                case 6: return SelfStatsKind.HEART_PINK;
                default:
                    // ❤ 但颜色不认识（比如「负生命」的灰心）→ 归到生命
                    if (ch == '\u2764') return SelfStatsKind.HEALTH;
            }
        }
        if (flat.contains("\uD83D\uDEE1")) {                 // 🛡
            if (flat.contains("\uD83D\uDD25")                // 🔥 火
                    || flat.contains("\u2693")               // ⚓ 水
                    || flat.contains("\u26A1")               // ⚡ 电
                    || flat.contains("\u2603")               // ☃ 冰
                    || flat.contains("\u2620")               // ☠ 暗
                    || flat.contains("\u2600")) {            // ☀ 光
                return SelfStatsKind.ELEMENT;
            }
            return SelfStatsKind.DEFENCE;
        }
        if (flat.contains("\uD83D\uDDE1") || flat.contains("\uD83C\uDFF9")) {   // 🗡 / 🏹
            return SelfStatsKind.ATTACK;
        }
        if (flat.contains("\u2708")) return SelfStatsKind.SPEED;                // ✈
        if (flat.contains("\u270F")) return SelfStatsKind.MANA;                 // ✏
        return SelfStatsKind.MISC;
    }

    /** 把一行的所有样式段摊平后扫心数，写进给定数组（索引同 {@link #heartCounts()}）。 */
    static void addHearts(Analyzed a, int[] into) {
        if (a == null || into == null) return;
        String flat = a.text();
        for (int i = 0; i < flat.length(); i++) {
            char ch = flat.charAt(i);
            if (!isHeartGlyph(ch)) continue;
            int rgb = i < a.colors.size() ? a.colors.get(i) : 0xFFFFFF;
            int kind = heartKindOf(String.valueOf(ch), rgb);
            if (kind <= 0) continue;
            int amount = amountBefore(flat, i);
            if (amount >= 0) into[kind] = amount;
        }
    }

    /** 一行里能解析出的心数（纯函数，selfCheck 用）。 */
    static int[] heartsOf(Component message) {
        int[] out = new int[7];
        addHearts(analyze(message), out);
        return out;
    }

    // ── 地图翻译键表 ──

    private static java.util.Map<String, SelfStatsKind> buildKeyKinds() {
        java.util.Map<String, SelfStatsKind> m = new java.util.HashMap<>();
        // 状态效果：Misc_View_Stats_49 … 76（纯文本，一行一个）
        for (int i = 49; i <= 76; i++) {
            m.put(KEY_PREFIX + i, SelfStatsKind.STATUS);
        }
        // 生命：MaxHP 倍率 / 基础 MaxHP、治疗倍率 / 基础治疗
        for (int i : new int[]{4, 5, 20, 21}) {
            m.put(KEY_PREFIX + i, SelfStatsKind.HEALTH);
        }
        // 攻击：职业属性、力量、攻速
        for (int i : new int[]{8, 9, 10, 11, 12, 13}) {
            m.put(KEY_PREFIX + i, SelfStatsKind.ATTACK);
        }
        // 防御：Defence、TrueArmor
        for (int i : new int[]{16, 17, 18, 19}) {
            m.put(KEY_PREFIX + i, SelfStatsKind.DEFENCE);
        }
        // 元素护甲：火
        for (int i : new int[]{25, 26}) {
            m.put(KEY_PREFIX + i, SelfStatsKind.ELEMENT);
        }
        // 速度
        for (int i : new int[]{33, 34}) {
            m.put(KEY_PREFIX + i, SelfStatsKind.SPEED);
        }
        // 法力
        for (int i : new int[]{40, 41}) {
            m.put(KEY_PREFIX + i, SelfStatsKind.MANA);
        }
        return java.util.Map.copyOf(m);
    }
}
