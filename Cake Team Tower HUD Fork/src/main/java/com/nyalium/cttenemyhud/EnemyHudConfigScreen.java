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
 *          [ 完成 ]        ← 贴屏幕最底部
 * </pre>
 *
 * <p>四项一纵排布、从 {@code TOP} 起排列；「完成」固定在屏幕底部。
 * 点进去是独立子界面（见 {@link EnemyHudSettingsScreen}），子界面只有「返回」。</p>
 *
 * <p>「完成」= 归一化后写盘并关闭；总界面 ESC 等同「完成」。</p>
 */
public final class EnemyHudConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int BUTTON_WIDTH = 220;

    private final Screen parent;

    public EnemyHudConfigScreen(Screen parent) {
        super(Component.translatable("ctt_enemy_hud.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int y = TOP;

        addRenderableWidget(Button.builder(section("general"),
                        b -> open(EnemyHudSettingsScreen.Section.GENERAL))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        y += ROW_GAP;
        addRenderableWidget(Button.builder(section("enemy"),
                        b -> open(EnemyHudSettingsScreen.Section.ENEMY))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        y += ROW_GAP;
        addRenderableWidget(Button.builder(section("player"),
                        b -> open(EnemyHudSettingsScreen.Section.PLAYER))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        y += ROW_GAP;
        addRenderableWidget(Button.builder(section("self_stats"),
                        b -> open(EnemyHudSettingsScreen.Section.SELF_STATS))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        y += ROW_GAP;
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.reset"), b -> {
            // 只改内存、不落盘：还要点「完成」才写盘。
            EnemyHudConfig.reset();
        }).bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());

        // 完成：永远贴屏幕最底部。
        int doneY = Math.max(y + ROW_GAP, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(x, doneY, BUTTON_WIDTH, ROW_HEIGHT).build());
    }

    private void open(EnemyHudSettingsScreen.Section section) {
        this.minecraft.gui.setScreen(new EnemyHudSettingsScreen(this, section));
    }

    /** 完成：写盘并关闭。 */
    private void done() {
        EnemyHudConfig.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
    }

    /** ESC 等同「完成」（与原版设置界面一致）。 */
    @Override
    public void onClose() {
        done();
    }

    private static Component section(String id) {
        return Component.translatable("ctt_enemy_hud.config.section." + id);
    }
}
