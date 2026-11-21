# Cake Team Tower HUD - Fork

> **这是 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud) 的 Fork 版**（原作者 TheHolyLord，MIT 许可）。
>
> 当前版本 **1.11.0**：敌人（红条）与队友（绿条）的血量都画在名牌上方；顶部敌人 Boss 条默认隐藏；设置界面按「总开关 / 敌人血条 / 玩家血条 / 通用」四类分页。

| 项目 | 内容 |
| --- | --- |
| Mod ID | `ctt-hud-fork` |
| 版本 | `1.11.0` |
| Minecraft | `26.3` |
| 环境 | 仅客户端 |
| 依赖 | Fabric Loader ≥ 0.20.1、Fabric API、Java ≥ 25；**Mod Menu 可选** |
| 产物 | `build/libs/cake_team_tower_hud_fork-1.11.0.jar` |

## 显示效果

```
        当前血量/最大血量 — 🛡防御 🔥火 ☃冰 …
        ▬▬▬▬▬▬ 原版 bossbar 纹理 ▬▬▬▬▬▬
        原版的自定义名称（原版自己画，本 Mod 不碰）
```

居中对齐，锚点复用原版名牌锚点（`EntityAttachment.NAME_TAG` + 0.5 格），
按原版同一条公式（1 名牌像素 = 0.025 格）换算，与原生名牌同字号、同远近缩放。

## 数据来源（纯客户端）

| 数据源 | 能拿到什么 | 覆盖 |
| --- | --- | --- |
| 客户端 Boss 栏 `minecraft:targetting` 标题 | 精确 `当前/上限` + `🛡防御` + 元素护甲 | 被地图锁定的那一只 |
| `below_name` 展示槽的 `HealthPercent` | 0~100 百分比 | 视野内全部敌人 |

敌人判据是「有没有 CTT 的血量分数」，**不用 scoreboard tag**（原版不同步实体标签，客户端永远是空集）。

## 血条纹理

**不打包任何材质**，直接调用原版 GUI 图集：

- `minecraft:boss_bar/red_background`
- `minecraft:boss_bar/red_progress`

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
  "showArmor": true,
  "showPercentText": true,
  "hideEnemyBossBar": true,
  "showTeammates": true,
  "autoPartyBossbar": true,
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
| `opacity` | 整体不透明度（文字 + 血条） | 0.50 ~ 1.00（默认 0.75） |
| `textShadow` | 文字阴影 | — |
| `showBar` / `showNumbers` / `showArmor` | 分别开关血条 / 数字 / 护甲 | — |
| `showPercentText` | 只有百分比数据时是否显示 `73%`；关掉后仍显示护甲 | — |
| `hideEnemyBossBar` | 隐藏数据包挂在顶部的最近敌人 Boss 条 | — |
| `showTeammates` | 显示队友血量（绿色条，自己永不显示） | — |
| `autoPartyBossbar` | 自动 `/trigger TogglePartyBossbar` 打开队伍条（队友血量数据源） | — |
| `maxDistance` | 超过该距离（格）不画 | 4 ~ 256 |
| `barWidth` | 血条长度（原版名牌像素） | 50 ~ 150（默认 100） |
| `scale` | 整体缩放 | 0.75 ~ 1.50 |
| `verticalOffset` | 垂直微调，负值更高 | -30 ~ 0 |
| `hideBehindWalls` | 被方块挡住视线时不画 | — |
| `debug` | 左上角打印 `entities= / withData= / drawn=` | — |

## 未知诅咒

数据包 `floors/curse_rooms/d_curse02_unknown.mcfunction` 会把个人状态条换成乱码并清空展示槽。
本 Mod 用翻译键前缀 `Floors_Curse_Rooms_D_Curse02_Unknown_` 识别，命中期间**整个 UI 不画**，
无开关（符合地图设定）；诅咒结束后 2 秒自动恢复。

## 自检

```bash
gradlew selfCheck      # gradlew build 会自动带上
```

**1407 条断言**：8 种显示开关组合 × 6 种数据形态、血条长度 9 档、垂直偏移 4 档、缩放 5 档、
配置夹取（越界 / NaN / null）、Boss 栏标题解析（中英文/全角/负血/零上限/队伍条）、诅咒键判定。

## 构建

```bash
# JDK 25
./gradlew build
```
产物 `build/libs/cake_team_tower_hud_fork-1.11.0.jar` → 丢进 `.minecraft/mods`。

## 与本分支更早版本的区别

| 版本 | 内容 |
| --- | --- |
| 1.4.0 | 仅头顶血条（本版功能集基线） |
| 1.5.0 | + 隐藏顶部敌人 Boss 条 |
| 1.6.0 | + 队友头顶条、诅咒门控 |
| 1.7.0 | + 自动开启队伍条 |
| 1.8.0 | + 队友/自身面板、拖拽、命令闸门 |
| 1.9.0 | Mod ID 改为 `ctt-hud-fork` |
| **1.11.0** | **回退到 1.4.0 功能集 + 修复血条纹理** |

## 已知限制

- 只在 **26.3** 上验证；1.21.4 需另开分支。
- 精确血量只覆盖 Boss 栏指向的那一只，其余是百分比（纯客户端上限）。
- 心数（魂/黑/蓝/碎）客户端拿不到，需要服务端组件。
