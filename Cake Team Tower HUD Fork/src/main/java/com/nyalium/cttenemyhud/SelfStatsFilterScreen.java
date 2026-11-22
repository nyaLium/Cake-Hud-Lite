package com.nyalium.cttenemyhud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「内容过滤」界面（1.20.4）：勾掉某一类，属性面板里就不再出现那一类。
 *
 * <p>每个开关对应 {@link SelfStatsKind#FILTERABLE} 里的一项（红心 / 魂心 / 生命 /
 * 攻击 / 防御 / 元素护甲 / 速度 / 法力 / 杂项 / 状态 / 计时 / 碎心 / 粉心）。</p>
 *
 * <p>编辑的是<b>当前生效</b>的那一份列表：认到了职业就改该职业的表，否则改默认表 ——
 * 所以在不同职业下打开这个界面，改的就是那个职业专属的过滤。</p>
 *
 * <p>布局按屏幕自适应：宽屏 3 列、窄屏 2 列，行距在矮屏上压缩，「返回」永远贴底。</p>
 */
public final class SelfStatsFilterScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int TOP = 60;
    private static final int GAP = 8;

    private final EnemyHudSettingsScreen parent;
    private List<String> hidden;

    public SelfStatsFilterScreen(EnemyHudSettingsScreen parent) {
        super(Component.translatable("ctt_enemy_hud.config.selfStatsFilter"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.hidden = EnemyHudConfig.hiddenList();
        SelfStatsKind[] kinds = SelfStatsKind.FILTERABLE;
        int columns = this.width >= 380 ? 3 : 2;
        int w = Math.max(70, Math.min(150, (this.width - 24 - (columns - 1) * GAP) / columns));
        int totalWidth = columns * w + (columns - 1) * GAP;
        int x0 = Math.max(4, this.width / 2 - totalWidth / 2);
        int rows = Math.max(1, (kinds.length + columns - 1) / columns);
        int top = Math.min(TOP, Math.max(30, this.height / 6));
        int rowGap = Math.max(18, Math.min(24, (this.height - top - 40) / rows));

        for (int i = 0; i < kinds.length; i++) {
            SelfStatsKind kind = kinds[i];
            int column = i % columns;
            int row = i / columns;
            int x = x0 + column * (w + GAP);
            int y = top + row * rowGap;
            addRenderableWidget(CycleButton.onOffBuilder(shown(kind))
                    .create(x, y, w, ROW_HEIGHT, label(kind), (button, value) -> set(kind, value)));
        }

        int buttonY = Math.min(top + rows * rowGap + 6, this.height - ROW_HEIGHT - 6);
        int buttonWidth = Math.max(60, Math.min(120, (this.width - 24) / 2));
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.selfStatsFilter.showAll"), b -> {
            this.hidden.clear();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - buttonWidth - GAP / 2, buttonY, buttonWidth, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_enemy_hud.config.back"), b -> back())
                .bounds(this.width / 2 + GAP / 2, buttonY, buttonWidth, ROW_HEIGHT).build());
    }

    private boolean shown(SelfStatsKind kind) {
        return !this.hidden.contains(kind.id);
    }

    private void set(SelfStatsKind kind, boolean show) {
        if (show) {
            this.hidden.remove(kind.id);
        } else if (!this.hidden.contains(kind.id)) {
            this.hidden.add(kind.id);
        }
    }

    private void back() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("ctt_enemy_hud.config.title"),
                this.width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.title, this.width / 2, 26, 0xFFFFD060);
        String target = targetLabel().getString();
        graphics.text(this.font, this.font.plainSubstrByWidth(target, Math.max(20, this.width - 16)),
                8, 42, 0xFF9FE8FF, true);
    }

    /** 「现在编辑的是哪一份」：默认表还是某个职业表。 */
    private Component targetLabel() {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        String detected = ClassTracker.current();
        if (ClassTracker.MODE_OFF.equals(cfg.selfStatsClass)) {
            return Component.translatable("ctt_enemy_hud.config.selfStatsFilter.targetDefault");
        }
        if (ClassTracker.MODE_AUTO.equals(cfg.selfStatsClass) && detected == null) {
            return Component.translatable("ctt_enemy_hud.config.selfStatsFilter.targetUnknown");
        }
        String key = ClassTracker.resolvedClass(cfg, detected);
        return Component.translatable("ctt_enemy_hud.config.selfStatsFilter.targetClass",
                key == null ? "?" : ClassTracker.displayName(key));
    }

    @Override
    public void onClose() {
        back();
    }

    private static Component label(SelfStatsKind kind) {
        return Component.translatable("ctt_enemy_hud.config.kind." + kind.id);
    }
}
