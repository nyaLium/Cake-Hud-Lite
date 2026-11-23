# 代码审计 · CTT HUD - Fork 1.20.24

> 审计范围：`Cake Team Tower HUD Fork/src/main`（43 个 java 文件 / 6,908 行）、
> `src/test/SelfCheck.java`（1,641 行 / 5,176 条断言）、5 个 client mixin、125 个语言键、
> `fabric.mod.json`、`ctt_hud_fork.mixins.json`。
> 方法：① 逐个成员提取 javadoc 与签名；② 全源集符号引用计数找死代码；
> ③ 把每个代码事实拿到**所有更新说明**（CHANGELOG + 两份 README + 49 个 `versions/v*/RELEASE.md`）里检索，
> 判断「有没有被任何一次更新提过」；④ 人工核对文档与代码的数值/描述是否一致。
> 审计脚本留在仓库外：`work/audit_members.py`、`work/audit_unused.py`、`work/audit_docs.py`。

---

## 一、整体结构与四条链路

```
客户端入口 CttEnemyHudClient
 ├─ HUD 元素①  SelfStatsPanel        → 属性面板（HUD 上画）
 ├─ HUD 元素②  ShieldIndicatorHud    → 盾牌计量条
 ├─ tick      SelfStats::tick        → 定时 /trigger ViewStats
 ├─ 聊天过滤  ClientReceiveMessageEvents.ALLOW_GAME
 │              ├─ SelfStats.onGameMessage  → 收进面板 / 决定吞不吞
 │              └─ ChatTriggerFilter.shouldHide → 吞自己发的 trigger 回显
 └─ 断线清零  DISCONNECT → SelfStats.reset + ClientMobHealthCache.reset

世界空间（头顶条） NameTagHudMixin（submitNameDisplay TAIL）
 └─ HudWorldRenderer.submit
      ├─ HudRenderBridge.entityOf        （渲染状态 → 实体）
      ├─ HudTarget.managed/visible        （归属 + 距离 + 遮挡 + CurseDetector）
      ├─ 数据：ClientMobHealthCache.lookup（服务端选装）
      │        ↘ 回落 EnemyData.read（计分板）+ BossBarTracker.resolve（bossbar 认领）
      ├─ EnemyHudLayout.compute           （纯函数：分段 / 折行 / 宽度）
      ├─ HeartCycle.pick / ArmourCycle.page / ArmourCache （轮换与缓存）
      └─ BossBarTexture.slices + RenderTypes.text / textSeeThrough（两遍提交）

队伍条（队友） 同一路径：teamOptions + BossBarTracker.teamEntriesCached + TeamBarTitle
               PartyBossbarAutoToggle 负责把这条 bossbar 打开

容器界面面板  ContainerPanelMixin（Screen.extractRenderState TAIL）
               └─ SelfStatsPanelAccess → SelfStatsPanel.drawOnContainer

服务端选装     CttEnemyHudServer（每 4 tick、64 格、最多 32 条）
               └─ MobHealthPayload → 客户端 ClientMobHealthCache（5 s 失鲜）

两条「关门」    BossBarHudRenderMixin（隐藏数据包那条敌人 bossbar）
               NameTagScoreMixin（隐藏 below_name 的 HealthPercent 分数）
```

---

## 二、逐文件职责

| 文件 | 行 | 干什么 | 关键常量 / 细节 | 调用者 |
| --- | --- | --- | --- | --- |
| `CttEnemyHudClient` | 67 | 客户端入口：注册 2 个 HUD 元素、2 个 tick、聊天过滤、断线清理、`F8` 键 | `F8` 打开设置 | Fabric client entrypoint |
| `CttEnemyHudServer` | 178 | 服务端选装组件：扫附近 CTT 怪 + 队友，差量推 payload | `INTERVAL_TICKS=4`（5 Hz）、`RADIUS=64`、`KEEPALIVE_MS=3000`、`MAX_ENTRIES=32` | Fabric main entrypoint |
| `MobHealthPayload` | 173 | S2C 包定义（`ctt-hud-fork:mob_health`） | `CURRENT_VERSION=1`、zigzag 变长编码 | 双端 |
| `ClientMobHealthCache` | 92 | 客户端按 UUID 缓存推送数据 | `FRESH_WINDOW_MS=5000` | 渲染端 / 入口 |
| `EnemyData` | 236 | 从**客户端计分板**读敌人真实数值 | 7 个护甲 objective + 颜色表；`healthPercentOf` | HudWorldRenderer / HudTarget / SelfStats |
| `BossBarTracker` | 585 | 读客户端的**额外 Boss 栏**，把每条 bar 认领到具体实体（= 精确血量 + 护甲） | `PERCENT_TOLERANCE=5`、`STICKY_BONUS=4.0`（2 格²）、队伍条短缓存 | HudWorldRenderer / HudTarget / 自动开条 |
| `BossBarTitle` | 94 | 解析 bar 标题里的 `当前/上限` 与护甲段（纯函数） | `suffixStart`、`looksLikeTeamBar` | BossBarTracker |
| `TeamBarTitle` | 196 | 解析队伍条标题 `名字 (x/y)`（严格 + 宽松两套） | `scanLoose` / `parse` | BossBarTracker |
| `BossBarTexture` | 136 | 原版 bossbar 纹理封装：颜色、端盖分段 | `NATIVE_WIDTH=182`、`CAP_TEXELS=4`、`slices()` | HudWorldRenderer |
| `HudWorldRenderer` | 380 | 世界空间头顶条实现（两遍提交、分段、着火前推、透视副本） | `CACHE_NS=50ms`、`FIRE_PUSH_MAX=1.2`、透视 alpha≈128/255 | NameTagHudMixin |
| `HudRenderBridge` | 36 | 渲染状态 → 实体 的 `RenderStateDataKey` 桥 | — | NameTagScoreMixin / HudWorldRenderer |
| `HudTarget` | 135 | 判定「归我管 / 这一帧画不画」：距离、旁观、诅咒、墙后 | `MEMO_NS=8ms` | HudWorldRenderer |
| `BoxOcclusion` | 44 | 屏幕空间遮挡覆盖比例（纯函数） | `DEFAULT_THRESHOLD=0.70` | **无人调用（见第四节）** |
| `CurseDetector` | 85 | 客户端探测「未知诅咒」（读 bossbar 翻译键） | 前缀 `Floors_Curse_Rooms_D_Curse02_Unknown_`、`PROBE_INTERVAL_MS=100`、`RELEASE_DELAY_MS=2000` | HudTarget / HudWorldRenderer |
| `EnemyHudLayout` | 388 | 纯函数布局：分段、折行、宽度、k/m/b 缩写 | `TEXT_TOP=-18`、`BAR_TOP=-7`、`NUM_EXACT_LIMIT=100000`、`ARMOUR_PAGE_SIZE=2` | HudWorldRenderer / 自检 |
| `HeartCounts` | 127 | 各类心数量模型（红/魂/黑/蓝/碎/粉） | `UNKNOWN=Integer.MIN_VALUE` | 布局 / 渲染 / 测试 |
| `HeartCycle` | 63 | 除红心外「循环显示哪一类心」 | `KIND_MS=2000`、敌/玩家两套顺序 | HudWorldRenderer |
| `ArmourCycle` | 28 | 元素护甲翻页时钟 | `PAGE_MS=2000` | HudWorldRenderer |
| `ArmourCache` | 69 | 元素护甲 8 槽的跨帧缓存（bossbar 是逐 tick 轮换的） | `SLOT_TTL_MS=18000`、`ENTRY_TTL_MS=30000` | BossBarTracker / HudWorldRenderer |
| `ChatTriggerFilter` | 76 | 吞掉「本模组自己发的 trigger」的回显与报错 | `ECHO_WINDOW_MS=3000`、`MUTED_TRIGGERS={TogglePartyBossbar,ViewStats}`、`commands.trigger.failed*` | 入口 / 自动开条 / 自检 |
| `PartyBossbarAutoToggle` | 96 | 自动打开「队伍血量」bar（队友数据唯一来源） | `COOLDOWN_MS=3000`、`MAX_ATTEMPTS=5`、`FAILSAFE_MS=600000`、**`REOPEN_DELAY_MS=10000`**、**`MAX_SENDS=20`** | HudWorldRenderer.refresh |
| `EnemyHudConfig` | 564 | 配置模型 + 迁移 + 夹取 + 三套面板参数 | `CURRENT_VERSION=11`、`FILE_NAME=ctt-hud-fork.json`、粗类展开表 | 全部 |
| `EnemyHudConfigScreen` | 100 | 外层菜单：总开关 + 3 个入口 + 恢复默认 + 完成 | — | `F8` / Mod Menu |
| `EntityHudSettingsScreen` | 256 | 实体血条设置（可滚动 + 真裁剪） | `BOTTOM_PAD=56`、组灰化 | 外层菜单 |
| `SelfStatsSettingsScreen` | 288 | 属性面板设置（可滚动、模式循环、对象切换） | `nextMode` 2→0→1→2 | 外层菜单 |
| `SelfStatsLayoutScreen` | 235 | 「移动面板」：绝对拖动 + 方向键 + 就地切对象 | `fitInScreen` 夹取 | 属性面板设置 |
| `SelfStatsFilterScreen` | 224 | 内容过滤：双列按钮 + 行滚动 | `COLUMN_WIDTH=200`、`TOP=54`、`BOTTOM_PAD=40` | 属性面板设置 |
| `SelfStats` | 839 | 属性面板数据源：触发 `/trigger ViewStats`、抓聊天、分类、过滤、时间冻住判定 | `CAPTURE_TIMEOUT_MS=3000`、`MAX_LINES=64`、`MAX_EMPTY_CAPTURES=2`、`DENIED_BACKOFF_MS=10000`、`GATE_RECHECK_MS=5000`、`TIME_STALE_MIN_GAP_MS=1500` | 入口 tick / 聊天过滤 |
| `SelfStatsKind` | 139 | 一行内容的归类（60 个可过滤类 + HINT） | `FILTERABLE`、`byId` | 面板 / 过滤页 |
| `SelfStatsPanel` | 239 | 面板绘制（1~3 列、缩放、透明度、容器 hover） | `ROW_HEIGHT=10`、`COLUMN_GAP=8`、示例行 `demoLines()` | HUD 元素 / mixin |
| `SelfStatsPanelAccess` | 18 | 给 mixin 的公开桥 | — | ContainerPanelMixin |
| `ShieldIndicatorHud` | 112 | 盾牌计量条（判定 `custom_data.shield/shieldAbility`、耐久四档） | 纹理 36×106、缩放 0.5~1.5 | HUD 元素 |
| `ShieldIndicatorSettingsScreen` | 125 | 盾牌设置：开关 / 缩放 / 不透明度 / 移动 | — | 外层菜单 |
| `ShieldLayoutScreen` | 183 | 「移动盾牌指示器」：绝对拖动 + 方向键 | `-1` = 跟随副手槽 | 盾牌设置 |
| `ModMenuIntegration` | 16 | Mod Menu 入口（可选依赖） | — | modmenu entrypoint |
| mixin `NameTagHudMixin` | 32 | 在 `EntityRenderer.submitNameDisplay` 尾部注入，画头顶条 | TAIL | — |
| mixin `NameTagScoreMixin` | 38 | 隐藏 `below_name` 分数（并挂 `HudRenderBridge`） | — | — |
| mixin `BossBarHudRenderMixin` | 65 | 隐藏数据包给的敌人 bossbar（画自己的） | — | — |
| mixin `BossHealthOverlayAccessor` | 24 | 读 `BossHealthOverlay` 的私有 map | `@Accessor` | BossBarTracker |
| mixin `ContainerPanelMixin` | 30 | 在容器界面上叠面板 | TAIL of `Screen.extractRenderState` | — |

---

## 三、代码里有、但**任何一次更新说明都没提过**的部分

检索范围：`CHANGELOG.md`（1.10.0→1.20.24 全部条目）、`README.md`、`Cake Team Tower HUD Fork/README.md`、
`versions/v*/RELEASE.md`（49 个）。

### 3.1 完全没出现过

| 代码事实 | 在哪 | 说明 |
| --- | --- | --- |
| `GATE_RECHECK_MS = 5000` | `SelfStats.tick` | 不在对局时每 **5 s** 才重查一次计分板（省性能），从未写进任何说明 |
| `MAX_LINES = 64` | `SelfStats` | 一趟最多收 64 行；超出直接丢弃 |
| `CACHE_NS = 50 ms` | `HudWorldRenderer.refresh` | 一帧内 Boss 栏 / 队伍条只解析一次 |
| `MEMO_NS = 8 ms` | `HudTarget` | `visible()` 的 8 ms 记忆化 |
| `PROBE_INTERVAL_MS = 100` / `RELEASE_DELAY_MS = 2000` | `CurseDetector` | 诅咒探测每 100 ms 一次、键消失后再压 2 s（防翻页抖动） |
| `SelfStatsPanelAccess` | `mixin/` | mixin 与包可见面板之间的公开桥，纯内部 |
| `BossBarTitle.suffix()/suffixStart` | `BossBarTitle` | 从标题里截「右括号之后的护甲段」 |
| `SelfStatsKind.byId` / `FILTERABLE` 顺序 | `SelfStatsKind` | 过滤页按钮顺序即数组顺序 |
| `ScrollPane.scrollToTop` | `ScrollPane` | 无人调用（死代码，见第四节） |

### 3.2 只提过概念、没提过数值 / 机制

| 代码事实 | 文档里怎么写的 |
| --- | --- |
| `ArmourCache`：`SLOT_TTL_MS=18 s`、`ENTRY_TTL_MS=30 s` | 只说「护甲段缓存」，没写 TTL |
| `REOPEN_DELAY_MS=10 s`、`MAX_SENDS=20`（自动开队伍条） | README 的表格里**有**这两条行为；但 CHANGELOG / RELEASE 从未提过，常量名也没出现（1.20.25 已补进 README 常量表） |
| `NUM_EXACT_LIMIT=100000` | 只说「大数值 k/m/b」，没说从 10 万开始缩写 |
| `MAX_ENTRIES=32` / `INTERVAL_TICKS=4` | 只说「5 Hz 差量 + 3 s keepalive」，没说一次最多 32 条 |
| `EDGE_KEEP=16`（旧拖拽夹取） | 1.20.23 说明里提了「整块留屏内」，旧的 16 px 规则没写，且已被 `fitInScreen` 取代 |
| `demoLines()`（移动面板的示例行） | 文档只写「用示例行撑预览」，没写是哪几行 |
| `STICKY_BONUS=4.0`（=2 格²） | 只写「粘性 2 格」，没写是**平方格**单位 |
| `writeSigned` zigzag 变长编码 | README 协议节提到 zigzag，但没写具体编码方式 |
| `COARSE_KINDS` 粗类展开表 | 只写「旧粗类自动展开」，没写映射内容 |
| `columnOf/columnOffsets`（列优先填充） | 只写「1~3 列」，没写填充顺序 |
| 透视副本 alpha ≈ 128/255（≈50%）、背景 25% | 只写「恢复文字透视副本」，没写透明度 |

### 3.3 代码里有、文档没有的「隐形开关」

| 项 | 现状 |
| --- | --- |
| `debug` | 唯一作用是 `SelfStatsPanel` 左下角画一行调试文本（面板位置/列数/缩放/行数/gui 尺寸）。文档只在 ARCHITECTURE 里提「调试开关」 |
| `hideBehindEntities` | 字段默认 `true`，`normalize()` 强制 `false`，**没有任何代码读它** —— 配置里会一直写 `false`，玩家看到却完全不生效 |
| `hideBehindWalls` | 仍被 `HudTarget` 读，但同样被强制 `false` |
| `autoPartyBossbar` | 仍被 `HudWorldRenderer.refresh` 读，被强制 `true` |
| `ChatTriggerFilter` 的 3 s 回显窗口 | 只吞「自己刚发命令后 3 s 内」的 trigger 回显与 `commands.trigger.failed*` |

---

## 四、死代码 / 未使用成员

（`work/audit_unused.py` 全源集引用计数，含自检）

### 4.1 彻底没人用（自检也没引用）

| 成员 | 位置 | 建议 |
| --- | --- | --- |
| `BoxOcclusion`（整个类） | `BoxOcclusion.java` | 遮挡功能已写死关闭 → 删除或标注 `@Deprecated` 保留 |
| `EnemyHudConfig.copy()` / `restore(Cfg)` | `EnemyHudConfig` | 旧「取消」回滚用，界面已无「取消」→ 删 |
| `EnemyHudConfig.panelFor(int, boolean)` | `EnemyHudConfig` | 只是 `panel(mode, container)` 的别名，仅被 javadoc 引用 → 删 |
| `BossBarTexture.drawBackground/drawProgress` | `BossBarTexture` | 世界条改走 `slices+quad` 后失效 → 删 |
| `ClientMobHealthCache.serverComponentPresent()` | `ClientMobHealthCache` | 建议删 |
| `ClientMobHealthCache.payloadType()` | 同上 | 注释里写明「故意保留」，可留 |
| `ScrollPane.scrollToTop()` | `ScrollPane` | 删 |
| `SelfStats.displayName(SelfStatsKind)` | `SelfStats` | 1.20.16 起过滤页改用官方 lang 键 → 删 |
| `SelfStats.storedLinesOf(kind)` | `SelfStats` | 删 |
| `SelfStatsPanel.clampX/clampY` + `EDGE_KEEP` | `SelfStatsPanel` | 1.20.23 起改用 `fitInScreen` → 删（`clampX` 还留着 javadoc 交叉引用） |
| `SelfStats.windowEndMs` | `SelfStats` | 1.20.24 改成「计时行收尾」后只写不读（`hardEndMs` 仍在 `settleIfTimedOut` 里读）→ 删 |
| `SelfStats.CAPTURE_WINDOW_MS` | `SelfStats` | 同上，main 里已不再使用（`beginCapture` 用 `CAPTURE_TIMEOUT_MS`），只剩自检引用 |

### 4.2 只有 `SelfCheck` 在用

`SelfStats.CAPTURE_WINDOW_MS`、`EnemyHudLayout.NAME_TOP`、`BossBarTexture.NATIVE_HEIGHT`、
`SelfStats.heartsOf`、`EnemyHudLayout.isArmourRow`、`HeartCounts.knownSum`、`BossBarTitle.suffix`、
`SelfStats.looksLikeStat`（1.20.23 起被 `statGlyphLine` 取代）。

> 这些不是「坏代码」：它们是**纯函数锚点**，自检靠它们做穷举。建议在注释里写明
> 「仅自检使用，改动需同步 SelfCheck」，避免以后被当死代码删掉。

---

## 五、文档与代码不一致

| # | 文档说 | 代码是 |
| --- | --- | --- |
| 1 | `EnemyHudConfig` 类注释：`config/ctt-enemy-hud.json` | 实际 `ctt-hud-fork.json`（1.20.6 改名，注释没跟着改） |
| 2 | 项目 README「设置界面」小节：仍是 1.18 的两列布局表 | 1.20.19 起是「总开关 + 3 个入口 + 恢复默认 + 完成」（README 里已加历史注记，但表格本身过期） |
| 3 | 项目 README「版本沿革」止于 1.20.10 | 已到 1.20.24（缺 11 个版本条目） |
| 4 | `PartyBossbarAutoToggle` 注释 + README：3 s 冷却 / 5 次 / 10 分钟熔断 | 还多出 10 s 重开延迟、单局 20 次上限（见 3.2） |
| 5 | `SelfStatsPanel` 的 `draw()` javadoc 与 `bounds()` javadoc 挤在同一行（排版损坏） | 功能无影响，但会误导读者 |
| 6 | 类名 `CttEnemyHudClient` / `CttEnemyHudServer`、`EnemyHudConfig` 注释写「CTT Enemy HUD」 | mod 名从 1.20.6 起是 **CTT HUD - Fork** |
| 7 | `SelfStats` 类注释：照抄上游「Cake Tower HUD Plus」 | 实际参考的是 Cake-Hud（mod id `ctt-health-display`） |
| 8 | `EnemyHudConfigScreen` 类注释画着 1.18 的四项菜单 | 现在是 6 行（总开关/3 入口/恢复默认/完成） |

---

## 六、风险点

| 风险 | 现状 / 评估 |
| --- | --- |
| 静态可变状态（`SelfStats.SHOWN/PENDING`、`BossBarTracker.BINDINGS` 与队伍条缓存、`CurseDetector` 缓存、`ArmourCache`） | 全部只在客户端主线程读写；网络包已用 `client.execute(...)` 转线程（`ClientMobHealthCache.register`）→ 目前安全，但没有编译期约束 |
| 冻结开关仍参与序列化 | `hideBehindEntities` 默认 true→强制 false→无人读；玩家改它毫无效果（见 3.3） |
| mixin 依赖内部方法名 | `submitNameDisplay` / `Screen.extractRenderState` / `BossHealthOverlay.bossBars` / `EntityRenderer` 内部字段，26.x 小版本升级就可能断（每次升级都要重验） |
| 坐标口径靠猜 | `SelfStatsLayoutScreen.toGui` / `ShieldLayoutScreen.toGui` 用 `x > width` 判断「是 GUI 坐标还是帧缓冲坐标」，属防御式换算；实机键盘焦点已验证但**鼠标拖拽路径在本机不可测** |
| 双端共享类 | `EnemyHudConfig`、`MobHealthPayload`、`MobHealthData` 会在专用服务端加载；它们只依赖纯 Java / MC 通用类，未引用 client-only 类（已核对） |
| 语言键硬编码 | 125 个键分布在 2 个语言文件；过滤页 60 个属性名是**地图资源包的中文名硬编码**，地图改名不会跟着变 |
| 测试只覆盖纯函数 | `SelfCheck` 覆盖布局/解析/夹取/状态机；渲染、mixin、真实网络包没有自动化覆盖 |

---

## 七、建议（按优先级）

1. **删死代码**（4.1 前 6 项），并给 4.2 的「自检专用」成员加注释标注。
2. **处理僵尸配置**：`hideBehindEntities` 要么删字段+迁移，要么让 `HudTarget` 真正读它；`debug` 要么写进 README，要么改名 `debugOverlay`。
3. **补文档**：README 加一节「内部常量表」（第三节 3.1/3.2 全部），并修第五节的 8 处不一致。
4. **重写 `EnemyHudConfig` 类注释**（文件名已改），顺手把 `CttEnemyHud*` 改名为 `CttHudFork*`（涉及 `fabric.mod.json` entrypoint，需一起改）。
5. **加一层坐标口径收敛**：`toGui()` 两处重复，建议抽成 `GuiCoords.toGui(event, width, height, window)` 一处实现。
