package com.nyalium.ctthudfork;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.nyalium.ctthudfork.mixin.RenderTypeAccessor;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 血条专用渲染类型：和原版世界内文字**同一套着色器 / 顶点格式**，只多开一个**写深度**。
 *
 * <h2>为什么需要它</h2>
 * <p>血条是在“名字牌”那一批里提交的（和原版名牌同一阶段），而原版 {@code RenderTypes.text}
 * 的管线<b>不写深度</b>。半透明方块（蜂蜜 / 水 / 玻璃）在名字牌之后才画，深度测试放行，
 * 于是整块方块被混到血条上 —— 看起来就像“方块盖在血条上面 / 血条变透明”。</p>
 *
 * <p>让血条自己写深度以后：<b>更远的</b>半透明方块会被深度测试挡掉（血条保持实心），
 * <b>更近的</b>方块照常盖住血条（遮挡关系仍然正确），不透明方块的行为完全不变。</p>
 *
 * <h2>兜底</h2>
 * <p>这套东西依赖 RenderPearl 的管线细节，任何一步出问题（构造管线 / 建渲染类型失败）就整体
 * 退回原版 {@link RenderTypes#text} —— 最坏情况只是保持旧观感，绝不会崩。</p>
 */
final class BarRenderTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger("ctt-hud-fork/bar-render");

    /** 原版世界内文字管线的“写深度”版本；{@code null} = 这套玩法不可用。 */
    private static final RenderPipeline DEPTH_TEXT = createDepthTextPipeline();
    private static final Map<Identifier, RenderType> CACHE = new HashMap<>();
    private static boolean warned;

    private BarRenderTypes() {
    }

    /** 某个纹理对应的血条渲染类型（写深度）；不可用时退回原版文字渲染类型。 */
    static RenderType forTexture(Identifier texture) {
        if (DEPTH_TEXT == null) {
            return RenderTypes.text(texture);
        }

        synchronized (CACHE) {
            RenderType cached = CACHE.get(texture);
            if (cached != null) {
                return cached;
            }

            RenderType made = null;
            try {
                RenderSetup setup = RenderSetup.builder(DEPTH_TEXT)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .createRenderSetup();
                made = RenderTypeAccessor.ctthud$create("ctt_hud_bar", setup);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    LOGGER.warn("[CTT HUD] 血条写深度渲染类型不可用，退回原版文字渲染", t);
                }
            }

            RenderType result = made != null ? made : RenderTypes.text(texture);
            CACHE.put(texture, result);
            return result;
        }
    }

    /**
     * 复制一份原版世界内文字管线，只把 {@code writeDepth} 打开。
     *
     * <p>管线不可变、构造器是 protected，所以这里继承出来：除了深度状态，其余（着色器、
     * 顶点格式、绑定组、面剔除、图元类型…）全部照抄 —— 顶点数据格式完全不用改。</p>
     */
    private static RenderPipeline createDepthTextPipeline() {
        try {
            RenderPipeline base = RenderPipelines.TEXT;
            return new RenderPipeline(
                    base.getLocation().withSuffix("_ctt_bar_depth"),
                    base.getShaders(),
                    base.getShaderDefines(),
                    base.getBindGroupLayouts(),
                    base.getColorTargetStates().toArray(new ColorTargetState[0]),
                    new DepthStencilState(base.getDepthStencilState().depthTest(), true),
                    base.getPolygonMode(),
                    base.isCull(),
                    base.getVertexFormatBindings().toArray(new VertexFormat[0]),
                    base.getPrimitiveTopology(),
                    base.pushConstantSize(),
                    base.getSortKey()) {
            };
        } catch (Throwable t) {
            LOGGER.warn("[CTT HUD] 血条写深度管线构造失败，退回原版文字渲染", t);
            return null;
        }
    }
}
