package com.nyalium.cttenemyhud;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 判断「这只实体归本 Mod 管吗 / 这一帧要画它头顶的 HUD 吗」。
 *
 * <p>两个层次：</p>
 * <ol>
 *   <li>{@link #managed}：数据包管控的敌人（有 CTT 计分板数据）或队伍条里的队友。
 *       只要为真，原版 {@code below_name} 分数就该被摘掉（信息已由血条表达）；</li>
 *   <li>{@link #visible}：在 {@code managed} 之上再叠加距离 / 遮挡 / 显示项开关，
 *       为真时本 Mod 才会画出数值、血条与<b>名称</b>。</li>
 * </ol>
 *
 * <p><b>两处必须完全一致</b>：{@link EnemyHudElement} 用 {@link #visible} 决定画不画，
 * {@code NameTagScoreMixin} 用它决定要不要把原版名称清掉。判据一旦有偏差，就会出现
 * 「名称被清掉但没重画」的空档，或者「原版名称与自绘名称叠在一起」的重影。</p>
 *
 * <h2>缓存</h2>
 * <p>同一帧里世界渲染阶段（Mixin）与 HUD 阶段（元素）会各问一次，其中「是否被方块挡住」
 * 要打一条 {@code clip} 射线。用 8 ms 的实体身份缓存把一帧里的重复询问合并成一次，
 * 避免每帧射线数翻倍。</p>
 */
public final class HudTarget {

    /** 一帧的量级：世界渲染 → HUD 渲染之间只有几微秒。 */
    private static final long MEMO_NS = 8_000_000L;

    private static final Map<Entity, Boolean> MEMO = new IdentityHashMap<>();
    private static long memoStamp = 0L;

    private HudTarget() {}

    /** 本 Mod 接管的实体（有 CTT 数据 / 是队友），不考虑距离与遮挡。 */
    public static boolean managed(Minecraft mc, Entity entity) {
        if (mc == null || entity == null) return false;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled) return false;
        if (mc.level == null || mc.player == null) return false;
        // 诅咒：与地图设定一致，整个 UI 隐藏（名称也不接管，否则名字会凭空消失）。
        if (CurseDetector.isActive(mc)) return false;
        if (entity == mc.player) return false;
        if (entity instanceof ArmorStand) return false;
        if (!entity.isAlive() || entity.isInvisible()) return false;

        if (entity instanceof Player player) {
            return cfg.showTeammates && BossBarTracker.isTeammate(mc, player);
        }
        // 有数据才算「数据包管控的敌人」——这一条也排除了掉落物、船、画等。
        return EnemyData.hasData(mc.level.getScoreboard(), entity);
    }

    /** 这一帧真的会画这只实体头顶的 HUD（含名称）。 */
    public static boolean visible(Minecraft mc, Entity entity) {
        if (mc == null || entity == null) return false;
        long now = System.nanoTime();
        if (now - memoStamp > MEMO_NS) {
            MEMO.clear();
            memoStamp = now;
        }
        Boolean cached = MEMO.get(entity);
        if (cached != null) return cached;
        boolean result;
        try {
            result = computeVisible(mc, entity);
        } catch (Throwable t) {
            result = false;
        }
        MEMO.put(entity, result);
        return result;
    }

    private static boolean computeVisible(Minecraft mc, Entity entity) {
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!managed(mc, entity)) return false;

        Vec3 camPos = cameraPos(mc);
        double distance = camPos.distanceTo(entity.position());
        double limit = entity instanceof Player ? cfg.teamMaxDistance : cfg.maxDistance;
        if (distance > limit) return false;

        if (!entity.shouldRender(camPos.x, camPos.y, camPos.z)) return false;
        return !cfg.hideBehindWalls || !occluded(mc, entity, camPos);
    }

    /** 相机位置；拿不到时退回玩家眼睛位置。 */
    static Vec3 cameraPos(Minecraft mc) {
        try {
            if (mc.gameRenderer != null) {
                Camera camera = mc.gameRenderer.mainCamera();
                if (camera != null) return camera.position();
            }
        } catch (Throwable ignored) {
            // 某些状态下没有相机，退回玩家位置。
        }
        return mc.player != null ? mc.player.getEyePosition() : Vec3.ZERO;
    }

    /**
     * 原版名牌锚点，与 {@code SubmitNodeCollection.submitNameTag} 一致：
     * 实体位置 + {@code EntityAttachment.NAME_TAG} 局部偏移 + 0.5 格。
     */
    public static Vec3 nameTagAnchor(Entity entity, Vec3 pos, float partialTick) {
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

    private static boolean occluded(Minecraft mc, Entity entity, Vec3 from) {
        try {
            Vec3 to = nameTagAnchor(entity, entity.position(), 0.0f);
            HitResult hit = mc.level.clip(new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            return hit != null && hit.getType() != HitResult.Type.MISS;
        } catch (Throwable t) {
            return false;
        }
    }
}
