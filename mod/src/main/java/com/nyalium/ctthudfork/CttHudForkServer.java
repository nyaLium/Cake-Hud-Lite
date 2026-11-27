package com.nyalium.ctthudfork;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 可选的服务端组件：按实体 UUID 推送 CTT 怪的血量 / 护甲 / 各类心数。
 *
 * <h2>为什么它是「选装」</h2>
 * <p>本 Mod 的定位始终是 <b>客户端必装、服务端选装</b>：</p>
 * <ul>
 *   <li>客户端没装 → 服务端这部分什么都不做（{@code canSend} 为假就跳过）；</li>
 *   <li>服务端没装 → 客户端照常工作，用「Boss 栏 + HealthPercent」兜底；</li>
 *   <li>两端都装 → 每只怪都有 {@code AllHearts/MaxHP/护甲/各类心}，同名怪也不会串数据。</li>
 * </ul>
 *
 * <h2>循环</h2>
 * <p>{@code END_SERVER_TICK} 钩子，每 {@value #INTERVAL_TICKS} tick（5 = 4 Hz）扫一次
 * 每名玩家周围 {@value #RADIUS} 格内的 CTT 实体，**玩家（含自己）优先、其余按距离**，
 * 合计取最近 {@link MobHealthPayload#MAX_ENTRIES} 条，拼一包发给该玩家。差量：与上次内容
 * 完全一致就跳过（静态场景 0 流量），但每 {@value #KEEPALIVE_ROUNDS} 轮至少重发一次，
 * 保证客户端的 5 s 失鲜窗口不会误判。</p>
 *
 * <h2>开销</h2>
 * <p>4 人队、每人视野 64 条实体：每 tick 4×64×13 次计分板读取（约 3.3k 次哈希查表）≈ 可忽略；
 * 稳态差量命中时 0 发包，战斗全变时每包约 2.6 KB（5 Hz → 约 13 KB/s）。</p>
 */
public final class CttHudForkServer implements ModInitializer {

    /** 推送间隔（tick）：5 tick = 4 Hz（与上游 Cake-Hud 一致）。 */
    private static final int INTERVAL_TICKS = 5;
    /** 扫描半径（格）：128（与上游一致；比原来的 64 更能覆盖 Boss 场地）。 */
    private static final double RADIUS = 128.0;
    /** 内容没变化时也强制重发的周期，需小于客户端 5 s 失鲜窗口。 */
    private static final long KEEPALIVE_MS = 3_000L;

    /** 抑制列表最多带几个 NPC（只用于「别画、把分数摘掉」，按距离取最近的）。 */
    private static final int MAX_NPC = 32;

    /** 数据包里的 objective 名（与数据包一致）。 */
    private static final String O_ALL = "AllHearts";
    /** AllHearts 缺失时退回 RedHearts（地图每 tick 先 reset AllHearts，只给 tag=CTTAll 重算）。 */
    private static final String O_RED = "RedHearts";
    private static final String O_MAX_HP = "MaxHP";
    private static final String O_SOUL = "SoulHearts";
    private static final String O_BLACK = "BlackHearts";
    private static final String O_BLUE = "BlueHearts";
    private static final String O_CRACKED = "CrackedHearts";
    private static final String O_MAX_SOUL = "MaxSoulHearts";
    private static final String O_MAX_BLACK = "MaxBlackHearts";
    private static final String O_MAX_BLUE = "MaxBlueHearts";
    private static final String O_DEFENCE = "Defence";

    /** 一次要发的内容：要画的条目 + 要抑制显示的 NPC。 */
    private record Plan(List<MobHealthPayload.Entry> entries, List<UUID> suppressed) {}

    /** 每名玩家上次实际发出去的内容 + 时间（差量判定）。 */
    private static final Map<UUID, Plan> LAST_SENT = new HashMap<>();
    /** 每个玩家上一次告知的「调试标签」状态（只在变化时发包）。 */
    private static final Map<UUID, Boolean> DEBUG_TAG_SENT = new HashMap<>();
    private static final Map<UUID, Long> LAST_SENT_MS = new HashMap<>();

    private static int tickCounter;

    @Override
    public void onInitialize() {
        // 通道注册必须两端都有，否则客户端收到未知 payload 会被丢掉。
        // 注意 Fabric API 6.x 的方法名：clientboundPlay() / serverboundPlay()
        // （旧版叫 playS2C() / playC2S()，这里按当前 API 来）。
        PayloadTypeRegistry.clientboundPlay().register(MobHealthPayload.TYPE, MobHealthPayload.CODEC);
        // 调试标签通道（实体 tag 是纯服务端数据，客户端读不到，只能由这里告知）。
        PayloadTypeRegistry.clientboundPlay().register(DebugTagPayload.TYPE, DebugTagPayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(CttHudForkServer::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        if (server == null) return;
        if (++tickCounter < INTERVAL_TICKS) return;
        tickCounter = 0;

        Scoreboard scoreboard = server.getScoreboard();
        long now = System.currentTimeMillis();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        for (ServerPlayer player : players) {
            if (player == null) continue;
            // 客户端没装本 Mod（或没声明接收）→ 血量包不发。
            boolean canSendHealth = ServerPlayNetworking.canSend(player, MobHealthPayload.TYPE);
            if (!canSendHealth) continue;
            Plan plan = collect(scoreboard, player);
            UUID id = player.getUUID();

            // 调试标签 —— 只在状态变化时发一次（一字节，随血量包同一个 4 Hz 循环）。
            if (ServerPlayNetworking.canSend(player, DebugTagPayload.TYPE)) {
                boolean hasTag = player.entityTags().contains(DebugTagPayload.DEBUG_TAG);
                Boolean was = DEBUG_TAG_SENT.get(id);
                if (was == null || was != hasTag) {
                    try {
                        ServerPlayNetworking.send(player, new DebugTagPayload(hasTag));
                        DEBUG_TAG_SENT.put(id, hasTag);
                    } catch (Throwable ignored) {
                        // 发包失败无副作用：下一轮还会重试。
                    }
                }
            }

            // 客户端没装本 Mod（或没声明接收）→ 血量包不发。
            Plan previous = LAST_SENT.get(id);
            boolean changed = previous == null || !previous.equals(plan);
            long lastMs = LAST_SENT_MS.getOrDefault(id, 0L);
            boolean keepaliveDue = now - lastMs >= KEEPALIVE_MS;
            if (!changed && !keepaliveDue) continue;

            LAST_SENT.put(id, plan);
            LAST_SENT_MS.put(id, now);
            try {
                ServerPlayNetworking.send(player, new MobHealthPayload(
                        MobHealthPayload.CURRENT_VERSION, plan.entries(), plan.suppressed()));
            } catch (Throwable ignored) {
                // 发包失败无副作用：下一轮还会重试。
            }
        }

        // 玩家离线后清掉记录，避免长期运行内存增长。
        LAST_SENT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        LAST_SENT_MS.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        DEBUG_TAG_SENT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
    }

    /** 取这名玩家周围的 CTT 实体：**玩家（含自己）优先，其余按距离**，最多 MAX_ENTRIES 条。 */
    private static Plan collect(Scoreboard scoreboard, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        // getEntities(player, box) **排除**了 source 实体本身，
        // 所以「自己」永远不会出现在 nearby 里 —— 必须先手动加进来，
        // 否则 showSelf 在双端也拿不到自己的 payload（只有百分比）。
        List<Entity> nearby = new ArrayList<>(player.level().getEntities(player, box));
        nearby.add(player);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        List<Entity> mobs = new ArrayList<>(nearby.size());
        List<Entity> npcs = new ArrayList<>();
        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity instanceof ArmorStand) continue;
            if (!entity.isAlive() || entity.isRemoved()) continue;
            // 玩家也推：队友那一行同样需要四色心 / 碎心 / 粉心（队伍条只有 AllHearts/MaxHP）。
            // **自己**也推一份 —— 调试模式（debug）会在自己头顶画条，
            // 装服务端时就能用上精确的四色心，而不是只有百分比。
            // 死亡 / 观战：CTT 里死掉的玩家就是旁观模式，客户端一律不画，推了也没人用。
            if (entity instanceof Player other && other.isSpectator()) continue;
            if (!isCttMob(scoreboard, entity)) continue;
            // NPC（tag=NPC，如大厅/塔里的对话角色）有 RedHearts/MaxHP/HealthPercent 分数，
            // 但它们是布景、不是敌人。客户端看不到 tag，所以放进「抑制列表」让客户端别画。
            // 同名分身 Boss（Psy：真身 tag=Psy / 分身 tag=PsyClone）也一并抑制 ——
            // 它们名字与外观完全一样，画血条等于告诉玩家哪个是真身。
            if (isHiddenIdentity(entity)) {
                npcs.add(entity);
                continue;
            }
            mobs.add(entity);
        }
        if (mobs.isEmpty() && npcs.isEmpty()) return new Plan(List.of(), List.of());

        // 玩家优先 —— 先排玩家（含自己，距离 0），再排怪；同组内按距离。
        // 以前纯按距离，怪多的时候远处的队友会被挤掉 → 那一格退回「当前/上限」。
        mobs.sort(Comparator
                .comparingInt((Entity e) -> e instanceof Player ? 0 : 1)
                .thenComparingDouble(e -> e.distanceToSqr(px, py, pz)));
        // Psy 的分身（tag=PsyClone）与真身（tag=Psy）同名同外观 —— 分身一律**复制真身的读数**，
        // 于是所有 Psy 头顶显示完全一样，玩家没法靠血条认出真身。
        Entity psy = null;
        for (Entity entity : nearby) {
            if (entity.entityTags().contains("Psy")) {
                psy = entity;
                break;
            }
        }
        int limit = Math.min(mobs.size(), MobHealthPayload.MAX_ENTRIES);
        List<MobHealthPayload.Entry> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            Entity entity = mobs.get(i);
            Entity data = entity.entityTags().contains("PsyClone") && psy != null ? psy : entity;
            out.add(entryOf(scoreboard, data, entity.getUUID()));
        }
        // NPC 只按距离取最近的若干条，避免大厅人多时把包撑大。
        npcs.sort(Comparator.comparingDouble(e -> e.distanceToSqr(px, py, pz)));
        int npcLimit = Math.min(npcs.size(), MAX_NPC);
        List<UUID> hidden = new ArrayList<>(npcLimit);
        for (int i = 0; i < npcLimit; i++) hidden.add(npcs.get(i).getUUID());
        return new Plan(List.copyOf(out), List.copyOf(hidden));
    }

    /** 不该出现在 HUD 里的实体（布景 NPC）。 */
    private static boolean isHiddenIdentity(Entity entity) {
        return entity.entityTags().contains("NPC");
    }

    private static MobHealthPayload.Entry entryOf(Scoreboard scoreboard, Entity entity, UUID uuid) {
        int[] armour = new int[EnemyData.ARMOR_OBJECTIVES.length];
        for (int i = 0; i < armour.length; i++) {
            armour[i] = score(scoreboard, EnemyData.ARMOR_OBJECTIVES[i], entity);
        }
        boolean targetted = entity.entityTags().contains("Targetting");
        // 结构化姓名（保留 translate 键与样式），客户端做 Psy 识别 / 悬浮显示都不再依赖实体名。
        net.minecraft.network.chat.Component nameText = entity.getCustomName() != null
                ? entity.getCustomName() : entity.getDisplayName();
        // 与客户端 EnemyData.read 同口径 —— AllHearts 读不到（地图先 reset、这一 tick 还没给
        // CTTAll 补上）时退回 RedHearts；否则存活怪会显示成「0/上限」、条也是空的。
        int all = score(scoreboard, O_ALL, entity);
        if (all <= 0) all = score(scoreboard, O_RED, entity);
        return new MobHealthPayload.Entry(
                uuid,
                nameText,
                all,
                score(scoreboard, O_MAX_HP, entity),
                score(scoreboard, O_SOUL, entity),
                score(scoreboard, O_BLACK, entity),
                score(scoreboard, O_BLUE, entity),
                score(scoreboard, O_CRACKED, entity),
                score(scoreboard, O_MAX_SOUL, entity),
                score(scoreboard, O_MAX_BLACK, entity),
                score(scoreboard, O_MAX_BLUE, entity),
                score(scoreboard, O_DEFENCE, entity),
                armour,
                targetted);
    }

    /** 判据与客户端一致：有 CTT 血量分数就算数据包管控的怪。 */
    private static boolean isCttMob(Scoreboard scoreboard, Entity entity) {
        return score(scoreboard, O_MAX_HP, entity) > 0
                || score(scoreboard, O_ALL, entity) > 0
                || score(scoreboard, "HealthPercent", entity) > 0;
    }

    private static int score(Scoreboard scoreboard, String objectiveName, ScoreHolder holder) {
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) return 0;
        ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(holder, objective);
        return info == null ? 0 : info.value();
    }
}
