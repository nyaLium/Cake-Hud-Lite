package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * 原版 Bossbar 纹理调用封装。
 *
 * <p><b>不打包任何材质</b>：直接用原版 GUI 图集里的
 * {@code minecraft:boss_bar/red_background} / {@code red_progress}（182×5），
 * 与 {@code BossHealthOverlay} 画 Boss 条用的是同一对 sprite，资源包替换会同步生效。</p>
 *
 * <p>走 {@code blitSprite(pipeline, id, x, y, w, h, color)} 这个重载：
 * sprite 由 {@code GuiGraphicsExtractor} 自己从 GUI 图集里取（并处理
 * {@code GuiSpriteScaling}），颜色参数带 alpha，所以纹理正确 <b>且</b> 跟随透明度。
 * 早期版本手工拼 UV（`getX()/getY()` + 自算图集尺寸）一旦推导有偏差就会采样到图集别处，
 * 表现为“血条不是原版纹理”。</p>
 */
final class BossBarTexture {

    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("boss_bar/red_background");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("boss_bar/red_progress");
    /** 队友血条用原版绿色 bossbar 纹理。 */
    private static final Identifier BACKGROUND_GREEN = Identifier.withDefaultNamespace("boss_bar/green_background");
    private static final Identifier PROGRESS_GREEN = Identifier.withDefaultNamespace("boss_bar/green_progress");

    /** 原版血条纹理尺寸。 */
    static final int NATIVE_WIDTH = 182;
    static final int NATIVE_HEIGHT = 5;

    /** 心类对应的条颜色（名字就是原版 sprite 的名字）。 */
    enum HeartTint { RED, BLUE, PURPLE, YELLOW }

    /**
     * 心类 → 条颜色，按需求：红心→红、魂心→黄、黑心→紫、蓝心→蓝。
     *
     * <p>数据包自己的 bossbar 颜色与这个映射不完全一样（敌人的红心通道用的是
     * {@code blue}、魂心是 {@code yellow}），所以先由
     * {@code BossBarTracker} 把颜色翻译成 {@link HeartCounts.Kind}，这里只做
     * 「心类 → 纹理」这一步。</p>
     */
    static HeartTint tintFor(HeartCounts.Kind kind) {
        if (kind == null) return HeartTint.RED;
        return switch (kind) {
            case SOUL -> HeartTint.YELLOW;
            case BLACK -> HeartTint.PURPLE;
            case BLUE -> HeartTint.BLUE;
            default -> HeartTint.RED;
        };
    }

    private static final Identifier BACKGROUND_BLUE = Identifier.withDefaultNamespace("boss_bar/blue_background");
    private static final Identifier PROGRESS_BLUE = Identifier.withDefaultNamespace("boss_bar/blue_progress");
    private static final Identifier BACKGROUND_PURPLE = Identifier.withDefaultNamespace("boss_bar/purple_background");
    private static final Identifier PROGRESS_PURPLE = Identifier.withDefaultNamespace("boss_bar/purple_progress");
    private static final Identifier BACKGROUND_YELLOW = Identifier.withDefaultNamespace("boss_bar/yellow_background");
    private static final Identifier PROGRESS_YELLOW = Identifier.withDefaultNamespace("boss_bar/yellow_progress");
    private BossBarTexture() {}

    /** 按心类选条纹理的空槽。 */
    static void drawBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, HeartTint tint) {
        if (w <= 0 || h <= 0) return;
        Identifier id = switch (tint == null ? HeartTint.RED : tint) {
            case BLUE -> BACKGROUND_BLUE;
            case PURPLE -> BACKGROUND_PURPLE;
            case YELLOW -> BACKGROUND_YELLOW;
            default -> BACKGROUND;
        };
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h, color);
    }

    /** 按心类选条纹理的填充段。 */
    static void drawProgress(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, HeartTint tint) {
        if (w <= 0 || h <= 0) return;
        Identifier id = switch (tint == null ? HeartTint.RED : tint) {
            case BLUE -> PROGRESS_BLUE;
            case PURPLE -> PROGRESS_PURPLE;
            case YELLOW -> PROGRESS_YELLOW;
            default -> PROGRESS;
        };
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h, color);
    }
    /** 空槽（深红底板）。 */
    static void drawBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, w, h, color);
    }

    /** 队友条：原版绿色空槽。 */
    static void drawBackgroundGreen(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_GREEN, x, y, w, h, color);
    }

    /** 队友条：原版绿色填充段。 */
    static void drawProgressGreen(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_GREEN, x, y, w, h, color);
    }

    /** 已填充段；{@code w} 是这段占的像素宽。 */
    static void drawProgress(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS, x, y, w, h, color);
    }
}
