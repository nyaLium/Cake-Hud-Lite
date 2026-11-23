package com.nyalium.ctthudfork;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Boss 栏标题解析（纯函数，不引用任何 Minecraft 类，便于穷举测试）。
 *
 * <p>数据包 {@code bossbars/targetting_enemy.mcfunction} 拼出来的标题形如：</p>
 * <pre>
 *   Frost Wolf (HP 73/120) 🛡3 🔥5
 *   冰霜狼 （生命值 73/120） 🛡3
 * </pre>
 * <p>其中 {@code " (HP "} 来自 lang 键 {@code Misc_Bossbars_P10_Bossbar_1}
 * （en_us 实测值是 {@code " (HP "}，另有中文/英文两种写法），
 * 后面紧跟 {@code AllHearts}、{@code "/"}、{@code MaxHP}、{@code ") "}，
 * 再往后是护甲段。</p>
 *
 * <p>所以：名字 = 括号前的内容，血量 = 括号里的 {@code 当前/上限}，
 * 护甲段 = 右括号之后的全部内容。</p>
 */
public final class BossBarTitle {

    /** 兼容 HP / 生命值 / Health，以及半角与全角括号。 */
    private static final Pattern HP = Pattern.compile(
            "[（(]\\s*(?:HP|生命值|Health)\\s*(-?\\d+)\\s*/\\s*(-?\\d+)\\s*[）)]");

    /** 玩家自己 / 队友的 bossbar 用 " (/)" 之类标记，遇到就不当作怪物条。 */
    private static final Pattern TEAM_MARK = Pattern.compile("[（(]\\s*/\\s*[）)]");

    public final String name;
    public final int hp;
    public final int maxHp;

    private BossBarTitle(String name, int hp, int maxHp) {
        this.name = name;
        this.hp = hp;
        this.maxHp = maxHp;
    }

    /** 解析失败返回 null（这条 bossbar 不是怪物血量条）。 */
    public static BossBarTitle parse(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;
        Matcher m = HP.matcher(text);
        if (!m.find()) return null;

        String name = text.substring(0, m.start()).trim();
        if (name.isEmpty()) return null;
        // 名字里不该再出现括号，出现了说明这是别的东西（例如玩家状态条）。
        if (name.indexOf('(') >= 0 || name.indexOf('（') >= 0) return null;

        int hp;
        int maxHp;
        try {
            hp = Integer.parseInt(m.group(1));
            maxHp = Integer.parseInt(m.group(2));
        } catch (NumberFormatException e) {
            return null;
        }
        if (maxHp <= 0) return null;

        return new BossBarTitle(name, hp, maxHp);
    }

    /** 是否像玩家 / 队伍状态条（不是要画的怪物条）。 */
    public static boolean looksLikeTeamBar(String text) {
        return text != null && TEAM_MARK.matcher(text).find();
    }

    /**
     * 这条 bossbar 是不是「数据包给最近敌人做的那条血量条」。
     *
     * <p>用于两处：① 解析出精确血量；② 按配置把这条 bar 从屏幕上隐藏掉。
     * 判据必须同时满足「能解析成怪物血量条」且「不是玩家自己的条」。</p>
     *
     * @param playerName 本地玩家名；玩家条的标题里一定含它，据此排除
     */
    public static boolean isEnemyBarText(String text, String playerName) {
        if (text == null) return false;
        if (looksLikeTeamBar(text)) return false;
        if (playerName != null && !playerName.isEmpty() && text.contains(playerName)) return false;
        return parse(text) != null;
    }

    /** 血量比例 0..1。 */
    public float ratio() {
        return Math.max(0.0f, Math.min(1.0f, (float) hp / (float) maxHp));
    }
}
