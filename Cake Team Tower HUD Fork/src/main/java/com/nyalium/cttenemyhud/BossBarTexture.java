package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * 原版 Bossbar 纹理调用封装 —— 敌人条与队友条走<b>同一条</b>绘制路径。
 *
 * <p><b>不打包任何材质</b>：直接用原版 GUI 图集里的
 * {@code minecraft:boss_bar/*_background} / {@code *_progress}（182×5），
 * 与 {@code BossHealthOverlay} 画 Boss 条用的是同一批 sprite，资源包替换会同步生效。</p>
 *
 * <p>走 {@code blitSprite(pipeline, id, x, y, w, h, color)}：sprite 由
 * {@code GuiGraphicsExtractor} 自己从 GUI 图集里取（并处理 {@code GuiSpriteScaling}），
 * 颜色参数带 alpha，所以纹理正确 <b>且</b> 跟随透明度。早期版本手工拼 UV
 * （{@code getX()/getY()} + 自算图集尺寸）一旦推导有偏差就会采样到图集别处，
 * 表现为「血条不是原版纹理」。</p>
 *
 * <p>颜色只用 {@link Tint} 表达：敌人按当前心类取红/黄/紫/蓝，队友固定绿。
 * 两边的绘制调用完全相同，差别只有 tint。</p>
 */
final class BossBarTexture {

    /** 原版血条纹理尺寸。 */
    static final int NATIVE_WIDTH = 182;
    static final int NATIVE_HEIGHT = 5;

    /** 条颜色（名字就是原版 sprite 的前缀）。 */
    enum Tint {
        RED, YELLOW, PURPLE, BLUE, GREEN;

        Identifier background() {
            return Identifier.withDefaultNamespace("boss_bar/"
                    + name().toLowerCase(java.util.Locale.ROOT) + "_background");
        }

        Identifier progress() {
            return Identifier.withDefaultNamespace("boss_bar/"
                    + name().toLowerCase(java.util.Locale.ROOT) + "_progress");
        }
    }

    /** 默认色，也是空槽与填充的兜底色。 */
    private static final Tint DEFAULT = Tint.RED;

    private BossBarTexture() {}

    /**
     * 心类 → 条颜色，按需求：红心→红、魂心→黄、黑心→紫、蓝心→蓝。
     *
     * <p>数据包自己的 bossbar 颜色与这个映射不完全一样（敌人的红心通道用的是
     * {@code blue}、魂心是 {@code yellow}），所以先由
     * {@code BossBarTracker} 把颜色翻译成 {@link HeartCounts.Kind}，这里只做
     * 「心类 → 纹理」这一步。</p>
     */
    static Tint tintFor(HeartCounts.Kind kind) {
        if (kind == null) return DEFAULT;
        return switch (kind) {
            case SOUL -> Tint.YELLOW;
            case BLACK -> Tint.PURPLE;
            case BLUE -> Tint.BLUE;
            default -> Tint.RED;
        };
    }

    /** 空槽。tint 为 null 时用红。 */
    static void drawBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, Tint tint) {
        if (w <= 0 || h <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, (tint == null ? DEFAULT : tint).background(),
                x, y, w, h, color);
    }

    /** 已填充段；{@code w} 是这段占的像素宽。 */
    static void drawProgress(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, Tint tint) {
        if (w <= 0 || h <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, (tint == null ? DEFAULT : tint).progress(),
                x, y, w, h, color);
    }

    // ── 世界空间路径（原版 nameTag 那条路）用 ──

    /**
     * 空槽纹理文件。GUI 里走的是 GUI 图集里的同名 sprite，这里直接指向
     * {@code textures/gui/sprites/boss_bar/*.png} 原文件：两者是同一张图，
     * 资源包替换同样生效，且不需要去碰图集 UV。
     */
    static Identifier backgroundTexture(Tint tint) {
        return texture(tint == null ? DEFAULT : tint, false);
    }

    /** 已填充段纹理文件。 */
    static Identifier progressTexture(Tint tint) {
        return texture(tint == null ? DEFAULT : tint, true);
    }

    private static Identifier texture(Tint tint, boolean progress) {
        return Identifier.withDefaultNamespace("textures/gui/sprites/boss_bar/"
                + tint.name().toLowerCase(java.util.Locale.ROOT)
                + (progress ? "_progress" : "_background") + ".png");
    }

    /** {@code 0xRRGGBB} + alpha → {@code 0xAARRGGBB}。 */
    static int argb(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
