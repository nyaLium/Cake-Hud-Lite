package com.nyalium.ctthudfork;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
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
 *   <li>{@link #visible}：在 {@code managed} 之上再叠加距离 / 原版渲染可见性（shouldRender），
 *       为真时本 Mod 才会画出数值、血条与<b>名称</b>。</li>
 * </ol>
 *
 * <p><b>两处必须完全一致</b>：{@link HudWorldRenderer} 用 {@link #visible} 决定画不画，
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

    /** 本 Mod 接管的实体（有 CTT 数据 / 是队友），不考虑距离与渲染可见性。 */
    public static boolean managed(Minecraft mc, Entity entity) {
        if (!uiAllowed(mc)) return false;
        return managedCore(mc, entity);
    }

    /**
     * 「UI 允许显示」的总闸：未知诅咒（整个 UI）与癫狂（实体血条）都关它。
     *
     * <p>1.2.6：癫狂不再是「完全不画」—— 见 {@link HudWorldRenderer}，此时改画百分比条，
     * 但仍不接管名字 / 不摘原版 {@code below_name} 分数（百分比要留给玩家看）。</p>
     */
    static boolean uiAllowed(Minecraft mc) {
        if (mc == null) return false;
        // 1.2.10：癫狂不再挡实体 HUD（只隐藏自身属性面板，见 SelfStatsPanel）。
        return !CurseDetector.isActive(mc);
    }

    /**
     * 是否要把原版 {@code below_name} 分数摘掉（1.2.9）。
     *
     * <p>1.2.10 起癫狂不再影响它（实体 HUD 恢复正常），与 {@link #managed} 完全一致。</p>
     */
    public static boolean hidesBelowNameScore(Minecraft mc, Entity entity) {
        return managed(mc, entity);
    }

    /** 不看诅咒 / 癫狂、只看「这只实体归不归本 Mod 管」。 */
    static boolean managedCore(Minecraft mc, Entity entity) {
        if (mc == null || entity == null) return false;
        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled) return false;
        if (mc.level == null || mc.player == null) return false;
        if (entity == mc.player) return false;
        if (entity instanceof ArmorStand) return false;
        if (!entity.isAlive() || entity.isInvisible()) return false;

        if (entity instanceof Player player) {
            if (!cfg.showTeammates) return false;
            if (BossBarTracker.isTeammate(mc, player)) return true;
            // 队伍条里没有他时的回退：地图给本局参战玩家（tag CTTAll）都算了
            // below_name 的 HealthPercent（health.mcfunction:102-104），有这条分数
            // 就说明他在这局里 = 队友；没有就不管（大厅里的其他玩家）。
            return EnemyData.healthPercentOf(mc.level.getScoreboard(), player) >= 0;
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
        if (!uiAllowed(mc)) return false;
        return computeVisibleCore(mc, entity);
    }

    private static boolean computeVisibleCore(Minecraft mc, Entity entity) {
        HudConfig.Cfg cfg = HudConfig.get();
        if (!managedCore(mc, entity)) return false;

        // 死亡 / 观战：CTT 里死掉的玩家就是**旁观模式**，所以一条规则同时管住两种状态。
        // 只挡「画」，不挡 managed —— managed 还负责摘掉原版 below_name 分数，
        // 旁观者那条 0% 同样不该冒出来。
        if (entity instanceof Player player && player.isSpectator()) return false;

        Vec3 camPos = cameraPos(mc);
        double distance = camPos.distanceTo(entity.position());
        double limit = entity instanceof Player ? cfg.teamMaxDistance : cfg.maxDistance;
        if (distance > limit) return false;

        return entity.shouldRender(camPos.x, camPos.y, camPos.z);
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

}
