package com.nyalium.ctthudfork;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * 渲染状态 → 实体 的桥。
 *
 * <p>26.3 的实体渲染拆成了两段：{@code extractRenderState(Entity, EntityRenderState, float)}
 * 里能拿到实体，而真正提交几何的 {@code EntityRenderer.submit(EntityRenderState, …)} 只剩
 * 渲染状态。<b>血条要按实体 UUID 取数据</b>，所以在 extract 阶段把实体挂在状态上，
 * submit 阶段再取回来。</p>
 *
 * <p>挂载点是 Fabric 的 {@link FabricRenderState#setData}：状态对象由原版持有并复用，
 * 不需要额外的 Map，也没有跨帧泄漏。</p>
 */
public final class HudRenderBridge {

    /** 状态上的实体引用。 */
    public static final RenderStateDataKey<Entity> ENTITY =
            RenderStateDataKey.create(() -> "ctt_hud_entity");

    private HudRenderBridge() {}

    /** extract 阶段调用：把实体挂到状态上。 */
    public static void attach(EntityRenderState state, Entity entity) {
        if (state instanceof FabricRenderState renderState && entity != null) {
            renderState.setData(ENTITY, entity);
        }
    }

    /** submit 阶段调用：取回实体。 */
    public static Entity entityOf(EntityRenderState state) {
        return state instanceof FabricRenderState renderState ? renderState.getData(ENTITY) : null;
    }
}
