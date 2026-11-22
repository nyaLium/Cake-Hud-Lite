package com.nyalium.cttenemyhud;

/**
 * 自身属性面板里「一行内容」的归类（1.20.4）。
 *
 * <h2>为什么要有归类</h2>
 * <p>地图 {@code /trigger ViewStats} 一次会发十到几十行，混着标题、计时、四色心、
 * 生命/力量/防御/元素护甲/速度/法力/杂项/状态…。玩家只想看其中一部分，所以每行先归到
 * 一个 {@link SelfStatsKind}，再由配置里「隐藏哪些类」决定画不画（见
 * {@link EnemyHudConfig#selfStatsHidden}）。</p>
 *
 * <h2>怎么认</h2>
 * <p>优先认地图的翻译键（{@code Misc_View_Stats_N}，见 {@code view_stats.mcfunction}），
 * 因为键和属性是一一对应的、且<b>装了地图资源包也还在</b>（翻译改变的是渲染文本，
 * 不改变 Component 里的键）；心形这类没有键的行则按「颜色 + 字形」兜底。</p>
 *
 * <h2>HINT</h2>
 * <p>{@link #HINT} 是地图的标题与悬停说明（{@code Misc_View_Stats_1} /
 * {@code Misc_View_Stats_2}），属于提示性文本、不是数据，<b>永远不上面板</b>，
 * 也不进过滤列表。</p>
 */
public enum SelfStatsKind {

    /** 标题 / 悬停提示：提示性文本，永远不显示，也不可过滤。 */
    HINT("hint", false),
    /** 游戏计时（{@code Misc_View_Stats_78}）。 */
    TIME("time", true),
    HEART_RED("heart_red", true),
    HEART_SOUL("heart_soul", true),
    HEART_BLACK("heart_black", true),
    HEART_BLUE("heart_blue", true),
    HEART_CRACKED("heart_cracked", true),
    HEART_PINK("heart_pink", true),
    /** 生命：MaxHP 倍率 / 基础 MaxHP / 治疗 / 治疗倍率 / 百分比治疗。 */
    HEALTH("health", true),
    /** 攻击：职业属性、力量、攻速、攻击距离、耐力、弓术。 */
    ATTACK("attack", true),
    /** 防御：Defence / TrueArmor。 */
    DEFENCE("defence", true),
    /** 元素护甲：火 / 水 / 电 / 冰 / 暗 / 光 / 真伤护甲。 */
    ELEMENT("element", true),
    /** 速度：MaxSpeed / Speed 倍率。 */
    SPEED("speed", true),
    /** 法力：ManaPower / 回蓝速度。 */
    MANA("mana", true),
    /** 杂项：充能、跳跃、体型、重力、因果、鱼、蜂… */
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

    /** 过滤界面里按顺序列出的项（不含 {@link #HINT}）。 */
    public static final SelfStatsKind[] FILTERABLE = buildFilterable();

    private static SelfStatsKind[] buildFilterable() {
        java.util.List<SelfStatsKind> out = new java.util.ArrayList<>();
        for (SelfStatsKind kind : values()) {
            if (kind.filterable) out.add(kind);
        }
        return out.toArray(new SelfStatsKind[0]);
    }

    /** 未知 id 返回 null（配置里手写错的项就当不存在）。 */
    public static SelfStatsKind byId(String id) {
        if (id == null) return null;
        for (SelfStatsKind kind : values()) {
            if (kind.id.equals(id)) return kind;
        }
        return null;
    }
}
