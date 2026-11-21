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
 *   ┌──────────────┬──────────────┐
 *   │  通用设置     │  敌人设置     │
 *   │  玩家设置     │  恢复默认     │
 *   └──────────────┴──────────────┘
 *          [ 完成 ]  [ 取消 ]
 * </pre>
 *
 * <p>四项按两列排布（与子界面同一套几何：从 {@code TOP} 起、两列居中），点进去是独立子界面
 * （见 {@link EnemyHudSettingsScreen}），不再用「上一类 / 下一类」翻页。</p>
 *
 * <p>底部「完成」= 归一化后写盘并关闭；「取消」= 回滚到打开本界面时的快照（不写盘）并关闭。
 * 快照在构造时抓取，因此进子界面改完再退回来，依然可以整体取消。</p>
 */
public final class EnemyHudConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 52;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_WIDTH_MAX = 210;
    private static final int BUTTON_GAP = 8;

    private final Screen parent;
    /** 「取消」的回滚点：打开本界面那一刻的配置。 */
    private final EnemyHudConfig.Cfg snapshot;

    public EnemyHudConfigScreen(Screen parent) {
        super(Component.translatable("ctt_enemy_hud.config.title"));
        this.parent = parent;
        this.snapshot = EnemyHudConfig.copy();
    }

    /** Mod Menu 传进来的上一层界面（「完成」/「取消」都回到这里）。 */
    Screen root() {
        return parent;
    }

    /** 会话开始时的配置快照。 */
    EnemyHudConfig.Cfg snapshot() {
        return snapshot;
    }

    @Override
    protected void init() {
        int w = Math.max(96, Math.min(ROW_WIDTH_MAX, (this.width - COLUMN_GAP * 3) / 2));
        int leftX = this.width / 2 - w - COLUMN_GAP / 2;
        int rightX = this.width / 2 + COLUMN_GAP / 2;

        addRenderableWidget(Button.builder(section("general"),
                        b -> open(EnemyHudSettingsScreen.Section.GENERAL))
                .bounds(leftX, TOP, w, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(section("enemy"),
                        b -> open(EnemyHudSettingsScreen.Section.ENEMY))
                .bounds(rightX, TOP, w, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(section("player"),
                        b -> open(EnemyHudSettingsScreen.Section.PLAYER))
                .bounds(leftX, TOP + ROW_GAP, w, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.reset"), b -> {
            // 只改内存、不落盘也不重建界面：这样点完「恢复默认」还能用「取消」反悔，
            // 而「取消」的回滚点也不会被默认值覆盖。
            EnemyHudConfig.reset();
        }).bounds(rightX, TOP + ROW_GAP, w, ROW_HEIGHT).build());

        int totalWidth = w * 2 + COLUMN_GAP;
        int half = (totalWidth - BUTTON_GAP) / 2;
        // 贴屏幕底部，但不与上面两行重叠。
        int buttonY = Math.max(TOP + ROW_GAP * 2 + 16, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(leftX, buttonY, half, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> cancel())
                .bounds(leftX + half + BUTTON_GAP, buttonY, half, ROW_HEIGHT).build());
    }

    private void open(EnemyHudSettingsScreen.Section section) {
        this.minecraft.gui.setScreen(new EnemyHudSettingsScreen(this, section));
    }

    /** 完成：写盘并关闭。 */
    private void done() {
        EnemyHudConfig.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    /** 取消：回滚到打开界面时的配置并关闭（不写盘）。 */
    private void cancel() {
        EnemyHudConfig.restore(this.snapshot);
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    /** ESC 等同「完成」（与原版设置界面一致），要反悔请点「取消」。 */
    @Override
    public void onClose() {
        done();
    }

    private static Component section(String id) {
        return Component.translatable("ctt_enemy_hud.config.section." + id);
    }
}
