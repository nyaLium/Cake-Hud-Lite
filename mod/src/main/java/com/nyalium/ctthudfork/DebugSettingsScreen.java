package com.nyalium.ctthudfork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 「调试功能」：把排查用的特殊开关集中到一页，正常游玩保持默认即可。
 *
 * <p>总界面的「调试模式」开关改成这个入口按钮；点进来是下面这张表。</p>
 *
 * <h2>行 ↔ 配置字段</h2>
 * <table>
 *   <tr><th>行</th><th>字段</th><th>范围 / 默认</th><th>作用</th></tr>
 *   <tr><td>优先使用服务端数据</td><td>{@code preferServerData}</td><td>默认开</td>
 *       <td>关掉后完全忽略 payload，强制走 Boss 栏 + HealthPercent 的纯客户端回退</td></tr>
 *   <tr><td>筛选实体数量</td><td>{@code payloadLimit}</td><td>1 ~ {@link MobHealthPayload#MAX_ENTRIES}，默认 64</td>
 *       <td>客户端只保留 payload 的前 N 条（服务端已按「玩家优先、其余按距离」排好序）</td></tr>
 *   <tr><td>隐藏名称下方分数</td><td>{@code hideBelowNameScore}</td><td>默认开</td><td>1.4.7 从「血条设置」挪进来</td></tr>
 *   <tr><td>隐藏上方 Boss 栏（最近敌人）</td><td>{@code hideEnemyBossBar}</td><td>默认开</td><td>同上</td></tr>
 *   <tr><td>隐藏上方 Boss 栏（队伍血量）</td><td>{@code hideTeamBossBar}</td><td>默认开</td><td>同上（数据照常读取）</td></tr>
 * </table>
 *
 * <p>「优先使用服务端数据」关掉时，「筛选实体数量」灰掉 —— 都不读 payload 了，条数没有意义。</p>
 */
public final class DebugSettingsScreen extends Screen {

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int TOP = 60;
    private static final int WIDTH = 260;

    /**
     * 调试设置要求的最低权限等级 —— <b>写死在代码里，不提供任何设置项</b>。
     *
     * <p>2 = 游戏管理员（gamemaster，原版 op 等级 2）：能用 {@code /gamemode}、{@code /give}
     * 这一档。1 = 协管（moderator），3 = 管理员（admin），4 = 服主（owner）。
     * 想放宽/收紧就改这一行常量，配置界面上没有对应开关。</p>
     */
    static final int REQUIRED_PERMISSION_LEVEL = 2;

    private final HudConfigScreen menu;

    public DebugSettingsScreen(HudConfigScreen menu) {
        super(Component.translatable("ctt_hud_fork.config.debugTitle"));
        this.menu = menu;
    }

    /** 等级 → 26.3 的权限节点（0 = 不需要权限，返回 null）。 */
    private static Permission permissionForLevel(int level) {
        return switch (level) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            case 4 -> Permissions.COMMANDS_OWNER;
            default -> null;
        };
    }

    /**
     * 当前本地玩家有没有权限改调试设置。
     *
     * <p>26.3 把 op 等级换成了 {@code PermissionSet}（服务端同步到客户端，原版 F3 快捷键
     * 判定用的就是同一套），所以这里直接查 {@link Permissions#COMMANDS_GAMEMASTER}。
     * 单人游戏开了作弊时原版给的就是 4 级，一样通过。</p>
     *
     * <p>除权限外，还要求玩家身上有 {@link DebugTagPayload#DEBUG_TAG} 标签。
     * 实体 tag 是纯服务端数据（原版协议不同步给客户端），所以这个状态由本 Mod 的服务端
     * 在变化时推过来（见 {@link DebugTagPayload}）。服务端没装本 Mod → 按「没有标签」处理。</p>
     */
    static boolean canAdjust() {
        return lockReason() == Lock.NONE;
    }

    /** 不能调整时的原因。 */
    enum Lock {
        /** 可以调整。 */
        NONE,
        /** 权限等级不够。 */
        PERMISSION,
        /** 缺少调试标签（或服务端没告知）。 */
        TAG
    }

    static Lock lockReason() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return Lock.PERMISSION;
        Permission required = permissionForLevel(REQUIRED_PERMISSION_LEVEL);
        if (required != null && !mc.player.permissions().hasPermission(required)) {
            return Lock.PERMISSION;
        }
        // 第二个条件 —— CTT-HUD-LITE-DEBUG 标签。
        if (!ClientMobHealthCache.debugTag()) return Lock.TAG;
        return Lock.NONE;
    }

    @Override
    protected void init() {
        int width = Math.min(WIDTH, this.width - 40);
        int x = this.width / 2 - width / 2;
        int y = TOP;
        HudConfig.Cfg cfg = HudConfig.get();

        // 权限不够时整页不可调整，只留一个「返回」。
        if (!canAdjust()) {
            addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                    .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
            return;
        }

        // 优先使用服务端数据：preferServerData（关掉后强制纯客户端回退，方便双端对比）
        addRenderableWidget(CycleButton.onOffBuilder(cfg.preferServerData)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.preferServerData"),
                        (b, v) -> {
                            HudConfig.get().preferServerData = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;

        // 筛选实体数量：payloadLimit（1 ~ 单包上限；不读服务端数据时没有意义，灰掉）
        AbstractSliderButton limit = new ValueSlider(x, y, width, ROW_HEIGHT,
                1, MobHealthPayload.MAX_ENTRIES, 1, HudConfig.get().payloadLimit,
                v -> Component.translatable("ctt_hud_fork.config.payloadLimit",
                        (int) Math.round(v)),
                v -> HudConfig.get().payloadLimit = (int) Math.round(v));
        limit.active = cfg.preferServerData;
        addRenderableWidget(limit);
        y += ROW_GAP;

        // 原来在「血条设置 → 通用」里的三个隐藏开关，挪到调试功能页。
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideBelowNameScore)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.hideBelowNameScore"),
                        (b, v) -> HudConfig.get().hideBelowNameScore = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideEnemyBossBar)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.hideEnemyBossBar"),
                        (b, v) -> HudConfig.get().hideEnemyBossBar = v));
        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(cfg.hideTeamBossBar)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.hideTeamBossBar"),
                        (b, v) -> HudConfig.get().hideTeamBossBar = v));
        y += ROW_GAP;

        // ── 1.6.0 新增三项 ──
        // 常显敌人名称：原版只在「近 / 准星对着」时画名牌，开了就任何距离都画
        addRenderableWidget(CycleButton.onOffBuilder(cfg.alwaysShowEnemyName)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.alwaysShowEnemyName"),
                        (b, v) -> HudConfig.get().alwaysShowEnemyName = v));
        y += ROW_GAP;
        // 血条不可透视：隔墙看不到，但头顶的方块也切不掉
        addRenderableWidget(CycleButton.onOffBuilder(cfg.barNoSeeThrough)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.barNoSeeThrough"),
                        (b, v) -> HudConfig.get().barNoSeeThrough = v));
        y += ROW_GAP;
        // 血量条长度自动调整：100% = 上限，0% = 下限
        addRenderableWidget(CycleButton.onOffBuilder(cfg.autoBarLength)
                .create(x, y, width, ROW_HEIGHT,
                        Component.translatable("ctt_hud_fork.config.autoBarLength"),
                        (b, v) -> {
                            HudConfig.get().autoBarLength = v;
                            this.rebuildWidgets();
                        }));
        y += ROW_GAP;
        AbstractSliderButton minWidth = new ValueSlider(x, y, width, ROW_HEIGHT,
                HudLayout.BAR_WIDTH_MIN, HudLayout.BAR_WIDTH_MAX, 1, cfg.autoBarMinWidth,
                v -> Component.translatable("ctt_hud_fork.config.autoBarMinWidth",
                        (int) Math.round(v)),
                v -> HudConfig.get().autoBarMinWidth = Math.round(v));
        minWidth.active = cfg.autoBarLength;
        addRenderableWidget(minWidth);
        y += ROW_GAP;
        AbstractSliderButton maxWidth = new ValueSlider(x, y, width, ROW_HEIGHT,
                HudLayout.BAR_WIDTH_MIN, HudLayout.BAR_WIDTH_MAX, 1, cfg.autoBarMaxWidth,
                v -> Component.translatable("ctt_hud_fork.config.autoBarMaxWidth",
                        (int) Math.round(v)),
                v -> HudConfig.get().autoBarMaxWidth = Math.round(v));
        maxWidth.active = cfg.autoBarLength;
        addRenderableWidget(maxWidth);

        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.resetSection"), b -> {
            HudConfig.resetDebug();
            this.rebuildWidgets();
        }).bounds(this.width / 2 - 60 - 128, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("ctt_hud_fork.config.back"), b -> back())
                .bounds(this.width / 2 - 60, this.height - ROW_HEIGHT - 8, 120, ROW_HEIGHT).build());
    }

    private void back() {
        HudConfig.save();
        this.minecraft.gui.setScreen(this.menu);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
        // 没有权限 / 没有标签时**不写任何原因**（入口已经灰掉，普通玩家不需要知道细节）。
        if (!canAdjust()) return;
        graphics.centeredText(this.font, Component.translatable("ctt_hud_fork.config.debugHint"),
                this.width / 2, 40, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        back();
    }

    private static final class ValueSlider extends AbstractSliderButton {
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

        private double currentValue() {
            double raw = this.min + (this.max - this.min) * this.value;
            double snapped = Math.round(raw / this.step) * this.step;
            return Math.max(this.min, Math.min(this.max, snapped));
        }

        @Override
        protected void updateMessage() {
            if (this.label != null) setMessage(this.label.apply(currentValue()));
        }

        @Override
        protected void applyValue() {
            if (this.onChange != null) this.onChange.accept(currentValue());
        }
    }
}
