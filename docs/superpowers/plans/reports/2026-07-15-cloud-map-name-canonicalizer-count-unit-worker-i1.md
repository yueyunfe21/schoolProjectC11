# Cloud MapNameCanonicalizer Count Unit Worker I1

## CLAIMED

- `task=W-COUNT-MAP-NAME-CANONICALIZER-WHOLE-1`
- `worker=Internal Count Worker I1`
- `claimedAt=2026-07-15T01:20:38-04:00`
- `countUnit=MapNameCanonicalizer::canonicalize`
- `countDelta=+1`
- 角色：implementation-only，不是 reviewer；本报告不自行加计数。
- 状态：`IMPLEMENTED / PARENT SOURCE REVIEW + UNIFIED FRESH BUILD PENDING`

## 基线与精确缺口

业务权威为 `696a12b0:src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java`。Cloud
迁移前副本与该基线逐字一致，但其字典仍在运行时读取 DHXY 工作目录的
`images/template/map_label/*.png` 和 `config/maps.json`，因此生产 Cloud 不能独立闭合名字权威。

696 的名字集合是两侧并集：61 个 `map_label` 名字 + 31 个 `maps.json` key，重叠 29 个，共 63 个；
其中 `天宫`、`御马监` 是仅存在于 transform 表的名字。本次把这 63 个名字收口到 Cloud 已有的唯一
资源 `src/main/resources/config/maps.json`：保留原 31 个 transform 对象，只以 `null` 增补 32 个
name-only key。`ObjectiveTextRecognizer::loadMapTransforms` 只接收 `value.isObject()`，所以这些
name-only key 不会变成坐标 transform，也不改变既有导航参数。

## Baseline Method Map

| 696 方法/区段 | 本次 Cloud 对应 | 保持内容 |
|---|---|---|
| `canonicalize` | `MapNameCanonicalizer.java:44` | null/blank 返回空串；trim；标点/空白 normalize；exact 优先；edit-distance 排序；安全纠正；ambiguity 返回 trimmed 原文 |
| `isSafeCorrection` | `MapNameCanonicalizer.java:86` | distance=1 直接通过；五倍 tracker 的长度/距离/runner-up 阈值；通用长度/距离/runner-up 阈值全部不变 |
| `knownMapNames` | `MapNameCanonicalizer.java:102` | lazy `AtomicReference` cache 与首次装载日志不变 |
| `loadKnownMapNames` | `MapNameCanonicalizer.java:115` | 仅把 696 的双文件系统来源收口为单一 Cloud classpath resource；不新增 helper/第二资源 |
| `normalizeForMatch` / `MapNameCandidate` | `MapNameCanonicalizer.java:132/139` | 正则、record、排序输入均不变 |

静态逐字符对照结果：`canonicalizeExact=True`，`thresholdExact=True`。

## 文件表

| 文件 | 操作 | 证据 |
|---|---|---|
| `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/MapNameCanonicalizer.java` | 修改 | SHA-256 `6A8E3AAA...` -> `E9A017BE...`；只改 import/Javadoc、单一 resource 常量及既有加载块 |
| `D:/mavenProject/dhxy-cloud-brain/src/main/resources/config/maps.json` | 修改 | SHA-256 `43630524...` -> `FF903924...`；31 个 transform 对象保留，追加 32 个 name-only `null` key |
| `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-15-cloud-map-name-canonicalizer-count-unit-worker-i1.md` | 新增 | 本固定报告 |

冻结 caller 未修改：

- `NavigationService.java` SHA-256 仍为 `F3035A485F05D9C2511E0068C7BE6750FD0B8030C1D1C6F6F9A5ED125210CACA`。
- `TaskTrackerPanelService.java` SHA-256 仍为 `9C02BDE6ADC7E54FABBD5F66D7F3D122781791E44683D6C26FFAA3FECA5D7C77`。

## 完整真实链

### Navigation caller 到 terminal

1. `NavigationRequest` 的目标地图文字与当前 `PlayerCharacter.currentMapName` 进入
   `NavigationService::submitWorldMapSearchAndClickDestination`。
2. `canonicalCurrentMapForWorldMapRouteMemory` / `canonicalMapName` 先做原有 nullable normalize，随后在
   `NavigationService.java:2752` 调用 Cloud `mapNameCanonicalizer.canonicalize(normalized, source)`。
3. canonicalizer 从单一 Cloud classpath `config/maps.json` 取得 63 名字，依 696 exact/fuzzy/ambiguity
   算法返回 canonical 或 trimmed 原文。
4. 原 caller 把 from/target canonical 值交给
   `performWorldMapSearchAndClickDestination` 与 route-memory 记录，不改变输入动作或 fallback。
5. 原分支闭合为 `NavigationResult.mapNotReached(...)`，或
   `NavigationResult.pathingStarted(...)`（yellow-destination / legacy-green-link 原终态）。

### TaskTracker typed map-text 到 terminal

1. `TaskTrackerPanelService::recognizeWubeiGreenMapText` 从当前窗口 tracker crop 得到 typed
   `List<OcrWordResult>`，按原顺序 join/normalize 为 `rawText`。
2. `TaskTrackerPanelService.java:907` 以原 source
   `wubei-tracker-green-map:<safeSource>` 调用同一 Cloud `canonicalize`。
3. canonicalizer exact 命中直接返回；否则按 edit distance、候选长度与 runner-up 排序；五倍专属
   distance=2 分支仍要求 normalized 长度至少 3 且 runner-up 至少多 1；不安全时返回原 OCR 文本。
4. 返回值进入 typed `WubeiGreenMapText`，再进入 `TaskTrackerGreenLink.targetMapName`，最终由原
   `readWubeiTrackerDetail` 构造 closed `TaskTrackerPanelReadResult`；未增加 capture、OCR、输入或 retry。

因此本 countUnit 是两个真实 caller 可达的完整 Cloud 算法链，不是 DTO/helper/resource 单独计数。

## Exact / Fuzzy / Ambiguity 保真

- exact：候选与 raw 使用同一 `normalizeForMatch`；首个 normalized exact 返回 canonical。
- fuzzy：`OcrTextMatcher.editDistance` 升序，距离相同按候选名字长度升序；无候选返 trimmed 原文。
- distance 1：与 696 一样直接安全纠正。
- 五倍 tracker：`length >= 3 && best <= 2 && second >= best + 1`。
- 通用：raw 长度至少 4 时 `maxDistance=2`，否则 1，且 `second >= best + 2`。
- ambiguity：不满足安全条件时保留并返回 trimmed 原文；仅原有 `best <= 2` warning。
- 未改阈值、排序、日志条件、缓存、terminal、caller 或 fallback。

## Scoped Check

- JSON 静态解析：`OK`；顶层 key=`63`，重复顶层 key=`0`。
- transform 对象=`31`；name-only null=`32`；`天宫/御马监` 均在权威资源。
- Cloud 现有 61 个 map-label 名字对资源缺失数=`0`；资源额外仅 `天宫,御马监`；并集=`63`。
- `MapNameCanonicalizer.java` 对 `Files/Path/images/template/map_label/MAP_LABEL_DIR/MAP_CONFIG_PATH`
  scoped grep=`NONE`。
- 资源消费者核对：`ObjectiveTextRecognizer` 仅把 object value 装入 transform map，null key 被忽略；
  `MiniMapPointResolver` 只通过该 transform map 消费，不受 name-only key 影响。
- 按用户禁令未运行 Maven、test、runtime/application/server/Task/poller/UI/capture/input，也未执行 Git
  mutation 或任何 Git 命令。

## 业务差异与计数门禁

`无已批准业务差异；按 696a12b0 基线等价迁移`。唯一变化是把原 63 名字权威从 DHXY 运行时目录依赖
收口到现有 Cloud classpath resource；算法、阈值、歧义、caller 和终态不变。

本 worker 只申报候选 `countDelta=+1`。只有父级源码审查通过，并由父级统一 fresh build 通过后，
`countUnit=MapNameCanonicalizer::canonicalize` 才能真正计数；当前为 `COUNT PENDING REVIEW/BUILD`。

## Parent Source Review #1 - BLOCKED - 2026-07-15T01:36:00-04:00

结论：**P0=0/P1=1/P2=0，BLOCKED / 原 I1 返修。** Java 的 classpath 单一资源加载、canonicalize 算法、
阈值与 31 个 transform 保留可继续使用；但报告的 61 个 baseline map-label 结论错误。

- 精确证据：父级以 `git -c core.quotepath=false ls-tree -r --name-only 696a12b0 --
  images/template/map_label` 读取业务基线，实际是 **60** 个 label；与 31 个 config key 的并集是 **62**。
- 当前 dirty DHXY 树额外存在基线没有的 `images/template/map_label/修罗古城.png`；I1 用当前目录而不是
  `696a12b0` tree 构造资源，把 `"修罗古城": null` 写入 Cloud，使资源变成 63 个名字。
- 影响：`canonicalize("修罗古城", ...)` 会从基线的原文/模糊判定变成 exact canonical，属于未批准业务差异；
  不能把用户的 dirty/untracked 模板当作迁云业务权威。

返修条件：只修改 Cloud `src/main/resources/config/maps.json` 与本报告，删除唯一额外
`"修罗古城": null`，确认 62 key / 31 object / 31 null，且相对 baseline union missing=0/extra=0；Java、caller、
其它资源全部冻结。不运行 build/test/runtime/Git。返修后交 Repair #1，父级复审 + fresh build 同轮才 `+1`。

## Implementation Repair #1 - 2026-07-15T01:36:54-04:00

已按 Parent Source Review #1 对业务基线集合做单点修复：仅从 Cloud 唯一名字资源
`src/main/resources/config/maps.json` 删除非 `696a12b0` 基线 key `"修罗古城": null`。未改任何
transform 对象、Java、caller、其它资源、算法、阈值、排序、歧义返回或 terminal。

### 只读基线证据

- 使用父级指定的只读命令
  `git -c core.quotepath=false ls-tree -r --name-only 696a12b0 -- images/template/map_label`
  枚举基线 label：`baselineLabels=60`。
- 只读解析 `696a12b0:config/maps.json`：`baselineConfig=31`。
- 两者按名字去重：`baselineUnion=62`；`修罗古城` 不在该 union。
- 修复前资源：`currentKeys=63`、相对 baseline union `missing=0/extra=1`，唯一 extra=`修罗古城`。

### Repair 后 scoped check

- Cloud resource JSON 解析=`OK`。
- `currentKeys=62`、`object=31`、`null=31`。
- 修复后 resource SHA-256=`14960C71549BFC999012C3C7592BF9880985646374C2987252F8B220C6F7D7FA`。
- 相对 `696a12b0` union：`missing=0`、`extra=0`。
- 顶层重复 key=`0`；`修罗古城` key grep=`0`。
- `MapNameCanonicalizer.java` SHA-256 仍为
  `E9A017BEF8B09B5C985E743E6BAC8C7877CB2BB9A8BCA4F8B6B6BDC12FC88A68`，Java 冻结。
- `NavigationService.java` SHA-256 仍为
  `F3035A485F05D9C2511E0068C7BE6750FD0B8030C1D1C6F6F9A5ED125210CACA`，caller 冻结。
- `TaskTrackerPanelService.java` SHA-256 仍为
  `9C02BDE6ADC7E54FABBD5F66D7F3D122781791E44683D6C26FFAA3FECA5D7C77`，caller 冻结。
- 未运行 build/Maven/test/runtime/application/server/Task/poller/UI/capture/input；仅执行上述父级指定的
  read-only `git ls-tree`/`git show` 基线核验，无 Git mutation。

返修后结论：`无已批准业务差异；按 696a12b0 基线等价迁移`。本 worker 不自行计数；
`countUnit=MapNameCanonicalizer::canonicalize`、`countDelta=+1` 继续等待父级源码复审与统一 fresh build。

## Parent Source Review #2 - APPROVED - 2026-07-15T01:42:00-04:00

父级独立复核 Repair #1：Cloud classpath authority 现为 `62 keys / 31 transform objects / 31 name-only nulls`，
相对 `696a12b0` 的 60 个 map-label 与 31 个 config key 去重并集 `missing=0 / extra=0`；非基线
`修罗古城` 已删除。Java 的 exact/fuzzy/edit-distance/runner-up ambiguity、五倍两字符距离特例、caller 与 closed result
均未改。结论：**P0=0/P1=0/P2=0，SOURCE APPROVED / COUNT PENDING BUILD。**

`countUnit=MapNameCanonicalizer::canonicalize` 仅在父级 fresh Cloud package 通过同轮原子 `+1`；ledger 暂不动。
无已批准业务差异；按 `696a12b0` 等价迁移。
