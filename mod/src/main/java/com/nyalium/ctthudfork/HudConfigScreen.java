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
 *   │  重置设置                │
 *   │  调试功能…               │  ← 调试模式 / 优先服务端数据 / 筛选实体数量
 *   └────────────────────────┘
 *          [ 完成 ]            ← 贴屏幕最底部
 * </pre>
 *
 * <p>一纵排布、从 {@code TOP} 起排列；总开关关闭时三个入口灰掉。
 * 子界面各有自己的「返回」；每页的「返回 / ESC / 完成」都会写盘（含拖动、过滤这类子页）。</p>
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

        // /title 的标题 / 副标题（位移 + 不透明度），不含 actionbar。
        Button title = Button.builder(section("title"),
                        b -> this.minecraft.gui.setScreen(new TitleSettingsScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build();
        if (!cfg.enabled) title.active = false;
        addRenderableWidget(title);
        y += ROW_GAP;

        // 原来是「调试模式」一行开关，现在改成入口按钮 → 调试功能页
        // （里面是 调试模式 / 优先使用服务端数据 / 筛选实体数量）。
        // 调试设置需要权限（等级写死在 DebugSettingsScreen.REQUIRED_PERMISSION_LEVEL），
        // 没有权限时这个入口直接灰掉，并在这里补一句提示。
        Button debug = Button.builder(Component.translatable("ctt_hud_fork.config.section.debug"),
                        b -> this.minecraft.gui.setScreen(new DebugSettingsScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, ROW_HEIGHT).build();
        // 调试设置需要权限 + CTT-HUD-LITE-DEBUG 标签。
        // 不满足条件就把入口**直接灰掉**，并且**不写任何提示文案**
        // （调试设置是给维护者排查用的，普通玩家不该去改它）。
        if (!cfg.enabled || !DebugSettingsScreen.canAdjust()) debug.active = false;
        addRenderableWidget(debug);

        // 底部：完成与上方入口**同宽同轴**；重置是它左侧的一个小按钮。
        int bottomY = Math.max(y + ROW_GAP, this.height - ROW_HEIGHT - 8);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(x, bottomY, BUTTON_WIDTH, ROW_HEIGHT).build());
        int resetWidth = 60;
        int resetX = Math.max(0, x - 8 - resetWidth);
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetAll"), b -> {
            // 调试页本来就要求 op≥2 + DEBUG 标签才能改；总重置不能绕过这道门，
            // 否则没权限的玩家一按就把调试字段也清掉了。
            if (DebugSettingsScreen.canAdjust()) {
                HudConfig.reset();
            } else {
                HudConfig.resetPreservingDebug();
            }
            HudConfig.save();
            this.rebuildWidgets();
        }).bounds(resetX, bottomY, resetWidth, ROW_HEIGHT).build());
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
