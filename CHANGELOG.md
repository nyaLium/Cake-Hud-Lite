# CHANGELOG · CTT HUD - Fork

> 这是 [Cake-Hud](https://github.com/TheHolyLord/Cake-Hud)（作者 TheHolyLord，MIT）的 Fork 版。
> 版本规则见 [`VERSIONING.md`](VERSIONING.md)：**每个版本都必须留档，不覆盖、不删除。**
> 公开版本清单（版本 / 大小 / SHA-256 / Release 链接）见 **[`RELEASES.md`](RELEASES.md)**，
> 由 `python tools/archive_audit.py` 生成并校验（它审计维护者本地的 `versions/`）。
> **编号说明**：`v1.0.0` 是**正式版**，版本号从这里重新起算；`1.10.0 – 1.20.42` 是发布前的工作版本线，
> 它们的归档**只在本地** `versions/pre-release/`，不随仓库上传（下表里标「本地（未上传）」）。

## 版本索引

| 版本 | 功能集 | 归档 |
| --- | --- | --- |
| **1.3.6**（本地调整，未发布） | 配置迁移改**表驱动**（RULES / FLAG_RULES，按版本升序）；规则一条不少（含已发布 v1.0.0 / v1.1.0 / v1.0.3 时代的 v22 / v23），行为与旧 if 链逐条对齐 |
| **1.3.5**（本地调整，未发布） | 取 bossbar 统一走 BossBarTracker.bossBars()（消掉 4 处重复反射）；BossBarKeyDetector 也改用它；删掉退化的 HudTarget.hidesBelowNameScore 包装 |
| **1.3.4**（本地调整，未发布） | 合并 `HudWorldRenderer.options()/teamOptions()` 为 `options(cfg, teammate)`（`playerStyle` 一并收进去）；**配置项一个没动**（字段/默认值/范围/界面/文件版本全部不变） | 探测逻辑合并：诅咒/癫狂共用 `BossBarKeyDetector`（各持实例，效果仍分开）；`SelfStatsPanel` 的两处条件按状态拆成显式判断；删掉死代码 `HudTarget.visibleCore` | 魂心归入「属性 - 心型」；队伍条名册本来就含自己 —— `resolveTeam` 不再排除自己，服务端 `collect` 也不再跳过自己，调试模式的「自己」与队友完全同一条取数路径 | 过滤界面「属性」细分为 属性 / 属性 - 心型 / 属性 - 倍率 / 属性 - 护甲 / 属性 - 杂项；调试模式的「自己」改用个人状态条取数（与队友同一套逻辑） | 属性面板过滤按**新的数据包**重新对照：分类改为优先用图标上的 hover 键 `view_stats_N`；「乘算前」原始值单独成类（新增 8 项，可过滤项 88 → **96**）；修正 `Misc_View_Stats_101/104/107/115/122/124` 等倍率键；名称对齐数据包；设置页新增「调试模式」 | 癫狂改为**只隐藏自身属性面板**（数据包查证：癫狂不隐藏最近敌人条 / 队伍血量条 / below_name，实体血条与信息照常）；修「魂心溢出时血条瞬间变满」（队友百分比读不到时给 5 s 粘性缓存）；「当前/上限」前加 ❤ | [Release v1.2.10](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.10) |
| **1.2.9** | 癫狂期间也摘原版 `below_name` 分数：1.2.6 起癫狂分支绕过 `managed()`，导致本 Mod 的 `❤ 百分比` 与原版数字重复、名字被抬高 9 px；新增 `HudTarget.hidesBelowNameScore` 统一判定 | [Release v1.2.9](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.9) |
| **1.2.8** | 百分比文本前加红心：`❤ 73%`（`percentOnly` 分支统一处理，癫狂 / 队友回退 / 敌人回退都生效） | [Release v1.2.8](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.8) |
| **1.2.7** | 癫狂下补上百分比**文本**：只关心数/护甲，`showNumbers` 保留 → `HudLayout` 走 percentOnly 分支写 `73%`，配上百分比条（1.2.6 只有条没有字） | [Release v1.2.7](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.7) |
| **1.2.6** | 癫狂下改画**百分比血条**：地图不清 `below_name` 的 `HealthPercent`，所以只画条、不写数字/心数/护甲（`EnemyData.percent` 新工厂）；未知诅咒仍整块隐藏（地图 `setdisplay below_name` 清空，百分比本就不在） | [Release v1.2.6](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.6) |
| **1.2.5** | 癫狂（Berserk）改用**专属 Boss 栏**识别：数据包在玩家带 `Berserk` 标签时把个人状态条换成 `Misc_Bossbars_P10_Bossbar_11`（p1..p10 共用）；不再依赖聊天消息（带癫狂进服也能识别） | [Release v1.2.5](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.5) |
| **1.2.4** | 「未知诅咒」现在也隐藏属性面板（HUD 与容器两处）；新增 `BerserkDetector`：癫狂期间隐藏玩家/敌人的头顶血条与心数（靠 `Items_status_effect21_berserk_1/_3` 聊天键识别，无开关），属性面板不受影响 | [Release v1.2.4](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.4) |
| **1.2.3** | 属性名统一（盔甲 → 防御、真•盔甲 → 真·防御）；玩家心数改两行排版（上排 魂/黑/蓝，下排 红 当前/上限 + 碎/粉，不再轮换）；心数统一缩写（玩家也缩）；缩写去掉「整数位 ≥100 不带小数」，999999 → 999.9K；`mod/README.md` 新增「隐藏特征」一节 | [Release v1.2.3](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.3) |
| **1.2.2** | 六类心统一 `❤` + 颜色（与地图 / 属性面板一致）；真盔甲不再参与护甲翻页、常驻显示；纯客户端队友绿条改用 `below_name` 的 `HealthPercent` 填进度（文字不变）；玩家头顶心数不缩写；缩写阈值 100000 → 1000、保留一位小数截断；血条通用透明度改为 10%~100%、默认 50%（config v23） | [Release v1.2.2](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.2) |
| **1.2.1** | 修纯客户端（服务端未装本 Mod）时队友血量的两个问题：①「NIVEI / NIVEIV」这类前缀名会互相认领（`NIVEI` 的条目挂到 `NIVEIV` 头顶）；②蓝心 / 魂心让 AllHearts 超过 MaxHP 时，头顶数字被夹成「MaxHP/MaxHP」，与 Boss 栏对不上。现在完整玩家名只做精确匹配、宽松匹配要求唯一候选，且显示值不再夹上限 | [Release v1.2.1](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.1) |
| **1.2.0** | 「调整位置…」（盾牌）界面新增「初始位置是否跟随副手变化」（`shieldAutoMirror`，默认开，悬停有说明，在「回到初始位置」左侧）：关掉后初始位置固定按右手（副手在左），切换主手不再镜像；配置 schema 不变（新字段默认值 = 旧行为） | [Release v1.2.0](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.2.0) |
| **1.1.0** | 盾牌指示器纹理换成新的 34×86 四档（`meter_0..3`）；Mod 简介改为「为地图“蛋糕塔”（Cake Team Towers）制作」的三条卖点；版本号 1.1.0（语义化：新内容，非修 bug） | [Release v1.1.0](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.1.0) |
| **1.0.3** | `fabric.mod.json` 增加 `contact`（homepage / sources / issues 指向 [nyaLium/CTT-HUD-Mod-Fork-Version](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version)），Mod Menu 的模组页会显示这三个链接；无代码改动 | [Release v1.0.3](https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version/releases/tag/v1.0.3) |
| **1.0.2** | 修正 LICENSE 里的上游版权署名：上游 Cake-Hud 的 LICENSE 写的是 `Copyright (c) 2026 Kirin0321 (麒麟)`（不是仓库账号名），根 `LICENSE` / `NOTICE.md` / `mod/LICENSE`（内嵌进 jar）与 `fabric.mod.json` 的 authors 一并更正；无代码改动 | 本地归档（未单独发布） |
| ~~1.0.1~~ | 注释与文档岗位（无行为变化）—— **已撤回**（许可署名错误），归档从仓库移除 | — |
| ~~1.0.0（正式版）~~ | 界面文案统一、删除面板「文字大小」与血条「垂直偏移」、显示方式默认 HUD + 容器界面、盾牌缩放步进 0.05、配置 v22、清理死代码 —— **已撤回**（许可署名错误），归档从仓库移除 | — |
| **1.20.42** | **容器界面**那两套面板的默认位置也改成 x=4、y = 屏幕高 ÷ 3（打开箱子/背包时同样在偏上 1/3）；「回到初始位置」四套都回动态默认；配置 v21（只有仍停在旧默认 20 的才迁移） | 本地（未上传） |
| **1.20.41** | 属性面板**默认位置**改成 x=4、y = 屏幕高 ÷ 3（左侧偏上，随窗口大小自动保持比例）；「回到初始位置」写回这个动态默认；配置 v20（只有仍停在旧默认 40 的那两套才迁移） | 本地（未上传） |
| **1.20.40** | 属性面板 / 盾牌指示器的位置改成**相对位置**：坐标连同「量它的那套 GUI 尺寸」（`layoutGuiW/H`）一起存，窗口大小或 GUI 缩放变化后按比例换算，不再错位；配置 v19 | 本地（未上传） |
| **1.20.39** | 回退 1.20.38 的「迟到的输出也算我们的」认领机制（会把玩家主动输入的 ViewStats 一并吞掉）；改成**进存档 / 进服后静默 3 s** 再自动问 ViewStats（这之前不发 trigger、这段时间里来的 ViewStats 输出也不显示） | 本地（未上传） |
| 1.20.38 | 进存档那一刻漏进聊天栏的 ViewStats 属性行：采集 3 s 超时收尾后，自己 trigger 的迟到输出仍按「我们那趟」吞掉（20 s 认领窗口 + 欠账计数，标题丢了也认）—— **已回退**（会误吞玩家手动触发的输出） | 本地（未上传） |
| **1.20.37** | 回退 1.20.36 的「选过职业」门控（行为同 1.20.35）；**项目与产物彻底改名 CTT HUD - Fork**（目录、包名、类名、产物名、翻译键、Mixin 前缀） | 本地（未上传） |
| 1.20.36 | 准备大厅（还没选职业）不再先闪面板：`visible()` 加「选过职业」门控 —— `ClassPassive`（客户端读得到时）→ 职业被动道具 → ViewStats「职业点」行依次回退（**已回退**） | 本地（未上传） |
| 1.20.35 | 「优先使用服务端数据」默认保持开启且不再可改：界面去掉开关、`useServerData` 字段删除（配置 v18），代码写死优先使用服务端组件数据 | 本地（未上传） |
| 1.20.34 | 敌人 / 玩家各一个「显示血条」整块开关（关掉后血条 + 上方文字都消失，只剩原版名字）；删掉 `teamShowBar`（配置 v17）；滚动提示不再写「滚动条可点可拖」 | 本地（未上传） |
| 1.20.33 | 所有滚动页的滚动条都能点/拖（命中 ±5 px、悬停提亮）；队友血条独立成 `teamShowBar`；实体设置页滑块重建不回弹；「回到初始位置」按对象取默认 y；配置 v16 | 本地（未上传） |
| 1.20.32 | 游戏时间 / 难度骷髅移入「其它」；滚动条可点可拖（三个滚动页）；盾牌不透明度默认 65%；属性面板文字阴影独立、实体血条「文字阴影」默认关；配置 v15 | 本地（未上传） |
| 1.20.31 | 游戏时间那行真正上面板（以前只当收尾信号）；内容过滤分「属性 58 / 状态效果 28 / 其它 2」三节、共 88 项；草方块 `□` 不再被整行丢掉；配置 v14 | 本地（未上传） |
| 1.20.30 | `F8` 从「杂项」挪到独立按键分类「CTT HUD - Fork」（绑定本来就有，只是不好找） | 本地（未上传） |
| 1.20.29 | 两个拖动界面把「按住拖动 / 方向键微调（Shift 16 px）」写进界面提示 | 本地（未上传） |
| 1.20.28 | 拖动界面改成「必须真拖」：只在面板上按下、位移 ≥2 px 才写位置，点一下不再挪动 | 本地（未上传） |
| 1.20.27 | 两个「移动面板 / 移动指示器」界面关掉背景模糊，方便对着真实画面拖位置 | 本地（未上传） |
| 1.20.26 | 盾牌换 34×86 新纹理；默认缩放 0.7 / 不透明度 75%；默认位置统一（预览不再镜像到右侧）；mod 图标换成 pack.png | 本地（未上传） |
| 1.20.25 | 代码审计落地：删 9 处死代码、移除 `hideBehindEntities`（配置 v12）、补「内部常量一览」+「设置界面」现状表 + 版本沿革 15 行、修 8 处文档/代码不一致 | 本地（未上传） |
| 1.20.24 | 手动 `/trigger ViewStats` 不再被吞（对齐上游 `hideCapture`）+ 计时行当收尾；删掉「回到初始位置」那句提示；内容过滤列表多一行 | 本地（未上传） |
| 1.20.23 | 拖动改上游式绝对定位（+方向键微调、就地切对象）；「重置位置」→「回到初始位置」；聊天不再被误吞；游戏时间冻住不显示面板 | 本地（未上传） |
| 1.20.22 | 盾牌缩放改「0.5~1.5 倍」且真缩放（修掉平铺）；滚动列表改真裁剪，最后一行不再越过屏幕 | 本地（未上传） |
| 1.20.21 | 盾牌尺寸改为「倍」+整数倍（不拉伸）；滚动边界统一；模式 2 拖动标题写明 HUD/容器 | 本地（未上传） |
| 1.20.20 | 属性面板设置页滚动+滚动条；实体页修正滚动上下限+滚动条；盾牌指示器拖动定位+不透明度+新纹理 | 本地（未上传） |
| 1.20.19 | 开关联动禁用；显示方式更名排序；总界面重排 + 实体血条合并页（可滚动）；盾牌指示器整合 | 本地（未上传） |
| 1.20.18 | 模式 2 拆成 HUD / 容器两套独立配置（`both*` + `bothContainer*`，共 4 套）；设置里模式 2 多一行「编辑对象」 | 本地（未上传） |
| 1.20.17 | 三种显示方式各自独立一套位置/列数/字号/不透明度（新增 `both*`） | 本地（未上传） |
| 1.20.16 | 过滤界面的属性名改为地图官方中文（资源包 `ctt_lang`）；显示方式新增「HUD + 容器界面」 | 本地（未上传） |
| 1.20.15 | 自动开队伍条写死开启、墙/实体遮挡写死关闭（删除界面行）；过滤界面属性名改用 ViewStats 官方 hover 文本；容器面板悬停显示官方属性名 | 本地（未上传） |
| 1.20.14 | 「被墙挡住时隐藏」默认关且不再提供开关；面板字号最小 1.00；新增显示方式「始终显示 / 在容器界面显示」（两套独立配置、点击即时切换）；关闭面板后其余项不可交互；服务器强制 3 s 时刷新滑条禁用 | 本地（未上传） |
| 1.20.13 | 属性面板刷新默认 3 s / 1~5 s / 最短「立即」/ 他人服务器强制 3 s（不改配置）/ 悬停提示；HUD 全亮光照；`K/M/B` 大写；面板行去重 | 本地（未上传） |
| 1.20.12 | 上游 `ctt-health-display` 共存提示（1.20.13 按「基于 1.20.11」撤掉） | 本地（未上传） |
| 1.20.11 | 没开局时面板不再挂上一局数据（触发被拒立即清空，冷却 60 s→10 s；连续 2 趟空采集也清）；敌人设置页改成两列各 3 行 | 本地（未上传） |
| 1.20.10 | bossbar 认领在同一血量档里优先**离玩家最近**的同名怪，粘性只保留 2 格（超过就换）；依据是地图 `targetting_enemy` 本身「取离某名 CTT 玩家最近的 E」 | 本地（未上传） |
| 1.20.9 | 面板位置只留拖动（删 X/Y 滑条）；过滤页双列按钮固定 200（原版宽度）并居中；bossbar 认领加 5 个百分点硬门槛（同名怪不再粘错）；血条补透视副本 | 本地（未上传） |
| 1.20.8 | 修大厅里那句「无法触发该计分项」：开局前不再发 `/trigger ViewStats`（用 `HealthPercent` / 本人 bossbar 判开局），失败回显被吞并冷却 60 s，回大厅清空面板 | 本地（未上传） |
| 1.20.7 | 纯客户端（只有 `HealthPercent`）时敌人也显示**百分比数字**（条 + `40%`），不再只有条没有字；队友百分比回退同理 | 本地（未上传） |
| 1.20.6 | 过滤界面换成双列带边框按钮 + 滚轮翻行；Mod 改名 **CTT HUD - Fork**，配置文件改为 `ctt-hud-fork.json`（旧文件自动复制迁移）；钓鱼/业力/塔内回血/蜂蚁点按**码位**精确区分 | 本地（未上传） |
| 1.20.5 | 刷新单位写明「现实时间」；删除职业过滤；过滤界面改成可滚动列表、按属性细分到 60 项；配置 v6（旧粗类自动展开） | 本地（未上传） |
| 1.20.4 | 属性面板去提示行（`Misc_View_Stats_1/2` 与未解析键行不再上屏）、双缓冲不再闪；字号/透明度可调；「移动面板」拖拽 + 水平/垂直滑块；内容过滤 15 类；按职业过滤（1.20.5 已删除）；`F8` 打开设置；队友 `below_name = HealthPercent` 百分比回退；配置解析失败不再静默重置 | 本地（未上传） |
| 1.20.3 | 新增**自身属性面板**（纯客户端：定时 `/trigger ViewStats`，抓地图回的那几十行属性、吞掉聊天刷屏、按列画成面板）；顺带解析四色心/碎粉心 | 本地（未上传） |
| 1.20.2 | 血条**两端端盖按纹素原尺寸分段绘制**（原版圆头，不再是长方形）；竖向 UV 回 0~1，保住上下深色描边 | 本地（未上传） |
| 1.20.1 | 血条改回**原版 BossBar 画法**（整条背景 + 左侧裁切进度），修掉未满时中间那条 1 px 接缝 | 本地（未上传） |
| 1.20.0 | 心类轮换：敌人红心 + 魂/黑/蓝/碎/粉（每次一种），玩家红心 + 魂↔黑；碎心/蓝心固定在玩家最上面一行；护甲与元素护甲之间改两个空格 | 本地（未上传） |
| 1.19.0 | 文字恢复透视副本（SEE_THROUGH 第二遍）；默认缩放 0.9；碎心/粉心/蓝心挪到最上面一行 | 本地（未上传） |
| 1.18.0 | 布局固定三段（护甲最上 / 心数贴条 / 血条 / 名称）；缩放与不透明度对文字生效；血条默认 75 | 本地（未上传） |
| 1.17.0 | 护甲：🛡 常驻 + 元素护甲 2 项/页循环（2 s）；大数值 k/m/b 缩写；着火时 HUD 沿视线往相机推 | 本地（未上传） |
| 1.16.3 | 队友只画心与血条，护甲段不再显示（`teamOptions.showArmor = false`）；敌人照旧 | 本地（未上传） |
| 1.16.2 | 死亡 / 观战（旁观模式）的玩家不再显示头顶 HUD：客户端 `isSpectator()` 不画、服务端不进 payload | 本地（未上传） |
| 1.16.1 | 队友绿条进度固定为 **红心 ÷ MaxHP**（不再按当前心类分层）；文字行距 9 → **10**，修掉两行之间那条叠加暗带 | 本地（未上传） |
| 1.16.0 | 队友（玩家）也能显示四色心 / 碎心 / 粉心：服务端把附近玩家一起推，客户端优先用 payload | 本地（未上传） |
| 1.15.1 | 修血条边缘竖条纹闪烁：两段不再共面重叠 + UV 内收半纹素 | 本地（未上传） |
| 1.15.0 | 渲染改为世界空间：与原版名字同一个 `submitNameTag` 提交点，深度测试 / 随距离缩放全部对齐；删除 GUI 元素 | 本地（未上传） |
| 1.14.5 | 血条改画在动作栏/标题/聊天之下；新增「被其它实体挡住时隐藏」（屏幕矩形覆盖率 ≥70% 就不画） | 本地（未上传） |
| 1.14.4 | 新增「换行阈值」`wrapWidth`（40~200，最右=自动=不超过血条长度），折行阈值可调 | 本地（未上传） |
| 1.14.3 | 红心写 `❤当前/上限`；整行超过阈值时折两行：红心在上、其余心+护甲在下 | 本地（未上传） |
| 1.14.2 | 碎心按符号拆两栏：`> 0` 碎心（暗红 💔）/ `< 0` 粉心（亮紫 💗），负值不再被夹成 0 | 本地（未上传） |
| 1.14.1 | 心数行合并：各类心之间只隔一个空格，`\|` 只留在「心组 ↔ 护甲」之间（`❤160 💗2 \| 🛡3 ☃5`） | 本地（未上传） |
| 1.14.0 | 新增服务端选装组件（`ctt-hud-fork:mob_health`，按 UUID 推 `AllHearts/MaxHP/护甲/红魂黑蓝碎`，5 Hz 差量 + 3 s keepalive）；客户端优先使用并在 5 s 失鲜后自动回落；新增 `useServerData` | 本地（未上传） |
| 1.13.5 | 修聊天栏被全拦（ALLOW_GAME 返回值反了）；设置页「返回」按实际行数定位；同名敌人按 `HealthPercent` + 绑定粘性唯一认领 | 本地（未上传） |
| 1.13.4 | 定位「客户端必装、服务端选装」；队友条与敌人条共用同一条原版 bossbar 纹理绘制；恢复自动开队伍条；新增 `ChatTriggerFilter`（吞成功回显与 `commands.trigger.failed*`） | 本地（未上传） |
| 1.13.3 | 总界面一纵排布 + 底部「完成」（取消移除）；移除 `showNumbers` / `showHeartCounts` / `showArmor` 三项显示开关（永久开启）；两条 Boss 栏开关并入通用并改名；修复队伍条「时不时闪一下」（宽松扫描 + Tab 名单匹配） | 本地（未上传） |
| 1.13.2 | 设置界面恢复「完成 / 取消」；总界面四项两列排布，「完成 / 取消」贴屏幕底部；「恢复默认」只改内存不落盘 | 本地（未上传） |
| 1.13.1 | 修复「高塔层数」Boss 条被当成队伍条误隐藏（要求至少一段名字能匹配真实玩家）；设置界面改版为四项入口；玩家页队伍条开关改为「显示与否」；移除自绘名称 | 本地（未上传） |
| 1.13.0 | 心数行（红/魂精确反推）；魂心→黄条、蓝心→蓝条；名称改由本 Mod 自绘（1.13.1 已移除） | 本地（未上传） |
| 1.12.0 | 隐藏名称下方计分板分数；删除百分比文字；玩家血条独立设置（含隐藏队伍条）；血条按心类变色 | 本地（未上传） |
| 1.11.0 | **回补顶部Boss条隐藏、队友血条；百分比可开关且保留护甲；| 分隔；默认透明度 75%；设置分四类** | 本地（未上传） |
| 1.10.0 | 回退到「仅头顶血条」+ 修复血条纹理 | 本地（未上传） |
| 1.9.0 | Mod ID 改为 `ctt-hud-fork`；间隔上限 5000ms；重做队伍条自动开启；面板可调透明度/宽度 | 未归档（已回退） |
| 1.8.0 | 队友血量面板 + 自身属性面板（可拖拽、预设左侧中心）+ 命令间隔闸门 | 未归档（已回退） |
| 1.7.0 | 自动执行 `/trigger TogglePartyBossbar` 打开队伍条（3s 冷却 + 熔断） | 未归档（已回退） |
| 1.6.0 | 队友头顶血条（绿色 bossbar 纹理）+ 未知诅咒门控 | 未归档（已回退） |
| 1.5.0 | 隐藏数据包的「最近敌人 Boss 条」（默认隐藏，可开关） | 未归档（已回退） |
| 1.4.0 | 血条长度/缩放/垂直偏移的区间调整；移除最低最大血量 | 未归档（已回退） |
| 1.3.0 | 纯客户端精确值通道：解析客户端 Boss 栏标题 | 未归档（已回退） |
| 1.2.0 | 敌人判据改为「有 CTT 血量分数」+ Mod Menu 联动 | 未归档（已回退） |
| 1.1.0 | 血条长度可调；选项穷举自检 | 未归档（已回退） |
| 旧 1.0.0 | 首个版本：敌人头顶血条（**与正式版 1.0.0 同名但不是一回事**；未归档、已回退） | 未归档（已回退） |

> 1.0.0 – 1.9.0 采用「同目录改名」的工作方式，源码与 jar 已不可回溯，仅保留功能记录。
> 从 1.10.0 起改为 `versions/vX.Y.Z/` 独立归档。

## 1.2.10

### ① 癫狂口径（数据包查证）

| 项 | 癫狂期间是否被地图隐藏 |
| --- | --- |
| 最近敌人 Boss 栏 `targetting` | 否 |
| 队伍血量 Boss 栏 `spectator_bossbar` | 否（`misc.mcfunction` 每 tick 无条件重设） |
| 玩家头顶 `below_name` 百分比 | 否（只有诅咒会 `setdisplay below_name`） |

所以：**只隐藏自身属性面板**（HUD 与容器），队友/敌人的血条与信息照常显示。
1.2.6–1.2.9 的癫狂专用绘制（百分比条/文本）与 `EnemyData.percent` 工厂已删除。

### ② 魂心溢出「瞬间变满」

队友进度优先 `HealthPercent`；读不到时原来退到 `entry.ratio()`（AllHearts/MaxHP，溢出时恒 1）。
改为 **5 s 粘性缓存**（`PERCENT_STICKY_MS`），读不到时用最近一次已知百分比；进服/断线清空。

### ③ `❤ 当前/上限`

`HudLayout` 的「拆不出各类心」兜底分支加红心前缀，与百分比/心数行统一。

### ④ 自检

`checks=4883 failures=0`。

### ⑤ 未验证

没有实机复现闪满与癫狂新口径。

## 1.2.9

### ① 原因

1.2.6 起癫狂分支用 `managedCore` / `visibleCore` 绕过 `managed()`，`NameTagScoreMixin` 的
「隐藏 below_name 分数」也随之失效 → 头顶同时有本 Mod 的 `❤ 76%` 和原版 `76`，
名字还被原版上抬 9 px，挤在文字与血条之间。

### ② 修法

`HudTarget.hidesBelowNameScore(mc, entity)`：诅咒 false；癫狂 `managedCore`；平时 `managed`。
`NameTagScoreMixin` 改用它。

### ③ 自检

`checks=4875 failures=0`。

### ④ 未验证

没有实机再进癫狂复核。

## 1.2.8

### ① 百分比文本带红心

`HudLayout` 的 `percentOnly` 分支改成三段：`❤`（`#FF5555`）+ 空格 + 百分比（白）→ **`❤ 73%`**。
覆盖：癫狂下的百分比显示、队友无队伍条的百分比回退、敌人无 Boss 栏的百分比。

### ② 自检

`checks=4875 failures=0`（percentOnly 系列与队友回退的期望改为 `❤ NN%`）。

### ③ 未验证

没有实机看间距。

## 1.2.7

### ① 癫狂排版：百分比文本 + 条

`HudWorldRenderer` 癫狂分支只关 `showHearts` / `showArmor`，不再关 `showNumbers`：

```java
options.showHearts = false;
options.showArmor  = false;
```

`HudLayout` 因此走 `percentOnly` 分支，把 `73%` 写在血条上方，条按 `HealthPercent` 填充。

### ② 自检

`checks=4859 failures=0`（新增：`percentOnly` + 关心数 → 文本为 `73%` 且仍有条宽）。

### ③ 未验证

没有实机进癫狂看排版。

## 1.2.6

### ① 查证

| 状态 | 地图对 `below_name` 百分比的处理 | 结论 |
| --- | --- | --- |
| 癫狂（Berserk） | **不动**：`status_effect21_berserk` 只加分数/发消息，没有 `setdisplay` | 百分比仍可见 |
| 未知诅咒 | `d_curse02_unknown.mcfunction` 第 9-11 行：`scoreboard objectives setdisplay below_name` / `list` / `sidebar`（不带 objective = 清空） | 百分比消失 |

### ② 癫狂改为画百分比条

`HudWorldRenderer.submit` 里癫狂分支：

- 用 `managedCore` / `visibleCore` 判定实体（绕过癫狂闸门）；
- `EnemyData.healthPercentOf` 取百分比 → `EnemyData.percent(p)`；
- `showNumbers/showHearts/showArmor = false` → 只画条；
- 不接管名字、不摘原版分数；取不到百分比（< 0）不画。

### ③ `EnemyData.percent(int)`

`percent(73)` → `Snapshot(hp=73, maxHp=100, percentOnly=true, percent=73, ratio=0.73)`；>100 夹 100，<0 夹 0。

### ④ 自检

`checks=4857 failures=0`。

### ⑤ 未验证

没有实机进癫狂看百分比条。

## 1.2.5

### ① 数据包里的专属 Boss 栏

`misc/bossbars/p1..p10_bossbar.mcfunction` 第 7 行：

```mcfunction
execute as @a[scores={BossbarID=10NN},tag=Berserk] run bossbar set minecraft:player_10NN name {"translate":"Misc_Bossbars_P10_Bossbar_11","italic":true,"color":"dark_red"}
```

十个槽位共用翻译键 `Misc_Bossbars_P10_Bossbar_11`（文本「我只有一个想法，战…………」），
`value` 同时被设成 `100000`。

### ② 识别方式

`BerserkDetector` 改为探测 Boss 栏组件树里的这个翻译键（100 ms 节流、500 ms 释放），
不再看聊天消息；`JOIN` / `DISCONNECT` 仍 reset。

行为不变：癫狂隐藏玩家/敌人的头顶血条与心数，**属性面板不受影响**。

### ③ 自检

`checks=4851 failures=0`。

### ④ 未验证

没有实机触发癫狂。

## 1.2.4

### ① 未知诅咒也隐藏属性面板

之前只挡实体血条；现在 `SelfStatsPanel` 的两条绘制路径（HUD / 容器）都加了
`CurseDetector.isActive(mc)` 判断。诅咒识别方式不变（bossbar 翻译键前缀 + 100 ms 探测 + 2 s 释放）。

### ② 癫狂（Berserk）只挡实体血条

新增 `BerserkDetector`：聊天里出现 `Items_status_effect21_berserk_1` 置为癫狂、
`Items_status_effect21_berserk_3` 清除；进服 / 断线 reset。
癫狂期间 `HudTarget.managed()` 返回 false（玩家与敌人的血条 / 心数 / 护甲都不画，名字交还原版），
`HudWorldRenderer` 兜底再挡一次；**属性面板不受影响**。

> 只能靠聊天：标签/分数不进客户端，地图也没给原版状态效果。
> 局限：带癫狂进服会收不到开场消息。

### ③ 自检

`checks=4850 failures=0`。

### ④ 未验证

没有实机触发癫狂 / 诅咒。

## 1.2.3

### ① 命名

| key | 旧 | 新 |
| --- | --- | --- |
| `config.kind.defence` | 盔甲 | 防御 |
| `config.kind.true_armor` | 真•盔甲 | 真·防御 |
| `config.kind.true_armor_amp` | 真•盔甲倍率 | 真·防御倍率 |
| en_us | Armour / True armour / True armour multiplier | Defence / True defence / True defence multiplier |

### ② 玩家心数排版

```
魂心N 黑心N 蓝心N
红心N/MaxN 碎心（粉心）N
```

上排 魂→黑→蓝；下排红心在前、碎/粉在后；玩家不再轮换（敌人不变）。

### ③ 心数统一缩写

玩家心数也走 K/M/B（`< 1000` 原样）。

### ④ 999 999 → 999.9K

1.2.2 的「整数位 ≥ 100 不带小数」是代码行为（当时为控行宽），不是笔误。
1.2.3 去掉该规则：999999 → `999.9K`、999999999 → `999.9M`，整数位仍 ≥ 1。

### ⑤ 文档与注释

`mod/README.md` 新增「隐藏特征（写死在代码里的常量与行为）」五组表格；
`HudWorldRenderer.CACHE_NS`、`BossBarTracker.TEAM_CACHE_MS` 等补上单位注释，
`HeartCycle.PLAYER_ORDER` 标注为不再使用。

### ⑥ 自检

`checks=4846 failures=0`。

### ⑦ 未验证

没有实机看两行排版；数值部分由自检覆盖。

## 1.2.2

### ① 心形统一：六类心都用 `❤` + 颜色

地图 `view_stats.mcfunction` 对每一类心都是 `{text:"❤", color:...}`（同一个字形换颜色），
而 HUD 之前红心用 `❤`、其它用 💛🖤💙💔💗 —— 字体里是另一套 emoji 字形，所以魂心看起来和红心不是一套。

现在 `HeartCounts.ICONS` 六项全部是 `❤`，颜色仍按 `COLORS`（红/黄/紫/蓝/暗红/亮紫）。

### ② 真盔甲（TrueArmor）不再参与护甲翻页

数据包里它是 `ArmorDisplay` 的一页（白 🛡），但语义上属于「护甲」而不是元素抗性：

- `HudLayout.armourPage` / `armourPageCount` 都跳过 `TrueArmor`；
- 它和 `🛡 防御` 一样**常驻**在顶部行（白 🛡，`EnemyData.TRUE_ARMOR_INDEX`）。

### ③ 队友绿条进度改用 `below_name` 的 `HealthPercent`

纯客户端时队友的血条原来是 `AllHearts/MaxHP`（蓝心/魂心会把绿条撑满）。
现在优先用 `below_name` 的 `HealthPercent`（= 红心 ÷ MaxHP，数据包对所有参战玩家都算），
**文字仍按队伍条的当前/上限**显示 —— 条和数是两个口径，各自取更准的那个。

### ④ 数字显示

- **玩家头顶的心数不缩写**（魂/黑/蓝/碎/粉照原样写，例如 `10000` 不会变 `10K`）；
- **缩写规则**：`< 1000` 原样（900 仍是 `900`，不会 `0.9K`）；`≥ 1000` 用 `K/M/B`，
  小数**截断**（不是四舍五入）：24999 → `24.9K`、1100 → `1.1K`、1 100 000 → `1.1M`；
  整数位 ≥ 100 时只留整数（999 999 → `999K`）。阈值 `HudLayout.NUM_EXACT_LIMIT` 从 100000 改成 1000。

### ⑤ 血条通用透明度

| 项 | 旧 | 新 |
| --- | --- | --- |
| 范围 | 0.50 ~ 1.00 | **0.10 ~ 1.00** |
| 默认 | 0.75 | **0.50** |
| 界面 | 50 ~ 100%，1% 一档 | **10 ~ 100%，1% 一档** |
| 配置 | v22 | **v23**（旧配置里仍是 0.75 的迁移成 0.50） |

### ⑥ 自检

`checks=4845 failures=0`（新增：心形统一、真盔甲页数、玩家心数不缩写、缩写新阈值、
透明度下限/默认/迁移、alpha 区间）。

### ⑦ 未验证

没有实机进游戏看新的心形显示与绿条进度；改的是显示口径与常量，自检覆盖了数值部分。

## 1.2.1

修**纯客户端**（服务端未装本 Mod）时，队友头顶血条的读数问题。服务端装了本 Mod 时走 `MobHealthPayload`，不受影响。

### ① 问题一：前缀名互相认领（NIVEI / NIVEIV）

队伍条标题是数据包拼出来的 `名字 (AllHearts/MaxHP)`。原来的认领流程是「先精确匹配、再包含匹配」，
而包含匹配对**前缀关系**没有防护：`NIVEI` 是 `NIVEIV` 的前缀，于是当 `NIVEI` 的条目找不到对应实体
（例如 `NIVEI` 就是本地玩家、被 `candidates` 排除）时，它会落到 `NIVEIV` 身上 —— 队友头顶显示成别人的血量。

修法（`BossBarTracker`）：

- 新增 `knowsPlayerName()`：判断条目里的名字是不是某位真实玩家的**完整名字**（自己 / 视野内 / Tab 名单）；
- `allowLooseMatch(knownPlayerName, looseCandidates)`：只有「不是完整玩家名 **且** 宽松候选恰好 1 个」才允许包含匹配；
- `resolveTeam()` 增加 `used` 集合，一条条目只认领一名玩家，一名玩家也只被一条条目认领；
- `isTeammate()` 用同一套判定，避免把路人误判成队友。

### ② 问题二：蓝心 / 魂心超过上限时被夹掉

`EnemyData.exact()` 原来把显示值 `hp` 夹到 `maxHp`（`min(hp, maxHp)`）。队伍条写的是
`AllHearts/MaxHP`，蓝心 / 魂心可以突破红心上限（`AllHearts > MaxHP`），夹完就变成
「MaxHP/MaxHP」= 红心上限，与 Boss 栏不符。

修法：`hp` 原样保留（只夹到 `>= 0`），**百分比**仍然夹在 0~100；血条进度仍用 `hp/maxHp`（>1 时按满格画）。

### ③ 自检

`checks=4801 failures=0`（新增 9 条：`exact(300,200)` 保留 300、百分比 100、`124/200 → 62%`、
`allowLooseMatch` 的四种组合）。

### ④ 未验证

没有实机复现「蓝心超过上限」的场景；两项修复分别由代码路径与自检覆盖，建议进游戏用蓝心 / 魂心场景复核一次。

## 1.2.0

### ① 新选项：初始位置是否跟随副手变化

| 项 | 值 |
| --- | --- |
| 字段 | `HudConfig.Cfg.shieldAutoMirror`（默认 `true`） |
| 位置 | 「盾牌指示器设置…」→「调整位置…」界面底部，**「回到初始位置」左侧** |
| 悬停提示 | 「只影响未移动的初始位置`\n\n`开启：切换主手后，盾牌指示器会自动镜像处理`\n`关闭：按照主手为右手的情况进行摆放」 |

### ② 默认位置的判定（检查结果）

盾牌指示器的默认位置（`shieldX/Y = -1` 时）由两段组成：

1. **水平**：对准**副手物品格中心**。原版快捷栏 182 px 宽，左侧物品格中心在 `guiW/2 − 109`，
   右侧在 `guiW/2 + 109`；主手决定副手在哪一侧 ——
   `offhandOnLeft = (mainArm == RIGHT)`（即右手玩家副手在左，左手玩家在右）。
2. **垂直**：整条指示器的底边距屏幕底部 76 px（`guiH − 76 − h`）。

所以切换主手（`Options → Skin Customization` 旁的「主手」）时，副手物品格换到另一侧，
默认位置就跟着镜像 —— 这就是你看到的现象。

### ③ 实现

- `ShieldIndicatorHud.offhandOnLeft(cfg, mainArm)` 统一判定：`!cfg.shieldAutoMirror` 时恒为
  「副手在左」，否则按主手；
- HUD 绘制（`ShieldIndicatorHud`）与「调整位置」预览（`ShieldLayoutScreen`）共用它，预览与实际一致；
- 配置新字段默认 `true`，旧配置文件缺这个键时读出来就是旧行为，**不需要迁移**，schema 仍是 22。

### ④ 自检

`checks=4792 failures=0`（新增 6 条：默认值、右手/左手镜像、关闭后固定右手、左右两个默认 x 公式）。

### ⑤ 未验证

没有实机进游戏点这个开关；界面位置/悬停提示是照现有设置页的写法加的（`CycleButton` + `Tooltip.create`）。

## 1.1.0

### ① 内容

| 文件 | 改动 |
| --- | --- |
| `assets/ctt-hud-fork/textures/gui/meter_0..3.png` | 换成新的一套四档纹理（34×86，对应 空 / 红 ~30% / 黄 ~55% / 绿 满），旧图被替换 |
| `fabric.mod.json` | `description` 改为「为地图“蛋糕塔”（Cake Team Towers）制作！能在敌人、玩家的头顶显示血量条及相关信息；盾牌进入冷却时，会显示冷却指示器；能够随时查看属性的属性面板。」 |
| `gradle.properties` | `mod_version=1.1.0` |

> 说明：`fabric.mod.json` 的 `description` 是**单行**字段（Mod Menu 一行渲染）。
> README 里的简介保留原来的四条列表排版。

### ② 自检

`checks=4786 failures=0`（改的是资源与元数据，没有动逻辑）。

### ③ 纹理档位映射（未变）

`damage / maxDamage`：`<0.4 → meter_3`、`<0.6 → meter_2`、`<0.8 → meter_1`、否则 `meter_0`。

### ④ 未验证

新纹理在游戏里的实际观感（透明度 65% / 默认缩放 0.7 下的叠加效果）没有实机截图。

## 1.0.3

### ① 内容

| 文件 | 改动 |
| --- | --- |
| `fabric.mod.json` | 新增 `contact` 块：`homepage` / `sources` → `https://github.com/nyaLium/CTT-HUD-Mod-Fork-Version`，`issues` → `…/issues`。Mod Menu 的模组详情页会显示这三个链接（之前没有联系方式） |
| `gradle.properties` | `mod_version=1.0.3` |

**无代码改动**：`checks=4786 failures=0`，行为与 1.0.2 完全一致。

### ② 为什么单独发一版

`fabric.mod.json` 会被打进 jar，加上 `contact` 后 jar 字节必然变化；按仓库规则「jar 与源码成对保留」，
所以作为 1.0.3 归档，而不是覆盖 1.0.2 的产物。

### ③ 配置

不变（schema 22）。

## 1.0.2

### ① 内容（只改许可署名与版本号，无代码改动）

| 文件 | 改动 |
| --- | --- |
| `LICENSE`（根） | 改为标准 MIT 文本 + 两行版权声明：`Copyright (c) 2026 nyaLium` / `Copyright (c) 2026 Kirin0321 (麒麟)`；删掉夹在标题与正文之间的 Fork 说明段（GitHub 的 LICENSE 识别能正常认成 MIT） |
| `mod/LICENSE` | 同上（这份是**打包进 jar** 的 `LICENSE_ctt-hud-fork`），末尾保留一句「部分设计源自 Cake-Hud」 |
| `NOTICE.md` | 上游署名改为仓库 `TheHolyLord/Cake-Hud` + 其 LICENSE 的版权人 `Kirin0321（麒麟）` |
| `fabric.mod.json` | `authors` 第二项改为 `Fork of Cake-Hud (TheHolyLord/Cake-Hud, MIT, (c) Kirin0321)` |
| `mod/README.md` | 开头 Fork 说明同步补上真实版权人 |

原来的写法把上游版权人写成了仓库账号名 `TheHolyLord`，而上游 `LICENSE` 实际写的是 `Kirin0321 (麒麟)` —— 这次更正。

### ② 自检

`checks=4786 failures=0`（无代码改动，断言数不变）。

### ③ 未验证

GitHub 的 LICENSE 识别（仓库页右上角显示 MIT 还是 Other）要等推送后 GitHub 重新扫描，属平台行为。

## 1.0.1（已撤回）

> **已撤回**：LICENSE 把上游版权人写成了仓库账号名（实际为 `Kirin0321（麒麟）`）。
> 为免分发署名错误的产物，该版本的 jar 与归档已从仓库移除，仅保留本条文字记录。

### ① 内容（只动注释与文档，不改行为）

- **三个设置页各加一张「行 ↔ 配置字段 ↔ 范围」表**（类 javadoc），照着表就能对上界面、JSON 与代码：
  `EntityHudSettingsScreen`（12 行）、`SelfStatsSettingsScreen`（9 行）、`ShieldIndicatorSettingsScreen`（4 行）。
- **每行一句行内注释**：`buildRows()` / `init()` 里每个 `toggle(...)` / `slider(...)` / `button(...)`
  上方写明它对应哪个字段、范围、默认值和特殊语义（例如 `showBar` 是整块开关、`selfStatsSeconds` 在他人服务器强制 3 s）。
- **校正 1.0.0 删改后的陈旧注释**：把「文字大小 / 垂直偏移 / 不透明度 / 移动面板 / 内容过滤 / 启动盾牌指示器」
  等旧说法统一成现在的文案，并在注释里标注「旧名『xxx』」，方便和旧文档 / 旧截图对照。
- **补数据类字段说明**：`Panel`（x/y/columns/opacity）、`HudLayout.Line`（text/rgb）、`HudLayout.Result`
  （textWidth/barWidth/barTop/hasText）、`HudLayout.Options` 各开关、`BossBarTitle`（name/hp/maxHp）、
  `HeartCounts` 六个下标常量与 `get(kind)`、`EnemyData`（defence / ARMOR_ICONS / hasArmor）、
  `MobHealthPayload.CODEC`、`CttHudForkClient.MOD_ID`。
- **README 新增两节**：「代码导览（每个类做什么）」（39 类 + 5 mixin 一句话职责）与
  「界面文案 ↔ 配置字段对照」（文案 → JSON 字段 → 代码位置），并写明注释约定。

### ② 自检

`checks=4786 failures=0`（与 1.0.0 完全一致 —— 本版没有改任何逻辑，java 只增删注释）。

### ③ 未验证

注释与表格的正确性靠人工逐条核对（对照真实字段与 `lang` 文件），没有自动化校验；
如发现哪条注释与代码不符，告我具体位置即可。

## 1.0.0（正式版 · 已撤回）

> **已撤回**：同上（LICENSE 上游署名错误）。jar 与归档已从仓库移除，仅保留本条文字记录。

> 版本号在这里重新起算：`1.0.0` 是正式版，`1.10.0 – 1.20.42` 是发布前的工作版本线。
> 更早的「1.0.0」只是 2026 年初那条工作线的起点、未归档，和这个正式版不是一回事。

### ① 界面文案统一

| 页面 | 改动 |
| --- | --- |
| 总界面 | 「实体血条设置…」→ **「血条设置…」** |
| 血条设置 | 「显示距离」→ **「最远显示距离」**（敌人 / 玩家两处）；「整体缩放」→ **「缩放比例」**；**删掉两处「垂直偏移」** |
| 属性面板设置 | 刷新间隔的「立即」→ 正常显示 **1 秒**（1~5 秒，不再有「立即」档）；**删掉「文字大小」**；「不透明度」→ **「透明度」**；显示方式默认 **HUD + 容器界面**；「移动面板」→ **「调整位置…」**；「内容过滤」→ **「显示内容过滤…」** |
| 盾牌指示器设置 | 「缩放」→ **「缩放比例」**（步进 **0.05**，两位小数，不再写「倍」）；「移动指示器」→ **「调整位置…」**；「启用盾牌指示器」→ **「显示盾牌指示器」**；「不透明度」→ **「透明度」** |

### ② 删掉的功能（配置 v22）

- **面板「文字大小」**：四套面板的 `*Scale` 字段全部删除，面板固定 1.0 倍（`Panel.scale` 也从记录里去掉，
  绘制 / 命中 / 外框都不再做缩放换算）。
- **实体血条「垂直偏移」**：`verticalOffset` / `teamVerticalOffset` 删除，
  `HudLayout.Options.verticalOffset` 一并删除；布局固定为 `TEXT_TOP` / `BAR_TOP`。
- 迁移：v22 只把「显示方式」的旧默认 0（始终显示 HUD）改成 2（HUD + 容器界面），
  自己选过 1 的不动；被删字段在旧 JSON 里直接忽略，下次写盘就消失。

### ③ 死代码清理

| 删除项 | 为什么是死代码 |
| --- | --- |
| `BoxOcclusion`（整个类） | 唯一使用者是 1.20.25 删掉的「被实体挡住时隐藏」，之后只有自检在用 |
| `HudTarget.occluded()` + `HudConfig.hideBehindWalls` | 该字段从 1.20.14 起被 `normalize()` 写死 false，分支永远短路 |
| `HudConfig.autoPartyBossbar` | 写死 true，条件 `showTeammates && autoPartyBossbar` 等价于 `showTeammates` |
| `HudConfig.panel()` / `panel(int)` / `movePanel(x,y)` / `movePanel(mode,container,x,y)` / `setPanelColumns(int)` / `setPanelOpacity(double)` | 生产代码已全部走带 mode / GUI 尺寸的版本，这些只是历史重载 |
| `SelfStats.looksLikeStat()` | 1.20.24 起线上判据是 `statGlyphLine`，它只被自检当「宽判据锚点」 |
| `BossBarTexture.Tint.background()/progress()` | 实际绘制走 `backgroundTexture(Tint)`，这两个没被调用 |
| `BossBarTitle.suffix()` + `suffixStart` | 护甲段早已不做解析（ArmorCache 从计分板/组件取），只有自检在用 |
| `HudTarget.nameTagAnchor()` | 唯一调用者是上面删掉的 `occluded()` |
| 死语言键（`refreshNow`、`selfStatsScale`、`verticalOffset`、`shieldOffset/Gap/Position*`、`hideBehindWalls/Entities`、`autoPartyBossbar`、`selfStatsX/Y`、两个 `tooltip*`） | 对应功能 / 字段已删 |
| 未使用的 import（`Identifier`、`Locale`、`ClipContext`、`HitResult`、`EntityAttachment`） | 随上面的删除一起失效 |

### ④ 自检

`checks=4786 failures=0`（1.20.42 的 5395 条里，720 条「垂直偏移 × 血条长度 × 缩放」扫描、
约 70 条已删功能（`BoxOcclusion` / 文字大小 / `looksLikeStat` / `suffix` / 写死开关）的断言一并删除；
新增 v22 迁移断言：旧默认 0 → 2、自己选过的 1 保留）。

### ⑤ 未验证

界面文案与设置项的增删看的是编译与自检，**没有实机截图**（无头环境起不了游戏）。
手感层面（滑条步进、按钮文案长度是否溢出）要你进游戏确认。

## 1.20.42

### ① 内容

容器界面那两套配置（模式 1 的 `container*`、模式 2 的 `bothContainer*`）的**默认位置**也改成
`x=4`、`y = 屏幕高 ÷ 3`，和 1.20.41 的 HUD 那两套一致 —— 打开箱子 / 背包时面板同样落在屏幕偏上 1/3。

| 项 | 行为 |
| --- | --- |
| 新配置 | `containerY = -1`、`bothContainerY = -1`（哨兵 = 动态默认，按当前高度算） |
| 「回到初始位置」 | 四套全部写回哨兵（原来容器那两套固定写 `y=20`） |
| 老配置（v20 → v21） | 只有 `containerY` / `bothContainerY` **仍等于旧默认 20** 的才换成哨兵；自己拖过的不动 |
| 配置版本 | v21 |

### ② 自检

`checks=5395 failures=0`（1.20.41 的 5392 + 3）：新配置四套全是哨兵、容器两套回位后按 640×360 画在 y=120、
v20→v21 迁移（20→哨兵、拖过的 55/66 保留）、容器 y 非法值夹到 -1；另把 `SelfStatsLayoutScreen.defaultY`
（已无意义，四套默认一致）换成直接验 `HudConfig.defaultPanelY`。

### ③ 未验证

「打开箱子时面板出现在偏上 1/3」同样只能实机看（自检验的是坐标与迁移）。

## 1.20.41

### ① 内容

属性面板的**默认（初始）位置**从 `x=4, y=40`（贴着左上角）改成 **`x=4, y = 屏幕高 ÷ 3`** —— 左侧偏上，
而且跟着窗口大小走（不是写死一个像素）。

| 项 | 行为 |
| --- | --- |
| 新配置 | `selfStatsY = -1`、`bothY = -1`（哨兵 = 动态默认）；绘制时算成 `屏幕高 ÷ 3`，`x` 固定 4 |
| 窗口变化 | 哨兵每次按当前高度重算，所以永远停在「偏上三分之一」；拖过之后才变成固定像素（仍按 1.20.40 的相对位置换算） |
| 「回到初始位置」 | 写回哨兵（不是当场算一个像素值），三种显示方式各自恢复：HUD / 模式 2 的 HUD 那套 → 动态默认；容器那两套仍是 `y=20` |
| 老配置（v19） | 只有 `selfStatsY` / `bothY` **仍等于旧默认 40** 的才换成哨兵；自己拖过的不动；容器那两套不碰 |
| 配置版本 | v20 |

### ② 自检

`checks=5392 failures=0`（1.20.40 的 5374 + 18）：`defaultPanelY` 4 条（720→240 / 360→120 / 361→120 /
尺寸未知→0）、新配置哨兵与绘制 3 条、拖动后回位 3 条、三种显示方式各自回位 3 条、
v19→v20 迁移 3 条（旧默认 40→哨兵 / 拖过的保留 / 容器不碰）、非法 y 夹到 -1 1 条、`defaultY` 1 条。

### ③ 未验证

真机上「面板一进游戏就出现在屏幕偏上三分之一处」这一条没法在无头环境里看（自检只验了坐标计算）。
若你的配置里 `selfStatsY` 之前被拖过（不是 40），升级后**不会**自动跳到新默认 ——
在「移动面板」里点一次「回到初始位置」即可。

## 1.20.40（当前）

### ① 现象

属性面板和盾牌指示器的位置不是真正的「相对位置」：游戏窗口大小一变（或改 GUI 缩放），
两者有时会跑到屏幕的另一处 —— 靠右边的被挤到中间、靠下的悬在半空。

### ② 根因：存的是**绝对 GUI 像素**

位置一直是 `selfStatsX/Y`、`shieldX/Y` 这种绝对像素（1.20.4 / 1.20.20 起）。而 GUI 像素的
总数随窗口大小和 GUI 缩放变化：1280×720 的窗口里 `x=400` 是「中间」，2560×1440 里就变成「左四分之一」。
所以同一个数字在不同窗口下对应不同的相对位置。

### ③ 改法：坐标 + 「量它的那套 GUI 尺寸」

配置新增 `layoutGuiW / layoutGuiH`（v19）：

| 项 | 行为 |
| --- | --- |
| 存 | 坐标仍然是像素，但**含义变成「在 layoutGuiW × layoutGuiH 这套 GUI 下量的」** |
| 画 | `当前尺寸 ÷ 记录尺寸` 换算后再画（`HudConfig.scalePos`）；面板和盾牌共用这一套 |
| 拖 | 拖动 / 方向键 / 「回到初始位置」都走 `movePanel(..., guiW, guiH)`、`setShieldPosition(...)`：先把其它几套坐标按旧比例换算到新尺寸，再记下新坐标与新参考尺寸 |
| 老配置 | 没有这两个字段（= 0，未知）→ 第一次绘制时用**当时的**尺寸补上；此后窗口怎么变都按比例跟随 |
| 未设置 | 盾牌的 `-1`（跟随副手槽）语义不变；面板默认 (4, 40) 也不变 |

举个数：面板 `x=200` 量在 400 宽的 GUI 里（一半处），窗口放大到 800 → 画在 400（仍是一半处），
换回 400 → 回到 200。靠右的面板换算后可能超出屏幕，仍由原来的 `fitInScreen` / `clampInto` 夹进去
（夹取只影响绘制，不改存储值，所以不会越拖越偏）。

### ④ 自检

`checks=5374 failures=0`（1.20.39 的 5353 + 21：换算纯函数 6 条、面板「放大/原样/换参考系后减半」5 条、
盾牌 5 条、老配置补基准 2 条、非法基准归零 2 条、配置版本 1 条）。

### ⑤ 未验证

真机上「拖到某个位置 → 改窗口大小 → 位置是否仍然对得上」没法在无头环境里看，
自检只证明了换算数学与状态流转。如果某种窗口变化下仍偏，告我一声（哪种操作、变化前后大小）。

## 1.20.39

### ① 为什么要回退 1.20.38

1.20.38 的思路是「迟到的输出仍然算我们自己问来的」——用 20 s 认领窗口 + 欠账计数
（`ownTriggersOutstanding`）把迟到的整批继续吞掉。副作用在实际游玩中暴露了：
**玩家主动敲 `/trigger ViewStats` 有时也会被吞**。

原因在计数与认领的组合上：欠账只在一趟「自己发起的采集拿到内容」时减一，
所以只要出现过一次「采集收尾时 `PENDING` 是空的」（丢包、地图那趟没回够、或那趟里可上屏的行被
`panelWorthy` 过滤掉了），欠账就永久留下 +1；之后再手动触发，标题行/第一行属性行都会被
`claimedAsOwn` 认成「我们那趟」，于是整批进了面板、聊天栏里一个字都看不到。
窗口又会被每 1~5 s 一次的自动触发不断刷新，等于「一旦欠账没清干净，就一直吞」。

**结论**：这条思路本身和 1.20.24 的「手动触发照常显示」冲突，回退。

### ② 新做法：进世界后先安静 3 s（按你的方案）

| 时机 | 行为 |
| --- | --- |
| `ClientPlayConnectionEvents.JOIN` | `SelfStats.onWorldJoined(now)`：清上一服残留，`quietUntilMs = now + 3000`，第一次自动触发也推到 3 s 后 |
| 静默窗口内 | `tick()` 不发 `/trigger ViewStats`；聊天里来的 ViewStats 输出（标题 / 属性行 / 提示行 / 计时行 / 回执）直接丢掉，**也不收进面板** |
| 静默结束后 | 照 1.20.37 的原样：发 trigger、吞这一趟、计时行收尾；**手动触发照旧显示**（1.20.24 不变） |

为什么这样能根治：地图**只会回答 `trigger`**，从不主动推属性。进存档那一刻刷屏的那批，
就是我们自己在「加载地形」时问出来的迟到回答；等世界跑起来再问，一问一答都是毫秒级，
3 s 超时根本碰不到，`selfTriggered` 也就不会被复位成 false。

除「进世界后 3 s」之外还加了第二个条件：本人实体至少过 `JOIN_GRACE_TICKS = 20` tick（≈1 s）。
慢机上「3 s 到了但世界还在载入」时，靠它再挡一层。

### ③ 自检

`checks=5353 failures=0`（1.20.37 的 5337 + 16：静默窗口边界 5 条、常量约束 2 条、
「进世界 → 3 s 内吞掉属性行 / 普通消息照常 / 窗口外手动触发照旧」端到端 8 条、
reset 清零 1 条）。

### ④ 未验证

真机上「3 s 够不够」取决于你那台机器载入存档的耗时，只能在游戏里看：
如果 3 s 之后仍会刷，说明载入比我估的慢，把 `JOIN_GRACE_MS` 调大（或直接告我，我提到 5~8 s）。

## 1.20.38

> **1.20.39 已回退本节的认领机制**（保留此条目作为历史记录）。

### ① 现象

进存档 / 载入世界的那一下，聊天栏会刷出一整批 ViewStats 属性行 —— 本该被吞掉的那些。
稳定之后不复现（一轮问答只要几十毫秒），所以只在「刚进存档」这个时刻看得到。

### ② 根因：3 s 采集超时先把这一趟收了尾

`SelfStats` 的采集是一次「问答」：

1. `tick()` 里 `beginCapture(now, true)`（`selfTriggered = true`），再发 `/trigger ViewStats`；
2. 地图 `cake_team_tower:misc/view_stats.mcfunction` 那 240 多条 `tellraw @s` 回来，`onGameMessage` 逐行吞；
3. 最后一条是计时行（`Misc_View_Stats_78`），它一到就 `endCapture` 收尾。

进存档时世界还在载入（玩家卡在「加载地形」、区块还在生成），服务端往往要好几秒才回这一趟，
而 `settleIfTimedOut` 在 **3 s**（`CAPTURE_TIMEOUT_MS`）就把这趟收了尾：`PENDING` 空、`selfTriggered` 复位成 `false`。

等那批迟到的行到达时已经「不在采集里」，第一行是标题行 `Misc_View_Stats_1`，于是走了
「**玩家手动触发**」那条分支（1.20.24 的约定：手动触发照常显示）→ `selfTriggered = false`
→ 之后每一行都 `return false` → **整批刷进聊天栏**。

### ③ 改法：迟到的那批仍然算「我们问来的」

| 位置 | 改动 |
| --- | --- |
| 发命令 | `noteAutoTriggerSent(now)`：记 `lastAutoTriggerMs`，欠账计数 `ownTriggersOutstanding++`（上限 `MAX_OUTSTANDING_AUTO = 6`） |
| 空手收尾 | 我们自己那趟**一行都没收到**就超时（或连续空采集）时，欠账**不减** —— 还在路上 |
| 拿到输出 | 我们自己那趟收到内容并收尾 → 欠账减一（进存档时可能连发好几趟、服务端随后连回好几批） |
| 迟到到达 | 窗口 `OWN_RESPONSE_WINDOW_MS = 20 s` 内：标题行 → `beginCapture(now, auto=true)` 继续吞 |
| 标题丢了 | `looksLikeViewStatsPayload(flat, a)`（翻译键 / 未解析键 / 提示行 / 计时行 / 数字+图形 / 草方块 `□`）认领回来并重开采集 |
| 回执 | 「已触发 ViewStats」与「无法触发该计分项 / 游戏未开始」也按认领判定吞掉，并把欠账清零 |

欠账用**计数**而不是布尔：进存档时可能连着发好几趟（1~5 s 一次），服务端随后一批一批地回；
只用一个布尔的话，第一批一到就清掉，第二批又会漏进聊天栏。

玩家**真的**手动敲 `/trigger ViewStats`（窗口之外、欠账已清）照旧显示 —— 1.20.24 的结论不变，自检里有反向用例。
「没开局」的大厅分支也会把欠账清零，避免把等待带进下一个大厅。

### ④ 自检

`checks=5371 failures=0`（相对 1.20.37 的 5337 条 +34：认领窗口真值表 7、载荷识别 8、
迟到时序端到端 6、欠账计数 12、窗口常量约束 1）。

端到端那条照着故障时序写：`beginCapture(1000, true)` + `noteAutoTriggerSent(1000)` →
6 s 时喂标题行 / 属性行 / 计时行 → 三行都返回「吞掉」，且属性行确实进了面板；再把同样三行在 90 s
（无 trigger）喂一遍 → 都不吞（手动触发的显示行为保住了）。另有一组「两趟都没回、连回两批」的用例：
第一批收完欠账剩 1、第二批仍然吞、清完之后手动触发不再吞。

### ⑤ 未验证

进存档那一批的**真实到达延迟**（可能落在 3~10 s 任意位置）没法在无头环境里复现，
所以「20 s 窗口够不够」是按最坏情况估的，不是实测值。若打上后仍会漏，告诉我大概漏在哪几行
（标题在不在、隔了多久），我按实测把窗口或超时再调。

## 1.20.37

### ① 回退 1.20.36

1.20.36 的「选过职业」门控（`ClassPassive` → 职业被动道具 → ViewStats「职业点」行）整段删掉，
`SelfStats.visible(...)` 恢复成 1.20.35 的签名与判定：

```java
public static boolean visible() {
    return !notStarted && !timeFrozen && !SHOWN.isEmpty();
}
```

`EnemyData.classPassiveOf(...)`、`hasClassPassiveItem/isClassPassiveItem`、`pendingHadClassStat` /
`lastCaptureHadClassStat` / `classChosen` 缓存、调试行里的 `class yes/no` 一并删除；
`SelfCheck` 里那 12 条 `classGate()` 断言也删掉。**行为与 1.20.35 完全一致（`checks=5337`）。**

> 1.20.36 里关于「`ClassPassive` 客户端读不到」的结论仍然成立，只是不拿它做门控了 —— 详见下一节。

### ② 彻底改名：CTT HUD - Fork

| 类别 | 旧 | 新 |
| --- | --- | --- |
| 仓库目录 | `CTT - Enemy HUD/` | `CTT HUD - Fork/` |
| Gradle 工程目录 | `Cake Team Tower HUD Fork/` | `mod/` |
| `rootProject.name` | `ctt-enemy-hud` | `ctt-hud-fork` |
| 产物名（jar / sources） | `cake_team_tower_hud_fork-1.20.36.jar` | `ctt-hud-fork-1.20.37.jar` |
| Java 包 | `com.nyalium.cttenemyhud` | `com.nyalium.ctthudfork` |
| 入口类 | `CttEnemyHudClient` / `CttEnemyHudServer` | `CttHudForkClient` / `CttHudForkServer` |
| 配置 / 布局类 | `EnemyHudConfig` / `EnemyHudConfigScreen` / `EnemyHudLayout` | `HudConfig` / `HudConfigScreen` / `HudLayout` |
| 翻译键前缀 | `ctt_enemy_hud.*`（约 160 个键） | `ctt_hud_fork.*` |
| 键位名 | `key.ctt_enemy_hud.open_config` | `key.ctt_hud_fork.open_config` |
| Mixin 注入前缀 | `cttEnemyHud$…` | `cttHudFork$…` |

- 保留：`fabric.mod.json` 的 `id = ctt-hud-fork`、配置文件名 `ctt-hud-fork.json`、Mixin 配置文件名
  `ctt_hud_fork.mixins.json`（本来就是新名）。
- **刻意保留**：`LEGACY_FILE_NAME = "ctt-enemy-hud.json"` —— 那是玩家磁盘上可能还存在的旧配置文件名，
  改了就没法自动迁移。
- 归档目录 `versions/`（维护者本地）与 `CODE-AUDIT-1.20.24.md` 是历史记录，**不改写**；`tools/archive_audit.py`
  已改成两种产物名都认（老版本 `cake_team_tower_hud_fork-*`、新版本 `ctt-hud-fork-*`）。
- 副作用：`options.txt` 里旧的 `key_key.ctt_enemy_hud.open_config` 行会失效，键位名变了、默认仍是 `F8`；
  旧的 `ctt-hud-fork.json` 照常读取（配置字段没变），无需手工迁移。

### 自检

`checks=5337 failures=0`（回到 1.20.35 的断言数）；`gradlew build` 在 `CTT HUD - Fork/mod` 下通过。

## 1.20.36

> **1.20.37 已回退本节的门控**（保留此条目作为历史记录）。

### ① 问题

地图在 `gamestart` 就 `enable ViewStats` 了，但职业要等选择窗口结束（`ClassPassive` 由 0 变成 1..26）才定。
这段几秒的窗口里，本模组已经在正常采集、正常上屏 —— 所以「准备大厅 » 选择职业」之间面板会先闪一下。

### ② 关键发现：客户端读不到 `ClassPassive`

按你说的用 `ClassPassive > 0` 做判据是最准的，但**它在原版地图上客户端拿不到**：

- 地图 `scoreboards_part_2.mcfunction` 只是 `scoreboard objectives add ClassPassive dummy`，
  **从未把 `ClassPassive` 放进任何显示槽**（全部 `setdisplay` 语句里没有它）；
- 26.3 的服务端只对「被追踪（`ServerScoreboard.trackedObjectives`，由 `setDisplayObjective` 触发）」的
  objective 同步分数 —— 反编译 `ServerScoreboard.onScoreChanged/onObjectiveAdded` 确认：加分只在
  `trackedObjectives.contains(obj)` 时才发 `ClientboundSetScorePacket`，而 `onObjectiveAdded` 只置脏、不广播。

所以 1.20.36 仍然把 `ClassPassive` 排在第一位（哪天地图把它挂进显示槽、或整合包自己推，就直接生效），
但实际生效的是下面两条**客户端看得见**的等价信号：

### ③ 门控怎么判（按可信度回退）

`SelfStats.classChosen()`，每 250 ms 重算一次（`CLASS_RECHECK_MS`）：

| 顺序 | 信号 | 为什么可信 |
| --- | --- | --- |
| 1 | `ClassPassive > 0`（客户端真能看到时） | 地图自己的判据 |
| 2 | 背包里有职业被动道具 `lime_dye[custom_data={classPassive:1b}]` | `gamestart` 会 `clear @a[tag=!OP]` 清空背包，选完职业才由 `class_passive_main` 重新发这件道具 —— 正好是「已选职业」的直接证据 |
| 3 | 最近一趟 ViewStats 里出现过「职业点」那行（`ClassStat != 0` 才发） | 给「把道具存进箱子」的人兜底 |

三条都没有 → `visible()` 返回 false：面板不画（数据照常采集，`debug=true` 的调试行会多出 `class yes/no`）。
地图回「没开局 / 无法触发」、断线重连、回到大厅时，这三条信号一起清掉，避免把上一局的判定带进下一个准备大厅。

### ④ 配置 / 自检

- 配置版本**不变（v18）**：这次没有任何字段变化，只是行为门控。
- `checks=5349 failures=0`（+12：可见性真值表、「职业点」行置位/清位、没开局回执清信号、道具判定）。

## 1.20.35

### ① 「优先使用服务端数据」写死开启

这个开关在「敌人设置」里，默认就是开；关掉它的唯一效果是让客户端**无视**服务端组件推来的权威血量/心数，
退回「Boss 栏那条最近敌人 + ArmorCache」的粗糙口径 —— 没有任何收益，反而容易让人误以为数据不准。按你的要求：

- 界面里这一行删掉（「敌人设置」现在只剩：显示血条 / 显示距离 / 血条长度 / 整体缩放 / 垂直偏移）；
- `Cfg.useServerData` 字段删掉，`HudWorldRenderer.resolveData()` 直接读 `ClientMobHealthCache`；
- 回落逻辑不变：服务端**没装**本 Mod 时缓存里没有条目（或 5 s 后失鲜，见 `FRESH_WINDOW_MS`），
  照旧回落到队伍条 / `below_name HealthPercent` / Boss 栏，所以「客户端必装、服务端选装」不受影响。

### ② 配置

- 版本 v17 → **v18**：旧 JSON 里的 `useServerData` 键读取时忽略，下次「完成」写盘就消失。
- 没有其它数据迁移。

### 自检

- `checks=5337 failures=0`（这次只删字段/界面行，断言总数不变，版本号断言跟到 18）。
- 按要求不进游戏实测。

## 1.20.34

### ① 两个「显示血条」统一成整块开关

之前在「实体血条设置」里有三个相关开关，语义互相重叠、名字还不一样：

| 位置 | 字段 | 1.20.33 之前的行为 |
| --- | --- | --- |
| 敌人设置 → 显示血条 | `showBar` | **只关血条**，心数 / 护甲文字照旧画 |
| 玩家设置 → 显示队友血量 | `showTeammates` | 关掉队友**整块**（血条 + 文字，`HudTarget.managed` 直接返回 false） |
| 玩家设置 → 显示血量条 | `teamShowBar`（1.20.33 新加） | 只关队友血条，文字照旧 |

这就是你问的「这两者有什么区别」：**一个只关条、一个连文字一起关**。现在按你的要求统一：

- 敌人、玩家设置里**各只有一个「显示血条」**：开 / 关；
- **关掉后血条和上方文本都消失**，只剩原版名字：
  - 敌人：`HudWorldRenderer.options()` 把 `showNumbers / showHearts / showArmor / showBar`
    四个 flag 全跟着 `showBar` 走 → `HudLayout.compute` 返回空 → 渲染直接 return；
  - 玩家：`showTeammates` 关掉时 `HudTarget.managed()` 直接 false，整块不画（本来就如此），
    界面标签改成「显示血条」；
- `teamShowBar` 删掉（它就是为了「只关条」存在的，和「关掉整块」冲突）。配置 v17：旧键读取时忽略。

### ② 滚动提示不再写「右侧滚动条可点可拖」

界面底部提示回到：

- 设置页：`滚轮滚动查看更多`
- 内容过滤：`滚轮滚动 · 点击一行切换显示/隐藏`

拖动能力保留（±5 px 命中、悬停/拖动提亮都还在），只是不写在提示里。

### 自检

- `checks=5337 failures=0`：把 1.20.33 里 `teamShowBar` 的几条断言换成
  「关掉「显示血条」后敌人四个 flag 全关 + 布局为空」「打开后有内容」「关敌人不影响队友」。
- 按要求不进游戏实测；界面外观与手感只能在游戏里确认。

## 1.20.33

### ① 所有滚动条都能点/拖

本 Mod 一共有 3 个带滚动条的界面：**实体血条设置**、**属性面板设置**、**内容过滤**。
1.20.32 给它们都接上了 `mouseClicked / mouseDragged / mouseReleased`；这一版做的是「确认没有漏、并且更好抓」：

- 三个界面的拖动状态机、命中判定、反解统一走 `ScrollPane`（滑块高度 / 偏移 / `scrollForThumbTop`），
  过滤页的条形几何也用同一组纯函数；
- **命中容差 3 → 5 px**（条本身 4 px，手指/鼠标很难精确压在 4 px 上）；纵向只在轨道范围内响应，
  不会把下面的按钮/空白误判成拖条；
- **悬停/拖动时滑块提亮**（`0xC0FFFFFF → 0xFFFFFFFF`，轨道底色也加深一点），能看出「这条是能抓的」；
- 点滑块 = 保持抓取偏移拖动；点轨道空白 = 滑块中心跳过去再继续拖；滚轮照旧。

> 顺手核对了 26.3 的输入链（`javap` 看 `MouseHandler`）：`Screen.mouseDragged` 只要求
> 「有按下的按钮 + `mousePressedTime > 0`」，跟 `mouseClicked` 的返回值无关，
> 所以按下滚动条后拖动一定会收到 `mouseDragged`；`ContainerEventHandler.mouseClicked` 只在
> 光标底下**有控件**时才返回 true，滚动条那一条列是空的，不会把点击先吞掉。

### ② 局部设置 / 全局设置再审计

按「一处设置不许带动另一处」重新扫了一遍所有写配置的路径（界面 + 配置层），修掉 3 处：

| 问题 | 表现 | 修法 |
| --- | --- | --- |
| 队友血条用了敌人的 `showBar` | 在「敌人设置」里关掉血条，队友的条一起消失 | 新增 `teamShowBar`（默认开），`HudWorldRenderer.teamOptions` 改读它；「玩家设置」多一行「显示血量条」（`showTeammates` 关掉时灰掉） |
| 实体设置页滑块重建时回弹 | 滚一下/切个开关，滑条位置跳回**建表时**的旧值（值没丢，但看起来像被改了，之后再拖也会从错的位置算） | `SliderSpec` 的初值从快照值改成 `DoubleSupplier`，每次 `rebuildWidgets()` 都读当前配置 |
| 「回到初始位置」写死 `(4, 40)` | 容器界面那套的出厂 y 是 20，点「回初始位置」却跑回 HUD 的 40 | `SelfStatsLayoutScreen.defaultY(container)`：HUD 40 / 容器 20 |

确认本来就独立、这次没动的：四套面板配置（`selfStats*` / `container*` / `both*` / `bothContainer*` 的位置、列数、
字号、不透明度各自成套）、`textShadow` 与 `selfStatsTextShadow`、盾牌的位置/缩放/不透明度、
两条 Boss 栏开关（`hideEnemyBossBar` / `hideTeamBossBar`）。
**仍然全局**（有意的）：总开关 `enabled`、实体血条共用的 `opacity`、面板刷新间隔 `selfStatsSeconds`、
内容过滤表 `selfStatsHidden`（面板各套共用同一份过滤，切显示方式不会变）。

### 自检与验证

- `checks=5337 failures=0`（+19：队友/敌人条开关互不影响、四套面板写入互不串改、两种文字阴影、
  回初始位置默认 y、滚动条 ±5 px 命中边界）。
- 按要求**不进游戏实测**：鼠标手势本身在无头环境点不了，验证靠自检 + 反编译核对输入链；
  进游戏若某条滚动条仍拖不动，告诉我「哪一页、按在滑块还是轨道上、有没有悬停提亮」。

## 1.20.32

### ① 「游戏时间」「难度骷髅」移入「其它」

1.20.31 分节时它们还留在「属性」里。按你的要求挪到「其它」，现在三节是：

| 分节 | 项数 | 内容 |
| --- | --- | --- |
| ── 属性 ── | 56 | 心、生命/治疗、攻击、防御、元素护甲、速度/跳跃、法力、杂项数值 |
| ── 状态效果 ── | 28 | `status_49` … `status_76`，一条一项 |
| ── 其它 ── | 4 | **游戏时间（`timer`）、难度骷髅（`skulls`）**、草方块（`grass`）、未识别行（`misc`） |

分节顺序由枚举声明顺序决定，所以把 `TIMER` / `SKULLS` 两个常量挪到了 `GRASS` 前面。
行结构仍是 47 行（属性 28 行 + 状态效果 14 行 + 其它 2 行 + 3 行标题），滚动范围不变。

### ② 滚动条可以用鼠标点/拖了

以前三个滚动页（实体血条设置 / 属性面板设置 / 内容过滤）的滚动条只是画出来，点它没反应。
现在：

- `ScrollPane` 出几何 + 命中判定：滑块高度 = 视口² ÷ 内容（过滤页按可见行占比、下限 8 px），
  偏移 = `scroll / maxScroll` 映射到 `[0, 轨道 - 滑块]`，反解 `scrollForThumbTop()` 是它的逆运算；
- 命中带 **±3 px 容差**（条本身只有 4 px，不放宽根本点不中）；
- 点滑块 → 按住拖动，抓取偏移不跳位；点轨道空白 → 滑块中心跳过来再继续拖；
- 拖动状态放在各界面里（`barDragging` / `barGrab`），因为滚动会 `rebuildWidgets()` 重建 `ScrollPane`；
- 事件坐标沿用两个拖动界面的处理：坐标超过 GUI 尺寸就按窗口物理像素换算（`Pointer.gui`）；
- 三个页面的提示同步改成「滚轮滚动 · 右侧滚动条可点可拖」。

### ③ 盾牌指示器默认不透明度 75% → 65%

`Cfg.shieldOpacity` 默认值改 `0.65`；迁移只改「仍是旧默认 0.75」的配置（v12 的 1.0 先经 v13 落到 0.75、
再经 v15 落到 0.65），自己调过的不动。

### ④ 属性面板的文字阴影独立出来

以前实体血条的「通用 → 文字阴影」和属性面板共用 `Cfg.textShadow`，改一边连带改另一边。现在：

- `textShadow`：只管实体血条（名字 / 心数 / 护甲），**默认关闭**（按你的要求）；
- `selfStatsTextShadow`：新的属性面板开关，**默认开启**（= 升级前 `textShadow=true` 的观感），
  加在「属性面板设置」页的第 6 行（不透明度下面），`SelfStatsPanel` 的 HUD / 容器面板 / 移动预览都读它；
- 迁移 v14→v15：实体血条那个 `textShadow` 跟到新默认「关」；`selfStatsTextShadow` 是新字段，
  直接用它的默认值「开」—— **不继承**旧的 `textShadow`。因为以前面板是被它顺带影响的，
  为了关头顶阴影而关掉它的人，面板不该被一起带走。

### 自检与验证

- `checks=5318 failures=0`（+38：分节归属、47 行结构、滚动条滑块/偏移/反解/命中容差、文字阴影两开关与 v15 迁移）。
- 按你的要求**没有进游戏实测**：这一版没有游戏内截图，验证只有自检 + 源码对照。
  鼠标事件链（`mouseClicked/MouseDragged/mouseReleased`）本身无法在无头环境点，只有自检覆盖纯函数部分。

## 1.20.31

### ① 游戏时间真的上面板了

`Misc_View_Stats_78` 那行以前只被当作「这一趟 ViewStats 到此结束」的收尾信号：
`onGameMessage` 收到它就 `pendingTimeText = flat; endCapture(...)`，**这一行本身从来没进过面板**，
所以「内容过滤」里那条「游戏时间」开关等于空转。

现在收尾前先把它按 `SelfStatsKind.TIMER` 收进 `PENDING`：面板最后一行就是「游戏时间为 00:00:19」
（地图最后才发它，所以顺序天然在最后）。两种情况仍然不显示：

- 没装地图资源包 → 这行是未解析的翻译键（`Misc_View_Stats_7800:00:19`），照旧不上面板；
- 「内容过滤」里把「游戏时间」关掉。

### ② 「状态效果」「其它」不再是一个大桶

地图 `view_stats.mcfunction` 里有 28 条状态效果（`Misc_View_Stats_49..76`，带对应 tag 才发），
以前全归一个 `status` 类；「其它」那个 `misc` 兜底则把草方块和一切认不出的行混在一起。
现在拆成一条一项，并按三节分组：

| 分节 | 项数 | 内容 |
| --- | --- | --- |
| ── 属性 ── | 58 | 心、生命/治疗、攻击、防御、元素护甲、速度/跳跃、法力、杂项数值（逐项，和以前一样） |
| ── 状态效果 ── | 28 | `status_49` … `status_76`：斗志昂扬 / 食物专家 / … / 审视（名字取自地图资源包官方文案，中英都补了） |
| ── 其它 ── | 2 | 草方块（`grass`）+ 未识别行（`misc` 兜底） |

- 过滤界面从「平铺两列」改成「分节 + 每节一行标题 + 两列按钮」：标题不可点、不占按钮位，
  观感和实体血条设置里的「── 通用 ── / ── 敌人设置 ──」一致。
- **状态效果 28 条的名字**：中文取地图资源包 `ctt_lang` 的 `Misc_View_Stats_49..76`，英文取 `en_us`
  （`Fired Up`、`Food Connoisseur`…）。`status_49` 的键号 = 地图 `Misc_View_Stats_49`。
- **「其它」里的草方块**是顺手修的 bug：地图 `view_stats_83` 那一行就是一个绿色的 `□`，
  没有翻译键也没有数字，连「像不像属性行」都过不了，以前被整行丢掉 —— 所以面板上从来见不到它。
  现在按 `isGrassLine()`（`strip()` 后恰好是 `□`）认，能显示、也能单独关。
- 「未识别行」保留：真正认不出的行（以后地图加了新属性）仍然可以一键关掉，
  但它不再兜住上面这些能认出来的东西。

### 兼容 / 迁移

- 配置版本 13 → 14。旧配置里的粗类 `status` 在读取时展开成 28 条（`COARSE_KINDS`），
  粗类 `misc` 的展开表里多了 `grass`；其余字段不变。
- 过滤项总数 60 → 88。

### 自检与验证

- `checks=5280 failures=0`（+82：分节行结构、28 条状态效果映射、草方块识别、计时行上屏、旧粗类展开、列宽）。
- 另跑了一个离线脚本按 `view_stats.mcfunction` 复刻 `classify()`：全表 134 条 `tellraw @s` 里
  只有 `□` 那一条会落到 `misc`，其余都能归到具体类 —— 所以「其它」拆开后只剩「草方块 + 兜底」。
- 语言文件另做静态校验：`SelfStatsKind` 89 个枚举里 88 个可过滤项，中英两份 lang 各 161 键、键集完全一致。
- **按你的要求没有进游戏实测**：这一版没有游戏内截图，验证只有自检 + 源码/数据包对照。

## 1.20.30

- **F8 键位本来就在「按键控制」里**：`KeyMappingHelper.registerKeyMapping` 把
  `key.ctt_hud_fork.open_config`（默认 F8）注册进 `Options.keyMappings`，证据是
  `options.txt` 里有 `key_key.ctt_hud_fork.open_config:key.keyboard.f8`（这一行只有注册过的绑定才会被写出）；
  之前它挂在 `KeyMapping.Category.MISC`（杂项），跟一堆调试键混在一起不好找。
- **改成独立分类**：`KeyMapping.Category.register(ctt-hud-fork:main)`，显示名走
  `key.category.ctt-hud-fork.main`（中英都补了）→ 现在「按键控制」里会看到一栏「CTT HUD - Fork」。
- 自检：`checks=5198 failures=0`（键位注册不参与自检）。
- 已验证：开发客户端能正常启动到标题画面，日志无 `Category ... already registered` 之类的异常；
  `options.txt` 里该绑定仍在。**「按键控制」界面本身没截图**（要进游戏一路点进去，按你说的没测 UI）。

## 1.20.29

- 拖动界面的操作方式写进界面提示（`ctt_hud_fork.config.selfStatsMove.dragHint`）：
  中文「按住拖动；也可用方向键微调（按住 Shift 每次 16 px）」/ 英文「Drag to move; arrow keys nudge (Shift = 16 px per press)」。
  「移动面板」和「移动指示器」共用这条文案，画在标题下方一行（黄色），显示「当前正在调整」那行下移到 y=50。
- 自检：`checks=5198 failures=0`（纯文案/布局）。
- 实测：进游戏打开「移动指示器」，提示正常显示、不与标题重叠。

## 1.20.28

- **防误操作：拖动界面只认「真拖」**。之前是「整屏可抓 + 按下即吸附」，点一下空白处面板/指示器就跳过去。
  现在：只在面板（含 4 px 余量）上按下才算抓住；按下后位移 ≥2 px 才写位置；点一下或手抖 1 px 都不动。
- 判定抽成 `SelfStatsPanel.grabHit(bounds, margin, x, y)` / `dragExceeds(dx, dy, threshold)`，两个界面共用。
- 方向键微调（4 px / Shift 16 px）不变，鼠标不好使时仍然可用。
- 自检：`checks=5198 failures=0`（+13：抓取余量边界、拖动阈值）。
- 未在真机验证鼠标（这台开发客户端的鼠标注入进不了窗口），只有自检 + 代码路径。

## 1.20.27

- **拖动预览不再模糊背景**：`SelfStatsLayoutScreen` / `ShieldLayoutScreen` 覆写
  `extractBlurredBackground`（不调 `blurBeforeThisStratum()`），世界保持清晰，方便对着真实画面把面板/指示器拖到想要的位置。
  原版那层很淡的压暗（`INWORLD_MENU_BACKGROUND`）保留，文字仍然清楚。
- 自检：`checks=5185 failures=0`（纯渲染开关，无逻辑改动）。
- 实测：进游戏打开「移动指示器」，背景的墙面边缘、门框、手臂纹理都清晰可辨（对比 1.20.26 的糊背景）。

## 1.20.26

- **盾牌纹理换成 34×86 新四张**（meter_0 = 空 / meter_1 = 1 格红 / meter_2 = 2 格黄 / meter_3 = 3 格绿），
  `ShieldIndicatorHud.TEX_W/TEX_H` 跟着改成 34/86；仍然是「整张等比缩放」，没有平铺。
- **默认值**：`shieldScale 1.0 → 0.7`、`shieldOpacity 1.0 → 0.75`；配置版本 12 → 13，
  迁移沿用本文件惯例：只有仍停在旧默认 `1.0` 的才跟着改，自己调过的不动。
- **默认位置统一并写清**：`defaultX/defaultY` —— 水平对准副手物品格中心（右手玩家 `guiW/2 − 109`，
  左手玩家镜像 `+109`），底边距屏幕底 76 px。**同时修掉「移动指示器」预览固定用 `+109` 的 bug**：
  之前预览画在右侧、实际 HUD 画在左侧，看起来像「位置会跟着副手槽跳」。
- **mod 图标**：`assets/ctt-hud-fork/icon.png` 换成你给的 `pack.png`（24×24，原样，未放大）。
- 自检：`checks=5185 failures=0`（+10：默认值 / 默认位置 / 纹理尺寸 / v13 迁移）。
- 实测：进游戏给了一把 `damage=150` 的盾牌 —— 指示器落在 GUI (92,104)、尺寸 24×60（GUI 缩放 6），
  即 `guiW/2−109−12`、`guiH−76−60`，与公式逐像素一致；新纹理渲染正常（无平铺/拉伸）。

## 1.20.25

- **死代码清理**（审计 `CODE-AUDIT-1.20.24.md` 第四节）：删除 `HudConfig.copy/restore/panelFor`、
  `BossBarTexture.drawBackground/drawProgress`、`ClientMobHealthCache.serverComponentPresent/payloadType`、
  `ScrollPane.scrollToTop`、`SelfStats.displayName/storedLinesOf/windowEndMs/CAPTURE_WINDOW_MS`、
  `SelfStatsPanel.clampX/clampY/EDGE_KEEP`（拖动早已改用 `fitInScreen`），以及随之失效的 `KIND_HOVER`/`rememberHover`。
- **僵尸配置**：`hideBehindEntities` 字段删除（默认 true 却没人读、normalize 又强制 false）；
  旧配置里的这个键读取时忽略、下次写盘消失；配置版本 11 → 12。`hideBehindWalls` / `autoPartyBossbar` 保留并注明写死原因。
- **自检专用成员**：`looksLikeStat`、`heartsOf`、`NAME_TOP`、`NATIVE_HEIGHT` 等加「仅 SelfCheck 使用」注释，防误删。
- **文档**：README 新增「内部常量一览」（属性面板/渲染判定/网络聊天三张表，共 30+ 个常量）；
  「设置界面」整节重写为 1.20.25 现状；「版本沿革」补 1.20.11–1.20.25 共 15 行；
  `HudConfig` / `HudConfigScreen` / `CttHudForkClient` / `PartyBossbarAutoToggle` / `BoxOcclusion` 的类注释订正。
- 自检：`checks=5175 failures=0`（版本断言 11→12，其余不变）。

## 1.20.24

- **手动 `/trigger ViewStats` 不再被吞**：照抄上游 `StatsData` 的 `hideCapture` ——
  `markAutoTriggered()` 只在本模组自己发命令时置位，`processMessage` 每条都 `return hideCapture`。
  我这边加 `selfTriggered`：自动刷新那趟才吞；玩家手动触发那趟**全部照常显示**，同时仍然收进面板。
  收尾信号也换成上游的做法：**计时行（`Misc_View_Stats_78` / 「游戏时间」）到达就结束这一趟**，
  等不到才用 3 s 超时兜底；另外补了 `已触发 ViewStats` 确认行与 `Hover over / 停留` 提示行的处理。
- **删掉「回到初始位置」那句提示**：盾牌指示器的默认位置是「跟随副手槽」，不是左上角，原提示是错的。
  `selfStatsMove.hint` 两个语言文件都删了，面板/盾牌两个拖动界面不再画那行黄字。
- **内容过滤列表不再挤**：`TOP 58→54`、`BOTTOM_PAD 58→40`，「共 N 项 · 已隐藏 M 项」与
  「滚轮滚动 · 点击一行切换」合成一行 → 同一窗口下多显示一行（480 高：14→15 行）。
- 自检：`checks=5176 failures=0`。
- 实测：手动触发三行（标题 / `87❤` / 计时行）在聊天栏**全部可见**、面板同时更新；
  过滤页多一行且表头合并成一行；拖动界面已无「回左上角」提示。

## 1.20.23

- **拖动改上游式绝对定位**：`_tools\cakehud\ConfigScreen` 的 `beginPreviewDrag / movePreviewDrag`
  是「按下记偏移 → 移动用鼠标绝对坐标减偏移」，我这边原来是累加 `dragX/dragY`，
  增量丢帧或后端只给绝对坐标时就「拖不动」（容器界面那套就是这么卡的）。
  两个拖动界面都改了，并补上：整屏可抓、`fitInScreen` 整块留屏内、方向键 4 px（Shift 16）、
  模式 2 就地切换「对象：HUD / 容器界面」。
- **「重置位置」→「回到初始位置」**（按钮 + 提示，属性面板两套与盾牌共用）。
- **聊天不再被误吞**：删掉「两个字以内」和「数字开头」两条宽规则，改成按地图 tellraw 真实形态判定
  （`Misc_View_Stats_*` 键 或「数字 + 图形」），并对 `chat.*` 顶层键一票否决；
  玩家手动 `/trigger ViewStats` 时标题行自己开一趟采集。
- **游戏时间冻住不显示面板**：计时行 `Misc_View_Stats_78` 两趟完全相同且间隔 ≥1500 ms → 视为没开局。
- 自检：`checks=5174 failures=0`（+23）。
- 实测：盾牌拖动界面方向键微调 → 读数从 `auto x=304 y=58` 变 `x=218 y=69`，完成后面板写盘 `shieldX=218 / shieldY=69`。
- 未验证：容器界面那套拖动的真机截图；聊天过滤 / 时间冻住只做了自检。

## 1.20.22

- **盾牌指示器：缩放**
  - 设置项改名「尺寸」→「缩放」，滑块 `0.5 ~ 1.5`、步长 0.1，显示 `1.2 倍`；
  - 根因修复：之前用 10 参数的 `blit`，它的宽高**同时被当成源区**，
    目标放大到 k 倍时 UV 跨了 k 张纹理 → 画成 k×k 平铺（1.2 倍能看到右下多两条）。
    现在改用 13 参数重载 `(x, y, u, v, 目标宽, 目标高, 源宽, 源高, 纹理宽, 纹理高, 颜色)`，
    源区固定整张 36×106，目标 `36*k × 106*k` —— 真缩放。
  - 拖动预览与 HUD 共用 `ShieldIndicatorHud.scaledWidth/Height/blitScaled`，不会再各算一套。
  - 配置版本 10 → 11，`shieldScale` 夹取改成 `0.5 ~ 1.5`。
- **滚动列表：真裁剪**
  - 根因：内容控件由 `Screen` 自动绘制，没有任何裁剪；滚到底时最后一行直接画到屏幕下沿之外，
    还会盖住「滚轮滚动查看更多」和「返回」。
  - 现在内容控件用 `addWidget` 只登记输入，绘制交给 `paintContent()` 在
    `enableScissor(top, bottom)` 之间完成 —— 部分露出的行被**切掉下半截**，绝不会越过可视区；
    完全在区外的行连控件都不建（鼠标点不到屏幕外的东西）。
  - 实体血条设置、属性面板设置两页都改成同一套（内容过滤页本来就是按可见行创建，未变）。
- 自检：`checks=5151 failures=0`（+26：缩放夹取/像素尺寸、滚动可视区数学）。
- 实测见下：0.5 / 1.0 / 1.5 倍黄区像素 54×174 / 108×348 / 162×522（严格 1:2:3）；
  两个滚动页滚到底的截图里，最后一行之后直接是提示与「返回」。

## 1.20.21

- **盾牌尺寸**：单位改为「倍」（1~4、步长 1）；渲染按 `TEX_W*k × TEX_H*k` 整数倍放大，
  不再拉伸纹理（修掉 1.1 倍时的双列伪影）；拖动预览同一套计算。
- **滚动边界统一**：可视区下沿 `高度-56`、提示 `高度-46`、返回 `高度-28`；
  控件只在整行完全可见时创建，分组标题同样裁剪；内容过滤页 `BOTTOM_PAD 34→58`。
- **模式 2 拖动标题**：显示「HUD + 容器界面 · HUD」或「· 容器界面」。
- 自检：`checks=5125 failures=0`。
- 实测：1 倍盾牌指示器单列渲染正常；滚动条/标题文字未截图。

## 1.20.20

- **统一滚动**

- **统一滚动**：新增 `ScrollPane`（可视区夹取 + 右侧 4px 滚动条 + 滚轮按行）；
  实体血条页修正上下限；「属性面板设置…」（外层改名）也改成同一滚动框架。
- **删除** `EnemyHudSettingsScreen`（功能并入新的属性面板设置页）。
- **盾牌指示器**：位置改为 `shieldX/shieldY`（GUI 坐标，-1 = 自动副手槽上方）+「移动指示器」拖动界面；
  新增 `shieldOpacity`（`blit(RenderPipelines.GUI_TEXTURED, …, color)` 带 alpha）；
  纹理替换为用户提供的 36×106 `meter_0..3.png`。
- 配置版本 9 → 10（`shield*` 字段改为坐标/透明度）。
- 自检：`checks=5125 failures=0`（+3：盾牌字段夹取）。
- 实测：外层菜单、盾牌条渲染（`versions/pre-release/v1.20.20/*.png`）；滚动条与拖动未截图验证。

## 1.20.19

- **开关联动**

- **开关联动**：总开关关闭 → 三个入口灰掉；显示血条关闭 → 敌人组灰掉；显示队友血量关闭 → 玩家组灰掉；
  盾牌开关关闭 → 盾牌组灰掉（属性面板原行为保留）。
- **显示方式**改名并排序：第一项「HUD + 容器界面」、第二项「HUD 显示」、第三项「容器界面显示」；
  按钮按 2 → 0 → 1 循环，三套配置保持独立。
- **总界面重排**：`启用 HUD`（外置）、`实体血条设置…`、`属性面板设置…`、`盾牌指示器设置…`、`恢复默认`、`完成`。
- **实体血条设置**：合并原通用/敌人/玩家三页，单列 + 分组标题 + 滚轮滚动（只创建可视区控件）。
- **盾牌指示器整合**：移植 `CTT - Shield Enhancing` 的 HUD 到 26.3（`HudElementRegistry` + `blit`），
  纹理复制进本 Mod，新增 `shieldEnabled/shieldPosition/shieldScale/shieldOffset/shieldGap` 配置与设置页。
- 自检：`checks=5122 failures=0`。
- 实测：外层菜单截图（`versions/pre-release/v1.20.19/outer-menu.png`）。
  **未验证**：实体页滚动/灰化、盾牌条渲染（需要带 `shield:1b` 的副手物品且掉耐久）。

## 1.20.18

- **模式 2 再拆两套**

- **模式 2 再拆两套**：`both*`（HUD）+ `bothContainer*`（容器界面），配置共 4 套；
  `panel(mode, container)` 统一取值，写接口 `movePanel/setPanelColumns/Scale/Opacity` 都带 `(mode, container)`；
- 模式 2 的设置段多一行「编辑对象：HUD / 容器界面」，切换立即重建；其它模式编辑目标固定；
- 配置版本 8 → 9：`bothContainer*` 从 `both*` 复制。
- 自检：`checks=5122 failures=0`（+2：模式 2 两套互不影响、v8→v9 迁移）。

## 1.20.17

- **三种显示方式独立配置**

- **三种显示方式独立配置**：模式 0 用 `selfStats*`、模式 1 用 `container*`、模式 2 用新增的 `both*`；
  「移动面板」「列数」「字号」「不透明度」只写当前模式那一套，切换互不影响。
- 配置版本 7 → 8：`both*` 从 HUD 那套复制而来。
- 自检：`checks=5120 failures=0`（+3：模式 2 取值/移动隔离、v7→v8 迁移）。
- 说明：模式 2 的 HUD 与容器界面**共用** `both*` 这一套；若希望模式 2 内部再分两套，需要另加字段。

## 1.20.16

- **过滤界面的属性名改为官方中文**

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
- 实测：模式 0 HUD 显示、模式 1 背包里显示、模式 1 关背包后 HUD 不显示（`versions/pre-release/v1.20.14/*.png`）；
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
- 实测：夜间暗处 `❤150K/200K` 全亮（`versions/pre-release/v1.20.13/big-numbers-night.png`）。
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
- 实机：真 CTT 服开局后面板正常出数；`versions/pre-release/v1.20.11/enemy-layout-3x3.png` 为设置页截图
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
- 实机：`versions/pre-release/v1.20.9/settings-no-position.png`、`filter-vanilla-width.png`、
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
- 证据：`versions/pre-release/v1.20.8/run-panel.png`。

## 1.20.7

- **纯客户端也显示百分比数字**：`HudLayout` 的 `percentOnly` 分支以前不产出文字
  （只有血条），现在写 `40%`（`round(hp*100/maxHp)` 夹 0~100，`maxHp<=0` 按 100%）。
  受影响的是「只有 `below_name = HealthPercent`」的两类：服务端没装本 Mod 时的敌人，
  以及队伍条里没有条目的队友（绿条 + `63%`）。
- 有精确数据（`❤80/200`）或「当前/上限」（`63/120`）的路径完全不变；`showNumbers` 开关仍能关掉数字。
- 自检：`checks=5063 failures=0`（+34：percentOnly 各类取值、关数字开关、队友百分比回退、未知心数的 当前/最大）。
- 实机 A/B（同一只怪只切 `useServerData`）：`versions/pre-release/v1.20.7/percent-only.png`（`40%`）↔
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
- 实机：本地（未上传） 四张图（默认面板、改名后的标题、双列过滤、滚动+切换后 `1 hidden`）。

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
- 实机：本地（未上传） 四张图（默认面板、隐藏两个属性、过滤列表界面、现实时间文案）。

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
- 实机：本地（未上传） 下 6 张证据图（提示行消失 / 字号透明度 / 红心过滤 / 职业过滤生效 /
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
- 实机：`versions/pre-release/v1.20.3/self-stats.png`（真局里面板内容 + 聊天栏无刷屏）。

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
- 实机对照 `versions/pre-release/v1.20.2/bar-caps.png`（旧=方头 / 新=圆头 / 40% 直切边界）。

## 1.20.1

- **血条改回原版 BossBar 画法**：以前是「progress 0..fill + background fill..end」两段相邻拼接，
  两张纹理各裁一半、端盖对不上，接缝处会漏 1 px 的缝（未满时中间一条白线）。
  现在先画**整条** `*_background`，再把 `*_progress` 从左裁到 `fill` 叠上去（同原版
  `BossHealthOverlay`）；背景往远处挪 0.02 个名牌像素，保证进度始终在上、不闪烁。
  对照图 `versions/pre-release/v1.20.1/bar-before-after.png`。
- 自检：`checks=1580 failures=0`（布局与数据逻辑未变）。

## 1.20.0

- **心类轮换**（新增纯函数 `HeartCycle`，每 2 s 一种、实体 id 作 `salt` 错开）：
  - 敌人：`❤当前/上限` + 魂/黑/蓝/碎/粉里**当前这一种**（顺序固定，只挑真有的）；
  - 玩家：`❤当前/上限` + 魂 ↔ 黑。
  实测 6 帧：敌人 `💛50` ↔ `💔2`；玩家 `💛40` ↔ `🖤30`（`versions/pre-release/v1.20.0/*-cycle.png`）。
- **玩家碎心/粉心 + 蓝心固定在最上面一行**（同时显示，不参与轮换）。
- **护甲与元素护甲之间两个空格**（`HudLayout.ARMOUR_GAP`）：`🛡5  🔥3 ☃1`。
- **取消 1.19.0 的玩家心数换行**：行数回到固定（敌人 1~2 行、玩家 1~2 行），
  `Options.wrapHearts` 与换行代码/断言一起删除。
- 自检：`checks=1580 failures=0`（+14：`HeartCycle` 各分支、单一轮换心、两空格、玩家特殊心置顶）。

## 1.19.0

- **透视副本恢复**：每行文字提交两遍 —— `NORMAL`（不透明、无背景、深度测试）+
  `SEE_THROUGH`（约 50% 文字 + 25% 黑背景、不做深度测试），参数与原版 `submitNameTag` 一致。
  实测僵尸挡在 Bot2 前面时，Bot2 的 `❤100/100 💛50` 仍隐约可见
  （`versions/pre-release/v1.19.0/see-through.png`）。
- **默认缩放 0.9**：`scale` / `teamScale` 默认由 1.0 改 0.9，`configVersion` 升到 3；
  v1（无 `configVersion`）迁移 100→75、1.0→0.9，v2 迁移 1.0→0.9，自己调过的不动。
- **玩家心数自动换行**（1.20.0 已取消）：新增 `Options.wrapHearts`（玩家 true、敌人 false），
  阈值**写死**为血条长度（血条关掉时 75），无配置项；贪心换行，每行至少一段。
- **碎心/粉心/蓝心挪到最上面一行**：最上面一行 = 护甲 + 碎心/粉心 + 蓝心；紧贴血条的只剩红/魂/黑。
  截图 `versions/pre-release/v1.19.0/player-hearts-wrap.png`。
- 自检：`checks=1566 failures=0`（+18：特殊心所在行、玩家换行 3 种宽度、默认缩放与 v1/v2/v3 迁移）。

## 1.18.0

- **布局固定三段**：护甲行在最上面、心数行紧贴血条（永远只有一行）、血条、名称。
  以前文字按宽度在 1~2 行之间跳，同一只怪的行数会变；现在只取决于「有没有护甲」。
  `Result.rows` 改为从上到下排列，新增 `Row.isArmourRow()`；`wrapWidth` 配置与设置界面滑块删除。
  截图 `versions/pre-release/v1.18.0/layout.png`。
- **缩放设置生效**：文字从 `submitNameTag` 改为自己 `submitText` 提交（原版把缩放写死 0.025），
  血条同样乘 `scale`。实测 `scale 1.0` → 血条 161 px，`scale 1.5` → 239 px（×1.485）。
- **不透明度对文字生效**：文字颜色改为带 alpha（原版写死不透明），背景框同步变淡。
  实测 `opacity 1.0` → 文字主色 (224,99,100)，`0.5` → (200,118,118)（与背景混合）。
- **血条默认长度 75**：`BAR_WIDTH_DEFAULT` / `barWidth` / `teamBarWidth` 默认都改 75；
  配置新增 `configVersion = 2`，老配置里仍是旧默认 100 的自动迁移到 75，自己调过的不动。
  实测 75 → 161 px、100 → 215 px（比值 1.335 ≈ 100/75）。截图 `versions/pre-release/v1.18.0/scale-opacity.png`。
- 自检：`checks=1548 failures=0`（折行断言换成固定两行断言、默认长度与迁移规则）。

## 1.17.0

- **护甲分页**：`🛡 防御` 常驻，七种元素护甲每页最多 **2 项**、**2 s** 翻一页（5 种 = 2+2+1）。
  新增纯函数 `ArmourCycle.page(armor, nowMs, salt)`，`salt` 用实体 id 错开，避免满屏同时翻。
  `HudLayout` 新增 `ARMOUR_PAGE_SIZE / armourPageCount() / clampArmourPage()` 与 `Options.armourPage`。
  实测逐帧：`🛡5 🔥3 ☃1` → `🛡5 ☠2 ☀4` → `🛡5 ⚓6`（`versions/pre-release/v1.17.0/armour-pages.png`）。
- **大数值缩写**：`999999` 实测会把整行拉出屏幕。新增 `HudLayout.num()`：< 100000 原样，
  再大改 `k / m / b` 且小数截断（`999k`、`1.2m`、`123.4m`、`2.1b`）。心数 / 上限 / 防御 / 元素护甲统一走它。
  实测 `999999 × 5` → 每段 `999k`，整行缩到约 1/3 并折两行（`versions/pre-release/v1.17.0/big-numbers.png`）。
- **着火不再盖住血条**：燃烧是不透明几何（`solid`、写深度），火焰盒子向相机一侧伸出
  `0.7 × boundingBoxWidth`，血条平面穿过盒子中间因而被深度裁掉。现在 `displayFireAnimation`
  为真时把**整组 HUD（文字 + 血条）**沿视线往相机推 `0.7 × 宽 + 0.05` 格（上限 1.2），
  同一条射线屏幕位置不变。实测同一块裁切里血条/文字的红像素 15.79% → 20.81%
  （`versions/pre-release/v1.17.0/fire-before-after.png`）。
- 自检：`checks=1540 failures=0`（+39：分页/翻页时钟/三页布局/`num()` 边界）。
  总数比 1.16.3 的 1595 少，是因为「逐段」断言随护甲段数从 7 降到 2 而减少，**没有删断言**。

## 1.16.3

- **队友只画心与血条，护甲段不再显示**：`HudWorldRenderer.teamOptions()` 里 `showArmor = false`。
  `HudLayout` 的护甲段与 `|` 分隔符都写在 `if (opt.showArmor && hasArmor(...))` 内，
  关掉这一个开关就同时去掉分隔符 + 防御 + 七档元素护甲，不会留下孤零零的 `|`。
- payload 的 `defence / armour` 字段照旧收下（协议与数据结构不动），只是不给玩家那一套布局用；
  敌人仍是 `showArmor = true`，`🛡5 🔥3` 照常显示。
- 实测（同一帧三只）：Bot1 `Defence 5 / FireArmor 3 / IceArmor 1` → 只有 `❤60/150` / `💛40 🖤30 💙20 💔2`；
  Bot2 `Defence 7 / FireArmor 4` → 只有 `❤100/100 💛50`；对照的僵尸 → `❤100/100` / `🛡5 🔥3`。
  截图 `versions/pre-release/v1.16.3/teammates-vs-enemy.png`。
- 自检：`checks=1595 failures=0`（+6：队友无 🛡/🔥/`|`、只剩心、血条仍在、敌人护甲未被误伤）。

## 1.16.2

- **死亡 / 观战（旁观模式）的玩家不再显示头顶 HUD**：CTT 里死掉的玩家就是 `/gamemode spectator`，
  所以一条规则同时管住两种状态。
  - 客户端 `HudTarget.computeVisible()`：`player.isSpectator()` → 这一帧不画（心数行 + 血条）；
  - 服务端 `CttHudForkServer.collect()`：旁观模式的玩家不进 payload。
- 检查放在 `computeVisible()` 而不是 `managed()`：`managed` 还负责摘掉原版 `below_name` 分数，
  旁观者那条 `0%` 同样不该冒出来，所以分数照摘、HUD 不画。
- 客户端游戏模式来自玩家列表（`PlayerInfo.getGameMode()`），原版会同步，**纯客户端也判得准**。
- 实测（Carpet 假人 + 旁观视角）：Bot2 存活 → `❤100/100 💛50` + 满绿条；`gamemode spectator Bot2` →
  半透明模型仍在视野里，但心数行与绿条整块消失，只剩原版名字；改回创造 → 立刻回来。
  截图 `versions/pre-release/v1.16.2/spectator-before-after.png`。
- 自检：`checks=1589 failures=0`（无新增：判定依赖 `Player.isSpectator()`，纯 JVM 测不了，由实机覆盖）。

## 1.16.1

- **队友（玩家）绿条进度改为 红心 ÷ MaxHP**：`EnemyData.fromPayloadPlayer()` 只改比例与配色通道，
  心数行照旧输出全部颜色。数据包的分层进度（蓝 > 黑 > 魂 > 红）放在单色绿条上读不出来：
  `Red 100 + Soul 50 / MaxHP 100` 旧版画 50%（魂心 50/100）、新版画 100%（红心 100/100）。
  敌人不受影响，仍是分层进度。
- **两行文字中间的重叠暗带**：原版文字背景框是 `y - 1 ~ y + 9`（10 像素），行距却用了字体行高 9，
  两行背景重叠 1 像素、两层 25% 黑叠成一条深色横带。新增
  `HudLayout.TEXT_BOX_HEIGHT = 10`，`ROW_STEP` 由 9 改为 10；折行时上面那一行从 -27 变成 -28。
- 实测（Carpet 假人 + 白墙背景，同一机位 A/B）：交界两行亮度 旧 96.3 / 新 128.0（单层背景 128.0）；
  截图 `versions/pre-release/v1.16.1/rows-before-after.png`、`versions/pre-release/v1.16.1/teammates-progress.png`。
- 自检：`checks=1589 failures=0`（+63：行距/背景框不重叠、`fromPayloadPlayer` 的红心口径与边界）。

## 1.16.0

- **队友（玩家）也能显示完整心数**：魂心 / 黑心 / 蓝心 / 碎心 / 粉心，与敌人同一套渲染。
  `CttHudForkServer.collect()` 不再跳过玩家（附近的其他玩家按 UUID 一起推，自己不推）；
  客户端 `resolveData` 对玩家也优先用 payload，拿不到才回退队伍条的 `AllHearts/MaxHP`。
- `teamOptions` 改为 `showHearts/showArmor = true`：没有心数时 `HudLayout` 自动退回 `当前/最大`。
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
- `CttHudForkClient` 不再注册 `HudElement`；`BossBarTexture` 增加世界空间用的纹理 id 与 `argb`。
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
- `HudLayout` 去掉硬编码的 `MIN_ROW_WIDTH=90`，改为 `Options.wrapWidth` + `resolveWrapWidth()`；
  新增 `WRAP_MIN` / `WRAP_MAX` / `WRAP_AUTO` 常量。
- 自检：`checks=1504 failures=0`（+13 条：阈值解析 5 条、阈值 60 折行 3 条、配置归一化 5 条）。

## 1.14.3

- **红心带上限**：红心段渲染成 `❤当前/上限`（上限 = `MaxHP`）；`MaxHP <= 0` 时只写当前值。
- **文字可折成两行**：整行宽度超过 `max(血条长度, MIN_ROW_WIDTH=90)` 时，
  红心单独占上面一行，其余心与护甲留在下面一行（换行代替 `|`）。
  第二行仍贴 `TEXT_TOP=-18`，第一行抬到 `-27`；血条位置不变。
- `HudLayout` 新增 `Row(lines, width, top)` 与 `ROW_STEP` / `MIN_ROW_WIDTH`；
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
- 新增常量 `HudLayout.HEART_GAP = " "`，与 `SEPARATOR = " | "` 区分开；
  顺序与配色不变（红 → 魂 → 黑 → 蓝 → 碎，各用各的颜色）。
- 协议、配置项、界面位置均未改动，可与 1.14.0 服务端混用。
- 自检：`checks=1479 failures=0`（+3 条：组内无分隔符 / 组间有分隔符 / 两色心精确相等）。

## 1.14.0

- **服务端选装组件**：新增 `MobHealthPayload`（S2C `ctt-hud-fork:mob_health`），
  服务端 `CttHudForkServer` 每 4 tick（5 Hz）按玩家周围最近的 32 只怪推
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
