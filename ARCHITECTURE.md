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
│       └─ submitBar → collector.submitCustomGeometry(...)
│           ├─ RenderTypes.text(...)            原版世界文字管线（26.3 本来就地写深度）
│           ├─ textSeeThrough 透视副本          被墙挡住时的淡影（无深度测试）
│           └─ 底板不透明 + 填充按「血条不透明度」
│
├─ ③ 方块 HUD
│   └─ BossHealthOverlay.extractRenderState ── BossBarHudRenderMixin(Redirect Map.values)
│       └─ 过滤「最近敌人 / 队伍血量」两条上方 Boss 栏
│
├─ ④ 半透明地形（蜂蜜 / 水 / 玻璃）
│   └─ 血条已写深度 ⇒ 更远的半透明方块被深度测试挡掉；更近的照常盖住血条
│
├─ ⑤ /title
│   └─ Hud.extractTitle ── TitleTextMixin(Redirect textWithBackdrop ×2)
│       └─ 位移 / 不透明度 / 缩放 + 「文字盒不出屏」钳制
│
└─ ⑥ GUI（最后画，永远最上层）
    ├─ SelfStatsPanel（HUD 元素）
    ├─ ShieldIndicatorHud（HUD 元素）
    └─ Screen.extractRenderState ── ContainerPanelMixin(TAIL)
        └─ SelfStatsPanel.drawOnContainer（容器 / 箱子 / 背包界面上的面板）
```

**上下关系规则**

| 层 | 内容 | 会不会被盖住 |
| --- | --- | --- |
| 世界不透明方块 | 墙、地板 | 挡住血条 / 名字 / 面板之外的一切 |
| 血条 + 头顶文字 | 名字牌批次，原版 TEXT 管线（`writeDepth = true`） | 被更近的方块挡住；不会被子透明地形冲淡 |
| 半透明地形 | 蜂蜜 / 水 / 玻璃 | 只能盖住**不写深度**的内容（原版名字牌、粒子） |
| GUI | 属性面板 / 盾牌条 / 容器面板 | 永远最上层，不受世界深度影响 |

**两条硬规则**

1. **世界内画的东西，一律晚于不透明地形、早于半透明地形**。原版世界文字（TEXT）管线本身就是「深度测试 + 写深度」（`DepthStencilState.DEFAULT = (GREATER_THAN_OR_EQUAL, true)`），所以血条不需要自建渲染类型；真要改深度行为，得换管线，而不是拿同一套管线复制一遍。
2. **GUI 层画的东西不看世界深度**，所以「被墙挡住」这类判断必须自己做（`HudTarget` / 射线）。

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

## 7. 已知但这次没动（留给后续）

| 位置 | 现状 | 建议 |
| --- | --- | --- |
| `HudConfig`（1000+ 行） | 配置字段 + 位置换算 + 面板数学 + 迁移表混在一起 | 拆 `ConfigStore`（读写/迁移）与 `PanelLayout`（坐标数学） |
| `BossBarTracker`（568 行） | 抓取 + 解析 + 缓存 + 自动开关 | 拆「解析（纯函数）」与「状态缓存」 |
| 6 个 per-mode 字段 | `container*` / `both*` / `bothContainer*` 的列数与不透明度是 v7/v8/v9 迁移载体，当前渲染只读全局值 | 已在字段注释标注；下一轮大版本可删字段并停写 JSON |
| 5 个页面的底部按钮对 | 几何完全重复（120×20 于 `width/2-60-128`） | 抽 `footer(reset, back)` |
| `ScrollPane` 与过滤页滚动条 | 条宽 4 vs 3、滑块最小 12 vs 8、像素 vs 行 | 参数化后合并（直接替换会改变外观） |
| 关掉「隐藏 below_name 分数」时 | 原版会把名字上抬 10.35 px，与我们的条 / 文字行重叠 | 该组合下整体下移 `TEXT_TOP / BAR_TOP`，或对被接管的实体强制摘分数 |
| 文字与血条各有「正常 + 透视」两份 | 正常份在 `texts`、血条透视份在 `translucentCustomGeometry`、文字透视份在 `seeThrough`，三个阶段遮挡表现不一致 | 统一成一份 + 一个透视阶段，或在 noDepth 模式下只留透视份 |
| 每帧分配 | `HudLayout.compute` 每实体每帧重建 Row/List、`toComponent` 每行 new Component、`BossBarTexture.slices` 每段 new 数组 | 按「数据版本 + 宽度」缓存；`slices` 用静态数组 |

## 8. 改代码时的检查清单

1. **新配置字段**：`Cfg` 字段 → `normalize` 夹取 → 对应 `resetXxx` → lang 键（中英）→ 界面。
2. **新设置页**：`back()` / `onClose()` 都要 `HudConfig.save()`；parent 关系写清楚。
3. **新渲染内容**：先决定「世界内还是 GUI」；世界内的默认会被半透明地形覆盖，需要不被冲淡就得写深度。
4. **新 mixin**：注入点全部 `require = 0` 兜底；不要假设原版方法签名长期稳定。
5. **改默认值**：在 `RULES` / `FLAG_RULES` 加一条「旧默认 → 新默认」，并确认读取顺序仍是「迁移在归一化之前」。
6. **改完跑**：`gradle build --offline`（会跑 `selfCheck`，5,070 条断言）。
