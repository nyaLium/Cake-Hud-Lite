package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 「未知诅咒」（Curse of the Unknown）客户端探测 —— 是
 * {@link BossBarKeyDetector} 的一个实例（探测逻辑与癫狂共用，<b>效果不共用</b>）。
 *
 * <h2>地图里是什么</h2>
 * <p>数据包 {@code floors/curse_rooms/d_curse02_unknown.mcfunction:16+} 在诅咒期间把
 * {@code player_1001..1010} 全部个人状态条的名称换成乱码文本，这些文本段的翻译键
 * 统一以 {@code Floors_Curse_Rooms_D_Curse02_Unknown_} 开头。翻译键不随语言包变化，
 * 是客户端最稳的识别锚点。</p>
 *
 * <h2>效果（与癫狂不同）</h2>
 * <p>诅咒期间本 Mod 的 UI <b>全部隐藏</b>：实体血条（{@code HudTarget} / {@code HudWorldRenderer}）
 * 与属性面板（HUD + 容器，{@code SelfStatsPanel}）。不提供开关，与地图「看不到任何 UI」的设定一致。</p>
 *
 * <p>命中后带 2 s 滞后释放，避免 bossbar 逐 tick 翻页期间闪烁。</p>
 */
final class CurseDetector {

    /** 诅咒 bossbar 文本段的翻译键前缀（与数据包一一对应）。 */
    static final String CURSE_KEY_PREFIX = "Floors_Curse_Rooms_D_Curse02_Unknown_";

    /** 诅咒状态的探测器实例（前缀匹配、释放 2 s）。 */
    static final BossBarKeyDetector STATE =
            new BossBarKeyDetector(CURSE_KEY_PREFIX, true, 2000L);

    private CurseDetector() {}

    /** 当前是否处于诅咒状态（含滞后窗口）。 */
    static boolean isActive(Minecraft mc) {
        return STATE.isActive(mc);
    }

    static void reset() {
        STATE.reset();
    }

    /** 组件树中任一段的翻译键命中前缀即为诅咒条。 */
    static boolean marksCurse(Component text) {
        return STATE.marks(text);
    }

    /** 纯函数部分，便于自检。 */
    static boolean isCurseKey(String key) {
        return STATE.matches(key);
    }
}
