package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * 「滚轮列表」界面共用的鼠标输入小工具（1.20.32 为滚动条拖拽抽出）。
 *
 * <p>26.3 的 {@link MouseButtonEvent} 在不同输入后端下，坐标有时是 GUI 缩放后的值、
 * 有时是窗口物理像素；这里沿用两个拖动界面（{@code SelfStatsLayoutScreen} /
 * {@code ShieldLayoutScreen}）的判定：坐标一旦超过 GUI 尺寸就按窗口尺寸换算。</p>
 */
final class Pointer {

    private Pointer() {}

    /** 左键为主；部分 SDL/新输入链会把左键报成 1，一并接受。 */
    static boolean accept(MouseButtonEvent event) {
        return event != null && (event.button() == 0 || event.button() == 1);
    }

    /** 事件坐标 → GUI 坐标（{@code {x, y}}）。 */
    static double[] gui(Screen screen, MouseButtonEvent event) {
        if (event == null) return new double[]{0, 0};
        double x = event.x();
        double y = event.y();
        Minecraft mc = Minecraft.getInstance();
        if (screen == null || mc == null || mc.getWindow() == null) return new double[]{x, y};
        int fbW = Math.max(1, mc.getWindow().getWidth());
        int fbH = Math.max(1, mc.getWindow().getHeight());
        if (x > screen.width || y > screen.height) {
            x = x * screen.width / fbW;
            y = y * screen.height / fbH;
        }
        return new double[]{x, y};
    }
}
