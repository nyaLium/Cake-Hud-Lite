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
 * <h2>1.20.31 / 1.20.32：分组</h2>
 * <p>过滤界面不再是「一大串属性 + 状态效果 + 其它」三段粗桶，而是按 {@link Group} 分成三节，
 * 节与节之间画一条标题（和实体血条设置里的「── 通用 ── / ── 敌人设置 ──」是同一套观感）：</p>
 * <ul>
 *   <li>{@link Group#ATTRIBUTE}「属性」—— 心、生命、攻击、防御、护甲、速度、法力、杂项数值；</li>
 *   <li>{@link Group#STATUS}「状态效果」—— 地图的 28 条增益/诅咒，<b>一条一项</b>（{@link #STATUS_49} … {@link #STATUS_76}）；</li>
 *   <li>{@link Group#OTHER}「其它」—— 游戏时间 {@link #TIMER}、难度骷髅 {@link #SKULLS}（1.20.32 从「属性」挪过来）、
 *       草方块 {@link #GRASS} 与未识别兜底 {@link #MISC}。</li>
 * </ul>
 *
 * <h2>HINT</h2>
 * <p>{@link #HINT} 是地图的标题与悬停说明（{@code Misc_View_Stats_1} /
 * {@code Misc_View_Stats_2}），属于提示性文本、不是数据，<b>永远不上面板</b>，也不进过滤列表。</p>
 */
public enum SelfStatsKind {

    /** 标题 / 悬停提示：提示性文本，永远不显示，也不可过滤。 */
    HINT("hint", false, Group.ATTRIBUTE),

    // ── 心 ──
    HEART_RED("heart_red", true, Group.ATTRIBUTE),
    HEART_SOUL("heart_soul", true, Group.ATTRIBUTE),
    HEART_BLACK("heart_black", true, Group.ATTRIBUTE),
    HEART_BLUE("heart_blue", true, Group.ATTRIBUTE),
    HEART_CRACKED("heart_cracked", true, Group.ATTRIBUTE),
    HEART_PINK("heart_pink", true, Group.ATTRIBUTE),

    // ── 生命 / 治疗 ──
    HP_AMP("hp_amp", true, Group.ATTRIBUTE),
    HP_RAW("hp_raw", true, Group.ATTRIBUTE),
    HP_NEG("hp_neg", true, Group.ATTRIBUTE),
    HEAL_AMP("heal_amp", true, Group.ATTRIBUTE),
    HEAL_RAW("heal_raw", true, Group.ATTRIBUTE),
    HEAL_PERCENT("heal_percent", true, Group.ATTRIBUTE),
    HEAL_EXTRA("heal_extra", true, Group.ATTRIBUTE),

    // ── 攻击 ──
    CLASS_STAT("class_stat", true, Group.ATTRIBUTE),
    STRENGTH("strength", true, Group.ATTRIBUTE),
    ATTACK_SPEED("attack_speed", true, Group.ATTRIBUTE),
    ATTACK_RANGE("attack_range", true, Group.ATTRIBUTE),
    STAMINA("stamina", true, Group.ATTRIBUTE),
    ARCHERY("archery", true, Group.ATTRIBUTE),
    CLASS_STAT_AMP("class_stat_amp", true, Group.ATTRIBUTE),
    STRENGTH_AMP("strength_amp", true, Group.ATTRIBUTE),
    ATTACK_SPEED_AMP("attack_speed_amp", true, Group.ATTRIBUTE),

    // ── 防御 ──
    DEFENCE("defence", true, Group.ATTRIBUTE),
    TRUE_ARMOR("true_armor", true, Group.ATTRIBUTE),
    DEFENCE_AMP("defence_amp", true, Group.ATTRIBUTE),
    TRUE_ARMOR_AMP("true_armor_amp", true, Group.ATTRIBUTE),

    // ── 元素护甲 ──
    FIRE_ARMOR("fire_armor", true, Group.ATTRIBUTE),
    TRUE_FIRE_ARMOR("true_fire_armor", true, Group.ATTRIBUTE),
    WATER_ARMOR("water_armor", true, Group.ATTRIBUTE),
    ELECTRIC_ARMOR("electric_armor", true, Group.ATTRIBUTE),
    ICE_ARMOR("ice_armor", true, Group.ATTRIBUTE),
    DARK_ARMOR("dark_armor", true, Group.ATTRIBUTE),
    LIGHT_ARMOR("light_armor", true, Group.ATTRIBUTE),
    FIRE_ARMOR_AMP("fire_armor_amp", true, Group.ATTRIBUTE),

    // ── 速度 / 跳跃 ──
    MAX_SPEED("max_speed", true, Group.ATTRIBUTE),
    SPEED_AMP("speed_amp", true, Group.ATTRIBUTE),
    JUMP("jump", true, Group.ATTRIBUTE),
    DOUBLE_JUMP("double_jump", true, Group.ATTRIBUTE),

    // ── 法力 ──
    MANA_POWER("mana_power", true, Group.ATTRIBUTE),
    MANA_RECHARGE("mana_recharge", true, Group.ATTRIBUTE),
    MANA_AMP("mana_amp", true, Group.ATTRIBUTE),

    // ── 杂项数值 ──
    ACTIVE_CHARGE("active_charge", true, Group.ATTRIBUTE),
    MAX_CHARGE("max_charge", true, Group.ATTRIBUTE),
    ACTIVE_SPEED("active_speed", true, Group.ATTRIBUTE),
    SUMMONING("summoning", true, Group.ATTRIBUTE),
    SIZE("size", true, Group.ATTRIBUTE),
    GRAVITY("gravity", true, Group.ATTRIBUTE),
    TOWERS_REGEN("towers_regen", true, Group.ATTRIBUTE),
    BITCHES("bitches", true, Group.ATTRIBUTE),
    FISHING("fishing", true, Group.ATTRIBUTE),
    BEE("bee", true, Group.ATTRIBUTE),
    ANT("ant", true, Group.ATTRIBUTE),
    KARMA_LIGHT("karma_light", true, Group.ATTRIBUTE),
    KARMA_NEUTRAL("karma_neutral", true, Group.ATTRIBUTE),
    KARMA_DARK("karma_dark", true, Group.ATTRIBUTE),
    KARMA_CELESTIAL("karma_celestial", true, Group.ATTRIBUTE),

    // ── 其它 ──

    /**
     * 游戏计时（{@code Misc_View_Stats_78}）。
     *
     * <p>1.20.31 起这一行会上面板（原来只当收尾信号）；1.20.32 起按你的要求归到「其它」一节，
     * 不再混在属性里。</p>
     */
    TIMER("timer", true, Group.OTHER),

    /** 难度骷髅（地图 {red ☠ = 正数 / 灰 ☠ = 负数}，键号 {@code view_stats_84}）。1.20.32 起归到「其它」。 */
    SKULLS("skulls", true, Group.OTHER),

    /**
     * 「本轮还没碰过草方块」那条（地图 {@code view_stats_83}，一行绿色的 {@code □}）。
     *
     * <p>1.20.31 起单独成类：它既没有翻译键、也没有数字，之前被当成「不认识的行」整行丢掉，
     * 所以面板上从来没见过它 —— 现在它属于「其它」这一节，可以单独关。</p>
     */
    GRASS("grass", true, Group.OTHER),

    /**
     * 兜底：认不出来的行（含灰色负值、颜色撞车的私有区字形）。
     *
     * <p>1.20.31 起它在过滤界面里叫「未识别行」，放在「其它」一节的最下面 ——
     * 已经能单独认出来的属性（含 28 条状态效果、草方块）都有自己的项，这里只剩真正认不出的。</p>
     */
    MISC("misc", true, Group.OTHER),

    // ── 状态效果：地图 Misc_View_Stats_49 .. 76（一行一个，各自一条） ──

    /** 状态效果 49：斗志昂扬（Fired Up）。 */
    STATUS_49("status_49", true, Group.STATUS),
    /** 状态效果 50：食物专家（Food Connoisseur）。 */
    STATUS_50("status_50", true, Group.STATUS),
    /** 状态效果 51：神秘的吸血鬼挑战（Mysterious Vampire Challenge）。 */
    STATUS_51("status_51", true, Group.STATUS),
    /** 状态效果 52：护送姜饼人（Gingerbread Protector）。 */
    STATUS_52("status_52", true, Group.STATUS),
    /** 状态效果 53：追杀被诅咒的模仿者（Tracking Cursed Mimics）。 */
    STATUS_53("status_53", true, Group.STATUS),
    /** 状态效果 54：光之加护审判（Light Armor Judged）。 */
    STATUS_54("status_54", true, Group.STATUS),
    /** 状态效果 55：进度锁定（Advancement Progression Disabled）。 */
    STATUS_55("status_55", true, Group.STATUS),
    /** 状态效果 56：腿骨骨折（Broken Leg）。 */
    STATUS_56("status_56", true, Group.STATUS),
    /** 状态效果 57：胸骨断裂（Broken Ribcage）。 */
    STATUS_57("status_57", true, Group.STATUS),
    /** 状态效果 58：头骨骨折（Broken Skull）。 */
    STATUS_58("status_58", true, Group.STATUS),
    /** 状态效果 59：手臂骨折（Broken Arm）。 */
    STATUS_59("status_59", true, Group.STATUS),
    /** 状态效果 60：股骨破碎（Broken Femur）。 */
    STATUS_60("status_60", true, Group.STATUS),
    /** 状态效果 61：枪伤（Bullet Wound）。 */
    STATUS_61("status_61", true, Group.STATUS),
    /** 状态效果 62：燃烧（Burnt）。 */
    STATUS_62("status_62", true, Group.STATUS),
    /** 状态效果 63：头晕目眩（Dizzy）。 */
    STATUS_63("status_63", true, Group.STATUS),
    /** 状态效果 64：窥伺（Stalked）。 */
    STATUS_64("status_64", true, Group.STATUS),
    /** 状态效果 65：诅咒（Cursed）。 */
    STATUS_65("status_65", true, Group.STATUS),
    /** 状态效果 66：消沉（Depressed）。 */
    STATUS_66("status_66", true, Group.STATUS),
    /** 状态效果 67：恐惧（Afraid）。 */
    STATUS_67("status_67", true, Group.STATUS),
    /** 状态效果 68：失忆（Amnesia）。 */
    STATUS_68("status_68", true, Group.STATUS),
    /** 状态效果 69：窃贼（Thief）。 */
    STATUS_69("status_69", true, Group.STATUS),
    /** 状态效果 70：被污染者通缉（Wanted by the Garbage Collectors）。 */
    STATUS_70("status_70", true, Group.STATUS),
    /** 状态效果 71：诅咒之剑（Cursed Sword）。 */
    STATUS_71("status_71", true, Group.STATUS),
    /** 状态效果 72：星尘的诅咒（Xing's Curse）。 */
    STATUS_72("status_72", true, Group.STATUS),
    /** 状态效果 73：开放性伤口（Open Wound）。 */
    STATUS_73("status_73", true, Group.STATUS),
    /** 状态效果 74：癫狂（Berserk）。 */
    STATUS_74("status_74", true, Group.STATUS),
    /** 状态效果 75：鬼缠契约（Contract Haunted）。 */
    STATUS_75("status_75", true, Group.STATUS),
    /** 状态效果 76：审视（Observed）。 */
    STATUS_76("status_76", true, Group.STATUS);

    /** 过滤界面里的分节（1.20.31）。顺序即显示顺序。 */
    public enum Group {
        /** 面板里的具体属性。 */
        ATTRIBUTE("ctt_enemy_hud.config.header.attributes"),
        /** 地图那 28 条状态效果。 */
        STATUS("ctt_enemy_hud.config.header.status"),
        /** 其它：游戏时间、难度骷髅、草方块、未识别兜底。 */
        OTHER("ctt_enemy_hud.config.header.other");

        /** 分节标题的翻译键（画在两组之间，和实体血条设置的标题同一套观感）。 */
        public final String headerKey;

        Group(String headerKey) {
            this.headerKey = headerKey;
        }
    }

    /** 配置里存的名字（写进 JSON，改了就迁移不了，别动）。 */
    public final String id;
    /** 是否出现在「内容过滤」界面里。 */
    public final boolean filterable;
    /** 归在哪一节（过滤界面按节分组显示）。 */
    public final Group group;

    SelfStatsKind(String id, boolean filterable, Group group) {
        this.id = id;
        this.filterable = filterable;
        this.group = group;
    }

    /** 过滤界面里的顺序（不含 {@link #HINT}），按声明顺序。 */
    public static final SelfStatsKind[] FILTERABLE = buildFilterable();

    private static SelfStatsKind[] buildFilterable() {
        java.util.List<SelfStatsKind> out = new java.util.ArrayList<>();
        for (SelfStatsKind kind : values()) {
            if (kind.filterable) out.add(kind);
        }
        return out.toArray(new SelfStatsKind[0]);
    }

    /** 某一节里可过滤的项（按声明顺序）；空节返回空数组。 */
    public static SelfStatsKind[] section(Group group) {
        if (group == null) return new SelfStatsKind[0];
        java.util.List<SelfStatsKind> out = new java.util.ArrayList<>();
        for (SelfStatsKind kind : FILTERABLE) {
            if (kind.group == group) out.add(kind);
        }
        return out.toArray(new SelfStatsKind[0]);
    }

    /** 状态效果的键号（{@code Misc_View_Stats_N} 的 N，49~76）→ 类；其它数字返回 null。 */
    public static SelfStatsKind status(int keyNumber) {
        if (keyNumber < 49 || keyNumber > 76) return null;
        return byId("status_" + keyNumber);
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
