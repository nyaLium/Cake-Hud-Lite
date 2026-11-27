package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 可滚动列表的公共计算（1.20.20 抽出，给实体血条设置 / 属性面板设置共用）。
 *
 * <p>可视区 = {@code [top, bottom)}；内容从 0 开始排。滚动量夹在
 * {@code [0, contentHeight - viewportHeight]}。滚动条画在右侧 4px 宽。</p>
 *
 * <h2>滚动条可用鼠标点/拖</h2>
 * <p>以前滚动条只是画出来给人看的，点它没反应。现在这里提供几何 + 命中判定 + 反解：
 * 点轨道任意位置把滑块跳过去（<b>以光标为中心</b>，按下不跳位），按住滑块拖动实时跟手。
 * 拖动状态（是否正在拖、抓取偏移）留在各个界面里 —— 因为界面滚动时会
 * {@code rebuildWidgets()}，本对象会被重建，状态放这儿会丢。</p>
 */
final class ScrollPane {

    /** 滚动条宽度（像素）。 */
    static final int BAR_WIDTH = 4;
    /** 命中容差：4 px 的条太细，左右各放宽这么多像素才抓得住（1.20.33 从 3 加到 5）。 */
    static final int BAR_HIT_PAD = 5;

    private final int top;
    private final int bottom;
    private int scroll;
    private int contentHeight;

    ScrollPane(int top, int bottom) {
        this.top = top;
        this.bottom = Math.max(top + 20, bottom);
    }

    int top() {
        return top;
    }

    int bottom() {
        return bottom;
    }

    int viewportHeight() {
        return bottom - top;
    }

    int scroll() {
        return scroll;
    }

    void contentHeight(int contentHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        scroll = clamp(scroll);
    }

    int maxScroll() {
        return Math.max(0, contentHeight - viewportHeight());
    }

    /** 内容坐标 → 屏幕 y。 */
    int screenY(int contentY) {
        return top + contentY - scroll;
    }

    /** 该行是否与可视区相交（含 1 行的余量）。 */
    boolean intersects(int contentY, int height) {
        int y = screenY(contentY);
        return y + height > top - 2 && y < bottom;
    }

    /**
     * 进入可视区裁剪。
     *
     * <p>内容控件不再交给 {@code Screen} 自动绘制，而是由界面自己在
     * {@code beginClip} / {@code endClip} 之间画 —— 这样滚到边界时最后一行被
     * <b>截断</b>，而不是盖到提示文本和「返回」按钮上（1.20.21 之前只是不创建
     * 完全越界的行，部分露出的行仍会画到屏幕外）。</p>
     */
    void beginClip(GuiGraphicsExtractor g, int width) {
        g.enableScissor(0, top, Math.max(0, width), bottom);
    }

    void endClip(GuiGraphicsExtractor g) {
        g.disableScissor();
    }

    void scrollBy(int deltaPixels) {
        scroll = clamp(scroll + deltaPixels);
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(maxScroll(), v));
    }

    // ── 滚动条几何 / 命中 ──

    /** 滚动条那一列的 x（调用方给一个「右侧锚点」）。 */
    int barX(int rightX) {
        return rightX - BAR_WIDTH;
    }

    /** 现在有没有滚动条（内容比可视区高才有）。 */
    boolean scrollbarVisible() {
        return maxScroll() > 0;
    }

    /** 滑块高度（可视区在内容里的占比，至少 12 px）。 */
    int thumbHeight() {
        return thumbHeight(viewportHeight(), contentHeight);
    }

    /** 滑块顶端相对可视区顶的偏移。 */
    int thumbOffset() {
        return thumbOffset(viewportHeight(), thumbHeight(), scroll, maxScroll());
    }

    /** 光标是否落在滚动条那一列上（含左右各 {@link #BAR_HIT_PAD} 的容差；纵向限在可视区内）。 */
    boolean hitsBar(double mouseX, double mouseY, int rightX) {
        if (!scrollbarVisible()) return false;
        int x = barX(rightX);
        return mouseX >= x - BAR_HIT_PAD && mouseX <= x + BAR_WIDTH + BAR_HIT_PAD
                && mouseY >= top && mouseY <= bottom;
    }

    /** 光标是否落在滑块上（纵向；调用方通常先过一遍 {@link #hitsBar}）。 */
    boolean hitsThumb(double mouseY) {
        if (!scrollbarVisible()) return false;
        int y = top + thumbOffset();
        return mouseY >= y - BAR_HIT_PAD && mouseY <= y + thumbHeight() + BAR_HIT_PAD;
    }

    /** 按下瞬间记录「滑块顶端相对光标」的偏移，拖动时滑块才不会跳到光标正中。 */
    int grabOffset(double mouseY) {
        int offset = (int) Math.round(mouseY) - (top + thumbOffset());
        return Math.max(0, Math.min(Math.max(0, thumbHeight() - 1), offset));
    }

    /** 拖动：把滑块顶端放到 {@code thumbTop}（屏幕坐标的反解交给纯函数）。 */
    void dragThumbTo(int thumbTop) {
        scroll = clamp(scrollForThumbTop(thumbTop - top, viewportHeight(), thumbHeight(), maxScroll()));
    }

    /**
     * 画右侧滚动条（轨道 + 滑块）；没有可滚动内容时什么都不画。
     *
     * <p>光标停在条上 / 正在拖时滑块提亮，告诉玩家「这条能抓」。</p>
     */
    void drawScrollbar(GuiGraphicsExtractor g, int rightX, double mouseX, double mouseY, boolean dragging) {
        if (!scrollbarVisible()) return;
        int x = barX(rightX);
        boolean hot = dragging || hitsBar(mouseX, mouseY, rightX);
        g.fill(x, top, x + BAR_WIDTH, bottom, hot ? 0x40000000 : 0x30000000);
        int thumb = thumbHeight();
        int offset = thumbOffset();
        g.fill(x, top + offset, x + BAR_WIDTH, top + offset + thumb,
                hot ? 0xFFFFFFFF : 0xC0FFFFFF);
    }

    // ── 纯函数（selfCheck 直接打） ──

    /** 滑块高度：可视区占比，至少 12 px。 */
    static int thumbHeight(int trackHeight, int totalContent) {
        if (trackHeight <= 0) return 0;
        return Math.max(12, trackHeight * trackHeight / Math.max(1, totalContent));
    }

    /** 滑块偏移：{@code scroll / maxScroll} 映射到 {@code [0, track - thumb]}。 */
    static int thumbOffset(int trackHeight, int thumbHeight, int scroll, int maxScroll) {
        int span = Math.max(0, trackHeight - Math.max(0, thumbHeight));
        if (maxScroll <= 0 || span == 0) return 0;
        return span * Math.max(0, Math.min(maxScroll, scroll)) / maxScroll;
    }

    /** 反解：滑块顶端偏移 → 滚动量（拖动时用）。 */
    static int scrollForThumbTop(int thumbTop, int trackHeight, int thumbHeight, int maxScroll) {
        int span = Math.max(0, trackHeight - Math.max(0, thumbHeight));
        if (maxScroll <= 0 || span == 0) return 0;
        int t = Math.max(0, Math.min(span, thumbTop));
        return (int) Math.round((double) t * maxScroll / span);
    }
}
