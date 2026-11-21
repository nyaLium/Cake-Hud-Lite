package com.nyalium.cttenemyhud.mixin;

import com.nyalium.cttenemyhud.EnemyHudConfig;
import com.nyalium.cttenemyhud.HudTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把「原版名称 + below_name 分数」从本 Mod 接管的实体上摘掉。
 *
 * <h2>为什么要摘分数</h2>
 * <p>数据包把 {@code HealthPercent} 挂在 {@code below_name} 上，血量信息已经由血条表达，
 * 分数是重复的。顺便它也决定了名字的位置：{@code submitNameDisplay} 只有在
 * {@code scoreText != null} 时才把名字整体上抬 {@code 9 × 1.15 × 0.025} 格，
 * 分数清掉后原版名字会自己落回 {@code y ∈ [0, 9]}（见 {@code EnemyHudLayout} 的坐标系说明）。</p>
 *
 * <h2>为什么要摘名称</h2>
 * <p>按需求名称改由本 Mod 绘制（{@code EnemyHudElement#drawName}）：字形、颜色、阴影、
 * 原版底框全部照抄 {@code submitNameTag}，不受透明度 / 缩放 / 垂直偏移影响。
 * 判定条件与画血条的条件共用 {@link HudTarget#visible}，两边绝不会一边清一边不画。</p>
 */
@Mixin(EntityRenderer.class)
public abstract class NameTagScoreMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cttEnemyHud$hideVanillaNameTag(Entity entity, EntityRenderState state, float partialTick,
                                                CallbackInfo ci) {
        if (state == null || entity == null) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled) return;
        if (!cfg.hideBelowNameScore && !cfg.hideVanillaName) return;

        Minecraft mc = Minecraft.getInstance();
        // 分数：只要这只实体归本 Mod 管就摘（数据包的百分比与血条重复，远近都不必留）。
        if (cfg.hideBelowNameScore && HudTarget.managed(mc, entity)) {
            state.scoreText = null;
        }
        // 名称：只有这一帧确实会自绘时才摘，保证任何时刻都有名字可看。
        if (cfg.hideVanillaName && HudTarget.visible(mc, entity)) {
            state.nameTag = null;
        }
    }
}
