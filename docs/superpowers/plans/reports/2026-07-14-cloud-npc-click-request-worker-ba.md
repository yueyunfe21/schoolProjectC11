# Cloud NpcClick Smart Request - Internal Worker BA

## Parent Direct Implementation Task - `W-NPCCLICK-REQUEST-DTO-IMP1` - 2026-07-14T02:27:00-04:00

Internal BA 直接实现，不写 Design。业务权威为 DHXY committed `0114604e`，不得读取/复制当前 dirty 的 DHXY
工作树版本作为基线。

### 唯一写集

- New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\task\NpcClickSmartCloudRequest.java`
- 本报告仅 append-only

目标写前不存在；Cloud 已存在 `com.bot.dhxy.model.npc.NpcClickRequest` 与 Lombok。其他 Worker 正并行修改
不同文件；不得回滚、覆盖、清理或提交任何 dirty/untracked。

### 精确实现

从 committed `0114604e:src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudRequest.java` 完整复制该文件：

- 外层 `@Value @Builder(toBuilder = true)` closed request 的全部字段及顺序；
- `scanRegions/templateSpecs/targetTemplateSpecs/yellowTemplateSpecs/glyphMetadata` 的原 `@Builder.Default`；
- public nested `Roi` 与 `ScanRegion` 的字段、顺序与 Lombok 注解。

只允许补充不改变执行 token 的类 JavaDoc，说明这是后续同名 Cloud `NpcClickService` 的 committed request DTO；
不得增加校验、factory、getter、caller、mapper、codec/schema、state/session/ledger/TTL/retry、capture/input/remote
调用或其它类型。不得修改 DHXY 源文件或任何既有 Cloud 文件。

运行 Cloud `mvn -q compile`（不 clean），在真实 EOF追加 `CLAIMED` 与 `Implementation #1`，列 source/target
bytes+SHA-256、diff=0 与构建结果。自审仅 QA，不构成 Approved；父级独立源码审查。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED - `W-NPCCLICK-REQUEST-DTO-IMP1`

- Worker: Internal Worker BA
- Claimed at: `2026-07-14T02:29:04-04:00`
- Authority: committed `0114604e:src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudRequest.java`
- Scope guard: target was absent before write; current dirty DHXY source was not read; all unrelated Cloud dirty/untracked files remain untouched.

## Implementation #1 - `W-NPCCLICK-REQUEST-DTO-IMP1`

- Completed at: `2026-07-14T02:30:24-04:00`
- Source: committed `0114604e:src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudRequest.java`; bytes=`2367`; SHA-256=`6a560d5fa4c85bf27fb23033d2911556bac9dec018fb129cf66a30768e69e2bb`.
- Target: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\cloud\task\NpcClickSmartCloudRequest.java`; bytes=`2367`; SHA-256=`6a560d5fa4c85bf27fb23033d2911556bac9dec018fb129cf66a30768e69e2bb`.
- Mechanical byte comparison: `diff=0`.
- Build: Cloud `mvn -q compile` (no `clean`) completed with exit code `0` in `15.9s`.
- Scope result: only the new target DTO was created; no test, application/host, Task/UI, capture, input, or Git mutation was performed.
- Worker self-review: QA confirms the target is byte-identical to the committed authority and contains no scope expansion. This QA is not `Approved`; parent independent source review remains required.

## Parent Source Review #1 - APPROVED / `W-NPCCLICK-REQUEST-DTO-IMP1` - 2026-07-14T02:32:00-04:00

父级对 committed `0114604e:src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartCloudRequest.java`
与 Cloud 新文件做完整 blob/bytes 复核，结论 `P0=0 / P1=0 / P2=0`：

- baseline 与 target Git blob 均为 `5dab95b859ab4cee4eed1bc960e439762cc5eda5`，长度均为 `2367`
  bytes；目标 SHA-256 为 `6a560d5fa4c85bf27fb23033d2911556bac9dec018fb129cf66a30768e69e2bb`，
  与 BA 报告一致，证明整个 DTO byte-identical。
- 外层 Lombok 注解、字段顺序、五个 `@Builder.Default` 集合，以及 public nested `Roi/ScanRegion` 均原样保留；
  没有新增校验/factory/caller/mapper/codec/schema、state/session/ledger/TTL/retry、capture/input/remote 调用。
- BA 的 Cloud `mvn -q compile`（不 clean）exit 0；未改 DHXY 或任何既有 Cloud 文件。

本 NpcClick smart request DTO cohort `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
