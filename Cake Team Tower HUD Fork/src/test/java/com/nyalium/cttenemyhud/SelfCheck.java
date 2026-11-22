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
        check(defaults.showBar, "默认应画血条");
        check(defaults.hideBehindWalls, "默认应忽略被墙挡住的怪");
        check(defaults.hideBehindEntities, "默认应忽略被其它实体挡住的怪");
        check(defaults.hideBelowNameScore, "默认应隐藏 below_name 分数");
        check(defaults.hideEnemyBossBar, "默认应隐藏上方 Boss 栏里的最近敌人信息");
        check(defaults.hideTeamBossBar, "默认应隐藏上方 Boss 栏里的队伍血量");
        check(defaults.showTeammates, "默认应显示队友血量");
        check(defaults.autoPartyBossbar, "默认应自动开启队伍血量条（纯客户端数据源）");

        check(EnemyHudConfig.normalize(null) != null, "normalize(null) 不能返回 null");

        // 换行阈值：40~200，200 = 自动。
        EnemyHudConfig.Cfg wrap = new EnemyHudConfig.Cfg();
        wrap.wrapWidth = 0;
        EnemyHudConfig.normalize(wrap);
        check(wrap.wrapWidth == EnemyHudLayout.WRAP_MIN, "wrapWidth 0 应夹到 " + EnemyHudLayout.WRAP_MIN);
        wrap.wrapWidth = 9999;
        EnemyHudConfig.normalize(wrap);
        check(wrap.wrapWidth == EnemyHudLayout.WRAP_AUTO, "wrapWidth 9999 应夹到自动");
        wrap.wrapWidth = Double.NaN;
        EnemyHudConfig.normalize(wrap);
        check(wrap.wrapWidth == EnemyHudLayout.WRAP_AUTO, "wrapWidth NaN 应回落自动");
        wrap.wrapWidth = 80;
        EnemyHudConfig.normalize(wrap);
        check(wrap.wrapWidth == 80, "wrapWidth 80 应保持");
        check(defaults.wrapWidth == EnemyHudLayout.WRAP_AUTO, "默认换行阈值应为自动");

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
        check(!joined.contains(EnemyHudLayout.SEPARATOR), "同类之间不该有分隔符=" + joined);
        check(r.rowCount() == 1, "不超宽时不该折行: " + r.rowCount());

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

        // 一行放得下时，心组与护甲之间只有一个分隔符。
        EnemyHudLayout.Options withArmor = new EnemyHudLayout.Options();
        withArmor.showBar = false;
        EnemyHudLayout.Result narrowArmor = run("窄护甲", new Data("一个护甲", 100, 100, false, 0,
                new int[]{3, 0, 0, 0, 0, 0, 0}), withArmor);
        check(narrowArmor.rowCount() == 1, "窄内容不该折行: " + narrowArmor.rowCount());
        check(join(narrowArmor).startsWith("❤100/100 | "), "心组与护甲之间才该有分隔符: "
                + join(narrowArmor));
        check(!join(narrowArmor).contains("❤100/100 | ❤"), "心里面不该出现分隔符: " + join(narrowArmor));

        // 一行放不下（超过 max(条长, MIN_ROW_WIDTH)）→ 红心顶到上面一行。
        EnemyHudLayout.Result armored = run("心数+护甲", new Data("带护甲", 160, 100, false, 3,
                new int[]{0, 5, 0, 0, 0, 0, 0}), withArmor);
        check(armored.rowCount() == 2, "超宽时应折成两行: " + armored.rowCount());
        check(rowText(armored, 0).equals("❤100/100"), "第一行应只有红心: " + rowText(armored, 0));
        check(rowText(armored, 1).equals("💛60 | " + "\uD83D\uDEE13" + " "
                + EnemyData.ARMOR_ICONS[1] + "5"), "第二行应是其它心+护甲: " + rowText(armored, 1));
        check(armored.rows.get(0).top == EnemyHudLayout.TEXT_TOP - EnemyHudLayout.ROW_STEP,
                "上面一行要再抬 " + EnemyHudLayout.ROW_STEP + " 像素");
        check(armored.rows.get(1).top == EnemyHudLayout.TEXT_TOP, "下面一行贴着 TEXT_TOP");
        check(armored.textTop == EnemyHudLayout.TEXT_TOP, "textTop 仍指最底一行");

        // 换行阈值的解析：最右 = 自动（不超过血条长度），血条关掉时用默认长度兜底。
        check(EnemyHudLayout.resolveWrapWidth(EnemyHudLayout.WRAP_AUTO, 120) == 120,
                "自动阈值应等于条长");
        check(EnemyHudLayout.resolveWrapWidth(EnemyHudLayout.WRAP_AUTO, 0)
                == EnemyHudLayout.BAR_WIDTH_DEFAULT, "血条关掉时自动阈值应兜底");
        check(EnemyHudLayout.resolveWrapWidth(Double.NaN, 120) == 120, "NaN 应视为自动");
        check(EnemyHudLayout.resolveWrapWidth(0, 120) == 120, "0 应视为自动");
        check(EnemyHudLayout.resolveWrapWidth(60, 120) == 60, "手填阈值应生效");

        // 手填 60 像素：78 像素的两色行必须折成两行。
        EnemyHudLayout.Options smallWrap = new EnemyHudLayout.Options();
        smallWrap.showBar = false;
        smallWrap.showArmor = false;
        smallWrap.wrapWidth = 60;
        EnemyHudLayout.Result wrapped = run("小阈值", new Data("两色", 160, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), smallWrap);
        check(wrapped.rowCount() == 2, "阈值 60 时 78 像素应折行: " + wrapped.rowCount());
        check(rowText(wrapped, 0).equals("❤100/100"), "折行后第一行=" + rowText(wrapped, 0));
        check(rowText(wrapped, 1).equals("💛60"), "折行后第二行=" + rowText(wrapped, 1));

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
        check(teamHearts.rowCount() == 2, "队友五色心应折两行: " + teamHearts.rowCount());
        check(rowText(teamHearts, 0).equals("❤60/150"), "队友第一行=" + rowText(teamHearts, 0));
        check(rowText(teamHearts, 1).equals("💛40 🖤30 💙20 💔2"),
                "队友第二行=" + rowText(teamHearts, 1));
        // 用转义写死期望值：🛡 在源码里常被写成「🛡+U+FE0F」，直接比字面量会踩变体选择符。
        check(join(armored).contains("\uD83D\uDEE13"), "防御段缺失: " + join(armored));
        check(join(armored).contains(EnemyData.ARMOR_ICONS[1] + "5"), "元素护甲段缺失: " + join(armored));
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
