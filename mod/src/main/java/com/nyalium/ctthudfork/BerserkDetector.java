package com.nyalium.ctthudfork;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 「癫狂（Berserk）」客户端探测 —— 写死行为，没有开关（1.2.4）。
 *
 * <h2>地图里是什么</h2>
 * <p>数据包 {@code items/status_effect21_berserk.mcfunction}：玩家带 {@code Berserk} 标签时
 * 每次触发会发两条 tellraw：</p>
 * <ul>
 *   <li>{@code Items_status_effect21_berserk_1}「你陷入了癫狂」</li>
 *   <li>{@code Items_status_effect21_berserk_2}「+1 力量，但无法查看你的 UI」</li>
 * </ul>
 * <p>解除时发 {@code Items_status_effect21_berserk_3}「你不再是癫狂」。</p>
 *
 * <h2>为什么只能靠聊天</h2>
 * <p>标签 / 分数（{@code Berserk}）不上展示槽，客户端读不到；地图也没有给原版状态效果。
 * 客户端唯一可见的锚点就是这三条 tellraw 的<b>翻译键</b>（不随语言包变）。</p>
 *
 * <h2>硬性约定</h2>
 * <p>癫狂期间<b>隐藏玩家与敌人的头顶血条 / 心数 / 护甲</b>（与原版名字一起交还原版），
 * <b>但不动自身属性面板</b> —— 面板是「查看自己属性」的通道，玩家癫狂时仍应可用。</p>
 *
 * <p>局限：如果玩家<b>带着癫狂进服</b>（消息在别处发过），客户端收不到开场消息，
 * 会一直当作「没癫狂」；直到下次看到 {@code _1} 或 {@code _3} 才会更新。</p>
 */
final class BerserkDetector {

    /** 进入癫狂的翻译键。 */
    static final String ON_KEY = "Items_status_effect21_berserk_1";
    /** 解除癫狂的翻译键。 */
    static final String OFF_KEY = "Items_status_effect21_berserk_3";

    private static volatile boolean active = false;

    private BerserkDetector() {}

    /** 当前是否处于癫狂（只影响实体血条，不影响属性面板）。 */
    static boolean isActive() {
        return active;
    }

    /** 进服 / 断线时清掉，避免把上一局的状态带过来。 */
    static void reset() {
        active = false;
    }

    /**
     * 观察一条聊天消息；命中 {@link #ON_KEY} / {@link #OFF_KEY} 时更新状态。
     *
     * <p>只观察、不拦截：这两条消息仍然照常显示在聊天栏。</p>
     */
    static void observe(Component message, boolean overlay) {
        if (overlay || message == null) return;
        if (hasKey(message, ON_KEY)) {
            active = true;
        } else if (hasKey(message, OFF_KEY)) {
            active = false;
        }
    }

    /** 组件树里任一段的翻译键等于 {@code key}。 */
    private static boolean hasKey(Component message, String key) {
        if (message.getContents() instanceof TranslatableContents ttc && ttc.getKey() != null
                && ttc.getKey().equals(key)) {
            return true;
        }
        for (Component sibling : message.getSiblings()) {
            if (hasKey(sibling, key)) return true;
        }
        return false;
    }

    /** 纯函数：这条键是不是癫狂的开 / 关消息。 */
    static boolean isBerserkKey(String key) {
        return ON_KEY.equals(key) || OFF_KEY.equals(key);
    }
}
