package com.nyalium.cttenemyhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

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
        // 不再注册 GUI 元素：血条改为在「实体名字的提交点」世界空间绘制（mixin
        // NameTagHudMixin → HudWorldRenderer），因此天然带深度测试，被挡住就看不见。
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
