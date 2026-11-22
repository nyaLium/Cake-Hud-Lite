package com.nyalium.cttenemyhud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户端能看到的「我这局是什么职业」（1.20.4）。
 *
 * <h2>数据从哪来</h2>
 * <p>地图<b>没有</b>把职业放进任何客户端可见的计分板槽（{@code sidebar=Lives}、
 * {@code below_name=HealthPercent}、{@code list=Coins}），所以最可靠的一条路是选完职业时
 * 地图会向全服喊一句：</p>
 *
 * <pre>
 *   tellraw @a ["", {"selector":"@a[tag=SwanSelected]"}, {"translate":"Floors_Scenes_Class_Select_75"}, {"translate":"class_select_12"}]
 *   → 「Player785 has become Swan」
 * </pre>
 *
 * <p>这一行的结尾是职业翻译键（{@code class_select_12} / {@code class_select_13} …），
 * 开头是<b>选中的人名</b>。本类在聊天里找「带职业键、且以自己名字开头」的行，
 * 把键记下来 —— 键在装了资源包后依旧留在 Component 里，所以两种情况下都能认。</p>
 *
 * <h2>局限（必须说清楚）</h2>
 * <ul>
 *   <li>只有<b>真的走了选职业流程</b>（点了职业台 / 有人替你选）才会喊这一句；
 *       某些快捷开局（QuickStart）可能直接设分、不喊话，这时就<b>认不出来</b>；</li>
 *   <li>所以配置里还能手动指定职业（{@link EnemyHudConfig#selfStatsClass}），
 *       认不出来时用「默认」那套过滤。</li>
 * </ul>
 */
public final class ClassTracker {

    /** {@code OFF}：完全不用职业过滤。 */
    public static final String MODE_OFF = "OFF";
    /** {@code AUTO}：按聊天里看到的职业自动切。 */
    public static final String MODE_AUTO = "AUTO";

    /** 地图选职业时用的键 → 英文名（取自地图 {@code en_us.json}，仅作界面显示）。 */
    private static final java.util.Map<String, String> CLASS_NAMES = buildNames();

    private static final List<String> OPTIONS = buildOptions();

    /** 当前识别到的职业键；null = 还没看到。 */
    private static String current;

    private ClassTracker() {}

    private static java.util.Map<String, String> buildNames() {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        String[] names = {
                "Swan", "Super", "Moose", "Lazy", "Wazy", "Nelfin", "Smash", "Thew", "Ben",
                "Nut", "Joey", "B'", "Dark Swan", "JJ", "Swan Jelly", "Gladiatus", "Mustin",
                "Henry", "Laozi"
        };
        // class_select_12 … class_select_30（顺序与地图一致）
        for (int i = 0; i < names.length; i++) {
            m.put("class_select_" + (12 + i), names[i]);
        }
        m.put("Floors_Scenes_Class_Select_101", "Vision");
        m.put("Floors_Scenes_Class_Select_104", "Fancy");
        return java.util.Collections.unmodifiableMap(m);
    }

    private static List<String> buildOptions() {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add(MODE_OFF);
        out.add(MODE_AUTO);
        out.addAll(CLASS_NAMES.keySet());
        return List.copyOf(out);
    }

    /** 职业过滤下拉框的全部取值：{@code OFF}、{@code AUTO}、各职业键。 */
    public static List<String> options() {
        return OPTIONS;
    }

    /** 职业键 → 显示名；{@code OFF}/{@code AUTO} 与未知键给出可读文本。 */
    public static String displayName(String key) {
        if (key == null) return "?";
        if (MODE_OFF.equals(key)) return "关";
        if (MODE_AUTO.equals(key)) return "自动";
        String name = CLASS_NAMES.get(key);
        return name != null ? name : key;
    }

    public static boolean isKnownClass(String key) {
        return key != null && CLASS_NAMES.containsKey(key);
    }

    /** 当前识别到的职业键。 */
    public static String current() {
        return current;
    }

    /** 断线 / 换服时清空。 */
    public static void reset() {
        current = null;
    }

    /** 供自检直接设定（不经过聊天解析）。 */
    static void setForTest(String key) {
        current = key;
    }

    /**
     * 配置里的职业模式 + 当前识别结果 → 真正生效的职业键（null = 用默认那套）。
     *
     * <p>纯函数，selfCheck 直接打。</p>
     */
    public static String resolvedClass(EnemyHudConfig.Cfg cfg, String detected) {
        if (cfg == null) return null;
        String mode = cfg.selfStatsClass;
        if (mode == null || MODE_OFF.equals(mode)) return null;
        if (MODE_AUTO.equals(mode)) return detected;
        return mode;
    }

    /**
     * 从一行聊天里认出职业（纯函数）。
     *
     * @param ownName 自己的玩家名；null 时不做身份限制（自检用）
     * @return 职业键，认不出返回 null
     */
    static String detect(Component message, String ownName) {
        if (message == null) return null;
        Set<String> classKeys = new LinkedHashSet<>();
        collectClassKeys(message, classKeys);
        if (classKeys.isEmpty()) return null;
        String flat = message.getString();
        if (flat == null) return null;
        if (ownName != null && !ownName.isBlank() && !flat.stripLeading().startsWith(ownName)) {
            return null;      // 别人选中职业的公告
        }
        // 一行里若同时出现多个职业键，取最后一个（「A has become B」里 B 在最后）
        String last = null;
        for (String key : classKeys) last = key;
        return last;
    }

    private static void collectClassKeys(Component component, Set<String> out) {
        if (component.getContents() instanceof TranslatableContents tc && isKnownClass(tc.getKey())) {
            out.add(tc.getKey());
        }
        for (Component sibling : component.getSiblings()) {
            collectClassKeys(sibling, out);
        }
    }

    /** 在 {@code ALLOW_GAME} 里调用：看到「自己选职业」的公告就记住。 */
    public static void onGameMessage(Component message, String ownName) {
        String key = detect(message, ownName);
        if (key != null && !key.equals(current)) {
            current = key;
        }
    }
}
