package com.nyalium.cttenemyhud;

/**
 * 自检：穷举配置选项与解析器边界，验证不会产生非法结果。
 *
 * <p>纯 JVM 运行（不启动游戏）：{@code gradlew selfCheck}。</p>
 */
public final class SelfCheck {

    private static int checks = 0;
    private static final java.util.List<String> failures = new java.util.ArrayList<>();

    public static void main(String[] args) {
        layoutCombinations();
        barWidthSweep();
        configNormalization();
        armorEdgeCases();
        bossBarTitles();
        curseKeys();
        teamBarTitles();

        System.out.println();
        System.out.println("checks=" + checks + " failures=" + failures.size());
        if (!failures.isEmpty()) {
            for (String f : failures) System.out.println("  FAIL " + f);
            System.out.println("SELF CHECK FAILED");
            System.exit(1);
        }
        System.out.println("SELF CHECK PASSED");
    }

    // ── 1. 显示开关 × 数据形态 ──

    private static void layoutCombinations() {
        Data[] datas = {
                new Data("普通", 73, 120, false, 3, new int[]{5, 2, 0, 0, 0, 0, 1}),
                new Data("仅百分比", 73, 100, true, 0, new int[]{0, 0, 0, 0, 0, 0, 0}),
                new Data("无护甲", 40, 40, false, 0, new int[]{0, 0, 0, 0, 0, 0, 0}),
                new Data("零血量", 0, 0, false, 0, new int[]{0, 0, 0, 0, 0, 0, 0}),
                new Data("满护甲", 999, 1000, false, 20, new int[]{7, 6, 5, 4, 3, 2, 1}),
                new Data("负值护甲", 5, 10, false, -3, new int[]{-1, 0, -5, 0, 0, 0, 0}),
        };

        for (int mask = 0; mask < 8; mask++) {
            for (Data d : datas) {
                EnemyHudLayout.Options o = new EnemyHudLayout.Options();
                o.showNumbers = (mask & 1) != 0;
                o.showArmor = (mask & 2) != 0;
                o.showBar = (mask & 4) != 0;

                String tag = "开关 mask=" + mask + " 数据=" + d.name;
                EnemyHudLayout.Result r = run(tag, d, o);

                boolean hasArmorData = EnemyHudLayout.hasArmor(d.defence, d.armor);
                boolean expectText = (o.showNumbers && !d.percentOnly) || (o.showArmor && hasArmorData);
                boolean expectEmpty = !expectText && !o.showBar;

                if (expectEmpty) {
                    check(r.isEmpty(), tag + " 应当为空");
                } else {
                    check(!r.isEmpty(), tag + " 不应当为空");
                }
                if (expectText) {
                    check(r.hasText, tag + " 应产出文字");
                } else {
                    check(!r.hasText, tag + " 不该产出文字");
                }

                if (!o.showBar) {
                    check(r.barWidth == 0, tag + " 关血条时 barWidth 必须为 0");
                } else {
                    check(r.barWidth >= EnemyHudLayout.BAR_WIDTH_MIN
                            && r.barWidth <= EnemyHudLayout.BAR_WIDTH_MAX, tag + " barWidth 越界");
                }

                int sum = 0;
                for (EnemyHudLayout.Line l : r.lines) sum += width(l.text);
                check(sum == r.textWidth, tag + " textWidth 不等于分段之和");

                for (EnemyHudLayout.Line l : r.lines) {
                    check(!l.text.isEmpty(), tag + " 出现空文字段");
                    check((l.rgb & 0xFF000000) == 0, tag + " 颜色含高位");
                }

                check(r.barTop <= -10, tag + " 血条压到名字行");
                check(r.textTop <= -10, tag + " 文字压到名字行");
                check(r.textTop + 9 <= r.barTop, tag + " 文字与血条重叠");
            }
        }
    }

    // ── 2. 血条长度 ──

    private static void barWidthSweep() {
        double[] configs = {0.0, 16.0, 50.0, 100.0, 150.0, 200.0, 1000.0, -5.0, Double.NaN};
        int[] offsets = {-30, -15, 0, 200};
        double[] scales = {0.5, 0.75, 1.0, 1.5, 4.0};
        Data d = new Data("扫描", 73, 120, false, 3, new int[]{5, 2, 0, 0, 0, 0, 1});

        for (double bw : configs) {
            for (int off : offsets) {
                for (double sc : scales) {
                    EnemyHudLayout.Options o = new EnemyHudLayout.Options();
                    o.barWidth = bw;
                    o.verticalOffset = off;
                    o.scale = sc;
                    String tag = "barWidth=" + bw + " offset=" + off + " scale=" + sc;
                    EnemyHudLayout.Result r = run(tag, d, o);
                    check(r.barWidth >= EnemyHudLayout.BAR_WIDTH_MIN
                            && r.barWidth <= EnemyHudLayout.BAR_WIDTH_MAX, tag + " barWidth 越界");
                    check(r.textTop == EnemyHudLayout.TEXT_TOP + off, tag + " textTop 偏移错误");
                    check(r.barTop == EnemyHudLayout.BAR_TOP + off, tag + " barTop 偏移错误");
                    check(r.textTop + 9 <= r.barTop, tag + " 文字与血条重叠");
                }
            }
        }

        check(EnemyHudLayout.resolveBarWidth(100) == 100, "默认 100 应保持");
        check(EnemyHudLayout.resolveBarWidth(50) == 50, "下限 50 应保持");
        check(EnemyHudLayout.resolveBarWidth(150) == 150, "上限 150 应保持");
        check(EnemyHudLayout.resolveBarWidth(0) == EnemyHudLayout.BAR_WIDTH_MIN, "0 应夹到下限");
        check(EnemyHudLayout.resolveBarWidth(9999) == EnemyHudLayout.BAR_WIDTH_MAX, "过大应夹到上限");
        check(EnemyHudLayout.resolveBarWidth(Double.NaN) == EnemyHudLayout.BAR_WIDTH_DEFAULT, "NaN 回落默认");
    }

    // ── 3. 配置归一化 ──

    private static void configNormalization() {
        EnemyHudConfig.Cfg c = new EnemyHudConfig.Cfg();
        c.opacity = 0.1;
        c.scale = 0.01;
        c.maxDistance = 0;
        c.barWidth = -100;
        c.verticalOffset = 9999;
        EnemyHudConfig.normalize(c);
        check(c.opacity == 0.5, "opacity 0.1 应夹到 0.5");
        check(c.scale == 0.75, "scale 0.01 应夹到 0.75");
        check(c.maxDistance == 4.0, "maxDistance 0 应夹到 4");
        check(c.barWidth == EnemyHudLayout.BAR_WIDTH_MIN, "barWidth -100 应夹到 50");
        check(c.verticalOffset == 0, "verticalOffset 9999 应夹到 0");

        c.opacity = 3.0;
        c.scale = 99;
        c.maxDistance = 9999;
        c.barWidth = 9999;
        c.verticalOffset = -9999;
        EnemyHudConfig.normalize(c);
        check(c.opacity == 1.0, "opacity 3 应夹到 1.0");
        check(c.scale == 1.5, "scale 99 应夹到 1.5");
        check(c.maxDistance == 256.0, "maxDistance 9999 应夹到 256");
        check(c.barWidth == EnemyHudLayout.BAR_WIDTH_MAX, "barWidth 9999 应夹到 150");
        check(c.verticalOffset == -30, "verticalOffset -9999 应夹到 -30");

        c.opacity = Double.NaN;
        c.barWidth = Double.NaN;
        EnemyHudConfig.normalize(c);
        check(c.opacity == 1.0, "opacity NaN 应回落 1.0");
        check(c.barWidth == EnemyHudLayout.BAR_WIDTH_DEFAULT, "barWidth NaN 应回落 100");

        EnemyHudConfig.Cfg defaults = EnemyHudConfig.normalize(new EnemyHudConfig.Cfg());
        check(defaults.barWidth == 100.0, "默认血条长度应为 100");
        check(defaults.scale == 1.0, "默认缩放应为 1.0");
        check(defaults.verticalOffset == 0, "默认垂直偏移应为 0");
        check(defaults.opacity == 0.75, "默认透明度应为 0.75");
        check(defaults.showBar && defaults.showNumbers && defaults.showArmor, "默认三个显示项应全开");
        check(defaults.hideEnemyBossBar, "默认应隐藏顶部敌人Boss条");
        check(defaults.showTeammates, "默认应显示队友血量");
        check(defaults.autoPartyBossbar, "默认应自动开启队伍条");

        check(EnemyHudConfig.normalize(null) != null, "normalize(null) 不能返回 null");

        for (double op = 0.4; op <= 1.2; op += 0.05) {
            EnemyHudConfig.Cfg t = new EnemyHudConfig.Cfg();
            t.opacity = op;
            EnemyHudConfig.normalize(t);
            int alpha = (int) Math.round(255.0 * t.opacity);
            check(alpha >= 128 && alpha <= 255, "opacity=" + op + " 映射出 alpha=" + alpha);
        }
    }

    // ── 4. 护甲边界 ──

    private static void armorEdgeCases() {
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showNumbers = false;
        o.showArmor = true;
        o.barWidth = 64;

        run("超长护甲数组", new Data("超长", 1, 1, false, 0,
                new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}), o);
        run("空护甲数组", new Data("空", 1, 1, false, 0, new int[0]), o);
        run("null 护甲数组", new Data("null", 1, 1, false, 0, null), o);
        EnemyHudLayout.Result r2 = run("只有防御", new Data("防御", 1, 1, false, 7,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        check(r2.hasText, "只有防御时也应产出文字");
    }

    // ── 5. Boss 栏标题解析 ──

    private static void bossBarTitles() {
        expectTitle("Frost Wolf (HP 73/120) 🛡3 🔥5", "Frost Wolf", 73, 120);
        expectTitle("Skeleton Warrior (HP 100/100) 🛡0", "Skeleton Warrior", 100, 100);
        expectTitle("冰霜狼 （生命值 73/120） 🛡3", "冰霜狼", 73, 120);
        expectTitle("骷髅战士 (健康值 5/10) 🛡1", null, -1, -1);
        expectTitle("Mob (Health 42/80)", "Mob", 42, 80);
        expectTitle("带空格的名字  (HP 1/2)", "带空格的名字", 1, 2);
        expectTitle("负血 (HP -3/100) 🛡0", "负血", -3, 100);

        expectTitle(null, null, -1, -1);
        expectTitle("", null, -1, -1);
        expectTitle("普通文本没有血量", null, -1, -1);
        expectTitle("零上限 (HP 5/0)", null, -1, -1);
        expectTitle("(HP 5/10)", null, -1, -1);
        expectTitle("Player (Coins 12) (HP 5/10)", null, -1, -1);

        BossBarTitle t = BossBarTitle.parse("Frost Wolf (HP 73/120) 🛡3 🔥5");
        check(t != null && t.suffix("Frost Wolf (HP 73/120) 🛡3 🔥5").trim().equals("🛡3 🔥5"),
                "护甲段切分错误");
        check(t != null && Math.abs(t.ratio() - 73f / 120f) < 1e-6, "ratio 计算错误");

        check(BossBarTitle.looksLikeTeamBar("Someone (/)"), "队伍条标记未识别");
        check(BossBarTitle.looksLikeTeamBar("Someone （/）"), "全角队伍条标记未识别");
        check(!BossBarTitle.looksLikeTeamBar("Frost Wolf (HP 73/120)"), "普通条被误判为队伍条");
    }

    private static void expectTitle(String text, String name, int hp, int maxHp) {
        BossBarTitle t = BossBarTitle.parse(text);
        String label = "bossbar=\"" + text + "\"";
        if (name == null) {
            check(t == null, label + " 应当解析失败");
            return;
        }
        if (t == null) {
            check(false, label + " 应当解析成功");
            return;
        }
        check(name.equals(t.name), label + " 名字=" + t.name);
        check(t.hp == hp, label + " hp=" + t.hp);
        check(t.maxHp == maxHp, label + " maxHp=" + t.maxHp);
    }

    // ── 6. 诅咒识别锚点 ──

    private static void curseKeys() {
        check(CurseDetector.isCurseKey("Floors_Curse_Rooms_D_Curse02_Unknown_14"), "诅咒键应命中");
        check(!CurseDetector.isCurseKey("Misc_Bossbars_P10_Bossbar_1"), "普通键不应命中");
        check(!CurseDetector.isCurseKey(null), "null 键不应命中");
        check(!CurseDetector.isCurseKey(""), "空键不应命中");
    }

    // ── 7. 队伍条（队友血量） ──

    private static void teamBarTitles() {
        java.util.List<TeamBarTitle.Entry> one = TeamBarTitle.parse("nyaLium (73/120)");
        check(one.size() == 1, "单条队伍条目解析失败");
        if (one.size() == 1) {
            check("nyaLium".equals(one.get(0).name), "队伍名字=" + one.get(0).name);
            check(one.get(0).hp == 73 && one.get(0).maxHp == 120, "队伍血量解析错误");
            check(Math.abs(one.get(0).ratio() - 73f / 120f) < 1e-6, "队伍 ratio 错误");
        }

        java.util.List<TeamBarTitle.Entry> many = TeamBarTitle.parse("A (10/20) B (30/40) C (5/5)");
        check(many.size() == 3, "多条队伍条目数量错误: " + many.size());

        check(TeamBarTitle.parse("nyaLium (73/120) ").size() == 1, "尾随空格应仍能解析");
        check(TeamBarTitle.parse("A(1/2)B(3/4)").size() == 2, "无空格相邻应能解析");
        check(TeamBarTitle.parse("A （1/2）").size() == 1, "全角括号应能解析");

        check(TeamBarTitle.parse("Frost Wolf (HP 73/120)").isEmpty(), "敌人条不能当队伍条");
        check(TeamBarTitle.parse("Frost Wolf (HP 73/120) 🛡3").isEmpty(), "带护甲的敌人条不能当队伍条");
        check(TeamBarTitle.parse("").isEmpty(), "空串应解析为空");
        check(TeamBarTitle.parse(null).isEmpty(), "null 应解析为空");
        check(TeamBarTitle.parse("Someone (/)").isEmpty(), "队伍标记条不应解析出玩家");
        check(TeamBarTitle.parse("普通文本").isEmpty(), "普通文本不应解析出玩家");
        check(TeamBarTitle.parse("A (10/0)").isEmpty(), "上限为 0 的条目应丢弃");
    }

    // ── 基础设施 ──

    private static EnemyHudLayout.Result run(String tag, Data d, EnemyHudLayout.Options o) {
        try {
            EnemyHudLayout.Result r = EnemyHudLayout.compute(
                    d.hp, d.maxHp, d.percentOnly, d.defence, d.armor, o, SelfCheck::width);
            if (r == null) {
                fail(tag + " 返回 null");
                return null;
            }
            return r;
        } catch (Throwable t) {
            fail(tag + " 抛异常 " + t);
            return null;
        }
    }

    private static int width(String s) {
        if (s == null) return 0;
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += cp < 0x2000 ? 6 : 9;
            i += Character.charCount(cp);
        }
        return w;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) fail(message);
    }

    private static void fail(String message) {
        failures.add(message);
    }

    private static final class Data {
        final String name;
        final int hp;
        final int maxHp;
        final boolean percentOnly;
        final int defence;
        final int[] armor;

        Data(String name, int hp, int maxHp, boolean percentOnly, int defence, int[] armor) {
            this.name = name;
            this.hp = hp;
            this.maxHp = maxHp;
            this.percentOnly = percentOnly;
            this.defence = defence;
            this.armor = armor;
        }
    }
}
