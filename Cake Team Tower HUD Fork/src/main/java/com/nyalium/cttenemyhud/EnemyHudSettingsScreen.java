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
 * <p>左右两列排布，底部只有「返回」（回到总界面，改动保留；写盘由总界面的「完成」负责）。</p>
 */
public final class EnemyHudSettingsScreen extends Screen {

    /** 四个子页面。 */
    public enum Section { GENERAL, ENEMY, PLAYER, SELF_STATS }

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_WIDTH_MAX = 210;

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
            case SELF_STATS -> "self_stats";
        };
    }

    @Override
    protected void init() {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int w = Math.max(96, Math.min(ROW_WIDTH_MAX, (this.width - COLUMN_GAP * 3) / 2));
        int leftX = this.width / 2 - w - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;

        int rows = switch (section) {
            case GENERAL -> initGeneral(cfg, w, leftX, rightX);
            case ENEMY -> initEnemy(cfg, w, leftX, rightX);
            case PLAYER -> initPlayer(cfg, w, leftX, rightX);
            case SELF_STATS -> initSelfStats(cfg, w, leftX, rightX);
        };

        int totalWidth = w * 2 + COLUMN_GAP;
        // 「返回」必须落在最长那一列的下方，否则会压住最后一个开关。
        int contentBottom = TOP + ROW_GAP * (Math.max(1, rows) - 1) + ROW_HEIGHT;
        int buttonY = Math.min(contentBottom + 12, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(leftX + totalWidth / 2 - 60, buttonY, 120, ROW_HEIGHT).build());
    }

    /** 返回：改动保留，回到总界面。 */
    private void back() {
        this.minecraft.gui.setScreen(this.menu);
    }

    // ── 通用设置 ──

    /** @return 占用的行数（左列最多 3，右列最多 4） */
    private int initGeneral(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
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
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                50.0, 100.0, 1.0, cfg.opacity * 100.0,
                v -> key("opacity", Math.round(v) + "%"),
                v -> EnemyHudConfig.get().opacity = v / 100.0));

        y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBehindWalls)
                .create(rightX, y, w, ROW_HEIGHT, key("hideBehindWalls"),
                        (b, v) -> EnemyHudConfig.get().hideBehindWalls = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBehindEntities)
                .create(rightX, y, w, ROW_HEIGHT, key("hideBehindEntities"),
                        (b, v) -> EnemyHudConfig.get().hideBehindEntities = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideEnemyBossBar)
                .create(rightX, y, w, ROW_HEIGHT, key("hideEnemyBossBar"),
                        (b, v) -> EnemyHudConfig.get().hideEnemyBossBar = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideTeamBossBar)
                .create(rightX, y, w, ROW_HEIGHT, key("hideTeamBossBar"),
                        (b, v) -> EnemyHudConfig.get().hideTeamBossBar = v));
        return 4;
    }

    // ── 敌人设置 ──

    /** @return 占用的行数 */
    private int initEnemy(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showBar)
                .create(leftX, y, w, ROW_HEIGHT, key("showBar"), (b, v) -> EnemyHudConfig.get().showBar = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                4.0, 128.0, 4.0, cfg.maxDistance,
                v -> key("maxDistance", (int) Math.round(v)),
                v -> EnemyHudConfig.get().maxDistance = v));

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
        addRenderableWidget(CycleButton.onOffBuilder(cfg.useServerData)
                .create(rightX, y, w, ROW_HEIGHT, key("useServerData"),
                        (b, v) -> EnemyHudConfig.get().useServerData = v));
        return 4;
    }

    // ── 玩家设置 ──

    /** @return 占用的行数 */
    private int initPlayer(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showTeammates)
                .create(leftX, y, w, ROW_HEIGHT, key("showTeammates"),
                        (b, v) -> EnemyHudConfig.get().showTeammates = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.autoPartyBossbar)
                .create(leftX, y, w, ROW_HEIGHT, key("autoPartyBossbar"),
                        (b, v) -> EnemyHudConfig.get().autoPartyBossbar = v));
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
        return 3;
    }

    // ── 自身属性面板设置 ──

    /** @return 占用的行数 */
    private int initSelfStats(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showSelfStats)
                .create(leftX, y, w, ROW_HEIGHT, key("showSelfStats"),
                        (b, v) -> EnemyHudConfig.get().showSelfStats = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                0.0, 60.0, 1.0, cfg.selfStatsSeconds,
                v -> key("selfStatsSeconds", (int) Math.round(v)),
                v -> EnemyHudConfig.get().selfStatsSeconds = (int) Math.round(v)));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                1.0, 3.0, 1.0, cfg.selfStatsColumns,
                v -> key("selfStatsColumns", (int) Math.round(v)),
                v -> EnemyHudConfig.get().selfStatsColumns = (int) Math.round(v)));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                0.5, 2.0, 0.05, cfg.selfStatsScale,
                v -> key("selfStatsScale", String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().selfStatsScale = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                0.0, 800.0, 1.0, cfg.selfStatsX,
                v -> key("selfStatsX", (int) Math.round(v)),
                v -> EnemyHudConfig.get().selfStatsX = (int) Math.round(v)));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(leftX, y, w, ROW_HEIGHT,
                0.0, 800.0, 1.0, cfg.selfStatsY,
                v -> key("selfStatsY", (int) Math.round(v)),
                v -> EnemyHudConfig.get().selfStatsY = (int) Math.round(v)));

        y = TOP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                0.2, 1.0, 0.05, cfg.selfStatsOpacity,
                v -> key("selfStatsOpacity", Math.round(v * 100.0) + "%"),
                v -> EnemyHudConfig.get().selfStatsOpacity = v));
        y += ROW_GAP;
        addRenderableWidget(Button.builder(key("selfStatsMove"),
                        b -> this.minecraft.gui.setScreen(new SelfStatsLayoutScreen(this)))
                .bounds(rightX, y, w, ROW_HEIGHT).build());
        y += ROW_GAP;
        addRenderableWidget(Button.builder(key("selfStatsFilter"),
                        b -> this.minecraft.gui.setScreen(new SelfStatsFilterScreen(this)))
                .bounds(rightX, y, w, ROW_HEIGHT).build());
        return 6;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.title"),
                this.width / 2, 24, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, 40, 0xFFFFD060);
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
