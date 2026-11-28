package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

/**
 * 「标题设置」：{@code /title} 的<b>标题</b>与<b>副标题</b>的整体位移，
 * 以及两者各自的不透明度。
 *
 * <p><b>不含 actionbar</b>：actionbar 走的是 {@code Hud.extractOverlayMessage}，
 * 本功能的 mixin 只挂 {@code Hud.extractTitle}，所以 {@code /title ... actionbar ...}
 * 与 {@code /title ... title/subtitle ...} 天然分开。</p>
 *
 * <h2>布局</h2>
 * <p>两列：<b>左列 = 标题</b>，<b>右列 = 副标题</b>，每列四行（不透明度 / 水平位移 / 垂直位移 / 缩放）。
 * 这样在 GUI 高度较小（GUI Scale 3~4）时也不用滚动。</p>
 *
 * <h2>行 ↔ 配置字段</h2>
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th></tr>
 *   <tr><td>标题不透明度</td><td>{@code titleOpacity}</td><td>0.10~1.00，默认 <b>0.60</b></td></tr>
 *   <tr><td>副标题不透明度</td><td>{@code subtitleOpacity}</td><td>0.10~1.00，默认 <b>0.60</b></td></tr>
 *   <tr><td>水平位移</td><td>{@code titleOffsetX} / {@code subtitleOffsetX}</td><td>-300~300 px，默认 0（正 = 右）</td></tr>
 *   <tr><td>垂直位移</td><td>{@code titleOffsetY} / {@code subtitleOffsetY}</td><td>-300~300 px，默认 <b>-135</b>（正 = 下）</td></tr>
 *   <tr><td>缩放</td><td>{@code titleScale} / {@code subtitleScale}</td><td>0.50~3.00，默认 1.00</td></tr>
 * </table>
 */
public final class TitleSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int WIDTH = 340;
    private static final int COLUMN_GAP = 10;
    /** 位移滑块的上下限（GUI 像素）。 */
    private static final int OFFSET_LIMIT = 300;
    /** 缩放滑块的上下限（相对原版字号）。 */
    private static final double SCALE_MIN = 0.5;
    private static final double SCALE_MAX = 3.0;
    private static final double SCALE_STEP = 0.05;

    private final HudConfigScreen menu;

    public TitleSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.titlePage"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int left = this.width / 2 - width / 2;
        int column = (width - COLUMN_GAP) / 2;
        int right = left + column + COLUMN_GAP;
        HudConfig.Cfg cfg = HudConfig.get();

        // ── 左列：标题 ──
        columnOf(left, column, TOP,
                cfg.titleOpacity,
                cfg.titleOffsetX, cfg.titleOffsetY,
                cfg.titleScale,
                "titleOpacity", "titleOffsetX", "titleOffsetY", "titleScale",
                v -> HudConfig.get().titleOpacity = v,
                v -> HudConfig.get().titleOffsetX = (int) Math.round(v),
                v -> HudConfig.get().titleOffsetY = (int) Math.round(v),
                v -> HudConfig.get().titleScale = v);

        // ── 右列：副标题 ──
        columnOf(right, column, TOP,
                cfg.subtitleOpacity,
                cfg.subtitleOffsetX, cfg.subtitleOffsetY,
                cfg.subtitleScale,
                "subtitleOpacity", "subtitleOffsetX", "subtitleOffsetY", "subtitleScale",
                v -> HudConfig.get().subtitleOpacity = v,
                v -> HudConfig.get().subtitleOffsetX = (int) Math.round(v),
                v -> HudConfig.get().subtitleOffsetY = (int) Math.round(v),
                v -> HudConfig.get().subtitleScale = v);

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetTitle();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    /** 一列四行：不透明度 / 水平位移 / 垂直位移 / 缩放。 */
    private void columnOf(int x, int width, int top,
                          double opacity, int offsetX, int offsetY, double scale,
                          String opacityKey, String offsetXKey, String offsetYKey, String scaleKey,
                          DoubleConsumer onOpacity, DoubleConsumer onOffsetX,
                          DoubleConsumer onOffsetY, DoubleConsumer onScale) {
        int y = top;

        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                0.1, 1.0, 0.05, opacity,
                v -> Component.translatable("ctt_hud_fork.config." + opacityKey, percent(v)),
                onOpacity));
        y += ROW_GAP;

        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                -OFFSET_LIMIT, OFFSET_LIMIT, 1, offsetX,
                v -> Component.translatable("ctt_hud_fork.config." + offsetXKey,
                        String.valueOf((int) Math.round(v))),
                onOffsetX));
        y += ROW_GAP;

        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                -OFFSET_LIMIT, OFFSET_LIMIT, 1, offsetY,
                v -> Component.translatable("ctt_hud_fork.config." + offsetYKey,
                        String.valueOf((int) Math.round(v))),
                onOffsetY));
        y += ROW_GAP;

        addRenderableWidget(new ValueSlider(x, y, width, ROW_HEIGHT,
                SCALE_MIN, SCALE_MAX, SCALE_STEP, scale,
                v -> Component.translatable("ctt_hud_fork.config." + scaleKey, percent(v)),
                onScale));
    }

    private static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    private void back() {
        HudConfig.save();
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

}
