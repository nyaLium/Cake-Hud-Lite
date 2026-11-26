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
 * 盾牌指示器设置（1.20.20：位置改为拖动定位 + 不透明度）。
 *
 * <h2>行 ↔ 配置字段</h2>
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th></tr>
 *   <tr><td>显示盾牌指示器</td><td>{@code shieldEnabled}</td><td>默认开；关掉后下面几项灰掉</td></tr>
 *   <tr><td>缩放比例</td><td>{@code shieldScale}</td><td>0.50~1.50，步进 0.05，默认 0.70（不写「倍」）</td></tr>
 *   <tr><td>不透明度</td><td>{@code shieldOpacity}</td><td>0.10~1.00，默认 0.65</td></tr>
 *   <tr><td>调整位置…</td><td>{@code shieldX/Y} + {@code layoutGuiW/H}</td><td>拖动；{@code -1} = 跟随副手槽</td></tr>
 * </table>
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

        // 显示盾牌指示器：shieldEnabled（关掉后下面的缩放 / 不透明度 / 调整位置灰掉）
        addRenderableWidget(CycleButton.onOffBuilder(cfg.shieldEnabled)
                .create(x, y, width, ROW_HEIGHT, Component.translatable("ctt_hud_fork.config.shieldEnabled"),
                        (b, v) -> {
                            HudConfig.get().shieldEnabled = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        // 缩放比例：shieldScale（0.50~1.50，步进 0.05，默认 0.70；两位小数、不带「倍」）
        AbstractSliderButton scale = new ValueSlider(x, y, width, ROW_HEIGHT,
                HudConfig.SHIELD_SCALE_MIN, HudConfig.SHIELD_SCALE_MAX, 0.05,
                ShieldIndicatorHud.clampScale(cfg.shieldScale),
                v -> Component.translatable("ctt_hud_fork.config.shieldScale",
                        String.format(Locale.ROOT, "%.2f", v)),
                v -> HudConfig.get().shieldScale = ShieldIndicatorHud.clampScale(v));
        scale.active = on;
        addRenderableWidget(scale);
        y += ROW_GAP;

        // 不透明度：shieldOpacity（0.10~1.00，默认 0.65）
        AbstractSliderButton opacity = new ValueSlider(x, y, width, ROW_HEIGHT,
                0.1, 1.0, 0.05, cfg.shieldOpacity,
                v -> Component.translatable("ctt_hud_fork.config.shieldOpacity",
                        Math.round(v * 100.0) + "%"),
                v -> HudConfig.get().shieldOpacity = v);
        opacity.active = on;
        addRenderableWidget(opacity);
        y += ROW_GAP;

        // 调整位置…：进 ShieldLayoutScreen（拖动 / 方向键；写 shieldX/Y + layoutGuiW/H）
        Button move = Button.builder(Component.translatable("ctt_hud_fork.config.shieldMove"),
                        b -> this.minecraft.gui.setScreen(new ShieldLayoutScreen(this)))
                .bounds(x, y, width, ROW_HEIGHT).build();
        move.active = on;
        addRenderableWidget(move);

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetShield();
            this.init();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private void back() {
        HudConfig.save();
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
