package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 「标题设置」（1.6.1）：{@code /title} 的<b>标题</b>与<b>副标题</b>的整体位移，
 * 以及两者各自的不透明度。
 *
 * <p><b>不含 actionbar</b>：actionbar 走的是 {@code Hud.extractOverlayMessage}，
 * 本功能的 mixin 只挂 {@code Hud.extractTitle}，所以 {@code /title ... actionbar ...}
 * 与 {@code /title ... title/subtitle ...} 天然分开。</p>
 *
 * <h2>行 ↔ 配置字段</h2>
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th></tr>
 *   <tr><td>标题不透明度</td><td>{@code titleOpacity}</td><td>0.10~1.00，默认 1.00</td></tr>
 *   <tr><td>副标题不透明度</td><td>{@code subtitleOpacity}</td><td>0.10~1.00，默认 1.00</td></tr>
 *   <tr><td>水平位移</td><td>{@code titleOffsetX}</td><td>-300~300 px，默认 0（正 = 右）</td></tr>
 *   <tr><td>垂直位移</td><td>{@code titleOffsetY}</td><td>-300~300 px，默认 0（正 = 下）</td></tr>
 * </table>
 */
public final class TitleSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int WIDTH = 260;
    /** 位移滑块的上下限（GUI 像素）。 */
    private static final int OFFSET_LIMIT = 300;

    private final HudConfigScreen menu;

    public TitleSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.section.title"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;
        HudConfig.Cfg cfg = HudConfig.get();

        // 标题不透明度：titleOpacity（0.10 ~ 1.00，步进 0.05）
        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                0.1, 1.0, 0.05, cfg.titleOpacity,
                v -> Component.translatable("ctt_hud_fork.config.titleOpacity",
                        Math.round(v * 100.0) + "%"),
                v -> HudConfig.get().titleOpacity = v));
        y += ROW_GAP;

        // 副标题不透明度：subtitleOpacity
        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                0.1, 1.0, 0.05, cfg.subtitleOpacity,
                v -> Component.translatable("ctt_hud_fork.config.subtitleOpacity",
                        Math.round(v * 100.0) + "%"),
                v -> HudConfig.get().subtitleOpacity = v));
        y += ROW_GAP;

        // 水平位移：titleOffsetX（-300 ~ 300 px，步进 1）
        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                -OFFSET_LIMIT, OFFSET_LIMIT, 1, cfg.titleOffsetX,
                v -> Component.translatable("ctt_hud_fork.config.titleOffsetX",
                        String.valueOf((int) Math.round(v))),
                v -> HudConfig.get().titleOffsetX = (int) Math.round(v)));
        y += ROW_GAP;

        // 垂直位移：titleOffsetY
        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                -OFFSET_LIMIT, OFFSET_LIMIT, 1, cfg.titleOffsetY,
                v -> Component.translatable("ctt_hud_fork.config.titleOffsetY",
                        String.valueOf((int) Math.round(v))),
                v -> HudConfig.get().titleOffsetY = (int) Math.round(v)));

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetTitle();
            this.init();
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
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.titleHint"),
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
