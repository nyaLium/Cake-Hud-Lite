package com.nyalium.ctthudfork;

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
 * 自身属性面板的数据源：<b>纯客户端</b>，做法照上游 Cake-Hud（mod id {@code ctt-health-display}）的
 * {@code StatsData.processMessage}：标题行开一趟、逐行收、计时行收尾，只有自己发的命令才吞聊天。
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

    /** 一趟最快什么时候可以收尾（等计时行；等不到就按这个超时收）。 */
    static final long CAPTURE_TIMEOUT_MS = 3000L;
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
    private static long hardEndMs = 0L;
    private static long nextTriggerMs = 0L;
    private static boolean notStarted = false;
    /**
     * 这一趟是不是**我们自己**发的 {@code /trigger ViewStats}。
     * 只有自己发的才把输出从聊天栏吞掉；玩家手动触发的照常显示（对齐上游 {@code hideCapture}）。
     */
    private static boolean selfTriggered;
    /** 这一趟开始的时间（毫秒），用于超时收尾。 */
    private static long captureStartMs;

    // ── 1.20.23：游戏时间没走 → 数据是「冻住的」，面板不上屏 ──

    /** 「时间冻住」判定：两次采样至少隔这么久（ms），免得 1 s 刷新时撞到同一秒被误判。 */
    static final long TIME_STALE_MIN_GAP_MS = 1500L;

    /** 上一趟看到的游戏时间原文（{@code Misc_View_Stats_78} 那行的渲染文本）。 */
    private static String lastTimeText;
    /** 这一趟收到的游戏时间原文。 */
    private static String pendingTimeText;
    /** 连续多少趟时间完全没变。 */
    private static int sameTimeRounds;
    /** 上一次采样的墙上时间。 */
    private static long lastTimeSampleMs;
    /** true = 时间冻住，面板先不画。 */
    private static boolean timeFrozen;

    private SelfStats() {}

    /** 面板上的一行：渲染用的原始 Component + 分类。 */
    static final class Line {
        final Component text;
        final SelfStatsKind kind;
        /** 这一行在 ViewStats 聊天里带的 hover 文本（官方属性名/说明），没有则 null。 */
        final Component hover;

        Line(Component text, SelfStatsKind kind) {
            this.text = text;
            this.kind = kind;
            this.hover = findHover(text);
        }
    }

    /** 从组件树里找第一个 {@code show_text} hover。 */
    static Component findHover(Component component) {
        if (component == null) return null;
        var hover = component.getStyle().getHoverEvent();
        if (hover instanceof net.minecraft.network.chat.HoverEvent.ShowText showText) {
            return showText.value();
        }
        for (Component sibling : component.getSiblings()) {
            Component found = findHover(sibling);
            if (found != null) return found;
        }
        return null;
    }

    // ── 触发 ──

    /** 客户端每 tick 调用：先把已经收齐的一趟换上屏，再看要不要发下一次。 */
    public static void tick(net.minecraft.client.Minecraft mc) {
        if (mc == null) return;
        long now = System.currentTimeMillis();
        settleIfTimedOut(now);
        if (mc.player == null || mc.getConnection() == null) return;
        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        int seconds = effectiveSeconds(mc);
        if (seconds <= 0) return;                       // ≤0 = 关闭自动刷新
        if (now < nextTriggerMs) return;
        // 1.20.8：大厅/菜单里**不发命令**。地图只在开局后 setdisplay below_name HealthPercent
        // 并 enable ViewStats；没开局时触发只会收到一行「无法触发该计分项」。
        if (!inGame(mc)) {
            // 回到大厅：上一局的数字不该继续挂着。
            if (!SHOWN.isEmpty() || capturing) {
                SHOWN.clear();
                PENDING.clear();
                java.util.Arrays.fill(HEARTS, 0);
                capturing = false;
                selfTriggered = false;
            }
            nextTriggerMs = now + GATE_RECHECK_MS;
            return;
        }
        nextTriggerMs = now + seconds * 1000L;
        // auto = true：这一趟的输出要从聊天栏里吞掉（上游 hideCapture 的同一套思路）。
        beginCapture(now, true);
        ChatTriggerFilter.noteTriggerSent(now);
        try {
            mc.getConnection().sendCommand("trigger ViewStats");
        } catch (Exception ignored) {
            // 断线瞬间发包失败：下一轮再来
        }
    }

    /** 没对上局时的重查间隔（大厅里不会每 tick 查计分板）。 */
    private static final long GATE_RECHECK_MS = 5_000L;

    /** 进入他人服务器（非单人存档）时强制使用的刷新间隔（秒）。 */
    public static final int FORCED_SERVER_SECONDS = 3;

    /**
     * 实际生效的刷新间隔（秒）。
     *
     * <p>1.20.13：在别人开的服务器上强制 {@value #FORCED_SERVER_SECONDS} 秒；
     * <b>只影响运行时，不写回配置</b>（单机存档继续用你设置的值）。</p>
     */
    public static int effectiveSeconds(net.minecraft.client.Minecraft mc) {
        boolean singleplayer = mc == null || mc.getSingleplayerServer() != null;
        return effectiveSeconds(singleplayer, HudConfig.get().selfStatsSeconds);
    }

    /** 纯函数版：单机存档用配置值，服务器强制 3 s；顺带夹到 1~5。 */
    static int effectiveSeconds(boolean singleplayer, int configured) {
        int v = Math.max(1, Math.min(5, configured));
        return singleplayer ? v : FORCED_SERVER_SECONDS;
    }

    /** 当前是否处于「服务器强制 3 秒」状态（设置界面的提示用）。 */
    public static boolean forcedByServer(net.minecraft.client.Minecraft mc) {
        return mc != null && mc.getSingleplayerServer() == null;
    }

    /**
     * 客户端能看到的「这局已经开始了」信号（纯客户端，无需服务端组件）：
     *
     * <ul>
     *   <li>地图在 {@code gamestart.mcfunction} 里 {@code setdisplay below_name HealthPercent}，
     *       开局后自己身上就有这条分数；大厅里没有（objective 甚至还不存在）；</li>
     *   <li>或者本人那条 {@code player_100N} bossbar 已经出现（{@link BossBarTracker#hasPersonalBar}，
     *       自动开队伍条用的也是这个判据）。</li>
     * </ul>
     *
     * <p>两者取或：加入一局时两条信号可能差一两个 tick 到达。</p>
     */
    static boolean inGame(net.minecraft.client.Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) return false;
        if (EnemyData.healthPercentOf(mc.level.getScoreboard(), mc.player) >= 0) return true;
        return BossBarTracker.hasPersonalBar(mc);
    }

    /** 地图回「无法触发该计分项 / Cannot trigger this objective」——触发项还没 enable。 */
    static boolean isTriggerDenied(String flat) {
        if (flat == null) return false;
        String s = flat.toLowerCase(Locale.ROOT);
        return s.contains("cannot trigger this objective")
                || s.contains("can't trigger this objective")
                || flat.contains("无法触发该计分项")
                || flat.contains("无法触发此计分项")
                || flat.contains("尚未启用");
    }

    /**
     * 被拒绝后安静一会儿，避免继续往聊天栏丢提示。
     *
     * <p>1.20.11：60 s → 10 s。拒绝提示本身已被 {@link ChatTriggerFilter} 吞掉，
     * 10 s 一次的重试成本可以忽略；好处是开局后最多等 10 s 面板就能重新出数，
     * 而不是「上一局的数据挂一分钟」。</p>
     */
    private static final long DENIED_BACKOFF_MS = 10_000L;

    /**
     * 开始一趟采集（1.20.24 起对齐上游 {@code StatsData} 的做法）。
     *
     * @param now  当前毫秒
     * @param auto {@code true} = 本模组自己发的 {@code /trigger ViewStats}：这一趟的输出要
     *             <b>从聊天栏吞掉</b>；{@code false} = 玩家自己手动触发的：<b>照常显示</b>，
     *             只是顺便收进面板（上游的 {@code markAutoTriggered()} / {@code hideCapture} 同一回事）
     */
    static void beginCapture(long now, boolean auto) {
        PENDING.clear();
        java.util.Arrays.fill(PENDING_HEARTS, 0);
        capturing = true;
        selfTriggered = auto;
        captureStartMs = now;
        notStarted = false;
        pendingTimeText = null;
        hardEndMs = now + CAPTURE_HARD_MS;
    }

    /** 超时收尾（计时行没等到时靠它兜底）。 */
    private static void settleIfTimedOut(long now) {
        if (!capturing) return;
        if (now - captureStartMs <= CAPTURE_TIMEOUT_MS && now <= hardEndMs) return;
        endCapture(now);
    }

    /**
     * 收尾一趟：把 PENDING 整体换到 SHOWN，并复位「要不要吞」。
     *
     * <p>空结果默认不覆盖旧面板（偶发丢包时不闪）。连续 {@value #MAX_EMPTY_CAPTURES}
     * 趟都拿不到任何行，就当作「当前取不到数据」，清空面板 —— 否则两局之间/旁观时会一直挂着
     * 上一局的时间和心数。</p>
     */
    private static void endCapture(long now) {
        if (!capturing) return;
        capturing = false;
        selfTriggered = false;
        if (PENDING.isEmpty()) {
            pendingTimeText = null;
            if (++emptyCaptures >= MAX_EMPTY_CAPTURES) {
                SHOWN.clear();
                java.util.Arrays.fill(HEARTS, 0);
            }
            return;
        }
        emptyCaptures = 0;
        SHOWN.clear();
        // 去重：同一行文字只保留第一次出现，避免「多重显示」。
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Line line : PENDING) {
            if (seen.add(line.text.getString())) SHOWN.add(line);
        }
        System.arraycopy(PENDING_HEARTS, 0, HEARTS, 0, HEARTS.length);
        updateTimeFrozen(now);
    }

    /**
     * 1.20.23：判断游戏时间有没有在走。
     *
     * <p>地图的计时行（{@code Misc_View_Stats_78}）在没开局 / 结束后是冻住的；这种时候属性面板
     * 还挂着上一批数字会误导，所以直接不画。两次采样至少隔 {@link #TIME_STALE_MIN_GAP_MS}
     * 才算「真的没走」，避免 1 s 刷新时两次落在同一秒被误判。</p>
     */
    private static void updateTimeFrozen(long now) {
        String text = pendingTimeText;
        pendingTimeText = null;
        if (text == null) return;                    // 这趟没发计时行：保持上一次判断
        long gap = now - lastTimeSampleMs;
        sameTimeRounds = nextSameTimeRounds(lastTimeText, text, gap, sameTimeRounds);
        timeFrozen = sameTimeRounds > 0;
        lastTimeText = text;
        lastTimeSampleMs = now;
    }

    /**
     * 纯函数版「时间冻住」状态机（selfCheck 直接打）：返回新的「连续没变」计数。
     *
     * <p>同一段文本、且两次采样隔够 {@link #TIME_STALE_MIN_GAP_MS} 才算没走；
     * 文本变了立刻归零。</p>
     */
    static int nextSameTimeRounds(String previous, String current, long gapMs, int rounds) {
        if (current == null) return rounds;
        if (!current.equals(previous)) return 0;
        if (gapMs < TIME_STALE_MIN_GAP_MS) return rounds;
        return rounds + 1;
    }

    /** 面板里是否已有文字完全相同的行。 */
    private static boolean containsSameText(java.util.List<Line> lines, Component message) {
        String text = message.getString();
        for (Line line : lines) {
            if (line.text.getString().equals(text)) return true;
        }
        return false;
    }

    /** 连续多少趟空采集就认为「现在没有数据」，清空面板。 */
    private static final int MAX_EMPTY_CAPTURES = 2;

    private static int emptyCaptures;

    /** 断线 / 换服时清空，避免看到上一服的面板。 */
    public static void reset() {
        SHOWN.clear();
        PENDING.clear();
        java.util.Arrays.fill(HEARTS, 0);
        java.util.Arrays.fill(PENDING_HEARTS, 0);
        capturing = false;
        selfTriggered = false;
        captureStartMs = 0L;
        emptyCaptures = 0;
        hardEndMs = 0L;
        nextTriggerMs = 0L;
        notStarted = false;
        lastTimeText = null;
        pendingTimeText = null;
        sameTimeRounds = 0;
        lastTimeSampleMs = 0L;
        timeFrozen = false;
    }

    // ── 采集 ──

    /**
     * 在 {@code ClientReceiveMessageEvents.ALLOW_GAME} 里调用。
     *
     * @return true = 这行属于本次 ViewStats 输出，<b>吞掉、不显示在聊天栏</b>
     */
    public static boolean onGameMessage(Component message, boolean overlay, long nowMs) {
        if (overlay || message == null) return false;
        // 玩家聊天（chat.type.text / chat.type.announcement…）**永远不吞**。
        if (isPlayerChat(message)) return false;
        String flat = message.getString();
        if (flat == null) return false;

        // 采集超时先收尾（等不到计时行时靠它兜底）。
        settleIfTimedOut(nowMs);

        // 「没开局 / 触发项没 enable」这两种回执：清面板、停这一趟。
        if (isGameNotStarted(flat)) {
            boolean hide = selfTriggered;
            notStarted = true;
            capturing = false;
            selfTriggered = false;
            SHOWN.clear();                 // 地图明确说「没开局」：清空，别留上一局的数字
            java.util.Arrays.fill(HEARTS, 0);
            return hide;
        }
        if (isTriggerDenied(flat)) {
            // 触发项还没 enable（对局没开始 / 两局之间 / 这张图不给触发）：
            // 停掉这一趟、清空面板；自己是自动触发时顺手把这行提示吞掉。
            boolean hide = selfTriggered;
            notStarted = true;
            capturing = false;
            selfTriggered = false;
            SHOWN.clear();
            PENDING.clear();
            java.util.Arrays.fill(HEARTS, 0);
            nextTriggerMs = nowMs + DENIED_BACKOFF_MS;
            return hide;
        }

        // 「已触发 ViewStats」的确认行：只有自动触发时吞。
        if (isTriggerAck(flat)) return selfTriggered;

        // 标题行（Your Stats / 统计信息 / Misc_View_Stats_1）：
        // 不在采集里就说明是**玩家手动**触发的 —— 开一趟，但这一趟不吞。
        if (isViewStatsTitle(message)) {
            if (!capturing) beginCapture(nowMs, false);
            return selfTriggered;
        }
        if (!capturing) return false;

        // 提示行（Hover over / 停留…）：不开面板，但算这一趟的输出。
        if (isHintLine(flat)) return selfTriggered;

        Analyzed a = analyze(message);

        // 计时行 = 这一趟结束（上游同样用它当收尾信号），顺手记下来判「时间冻住」。
        // 1.20.31：这一行本身也上面板（以前只当收尾信号、白扔掉，所以面板上从来见不到游戏时间）。
        if (a.keys.contains(TIME_KEY) || isGameTimeLine(flat)) {
            boolean hide = selfTriggered;
            pendingTimeText = flat;
            if (panelWorthy(a, SelfStatsKind.TIMER) && PENDING.size() < MAX_LINES
                    && !containsSameText(PENDING, message)) {
                PENDING.add(new Line(message, SelfStatsKind.TIMER));
            }
            endCapture(nowMs);
            return hide;
        }

        SelfStatsKind kind = classify(a);
        // 1.20.31：草方块那条（一行绿色 □，没有键也没有数字）也属于本模组的内容，
        // 以前它连「像不像属性行」这一关都过不了，被整行丢掉。
        boolean ours = isViewStatsLine(flat) || a.hasKey() || a.unresolvedKey
                || statGlyphLine(flat) || kind == SelfStatsKind.GRASS;
        if (!ours) return false;

        // 提示性文本（标题 / 悬停说明）不上面板；缺地图语言文件时整行原文也不上屏。
        if (panelWorthy(a, kind) && PENDING.size() < MAX_LINES && !containsSameText(PENDING, message)) {
            PENDING.add(new Line(message, kind));
        }
        addHearts(a, PENDING_HEARTS);
        return selfTriggered;
    }

    // ── 读取（面板用） ──

    /**
     * 这一行该不该上屏。
     *
     * <p>提示性文本（{@link SelfStatsKind#HINT}）永远不画；客户端没装地图资源包时整行是未解析的
     * 翻译键（例如 {@code Misc_View_Stats_7800:00:19}），也不画。</p>
     */
    static boolean panelWorthy(Analyzed a, SelfStatsKind kind) {
        return kind != SelfStatsKind.HINT && !(a != null && a.unresolvedKey);
    }

    /** 面板内容（已按配置过滤掉隐藏类；地图发来时就是带样式的 Component，直接渲染）。 */
    public static List<Component> lines() {
        Set<String> hidden = HudConfig.hiddenKinds();
        List<Component> out = new ArrayList<>(SHOWN.size());
        for (Line line : SHOWN) {
            if (hidden.contains(line.kind.id)) continue;
            out.add(line.text);
        }
        return out;
    }

    /** 过滤后第 {@code index} 行的官方 hover（没有返回 null）。 */
    static Component hoverAt(int index) {
        Set<String> hidden = HudConfig.hiddenKinds();
        int i = 0;
        for (Line line : SHOWN) {
            if (hidden.contains(line.kind.id)) continue;
            if (i == index) return line.hover;
            i++;
        }
        return null;
    }

    /** 不受过滤影响的完整行数（自检 / 调试用）。 */
    static int storedLineCount() {
        return SHOWN.size();
    }

    /** 解析出来的心数：索引 1~6 = 红/魂/黑/蓝/碎/粉；全 0 表示没解析到。 */
    public static int[] heartCounts() {
        return HEARTS.clone();
    }

    /** 面板是否该画：有内容、地图没说「游戏没开始」、且游戏时间还在走。 */
    public static boolean visible() {
        return !notStarted && !timeFrozen && !SHOWN.isEmpty();
    }

    /** 游戏时间是否被判定为「冻住」（自检 / 调试用）。 */
    static boolean timeFrozen() {
        return timeFrozen;
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

    /**
     * 这一行像不像 ViewStats 的属性行（数字开头 / 带心形 / 带属性图标）。
     *
     * <p><b>仅 SelfCheck 使用</b>：1.20.24 起线上判定换成 {@link #statGlyphLine(String)} 与翻译键前缀，
     * 本方法保留为「宽判据」的测试锚点，改动需同步 {@code SelfCheck}。</p>
     */
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
     * 地图 ViewStats 里「数字 + 图形」那一类行（{@code ["",{score},{text:"🗡",hover}]} 的形态）。
     *
     * <p>1.20.23：把原来「数字开头就算」和「两个字以内就算」两条宽规则换成这条 ——
     * 地图 134 条 tellraw 里，除了带 {@code Misc_View_Stats_*} 翻译键的，剩下的全都是
     * 「数字 + 下面这些图形之一」，既不会漏，也不会把玩家的「123」「ok」当成属性行。</p>
     */
    static boolean statGlyphLine(String flat) {
        if (flat == null || flat.isEmpty()) return false;
        boolean digit = false;
        boolean glyph = false;
        for (int i = 0; i < flat.length(); ) {
            int cp = flat.codePointAt(i);
            i += Character.charCount(cp);
            if (cp >= '0' && cp <= '9') digit = true;
            else if (isStatGlyphCp(cp)) glyph = true;
            if (digit && glyph) return true;
        }
        return false;
    }

    /**
     * 「本轮还没碰过草方块」那条（地图 {@code view_stats_83}）：整行就是一个绿色的 {@code □}。
     *
     * <p>制表符以外的空白一律先去掉再比，避免地图哪天多一个空格就认不出来。</p>
     */
    static boolean isGrassLine(String flat) {
        return flat != null && flat.strip().equals("\u25A1");
    }

    /** 地图属性行里用到的图形码位（含私有区字形）。 */
    static boolean isStatGlyphCp(int cp) {
        if (cp == 0x2764) return true;                    // ❤
        if (cp >= 0xE000 && cp <= 0xF8FF) return true;    // 地图私有区字形
        for (int icon : STAT_ICONS) {
            if (icon == cp) return true;
        }
        return false;
    }

    /** 🛡 🗡 🏹 ⚡ ✈ 🔥 ☠ ✏ ⚓ ☃ ☀ ⬇ ⬆ ⇑ ⇓（从 view_stats.mcfunction 里抽出来的全部图形）。 */
    private static final int[] STAT_ICONS = {
            0x1F6E1, 0x1F5E1, 0x1F3F9, 0x26A1, 0x2708, 0x1F525, 0x2620, 0x270F,
            0x2693, 0x2603, 0x2600, 0x2B07, 0x2B06, 0x21D1, 0x21D3,
    };

    /** 地图回「已触发 ViewStats / Triggered ViewStats」的确认行。 */
    static boolean isTriggerAck(String flat) {
        if (flat == null) return false;
        return (flat.contains("已触发") || flat.contains("Triggered")) && flat.contains("ViewStats");
    }

    /** ViewStats 里的提示行（原版英文 / 地图中文两套都认）。 */
    static boolean isHintLine(String flat) {
        if (flat == null) return false;
        return flat.contains("Hover over") || flat.contains("停留") || flat.contains("查看属性");
    }

    /** 计时行（没装地图资源包时只能按「游戏时间 / Game time」这两个词认）。 */
    static boolean isGameTimeLine(String flat) {
        if (flat == null) return false;
        return flat.contains("游戏时间") || flat.contains("Game time");
    }

    /** 玩家聊天：原版一定用 {@code chat.*} 这个可翻译键，据此一票否决。 */
    static boolean isPlayerChat(Component message) {
        if (message == null) return false;
        if (message.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            return key != null && key.startsWith("chat.");
        }
        return false;
    }

    /** ViewStats 的标题行（{@code Misc_View_Stats_1}）：玩家手动触发时靠它开一趟采集。 */
    static boolean isViewStatsTitle(Component message) {
        if (message == null) return false;
        if (message.getContents() instanceof TranslatableContents tc) {
            return (KEY_PREFIX + "1").equals(tc.getKey());
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
            if (TIME_KEY.equals(key)) return SelfStatsKind.TIMER;
        }
        for (String key : a.keys) {
            SelfStatsKind kind = KEY_KINDS.get(key);
            if (kind != null) return kind;
        }
        return glyphKind(a.text(), a.colors);
    }

    /**
     * 没有翻译键时的兜底：按「图标 + 颜色」认到具体属性。
     *
     * <p>地图给每个属性配了固定的图标和颜色（🗡 金 = 职业属性、🗡 红 = 力量、🗡 蓝 = 攻速…），
     * 所以数值行即使没有键也能分开。灰色（负值）的颜色丢了身份，落到组内默认项。</p>
     */
    static SelfStatsKind glyphKind(String flat, List<Integer> colors) {
        if (flat == null || flat.isEmpty()) return SelfStatsKind.MISC;

        // 草方块那条：地图就发一个绿色的 □（view_stats_83），没有数字也没有翻译键。
        if (isGrassLine(flat)) return SelfStatsKind.GRASS;

        // 「数字❤%」：地图里只有治疗百分比用这个组合（红 = HealPercent，亮紫 = ExtraHealing）
        int percent = flat.indexOf("\u2764%");
        if (percent >= 0) {
            return colorAt(colors, percent) == LIGHT_PURPLE
                    ? SelfStatsKind.HEAL_EXTRA : SelfStatsKind.HEAL_PERCENT;
        }

        // 元素护甲：🛡 + 元素图标（颜色再分「真伤护甲」和普通护甲）
        int shield = flat.indexOf("\uD83D\uDEE1");
        if (shield >= 0) {
            int after = shield + 2;
            int rgb = colorAt(colors, shield);
            if (flat.startsWith("\uD83D\uDD25", after)) {          // 🔥 火
                return rgb == 0xFFFFFF ? SelfStatsKind.TRUE_FIRE_ARMOR : SelfStatsKind.FIRE_ARMOR;
            }
            if (flat.startsWith("\u2693", after)) return SelfStatsKind.WATER_ARMOR;      // ⚓
            if (flat.startsWith("\u26A1", after)) return SelfStatsKind.ELECTRIC_ARMOR;   // ⚡
            if (flat.startsWith("\u2603", after)) return SelfStatsKind.ICE_ARMOR;        // ☃
            if (flat.startsWith("\u2620", after)) return SelfStatsKind.DARK_ARMOR;       // ☠
            if (flat.startsWith("\u2600", after)) return SelfStatsKind.LIGHT_ARMOR;      // ☀
            return rgb == 0xFFFFFF ? SelfStatsKind.TRUE_ARMOR : SelfStatsKind.DEFENCE;
        }

        // 🗡 五档（颜色区分），🏹 弓术
        int sword = flat.indexOf("\uD83D\uDDE1");
        if (sword >= 0) {
            int rgb = colorAt(colors, sword);
            if (rgb == 0xFF5555) return SelfStatsKind.STRENGTH;          // 红
            if (rgb == 0x5555FF) return SelfStatsKind.ATTACK_SPEED;      // 蓝
            if (rgb == 0x00AA00) return SelfStatsKind.ATTACK_RANGE;      // 深绿
            if (rgb == 0x55FF55) return SelfStatsKind.STAMINA;           // 绿
            return SelfStatsKind.CLASS_STAT;                             // 金 / 灰
        }
        if (flat.indexOf("\uD83C\uDFF9") >= 0) return SelfStatsKind.ARCHERY;   // 🏹

        // ✈：最高速度 / 跳跃 / 二段跳
        int plane = flat.indexOf("\u2708");
        if (plane >= 0) {
            int rgb = colorAt(colors, plane);
            if (rgb == 0x55FF55) return SelfStatsKind.JUMP;              // 绿
            if (rgb == 0xFF87FF) return SelfStatsKind.DOUBLE_JUMP;       // 粉
            return SelfStatsKind.MAX_SPEED;                              // 青 / 灰
        }
        // ✏：法力强度 / 回蓝速度
        int pencil = flat.indexOf("\u270F");
        if (pencil >= 0) {
            return colorAt(colors, pencil) == 0x55FFFF
                    ? SelfStatsKind.MANA_RECHARGE : SelfStatsKind.MANA_POWER;
        }
        // ⚡：主动充能 / 上限 / 速度
        int bolt = flat.indexOf("\u26A1");
        if (bolt >= 0) {
            int rgb = colorAt(colors, bolt);
            if (rgb == 0xFFAA00) return SelfStatsKind.MAX_CHARGE;        // 金
            if (rgb == 0xFFFFFF) return SelfStatsKind.ACTIVE_SPEED;      // 白
            return SelfStatsKind.ACTIVE_CHARGE;                          // 黄 / 灰
        }
        // ☠：召唤 / 骷髅数
        int skull = flat.indexOf("\u2620");
        if (skull >= 0) {
            return colorAt(colors, skull) == 0xFF5555 ? SelfStatsKind.SKULLS : SelfStatsKind.SUMMONING;
        }
        if (flat.indexOf("\u2B07") >= 0 || flat.indexOf("\u2B06") >= 0) return SelfStatsKind.SIZE;      // ⬇⬆
        if (flat.indexOf("\u21D1") >= 0 || flat.indexOf("\u21D3") >= 0) return SelfStatsKind.GRAVITY;   // ⇑⇓

        // 心形（含碎心/粉心用的私有区字形）
        for (int i = 0; i < flat.length(); i++) {
            char ch = flat.charAt(i);
            if (!isHeartGlyph(ch)) continue;
            int rgb = colorAt(colors, i);
            if (ch == '\u2764') {
                if (rgb == RED) return SelfStatsKind.HEART_RED;
                if (rgb == YELLOW) return SelfStatsKind.HEART_SOUL;
                if (rgb == BLACK) return SelfStatsKind.HEART_BLACK;
                if (rgb == BLUE) return SelfStatsKind.HEART_BLUE;
                if (rgb == LIGHT_PURPLE) return SelfStatsKind.HEAL_RAW;   // 治疗图标也是 ❤
                if (rgb == DARK_RED) return SelfStatsKind.BITCHES;
                return SelfStatsKind.HP_NEG;                              // 灰 = 负生命
            }
            // 地图自定义字体里的私有区图标：字符本身就带身份（见 view_stats.mcfunction）
            switch (ch) {
                case '\uE026':                       // 碎心 / 粉心：同一个字形，颜色区分
                    if (rgb == DARK_RED) return SelfStatsKind.HEART_CRACKED;
                    if (rgb == LIGHT_PURPLE) return SelfStatsKind.HEART_PINK;
                    return SelfStatsKind.HEART_CRACKED;
                case '\uE004':                       // 塔内回血
                    return SelfStatsKind.TOWERS_REGEN;
                case '\uE003':                       // 钓鱼
                    return SelfStatsKind.FISHING;
                case '\uE001':                       // 蜜蜂点 / 蚂蚁点：同一个字形，颜色区分
                    if (rgb == 0x000000) return SelfStatsKind.ANT;
                    return SelfStatsKind.BEE;
                case '\uE010':                       // 光明因果 / 中立因果：同一个字形，颜色区分
                    if (rgb == 0xAA00AA) return SelfStatsKind.KARMA_NEUTRAL;
                    return SelfStatsKind.KARMA_LIGHT;
                case '\uE011':                       // 黑暗因果
                    return SelfStatsKind.KARMA_DARK;
                case '\uE027':                       // 天界因果
                    return SelfStatsKind.KARMA_CELESTIAL;
                default:
                    break;
            }
        }
        return SelfStatsKind.MISC;
    }

    /** 第 {@code index} 个字符的颜色（越界/没有颜色时按白色）。 */
    private static int colorAt(List<Integer> colors, int index) {
        if (colors == null || index < 0 || index >= colors.size()) return 0xFFFFFF;
        return colors.get(index);
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

    /** 一行里能解析出的心数（纯函数，<b>仅 selfCheck 使用</b>）。 */
    static int[] heartsOf(Component message) {
        int[] out = new int[7];
        addHearts(analyze(message), out);
        return out;
    }

    // ── 地图翻译键表 ──

    private static java.util.Map<String, SelfStatsKind> buildKeyKinds() {
        java.util.Map<String, SelfStatsKind> m = new java.util.HashMap<>();
        // 状态效果：Misc_View_Stats_49 … 76（纯文本，一行一个；1.20.31 起一条一项）
        for (int i = 49; i <= 76; i++) {
            SelfStatsKind kind = SelfStatsKind.status(i);
            if (kind != null) m.put(KEY_PREFIX + i, kind);
        }
        // 倍率 / 基础值那一行都带地图翻译键，键和属性一一对应
        m.put(KEY_PREFIX + "4", SelfStatsKind.HP_AMP);
        m.put(KEY_PREFIX + "5", SelfStatsKind.HP_RAW);
        m.put(KEY_PREFIX + "8", SelfStatsKind.CLASS_STAT_AMP);
        m.put(KEY_PREFIX + "9", SelfStatsKind.CLASS_STAT_AMP);
        m.put(KEY_PREFIX + "10", SelfStatsKind.STRENGTH_AMP);
        m.put(KEY_PREFIX + "11", SelfStatsKind.STRENGTH_AMP);
        m.put(KEY_PREFIX + "12", SelfStatsKind.ATTACK_SPEED_AMP);
        m.put(KEY_PREFIX + "13", SelfStatsKind.ATTACK_SPEED_AMP);
        m.put(KEY_PREFIX + "16", SelfStatsKind.DEFENCE_AMP);
        m.put(KEY_PREFIX + "17", SelfStatsKind.DEFENCE_AMP);
        m.put(KEY_PREFIX + "18", SelfStatsKind.TRUE_ARMOR_AMP);
        m.put(KEY_PREFIX + "19", SelfStatsKind.TRUE_ARMOR_AMP);
        m.put(KEY_PREFIX + "20", SelfStatsKind.HEAL_AMP);
        m.put(KEY_PREFIX + "21", SelfStatsKind.HEAL_AMP);
        m.put(KEY_PREFIX + "25", SelfStatsKind.FIRE_ARMOR_AMP);
        m.put(KEY_PREFIX + "26", SelfStatsKind.FIRE_ARMOR_AMP);
        m.put(KEY_PREFIX + "33", SelfStatsKind.SPEED_AMP);
        m.put(KEY_PREFIX + "34", SelfStatsKind.SPEED_AMP);
        m.put(KEY_PREFIX + "40", SelfStatsKind.MANA_AMP);
        m.put(KEY_PREFIX + "41", SelfStatsKind.MANA_AMP);
        return java.util.Map.copyOf(m);
    }
}
