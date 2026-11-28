package com.nyalium.ctthudfork;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 自身属性面板的「解析 / 分类」目录：颜色常量、地图翻译键表、Component 摊平、行分类与心形解析。
 *
 * <p>这些是从 {@link SelfStats} 拆出来的<b>纯函数</b>，不碰 Minecraft 采集状态
 * （双缓冲、计时行、静默窗口、配置过滤都还留在 {@link SelfStats}）。{@link SelfStats} 保留同名的
 * 包内 static 转发，所以调用方与 {@code SelfCheck} 的用法、行为都跟拆分前完全一致。</p>
 */
final class SelfStatsCatalog {

    private SelfStatsCatalog() {}

    /** 地图 tellraw 里用的颜色（{@code ChatFormatting} 的 RGB）。 */
    static final int RED = 0xFF5555;
    static final int YELLOW = 0xFFFF55;
    static final int BLACK = 0x000000;
    static final int BLUE = 0x5555FF;
    static final int DARK_RED = 0xAA0000;
    static final int LIGHT_PURPLE = 0xFF55FF;
    /** 原版 gray（{@link net.minecraft.ChatFormatting#GRAY}），「负生命」那行用它。 */
    static final int GRAY = 0xAAAAAA;

    // ── 地图翻译键（view_stats.mcfunction） ──

    static final String KEY_PREFIX = "Misc_View_Stats_";
    /** 标题 + 「悬停图标查看说明」：提示性文本，永远不上面板。 */
    static final Set<String> HINT_KEYS = Set.of(KEY_PREFIX + "1", KEY_PREFIX + "2");
    /** 游戏计时行：「The Game time is 」+ 时分秒。 */
    static final String TIME_KEY = KEY_PREFIX + "78";
    /** 键 → 分类（键与属性一一对应，见地图 view_stats.mcfunction）。 */
    private static final java.util.Map<String, SelfStatsKind> KEY_KINDS = buildKeyKinds();
    /** hover 键 {@code view_stats_N} → 分类（属性行优先用这个认）。 */
    private static final java.util.Map<String, SelfStatsKind> HOVER_KINDS = buildHoverKinds();
    /** 计分板目标名 → 分类（数值行最稳的身份：与语言 / 颜色 / 图标都无关）。 */
    private static final java.util.Map<String, SelfStatsKind> OBJECTIVE_KINDS = buildObjectiveKinds();

    /** hover 键 → 分类（供自检）。 */
    static SelfStatsKind hoverKind(String key) {
        return HOVER_KINDS.get(key);
    }

    /** 计分板目标名 → 分类（供自检）。 */
    static SelfStatsKind objectiveKind(String objective) {
        return OBJECTIVE_KINDS.get(objective);
    }

    // ── 解析（纯函数，selfCheck 直接打） ──

    /** 一行解析出来的原始素材：文本、每字符颜色、翻译键、是否含未解析键。 */
    static final class Analyzed {
        final StringBuilder flat = new StringBuilder();
        final List<Integer> colors = new ArrayList<>();
        final Set<String> keys = new LinkedHashSet<>();
        /**
         * 图标上的 hover 翻译键（{@code view_stats_N}）。
         *
         * <p>数据包给每个属性图标都挂了 {@code hover_event.show_text -> view_stats_N}，
         * 这是**每个属性唯一且稳定**的标识（比「字形 + 颜色」可靠得多）。</p>
         */
        final Set<String> hoverKeys = new LinkedHashSet<>();
        /**
         * 这一行里出现过的<b>计分板目标名</b>（{@code {"score":{"objective":"Jump"}}}）。
         *
         * <p>数值行一定带目标名，而且它<b>与语言、颜色、图标都无关</b> —— 比 hover 键更稳：
         * 地图有些行根本没有 hover 键（例如 {@code DarkKarma}），单靠键会落到兜底。</p>
         */
        final Set<String> objectives = new LinkedHashSet<>();
        /** true = 这一行里有计分板分数（{@code visit} 摊平时分数不会变成文本）。 */
        boolean hasScore;
        /** true = 有翻译键没被客户端解析出来（没装地图资源包时会这样）。 */
        boolean unresolvedKey;

        String text() {
            return flat.toString();
        }

        boolean hasKey() {
            for (String key : keys) {
                if (key.startsWith(KEY_PREFIX)) return true;
            }
            return false;
        }
    }

    /** 摊平一段 Component：文本 + 每字符颜色 + 全部翻译键。 */
    static Analyzed analyze(Component message) {
        Analyzed a = new Analyzed();
        if (message == null) return a;
        message.visit((style, text) -> {
            int rgb = style.getColor() != null ? style.getColor().getValue() : 0xFFFFFF;
            a.flat.append(text);
            for (int i = 0; i < text.length(); i++) a.colors.add(rgb);
            // 把图标 hover 里的 view_stats_N 也收集起来（属性身份的唯一锚点）。
            if (style.getHoverEvent() instanceof net.minecraft.network.chat.HoverEvent.ShowText show) {
                collectHoverKeys(show.value(), a);
            }
            return Optional.empty();
        }, Style.EMPTY);
        collectKeys(message, a);
        collectObjectives(message, a);
        return a;
    }

    /** 递归收集计分板目标名（分数行一定会带，且与语言无关）。 */
    private static void collectObjectives(Component component, Analyzed a) {
        if (component == null) return;
        if (component.getContents() instanceof ScoreContents score) {
            a.hasScore = true;
            String objective = score.objective();
            if (objective != null && !objective.isEmpty()) a.objectives.add(objective);
        }
        for (Component sibling : component.getSiblings()) {
            collectObjectives(sibling, a);
        }
    }

    /** 递归收集 hover 文本里的 {@code view_stats_N} 键。 */
    private static void collectHoverKeys(Component component, Analyzed a) {
        if (component == null) return;
        if (component.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if (key != null && key.startsWith("view_stats_")) a.hoverKeys.add(key);
        }
        for (Component sibling : component.getSiblings()) {
            collectHoverKeys(sibling, a);
        }
    }

    private static void collectKeys(Component component, Analyzed a) {
        if (component.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if (key != null) {
                a.keys.add(key);
                // 没解析出来时，渲染文本就是键本身（带参数时是「键 + 参数」），据此判定。
                String own;
                try {
                    own = component.plainCopy().getString();
                } catch (RuntimeException e) {
                    own = null;
                }
                if (key.startsWith(KEY_PREFIX) && own != null && own.startsWith(key)) {
                    a.unresolvedKey = true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectKeys(sibling, a);
        }
    }

    /** 心形字形：{@code ❤} 或地图字体里用于碎心/粉心的私有区字符。 */
    static boolean isHeartGlyph(char ch) {
        return ch == '\u2764' || (ch >= '\uE000' && ch <= '\uF8FF');
    }

    /**
     * 字形 + 颜色 → 心类。
     *
     * @return 1 红 2 魂 3 黑 4 蓝 5 碎 6 粉，0 = 不是心
     */
    static int heartKindOf(String glyph, int rgb) {
        boolean heart = glyph != null && glyph.indexOf('\u2764') >= 0;
        if (heart) {
            if (rgb == RED) return 1;
            if (rgb == YELLOW) return 2;
            if (rgb == BLACK) return 3;
            if (rgb == BLUE) return 4;
            return 0;
        }
        // 碎心 / 粉心用私有区字形，只能靠颜色认
        if (rgb == DARK_RED) return 5;
        if (rgb == LIGHT_PURPLE) return 6;
        return 0;
    }

    /** 取 {@code flat[idx]} 这个字形前面紧邻的数字（地图格式是「数字+字形」）。 */
    static int amountBefore(String flat, int idx) {
        if (flat == null || idx <= 0) return -1;
        int i = idx - 1;
        while (i >= 0 && flat.charAt(i) == ' ') i--;
        int end = i;
        while (i >= 0 && Character.isDigit(flat.charAt(i))) i--;
        if (i == end) return -1;
        try {
            return Integer.parseInt(flat.substring(i + 1, end + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    /**
     * 地图 ViewStats 里「数字 + 图形」那一类行（{@code ["",{score},{text:"🗡",hover}]} 的形态）。
     *
     * <p>把原来「数字开头就算」和「两个字以内就算」两条宽规则换成这条 ——
     * 地图 134 条 tellraw 里，除了带 {@code Misc_View_Stats_*} 翻译键的，剩下的全都是
     * 「数字 + 下面这些图形之一」，既不会漏，也不会把玩家的「123」「ok」当成属性行。</p>
     */
    static boolean statGlyphLine(String flat) {
        if (flat == null || flat.isEmpty()) return false;
        boolean digit = false;
        boolean glyph = false;
        for (int i = 0; i < flat.length(); ) {
            int cp = flat.codePointAt(i);
            i += Character.charCount(cp);
            if (cp >= '0' && cp <= '9') digit = true;
            else if (isStatGlyphCp(cp)) glyph = true;
            if (digit && glyph) return true;
        }
        return false;
    }

    /**
     * 「本轮还没碰过草方块」那条（地图 {@code view_stats_83}）：整行就是一个绿色的 {@code □}。
     *
     * <p>制表符以外的空白一律先去掉再比，避免地图哪天多一个空格就认不出来。</p>
     */
    static boolean isGrassLine(String flat) {
        return flat != null && flat.strip().equals("\u25A1");
    }

    /** 地图属性行里用到的图形码位（含私有区字形）。 */
    static boolean isStatGlyphCp(int cp) {
        if (cp == 0x2764) return true;                    // ❤
        if (cp >= 0xE000 && cp <= 0xF8FF) return true;    // 地图私有区字形
        for (int icon : STAT_ICONS) {
            if (icon == cp) return true;
        }
        return false;
    }

    /** 🛡 🗡 🏹 ⚡ ✈ 🔥 ☠ ✏ ⚓ ☃ ☀ ⬇ ⬆ ⇑ ⇓（从 view_stats.mcfunction 里抽出来的全部图形）。 */
    static final int[] STAT_ICONS = {
            0x1F6E1, 0x1F5E1, 0x1F3F9, 0x26A1, 0x2708, 0x1F525, 0x2620, 0x270F,
            0x2693, 0x2603, 0x2600, 0x2B07, 0x2B06, 0x21D1, 0x21D3,
    };

    /**
     * 一行的分类（纯函数）。
     *
     * <p>优先级：提示键 &gt; 计时键 &gt; 其余属性键 &gt; 字形/颜色兜底。</p>
     */
    static SelfStatsKind classify(Analyzed a) {
        if (a == null) return SelfStatsKind.MISC;
        for (String key : a.keys) {
            if (HINT_KEYS.contains(key)) return SelfStatsKind.HINT;
        }
        for (String key : a.keys) {
            if (TIME_KEY.equals(key)) return SelfStatsKind.TIMER;
        }
        // 先按 hover 键（view_stats_N）认 —— 每个属性一个键，精确且不依赖颜色。
        for (String key : a.hoverKeys) {
            SelfStatsKind kind = HOVER_KINDS.get(key);
            if (kind != null) return kind;
        }
        // 再看计分板目标名：数值行的「身份证」，与语言 / 颜色 / 图标都无关。
        for (String objective : a.objectives) {
            SelfStatsKind kind = OBJECTIVE_KINDS.get(objective);
            if (kind != null) return kind;
        }
        for (String key : a.keys) {
            SelfStatsKind kind = KEY_KINDS.get(key);
            if (kind != null) return kind;
        }
        return glyphKind(a.text(), a.colors);
    }

    /**
     * 没有翻译键时的兜底：按「图标 + 颜色」认到具体属性。
     *
     * <p>地图给每个属性配了固定的图标和颜色（🗡 金 = 职业属性、🗡 红 = 力量、🗡 蓝 = 攻速…），
     * 所以数值行即使没有键也能分开。灰色（负值）的颜色丢了身份，落到组内默认项。</p>
     */
    static SelfStatsKind glyphKind(String flat, List<Integer> colors) {
        if (flat == null || flat.isEmpty()) return SelfStatsKind.MISC;

        // 草方块那条：地图就发一个绿色的 □（view_stats_83），没有数字也没有翻译键。
        if (isGrassLine(flat)) return SelfStatsKind.GRASS;

        // 「数字❤%」：地图里只有治疗百分比用这个组合（红 = HealPercent，亮紫 = ExtraHealing）
        int percent = flat.indexOf("\u2764%");
        if (percent >= 0) {
            return colorAt(colors, percent) == LIGHT_PURPLE
                    ? SelfStatsKind.HEAL_EXTRA : SelfStatsKind.HEAL_PERCENT;
        }

        // 元素护甲：🛡 + 元素图标（颜色再分「真伤护甲」和普通护甲）
        int shield = flat.indexOf("\uD83D\uDEE1");
        if (shield >= 0) {
            int after = shield + 2;
            int rgb = colorAt(colors, shield);
            if (flat.startsWith("\uD83D\uDD25", after)) {          // 🔥 火
                return rgb == 0xFFFFFF ? SelfStatsKind.TRUE_FIRE_ARMOR : SelfStatsKind.FIRE_ARMOR;
            }
            if (flat.startsWith("\u2693", after)) return SelfStatsKind.WATER_ARMOR;      // ⚓
            if (flat.startsWith("\u26A1", after)) return SelfStatsKind.ELECTRIC_ARMOR;   // ⚡
            if (flat.startsWith("\u2603", after)) return SelfStatsKind.ICE_ARMOR;        // ☃
            if (flat.startsWith("\u2620", after)) return SelfStatsKind.DARK_ARMOR;       // ☠
            if (flat.startsWith("\u2600", after)) return SelfStatsKind.LIGHT_ARMOR;      // ☀
            return rgb == 0xFFFFFF ? SelfStatsKind.TRUE_ARMOR : SelfStatsKind.DEFENCE;
        }

        // 🗡 五档（颜色区分），🏹 弓术
        int sword = flat.indexOf("\uD83D\uDDE1");
        if (sword >= 0) {
            int rgb = colorAt(colors, sword);
            if (rgb == 0xFF5555) return SelfStatsKind.STRENGTH;          // 红
            if (rgb == 0x5555FF) return SelfStatsKind.ATTACK_SPEED;      // 蓝
            if (rgb == 0x00AA00) return SelfStatsKind.ATTACK_RANGE;      // 深绿
            if (rgb == 0x55FF55) return SelfStatsKind.STAMINA;           // 绿
            return SelfStatsKind.CLASS_STAT;                             // 金 / 灰
        }
        if (flat.indexOf("\uD83C\uDFF9") >= 0) return SelfStatsKind.ARCHERY;   // 🏹

        // ✈：最高速度 / 跳跃 / 二段跳
        int plane = flat.indexOf("\u2708");
        if (plane >= 0) {
            int rgb = colorAt(colors, plane);
            if (rgb == 0x55FF55) return SelfStatsKind.JUMP;              // 绿
            if (rgb == 0xFF87FF) return SelfStatsKind.DOUBLE_JUMP;       // 粉
            return SelfStatsKind.MAX_SPEED;                              // 青 / 灰
        }
        // ✏：法力强度 / 回蓝速度
        int pencil = flat.indexOf("\u270F");
        if (pencil >= 0) {
            return colorAt(colors, pencil) == 0x55FFFF
                    ? SelfStatsKind.MANA_RECHARGE : SelfStatsKind.MANA_POWER;
        }
        // ⚡：主动充能 / 上限 / 速度
        int bolt = flat.indexOf("\u26A1");
        if (bolt >= 0) {
            int rgb = colorAt(colors, bolt);
            if (rgb == 0xFFAA00) return SelfStatsKind.MAX_CHARGE;        // 金
            if (rgb == 0xFFFFFF) return SelfStatsKind.ACTIVE_SPEED;      // 白
            return SelfStatsKind.ACTIVE_CHARGE;                          // 黄 / 灰
        }
        // ☠：召唤 / 骷髅数
        int skull = flat.indexOf("\u2620");
        if (skull >= 0) {
            return colorAt(colors, skull) == 0xFF5555 ? SelfStatsKind.SKULLS : SelfStatsKind.SUMMONING;
        }
        if (flat.indexOf("\u2B07") >= 0 || flat.indexOf("\u2B06") >= 0) return SelfStatsKind.SIZE;      // ⬇⬆
        if (flat.indexOf("\u21D1") >= 0 || flat.indexOf("\u21D3") >= 0) return SelfStatsKind.GRAVITY;   // ⇑⇓

        // 心形（含碎心/粉心用的私有区字形）
        for (int i = 0; i < flat.length(); i++) {
            char ch = flat.charAt(i);
            if (!isHeartGlyph(ch)) continue;
            int rgb = colorAt(colors, i);
            if (ch == '\u2764') {
                if (rgb == RED) return SelfStatsKind.HEART_RED;
                if (rgb == YELLOW) return SelfStatsKind.HEART_SOUL;
                if (rgb == BLACK) return SelfStatsKind.HEART_BLACK;
                if (rgb == BLUE) return SelfStatsKind.HEART_BLUE;
                if (rgb == LIGHT_PURPLE) return SelfStatsKind.HEAL_RAW;   // 治疗图标也是 ❤
                if (rgb == DARK_RED) return SelfStatsKind.BITCHES;
                return SelfStatsKind.HP_NEG;                              // 灰 = 负生命
            }
            // 地图自定义字体里的私有区图标：字符本身就带身份（见 view_stats.mcfunction）
            switch (ch) {
                case '\uE026':                       // 碎心 / 粉心：同一个字形，颜色区分
                    if (rgb == DARK_RED) return SelfStatsKind.HEART_CRACKED;
                    if (rgb == LIGHT_PURPLE) return SelfStatsKind.HEART_PINK;
                    return SelfStatsKind.HEART_CRACKED;
                case '\uE004':                       // 塔内回血
                    return SelfStatsKind.TOWERS_REGEN;
                case '\uE003':                       // 钓鱼
                    return SelfStatsKind.FISHING;
                case '\uE001':                       // 蜜蜂点 / 蚂蚁点：同一个字形，颜色区分
                    if (rgb == 0x000000) return SelfStatsKind.ANT;
                    return SelfStatsKind.BEE;
                case '\uE010':                       // 光明因果 / 中立因果：同一个字形，颜色区分
                    if (rgb == 0xAA00AA) return SelfStatsKind.KARMA_NEUTRAL;
                    return SelfStatsKind.KARMA_LIGHT;
                case '\uE011':                       // 黑暗因果
                    return SelfStatsKind.KARMA_DARK;
                case '\uE027':                       // 天界因果
                    return SelfStatsKind.KARMA_CELESTIAL;
                default:
                    break;
            }
        }
        return SelfStatsKind.MISC;
    }

    /** 第 {@code index} 个字符的颜色（越界/没有颜色时按白色）。 */
    private static int colorAt(List<Integer> colors, int index) {
        if (colors == null || index < 0 || index >= colors.size()) return 0xFFFFFF;
        return colors.get(index);
    }

    // ── 地图翻译键表 ──

    private static java.util.Map<String, SelfStatsKind> buildKeyKinds() {
        java.util.Map<String, SelfStatsKind> m = new java.util.HashMap<>();
        // 状态效果：Misc_View_Stats_49 … 76（纯文本，一行一个；一条一项）
        for (int i = 49; i <= 76; i++) {
            SelfStatsKind kind = SelfStatsKind.status(i);
            if (kind != null) m.put(KEY_PREFIX + i, kind);
        }
        // 倍率 / 基础值：地图的键号分两套（旧 4/8/10/12/16/18/20/25/33/40，新 83/87/90/93/101/104/107/115/122/124），
        // **原始值（乘算前）单独成类**，不再并进「倍率」。
        m.put(KEY_PREFIX + "4", SelfStatsKind.HP_AMP);
        m.put(KEY_PREFIX + "83", SelfStatsKind.HP_AMP);
        m.put(KEY_PREFIX + "5", SelfStatsKind.HP_RAW);
        m.put(KEY_PREFIX + "8", SelfStatsKind.CLASS_STAT_AMP);
        m.put(KEY_PREFIX + "87", SelfStatsKind.CLASS_STAT_AMP);
        m.put(KEY_PREFIX + "9", SelfStatsKind.CLASS_STAT_RAW);
        m.put(KEY_PREFIX + "10", SelfStatsKind.STRENGTH_AMP);
        m.put(KEY_PREFIX + "90", SelfStatsKind.STRENGTH_AMP);
        m.put(KEY_PREFIX + "11", SelfStatsKind.STRENGTH_RAW);
        m.put(KEY_PREFIX + "12", SelfStatsKind.ATTACK_SPEED_AMP);
        m.put(KEY_PREFIX + "93", SelfStatsKind.ATTACK_SPEED_AMP);
        m.put(KEY_PREFIX + "13", SelfStatsKind.ATTACK_SPEED_RAW);
        m.put(KEY_PREFIX + "16", SelfStatsKind.DEFENCE_AMP);
        m.put(KEY_PREFIX + "101", SelfStatsKind.DEFENCE_AMP);
        m.put(KEY_PREFIX + "17", SelfStatsKind.DEFENCE_RAW);
        m.put(KEY_PREFIX + "18", SelfStatsKind.TRUE_ARMOR_AMP);
        m.put(KEY_PREFIX + "104", SelfStatsKind.TRUE_ARMOR_AMP);
        m.put(KEY_PREFIX + "19", SelfStatsKind.TRUE_ARMOR_RAW);
        m.put(KEY_PREFIX + "20", SelfStatsKind.HEAL_AMP);
        m.put(KEY_PREFIX + "107", SelfStatsKind.HEAL_AMP);
        m.put(KEY_PREFIX + "21", SelfStatsKind.HEAL_RAW);
        m.put(KEY_PREFIX + "25", SelfStatsKind.FIRE_ARMOR_AMP);
        m.put(KEY_PREFIX + "115", SelfStatsKind.FIRE_ARMOR_AMP);
        m.put(KEY_PREFIX + "26", SelfStatsKind.FIRE_ARMOR_RAW);
        m.put(KEY_PREFIX + "33", SelfStatsKind.SPEED_AMP);
        m.put(KEY_PREFIX + "122", SelfStatsKind.SPEED_AMP);
        m.put(KEY_PREFIX + "34", SelfStatsKind.SPEED_RAW);
        m.put(KEY_PREFIX + "40", SelfStatsKind.MANA_AMP);
        m.put(KEY_PREFIX + "124", SelfStatsKind.MANA_AMP);
        m.put(KEY_PREFIX + "41", SelfStatsKind.MANA_POWER_RAW);
        return java.util.Map.copyOf(m);
    }

    /** hover 键 {@code view_stats_1..54} → 分类（与地图 view_stats.mcfunction 一一对应）。 */
    /**
     * 计分板目标名 → 分类。
     *
     * <p>地图 {@code view_stats.mcfunction} 的数值行都是 {@code ["",{score:{objective:...}},{icon,hover}]}：
     * <b>目标名一定在</b>，而 hover 键不一定（例如 {@code DarkKarma} 那两行就没有）。所以把
     * 目标名当成第二层身份：比「图标 + 颜色」稳，也不受语言文件影响。</p>
     *
     * <p>这张表按<b>当前地图</b>（CakeTeamPack 的 {@code misc/view_stats.mcfunction}）逐个目标名核对过；
     * 地图加了新属性而这里没跟上时，行不会消失，只会落到「未识别行」兜底 —— 再加一行即可。</p>
     */
    private static java.util.Map<String, SelfStatsKind> buildObjectiveKinds() {
        java.util.Map<String, SelfStatsKind> m = new java.util.HashMap<>();
        // 心
        m.put("RedHearts", SelfStatsKind.HEART_RED);
        m.put("SoulHearts", SelfStatsKind.HEART_SOUL);
        m.put("BlackHearts", SelfStatsKind.HEART_BLACK);
        m.put("BlueHearts", SelfStatsKind.HEART_BLUE);
        m.put("CrackedHearts", SelfStatsKind.HEART_CRACKED);
        m.put("PinkHearts", SelfStatsKind.HEART_PINK);
        m.put("NegMaxHealth", SelfStatsKind.HP_NEG);
        // 生命 / 治疗
        m.put("MaxHPAmplifier", SelfStatsKind.HP_AMP);
        m.put("MaxHPRaw", SelfStatsKind.HP_RAW);
        m.put("Healing", SelfStatsKind.HEAL_RAW);
        m.put("HealingAmplifier", SelfStatsKind.HEAL_AMP);
        m.put("HealPercent", SelfStatsKind.HEAL_PERCENT);
        m.put("ExtraHealing", SelfStatsKind.HEAL_EXTRA);
        m.put("TowersRegen", SelfStatsKind.TOWERS_REGEN);
        // 攻击
        m.put("ClassStat", SelfStatsKind.CLASS_STAT);
        m.put("ClassStatAmplifier", SelfStatsKind.CLASS_STAT_AMP);
        m.put("ClassStatRaw", SelfStatsKind.CLASS_STAT_RAW);
        m.put("Strength", SelfStatsKind.STRENGTH);
        m.put("StrengthAmplifier", SelfStatsKind.STRENGTH_AMP);
        m.put("StrengthRaw", SelfStatsKind.STRENGTH_RAW);
        m.put("AttackSpeed", SelfStatsKind.ATTACK_SPEED);
        m.put("AttackSpeedAmplifier", SelfStatsKind.ATTACK_SPEED_AMP);
        m.put("AttackSpeedRaw", SelfStatsKind.ATTACK_SPEED_RAW);
        m.put("AttackRange", SelfStatsKind.ATTACK_RANGE);
        m.put("StaminaSpeed", SelfStatsKind.STAMINA);
        m.put("Archery", SelfStatsKind.ARCHERY);
        // 防御 / 护甲
        m.put("Defence", SelfStatsKind.DEFENCE);
        m.put("DefenceAmplifier", SelfStatsKind.DEFENCE_AMP);
        m.put("DefenceRaw", SelfStatsKind.DEFENCE_RAW);
        m.put("TrueArmor", SelfStatsKind.TRUE_ARMOR);
        m.put("TrueArmorAmplifier", SelfStatsKind.TRUE_ARMOR_AMP);
        m.put("TrueArmorRaw", SelfStatsKind.TRUE_ARMOR_RAW);
        m.put("FireArmor", SelfStatsKind.FIRE_ARMOR);
        m.put("FireArmorAmplifier", SelfStatsKind.FIRE_ARMOR_AMP);
        m.put("FireArmorRaw", SelfStatsKind.FIRE_ARMOR_RAW);
        m.put("TrueFireArmor", SelfStatsKind.TRUE_FIRE_ARMOR);
        m.put("WaterArmor", SelfStatsKind.WATER_ARMOR);
        m.put("ElectricArmor", SelfStatsKind.ELECTRIC_ARMOR);
        m.put("IceArmor", SelfStatsKind.ICE_ARMOR);
        m.put("DarkArmor", SelfStatsKind.DARK_ARMOR);
        m.put("LightArmor", SelfStatsKind.LIGHT_ARMOR);
        // 速度 / 跳跃 / 法力
        m.put("MaxSpeed", SelfStatsKind.MAX_SPEED);
        m.put("SpeedAmplifier", SelfStatsKind.SPEED_AMP);
        m.put("SpeedRaw", SelfStatsKind.SPEED_RAW);
        m.put("Jump", SelfStatsKind.JUMP);
        m.put("DoubleJump", SelfStatsKind.DOUBLE_JUMP);
        m.put("ManaPower", SelfStatsKind.MANA_POWER);
        m.put("ManaPowerAmplifier", SelfStatsKind.MANA_AMP);
        m.put("ManaPowerRaw", SelfStatsKind.MANA_POWER_RAW);
        m.put("ManaRechargeSpeed", SelfStatsKind.MANA_RECHARGE);
        // 其它属性
        m.put("Size", SelfStatsKind.SIZE);
        m.put("Gravity", SelfStatsKind.GRAVITY);
        m.put("ActiveCharge", SelfStatsKind.ACTIVE_CHARGE);
        m.put("MaxActiveCharge", SelfStatsKind.MAX_CHARGE);
        m.put("ActiveSpeed", SelfStatsKind.ACTIVE_SPEED);
        m.put("Summoning", SelfStatsKind.SUMMONING);
        m.put("Fishing", SelfStatsKind.FISHING);
        m.put("BeePoints", SelfStatsKind.BEE);
        m.put("AntPoints", SelfStatsKind.ANT);
        m.put("Bitches", SelfStatsKind.BITCHES);
        m.put("LightKarma", SelfStatsKind.KARMA_LIGHT);
        m.put("NeutralKarma", SelfStatsKind.KARMA_NEUTRAL);
        m.put("DarkKarma", SelfStatsKind.KARMA_DARK);
        m.put("CelestialKarma", SelfStatsKind.KARMA_CELESTIAL);
        // 难度骷髅（#Skulls CTT）
        m.put("CTT", SelfStatsKind.SKULLS);
        return m;
    }

    /** 这一行里有没有地图属性用的图形（❤ / 私有区 / ✈ 🗡 那批）。 */
    static boolean hasStatGlyph(String flat) {
        if (flat == null || flat.isEmpty()) return false;
        for (int i = 0; i < flat.length(); ) {
            int cp = flat.codePointAt(i);
            i += Character.charCount(cp);
            if (isStatGlyphCp(cp)) return true;
        }
        return false;
    }

    private static java.util.Map<String, SelfStatsKind> buildHoverKinds() {
        java.util.Map<String, SelfStatsKind> m = new java.util.HashMap<>();
        m.put("view_stats_1", SelfStatsKind.HEART_RED);
        m.put("view_stats_2", SelfStatsKind.HEART_SOUL);
        m.put("view_stats_3", SelfStatsKind.HEART_BLACK);
        m.put("view_stats_4", SelfStatsKind.HEART_BLUE);
        m.put("view_stats_5", SelfStatsKind.HEART_CRACKED);
        m.put("view_stats_6", SelfStatsKind.HEART_PINK);
        m.put("view_stats_7", SelfStatsKind.HP_NEG);
        m.put("view_stats_8", SelfStatsKind.CLASS_STAT);
        m.put("view_stats_9", SelfStatsKind.CLASS_STAT_AMP);
        m.put("view_stats_10", SelfStatsKind.STRENGTH);
        m.put("view_stats_11", SelfStatsKind.STRENGTH_AMP);
        m.put("view_stats_12", SelfStatsKind.ATTACK_SPEED);
        m.put("view_stats_13", SelfStatsKind.ATTACK_SPEED_AMP);
        m.put("view_stats_14", SelfStatsKind.ATTACK_RANGE);
        m.put("view_stats_15", SelfStatsKind.STAMINA);
        m.put("view_stats_16", SelfStatsKind.ARCHERY);
        m.put("view_stats_17", SelfStatsKind.DEFENCE);
        m.put("view_stats_18", SelfStatsKind.DEFENCE_AMP);
        m.put("view_stats_19", SelfStatsKind.TRUE_ARMOR);
        m.put("view_stats_20", SelfStatsKind.TRUE_ARMOR);
        m.put("view_stats_21", SelfStatsKind.TRUE_ARMOR_AMP);
        m.put("view_stats_22", SelfStatsKind.HEAL_RAW);
        m.put("view_stats_23", SelfStatsKind.HEAL_AMP);
        m.put("view_stats_24", SelfStatsKind.TOWERS_REGEN);
        m.put("view_stats_25", SelfStatsKind.HEAL_PERCENT);
        m.put("view_stats_26", SelfStatsKind.HEAL_EXTRA);
        m.put("view_stats_27", SelfStatsKind.TRUE_FIRE_ARMOR);
        m.put("view_stats_28", SelfStatsKind.FIRE_ARMOR);
        m.put("view_stats_29", SelfStatsKind.FIRE_ARMOR_AMP);
        m.put("view_stats_30", SelfStatsKind.WATER_ARMOR);
        m.put("view_stats_31", SelfStatsKind.ELECTRIC_ARMOR);
        m.put("view_stats_32", SelfStatsKind.ICE_ARMOR);
        m.put("view_stats_33", SelfStatsKind.DARK_ARMOR);
        m.put("view_stats_34", SelfStatsKind.LIGHT_ARMOR);
        m.put("view_stats_35", SelfStatsKind.MAX_SPEED);
        m.put("view_stats_36", SelfStatsKind.SPEED_AMP);
        m.put("view_stats_37", SelfStatsKind.SIZE);
        m.put("view_stats_38", SelfStatsKind.GRAVITY);
        m.put("view_stats_39", SelfStatsKind.MANA_POWER);
        m.put("view_stats_40", SelfStatsKind.MANA_AMP);
        m.put("view_stats_41", SelfStatsKind.MANA_RECHARGE);
        m.put("view_stats_42", SelfStatsKind.JUMP);
        m.put("view_stats_43", SelfStatsKind.DOUBLE_JUMP);
        m.put("view_stats_44", SelfStatsKind.ACTIVE_CHARGE);
        m.put("view_stats_45", SelfStatsKind.MAX_CHARGE);
        m.put("view_stats_46", SelfStatsKind.ACTIVE_SPEED);
        m.put("view_stats_47", SelfStatsKind.SUMMONING);
        m.put("view_stats_48", SelfStatsKind.FISHING);
        m.put("view_stats_49", SelfStatsKind.BEE);
        m.put("view_stats_50", SelfStatsKind.ANT);
        m.put("view_stats_51", SelfStatsKind.BITCHES);
        m.put("view_stats_52", SelfStatsKind.KARMA_LIGHT);
        m.put("view_stats_53", SelfStatsKind.KARMA_NEUTRAL);
        m.put("view_stats_54", SelfStatsKind.KARMA_CELESTIAL);
        return java.util.Map.copyOf(m);
    }
}
