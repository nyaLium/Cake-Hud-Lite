# 架构与顺序 · CTT HUD - Fork

> 目的：把「谁先算、谁后画、谁能盖住谁、配置怎么读」固定下来，后续加功能时知道该往哪儿接。
> 竖状图从**最外层**往**最内层**读；`├─` / `└─` 表示同一层的先后。
> 详见各文件顶部 javadoc；本文件只讲「上下关系 / 先后关系」。

代码规模：`mod/src/main` 共 49 个 java 文件 / 约 8,800 行；`src/test` 自检 5,070 条断言。

---

## 0. 三条竖线

| 竖线 | 入口 | 一句话 |
| --- | --- | --- |
| **启动线** | `CttHudForkClient.onInitializeClient` | 注册 HUD 元素、tick、聊天钩子、payload、进出世界回调、F8 |
| **每帧线** | 原版渲染回调（mixin） | 实体状态 → 名字牌批次里提交血条/文字 → 半透明地形 → `/title` → GUI |
| **配置线** | `HudConfig.load/save` | 解析 → 迁移 → 归一化 → 内存；界面直接改内存，离开页面写盘 |

---

## 1. 竖状图 A：启动与注册（一次性）

```
ClientModInitializer.onInitializeClient        CttHudForkClient
├─ HudConfig.load()                            配置（缺失则写默认）
├─ HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, …)
│   ├─ "ctt-hud-fork:self_stats"  → SelfStatsPanel      属性面板（GUI）
│   └─ "ctt-hud-fork:shield_indicator" → ShieldIndicatorHud  盾牌计量条（GUI）
├─ ClientTickEvents.END_CLIENT_TICK
│   ├─ SelfStats::tick            每 tick 判「该不该再问一次 /trigger ViewStats」
│   └─ CttHudForkClient::tickKeys  F8 → HudConfigScreen
├─ KeyMappingHelper.registerKeyMapping("key.ctt_hud_fork.open_config", F8)
├─ ClientReceiveMessageEvents.ALLOW_GAME
│   ├─ SelfStats.onGameMessage(...)   属性面板收行 / 吞自己触发的回显
│   └─ ChatTriggerFilter.shouldHide(…) 兜底过滤 "Triggered [ViewStats]"
├─ ClientMobHealthCache.register()     服务端 payload（按 UUID 的权威血量）
├─ ClientPlayConnectionEvents.JOIN
│   ├─ BerserkDetector.reset / CurseDetector.reset
│   ├─ HudWorldRenderer.resetPercentCache
│   └─ SelfStats.onWorldJoined（进服静默 3 s 再问属性）
└─ ClientPlayConnectionEvents.DISCONNECT
    ├─ ClientMobHealthCache.reset
    └─ SelfStats.reset + 两个检测器 + 百分比缓存
```

---

## 2. 竖状图 B：一帧之内的绘制顺序（谁能盖住谁）

```
帧
│
├─ ① 实体状态提取（CPU）
│   └─ EntityRenderer.extractRenderState  ── NameTagScoreMixin(TAIL)
│       ├─ HudRenderBridge.attach(state, entity)   渲染状态 → 实体（submit 阶段只能拿到状态）
│       ├─ alwaysShowEnemyName → 补 state.nameTag
│       └─ hideBelowNameScore → state.scoreText = null（顺带把名字落回原行）
│
├─ ② 实体提交（世界内、相机空间）
│   └─ EntityRenderer.submitNameDisplay  ── NameTagHudMixin(TAIL) → HudWorldRenderer.submit
│       ├─ HudRenderGate.suppressAll（未知诅咒 → 整组不画）
│       ├─ HudRenderBridge.entityOf(state)
│       ├─ HudTarget（归属 / 距离 / 遮挡）
│       ├─ 数据：ClientMobHealthCache → EnemyData → BossBarTracker
│       ├─ HudLayout.compute → 行 / 条宽
│       ├─ submitRows → collector.submitText(...)          原版世界文字渲染类型
│       │   ├─ NORMAL        深度测试 + 写深度（solid / texts 阶段）
│       │   └─ SEE_THROUGH   进 SubmitNodeStorage.seeThrough()，最后单独一遍、无深度附件
│       └─ submitBar → collector.submitCustomGeometry(...) + SeeThroughQuads.submit(...)
│           ├─ RenderTypes.text(...)            实心层：深度测试 + 写深度
│           └─ SeeThroughQuads（textSeeThrough）透视层：塞进 seeThrough 阶段，带原版纹理
│
├─ ③ 方块 HUD
│   └─ BossHealthOverlay.extractRenderState ── BossBarHudRenderMixin(Redirect Map.values)
│       └─ 过滤「最近敌人 / 队伍血量」两条上方 Boss 栏
│
├─ ④ 半透明地形（蜂蜜 / 水 / 玻璃）
│   └─ 只影响实心层（关掉「血条不可透视」时才提交）：血条已写深度 ⇒ 更远的被挡掉、更近的照常盖住
│      透视层在这之后画，半透明方块碰不到它
│
├─ ⑤ /title
│   └─ Hud.extractTitle ── TitleTextMixin(Redirect textWithBackdrop ×2)
│       └─ 位移 / 不透明度 / 缩放 + 「文字盒不出屏」钳制
│
├─ ⑥ See-through features（文字透视副本 / 血条透视副本 / 原版名字的透视层）
│   ├─ 在所有地形之后、一份**没有深度附件**的 render pass 里画 ⇒ 永远压在最上面
│   └─ 血条透视层与文字同阶段：`SeeThroughQuads`（自定义 TranslucentSubmit + FeatureRenderer）
│
└─ ⑦ GUI（最后画，永远最上层）
    ├─ SelfStatsPanel（HUD 元素）
    ├─ ShieldIndicatorHud（HUD 元素）
    └─ AbstractContainerScreen.extractRenderState ── ContainerPanelMixin(TAIL)
        └─ SelfStatsPanel.drawOnContainer（容器 / 箱子 / 背包界面上的面板）
```

**上下关系规则**

| 层 | 内容 | 会不会被盖住 |
| --- | --- | --- |
| 世界不透明方块 | 墙、地板 | 挡住实心层（文字 / 血条）/ 名字；挡不住透视层 |
| 血条 + 头顶文字（NORMAL） | 名字牌批次，原版 TEXT 管线（`writeDepth = true`） | 被更近的方块挡住；不会被半透明地形冲淡 |
| 半透明地形 | 蜂蜜 / 水 / 玻璃 | 只能盖住**不写深度**且画在它之前的几何 |
| 透视层（文字 / 血条 SEE_THROUGH） | `SubmitNodeStorage.seeThrough()` | 单独一遍、无深度附件 ⇒ 什么也盖不住它 |
| GUI | 属性面板 / 盾牌条 / 容器面板 | 永远最上层，不受世界深度影响 |

**三条硬规则**

1. **世界内画的东西，一律晚于不透明地形、早于半透明地形**。原版世界文字（TEXT）管线本身就是「深度测试 + 写深度」（`DepthStencilState.DEFAULT = (GREATER_THAN_OR_EQUAL, true)`），所以实心层不需要自建渲染类型；真要改深度行为，得换管线，而不是拿同一套管线复制一遍。
2. **GUI 层画的东西不看世界深度**，所以「被墙挡住」这类判断必须自己做（`HudTarget` / 射线）。
3. **「透视」是阶段，不是渲染类型**。`submitText(DisplayMode.SEE_THROUGH)` 会把节点送进 `SubmitNodeStorage.seeThrough()`，由 `LevelRenderer.executeSeeThrough` 在所有地形之后、于一份没有深度附件的 pass 里单独画。`submitCustomGeometry` 本身没有这个阶段可选（只按 `hasBlending()` 进 `translucentCustomGeometry` 或 `solid`），**但可以把自定义节点直接塞进 `SubmitNodeStorage.seeThrough()`**：需要同时提供一个 `TranslucentSubmit`（记录 pose / 距离 / 各层 `RenderType` + 几何）和一个 `RenderTypeFeatureRenderer` 子类，并在 `FeatureRenderDispatcher` 构造时 `put` 进 `FeatureRendererMap` —— 这就是 `SeeThroughQuads` 做的事。注意这一遍没有深度附件，层里只能用不依赖深度的管线（`RenderTypes.textSeeThrough`），且执行时 `OitStage == null`，不要求 OIT 管线集。

---

## 3. 竖状图 C：一条实体血条（tick → 提交）

```
每 tick（20 Hz）
├─ 服务端 payload → ClientMobHealthCache（按 UUID 存，带 TTL）
├─ HudWorldRenderer.refresh（内部节流）
│   ├─ BossBarTracker.snapshot / resolve     上方 Boss 栏 → 名字 + 血量
│   ├─ BossBarTracker.resolveTeam            队伍条
│   └─ PartyBossbarAutoToggle.tick           必要时自动开队伍条
└─（不变量）三条数据源优先级：payload > 计分板/名字牌 > BossBar 标题
   ↓
每次提交（每实体每帧）
└─ HudWorldRenderer.submit
    ├─ 过滤：HudRenderGate → HudTarget.managed/visible（归属 / 距离 / 遮挡 / 未知诅咒）
    ├─ 取数：ClientMobHealthCache.lookup → 回落 EnemyData.read → BossBarTracker
    ├─ 布局：HudLayout.compute(...)（纯函数，自检直接打）
    ├─ 轮换：HeartCycle / ArmourCycle / ArmourCache（心数与护甲行）
    └─ 绘制：submitRows（文字） + submitBar（背景 / 填充 / 透视副本）
```

---

## 4. 竖状图 D：属性面板（聊天 → 面板）

```
每 tick
└─ SelfStats.tick
    ├─ 进服静默 3 s（quietUntil）
    ├─ 每 N 秒发一次 /trigger ViewStats（N = 配置，或被服务端强制）
    └─ 连续失败 → 退避（DENIED_BACKOFF）
       ↓ 服务端 tellraw 一串行
ClientReceiveMessageEvents.ALLOW_GAME
└─ SelfStats.onGameMessage
    ├─ 标题行 → 开一趟采集（beginCapture）
    ├─ 普通行 → SelfStatsCatalog.analyze
    │   ├─ flat 文本 + 逐字符颜色
    │   ├─ 翻译键（Misc_View_Stats_* / view_stats_*）
    │   └─ 计分板目标名（ScoreContents.objective）
    ├─ classify 优先级：HINT > TIMER > hover 键 > 目标名 > 标签键 > 图标+颜色 > MISC
    ├─ 去重身份串 = 分类 + 文本 + 目标名 + hover 键 + 颜色
    ├─ 计时行 → endCapture（PENDING → SHOWN，按 selfStatsHidden 过滤）
    └─ 自己发的回显 → 吞掉（不显示在聊天栏）
       ↓
SelfStatsPanel（GUI）
├─ lines()   按隐藏类过滤后的行
└─ hoverAt() 原版 hover 文本（鼠标悬停显示属性说明）
```

---

## 5. 竖状图 E：设置界面树与配置读写顺序

```
Mod Menu「Config…」 / 游戏内 F8
└─ HudConfigScreen                       总开关 · 重置 · 完成
   ├─ 血条设置…  EntityHudSettingsScreen   通用 / 敌人 / 玩家（滚动列表）
   ├─ 属性面板设置… SelfStatsSettingsScreen
   │   ├─ 调整位置…   SelfStatsLayoutScreen   拖动定位（返回即写盘）
   │   └─ 显示内容过滤… SelfStatsFilterScreen 96 项（勾选即写盘）
   ├─ 盾牌指示器设置… ShieldIndicatorSettingsScreen
   │   └─ 调整位置…   ShieldLayoutScreen      拖动定位（返回即写盘）
   ├─ 标题设置…   TitleSettingsScreen    左列标题 / 右列副标题
   └─ 调试功能…   DebugSettingsScreen    需 op≥2 且带 DEBUG 标签，否则入口灰掉
```

```
配置读取（load / 每 2 s 热加载）
  JSON 文本
    ├─ Gson 解析
    ├─ migrate   旧默认值 → 新默认值（只在「仍等于旧默认」时改，幂等）
    └─ normalize 夹取范围 + 0.05 步进字段 round2 + 非法值回落
配置写盘（save）
  ├─ normalize（同一套夹取）
  └─ 写文件 + 更新 lastModified（避免自己触发热加载）
```

> **顺序不能反**：`migrate` 必须在 `normalize` 之前。反过来的话，旧默认值会先被夹进新范围，迁移规则（「仍等于旧默认才改」）就永远匹配不到。

---

## 6. 本次整理（1.6.15）改了什么

### 6.1 回退 / 纠正

- **删掉 1.6.14 的自建「写深度」渲染管线**（`BarRenderTypes` + `RenderTypeAccessor`）。反编译核对 26.3 后确认：原版世界文字管线 `DepthStencilState.DEFAULT` 就是 `(GREATER_THAN_OR_EQUAL, writeDepth = true)`，自建版本与之等价、改动无效；而且它没设 `setOitPipelines(...)`，用户开启「改进的透明（OIT）」时绘制阶段会抛 `IllegalStateException`。血条回到 `RenderTypes.text(...)`。
- 保留 1.6.13 的**不透明底板**：那一条针对的是「65% 底板与明亮半透明背景混色」，与深度无关，确实有效。

### 6.2 渲染 / 线程

- **容器界面上的属性面板改挂 `AbstractContainerScreen.extractRenderState` 的 TAIL**。以前挂 `Screen.extractRenderState`，对容器界面来说落点在 `extractContents` 开头，面板会被槽位物品 / 物品名 / 高亮压住（和注释写的“画在容器界面之上”相反）。
- **「血条不可透视」关闭（`noDepth` 模式）时也保留不透明底板**，否则该模式底板退回半透明，半透明方块又糊上来。
- **队伍条自动开关从渲染线程挪到 `ClientTickEvents`**：以前在 `HudWorldRenderer.refresh()`（渲染线程、50 ms 一次）里发 `trigger` 命令并推动状态机。
- **统一闸门补齐**：`ShieldIndicatorHud` 加 `HudRenderGate.suppressAll`；`TitleTextMixin` 在总开关关闭时直接原样交还原版（不再改位移 / 缩放 / 不透明度）。

### 6.3 去重 / 收敛

- 5 个设置页各自私有、**逐行相同**的 `ValueSlider` → 合并成共享 `ValueSlider`（净减约 105 行；键盘处理统一回原版语义）。
- 删除 `SelfStatsPanelAccess` 纯转发桥接；`SelfStatsPanel.drawOnContainer` 改为 `public`。
- 两个拖动页里内联的「左键判定 + 坐标换算」改用共享 `Pointer`。
- 删除死代码：`HudConfig.panel(int, boolean)` 两参重载、`setPanelColumns` / `setPanelOpacity`（无生产调用者）、属性面板页里带副作用的 `HudConfig.panel(...)` 调用。

### 6.4 配置 / 界面

- `HudConfig.read()` 顺序改为「解析 → **迁移** → 归一化」；反过来的话旧默认值会先被夹进新范围，迁移规则永远不命中。
- 所有 0.05 步进字段在 `normalize` 里 `round2`，不再把 `0.9000000000000001` 写进 JSON。
- 三个二级页（两个「调整位置」+「过滤」）离开时写盘；过滤页每次勾选即时写盘并从配置重新取列表（防热加载换对象后丢改动）。
- `resetPanel()` 补 `selfStatsHidden`、清零位置基准；`resetPanelPosition()` 同样清基准（默认位置不再被旧窗口尺寸放大成 8 px）。
- 根页「重置」在无调试权限时保留调试字段（`resetPreservingDebug`），不再绕过调试页门槛。
- 「玩家」组 5 个滑块改用独立文案键（补 5 个键、删 5 个死键）；距离滑块与文档 / `normalize` 统一到 4~256。
- 属性面板总开关关闭时，「显示方式 / 过滤」仍可调。
- 注释纠正：过滤项数 88 → 96、标题页默认值（0.60 / -135 / 0.5~3.0）、过期注释若干。

### 6.5 1.6.16：血条为什么会被半透明方块冲淡（而文字不会）

| | 文字行 | 血条 |
| --- | --- | --- |
| 提交方式 | `submitText(..., DisplayMode.SEE_THROUGH)` | `submitCustomGeometry(pose, RenderTypes.textSeeThrough(...))` |
| 落到哪个阶段 | `SubmitNodeStorage.seeThrough()`（独立一遍） | `translucentCustomGeometry`（按 `hasBlending()` 选的） |
| 这一遍什么时候画 | `LevelRenderer.executeSeeThrough`：**所有地形（含半透明）之后** | `executeClassicTransparency` 内：**半透明地形之前** |
| 有没有深度附件 | 没有（`withColorAttachment` 只给颜色） | 有，但 `TEXT_SEE_THROUGH` 管线是 `DepthStencilState(Optional.empty())`（不测、不写） |
| 结果 | 谁也盖不住它 | 随后画的蜂蜜 / 水 / 玻璃深度测试放行，直接混到条上 ⇒ 看起来「变透明」 |

1.6.16 的改法是**血条只用 `RenderTypes.text(...)` 提交一次**（深度测试 + 写深度），删掉透视副本；「血条不可透视」也不再切无深度通道。同时避免了 `textSeeThrough` 在 OIT 下没有 OIT 管线集、`drawFromBufferOit` 会抛 `IllegalStateException` 的隐患。

同时把 1.6.13 写死的「底板永远不透明」改回来：底板与填充共用 `barOpacity` 的 alpha。写死那一条本来是为了压住「半透明方块糊上来」，而那是阶段问题；写死的副作用是「血条不透明度」只改进度纹理、外框一直不透明。底板照旧写深度，更远的半透明方块仍被挡掉。

### 6.6 1.6.17：遮挡射线把「看得见」判成「被挡住」

`HudTarget.occludedByBlocks` 是「血条不可透视」的唯一闸门，它用 `seeThroughBlock` 的白名单决定哪个方块算「能看过去」。原来的白名单只有 **玻璃系 + 植物**，蜂蜜块不在里面 —— 于是隔着蜂蜜看实体时，射线第一下就打中蜂蜜 → 判成「被墙挡住」→ 整组 HUD（数字 / 心数 / 血条）**一条都不提交**，而原版名字因为自带一对最上层的 SEE_THROUGH 副本照样显示，看上去就是「血条被方块挡掉了」。

1.6.17 做了两件事：

1. 白名单加入 `honey_block`（隔着蜂蜜明明看得见，原版也照画名字）；冰类 / 铜格栅 / 树叶仍按用户口径算挡，层数上限仍是 1 层。
2. `clear()` 跳过**眼睛所在的那一格**：贴墙、站在薄方块边缘、旁观穿模时，射线起点就在方块内部，`clip` 会立刻返回一个距离 0 的命中，把所有实体都判成被挡。现在这一格既不判挡、也不计入层数。

### 6.7 1.6.18：血条重新拥有「透视副本」，而且不再被半透明方块染色

1.6.16 为了修「半透明方块糊到血条上」把血条的透视副本整个删了，代价是：天花板 / 屋檐 / 墙沿会像切实心几何那样把条切掉，而它上方的文字和名字还有透视层 —— 看起来就是「名字还在、血条没了」。1.6.17 之后又把「不可透视」误解成「条不能画在方块上面」，问题仍在。

1.6.18 的做法是把透视层**放回它本来就该在的阶段**：

| | 1.6.15（被冲淡） | 1.6.16 / 1.6.17（被切掉） | 1.6.18（当前） |
| --- | --- | --- | --- |
| 血条透视层 | `submitCustomGeometry` + `textSeeThrough` ⇒ `translucentCustomGeometry`，**半透明地形之前** | 没有 | `SeeThroughQuads` ⇒ `SubmitNodeStorage.seeThrough()`，**所有地形之后** |
| 被方块切掉 | 否 | 是 | 否 |
| 被蜂蜜 / 水 / 玻璃染色 | 是 | 否 | 否 |
| 纹理 | 原版 `boss_bar` | 原版 `boss_bar` | 原版 `boss_bar`（**没有**换纯色矩形） |

实现要点：

1. `SeeThroughQuads implements TranslucentSubmit`：节点里存 billboard pose（`pose.last().copy()`）、`computeDistanceToCameraSq` 与若干 `Layer(renderType, geometry)`；一张条把「背景 + 进度」放进同一个节点，绘制顺序不会被阶段的距离排序打乱。
2. `SeeThroughQuads.Renderer extends RenderTypeFeatureRenderer`：`buildGroup` 里按层 `getVertexBuilder(renderType)` 写同一套 `barSpan` 几何（分段 / UV 规则与实心层共用）。
3. `FeatureRenderDispatcherMixin` 在 `FeatureRenderDispatcher` 构造 TAIL 处把 renderer `put` 进 `FeatureRendererMap`；不注册的话 `PreparedFrame` 会在 `getOrThrow` 抛异常。
4. `submitBar` 的行为：**「血条不可透视」开（默认）** ⇒ 只提交透视层（配置的不透明度），「看不看得见实体」完全由 `HudTarget.occludedByBlocks` 的射线决定；**关** ⇒ 实心层 + 约 50% 的透视副本，和原版名字同一套规则。

### 6.9 1.6.22：遮挡射线把 `clip` 的 MISS 当成命中

`BlockGetter.clip` 在「什么都没打中」时**不返回 null**，而是返回 `HitResult.Type.MISS`，
其 `getBlockPos()` 正是**终点所在的那一格**。`HudTarget.clear()` 原来只判
`instanceof BlockHitResult`，于是实体站在水里时，终点格 `water` 被当成「挡板」→ 整组 HUD 不画。
**修复**：先判 `hit.getType() == HitResult.Type.BLOCK` 再取方块。同一版把碰撞上下文从
`mc.player` 换成 `CollisionContext.empty()`（26.3 的 `LiquidBlock.getCollisionShape` 只在
「该实体正与流体碰撞」时返回方块形状，玩家游泳会污染判定）。

### 6.10 1.6.23：口径收窄为「只看碰撞体积」

用户决定：**只有无碰撞体积的方块不挡**。射线本来就是 `Block.COLLIDER` + `CollisionContext.empty()`，
即「能被打中 ⇔ 有碰撞体积」，所以**材质白名单是多余的**：

| | 1.6.0 ~ 1.6.22 | 1.6.23 |
| --- | --- | --- |
| 玻璃系（34 个 id） | 不挡（最多 1 层） | **挡** |
| 蜂蜜块 | 不挡 | **挡** |
| 植物 / 树叶 | 植物不挡 / 树叶挡 | 无碰撞箱 ⇒ 不挡；树叶有碰撞箱 ⇒ 挡 |
| 水 / 熔岩 / 火把 / 告示牌 / 按钮 / 雪层 / 蜘蛛网… | 不挡 | 不挡（打不到） |
| 石头 / 台阶 / 楼梯 / 栅栏 / 箱子 / 冰 / 铜格栅 | 挡 | 挡 |

代码上删掉了 `seeThroughBlock`、`seeThroughId`、`MAX_SEE_THROUGH_DEPTH`：`clear()` 里
「打中方块且不是眼睛格 ⇒ 挡；MISS ⇒ 通」。

## 7. 已知但这次没动（留给后续）

| 位置 | 现状 | 建议 |
| --- | --- | --- |
| `HudConfig`（1000+ 行） | 配置字段 + 位置换算 + 面板数学 + 迁移表混在一起 | 拆 `ConfigStore`（读写/迁移）与 `PanelLayout`（坐标数学） |
| `BossBarTracker`（568 行） | 抓取 + 解析 + 缓存 + 自动开关 | 拆「解析（纯函数）」与「状态缓存」 |
| 6 个 per-mode 字段 | `container*` / `both*` / `bothContainer*` 的列数与不透明度是 v7/v8/v9 迁移载体，当前渲染只读全局值 | 已在字段注释标注；下一轮大版本可删字段并停写 JSON |
| 5 个页面的底部按钮对 | 几何完全重复（120×20 于 `width/2-60-128`） | 抽 `footer(reset, back)` |
| `ScrollPane` 与过滤页滚动条 | 条宽 4 vs 3、滑块最小 12 vs 8、像素 vs 行 | 参数化后合并（直接替换会改变外观） |
| 关掉「隐藏 below_name 分数」时 | 原版会把名字上抬 10.35 px，与我们的条 / 文字行重叠 | 该组合下整体下移 `TEXT_TOP / BAR_TOP`，或对被接管的实体强制摘分数 |
| 文字 / 血条的透视层顺序 | 两者都在同一遍 `seeThrough` 阶段、按距离排序；同一张条的「背景 + 进度」在一个节点里所以内部有序，但不同实体的条之间只按距离排 | 如果出现「近条被远条压住」，给节点一个「同距离时的稳定次序」或把条拆成单个节点并按相机距离反排 |
| 每帧分配 | `HudLayout.compute` 每实体每帧重建 Row/List、`toComponent` 每行 new Component、`BossBarTexture.slices` 每段 new 数组 | 按「数据版本 + 宽度」缓存；`slices` 用静态数组 |

## 8. 改代码时的检查清单

1. **新配置字段**：`Cfg` 字段 → `normalize` 夹取 → 对应 `resetXxx` → lang 键（中英）→ 界面。
2. **新设置页**：`back()` / `onClose()` 都要 `HudConfig.save()`；parent 关系写清楚。
3. **新渲染内容**：先决定「世界内还是 GUI」；世界内的默认会被半透明地形覆盖，需要不被冲淡就得写深度（`RenderTypes.text`）。想要「透视」：文字直接用 `submitText(DisplayMode.SEE_THROUGH)`；**自定义几何**要进 `seeThrough` 阶段必须像 `SeeThroughQuads` 那样，自带 `TranslucentSubmit` + `RenderTypeFeatureRenderer` 并在 `FeatureRenderDispatcher` 构造时注册 —— 只把 `textSeeThrough` 交给 `submitCustomGeometry` 只会得到「不写深度、画在半透明地形之前」，会被后面的蜂蜜 / 水 / 玻璃染色。
4. **新 mixin**：注入点全部 `require = 0` 兜底；不要假设原版方法签名长期稳定。
5. **改默认值**：在 `RULES` / `FLAG_RULES` 加一条「旧默认 → 新默认」，并确认读取顺序仍是「迁移在归一化之前」。
6. **改完跑**：`gradle build --offline`（会跑 `selfCheck`，5,070 条断言）。
