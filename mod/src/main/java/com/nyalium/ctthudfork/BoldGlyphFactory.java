package com.nyalium.ctthudfork;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.jspecify.annotations.Nullable;

/**
 * 「膨胀变体」的延迟生成器 —— 第一次真的需要画粗体时才动手。
 *
 * <p>由两个字形提供者的 mixin 在 {@code bake} 里建好，交给 {@link BoldGlyphPair}。
 * 真正执行时会：从 {@link BoldPixelSource} 取回原字形像素 → 做掩膜膨胀 →
 * 走<b>原版同一个</b> {@link UnbakedGlyph.Stitcher} 上图集、分槽、算 UV，
 * 于是拿到的 {@code BakedSheetGlyph} 和原版字形完全同构，只是像素更粗。</p>
 *
 * <p>不用 lambda 是刻意的：mixin 注入的字节码里带 lambda 会多一层不确定性，
 * 这点成本换稳定。</p>
 */
public final class BoldGlyphFactory implements Supplier<@Nullable BakedGlyph> {
    private final UnbakedGlyph.Stitcher stitcher;
    private final GlyphInfo info;
    private final GlyphBitmap bitmap;
    private final BoldPixelSource pixels;

    public BoldGlyphFactory(
        UnbakedGlyph.Stitcher stitcher, GlyphInfo info, GlyphBitmap bitmap, BoldPixelSource pixels
    ) {
        this.stitcher = stitcher;
        this.info = info;
        this.bitmap = bitmap;
        this.pixels = pixels;
    }

    @Override
    public @Nullable BakedGlyph get() {
        return this.stitcher.stitch(this.info, new BoldDilatedBitmap(this.bitmap, this.pixels));
    }
}
