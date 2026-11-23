# 来源与致谢（NOTICE）

本仓库是 **Cake-Hud**（<https://github.com/TheHolyLord/Cake-Hud>）的 Fork。
上游采用 **MIT** 许可，其 `LICENSE` 的版权声明为
`Copyright (c) 2026 Kirin0321 (麒麟)`；本仓库按 MIT 要求保留该声明（见 [`LICENSE`](LICENSE)）。

## 哪些东西来自上游 / 地图

| 内容 | 来源 | 说明 |
| --- | --- | --- |
| 「名字上方画血条 + 心数」的整体思路、`BossBarTracker` / `StatsData` 那套做法 | Cake-Hud（MIT，© Kirin0321 / 麒麟） | 本仓库重写过一遍，但设计沿袭上游 |
| `/trigger ViewStats` 属性能问到自己 | 地图 `CakeTeamPack`（`cake_team_tower:misc/view_stats.mcfunction`） | 本 Mod 只读取客户端能看到的输出，不分发地图文件 |
| 原版 `boss_bar` 纹理（`*_background` / `*_progress`） | Minecraft（Mojang） | 运行时引用，不打包、不重分发 |
| `ctt_lang` 的中文属性名 | 地图资源包 | 运行时解析，不打包 |

## 不包含的内容

- **不打包任何地图文件、资源包、纹理**：`assets/` 下只有本 Mod 自己的图标与盾牌计量条贴图；
- 归档里的截图是**本 Mod 自己的界面**（含地图 UI 背景），仅作证据，不随 Mod 分发地图内容；
- 与 Mojang / Microsoft / Cake Team Towers 地图作者**无从属关系**，Minecraft 相关名称与素材归其各自所有者。

## 第三方库

无运行时第三方依赖（Fabric API 由玩家自行安装）。构建期依赖见 `mod/build.gradle`：
Fabric Loom、Fabric Loader、Fabric API、Mod Menu（`compileOnly` / `runtimeOnly`）。
