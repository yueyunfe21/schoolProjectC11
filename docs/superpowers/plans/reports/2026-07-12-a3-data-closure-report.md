# A-3 Data Closure Delivery Report

## Status

`DONE`

交付按 2026-07-12 用户最新方向收窄为同步 `RemoteGameClientPort` 首版 lift-and-shift 的最小云端状态边界。完整 PostgreSQL/灾备形式化 DDL 不再是开工门；首版采用 in-memory state + append-only runtime journal，并保留未来持久化接口。

## Changed Files

1. `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md`
2. `docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md`
3. `docs/superpowers/plans/reports/2026-07-12-a3-data-closure-report.md`

未修改 Java、总稿、migration matrix、protocol/security 工件、CR、dashboard 或 Exclusive Write Scope 外任何文件。

## Revised Requirement Mapping 1-13

| # | 收口结果 |
|---|---|
| 1 | 保留 authenticated tenant/device 隔离与云端业务权威；明确客户端本地状态不形成 fallback。 |
| 2 | 两份规范已自包含；首版编码不依赖旧版本工件。 |
| 3 | 用 `AuthenticatedScope`、`WindowKey`、`TaskRunKey`、`ActionKey`、`RequestKey` 明确同步运行所需完整身份；不再要求先落完整 SQL composite FK。 |
| 4 | 多 writer authority current/event/transfer 延后；首版单云端 writer + per-window serialization 足够支撑直迁。 |
| 5 | byte-exact durable outbox 延后；首版同步调用使用 PREPARED → DISPATCHED → outcome/UNKNOWN，并要求 DISPATCHED journal 在调用前 flush。 |
| 6 | upload grant/frame metadata DDL 延后；重连只要求 fresh observation reference 和云端 resync gate。 |
| 7 | memory publication/verdict schema 不属于本轮 RemoteGameClientPort 最小边界，已移出首版开工门。 |
| 8 | 保留 UNKNOWN 历史、首个可信迟到 outcome、同 digest 幂等和不同 digest 冲突。 |
| 9 | protocol fact 形式化矩阵改为首版 runtime journal 事件全集；事件包含 scope/window/run/turn/action/request identity 与 digest 链。 |
| 10 | 明确 task 终态、command 值、current turn、stop/pause、断线 UNKNOWN、重连 gate；对象引用/GC 延后。 |
| 11 | policy/quota/SLO/verifier/evidence 完整表延后，不阻塞首版迁移；任务业务 payload 保持 versioned opaque。 |
| 12 | journal namespace 固定使用 authenticated tenant/device/window；恢复不从 payload、本地 cache 或日志猜测业务结果。 |
| 13 | 物理 SQL 类型、fillfactor、RPO/RTO 实测值继续属于后续 infrastructure tuning；首版接口和状态语义已冻结。 |

## Semantic Choices

- 在线权威状态按 `WindowKey` 串行化，不同窗口并发，同一窗口最多一个在途物理 action。
- `TaskTurnState.turnRevision` 是 task turn 的 CAS 版本；action 签发、outcome、command、UNKNOWN、重连裁决都增加 revision。
- 同步调用固定走 prepare → journal+flush dispatched → `RemoteGameClientPort` → outcome 或 UNKNOWN。
- request 以 `(tenantId,deviceId,requestId)` 幂等；同 digest 重放原 response，不同 digest 冲突。
- pause/stop 只控制 dispatch 与 checkpoint，不改变现有任务 phase、OCR、点击、导航、fallback 或成功条件。
- 断线增加 connection generation；旧 generation 结果不能直接推进 current turn。
- UNKNOWN action 不自动重放；首个可信迟到 outcome 形成追加 resolution，是否继续由云端原任务逻辑结合 fresh observation 决定。
- journal 可用于诊断和可选重建；缺失或损坏时 fail closed，逐窗口重新注册/重连/resync。
- `RuntimeStateStore`、`RuntimeJournal`、`RuntimeRecovery`、`ReconnectCoordinator` 是未来持久化 adapter 的稳定边界。

## Verification

Command:

```powershell
rg -n "same v|同 v|TBD|TODO|复合 FK 到|例如|实现者|按需|以后决定" docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md
```

Result: native `rg` exit `1`, which means no matches; the command wrapper normalized that expected no-match result to exit `0`. No unresolved placeholder/example-as-contract text. Historical matches: none.

Command:

```powershell
git diff --check -- docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md docs/superpowers/plans/reports/2026-07-12-a3-data-closure-report.md
```

Final result after report creation: exit `0`.

No Java changed, so the Java compile gate does not apply. No local automated test was created or run.

## Current SHA-256

| File | SHA-256 |
|---|---|
| `docs/superpowers/specs/2026-07-12-thin-client-state-data-model.md` | `B2F35B91A273F92694F7EA7B24CED80584757557D1466E01F4709789498E7F1B` |
| `docs/superpowers/specs/2026-07-12-thin-client-dr-backup.md` | `204B8443DF481B1A5C25480F3E24C466B0EBF5798DB3E0CA4C2976B52B4510F5` |

报告文件不内嵌自身 SHA-256，避免自引用导致哈希在写入后失效。

## Remaining Concerns

- append-only journal 的具体介质、flush API 和签名/会话校验实现仍需在编码阶段选型，但不改变本文状态接口，也不阻塞首版迁移。
- 每个迁移任务需要把现有 state/phase 放入 versioned opaque payload，并复用已有 resync/观察逻辑；状态层不得自行发明新业务判定。
- 完整 PostgreSQL durable store、跨区 RPO/RTO、outbox 与 evidence/object 生命周期是后续增强，不是本轮遗漏或开工阻塞项。
