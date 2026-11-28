package com.nyalium.ctthudfork;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.List;

/**
 * 「透视阶段」里的任意带 UV 四边形 —— 给血条那条<b>带纹理</b>的透视副本用。
 *
 * <h2>为什么需要它</h2>
 * <p>26.3 里「透视」不是 {@link RenderType} 的属性，而是<b>渲染阶段</b>：
 * {@link SubmitNodeStorage#seeThrough()} 由 {@code LevelRenderer.executeSeeThrough}
 * 在<b>所有地形（含半透明）之后</b>、于一份<b>没有深度附件</b>的 render pass 里执行。
 * 原版只有 {@code submitText(..., DisplayMode.SEE_THROUGH, ...)} 能进来，
 * 而 {@code submitCustomGeometry} 只按 {@code renderType.hasBlending()} 二选一
 * （{@code translucentCustomGeometry} 或 {@code solid}）—— 两者都在半透明地形<b>之前</b>，
 * 所以「关掉深度测试」的自定义几何会被随后画的蜂蜜 / 水 / 玻璃再混一次色，
 * 看起来就是「血条变透明了」。</p>
 *
 * <p>本类绕过这个限制：自己实现一个 {@link TranslucentSubmit} 节点，直接塞进
 * {@code SubmitNodeStorage.seeThrough()}，并注册一个对应的
 * {@link RenderTypeFeatureRenderer} 来画它。节点里的每一层都是
 * 「{@link RenderType} + 一段几何写入回调」，所以一条血条可以把
 * 「背景纹理 + 进度纹理」放进<b>同一个节点</b>，绘制顺序不会被阶段的距离排序打乱。</p>
 *
 * <h2>深度 / 混合</h2>
 * <p>这个阶段没有深度附件，所以层里必须用不依赖深度的管线 —— 血条用的是
 * {@code RenderTypes.textSeeThrough(...)}（和原版名牌的「透视副本」同一条管线）。
 * 执行时 {@code OitStage} 为 null，走普通的 {@code drawFromBuffer}，
 * 不需要 OIT 管线集（「改进的透明」开着也不会抛异常）。</p>
 *
 * <p>渲染器在 {@code FeatureRenderDispatcher} 构造时注册（见
 * {@code FeatureRenderDispatcherMixin}）；不注册的话 {@code PreparedFrame}
 * 会在 {@code getOrThrow} 处抛异常。</p>
 */
public final class SeeThroughQuads implements TranslucentSubmit {

    /** 本节点类型；{@code FeatureRendererType} 的 id 由原版自增分配。 */
    public static final FeatureRendererType<SeeThroughQuads> TYPE =
            FeatureRendererType.create("ctt_hud_fork_see_through_quads");

    /** 一层：一种渲染类型 + 写进它的几何。同一层里的多个四边形按顺序落进同一批。 */
    record Layer(RenderType renderType, CustomGeometryRenderer geometry) {}

    private final PoseStack.Pose pose;
    private final float distanceToCameraSq;
    private final List<Layer> layers;

    /**
     * 渲染器有没有成功注册（{@code FeatureRenderDispatcherMixin} 在构造末尾置位）。
     *
     * <p>没注册还往 {@code seeThrough} 里塞节点的话，{@code PreparedFrame} 会在
     * {@code getOrThrow} 处直接抛异常 —— 所以先问一句，拿不到就退回实心层。
     * 正常情况下它恒为 true。</p>
     */
    private static volatile boolean registered;
    /** 透视阶段这一帧有没有真的被执行（见 {@code LevelRendererMixin}）。 */
    private static volatile boolean passSeen;
    /** 第一次提交的纳秒时间；用于「还没见过这一遍」的宽限期。 */
    private static volatile long firstSubmitNs;
    /** 宽限期：约 300 ms（十几帧）。超过还没见过 {@code executeSeeThrough} 就退回实心层。 */
    private static final long GRACE_NS = 300_000_000L;

    public static void markRegistered() {
        registered = true;
    }

    /** {@code LevelRenderer.executeSeeThrough} 被执行时调用（说明这一遍在这个环境里是活的）。 */
    public static void markPassSeen() {
        passSeen = true;
    }

    /**
     * 透视阶段现在能不能用：渲染器注册成功，且「已经亲眼见过它执行」或仍在宽限期内。
     *
     * <p>有些环境（替换了 LevelRenderer 的优化 mod / 特殊后端）不会执行这一遍 —— 那时整组 HUD
     * 只提交 seeThrough 就等于什么都不画。宽限期过后仍没见过，就退回实心层（至少看得见）。</p>
     */
    /** 诊断用：透视阶段是否已经见过（只读）。 */
    public static boolean passSeenForDiag() {
        return passSeen;
    }

    public static boolean passAvailable() {
        if (!registered) return false;
        if (passSeen) return true;
        long first = firstSubmitNs;
        if (first == 0L) return true;   // 还没试过：先让它提交一次，才知道这一遍活不活
        return System.nanoTime() - first < GRACE_NS;
    }

    static boolean available() {
        return registered;
    }

    private SeeThroughQuads(PoseStack.Pose pose, List<Layer> layers) {
        this.pose = pose;
        this.distanceToCameraSq = TranslucentSubmit.computeDistanceToCameraSq(pose.pose());
        this.layers = layers;
    }

    /**
     * 把一组层提交到本帧的透视阶段。
     *
     * <p>{@code collector} 在实体提交链路里就是 {@link SubmitNodeStorage} 本身
     * （{@code LevelRenderer.submitFeatures} 直接把 storage 往下传）；万一被包装过就放弃这一层，
     * 血条退化成只有实心那层，不会崩。</p>
     *
     * <p>{@code pose} 必须是调用方已经摆好的 billboard 变换（与 {@code submitCustomGeometry}
     * 收到的那个一致）；这里立刻 {@code copy()}，避免 PoseStack 后续被改。</p>
     *
     * @return true = 已提交；false = 这一帧没有可用的透视阶段，调用方应退回实心层
     */
    public static boolean submit(SubmitNodeCollector collector, PoseStack pose, List<Layer> layers) {
        if (!registered || layers == null || layers.isEmpty()) return false;
        if (!(collector instanceof SubmitNodeStorage storage)) return false;
        if (firstSubmitNs == 0L) firstSubmitNs = System.nanoTime();
        storage.seeThrough().submit(new SeeThroughQuads(pose.last().copy(), layers));
        return true;
    }

    @Override
    public float distanceToCameraSq() {
        return distanceToCameraSq;
    }

    @Override
    public FeatureRendererType<SeeThroughQuads> featureType() {
        return TYPE;
    }

    /** 画本节点：按层顺序取 {@link VertexConsumer} 并写入几何。 */
    public static final class Renderer extends RenderTypeFeatureRenderer<SeeThroughQuads> {
        @Override
        protected void buildGroup(FeatureFrameContext context, List<SeeThroughQuads> submits) {
            for (SeeThroughQuads submit : submits) {
                for (Layer layer : submit.layers) {
                    layer.geometry().render(submit.pose, getVertexBuilder(layer.renderType()));
                }
            }
        }
    }
}
