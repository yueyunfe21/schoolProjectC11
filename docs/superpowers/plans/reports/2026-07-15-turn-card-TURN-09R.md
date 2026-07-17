# TURN-09R - atomic mouse sequence protocol/executor repair

## READY / PARENT FROZEN BRIEF - 2026-07-15T21:52:56-04:00

- 状态：`READY`；类型：`FOUNDATION REPAIR`；`countDelta=0`；startDependsOn：`TURN-09`、`TURN-11`。
- 阻断证据：当前 `TurnInputAction` 没有 `MOVE_MOUSE`，且 `LocalTurnActionExecutor` 将每个 mouse INPUT
  分别提交给 `InputActionQueue`。因此 payload 无法等价表达基线的
  `move -> settle WAIT -> click`，即使拆成多个 step 也会允许其它窗口在 step 之间插入。
- 完成边界：
  1. 双仓协议同字节增加 `MOVE_MOUSE`，只接受一个 screen-absolute `x/y`；
  2. DHXY mapper 将其映射为现有 `InputAction.moveMouse(...)`；
  3. `LocalTurnActionExecutor`/`TurnInputStepExecutor` 将一个连续的 mouse INPUT/positive WAIT 片段
     （首尾均为 mouse INPUT）一次性映射并提交为**一个** input-queue request；这同时覆盖
     `MOVE_MOUSE -> WAIT -> CLICK_LEFT` 和 first-aid 等多点击 closed action；
  4. 成功时按原 step index 逐项产出 COMPLETED；队列失败/STOPPED 时从该原子片段 fail-closed，后续 step
     NOT_RUN，不伪造成功；其它既有单步 mouse、background key、capture、local Service 行为不变；
  5. 不新增 retry、第二 exchange、session/owner/ledger/TTL，也不启动任何 runtime。
- Exact production write set：
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`
  - Cloud `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
  - Cloud `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- Exact test/report write set：
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - 本固定报告。
- Named-test acceptance：双仓 JSON round-trip/validator 覆盖合法 `MOVE_MOUSE` 与多余字段拒绝；DHXY executor
  测试必须证明三 step 只产生一个 queue submission，内部 action 顺序严格为
  `MOVE_MOUSE, SLEEP, CLICK_LEFT`，并另证连续多 click/WAIT 仍为一个 submission；覆盖 queue failure、STOPPED、
  后续 NOT_RUN、其它输入行为不回归。
- Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行 Git mutation；
  父级在全部 Java writers 稳定后运行本卡 named tests 与适用 compile。

**无已批准业务差异；本卡补回计划已经承诺的原子 move+click 基线。**

<!-- TRUE_EOF: TURN-09R parent frozen brief -->

## CLAIMED - 2026-07-15T21:58:16.4028284-04:00

- 身份：CR271 TURN-09R implementation Worker（Codex）；不是 reviewer，父级是唯一 manager/final reviewer。
- Exact production write set：
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputActionMapper.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
  - DHXY `src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`
  - Cloud `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputAction.java`
  - Cloud `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
- Exact test/report write set：
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`
  - DHXY `docs/superpowers/plans/reports/2026-07-15-turn-card-TURN-09R.md`（本卡，仅 true EOF append）。
- 领取边界：保护双仓全部既有 dirty/untracked 与并发写入；不改写集外文件，不执行 Git mutation，不运行
  Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input。

`CLAIMED`

## SOURCE+TEST SOURCES DELIVERED - 2026-07-15T22:16:42.1621572-04:00

- 状态：`SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW + NAMED TESTS + COMPILE PENDING`；`countDelta=0`。
  Worker 到此停止，不自批，不写 `APPROVED/CLOSED`。
- 生产源码与 SHA-256：
  - DHXY `protocol/TurnInputAction.java`：
    `FDDD91B9CF74BC4DFD8015F68FDBFAC43832F2E4E02B590E7606CA7EEE0C911D`
  - DHXY `protocol/TurnProtocolValidator.java`：
    `8119DA1CAE043AFBA669BBF8556B50B1A8C77B08D8DC91BDA40254F82F80DBB3`
  - DHXY `TurnInputActionMapper.java`：
    `1E017B181356DC48D98DF57C27F990C3078D5538F369D034D1E3668F868DED76`
  - DHXY `TurnInputStepExecutor.java`：
    `07E4AFD2902607343500158F6321E7D2A7A2F532D548301CFCAEC216822AA1A2`
  - DHXY `LocalTurnActionExecutor.java`：
    `F1DF9887C81F6BE433160C020BC6B96B6FA5746861E2E6157F47EDC34B96705C`
  - Cloud `protocol/TurnInputAction.java`：
    `FDDD91B9CF74BC4DFD8015F68FDBFAC43832F2E4E02B590E7606CA7EEE0C911D`
  - Cloud `protocol/TurnProtocolValidator.java`：
    `8119DA1CAE043AFBA669BBF8556B50B1A8C77B08D8DC91BDA40254F82F80DBB3`
- 测试源码与 SHA-256：
  - DHXY `protocol/TurnActionGoldenJsonTest.java`：
    `D955ED0BCCEBD12F240915F6FBDF17E0D9DFAC4B4449DCB9C300C70413CB5679`
  - DHXY `protocol/TurnProtocolValidatorContractTest.java`：
    `1687EF6EA058F81471447C230156A950C47E1C69A0339F0A180218DBE2CABAB1`
  - DHXY `TurnInputStepExecutorContractTest.java`：
    `AB80B47A29276B76E7A1A27A5906E6111C6D08C8D428572D92719F78DCA5130C`
  - DHXY `LocalTurnActionExecutorContractTest.java`：
    `2326F15A0B8845CE01FC78BACBBDE3F1657BFEFE24B1C3CF410894F0663D4DDB`
  - Cloud `protocol/TurnActionGoldenJsonTest.java`：
    `D955ED0BCCEBD12F240915F6FBDF17E0D9DFAC4B4449DCB9C300C70413CB5679`
  - Cloud `protocol/TurnProtocolValidatorContractTest.java`：
    `1687EF6EA058F81471447C230156A950C47E1C69A0339F0A180218DBE2CABAB1`

### 静态实现证据

- 双仓两个 production protocol 文件分别 SHA 完全一致；双仓两个 protocol test 文件也分别 SHA 完全一致。
  `MOVE_MOUSE` 仅允许 non-null screen-absolute `x/y`，`endX/endY/scrollDelta/key/text` 任一带值均由 validator
  fail-closed；未知 JSON input 字段由 strict mapper 拒绝。
- `TurnInputActionMapper` 将 `MOVE_MOUSE` 直接映射为现有 `InputAction.moveMouse(x,y)`，仍校验点位属于本次
  refreshed `TurnWindowRect`，不缩放、不转相对坐标。
- `LocalTurnActionExecutor` 只在当前位置存在后续 mouse INPUT 时形成闭合片段；片段内部只跨 mouse INPUT 与
  positive WAIT，且截到最后一个 mouse INPUT，尾随 WAIT 留在原单步路径。`TurnInputStepExecutor` 在提交前完整
  映射为 ordered `InputAction` list，并只调用一次 `inputActionQueue.submitAndWait(...)`。
- 单一 production queue sink 静态计数为 `1`；三个目标 DHXY production 文件中 `InputProvider=0`、
  `submitExclusiveAndWait=0`、transport `exchange=0`。未新增 retry、第二 exchange、owner/session/ledger/TTL 或
  durable workflow。
- 原子请求成功才按原 index 为片段每个 INPUT/WAIT 产出 `COMPLETED/OK`。queue false、interrupt/STOPPED、映射
  异常或无法确认部分进度时，只把片段首 step 作为 terminal FAILED/STOPPED，其余片段与 action tail 全部
  `NOT_RUN`；不伪造部分成功。
- 单步 mouse 因不存在后续 mouse 而继续原 `execute(...)` 路径；background `KEY_TAP`、unsupported key、独立
  WAIT、MATCH click、CAPTURE/failure evidence、LOCAL_SERVICE exact-window context 分支未改。

### 测试源码证据与未运行门

- 双仓 golden/validator 新增合法 `MOVE_MOUSE` round-trip、缺 x/y、全部五类已知多余字段和未知 JSON 字段拒绝。
- `TurnInputStepExecutorContractTest` 新增七种 mouse mapper 覆盖，并直接断言一个 submission 内 action 顺序为
  `MOVE_MOUSE, SLEEP, CLICK_LEFT`，screen-absolute 坐标与 `150ms` 精确不变；既有后台键、queue failure、WAIT、
  STOPPED 用例保留。
- `LocalTurnActionExecutorContractTest` 新增：三 step 成功逐项 COMPLETED；first-aid 形态三 click/两 WAIT 仍只有
  一个 submission；queue failure 从 index 0 FAILED 且其余/tail NOT_RUN；queue interrupt 映射 STOPPED、tail
  NOT_RUN 且不抓 capture/failure evidence。
- 按冻结禁令未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input；所有 fresh
  named-test 命令、父级断言审查与双仓适用 compile 均待父级 writer cohort 稳定后执行。
- 写集外已知 gate：双仓既有 `TurnCoreProtocolGoldenJsonTest` 仍硬编码十个 input enum/十份 input spec；新增
  `MOVE_MOUSE` 后未来全量 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 运行前必须由该测试的 owner/父级同步。用户明确
  `Exact write set 不变`，本 Worker 未越界修改。Cloud 两个测试文件还受既有 `.gitignore:15 src/test/` 隐藏；
  未修改 ignore、未 stage、未执行任何 Git mutation。
- 业务基线核对：`docs/业务逻辑.md`“业务基线使用规则”及五倍普通怪/黄袍绿字“move 与 click 不得拆成可插队
  两段”规则；无已批准业务差异；按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

`SOURCE+TEST SOURCES DELIVERED / PARENT REVIEW PENDING`

## PARENT REVIEW #1 - 2026-07-15T22:23:06-04:00

- 结论：`REVIEW REQUIRED / REPAIR #1`；父级独立审查 `P0/P1/P2=0/1/1`。Worker 自述不作为结论。
- 已通过部分：双仓 `TurnInputAction`、`TurnProtocolValidator` 及两组本卡 protocol tests 的 SHA-256 分别
  完全一致；`MOVE_MOUSE` 保持 screen-absolute、不缩放；`LocalTurnActionExecutor` 的闭合 mouse/positive-WAIT
  片段只调用一次 `InputActionQueue.submitAndWait(...)`，failure/STOPPED 后续 `NOT_RUN`，未发现 retry、第二
  exchange、session/owner/ledger/TTL。
- **P1：既有点名核心 golden test 必然失配。** 双仓
  `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java:59-91` 仍把合同写成
  `TenInputActions`、只提供十个 `inputSpecs`，并硬编码 WAIT/CAPTURE/MATCH/LOCAL 为 index `10..13` 与
  `subList(0, 10)`。生产 enum 已有十一项，当前测试会在构造第十一项时越界，不能进入批准门。
- **P2：尾随 WAIT 的关键边界缺少直接回归。** production
  `LocalTurnActionExecutor.java:136-160` 明确声称 trailing WAIT 留在原单步路径，但当前新增测试只覆盖闭合片段
  末尾正好为 mouse INPUT；没有证明 `MOVE -> WAIT -> CLICK -> trailing WAIT -> CAPTURE` 时最后 WAIT 不会被
  吞入同一 queue submission。
- Repair #1 exact write-set amendment（其它文件全部只读）：
  1. 双仓 `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`：改为十一种
     input action，按 enum 顺序补 `MOVE_MOUSE(x,y)` spec，重命名测试，并把后续 index/subList 精确迁移到
     `11..14` / `0..11`；两仓文件必须同字节。
  2. DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`：补 trailing-WAIT
     回归，断言 queue 仍只有一次且内部只有闭合 mouse 片段的三项 action，尾随 WAIT 与后续 CAPTURE 均按原
     step 顺序完成。
  3. 本固定报告：真实 EOF 追加 `REPAIR #1 CLAIMED` 与交付证据。
- 返修不得改 production、协议、POM/.gitignore、其它测试或任何业务路径；不运行 Maven/JUnit/compile/runtime，
  不执行 Git mutation。修复后父级重新逐文件审查；当前 `TURN-19/21/23` 继续受本卡阻断。

<!-- TRUE_EOF: TURN-09R PARENT REVIEW #1 REPAIR REQUIRED -->

## REPAIR #1 CLAIMED - 2026-07-15T22:25:50-04:00

- Identity: `CR271 TURN-09R implementation Worker (Codex)`; implementation worker only, not reviewer.
- Exact repair write set:
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`
  - This fixed report, true-EOF append only.
- Scope: migrate the byte-identical core golden contract from ten to eleven input actions and add the direct
  `MOVE -> WAIT -> CLICK -> trailing WAIT -> CAPTURE` queue-boundary regression required by Parent Review #1.
- Guardrails: production/protocol/POM/`.gitignore`/all other tests are read-only; preserve all dirty/untracked and
  concurrent work; no Maven/JUnit/compile/runtime/application/server/task/UI/capture/input execution; no Git mutation.

<!-- TRUE_EOF: TURN-09R REPAIR #1 CLAIMED -->

## SOURCE+TEST SOURCES REDELIVERED - 2026-07-15T22:28:27-04:00

- Status: `SOURCE+TEST SOURCES REDELIVERED / PARENT RE-REVIEW + NAMED TESTS + COMPILE PENDING`.
  Worker stops at redelivery, does not self-review, and does not write `APPROVED/CLOSED`.
- Repair #1 test sources and SHA-256:
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`:
    `A62A36C4F743F9700CECFC712876610A507B5FC00A55FF88B4552F04FCFF86CB`
  - Cloud `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnCoreProtocolGoldenJsonTest.java`:
    `A62A36C4F743F9700CECFC712876610A507B5FC00A55FF88B4552F04FCFF86CB`
  - DHXY `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`:
    `BDB3980E2EA6497A960D71D9696B24730613000A5FAAFA524999DA4011ACA366`
- Static evidence:
  - Byte comparison of the two `TurnCoreProtocolGoldenJsonTest.java` files returned `BYTE_EQUAL=True`; both
    closed-enum assertions now start with `MOVE_MOUSE`, the renamed `ElevenInputActions` method contains exactly
    eleven `TurnInputSpec` constructors in enum order, and the tail indices/sublist are `11..14` / `0..11`.
  - No stale `TenInputActions` or `subList(0, 10)` remains in the core golden file.
  - `trailingWaitAfterClosedMouseFragmentRunsAsOriginalStepBeforeCapture` constructs
    `MOVE(0) -> WAIT(1) -> CLICK(2) -> WAIT(3) -> CAPTURE(4)`, asserts all five original index/type results are
    `COMPLETED`, and asserts exactly one queue submission containing only
    `MOVE_MOUSE, SLEEP, CLICK_LEFT`; the trailing WAIT is excluded and the next event is `capture:roi`.
  - Trailing-whitespace scan returned zero matches for both repaired DHXY test files; the Cloud golden file is
    byte-identical to its DHXY counterpart.
- Scope guard: this worker changed no production/protocol/POM/`.gitignore`/other test file during Repair #1 and
  performed no Git mutation. The fixed report was append-only and is intentionally not self-hashed.
- Not-run gate: per the frozen card and Parent Review #1, no Maven/JUnit/compile/package/runtime/application/server/
  task/UI/capture/input command was run. Named tests, compile, and parent re-review remain parent-owned gates.

`SOURCE+TEST SOURCES REDELIVERED / PARENT REVIEW PENDING`

<!-- TRUE_EOF: TURN-09R REPAIR #1 SOURCE+TEST SOURCES REDELIVERED -->

## PARENT RE-REVIEW #2 - 2026-07-15T22:45:02-04:00

- 结论：`通过 / P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。本结论由父级独立读取
  Repair #1 三个实际测试文件后作出，不以 Worker 自述代替。
- 双仓 `TurnCoreProtocolGoldenJsonTest.java` SHA-256 均为
  `A62A36C4F743F9700CECFC712876610A507B5FC00A55FF88B4552F04FCFF86CB`，文件同字节；closed enum、
  spec 顺序与 round-trip 已扩为十一项且以 `MOVE_MOUSE` 开头，后续 step index 为 `11..14`，输入子表为
  `subList(0, 11)`。原 P1 已关闭。
- DHXY `LocalTurnActionExecutorContractTest.java` SHA-256 为
  `BDB3980E2EA6497A960D71D9696B24730613000A5FAAFA524999DA4011ACA366`；新增真实
  `MOVE -> WAIT -> CLICK -> trailing WAIT -> CAPTURE` 回归，断言唯一 queue submission 只含
  `MOVE_MOUSE, SLEEP, CLICK_LEFT`，五个原 step 均按 index/type 完成，capture 在 queue 后执行。原 P2 已关闭。
- Repair 未改 production/protocol/POM/`.gitignore` 或其它测试；未发现 retry、第二 exchange、
  owner/session/ledger/TTL/durable workflow。TURN-19 与 TURN-21 的原子 move-settle-click 前置现已解除；
  TURN-23 仍需父级先冻结 pointer-over-ROI 的基线等价表达。
- Fresh named tests 与双仓适用 compile 仍等待所有 Java writers 稳定后由父级运行，因此本卡状态是
  `SOURCE+TEST SOURCE REVIEW PASSED / NAMED TEST+DUAL COMPILE PENDING`，不是 `CARD APPROVED/CLOSED`。

**无已批准业务差异；本卡恢复并证明计划已承诺的原子 move-settle-click 基线。**

<!-- TRUE_EOF: TURN-09R PARENT RE-REVIEW #2 SOURCE+TEST SOURCE REVIEW PASSED -->
