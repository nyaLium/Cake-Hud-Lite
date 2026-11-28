package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.SelfStatsPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「在容器界面显示」模式：把属性面板画在容器界面（箱子 / 背包 / 商店…）之上。
 *
 * <p>属性面板本身是 HUD 元素，打开任何 Screen 时 HUD 就不再绘制；这个 mixin 挂在
 * {@code Screen.extractRenderState} 的末尾，只在 {@link AbstractContainerScreen} 上生效，
 * 并使用<b>容器那一套</b>位置 / 列数 / 不透明度（见 {@code HudConfig.panel(mode, true, guiW, guiH)}）。</p>
 *
 * <p>仅为客户端 mixin，见 {@code ctt_hud_fork.mixins.json}。</p>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerPanelMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cttHudFork$drawSelfStatsOnContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                      float partialTick, CallbackInfo ci) {
        // 直接挂 AbstractContainerScreen：容器界面的 extractRenderState 会先调 extractContents
        // 画槽位 / 物品名 / 高亮，TAIL 才是「这些都画完之后」—— 面板才真的在它们之上。
        // （以前挂在 Screen 上，对容器界面而言落点在 extractContents 的开头，面板会被物品压住。）
        SelfStatsPanel.drawOnContainer(graphics, mouseX, mouseY);
    }
}
