# CTT HUD - Fork

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/loader-Fabric%20%E2%89%A5%200.20.1-blueviolet.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-%E2%89%A5%2025-orange.svg)](https://adoptium.net/)

> **为地图“蛋糕塔”（Cake Team Towers）制作！**
>
> - 能在敌人、玩家的头顶显示血量条及相关信息
> - 盾牌进入冷却时，会显示冷却指示器
> - 能够随时查看属性的属性面板
>
> 纯客户端（Fork 自 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud)，MIT）；**客户端必装、服务端选装**，
> 服务端只装 Fabric API 也能用完整功能。
>
> 当前正式版 **1.2.10**。发布前的 67 个测试版（1.10.0 – 1.20.42）只在本地归档，**不随仓库上传**。

## 功能

- **头顶血条**：`心数 → 血条 → 名字` 三段一体，血条按当前心类换原版配色纹理（红/黄/紫/蓝/绿）；
  敌人与队友各自独立开关、长度、缩放、最远显示距离。
- **自身属性面板**：定时 `/trigger ViewStats` 抓地图回的属性行，画成 1~3 列面板；
  88 个属性逐项开关（三节分组 + 滚动过滤），可拖动定位、调透明度与文字阴影；
  支持「HUD / 容器界面 / 两者」三种显示方式，进存档先静默 3 s 避免刷屏。
- **盾牌指示器**：副手盾牌耐久计量条（34×86 纹理，按耐久四档），可调缩放比例与透明度、可拖动定位。
- **纯客户端**：所有基础数据来自客户端可见渠道（Boss 栏、计分板展示槽、实体），
  服务端装了本 Mod 才额外提供按 UUID 的权威血量（不装自动回落）。
- **不刷屏**：自己触发的 `ViewStats` 输出与 `/trigger` 回显都被吞掉；手动触发照常显示。

## 下载

jar 在 **GitHub Releases** 里：<https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases>

| 版本 | 附件 | 说明 |
| --- | --- | --- |
| **1.2.10**（最新） | `ctt-hud-fork-1.2.10.jar` + `-sources.jar` | 癫狂只隐藏属性面板（实体血条照常）；修魂心溢出时血条瞬间变满；「当前/上限」前加 ❤ |
| 1.2.9 | `ctt-hud-fork-1.2.9.jar` + `-sources.jar` | 癫狂期间也摘原版 `below_name` 分数 |
| 1.2.8 | `ctt-hud-fork-1.2.8.jar` + `-sources.jar` | 百分比文本前加红心：`❤ 73%` |
| 1.2.7 | `ctt-hud-fork-1.2.7.jar` + `-sources.jar` | 癫狂下显示「百分比文本 + 百分比条」 |
| 1.2.6 | `ctt-hud-fork-1.2.6.jar` + `-sources.jar` | 癫狂下改画百分比血条 |
| 1.2.5 | `ctt-hud-fork-1.2.5.jar` + `-sources.jar` | 癫狂改用专属 Boss 栏识别；未知诅咒也隐藏属性面板 |
| 1.2.4 | `ctt-hud-fork-1.2.4.jar` + `-sources.jar` | 未知诅咒也隐藏属性面板；癫狂隐藏实体血条、保留面板 |
| 1.2.3 | `ctt-hud-fork-1.2.3.jar` + `-sources.jar` | 玩家心数两行排版；心数统一缩写（999.9K）；盔甲改名防御 |
| 1.2.2 | `ctt-hud-fork-1.2.2.jar` + `-sources.jar` | 六类心统一 ❤ + 颜色；真盔甲常驻；队友绿条用 below_name 百分比；透明度 10%~100% |
| 1.2.1 | `ctt-hud-fork-1.2.1.jar` + `-sources.jar` | 修队友血量：前缀名误认领、蓝/魂心被夹到 MaxHP |
| 1.1.0 | `ctt-hud-fork-1.1.0.jar` + `-sources.jar` | 盾牌指示器换新纹理（34×86 四档）；Mod 简介改为「蛋糕塔」版本 |
| 1.1.0 | `ctt-hud-fork-1.1.0.jar` + `-sources.jar` | 盾牌指示器换新纹理（34×86 四档）；Mod 简介改为「蛋糕塔」版本 |
| 1.0.3 | `ctt-hud-fork-1.0.3.jar` + `-sources.jar` | 上一版：jar 内带仓库地址（Mod Menu 的 Homepage / Source / Issues） |

仓库里**不保存**任何 jar 或历史归档：完整版本索引见 [`RELEASES.md`](RELEASES.md)。

> **1.0.0 / 1.0.1 已撤回**：这两个版本的 `LICENSE` 把上游版权人写成了仓库账号名（实际为 `Copyright (c) 2026 Kirin0321 (麒麟)`）。为免分发署名错误的产物，它们的 jar、归档与 tag 已从本仓库移除，请使用最新的 **1.1.0**。

依赖：Fabric Loader ≥ 0.20.1、Fabric API、Java ≥ 25；Mod Menu 可选（装了就有设置入口）。
配置：`config/ctt-hud-fork.json`；旧的 `ctt-enemy-hud.json` 会自动复制迁移。

## 截图

> 截图来自 **1.20.x 测试版**并已压缩（原图在本地归档里）。1.0.0 改过部分文案
> （「移动面板」→「调整位置…」、「内容过滤」→「显示内容过滤…」、「实体血条设置」→「血条设置」），
> 布局与观感一致，按钮名字以外没有差别。

| 属性面板 | 调整位置 | 盾牌指示器 |
| --- | --- | --- |
| ![属性面板](docs/screenshots/panel.png) | ![调整位置](docs/screenshots/layout.png) | ![盾牌指示器](docs/screenshots/shield.png) |

## 安装

1. 客户端：Fabric Loader ≥ 0.20.1 + Fabric API + 本 jar，丢进 `.minecraft/mods`；
2. 服务端（可选）：同一个 jar 放进 `mods` 即可启用按 UUID 的权威血量推送；不装也完全可用；
3. 游戏内 `F8` 打开设置（Mod Menu 里也有入口）；
4. 配置：`config/ctt-hud-fork.json`（1.20.6 起；旧的 `ctt-enemy-hud.json` 会自动复制过来）。

## 从源码构建

```bash
cd mod
./gradlew build        # 需要 JDK 25；会自动跑 selfCheck（4786 条断言）
# 产物：mod/build/libs/ctt-hud-fork-1.2.10.jar（本地构建用；正式包见 Releases）
```

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `mod/` | 源码工程（Gradle + Loom，Minecraft 26.3 / Fabric） |
| `RELEASES.md` | 正式版清单（版本 / 大小 / SHA-256 / Release 链接） |
| `tools/archive_audit.py` | 归档审计：维护者本地校验 jar 与生成清单（`versions/` 不入库） |
| `LICENSE` / `NOTICE.md` | MIT 许可（含上游 Fork 声明）/ 来源与第三方说明 |

## 发布规则

- **仓库里不放 jar**：正式版的 jar / sources / RELEASE 都作为 **GitHub Release 附件**发布；
- 每个版本都要有归档，**不覆盖、不删除**（维护者本地 `versions/vX.Y.Z/`，已 `.gitignore` 排除）；
- 测试版（1.10.0 – 1.20.42）只在本地 `versions/pre-release/`，不公开；
- 维护者发版后运行 `python tools/archive_audit.py` 校验并刷新 `RELEASES.md`。

## 主要文档

- 功能与配置详解：[`mod/README.md`](mod/README.md)（代码导览、配置字段表、界面文案 ↔ 字段对照）
- 版本沿革：[`CHANGELOG.md`](CHANGELOG.md)
- 最新版说明：[`RELEASES.md`](RELEASES.md) 或 [Release v1.2.10](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.10)
- 发布清单：[`RELEASES.md`](RELEASES.md)
- 版本规则：[`VERSIONING.md`](VERSIONING.md)

## 许可

MIT（见 [`LICENSE`](LICENSE)）。本仓库是 Cake-Hud 的 Fork，原版权声明保留；
不打包任何地图 / 资源包 / 原版纹理，来源说明见 [`NOTICE.md`](NOTICE.md)。
