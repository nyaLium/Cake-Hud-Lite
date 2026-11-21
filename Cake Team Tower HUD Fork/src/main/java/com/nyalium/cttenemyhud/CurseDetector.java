package com.nyalium.cttenemyhud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 「未知诅咒」（Curse of the Unknown）客户端探测。
 *
 * <p>数据包 {@code floors/curse_rooms/d_curse02_unknown.mcfunction:16+} 在诅咒期间把
 * {@code player_1001..1010} 全部个人状态条的名称换成乱码文本，这些文本段的翻译键
 * 统一以 {@code Floors_Curse_Rooms_D_Curse02_Unknown_} 开头。翻译键不随语言包变化，
 * 是客户端最稳的识别锚点。</p>
 *
 * <p>按用户要求<b>不提供开关</b>：诅咒生效期间本 Mod 全部 UI 一律隐藏，与「玩家此刻
 * 看不到任何 UI」的设定保持一致。</p>
 *
 * <p>命中后带 2 s 滞后释放，避免 bossbar 逐 tick 翻页期间闪烁。</p>
 */
final class CurseDetector {

    /** 诅咒 bossbar 文本段的翻译键前缀（与数据包一一对应）。 */
    static final String CURSE_KEY_PREFIX = "Floors_Curse_Rooms_D_Curse02_Unknown_";

    /** 键消失后再持续隐藏这么久，避开逐 tick 翻页的抖动。 */
    private static final long RELEASE_DELAY_MS = 2000L;

    private static boolean active = false;
    private static long lastSeenMs = 0L;
    private static long lastProbeMs = 0L;
    /** 每 100 ms 探测一次足够，bossbar 内容不会比这更快。 */
    private static final long PROBE_INTERVAL_MS = 100L;

    private CurseDetector() {}

    /** 当前是否处于诅咒状态（含滞后窗口）。 */
    static boolean isActive(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastProbeMs >= PROBE_INTERVAL_MS) {
            lastProbeMs = now;
            if (probe(mc)) {
                lastSeenMs = now;
                active = true;
            }
        }
        if (active && now - lastSeenMs > RELEASE_DELAY_MS) {
            active = false;
        }
        return active;
    }

    static void reset() {
        active = false;
        lastSeenMs = 0L;
        lastProbeMs = 0L;
    }

    private static boolean probe(Minecraft mc) {
        if (mc.player == null || mc.gui == null || mc.gui.hud == null) return false;
        try {
            var bars = ((com.nyalium.cttenemyhud.mixin.BossHealthOverlayAccessor) (Object)
                    mc.gui.hud.getBossOverlay()).cttEnemyHud$getBossBars();
            if (bars == null || bars.isEmpty()) return false;
            for (var bar : bars.values()) {
                if (bar != null && marksCurse(bar.getName())) return true;
            }
        } catch (Throwable ignored) {
            // 结构不符就当作没触发，绝不因为探测失败崩游戏。
        }
        return false;
    }

    /** 组件树中任一段的翻译键命中前缀即为诅咒条（bossbar 名称是兄弟段列表）。 */
    static boolean marksCurse(Component text) {
        if (text == null) return false;
        if (text.getContents() instanceof TranslatableContents ttc && isCurseKey(ttc.getKey())) {
            return true;
        }
        for (Component sibling : text.getSiblings()) {
            if (marksCurse(sibling)) return true;
        }
        return false;
    }

    /** 纯函数部分，便于自检。 */
    static boolean isCurseKey(String key) {
        return key != null && key.startsWith(CURSE_KEY_PREFIX);
    }
}
