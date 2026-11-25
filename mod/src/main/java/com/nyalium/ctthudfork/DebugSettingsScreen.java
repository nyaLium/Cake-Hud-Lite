package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 「调试功能」（1.4.5）：把排查用的特殊开关集中到一页，正常游玩保持默认即可。
 *
 * <p>总界面的「调试模式」开关改成这个入口按钮；点进来是下面这张表。</p>
 *
 * <h2>行 ↔ 配置字段</h2>
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th><th>作用</th></tr>
 *   <tr><td>优先使用服务端数据</td><td>{@code preferServerData}</td><td>默认开</td>
 *       <td>关掉后完全忽略 payload，强制走 Boss 栏 + HealthPercent 的纯客户端回退</td></tr>
 *   <tr><td>筛选实体数量</td><td>{@code payloadLimit}</td><td>1 ~ {@link MobHealthPayload#MAX_ENTRIES}，默认 64</td>
 *       <td>客户端只保留 payload 的前 N 条（服务端已按「玩家优先、其余按距离」排好序）</td></tr>
 *   <tr><td>隐藏名称下方分数</td><td>{@code hideBelowNameScore}</td><td>默认开</td><td>1.4.7 从「血条设置」挪进来</td></tr>
 *   <tr><td>隐藏上方 Boss 栏（最近敌人）</td><td>{@code hideEnemyBossBar}</td><td>默认开</td><td>同上</td></tr>
 *   <tr><td>隐藏上方 Boss 栏（队伍血量）</td><td>{@code hideTeamBossBar}</td><td>默认开</td><td>同上（数据照常读取）</td></tr>
 * </table>
 *
 * <p>「优先使用服务端数据」关掉时，「筛选实体数量」灰掉 —— 都不读 payload 了，条数没有意义。</p>
 */
public final class DebugSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 60;
    private static final int WIDTH = 260;

    private final HudConfigScreen menu;

    public DebugSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.debugTitle"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;
        HudConfig.Cfg cfg = HudConfig.get();

        // 优先使用服务端数据：preferServerData（关掉后强制纯客户端回退，方便双端对比）
        addRenderableWidget(CycleButton.onOffBuilder(cfg.preferServerData)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.preferServerData"),
                        (b, v) -> {
                            HudConfig.get().preferServerData = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        // 筛选实体数量：payloadLimit（1 ~ 单包上限；不读服务端数据时没有意义，灰掉）
        AbstractSliderButton limit = new ValueSlider(x, y, width, ROW_HEIGHT,
                1, MobHealthPayload.MAX_ENTRIES, 1, HudConfig.get().payloadLimit,
                v -> Component.translatable("ctt_hud_fork.config.payloadLimit",
                        (int) Math.round(v)),
                v -> HudConfig.get().payloadLimit = (int) Math.round(v));
        limit.active = cfg.preferServerData;
        addRenderableWidget(limit);
        y += ROW_GAP;

        // 1.4.7：原来在「血条设置 → 通用」里的三个隐藏开关，挪到调试功能页。
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBelowNameScore)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.hideBelowNameScore"),
                        (b, v) -> HudConfig.get().hideBelowNameScore = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideEnemyBossBar)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.hideEnemyBossBar"),
                        (b, v) -> HudConfig.get().hideEnemyBossBar = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideTeamBossBar)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.hideTeamBossBar"),
                        (b, v) -> HudConfig.get().hideTeamBossBar = v));

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetDebug();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private void back() {
        this.minecraft.gui.setScreen(this.menu);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.debugHint"),
                this.width / 2, 40, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        back();
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<Component> label;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int width, int height, double min, double max, double step, double initial,
                    DoubleFunction<Component> label, DoubleConsumer onChange) {
            super(x, y, width, height, Component.empty(), (initial - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        private double currentValue() {
            double raw = this.min + (this.max - this.min) * this.value;
            double snapped = Math.round(raw / this.step) * this.step;
            return Math.max(this.min, Math.min(this.max, snapped));
        }

        @Override
        protected void updateMessage() {
            if (this.label != null) setMessage(this.label.apply(currentValue()));
        }

        @Override
        protected void applyValue() {
            if (this.onChange != null) this.onChange.accept(currentValue());
        }
    }
}
