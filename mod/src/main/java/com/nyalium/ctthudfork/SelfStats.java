package com.nyalium.ctthudfork;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * <h2>1.20.39：进存档后先安静 3 s</h2>
 * <p>世界刚载入时（玩家卡在「加载地形」、区块还在生成）就去问 {@code ViewStats}，服务端要隔好几秒
 * 才回；而一趟采集 {@value #CAPTURE_TIMEOUT_MS} ms 就超时收尾了，迟到的这批会被当成
 * <b>玩家手动触发</b>（1.20.24 的约定：手动触发照常显示）→ 整屏刷进聊天栏。
 * 现在 {@code ClientPlayConnectionEvents.JOIN} 记一笔「刚进世界」（{@link #onWorldJoined}），
 * {@value #JOIN_GRACE_MS} ms 内不发 trigger、聊天里的 ViewStats 输出也直接丢掉；
 * 世界跑起来之后再问，一问一答都是毫秒级，问题就不存在了。</p>
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

    /**
     * 1.20.39：进存档 / 进服后的静默时长（毫秒）—— 这之前**不自动发** {@code /trigger ViewStats}。
     *
     * <p>世界刚载入时玩家还卡在「加载地形」、区块还在生成，这时候问，服务端要隔好几秒才回；
     * 而一趟采集 {@value #CAPTURE_TIMEOUT_MS} ms 就按 {@link #settleIfTimedOut} 超时收尾了，
     * 迟到的这批会被当成<b>玩家手动触发</b>（1.20.24 的约定：手动触发照常显示）→ 整屏刷进聊天栏。
     * 等世界真的跑起来再问，一问一答都是毫秒级，这问题就不存在了。</p>
     */
    static final long JOIN_GRACE_MS = 3000L;

    /**
     * 1.20.39：静默窗口的第二个条件 —— 本人实体至少要过这么多 tick（世界真的在跑）。
     *
     * <p>只看「进世界后 3 s」在慢机上可能还不够：加载没完就又是同一批迟到输出。
     * 20 tick ≈ 1 s，实体没在 tick 就说明还在载入。</p>
     */
    static final int JOIN_GRACE_TICKS = 20;

    // ── 颜色常量：定义在 SelfStatsCatalog，这里只保留同名的包内别名（自检/面板继续用） ──

    /** 地图 tellraw 里用的颜色（{@code ChatFormatting} 的 RGB）。 */
    static final int RED = SelfStatsCatalog.RED;
    static final int YELLOW = SelfStatsCatalog.YELLOW;
    static final int BLACK = SelfStatsCatalog.BLACK;
    static final int BLUE = SelfStatsCatalog.BLUE;
    static final int DARK_RED = SelfStatsCatalog.DARK_RED;
    static final int LIGHT_PURPLE = SelfStatsCatalog.LIGHT_PURPLE;
    /** 原版 gray（{@link net.minecraft.ChatFormatting#GRAY}），「负生命」那行用它。 */
    static final int GRAY = SelfStatsCatalog.GRAY;

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
    /**
     * 1.20.39：静默窗口的结束时刻（ms）。这之前不发 trigger，聊天里来的 ViewStats 输出也不显示。
     *
     * <p>由 {@link #onWorldJoined(long)} 置位；{@link #reset()} 清 0。</p>
     */
    private static long quietUntilMs;

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

    /** hover 键 → 分类（SelfStatsCatalog 的转发，供自检）。 */
    static SelfStatsKind hoverKind(String key) {
        return SelfStatsCatalog.hoverKind(key);
    }

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

    /**
     * 1.20.39：进入存档 / 服务器时调用（{@code ClientPlayConnectionEvents.JOIN}）。
     *
     * <p>清掉上一服的残留，并把「第一次自动触发」推到 {@value #JOIN_GRACE_MS} ms 之后 ——
     * 那一下世界还在载入，问了也只会得到一批迟到输出（见 {@link #JOIN_GRACE_MS}）。</p>
     */
    public static void onWorldJoined(long nowMs) {
        reset();
        quietUntilMs = nowMs + JOIN_GRACE_MS;
        nextTriggerMs = Math.max(nextTriggerMs, quietUntilMs);
    }

    /**
     * 纯函数版「还在进世界的静默窗口里」（selfCheck 直接打）。
     *
     * @param nowMs         现在
     * @param quietUntilMs  静默窗口结束时刻
     * @param playerTick    本人实体已过的 tick（&lt;0 表示还不知道/没有实体，按「还在等」处理）
     * @param graceTicks    需要的 tick 数
     * @return true = 先别自动问
     */
    static boolean inJoinGrace(long nowMs, long quietUntilMs, int playerTick, int graceTicks) {
        if (nowMs < quietUntilMs) return true;
        return playerTick < Math.max(0, graceTicks);
    }

    /** 静默窗口里不自动触发（true = 再等等）。 */
    private static boolean joinQuiet(long nowMs, net.minecraft.client.Minecraft mc) {
        int tick = (mc == null || mc.player == null) ? -1 : mc.player.tickCount;
        return inJoinGrace(nowMs, quietUntilMs, tick, JOIN_GRACE_TICKS);
    }

    /** 静默窗口结束时刻（自检 / 调试用）。 */
    static long joinQuietUntilMs() {
        return quietUntilMs;
    }

    /** 下一次自动触发的时刻（自检 / 调试用）。 */
    static long nextTriggerMs() {
        return nextTriggerMs;
    }

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
        // 1.20.39：刚进存档/进服先安静一会儿（世界还在载入时问了只会收到一批迟到输出）。
        if (joinQuiet(now, mc)) return;
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
        quietUntilMs = 0L;
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

        // 1.20.39：还在进世界的静默窗口里 —— 我们自己还没问过，这一批只可能是
        // 「上一趟迟到的」或「地图补发的」，一律不进聊天栏，也不收进面板。
        if (nowMs <= quietUntilMs && (isTriggerAck(flat) || isViewStatsTitle(message)
                || looksLikeViewStatsPayload(flat, analyze(message)))) {
            return true;
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

        SelfStatsCatalog.Analyzed a = analyze(message);

        // 计时行 = 这一趟结束（上游同样用它当收尾信号），顺手记下来判「时间冻住」。
        // 1.20.31：这一行本身也上面板（以前只当收尾信号、白扔掉，所以面板上从来见不到游戏时间）。
        if (a.keys.contains(SelfStatsCatalog.TIME_KEY) || isGameTimeLine(flat)) {
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
    static boolean panelWorthy(SelfStatsCatalog.Analyzed a, SelfStatsKind kind) {
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

    // ── 解析（纯函数，selfCheck 直接打；实现在 SelfStatsCatalog，这里只转发） ──

    /** 摊平一段 Component：文本 + 每字符颜色 + 全部翻译键。 */
    static SelfStatsCatalog.Analyzed analyze(Component message) {
        return SelfStatsCatalog.analyze(message);
    }

    /** 心形字形：{@code ❤} 或地图字体里用于碎心/粉心的私有区字符。 */
    static boolean isHeartGlyph(char ch) {
        return SelfStatsCatalog.isHeartGlyph(ch);
    }

    /**
     * 字形 + 颜色 → 心类。
     *
     * @return 1 红 2 魂 3 黑 4 蓝 5 碎 6 粉，0 = 不是心
     */
    static int heartKindOf(String glyph, int rgb) {
        return SelfStatsCatalog.heartKindOf(glyph, rgb);
    }

    /** 取 {@code flat[idx]} 这个字形前面紧邻的数字（地图格式是「数字+字形」）。 */
    static int amountBefore(String flat, int idx) {
        return SelfStatsCatalog.amountBefore(flat, idx);
    }

    /**
     * 地图 ViewStats 里「数字 + 图形」那一类行（{@code ["",{score},{text:"🗡",hover}]} 的形态）。
     */
    static boolean statGlyphLine(String flat) {
        return SelfStatsCatalog.statGlyphLine(flat);
    }

    /**
     * 「本轮还没碰过草方块」那条（地图 {@code view_stats_83}）：整行就是一个绿色的 {@code □}。
     */
    static boolean isGrassLine(String flat) {
        return SelfStatsCatalog.isGrassLine(flat);
    }

    /** 🛡 🗡 🏹 ⚡ ✈ 🔥 ☠ ✏ ⚓ ☃ ☀ ⬇ ⬆ ⇑ ⇓（从 view_stats.mcfunction 里抽出来的全部图形）。 */
    static final int[] STAT_ICONS = SelfStatsCatalog.STAT_ICONS;

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
            return (SelfStatsCatalog.KEY_PREFIX + "1").equals(tc.getKey());
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

    /**
     * 这一行像不像 ViewStats 的输出（1.20.39：静默窗口里判「这是不是那批属性行」用）。
     *
     * <p>比 {@link #isViewStatsLine(String)} 宽：装了资源包时翻译键已经变成中文/英文，
     * 所以「提示行 / 计时行 / 未解析键 / 数字 + 图形 / 草方块」都算。</p>
     */
    static boolean looksLikeViewStatsPayload(String flat, SelfStatsCatalog.Analyzed a) {
        if (flat == null) return false;
        if (isViewStatsLine(flat)) return true;
        if (isHintLine(flat) || isGameTimeLine(flat)) return true;
        if (a != null && (a.hasKey() || a.unresolvedKey)) return true;
        return statGlyphLine(flat) || isGrassLine(flat);
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
    static SelfStatsKind classify(SelfStatsCatalog.Analyzed a) {
        return SelfStatsCatalog.classify(a);
    }

    /**
     * 没有翻译键时的兜底：按「图标 + 颜色」认到具体属性。
     *
     * <p>地图给每个属性配了固定的图标和颜色（🗡 金 = 职业属性、🗡 红 = 力量、🗡 蓝 = 攻速…），
     * 所以数值行即使没有键也能分开。灰色（负值）的颜色丢了身份，落到组内默认项。</p>
     */
    static SelfStatsKind glyphKind(String flat, List<Integer> colors) {
        return SelfStatsCatalog.glyphKind(flat, colors);
    }

    /** 把一行的所有样式段摊平后扫心数，写进给定数组（索引同 {@link #heartCounts()}）。 */
    static void addHearts(SelfStatsCatalog.Analyzed a, int[] into) {
        if (a == null || into == null) return;
        String flat = a.text();
        for (int i = 0; i < flat.length(); i++) {
            char ch = flat.charAt(i);
            if (!SelfStatsCatalog.isHeartGlyph(ch)) continue;
            // 1.4.6：地图里除了六类心，还有「❤%」这种**治疗百分比**行
            // （view_stats.mcfunction 的 HealPercent / ExtraHealing，红 ❤% 也是红心色）。
            // 只认「心形是本行最后一个非空白字符」的行，否则治疗百分比会把红心数覆盖掉
            // （实测 164 → 10）。
            if (!endsLine(flat, i + 1)) continue;
            int rgb = i < a.colors.size() ? a.colors.get(i) : 0xFFFFFF;
            int kind = SelfStatsCatalog.heartKindOf(String.valueOf(ch), rgb);
            if (kind <= 0) continue;
            int amount = SelfStatsCatalog.amountBefore(flat, i);
            if (amount >= 0) into[kind] = amount;
        }
    }

    /** 从 {@code from} 起只剩空白 → 这颗心确实在行尾（不是「❤%」那种复合图标）。 */
    private static boolean endsLine(String flat, int from) {
        for (int i = from; i < flat.length(); i++) {
            if (!Character.isWhitespace(flat.charAt(i))) return false;
        }
        return true;
    }

    /** 一行里能解析出的心数（纯函数，<b>仅 selfCheck 使用</b>）。 */
    static int[] heartsOf(Component message) {
        int[] out = new int[7];
        addHearts(analyze(message), out);
        return out;
    }

    // ── 地图翻译键表已搬到 SelfStatsCatalog（KEY_KINDS / HOVER_KINDS / buildKeyKinds / buildHoverKinds） ──
}
