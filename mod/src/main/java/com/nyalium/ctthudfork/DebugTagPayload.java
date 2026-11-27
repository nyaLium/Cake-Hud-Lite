package com.nyalium.ctthudfork;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C：服务端告诉这个玩家「你身上有没有 {@value #DEBUG_TAG} 标签」。
 *
 * <h2>为什么必须由服务端告知</h2>
 * <p>实体 tag（{@code /tag add}）是<b>纯服务端</b>数据，原版协议<b>从不</b>把它同步给客户端 ——
 * 客户端 {@code entity.getTags()} 对任何玩家都是空的。而调试设置的权限门槛需要这个标签，
 * 所以由本 Mod 的服务端部分在每个玩家身上查一次，只在状态变化时发这个一字节的包。</p>
 *
 * <p>服务端没装本 Mod 时收不到这个包 → 客户端按「没有标签」处理（调试设置保持锁定），
 * 这与「必须拥有该标签才能调整调试设置」的要求一致。</p>
 *
 * @param hasTag 这个玩家是否带 {@link #DEBUG_TAG} 标签
 */
public record DebugTagPayload(boolean hasTag) implements CustomPacketPayload {

    /** 调试设置要求的标签名（服务端 {@code /tag <player> add CTT-HUD-LITE-DEBUG}）。 */
    public static final String DEBUG_TAG = "CTT-HUD-LITE-DEBUG";

    /** 通道 id：{@code ctt-hud-fork:debug_tag}。 */
    public static final CustomPacketPayload.Type<DebugTagPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("ctt-hud-fork", "debug_tag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DebugTagPayload> CODEC =
            CustomPacketPayload.codec(
                    (payload, buf) -> buf.writeBoolean(payload.hasTag()),
                    buf -> new DebugTagPayload(buf.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
