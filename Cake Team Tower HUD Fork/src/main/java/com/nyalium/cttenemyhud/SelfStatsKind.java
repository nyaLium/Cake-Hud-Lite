package com.nyalium.cttenemyhud;

/**
 * 自身属性面板里「一行内容」的归类（1.20.5 起按<b>单个属性</b>细分）。
 *
 * <h2>为什么要有归类</h2>
 * <p>地图 {@code /trigger ViewStats} 一次会发几十行。玩家只想看其中一部分，所以每行先归到一个
 * {@link SelfStatsKind}，再由配置里「隐藏哪些类」决定画不画（见
 * {@link EnemyHudConfig#selfStatsHidden}）。过滤界面把这个枚举整个列成可滚动列表，
 * <b>每一项都是一个可以单独关掉的属性</b>。</p>
 *
 * <h2>怎么认</h2>
 * <p>两条线索：</p>
 * <ol>
 *   <li><b>地图翻译键</b>（{@code Misc_View_Stats_N}）—— 键和属性一一对应，装了地图资源包也还在；</li>
 *   <li><b>字形 + 颜色</b> —— 数值行没有键，但地图给每个属性配了固定的图标和颜色
 *       （例如 🗡 金 = 职业属性、🗡 红 = 力量、🗡 蓝 = 攻速…）。</li>
 * </ol>
 *
 * <p><b>做不到的部分</b>：负值行一律是灰色，颜色一丢就分不出是哪个属性（灰 🗡 可能是职业属性 /
 * 力量 / 攻速），这类只能落到组内默认项；私有区字形的几条（钓鱼 / 因果…）也有颜色撞车的，
 * 归到 {@link #MISC}。</p>
 *
 * <h2>HINT</h2>
 * <p>{@link #HINT} 是地图的标题与悬停说明（{@code Misc_View_Stats_1} /
 * {@code Misc_View_Stats_2}），属于提示性文本、不是数据，<b>永远不上面板</b>，也不进过滤列表。</p>
 */
public enum SelfStatsKind {

    /** 标题 / 悬停提示：提示性文本，永远不显示，也不可过滤。 */
    HINT("hint", false),
    /** 游戏计时（{@code Misc_View_Stats_78}）。 */
    TIMER("timer", true),

    // ── 心 ──
    HEART_RED("heart_red", true),
    HEART_SOUL("heart_soul", true),
    HEART_BLACK("heart_black", true),
    HEART_BLUE("heart_blue", true),
    HEART_CRACKED("heart_cracked", true),
    HEART_PINK("heart_pink", true),

    // ── 生命 / 治疗 ──
    HP_AMP("hp_amp", true),
    HP_RAW("hp_raw", true),
    HP_NEG("hp_neg", true),
    HEAL_AMP("heal_amp", true),
    HEAL_RAW("heal_raw", true),
    HEAL_PERCENT("heal_percent", true),
    HEAL_EXTRA("heal_extra", true),

    // ── 攻击 ──
    CLASS_STAT("class_stat", true),
    STRENGTH("strength", true),
    ATTACK_SPEED("attack_speed", true),
    ATTACK_RANGE("attack_range", true),
    STAMINA("stamina", true),
    ARCHERY("archery", true),
    CLASS_STAT_AMP("class_stat_amp", true),
    STRENGTH_AMP("strength_amp", true),
    ATTACK_SPEED_AMP("attack_speed_amp", true),

    // ── 防御 ──
    DEFENCE("defence", true),
    TRUE_ARMOR("true_armor", true),
    DEFENCE_AMP("defence_amp", true),
    TRUE_ARMOR_AMP("true_armor_amp", true),

    // ── 元素护甲 ──
    FIRE_ARMOR("fire_armor", true),
    TRUE_FIRE_ARMOR("true_fire_armor", true),
    WATER_ARMOR("water_armor", true),
    ELECTRIC_ARMOR("electric_armor", true),
    ICE_ARMOR("ice_armor", true),
    DARK_ARMOR("dark_armor", true),
    LIGHT_ARMOR("light_armor", true),
    FIRE_ARMOR_AMP("fire_armor_amp", true),

    // ── 速度 / 跳跃 ──
    MAX_SPEED("max_speed", true),
    SPEED_AMP("speed_amp", true),
    JUMP("jump", true),
    DOUBLE_JUMP("double_jump", true),

    // ── 法力 ──
    MANA_POWER("mana_power", true),
    MANA_RECHARGE("mana_recharge", true),
    MANA_AMP("mana_amp", true),

    // ── 杂项 ──
    ACTIVE_CHARGE("active_charge", true),
    MAX_CHARGE("max_charge", true),
    ACTIVE_SPEED("active_speed", true),
    SUMMONING("summoning", true),
    SIZE("size", true),
    GRAVITY("gravity", true),
    TOWERS_REGEN("towers_regen", true),
    SKULLS("skulls", true),
    BITCHES("bitches", true),
    FISHING("fishing", true),
    BEE("bee", true),
    ANT("ant", true),
    KARMA_LIGHT("karma_light", true),
    KARMA_NEUTRAL("karma_neutral", true),
    KARMA_DARK("karma_dark", true),
    KARMA_CELESTIAL("karma_celestial", true),
    /** 兜底：认不出来的行（含灰色负值、颜色撞车的私有区字形）。 */
    MISC("misc", true),

    /** 状态效果（地图 {@code Misc_View_Stats_49..76} 那一批纯文本）。 */
    STATUS("status", true);

    /** 配置里存的名字（写进 JSON，改了就迁移不了，别动）。 */
    public final String id;
    /** 是否出现在「内容过滤」界面里。 */
    public final boolean filterable;

    SelfStatsKind(String id, boolean filterable) {
        this.id = id;
        this.filterable = filterable;
    }

    /** 过滤界面里的顺序（不含 {@link #HINT}）。 */
    public static final SelfStatsKind[] FILTERABLE = buildFilterable();

    private static SelfStatsKind[] buildFilterable() {
        java.util.List<SelfStatsKind> out = new java.util.ArrayList<>();
        for (SelfStatsKind kind : values()) {
            if (kind.filterable) out.add(kind);
        }
        return out.toArray(new SelfStatsKind[0]);
    }

    /** 未知 id 返回 null（配置里手写错 / 旧版本的粗类由配置层先展开）。 */
    public static SelfStatsKind byId(String id) {
        if (id == null) return null;
        for (SelfStatsKind kind : values()) {
            if (kind.id.equals(id)) return kind;
        }
        return null;
    }
}
