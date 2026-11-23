# 版本规则

**每个发布过的版本都必须留档，不覆盖、不删除。** 本仓库采用「仓库只放源码文档、产物走 Release」的方式：

| 内容 | 位置 | 是否公开 |
| --- | --- | --- |
| 源码 / 文档 / 构建脚本 | 仓库（`mod/`、README、CHANGELOG…） | ✅ 公开 |
| 正式版 jar / sources / RELEASE.md | **GitHub Release 附件**（每个 tag 一个 Release） | ✅ 公开 |
| 归档目录 `versions/vX.Y.Z/` | 维护者本地，`.gitignore` 排除 | ❌ 不上传 |
| 测试版（1.10.0 – 1.20.42） | 维护者本地 `versions/pre-release/` | ❌ 不公开 |

## 编号

- `v1.0.0` 起是**正式版**线，版本号重新从 1.0.0 起算；
- `1.10.0 – 1.20.42` 是之前的发布前工作版本线（只记录在 `CHANGELOG.md`，产物不公开）；
- 更早的「1.0.0 – 1.9.0」未归档、已回退，与现在的正式版 1.0.0 同名但不是一回事；
- 排序按**发布时间**，不按版本号大小。

## 例外（2026-11-23）

`1.0.0` / `1.0.1` 因 LICENSE 的上游版权署名错误（应为 `Copyright (c) 2026 Kirin0321 (麒麟)`）
已**撤回**：jar、归档与 tag 均从仓库移除，不再分发。这是唯一一次「撤回」而非「保留」。

## 发布流程

1. 改 `mod/gradle.properties` 的 `mod_version`；
2. `./gradlew clean build`（会自动跑 `selfCheck`）；
3. 本地建归档目录 `versions/vX.Y.Z/`：jar + sources + README 快照 + `RELEASE.md`；
4. 更新 `CHANGELOG.md`，并用 `python tools/archive_audit.py` 刷新 `RELEASES.md`；
5. `git add -A && git commit -m "X.Y.Z: …" && git tag vX.Y.Z && git push origin main vX.Y.Z`；
6. 在 GitHub 上为 `vX.Y.Z` 建 **Release**：正文粘 `versions/vX.Y.Z/RELEASE.md`，
   附件上传 `ctt-hud-fork-X.Y.Z.jar` 与 `-sources.jar`；
7. 本地 `versions/` 目录保留（不进仓库）。

## 硬性要求

- **不覆盖**：新版本永远写到新的目录 / 新的 Release，旧的一个字节都不动；
- **不删除**：即使某版本被判定回退，也保留其归档；
- **jar 与源码成对保留**：Release 附件里有 jar 就有 sources；
- **工作副本不带版本号**：避免「改名即丢历史」。
