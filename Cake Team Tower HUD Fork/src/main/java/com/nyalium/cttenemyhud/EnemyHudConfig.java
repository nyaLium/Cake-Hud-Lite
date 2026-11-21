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
        public double opacity = 1.0;
        /** 文字是否带阴影。 */
        public boolean textShadow = true;
        /** 是否画血条（原版 bossbar 纹理）。 */
        public boolean showBar = true;
        /** 是否画「当前血量/最大血量」文字。 */
        public boolean showNumbers = true;
        /** 是否画护甲段（🛡Defence + 元素护甲）。 */
        public boolean showArmor = true;
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
        return read;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v)) return hi;
        return Math.max(lo, Math.min(hi, v));
    }
}
