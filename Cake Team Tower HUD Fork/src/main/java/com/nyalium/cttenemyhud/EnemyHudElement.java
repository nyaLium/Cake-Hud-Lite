package com.nyalium.cttenemyhud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 敌人血条 HUD（屏幕空间）：只在实体名牌正上方显示血条、心数与护甲，名称本身也由本 Mod 绘制。
 *
 * <pre>
 *     ❤120 | 💛40 | 🛡3 🔥5      心数 + 护甲
 *     ▬▬▬▬▬▬ 原版 bossbar 纹理 ▬▬▬▬▬▬
 *     冰霜狼                       与原版逐像素同款（0.025 比例 + 原生背景框）
 * </pre>
 *
 * <h2>数据来源（纯客户端）</h2>
 * <ul>
 *   <li>精确值：客户端 Boss 栏 {@code targetting} 标题里的 {@code 名字 (HP 当前/上限) 🛡防御}
 *       与这条 bar 的填充比例 / 颜色；</li>
 *   <li>兜底：{@code below_name} 展示槽的 {@code HealthPercent} 百分比。</li>
 * </ul>
 *
 * <p>「未知诅咒」期间与地图设定一致，整个 UI 隐藏（{@link CurseDetector}）。</p>
 */
public final class EnemyHudElement implements HudElement {

    /** 原版名牌的世界缩放：1 个「名牌像素」= 0.025 格。 */
    private static final double NAMETAG_SCALE = 0.025;

    private static final double MIN_UNIT = 0.18;
    private static final double MAX_UNIT = 6.0;

    private static final class Placement {
        EnemyData.Snapshot data;
        double gx;
        double gy;
        double ppu;
        /** true = 队友（绿色条、无护甲）。 */
        boolean player;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled) return;
        // 诅咒：与地图设定一致，整个 UI 隐藏；按需求不提供开关。
        if (CurseDetector.isActive(mc)) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        Matrix4f viewProj = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Vector3fc left = camera.leftVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        float partialTick = camera.getCameraEntityPartialTicks(deltaTracker);

        int guiW = g.guiWidth();
        int guiH = g.guiHeight();
        double margin = 120.0;

        Scoreboard sb = mc.level.getScoreboard();
        Map<UUID, BossBarTracker.Bar> exact = BossBarTracker.resolve(mc, BossBarTracker.snapshot(mc));

        // 队友：数据来自「队伍血量」这条 Boss 栏。
        // 那条 bar 默认是关的，需要 /trigger TogglePartyBossbar 打开（数据包里的真开关），
        // 所以这里带上冷却 / 熔断 / 防误关的自动开启（见 PartyBossbarAutoToggle）。
        if (cfg.showTeammates && cfg.autoPartyBossbar) {
            PartyBossbarAutoToggle.tick(mc, BossBarTracker.hasTeamBar(mc),
                    BossBarTracker.hasPersonalBar(mc));
        }
        Map<UUID, TeamBarTitle.Entry> teammates = cfg.showTeammates
                ? BossBarTracker.resolveTeam(mc, BossBarTracker.teamEntriesCached(mc))
                : Map.of();

        List<Placement> placements = new ArrayList<>();
        double[] pt = new double[2];
        double[] ptRight = new double[2];
        int scanned = 0;
        int withData = 0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            scanned++;
            if (entity instanceof Player || entity instanceof ArmorStand) continue;
            if (!HudTarget.visible(mc, entity)) continue;

            EnemyData.Snapshot data = EnemyData.read(sb, entity);
            if (data == null) continue;   // HudTarget 已保证有数据，这里只是保险

            // ① 服务端选装组件：按 UUID 的权威数据（每只怪都有绝对值 + 四色心）。
            MobHealthPayload.Entry pushed =
                    cfg.useServerData ? ClientMobHealthCache.lookup(entity.getUUID()) : null;
            if (pushed != null) {
                data = EnemyData.fromPayload(pushed.hp(), pushed.maxHp(),
                        pushed.soul(), pushed.black(), pushed.blue(), pushed.cracked(),
                        pushed.maxSoul(), pushed.maxBlack(), pushed.maxBlue(),
                        pushed.defence(), pushed.armour());
                if (pushed.targetted()) {
                    // 被锁定的那只跟着数据包的护甲轮换走（ArmourCache 才有最新一档）
                    ArmourCache.observe(entity.getUUID(), fullArmourSlots(pushed));
                }
            } else {
                // ② 纯客户端：客户端 Boss 栏那条「最近敌人」条 + ArmourCache 轮换累积。
                BossBarTracker.Bar bar = exact.get(entity.getUUID());
                if (bar != null) {
                    int[] slots = ArmourCache.snapshot(entity.getUUID());
                    int[] elements = new int[EnemyData.ARMOR_OBJECTIVES.length];
                    System.arraycopy(slots, 1, elements, 0, elements.length);
                    data = EnemyData.exact(bar.hp, bar.maxHp, bar.progress, slots[0], elements,
                            HeartCounts.forMob(bar.hp, bar.maxHp));
                    data.kind = bar.kind;
                }
            }
            withData++;

            Vec3 pos = entity.getPosition(partialTick);
            Vec3 anchor = HudTarget.nameTagAnchor(entity, pos, partialTick);
            if (!project(viewProj, camPos, anchor, guiW, guiH, pt)) continue;
            if (!project(viewProj, camPos, anchor.add(right), guiW, guiH, ptRight)) continue;
            double ppu = ptRight[0] - pt[0];
            if (!(ppu > 1.0e-3)) continue;
            if (pt[0] < -margin || pt[0] > guiW + margin || pt[1] < -margin || pt[1] > guiH + margin) continue;

            Placement p = new Placement();
            p.data = data;
            p.gx = pt[0];
            p.gy = pt[1];
            p.ppu = ppu;
            placements.add(p);
        }

        // 队友：绿色条，自己永远不画（第一/第三人称都看不到自己的条）。
        int mateCount = 0;
        if (!teammates.isEmpty()) {
            int[] noArmour = new int[EnemyData.ARMOR_OBJECTIVES.length];
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof Player)) continue;
                if (entity == mc.player) continue;
                TeamBarTitle.Entry entry = teammates.get(entity.getUUID());
                if (entry == null) continue;
                if (!HudTarget.visible(mc, entity)) continue;

                Vec3 pos = entity.getPosition(partialTick);
                Vec3 anchor = HudTarget.nameTagAnchor(entity, pos, partialTick);
                if (!project(viewProj, camPos, anchor, guiW, guiH, pt)) continue;
                if (!project(viewProj, camPos, anchor.add(right), guiW, guiH, ptRight)) continue;
                double ppu = ptRight[0] - pt[0];
                if (!(ppu > 1.0e-3)) continue;
                if (pt[0] < -margin || pt[0] > guiW + margin || pt[1] < -margin || pt[1] > guiH + margin) continue;

                Placement p = new Placement();
                // 队友：只有 AllHearts/MaxHP，拆不出四色心 → 心数未知，这一行退回「当前/最大」。
                p.data = EnemyData.exact(entry.hp, entry.maxHp, entry.ratio(), 0, noArmour,
                        HeartCounts.unknown());
                p.gx = pt[0];
                p.gy = pt[1];
                p.ppu = ppu;
                p.player = true;
                placements.add(p);
                mateCount++;
            }
        }

        if (cfg.debug) {
            g.text(mc.font, "CTT EnemyHUD  entities=" + scanned + " withData=" + withData
                            + " mates=" + mateCount + " drawn=" + placements.size()
                            + (placements.isEmpty() ? "" : "  " + (placements.get(0).data.percentOnly ? "percent" : "absolute")),
                    4, 4, 0xFFFFFF00, true);
        }

        if (placements.isEmpty()) return;
        placements.sort((a, b) -> Double.compare(a.ppu, b.ppu));

        Font font = mc.font;
        int alpha = (int) Math.round(255.0 * cfg.opacity);
        EnemyHudLayout.Options options = options(cfg);
        EnemyHudLayout.Options teamOptions = teamOptions(cfg);

        for (Placement p : placements) {
            drawOne(g, font, p, p.player ? teamOptions : options, alpha);
        }
    }

    /**
     * 玩家血条用独立的一套长度/缩放/偏移。
     *
     * <p>队友没有护甲数据、也拆不出四色心，所以这一行固定是「当前/最大」。</p>
     */
    private static EnemyHudLayout.Options teamOptions(EnemyHudConfig.Cfg cfg) {
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showNumbers = true;
        o.showHearts = false;
        o.showArmor = false;
        o.showBar = cfg.showBar;
        o.barWidth = cfg.teamBarWidth;
        o.verticalOffset = cfg.teamVerticalOffset;
        o.textShadow = cfg.textShadow;
        o.scale = cfg.teamScale;
        return o;
    }

    /** 敌人：心数、护甲永久显示，只有血条可关。 */
    private static EnemyHudLayout.Options options(EnemyHudConfig.Cfg cfg) {
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showNumbers = true;
        o.showHearts = true;
        o.showArmor = true;
        o.showBar = cfg.showBar;
        o.barWidth = cfg.barWidth;
        o.verticalOffset = cfg.verticalOffset;
        o.textShadow = cfg.textShadow;
        o.scale = cfg.scale;
        return o;
    }

    private static void drawOne(GuiGraphicsExtractor g, Font font, Placement p,
                                EnemyHudLayout.Options options, int alpha) {
        double unit = NAMETAG_SCALE * p.ppu * options.scale;
        boolean drawHud = unit >= MIN_UNIT;
        if (drawHud) unit = Math.min(unit, MAX_UNIT);

        Matrix3x2fStack pose = g.pose();
        if (drawHud) {
            EnemyHudLayout.Result layout = EnemyHudLayout.compute(
                    p.data.hp, p.data.maxHp, p.data.percentOnly,
                    p.data.defence, p.data.armor, p.data.hearts,
                    options, font::width);

            if (!layout.isEmpty()) {
                pose.pushMatrix();
                pose.translate((float) p.gx, (float) p.gy);
                pose.scale((float) unit, (float) unit);

                if (layout.hasText) {
                    int cursor = -layout.textWidth / 2;
                    for (EnemyHudLayout.Line line : layout.lines) {
                        g.text(font, line.text, cursor, layout.textTop,
                                withAlpha(line.rgb, alpha), options.textShadow);
                        cursor += font.width(line.text);
                    }
                }

                if (layout.barWidth > 0) {
                    pose.pushMatrix();
                    pose.translate(0.0f, layout.barTop);
                    pose.scale(layout.barWidth / (float) BossBarTexture.NATIVE_WIDTH, 1.0f);
                    int bx = -BossBarTexture.NATIVE_WIDTH / 2;
                    int white = withAlpha(0xFFFFFF, alpha);
                    // 敌人与队友走同一条绘制路径，差别只有 tint（都来自原版 boss_bar 纹理）。
                    BossBarTexture.Tint tint = tintOf(p);
                    BossBarTexture.drawBackground(g, bx, 0, BossBarTexture.NATIVE_WIDTH,
                            EnemyHudLayout.BAR_HEIGHT, white, tint);
                    int fill = Math.round(BossBarTexture.NATIVE_WIDTH * p.data.ratio);
                    fill = Math.max(0, Math.min(BossBarTexture.NATIVE_WIDTH, fill));
                    BossBarTexture.drawProgress(g, bx, 0, fill,
                            EnemyHudLayout.BAR_HEIGHT, white, tint);
                    pose.popMatrix();
                }

                pose.popMatrix();
            }
        }
    }

    /** 队友固定绿条；敌人按当前心类取色。 */
    private static BossBarTexture.Tint tintOf(Placement p) {
        if (p.player) return BossBarTexture.Tint.GREEN;
        return BossBarTexture.tintFor(p.data.kind);
    }

    /** 把服务端推来的 7 档元素护甲还原成 {@link ArmourCache} 的 8 槽格式（0=防御）。 */
    private static int[] fullArmourSlots(MobHealthPayload.Entry entry) {
        int[] slots = new int[ArmourCache.SLOTS];
        slots[0] = entry.defence();
        int[] armour = entry.armour();
        if (armour != null) {
            System.arraycopy(armour, 0, slots, 1, Math.min(armour.length, slots.length - 1));
        }
        return slots;
    }

    // ── 工具 ──

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static boolean project(Matrix4f viewProj, Vec3 camPos, Vec3 world,
                                   int guiW, int guiH, double[] out) {
        Vector4f v = new Vector4f(
                (float) (world.x - camPos.x),
                (float) (world.y - camPos.y),
                (float) (world.z - camPos.z),
                1.0f);
        viewProj.transform(v);
        if (v.w() < 0.3f) return false;
        double ndcX = v.x() / v.w();
        double ndcY = v.y() / v.w();
        if (!Double.isFinite(ndcX) || !Double.isFinite(ndcY)) return false;
        out[0] = (ndcX * 0.5 + 0.5) * guiW;
        out[1] = (0.5 - ndcY * 0.5) * guiH;
        return true;
    }
}
