package com.nyalium.cttenemyhud;

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
 * {@code ctt_enemy_hud_expose.mcfunction}），绝对值通道就立刻打通，本类无需改动。</p>
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
    public static final String[] ARMOR_ICONS = {
            "\uD83D\uDD25", "\u2603", "\u2620", "\u2600", "\uD83D\uDEE1", "\u2693", "\u26A1"
    };
    /** gold / aqua / dark_purple / white / white / aqua / yellow（与原版 ChatFormatting 同值）。 */
    public static final int[] ARMOR_COLORS = {
            0xFFAA00, 0x55FFFF, 0xAA00AA, 0xFFFFFF, 0xFFFFFF, 0x55FFFF, 0xFFFF55
    };
    /** 蓝 🛡 Defence 的颜色。 */
    public static final int DEFENCE_COLOR = 0x5555FF;

    private EnemyData() {}

    /** 单只敌人一帧的读数。 */
    public static final class Snapshot {
        public final int hp;
        public final int maxHp;
        /** true = 只有百分比（读到的是 HealthPercent），画 "73%" 而不是 "73/120"。 */
        public final boolean percentOnly;
        public final int percent;
        public final int defence;
        /** 7 档元素护甲，0 = 没有。 */
        public final int[] armor;

        Snapshot(int hp, int maxHp, boolean percentOnly, int percent, int defence, int[] armor) {
            this.hp = hp;
            this.maxHp = maxHp;
            this.percentOnly = percentOnly;
            this.percent = percent;
            this.defence = defence;
            this.armor = armor;
        }

        /** 血量比例 0..1，血条按它填充。 */
        public float ratio() {
            if (percentOnly) {
                return Math.max(0.0f, Math.min(1.0f, percent / 100.0f));
            }
            if (maxHp <= 0) return 0.0f;
            return Math.max(0.0f, Math.min(1.0f, (float) hp / (float) maxHp));
        }

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
     * @param armor 七档元素护甲，长度不足按 0 处理
     */
    public static Snapshot exact(int hp, int maxHp, int defence, int[] armor) {
        int[] fixed = new int[ARMOR_OBJECTIVES.length];
        if (armor != null) {
            System.arraycopy(armor, 0, fixed, 0, Math.min(armor.length, fixed.length));
        }
        int safeMax = Math.max(1, maxHp);
        int safeHp = Math.max(0, Math.min(hp, safeMax));
        return new Snapshot(safeHp, safeMax, false, pct(safeHp, safeMax), defence, fixed);
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
            return new Snapshot(current, maxHp, false, pct(current, maxHp), defence, armor);
        }

        // 绝对值通道没打通：退回 below_name 的百分比（每只怪都有）。
        int p = score(sb, "HealthPercent", entity);
        if (p > 0) {
            p = Math.min(100, p);
            return new Snapshot(p, 100, true, p, defence, armor);
        }
        return null;
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
