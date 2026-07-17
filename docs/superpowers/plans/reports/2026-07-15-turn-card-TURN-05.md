# TURN-05 Cloud Routes Single Integration

## CLAIMED

- Claimed at: `2026-07-15T15:39:00-04:00`.
- Worker role: Internal implementation Worker only; not manager/reviewer, cannot self-approve, and cannot claim another card.
- Card type/count: `INTEGRATION`, `countDelta=0`.
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`; no approved business difference.
- Workspace baseline: Cloud `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`; all pre-existing dirty/untracked files are preserved.
- Dependencies read: TURN-03B and TURN-04 are parent `SOURCE APPROVED / BUILD COHORT PENDING`.

## Exact Write Set

Cloud repository `D:\mavenProject\dhxy-cloud-brain`:

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnRoutes.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/CloudBrainServer.java`

DHXY repository:

- `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-05.md`

No protocol, DTO, handler, configuration, legacy route, test, or other source file is owned by this Worker.

## SOURCE DELIVERED / BUILD COHORT PENDING

- Delivered at: `2026-07-15T15:45:13-04:00`.
- Worker conclusion only: source delivered for parent review. This is not `APPROVED`, not `CLOSED`, and does not change the `189/407` ledger.
- `CloudTurnRoutes` assembles one existing `CloudTurnExchange`, one canonical `CloudTemplateCatalog` over the existing `PackagedTemplateAssets`, the existing TURN-04 `CloudTurnHttpHandler`, and the existing TURN-03B `CloudTemplateHttpHandler` into one opaque bundle.
- The bundle exposes only the two `HttpHandler` capabilities publicly. Its typed `CloudTurnCommandPort` remains package-private, keeping Cloud command publication separate from HTTP request parsing.
- `CloudBrainServer.start(...)` creates one route bundle and registers `CloudTurnHttpHandler.PATH` and `CloudTemplateHttpHandler.PATH_PREFIX` exactly once each before the existing root `/` gateway context.
- The existing legacy gateway routes, remote poll/outcome/task-run routes, Xiuluo route, OCR health route, executor sizing, startup sequence, base URL, and close behavior remain intact.
- The listener remains bound to `127.0.0.1` over HTTP. An inline boundary comment records that external HTTPS is supplied by TLS termination before forwarding to this loopback backend; no TLS/configuration surface was added.
- No handler logic was copied. No extra route, owner/permit/session/ledger/compaction/durable workflow/business TTL, automatic retry, WebSocket, or Base64 image path was added.

### Exact Source Evidence

- `CloudTurnRoutes.java:29-39`: one bundle factory reuses the existing exchange, packaged template assets, catalog, and both handlers.
- `CloudTurnRoutes.java:43-68`: opaque bundle has two public handler accessors and one package-private typed command accessor.
- `CloudBrainServer.java:59-61`: startup behavior remains a loopback HTTP listener with explicit external TLS-termination boundary.
- `CloudBrainServer.java:87-94`: exactly one bundle instance, exactly one `/api/v1/client/turn` context, exactly one `/api/v1/templates/` context, then the unchanged root gateway context.
- Scoped registration count: `CloudTurnHttpHandler.PATH=1`; `CloudTemplateHttpHandler.PATH_PREFIX=1` in `CloudBrainServer.java`.
- Scoped forbidden-pattern scan over the two Java files returned no retry/scheduler/owner/permit/session/ledger/TTL/WebSocket/Base64 matches.
- `git diff --check` returned no whitespace errors; trailing-whitespace scan returned none.

### File SHA-256

- `CloudTurnRoutes.java`: `502DAF66C5485245A964B50092B8C07AEF6444BFD63FAD9EEDBF883110C70D00`
- `CloudBrainServer.java`: `7CC098CF29427B9236D1576872E5930D4B2F1E1BFDCDFE08E1A33C086ADA8B0E`

### Build And Runtime Gate

- Maven was deliberately not run because parallel Java writers remain active and the parent owns the shared build cohort gate.
- No automated tests, runtime/application/server, Task, poller, UI, capture, or input was started.
- No Git mutation was performed.
- Required next gate: parent independent source review with P0/P1/P2, then fresh Cloud `mvn -q clean package` when all Java writers are stable.

## Worker Handoff

- This Worker stops after delivery and waits for parent review.
- Any parent P0/P1/P2 remains on this same card for repair; this Worker does not self-approve or claim another card.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1

- 审查时间：`2026-07-15T15:54:00-04:00`；父级独立展开 `CloudTurnRoutes.java`、`CloudBrainServer.java`，
  并沿实际注册边界回读两个 handler，而非只接受 Worker 交付说明。
- 本卡两文件结论：route bundle 只创建一次，`/api/v1/client/turn` 与 `/api/v1/templates/` 各注册一次，旧 route、
  loopback HTTP 与外部 TLS termination 边界均保留；本卡自身未见 P0/P2。
- 总体结论：`INTEGRATION BLOCKED / BUILD COHORT PENDING`；`P0=0 / P1=1 / P2=0`。
- **P1（上游写集）：** 实际激活的 `CloudTemplateHttpHandler.java:58` 仍使用
  `Headers.getFirst("Authorization")`，重复 Authorization values 可由首值绕过唯一认证头约束。TURN-04 已对
  `/turn` 修复同类问题，但模板 endpoint 经 TURN-05 注册后仍会真实暴露该歧义。
- 影响：同一 Cloud server 的两个新 endpoint 对重复安全头采用不同策略，template 下载认证 fail-closed 不完整。
- 返修归属：不得修改 TURN-05 两文件；已把精确修复退回原 `TURN-03B` write set，要求完整 values list 恰好一个且
  exact match，保持现有 401/WWW-Authenticate 与其它 path/catalog/ETag 语义。TURN-03B Repair #2 通过后，
  父级直接复验本卡集成状态。

## Parent Re-review #1

- 复审时间：`2026-07-15T15:58:00-04:00`；父级确认 TURN-03B Repair #2 已把 template endpoint 的
  Authorization 收紧为完整 values list 恰好一个且 exact match，TURN-05 两个 route 文件未被返修越界修改。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`，原 integration P1 关闭，owner 释放。
- 剩余门仅为 Cloud Java writers 稳定后的父级 `mvn -q clean package` cohort；本轮不启动 server/runtime。
