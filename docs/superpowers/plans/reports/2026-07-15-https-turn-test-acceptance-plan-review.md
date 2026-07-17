# CR271 HTTPS Turn Test Acceptance Plan Review

## Parent conclusion

- Decision: `PARENT TEST-PLAN APPROVED / IMPLEMENTATION PAUSED`
- Severity: `P0/P1/P2=0/1/0`, with the P1 closed by the revised authority plan.
- Business baseline: `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`.
- Approved difference: none. `无已批准业务差异；按基线等价迁移。`

## P1 found

Before this review, the two repositories contained no HTTPS turn unit/contract tests. The card plan used source
review and compile/package as its Java acceptance gate, so `SOURCE APPROVED` did not prove:

1. Cloud action JSON can be parsed and executed by DHXY.
2. DHXY success, failed-step, stopped, or uncertain outcomes are interpreted correctly by Cloud.
3. Raw PNG multipart bytes match their metadata and reach the original waiting Cloud caller.
4. Duplicate/late action or stable task-start delivery cannot cause a second execution.
5. Migrated Services and Tasks preserve 696a12b0 conditions, order, counts, delays, fallback and terminal rules.

Impact: a source-review-clean card could compile while still violating the wire contract or business sequence.
That is a release blocker, not a later nice-to-have test task.

## Closed acceptance design

The user explicitly authorized `HTTPS_TURN_CONTRACT_TEST_FAMILY`. Authority-plan Section 19 now freezes:

- four Foundation debt cards `TURN-T01..T04`;
- a unique test write set for each unimplemented Java card;
- protocol golden JSON and byte/SHA parity;
- scripted `COMPLETED`, `FAILED`, `STOPPED`, and `DUPLICATE_OR_UNCERTAIN` outcomes;
- failed-step short circuit with remaining steps `NOT_RUN`;
- raw PNG byte, SHA, dimensions, region and source-step assertions;
- stable `startRequestId`/ack, duplicate suppression and pause/stop lifecycle assertions;
- per-Service and per-Task 696a12b0 equivalence assertions;
- exact Maven `-Dtest` commands and a separate compile gate.

Non-Java manifest cards use exact path/reference/SHA review. Delete cards add named source guards, zero-reference
scans and compile. TURN-41 remains the separate real Win32 user-runtime gate; unit tests do not replace it.

## Approval rule

```text
SOURCE DELIVERED + TEST DELIVERED
  -> PARENT SOURCE REVIEW
  -> PARENT TEST REVIEW
  -> NAMED TESTS exit 0
  -> APPLICABLE COMPILE exit 0
  -> CARD APPROVED
```

Happy-path-only tests, metadata-only image checks, continued execution after a failed step, weakened fixtures, or
mocks that bypass the card's production boundary are `TEST BLOCKED` and return to the original Worker.

## Verification performed in this review

- Read both repositories' existing test trees and Maven test configuration.
- Read the complete authority card registry and performed a forward and reverse card-to-test mapping pass.
- Obtained two independent non-binding read-only audits: Foundation/transport/runtime and business/Task/delete.
- Confirmed no Java/test/runtime/server/Task/UI/capture/input or Git operation was performed in this planning pass.
- Test execution status: not run; no test source exists yet. Implementation remains paused.
