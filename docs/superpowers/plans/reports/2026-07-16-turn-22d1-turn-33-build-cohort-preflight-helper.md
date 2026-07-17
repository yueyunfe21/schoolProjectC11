# CR271 TURN-22D1 / TURN-33 stable-writer build cohort preflight

## 角色与快照

- 角色：CR271 Internal helper；不是 implementation owner、reviewer、父级或批准者。
- 只读快照时间：`2026-07-16T11:29:35.947-04:00`。
- 已完整读取本任务要求的 `AGENTS.md`、`docs/ACTIVE_WORK.md` 顶部最新 CR271 记录、权威计划第 19 节、
  TURN-22D1 与 TURN-33 原卡最新 true EOF，以及两份对应 named-test 源码；并按 `AGENTS.md` 读取
  `docs/DHXY_CONTEXT.md`。
- 本报告只列候选 build-cohort 门。未运行下列 Maven/JUnit/compile/package 命令，未启动 runtime、application、
  server、Task、UI、capture、OCR 或 input，未执行 Git mutation，也未改 Java、POM、原卡、计划或 ACTIVE_WORK。

## 两仓 status

| 仓库 | branch / HEAD | Maven 模块 | status 快照 |
|---|---|---|---|
| `D:\mavenProject\DHXY` | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 单模块 `com.myrobot:DHXY2Robot:1.0-SNAPSHOT`；无 `<modules>` | dirty；`--untracked-files=all` 共 732 项：43 modified、1 deleted、688 untracked |
| `D:\mavenProject\dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 单模块 `com.yueyunfe.dhxy:dhxy-cloud-brain:0.1.0-SNAPSHOT`；无 `<modules>` | dirty；`--untracked-files=all` 共 550 项：9 modified、541 untracked |

dirty/untracked 是迁移现场，不等于 active writer，也不能作为 stable-writer 的充分或否定条件；writer 真值必须由
卡片物理 true EOF、真实 source/test 增量、canonical delivery/return 和父级 owner 释放共同判定。

## 两卡冻结状态

### TURN-22D1

- 原卡最新 true EOF：`PARENT INDEPENDENT-REVIEW-GATE 2/2-APPROVED P0P1P2=0/0/0 BUILD-PENDING`
  （`2026-07-16T10:00:30-04:00`）；不是 `CARD APPROVED`。
- DHXY production `TurnInputStepExecutor.java` SHA-256：
  `a64422b061021dcbcec51837ac9f68a59bf21d57984469703cd1a68c2963134e`。
- DHXY named test `TurnInputStepExecutorContractTest.java`：695 行、12 个 `@Test`，SHA-256：
  `f5a7992fc6566f00b56f4e7e21c8e66fcf328f519523e73d6858ae93042e7a81`。
- 当前两份 SHA 与原卡 Parent Review #2 / 双 reviewer 采用值一致。

### TURN-33

- 原卡最新 true EOF：`PARENT ACCEPTED R1-R2 DUAL-REVIEW APPROVED 2-OF-2 P0P1P2=0/0/0 BUILD-PENDING`
  （`2026-07-16T06:29:00-04:00`）；不是 `CARD APPROVED`。
- Cloud `SummonSkillService.java` SHA-256：
  `991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`。
- Cloud `CloudSummonSkillWholePassCapability.java` SHA-256：
  `3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d`。
- Cloud `CloudTaskExclusiveInteractionAuthority.java` SHA-256：
  `91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc`。
- Cloud named test `SummonSkillTurnContractTest.java`：1683 行、19 个 `@Test`，SHA-256：
  `6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998`。
- 当前四份 SHA 与原卡 Parent Review #5 / 双 reviewer 采用值一致。

## 精确 Maven 命令

以下命令均属于用户已授权的 `HTTPS_TURN_CONTRACT_TEST_FAMILY` named-test 门或其适用 compile 门；本 helper
**只列出，未执行**。

| 顺序 | 卡 / 工作目录 | 精确命令 | 预期模块与结果边界 |
|---:|---|---|---|
| 1 | TURN-22D1，`D:\mavenProject\DHXY` | `mvn -q -Dtest=TurnInputStepExecutorContractTest test` | DHXY 单模块；Surefire 目标是唯一 `com.bot.dhxy.cloud.turn.TurnInputStepExecutorContractTest`，当前预期发现 12 tests |
| 2 | TURN-22D1，`D:\mavenProject\DHXY` | `mvn -q -DskipTests compile` | DHXY Java compile gate；不是 Cloud 命令，也不替代上一步 named test |
| 3 | TURN-33，`D:\mavenProject\dhxy-cloud-brain` | `mvn -q -Dtest=SummonSkillTurnContractTest test` | Cloud 单模块；Surefire 目标是唯一 `com.yueyunfe.dhxy.cloudbrain.service.SummonSkillTurnContractTest`，当前预期发现 19 tests |
| 4 | TURN-33，`D:\mavenProject\dhxy-cloud-brain` | `mvn -q clean compile` | Cloud 非测试 source gate；不得添加 skip/enforcer 绕过参数 |

- 按权威计划第 19.1 节，named-test 命令必须保留完整命令、exit code、Tests run、Failures、Errors；`-Dtest`
  只筛 Surefire 执行类，不会跳过该模块全部 main compile 与 testCompile。
- TURN-22D1 不包含 Cloud `TeamReturnTurnContractTest`；它属于 TURN-22C1/父卡聚合门，不得拿来替代 D1 的
  DHXY named test。
- TURN-33 的 production/test 全在 Cloud 模块，没有适用的 DHXY compile；Cloud 也不会自动从 sibling DHXY
  workspace 取 classpath。
- `mvn -q clean package` 会运行 Cloud 全测试，权威计划要求另行明确授权；它不在本次 named-test/compile
  preflight 命令集内。

## 当前 writer 判定

### 真值规则

1. 只有固定卡物理 true EOF 的当前 `CLAIMED/REPAIR CLAIMED`，并伴随真实 source/test 增量且尚无 canonical
   delivery/return，才是 active Java writer；旧 heartbeat、旧 claim、lane Markdown 或单纯 dirty status 不能复活 owner。
2. `SOURCE+TEST DELIVERED` 后 Worker 必须停写，但父级若已退新 Repair，源码仍未进入可构建冻结态；
   `OWNER RETURNED` 也必须由父级接受。READY/ASSIGNED 未 claim 本身不是 writer。
3. build cohort 还要求父级显式暂停新 claim，并在命令开始前复读所有当前卡 true EOF、复算目标 source/test/POM
   SHA。命令期间两仓 Java、test 与 POM 必须保持只读；瞬时零 owner 不等于 stable-writer window。

### `11:29:35.947-04:00` 现场

- A：TURN-28Q Repair #3，最新 true EOF 为 `FRESH-RESTART / CLAIM-REQUIRED`，当前零 owner。
- B：TURN-28S2，最新 true EOF 为 `FRESH-EXTERNAL-B-NEXT / ZERO-OWNER / CLAIM-REQUIRED`。
- C：TURN-34BP1 最新 true EOF 为 Parent Review #2 `P0/P1/P2=0/1/2 / REPAIR #2 REQUIRED /
  EXTERNAL-C-NEXT`；Repair #2 尚无新 claim，但 C 可在下一 heartbeat 立即领取。
- D：TURN-34AT1 Repair #3，最新 true EOF 为 `FRESH-RESTART / TEST-ONLY / CLAIM-REQUIRED`，当前零 owner。
- 权威计划最新状态同样是 A/B/D fresh restart、C Repair #2 next；尚无父级“暂停新 claim、打开 build
  cohort”结论。

因此当前结论是：**目标两卡自身 reviewed SHA 稳定，但全局 stable-writer build window 尚未被父级打开；不得运行
上述 Maven 命令。** C 的下一轮 Repair 以及 A/B/D 可重新 claim 的队列，使当前零 owner 只是一段瞬时空档。

## 禁止 runtime / input 边界

- TURN-22D1 named test 只能使用当前 in-memory recording queue、public resolver 和 real-queue pre-enqueue drift
  harness；不得启动 `InputActionWorker`、真实 provider、窗口 focus 或物理鼠标/键盘。
- TURN-33 named test 只能使用 synthetic PNG、packaged test assets、scripted command port、fake local Service/
  cleaner/context；不得启动 Cloud server、gateway、网络 turn、OCR sidecar、真实截图或输入。
- 禁止 application/server/Task/UI/runtime、Win32 capture、OCR runtime、鼠标、键盘、窗口 focus；禁止并发 Maven
  与任何 Java writer；禁止 IDE/stale `target` 代替 fresh 命令证据。
- 任一步在 main compile 或 testCompile 失败时，只能记录真实 first error 与“selected test 未运行”；不得删无关
  source/test、改 POM include/exclude、加 retry/skip flag 或继续用旧产物制造通过。
- 本 helper 不执行命令、不写 blocker/approval、不改卡片状态；最终运行、错误归属和 `CARD APPROVED` 只能由父级处理。

TRUE_EOF PRECHECK_COMPLETE
