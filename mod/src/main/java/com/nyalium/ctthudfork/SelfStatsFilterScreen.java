package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 「内容过滤」界面（1.20.6 起双列按钮 + 滚动；<b>1.20.31 起分三节</b>；1.20.32 滚动条可点可拖）。
 *
 * <p>以前是「一大串属性 + 状态效果 + 其它」平铺：状态效果 28 条挤在一个按钮里、
 * 「其它」把草方块和认不出的行混在一起，太粗。现在按
 * {@link SelfStatsKind.Group} 分成三节，节与节之间画一条标题
 * （和实体血条设置里「── 通用 ── / ── 敌人设置 ──」是同一种观感）：</p>
 *
 * <pre>
 *   ── 属性 ──
 *   [✔ 红心]            [✔ 魂心]
 *   [✔ 黑心]            [✔ 蓝心]
 *   …
 *   ── 状态效果 ──
 *   [✔ 斗志昂扬]        [✔ 食物专家]
 *   …（28 条，一条一项）
 *   ── 其它 ──
 *   [✔ 游戏时间]        [✔ 难度骷髅]
 *   [✔ 草方块]          [✔ 未识别行]
 * </pre>
 *
 * <p>列表项仍是真正的 {@link Button}（原版按钮纹理 = 有边框、有悬停高亮、可聚焦）：
 * 一屏放不下就滚轮翻行，右侧滚动条可用鼠标点/拖（1.20.32）。标题行不占按钮位、不可点。</p>
 *
 * <p>布局与滚动的数学都是纯函数（{@link #buildRows} / {@link #visibleRows} /
 * {@link #maxScroll} / {@link #clampScroll} / {@link #scrollByWheel}），滚动条几何在
 * {@link ScrollPane}（{@code thumbHeight} / {@code thumbOffset} / {@code scrollForThumbTop}），
 * selfCheck 直接打。</p>
 */
public final class SelfStatsFilterScreen extends Screen {

    /** 列数：两列按钮。 */
    static final int COLUMNS = 2;
    /** 单个按钮的目标宽度：和原版菜单按钮一致（200）。窄屏时按可用宽度收缩。 */
    static final int COLUMN_WIDTH = 200;
    /** 按钮高度。 */
    static final int ROW_HEIGHT = 20;
    /** 行间距（标题行也用同样的步长）。 */
    static final int ROW_GAP = 5;
    /** 一行的纵向步长。 */
    static final int STEP = ROW_HEIGHT + ROW_GAP;
    /** 列间距。 */
    static final int COLUMN_GAP = 8;
    /** 列表上边（GUI 像素）：标题 + 名称 + 统计/提示合成一行，占掉的部分。 */
    static final int TOP = 54;
    /** 底部留给按钮的高度（底部按钮 20 高、贴 height-26；这里再留 14 px 呼吸）。 */
    static final int BOTTOM_PAD = 40;
    /** 左右边距 + 滚动条宽度。 */
    static final int SIDE = 10;
    static final int SCROLLBAR_WIDTH = 3;
    /** 滚动条命中容差：3 px 的条太细，左右各放宽这么多像素才抓得住（1.20.33 从 3 加到 5）。 */
    static final int SCROLLBAR_HIT_PAD = 5;
    /** 分组标题文字相对行顶的下移量（行高 20、字高 8 → 居中）。 */
    static final int HEADER_TEXT_DY = 6;
    /** 分组标题颜色（和设置界面里的分节标题一致）。 */
    static final int HEADER_COLOR = 0xFFFFD060;

    private final Screen parent;
    private List<String> hidden;
    /** 第一个可见<b>行</b>（一行 = 一个标题，或两个属性按钮）。 */
    private int scroll;
    /** 整个列表的行结构（不随滚动变化）。 */
    private final List<Row> rows = buildRows();

    /**
     * 列表里的一行。
     *
     * @param header 非 null = 这一行是分节标题（{@code left/right} 都为 null）
     * @param left   左边的项（null = 空位）
     * @param right  右边的项（null = 空位）
     */
    record Row(SelfStatsKind.Group header, SelfStatsKind left, SelfStatsKind right) {
        boolean isHeader() {
            return header != null;
        }
    }

    public SelfStatsFilterScreen(Screen parent) {
        super(Component.translatable("ctt_hud_fork.config.selfStatsFilter"));
        this.parent = parent;
    }

    /**
     * 把三节的项排成行（纯函数）。
     *
     * <p>每节先插一行标题，再把这一节的项两个一行地排；<b>不跨节合并</b>
     * （否则「状态效果」的第一项会和「属性」的最后一项挤在同一行，分节就白分了）。</p>
     */
    static List<Row> buildRows() {
        List<Row> out = new ArrayList<>();
        for (SelfStatsKind.Group group : SelfStatsKind.Group.values()) {
            SelfStatsKind[] members = SelfStatsKind.section(group);
            if (members.length == 0) continue;
            out.add(new Row(group, null, null));
            for (int i = 0; i < members.length; i += COLUMNS) {
                SelfStatsKind left = members[i];
                SelfStatsKind right = i + 1 < members.length ? members[i + 1] : null;
                out.add(new Row(null, left, right));
            }
        }
        return out;
    }

    @Override
    protected void init() {
        this.hidden = HudConfig.hiddenList();
        this.scroll = clampScroll(this.scroll, maxScroll());

        int width = columnWidth();
        int left = gridLeft();
        int visible = visibleRows();
        for (int row = 0; row < visible; row++) {
            int index = this.scroll + row;
            if (index >= this.rows.size()) break;
            Row line = this.rows.get(index);
            if (line.isHeader()) continue;
            int y = TOP + row * STEP;
            if (line.left() != null) {
                addRenderableWidget(button(line.left(), left, y, width));
            }
            if (line.right() != null) {
                addRenderableWidget(button(line.right(), left + width + COLUMN_GAP, y, width));
            }
        }

        int buttonY = this.height - 26;
        int buttonWidth = Math.max(60, Math.min(120, (this.width - 24) / 2));
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.selfStatsFilter.showAll"), b -> {
            this.hidden.clear();
            this.scroll = 0;
            this.rebuildWidgets();
        }).bounds(this.width / 2 - buttonWidth - 4, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 + 4, buttonY, buttonWidth, 20).build());
    }

    private Button button(SelfStatsKind kind, int x, int y, int width) {
        return Button.builder(label(kind), b -> toggle(kind)).bounds(x, y, width, ROW_HEIGHT).build();
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

    /** 列表高度能放几行（含行间距）。 */
    static int visibleRows(int listHeight) {
        if (listHeight <= 0) return 0;
        return (listHeight + ROW_GAP) / STEP;
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
        return maxScroll(this.rows.size(), visibleRows());
    }

    // ── 绘制 ──

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.title"),
                this.width / 2, 10, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFD060);
        // 1.20.24：计数与「滚轮/点击」提示合成一行，给列表多腾一行高度。
        Component countLine = Component.translatable("ctt_hud_fork.config.selfStatsFilter.count",
                        SelfStatsKind.FILTERABLE.length, this.hidden.size())
                .copy().append("  ·  ")
                .append(Component.translatable("ctt_hud_fork.config.selfStatsFilter.scrollHint"));
        graphics.centeredText(this.font, countLine, this.width / 2, 40, 0xFF9FE8FF);

        // 分组标题：只画可见的那几行（行结构不滚动，滚动只改起点）。
        int visible = visibleRows();
        for (int row = 0; row < visible; row++) {
            int index = this.scroll + row;
            if (index >= this.rows.size()) break;
            Row line = this.rows.get(index);
            if (!line.isHeader()) continue;
            graphics.centeredText(this.font, Component.translatable(line.header().headerKey),
                    this.width / 2, TOP + row * STEP + HEADER_TEXT_DY, HEADER_COLOR);
        }

        // 右侧滚动条（贴着两列按钮的右边）
        drawScrollbar(graphics, mouseX, mouseY);
    }

    // ── 滚动条（1.20.32：可点可拖，绘制与命中判定共用一套几何） ──

    /** 滚动条那一列的 x。 */
    private int trackLeft() {
        return gridLeft() + columnWidth() * COLUMNS + COLUMN_GAP + 4;
    }

    /** 轨道高度：可见行占的高度（去掉最后一行的间距）。 */
    private int trackHeight() {
        return Math.max(0, visibleRows() * STEP - ROW_GAP);
    }

    /** 滑块高度：可视行数占全部行数的比例，至少 8 px。 */
    private int thumbHeight() {
        return thumbHeight(trackHeight(), this.rows.size(), visibleRows());
    }

    /** 纯函数版（selfCheck 直接打）。 */
    static int thumbHeight(int trackHeight, int rows, int visible) {
        if (trackHeight <= 0 || rows <= 0 || visible <= 0) return 0;
        return Math.max(8, trackHeight * visible / rows);
    }

    /** 滑块顶端相对列表顶的偏移（行刻度）。 */
    private int thumbOffset() {
        return ScrollPane.thumbOffset(trackHeight(), thumbHeight(), this.scroll, maxScroll());
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        int track = trackHeight();
        if (track <= 0 || maxScroll() <= 0) return;
        int x = trackLeft();
        // 光标停在条上 / 正在拖时提亮（1.20.33），让人知道这条能抓
        boolean hot = this.barDragging || (hitsBar(mouseX, mouseY));
        graphics.fill(x, TOP, x + SCROLLBAR_WIDTH, TOP + track, hot ? 0x40000000 : 0x30000000);
        int thumb = thumbHeight();
        int offset = thumbOffset();
        graphics.fill(x, TOP + offset, x + SCROLLBAR_WIDTH, TOP + offset + thumb,
                hot ? 0xFFFFFFFF : 0xC0FFFFFF);
    }

    /** 光标是否落在滚动条那一列（含左右各 {@link #SCROLLBAR_HIT_PAD} 的容差；纵向限在轨道上）。 */
    private boolean hitsBar(double mouseX, double mouseY) {
        return maxScroll() > 0
                && mouseX >= trackLeft() - SCROLLBAR_HIT_PAD
                && mouseX <= trackLeft() + SCROLLBAR_WIDTH + SCROLLBAR_HIT_PAD
                && mouseY >= TOP && mouseY <= TOP + trackHeight();
    }

    private boolean hitsThumb(double mouseY) {
        int y = TOP + thumbOffset();
        return maxScroll() > 0
                && mouseY >= y - SCROLLBAR_HIT_PAD
                && mouseY <= y + thumbHeight() + SCROLLBAR_HIT_PAD;
    }

    private int grabOffset(double mouseY) {
        int offset = (int) Math.round(mouseY) - (TOP + thumbOffset());
        return Math.max(0, Math.min(Math.max(0, thumbHeight() - 1), offset));
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
        if (!hitsBar(gui[0], gui[1])) return false;
        // 按在滑块上：保持抓取偏移；按在轨道空白处：把滑块中心跳过来
        this.barDragging = true;
        this.barGrab = hitsThumb(gui[1]) ? grabOffset(gui[1]) : thumbHeight() / 2;
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
        int thumbTop = (int) Math.round(mouseY) - this.barGrab - TOP;
        this.scroll = clampScroll(ScrollPane.scrollForThumbTop(thumbTop, trackHeight(),
                thumbHeight(), maxScroll()), maxScroll());
        this.rebuildWidgets();
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
        // 1.20.16：内置翻译已改为官方中文（取自地图资源包），不再走 hover。
        MutableComponent name = Component.translatable("ctt_hud_fork.config.kind." + kind.id)
                .withStyle(style -> style.withColor(shown
                        ? TextColor.fromRgb(0xFFFFFF) : TextColor.fromRgb(0x8A8A8A)));
        return mark.append(name);
    }
}
