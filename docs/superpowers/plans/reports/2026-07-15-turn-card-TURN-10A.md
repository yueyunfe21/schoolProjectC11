# TURN-10A BagService Closed Adapter

## Claim

- Card: `TURN-10A`
- Role: Internal implementation Worker only; parent remains the sole manager/final reviewer.
- countDelta: `0`
- Exact Java write set: `src/main/java/com/bot/dhxy/cloud/turn/local/BagLocalOperationExecutor.java`
- Report: `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-10A.md`
- Baseline read: `AGENTS.md`, `docs/DHXY_CONTEXT.md`, CR271 at the top of
  `docs/ACTIVE_WORK.md`, the authoritative HTTPS-turn card plan, protocol foundation/design,
  current turn DTO/validator source, current `BagService` and bag models, and both relevant
  current/baseline call paths.
- Workspace protection: the pre-existing dirty/untracked tree was retained; no Git mutation,
  rollback, cleanup, runtime, application, server, Task, poller, UI, capture, input, tests, or
  Maven command was performed.

## SOURCE DELIVERED

- Status: `SOURCE DELIVERED / PARENT REVIEW REQUIRED`.
- Final approval remains dependent on `TURN-01D`; this Worker does not approve itself and does
  not claim another card.
- Delivered file SHA-256:
  `3A8ECC1D67C1CD1EB1C086B1A957BDA5FCFD389AEEAA70CFC8DFF771A250B3DA`.

### Exact evidence

- `BagLocalOperationExecutor.java:38-46` exposes one closed entry and accepts only
  `BAG_RETURN_ITEM` / `BAG_USE_INCENSE`; null/unknown operations return a failed
  `LocalServiceExecution` before Service invocation.
- `BagLocalOperationExecutor.java:49-68` validates the return-item variant before invoking
  `BagService.runReturnItemMacroDirectForExclusive(...)`, then maps the existing closed result
  to a typed private JSON record and `LocalServiceExecution.completed(...)`.
- `BagLocalOperationExecutor.java:71-79` rejects all arguments for incense and directly invokes
  `BagService.runUseIncenseMacroDirectForExclusive(...)` once.
- `BagLocalOperationExecutor.java:82-140` converts only the existing typed
  `TurnBagOperationArguments` / `TurnReturnItemCachePoint` variants into existing bag models;
  it preserves the committed null-cache behavior for `USE_CACHED_RETURN_ITEM`.
- `BagLocalOperationExecutor.java:142-175` uses Jackson over private closed result records;
  there is no reflection, arbitrary map, fifth Service, retry, OCR, capture, or input logic.
- Existing read-only atomicity evidence: `BagService.java:251-281` and `:296-303` require execution
  on the input worker and call the existing exclusive cores without acquiring another queue.

### Risk and deferred gates

- Integration invariant: `TURN-10E` / `TURN-11` must invoke this adapter from the existing
  exclusive input callback. The adapter deliberately does not enqueue; `BagService` fail-fast
  enforcement prevents an out-of-bound caller from issuing physical input.
- The adapter serializes only small closed records; Cloud-side parsing/consumption belongs to
  later integration/business cards and was not added here.
- Per the active parallel-writer instruction, no Maven compile/package gate was run. Parent must
  perform the fresh DHXY compile in the stable Java-writer cohort before approval/closure.
- No runtime evidence was produced or requested.

`SOURCE DELIVERED`

## Parent Source Review #1

- 审查时间：`2026-07-15T15:55:00-04:00`；父级独立展开 adapter、`LocalServiceExecution`、
  `BagService.runReturnItemMacroDirectForExclusive/runUseIncenseMacroDirectForExclusive` 与现有 bag model/caller。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`，owner 释放。
- 证据：`:38-46` closed switch 只允许 BAG_RETURN_ITEM/BAG_USE_INCENSE；`:49-79` 保持现有 direct exclusive
  macro 及 null context 既有调用方式；`:82-140` 逐 variant 映射冻结参数/cache point；未知 operation 与非法参数
  在调用 Service 前 fail closed。未增加 queue、input、capture、OCR、retry、反射或第五 Service。
- 剩余门：本卡不单独运行 Maven；由父级在所有 DHXY Java writers 稳定时执行 compile cohort。
