# CR271 Cloud-native ImageProcessorService Collaboration Log

> Append-only collaboration log. Do not edit or delete earlier entries. The external worker may append only the requested design/repair/implementation sections. Parent approval is required before Java changes.

## Parent Brief #1 - 2026-07-12

### Context

- Repositories: `D:\mavenProject\DHXY` and `D:\mavenProject\dhxy-cloud-brain`.
- Protect all existing dirty work. No reset, checkout, clean, revert, Git staging, commit, or push.
- CR271 has already closed the Cloud task/service context and retained-action typed Service port slices with `P0/P1/P2=0`.
- `com.bot.dhxy.cloud.task.ImageProcessorService` and its result DTOs already exist in Cloud Brain.
- DHXY `CloudImageProcessor` is **not** a Cloud business leaf. It delegates to the local `ImagePreprocessWashedImageClient`, which reads local `TaskExecutionContextHolder`, local HWND/geometry/path metadata, and calls the remote `IMAGE_PREPROCESS` decision service. Copying those classes into Cloud would create cloud-to-itself transport and reintroduce local runtime authority.
- Cloud Brain already owns the actual algorithms in `com.yueyunfe.dhxy.cloudbrain.ImageAlgorithms` and the current HTTP decision path in `DecisionEngine.imagePreprocess(...)`.

### Design objective

Design one Cloud-native, in-process implementation of the existing `ImageProcessorService` contract so subsequently migrated Services can invoke the same image operations without HTTP/self-calls and without local window/input/runtime authority. Preserve the current operation/result/fail-closed business contract. Do not implement yet.

### Required design coverage

1. Exact proposed files and visibility. Prefer one real ownership boundary; no wrapper nesting or duplicate algorithm implementation.
2. Mapping for every `ImagePreprocessOperation` currently exposed by `ImageProcessorService`, including image outputs, scalar/structured diagnostics, `ROUTE_PACKED_LINE_MASK`, `ROUTE_DESTINATION_SEGMENTS`, fingerprint operations, and team-tooltip stats.
3. How the in-process path reuses `ImageAlgorithms` without broadening raw/public access unnecessarily and without duplicating the HTTP diagnostic parser.
4. Exact compatibility semantics versus DHXY `CloudImageProcessor`: required-output validation, empty-result behavior, status/reason, metadata, coordinate spaces, list ordering, numeric parsing, image ownership/flush, and exception handling.
5. `washToPath` ownership: Cloud code must never interpret a DHXY-local filesystem path as Cloud authority. State whether the method is Cloud-local debug/output only, requires a later context-scoped artifact adapter, or must remain unsupported/fail-closed until callers migrate. Inventory affected callers before choosing.
6. Spring/host reachability: the bean may be compile-ready for dormant migrated Services, but must not start Task host/poller/UI/capture/input or expose a raw HTTP/poll/completion bypass.
7. Dual-authority prevention: `CloudImageProcessor` and `ImagePreprocessWashedImageClient` stay local and are not copied; the Cloud implementation must use the same algorithm owner as the existing decision endpoint so behavior cannot drift.
8. Concurrency, memory/capacity, tenant isolation, mutable `BufferedImage` ownership, and failure/observability risks.
9. Baseline evidence and a compatibility table covering current `ImageProcessorService` callers that can migrate unchanged, callers blocked by local `Path`/HWND/runtime assumptions, and required later adapters.
10. Verification plan: no new/restored tests; fresh Cloud `mvn -q clean package` without skip flags; static reachability/visibility and `src/test`-unchanged evidence. No DHXY Java change unless separately approved.

### Hard constraints

- Design only in the first worker turn. Append `## External Worker - Design #1` to this file and stop.
- Do not modify Java, Maven, resources, CR status, dashboard, or any other document before parent writes explicit `DESIGN APPROVED` here.
- Do not change task phase/retry/fallback/click/navigation/stop/pause semantics.
- Do not add TTL, takeover, retry, public raw request/poll/outcome APIs, local HWND/window/input/runner authority, or a second image algorithm owner.
- Do not start or call Task host, poller, UI, capture, OCR, input, HTTP server, or production cutover.
- No local tests. No Git mutation.
- `无已批准业务差异；按基线等价迁移。`

## External Worker - Design #1 - 2026-07-12

### 结论与风险级别

- 建议新增一个 Cloud-native、in-process 的 `ImageProcessorService` 实现，并把现有 HTTP
  `DecisionEngine.imagePreprocess(...)` 改为调用同一个 `ImageAlgorithms.process(...)` 分派入口。算法、operation
  分派、排序和数值计算只保留一份；HTTP 路径只负责把同一 typed result 序列化为旧 diagnostics，in-process 路径
  直接映射 typed result，不调用 HTTP，也不反向解析 HTTP diagnostics。
- `P0=0`。
- `P1=1`：`washToPath(Path, Path, ...)` 无法在 Cloud 安全地兼容当前 DHXY caller。三个 caller 的 `Path` 都是
  DHXY 窗口作用域本地临时文件，不是 Cloud artifact。建议 Cloud 实现对该方法无条件 fail-closed，既不读
  `rawPath` 也不写 `outputPath`；对应 caller 迁移前必须改为内存图像调用，并由后续 tenant/context-scoped artifact
  adapter 负责可选调试落盘。**父级决策点：批准该 fail-closed 边界，或另开 artifact adapter 设计；不得把任意
  `Path` 当作 Cloud 本地路径。**
- `P2=1`：in-process 结果可构造兼容的 `ImagePreprocessCloudDecision`，但不存在真实 HTTP
  `CloudDecisionResult`，所以 `decision.cloudResult` 必须为 `null`；为避免每次重新 PNG/Base64 编码，建议
  `decision.washedImagePayloadBase64/sha256` 也为空，直接图像只由 `ImageProcessorResult.image` 承载。当前精确
  caller 没有读取 `decision()`，业务必需输出完全由 `ImageProcessorResult` 判定。**父级决策点：确认按 Service
  contract 等价而非伪造 transport provenance；若要求 transport DTO 字段逐项等价，将产生额外 PNG/Base64
  内存峰值，且仍无法伪造真实 `cloudResult`。**
- 除上述明确边界外：`无已批准业务差异；按基线等价迁移。`

### 基线与在途保护证据

- DHXY：分支 `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；全仓 dirty 状态已读，
  本设计不修改 DHXY Java。`ImageProcessorService.java` SHA-256 为
  `1E86E86CF5B4AEEE74964828F026B8B6C600F141C3201E14B604554988E54B76`；Cloud 副本同 hash。
- Cloud Brain：分支 `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`；全仓 dirty 状态已读。
  `DecisionEngine.java` 已有 131 行无关在途增量，实施时只能在现有 `imagePreprocess(...)` 方法内做定点改动，
  不覆盖 `NAVIGATION_ROUTE_PLAN`、MINIMAP 或 CR267 变更。`CloudServiceConfiguration.java` 和
  `ImageProcessorService.java` 当前均是父级/并行工作的 untracked 文件，实施必须在当前内容上追加，不得重建。
- 当前关键文件 SHA-256：`ImageAlgorithms.java`
  `8F0B72BDC31F463F6B544B39F915089CAB938358F7FB6F9BB0AEAE5211C2B187`；`DecisionEngine.java`
  `1CDB02422C2488CDDF9CB121052FB0D25CD4180A01B68EC2F210C071EB253F40`；
  `CloudServiceConfiguration.java`
  `7B94FA396DB2F73C8ADEAB111A61F3C2D89E7D186235EEC58BC3EF6A4B3728A7`。实施前应重新核对 scoped status/hash，
  若变化则基于最新内容重放最小补丁。

### 设计不变量

1. `ImageAlgorithms` 继续是唯一算法属主；不把算法复制到 `com.bot.dhxy.cloud.task`，不复制 DHXY
   `CloudImageProcessor`、`ImagePreprocessWashedImageClient` 或 `ImagePreprocessCloudService`。
2. `ImageAlgorithms.process(raw, operation, parameters)` 是 HTTP 与 in-process 唯一 operation 分派点；
   `DecisionEngine.imagePreprocess(...)` 不再自行执行 `addImageDiagnostics + wash` 两段分派。
3. in-process 调用不读取 `TaskExecutionContextHolder`、HWND、窗口几何、runner、input queue、capture 或本地路径；
   `RequestMetadata` 仅作诊断上下文，`parameters.first` 是唯一现有算法参数。
4. 不新增 retry、TTL、fallback、takeover、线程池、缓存或持久状态；一次调用只处理一次输入并同步返回。
5. 所有必需输出继续经过一个与 `CloudImageProcessor.requireCloudOutput(...)` 等价的终检；算法返回
   `EXECUTED` 但缺少该 operation 必需输出时统一降为 `REQUIRED_FAILURE`。
6. 输入 `BufferedImage` 所有权始终属于 caller，processor/algorithm 不 flush、不缓存、不修改输入；返回的新图像
   所有权交给 caller，caller 负责且仅负责一次最终 `flush()`。
7. 候选顺序、空列表、坐标和四位小数 round-trip 保持当前 HTTP 可见行为；不利用 in-process 可见的额外像素统计
   改变现有 DTO 值，例如 `GreenTextBand.pixels` 仍返回 `0`。
8. Cloud bean 只在显式 `CloudServiceHost.create(...)` 的 tenant host 内可达；不把它挂到
   `CloudBrainServer` routes，不启动 host/poller/UI/capture/OCR/input，也不暴露 raw request/poll/completion API。

### 精确拟改文件与可见性

1. **新增** `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\CloudNativeImageProcessor.java`
   - `public final class CloudNativeImageProcessor implements ImageProcessorService`；之所以 class/constructor 为
     `public`，仅因为 bean factory 位于 `com.yueyunfe.dhxy.cloudbrain.host` 子包。
   - 不加 `@Service`，不增加任何 public raw/generic API；公开面仅为既有接口方法。
   - 所有接口方法直接调用一个类内单层 `process(...)`，再做 typed result 映射与 required-output 终检；不建立
     `prepare/handle/resolve` wrapper 链。
2. **修改** `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\ImageAlgorithms.java`
   - class 继续 package-private `final`；现有 low-level 算法继续 private/package-private，不扩大可见性。
   - 新增 package-private `process(...)` 和 package-private nested immutable records（operation status、washed image、
     candidates、stats、canonical diagnostics）。这些类型只供同包的 processor 与 `DecisionEngine` 使用。
   - 把现有 diagnostics helper 改为从同一 typed result 序列化；不改变阈值、排序、颜色判断或算法顺序。
3. **修改** `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`
   - 仅替换现有 `imagePreprocess(...)` 内部的算法调用为 `ImageAlgorithms.process(...)`，保留原
     unreadable/no-result/error decision、confidence、algorithm、debugToken、coordinateSpace、PNG payload 与 flush
     行为；其它 dirty 区域不动。
4. **修改** `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\host\CloudServiceConfiguration.java`
   - 新增一个 `@Bean ImageProcessorService imageProcessorService()`，显式 `new CloudNativeImageProcessor()`。
   - 不扩大 `@ComponentScan`，不修改 `CloudServiceHost`、`CloudBrainServer`、routes、pom 或 resources。
- **不修改** `ImageProcessorService.java`、`ImagePreprocessOperation.java`、任何 DHXY Java/Maven/resources。

### 逐 operation 映射

| Operation / 接口 | 唯一算法路径 | `ImageProcessorResult` 必需输出与兼容细节 |
|---|---|---|
| `WASH_YELLOW` / `washYellowText` | `washYellowText` | 新白底黑/白前景 PNG 对应的内存图；必须有 image。保留当前附带 `pixelCount=countForeground(washed)`。 |
| `WASH_GREEN` / `washGreenTextToBlackAndWhite` | `binary(...isOptionGreen...)` | 必须有 image；不改变 mask 色值或尺寸。 |
| `WASH_PURPLE` / `washPurpleTextToBlackAndWhite` | `washPurpleHsvComponentFilter` | 必须有 image；OpenCV Mat 全部 invocation-local 并在算法内释放。 |
| `WASH_WHITE` / `washThinWhiteTextToBlackAndWhite` | `washThinWhiteText` | 必须有 image。 |
| `WASH_DIALOG_OPTION_TEMPLATE` | green-or-highlighted-yellow `binary` | 必须有 image。 |
| `WASH_AUTO_COMBAT_ROUND_RED_DIGITS` | `scaledRedDigits` | 必须同时有 image 与 washed foreground `pixelCount`。 |
| `ROUTE_PACKED_LINE_MASK` | 当前 `washYellowText` + `addRoutePackedLineMappings` | 必须有 image 与至少一个 mapping；当前 identity mapping 八元组和列表顺序原样保留。 |
| `ROUTE_DESTINATION_SEGMENTS` | `findTextCandidates` | 无 image；返回按 pixels 降序的 `textCandidates` 和同序中心点；候选为空时 `REQUIRED_FAILURE`。 |
| `COUNT_YELLOW_PIXELS` | `countPixels(...isYellowText...)` | `pixelCount`，`0` 是有效 `CLOUD_EXECUTED` 输出。 |
| `COUNT_GREEN_PIXELS_HSV` | `countPixels(...isGreenHsv...)` | `pixelCount`，`0` 有效。 |
| `COUNT_THIN_WHITE_PIXELS_HSV` | `countMask(thinWhiteTextMask)` | `pixelCount`，`0` 有效。 |
| `FIND_GREEN_TEXT_BANDS` | `findGreenBands` | boxes 保持扫描顺序；空列表降 `REQUIRED_FAILURE`；`GreenTextBand.pixels=0` 保持 transport 可见值。 |
| `PICK_GREEN_TEXT_BAND` | `findGreenBands` 后按 `first` 取首/尾 | 仅返回选中的一个 band；metadata parameters 与方法参数合并时方法参数 `first` 覆盖同名值；空则 required failure。 |
| `BUILD_BINARY_FINGERPRINT` | `binaryFingerprint` | `widthxheight:hex` 非空字符串；位顺序、尾 nibble padding 原样保留。 |
| `BINARY_FINGERPRINT_DISTANCE` | `binaryFingerprintDistance` | 合法输入返回 Hamming distance；null/blank 保持 `NO_RESULT` 且值为 `Integer.MAX_VALUE`、`hasRequiredOutput=false`；格式/尺寸/hex 非法但非空时保持 `CLOUD_EXECUTED + Integer.MAX_VALUE`。 |
| `DETECT_THIN_WHITE_TEXT_LINE_PATTERN` | `addThinWhiteLinePattern` 对应 typed stats | 五字段必须全部存在；阈值与 row/cluster/span 规则不变。 |
| `MEASURE_STDDEV` | `stddev` | 先按 `Locale.ROOT %.4f` 格式化再转 `Double`，避免 in-process 返回额外精度。 |
| `TEXT_CANDIDATES` / `findTextCandidates` | `findTextCandidates` | 无 washed image；候选按 pixels 降序，score 从 N 递减、component/long-row/long-column/reason/density 默认值完全保持；空则 required failure。 |
| `MEASURE_TEAM_TOOLTIP_TEXT` | `addTooltipTextDiagnostics` 对应 typed stats | 六字段 `white/purple/rows/columns/transitions/maxRowPixels` 必须全部存在。 |
| `FINGERPRINT` | 当前 enum 保留项，无 `ImageProcessorService` 方法且无 caller | 不新增 generic dispatcher/public 方法，不猜测语义；保持不可达。若未来要启用，必须另开 contract 变更。 |

### 兼容语义

- **状态/原因**：null raw 或 null operation（仅内部共用入口）返回 `REQUIRED_FAILURE`，reason
  `missing raw image/operation`；正常完整输出为 `CLOUD_EXECUTED`，reason
  `cloud-brain-image-preprocess`；算法显式 `resultStatus=NO_RESULT` 或处理期 `Exception` 保持 `NO_RESULT`，error reason
  `cloud-brain-image-error`。和现有 `DecisionEngine` 一样只 catch `Exception`，不吞 `Error`。
- **空结果**：image/count 的零像素不是空；candidate/band/selected/fingerprint/stats/mapping 缺失按
  `hasRequiredOutput()` 精确降 `REQUIRED_FAILURE`。降级时清空 image/scalar/lists，保留 operation、原 reason 加
  `; missing required ...` 和 synthetic decision，等价于 `CloudImageProcessor.requireCloudOutput(...)`。
- **坐标空间**：算法接收的 `BufferedImage` 左上角是 `(0,0)`；候选 box/point 继续是 supplied-image-local pixels。
  当前 DHXY transport 总是发送 ROI `(0,0,min(raw,window))`，所以现有实际数值也没有非零 ROI offset。in-process
  不凭 metadata/HWND 猜窗口偏移；未来 remote capture/artifact adapter 必须携带 crop origin，并由 caller 在产生
  physical/window-relative action 前显式换算。
- **排序/列表**：`findTextCandidates` 保持 pixels descending 的稳定排序；green bands 保持 y 扫描顺序；所有公开
  lists 使用 `List.copyOf`，null 规范化为空列表；候选 boxes、points 和各并行 stats list 必须同 index 对齐。
- **数值解析**：整数溢出/非法值不得 silently truncate；typed path避免字符串解析，但必须主动复现旧 parser 的
  null/0/default 与 `stddev` 四位小数 round-trip。density 仍按四位小数字符串的数值结果暴露。
- **metadata**：null metadata 规范化为 empty `RequestMetadata`；不从 cloud thread-local 补 task/window/HWND。
  `source/taskCode/phase/windowId/taskRunId/debugImageId/policyVersion` 只进入结构化日志；`rawImagePath` 与 `hwnd`
  只记录 presence，不记录值，不参与算法。parameters Map 防御性复制；仅 `first` 影响现有算法。
- **图像生命周期**：输入从不 flush；washed output 是新对象，成功返回后由 caller flush。required failure 或异常路径
  中已创建但未交付的 output 由 processor 在返回前 flush。HTTP `DecisionEngine` 仍在 PNG 编码后 flush 自己收到的
  output；两条路径不共享可变 image 实例。

### 当前 caller 兼容表

| 当前 caller | 精确调用 | Service 调用兼容性 | 迁移结论/后续 adapter |
|---|---|---|---|
| `DialogService` | 四种 in-memory wash、fingerprint/distance、green/white count、line pattern、stddev | 这些调用可原签名迁移 | caller 自身仍需 remote capture/context；不得改变 dialog phase/fallback。 |
| `DialogService.cloudWashToPath` | `washToPath` | **P1 blocked** | 改为读取 remote capture/artifact 的内存图，调用对应 wash，再由 tenant artifact adapter 可选写 debug output。 |
| `TaskTrackerPanelService` | in-memory green wash/bands/pick、fingerprint/distance | 可原签名迁移 | capture 与 tracker crop origin 另由 remote capture adapter 提供；band 坐标仍 image-local。 |
| `TaskTrackerPanelService.washYellowToPath` | `washToPath` | **P1 blocked** | 同上；stale output cleanup 不能对 Cloud 任意 Path 执行，artifact adapter 应按 scoped artifact id replace/delete。 |
| `CoordinateHelper.findGreenTextInRegion` | `washToPath` 后 `ImageFinder.find(path,...)` | **P1 blocked** | 两段都依赖 DHXY 本地 Path；需后续 in-memory template matcher + scoped template/artifact adapter，不能只替换 wash。 |
| `SummonSkillService` | count yellow + in-memory wash，随后本地保存/模板匹配 | processor 调用可原签名迁移 | 后续需要 template asset 与 debug artifact adapter；本切片不迁该业务。 |
| `TeamRoleDetectionService` | tooltip stats、stddev | 可原签名迁移 | 图像来源仍需 remote capture；阈值决策保持 caller 所有。 |
| `ObjectiveTextRecognitionService` | in-memory green wash | 可原签名迁移 | 后续 OCR/capture authority 另审，本切片不启动 OCR。 |
| `WubeiTask`、`QuestManagerService`、`NpcClickService` | 仅注入/import 或 metadata helper，当前无精确 operation call | 无运行调用需要适配 | 不据此复制本地 runtime；待真实调用出现再按 operation 审查。 |
| 当前无 caller 的 purple/red/route/text-candidate operations | 无 | contract compile-ready | dormant，不新增 host 入口或测试调用。 |

### `washToPath` 权威方案

- 本切片选择 **unsupported/fail-closed until callers migrate**，不是 Cloud-local debug/output convenience。
- 实现不得调用 `Path.toFile()`、`Files.*`、`ImageIO.read/write`、`toAbsolutePath()`，也不得创建目录或删除 stale
  output；返回 operation 对应的 `REQUIRED_FAILURE`、空 image/lists 和固定 reason
  `cloud washToPath unsupported: use in-memory image plus tenant-scoped artifact adapter`。
- 后续 adapter 必须以认证的 `CloudServiceScope` 和 opaque artifact id 为输入，在 `CloudServiceStorage` 根下解析；
  禁止接受 DHXY 绝对路径/相对路径穿越，禁止把 metadata `rawImagePath` 升格为 authority。caller 应先获得/解码
  image，再调用本 processor，最后按明确 debug policy 写 Cloud artifact。该 adapter 不属于本切片。

### Spring、并发、内存、租户与可观测性

- `CloudServiceConfiguration` 的显式 bean 是唯一装配点。`CloudServiceHost` 当前不会被 `CloudBrainServer.start()`
  创建或注册，因此 bean compile-ready 但 dormant；新增 bean constructor 只建无状态对象，无线程、文件、网络或
  native capture side effect。
- processor 与 `ImageAlgorithms` 无共享可变业务状态，可由一个 tenant host 内并发调用。每次调用的 diagnostics、
  candidates、OpenCV Mat/mask 和 output image 都是局部对象；同一个 mutable input image 不允许被 caller 同时修改，
  这是接口所有权前提，不通过全局锁掩盖。
- 容量风险：算法复杂度约为 `O(width*height)`，部分 wash/OpenCV/mask 会同时持有输入、mask、Mat 和 output；当前
  interface 没有经批准的尺寸上限。为保持基线，本切片不自行新增像素阈值/拒绝策略。实现记录 width/height、
  operation、elapsedMs、status；后续若 host 暴露不可信大图，必须另开 capacity CR 在 capture/upload 边界限流，
  不能在算法内悄加业务差异。
- 不做 PNG/Base64 round-trip 是 in-process 的主要内存收益。若父级要求 P2 的 transport DTO payload 逐字段兼容，
  峰值将额外包含 PNG byte[]、Base64 char/string 与 hash buffer，应先明确容量预算。
- tenant 隔离来自每个 `CloudServiceHost` 的独立 bean graph；processor 无缓存/文件/静态 tenant map，不可能跨 tenant
  返回旧图。日志应由 bean factory 注入/绑定 scope 的安全短标识，或至少记录 host scope hash，不记录用户名、token、
  原始 path、base64、fingerprint 内容或 HWND。若当前 scope API 不提供安全标签，记录 `windowId/taskRunId/source`
  并把 tenant tag 缺失列为 observability debt，不能读取本地 runtime 补齐。
- 失败日志：单行结构化字段 `operation/source/taskCode/phase/windowId/taskRunId/debugImageId/imageWxH/status/reason/elapsedMs`；
  normal success 可 debug，`NO_RESULT` 可 info/debug，`REQUIRED_FAILURE` warn。不得记录图像内容或完整 diagnostics 大串。

### 无双重算法权威措施

1. DHXY `CloudImageProcessor`、`ImagePreprocessWashedImageClient`、`ImagePreprocessCloudService` 原地保留，只服务
   thin client transport，不复制到 Cloud。
2. Cloud `ImageAlgorithms.process(...)` 同时服务 `DecisionEngine.imagePreprocess(...)` 与
   `CloudNativeImageProcessor`；任何新增 operation 必须先进入该 switch，两个入口不得各自增加 switch。
3. `DecisionEngine` 只序列化 `process` 的 canonical diagnostics/image；in-process 只读 typed fields，不复制
   `ImagePreprocessCloudService.parseCandidates/parseWashedImage/fields/resultValues` 等 HTTP parser。
4. `CloudNativeImageProcessor` 只做 contract projection、所有权和 required-output gate，不含颜色阈值、候选扫描、
   fingerprint、stddev 或 tooltip 算法。
5. 不开放 `ImageAlgorithms` 为 public，不新增 raw algorithm bean，不提供通用 operation HTTP endpoint或 self-call。

### 批准后验证命令与证据

- 本轮按硬约束未运行 Maven、测试、应用、HTTP、host/poller/UI/capture/OCR/input。
- 实施后只运行 Cloud fresh build（不得 skip）：

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package
```

- 静态可见性/双权威检查：

```powershell
rg -n "class ImageAlgorithms|process\(|addImageDiagnostics\(|ImageAlgorithms\.wash\(|ImagePreprocessWashedImageClient|CloudImageProcessor|ImagePreprocessCloudService" src/main/java
rg -n "CloudNativeImageProcessor|ImageProcessorService imageProcessorService|CloudServiceHost\.create|CloudBrainServer|RemoteTaskRunRoutes|createContext" src/main/java
rg -n "TaskExecutionContextHolder|WindowRuntimeContext|hwnd|InputAction|InputProvider|capture|HttpClient|Files\.|ImageIO\.(read|write)" src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudNativeImageProcessor.java
```

- 二进制可见性与 bean API：

```powershell
javap -classpath target/classes -p com.yueyunfe.dhxy.cloudbrain.ImageAlgorithms
javap -classpath target/classes -p com.yueyunfe.dhxy.cloudbrain.CloudNativeImageProcessor
javap -classpath target/classes -p com.yueyunfe.dhxy.cloudbrain.host.CloudServiceConfiguration
```

- 在实施前后记录并比较，不新增/恢复/运行测试，不触碰 DHXY Java：

```powershell
git status --short --branch
git diff -- src/main/java/com/yueyunfe/dhxy/cloudbrain/ImageAlgorithms.java src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudNativeImageProcessor.java
git status --short -- src/test
git -C D:\mavenProject\DHXY status --short -- src/main/java pom.xml src/main/resources
```

- 验收应明确：fresh package exit 0；现有 suites 全部执行且无 skip；`src/test` unchanged；无 DHXY Java/Maven/resource
  change；无 Task host/poller/UI/capture/OCR/input/HTTP 启动；无 Git mutation。Design #1 到此停止，等待父级在本日志
  写入 `DESIGN APPROVED` 或 `BLOCKED`。

## Local Design Review #1 - BLOCKED - 2026-07-12

### 结论

- `P0=0 / P1=1 / P2=0`，本轮 **BLOCKED**，不得修改 Java/Maven/resources。
- 认可的方向：不复制本地 `CloudImageProcessor`/transport client；Cloud 只保留一个 `ImageAlgorithms` 算法属主；
  新 in-process `ImageProcessorService` bean 继续 dormant；`washToPath` 不得解释 DHXY 本地 `Path`。

### P1-1：为建立直连契约而重写 diagnostics/algorithm result，改动面超过必要边界

- **证据**：Design #1 要把 `ImageAlgorithms.addImageDiagnostics(...)` 改成从一组新 typed nested records
  序列化，并让 `process(...)` 持有 operation status、washed image、candidate/stats 等新模型。这会同时改写当前已运行的
  HTTP `DecisionEngine.imagePreprocess(...)` 生产路径。现有 `addImageDiagnostics(...)`、`wash(...)` 与其各 helper 已经
  是唯一算法实现和兼容输出；本切片只需要复用，不需要把每个 helper 的返回形态全部翻新。
- **影响**：候选顺序、空值/default、并行 stats index、四位小数、额外 diagnostics、`NO_RESULT` 判定或输出 flush
  任一细节漂移，都可能让现有 HTTP 路径与本地基线产生业务差异；无本地测试模式下，该大范围重构缺少相称证据。

### 已拍板的两个设计点

1. **`washToPath` 方案批准为 fail-closed**：Cloud 实现不得读取/写入/规范化/删除任意传入 `Path`，固定返回
   `REQUIRED_FAILURE`。`DialogService.cloudWashToPath`、`TaskTrackerPanelService.washYellowToPath`、
   `CoordinateHelper.findGreenTextInRegion` 在完成内存图像 + tenant-scoped artifact/template adapter 前不得进入可激活
   Service cohort；这不是当前运行时业务变化，因为本切片及 host 均 dormant。
2. **transport provenance 不伪造**：Cloud in-process `ImageProcessorResult.decision()` 固定为 `null`；不得构造
   `cloudResult=null`、payload/hash 为空的 synthetic `ImagePreprocessCloudDecision`。当前源码扫描无 caller 读取该字段，
   required-output 只依赖 typed fields；后续若 caller 真正需要 transport provenance，另开 contract，不在本切片伪造。

### Design Repair #1 精确返修条件

1. 将实现收窄为最小共享分派：`ImageAlgorithms.process(...)` 可返回一个 package-private immutable result，但只能
   包含 `status/reason/washedImage/canonicalDiagnostics`（或同等最小字段）。它必须直接调用并保留现有
   `addImageDiagnostics(...)`、`wash(...)` 与 low-level helper 的现有逻辑；不得把每个 helper 改造成新 typed record，
   不得修改阈值、排序、格式化、默认值或 helper 主体。
2. `DecisionEngine.imagePreprocess(...)` 只把原来的两段调用替换为上述最小 `process(...)`，继续按原路径序列化同一
   canonical diagnostics、PNG/hash/尺寸、confidence/algorithm/reason，并由 engine 继续 flush HTTP-owned raw/output。
3. `CloudNativeImageProcessor` 是 canonical diagnostics 到现有 `ImageProcessorResult` 的唯一 Cloud-side projection；
   允许把本地 `CloudImageProcessor` 的纯解析语义定点移植，但不得复制颜色/候选/fingerprint/stddev 算法。成功和
   required-failure 均令 `decision=null`。
4. 明确 `process(...)` 不 flush/mutate caller-owned raw；Cloud processor 在 failure 丢弃未交付 output 时 flush，
   success 将新 output 所有权交给 caller。HTTP engine 保持自己的既有 raw/output flush。
5. 保持拟改文件最多为 Design #1 的四个 Cloud 文件；`ImageProcessorService`、DHXY Java、pom/resources/tests 不动。
   `CloudServiceConfiguration` 仅显式注册 dormant bean，不扩大扫描或 runtime reachability。
6. 追加完整 `## External Worker - Design Repair #1 - 2026-07-12`，列出 revised pseudocode、逐文件 diff budget、
   逐 operation 兼容表与 `P0/P1/P2`。只有开放问题归零后父级才会写 `DESIGN APPROVED`。

`无已批准业务差异；按基线等价迁移。`
## External Worker - Design Repair #1 - 2026-07-12

### 返修结论

- 已逐条接受 `Local Design Review #1 - BLOCKED` 的六项条件。本版撤回 Design #1 中“把每个 diagnostics helper
  改成 typed record”的方案；`ImageAlgorithms.addImageDiagnostics(...)`、`wash(...)`、所有颜色/候选/
  fingerprint/stddev/tooltip/route helper 的签名与主体全部保留。
- 共享改动只新增一个最小 package-private canonical process result。HTTP 与 in-process 都调用现有两段
  `addImageDiagnostics(...) -> wash(...)`，不改算法、不改 diagnostics 字符串格式、不改排序/default/阈值。
- 父级拍板项原样采用：Cloud `washToPath` 无条件 fail-closed 且绝不触碰传入 `Path`；所有 in-process
  `ImageProcessorResult.decision()` 固定为 `null`，不伪造 transport provenance。
- 本版风险结论：`P0=0 / P1=0 / P2=0`，无剩余设计 blocker，等待父级复审。仍为
  `无已批准业务差异；按基线等价迁移。`

### 收窄后的设计不变量

1. `ImageAlgorithms` 仍是唯一算法属主；新增 `process(...)` 只是现有两段调用的 canonical orchestration，不拥有
   第二份算法、parser 或阈值。
2. `addImageDiagnostics(...)`、`wash(...)` 及其 low-level helper 主体零语义改动；特别是候选顺序、并行 stats
   index、`%.4f`、空字符串、`resultStatus=NO_RESULT`、identity packed mapping、fingerprint invalid sentinel 均不动。
3. `ImageAlgorithms.ProcessResult` 只含 `status/reason/washedImage/canonicalDiagnostics`；不增加 candidate/stats
   typed records，不把 `ImageProcessorService` DTO 下沉到算法层。
4. `DecisionEngine.imagePreprocess(...)` 保留现有 HTTP decision、confidence、algorithm、debugToken、
   coordinateSpace、PNG/Base64/hash/尺寸和 raw/output flush；只把两段算法调用替换成一次 `process(...)`。
5. `CloudNativeImageProcessor` 是 Cloud 内唯一 canonical diagnostics -> `ImageProcessorResult` projection；只定点
   移植 DHXY `CloudImageProcessor` 的纯解析/required-output 语义，不包含任何图像算法。
6. `process(...)` 不 mutate/flush caller-owned raw。成功时 output 所有权交给调用者；失败或 required-output 降级时，
   未交付 output 由直接调用者 flush。HTTP engine 始终 flush 自己编码过的 output 和 decoded raw。
7. 不读取本地 `TaskExecutionContextHolder`、HWND、窗口几何、capture/input/runner/path；metadata 只作诊断和构造
   现有 `param.*` context。
8. bean 只进入显式创建的 dormant `CloudServiceHost`，不扩大 component scan，不连接 server routes、host activation、
   poll/completion 或任何机械能力。

### Revised pseudocode

#### `ImageAlgorithms` 最小 canonical process

```java
// package-private; existing addImageDiagnostics/wash/helper bodies remain unchanged.
static ProcessResult process(
        BufferedImage raw,
        String operation,
        String targetName,
        JsonNode context) {
    Map<String, String> diagnostics = new LinkedHashMap<>();
    diagnostics.put("candidateBoxes", "");
    diagnostics.put("candidatePoints", "");
    BufferedImage washed = null;
    try {
        addImageDiagnostics(raw, operation, diagnostics, context);
        if ("NO_RESULT".equals(diagnostics.get("resultStatus"))) {
            return ProcessResult.noResult(
                    "cloud-brain-image-preprocess-no-result", diagnostics);
        }
        washed = wash(raw, operation, targetName);
        if (washed != null && !diagnostics.containsKey("pixelCount")) {
            diagnostics.put("pixelCount", Integer.toString(countForeground(washed)));
        }
        return ProcessResult.executed(
                "cloud-brain-image-preprocess", washed, diagnostics);
    } catch (Exception e) {
        // Carry an already-created output back so the direct caller can flush it.
        return ProcessResult.failed(
                "cloud-brain-image-error", washed, diagnostics);
    }
}

enum ProcessStatus { EXECUTED, NO_RESULT, FAILED }

record ProcessResult(
        ProcessStatus status,
        String reason,
        BufferedImage washedImage,
        Map<String, String> canonicalDiagnostics) {
    // Compact factories defensively copy diagnostics; all are package-private.
}
```

- `candidateBoxes`/`candidatePoints` 的空 seed 从原 engine 原样移入 canonical map；`debugToken` 与
  `coordinateSpace` 仍是 HTTP transport 字段，由 engine 在 `putAll(canonicalDiagnostics)` 前按原顺序写入。
- `FAILED` 不暴露 exception/type/message，保持当前 `cloud-brain-image-error` fail-closed 可见语义；不 catch `Error`。
- 若 `wash(...)` 自身在返回前抛异常，和现状一样没有已返回 image 可供 caller flush；若异常发生在 image 已赋值后的
  foreground count，则 result 携带该 image，由 caller finally flush。

#### `DecisionEngine.imagePreprocess(...)` 最小替换

```java
BufferedImage raw = ImageAlgorithms.decodeImage(payload);
Map<String, String> diagnostics = new LinkedHashMap<>();
diagnostics.put("debugToken", "cloud-brain-image-preprocess");
diagnostics.put("coordinateSpace", "ROI_RELATIVE");
if (raw == null) {
    return existingUnreadableDecision(diagnostics);
}

ImageAlgorithms.ProcessResult processed = null;
try {
    processed = ImageAlgorithms.process(raw, operation, targetName, context);
    diagnostics.putAll(processed.canonicalDiagnostics());
    if (processed.status() != EXECUTED) {
        return existingNoResultDecision(operation, processed.reason(), diagnostics);
    }
    BufferedImage washed = processed.washedImage();
    if (washed != null) {
        // Existing pngPayload + payload/hash/width/height diagnostics, unchanged.
    }
    return existingExecutedDecision(operation, processed.reason(), diagnostics);
} catch (Exception e) {
    return existingImageErrorDecision(operation, diagnostics);
} finally {
    if (processed != null && processed.washedImage() != null) {
        processed.washedImage().flush();
    }
    raw.flush();
}
```

- 实现时不抽取示意中的 `existing*Decision` helper；保留当前 inline `new Decision(...)`，避免 wrapper nesting。
- `FAILED` 与原 catch 一样返回 `status=NO_RESULT`、confidence `0.8d`、algorithm `image-preprocess`、reason
  `cloud-brain-image-error`；显式 diagnostics `NO_RESULT` 保持原
  `cloud-brain-image-preprocess-no-result`。
- PNG encoding 异常仍由 engine catch；output/raw 在 finally 释放，成功 HTTP response 仍携带原 payload/hash/尺寸。

#### `CloudNativeImageProcessor` projection

```java
public ImageProcessorResult washYellowText(BufferedImage raw, RequestMetadata metadata) {
    return process(raw, WASH_YELLOW, metadata, Map.of());
}

private ImageProcessorResult process(
        BufferedImage raw,
        ImagePreprocessOperation operation,
        RequestMetadata metadata,
        Map<String, String> methodParameters) {
    if (raw == null || operation == null) {
        return requiredFailure(operation, "missing raw image/operation"); // decision=null
    }
    JsonNode context = contextFrom(metadata, methodParameters); // only param.* strings
    ImageAlgorithms.ProcessResult canonical =
            ImageAlgorithms.process(raw, operation.name(), "", context);
    BufferedImage output = canonical.washedImage();
    boolean handedOff = false;
    try {
        if (canonical.status() != EXECUTED) {
            return noResult(operation, canonical.reason()); // decision=null
        }
        ImageProcessorResult projected = project(
                operation, output, canonical.canonicalDiagnostics());
        ImageProcessorResult checked = requireOutput(projected, operationSpecificReason);
        handedOff = checked.image() == output && checked.hasRequiredOutput();
        return checked; // every branch has decision=null
    } catch (Exception e) {
        return noResult(operation, "cloud-brain-image-error");
    } finally {
        if (!handedOff && output != null) {
            output.flush();
        }
    }
}

public ImageProcessorResult washToPath(Path rawPath, Path outputPath, ..., RequestMetadata metadata) {
    // Do not inspect, normalize, read, write or delete either Path.
    return requiredFailure(operation,
            "cloud washToPath unsupported: use in-memory image plus tenant-scoped artifact adapter");
}
```

- `project(...)` 只包含原 `CloudImageProcessor` 的 `parseInteger/Double/Boolean`、parallel-list defaults、box/point
  parsing、`TextLinePatternStats`、`TextCandidateBox`、`PackedLineMapping`、`TeamTooltipTextStats` 和
  `requireCloudOutput` 等价逻辑。它不调用 `ImageIO`、OpenCV 或像素 API。
- `binaryFingerprintDistance` 将方法参数覆盖 metadata 同名 parameter，并使用现有 1x1 transport-equivalent raw；
  blank/null 仍由 canonical diagnostics 产生 `NO_RESULT + Integer.MAX_VALUE`，projection 不另算距离。
- `pickGreenTextBand` 同样由方法参数 `first` 覆盖 metadata；其它 metadata 不影响算法。
- `decision` 在 success、`NO_RESULT`、`REQUIRED_FAILURE`、parse failure 全部为 `null`。

### 逐文件 diff budget

| 文件 | 允许 diff budget | 精确允许内容 | 明确禁止 |
|---|---:|---|---|
| `ImageAlgorithms.java` | `+45..75 / -0..10` 行 | 一个 `process(...)`、一个三值 package-private enum、一个四字段 package-private record/factories；调用现有方法 | 改任何颜色阈值、helper 主体/签名、排序、格式化、mask/OpenCV/fingerprint/stddev 算法 |
| `DecisionEngine.java` | `+12..30 / -18..35` 行，仅 `imagePreprocess(...)` | 用 `ProcessResult` 替换原 add+wash 两段；保留原 Decision/PNG/diagnostics/finally | 触碰其它 service switch、MINIMAP、CR267、route/OCR/helper 或当前其它 dirty 增量 |
| `CloudNativeImageProcessor.java`（新增） | `<=470` 行 | 20 个既有接口方法、一个单层 process、纯 diagnostics projection、required-output gate、日志 | 图像算法、filesystem/HTTP/capture/input/runtime、synthetic decision、额外 public API/wrapper 链 |
| `CloudServiceConfiguration.java` | `+6..12 / -0` 行 | imports + 一个返回接口类型的显式 dormant `@Bean` | 扩大 scan、启动 host、改 storage/memory bean、注册 endpoint/thread |

- 总写集最多上述四个 Cloud 文件。`ImageProcessorService.java`、`ImagePreprocessOperation.java`、DHXY Java、pom、
  resources、tests、其它文档均为零 diff。
- budget 是审查上限，不是必须填满；若 projection 超过 470 行，先复用类内单层 parser，不能新增 parser class 或
  wrapper 层。

### 逐 operation 等价表

| Operation | canonical diagnostics / washed image（现有 helper 原样） | projection 与 required-output |
|---|---|---|
| `WASH_YELLOW` | default text-candidate diagnostics；`washYellowText`；若无 count 则 washed foreground count | image 必需；附带现有 pixelCount/candidates 解析；空 image -> required failure |
| `WASH_GREEN` | default candidates；`binary(isOptionGreen)`；foreground count | image 必需，尺寸/色值不变 |
| `WASH_PURPLE` | default candidates；`washPurpleHsvComponentFilter`；foreground count | image 必需；不改 OpenCV helper |
| `WASH_WHITE` | default candidates；`washThinWhiteText`；foreground count | image 必需 |
| `WASH_DIALOG_OPTION_TEMPLATE` | default candidates；green-or-highlighted-yellow binary；foreground count | image 必需 |
| `WASH_AUTO_COMBAT_ROUND_RED_DIGITS` | default candidates；`scaledRedDigits`；foreground count | image + pixelCount 同时必需 |
| `ROUTE_PACKED_LINE_MASK` | `packedLineMappings` identity 八元组；yellow wash；foreground count | image + 非空合法 mapping 必需；8 项分组、顺序、正尺寸校验不变 |
| `ROUTE_DESTINATION_SEGMENTS` | `addTextCandidateDiagnostics`，washed null | textCandidates 或 candidatePoints 非空；空 -> required failure |
| `COUNT_YELLOW_PIXELS` | `pixelCount=countPixels(isYellowText)`，washed null | Integer 必需；0 有效 |
| `COUNT_GREEN_PIXELS_HSV` | `pixelCount=countPixels(isGreenHsv)` | Integer 必需；0 有效 |
| `COUNT_THIN_WHITE_PIXELS_HSV` | `pixelCount=countMask(thinWhiteTextMask)` | Integer 必需；0 有效 |
| `FIND_GREEN_TEXT_BANDS` | `candidateBoxes=boxes(findGreenBands)` | 保持 y 扫描顺序；`pixels=0`；空 -> required failure |
| `PICK_GREEN_TEXT_BAND` | 现有 `param.first` 首/尾选择，最多一个 box | selected band 必需；空 -> required failure |
| `BUILD_BINARY_FINGERPRINT` | 原 `widthxheight:hex` | 非 blank fingerprint 必需 |
| `BINARY_FINGERPRINT_DISTANCE` | 原 left/right parser 与 `Integer.MAX_VALUE` sentinel | blank -> `NO_RESULT` 且 distance MAX；非 blank 非法 -> executed + MAX；不本地重算 |
| `DETECT_THIN_WHITE_TEXT_LINE_PATTERN` | 原五个字符串字段 | 五字段全部合法才构造 stats，否则 required failure |
| `MEASURE_STDDEV` | 原 `Locale.ROOT %.4f` | 用 `Double.parseDouble` 解析四位小数结果，不暴露额外精度 |
| `TEXT_CANDIDATES` | 原 boxes/points + 7 组并行 diagnostics | pixels 降序及 index 对齐不变；非法项按旧 parser 0/default；空 -> required failure |
| `MEASURE_TEAM_TOOLTIP_TEXT` | 原六字段 metrics | 六字段全部合法才构造 stats，否则 required failure |
| `FINGERPRINT` | enum 存在但接口无方法，当前不可达 | 不新增入口，不猜测/实现；保持不可达 |

### 坐标、空值、解析和所有权等价

- canonical candidate 数值仍是 supplied-image/ROI-local。Cloud processor 没有可信窗口 geometry，不根据
  `rawImagePath/hwnd/windowId` 添加偏移；后续 capture adapter 负责携带 crop origin。projection 对 box/point 做
  非负、正尺寸和 supplied-image bounds 校验，失败时 required failure，不产生 physical coordinate。
- `candidateBoxes/candidatePoints` 空字符串解析为空 list；并行 integer/double list 非法项按旧 projection 降为
  `0/0.0`，缺 index 使用旧 fallback；reason 缺失使用 `cloud-candidate`。
- `findTextCandidates` 继续使用现有稳定 pixels-descending 排序；projection 不二次排序。green band 和 pick 也不排序。
- canonical `NO_RESULT` 不经过 required-output 二次改写；保持 `NO_RESULT`。只有 canonical `EXECUTED` 缺其 operation
  必需输出时才降 `REQUIRED_FAILURE`。
- process 不 flush raw。Cloud success 交付 output；projection/required gate/exception 丢弃 output 时 processor
  flush。HTTP engine 无论成功/失败均 finally flush process output 和 decoded raw；不会出现双 flush 同一已交付对象。

### `washToPath` 与 cohort gate（父级已拍板）

- Cloud 实现固定 fail-closed，不调用 `Path` 的任何实例方法，不调用 `Files`/`ImageIO`，不记录 Path 文本，不做 stale
  cleanup。返回 `REQUIRED_FAILURE`、operation 原值、全空 typed outputs、`decision=null`。
- `DialogService.cloudWashToPath`、`TaskTrackerPanelService.washYellowToPath`、
  `CoordinateHelper.findGreenTextInRegion` 在完成 in-memory capture + tenant-scoped artifact/template adapter 前不得加入
  active Service cohort。该 gate 记录在后续 caller migration 卡，不在本切片新增 runtime gate/flag。
- 后续 adapter 只接受认证 scope 下 opaque artifact/template id；不得把 DHXY Path、metadata.rawImagePath 或 HWND
  解释成 Cloud authority。本切片不设计/实现该 adapter。

### Spring reachability、并发、容量、租户与日志

- `CloudServiceConfiguration` 只显式注册 `ImageProcessorService` bean；`CloudNativeImageProcessor` 不加
  `@Service`。现有 `CloudServiceHost.create(...)` 仍是唯一 context 构造点，且未接到 `CloudBrainServer`，因此 bean
  compile-ready/dormant，无 endpoint、poller、completion、线程或启动副作用。
- processor 无缓存/静态 mutable state；每次 diagnostics/result/output 均 invocation-local。同一 mutable raw 不能由
  caller 并发写；processor 不用全局锁掩盖 caller ownership 错误。
- 保持现有无尺寸上限行为，不在本切片新增容量阈值、TTL 或拒绝规则。记录 operation、image dimensions、elapsed、
  status；不记录 payload、fingerprint、raw path、HWND 或完整 diagnostics。大图限流必须另在认证 capture/upload 边界
  立项。
- 每 tenant host 获得独立无状态 bean；无 filesystem/cache 即无跨 tenant 数据残留。日志使用现有安全 scope 标识（若
  当前 bean factory 无可用安全 hash，则只记录 taskRun/window/source，不能为补 tenant 字段扩大本切片文件集）。

### 六条返修条件对照

1. **最小 result**：满足。仅 status/reason/image/map；现有 add/wash/helper 主体保留。
2. **DecisionEngine 最小替换**：满足。只替换 imagePreprocess 两段调用；HTTP 序列化与 flush 保留。
3. **唯一 projection**：满足。CloudNative 是唯一 Cloud-side parser；`decision=null`，不复制算法。
4. **图像所有权**：满足。process 不动 raw；processor 丢弃时 flush、成功 handoff；engine 保持 raw/output flush。
5. **四文件上限**：满足。接口、DHXY、pom/resources/tests 不动；配置仅 dormant bean。
6. **完整 Repair**：满足。已给 pseudocode、diff budget、逐 operation 表；`P0/P1/P2=0`。

### 批准后验证（本轮不执行）

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q clean package

rg -n "process\(|addImageDiagnostics\(|ImageAlgorithms\.wash\(" src/main/java/com/yueyunfe/dhxy/cloudbrain
rg -n "CloudNativeImageProcessor|ImageProcessorService imageProcessorService" src/main/java
rg -n "TaskExecutionContextHolder|WindowRuntimeContext|HttpClient|Files\.|ImageIO\.(read|write)|InputAction|capture" src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudNativeImageProcessor.java
javap -classpath target/classes -p com.yueyunfe.dhxy.cloudbrain.ImageAlgorithms
javap -classpath target/classes -p com.yueyunfe.dhxy.cloudbrain.CloudNativeImageProcessor
git status --short -- src/test pom.xml src/main/resources
git -C D:\mavenProject\DHXY status --short -- src/main/java pom.xml src/main/resources
```

- fresh Cloud package 必须 exit 0 且不得使用 skip flag；现有 suites 正常执行。静态证据须确认
  `ImageAlgorithms`/nested result 仍 package-private、processor 无额外 public/raw API、`washToPath` 无 filesystem
  引用、host/server routes 未改、`src/test`/pom/resources/DHXY Java 零 diff。
- 本轮未运行 Maven、测试、应用、HTTP、host/poller/UI/capture/OCR/input，未执行 Git mutation。Design Repair #1
  到此停止，等待父级写 `DESIGN APPROVED` 或新的 `BLOCKED`。

## Local Design Review #2 - DESIGN APPROVED - 2026-07-12

- 结论：`P0=0 / P1=0 / P2=0`，`Design Repair #1` **DESIGN APPROVED**。
- 父级独立复核确认返修已把改动收窄到必要边界：现有 `addImageDiagnostics(...)`、`wash(...)`、全部
  low-level helper/阈值/排序/格式化/default 保持原样；新增 `ProcessResult` 只承载
  `status/reason/washedImage/canonicalDiagnostics`，HTTP 与 in-process 共用同一分派但不复制算法。
- 已批准的路径与 provenance 决策继续生效：Cloud `washToPath` 绝不触碰传入 `Path` 并固定
  `REQUIRED_FAILURE`；依赖该方法的三个 caller 在内存图像 + tenant artifact/template adapter 完成前不得进入 active
  cohort；所有 in-process `ImageProcessorResult.decision()` 固定为 `null`。
- 允许同一 worker 进入 `External Worker - Implementation #1`，写集严格限定为：新增
  `CloudNativeImageProcessor.java`，定点修改 `ImageAlgorithms.java`、`DecisionEngine.java` 的
  `imagePreprocess(...)`、`host/CloudServiceConfiguration.java`。不得触碰其它 Cloud/DHXY Java、pom/resources/tests/
  routes/host activation。
- 实现后必须 fresh Cloud `mvn -q clean package`（无 skip flag），并追加精确 diff、可见性/reachability、
  `washToPath` 无 filesystem、`src/test` unchanged、Surefire、JAR 与无运行/Git mutation 证据；自审不算父级批准。

`无已批准业务差异；按基线等价迁移。`

## External Worker - Implementation #1 - 2026-07-12

### 实施结论

- 已严格按 `Local Design Review #2 - DESIGN APPROVED` 实施 Cloud-native in-process
  `ImageProcessorService`。写集仅为批准的四个 Cloud 文件和本协作日志；未修改接口、DHXY Java、pom、resources、
  tests、routes 或 host activation。
- `ImageAlgorithms.addImageDiagnostics(...)`、`wash(...)` 及全部 low-level helper/阈值/排序/格式化/default 主体
  未修改。新增 `process(...)` 只按现有顺序调用 `addImageDiagnostics -> wash -> countForeground`，并返回最小
  `status/reason/washedImage/canonicalDiagnostics`。
- `washToPath` 固定 `REQUIRED_FAILURE`，未读取、规范化、记录、写入或删除任一传入 `Path`；所有 in-process
  `ImageProcessorResult` 构造路径的 `decision` 均为 `null`。
- worker 自审：`P0=0 / P1=0 / P2=0`，当前无实现 blocker。此处自审不构成父级 `Approved`，等待父级独立 review。
  `无已批准业务差异；按基线等价迁移。`

### 精确文件与 diff

| 文件 | 本轮精确变化 | 最终证据 |
|---|---|---|
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudNativeImageProcessor.java` | 新建；实现既有 20 个接口方法、canonical diagnostics projection、required-output gate、所有权释放与 dormant 日志 | 470 lines，22,820 bytes，SHA-256 `069F9B4F6CD8DECED737BE304595ADDF09EBDBE9D7FF1CFD9EA455C7E4E5C85C` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/ImageAlgorithms.java` | `+63/-0`；仅新增 2 imports、`process(...)`、三值 `ProcessStatus`、四字段 `ProcessResult` | 最终 SHA-256 `B946C6B2A4A475AA9B47A8D59927CCF128C1C7027524E0AFA5EDE5D94950A795`；diff 显示 helper 主体零改动 |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/DecisionEngine.java` | 仅 `imagePreprocess(...)` 本轮增量 `+11/-10`：以 `ImageAlgorithms.process(...)` 替换原 add/wash 两段，并在 finally flush process output | 全文件相对 HEAD 为 `+142/-10`，其中开工前已有无关 `+131/-0` dirty；未覆盖 NAVIGATION/MINIMAP/CR267 等并行内容；最终 SHA-256 `857291B7C0E40289505846B2747A9DEE89EB45F3992642BB9E25AD76D2FD04FC` |
| `src/main/java/com/yueyunfe/dhxy/cloudbrain/host/CloudServiceConfiguration.java` | 在开工前 untracked 现有内容上定点 `+7`：2 imports + 一个显式 `ImageProcessorService` bean | 42 lines，最终 SHA-256 `EADEF3CB0B84A1535B28BFA01EC2A5EA913D6FD873978D3E71B7455249B857D5`；component scan/storage bean 未改 |

- `ImageProcessorService.java`、`ImagePreprocessOperation.java`、DHXY `CloudImageProcessor`、
  `ImagePreprocessWashedImageClient`、`ImagePreprocessCloudService` 均未修改/复制。
- 实施前 scoped hash 与 Design #1 记录一致；所有编辑均基于最新 dirty 内容用定点 patch 完成，无 reset/checkout/
  clean/revert/stage/commit/push。

### Operation 映射

| Operation | canonical owner / projection | 必需输出与状态 |
|---|---|---|
| `WASH_YELLOW` | 现有 default candidate diagnostics + `washYellowText` | image 必需；foreground pixelCount 保留 |
| `WASH_GREEN` | 现有 `binary(isOptionGreen)` | image 必需 |
| `WASH_PURPLE` | 现有 `washPurpleHsvComponentFilter` | image 必需；OpenCV helper 未改 |
| `WASH_WHITE` | 现有 `washThinWhiteText` | image 必需 |
| `WASH_DIALOG_OPTION_TEMPLATE` | 现有 green/highlight-yellow binary | image 必需 |
| `WASH_AUTO_COMBAT_ROUND_RED_DIGITS` | 现有 `scaledRedDigits` | image + pixelCount 必需 |
| `ROUTE_PACKED_LINE_MASK` | 现有 yellow wash + identity `packedLineMappings` | image + 合法非空 mapping 必需 |
| `ROUTE_DESTINATION_SEGMENTS` | 现有 `addTextCandidateDiagnostics` | candidate box/point 非空；无 washed image |
| `COUNT_YELLOW_PIXELS` | 现有 yellow predicate count | Integer 必需，0 有效 |
| `COUNT_GREEN_PIXELS_HSV` | 现有 green HSV count | Integer 必需，0 有效 |
| `COUNT_THIN_WHITE_PIXELS_HSV` | 现有 thin-white mask count | Integer 必需，0 有效 |
| `FIND_GREEN_TEXT_BANDS` | 现有 `findGreenBands` boxes | 保持扫描顺序，`pixels=0`；空则 required failure |
| `PICK_GREEN_TEXT_BAND` | 现有 `param.first` 首/尾选择 | selected band 必需；方法参数覆盖 metadata 同名值 |
| `BUILD_BINARY_FINGERPRINT` | 现有 `binaryFingerprint` | 非 blank fingerprint 必需 |
| `BINARY_FINGERPRINT_DISTANCE` | 现有 diagnostics helper + 1x1 transport-equivalent raw | blank/null 为 `NO_RESULT + MAX_VALUE`；非空非法保持 executed + MAX_VALUE |
| `DETECT_THIN_WHITE_TEXT_LINE_PATTERN` | 现有五项 diagnostics | 五字段完整才构造 stats |
| `MEASURE_STDDEV` | 现有 `Locale.ROOT %.4f` diagnostics | projection 解析四位小数 Double |
| `TEXT_CANDIDATES` | 现有 boxes/points + 七组并行 stats | pixels-descending 顺序与 index/default 不变；空则 required failure |
| `MEASURE_TEAM_TOOLTIP_TEXT` | 现有六项 tooltip diagnostics | 六字段完整才构造 stats |
| `FINGERPRINT` | enum 保留但接口无方法 | 仍不可达，未新增 generic/public 入口 |

### 等价、解析与所有权

- canonical map 保留 `candidateBoxes/candidatePoints` 空 seed 和所有原字符串 diagnostics；Cloud projection 是唯一
  Cloud-side parser，按旧语义处理 integer/double/boolean、parallel-list `0/0.0/default`、packed 8 元组及候选顺序。
- candidate box/point 只按 supplied-image bounds 校验并保持 image-local；不读取 HWND/window geometry，不猜测
  crop origin，不产生 physical coordinate。
- canonical `NO_RESULT/FAILED` 返回 `NO_RESULT` 且不交付 image；canonical `EXECUTED` 才可交付 output。只有
  `EXECUTED` 缺 operation 必需输出时降 `REQUIRED_FAILURE`。
- `process(...)` 不 mutate/flush caller raw。Cloud processor success 将新 output 所有权交 caller；parse/
  required-output/failed 分支由 processor flush 未交付 output。`binaryFingerprintDistance` 自己创建并 finally flush
  1x1 raw。HTTP engine 继续 finally flush decoded raw 与 process output，PNG/hash/尺寸/confidence/algorithm/reason
  保持原路径。
- 实施中静态自审发现并修正过一个未交付 image 引用：non-executed projection 现在固定传 `null` image，再释放
  canonical output；最终 fresh package 在该修正后重新执行成功。

### Fresh package / Surefire / JAR

- 最终命令：`mvn -q clean package`，未带任何 skip flag；exit `0`，wall time `74.7s`。这是所有 Java 修正后的
  第二次 fresh clean package，作为最终交付证据。
- Surefire：`4 suites / 21 tests / 0 failures / 0 errors / 0 skipped`：
  `CloudBrainTemplateResourceGuardTest`、`IncenseStatusCloudBrainContractTest`、
  `WuhuanRouteCandidateContractTest`、`XiuluoCloudBrainContractTest`。
- 最终 shaded JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119,479,557 bytes，SHA-256
  `9525CB52B4D2596BAC1D3BE987F395545F12F64293BA61D29D92300E07B91553`。
- 未新增/恢复测试；只执行仓库既有 package 生命周期中的现有 suites。

### `javap` / public API

- `javap -p ImageAlgorithms$ProcessResult/ProcessStatus`：四字段 result 和三值 enum 存在；两者被 package-private
  `final class ImageAlgorithms` 封闭，constructors/factories 无跨 package 可达性。
- `javap -public CloudNativeImageProcessor`：public 面只有无参 constructor 与既有 `ImageProcessorService` 的
  20 个方法；无 raw request、generic operation、HTTP、poll、completion、algorithm getter 或额外 public helper。
- `javap -public CloudServiceConfiguration`：新增 public bean factory 仅
  `ImageProcessorService imageProcessorService()`；原 dialog memory bean 保留。
- `decision` 构造扫描无 builder/new `ImagePreprocessCloudDecision`/`CloudDecisionResult`；三个
  `new ImageProcessorResult(...)` 构造点末字段均为 `null`。扫描命中的 `CandidateBox/CandidatePoint` 仅为 typed
  projection，不是 decision provenance。

### Raw / self-HTTP / filesystem / reachability 扫描

- 对 `CloudNativeImageProcessor.java` 扫描 `TaskExecutionContextHolder|WindowRuntimeContext|HttpClient|
  CloudImageProcessor|ImagePreprocessWashedImageClient|ImagePreprocessCloudService|InputAction|InputProvider|capture|
  poll|completion`：`0 matches`。
- filesystem call 扫描 `Files.|ImageIO.|toFile|toAbsolutePath|normalize|resolve|delete|rawPath.|outputPath.`：
  `0 matches`。`Path` 仅出现于接口 import、`washToPath` 参数和固定 fail-closed reason；方法未解引用参数。
- `DecisionEngine` 的 `IMAGE_PREPROCESS` 位置只调用 `ImageAlgorithms.process(...)`；仓内其它既有业务位置对
  `ImageAlgorithms.wash(...)` 的调用不属于本切片，未修改。
- server reachability 扫描：`CloudBrainServer.java`、`CloudBrainApplication.java`、`remote/`、`api/` 对
  `CloudServiceHost|CloudServiceConfiguration|CloudNativeImageProcessor` 为 `0 matches`。新 processor 仅在
  `host/CloudServiceConfiguration` 显式 bean 中出现，host 继续 dormant；未启动 server/host/poller/UI/capture/
  OCR/input。

### Protected scope / unchanged 证据

- Cloud `git status --short -- src/test`：空，tests unchanged。
- Cloud protected status 与开工快照一致：`pom.xml` 仍是既有 `M`，
  `src/main/resources/images/template/battle/auto_remaining.png` 仍是既有 untracked；本轮未写 pom/resources。
- DHXY `git status --short -- src/main/java pom.xml src/main/resources` 与开工快照逐项一致：原有 Java dirty/
  untracked 项保持，pom/resources 无新增状态；本轮未写 DHXY Java/Maven/resources，也未运行 DHXY Maven、测试或应用。
- 未运行 HTTP/server/host/poller/UI/capture/OCR/input；未执行任何 Git mutation。

### Blocker / handoff

- 当前实现 blocker：无。worker 自审为 `P0=0 / P1=0 / P2=0`。
- 仍需父级对最新源码、diff、构建与扫描证据做独立 Implementation Review；本段不把 worker 自审视为
  `Approved`。Implementation #1 到此停止。

## Local Implementation Review #1 - APPROVED - 2026-07-12

- 结论：`P0=0 / P1=0 / P2=0`，最新实现 **APPROVED**，本协作切片 CLOSED。
- 父级独立逐文件复核确认：`ImageAlgorithms` 只新增最小 `process`/package-private result，现有
  `addImageDiagnostics`、`wash` 与全部 low-level helper 主体未改；`DecisionEngine.imagePreprocess(...)` 只替换
  canonical 分派并保留 HTTP diagnostics/PNG/hash/status/reason/raw+output flush；`CloudNativeImageProcessor` 只做
  typed projection/required gate，无算法、self-HTTP、本地 runtime/HWND/input/capture/path authority；配置只增加
  dormant `ImageProcessorService` bean。
- `washToPath` 源码确认不解引用任一 `Path`，固定 `REQUIRED_FAILURE`；所有 success/failure 构造的
  `ImageProcessorResult.decision()` 均为 `null`。三个 Path caller 的 cohort gate 继续有效，未适配前不得激活。
- 父级独立执行 Cloud `mvn -q clean package`，exit `0`，wall time `73.3s`；Surefire 为 4 suites / 21 tests /
  0 failures / 0 errors / 0 skipped。父级产出的 shaded JAR 为 119,479,557 bytes，SHA-256
  `96C2AB2433B747F6F0C002556817FFFC96DE193E08EEF799063953C900C50F92`（shade 产物非 reproducible，故与 worker
  构建 hash 不同但长度一致；本结论以父级当前 fresh artifact 为准）。
- `javap` 确认 processor public 面仅无参构造 + 既有 20 个接口方法；`ProcessResult/ProcessStatus` 封闭在
  package-private `ImageAlgorithms`；配置只新增接口 bean factory。静态扫描确认 processor 无 transport client、
  HTTP、poll/completion、window/input/capture、filesystem 调用，server/api/remote 对新 host bean 零可达。
- `src/test` 无变化；Cloud 既有 `pom.xml`/resource dirty 状态未被本切片改写；DHXY Java/Maven/resources 未触碰；
  未启动 server/host/poller/UI/capture/OCR/input，未执行 Git mutation。

`无已批准业务差异；按基线等价迁移。`
