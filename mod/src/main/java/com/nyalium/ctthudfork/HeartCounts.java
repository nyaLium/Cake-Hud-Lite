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
    /** 黑心（数据包 {black ❤}）。 */
    public static final int BLACK = 2;
    /** 蓝心（{blue ❤}）。 */
    public static final int BLUE = 3;
    /** 碎心（私有区字形 + 暗红）。 */
    public static final int CRACKED = 4;
    /**
     * 粉心。数据包里和碎心共用一个 objective：
     * {@code CrackedHearts > 0} 是碎心，{@code < 0} 是粉心（绝对值才是数量）。
     * 见数据包 {@code cake_team_tower:misc/view_stats.mcfunction} 第 24-27 行：
     * 正数 {@code dark_red}，负数先取相反数存进 {@code PinkHearts} 再画 {@code light_purple}。
     */
    public static final int PINK = 5;
    /** 心类总数（下标 0~5 有效，0 号不用；心数数组长度用 7 见各类的 {@code int[7]}）。 */
    public static final int COUNT = 6;

    /** 数值未知（不是 0）。 */
    public static final int UNKNOWN = Integer.MIN_VALUE;

    /**
     * 图标：红 / 魂 / 黑 / 蓝 = {@code ❤}，碎 / 粉 = 地图资源包里的私有区字形 {@code U+E026}。
     *
     * <p>六类原来都用 ❤，靠颜色区分。但地图的碎心 / 粉心用的是<b>自定义字形</b>
     * （数据包 {@code view_stats.mcfunction} 第 26/27 行 {@code {text:"\uE026"}}，
     * 资源包 {@code assets/minecraft/font/default.json} 把它映射到
     * {@code minecraft:custom/cracked_heart.png}，一张白色裂心贴图，由文字颜色染色）。
     * 所以 HUD 里碎 / 粉也要写这个字形，才和游戏内一致；</p>
     *
     * <p>客户端没装地图资源包时这个字形不存在，{@link #icon(int)} 会自动退回 ❤
     * （可用性由渲染端用 {@code Font.width} 探测一次，见 {@link #setCrackedGlyphAvailable(boolean)}）。</p>
     */
    public static final String HEART_GLYPH = "\u2764";
    /** 地图字体里的裂心（碎 / 粉共用，颜色区分）：{@code minecraft:custom/cracked_heart.png}。 */
    public static final String CRACKED_GLYPH = "\uE026";

    /** 六类心的图标（静态表；实际绘制请用 {@link #icon(int)}，会处理资源包缺席）。 */
    public static final String[] ICONS = {
            HEART_GLYPH,             // ❤ 红
            HEART_GLYPH,             // ❤ 魂（黄）
            HEART_GLYPH,             // ❤ 黑（紫）
            HEART_GLYPH,             // ❤ 蓝（蓝）
            CRACKED_GLYPH,           // 裂心 碎（暗红）
            CRACKED_GLYPH            // 裂心 粉（亮紫）
    };

    /** 地图资源包的裂心字形是否可用（默认 false = 没资源包，退回 ❤）。 */
    private static volatile boolean crackedGlyphAvailable = false;

    /** 渲染端探测到地图资源包字体后调用。 */
    public static void setCrackedGlyphAvailable(boolean available) {
        crackedGlyphAvailable = available;
    }

    /** 裂心字形当前是否可用（自检 / 调试用）。 */
    public static boolean crackedGlyphAvailable() {
        return crackedGlyphAvailable;
    }

    /** 某一类心实际要写的字形：碎 / 粉在资源包缺席时退回 ❤。 */
    public static String icon(int kind) {
        if (kind == CRACKED || kind == PINK) {
            return crackedGlyphAvailable ? CRACKED_GLYPH : HEART_GLYPH;
        }
        return HEART_GLYPH;
    }

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

    /**
     * 取某一类心的数量。
     *
     * @param kind {@link #RED} … {@link #PINK}
     * @return 该类的数量；下标越界返回 {@link #UNKNOWN}（不是 0）
     */
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
