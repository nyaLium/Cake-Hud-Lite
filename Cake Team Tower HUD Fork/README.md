# Cake Team Tower HUD - Fork

> **这是 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud) 的 Fork 版**（原作者 TheHolyLord，MIT 许可）。
>
> 当前版本 **1.14.5**：敌人血条为「心数 → 血条 → 名称」三段一体，血条按当前心类换原版配色纹理；
> 设置界面一纵排布（通用 / 敌人 / 玩家 / 恢复默认 + 底部完成）。

| 项目 | 内容 |
| --- | --- |
| Mod ID | `ctt-hud-fork` |
| 版本 | `1.14.0` |
| Minecraft | `26.3` |
| 环境 | 仅客户端 |
| 依赖 | Fabric Loader ≥ 0.20.1、Fabric API、Java ≥ 25；**Mod Menu 可选** |
| 产物 | `build/libs/cake_team_tower_hud_fork-1.14.5.jar` |

## 显示效果

```
        ❤120/120 💛40 | 🛡3 🔥5    ← 放得下：一行（红心写「当前/上限」，同类只用空格隔开）
        ▬▬▬▬▬▬ 原版 bossbar 纹理 ▬▬▬▬▬▬
        冰霜狼                      ← 原版名称（本 Mod 只摘掉下面那行分数）

        ❤200/200                    ← 放不下：红心单独占上面一行
        💗5 | 🛡5 🔥3 ☃1            ← 其余心 + 护甲留在下面一行（换行代替了那个 |）
        ▬▬▬▬▬▬ 原版 bossbar 纹理 ▬▬▬▬▬▬
        冰霜狼
```

| 行 | 距离锚点 | 说明 |
| --- | --- | --- |
| 心数 / 护甲 | -18 ~ -9 像素（折行时上面的行到 -27 起） | `verticalOffset` 可再往上挪 |
| 血条 | -7 ~ -2 像素 | 原版 bossbar 纹理，5 像素高 |
| 名称（原版） | 0 ~ 9 像素 | 分数一摘，原版自己就落回这一行 |

坐标单位是「原版名牌像素」，1 像素 = 0.025 格，锚点复用原版名牌锚点
（`EntityAttachment.NAME_TAG` + 0.5 格），所以与原生名牌同字号、同远近缩放。

> 名字为什么自己就下来了：`EntityRenderer.submitNameDisplay` 的那次上抬
> `9 × 1.15 × 0.025` 写在 `if (scoreText != null)` 里。本 Mod 把 `below_name` 分数摘掉后，
> 上抬不再执行，名字自然落回 `y ∈ [0, 9]`（原来分数占的那行），所以不需要自己画名字。

## 心类与配色

血条用**原版 bossbar 纹理**（`red / yellow / purple / blue` 四套），按当前显示的心类换色：

| 心类 | 血条 | 图标 | 数据包通道 |
| --- | --- | --- | --- |
| 红心 | 红 | ❤ 当前/上限 | `HeartValue=1`（敌方条颜色 `blue`） |
| 魂心 | **黄** | 💛 | `HeartValue=2`（敌方条颜色 `yellow`） |
| 黑心 | **紫** | 🖤 | `HeartValue=3`（玩家条颜色 `purple`） |
| 蓝心 | **蓝** | 💙 | `HeartValue=4`（玩家条颜色 `blue`） |
| 碎心 | — | 💔 暗红 | `CrackedHearts > 0`，客户端拿不到（只在服务端组件里推） |
| 粉心 | — | 💗 亮紫 | `CrackedHearts < 0`（取绝对值），同样是 `CrackedHearts` 这一栏 |

## 心数怎么算出来的（纯客户端）

数据包 `misc/health.mcfunction`：

```
AllHearts = RedHearts + SoulHearts + BlackHearts + BlueHearts
RedHearts = min(RedHearts, MaxHP)          # MaxHP = 红心上限
伤害顺序：蓝 → 黑 → 魂 → 红                 # 前面全空，红心才掉
```

所以 **红心恒等于 `min(AllHearts, MaxHP)`**，多出来的部分只能是其余心。
敌人只可能带魂心（带 `NoSoulHearts` 标签的怪连魂心都没有），于是：

```
红心 = min(AllHearts, MaxHP)
魂心 = AllHearts - 红心
```

两个数都在客户端 Boss 栏标题里（`名字 (HP 120/100) 🛡3`），
所以**敌人心数 100% 精确，不需要任何服务端组件**。

## 数据来源（客户端必装 / 服务端选装）

| 数据源 | 能拿到什么 | 覆盖 | 需要服务端吗 |
| --- | --- | --- | --- |
| **服务端组件 `ctt-hud-fork:mob_health`** | 按 **UUID** 的 `AllHearts / MaxHP / 护甲 / 红魂黑蓝碎` | 视野内最近 32 只怪 | 选装 |
| 客户端 Boss 栏 `targetting*` 标题 | 精确 `AllHearts / MaxHP` + `🛡防御` + 元素护甲 | 被地图锁定的那一只 | 否 |
| 同一条 bar 的 progress / color | 当前心类的剩余比例 + 是哪一类心 | 同上 | 否 |
| `below_name` 展示槽的 `HealthPercent` | 0~100 百分比（红心口径） | 视野内全部敌人 | 否 |
| 队伍条 `spectator_bossbar` | 每名队友的 `AllHearts / MaxHP` | 队伍条存在时 | 否 |

敌人判据是「有没有 CTT 的血量分数」，**不用 scoreboard tag**（原版不同步实体标签，客户端永远是空集）。

### 三种安装组合

| 客户端 | 服务端 | 效果 |
| --- | --- | --- |
| ✅ | ❌ | 被锁定的那只 `x/y` + 心数；其余怪只有百分比 |
| ✅ | ✅ | **每只怪**都有 `x/y` + 护甲 + 各类心，同名怪也不串（按 UUID） |
| ❌ | ✅ | 服务端什么都不发（`canSend` 为假），零副作用 |

单机（集成服务器）自动等于「两端都装」。

### 服务端组件协议

`MobHealthPayload`（S2C，每 4 tick = 5 Hz，差量 + 3 s keepalive）：

| 字段 | 说明 |
| --- | --- |
| `version` | 协议版本，客户端遇到更高版本整包忽略 |
| `entries[]` | 最近 32 只怪，按距离 |

单条：`uuid / hp(AllHearts) / maxHp / soul / black / blue / cracked / maxSoul / maxBlack / maxBlue / defence / armour[7] / targetted`。
可负字段用 zigzag varint，一只怪约 40 字节。红心由 `AllHearts - 魂 - 黑 - 蓝` 反算。
客户端在 5 s 内收到包才用，超时自动回落到纯客户端路径（`ClientMobHealthCache`）。

### 上方 Boss 栏的两条如何识别

| Boss 条 | 形状 | 识别方式 |
| --- | --- | --- |
| 最近敌人信息（`targetting*`） | `名字 (HP 120/100) 🛡3` | 括号里有 `HP` 前缀 |
| 队伍血量（`spectator_bossbar`） | `A (10/20) B (30/40) (/) …` | 至少一段名字能匹配到**玩家列表**里的真实玩家 |
| 高塔层数（`floor`） | `高塔 (12/50)` | 名字是场景名，匹配不到玩家 → **不会被误隐藏** |

队伍条标题按 10 个槽位拼，空槽会渲染成 ` (/) ` 残渣，所以判定走两道：
先严格整串解析（`TeamBarTitle.parse`），失败再宽松扫描（`TeamBarTitle.scanLoose`）。
名字匹配来源有本地玩家、视野内玩家实体、以及 Tab 名单（`getPlayerInfo`）三路 ——
最后一路很关键，队伍条会列出不在渲染距离内的队友。

### 队友为什么没有心数

玩家独有的黑心 / 蓝心只出现在**玩家自己**的 `player_1001..1010` 上，而那条 bar
用 `bossbar set ... players @a[scores={BossbarID=100N}]` 只发给本人；
队友能看到的 `spectator_bossbar` 标题里只有 `AllHearts/MaxHP`。四个分项拆不开，所以队友这一行
按原样显示 `当前/最大`（不做近似，宁缺毋假）。补齐需要服务端组件（参考 Cake-Hud 的
`TeamHeartsPayload`），那是另一个工程。

## 同名敌人的唯一认领

装了服务端组件时**根本不需要认领**：数据按实体 UUID 走，名字再像也不会串。

纯客户端时没有 UUID，只能靠两个信号把那条 Boss 栏锁到唯一一只：

1. **`below_name` 的 `HealthPercent`**：只有真正被锁定的那只，它的百分比才等于
   bar 里的 `min(AllHearts, MaxHP) * 100 / MaxHP`（整数截断，与 `misc/health.mcfunction:102-104` 同口径）；
2. **绑定粘性**：按 bar 的 UUID 记住上一帧绑到哪只实体，只要它还在候选里就优先继续绑
   —— 名字一样的两只怪贴得很近时，数字不会在它们之间来回跳。

打分顺序（从强到弱）：百分比精确吻合 → 差 ≤ 2（同步差一两个 tick）→ 名字完全相同 →
上一帧绑定 → 最后才是距离。都匹配不上时宁可不给绝对值（那只怪退回百分比条），
也不会把数字挂到错的怪头上。

## 设置界面

```
CTT敌人血条设置
  通用设置
  敌人设置
  玩家设置
  恢复默认
         …
  [ 完成 ]        ← 贴屏幕最底部
```

| 入口 | 内容 |
| --- | --- |
| 通用设置 | 左列：总开关 / 文字阴影 / 隐藏名称下方分数 / 透明度；右列：被墙挡住时隐藏 / **隐藏上方Boss栏 - 最近敌人的信息** / **隐藏上方Boss栏 - 队伍血量** |
| 敌人设置 | 左列：显示血条 / 显示距离；右列：血条长度 / 整体缩放 / 垂直偏移 / **优先使用服务端数据** |
| 玩家设置 | 左列：显示队友血量 / **自动开启队伍血量条** / 显示距离；右列：血条长度 / 整体缩放 / 垂直偏移 |
| 恢复默认 | 只改内存不落盘，点「完成」才写盘 |
| 完成 | 归一化后写盘并关闭（总界面 ESC 等同「完成」） |

- 只有「完成」，**没有「取消」**；子界面底部只有「返回」（改动保留，写盘由总界面负责）。
- 「返回」按钮会落在**最长那一列的下方**（`contentBottom + 12`），不会再压住最后一个开关。
- 心数 / 护甲 / 血量数字**永久开启**，没有开关。

## 队友血条的数据从哪来（客户端必装 / 服务端选装）

所有数据都来自**客户端可见**的渠道，服务端不装任何东西也完整可用：

| 数据 | 客户端渠道 |
| --- | --- |
| 队友血量 | 「队伍血量」Boss 栏标题（`spectator_bossbar`），靠 `/trigger TogglePartyBossbar` 打开 |
| 队友命数 | 本地计分板 `Lives`（原版会同步） |
| 敌人精确血量 / 护甲 / 心数 | 原版 Boss 栏标题 + 数据包心模型反推 |

那条队伍条**默认是关的**，数据包里 `TogglePartyBossbarA` 第一次按后一直保持 1（每 tick 重新打 tag），
所以它是**真开关**——再按一次才关。本 Mod 会自动开（见下一节）。

> 服务端**如果**将来愿意装一个 mod 组件（像 Cake-Hud 的 `TeamHeartsPayload`）就能补上队友四色心；
> 那是纯可选增强，不做也不影响任何现有功能。

## 自动开队伍条（3 s 冷却 / 5 次失败 → 10 分钟熔断）

`PartyBossbarAutoToggle`，参数与 Cake-Hud 的 `maybeAutoTogglePartyBossbar` 一致：

| 项 | 值 / 行为 |
| --- | --- |
| 触发前置 | 必须已看到「自己的」Boss 条（进局了）且当前**没看到**队伍条 |
| 冷却 | 3 s |
| 连续失败 | 5 次 → **10 分钟熔断**，期间完全不发 |
| 防误关 | 见过队伍条后，要**连续消失 10 s** 才允许再开（一次检测抖动不会把条点掉） |
| 界面保护 | 暂停 / 背包 / 聊天界面里一律不发 |
| 上限 | 单局最多 20 次；切世界状态清零 |

> 历史坑：1.12.x 的旧版检测队伍条只看「整串 `名字 (x/y)`」，标题里夹着 ` (/) ` 空槽残渣时解析失败，
> 于是一直认为「没开」→ 每 3 秒按一次 → **开了又关**，表现为「顶部队伍血量条时不时闪一下」，
> 触发项没 enable 时还会刷「无法触发该计分项」。1.13.3 起检测改成宽松扫描 + Tab 名单匹配，
> 1.13.4 起把自动开启按上面的策略恢复，并把聊天回显 / 报错一起过滤掉。

## 聊天栏不再被自己的命令刷屏

`ChatTriggerFilter`（注册在 `ClientReceiveMessageEvents.ALLOW_GAME`），只在自己刚发过命令的 3 秒内过滤，
手动敲 `/trigger` 不受影响：

| 吞掉的消息 | 判定 |
| --- | --- |
| 成功回显 | 文本含 `Triggered` / `已触发` 且含 `TogglePartyBossbar` |
| 失败报错 | 翻译键以 `commands.trigger.failed` 开头（「无法触发该计分项」） |

## 血条纹理（敌人 / 队友同一条路径）

**不打包任何材质**，直接调用原版 GUI 图集里的
`minecraft:boss_bar/{red,yellow,purple,blue,green}_{background,progress}`：

```java
BossBarTexture.Tint tint = p.player ? Tint.GREEN : tintFor(p.heartKind);
BossBarTexture.drawBackground(g, bx, 0, W, H, white, tint);
BossBarTexture.drawProgress(g, bx, 0, fill, H, white, tint);
```

走的是 `GuiGraphicsExtractor.blitSprite(pipeline, id, x, y, w, h, color)` ——
sprite 由图集自己解析（含 `GuiSpriteScaling` 处理），颜色参数带 alpha。
早期版本手工拼 UV（`getX()/getY()` + 自算图集尺寸）推导有偏差时会采样到图集别处，
表现就是「血条不再是原版纹理」；现在这条路径与 `BossHealthOverlay` 画 Boss 条一致。
1.13.4 起队友条也走这同一条调用（只是 tint 固定绿），不再有单独的绿色分支。

## 配置

`config/ctt-enemy-hud.json`（首次启动生成；游戏内改 2 秒热重载，也可用 Mod Menu 界面）。

```json
{
  "enabled": true,
  "opacity": 0.75,
  "textShadow": true,
  "showBar": true,
  "useServerData": true,
  "hideBelowNameScore": true,
  "hideEnemyBossBar": true,
  "showTeammates": true,
  "autoPartyBossbar": true,
  "hideTeamBossBar": true,
  "teamBarWidth": 100.0,
  "teamScale": 1.0,
  "teamVerticalOffset": 0,
  "teamMaxDistance": 48.0,
  "maxDistance": 48.0,
  "barWidth": 100.0,
  "wrapWidth": 200.0,
  "scale": 1.0,
  "verticalOffset": 0,
  "hideBehindWalls": true,
  "debug": false
}
```

| 字段 | 说明 | 范围（越界自动夹回） |
| --- | --- | --- |
| `enabled` | 总开关 | — |
| `opacity` | 文字 + 血条不透明度（名称不吃） | 0.50 ~ 1.00（默认 0.75） |
| `textShadow` | 文字阴影 | — |
| `showBar` | 是否画血条（心数 / 护甲永久显示） | — |
| `useServerData` | 优先使用服务端组件推来的权威数据（服务端没装则无影响） | — |
| `hideBelowNameScore` | 隐藏数据包挂在名称下方的 `HealthPercent` 分数（名字因此自动落到原位） | — |
| `hideEnemyBossBar` | 隐藏上方 Boss 栏里「最近敌人的信息」 | — |
| `hideTeamBossBar` | 隐藏上方 Boss 栏里「队伍血量」（数据照常读取） | — |
| `showTeammates` | 显示队友血量（绿色条，自己永不显示） | — |
| `autoPartyBossbar` | 自动 `/trigger TogglePartyBossbar` 打开「队伍血量」条（队友血量的纯客户端数据源） | — |
| `teamBarWidth` / `teamScale` / `teamVerticalOffset` | 玩家血条的长度 / 缩放 / 垂直微调 | 50~150 / 0.75~1.50 / -30~0 |
| `teamMaxDistance` | 玩家血条显示距离（格） | 4 ~ 256 |
| `wrapWidth` | 换行阈值（原版名牌像素）。`200` = **自动**，即不超过血条长度；整行超过它就折两行 | 40 ~ 200 |
| `maxDistance` | 敌人血条显示距离（格） | 4 ~ 256 |
| `barWidth` | 敌人血条长度（原版名牌像素） | 50 ~ 150（默认 100） |
| `scale` | 敌人整体缩放 | 0.75 ~ 1.50 |
| `verticalOffset` | 敌人垂直微调，负值更高 | -30 ~ 0 |
| `hideBehindEntities` | 被更近的实体挡住（屏幕矩形覆盖 ≥70%）时不绘制；解决「后方血条压住前面名字/模型」 | — |
| `hideBehindWalls` | 被方块挡住视线时不画 | — |
| `debug` | 左上角打印 `entities= / withData= / mates= / drawn=`（**仅配置文件，界面无此项**） | — |

> 1.13.3 起 `showNumbers` / `showHeartCounts` / `showArmor` 已删除（永久开启），
> 老配置文件里的这几个键会被忽略，下次「完成」时自动消失。
> `autoPartyBossbar` 在 1.13.3 一度移除，1.13.4 恢复（默认开）。

## 未知诅咒

数据包 `floors/curse_rooms/d_curse02_unknown.mcfunction` 会把个人状态条换成乱码并清空展示槽。
本 Mod 用翻译键前缀 `Floors_Curse_Rooms_D_Curse02_Unknown_` 识别，命中期间**整个 UI 不画**，
无开关（符合地图设定）；诅咒结束后 2 秒自动恢复。

## 自检

```bash
gradlew selfCheck      # gradlew build 会自动带上
```

**1476 条断言**：8 种显示开关组合 × 6 种数据形态、血条长度 9 档、垂直偏移 4 档、缩放 5 档、
心数推导边界（溢出 / 受击 / 0 / 负值 / 越界下标）、心数行顺序与组内/组间分隔、队伍条严格解析与宽松扫描、
配置夹取（越界 / NaN / null）、Boss 栏标题解析（中英文/全角/负血/零上限）、诅咒键判定。

## 构建

```bash
# JDK 25
./gradlew build
```
产物 `build/libs/cake_team_tower_hud_fork-1.14.5.jar` → 丢进 `.minecraft/mods`。

## 版本沿革

| 版本 | 内容 |
| --- | --- |
| 1.4.0 | 仅头顶血条（功能集基线） |
| 1.5.0 | + 隐藏顶部敌人 Boss 条 |
| 1.6.0 | + 队友头顶条、诅咒门控 |
| 1.7.0 | + 自动开启队伍条 |
| 1.8.0 | + 队友/自身面板、拖拽、命令闸门 |
| 1.9.0 | Mod ID 改为 `ctt-hud-fork` |
| 1.11.0 | 回退到 1.4.0 功能集 + 修复血条纹理 |
| 1.12.0 | 隐藏 below_name 分数、删远端百分比文字、玩家血条独立设置、心类配色 |
| 1.13.0 | 心数行（红/魂精确）、魂心→黄条·蓝心→蓝条；名称一度改为自绘 |
| 1.13.1 | 修复「高塔层数」Boss 条被当成队伍条隐藏；设置界面改为四项菜单；移除自绘名称 |
| 1.13.2 | 设置界面恢复「完成 / 取消」；总界面四项两列排布 |
| 1.13.3 | 一纵排布 + 底部完成（取消移除）；心数/护甲永久开启；两条 Boss 栏开关并入通用并改名 |
| 1.13.4 | 定位「客户端必装、服务端选装」；队友条与敌人条共用同一条原版 bossbar 纹理绘制；恢复自动开队伍条；聊天回显与报错过滤 |
| 1.13.5 | 修聊天栏被全拦（`ALLOW_GAME` 返回值反了）；设置页「返回」按实际行数定位；同名敌人按 `HealthPercent` + 绑定粘性唯一认领 |
| **1.14.0** | 新增服务端选装组件（按 UUID 推权威血量/护甲/各类心，5 Hz 差量）；客户端优先使用并自动回落；新增 `useServerData` |

## 已知限制

- 只在 **26.3** 上验证；1.21.4 需另开分支。
- **纯客户端**下精确血量只覆盖 Boss 栏指向的那一只，其余是百分比；装了服务端组件后每只怪都有绝对值。
- 队友的黑/蓝/碎心客户端拿不到（`spectator_bossbar` 只有 `AllHearts/MaxHP`），那一行保持 `当前/最大`；
  同一套服务端通道扩一列即可补上，尚未做。
- 队友头顶条依赖「队伍血量」这条 Boss 栏自身存在（自动开启会替你开，3 s 冷却 + 10 分钟熔断）。
