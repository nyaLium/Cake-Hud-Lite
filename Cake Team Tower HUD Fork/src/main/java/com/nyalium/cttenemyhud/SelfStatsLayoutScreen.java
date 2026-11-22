package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「移动面板」界面（1.20.4）：直接拖着属性面板放到想要的位置。
 *
 * <p>面板是 GUI 元素、跟屏幕坐标绑死，所以最直观的定位方式就是<b>拖</b>：
 * 打开这个界面后会按当前设置画一份预览（没有实时数据时用示例行），
 * 用鼠标拖到满意位置，点「完成」即可（写盘仍由总界面的「完成」负责）。</p>
 *
 * <p>坐标存在 {@code selfStatsX / selfStatsY}，单位是 GUI 像素 —— 与界面缩放无关，
 * 换分辨率后位置比例会变，但不会被夹出屏幕（见 {@link SelfStatsPanel#clampX}）。</p>
 */
public final class SelfStatsLayoutScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int GAP = 8;

    private final EnemyHudSettingsScreen parent;

    private boolean dragging;

    public SelfStatsLayoutScreen(EnemyHudSettingsScreen parent) {
        super(Component.translatable("ctt_enemy_hud.config.selfStatsMove"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height - ROW_HEIGHT - 8;
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.selfStatsMove.reset"), b -> {
            // 只重置当前显示方式那一套（HUD / 容器界面各有一套）。
            EnemyHudConfig.movePanel(4, EnemyHudConfig.containerMode() ? 20 : 40);
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
        // 提示当前在改哪一套配置（HUD / 容器界面）。
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.selfStatsMode.current",
                        Component.translatable(EnemyHudConfig.containerMode()
                                ? "ctt_enemy_hud.config.selfStatsMode.container"
                                : "ctt_enemy_hud.config.selfStatsMode.always")),
                this.width / 2, 50, 0xFF9FE8FF);

        List<Component> lines = SelfStats.lines();
        boolean demo = lines.isEmpty();
        if (demo) lines = SelfStatsPanel.demoLines();
        EnemyHudConfig.Panel panel = EnemyHudConfig.panel();
        SelfStatsPanel.draw(graphics, this.font, lines, panel, true);
        int[] bounds = SelfStatsPanel.bounds(this.font, lines, panel);
        outline(graphics, bounds, dragging ? 0xC0FFD060 : 0x80FFFFFF);
        String info = "x=" + panel.x + "  y=" + panel.y + "  " + bounds[2] + "x" + bounds[3]
                + (demo ? Component.translatable("ctt_enemy_hud.config.selfStatsMove.demo").getString() : "");
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int[] bounds = currentBounds();
        if (acceptButton(event) && insideGui(bounds, event)) {
            this.dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            // 用**增量**而不是绝对坐标：和原版滑块一致，也对不同输入后端更稳
            // （有的后端在拖动过程中只给相对位移）。
            int[] bounds = currentBounds();
            EnemyHudConfig.Panel panel = EnemyHudConfig.panel();
            EnemyHudConfig.movePanel(
                    SelfStatsPanel.clampX(panel.x + (int) Math.round(dragX), bounds[2], this.width),
                    SelfStatsPanel.clampY(panel.y + (int) Math.round(dragY), bounds[3], this.height));
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

    /** 左键（0）为主；部分 SDL/新输入链会把左键报成 1，一并接受。 */
    private static boolean acceptButton(MouseButtonEvent event) {
        return event != null && (event.button() == 0 || event.button() == 1);
    }

    /**
     * 事件坐标 → GUI 坐标。
     *
     * <p>26.3 的 {@link MouseButtonEvent} 在不同输入后端下可能带「GUI 缩放后的坐标」，
     * 也可能带「窗口物理像素」；后者会大于 GUI 尺寸。这里按数值判断并统一换算，
     * 免得拖动只在某些机器上有效。</p>
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

    private boolean insideGui(int[] bounds, MouseButtonEvent event) {
        double[] gui = toGui(event);
        return inside(bounds, gui[0], gui[1]);
    }

    private int[] currentBounds() {
        List<Component> lines = SelfStats.lines();
        if (lines.isEmpty()) lines = SelfStatsPanel.demoLines();
        return SelfStatsPanel.bounds(this.font, lines, EnemyHudConfig.panel());
    }

    private static boolean inside(int[] bounds, double x, double y) {
        return x >= bounds[0] && x <= bounds[0] + bounds[2]
                && y >= bounds[1] && y <= bounds[1] + bounds[3];
    }

    @Override
    public void onClose() {
        back();
    }
}
