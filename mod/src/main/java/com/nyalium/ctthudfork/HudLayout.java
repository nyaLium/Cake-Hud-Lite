package com.nyalium.ctthudfork;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯函数布局计算：<b>不引用任何 Minecraft 类</b>，因此可以在普通 JVM 里穷举测试
 * 每一个配置组合（见 {@code src/test/java/.../SelfCheck.java}）。
 *
 * <h2>坐标系</h2>
 * <p>「原版名牌像素」：原点 = 原版名牌锚点，y 向下为正，1 像素 = 0.025 格（再乘缩放）。</p>
 * <p>原版 {@code EntityRenderer.submitNameDisplay} 的顺序是：</p>
 * <pre>
 *   if (scoreText != null) { 画分数; pose.translate(0, 9 × 1.15 × 0.025, 0); }  // 上抬 10.35 像素
 *   if (nameTag   != null) { 画名字 }
 * </pre>
 * <p>分数一旦隐藏，那次上抬就不执行，名字自然落回 {@code y ∈ [0, 9]}。</p>
 *
 * <h2>固定排版</h2>
 * <p>位置从下往上排，行数<b>不随内容宽度变化</b>：</p>
 * <pre>
 *   敌人：  护甲行（🛡 + 2 项元素护甲，🛡 与元素之间两个空格）
 *           ❤当前/上限 + 一种轮换心（魂/黑/蓝/碎/粉）
 *   ───────── 血条 ─────────
 *   名称（原版）
 *
 *   玩家：  碎心/粉心 + 蓝心（同时显示）
 *           ❤当前/上限 + 一种轮换心（魂 / 黑）
 *   ───────── 血条 ─────────
 *   名称（原版）
 * </pre>
 *
 * <p>所以 {@link Result#rows} 的顺序是<b>从上到下</b>，最下面那一行永远贴在 {@link #TEXT_TOP}。</p>
 */
public final class HudLayout {

    /** 最下面一行文字的顶部（布局像素）。 */
    public static final int TEXT_TOP = -18;
    /** 血条顶部（布局像素）。血条下沿 = -2，正好留出名字背景框的上沿。 */
    public static final int BAR_TOP = -7;
    /** 血条高度 = 原版 bossbar 纹理高度。 */
    public static final int BAR_HEIGHT = 5;
    /** 名称行顶部。与原版「没有 below_name 分数时」的位置逐像素一致。<b>仅自检使用</b>（线上由渲染端固定 0）。 */
    public static final int NAME_TOP = 0;
    /** 名称行高度（原版字体行高）。 */
    public static final int NAME_HEIGHT = 9;

    /**
     * 一行文字的<b>背景框</b>高度。
     *
     * <p>原版 {@code Font.PreparedTextBuilder.markBackground} 是从 {@code y - 1} 画到
     * {@code y + 9}，比字体的 9 像素行高<b>多出 1 像素</b>。两行挨着画时行距必须是 10，
     * 否则上行的底边会压住下行的顶边 1 像素，两层 25% 黑叠成一条更深的暗带。</p>
     */
    public static final int TEXT_BOX_HEIGHT = NAME_HEIGHT + 1;

    /** 行距：往上一行抬这么多像素。必须 ≥ {@link #TEXT_BOX_HEIGHT}。 */
    public static final int ROW_STEP = TEXT_BOX_HEIGHT;

    /** 血条长度的允许范围与默认值（原版名牌像素）。默认 75。 */
    /** 血条长度下限（原版名牌像素）。 */
    public static final int BAR_WIDTH_MIN = 50;
    /** 血条长度上限。 */
    public static final int BAR_WIDTH_MAX = 150;
    /** 血条长度默认值（75，旧默认 100 会被迁移改掉）。 */
    public static final int BAR_WIDTH_DEFAULT = 75;

    /** 同一行内各段之间的间隔（各类心之间、元素护甲之间）。 */
    public static final String HEART_GAP = " ";

    /**
     * 「护甲 ↔ 元素护甲」之间的间隔：**两个空格**。
     * 比心与心之间的一个空格更宽，玩家一眼能把防御值和元素护甲分开。
     */
    public static final String ARMOUR_GAP = "  ";

    /**
     * 元素护甲一页最多显示几项。
     *
     * <p>七种元素护甲同时显示会把整行拉得很长，所以护甲部分**每页 2 项、循环翻页**；
     * {@code 🛡 防御} 不受分页影响，一直显示。翻页节奏见 {@code ArmourCycle}。</p>
     */
    public static final int ARMOUR_PAGE_SIZE = 2;

    /** 文字宽度测量器（游戏里是真 Font，测试里可以是假实现）。 */
    public interface WidthMeasurer {
        int width(String text);
    }

    /** 一帧布局需要的开关（与 {@link HudConfig.Cfg} 解耦，便于测试）。 */
    public static final class Options {
        /** 是否画血量数字（敌人：{@code 当前/上限} 或百分比）。 */
        public boolean showNumbers = true;
        /** 血量数字优先用心数（红/魂/黑/蓝/碎），拆不出来时才退回「当前/最大」。 */
        public boolean showHearts = true;
        /** 是否画护甲段（玩家侧固定关，见 {@code HudWorldRenderer#teamOptions}）。 */
        public boolean showArmor = true;
        /** 是否画血条（敌人 / 玩家都是「整块」开关的一部分）。 */
        public boolean showBar = true;
        /** 固定长度（原版名牌像素），夹在 BAR_WIDTH_MIN ~ BAR_WIDTH_MAX。 */
        public double barWidth = BAR_WIDTH_DEFAULT;
        /** 文字是否带阴影（纯渲染开关，放这里只为让测试能覆盖到）。 */
        public boolean textShadow = true;
        /** 整组 UI 缩放，1.0 = 与原版名牌同字号。 */
        public double scale = 1.0;
        /**
         * 元素护甲当前翻到第几页（0 起）。{@code 🛡 防御} 不参与分页。
         * 只会取 {@code [0, 页数)}，越界时夹回范围内（见 {@link #armourPageCount(int[])}）。
         */
        public int armourPage = 0;
        /**
         * 心数行里「除红心外要显示的那一类心」（由 {@link HeartCycle} 按时间挑出来）。
         * {@code -1} = 不显示；布局只认「这一类心确实存在」的情况。
         */
        public int cycleKind = -1;
        /**
         * true = 玩家排版：碎心/粉心与蓝心**同时**固定在最上面一行，心数行只轮换魂 / 黑。
         * false = 敌人排版：最上面只有护甲，魂 / 黑 / 蓝 / 碎 / 粉都进轮换。
         */
        public boolean playerStyle = false;
    }

    /** 一段带颜色的文字。 */
    public static final class Line {
        /** 这一段的文字（已按段切开，绘制时不再做样式解析）。 */
        public final String text;
        /** 这一段的颜色（0xRRGGBB，不含 alpha）。 */
        public final int rgb;

        Line(String text, int rgb) {
            this.text = text;
            this.rgb = rgb;
        }
    }

    /** 一整行（若干段首尾相接），多行时各自居中。 */
    public static final class Row {
        public final List<Line> lines;
        /** 本行总宽（布局像素）。 */
        public final int width;
        /** 本行文字顶部（布局像素，向上为负）。 */
        public final int top;

        Row(List<Line> lines, int width, int top) {
            this.lines = lines;
            this.width = width;
            this.top = top;
        }

        /** 本行是不是护甲行（第一段是 🛡 或护甲图标）。调试 / 测试用。 */
        public boolean isArmourRow() {
            return !lines.isEmpty() && isArmourIcon(lines.get(0).text);
        }

        private static boolean isArmourIcon(String text) {
            if (text == null || text.isEmpty()) return false;
            if (text.startsWith("\uD83D\uDEE1")) return true;          // 🛡 防御
            for (String icon : EnemyData.ARMOR_ICONS) {
                if (text.startsWith(icon)) return true;
            }
            return false;
        }
    }

    /** 布局结果。 */
    public static final class Result {
        /** 从上到下的可见行；最后一个是最靠近血条那行。 */
        public final List<Row> rows;
        /** 所有行扁平化后的段（调试 / 测试用）。 */
        public final List<Line> lines;
        /** 最宽那一行文字的像素宽（排版列宽用）。 */
        public final int textWidth;
        /** 解析后的血条长度（已夹进 BAR_WIDTH_MIN~MAX；{@code showBar=false} 时为 0）。 */
        public final int barWidth;
        /** 最下面一行的文字顶部（多行时上面的行依次再往上 ROW_STEP 像素）。 */
        public final int textTop;
        /** 血条顶部的相对 y（{@code BAR_TOP}）。 */
        public final int barTop;
        /** 是否有任何文字行（{@code false} 时只画血条）。 */
        public final boolean hasText;

        Result(List<Row> rows, List<Line> lines, int textWidth, int barWidth,
               int textTop, int barTop) {
            this.rows = rows;
            this.lines = lines;
            this.textWidth = textWidth;
            this.barWidth = barWidth;
            this.textTop = textTop;
            this.barTop = barTop;
            this.hasText = textWidth > 0;
        }

        /** 完全没有内容可画（三个开关全关 / 没有任何数据）。 */
        public boolean isEmpty() {
            return !hasText && barWidth <= 0;
        }

        /** 文字一共占几行。 */
        public int rowCount() {
            return rows.size();
        }
    }

    private HudLayout() {}

    /**
     * 计算一帧的布局。
     *
     * @param hp         当前血量（{@code AllHearts}）
     * @param maxHp      最大血量（{@code MaxHP}，红心上限）
     * @param percentOnly true = 只有百分比，没有绝对值
     * @param defence    防御值，0 = 不画
     * @param armor      七档元素护甲，0 = 不画
     * @param hearts     各类心的数量，null / 全未知 = 退回「当前/最大」
     */
    public static Result compute(int hp, int maxHp, boolean percentOnly,
                                 int defence, int[] armor, HeartCounts hearts,
                                 Options options, WidthMeasurer measurer) {
        Options opt = options != null ? options : new Options();
        WidthMeasurer w = measurer != null ? measurer : String::length;

        // ① 最上面那一行
        //    敌人：只有护甲（🛡 + 元素护甲按页取 2 项），🛡 与元素护甲之间留**两个空格**；
        //    玩家：碎心/粉心 + 蓝心（同时显示，护甲段对玩家关闭）。
        List<Line> top = new ArrayList<>(8);
        if (opt.showArmor && hasArmor(defence, armor)) {
            if (defence != 0) {
                top.add(new Line("\uD83D\uDEE1" + num(defence), EnemyData.DEFENCE_COLOR));
            }
            // 真盔甲（TrueArmor）不再参与翻页，和 🛡 防御一样常驻。
            // 数据包里它是 ArmorDisplay 的一页（白 🛡），但它属于「护甲」类而不是元素抗性。
            int trueArmor = trueArmorValue(armor);
            if (trueArmor != 0) {
                // 只有前面已经有内容（🛡 防御）时才补分隔空格 ——
                // 没有防御、只有真·防御/元素护甲时，行首不会再顶一个空格。
                if (!top.isEmpty()) {
                    top.add(new Line(HEART_GAP, 0xFFFFFF));
                }
                top.add(new Line(EnemyData.ARMOR_ICONS[EnemyData.TRUE_ARMOR_INDEX]
                        + num(trueArmor), EnemyData.ARMOR_COLORS[EnemyData.TRUE_ARMOR_INDEX]));
            }
            List<Line> elements = armourPage(armor, opt.armourPage);
            if (!elements.isEmpty()) {
                if (!top.isEmpty()) {
                    // 「护甲 ↔ 元素护甲」之间两个空格，比心与心之间更醒目。
                    top.add(new Line(ARMOUR_GAP, 0xFFFFFF));
                }
                for (int i = 0; i < elements.size(); i++) {
                    if (i > 0) top.add(new Line(HEART_GAP, 0xFFFFFF));
                    top.add(elements.get(i));
                }
            }
        }

        // ② 心数行：红心 + **一种**其它心（按时间轮换，见 HeartCycle）。
        //    玩家把碎心/粉心、蓝心固定放在最上面一行，所以这里只会是魂 / 黑。
        List<Line> heartRow = new ArrayList<>(4);
        if (opt.showNumbers) {
            if (opt.showHearts && hearts != null && hearts.any()) {
                if (opt.playerStyle) {
                    // 玩家固定两行、不再轮换：
                    //   上排：魂心 / 黑心 / 蓝心（有才写）
                    //   下排：红心 当前/上限 + 碎心或粉心（有才写）
                    addKinds(top, hearts,
                            HeartCounts.SOUL, HeartCounts.BLACK, HeartCounts.BLUE);
                    if (hearts.has(HeartCounts.RED)) {
                        heartRow.add(redLine(hearts.get(HeartCounts.RED), maxHp));
                    }
                    addKinds(heartRow, hearts, HeartCounts.CRACKED, HeartCounts.PINK);
                } else {
                    if (hearts.has(HeartCounts.RED)) {
                        heartRow.add(redLine(hearts.get(HeartCounts.RED), maxHp));
                    }
                    if (opt.cycleKind >= 0 && hearts.has(opt.cycleKind)) {
                        if (!heartRow.isEmpty()) {
                            heartRow.add(new Line(HEART_GAP, 0xFFFFFF));
                        }
                        heartRow.add(kindLine(opt.cycleKind, hearts.get(opt.cycleKind)));
                    }
                }
            } else if (percentOnly) {
                // 纯客户端只有 below_name 的 HealthPercent：拆不出各类心，但**至少把百分比数字画出来**
                // —— 血条本来就是按这个百分比填的，带上数字才读得懂（队友无队伍条时同一条路径）。
                // HealthPercent 是「红心 ÷ MaxHP」，所以前面加一个红心图标（与心数行同款）。
                heartRow.add(new Line(HeartCounts.icon(HeartCounts.RED), HeartCounts.COLORS[HeartCounts.RED]));
                heartRow.add(new Line(HEART_GAP, 0xFFFFFF));
                heartRow.add(new Line(percentText(hp, maxHp), 0xFFFFFF));
            } else {
                // 拆不出各类心（例如纯客户端队友只有 AllHearts/MaxHP）：退回「当前/最大」。
                // 前面加红心（这是红心血量的口径），与 percentOnly / 心数行统一。
                heartRow.add(new Line(HeartCounts.icon(HeartCounts.RED), HeartCounts.COLORS[HeartCounts.RED]));
                heartRow.add(new Line(HEART_GAP, 0xFFFFFF));
                heartRow.add(new Line(num(Math.max(0, hp)) + "/" + num(Math.max(0, maxHp)), 0xFFFFFF));
            }
        }

        int barWidth = 0;
        if (opt.showBar) {
            barWidth = resolveBarWidth(opt.barWidth);
        }

        // 从上到下：最上面那一行 → 心数行。最下面那行贴 TEXT_TOP。
        List<List<Line>> groups = new ArrayList<>(2);
        if (!top.isEmpty()) groups.add(top);
        if (!heartRow.isEmpty()) groups.add(heartRow);

        int bottomTop = TEXT_TOP;
        int count = groups.size();
        List<Row> rows = new ArrayList<>(count);
        List<Line> flat = new ArrayList<>(12);
        int textWidth = 0;
        for (int i = 0; i < count; i++) {
            List<Line> group = groups.get(i);
            int width = width(group, w);
            rows.add(new Row(group, width, bottomTop - ROW_STEP * (count - 1 - i)));
            flat.addAll(group);
            textWidth += width;
        }

        return new Result(rows, flat, textWidth, barWidth, bottomTop, BAR_TOP);
    }

    /** 当前页的元素护甲段（最多 {@link #ARMOUR_PAGE_SIZE} 项，不带分隔空格）。 */
    private static List<Line> armourPage(int[] armor, int page) {
        List<Line> out = new ArrayList<>(ARMOUR_PAGE_SIZE);
        if (armor == null) return out;
        int current = clampArmourPage(page, armourPageCount(armor));
        int seen = 0;
        int n = Math.min(armor.length, EnemyData.ARMOR_ICONS.length);
        for (int i = 0; i < n; i++) {
            if (i == EnemyData.TRUE_ARMOR_INDEX) continue;   // 1.2.2：真盔甲常驻，不占页
            int v = armor[i];
            if (v == 0) continue;
            int itemPage = seen / ARMOUR_PAGE_SIZE;
            seen++;
            if (itemPage != current) continue;
            out.add(new Line(EnemyData.ARMOR_ICONS[i] + num(v), EnemyData.ARMOR_COLORS[i]));
        }
        return out;
    }

    /**
     * 红心段：{@code ❤当前/红心上限}。
     *
     * <p>红心上限就是 {@code MaxHP}（数据包 {@code RedHearts = min(RedHearts, MaxHP)}）；
     * 上限未知（≤ 0）时只写当前值。</p>
     */
    private static Line redLine(int red, int maxHp) {
        String icon = HeartCounts.icon(HeartCounts.RED);
        String cur = num(Math.max(0, red));
        String text = maxHp > 0 ? icon + cur + "/" + num(maxHp) : icon + cur;
        return new Line(text, HeartCounts.COLORS[HeartCounts.RED]);
    }

    /** 某一类心的一段：图标 + 数量（数量走 {@link #num} 缩写）。 */
    private static Line kindLine(int kind, int value) {
        return new Line(HeartCounts.icon(kind) + num(value), HeartCounts.COLORS[kind]);
    }

    /**
     * 「73%」：纯客户端只有 {@code HealthPercent} 时的数字行。
     *
     * <p>口径与数据包 {@code health.mcfunction:102-104} 一致：{@code hp*100/maxHp} 四舍五入后夹到 0~100；
     * 上限非法（≤ 0）时按 100% 处理，与「没有血量信息」时的默认填充率一致。</p>
     */
    static String percentText(int hp, int maxHp) {
        int percent;
        if (maxHp <= 0) {
            percent = 100;
        } else {
            percent = Math.round(Math.max(0, hp) * 100.0f / maxHp);
        }
        return Math.max(0, Math.min(100, percent)) + "%";
    }

    private static int width(List<Line> lines, WidthMeasurer w) {
        int total = 0;
        for (Line line : lines) {
            total += Math.max(0, w.width(line.text));
        }
        return total;
    }

    /**
     * 把指定几类心各写成一段，追加到 {@code lines}，<b>只写存在的类型</b>。
     * 段之前补一个空格（{@code lines} 已有内容时）。碎心与粉心互斥。
     */
    private static void addKinds(List<Line> lines, HeartCounts hearts, int... kinds) {
        for (int kind : kinds) {
            if (!hearts.has(kind)) continue;
            if (!lines.isEmpty()) {
                lines.add(new Line(HEART_GAP, 0xFFFFFF));
            }
            lines.add(kindLine(kind, hearts.get(kind)));
        }
    }

    // ── 大数值缩写 ──

    /** 超过这个值开始用 K / M / B 缩写（从 100000 降到 1000）。
     *
     * <p>按需求「整数位必须 ≥ 1」：900 仍写 {@code 900}，不会变成 {@code 0.9K}；
     * 1000 才进 K。小数一位、**截断**（24999 → {@code 24.9K}）。</p> */
    public static final int NUM_EXACT_LIMIT = 1_000;

    /**
     * 数字的显示文本：{@code < 100000} 原样，再大改写成 {@code 123.4K / 12.3M / 2.1B}。
     *
     * <p>实测过：把心数写成 {@code 999999} 这种 6 位数时，一行五段会把文字拉出屏幕两侧
     * （centered 后被裁掉）。缩写把每段压到 5~6 个字符；小数**截断**而不是四舍五入，
     * 所以宁可少报一点也不虚报。后缀用大写（K/M/B），和常见游戏一致。</p>
     */
    public static String num(int value) {
        long n = Math.abs((long) value);
        String sign = value < 0 ? "-" : "";
        if (n < NUM_EXACT_LIMIT) return sign + n;
        if (n < 1_000_000L) return sign + scaled(n, 1_000L) + "K";
        if (n < 1_000_000_000L) return sign + scaled(n, 1_000_000L) + "M";
        return sign + scaled(n, 1_000_000_000L) + "B";
    }

    /**
     * 截断到 1 位小数（不四舍五入）。
     *
     * <p>去掉「整数位 ≥ 100 就不带小数」的旧规则 —— 999999 现在写 {@code 999.9K}，
     * 不再是 {@code 999K}。整数位仍保证 ≥ 1（不会出现 {@code 0.9K}）。</p>
     */
    private static String scaled(long n, long unit) {
        long whole = n / unit;
        long tenth = (n % unit) / (unit / 10L);
        if (tenth == 0) return Long.toString(whole);
        return whole + "." + tenth;
    }

    // ── 元素护甲翻页 ──

    /** 元素护甲一共几页；没有护甲时 0 页。{@code 🛡 防御} 不计入。 */
    public static int armourPageCount(int[] armor) {
        if (armor == null) return 0;
        int items = 0;
        int n = Math.min(armor.length, EnemyData.ARMOR_ICONS.length);
        for (int i = 0; i < n; i++) {
            if (i == EnemyData.TRUE_ARMOR_INDEX) continue;   // 真盔甲常驻，不算页
            if (armor[i] != 0) items++;
        }
        return (items + ARMOUR_PAGE_SIZE - 1) / ARMOUR_PAGE_SIZE;
    }

    /** 真盔甲的值（下标见 {@link EnemyData#TRUE_ARMOR_INDEX}）；数组不够长时 0。 */
    static int trueArmorValue(int[] armor) {
        if (armor == null || armor.length <= EnemyData.TRUE_ARMOR_INDEX) return 0;
        return armor[EnemyData.TRUE_ARMOR_INDEX];
    }

    /** 页号夹到 {@code [0, pages)}；页数为 0 时返回 0。 */
    public static int clampArmourPage(int page, int pages) {
        if (pages <= 0) return 0;
        return Math.max(0, Math.min(pages - 1, page));
    }

    /**
     * 血条要画多长（像素）：{@code ratio <= 0} 时 0，超小比例（残血）至少 **1 px**。
     *
     * <p>地图的分数是整数，{@code 红心 1 / 上限 325} 算出来是 0.3%，乘条宽后
     * {@code Math.round} 会变成 0 —— 看上去像「血条完全不渲染」。这里保证比例 &gt; 0 时
     * 至少留 1 px，让「还剩一点」看得出来。</p>
     */
    public static int fillPixels(int width, float ratio) {
        if (width <= 0) return 0;
        float r = Math.max(0.0f, Math.min(1.0f, ratio));
        int fill = Math.round(width * r);
        if (fill <= 0 && r > 0.0f) fill = 1;
        return Math.min(width, fill);
    }

    /** 血条长度：夹在 [BAR_WIDTH_MIN, BAR_WIDTH_MAX]，非法值回落默认值。 */
    public static int resolveBarWidth(double configured) {
        if (Double.isNaN(configured)) return BAR_WIDTH_DEFAULT;
        int width = (int) Math.round(configured);
        return Math.max(BAR_WIDTH_MIN, Math.min(BAR_WIDTH_MAX, width));
    }

    /** 这一实体有没有护甲段要画（{@code 🛡 防御} 非 0 或任一元素护甲非 0）。 */
    public static boolean hasArmor(int defence, int[] armor) {
        if (defence != 0) return true;
        if (armor == null) return false;
        for (int v : armor) {
            if (v != 0) return true;
        }
        return false;
    }
}
