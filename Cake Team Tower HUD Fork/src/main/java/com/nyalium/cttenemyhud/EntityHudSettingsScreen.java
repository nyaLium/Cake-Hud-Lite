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
 * 「实体血条设置」（1.20.19 合并原「通用 / 敌人 / 玩家」三个子页面）。
 *
 * <pre>
 *   实体血条设置
 *   ── 通用 ──
 *   文字阴影 / 隐藏名字下方的分数 / 隐藏敌人 Boss 栏 / 隐藏队伍 Boss 栏 / 整体不透明度
 *   ── 敌人设置 ──
 *   显示血条 / 显示距离 / 血条长度 / 整体缩放 / 垂直偏移 / 优先服务端数据
 *   ── 玩家设置 ──
 *   显示队友血量 / 队友显示距离 / 队友血条长度 / 队友缩放 / 队友垂直偏移
 * </pre>
 *
 * <p>行数超过屏幕时用滚轮滚动（每次重建控件，只创建落在可视区内的行）；
 * 某个总开关关闭时，它下面那一组的控件全部 {@code active = false}。</p>
 */
public final class EntityHudSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 26;
    private static final int HEADER_HEIGHT = 24;
    private static final int TOP = 52;
    private static final int BOTTOM_PAD = 44;
    private static final int WIDTH_MAX = 320;

    private final EnemyHudConfigScreen menu;

    private int scroll;
    /** 每行/每个标题在内容坐标系里的 y（0 = 内容顶部）。 */
    private final List<Row> rows = new ArrayList<>();

    public EntityHudSettingsScreen(EnemyHudConfigScreen menu) {
        super(Component.translatable("ctt_enemy_hud.config.section.entity"));
        this.menu = menu;
    }

    /** 一行内容：header 只画字，其他控件在 rebuild 时按可视区创建。 */
    private record Row(Component label, int y, int height, Kind kind, Object payload) {}

    private enum Kind { HEADER, TOGGLE, SLIDER, BUTTON }

    @Override
    protected void init() {
        buildRows();
        rebuildWidgets();
    }

    private void back() {
        this.minecraft.gui.setScreen(this.menu);
    }

    /** 描述所有行（只描述一次；滚动时重建控件但不重建描述）。 */
    private void buildRows() {
        if (!rows.isEmpty()) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int y = 0;
        y = addHeader("ctt_enemy_hud.config.header.general", y);
        y = addToggle("textShadow", y, () -> EnemyHudConfig.get().textShadow, v -> EnemyHudConfig.get().textShadow = v);
        y = addToggle("hideBelowNameScore", y, () -> EnemyHudConfig.get().hideBelowNameScore, v -> EnemyHudConfig.get().hideBelowNameScore = v);
        y = addToggle("hideEnemyBossBar", y, () -> EnemyHudConfig.get().hideEnemyBossBar, v -> EnemyHudConfig.get().hideEnemyBossBar = v);
        y = addToggle("hideTeamBossBar", y, () -> EnemyHudConfig.get().hideTeamBossBar, v -> EnemyHudConfig.get().hideTeamBossBar = v);
        y = addSlider("opacity", y, 50.0, 100.0, 1.0, cfg.opacity * 100.0,
                v -> Component.translatable("ctt_enemy_hud.config.opacity", Math.round(v) + "%"),
                v -> EnemyHudConfig.get().opacity = v / 100.0);
        y = addHeader("ctt_enemy_hud.config.header.enemy", y);
        y = addToggle("showBar", y, () -> EnemyHudConfig.get().showBar, v -> EnemyHudConfig.get().showBar = v);
        y = addSlider("maxDistance", y, 4.0, 128.0, 4.0, cfg.maxDistance,
                v -> Component.translatable("ctt_enemy_hud.config.maxDistance", (int) Math.round(v)),
                v -> EnemyHudConfig.get().maxDistance = v);
        y = addSlider("barWidth", y, 50.0, 150.0, 1.0, cfg.barWidth,
                v -> Component.translatable("ctt_enemy_hud.config.barWidth", (int) Math.round(v)),
                v -> EnemyHudConfig.get().barWidth = v);
        y = addSlider("scale", y, 0.75, 1.5, 0.05, cfg.scale,
                v -> Component.translatable("ctt_enemy_hud.config.scale", String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().scale = v);
        y = addSlider("verticalOffset", y, -30.0, 0.0, 1.0, cfg.verticalOffset,
                v -> Component.translatable("ctt_enemy_hud.config.verticalOffset", signed((int) Math.round(v))),
                v -> EnemyHudConfig.get().verticalOffset = (int) Math.round(v));
        y = addToggle("useServerData", y, () -> EnemyHudConfig.get().useServerData, v -> EnemyHudConfig.get().useServerData = v);
        y = addHeader("ctt_enemy_hud.config.header.player", y);
        y = addToggle("showTeammates", y, () -> EnemyHudConfig.get().showTeammates, v -> EnemyHudConfig.get().showTeammates = v);
        y = addSlider("teamMaxDistance", y, 4.0, 128.0, 4.0, cfg.teamMaxDistance,
                v -> Component.translatable("ctt_enemy_hud.config.maxDistance", (int) Math.round(v)),
                v -> EnemyHudConfig.get().teamMaxDistance = v);
        y = addSlider("teamBarWidth", y, 50.0, 150.0, 1.0, cfg.teamBarWidth,
                v -> Component.translatable("ctt_enemy_hud.config.barWidth", (int) Math.round(v)),
                v -> EnemyHudConfig.get().teamBarWidth = v);
        y = addSlider("teamScale", y, 0.75, 1.5, 0.05, cfg.teamScale,
                v -> Component.translatable("ctt_enemy_hud.config.scale", String.format(Locale.ROOT, "%.2f", v)),
                v -> EnemyHudConfig.get().teamScale = v);
        y = addSlider("teamVerticalOffset", y, -30.0, 0.0, 1.0, cfg.teamVerticalOffset,
                v -> Component.translatable("ctt_enemy_hud.config.verticalOffset", signed((int) Math.round(v))),
                v -> EnemyHudConfig.get().teamVerticalOffset = (int) Math.round(v));
        contentHeight = y;
    }

    private int contentHeight;

    private int addHeader(String key, int y) {
        rows.add(new Row(Component.translatable(key), y, HEADER_HEIGHT, Kind.HEADER, key));
        return y + HEADER_HEIGHT;
    }

    private int addToggle(String key, int y, java.util.function.BooleanSupplier get,
                          java.util.function.Consumer<Boolean> set) {
        rows.add(new Row(Component.translatable("ctt_enemy_hud.config." + key), y, ROW_HEIGHT,
                Kind.TOGGLE, new ToggleSpec(get, set)));
        return y + ROW_STEP;
    }

    private record ToggleSpec(java.util.function.BooleanSupplier get,
                              java.util.function.Consumer<Boolean> set) {}

    private int addSlider(String key, int y, double min, double max, double step, double value,
                          DoubleFunction<Component> label, DoubleConsumer onChange) {
        rows.add(new Row(Component.translatable("ctt_enemy_hud.config." + key), y, ROW_HEIGHT,
                Kind.SLIDER, new SliderSpec(key, min, max, step, value, label, onChange)));
        return y + ROW_STEP;
    }

    private record SliderSpec(String key, double min, double max, double step, double value,
                              DoubleFunction<Component> label, DoubleConsumer onChange) {}

    private int maxScroll() {
        int visible = this.height - TOP - BOTTOM_PAD;
        return Math.max(0, contentHeight - visible);
    }

    @Override
    public void rebuildWidgets() {
        clearWidgets();
        // 先按当前滚动量创建可视区内的控件，再重建底部「返回」。
        int width = Math.min(WIDTH_MAX, this.width - 40);
        int x = this.width / 2 - width / 2;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        boolean enemyActive = cfg.showBar;
        boolean playerActive = cfg.showTeammates;
        for (Row row : rows) {
            if (row.kind == Kind.HEADER) continue;
            int y = TOP + row.y - scroll;
            if (y + ROW_HEIGHT < TOP - ROW_STEP || y > this.height - BOTTOM_PAD) continue;
            AbstractWidget widget = switch (row.kind) {
                case TOGGLE -> createToggle(row, x, y, width);
                case SLIDER -> createSlider(row, x, y, width);
                default -> null;
            };
            if (widget == null) continue;
            if (isEnemyKey(row) && !enemyActive) widget.active = false;
            if (isPlayerKey(row) && !playerActive) widget.active = false;
            addRenderableWidget(widget);
        }
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private static boolean isEnemyKey(Row row) {
        return row.kind == Kind.SLIDER && row.payload instanceof SliderSpec s
                ? !s.key().startsWith("team") && !s.key().equals("opacity") : false;
    }

    private static boolean isPlayerKey(Row row) {
        return row.kind == Kind.SLIDER && row.payload instanceof SliderSpec s
                ? s.key().startsWith("team") : false;
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
                spec.value(), spec.label(), spec.onChange());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 22, 0xFFFFFFFF);
        for (Row row : rows) {
            if (row.kind != Kind.HEADER) continue;
            int y = TOP + row.y - scroll;
            if (y + row.height < TOP - ROW_STEP || y > this.height - BOTTOM_PAD) continue;
            graphics.centeredText(this.font, row.label, this.width / 2, y + 6, 0xFFFFD060);
        }
        // 滚动提示
        if (maxScroll() > 0) {
            graphics.centeredText(this.font,
                    Component.translatable("ctt_enemy_hud.config.scrollHint"),
                    this.width / 2, this.height - ROW_HEIGHT - 26, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int next = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.round(scrollY * ROW_STEP)));
        if (next != this.scroll) {
            this.scroll = next;
            this.rebuildWidgets();
        }
        return true;
    }

    private static String signed(int v) {
        return v > 0 ? "+" + v : Integer.toString(v);
    }

    /** 简化版滑块（原 EnemyHudSettingsScreen 的内部类在这里重写一份）。 */
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
