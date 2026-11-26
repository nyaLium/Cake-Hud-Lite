package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.HudConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.6.1：{@code /title} 的<b>标题</b>与<b>副标题</b>可以整体位移、并各自调整文本不透明度。
 *
 * <p>只挂 {@code Hud.extractTitle} —— 那是原版画「title / subtitle」的唯一入口；
 * <b>actionbar 不走这里</b>（{@code extractOverlayMessage}），所以本功能天然排除 actionbar。</p>
 *
 * <h2>实现</h2>
 * <ul>
 *   <li><b>位置</b>：{@code @Inject} 在 HEAD 推一层矩阵并 translate，RETURN 再 pop。
 *       原版进方法后自己会 translate 到屏幕中心、再按 4x / 2x 缩放画字，我们的外层平移
 *       因此是「整块标题」的 GUI 像素位移（正 = 右下）。方法里所有 return 都会经过 RETURN，
 *       压栈不会失衡。</li>
 *   <li><b>不透明度</b>：原版两处 {@code textWithBackdrop(font, text, x, y, w, color)} 用的是
 *       同一个 {@code ARGB.white(alpha)} 颜色（带淡入淡出），所以用两个 {@code @Redirect}
 *       （{@code ordinal = 0} 标题 / {@code ordinal = 1} 副标题）把颜色里的 alpha 乘上去 ——
 *       不影响原版的淡入淡出节奏，只是在这个基础上再乘一个系数。</li>
 * </ul>
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

    @Inject(method = EXTRACT_TITLE, at = @At("HEAD"))
    private void cttHudFork$titleOffsetPush(GuiGraphicsExtractor graphics, DeltaTracker delta,
                                            CallbackInfo ci) {
        graphics.pose().pushMatrix();
        // 位移按窗口比例换算（配置里存的是参考 GUI 尺寸下的像素），
        // 这样改窗口大小 / GUI Scale 时标题的相对位置不变，不会被顶出屏幕。
        int[] offset = HudConfig.titleOffset(graphics.guiWidth(), graphics.guiHeight());
        if (offset[0] != 0 || offset[1] != 0) {
            graphics.pose().translate((float) offset[0], (float) offset[1]);
        }
    }

    @Inject(method = EXTRACT_TITLE, at = @At("RETURN"))
    private void cttHudFork$titleOffsetPop(GuiGraphicsExtractor graphics, DeltaTracker delta,
                                           CallbackInfo ci) {
        graphics.pose().popMatrix();
    }

    /** 标题：ordinal = 0（原版里第一处 textWithBackdrop 就是 title）。 */
    @Redirect(method = EXTRACT_TITLE, at = @At(value = "INVOKE", target = TEXT_WITH_BACKDROP, ordinal = 0))
    private void cttHudFork$titleOpacity(GuiGraphicsExtractor graphics, Font font, Component text,
                                         int x, int y, int width, int argb) {
        graphics.textWithBackdrop(font, text, x, y, width,
                applyOpacity(argb, HudConfig.get().titleOpacity));
    }

    /** 副标题：ordinal = 1。 */
    @Redirect(method = EXTRACT_TITLE, at = @At(value = "INVOKE", target = TEXT_WITH_BACKDROP, ordinal = 1))
    private void cttHudFork$subtitleOpacity(GuiGraphicsExtractor graphics, Font font, Component text,
                                            int x, int y, int width, int argb) {
        graphics.textWithBackdrop(font, text, x, y, width,
                applyOpacity(argb, HudConfig.get().subtitleOpacity));
    }

    /** 把颜色里已有的 alpha（原版淡入淡出算出来的）再乘一个系数。 */
    private static int applyOpacity(int argb, double opacity) {
        double o = Double.isNaN(opacity) ? 1.0 : Math.max(0.0, Math.min(1.0, opacity));
        int base = (argb >>> 24) & 0xFF;
        int alpha = (int) Math.round(base * o);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
