package com.nyalium.cttenemyhud;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯函数布局计算：<b>不引用任何 Minecraft 类</b>，因此可以在普通 JVM 里穷举测试
 * 每一个配置组合（见 {@code src/test/java/.../SelfCheck.java}）。
 *
 * <h2>坐标系</h2>
 * <p>「原版名牌像素」：原点 = 原版名牌锚点，y 向下为正。
 * 原版 {@code submitNameDisplay} 把名字画在锚点上方 {@code 9 × 1.15 × 0.025} 格 = 10.35 像素处，
 * 名字自身高 9 像素，所以名字行占 {@code y ∈ [-10.35, -1.35]}；
 * 分数行（below_name）占 {@code y ∈ [0, 9]}。
 * 本 Mod 的两行必须在 -10.35 之上。</p>
 */
public final class EnemyHudLayout {

    /** 文字行顶部（布局像素）。 */
    public static final int TEXT_TOP = -29;
    /** 血条顶部（布局像素）。 */
    public static final int BAR_TOP = -18;
    /** 血条高度 = 原版 bossbar 纹理高度。 */
    public static final int BAR_HEIGHT = 5;

    /** 血条长度的允许范围与默认值（原版名牌像素）。 */
    public static final int BAR_WIDTH_MIN = 50;
    public static final int BAR_WIDTH_MAX = 150;
    public static final int BAR_WIDTH_DEFAULT = 100;

    /** 文字宽度测量器（游戏里是真 Font，测试里可以是假实现）。 */
    public interface WidthMeasurer {
        int width(String text);
    }

    /** 一帧布局需要的开关（与 {@link EnemyHudConfig.Cfg} 解耦，便于测试）。 */
    public static final class Options {
        public boolean showNumbers = true;
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
     * @param hp         当前血量
     * @param maxHp      最大血量
     * @param percentOnly true = 只有百分比，画 "73%"
     * @param defence    防御值，0 = 不画
     * @param armor      七档元素护甲，0 = 不画
     */
    public static Result compute(int hp, int maxHp, boolean percentOnly,
                                 int defence, int[] armor,
                                 Options options, WidthMeasurer measurer) {
        Options opt = options != null ? options : new Options();
        WidthMeasurer w = measurer != null ? measurer : String::length;

        List<Line> lines = new ArrayList<>(9);

        if (opt.showNumbers) {
            String hpText = percentOnly ? Math.max(0, hp) + "%" : Math.max(0, hp) + "/" + Math.max(0, maxHp);
            lines.add(new Line(hpText, 0xFFFFFF));
        }

        if (opt.showArmor && hasArmor(defence, armor)) {
            if (!lines.isEmpty()) {
                lines.add(new Line("  \u2014  ", 0x9E9E9E));
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
