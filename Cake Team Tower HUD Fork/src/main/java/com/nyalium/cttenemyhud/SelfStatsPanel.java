package com.nyalium.cttenemyhud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 自身属性面板（GUI 层，一列/两列文字）。
 *
 * <p>内容就是地图 {@code /trigger ViewStats} 回的那几十行 {@link Component}，
 * 原样按列排开画出来 —— 颜色、图标、悬停提示都保留，和游戏里聊天栏看到的一致。</p>
 *
 * <p>位置 / 列数在配置里：{@code selfStatsX}、{@code selfStatsY}、{@code selfStatsColumns}、
 * {@code showSelfStats}；刷新间隔 {@code selfStatsSeconds}（0 = 关）。</p>
 */
final class SelfStatsPanel implements HudElement {

    /** 行高（字体 8 px + 2 px 行距，和原版聊天行距一致）。 */
    static final int ROW_HEIGHT = 10;
    /** 列间距。 */
    static final int COLUMN_GAP = 8;
    /** 面板内边距。 */
    private static final int PADDING = 3;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.showSelfStats) return;
        List<Component> lines = SelfStats.lines();
        if (lines.isEmpty() || !SelfStats.visible()) return;

        Font font = mc.font;
        int cols = Math.max(1, Math.min(3, cfg.selfStatsColumns));
        int rows = rows(lines.size(), cols);

        int[] colWidths = new int[cols];
        for (int i = 0; i < lines.size(); i++) {
            // 地图那些「翻译键 + 参数」的行（计时行）用 Component 量宽会漏掉参数，
            // 所以只用来排各列的起点，不画背景框。
            Component line = lines.get(i);
            int w = Math.max(font.width(line), font.width(line.getString()));
            int c = columnOf(i, rows);
            if (w > colWidths[c]) colWidths[c] = w;
        }
        int[] offsets = columnOffsets(colWidths, COLUMN_GAP);

        int x = Math.max(0, cfg.selfStatsX);
        int y = Math.max(0, cfg.selfStatsY);
        // 不画背景框：地图那些「翻译键 + 参数」的行（计时行）用 Component 量出来的宽度
        // 比实际渲染窄，背景框会盖不住；属性行本身自带颜色，直接画字更稳也更像原版。
        for (int i = 0; i < lines.size(); i++) {
            int c = columnOf(i, rows);
            int r = i - c * rows;
            g.text(font, lines.get(i), x + offsets[c], y + r * ROW_HEIGHT, 0xFFFFFFFF);
        }
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
