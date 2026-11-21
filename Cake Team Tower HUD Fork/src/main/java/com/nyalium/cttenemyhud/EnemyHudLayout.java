package com.nyalium.cttenemyhud;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯函数布局计算：<b>不引用任何 Minecraft 类</b>，因此可以在普通 JVM 里穷举测试
 * 每一个配置组合（见 {@code src/test/java/.../SelfCheck.java}）。
 *
 * <h2>坐标系</h2>
 * <p>「原版名牌像素」：原点 = 原版名牌锚点，y 向下为正，1 像素 = 0.025 格。</p>
 * <p>原版 {@code EntityRenderer.submitNameDisplay} 的顺序是：</p>
 * <pre>
 *   if (scoreText != null) { 画分数; pose.translate(0, 9 × 1.15 × 0.025, 0); }  // 上抬 10.35 像素
 *   if (nameTag   != null) { 画名字 }
 * </pre>
 * <p>也就是说<b>分数</b>贴在锚点（{@code y ∈ [0, 9]}），<b>名字</b>在分数被上抬后才画。
 * 分数一旦隐藏，那次上抬就不执行，名字自然落回 {@code y ∈ [0, 9]} —— 这正是需要的
 * 「名字下移」位置，本 Mod 自己画名字时就用它（{@link #NAME_TOP}）。</p>
 *
 * <p>于是整组 HUD 自下而上：名称 {@code [0, 9]} → 血条 → 文字，互不重叠。</p>
 *
 * <h2>两行文字</h2>
 * <p>红心现在写成 {@code ❤当前/上限}，配上魂心/护甲后整行容易比血条还宽。所以文字是
 * <b>可折行</b>的：超过「换行阈值」({@link Options#wrapWidth}，默认
 * {@link #WRAP_AUTO} = 不超过血条长度) 时，红心单独占上面一行，其余心与护甲留在下面一行；
 * 上面的行每次再抬 {@link #ROW_STEP} 像素，血条位置不变。</p>
 */
public final class EnemyHudLayout {

    /** 文字行顶部（布局像素）。 */
    public static final int TEXT_TOP = -18;
    /** 血条顶部（布局像素）。血条下沿 = -2，正好留出名字背景框的上沿。 */
    public static final int BAR_TOP = -7;
    /** 血条高度 = 原版 bossbar 纹理高度。 */
    public static final int BAR_HEIGHT = 5;
    /** 名称行顶部。与原版「没有 below_name 分数时」的位置逐像素一致。 */
    public static final int NAME_TOP = 0;
    /** 名称行高度（原版字体行高）。 */
    public static final int NAME_HEIGHT = 9;

    /** 行高：文字折成多行时，每往上一行抬这么多像素（与原版字体行高一致）。 */
    public static final int ROW_STEP = 9;

    /** 换行阈值可调下限（像素）。 */
    public static final int WRAP_MIN = 40;
    /** 换行阈值可调上限；滑块拉到这里 = **自动**。 */
    public static final int WRAP_MAX = 200;
    /** 阈值等于这个值即「自动」：不超过血条长度。 */
    public static final double WRAP_AUTO = WRAP_MAX;

    /** 血条长度的允许范围与默认值（原版名牌像素）。 */
    public static final int BAR_WIDTH_MIN = 50;
    public static final int BAR_WIDTH_MAX = 150;
    public static final int BAR_WIDTH_DEFAULT = 100;

    /** 分段之间的分隔符与颜色（灰）。 */
    public static final String SEPARATOR = " | ";
    public static final int SEPARATOR_COLOR = 0x9E9E9E;

    /** 同一组内各段之间的间隔（各类心之间用它，不是分隔符）。 */
    public static final String HEART_GAP = " ";

    /** 文字宽度测量器（游戏里是真 Font，测试里可以是假实现）。 */
    public interface WidthMeasurer {
        int width(String text);
    }

    /** 一帧布局需要的开关（与 {@link EnemyHudConfig.Cfg} 解耦，便于测试）。 */
    public static final class Options {
        public boolean showNumbers = true;
        /** 血量数字优先用心数（红/魂/黑/蓝/碎），拆不出来时才退回「当前/最大」。 */
        public boolean showHearts = true;
        public boolean showArmor = true;
        public boolean showBar = true;
        /** 固定长度（原版名牌像素），夹在 BAR_WIDTH_MIN ~ BAR_WIDTH_MAX。 */
        public double barWidth = BAR_WIDTH_DEFAULT;
        /**
         * 换行阈值（像素）。{@link #WRAP_AUTO} = 自动（不超过血条长度）。
         * 整行文字超过它就折成两行：红心在上，其余心 + 护甲在下。
         */
        public double wrapWidth = WRAP_AUTO;
        public int verticalOffset = 0;
        /** 文字是否带阴影（纯渲染开关，放这里只为让测试能覆盖到）。 */
        public boolean textShadow = true;
        /** 整组 UI 缩放，1.0 = 与原版名牌同字号。 */
        public double scale = 1.0;
    }

    /** 一段带颜色的文字。 */
    public static final class Line {
        public final String text;
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
    }

    /** 布局结果。 */
    public static final class Result {
        /** 从下到上按视觉顺序排列：最后一个是最靠近血条那行。 */
        public final List<Row> rows;
        /** 所有行扁平化后的段（调试 / 测试用）。 */
        public final List<Line> lines;
        public final int textWidth;
        public final int barWidth;
        /** 最底一行的文字顶部（多行时上面的行依次再往上 9 像素）。 */
        public final int textTop;
        public final int barTop;
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

    private EnemyHudLayout() {}

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

        // 红心（或队友的「当前/最大」）单独一组，太长时它自己占上面一行。
        List<Line> head = new ArrayList<>(4);
        // 其它心 + 护甲，留在下面一行。
        List<Line> rest = new ArrayList<>(12);

        if (opt.showNumbers) {
            if (opt.showHearts && hearts != null && hearts.any()) {
                if (hearts.has(HeartCounts.RED)) {
                    head.add(redLine(hearts.get(HeartCounts.RED), maxHp));
                }
                appendHeartSegments(rest, hearts);
            } else if (!percentOnly) {
                // 拆不出各类心（例如队友只有 AllHearts/MaxHP）：退回原来的「当前/最大」。
                head.add(new Line(Math.max(0, hp) + "/" + Math.max(0, maxHp), 0xFFFFFF));
            }
        }

        if (opt.showArmor && hasArmor(defence, armor)) {
            // 分隔符是「心组 ↔ 护甲」之间的，红心在 head 里也算有内容。
            if (!head.isEmpty() || !rest.isEmpty()) {
                rest.add(new Line(SEPARATOR, SEPARATOR_COLOR));
            }
            if (defence != 0) {
                rest.add(new Line("\uD83D\uDEE1" + defence, EnemyData.DEFENCE_COLOR));
            }
            if (armor != null) {
                int n = Math.min(armor.length, EnemyData.ARMOR_ICONS.length);
                for (int i = 0; i < n; i++) {
                    int v = armor[i];
                    if (v == 0) continue;
                    if (!rest.isEmpty()) {
                        rest.add(new Line(" ", 0xFFFFFF));
                    }
                    rest.add(new Line(EnemyData.ARMOR_ICONS[i] + v, EnemyData.ARMOR_COLORS[i]));
                }
            }
        }

        int barWidth = 0;
        if (opt.showBar) {
            barWidth = resolveBarWidth(opt.barWidth);
        }

        // 分隔符只存在于「心 ↔ 护甲」之间；护甲成为单独一行时，换行就代替了它。
        boolean hasSeparator = !rest.isEmpty() && SEPARATOR.equals(rest.get(0).text);

        List<Line> merged = new ArrayList<>(head.size() + rest.size() + 1);
        merged.addAll(head);
        if (!head.isEmpty() && !rest.isEmpty() && !hasSeparator) {
            merged.add(new Line(HEART_GAP, 0xFFFFFF));
        }
        merged.addAll(rest);

        List<Line> bottom = hasSeparator
                ? new ArrayList<>(rest.subList(1, rest.size()))
                : rest;

        int headWidth = width(head, w);
        int bottomWidth = width(bottom, w);
        int mergedWidth = width(merged, w);

        // 一行放得下就一行；放不下（且红心之后确实还有东西）就把红心顶到上面一行。
        int wrapLimit = resolveWrapWidth(opt.wrapWidth, barWidth);
        List<List<Line>> groups = new ArrayList<>(2);
        if (headWidth > 0 && bottomWidth > 0 && mergedWidth > wrapLimit) {
            groups.add(head);
            groups.add(bottom);
        } else if (!merged.isEmpty()) {
            groups.add(merged);
        }

        int bottomTop = TEXT_TOP + opt.verticalOffset;
        int count = groups.size();
        List<Row> rows = new ArrayList<>(count);
        List<Line> flat = new ArrayList<>(12);
        int flatWidth = 0;
        for (int i = 0; i < count; i++) {
            List<Line> group = groups.get(i);
            int width = width(group, w);
            // 最后一行贴着 TEXT_TOP，往上的行依次再抬 9 像素。
            rows.add(new Row(group, width, bottomTop - ROW_STEP * (count - 1 - i)));
            flat.addAll(group);
            flatWidth += width;
        }

        return new Result(rows, flat, flatWidth, barWidth,
                bottomTop, BAR_TOP + opt.verticalOffset);
    }

    /**
     * 红心段：{@code ❤当前/红心上限}。
     *
     * <p>红心上限就是 {@code MaxHP}（数据包 {@code RedHearts = min(RedHearts, MaxHP)}）；
     * 上限未知（≤ 0）时只写当前值。</p>
     */
    private static Line redLine(int red, int maxHp) {
        String icon = HeartCounts.ICONS[HeartCounts.RED];
        String text = maxHp > 0 ? icon + Math.max(0, red) + "/" + maxHp : icon + Math.max(0, red);
        return new Line(text, HeartCounts.COLORS[HeartCounts.RED]);
    }

    private static int width(List<Line> lines, WidthMeasurer w) {
        int total = 0;
        for (Line line : lines) {
            total += Math.max(0, w.width(line.text));
        }
        return total;
    }

    /**
     * 除红心以外的各类心：「魂心 X 黑心 X 蓝心 X 碎心 X 粉心 X」，<b>只写存在的类型</b>。
     *
     * <p>顺序固定为魂 / 黑 / 蓝 / 碎 / 粉，不随数值大小变化，眼睛每次落在同一位置。
     * 碎心与粉心互斥（数据包 {@code CrackedHearts} 的符号决定是哪一个），所以最多只出现一个。</p>
     *
     * <p>各类心之间只用 <b>一个空格</b> 相连，整组算「一段」；只有心组与护甲之间
     * 才用 {@link #SEPARATOR} 隔开（{@code ❤100/100 💛60 | 🛡3 ☃5}）。红心由
     * {@link #redLine} 单独产出，这里从 {@link HeartCounts#SOUL} 开始。</p>
     */
    private static void appendHeartSegments(List<Line> lines, HeartCounts hearts) {
        for (int kind = HeartCounts.SOUL; kind < HeartCounts.COUNT; kind++) {
            if (!hearts.has(kind)) continue;
            if (!lines.isEmpty()) {
                lines.add(new Line(HEART_GAP, 0xFFFFFF));
            }
            lines.add(new Line(HeartCounts.ICONS[kind] + hearts.get(kind), HeartCounts.COLORS[kind]));
        }
    }

    /** 血条长度：夹在 [BAR_WIDTH_MIN, BAR_WIDTH_MAX]，非法值回落默认值。 */
    public static int resolveBarWidth(double configured) {
        if (Double.isNaN(configured)) return BAR_WIDTH_DEFAULT;
        int width = (int) Math.round(configured);
        return Math.max(BAR_WIDTH_MIN, Math.min(BAR_WIDTH_MAX, width));
    }

    /**
     * 换行阈值的最终像素值。
     *
     * <p>拉到最右（{@code >= WRAP_AUTO}）或值非法 → **自动**，直接用血条长度；
     * 血条关掉（长度 0）时用 {@link #BAR_WIDTH_DEFAULT} 兜底，避免所有怪都被折成两行。</p>
     */
    public static int resolveWrapWidth(double configured, int barWidth) {
        if (Double.isNaN(configured) || configured <= 0 || configured >= WRAP_AUTO) {
            return barWidth > 0 ? barWidth : BAR_WIDTH_DEFAULT;
        }
        return (int) Math.round(configured);
    }

    public static boolean hasArmor(int defence, int[] armor) {
        if (defence != 0) return true;
        if (armor == null) return false;
        for (int v : armor) {
            if (v != 0) return true;
        }
        return false;
    }
}
