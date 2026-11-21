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
 * Mod Menu 设置界面，按四类分页：
 * <ol>
 *   <li>总开关</li>
 *   <li>敌人血条设置</li>
 *   <li>玩家血条设置（队友）</li>
 *   <li>通用设置</li>
 * </ol>
 * 拖动即时生效，关闭界面时才归一化并写回磁盘。
 */
public final class EnemyHudConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 52;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_WIDTH_MAX = 210;

    private final Screen parent;
    private final int page;

    public EnemyHudConfigScreen(Screen parent) {
        this(parent, 0);
    }

    private EnemyHudConfigScreen(Screen parent, int page) {
        super(Component.translatable("ctt_enemy_hud.config.title"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int w = Math.max(96, Math.min(ROW_WIDTH_MAX, (this.width - COLUMN_GAP * 3) / 2));
        int leftX = this.width / 2 - w - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;

        switch (page) {
            case 0 -> initMaster(cfg, w, leftX, rightX);
            case 1 -> initEnemy(cfg, w, leftX, rightX);
            case 2 -> initPlayer(cfg, w, leftX, rightX);
            default -> initGeneral(cfg, w, leftX, rightX);
        }

        int totalWidth = w * 2 + COLUMN_GAP;
        int buttonY = Math.min(TOP + ROW_GAP * 5 + 4, this.height - ROW_HEIGHT - 8);
        int small = (totalWidth - 16) / 3;

        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.prev_section"),
                        b -> switchPage(-1))
                .bounds(leftX, buttonY, small, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.next_section"),
                        b -> switchPage(1))
                .bounds(leftX + small + 8, buttonY, small, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftX + (small + 8) * 2, buttonY, totalWidth - (small + 8) * 2, ROW_HEIGHT).build());
    }

    private void switchPage(int delta) {
        int next = (page + delta + 4) % 4;
        this.minecraft.gui.setScreen(new EnemyHudConfigScreen(this.parent, next));
    }

    // ── 第 1 页：总开关 ──

    private void initMaster(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
                .create(leftX, y, w, ROW_HEIGHT, key("enabled"),
                        (b, v) -> EnemyHudConfig.get().enabled = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.textShadow)
                .create(leftX, y, w, ROW_HEIGHT, key("textShadow"),
                        (b, v) -> EnemyHudConfig.get().textShadow = v));

        y = TOP;
        addRenderableWidget(new ValueSlider(rightX, y, w, ROW_HEIGHT,
                50.0, 100.0, 1.0, cfg.opacity * 100.0,
                v -> key("opacity", Math.round(v) + "%"),
                v -> EnemyHudConfig.get().opacity = v / 100.0));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.debug)
                .create(rightX, y, w, ROW_HEIGHT, key("debug"),
                        (b, v) -> EnemyHudConfig.get().debug = v));
    }

    // ── 第 2 页：敌人血条 ──

    private void initEnemy(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showBar)
                .create(leftX, y, w, ROW_HEIGHT, key("showBar"), (b, v) -> EnemyHudConfig.get().showBar = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showNumbers)
                .create(leftX, y, w, ROW_HEIGHT, key("showNumbers"),
                        (b, v) -> EnemyHudConfig.get().showNumbers = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showPercentText)
                .create(leftX, y, w, ROW_HEIGHT, key("showPercentText"),
                        (b, v) -> EnemyHudConfig.get().showPercentText = v));
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

    // ── 第 3 页：玩家血条 ──

    private void initPlayer(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showTeammates)
                .create(leftX, y, w, ROW_HEIGHT, key("showTeammates"),
                        (b, v) -> EnemyHudConfig.get().showTeammates = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.autoPartyBossbar)
                .create(leftX, y, w, ROW_HEIGHT, key("autoPartyBossbar"),
                        (b, v) -> EnemyHudConfig.get().autoPartyBossbar = v));
    }

    // ── 第 4 页：通用设置 ──

    private void initGeneral(EnemyHudConfig.Cfg cfg, int w, int leftX, int rightX) {
        int y = TOP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBehindWalls)
                .create(leftX, y, w, ROW_HEIGHT, key("hideBehindWalls"),
                        (b, v) -> EnemyHudConfig.get().hideBehindWalls = v));
        y += ROW_GAP;
        addRenderableWidget(Button.builder(key("reset"), b -> {
            EnemyHudConfig.reset();
            this.minecraft.gui.setScreen(new EnemyHudConfigScreen(this.parent, page));
        }).bounds(leftX, y, w, ROW_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        String[] sections = {"master", "enemy", "player", "general"};
        Component title = Component.translatable("ctt_enemy_hud.config.title");
        graphics.centeredText(this.font, title, this.width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(this.font,
                Component.translatable("ctt_enemy_hud.config.section." + sections[page]),
                this.width / 2, 28, 0xFFFFD060);
    }

    @Override
    public void onClose() {
        EnemyHudConfig.save();
        this.minecraft.gui.setScreen(this.parent);
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
