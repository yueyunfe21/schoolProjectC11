# Cloud TeamReturn Pure Diagnostic Worker BL

## CLAIMED

- task: `W-TEAMRETURN-PURE-DIAGNOSTIC-CPU-IMP1`
- claimedAt: `2026-07-14T04:00:59.9852935-04:00`
- baseline: committed DHXY `0114604e`, `src/main/java/com/bot/dhxy/service/TeamReturnService.java`
- unique Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`
- unique report write set: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-team-return-pure-diagnostic-worker-bl.md`
- implementation scope: exact-copy private methods `bestRectText(Point,int,int)`, `logPrefix(TaskExecutionContext)`, and `windowKey(TaskExecutionContext)`; add only the directly required `java.awt.Point` import and outer-class JavaDoc note.
- frozen scope: all previously approved methods/types, `parseNativeTitle`, runtime/pathing state, capture/template/I/O, remote/input/caller, public API, wrappers, Spring/window/queue/owner/session/ledger/TTL/retry/runtime behavior.
- safety: no Git mutation, tests, `mvn clean`, application/server/host/Task/UI/capture/input execution, rollback, overwrite, or cleanup.

## Implementation #1

- completedAt: `2026-07-14T04:03:12.0676742-04:00`
- Java write set: only `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\TeamReturnService.java`
- report write set: only this append-only report.
- changed Java surface:
  - added directly required `java.awt.Point` import;
  - extended only the outer class JavaDoc to state that the three copied helpers are dormant and have no caller;
  - exact-copied private `bestRectText(Point,int,int)`, `logPrefix(TaskExecutionContext)`, and `windowKey(TaskExecutionContext)` from committed `0114604e`.
- target file SHA-256 before: `c00066cd24c3b3d32a628c377b8b3df9aa22eef526e9699d64ca8f4ede740a6b`
- target file SHA-256 after: `f23f05d653ad494ee3dee426e7ffb0af5245f5b59da8ef2c04e05e640c1a2fc8`

### Exact baseline evidence

| Method | Source chars | Target chars | Exact diff | Block SHA-256 |
|---|---:|---:|---|---|
| `bestRectText(Point,int,int)` | 216 | 216 | `0` / `exact=true` | `fe9187f1a04e90045fe0c76e4d0af02dd7ef3d195496be442ac0a9a33dd6b5f9` |
| `logPrefix(TaskExecutionContext)` | 140 | 140 | `0` / `exact=true` | `4d14a9c9f3e387a0dfbcddfbbaeacfe571213f7ed42c0e1ea05be1faf81f5970` |
| `windowKey(TaskExecutionContext)` | 240 | 240 | `0` / `exact=true` | `ef846ac8e2ee24c1fa039dd0f5ea265621966113b2e2ed51eb5c60286384e411` |

### Existing approved blocks unchanged

The following method/type hashes were captured before the edit and recomputed after the edit; every value is unchanged:

- `probeMemberReturnMarker`: `2f6e6d82606cab2efa732fd93476774fd18851aaccb8e192e56e8dad924ac7fc`
- `isReturnTeamSignalPresent`: `8e7eb7449a399ca317ada72c31ed92a18634c4ee6e4c57173eaabf94669f303b`
- `waitForMembersReturnIfNeeded`: `4e1f7b4541c69b6c880cd0c27005f98ec128b584199c2b560ec7c619f73cfc74`
- `leaderWaitTimeoutMs`: `00213a18f7f5c6114d40d23d5d76b0d50feb746d22042796084d59f034fafe2e`
- `leaderWaitPollMs`: `3d6c53571cbceb9937b0cd6166febb7cb26f87764e3c993017c9e18dfe6a10d8`
- `safeSource`: `d1f9d5bf5512c2c49658bfb4f7ff97e8055a788248213df5acc99a81f4889c38`
- `LeaderSignalPrecheckStatus`: `10017c94d81fb8431b735c4bb4fabfed8f77056eb3570867e8db469940e5c63b`

### Compile and forbidden-scope checks

- Cloud command: `mvn -q compile`
- result: exit `0` in 16 seconds; no `clean` and no test command.
- each new helper name occurs exactly once in the target class, proving it is definition-only/dormant with no caller.
- the three-method block contains none of `parseNativeTitle`, runtime/pathing state, capture/template/I/O, remote/input, owner/session/ledger/TTL/retry symbols.
- no public API, wrapper, Spring/window/queue integration, behavior, Maven model, DHXY Java, tests, runtime, or Git state was changed.

Worker self-check only; this is not reviewer approval. Waiting for parent source review.

## Parent Source Review #1 - APPROVED - 2026-07-14T04:07:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取三个完整 private 方法，按 LF 归一化逐字符复核，
结论 `P0=0 / P1=0 / P2=0`：`bestRectText`、`logPrefix`、`windowKey` 均 `exact=True`，
长度分别为 `216/216`、`140/140`、`240/240`。矩形文本、unknown prefix 与 window-id/default fallback
无漂移。父级复算文件 SHA-256 为
`f23f05d653ad494ee3dee426e7ffb0af5245f5b59da8ef2c04e05e640c1a2fc8`，与 BL 交付一致；
BL 的 Cloud `mvn -q compile` exit 0。没有 parseNativeTitle、runtime/pathing、capture/template I/O、remote/input/caller。

本 TeamReturn diagnostic cohort `SOURCE APPROVED`。Worker BL 可以关闭。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**
