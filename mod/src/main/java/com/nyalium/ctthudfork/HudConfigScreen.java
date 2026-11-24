package com.nyalium.ctthudfork;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 设置总界面（{@code F8} 或 Mod Menu 打开）。
 *
 * <pre>
 *   CTT HUD - Fork 设置
 *   ┌────────────────────────┐
 *   │  总开关：开 / 关         │
 *   │  实体血条设置…           │  ← 可滚动列表
 *   │  属性面板设置…           │  ← 可滚动列表
 *   │  盾牌指示器设置…         │
 *   │  恢复默认                │
 *   └────────────────────────┘
 *          [ 完成 ]            ← 贴屏幕最底部
 * </pre>
 *
 * <p>一纵排布、从 {@code TOP} 起排列；总开关关闭时三个入口灰掉（1.20.19）。
 * 子界面各有自己的「返回」；写盘只发生在总界面的「完成」（或 ESC）。</p>
 *
 * <p>「完成」= 归一化后写盘并关闭；总界面 ESC 等同「完成」。</p>
 */
public final class HudConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 56;
    private static final int BUTTON_WIDTH = 220;

    private final Screen parent;

    public HudConfigScreen(Screen parent) {
        super(Component.translatable("ctt_hud_fork.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int y = TOP;
        HudConfig.Cfg cfg = HudConfig.get();

        // 总开关（1.20.19 从「通用设置」移到外层）。
        addRenderableWidget(CycleButton.onOffBuilder(cfg.enabled)
                .create(x, y, BUTTON_WIDTH, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.enabled"),
                        (b, v) -> {
                            HudConfig.get().enabled = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        Button entity = Button.builder(section("entity"),
                        b -> this.minecraft.gui.setScreen(new EntityHudSettingsScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build();
        if (!cfg.enabled) entity.active = false;
        addRenderableWidget(entity);
        y += ROW_GAP;

        Button stats = Button.builder(section("self_stats"),
                        b -> this.minecraft.gui.setScreen(new SelfStatsSettingsScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build();
        if (!cfg.enabled) stats.active = false;
        addRenderableWidget(stats);
        y += ROW_GAP;

        Button shield = Button.builder(section("shield"),
                        b -> this.minecraft.gui.setScreen(new ShieldIndicatorSettingsScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build();
        if (!cfg.enabled) shield.active = false;
        addRenderableWidget(shield);
        y += ROW_GAP;

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.reset"), b -> {
            // 只改内存、不落盘：还要点「完成」才写盘。
            HudConfig.reset();
            this.rebuildWidgets();
        }).bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
        y += ROW_GAP;

        // 1.3.0：调试模式（写 cfg.debug）。开着时：
        //   ① 头顶 HUD 也画**自己**（正常跳过本地玩家）；
        //   ② 属性面板左下角打印一行调试信息。
        CycleButton<Boolean> debug = CycleButton.onOffBuilder(cfg.debug)
                .create(x, y, BUTTON_WIDTH, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.debug"),
                        (b, v) -> HudConfig.get().debug = v);
        addRenderableWidget(debug);

        // 完成：永远贴屏幕最底部。
        int doneY = Math.max(y + ROW_GAP, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(x, doneY, BUTTON_WIDTH, ROW_HEIGHT).build());
    }

    /** 完成：写盘并关闭。 */
    private void done() {
        HudConfig.save();
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
        return Component.translatable("ctt_hud_fork.config.section." + id);
    }
}
