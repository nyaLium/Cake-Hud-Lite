package com.nyalium.ctthudfork;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 护甲段缓存。
 *
 * <p>数据包的护甲是<b>轮换</b>的：{@code bossbars_universal.mcfunction} 每 40 tick 让
 * {@code ArmorDisplay} 进一档，同一时刻 bossbar 后缀里只带一种元素护甲。
 * 所以必须按实体累积——每帧见到几段就记几段，超过
 * {@value #SLOT_TTL_MS} ms 没再出现的段才作废（≈16 s 一整圈轮换 + 2 s 余量）。
 * 直接每帧现解析现画的话，除了当前那一档以外全部会闪断。</p>
 */
final class ArmourCache {

    /** 0 = Defence（蓝🛡），1..7 = 元素护甲，与 {@link EnemyData#ARMOR_OBJECTIVES} 对应。 */
    static final int SLOTS = 8;
    /** 一整个轮换周期 ≈ 16 s，TTL 取 18 s 留余量。 */
    private static final long SLOT_TTL_MS = 18_000L;
    /** 条目 30 s 没被任何帧 touch 就整体清掉。 */
    private static final long ENTRY_TTL_MS = 30_000L;

    private static final class Entry {
        final int[] values = new int[SLOTS];
        final long[] seenMs = new long[SLOTS];
        long lastTouchMs;
    }

    private static final Map<UUID, Entry> MAP = new ConcurrentHashMap<>();

    private ArmourCache() {}

    /** 记录一帧解析到的护甲段：{@code slotValues[slot] != 0} 表示本帧见到了该段。 */
    static void observe(UUID id, int[] slotValues) {
        if (id == null || slotValues == null) return;
        Entry entry = MAP.computeIfAbsent(id, k -> new Entry());
        long now = System.currentTimeMillis();
        entry.lastTouchMs = now;
        for (int i = 0; i < SLOTS; i++) {
            if (slotValues[i] != 0) {
                entry.values[i] = slotValues[i];
                entry.seenMs[i] = now;
            }
        }
        purge(now);
    }

    /** 当前仍有效的 8 段（过期或没见过的为 0）。 */
    static int[] snapshot(UUID id) {
        int[] out = new int[SLOTS];
        if (id == null) return out;
        Entry entry = MAP.get(id);
        if (entry == null) return out;
        long now = System.currentTimeMillis();
        for (int i = 0; i < SLOTS; i++) {
            if (now - entry.seenMs[i] <= SLOT_TTL_MS) {
                out[i] = entry.values[i];
            }
        }
        return out;
    }

    static void reset() {
        MAP.clear();
    }

    private static void purge(long now) {
        if (now % 5000 > 1) return; // 大约每 5 s 清一次，避免每帧遍历
        MAP.entrySet().removeIf(e -> now - e.getValue().lastTouchMs > ENTRY_TTL_MS);
    }
}
