package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 可滚动列表的公共计算（1.20.20 抽出，给实体血条设置 / 属性面板设置共用）。
 *
 * <p>可视区 = {@code [top, bottom)}；内容从 0 开始排。滚动量夹在
 * {@code [0, contentHeight - viewportHeight]}。滚动条画在右侧 4px 宽。
 */
final class ScrollPane {

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
     * 进入可视区裁剪（1.20.22）。
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

    /**
     * 画右侧滚动条（轨道 + 滑块）；没有可滚动内容时什么都不画。
     *
     * @return 滚动条占用的 x（调用方可据此留边）
     */
    int drawScrollbar(GuiGraphicsExtractor g, int rightX) {
        int max = maxScroll();
        if (max <= 0) return rightX;
        int x = rightX - 4;
        g.fill(x, top, x + 4, bottom, 0x30000000);
        int thumbHeight = Math.max(12, viewportHeight() * viewportHeight() / Math.max(1, contentHeight));
        int span = viewportHeight() - thumbHeight;
        int offset = max == 0 ? 0 : span * scroll / max;
        g.fill(x, top + offset, x + 4, top + offset + thumbHeight, 0xC0FFFFFF);
        return x - 4;
    }
}
