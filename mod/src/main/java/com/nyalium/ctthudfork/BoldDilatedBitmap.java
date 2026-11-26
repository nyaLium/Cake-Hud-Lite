package com.nyalium.ctthudfork;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;

/**
 * 一个「膨胀字形」的位图描述：尺寸比原字形四周各大 {@code margin} 纹素，基准点相应外移，
 * 于是<b>上屏位置与原字形完全一致</b>，只是多了一圈加粗的像素。
 *
 * <p>所有几何量都从原 {@link GlyphBitmap} 推导（{@code oversample} / {@code getLeft()} /
 * {@code getTop()}），所以位图字体和 oversample=3 的 TTF 都对得上：
 * {@code FontTexture.add} 会按 {@link #getPixelWidth()} 重新分槽，再用
 * {@link #getLeft()} / {@link #getTop()} 算四边形边界与 UV。</p>
 */
public final class BoldDilatedBitmap implements GlyphBitmap {
    private final GlyphBitmap base;
    private final BoldPixelSource pixels;

    public BoldDilatedBitmap(GlyphBitmap base, BoldPixelSource pixels) {
        this.base = base;
        this.pixels = pixels;
    }

    @Override
    public int getPixelWidth() {
        return this.base.getPixelWidth() + 2 * this.margin();
    }

    @Override
    public int getPixelHeight() {
        return this.base.getPixelHeight() + 2 * this.margin();
    }

    @Override
    public float getOversample() {
        return this.base.getOversample();
    }

    @Override
    public boolean isColored() {
        return this.base.isColored();
    }

    @Override
    public float getLeft() {
        return this.base.getLeft() - this.margin() / this.getOversample();
    }

    @Override
    public float getTop() {
        return this.base.getTop() - this.margin() / this.getOversample();
    }

    @Override
    public float getBearingLeft() {
        return this.base.getBearingLeft() + this.margin() / this.getOversample();
    }

    @Override
    public float getBearingTop() {
        return this.base.getBearingTop() + this.margin() / this.getOversample();
    }

    /** 四周各扩多少纹素（随 oversample 与当前强度变化）。 */
    private int margin() {
        return BoldDilation.marginFor(this.base.getOversample(), BoldDilation.strength());
    }

    @Override
    public void upload(int x, int y, GpuTexture texture) {
        NativeImage source = this.pixels.render();
        if (source == null) {
            // 让上层走降级（FontSet 会给回 missing 字形，BoldGlyphPair 会识破并退回双画）
            throw new IllegalStateException("bold dilation pixel source unavailable");
        }

        NativeImage dilated = null;

        try {
            dilated = BoldDilation.dilate(source, BoldDilation.strength(), this.getOversample());
            if (dilated == null) {
                throw new IllegalStateException("bold dilation unsupported for this glyph");
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, dilated, 0, 0, x, y);
        } finally {
            // 原版 TTF 的 upload 也是写完立刻 close —— writeToTexture 会把字节立刻交给后端
            closeQuietly(source);
            closeQuietly(dilated);
        }
    }

    private static void closeQuietly(NativeImage image) {
        if (image == null) {
            return;
        }

        try {
            image.close();
        } catch (Throwable ignored) {
            // 已经关过就算了
        }
    }
}
