package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** 盾牌指示器设置（1.20.19 合并自 CTT Shield Indicator）。 */
public final class ShieldIndicatorSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int WIDTH = 260;

    private final EnemyHudConfigScreen menu;

    public ShieldIndicatorSettingsScreen(EnemyHudConfigScreen menu) {
        super(Component.translatable("ctt_enemy_hud.config.section.shield"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        boolean on = cfg.shieldEnabled;

        CycleButton<Boolean> toggle = CycleButton.onOffBuilder(cfg.shieldEnabled)
                .create(x, y, width, ROW_HEIGHT, Component.translatable("ctt_enemy_hud.config.shieldEnabled"),
                        (b, v) -> {
                            EnemyHudConfig.get().shieldEnabled = v;
                            this.rebuildWidgets();
                        });
        addRenderableWidget(toggle);
        y += ROW_GAP;

        AbstractSliderButton scale = new ValueSlider(x, y, width, ROW_HEIGHT,
                0.5, 3.0, 0.05, cfg.shieldScale,
                v -> Component.translatable("ctt_enemy_hud.config.shieldScale",
                        String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().shieldScale = v);
        scale.active = on;
        addRenderableWidget(scale);
        y += ROW_GAP;

        AbstractSliderButton offset = new ValueSlider(x, y, width, ROW_HEIGHT,
                -160.0, 160.0, 1.0, cfg.shieldOffset,
                v -> Component.translatable("ctt_enemy_hud.config.shieldOffset", (int) Math.round(v)),
                v -> EnemyHudConfig.get().shieldOffset = (int) Math.round(v));
        offset.active = on;
        addRenderableWidget(offset);
        y += ROW_GAP;

        AbstractSliderButton gap = new ValueSlider(x, y, width, ROW_HEIGHT,
                0.0, 40.0, 1.0, cfg.shieldGap,
                v -> Component.translatable("ctt_enemy_hud.config.shieldGap", (int) Math.round(v)),
                v -> EnemyHudConfig.get().shieldGap = (int) Math.round(v));
        gap.active = on;
        addRenderableWidget(gap);
        y += ROW_GAP;

        net.minecraft.client.gui.components.Button position =
                net.minecraft.client.gui.components.Button.builder(
                                Component.translatable("ctt_enemy_hud.config.shieldPosition",
                                        Component.translatable(cfg.shieldPosition == 1
                                                ? "ctt_enemy_hud.config.shieldPosition.center"
                                                : "ctt_enemy_hud.config.shieldPosition.offhand")), b -> {
                                    EnemyHudConfig.Cfg c = EnemyHudConfig.get();
                                    c.shieldPosition = c.shieldPosition == 1 ? 0 : 1;
                                    this.rebuildWidgets();
                                })
                        .bounds(x, y, width, ROW_HEIGHT).build();
        position.active = on;
        addRenderableWidget(position);

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private void back() {
        this.minecraft.gui.setScreen(this.menu);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        back();
    }

    /** 简化滑块。 */
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
