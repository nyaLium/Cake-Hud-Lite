package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 「移动盾牌指示器」界面（1.20.20）：和属性面板一样拖动定位。
 *
 * <p>预览用 {@code meter_0.png}，按当前 {@code shieldScale} 与 {@code shieldOpacity} 画；
 * 坐标存 {@code shieldX/shieldY}（GUI 像素，左上角）。</p>
 */
public final class ShieldLayoutScreen extends Screen {

    private static final Identifier PREVIEW =
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_0.png");
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int GAP = 8;

    private final ShieldIndicatorSettingsScreen parent;
    private boolean dragging;

    public ShieldLayoutScreen(ShieldIndicatorSettingsScreen parent) {
        super(Component.translatable("ctt_enemy_hud.config.shieldMove"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height - ROW_HEIGHT - 8;
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.selfStatsMove.reset"), b -> {
            EnemyHudConfig.get().shieldX = -1;
            EnemyHudConfig.get().shieldY = -1;
            this.rebuildWidgets();
        }).bounds(this.width / 2 - BUTTON_WIDTH - GAP / 2, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(this.width / 2 + GAP / 2, y, BUTTON_WIDTH, ROW_HEIGHT).build());
    }

    private void back() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.selfStatsMove.hint"),
                this.width / 2, 36, 0xFFFFD060);

        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int[] pos = position(cfg);
        int k = Math.max(1, Math.round((float) cfg.shieldScale));
        int w = ShieldIndicatorHud.TEX_W * k;
        int h = ShieldIndicatorHud.TEX_H * k;
        int alpha = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, cfg.shieldOpacity)));
        graphics.blit(RenderPipelines.GUI_TEXTURED, PREVIEW, pos[0], pos[1], 0.0f, 0.0f,
                w, h, ShieldIndicatorHud.TEX_W, ShieldIndicatorHud.TEX_H, (alpha << 24) | 0xFFFFFF);
        outline(graphics, pos[0], pos[1], w, h, dragging ? 0xC0FFD060 : 0x80FFFFFF);
        graphics.text(this.font, "x=" + pos[0] + "  y=" + pos[1],
                Math.max(4, pos[0]), Math.max(4, pos[1] - 12), 0xFFFFFFFF, true);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y, color);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        g.fill(x - 1, y, x, y + h, color);
        g.fill(x + w, y, x + w + 1, y + h, color);
    }

    /** 当前显示位置；未设置（-1）时按副手槽/屏幕中心算一个默认预览位置。 */
    private int[] position(EnemyHudConfig.Cfg cfg) {
        int k = Math.max(1, Math.round((float) cfg.shieldScale));
        int w = ShieldIndicatorHud.TEX_W * k;
        int h = ShieldIndicatorHud.TEX_H * k;
        int x = cfg.shieldX;
        int y = cfg.shieldY;
        if (x < 0 || y < 0) {
            x = this.width / 2 + 109 - w / 2;
            y = this.height - 76 - h;
        }
        x = Math.max(0, Math.min(this.width - w, x));
        y = Math.max(0, Math.min(this.height - h, y));
        return new int[]{x, y};
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (accept(event)) {
            int[] pos = position(EnemyHudConfig.get());
            int k = Math.max(1, Math.round((float) EnemyHudConfig.get().shieldScale));
            int w = ShieldIndicatorHud.TEX_W * k;
            int h = ShieldIndicatorHud.TEX_H * k;
            double[] gui = toGui(event);
            if (gui[0] >= pos[0] && gui[0] <= pos[0] + w && gui[1] >= pos[1] && gui[1] <= pos[1] + h) {
                this.dragging = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
            int[] pos = position(cfg);
            int k = Math.max(1, Math.round((float) cfg.shieldScale));
            int w = ShieldIndicatorHud.TEX_W * k;
            int h = ShieldIndicatorHud.TEX_H * k;
            cfg.shieldX = Math.max(0, Math.min(this.width - w, pos[0] + (int) Math.round(dragX)));
            cfg.shieldY = Math.max(0, Math.min(this.height - h, pos[1] + (int) Math.round(dragY)));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging && accept(event)) {
            this.dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private static boolean accept(MouseButtonEvent event) {
        return event != null && (event.button() == 0 || event.button() == 1);
    }

    private double[] toGui(MouseButtonEvent event) {
        double x = event.x();
        double y = event.y();
        int fbW = Math.max(1, this.minecraft.getWindow().getWidth());
        int fbH = Math.max(1, this.minecraft.getWindow().getHeight());
        if (x > this.width || y > this.height) {
            x = x * this.width / fbW;
            y = y * this.height / fbH;
        }
        return new double[]{x, y};
    }

    @Override
    public void onClose() {
        back();
    }
}
