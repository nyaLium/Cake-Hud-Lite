package com.nyalium.cttenemyhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/**
 * CTT Enemy HUD · 纯客户端入口。
 *
 * <p>功能只在客户端读取计分板 / 实体数据并绘制 HUD，服务端不需要安装任何东西。</p>
 */
public final class CttEnemyHudClient implements ClientModInitializer {

    public static final String MOD_ID = "ctt-hud-fork";

    @Override
    public void onInitializeClient() {
        EnemyHudConfig.load();
        // addLast：排在原版与其它 Mod 的全部 HUD 元素之后，敌人血条永远在最上层。
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "enemy_hud"),
                new EnemyHudElement());
    }
}
