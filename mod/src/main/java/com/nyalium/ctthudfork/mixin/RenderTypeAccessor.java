package com.nyalium.ctthudfork.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把 {@code RenderType.create(String, RenderSetup)} 暴露出来。
 *
 * <p>26.3 里它是包私有静态方法，但血条需要**自己的一份**渲染类型：原版“世界内文字”那套
 * （{@code RenderTypes.text}）不写深度，于是名字牌之后绘制的半透明方块（蜂蜜 / 水 / 玻璃）
 * 会被深度测试放过、直接混到血条上 —— 看起来就是“方块盖在血条上”。</p>
 */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {

    @Invoker("create")
    static RenderType ctthud$create(String name, RenderSetup setup) {
        throw new AssertionError("mixin invoker");
    }
}
