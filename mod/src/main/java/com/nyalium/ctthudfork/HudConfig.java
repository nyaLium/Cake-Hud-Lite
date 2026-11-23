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
 *   <li>头顶条通用 / 敌人 / 队友（`opacity`、`barWidth`、`scale`、`team*`、`show*`…）；</li>
 *   <li>自身属性面板的 4 套参数：模式 0 用 `selfStats*`、模式 1 用 `container*`、
 *       模式 2 分 `both*`（HUD）/ `bothContainer*`（容器界面）；</li>
 *   <li>盾牌指示器：`shield*`（`shieldX/Y = -1` = 跟随副手槽）；</li>
 *   <li>2 个写死的开关（见各自注释）：`autoPartyBossbar` / `hideBehindWalls`。
 *       「优先使用服务端数据」1.20.35 起也写死开启，字段已删除。</li>
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
        /** 整体不透明度，0.50 ~ 1.00（低于 0.50 会被夹到 0.50）。 */
        public double opacity = 0.75;
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
        /** 自动 /trigger TogglePartyBossbar 打开「队伍血量」条（队友血量的数据源，纯客户端）。 */
        public boolean autoPartyBossbar = true;
        /** 隐藏上方 Boss 栏里「队伍血量」那一条（数据照常读取）。 */
        public boolean hideTeamBossBar = true;
        /** 玩家血条长度（原版名牌像素），默认 75。 */
        public double teamBarWidth = 75.0;
        /** 玩家血条整体缩放。 */
        public double teamScale = 0.9;
        /** 玩家血条垂直微调。 */
        public int teamVerticalOffset = 0;
        /** 玩家血条显示距离（格）。 */
        public double teamMaxDistance = 48.0;
        /** 超过这个距离（格）不再绘制。 */
        public double maxDistance = 48.0;
        /** 血条长度（原版名牌像素），50 ~ 150，默认 75。 */
        public double barWidth = 75.0;
        /** 整组 UI 的额外缩放，0.75 ~ 1.50，0.9 = 默认（1.19.0 起）。 */
        public double scale = 0.9;
        /** 整组 UI 的垂直微调（原版名牌像素），-30 ~ 0，负数=更高。 */
        public int verticalOffset = 0;
        /** 被方块挡住视线时不绘制（防止隔墙看到血条）。1.20.14 起写死关闭、界面不再提供开关（字段保留以兼容旧配置）。 */
        public boolean hideBehindWalls = false;
        /** 左上角打印面板调试行（排查用，见 {@code SelfStatsPanel} 里 {@code debug} 分支）。 */
        public boolean debug = false;
        /**
         * 自身属性面板（1.20.3）。纯客户端：定时 {@code /trigger ViewStats}，
         * 抓地图回的那些属性行画成面板，不依赖服务端组件。
         */
        public boolean showSelfStats = true;
        /** 属性面板自动刷新间隔（秒），1 = 立即（每 tick 刷），1~5；进他人服务器时强制 3 s（不改这里）。 */
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
         * 每种上下文（HUD / 容器）各有一套位置·列数·字号·不透明度。
         */
        public int selfStatsMode = 0;
        /** 容器界面那一套配置（模式 1 专用）。 */
        public int containerX = 4;
        /** 同 {@link #selfStatsY}：{@code -1} = 动态默认位置（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}；1.20.42）。 */
        public int containerY = DEFAULT_Y_SENTINEL;
        public int containerColumns = 2;
        public double containerScale = 1.0;
        public double containerOpacity = 1.0;
        /** 模式 2 在 HUD 上的那一套配置。 */
        public int bothX = 4;
        /** 同 {@link #selfStatsY}：{@code -1} = 动态默认位置（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}）。 */
        public int bothY = DEFAULT_Y_SENTINEL;
        public int bothColumns = 2;
        public double bothScale = 1.0;
        public double bothOpacity = 1.0;
        /** 模式 2 在容器界面上的那一套配置（1.20.18 起独立）。 */
        public int bothContainerX = 4;
        /** 同 {@link #selfStatsY}：{@code -1} = 动态默认位置（屏幕高 ÷ {@value #DEFAULT_Y_DENOM}）。 */
        public int bothContainerY = DEFAULT_Y_SENTINEL;
        public int bothContainerColumns = 2;
        public double bothContainerScale = 1.0;
        public double bothContainerOpacity = 1.0;
        /** 属性面板文字缩放，0.50 ~ 2.00（1.20.4）。 */
        public double selfStatsScale = 1.0;
        /** 属性面板文字不透明度，0.20 ~ 1.00（1.20.4）。 */
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
        public int shieldY = -1;
        /** 缩放倍率（1.0 = 纹理原尺寸 34×86；1.20.22 起允许 0.5~1.5 的小数倍；1.20.26 起默认 0.7）。 */
        public double shieldScale = 0.7;
        /** 不透明度 0.1~1.0（1.20.26 起默认 0.75；1.20.32 起默认 0.65）。 */
        public double shieldOpacity = 0.65;

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
        public int layoutGuiH = 0;
    }

    /**
     * 属性面板的布局快照（绘制用）。位置 / 列数 / 缩放 / 透明度对所有职业统一 ——
     * 这属于「屏幕布局」，跟职业无关；按职业分开的只有「看什么」，见
     * {@link Cfg#selfStatsHidden}。
     */
    public static final class Panel {
        public final int x;
        public final int y;
        public final int columns;
        public final double scale;
        public final double opacity;

        public Panel(int x, int y, int columns, double scale, double opacity) {
            this.x = x;
            this.y = y;
            this.columns = columns;
            this.scale = scale;
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
    public static final int CURRENT_VERSION = 21;

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
     * @param guiW 当前 GUI 宽；{@code <=0} = 不换算（只取列数/缩放/不透明度的调用点用）
     * @param guiH 当前 GUI 高
     */
    public static Panel panel(int mode, boolean container, int guiW, int guiH) {
        Cfg c = withLayoutRef(get(), guiW, guiH);
        int x = resolveX(rawX(c, mode, container), c.layoutGuiW, guiW);
        int y = resolveY(rawY(c, mode, container), c.layoutGuiH, guiH);
        return switch (mode) {
            case 1 -> new Panel(x, y, c.containerColumns, c.containerScale, c.containerOpacity);
            case 2 -> container
                    ? new Panel(x, y, c.bothContainerColumns, c.bothContainerScale, c.bothContainerOpacity)
                    : new Panel(x, y, c.bothColumns, c.bothScale, c.bothOpacity);
            default -> new Panel(x, y, c.selfStatsColumns, c.selfStatsScale, c.selfStatsOpacity);
        };
    }

    /** 设置界面默认编辑 HUD 那一套。 */
    public static Panel panel(int mode) {
        return panel(mode, false);
    }

    /** 当前模式 + HUD 上下文（旧调用点/测试用）。 */
    public static Panel panel() {
        return panel(get().selfStatsMode, false);
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

    /** 写位置（拖动界面用；坐标是**当前 GUI** 下的像素）。 */
    public static void movePanel(int mode, boolean container, int x, int y) {
        movePanel(mode, container, x, y, 0, 0);
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

    public static void setPanelColumns(int mode, boolean container, int columns) {        Cfg c = get();
        switch (mode) {
            case 1 -> c.containerColumns = columns;
            case 2 -> {
                if (container) c.bothContainerColumns = columns;
                else c.bothColumns = columns;
            }
            default -> c.selfStatsColumns = columns;
        }
    }

    public static void setPanelScale(int mode, boolean container, double scale) {
        Cfg c = get();
        switch (mode) {
            case 1 -> c.containerScale = scale;
            case 2 -> {
                if (container) c.bothContainerScale = scale;
                else c.bothScale = scale;
            }
            default -> c.selfStatsScale = scale;
        }
    }

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

    /** 移动当前模式那一套面板（拖动界面用；三种方式互不影响）。 */
    public static void movePanel(int x, int y) {
        Cfg c = get();
        switch (c.selfStatsMode) {
            case 1 -> {
                c.containerX = x;
                c.containerY = y;
            }
            case 2 -> {
                c.bothX = x;
                c.bothY = y;
            }
            default -> {
                c.selfStatsX = x;
                c.selfStatsY = y;
            }
        }
    }

    /** 写当前模式那一套的列数 / 缩放 / 不透明度。 */
    public static void setPanelColumns(int columns) {
        Cfg c = get();
        switch (c.selfStatsMode) {
            case 1 -> c.containerColumns = columns;
            case 2 -> c.bothColumns = columns;
            default -> c.selfStatsColumns = columns;
        }
    }

    public static void setPanelScale(double scale) {
        Cfg c = get();
        switch (c.selfStatsMode) {
            case 1 -> c.containerScale = scale;
            case 2 -> c.bothScale = scale;
            default -> c.selfStatsScale = scale;
        }
    }

    public static void setPanelOpacity(double opacity) {
        Cfg c = get();
        switch (c.selfStatsMode) {
            case 1 -> c.containerOpacity = opacity;
            case 2 -> c.bothOpacity = opacity;
            default -> c.selfStatsOpacity = opacity;
        }
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
     * 老配置文件升级到 {@link #CURRENT_VERSION}。
     *
     * <ul>
     *   <li>v1（没有 {@code configVersion} 键）：血条长度还是旧默认 100 → 改成 75；</li>
     *   <li>v2（1.18.0）：缩放还是旧默认 1.0 → 改成 0.9。</li>
     * </ul>
     * <p>只在**仍等于旧默认值**时才跟着改；自己调过的（比如 120、1.25）保留原值 ——
     * 既不覆盖玩家的选择，也不会让老配置看起来「新默认值没生效」。</p>
     */
    static void migrate(String json, Cfg cfg) {
        if (cfg == null) return;
        boolean hasVersion = json != null && json.contains("\"configVersion\"");
        int version = hasVersion ? cfg.configVersion : 1;
        if (version >= CURRENT_VERSION) return;
        if (version < 2) {
            if (cfg.barWidth == LEGACY_BAR_WIDTH) cfg.barWidth = HudLayout.BAR_WIDTH_DEFAULT;
            if (cfg.teamBarWidth == LEGACY_BAR_WIDTH) cfg.teamBarWidth = HudLayout.BAR_WIDTH_DEFAULT;
        }
        if (version < 3) {
            if (cfg.scale == LEGACY_SCALE) cfg.scale = DEFAULT_SCALE;
            if (cfg.teamScale == LEGACY_SCALE) cfg.teamScale = DEFAULT_SCALE;
        }
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
        if (version < 12) {
            // 1.20.25：删掉了从未被读取的 hideBehindEntities 字段；旧文件里的这个键读取时直接忽略，
            // 下次写盘就不再出现。这里只保证版本号被写上。
        }
        if (version < 14) {
            // 1.20.31：内容过滤列表改成三节分组（属性 / 状态效果 / 其它），状态效果从「一条粗类」
            // 拆成 28 条、其它拆成「草方块 + 未识别行」。旧配置里写下的 "status" 粗类由
            // normalize() 里的 COARSE_KINDS 在**读取时**展开，这里只保证版本号被写上。
        }
        if (version < 13) {
            // 1.20.26：盾牌换成 34×86 的新纹理，默认缩放 0.7、默认不透明度 75%。
            // 沿用本文件的惯例：**只有仍停在旧默认值 1.0 时才跟着改**，自己调过的不覆盖。
            if (cfg.shieldScale == 1.0) cfg.shieldScale = 0.7;
            if (cfg.shieldOpacity == 1.0) cfg.shieldOpacity = 0.75;
        }
        if (version < 18) {
            // 1.20.35：删掉 useServerData 字段 —— 服务端组件推来的权威数据写死优先
            // （服务端没装本 Mod 时本来就会回落到客户端 Boss 栏 / HealthPercent）。
            // 旧文件里的这个键读取时直接忽略。
        }
        if (version < 19) {
            // 1.20.40：面板 / 盾牌的位置改成「相对位置」—— 坐标连同「量它的那套 GUI 尺寸」
            // （layoutGuiW/layoutGuiH）一起存，绘制时按比例换算到当前尺寸。
            // 老文件没有这两个字段（0 = 未知），第一次绘制时用当时的尺寸补上，
            // 所以这里不用改任何坐标值。
        }
        if (version < 17) {
            // 1.20.34：删掉 1.20.33 短暂存在的 teamShowBar —— 队友整块由 showTeammates 管，
            // 「显示血条」关掉时血条和文字一起消失。旧文件里的这个键读取时直接忽略。
        }
        if (version < 15) {
            // 1.20.32：① 实体血条的「文字阴影」跟到新默认值「关」；
            // ② 属性面板文字阴影是**新字段**，用它自己的默认值（开）——
            //    以前面板是被 textShadow 顺带影响的，不该把那个副作用继承下来
            //    （不然「为了关头顶阴影而关掉 textShadow」的人，面板阴影也跟着没了）；
            // ③ 盾牌默认不透明度 75% → 65%（放在 <13 之后：v12 的旧默认 1.0 先落到 0.75，再落到 0.65）。
            if (cfg.textShadow) cfg.textShadow = false;
            if (cfg.shieldOpacity == 0.75) cfg.shieldOpacity = 0.65;
        }
        if (version < 9) {
            // 模式 2 的容器那套从「模式 2 的 HUD 那套」复制。
            cfg.bothContainerX = cfg.bothX;
            cfg.bothContainerY = cfg.bothY;
            cfg.bothContainerColumns = cfg.bothColumns;
            cfg.bothContainerScale = cfg.bothScale;
            cfg.bothContainerOpacity = cfg.bothOpacity;
        }
        if (version < 8) {
            // 模式 2 的那一套从 HUD 那套复制，升级后三种方式各自独立但初值一致。
            cfg.bothX = cfg.selfStatsX;
            cfg.bothY = cfg.selfStatsY;
            cfg.bothColumns = cfg.selfStatsColumns;
            cfg.bothScale = cfg.selfStatsScale;
            cfg.bothOpacity = cfg.selfStatsOpacity;
        }
        if (version < 7) {
            // 容器界面那一套从 HUD 那套复制，避免升级后两套配置对不上。
            cfg.containerX = cfg.selfStatsX;
            cfg.containerY = cfg.selfStatsY;
            cfg.containerColumns = cfg.selfStatsColumns;
            cfg.containerScale = cfg.selfStatsScale;
            cfg.containerOpacity = cfg.selfStatsOpacity;
        }
        if (version < 20) {
            // 1.20.41：面板「初始位置」从 y=40 改成「y = 屏幕高 ÷ 3」（左侧偏上）。
            // 只动仍停在旧默认 40 的那两套（HUD / 模式 2 的 HUD 那套）；自己拖过的位置保留。
            // 写成哨兵 -1 之后，窗口大小变化时默认位置也跟着按 1/3 走。
            if (cfg.selfStatsY == 40) cfg.selfStatsY = DEFAULT_Y_SENTINEL;
            if (cfg.bothY == 40) cfg.bothY = DEFAULT_Y_SENTINEL;
        }
        if (version < 21) {
            // 1.20.42：容器界面那两套的默认位置也改成「屏幕高 ÷ 3」（原来是贴着顶部的 y=20）。
            // 同样只动仍停在旧默认 20 的那两套。
            if (cfg.containerY == 20) cfg.containerY = DEFAULT_Y_SENTINEL;
            if (cfg.bothContainerY == 20) cfg.bothContainerY = DEFAULT_Y_SENTINEL;
        }
        cfg.configVersion = CURRENT_VERSION;
    }

    /**
     * 把任意（含非法 / 缺省 / NaN）配置夹到安全范围。
     *
     * <p>包可见而不是 private：{@code selfCheck} 会直接拿攻击性输入打这个函数。</p>
     */
    static Cfg normalize(Cfg read) {
        if (read == null) read = new Cfg();
        read.opacity = clamp(read.opacity, 0.5, 1.0);
        read.scale = clamp(read.scale, 0.75, 1.5);
        read.maxDistance = clamp(read.maxDistance, 4.0, 256.0);
        // 血条长度：非法值回落默认 75，其余夹在 50~150。
        if (Double.isNaN(read.barWidth)) read.barWidth = HudLayout.BAR_WIDTH_DEFAULT;
        read.barWidth = clamp(read.barWidth, HudLayout.BAR_WIDTH_MIN, HudLayout.BAR_WIDTH_MAX);
        read.verticalOffset = (int) clamp(read.verticalOffset, -30.0, 0.0);
        if (Double.isNaN(read.teamBarWidth)) read.teamBarWidth = HudLayout.BAR_WIDTH_DEFAULT;
        read.teamBarWidth = clamp(read.teamBarWidth, HudLayout.BAR_WIDTH_MIN, HudLayout.BAR_WIDTH_MAX);
        read.teamScale = clamp(read.teamScale, 0.75, 1.5);
        read.teamVerticalOffset = (int) clamp(read.teamVerticalOffset, -30.0, 0.0);
        read.teamMaxDistance = clamp(read.teamMaxDistance, 4.0, 256.0);
        // 1.20.15：这两项写死，不再可配置（hideBehindEntities 在 1.20.25 直接从字段里删掉了）
        read.autoPartyBossbar = true;
        read.hideBehindWalls = false;
        // 自身属性面板（1.20.3）：间隔 0~600 s，列 1~3，坐标夹进屏幕范围
        read.selfStatsSeconds = (int) clamp(read.selfStatsSeconds, 1.0, 5.0);
        read.selfStatsColumns = (int) clamp(read.selfStatsColumns, 1.0, 3.0);
        read.selfStatsX = (int) clamp(read.selfStatsX, 0.0, 4000.0);
        // 1.20.41：y = -1 是「动态默认位置」的哨兵，允许保留。
        read.selfStatsY = (int) clamp(read.selfStatsY, -1.0, 4000.0);
        // 1.20.4：文字缩放、不透明度、内容过滤；1.20.14 起字号最小 1.00、范围 1.0~2.0
        read.selfStatsScale = clamp(read.selfStatsScale, 1.0, 2.0);
        read.selfStatsOpacity = clamp(read.selfStatsOpacity, 0.2, 1.0);
        // 1.20.14：显示方式（0=始终显示 HUD，1=在容器界面显示）+ 容器那一套配置
        read.selfStatsMode = (int) clamp(read.selfStatsMode, 0.0, 2.0);
        read.containerColumns = (int) clamp(read.containerColumns, 1.0, 3.0);
        read.containerX = (int) clamp(read.containerX, 0.0, 4000.0);
        read.containerY = (int) clamp(read.containerY, -1.0, 4000.0);
        read.containerScale = clamp(read.containerScale, 1.0, 2.0);
        read.containerOpacity = clamp(read.containerOpacity, 0.2, 1.0);
        read.bothColumns = (int) clamp(read.bothColumns, 1.0, 3.0);
        read.bothX = (int) clamp(read.bothX, 0.0, 4000.0);
        read.bothY = (int) clamp(read.bothY, -1.0, 4000.0);
        read.bothScale = clamp(read.bothScale, 1.0, 2.0);
        read.bothOpacity = clamp(read.bothOpacity, 0.2, 1.0);
        read.bothContainerColumns = (int) clamp(read.bothContainerColumns, 1.0, 3.0);
        read.bothContainerX = (int) clamp(read.bothContainerX, 0.0, 4000.0);
        read.bothContainerY = (int) clamp(read.bothContainerY, -1.0, 4000.0);
        read.bothContainerScale = clamp(read.bothContainerScale, 1.0, 2.0);
        read.bothContainerOpacity = clamp(read.bothContainerOpacity, 0.2, 1.0);
        read.selfStatsHidden = sanitizeKinds(read.selfStatsHidden);
        // 盾牌指示器（1.20.20：可拖动定位 + 透明度；1.20.22：缩放 0.5~1.5 小数倍）
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
                "stamina", "archery", "class_stat_amp", "strength_amp", "attack_speed_amp"});
        m.put("defence", new String[]{"defence", "true_armor", "defence_amp", "true_armor_amp"});
        m.put("element", new String[]{"fire_armor", "true_fire_armor", "water_armor",
                "electric_armor", "ice_armor", "dark_armor", "light_armor", "fire_armor_amp"});
        m.put("speed", new String[]{"max_speed", "speed_amp", "jump", "double_jump"});
        m.put("mana", new String[]{"mana_power", "mana_recharge", "mana_amp"});
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
