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

    /** 血条长度的允许范围与默认值（原版名牌像素）。 */
    public static final int BAR_WIDTH_MIN = 50;
    public static final int BAR_WIDTH_MAX = 150;
    public static final int BAR_WIDTH_DEFAULT = 100;

    /** 分段之间的分隔符与颜色（灰）。 */
    public static final String SEPARATOR = " | ";
    public static final int SEPARATOR_COLOR = 0x9E9E9E;

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

    /** 布局结果。 */
    public static final class Result {
        public final List<Line> lines;
        public final int textWidth;
        public final int barWidth;
        public final int textTop;
        public final int barTop;
        public final boolean hasText;

        Result(List<Line> lines, int textWidth, int barWidth, int textTop, int barTop) {
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

        List<Line> lines = new ArrayList<>(12);

        if (opt.showNumbers) {
            if (opt.showHearts && hearts != null && hearts.any()) {
                appendHeartSegments(lines, hearts);
            } else if (!percentOnly) {
                // 拆不出各类心（例如队友只有 AllHearts/MaxHP）：退回原来的「当前/最大」。
                lines.add(new Line(Math.max(0, hp) + "/" + Math.max(0, maxHp), 0xFFFFFF));
            }
        }

        if (opt.showArmor && hasArmor(defence, armor)) {
            if (!lines.isEmpty()) {
                lines.add(new Line(SEPARATOR, SEPARATOR_COLOR));
            }
            if (defence != 0) {
                lines.add(new Line("\uD83D\uDEE1" + defence, EnemyData.DEFENCE_COLOR));
            }
            if (armor != null) {
                int n = Math.min(armor.length, EnemyData.ARMOR_ICONS.length);
                for (int i = 0; i < n; i++) {
                    int v = armor[i];
                    if (v == 0) continue;
                    if (!lines.isEmpty()) {
                        lines.add(new Line(" ", 0xFFFFFF));
                    }
                    lines.add(new Line(EnemyData.ARMOR_ICONS[i] + v, EnemyData.ARMOR_COLORS[i]));
                }
            }
        }

        int textWidth = 0;
        for (Line line : lines) {
            textWidth += Math.max(0, w.width(line.text));
        }

        int barWidth = 0;
        if (opt.showBar) {
            barWidth = resolveBarWidth(opt.barWidth);
        }

        return new Result(lines, textWidth, barWidth,
                TEXT_TOP + opt.verticalOffset,
                BAR_TOP + opt.verticalOffset);
    }

    /**
     * 「红心 X | 魂心 X | 黑心 X | 蓝心 X | 碎心 X」，<b>只写存在的类型</b>。
     *
     * <p>顺序固定为红 / 魂 / 黑 / 蓝 / 碎，不随数值大小变化，眼睛每次落在同一位置。</p>
     */
    private static void appendHeartSegments(List<Line> lines, HeartCounts hearts) {
        for (int kind = 0; kind < HeartCounts.COUNT; kind++) {
            if (!hearts.has(kind)) continue;
            if (!lines.isEmpty()) {
                lines.add(new Line(SEPARATOR, SEPARATOR_COLOR));
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

    public static boolean hasArmor(int defence, int[] armor) {
        if (defence != 0) return true;
        if (armor == null) return false;
        for (int v : armor) {
            if (v != 0) return true;
        }
        return false;
    }
}
