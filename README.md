# CTT HUD - Fork

> Cake Team Towers 的客户端 HUD（Fork 自 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud)，MIT）：
> 敌人 / 队友头顶血条（原版 bossbar 纹理）、队友心数、以及纯客户端的**自身属性面板**
> （`/trigger ViewStats`，88 个属性逐项开关，分「属性 / 状态效果 / 其它」三节）。
>
> **客户端必装、服务端选装**：服务端只装 Fabric API 也能用完整功能。
>
> **1.20.37 起彻底改名**：仓库 `CTT HUD - Fork`、工程 `mod/`、包名 `com.nyalium.ctthudfork`、
> 产物 `ctt-hud-fork-x.y.z.jar`、翻译键前缀 `ctt_hud_fork.*`。旧配置 `ctt-enemy-hud.json` 仍会自动迁移。

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `mod/` | 源码工程（Gradle + Loom，Minecraft 26.3 / Fabric） |
| `versions/vX.Y.Z/` | **正式版的归档**（当前只有 v1.0.0 / v1.0.1）：jar、sources、README、RELEASE |
| `versions/pre-release/` | **发布前测试版的归档**（1.10.0 – 1.20.42，共 67 个）：同一套内容；清单见 `versions/pre-release/INDEX.md` |
| `versions/ARCHIVE.md` | 全部版本的清单（大小 / SHA-256 / 内嵌版本号 / git 标签） |
| `versions/VERSIONING.md` | 版本保留规则（每版必留档，不覆盖、不删除） |
| `versions/tools/archive_audit.py` | 归档审计：重新生成 `ARCHIVE.md` 并校验 |
| `ctt-hud-fork-1.0.1.jar` | 最新版 jar（**正式版**；与 `versions/v1.0.1/` 中的字节一致） |

## 安装

1. 客户端：Fabric Loader ≥ 0.20.1 + Fabric API + 本 jar，丢进 `.minecraft/mods`；
2. 服务端（可选）：同一个 jar 放进 `mods` 即可启用按 UUID 的权威血量推送；不装也完全可用；
3. 游戏内 `F8` 打开设置（Mod Menu 里也有入口）；
4. 配置：`config/ctt-hud-fork.json`（1.20.6 起；旧的 `ctt-enemy-hud.json` 会自动复制过来）。

## 归档规则

- 每个版本都必须有归档目录，**不覆盖、不删除**：正式版在 `versions/v1.0.x/`，测试版在 `versions/pre-release/vX.Y.Z/`（清单 `versions/pre-release/INDEX.md`）；
- 归档内容：jar + sources + README + RELEASE（+ 实机截图）；
- 每次发版后运行 `python versions/tools/archive_audit.py`，刷新 `versions/ARCHIVE.md`；
- 根目录只保留**最新一版**的 jar，历史 jar 全部在 `versions/` 里。

## 主要文档

- 功能与配置：`mod/README.md`
- 版本沿革：`CHANGELOG.md`
- 最新版说明：`versions/v1.0.1/RELEASE.md`
