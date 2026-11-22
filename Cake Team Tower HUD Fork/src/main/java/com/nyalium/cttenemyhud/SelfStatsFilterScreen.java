package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.List;

/**
 * 「内容过滤」界面（1.20.6）：<b>双列按钮 + 滚动</b>，每个属性一个按钮。
 *
 * <p>1.20.5 的列表没有边框，容易点岔；现在改成两列真正的 {@link Button}（原版按钮纹理 =
 * 有边框、有悬停高亮、可聚焦），一屏放不下就滚轮翻行。列表项是
 * {@link SelfStatsKind#FILTERABLE} 的全部属性（四色心、碎/粉心、最大生命、各类倍率、
 * 力量/攻速/攻击距离/耐力/弓术、防御/真伤、七种元素护甲、速度/跳跃、法力、
 * 充能/召唤/体型/重力/骷髅、钓鱼/蜂/蚁/因果…、状态效果、计时）。</p>
 *
 * <p>布局与滚动的数学都是纯函数（{@link #rowCount} / {@link #visibleRows} /
 * {@link #maxScroll} / {@link #clampScroll} / {@link #scrollByWheel} / {@link #itemAt}），
 * selfCheck 直接打。</p>
 */
public final class SelfStatsFilterScreen extends Screen {

    /** 列数：两列按钮。 */
    static final int COLUMNS = 2;
    /** 单个按钮的目标宽度：和原版菜单按钮一致（200）。窄屏时按可用宽度收缩。 */
    static final int COLUMN_WIDTH = 200;
    /** 按钮高度。 */
    static final int ROW_HEIGHT = 20;
    /** 行间距。 */
    static final int ROW_GAP = 5;
    /** 列间距。 */
    static final int COLUMN_GAP = 8;
    /** 列表上边（GUI 像素）：标题 + 统计 + 提示占掉的部分。 */
    static final int TOP = 58;
    /** 底部留给按钮的高度。 */
    static final int BOTTOM_PAD = 34;
    /** 左右边距 + 滚动条宽度。 */
    static final int SIDE = 10;
    static final int SCROLLBAR_WIDTH = 3;

    private final EnemyHudSettingsScreen parent;
    private List<String> hidden;
    /** 第一个可见<b>行</b>（一行 = 两个属性）。 */
    private int scroll;

    public SelfStatsFilterScreen(EnemyHudSettingsScreen parent) {
        super(Component.translatable("ctt_enemy_hud.config.selfStatsFilter"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.hidden = EnemyHudConfig.hiddenList();
        this.scroll = clampScroll(this.scroll, maxScroll());

        int width = columnWidth();
        int left = gridLeft();
        int rows = visibleRows(listHeight());
        SelfStatsKind[] kinds = SelfStatsKind.FILTERABLE;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int index = itemAt(this.scroll + row, column, COLUMNS, kinds.length);
                if (index < 0) continue;
                SelfStatsKind kind = kinds[index];
                int x = left + column * (width + COLUMN_GAP);
                int y = TOP + row * (ROW_HEIGHT + ROW_GAP);
                addRenderableWidget(Button.builder(label(kind), b -> toggle(kind))
                        .bounds(x, y, width, ROW_HEIGHT).build());
            }
        }

        int buttonY = this.height - 26;
        int buttonWidth = Math.max(60, Math.min(120, (this.width - 24) / 2));
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.selfStatsFilter.showAll"), b -> {
            this.hidden.clear();
            this.scroll = 0;
            this.rebuildWidgets();
        }).bounds(this.width / 2 - buttonWidth - 4, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(this.width / 2 + 4, buttonY, buttonWidth, 20).build());
    }

    private void toggle(SelfStatsKind kind) {
        if (this.hidden.contains(kind.id)) {
            this.hidden.remove(kind.id);
        } else {
            this.hidden.add(kind.id);
        }
        this.rebuildWidgets();
    }

    private void back() {
        this.minecraft.gui.setScreen(this.parent);
    }

    // ── 布局（纯函数，selfCheck 直接打） ──

    /** 需要几行（一行放 {@code columns} 个）。 */
    static int rowCount(int items, int columns) {
        if (items <= 0 || columns <= 0) return 0;
        return (items + columns - 1) / columns;
    }

    /** 列表高度能放几行（含行间距）。 */
    static int visibleRows(int listHeight) {
        if (listHeight <= 0) return 0;
        return (listHeight + ROW_GAP) / (ROW_HEIGHT + ROW_GAP);
    }

    /** 最大滚动量（单位：行）。 */
    static int maxScroll(int rows, int visibleRows) {
        return Math.max(0, rows - Math.max(0, visibleRows));
    }

    /** 把滚动量夹进 [0, maxScroll]。 */
    static int clampScroll(int scroll, int maxScroll) {
        return Math.max(0, Math.min(Math.max(0, maxScroll), scroll));
    }

    /** 滚轮：delta &gt; 0 = 往上滚；换算成行并夹取。 */
    static int scrollByWheel(int scroll, double delta, int maxScroll) {
        int rows = (int) Math.round(-delta);
        if (rows == 0 && delta != 0.0) rows = delta > 0 ? -1 : 1;
        return clampScroll(scroll + rows, maxScroll);
    }

    /**
     * 第 {@code row} 行第 {@code column} 列对应第几个属性；超出范围返回 -1。
     */
    static int itemAt(int row, int column, int columns, int items) {
        if (row < 0 || column < 0 || column >= columns) return -1;
        int index = row * columns + column;
        return index < items ? index : -1;
    }

    private int listHeight() {
        return this.height - BOTTOM_PAD - TOP;
    }

    /** 单个按钮宽度：目标 200（原版），屏幕窄时按可用宽度收缩。 */
    private int columnWidth() {
        return columnWidth(this.width);
    }

    /** 纯函数版：屏幕宽度 → 单列按钮宽度（≤ {@link #COLUMN_WIDTH}）。 */
    static int columnWidth(int screenWidth) {
        int avail = (screenWidth - SIDE * 2 - SCROLLBAR_WIDTH - 2 - COLUMN_GAP) / COLUMNS;
        return Math.max(60, Math.min(COLUMN_WIDTH, avail));
    }

    /** 两列整体居中后的左边起点。 */
    private int gridLeft() {
        int total = columnWidth() * COLUMNS + COLUMN_GAP;
        return Math.max(SIDE, (this.width - total) / 2);
    }

    private int visibleRows() {
        return visibleRows(listHeight());
    }

    private int maxScroll() {
        return maxScroll(rowCount(SelfStatsKind.FILTERABLE.length, COLUMNS), visibleRows());
    }

    // ── 绘制 ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.title"),
                this.width / 2, 10, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFD060);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.selfStatsFilter.count",
                        SelfStatsKind.FILTERABLE.length, this.hidden.size()),
                this.width / 2, 38, 0xFF9FE8FF);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.selfStatsFilter.scrollHint"),
                this.width / 2, 48, 0xFFA0A0A0);

        // 右侧滚动条（贴着两列按钮的右边）
        int visible = visibleRows();
        int trackTop = TOP;
        int trackHeight = visible * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        if (trackHeight > 0) {
            int x = gridLeft() + columnWidth() * COLUMNS + COLUMN_GAP + 4;
            graphics.fill(x, trackTop, x + SCROLLBAR_WIDTH, trackTop + trackHeight, 0x30000000);
            int rows = rowCount(SelfStatsKind.FILTERABLE.length, COLUMNS);
            if (rows > visible && visible > 0) {
                int thumbHeight = Math.max(8, trackHeight * visible / rows);
                int span = trackHeight - thumbHeight;
                int offset = maxScroll() == 0 ? 0 : span * this.scroll / maxScroll();
                graphics.fill(x, trackTop + offset, x + SCROLLBAR_WIDTH,
                        trackTop + offset + thumbHeight, 0xC0FFFFFF);
            }
        }
    }

    // ── 输入 ──

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int before = this.scroll;
        this.scroll = scrollByWheel(this.scroll, scrollY, maxScroll());
        if (this.scroll != before) {
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        back();
    }

    private Component label(SelfStatsKind kind) {
        boolean shown = !this.hidden.contains(kind.id);
        MutableComponent mark = Component.literal(shown ? "\u2714 " : "\u2718 ")
                .withStyle(style -> style.withColor(shown
                        ? TextColor.fromRgb(0x55FF55) : TextColor.fromRgb(0xFF5555)));
        MutableComponent name = Component.translatable("ctt_enemy_hud.config.kind." + kind.id)
                .withStyle(style -> style.withColor(shown
                        ? TextColor.fromRgb(0xFFFFFF) : TextColor.fromRgb(0x8A8A8A)));
        return mark.append(name);
    }
}
