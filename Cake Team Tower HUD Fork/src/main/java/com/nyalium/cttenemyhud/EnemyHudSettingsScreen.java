package com.nyalium.cttenemyhud;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 某一类设置的子界面（从 {@link EnemyHudConfigScreen} 点进来）。
 *
 * <p>左右两列排布，底部三个按钮：<b>返回</b>（回到总界面，改动保留）/
 * <b>完成</b>（写盘并关闭整套设置）/ <b>取消</b>（回滚到打开总界面时的快照并关闭）。</p>
 */
public final class EnemyHudSettingsScreen extends Screen {

    /** 三个子页面。 */
    public enum Section { GENERAL, ENEMY, PLAYER }

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 52;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_WIDTH_MAX = 210;
    private static final int BUTTON_GAP = 8;

    private final EnemyHudConfigScreen menu;
    private final Section section;

    public EnemyHudSettingsScreen(EnemyHudConfigScreen menu, Section section) {
        super(Component.translatable("ctt_enemy_hud.config.section." + id(section)));
        this.menu = menu;
        this.section = section;
    }

    private static String id(Section section) {
        return switch (section) {
            case GENERAL -> "general";
            case ENEMY -> "enemy";
            case PLAYER -> "player";
        };
    }

    @Override
    protected void init() {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int w = Math.max(96, Math.min(ROW_WIDTH_MAX, (this.width - COLUMN_GAP * 3) / 2));
        int leftX = this.width / 2 - w - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;

        switch (section) {
            case GENERAL -> initGeneral(cfg, w, leftX, rightX);
            case ENEMY -> initEnemy(cfg, w, leftX, rightX);
            case PLAYER -> initPlayer(cfg, w, leftX, rightX);
        }

        int totalWidth = w * 2 + COLUMN_GAP;
        int small = (totalWidth - BUTTON_GAP * 2) / 3;
        int buttonY = Math.min(TOP + ROW_GAP * 5 + 8, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(leftX, buttonY, small, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(leftX + small + BUTTON_GAP, buttonY, small, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> cancel())
                .bounds(leftX + (small + BUTTON_GAP) * 2, buttonY,
                        totalWidth - (small + BUTTON_GAP) * 2, ROW_HEIGHT).build());
    }

    /** 返回：改动保留，回到总界面。 */
    private void back() {
        this.minecraft.gui.setScreen(this.menu);
    }

    /** 完成：写盘并关闭整套设置。 */
    private void done() {
        EnemyHudConfig.save();
        this.minecraft.gui.setScreen(this.menu.root());
    }

    /** 取消：回滚到打开总界面时的配置并关闭（不写盘）。 */
    private void cancel() {
        EnemyHudConfig.restore(this.menu.snapshot());
        this.minecraft.gui.setScreen(this.menu.root());
    }

    // ── 通用设置 ──

    private void initGeneral(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
                .create(leftX, y, w, ROW_HEIGHT, key("enabled"), (b, v) -> EnemyHudConfig.get().enabled = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.textShadow)
                .create(leftX, y, w, ROW_HEIGHT, key("textShadow"),
                        (b, v) -> EnemyHudConfig.get().textShadow = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBelowNameScore)
                .create(leftX, y, w, ROW_HEIGHT, key("hideBelowNameScore"),
                        (b, v) -> EnemyHudConfig.get().hideBelowNameScore = v));

        y = TOP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                50.0, 100.0, 1.0, cfg.opacity * 100.0,
                v -> key("opacity", Math.round(v) + "%"),
                v -> EnemyHudConfig.get().opacity = v / 100.0));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBehindWalls)
                .create(rightX, y, w, ROW_HEIGHT, key("hideBehindWalls"),
                        (b, v) -> EnemyHudConfig.get().hideBehindWalls = v));
    }

    // ── 敌人设置 ──

    private void initEnemy(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showBar)
                .create(leftX, y, w, ROW_HEIGHT, key("showBar"), (b, v) -> EnemyHudConfig.get().showBar = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showNumbers)
                .create(leftX, y, w, ROW_HEIGHT, key("showNumbers"),
                        (b, v) -> EnemyHudConfig.get().showNumbers = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showHeartCounts)
                .create(leftX, y, w, ROW_HEIGHT, key("showHeartCounts"),
                        (b, v) -> EnemyHudConfig.get().showHeartCounts = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showArmor)
                .create(leftX, y, w, ROW_HEIGHT, key("showArmor"),
                        (b, v) -> EnemyHudConfig.get().showArmor = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideEnemyBossBar)
                .create(leftX, y, w, ROW_HEIGHT, key("hideEnemyBossBar"),
                        (b, v) -> EnemyHudConfig.get().hideEnemyBossBar = v));

        y = TOP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                50.0, 150.0, 1.0, cfg.barWidth,
                v -> key("barWidth", (int) Math.round(v)),
                v -> EnemyHudConfig.get().barWidth = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                0.75, 1.5, 0.05, cfg.scale,
                v -> key("scale", String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().scale = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                -30.0, 0.0, 1.0, cfg.verticalOffset,
                v -> key("verticalOffset", signed((int) Math.round(v))),
                v -> EnemyHudConfig.get().verticalOffset = (int) Math.round(v)));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                4.0, 128.0, 4.0, cfg.maxDistance,
                v -> key("maxDistance", (int) Math.round(v)),
                v -> EnemyHudConfig.get().maxDistance = v));
    }

    // ── 玩家设置 ──

    private void initPlayer(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showTeammates)
                .create(leftX, y, w, ROW_HEIGHT, key("showTeammates"),
                        (b, v) -> EnemyHudConfig.get().showTeammates = v));
        y += ROW_GAP;
        // 顶部那条队伍血量 BossBar：配置里是「是否隐藏」，界面上按需求显示成「是否显示」。
        addRenderableWidget(CycleButton.onOffBuilder(!cfg.hideTeamBossBar)
                .create(leftX, y, w, ROW_HEIGHT, key("teamBossBar"),
                        (b, v) -> EnemyHudConfig.get().hideTeamBossBar = !v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                4.0, 128.0, 4.0, cfg.teamMaxDistance,
                v -> key("maxDistance", (int) Math.round(v)),
                v -> EnemyHudConfig.get().teamMaxDistance = v));

        y = TOP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                50.0, 150.0, 1.0, cfg.teamBarWidth,
                v -> key("barWidth", (int) Math.round(v)),
                v -> EnemyHudConfig.get().teamBarWidth = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                0.75, 1.5, 0.05, cfg.teamScale,
                v -> key("scale", String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().teamScale = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                -30.0, 0.0, 1.0, cfg.teamVerticalOffset,
                v -> key("verticalOffset", signed((int) Math.round(v))),
                v -> EnemyHudConfig.get().teamVerticalOffset = (int) Math.round(v)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.title"),
                this.width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, 28, 0xFFFFD060);
    }

    @Override
    public void onClose() {
        back();
    }

    private static Component key(String path) {
        return Component.translatable("ctt_enemy_hud.config." + path);
    }

    private static Component key(String path, Object arg) {
        return Component.translatable("ctt_enemy_hud.config." + path, arg);
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    /** 带数值标签的滑块。 */
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
            if (this.label == null) return;
            setMessage(this.label.apply(currentValue()));
        }

        @Override
        protected void applyValue() {
            if (this.onChange == null) return;
            double value = currentValue();
            this.value = (value - this.min) / (this.max - this.min);
            this.onChange.accept(value);
            updateMessage();
        }
    }
}
