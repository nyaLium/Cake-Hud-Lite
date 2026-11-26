package com.nyalium.ctthudfork;

import com.mojang.blaze3d.font.GlyphInfo;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

/**
 * 一个字形 + 它的「粗体膨胀变体」，按 {@link Style#isBold()} 二选一。
 *
 * <p>挂接点在 {@code BitmapProvider.Glyph#bake}：原版 bake 完普通字形后，这里再包一层，
 * 膨胀变体<b>按需</b>生成（第一次真的画到粗体时才膨胀 + 上图集），所以没用粗体的界面
 * 完全零开销、图集也不会翻倍。</p>
 *
 * <h2>为什么把 style 的 bold 抹掉</h2>
 * <p>原版「画两遍」的判断在 {@code BakedSheetGlyph.GlyphInstance.renderChar} 里：
 * {@code if (style.isBold()) 再画一遍}。膨胀变体的贴图本身已经更粗，所以交给它的是一个
 * {@code bold=false} 的 style，只画一遍；而<b>排版宽度</b>来自 {@code GlyphInfo.getAdvance(bold)}，
 * 走的是 {@link #info()}（原样转发），仍然是粗体宽度 —— 文字位置、居中、换行都不变。</p>
 */
public final class BoldGlyphPair implements BakedGlyph {
    private final BakedGlyph normal;
    private final Supplier<@Nullable BakedGlyph> dilatedSupplier;
    private final int generation = BoldDilation.generation();

    /** 当前膨胀结果是用哪一版参数做的；-1 = 还没做过。 */
    private int params = -1;
    private @Nullable BakedGlyph dilated;

    private @Nullable Style lastBoldStyle;
    private @Nullable Style lastNonBoldStyle;

    public BoldGlyphPair(BakedGlyph normal, Supplier<@Nullable BakedGlyph> dilatedSupplier) {
        this.normal = normal;
        this.dilatedSupplier = dilatedSupplier;
    }

    @Override
    public GlyphInfo info() {
        return this.normal.info();
    }

    @Override
    public TextRenderable.@Nullable Styled createGlyph(
        float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset
    ) {
        if (style != null && style.isBold() && BoldDilation.enabled()) {
            if (BoldDilation.dilationMode()) {
                BakedGlyph thick = this.dilated();
                if (thick != null) {
                    return thick.createGlyph(
                        x, y, color, shadowColor, this.nonBold(style), 0.0F, shadowOffset
                    );
                }
            } else {
                return this.normal.createGlyph(x, y, color, shadowColor, style, BoldDilation.offset(), shadowOffset);
            }
        }

        return this.normal.createGlyph(x, y, color, shadowColor, style, boldOffset, shadowOffset);
    }

    private @Nullable BakedGlyph dilated() {
        // 图集重载过：旧的 stitcher 已经 reset，再去 stitch 就是在往已释放的纹理里写
        if (this.generation != BoldDilation.generation()) {
            return null;
        }

        int version = BoldDilation.paramsVersion();
        if (this.params == version) {
            return this.dilated;
        }

        this.params = version;

        try {
            BakedGlyph baked = this.dilatedSupplier.get();

            // stitch 失败时 FontSet 会给回 missing 字形，那不是膨胀成功
            if (baked instanceof net.minecraft.client.gui.font.glyphs.BakedSheetGlyph) {
                this.dilated = baked;
                BoldDilation.noteFirstGlyph();
            } else {
                this.dilated = null;
            }
        } catch (Throwable t) {
            BoldDilation.disable("膨胀字形生成失败", t);
            this.dilated = null;
        }

        return this.dilated;
    }

    /** 同 style 只去掉 bold；样式对象不可变，缓存最后一个避免每条文本都新建。 */
    private Style nonBold(Style style) {
        if (this.lastBoldStyle == style && this.lastNonBoldStyle != null) {
            return this.lastNonBoldStyle;
        }

        Style result = style.withBold(false);
        this.lastBoldStyle = style;
        this.lastNonBoldStyle = result;
        return result;
    }
}
