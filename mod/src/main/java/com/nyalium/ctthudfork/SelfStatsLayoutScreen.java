package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「调整位置…」界面（旧名「移动面板」；1.20.4 起；1.20.23 改成上游 Cake-Hud 那种拖动方式）。
 *
 * <p>上游 {@code ConfigScreen.beginPreviewDrag/movePreviewDrag} 的做法：
 * <b>按下时记住「鼠标 ↔ 面板左上角」的偏移，移动时直接用鼠标绝对坐标减偏移定位</b>，
 * 而不是累加每帧的 {@code dragX/dragY}。累加式在两个地方会失灵：</p>
 * <ul>
 *   <li>某一帧的增量被丢（鼠标事件被别的界面吃掉、切窗口）时，位置就永久落后；</li>
 *   <li>部分输入后端只给绝对坐标、增量为 0，拖了等于没拖 —— 这正是「容器界面那套拖不动」的表现。</li>
 * </ul>
 *
 * <p>另外三条一起改：</p>
 * <ul>
 *   <li><b>只在面板上按下 + 真的拖动才移动</b>（1.20.28）：点一下空白处、或按下后手抖 1 px
 *       都不会改变坐标；想挪位置就按住面板拖；</li>
 *   <li><b>方向键微调</b>：←→↑↓ 每次 4 px，Shift 加速到 16 px（鼠标不好使时的兜底）；</li>
 *   <li><b>就地切换对象</b>：模式 2 下多一个「对象：HUD / 容器界面」按钮，
 *       不用退回上一页就能确认自己在拖哪一套。</li>
 * </ul>
 */
public final class SelfStatsLayoutScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int GAP = 8;

    private final SelfStatsSettingsScreen parent;
    /** 编辑哪一套：false = HUD，true = 容器界面（模式 2 下两者独立）。 */
    private boolean containerConfig;

    private boolean dragging;
    /** 按下时鼠标相对于面板左上角的偏移（上游同款）。 */
    private double dragOffsetX;
    private double dragOffsetY;

    /**
     * @param parent          返回时回到的属性面板设置页（会把当前编辑对象同步回去）
     * @param containerConfig {@code true} = 编辑容器界面那一套，{@code false} = 编辑 HUD 那一套
     */
    public SelfStatsLayoutScreen(SelfStatsSettingsScreen parent, boolean containerConfig) {
        super(Component.translatable("ctt_hud_fork.config.selfStatsMove"));
        this.parent = parent;
        this.containerConfig = containerConfig;
    }

    /** 当前模式的布局参数（跟随 {@link #containerConfig}；1.20.40 起按本界面 GUI 尺寸换算位置）。 */
    private HudConfig.Panel currentPanel() {
        return HudConfig.panel(HudConfig.get().selfStatsMode, this.containerConfig,
                this.width, this.height);
    }

    @Override
    protected void init() {
        int y = this.height - ROW_HEIGHT - 8;
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.selfStatsMove.reset"),
                b -> resetPosition()).bounds(this.width / 2 - BUTTON_WIDTH - GAP / 2, y, BUTTON_WIDTH, ROW_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 + GAP / 2, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        // 模式 2 才有「两套」：就地切换正在拖的对象，避免拖错一套还以为拖不动。
        if (HudConfig.get().selfStatsMode == 2) {
            int switchWidth = Math.min(240, this.width - 40);
            addRenderableWidget(Button.builder(targetLabel(), b -> {
                this.containerConfig = !this.containerConfig;
                this.rebuildWidgets();
            }).bounds(this.width / 2 - switchWidth / 2, y - ROW_HEIGHT - 4, switchWidth, ROW_HEIGHT).build());
        }
    }

    private Component targetLabel() {
        return Component.translatable("ctt_hud_fork.config.selfStatsMove.target",
                Component.translatable(this.containerConfig
                        ? "ctt_hud_fork.config.selfStatsTarget.container"
                        : "ctt_hud_fork.config.selfStatsTarget.hud"));
    }

    /** 「回到初始位置」：只重置当前这一套（1.20.41 起回到「屏幕高 ÷ 3」，容器那套自 1.20.42 起一样）。 */
    private void resetPosition() {
        HudConfig.resetPanelPosition(HudConfig.get().selfStatsMode, this.containerConfig);
        this.dragging = false;
        this.rebuildWidgets();
    }

    private void back() {
        if (this.parent != null) this.parent.setEditContainer(this.containerConfig);
        this.minecraft.gui.setScreen(this.parent);
    }

    /**
     * 对齐时不要模糊背景（1.20.27）。
     *
     * <p>原版 {@code Screen.extractBackground} 会调 {@code blurBeforeThisStratum()} 把世界糊掉；
     * 拖动/对齐面板时玩家恰恰要靠真实画面判断位置，所以这里直接不画模糊层。
     * 那层很淡的压暗（{@code INWORLD_MENU_BACKGROUND}）保留，文字仍然清楚。</p>
     */
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
        int mode = HudConfig.get().selfStatsMode;
        Component modeName = Component.translatable(SelfStatsSettingsScreen.modeLabelKey(mode));
        Component shown = modeName;
        if (mode == 2) {
            shown = modeName.copy().append(" · ").append(Component.translatable(this.containerConfig
                    ? "ctt_hud_fork.config.selfStatsTarget.container"
                    : "ctt_hud_fork.config.selfStatsTarget.hud"));
        }
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.selfStatsMode.current", shown),
                this.width / 2, 50, 0xFF9FE8FF);

        List<Component> lines = SelfStats.lines();
        boolean demo = lines.isEmpty();
        if (demo) lines = SelfStatsPanel.demoLines();
        HudConfig.Panel panel = currentPanel();
        SelfStatsPanel.draw(graphics, this.font, lines, panel, HudConfig.get().selfStatsTextShadow);
        int[] bounds = SelfStatsPanel.bounds(this.font, lines, panel);
        outline(graphics, bounds, dragging ? 0xC0FFD060 : 0x80FFFFFF);
        String info = "x=" + panel.x + "  y=" + panel.y + "  " + bounds[2] + "x" + bounds[3]
                + (demo ? Component.translatable("ctt_hud_fork.config.selfStatsMove.demo").getString() : "");
        graphics.text(this.font, info, Math.max(4, bounds[0]), Math.max(4, bounds[1] - 12), 0xFFFFFFFF, true);
    }

    private static void outline(GuiGraphicsExtractor graphics, int[] bounds, int color) {
        int x = bounds[0];
        int y = bounds[1];
        int w = Math.max(1, bounds[2]);
        int h = Math.max(1, bounds[3]);
        graphics.fill(x - 1, y - 1, x + w + 1, y, color);             // 上
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, color);     // 下
        graphics.fill(x - 1, y, x, y + h, color);                     // 左
        graphics.fill(x + w, y, x + w + 1, y + h, color);             // 右
    }

    // ── 拖拽（26.3 的输入事件是 MouseButtonEvent） ──
    //
    // 1.20.28：改成「必须真的拖」才移动 —— 只在面板上按下 → 拖动超过阈值才写位置；
    // 点一下空白处或手抖 1 px 都不会改变坐标（之前是「整屏可抓 + 按下即吸附」，太容易误操作）。

    /** 面板外这么多像素以内也算抓住（盾牌指示器只有 24×60，给它一点余量）。 */
    private static final int GRAB_MARGIN = 4;
    /** 按下后要移动这么多像素才算拖动。 */
    private static final int DRAG_THRESHOLD = 2;

    /** 按下点（GUI 坐标），用来判拖动阈值。 */
    private double pressX;
    private double pressY;
    /** 本次按下之后是否已经真正开始拖动（超过阈值才置位）。 */
    private boolean moved;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        // 先让按钮/滑块吃事件，别把「返回」「回到初始位置」的点击吞成拖动。
        if (super.mouseClicked(event, doubled)) return true;
        if (!acceptButton(event)) return false;
        int[] bounds = currentBounds();
        double[] gui = toGui(event);
        // 只允许在面板（含 4 px 余量）上按下开始抓；点空白处什么都不做。
        if (!SelfStatsPanel.grabHit(bounds, GRAB_MARGIN, gui[0], gui[1])) return false;
        this.dragOffsetX = gui[0] - bounds[0];
        this.dragOffsetY = gui[1] - bounds[1];
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
                // 还没超过阈值：这次拖动可能是手抖，不写位置。
                if (!SelfStatsPanel.dragExceeds(gui[0] - this.pressX, gui[1] - this.pressY, DRAG_THRESHOLD)) {
                    return true;
                }
                this.moved = true;
            }
            // 绝对定位：不用 dragX/dragY（某些后端给 0，累加式就永远拖不动）。
            applyDrag(gui[0], gui[1]);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging && acceptButton(event)) {
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
        int[] bounds = currentBounds();
        HudConfig.Panel panel = currentPanel();
        moveTo(panel.x + dx, panel.y + dy, bounds);
        return true;
    }

    private void applyDrag(double mouseX, double mouseY) {
        int[] bounds = currentBounds();
        moveTo((int) Math.round(mouseX - this.dragOffsetX), (int) Math.round(mouseY - this.dragOffsetY),
                bounds);
    }

    /** 写位置 + 夹进屏幕（整块留在屏内，拖不出边界）；1.20.40 起连同当前 GUI 尺寸一起写。 */
    private void moveTo(int x, int y, int[] bounds) {
        HudConfig.movePanel(HudConfig.get().selfStatsMode, this.containerConfig,
                SelfStatsPanel.fitInScreen(x, bounds[2], this.width),
                SelfStatsPanel.fitInScreen(y, bounds[3], this.height),
                this.width, this.height);
    }

    /** 左键（0）为主；部分 SDL/新输入链会把左键报成 1，一并接受。 */
    private static boolean acceptButton(MouseButtonEvent event) {
        return event != null && (event.button() == 0 || event.button() == 1);
    }

    /**
     * 事件坐标 → GUI 坐标。
     *
     * <p>26.3 的 {@link MouseButtonEvent} 不同输入后端可能带「GUI 缩放后的坐标」，
     * 也可能带「窗口物理像素」；后者会大于 GUI 尺寸，按数值判断并统一换算。</p>
     */
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

    private int[] currentBounds() {
        return SelfStatsPanel.bounds(this.font, currentLines(), currentPanel());
    }

    private List<Component> currentLines() {
        List<Component> lines = SelfStats.lines();
        return lines.isEmpty() ? SelfStatsPanel.demoLines() : lines;
    }

    @Override
    public void onClose() {
        back();
    }
}
