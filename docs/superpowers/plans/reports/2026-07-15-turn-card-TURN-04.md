# TURN-04 Cloud Bounded JSON/Multipart Ingress

## CLAIMED

- Claimed at: `2026-07-15T14:56:03-04:00`
- Worker role: TURN-04 implementation Worker only; not manager/reviewer and cannot self-approve.
- Card type/count: `INFRA`, `countDelta=0`.
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Workspace/transport baseline: DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`; Cloud `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01` with all existing dirty/untracked preserved.
- Dependency baseline: TURN-01A/B/C protocol DTOs are source-approved, TURN-02 `CloudTurnExchange` is source-approved, and TURN-01D validator/parity is an approval dependency still being implemented in a disjoint write set.

## Exact Write Set

Cloud repository `D:\mavenProject\dhxy-cloud-brain`:

- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/TurnMultipartReader.java`
- `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnHttpHandler.java`

DHXY repository:

- `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-04.md` (append-only delivery report)

No other source, plan, CR card, route registration, test, or configuration file is owned by this Worker.

## Contract And Risk Notes

- Accept only authenticated `POST /api/v1/client/turn` requests with strict `application/json`, or strict `multipart/form-data` containing exactly `metadata` JSON and `frame` PNG parts.
- Bound JSON at 256 KiB and PNG at 8 MiB. Reject malformed boundary/CRLF/headers, duplicate or extra parts, trailing bytes, and declared/actual media-type mismatches.
- Deserialize with strict duplicate detection, unknown-property rejection, and trailing-token rejection; delegate protocol validity and one-slot correlation to `TurnProtocolValidator` and `CloudTurnExchange`.
- A legal `200 application/json` acknowledges the submitted previous outcome even when the response is `IDLE`; no new acknowledgement field or lifecycle state is introduced.
- Map wrong method/path/content type/auth/body/correlation to typed fail-closed HTTP responses. Do not copy the single-slot state machine and do not register the route owned by TURN-05.
- Primary implementation risks are multipart delimiter ambiguity, unbounded header/body reads, and accidentally treating an exchange conflict as a generic `400`. These are contained inside the exact write set.
- No approved business difference: transport/mechanics only, equivalent to the `696a12b0` baseline.

## Status

`CLAIMED / IMPLEMENTING`

## SOURCE DELIVERED / BUILD COHORT PENDING

- Delivered at: `2026-07-15T15:01:05-04:00`.
- Worker conclusion only: source delivered for parent review. This is not `APPROVED`, not `CLOSED`, and does not change the `189/407` ledger.
- `TurnMultipartReader` reads a single bounded in-memory body, requires the declared boundary at byte zero, permits no preamble/epilogue, and accepts exactly one `metadata` plus one `frame` part with strict CRLF framing. Part headers are printable ASCII, individually bounded to 8 KiB, unique, and limited to `Content-Disposition` plus `Content-Type`.
- JSON metadata is capped at 256 KiB and parsed with duplicate detection, unknown/trailing-token rejection, primitive-null/enum-number/scalar-coercion rejection, then passed to `TurnProtocolValidator`. Raw PNG is capped at 8 MiB, retains its bytes unchanged, and must have the PNG signature.
- `CloudTurnHttpHandler` accepts only the exact no-query path and authenticated `POST`; JSON-only and multipart requests share one handler. It maps path/method/auth/content/body/limit/correlation failures to typed fail-closed JSON responses and never registers a route.
- The handler invokes the existing `CloudTurnExchange.exchange(...)` exactly once. It does not copy slot state. PNG decode, exact dimensions, metadata equality, and SHA-256 verification remain owned by that exchange. A legal `200`, including `IDLE`, is emitted only after a supplied previous outcome is reported accepted.
- No owner/permit/session/ledger/compaction/TTL/durable workflow/automatic retry, Base64 image, poller, server startup, test, or runtime behavior was added.

### File SHA-256

- `TurnMultipartReader.java`: `186804298e14ed9cee08d9563d81c97001b3d91c5214fc1723201371cf569e3e`
- `CloudTurnHttpHandler.java`: `e6f8c760577946928b22611054824c34be5805f3a70a9b1da4702a87c13c3272`

### Scoped Evidence

- `rg` found size gates at `TurnMultipartReader.java:25-27`, per-part enforcement at `:102/:111`, exact two-part enforcement at `:133`, and validator delegation at `:138`.
- `rg` found one exchange call at `CloudTurnHttpHandler.java:149`, explicit previous-outcome acknowledgement gating at `:153`, typed `401/405/409/413` paths at `:80/:85/:111/:114/:218/:236`, and strict JSON configuration at `:57-63`.
- Scoped forbidden-pattern scan over the two files returned `FORBIDDEN_OR_OUT_OF_SCOPE_MATCHES=0` for route registration, duplicate state machine primitives, owner/permit/session/ledger/compaction/TTL/retry, Base64, and poll references.
- Both files have zero trailing-whitespace lines. Cloud status shows only these two target files as new in this Worker write set; all pre-existing dirty/untracked files remain untouched.

### Build And Runtime Gate

- Maven was deliberately not run because parallel Java writers remain active and this card belongs to the shared Foundation build cohort.
- No automated tests, application/server, Task, poller, UI, capture, or input were started.
- Required next gate: parent independent source review, then fresh Cloud `mvn -q clean package` when all Java writers are stable. Any parent P0/P1/P2 stays on this same card for repair.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## Parent Source Review #1

- 审查时间：`2026-07-15T15:14:00-04:00`；父级独立逐行审查两个 Java 文件。
- 结论：`REPAIR REQUIRED / BUILD COHORT PENDING`；`P0=0 / P1=1 / P2=0`。
- **P1 - Authorization 重复头存在解析歧义。** `CloudTurnHttpHandler.java:113` 使用
  `getFirst("Authorization")`，因此请求携带多个 Authorization 值且第一个正确时仍会通过；同文件对
  `Content-Type`/`Content-Length` 已要求唯一值。代理与 backend 对重复安全头可能有不同解释，Bearer 边界必须
  fail-closed。
- 返修条件：只在本卡写集内要求 Authorization 恰好一个值且精确等于预期 Bearer；缺失、重复、空白或
  不匹配统一返回现有 typed `401 UNAUTHORIZED`。不得改 multipart、exchange、route 或其它错误映射。
- 复验点：父级确认不存在 Authorization 的 `getFirst` 宽松路径，且其它已通过语义保持不变。
- Maven：writers 活动中，仍不运行；本结论不允许记 APPROVED/CLOSED。

## Repair #1 DELIVERED / BUILD COHORT PENDING

- Delivered at: `2026-07-15T15:36:26-04:00`.
- Parent Review #1 finding addressed only in `CloudTurnHttpHandler.java`; multipart parsing, exchange behavior, route ownership, response acknowledgement, and all other HTTP mappings are unchanged.
- Authorization now reads the complete `Headers.get("Authorization")` values list. A request passes only when the list exists, contains exactly one value, and that value exactly equals the configured `Bearer <token>` string.
- Missing, duplicated, blank, or mismatched Authorization values all reach the existing `401 UNAUTHORIZED` branch and retain the existing `WWW-Authenticate: Bearer` response behavior.
- The Authorization-specific `getFirst("Authorization")` path has been removed.

### Repair #1 SHA-256 And Scoped Evidence

- `CloudTurnHttpHandler.java`: `f1dca91cd6754b05919d4668caf38ce40ba783521d1794ef366b7fecc92b5c8c`
- `TurnMultipartReader.java` unchanged: `186804298e14ed9cee08d9563d81c97001b3d91c5214fc1723201371cf569e3e`
- `CloudTurnHttpHandler.java:113-117` contains all three fail-closed predicates: null list, `size() != 1`, and exact expected-header comparison.
- Scoped scan result: `authorizationGetFirstMatches=0`; `strictAuthorizationPredicates=3`; trailing whitespace remains `0`.
- No Maven, tests, runtime/application/server, Task, poller, UI, capture, input, or Git mutation was run. Status remains source-delivered pending parent re-review and the shared build cohort.

Worker does not self-approve. Any further P0/P1/P2 remains on this same card.

## Parent Re-review #1

- 复审时间：`2026-07-15T15:39:00-04:00`；父级独立读取 Repair #1 当前源码。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`。
- 证据：`CloudTurnHttpHandler.java:113-117` 读取完整 Authorization values，严格要求 `size()==1`
  且唯一值精确匹配 Bearer；缺失、重复或不匹配统一进入现有 typed `401`，旧 `getFirst` 路径已消失。
  multipart/exchange/routes/其它错误映射未改变。
- 原 P1 已关闭，源码 owner 释放；`TURN-05` 可按依赖立即领取。Maven 仍待 writers 稳定后统一执行。
