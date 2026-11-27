package com.nyalium.ctthudfork;

import net.minecraft.network.chat.Component;

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
        barSlicing();
        configNormalization();
        armorEdgeCases();
        heartCountsAndRows();
        armourPaging();
        heartCycling();
        bigNumbers();
        barOwnershipScoring();
        payloadConversion();
        bossBarTitles();
        curseKeys();
        teamBarTitles();
        selfStatsAndPanel();
        selfStatsFineKinds();
        percentOnlyText();
        lobbyTriggerGuard();
        bossBarBinding();
        filterGridWidth();
        panelModes();
        scrollBand();
        chatAndDrag();
        panelTimerAndGrass();
        scrollbarDrag();
        scopedSettings();
        relativePositions();

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
                HudLayout.Options o = new HudLayout.Options();
                o.showNumbers = (mask & 1) != 0;
                o.showArmor = (mask & 2) != 0;
                o.showBar = (mask & 4) != 0;

                String tag = "开关 mask=" + mask + " 数据=" + d.name;
                HudLayout.Result r = run(tag, d, o);

                boolean hasArmorData = HudLayout.hasArmor(d.defence, d.armor);
                boolean expectText = o.showNumbers || (o.showArmor && hasArmorData);
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
                    check(r.barWidth >= HudLayout.BAR_WIDTH_MIN
                            && r.barWidth <= HudLayout.BAR_WIDTH_MAX, tag + " barWidth 越界");
                }

                int sum = 0;
                for (HudLayout.Line l : r.lines) sum += width(l.text);
                check(sum == r.textWidth, tag + " textWidth 不等于分段之和");

                for (HudLayout.Line l : r.lines) {
                    check(!l.text.isEmpty(), tag + " 出现空文字段");
                    check((l.rgb & 0xFF000000) == 0, tag + " 颜色含高位");
                }

                check(r.barTop + HudLayout.BAR_HEIGHT <= HudLayout.NAME_TOP,
                        tag + " 血条压到名称行");
                check(r.textTop < HudLayout.NAME_TOP, tag + " 文字压到名称行");
                check(r.textTop + 9 <= r.barTop, tag + " 文字与血条重叠");

                // 行距必须 ≥ 原版文字背景框高度（y-1 ~ y+9 = 10 像素）；用 9 会让
                // 上行的底边与下行的顶边重叠 1 像素，两层半透明黑叠成一道更深的暗带。
                check(HudLayout.ROW_STEP >= HudLayout.TEXT_BOX_HEIGHT,
                        tag + " ROW_STEP 小于背景框高度");
                for (int i = 1; i < r.rows.size(); i++) {
                    check(r.rows.get(i - 1).top + HudLayout.TEXT_BOX_HEIGHT <= r.rows.get(i).top,
                            tag + " 第 " + i + " 行与上一行的背景框重叠");
                }
            }
        }
    }

    // ── 1b. 纯客户端（只有百分比）至少要有数字 ──

    /**
     * 纯客户端只有 {@code below_name = HealthPercent} 时：血条 + 「73%」数字。
     *
     * <p>percentOnly 不再只画条：数字行写百分比（口径 = hp*100/maxHp 四舍五入夹 0~100）。</p>
     */
    private static void percentOnlyText() {
        int[][] cases = {
                {73, 100, 73},
                {41, 100, 41},
                {0, 100, 0},
                {100, 100, 100},
                {5, 0, 100},      // 上限非法 → 100%
                {50, 100, 50},
        };
        for (int[] c : cases) {
            Data d = new Data("仅百分比", c[0], c[1], true, 0, new int[7]);
            HudLayout.Options o = new HudLayout.Options();
            o.showNumbers = true;
            o.showBar = true;
            var r = run("percentOnly " + c[0] + "/" + c[1], d, o);
            if (r == null) continue;
            check(r.hasText, "percentOnly " + c[0] + "/" + c[1] + " 应有文字");
            check(flatten(r).equals("❤ " + c[2] + "%"),
                    "percentOnly " + c[0] + "/" + c[1] + " 应显示 ❤ " + c[2] + "%，实际 " + flatten(r));
            check(r.barWidth > 0, "percentOnly 仍应有血条宽度");

            o.showNumbers = false;
            var r2 = run("percentOnly 无数字 " + c[0] + "/" + c[1], d, o);
            if (r2 != null) {
                check(!r2.hasText, "关掉数字后 percentOnly 不该有文字");
            }
        }

        // 队友没有队伍条条目：同样只有 HealthPercent → 也要有百分比数字
        HeartCounts unknown = HeartCounts.unknown();
        HudLayout.Options team = new HudLayout.Options();
        team.showNumbers = true;
        team.showArmor = false;
        team.playerStyle = true;
        team.showBar = true;
        var tr = HudLayout.compute(63, 100, true, 0, new int[7], unknown, team, SelfCheck::width);
        check(tr != null && flatten(tr).equals("❤ 63%"), "队友百分比回退应显示 ❤ 63%，实际 " + flatten(tr));

        // 「拆不出心数但已知当前/上限」（队伍条那一份）仍然写 当前/最大
        var exact = HudLayout.compute(63, 120, false, 0, new int[7], unknown, team, SelfCheck::width);
        check(exact != null && flatten(exact).equals("❤ 63/120"),
                "未知心数的绝对值应退回 63/120，实际 " + flatten(exact));
    }

    /** 把布局里的所有文字段拼起来（测试断言用）。 */
    private static String flatten(HudLayout.Result r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        for (HudLayout.Line line : r.lines) sb.append(line.text);
        return sb.toString();
    }

    // ── 1c. 大厅里不该发 ViewStats / 失败提示要吞掉 ──

    /**
     * 大厅里不发 {@code /trigger ViewStats}，并且「无法触发该计分项」这类回显要被过滤。
     */
    private static void lobbyTriggerGuard() {
        check(SelfStats.isTriggerDenied("You cannot trigger this objective yet"),
                "英文「无法触发该计分项」应被识别");
        check(SelfStats.isTriggerDenied("Cannot trigger this objective"), "无 yet 的英文也应识别");
        check(SelfStats.isTriggerDenied("无法触发该计分项"), "中文应识别");
        check(SelfStats.isTriggerDenied("无法触发此计分项"), "「此计分项」也应识别");
        check(!SelfStats.isTriggerDenied("50\u2764"), "属性行不该被当成拒绝提示");
        check(!SelfStats.isTriggerDenied("Game not started"), "「游戏没开始」是另一条，不该混");
        check(!SelfStats.isTriggerDenied(""), "空串不该识别");

        // ChatTriggerFilter：只有「刚发过 trigger」的时间窗内才拦
        ChatTriggerFilter.noteTriggerSent(0L);          // 过期
        check(!ChatTriggerFilter.shouldHide(
                        net.minecraft.network.chat.Component.literal("Triggered [ViewStats]"), false),
                "窗口外不该拦回显");
        long now = System.currentTimeMillis();
        ChatTriggerFilter.noteTriggerSent(now);
        check(ChatTriggerFilter.shouldHide(
                        net.minecraft.network.chat.Component.literal("Triggered [ViewStats]"), false),
                "刚发过 trigger 时 ViewStats 的成功回显应被吞掉");
        check(ChatTriggerFilter.shouldHide(
                        net.minecraft.network.chat.Component.translatable("commands.trigger.failed.unprimed"),
                        false),
                "commands.trigger.failed.* 应被吞掉（不论语言）");
        check(!ChatTriggerFilter.shouldHide(
                        net.minecraft.network.chat.Component.literal("Triggered [FooBar]"), false),
                "别的 trigger 回显不该被误吞");
        check(!ChatTriggerFilter.shouldHide(
                        net.minecraft.network.chat.Component.literal("200\u2764"), false),
                "属性行不该被聊天过滤器吞掉");
        check(!ChatTriggerFilter.shouldHide(
                        net.minecraft.network.chat.Component.literal("Triggered [ViewStats]"), true),
                "overlay（动作栏）不做处理");
        ChatTriggerFilter.noteTriggerSent(0L);          // 收尾：别影响别的用例
    }

    // ── 1d. 同名怪：bossbar 认领要「血量对得上」才认 ──

    /**
     * 同名怪附近时，bossbar 只认血量差 ≤ {@link BossBarTracker#PERCENT_TOLERANCE} 的实体，
     * 不再因为「上一帧绑过」或「名字一样」就继续粘着旧的那只。
     */
    private static void bossBarBinding() {
        check(BossBarTracker.PERCENT_TOLERANCE == 5, "容差应为 5 个百分点");
        check(BossBarTracker.percentDiff(99, 99) == 0, "99 vs 99 差 0");
        check(BossBarTracker.percentDiff(99, 97) == 2, "99 vs 97 差 2");
        check(BossBarTracker.percentDiff(99, 90) == 9, "99 vs 90 差 9");
        check(BossBarTracker.percentDiff(-1, 99) == Integer.MAX_VALUE, "bar 没数字时不可比");
        check(BossBarTracker.percentDiff(99, -1) == Integer.MAX_VALUE, "实体没有 HealthPercent 时不可比");

        check(BossBarTracker.acceptable(0), "差 0 应认领");
        check(BossBarTracker.acceptable(5), "差 5 应认领（容差边界）");
        check(!BossBarTracker.acceptable(6), "差 6 不该认领");
        check(!BossBarTracker.acceptable(Integer.MAX_VALUE), "不可比时不该认领");

        // 排序：精确 > 差≤2 > 差≤容差 > 粘性 > 名字 > 距离
        check(BossBarTracker.matchScore(0, true, false, 100)
                        > BossBarTracker.matchScore(2, true, true, 0),
                "精确吻合应压过「差2 + 粘性」");
        check(BossBarTracker.matchScore(2, true, false, 0)
                        > BossBarTracker.matchScore(5, true, true, 0),
                "差≤2 应压过「差≤容差 + 粘性」");
        check(BossBarTracker.matchScore(0, true, true, 0)
                        > BossBarTracker.matchScore(0, true, false, 0),
                "同分时粘性优先");
        check(BossBarTracker.matchScore(0, true, false, 0)
                        > BossBarTracker.matchScore(0, true, false, 4096),
                "同分时更近的优先");
        // 距离优先于粘性，但 2 格以内不换绑
        check(BossBarTracker.matchScore(0, true, true, 9)
                        > BossBarTracker.matchScore(0, true, false, 6),
                "粘性绑定的怪在 2 格以内应继续沿用");
        check(BossBarTracker.matchScore(0, true, true, 9)
                        < BossBarTracker.matchScore(0, true, false, 1),
                "更近的同名怪（差 2 格以上）应接手");
        check(BossBarTracker.matchScore(0, true, false, 0)
                        > BossBarTracker.matchScore(0, true, true, 100),
                "远处的旧绑定不该压过眼前的同名怪");
        check(BossBarTracker.matchScore(0, true, true, 0)
                        > BossBarTracker.matchScore(0, false, true, 0),
                "名字完全相同优于互相包含");
    }

    // ── 1e. 过滤界面：按钮宽度参照原版（200），窄屏才收缩 ──

    private static void filterGridWidth() {
        check(SelfStatsFilterScreen.columnWidth(853) == 200, "宽屏（GUI 853）两列按钮应为原版宽度 200");
        check(SelfStatsFilterScreen.columnWidth(426) == 196, "GUI 426 收缩到 196（200 放不下两个）");
        check(SelfStatsFilterScreen.columnWidth(320) < 200, "窄屏（GUI 320）应收缩到可用宽度");
        check(SelfStatsFilterScreen.columnWidth(320) >= 60, "再窄也不该小于 60");
        check(SelfStatsFilterScreen.columnWidth(150) == 60, "极窄屏兜底 60");
    }

    // ── 1f. 属性面板显示方式：四套配置 ──

    /** HUD / 容器界面各一套位置·列数·不透明度；模式 0/1/2（v1.0.0 起没有「文字大小」）。 */
    private static void panelModes() {
        HudConfig.Cfg c = HudConfig.get();
        int oldMode = c.selfStatsMode;
        try {
            c.selfStatsMode = 0;
            check(!HudConfig.containerMode(), "模式 0 不该是容器模式");
            c.selfStatsX = 11;
            c.selfStatsColumns = 3;
            c.selfStatsOpacity = 0.6;
            HudConfig.Panel hud = HudConfig.panel(0, false, 320, 240);
            check(hud.x == 11 && hud.columns == 3 && hud.opacity == 0.6,
                    "HUD 模式应取 selfStats* 那一套");

            c.selfStatsMode = 1;
            check(HudConfig.containerMode(), "模式 1 应是容器模式");
            check(!HudConfig.hudMode(), "模式 1 不应在 HUD 上画");
            c.selfStatsMode = 2;
            check(HudConfig.containerMode() && HudConfig.hudMode(), "模式 2 应 HUD + 容器都画");
            c.selfStatsMode = 1;   // 后面的断言按「容器模式」继续
            c.containerX = 22;
            c.containerColumns = 1;
            c.containerOpacity = 0.3;
            HudConfig.Panel box = HudConfig.panel(1, false, 320, 240);
            check(box.x == 22 && box.columns == c.selfStatsColumns
                            && box.opacity == c.selfStatsOpacity,
                    "容器模式位置取 container*，列数/不透明度用全局值");

            // 移动只改当前模式那一套
            HudConfig.movePanel(1, false, 33, 44, 320, 240);
            check(c.containerX == 33 && c.containerY == 44, "容器模式下移动应写 container*");
            check(c.selfStatsX == 11, "HUD 那一套不该被改");
            HudConfig.setPanelColumns(1, false, 2);
            check(c.containerColumns == 2 && c.selfStatsColumns == 3, "setPanelColumns 应只改当前模式");

            // 模式 2 有自己的一套（v8）
            c.selfStatsMode = 2;
            c.bothX = 55;
            c.bothColumns = 1;
            c.bothOpacity = 0.5;
            HudConfig.Panel both = HudConfig.panel(2, false, 320, 240);
            check(both.x == 55 && both.columns == c.selfStatsColumns
                            && both.opacity == c.selfStatsOpacity,
                    "模式 2 位置取 both*，列数/不透明度用全局值");
            HudConfig.movePanel(2, false, 66, 77, 320, 240);
            check(c.bothX == 66 && c.bothY == 77, "模式 2（HUD）移动应写 both*");
            check(c.selfStatsX == 11 && c.containerX == 33, "模式 2 移动不该改另外两套");
            // 模式 2 的容器那一套独立
            c.bothContainerX = 88;
            c.bothContainerColumns = 3;
            HudConfig.Panel bothBox = HudConfig.panel(2, true, 320, 240);
            check(bothBox.x == 88 && bothBox.columns == c.selfStatsColumns,
                "模式 2 容器位置取 bothContainer*，列数用全局值");
            HudConfig.movePanel(2, true, 99, 111, 320, 240);
            check(c.bothContainerX == 99 && c.bothX == 66, "模式 2 容器移动应只改 bothContainer*");
        } finally {
            c.selfStatsMode = oldMode;
        }

        // 迁移：v6 之前没有 container*，应从 selfStats* 复制
        HudConfig.Cfg old = new HudConfig.Cfg();
        old.configVersion = 6;
        old.selfStatsX = 123;
        old.selfStatsY = 45;
        old.selfStatsColumns = 3;
        old.selfStatsOpacity = 0.7;
        HudConfig.migrate("{\"configVersion\":6}", old);
        check(old.containerX == 123 && old.containerY == 45 && old.containerColumns == 3,
                "v6 → v7 应把 HUD 配置复制给容器那套");
        check(Math.abs(old.containerOpacity - 0.7) < 1e-9, "v6 → v7 不透明度也应复制");

        // 夹取：模式 0~2、列数 1~3
        HudConfig.Cfg wild = new HudConfig.Cfg();
        wild.selfStatsMode = 9;
        wild.containerColumns = 9;
        HudConfig.normalize(wild);
        check(wild.selfStatsMode == 2, "selfStatsMode 9 应夹到 2");
        check(wild.containerColumns == 3, "containerColumns 9 应夹到 3");
    }

    // ── 2. 血条长度 ──

    private static void barWidthSweep() {
        double[] configs = {0.0, 16.0, 50.0, 100.0, 150.0, 200.0, 1000.0, -5.0, Double.NaN};
        double[] scales = {0.5, 0.75, 1.0, 1.5, 4.0};
        Data d = new Data("扫描", 73, 120, false, 3, new int[]{5, 2, 0, 0, 0, 0, 1});

        for (double bw : configs) {
            for (double sc : scales) {
                HudLayout.Options o = new HudLayout.Options();
                o.barWidth = bw;
                o.scale = sc;
                String tag = "barWidth=" + bw + " scale=" + sc;
                HudLayout.Result r = run(tag, d, o);
                check(r.barWidth >= HudLayout.BAR_WIDTH_MIN
                        && r.barWidth <= HudLayout.BAR_WIDTH_MAX, tag + " barWidth 越界");
                check(r.textTop == HudLayout.TEXT_TOP, tag + " textTop 应在 TEXT_TOP");
                check(r.barTop == HudLayout.BAR_TOP, tag + " barTop 应在 BAR_TOP");
                check(r.textTop + 9 <= r.barTop, tag + " 文字与血条重叠");
            }
        }
    }

    // ── 3. 配置归一化 ──

    private static void configNormalization() {
        HudConfig.Cfg c = new HudConfig.Cfg();
        c.barOpacity = 0.1;
        c.textOpacity = 0.1;
        c.scale = 0.01;
        c.maxDistance = 0;
        c.barWidth = -100;
        HudConfig.normalize(c);
        check(c.barOpacity == 0.1, "barOpacity 0.1 应保持 0.1（下限 0.10）");
        check(c.textOpacity == 0.1, "textOpacity 0.1 应保持 0.1（下限 0.10）");
        check(c.scale == 0.75, "scale 0.01 应夹到 0.75");
        check(c.maxDistance == 4.0, "maxDistance 0 应夹到 4");
        check(c.barWidth == HudLayout.BAR_WIDTH_MIN, "barWidth -100 应夹到 50");

        c.barOpacity = 3.0;
        c.textOpacity = 3.0;
        c.scale = 99;
        c.maxDistance = 9999;
        c.barWidth = 9999;
        HudConfig.normalize(c);
        check(c.barOpacity == 1.0, "barOpacity 3 应夹到 1.0");
        check(c.textOpacity == 1.0, "textOpacity 3 应夹到 1.0");
        check(c.scale == 1.5, "scale 99 应夹到 1.5");
        check(c.maxDistance == 256.0, "maxDistance 9999 应夹到 256");
        check(c.barWidth == HudLayout.BAR_WIDTH_MAX, "barWidth 9999 应夹到 150");

        c.barOpacity = Double.NaN;
        c.textOpacity = Double.NaN;
        c.barWidth = Double.NaN;
        HudConfig.normalize(c);
        check(c.barOpacity == 1.0, "barOpacity NaN 应回落 1.0");
        check(c.textOpacity == 1.0, "textOpacity NaN 应回落 1.0");
        check(c.barWidth == HudLayout.BAR_WIDTH_DEFAULT, "barWidth NaN 应回落 100");

        HudConfig.Cfg defaults = HudConfig.normalize(new HudConfig.Cfg());
        check(defaults.barWidth == 75.0, "默认血条长度应为 75");
        check(defaults.teamBarWidth == 75.0, "队友血条默认长度也应为 75");
        check(defaults.scale == 0.9, "默认缩放应为 0.9: " + defaults.scale);
        // 自己的血条默认关
        check(!new HudConfig.Cfg().showSelf, "自己的血条默认应关闭");
        // 调试功能默认值 —— 优先服务端数据、筛选数量 = 单包上限
        check(new HudConfig.Cfg().preferServerData, "默认应优先使用服务端数据");
        check(defaults.payloadLimit == MobHealthPayload.MAX_ENTRIES,
                "默认筛选数量应等于单包上限: " + defaults.payloadLimit);
        HudConfig.Cfg lowLimit = new HudConfig.Cfg();
        lowLimit.payloadLimit = 0;
        check(HudConfig.normalize(lowLimit).payloadLimit == 1, "筛选数量下限应夹到 1");
        HudConfig.Cfg highLimit = new HudConfig.Cfg();
        highLimit.payloadLimit = 999;
        check(HudConfig.normalize(highLimit).payloadLimit == MobHealthPayload.MAX_ENTRIES,
                "筛选数量上限应夹到 " + MobHealthPayload.MAX_ENTRIES);
        check(defaults.teamScale == 0.9, "队友默认缩放也应为 0.9: " + defaults.teamScale);
        // 血条 / 文字的不透明度分开，默认 65% / 100%
        check(defaults.barOpacity == 0.65, "默认血条不透明度应为 0.65: " + defaults.barOpacity);
        check(defaults.textOpacity == 1.0, "默认文字不透明度应为 1.00: " + defaults.textOpacity);
        check(defaults.teamBarOpacity == 0.65, "玩家默认血条不透明度应为 0.65: " + defaults.teamBarOpacity);
        check(defaults.teamTextOpacity == 1.0, "玩家默认文字不透明度应为 1.00: " + defaults.teamTextOpacity);
        check(defaults.showBar, "默认应画血条");
        check(defaults.hideBelowNameScore, "默认应隐藏 below_name 分数");
        check(defaults.hideEnemyBossBar, "默认应隐藏上方 Boss 栏里的最近敌人信息");
        check(defaults.hideTeamBossBar, "默认应隐藏上方 Boss 栏里的队伍血量");
        check(defaults.showTeammates, "默认应显示队友血量");

        check(HudConfig.normalize(null) != null, "normalize(null) 不能返回 null");

        // 癫狂靠专属 Boss 栏的翻译键识别（写死，无开关）
        check(BerserkDetector.isBerserkKey("Misc_Bossbars_P10_Bossbar_11"),
                "癫狂专属 bar 的键应被识别");
        check(!BerserkDetector.isBerserkKey("Items_status_effect21_berserk_1"),
                "聊天消息键不再作为识别依据");
        check(!BerserkDetector.isBerserkKey("Floors_Curse_Rooms_D_Curse02_Unknown_1"),
                "诅咒键不该被当成癫狂");
        check(BerserkDetector.marksBerserk(
                net.minecraft.network.chat.Component.translatable("Misc_Bossbars_P10_Bossbar_11")),
                "translatable 组件应被识别为癫狂 bar");
        check(!BerserkDetector.marksBerserk(
                net.minecraft.network.chat.Component.literal("Misc_Bossbars_P10_Bossbar_11")),
                "普通文本不该被识别");

        // v23：通用不透明度默认 0.75 → 0.50（下限 0.10）；v22：显示方式默认值改成「HUD + 容器界面」
        HudConfig.Cfg modeOld = new HudConfig.Cfg();
        modeOld.configVersion = 21;
        modeOld.selfStatsMode = 0;
        HudConfig.migrate("{\"configVersion\":21}", modeOld);
        check(modeOld.selfStatsMode == 2, "v21 的旧默认「始终显示」应迁移到 HUD + 容器");
        HudConfig.Cfg modeKept = new HudConfig.Cfg();
        modeKept.configVersion = 21;
        modeKept.selfStatsMode = 1;
        HudConfig.migrate("{\"configVersion\":21}", modeKept);
        check(modeKept.selfStatsMode == 1, "自己选过的显示方式不该被迁移改掉");

        // 老配置（没有 configVersion）迁移：旧默认 100 → 75，其它字段不动。
        HudConfig.Cfg old = new HudConfig.Cfg();
        old.barWidth = 100.0;
        old.teamBarWidth = 100.0;
        old.scale = 1.2;
        HudConfig.migrate("{\"barWidth\":100.0,\"scale\":1.2}", old);
        check(old.barWidth == 75.0, "旧默认 100 应迁移到 75: " + old.barWidth);
        check(old.teamBarWidth == 75.0, "队友旧默认 100 也应迁移到 75: " + old.teamBarWidth);
        check(old.scale == 1.2, "迁移不该动其它字段: " + old.scale);
        check(old.configVersion == HudConfig.CURRENT_VERSION, "迁移后应写上 configVersion");

        HudConfig.Cfg custom = new HudConfig.Cfg();
        custom.barWidth = 120.0;
        custom.teamBarWidth = 60.0;
        HudConfig.migrate("{\"barWidth\":120.0,\"teamBarWidth\":60.0}", custom);
        check(custom.barWidth == 120.0 && custom.teamBarWidth == 60.0,
                "自己调过的长度不该被迁移覆盖: " + custom.barWidth + "/" + custom.teamBarWidth);

        HudConfig.Cfg current = new HudConfig.Cfg();
        current.configVersion = 2;
        current.barWidth = 100.0;
        HudConfig.migrate("{\"configVersion\":2,\"barWidth\":100.0}", current);
        check(current.barWidth == 100.0, "新配置里显式写 100 不该被改: " + current.barWidth);

        // v2 的旧默认缩放 1.0 → 0.9，自己调过的保留。
        HudConfig.Cfg v2 = new HudConfig.Cfg();
        v2.configVersion = 2;
        v2.scale = 1.0;
        v2.teamScale = 1.0;
        HudConfig.migrate("{\"configVersion\":2,\"scale\":1.0}", v2);
        check(v2.scale == 0.9 && v2.teamScale == 0.9,
                "v2 的旧默认缩放应迁移到 0.9: " + v2.scale + "/" + v2.teamScale);

        HudConfig.Cfg customScale = new HudConfig.Cfg();
        customScale.configVersion = 2;
        customScale.scale = 1.25;
        HudConfig.migrate("{\"configVersion\":2,\"scale\":1.25}", customScale);
        check(customScale.scale == 1.25, "自己调过的缩放不该被迁移覆盖: " + customScale.scale);

        // 表驱动后仍保持旧顺序 —— 先做 v7 的跨字段复制，再由 v21 把仍为 20 的 containerY
        // 改成「屏幕高 ÷ 3」哨兵（如果顺序反了，复制会把哨兵又覆盖回 20）。
        HudConfig.Cfg v5 = new HudConfig.Cfg();
        v5.configVersion = 5;
        v5.selfStatsY = 20;
        HudConfig.migrate("{\"configVersion\":5}", v5);
        check(v5.containerY == HudConfig.DEFAULT_Y_SENTINEL,
                "v5 时 containerY 应从复制值 20 再落成哨兵: " + v5.containerY);
        check(v5.selfStatsY == 20, "v20 只搬 40，v5 的 20 不该被动: " + v5.selfStatsY);

        HudConfig.Cfg v3 = new HudConfig.Cfg();
        v3.configVersion = 3;
        v3.scale = 1.0;
        HudConfig.migrate("{\"configVersion\":3,\"scale\":1.0}", v3);
        check(v3.scale == 1.0, "已经是 v3 的配置不该再迁移: " + v3.scale);

        // 下限 0.10（原来 0.50）；血条 / 文字各自映射，alpha 合法区间仍是 26~255。
        for (double op = 0.05; op <= 1.2; op += 0.05) {
            HudConfig.Cfg t = new HudConfig.Cfg();
            t.barOpacity = op;
            t.textOpacity = op;
            HudConfig.normalize(t);
            int barAlpha = (int) Math.round(255.0 * t.barOpacity);
            int textAlpha = (int) Math.round(255.0 * t.textOpacity);
            check(barAlpha >= 26 && barAlpha <= 255, "barOpacity=" + op + " 映射出 alpha=" + barAlpha);
            check(textAlpha >= 26 && textAlpha <= 255, "textOpacity=" + op + " 映射出 alpha=" + textAlpha);
        }
    }

    // ── 4. 护甲边界 ──

    private static void armorEdgeCases() {
        HudLayout.Options o = new HudLayout.Options();
        o.showNumbers = false;
        o.showArmor = true;
        o.barWidth = 64;

        run("超长护甲数组", new Data("超长", 1, 1, false, 0,
                new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}), o);
        run("空护甲数组", new Data("空", 1, 1, false, 0, new int[0]), o);
        run("null 护甲数组", new Data("null", 1, 1, false, 0, null), o);
        HudLayout.Result r2 = run("只有防御", new Data("防御", 1, 1, false, 7,
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

        // 文字行：红心 + **一种**其它心（由 HeartCycle 挑出来，这里直接指定 cycleKind）。
        HudLayout.Options o = new HudLayout.Options();
        o.showArmor = false;
        o.showBar = false;
        o.cycleKind = HeartCounts.SOUL;
        HudLayout.Result r = run("心数行", new Data("两色", 160, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        String joined = join(r);
        check(joined.equals("❤100/100 ❤60"), "心数行内容=" + joined);
        check(!joined.contains("|"), "同一条心数行不该有分隔符=" + joined);
        check(r.rowCount() == 1, "没有护甲时只有一行心数: " + r.rowCount());

        HudLayout.Result single = run("单色", new Data("单红", 80, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        check(join(single).equals("❤80/100"), "红心应写成 当前/上限: " + join(single));

        // 上限未知（MaxHP<=0）时只写当前值。
        HudLayout.Result noCap = HudLayout.compute(70, 0, false, 0, new int[7],
                HeartCounts.of(70, 0, 0, 0, 0), o, SelfCheck::width);
        check(join(noCap).equals("❤70"), "上限未知时不该写 /0: " + join(noCap));

        // 敌人：碎心/粉心/蓝心也走轮换，每次只显示一种，固定一行。
        o.cycleKind = HeartCounts.CRACKED;
        HudLayout.Result crackedLine = HudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(50, 0, 0, 0, 5), o, SelfCheck::width);
        check(crackedLine.rowCount() == 1, "敌人心数固定一行: " + crackedLine.rowCount());
        check(rowText(crackedLine, 0).equals("❤50/50 ❤5"),
                "轮换到碎心时应显示: " + rowText(crackedLine, 0));
        o.cycleKind = HeartCounts.PINK;
        HudLayout.Result pinkLine = HudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(50, 0, 0, 0, -5), o, SelfCheck::width);
        check(rowText(pinkLine, 0).equals("❤50/50 ❤5"),
                "轮换到粉心时应显示: " + rowText(pinkLine, 0));
        check(HeartCounts.COLORS[HeartCounts.CRACKED] == 0xAA0000, "碎心配色应为 dark_red");
        check(HeartCounts.COLORS[HeartCounts.PINK] == 0xFF55FF, "粉心配色应为 light_purple");

        o.cycleKind = HeartCounts.BLUE;
        HudLayout.Result blueLine = HudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(30, 0, 0, 20, 0), o, SelfCheck::width);
        check(rowText(blueLine, 0).equals("❤30/50 ❤20"),
                "轮换到蓝心时应显示: " + rowText(blueLine, 0));

        // 轮换到该类心不存在时，只显示红心。
        HudLayout.Result noBlue = HudLayout.compute(50, 50, false, 0, new int[7],
                HeartCounts.of(50, 0, 0, 0, 0), o, SelfCheck::width);
        check(rowText(noBlue, 0).equals("❤50/50"), "不存在的类型不该显示: " + rowText(noBlue, 0));
        o.cycleKind = HeartCounts.SOUL;

        // 拆不出心数（percentOnly）→ 显示百分比数字；已知当前/上限但拆不出心数 → 当前/最大。
        HudLayout.Result fallback = run("回退", new Data("只有百分比", 73, 100, true, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        check(join(fallback).equals("❤ 73%"), "只有百分比时应显示 ❤ 73%: " + join(fallback));

        HudLayout.Options noHearts = new HudLayout.Options();
        noHearts.showHearts = false;
        noHearts.showArmor = false;
        noHearts.showBar = false;
        HudLayout.Result numbers = run("关掉心数", new Data("普通", 160, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), noHearts);
        check(join(numbers).equals("❤ 160/100"), "关掉心数应退回「❤ 当前/上限」: " + join(numbers));

        // 护甲在最上面一行，🛡 与元素护甲之间**两个空格**。
        HudLayout.Options withArmor = new HudLayout.Options();
        withArmor.showBar = false;
        withArmor.cycleKind = HeartCounts.SOUL;
        HudLayout.Result narrowArmor = run("窄护甲", new Data("一个护甲", 100, 100, false, 0,
                new int[]{3, 0, 0, 0, 0, 0, 0}), withArmor);
        check(narrowArmor.rowCount() == 2, "有护甲时固定两行: " + narrowArmor.rowCount());
        check(rowText(narrowArmor, 0).equals("\uD83D\uDD253"), "护甲行在最上面: " + rowText(narrowArmor, 0));
        check(rowText(narrowArmor, 1).equals("❤100/100"), "心数行在护甲下面: " + rowText(narrowArmor, 1));
        check(!join(narrowArmor).contains("|"), "分成两行之后不再需要分隔符: " + join(narrowArmor));

        HudLayout.Result armored = run("心数+护甲", new Data("带护甲", 160, 100, false, 3,
                new int[]{0, 5, 0, 0, 0, 0, 0}), withArmor);
        check(armored.rowCount() == 2, "有护甲时固定两行: " + armored.rowCount());
        check(rowText(armored, 0).equals("\uD83D\uDEE13  " + EnemyData.ARMOR_ICONS[1] + "5"),
                "护甲与元素护甲之间应有两个空格: " + rowText(armored, 0));
        check(rowText(armored, 1).equals("❤100/100 ❤60"),
                "第二行应是红心 + 一种轮换心: " + rowText(armored, 1));
        check(armored.rows.get(0).top == HudLayout.TEXT_TOP - HudLayout.ROW_STEP,
                "护甲行要再抬 " + HudLayout.ROW_STEP + " 像素");
        check(armored.rows.get(1).top == HudLayout.TEXT_TOP, "心数行贴着 TEXT_TOP（紧贴血条）");
        check(armored.textTop == HudLayout.TEXT_TOP, "textTop 仍指最底一行");
        check(armored.rows.get(0).isArmourRow(), "第一行应被标记为护甲行");
        check(!armored.rows.get(1).isArmourRow(), "第二行不是护甲行");
        // 行距正好等于背景框高度：两行背景相邻但**不重叠**（原版 y-1 ~ y+9 共 10 像素）。
        check(HudLayout.TEXT_BOX_HEIGHT == 10, "背景框高度应为 10 像素");
        check(armored.rows.get(0).top + HudLayout.TEXT_BOX_HEIGHT == armored.rows.get(1).top,
                "两行背景框应正好相邻，不留缝也不重叠");

        // 敌人有 5 类心时，心数行也只出现「当前那一种」。
        HudLayout.Options oneKind = new HudLayout.Options();
        oneKind.showBar = false;
        oneKind.cycleKind = HeartCounts.BLACK;
        HudLayout.Result one = HudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), oneKind, SelfCheck::width);
        check(one.rowCount() == 1, "敌人心数固定一行: " + one.rowCount());
        check(rowText(one, 0).equals("❤60/150 ❤30"),
                "敌人只显示一种轮换心: " + rowText(one, 0));

        // 玩家排版（playerStyle）：碎心/粉心 + 蓝心固定在最上面一行，心数行只轮换魂/黑。
        HudLayout.Options teamLike = new HudLayout.Options();
        teamLike.showHearts = true;
        teamLike.showArmor = true;
        teamLike.showBar = false;
        teamLike.playerStyle = true;
        teamLike.cycleKind = HeartCounts.SOUL;

        HudLayout.Result teamNoHearts = HudLayout.compute(73, 120, false, 0, new int[7],
                HeartCounts.unknown(), teamLike, SelfCheck::width);
        check(join(teamNoHearts).equals("❤ 73/120"),
                "队友拿不到心数时应退回当前/最大: " + join(teamNoHearts));

        HudLayout.Result teamHearts = HudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), teamLike, SelfCheck::width);
        check(teamHearts.rowCount() == 2, "玩家：上排魂/黑/蓝 + 下排红心/碎心，共两行: " + teamHearts.rowCount());
        check(rowText(teamHearts, 0).equals("❤40 ❤30 ❤20"),
                "玩家最上面一行 = 魂心 + 黑心 + 蓝心: " + rowText(teamHearts, 0));
        check(rowText(teamHearts, 1).equals("❤60/150 ❤2"),
                "玩家下排 = 红心 当前/上限 + 碎心: " + rowText(teamHearts, 1));

        teamLike.cycleKind = HeartCounts.BLACK;
        HudLayout.Result teamBlack = HudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), teamLike, SelfCheck::width);
        check(rowText(teamBlack, 1).equals("❤60/150 ❤2"),
                "玩家不再轮换（cycleKind 不影响）: " + rowText(teamBlack, 1));

        teamLike.cycleKind = HeartCounts.SOUL;
        HudLayout.Result teamPlain = HudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 0, 0, 0), teamLike, SelfCheck::width);
        check(teamPlain.rowCount() == 2, "玩家有魂心时仍是两行: " + teamPlain.rowCount());
        check(rowText(teamPlain, 0).equals("❤40"),
                "玩家上排只有魂心: " + rowText(teamPlain, 0));
        check(rowText(teamPlain, 1).equals("❤60/150"),
                "玩家下排只有红心: " + rowText(teamPlain, 1));

        // 玩家排版下，红心与轮换心的顺序固定：红心在前。
        check(rowText(teamHearts, 1).startsWith("❤60/150"), "红心永远在最前面");

        // 队友**只画心与血条**，护甲段一律不画 —— 即使 payload 带了 defence / 元素护甲。
        HudLayout.Options teamNoArmor = new HudLayout.Options();
        teamNoArmor.showHearts = true;
        teamNoArmor.showArmor = false;
        teamNoArmor.showBar = true;
        teamNoArmor.barWidth = 100;
        teamNoArmor.playerStyle = true;
        teamNoArmor.cycleKind = HeartCounts.SOUL;

        HudLayout.Result teamArmored = HudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                teamNoArmor, SelfCheck::width);
        String teamFlat = join(teamArmored);
        check(!teamFlat.contains("\uD83D\uDEE1"), "队友不该出现 🛡 护甲段: " + teamFlat);
        check(!teamFlat.contains(EnemyData.ARMOR_ICONS[1]), "队友不该出现 🔥 元素护甲段: " + teamFlat);
        check(!teamFlat.contains("|"), "队友不该出现护甲分隔符: " + teamFlat);
        // join() 把几行首尾相接，去掉空格再比（玩家：特殊心行 + 红心/轮换心行）。
        check(teamFlat.replace(" ", "").equals("❤40❤30❤20❤60/150❤2"),
                "队友这几行只应有心: " + teamFlat);
        check(teamArmored.barWidth == 100, "队友的血条仍要画: " + teamArmored.barWidth);

        // 同样的数据给敌人：护甲在最上面，心数行只有一种轮换心。
        HudLayout.Options enemyArmor = new HudLayout.Options();
        enemyArmor.showHearts = true;
        enemyArmor.showArmor = true;
        enemyArmor.showBar = false;
        enemyArmor.barWidth = 100;
        enemyArmor.cycleKind = HeartCounts.BLACK;
        HudLayout.Result mobArmored = HudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                enemyArmor, SelfCheck::width);
        check(rowText(mobArmored, 0).equals("\uD83D\uDEE15  \uD83D\uDD253 \u26031"),
                "敌人护甲行（两个空格 + 元素护甲）: " + rowText(mobArmored, 0));
        check(rowText(mobArmored, 1).equals("❤60/150 ❤30"),
                "敌人心数行只显示一种轮换心: " + rowText(mobArmored, 1));
        // 用转义写死期望值：🛡 在源码里常被写成「🛡+U+FE0F」，直接比字面量会踩变体选择符。
        check(join(armored).contains("\uD83D\uDEE13"), "防御段缺失: " + join(armored));
        check(join(armored).contains(EnemyData.ARMOR_ICONS[1] + "5"), "元素护甲段缺失: " + join(armored));
    }

    // ── 4b. 元素护甲翻页 ──

    private static void armourPaging() {
        // 🔥3 ☃1 ☠2 ☀4 ⚓6 = 5 项 → 2 + 2 + 1 三页
        int[] five = {3, 1, 2, 4, 0, 6, 0};
        check(HudLayout.armourPageCount(five) == 3,
                "5 种护甲应分 3 页: " + HudLayout.armourPageCount(five));
        check(HudLayout.armourPageCount(new int[]{5, 0, 0, 0, 0, 0, 0}) == 1, "1 种护甲应 1 页");
        check(HudLayout.armourPageCount(new int[7]) == 0, "没有护甲应 0 页");
        check(HudLayout.armourPageCount(null) == 0, "护甲为 null 应 0 页");
        check(HudLayout.clampArmourPage(-3, 3) == 0, "页号过小应夹到 0");
        check(HudLayout.clampArmourPage(9, 3) == 2, "页号过大应夹到最后一页");
        check(HudLayout.clampArmourPage(1, 0) == 0, "没有页时返回 0");

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
            HudLayout.Options o = new HudLayout.Options();
            o.showHearts = true;
            o.showArmor = true;
            o.showBar = false;
            o.armourPage = page;
            o.cycleKind = HeartCounts.SOUL;
            HudLayout.Result r = HudLayout.compute(150, 150, false, 5, five,
                    HeartCounts.of(60, 40, 0, 0, 0), o, SelfCheck::width);
            String text = join(r);
            String armour = rowText(r, 0);
            check(r.rowCount() == 2, "有护甲时应固定两行: " + r.rowCount());
            check(r.rows.get(0).isArmourRow(), "第一行应是护甲行");
            check(armour.contains("\uD83D\uDEE15"), "第 " + page + " 页应始终带 🛡5: " + armour);
            check(armour.contains(pageArmour[page]),
                    "第 " + page + " 页应显示 " + pageArmour[page] + ": " + armour);
            check(rowText(r, 1).equals("❤60/150 ❤40"), "第二行应是心数: " + rowText(r, 1));
            check(text.contains("❤60/150"), "心数行始终在: " + text);
        }

        HudLayout.Options p0 = new HudLayout.Options();
        p0.showHearts = true;
        p0.showArmor = true;
        p0.showBar = false;
        p0.armourPage = 0;
        p0.cycleKind = HeartCounts.SOUL;
        String zero = join(HudLayout.compute(150, 150, false, 5, five,
                HeartCounts.of(60, 40, 0, 0, 0), p0, SelfCheck::width));
        for (int i = 1; i < otherPages.length; i++) {
            check(!zero.contains(otherPages[i]),
                    "第 0 页不该出现其它页的护甲 " + otherPages[i] + ": " + zero);
        }

        // 页号越界时夹回范围内，不会画空。
        p0.armourPage = 99;
        String clamped = join(HudLayout.compute(150, 150, false, 5, five,
                HeartCounts.of(60, 40, 0, 0, 0), p0, SelfCheck::width));
        check(clamped.contains("\u26936"), "页号越界应夹到最后一页: " + clamped);
    }

    // ── 4c. 心类轮换 ──

    private static void heartCycling() {
        HeartCounts enemy = HeartCounts.of(60, 40, 30, 20, 2);   // 魂/黑/蓝/碎 四种
        check(HeartCycle.presentCount(enemy, false) == 4,
                "敌人可轮换心类=4: " + HeartCycle.presentCount(enemy, false));
        check(HeartCycle.presentCount(enemy, true) == 2,
                "玩家可轮换心类=2（魂/黑）: " + HeartCycle.presentCount(enemy, true));
        check(HeartCycle.presentCount(null, false) == 0, "hearts=null 时没有可轮换的");
        check(HeartCycle.presentCount(HeartCounts.of(50, 0, 0, 0, 0), false) == 0,
                "只有红心时没有可轮换的");
        check(HeartCycle.pick(null, false, 0, 0) == -1, "hearts=null → -1");

        long ms = HeartCycle.KIND_MS;
        check(HeartCycle.pick(enemy, false, 0, 0) == HeartCounts.SOUL, "t=0 敌人应显示魂心");
        check(HeartCycle.pick(enemy, false, ms, 0) == HeartCounts.BLACK, "t=1 敌人应显示黑心");
        check(HeartCycle.pick(enemy, false, ms * 2, 0) == HeartCounts.BLUE, "t=2 敌人应显示蓝心");
        check(HeartCycle.pick(enemy, false, ms * 3, 0) == HeartCounts.CRACKED, "t=3 敌人应显示碎心");
        check(HeartCycle.pick(enemy, false, ms * 4, 0) == HeartCounts.SOUL, "t=4 敌人应回到魂心");
        check(HeartCycle.pick(enemy, false, ms, 1) == HeartCounts.BLUE, "salt=1 应错开一格");

        check(HeartCycle.pick(enemy, true, 0, 0) == HeartCounts.SOUL, "t=0 玩家应显示魂心");
        check(HeartCycle.pick(enemy, true, ms, 0) == HeartCounts.BLACK, "t=1 玩家应显示黑心");
        check(HeartCycle.pick(enemy, true, ms * 2, 0) == HeartCounts.SOUL, "t=2 玩家应回到魂心");

        // 粉心单独存在时也能轮到。
        HeartCounts pink = HeartCounts.of(50, 0, 0, 0, -5);
        check(HeartCycle.pick(pink, false, 0, 0) == HeartCounts.PINK, "只有粉心时应显示粉心");

        // 消失的类型跳过（只有魂 + 蓝 → 两步循环）。
        HeartCounts two = HeartCounts.of(50, 10, 0, 20, 0);
        check(HeartCycle.presentCount(two, false) == 2, "魂+蓝 = 2 种");
        check(HeartCycle.pick(two, false, 0, 0) == HeartCounts.SOUL, "两步循环第一步魂");
        check(HeartCycle.pick(two, false, ms, 0) == HeartCounts.BLUE, "两步循环第二步蓝");
        check(HeartCycle.pick(two, false, ms * 2, 0) == HeartCounts.SOUL, "两步循环回到魂");
    }

    // ── 4d. 大数值缩写 ──

    private static void bigNumbers() {
        check(HudLayout.num(0).equals("0"), "num(0)");
        check(HudLayout.num(100).equals("100"), "num(100)=" + HudLayout.num(100));
        check(HudLayout.num(900).equals("900"), "num(900) 不该缩写成 0.9K: " + HudLayout.num(900));
        check(HudLayout.num(999).equals("999"), "num(999)=" + HudLayout.num(999));
        check(HudLayout.num(1000).equals("1K"), "num(1000)=" + HudLayout.num(1000));
        check(HudLayout.num(1100).equals("1.1K"), "num(1100)=" + HudLayout.num(1100));
        check(HudLayout.num(24999).equals("24.9K"), "num(24999) 应截断成 24.9K: " + HudLayout.num(24999));
        check(HudLayout.num(99_999).equals("99.9K"), "num(99999)=" + HudLayout.num(99_999));
        check(HudLayout.num(100_000).equals("100K"), "num(100000)=" + HudLayout.num(100_000));
        check(HudLayout.num(123_456).equals("123.4K"), "num(123456)=" + HudLayout.num(123_456));
        check(HudLayout.num(999_999).equals("999.9K"), "num(999999)=" + HudLayout.num(999_999));
        check(HudLayout.num(1_100_000).equals("1.1M"), "num(1100000)=" + HudLayout.num(1_100_000));
        check(HudLayout.num(2_100_000).equals("2.1M"), "num(2100000)=" + HudLayout.num(2_100_000));
        check(HudLayout.num(12_345_678).equals("12.3M"), "num(12345678)="
                + HudLayout.num(12_345_678));
        check(HudLayout.num(999_999_999).equals("999.9M"), "num(999999999)="
                + HudLayout.num(999_999_999));
        check(HudLayout.num(Integer.MAX_VALUE).equals("2.1B"), "num(MAX)="
                + HudLayout.num(Integer.MAX_VALUE));
        check(HudLayout.num(Integer.MIN_VALUE).startsWith("-"), "num(MIN) 应带负号: "
                + HudLayout.num(Integer.MIN_VALUE));
        check(HudLayout.num(-123_456).equals("-123.4K"), "num(-123456)="
                + HudLayout.num(-123_456));

        // 缩写的目的是行宽可控：五段 999999 以前约 40 个字符，现在每段 4~5 个。
        HudLayout.Options o = new HudLayout.Options();
        o.showHearts = true;
        o.showBar = false;
        o.barWidth = 100;
        o.cycleKind = HeartCounts.SOUL;
        String text = join(HudLayout.compute(999_999, 999_999, false, 0, new int[7],
                HeartCounts.of(999_999, 999_999, 999_999, 999_999, 999_999), o, SelfCheck::width));
        check(!text.contains("999999"), "大数值应先缩写: " + text);
        check(text.contains("999.9K/999.9K"), "大数值缩写格式: " + text);
        check(text.length() < 60, "缩写后整行不该太长: " + text.length() + " -> " + text);

        // 玩家头顶的心数**不缩写**（魂/黑/蓝/碎/粉都照原样写）
        HudLayout.Options p2 = new HudLayout.Options();
        p2.showArmor = false;
        p2.showBar = false;
        p2.playerStyle = true;
        p2.cycleKind = HeartCounts.SOUL;
        String pt = join(HudLayout.compute(12_345, 12_345, false, 0, new int[7],
                HeartCounts.of(12_345, 10_000, 0, 0, 0), p2, SelfCheck::width));
        check(pt.contains("12.3K") && pt.contains("10K"), "玩家心数也要缩写: " + pt);
        check(!pt.contains("12345") && !pt.contains("10000"), "玩家心数不该出现原样大数: " + pt);

        // 红 / 魂 / 黑 / 蓝 用 ❤；碎 / 粉用地图资源包的裂心字形 U+E026（颜色区分）。
        for (int i = 0; i <= HeartCounts.BLUE; i++) {
            check(HeartCounts.ICONS[i].equals("\u2764"), "第 " + i + " 类心应使用 ❤");
        }
        check(HeartCounts.ICONS[HeartCounts.CRACKED].equals("\uE026"), "碎心应使用地图裂心字形");
        check(HeartCounts.ICONS[HeartCounts.PINK].equals("\uE026"), "粉心应使用地图裂心字形");
        // 没资源包时退回 ❤，有资源包时用裂心字形（两种状态都测）
        HeartCounts.setCrackedGlyphAvailable(false);
        check(HeartCounts.icon(HeartCounts.CRACKED).equals("\u2764"), "无资源包时碎心应退回 ❤");
        check(HeartCounts.icon(HeartCounts.RED).equals("\u2764"), "红心永远用 ❤");
        HeartCounts.setCrackedGlyphAvailable(true);
        check(HeartCounts.icon(HeartCounts.CRACKED).equals("\uE026"), "有资源包时碎心用裂心字形");
        check(HeartCounts.icon(HeartCounts.PINK).equals("\uE026"), "有资源包时粉心用裂心字形");
        HeartCounts.setCrackedGlyphAvailable(false);

        // 不透明度拆成「血条」与「文字」两条，各自 0.10 ~ 1.00，越界夹回。
        HudConfig.Cfg low = new HudConfig.Cfg();
        low.barOpacity = 0.05;
        low.textOpacity = 0.02;
        low.teamBarOpacity = 0.0;
        low.teamTextOpacity = 0.04;
        HudConfig.normalize(low);
        check(low.barOpacity == 0.1 && low.textOpacity == 0.1 && low.teamBarOpacity == 0.1
                        && low.teamTextOpacity == 0.1,
                "四条不透明度都应夹到 0.10: " + low.barOpacity + "/" + low.textOpacity
                        + "/" + low.teamBarOpacity + "/" + low.teamTextOpacity);
        HudConfig.Cfg kept = new HudConfig.Cfg();
        kept.barOpacity = 0.4;
        kept.textOpacity = 0.9;
        HudConfig.normalize(kept);
        check(kept.barOpacity == 0.4 && kept.textOpacity == 0.9,
                "夹取范围内的值应原样保留: " + kept.barOpacity + "/" + kept.textOpacity);

        // 真盔甲不参与翻页，常驻在 🛡 防御后面
        int[] onlyTrue = new int[EnemyData.ARMOR_OBJECTIVES.length];
        onlyTrue[EnemyData.TRUE_ARMOR_INDEX] = 20;
        check(HudLayout.armourPageCount(onlyTrue) == 0, "真盔甲不该占翻页名额");
        HudLayout.Options p3 = new HudLayout.Options();
        p3.showArmor = true;
        p3.showBar = false;
        p3.showHearts = false;
        String trueTop = join(HudLayout.compute(100, 100, false, 0, onlyTrue,
                HeartCounts.forMob(100, 100), p3, SelfCheck::width));
        check(trueTop.contains("20"), "只有真盔甲时也应显示: " + trueTop);
        // 没有 🛡 防御、只有真·防御时，行首不能带空格
        check(!trueTop.startsWith(" "), "没有防御时护甲行不该以空格开头: [" + trueTop + "]");
        // 有防御时，真·防御前面仍要有分隔空格
        String defTrue = join(HudLayout.compute(100, 100, false, 7, onlyTrue,
                HeartCounts.forMob(100, 100), p3, SelfCheck::width));
        check(defTrue.startsWith("\uD83D\uDEE17 "), "防御后面应有一个空格再跟真·防御: " + defTrue);
        int[] bothArmor = new int[EnemyData.ARMOR_OBJECTIVES.length];
        bothArmor[0] = 10;                       // 火
        bothArmor[EnemyData.TRUE_ARMOR_INDEX] = 20;
        check(HudLayout.armourPageCount(bothArmor) == 1, "真盔甲不算页后只剩 1 页: "
                + HudLayout.armourPageCount(bothArmor));
    }

    /** 第 index 行拼起来的文字（0 = 最上面那行）。 */
    private static String rowText(HudLayout.Result r, int index) {
        if (r == null || index < 0 || index >= r.rows.size()) return "";
        StringBuilder sb = new StringBuilder();
        for (HudLayout.Line l : r.rows.get(index).lines) sb.append(l.text);
        return sb.toString();
    }

    private static String join(HudLayout.Result r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        for (HudLayout.Line l : r.lines) sb.append(l.text);
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

        // 单包上限 32 → 64 条（玩家优先），协议版本跟着升到 2；
        // 旧客户端（v1）收到 v2 会整包忽略（ClientMobHealthCache.accept），安全降级。
        check(MobHealthPayload.MAX_ENTRIES == 64, "单包上限应为 64 条: " + MobHealthPayload.MAX_ENTRIES);
        check(MobHealthPayload.CURRENT_VERSION == 4, "协议版本应为 3: " + MobHealthPayload.CURRENT_VERSION);

        // 1.4.5 调试功能「筛选实体数量」：客户端只保留前 N 条，保持服务端顺序（玩家优先）。
        MobHealthPayload.Entry e1 = new MobHealthPayload.Entry(java.util.UUID.randomUUID(),
                net.minecraft.network.chat.Component.literal("mob"),
                10, 10, 0, 0, 0, 0, 0, 0, 0, 0, new int[7], false);
        MobHealthPayload.Entry e2 = new MobHealthPayload.Entry(java.util.UUID.randomUUID(),
                net.minecraft.network.chat.Component.literal("mob"),
                20, 20, 0, 0, 0, 0, 0, 0, 0, 0, new int[7], false);
        MobHealthPayload.Entry e3 = new MobHealthPayload.Entry(java.util.UUID.randomUUID(),
                net.minecraft.network.chat.Component.literal("mob"),
                30, 30, 0, 0, 0, 0, 0, 0, 0, 0, new int[7], false);
        java.util.List<MobHealthPayload.Entry> three = java.util.List.of(e1, e2, e3);
        java.util.List<MobHealthPayload.Entry> capped = ClientMobHealthCache.cap(three, 2);
        check(capped.size() == 2 && capped.get(0) == e1 && capped.get(1) == e2,
                "筛选数量应只留前 N 条且保持顺序");
        check(ClientMobHealthCache.cap(three, 0).size() == 1, "筛选数量 0 应夹到 1");
        check(ClientMobHealthCache.cap(three, 999).size() == 3, "筛选数量超大时应全保留（服务端只推了 3 条）");
        check(ClientMobHealthCache.cap(java.util.List.of(), 5).isEmpty(), "空包截断后仍为空");

        // payload 的 AllHearts 缺失时（新刷的怪还没被 tag=CTTAll 补算），客户端用百分比兜底 ——
        // 显示「❤ 73%」+ 百分比条，而不是「0/上限」+ 空条。
        EnemyData.Snapshot pctOnly = EnemyData.percentOnly(73, 3,
                new int[]{1, 0, 0, 0, 0, 0, 0});
        check(pctOnly.percentOnly, "percentOnly 快照应标记 percentOnly");
        check(pctOnly.hp == 73 && pctOnly.maxHp == 100, "percentOnly hp/maxHp="
                + pctOnly.hp + "/" + pctOnly.maxHp);
        check(Math.abs(pctOnly.ratio - 0.73f) < 1e-6, "percentOnly ratio=" + pctOnly.ratio);
        check(pctOnly.defence == 3 && pctOnly.armor[0] == 1, "percentOnly 护甲应保留");
        check(!pctOnly.hearts.any(), "percentOnly 心数应为未知");
        check(EnemyData.percentOnly(-5, 0, new int[7]).percent == 0, "percentOnly 负值应夹到 0");
        check(EnemyData.percentOnly(150, 0, new int[7]).percent == 100, "percentOnly 超 100 应夹到 100");

        // 残血时血条至少 1 px（整数分数下 1/325 = 0.3%，乘条宽 round 后会是 0）
        check(HudLayout.fillPixels(75, 0.0f) == 0, "比例 0 不应有填充");
        check(HudLayout.fillPixels(75, -1.0f) == 0, "负比例不应有填充");
        check(HudLayout.fillPixels(75, 1.0f) == 75, "满血应填满");
        check(HudLayout.fillPixels(75, 0.5f) == 38, "半血约 38 px: " + HudLayout.fillPixels(75, 0.5f));
        check(HudLayout.fillPixels(75, 1.0f / 325.0f) == 1,
                "1/325 应至少 1 px: " + HudLayout.fillPixels(75, 1.0f / 325.0f));
        check(HudLayout.fillPixels(0, 1.0f) == 0, "宽度 0 不应有填充");

        // NPC 抑制列表 —— 服务端说「别画」的实体，客户端只隐藏、不画
        ClientMobHealthCache.reset();
        java.util.UUID npcId = java.util.UUID.randomUUID();
        ClientMobHealthCache.accept(new MobHealthPayload(MobHealthPayload.CURRENT_VERSION,
                java.util.List.of(e1), java.util.List.of(npcId)));
        check(ClientMobHealthCache.isSuppressed(npcId), "抑制列表里的实体应被判为抑制");
        check(!ClientMobHealthCache.isSuppressed(e1.uuid()), "普通怪不应被抑制");
        check(ClientMobHealthCache.lookup(e1.uuid()) != null, "普通怪仍应有 payload");
        check(ClientMobHealthCache.lookup(npcId) == null, "被抑制的 NPC 不在数据表里");
        ClientMobHealthCache.reset();
        check(!ClientMobHealthCache.isSuppressed(npcId), "reset 后抑制表应清空");

        // Psy 只按**翻译键**判定（去掉末尾不可见字符 U+E007 后精确比较）
        check(HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Psy")),
                "键名 Psy 应被隐藏");
        check(HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Psy\uE007")),
                "键名 Psy + U+E007 应被隐藏");
        check(HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Psy\uE007\uE007\uE007")),
                "键名 Psy + 多个 U+E007 也应被隐藏");
        check(HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Psy_2XXX")),
                "键名 Psy_2XXX 应被隐藏");
        check(HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Rockstar_Psy")),
                "键名 Rockstar_Psy 应被隐藏");
        check(HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Rockstar Psy")),
                "键名 Rockstar Psy 应被隐藏");
        // 负向：必须精确匹配（改成前缀匹配就会误伤地图的物品名键）
        check(!HudTarget.hidesByName(net.minecraft.network.chat.Component.translatable("Psys Hat")),
                "物品名键 Psys Hat 不该被隐藏");
        // 负向：字面量不参与匹配（只看键名，与显示语言无关）
        check(!HudTarget.hidesByName(net.minecraft.network.chat.Component.literal("Psy")),
                "字面量 Psy 不该被隐藏");
        check(!HudTarget.hidesByName(null), "null 名字不该被隐藏");

        // 同名分身组统一显示 —— 组内取最低百分比（分身 100、真身掉血后更低）
        check(EnemyData.minPercent(-1, 80) == 80, "只有一侧有值时取那一侧");
        check(EnemyData.minPercent(80, -1) == 80, "只有一侧有值时取那一侧（反序）");
        check(EnemyData.minPercent(80, 100) == 80, "应取较低的 80");
        check(EnemyData.minPercent(100, 80) == 80, "应取较低的 80（反序）");
        check(EnemyData.minPercent(-1, -1) == -1, "都没有时返回 -1");
        check(EnemyData.minPercent(0, 100) == 0, "0 也是合法百分比");
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

    private static HudLayout.Result run(String tag, Data d, HudLayout.Options o) {
        try {
            HudLayout.Result r = HudLayout.compute(
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

    // ── 血条分段（端盖原尺寸 + 中段拉伸） ──

    private static void barSlicing() {
        final float nw = BossBarTexture.NATIVE_WIDTH;
        final float cap = BossBarTexture.CAP_TEXELS;

        check(nw == 182 && BossBarTexture.NATIVE_HEIGHT == 5, "原版 boss_bar 纹理应为 182x5");
        check(cap * 2 < HudLayout.BAR_WIDTH_MIN,
                "两个端盖加起来必须短于最短的条: cap=" + cap);
        check(BossBarTexture.NATIVE_HEIGHT == HudLayout.BAR_HEIGHT,
                "纹理高 5 纹素应正好等于条的布局高度，否则端盖会被压扁");

        // ① 整条背景：左右端盖都是 4 纹素宽、UV 贴住纹理两端，中段吃长度差
        for (int bw = HudLayout.BAR_WIDTH_MIN; bw <= HudLayout.BAR_WIDTH_MAX; bw++) {
            float xa = -bw / 2.0f;
            float xb = xa + bw;
            String tag = "背景 bw=" + bw;
            float[][] seg = BossBarTexture.slices(xa, xb, 0.0f, nw);
            check(seg.length == 3, tag + " 应切成 3 段，实际 " + seg.length);
            if (seg.length != 3) continue;
            check(seg[0][0] == xa && seg[0][1] == xa + cap, tag + " 左端盖位置/宽度不对");
            check(seg[0][2] == 0.0f && seg[0][3] == cap, tag + " 左端盖 UV 不对");
            check(seg[2][0] == xb - cap && seg[2][1] == xb, tag + " 右端盖位置/宽度不对");
            check(seg[2][2] == nw - cap && seg[2][3] == nw, tag + " 右端盖 UV 不对");
            check(seg[0][1] == seg[1][0], tag + " 左端盖与中段之间有空隙或重叠");
            check(seg[1][1] == seg[2][0], tag + " 中段与右端盖之间有空隙或重叠");
            check(seg[1][2] == cap && seg[1][3] == nw - cap, tag + " 中段 UV 不对");
            check(seg[2][1] - seg[2][0] == seg[0][1] - seg[0][0], tag + " 两端端盖宽度应相同");
        }

        // ② 进度满格时 ub = 182 → 露出右端盖（和原版 blitSprite 满格时一样是圆的）
        for (int bw = HudLayout.BAR_WIDTH_MIN; bw <= HudLayout.BAR_WIDTH_MAX; bw++) {
            float xa = -bw / 2.0f;
            float xb = xa + bw;
            float[][] seg = BossBarTexture.slices(xa, xb, 0.0f, nw);
            check(seg.length == 3 && seg[2][3] == nw && seg[2][1] == xb,
                    "满格进度 bw=" + bw + " 应露出右端盖");
        }

        // ③ 进度没满 → 左端盖 + 中段，右边界直切在 fill 上、不采纹理最右端
        for (int bw = HudLayout.BAR_WIDTH_MIN; bw <= HudLayout.BAR_WIDTH_MAX; bw++) {
            for (float ratio : new float[]{0.02f, 0.1f, 0.5f, 0.75f, 0.95f}) {
                float fill = Math.round(bw * ratio);
                if (fill <= 0.0f) continue;
                float xa = -bw / 2.0f;
                float ub = nw * Math.min(1.0f, fill / bw);
                String tag = "进度 bw=" + bw + " ratio=" + ratio;
                float[][] seg = BossBarTexture.slices(xa, xa + fill, 0.0f, ub);
                if (ub >= nw) {
                    // 四舍五入后已经满格：这时应该露右端盖，和满格一致
                    check(seg.length == 3, tag + " 已满格应切 3 段");
                    continue;
                }
                if (fill >= cap * 2) {
                    check(seg.length == 2, tag + " 应只有 左端盖 + 中段，实际 " + seg.length);
                    if (seg.length != 2) continue;
                    check(seg[0][3] == cap, tag + " 左端盖 UV 不对");
                    check(seg[1][0] == xa + cap && seg[1][1] == xa + fill,
                            tag + " 进度右边界应正好落在 fill 上");
                    check(seg[1][3] == ub, tag + " 中段 UV 右端应为 ub");
                    check(seg[1][3] < nw, tag + " 没满时不该采到纹理最右端");
                } else {
                    check(seg.length == 1, tag + " 过窄的进度应整段拉伸，实际 " + seg.length + " 段");
                    if (seg.length != 1) continue;
                    check(seg[0][0] == xa && seg[0][1] == xa + fill, tag + " 窄进度范围不对");
                }
            }
        }

        // ④ 边界：条短到放不下两个端盖时整段拉伸；u 不在两端时绝不分段
        float[][] tiny = BossBarTexture.slices(0.0f, cap * 2 - 1.0f, 0.0f, nw);
        check(tiny.length == 1 && tiny[0][0] == 0.0f && tiny[0][1] == cap * 2 - 1.0f,
                "条短于 2×CAP 时应整段拉伸");
        float[][] inner = BossBarTexture.slices(0.0f, 60.0f, 10.0f, 100.0f);
        check(inner.length == 1, "u 不贴纹理两端时不该分段，实际 " + inner.length + " 段");
        check(inner[0][2] == 10.0f && inner[0][3] == 100.0f, "内段 UV 应原样保留");

        // ⑤ 段必须按 x 从左到右排列，否则绘制顺序会乱
        float[][] ordered = BossBarTexture.slices(-50.0f, 50.0f, 0.0f, nw);
        check(ordered.length == 3, "满格背景应切 3 段");
        if (ordered.length == 3) {
            check(ordered[0][0] <= ordered[1][0] && ordered[1][0] <= ordered[2][0],
                    "分段必须按 x 递增排列");
            check(ordered[0][2] <= ordered[1][2] && ordered[1][2] <= ordered[2][2],
                    "分段必须按 u 递增排列");
        }
    }

    // ── 自身属性面板（/trigger ViewStats 抓取 + 布局） ──

    private static void selfStatsAndPanel() {
        // ① 心形 + 颜色 → 心类
        check(SelfStats.heartKindOf("\u2764", SelfStats.RED) == 1, "红心颜色应判为 1");
        check(SelfStats.heartKindOf("\u2764", SelfStats.YELLOW) == 2, "魂心颜色应判为 2");
        check(SelfStats.heartKindOf("\u2764", SelfStats.BLACK) == 3, "黑心颜色应判为 3");
        check(SelfStats.heartKindOf("\u2764", SelfStats.BLUE) == 4, "蓝心颜色应判为 4");
        check(SelfStats.heartKindOf("\u2764", 0xFFFFFF) == 0, "白心不该认成任何一种心");
        check(SelfStats.heartKindOf("\uE026", SelfStats.DARK_RED) == 5, "私有区字形 + 暗红应判为碎心");
        check(SelfStats.heartKindOf("\uE026", SelfStats.LIGHT_PURPLE) == 6, "私有区字形 + 亮紫应判为粉心");
        check(SelfStats.heartKindOf("\uE026", 0xFFFFFF) == 0, "白色私有区字形不该认");

        // ② 「数字+字形」里的数字
        check(SelfStats.amountBefore("50\u2764", 2) == 50, "50❤ 应解析出 50");
        check(SelfStats.amountBefore("0\u2764", 1) == 0, "0❤ 应解析出 0");
        check(SelfStats.amountBefore(" \u2764", 1) == -1, "前面没有数字应返回 -1");
        check(SelfStats.amountBefore("abc\u2764", 3) == -1, "非数字前缀应返回 -1");
        check(SelfStats.amountBefore("123", 0) == -1, "下标 0 应返回 -1");
        check(SelfStats.amountBefore("7 \u2764", 2) == 7, "中间夹空格也应解析出 7");

        // ③ 属性行识别 / 「游戏没开始」
        check(SelfStats.isGameNotStarted("Game not started"), "英文「游戏没开始」应识别");
        check(SelfStats.isGameNotStarted("游戏未开始"), "中文「游戏未开始」应识别");
        check(!SelfStats.isGameNotStarted("50\u2764"), "属性行不该被当成「游戏没开始」");
        check(SelfStats.isViewStatsLine("Misc_View_Stats_1"), "ViewStats 标题行应被识别");
        check(SelfStats.isViewStatsLine("Misc_View_Stats_78"), "ViewStats 计时行应被识别");
        check(!SelfStats.isViewStatsLine("50\u2764"), "属性行不算 ViewStats 标题行");
        check(SelfStats.CAPTURE_TIMEOUT_MS > 0
                && SelfStats.CAPTURE_HARD_MS > SelfStats.CAPTURE_TIMEOUT_MS,
                "采集超时应大于 0 且硬上限大于它");

        // ③.5 进存档 / 进服后先安静 3 s
        check(SelfStats.JOIN_GRACE_MS > 0 && SelfStats.JOIN_GRACE_TICKS > 0,
                "静默时长与实体 tick 门槛都应大于 0");
        check(SelfStats.JOIN_GRACE_MS >= 1_000L, "静默时长不该小于 1 s");
        check(SelfStats.inJoinGrace(1_000L, 4_000L, 100, 20),
                "还没到静默结束时刻 → 先别问");
        check(!SelfStats.inJoinGrace(4_000L, 4_000L, 100, 20),
                "到点且实体已经在 tick → 可以问");
        check(SelfStats.inJoinGrace(5_000L, 4_000L, 5, 20),
                "时间到但实体还没 tick 够 → 仍在静默");
        check(!SelfStats.inJoinGrace(5_000L, 4_000L, 20, 20), "tick 刚好够 → 可以问");
        check(SelfStats.inJoinGrace(5_000L, 4_000L, -1, 20),
                "还没有实体（null）→ 按静默处理");

        // ③.6 端到端：进存档 → 静默窗口里来的属性行不进聊天栏、也不收进面板
        SelfStats.reset();
        SelfStats.onWorldJoined(1_000L);
        check(SelfStats.joinQuietUntilMs() == 4_000L,
                "静默窗口应是进世界后 3 s，实际 " + SelfStats.joinQuietUntilMs());
        check(SelfStats.nextTriggerMs() == 4_000L,
                "第一次自动触发应被推到 3 s 后，实际 " + SelfStats.nextTriggerMs());
        var joinTitle = net.minecraft.network.chat.Component.translatable("Misc_View_Stats_1");
        var joinStat = net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("50")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))))
                .append(net.minecraft.network.chat.Component.literal("\u2764")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))));
        check(SelfStats.onGameMessage(joinTitle, false, 1_500L),
                "静默窗口里的 ViewStats 标题行要吞");
        check(SelfStats.onGameMessage(joinStat, false, 2_000L),
                "静默窗口里的属性行也要吞（这时我们还没问过）");
        check(SelfStats.storedLineCount() == 0,
                "静默窗口里吞掉的行不该进面板，实际 " + SelfStats.storedLineCount() + " 行");
        check(!SelfStats.onGameMessage(net.minecraft.network.chat.Component.literal("hello"), false, 2_000L),
                "静默窗口里普通消息照常显示");
        check(!SelfStats.onGameMessage(joinTitle.copy(), false, 10_000L),
                "窗口之外玩家手动触发的标题照旧显示");
        check(!SelfStats.onGameMessage(joinStat.copy(), false, 10_010L),
                "窗口之外玩家手动触发的属性行照旧显示（1.20.24 的结论不变）");
        SelfStats.reset();
        check(SelfStats.joinQuietUntilMs() == 0L && SelfStats.nextTriggerMs() == 0L,
                "reset 后静默窗口与下次触发时刻都应清零");

        // ④ 端到端：把地图那种「数字 + 彩色心」的 Component 喂进解析器
        SelfStats.reset();
        var redLine = net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("120")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))))
                .append(net.minecraft.network.chat.Component.literal("\u2764")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))));
        int[] h = SelfStats.heartsOf(redLine);
        check(h[1] == 120, "端到端：红心应解析为 120，实际 " + h[1]);
        var multi = net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("40")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.YELLOW))))
                .append(net.minecraft.network.chat.Component.literal("\u2764")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.YELLOW))));
        int[] h2 = SelfStats.heartsOf(multi);
        check(h2[2] == 40, "端到端：魂心应解析为 40，实际 " + h2[2]);
        // 地图的「❤%」是治疗百分比（HealPercent / ExtraHealing），不能当成红心数 ——
        // 以前红 ❤% 会把红心数覆盖成治疗百分比（实测 164 → 10）。
        var healPct = net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("10")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))))
                .append(net.minecraft.network.chat.Component.literal("\u2764%")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))));
        int[] hpPct = SelfStats.heartsOf(healPct);
        check(hpPct[1] == 0, "❤% 不该被当成红心数，实际 " + hpPct[1]);
        // 行尾带空格的正常心数行仍要认
        int[] hSpaced = SelfStats.heartsOf(net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("164")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))))
                .append(net.minecraft.network.chat.Component.literal("\u2764 ")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED)))));
        check(hSpaced[1] == 164, "行尾带空格的 164❤ 应解析为 164，实际 " + hSpaced[1]);
        check(h2[1] == 0 && h2[3] == 0 && h2[4] == 0 && h2[5] == 0 && h2[6] == 0,
                "没出现的颜色应保持 0");
        SelfStats.reset();
        int[] cleared = SelfStats.heartCounts();
        check(cleared[1] == 0 && cleared[2] == 0, "reset 后心数应清零");

        // ⑤ 面板布局（列优先）
        check(SelfStatsPanel.rows(0, 2) == 0, "0 行不需要行");
        check(SelfStatsPanel.rows(1, 2) == 1, "1 行两列 = 1 行");
        check(SelfStatsPanel.rows(4, 2) == 2, "4 行两列 = 2 行");
        check(SelfStatsPanel.rows(5, 2) == 3, "5 行两列 = 3 行");
        check(SelfStatsPanel.rows(5, 0) == 5, "列数 0 应兜底成 1 列");
        check(SelfStatsPanel.columnOf(0, 3) == 0, "第 0 条在第 0 列");
        check(SelfStatsPanel.columnOf(2, 3) == 0, "第 2 条还在第 0 列");
        check(SelfStatsPanel.columnOf(3, 3) == 1, "第 3 条换到第 1 列");
        check(SelfStatsPanel.columnOf(6, 3) == 2, "第 6 条到第 2 列");
        check(SelfStatsPanel.columnOf(4, 0) == 0, "行数 0 时列号兜底 0");
        int[] off = SelfStatsPanel.columnOffsets(new int[]{10, 20, 30}, SelfStatsPanel.COLUMN_GAP);
        check(off[0] == 0, "第 0 列 x 应为 0");
        check(off[1] == 10 + SelfStatsPanel.COLUMN_GAP, "第 1 列 x 应为 第0列宽 + 间距");
        check(off[2] == 10 + SelfStatsPanel.COLUMN_GAP + 20 + SelfStatsPanel.COLUMN_GAP,
                "第 2 列 x 累加错误");
        check(SelfStatsPanel.ROW_HEIGHT == 10, "行高应与原版聊天行距一致（10）");

        // ⑥ 配置：新字段的夹取 + 版本迁移
        HudConfig.Cfg c = new HudConfig.Cfg();
        c.selfStatsSeconds = -5;
        c.selfStatsColumns = 9;
        c.selfStatsX = -100;
        c.selfStatsY = 99999;
        HudConfig.normalize(c);
        check(c.selfStatsSeconds == 1, "selfStatsSeconds -5 应夹到 1");
        HudConfig.Cfg big = new HudConfig.Cfg();
        big.selfStatsSeconds = 999;
        check(HudConfig.normalize(big).selfStatsSeconds == 5, "selfStatsSeconds 999 应夹到 5");
        check(c.selfStatsColumns == 3, "selfStatsColumns 9 应夹到 3");
        check(c.selfStatsX == 0, "selfStatsX -100 应夹到 0");
        check(c.selfStatsY == 4000, "selfStatsY 99999 应夹到 4000");
        check(HudConfig.CURRENT_VERSION == 23, "配置版本应为 23");
        HudConfig.Cfg defaults = HudConfig.normalize(new HudConfig.Cfg());
        check(defaults.showSelfStats, "自身属性面板默认开启");
        check(defaults.selfStatsSeconds == 3, "自身属性面板默认 3 s 刷新");
        check(defaults.selfStatsColumns == 2, "自身属性面板默认两列");
        HudConfig.Cfg v3 = new HudConfig.Cfg();
        v3.configVersion = 3;
        v3.barWidth = 90;
        HudConfig.migrate("{\"configVersion\":3}", v3);
        check(v3.configVersion == 23, "v3 → v23 应升版本号");
        check(v3.barWidth == 90, "迁移不该动已调过的值");
    }

    /** 按单个属性分类 / 提示行过滤 / 可滚动过滤列表 / 缩放不透明度。 */
    private static void selfStatsFineKinds() {
        var hint = net.minecraft.network.chat.Component.translatable("Misc_View_Stats_1");
        check(SelfStats.classify(SelfStats.analyze(hint)) == SelfStatsKind.HINT,
                "标题行（Misc_View_Stats_1）应归为 HINT");
        check(SelfStats.analyze(hint).unresolvedKey,
                "没有地图语言文件时，标题行应被标成「未解析翻译键」");

        // 带翻译键的倍率行：键 → 具体属性
        check(kindOfKey(4) == SelfStatsKind.HP_AMP, "键 4 应归为最大生命倍率");
        check(kindOfKey(5) == SelfStatsKind.HP_RAW, "键 5 应归为最大生命基础值");
        check(kindOfKey(8) == SelfStatsKind.CLASS_STAT_AMP, "键 8 应归为职业属性倍率");
        check(kindOfKey(10) == SelfStatsKind.STRENGTH_AMP, "键 10 应归为力量倍率");
        check(kindOfKey(12) == SelfStatsKind.ATTACK_SPEED_AMP, "键 12 应归为攻速倍率");
        check(kindOfKey(101) == SelfStatsKind.DEFENCE_AMP, "键 101 应归为防御倍率");
        check(kindOfKey(17) == SelfStatsKind.DEFENCE_RAW, "键 17 应归为防御（乘算前）");
        check(kindOfKey(104) == SelfStatsKind.TRUE_ARMOR_AMP, "键 104 应归为真•防御倍率");
        check(kindOfKey(19) == SelfStatsKind.TRUE_ARMOR_RAW, "键 19 应归为真•防御（乘算前）");
        check(kindOfKey(20) == SelfStatsKind.HEAL_AMP, "键 20 应归为治疗倍率");
        check(kindOfKey(25) == SelfStatsKind.FIRE_ARMOR_AMP, "键 25 应归为火焰护甲倍率");
        check(kindOfKey(33) == SelfStatsKind.SPEED_AMP, "键 33 应归为速度倍率");
        check(kindOfKey(40) == SelfStatsKind.MANA_AMP, "键 40 应归为法力倍率");
        check(kindOfKey(56) == SelfStatsKind.STATUS_56, "键 56 应归为「胸骨断裂」那条状态效果");
        check(kindOfKey(78) == SelfStatsKind.TIMER, "键 78 应归为计时");

        // hover 键（view_stats_N）是属性行最可靠的锚点
        check(SelfStats.hoverKind("view_stats_17") == SelfStatsKind.DEFENCE, "view 17 应是盔甲");
        check(SelfStats.hoverKind("view_stats_18") == SelfStatsKind.DEFENCE_AMP, "view 18 应是防御倍率");
        check(SelfStats.hoverKind("view_stats_19") == SelfStatsKind.TRUE_ARMOR, "view 19 应是真•防御");
        check(SelfStats.hoverKind("view_stats_21") == SelfStatsKind.TRUE_ARMOR_AMP, "view 21 应是真•防御倍率");
        check(SelfStats.hoverKind("view_stats_42") == SelfStatsKind.JUMP, "view 42 应是跳跃");
        check(SelfStats.hoverKind("view_stats_47") == SelfStatsKind.SUMMONING, "view 47 应是召唤力");

        // 只有图标的数值行：图标 + 颜色 → 具体属性
        check(glyphKind("120\u2764", SelfStats.RED) == SelfStatsKind.HEART_RED, "红心行应归为红心");
        check(glyphKind("40\u2764", SelfStats.YELLOW) == SelfStatsKind.HEART_SOUL, "黄心行应归为魂心");
        check(glyphKind("0\u2764", SelfStats.BLACK) == SelfStatsKind.HEART_BLACK, "黑心行应归为黑心");
        check(glyphKind("5\u2764", SelfStats.BLUE) == SelfStatsKind.HEART_BLUE, "蓝心行应归为蓝心");
        check(glyphKind("5\u2764", SelfStats.GRAY) == SelfStatsKind.HP_NEG, "灰心应归为负生命");
        check(glyphKind("5\u2764", SelfStats.LIGHT_PURPLE) == SelfStatsKind.HEAL_RAW,
                "亮紫心应归为治疗（地图用同一个 ❤ 图标）");
        check(glyphKind("5\u2764", SelfStats.DARK_RED) == SelfStatsKind.BITCHES, "暗红心应归为特殊红心");
        check(glyphKind("10\u2764%", SelfStats.RED) == SelfStatsKind.HEAL_PERCENT,
                "「10❤%」是治疗百分比，不该当成红心");
        check(glyphKind("10\u2764%", SelfStats.LIGHT_PURPLE) == SelfStatsKind.HEAL_EXTRA,
                "亮紫「❤%」应归为额外治疗百分比");
        check(glyphKind("5\u2764", 0x5500AA) == SelfStatsKind.HP_NEG, "颜色不认识的 ❤ 应归为负生命");

        check(glyphKind("15\uD83D\uDDE1", 0xFFAA00) == SelfStatsKind.CLASS_STAT, "金 🗡 应归为职业属性");
        check(glyphKind("1\uD83D\uDDE1", SelfStats.RED) == SelfStatsKind.STRENGTH, "红 🗡 应归为力量");
        check(glyphKind("1\uD83D\uDDE1", 0x5555FF) == SelfStatsKind.ATTACK_SPEED, "蓝 🗡 应归为攻速");
        check(glyphKind("1\uD83D\uDDE1", 0x00AA00) == SelfStatsKind.ATTACK_RANGE, "深绿 🗡 应归为攻击距离");
        check(glyphKind("1\uD83D\uDDE1", 0x55FF55) == SelfStatsKind.STAMINA, "绿 🗡 应归为耐力");
        check(glyphKind("1\uD83C\uDFF9", 0x55FF55) == SelfStatsKind.ARCHERY, "🏹 应归为弓术");

        check(glyphKind("0\uD83D\uDEE1", 0x5555FF) == SelfStatsKind.DEFENCE, "蓝 🛡 应归为防御");
        check(glyphKind("0\uD83D\uDEE1", 0xFFFFFF) == SelfStatsKind.TRUE_ARMOR, "白 🛡 应归为真伤护甲");
        check(glyphKind("15\uD83D\uDEE1\uD83D\uDD25", 0xFFAA00) == SelfStatsKind.FIRE_ARMOR,
                "金 🛡🔥 应归为火焰护甲");
        check(glyphKind("15\uD83D\uDEE1\uD83D\uDD25", 0xFFFFFF) == SelfStatsKind.TRUE_FIRE_ARMOR,
                "白 🛡🔥 应归为真伤火焰护甲");
        check(glyphKind("5\uD83D\uDEE1\u2693", 0x5555FF) == SelfStatsKind.WATER_ARMOR, "🛡⚓ 应归为水生护甲");
        check(glyphKind("5\uD83D\uDEE1\u26A1", 0xFFFF55) == SelfStatsKind.ELECTRIC_ARMOR, "🛡⚡ 应归为电护甲");
        check(glyphKind("5\uD83D\uDEE1\u2603", 0x55FFFF) == SelfStatsKind.ICE_ARMOR, "🛡☃ 应归为冰护甲");
        check(glyphKind("5\uD83D\uDEE1\u2620", 0xAA00AA) == SelfStatsKind.DARK_ARMOR, "🛡☠ 应归为暗护甲");
        check(glyphKind("5\uD83D\uDEE1\u2600", 0xFFFFFF) == SelfStatsKind.LIGHT_ARMOR, "🛡☀ 应归为光护甲");

        check(glyphKind("5\u2708", 0x55FFFF) == SelfStatsKind.MAX_SPEED, "青 ✈ 应归为最高速度");
        check(glyphKind("5\u2708", 0x55FF55) == SelfStatsKind.JUMP, "绿 ✈ 应归为跳跃");
        check(glyphKind("5\u2708", 0xFF87FF) == SelfStatsKind.DOUBLE_JUMP, "粉 ✈ 应归为二段跳");
        check(glyphKind("15\u270F", 0xFFAA00) == SelfStatsKind.MANA_POWER, "金 ✏ 应归为法力强度");
        check(glyphKind("15\u270F", 0x55FFFF) == SelfStatsKind.MANA_RECHARGE, "青 ✏ 应归为回蓝速度");
        check(glyphKind("15\u26A1", 0xFFFF55) == SelfStatsKind.ACTIVE_CHARGE, "黄 ⚡ 应归为主动充能");
        check(glyphKind("15\u26A1", 0xFFAA00) == SelfStatsKind.MAX_CHARGE, "金 ⚡ 应归为充能上限");
        check(glyphKind("1\u26A1", 0xFFFFFF) == SelfStatsKind.ACTIVE_SPEED, "白 ⚡ 应归为主动速度");
        check(glyphKind("0\u2620", 0xAA00AA) == SelfStatsKind.SUMMONING, "暗紫 ☠ 应归为召唤");
        check(glyphKind("3\u2620", SelfStats.RED) == SelfStatsKind.SKULLS, "红 ☠ 应归为骷髅数");
        check(glyphKind("1\u2B07", 0x5469FF) == SelfStatsKind.SIZE, "⬇ 应归为体型");
        check(glyphKind("1\u21D1", 0xE854FF) == SelfStatsKind.GRAVITY, "⇑ 应归为重力");
        check(glyphKind("\uE026", SelfStats.DARK_RED) == SelfStatsKind.HEART_CRACKED,
                "私有区字形 + 暗红应归为碎心");
        check(glyphKind("\uE026", SelfStats.LIGHT_PURPLE) == SelfStatsKind.HEART_PINK,
                "私有区字形 + 亮紫应归为粉心");

        // 过滤表：细类、分节、粗类展开、非法值（属性 / 状态效果 / 其它；1.20.32 游戏时间与难度骷髅移到「其它」）
        check(SelfStatsKind.FILTERABLE.length == 96,
                "可过滤项应为 96 项（1.2.11 新增 8 个「乘算前」原始值），实际 " + SelfStatsKind.FILTERABLE.length);
        check(SelfStatsKind.section(SelfStatsKind.Group.ATTRIBUTE).length == 64, "「属性」一节应有 64 项");
        check(SelfStatsKind.section(SelfStatsKind.Group.STATUS).length == 28, "「状态效果」一节应有 28 项");
        check(SelfStatsKind.section(SelfStatsKind.Group.OTHER).length == 4,
                "「其它」一节应为 游戏时间 / 难度骷髅 / 草方块 / 未识别行 4 项");
        check(SelfStatsKind.TIMER.group == SelfStatsKind.Group.OTHER
                && SelfStatsKind.SKULLS.group == SelfStatsKind.Group.OTHER,
                "游戏时间与难度骷髅应归到「其它」");
        check(SelfStatsKind.status(49) == SelfStatsKind.STATUS_49, "键 49 应映射到 status_49");
        check(SelfStatsKind.status(76) == SelfStatsKind.STATUS_76, "键 76 应映射到 status_76");
        check(SelfStatsKind.status(48) == null && SelfStatsKind.status(77) == null,
                "49~76 之外不该有状态效果类");
        check(SelfStatsKind.byId("misc").filterable, "未识别行仍要能单独关掉（兜底保留）");
        check(HudConfig.sanitizeKinds(java.util.List.of("heart_red")).size() == 1, "合法隐藏类应保留");
        check(HudConfig.sanitizeKinds(java.util.List.of("hint")).isEmpty(), "提示类不该能进过滤表");
        check(HudConfig.sanitizeKinds(java.util.List.of("nope")).isEmpty(), "未知类应被丢掉");
        check(HudConfig.sanitizeKinds(java.util.List.of("heart_red", "heart_red")).size() == 1,
                "重复的隐藏类应去重");
        check(HudConfig.sanitizeKinds(null).isEmpty(), "null 隐藏表应变成空表");
        check(HudConfig.sanitizeKinds(java.util.List.of("health")).size() == 7,
                "旧粗类 health 应展开成 7 个细类");
        java.util.List<String> expandedHealth = HudConfig.sanitizeKinds(java.util.List.of("health"));
        check(expandedHealth.contains("hp_neg") && expandedHealth.contains("heal_percent"),
                "health 展开应包含负生命与治疗百分比");
        check(HudConfig.sanitizeKinds(java.util.List.of("attack")).size() == 12,
                "旧粗类 attack 应展开成 12 个细类（含 3 个乘算前）");
        check(HudConfig.sanitizeKinds(java.util.List.of("misc")).contains("skulls"),
                "misc 展开应包含骷髅数");
        check(HudConfig.sanitizeKinds(java.util.List.of("misc")).contains("grass"),
                "misc 展开应包含草方块");
        check(HudConfig.sanitizeKinds(java.util.List.of("status")).size() == 28,
                "旧粗类 status 应展开成 28 条状态效果，实际 "
                        + HudConfig.sanitizeKinds(java.util.List.of("status")).size());
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (SelfStatsKind kind : SelfStatsKind.FILTERABLE) {
            check(ids.add(kind.id), "过滤项 id 不能重复: " + kind.id);
            check(kind != SelfStatsKind.HINT, "HINT 不该出现在过滤列表");
        }

        // 分节列表：每节一行标题 + 两个一项（不跨节合并）
        java.util.List<SelfStatsFilterScreen.Row> rows = SelfStatsFilterScreen.buildRows();
        check(rows.size() == 56, "三节 + 5 个属性细分共 56 行，实际 " + rows.size());
        check(rows.get(0).isHeader() && rows.get(0).subHeader() == SelfStatsKind.Sub.CORE,
                "第一行应是「属性」细分标题");
        check(!rows.get(1).isHeader() && rows.get(1).left() == SelfStatsKind.HP_RAW,
                "属性一节第一项应是最大生命值（乘算前）；hp_amp 归「倍率」");
        check(rows.get(38).isHeader() && rows.get(38).header() == SelfStatsKind.Group.STATUS,
                "第 38 行应是「状态效果」标题");
        check(!rows.get(39).isHeader() && rows.get(39).left() == SelfStatsKind.STATUS_49
                        && rows.get(39).right() == SelfStatsKind.STATUS_50,
                "状态效果一节应从 49/50 并排开始");
        check(rows.get(53).isHeader() && rows.get(53).header() == SelfStatsKind.Group.OTHER,
                "第 53 行应是「其它」标题");
        check(rows.get(54).left() == SelfStatsKind.TIMER && rows.get(54).right() == SelfStatsKind.SKULLS,
                "其它一节第一行应是 游戏时间 + 难度骷髅");
        check(rows.get(55).left() == SelfStatsKind.GRASS && rows.get(55).right() == SelfStatsKind.MISC,
                "其它一节第二行应是 草方块 + 未识别行");
        // 属性细分（心型 / 倍率 / 护甲 / 杂项）
        check(SelfStatsKind.HEART_RED.sub() == SelfStatsKind.Sub.HEART, "红心应归心型");
        check(SelfStatsKind.HP_NEG.sub() == SelfStatsKind.Sub.HEART, "负心应归心型");
        check(SelfStatsKind.HEART_SOUL.sub() == SelfStatsKind.Sub.HEART, "魂心应归心型");
        check(SelfStatsKind.HEART_SOUL.sub() == SelfStatsKind.Sub.HEART, "魂心应归心型");
        check(SelfStatsKind.DEFENCE_AMP.sub() == SelfStatsKind.Sub.MULT, "防御倍率应归倍率");
        check(SelfStatsKind.SIZE.sub() == SelfStatsKind.Sub.MISC, "体型应归杂项");
        check(SelfStatsKind.KARMA_DARK.sub() == SelfStatsKind.Sub.MISC, "业力应归杂项");
        check(SelfStatsKind.DEFENCE.sub() == SelfStatsKind.Sub.ARMOR, "防御应归护甲");
        check(SelfStatsKind.FIRE_ARMOR.sub() == SelfStatsKind.Sub.ARMOR, "火之加护应归护甲");
        check(SelfStatsKind.STAMINA.sub() == SelfStatsKind.Sub.CORE, "体力恢复速度应归属性");
        check(SelfStatsKind.STATUS_49.sub() == null, "状态效果不属于属性细分");
        int headers = 0;
        for (SelfStatsFilterScreen.Row row : rows) if (row.isHeader()) headers++;
        check(headers == 7, "应有 7 个标题（5 个属性细分 + 状态效果 + 其它），实际 " + headers);

        // 双列按钮 + 滚动的纯函数
        check(SelfStatsFilterScreen.visibleRows(148) == 6, "148 px 列表应能放 6 行，实际 "
                + SelfStatsFilterScreen.visibleRows(148));
        check(SelfStatsFilterScreen.visibleRows(0) == 0, "列表高度 0 应放 0 行");
        check(SelfStatsFilterScreen.maxScroll(56, 6) == 50, "56 行 / 6 行可见 → 最大滚动 50");
        check(SelfStatsFilterScreen.maxScroll(5, 10) == 0, "内容比列表短时不该能滚");
        check(SelfStatsFilterScreen.clampScroll(-3, 41) == 0, "滚动量不该小于 0");
        check(SelfStatsFilterScreen.clampScroll(999, 41) == 41, "滚动量不该超过上限");
        check(SelfStatsFilterScreen.scrollByWheel(2, -1.0, 10) == 3, "向下滚一格 → +1 行");
        check(SelfStatsFilterScreen.scrollByWheel(0, 1.0, 10) == 0, "已在顶部再往上滚应保持 0");
        check(SelfStatsFilterScreen.scrollByWheel(9, 1.0, 10) == 8, "向上滚一格 → -1 行");
        check(SelfStatsFilterScreen.scrollByWheel(0, -0.5, 10) == 1, "半格滚动应至少动一行");
        check(SelfStatsFilterScreen.columnWidth(426) == 196,
                "426 px 屏（开发端 GUI 宽）单列按钮应为 196，实际 "
                        + SelfStatsFilterScreen.columnWidth(426));
        check(SelfStatsFilterScreen.columnWidth(1000) == 200, "宽屏单列按钮封顶 200");
        check(SelfStatsFilterScreen.columnWidth(200) == 83,
                "200 px 窄屏单列按钮应按可用宽度收缩，实际 "
                        + SelfStatsFilterScreen.columnWidth(200));

        // 地图私有区字形（从 view_stats.mcfunction 里抽出来的真实码位）
        check(glyphKind("5\uE004", 0x55FF55) == SelfStatsKind.TOWERS_REGEN, "U+E004 应归为塔内回血");
        check(glyphKind("5\uE003", 0x55FFFF) == SelfStatsKind.FISHING, "U+E003 应归为钓鱼");
        check(glyphKind("5\uE001", 0xFFFF55) == SelfStatsKind.BEE, "U+E001 黄色应归为蜜蜂点");
        check(glyphKind("5\uE001", 0x000000) == SelfStatsKind.ANT, "U+E001 黑色应归为蚂蚁点");
        check(glyphKind("5\uE010", 0x55FFFF) == SelfStatsKind.KARMA_LIGHT, "U+E010 青色应归为光明因果");
        check(glyphKind("5\uE010", 0xAA00AA) == SelfStatsKind.KARMA_NEUTRAL, "U+E010 暗紫应归为中立因果");
        check(glyphKind("5\uE011", 0xAA0000) == SelfStatsKind.KARMA_DARK, "U+E011 应归为黑暗因果");
        check(glyphKind("5\uE027", 0x892EFF) == SelfStatsKind.KARMA_CELESTIAL, "U+E027 应归为天界因果");
        check(glyphKind("5\uE026", 0xAA0000) == SelfStatsKind.HEART_CRACKED, "U+E026 暗红应归为碎心");
        check(glyphKind("5\uE026", 0xFF55FF) == SelfStatsKind.HEART_PINK, "U+E026 亮紫应归为粉心");

        // 配置：版本、默认值、粗类展开
        HudConfig.Cfg cfg = new HudConfig.Cfg();
        check(HudConfig.CURRENT_VERSION == 23, "配置版本应为 23");
        HudConfig.Cfg defaults = HudConfig.normalize(new HudConfig.Cfg());
        check(defaults.showSelfStats, "自身属性面板默认开启");
        check(defaults.selfStatsSeconds == 3, "自身属性面板默认 3 s 刷新");
        check(defaults.selfStatsColumns == 2, "自身属性面板默认两列");
        check(defaults.selfStatsOpacity == 1.0, "默认不透明度应为 1.0");
        check(defaults.selfStatsMode == 2, "默认显示方式应为 HUD + 容器界面（v1.0.0 起）");
        check(defaults.selfStatsHidden.isEmpty(), "默认不隐藏任何属性");

        // 盾牌默认位置是否跟随主手（shieldAutoMirror）
        check(defaults.shieldAutoMirror, "盾牌默认位置默认跟随主手镜像");
        HudConfig.Cfg mirrorOn = new HudConfig.Cfg();
        check(ShieldIndicatorHud.offhandOnLeft(mirrorOn, net.minecraft.world.entity.HumanoidArm.RIGHT),
                "右手玩家：副手应在快捷栏左侧");
        check(!ShieldIndicatorHud.offhandOnLeft(mirrorOn, net.minecraft.world.entity.HumanoidArm.LEFT),
                "左手玩家：开启镜像时副手应在右侧");
        HudConfig.Cfg mirrorOff = new HudConfig.Cfg();
        mirrorOff.shieldAutoMirror = false;
        check(ShieldIndicatorHud.offhandOnLeft(mirrorOff, net.minecraft.world.entity.HumanoidArm.LEFT),
                "关闭镜像后左手玩家也按右手习惯（副手在左）");
        check(ShieldIndicatorHud.defaultX(true, 400, 24) == 400 / 2 - 109 - 12,
                "副手在左时 x 应对准快捷栏左侧物品格");
        check(ShieldIndicatorHud.defaultX(false, 400, 24) == 400 / 2 + 109 - 12,
                "副手在右时 x 应镜像到另一侧");

        // 显示值不再夹到 MaxHP（蓝心 / 魂心可以超过上限）
        EnemyData.Snapshot over = EnemyData.exact(300, 200, 1.0f, 0, null, null);
        check(over.hp == 300, "AllHearts 300 / MaxHP 200 时显示值应保留 300");
        check(over.maxHp == 200, "上限仍应是 MaxHP");
        check(over.percent == 100, "超过上限时百分比应夹到 100");
        EnemyData.Snapshot under = EnemyData.exact(124, 200, 0.62f, 0, null, null);
        check(under.hp == 124 && under.maxHp == 200, "普通血量应原样保留");
        check(under.percent == 62, "124/200 应算出 62%");

        // 队伍条认领 —— 完整玩家名绝不走「包含匹配」（NIVEI vs NIVEIV）
        check(!BossBarTracker.allowLooseMatch(true, 1), "已知全名时不允许宽松匹配");
        check(BossBarTracker.allowLooseMatch(false, 1), "未知名字且唯一候选时允许宽松匹配");
        check(!BossBarTracker.allowLooseMatch(false, 2), "未知名字但有多个候选时不允许宽松匹配");
        check(!BossBarTracker.allowLooseMatch(false, 0), "没有候选时不允许宽松匹配");

        HudConfig.Cfg wild = new HudConfig.Cfg();
        wild.selfStatsOpacity = -3.0;
        wild.selfStatsHidden = new java.util.ArrayList<>(java.util.List.of("heart_red", "hint", "heart_red", "health"));
        HudConfig.normalize(wild);
        check(wild.selfStatsOpacity == 0.2, "selfStatsOpacity -3 应夹到 0.2");
        check(wild.selfStatsHidden.contains("heart_red") && !wild.selfStatsHidden.contains("hint"),
                "隐藏表应去掉 hint 且保留 heart_red");
        check(wild.selfStatsHidden.contains("hp_amp"), "隐藏表里的旧粗类 health 应被展开");

        HudConfig.Cfg v4 = new HudConfig.Cfg();
        v4.configVersion = 4;
        v4.barWidth = 90;
        HudConfig.migrate("{\"configVersion\":4}", v4);
        check(v4.configVersion == 23, "v4 → v23 应升版本号");
        check(v4.barWidth == 90, "迁移不该动已调过的值");

        // hiddenKinds() 与过滤真的作用到面板
        HudConfig.Cfg live = HudConfig.get();
        live.selfStatsHidden = new java.util.ArrayList<>();
        SelfStats.reset();
        check(SelfStats.lines().isEmpty(), "没有数据时 lines() 应为空");
        check(SelfStats.heartsOf(redHeartLine("120")).length == 7, "心数数组长度应为 7");
        live.selfStatsHidden.add(SelfStatsKind.HEART_RED.id);
        check(HudConfig.hiddenKinds().contains(SelfStatsKind.HEART_RED.id),
                "隐藏表应被 hiddenKinds() 读到");
        live.selfStatsHidden.clear();
        check(HudConfig.hiddenKinds().isEmpty(), "清空后 hiddenKinds() 应为空");
        HudConfig.reset();
        HudConfig.get().selfStatsHidden = new java.util.ArrayList<>();
        SelfStats.reset();
    }

    private static SelfStatsKind kindOfKey(int key) {
        var component = net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("100"))
                .append(net.minecraft.network.chat.Component.translatable("Misc_View_Stats_" + key));
        return SelfStats.classify(SelfStats.analyze(component));
    }

    private static SelfStatsKind glyphKind(String flat, int rgb) {
        java.util.List<Integer> colors = new java.util.ArrayList<>();
        for (int i = 0; i < flat.length(); i++) colors.add(rgb);
        return SelfStats.glyphKind(flat, colors);
    }

    private static net.minecraft.network.chat.MutableComponent redHeartLine(String amount) {
        return net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal(amount)
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))))
                .append(net.minecraft.network.chat.Component.literal("\u2764")
                        .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(SelfStats.RED))));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) fail(message);
    }

    /**
     * 聊天不再被误吞 + 游戏时间冻住时不上屏 + 拖动夹取。
     *
     * <p>「属性行」的判据换成地图 tellraw 的真实形态：带 {@code Misc_View_Stats_*} 键，
     * 或者「数字 + 图形」。原来那种「数字开头 / 两个字以内就算」会把玩家的
     * 「123」「ok」当成属性行吃掉。</p>
     */
    private static void chatAndDrag() {
        check(SelfStats.statGlyphLine("120\u2764"), "「120❤」应算属性行");
        check(SelfStats.statGlyphLine("1 \uD83D\uDDE1"), "「1 🗡」应算属性行");
        check(SelfStats.statGlyphLine("5 \uD83D\uDEE1 3 \u2603"), "护甲行应算属性行");
        check(SelfStats.statGlyphLine("3 \uE001"), "私有区字形应算属性行");
        check(!SelfStats.statGlyphLine("123"), "纯数字聊天不该算属性行");
        check(!SelfStats.statGlyphLine("ok"), "两字聊天不该算属性行");
        check(!SelfStats.statGlyphLine("3楼集合"), "中文聊天不该算属性行");

        SelfStats.reset();
        SelfStats.beginCapture(1_000L, true);
        check(SelfStats.onGameMessage(Component.literal("120\u2764"), false, 1_100L), "自动采集：属性行应被吞掉");
        check(!SelfStats.onGameMessage(Component.literal("ok"), false, 1_100L), "两字聊天不该被吞");
        check(!SelfStats.onGameMessage(Component.literal("123"), false, 1_100L), "纯数字聊天不该被吞");
        check(!SelfStats.onGameMessage(Component.translatable("chat.type.text", "A", Component.literal("hi")),
                false, 1_100L), "玩家聊天不该被吞");
        check(SelfStats.onGameMessage(Component.translatable("Misc_View_Stats_78"), false, 1_100L),
                "计时行应被吞");

        // 玩家手动 /trigger ViewStats：**不吞**（上游 hideCapture=false 同款），但要进面板
        SelfStats.reset();
        check(!SelfStats.onGameMessage(Component.translatable("Misc_View_Stats_1"), false, 5_000L),
                "手动触发：标题行不该被吞");
        check(!SelfStats.onGameMessage(Component.literal("87\u2764"), false, 5_050L),
                "手动触发：属性行不该被吞");
        check(!SelfStats.onGameMessage(Component.translatable("Misc_View_Stats_78"), false, 5_100L),
                "手动触发：计时行不该被吞");
        check(SelfStats.storedLineCount() >= 1,
                "手动触发的内容也要进面板: " + SelfStats.storedLineCount());

        // 游戏时间冻住：同样文本 + 间隔够才计数
        check(SelfStats.nextSameTimeRounds(null, "T", 3_000, 0) == 0, "第一次采样不算冻住");
        check(SelfStats.nextSameTimeRounds("T", "T", 3_000, 0) == 1, "同样文本 + 间隔够 → 计数 1");
        check(SelfStats.nextSameTimeRounds("T", "T", 1_000, 0) == 0, "间隔不足不计数（1 s 刷新不误判）");
        check(SelfStats.nextSameTimeRounds("T", "T2", 3_000, 5) == 0, "文本变了立刻归零");
        check(SelfStats.nextSameTimeRounds("T", null, 3_000, 5) == 5, "没收到计时行保持原判断");

        // 拖动夹取：整块留在屏内
        check(SelfStatsPanel.fitInScreen(-5, 200, 800) == 0, "左边越界应夹到 0");
        check(SelfStatsPanel.fitInScreen(900, 200, 800) == 600, "右下越界应夹到 屏幕-宽");
        check(SelfStatsPanel.fitInScreen(100, 200, 800) == 100, "屏内应保持");
        check(SelfStatsPanel.fitInScreen(300, 2_000, 800) == 0, "面板比屏幕还宽时贴 0");

        // 拖动界面「只许拖、不许点」
        int[] box = {100, 100, 40, 60};
        check(SelfStatsPanel.grabHit(box, 4, 100, 100), "左上角内侧应能抓住");
        check(SelfStatsPanel.grabHit(box, 4, 140, 160), "右下角内侧应能抓住");
        check(SelfStatsPanel.grabHit(box, 4, 96, 96), "左上 −4 的余量内也应能抓住");
        check(SelfStatsPanel.grabHit(box, 4, 144, 164), "右下 +4 的余量内也应能抓住");
        check(!SelfStatsPanel.grabHit(box, 4, 95, 100), "超出余量（左 5 px）不该抓住");
        check(!SelfStatsPanel.grabHit(box, 4, 100, 165), "超出余量（下 5 px）不该抓住");
        check(!SelfStatsPanel.grabHit(null, 4, 0, 0), "bounds 为 null 时不该抓住");
        check(!SelfStatsPanel.dragExceeds(0, 0, 2), "原地不动不算拖动");
        check(!SelfStatsPanel.dragExceeds(1, 1, 2), "1 px 位移不算拖动（防手抖）");
        check(SelfStatsPanel.dragExceeds(2, 0, 2), "2 px 位移算拖动");
        check(SelfStatsPanel.dragExceeds(0, -2, 2), "向上/斜向位移也算拖动");
        check(!SelfStatsPanel.dragExceeds(0, 0, 0), "阈值 0 会被兜底成 1，原地仍不算");
        check(SelfStatsPanel.dragExceeds(1, 0, 0), "阈值 0 兜底后 1 px 就算拖动");

    }

    /**
     * 游戏时间真的上面板 + 草方块那条不再被丢掉。
     *
     * <p>以前计时行只当「这一趟到此结束」的信号，收完就扔，所以面板上永远看不到游戏时间；
     * 草方块那条（一个绿色 {@code □}）既没有翻译键也没有数字，连「像不像属性行」都过不了，
     * 被整行丢掉。这里锁住：两条都会进 PENDING → SHOWN，并且都能在过滤表里单独关掉。</p>
     */
    private static void panelTimerAndGrass() {
        // 草方块：单行 □（地图 view_stats_83）
        check(SelfStats.isGrassLine("\u25A1"), "单个 □ 应认成草方块那行");
        check(SelfStats.isGrassLine(" \u25A1 "), "两侧空格不影响识别");
        check(!SelfStats.isGrassLine("\u25A1\u25A1"), "两个 □ 不算草方块那行");
        check(!SelfStats.isGrassLine("ok"), "聊天不该认成草方块");
        check(!SelfStats.statGlyphLine("\u25A1"),
                "□ 没有数字，不属于「数字 + 图形」行（靠专用判据认）");
        check(glyphKind("\u25A1", 0x55FF55) == SelfStatsKind.GRASS, "绿色 □ 应归为草方块");

        // 采集：草方块 + 属性 + 计时行都该进面板（计时行同时是这一趟的收尾信号）
        HudConfig.reset();
        HudConfig.get().selfStatsHidden = new java.util.ArrayList<>();
        SelfStats.reset();
        SelfStats.beginCapture(1_000L, true);
        check(SelfStats.onGameMessage(Component.literal("\u25A1"), false, 1_100L),
                "自动采集：草方块那行应被吞掉");
        check(SelfStats.onGameMessage(Component.literal("10 \u2764%"), false, 1_200L),
                "自动采集：属性行应被吞掉");
        check(SelfStats.onGameMessage(Component.literal("游戏时间为 0:00:19"), false, 1_300L),
                "自动采集：计时行应被吞掉（并收尾这一趟）");
        check(SelfStats.storedLineCount() == 3,
                "草方块 + 属性 + 计时都该上面板，实际 " + SelfStats.storedLineCount());
        java.util.List<Component> lines = SelfStats.lines();
        check(lines.size() == 3, "过滤后仍是 3 行，实际 " + lines.size());
        check(lines.get(2).getString().contains("0:00:19"),
                "最后一行应是游戏时间（地图最后才发它）");

        // 没装地图资源包时计时行是未解析的翻译键：不上面板（否则会出现「Misc_View_Stats_7800:00:19」）
        SelfStats.reset();
        SelfStats.beginCapture(2_000L, true);
        SelfStats.onGameMessage(Component.translatable("Misc_View_Stats_78"), false, 2_100L);
        check(SelfStats.storedLineCount() == 0,
                "翻译键没解析出来时计时行不该上面板，实际 " + SelfStats.storedLineCount());

        // 过滤表：两条都能单独关
        SelfStats.reset();
        SelfStats.beginCapture(3_000L, true);
        SelfStats.onGameMessage(Component.literal("\u25A1"), false, 3_100L);
        SelfStats.onGameMessage(Component.literal("游戏时间为 0:00:20"), false, 3_200L);
        check(SelfStats.lines().size() == 2, "关之前应有两行（草方块 + 计时）");
        HudConfig.get().selfStatsHidden.add(SelfStatsKind.GRASS.id);
        check(SelfStats.lines().size() == 1, "关掉草方块后只剩计时行");
        HudConfig.get().selfStatsHidden.add(SelfStatsKind.TIMER.id);
        check(SelfStats.lines().isEmpty(), "两个都关掉后面板为空");
        HudConfig.reset();
    }

    /**
     * 滚动条可以点/拖。
     *
     * <p>以前滚动条只是画出来，点它没反应。这里锁住几何：滑块高度按「视口 ÷ 内容」、
     * 偏移按 {@code scroll / maxScroll} 映射、反解是它的逆运算；命中判定带 ±3 px 容差
     * （4 px 的条太细，鼠标不好抓）。</p>
     */
    private static void scrollbarDrag() {
        // 纯函数：滑块高度 / 偏移 / 反解
        check(ScrollPane.thumbHeight(148, 500) == 148 * 148 / 500,
                "滑块高度 = 视口² ÷ 内容，实际 " + ScrollPane.thumbHeight(148, 500));
        check(ScrollPane.thumbHeight(148, 148) == 148, "内容刚好等于视口时滑块吃满");
        check(ScrollPane.thumbHeight(0, 100) == 0, "轨道高 0 → 没有滑块");
        check(ScrollPane.thumbOffset(148, 40, 0, 300) == 0, "在顶部时偏移 0");
        check(ScrollPane.thumbOffset(148, 40, 300, 300) == 148 - 40, "到底时滑块贴底");
        check(ScrollPane.thumbOffset(148, 40, 150, 300) == (148 - 40) / 2, "滚到一半时滑块也在中间");
        check(ScrollPane.thumbOffset(148, 40, 9999, 300) == 148 - 40, "越界滚动量按最底算");
        check(ScrollPane.scrollForThumbTop(0, 148, 40, 300) == 0, "滑块在顶 → 滚动 0");
        check(ScrollPane.scrollForThumbTop(148 - 40, 148, 40, 300) == 300, "滑块在底 → 滚到底");
        check(ScrollPane.scrollForThumbTop(-20, 148, 40, 300) == 0, "滑块越上界应夹到 0");
        check(ScrollPane.scrollForThumbTop(9999, 148, 40, 300) == 300, "滑块越下界应夹到底");
        check(ScrollPane.scrollForThumbTop(50, 148, 40, 0) == 0, "没有可滚动内容时反解为 0");
        int offset = ScrollPane.thumbOffset(148, 40, 123, 300);
        check(Math.abs(ScrollPane.scrollForThumbTop(offset, 148, 40, 300) - 123) <= 1,
                "偏移 → 反解应回到原滚动量（整数截断允许 1 行误差），偏移 " + offset);

        // 实例：命中判定（含容差）
        ScrollPane pane = new ScrollPane(52, 200);
        pane.contentHeight(500);            // 视口 148，最大滚动 352
        check(pane.scrollbarVisible(), "内容比视口高时应有滚动条");
        check(pane.barX(400) == 396, "滚动条 x = 锚点 - 宽（4）");
        check(pane.hitsBar(396, 100, 400) && pane.hitsBar(391, 100, 400) && pane.hitsBar(405, 100, 400),
                "滚动条 ±5 px 容差内都应命中");
        check(!pane.hitsBar(390, 100, 400) && !pane.hitsBar(406, 100, 400), "容差外不该命中");
        check(!pane.hitsBar(396, 20, 400) && !pane.hitsBar(396, 220, 400),
                "可视区上下之外的同列位置不该响应（否则会点到下面的按钮/Tab 键）");
        check(pane.hitsThumb(52 + 5), "滑块在顶部时应命中上沿");
        check(!pane.hitsThumb(195), "滑块在顶部时底部不该命中");
        check(pane.grabOffset(52 + 10) == 10, "抓取偏移 = 光标 - 滑块顶");
        pane.dragThumbTo(52 + 100);
        check(pane.scroll() > 0 && pane.scroll() < pane.maxScroll(), "拖到中间应落在中间");
        pane.dragThumbTo(52 + 9999);
        check(pane.scroll() == pane.maxScroll(), "拖到底应到最大滚动");
        pane.dragThumbTo(0);
        check(pane.scroll() == 0, "拖到顶应回到 0");

        // 没有可滚动内容时不该响应
        ScrollPane shortPane = new ScrollPane(52, 200);
        shortPane.contentHeight(100);
        check(!shortPane.scrollbarVisible(), "内容不足时不该有滚动条");
        check(!shortPane.hitsBar(396, 100, 400) && !shortPane.hitsThumb(60),
                "内容不足时点滚动条区域不该有反应");

        // 过滤列表的滑块（8 px 下限，按行数占比）
        check(SelfStatsFilterScreen.thumbHeight(145, 47, 6) == Math.max(8, 145 * 6 / 47),
                "过滤列表滑块高度按可见行占比，实际 " + SelfStatsFilterScreen.thumbHeight(145, 47, 6));
        check(SelfStatsFilterScreen.thumbHeight(0, 47, 6) == 0, "轨道高 0 → 过滤列表没有滑块");
        check(SelfStatsFilterScreen.thumbHeight(145, 0, 6) == 0, "没有行 → 没有滑块");
    }

    /**
     * 1.20.33 / 局部设置不许影响全局设置。
     *
     * <p>1.20.33 修掉的三处「串台」：① 队友血条跟着敌人的 {@code showBar} 走；
     * ② 实体设置页的滑块在 {@code rebuildWidgets()} 时回弹到建表时的旧值；
     * ③ 「回到初始位置」对容器面板用了 HUD 的默认 y。</p>
     *
     * <p>「显示血条」升级成<strong>整块开关</strong> —— 敌人关掉后血条 + 上方心数/护甲
     * 全不画（布局为空），队友则由 {@code showTeammates} 整块管。</p>
     */
    private static void scopedSettings() {
        // ① 「显示血条」= 整块：关掉敌人后布局为空，队友不受它影响
        HudConfig.Cfg cfg = new HudConfig.Cfg();
        cfg.showBar = false;
        HudLayout.Options off = HudWorldRenderer.options(cfg, false);
        check(!off.showBar && !off.showNumbers && !off.showHearts && !off.showArmor,
                "关掉「显示血条」后敌人四个开关都应关（血条 + 文字）");
        HudLayout.Result empty = HudLayout.compute(100, 100, false, 5,
                new int[]{0, 0, 0, 0, 0, 0, 0}, HeartCounts.forMob(100, 100), off, String::length);
        check(empty.isEmpty(), "关掉「显示血条」后敌人整块布局应为空（只剩原版名字）");
        check(HudWorldRenderer.options(cfg, true).showBar, "关掉敌人「显示血条」不该连带队友");
        cfg.showBar = true;
        check(HudWorldRenderer.options(cfg, false).showBar, "打开后敌人血条应回来");

        // 玩家（队友 + 自己）统一用玩家排版 —— 只有各类心型 + 条，不含护甲段；
        // 长度 / 缩放 / 整块开关都跟「玩家」设置走，不再借敌人的 showBar / barWidth / scale。
        HudLayout.Options playerOpts = HudWorldRenderer.options(cfg, true);
        check(playerOpts.playerStyle, "玩家应使用玩家排版（两排心）");
        check(!playerOpts.showArmor, "玩家（含自己）不该画护甲段");
        check(playerOpts.showBar, "玩家的条不该被敌人「显示血条」关掉");
        check(playerOpts.barWidth == cfg.teamBarWidth && playerOpts.scale == cfg.teamScale,
                "玩家的条长度 / 缩放应取玩家设置");
        HudLayout.Result playerLayout = HudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                playerOpts, SelfCheck::width);
        check(join(playerLayout).replace(" ", "").equals("❤40❤30❤20❤60/150❤2"),
                "玩家排版即便带护甲数据也只画心: " + join(playerLayout));
        HudLayout.Result on = HudLayout.compute(100, 100, false, 5,
                new int[]{0, 0, 0, 0, 0, 0, 0}, HeartCounts.forMob(100, 100), HudWorldRenderer.options(cfg, false),
                String::length);
        check(!on.isEmpty(), "打开后敌人布局应有内容");

        // ② 四套面板配置互不串改
        HudConfig.reset();
        HudConfig.movePanel(2, true, 11, 22, 320, 240);
        HudConfig.Cfg live = HudConfig.get();
        check(live.bothContainerX == 11 && live.bothContainerY == 22, "模式 2 容器那套应被写入");
        check(live.bothX == 4 && live.bothY == -1, "模式 2 的 HUD 那套不该被带动（y 仍是动态默认）");
        check(live.selfStatsX == 4 && live.selfStatsY == -1, "模式 0 那套不该被带动（y 仍是动态默认）");
        check(live.containerX == 4 && live.containerY == -1, "模式 1 那套不该被带动（y 仍是动态默认）");
        HudConfig.setPanelColumns(2, false, 3);
        check(live.bothColumns == 3, "模式 2 HUD 列数应写入");
        check(live.bothContainerColumns == 2, "模式 2 容器列数不该被带动");
        HudConfig.setPanelOpacity(2, true, 0.5);
        check(live.bothContainerOpacity == 0.5 && live.bothOpacity == 1.0, "不透明度只写当前对象");

        // ③ 两种文字阴影互不影响，且都不碰盾牌
        live.textShadow = true;
        check(live.selfStatsTextShadow, "改实体血条阴影不该动面板阴影（默认开）");
        live.selfStatsTextShadow = false;
        check(live.textShadow, "改面板阴影不该动实体血条阴影");
        check(Math.abs(live.shieldScale - 0.7) < 1e-9 && Math.abs(live.shieldOpacity - 0.65) < 1e-9,
                "面板/实体设置不该动盾牌默认值");
        HudConfig.reset();

        // ④ 「回到初始位置」四套都回到「屏幕高 ÷ 3」（容器那两套也一样）
        check(HudConfig.defaultPanelY(720) == 240, "默认 y = 屏幕高 ÷ 3");
        check(HudConfig.defaultPanelY(360) == 120, "屏幕矮一半 → 默认 y 也跟着减半");
    }

    /**
     * 面板 / 盾牌的位置是**相对位置**。
     *
     * <p>以前存的是绝对 GUI 像素，窗口大小或 GUI 缩放一变，同一个数字就落到屏幕的另一处
     * （「窗口一变就错位」）。现在坐标连同「量它的那套 GUI 尺寸」一起存
     * （{@code layoutGuiW/H}），绘制时按 {@code 当前 ÷ 记录} 换算。</p>
     */
    private static void relativePositions() {
        // ① 纯换算
        check(HudConfig.scalePos(100, 400, 800) == 200, "尺寸放大一倍 → 坐标也翻倍");
        check(HudConfig.scalePos(100, 400, 400) == 100, "同一尺寸 → 原样");
        check(HudConfig.scalePos(100, 0, 800) == 100, "基准未知 → 原样（老配置第一次进游戏）");
        check(HudConfig.scalePos(100, 400, 0) == 100, "当前尺寸未知 → 原样");
        check(HudConfig.scalePos(-5, 400, 800) == -10, "负坐标同样按比例换算");
        check(HudConfig.scalePos(1, 3, 2) == 1, "三分之一缩到三分之二 → 四舍五入到 1");

        // ② 面板：窗口变大变小，面板留在同一个相对位置
        HudConfig.reset();
        HudConfig.Cfg c = HudConfig.get();
        c.layoutGuiW = 400;
        c.layoutGuiH = 300;
        c.selfStatsX = 200;
        c.selfStatsY = 150;
        HudConfig.Panel big = HudConfig.panel(0, false, 800, 600);
        check(big.x == 400 && big.y == 300,
                "窗口 2 倍 → 面板坐标也 2 倍（相对位置守恒），实际 " + big.x + "," + big.y);
        HudConfig.Panel same = HudConfig.panel(0, false, 400, 300);
        check(same.x == 200 && same.y == 150, "同尺寸读取应原样");

        // ③ 拖动：参考尺寸跟着换；换完别的窗口尺寸按新比例算
        HudConfig.movePanel(0, false, 100, 120, 800, 600);
        check(c.layoutGuiW == 800 && c.layoutGuiH == 600, "拖动后参考尺寸应更新为当前 GUI");
        check(c.selfStatsX == 100 && c.selfStatsY == 120, "拖动写入的就是当前 GUI 下的坐标");
        HudConfig.Panel half = HudConfig.panel(0, false, 400, 300);
        check(half.x == 50 && half.y == 60,
                "换回一半的窗口 → 坐标也减半，实际 " + half.x + "," + half.y);

        // ④ 盾牌：同一套换算；未设置时仍然是「跟随副手槽」
        HudConfig.reset();
        c = HudConfig.get();
        c.layoutGuiW = 400;
        c.layoutGuiH = 300;
        c.shieldX = 100;
        c.shieldY = 50;
        int[] shieldBig = HudConfig.shieldPosition(800, 600);
        check(shieldBig[0] == 200 && shieldBig[1] == 100, "盾牌同样按比例换算，实际 "
                + shieldBig[0] + "," + shieldBig[1]);
        check(HudConfig.shieldPosition(400, 300)[0] == 100, "盾牌同尺寸原样");
        c.shieldX = -1;
        c.shieldY = -1;
        check(HudConfig.shieldPosition(800, 600)[0] == -1, "未设置 → -1（跟随副手槽）");
        HudConfig.setShieldPosition(60, 30, 400, 300);
        check(c.shieldX == 60 && c.layoutGuiW == 400, "写盾牌位置时也记录当前 GUI 尺寸");
        check(HudConfig.shieldPosition(200, 150)[0] == 30, "盾牌换到一半窗口 → 坐标减半");

        // ⑤ 老配置没有基准（0）：第一次绘制时用当时的尺寸补上，之后才开始按比例
        HudConfig.reset();
        c = HudConfig.get();
        check(c.layoutGuiW == 0 && c.layoutGuiH == 0, "新配置的基准应是「未知」");
        HudConfig.panel(0, false, 640, 360);
        check(c.layoutGuiW == 640 && c.layoutGuiH == 360, "第一次绘制应用当前尺寸补上基准");
        check(HudConfig.panel(0, false, 1280, 720).x == c.selfStatsX * 2,
                "补上基准之后，放大一倍窗口 → 坐标翻倍");

        // ⑥ 非法基准按「未知」处理（0/负数/NaN 都别把位置算飞）
        HudConfig.Cfg bad = new HudConfig.Cfg();
        bad.layoutGuiW = -5;
        bad.layoutGuiH = 99999;
        HudConfig.normalize(bad);
        check(bad.layoutGuiW == 0 && bad.layoutGuiH == 0, "非法基准应归零（当作未知）");
        bad.layoutGuiW = 400;
        bad.layoutGuiH = 0;
        HudConfig.normalize(bad);
        check(bad.layoutGuiW == 0 && bad.layoutGuiH == 0, "半套基准也当归零");

        // ⑦ 面板默认位置 = x 4 / y 屏幕高 ÷ 3（左侧偏上），并且随窗口走
        check(HudConfig.defaultPanelY(720) == 240, "720 高 → 默认 y 240");
        check(HudConfig.defaultPanelY(360) == 120, "360 高 → 默认 y 120");
        check(HudConfig.defaultPanelY(361) == 120, "361 高 → 四舍五入到 120");
        check(HudConfig.defaultPanelY(0) == 0, "尺寸未知 → 0（不抛异常）");
        HudConfig.reset();
        c = HudConfig.get();
        check(c.selfStatsY == -1 && c.bothY == -1 && c.containerY == -1 && c.bothContainerY == -1,
                "新配置的四套面板 y 都应是动态默认哨兵");
        HudConfig.Panel defBig = HudConfig.panel(0, false, 1280, 720);
        check(defBig.x == 4 && defBig.y == 240,
                "默认位置 x=4、y=240（720 高），实际 " + defBig.x + "," + defBig.y);
        check(HudConfig.panel(0, false, 640, 360).y == 120, "窗口矮一半 → 默认 y 也到 120");
        HudConfig.movePanel(0, false, 10, 500, 640, 360);
        check(HudConfig.panel(0, false, 640, 360).y == 500, "拖到 500 应显示 500");
        HudConfig.resetPanelPosition(0, false);
        check(c.selfStatsY == -1, "回到初始位置应写回哨兵");
        check(HudConfig.panel(0, false, 640, 360).y == 120, "回到初始位置 → 屏幕高 ÷ 3");
        HudConfig.resetPanelPosition(1, false);
        check(c.containerY == -1, "容器那套「回到初始位置」也应是动态默认");
        check(HudConfig.panel(1, false, 640, 360).y == 120, "容器那套默认 y = 屏幕高 ÷ 3");
        HudConfig.resetPanelPosition(2, false);
        check(c.bothY == -1, "模式 2 的 HUD 那套回到动态默认");
        HudConfig.resetPanelPosition(2, true);
        check(c.bothContainerY == -1, "模式 2 的容器那套也回到动态默认");
        check(HudConfig.panel(2, true, 640, 360).y == 120, "模式 2 容器那套默认 y = 屏幕高 ÷ 3");

        // ⑧ v19 → v20 迁移：只有仍停在旧默认 40 的才换成哨兵
        HudConfig.Cfg m1 = new HudConfig.Cfg();
        m1.configVersion = 19;
        m1.selfStatsY = 40;
        m1.bothY = 40;
        HudConfig.migrate("{\"configVersion\":19}", m1);
        check(m1.selfStatsY == -1 && m1.bothY == -1, "旧默认 40 → 动态默认哨兵");
        HudConfig.Cfg m2 = new HudConfig.Cfg();
        m2.configVersion = 19;
        m2.selfStatsY = 123;
        m2.bothY = 77;
        HudConfig.migrate("{\"configVersion\":19}", m2);
        check(m2.selfStatsY == 123 && m2.bothY == 77, "自己拖过的位置不该被迁移改掉");
        check(m2.containerY == -1 && m2.bothContainerY == -1,
                "容器那两套仍停在旧默认 20 → 也应换成动态默认");
        HudConfig.Cfg m3 = new HudConfig.Cfg();
        m3.configVersion = 20;
        m3.containerY = 55;
        m3.bothContainerY = 66;
        HudConfig.migrate("{\"configVersion\":20}", m3);
        check(m3.containerY == 55 && m3.bothContainerY == 66, "容器那两套拖过的位置不该被迁移改掉");
        HudConfig.Cfg neg = new HudConfig.Cfg();
        neg.selfStatsY = -5;
        neg.bothY = -3;
        HudConfig.normalize(neg);
        check(neg.selfStatsY == -1 && neg.bothY == -1, "小于 -1 的 y 应夹到 -1（动态默认）");
        HudConfig.Cfg neg2 = new HudConfig.Cfg();
        neg2.containerY = -9;
        neg2.bothContainerY = -2;
        HudConfig.normalize(neg2);
        check(neg2.containerY == -1 && neg2.bothContainerY == -1, "容器那两套的非法 y 也夹到 -1");
        HudConfig.reset();
    }

    /**
     * 滚动区的上下限与裁剪。
     *
     * <p>「下限超出屏幕」的根因是内容控件由 {@code Screen} 自动绘制、没有裁剪；
     * 现在内容只在 {@code [top, bottom)} 里画。这里锁住可视区数学：
     * 下沿 = 高度 - 56，至少留一行，滚动量夹在 [0, content - viewport]。</p>
     */
    private static void scrollBand() {
        ScrollPane pane = new ScrollPane(52, 200);
        check(pane.top() == 52 && pane.bottom() == 200, "可视区应为 [52,200)");
        check(pane.viewportHeight() == 148, "可视高应为 148");
        pane.contentHeight(500);
        check(pane.maxScroll() == 352, "最大滚动 = 内容高 - 可视高: " + pane.maxScroll());
        check(pane.screenY(0) == 52, "内容 0 应贴在 top");
        check(pane.intersects(0, 20), "第一行应相交");
        check(pane.intersects(140, 20), "部分露出的行也应相交（由裁剪切掉下半截）");
        check(!pane.intersects(148, 20), "完全在可视区下方的行不该相交");
        check(!pane.intersects(-30, 20), "完全在可视区上方的行不该相交");
        pane.scrollBy(9999);
        check(pane.scroll() == 352, "滚动量应夹到上限");
        check(pane.screenY(0) == 52 - 352, "滚到底后首行应移出可视区");
        pane.scrollBy(-9999);
        check(pane.scroll() == 0, "滚动量不该小于 0");

        // 可视区至少一行；正常窗口的下沿 = 高度 - 56（提示文本 / 返回按钮之上）
        check(new ScrollPane(52, 60).bottom() == 72, "可视区至少留 20px");
        check(new ScrollPane(52, 300 - 56).bottom() == 244, "可视区下沿应为 高度 - 56");
        check(new ScrollPane(52, 200).viewportHeight() == 148
                && new ScrollPane(52, 200).maxScroll() == 0, "内容为空时不该能滚");
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
