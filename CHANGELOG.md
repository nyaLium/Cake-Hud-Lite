# CHANGELOG · Cake Team Tower HUD - Fork

> 这是 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud)（作者 TheHolyLord，MIT）的 Fork 版。
> 版本保留规则见 [versions/VERSIONING.md](versions/VERSIONING.md)：**每个版本都必须留档，不覆盖、不删除。**
> 全量归档清单（大小 / SHA-256 / 内嵌版本号 / git 标签）见 **[versions/ARCHIVE.md](versions/ARCHIVE.md)**，
> 用 `python versions/tools/archive_audit.py` 可重新生成并校验。

## 版本索引

| 版本 | 功能集 | 归档 |
| --- | --- | --- |
| **1.20.16** | 过滤界面的属性名改为地图官方中文（资源包 `ctt_lang`）；显示方式新增「HUD + 容器界面」 | `versions/v1.20.16/` |
| 1.20.15 | 自动开队伍条写死开启、墙/实体遮挡写死关闭（删除界面行）；过滤界面属性名改用 ViewStats 官方 hover 文本；容器面板悬停显示官方属性名 | `versions/v1.20.15/` |
| 1.20.14 | 「被墙挡住时隐藏」默认关且不再提供开关；面板字号最小 1.00；新增显示方式「始终显示 / 在容器界面显示」（两套独立配置、点击即时切换）；关闭面板后其余项不可交互；服务器强制 3 s 时刷新滑条禁用 | `versions/v1.20.14/` |
| 1.20.13 | 属性面板刷新默认 3 s / 1~5 s / 最短「立即」/ 他人服务器强制 3 s（不改配置）/ 悬停提示；HUD 全亮光照；`K/M/B` 大写；面板行去重 | `versions/v1.20.13/` |
| 1.20.12 | 上游 `ctt-health-display` 共存提示（1.20.13 按「基于 1.20.11」撤掉） | `versions/v1.20.12/` |
| 1.20.11 | 没开局时面板不再挂上一局数据（触发被拒立即清空，冷却 60 s→10 s；连续 2 趟空采集也清）；敌人设置页改成两列各 3 行 | `versions/v1.20.11/` |
| 1.20.10 | bossbar 认领在同一血量档里优先**离玩家最近**的同名怪，粘性只保留 2 格（超过就换）；依据是地图 `targetting_enemy` 本身「取离某名 CTT 玩家最近的 E」 | `versions/v1.20.10/` |
| 1.20.9 | 面板位置只留拖动（删 X/Y 滑条）；过滤页双列按钮固定 200（原版宽度）并居中；bossbar 认领加 5 个百分点硬门槛（同名怪不再粘错）；血条补透视副本 | `versions/v1.20.9/` |
| 1.20.8 | 修大厅里那句「无法触发该计分项」：开局前不再发 `/trigger ViewStats`（用 `HealthPercent` / 本人 bossbar 判开局），失败回显被吞并冷却 60 s，回大厅清空面板 | `versions/v1.20.8/` |
| 1.20.7 | 纯客户端（只有 `HealthPercent`）时敌人也显示**百分比数字**（条 + `40%`），不再只有条没有字；队友百分比回退同理 | `versions/v1.20.7/` |
| 1.20.6 | 过滤界面换成双列带边框按钮 + 滚轮翻行；Mod 改名 **CTT HUD - Fork**，配置文件改为 `ctt-hud-fork.json`（旧文件自动复制迁移）；钓鱼/业力/塔内回血/蜂蚁点按**码位**精确区分 | `versions/v1.20.6/` |
| 1.20.5 | 刷新单位写明「现实时间」；删除职业过滤；过滤界面改成可滚动列表、按属性细分到 60 项；配置 v6（旧粗类自动展开） | `versions/v1.20.5/` |
| 1.20.4 | 属性面板去提示行（`Misc_View_Stats_1/2` 与未解析键行不再上屏）、双缓冲不再闪；字号/透明度可调；「移动面板」拖拽 + 水平/垂直滑块；内容过滤 15 类；按职业过滤（1.20.5 已删除）；`F8` 打开设置；队友 `below_name = HealthPercent` 百分比回退；配置解析失败不再静默重置 | `versions/v1.20.4/` |
| 1.20.3 | 新增**自身属性面板**（纯客户端：定时 `/trigger ViewStats`，抓地图回的那几十行属性、吞掉聊天刷屏、按列画成面板）；顺带解析四色心/碎粉心 | `versions/v1.20.3/` |
| 1.20.2 | 血条**两端端盖按纹素原尺寸分段绘制**（原版圆头，不再是长方形）；竖向 UV 回 0~1，保住上下深色描边 | `versions/v1.20.2/` |
| 1.20.1 | 血条改回**原版 BossBar 画法**（整条背景 + 左侧裁切进度），修掉未满时中间那条 1 px 接缝 | `versions/v1.20.1/` |
| 1.20.0 | 心类轮换：敌人红心 + 魂/黑/蓝/碎/粉（每次一种），玩家红心 + 魂↔黑；碎心/蓝心固定在玩家最上面一行；护甲与元素护甲之间改两个空格 | `versions/v1.20.0/` |
| 1.19.0 | 文字恢复透视副本（SEE_THROUGH 第二遍）；默认缩放 0.9；碎心/粉心/蓝心挪到最上面一行 | `versions/v1.19.0/` |
| 1.18.0 | 布局固定三段（护甲最上 / 心数贴条 / 血条 / 名称）；缩放与不透明度对文字生效；血条默认 75 | `versions/v1.18.0/` |
| 1.17.0 | 护甲：🛡 常驻 + 元素护甲 2 项/页循环（2 s）；大数值 k/m/b 缩写；着火时 HUD 沿视线往相机推 | `versions/v1.17.0/` |
| 1.16.3 | 队友只画心与血条，护甲段不再显示（`teamOptions.showArmor = false`）；敌人照旧 | `versions/v1.16.3/` |
| 1.16.2 | 死亡 / 观战（旁观模式）的玩家不再显示头顶 HUD：客户端 `isSpectator()` 不画、服务端不进 payload | `versions/v1.16.2/` |
| 1.16.1 | 队友绿条进度固定为 **红心 ÷ MaxHP**（不再按当前心类分层）；文字行距 9 → **10**，修掉两行之间那条叠加暗带 | `versions/v1.16.1/` |
| 1.16.0 | 队友（玩家）也能显示四色心 / 碎心 / 粉心：服务端把附近玩家一起推，客户端优先用 payload | `versions/v1.16.0/` |
| 1.15.1 | 修血条边缘竖条纹闪烁：两段不再共面重叠 + UV 内收半纹素 | `versions/v1.15.1/` |
| 1.15.0 | 渲染改为世界空间：与原版名字同一个 `submitNameTag` 提交点，深度测试 / 随距离缩放全部对齐；删除 GUI 元素 | `versions/v1.15.0/` |
| 1.14.5 | 血条改画在动作栏/标题/聊天之下；新增「被其它实体挡住时隐藏」（屏幕矩形覆盖率 ≥70% 就不画） | `versions/v1.14.5/` |
| 1.14.4 | 新增「换行阈值」`wrapWidth`（40~200，最右=自动=不超过血条长度），折行阈值可调 | `versions/v1.14.4/` |
| 1.14.3 | 红心写 `❤当前/上限`；整行超过阈值时折两行：红心在上、其余心+护甲在下 | `versions/v1.14.3/` |
| 1.14.2 | 碎心按符号拆两栏：`> 0` 碎心（暗红 💔）/ `< 0` 粉心（亮紫 💗），负值不再被夹成 0 | `versions/v1.14.2/` |
| 1.14.1 | 心数行合并：各类心之间只隔一个空格，`\|` 只留在「心组 ↔ 护甲」之间（`❤160 💗2 \| 🛡3 ☃5`） | `versions/v1.14.1/` |
| 1.14.0 | 新增服务端选装组件（`ctt-hud-fork:mob_health`，按 UUID 推 `AllHearts/MaxHP/护甲/红魂黑蓝碎`，5 Hz 差量 + 3 s keepalive）；客户端优先使用并在 5 s 失鲜后自动回落；新增 `useServerData` | `versions/v1.14.0/` |
| 1.13.5 | 修聊天栏被全拦（ALLOW_GAME 返回值反了）；设置页「返回」按实际行数定位；同名敌人按 `HealthPercent` + 绑定粘性唯一认领 | `versions/v1.13.5/` |
| 1.13.0 | 心数行（红/魂精确反推）；魂心→黄条、蓝心→蓝条；名称改由本 Mod 自绘（1.13.1 已移除） | `versions/v1.13.0/` |
| 1.12.0 | 隐藏名称下方计分板分数；删除百分比文字；玩家血条独立设置（含隐藏队伍条）；血条按心类变色 | `versions/v1.12.0/` |
| 1.11.0 | **回补顶部Boss条隐藏、队友血条；百分比可开关且保留护甲；| 分隔；默认透明度 75%；设置分四类** | `versions/v1.11.0/` |
| 1.10.0 | 回退到「仅头顶血条」+ 修复血条纹理 | `versions/v1.10.0/` |
| 1.9.0 | Mod ID 改为 `ctt-hud-fork`；间隔上限 5000ms；重做队伍条自动开启；面板可调透明度/宽度 | 未归档（已回退） |
| 1.8.0 | 队友血量面板 + 自身属性面板（可拖拽、预设左侧中心）+ 命令间隔闸门 | 未归档（已回退） |
| 1.7.0 | 自动执行 `/trigger TogglePartyBossbar` 打开队伍条（3s 冷却 + 熔断） | 未归档（已回退） |
| 1.6.0 | 队友头顶血条（绿色 bossbar 纹理）+ 未知诅咒门控 | 未归档（已回退） |
| 1.5.0 | 隐藏数据包的「最近敌人 Boss 条」（默认隐藏，可开关） | 未归档（已回退） |
| 1.4.0 | 血条长度/缩放/垂直偏移的区间调整；移除最低最大血量 | 未归档（已回退） |
| 1.3.0 | 纯客户端精确值通道：解析客户端 Boss 栏标题 | 未归档（已回退） |
| 1.2.0 | 敌人判据改为「有 CTT 血量分数」+ Mod Menu 联动 | 未归档（已回退） |
| 1.1.0 | 血条长度可调；选项穷举自检 | 未归档（已回退） |
| 1.0.0 | 首个版本：敌人头顶血条 | 未归档（已回退） |

> 1.0.0 – 1.9.0 采用「同目录改名」的工作方式，源码与 jar 已不可回溯，仅保留功能记录。
> 从 1.10.0 起改为 `versions/vX.Y.Z/` 独立归档。

## 1.20.16（当前）

- **过滤界面的属性名改为官方中文**：从地图资源包
  `CTT Datapack & Resource Pack/resources 26.3 - Baby Updating/assets/ctt_lang/lang/zh_cn.json`
  的 `view_stats_*` 键取值（红心/魂心/…/真•盔甲/火之加护/法力强度倍率…），写进内置 `kind.*` 翻译；
  不再依赖运行时 hover。英文侧同步补了第三档模式名。
- **新增显示方式第三档「HUD + 容器界面」**：`selfStatsMode = 2`，HUD 上画、容器界面里也画；
  设置里的按钮按 0 → 1 → 2 → 0 循环。
- 自检：`checks=5117 failures=0`（+2：模式 2 的 hud/container 判定、模式夹取到 2）。

## 1.20.15

- **三个开关写死**

- **三个开关写死**：`normalize()` 强制 `autoPartyBossbar = true`、`hideBehindWalls = false`、
  `hideBehindEntities = false`；玩家设置的「自动开启队伍血量条」、通用设置的「被其他实体挡住时隐藏」两行删除。
- **过滤界面用官方属性名**：`ViewStats` 行自带的 `hover_event.show_text`（官方翻译）按属性记下来，
  `SelfStats.displayName(kind)` 优先用它，没有才回退内置翻译；过滤按钮标签同步。
- **容器面板悬停提示**：`ContainerPanelMixin` 传入鼠标坐标，面板按位置/缩放/列宽算命中行，
  用 `setComponentTooltipForNextFrame` 显示该行官方 hover（仅「在容器界面显示」模式）。
- 自检：`checks=5115 failures=0`。
- 真机：进服/渲染无异常；hover 与过滤名**未截图验证**（开发客户端鼠标事件不可靠）。

## 1.20.14

- **「被墙挡住时隐藏」默认关闭，界面不再提供开关**：`hideBehindWalls` 默认 `false`；通用设置里删掉该行
  （配置项仍可手改 JSON）。
- **属性面板字号最小 1.00**：`selfStatsScale` / `containerScale` 夹取 1.0~2.0。
- **新增「显示方式」**（`selfStatsMode`）：
  - `始终显示`（0）：HUD 元素，一直画（原行为）；
  - `在容器界面显示`（1）：HUD 上不画，改为画在箱子/背包/商店等容器界面上；
  - 两套**独立**配置：HUD 用 `selfStatsX/Y/Columns/Scale/Opacity`，容器用 `containerX/Y/Columns/Scale/Opacity`；
    升级时容器那套从 HUD 复制一份；点击切换立刻重建生效，「移动面板」改当前模式那一套；
  - 容器绘制：新 mixin `ContainerPanelMixin` 挂在 `Screen.extractRenderState` TAIL，仅对 `AbstractContainerScreen`。
- **关闭属性面板后其余项不可交互**：刷新间隔 / 列数 / 字号 / 显示方式 / 不透明度 / 移动面板 / 内容过滤全部 `active=false`。
- **服务器强制 3 s**：刷新滑条同样禁用，标签显示「3 秒（服务器固定）」，悬停提示说明配置值保留。
- 配置版本 6 → 7（迁移：`container*` 从 `selfStats*` 复制）。
- 自检：`checks=5113 failures=0`（+13：两套配置取值/移动/夹取、v6→v7 迁移）。
- 实测：模式 0 HUD 显示、模式 1 背包里显示、模式 1 关背包后 HUD 不显示（`versions/v1.20.14/*.png`）；
  设置界面的禁用/悬停/切换**未截到图**（开发客户端鼠标/键盘导航异常）。

## 1.20.13

- **属性面板刷新间隔**：默认 3 秒；滑条 1~5 秒（1 = 「立即」）；进入他人服务器强制 3 秒
  （`FORCED_SERVER_SECONDS`，用 `getSingleplayerServer() == null` 判定），**不写回配置**；
  服务器上设置界面直接显示「3 秒（服务器固定）」；滑条加了悬停提示。
- **HUD 全亮**：`HudWorldRenderer` 不再用实体的 `lightCoords`，固定 `0xF000F0`（FULL_BRIGHT），
  暗处/阴影里文字和血条不再变黑。
- **大写缩写**：`123.4k → 123.4K`、`m → M`、`b → B`。
- **「多重显示」去重**：`onGameMessage` 跳过同文字的行，`settle` 再按字符串去重一次；
  挡住「手动 /trigger 与自动刷新撞窗口时地图回两遍」。
- 撤掉 1.20.12 的进服提示（按「基于 1.20.11」的要求）。
- 自检：`checks=5100 failures=0`（+1：服务器强制 3 s / 单机用配置值）。
- 实测：夜间暗处 `❤150K/200K` 全亮（`versions/v1.20.13/big-numbers-night.png`）。
  设置界面的「立即」标签与悬停提示**未截到图**（开发客户端这轮鼠标/键盘导航异常）。

## 1.20.12

- 上游 `ctt-health-display` 共存提示（**1.20.13 已撤销**，归档保留原样）。

## 1.20.11

- **没正式开局时，属性面板不再挂上一局的数据**：
  - 原因：`gamestart.mcfunction:148` 才 `enable @a[tag=CTT] ViewStats`，开局前 `/trigger` 会被拒绝；
    1.20.8 只吞提示 + 冷却 60 s，没有清面板，于是「游戏时间为 00:10:54」这类上一局数字一直挂着。
  - 现在拒绝分支：`notStarted` + 清空 `SHOWN/PENDING/HEARTS`，面板立刻消失；冷却 60 s → **10 s**，
    开局后最多 10 s 面板就回来。
  - `settle()` 新增：连续 2 趟空采集（`MAX_EMPTY_CAPTURES = 2`）→ 清空面板；单趟丢包仍保留旧数据不闪。
- **敌人设置排版 3 + 3**：左列「显示血条 / 显示距离 / 优先使用服务端数据」，
  右列「血条长度 / 整体缩放 / 垂直偏移」，返回按钮居中在内容下方（以前左 2 右 4、按钮被压到最下）。
- 自检：`checks=5099 failures=0`。
- 实机：真 CTT 服开局后面板正常出数；`versions/v1.20.11/enemy-layout-3x3.png` 为设置页截图
  （GUI scale 3）。拒绝分支的「立即清空」为代码 + 单测覆盖，未构造开局前窗口做真机对拍。

## 1.20.10

- **同名怪：同一血量档里优先离玩家最近的那只**。`BossBarTracker.matchScore` 的排序改为
  「精确 > 差≤2 > 差≤容差 > 名字完全相同 > **距离** > 粘性(+4)」，
  粘性奖励从 `1e6` 降到 `4.0`（= 2 格²）：上一帧绑定的怪在最近候选 2 格以内才继续用，
  超过就让更近的同名怪接手。
- 依据：`misc/bossbars/targetting_enemy.mcfunction` 的选法本来就是
  「在随机一名 CTT 玩家里取离他最近的 `E`」，距离是很强的先验。
- 自检：`checks=5099 failures=0`（+3：2 格内沿用、超 2 格换近的、远处旧绑定不压眼前新怪）。
- **如实说明**：没做真机对拍 —— 本图把 `minecraft:targetting` 只发给 `@a[tag=SpectatingGame]`，
  活人收不到这条 bar；测试假怪的 tag 也会被地图清 tag 流程抹掉，这一步没跑通。
- 顺带解释「信息不更新」：活人纯客户端本来就没有这条 bar 的精确数据，头顶就是每 tick 重算的
  `HealthPercent`；真正的同名串数据只可能发生在**旁观者**或**装服务端组件**（payload 按 UUID，无此问题）。

## 1.20.9

- **面板位置只留拖动**：Stats panel 段删掉「水平位置 / 垂直位置」两个滑条（6 行 → 4 行）。
  `selfStatsX/Y` 字段保留，老配置/手改 JSON 照旧生效。
- **过滤页按钮宽度参照原版**：`SelfStatsFilterScreen` 单列按钮目标宽度 `200`（原版菜单按钮），
  屏幕放不下才收缩（下限 60），两列整体居中；旧版在 GUI 853 时会把单个按钮拉到 410。
- **bossbar 认领加硬门槛（同名怪）**：`BossBarTracker.PERCENT_TOLERANCE = 5`——
  `|bar 百分比 - HealthPercent| > 5` 的候选不再参与认领；门槛内再按
  「精确 > 差≤2 > 差≤容差 > 名字 > 上一帧绑定 > 距离」排序。一只都对不上就不绑，
  那只怪退回百分比，避免「最近敌人的数字不更新 / 张冠李戴」。
- **血条补透视副本**：文字一直有 `NORMAL + SEE_THROUGH` 两层，血条只有一层（被挡住就整条消失）。
  现在血条也提交一层 `textSeeThrough`（50%），被方块/天花板挡住时留下半透明的一条。
- 自检：`checks=5096 failures=0`（+20：容差/打分排序、面板位置行数、过滤页宽度）。
- 实机：`versions/v1.20.9/settings-no-position.png`、`filter-vanilla-width.png`、
  `bar-through-wall.png`（GUI scale 2 下的 200 宽居中按钮；隔墙半透明血条）。

## 1.20.8

- **修大厅里的「无法触发该计分项 / You cannot trigger this objective yet」**：
  - 原因①：属性面板以前**在大厅也每 30 s 发** `/trigger ViewStats`，地图开局才 `enable`；
  - 原因②：这句回显没被 `ChatTriggerFilter` 覆盖（只登记了 `TogglePartyBossbar`）。
- 改法：
  - `SelfStats.tick` 用 `inGame()` 判开局：自己身上有 `below_name = HealthPercent` 分数，
    或本人 `player_100N` bossbar 已出现（`BossBarTracker.hasPersonalBar`，与自动开队伍条同一判据）；
    大厅里每 5 s 复查一次，不发命令；
  - 发命令前 `ChatTriggerFilter.noteTriggerSent`，并把 `ViewStats` 加进 `MUTED_TRIGGERS`（成功回显也吞）；
  - 新增 `isTriggerDenied()`：真收到拒绝提示时吞掉、终止本趟采集、60 s 冷却；
  - 回到大厅（`inGame()` 变假）时清空面板与心数缓存，不再挂着上一局数字。
- 实测（真 CTT 服，`selfStatsSeconds=5` 加速）：大厅 30 s **0 条**拒绝提示；QuickStart 开局后面板正常出数、仍 **0 条**。
- 自检：`checks=5076 failures=0`（+13：拒绝提示中英文识别、聊天过滤器对 ViewStats 回显/`commands.trigger.failed.*` 的处理）。
- 证据：`versions/v1.20.8/run-panel.png`。

## 1.20.7

- **纯客户端也显示百分比数字**：`EnemyHudLayout` 的 `percentOnly` 分支以前不产出文字
  （只有血条），现在写 `40%`（`round(hp*100/maxHp)` 夹 0~100，`maxHp<=0` 按 100%）。
  受影响的是「只有 `below_name = HealthPercent`」的两类：服务端没装本 Mod 时的敌人，
  以及队伍条里没有条目的队友（绿条 + `63%`）。
- 有精确数据（`❤80/200`）或「当前/上限」（`63/120`）的路径完全不变；`showNumbers` 开关仍能关掉数字。
- 自检：`checks=5063 failures=0`（+34：percentOnly 各类取值、关数字开关、队友百分比回退、未知心数的 当前/最大）。
- 实机 A/B（同一只怪只切 `useServerData`）：`versions/v1.20.7/percent-only.png`（`40%`）↔
  `server-data.png`（`❤80/200 ❤2` + 护甲）。

## 1.20.6

- **过滤界面改成双列按钮 + 滚动**：1.20.5 的无边框列表容易点岔，现在每项都是真正的 `Button`
  （原版纹理 = 有边框、悬停高亮、可聚焦），两列排布（`COLUMNS=2`、行高 20、列间距 8、行间距 5）；
  滚轮按行翻（一行两个属性），右侧细滚动条，`rebuildWidgets` 重排；`✔` 绿 / `✘` 红 + 名称变灰；
  计数「60 items · N hidden」不变。
- **改名**：
  - `fabric.mod.json` → `CTT HUD - Fork`；Mod Menu 名称、设置界面标题（中/英）同步；
  - 配置文件 `ctt-enemy-hud.json` → **`ctt-hud-fork.json`**：启动时若新文件不存在、旧文件存在，
    就把旧文件**复制**成新文件（旧文件保留），日志写一行「配置改名」；Mod ID 不变。
- **私有区字形按码位精确区分**（用你给的资源包 `font/default.json` + `view_stats.mcfunction` 对齐）：
  | 码位 | 贴图 | 属性 |
  | --- | --- | --- |
  | U+E001 | `beepoint.png` | 蜜蜂点 / 蚂蚁点（黄 / 黑） |
  | U+E003 | `fishing.png` | 钓鱼 |
  | U+E004 | `broccoli.png` | 塔内回血 |
  | U+E010 | `cross.png` | 光明因果 / 中立因果（青 / 暗紫） |
  | U+E011 | `evil_cross.png` | 黑暗因果 |
  | U+E026 | `cracked_heart.png` | 碎心 / 粉心（暗红 / 亮紫） |
  | U+E027 | `planetarium.png` | 天界因果 |

  1.20.5 里「钓鱼与光明因果都是青色、只能归到其它」的限制因此解除。
- 自检：`checks=5029 failures=0`（+16：双列布局/滚动/`itemAt` 命中、码位识别）。
- 实机：`versions/v1.20.6/` 四张图（默认面板、改名后的标题、双列过滤、滚动+切换后 `1 hidden`）。

## 1.20.5

- **刷新单位写明「现实时间」**：`SelfStats` 用 `System.currentTimeMillis()`，30 s 就是现实 30 秒，
  与游戏刻无关；界面文案改成「刷新间隔：30 秒（现实时间）」。
- **删除职业过滤**：`ClassTracker`、`selfStatsClass`、`selfStatsClassHidden`、设置页下拉、
  过滤界面的「当前编辑：XX 专属过滤」全部移除；旧 JSON 里的这两个键会被忽略。
- **过滤界面重写为可滚动列表**，并按**单个属性**细分（`SelfStatsKind` 从 15 个粗类扩到 **60 项**）：
  - 列表：每行 `✔/✘ + 属性名`，悬停高亮，右侧细滚动条，滚轮逐行滚动；点一行即时切换；
  - 识别：带翻译键的行按键认（倍率/基础值/状态/计时），数值行按「图标 + 颜色」认
    （四色心、`❤%`、`🗡` 金/红/蓝/深绿/绿、`🏹`、`🛡` 及七种元素护甲、`✈` 青/绿/粉、`✏` 金/青、`⚡` 黄/金/白、`☠` 暗紫/红、`⬇⬆`、`⇑⇓`、私有区字形等）；
  - 做不到的：负值统一灰色分不出属性、钓鱼/光明因果同为青色 → 归到「其它」；
  - 旧配置的粗类自动展开：`health`→7、`attack`→9、`defence`→4、`element`→8、`speed`→4、`mana`→3、`misc`→17、`time`→`timer`。
- 配置 `configVersion` 5 → **6**。
- 自检：`checks=5013 failures=0`（+152：60 类识别、粗类展开、滚动列表的可见行/最大滚动/夹取/滚轮/行命中）。
- 实机：`versions/v1.20.5/` 四张图（默认面板、隐藏两个属性、过滤列表界面、现实时间文案）。

## 1.20.4

- **提示性文本不再上面板**：地图的 `Misc_View_Stats_1`（`Your Stats:`）与 `_2`
  （`Hover over an icon to see what it is`）按 `TranslatableContents#getKey()` 认键后直接丢弃，
  **不依赖是否装了地图资源包**（翻译改变渲染文本，键仍在 Component 里）。
  另外，缺地图语言文件时含**未解析翻译键**的行（例如计时行 `Misc_View_Stats_7800:00:19`）整行丢弃。
- **刷新不再闪**：采集改成双缓冲 —— 新一趟数据先进 `PENDING`，这一趟收完（窗口到期）再整体替换上屏内容；
  空结果不覆盖旧面板；地图说「游戏没开始」时才清空。
- **字号 / 透明度可调**：`selfStatsScale`（0.5~2.0）、`selfStatsOpacity`（0.2~1.0）；
  透明度对带颜色的样式段同样生效（实测红/金/青/暗红一起变淡）。
- **移动面板**：新增 `SelfStatsLayoutScreen`，界面内实时预览 + 拖拽（增量位移、夹在屏幕内、至少留 16 px 可见），
  实时显示 `x/y` 与面板尺寸；设置页另有水平/垂直滑块。
- **内容过滤**：新增 `SelfStatsKind`（15 类：四色心、碎/粉心、生命、攻击、防御、元素护甲、速度、法力、
  杂项、状态、计时；`hint` 不可过滤），`SelfStatsFilterScreen` 提供 15 个开关（宽屏 3 列 / 窄屏 2 列）。
  分类优先认地图翻译键（键与属性一一对应），心形这类没有键的行按「颜色 + 字形」兜底。
- **按职业过滤**：新增 `ClassTracker`，从地图选职业时的聊天公告
  「`<自己> has become <职业键>`」里认出职业（键 `class_select_12..30`、
  `Floors_Scenes_Class_Select_101/104`），用 `selfStatsClassHidden[职业键]` 那一份隐藏表；
  `selfStatsClass` 可取 `OFF` / `AUTO` / 手动指定职业。职业表里的**空列表**表示「该职业什么都不隐藏」。
- **队友百分比回退**：队伍条里没有该条目（或玩家不在队伍条但本局参战）时，
  用 `below_name = HealthPercent`（= 红心 ÷ MaxHP，地图对所有 `CTTAll` 都算）画百分比绿条。
- **`F8` 打开设置**（`KeyMapping.Category.MISC`，可改键）：没装 Mod Menu 也能进设置界面。
- 配置 `configVersion` 4 → **5**；新字段都有默认值，旧值不动。
  **手改坏配置时保留上一份可用配置**（以前会静默 `new Cfg()` 恢复默认）。
- 自检：`checks=4861 failures=0`（+62：内容分类、提示键、过滤表清洗、职业识别、缩放/位置夹取、v4→v5）。
- 实机：`versions/v1.20.4/` 下 6 张证据图（提示行消失 / 字号透明度 / 红心过滤 / 职业过滤生效 /
  移动面板 / 设置页）。

## 1.20.3

- **新增「自身属性面板」**（`SelfStats` + `SelfStatsPanel`），照搬上游 Cake Tower HUD Plus 的取数方式，
  而且**纯客户端**、服务端只装 API 也能用：
  1. 每 `selfStatsSeconds` 秒（默认 30）客户端自己发地图的 `/trigger ViewStats`；
  2. 地图 `cake_team_tower:misc/view_stats.mcfunction` 用一批 `tellraw @s` 回属性
     （血量、四色心、碎/粉心、🗡力量、🗡攻速、🛡防御…）；
  3. 在 `ClientReceiveMessageEvents.ALLOW_GAME` 里截下这些行：**吞掉不进聊天栏**，攒进面板，
     窗口随到随续（单次最多 8 s），并顺手按「颜色 + 心形字形」解析出红/魂/黑/蓝/碎/粉心数。
- **地图说「游戏没开始」时不画**（`isGameNotStarted`），避免大厅里糊一片。
- 新配置（`configVersion` 3 → **4**，不改任何旧值）：
  `showSelfStats`（默认开）、`selfStatsSeconds`（30，0=关）、`selfStatsColumns`（2，1~3）、
  `selfStatsX` / `selfStatsY`（GUI 像素，默认 4 / 40）。
- 自检：`checks=4799 failures=0`（+53：心形/颜色映射、数字解析、属性行识别、ViewStats 行识别、
  「游戏没开始」、端到端 Component 解析、面板列布局、新配置夹取与 v3→v4 迁移）。
- 实机：`versions/v1.20.3/self-stats.png`（真局里面板内容 + 聊天栏无刷屏）。

## 1.20.2

- **血条两端恢复成原版的「圆头」**。以前把整张 182×5 纹理横向压到 50~150 个名牌像素，
  端盖那几列（含被挖成透明的两个角）一起被压到 1 px 以下，看起来是**纯长方形**。
  现在按纹素切三段（`BossBarTexture.slices`，自检覆盖 101 档条长）：
  - 左端盖固定 `CAP_TEXELS = 4` 纹素 = 4 个布局像素（与条高 5 纹素同比例，不压扁）；
  - 中段拉伸（182 纹素里中间全是同一列颜色，拉伸看不出来）；
  - 右端盖同样 4 纹素，只在进度满格（u 到纹理最右）时画 —— 没满时右边界本来就是直切的，
    和原版 `blitSprite` 一致。
- **竖向 UV 改回 0~1**：以前内收半个纹素，某些距离下最上/最下一行深色描边会被采样吃掉，
  条看着更「平」。
- 自检：`checks=4746 failures=0`（+2966：血条分段几何、端盖宽度、满格右端盖、窄条退化）。
- 实机对照 `versions/v1.20.2/bar-caps.png`（旧=方头 / 新=圆头 / 40% 直切边界）。

## 1.20.1

- **血条改回原版 BossBar 画法**：以前是「progress 0..fill + background fill..end」两段相邻拼接，
  两张纹理各裁一半、端盖对不上，接缝处会漏 1 px 的缝（未满时中间一条白线）。
  现在先画**整条** `*_background`，再把 `*_progress` 从左裁到 `fill` 叠上去（同原版
  `BossHealthOverlay`）；背景往远处挪 0.02 个名牌像素，保证进度始终在上、不闪烁。
  对照图 `versions/v1.20.1/bar-before-after.png`。
- 自检：`checks=1580 failures=0`（布局与数据逻辑未变）。

## 1.20.0

- **心类轮换**（新增纯函数 `HeartCycle`，每 2 s 一种、实体 id 作 `salt` 错开）：
  - 敌人：`❤当前/上限` + 魂/黑/蓝/碎/粉里**当前这一种**（顺序固定，只挑真有的）；
  - 玩家：`❤当前/上限` + 魂 ↔ 黑。
  实测 6 帧：敌人 `💛50` ↔ `💔2`；玩家 `💛40` ↔ `🖤30`（`versions/v1.20.0/*-cycle.png`）。
- **玩家碎心/粉心 + 蓝心固定在最上面一行**（同时显示，不参与轮换）。
- **护甲与元素护甲之间两个空格**（`EnemyHudLayout.ARMOUR_GAP`）：`🛡5  🔥3 ☃1`。
- **取消 1.19.0 的玩家心数换行**：行数回到固定（敌人 1~2 行、玩家 1~2 行），
  `Options.wrapHearts` 与换行代码/断言一起删除。
- 自检：`checks=1580 failures=0`（+14：`HeartCycle` 各分支、单一轮换心、两空格、玩家特殊心置顶）。

## 1.19.0

- **透视副本恢复**：每行文字提交两遍 —— `NORMAL`（不透明、无背景、深度测试）+
  `SEE_THROUGH`（约 50% 文字 + 25% 黑背景、不做深度测试），参数与原版 `submitNameTag` 一致。
  实测僵尸挡在 Bot2 前面时，Bot2 的 `❤100/100 💛50` 仍隐约可见
  （`versions/v1.19.0/see-through.png`）。
- **默认缩放 0.9**：`scale` / `teamScale` 默认由 1.0 改 0.9，`configVersion` 升到 3；
  v1（无 `configVersion`）迁移 100→75、1.0→0.9，v2 迁移 1.0→0.9，自己调过的不动。
- **玩家心数自动换行**（1.20.0 已取消）：新增 `Options.wrapHearts`（玩家 true、敌人 false），
  阈值**写死**为血条长度（血条关掉时 75），无配置项；贪心换行，每行至少一段。
- **碎心/粉心/蓝心挪到最上面一行**：最上面一行 = 护甲 + 碎心/粉心 + 蓝心；紧贴血条的只剩红/魂/黑。
  截图 `versions/v1.19.0/player-hearts-wrap.png`。
- 自检：`checks=1566 failures=0`（+18：特殊心所在行、玩家换行 3 种宽度、默认缩放与 v1/v2/v3 迁移）。

## 1.18.0

- **布局固定三段**：护甲行在最上面、心数行紧贴血条（永远只有一行）、血条、名称。
  以前文字按宽度在 1~2 行之间跳，同一只怪的行数会变；现在只取决于「有没有护甲」。
  `Result.rows` 改为从上到下排列，新增 `Row.isArmourRow()`；`wrapWidth` 配置与设置界面滑块删除。
  截图 `versions/v1.18.0/layout.png`。
- **缩放设置生效**：文字从 `submitNameTag` 改为自己 `submitText` 提交（原版把缩放写死 0.025），
  血条同样乘 `scale`。实测 `scale 1.0` → 血条 161 px，`scale 1.5` → 239 px（×1.485）。
- **不透明度对文字生效**：文字颜色改为带 alpha（原版写死不透明），背景框同步变淡。
  实测 `opacity 1.0` → 文字主色 (224,99,100)，`0.5` → (200,118,118)（与背景混合）。
- **血条默认长度 75**：`BAR_WIDTH_DEFAULT` / `barWidth` / `teamBarWidth` 默认都改 75；
  配置新增 `configVersion = 2`，老配置里仍是旧默认 100 的自动迁移到 75，自己调过的不动。
  实测 75 → 161 px、100 → 215 px（比值 1.335 ≈ 100/75）。截图 `versions/v1.18.0/scale-opacity.png`。
- 自检：`checks=1548 failures=0`（折行断言换成固定两行断言、默认长度与迁移规则）。

## 1.17.0

- **护甲分页**：`🛡 防御` 常驻，七种元素护甲每页最多 **2 项**、**2 s** 翻一页（5 种 = 2+2+1）。
  新增纯函数 `ArmourCycle.page(armor, nowMs, salt)`，`salt` 用实体 id 错开，避免满屏同时翻。
  `EnemyHudLayout` 新增 `ARMOUR_PAGE_SIZE / armourPageCount() / clampArmourPage()` 与 `Options.armourPage`。
  实测逐帧：`🛡5 🔥3 ☃1` → `🛡5 ☠2 ☀4` → `🛡5 ⚓6`（`versions/v1.17.0/armour-pages.png`）。
- **大数值缩写**：`999999` 实测会把整行拉出屏幕。新增 `EnemyHudLayout.num()`：< 100000 原样，
  再大改 `k / m / b` 且小数截断（`999k`、`1.2m`、`123.4m`、`2.1b`）。心数 / 上限 / 防御 / 元素护甲统一走它。
  实测 `999999 × 5` → 每段 `999k`，整行缩到约 1/3 并折两行（`versions/v1.17.0/big-numbers.png`）。
- **着火不再盖住血条**：燃烧是不透明几何（`solid`、写深度），火焰盒子向相机一侧伸出
  `0.7 × boundingBoxWidth`，血条平面穿过盒子中间因而被深度裁掉。现在 `displayFireAnimation`
  为真时把**整组 HUD（文字 + 血条）**沿视线往相机推 `0.7 × 宽 + 0.05` 格（上限 1.2），
  同一条射线屏幕位置不变。实测同一块裁切里血条/文字的红像素 15.79% → 20.81%
  （`versions/v1.17.0/fire-before-after.png`）。
- 自检：`checks=1540 failures=0`（+39：分页/翻页时钟/三页布局/`num()` 边界）。
  总数比 1.16.3 的 1595 少，是因为「逐段」断言随护甲段数从 7 降到 2 而减少，**没有删断言**。

## 1.16.3

- **队友只画心与血条，护甲段不再显示**：`HudWorldRenderer.teamOptions()` 里 `showArmor = false`。
  `EnemyHudLayout` 的护甲段与 `|` 分隔符都写在 `if (opt.showArmor && hasArmor(...))` 内，
  关掉这一个开关就同时去掉分隔符 + 防御 + 七档元素护甲，不会留下孤零零的 `|`。
- payload 的 `defence / armour` 字段照旧收下（协议与数据结构不动），只是不给玩家那一套布局用；
  敌人仍是 `showArmor = true`，`🛡5 🔥3` 照常显示。
- 实测（同一帧三只）：Bot1 `Defence 5 / FireArmor 3 / IceArmor 1` → 只有 `❤60/150` / `💛40 🖤30 💙20 💔2`；
  Bot2 `Defence 7 / FireArmor 4` → 只有 `❤100/100 💛50`；对照的僵尸 → `❤100/100` / `🛡5 🔥3`。
  截图 `versions/v1.16.3/teammates-vs-enemy.png`。
- 自检：`checks=1595 failures=0`（+6：队友无 🛡/🔥/`|`、只剩心、血条仍在、敌人护甲未被误伤）。

## 1.16.2

- **死亡 / 观战（旁观模式）的玩家不再显示头顶 HUD**：CTT 里死掉的玩家就是 `/gamemode spectator`，
  所以一条规则同时管住两种状态。
  - 客户端 `HudTarget.computeVisible()`：`player.isSpectator()` → 这一帧不画（心数行 + 血条）；
  - 服务端 `CttEnemyHudServer.collect()`：旁观模式的玩家不进 payload。
- 检查放在 `computeVisible()` 而不是 `managed()`：`managed` 还负责摘掉原版 `below_name` 分数，
  旁观者那条 `0%` 同样不该冒出来，所以分数照摘、HUD 不画。
- 客户端游戏模式来自玩家列表（`PlayerInfo.getGameMode()`），原版会同步，**纯客户端也判得准**。
- 实测（Carpet 假人 + 旁观视角）：Bot2 存活 → `❤100/100 💛50` + 满绿条；`gamemode spectator Bot2` →
  半透明模型仍在视野里，但心数行与绿条整块消失，只剩原版名字；改回创造 → 立刻回来。
  截图 `versions/v1.16.2/spectator-before-after.png`。
- 自检：`checks=1589 failures=0`（无新增：判定依赖 `Player.isSpectator()`，纯 JVM 测不了，由实机覆盖）。

## 1.16.1

- **队友（玩家）绿条进度改为 红心 ÷ MaxHP**：`EnemyData.fromPayloadPlayer()` 只改比例与配色通道，
  心数行照旧输出全部颜色。数据包的分层进度（蓝 > 黑 > 魂 > 红）放在单色绿条上读不出来：
  `Red 100 + Soul 50 / MaxHP 100` 旧版画 50%（魂心 50/100）、新版画 100%（红心 100/100）。
  敌人不受影响，仍是分层进度。
- **两行文字中间的重叠暗带**：原版文字背景框是 `y - 1 ~ y + 9`（10 像素），行距却用了字体行高 9，
  两行背景重叠 1 像素、两层 25% 黑叠成一条深色横带。新增
  `EnemyHudLayout.TEXT_BOX_HEIGHT = 10`，`ROW_STEP` 由 9 改为 10；折行时上面那一行从 -27 变成 -28。
- 实测（Carpet 假人 + 白墙背景，同一机位 A/B）：交界两行亮度 旧 96.3 / 新 128.0（单层背景 128.0）；
  截图 `versions/v1.16.1/rows-before-after.png`、`versions/v1.16.1/teammates-progress.png`。
- 自检：`checks=1589 failures=0`（+63：行距/背景框不重叠、`fromPayloadPlayer` 的红心口径与边界）。

## 1.16.0

- **队友（玩家）也能显示完整心数**：魂心 / 黑心 / 蓝心 / 碎心 / 粉心，与敌人同一套渲染。
  `CttEnemyHudServer.collect()` 不再跳过玩家（附近的其他玩家按 UUID 一起推，自己不推）；
  客户端 `resolveData` 对玩家也优先用 payload，拿不到才回退队伍条的 `AllHearts/MaxHP`。
- `teamOptions` 改为 `showHearts/showArmor = true`：没有心数时 `EnemyHudLayout` 自动退回 `当前/最大`。
- 实测（Carpet 假人）：Bot1 `❤60/150` + `💛40 🖤30 💙20 💔2`；Bot2 `❤100/100 💗3`。
- 自检：`checks=1526 failures=0`（+4：队友无心数回退、队友五色心折两行）。

## 1.15.1

- **修血条边缘的竖条纹闪烁**（魂心 / 黑心 / 蓝心最明显）。两个原因：
  ① 旧实现是「整条 `background` + 压在上面的 `progress`」，两张**共面**四边形在深度测试下抢像素，
  移动视角时逐帧翻转；② UV 的 `0/1` 正好压在 182×5 纹理边界上，拉伸时采到端盖纹素。
- 现在两段**不重叠**（填充段 `progress`、空槽段 `background`，UV 接着切），
  并把 UV 全部内收半个纹素。
- 确认数据包会把 `MaxSoulHearts` 置为 100，所以 `Soul 50 / 100` 显示半条是正确行为。
- 自检：`checks=1522 failures=0`（布局逻辑未改）。

## 1.15.0

- **渲染架构改为世界空间**：血条不再画在 GUI 层，而是挂在
  `EntityRenderer.submitNameDisplay` 的 `TAIL`（`NameTagHudMixin`）上——
  **和原版名字用同一个提交函数**（`SubmitNodeCollector.submitNameTag`，`DisplayMode.NORMAL`）。
  于是遮挡与深度行为与原版名字一致：被墙挡住消失、被更近的模型逐像素裁掉、尺寸随距离缩放。
- 血条几何用同样的 billboard 三步变换 + `submitCustomGeometry(RenderTypes.text(boss_bar PNG))`，
  仍是原版 182×5 纹理（直接采文件、UV 铺满），资源包替换照常生效。
- 新增 `HudWorldRenderer`（取代并删除 `EnemyHudElement`）、`HudRenderBridge`
  （`FabricRenderState` + `RenderStateDataKey<Entity>`，解决 submit 阶段拿不到实体）。
- `CttEnemyHudClient` 不再注册 `HudElement`；`BossBarTexture` 增加世界空间用的纹理 id 与 `argb`。
- 文字行带上了原版名字那种深色背景框；`hideBehindWalls` / `hideBehindEntities`
  降级为「少提交一点」的额外剔除，真正的遮挡交给深度测试。
- 自检：`checks=1522 failures=0`（布局逻辑未改）。

## 1.14.5

- **血条不再压住 UI 文字**：`HudElementRegistry.addLast` → `attachElementBefore(VanillaHudElements.OVERLAY_MESSAGE)`，
  动作栏 / 标题 / 聊天 / 玩家列表现在都画在血条之上（血条仍在世界之后）。
- **新增 `hideBehindEntities`（默认开）**：把每只怪的包围盒投影成屏幕矩形，按距离从近到远处理，
  被更近的实体盖住 ≥70% 就不画它的 HUD —— 解决「后方怪的血条压着前面怪的名字和模型」。
- 新增纯几何类 `BoxOcclusion`（覆盖率 / 遮挡判定）。
- `EnemyHudElement` 的 `Placement` 增加 `depth` / `screenBox`，新增 `projectBox()` / `dropOccluded()`。
- 设置页通用页右列新增「被其它实体挡住时隐藏」。
- 自检：`checks=1522 failures=0`（+18 条遮挡覆盖率边界）。

## 1.14.4

- **新增「换行阈值」配置**：`wrapWidth`，40 ~ 200 像素，**拉到最右 = 自动**（= 不超过血条长度）。
  设置页在「敌人设置」左列第三行，最右端标签显示「换行阈值：自动」。
- 阈值只在「整行宽度 > 阈值」且「红心之后还有别的段」时才折行；自动模式下血条关掉时兜底 100。
- `EnemyHudLayout` 去掉硬编码的 `MIN_ROW_WIDTH=90`，改为 `Options.wrapWidth` + `resolveWrapWidth()`；
  新增 `WRAP_MIN` / `WRAP_MAX` / `WRAP_AUTO` 常量。
- 自检：`checks=1504 failures=0`（+13 条：阈值解析 5 条、阈值 60 折行 3 条、配置归一化 5 条）。

## 1.14.3

- **红心带上限**：红心段渲染成 `❤当前/上限`（上限 = `MaxHP`）；`MaxHP <= 0` 时只写当前值。
- **文字可折成两行**：整行宽度超过 `max(血条长度, MIN_ROW_WIDTH=90)` 时，
  红心单独占上面一行，其余心与护甲留在下面一行（换行代替 `|`）。
  第二行仍贴 `TEXT_TOP=-18`，第一行抬到 `-27`；血条位置不变。
- `EnemyHudLayout` 新增 `Row(lines, width, top)` 与 `ROW_STEP` / `MIN_ROW_WIDTH`；
  `Result` 增加 `rows` / `rowCount()`，`textTop` 语义保持「最底一行」。
- 修护甲分隔符判定：红心搬进 `head` 后，原判据只看 `rest` 非空，会漏掉 `|`。
- 自检：`checks=1491 failures=0`（红心 `/上限`、上限未知、不折行、折两行、两行 top 位置）。

## 1.14.2

- **碎心按符号拆两栏**：`CrackedHearts > 0` = 碎心（暗红 `💔`，`0xAA0000`），
  `< 0` = 粉心（亮紫 `💗`，`0xFF55FF`，数量取绝对值）。依据是数据包
  `cake_team_tower:misc/view_stats.mcfunction:24-27`（正数 `dark_red`、负数转 `PinkHearts` 用 `light_purple`）。
  以前 `HeartCounts.of` 直接 `Math.max(0, cracked)`，粉心永远画不出来。
- `HeartCounts` 新增 `PINK = 5`（`COUNT` 5 → 6），`ICONS`/`COLORS` 各加一栏；
  `CRACKED` 的颜色从粉改成暗红。
- 渲染顺序变为 红 → 魂 → 黑 → 蓝 → 碎 → 粉，两者互斥，最多出现一个。
- 协议未改；纯客户端路径仍拿不到 `CrackedHearts`（该 objective 不在显示槽里）。
- 自检：`checks=1495 failures=0`（+16 条：正负号拆分、`Integer.MIN_VALUE` 不溢出、粉心行 `❤50 💗5`、碎心行 `❤50 💔5`、两个配色常量）。

## 1.14.1

- **心数行合并成一组**：各类心之间由 `" | "` 改为只隔一个空格，整组算一段；
  `|` 现在只出现在「心组 ↔ 护甲」之间。
  例：`❤160 💗2 | 🛡3 ☃5`（1.14.0 是 `❤160 | 💗2 | 🛡3 ☃5`）。
- 新增常量 `EnemyHudLayout.HEART_GAP = " "`，与 `SEPARATOR = " | "` 区分开；
  顺序与配色不变（红 → 魂 → 黑 → 蓝 → 碎，各用各的颜色）。
- 协议、配置项、界面位置均未改动，可与 1.14.0 服务端混用。
- 自检：`checks=1479 failures=0`（+3 条：组内无分隔符 / 组间有分隔符 / 两色心精确相等）。

## 1.14.0

- **服务端选装组件**：新增 `MobHealthPayload`（S2C `ctt-hud-fork:mob_health`），
  服务端 `CttEnemyHudServer` 每 4 tick（5 Hz）按玩家周围最近的 32 只怪推
  `uuid / AllHearts / MaxHP / 魂 / 黑 / 蓝 / 碎 / MaxSoul / MaxBlack / MaxBlue / 防御 / 元素护甲×7 / targetted`。
  差量（内容一致不重发）+ 每 3 s keepalive；`canSend` 为假（客户端没装）完全不发。
- **客户端优先使用**：`ClientMobHealthCache`（UUID → Entry，5 s 失鲜，断线清空），
  `EnemyData.fromPayload` 复现数据包 `HeartValue` 分段（蓝 → 黑 → 魂 → 红）算血条比例，
  红心由 `AllHearts - 魂 - 黑 - 蓝` 反算。失鲜或关闭 `useServerData` → 回落 1.13.5 的纯客户端路径。
- 新增配置 `useServerData`（默认开，界面在敌人设置）。
- `fabric.mod.json` 的 `environment` 改为 `*` 并加 `main` 入口（只装客户端时该入口在集成服务器上跑，
  单机自动获得完整数据；只装服务端时客户端不受影响）。
- **修一个启动崩溃**：`CustomPacketPayload.createType(String)` 收的是 path 不是 `ns:path`，
  传 `"ctt-hud-fork:mob_health"` 会在 main 入口抛 `IdentifierException`，改用
  `new Type<>(Identifier.fromNamespaceAndPath(...))`。Fabric API 6.x 的注册入口是
  `PayloadTypeRegistry.clientboundPlay()`（`playS2C()` 已不存在）。
- 自检：`checks=1476 failures=0`（新增 18 条 payload 换算断言）。

## 1.13.5

- **修复聊天栏被全部拦截**：`ClientReceiveMessageEvents.ALLOW_GAME` 是「true = 允许显示」，
  1.13.4 把 `shouldHide` 直接注册进去 → 非目标消息全被拦掉。改为 `!shouldHide(...)`。
- **修复设置界面按钮错位**：子页面改为返回实际占用行数，「返回」落在最长一列下方。
- **同名敌人血条唯一化**：`Bar.expectedPercent()`（`min(AllHearts, MaxHP) * 100 / MaxHP`）+
  绑定粘性 + 名字完全一致 + 距离 四级打分。
- 自检：`checks=1458 failures=0`。

## 1.13.4

- **定位澄清：客户端必装、服务端选装。** 数据全部来自客户端可见渠道。
- **队友条与敌人条共用同一条绘制路径**：`BossBarTexture.Tint` → 原版 `boss_bar/*_background|progress`。
- **恢复自动开队伍条**：3 s 冷却 / 5 次失败 → 10 分钟熔断 / 连续消失 10 s 才允许重开（防误关）/
  暂停与界面中不发 / 单局最多 20 次 / 切世界清零。
- **新增 `ChatTriggerFilter`**：吞成功回显与 `commands.trigger.failed*` 报错。
- 自检：`checks=1450 failures=0`。

## 1.13.3

- **总界面一纵排布**（通用 / 敌人 / 玩家 / 恢复默认）+ 底部「完成」，「取消」全部移除。
- **移除三项显示开关**（永久开启）：`showNumbers` / `showHeartCounts` / `showArmor`。
- **两条 Boss 栏开关并入通用设置并改名**：`hideEnemyBossBar` →「隐藏上方Boss栏 - 最近敌人的信息」，
  `hideTeamBossBar` →「隐藏上方Boss栏 - 队伍血量」。
- **修复顶部队伍血量条「时不时闪一下」**：宽松扫描 `TeamBarTitle.scanLoose` + Tab 名单匹配名字。
- （1.13.4 已撤销）删掉 `PartyBossbarAutoToggle` 与 `autoPartyBossbar`。
- 自检：`checks=1449 failures=0`。

## 1.13.2

- **设置界面恢复「完成 / 取消」**：完成 = 写盘并关闭；取消 = 回滚到打开总界面时的快照并关闭（不写盘）。
- **总界面不再上下居中**：四项按两列两行从 `TOP=52` 起排，「完成 / 取消」贴屏幕底部。
- 「恢复默认」改为只改内存不落盘。
- 自检：`checks=1440 failures=0`。

## 1.13.1

- **修复 `floor` 条（高塔层数）被误杀**：它和队伍条同形（`高塔 (12/50)`），
  只按形状判定会被 `hideTeamBossBar` 隐藏；现在要求「至少一段名字能匹配真实玩家」才算队伍条。
  同步修正 `teamEntries` / `isTeammate` / `hasTeamBar` 的误判（会导致队友条整组不显示）。
- **设置界面改版**：总界面四项（通用设置 / 敌人设置 / 玩家设置 / 恢复默认），点进去分项细调，
  去掉「上一类 / 下一类」；`debug` 从界面移除（保留在 JSON）。
- 玩家页的顶部队伍条开关改为**显示与否**（`!hideTeamBossBar`），不再是「是否自动开启」；
  `autoPartyBossbar` 退居配置文件。玩家血条的长度/缩放/偏移/距离四项补进界面。
- **移除自绘名称**（`hideVanillaName` 及其代码）：分数一摘原版名字就会落回原位，自绘多余。
- 自检：`checks=1440 failures=0`。

## 1.13.0

- **心数行**：`❤红 | 💛魂 | 🖤黑 | 💙蓝 | 💔碎`，只写存在的类型；敌人两类心由
  `min(AllHearts, MaxHP)` 与 `AllHearts - 红心` **精确反推**，无需服务端组件。
- **心类配色改回常识**：魂心→黄条、蓝心→蓝条、黑心→紫条、红心→红条（1.12.0 的映射错了）。
  敌人的红心通道数据包用的是 `blue` 颜色，已在 `BossBarTracker.kindOf` 里翻译。
- **行位整体下移**：文字行 / 血条改为 `-18 ~ -9` / `-7 ~ -2`，三段紧贴、不再留 13 px 空档。
- 新增配置 `showHeartCounts`。
- 新增 `HudTarget`：Mixin 与渲染元素共用同一判据。
- 自检：`checks=1441 failures=0`。

## 1.12.0

- `hideBelowNameScore`（默认开）隐藏名称下方的计分板分数。
- 删除「远端百分比文字」：只有百分比数据时不再显示 `73%`，护甲照旧。
- 玩家血条独立设置：`hideTeamBossBar` / `teamBarWidth` / `teamScale` / `teamVerticalOffset` / `teamMaxDistance`。
- 血条按心类变色：魂心→蓝、黑心→紫、蓝心→黄（红心保持红）。
- 自检：`checks=1417 failures=0`。

## 1.11.0

- `showPercentText`（默认开）：只有百分比数据时可选择是否显示 `73%`；关掉后仍保留护甲与血条。
- 血量与护甲之间改为 `" | "` 分隔。
- `opacity` 默认 0.75。
- 回补 `hideEnemyBossBar`（`@Redirect` 隐藏顶部最近敌人 Boss 条，默认开）。
- 回补队友头顶血条（绿色原版 bossbar 纹理）+ 自动开启队伍条。
- 设置界面按「总开关 / 敌人血条 / 玩家血条 / 通用」分四类分页。
- 自检：`checks=1432 failures=0`。

## 1.10.0

**回退 + 修复**

- 功能集回退到「仅在实体名牌上方显示血量」：移除队友/自身面板、队友头顶条、
  队伍条自动开启、命令间隔闸门、顶部 Boss 条隐藏。
- 保留：精确值（客户端 Boss 栏）+ 百分比兜底、未知诅咒门控、Mod Menu 单页设置。
- **修复血条纹理**：改走 `GuiGraphicsExtractor.blitSprite(pipeline, id, x, y, w, h, color)`，
  sprite 由 GUI 图集自行解析；不再手工拼 UV（旧写法推导 `atlasW` 有偏差时会采样到图集别处）。
- 自检：`checks=1407 failures=0`。

## 各版本要点（历史）

- **1.0.0** 敌人头顶血条：屏幕空间投影、原版名牌锚点、红色 bossbar 纹理。
- **1.1.0** 血条长度可调；引入纯函数布局 + 穷举自检。
- **1.2.0** 发现 `entity.entityTags()` 在客户端永远为空（原版不同步实体标签），
  改用「有没有 CTT 血量分数」判定敌人；接 Mod Menu。
- **1.3.0** 找到纯客户端精确值通道：解析 `minecraft:targetting` Boss 条标题；首个 Mixin。
- **1.4.0** 区间调整：血条 50–150（默认 100）、缩放 0.75–1.50、垂直偏移 -30–0；删除最低最大血量。
- **1.5.0** 用 `@Redirect` 隐藏数据包的最近敌人 Boss 条。
- **1.6.0** 队友头顶血条（绿色 bossbar 纹理）+ 未知诅咒门控（无开关）。
- **1.7.0** 自动开启队伍血量条，带冷却与熔断。
- **1.8.0** 队友/自身 2D 面板、拖拽界面、命令间隔闸门。
- **1.9.0** Mod ID 与资源命名空间改为 `ctt-hud-fork`；间隔上限 5000ms；重做自动开关逻辑；
  面板可调透明度/宽度。
