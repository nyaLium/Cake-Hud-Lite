package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 给 mixin 用的公开入口（{@link SelfStatsPanel} 是包可见的）。
 *
 * <p>{@link com.nyalium.cttenemyhud.mixin.ContainerPanelMixin} 在容器界面上调用
 * {@link #drawOnContainer(GuiGraphicsExtractor)}，由面板自己判断模式、开关与数据。</p>
 */
public final class SelfStatsPanelAccess {

    private SelfStatsPanelAccess() {}

    /** 在容器界面上画属性面板（模式不为 1、开关关闭或没有数据时什么都不做）。 */
    public static void drawOnContainer(GuiGraphicsExtractor graphics) {
        SelfStatsPanel.drawOnContainer(graphics);
    }
}
