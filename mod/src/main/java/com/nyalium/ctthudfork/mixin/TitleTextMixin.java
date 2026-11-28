package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.HudConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code /title} 的<b>标题</b>与<b>副标题</b>可以整体位移、并各自调整文本不透明度。
 *
 * <p>只挂 {@code Hud.extractTitle} —— 那是原版画「title / subtitle」的唯一入口；
 * <b>actionbar 不走这里</b>（{@code extractOverlayMessage}），所以本功能天然排除 actionbar。</p>
 *
 * <h2>原版结构</h2>
 * <pre>
 * pose.translate(guiW/2, guiH/2);        // 原点挪到屏幕中心
 *   pose.pushMatrix(); pose.scale(4, 4);  // 标题：local 坐标 × 4
 *   textWithBackdrop(font, title, -w/2, -10, w, color);
 *   pose.popMatrix();
 *   pose.pushMatrix(); pose.scale(2, 2);  // 副标题：local 坐标 × 2
 *   textWithBackdrop(font, subtitle, -w/2, 5, sw, color);
 *   pose.popMatrix();
 * </pre>
 *
 * <h2>怎么叠加我们自己的变换</h2>
 * <p>两个 item 唯一的区分点就是这两次 {@code textWithBackdrop}（{@code ordinal} 0 / 1），
 * 所以每个 {@code @Redirect} 里在调用前后各 push / pop 一次矩阵，叠加：</p>
 * <pre>
 * T(位移 / 该 item 的原版倍数) · T(文字中心) · S(我们的缩放) · T(-文字中心)
 * </pre>
 * <p>两点细节：</p>
 * <ul>
 *   <li><b>位移要除以原版倍数</b>：进来时 pose 已经带了 {@code scale(4)} / {@code scale(2)}，
 *       在这个坐标系里 1 单位不再等于 1 GUI 像素，所以 GUI 像素的位移要先除掉该倍数
 *       （{@link #TITLE_POSE_SCALE} / {@link #SUBTITLE_POSE_SCALE}）。</li>
 *   <li><b>位移要在缩放之外</b>：矩阵是「后写的先作用于顶点」，把位移写在最前面，
 *       它就不会被我们自己的缩放系数再放大一次 —— 缩放改变的是字号，位移始终是屏幕像素。</li>
 * </ul>
 * <p>缩放原点取文字盒的中心（{@code x + width/2, y + }），于是放大 / 缩小时文字仍然居中，
 * 不会往某一侧歪。位移与不透明度都是对原版淡入淡出的最终结果再叠加，不影响原版节奏。</p>
 */
@Mixin(Hud.class)
public abstract class TitleTextMixin {

    /** 原版 {@code Hud.extractTitle(GuiGraphicsExtractor, DeltaTracker)} 的完整描述符。 */
    private static final String EXTRACT_TITLE =
            "extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V";

    /** 原版画标题 / 副标题用的调用。 */
    private static final String TEXT_WITH_BACKDROP =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop("
                    + "Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V";

    /** 原版标题前面的 {@code scale(4, 4)}：local 1 单位 = 4 GUI 像素。 */
    private static final float TITLE_POSE_SCALE = 4.0F;
    /** 原版副标题前面的 {@code scale(2, 2)}。 */
    private static final float SUBTITLE_POSE_SCALE = 2.0F;
    /** 文字盒中心相对 {@code y} 的偏移（字体行高 9 的一半）。 */
    private static final float TEXT_CENTER_Y = 4.5F;

    /** 原版字体行高（GUI 像素），用来算文字盒的下边界。 */
    private static final float TEXT_HEIGHT = 9.0F;

    /** 钳制位移时留出的屏幕边距（GUI 像素），贴着边不好看。 */
    private static final int SCREEN_MARGIN = 2;

    /** 标题：{@code ordinal = 0}（原版第一处 {@code textWithBackdrop}）。 */
    @Redirect(method = EXTRACT_TITLE, at = @At(value = "INVOKE", target = TEXT_WITH_BACKDROP, ordinal = 0))
    private void cttHudFork$title(GuiGraphicsExtractor graphics, Font font, Component text,
                                  int x, int y, int width, int argb) {
        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled) {
            // 总开关关掉 = 本模组完全不干预：位置 / 缩放 / 不透明度全部交还原版
            graphics.textWithBackdrop(font, text, x, y, width, argb);
            return;
        }
        int[] offset = HudConfig.titleOffset(graphics.guiWidth(), graphics.guiHeight());
        drawItem(graphics, font, text, x, y, width, argb,
                TITLE_POSE_SCALE, offset, cfg.titleOpacity, cfg.titleScale);
    }

    /** 副标题：{@code ordinal = 1}。 */
    @Redirect(method = EXTRACT_TITLE, at = @At(value = "INVOKE", target = TEXT_WITH_BACKDROP, ordinal = 1))
    private void cttHudFork$subtitle(GuiGraphicsExtractor graphics, Font font, Component text,
                                     int x, int y, int width, int argb) {
        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled) {
            graphics.textWithBackdrop(font, text, x, y, width, argb);
            return;
        }
        int[] offset = HudConfig.subtitleOffset(graphics.guiWidth(), graphics.guiHeight());
        drawItem(graphics, font, text, x, y, width, argb,
                SUBTITLE_POSE_SCALE, offset, cfg.subtitleOpacity, cfg.subtitleScale);
    }

    private static void drawItem(GuiGraphicsExtractor graphics, Font font, Component text,
                                 int x, int y, int width, int argb,
                                 float poseScale, int[] offset, double opacity, double scale) {
        float factor = poseScale <= 0.0F ? 1.0F : poseScale;
        float zoom = (float) clamp(scale, 0.25, 4.0);
        float centerX = x + width / 2.0F;
        float centerY = y + TEXT_CENTER_Y;
        int[] clamped = clampToScreen(graphics, x, y, width, factor, zoom, offset);

        graphics.pose().pushMatrix();
        // 位移：GUI 像素 → 当前 item 的 local 单位；写在最外层，不受 zoom 影响
        if (clamped[0] != 0 || clamped[1] != 0) {
            graphics.pose().translate(clamped[0] / factor, clamped[1] / factor);
        }
        // 缩放：绕文字盒中心
        if (zoom != 1.0F) {
            graphics.pose().translate(centerX, centerY);
            graphics.pose().scale(zoom, zoom);
            graphics.pose().translate(-centerX, -centerY);
        }

        graphics.textWithBackdrop(font, text, x, y, width, applyOpacity(argb, opacity));
        graphics.pose().popMatrix();
    }

    /**
     * 把位移钳制到「文字仍然完整留在屏幕内」的范围。
     *
     * <p>进 {@code drawItem} 时 pose 原点已经在屏幕中心，所以文字盒在屏幕上的中心是
     * {@code 屏幕中心 + 文字盒中心 × 原版倍数}（不含位移），半宽 / 半高再乘上我们的缩放。
     * 位移取能让四条边都留在屏幕内的区间；文字本身就比屏幕还宽时不硬塞，直接居中。</p>
     */
    private static int[] clampToScreen(GuiGraphicsExtractor graphics, int x, int y, int width,
                                       float factor, float zoom, int[] offset) {
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        float screenCenterX = guiWidth / 2.0F + (x + width / 2.0F) * factor;
        float screenCenterY = guiHeight / 2.0F + (y + TEXT_CENTER_Y) * factor;
        float halfWidth = width * factor * zoom / 2.0F;
        float halfHeight = TEXT_HEIGHT * factor * zoom / 2.0F;

        return new int[]{
                clampAxis(offset[0], screenCenterX, halfWidth, guiWidth),
                clampAxis(offset[1], screenCenterY, halfHeight, guiHeight)
        };
    }

    private static int clampAxis(int offset, float center, float half, int size) {
        float lower = SCREEN_MARGIN + half - center;
        float upper = size - SCREEN_MARGIN - half - center;

        if (lower > upper) {
            // 文字比可用区域还大：居中，别让它再被推到一边
            return 0;
        }

        return (int) Math.max(Math.ceil(lower), Math.min(Math.floor(upper), (double) offset));
    }

    /** 把颜色里已有的 alpha（原版淡入淡出算出来的）再乘一个系数。 */
    private static int applyOpacity(int argb, double opacity) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * clamp(opacity, 0.0, 1.0));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static double clamp(double value, double lo, double hi) {
        if (Double.isNaN(value)) return 1.0;
        return Math.max(lo, Math.min(hi, value));
    }
}
