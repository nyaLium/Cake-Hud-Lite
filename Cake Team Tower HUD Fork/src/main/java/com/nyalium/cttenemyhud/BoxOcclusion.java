package com.nyalium.cttenemyhud;

import java.util.List;

/**
 * 屏幕空间遮挡判定：一只怪在画面上被「更近的实体」挡住了多少。
 *
 * <p>血条画在 GUI 层，天然盖在世界里的模型与名牌之上；不做处理的话，
 * 站在后面的怪的血条会压住前面怪的模型和名字。这里用投影后的矩形做粗略覆盖率：
 * 被更近的实体盖住超过阈值就当作「看不见」，不画它头顶的 HUD。</p>
 *
 * <p>纯几何、无 Minecraft 依赖，可在普通 JVM 里穷举测试。</p>
 */
public final class BoxOcclusion {

    /** 默认阈值：被更近的实体盖住 70% 以上就不画。 */
    public static final double DEFAULT_THRESHOLD = 0.70;

    private BoxOcclusion() {}

    /**
     * 覆盖比例（0 ~ 1）。
     *
     * @param self   自身矩形 {@code {x0, y0, x1, y1}}（GUI 像素）
     * @param nearer 更近的实体矩形，允许为空
     */
    public static double coverage(double[] self, List<double[]> nearer) {
        if (self == null || nearer == null || nearer.isEmpty()) return 0.0;
        double w = self[2] - self[0];
        double h = self[3] - self[1];
        double area = w * h;
        if (!(area > 1.0e-6)) return 0.0;

        double covered = 0.0;
        for (double[] other : nearer) {
            if (other == null) continue;
            double ix = Math.min(self[2], other[2]) - Math.max(self[0], other[0]);
            double iy = Math.min(self[3], other[3]) - Math.max(self[1], other[1]);
            if (ix > 0.0 && iy > 0.0) covered += ix * iy;
        }
        return Math.min(1.0, covered / area);
    }

    /** 是否已经「看不见」：覆盖比例达到阈值。 */
    public static boolean hidden(double[] self, List<double[]> nearer, double threshold) {
        double limit = threshold <= 0.0 || threshold > 1.0 ? DEFAULT_THRESHOLD : threshold;
        return coverage(self, nearer) >= limit;
    }
}
