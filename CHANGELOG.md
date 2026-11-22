# CHANGELOG · Cake Team Tower HUD - Fork

> 这是 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud)（作者 TheHolyLord，MIT）的 Fork 版。
> 版本保留规则见 [versions/VERSIONING.md](versions/VERSIONING.md)：**每个版本都必须留档，不覆盖、不删除。**

## 版本索引

| 版本 | 功能集 | 归档 |
| --- | --- | --- |
| **1.18.0** | 布局固定三段（**护甲最上 / 心数紧贴血条 / 血条 / 名称**），不再折行；**缩放与不透明度对文字生效**；血条默认长度 **75**（旧配置自动迁移） | `versions/v1.18.0/` |
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

## 1.18.0（当前）

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
