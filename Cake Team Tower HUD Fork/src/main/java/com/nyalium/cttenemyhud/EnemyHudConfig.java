package com.nyalium.cttenemyhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置文件 {@code config/ctt-enemy-hud.json}。
 *
 * <p>三条写入路径：① 首次启动写默认值；② Mod Menu 里改完点「完成」写回；
 * ③ 直接在磁盘上手改，游戏每 2 秒检查一次修改时间并热加载。</p>
 */
public final class EnemyHudConfig {

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
        /** 文字是否带阴影。 */
        public boolean textShadow = true;
        /** 是否画血条（原版 bossbar 纹理）。心数 / 护甲永远显示，不再提供开关。 */
        public boolean showBar = true;
        /**
         * 优先使用服务端选装组件推来的权威数据（{@link MobHealthPayload}）。
         * 服务端没装本 Mod 时这一项无影响，自动回落到客户端 Boss 栏 + HealthPercent。
         */
        public boolean useServerData = true;
        /** 隐藏实体名称下方那条计分板分数（数据包的 HealthPercent 显示）。 */
        public boolean hideBelowNameScore = true;
        /** 隐藏上方 Boss 栏里「最近敌人的信息」那一条（头顶已经画过了）。 */
        public boolean hideEnemyBossBar = true;
        /** 是否显示队友血量（绿色条）。自己永远不显示。 */
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
        /** 被方块挡住视线时不绘制（防止隔墙看到血条）。1.20.14 起默认关闭、界面不再提供开关。 */
        public boolean hideBehindWalls = false;
        /** 被更近的实体挡住时不绘制（防止后方血条压住前面的名字和模型）。 */
        public boolean hideBehindEntities = true;
        /** 左上角打印每帧统计（排查用）。 */
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
        /** 属性面板左上角屏幕坐标（GUI 像素）。 */
        public int selfStatsX = 4;
        public int selfStatsY = 40;
        /**
         * 属性面板显示方式：0 = 始终显示（HUD），1 = 在容器界面显示，2 = HUD + 容器都显示（1.20.16）。
         * 每种上下文（HUD / 容器）各有一套位置·列数·字号·不透明度。
         */
        public int selfStatsMode = 0;
        /** 容器界面那一套配置（模式 1 专用）。 */
        public int containerX = 4;
        public int containerY = 20;
        public int containerColumns = 2;
        public double containerScale = 1.0;
        public double containerOpacity = 1.0;
        /** 模式 2 在 HUD 上的那一套配置。 */
        public int bothX = 4;
        public int bothY = 40;
        public int bothColumns = 2;
        public double bothScale = 1.0;
        public double bothOpacity = 1.0;
        /** 模式 2 在容器界面上的那一套配置（1.20.18 起独立）。 */
        public int bothContainerX = 4;
        public int bothContainerY = 20;
        public int bothContainerColumns = 2;
        public double bothContainerScale = 1.0;
        public double bothContainerOpacity = 1.0;
        /** 属性面板文字缩放，0.50 ~ 2.00（1.20.4）。 */
        public double selfStatsScale = 1.0;
        /** 属性面板文字不透明度，0.20 ~ 1.00（1.20.4）。 */
        public double selfStatsOpacity = 1.0;
        /**
         * 默认（全部职业共用）时要隐藏的内容类，取值见 {@link SelfStatsKind#id}。
         *
         * <p>例如 {@code ["heart_red", "hp_neg"]} = 红心与负生命那几行不上面板。
         * 1.20.5 起过滤粒度是<b>单个属性</b>；旧版本写的粗类（{@code health}/{@code attack}…）
         * 会在读取时自动展开成对应的一串细类。</p>
         */
        public java.util.List<String> selfStatsHidden = new java.util.ArrayList<>();
    }

    /**
     * 属性面板的布局快照（绘制用）。位置 / 列数 / 缩放 / 透明度对所有职业统一 ——
     * 这属于「屏幕布局」，跟职业无关；按职业分开的只有「看什么」，见
     * {@link Cfg#selfStatsClassHidden}。
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
    public static final int CURRENT_VERSION = 9;
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

    private EnemyHudConfig() {}

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
        Cfg c = get();
        return switch (mode) {
            case 1 -> new Panel(c.containerX, c.containerY, c.containerColumns,
                    c.containerScale, c.containerOpacity);
            case 2 -> container
                    ? new Panel(c.bothContainerX, c.bothContainerY, c.bothContainerColumns,
                            c.bothContainerScale, c.bothContainerOpacity)
                    : new Panel(c.bothX, c.bothY, c.bothColumns, c.bothScale, c.bothOpacity);
            default -> new Panel(c.selfStatsX, c.selfStatsY, c.selfStatsColumns,
                    c.selfStatsScale, c.selfStatsOpacity);
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

    /** 当前模式 + 当前编辑目标（模式 2 可能是容器界面）。 */
    public static Panel panelFor(int mode, boolean container) {
        return panel(mode, container);
    }

    /** 写位置（拖动界面用）。 */
    public static void movePanel(int mode, boolean container, int x, int y) {
        Cfg c = get();
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

    /** 当前配置的深拷贝，用作设置界面「取消」的回滚点（不落盘）。 */
    public static Cfg copy() {
        Cfg copy = GSON.fromJson(GSON.toJson(cfg), Cfg.class);
        return normalize(copy);
    }

    /** 换回给定配置（「取消」用；不落盘）。 */
    public static void restore(Cfg snapshot) {
        cfg = normalize(snapshot);
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
            if (cfg.barWidth == LEGACY_BAR_WIDTH) cfg.barWidth = EnemyHudLayout.BAR_WIDTH_DEFAULT;
            if (cfg.teamBarWidth == LEGACY_BAR_WIDTH) cfg.teamBarWidth = EnemyHudLayout.BAR_WIDTH_DEFAULT;
        }
        if (version < 3) {
            if (cfg.scale == LEGACY_SCALE) cfg.scale = DEFAULT_SCALE;
            if (cfg.teamScale == LEGACY_SCALE) cfg.teamScale = DEFAULT_SCALE;
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
        if (Double.isNaN(read.barWidth)) read.barWidth = EnemyHudLayout.BAR_WIDTH_DEFAULT;
        read.barWidth = clamp(read.barWidth, EnemyHudLayout.BAR_WIDTH_MIN, EnemyHudLayout.BAR_WIDTH_MAX);
        read.verticalOffset = (int) clamp(read.verticalOffset, -30.0, 0.0);
        if (Double.isNaN(read.teamBarWidth)) read.teamBarWidth = EnemyHudLayout.BAR_WIDTH_DEFAULT;
        read.teamBarWidth = clamp(read.teamBarWidth, EnemyHudLayout.BAR_WIDTH_MIN, EnemyHudLayout.BAR_WIDTH_MAX);
        read.teamScale = clamp(read.teamScale, 0.75, 1.5);
        read.teamVerticalOffset = (int) clamp(read.teamVerticalOffset, -30.0, 0.0);
        read.teamMaxDistance = clamp(read.teamMaxDistance, 4.0, 256.0);
        // 1.20.15：这三项写死，不再可配置
        read.autoPartyBossbar = true;
        read.hideBehindWalls = false;
        read.hideBehindEntities = false;
        // 自身属性面板（1.20.3）：间隔 0~600 s，列 1~3，坐标夹进屏幕范围
        read.selfStatsSeconds = (int) clamp(read.selfStatsSeconds, 1.0, 5.0);
        read.selfStatsColumns = (int) clamp(read.selfStatsColumns, 1.0, 3.0);
        read.selfStatsX = (int) clamp(read.selfStatsX, 0.0, 4000.0);
        read.selfStatsY = (int) clamp(read.selfStatsY, 0.0, 4000.0);
        // 1.20.4：文字缩放、不透明度、内容过滤；1.20.14 起字号最小 1.00、范围 1.0~2.0
        read.selfStatsScale = clamp(read.selfStatsScale, 1.0, 2.0);
        read.selfStatsOpacity = clamp(read.selfStatsOpacity, 0.2, 1.0);
        // 1.20.14：显示方式（0=始终显示 HUD，1=在容器界面显示）+ 容器那一套配置
        read.selfStatsMode = (int) clamp(read.selfStatsMode, 0.0, 2.0);
        read.containerColumns = (int) clamp(read.containerColumns, 1.0, 3.0);
        read.containerX = (int) clamp(read.containerX, 0.0, 4000.0);
        read.containerY = (int) clamp(read.containerY, 0.0, 4000.0);
        read.containerScale = clamp(read.containerScale, 1.0, 2.0);
        read.containerOpacity = clamp(read.containerOpacity, 0.2, 1.0);
        read.bothColumns = (int) clamp(read.bothColumns, 1.0, 3.0);
        read.bothX = (int) clamp(read.bothX, 0.0, 4000.0);
        read.bothY = (int) clamp(read.bothY, 0.0, 4000.0);
        read.bothScale = clamp(read.bothScale, 1.0, 2.0);
        read.bothOpacity = clamp(read.bothOpacity, 0.2, 1.0);
        read.bothContainerColumns = (int) clamp(read.bothContainerColumns, 1.0, 3.0);
        read.bothContainerX = (int) clamp(read.bothContainerX, 0.0, 4000.0);
        read.bothContainerY = (int) clamp(read.bothContainerY, 0.0, 4000.0);
        read.bothContainerScale = clamp(read.bothContainerScale, 1.0, 2.0);
        read.bothContainerOpacity = clamp(read.bothContainerOpacity, 0.2, 1.0);
        read.selfStatsHidden = sanitizeKinds(read.selfStatsHidden);
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
                "karma_light", "karma_neutral", "karma_dark", "karma_celestial", "misc"});
        return java.util.Map.copyOf(m);
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
