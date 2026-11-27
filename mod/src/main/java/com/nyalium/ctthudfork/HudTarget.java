package com.nyalium.ctthudfork;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
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

    /**
     * Boss 的身份名（数据包里的 custom name 键）。
     *
     * <p>地图给 Psy 的皮肤/分身用的是「{@code Psy} + N 个 {@code U+E007}」这类键
     * （{@code U+E007} 是地图字体里的不可见字符），翻译后都显示「鸟叔」；
     * 真身是 {@code Psy}、地狱皮肤是 {@code Rockstar_Psy}。之前只匹配精确的 {@code "Psy"}，
     * 所以 1.5.6 的纯客户端判定全部失效 —— 日志已经证实客户端能读到 custom name。</p>
     */
    private static final java.util.Set<String> HIDDEN_NAMES =
            java.util.Set.of("Psy", "Psy_2XXX", "Rockstar_Psy", "Rockstar Psy");

    /** 去掉键名末尾的不可见字符（{@code U+E007}，地图用来区分皮肤）。 */
    static String stripNameKey(String key) {
        if (key == null) return null;
        int end = key.length();
        while (end > 0 && key.charAt(end - 1) == '\uE007') end--;
        return key.substring(0, end);
    }

    /**
     * 这个名字是不是「身份必须隐藏」的 Boss（纯函数，自检直接打）。
     *
     * <p><b>只看翻译键</b>，不看渲染出来的文字 —— 这样与语言无关：
     * 中文是「鸟叔」、英文是「Psy」、其它语言又是别的，按显示名判定会漏。</p>
     */
    static boolean hidesByName(Component name) {
        if (name == null) return false;
        if (!(name.getContents() instanceof TranslatableContents tc) || tc.getKey() == null) {
            return false;
        }
        return HIDDEN_NAMES.contains(stripNameKey(tc.getKey()));
    }

    /** 这只实体要不要「整组统一显示」（摘掉原版分数，并让整组共用一份快照）。 */
    static boolean hidesHud(Entity entity) {
        if (entity == null) return false;
        if (hidesByName(entity.getCustomName())) return true;
        // 服务端下发的结构化名字（translate 键原样）也能命中 —— 双端时更可靠。
        MobHealthPayload.Entry pushed = ClientMobHealthCache.lookup(entity.getUUID());
        return pushed != null && hidesByName(pushed.nameText());
    }

    private HudTarget() {}

    /** 本 Mod 接管的实体（有 CTT 数据 / 是队友），不考虑距离与渲染可见性。 */
    public static boolean managed(Minecraft mc, Entity entity) {
        if (!uiAllowed(mc)) return false;
        return managedCore(mc, entity);
    }

    /**
     * 「UI 允许显示」的总闸：未知诅咒（整个 UI）与癫狂（实体血条）都关它。
     *
     * <p><b>癫狂不在这里</b>：癫狂只做两件事 —— 隐藏自身属性面板（{@link SelfStatsPanel}）
     * 与不画「自己」那条血条（{@link HudWorldRenderer}）——<b>敌人与队友的头顶血条完全照常</b>，
     * 玩家在癫狂期间照样要能判断敌人血量。</p>
     *
     * <p>注意区分：{@code EnemyData.percentOnly} / {@code HudLayout} 的 percentOnly 分支是
     * 「数据只给了 {@code HealthPercent}」时的渲染方式，与癫狂无关。</p>
     */
    static boolean uiAllowed(Minecraft mc) {
        if (mc == null) return false;
        // 癫狂不再挡实体 HUD（只隐藏自身属性面板，见 SelfStatsPanel）。
        return !HudRenderGate.suppressAll(mc);
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
            if (player.isSpectator()) return false;   // 1.4.0：旁观者不画
            if (!cfg.showTeammates) return false;
            if (BossBarTracker.isTeammate(mc, player)) return true;
            // 队伍条里没有他时的回退：地图给本局参战玩家（tag CTTAll）都算了
            // below_name 的 HealthPercent（health.mcfunction:102-104），有这条分数
            // 就说明他在这局里 = 队友；没有就不管（大厅里的其他玩家）。
            return EnemyData.healthPercentOf(mc.level.getScoreboard(), player) >= 0;
        }
        // 服务端明确要求抑制的实体（NPC）也算「接管」——接管只为了把原版 below_name
        // 分数摘掉，渲染端会跳过它（见 HudWorldRenderer）。这样大厅 NPC 不再顶着血条/数字。
        if (ClientMobHealthCache.isSuppressed(entity.getUUID())) return true;
        // 同名分身 Boss（Psy 等）同样「接管但不画」，否则原版 below_name 的
        // 百分比也会把真身暴露出来。
        if (hidesHud(entity)) return true;
        // 有数据才算「数据包管控的敌人」——这一条也排除了掉落物、船、画等。
        if (EnemyData.hasData(mc.level.getScoreboard(), entity)) {
            MANAGED_RECENT.put(entity, System.currentTimeMillis());
            return true;
        }
        // 地图每 tick 会先 `scoreboard players reset @e AllHearts` 再逐只重算，
        // 客户端恰好在这两个包之间取样时会读到「没有分数」；以前这会让整组 HUD 闪一下。
        // 这里给 500 ms 的粘性：刚被接管过的实体，短暂读不到分数也继续算「接管」。
        Long at = MANAGED_RECENT.get(entity);
        if (at != null && System.currentTimeMillis() - at <= MANAGED_STICKY_MS) {
            return true;
        }
        return false;
    }

    /** 「这只实体最近被数据包接管过」的记忆（见 {@link #managedCore}）。 */
    private static final Map<Entity, Long> MANAGED_RECENT = new IdentityHashMap<>();
    private static final long MANAGED_STICKY_MS = 500L;

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

    // ── 1.6.0「血条不可透视」 ──

    /**
     * 这一帧到底要不要显示这只实体的头顶信息（文字行 + 血条 + 强制补上的原版名字）。
     *
     * <p>名字与血条必须用<b>同一个</b>判据，否则会出现「有名字没血条 / 有血条没名字」的不同步；
     * 本方法就是那个唯一入口：{@link #visible}（接管 / 距离 / 原版可见性）
     * 再加上「不可透视」开启时的方块遮挡判定。</p>
     */
    public static boolean showsHud(Minecraft mc, Entity entity) {
        if (!visible(mc, entity)) return false;
        return !(HudConfig.get().barNoSeeThrough && occludedByBlocks(mc, entity));
    }

    /** 遮挡判定的缓存（射线比较贵，一帧内名字与血条各问一次）。 */
    private static final Map<Entity, Boolean> OCCLUDED = new IdentityHashMap<>();
    private static long occludedStamp = 0L;

    /**
     * 相机到这只实体是不是被<b>不透明</b>方块挡住了。
     *
     * <p>采样点 = 身体中心 / 身高 85% 处 / 四个水平角，任意一条通视就算看得见。
     * 判定打在<b>碰撞箱</b>上而不是头顶那个悬空的 HUD 挂点 —— 挂点比头顶还高半格，
     * 拿它当判据的话头顶有天花板就会整组消失。</p>
     *
     * <p>判定结果带<b>防抖</b>（见 {@link #settle}）。实体刚好压在方块边缘时，
     * 相机每帧动一点点就会让射线在「通 / 挡」之间来回跳，直接用原始结果会让血条闪烁。</p>
     *
     * @return true = 被挡住（不该显示）
     */
    public static boolean occludedByBlocks(Minecraft mc, Entity entity) {
        if (mc == null || mc.level == null || mc.player == null || entity == null) return false;
        long now = System.nanoTime();
        if (now - occludedStamp > MEMO_NS) {
            OCCLUDED.clear();
            occludedStamp = now;
        }
        Boolean cached = OCCLUDED.get(entity);
        if (cached != null) return cached;
        boolean raw;
        try {
            raw = computeOccluded(mc, entity);
        } catch (Throwable t) {
            raw = false;
        }
        // 缓存的是「防抖之后」的结论：同一帧里名字与血条拿到的一定是同一个结果。
        boolean result = settle(entity, raw);
        OCCLUDED.put(entity, result);
        return result;
    }

    /** 防抖窗口：实体在视线边缘抖动时，短时间的「被挡」不会真的把 UI 关掉。 */
    static final long OCCLUSION_SETTLE_MS = 200L;

    /** 每只实体的防抖状态。 */
    private static final class OcclusionState {
        /** 当前对外结论：true = 隐藏。 */
        boolean blocked;
        /** 本次「开始被挡」的时刻（ms）；0 = 没在计时。 */
        long blockedSince;
    }

    private static final Map<Entity, OcclusionState> SETTLE = new IdentityHashMap<>();
    private static long settlePruneAt = 0L;

    /**
     * 遮挡防抖：修「实体快要被不透明方块遮住时血条闪烁」。
     *
     * <p>根因：实体位置是 20 Hz 的整数 tick，相机是每帧连续的，再加上射线只取有限几个采样点，
     * 于是在「刚好压在方块边缘」的视角下，射线会一帧通、一帧挡地跳。直接按原始结果开关 UI，
     * 看起来就是血条一闪一闪。</p>
     *
     * <p>规则：</p>
     * <ul>
     *   <li><b>变清晰 → 立刻显示</b>（否则会有「人都露出来了血条还没回来」的迟滞感）；</li>
     *   <li><b>变遮挡 → 先等 {@link #OCCLUSION_SETTLE_MS} ms</b>，期间保持显示；
     *       只有连续被挡超过这个窗口才真的隐藏。抖动周期 ≤ 120 ms 时永远隐藏不了，
     *       闪烁就消失了。</li>
     * </ul>
     */
    private static boolean settle(Entity entity, boolean raw) {
        long now = System.currentTimeMillis();
        if (now - settlePruneAt > 5_000L) {
            settlePruneAt = now;
            SETTLE.keySet().removeIf(e -> e == null || !e.isAlive());
        }
        OcclusionState state = SETTLE.get(entity);
        if (state == null) {
            state = new OcclusionState();
            SETTLE.put(entity, state);
        }
        if (raw == state.blocked) {
            state.blockedSince = 0L;      // 结论没变，清掉计时
            return state.blocked;
        }
        if (!raw) {
            state.blocked = false;        // 重新看见 → 立刻显示
            state.blockedSince = 0L;
            return false;
        }
        if (state.blockedSince == 0L) state.blockedSince = now;
        if (now - state.blockedSince >= OCCLUSION_SETTLE_MS) {
            state.blocked = true;
            state.blockedSince = 0L;
            return true;
        }
        return state.blocked;             // 宽限期内维持原结论
    }

    private static boolean computeOccluded(Minecraft mc, Entity entity) {
        Vec3 camPos = cameraPos(mc);
        // 用**插值后**的碰撞箱（和画面上看到的位置一致），而不是 20 Hz 的整数 tick 位置。
        // 否则实体每 tick 才跳一次、相机每帧都在动，射线会在方块边缘出现「整块 20 Hz 抖动」。
        AABB box = entity.getBoundingBox();
        float partial = 0.0f;
        try {
            partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        } catch (Throwable ignored) {
            // 拿不到 partial tick 就用当前 tick 位置，只是精度差一点。
        }
        Vec3 interp = entity.getPosition(partial);
        Vec3 cur = entity.position();
        double dxs = interp.x - cur.x;
        double dys = interp.y - cur.y;
        double dzs = interp.z - cur.z;
        if (dxs * dxs + dys * dys + dzs * dzs > 1.0e-12) {
            box = box.move(dxs, dys, dzs);
        }
        double midY = (box.minY + box.maxY) * 0.5;
        double headY = box.maxY - (box.maxY - box.minY) * 0.15;
        double cx = (box.minX + box.maxX) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double halfW = Math.max(0.2, (box.maxX - box.minX) * 0.5);
        double halfD = Math.max(0.2, (box.maxZ - box.minZ) * 0.5);
        if (clear(mc, camPos, new Vec3(cx, midY, cz))) return false;
        if (clear(mc, camPos, new Vec3(cx, headY, cz))) return false;
        for (int i = 0; i < 4; i++) {
            double dx = (i & 1) == 0 ? -halfW : halfW;
            double dz = (i & 2) == 0 ? -halfD : halfD;
            if (clear(mc, camPos, new Vec3(cx + dx, midY, cz + dz))) return false;
        }
        return true;
    }

    /**
     * 一条视线最多允许穿透几层「半透明方块」，超过就按不透明处理
     * （一层玻璃看得过去，两层叠起来就看不透了 —— 用户口径：「有一定方块深度就不算半透明」）。
     */
    static final int MAX_SEE_THROUGH_DEPTH = 1;

    /** 相机 → 目标点这条视线有没有被不透明方块挡住（半透明方块会穿过去继续打）。 */
    private static boolean clear(Minecraft mc, Vec3 from, Vec3 to) {
        try {
            Vec3 start = from;
            Vec3 dir = to.subtract(from);
            double length = dir.length();
            if (length < 1.0e-4) return true;
            Vec3 unit = dir.scale(1.0 / length);
            int depth = 0;
            for (int i = 0; i < 8; i++) {
                HitResult hit = mc.level.clip(new ClipContext(start, to, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, mc.player));
                if (!(hit instanceof BlockHitResult blockHit)) return true;   // 没打到方块 = 通畅
                if (!seeThroughBlock(mc.level.getBlockState(blockHit.getBlockPos()))) return false;
                // 半透明也有限度：叠得太厚就当作一堵不透明的墙。
                if (++depth > MAX_SEE_THROUGH_DEPTH) return false;
                Vec3 next = blockHit.getLocation().add(unit.scale(0.05));
                if (next.distanceTo(to) <= 0.05 || next.subtract(start).length() < 1.0e-3) return true;
                start = next;
            }
            return true;
        } catch (Throwable t) {
            return true;   // 异常按「通畅」处理，宁可多画一帧
        }
    }

    /**
     * 这个方块能不能「看过去」（1.6.0 口径，用户第二次修订）。
     *
     * <table>
     *   <tr><th>类别</th><th>例子</th><th>是否挡</th></tr>
     *   <tr><td>透明建材</td><td>玻璃、染色玻璃、玻璃板、染色玻璃板（id 含 {@code glass}）</td><td>不挡</td></tr>
     *   <tr><td>植物</td><td>草、花、树苗、蘑菇…（{@code BushBlock}）</td><td>不挡</td></tr>
     *   <tr><td>树叶</td><td>橡木树叶…（{@code LeavesBlock}，<b>不算植物</b>）</td><td>挡</td></tr>
     *   <tr><td>无碰撞箱的功能方块</td><td>火把、按钮、拉杆、红石线…</td><td>不挡（射线用 COLLIDER 打不到，永远算透明）</td></tr>
     *   <tr><td>流体</td><td>水、岩浆</td><td>不挡（射线用 {@code Fluid.NONE}）</td></tr>
     *   <tr><td>冰类 / 铜格栅</td><td>冰、蓝冰、霜冰、铜格栅</td><td><b>挡</b>（按用户要求排除）</td></tr>
     *   <tr><td>其余一切</td><td>台阶、楼梯、栅栏、门、活板门、箱子、石头…</td><td>挡</td></tr>
     * </table>
     *
     * <p>另外穿透层数受 {@link #MAX_SEE_THROUGH_DEPTH} 限制 —— 一层玻璃算透明，两层就算墙。</p>
     */
    static boolean seeThroughBlock(BlockState state) {
        if (state == null || state.isAir()) return true;
        Block block = state.getBlock();
        // 植物（草 / 花 / 树苗 / 蘑菇…）：树叶是 LeavesBlock，刻意不在此列 —— 树叶要挡。
        if (block instanceof BushBlock) return true;
        // 只认玻璃系（玻璃 / 染色玻璃 / 玻璃板）；冰类、铜格栅按用户要求不再算透明。
        return BuiltInRegistries.BLOCK.getKey(block).getPath().contains("glass");
    }

}
