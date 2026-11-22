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
        heartCountsAndRows();
        armourPaging();
        bigNumbers();
        barOwnershipScoring();
        screenOcclusion();
        payloadConversion();
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

                check(r.barTop + EnemyHudLayout.BAR_HEIGHT <= EnemyHudLayout.NAME_TOP,
                        tag + " 血条压到名称行");
                check(r.textTop < EnemyHudLayout.NAME_TOP, tag + " 文字压到名称行");
                check(r.textTop + 9 <= r.barTop, tag + " 文字与血条重叠");

                // 行距必须 ≥ 原版文字背景框高度（y-1 ~ y+9 = 10 像素）；用 9 会让
                // 上行的底边与下行的顶边重叠 1 像素，两层半透明黑叠成一道更深的暗带。
                check(EnemyHudLayout.ROW_STEP >= EnemyHudLayout.TEXT_BOX_HEIGHT,
                        tag + " ROW_STEP 小于背景框高度");
                for (int i = 1; i < r.rows.size(); i++) {
                    check(r.rows.get(i - 1).top + EnemyHudLayout.TEXT_BOX_HEIGHT <= r.rows.get(i).top,
                            tag + " 第 " + i + " 行与上一行的背景框重叠");
                }
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

        check(EnemyHudLayout.resolveBarWidth(100) == 100, "100 在范围内应保持");
        check(EnemyHudLayout.resolveBarWidth(50) == 50, "下限 50 应保持");
        check(EnemyHudLayout.resolveBarWidth(150) == 150, "上限 150 应保持");
        check(EnemyHudLayout.resolveBarWidth(0) == EnemyHudLayout.BAR_WIDTH_MIN, "0 应夹到下限");
        check(EnemyHudLayout.resolveBarWidth(9999) == EnemyHudLayout.BAR_WIDTH_MAX, "过大应夹到上限");
        check(EnemyHudLayout.resolveBarWidth(Double.NaN) == EnemyHudLayout.BAR_WIDTH_DEFAULT, "NaN 回落默认");
        check(EnemyHudLayout.BAR_WIDTH_DEFAULT == 75, "默认长度应为 75 像素: "
                + EnemyHudLayout.BAR_WIDTH_DEFAULT);
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
        check(defaults.barWidth == 75.0, "默认血条长度应为 75");
        check(defaults.teamBarWidth == 75.0, "队友血条默认长度也应为 75");
        check(defaults.scale == 1.0, "默认缩放应为 1.0");
        check(defaults.verticalOffset == 0, "默认垂直偏移应为 0");
        check(defaults.opacity == 0.75, "默认透明度应为 0.75");
        check(defaults.showBar, "默认应画血条");
        check(defaults.hideBehindWalls, "默认应忽略被墙挡住的怪");
        check(defaults.hideBehindEntities, "默认应忽略被其它实体挡住的怪");
        check(defaults.hideBelowNameScore, "默认应隐藏 below_name 分数");
        check(defaults.hideEnemyBossBar, "默认应隐藏上方 Boss 栏里的最近敌人信息");
        check(defaults.hideTeamBossBar, "默认应隐藏上方 Boss 栏里的队伍血量");
        check(defaults.showTeammates, "默认应显示队友血量");
        check(defaults.autoPartyBossbar, "默认应自动开启队伍血量条（纯客户端数据源）");

        check(EnemyHudConfig.normalize(null) != null, "normalize(null) 不能返回 null");

        // 老配置（没有 configVersion）迁移：旧默认 100 → 75，其它字段不动。
        EnemyHudConfig.Cfg old = new EnemyHudConfig.Cfg();
        old.barWidth = 100.0;
        old.teamBarWidth = 100.0;
        old.scale = 1.2;
        EnemyHudConfig.migrate("{\"barWidth\":100.0,\"scale\":1.2}", old);
        check(old.barWidth == 75.0, "旧默认 100 应迁移到 75: " + old.barWidth);
        check(old.teamBarWidth == 75.0, "队友旧默认 100 也应迁移到 75: " + old.teamBarWidth);
        check(old.scale == 1.2, "迁移不该动其它字段: " + old.scale);
        check(old.configVersion == EnemyHudConfig.CURRENT_VERSION, "迁移后应写上 configVersion");

        EnemyHudConfig.Cfg custom = new EnemyHudConfig.Cfg();
        custom.barWidth = 120.0;
        custom.teamBarWidth = 60.0;
        EnemyHudConfig.migrate("{\"barWidth\":120.0,\"teamBarWidth\":60.0}", custom);
        check(custom.barWidth == 120.0 && custom.teamBarWidth == 60.0,
                "自己调过的长度不该被迁移覆盖: " + custom.barWidth + "/" + custom.teamBarWidth);

        EnemyHudConfig.Cfg current = new EnemyHudConfig.Cfg();
        current.barWidth = 100.0;
        EnemyHudConfig.migrate("{\"configVersion\":2,\"barWidth\":100.0}", current);
        check(current.barWidth == 100.0, "新配置里显式写 100 不该被改: " + current.barWidth);

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

    // ── 5. 心数行 ──

    private static void heartCountsAndRows() {
        // 敌人：红心上限 MaxHP，多出来的都是魂心。
        HeartCounts mob = HeartCounts.forMob(160, 100);
        check(mob.get(HeartCounts.RED) == 100, "敌人红心应被 MaxHP 夹住: " + mob.get(HeartCounts.RED));
        check(mob.get(HeartCounts.SOUL) == 60, "敌人魂心应是溢出部分: " + mob.get(HeartCounts.SOUL));
        check(mob.any(), "有血时应能画");
        check(!mob.has(HeartCounts.BLACK) && !mob.has(HeartCounts.BLUE), "敌人不该有黑/蓝心");
        check(!mob.has(HeartCounts.CRACKED), "碎心读不到，不该显示");
        check(!mob.has(HeartCounts.PINK), "粉心读不到，不该显示");

        HeartCounts full = HeartCounts.forMob(100, 100);
        check(full.get(HeartCounts.RED) == 100 && full.get(HeartCounts.SOUL) == 0, "满红心无魂心");
        check(!full.has(HeartCounts.SOUL), "魂心 0 不算存在");

        HeartCounts hurt = HeartCounts.forMob(40, 100);
        check(hurt.get(HeartCounts.RED) == 40 && hurt.get(HeartCounts.SOUL) == 0, "红心受击后无魂心");

        HeartCounts dead = HeartCounts.forMob(0, 0);
        check(!dead.any(), "0/0 不该画心数");
        HeartCounts negative = HeartCounts.forMob(-7, 100);
        check(negative.get(HeartCounts.RED) == 0, "负血量夹到 0");
        check(!HeartCounts.unknown().any(), "未知不该画心数");
        check(HeartCounts.unknown().get(HeartCounts.RED) == HeartCounts.UNKNOWN, "未知哨兵值");
        check(HeartCounts.unknown().get(99) == HeartCounts.UNKNOWN, "越界下标应安全");
        check(HeartCounts.forMob(160, 100).knownSum() == 160, "已知心数之和应等于 AllHearts");

        // 文字行：红心是「当前/上限」，其余心只隔一个空格，同一行内不插分隔符。
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showArmor = false;
        o.showBar = false;
        EnemyHudLayout.Result r = run("心数行", new Data("两色", 160, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        String joined = join(r);
        check(joined.equals("❤100/100 💛60"), "心数行内容=" + joined);
        check(!joined.contains("|"), "同一条心数行不该有分隔符=" + joined);
        check(r.rowCount() == 1, "没有护甲时只有一行心数: " + r.rowCount());

        EnemyHudLayout.Result single = run("单色", new Data("单红", 80, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        check(join(single).equals("❤80/100"), "红心应写成 当前/上限: " + join(single));

        // 上限未知（MaxHP<=0）时只写当前值。
        EnemyHudLayout.Result noCap = EnemyHudLayout.compute(70, 0, false, 0, new int[7],
                HeartCounts.of(70, 0, 0, 0, 0), o, SelfCheck::width);
        check(join(noCap).equals("❤70"), "上限未知时不该写 /0: " + join(noCap));

        // 碎心 / 粉心是同一 objective 的两面，画出来互斥、各用各的颜色。
        EnemyHudLayout.Result crackedLine = EnemyHudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(50, 0, 0, 0, 5), o, SelfCheck::width);
        check(join(crackedLine).equals("❤50/50 💔5"), "碎心行=" + join(crackedLine));
        EnemyHudLayout.Result pinkLine = EnemyHudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(50, 0, 0, 0, -5), o, SelfCheck::width);
        check(join(pinkLine).equals("❤50/50 💗5"), "粉心行=" + join(pinkLine));
        check(HeartCounts.COLORS[HeartCounts.CRACKED] == 0xAA0000, "碎心配色应为 dark_red");
        check(HeartCounts.COLORS[HeartCounts.PINK] == 0xFF55FF, "粉心配色应为 light_purple");

        // 拆不出心数（percentOnly / 未知）→ 退回「当前/最大」。
        EnemyHudLayout.Result fallback = run("回退", new Data("只有百分比", 73, 100, true, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        check(!fallback.hasText, "只有百分比且无心数时不该有文字: " + join(fallback));

        EnemyHudLayout.Options noHearts = new EnemyHudLayout.Options();
        noHearts.showHearts = false;
        noHearts.showArmor = false;
        noHearts.showBar = false;
        EnemyHudLayout.Result numbers = run("关掉心数", new Data("普通", 160, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), noHearts);
        check(join(numbers).equals("160/100"), "关掉心数应退回绝对血量: " + join(numbers));

        // 1.18.0 起固定两行：护甲在最上面，心数紧贴血条；行数不随内容宽度变化。
        EnemyHudLayout.Options withArmor = new EnemyHudLayout.Options();
        withArmor.showBar = false;
        EnemyHudLayout.Result narrowArmor = run("窄护甲", new Data("一个护甲", 100, 100, false, 0,
                new int[]{3, 0, 0, 0, 0, 0, 0}), withArmor);
        check(narrowArmor.rowCount() == 2, "有护甲时固定两行: " + narrowArmor.rowCount());
        check(rowText(narrowArmor, 0).equals("\uD83D\uDD253"), "护甲行在最上面: " + rowText(narrowArmor, 0));
        check(rowText(narrowArmor, 1).equals("❤100/100"), "心数行在护甲下面: " + rowText(narrowArmor, 1));
        check(!join(narrowArmor).contains("|"), "分成两行之后不再需要分隔符: " + join(narrowArmor));

        EnemyHudLayout.Result armored = run("心数+护甲", new Data("带护甲", 160, 100, false, 3,
                new int[]{0, 5, 0, 0, 0, 0, 0}), withArmor);
        check(armored.rowCount() == 2, "有护甲时固定两行: " + armored.rowCount());
        check(rowText(armored, 0).equals("\uD83D\uDEE13 " + EnemyData.ARMOR_ICONS[1] + "5"),
                "第一行应是护甲: " + rowText(armored, 0));
        check(rowText(armored, 1).equals("❤100/100 💛60"),
                "第二行应是全部心数: " + rowText(armored, 1));
        check(armored.rows.get(0).top == EnemyHudLayout.TEXT_TOP - EnemyHudLayout.ROW_STEP,
                "护甲行要再抬 " + EnemyHudLayout.ROW_STEP + " 像素");
        check(armored.rows.get(1).top == EnemyHudLayout.TEXT_TOP, "心数行贴着 TEXT_TOP（紧贴血条）");
        check(armored.textTop == EnemyHudLayout.TEXT_TOP, "textTop 仍指最底一行");
        check(armored.rows.get(0).isArmourRow(), "第一行应被标记为护甲行");
        check(!armored.rows.get(1).isArmourRow(), "第二行不是护甲行");
        // 行距正好等于背景框高度：两行背景相邻但**不重叠**（原版 y-1 ~ y+9 共 10 像素）。
        check(EnemyHudLayout.TEXT_BOX_HEIGHT == 10, "背景框高度应为 10 像素");
        check(armored.rows.get(0).top + EnemyHudLayout.TEXT_BOX_HEIGHT == armored.rows.get(1).top,
                "两行背景框应正好相邻，不留缝也不重叠");

        // 心数再长也只有一行（不折行 → 行数稳定）。五色心 + 4 位数，以前会折两行。
        EnemyHudLayout.Result wideHearts = EnemyHudLayout.compute(9999, 9999, false, 0, new int[7],
                HeartCounts.of(9999, 9999, 9999, 9999, 9999), o, SelfCheck::width);
        check(wideHearts.rowCount() == 1, "心数再长也不折行: " + wideHearts.rowCount());
        check(!join(wideHearts).contains("|"), "心数行不该有分隔符: " + join(wideHearts));

        // 队友（玩家）也能有心数：有 payload 时和敌人一样画心，没 payload 时退回「当前/最大」。
        EnemyHudLayout.Options teamLike = new EnemyHudLayout.Options();
        teamLike.showHearts = true;
        teamLike.showArmor = true;
        teamLike.showBar = false;

        EnemyHudLayout.Result teamNoHearts = EnemyHudLayout.compute(73, 120, false, 0, new int[7],
                HeartCounts.unknown(), teamLike, SelfCheck::width);
        check(join(teamNoHearts).equals("73/120"),
                "队友拿不到心数时应退回当前/最大: " + join(teamNoHearts));

        EnemyHudLayout.Result teamHearts = EnemyHudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), teamLike, SelfCheck::width);
        check(teamHearts.rowCount() == 1, "队友五色心也是一行: " + teamHearts.rowCount());
        check(rowText(teamHearts, 0).equals("❤60/150 💛40 🖤30 💙20 💔2"),
                "队友心数行=" + rowText(teamHearts, 0));

        // 1.16.3 起：队友**只画心与血条**，护甲段一律不画 —— 即使 payload 带了 defence / 元素护甲。
        EnemyHudLayout.Options teamNoArmor = new EnemyHudLayout.Options();
        teamNoArmor.showHearts = true;
        teamNoArmor.showArmor = false;
        teamNoArmor.showBar = true;
        teamNoArmor.barWidth = 100;

        EnemyHudLayout.Result teamArmored = EnemyHudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                teamNoArmor, SelfCheck::width);
        String teamFlat = join(teamArmored);
        check(!teamFlat.contains("\uD83D\uDEE1"), "队友不该出现 🛡 护甲段: " + teamFlat);
        check(!teamFlat.contains(EnemyData.ARMOR_ICONS[1]), "队友不该出现 🔥 元素护甲段: " + teamFlat);
        check(!teamFlat.contains("|"), "队友不该出现护甲分隔符: " + teamFlat);
        // 折成两行时 join() 会把两行首尾相接，所以去掉空格再比。
        check(teamFlat.replace(" ", "").equals("\u276460/150\uD83D\uDC9B40\uD83D\uDDA430\uD83D\uDC9920\uD83D\uDC942"),
                "队友这一行只应有心: " + teamFlat);
        check(teamArmored.barWidth == 100, "队友的血条仍要画: " + teamArmored.barWidth);

        // 同样的数据给敌人，护甲段照旧要在（确认只改了队友那一套选项）。
        EnemyHudLayout.Options enemyArmor = new EnemyHudLayout.Options();
        enemyArmor.showHearts = true;
        enemyArmor.showArmor = true;
        enemyArmor.showBar = false;
        enemyArmor.barWidth = 100;
        EnemyHudLayout.Result mobArmored = EnemyHudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                enemyArmor, SelfCheck::width);
        check(join(mobArmored).contains("\uD83D\uDEE1"), "敌人的防御段不该被误伤: " + join(mobArmored));
        // 用转义写死期望值：🛡 在源码里常被写成「🛡+U+FE0F」，直接比字面量会踩变体选择符。
        check(join(armored).contains("\uD83D\uDEE13"), "防御段缺失: " + join(armored));
        check(join(armored).contains(EnemyData.ARMOR_ICONS[1] + "5"), "元素护甲段缺失: " + join(armored));
    }

    // ── 4b. 元素护甲翻页（1.17.0） ──

    private static void armourPaging() {
        // 🔥3 ☃1 ☠2 ☀4 ⚓6 = 5 项 → 2 + 2 + 1 三页
        int[] five = {3, 1, 2, 4, 0, 6, 0};
        check(EnemyHudLayout.armourPageCount(five) == 3,
                "5 种护甲应分 3 页: " + EnemyHudLayout.armourPageCount(five));
        check(EnemyHudLayout.armourPageCount(new int[]{5, 0, 0, 0, 0, 0, 0}) == 1, "1 种护甲应 1 页");
        check(EnemyHudLayout.armourPageCount(new int[7]) == 0, "没有护甲应 0 页");
        check(EnemyHudLayout.armourPageCount(null) == 0, "护甲为 null 应 0 页");
        check(EnemyHudLayout.clampArmourPage(-3, 3) == 0, "页号过小应夹到 0");
        check(EnemyHudLayout.clampArmourPage(9, 3) == 2, "页号过大应夹到最后一页");
        check(EnemyHudLayout.clampArmourPage(1, 0) == 0, "没有页时返回 0");

        // 翻页时钟：2 s 一页，周而复始；salt 用来错开。
        long ms = ArmourCycle.PAGE_MS;
        check(ArmourCycle.page(five, 0, 0) == 0, "t=0 应在第 0 页");
        check(ArmourCycle.page(five, ms, 0) == 1, "t=1 页应在第 1 页");
        check(ArmourCycle.page(five, ms * 2, 0) == 2, "t=2 页应在第 2 页");
        check(ArmourCycle.page(five, ms * 3, 0) == 0, "t=3 页应回到第 0 页");
        check(ArmourCycle.page(five, ms, 1) == 2, "salt=1 应错开一页: " + ArmourCycle.page(five, ms, 1));
        check(ArmourCycle.page(five, ms, 2) == 0, "salt=2 应错开两页");
        check(ArmourCycle.page(new int[]{1, 0, 0, 0, 0, 0, 0}, ms * 5, 3) == 0,
                "只有一页时不该翻页");
        check(ArmourCycle.page(null, ms * 5, 3) == 0, "没有护甲时页号为 0");

        // 布局：🛡 常驻，元素护甲每页最多 2 项。
        String[] pageArmour = {
                "\uD83D\uDD253 \u26031",              // 🔥3 ☃1
                "\u26202 \u26004",                    // ☠2 ☀4
                "\u26936",                            // ⚓6
        };
        String[] otherPages = {
                "\u26202", "\u26004", "\u26936"       // 不该出现在第 0 页的
        };
        for (int page = 0; page < 3; page++) {
            EnemyHudLayout.Options o = new EnemyHudLayout.Options();
            o.showHearts = true;
            o.showArmor = true;
            o.showBar = false;
            o.armourPage = page;
            EnemyHudLayout.Result r = EnemyHudLayout.compute(150, 150, false, 5, five,
                    HeartCounts.of(60, 40, 0, 0, 0), o, SelfCheck::width);
            String text = join(r);
            String armour = rowText(r, 0);
            check(r.rowCount() == 2, "有护甲时应固定两行: " + r.rowCount());
            check(r.rows.get(0).isArmourRow(), "第一行应是护甲行");
            check(armour.contains("\uD83D\uDEE15"), "第 " + page + " 页应始终带 🛡5: " + armour);
            check(armour.contains(pageArmour[page]),
                    "第 " + page + " 页应显示 " + pageArmour[page] + ": " + armour);
            check(rowText(r, 1).equals("❤60/150 💛40"), "第二行应是心数: " + rowText(r, 1));
            check(text.contains("❤60/150"), "心数行始终在: " + text);
        }

        EnemyHudLayout.Options p0 = new EnemyHudLayout.Options();
        p0.showHearts = true;
        p0.showArmor = true;
        p0.showBar = false;
        p0.armourPage = 0;
        String zero = join(EnemyHudLayout.compute(150, 150, false, 5, five,
                HeartCounts.of(60, 40, 0, 0, 0), p0, SelfCheck::width));
        for (int i = 1; i < otherPages.length; i++) {
            check(!zero.contains(otherPages[i]),
                    "第 0 页不该出现其它页的护甲 " + otherPages[i] + ": " + zero);
        }

        // 页号越界时夹回范围内，不会画空。
        p0.armourPage = 99;
        String clamped = join(EnemyHudLayout.compute(150, 150, false, 5, five,
                HeartCounts.of(60, 40, 0, 0, 0), p0, SelfCheck::width));
        check(clamped.contains("\u26936"), "页号越界应夹到最后一页: " + clamped);
    }

    // ── 4c. 大数值缩写（1.17.0） ──

    private static void bigNumbers() {
        check(EnemyHudLayout.num(0).equals("0"), "num(0)");
        check(EnemyHudLayout.num(99999).equals("99999"), "num(99999)=" + EnemyHudLayout.num(99999));
        check(EnemyHudLayout.num(100_000).equals("100k"), "num(100000)=" + EnemyHudLayout.num(100_000));
        check(EnemyHudLayout.num(123_456).equals("123k"), "num(123456)=" + EnemyHudLayout.num(123_456));
        check(EnemyHudLayout.num(999_999).equals("999k"), "num(999999)=" + EnemyHudLayout.num(999_999));
        check(EnemyHudLayout.num(1_234_567).equals("1.2m"), "num(1234567)=" + EnemyHudLayout.num(1_234_567));
        check(EnemyHudLayout.num(12_345_678).equals("12.3m"), "num(12345678)="
                + EnemyHudLayout.num(12_345_678));
        check(EnemyHudLayout.num(999_999_999).equals("999m"), "num(999999999)="
                + EnemyHudLayout.num(999_999_999));
        check(EnemyHudLayout.num(Integer.MAX_VALUE).equals("2.1b"), "num(MAX)="
                + EnemyHudLayout.num(Integer.MAX_VALUE));
        check(EnemyHudLayout.num(Integer.MIN_VALUE).startsWith("-"), "num(MIN) 应带负号: "
                + EnemyHudLayout.num(Integer.MIN_VALUE));
        check(EnemyHudLayout.num(-123_456).equals("-123k"), "num(-123456)="
                + EnemyHudLayout.num(-123_456));

        // 缩写的目的是行宽可控：五段 999999 以前约 40 个字符，现在每段 4~5 个。
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showHearts = true;
        o.showBar = false;
        o.barWidth = 100;
        String text = join(EnemyHudLayout.compute(999_999, 999_999, false, 0, new int[7],
                HeartCounts.of(999_999, 999_999, 999_999, 999_999, 999_999), o, SelfCheck::width));
        check(!text.contains("999999"), "大数值应先缩写: " + text);
        check(text.contains("999k/999k"), "大数值缩写格式: " + text);
        check(text.length() < 60, "缩写后整行不该太长: " + text.length() + " -> " + text);
    }

    /** 第 index 行拼起来的文字（0 = 最上面那行）。 */
    private static String rowText(EnemyHudLayout.Result r, int index) {
        if (r == null || index < 0 || index >= r.rows.size()) return "";
        StringBuilder sb = new StringBuilder();
        for (EnemyHudLayout.Line l : r.rows.get(index).lines) sb.append(l.text);
        return sb.toString();
    }

    private static String join(EnemyHudLayout.Result r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        for (EnemyHudLayout.Line l : r.lines) sb.append(l.text);
        return sb.toString();
    }

    // ── 5b. 同名怪物的唯一认领 ──

    private static void barOwnershipScoring() {
        // expectedPercent 必须与数据包 misc/health.mcfunction:102-104 的整数截断口径一致
        expectBarPercent(160, 100, 100);   // 红心被 MaxHP 夹住 → 100%
        expectBarPercent(100, 100, 100);
        expectBarPercent(73, 120, 60);     // 73*100/120 = 60.83 → 截断 60
        expectBarPercent(50, 120, 41);     // 41.67 → 41
        expectBarPercent(1, 3, 33);        // 33.3 → 33
        expectBarPercent(0, 100, 0);
        expectBarPercent(5, 0, -1);        // 上限非法 → 算不出来
        expectBarPercent(-9, 100, 0);      // 负数夹到 0
    }

    private static void expectBarPercent(int hp, int maxHp, int expected) {
        BossBarTracker.Bar bar = new BossBarTracker.Bar(
                java.util.UUID.randomUUID(), "Stone Zombie", hp, maxHp,
                new int[8], 1.0f, HeartCounts.Kind.RED);
        check(bar.expectedPercent() == expected,
                "expectedPercent(" + hp + "/" + maxHp + ")=" + bar.expectedPercent());
    }

    // ── 5b-2. 屏幕空间遮挡（后方的怪被前面的挡住就不画） ──

    private static void screenOcclusion() {
        java.util.List<double[]> none = java.util.List.of();
        double[] self = {0, 0, 100, 100};

        check(BoxOcclusion.coverage(self, none) == 0.0, "没有更近实体时覆盖率应为 0");
        check(!BoxOcclusion.hidden(self, none, BoxOcclusion.DEFAULT_THRESHOLD), "没有遮挡不该隐藏");

        double[] half = {0, 0, 50, 100};
        check(Math.abs(BoxOcclusion.coverage(self, java.util.List.of(half)) - 0.5) < 1e-9,
                "盖住一半时覆盖率应 0.5");
        check(!BoxOcclusion.hidden(self, java.util.List.of(half), 0.7), "50% 不该隐藏");

        double[] most = {0, 0, 80, 100};
        check(BoxOcclusion.hidden(self, java.util.List.of(most), 0.7), "80% 应当隐藏");
        check(!BoxOcclusion.hidden(self, java.util.List.of(most), 0.9), "阈值 0.9 时 80% 不该隐藏");

        // 两块各盖 40%、互不重叠 → 合计 80%
        double[] left = {0, 0, 40, 100};
        double[] right = {40, 0, 80, 100};
        check(Math.abs(BoxOcclusion.coverage(self, java.util.List.of(left, right)) - 0.8) < 1e-9,
                "两段合计应 0.8");
        check(BoxOcclusion.hidden(self, java.util.List.of(left, right), 0.7), "合计 80% 应隐藏");

        // 完全重合的两个框仍然只算 100%，不会超过 1
        check(BoxOcclusion.coverage(self, java.util.List.of(half, half)) == 1.0,
                "重复覆盖应夹到 1.0");

        // 完全错开 / 只有边界接触 / 退化矩形
        double[] away = {200, 200, 300, 300};
        check(BoxOcclusion.coverage(self, java.util.List.of(away)) == 0.0, "错开的框覆盖率 0");
        double[] touching = {100, 0, 150, 100};
        check(BoxOcclusion.coverage(self, java.util.List.of(touching)) == 0.0, "只贴边不算覆盖");
        double[] flat = {0, 50, 100, 50};
        check(BoxOcclusion.coverage(flat, java.util.List.of(most)) == 0.0, "零面积自身覆盖率 0");
        check(BoxOcclusion.coverage(null, java.util.List.of(most)) == 0.0, "自身为 null 时 0");
        check(BoxOcclusion.coverage(self, java.util.Collections.singletonList((double[]) null)) == 0.0,
                "对比框为 null 时 0");

        // 阈值非法 → 回落默认 0.70
        check(BoxOcclusion.hidden(self, java.util.List.of(most), 0.0), "阈值 0 应回落默认并隐藏");
        check(BoxOcclusion.hidden(self, java.util.List.of(most), 2.0), "阈值 2 应回落默认并隐藏");
    }

    // ── 5c. 服务端选装组件的数据换算 ──

    private static void payloadConversion() {
        // 魂心阶段：AllHearts 160 / MaxHP 100 / 魂 60 → 红 100，比例 60/100（数据包 Boss 栏同款口径）
        EnemyData.Snapshot soul = EnemyData.fromPayload(160, 100, 60, 0, 0, 0, 100, 0, 0, 3,
                new int[]{1, 0, 0, 0, 0, 0, 0});
        check(soul.hearts.get(HeartCounts.RED) == 100, "payload 红心=" + soul.hearts.get(HeartCounts.RED));
        check(soul.hearts.get(HeartCounts.SOUL) == 60, "payload 魂心=" + soul.hearts.get(HeartCounts.SOUL));
        check(soul.kind == HeartCounts.Kind.SOUL, "魂心阶段 kind=" + soul.kind);
        check(Math.abs(soul.ratio - 0.6f) < 1e-6, "魂心阶段 ratio=" + soul.ratio);
        check(soul.hp == 160 && soul.maxHp == 100, "payload hp/maxHp=" + soul.hp + "/" + soul.maxHp);
        check(soul.defence == 3 && soul.armor[0] == 1, "payload 护甲没接上");

        // 红心阶段：只有红心 → 比例按 min(红,MaxHP)/MaxHP
        EnemyData.Snapshot red = EnemyData.fromPayload(80, 100, 0, 0, 0, 0, 0, 0, 0, 0, new int[7]);
        check(red.kind == HeartCounts.Kind.RED, "红心阶段 kind=" + red.kind);
        check(Math.abs(red.ratio - 0.8f) < 1e-6, "红心阶段 ratio=" + red.ratio);
        check(red.hearts.get(HeartCounts.RED) == 80, "红心阶段红心=" + red.hearts.get(HeartCounts.RED));

        // 蓝心优先级最高（数据包 HeartValue 顺序 蓝 > 黑 > 魂 > 红）
        EnemyData.Snapshot blue = EnemyData.fromPayload(100, 100, 0, 0, 30, 0, 0, 0, 60, 0, new int[7]);
        check(blue.kind == HeartCounts.Kind.BLUE, "蓝心阶段 kind=" + blue.kind);
        check(Math.abs(blue.ratio - 0.5f) < 1e-6, "蓝心阶段 ratio=" + blue.ratio);
        check(blue.hearts.get(HeartCounts.RED) == 70, "蓝心阶段反算红心=" + blue.hearts.get(HeartCounts.RED));

        // 玩家（队友）的绿条：同一份 payload，进度固定是 红心 ÷ MaxHP，不吃「按心类分层」。
        // 红 60 / 魂 40 / 黑 30 / 蓝 20，MaxHP 150 → 红 60/150 = 0.4；
        // 敌人口径则会取蓝心 20/60 ≈ 0.333。
        EnemyData.Snapshot team = EnemyData.fromPayloadPlayer(150, 150, 40, 30, 20, 0,
                100, 100, 60, 0, new int[7]);
        check(team.kind == HeartCounts.Kind.RED, "玩家进度必须走红心口径 kind=" + team.kind);
        check(Math.abs(team.ratio - 0.4f) < 1e-6, "玩家进度 60/150=" + team.ratio);
        check(team.hearts.get(HeartCounts.RED) == 60 && team.hearts.get(HeartCounts.SOUL) == 40
                        && team.hearts.get(HeartCounts.BLACK) == 30
                        && team.hearts.get(HeartCounts.BLUE) == 20,
                "玩家的心数行必须和敌人同一套");
        EnemyData.Snapshot mobSame = EnemyData.fromPayload(150, 150, 40, 30, 20, 0,
                100, 100, 60, 0, new int[7]);
        check(mobSame.kind == HeartCounts.Kind.BLUE, "敌人进度仍按当前心类分层 kind=" + mobSame.kind);
        check(Math.abs(mobSame.ratio - (20f / 60f)) < 1e-5, "敌人分层进度 20/60=" + mobSame.ratio);
        // 红心被 MaxHP 夹住（红心不会超过上限），进度不得 > 1。
        EnemyData.Snapshot teamCapped = EnemyData.fromPayloadPlayer(160, 100, 60, 0, 0, 0,
                100, 0, 0, 0, new int[7]);
        check(Math.abs(teamCapped.ratio - 1.0f) < 1e-6, "玩家进度应夹到 1: " + teamCapped.ratio);
        // MaxHP 缺失 → 进度 0（心数行照旧）。
        EnemyData.Snapshot teamNoMax = EnemyData.fromPayloadPlayer(30, 0, 0, 0, 0, 0,
                0, 0, 0, 0, new int[7]);
        check(teamNoMax.ratio == 0.0f, "玩家 MaxHP=0 时 ratio=" + teamNoMax.ratio);

        // 黑心次之
        EnemyData.Snapshot black = EnemyData.fromPayload(100, 100, 0, 20, 0, 0, 0, 40, 0, 0, new int[7]);
        check(black.kind == HeartCounts.Kind.BLACK, "黑心阶段 kind=" + black.kind);
        check(Math.abs(black.ratio - 0.5f) < 1e-6, "黑心阶段 ratio=" + black.ratio);

        // 边界：数据自相矛盾（各色之和 > AllHearts）→ 红心夹到 0，不能为负
        EnemyData.Snapshot weird = EnemyData.fromPayload(10, 100, 20, 0, 0, 0, 20, 0, 0, 0, new int[7]);
        check(weird.hearts.get(HeartCounts.RED) == 0, "红心不该为负: " + weird.hearts.get(HeartCounts.RED));

        // 边界：MaxHP 为 0 / 缺字段 → 比例 0，但心数照旧
        EnemyData.Snapshot noMax = EnemyData.fromPayload(30, 0, 0, 0, 0, 0, 0, 0, 0, 0, new int[7]);
        check(noMax.ratio == 0.0f, "MaxHP=0 时 ratio=" + noMax.ratio);
        check(noMax.hearts.get(HeartCounts.RED) == 30, "MaxHP=0 时红心=" + noMax.hearts.get(HeartCounts.RED));

        // 碎心的符号：> 0 是碎心，< 0 是粉心（数据包 view_stats 的口径）
        EnemyData.Snapshot crackedOnly = EnemyData.fromPayload(50, 50, 0, 0, 0, 3, 0, 0, 0, 0, new int[7]);
        check(crackedOnly.hearts.get(HeartCounts.CRACKED) == 3, "正碎心="
                + crackedOnly.hearts.get(HeartCounts.CRACKED));
        check(crackedOnly.hearts.get(HeartCounts.PINK) == 0, "正碎心不该有粉心="
                + crackedOnly.hearts.get(HeartCounts.PINK));

        EnemyData.Snapshot pink = EnemyData.fromPayload(50, 50, 0, 0, 0, -5, 0, 0, 0, 0, new int[7]);
        check(pink.hearts.get(HeartCounts.CRACKED) == 0, "负碎心不该进碎心栏: "
                + pink.hearts.get(HeartCounts.CRACKED));
        check(pink.hearts.get(HeartCounts.PINK) == 5, "负碎心应变成 5 粉心: "
                + pink.hearts.get(HeartCounts.PINK));
        check(pink.hearts.has(HeartCounts.PINK) && !pink.hearts.has(HeartCounts.CRACKED),
                "粉心存在而碎心不存在");

        // 直接测 HeartCounts.of 的四条边界
        check(HeartCounts.of(1, 0, 0, 0, 7).get(HeartCounts.CRACKED) == 7, "of(+7) 碎心");
        check(HeartCounts.of(1, 0, 0, 0, 7).get(HeartCounts.PINK) == 0, "of(+7) 粉心");
        check(HeartCounts.of(1, 0, 0, 0, -7).get(HeartCounts.PINK) == 7, "of(-7) 粉心");
        check(HeartCounts.of(1, 0, 0, 0, -7).get(HeartCounts.CRACKED) == 0, "of(-7) 碎心");
        check(HeartCounts.of(0, 0, 0, 0, 0).get(HeartCounts.PINK) == 0, "of(0) 粉心");
        check(HeartCounts.of(0, 0, 0, 0, Integer.MIN_VALUE).get(HeartCounts.PINK) == Integer.MAX_VALUE,
                "MIN_VALUE 粉心不该溢出为负");
        check(HeartCounts.unknown().get(HeartCounts.PINK) == HeartCounts.UNKNOWN, "未知粉心哨兵值");
    }

    // ── 6. Boss 栏标题解析 ──

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

        // 宽松扫描：队伍条标题里夹着空槽残渣（@a 选择器与 score 渲染成空）时仍要能抠出玩家。
        java.util.List<TeamBarTitle.Entry> loose =
                TeamBarTitle.scanLoose("A (10/20) (/) B (30/40) (0/0) ");
        check(loose.size() == 2, "宽松扫描条目数=" + loose.size());
        if (loose.size() == 2) {
            check("A".equals(loose.get(0).name) && loose.get(0).hp == 10 && loose.get(0).maxHp == 20,
                    "宽松扫描第 1 条=" + loose.get(0).name);
            check("B".equals(loose.get(1).name) && loose.get(1).hp == 30 && loose.get(1).maxHp == 40,
                    "宽松扫描第 2 条=" + loose.get(1).name);
        }
        check(TeamBarTitle.scanLoose("高塔 (12/50)").size() == 1, "场景名也是合法形状");
        check(TeamBarTitle.scanLoose(" (/) ").isEmpty(), "空槽残渣不该解析出条目");
        check(TeamBarTitle.scanLoose("(0/0)").isEmpty(), "上限 0 不该解析出条目");
        check(TeamBarTitle.scanLoose("").isEmpty(), "宽松扫描空串");
        check(TeamBarTitle.scanLoose(null).isEmpty(), "宽松扫描 null");
        // 敌人条括号里有 "HP " 前缀，数字解析直接失败 —— 宽松扫描也不会把它当队伍条目。
        check(TeamBarTitle.scanLoose("Frost Wolf (HP 73/120)").isEmpty(),
                "敌人条的 (HP x/y) 不该被宽松扫描当成队伍条目");
        check(TeamBarTitle.scanLoose("Frost Wolf (73/120)").size() == 1,
                "无 HP 前缀的 (x/y) 会被扫出来，但名字匹配不到玩家就不算队伍条");
    }

    // ── 基础设施 ──

    private static EnemyHudLayout.Result run(String tag, Data d, EnemyHudLayout.Options o) {
        try {
            EnemyHudLayout.Result r = EnemyHudLayout.compute(
                    d.hp, d.maxHp, d.percentOnly, d.defence, d.armor, d.hearts, o, SelfCheck::width);
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
        final HeartCounts hearts;

        Data(String name, int hp, int maxHp, boolean percentOnly, int defence, int[] armor) {
            this.name = name;
            this.hp = hp;
            this.maxHp = maxHp;
            this.percentOnly = percentOnly;
            this.defence = defence;
            this.armor = armor;
            this.hearts = percentOnly ? HeartCounts.unknown() : HeartCounts.forMob(hp, maxHp);
        }
    }
}
