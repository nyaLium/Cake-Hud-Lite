package com.nyalium.ctthudfork;

import com.nyalium.ctthudfork.mixin.BossHealthOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 从<b>客户端 Boss 栏</b>读「精确血量 + 护甲」，并把这条 bar 认领到具体实体上。
 *
 * <h2>纯客户端唯一能拿到绝对值的通道</h2>
 * <p>计分板里 {@code AllHearts} / {@code MaxHP} / {@code Defence} 这些 objective 没上展示槽，
 * 原版不会同步给客户端；但 {@code ClientboundBossEventPacket} 是<b>逐玩家</b>下发的，
 * 服务端建了什么 bar 客户端就有什么。数据包 {@code bossbars/targetting_enemy.mcfunction}
 * 恰好把 {@code 名字 (HP 当前/上限) 🛡防御 [元素护甲]} 拼进了 {@code minecraft:targetting}
 * 的标题，所以这段标题就是权威数值。</p>
 *
 * <h2>局限</h2>
 * <p>数据包同一时刻只维护一条这样的 bar，所以<b>只有被地图锁定的那只敌人</b>能拿到精确值；
 * 其余敌人退回 {@code below_name} 的 {@code HealthPercent} 百分比。</p>
 *
 * <h2>护甲是轮换的</h2>
 * <p>同一帧的后缀里只带一种元素护甲（{@code ArmorDisplay} 每 40 tick 进一档），
 * 所以按实体累积到 {@link ArmourCache}，超过一个轮换周期没再出现才作废。</p>
 */
public final class BossBarTracker {

    /** 一条可解析的怪物 bar。 */
    static final class Bar {
        /** bossbar 自身的 UUID，用于「绑定到实体」的稳定性（同名怪物之间不来回跳）。 */
        final UUID id;
        final String name;
        /** 标题里的 {@code AllHearts}（全部心的总量）。 */
        final int hp;
        /** 标题里的 {@code MaxHP}（红心上限）。 */
        final int maxHp;
        /** 本帧解析到的护甲段，下标同 {@link ArmourCache}：0=防御，1..7=元素。 */
        final int[] armour;
        /**
         * bar 自身的填充比例（{@code bossbar set ... value/max} 换算出来的 progress）。
         * 数据包按「当前这一类心」设 value/max，所以它就是这一类心的剩余比例。
         */
        final float progress;
        /**
         * 这条 bar 当前表现的是哪一类心。数据包用颜色表达通道
         * （{@code targetting_enemy.mcfunction}：蓝 = 红心、黄 = 魂心）。
         */
        final HeartCounts.Kind kind;

        Bar(UUID id, String name, int hp, int maxHp, int[] armour, float progress, HeartCounts.Kind kind) {
            this.id = id;
            this.name = name;
            this.hp = hp;
            this.maxHp = maxHp;
            this.armour = armour;
            this.progress = progress;
            this.kind = kind;
        }

        /**
         * 这条 bar 对应的「红心百分比」。
         *
         * <p>数据包 {@code misc/health.mcfunction:102-104}：
         * {@code HealthPercent = min(RedHearts, MaxHP) * 100 / MaxHP}（整数除法，截断）。
         * 标题里左数就是 {@code AllHearts}，所以 {@code 红心 = min(AllHearts, MaxHP)}。</p>
         *
         * @return 0~100；算不出来时 -1
         */
        int expectedPercent() {
            if (maxHp <= 0) return -1;
            int red = Math.max(0, Math.min(hp, maxHp));
            return Math.min(100, (int) ((long) red * 100L / maxHp));
        }
    }

    private static final int SLOT_SHIELD = 0x1F6E1; // 🛡
    private static final int SLOT_FIRE = 0x1F525;   // 🔥
    private static final int SLOT_ICE = 0x2603;     // ☃
    private static final int SLOT_DARK = 0x2620;    // ☠
    private static final int SLOT_LIGHT = 0x2600;   // ☀
    private static final int SLOT_WATER = 0x2693;   // ⚓
    private static final int SLOT_ELECTRIC = 0x26A1;// ⚡

    private BossBarTracker() {}

    /** 每帧取一次当前客户端所有「怪物血量条」。 */
    static List<Bar> snapshot(Minecraft mc) {
        if (mc.gui == null || mc.gui.hud == null) return List.of();
        BossHealthOverlay overlay = mc.gui.hud.getBossOverlay();
        if (overlay == null) return List.of();

        Map<UUID, BossEvent> bars;
        try {
            bars = ((BossHealthOverlayAccessor) (Object) overlay).cttHudFork$getBossBars();
        } catch (Throwable t) {
            return List.of();
        }
        if (bars == null || bars.isEmpty()) return List.of();

        String playerName = mc.player != null ? mc.player.getName().getString() : "";
        List<Bar> out = new ArrayList<>(2);
        for (BossEvent bar : bars.values()) {
            Component component = bar.getName();
            if (component == null) continue;
            String text = component.getString();

            // 玩家自己 / 队伍状态条不是怪物条
            if (BossBarTitle.looksLikeTeamBar(text)) continue;
            if (!playerName.isEmpty() && text.contains(playerName)) continue;

            BossBarTitle title = BossBarTitle.parse(text);
            if (title == null) continue;
            out.add(new Bar(bar.getId(), title.name, title.hp, title.maxHp, parseArmour(component),
                    clampProgress(bar.getProgress()), kindOf(bar.getColor())));
        }
        return out;
    }

    /**
     * 数据包的心通道 → 心类。
     *
     * <p>敌人条（{@code targetting} / {@code targetting_100N}）里
     * {@code HeartValue=1} 是红心（颜色设为 {@code blue}）、{@code HeartValue=2}
     * 是魂心（颜色 {@code yellow}）；敌人不会有黑心 / 蓝心，所以只需要这两条映射。</p>
     */
    static HeartCounts.Kind kindOf(BossEvent.BossBarColor color) {
        if (color == null) return HeartCounts.Kind.UNKNOWN;
        return switch (color) {
            case BLUE -> HeartCounts.Kind.RED;
            case YELLOW -> HeartCounts.Kind.SOUL;
            case PURPLE -> HeartCounts.Kind.BLACK;
            case RED, PINK -> HeartCounts.Kind.RED;
            default -> HeartCounts.Kind.UNKNOWN;
        };
    }

    private static float clampProgress(float progress) {
        if (Float.isNaN(progress)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    /** bar UUID → 实体 UUID。绑上之后不再每帧重新挑，避免同名怪物之间来回跳。 */
    private static final Map<UUID, UUID> BINDINGS = new HashMap<>();

    /**
     * 认领时允许的百分比误差（百分点）。超过这个误差就**不认领**：
     * 宁可让这只怪退回百分比显示，也不能把别的怪的数字挂到它头上。
     *
     * <p>取 5 是因为 {@code HealthPercent} 与 bossbar 标题的 {@code (x/y)} 是两条独立同步的
     * 数据，极端时能差一两个 tick（1~2 个百分点）；5 已经留足余量，又能挡住「两只同名怪
     * 血量差很多」的情况。</p>
     */
    static final int PERCENT_TOLERANCE = 5;

    /** 两个百分比的差值；任一未知返回 {@link Integer#MAX_VALUE}。 */
    static int percentDiff(int expected, int percent) {
        if (expected < 0 || percent < 0) return Integer.MAX_VALUE;
        return Math.abs(expected - percent);
    }

    /** 是否值得把这条 bar 认给这只怪（差值在容差内）。 */
    static boolean acceptable(int diff) {
        return diff <= PERCENT_TOLERANCE;
    }

    /**
     * 认领打分（越大越优先）。
     *
     * <p>1.20.9 起：百分比吻合是**硬门槛**（{@link #acceptable}），门槛内再按
     * 精确吻合 &gt; 差 ≤2 &gt; 差 ≤容差 排序。</p>
     *
     * <p>1.20.10 起：同一档里先看<b>与玩家的距离</b>（越近越优先），再给上一帧的绑定一个
     * {@link #STICKY_BONUS} 的粘性奖励。数据包 {@code targetting_enemy.mcfunction} 的选法是
     * 「在随机一名 CTT 玩家里取离他最近的 E」，所以距离是很强的先验；粘性奖励只够抵消
     * 2 格内的抖动，超过就让更近的那只接手，避免「数字一直挂在远处那只同名怪身上」。</p>
     */
    static double matchScore(int diff, boolean exactName, boolean sticky, double distanceSq) {
        double score = 0.0;
        if (diff == 0) score += 1.0e9;
        else if (diff <= 2) score += 5.0e8;
        else if (diff <= PERCENT_TOLERANCE) score += 1.0e8;
        if (exactName) score += 1.0e7;
        score -= distanceSq;
        if (sticky) score += STICKY_BONUS;
        return score;
    }

    /**
     * 粘性奖励（单位 = 格²）：等于「2 格」。上一帧绑定的那只只要在最近候选的 2 格以内就继续用，
     * 超过就换成更近的那只。
     */
    static final double STICKY_BONUS = 4.0;

    /**
     * 把每条 bar 认领到唯一一只实体。
     *
     * <h2>同名怪物怎么办</h2>
     * <p>数据包那条 {@code targetting} 标题里只有实体<b>名字</b>，没有 UUID，所以名字本身区分不了
     * 「Stone Zombie ×5」。能区分的是<b>{@code below_name} 的 {@code HealthPercent}</b>：
     * 只有真正被锁定的那只，它的百分比才会等于 bar 里的
     * {@code min(AllHearts, MaxHP) * 100 / MaxHP}（见 {@link Bar#expectedPercent()}）。</p>
     *
     * <p>打分（从强到弱）：</p>
     * <ol>
     *   <li>百分比精确吻合 → 基本可以确定就是它；</li>
     *   <li>百分比差 ≤ 2（同步差一两个 tick）→ 次之；</li>
     *   <li>百分比差 ≤ {@link #PERCENT_TOLERANCE} → 再次之；差超过容差的候选<b>直接不参与</b>；</li>
     *   <li>名字完全相同优于「互相包含」；</li>
     *   <li><b>与玩家的距离</b>（越近越优先）—— 数据包取的是「离某名 CTT 玩家最近的 E」；</li>
     *   <li>上一帧的绑定给 {@link #STICKY_BONUS}（≈2 格²）的粘性奖励，只压住小抖动。</li>
     * </ol>
     * <p>命中后顺手把本帧的护甲段喂给 {@link ArmourCache}。</p>
     */
    static Map<UUID, Bar> resolve(Minecraft mc, List<Bar> bars) {
        Map<UUID, Bar> out = new HashMap<>();
        if (bars.isEmpty() || mc.level == null || mc.player == null) return out;

        Scoreboard sb = mc.level.getScoreboard();
        Vec3 playerPos = mc.player.position();
        List<Entity> candidates = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player || entity instanceof ArmorStand) continue;
            if (!entity.isAlive() || entity.isInvisible()) continue;
            candidates.add(entity);
        }
        if (candidates.isEmpty()) {
            BINDINGS.clear();
            return out;
        }

        // 清掉已经消失的 bar 的绑定，避免无限增长。
        Set<UUID> liveBarIds = new HashSet<>();
        for (Bar bar : bars) liveBarIds.add(bar.id);
        BINDINGS.keySet().retainAll(liveBarIds);

        for (Bar bar : bars) {
            int expected = bar.expectedPercent();
            UUID bound = BINDINGS.get(bar.id);

            Entity best = null;
            double bestScore = -Double.MAX_VALUE;
            for (Entity entity : candidates) {
                boolean exact = nameMatches(entity, bar.name, true);
                if (!exact && !nameMatches(entity, bar.name, false)) continue;

                int percent = EnemyData.healthPercentOf(sb, entity);
                int diff = percentDiff(expected, percent);
                if (!acceptable(diff)) continue;   // 血量对不上：不认领，宁可显示百分比

                double score = matchScore(diff, exact, bound != null && bound.equals(entity.getUUID()),
                        entity.position().distanceToSqr(playerPos));
                if (score > bestScore) {
                    bestScore = score;
                    best = entity;
                }
            }
            if (best != null) {
                BINDINGS.put(bar.id, best.getUUID());
                out.put(best.getUUID(), bar);
                ArmourCache.observe(best.getUUID(), bar.armour);
            } else {
                BINDINGS.remove(bar.id);
            }
        }
        return out;
    }
    // ── 名字匹配 ──


    // ── 队伍条（队友血量） ──

    /** 队伍血量条是否已经在客户端上（只读判定）。 */
    static boolean hasTeamBar(Minecraft mc) {
        return !teamEntriesCached(mc).isEmpty();
    }

    /**
     * 是否存在「自己」的血量条 —— 用来判断玩家是否已进入对局。
     *
     * <p>数据包给每名参赛玩家一条 {@code player_100N}（标题里含自己名字 + {@code (x/y)}），
     * 大厅里没有。自动开队伍条必须以此为前提，否则会在菜单里白发命令。</p>
     */
    static boolean hasPersonalBar(Minecraft mc) {
        if (mc.gui == null || mc.gui.hud == null || mc.player == null) return false;
        BossHealthOverlay overlay = mc.gui.hud.getBossOverlay();
        if (overlay == null) return false;
        Map<UUID, BossEvent> bars;
        try {
            bars = ((BossHealthOverlayAccessor) (Object) overlay).cttHudFork$getBossBars();
        } catch (Throwable t) {
            return false;
        }
        if (bars == null || bars.isEmpty()) return false;
        String playerName = mc.player.getName().getString();
        if (playerName.isEmpty()) return false;
        for (BossEvent bar : bars.values()) {
            Component component = bar.getName();
            if (component == null) continue;
            String text = component.getString();
            if (text.contains(playerName) && BossBarTitle.parse(text) != null) return true;
        }
        return false;
    }

    /** 从客户端 Boss 栏里找那条队伍血量条并解析出每个玩家的 当前/上限。 */
    static List<TeamBarTitle.Entry> teamEntries(Minecraft mc) {
        if (mc.gui == null || mc.gui.hud == null) return List.of();
        BossHealthOverlay overlay = mc.gui.hud.getBossOverlay();
        if (overlay == null) return List.of();

        Map<UUID, BossEvent> bars;
        try {
            bars = ((BossHealthOverlayAccessor) (Object) overlay).cttHudFork$getBossBars();
        } catch (Throwable t) {
            return List.of();
        }
        if (bars == null || bars.isEmpty()) return List.of();

        String playerName = mc.player != null ? mc.player.getName().getString() : "";
        for (BossEvent bar : bars.values()) {
            Component component = bar.getName();
            if (component == null) continue;
            String text = component.getString();
            if (BossBarTitle.isEnemyBarText(text, playerName)) continue;
            // 严格整串解析；失败就宽松扫一遍（标题里夹着空槽残渣时仍能救回来）。
            List<TeamBarTitle.Entry> strict = TeamBarTitle.parse(text);
            if (!strict.isEmpty() && matchesAnyPlayer(mc, strict)) return strict;
            List<TeamBarTitle.Entry> loose = TeamBarTitle.scanLoose(text);
            if (!loose.isEmpty() && matchesAnyPlayer(mc, loose)) return loose;
        }
        return List.of();
    }

    /**
     * 这条标题是不是「队员血量条」。
     *
     * <p>{@link TeamBarTitle} 只做形状匹配，而数据包的 {@code floor} 条
     * （{@code 高塔 (12/50)}、{@code 休息室 (3/50)} …，见
     * {@code floors/scenes/break_room_universal.mcfunction:397-402}）形状一模一样，
     * 只按形状判定会把「高塔层数」整条隐藏掉。所以这里再要求
     * <b>至少有一段名字能匹配到真实玩家</b>。</p>
     *
     * <p>名字的匹配来源有三个：本地玩家、视野内玩家实体、以及<b>玩家列表（Tab 名单）</b>。
     * 最后一个很关键 —— 队伍条会列出不在渲染距离内的队友，只靠实体匹配会时灵时不灵，
     * 表现就是「顶部那条队伍血量条时不时闪一下」。</p>
     */
    public static boolean looksLikeTeamBar(Minecraft mc, String text) {
        if (text == null || text.isEmpty()) return false;
        List<TeamBarTitle.Entry> strict = TeamBarTitle.parse(text);
        if (!strict.isEmpty() && matchesAnyPlayer(mc, strict)) return true;
        List<TeamBarTitle.Entry> loose = TeamBarTitle.scanLoose(text);
        return !loose.isEmpty() && matchesAnyPlayer(mc, loose);
    }

    private static boolean matchesAnyPlayer(Minecraft mc, List<TeamBarTitle.Entry> entries) {
        if (mc == null || entries.isEmpty()) return false;
        for (TeamBarTitle.Entry entry : entries) {
            if (matchesPlayerName(mc, entry.name)) return true;
        }
        return false;
    }

    private static boolean matchesPlayerName(Minecraft mc, String name) {
        if (name == null || name.isEmpty()) return false;
        if (mc.player != null && nameMatches(mc.player, name, false)) return true;
        if (mc.level != null) {
            for (Player player : mc.level.players()) {
                if (nameMatches(player, name, false)) return true;
            }
        }
        // Tab 名单里的玩家（不在渲染距离内也算队友）。
        var connection = mc.getConnection();
        return connection != null && connection.getPlayerInfo(name) != null;
    }

    /** 队伍条解析结果的短缓存（{@link #isTeammate} 每帧会被问很多次）。 */
    private static List<TeamBarTitle.Entry> cachedTeamEntries = null;
    private static long cachedTeamAtMs = 0L;
    private static final long TEAM_CACHE_MS = 200L;

    /** 带短缓存的 {@link #teamEntries}。 */
    static synchronized List<TeamBarTitle.Entry> teamEntriesCached(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (cachedTeamEntries != null && now - cachedTeamAtMs < TEAM_CACHE_MS) {
            return cachedTeamEntries;
        }
        cachedTeamEntries = teamEntries(mc);
        cachedTeamAtMs = now;
        return cachedTeamEntries;
    }

    /** 这名玩家是否在队伍条里（= 是不是队友）。 */
    static boolean isTeammate(Minecraft mc, Player player) {
        if (player == null) return false;
        for (TeamBarTitle.Entry entry : teamEntriesCached(mc)) {
            if (nameMatches(player, entry.name, true)) return true;
        }
        for (TeamBarTitle.Entry entry : teamEntriesCached(mc)) {
            if (nameMatches(player, entry.name, false)) return true;
        }
        return false;
    }

    /** 把队伍条目认领到附近的同名玩家实体（自己永不参与）。 */
    static Map<UUID, TeamBarTitle.Entry> resolveTeam(Minecraft mc, List<TeamBarTitle.Entry> entries) {
        Map<UUID, TeamBarTitle.Entry> out = new HashMap<>();
        if (entries.isEmpty() || mc.level == null || mc.player == null) return out;

        List<Player> candidates = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != mc.player && player.isAlive()) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) return out;

        Vec3 self = mc.player.position();
        for (TeamBarTitle.Entry entry : entries) {
            for (int pass = 0; pass < 2; pass++) {
                Player best = null;
                double bestDist = Double.MAX_VALUE;
                for (Player player : candidates) {
                    if (!nameMatches(player, entry.name, pass == 0)) continue;
                    double dist = player.position().distanceToSqr(self);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = player;
                    }
                }
                if (best != null) {
                    out.put(best.getUUID(), entry);
                    break;
                }
            }
        }
        return out;
    }

    private static boolean nameMatches(Entity entity, String barName, boolean exact) {
        if (barName.isEmpty()) return false;
        for (String candidate : nameCandidates(entity)) {
            if (candidate == null || candidate.isEmpty()) continue;
            if (exact) {
                if (candidate.equalsIgnoreCase(barName)) return true;
            } else if (candidate.contains(barName) || barName.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 一只实体可能被叫成哪些名字：客户端本地化后的显示名，以及可翻译名字的**原始 key**
     * （数据包用 {@code {translate:"Skeleton Warrior"}} 起名，key 本身就是服务端下发的英文名）。
     */
    private static List<String> nameCandidates(Entity entity) {
        List<String> out = new ArrayList<>(4);
        addCandidate(out, entity.getName());
        Component display = entity.getDisplayName();
        if (display != null) addCandidate(out, display);
        return out;
    }

    private static void addCandidate(List<String> out, Component component) {
        if (component == null) return;
        String localized = component.getString();
        if (localized != null && !localized.isEmpty()) out.add(localized.trim());
        // 数据包用 {translate:"Skeleton Warrior"} 起名，key 本身就是服务端下发的英文名；
        // 26.3 的 TranslatableContents#getKey() 是公开方法，直接取即可。
        if (component.getContents() instanceof TranslatableContents ttc) {
            String key = ttc.getKey();
            if (key != null && !key.isEmpty()) out.add(key.trim());
        }
    }

    // ── 护甲段解析 ──

    /**
     * 从整条标题里扫出「图标 + 数字」段。
     *
     * <p>不用正则：Java 的字符类处理代理对（🛡🔥 等 4 字节 emoji）会踩坑，
     * 参考 Mod 就因此让两种 🛡 全都不显示。这里按码点线性扫描。</p>
     */
    static int[] parseArmour(Component title) {
        int[] slots = new int[ArmourCache.SLOTS];
        if (title == null) return slots;

        StringBuilder flat = new StringBuilder();
        List<Integer> colors = new ArrayList<>();
        try {
            title.visit((style, text) -> {
                int color = resolveColor(style);
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (isInvisibleMark(c)) continue;
                    flat.append(c);
                    colors.add(color);
                }
                return Optional.empty();
            }, Style.EMPTY);
        } catch (Throwable t) {
            return slots;
        }

        String s = flat.toString();
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int slot = slotOf(cp, i < colors.size() ? colors.get(i) : 0xFFFFFF);
            if (slot < 0) {
                i += Character.charCount(cp);
                continue;
            }
            int j = i + Character.charCount(cp);
            while (j < s.length() && s.charAt(j) == ' ') j++;
            int start = j;
            if (j < s.length() && (s.charAt(j) == '-' || s.charAt(j) == '+')) j++;
            while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
            if (j > start) {
                try {
                    slots[slot] = Integer.parseInt(s.substring(start, j));
                } catch (NumberFormatException ignored) {
                    // 数字过长等异常：跳过这一段
                }
            }
            i = Math.max(j, i + Character.charCount(cp));
        }
        return slots;
    }

    /** ⚓ 是 BMP 内符号，☃☠☀⚡ 也是；🛡🔥 是代理对，这里统一按码点比较。 */
    private static int slotOf(int codePoint, int rgb) {
        if (codePoint == SLOT_SHIELD) {
            // 蓝🛡 = Defence，白🛡 = TrueArmor（数据包靠颜色区分同图标）
            int r = (rgb >> 16) & 0xFF;
            int b = rgb & 0xFF;
            return b > r ? 0 : 5;
        }
        switch (codePoint) {
            case SLOT_FIRE:
                return 1;
            case SLOT_ICE:
                return 2;
            case SLOT_DARK:
                return 3;
            case SLOT_LIGHT:
                return 4;
            case SLOT_WATER:
                return 6;
            case SLOT_ELECTRIC:
                return 7;
            default:
                return -1;
        }
    }

    private static int resolveColor(Style style) {
        TextColor color = style == null ? null : style.getColor();
        return color == null ? 0xFFFFFF : (color.getValue() & 0xFFFFFF);
    }

    private static boolean isInvisibleMark(char c) {
        return c == '\uFE0E' || c == '\uFE0F'
                || c == '\u200B' || c == '\u200C' || c == '\u200D'
                || c == '\u2060' || c == '\uFEFF';
    }
}
