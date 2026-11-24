package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 「癫狂（Berserk）」客户端探测 —— 靠地图的**专属 Boss 栏**识别（1.2.5 起），写死、无开关。
 *
 * <h2>地图里是什么</h2>
 * <p>数据包 {@code misc/bossbars/p1..p10_bossbar.mcfunction} 的前几行：玩家带 {@code Berserk} 标签时，
 * 他本人那条个人状态条（{@code player_1001..1010}）会被换成一整段</p>
 * <pre>
 *   {"translate":"Misc_Bossbars_P10_Bossbar_11","italic":true,"color":"dark_red"}
 * </pre>
 * <p>文本是「我只有一个想法，战…………」（十个槽位用的是**同一个翻译键**），
 * 并且 {@code value} 被设成 {@code 100000}。也就是说：<b>癫狂期间，你上方会多出一条专属 Boss 栏</b>。</p>
 *
 * <h2>为什么用翻译键</h2>
 * <p>翻译键不随语言包变化，也比聊天消息可靠：bar 每 tick 都在，不需要「刚好看到那条消息」。
 * 1.2.4 用聊天里的 {@code Items_status_effect21_berserk_1/_3} 识别，带癫狂进服会漏；这里换成 bar 探测。</p>
 *
 * <h2>硬性约定</h2>
 * <p>癫狂期间<b>隐藏玩家与敌人的头顶血条 / 心数 / 护甲</b>（名字交还原版），
 * <b>但不动自身属性面板</b> —— 面板是「查看自己属性」的通道，癫狂时仍应可用。</p>
 */
final class BerserkDetector {

    /** 癫狂专属 Boss 栏标题里的翻译键（p1..p10 共用这一个）。 */
    static final String KEY = "Misc_Bossbars_P10_Bossbar_11";

    /** 探测节流：bar 每 tick 都会重设，100 ms 足够。 */
    private static final long PROBE_INTERVAL_MS = 100L;
    /** 键消失后继续保持这么久，避开逐 tick 重建期间的闪烁。 */
    private static final long RELEASE_DELAY_MS = 500L;

    private static boolean active = false;
    private static long lastSeenMs = 0L;
    private static long lastProbeMs = 0L;

    private BerserkDetector() {}

    /** 当前是否处于癫狂（只影响实体血条，不影响属性面板）。 */
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

    /** 进服 / 断线时清掉，避免把上一局的状态带过来。 */
    static void reset() {
        active = false;
        lastSeenMs = 0L;
        lastProbeMs = 0L;
    }

    private static boolean probe(Minecraft mc) {
        if (mc == null || mc.gui == null || mc.gui.hud == null) return false;
        try {
            var bars = ((com.nyalium.ctthudfork.mixin.BossHealthOverlayAccessor) (Object)
                    mc.gui.hud.getBossOverlay()).cttHudFork$getBossBars();
            if (bars == null || bars.isEmpty()) return false;
            for (var bar : bars.values()) {
                if (bar != null && marksBerserk(bar.getName())) return true;
            }
        } catch (Throwable ignored) {
            // 结构不符就当作没触发，绝不因为探测失败崩游戏。
        }
        return false;
    }

    /** 组件树里任一段的翻译键是癫狂键。 */
    static boolean marksBerserk(Component text) {
        if (text == null) return false;
        if (text.getContents() instanceof TranslatableContents ttc && isBerserkKey(ttc.getKey())) {
            return true;
        }
        for (Component sibling : text.getSiblings()) {
            if (marksBerserk(sibling)) return true;
        }
        return false;
    }

    /** 纯函数部分，便于自检。 */
    static boolean isBerserkKey(String key) {
        return KEY.equals(key);
    }
}
