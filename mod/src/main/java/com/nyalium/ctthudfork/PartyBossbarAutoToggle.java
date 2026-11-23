package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;

/**
 * 自动把「队伍血量」这条 Boss 栏打开（队友血量的唯一客户端数据源）。
 *
 * <h2>为什么能开、怎么开</h2>
 * <p>数据包 {@code misc.mcfunction:997-1002}：</p>
 * <pre>
 *   scoreboard players enable @a[tag=CTT] TogglePartyBossbar
 *   scoreboard players add @a[scores={TogglePartyBossbar=1..}] TogglePartyBossbarA 1
 *   scoreboard players set @a[scores={TogglePartyBossbarA=2..}] TogglePartyBossbarA 0
 *   tag @a[scores={TogglePartyBossbarA=1},tag=CTTAll] add PlayerHealthBossbar   # 每 tick 重新加
 * </pre>
 * <p>{@code TogglePartyBossbarA} 在第一次按后<b>一直保持 1</b>，所以那条 tag 每 tick 都会被重新加，
 * 队伍条就会持续显示；再按一次 {@code A=2→0} 才关。也就是说它是个<b>真开关</b>。</p>
 *
 * <h2>所以最大的风险是「多按一次」</h2>
 * <p>多按一次 = 把条关掉。因此策略是<b>宁可不开，也不乱开</b>：</p>
 * <ul>
 *   <li>只在<b>真的没看到队伍条</b>时发（检测走 {@link BossBarTracker#hasTeamBar}，已含宽松扫描 + Tab 名单匹配）；</li>
 *   <li>第一次成功之后，要连续消失 {@value #REOPEN_DELAY_MS} ms 才允许再开一次
 *       —— 一次检测抖动不会把条点掉；</li>
 *   <li>两次发送至少间隔 {@value #COOLDOWN_MS} ms；</li>
 *   <li>连续 {@value #MAX_ATTEMPTS} 次尝试都没出现（多半是触发项没 enable，例如不在 CTT 标签里）→
 *       进入 {@value #FAILSAFE_MS} ms 熔断，期间完全不发，聊天栏也就不会再刷「无法触发该计分项」；</li>
 *   <li>单局最多发 {@value #MAX_SENDS} 次（{@code MAX_SENDS}，1.20.25 才写进文档的兜底上限）；</li>
 *   <li>暂停 / 开着界面（背包、聊天、ESC）时一律不发；</li>
 *   <li>换世界 / 换服务器状态清零。</li>
 * </ul>
 * <p>参数与 Cake-Hud 的 {@code maybeAutoTogglePartyBossbar} 一致（3 s 冷却 / 5 次失败 / 10 分钟熔断）。</p>
 */
final class PartyBossbarAutoToggle {

    private static final long COOLDOWN_MS = 3_000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final long FAILSAFE_MS = 600_000L;
    /** 见过队伍条之后，要连续消失这么久才允许再开（防一次检测抖动把条点掉）。 */
    private static final long REOPEN_DELAY_MS = 10_000L;
    /** 单局最多发几次，兜底防跑飞。 */
    private static final int MAX_SENDS = 20;

    private static Object levelRef;
    private static int attempts;
    private static int sends;
    private static long lastSendMs;
    private static long failsafeUntilMs;
    private static long absentSinceMs;
    private static boolean seenTeamBar;

    private PartyBossbarAutoToggle() {}

    static void tick(Minecraft mc, boolean teamBarPresent, boolean personalBarPresent) {
        long now = System.currentTimeMillis();

        // 换世界 / 换服：状态清零。
        if (mc.level != levelRef) {
            levelRef = mc.level;
            attempts = 0;
            sends = 0;
            lastSendMs = 0L;
            failsafeUntilMs = 0L;
            absentSinceMs = 0L;
            seenTeamBar = false;
        }

        if (teamBarPresent) {
            seenTeamBar = true;
            attempts = 0;
            failsafeUntilMs = 0L;
            absentSinceMs = 0L;
            return;
        }

        // 不在对局（看不到自己的状态条）：清计数，不消耗尝试次数。
        if (!personalBarPresent) {
            attempts = 0;
            absentSinceMs = 0L;
            return;
        }

        if (absentSinceMs == 0L) absentSinceMs = now;
        // 见过之后必须先连续消失一段时间，才认为「真的被关了」。
        if (seenTeamBar && now - absentSinceMs < REOPEN_DELAY_MS) return;

        if (now < failsafeUntilMs) return;
        if (sends >= MAX_SENDS) return;
        if (now - lastSendMs < COOLDOWN_MS) return;
        if (mc.getConnection() == null || mc.player == null) return;
        // 暂停菜单 / 背包 / 聊天 里不发，避免在这些界面刷聊天报错。
        if (mc.isPaused()) return;
        if (mc.gui != null && mc.gui.screen() != null) return;

        lastSendMs = now;
        attempts++;
        sends++;
        ChatTriggerFilter.noteTriggerSent(now);
        mc.getConnection().sendCommand("trigger TogglePartyBossbar");

        if (attempts >= MAX_ATTEMPTS) {
            failsafeUntilMs = now + FAILSAFE_MS;
            attempts = 0;
        }
    }
}
