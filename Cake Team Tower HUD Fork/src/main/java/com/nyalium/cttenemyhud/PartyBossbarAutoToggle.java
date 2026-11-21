package com.nyalium.cttenemyhud;

import net.minecraft.client.Minecraft;

/**
 * 自动把「队伍血量条」打开（队友血量的唯一客户端数据源）。
 *
 * <p>数据包 {@code spectator_bossbar} 默认是关的，需要 {@code /trigger TogglePartyBossbar}。
 * 发命令太勤会被服务端限流甚至踢人，所以：</p>
 * <ul>
 *   <li>只在<b>真正对局中</b>尝试（看得到自己的个人状态条）；</li>
 *   <li>队伍条一出现就停手，并记住「这局已经开过」——玩家之后自己关掉就不再干预；</li>
 *   <li>一个窗口最多 {@value #MAX_ATTEMPTS} 次，用完等 {@value #RETRY_WINDOW_MS} ms 再开新窗口；</li>
 *   <li>两次尝试之间至少 {@value #COOLDOWN_MS} ms。</li>
 * </ul>
 */
final class PartyBossbarAutoToggle {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_WINDOW_MS = 15_000L;
    private static final long COOLDOWN_MS = 3_000L;

    private static int attempts = 0;
    private static long lastTryMs = 0L;
    private static long retryAfterMs = 0L;
    private static boolean wasInGame = false;
    private static boolean seenTeamBar = false;

    private PartyBossbarAutoToggle() {}

    static void tick(Minecraft mc, boolean teamBarPresent, boolean personalBarPresent) {
        if (teamBarPresent) {
            seenTeamBar = true;
            attempts = 0;
            retryAfterMs = 0L;
            wasInGame = personalBarPresent;
            return;
        }

        // 不在对局：清空状态，不消耗尝试次数。
        if (!personalBarPresent) {
            attempts = 0;
            retryAfterMs = 0L;
            seenTeamBar = false;
            wasInGame = false;
            return;
        }

        // 刚进对局 → 立刻允许尝试，保证一开打就能看到 HUD。
        if (!wasInGame) {
            attempts = 0;
            retryAfterMs = 0L;
            seenTeamBar = false;
            wasInGame = true;
        }

        // 这一局见过又消失 → 玩家自己关的，不再自动开。
        if (seenTeamBar) return;

        long now = System.currentTimeMillis();
        if (now < retryAfterMs) return;
        if (attempts >= MAX_ATTEMPTS) {
            attempts = 0;
            retryAfterMs = now + RETRY_WINDOW_MS;
            return;
        }
        if (now - lastTryMs < COOLDOWN_MS) return;
        if (mc.getConnection() == null) return;

        lastTryMs = now;
        attempts++;
        mc.getConnection().sendCommand("trigger TogglePartyBossbar");
    }

    static void reset() {
        attempts = 0;
        lastTryMs = 0L;
        retryAfterMs = 0L;
        wasInGame = false;
        seenTeamBar = false;
    }
}
