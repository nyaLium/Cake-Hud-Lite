package com.nyalium.cttenemyhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/**
 * CTT Enemy HUD · 纯客户端入口。
 *
 * <p><b>客户端必装、服务端选装</b>：所有数据都从客户端可见的渠道取
 * （计分板展示槽、原版 Boss 栏标题 / 颜色 / 进度、实体、玩家列表），服务端<b>不需要</b>安装任何东西。</p>
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
        // 吞掉本 Mod 自发 /trigger 的聊天回显（成功回显 + 「无法触发该计分项」）。
        ClientReceiveMessageEvents.ALLOW_GAME.register(ChatTriggerFilter::shouldHide);
    }
}
