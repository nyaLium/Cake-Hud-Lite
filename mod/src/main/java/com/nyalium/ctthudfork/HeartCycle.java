package com.nyalium.ctthudfork;

/**
 * 除红心之外「循环显示哪一类心」的时钟（纯函数，可在普通 JVM 里测）。
 *
 * <p>1.20.0 起心数行只留红心 + <b>一种</b>其它心，其余按时间轮换：</p>
 * <ul>
 *   <li><b>敌人</b>：魂 → 黑 → 蓝 → 碎 → 粉，只挑该实体真有的，每次一种；</li>
 *   <li><b>玩家</b>：只在魂 ↔ 黑之间轮换（碎心/粉心与蓝心固定在最上面一行，不参与）。</li>
 * </ul>
 *
 * <p>和护甲翻页一样，{@code salt}（实体 id）用来错开，避免满屏的条同时跳字。</p>
 */
public final class HeartCycle {

    /** 每类心停留时长（毫秒）。 */
    public static final long KIND_MS = 2_000L;

    /** 敌人除红心外的轮换顺序。 */
    public static final int[] ENEMY_ORDER = {
            HeartCounts.SOUL, HeartCounts.BLACK, HeartCounts.BLUE,
            HeartCounts.CRACKED, HeartCounts.PINK
    };

    /** 玩家：只在魂 / 黑之间轮换。 */
    public static final int[] PLAYER_ORDER = {
            HeartCounts.SOUL, HeartCounts.BLACK
    };

    private HeartCycle() {}

    public static int[] order(boolean player) {
        return player ? PLAYER_ORDER : ENEMY_ORDER;
    }

    /** 该实体有几种可轮换的非红心血类。 */
    public static int presentCount(HeartCounts hearts, boolean player) {
        if (hearts == null) return 0;
        int count = 0;
        for (int kind : order(player)) {
            if (hearts.has(kind)) count++;
        }
        return count;
    }

    /**
     * 当前该显示哪一类心。
     *
     * @param hearts 各类心（可为 null）
     * @param player true = 玩家（只轮换魂/黑），false = 敌人
     * @param nowMs  当前时间（{@code System.currentTimeMillis()}）
     * @param salt   错开用的整数（一般传实体 id）
     * @return 心类下标；没有任何可轮换的心时返回 {@code -1}
     */
    public static int pick(HeartCounts hearts, boolean player, long nowMs, int salt) {
        if (hearts == null) return -1;
        int count = presentCount(hearts, player);
        if (count == 0) return -1;
        long step = Math.floorDiv(Math.max(0L, nowMs), KIND_MS);
        int slot = (int) Math.floorMod(step + salt, (long) count);
        for (int kind : order(player)) {
            if (!hearts.has(kind)) continue;
            if (slot == 0) return kind;
            slot--;
        }
        return -1;
    }
}
