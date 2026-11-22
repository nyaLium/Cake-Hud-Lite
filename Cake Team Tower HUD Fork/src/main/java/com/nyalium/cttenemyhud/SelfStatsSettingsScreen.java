package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 「属性面板设置」（1.20.20 从子页面独立成可滚动界面，带右侧滚动条）。
 */
public final class SelfStatsSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 26;
    private static final int TOP = 52;
    private static final int BOTTOM_PAD = 56;
    private static final int WIDTH_MAX = 300;

    private final EnemyHudConfigScreen menu;
    private ScrollPane pane = new ScrollPane(TOP, 1000);
    private final List<Row> rows = new ArrayList<>();
    /** 滚动区里的控件：只登记输入（{@code addWidget}），绘制由 {@link #paintContent} 裁剪。 */
    private final List<AbstractWidget> contentRows = new ArrayList<>();
    private int contentHeight;
    private boolean editContainer;

    public SelfStatsSettingsScreen(EnemyHudConfigScreen menu) {
        super(Component.translatable("ctt_enemy_hud.config.section.self_stats"));
        this.menu = menu;
    }

    private record Row(String id, Component label, int y, int h, Kind kind, Object payload) {}

    private enum Kind { TOGGLE, SLIDER, BUTTON }

    private record SliderSpec(double min, double max, double step, double value,
                              DoubleFunction<Component> label, DoubleConsumer onChange) {}

    @Override
    protected void init() {
        buildRows();
        rebuildWidgets();
    }

    private void buildRows() {
        if (!rows.isEmpty()) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int mode = cfg.selfStatsMode;
        EnemyHudConfig.Panel panel = EnemyHudConfig.panel(mode, mode == 1 || (mode == 2 && editContainer));
        int y = 0;
        y = toggle("showSelfStats", y);
        y = slider("selfStatsSeconds", y, 1, 5, 1, cfg.selfStatsSeconds, mode);
        y = slider("selfStatsColumns", y, 1, 3, 1, panel.columns, mode);
        y = slider("selfStatsScale", y, 1.0, 2.0, 0.05, panel.scale, mode);
        y = slider("selfStatsOpacity", y, 0.2, 1.0, 0.05, panel.opacity, mode);
        y = button("selfStatsMode", y, "mode");
        if (mode == 2) y = button("selfStatsTarget", y, "target");
        y = button("selfStatsMove", y, "move");
        y = button("selfStatsFilter", y, "filter");
        contentHeight = y;
    }

    private int toggle(String key, int y) {
        rows.add(new Row(key, k(key), y, ROW_HEIGHT, Kind.TOGGLE, null));
        return y + ROW_STEP;
    }

    private int slider(String key, int y, double min, double max, double step, double value, int mode) {
        rows.add(new Row(key, k(key), y, ROW_HEIGHT, Kind.SLIDER,
                new SliderSpec(min, max, step, value, null, null)));
        return y + ROW_STEP;
    }

    private int button(String key, int y, String action) {
        rows.add(new Row(key, k(key), y, ROW_HEIGHT, Kind.BUTTON, action));
        return y + ROW_STEP;
    }

    /** 显示方式循环顺序：HUD + 容器界面(2) → HUD 显示(0) → 容器界面显示(1) → 回到 2。 */
    static int nextMode(int mode) {
        return switch (mode) {
            case 2 -> 0;
            case 0 -> 1;
            default -> 2;
        };
    }

    /** 显示方式按钮的翻译键（0/1/2）。 */
    static String modeLabelKey(int mode) {
        return switch (mode) {
            case 1 -> "ctt_enemy_hud.config.selfStatsMode.container";
            case 2 -> "ctt_enemy_hud.config.selfStatsMode.both";
            default -> "ctt_enemy_hud.config.selfStatsMode.always";
        };
    }

    private static Component k(String id) {
        return Component.translatable("ctt_enemy_hud.config." + id);
    }

    @Override
    public void rebuildWidgets() {
        int keep = this.pane.scroll();
        this.pane = new ScrollPane(TOP, this.height - BOTTOM_PAD);
        this.pane.contentHeight(contentHeight);
        this.pane.scrollBy(keep);
        clearWidgets();
        this.contentRows.clear();

        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int mode = cfg.selfStatsMode;
        boolean editContainer = mode == 1 || (mode == 2 && this.editContainer);
        boolean enabled = cfg.showSelfStats;
        boolean forced = SelfStats.forcedByServer(this.minecraft);
        EnemyHudConfig.Panel panel = EnemyHudConfig.panel(mode, editContainer);
        int width = Math.max(140, Math.min(WIDTH_MAX, this.width - 60));
        int x = this.width / 2 - width / 2;
        List<AbstractWidget> dependent = new ArrayList<>();

        for (Row row : rows) {
            // 整行都在可视区外的行不建控件
            if (!this.pane.intersects(row.y, row.h)) continue;
            int y = this.pane.screenY(row.y);
            AbstractWidget widget = null;
            switch (row.kind) {
                case TOGGLE -> widget = CycleButton.onOffBuilder(cfg.showSelfStats)
                        .create(x, y, width, ROW_HEIGHT, row.label, (b, v) -> {
                            EnemyHudConfig.get().showSelfStats = v;
                            this.rebuildWidgets();
                        });
                case SLIDER -> {
                    SliderSpec spec = (SliderSpec) row.payload;
                    String id = row.id();
                    if (id.equals("selfStatsSeconds")) {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 1, 5, 1, cfg.selfStatsSeconds,
                                v -> {
                                    if (SelfStats.forcedByServer(this.minecraft)) {
                                        return Component.translatable("ctt_enemy_hud.config.selfStatsSecondsForced",
                                                SelfStats.FORCED_SERVER_SECONDS);
                                    }
                                    return Math.round(v) <= 1
                                            ? Component.translatable("ctt_enemy_hud.config.refreshNow")
                                            : Component.translatable("ctt_enemy_hud.config.selfStatsSeconds",
                                                    (int) Math.round(v));
                                },
                                v -> EnemyHudConfig.get().selfStatsSeconds = (int) Math.round(v));
                        widget.active = enabled && !forced;
                    } else if (id.equals("selfStatsColumns")) {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 1, 3, 1, panel.columns,
                                v -> Component.translatable("ctt_enemy_hud.config.selfStatsColumns",
                                        (int) Math.round(v)),
                                v -> EnemyHudConfig.setPanelColumns(mode, editContainer, (int) Math.round(v)));
                    } else if (id.equals("selfStatsScale")) {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 1.0, 2.0, 0.05, panel.scale,
                                v -> Component.translatable("ctt_enemy_hud.config.selfStatsScale",
                                        String.format(Locale.ROOT, "%.2f", v)),
                                v -> EnemyHudConfig.setPanelScale(mode, editContainer, v));
                    } else {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 0.2, 1.0, 0.05, panel.opacity,
                                v -> Component.translatable("ctt_enemy_hud.config.selfStatsOpacity",
                                        Math.round(v * 100.0) + "%"),
                                v -> EnemyHudConfig.setPanelOpacity(mode, editContainer, v));
                    }
                }
                case BUTTON -> {
                    String action = (String) row.payload;
                    Component label = switch (action) {
                        case "mode" -> Component.translatable("ctt_enemy_hud.config.selfStatsMode",
                                Component.translatable(modeLabelKey(mode)));
                        case "target" -> Component.translatable("ctt_enemy_hud.config.selfStatsTarget",
                                Component.translatable(editContainer
                                        ? "ctt_enemy_hud.config.selfStatsTarget.container"
                                        : "ctt_enemy_hud.config.selfStatsTarget.hud"));
                        default -> row.label;
                    };
                    widget = Button.builder(label, b -> {
                        switch (action) {
                            case "mode" -> {
                                EnemyHudConfig.Cfg c = EnemyHudConfig.get();
                                c.selfStatsMode = nextMode(c.selfStatsMode);
                                this.editContainer = false;
                                rows.clear();      // 模式 2 多一行「编辑对象」，行表要重建
                                buildRows();
                                this.rebuildWidgets();
                            }
                            case "target" -> {
                                this.editContainer = !this.editContainer;
                                this.rebuildWidgets();
                            }
                            case "move" -> this.minecraft.gui.setScreen(
                                    new SelfStatsLayoutScreen(this, editContainer));
                            case "filter" -> this.minecraft.gui.setScreen(
                                    new SelfStatsFilterScreen(this));
                            default -> {
                            }
                        }
                    }).bounds(x, y, width, ROW_HEIGHT).build();
                }
            }
            if (widget == null) continue;
            addWidget(widget);
            this.contentRows.add(widget);
            if (!row.kind.equals(Kind.TOGGLE)) dependent.add(widget);
        }
        if (!enabled) for (AbstractWidget w : dependent) w.active = false;

        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.pane.beginClip(graphics, this.width);
        for (AbstractWidget widget : this.contentRows) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.pane.endClip(graphics);
        graphics.centeredText(this.font, this.title, this.width / 2, 22, 0xFFFFFFFF);
        this.pane.drawScrollbar(graphics, this.width / 2 + Math.min(WIDTH_MAX, this.width - 60) / 2 + 10);
        if (this.pane.maxScroll() > 0) {
            graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.scrollHint"),
                    this.width / 2, this.height - ROW_HEIGHT - 26, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.pane.scrollBy(-(int) Math.round(scrollY * ROW_STEP));
        this.rebuildWidgets();
        return true;
    }

    private void back() {
        this.minecraft.gui.setScreen(this.menu);
    }

    @Override
    public void onClose() {
        back();
    }

    private static final class ValueSlider extends net.minecraft.client.gui.components.AbstractSliderButton {
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
