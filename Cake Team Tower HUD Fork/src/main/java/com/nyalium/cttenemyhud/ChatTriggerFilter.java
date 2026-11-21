package com.nyalium.cttenemyhud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 吞掉「本 Mod 自己发的 /trigger」在聊天栏里的回显与报错。
 *
 * <h2>为什么需要</h2>
 * <p>自动开队伍条要发 {@code /trigger TogglePartyBossbar}，每次执行原版都会往聊天栏留一条：</p>
 * <ul>
 *   <li>成功：{@code Triggered [TogglePartyBossbar]} / {@code 已触发 [TogglePartyBossbar]}；</li>
 *   <li>失败：{@code commands.trigger.failed.unprimed} → 中文「无法触发该计分项」、英文
 *       "Cannot trigger this objective"（触发项没 enable，例如玩家身上没有 CTT 标签）。</li>
 * </ul>
 *
 * <h2>判定</h2>
 * <p>只在「刚刚自己发过命令」（{@value #ECHO_WINDOW_MS} ms 内）时才过滤，所以玩家手动敲
 * {@code /trigger} 的回显不受影响：</p>
 * <ol>
 *   <li>翻译键以 {@code commands.trigger.failed} 开头 → 失败消息；</li>
 *   <li>文本含 {@code Triggered} 或 {@code 已触发} 且含本次命令名 → 成功回显。</li>
 * </ol>
 * <p>只影响本地聊天 HUD，服务端记录与其它玩家看到的都照旧。</p>
 */
final class ChatTriggerFilter {

    /** 自己发命令到回显到达之间的合理窗口。 */
    private static final long ECHO_WINDOW_MS = 3_000L;

    /** 本 Mod 会自发发送的 trigger 名称（将来新增自动触发命令时在这里追加）。 */
    private static final String[] MUTED_TRIGGERS = {
            "TogglePartyBossbar",
    };

    private static final String FAILED_KEY_PREFIX = "commands.trigger.failed";

    private static volatile long lastSentMs = 0L;

    private ChatTriggerFilter() {}

    /** 每次发命令前调用。 */
    static void noteTriggerSent(long nowMs) {
        lastSentMs = nowMs;
    }

    /** true = 这条聊天消息应当从本地聊天里隐藏。 */
    static boolean shouldHide(Component message, boolean overlay) {
        if (overlay || message == null) return false;
        if (System.currentTimeMillis() - lastSentMs > ECHO_WINDOW_MS) return false;

        if (hasFailedKey(message)) return true;

        String raw = message.getString();
        if (raw.isEmpty()) return false;
        boolean triggered = raw.contains("Triggered") || raw.contains("已触发");
        if (!triggered) return false;
        for (String name : MUTED_TRIGGERS) {
            if (raw.contains(name)) return true;
        }
        return false;
    }

    /** 组件树里任一段的翻译键以 {@code commands.trigger.failed} 开头。 */
    private static boolean hasFailedKey(Component message) {
        if (message == null) return false;
        if (message.getContents() instanceof TranslatableContents ttc
                && ttc.getKey() != null
                && ttc.getKey().startsWith(FAILED_KEY_PREFIX)) {
            return true;
        }
        for (Component sibling : message.getSiblings()) {
            if (hasFailedKey(sibling)) return true;
        }
        return false;
    }
}
