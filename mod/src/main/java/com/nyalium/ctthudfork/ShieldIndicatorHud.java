package com.nyalium.ctthudfork;

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
 * 盾牌计量条（1.20.19 合并自 CTT Shield Indicator；1.20.20 改为可拖动定位 + 不透明度）。
 *
 * <p>判定：副手物品非空、{@code custom_data} 里 {@code shield:1b}/{@code shieldAbility:1b} 为真、
 * 可损坏且耐久未满；耐久满（damage=0）不画。</p>
 *
 * <p>位置：{@code shieldX/Y} 是屏幕 GUI 坐标（左上角），量在 {@code layoutGuiW/H} 那套尺寸下；
 * 按<b>相对位置</b>换算到当前 GUI（见 {@link HudConfig#shieldPosition}）。
 * 未设置（-1）时走
 * {@link #defaultX}/{@link #defaultY} 的<b>默认位置</b>：</p>
 * <ul>
 *   <li>水平：对准副手**物品格**的中心 —— 右手玩家副手在快捷栏左侧，即 {@code guiW/2 - 109}；
 *       左手玩家（{@code main_hand=left}）镜像到 {@code guiW/2 + 109}；
 *       是否镜像由 {@code shieldAutoMirror} 决定（可在设置里关掉，关掉后固定按右手）；</li>
 *   <li>垂直：整条指示器的**底边**落在屏幕底部往上 76 px（{@code guiH - 76 - h}）。</li>
 * </ul>
 *
 * <p>缩放改成 {@code 0.5~1.5} 小数倍，并且是<b>真正的缩放</b> ——
 * 源区固定为整张纹理，目标框 {@code TEX_W*k × TEX_H*k}。</p>
 *
 * <p>纹理换成 34×86 的新四张（meter_0..3），默认缩放 0.7、默认不透明度 75%；
 * 默认不透明度改为 65%。</p>
 */
final class ShieldIndicatorHud implements HudElement {

    /** 新纹理：34×86（1.20.26 换图）。 */
    static final int TEX_W = 34;
    static final int TEX_H = 86;

    /** 默认位置：指示器**底边**距屏幕底部的像素数。 */
    static final int DEFAULT_BOTTOM_MARGIN = 76;
    /** 默认位置：副手物品格中心距屏幕水平中心的像素数（原版快捷栏 182 宽，物品格中心在 center∓109）。 */
    static final int OFFHAND_CENTER_OFFSET = 109;

    private static final Identifier[] METER = {
            Identifier.fromNamespaceAndPath(CttHudForkClient.MOD_ID, "textures/gui/meter_0.png"),
            Identifier.fromNamespaceAndPath(CttHudForkClient.MOD_ID, "textures/gui/meter_1.png"),
            Identifier.fromNamespaceAndPath(CttHudForkClient.MOD_ID, "textures/gui/meter_2.png"),
            Identifier.fromNamespaceAndPath(CttHudForkClient.MOD_ID, "textures/gui/meter_3.png"),
    };

    /** 缩放倍率夹进 {@code 0.5~1.5}。 */
    static double clampScale(double scale) {
        if (Double.isNaN(scale)) return 1.0;
        return Math.max(HudConfig.SHIELD_SCALE_MIN,
                Math.min(HudConfig.SHIELD_SCALE_MAX, scale));
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
     * 默认位置该用哪一侧（1.2.0 抽出，绘制与拖动预览共用，保证两边一致）。
     *
     * <p>{@code shieldAutoMirror} 开：副手侧跟着主手走（主手右手 → 副手在左；
     * 主手左手 → 副手在右）；关：永远按右手习惯（副手在左），切换主手也不镜像。</p>
     *
     * @param mainArm 玩家主手；{@code null}（界面里还没拿到玩家）按右手处理
     */
    static boolean offhandOnLeft(HudConfig.Cfg cfg, HumanoidArm mainArm) {
        if (!cfg.shieldAutoMirror) return true;
        return mainArm != HumanoidArm.LEFT;
    }

    /**
     * 默认水平位置（左上角 x）：对准副手物品格中心。
     *
     * @param offhandOnLeft {@code true} = 副手在快捷栏左侧（右手玩家，绝大多数情况）
     */
    static int defaultX(boolean offhandOnLeft, int guiWidth, int width) {
        int center = Math.round(guiWidth / 2.0f
                + (offhandOnLeft ? -OFFHAND_CENTER_OFFSET : OFFHAND_CENTER_OFFSET));
        return center - width / 2;
    }

    /** 默认垂直位置（左上角 y）：底边距屏幕底部 {@link #DEFAULT_BOTTOM_MARGIN} px。 */
    static int defaultY(int guiHeight, int height) {
        return guiHeight - DEFAULT_BOTTOM_MARGIN - height;
    }

    /** 把指示器夹进屏幕（整块可见）。 */
    static int clampInto(int pos, int size, int screen) {
        return Math.max(0, Math.min(Math.max(0, screen - Math.max(1, size)), pos));
    }

    /**
     * 把整张纹理等比缩放到目标框：源区 {(0,0) → (TEX_W,TEX_H)}，目标 {@code w×h}。
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
        HudConfig.Cfg cfg = HudConfig.get();
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

        // 位置是「相对位置」—— 配置里存的是量在 layoutGuiW/H 那套 GUI 下的像素，
        // 这里换算到当前尺寸，窗口/缩放变化时不会错位。
        int[] pos = HudConfig.shieldPosition(guiW, guiH);
        int x = pos[0];
        int y = pos[1];
        if (x < 0 || y < 0) {
            boolean offhandOnLeft = offhandOnLeft(cfg, mc.player.getMainArm());
            x = defaultX(offhandOnLeft, guiW, w);
            y = defaultY(guiH, h);
        }
        x = clampInto(x, w, guiW);
        y = clampInto(y, h, guiH);

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
