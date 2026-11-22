package com.nyalium.cttenemyhud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 盾牌计量条（1.20.19 从 CTT Shield Indicator 合并进来）。
 *
 * <p>判定（纯客户端）：副手物品非空、{@code minecraft:custom_data} 里
 * {@code shield:1b}/{@code shieldAbility:1b} 为真、可损坏且耐久未满。
 * 耐久满（damage=0）时不画。</p>
 *
 * <p>位置：副手物品格正上方（默认，可调与 actionbar 的间距）或屏幕正中；
 * 两种位置共用一套缩放/水平偏移（1.20.19 起合并为单套，简化配置）。</p>
 *
 * <p>尺寸按纹理可见内容 52×128 计算：可见宽 = 20px（一格）× scale。</p>
 */
final class ShieldIndicatorHud implements HudElement {

    private static final Identifier[] METER = {
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_0.png"),
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_1.png"),
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_2.png"),
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_3.png"),
    };

    private static final int CANVAS = 136;
    private static final int CONTENT_LEFT = 42;
    private static final int CONTENT_TOP = 4;
    private static final int CONTENT_W = 52;
    private static final int CONTENT_H = 128;
    private static final int SLOT_CELL = 20;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled || !cfg.shieldEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand == null || offhand.isEmpty() || !hasShieldFlag(offhand)) return;
        int maxDamage = offhand.getMaxDamage();
        if (maxDamage <= 0) return;
        int damage = offhand.getDamageValue();
        if (damage <= 0) return;                 // 耐久满 → 不显示
        float ratio = (float) damage / (float) maxDamage;
        int stage = ratio < 0.4f ? 3 : ratio < 0.6f ? 2 : ratio < 0.8f ? 1 : 0;

        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();
        float width = Math.max(1.0f, Math.min(400.0f, SLOT_CELL * (float) cfg.shieldScale));
        float height = width * ((float) CONTENT_H / (float) CONTENT_W);
        float anchorX;
        float bottomY;
        if (cfg.shieldPosition == 1) {
            anchorX = guiW / 2.0f;
            bottomY = guiH / 2.0f + height / 2.0f;
        } else {
            HumanoidArm arm = mc.player.getMainArm().getOpposite();
            anchorX = arm == HumanoidArm.LEFT ? guiW / 2.0f - 109.0f : guiW / 2.0f + 109.0f;
            bottomY = guiH - 72 - cfg.shieldGap;
        }
        anchorX += cfg.shieldOffset;
        float half = width / 2.0f;
        anchorX = Math.max(half + 2.0f, Math.min(guiW - half - 2.0f, anchorX));
        bottomY = Math.max(height + 2.0f, Math.min(guiH - 2.0f, bottomY));

        int x = Math.round(anchorX - half);
        int y = Math.round(bottomY - height);
        graphics.blit(METER[stage], x, y, CONTENT_LEFT, CONTENT_TOP,
                Math.round(width), Math.round(height), CANVAS, CANVAS);
    }

    /** {@code custom_data} 里 {@code shield} 或 {@code shieldAbility} 为真。 */
    private static boolean hasShieldFlag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        if (tag == null) return false;
        return tag.getBoolean("shield").orElse(false) || tag.getBoolean("shieldAbility").orElse(false);
    }
}
