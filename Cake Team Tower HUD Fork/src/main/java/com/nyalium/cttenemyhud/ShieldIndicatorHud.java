package com.nyalium.cttenemyhud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 盾牌计量条（1.20.19 合并自 CTT Shield Indicator；1.20.20 改为可拖动定位 + 透明度）。
 *
 * <p>判定：副手物品非空、{@code custom_data} 里 {@code shield:1b}/{@code shieldAbility:1b} 为真、
 * 可损坏且耐久未满；耐久满（damage=0）不画。</p>
 *
 * <p>位置与属性面板一致：{@code shieldX/Y} 是屏幕 GUI 坐标（左上角），
 * 未设置（-1）时默认贴副手槽上方。</p>
 */
final class ShieldIndicatorHud implements HudElement {

    /** 新纹理：36×106。 */
    static final int TEX_W = 36;
    static final int TEX_H = 106;

    private static final Identifier[] METER = {
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_0.png"),
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_1.png"),
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_2.png"),
            Identifier.fromNamespaceAndPath(CttEnemyHudClient.MOD_ID, "textures/gui/meter_3.png"),
    };

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
        if (damage <= 0) return;
        float ratio = (float) damage / (float) maxDamage;
        int stage = ratio < 0.4f ? 3 : ratio < 0.6f ? 2 : ratio < 0.8f ? 1 : 0;

        int guiW = graphics.guiWidth();
        int guiH = graphics.guiHeight();
        // 1.20.21：尺寸是「倍」，按整数倍放大原纹理，不再拉伸纹理（避免 1.1 倍时的双列伪影）。
        int scale = Math.max(1, Math.round((float) cfg.shieldScale));
        int w = TEX_W * scale;
        int h = TEX_H * scale;

        int x = cfg.shieldX;
        int y = cfg.shieldY;
        if (x < 0 || y < 0) {
            HumanoidArm arm = mc.player.getMainArm().getOpposite();
            x = Math.round(arm == HumanoidArm.LEFT ? guiW / 2.0f - 109.0f - w / 2.0f
                    : guiW / 2.0f + 109.0f - w / 2.0f);
            y = guiH - 76 - h;
        }
        x = Math.max(0, Math.min(guiW - w, x));
        y = Math.max(0, Math.min(guiH - h, y));

        int alpha = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, cfg.shieldOpacity)));
        int color = (alpha << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, METER[stage], x, y, 0.0f, 0.0f,
                w, h, TEX_W, TEX_H, color);
    }

    private static boolean hasShieldFlag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        if (tag == null) return false;
        return tag.getBoolean("shield").orElse(false) || tag.getBoolean("shieldAbility").orElse(false);
    }
}
