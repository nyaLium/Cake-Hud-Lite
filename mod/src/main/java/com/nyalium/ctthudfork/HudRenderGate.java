package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;

/**
 * 「什么时候干脆不画」的统一闸门。
 *
 * <p>此前「未知诅咒」和「癫狂」的判定分散在 {@code HudTarget}、{@code HudWorldRenderer}、
 * {@code SelfStatsPanel} 三处，各自写一遍 {@code CurseDetector.isActive()} /
 * {@code BerserkDetector.isActive()}。加新的 HUD 入口时很容易漏判，于是统一收到这里。</p>
 *
 * <h2>两种状态的语义不同，不要合并</h2>
 * <ul>
 *   <li><b>未知诅咒</b>：整块 UI 都被藏起来 —— 实体血条、名字、属性面板、容器面板全不画
 *       （{@link #suppressAll}）。</li>
 *   <li><b>癫狂</b>：只有<b>属性面板</b>消失（{@link #suppressPanel}），以及不画
 *       <b>自己</b>那一条血条（{@link #suppressSelfBar}）；<b>敌人与队友的头顶血条、名字、
 *       {@code below_name} 分数全部照常</b> —— 玩家在癫狂期间照样要能判断敌人血量。</li>
 * </ul>
 *
 * <p>对照上游：上游用一个 {@code HudRenderGate.shouldSuppressModHud()} 把两种状态都当成
 * 「整组隐藏」，我们保留了这个区分（癫狂期间玩家仍需要看到敌人血量），只统一了入口。</p>
 */
public final class HudRenderGate {

    private HudRenderGate() {
    }

    /**
     * 整个模组 HUD 都不该画（未知诅咒）。
     *
     * @return {@code true} = 实体血条 / 名字 / 面板全部跳过
     */
    public static boolean suppressAll(Minecraft mc) {
        return mc != null && CurseDetector.isActive(mc);
    }

    /**
     * 属性面板（HUD 上的与容器界面上的）不该画。
     *
     * @return {@code true} = 未知诅咒或癫狂
     */
    public static boolean suppressPanel(Minecraft mc) {
        return mc != null && (CurseDetector.isActive(mc) || BerserkDetector.isActive(mc));
    }

    /**
     * 「自己」那条血条不该画（癫狂）：别人的血条照常。
     *
     * @return {@code true} = 癫狂期间
     */
    public static boolean suppressSelfBar(Minecraft mc) {
        return mc != null && BerserkDetector.isActive(mc);
    }
}
