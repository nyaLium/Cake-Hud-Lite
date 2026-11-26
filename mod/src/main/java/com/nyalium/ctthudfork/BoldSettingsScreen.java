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
 * 「杂项… → 粗体渲染…」（1.6.3）。
 *
 * <p>列表：是否启用 / 粗体模式 / 膨胀强度 / 偏移距离。</p>
 *
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th></tr>
 *   <tr><td>是否启用</td><td>{@code boldOptimize}</td><td>默认开（关掉 = 完全原版）</td></tr>
 *   <tr><td>粗体模式</td><td>{@code boldMode}</td><td>0 = 膨胀（默认，省一半 quad）/ 1 = 偏移（保留双画，只调距离）</td></tr>
 *   <tr><td>膨胀强度</td><td>{@code boldDilation}</td><td>0 ~ 0.5，默认 0.25（仅膨胀模式）</td></tr>
 *   <tr><td>偏移距离</td><td>{@code boldOffset}</td><td>0 ~ 1.0，默认 0.35（仅偏移模式）</td></tr>
 * </table>
 */
public final class BoldSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int WIDTH = 260;

    /** 粗体模式取值。 */
    static final int MODE_DILATION = 0;
    /** 偏移模式（保留双画，仅调整距离；不含性能收益）。 */
    static final int MODE_OFFSET = 1;

    private final Screen parent;

    public BoldSettingsScreen(Screen parent) {
        super(Component.translatable("ctt_hud_fork.config.boldTitle"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;
        HudConfig.Cfg cfg = HudConfig.get();
        boolean on = cfg.boldOptimize;
        boolean dilation = cfg.boldMode == MODE_DILATION;

        // 是否启用
        addRenderableWidget(CycleButton.onOffBuilder(cfg.boldOptimize)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.boldOptimize"),
                        (b, v) -> {
                            HudConfig.get().boldOptimize = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        // 粗体模式：膨胀 / 偏移
        addRenderableWidget(CycleButton.<Integer>builder(mode -> Component.translatable(
                        "ctt_hud_fork.config.boldMode." + (mode == MODE_DILATION ? "dilation" : "offset")),
                        cfg.boldMode)
                .withValues(MODE_DILATION, MODE_OFFSET)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.boldMode"),
                        (b, v) -> {
                            HudConfig.get().boldMode = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        // 膨胀强度：0 ~ 0.5（仅膨胀模式）
        AbstractSliderButton dilationSlider = new ValueSlider(x, y, width, ROW_HEIGHT,
                0.0, 0.5, 0.05, cfg.boldDilation,
                v -> Component.translatable("ctt_hud_fork.config.boldDilation",
                        String.format(java.util.Locale.ROOT, "%.2f", v)),
                v -> HudConfig.get().boldDilation = v);
        dilationSlider.active = on && dilation;
        addRenderableWidget(dilationSlider);
        y += ROW_GAP;

        // 偏移距离：0 ~ 1.0（仅偏移模式）
        AbstractSliderButton offsetSlider = new ValueSlider(x, y, width, ROW_HEIGHT,
                0.0, 1.0, 0.05, cfg.boldOffset,
                v -> Component.translatable("ctt_hud_fork.config.boldOffset",
                        String.format(java.util.Locale.ROOT, "%.2f", v)),
                v -> HudConfig.get().boldOffset = v);
        offsetSlider.active = on && !dilation;
        addRenderableWidget(offsetSlider);

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetBold();
            this.init();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private void back() {
        HudConfig.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.boldHint"),
                this.width / 2, 40, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        back();
    }

    /** 与其它设置页同款的滑块（步进吸附 + 自定义文案 + 变化回调）。 */
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
