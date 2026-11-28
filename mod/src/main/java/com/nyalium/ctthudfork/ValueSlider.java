package com.nyalium.ctthudfork;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 设置页统一的「步进滑块」：整数/小数区间 + 吸附到 {@code step} + 自定义文案 + 变化回调。
 *
 * <p>以前每个设置页（血条 / 属性面板 / 盾牌 / 标题 / 调试）各自复制了一份<b>完全相同</b>的私有实现，
 * 一共 5 份；哪一份单独改过（比如加过键盘处理）就会和其它页行为不一致。统一到这里以后：</p>
 * <ul>
 *   <li><b>行为单一</b>：键盘、吸附、文案、回调只有一处实现；</li>
 *   <li><b>默认就是原版键盘</b>：不重写 {@code keyPressed}，方向键负责界面焦点导航，
 *       调值交给原版 {@link AbstractSliderButton} 的语义（跟 26.3 一致）；</li>
 *   <li><b>取值永远在 [min, max] 且落在 step 上</b>，回调写回配置时不用再自己夹一次。</li>
 * </ul>
 *
 * <p>用法：{@code new ValueSlider(x, y, w, h, min, max, step, initial, v -> 文案, v -> 写回配置)}。
 * 文案 / 回调都可以传 {@code null}（只显示不联动，或只联动不显示）。</p>
 */
final class ValueSlider extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final double step;
    private final DoubleFunction<Component> label;
    private final DoubleConsumer onChange;

    ValueSlider(int x, int y, int width, int height, double min, double max, double step, double initial,
                DoubleFunction<Component> label, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), (initial - min) / (max - min));
        this.min = min;
        this.max = max;
        this.step = step;
        this.label = label;
        this.onChange = onChange;
        updateMessage();
    }

    /** 当前值（已经吸附到 step 并夹在区间内）。 */
    double currentValue() {
        double raw = this.min + (this.max - this.min) * this.value;
        double snapped = this.step > 0.0 ? Math.round(raw / this.step) * this.step : raw;
        return Math.max(this.min, Math.min(this.max, snapped));
    }

    @Override
    protected void updateMessage() {
        if (this.label != null) {
            setMessage(this.label.apply(currentValue()));
        }
    }

    @Override
    protected void applyValue() {
        if (this.onChange != null) {
            this.onChange.accept(currentValue());
        }
    }
}
