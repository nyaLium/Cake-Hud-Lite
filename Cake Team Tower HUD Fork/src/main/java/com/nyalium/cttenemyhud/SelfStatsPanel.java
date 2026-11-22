package com.nyalium.cttenemyhud;

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
 *   <li>{@code selfStatsScale}：整块文字缩放（{@link GuiGraphicsExtractor#pose()} 变换）；</li>
 *   <li>{@code selfStatsOpacity}：整块文字透明度；</li>
 *   <li>内容过滤：{@link SelfStats#lines()} 已经把「隐藏类」剔掉了；</li>
 *   <li>同样的绘制代码给「移动面板」界面做预览用（{@link #draw}）。</li>
 * </ul>
 */
final class SelfStatsPanel implements HudElement {

    /** 行高（字体 8 px + 2 px 行距，和原版聊天行距一致）。 */
    static final int ROW_HEIGHT = 10;
    /** 列间距。 */
    static final int COLUMN_GAP = 8;
    /** 拖拽界面里至少留多少像素在屏幕内。 */
    private static final int EDGE_KEEP = 16;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        // 1.20.14：「在容器界面显示」模式下 HUD 上不画，改由 ContainerPanelMixin 画在容器界面上。
        if (!EnemyHudConfig.hudMode()) return;
        if (!SelfStats.visible()) return;
        List<Component> lines = SelfStats.lines();
        if (lines.isEmpty()) return;
        EnemyHudConfig.Panel panel = EnemyHudConfig.panel();
        draw(g, mc.font, lines, panel, cfg.textShadow);
        if (cfg.debug) {
            // 排查用：屏幕/面板尺寸、过滤前后的行数（debug=true 时左下角一行，避开地图顶部的字）。
            int guiW = mc.getWindow().getGuiScaledWidth();
            int guiH = mc.getWindow().getGuiScaledHeight();
            String info = String.format(java.util.Locale.ROOT,
                    "panel %d,%d cols=%d scale=%.2f op=%.2f | lines %d/%d | gui %dx%d fb %dx%d",
                    panel.x, panel.y, panel.columns, panel.scale, panel.opacity,
                    lines.size(), SelfStats.storedLineCount(),
                    guiW, guiH, mc.getWindow().getWidth(), mc.getWindow().getHeight());
            g.text(mc.font, info, 4, Math.max(4, guiH - 12), 0xFFFFFFFF, true);
        }
    }

    /** 在容器界面上画属性面板（「在容器界面显示」模式），并在光标下显示该属性的官方名称。 */
    static void drawOnContainer(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        if (!EnemyHudConfig.containerMode()) return;
        if (!SelfStats.visible()) return;
        List<Component> lines = SelfStats.lines();
        if (lines.isEmpty()) return;
        EnemyHudConfig.Panel panel = EnemyHudConfig.panel();
        draw(g, mc.font, lines, panel, cfg.textShadow);
        // 1.20.15：光标停在某一行上时，显示这一行的官方 hover（和原版 ViewStats 聊天里一样）。
        int index = lineAt(mc.font, lines, panel, mouseX, mouseY);
        if (index >= 0) {
            Component hover = SelfStats.hoverAt(index);
            if (hover != null) {
                g.setComponentTooltipForNextFrame(mc.font, List.of(hover), mouseX, mouseY);
            }
        }
    }

    /** 屏幕坐标 → 命中的行下标（没有命中返回 -1）。 */
    static int lineAt(Font font, List<Component> lines, EnemyHudConfig.Panel panel,
                      double mouseX, double mouseY) {
        if (font == null || lines == null || lines.isEmpty() || panel == null) return -1;
        int cols = Math.max(1, Math.min(3, panel.columns));
        int rows = rows(lines.size(), cols);
        double scale = Math.max(0.25, Math.min(3.0, panel.scale));
        double lx = (mouseX - panel.x) / scale;
        double ly = (mouseY - panel.y) / scale;
        if (lx < 0 || ly < 0) return -1;
        int r = (int) (ly / ROW_HEIGHT);
        if (r >= rows) return -1;

        int[] widths = new int[cols];
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int w = Math.max(font.width(line), font.width(line.getString()));
            int c = columnOf(i, rows);
            if (w > widths[c]) widths[c] = w;
        }
        int[] offsets = columnOffsets(widths, COLUMN_GAP);
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

    /** 画整块面板（HUD 元素与「移动面板」预览共用）。 */    static void draw(GuiGraphicsExtractor g, Font font, List<Component> lines,
                     EnemyHudConfig.Panel panel, boolean shadow) {
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

        float scale = (float) Math.max(0.25, Math.min(3.0, panel.scale));
        int alpha = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, panel.opacity)));
        int color = (alpha << 24) | 0xFFFFFF;

        g.pose().pushMatrix();
        g.pose().translate((float) Math.max(0, panel.x), (float) Math.max(0, panel.y));
        if (scale != 1.0f) g.pose().scale(scale, scale);
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
     * <p>「移动面板」界面用它做命中判定与高亮框。</p>
     */
    static int[] bounds(Font font, List<Component> lines, EnemyHudConfig.Panel panel) {
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
    static int[] boundsOf(int[] columnWidths, int rowCount, EnemyHudConfig.Panel panel) {
        if (panel == null) return new int[]{0, 0, 0, 0};
        int total = 0;
        if (columnWidths != null) {
            for (int i = 0; i < columnWidths.length; i++) {
                total += Math.max(0, columnWidths[i]);
                if (i > 0) total += COLUMN_GAP;
            }
        }
        float scale = (float) Math.max(0.25, Math.min(3.0, panel.scale));
        int x = Math.max(0, panel.x);
        int y = Math.max(0, panel.y);
        return new int[]{x, y, Math.round(total * scale), Math.round(Math.max(0, rowCount) * ROW_HEIGHT * scale)};
    }

    /** 把面板坐标夹到屏幕内（拖拽用；至少留 {@value #EDGE_KEEP} px 可见）。 */
    static int clampX(int x, int panelWidth, int screenWidth) {
        int max = Math.max(0, screenWidth - Math.min(EDGE_KEEP, Math.max(1, panelWidth)));
        return Math.max(0, Math.min(max, x));
    }

    static int clampY(int y, int panelHeight, int screenHeight) {
        int max = Math.max(0, screenHeight - Math.min(EDGE_KEEP, Math.max(1, panelHeight)));
        return Math.max(0, Math.min(max, y));
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
