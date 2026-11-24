package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
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
 * 1.20.28 起只在指示器上按住并拖动才移动，点一下空白处不会挪位置。另外支持方向键微调（4 px，Shift 16 px）。</p>
 *
 * <p>1.2.0：「初始位置是否跟随副手变化」（{@code shieldAutoMirror}）挪到本界面，
 * 放在「回到初始位置」左侧；只影响未移动过的默认位置。</p>
 *
 * <p>预览用 {@code meter_0.png}，按当前 {@code shieldScale} 与 {@code shieldOpacity} 画；
 * 坐标存 {@code shieldX/shieldY}（GUI 像素，左上角；{@code -1} = 跟随副手槽的默认位置，见
 * {@link ShieldIndicatorHud#defaultX}/{@link ShieldIndicatorHud#defaultY}）。</p>
 */
public final class ShieldLayoutScreen extends Screen {

    private static final Identifier PREVIEW =
            Identifier.fromNamespaceAndPath(CttHudForkClient.MOD_ID, "textures/gui/meter_0.png");
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int GAP = 8;

    private final ShieldIndicatorSettingsScreen parent;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    /** 面板外这么多像素以内也算抓住（指示器只有 24×60，给它一点余量）。 */
    private static final int GRAB_MARGIN = 4;
    /** 按下后要移动这么多像素才算拖动（1.20.28：点一下不再挪位置）。 */
    private static final int DRAG_THRESHOLD = 2;
    /** 按下点（GUI 坐标）与「已经真正拖动」标记。 */
    private double pressX;
    private double pressY;
    private boolean moved;

    public ShieldLayoutScreen(ShieldIndicatorSettingsScreen parent) {
        super(Component.translatable("ctt_hud_fork.config.shieldMove"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height - ROW_HEIGHT - 8;
        HudConfig.Cfg cfg = HudConfig.get();

        // 初始位置是否跟随副手变化：shieldAutoMirror（1.2.0）
        // 只影响「未移动过的初始位置」；放在「回到初始位置」左侧。
        Component mirrorLabel = Component.translatable("ctt_hud_fork.config.shieldAutoMirror");
        int mirrorWidth = Math.max(BUTTON_WIDTH, this.font.width(mirrorLabel) + 40);
        int totalWidth = mirrorWidth + BUTTON_WIDTH * 2 + GAP * 2;
        int left;
        int mirrorY = y;
        if (totalWidth > this.width - 8) {
            // 窗口太窄：这一项单独占一行，放在下面两个按钮的上方
            mirrorWidth = Math.min(mirrorWidth, this.width - 16);
            totalWidth = mirrorWidth;
            left = this.width / 2 - mirrorWidth / 2;
            mirrorY = y - ROW_HEIGHT - 4;
        } else {
            left = this.width / 2 - totalWidth / 2;
        }
        CycleButton<Boolean> mirror = CycleButton.onOffBuilder(cfg.shieldAutoMirror)
                .create(left, mirrorY, mirrorWidth, ROW_HEIGHT, mirrorLabel,
                        (b, v) -> HudConfig.get().shieldAutoMirror = v);
        mirror.setTooltip(Tooltip.create(
                Component.translatable("ctt_hud_fork.config.shieldAutoMirror.tooltip")));
        addRenderableWidget(mirror);

        int rowLeft = left + mirrorWidth + GAP;
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.selfStatsMove.reset"), b -> {
            HudConfig.setShieldPosition(-1, -1, this.width, this.height);
            this.dragging = false;
            this.rebuildWidgets();
        }).bounds(rowLeft, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(rowLeft + BUTTON_WIDTH + GAP, y, BUTTON_WIDTH, ROW_HEIGHT).build());
    }

    private void back() {
        this.minecraft.gui.setScreen(this.parent);
    }

    /** 对齐时不要模糊背景（1.20.27，见 {@code SelfStatsLayoutScreen} 的同一段说明）。 */
    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        // 故意留空
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        // 操作提示（1.20.29）：只拖不点 + 方向键微调
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.selfStatsMove.dragHint"),
                this.width / 2, 36, 0xFFFFD060);

        HudConfig.Cfg cfg = HudConfig.get();
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

    /**
     * 当前显示位置；未设置（-1）时用 {@link ShieldIndicatorHud#defaultX}/{@link ShieldIndicatorHud#defaultY}
     * 的同一套默认位置（跟着副手槽那一侧，底边距屏幕底 76 px），保证预览和 HUD 完全一致。
     */
    private int[] position(HudConfig.Cfg cfg) {
        int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
        int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
        // 1.20.40：配置里存的是「相对位置」，这里换算到本界面的 GUI 尺寸。
        int[] stored = HudConfig.shieldPosition(this.width, this.height);
        int x = stored[0];
        int y = stored[1];
        if (x < 0 || y < 0) {
            boolean offhandOnLeft = ShieldIndicatorHud.offhandOnLeft(cfg,
                    this.minecraft.player == null ? null : this.minecraft.player.getMainArm());
            x = ShieldIndicatorHud.defaultX(offhandOnLeft, this.width, w);
            y = ShieldIndicatorHud.defaultY(this.height, h);
        }
        x = ShieldIndicatorHud.clampInto(x, w, this.width);
        y = ShieldIndicatorHud.clampInto(y, h, this.height);
        return new int[]{x, y};
    }

    // ── 拖动（绝对定位，上游同款） ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) return true;
        if (!accept(event)) return false;
        HudConfig.Cfg cfg = HudConfig.get();
        int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
        int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
        int[] pos = position(cfg);
        double[] gui = toGui(event);
        // 1.20.28：只允许在指示器（含 4 px 余量）上按下开始抓；点空白处不再挪位置。
        if (!SelfStatsPanel.grabHit(new int[]{pos[0], pos[1], w, h}, GRAB_MARGIN, gui[0], gui[1])) return false;
        this.dragOffsetX = gui[0] - pos[0];
        this.dragOffsetY = gui[1] - pos[1];
        this.pressX = gui[0];
        this.pressY = gui[1];
        this.moved = false;
        this.dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            double[] gui = toGui(event);
            if (!this.moved) {
                if (!SelfStatsPanel.dragExceeds(gui[0] - this.pressX, gui[1] - this.pressY, DRAG_THRESHOLD)) {
                    return true;
                }
                this.moved = true;
            }
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
        HudConfig.Cfg cfg = HudConfig.get();
        int[] pos = position(cfg);
        moveTo(pos[0] + dx, pos[1] + dy);
        return true;
    }

    private void applyDrag(double mouseX, double mouseY) {
        moveTo((int) Math.round(mouseX - this.dragOffsetX), (int) Math.round(mouseY - this.dragOffsetY));
    }

    private void moveTo(int x, int y) {
        HudConfig.Cfg cfg = HudConfig.get();
        int w = ShieldIndicatorHud.scaledWidth(cfg.shieldScale);
        int h = ShieldIndicatorHud.scaledHeight(cfg.shieldScale);
        // 1.20.40：连同「量它的 GUI 尺寸」一起写，之后窗口变化按比例换算。
        HudConfig.setShieldPosition(ShieldIndicatorHud.clampInto(x, w, this.width),
                ShieldIndicatorHud.clampInto(y, h, this.height), this.width, this.height);
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
