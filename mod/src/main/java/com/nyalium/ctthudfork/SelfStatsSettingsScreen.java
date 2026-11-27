package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 「属性面板设置」（1.20.20 从子页面独立成可滚动界面，带右侧滚动条）。
 *
 * <h2>行 ↔ 配置字段（从上到下的顺序）</h2>
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th></tr>
 *   <tr><td>开关</td><td>{@code showSelfStats}</td><td>默认开</td></tr>
 *   <tr><td>刷新间隔</td><td>{@code selfStatsSeconds}</td><td>1~5 秒，默认 3；他人服务器强制 3 s 且滑条禁用</td></tr>
 *   <tr><td>列数</td><td>{@code *Columns}</td><td>1~3，默认 2</td></tr>
 *   <tr><td>不透明度</td><td>{@code *Opacity}</td><td>0.20~1.00，默认 1.0</td></tr>
 *   <tr><td>文字阴影</td><td>{@code selfStatsTextShadow}</td><td>默认开（与实体血条 {@code textShadow} 分开）</td></tr>
 *   <tr><td>显示方式</td><td>{@code selfStatsMode}</td><td>0 HUD / 1 容器 / 2 两者，默认 2</td></tr>
 *   <tr><td>编辑对象</td><td>（仅模式 2）</td><td>HUD 那套 ↔ 容器那套</td></tr>
 *   <tr><td>调整位置…</td><td>{@code *X / *Y} + {@code layoutGuiW/H}</td><td>拖到哪存哪；{@code -1} = 屏幕高 ÷ 3</td></tr>
 *   <tr><td>显示内容过滤…</td><td>{@code selfStatsHidden}</td><td>88 项，默认全显示</td></tr>
 * </table>
 */
public final class SelfStatsSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 26;
    private static final int TOP = 52;
    private static final int BOTTOM_PAD = 56;
    private static final int WIDTH_MAX = 300;

    private final HudConfigScreen menu;
    private ScrollPane pane = new ScrollPane(TOP, 1000);
    private final List<Row> rows = new ArrayList<>();
    /** 滚动区里的控件：只登记输入（{@code addWidget}），绘制由 {@link #paintContent} 裁剪。 */
    private final List<AbstractWidget> contentRows = new ArrayList<>();
    private int contentHeight;
    private boolean editContainer;

    public SelfStatsSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.section.self_stats"));
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
        HudConfig.Cfg cfg = HudConfig.get();
        int mode = cfg.selfStatsMode;
        int y = 0;
        // 总开关：showSelfStats（整个模组的总开关是 enabled，不在这页）
        y = toggle("showSelfStats", y);
        // 刷新间隔：selfStatsSeconds（1~5 秒，默认 3；没有「立即」档）
        y = slider("selfStatsSeconds", y, 1, 5, 1, cfg.selfStatsSeconds, mode);
        // 列数 / 不透明度（**全局**，只看 selfStatsColumns / selfStatsOpacity）
        y = slider("selfStatsColumns", y, 1, 3, 1, cfg.selfStatsColumns, mode);
        y = slider("selfStatsOpacity", y, 0.2, 1.0, 0.05, cfg.selfStatsOpacity, mode);
        // 文字阴影：selfStatsTextShadow（只管面板，与实体血条 textShadow 分开）
        y = toggle("selfStatsTextShadow", y);
        // 显示方式：selfStatsMode（0 HUD / 1 容器 / 2 两者；循环顺序见 nextMode）
        y = button("selfStatsMode", y, "mode");
        // 不再有「编辑对象」—— 模式 2 的两套位置在「调整位置…」里各自摆。
        // 调整位置…：进 SelfStatsLayoutScreen（拖动 / 方向键，写 *X/*Y + layoutGuiW/H）
        y = button("selfStatsMove", y, "move");
        // 显示内容过滤…：进 SelfStatsFilterScreen（88 项，三节；写 selfStatsHidden）
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
            case 1 -> "ctt_hud_fork.config.selfStatsMode.container";
            case 2 -> "ctt_hud_fork.config.selfStatsMode.both";
            default -> "ctt_hud_fork.config.selfStatsMode.always";
        };
    }

    private static Component k(String id) {
        return Component.translatable("ctt_hud_fork.config." + id);
    }

    /** 开关行的当前值（面板文字阴影独立，不再只有一个开关）。 */
    private static boolean toggleValue(String id) {
        HudConfig.Cfg cfg = HudConfig.get();
        return switch (id) {
            case "selfStatsTextShadow" -> cfg.selfStatsTextShadow;
            default -> cfg.showSelfStats;
        };
    }

    private static void setToggle(String id, boolean value) {
        HudConfig.Cfg cfg = HudConfig.get();
        switch (id) {
            case "selfStatsTextShadow" -> cfg.selfStatsTextShadow = value;
            default -> cfg.showSelfStats = value;
        }
    }

    @Override
    public void rebuildWidgets() {
        int keep = this.pane.scroll();
        this.pane = new ScrollPane(TOP, this.height - BOTTOM_PAD);
        this.pane.contentHeight(contentHeight);
        this.pane.scrollBy(keep);
        clearWidgets();
        this.contentRows.clear();

        HudConfig.Cfg cfg = HudConfig.get();
        int mode = cfg.selfStatsMode;
        // 列数/不透明度全局；这里只在「容器显示」时把位置编辑指向容器那套。
        boolean editContainer = mode == 1;
        boolean enabled = cfg.showSelfStats;
        boolean forced = SelfStats.forcedByServer(this.minecraft);
        HudConfig.Panel panel = HudConfig.panel(mode, editContainer, this.width, this.height);
        int width = Math.max(140, Math.min(WIDTH_MAX, this.width - 60));
        int x = this.width / 2 - width / 2;
        List<AbstractWidget> dependent = new ArrayList<>();

        for (Row row : rows) {
            // 整行都在可视区外的行不建控件
            if (!this.pane.intersects(row.y, row.h)) continue;
            int y = this.pane.screenY(row.y);
            AbstractWidget widget = null;
            switch (row.kind) {
                case TOGGLE -> widget = CycleButton.onOffBuilder(toggleValue(row.id()))
                        .create(x, y, width, ROW_HEIGHT, row.label, (b, v) -> {
                            setToggle(row.id(), v);
                            this.rebuildWidgets();
                        });
                case SLIDER -> {
                    SliderSpec spec = (SliderSpec) row.payload;
                    String id = row.id();
                    if (id.equals("selfStatsSeconds")) {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 1, 5, 1, cfg.selfStatsSeconds,
                                v -> {
                                    if (SelfStats.forcedByServer(this.minecraft)) {
                                        return Component.translatable("ctt_hud_fork.config.selfStatsSecondsForced",
                                                SelfStats.FORCED_SERVER_SECONDS);
                                    }
                                    return Component.translatable(
                                            "ctt_hud_fork.config.selfStatsSeconds", (int) Math.round(v));
                                },
                                v -> HudConfig.get().selfStatsSeconds = (int) Math.round(v));
                        widget.active = enabled && !forced;
                    } else if (id.equals("selfStatsColumns")) {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 1, 3, 1, cfg.selfStatsColumns,
                                v -> Component.translatable("ctt_hud_fork.config.selfStatsColumns",
                                        (int) Math.round(v)),
                                v -> HudConfig.get().selfStatsColumns = (int) Math.round(v));
                    } else {
                        widget = new ValueSlider(x, y, width, ROW_HEIGHT, 0.2, 1.0, 0.05, cfg.selfStatsOpacity,
                                v -> Component.translatable("ctt_hud_fork.config.selfStatsOpacity",
                                        Math.round(v * 100.0) + "%"),
                                v -> HudConfig.get().selfStatsOpacity = v);
                    }
                }
                case BUTTON -> {
                    String action = (String) row.payload;
                    Component label = switch (action) {
                        case "mode" -> Component.translatable("ctt_hud_fork.config.selfStatsMode",
                                Component.translatable(modeLabelKey(mode)));
                        default -> row.label;
                    };
                    widget = Button.builder(label, b -> {
                        switch (action) {
                            case "mode" -> {
                                HudConfig.Cfg c = HudConfig.get();
                                c.selfStatsMode = nextMode(c.selfStatsMode);
                                this.editContainer = false;
                                rows.clear();      // 模式 2 多一行「编辑对象」，行表要重建
                                buildRows();
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

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetPanel();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
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
        this.pane.drawScrollbar(graphics, scrollbarRight(), mouseX, mouseY, this.barDragging);
        if (this.pane.maxScroll() > 0) {
            graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.scrollHint"),
                    this.width / 2, this.height - ROW_HEIGHT - 26, 0xFFA0A0A0);
        }
    }

    /** 滚动条右侧的锚点（和画滚动条用同一个值，命中判定才不会错位）。 */
    private int scrollbarRight() {
        return this.width / 2 + Math.min(WIDTH_MAX, this.width - 60) / 2 + 10;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.pane.scrollBy(-(int) Math.round(scrollY * ROW_STEP));
        this.rebuildWidgets();
        return true;
    }

    // ── 滚动条：可点可拖 ──

    /** 是否正在拖滑块。 */
    private boolean barDragging;
    /** 按下瞬间「滑块顶端相对光标」的偏移（拖动时滑块不跳）。 */
    private int barGrab;

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) return true;
        if (!Pointer.accept(event)) return false;
        double[] gui = Pointer.gui(this, event);
        if (!this.pane.hitsBar(gui[0], gui[1], scrollbarRight())) return false;
        this.barDragging = true;
        this.barGrab = this.pane.hitsThumb(gui[1]) ? this.pane.grabOffset(gui[1]) : this.pane.thumbHeight() / 2;
        dragBarTo(gui[1]);
        return true;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        if (this.barDragging) {
            dragBarTo(Pointer.gui(this, event)[1]);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (this.barDragging && Pointer.accept(event)) {
            this.barDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void dragBarTo(double mouseY) {
        this.pane.dragThumbTo((int) Math.round(mouseY) - this.barGrab);
        this.rebuildWidgets();
    }

    private void back() {
        HudConfig.save();
        this.minecraft.gui.setScreen(this.menu);
    }

    /** 「调整位置…」里就地切换了 HUD / 容器界面后，回到本页要跟着变。 */
    void setEditContainer(boolean editContainer) {
        this.editContainer = editContainer;
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
