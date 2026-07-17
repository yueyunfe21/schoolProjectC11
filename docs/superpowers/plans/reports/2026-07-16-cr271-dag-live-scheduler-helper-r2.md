# CR271 DAG live scheduler helper R2

## HELPER CLAIMED - 2026-07-16T04:41:49.707-04:00

- helper：Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`。
- 角色：CR271 DAG/写集排班 helper；不是 implementation Worker、reviewer、manager 或 final reviewer，不能审核自己刚交付的 TURN-28P，也不能批准或阻断任何卡。
- 唯一写集：本报告 append-only。Java、测试、fixture、计划、`ACTIVE_WORK`、原卡及其它报告全部只读。
- 工作范围：独立读取权威 DAG、所有当前 true EOF 状态和两仓 status；列出 TURN-28P/33 source gate 后立即 READY 的真实 implementation、依赖、生产/测试写集冲突与 Internal/External lane 顺序，并识别当前不等待这两个门的互斥 READY 实现。
- 禁令：不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行 Git mutation，不伪造 owner/claim/READY，不写 `APPROVED/BLOCKED/CLOSED`。

<!-- TRUE_EOF: CR271 DAG SCHEDULER HELPER R2 CLAIMED Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T04:41:49.707-04:00 -->

## PRECHECK_COMPLETE - 2026-07-16T04:51:24.5552180-04:00

### 1. Evidence boundary and latest true-EOF snapshot

- This is scheduling evidence only. It does not approve, block, close, claim, or change the READY state of any card.
- Read-only inputs included `AGENTS.md`, `DHXY_CONTEXT.md`, the current CR271 header in `ACTIVE_WORK.md`, plan Sections
  14-19, the HTTPS-turn protocol and business rules, both repository statuses, and every current structured true-EOF
  `CLAIMED/DELIVERED/REPAIR/PRECHECK` report. Existing dirty/untracked content in both repositories remains protected.
- `TURN-28P` changed while this scan was running. Its latest fixed-card true EOF is now parent
  `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED` at `04:48:07.493`. Its two independent delivery reviewers are
  read-only and still at `REVIEW CLAIMED`; named tests and dual compile/build remain later approval gates. The latest
  parent review explicitly says this source gate may release External A/B consumers now.
- `TURN-22 Repair #1` was then parent-marked ready for External A and has a real fixed-card true-EOF claim by External A
  at `04:50:39`. It is the current implementation owner for that two-file repair.
- `TURN-28` has all formal source predecessors available, but its fixed implementation card does not yet exist at this
  snapshot. `TURN-28 launch preflight helper R2` is only `HELPER CLAIMED`; External B must wait for the parent-frozen
  card plus an explicit parent READY assignment before claiming Java.
- `TURN-33` latest true EOF remains `REPAIR #1 PARTIAL P1-1_IMPLEMENTED / P1-2_BLOCKED / PARENT DECISION REQUIRED`.
  Its exact write set must remain exclusive to the existing repair lane. It has not released the source gate for
  `TURN-34A` or `TURN-34B`.
- External A/B/C/D are implementation lanes, not reviewers. A is now implementing TURN-22 Repair #1; B is reserved for
  TURN-28; C is reserved for TURN-34A; D is reserved for TURN-34B.

### 2. Immediate release graph

| Source gate/event | Formal dependent implementation | Exact remaining condition | Scheduling result (non-binding) |
|---|---|---|---|
| TURN-28P source gate passed | TURN-22 Repair #1 | Parent READY + fixed-card claim | Already satisfied and claimed by External A |
| TURN-28P source gate passed | TURN-28 | Parent freezes fixed card/brief and writes READY | Immediate next candidate for External B; TURN-22 is priority, not a formal dependency |
| TURN-33 source gate passes | TURN-34A | Parent freezes final one-file brief and writes READY | Immediate candidate for External C |
| TURN-33 source gate passes | TURN-34B | TURN-22 Repair #1 must also pass its parent source gate; then parent READY | External D remains waiting until both source gates are present |
| TURN-28 source gate passes | TURN-27 | Parent freezes final TURN-28 API plus TURN-27 scope ambiguities | First downstream Navigation candidate; suitable for an Internal implementation lane |
| TURN-22 + TURN-34A + TURN-34B pass | TURN-34C | Parent freezes one-file AutoBattle brief | Next orchestration candidate after both caller services converge |

Formal dependency facts from the authoritative registry:

- `TURN-22`: `S=14+18+23+generic queue-owned post-click mechanics`; TURN-28P now supplies the generic mechanics.
- `TURN-28`: `S=23+24+26+28P`. TURN-22 and TURN-33 are not formal start dependencies.
- `TURN-34A`: `S=19+20+21+23+24+33`; only TURN-33 remains unresolved at this snapshot.
- `TURN-34B`: `S=21+22+23+26+33`; both TURN-22 Repair #1 and TURN-33 remain unresolved.
- `TURN-27`: `S=15+18+23+24+26+28`.
- `TURN-34C`: `S=19+21+22+23+34A+34B`.

The parent review's explicit `04:48:07.493` statement resolves the earlier scheduling ambiguity around symbolic
`TURN-24`: it states External B's TURN-28 may now be marked READY. This helper does not independently change that
state and does not add a synthetic `S=TURN-22` edge to TURN-28.

### 3. Production/test mutex map for the four External queue heads

| Lane/card | Production write set | Sole named-test write set | File conflict with other queue heads |
|---|---|---|---|
| External A / TURN-22 Repair #1 | `CloudTeamReturnPortAssembly.java` only; `TeamReturnService.java` is read-only in Repair #1 | `service/TeamReturnTurnContractTest.java` | None |
| External B / TURN-28 | `NpcClickService.java`, `ObjectiveTextRecognizer.java`, `SmartClickRecognizer.java` | `service/NpcClickTurnContractTest.java` | None |
| External C / TURN-34A | `AutoCombatService.java` only | `service/AutoCombatServiceTurnContractTest.java` | None |
| External D / TURN-34B | `TaskMaintenanceService.java` only | `service/TaskMaintenanceTurnContractTest.java` | None |

All four named tests resolve under Cloud test root
`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/`. Each card also owns only its parent-
frozen append-only fixed report. The four production sets, four test files, and four reports are pairwise disjoint.

Relevant predecessor ownership is also file-disjoint:

- TURN-28P owns protocol/input/capture/keyboard/executor files plus its focused tests/fixtures; its implementation owner
  is released at source gate. Current TURN-28P delivery reviewers are read-only and do not conflict with A/B Java.
- TURN-33 owns `SummonSkillService.java`, `CloudSummonSkillWholePassCapability.java`,
  `CloudTaskExclusiveInteractionAuthority.java`, and `SummonSkillTurnContractTest.java`. It has no file overlap with
  TURN-34A/B, but remains their formal/API predecessor.
- TURN-27 later owns `NavigationService.java`, `CloudMiniMapCoordinateReadability.java`, `MiniMapPointResolver.java`,
  `NavigationRoutePlanResolver.java`, and `NavigationTurnContractTest.java`; it is file-disjoint from A-D but must
  consume TURN-28's reviewed final API.
- TURN-34C later owns only `task/AutoBattleTask.java` and `task/AutoBattleTaskTurnContractTest.java`; it is file-disjoint
  but logically waits for TURN-22/34A/34B.

### 4. Recommended rolling lane order

1. **External A:** continue the already claimed TURN-22 Repair #1. Do not assign another writer to its assembly/test.
2. **External B:** as soon as the parent freezes `2026-07-16-turn-card-TURN-28.md` and writes READY, claim TURN-28 and
   run it concurrently with A. The formal DAG and exact write sets permit this concurrency.
3. **External C:** remain reserved for TURN-34A. Claim only after TURN-33's repair receives a parent source gate and the
   parent freezes the final AutoCombat brief.
4. **External D:** remain reserved for TURN-34B. Claim only after both TURN-22 Repair #1 and TURN-33 receive parent
   source gates and the final TaskMaintenance brief is frozen.
5. **Internal pool now:** finish the existing TURN-28 launch preflight and TURN-28P read-only reviews, then release those
   helper/reviewer slots. Do not duplicate External A/B implementation ownership. TURN-33 may resume only after the
   parent resolves its P1-2 business-contract choice; a second TURN-33 writer would violate the write set.
6. **Next Internal implementation after B:** after TURN-28 source delivery passes and the parent freezes its final API,
   prioritize TURN-27. It can run concurrently with later TURN-34A/34B because all production/test paths are disjoint.
7. **After caller convergence:** schedule TURN-34C, then roll the whole-task cards only when their exact predecessor sets
   are satisfied: TURN-35 waits for 22/27/28/34A/34B; TURN-36 waits for 27/28/34A plus its separately frozen open-main-
   bag prerequisite; TURN-37 waits for 22/27/28/34A/34B.

### 5. Current answer: is there another independent READY implementation?

- **One real implementation is already active:** TURN-22 Repair #1 is true-EOF `CLAIMED` by External A.
- **One additional immediate candidate exists:** TURN-28 no longer waits for either TURN-28P or TURN-33. All formal
  source dependencies are present, and the parent has explicitly authorized marking External B READY; however, the
  fixed TURN-28 implementation card/brief is still missing, so B must not claim or edit yet.
- **There is no third independent implementation that can legally start now.** TURN-34A waits for TURN-33;
  TURN-34B waits for TURN-22 Repair #1 plus TURN-33; TURN-27 waits for TURN-28; TURN-34C and TURN-35/36/37 wait for
  those caller cards. Existing source-approved/build-pending cards are verification cohort debt, not new implementation
  cards. TURN-33's partial repair is an existing parent-decision wait, not a free READY card for a replacement writer.

### 6. Report-validity cautions

- `TURN-28 readiness`, `TURN-34A readiness`, and `TURN-35 readiness` currently lack a structured HTML true EOF. Their
  source evidence may help the parent, but none can substitute for a parent-frozen implementation card/READY marker.
- `TURN-34B`, `TURN-34C`, `TURN-36`, and `TURN-37` have structured precheck EOFs, but precheck completion is not an
  implementation claim or approval.
- Maven/JUnit/compile/runtime/input were not run. This helper made no Java/test/plan/ACTIVE_WORK/original-card change and
  performed no Git mutation.

## PRECHECK_COMPLETE

- helper: Maxwell `019f69f0-014a-7543-bfbf-b18c8864e411`
- result: live DAG, exact dependencies, External/Internal rolling order, and production/test mutex map recorded
- current implementation front: External A TURN-22 Repair #1 active; External B TURN-28 awaiting fixed card/READY
- additional immediately startable implementation beyond A/B: none
- authority: scheduling evidence only; parent remains the sole manager/final reviewer
- next action: remain online and wait for parent reassignment

<!-- TRUE_EOF: CR271 DAG SCHEDULER HELPER R2 PRECHECK_COMPLETE Maxwell 019f69f0-014a-7543-bfbf-b18c8864e411 2026-07-16T04:51:24.5552180-04:00 -->
