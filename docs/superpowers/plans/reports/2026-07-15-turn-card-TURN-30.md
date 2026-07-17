# TURN-30 - Xiuluo TaskTracker real caller cutover

## READY / PARENT FROZEN BRIEF - 2026-07-15T22:36:43-04:00

- 类型：`COUNT`；唯一 `countUnit=XiuluoTaskV2::taskTrackerCaller`；`countDelta=+1`；startDependsOn=`TURN-29`
  source/test-source review passed。父级是唯一 manager/final reviewer，Worker 不得自批。
- Exact production write set：仅
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`。
- Exact test/report write set：
  - Create/modify only
    `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoTaskTrackerTurnContractTest.java`
  - 本固定报告 true EOF append。
- 其余两仓文件全部只读；不得修改 TaskTracker core、协议、Service、context、POM、其它 Task/测试/报告。
- 先完整核对 `docs/业务逻辑.md` 修罗 tracker shortcut、维护后重读、首次/后续 miss、park/terminal 与
  `696a12b0` Task 源码。真实 runnable caller 必须只调用 TURN-29 typed Cloud Service；保持 phase、顺序、
  click/confirm、fallback、park、retry 次数和 terminal 完全不变，不得恢复 snapshot/local OCR/local tracker 算法。
- Named test 必须实际调用 production Task caller，覆盖 tracker hit/miss、维护后 fresh read、click success/failure、
  expected/incidental combat、park/terminal/checkpoint，且每次显式 tracker read 只有一个 UUID/command、失败无自动 retry。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不做 Git mutation；交付
  `CLAIMED` 后只写 `SOURCE+TEST SOURCES DELIVERED`，等待父级审查与 stable-writer cohort。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-30 parent frozen brief -->

## CLAIMED - 2026-07-15T22:41:39-04:00

- Role: CR271 TURN-30 implementation Worker; parent remains the only manager/final reviewer.
- Claimed exact write set: Cloud `XiuluoTaskV2.java`, `XiuluoTaskTrackerTurnContractTest.java`, and true-EOF append to this report only.
- Baseline gate read: authority plan Sections 14-19, `docs/业务逻辑.md` Xiuluo tracker shortcut/fallback table, and `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` as the frozen business authority.
- No Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input and no Git mutation.

<!-- TRUE_EOF: TURN-30 claimed -->

## CLAIMED - 2026-07-15T22:41:38-04:00

- Role: CR271 TURN-30 implementation Worker; parent remains the sole manager/final reviewer.
- Claimed production write set: only
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`.
- Claimed named-test write set: only
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoTaskTrackerTurnContractTest.java`.
- This report is the only writable Markdown. All other files in both repositories remain read-only. No Git mutation,
  Maven/JUnit/compile, runtime/application/server/Task/UI/capture/input is authorized for this Worker.
- Baseline gate read before claim: `docs/业务逻辑.md` lines 215-226, 1115-1214 and 1253-1299; authoritative
  plan Sections 14-19; `docs/ACTIVE_WORK.md` top CR271 entry. No approved business difference.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-30 CLAIMED -->

## REPLACEMENT CLAIMED - 2026-07-15T23:03:40-04:00

- Role: CR271 TURN-30 replacement implementation Worker after the prior Worker became unreachable; parent remains
  the sole manager/final reviewer, and this Worker is not a reviewer.
- Existing shared-workspace content was preserved. The production target still has its pre-claim SHA-256
  `B6DFA9F5F6F9B22DA853F3AA57CAF16E48511303BD379C38BB68F1CDD39CF3D8`; the named test does not yet exist.
- Exact writable set remains only Cloud `XiuluoTaskV2.java`,
  `XiuluoTaskTrackerTurnContractTest.java`, and true-EOF appends to this report. All other files in both repositories
  remain read-only; no rollback, overwrite, cleanup, commit, or other Git mutation is authorized.
- Baseline gate re-read before replacement claim: complete `AGENTS.md`, complete `docs/DHXY_CONTEXT.md`,
  `docs/ACTIVE_WORK.md` top, `docs/业务逻辑.md` Xiuluo tracker rules and `696a12b0` fallback table, authority plan
  Sections 14-19, and this frozen card. No Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-30 REPLACEMENT CLAIMED -->

## SOURCE+TEST SOURCES DELIVERED - 2026-07-15T23:29:12-04:00

- Worker role only. Parent remains the sole manager/final reviewer; this entry is not an approval,
  closure, or review conclusion.
- Production source delivered:
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java`
  - pre-replacement SHA-256: `B6DFA9F5F6F9B22DA853F3AA57CAF16E48511303BD379C38BB68F1CDD39CF3D8`
  - delivered SHA-256: `46F9665999F644BE63B7F27E772429E68190322FBDE487641CBEFF0F747F519A`
  - accept-time background tracker work now binds the original exact `TaskExecutionContext` and calls
    `TaskTrackerPanelService.readXiuluoTrackerPanel(...)` once. The old
    `readXiuluoTrackerPanelFromSnapshot(...)` caller and old background snapshot-parser helper have
    zero remaining references in this production file.
  - typed recognition miss remains ordinary fallback data. Exceptional typed turn/transport/checkpoint
    terminals remain exceptional through the accept future and are not converted into tracker miss or
    retried automatically.
  - startup hit, accept-future consumption, maintenance-cleared fresh read, pathing-terminal explicit
    re-read, absolute move+click order, first/later fallback split, expected/incidental combat split,
    park wait spec, retry counts and terminal phases remain on the `696a12b0` flow. The added null
    window-relative argument only adapts the current retained `XiuluoRoundContext` signature; the
    baseline screen-absolute click point and behavior remain unchanged.
- Named test source delivered:
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/task/xiuluo/XiuluoTaskTrackerTurnContractTest.java`
  - delivered SHA-256: `F42E903C38D6FCF5BD29595C1C22EF5AEB9495850606C1738890399B8EAA0480`
  - four production-caller contract methods cover accept-future hit, tracker hit/miss, maintenance fresh
    read, initial/later click-failure fallback, successful-click park state, pathing-terminal re-read,
    expected/incidental combat policy, checkpoint-before-command, typed terminal propagation, and no
    automatic retry.
  - the scripted TURN-29 port asserts one UUID, one command and one full-window raw-PNG capture for each
    explicit tracker read; future reuse and checkpoint cases assert zero command/UUID.
- Static source evidence only: production `readXiuluoTrackerPanel(...)` has three caller sites (startup,
  fresh/terminal shortcut read, accept background read); forbidden snapshot API/helper searches returned
  zero; production/test brace counts are `1068/1068` and `109/109`; trailing-whitespace count is zero for
  both files.
- Workspace delivery note: Cloud `.gitignore` currently ignores `src/test/`, so the named test exists at
  the exact required path but is hidden by ordinary `git status`; this Worker performed no force-add,
  staging, commit, cleanup, or other Git mutation.
- Unrun gates by frozen instruction: no Maven, JUnit, Java compile/package, runtime/application/server,
  Task/UI, capture, input, or fresh-runtime execution was started. Parent/stable-writer cohort still owns
  compilation and the named-test run before any final review judgment.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。等待父级审查。**

<!-- TRUE_EOF: TURN-30 SOURCE+TEST SOURCES DELIVERED -->

## PARENT SOURCE+TEST SOURCE REVIEW PASSED - 2026-07-15T23:34:00-04:00

- Parent independent review: `P0/P1/P2=0/0/0`. Worker delivery text was not used as approval evidence.
- Production evidence:
  - `XiuluoTaskV2.java:999-1000` keeps the startup-screen read as one TURN-29 typed call.
  - `XiuluoTaskV2.java:1632-1643` consumes the accept future without another command and performs only the
    baseline maintenance/pathing-terminal explicit fresh read when that future is absent or retry count is nonzero.
  - `XiuluoTaskV2.java:3173-3189` binds the original `TaskExecutionContext` inside the async thread and issues
    exactly one typed read. Recognition miss remains typed data; runtime/terminal failure remains exceptional.
  - Independent searches found zero `readXiuluoTrackerPanelFromSnapshot` and zero old
    `scheduleAcceptTrackerBackgroundParse` reference in the delivered production file.
- Named-test source evidence:
  - `XiuluoTaskTrackerTurnContractTest.java:104-137` covers accept-future reuse and maintenance fresh read;
    `:140-219` covers miss, first/later fallback and the pathing-terminal explicit read;
    `:222-264` proves async exact-context binding and terminal propagation with one command;
    `:267-345` freezes checkpoint, expected/incidental combat and park/wake semantics.
  - `XiuluoTaskTrackerTurnContractTest.java:509-529` asserts zero-command reuse/checkpoint and exactly one UUID,
    one command and one full-window raw-PNG CAPTURE for every explicit read.
- Independent SHA-256 verification matches the delivered values:
  production `46F9665999F644BE63B7F27E772429E68190322FBDE487641CBEFF0F747F519A`, test
  `F42E903C38D6FCF5BD29595C1C22EF5AEB9495850606C1738890399B8EAA0480`.
- Result: source and named-test source review passed. Maven/JUnit/compile remain in the stable-writer cohort;
  this is not yet `CARD APPROVED/CLOSED` and does not count before its named gate succeeds.

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

<!-- TRUE_EOF: TURN-30 PARENT SOURCE+TEST SOURCE REVIEW PASSED -->
