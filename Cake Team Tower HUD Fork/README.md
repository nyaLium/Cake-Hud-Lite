# Cake Team Tower HUD - Fork

> **这是 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud) 的 Fork 版**（原作者 TheHolyLord，MIT 许可）。
>
> 当前版本 **1.13.1**：敌人血条为「心数 → 血条 → 名称」三段一体，血条按当前心类换原版配色纹理；
> 设置界面改成「通用设置 / 敌人设置 / 玩家设置 / 恢复默认」四项菜单，点进去分项细调。

| 项目 | 内容 |
| --- | --- |
| Mod ID | `ctt-hud-fork` |
| 版本 | `1.13.1` |
| Minecraft | `26.3` |
| 环境 | 仅客户端 |
| 依赖 | Fabric Loader ≥ 0.20.1、Fabric API、Java ≥ 25；**Mod Menu 可选** |
| 产物 | `build/libs/cake_team_tower_hud_fork-1.13.1.jar` |

## 显示效果

```
        ❤120 | 💛40 | 🛡3 🔥5      ← 心数（只写存在的类型）+ 护甲
        ▬▬▬▬▬▬ 原版 bossbar 纹理 ▬▬▬▬▬▬
        冰霜狼                      ← 原版名称（本 Mod 只摘掉下面那行分数）
```

| 行 | 距离锚点 | 说明 |
| --- | --- | --- |
| 心数 / 护甲 | -18 ~ -9 像素 | `verticalOffset` 可再往上挪 |
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
| 红心 | 红 | ❤ | `HeartValue=1`（敌方条颜色 `blue`） |
| 魂心 | **黄** | 💛 | `HeartValue=2`（敌方条颜色 `yellow`） |
| 黑心 | **紫** | 🖤 | `HeartValue=3`（玩家条颜色 `purple`） |
| 蓝心 | **蓝** | 💙 | `HeartValue=4`（玩家条颜色 `blue`） |
| 碎心 | — | 💔 | `CrackedHearts`（负数 = 粉心），客户端拿不到 |

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

## 数据来源（纯客户端）

| 数据源 | 能拿到什么 | 覆盖 |
| --- | --- | --- |
| 客户端 Boss 栏 `targetting*` 标题 | 精确 `AllHearts / MaxHP` + `🛡防御` + 元素护甲 | 被地图锁定的那一只 |
| 同一条 bar 的 progress / color | 当前心类的剩余比例 + 是哪一类心 | 同上 |
| `below_name` 展示槽的 `HealthPercent` | 0~100 百分比（红心口径） | 视野内全部敌人 |
| 队伍条 `spectator_bossbar` | 每名队友的 `AllHearts / MaxHP` | 全队 |

敌人判据是「有没有 CTT 的血量分数」，**不用 scoreboard tag**（原版不同步实体标签，客户端永远是空集）。

### 队伍条怎么和「高塔层数」条区分

数据包的 `floor` 条同样长成 `名字 (x/y)`（`高塔 (12/50)`、`休息室 (3/50)` …，
见 `break_room_universal.mcfunction:397-402`），只按形状判定会把高塔层数整条隐藏掉。
所以队伍条判定额外要求**至少有一段名字能匹配到真实玩家**（`BossBarTracker.looksLikeTeamBar`），
`floor` 条的名字是场景名，永远匹配不到，于是不再被误杀。

### 队友为什么没有心数

玩家独有的黑心 / 蓝心只出现在**玩家自己**的 `player_1001..1010` 上，而那条 bar
用 `bossbar set ... players @a[scores={BossbarID=100N}]` 只发给本人；
队友能看到的 `spectator_bossbar` 标题里只有 `AllHearts/MaxHP`。四个分项拆不开，所以队友这一行
按原样显示 `当前/最大`（不做近似，宁缺毋假）。补齐需要服务端组件（参考 Cake-Hud 的
`TeamHeartsPayload`），那是另一个工程。

## 设置界面

```
CTT敌人血条设置
┌──────────────┐
│  通用设置     │  总开关 / 文字阴影 / 隐藏名称下方分数 / 透明度 / 被墙挡住时隐藏
│  敌人设置     │ 显示血条 / 数字 / 心数 / 护甲 / 隐藏顶部敌人Boss条 + 长度 / 缩放 / 偏移 / 距离
│  玩家设置     │ 显示队友血量 / 显示顶部队伍血量条 / 长度 / 缩放 / 偏移 / 距离
│  恢复默认     │ 立刻恢复默认并落盘
└──────────────┘
```

子界面底部是「返回」（ESC 同样返回），写盘统一发生在关闭总界面时。

## 血条纹理

**不打包任何材质**，直接调用原版 GUI 图集里的
`minecraft:boss_bar/{red,yellow,purple,blue,green}_{background,progress}`：

走的是 `GuiGraphicsExtractor.blitSprite(pipeline, id, x, y, w, h, color)` ——
sprite 由图集自己解析（含 `GuiSpriteScaling` 处理），颜色参数带 alpha。
早期版本手工拼 UV（`getX()/getY()` + 自算图集尺寸）推导有偏差时会采样到图集别处，
表现就是「血条不再是原版纹理」；现在这条路径与 `BossHealthOverlay` 画 Boss 条一致。

## 配置

`config/ctt-enemy-hud.json`（首次启动生成；游戏内改 2 秒热重载，也可用 Mod Menu 界面）。

```json
{
  "enabled": true,
  "opacity": 0.75,
  "textShadow": true,
  "showBar": true,
  "showNumbers": true,
  "showHeartCounts": true,
  "showArmor": true,
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
| `showBar` / `showNumbers` / `showArmor` | 分别开关血条 / 数字 / 护甲 | — |
| `showHeartCounts` | 数字优先画心数，拆不出来才退回 `当前/最大` | — |
| `hideBelowNameScore` | 隐藏数据包挂在名称下方的 `HealthPercent` 分数（名字因此自动落到原位） | — |
| `hideEnemyBossBar` | 隐藏数据包挂在顶部的最近敌人 Boss 条 | — |
| `showTeammates` | 显示队友血量（绿色条，自己永不显示） | — |
| `autoPartyBossbar` | 自动 `/trigger TogglePartyBossbar` 打开队伍条（队友血量的数据源；**仅配置文件，界面无此项**） | — |
| `hideTeamBossBar` | 隐藏顶部那条队伍血量条（数据照常读取） | — |
| `teamBarWidth` / `teamScale` / `teamVerticalOffset` | 玩家血条的长度 / 缩放 / 垂直微调 | 50~150 / 0.75~1.50 / -30~0 |
| `teamMaxDistance` | 玩家血条显示距离（格） | 4 ~ 256 |
| `maxDistance` | 敌人血条显示距离（格） | 4 ~ 256 |
| `barWidth` | 敌人血条长度（原版名牌像素） | 50 ~ 150（默认 100） |
| `scale` | 敌人整体缩放 | 0.75 ~ 1.50 |
| `verticalOffset` | 敌人垂直微调，负值更高 | -30 ~ 0 |
| `hideBehindWalls` | 被方块挡住视线时不画 | — |
| `debug` | 左上角打印 `entities= / withData= / mates= / drawn=`（**仅配置文件，界面无此项**） | — |

## 未知诅咒

数据包 `floors/curse_rooms/d_curse02_unknown.mcfunction` 会把个人状态条换成乱码并清空展示槽。
本 Mod 用翻译键前缀 `Floors_Curse_Rooms_D_Curse02_Unknown_` 识别，命中期间**整个 UI 不画**，
无开关（符合地图设定）；诅咒结束后 2 秒自动恢复。

## 自检

```bash
gradlew selfCheck      # gradlew build 会自动带上
```

**1440 条断言**：8 种显示开关组合 × 6 种数据形态、血条长度 9 档、垂直偏移 4 档、缩放 5 档、
心数推导边界（溢出 / 受击 / 0 / 负值 / 越界下标）、心数行顺序与分隔符、配置夹取（越界 / NaN / null）、
Boss 栏标题解析（中英文/全角/负血/零上限/队伍条）、诅咒键判定。

## 构建

```bash
# JDK 25
./gradlew build
```
产物 `build/libs/cake_team_tower_hud_fork-1.13.1.jar` → 丢进 `.minecraft/mods`。

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
| **1.13.1** | 修复「高塔层数」Boss 条被当成队伍条隐藏；设置界面改为四项菜单；移除自绘名称 |

## 已知限制

- 只在 **26.3** 上验证；1.21.4 需另开分支。
- 精确血量只覆盖 Boss 栏指向的那一只，其余是百分比（纯客户端上限）。
- 敌人心数（红 / 魂）精确；**队友的黑/蓝/碎心客户端拿不到**，那一行保持 `当前/最大`。
