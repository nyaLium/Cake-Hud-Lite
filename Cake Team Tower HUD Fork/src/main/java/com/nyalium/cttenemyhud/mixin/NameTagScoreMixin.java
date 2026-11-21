package com.nyalium.cttenemyhud.mixin;

import com.nyalium.cttenemyhud.EnemyHudConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏实体名称下方那条计分板分数（数据包把 {@code HealthPercent} 挂在 {@code below_name} 上）。
 *
 * <p>{@code EntityRenderer.submitNameDisplay} 会先画 {@code scoreText}，再把名字整体上抬
 * {@code 9 × 1.15 × 0.025} 格；只清空 {@code scoreText} 的话名字会留在原来的高处。
 * 所以这里用两步：</p>
 * <ol>
 *   <li>{@code extractRenderState} 收尾时把 {@code scoreText} 置空（每帧重建，安全）；</li>
 *   <li>{@link NameTagOffsetMixin} 在 {@code submitNameDisplay} 里把那次上抬改成 0，
 *       名字于是落回原位。</li>
 * </ol>
 */
@Mixin(EntityRenderer.class)
public abstract class NameTagScoreMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cttEnemyHud$hideBelowNameScore(Entity entity, EntityRenderState state, float partialTick,
                                                CallbackInfo ci) {
        if (!EnemyHudConfig.get().hideBelowNameScore) return;
        if (state != null) {
            state.scoreText = null;
        }
    }
}
