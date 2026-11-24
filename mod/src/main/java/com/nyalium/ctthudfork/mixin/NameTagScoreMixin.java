package com.nyalium.ctthudfork.mixin;

import com.nyalium.ctthudfork.HudConfig;
import com.nyalium.ctthudfork.HudRenderBridge;
import com.nyalium.ctthudfork.HudTarget;
import net.minecraft.client.Minecraft;
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
 * <p>血量信息已经由头顶血条表达，分数是重复的。顺带它决定名字的位置：
 * {@code EntityRenderer.submitNameDisplay} 的那次上抬 {@code 9 × 1.15 × 0.025} 写在
 * {@code if (scoreText != null)} 里面，所以分数一清空，原版名字就不再上抬，
 * 自动落回 {@code y ∈ [0, 9]}（即「分数原本占的那一行」）—— 这正是需要的位置，
 * 本 Mod 无需自己画名字。</p>
 *
 * <p>判定用 {@link HudTarget#managed}：数据包真正管控的实体（敌人 / 队友）才摘，
 * 其它实体（掉落物、船、画、Boss 条无关实体）一律不动。诅咒期间整块 UI 隐藏，`managed`
 * 也会返回 false（名字交还原版）。</p>
 */
@Mixin(EntityRenderer.class)
public abstract class NameTagScoreMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cttHudFork$hideBelowNameScore(Entity entity, EntityRenderState state, float partialTick,
                                                CallbackInfo ci) {
        if (state == null || entity == null) return;
        // 无论摘不摘分数，都要把实体挂到渲染状态上：submit 阶段只拿得到状态，
        // 而头顶血条要按实体 UUID 取数据（见 HudRenderBridge）。
        HudRenderBridge.attach(state, entity);
        if (!HudConfig.get().hideBelowNameScore) return;
        if (HudTarget.managed(Minecraft.getInstance(), entity)) {
            state.scoreText = null;
        }
    }
}
