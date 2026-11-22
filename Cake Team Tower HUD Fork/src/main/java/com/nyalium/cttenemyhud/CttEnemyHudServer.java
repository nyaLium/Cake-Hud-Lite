package com.nyalium.cttenemyhud;

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
 * <p>{@code END_SERVER_TICK} 钩子，每 {@value #INTERVAL_TICKS} tick（默认 4 = 5 Hz）扫一次
 * 每名玩家周围 {@value #RADIUS} 格内的怪，按距离取最近 {@link MobHealthPayload#MAX_ENTRIES} 只，
 * 拼一包发给该玩家。差量：与上次内容完全一致就跳过（静态场景 0 流量），
 * 但每 {@value #KEEPALIVE_MS} ms 至少重发一次，保证客户端的 5 s 失鲜窗口不会误判。</p>
 *
 * <h2>开销</h2>
 * <p>4 人队、每人视野 32 只怪：每 tick 4×32×13 次计分板读取（约 1.7k 次哈希查表）≈ 可忽略；
 * 稳态差量命中时 0 发包，战斗中有变化时每包约 1.3 KB。</p>
 */
public final class CttEnemyHudServer implements ModInitializer {

    /** 推送间隔（tick）：4 tick = 5 Hz。 */
    private static final int INTERVAL_TICKS = 4;
    /** 扫描半径（格）。 */
    private static final double RADIUS = 64.0;
    /** 内容没变化时也强制重发的周期，需小于客户端 5 s 失鲜窗口。 */
    private static final long KEEPALIVE_MS = 3_000L;

    /** 数据包里的 objective 名（与数据包一致）。 */
    private static final String O_ALL = "AllHearts";
    private static final String O_MAX_HP = "MaxHP";
    private static final String O_SOUL = "SoulHearts";
    private static final String O_BLACK = "BlackHearts";
    private static final String O_BLUE = "BlueHearts";
    private static final String O_CRACKED = "CrackedHearts";
    private static final String O_MAX_SOUL = "MaxSoulHearts";
    private static final String O_MAX_BLACK = "MaxBlackHearts";
    private static final String O_MAX_BLUE = "MaxBlueHearts";
    private static final String O_DEFENCE = "Defence";

    /** 每名玩家上次实际发出去的内容 + 时间（差量判定）。 */
    private static final Map<UUID, List<MobHealthPayload.Entry>> LAST_SENT = new HashMap<>();
    private static final Map<UUID, Long> LAST_SENT_MS = new HashMap<>();

    private static int tickCounter;

    @Override
    public void onInitialize() {
        // 通道注册必须两端都有，否则客户端收到未知 payload 会被丢掉。
        // 注意 Fabric API 6.x 的方法名：clientboundPlay() / serverboundPlay()
        // （旧版叫 playS2C() / playC2S()，这里按当前 API 来）。
        PayloadTypeRegistry.clientboundPlay().register(MobHealthPayload.TYPE, MobHealthPayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(CttEnemyHudServer::onEndServerTick);
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
            // 客户端没装本 Mod（或没声明接收）→ 不发。
            if (!ServerPlayNetworking.canSend(player, MobHealthPayload.TYPE)) continue;

            List<MobHealthPayload.Entry> entries = collect(scoreboard, player);
            UUID id = player.getUUID();
            List<MobHealthPayload.Entry> previous = LAST_SENT.get(id);
            boolean changed = previous == null || !previous.equals(entries);
            long lastMs = LAST_SENT_MS.getOrDefault(id, 0L);
            boolean keepaliveDue = now - lastMs >= KEEPALIVE_MS;
            if (!changed && !keepaliveDue) continue;

            LAST_SENT.put(id, entries);
            LAST_SENT_MS.put(id, now);
            try {
                ServerPlayNetworking.send(player, new MobHealthPayload(
                        MobHealthPayload.CURRENT_VERSION, entries));
            } catch (Throwable ignored) {
                // 发包失败无副作用：下一轮还会重试。
            }
        }

        // 玩家离线后清掉记录，避免长期运行内存增长。
        LAST_SENT.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        LAST_SENT_MS.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
    }

    /** 取这名玩家周围最近的 CTT 怪，外加附近的其他玩家（队友）。 */
    private static List<MobHealthPayload.Entry> collect(Scoreboard scoreboard, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<Entity> nearby = player.level().getEntities(player, box);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        List<Entity> mobs = new ArrayList<>(nearby.size());
        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity instanceof ArmorStand) continue;
            if (!entity.isAlive() || entity.isRemoved()) continue;
            // 玩家也推：队友那一行同样需要四色心 / 碎心 / 粉心（队伍条只有 AllHearts/MaxHP）。
            // 自己不用推（本端不画自己的条）。
            if (entity == player) continue;
            if (!isCttMob(scoreboard, entity)) continue;
            mobs.add(entity);
        }
        if (mobs.isEmpty()) return List.of();

        mobs.sort(Comparator.comparingDouble(e -> e.distanceToSqr(px, py, pz)));
        int limit = Math.min(mobs.size(), MobHealthPayload.MAX_ENTRIES);
        List<MobHealthPayload.Entry> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            Entity entity = mobs.get(i);
            out.add(entryOf(scoreboard, entity));
        }
        return List.copyOf(out);
    }

    private static MobHealthPayload.Entry entryOf(Scoreboard scoreboard, Entity entity) {
        int[] armour = new int[EnemyData.ARMOR_OBJECTIVES.length];
        for (int i = 0; i < armour.length; i++) {
            armour[i] = score(scoreboard, EnemyData.ARMOR_OBJECTIVES[i], entity);
        }
        boolean targetted = entity.entityTags().contains("Targetting");
        return new MobHealthPayload.Entry(
                entity.getUUID(),
                score(scoreboard, O_ALL, entity),
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
