package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 「移动盾牌指示器」界面（1.20.20）：和属性面板一样拖动定位。
 *
 * <p>1.20.23：拖动方式改成上游 Cake-Hud 那种<b>绝对定位</b> —— 按下时记
 * 「鼠标 ↔ 指示器左上角」的偏移，移动时直接 {@code 鼠标位置 - 偏移}，
 * 不累加每帧增量（增量在某些输入后端是 0，累加式会「拖不动」）。
 * 另外支持方向键微调（4 px，Shift 16 px）。</p>
 *
 * <p>预览用 {@code meter_0.png}，按当前 {@code shieldScale} 与 {@code shieldOpacity} 画；
 * 坐标存 {@code shieldX/shieldY}（GUI 像素，左上角；{@code -1} = 自动贴副手槽上方）。</p>
 */
public final class ShieldLayoutScreen extends Screen {

    private static final Identifier PREVIEW =
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_0.png");
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int GAP = 8;

    private final ShieldIndicatorSettingsScreen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;

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
            this.dragging = false;
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
        int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
        int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
        ShieldIndicatorHud.blitScaled(graphics, PREVIEW, pos[0], pos[1], cfg.shieldScale, cfg.shieldOpacity);
        outline(graphics, pos[0], pos[1], w, h, dragging ? 0xC0FFD060 : 0x80FFFFFF);
        String info = (cfg.shieldX < 0 || cfg.shieldY < 0 ? "auto " : "") + "x=" + pos[0] + "  y=" + pos[1];
        graphics.text(this.font, info, Math.max(4, pos[0]), Math.max(4, pos[1] - 12), 0xFFFFFFFF, true);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y, color);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        g.fill(x - 1, y, x, y + h, color);
        g.fill(x + w, y, x + w + 1, y + h, color);
    }

    /** 当前显示位置；未设置（-1）时按副手槽/屏幕中心算一个默认预览位置。 */
    private int[] position(EnemyHudConfig.Cfg cfg) {
        int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
        int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
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

    // ── 拖动（绝对定位，上游同款） ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) return true;
        if (accept(event)) {
            EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
            int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
            int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
            int[] pos = position(cfg);
            double[] gui = toGui(event);
            if (gui[0] >= pos[0] && gui[0] <= pos[0] + w && gui[1] >= pos[1] && gui[1] <= pos[1] + h) {
                this.dragOffsetX = gui[0] - pos[0];
                this.dragOffsetY = gui[1] - pos[1];
            } else {
                // 按在空白处也接住：把指示器中心吸附到光标，避免「抓不住」。
                this.dragOffsetX = w / 2.0;
                this.dragOffsetY = h / 2.0;
            }
            this.dragging = true;
            applyDrag(gui[0], gui[1]);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            double[] gui = toGui(event);
            applyDrag(gui[0], gui[1]);
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

    /** 键盘兜底：方向键 4 px、按住 Shift 16 px。 */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int step = event.hasShiftDown() ? 16 : 4;
        int dx = 0;
        int dy = 0;
        if (event.isLeft()) dx = -step;
        else if (event.isRight()) dx = step;
        else if (event.isUp()) dy = -step;
        else if (event.isDown()) dy = step;
        if (dx == 0 && dy == 0) return super.keyPressed(event);
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int[] pos = position(cfg);
        moveTo(pos[0] + dx, pos[1] + dy);
        return true;
    }

    private void applyDrag(double mouseX, double mouseY) {
        moveTo((int) Math.round(mouseX - this.dragOffsetX), (int) Math.round(mouseY - this.dragOffsetY));
    }

    private void moveTo(int x, int y) {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
        int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
        cfg.shieldX = SelfStatsPanel.fitInScreen(x, w, this.width);
        cfg.shieldY = SelfStatsPanel.fitInScreen(y, h, this.height);
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
