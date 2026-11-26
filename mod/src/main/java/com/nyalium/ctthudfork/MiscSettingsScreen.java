package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 「杂项」设置页（1.6.2）。
 *
 * <p>放那些不属于某一块 HUD、但会影响全局渲染开销的入口。目前只有一项：</p>
 * <ul>
 *   <li><b>粗体渲染…</b> → {@link BoldSettingsScreen}：是否启用 / 粗体模式 / 膨胀强度 / 偏移距离。</li>
 * </ul>
 */
public final class MiscSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int WIDTH = 260;

    private final HudConfigScreen menu;

    public MiscSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.section.misc"));
        this.menu = menu;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;

        // 粗体渲染… → BoldSettingsScreen
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.section.bold"),
                        b -> this.minecraft.gui.setScreen(new BoldSettingsScreen(this)))
                .bounds(x, y, width, ROW_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetMisc();
            this.init();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private void back() {
        HudConfig.save();
        this.minecraft.gui.setScreen(this.menu);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.miscHint"),
                this.width / 2, 40, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        back();
    }
}
