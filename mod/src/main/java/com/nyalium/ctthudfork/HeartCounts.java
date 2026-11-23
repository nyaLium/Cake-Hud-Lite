package com.nyalium.ctthudfork;

import java.util.Arrays;

/**
 * 各类心的数量（纯数据，不引用任何 Minecraft 类，可在普通 JVM 里穷举测试）。
 *
 * <h2>数据包的心模型</h2>
 * <p>{@code misc/health.mcfunction} 里：</p>
 * <pre>
 *   AllHearts      = RedHearts + SoulHearts + BlackHearts + BlueHearts
 *   RedHearts      = min(RedHearts, MaxHP)        // 红心上限 = MaxHP
 *   伤害顺序       蓝 → 黑 → 魂 → 红               // 只有前面的心全空，红心才会掉
 * </pre>
 * <p>所以「红心」恒为 {@code min(AllHearts, MaxHP)}，其余各色整体上就在
 * {@code AllHearts - 红心} 里。敌人只可能带<b>魂心</b>（{@code NoSoulHearts}
 * 标签的怪连魂心都没有），因此敌人的两类心可以<b>精确</b>反推：</p>
 * <pre>
 *   红心 = min(AllHearts, MaxHP)
 *   魂心 = AllHearts - 红心
 * </pre>
 * <p>玩家独有黑心 / 蓝心（数据包 {@code no_soul_hearts.mcfunction} 只对怪清零这两种），
 * 它们只在玩家自己的 {@code pN_bossbar} 里出现，而那条 bar 只发给本人，
 * 队友只能看到 {@code spectator_bossbar} 的 {@code AllHearts/MaxHP}，
 * 拆不出黑心与蓝心 —— 所以队友这一栏留空（{@link #unknown()}），界面退回
 * 「当前/最大」。</p>
 */
public final class HeartCounts {

    /** 心类下标，顺序与需求一致：红 / 魂 / 黑 / 蓝 / 碎 / 粉。 */
    public static final int RED = 0;
    public static final int SOUL = 1;
    public static final int BLACK = 2;
    public static final int BLUE = 3;
    public static final int CRACKED = 4;
    /**
     * 粉心。数据包里和碎心共用一个 objective：
     * {@code CrackedHearts > 0} 是碎心，{@code < 0} 是粉心（绝对值才是数量）。
     * 见数据包 {@code cake_team_tower:misc/view_stats.mcfunction} 第 24-27 行：
     * 正数 {@code dark_red}，负数先取相反数存进 {@code PinkHearts} 再画 {@code light_purple}。
     */
    public static final int PINK = 5;
    public static final int COUNT = 6;

    /** 数值未知（不是 0）。 */
    public static final int UNKNOWN = Integer.MIN_VALUE;

    /**
     * 图标。红/魂/黑/蓝/碎用 emoji；碎心画暗红、粉心画亮紫（与数据包 view_stats 同色）。
     */
    public static final String[] ICONS = {
            "\u2764",             // ❤
            "\uD83D\uDC9B",       // 💛
            "\uD83D\uDDA4",       // 🖤
            "\uD83D\uDC99",       // 💙
            "\uD83D\uDC94",       // 💔 碎心
            "\uD83D\uDC97"        // 💗 粉心
    };

    /** 与血条配色一致：红=红、魂=黄、黑=紫、蓝=蓝、碎=暗红、粉=亮紫（同数据包 view_stats）。 */
    public static final int[] COLORS = {
            0xFF5555, 0xFFFF55, 0xAA00AA, 0x5555FF, 0xAA0000, 0xFF55FF
    };

    /** 当前血条表现的是哪一类心（来自数据包给 bossbar 设的颜色）。 */
    public enum Kind { RED, SOUL, BLACK, BLUE, UNKNOWN }

    private final int[] values = new int[COUNT];

    private HeartCounts(int fill) {
        Arrays.fill(values, fill);
    }

    /** 一个数都不知道 —— 界面上退回「当前/最大」。 */
    public static HeartCounts unknown() {
        return new HeartCounts(UNKNOWN);
    }

    /**
     * 敌人：只有红心与魂心，两者都精确。
     *
     * @param allHearts bossbar 标题里的 {@code AllHearts}（全部心的总量）
     * @param maxHp     bossbar 标题里的 {@code MaxHP}（红心上限）
     */
    public static HeartCounts forMob(int allHearts, int maxHp) {
        int all = Math.max(0, allHearts);
        int cap = Math.max(0, maxHp);
        int red = Math.min(all, cap);
        HeartCounts h = new HeartCounts(0);
        h.values[RED] = red;
        h.values[SOUL] = Math.max(0, all - red);
        return h;
    }

    /**
     * 全部心类都已知（服务端组件推送时用）。
     *
     * <p>碎心按数据包口径拆两栏：{@code cracked > 0} 记进 {@link #CRACKED}，
     * {@code cracked < 0} 取绝对值记进 {@link #PINK}（粉心）。</p>
     */
    public static HeartCounts of(int red, int soul, int black, int blue, int cracked) {
        HeartCounts h = new HeartCounts(0);
        h.values[RED] = Math.max(0, red);
        h.values[SOUL] = Math.max(0, soul);
        h.values[BLACK] = Math.max(0, black);
        h.values[BLUE] = Math.max(0, blue);
        h.values[CRACKED] = Math.max(0, cracked);
        h.values[PINK] = cracked < 0 ? Math.max(0, -(long) cracked > Integer.MAX_VALUE
                ? Integer.MAX_VALUE : -cracked) : 0;
        return h;
    }

    public int get(int kind) {
        return kind >= 0 && kind < COUNT ? values[kind] : UNKNOWN;
    }

    /** 这一类心存在（数值已知且 &gt; 0）。「只显示存在的类型」用它判定。 */
    public boolean has(int kind) {
        int v = get(kind);
        return v != UNKNOWN && v > 0;
    }

    /** 至少有类心能画。 */
    public boolean any() {
        for (int v : values) {
            if (v != UNKNOWN && v > 0) return true;
        }
        return false;
    }

    /** 已知的心数之和（只用于自检与调试）。 */
    public int knownSum() {
        int sum = 0;
        for (int v : values) {
            if (v != UNKNOWN) sum += v;
        }
        return sum;
    }
}
