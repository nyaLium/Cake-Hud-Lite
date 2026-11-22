package com.nyalium.cttenemyhud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;

import java.util.Map;
import java.util.UUID;

/**
 * 头顶血条的<b>世界空间</b>实现——完全照抄原版名牌那条路。
 *
 * <h2>为什么改成世界空间</h2>
 * <p>原版名字是<b>深度测试</b>的：它和模型、方块一起提交，被墙挡住就被裁掉，
 * 被更近的怪挡住就被更近的模型盖住。之前本 Mod 的量具画在 GUI 层（屏幕空间，无深度），
 * 所以后方的条会压住前面的名字和模型，只能靠射线 / 屏幕矩形覆盖率去「猜」遮挡。</p>
 *
 * <p>现在把文字交给 {@link SubmitNodeCollector#submitNameTag}（原版画名字的那个方法，
 * {@code Font.DisplayMode.NORMAL} = 深度测试），血条用同样的 billboard 变换 +
 * {@link SubmitNodeCollector#submitCustomGeometry} 提交，
 * 于是<b>遮挡行为与原版名字逐像素一致</b>，不再需要任何启发式判定。</p>
 *
 * <h2>坐标系</h2>
 * <p>{@code submitNameTag} 内部做：{@code translate(attachment + 0.5y) → rotate(相机朝向)
 * → scale(0.025, -0.025, 0.025)}，之后 1 个单位 = 1 个「名牌像素」。
 * 所以本 Mod 的布局像素（{@code textTop = -18}、{@code barTop = -7}）可以直接当参数用。</p>
 */
public final class HudWorldRenderer {

    /** 数据缓存时长（一帧之内所有实体共用一次解析）。 */
    private static final long CACHE_NS = 50_000_000L;

    private static long cacheStamp = 0L;
    private static Map<UUID, BossBarTracker.Bar> exactBars = Map.of();
    private static Map<UUID, TeamBarTitle.Entry> teamEntries = Map.of();

    /** 队友那一行没有护甲数据。 */
    private static final int[] NO_ARMOUR = new int[EnemyData.ARMOR_OBJECTIVES.length];

    private HudWorldRenderer() {}

    /**
     * 为一只实体提交头顶 HUD（在 {@code EntityRenderer.submitNameDisplay} 的尾部调用）。
     *
     * @param state      渲染状态（拿 nameTagAttachment / lightCoords）
     * @param pose       原版画名字用的那个 pose（原点 = 实体脚下）
     * @param collector  提交器（本身就是 OrderedSubmitNodeCollector）
     * @param camera     相机状态（billboard 朝向）
     */
    public static void submit(EntityRenderState state, PoseStack pose,
                              SubmitNodeCollector collector, CameraRenderState camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;

        Entity entity = HudRenderBridge.entityOf(state);
        if (entity == null || entity == mc.player) return;

        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        if (!cfg.enabled) return;
        // 诅咒：与地图设定一致，整个 UI 隐藏（不提供开关）。
        if (CurseDetector.isActive(mc)) return;
        // 只画本 Mod 接管的实体：数据包管控的敌人 / 队伍条里的队友。
        if (!HudTarget.managed(mc, entity)) return;
        // 距离 / 被墙挡住 / 被其它实体挡住 —— 保留为「少提交一点」的额外剔除，
        // 真正的遮挡由深度测试负责。
        if (!HudTarget.visible(mc, entity)) return;

        boolean teammate = entity instanceof Player;
        EnemyData.Snapshot data = resolveData(mc, entity, cfg, teammate);
        if (data == null) return;

        EnemyHudLayout.Options options = teammate ? teamOptions(cfg) : options(cfg);
        // 元素护甲每页 2 项，按时间循环（用实体 id 错开，避免同一帧全体翻页）。
        options.armourPage = ArmourCycle.page(data.armor, System.currentTimeMillis(), entity.getId());
        EnemyHudLayout.Result layout = EnemyHudLayout.compute(
                data.hp, data.maxHp, data.percentOnly,
                data.defence, data.armor, data.hearts, options, mc.font::width);
        if (layout.isEmpty()) return;

        Vec3 attachment = state.nameTagAttachment;
        if (attachment == null) attachment = new Vec3(0.0, state.boundingBoxHeight, 0.0);
        int light = state.lightCoords;

        // ① 着火：把整组 HUD 沿视线往相机方向推出去，推到火焰盒子前面。
        float push = firePush(state);
        boolean pushed = push > 0.0f;
        if (pushed) {
            Vec3 dir = new Vec3(state.x, state.y, state.z).add(attachment).subtract(camera.pos);
            double len = dir.length();
            if (len > 1.0e-4) {
                Vec3 step = dir.scale(push / len);
                pose.pushPose();
                pose.translate(-step.x, -step.y, -step.z);
            } else {
                pushed = false;
            }
        }

        // ② 文字行：直接交给原版画名字的 submitNameTag（背景框 + 深度测试都白拿）。
        int index = 0;
        for (EnemyHudLayout.Row row : layout.rows) {
            // submitNameTag 的 int 参数就是「名牌像素」里的 y，正好是我们的 row.top。
            collector.submitNameTag(pose, attachment, row.top, toComponent(row),
                    cfg.textShadow, light, camera);
            index++;
        }

        // ③ 血条：同样的 billboard 变换，用原版 boss_bar 纹理自己提交两个四边形。
        if (layout.barWidth > 0) {
            submitBar(mc, pose, collector, camera, attachment, layout, data, teammate, cfg, light);
        }

        if (pushed) pose.popPose();
    }

    /**
     * 着火时把 HUD 往相机方向推多远（格）。
     *
     * <h2>为什么要推</h2>
     * <p>燃烧动画是<b>不透明</b>几何（{@code solid} 阶段，写深度），画在血条之前；
     * 它的火焰盒子以实体为中心，向相机一侧伸出 {@code 0.7 × boundingBoxWidth}。
     * 血条的 billboard 平面正好穿过盒子中间，于是火焰在血条<b>前面</b>的像素会把血条
     * 深度测试掉 —— 看起来就是「着火把血条盖住了」（大/矮胖的怪尤其明显，比如蜘蛛）。</p>
     *
     * <p>沿着<b>视线方向</b>把 HUD 推出去，屏幕位置不变（同一条射线），只是离相机近了一点；
     * 推出 {@code 0.7 × 宽 + 5 cm} 之后整组 HUD 就在火焰盒子前面，深度测试不再被裁。
     * 最多推 {@value #FIRE_PUSH_MAX} 格，避免大体型怪把 HUD 推到离谱的位置。</p>
     */
    private static float firePush(EntityRenderState state) {
        if (!state.displayFireAnimation) return 0.0f;
        float width = Math.max(0.0f, state.boundingBoxWidth);
        return Math.min(FIRE_PUSH_MAX, 0.7f * width + 0.05f);
    }

    /** 着火时最多把 HUD 往相机方向推多远（格）。 */
    private static final float FIRE_PUSH_MAX = 1.2f;


    // ── 数据 ──

    private static EnemyData.Snapshot resolveData(Minecraft mc, Entity entity,
                                                  EnemyHudConfig.Cfg cfg, boolean teammate) {
        UUID id = entity.getUUID();

        // 服务端选装组件优先：玩家和怪一样有 uuid/hp/四色心/碎心/粉心，
        // 队友那一行因此也能画心数（没有 payload 时下面的回退只有 AllHearts/MaxHP）。
        MobHealthPayload.Entry pushed = cfg.useServerData ? ClientMobHealthCache.lookup(id) : null;
        if (pushed != null) {
            // 玩家头顶是那条**绿条**：进度按 红心 ÷ MaxHP 填，不跟当前心类分层
            // （分层进度换色换满格，放在单色绿条上读不出来）。敌人仍按数据包口径。
            EnemyData.Snapshot data = teammate
                    ? EnemyData.fromPayloadPlayer(pushed.hp(), pushed.maxHp(),
                    pushed.soul(), pushed.black(), pushed.blue(), pushed.cracked(),
                    pushed.maxSoul(), pushed.maxBlack(), pushed.maxBlue(),
                    pushed.defence(), pushed.armour())
                    : EnemyData.fromPayload(pushed.hp(), pushed.maxHp(),
                    pushed.soul(), pushed.black(), pushed.blue(), pushed.cracked(),
                    pushed.maxSoul(), pushed.maxBlack(), pushed.maxBlue(),
                    pushed.defence(), pushed.armour());
            if (pushed.targetted()) {
                ArmourCache.observe(id, fullArmourSlots(pushed));
            }
            return data;
        }

        if (teammate) {
            refresh(mc);
            TeamBarTitle.Entry entry = teamEntries.get(id);
            if (entry == null) return null;
            // 没有服务端组件时队伍条只给 AllHearts / MaxHP，拆不出红心 —— 只能按它填。
            return EnemyData.exact(entry.hp, entry.maxHp, entry.ratio(), 0, NO_ARMOUR,
                    HeartCounts.unknown());
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        EnemyData.Snapshot data = EnemyData.read(scoreboard, entity);
        if (data == null) return null;

        // 纯客户端：客户端 Boss 栏那条「最近敌人」条 + ArmourCache 轮换累积。
        refresh(mc);
        BossBarTracker.Bar bar = exactBars.get(id);
        if (bar != null) {
            int[] slots = ArmourCache.snapshot(id);
            int[] elements = new int[EnemyData.ARMOR_OBJECTIVES.length];
            System.arraycopy(slots, 1, elements, 0, elements.length);
            data = EnemyData.exact(bar.hp, bar.maxHp, bar.progress, slots[0], elements,
                    HeartCounts.forMob(bar.hp, bar.maxHp));
            data.kind = bar.kind;
        }
        return data;
    }

    /** 一帧解析一次 Boss 栏与队伍条；顺带跑队伍条自动开启。 */
    private static void refresh(Minecraft mc) {
        long now = System.nanoTime();
        if (now - cacheStamp < CACHE_NS) return;
        cacheStamp = now;
        EnemyHudConfig.Cfg cfg = EnemyHudConfig.get();
        exactBars = BossBarTracker.resolve(mc, BossBarTracker.snapshot(mc));
        if (cfg.showTeammates && cfg.autoPartyBossbar) {
            PartyBossbarAutoToggle.tick(mc, BossBarTracker.hasTeamBar(mc),
                    BossBarTracker.hasPersonalBar(mc));
        }
        teamEntries = cfg.showTeammates
                ? BossBarTracker.resolveTeam(mc, BossBarTracker.teamEntriesCached(mc))
                : Map.of();
    }

    // ── 血条 ──

    private static void submitBar(Minecraft mc, PoseStack pose, SubmitNodeCollector collector,
                                  CameraRenderState camera, Vec3 attachment,
                                  EnemyHudLayout.Result layout, EnemyData.Snapshot data,
                                  boolean teammate, EnemyHudConfig.Cfg cfg, int light) {
        BossBarTexture.Tint tint = teammate
                ? BossBarTexture.Tint.GREEN
                : BossBarTexture.tintFor(data.kind);
        int alpha = (int) Math.round(255.0 * cfg.opacity);
        int color = BossBarTexture.argb(0xFFFFFF, alpha);

        float x0 = -layout.barWidth / 2.0f;
        float y0 = layout.barTop;
        float y1 = layout.barTop + EnemyHudLayout.BAR_HEIGHT;
        float width = layout.barWidth;
        float fill = Math.round(width * Math.max(0.0f, Math.min(1.0f, data.ratio)));

        pose.pushPose();
        pose.translate(attachment.x, attachment.y + 0.5, attachment.z);
        pose.rotate(camera.orientation);
        // 与原版 submitNameTag 完全一致的缩放：1 单位 = 1 个名牌像素，y 向下为正。
        pose.scale(0.025f, -0.025f, 0.025f);

        // 两段**不重叠**：填充段用 progress 纹理，空槽段用 background 纹理。
        // 早先是「整条 background + 压在它上面的 progress」，两张共面四边形在深度
        // 测试下会互相抢像素，移动视角时表现为血条边缘的竖条纹闪烁。
        //
        // UV 再往里收半个纹素：182×5 的纹理被拉伸到几十个屏幕像素时，采样正好落在
        // 纹理边缘会采到相邻纹素（端盖那两列深色），同样表现为细竖条。
        final float u0 = 0.5f / BossBarTexture.NATIVE_WIDTH;
        final float u1 = 1.0f - 0.5f / BossBarTexture.NATIVE_WIDTH;
        final float v0 = 0.5f / BossBarTexture.NATIVE_HEIGHT;
        final float v1 = 1.0f - 0.5f / BossBarTexture.NATIVE_HEIGHT;

        RenderType background = RenderTypes.text(BossBarTexture.backgroundTexture(tint));
        RenderType progress = RenderTypes.text(BossBarTexture.progressTexture(tint));

        if (fill <= 0.0f) {
            collector.submitCustomGeometry(pose, background, (p, vc) ->
                    quad(vc, p, x0, y0, x0 + width, y1, u0, u1, v0, v1, color, light));
        } else if (fill >= width - 0.5f) {
            collector.submitCustomGeometry(pose, progress, (p, vc) ->
                    quad(vc, p, x0, y0, x0 + width, y1, u0, u1, v0, v1, color, light));
        } else {
            float split = x0 + fill;
            collector.submitCustomGeometry(pose, progress, (p, vc) ->
                    quad(vc, p, x0, y0, split, y1, u0, u1, v0, v1, color, light));
            float uSplit = u0 + (u1 - u0) * (fill / width);
            collector.submitCustomGeometry(pose, background, (p, vc) ->
                    quad(vc, p, split, y0, x0 + width, y1, uSplit, u1, v0, v1, color, light));
        }
        pose.popPose();
    }

    /** 一个带 UV 的矩形；UV 用纹素中心收进去，避免拉伸时采到相邻纹素。 */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1,
                             float u0, float u1, float v0, float v1,
                             int argb, int light) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        vc.addVertex(pose, x0, y0, 0.0f).setColor(r, g, b, a).setUv(u0, v0).setLight(light);
        vc.addVertex(pose, x0, y1, 0.0f).setColor(r, g, b, a).setUv(u0, v1).setLight(light);
        vc.addVertex(pose, x1, y1, 0.0f).setColor(r, g, b, a).setUv(u1, v1).setLight(light);
        vc.addVertex(pose, x1, y0, 0.0f).setColor(r, g, b, a).setUv(u1, v0).setLight(light);
    }

    // ── 布局选项 ──

    /**
     * 玩家血条用独立的一套长度/缩放/偏移。
     *
     * <p>{@code showHearts = true}：装了服务端组件时队友也有四色心 / 碎心 / 粉心，
     * 会画成和敌人一样的心数行；拿不到心数（服务端没装）时
     * {@code EnemyHudLayout} 会自动退回「当前/最大」。</p>
     *
     * <p>{@code showArmor = false}（1.16.3 起）：队友**只画心与血条**，不画护甲段 ——
     * 玩家那一行没有分隔符、没有 🛡 / 🔥 / ☃ 这类护甲段。payload 里的 defence / armour
     * 照旧收下（数据结构不用改），只是不给布局用。</p>
     */
    private static EnemyHudLayout.Options teamOptions(EnemyHudConfig.Cfg cfg) {
        EnemyHudLayout.Options o = new EnemyHudLayout.Options();
        o.showNumbers = true;
        o.showHearts = true;
        o.showArmor = false;
        o.showBar = cfg.showBar;
        o.barWidth = cfg.teamBarWidth;
        o.wrapWidth = cfg.wrapWidth;
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
        o.wrapWidth = cfg.wrapWidth;
        o.verticalOffset = cfg.verticalOffset;
        o.textShadow = cfg.textShadow;
        o.scale = cfg.scale;
        return o;
    }

    /** 一行里的彩色段拼成一个 Component，交给原版 nameTag 渲染（自带背景框）。 */
    private static Component toComponent(EnemyHudLayout.Row row) {
        MutableComponent component = Component.empty();
        for (EnemyHudLayout.Line line : row.lines) {
            component.append(Component.literal(line.text)
                    .withStyle(style -> style.withColor(line.rgb & 0xFFFFFF)));
        }
        return component;
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
}
