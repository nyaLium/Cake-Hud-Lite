package com.nyalium.cttenemyhud;

/**
 * 元素护甲的翻页时钟（纯函数，可在普通 JVM 里测）。
 *
 * <p>七种元素护甲一次全画会把整行拉得很长，所以护甲部分每页最多
 * {@link EnemyHudLayout#ARMOUR_PAGE_SIZE} 项、按时间循环翻页：5 种就是 2 + 2 + 1 三页。
 * {@code 🛡 防御} 不参与翻页（{@link EnemyHudLayout#armourPageCount(int[])} 不算它）。</p>
 *
 * <p>翻页是<b>全局同步</b>的（同一时刻所有怪翻到同一页），额外加一个 {@code salt}
 * （用实体 id）错开，避免满屏的条同时跳动。</p>
 */
public final class ArmourCycle {

    /** 每页停留时长（毫秒）。 */
    public static final long PAGE_MS = 2_000L;

    private ArmourCycle() {}

    /**
     * 当前应该显示第几页。
     *
     * @param armor 七档元素护甲（{@code 0} = 没有这一类）
     * @param nowMs 当前时间（{@code System.currentTimeMillis()}）
     * @param salt  错开用的整数（一般传实体 id），可以任意大小、可负
     * @return {@code [0, 页数)}；只有一页或没有护甲时恒为 0
     */
    public static int page(int[] armor, long nowMs, int salt) {
        int pages = EnemyHudLayout.armourPageCount(armor);
        if (pages <= 1) return 0;
        long step = Math.floorDiv(Math.max(0L, nowMs), PAGE_MS);
        return (int) Math.floorMod(step + salt, (long) pages);
    }
}
