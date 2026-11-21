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
