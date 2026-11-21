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
        /** 总开关：false 时完全不绘制。 */
        public boolean enabled = true;
        /** 整体不透明度，0.50 ~ 1.00（低于 0.50 会被夹到 0.50）。 */
        public double opacity = 0.75;
        /** 文字是否带阴影。 */
        public boolean textShadow = true;
        /** 是否画血条（原版 bossbar 纹理）。心数 / 护甲永远显示，不再提供开关。 */
        public boolean showBar = true;
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
        /** 玩家血条长度（原版名牌像素）。 */
        public double teamBarWidth = 100.0;
        /** 玩家血条整体缩放。 */
        public double teamScale = 1.0;
        /** 玩家血条垂直微调。 */
        public int teamVerticalOffset = 0;
        /** 玩家血条显示距离（格）。 */
        public double teamMaxDistance = 48.0;
        /** 超过这个距离（格）不再绘制。 */
        public double maxDistance = 48.0;
        /** 血条长度（原版名牌像素），50 ~ 150，默认 100。 */
        public double barWidth = 100.0;
        /** 整组 UI 的额外缩放，0.75 ~ 1.50，1.0 = 与原版名牌同字号。 */
        public double scale = 1.0;
        /** 整组 UI 的垂直微调（原版名牌像素），-30 ~ 0，负数=更高。 */
        public int verticalOffset = 0;
        /** 被方块挡住视线时不绘制（防止隔墙看到血条）。 */
        public boolean hideBehindWalls = true;
        /** 左上角打印每帧统计（排查用）。 */
        public boolean debug = false;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long RECHECK_MS = 2000L;

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
            file = FabricLoader.getInstance().getConfigDir().resolve("ctt-enemy-hud.json");
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
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Cfg read = GSON.fromJson(json, Cfg.class);
            cfg = normalize(read);
        } catch (Exception e) {
            cfg = new Cfg();
        }
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
        // 血条长度：非法值回落默认 100，其余夹在 50~150。
        if (Double.isNaN(read.barWidth)) read.barWidth = EnemyHudLayout.BAR_WIDTH_DEFAULT;
        read.barWidth = clamp(read.barWidth, EnemyHudLayout.BAR_WIDTH_MIN, EnemyHudLayout.BAR_WIDTH_MAX);
        read.verticalOffset = (int) clamp(read.verticalOffset, -30.0, 0.0);
        if (Double.isNaN(read.teamBarWidth)) read.teamBarWidth = EnemyHudLayout.BAR_WIDTH_DEFAULT;
        read.teamBarWidth = clamp(read.teamBarWidth, EnemyHudLayout.BAR_WIDTH_MIN, EnemyHudLayout.BAR_WIDTH_MAX);
        read.teamScale = clamp(read.teamScale, 0.75, 1.5);
        read.teamVerticalOffset = (int) clamp(read.teamVerticalOffset, -30.0, 0.0);
        read.teamMaxDistance = clamp(read.teamMaxDistance, 4.0, 256.0);
        return read;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return hi;
        return Math.max(lo, Math.min(hi, v));
    }
}
