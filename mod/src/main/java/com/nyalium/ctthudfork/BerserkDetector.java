package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 「癫狂（Berserk）」客户端探测 —— 1.3.3 起是 {@link BossBarKeyDetector} 的一个实例
 * （探测逻辑与诅咒共用，<b>效果不共用</b>）。
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
 * <h2>效果（与诅咒不同）</h2>
 * <p>癫狂期间<b>只隐藏自身属性面板</b>（HUD + 容器）；玩家/敌人的头顶血条与信息<b>照常显示</b>。
 * 面板本身不显示，与地图「无法查看你的 UI」的说法一致。</p>
 *
 * <p>键消失后保持 500 ms，避开逐 tick 重建期间的闪烁。</p>
 */
final class BerserkDetector {

    /** 癫狂专属 Boss 栏标题里的翻译键（p1..p10 共用这一个）。 */
    static final String KEY = "Misc_Bossbars_P10_Bossbar_11";

    /** 癫狂状态的探测器实例（等值匹配、释放 0.5 s）。 */
    static final BossBarKeyDetector STATE = new BossBarKeyDetector(KEY, false, 500L);

    private BerserkDetector() {}

    /** 当前是否处于癫狂（只影响属性面板，不影响实体血条）。 */
    static boolean isActive(Minecraft mc) {
        return STATE.isActive(mc);
    }

    /** 进服 / 断线时清掉，避免把上一局的状态带过来。 */
    static void reset() {
        STATE.reset();
    }

    /** 组件树里任一段的翻译键是癫狂键。 */
    static boolean marksBerserk(Component text) {
        return STATE.marks(text);
    }

    /** 纯函数部分，便于自检。 */
    static boolean isBerserkKey(String key) {
        return STATE.matches(key);
    }
}
