package com.nyalium.cttenemyhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * 客户端入口（Mod 名：<b>CTT HUD - Fork</b>，mod id {@code ctt-hud-fork}；类名沿用早期的 CTT Enemy HUD）。
 *
 * <p><b>客户端必装、服务端选装</b>：所有基础数据都从客户端可见的渠道取
 * （计分板展示槽、原版 Boss 栏标题 / 颜色 / 进度、实体、玩家列表），服务端<b>不需要</b>安装任何东西。
 * 服务端若额外装了本 Mod，会通过 {@link MobHealthPayload} 推来按 UUID 的权威数据，
 * 本端自动优先使用（{@code useServerData}）。</p>
 *
 * <p>1.20.3 起多了<b>自身属性面板</b>（{@link SelfStats}）：它同样纯客户端——
 * 定时 {@code /trigger ViewStats} 问地图，再把地图回的属性行画成面板。
 * 1.20.4 起面板支持内容过滤 / 拖动定位，并可用 {@code F8} 直接打开设置。</p>
 */
public final class CttEnemyHudClient implements ClientModInitializer {

    public static final String MOD_ID = "ctt-hud-fork";

    /** 游戏内打开设置界面的快捷键（默认 F8；没装 Mod Menu 也能用）。 */
    private static KeyMapping openConfig;

    @Override
    public void onInitializeClient() {
        EnemyHudConfig.load();
        // 血条：在「实体名字的提交点」世界空间绘制（mixin NameTagHudMixin → HudWorldRenderer），
        // 天然带深度测试，被挡住就看不见。
        // 自身属性面板：唯一一个 GUI 元素，挂在聊天栏之前，免得压住聊天。
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "self_stats"),
                new SelfStatsPanel());
        // 盾牌计量条（1.20.19 合并自 CTT Shield Indicator）。
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "shield_indicator"),
                new ShieldIndicatorHud());
        // 每 tick 检查是否该刷新属性面板（内部自己节流），顺便处理设置快捷键。
        ClientTickEvents.END_CLIENT_TICK.register(SelfStats::tick);
        ClientTickEvents.END_CLIENT_TICK.register(CttEnemyHudClient::tickKeys);
        openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ctt_enemy_hud.open_config",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F8,
                KeyMapping.Category.MISC));
        // 聊天过滤：
        // ① 本 Mod 自发 /trigger 的回显（成功回显 + 「无法触发该计分项」）→ 吞掉；
        // ② 本次 ViewStats 的属性行 → 吞掉（不进聊天栏，只进面板）。
        // 注意 ALLOW_GAME 的返回值语义：true = 允许显示，false = 拦掉。
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (SelfStats.onGameMessage(message, overlay, System.currentTimeMillis())) return false;
            return !ChatTriggerFilter.shouldHide(message, overlay);
        });
        // 服务端选装组件：接收按 UUID 的怪物权威数据。
        ClientMobHealthCache.register();
        // 断线 / 换服清缓存，避免看到上一局的残留数据。
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientMobHealthCache.reset();
            SelfStats.reset();
        });
    }

    private static void tickKeys(Minecraft mc) {
        if (mc == null || openConfig == null) return;
        while (openConfig.consumeClick()) {
            if (mc.gui.screen() == null) {
                mc.gui.setScreen(new EnemyHudConfigScreen(null));
            }
        }
    }
}
