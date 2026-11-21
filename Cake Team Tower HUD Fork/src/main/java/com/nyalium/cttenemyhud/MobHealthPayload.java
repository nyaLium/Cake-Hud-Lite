package com.nyalium.cttenemyhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * S2C：每只 CTT 怪的权威血量 / 护甲 / 各类心数（<b>可选服务端组件</b>）。
 *
 * <h2>定位</h2>
 * <p>本 Mod 的定位是「客户端必装、服务端选装」：客户端不装这个包也能完整工作
 * （退回 Boss 栏 + {@code HealthPercent}）。装了服务端部分，每只怪都能拿到
 * {@code AllHearts / MaxHP / Defence / 元素护甲 / 红魂黑蓝碎}，于是头顶显示
 * {@code ❤120 | 💛40 | 🛡3} 这种绝对数值，而不是「只有条没有字」或 {@code 73%}。</p>
 *
 * <h2>为什么按 UUID</h2>
 * <p>数据包那条「最近敌人」Boss 栏标题里只有<b>名字</b>，同名怪根本分不开
 * （见 {@code BossBarTracker.resolve} 的说明）。按实体 UUID 推送就彻底解决了唯一性：
 * 客户端 {@code entity.getUUID()} 就是服务端的 UUID。</p>
 *
 * <h2>字段取舍</h2>
 * <p>不推 {@code AllHearts}/{@code RedHearts} 的原始值，只推能算回它们的最小集：</p>
 * <pre>
 *   AllHearts = RedHearts + SoulHearts + BlackHearts + BlueHearts   （health.mcfunction:25-28）
 *   RedHearts = AllHearts - SoulHearts - BlackHearts - BlueHearts
 * </pre>
 * <p>所以推 {@code AllHearts + 魂/黑/蓝/碎} 就够。{@code MaxSoulHearts / MaxBlackHearts /
 * MaxBlueHearts} 也要推 —— 客户端靠它们复现数据包 Boss 栏的「按当前心类分层」比例。</p>
 *
 * <h2>编码</h2>
 * <p>可能为负的字段（碎心、防御、元素护甲）用 zigzag varint，其余用普通 varint，
 * 一只怪典型 40 字节左右；配合服务端的差量判定，静态场景完全静默。</p>
 */
public record MobHealthPayload(byte version, List<Entry> entries) implements CustomPacketPayload {

    /** 协议版本；客户端遇到更高版本会整包忽略，避免半懂不懂地解析。 */
    public static final byte CURRENT_VERSION = 1;

    /** 一次最多推多少只怪（按距离取最近的）。 */
    public static final int MAX_ENTRIES = 32;

    /**
     * 通道 id：{@code ctt-hud-fork:mob_health}。
     *
     * <p>注意 {@code CustomPacketPayload.createType(String)} 收的是 <b>path</b>
     * （它会自动加上 {@code minecraft:} 命名空间），传 {@code "ns:path"} 会直接抛
     * {@code IdentifierException}。所以这里自己拿 {@link Identifier} 构造。</p>
     */
    public static final CustomPacketPayload.Type<MobHealthPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("ctt-hud-fork", "mob_health"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobHealthPayload> CODEC =
            CustomPacketPayload.codec(MobHealthPayload::write, MobHealthPayload::read);

    /**
     * 一只怪的一帧数据。
     *
     * @param hp        {@code AllHearts}（全部心的总量）
     * @param maxHp     {@code MaxHP}（红心上限）
     * @param soul      魂心数
     * @param black     黑心数
     * @param blue      蓝心数
     * @param cracked   碎心数（负数 = 粉心）
     * @param maxSoul   {@code MaxSoulHearts}
     * @param maxBlack  {@code MaxBlackHearts}
     * @param maxBlue   {@code MaxBlueHearts}
     * @param defence   防御值
     * @param armour    7 档元素护甲（Fire/Ice/Dark/Light/True/Water/Electric）
     * @param targetted 数据包当前是否把它当成「被锁定的敌人」（{@code Targetting} 标签）
     */
    public record Entry(UUID uuid, int hp, int maxHp, int soul, int black, int blue, int cracked,
                        int maxSoul, int maxBlack, int maxBlue,
                        int defence, int[] armour, boolean targetted) {

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Entry e)) return false;
            return hp == e.hp && maxHp == e.maxHp && soul == e.soul && black == e.black
                    && blue == e.blue && cracked == e.cracked
                    && maxSoul == e.maxSoul && maxBlack == e.maxBlack && maxBlue == e.maxBlue
                    && defence == e.defence && targetted == e.targetted
                    && uuid.equals(e.uuid) && Arrays.equals(armour, e.armour);
        }

        @Override
        public int hashCode() {
            int h = uuid.hashCode();
            h = 31 * h + hp;
            h = 31 * h + maxHp;
            h = 31 * h + soul;
            h = 31 * h + black;
            h = 31 * h + blue;
            h = 31 * h + cracked;
            h = 31 * h + defence;
            h = 31 * h + Arrays.hashCode(armour);
            return h;
        }

        @Override
        public String toString() {
            return "Entry[" + uuid + " " + hp + "/" + maxHp + " soul=" + soul + " black=" + black
                    + " blue=" + blue + " cracked=" + cracked + " def=" + defence + "]";
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 空包：告诉客户端「视野内没有 CTT 怪」，用来清缓存。 */
    public static MobHealthPayload empty() {
        return new MobHealthPayload(CURRENT_VERSION, List.of());
    }

    // ── 编解码 ──

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeByte(version);
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeUUID(e.uuid());
            buf.writeVarInt(Math.max(0, e.hp()));
            buf.writeVarInt(Math.max(0, e.maxHp()));
            buf.writeVarInt(Math.max(0, e.soul()));
            buf.writeVarInt(Math.max(0, e.black()));
            buf.writeVarInt(Math.max(0, e.blue()));
            writeSigned(buf, e.cracked());
            buf.writeVarInt(Math.max(0, e.maxSoul()));
            buf.writeVarInt(Math.max(0, e.maxBlack()));
            buf.writeVarInt(Math.max(0, e.maxBlue()));
            writeSigned(buf, e.defence());
            int[] armour = e.armour() == null ? new int[0] : e.armour();
            buf.writeByte(armour.length);
            for (int v : armour) writeSigned(buf, v);
            buf.writeBoolean(e.targetted());
        }
    }

    private static MobHealthPayload read(RegistryFriendlyByteBuf buf) {
        byte version = buf.readByte();
        int count = Math.max(0, Math.min(MAX_ENTRIES, buf.readVarInt()));
        List<Entry> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            int hp = buf.readVarInt();
            int maxHp = buf.readVarInt();
            int soul = buf.readVarInt();
            int black = buf.readVarInt();
            int blue = buf.readVarInt();
            int cracked = readSigned(buf);
            int maxSoul = buf.readVarInt();
            int maxBlack = buf.readVarInt();
            int maxBlue = buf.readVarInt();
            int defence = readSigned(buf);
            int armourLen = Math.max(0, Math.min(16, buf.readByte() & 0xFF));
            int[] armour = new int[armourLen];
            for (int k = 0; k < armourLen; k++) armour[k] = readSigned(buf);
            boolean targetted = buf.readBoolean();
            out.add(new Entry(uuid, hp, maxHp, soul, black, blue, cracked,
                    maxSoul, maxBlack, maxBlue, defence, armour, targetted));
        }
        return new MobHealthPayload(version, List.copyOf(out));
    }

    /** zigzag：小负数/小正数都只占 1 字节。 */
    private static void writeSigned(RegistryFriendlyByteBuf buf, int value) {
        buf.writeVarInt((value << 1) ^ (value >> 31));
    }

    private static int readSigned(RegistryFriendlyByteBuf buf) {
        int raw = buf.readVarInt();
        return (raw >>> 1) ^ -(raw & 1);
    }

    /** 供调试/日志用的 id。 */
    public static Identifier id() {
        return TYPE.id();
    }
}
