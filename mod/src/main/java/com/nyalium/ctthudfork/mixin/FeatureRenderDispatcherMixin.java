package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.SeeThroughQuads;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把 {@link SeeThroughQuads} 的渲染器注册进 {@code FeatureRenderDispatcher}。
 *
 * <p>原版在 {@code FeatureRenderDispatcher} 构造里把内置的 feature renderer
 * 一个个 {@code put} 进 {@code FeatureRendererMap}；我们只能等它填完再补自己的，
 * 否则 {@code PreparedFrame.prepare} 里的 {@code getOrThrow} 会因为找不到渲染器
 * 直接抛 {@code IllegalArgumentException}。</p>
 *
 * <p>只加不替换：内置渲染器一个都不动，其它 mod 的注册也不受影响
 * （{@code FeatureRendererMap.put} 会自行扩容）。</p>
 */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin {

    @Shadow
    @Final
    private FeatureRendererMap featureRenderers;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cttHud$registerSeeThroughQuads(CallbackInfo ci) {
        featureRenderers.put(SeeThroughQuads.TYPE, new SeeThroughQuads.Renderer());
        SeeThroughQuads.markRegistered();
    }
}
