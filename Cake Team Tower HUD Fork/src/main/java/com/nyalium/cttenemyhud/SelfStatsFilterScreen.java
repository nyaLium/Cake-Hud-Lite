package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「内容过滤」界面（1.20.5）：<b>可滚动的属性列表</b>，每一项都能单独关掉。
 *
 * <p>列表里是 {@link SelfStatsKind#FILTERABLE} 的全部属性（四色心、碎/粉心、最大生命、
 * 各类倍率、力量/攻速/攻击距离/耐力/弓术、防御/真伤、七种元素护甲、速度/跳跃、
 * 法力、充能/召唤/体型/重力/骷髅…、状态效果、计时）。点击一行 = 显示/隐藏，
 * 滚轮滚动；右边有一条细滚动条。</p>
 *
 * <p>列表的命中判定与滚动量都是纯函数（{@link #visibleRows} / {@link #maxScroll} /
 * {@link #clampScroll} / {@link #rowAt} / {@link #firstIndex}），selfCheck 直接打。</p>
 */
public final class SelfStatsFilterScreen extends Screen {

    /** 一行的高度（GUI 像素）。 */
    static final int ROW_HEIGHT = 18;
    /** 列表上边（GUI 像素）：标题 + 统计 + 提示占掉的部分。 */
    static final int TOP = 58;
    /** 底部留给按钮的高度。 */
    static final int BOTTOM_PAD = 32;
    /** 左边距 / 右边距。 */
    static final int SIDE = 12;
    /** 滚动条宽度。 */
    static final int SCROLLBAR_WIDTH = 3;

    private final EnemyHudSettingsScreen parent;
    private List<String> hidden;
    private int scroll;

    public SelfStatsFilterScreen(EnemyHudSettingsScreen parent) {
        super(Component.translatable("ctt_enemy_hud.config.selfStatsFilter"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.hidden = EnemyHudConfig.hiddenList();
        this.scroll = clampScroll(this.scroll, maxScroll(SelfStatsKind.FILTERABLE.length, this.visibleRows()));

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

    private void back() {
        this.minecraft.gui.setScreen(this.parent);
    }

    // ── 布局（纯函数，selfCheck 直接打） ──

    /** 列表现有高度能放几行。 */
    static int visibleRows(int listHeight, int rowHeight) {
        if (rowHeight <= 0 || listHeight <= 0) return 0;
        return listHeight / rowHeight;
    }

    /** 最大滚动量（0 = 不需要滚）。 */
    static int maxScroll(int rowCount, int visibleRows) {
        return Math.max(0, rowCount - Math.max(0, visibleRows));
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

    /** 第一个可见项在总列表里的下标。 */
    static int firstIndex(int scroll) {
        return Math.max(0, scroll);
    }

    /**
     * 鼠标落在列表第几行（0 = 第一可见行）；不在列表区域内返回 -1。
     *
     * @param mouseY      鼠标 Y（GUI 像素）
     * @param listTop     列表上边
     * @param rowHeight   行高
     * @param visibleRows 可见行数
     */
    static int rowAt(double mouseY, int listTop, int rowHeight, int visibleRows) {
        if (rowHeight <= 0 || visibleRows <= 0) return -1;
        double offset = mouseY - listTop;
        if (offset < 0) return -1;
        int row = (int) (offset / rowHeight);
        return row < visibleRows ? row : -1;
    }

    private int listTop() {
        return TOP;
    }

    private int listBottom() {
        return this.height - BOTTOM_PAD;
    }

    private int visibleRows() {
        return visibleRows(listBottom() - listTop(), ROW_HEIGHT);
    }

    private int maxScroll() {
        return maxScroll(SelfStatsKind.FILTERABLE.length, visibleRows());
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

        SelfStatsKind[] kinds = SelfStatsKind.FILTERABLE;
        int visible = visibleRows();
        int first = firstIndex(this.scroll);
        int width = this.width - SIDE * 2 - SCROLLBAR_WIDTH - 2;
        for (int row = 0; row < visible; row++) {
            int index = first + row;
            if (index >= kinds.length) break;
            SelfStatsKind kind = kinds[index];
            int y = listTop() + row * ROW_HEIGHT;
            boolean hovered = mouseX >= SIDE && mouseX <= SIDE + width
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean shown = !this.hidden.contains(kind.id);
            int background = hovered ? 0x50FFFFFF : (row % 2 == 0 ? 0x40000000 : 0x28000000);
            graphics.fill(SIDE, y, SIDE + width, y + ROW_HEIGHT - 1, background);
            graphics.text(this.font, Component.literal(shown ? "\u2714" : "\u2718"),
                    SIDE + 4, y + 5, shown ? 0xFF55FF55 : 0xFFFF5555, true);
            graphics.text(this.font, label(kind), SIDE + 18, y + 5,
                    shown ? 0xFFFFFFFF : 0xFF8A8A8A, true);
        }

        // 滚动条
        int trackTop = listTop();
        int trackHeight = visible * ROW_HEIGHT;
        if (trackHeight > 0) {
            int x = SIDE + width + 3;
            graphics.fill(x, trackTop, x + SCROLLBAR_WIDTH, trackTop + trackHeight, 0x30000000);
            int total = kinds.length;
            if (total > visible && visible > 0) {
                int thumbHeight = Math.max(8, trackHeight * visible / total);
                int span = trackHeight - thumbHeight;
                int offset = maxScroll() == 0 ? 0 : span * this.scroll / maxScroll();
                graphics.fill(x, trackTop + offset, x + SCROLLBAR_WIDTH,
                        trackTop + offset + thumbHeight, 0xC0FFFFFF);
            }
        }
    }

    // ── 输入 ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int row = rowAt(event.y(), listTop(), ROW_HEIGHT, visibleRows());
        if (row >= 0) {
            int index = firstIndex(this.scroll) + row;
            if (index < SelfStatsKind.FILTERABLE.length) {
                SelfStatsKind kind = SelfStatsKind.FILTERABLE[index];
                if (this.hidden.contains(kind.id)) {
                    this.hidden.remove(kind.id);
                } else {
                    this.hidden.add(kind.id);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int before = this.scroll;
        this.scroll = scrollByWheel(this.scroll, scrollY, maxScroll());
        return this.scroll != before || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        back();
    }

    private static Component label(SelfStatsKind kind) {
        return Component.translatable("ctt_enemy_hud.config.kind." + kind.id);
    }
}
