package com.nyalium.cttenemyhud.mixin;

import com.nyalium.cttenemyhud.BossBarTitle;
import com.nyalium.cttenemyhud.EnemyHudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 隐藏「数据包给最近敌人做的那条 Boss 条」。
 *
 * <p>那套数值已经画在敌人头顶了，屏幕顶部再挂一条属于重复信息；默认隐藏，可开关。</p>
 *
 * <p>实现上不改 {@code events} 这个 Map：只在 {@code extractRenderState} 取迭代源时
 * （{@code Map.values()} 那一次调用）返回过滤后的快照，原 Map 永不被修改，
 * 数据通道 {@link com.nyalium.cttenemyhud.BossBarTracker} 照常能读到。</p>
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossBarHudRenderMixin {

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;values()Ljava/util/Collection;"
            )
    )
    private Collection<BossEvent> cttEnemyHud$filterEnemyBars(Map<UUID, BossEvent> map) {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.hideEnemyBossBar) {
            return map.values();
        }

        String playerName = "";
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            playerName = mc.player.getName().getString();
        }

        List<BossEvent> kept = new ArrayList<>(map.size());
        boolean anyHidden = false;
        for (BossEvent bar : map.values()) {
            if (bar == null) continue;
            if (BossBarTitle.isEnemyBarText(bar.getName().getString(), playerName)) {
                anyHidden = true;
                continue;
            }
            kept.add(bar);
        }
        return anyHidden ? kept : map.values();
    }
}
