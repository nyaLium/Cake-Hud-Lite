package com.nyalium.ctthudfork;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

/**
 * 从<b>客户端可见的计分板</b>里读出 CTT 敌人的真实数值。
 *
 * <h2>为什么能读到</h2>
 * <p>原版服务端只把「挂在某个展示槽（sidebar / below_name / list / sidebar.team.*）上的
 * objective」的分数同步给客户端 —— {@code ServerScoreboard.setDisplayObjective} 里
 * 只有 objective 从未被追踪过时才调用 {@code startTrackingObjective}，而
 * {@code PlayerList.updateEntireScoreboard} 只遍历 {@code DisplaySlot.values()}。
 * 数据包 {@code gamestart.mcfunction} 只挂了 {@code sidebar Lives}、
 * {@code below_name HealthPercent}、{@code list Coins}，所以：</p>
 * <ul>
 *   <li>{@code HealthPercent}（0~100）—— 每只怪都有一条，<b>纯客户端一定能读</b>；</li>
 *   <li>{@code AllHearts} / {@code MaxHP} / {@code Defence} / {@code FireArmor}… ——
 *       没上展示槽，默认读不到。</li>
 * </ul>
 *
 * <p>因此本类按「有绝对值就用绝对值，没有就退回百分比」自动降级。
 * 只要服务端把那几个 objective 挂到任意一个没被占用的展示槽上（见随包的
 * {@code ctt_hud_fork_expose.mcfunction}），绝对值通道就立刻打通，本类无需改动。</p>
 *
 * <p>血量的数据包口径（与 {@code targetting_enemy.mcfunction} 的 bossbar 标题一致）：
 * 左 = {@code AllHearts}（全部心的总量，缺失时退回 {@code RedHearts}），
 * 右 = {@code MaxHP}。</p>
 */
public final class EnemyData {

    /** 元素护甲 objective / 图标 / 颜色，顺序与数据包 ArmorDisplay 1..7 一致。 */
    public static final String[] ARMOR_OBJECTIVES = {
            "FireArmor", "IceArmor", "DarkArmor", "LightArmor", "TrueArmor", "WaterArmor", "ElectricArmor"
    };
    /** 元素护甲字形与计分板 objective 的顺序（索引 0..6 对应火/真火/水/电/冰/暗/光）。 */
    public static final String[] ARMOR_ICONS = {
            "\uD83D\uDD25", "\u2603", "\u2620", "\u2600", "\uD83D\uDEE1", "\u2693", "\u26A1"
    };
    /** gold / aqua / dark_purple / white / white / aqua / yellow（与原版 ChatFormatting 同值）。 */
    public static final int[] ARMOR_COLORS = {
            0xFFAA00, 0x55FFFF, 0xAA00AA, 0xFFFFFF, 0xFFFFFF, 0x55FFFF, 0xFFFF55
    };
    /** 蓝 🛡 Defence 的颜色。 */
    public static final int DEFENCE_COLOR = 0x5555FF;
    /** 真盔甲在 {@link #ARMOR_OBJECTIVES} / {@link #ARMOR_ICONS} 里的下标（1.2.2：不参与翻页）。 */
    public static final int TRUE_ARMOR_INDEX = 4;

    private EnemyData() {}

    /** 单只敌人一帧的读数。 */
    public static final class Snapshot {
        /** {@code AllHearts}（全部心的总量）。 */
        public final int hp;
        /** {@code MaxHP}（红心上限）。 */
        public final int maxHp;
        /** true = 只有百分比（读到的是 HealthPercent），画 "73%" 而不是 "73/120"。 */
        public final boolean percentOnly;
        public final int percent;
        /** 护甲（{@code 🛡 防御}，数据包 {@code Defence}；0 = 没有）。 */
    public final int defence;
        /** 7 档元素护甲，0 = 没有。 */
        public final int[] armor;
        /** 各类心的数量；{@link HeartCounts#unknown()} = 拆不出来，界面退回「当前/最大」。 */
        public final HeartCounts hearts;
        /** 血条填充比例 0..1。 */
        public final float ratio;
        /**
         * 当前血条表现的心类（决定纹理配色）；null = 红条。
         * 由 Boss 栏认领或服务端组件填，见 {@link HudWorldRenderer}。
         */
        public HeartCounts.Kind kind;

        Snapshot(int hp, int maxHp, boolean percentOnly, int percent, int defence, int[] armor,
                 HeartCounts hearts, float ratio) {
            this.hp = hp;
            this.maxHp = maxHp;
            this.percentOnly = percentOnly;
            this.percent = percent;
            this.defence = defence;
            this.armor = armor;
            this.hearts = hearts != null ? hearts : HeartCounts.unknown();
            this.ratio = Math.max(0.0f, Math.min(1.0f, Float.isNaN(ratio) ? 0.0f : ratio));
        }

        /** 有没有要画的护甲段（{@code 🛡 防御} 或任一元素护甲非 0）。 */
    public boolean hasArmor() {
            if (defence != 0) return true;
            for (int v : armor) {
                if (v != 0) return true;
            }
            return false;
        }
    }

    /**
     * 用外部来源（客户端 Boss 栏）得到的绝对值构造读数。
     *
     * @param armor  七档元素护甲，长度不足按 0 处理
     * @param hearts 各类心的数量，null = 不知道
     * @param ratio  血条填充比例（数据包按「当前这一类心」设的 value/max）
     */
    public static Snapshot exact(int hp, int maxHp, float ratio, int defence, int[] armor, HeartCounts hearts) {
        int[] fixed = new int[ARMOR_OBJECTIVES.length];
        if (armor != null) {
            System.arraycopy(armor, 0, fixed, 0, Math.min(armor.length, fixed.length));
        }
        int safeMax = Math.max(1, maxHp);
        // 1.2.1：**不再把 hp 夹到 maxHp**。数据包的队伍条写的是 AllHearts/MaxHP，
        // 蓝心 / 魂心可以突破红心上限（AllHearts > MaxHP），夹了就会显示成「红心上限/上限」，
        // 与 Boss 栏对不上。只把百分比夹到 0~100，血量本身原样保留。
        int safeHp = Math.max(0, hp);
        HeartCounts h = hearts != null ? hearts : HeartCounts.forMob(safeHp, safeMax);
        return new Snapshot(safeHp, safeMax, false, pct(safeHp, safeMax), defence, fixed, h, ratio);
    }

    /**
     * 用服务端组件（{@link MobHealthPayload}）推送的权威数据构造读数。
     *
     * <p><b>纯函数</b>，只吃整数，便于自检穷举。红心由
     * {@code AllHearts - 魂 - 黑 - 蓝} 反算（数据包 {@code health.mcfunction:25-28}）。</p>
     *
     * <p>血条填充比例复现数据包 Boss 栏的「按当前心类分层」（{@code HeartValue} 的优先级
     * 蓝 &gt; 黑 &gt; 魂 &gt; 红）：当前这一类的剩余量 ÷ 它的上限。
     * 四类都没有时按红心算 {@code min(红, MaxHP) / MaxHP}。</p>
     */
    public static Snapshot fromPayload(int hp, int maxHp, int soul, int black, int blue, int cracked,
                                       int maxSoul, int maxBlack, int maxBlue,
                                       int defence, int[] armour) {
        return fromPayload(hp, maxHp, soul, black, blue, cracked, maxSoul, maxBlack, maxBlue,
                defence, armour, false);
    }

    /**
     * 玩家（队友）读数：<b>进度固定为 红心 ÷ MaxHP</b>，不随当前心类分层。
     *
     * <p>队友头顶那条是<b>绿条</b>（一格颜色固定），数据包 bossbar 那种「按当前心类换色 +
     * 重新从满格开始」的分层进度放在绿条上会读不出来，所以玩家一律按红心的剩余比例填。
     * 心数那一行照旧输出全部颜色（{@link #fromPayload} 同一套）。</p>
     */
    public static Snapshot fromPayloadPlayer(int hp, int maxHp, int soul, int black, int blue, int cracked,
                                             int maxSoul, int maxBlack, int maxBlue,
                                             int defence, int[] armour) {
        return fromPayload(hp, maxHp, soul, black, blue, cracked, maxSoul, maxBlack, maxBlue,
                defence, armour, true);
    }

    private static Snapshot fromPayload(int hp, int maxHp, int soul, int black, int blue, int cracked,
                                        int maxSoul, int maxBlack, int maxBlue,
                                        int defence, int[] armour, boolean redProgress) {
        int all = Math.max(0, hp);
        int cap = Math.max(0, maxHp);
        int soulC = Math.max(0, soul);
        int blackC = Math.max(0, black);
        int blueC = Math.max(0, blue);
        int red = Math.max(0, all - soulC - blackC - blueC);
        int redClamped = cap > 0 ? Math.min(red, cap) : red;

        HeartCounts hearts = HeartCounts.of(red, soulC, blackC, blueC, cracked);

        float ratio;
        HeartCounts.Kind kind;
        if (redProgress) {
            ratio = cap > 0 ? redClamped / (float) cap : 0.0f;
            kind = HeartCounts.Kind.RED;
        } else if (blueC > 0 && maxBlue > 0) {
            ratio = blueC / (float) maxBlue;
            kind = HeartCounts.Kind.BLUE;
        } else if (blackC > 0 && maxBlack > 0) {
            ratio = blackC / (float) maxBlack;
            kind = HeartCounts.Kind.BLACK;
        } else if (soulC > 0 && maxSoul > 0) {
            ratio = soulC / (float) maxSoul;
            kind = HeartCounts.Kind.SOUL;
        } else {
            ratio = cap > 0 ? redClamped / (float) cap : 0.0f;
            kind = HeartCounts.Kind.RED;
        }

        int[] fixed = new int[ARMOR_OBJECTIVES.length];
        if (armour != null) {
            System.arraycopy(armour, 0, fixed, 0, Math.min(armour.length, fixed.length));
        }
        int safeMax = Math.max(1, cap);
        Snapshot snapshot = new Snapshot(all, safeMax, false, pct(all, safeMax), defence, fixed,
                hearts, ratio);
        snapshot.kind = kind;
        return snapshot;
    }

    /**
     * 读一只敌人；完全没有任何数据（未受数据包管控的实体）返回 null。
     */
    public static Snapshot read(Scoreboard sb, Entity entity) {
        if (sb == null || entity == null) return null;

        int allHearts = score(sb, "AllHearts", entity);
        int current = allHearts > 0 ? allHearts : score(sb, "RedHearts", entity);
        int maxHp = score(sb, "MaxHP", entity);

        int defence = score(sb, "Defence", entity);
        int[] armor = new int[ARMOR_OBJECTIVES.length];
        for (int i = 0; i < ARMOR_OBJECTIVES.length; i++) {
            armor[i] = score(sb, ARMOR_OBJECTIVES[i], entity);
        }

        if (current > 0 || maxHp > 0) {
            if (maxHp <= 0) maxHp = Math.max(1, current);
            if (current < 0) current = 0;
            return new Snapshot(current, maxHp, false, pct(current, maxHp), defence, armor,
                    HeartCounts.forMob(current, maxHp), pct(current, maxHp) / 100.0f);
        }

        // 绝对值通道没打通：退回 below_name 的百分比（每只怪都有）。
        int p = score(sb, "HealthPercent", entity);
        if (p > 0) {
            p = Math.min(100, p);
            return new Snapshot(p, 100, true, p, defence, armor,
                    HeartCounts.unknown(), p / 100.0f);
        }
        return null;
    }

    /**
     * 只有 {@code below_name} 百分比时的读数（1.2.6，癫狂期间用）。
     *
     * <p>癫狂不会清掉 {@code below_name} 的 {@code HealthPercent}（地图只在未知诅咒里
     * {@code setdisplay below_name} 清过一次），所以此时还能按百分比画一条条、不写数字。</p>
     *
     * @param percent 0~100；小于 0 由调用方过滤
     */
    public static Snapshot percent(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        return new Snapshot(p, 100, true, p, 0, new int[ARMOR_OBJECTIVES.length],
                HeartCounts.unknown(), p / 100.0f);
    }

    /**
     * 只判断「这只实体受数据包管控」，不构造读数 —— 供每帧每实体的判定用，零分配。
     *
     * <p>判据与 {@link #read} 完全一致：{@code AllHearts / RedHearts / MaxHP / HealthPercent}
     * 里任意一项有分，就说明数据包在管它。</p>
     */
    public static boolean hasData(Scoreboard sb, Entity entity) {
        if (sb == null || entity == null) return false;
        return score(sb, "AllHearts", entity) > 0
                || score(sb, "RedHearts", entity) > 0
                || score(sb, "MaxHP", entity) > 0
                || score(sb, "HealthPercent", entity) > 0;
    }

    /**
     * 读一只实体的 {@code below_name} {@code HealthPercent}（0~100）；没有分数返回 -1。
     *
     * <p>用途：把「最近敌人」那条 bossbar 认领到正确的实体上 —— 同名怪物有好几只时，
     * 只有真正被锁定的那只，它的 {@code HealthPercent} 才会和 bar 里的
     * {@code min(AllHearts, MaxHP) / MaxHP} 对得上。</p>
     */
    public static int healthPercentOf(Scoreboard sb, Entity entity) {
        if (sb == null || entity == null) return -1;
        Objective obj = sb.getObjective("HealthPercent");
        if (obj == null) return -1;
        ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(entity, obj);
        return info == null ? -1 : info.value();
    }

    private static int pct(int hp, int maxHp) {
        if (maxHp <= 0) return 100;
        return Math.max(0, Math.min(100, Math.round(hp * 100.0f / maxHp)));
    }

    private static int score(Scoreboard sb, String objectiveName, ScoreHolder holder) {
        Objective obj = sb.getObjective(objectiveName);
        if (obj == null) return 0;
        ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(holder, obj);
        return info == null ? 0 : info.value();
    }
}
