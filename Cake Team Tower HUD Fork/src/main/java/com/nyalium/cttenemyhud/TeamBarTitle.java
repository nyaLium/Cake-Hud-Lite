package com.nyalium.cttenemyhud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 队伍 Boss 条标题解析（纯函数，可穷举测试）。
 *
 * <p>数据包 {@code misc.mcfunction:1013} 把每个玩家槽位拼成交替的
 * {@code 名字 (当前血量/上限)}，整条渲染出来是：</p>
 * <pre>
 *   nyaLium (73/120) OnlyScoutCat (45/45) Kirin0321 (12/12)
 * </pre>
 * <p>与敌人条（{@code 名字 (HP 73/120) 🛡3}）的区别是括号里没有 {@code HP} 前缀，两者互不误判。</p>
 */
public final class TeamBarTitle {

    /** 一名队友的读数。 */
    public static final class Entry {
        public final String name;
        public final int hp;
        public final int maxHp;

        Entry(String name, int hp, int maxHp) {
            this.name = name;
            this.hp = hp;
            this.maxHp = maxHp;
        }

        public float ratio() {
            if (maxHp <= 0) return 0.0f;
            return Math.max(0.0f, Math.min(1.0f, (float) hp / (float) maxHp));
        }
    }

    private TeamBarTitle() {}

    /**
     * 宽松扫描：把整串里**所有** {@code 名字 (x/y)} 片段都抠出来，不管前后还有什么。
     *
     * <h2>为什么需要</h2>
     * <p>数据包 {@code spectator_bossbar} 的标题是按 10 个玩家槽位拼的
     * （{@code misc.mcfunction:1013}）：某个槽位没人时，{@code {"selector":"@a[...]"}} 与
     * {@code {"score":...}} 会渲染成空，于是整条标题里夹着 {@code " (/) "} 这类残渣，
     * {@link #parse} 的严格整串解析会直接失败 —— 结果就是「顶部队伍血量条该隐藏时没隐藏、时不时闪一下」。
     * 这里只要有一个完整片段能解析出来即可。</p>
     */
    public static List<Entry> scanLoose(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        int n = text.length();
        java.util.List<Entry> out = new ArrayList<>(4);
        int i = 0;
        while (i < n) {
            char open = text.charAt(i);
            if (open != '(' && open != '（') {
                i++;
                continue;
            }
            // ── 名字：左括号往前取一段非空白、非括号的字符 ──
            int nameEnd = i;
            int k = nameEnd - 1;
            while (k >= 0 && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) k--;
            int nameStart = k;
            while (nameStart >= 0) {
                char ch = text.charAt(nameStart);
                if (Character.isWhitespace(ch) || ch == '(' || ch == '（'
                        || ch == ')' || ch == '）') {
                    break;
                }
                nameStart--;
            }
            nameStart++;

            int j = i + 1;
            while (j < n && text.charAt(j) == ' ') j++;
            int hpStart = j;
            if (j < n && (text.charAt(j) == '-' || text.charAt(j) == '+')) j++;
            int digits = j;
            while (j < n && Character.isDigit(text.charAt(j))) j++;
            if (j == digits) {
                i++;
                continue;
            }
            int hp;
            try {
                hp = Integer.parseInt(text.substring(hpStart, j));
            } catch (NumberFormatException e) {
                i++;
                continue;
            }

            while (j < n && text.charAt(j) == ' ') j++;
            if (j >= n || text.charAt(j) != '/') {
                i++;
                continue;
            }
            j++;
            while (j < n && text.charAt(j) == ' ') j++;
            int maxStart = j;
            if (j < n && (text.charAt(j) == '-' || text.charAt(j) == '+')) j++;
            digits = j;
            while (j < n && Character.isDigit(text.charAt(j))) j++;
            if (j == digits) {
                i++;
                continue;
            }
            int maxHp;
            try {
                maxHp = Integer.parseInt(text.substring(maxStart, j));
            } catch (NumberFormatException e) {
                i++;
                continue;
            }

            while (j < n && text.charAt(j) == ' ') j++;
            if (j >= n) break;
            char close = text.charAt(j);
            if (close != ')' && close != '）') {
                i++;
                continue;
            }
            j++;

            if (maxHp > 0 && nameEnd > nameStart) {
                String name = text.substring(nameStart, nameEnd).trim();
                if (!name.isEmpty()) out.add(new Entry(name, hp, maxHp));
            }
            i = j;
        }
        return out;
    }

    /** 必须整串都由 {@code 名字 (x/y)} 组成才算队伍条，避免误判别的 bar。 */
    public static List<Entry> parse(String text) {
        if (text == null) return Collections.emptyList();
        String s = text;
        int n = s.length();
        int i = 0;
        List<Entry> out = new ArrayList<>(4);

        while (i < n) {
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n) break;

            int nameStart = i;
            while (i < n) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c) || c == '(' || c == '（' || c == ')' || c == '）') break;
                i++;
            }
            if (i == nameStart) return Collections.emptyList();
            String name = s.substring(nameStart, i);

            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n) return Collections.emptyList();
            char open = s.charAt(i);
            if (open != '(' && open != '（') return Collections.emptyList();
            i++;

            int hpStart = i;
            if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
            int digits = i;
            while (i < n && Character.isDigit(s.charAt(i))) i++;
            if (i == digits) return Collections.emptyList();
            int hp;
            try {
                hp = Integer.parseInt(s.substring(hpStart, i));
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }

            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n || s.charAt(i) != '/') return Collections.emptyList();
            i++;

            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            int maxStart = i;
            if (i < n && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
            digits = i;
            while (i < n && Character.isDigit(s.charAt(i))) i++;
            if (i == digits) return Collections.emptyList();
            int maxHp;
            try {
                maxHp = Integer.parseInt(s.substring(maxStart, i));
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }

            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n) return Collections.emptyList();
            char close = s.charAt(i);
            if (close != ')' && close != '）') return Collections.emptyList();
            i++;

            if (maxHp > 0 && !name.isEmpty()) {
                out.add(new Entry(name, hp, maxHp));
            }
        }
        return out;
    }
}
