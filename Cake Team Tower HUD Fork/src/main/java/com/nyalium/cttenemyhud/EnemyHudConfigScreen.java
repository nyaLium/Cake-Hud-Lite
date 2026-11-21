package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Mod Menu 配置总界面。
 *
 * <pre>
 *   CTT敌人血条设置
 *   ┌──────────────┐
 *   │  通用设置     │
 *   │  敌人设置     │
 *   │  玩家设置     │
 *   │  恢复默认     │
 *   └──────────────┘
 * </pre>
 *
 * <p>每一项点进去是独立的子界面（见 {@link EnemyHudSettingsScreen}），不再用「上一类 / 下一类」翻页。
 * 关闭总界面时才归一化并写回磁盘。</p>
 */
public final class EnemyHudConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int BUTTON_WIDTH = 220;

    private final Screen parent;

    public EnemyHudConfigScreen(Screen parent) {
        super(Component.translatable("ctt_enemy_hud.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        // 四条按钮整体居中（标题占上面一行）。
        int startY = this.height / 2 - (ROW_GAP * 4) / 2 + 8;

        addRenderableWidget(Button.builder(section("general"), b -> open(EnemyHudSettingsScreen.Section.GENERAL))
                .bounds(x, startY, BUTTON_WIDTH, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(section("enemy"), b -> open(EnemyHudSettingsScreen.Section.ENEMY))
                .bounds(x, startY + ROW_GAP, BUTTON_WIDTH, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(section("player"), b -> open(EnemyHudSettingsScreen.Section.PLAYER))
                .bounds(x, startY + ROW_GAP * 2, BUTTON_WIDTH, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.reset"), b -> {
            // 立刻落盘，避免「点了恢复默认又直接退游戏」时文件里还是旧值。
            EnemyHudConfig.reset();
            EnemyHudConfig.save();
            this.minecraft.gui.setScreen(new EnemyHudConfigScreen(this.parent));
        }).bounds(x, startY + ROW_GAP * 3, BUTTON_WIDTH, ROW_HEIGHT).build());
    }

    private void open(EnemyHudSettingsScreen.Section section) {
        this.minecraft.gui.setScreen(new EnemyHudSettingsScreen(this, section));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.title"),
                this.width / 2, this.height / 2 - (ROW_GAP * 4) / 2 - 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        EnemyHudConfig.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    private static Component section(String id) {
        return Component.translatable("ctt_enemy_hud.config.section." + id);
    }
}
