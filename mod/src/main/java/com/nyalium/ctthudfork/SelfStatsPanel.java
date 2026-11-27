package com.nyalium.ctthudfork;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 自身属性面板（GUI 层，1~3 列文字）。
 *
 * <p>内容就是地图 {@code /trigger ViewStats} 回的那几十行 {@link Component}，
 * 原样按列排开画出来 —— 颜色、图标都保留，和游戏里聊天栏看到的一致。</p>
 *
 * <h2>1.20.4 新增</h2>
 * <ul>
 *   <li>{@code selfStatsOpacity}：整块文字不透明度；</li>
 *   <li>显示内容过滤：{@link SelfStats#lines()} 已经把「隐藏类」剔掉了；</li>
 *   <li>同样的绘制代码给「调整位置…」界面做预览用（{@link #draw}）。</li>
 * </ul>
 *
 * <p>位置从「绝对 GUI 像素」改成<b>相对位置</b> —— 配置里存的是量在
 * {@code layoutGuiW × layoutGuiH} 那套 GUI 下的坐标，这里按当前 {@code guiWidth/guiHeight}
 * 换算（{@link HudConfig#panel(int, boolean, int, int)}），窗口大小或 GUI 缩放变化时不会错位。</p>
 */
final class SelfStatsPanel implements HudElement {

    /** 行高（字体 8 px + 2 px 行距，和原版聊天行距一致）。 */
    static final int ROW_HEIGHT = 10;
    /** 列间距。 */
    static final int COLUMN_GAP = 8;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        // 统一闸门：未知诅咒（整个 UI）与癫狂（只有面板）都让属性面板消失；
        // 实体血条两者的处理不同，见 HudRenderGate。
        if (HudRenderGate.suppressPanel(mc)) return;
        // 「在容器界面显示」模式下 HUD 上不画，改由 ContainerPanelMixin 画在容器界面上。
        if (!HudConfig.hudMode()) return;
        if (!SelfStats.visible()) return;
        List<Component> lines = SelfStats.lines();
        if (lines.isEmpty()) return;
        // 位置按当前 GUI 尺寸换算（存的是「相对位置」，窗口一变不会错位）。
        HudConfig.Panel panel = HudConfig.panel(HudConfig.get().selfStatsMode, false,
                g.guiWidth(), g.guiHeight());
        // 面板文字阴影有自己的开关，不再跟着实体血条的 textShadow。
        draw(g, mc.font, lines, panel, cfg.selfStatsTextShadow);
    }

    /** 在容器界面上画属性面板（「在容器界面显示」模式），并在光标下显示该属性的官方名称。 */
    static void drawOnContainer(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        // 容器面板同样走统一闸门。
        if (HudRenderGate.suppressPanel(mc)) return;
        if (!HudConfig.containerMode()) return;
        if (!SelfStats.visible()) return;
        List<Component> lines = SelfStats.lines();
        if (lines.isEmpty()) return;
        HudConfig.Panel panel = HudConfig.panel(HudConfig.get().selfStatsMode, true,
                g.guiWidth(), g.guiHeight());
        draw(g, mc.font, lines, panel, cfg.selfStatsTextShadow);
        // 光标停在某一行上时，显示这一行的官方 hover（和原版 ViewStats 聊天里一样）。
        int index = lineAt(mc.font, lines, panel, mouseX, mouseY, g.guiWidth(), g.guiHeight());
        if (index >= 0) {
            Component hover = SelfStats.hoverAt(index);
            if (hover != null) {
                g.setComponentTooltipForNextFrame(mc.font, List.of(hover), mouseX, mouseY);
            }
        }
    }

    /** 屏幕坐标 → 命中的行下标（没有命中返回 -1）。 */
    static int lineAt(Font font, List<Component> lines, HudConfig.Panel panel,
                      double mouseX, double mouseY, int guiW, int guiH) {
        if (font == null || lines == null || lines.isEmpty() || panel == null) return -1;
        int cols = Math.max(1, Math.min(3, panel.columns));
        int rows = rows(lines.size(), cols);

        int[] widths = new int[cols];
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int w = Math.max(font.width(line), font.width(line.getString()));
            int c = columnOf(i, rows);
            if (w > widths[c]) widths[c] = w;
        }
        int[] offsets = columnOffsets(widths, COLUMN_GAP);

        // 与 draw() 用同一个「夹进屏幕后」的原点，否则悬停高亮会和实际绘制错位。
        int totalWidth = offsets[cols - 1] + Math.max(0, widths[cols - 1]);
        double originX = fitInScreen(panel.x, totalWidth, guiW);
        double originY = fitInScreen(panel.y, Math.max(1, rows) * ROW_HEIGHT, guiH);
        double lx = (mouseX - originX);
        double ly = (mouseY - originY);
        if (lx < 0 || ly < 0) return -1;
        int r = (int) (ly / ROW_HEIGHT);
        if (r >= rows) return -1;

        for (int c = 0; c < cols; c++) {
            int start = c * rows;
            if (start >= lines.size()) break;
            if (lx >= offsets[c] && lx <= offsets[c] + widths[c]) {
                int index = start + r;
                return index < Math.min(lines.size(), start + rows) ? index : -1;
            }
        }
        return -1;
    }

    /** 画整块面板（HUD 元素与「调整位置…」预览共用）。 */
    static void draw(GuiGraphicsExtractor g, Font font, List<Component> lines,
                     HudConfig.Panel panel, boolean shadow) {
        if (g == null || font == null || lines == null || lines.isEmpty() || panel == null) return;
        int cols = Math.max(1, Math.min(3, panel.columns));
        int rows = rows(lines.size(), cols);
        int[] widths = new int[cols];
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            // 地图那些「翻译键 + 参数」的行（计时行）用 Component 量宽会漏掉参数，
            // 所以宽度取两者较大的那个，仅用来排各列起点。
            int w = Math.max(font.width(line), font.width(line.getString()));
            int c = columnOf(i, rows);
            if (w > widths[c]) widths[c] = w;
        }
        int[] offsets = columnOffsets(widths, COLUMN_GAP);

        int alpha = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, panel.opacity)));
        int color = (alpha << 24) | 0xFFFFFF;

        // 绘制时再夹一次屏幕 —— 换窗口 / 改 GUI Scale / 容器界面尺寸不同时，
        // 换算出来的坐标仍可能把面板顶出屏幕（上游 HealthBarRenderer 也是这么做的）。
        int totalWidth = offsets[cols - 1] + Math.max(0, widths[cols - 1]);
        int totalHeight = Math.max(1, rows) * ROW_HEIGHT;
        int originX = fitInScreen(panel.x, totalWidth, g.guiWidth());
        int originY = fitInScreen(panel.y, totalHeight, g.guiHeight());
        g.pose().pushMatrix();
        g.pose().translate((float) originX, (float) originY);
        for (int i = 0; i < lines.size(); i++) {
            int c = columnOf(i, rows);
            int r = i - c * rows;
            g.text(font, lines.get(i), offsets[c], r * ROW_HEIGHT, color, shadow);
        }
        g.pose().popMatrix();
    }

    /**
     * 面板在屏幕上的外框（GUI 像素，已含缩放）：{@code {x, y, width, height}}。
     *
     * <p>「调整位置…」界面用它做命中判定与高亮框。</p>
     */
    static int[] bounds(Font font, List<Component> lines, HudConfig.Panel panel) {
        if (font == null || lines == null || lines.isEmpty() || panel == null) {
            return new int[]{Math.max(0, panel == null ? 0 : panel.x), Math.max(0, panel == null ? 0 : panel.y), 0, 0};
        }
        int cols = Math.max(1, Math.min(3, panel.columns));
        int rows = rows(lines.size(), cols);
        int[] widths = new int[cols];
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int w = Math.max(font.width(line), font.width(line.getString()));
            int c = columnOf(i, rows);
            if (w > widths[c]) widths[c] = w;
        }
        return boundsOf(widths, rows, panel);
    }

    /** 外框的纯函数版本（给定每列宽度与行数，自检直接打）。 */
    static int[] boundsOf(int[] columnWidths, int rowCount, HudConfig.Panel panel) {
        if (panel == null) return new int[]{0, 0, 0, 0};
        int total = 0;
        if (columnWidths != null) {
            for (int i = 0; i < columnWidths.length; i++) {
                total += Math.max(0, columnWidths[i]);
                if (i > 0) total += COLUMN_GAP;
            }
        }
        int x = Math.max(0, panel.x);
        int y = Math.max(0, panel.y);
        return new int[]{x, y, total, Math.max(0, rowCount) * ROW_HEIGHT};
    }

    /** 把面板坐标夹到屏幕内（整块可见）。 */
    static int fitInScreen(int pos, int size, int screen) {
        return Math.max(0, Math.min(Math.max(0, screen - Math.max(1, size)), pos));
    }

    /**
     * 抓取判定：点落在面板外 {@code margin} px 以内才算抓住。
     *
     * <p>拖动界面只认这个判定——点空白处不再改动位置，避免误触把面板/指示器挪走。</p>
     */
    static boolean grabHit(int[] bounds, int margin, double x, double y) {
        if (bounds == null) return false;
        int m = Math.max(0, margin);
        return x >= bounds[0] - m && x <= bounds[0] + bounds[2] + m
                && y >= bounds[1] - m && y <= bounds[1] + bounds[3] + m;
    }

    /** 拖动阈值：按下后位移不到 {@code threshold} px 就当成「点了一下」，不写位置。 */
    static boolean dragExceeds(double dx, double dy, int threshold) {
        double t = Math.max(1, threshold);
        return dx * dx + dy * dy >= t * t;
    }

    /** 没有实时数据时，用这几行示例撑起预览（界面里不会显示假数据到 HUD）。 */
    static List<Component> demoLines() {
        List<Component> out = new ArrayList<>();
        out.add(colored("120", SelfStats.RED).append(colored("\u2764", SelfStats.RED)));
        out.add(colored("1", SelfStats.RED).append(colored("\uD83D\uDDE1", SelfStats.RED)));
        out.add(colored("10", SelfStats.RED).append(colored("\u2764%", SelfStats.RED)));
        out.add(colored("40", SelfStats.YELLOW).append(colored("\u2764", SelfStats.YELLOW)));
        out.add(colored("15", 0xFFAA00).append(colored("\u270F", 0xFFAA00)));
        out.add(colored("0", SelfStats.RED).append(colored("\u2620", SelfStats.RED)));
        return out;
    }

    private static net.minecraft.network.chat.MutableComponent colored(String text, int rgb) {
        return Component.literal(text).withStyle(style -> style.withColor(TextColor.fromRgb(rgb)));
    }

    // ── 布局（纯函数，selfCheck 直接打） ──

    /** 给定行数与列数，需要几行（列优先填充：先填满第一列再换下一列）。 */
    static int rows(int lineCount, int columns) {
        if (lineCount <= 0) return 0;
        int cols = Math.max(1, columns);
        return (lineCount + cols - 1) / cols;
    }

    /** 第 {@code index} 行落在第几列（列优先）。 */
    static int columnOf(int index, int rowsPerColumn) {
        if (rowsPerColumn <= 0) return 0;
        return index / rowsPerColumn;
    }

    /** 每列的起始 x（相对面板左上角）。 */
    static int[] columnOffsets(int[] columnWidths, int gap) {
        int[] out = new int[columnWidths.length];
        int run = 0;
        for (int i = 0; i < columnWidths.length; i++) {
            out[i] = run;
            run += columnWidths[i] + gap;
        }
        return out;
    }
}
