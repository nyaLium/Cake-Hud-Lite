package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 「某一类 Boss 栏文本 = 某种状态」的通用探测器。
 *
 * <h2>为什么合并</h2>
 * <p>诅咒与癫狂共用同一套探测实现：都是 100 ms 探测一次、命中后保持一段时间、
 * 组件树里找翻译键。合并的只有这层<b>探测</b>；两者<b>命中后做什么</b>仍写在各自的调用点：</p>
 * <ul>
 *   <li><b>诅咒</b>：隐藏一切（实体 HUD + 属性面板 HUD/容器）；</li>
 *   <li><b>癫狂</b>：只隐藏属性面板，实体血条照常。</li>
 * </ul>
 *
 * <p>两个状态各持一个实例（{@link CurseDetector#STATE} / {@link BerserkDetector#STATE}），
 * 状态互不共享；释放时长也各自设置。</p>
 */
final class BossBarKeyDetector {

    /** bar 内容不会比这更快变化，100 ms 探测足够。 */
    private static final long PROBE_INTERVAL_MS = 100L;

    /** 翻译键（前缀模式 = 只比前缀）。 */
    private final String key;
    /** true = {@link #key} 是前缀；false = 必须完全相等。 */
    private final boolean prefix;
    /** 键消失后再保持这么久的「已命中」状态，避开逐 tick 重建的抖动。 */
    private final long releaseMs;

    private boolean active = false;
    private long lastSeenMs = 0L;
    private long lastProbeMs = 0L;

    BossBarKeyDetector(String key, boolean prefix, long releaseMs) {
        this.key = key;
        this.prefix = prefix;
        this.releaseMs = releaseMs;
    }

    /** 当前是否处于该状态（含滞后释放窗口）。 */
    boolean isActive(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastProbeMs >= PROBE_INTERVAL_MS) {
            lastProbeMs = now;
            if (probe(mc)) {
                lastSeenMs = now;
                active = true;
            }
        }
        if (active && now - lastSeenMs > releaseMs) {
            active = false;
        }
        return active;
    }

    /** 进服 / 断线清掉，避免把上一局的状态带过来。 */
    void reset() {
        active = false;
        lastSeenMs = 0L;
        lastProbeMs = 0L;
    }

    /** 组件树里任一段的翻译键命中即为命中（bossbar 名称是兄弟段列表）。 */
    boolean marks(Component text) {
        if (text == null) return false;
        if (text.getContents() instanceof TranslatableContents ttc && matches(ttc.getKey())) {
            return true;
        }
        for (Component sibling : text.getSiblings()) {
            if (marks(sibling)) return true;
        }
        return false;
    }

    /** 纯函数：键是否命中（便于自检）。 */
    boolean matches(String candidate) {
        if (candidate == null) return false;
        return prefix ? candidate.startsWith(key) : key.equals(candidate);
    }

    private boolean probe(Minecraft mc) {
        if (mc == null || mc.player == null) return false;
        // 统一走 BossBarTracker 的取条入口（同一个反射点，只此一处）。
        for (var bar : BossBarTracker.bossBars(mc).values()) {
            if (bar != null && marks(bar.getName())) return true;
        }
        return false;
    }
}
