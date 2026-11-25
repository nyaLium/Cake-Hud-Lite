package com.nyalium.ctthudfork;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置文件 {@code config/ctt-hud-fork.json}（1.20.6 起改名；旧文件 {@code ctt-enemy-hud.json} 只在迁移时读一次）。
 *
 * <p>三条写入路径：① 首次启动写默认值；② Mod Menu 里改完点「完成」写回；
 * ③ 直接在磁盘上手改，游戏每 2 秒检查一次修改时间并热加载。</p>
 *
 * <p>字段分四组：</p>
 * <ul>
 *   <li>头顶条通用 / 敌人 / 队友（`barOpacity`、`textOpacity`、`barWidth`、`scale`、`team*`、`show*`…）；</li>
 *   <li>自身属性面板的 4 套参数：模式 0 用 `selfStats*`、模式 1 用 `container*`、
 *       模式 2 分 `both*`（HUD）/ `bothContainer*`（容器界面）；</li>
 *   <li>盾牌指示器：`shield*`（`shieldX/Y = -1` = 跟随副手槽）。</li>
 * </ul>
 */
public final class HudConfig {

    /** 配置字段。默认值 = 首次运行写出的内容。 */
    public static final class Cfg {
        /**
         * 配置版本。老文件没有这个键 → 读到的仍是默认值，靠 {@link #read()} 里的
         * 「JSON 里到底有没有这个键」来判定，见 {@link #CURRENT_VERSION}。
         */
        public int configVersion = CURRENT_VERSION;
        /** 总开关：false 时完全不绘制。 */
        public boolean enabled = true;
        /**
         * 血条（进度条那一条）的**不透明度**，0.10 ~ 1.00。
         *
         * <p>1.4.6 从原来单一的 {@code opacity} 拆出来：血条与上方文字各自独立。
         * 默认 <b>0.65</b>（与盾牌指示器的默认一致）。</p>
         */
        public double barOpacity = 0.65;
        /**
         * 血条上方**文字**（心数 / 护甲行）的不透明度，0.10 ~ 1.00（文字背景框按它的 25% 黑一起变）。
         *
         * <p>1.4.6 从 {@code opacity} 拆出来，默认 <b>1.00</b>（文字保持清晰）。</p>
         */
        public double textOpacity = 1.00;
        /**
         * 实体血条（名字 / 心数 / 护甲）是否带文字阴影。
         *
         * <p>1.20.32 起默认<b>关闭</b>，并且只作用于头顶血条 —— 属性面板另有
         * {@link #selfStatsTextShadow}（以前两者共用这一个字段，改实体血条会连带改面板）。</p>
         */
        public boolean textShadow = false;
        /**
         * 敌人头顶 HUD 的总开关（界面文案「显示血条」）。
         *
         * <p>1.20.34 起关闭时<b>整块都不画</b>：血条 + 上方的心数 / 护甲文字一起消失，
         * 只剩原版名字（以前只关血条、心数和护甲还挂着）。</p>
         */
        public boolean showBar = true;
        /**
         * 隐藏实体名称下方那条计分板分数（数据包的 HealthPercent 显示）。
         */
        public boolean hideBelowNameScore = true;
        /** 隐藏上方 Boss 栏里「最近敌人的信息」那一条（头顶已经画过了）。 */
        public boolean hideEnemyBossBar = true;
        /**
         * 队友头顶 HUD 的总开关（界面文案同样是「显示血条」，1.20.34 起统一）。
         *
         * <p>关闭时队友整块不画：血条 + 心数文字都消失（{@link HudTarget#managed} 直接返回 false），
         * 只剩原版名字。自己永远不显示。</p>
         */
        public boolean showTeammates = true;
    /** 是否绘制**自己**头顶的血条（1.4.0；默认关，设置页「玩家 → 显示自己的血条」）。 */
    public boolean showSelf = false;
        /** 隐藏上方 Boss 栏里「队伍血量」那一条（数据照常读取）。 */
        public boolean hideTeamBossBar = true;
        /** 玩家血条长度（原版名牌像素），默认 75。 */
        public double teamBarWidth = 75.0;
        /** 玩家血条的缩放比例（界面文案「缩放比例」）。 */
        public double teamScale = 0.9;
        /** 玩家血条最远显示距离（格，界面文案「最远显示距离」）。 */
        public double teamMaxDistance = 48.0;
        /** 超过这个距离（格）不再绘制。 */
        public double maxDistance = 48.0;
        /** 血条长度（原版名牌像素），50 ~ 150，默认 75。 */
        public double barWidth = 75.0;
        /** 整组 UI 的额外缩放，0.75 ~ 1.50，0.9 = 默认（1.19.0 起）。 */
        public double scale = 0.9;
        /** 左上角打印面板调试行（排查用，见 {@code SelfStatsPanel} 里 {@code debug} 分支）。 */
        public boolean debug = false;
        /**
         * 1.4.5 调试功能：**优先使用服务端数据**（服务端组件推来的 payload）。
         *
         * <p>关掉后完全忽略 payload，强制走「Boss 栏 + {@code HealthPercent}」的纯客户端路径。
         * 双端环境下也能测到纯客户端的观感（不用卸服务端组件）。默认<b>开</b> = 正常行为。</p>
         */
        public boolean preferServerData = true;
        /**
         * 1.4.5 调试功能：**筛选实体数量** —— 客户端最多保留 payload 的前 N 条。
         *
         * <p>服务端已经按「玩家（含自己）优先、其余按距离」排好序，所以截断前 N 条
         * 等价于「服务端只推了 N 条」，用它模拟上限较低/较高的服务端做对比。
         * 范围 1 ~ {@link MobHealthPayload#MAX_ENTRIES}（调大也不会超过服务端实际推的条数）。</p>
         */
        public int payloadLimit = MobHealthPayload.MAX_ENTRIES;
        /**
         * 自身属性面板（1.20.3）。纯客户端：定时 {@code /trigger ViewStats}，
         * 抓地图回的那些属性行画成面板，不依赖服务端组件。
         */
        public boolean showSelfStats = true;
        /** 属性面板自动刷新间隔（秒），1~5（1.0.0 起不再有「立即」档）；进他人服务器时强制 3 s（不改这里）。 */
        public int selfStatsSeconds = 3;
        /** 属性面板列数，1 ~ 3。 */
        public int selfStatsColumns = 2;
        /** 属性面板左上角屏幕坐标（GUI 像素，量在 {@link #layoutGuiW}/{@link #layoutGuiH} 这套尺寸下）。 */
        public int selfStatsX = 4;
        /**
         * 1.20.41：{@code -1} = **动态默认位置**「x=4、y = 屏幕高 ÷ {@value #DEFAULT_Y_DENOM}」
         * （左侧偏上），窗口大小变了也跟着走；拖动后这里存实际像素。
         */
        public int selfStatsY = DEFAULT_Y_SENTINEL;
        /**
         * 属性面板显示方式：0 = 始终显示（HUD），1 = 在容器界面显示，2 = HUD + 容器都显示（1.20.16）。
         * 每种上下文（HUD / 容器）各有一套位置·列数·不透明度。v1.0.0 起默认 2（HUD + 容器界面）。
         */
        public int selfStatsMode = 2;
        /** 容器界面那一套配置（模式 1 专用）。 */
        public int containerX = 4;
        /** 同 {@link #selfStatsY}：{@code -1} = 动态默认位置（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}；1.20.42）。 */
        public int containerY = DEFAULT_Y_SENTINEL;
        /** 容器那套的列数（1~3）。 */
        public int containerColumns = 2;
        /** 容器那套的不透明度（0.20~1.00）。 */
        public double containerOpacity = 1.0;
        /** 模式 2 在 HUD 上的那一套配置。 */
        public int bothX = 4;
        /** 同 {@link #selfStatsY}：{@code -1} = 动态默认位置（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}）。 */
        public int bothY = DEFAULT_Y_SENTINEL;
        /** 模式 2 HUD 那套的列数（1~3）。 */
        public int bothColumns = 2;
        /** 模式 2 HUD 那套的不透明度（0.20~1.00）。 */
        public double bothOpacity = 1.0;
        /** 模式 2 在容器界面上的那一套配置（1.20.18 起独立）。 */
        public int bothContainerX = 4;
        /** 同 {@link #selfStatsY}：{@code -1} = 动态默认位置（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}）。 */
        public int bothContainerY = DEFAULT_Y_SENTINEL;
        /** 模式 2 容器那套的列数（1~3）。 */
        public int bothContainerColumns = 2;
        /** 模式 2 容器那套的不透明度（0.20~1.00）。 */
        public double bothContainerOpacity = 1.0;
        /** 属性面板不透明度，0.20 ~ 1.00（1.20.4 起可调；1.0.0 起界面文案叫「不透明度」）。 */
        public double selfStatsOpacity = 1.0;
        /**
         * 属性面板文字阴影（1.20.32 起独立）。
         *
         * <p>以前面板跟着 {@link #textShadow}（实体血条那个）走，改一边会连带改另一边；现在分开，
         * 默认<b>开启</b> —— 和升级前的观感一致（那时候 {@code textShadow} 默认 true）。
         * v14→v15 迁移时<b>不继承</b>旧的 {@code textShadow}：为了关头顶阴影而关掉它的人，
         * 面板不该被一起带走。</p>
         */
        public boolean selfStatsTextShadow = true;
        /**
         * 默认（全部职业共用）时要隐藏的内容类，取值见 {@link SelfStatsKind#id}。
         *
         * <p>例如 {@code ["heart_red", "hp_neg"]} = 红心与负生命那几行不上面板。
         * 1.20.5 起过滤粒度是<b>单个属性</b>；旧版本写的粗类（{@code health}/{@code attack}…）
         * 会在读取时自动展开成对应的一串细类。</p>
         */
        public java.util.List<String> selfStatsHidden = new java.util.ArrayList<>();

        /** 盾牌指示器（1.20.19 合并自 CTT Shield Indicator）。 */
        public boolean shieldEnabled = true;
        /**
         * 屏幕 GUI 坐标（左上角）；{@code -1} = 未设置 → 默认位置：
         * 水平对准副手物品格中心（右手玩家 = 快捷栏左侧 {@code guiW/2 - 109}），底边距屏幕底 76 px。
         */
        public int shieldX = -1;
        /** 同 {@link #shieldX}；两个都为 -1 时才走「跟随副手槽」的默认位置。 */
        public int shieldY = -1;
        /** 缩放倍率（1.0 = 纹理原尺寸 34×86；1.20.22 起允许 0.5~1.5 的小数倍；1.20.26 起默认 0.7）。 */
        public double shieldScale = 0.7;
        /** 不透明度 0.1~1.0（1.20.26 起默认 0.75；1.20.32 起默认 0.65；界面文案「不透明度」）。 */
        public double shieldOpacity = 0.65;
        /**
         * 默认位置是否**跟着主手镜像**（1.2.0 新增，默认开）。
         *
         * <p>默认水平位置是「对准副手物品格中心」，而副手在哪一侧取决于主手：
         * 右手玩家副手在快捷栏**左**侧、左手玩家在**右**侧。开启时切换主手会自动镜像；
         * 关闭后固定按右手习惯（副手在左）摆放，切换主手也不动。</p>
         *
         * <p>只影响**默认位置**（{@code shieldX/Y = -1}）；拖动过的自定义位置不受影响。</p>
         */
        public boolean shieldAutoMirror = true;

        /**
         * 1.20.40：这些「像素坐标」是量在多大的 GUI 上的（{@code 0} = 还不知道）。
         *
         * <p>面板 / 盾牌的位置以前是**绝对 GUI 像素**，窗口或 GUI 缩放一变，同一个像素数就落到
         * 屏幕的另一处相对位置（「窗口一变就错位」）。现在把坐标连同「量它的那套 GUI 尺寸」一起存，
         * 绘制时按 {@code 当前尺寸 ÷ 记录尺寸} 换算，等于把位置变成了<b>相对位置</b>：
         * 窗口放大一倍，面板也到两倍远处；靠边的则会因为夹进而贴边，不会跑到屏幕外。</p>
         *
         * <p>记录尺寸只在**拖动**时更新（那时坐标就是在当前尺寸下量的）；
         * 平时窗口怎么变都不改它，这样换算才有基准。</p>
         */
        public int layoutGuiW = 0;
        /** 同 {@link #layoutGuiW}；两个必须同时有效（任一为 0 就当未知，绘制时重新记录）。 */
        public int layoutGuiH = 0;
    }

    /**
     * 属性面板的布局快照（绘制用）。位置 / 列数 / 缩放 / 不透明度对所有职业统一 ——
     * 这属于「屏幕布局」，跟职业无关；按职业分开的只有「看什么」，见
     * {@link Cfg#selfStatsHidden}。
     */
    public static final class Panel {
        /** 当前 GUI 尺寸下的左上角 x（已按相对位置换算，直接画即可）。 */
        public final int x;
        /** 当前 GUI 尺寸下的左上角 y（{@code -1} 已解析成「屏幕高 ÷ 3」）。 */
        public final int y;
        /** 列数（1~3，列优先排版）。 */
        public final int columns;
        /** 整块文字的不透明度（0.20~1.00）。 */
        public final double opacity;

        /**
         * @param x       当前 GUI 下的左上角 x（已换算）
         * @param y       当前 GUI 下的左上角 y（已换算，不会为 -1）
         * @param columns 列数 1~3
         * @param opacity 不透明度 0.20~1.00
         */
        public Panel(int x, int y, int columns, double opacity) {
            this.x = x;
            this.y = y;
            this.columns = columns;
            this.opacity = opacity;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("ctt-hud/config");
    /** 配置文件（1.20.6 起改名为 {@code ctt-hud-fork.json}）。 */
    public static final String FILE_NAME = "ctt-hud-fork.json";
    /** 旧文件名：只在迁移时读一次。 */
    public static final String LEGACY_FILE_NAME = "ctt-enemy-hud.json";
    private static final long RECHECK_MS = 2000L;

    /** 当前配置版本。老文件没有 {@code configVersion} 键 → 按 v1 处理并迁移。 */
    public static final int CURRENT_VERSION = 23;

    /** 1.20.41：面板「默认位置」的哨兵值（{@code -1} = 用动态默认，见 {@link #defaultPanelY}）。 */
    public static final int DEFAULT_Y_SENTINEL = -1;
    /** 面板默认位置的左边距（GUI 像素）。 */
    public static final int DEFAULT_X = 4;
    /** 面板默认位置：{@code y = 屏幕高 ÷ 3}（顶部往下三分之一，左侧偏上）。 */
    public static final int DEFAULT_Y_DENOM = 3;

    /**
     * 面板默认 y（GUI 像素）：屏幕高 ÷ {@value #DEFAULT_Y_DENOM}。
     *
     * @param guiH 屏幕 GUI 高；{@code <=0}（尺寸未知，测试/设置页只取其它字段时）返回 0
     */
    public static int defaultPanelY(int guiH) {
        if (guiH <= 0) return 0;
        return Math.max(0, Math.round((float) guiH / DEFAULT_Y_DENOM));
    }

    /** 盾牌指示器的缩放范围（倍）。1.20.22 起是小数倍，纹理整张等比缩放。 */
    public static final double SHIELD_SCALE_MIN = 0.5;
    public static final double SHIELD_SCALE_MAX = 1.5;
    /** 旧版默认血条长度（1.18.0 起默认改成 75）。 */
    private static final double LEGACY_BAR_WIDTH = 100.0;
    /** 旧版默认缩放（1.19.0 起默认改成 0.9）。 */
    private static final double LEGACY_SCALE = 1.0;
    /** 1.19.0 起的默认缩放。 */
    private static final double DEFAULT_SCALE = 0.9;

    private static Cfg cfg = new Cfg();
    private static Path file;
    private static long lastModified = -1L;
    private static long lastCheckMs = 0L;
    private static boolean writing = false;

    private HudConfig() {}

    /** 当前配置（内部按 2 秒节流检查文件是否被改过）。 */
    public static Cfg get() {
        long now = System.currentTimeMillis();
        if (now - lastCheckMs > RECHECK_MS) {
            lastCheckMs = now;
            reloadIfChanged();
        }
        return cfg;
    }

    /** 启动时调用：定位文件、必要时写出默认值、读入。 */
    public static void load() {
        try {
            file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
            // 1.20.6 起改名：老文件名 → 新文件名（复制一份，旧文件保留，玩家设置不丢）
            Path legacy = FabricLoader.getInstance().getConfigDir().resolve(LEGACY_FILE_NAME);
            if (!Files.exists(file) && Files.exists(legacy)) {
                Files.createDirectories(file.getParent());
                Files.copy(legacy, file);
                LOGGER.info("配置改名：已把 {} 复制为 {}（旧文件保留）", LEGACY_FILE_NAME, FILE_NAME);
            }
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(new Cfg()), StandardCharsets.UTF_8);
            }
            read();
            lastModified = Files.getLastModifiedTime(file).toMillis();
        } catch (Exception e) {
            cfg = new Cfg();
        }
    }

    /** Mod Menu 关闭界面时调用：归一化后写回磁盘。 */
    public static void save() {
        if (file == null) return;
        try {
            normalize(cfg);
            writing = true;
            Files.writeString(file, GSON.toJson(cfg), StandardCharsets.UTF_8);
            lastModified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException ignored) {
        } finally {
            writing = false;
        }
    }

    /** 恢复默认（不落盘，调用方负责 save）。 */
    public static void reset() {
        cfg = new Cfg();
    }

    /** 当前显示方式 + 上下文对应的布局；{@code container=true} 取容器界面那一套。 */
    public static Panel panel(int mode, boolean container) {
        return panel(mode, container, 0, 0);
    }

    /**
     * 当前显示方式 + 上下文对应的布局，位置按当前 GUI 尺寸换算（1.20.40）。
     *
     * @param guiW 当前 GUI 宽；{@code <=0} = 不换算（只取列数 / 不透明度这类与位置无关的字段时可用）
     * @param guiH 当前 GUI 高
     */
    public static Panel panel(int mode, boolean container, int guiW, int guiH) {
        Cfg c = withLayoutRef(get(), guiW, guiH);
        int x = resolveX(rawX(c, mode, container), c.layoutGuiW, guiW);
        int y = resolveY(rawY(c, mode, container), c.layoutGuiH, guiH);
        // 1.4.1：列数 / 不透明度是**全局**设置（selfStatsColumns / selfStatsOpacity），
        // 只有位置（x/y）按显示方式区分。
        return new Panel(x, y, c.selfStatsColumns, c.selfStatsOpacity);
    }

    /** 某一套配置里存的（记录尺寸下的）左上角 x。 */
    private static int rawX(Cfg c, int mode, boolean container) {
        return switch (mode) {
            case 1 -> c.containerX;
            case 2 -> container ? c.bothContainerX : c.bothX;
            default -> c.selfStatsX;
        };
    }

    /** 某一套配置里存的（记录尺寸下的）左上角 y。 */
    private static int rawY(Cfg c, int mode, boolean container) {
        return switch (mode) {
            case 1 -> c.containerY;
            case 2 -> container ? c.bothContainerY : c.bothY;
            default -> c.selfStatsY;
        };
    }

    /**
     * 写位置（拖动界面用；坐标是当前 GUI 下的像素，1.20.40）。
     *
     * <p>顺手把「记录尺寸」更新到当前这一套 —— 之后窗口/缩放变化时按比例换算，
     * 面板就留在同一个相对位置上。</p>
     */
    public static void movePanel(int mode, boolean container, int x, int y, int guiW, int guiH) {
        Cfg c = get();
        rebase(c, guiW, guiH);
        switch (mode) {
            case 1 -> {
                c.containerX = x;
                c.containerY = y;
            }
            case 2 -> {
                if (container) {
                    c.bothContainerX = x;
                    c.bothContainerY = y;
                } else {
                    c.bothX = x;
                    c.bothY = y;
                }
            }
            default -> {
                c.selfStatsX = x;
                c.selfStatsY = y;
            }
        }
    }

    // ── 1.20.40：位置按「记录尺寸 → 当前尺寸」换算，变成相对位置 ──

    /**
     * 纯函数：把「{@code storedGui} 这套尺寸下量的坐标」换算到 {@code guiNow}（selfCheck 直接打）。
     *
     * <p>任何一边未知（{@code <=0}）就原样返回 —— 老配置第一次进游戏前走的就是这条路，
     * 不会因为缺基准把位置算飞。这里只按比例换算、不夹取；夹进屏幕由绘制方负责。</p>
     */
    static int scalePos(int pos, int storedGui, int guiNow) {
        if (storedGui <= 0 || guiNow <= 0 || storedGui == guiNow) return pos;
        return (int) Math.round((double) pos * (double) guiNow / (double) storedGui);
    }

    /** 老配置 / 首次运行：把当前 GUI 尺寸记成「坐标是在这上面量的」。 */
    private static Cfg withLayoutRef(Cfg c, int guiW, int guiH) {
        if (c != null && guiW > 0 && guiH > 0 && (c.layoutGuiW <= 0 || c.layoutGuiH <= 0)) {
            c.layoutGuiW = guiW;
            c.layoutGuiH = guiH;
        }
        return c;
    }

    /** 存的 x → 当前 GUI 下的 x；{@code <0} = 默认左边距。 */
    static int resolveX(int raw, int storedGui, int guiNow) {
        if (raw < 0) return DEFAULT_X;
        return scalePos(raw, storedGui, guiNow);
    }

    /** 存的 y → 当前 GUI 下的 y；{@code <0} = 动态默认（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}）。 */
    static int resolveY(int raw, int storedGui, int guiNow) {
        if (raw < 0) return defaultPanelY(guiNow);
        return scalePos(raw, storedGui, guiNow);
    }

    /**
     * 把「记录尺寸」改到 {@code guiW × guiH}，并先把已存的坐标换算过去（1.20.40）。
     *
     * <p>拖动时调用：新坐标是在当前 GUI 下量的，之前存的那几套（其它显示方式的、盾牌的）
     * 必须先按旧比例换算到新尺寸下，否则会被当成「也是在这个尺寸下量的」而整体偏掉。</p>
     */
    static void rebase(Cfg c, int guiW, int guiH) {
        if (c == null || guiW <= 0 || guiH <= 0) return;
        int oldW = c.layoutGuiW;
        int oldH = c.layoutGuiH;
        if (oldW > 0 && oldH > 0 && (oldW != guiW || oldH != guiH)) {
            // 哨兵值（-1 = 动态默认）不参与换算，否则会变成 -2 之类。
            c.selfStatsX = scalePos(c.selfStatsX, oldW, guiW);
            if (c.selfStatsY >= 0) c.selfStatsY = scalePos(c.selfStatsY, oldH, guiH);
            c.containerX = scalePos(c.containerX, oldW, guiW);
            if (c.containerY >= 0) c.containerY = scalePos(c.containerY, oldH, guiH);
            c.bothX = scalePos(c.bothX, oldW, guiW);
            if (c.bothY >= 0) c.bothY = scalePos(c.bothY, oldH, guiH);
            c.bothContainerX = scalePos(c.bothContainerX, oldW, guiW);
            if (c.bothContainerY >= 0) c.bothContainerY = scalePos(c.bothContainerY, oldH, guiH);
            if (c.shieldX >= 0) c.shieldX = scalePos(c.shieldX, oldW, guiW);
            if (c.shieldY >= 0) c.shieldY = scalePos(c.shieldY, oldH, guiH);
        }
        c.layoutGuiW = guiW;
        c.layoutGuiH = guiH;
    }

    /** 盾牌指示器位置（当前 GUI 尺寸下；{@code {-1,-1}} = 跟随副手槽的自动位置）。 */
    public static int[] shieldPosition(int guiW, int guiH) {
        Cfg c = withLayoutRef(get(), guiW, guiH);
        if (c.shieldX < 0 || c.shieldY < 0) return new int[]{-1, -1};
        return new int[]{scalePos(c.shieldX, c.layoutGuiW, guiW),
                scalePos(c.shieldY, c.layoutGuiH, guiH)};
    }

    /** 写盾牌位置（当前 GUI 下的像素；{@code -1} = 恢复自动位置）。 */
    public static void setShieldPosition(int x, int y, int guiW, int guiH) {
        Cfg c = get();
        if (x >= 0 && y >= 0) rebase(c, guiW, guiH);
        c.shieldX = x;
        c.shieldY = y;
    }

    /**
     * 「回到初始位置」：写回**动态默认** —— {@code x=4}、{@code y = 屏幕高 ÷ 3}（左侧偏上，1.20.41）。
     *
     * <p>存哨兵而不是当场算一个像素值，这样以后窗口怎么变，默认位置都保持在「偏上三分之一」。</p>
     */
    public static void resetPanelPosition(int mode, boolean container) {
        Cfg c = get();
        switch (mode) {
            case 1 -> {
                c.containerX = DEFAULT_X;
                c.containerY = DEFAULT_Y_SENTINEL;
            }
            case 2 -> {
                if (container) {
                    c.bothContainerX = DEFAULT_X;
                    c.bothContainerY = DEFAULT_Y_SENTINEL;
                } else {
                    c.bothX = DEFAULT_X;
                    c.bothY = DEFAULT_Y_SENTINEL;
                }
            }
            default -> {
                c.selfStatsX = DEFAULT_X;
                c.selfStatsY = DEFAULT_Y_SENTINEL;
            }
        }
    }

    /** 写当前显示方式 + 上下文那一套的列数（1~3）。 */
    public static void setPanelColumns(int mode, boolean container, int columns) {
        Cfg c = get();
        switch (mode) {
            case 1 -> c.containerColumns = columns;
            case 2 -> {
                if (container) c.bothContainerColumns = columns;
                else c.bothColumns = columns;
            }
            default -> c.selfStatsColumns = columns;
        }
    }

    /** 写当前显示方式 + 上下文那一套的不透明度（0.20~1.00）。 */
    public static void setPanelOpacity(int mode, boolean container, double opacity) {
        Cfg c = get();
        switch (mode) {
            case 1 -> c.containerOpacity = opacity;
            case 2 -> {
                if (container) c.bothContainerOpacity = opacity;
                else c.bothOpacity = opacity;
            }
            default -> c.selfStatsOpacity = opacity;
        }
    }

    /** 当前模式是否要在容器界面上画（模式 1、2）。 */
    public static boolean containerMode() {
        return get().selfStatsMode != 0;
    }

    /** 当前模式是否要在 HUD 上画（模式 0、2）。 */
    public static boolean hudMode() {
        return get().selfStatsMode != 1;
    }

    /** 当前生效的「隐藏类」集合。 */
    public static java.util.Set<String> hiddenKinds() {
        Cfg c = get();
        return new java.util.LinkedHashSet<>(c.selfStatsHidden);
    }

    /** 过滤界面正在编辑的那一份列表（可直接增删）。 */
    public static java.util.List<String> hiddenList() {
        return get().selfStatsHidden;
    }

    private static void reloadIfChanged() {
        if (file == null || writing) return;
        try {
            long m = Files.getLastModifiedTime(file).toMillis();
            if (m != lastModified) {
                lastModified = m;
                read();
            }
        } catch (IOException ignored) {
        }
    }

    private static void read() {
        Cfg previous = cfg;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Cfg read = GSON.fromJson(json, Cfg.class);
            cfg = normalize(read);
            migrate(json, cfg);
        } catch (Exception e) {
            // 手改坏了（类型不对 / JSON 写错）时**保留上一份可用配置**，不要静默恢复默认值
            // ——否则玩家以为自己只改错了一行，实际整套设置被清空。
            cfg = previous != null ? previous : new Cfg();
            LOGGER.warn("配置文件解析失败，继续使用上一份可用配置: {}", e.toString());
        }
    }

    /**
     * 老配置文件升级到 {@link #CURRENT_VERSION}（1.3.6 起表驱动）。
     *
     * <h2>为什么改成表</h2>
     * <p>以前每条规则都是一个 {@code if (version < N) { if (字段 == 旧默认) 字段 = 新默认; }}，
     * 而且**版本号并不是按顺序写的**（13 在 14 后面、15 在 17 后面），改默认值时很容易插错位置。
     * 现在所有「仍停在旧默认值才改」的规则集中到 {@link #RULES}，按版本升序排列，
     * 逐条判断；跨字段复制（v7/v8/v9）与一次性哨兵（v10）仍写成代码。</p>
     *
     * <h2>判定惯例</h2>
     * <p>只在**仍等于旧默认值**时才跟着改；自己调过的（比如 120、1.25）保留原值 ——
     * 既不覆盖玩家的选择，也不会让老配置看起来「新默认值没生效」。</p>
     */
    static void migrate(String json, Cfg cfg) {
        if (cfg == null) return;
        boolean hasVersion = json != null && json.contains("\"configVersion\"");
        int version = hasVersion ? cfg.configVersion : 1;
        if (version >= CURRENT_VERSION) return;

        // ── ① 跨字段复制：新的一套先照抄旧的一套，升级后各自独立但初值一致 ──
        //    （放在值规则之前：后面的 v20/v21 会把「仍等于旧默认 40/20」的 Y 改成哨兵，
        //     复制过来的那些 Y 也要经历同样的判定，与旧代码顺序一致。）
        if (version < 7) {
            cfg.containerX = cfg.selfStatsX;
            cfg.containerY = cfg.selfStatsY;
            cfg.containerColumns = cfg.selfStatsColumns;
            cfg.containerOpacity = cfg.selfStatsOpacity;
        }
        if (version < 8) {
            cfg.bothX = cfg.selfStatsX;
            cfg.bothY = cfg.selfStatsY;
            cfg.bothColumns = cfg.selfStatsColumns;
            cfg.bothOpacity = cfg.selfStatsOpacity;
        }
        if (version < 9) {
            cfg.bothContainerX = cfg.bothX;
            cfg.bothContainerY = cfg.bothY;
            cfg.bothContainerColumns = cfg.bothColumns;
            cfg.bothContainerOpacity = cfg.bothOpacity;
        }

        // ── ② 「旧默认值 → 新默认值」：按版本升序逐条套用 ──
        for (Rule rule : RULES) {
            if (version < rule.version() && rule.get().applyAsDouble(cfg) == rule.from()) {
                rule.set().accept(cfg, rule.to());
            }
        }
        for (FlagRule rule : FLAG_RULES) {
            if (version < rule.version() && rule.get().test(cfg) == rule.from()) {
                rule.set().accept(cfg, rule.to());
            }
        }

        // ── ③ 一次性哨兵 / 夹取 ──
        if (version < 10) {
            // 盾牌指示器改成可拖动坐标：旧的位置/偏移字段不再使用，默认交给自动位置。
            cfg.shieldX = -1;
            cfg.shieldY = -1;
        }
        if (version < 11) {
            // 1.20.22：盾牌缩放从「整数倍 1~4」改成「0.5~1.5 小数倍」。
            // 越界值在 normalize() 里统一夹取，这里只保证版本号被写上。
            cfg.shieldScale = Math.max(SHIELD_SCALE_MIN, Math.min(SHIELD_SCALE_MAX, cfg.shieldScale));
        }

        // ④ 只写版本号、不改字段的版本（保留注释备查）：
        //    v12 删 hideBehindEntities、v14 过滤列表分节、v17 删 teamShowBar、
        //    v18 删 useServerData、v19 位置改相对坐标（字段由 normalize / 首次绘制补）。

        cfg.configVersion = CURRENT_VERSION;
    }

    /**
     * 一条「值仍等于旧默认值时才改」的数值迁移规则。
     *
     * @param version 这条规则从哪个配置版本开始生效（{@code version < version} 才跑）
     * @param get     读字段
     * @param set     写字段
     * @param from    旧默认值
     * @param to      新默认值
     */
    private record Rule(int version, java.util.function.ToDoubleFunction<Cfg> get,
                        java.util.function.ObjDoubleConsumer<Cfg> set, double from, double to) {}

    /** 布尔迁移规则（目前只有 textShadow 一条）。 */
    private record FlagRule(int version, java.util.function.Predicate<Cfg> get,
                            java.util.function.BiConsumer<Cfg, Boolean> set, boolean from, boolean to) {}

    /** 所有「旧默认值 → 新默认值」规则，**必须按版本升序**（同一版本内按原代码顺序）。 */
    private static final java.util.List<Rule> RULES = java.util.List.of(
            // v2：血条长度默认 100 → 75（1.18.0）
            new Rule(2, c -> c.barWidth, (c, v) -> c.barWidth = v,
                    LEGACY_BAR_WIDTH, HudLayout.BAR_WIDTH_DEFAULT),
            new Rule(2, c -> c.teamBarWidth, (c, v) -> c.teamBarWidth = v,
                    LEGACY_BAR_WIDTH, HudLayout.BAR_WIDTH_DEFAULT),
            // v3：缩放默认 1.0 → 0.9
            new Rule(3, c -> c.scale, (c, v) -> c.scale = v, LEGACY_SCALE, DEFAULT_SCALE),
            new Rule(3, c -> c.teamScale, (c, v) -> c.teamScale = v, LEGACY_SCALE, DEFAULT_SCALE),
            // v13：盾牌新纹理，默认缩放 0.7、默认不透明度 75%
            new Rule(13, c -> c.shieldScale, (c, v) -> c.shieldScale = v, 1.0, 0.7),
            new Rule(13, c -> c.shieldOpacity, (c, v) -> c.shieldOpacity = v, 1.0, 0.75),
            // v15：盾牌默认不透明度 75% → 65%（在 v13 之后：1.0 先落到 0.75，再落到 0.65）
            new Rule(15, c -> c.shieldOpacity, (c, v) -> c.shieldOpacity = v, 0.75, 0.65),
            // v20：面板默认 y=40 → 「屏幕高 ÷ 3」哨兵
            new Rule(20, c -> c.selfStatsY, (c, v) -> c.selfStatsY = (int) v, 40, DEFAULT_Y_SENTINEL),
            new Rule(20, c -> c.bothY, (c, v) -> c.bothY = (int) v, 40, DEFAULT_Y_SENTINEL),
            // v21：容器两套默认 y=20 → 同一哨兵
            new Rule(21, c -> c.containerY, (c, v) -> c.containerY = (int) v, 20, DEFAULT_Y_SENTINEL),
            new Rule(21, c -> c.bothContainerY, (c, v) -> c.bothContainerY = (int) v, 20, DEFAULT_Y_SENTINEL),
            // v22：面板显示方式默认 0（仅 HUD）→ 2（HUD + 容器）
            new Rule(22, c -> c.selfStatsMode, (c, v) -> c.selfStatsMode = (int) v, 0, 2)
            // 1.4.6：原来的单一 opacity 拆成 barOpacity / textOpacity，不再迁移旧值
            // （新字段默认 条 0.65 / 文字 1.00，旧文件里的 "opacity" 键读到也不认）。
    );

    /** 布尔规则：v15 实体血条「文字阴影」新默认关。 */
    private static final java.util.List<FlagRule> FLAG_RULES = java.util.List.of(
            new FlagRule(15, c -> c.textShadow, (c, v) -> c.textShadow = v, true, false)
    );

    /** 重置「实体血条」这一页的设置（1.4.1 页内重置按钮）。 */
    static void resetEntity() {
        Cfg d = new Cfg();
        Cfg c = get();
        c.showBar = d.showBar;
        c.hideEnemyBossBar = d.hideEnemyBossBar;
        c.hideBelowNameScore = d.hideBelowNameScore;
        c.barWidth = d.barWidth;
        c.scale = d.scale;
        c.barOpacity = d.barOpacity;
        c.textOpacity = d.textOpacity;
        c.textShadow = d.textShadow;
        c.maxDistance = d.maxDistance;
        c.showTeammates = d.showTeammates;
        c.showSelf = d.showSelf;
        c.teamBarWidth = d.teamBarWidth;
        c.teamScale = d.teamScale;
        c.teamMaxDistance = d.teamMaxDistance;
        c.hideTeamBossBar = d.hideTeamBossBar;
    }

    /** 重置「属性面板」这一页的设置。 */
    static void resetPanel() {
        Cfg d = new Cfg();
        Cfg c = get();
        c.showSelfStats = d.showSelfStats;
        c.selfStatsSeconds = d.selfStatsSeconds;
        c.selfStatsColumns = d.selfStatsColumns;
        c.selfStatsOpacity = d.selfStatsOpacity;
        c.selfStatsTextShadow = d.selfStatsTextShadow;
        c.selfStatsMode = d.selfStatsMode;
        c.selfStatsX = d.selfStatsX;
        c.selfStatsY = d.selfStatsY;
        c.containerX = d.containerX;
        c.containerY = d.containerY;
        c.bothX = d.bothX;
        c.bothY = d.bothY;
        c.bothContainerX = d.bothContainerX;
        c.bothContainerY = d.bothContainerY;
    }

    /** 重置「调试功能」这一页的设置（1.4.5）。 */
    static void resetDebug() {
        Cfg d = new Cfg();
        Cfg c = get();
        c.debug = d.debug;
        c.preferServerData = d.preferServerData;
        c.payloadLimit = d.payloadLimit;
    }

    /** 重置「盾牌指示器」这一页的设置。 */
    static void resetShield() {
        Cfg d = new Cfg();
        Cfg c = get();
        c.shieldEnabled = d.shieldEnabled;
        c.shieldX = d.shieldX;
        c.shieldY = d.shieldY;
        c.shieldScale = d.shieldScale;
        c.shieldOpacity = d.shieldOpacity;
        c.shieldAutoMirror = d.shieldAutoMirror;
    }

    /**
     * 把任意（含非法 / 缺省 / NaN）配置夹到安全范围。
     *
     * <p>包可见而不是 private：{@code selfCheck} 会直接拿攻击性输入打这个函数。</p>
     */
    static Cfg normalize(Cfg read) {
        if (read == null) read = new Cfg();
        // 1.4.6：血条与文字的不透明度分开（各自 0.10 ~ 1.00）
        read.barOpacity = clamp(read.barOpacity, 0.1, 1.0);
        read.textOpacity = clamp(read.textOpacity, 0.1, 1.0);
        read.scale = clamp(read.scale, 0.75, 1.5);
        read.maxDistance = clamp(read.maxDistance, 4.0, 256.0);
        // 血条长度：非法值回落默认 75，其余夹在 50~150。
        if (Double.isNaN(read.barWidth)) read.barWidth = HudLayout.BAR_WIDTH_DEFAULT;
        read.barWidth = clamp(read.barWidth, HudLayout.BAR_WIDTH_MIN, HudLayout.BAR_WIDTH_MAX);
        if (Double.isNaN(read.teamBarWidth)) read.teamBarWidth = HudLayout.BAR_WIDTH_DEFAULT;
        read.teamBarWidth = clamp(read.teamBarWidth, HudLayout.BAR_WIDTH_MIN, HudLayout.BAR_WIDTH_MAX);
        read.teamScale = clamp(read.teamScale, 0.75, 1.5);
        read.teamMaxDistance = clamp(read.teamMaxDistance, 4.0, 256.0);
        // 自身属性面板（1.20.3）：间隔 0~600 s，列 1~3，坐标夹进屏幕范围
        read.selfStatsSeconds = (int) clamp(read.selfStatsSeconds, 1.0, 5.0);
        read.selfStatsColumns = (int) clamp(read.selfStatsColumns, 1.0, 3.0);
        read.selfStatsX = (int) clamp(read.selfStatsX, 0.0, 4000.0);
        // 1.20.41：y = -1 是「动态默认位置」的哨兵，允许保留。
        read.selfStatsY = (int) clamp(read.selfStatsY, -1.0, 4000.0);
        // 1.20.4：不透明度、内容过滤（「文字大小」在 1.0.0 删掉，面板固定 1.0 倍）
        read.selfStatsOpacity = clamp(read.selfStatsOpacity, 0.2, 1.0);
        // 1.20.14：显示方式（0=始终显示 HUD，1=在容器界面显示）+ 容器那一套配置
        read.selfStatsMode = (int) clamp(read.selfStatsMode, 0.0, 2.0);
        read.containerColumns = (int) clamp(read.containerColumns, 1.0, 3.0);
        read.containerX = (int) clamp(read.containerX, 0.0, 4000.0);
        read.containerY = (int) clamp(read.containerY, -1.0, 4000.0);
        read.containerOpacity = clamp(read.containerOpacity, 0.2, 1.0);
        read.bothColumns = (int) clamp(read.bothColumns, 1.0, 3.0);
        read.bothX = (int) clamp(read.bothX, 0.0, 4000.0);
        read.bothY = (int) clamp(read.bothY, -1.0, 4000.0);
        read.bothOpacity = clamp(read.bothOpacity, 0.2, 1.0);
        read.bothContainerColumns = (int) clamp(read.bothContainerColumns, 1.0, 3.0);
        read.bothContainerX = (int) clamp(read.bothContainerX, 0.0, 4000.0);
        read.bothContainerY = (int) clamp(read.bothContainerY, -1.0, 4000.0);
        read.bothContainerOpacity = clamp(read.bothContainerOpacity, 0.2, 1.0);
        read.selfStatsHidden = sanitizeKinds(read.selfStatsHidden);
        // 盾牌指示器（1.20.20：可拖动定位 + 不透明度；1.20.22：缩放 0.5~1.5 小数倍）
        read.shieldX = (int) clamp(read.shieldX, -1.0, 4000.0);
        read.shieldY = (int) clamp(read.shieldY, -1.0, 4000.0);
        // 1.20.40：位置换算的基准尺寸；0 = 还不知道（第一次绘制时补），非法值（负数/超大）同样按未知处理。
        read.layoutGuiW = (int) clamp(read.layoutGuiW, 0.0, 10000.0);
        read.layoutGuiH = (int) clamp(read.layoutGuiH, 0.0, 10000.0);
        if (read.layoutGuiW == 0 || read.layoutGuiH == 0) {
            read.layoutGuiW = 0;
            read.layoutGuiH = 0;
        }
        read.shieldScale = clamp(read.shieldScale, SHIELD_SCALE_MIN, SHIELD_SCALE_MAX);
        read.shieldOpacity = clamp(read.shieldOpacity, 0.1, 1.0);
        // 1.4.5 调试功能：筛选实体数量 1 ~ 单包上限
        read.payloadLimit = (int) clamp(read.payloadLimit, 1.0, MobHealthPayload.MAX_ENTRIES);
        return read;
    }

    /**
     * 1.20.4 的粗类 → 1.20.5 的细类。旧配置里的粗类在读取时展开，不需要手改。
     */
    private static final java.util.Map<String, String[]> COARSE_KINDS = buildCoarseKinds();

    private static java.util.Map<String, String[]> buildCoarseKinds() {
        java.util.Map<String, String[]> m = new java.util.HashMap<>();
        m.put("health", new String[]{"hp_amp", "hp_raw", "hp_neg", "heal_amp", "heal_raw",
                "heal_percent", "heal_extra"});
        m.put("attack", new String[]{"class_stat", "strength", "attack_speed", "attack_range",
                "stamina", "archery", "class_stat_amp", "strength_amp", "attack_speed_amp",
                "class_stat_raw", "strength_raw", "attack_speed_raw"});
        m.put("defence", new String[]{"defence", "true_armor", "defence_amp", "true_armor_amp",
                "defence_raw", "true_armor_raw"});
        m.put("element", new String[]{"fire_armor", "true_fire_armor", "water_armor",
                "electric_armor", "ice_armor", "dark_armor", "light_armor", "fire_armor_amp",
                "fire_armor_raw"});
        m.put("speed", new String[]{"max_speed", "speed_amp", "speed_raw", "jump", "double_jump"});
        m.put("mana", new String[]{"mana_power", "mana_recharge", "mana_amp", "mana_power_raw"});
        m.put("time", new String[]{"timer"});
        m.put("misc", new String[]{"active_charge", "max_charge", "active_speed", "summoning",
                "size", "gravity", "towers_regen", "skulls", "bitches", "fishing", "bee", "ant",
                "karma_light", "karma_neutral", "karma_dark", "karma_celestial",
                "grass", "misc"});
        // 1.20.31：状态效果从一条粗类拆成 28 条，旧配置里的 "status" 展开成全部 28 条。
        m.put("status", statusIds());
        return java.util.Map.copyOf(m);
    }

    /** 28 条状态效果的 id（{@code Misc_View_Stats_49..76}，顺序同地图）。 */
    private static String[] statusIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 49; i <= 76; i++) {
            SelfStatsKind kind = SelfStatsKind.status(i);
            if (kind != null) ids.add(kind.id);
        }
        return ids.toArray(new String[0]);
    }

    /** 只留下真正可过滤的类 id，去掉 null / 未知 / 重复；粗类先展开成细类。 */
    static java.util.List<String> sanitizeKinds(java.util.List<String> raw) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (raw == null) return out;
        for (String id : raw) {
            if (id == null) continue;
            String[] expanded = COARSE_KINDS.get(id);
            if (expanded != null) {
                for (String fine : expanded) {
                    if (!out.contains(fine)) out.add(fine);
                }
                continue;
            }
            SelfStatsKind kind = SelfStatsKind.byId(id);
            if (kind == null || !kind.filterable) continue;
            if (!out.contains(kind.id)) out.add(kind.id);
        }
        return out;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return hi;
        return Math.max(lo, Math.min(hi, v));
    }
}
