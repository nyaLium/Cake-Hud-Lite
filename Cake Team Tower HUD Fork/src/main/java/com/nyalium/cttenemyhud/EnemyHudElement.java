package com.nyalium.cttenemyhud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 敌人血条 HUD（屏幕空间）：只在实体名牌正上方显示血条与数值。
 *
 * <pre>
 *     当前血量/最大血量 — 🛡防御 🔥火 …
 *     ▬▬▬▬▬▬ 原版 bossbar 纹理 ▬▬▬▬▬▬
 *     原版的自定义名称（本 Mod 不画）
 * </pre>
 *
 * <h2>数据来源（纯客户端）</h2>
 * <ul>
 *   <li>精确值：客户端 Boss 栏 {@code minecraft:targetting} 标题里的 {@code 名字 (HP 当前/上限) 🛡防御}；</li>
 *   <li>兜底：{@code below_name} 展示槽的 {@code HealthPercent} 百分比。</li>
 * </ul>
 *
 * <p>「未知诅咒」期间与地图设定一致，整个 UI 隐藏（{@link CurseDetector}）。</p>
 */
public final class EnemyHudElement implements HudElement {

    /** 原版名牌的世界缩放：1 个「名牌像素」= 0.025 格。 */
    private static final double NAMETAG_SCALE = 0.025;

    private static final double MIN_UNIT = 0.18;
    private static final double MAX_UNIT = 6.0;

    private static final class Placement {
        EnemyData.Snapshot data;
        double gx;
        double gy;
        double ppu;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled) return;
        // 诅咒：与地图设定一致，整个 UI 隐藏；按需求不提供开关。
        if (CurseDetector.isActive(mc)) return;
        if (!cfg.showBar && !cfg.showNumbers && !cfg.showArmor) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        Matrix4f viewProj = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vector3fc left = camera.leftVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        float partialTick = camera.getCameraEntityPartialTicks(deltaTracker);

        int guiW = g.guiWidth();
        int guiH = g.guiHeight();
        double margin = 120.0;

        Scoreboard sb = mc.level.getScoreboard();
        Map<UUID, BossBarTracker.Bar> exact = BossBarTracker.resolve(mc, BossBarTracker.snapshot(mc));

        List<Placement> placements = new ArrayList<>();
        double[] pt = new double[2];
        double[] ptRight = new double[2];
        int scanned = 0;
        int withData = 0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            scanned++;
            if (entity == mc.player) continue;
            if (entity instanceof Player) continue;
            if (entity instanceof ArmorStand) continue;
            if (!entity.isAlive() || entity.isInvisible()) continue;

            Vec3 pos = entity.getPosition(partialTick);
            if (camPos.distanceTo(pos) > cfg.maxDistance) continue;
            if (!entity.shouldRender(camPos.x, camPos.y, camPos.z)) continue;

            EnemyData.Snapshot data = EnemyData.read(sb, entity);
            BossBarTracker.Bar bar = exact.get(entity.getUUID());
            if (bar != null) {
                int[] slots = ArmourCache.snapshot(entity.getUUID());
                int[] elements = new int[EnemyData.ARMOR_OBJECTIVES.length];
                System.arraycopy(slots, 1, elements, 0, elements.length);
                data = EnemyData.exact(bar.hp, bar.maxHp, slots[0], elements);
            }
            if (data == null) continue;
            withData++;

            Vec3 anchor = nameTagAnchor(entity, pos, partialTick);
            if (cfg.hideBehindWalls && blocked(mc, entity, camPos, anchor)) continue;
            if (!project(viewProj, camPos, anchor, guiW, guiH, pt)) continue;
            if (!project(viewProj, camPos, anchor.add(right), guiW, guiH, ptRight)) continue;
            double ppu = ptRight[0] - pt[0];
            if (!(ppu > 1.0e-3)) continue;
            if (pt[0] < -margin || pt[0] > guiW + margin || pt[1] < -margin || pt[1] > guiH + margin) continue;

            Placement p = new Placement();
            p.data = data;
            p.gx = pt[0];
            p.gy = pt[1];
            p.ppu = ppu;
            placements.add(p);
        }

        if (cfg.debug) {
            g.text(mc.font, "CTT EnemyHUD  entities=" + scanned + " withData=" + withData
                            + " drawn=" + placements.size()
                            + (placements.isEmpty() ? "" : "  " + (placements.get(0).data.percentOnly ? "percent" : "absolute")),
                    4, 4, 0xFFFFFF00, true);
        }

        if (placements.isEmpty()) return;
        placements.sort((a, b) -> Double.compare(a.ppu, b.ppu));

        Font font = mc.font;
        int alpha = (int) Math.round(255.0 * cfg.opacity);
        EnemyHudLayout.Options options = options(cfg);

        for (Placement p : placements) {
            drawOne(g, font, p, options, alpha);
        }
    }

    private static EnemyHudLayout.Options options(EnemyHudConfig.Cfg cfg) {
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showNumbers = cfg.showNumbers;
        o.showArmor = cfg.showArmor;
        o.showBar = cfg.showBar;
        o.barWidth = cfg.barWidth;
        o.verticalOffset = cfg.verticalOffset;
        o.textShadow = cfg.textShadow;
        o.scale = cfg.scale;
        return o;
    }

    private static void drawOne(GuiGraphicsExtractor g, Font font, Placement p,
                                EnemyHudLayout.Options options, int alpha) {
        double unit = NAMETAG_SCALE * p.ppu * options.scale;
        if (!(unit >= MIN_UNIT)) return;
        unit = Math.min(unit, MAX_UNIT);

        EnemyHudLayout.Result layout = EnemyHudLayout.compute(
                p.data.hp, p.data.maxHp, p.data.percentOnly,
                p.data.defence, p.data.armor,
                options, font::width);
        if (layout.isEmpty()) return;

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) p.gx, (float) p.gy);
        pose.scale((float) unit, (float) unit);

        if (layout.hasText) {
            int cursor = -layout.textWidth / 2;
            for (EnemyHudLayout.Line line : layout.lines) {
                g.text(font, line.text, cursor, layout.textTop,
                        withAlpha(line.rgb, alpha), options.textShadow);
                cursor += font.width(line.text);
            }
        }

        if (layout.barWidth > 0) {
            pose.pushMatrix();
            pose.translate(0.0f, layout.barTop);
            pose.scale(layout.barWidth / (float) BossBarTexture.NATIVE_WIDTH, 1.0f);
            int bx = -BossBarTexture.NATIVE_WIDTH / 2;
            int white = withAlpha(0xFFFFFF, alpha);
            BossBarTexture.drawBackground(g, bx, 0, BossBarTexture.NATIVE_WIDTH,
                    EnemyHudLayout.BAR_HEIGHT, white);
            int fill = Math.round(BossBarTexture.NATIVE_WIDTH * p.data.ratio());
            fill = Math.max(0, Math.min(BossBarTexture.NATIVE_WIDTH, fill));
            if (fill > 0) {
                BossBarTexture.drawProgress(g, bx, 0, fill, EnemyHudLayout.BAR_HEIGHT, white);
            }
            pose.popMatrix();
        }

        pose.popMatrix();
    }

    // ── 工具 ──

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * 原版名牌锚点，与 {@code SubmitNodeCollection.submitNameTag} 逐位一致：
     * 实体插值位置 + {@code EntityAttachment.NAME_TAG} 局部偏移 + 0.5 格。
     */
    private static Vec3 nameTagAnchor(Entity entity, Vec3 pos, float partialTick) {
        Vec3 attachment = null;
        try {
            attachment = entity.getAttachments()
                    .getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
        } catch (Throwable ignored) {
            // 没有该挂点 / 版本差异时退回原版经典值。
        }
        if (attachment == null) {
            attachment = new Vec3(0.0, entity.getBbHeight(), 0.0);
        }
        return pos.add(attachment.x, attachment.y + 0.5, attachment.z);
    }

    private static boolean project(Matrix4f viewProj, Vec3 camPos, Vec3 world,
                                   int guiW, int guiH, double[] out) {
        Vector4f v = new Vector4f(
                (float) (world.x - camPos.x),
                (float) (world.y - camPos.y),
                (float) (world.z - camPos.z),
                1.0f);
        viewProj.transform(v);
        if (v.w() < 0.3f) return false;
        double ndcX = v.x() / v.w();
        double ndcY = v.y() / v.w();
        if (!Double.isFinite(ndcX) || !Double.isFinite(ndcY)) return false;
        out[0] = (ndcX * 0.5 + 0.5) * guiW;
        out[1] = (0.5 - ndcY * 0.5) * guiH;
        return true;
    }

    private static boolean blocked(Minecraft mc, Entity entity, Vec3 from, Vec3 to) {
        try {
            HitResult hit = mc.level.clip(new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            return hit != null && hit.getType() != HitResult.Type.MISS;
        } catch (Throwable t) {
            return false;
        }
    }
}
