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
        barSlicing();
        configNormalization();
        armorEdgeCases();
        heartCountsAndRows();
        armourPaging();
        heartCycling();
        bigNumbers();
        barOwnershipScoring();
        screenOcclusion();
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

    // ── 1b. 纯客户端（只有百分比）至少要有数字 ──

    /**
     * 纯客户端只有 {@code below_name = HealthPercent} 时：血条 + 「73%」数字。
     *
     * <p>1.20.7 起 percentOnly 不再只画条：数字行写百分比（口径 = hp*100/maxHp 四舍五入夹 0~100）。</p>
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
            EnemyHudLayout.Options o = new EnemyHudLayout.Options();
            o.showNumbers = true;
            o.showBar = true;
            var r = run("percentOnly " + c[0] + "/" + c[1], d, o);
            if (r == null) continue;
            check(r.hasText, "percentOnly " + c[0] + "/" + c[1] + " 应有文字");
            check(flatten(r).equals(c[2] + "%"),
                    "percentOnly " + c[0] + "/" + c[1] + " 应显示 " + c[2] + "%，实际 " + flatten(r));
            check(r.barWidth > 0, "percentOnly 仍应有血条宽度");

            o.showNumbers = false;
            var r2 = run("percentOnly 无数字 " + c[0] + "/" + c[1], d, o);
            if (r2 != null) {
                check(!r2.hasText, "关掉数字后 percentOnly 不该有文字");
            }
        }

        // 队友没有队伍条条目：同样只有 HealthPercent → 也要有百分比数字
        HeartCounts unknown = HeartCounts.unknown();
        EnemyHudLayout.Options team = new EnemyHudLayout.Options();
        team.showNumbers = true;
        team.showArmor = false;
        team.playerStyle = true;
        team.showBar = true;
        var tr = EnemyHudLayout.compute(63, 100, true, 0, new int[7], unknown, team, SelfCheck::width);
        check(tr != null && flatten(tr).equals("63%"), "队友百分比回退应显示 63%，实际 " + flatten(tr));

        // 「拆不出心数但已知当前/上限」（队伍条那一份）仍然写 当前/最大
        var exact = EnemyHudLayout.compute(63, 120, false, 0, new int[7], unknown, team, SelfCheck::width);
        check(exact != null && flatten(exact).equals("63/120"),
                "未知心数的绝对值应退回 63/120，实际 " + flatten(exact));
    }

    /** 把布局里的所有文字段拼起来（测试断言用）。 */
    private static String flatten(EnemyHudLayout.Result r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        for (EnemyHudLayout.Line line : r.lines) sb.append(line.text);
        return sb.toString();
    }

    // ── 1c. 大厅里不该发 ViewStats / 失败提示要吞掉 ──

    /**
     * 1.20.8：大厅里不发 {@code /trigger ViewStats}，并且「无法触发该计分项」这类回显要被过滤。
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
     * 1.20.9：同名怪附近时，bossbar 只认血量差 ≤ {@link BossBarTracker#PERCENT_TOLERANCE} 的实体，
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
        // 1.20.10：距离优先于粘性，但 2 格以内不换绑
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

    // ── 1f. 属性面板显示方式：两套配置 ──

    /** 1.20.14：HUD / 容器界面各一套位置·列数·缩放·不透明度；模式 0/1。 */
    private static void panelModes() {
        EnemyHudConfig.Cfg c = EnemyHudConfig.get();
        int oldMode = c.selfStatsMode;
        try {
            c.selfStatsMode = 0;
            check(!EnemyHudConfig.containerMode(), "模式 0 不该是容器模式");
            c.selfStatsX = 11;
            c.selfStatsColumns = 3;
            c.selfStatsScale = 1.5;
            c.selfStatsOpacity = 0.6;
            EnemyHudConfig.Panel hud = EnemyHudConfig.panel();
            check(hud.x == 11 && hud.columns == 3 && hud.scale == 1.5 && hud.opacity == 0.6,
                    "HUD 模式应取 selfStats* 那一套");

            c.selfStatsMode = 1;
            check(EnemyHudConfig.containerMode(), "模式 1 应是容器模式");
            check(!EnemyHudConfig.hudMode(), "模式 1 不应在 HUD 上画");
            c.selfStatsMode = 2;
            check(EnemyHudConfig.containerMode() && EnemyHudConfig.hudMode(), "模式 2 应 HUD + 容器都画");
            c.selfStatsMode = 1;   // 后面的断言按「容器模式」继续
            c.containerX = 22;
            c.containerColumns = 1;
            c.containerScale = 2.0;
            c.containerOpacity = 0.3;
            EnemyHudConfig.Panel box = EnemyHudConfig.panel();
            check(box.x == 22 && box.columns == 1 && box.scale == 2.0 && box.opacity == 0.3,
                    "容器模式应取 container* 那一套");

            // 移动只改当前模式那一套
            EnemyHudConfig.movePanel(33, 44);
            check(c.containerX == 33 && c.containerY == 44, "容器模式下移动应写 container*");
            check(c.selfStatsX == 11, "HUD 那一套不该被改");
            EnemyHudConfig.setPanelColumns(2);
            check(c.containerColumns == 2 && c.selfStatsColumns == 3, "setPanelColumns 应只改当前模式");

            // 模式 2 有自己的一套（v8）
            c.selfStatsMode = 2;
            c.bothX = 55;
            c.bothColumns = 1;
            c.bothScale = 1.2;
            c.bothOpacity = 0.5;
            EnemyHudConfig.Panel both = EnemyHudConfig.panel();
            check(both.x == 55 && both.columns == 1 && both.scale == 1.2 && both.opacity == 0.5,
                    "模式 2 应取 both* 那一套");
            EnemyHudConfig.movePanel(2, false, 66, 77);
            check(c.bothX == 66 && c.bothY == 77, "模式 2（HUD）移动应写 both*");
            check(c.selfStatsX == 11 && c.containerX == 33, "模式 2 移动不该改另外两套");
            // 模式 2 的容器那一套独立
            c.bothContainerX = 88;
            c.bothContainerColumns = 3;
            EnemyHudConfig.Panel bothBox = EnemyHudConfig.panel(2, true);
            check(bothBox.x == 88 && bothBox.columns == 3, "模式 2 容器应取 bothContainer*");
            EnemyHudConfig.movePanel(2, true, 99, 111);
            check(c.bothContainerX == 99 && c.bothX == 66, "模式 2 容器移动应只改 bothContainer*");
        } finally {
            c.selfStatsMode = oldMode;
        }

        // 迁移：v6 之前没有 container*，应从 selfStats* 复制
        EnemyHudConfig.Cfg old = new EnemyHudConfig.Cfg();
        old.configVersion = 6;
        old.selfStatsX = 123;
        old.selfStatsY = 45;
        old.selfStatsColumns = 3;
        old.selfStatsScale = 1.3;
        old.selfStatsOpacity = 0.7;
        EnemyHudConfig.migrate("{\"configVersion\":6}", old);
        check(old.containerX == 123 && old.containerY == 45 && old.containerColumns == 3,
                "v6 → v7 应把 HUD 配置复制给容器那套");
        check(Math.abs(old.containerScale - 1.3) < 1e-9 && Math.abs(old.containerOpacity - 0.7) < 1e-9,
                "v6 → v7 缩放/不透明度也应复制");

        // 夹取：模式 0~1、容器缩放 ≥ 1.0
        EnemyHudConfig.Cfg wild = new EnemyHudConfig.Cfg();
        wild.selfStatsMode = 9;
        wild.containerScale = 0.1;
        wild.containerColumns = 9;
        wild.selfStatsScale = 0.1;
        EnemyHudConfig.normalize(wild);
        check(wild.selfStatsMode == 2, "selfStatsMode 9 应夹到 2");
        check(wild.containerScale == 1.0, "containerScale 0.1 应夹到 1.0（字号最小 1.00）");
        check(wild.containerColumns == 3, "containerColumns 9 应夹到 3");
        check(wild.selfStatsScale == 1.0, "selfStatsScale 0.1 应夹到 1.0（字号最小 1.00）");
        EnemyHudConfig.Cfg shield = new EnemyHudConfig.Cfg();
        shield.shieldScale = 99.0;
        shield.shieldOpacity = 0.0;
        shield.shieldX = 99999;
        EnemyHudConfig.normalize(shield);
        check(shield.shieldScale == EnemyHudConfig.SHIELD_SCALE_MAX, "shieldScale 应夹到 1.5");
        check(shield.shieldOpacity == 0.1, "shieldOpacity 应夹到 0.1");
        check(shield.shieldX == 4000, "shieldX 应夹到 4000");
        EnemyHudConfig.Cfg tiny = new EnemyHudConfig.Cfg();
        tiny.shieldScale = 0.01;
        EnemyHudConfig.normalize(tiny);
        check(tiny.shieldScale == EnemyHudConfig.SHIELD_SCALE_MIN, "shieldScale 应兜到 0.5");

        // 1.20.22：缩放是「整张纹理等比缩放」，倍率取 0.5~1.5 的小数
        check(ShieldIndicatorHud.clampScale(Double.NaN) == 1.0, "NaN 倍率应回落 1.0");
        check(ShieldIndicatorHud.clampScale(9.0) == 1.5, "9 倍应夹到 1.5");
        check(ShieldIndicatorHud.clampScale(0.1) == 0.5, "0.1 倍应夹到 0.5");
        check(ShieldIndicatorHud.scaledWidth(1.0) == ShieldIndicatorHud.TEX_W, "1.0 倍宽 = 原纹理宽");
        check(ShieldIndicatorHud.scaledHeight(1.0) == ShieldIndicatorHud.TEX_H, "1.0 倍高 = 原纹理高");
        check(ShieldIndicatorHud.scaledWidth(0.5) == 18, "0.5 倍宽应为 18");
        check(ShieldIndicatorHud.scaledHeight(0.5) == 53, "0.5 倍高应为 53");
        check(ShieldIndicatorHud.scaledWidth(1.5) == 54, "1.5 倍宽应为 54");
        check(ShieldIndicatorHud.scaledHeight(1.5) == 159, "1.5 倍高应为 159");
        check(ShieldIndicatorHud.scaledWidth(1.2) == 43, "1.2 倍宽应为 43");
        check(ShieldIndicatorHud.scaledWidth(99.0) == 54, "越界倍率应按 1.5 算宽");
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
        check(defaults.scale == 0.9, "默认缩放应为 0.9: " + defaults.scale);
        check(defaults.teamScale == 0.9, "队友默认缩放也应为 0.9: " + defaults.teamScale);
        check(defaults.verticalOffset == 0, "默认垂直偏移应为 0");
        check(defaults.opacity == 0.75, "默认透明度应为 0.75");
        check(defaults.showBar, "默认应画血条");
        check(!defaults.hideBehindWalls, "1.20.14 起默认不再按墙隐藏");
        check(!defaults.hideBehindEntities, "1.20.15 起被其它实体挡住时不再隐藏（写死关闭）");
        check(defaults.autoPartyBossbar, "1.20.15 起自动开队伍条写死开启");
        EnemyHudConfig.Cfg forcedCfg = new EnemyHudConfig.Cfg();
        forcedCfg.hideBehindWalls = true;
        forcedCfg.hideBehindEntities = true;
        forcedCfg.autoPartyBossbar = false;
        EnemyHudConfig.normalize(forcedCfg);
        check(!forcedCfg.hideBehindWalls && !forcedCfg.hideBehindEntities && forcedCfg.autoPartyBossbar,
                "normalize 应把这三项写死");
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
        current.configVersion = 2;
        current.barWidth = 100.0;
        EnemyHudConfig.migrate("{\"configVersion\":2,\"barWidth\":100.0}", current);
        check(current.barWidth == 100.0, "新配置里显式写 100 不该被改: " + current.barWidth);

        // v2 的旧默认缩放 1.0 → 0.9（1.19.0 起），自己调过的保留。
        EnemyHudConfig.Cfg v2 = new EnemyHudConfig.Cfg();
        v2.configVersion = 2;
        v2.scale = 1.0;
        v2.teamScale = 1.0;
        EnemyHudConfig.migrate("{\"configVersion\":2,\"scale\":1.0}", v2);
        check(v2.scale == 0.9 && v2.teamScale == 0.9,
                "v2 的旧默认缩放应迁移到 0.9: " + v2.scale + "/" + v2.teamScale);

        EnemyHudConfig.Cfg customScale = new EnemyHudConfig.Cfg();
        customScale.configVersion = 2;
        customScale.scale = 1.25;
        EnemyHudConfig.migrate("{\"configVersion\":2,\"scale\":1.25}", customScale);
        check(customScale.scale == 1.25, "自己调过的缩放不该被迁移覆盖: " + customScale.scale);

        EnemyHudConfig.Cfg v3 = new EnemyHudConfig.Cfg();
        v3.configVersion = 3;
        v3.scale = 1.0;
        EnemyHudConfig.migrate("{\"configVersion\":3,\"scale\":1.0}", v3);
        check(v3.scale == 1.0, "已经是 v3 的配置不该再迁移: " + v3.scale);

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

        // 文字行：红心 + **一种**其它心（由 HeartCycle 挑出来，这里直接指定 cycleKind）。
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showArmor = false;
        o.showBar = false;
        o.cycleKind = HeartCounts.SOUL;
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

        // 敌人：碎心/粉心/蓝心也走轮换，每次只显示一种，固定一行。
        o.cycleKind = HeartCounts.CRACKED;
        EnemyHudLayout.Result crackedLine = EnemyHudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(50, 0, 0, 0, 5), o, SelfCheck::width);
        check(crackedLine.rowCount() == 1, "敌人心数固定一行: " + crackedLine.rowCount());
        check(rowText(crackedLine, 0).equals("❤50/50 💔5"),
                "轮换到碎心时应显示: " + rowText(crackedLine, 0));
        o.cycleKind = HeartCounts.PINK;
        EnemyHudLayout.Result pinkLine = EnemyHudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(50, 0, 0, 0, -5), o, SelfCheck::width);
        check(rowText(pinkLine, 0).equals("❤50/50 💗5"),
                "轮换到粉心时应显示: " + rowText(pinkLine, 0));
        check(HeartCounts.COLORS[HeartCounts.CRACKED] == 0xAA0000, "碎心配色应为 dark_red");
        check(HeartCounts.COLORS[HeartCounts.PINK] == 0xFF55FF, "粉心配色应为 light_purple");

        o.cycleKind = HeartCounts.BLUE;
        EnemyHudLayout.Result blueLine = EnemyHudLayout.compute(50, 50, false, 0,
                new int[7], HeartCounts.of(30, 0, 0, 20, 0), o, SelfCheck::width);
        check(rowText(blueLine, 0).equals("❤30/50 💙20"),
                "轮换到蓝心时应显示: " + rowText(blueLine, 0));

        // 轮换到该类心不存在时，只显示红心。
        EnemyHudLayout.Result noBlue = EnemyHudLayout.compute(50, 50, false, 0, new int[7],
                HeartCounts.of(50, 0, 0, 0, 0), o, SelfCheck::width);
        check(rowText(noBlue, 0).equals("❤50/50"), "不存在的类型不该显示: " + rowText(noBlue, 0));
        o.cycleKind = HeartCounts.SOUL;

        // 拆不出心数（percentOnly）→ 1.20.7 起显示百分比数字；已知当前/上限但拆不出心数 → 当前/最大。
        EnemyHudLayout.Result fallback = run("回退", new Data("只有百分比", 73, 100, true, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), o);
        check(join(fallback).equals("73%"), "只有百分比时应显示 73%: " + join(fallback));

        EnemyHudLayout.Options noHearts = new EnemyHudLayout.Options();
        noHearts.showHearts = false;
        noHearts.showArmor = false;
        noHearts.showBar = false;
        EnemyHudLayout.Result numbers = run("关掉心数", new Data("普通", 160, 100, false, 0,
                new int[]{0, 0, 0, 0, 0, 0, 0}), noHearts);
        check(join(numbers).equals("160/100"), "关掉心数应退回绝对血量: " + join(numbers));

        // 1.20.0：护甲在最上面一行，🛡 与元素护甲之间**两个空格**。
        EnemyHudLayout.Options withArmor = new EnemyHudLayout.Options();
        withArmor.showBar = false;
        withArmor.cycleKind = HeartCounts.SOUL;
        EnemyHudLayout.Result narrowArmor = run("窄护甲", new Data("一个护甲", 100, 100, false, 0,
                new int[]{3, 0, 0, 0, 0, 0, 0}), withArmor);
        check(narrowArmor.rowCount() == 2, "有护甲时固定两行: " + narrowArmor.rowCount());
        check(rowText(narrowArmor, 0).equals("\uD83D\uDD253"), "护甲行在最上面: " + rowText(narrowArmor, 0));
        check(rowText(narrowArmor, 1).equals("❤100/100"), "心数行在护甲下面: " + rowText(narrowArmor, 1));
        check(!join(narrowArmor).contains("|"), "分成两行之后不再需要分隔符: " + join(narrowArmor));

        EnemyHudLayout.Result armored = run("心数+护甲", new Data("带护甲", 160, 100, false, 3,
                new int[]{0, 5, 0, 0, 0, 0, 0}), withArmor);
        check(armored.rowCount() == 2, "有护甲时固定两行: " + armored.rowCount());
        check(rowText(armored, 0).equals("\uD83D\uDEE13  " + EnemyData.ARMOR_ICONS[1] + "5"),
                "护甲与元素护甲之间应有两个空格: " + rowText(armored, 0));
        check(rowText(armored, 1).equals("❤100/100 💛60"),
                "第二行应是红心 + 一种轮换心: " + rowText(armored, 1));
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

        // 敌人有 5 类心时，心数行也只出现「当前那一种」。
        EnemyHudLayout.Options oneKind = new EnemyHudLayout.Options();
        oneKind.showBar = false;
        oneKind.cycleKind = HeartCounts.BLACK;
        EnemyHudLayout.Result one = EnemyHudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), oneKind, SelfCheck::width);
        check(one.rowCount() == 1, "敌人心数固定一行: " + one.rowCount());
        check(rowText(one, 0).equals("❤60/150 🖤30"),
                "敌人只显示一种轮换心: " + rowText(one, 0));

        // 玩家排版（playerStyle）：碎心/粉心 + 蓝心固定在最上面一行，心数行只轮换魂/黑。
        EnemyHudLayout.Options teamLike = new EnemyHudLayout.Options();
        teamLike.showHearts = true;
        teamLike.showArmor = true;
        teamLike.showBar = false;
        teamLike.playerStyle = true;
        teamLike.cycleKind = HeartCounts.SOUL;

        EnemyHudLayout.Result teamNoHearts = EnemyHudLayout.compute(73, 120, false, 0, new int[7],
                HeartCounts.unknown(), teamLike, SelfCheck::width);
        check(join(teamNoHearts).equals("73/120"),
                "队友拿不到心数时应退回当前/最大: " + join(teamNoHearts));

        EnemyHudLayout.Result teamHearts = EnemyHudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), teamLike, SelfCheck::width);
        check(teamHearts.rowCount() == 2, "玩家：特殊心 + 心数，共两行: " + teamHearts.rowCount());
        check(rowText(teamHearts, 0).equals("💔2 💙20"),
                "玩家最上面一行 = 碎心 + 蓝心: " + rowText(teamHearts, 0));
        check(rowText(teamHearts, 1).equals("❤60/150 💛40"),
                "玩家心数行只轮换魂/黑: " + rowText(teamHearts, 1));

        teamLike.cycleKind = HeartCounts.BLACK;
        EnemyHudLayout.Result teamBlack = EnemyHudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 30, 20, 2), teamLike, SelfCheck::width);
        check(rowText(teamBlack, 1).equals("❤60/150 🖤30"),
                "轮换到黑心时: " + rowText(teamBlack, 1));

        teamLike.cycleKind = HeartCounts.SOUL;
        EnemyHudLayout.Result teamPlain = EnemyHudLayout.compute(150, 150, false, 0, new int[7],
                HeartCounts.of(60, 40, 0, 0, 0), teamLike, SelfCheck::width);
        check(teamPlain.rowCount() == 1, "玩家没有碎心/蓝心时只有一行: " + teamPlain.rowCount());
        check(rowText(teamPlain, 0).equals("❤60/150 💛40"),
                "玩家心数行=" + rowText(teamPlain, 0));

        // 玩家排版下，红心与轮换心的顺序固定：红心在前。
        check(rowText(teamHearts, 1).startsWith("❤60/150"), "红心永远在最前面");

        // 1.16.3 起：队友**只画心与血条**，护甲段一律不画 —— 即使 payload 带了 defence / 元素护甲。
        EnemyHudLayout.Options teamNoArmor = new EnemyHudLayout.Options();
        teamNoArmor.showHearts = true;
        teamNoArmor.showArmor = false;
        teamNoArmor.showBar = true;
        teamNoArmor.barWidth = 100;
        teamNoArmor.playerStyle = true;
        teamNoArmor.cycleKind = HeartCounts.SOUL;

        EnemyHudLayout.Result teamArmored = EnemyHudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                teamNoArmor, SelfCheck::width);
        String teamFlat = join(teamArmored);
        check(!teamFlat.contains("\uD83D\uDEE1"), "队友不该出现 🛡 护甲段: " + teamFlat);
        check(!teamFlat.contains(EnemyData.ARMOR_ICONS[1]), "队友不该出现 🔥 元素护甲段: " + teamFlat);
        check(!teamFlat.contains("|"), "队友不该出现护甲分隔符: " + teamFlat);
        // join() 把几行首尾相接，去掉空格再比（玩家：特殊心行 + 红心/轮换心行）。
        check(teamFlat.replace(" ", "").equals("💔2💙20❤60/150💛40"),
                "队友这几行只应有心: " + teamFlat);
        check(teamArmored.barWidth == 100, "队友的血条仍要画: " + teamArmored.barWidth);

        // 同样的数据给敌人：护甲在最上面，心数行只有一种轮换心。
        EnemyHudLayout.Options enemyArmor = new EnemyHudLayout.Options();
        enemyArmor.showHearts = true;
        enemyArmor.showArmor = true;
        enemyArmor.showBar = false;
        enemyArmor.barWidth = 100;
        enemyArmor.cycleKind = HeartCounts.BLACK;
        EnemyHudLayout.Result mobArmored = EnemyHudLayout.compute(150, 150, false, 5,
                new int[]{3, 1, 0, 0, 0, 0, 0}, HeartCounts.of(60, 40, 30, 20, 2),
                enemyArmor, SelfCheck::width);
        check(rowText(mobArmored, 0).equals("\uD83D\uDEE15  \uD83D\uDD253 \u26031"),
                "敌人护甲行（两个空格 + 元素护甲）: " + rowText(mobArmored, 0));
        check(rowText(mobArmored, 1).equals("❤60/150 🖤30"),
                "敌人心数行只显示一种轮换心: " + rowText(mobArmored, 1));
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
            o.cycleKind = HeartCounts.SOUL;
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
        p0.cycleKind = HeartCounts.SOUL;
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

    // ── 4c. 心类轮换（1.20.0） ──

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

    // ── 4d. 大数值缩写（1.17.0） ──

    private static void bigNumbers() {
        check(EnemyHudLayout.num(0).equals("0"), "num(0)");
        check(EnemyHudLayout.num(99999).equals("99999"), "num(99999)=" + EnemyHudLayout.num(99999));
        check(EnemyHudLayout.num(100_000).equals("100K"), "num(100000)=" + EnemyHudLayout.num(100_000));
        check(EnemyHudLayout.num(123_456).equals("123K"), "num(123456)=" + EnemyHudLayout.num(123_456));
        check(EnemyHudLayout.num(999_999).equals("999K"), "num(999999)=" + EnemyHudLayout.num(999_999));
        check(EnemyHudLayout.num(1_234_567).equals("1.2M"), "num(1234567)=" + EnemyHudLayout.num(1_234_567));
        check(EnemyHudLayout.num(12_345_678).equals("12.3M"), "num(12345678)="
                + EnemyHudLayout.num(12_345_678));
        check(EnemyHudLayout.num(999_999_999).equals("999M"), "num(999999999)="
                + EnemyHudLayout.num(999_999_999));
        check(EnemyHudLayout.num(Integer.MAX_VALUE).equals("2.1B"), "num(MAX)="
                + EnemyHudLayout.num(Integer.MAX_VALUE));
        check(EnemyHudLayout.num(Integer.MIN_VALUE).startsWith("-"), "num(MIN) 应带负号: "
                + EnemyHudLayout.num(Integer.MIN_VALUE));
        check(EnemyHudLayout.num(-123_456).equals("-123K"), "num(-123456)="
                + EnemyHudLayout.num(-123_456));

        // 缩写的目的是行宽可控：五段 999999 以前约 40 个字符，现在每段 4~5 个。
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showHearts = true;
        o.showBar = false;
        o.barWidth = 100;
        o.cycleKind = HeartCounts.SOUL;
        String text = join(EnemyHudLayout.compute(999_999, 999_999, false, 0, new int[7],
                HeartCounts.of(999_999, 999_999, 999_999, 999_999, 999_999), o, SelfCheck::width));
        check(!text.contains("999999"), "大数值应先缩写: " + text);
        check(text.contains("999K/999K"), "大数值缩写格式: " + text);
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

    // ── 血条分段（端盖原尺寸 + 中段拉伸） ──

    private static void barSlicing() {
        final float nw = BossBarTexture.NATIVE_WIDTH;
        final float cap = BossBarTexture.CAP_TEXELS;

        check(nw == 182 && BossBarTexture.NATIVE_HEIGHT == 5, "原版 boss_bar 纹理应为 182x5");
        check(cap * 2 < EnemyHudLayout.BAR_WIDTH_MIN,
                "两个端盖加起来必须短于最短的条: cap=" + cap);
        check(BossBarTexture.NATIVE_HEIGHT == EnemyHudLayout.BAR_HEIGHT,
                "纹理高 5 纹素应正好等于条的布局高度，否则端盖会被压扁");

        // ① 整条背景：左右端盖都是 4 纹素宽、UV 贴住纹理两端，中段吃长度差
        for (int bw = EnemyHudLayout.BAR_WIDTH_MIN; bw <= EnemyHudLayout.BAR_WIDTH_MAX; bw++) {
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
        for (int bw = EnemyHudLayout.BAR_WIDTH_MIN; bw <= EnemyHudLayout.BAR_WIDTH_MAX; bw++) {
            float xa = -bw / 2.0f;
            float xb = xa + bw;
            float[][] seg = BossBarTexture.slices(xa, xb, 0.0f, nw);
            check(seg.length == 3 && seg[2][3] == nw && seg[2][1] == xb,
                    "满格进度 bw=" + bw + " 应露出右端盖");
        }

        // ③ 进度没满 → 左端盖 + 中段，右边界直切在 fill 上、不采纹理最右端
        for (int bw = EnemyHudLayout.BAR_WIDTH_MIN; bw <= EnemyHudLayout.BAR_WIDTH_MAX; bw++) {
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

    // ── 自身属性面板（1.20.3：/trigger ViewStats 抓取 + 布局） ──

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
        check(SelfStats.looksLikeStat("50\u2764"), "数字开头的行应算属性行");
        check(SelfStats.looksLikeStat("\uD83D\uDDE13"), "带 🗡 的行应算属性行");
        check(!SelfStats.looksLikeStat("hello world"), "普通聊天不该算属性行");
        check(!SelfStats.looksLikeStat("   "), "空白行不该算属性行");
        check(SelfStats.isGameNotStarted("Game not started"), "英文「游戏没开始」应识别");
        check(SelfStats.isGameNotStarted("游戏未开始"), "中文「游戏未开始」应识别");
        check(!SelfStats.isGameNotStarted("50\u2764"), "属性行不该被当成「游戏没开始」");
        check(SelfStats.isViewStatsLine("Misc_View_Stats_1"), "ViewStats 标题行应被识别");
        check(SelfStats.isViewStatsLine("Misc_View_Stats_78"), "ViewStats 计时行应被识别");
        check(!SelfStats.isViewStatsLine("50\u2764"), "属性行不算 ViewStats 标题行");
        check(SelfStats.CAPTURE_WINDOW_MS > 0
                && SelfStats.CAPTURE_HARD_MS > SelfStats.CAPTURE_WINDOW_MS,
                "采集窗口应大于 0 且硬上限大于窗口");

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
        EnemyHudConfig.Cfg c = new EnemyHudConfig.Cfg();
        c.selfStatsSeconds = -5;
        c.selfStatsColumns = 9;
        c.selfStatsX = -100;
        c.selfStatsY = 99999;
        EnemyHudConfig.normalize(c);
        check(c.selfStatsSeconds == 1, "selfStatsSeconds -5 应夹到 1");
        EnemyHudConfig.Cfg big = new EnemyHudConfig.Cfg();
        big.selfStatsSeconds = 999;
        check(EnemyHudConfig.normalize(big).selfStatsSeconds == 5, "selfStatsSeconds 999 应夹到 5");
        check(c.selfStatsColumns == 3, "selfStatsColumns 9 应夹到 3");
        check(c.selfStatsX == 0, "selfStatsX -100 应夹到 0");
        check(c.selfStatsY == 4000, "selfStatsY 99999 应夹到 4000");
        check(EnemyHudConfig.CURRENT_VERSION == 11, "配置版本应为 11");
        EnemyHudConfig.Cfg defaults = EnemyHudConfig.normalize(new EnemyHudConfig.Cfg());
        check(defaults.showSelfStats, "自身属性面板默认开启");
        check(defaults.selfStatsSeconds == 3, "自身属性面板默认 3 s 刷新");
        check(defaults.selfStatsColumns == 2, "自身属性面板默认两列");
        EnemyHudConfig.Cfg v3 = new EnemyHudConfig.Cfg();
        v3.configVersion = 3;
        v3.barWidth = 90;
        EnemyHudConfig.migrate("{\"configVersion\":3}", v3);
        check(v3.configVersion == 11, "v3 → v11 应升版本号");
        check(v3.barWidth == 90, "迁移不该动已调过的值");
    }

    /** 1.20.5：按单个属性分类 / 提示行过滤 / 可滚动过滤列表 / 缩放透明度。 */
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
        check(kindOfKey(16) == SelfStatsKind.DEFENCE_AMP, "键 16 应归为防御倍率");
        check(kindOfKey(18) == SelfStatsKind.TRUE_ARMOR_AMP, "键 18 应归为真伤护甲倍率");
        check(kindOfKey(20) == SelfStatsKind.HEAL_AMP, "键 20 应归为治疗倍率");
        check(kindOfKey(25) == SelfStatsKind.FIRE_ARMOR_AMP, "键 25 应归为火焰护甲倍率");
        check(kindOfKey(33) == SelfStatsKind.SPEED_AMP, "键 33 应归为速度倍率");
        check(kindOfKey(40) == SelfStatsKind.MANA_AMP, "键 40 应归为法力倍率");
        check(kindOfKey(56) == SelfStatsKind.STATUS, "键 56 应归为状态效果");
        check(kindOfKey(78) == SelfStatsKind.TIMER, "键 78 应归为计时");

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

        // 过滤表：细类、粗类展开、非法值
        check(SelfStatsKind.FILTERABLE.length >= 50, "可过滤项应有 50 项以上，实际 " + SelfStatsKind.FILTERABLE.length);
        check(EnemyHudConfig.sanitizeKinds(java.util.List.of("heart_red")).size() == 1, "合法隐藏类应保留");
        check(EnemyHudConfig.sanitizeKinds(java.util.List.of("hint")).isEmpty(), "提示类不该能进过滤表");
        check(EnemyHudConfig.sanitizeKinds(java.util.List.of("nope")).isEmpty(), "未知类应被丢掉");
        check(EnemyHudConfig.sanitizeKinds(java.util.List.of("heart_red", "heart_red")).size() == 1,
                "重复的隐藏类应去重");
        check(EnemyHudConfig.sanitizeKinds(null).isEmpty(), "null 隐藏表应变成空表");
        java.util.List<String> expandedHealth = EnemyHudConfig.sanitizeKinds(java.util.List.of("health"));
        check(expandedHealth.size() == 7, "旧粗类 health 应展开成 7 个细类，实际 " + expandedHealth.size());
        check(expandedHealth.contains("hp_neg") && expandedHealth.contains("heal_percent"),
                "health 展开应包含负生命与治疗百分比");
        java.util.List<String> expandedAttack = EnemyHudConfig.sanitizeKinds(java.util.List.of("attack"));
        check(expandedAttack.size() == 9, "旧粗类 attack 应展开成 9 个细类，实际 " + expandedAttack.size());
        check(EnemyHudConfig.sanitizeKinds(java.util.List.of("misc")).contains("skulls"),
                "misc 展开应包含骷髅数");
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (SelfStatsKind kind : SelfStatsKind.FILTERABLE) {
            check(ids.add(kind.id), "过滤项 id 不能重复: " + kind.id);
            check(kind != SelfStatsKind.HINT, "HINT 不该出现在过滤列表");
        }

        // 双列按钮 + 滚动的纯函数
        check(SelfStatsFilterScreen.rowCount(60, 2) == 30, "60 项两列 = 30 行，实际 " + SelfStatsFilterScreen.rowCount(60, 2));
        check(SelfStatsFilterScreen.rowCount(61, 2) == 31, "61 项两列 = 31 行（最后一行只放一个）");
        check(SelfStatsFilterScreen.rowCount(5, 0) == 0, "列数 0 应得 0 行");
        check(SelfStatsFilterScreen.visibleRows(148) == 6, "148 px 列表应能放 6 行，实际 " + SelfStatsFilterScreen.visibleRows(148));
        check(SelfStatsFilterScreen.visibleRows(0) == 0, "列表高度 0 应放 0 行");
        check(SelfStatsFilterScreen.maxScroll(30, 6) == 24, "30 行 / 6 行可见 → 最大滚动 24");
        check(SelfStatsFilterScreen.maxScroll(5, 10) == 0, "内容比列表短时不该能滚");
        check(SelfStatsFilterScreen.clampScroll(-3, 24) == 0, "滚动量不该小于 0");
        check(SelfStatsFilterScreen.clampScroll(999, 24) == 24, "滚动量不该超过上限");
        check(SelfStatsFilterScreen.scrollByWheel(2, -1.0, 10) == 3, "向下滚一格 → +1 行");
        check(SelfStatsFilterScreen.scrollByWheel(0, 1.0, 10) == 0, "已在顶部再往上滚应保持 0");
        check(SelfStatsFilterScreen.scrollByWheel(9, 1.0, 10) == 8, "向上滚一格 → -1 行");
        check(SelfStatsFilterScreen.scrollByWheel(0, -0.5, 10) == 1, "半格滚动应至少动一行");
        check(SelfStatsFilterScreen.itemAt(0, 0, 2, 60) == 0, "第 0 行第 0 列 = 第 0 项");
        check(SelfStatsFilterScreen.itemAt(0, 1, 2, 60) == 1, "第 0 行第 1 列 = 第 1 项");
        check(SelfStatsFilterScreen.itemAt(1, 0, 2, 60) == 2, "第 1 行第 0 列 = 第 2 项");
        check(SelfStatsFilterScreen.itemAt(29, 1, 2, 60) == 59, "第 29 行第 1 列 = 最后一项");
        check(SelfStatsFilterScreen.itemAt(29, 1, 2, 59) == -1, "最后一项之外应返回 -1");
        check(SelfStatsFilterScreen.itemAt(0, 2, 2, 60) == -1, "列号越界应返回 -1");
        check(SelfStatsFilterScreen.itemAt(-1, 0, 2, 60) == -1, "行号越界应返回 -1");

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
        EnemyHudConfig.Cfg cfg = new EnemyHudConfig.Cfg();
        check(EnemyHudConfig.CURRENT_VERSION == 11, "配置版本应为 11");
        EnemyHudConfig.Cfg defaults = EnemyHudConfig.normalize(new EnemyHudConfig.Cfg());
        check(defaults.showSelfStats, "自身属性面板默认开启");
        check(defaults.selfStatsSeconds == 3, "自身属性面板默认 3 s 刷新");
        check(defaults.selfStatsColumns == 2, "自身属性面板默认两列");
        check(defaults.selfStatsScale == 1.0 && defaults.selfStatsOpacity == 1.0, "默认字号/不透明度应为 1.0");
        check(defaults.selfStatsHidden.isEmpty(), "默认不隐藏任何属性");

        EnemyHudConfig.Cfg wild = new EnemyHudConfig.Cfg();
        wild.selfStatsScale = 99.0;
        wild.selfStatsOpacity = -3.0;
        wild.selfStatsHidden = new java.util.ArrayList<>(java.util.List.of("heart_red", "hint", "heart_red", "health"));
        EnemyHudConfig.normalize(wild);
        check(wild.selfStatsScale == 2.0, "selfStatsScale 99 应夹到 2.0");
        check(wild.selfStatsOpacity == 0.2, "selfStatsOpacity -3 应夹到 0.2");
        check(wild.selfStatsHidden.contains("heart_red") && !wild.selfStatsHidden.contains("hint"),
                "隐藏表应去掉 hint 且保留 heart_red");
        check(wild.selfStatsHidden.contains("hp_amp"), "隐藏表里的旧粗类 health 应被展开");

        EnemyHudConfig.Cfg v4 = new EnemyHudConfig.Cfg();
        v4.configVersion = 4;
        v4.barWidth = 90;
        EnemyHudConfig.migrate("{\"configVersion\":4}", v4);
        check(v4.configVersion == 11, "v4 → v11 应升版本号");
        check(v4.barWidth == 90, "迁移不该动已调过的值");

        // hiddenKinds() 与过滤真的作用到面板
        EnemyHudConfig.Cfg live = EnemyHudConfig.get();
        live.selfStatsHidden = new java.util.ArrayList<>();
        SelfStats.reset();
        check(SelfStats.lines().isEmpty(), "没有数据时 lines() 应为空");
        check(SelfStats.heartsOf(redHeartLine("120")).length == 7, "心数数组长度应为 7");
        live.selfStatsHidden.add(SelfStatsKind.HEART_RED.id);
        check(EnemyHudConfig.hiddenKinds().contains(SelfStatsKind.HEART_RED.id),
                "隐藏表应被 hiddenKinds() 读到");
        live.selfStatsHidden.clear();
        check(EnemyHudConfig.hiddenKinds().isEmpty(), "清空后 hiddenKinds() 应为空");
        EnemyHudConfig.reset();
        EnemyHudConfig.get().selfStatsHidden = new java.util.ArrayList<>();
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
     * 1.20.22：滚动区的上下限与裁剪。
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
