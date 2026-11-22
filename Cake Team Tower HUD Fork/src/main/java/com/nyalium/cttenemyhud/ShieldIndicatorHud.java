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
 *
 * <p>1.20.22：缩放改成 {@code 0.5~1.5} 小数倍，并且是<b>真正的缩放</b> ——
 * 源区固定为整张 36×106 纹理，目标框 {@code 36*k × 106*k}。之前用的是
 * {@code blit} 的「宽高当源区」重载，目标放大到 k 倍时 UV 也跟着跨了 k 张纹理，
 * 于是画成了 k×k 平铺（1.2 倍能看到右下角多出两条）。</p>
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

    /** 缩放倍率夹进 {@code 0.5~1.5}。 */
    static double clampScale(double scale) {
        if (Double.isNaN(scale)) return 1.0;
        return Math.max(EnemyHudConfig.SHIELD_SCALE_MIN,
                Math.min(EnemyHudConfig.SHIELD_SCALE_MAX, scale));
    }

    /** 缩放后的宽（GUI 像素，至少 1）。 */
    static int scaledWidth(double scale) {
        return Math.max(1, (int) Math.round(TEX_W * clampScale(scale)));
    }

    /** 缩放后的高（GUI 像素，至少 1）。 */
    static int scaledHeight(double scale) {
        return Math.max(1, (int) Math.round(TEX_H * clampScale(scale)));
    }

    /**
     * 把整张纹理等比缩放到目标框：源区 {(0,0) → (36,106)}，目标 {@code w×h}。
     *
     * <p>10 参数的 {@code blit} 会把宽高同时当源区用（放大 = 平铺），所以这里必须用
     * 13 参数重载：{@code (x, y, u, v, 目标宽, 目标高, 源宽, 源高, 纹理宽, 纹理高, 颜色)}。</p>
     */
    static void blitScaled(GuiGraphicsExtractor g, Identifier texture, int x, int y,
                           double scale, double opacity) {
        int w = scaledWidth(scale);
        int h = scaledHeight(scale);
        int alpha = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, opacity)));
        int color = (alpha << 24) | 0xFFFFFF;
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, w, h,
                TEX_W, TEX_H, TEX_W, TEX_H, color);
    }

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
        int w = scaledWidth(cfg.shieldScale);
        int h = scaledHeight(cfg.shieldScale);

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

        blitScaled(graphics, METER[stage], x, y, cfg.shieldScale, cfg.shieldOpacity);
    }

    private static boolean hasShieldFlag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        if (tag == null) return false;
        return tag.getBoolean("shield").orElse(false) || tag.getBoolean("shieldAbility").orElse(false);
    }
}
