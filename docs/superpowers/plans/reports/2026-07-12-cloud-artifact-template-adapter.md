# CR271 Cloud Artifact/Template Adapter - Worker B Log

> Append-only collaboration log. Worker B owns implementation/repair material in this file. The parent agent owns review verdicts. Do not edit or delete prior entries.

## Parent Task Brief #1 - 2026-07-12

### Worker identity and current phase

- You are **Worker B**, an implementation worker, not a reviewer. Do not review Worker A and do not create agents/reviewers.
- Work independently from Worker A. Worker A currently owns the cross-repository resume-reconcile/current-revision confirmation slice.
- Your fixed collaboration log is this file. Re-read it every five minutes. Append only when you have new design, repair, implementation, or build evidence; do not repeat unchanged status.
- **Current phase is design only.** Append `## External Worker B - Design #1 - 2026-07-12`, then stop for the parent agent's explicit `DESIGN APPROVED` or `BLOCKED`. Do not modify Java, Maven, resources, tests, or other process documents before design approval.

### Objective

Design the smallest Cloud-native, tenant-isolated artifact/template adapter that removes arbitrary filesystem `Path` authority from migrated Services and unblocks these three currently gated call paths without changing their business decisions:

1. `DialogService.cloudWashToPath`
2. `TaskTrackerPanelService.washYellowToPath`
3. `CoordinateHelper.findGreenTextInRegion`

The target flow is in-memory capture/image processing plus opaque scoped artifact/template identifiers. DHXY-local absolute/relative paths, HWND/runtime objects, temp directories, and raw filesystem access must never become Cloud authority.

### Required reads and evidence

Before writing Design #1, read:

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` top CR271 material
- `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-service-migration-matrix.md`
- `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-12-cloud-native-image-processor-service.md`
- Cloud: `host/CloudServiceScope.java`, `host/CloudServiceStorage.java`, `host/CloudServiceConfiguration.java`, `CloudNativeImageProcessor.java`, `ImageProcessorService.java`, all existing template/resource loader owners and the three future caller dependencies.
- DHXY read-only: the exact methods and callers for the three gated paths above, including template-match/image-find fallback order and debug-output behavior.
- Both repositories' latest `git status`; protect every existing dirty/untracked change.

Design #1 must cite concrete file/method evidence and include a complete proposed write set. If any proposed file overlaps Worker A's current files, redesign or report the overlap as a blocker; do not touch it.

### Design invariants

1. **No raw Path boundary:** no public/migrated Service API may accept a DHXY path, arbitrary Cloud path, path fragment, or caller-chosen filename. Never trust `RequestMetadata.rawImagePath` as authority.
2. **Two authorities remain distinct:** immutable packaged templates use a canonical allowlisted classpath resource ID; mutable debug/intermediate artifacts use an opaque ID scoped to authenticated `CloudServiceScope` and, where lifecycle-sensitive, exact `taskRunId`/revision ownership. Do not create a second loader for an already single-owned resource.
3. **Containment:** all mutable files resolve below the hashed tenant/user storage root. Reject traversal, separators, absolute paths, symlink/reparse escape, unknown IDs, cross-tenant IDs, and type/extension mismatch before I/O.
4. **Atomicity and capacity:** specify bounded bytes/pixels/count, deterministic encoding, atomic replace/write, collision behavior, cleanup/retention owner, restart behavior, concurrent access, and failure semantics. No unbounded in-memory or disk growth.
5. **Image ownership:** state who owns and flushes each `BufferedImage`; no double flush, use-after-flush, silent mutable sharing, or stale output reuse.
6. **Behavioral equivalence:** preserve the latest pushed business baseline's image preprocessing, template selection/order, thresholds, fallback order, and click/navigation semantics. Artifact persistence is diagnostics/plumbing, never new business truth.
7. **No bypass:** do not expose raw filesystem, raw resource enumeration, arbitrary template lookup, upload/download HTTP, poll/completion, or outcome-injection APIs to Services/host.
8. **Dormant integration:** do not migrate/activate the three callers yet and do not start host/Task/poller/UI/capture/OCR/input. Show how each caller will later switch from `washToPath` to in-memory processing plus the adapter.
9. **No A overlap:** do not modify `remote/`, `api/RemoteTaskRunEndpoint.java`, DHXY `cloud/remote/`, lifecycle, registry, ledger, poller, transport DTO/digest/error-code files, or any file listed in Worker A's current implementation section.
10. **Validation:** after design approval and implementation only, run Cloud `mvn -q clean package` without skipping tests. Touching DHXY Java is not expected for this slice; if design proves it unavoidable, stop and request a new approved write set before editing.

### Design #1 required sections

1. Current three call chains and the exact filesystem/template authority being removed.
2. Existing Cloud storage/resource owners and reuse-vs-new ownership decision.
3. Proposed typed API/value objects, visibility, package ownership, and exact write set.
4. Opaque ID format and authorization/binding rules without leaking paths.
5. Template lookup contract, allowlist, deterministic ordering, missing/corrupt behavior.
6. Mutable artifact write/read/delete lifecycle, atomicity, quotas, cleanup, restart, concurrency, and symlink/reparse defense.
7. Image ownership/flush matrix.
8. Per-caller future migration mapping preserving baseline semantics.
9. Threat matrix: cross-tenant access, forged IDs, traversal, stale revision, overwrite, resource exhaustion, corrupt image, restart, and operational observability.
10. Alternatives considered, recommendation, unresolved P0/P1/P2, and implementation/build plan.

### Stop condition

After appending complete Design #1, do not implement. Wait for the parent agent's explicit verdict in this file. Worker self-review is QA only and never counts as approval.

`无已批准业务差异；按基线等价迁移。`

## External Worker B - Design #1 - 2026-07-12

### 基线与在途保护证据

- DHXY：分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；Cloud：分支
  `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`。两仓 `git status --porcelain`
  已完整读取；全部既有 dirty/untracked 改动（含 Worker A 的 `remote/`、`api/`、DHXY `cloud/remote/` untracked
  目录）本轮零触碰。本轮为纯设计+源码调查，未修改任何 Java/Maven/resources/tests/其它文档。
- 必读完成：`AGENTS.md`（含 2A 业务基线门）、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` CR271 各基线段、
  `specs/2026-07-12-service-migration-matrix.md`（`DialogService`/`TaskTrackerPanelService` 行与 L1516/L1524 的
  adapter gate 记录）、`reports/2026-07-12-cloud-native-image-processor-service.md` 全文（含 Local Design Review
  #1/#2 拍板与 Implementation Review #1 CLOSED）、Cloud `host/` 四件套、`CloudNativeImageProcessor.java`、
  `com.bot.dhxy.cloud.task.ImageProcessorService.java`、`TemplateMatcher.java`、
  `CloudBrainTemplateResourceGuardTest.java`、云端已迁 `com.bot.dhxy.core.ImageFinder.java`，以及 DHXY 三条
  调用链的全部现场方法（见下）。

### 1. 三条调用链现状与被移除的文件系统/模板权威

**链 A：`DialogService.cloudWashToPath`（DialogService.java:1167-1187）**，三处调用分两种角色：

- **业务链**（verify white template，L685-708）：`detection.rawPath()` 缺失时把 `detection.image()` 存到
  `windowScopedTempPath.resolve("dialog_white_template_raw.png")`（L688-689）→ `cloudWashToPath(...WASH_WHITE)`
  写 `dialog_white_template_washed.png`（L697-699）→ `ImageFinder.find(washedPath, spec.templatePath(), 0.85)`
  （L708，path 版模板匹配）→ 命中经 `coordinateHelper.resolveMatchedPointInRect(rect, result)`（L713）产出
  `WHITE_TEMPLATE_VISIBLE` + 点击坐标。**washed 文件是 wash→模板匹配的文件中转媒介 + 诊断证据。**
- **纯 debug 链**：`hasOptionInLowerHalf`（L1547-1552，WASH_GREEN 落 debug PNG；真实判定走内存
  `countGreenPixelsHSV` L1553，阈值 `greenCount > 150` L1565）；`hasStoryInUpperHalf`（L1600-1608，WASH_WHITE +
  WASH_GREEN 两个 debug PNG；判定走内存 count/line-pattern L1609-1614）。washed 文件只进日志，不进决策。
- **被移除的权威**：`windowScopedTempPath` 任意相对路径的写/读、`spec.templatePath()` DHXY 相对路径字符串直接
  作为 `ImageFinder` 的磁盘模板路径、`Path.of(caller 字符串)`。

**链 B：`TaskTrackerPanelService.washYellowToPath`（L1595-1615）**，两处调用：

- L717（`readXiuluoTrackerPanelForReplay`）：入参 `panelRawPath` 为磁盘文件（L709 `Files.isRegularFile` 前验），
  `yellowPath = panelRawPath.resolveSibling(fileName + "." + safeSource + ".xiuluo-title-yellow.png")`（L715-716，
  **caller 拼接的任意兄弟路径**）→ wash YELLOW 落盘 → `findTitlePointInPanelImage(source, panelRawPath, yellowPath,
  ...)`（L722）内 `ImageFinder.find(matchPath, template.getTemplatePath(), template.getThreshold())`（L1565，
  模板列表迭代顺序 = caller 传入 List 顺序）→ 标题锚点 → 裁剪细读；`markedOutputPath` 为可选证据 PNG。
- L1546：同构（`task-tracker-title-yellow`）。
- 失败清理：`Files.deleteIfExists(outputPath)`（L1605-1611，stale washed 输出删除，best-effort）。
- **被移除的权威**：`resolveSibling` 任意路径写、`Files.deleteIfExists` 任意路径删、模板路径字符串直读磁盘。

**链 C：`CoordinateHelper.findGreenTextInRegion`（CoordinateHelper.java:293）**：`tracker.captureToFile` →
`windowScopedTempPath` raw PNG → `imageProcessorService.washToPath(...WASH_GREEN)`（L301）→ washed PNG →
`ImageFinder.find(washedScanPath, templatePath, matchRate)` → 命中点。**grep 证据：该方法当前全仓（DHXY src）
零调用方，仅存定义**；仍按 brief 列为 gated path 设计其未来形态。

**Cloud gate 现状**：`CloudNativeImageProcessor.washToPath`（L49-56）按前置切片父级拍板固定
`REQUIRED_FAILURE`，reason 即指向本 adapter：`use in-memory image plus tenant-scoped artifact adapter`。

### 2. 现有 Cloud 存储/资源属主与复用-新建决策

| 既有属主 | 证据 | 本设计决策 |
|---|---|---|
| `host/CloudServiceScope` + `host/CloudServiceStorage` | Storage 以 SHA-256(length-prefixed tenant/user) 为 scope 根；`resolvePrivateFile(fileName)` 只放行单段、无分隔符/穿越/绝对路径的固定文件名并做包含性终检（L50-79）；自身零 I/O | **复用不改**。artifact 文件名全部由 adapter 内部生成为单段 token 名，恰好通过既有校验；不给 Storage 加子目录/多文件能力，不扩大既有安全面 |
| `TemplateMatcher.readResourceImage(...)`（cloudbrain 包，package-private）+ `CloudBrainTemplateResourceGuardTest` | guard test 强制业务识别器只用 `TemplateMatcher.readResourceImage(` 读 `images/template/...`，禁 `readImage(Path.of(` | **复用为唯一物理读取路径**。模板 adapter 实现类放 `com.yueyunfe.dhxy.cloudbrain` 同包，委托该方法；不建第二个 classpath/PNG loader（brief 不变量 2"单一属主"逐字满足） |
| `CloudNativeImageProcessor` / `ImageAlgorithms` | 前置切片 CLOSED；in-memory wash 全集可用 | 零改动。三链迁移后 wash 全走既有 in-memory 方法 |
| 云端已迁 `com.bot.dhxy.core.ImageFinder` | **已有 in-memory 重载** `find(BufferedImage, BufferedImage, double)`（L53-79），与 path 版同一 `TM_CCOEFF_NORMED` + 中心点数学；`bufferedImageToMat` 走 TYPE_3BYTE_BGR，与 `Imgcodecs.imread` 同为 3 通道 BGR | 零改动。未来 caller 模板匹配全走内存版，本 adapter 不新增任何匹配实现 |
| `host/CloudServiceConfiguration` | 显式 bean 先例（DialogChoiceMemoryService 注入 scoped Path；ImageProcessorService 注入 CloudNative） | 唯一装配点，新增两个显式 dormant bean |

### 3. 拟新增 typed API、可见性、包属主与完整写集

**写集 = Cloud 4 new + 1 modify，DHXY 零改动**（与 Worker A 批准写集——Cloud `remote/**`、`remote/run/**`、
`api/RemoteTaskRunEndpoint.java`、`remote/RemoteTaskRunErrorCode.java`、`remote/CloudTaskRunExecutionGate.java`、
DHXY `cloud/remote/**`——**零交集**）：

| # | 文件 | New/Modify | 内容与可见性 |
|---|---|---|---|
| B1 | `host/CloudArtifactStore.java` | New | `public interface`；方法 `Optional<ArtifactId> writePng(String diagnosticTag, String ownerTaskRunId, BufferedImage image)`、`Optional<BufferedImage> readPng(ArtifactId id, String expectedOwnerTaskRunId)`、`boolean delete(ArtifactId id)`；nested `record ArtifactId(String token)`（compact constructor 做 `^af1-[0-9a-f]{32}$` 先验）。无枚举/列表/路径/流式 API |
| B2 | `host/ScopedPngArtifactStore.java` | New | package-private `final`，实现 B1；构造注入 `CloudServiceStorage`；全部 I/O、配额、原子写、清理在此 |
| B3 | `host/CloudTemplateAssets.java` | New | `public interface`；方法 `Optional<BufferedImage> loadTemplate(TemplateId id)`；nested `record TemplateId(String resourcePath)`（compact constructor 做语法+allowlist 先验）。无枚举 API |
| B4 | `com/yueyunfe/dhxy/cloudbrain/PackagedTemplateAssets.java` | New | `public final`（bean factory 在 host 子包，跨包需 public——与 `CloudNativeImageProcessor` 同理由），实现 B3；同包委托 `TemplateMatcher.readResourceImage(...)`，自身零 classpath/文件解析 |
| B5 | `host/CloudServiceConfiguration.java` | Modify（+8..14 行） | 2 imports + `@Bean CloudArtifactStore artifactStore(CloudServiceStorage storage)` + `@Bean CloudTemplateAssets templateAssets()`；不扩大 `@ComponentScan`，不动既有 bean |

接口放 `host` 包（cloud-native 边界，与 Scope/Storage 同居），**不放** `com.bot.dhxy.*`：镜像包只承载
lift-and-shift 副本，云端专属契约进镜像包会污染后续逐字迁移比对。

### 4. Opaque ID 格式与授权/绑定规则（不泄漏路径）

- **ArtifactId**：`af1-<32 个小写 hex>`（128-bit `SecureRandom`）。token 即文件名主干（`<token>.png`），完全由
  adapter 内部生成；`diagnosticTag`/caller 输入**永不进入**文件名。ID 文本不含 tenant/user/path/hwnd 任何成分。
- **授权边界**：ID 只能经"持有它的那个 tenant host 的 `CloudArtifactStore` bean"解析；物理根 =该 host 认证
  `CloudServiceScope` 的哈希目录。跨租户 ID 在本 scope 根下物理不存在 → 与"未知 ID"同响应（`empty`/`false`），
  不区分"不存在/越权"，避免存在性探测。
- **lifecycle 绑定**：`writePng` 记录 `ownerTaskRunId`（内存元数据，不落盘、不进文件名）；`readPng` 携带
  `expectedOwnerTaskRunId`，不匹配或元数据缺失（如重启后）→ `empty` fail-closed。诊断 artifact 不承载业务真值
  （不变量 6），owner 门只防陈旧复用，不新增业务语义。
- **TemplateId**：即 canonical classpath 相对资源路径（如 `images/template/dialog/xxx.png`），见 §5 校验。它是
  受限白名单资源坐标，不是文件系统权威；实现绝不将其 `Path.of`/`File` 化。

### 5. 模板查找合同

- **校验链（全部在任何 I/O 之前，任一失败 → `Optional.empty()` + 单行 warn）**：非空且 `trim` 等于原文；不含
  `\`、`..`、`:`、NUL、前导 `/`；以 `.png` 结尾；仅允许 `[A-Za-z0-9_\-./一-鿿]`；必须以唯一 allowlist 前缀
  `images/template/` 开头（三链全部模板实测均在该树下；云端 resources 已打包同树）。
- **加载**：命中校验后委托 `TemplateMatcher.readResourceImage(resourcePath)`——与 guard test 对业务识别器的强制
  纪律同一物理路径；打包/exploded 两形态由该属主既有逻辑覆盖。缺失资源/解码失败 → `empty` + warn（与基线
  `Imgcodecs.imread` empty → `find` 返回 null 的 fail-closed 语义对齐，caller 侧"未命中"分支照旧走）。
- **确定性排序**：本 API 只有单 ID 精确查找，**没有任何枚举/通配/列表语义**（也是不变量 7 的"无 raw resource
  enumeration"要求），故不存在排序面；模板尝试顺序由 caller 既有代码的 List 顺序决定（如
  `findTitlePointInPanelImage` 的模板迭代 L1560-1565），本 adapter 不改变。
- **所有权**：每次调用新解码并返回新 `BufferedImage`，调用方独占并负责 flush；**无缓存**（杜绝可变共享；若未来
  热路径实测需要缓存，另开切片并给出防共享方案，见 P2-2）。

### 6. 可变 artifact 生命周期

- **写（原子）**：入参校验（image 非空；`width,height ≤ 4096` 且 `width*height ≤ 8_388_608`）→ 内存 PNG 编码
  （`ImageIO.write` 到 `ByteArrayOutputStream`；bytes `≤ 8 MiB`，超限拒绝）→ 首次使用时 `createDirectories`(scope
  根，幂等；若根已存在但 `toRealPath` 不以 stateRoot 实路径开头 → fail-closed 拒绝整个 store)→
  `resolvePrivateFile("<token>.png.tmp")` 以 `CREATE_NEW` 写入 → 同目录 `ATOMIC_MOVE` 改名为
  `resolvePrivateFile("<token>.png")`。任何一步失败 → 清理 tmp → `empty`。
- **碰撞**：128-bit 随机 token 理论零碰撞；`CREATE_NEW` 兜底——命中即重生成一次 token，再失败 → `empty`。
  没有任何 REPLACE 语义；"替换"由 caller 显式"写新 ID + delete 旧 ID"组成（对应链 B stale-cleanup 的迁移形态）。
- **读**：ID 正则先验 → owner 门（§4）→ `resolvePrivateFile` → `Files.isRegularFile(path, NOFOLLOW_LINKS)` →
  大小 `≤ 8 MiB` → `ImageIO.read`；decode null/异常 → `empty` + warn。
- **删**：ID 正则先验 → `resolvePrivateFile` → `deleteIfExists`（NOFOLLOW 语义下只删普通文件：先
  `isRegularFile(NOFOLLOW_LINKS)`，非普通文件不删并 warn）。
- **配额与清理属主**：per-scope 活 artifact 上限 **512**；达到上限时写入前按内存 FIFO（createdAt 序）删除最旧者。
  retention owner = store 自身；**无 TTL、无后台线程**（FIFO 是纯容量机制，不引入时间性业务差异，符合 AGENTS 2A
  "不得因看似更安全而加 TTL"）。
- **重启行为**：内存索引（FIFO + owner 元数据）为空 → 首次操作时扫描 scope 根下 `af1-*.png` 按 lastModified 重建
  FIFO/计数并删除残留 `*.png.tmp`；owner 元数据不可恢复 → 带 owner 期望的读取一律 `empty`（fail-closed，陈旧
  快照不会复活为输入）。
- **并发**：store 实例内单锁串行化写/读/删与索引维护（每 tenant host 一个实例，跨 host 天然隔离）；锁内不做
  PNG 编码（编码在锁外完成，锁只覆盖文件系统与索引操作），避免大图长时间占锁。
- **symlink/reparse 防御**：文件名单段 + `resolvePrivateFile` 包含性终检（既有）；写用 `CREATE_NEW`（不跟随既有
  reparse 点）；读/删前 `NOFOLLOW_LINKS` 的 `isRegularFile` 门；scope 根 `toRealPath` 前缀校验（上文）。

### 7. 图像所有权/flush 矩阵

| 环节 | 输入 image | 输出 image | flush 责任 |
|---|---|---|---|
| `writePng` | caller 所有，store 只读编码，**绝不 flush/缓存/修改** | 无（返回 ID） | caller 用完自己的输入自行 flush（一次） |
| `readPng` | 无 | 新解码对象，所有权交 caller | caller 恰好一次 flush；store 不留引用 |
| `loadTemplate` | 无 | 新解码对象，所有权交 caller | caller 恰好一次 flush；无缓存 → 无共享 |
| 迁移后 wash（既有合同） | caller 所有，processor 不 flush 输入 | 成功输出归 caller；失败未交付输出由 processor flush（前置切片已固化） | caller 对交付输出恰好一次 flush |
| `ImageFinder.find(BufferedImage,BufferedImage,...)` | 两个入参均不被 flush（仅内部 Mat release，L53-79） | 无 | 不新增责任 |

无双 flush：store/assets 从不 flush 交付对象；无 use-after-flush：store/assets 不缓存任何交付过的引用；无
stale 输出复用：owner 门 + 重启 fail-closed（§6）。

### 8. 逐 caller 未来迁移映射（本切片不实施、不激活，dormant 说明）

| caller | 现状（path 链） | 迁移后（in-memory + adapter） | 基线语义保持点 |
|---|---|---|---|
| `DialogService` verify-white（业务） | saveImage → washToPath(WASH_WHITE) → `find(washedPath, spec.templatePath(), 0.85)` → resolveMatchedPointInRect | `detection.image()`（内存）→ 既有 `washThinWhiteTextToBlackAndWhite` → `find(washedImage, templateAssets.loadTemplate(new TemplateId(spec.templatePath())).orElse(null), 0.85)`（null → 未命中分支）→ 同一坐标换算；可选 `writePng("dialog-white-washed", taskRunId, washed)` 保诊断 | 阈值 0.85、spec 迭代顺序、wash 失败 → `WHITE_TEMPLATE_NOT_FOUND`、日志字段逐字保留；`spec.templatePath()` 字符串原值即 TemplateId，无重命名 |
| `DialogService` debug 两处 | wash 落 debug PNG，决策走内存 count | 既有 in-memory wash + 可选 `writePng`；决策路径零变化 | `greenCount > 150` 等阈值、日志语义不变；debug 落盘失败绝不影响决策（写失败仅 warn） |
| `TaskTrackerPanelService` | washToPath(WASH_YELLOW) 落盘 → `find(yellowPath, titleTemplate.path, threshold)`；失败 `deleteIfExists`；`markedOutputPath` 证据 | 既有 `washYellowText`（内存）→ `find(yellowImage, loadTemplate(titleTemplate ID), threshold)` 按同一模板 List 顺序；stale-cleanup 语义 = "写新删旧 ID"；marked 证据 → `writePng`；replay 入口 `panelRawPath` → `readPng(artifactId, taskRunId)` | 各模板阈值/顺序、title→crop 几何换算、miss 分支与 shadow 上报顺序不变。**依赖披露：replay 的 artifact 生产端（capture 侧写入 panel artifact）属未来 capture 切片，见 P2-1** |
| `CoordinateHelper.findGreenTextInRegion` | capture → washToPath(WASH_GREEN) → `find(washedPath, templatePath, rate)`；**当前零调用方** | 未来若复活：内存 capture → 既有 `washGreenTextToBlackAndWhite` → `find(washedImage, loadTemplate(id), rate)`；无任何文件中转 | 零调用方 = 无现行为可破坏；迁移时按此形态直写 |

三 caller 在本切片**不迁移、不激活**；cohort gate（前置切片 Local Design Review #1 拍板）继续有效。host 仍
dormant，不接 `CloudBrainServer` routes，不启动 host/poller/UI/capture/OCR/input。

### 9. 威胁矩阵

| 威胁 | 防御 |
|---|---|
| 跨租户访问 | ID 仅经本 host 的 scope-bound store 解析；根 = SHA-256(scope)；他租户 ID 物理不存在 → 与未知 ID 同响应，无存在性泄漏 |
| 伪造/猜测 ID | 128-bit SecureRandom；正则先验；文件名全内部生成，caller 文本零进入 |
| 路径穿越/绝对路径/分隔符 | 双层：ArtifactId/TemplateId compact-constructor 语法门 + `CloudServiceStorage.resolvePrivateFile` 既有单段/包含性终检；模板侧另有 allowlist 前缀门 |
| symlink/reparse 逃逸 | 写 `CREATE_NEW`、读/删 `NOFOLLOW_LINKS` `isRegularFile` 门、scope 根 `toRealPath` 前缀校验 |
| stale revision/陈旧复用 | owner `taskRunId` 期望读取；重启后元数据缺失 → fail-closed empty |
| 覆盖/篡改 | 无 REPLACE 语义；token 唯一 + `CREATE_NEW`；替换=显式新写+删旧 |
| 资源耗尽 | 4096×4096 / 8M 像素 / 8 MiB 三重上限 + per-scope 512 FIFO + 编码在锁外的单锁串行 |
| corrupt image | 写侧内存编码失败拒绝；读侧 decode null/异常 → empty + warn；模板同 |
| 重启 | 索引 lazy 重建 + tmp 清扫 + owner fail-closed（§6） |
| 可观测性 | 单行结构化日志：`event/tag(消毒后)/tokenPrefix(8hex)/bytes/WxH/status/reason`；绝不记录完整路径、tenant 明文、hwnd、图像内容 |

### 10. 备选方案、建议、未决 P 项与实施/构建计划

- **备选 1**：扩展 `CloudServiceStorage` 支持子目录/多文件族 —— 拒绝：扩大已审安全面；单段 token 名在既有
  `resolvePrivateFile` 合同内即满足需求。
- **备选 2**：模板 adapter 放 host 包自行读 classpath —— 拒绝：形成 `images/template/**` 第二 loader，违反
  guard test 纪律与 brief 不变量 2；同包委托 `TemplateMatcher.readResourceImage` 是唯一不增属主的路径。
- **备选 3**：artifact 纯内存 `Map<ID, byte[]>` 不落盘 —— 拒绝：基线 debug 文件的价值正是磁盘可人工取证；纯内存
  在重启/进程外检查下失效，等于静默削弱基线诊断能力（业务等价性口径下不可接受）。
- **建议**：按 §3 写集实施（Cloud 4 new + 1 modify，DHXY 零改动）。
- **未决项**：`P0=0；P1=0；P2=2` ——
  - **P2-1**：链 B replay 形态完全迁移依赖"capture 侧把 panel 写成 artifact"的未来 capture 切片；本 adapter 的
    `readPng` 合同已为其就绪，依赖顺序需父级在 caller 迁移排期时确认。
  - **P2-2**：模板无缓存导致每次调用重解码（模板为小 PNG，预估可接受）；若未来热路径实测超标，另开缓存切片并
    解决可变共享，不在本切片夹带。
- **实施/构建计划（仅在 `DESIGN APPROVED` 后）**：按写集实施 → Cloud `mvn -q clean package`（不带任何 skip
  flag）→ 追加 `## External Worker B - Implementation #1`：精确 diff/行数/SHA-256、javap 可见性证据、
  filesystem/raw-API 扫描（B1-B4 对 `getResources|Path.of(caller|toFile|URL` 等的零命中面）、Surefire
  suite/test/failure/error/skip 计数、`src/test`/pom/resources/DHXY 零 diff 证据、无启动/无 Git mutation 声明。
  实施前重新核对写集文件与 Worker A 在途改动的零交集。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅为 QA，不构成批准。Design #1 到此停止，等待父级在本文件
写入 `DESIGN APPROVED` 或 `BLOCKED`。

## Local Design Review #1 - BLOCKED - 2026-07-12

- 认可方向：三条 Path 链盘点完整；in-memory `ImageProcessorService` + `ImageFinder`、唯一
  `TemplateMatcher.readResourceImage` loader、无枚举模板 API、caller 顺序/阈值/fallback 保持、Cloud-only 写集与
  Worker A 零交集均成立。B 自报的 capture-producer 后置是明确 cohort 依赖，不是本 adapter 缺陷；模板暂不缓存是
  有界小 PNG 的明确取舍，也不作为 P2。
- 当前仍有 **`P0=0 / P1=3 / P2=0`，BLOCKED**。Java/Maven/resources/tests 继续冻结；Worker B 只追加
  `External Worker B - Design Repair #1`，一次性关闭下列三项。

### P1-1：artifact API 接受可伪造 owner 字符串，且 delete 无 owner/revision/current-context 门

- 证据：Design #1 §3 拟定 `writePng(String diagnosticTag, String ownerTaskRunId, ...)`、
  `readPng(ArtifactId, String expectedOwnerTaskRunId)`、`delete(ArtifactId)`；§4 只把 taskRunId 放在进程内 metadata。
  任意同 tenant host Service 可自行传 taskRunId，旧 revision 仍可写/读；delete 连 taskRunId 都不要求。Cloud 已有
  non-mintable `TaskExecutionContext`，公开 exact scope/taskRunId/runRevision 与 `revalidate()`，本设计却未使用。
- 影响：pause/resume 后的 stale stack 可继续生成/读取诊断 artifact，持有旧 token 的同租户 run 可删除新 run artifact；
  当 artifact 进入 tracker replay 等业务输入链时，旧 revision 数据可能被当成当前事实。
- 返修条件：write/read/delete 全部接收 exact `TaskExecutionContext`（或等价 non-mintable current-context capability），
  I/O 前调用 `revalidate()` 并要求 current confirmed ACTIVE；核对 context tenant/user 与本 store scope；metadata 精确记录并
  比对 `(taskRunId,runRevision)`，delete 同样受门控。不得新增 public raw owner/string mint 或修改 Worker A 文件。

### P1-2：容量只按单 host/scope 限 512，磁盘总量和并发编码内存仍无界

- 证据：Design #1 §6 的 512 项/8MiB 上限仅属于一个 store；当前 `CloudServiceHost.create(scope,stateRoot)` 每次可创建
  独立 context/store，仓内没有 host registry/global capacity owner，历史 scope 目录也不会因 host close 自动清除。
  同节又明确 PNG 编码在 store lock 外，多个 host/线程可同时编码 4096x4096 图像。
- 影响：不同 tenant/scope 或反复创建/关闭 host 可持续累积磁盘；并发编码可同时占用大量 raw+PNG 内存，违反 brief 的
  “无不受控内存/磁盘增长”硬边界。
- 返修条件：设计 root-wide 的共享磁盘预算与并发编码许可（明确唯一 owner、全局 bytes/count、跨 scope 公平性、
  crash 后重建上限和 fail-closed 行为），同时保留 per-scope 配额。不得用 TTL/后台线程；可用同步写入时的有界回收/
  admission。写集必须包含实现该共享 owner 所需的精确文件。

### P1-3：声称的 real-path containment 在当前 4-new+1-modify 写集中不可实现

- 证据：Design #1 §6/§9 要求 scope root `toRealPath` 必须位于 stateRoot real path 下；但
  `CloudServiceStorage.stateRoot/scopeRoot` 均为 private，仅公开 `resolvePrivateFile(...)`，B2 无法取得可信 stateRoot
  锚点进行 real-path 对比。写集又声明 `CloudServiceStorage` 复用不改。
- 影响：实现只能省略这道门或从 candidate parent 反推不完整锚点，现有文档承诺与可实现代码不一致；Windows junction/
  reparse 替换场景没有真实父根证明。
- 返修条件：明确扩展 `CloudServiceStorage` 的 package-private 安全能力或引入同等可信 root-bound collaborator，使
  `ScopedPngArtifactStore` 在任何 create/read/delete 前能比较真实 scope root 与真实 state root，并写清 TOCTOU/Windows
  reparse 的 fail-closed 边界；更新完整写集。不得向业务 Service 暴露 Path/root。

### Repair 验收门

1. 重列 typed API、唯一 authority assembly/bean 构造链、完整写集与锁序；证明仍与 Worker A/C/D 文件零交集。
2. 更新容量/重启/并发/owner/删除/路径威胁矩阵及三 caller 映射；capture producer 继续作为 caller cohort 前置门，
   无缓存继续作为接受的性能取舍，不再标开放 P2。
3. Design Repair #1 完成后停止，等待父级 `DESIGN APPROVED`；批准前不改 Java/Maven/resources/tests。

`无已批准业务差异；按基线等价迁移。`

## External Worker B - Design Repair #1 - 2026-07-12

### 返修结论

- 已逐条接受 `Local Design Review #1 - BLOCKED` 的 P1-1/P1-2/P1-3。本轮纯设计，未改任何 Java/Maven/resources/
  tests。风险自评 `P0=0 / P1=0 / P2=0`（P2-1 capture-producer 依父级认可降为 caller cohort 前置门、P2-2 无缓存
  依父级认可为取舍，均不再列开放 P2）。仍为 `无已批准业务差异；按基线等价迁移。`；Worker B 自审仅 QA。
- 三项根因均已用父级点名的现有能力关闭，并据此**扩大写集**（父级 P1-2/P1-3 明确授权）：Cloud **5 new + 2
  modify，DHXY 零改动**。

### 现有能力核验（只读，未修改任何在途/Worker A 文件）

- `remote/CloudTaskServiceExecutionContext.java`（Worker A，`public final`，构造 package-private 由
  `CloudTaskRunAuthorityAssembly` 独占——**非本 worker 可 mint**）公开：`revalidate() → RemoteTaskRunAuthorization`
  （L58-60，`executionGate.validate(runContext)`，只读陈旧探针，从不 wait/resume/retry/mint）、`scope() →
  RemoteTaskRunScope`（tenantId/userId/deviceId/clientSessionId）、`taskRunId() → String`、`runRevision() → long`。
- `remote/run/RemoteTaskRunAuthorization.java`（record）：`allowed==true` 由 compact constructor 保证
  `binding.status()==ACTIVE`（L17-22）。故 **"confirmed ACTIVE" 判据 = `revalidate().allowed()==true`**，无需本
  worker 解析 binding 内部。
- `CloudServiceHost.create(scope, stateRoot)` 全仓**零调用方**（grep 仅自引用；ACTIVE_WORK 云端基线同述），host
  持续 dormant，未接 `CloudBrainServer`/routes。`CloudArtifactCapacityGovernor`/`CloudArtifactStore`/
  `CloudTemplateAssets` 全仓零同名（grep 零命中）。

### P1-1 修复：artifact API 改用 non-mintable `CloudTaskServiceExecutionContext`，write/read/delete 全门控

- **typed API（B1，`host/CloudArtifactStore.java`，`public interface`）**：
  - `Optional<ArtifactId> writePng(CloudTaskServiceExecutionContext ctx, String diagnosticTag, BufferedImage image)`
  - `Optional<BufferedImage> readPng(CloudTaskServiceExecutionContext ctx, ArtifactId id)`
  - `boolean delete(CloudTaskServiceExecutionContext ctx, ArtifactId id)`
  - nested `record ArtifactId(String token)`，compact constructor 正则 `^af1-[0-9a-f]{32}$` 先验。
  - **删除 Design #1 的 `String ownerTaskRunId`/`expectedOwnerTaskRunId` 入参**——不再接受任何可伪造 owner 字符串，
    也不新增 public raw owner/string mint。
- **统一前置门 `authorize(ctx)`（B2 内 private，三方法 I/O 前必过，任一不满足 → `empty`/`false` fail-closed）**：
  1. `ctx != null`；
  2. `ctx.scope().tenantId().equals(storeScope.tenantId()) && ctx.scope().userId().equals(storeScope.userId())`
     ——把 run 授权 scope 与本 store 构造时的 `CloudServiceScope` 绑定；
  3. `RemoteTaskRunAuthorization auth = ctx.revalidate(); auth.allowed()` 为真（即 current confirmed ACTIVE）。
- **owner ledger（内存，不落盘、不进文件名）**：`writePng` 成功时记录 `token → (taskRunId, runRevision)`。
  `readPng`/`delete` 先过 `authorize`，再要求"该 token 的 owner 记录存在且 `== (ctx.taskRunId(), ctx.runRevision())`"，
  否则 `empty`/`false`。→ pause/resume 后 runRevision 变化的陈旧栈既不能读回、也不能删除新 run 的 artifact；重启后
  owner 记录丢失 → 一律 fail-closed（§重启）。delete 与 read/write **同门**，闭合"delete 无 owner/revision 门"。

### P1-2 修复：新增 root-wide 容量属主 `CloudArtifactCapacityGovernor`（唯一 owner + 全局预算 + 并发编码许可）

- **B5 `host/CloudArtifactCapacityGovernor.java`（`public final`）** = 进程级唯一属主，按**规范化 stateRoot 为键**的
  静态 `ConcurrentHashMap<Path, CloudArtifactCapacityGovernor>` 保证"同一 stateRoot 全进程唯一 governor"，跨独立
  `CloudServiceHost` 的 bean graph 共享（解决"每 host 独立 store、无全局属主"）。持有：
  - 全局 `AtomicLong totalBytes` + `AtomicInteger totalCount`，硬上限 `ROOT_MAX_BYTES`（如 256 MiB）/`ROOT_MAX_COUNT`
    （如 8192）；
  - `Semaphore encodePermits`（如 4），**并发 PNG 编码许可**，闭合"多 host/线程同时编码 4096² 无界内存"；
  - 各 store 注册的 reclaim 回调表（跨 scope 回收）。
- **admission 顺序（writePng）**：① `encodePermits.acquire`（有界，无锁）→ ② 锁外编码 PNG 到 `ByteArrayOutputStream`
  （dims ≤4096²、pixels ≤8_388_608、bytes ≤8 MiB，任一超限→释放 permit→`empty`）→ ③ `governor.admit(bytes)`：
  `tryReserve`(CAS)；超全局上限→跨 store **有界公平回收**后重试；仍超→释放 permit→`empty` fail-closed → ④ 取本
  store 锁写盘 → 成功 commit / 失败 `governor.release(bytes)`；finally 释放 permit。
- **跨 scope 公平回收**：`admit` 超限时，governor 在 `budgetLock` 内**快照**各 store `(scopeDigest, oldestCreatedAt)`
  →释放 `budgetLock`→按全局 FIFO（createdAt 升序，scopeDigest tie-break）逐个调用 victim `store.evictOldestForReclaim()`，
  **一次一个 victim 锁、绝不同时持两把 store 锁、绝不持 `budgetLock` 调 store 锁**（见锁序）。
- **crash 后重建上限**：governor 首次为某 stateRoot 服务时 lazy 全根扫描各 scope 目录 `af1-*.png` 累加 seed 全局
  `totalBytes/totalCount` 并删残留 `*.png.tmp`；若 seed 已超上限 → 新写一律 fail-closed，直到回收降到上限下。**无
  TTL、无后台线程**（FIFO/admission 为纯同步容量机制，不引入时间性业务差异，符合 AGENTS 2A）。
- per-scope 512 配额保留（store 内），与全局预算叠加：先过 per-scope FIFO 再过全局 admission。

### P1-3 修复：扩展 `CloudServiceStorage` 的 package-private 可信 root 能力（不向业务 Service 暴露 Path/root）

- **B6 修改 `host/CloudServiceStorage.java`**，仅新增 **package-private** 成员（同 `host` 包的 store/governor 可达；
  `com.bot.dhxy.*` 业务 Service 跨包不可见，满足"不向业务 Service 暴露 Path/root"）：
  - `Path stateRootKey()`：返回构造时已 `toAbsolutePath().normalize()` 的 `stateRoot`，作为 governor 注册表键（在
    目录尚不存在时也稳定）。
  - `Path establishRealScopeRoot() throws IOException`：`Files.createDirectories(scopeRoot)` → `Path realState =
    stateRoot.toRealPath(); Path realScope = scopeRoot.toRealPath();` → 要求 `realScope.startsWith(realState)`，否则
    抛 → store 构造 fail-closed。**闭合"B2 拿不到可信 stateRoot 锚点做 real-path 对比"**。
  - `Path resolveWithinRealScope(Path realScopeRoot, String fileName)`：复用现有单段/穿越/绝对/包含性校验，并在
    `resolve` 后额外 `resolved.toRealPath()`（存在时）/`resolved.normalize()` 的父目录必须 `equals(realScopeRoot)`，
    覆盖 TOCTOU/Windows junction/reparse 在 create↔commit 间被替换的场景（commit 前复检）。
  - 现有 `resolvePrivateFile`、`hashScope`、containment 终检、构造签名**不改**，既有 `DialogChoiceMemoryService`
    装配路径零影响。
- **TOCTOU/reparse fail-closed 边界**：store init 时 `establishRealScopeRoot()` 建根+校验一次；每次 write 的 tmp
  用 `CREATE_NEW`（不跟随既有 reparse 点），`ATOMIC_MOVE` 后 commit 前 `resolveWithinRealScope` 复检真实父根；
  read/delete 前 `Files.isRegularFile(path, NOFOLLOW_LINKS)` + 真实父根复检。任一不符 → fail-closed，不 I/O。

### 完整写集（替换 Design #1 §3；Cloud 5 new + 2 modify，DHXY 0）

| # | 文件 | New/Modify | 可见性/内容 |
|---|---|---|---|
| B1 | `host/CloudArtifactStore.java` | New | `public interface` + nested `record ArtifactId`；三方法均首参 `CloudTaskServiceExecutionContext`；无枚举/列表/Path/stream API |
| B2 | `host/ScopedPngArtifactStore.java` | New | package-private `final implements CloudArtifactStore`；构造注入 `CloudServiceScope`+`CloudServiceStorage`+`CloudArtifactCapacityGovernor`；`authorize/owner ledger/原子写/per-scope FIFO/evictOldestForReclaim/real-path 复检/日志` 全在此；单 `storeLock` 叶子锁 |
| B3 | `host/CloudTemplateAssets.java` | New | `public interface`+nested `record TemplateId`；单 `Optional<BufferedImage> loadTemplate(TemplateId)`；无枚举/列表 |
| B4 | `com/yueyunfe/dhxy/cloudbrain/PackagedTemplateAssets.java` | New | `public final implements CloudTemplateAssets`；同包委托 `TemplateMatcher.readResourceImage(...)`；allowlist 前缀 `images/template/` + 语法门；自身零 classpath/文件解析 |
| B5 | `host/CloudArtifactCapacityGovernor.java` | New | `public final`；进程级 stateRoot-keyed 唯一属主；全局 bytes/count 上限 + encode `Semaphore` + 跨 store 公平回收 + crash 重建；`budgetLock` |
| B6 | `host/CloudServiceStorage.java` | Modify（+~30 行，签名不改） | 新增 3 个 package-private：`stateRootKey()`/`establishRealScopeRoot()`/`resolveWithinRealScope(...)`；现有成员零改 |
| B7 | `host/CloudServiceConfiguration.java` | Modify（+~12 行） | imports + `@Bean CloudArtifactStore`（注入 scope+storage+`governorFor(storage.stateRootKey())`）+ `@Bean CloudTemplateAssets`；不扩 `@ComponentScan`；既有 dialog-memory / image-processor bean 零改 |

- **依赖（消费非修改）**：B1/B2 import Worker A 的 `remote/CloudTaskServiceExecutionContext` 及 `remote/run/
  RemoteTaskRunAuthorization`、`RemoteTaskRunScope` 作**入参类型**，不编辑其文件。
- `ImageProcessorService`/`ImagePreprocessOperation`/`CloudNativeImageProcessor`/`ImageAlgorithms`/`DecisionEngine`/
  `ImageFinder`/`TemplateMatcher` 及 DHXY Java、pom、resources、tests **零 diff**。

### 唯一 authority assembly / bean 构造链

`CloudServiceHost.create(scope, stateRoot)`（dormant）→ context 注册 `CloudServiceScope`+`CloudServiceStorage` →
`CloudServiceConfiguration`：`artifactStore` bean = `new ScopedPngArtifactStore(scope, storage,
CloudArtifactCapacityGovernor.forStateRoot(storage.stateRootKey()))`；`templateAssets` bean = `new
PackagedTemplateAssets()`。运行期 artifact 授权唯一来源 = 调用方传入的 `CloudTaskServiceExecutionContext`（由
Worker A `CloudTaskRunAuthorityAssembly` mint，本 worker 不 mint、不构造）。bean 仍只在显式创建的 dormant host 内
可达，不接 server routes/poller/UI/capture/OCR/input。

### 锁序（防死锁，全局定义）

- 层级（严格自外向内，绝不逆序）：**`encodePermits`（Semaphore）→ `governor.budgetLock` → `store.storeLock`（叶子）**。
- `storeLock` 为叶子：持它时**绝不**申请 permit、**绝不**调 `governor` 的任何加锁方法（commit 时对全局计数只做
  `AtomicLong` 无锁增减）。
- `governor` 回收：在 `budgetLock` 内只**快照** victim 列表，释放 `budgetLock` 后再逐个取 victim `storeLock`
  （一次一把，`scopeDigest` 升序），**绝不**在持 `budgetLock` 或任一 `storeLock` 时再取另一把 `storeLock`。
- PNG 编码在所有锁外完成，仅受 `encodePermits` 有界并发约束 → 无"持锁编码大图"。

### 与 Worker A/C/D 零交集证明

- 本写集全部落 `host/**` 与 `com/yueyunfe/dhxy/cloudbrain/PackagedTemplateAssets.java`。
- Worker A 明确属主：`remote/**`、`remote/run/**`、`api/RemoteTaskRunEndpoint.java`、`remote/RemoteTaskRunErrorCode.java`、
  `remote/CloudTaskRunExecutionGate.java`、DHXY `cloud/remote/**` —— 本写集**零命中**（仅只读 import 其 public 类型）。
- 其它切片（image-processor 已 CLOSED、checkpoint/execution-context/retained-action/task-service-context）均不属主
  `host/CloudArtifactStore|CloudTemplateAssets|CloudArtifactCapacityGovernor|ScopedPngArtifactStore|PackagedTemplateAssets`；
  `CloudServiceStorage.java`/`CloudServiceConfiguration.java` 为 host 基础设施，本轮按 P1-2/P1-3 授权在其现有内容上
  **追加** package-private 能力与 dormant bean，不重建、不改既有成员。开工前将重新核对 7 个 new 目标不存在、2 个
  modify 文件当前内容与本设计一致；冲突即停在本文件报告，不覆盖。

### 更新后的威胁 / 生命周期矩阵

| 维度 | 修复后设计 |
|---|---|
| owner/stale revision | write/read/delete 均过 `revalidate().allowed()`(ACTIVE) + tenant/user 绑定 + `(taskRunId,runRevision)` owner 比对；resume 后旧 revision 全拒；重启 owner 丢失 → fail-closed |
| delete 门 | 与 read/write 同门（P1-1 修复），旧 token 不能删新 run artifact |
| 跨租户 | store scope=SHA-256(CloudServiceScope) 根 + context tenant/user 必须等于 store scope；他租户 ID 物理不存在且授权不过 |
| 伪造/猜测 ID | 128-bit SecureRandom；`^af1-[0-9a-f]{32}$` 先验；文件名全内部生成 |
| 路径穿越/绝对/分隔符 | ArtifactId/TemplateId 语法门 + `resolveWithinRealScope` 真实父根复检 + 既有 `resolvePrivateFile` 单段/包含性终检 |
| symlink/reparse/TOCTOU | init `establishRealScopeRoot` real-path 前缀校验；write `CREATE_NEW`+commit 前真实父根复检；read/delete `NOFOLLOW_LINKS`+真实父根复检 |
| 全局容量（磁盘） | root-wide governor 全局 bytes/count 上限（P1-2），跨 scope FIFO 公平回收；per-scope 512 保留；无界累积消除 |
| 并发编码内存 | `encodePermits` 有界并发 + 锁外编码 + dims/pixels/bytes 三重上限（P1-2） |
| corrupt image | 写侧编码失败拒绝；读侧 decode null/异常 → empty+warn；模板同 |
| 重启 | governor lazy 全根 seed + tmp 清扫 + 超限 fail-closed；store owner ledger 空 → 读/删 fail-closed |
| 并发 | 锁序 permit→budgetLock→storeLock(叶)；编码锁外；回收一次一把 store 锁 |
| 可观测性 | 单行结构化：`event/tag(消毒)/tokenPrefix(8hex)/bytes/WxH/status/reason/globalBytes/globalCount`；不记完整路径、tenant 明文、hwnd、图像内容、token 全量 |

### 三 caller 未来迁移映射（capture-producer 作 cohort 前置门；本切片不迁不激活）

| caller | 迁移后（in-memory + adapter，均传 `CloudTaskServiceExecutionContext`） | 基线语义保持 |
|---|---|---|
| `DialogService` verify-white（业务） | `detection.image()` → 既有 `washThinWhiteTextToBlackAndWhite` → `ImageFinder.find(washed, templateAssets.loadTemplate(new TemplateId(spec.templatePath())).orElse(null), 0.85)`（null→未命中）→ 同坐标换算；可选 `writePng(ctx,"dialog-white-washed",washed)` | 阈值 0.85、spec 顺序、wash 失败→`WHITE_TEMPLATE_NOT_FOUND`、日志字段逐字保留 |
| `DialogService` debug×2 | 既有内存 wash + 可选 `writePng(ctx,...)`；决策仍走内存 count（`greenCount>150` 等） | 阈值/日志不变；debug 写失败仅 warn，绝不影响决策 |
| `TaskTrackerPanelService` | 既有 `washYellowText` → `ImageFinder.find(washed, loadTemplate(titleTemplate id), threshold)` 同模板 List 顺序；stale-cleanup=`delete(ctx,旧ID)`+`writePng(ctx,新)`；replay 入口 = `readPng(ctx, artifactId)` | 各阈值/顺序、title→crop 几何、miss 与 shadow 上报顺序不变；**replay 生产端（capture 写 panel artifact）= 未来 capture 切片，作 caller cohort 前置门** |
| `CoordinateHelper.findGreenTextInRegion`（当前零调用方） | 复活时：内存 capture → 既有 `washGreenTextToBlackAndWhite` → `ImageFinder.find(washed, loadTemplate(id), rate)`；无文件中转 | 零调用方=无现行为可破坏 |

三 caller 本切片不迁移/不激活，cohort gate（前置 image-processor 切片拍板 + 本 capture 前置门）继续有效。

### 逐条返修对照

- **P1-1**：满足。API 去除 owner 字符串，改 `CloudTaskServiceExecutionContext`；write/read/delete 全过
  `revalidate()`ACTIVE + tenant/user 绑定 + `(taskRunId,runRevision)` owner 比对；delete 同门；不新增 raw owner mint，
  不改 Worker A 文件。
- **P1-2**：满足。新增 root-wide `CloudArtifactCapacityGovernor` 唯一属主：全局 bytes/count 上限、encode 许可、跨
  scope 公平回收、crash 重建上限、fail-closed；保留 per-scope 512；无 TTL/后台线程；写集含实现该属主的 B5 + B6/B7
  装配。
- **P1-3**：满足。B6 给 `CloudServiceStorage` 加 package-private `stateRootKey/establishRealScopeRoot/
  resolveWithinRealScope`，store 在 create/read/delete 前后可比较真实 scope root 与真实 state root；TOCTOU/Windows
  reparse fail-closed 边界写清；不向业务 Service 暴露 Path/root；完整写集已更新。

### 批准后验证计划（仅 `DESIGN APPROVED` 后执行）

按写集实施 → Cloud `mvn -q clean package`（无 skip flag）→ 追加 `## External Worker B - Implementation #1`：逐文件精确
diff/行数/SHA-256；`javap` 证 `ArtifactId/TemplateId` 封闭、无 raw owner/enumeration/Path/HTTP/poll API、governor
public 面仅容量算子；扫描 B1-B5 对 `getResources|Path.of(caller|toFile|URL|new JarFile(` 的可见性面与
`ScopedPngArtifactStore` 仅经 `CloudServiceStorage` package-private 能力触盘；Surefire suite/test/failure/error/skip 计数；
`src/test`/pom/resources/DHXY 零 diff；Worker A/host 既有 dirty 未被覆盖；无 server/host/poller/UI/capture/OCR/input
启动；无 Git mutation。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Design Repair #1 到此停止，等待父级写
`DESIGN APPROVED` 或再次 `BLOCKED`。

## Local Design Review #2 - BLOCKED - 2026-07-12

Design Repair #1 已关闭原 P1-1：artifact write/read/delete 统一接受 non-mintable exact
`CloudTaskServiceExecutionContext`，并绑定 current confirmed ACTIVE、tenant/user、taskRunId/runRevision；可信 root 能力也已
进入完整写集。父级复审结论仍为 **BLOCKED，P0=0/P1=2/P2=2**，只退回容量事务与重启回收边界，不要求推翻 typed API。

### P1-1：root-wide bytes/count reservation 与 per-scope FIFO 的锁/事务顺序相互矛盾

- 证据：Repair #1 同时写明 governor 用独立 `AtomicLong totalBytes` + `AtomicInteger totalCount` 做 `tryReserve(CAS)`，又称
  `budgetLock` 是全局 owner；写路径还声明“先过 per-scope FIFO 再过 global admission”，而锁序要求
  `budgetLock -> storeLock` 且持 storeLock 不得调用 governor 加锁方法。
- 影响：两个独立 CAS 不是一个 `(bytes,count)` 原子配额；并发下可出现 bytes 已保留而 count 失败、rollback 与 eviction
  交错，或为了 per-scope FIFO 先拿 storeLock 再申请 budgetLock 形成逆序。容量拒绝、回收和落盘无法证明为同一事务，可能
  超限、负计数或死锁。
- 返修条件：Design Repair #2 必须定义一个 package-private immutable reservation handle，在**单一 budgetLock** 内原子检查并
  预留 bytes+count；文件成功 commit 后 handle commit，任何编码/路径/写/rename 失败均 exact-once rollback。per-scope eviction
  与全局 victim 回收必须给出唯一线性化顺序，明确每一步持有哪些锁；不得在 storeLock 内 acquire budgetLock，也不得用两个
  独立 atomic 代替 tuple authority。若保留无锁统计，只能是观测副本，不能作为 admission authority。

### P1-2：重启 seed 会统计未注册 scope 的 orphan 文件，但回收器没有能力释放它们

- 证据：governor 首次启动扫描整个 stateRoot，把所有 `af1-*.png` 计入 global totals；真正 reclaim 却只调用“各 store 注册
  的回调”。重启后 owner ledger 全空，且 dormant host 只为当前 tenant/user 创建一个 store，其余历史 scope 没有注册
  callback。
- 影响：历史未注册 scope 文件会永久占用 totalBytes/totalCount；接近或超过上限时，新写只能持续 fail-closed，所谓“直到
  回收降到上限”没有可达路径。多租户重启后会形成真实容量锁死。
- 返修条件：Design Repair #2 必须让 root governor 对 startup scan 得到的每个 artifact 建立有界、可信 real-root-bound 的
  reclaim index，并能在没有 scope bean/owner ledger 时按全局 FIFO 删除**仅由本 adapter 命名和验证**的 orphan artifact；或
  提供等价的全部历史 scope 注册机制。扫描、索引、删除都必须受 root-wide count/bytes 上限和 real-root containment，不能
  跟随 symlink/reparse，且 owner ledger 缺失只禁止业务 read/delete，不得阻止 governor 容量回收。

### P2-1：当前 Java Path 流程不能声称完全闭合 hostile reparse TOCTOU

- 证据：`establishRealScopeRoot()` 与 commit 前 `toRealPath()` 都是路径级检查；攻击者仍可在检查与 `CREATE_NEW`/move 之间
  替换父目录。Java NIO 的该设计没有持有 secure directory handle，`CREATE_NEW` 只保证叶文件不存在，不锁定父目录对象。
- 影响：设计对本地有权修改 stateRoot 的攻击者给出了无法实现的安全承诺，后续实现容易被错误批准。
- 返修条件：明确 threat boundary：stateRoot 及其父目录必须是受信、非对手可写的 Cloud 私有目录；real-path/NOFOLLOW
  检查用于防配置错误和检测异常，不宣称抵御有权并发替换父目录的本地管理员。若坚持 hostile-filesystem 威胁模型，则必须
  换成平台 secure-directory-handle 方案，本切片不要虚构能力。

### P2-2：capacity governor 无需 public

- 证据：governor 只被同 `host` 包的 storage/config/store 使用；Design 却定义为 `public final`。
- 影响：扩大了 root-wide capacity/reclaim 原语的可见面，未来业务代码可绕过 store API 直接取得容量能力。
- 返修条件：`CloudArtifactCapacityGovernor` 及 reservation/reclaim API 全部 package-private；业务只见
  `CloudArtifactStore`/`CloudTemplateAssets`，且二者不暴露 Path、枚举、容量或 reclaim。

### Repair #2 验收门

只追加 `External Worker B - Design Repair #2`，保留已通过的 typed context/owner/revision API 与 5-new+2-modify 上限；
重写 capacity reservation 状态机、锁序和 restart orphan reclaim，收窄 reparse threat claim 与 governor 可见性。Java/Maven/
resources/tests 继续冻结，host/caller cohort 继续 dormant。**无已批准业务差异；按基线等价迁移。**

## External Worker B - Design Repair #2 - 2026-07-12

### 返修结论

- 逐条接受 `Local Design Review #2 - BLOCKED` 的 P1-1/P1-2/P2-1/P2-2。本轮纯设计，未改任何 Java/Maven/resources/tests。
- **保留已通过项**：typed API 三方法首参 non-mintable `CloudTaskServiceExecutionContext` + `revalidate()`ACTIVE +
  tenant/user 绑定 + `(taskRunId,runRevision)` owner 门；写集上限 **Cloud 5 new + 2 modify，DHXY 0**。本轮只**重写**
  容量 reservation 状态机 / 锁序 / restart orphan reclaim，**收窄** reparse 威胁声明与 governor 可见性，不新增文件、不
  推翻 API。风险自评 `P0=0 / P1=0 / P2=0`。`无已批准业务差异；按基线等价迁移。` 自审仅 QA。

### P1-1 重写：单一 budgetLock 下的 (bytes,count) 元组权威 + 不可变 reservation handle + exact-once commit/rollback

- **容量权威唯一化**：撤回 Repair #1 的"两个独立 `AtomicLong/AtomicInteger` + per-scope FIFO 先于 global admission"。
  改为 **governor 在单一 `budgetLock` 内持有全部容量权威**：全局 `long totalBytes,totalCount` + 每 scope
  `Map<String,ScopeUsage{count,bytesByToken(LinkedHashMap 保 FIFO)}>`。**per-scope 512 与 root-wide bytes/count 是同一
  次 budgetLock 事务内的联合判定**，不再"先 storeLock 过 per-scope 再 budgetLock 过 global"。无锁统计（若保留）仅作
  日志观测副本，**绝不**作 admission 依据。
- **不可变 reservation handle（governor 内 package-private `record Reservation`）**：
  `reserve(scopeDigest, bytes) → Optional<Reservation>` 在**单一 budgetLock** 内原子完成：
  1. 计算为容纳 `(bytes,+1 count)` 需要腾出的 victim（先本 scope FIFO 满 512、再 root-wide bytes/count 压力，按全局
     `createdAt` 升序、`scopeDigest` tie-break 线性化选取）；
  2. **立即在账上扣除** victim 的 bytes/count 并从各 FIFO index **移除**（标记 `EVICTING`，交给本 reservation 独占，
     并发 reserve 因已移出 index 不会重选，杜绝双删/负计数）；
  3. 预留 `(bytes,+1)`；构造 `Reservation{token, scopeDigest, bytes, List<VictimRef> victims}`（immutable）。
  账面变更全部在 budgetLock 内一次完成，**无任何 I/O**。容量不足且无可腾空间 → 返回 `empty`（fail-closed）。
- **两段式（账/盘分离，杜绝持锁 I/O）**：reserve 返回后释放 budgetLock →（锁外）删 victim 文件 + 写新文件 →
  `reservation.commit()`（budgetLock 内把新 token 落入 FIFO index，victim 账已在 reserve 扣除，故 commit 只 finalize
  新条目）**或** `reservation.rollback()`（budgetLock 内**仅**回滚新 token 的 `(bytes,+1)` 预留；victim 已物理删除，其
  账扣除保持——一致且 exact-once）。任何 encode/path/write/rename 失败均触发 exact-once `rollback()`。
- **victim 删除线性化**：victim 可跨 scope；删除由 governor 按 `scopeDigest` 升序**逐个**取"该 scope 的 per-scope
  monitor"删文件，**一次一把、绝不同时持两把、绝不在 budgetLock 内删文件**。

### P1-1 锁序（全局唯一定义，消除逆序）

- 层级严格自外向内：**`encodePermits`(Semaphore) → `budgetLock`(容量权威，纯账、无 I/O) → per-scope monitor(叶，纯
  filesystem)**。
- `budgetLock` 与任何 per-scope monitor **绝不同时持有**：reserve/commit/rollback 进出 budgetLock 都不做 I/O；victim
  删除与新文件写在 budgetLock 外、只持 per-scope monitor。
- per-scope monitor 是叶：持它时绝不申请 permit、绝不进 budgetLock。多 victim 删除按 scopeDigest 升序一次一把。
- **per-scope monitor 统一属主**：由 governor 提供 `Object scopeMonitor(scopeDigest)`（`ConcurrentHashMap` 惰性建
  一个 monitor），**live store 的所有 filesystem 写/业务 read/delete 与 governor 的 reclaim 删除共用同一把**，因此活
  scope 与 orphan scope 的目录 I/O 都串行化在同一 monitor 上（解决活写与回收删并发触同目录）。

### P1-2 重写：governor 自持"有界、real-root-bound reclaim index"，可删 orphan，无需 store bean/owner ledger

- **startup scan → reclaim index**：governor 首次为某 stateRoot 服务时，**有界**扫描 `stateRoot/<64-hex>/af1-[0-9a-f]{32}.png`
  （仅这种 adapter 自命名+自验证形态；`<64-hex>` 必须匹配 scope 目录名规格；每条经 `NOFOLLOW_LINKS` 且 real-path 落在
  `stateRoot.toRealPath()` 下才纳入），为**每个** artifact（含无 live store 的历史 scope）建立
  `ReclaimEntry{token, scopeDigest, realPath, bytes, lastModified}` 并计入全局 totals 与 per-scope FIFO。扫描受
  root-wide count/bytes 上限约束：达上限即停止纳入并记 `SATURATED`（不无界读目录）。清扫遗留 `*.png.tmp`。
- **orphan 可回收**：reclaim 的 victim 选取与删除**只依据 reclaim index + 全局 FIFO**，删除由 governor 直接对
  `ReclaimEntry.realPath` 执行（该路径已在 scan 时 real-root+NOFOLLOW 验证，删除前于对应 scope monitor 内再验一次
  `isRegularFile(NOFOLLOW_LINKS)` 且真实父目录 == 该 scope real root）。**不依赖 store 注册回调、不依赖 owner ledger**，
  因此历史未注册 scope 的 orphan 也能按全局 FIFO 释放，消除"容量锁死"。
- **owner ledger 缺失的作用域**：仅禁止**业务 read/delete**（fail-closed），**不**阻止 governor 容量回收——回收是
  adapter 对自命名文件的容量自治，与业务 owner 门正交。
- **live store 与 governor 一致性**：live store 写成功经 `reservation.commit()` 把新 token 同时登记进 governor 的 FIFO/
  reclaim index；业务 delete 成功同样通知 governor 移除该条目（budgetLock 内）。故 index 始终是账面权威的镜像。
- 仍**无 TTL、无后台线程**：scan 惰性一次、reclaim 同步在 admission 路径触发，纯容量机制（符合 AGENTS 2A）。

### P2-1 收窄：明确 reparse/TOCTOU 威胁边界，不虚构 hostile-filesystem 能力

- **威胁边界声明**：`stateRoot` 及其所有父目录是**受信、非对手可写的 Cloud 私有目录**（部署前置）。本设计的
  `establishRealScopeRoot()` / commit 前 `toRealPath()` / `NOFOLLOW_LINKS` 检查用于**防配置错误、检测异常与拦截无意
  的 symlink**，**不**声称抵御"有权并发替换父目录的本地管理员"。
- Java NIO 无 secure-directory-handle：`CREATE_NEW` 只保证叶文件不存在、不锁定父目录对象；故对"检查与 create/move
  之间被特权替换父目录"的 hostile 场景，本切片**不承诺**闭合。若将来采纳 hostile-filesystem 威胁模型，须换平台
  secure-directory-handle 方案，另开切片，本切片不夹带该虚构能力。威胁矩阵相应行改述为"受信根内的错误/异常防护"。

### P2-2 收窄：governor 及 reservation/reclaim 原语全部 package-private

- `CloudArtifactCapacityGovernor` 由 `public final` 改为 **package-private `final class`**（host 包内 storage/config/store
  可达即可）；其 `reserve/commit/rollback/reclaim/scopeMonitor/registerScope` 与 nested `Reservation`/`ReclaimEntry`/
  `VictimRef` 全部 package-private。
- 业务侧只见 `CloudArtifactStore`/`CloudTemplateAssets`，二者**不暴露** Path、枚举、容量或 reclaim 能力；
  `PackagedTemplateAssets`（`com.yueyunfe.dhxy.cloudbrain` 包）不依赖 governor。B5 写集条目可见性相应更新为
  package-private。

### 更新后的写集（仍 Cloud 5 new + 2 modify，DHXY 0；仅可见性/内部结构变化）

| # | 文件 | New/Modify | 变更点（相对 Repair #1） |
|---|---|---|---|
| B1 | `host/CloudArtifactStore.java` | New | 不变（typed API + `ArtifactId`） |
| B2 | `host/ScopedPngArtifactStore.java` | New | 写路径改两段式（reserve→锁外 I/O→commit/rollback）；filesystem 串行用 `governor.scopeMonitor(scopeDigest)` 而非自有锁；owner ledger 只门业务 read/delete |
| B3 | `host/CloudTemplateAssets.java` | New | 不变 |
| B4 | `com/yueyunfe/dhxy/cloudbrain/PackagedTemplateAssets.java` | New | 不变 |
| B5 | `host/CloudArtifactCapacityGovernor.java` | New | **package-private** `final`；单 `budgetLock` 元组权威 + `Reservation`/`ReclaimEntry`/`VictimRef`（均 package-private）+ startup real-root-bound reclaim index + `scopeMonitor(scopeDigest)` 统一 filesystem 串行属主 + 直接删 orphan |
| B6 | `host/CloudServiceStorage.java` | Modify | 不变（package-private `stateRootKey/establishRealScopeRoot/resolveWithinRealScope`） |
| B7 | `host/CloudServiceConfiguration.java` | Modify | 不变（注入 scope+storage+`governorFor(storage.stateRootKey())`；bean 仍只 `CloudArtifactStore`/`CloudTemplateAssets`） |

- 依赖仍为只读 import Worker A 的 `CloudTaskServiceExecutionContext`/`RemoteTaskRunAuthorization`/`RemoteTaskRunScope`
  作入参类型，不改其文件。与 Worker A/C/D 零交集结论不变（写集全落 `host/**` + `PackagedTemplateAssets`）。

### 容量 reservation 状态机（单 budgetLock 线性化）

```
writePng(ctx, tag, image):
  authorize(ctx)               // revalidate ACTIVE + tenant/user + (无 I/O)
  permit = encodePermits.acquire()            // 层1
  try:
    bytes = encodePng(image)   // 锁外；dims/pixels/bytes 三重上限，超限→empty
    Optional<Reservation> r = governor.reserve(scopeDigest, bytes)   // 层2 budgetLock 内原子：选 victim+扣账+预留
    if r.empty: return empty                   // 容量满且无可腾→fail-closed
    try:
      governor.deleteVictims(r.victims)        // 层3 逐 scopeMonitor 一次一把，删文件
      token file: CREATE_NEW tmp → ATOMIC_MOVE → commit 前 resolveWithinRealScope 复检   // 层3 本 scopeMonitor
      recordOwner(token, ctx.taskRunId(), ctx.runRevision())   // 内存
      governor.commit(r)                       // 层2 budgetLock：新 token 入 FIFO/reclaim index
      return ArtifactId(token)
    catch any:
      governor.rollback(r)                     // 层2 budgetLock：仅回滚新 token 预留（exact-once）
      best-effort 删已写 tmp/target
      return empty
  finally: permit.release()
```

- `reserve` 与 `commit`/`rollback` 各是一次独立 budgetLock 临界区，之间无锁做 I/O；victim 账在 `reserve` 扣除、物理删
  在锁外、成功后不回补——一致。任一失败仅回滚新 token，绝不超限/负计数/死锁。

### 更新后的锁 / 容量 / 重启 / 威胁矩阵

| 维度 | 修复后 |
|---|---|
| 容量权威 | 单 `budgetLock` 内 `(totalBytes,totalCount)` + per-scope count 联合判定；无锁统计仅观测副本 |
| reservation | 不可变 handle；reserve 原子扣 victim+预留；commit/rollback exact-once；账/盘两段分离 |
| 锁序 | `encodePermits → budgetLock(纯账) → per-scope monitor(叶,纯 I/O)`；budgetLock 与 monitor 不同持；多 victim 按 scopeDigest 升序一次一把 |
| filesystem 串行属主 | `governor.scopeMonitor(scopeDigest)` 统一 live 写 / 业务 read-delete / reclaim 删，活/orphan 同 scope 都串行 |
| 全局容量 | root-wide bytes/count 硬上限；startup scan 建 reclaim index 计入全部 af1 文件；达上限停纳入记 SATURATED |
| orphan 回收 | governor 直接按全局 FIFO 删 adapter 自命名 orphan（real-root+NOFOLLOW 验证），不依赖 store 回调/owner ledger→无容量锁死 |
| owner ledger 缺失 | 仅 fail-closed 业务 read/delete；不阻止 governor 容量回收 |
| 并发编码内存 | `encodePermits` 有界 + 锁外编码 + dims/pixels/bytes 三重上限 |
| owner/stale revision | 三方法过 revalidate ACTIVE + tenant/user + `(taskRunId,runRevision)`；resume 后旧 revision 全拒；重启 owner 空→业务 read/delete fail-closed |
| 跨租户 | scope=SHA-256(CloudServiceScope) 根 + context tenant/user==store scope；他租户物理隔离且授权不过 |
| 路径/穿越/ID | ArtifactId/TemplateId 语法门 + `resolveWithinRealScope` 真实父根复检 + 既有单段/包含性终检 |
| reparse/TOCTOU | **受信根内错误/异常防护**（config error/无意 symlink/NOFOLLOW/real-path）；**不**声称抵御有权并发替换父目录的本地管理员（P2-1 边界；无 secure-dir-handle 不虚构） |
| 可观测性 | 单行结构化 `event/tag(消毒)/tokenPrefix(8hex)/bytes/WxH/status/reason/globalBytes/globalCount/evicted`；不记全路径/tenant 明文/hwnd/图像/token 全量 |

### 三 caller 映射与 cohort（不变）

三 caller（`DialogService` verify-white/debug×2、`TaskTrackerPanelService`、`CoordinateHelper.findGreenTextInRegion`）
迁移映射同 Repair #1（均传 `CloudTaskServiceExecutionContext`；模板走 `loadTemplate`；stale-cleanup=`delete`+`writePng`；
replay=`readPng`）。capture-producer 继续作 caller cohort 前置门；模板无缓存继续为接受取舍；本切片不迁不激活，host
dormant。

### 逐条对照

- **P1-1**：满足。单 budgetLock 元组权威 + immutable `Reservation` + exact-once commit/rollback；per-scope 与 global 同
  事务判定；锁序 `encodePermits→budgetLock→scopeMonitor(叶)` 无逆序；无双原子代替 tuple。
- **P1-2**：满足。governor 自持 real-root-bound reclaim index 覆盖全部扫描 af1 文件，可无 store bean/owner ledger 按全局
  FIFO 删 orphan；扫描/索引/删受 root-wide 上限 + real-root containment + NOFOLLOW；owner 缺失只门业务 read/delete。
- **P2-1**：满足。威胁边界收窄为"受信根内错误/异常防护"，明确不承诺 hostile 并发父目录替换，不虚构 secure-dir-handle。
- **P2-2**：满足。governor 及 reservation/reclaim 全 package-private；业务只见 store/template 接口，无 Path/枚举/容量/reclaim。

### 批准后验证计划（仅 `DESIGN APPROVED` 后执行）

同 Repair #1 计划，另加：`javap` 证 `CloudArtifactCapacityGovernor` 及 `Reservation/ReclaimEntry/VictimRef` 无 public
成员；扫描证 `budgetLock` 临界区内无 `Files.`/`ImageIO.`/move/delete 调用（账/盘分离）、per-scope monitor 内无
`budgetLock` 获取（锁序）。Cloud `mvn -q clean package`（无 skip），追加 `## External Worker B - Implementation #1`。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Design Repair #2 到此停止，等待父级写
`DESIGN APPROVED` 或再次 `BLOCKED`。

## Local Design Review #3 - BLOCKED - 2026-07-12

Repair #2 已关闭上一轮的 tuple authority、锁逆序、orphan 无 callback、reparse threat overclaim 与 public governor；这些项
不再重开。父级结论仍为 **BLOCKED，P0=0/P1=2/P2=0**，阻塞只在“账面释放早于物理删除”与 saturated scan 漏记。

### P1-1：victim 在物理删除前已从账面扣除，部分删除失败会永久低估磁盘并把未释放空间重复授予

- 证据：`reserve` 在 budgetLock 内把全部 victims 从 totals/FIFO 移除并立即预留新 `(bytes,+1)`；随后才锁外
  `deleteVictims`。catch/rollback 明确“仅回滚新 token，victim 账扣除保持”。若第 N 个 victim 删除失败，前 N-1 个可能已删，
  失败及未处理 victims 仍在盘上却全部不再计账；同时其它 reserve 在此窗口已可把这些尚未删除的 bytes 再次授予。
- 影响：真实 root bytes/count 可超过硬上限，失败路径产生永久账实漂移；并发 reservation 还会在 victim 尚存在时写入新文件，
  所谓 root-wide hard cap 不成立。
- 返修条件：Design Repair #3 必须改为显式 `EvictionPlan -> settle -> Reservation`（或等价状态机）。选中 victim 时只标记
  `EVICTING`、禁止被再次选择，但**仍计入** totalBytes/totalCount，不能把容量视为已释放；锁外逐项删除后，回到
  budgetLock 只对实际成功删除且 identity/size 仍匹配的 victim 扣账，失败/未处理项恢复 ACTIVE。只有 settle 后真实已释放
  空间足够，才原子创建新 token reservation；否则不写新目标并 fail-closed。新 reservation 的 commit/rollback 仍 exact-once。
  任意时刻 admission authority 都不得低于已知真实磁盘占用。

### P1-2：startup scan 达上限即停止会留下未计账、未索引、不可回收的 adapter 文件

- 证据：Repair #2 规定扫描“达 root-wide count/bytes 上限即停止纳入并记 SATURATED”，但未扫描的其余
  `<64hex>/af1-*.png` 不进入 totals 或 reclaim index；后续矩阵仍宣称 index 覆盖全部 af1 文件并消除容量锁死。
- 影响：真实磁盘占用被低估；若 SATURATED 仍允许 admission 会越限，若永久拒绝则未索引 orphan 又没有自动或明确运维
  恢复路径，设计声明自相矛盾。
- 返修条件：Design Repair #3 必须二选一并写死：
  1. **安全自动回收**：流式遍历所有匹配项、只保留有界 victim batch/heap，累计 total count/bytes 为标量；按批次同步
     reclaim 并重扫，直到真实占用进入上限，再开放 admission；内存有界、每个删除仍 real-root/NOFOLLOW 校验；或
  2. **明确运维 fail-closed**：超过可完整索引的 scan 上限或遇遍历错误时置 `SATURATED_UNINDEXED`，所有 write/reserve
     永久拒绝并输出不含敏感路径的 operator reason，直到受控运维清理/重启后完成全量核算。此方案不得再声称自动消除
     极端 orphan capacity lock。
  无论选哪种，未完整核算时绝不能开放新写，tmp cleanup 也不能被计作已核算 artifact。

### Repair #3 验收门

只追加 `External Worker B - Design Repair #3`；保留 typed context API、5-new+2-modify、package-private governor、受信
stateRoot threat boundary 与既定 caller 映射。只重写 eviction/accounting 两阶段状态机和 saturated scan 行为；Java/
Maven/resources/tests、host/caller cohort 继续冻结。**无已批准业务差异；按基线等价迁移。**

## External Worker B - Design Repair #3 - 2026-07-12

### 返修结论

- 逐条接受 `Local Design Review #3 - BLOCKED` 的 P1-1/P1-2。本轮纯设计，未改任何 Java/Maven/resources/tests。
- **保留已通过项**（不重开）：typed API 三方法 non-mintable `CloudTaskServiceExecutionContext` + `revalidate()`ACTIVE +
  tenant/user + `(taskRunId,runRevision)` owner 门；单 `budgetLock` 容量权威；锁序 `encodePermits→budgetLock→scopeMonitor`；
  package-private governor；受信 stateRoot 威胁边界；caller 映射；**Cloud 5 new + 2 modify，DHXY 0**。
- **本轮只重写两点**：①eviction/accounting 改为"**物理删除证实后才扣账**"的三阶段机（victim 删除前始终计账）；
  ②startup scan 改为"**流式有界全量核算 + 收敛前不开 admission**"。风险自评 `P0=0 / P1=0 / P2=0`。自审仅 QA。

### P1-1 重写：`EvictionPlan → (物删) → settleAndReserve` 三阶段，admission 权威永不低于真实磁盘占用

核心不变式：**victim 在被证实物理删除之前，始终计入 `totalBytes/totalCount`（容量不视为已释放）；新 token 只在
settle 确认真实腾出空间后才计账。** 任意时刻 `totalBytes/totalCount ≥ 已知真实磁盘占用`。

写路径三阶段（两段 budgetLock 临界区，中间锁外物删）：

```
writePng(ctx, tag, image):
  authorize(ctx)                                  // revalidate ACTIVE + tenant/user (无 I/O)
  permit = encodePermits.acquire()                // 层1
  try:
    bytes = encodePng(image)                      // 锁外; dims/pixels/bytes 三重上限, 超限→empty
    if governor.status != READY: return empty     // 未完成 startup 核算不开写 (见 P1-2)

    # 阶段A — planEviction (budgetLock)
    EvictionPlan plan = governor.planEviction(scopeDigest, bytes)
      # 在 budgetLock 内: 若当前 (totalBytes+bytes, totalCount+1) 或 per-scope count+1 超限,
      #   按全局 FIFO(createdAt 升序, scopeDigest tie-break) 选出"若全部删掉即可容纳"的最小 victim 集;
      #   把这些 victim 标 EVICTING(从可选 FIFO 移出, 禁止被并发再选), 但 **不扣账、仍计入 totals**;
      #   若把本 scope 所有可删项都算上仍不够 → 返回 INFEASIBLE(不标任何 victim)
    if plan.infeasible: return empty              // fail-closed, 账面零变更

    # 阶段B — 物理删除 (锁外, 逐 scopeMonitor 一次一把, scopeDigest 升序)
    DeleteResults dr = governor.deleteEvicting(plan)
      # 每 victim: 在其 scopeMonitor 内 NOFOLLOW_LINKS + 真实父根 + identity(token)/size 复检后删;
      #   记录 {victim → DELETED_MATCHED | FAILED | SKIPPED}; 绝不在此持 budgetLock

    # 阶段C — settleAndReserve (budgetLock)
    Optional<Reservation> r = governor.settleAndReserve(plan, dr, scopeDigest, bytes)
      # 在 budgetLock 内: 对 dr==DELETED_MATCHED 者才扣 totals 并移出 index;
      #   FAILED/SKIPPED 者恢复 ACTIVE(清 EVICTING, 仍计账, 回到 FIFO 尾按原 createdAt 位);
      #   settle 后若 (totalBytes+bytes, totalCount+1, per-scope+1) 现已真正可容纳 →
      #      预留新 token(计账 totals, 建 PENDING index 占位) 返回 Reservation; 否则 empty(不写新目标)
    if r.empty: return empty                       // fail-closed; 已删 victim 账已释放, 未删者仍计账

    # 阶段D — 写新 + commit/rollback
    try:
      tmp CREATE_NEW → ATOMIC_MOVE → resolveWithinRealScope 复检     // 层3 本 scopeMonitor
      recordOwner(token, ctx.taskRunId(), ctx.runRevision())        // 内存 owner ledger
      governor.commit(r)                           // budgetLock: PENDING→ACTIVE 落 FIFO/reclaim index (账已在 C 预留)
      return ArtifactId(token)
    catch any:
      governor.rollback(r)                         // budgetLock: 仅回滚新 token 的 (bytes,+1) 预留, exact-once
      best-effort 删 tmp/target
      return empty
  finally: permit.release()
```

- **部分删除失败安全**：阶段B 删到第 N 个失败时，前面 DELETED_MATCHED 的在阶段C 被扣账（真实已释放），失败/未处理者
  在阶段C 恢复 ACTIVE 仍计账（真实仍在盘）→ 账面 ≥ 真实占用，硬上限成立；且并发 reserve 在 victim EVICTING 期间既
  不能重选（已移出 FIFO）也看不到"已释放空间"（仍计账），杜绝把未删 bytes 重复授予。
- **runtime reclaim** 与 write 复用同一三阶段机（reclaim 的 victim 就是 ACTIVE token，按全局 FIFO 选，走 plan→删→settle，
  只是无"阶段D 新写"）。commit/rollback 仍 exact-once。
- 锁序不变：`encodePermits → budgetLock(纯账,无 I/O) → scopeMonitor(叶,纯 filesystem)`；budgetLock 与 scopeMonitor 不同持。

### P1-2 重写：startup 采用【方案 1 安全自动回收】—— 流式有界全量核算，收敛前 admission 关闭

选定父级**方案 1**（流式有界 + 批次 reclaim），并对"遍历错误"补一条明确 fail-closed，绝不自相矛盾：

```
governor.reconcile(realStateRoot):        // 首次为该 stateRoot 服务时惰性一次; status=RECONCILING, admission=CLOSED
  try:
    repeat:
      long scalarBytes=0; long scalarCount=0
      BoundedOldestHeap heap(K)           // 只保留按 lastModified 最旧的 K 项(有界内存), 非全量驻留
      for each dir D under realStateRoot (DirectoryStream 流式, 名匹配 ^[0-9a-f]{64}$):
        for each F=af1-[0-9a-f]{32}.png in D (DirectoryStream 流式):
          if !isRegularFile(F, NOFOLLOW_LINKS): continue
          if F.toRealPath().parent != D.realRoot: continue        // real-root 包含
          scalarBytes += size(F); scalarCount++                   // **全量**累计标量(不因上限停止)
          heap.offerBounded(F, lastModified)                      // 仅留最旧 K
        deleteStray(D, "*.png.tmp")                               // tmp 清理, **不计账**
      if scalarBytes<=CAP_BYTES && scalarCount<=CAP_COUNT: break  // 真实占用已入上限
      # 超限: 同步删 heap 中最旧的一批(每项再 NOFOLLOW/real-root/identity 复检), 然后重扫(下一轮)
      deleteOldestBatch(heap)                                      // 每轮至少删 1, 保证收敛/终止
    # 收敛: 真实占用<=cap; 此时存活集<=cap(有界) → 载入 runtime FIFO/reclaim index, totals=scalar
    status = READY; admission = OPEN
  catch traversal IOException / 无法完整核算:
    status = SATURATED_UNINDEXED; admission = CLOSED(永久)          // operator reason(不含敏感路径), 待受控运维清理/重启后全量核算
```

- **内存有界**：只驻留标量 totals + 大小 K 的最旧堆；不把全部条目载入。达上限时按批 reclaim 并重扫，直到真实占用入
  上限——因此**不存在"未计账/未索引的 adapter 文件"**（全量标量核算覆盖每个匹配文件），消除 Repair #2 的自相矛盾。
- **收敛前绝不开写**：`status != READY` 时 `planEviction`/write 一律 fail-closed；`SATURATED_UNINDEXED` 时永久拒绝并给
  运维 reason，**不**再声称"自动消除极端 orphan capacity lock"（遍历错误场景诚实交给运维）。
- `*.png.tmp` 只清理、**绝不计作已核算 artifact**。每个删除仍 real-root/NOFOLLOW/identity 复检。
- 稳态运行时 `totalCount ≤ CAP_COUNT`（admission 强制），故 runtime index 恒有界；startup 是唯一可能面对"盘上超量"的
  点，已由流式有界回收覆盖。

### 更新后的容量状态机 / 锁 / 重启矩阵（仅相关行）

| 维度 | Repair #3 |
|---|---|
| victim 账实一致 | victim 删除**证实前始终计账**；阶段C 只对 DELETED_MATCHED 扣账，FAILED/SKIPPED 恢复 ACTIVE；admission 权威 ≥ 真实占用 |
| 新 token | 仅在 settle 确认真实腾空后预留(PENDING)，commit 落 index / rollback 回滚，exact-once |
| 部分删除失败 | 未删 bytes 保持计账，不被并发重复授予；root hard cap 成立 |
| 锁序 | `encodePermits → budgetLock(纯账) → scopeMonitor(叶,纯 I/O)`；三阶段两段 budgetLock 中间锁外物删 |
| startup 核算 | 流式全量标量核算 + 有界最旧堆 + 批次 reclaim 重扫至真实占用入上限；收敛前 admission 关闭；tmp 不计账 |
| 遍历错误 | `SATURATED_UNINDEXED` 永久 fail-closed + operator reason，不虚构自动恢复 |
| orphan 回收 | 收敛过程按全局 FIFO(最旧堆)删 adapter 自命名 orphan，无需 store bean/owner ledger；owner 缺失只门业务 read/delete |

### 逐条对照

- **P1-1**：满足。改 `EvictionPlan→物删→settleAndReserve` 三阶段；victim 证实删除前不扣账、EVICTING 禁重选且仍计账；
  只对 DELETED_MATCHED 扣账，失败恢复 ACTIVE；新 token settle 后才预留；commit/rollback exact-once；admission 权威永不
  低于真实磁盘占用；并发窗口不再重复授予未释放空间。
- **P1-2**：满足。选定方案 1 流式有界全量核算，收敛前 admission 关闭；无未计账/未索引 adapter 文件；遍历错误明确
  `SATURATED_UNINDEXED` fail-closed 不自相矛盾；tmp 不计账；内存有界。

### 保留项与写集（不变）

- typed API / owner-revision 门 / package-private governor（含 `EvictionPlan`/`Reservation`/`ReclaimEntry`/`VictimRef`
  全 package-private）/ 受信 threat boundary / 三 caller 映射 / cohort dormant 均同 Repair #2。
- 写集仍 **Cloud 5 new（B1-B5）+ 2 modify（B6/B7）+ DHXY 0**；与 Worker A/C/D 零交集不变（只读 import Worker A public
  类型作入参）。B5 内部新增 `EvictionPlan`/`settleAndReserve`/`reconcile` 逻辑，均 package-private，不改写集文件数与
  可见性面。

### 批准后验证计划（仅 `DESIGN APPROVED` 后执行）

同前，另加：扫描证 `deleteEvicting`/物删只在 scopeMonitor、不在 budgetLock；`planEviction` 不扣 victim 账；
`settleAndReserve` 只对 DELETED_MATCHED 扣账；`reconcile` 收敛前 `status!=READY` 阻断 write；`javap` 证 governor 及嵌套
类型无 public 成员。Cloud `mvn -q clean package`（无 skip），追加 `## External Worker B - Implementation #1`。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Design Repair #3 到此停止，等待父级写
`DESIGN APPROVED` 或再次 `BLOCKED`。

## Parent Design Review #4 - DESIGN APPROVED - 2026-07-12

Repair #3 已关闭最后两个容量 P1：victim 在物理删除证实前保持 `EVICTING` 且继续计账，只对
`DELETED_MATCHED` settle 扣账；startup 改为 admission 关闭下的流式全量标量核算、有界 oldest heap、批次 reclaim 与
重扫收敛。typed exact context/owner-revision 门、package-private governor、受信 stateRoot 边界、5-new+2-modify 写集与
三个 caller dormant 映射不重开。结论：**DESIGN APPROVED，P0/P1/P2=0**，以下实现约束直接绑定，不再要求 Repair #4：

1. startup 标量 `bytes/count` 必须使用 checked/saturating addition；任何溢出或无法读取 size/identity 都进入
   `SATURATED_UNINDEXED`，admission 永久关闭，禁止 long wrap 后误判低于 cap。
2. 每轮 reclaim 必须记录 exact `DELETED_MATCHED` 进展；若超限而整批删除零进展，立即进入
   `SATURATED_UNINDEXED`，不得无限 rescan/hot loop。该状态只输出不含敏感路径的 operator reason，不后台 retry。
3. `FAILED/SKIPPED` victim 按原 `createdAt + scopeDigest/token` 排序键恢复 ACTIVE；不得因一次删除失败移动到队尾而改变
   deterministic global FIFO。`settleAndReserve` 只对 identity/size exact matched 的成功删除扣账。
4. 只允许批准的 Cloud 5 new + 2 modify；不改 DHXY、remote/lifecycle、caller、tests、host activation，不新增 raw Path/
   enum/list/download API。三 caller 在实现父级通过前继续 dormant。

现授权外部 Worker B 实施并向本文件追加 `External Worker B - Implementation #1`，运行 Cloud
`mvn -q clean package`（不 skip），回写源码/可见性/账盘状态机/无测试与构建证据。Worker 自审不算批准；父级将对落盘
源码和 fresh package 独立复核。**无已批准业务差异；按基线等价迁移。**

## External Worker B - Implementation #1 - 2026-07-12

严格按 `Parent Design Review #4 - DESIGN APPROVED` 的批准写集与 4 条绑定约束落地。**Cloud 5 new + 2 modify，DHXY 0**，
未改 remote/lifecycle/caller/tests/pom/resources，未 activate host，未新增 raw Path/enum/list/download API。三 caller 仍
dormant。以下为落盘证据。**Worker B 自述，不构成批准；请父级独立复核落盘源码与 fresh package。**

### 落盘写集（SHA-256 前 16 hex / 行数）

| 文件 | 类型 | SHA-256(16) | 行 |
|---|---|---|---|
| `host/CloudArtifactStore.java` | New（`public interface` + `record ArtifactId`） | `D690721110DB4A98` | 51 |
| `host/ScopedPngArtifactStore.java` | New（package-private `final`） | `72DC24340CB9377B` | 258 |
| `host/CloudTemplateAssets.java` | New（`public interface` + `record TemplateId`） | `59F44AD14B6D7519` | 33 |
| `host/CloudArtifactCapacityGovernor.java` | New（**package-private** `final`） | `ED2527250D5ECA76` | 466 |
| `cloudbrain/PackagedTemplateAssets.java` | New（`public final`，委托 `TemplateMatcher.readResourceImage`） | `7980FD81EB5F1A10` | 36 |
| `host/CloudServiceStorage.java` | Modify（+3 package-private：`stateRootKey`/`establishRealScopeRoot`/`resolveWithinRealScope`；现有成员未改） | `EF105DD1DA632BFE` | 156 |
| `host/CloudServiceConfiguration.java` | Modify（+2 dormant bean：`cloudArtifactStore`/`cloudTemplateAssets`；既有 bean 未改） | `04A93495A8E640D2` | 62 |

### 4 条绑定实现约束落实（源码位点）

1. **saturating/checked addition**：`CloudArtifactCapacityGovernor.addChecked` 用 `Math.addExact`，`ArithmeticException`→
   `SaturatedException`→`Status.SATURATED_UNINDEXED`（admission 永久关闭），杜绝 long wrap 误判低于 cap。startup 标量
   `scalarBytes/scalarCount` 全经 `addChecked`。
2. **零进展即 saturated、不 hot-loop**：`reconcile()` 循环中 `deleteOldestBatch` 返回实际删除数；`if (deleted == 0)
   throw SaturatedException("over-cap reclaim made zero progress")`。`SATURATED_UNINDEXED` 只 `log.warn` 不含敏感路径的
   reason，无后台 retry。
3. **FAILED/SKIPPED 按原 FIFO 键恢复**：`settleAndReserve` 对非 `DELETED_MATCHED` 者 `activeFifo.put(held.key(), held)`
   —— `FifoKey(order, scopeDigest, token)` 为 Entry 不可变原键，恢复到原确定性全局 FIFO 位置，不移队尾。只对
   `DELETED_MATCHED` 调 `debit`。
4. **仅 5-new+2-modify**：见上表；`git status` 确认只 host/ + `PackagedTemplateAssets.java` 变动（详见下）。

### 账/盘三阶段状态机（P1-1）落盘

- 单 `budgetLock` 元组权威：`planEviction`/`settleAndReserve`/`commit`/`rollback`/`releaseToken` 全在 `synchronized
  (budgetLock)` 内，**无任何 `Files.`/`ImageIO.`/move/delete**（账/盘分离）。
- victim 证实前保持计账：`planEviction` 只把 victim 从 `activeFifo` 移入 `evicting`（**不 debit**）；`deleteEvicting`
  在 `scopeMonitor` 内物删（`budgetLock` 外，NOFOLLOW+真实父根+size 复检）；`settleAndReserve` 只对 `DELETED_MATCHED`
  debit、FAILED 恢复 ACTIVE，再判 `fits` 后才 `pending` 预留新 token。`commit`=PENDING→ACTIVE（账已在 reserve 计）；
  `rollback`=exact-once 仅回滚新 token（`reservation.settled` 幂等）。
- 锁序 `encodePermits → budgetLock(纯账) → scopeMonitor(叶,纯 I/O)`：`ScopedPngArtifactStore.writePng` 先
  `encodePermits().acquire()`，`finally` release；文件写/删只在 `synchronized(governor.scopeMonitor(scopeDigest))`。

### startup 流式有界全量核算（P1-2，方案 1）落盘

- `streamScan` 用 `Files.newDirectoryStream` 逐 scope 目录流式，标量全量累计 + 大小 `SCAN_HEAP_K=4096` 的 oldest
  `PriorityQueue`（有界内存）；`*.png.tmp` 删除但**不计账**；每项 `NOFOLLOW_LINKS`+真实父根+`^af1-[0-9a-f]{32}\.png$`
  校验。超上限→`deleteOldestBatch`（每项再 NOFOLLOW/真实父根/size 复检删）→重扫；收敛后 `seedFromSurvivors` 建
  runtime index，超限则 `SaturatedException`。遍历 `IOException`→`SATURATED_UNINDEXED`（admission 永久关闭，operator
  reason 无路径）。收敛前 `status != READY`，`planEviction`/`writePng` 一律 fail-closed。

### 可见性证据（javap on target/classes）

- `CloudArtifactCapacityGovernor`：`final class ...`（**无 public**）；`javap | grep public` **零命中**——reservation/reclaim
  原语全 package-private，业务不可达。
- `CloudArtifactStore`/`CloudTemplateAssets`：`public interface`，公开方法仅
  `writePng(CloudTaskServiceExecutionContext,String,BufferedImage)`、`readPng(ctx,ArtifactId)`、`delete(ctx,ArtifactId)`、
  `loadTemplate(TemplateId)`——**无任何 Path / 枚举 / list / download / 容量 / reclaim 方法**；id 为 opaque record。
- `ScopedPngArtifactStore`：package-private `final implements CloudArtifactStore`。
- 首参强类型证据：三方法首参编译为 `com.yueyunfe.dhxy.cloudbrain.remote.CloudTaskServiceExecutionContext`（Worker A
  non-mintable 类型，只读消费未改其文件）。

### raw-API 扫描

- 对 7 个写集文件 grep `getResources|new JarFile|.toURI()|URLClassLoader|HttpURLConnection|.list()|listFiles`：**零命中**
  （无资源枚举/下载/HTTP）。governor 的 startup 回收用 `Files.newDirectoryStream`（package-private 内部有界扫描，非暴露
  给业务的枚举 API）。模板读取唯一经 `TemplateMatcher.readResourceImage`（不新增第二 loader）。

### 构建 / 测试 / 无副作用证据

- Cloud `mvn -q clean package`（**无任何 skip flag**）：**exit 0**。
- Surefire：**suites=4, tests=21, failures=0, errors=0, skipped=0**。未改 `src/test`（`git status -- src/test` 空）；本切片
  不新增测试文件（brief 冻结 tests），三个 new 方法体的运行时行为覆盖留待父级 fresh 复核 / 后续 caller cohort 激活。
  （注：与更早 CR262 期记录的 36 测试差异，源于云端树此后被其它切片改动，非本切片所致——本切片零测试改动。）
- `git status`（cloud，只读）：变动仅 `?? cloudbrain/PackagedTemplateAssets.java` 与 `?? cloudbrain/host/`（host/ 整包为
  在途 untracked，我的 5 new + 2 modify 均落此二处）；`pom.xml` 未被我改；`src/test` 空；DHXY `com/yueyunfe` 无文件（写集
  cloud-only）。
- 未启动 server/host/poller/UI/capture/OCR/input（仅 `mvn` 编译+测试）；未 `git add/commit`；未触碰 Worker A 的
  `remote/**`、`api/RemoteTaskRunEndpoint.java`、`RemoteTaskRunErrorCode`、`CloudTaskRunExecutionGate`、DHXY `cloud/remote/`
  （仅只读 import `CloudTaskServiceExecutionContext`/`RemoteTaskRunAuthorization`/`RemoteTaskRunScope`）。两仓其它在途 dirty
  未被覆盖。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Implementation #1 到此停止，等待父级对落盘源码
与 fresh package 的独立复核（`APPROVED` / `BLOCKED` / 新任务）。

## Parent Implementation Review #1 - BLOCKED - 2026-07-12

父级已审完 5-new+2-modify 落盘源码；七文件 SHA 与 Worker 报告一致，并在 H3/B 写入稳定后的当前 Cloud 树独立运行 fresh
`mvn -q clean package`，exit 0，4 suites / 21 tests，failures=0、errors=0、skipped=0，JAR SHA-256
`A2C6B7D1AECAFFA80F5ED9E3AB049C24BBAD7339308B28145EA89FC136CDBFFB`。编译门通过，但容量权威仍有并发/锁序缺陷，
结论：**BLOCKED，P0=0/P1=5/P2=2**。

### P1-1：encode permit 在 PNG 编码之后获取，不能限制并发编码内存

- 证据：`ScopedPngArtifactStore.writePng:72` 先 `encodePng(image)` 生成最多 8 MiB byte[]，`:78` 才
  `encodePermits.acquire()`；与批准的 `acquire -> encode -> finally release` 相反。
- 影响：任意数量并发 caller 都可先完成大图编码并占用堆，4 permits 只限制后续写路径，不能实现编码内存硬门。
- 返修条件：在任何 `encodePng`/ByteArrayOutputStream 分配前取得 permit；interrupt 返回 empty；唯一 finally exact release。

### P1-2：业务 delete 与 eviction 的两阶段竞态会产生 ghost ACTIVE 并低估 totals

- 证据：`ScopedPngArtifactStore.delete:146-158` 先在 scopeMonitor 物删，解锁后调用 `releaseToken`；并发
  `planEviction` 可在两步之间把同 Entry 从 activeFifo 移到 evicting。`releaseToken:461-469` 只移 byToken/activeFifo 并 debit，
  不识别 evicting；随后 `settleAndReserve:408-419` 会把物删失败/文件已不存在的 held entry 恢复 ACTIVE，却不重新 credit。
- 影响：activeFifo 留下无文件 ghost，`totalBytes/count` 已扣除，容量权威低于真实/索引状态；后续 eviction 可反复选择 ghost，
  破坏 hard cap 与收敛。
- 返修条件：业务 delete 也必须走 budgetLock 标记/scopeMonitor 物删/budgetLock settle 的同一 entry 状态机；与 eviction 对同
  `(scopeDigest,token)` 只能有一个 current operation。文件已由 exact 并发路径删除应作为 matched settlement，不得恢复 ghost；
  禁止 scopeMonitor→budgetLock 嵌套或 I/O under budgetLock。

### P1-3：governor 的 entry authority 只按 token，跨 scope 同 token 会互相覆盖

- 证据：`byToken`、`evicting`、`pending` 均为 `Map<String,Entry>`（`:138-140`），所有 put/remove 都只用 token；磁盘允许
  不同 `<scopeDigest>/af1-<token>.png` 同时存在，startup seed 和人工复制/随机碰撞都可形成同 token。
- 影响：一个 scope 的 seed/release/eviction/rollback 可覆盖或删除另一个 scope 的账目，导致 tenant 隔离与 root totals 错乱。
- 返修条件：所有 governor map/current-operation/reservation key 使用不可混淆的 `ArtifactKey(scopeDigest,token)`；plan、delete result、
  settle、commit、rollback、business delete 全链保持 full key，拒绝 duplicate full key，跨 scope token 相同互不影响。

### P1-4：startup seed 在 budgetLock 内遍历文件系统

- 证据：`seedFromSurvivors:295-328` 在 `synchronized(budgetLock)` 内执行两层 DirectoryStream、toRealPath、size、mtime；
  与类不变量“budgetLock performs no filesystem I/O”和批准锁序冲突。
- 影响：启动核算期间所有 admission/settle 被不受界的磁盘 I/O 阻塞；未来若与 scopeMonitor 路径组合，重新引入锁逆序风险。
- 返修条件：锁外流式构建有界 survivor snapshot（收敛后 count 已受 root cap 约束），完成 real-root/NOFOLLOW/size/mtime 校验；
  最后一次 budgetLock 内只做纯内存 checked seed，并确认 status 仍 RECONCILING、maps/totals 初始为空。任何扫描失败转
  SATURATED_UNINDEXED。

### P1-5：写失败 rollback 先释放账，best-effort 删除失败会留下未计账文件

- 证据：`writePng:99-103` catch 中先 `governor.rollback(reservation)`，随后 `bestEffortDelete(token)`；cleanup 结果被吞。
- 影响：ATOMIC_MOVE 已成功、后续 owner/commit 阶段异常且删除失败时，磁盘仍有 artifact，但 reservation 已 debit，hard cap 低估。
- 返修条件：失败结算必须先在 scopeMonitor 得到 target/tmp 的 exact 删除结果，再由 budgetLock 决定 rollback；若 target 无法证实
  删除，保留计账并转为可 reclaim 的 orphan/ACTIVE entry，或 latch SATURATED_UNINDEXED，绝不能静默 debit。

### P2-1：startup FIFO tie 没有 scope/token 确定性排序

- 证据：`streamScan` heap 与 `deleteOldestBatch` 都只比较 lastModified（`:234-235`, `:282`），同毫秒文件的保留集合/删除顺序
  取决于 DirectoryStream/PriorityQueue 非稳定顺序。
- 影响：与批准的 `createdAt + scopeDigest/token` deterministic global FIFO 不一致。
- 返修条件：heap 选择与 batch 排序统一使用 `(lastModified,scopeDigest,token)` 全键；max-heap 反序也必须覆盖全键。

### P2-2：TemplateId 把非 canonical 输入静默归一化

- 证据：`PackagedTemplateAssets:32` 对 resourceId `trim().replace('\\','/')`；因此带空白或反斜杠的非 canonical id 被接受，
  与接口“canonical allowlisted resource id”和注释“backslash rejected”矛盾。
- 影响：同一模板存在多种外部标识，审计/digest/caller contract 可漂移。
- 返修条件：不 trim、不替换；只在原字符串逐字满足前缀、字符 allowlist、无 `..`/反斜杠/首尾空白时读取，否则 empty。

同一外部 Worker B 只在批准的七文件内提交 `Implementation Repair #1`；不得扩展 public API、caller、tests、remote/DHXY 或
激活 host。返修报告须逐项给出 full-key 状态机、business-delete race、失败写账盘结算和 budgetLock 零 I/O 证据，再运行
Cloud `mvn -q clean package`（不 skip）。Worker 自审仍不算批准。**无已批准业务差异；按基线等价迁移。**

## External Worker B - Implementation Repair #1 - 2026-07-12

逐条关闭 `Parent Implementation Review #1` 的 P1-1..P1-5 + P2-1/P2-2。**仅改批准七文件中的 3 个**
（`CloudArtifactCapacityGovernor.java`、`ScopedPngArtifactStore.java`、`PackagedTemplateAssets.java`）；未扩展 public API、
未动 caller/tests/remote/DHXY、未激活 host。**Worker B 自述，不构成批准。**

### 变更文件 SHA-256(16) / 行数

| 文件 | SHA-256(16) | 行 |
|---|---|---|
| `host/CloudArtifactCapacityGovernor.java` | `19D67B3E9975A265` | 514 |
| `host/ScopedPngArtifactStore.java` | `CEC051F940ACC73A` | 285 |
| `cloudbrain/PackagedTemplateAssets.java` | `5E9427D58D419528` | 36 |

其余 4 文件（`CloudArtifactStore`/`CloudTemplateAssets`/`CloudServiceStorage`/`CloudServiceConfiguration`）本轮 SHA 不变。

### 逐条返修（源码位点）

- **P1-1（permit 晚于编码）**：`ScopedPngArtifactStore.writePng` 现在 `governor.encodePermits().acquire()` 在**任何**
  `encodePng`/`ByteArrayOutputStream` 分配**之前**；`InterruptedException`→`Thread.interrupt()`+返回 empty；唯一 `finally`
  `if (acquired) release()`。编码内存受 4-permit 硬门约束。
- **P1-2（business-delete×eviction 竞态产 ghost）**：业务 delete 改走与 eviction **同一** full-key 状态机——
  `governor.planBusinessDelete(key)` 在 `budgetLock` 内把该 entry 从 `activeFifo` 移入 `evicting`（standby 独占），
  `deleteEvicting` 在 scopeMonitor 物删，`settleBusinessDelete` 在 `budgetLock` 结算。因 entry 一旦进 `evicting` 即离
  `activeFifo`，eviction 与 business-delete 对同 `(scopeDigest,token)` **只能有一个 current operation**。物删发现文件已不存在
  报 `ALREADY_GONE`，`settleVictims` 视其为 matched（debit + `byKey.remove`），**不恢复 ghost**；仅 `FAILED`（文件仍在）才按
  **原 FifoKey** 恢复 ACTIVE 且不重复 credit。旧的 `releaseToken`/`releaseOwned` 旁路已删除。
- **P1-3（token 覆盖跨 scope）**：新增 `record ArtifactKey(scopeDigest, token)`；`byKey`/`evicting`/`pending` 全部改
  `Map<ArtifactKey,Entry>`，`deleteEvicting` 结果 `Map<ArtifactKey,DeleteOutcome>`；plan/delete/settle/commit/rollback/
  business-delete 全链 full key。跨 scope 同 token 互不影响（javap 证 `ArtifactKey` 双分量 record）。
- **P1-4（seed 在 budgetLock 内做 I/O）**：`reconcile` 全部 DirectoryStream/toRealPath/size/mtime 在 `budgetLock`
  **外**完成：收敛后 `collectSurvivors`（锁外，收敛后 ≤cap 有界）构建 survivor 快照并做 real-root/NOFOLLOW/size/mtime
  校验；仅最后一段 `synchronized(budgetLock)` 做**纯内存** checked seed，并先断言 `status==RECONCILING` 且 maps/totals 为空。
  任一扫描/遍历异常→`SATURATED_UNINDEXED`。
- **P1-5（写失败先 debit 再 best-effort 删）**：catch 改为**先证盘再定账**：`resolveWriteFailure(token)` 在 scopeMonitor 内清
  tmp、尝试删 target 并返回"删后 target 是否仍在盘"；`targetRemains==true` → `governor.commit(reservation)`（保留计账为可
  reclaim 的 ACTIVE orphan，账≥真实占用）；`false` → `governor.rollback(reservation)`（debit）。绝无静默 debit 留未计账文件。
- **P2-1（startup FIFO tie 无全键）**：新增 `Comparator<ScanEntry> FIFO_FULL_KEY =
  (lastModified, scopeDigest, token)`；`streamScan` 的 oldest max-heap 用 `FIFO_FULL_KEY.reversed()`，`deleteOldestBatch`
  用 `FIFO_FULL_KEY` 升序，与运行期 `FifoKey` 全键确定性一致。
- **P2-2（TemplateId 静默归一化）**：`PackagedTemplateAssets.loadTemplate` **不 trim、不 replace**；仅当原字符串逐字满足
  `equals(trim())`（无首尾空白）、无 `..`、无反斜杠、`startsWith("images/template/")` 且匹配字符 allowlist 时才读取，否则
  empty。一个模板恰一个外部标识。

### budgetLock 零 I/O 证据

- 7 处 `synchronized(budgetLock)`（L237 reconcile 纯内存 seed、L369 planEviction、L404 planBusinessDelete、L449
  settleAndReserve、L467 settleBusinessDelete、L500 commit、L513 rollback）**均只做内存账**；所有 `Files.`/`ImageIO.` 只在
  `streamScan`/`collectSurvivors`/`deleteOldestBatch`/`physicalDelete`（全部 budgetLock 外，或 scopeMonitor 内）。锁序
  `encodePermits → budgetLock(纯账) → scopeMonitor(叶,纯 I/O)` 保持，无 scopeMonitor→budgetLock 嵌套。

### 构建 / 可见性 / 无副作用证据

- Cloud `mvn -q clean package`（**无 skip**）：**exit 0**；Surefire **suites=4, tests=21, failures=0, errors=0, skipped=0**；
  fresh JAR SHA-256 `86FA2EE654FACE0633E1894D24BE72CA099FB946F8F899E6CB67FBB30D0B5F43`。
- `javap`：`CloudArtifactCapacityGovernor` 仍 `final class`、**零 public 成员**；`ArtifactKey` 为双分量 record；两业务接口
  未变（无 Path/枚举/list/download）。
- `git status`（cloud，只读）：仅 `?? PackagedTemplateAssets.java` 与 `?? host/`（在途 untracked）；`src/test` 空；**`pom.xml`
  的 ` M` 为本切片开工前既有的他方在途改动，非本轮所改，已保护未覆盖**；DHXY 未碰。未启动 server/host/poller/UI/capture/
  OCR/input；未 `git add/commit`；未触碰 Worker A `remote/**`、`api/RemoteTaskRunEndpoint.java`、`RemoteTaskRunErrorCode`、
  `CloudTaskRunExecutionGate`（仅只读 import `CloudTaskServiceExecutionContext`/`RemoteTaskRunAuthorization`/
  `RemoteTaskRunScope`）。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Implementation Repair #1 到此停止，等待父级对落盘
源码与 fresh package 的独立复核。

## Parent Implementation Review #2 - BLOCKED - 2026-07-12

父级已逐方法复核三份返修源码。上一轮 encode permit、business-delete/eviction full-key current operation、跨 scope key、
budgetLock 零 I/O、失败写先证盘后结算、full FIFO tie 与 verbatim template id 的原问题均已按方向修复；Worker 报告的
Cloud package 也为 exit 0。但新 token admission 与 startup 文件所有权仍有两个 P1，另有两个 P2，当前不运行父级 fresh
package 作为放行证据。结论：**BLOCKED，P0=0/P1=2/P2=2**。

### P1-1：新 token 未做 full-key collision admission，失败 cleanup 可删除旧 artifact 并留下 ghost ACTIVE

- 证据：`ScopedPngArtifactStore.writePng:81` 只调用一次 `mintToken()`；
  `CloudArtifactCapacityGovernor.settleAndReserve:447-457` 未检查新 `ArtifactKey` 是否已存在于
  `byKey/evicting/pending`，直接 `pending.put` + credit。若 token 与本 scope 已有 artifact 相同，随后
  `writeArtifactFile:215-231` 的 target 已存在而 move 失败，catch 中 `resolveWriteFailure:242-263` 会把该 target 当成本次写入
  删除；new reservation rollback 后，旧 `byKey/activeFifo` 仍保留但文件已消失。
- 影响：极低概率随机碰撞、startup survivor 或受控人工恢复出的同 token 都能触发旧 artifact 数据删除、old ACTIVE ghost、
  账盘分离；`pending.put` 还可能覆盖并发 pending record。128-bit 概率不能替代设计明确要求的 collision correctness。
- 返修条件：Repair #2 必须在 `budgetLock` 内对 full `ArtifactKey(scopeDigest,token)` 原子检查
  `byKey/evicting/pending`，任何命中都不得 put/credit。store 使用有界重铸（设计原约束至少一次 retry）取得未占用 reservation；
  collision 不能进入 target cleanup。写路径必须跟踪“本 attempt 是否已成功创建/移动 target”，只有能证明 target 属本 attempt
  时才能删除；已有 target 永远不能由失败 cleanup 删除。commit/rollback 继续 exact-once，不覆盖 map entry。

### P1-2：startup 用 `endsWith(".png.tmp")` 删除共享 scope 根中不属于本 adapter 的文件

- 证据：`CloudArtifactCapacityGovernor.streamScan:280-286` 对每个 hashed scope 目录中任意
  `name.endsWith(".png.tmp")` 都 `deleteIfExists`；scope 目录由通用 `CloudServiceStorage` 共享，并非 artifact store 专属目录。
- 影响：其它 Cloud Service 若使用合法的 `foo.png.tmp` 原子写中间文件，会被本 governor 在 startup reconcile 无条件删除，
  形成跨组件数据破坏。
- 返修条件：tmp owner 必须和 final artifact 一样严格，只清理 canonical
  `^af1-[0-9a-f]{32}\\.png\\.tmp$`；删除前做 NOFOLLOW regular-file 与 real-parent exact 检查。其它文件名一律忽略，不枚举为
  artifact、不删除。

### P2-1：write 失败后 `ownerLedger` 可能保留不可达 owner entry

- 证据：`ownerLedger.put` 在 `ScopedPngArtifactStore:97`，catch/`resolveWriteFailure` 路径没有对应 remove。若 commit 或其后的
  RuntimeException 进入 catch，方法返回 empty，但 owner map 仍可能持 token。
- 影响：长期失败可累积不可达 owner 元数据；target 被保留为 reclaimable orphan 时还会出现“调用方无 ID、owner ledger 却仍
  声称可读”的不一致。
- 返修条件：失败路径在盘/账结算完成后 exact remove owner entry；只有成功 commit 且即将返回 ID 时发布 owner。发布顺序要
  防止成功返回一个未入账/未拥有的 ID。

### P2-2：实现把模板 allowlist 收窄为 ASCII，违背批准设计且拒绝现有 CJK packaged templates

- 证据：`PackagedTemplateAssets:23` 的 regex 只有 `[A-Za-z0-9_./-]`；批准 Design #1 明确 allowlist 包含 CJK，Cloud
  `images/template/map_label/` 当前实际存在 `白骨山.png`、`大雁塔五层.png` 等 61 个中文资源。
- 影响：通用 `CloudTemplateAssets` 对合法 packaged template 返回 empty，未来 map-label caller 会出现无模板命中；这不是
  canonical 修复所要求的行为。
- 返修条件：保持 verbatim/no-trim/no-backslash/no-traversal，同时恢复批准的 CJK 文件名字符范围；不得开放 path、枚举或第二
  loader。

同一 B 只在既有七文件内追加 `External Worker B - Implementation Repair #2`；不改 public API/caller/tests/remote/DHXY/
host activation。返修后重跑 Cloud `mvn -q clean package`（不可 skip），父级再做 fresh package 与源码复审。
**无已批准业务差异；按基线等价迁移。**

## Parent Next Task Handoff - 2026-07-12

本 artifact/template 切片已父级 APPROVED。外部 Worker B 不停机，下一任务切换到：
`docs/superpowers/plans/reports/2026-07-12-cloud-auto-combat-panel-service-worker-b.md`。
每 5 分钟读取新固定日志，只按其中 `Parent Task Brief #1` 先追加 Design #1；父级批准前零 Java。

## External Worker B - Implementation Repair #2 - 2026-07-12

逐条关闭 `Parent Implementation Review #2` 的 P1-1/P1-2/P2-1/P2-2。**仅改批准七文件中的 3 个**
（`CloudArtifactCapacityGovernor.java`、`ScopedPngArtifactStore.java`、`PackagedTemplateAssets.java`）；未扩 public API、
未动 caller/tests/remote/DHXY、未激活 host。**Worker B 自述，不构成批准。**

### 变更文件 SHA-256(16) / 行数

| 文件 | SHA-256(16) | 行 |
|---|---|---|
| `host/CloudArtifactCapacityGovernor.java` | `B3DB66C7F0C43FC3` | 534 |
| `host/ScopedPngArtifactStore.java` | `CF7E857C9AF293F1` | 320 |
| `cloudbrain/PackagedTemplateAssets.java` | `5A7000814D3735AB` | 39 |

其余 4 文件本轮 SHA 不变。

### 逐条返修（源码位点）

- **P1-1（新 token 无 full-key collision admission）**：`settleAndReserve` 拆为 `settleWriteVictims`（budgetLock 只结算
  victim）+ `tryReserveToken(scopeDigest,newBytes,token)`（budgetLock 内**原子** full-key 检查：`byKey/evicting/pending`
  任一命中即返回 `COLLISION`，**不 put/不 credit**；`NO_ROOM` 时不预留）。store `writePng` 有界重铸——最多
  `TOKEN_MINT_ATTEMPTS=4` 次 `mintToken()`+`tryReserveToken`，`RESERVED` 成功、`COLLISION` 重铸、`NO_ROOM`/耗尽即
  fail-closed empty。写路径用 `boolean[] moved` 跟踪本 attempt 是否成功 `ATOMIC_MOVE`（无 `REPLACE_EXISTING`，target 已存在
  则 move 失败、`moved=false`）；失败 cleanup 仅当 `moved==true` 才 `deleteOwnTargetAfterFailure`，否则 `cleanupOwnTmp`
  **绝不删已存在 target**。commit/rollback 仍 exact-once、不覆盖 map entry。
- **P1-2（startup 删任意 `*.png.tmp`）**：`streamScan` 的 tmp 清理改为仅匹配 canonical `^af1-[0-9a-f]{32}\.png\.tmp$`
  且删前 `NOFOLLOW_LINKS` regular-file + `toRealPath().getParent()==realScopeDir` exact 校验；其它文件名一律忽略、不枚举
  不删。共享 scope 根中他组件的 `foo.png.tmp` 不受影响。
- **P2-1（write 失败留不可达 owner）**：`ownerLedger.put` 移到 `governor.commit(reservation)` **之后、返回 id 之前**
  发布（成功路径唯一发布点）；任何失败 catch 先 `ownerLedger.remove(token)`。target 被保留为 reclaimable orphan 时
  **无 owner**，业务不可 read/delete、仅 governor 容量可回收，消除"无 ID 却声称可读"的不一致。
- **P2-2（模板 allowlist 收窄成 ASCII，拒绝 CJK）**：`ALLOWED_ID` 恢复为
  `^images/template/[\p{L}\p{N}_./-]+\.png$`（`UNICODE_CHARACTER_CLASS`，含 CJK），同时保持 verbatim（`equals(trim())`
  无首尾空白）、无反斜杠、无 `..`。不开放 path/枚举/第二 loader。

### 构建 / 可见性 / P2-2 实证 / 无副作用

- Cloud `mvn -q clean package`（**无 skip**）：**exit 0**；Surefire **suites=4, tests=21, failures=0, errors=0,
  skipped=0**；fresh JAR SHA-256 `8E45EA4273574920DA79976DA0CC4030307D9050F9B2A6C33C851901CCF6D221`。
- **P2-2 运行时实证**（scratchpad 反射驱动，不启动 host/server）：`loadTemplate("images/template/map_label/白骨山.png")`
  → **present=true**（CJK 恢复）；`images/template\x.png`（反斜杠）、`images/template/../x.png`（穿越）、` images/template/x.png`
  （前导空格）→ 均 **empty**（verbatim 拒绝）。
- `javap`：`CloudArtifactCapacityGovernor` 仍 `final class`、零 public 成员；新增 `ReserveOutcome`/`TokenReservation`
  均 package-private；两业务接口未变（无 Path/枚举/list/download）。
- budgetLock 零 I/O 保持：`tryReserveToken`/`settleWriteVictims`/`settleBusinessDelete`/`commit`/`rollback` 均只内存账；
  所有 `Files.`/`ImageIO.` 仍只在 scopeMonitor（store 写/删）或 budgetLock 外（governor scan）。锁序不变。
- `git status`（只读）：仅 `?? PackagedTemplateAssets.java` 与 `?? host/`（在途 untracked）；`src/test` 空；`pom.xml` 的 `M`
  为开工前他方在途改动（非本轮，已保护）；DHXY 未碰。未启动 server/host/poller/UI/capture/OCR/input；未 `git add/commit`；
  未触碰 Worker A `remote/**`/`api/RemoteTaskRunEndpoint.java`/`RemoteTaskRunErrorCode`/`CloudTaskRunExecutionGate`（仅只读
  import 其 public 类型）。

`无已批准业务差异；按基线等价迁移。` Worker B 自审仅 QA，不构成批准。Implementation Repair #2 到此停止，等待父级对落盘
源码与 fresh package 的独立复核。

## Parent Implementation Review #3 - APPROVED - 2026-07-12

父级逐段复核 `ScopedPngArtifactStore`、`CloudArtifactCapacityGovernor`、`PackagedTemplateAssets` 最新落盘源码，结论：
**APPROVED，P0/P1/P2=0。** full-key `byKey/evicting/pending` collision admission + 4 次有界重铸成立；未 move 的 attempt
只清自己的 tmp，绝不删既存 target；owner 只在 commit 后发布且失败先移除；startup 只清 canonical
`af1-[0-9a-f]{32}.png.tmp` regular file；CJK `\p{L}` allowlist 与 traversal/backslash/verbatim 拒绝均保持。budgetLock
内仍零 I/O，public typed API/host/caller 均未扩大。

父级 fresh Cloud `mvn -q clean package` exit 0，4 suites/21 tests，failures/errors/skipped 均 0；JAR SHA-256
`3C0261F0D1DD56A056AD28F89D01B9AFB349B771B7A4FFD8CA39F76D68729606`。`git diff --check` 无 error，仅既有
LF/CRLF warning。artifact/template adapter 实现门关闭；三 caller 与 host 仍 dormant，本结论不授权生产切换。

**无已批准业务差异；按基线等价迁移。**
