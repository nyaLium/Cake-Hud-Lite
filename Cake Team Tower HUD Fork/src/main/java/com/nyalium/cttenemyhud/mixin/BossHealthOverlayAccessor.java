package com.nyalium.cttenemyhud.mixin;

import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * 读取客户端 Boss 栏的原始数据。
 *
 * <p><b>为什么能读</b>：{@code ClientboundBossEventPacket} 是逐玩家同步的，
 * 服务端建了什么 bossbar 客户端就有什么 —— 这是纯客户端唯一能拿到
 * 「地图自己算好的精确血量」的通道。数据包 {@code bossbars/targetting_enemy.mcfunction}
 * 会把 {@code 名字 (HP 当前/上限) 🛡防御 [元素护甲]} 写进 {@code minecraft:targetting}
 * 的标题里，本 Mod 就是解析这段标题。</p>
 *
 * <p>26.3 里 {@code BossBarHud} 已并入 {@link BossHealthOverlay}，字段名 {@code events}。</p>
 */
@Mixin(BossHealthOverlay.class)
public interface BossHealthOverlayAccessor {

    @Accessor("events")
    Map<UUID, BossEvent> cttEnemyHud$getBossBars();
}
