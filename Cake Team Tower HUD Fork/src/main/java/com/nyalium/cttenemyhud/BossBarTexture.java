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

    private BossBarTexture() {}

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
