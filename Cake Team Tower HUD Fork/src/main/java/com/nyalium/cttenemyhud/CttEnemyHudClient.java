package com.nyalium.cttenemyhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/**
 * CTT Enemy HUD · 客户端入口。
 *
 * <p><b>客户端必装、服务端选装</b>：所有基础数据都从客户端可见的渠道取
 * （计分板展示槽、原版 Boss 栏标题 / 颜色 / 进度、实体、玩家列表），服务端<b>不需要</b>安装任何东西。
 * 服务端若额外装了本 Mod，会通过 {@link MobHealthPayload} 推来按 UUID 的权威数据，
 * 本端自动优先使用（{@code useServerData}）。</p>
 */
public final class CttEnemyHudClient implements ClientModInitializer {

    public static final String MOD_ID = "ctt-hud-fork";

    @Override
    public void onInitializeClient() {
        EnemyHudConfig.load();
        // 画在「提示信息」之下：OVERLAY_MESSAGE（动作栏）、标题、聊天、玩家列表都在它之后，
        // 所以血条不会盖住这些 UI 文字；同时它仍在世界渲染之后，所以压得住模型与名牌。
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath(MOD_ID, "enemy_hud"),
                new EnemyHudElement());
        // 吞掉本 Mod 自发 /trigger 的聊天回显（成功回显 + 「无法触发该计分项」）。
        // 注意 ALLOW_GAME 的返回值语义：true = 允许显示，false = 拦掉。
        ClientReceiveMessageEvents.ALLOW_GAME.register(
                (message, overlay) -> !ChatTriggerFilter.shouldHide(message, overlay));
        // 服务端选装组件：接收按 UUID 的怪物权威数据。
        ClientMobHealthCache.register();
        // 断线 / 换服清缓存，避免看到上一局的残留数据。
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientMobHealthCache.reset());
    }
}
