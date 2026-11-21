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
 * Mod Menu 里的设置界面：只保留「头顶血条」相关选项。
 *
 * <p>拖动即时生效，关闭界面时才归一化并写回磁盘。</p>
 */
public final class EnemyHudConfigScreen extends Screen {

    private static final Component TITLE = Component.translatable("ctt_enemy_hud.config.title");
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 36;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_WIDTH_MAX = 210;

    private final Screen parent;

    public EnemyHudConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int rowWidth = Math.max(96, Math.min(ROW_WIDTH_MAX, (this.width - COLUMN_GAP * 3) / 2));
        int leftX = this.width / 2 - rowWidth - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;
        int y = TOP;

        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
                .create(leftX, y, rowWidth, ROW_HEIGHT, key("enabled"),
                        (b, v) -> EnemyHudConfig.get().enabled = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showBar)
                .create(leftX, y, rowWidth, ROW_HEIGHT, key("showBar"),
                        (b, v) -> EnemyHudConfig.get().showBar = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showNumbers)
                .create(leftX, y, rowWidth, ROW_HEIGHT, key("showNumbers"),
                        (b, v) -> EnemyHudConfig.get().showNumbers = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.showArmor)
                .create(leftX, y, rowWidth, ROW_HEIGHT, key("showArmor"),
                        (b, v) -> EnemyHudConfig.get().showArmor = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.textShadow)
                .create(leftX, y, rowWidth, ROW_HEIGHT, key("textShadow"),
                        (b, v) -> EnemyHudConfig.get().textShadow = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBehindWalls)
                .create(leftX, y, rowWidth, ROW_HEIGHT, key("hideBehindWalls"),
                        (b, v) -> EnemyHudConfig.get().hideBehindWalls = v));

        y = TOP;
        addRenderableWidget(new ValueSlider(rightX, y, rowWidth, ROW_HEIGHT,
                50.0, 100.0, 1.0, cfg.opacity * 100.0,
                v -> key("opacity", Math.round(v) + "%"),
                v -> EnemyHudConfig.get().opacity = v / 100.0));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, rowWidth, ROW_HEIGHT,
                50.0, 150.0, 1.0, cfg.barWidth,
                v -> key("barWidth", (int) Math.round(v)),
                v -> EnemyHudConfig.get().barWidth = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, rowWidth, ROW_HEIGHT,
                0.75, 1.5, 0.05, cfg.scale,
                v -> key("scale", String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().scale = v));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, rowWidth, ROW_HEIGHT,
                -30.0, 0.0, 1.0, cfg.verticalOffset,
                v -> key("verticalOffset", signed((int) Math.round(v))),
                v -> EnemyHudConfig.get().verticalOffset = (int) Math.round(v)));
        y += ROW_GAP;
        addRenderableWidget(new ValueSlider(rightX, y, rowWidth, ROW_HEIGHT,
                4.0, 128.0, 4.0, cfg.maxDistance,
                v -> key("maxDistance", (int) Math.round(v)),
                v -> EnemyHudConfig.get().maxDistance = v));

        int totalWidth = rowWidth * 2 + COLUMN_GAP;
        int half = (totalWidth - 8) / 2;
        int buttonY = Math.min(TOP + ROW_GAP * 6 + 6, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(key("reset"), b -> {
            EnemyHudConfig.reset();
            this.minecraft.gui.setScreen(new EnemyHudConfigScreen(this.parent));
        }).bounds(leftX, buttonY, half, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(leftX + half + 8, buttonY, totalWidth - half - 8, ROW_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, TITLE, this.width / 2, 12, 0xFFFFFFFF);
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

    /** 带数值标签的滑块：内部用 0..1 的归一化值，applyValue 时量化到 step 并回调。 */
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
