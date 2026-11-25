package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端：按实体 UUID 缓存服务端推来的怪物权威数据（可选组件）。
 *
 * <p>与 Cake-Hud 的 {@code ClientMobHealthCache} 同一套思路：</p>
 * <ul>
 *   <li>收到 {@link MobHealthPayload} 就整表替换（同一份包对同一台服务器是一致的）；</li>
 *   <li>{@link #isFresh()}：最近一次推送在 {@value #FRESH_WINDOW_MS} ms 内才算新鲜，
 *       超过就当服务端没装 / 掉线，渲染端立刻回落到「Boss 栏 + HealthPercent」路径；</li>
 *   <li>{@link #lookup(UUID)}：按实体 UUID 取，天然解决同名怪的唯一性问题。</li>
 * </ul>
 *
 * <p>断线 / 换服时 {@link #reset()}，避免看到上一局的残留数据。</p>
 */
final class ClientMobHealthCache {

    /** 服务端默认 5 Hz 推送，失鲜窗口给 5 s 余量（与 Cake-Hud 一致）。 */
    private static final long FRESH_WINDOW_MS = 5_000L;

    private static volatile Map<UUID, MobHealthPayload.Entry> snapshot = Collections.emptyMap();
    private static volatile long lastPushMs = 0L;

    private ClientMobHealthCache() {}

    /** 收到一包。 */
    static void accept(MobHealthPayload payload) {
        if (payload == null) return;
        // 协议版本比本端新：整包忽略，宁可不用这包也不猜字段。
        if (payload.version() > MobHealthPayload.CURRENT_VERSION) return;
        // 1.4.5 调试功能：只保留前 N 条（服务端已按「玩家优先、其余按距离」排序）。
        List<MobHealthPayload.Entry> entries = cap(payload.entries(), HudConfig.get().payloadLimit);
        Map<UUID, MobHealthPayload.Entry> map = new HashMap<>(Math.max(8, entries.size() * 2));
        for (MobHealthPayload.Entry entry : entries) {
            if (entry == null || entry.uuid() == null) continue;
            map.put(entry.uuid(), entry);
        }
        snapshot = Collections.unmodifiableMap(map);
        lastPushMs = System.currentTimeMillis();
    }

    /**
     * 截取前 {@code limit} 条（保持服务端给的顺序）。
     *
     * <p>服务端排序是「玩家（含自己）优先，其余按距离」，所以截断等价于
     * 「服务端只推了这些」—— 调试功能「筛选实体数量」就是靠它复现更小的上限。</p>
     */
    static List<MobHealthPayload.Entry> cap(List<MobHealthPayload.Entry> entries, int limit) {
        if (entries == null || entries.isEmpty()) return List.of();
        int n = Math.min(entries.size(), clampLimit(limit));
        return n >= entries.size() ? entries : entries.subList(0, n);
    }

    /** 条数上限夹到 {@code [1, MAX_ENTRIES]}（0 / 负数 / 超大值都安全）。 */
    static int clampLimit(int limit) {
        return Math.max(1, Math.min(MobHealthPayload.MAX_ENTRIES, limit));
    }

    /** 服务端数据是否还在新鲜窗口内。 */
    static boolean isFresh() {
        long last = lastPushMs;
        return last != 0L && (System.currentTimeMillis() - last) < FRESH_WINDOW_MS;
    }

    /** 按实体 UUID 取；失鲜或没有该条时返回 null（渲染端走回落路径）。 */
    static MobHealthPayload.Entry lookup(UUID entityId) {
        if (entityId == null || !isFresh()) return null;
        return snapshot.get(entityId);
    }

    static void reset() {
        snapshot = Collections.emptyMap();
        lastPushMs = 0L;
    }

    /** 供调试输出。 */
    static int size() {
        return snapshot.size();
    }

    /** 客户端注册接收器（由入口调用）。 */
    static void register() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                MobHealthPayload.TYPE,
                (payload, context) -> {
                    Minecraft client = context.client();
                    if (client == null) {
                        accept(payload);
                    } else {
                        client.execute(() -> accept(payload));
                    }
                });
    }
}
