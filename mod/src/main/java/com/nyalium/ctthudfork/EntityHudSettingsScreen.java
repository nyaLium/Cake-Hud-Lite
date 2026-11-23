package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/**
 * 「实体血条设置」（1.20.19 合并原通用/敌人/玩家；1.20.20 修正滚动并加滚动条）。
 *
 * <p>单列 + 分组标题；内容超出可视区时滚轮滚动，右侧显示滚动条并夹住上下限。</p>
 */
public final class EntityHudSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 26;
    private static final int HEADER_HEIGHT = 26;
    private static final int TOP = 52;
    private static final int BOTTOM_PAD = 56;
    private static final int WIDTH_MAX = 320;

    private final HudConfigScreen menu;
    private ScrollPane pane = new ScrollPane(TOP, 1000);
    private final List<Row> rows = new ArrayList<>();
    /** 滚动区里的控件：只登记输入（{@code addWidget}），绘制由 {@link #paintContent} 裁剪。 */
    private final List<AbstractWidget> contentRows = new ArrayList<>();

    public EntityHudSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.section.entity"));
        this.menu = menu;
    }

    private record Row(Component label, int y, int height, Kind kind, Object payload) {}

    private enum Kind { HEADER, TOGGLE, SLIDER }

    /** group：0 = 通用，1 = 敌人，2 = 玩家（父开关关掉时灰掉）。 */
    private record ToggleSpec(BooleanSupplier get, Consumer<Boolean> set, int group) {}

    /**
     * 滑块：{@code min/max/step} + 当前值 supplier。
     *
     * <p>1.20.33 起 value 是 {@link DoubleSupplier} 而不是快照值 —— 行表只建一次，
     * 以前滚动/开关导致 {@code rebuildWidgets()} 时会用**建表时**的旧值重建滑块，
     * 滑条位置会弹回去（值没丢，但看起来像被改了）。</p>
     */
    private record SliderSpec(double min, double max, double step, DoubleSupplier value,
                              DoubleFunction<Component> label, DoubleConsumer onChange,
                              int group) {}

    @Override
    protected void init() {
        buildRows();
        this.pane.contentHeight(lastY);
        rebuildWidgets();
    }

    private int lastY;

    private void buildRows() {
        if (!rows.isEmpty()) return;
        int y = 0;
        y = header("ctt_hud_fork.config.header.general", y);
        y = toggle("textShadow", y, 0, () -> HudConfig.get().textShadow, v -> HudConfig.get().textShadow = v);
        y = toggle("hideBelowNameScore", y, 0, () -> HudConfig.get().hideBelowNameScore,
                v -> HudConfig.get().hideBelowNameScore = v);
        y = toggle("hideEnemyBossBar", y, 0, () -> HudConfig.get().hideEnemyBossBar,
                v -> HudConfig.get().hideEnemyBossBar = v);
        y = toggle("hideTeamBossBar", y, 0, () -> HudConfig.get().hideTeamBossBar,
                v -> HudConfig.get().hideTeamBossBar = v);
        y = slider("opacity", y, 50, 100, 1, () -> HudConfig.get().opacity * 100,
                v -> key("opacity", Math.round(v) + "%"),
                v -> HudConfig.get().opacity = v / 100.0, 0);
        y = header("ctt_hud_fork.config.header.enemy", y);
        y = toggle("showBar", y, 0, () -> HudConfig.get().showBar, v -> HudConfig.get().showBar = v);
        y = slider("maxDistance", y, 4, 128, 4, () -> HudConfig.get().maxDistance,
                v -> key("maxDistance", (int) Math.round(v)), v -> HudConfig.get().maxDistance = v, 1);
        y = slider("barWidth", y, 50, 150, 1, () -> HudConfig.get().barWidth,
                v -> key("barWidth", (int) Math.round(v)), v -> HudConfig.get().barWidth = v, 1);
        y = slider("scale", y, 0.75, 1.5, 0.05, () -> HudConfig.get().scale,
                v -> key("scale", String.format(Locale.ROOT, "%.2f", v)), v -> HudConfig.get().scale = v, 1);
        y = slider("verticalOffset", y, -30, 0, 1, () -> HudConfig.get().verticalOffset,
                v -> key("verticalOffset", signed((int) Math.round(v))),
                v -> HudConfig.get().verticalOffset = (int) Math.round(v), 1);
        // 1.20.35：「优先使用服务端数据」写死开启，界面不再提供开关。
        y = header("ctt_hud_fork.config.header.player", y);
        // 1.20.34：玩家那边只有一个「显示血条」＝整块开关（关掉后血条和文字都不画）。
        y = toggle("showTeammates", y, 0, () -> HudConfig.get().showTeammates,
                v -> HudConfig.get().showTeammates = v);
        y = slider("teamMaxDistance", y, 4, 128, 4, () -> HudConfig.get().teamMaxDistance,
                v -> key("maxDistance", (int) Math.round(v)), v -> HudConfig.get().teamMaxDistance = v, 2);
        y = slider("teamBarWidth", y, 50, 150, 1, () -> HudConfig.get().teamBarWidth,
                v -> key("barWidth", (int) Math.round(v)), v -> HudConfig.get().teamBarWidth = v, 2);
        y = slider("teamScale", y, 0.75, 1.5, 0.05, () -> HudConfig.get().teamScale,
                v -> key("scale", String.format(Locale.ROOT, "%.2f", v)), v -> HudConfig.get().teamScale = v, 2);
        y = slider("teamVerticalOffset", y, -30, 0, 1, () -> HudConfig.get().teamVerticalOffset,
                v -> key("verticalOffset", signed((int) Math.round(v))),
                v -> HudConfig.get().teamVerticalOffset = (int) Math.round(v), 2);
        lastY = y;
    }

    private int header(String key, int y) {
        rows.add(new Row(Component.translatable(key), y, HEADER_HEIGHT, Kind.HEADER, null));
        return y + HEADER_HEIGHT;
    }

    private int toggle(String key, int y, int group, BooleanSupplier get, Consumer<Boolean> set) {
        rows.add(new Row(key(key), y, ROW_HEIGHT, Kind.TOGGLE, new ToggleSpec(get, set, group)));
        return y + ROW_STEP;
    }

    private int slider(String key, int y, double min, double max, double step, DoubleSupplier value,
                       DoubleFunction<Component> label, DoubleConsumer onChange, int group) {
        rows.add(new Row(key(key), y, ROW_HEIGHT, Kind.SLIDER,
                new SliderSpec(min, max, step, value, label, onChange, group)));
        return y + ROW_STEP;
    }

    @Override
    public void rebuildWidgets() {
        int keepScroll = this.pane.scroll();
        this.pane = new ScrollPane(TOP, this.height - BOTTOM_PAD);
        this.pane.contentHeight(lastY);
        this.pane.scrollBy(keepScroll);
        clearWidgets();
        this.contentRows.clear();
        HudConfig.Cfg cfg = HudConfig.get();
        boolean enemyActive = cfg.showBar;
        boolean playerActive = cfg.showTeammates;
        int width = Math.max(120, Math.min(WIDTH_MAX, this.width - 60));
        int x = this.width / 2 - width / 2;
        for (Row row : rows) {
            if (row.kind == Kind.HEADER) continue;
            // 整行都在可视区外的行不建控件（省得屏幕外还能点到）
            if (!this.pane.intersects(row.y, row.height)) continue;
            int y = this.pane.screenY(row.y);
            AbstractWidget widget = row.kind == Kind.TOGGLE ? createToggle(row, x, y, width)
                    : createSlider(row, x, y, width);
            // 父开关关掉时，属于它的那些行灰掉（开关和滑块一起处理，1.20.33）
            int group = row.kind == Kind.TOGGLE
                    ? ((ToggleSpec) row.payload).group()
                    : ((SliderSpec) row.payload).group();
            if (group == 1 && !enemyActive) widget.active = false;
            if (group == 2 && !playerActive) widget.active = false;
            addWidget(widget);
            this.contentRows.add(widget);
        }
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private AbstractWidget createToggle(Row row, int x, int y, int width) {
        ToggleSpec spec = (ToggleSpec) row.payload;
        return CycleButton.onOffBuilder(spec.get().getAsBoolean())
                .create(x, y, width, ROW_HEIGHT, row.label, (b, v) -> {
                    spec.set().accept(v);
                    this.rebuildWidgets();
                });
    }

    private AbstractWidget createSlider(Row row, int x, int y, int width) {
        SliderSpec spec = (SliderSpec) row.payload;
        return new ValueSlider(x, y, width, ROW_HEIGHT, spec.min(), spec.max(), spec.step(),
                spec.value().getAsDouble(), spec.label(), spec.onChange());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        paintContent(graphics, mouseX, mouseY, partialTick);
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

    /**
     * 画滚动区内容：在 {@code [top, bottom)} 里做真裁剪，滚到边界时最后一行被切掉，
     * 不会盖到下面的提示文本 / 「返回」按钮，更不会画到屏幕外。
     */
    private void paintContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.pane.beginClip(graphics, this.width);
        for (Row row : rows) {
            if (row.kind != Kind.HEADER || !this.pane.intersects(row.y, row.height)) continue;
            graphics.centeredText(this.font, row.label, this.width / 2, this.pane.screenY(row.y) + 8, 0xFFFFD060);
        }
        for (AbstractWidget widget : this.contentRows) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.pane.endClip(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.pane.scrollBy(-(int) Math.round(scrollY * ROW_STEP));
        this.rebuildWidgets();
        return true;
    }

    // ── 滚动条：可点可拖（1.20.32） ──

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
        // 按在滑块上：保持抓取偏移；按在轨道空白处：把滑块中心跳过来
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
        this.minecraft.gui.setScreen(this.menu);
    }

    @Override
    public void onClose() {
        back();
    }

    private static Component key(String id) {
        return Component.translatable("ctt_hud_fork.config." + id);
    }

    private static Component key(String id, Object... args) {
        return Component.translatable("ctt_hud_fork.config." + id, args);
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : Integer.toString(v);
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
