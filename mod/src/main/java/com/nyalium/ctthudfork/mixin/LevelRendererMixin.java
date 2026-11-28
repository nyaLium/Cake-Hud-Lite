package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.SeeThroughQuads;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 记录「seeThrough 这一遍到底有没有被执行」。
 *
 * <p>背景：1.6.18 起「血条不可透视」开（默认）时整组 HUD 只提交到
 * {@code SubmitNodeStorage.seeThrough()}，由 {@code LevelRenderer.executeSeeThrough}
 * 统一绘制。如果某个环境（替换了 LevelRenderer 的优化 mod、特殊渲染后端）根本不走这一遍，
 * 那整组 HUD 就等于「提交了但永不出现」。</p>
 *
 * <p>这里只在方法头打一个标记，不做任何修改；{@code SeeThroughQuads.passAvailable()} 用它
 * 决定「继续走透视层」还是「退回实心层」。{@code require = 0}：目标方法不存在时静默降级
 * （标记永远不置位 ⇒ 自动退回实心层，宁可被方块切也不能一条不画）。</p>
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "executeSeeThrough", at = @At("HEAD"), require = 0)
    private void cttHud$markSeeThroughPass(FeatureRenderDispatcher.PreparedFrame frame,
                                           RenderTarget target, CallbackInfo ci) {
        SeeThroughQuads.markPassSeen();
    }
}
