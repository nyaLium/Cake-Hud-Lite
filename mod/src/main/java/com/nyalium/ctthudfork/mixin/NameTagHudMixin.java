package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.HudWorldRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把头顶血条挂到原版名字的提交点上。
 *
 * <p>{@code EntityRenderer.submitNameDisplay} 正是原版提交名牌的地方：此时 pose 的原点
 * 就在实体脚下、相机空间里，{@code submitNameTag} 内部再做
 * 「平移挂点 → 转向相机 → 缩放 0.025」三步。在这里提交，血条与名字共享<b>同一套
 * 深度测试</b>，于是被墙、被更近的模型挡住时的表现与原版名字逐像素一致。</p>
 */
@Mixin(EntityRenderer.class)
public abstract class NameTagHudMixin {

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("TAIL"))
    private void cttHud$submitHud(EntityRenderState state, PoseStack pose,
                                  SubmitNodeCollector collector, CameraRenderState camera,
                                  int offset, CallbackInfo ci) {
        HudWorldRenderer.submit(state, pose, collector, camera);
    }
}
