package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 盾牌指示器设置（1.20.20：位置改为拖动定位 + 透明度）。
 */
public final class ShieldIndicatorSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int WIDTH = 260;

    private final HudConfigScreen menu;

    public ShieldIndicatorSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.section.shield"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;
        HudConfig.Cfg cfg = HudConfig.get();
        boolean on = cfg.shieldEnabled;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.shieldEnabled)
                .create(x, y, width, ROW_HEIGHT, Component.translatable("ctt_hud_fork.config.shieldEnabled"),
                        (b, v) -> {
                            HudConfig.get().shieldEnabled = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        AbstractSliderButton scale = new ValueSlider(x, y, width, ROW_HEIGHT,
                HudConfig.SHIELD_SCALE_MIN, HudConfig.SHIELD_SCALE_MAX, 0.05,
                ShieldIndicatorHud.clampScale(cfg.shieldScale),
                v -> Component.translatable("ctt_hud_fork.config.shieldScale",
                        String.format(Locale.ROOT, "%.2f", v)),
                v -> HudConfig.get().shieldScale = ShieldIndicatorHud.clampScale(v));
        scale.active = on;
        addRenderableWidget(scale);
        y += ROW_GAP;

        AbstractSliderButton opacity = new ValueSlider(x, y, width, ROW_HEIGHT,
                0.1, 1.0, 0.05, cfg.shieldOpacity,
                v -> Component.translatable("ctt_hud_fork.config.shieldOpacity",
                        Math.round(v * 100.0) + "%"),
                v -> HudConfig.get().shieldOpacity = v);
        opacity.active = on;
        addRenderableWidget(opacity);
        y += ROW_GAP;

        Button move = Button.builder(Component.translatable("ctt_hud_fork.config.shieldMove"),
                        b -> this.minecraft.gui.setScreen(new ShieldLayoutScreen(this)))
                .bounds(x, y, width, ROW_HEIGHT).build();
        move.active = on;
        addRenderableWidget(move);

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
