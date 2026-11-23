package com.nyalium.cttenemyhud.mixin;

import com.nyalium.cttenemyhud.SelfStatsPanelAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
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
 * 并使用<b>容器那一套</b>位置 / 列数 / 缩放 / 不透明度（见 {@code EnemyHudConfig.panel(mode, true)}）。</p>
 *
 * <p>仅为客户端 mixin，见 {@code ctt_hud_fork.mixins.json}。</p>
 */
@Mixin(Screen.class)
public abstract class ContainerPanelMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cttEnemyHud$drawSelfStatsOnContainer(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                      float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?>) {
            SelfStatsPanelAccess.drawOnContainer(graphics, mouseX, mouseY);
        }
    }
}
