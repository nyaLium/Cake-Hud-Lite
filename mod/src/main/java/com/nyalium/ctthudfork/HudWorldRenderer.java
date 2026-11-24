package com.nyalium.ctthudfork;

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

    /** 数据缓存时长：**50 ms**（一帧之内所有实体共用一次解析）。 */
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

        HudConfig.Cfg cfg = HudConfig.get();
        if (!cfg.enabled) return;
        // 诅咒：与地图设定一致，整个 UI 隐藏（不提供开关）。
        if (CurseDetector.isActive(mc)) return;
        // 1.2.4 癫狂：隐藏实体血条（面板另走 HudElement，不受影响）。
        if (BerserkDetector.isActive(mc)) return;
        // 只画本 Mod 接管的实体：数据包管控的敌人 / 队伍条里的队友。
        if (!HudTarget.managed(mc, entity)) return;
        // 距离 / 被墙挡住 / 被其它实体挡住 —— 保留为「少提交一点」的额外剔除，
        // 真正的遮挡由深度测试负责。
        if (!HudTarget.visible(mc, entity)) return;

        boolean teammate = entity instanceof Player;
        EnemyData.Snapshot data = resolveData(mc, entity, teammate);
        if (data == null) return;

        HudLayout.Options options = teammate ? teamOptions(cfg) : options(cfg);
        // 元素护甲每页 2 项，按时间循环（用实体 id 错开，避免同一帧全体翻页）。
        long now = System.currentTimeMillis();
        options.armourPage = ArmourCycle.page(data.armor, now, entity.getId());
        // 心数行只显示红心 + 一种其它心，按时间轮换（玩家只在魂/黑之间换）。
        options.playerStyle = teammate;
        options.cycleKind = HeartCycle.pick(data.hearts, teammate, now, entity.getId());
        HudLayout.Result layout = HudLayout.compute(
                data.hp, data.maxHp, data.percentOnly,
                data.defence, data.armor, data.hearts, options, mc.font::width);
        if (layout.isEmpty()) return;

        Vec3 attachment = state.nameTagAttachment;
        if (attachment == null) attachment = new Vec3(0.0, state.boundingBoxHeight, 0.0);
        // 1.20.13：HUD 用**全亮**光照，不再取实体所在方块的光照 ——
        // 以前在暗处/阴影里文字和血条会跟着变暗（甚至全黑），读不清。原版名牌也是全亮。
        final int light = 0xF000F0;   // LightTexture.FULL_BRIGHT：方块光 15 + 天空光 15

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

        // ② 文字行：自己用 submitText 提交。
        //    原版 submitNameTag 把颜色写死成不透明白、缩放写死 0.025，导致「透明度」和
        //    「缩放」对文字完全无效；自己提交后两者都生效（背景框仍按原版 y-1 ~ y+9 画）。
        int alpha = (int) Math.round(255.0 * cfg.opacity);
        int bgAlpha = (int) Math.round(0x40 * cfg.opacity);   // 原版名牌底色 ≈ 25% 黑
        submitRows(collector, pose, camera, attachment, layout, options.scale,
                cfg, light, alpha, bgAlpha);

        // ③ 血条：同样的 billboard 变换，用原版 boss_bar 纹理自己提交两个四边形。
        if (layout.barWidth > 0) {
            submitBar(mc, pose, collector, camera, attachment, layout, data, teammate, cfg, light,
                    (float) options.scale);
        }

        if (pushed) pose.popPose();
    }

    /**
     * 提交所有文字行（两遍，与原版名牌同款）。
     *
     * <ol>
     *   <li><b>NORMAL</b>：不透明文字、不带背景、深度测试 —— 正常可见、被墙/模型正常挡住；</li>
     *   <li><b>SEE_THROUGH</b>：约 50% 的文字 + 25% 黑的背景框，**不做深度测试** ——
     *       这就是原版名牌那层「透视副本」：隔墙也能隐约看到，挡住时也不会整块消失。</li>
     * </ol>
     *
     * <p>与原版 {@code submitNameTag} 的差别只有：颜色带 {@code opacity} 的 alpha（透明度生效）、
     * 缩放可调。字形阴影（{@code textShadow}）两遍都跟着走。</p>
     */
    private static void submitRows(SubmitNodeCollector collector, PoseStack pose,
                                   CameraRenderState camera, Vec3 attachment,
                                   HudLayout.Result layout, double scale,
                                   HudConfig.Cfg cfg, int light, int alpha, int bgAlpha) {
        float s = (float) scale;
        int color = (alpha << 24) | 0xFFFFFF;
        // 原版透视副本的文字 alpha ≈ 128（0.5），背景 ≈ 64（0.25）。
        int seeColor = (Math.max(0, alpha * 128 / 255) << 24) | 0xFFFFFF;
        int background = bgAlpha << 24;
        pose.pushPose();
        pose.translate(attachment.x, attachment.y + 0.5, attachment.z);
        pose.rotate(camera.orientation);
        // 与原版 submitNameTag 同样的映射：1 单位 = 1 个名牌像素，y 向下为正；再乘缩放。
        pose.scale(0.025f * s, -0.025f * s, 0.025f * s);
        for (HudLayout.Row row : layout.rows) {
            float x = -row.width / 2.0f;
            var text = toComponent(row).getVisualOrderText();
            collector.submitText(pose, x, row.top, text, cfg.textShadow,
                    Font.DisplayMode.NORMAL, light, color, 0, 0);
            collector.submitText(pose, x, row.top, text, cfg.textShadow,
                    Font.DisplayMode.SEE_THROUGH, light, seeColor, background, 0);
        }
        pose.popPose();
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

    private static EnemyData.Snapshot resolveData(Minecraft mc, Entity entity, boolean teammate) {
        UUID id = entity.getUUID();

        // 服务端选装组件写死优先（1.20.35 起没有开关）：玩家和怪一样有 uuid/hp/四色心/碎心/粉心，
        // 队友那一行因此也能画心数（没有 payload 时下面的回退只有 AllHearts/MaxHP）。
        // 服务端没装本 Mod 时 ClientMobHealthCache 里没有条目（或 5 s 后失鲜），自然回落到客户端通道。
        MobHealthPayload.Entry pushed = ClientMobHealthCache.lookup(id);
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
            if (entry != null) {
                // 1.2.2：纯客户端时 below_name 的 HealthPercent 就是「红心 ÷ MaxHP」的百分比
                // （数据包 health.mcfunction 对所有参战玩家都算），比队伍条的 AllHearts/MaxHP
                // 更贴近血条本意（蓝心/魂心不该撑满绿条）。有百分比就用它填进度，
                // 文字仍按队伍条的 当前/上限 显示。
                int percent = EnemyData.healthPercentOf(mc.level.getScoreboard(), entity);
                float ratio = percent >= 0
                        ? Math.max(0.0f, Math.min(1.0f, percent / 100.0f))
                        : entry.ratio();
                return EnemyData.exact(entry.hp, entry.maxHp, ratio, 0, NO_ARMOUR,
                        HeartCounts.unknown());
            }
            // 队伍条里没这一条（没开队伍条 / 解析时没认到他）→ 退回队友自己的
            // below_name HealthPercent（= 红心 ÷ MaxHP 的百分比，地图对所有参战玩家都算）。
            // 效果：绿条按百分比填、数字显示 "73%"，总比什么都没有强。
            return EnemyData.read(mc.level.getScoreboard(), entity);
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
        HudConfig.Cfg cfg = HudConfig.get();
        exactBars = BossBarTracker.resolve(mc, BossBarTracker.snapshot(mc));
        if (cfg.showTeammates) {
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
                                  HudLayout.Result layout, EnemyData.Snapshot data,
                                  boolean teammate, HudConfig.Cfg cfg, int light,
                                  float scale) {
        BossBarTexture.Tint tint = teammate
                ? BossBarTexture.Tint.GREEN
                : BossBarTexture.tintFor(data.kind);
        int alpha = (int) Math.round(255.0 * cfg.opacity);
        int color = BossBarTexture.argb(0xFFFFFF, alpha);

        float x0 = -layout.barWidth / 2.0f;
        float y0 = layout.barTop;
        float y1 = layout.barTop + HudLayout.BAR_HEIGHT;
        float width = layout.barWidth;
        float fill = Math.round(width * Math.max(0.0f, Math.min(1.0f, data.ratio)));

        pose.pushPose();
        pose.translate(attachment.x, attachment.y + 0.5, attachment.z);
        pose.rotate(camera.orientation);
        // 与文字同一套映射：1 单位 = 1 个名牌像素，y 向下为正；缩放与文字一致。
        pose.scale(0.025f * scale, -0.025f * scale, 0.025f * scale);

        // 原版 BossBar 的画法：**整条背景** + 把进度纹理**从左裁到 fill 宽度**叠在上面。
        // 以前是「进度段 + 空槽段」两段 adjacent 拼接，接缝处会露出一条 1 px 的缝
        // （两张纹理各裁一半，端盖都对不上）；改成整条背景打底就没有接缝了。
        //
        // 两张四边形共面，所以背景往远处挪 0.02 个名牌像素（原版文字背景也是 -0.01，
        // 负 z = 更远），这样无论深度写不写、先画谁，进度永远压在背景上面。
        //
        // 细节见 {@link #submitBarSpan}：182 纹素不能再整条拉伸 —— 两端 4 纹素的
        // 渐晕 + 圆角会被压成不到 1 px，条就成了长方形。
        final float BACKGROUND_Z = -0.02f;
        final float nativeWidth = BossBarTexture.NATIVE_WIDTH;

        RenderType background = RenderTypes.text(BossBarTexture.backgroundTexture(tint));
        RenderType progress = RenderTypes.text(BossBarTexture.progressTexture(tint));
        // 1.20.9：血条也补一层「透视副本」，和文字一致 —— 被天花板/方块挡住时不会整条消失，
        // 只会变成半透明的一条（以前只有文字有这层，条会被直接裁掉）。
        RenderType seeBackground = RenderTypes.textSeeThrough(BossBarTexture.backgroundTexture(tint));
        RenderType seeProgress = RenderTypes.textSeeThrough(BossBarTexture.progressTexture(tint));
        int seeColor = (Math.max(0, alpha * 128 / 255) << 24) | 0xFFFFFF;

        // ① 整条背景（含空槽）：两端圆角按原尺寸，中段拉伸
        submitBarSpan(collector, pose, seeBackground, x0, x0 + width, 0.0f, nativeWidth,
                y0, y1, seeColor, light, BACKGROUND_Z);
        submitBarSpan(collector, pose, background, x0, x0 + width, 0.0f, nativeWidth,
                y0, y1, color, light, BACKGROUND_Z);

        // ② 填充段：从左裁到 fill（和原版 blitSprite 的 width 参数一样）。
        //    裁到满格时 ub 正好是纹理右端，右端盖才会画出来 —— 与原版一致。
        if (fill > 0.0f) {
            float uFill = nativeWidth * Math.min(1.0f, fill / width);
            submitBarSpan(collector, pose, seeProgress, x0, x0 + fill, 0.0f, uFill,
                    y0, y1, seeColor, light, 0.01f);
            submitBarSpan(collector, pose, progress, x0, x0 + fill, 0.0f, uFill,
                    y0, y1, color, light, 0.0f);
        }
        pose.popPose();
    }

    /**
     * 画一条原版 {@code boss_bar} 纹理（182×5），分段规则见 {@link BossBarTexture#slices}：
     * <b>两端端盖按纹素原尺寸、中段拉伸</b>。
     *
     * @param ua / ub 这段对应的纹理横坐标（纹素，0 ~ 182）
     */
    private static void submitBarSpan(SubmitNodeCollector collector, PoseStack pose, RenderType rt,
                                      float xa, float xb, float ua, float ub,
                                      float y0, float y1, int color, int light, float z) {
        final float nw = BossBarTexture.NATIVE_WIDTH;
        for (float[] s : BossBarTexture.slices(xa, xb, ua, ub)) {
            final float ax = s[0];
            final float bx = s[1];
            final float au = s[2] / nw;
            final float bu = s[3] / nw;
            collector.submitCustomGeometry(pose, rt, (p, vc) ->
                    quad(vc, p, ax, y0, bx, y1, au, bu, V_TOP, V_BOTTOM, color, light, z));
        }
    }

    /**
     * 竖向 UV：<b>整张 5 行全要</b>。
     *
     * <p>以前往里收了半个纹素（0.5/5 ~ 4.5/5），本意是防拉伸时采到相邻纹素，
     * 代价是最上面那行深色和下边那行深色在不同距离下会有一行被吃掉 ——
     * 而条「有没有立体感/圆头」全靠这两行深色描边。纹理只有 5 纹素高、
     * 条的布局高度也正好 5 单位，直接用 0~1 才是 1:1。</p>
     */
    private static final float V_TOP = 0.0f;
    private static final float V_BOTTOM = 1.0f;

    /** 一个带 UV 的矩形；UV 已经换算成 0~1，边界落在纹素边界上（不内收，否则端盖会缺纹素）。 */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1,
                             float u0, float u1, float v0, float v1,
                             int argb, int light, float z) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        vc.addVertex(pose, x0, y0, z).setColor(r, g, b, a).setUv(u0, v0).setLight(light);
        vc.addVertex(pose, x0, y1, z).setColor(r, g, b, a).setUv(u0, v1).setLight(light);
        vc.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setUv(u1, v1).setLight(light);
        vc.addVertex(pose, x1, y0, z).setColor(r, g, b, a).setUv(u1, v0).setLight(light);
    }

    // ── 布局选项 ──

    /**
     * 玩家血条用独立的一套长度/缩放/偏移。
     *
     * <p>{@code showHearts = true}：装了服务端组件时队友也有四色心 / 碎心 / 粉心，
     * 会画成和敌人一样的心数行；拿不到心数（服务端没装）时
     * {@code HudLayout} 会自动退回「当前/最大」。</p>
     *
     * <p>{@code showArmor = false}（1.16.3 起）：队友**只画心与血条**，不画护甲段 ——
     * 玩家那一行没有分隔符、没有 🛡 / 🔥 / ☃ 这类护甲段。payload 里的 defence / armour
     * 照旧收下（数据结构不用改），只是不给布局用。</p>
     *
     * <p>1.20.34：队友整块由 {@code showTeammates} 管（关掉时 {@link HudTarget#managed} 直接返回
     * false，根本不会走到这里），所以能画的时候血条一定画。</p>
     */
    static HudLayout.Options teamOptions(HudConfig.Cfg cfg) {
        HudLayout.Options o = new HudLayout.Options();
        o.showNumbers = true;
        o.showHearts = true;
        o.showArmor = false;
        o.showBar = true;
        o.barWidth = cfg.teamBarWidth;
        o.textShadow = cfg.textShadow;
        o.scale = cfg.teamScale;
        return o;
    }

    /**
     * 敌人：{@code showBar} 是**整块**开关（1.20.34）。
     *
     * <p>关掉后血条和上方的心数 / 护甲文字一起不画：四个 flag 全跟着它走，
     * {@link HudLayout#compute} 会得到空结果，渲染直接返回——只剩原版名字。</p>
     */
    static HudLayout.Options options(HudConfig.Cfg cfg) {
        HudLayout.Options o = new HudLayout.Options();
        o.showNumbers = cfg.showBar;
        o.showHearts = cfg.showBar;
        o.showArmor = cfg.showBar;
        o.showBar = cfg.showBar;
        o.barWidth = cfg.barWidth;
        o.textShadow = cfg.textShadow;
        o.scale = cfg.scale;
        return o;
    }

    /** 一行里的彩色段拼成一个 Component，交给原版 nameTag 渲染（自带背景框）。 */
    private static Component toComponent(HudLayout.Row row) {
        MutableComponent component = Component.empty();
        for (HudLayout.Line line : row.lines) {
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
