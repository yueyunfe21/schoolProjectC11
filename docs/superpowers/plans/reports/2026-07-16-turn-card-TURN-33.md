# TURN-33 - SummonSkillService HTTPS turn whole-pass cutover

## READY / PARENT FROZEN IMPLEMENTATION BRIEF - 2026-07-16 02:13 EDT

- 状态：`READY / PARENT BRIEF FROZEN`；类型：`COUNT`；唯一
  `countUnit=TaskMaintenanceService::maybeCleanSummonSkill -> SummonSkillService::cleanSummonSkillsOnce`，
  `countDelta=+1`。父级是唯一 manager/final reviewer，Worker 不是 reviewer。
- startDependsOn：TURN-15、TURN-18、TURN-26 的 parent source gates 均已通过；approvalDependsOn：本卡 parent
  source/test-source review、唯一 `SummonSkillTurnContractTest`、依赖卡待执行 named tests 与适用 Cloud compile/build。
- 业务基线是组合合同：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 whole-pass、删除/确认、终极角、
  dialog cleanup、最多次数、40 秒边界、结果和 maintenance cooldown 顺序，加用户在
  `docs/业务逻辑.md:170-211` 明确批准的 live `if8` 与静态技能格倒扫规则。旧 hover 6/8 与第 4/7 起点不得恢复。

### Exact write set

- Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`。
- Modify
  `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`。
- Create
  `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`。
- Append only this fixed report true EOF。

其余两仓全部只读；尤其 `TaskMaintenanceService`、三个 Task caller、`SummonSkillStaticSlotPolicy`、tail scanner、
protocol、turn client/action factory/command port、DHXY、POM、Spring configuration、resources、旧 DTO/enum 和其它
测试/报告不得修改。不得删除计划外共享 legacy 类型导致其它 dirty source 失编，不得新增 wrapper chain、第二 capture/
template authority、自动 retry、owner/session/ledger/TTL/durable workflow。保护全部 dirty/untracked，不回滚、覆盖、
清理、提交、暂存或执行其它 Git mutation。

### Frozen production contract

1. **真实 public caller 不变。** 保持 `TaskMaintenanceService::maybeCleanSummonSkill` 调用
   `SummonSkillService::cleanSummonSkillsOnce(request)` 的 public shape、返回投影与三个真实 Task caller 的 maintenance
   顺序；TaskMaintenance/callers 全部只读。不得新建 facade 或把业务放回 DHXY。
2. **一次 Cloud whole pass，多枚 closed action。** 一次同步 Cloud 业务调用可在最多 40 秒内基于每次新 observation
   连续决策多个 ordered HTTPS JSON action。每个 action 单独 exact-window、单 UUID、单 command，并在 DHXY 全局
   input queue 内形成局部原子片段；观察后产生下一枚 UUID 是基线业务下一步，不是 transport retry。同一 action 零重发，
   不恢复旧 whole-pass remote command、pass-wide local exclusive session、ACQUIRE/RELEASE/ABORT、owner 或 ledger。
3. **每 action 创建 UUID 前重验。** 每次 observation/input action 都从当前 `TaskExecutionContext` 绑定 exact
   `TurnGameClient`，在 UUID/action 前读取 latest metadata，并核 deviceId/windowId、immutable native HWND/process、
   windowRect 与 STOP。错 context、重绑、missing metadata、无效 ROI、已 shutdown/stop 直接 fail closed且
   command/UUID=`0`；所有坐标为 latest window origin + 未缩放相对像素。
4. **布局只用 live if8。** 打开技能面板后先对窗口相对 ROI `(505,508)-(532,555)` 的 raw PNG 在 Cloud 匹配
   `images/template/zhaohuanshou/if8.png`。健康命中=8 格；只有 capture/template/matcher/ROI 全健康的正常 miss 才=6 格；
   任一机制失败=UNKNOWN，不能刷新 cooldown、不能继续删除，也不得回到旧 extra-slot hover。
5. **静态格与尾部倒扫。** 只读复用 `SummonSkillStaticSlotPolicy`、既有固定 6/8 格坐标和三张 packaged status
   template。sealed/unobtained=`LOCKED_SLOT`，inactive=`EMPTY_SLOT`，健康三模板全 miss=`OCCUPIED`，机制失败=
   `UNKNOWN`。从布局尾部倒扫：跳过 LOCKED；尾部连续 EMPTY 返回最前一个 EMPTY；否则首个 OCCUPIED 是最后有效格。
   固定 `O,O,O,E,E,E,L,L -> index 3`（零基）。UNKNOWN 必须终止本轮，不能绕过。
6. **仅待删格 hover，删除顺序逐值保持。** 只有倒扫选出的 OCCUPIED 格才按 696 的普通/高级/终极 tooltip 与模板
   顺序 hover 分类；KEEP 不删，普通技能按既有删除点/确认点/post-delete 顺序，终极技能按既有 corner 规则处理。
   固定坐标、等待、最多 5 次删除、最多 3 次 dialog、轻量 cleanup、关闭窗口和结果映射不得漂移或新增验证。
7. **Terminal/correlation。** 每个 completed action 严格核 action/window/step/frame/ROI/content type/PNG SHA/
   dimensions 与 result shape；known FAILED 按当前业务分支，confirmed STOP 传播，`DUPLICATE_OR_UNCERTAIN`、timeout
   uncertain、错 metadata/correlation 全部 fail closed且零自动 retry。只有完整成功 whole pass 才按 696 刷新成功
   cooldown；mechanics/UNKNOWN/uncertain 不得伪 success。
8. **Scoped legacy removal。** production reachable Summon path 对旧
   `CloudSummonSkillWholePassCapability` executable command、Summon 专属
   `CloudTaskExclusiveInteractionAuthority::executeSummonSkillWholePass`、retained invocation 与
   ACQUIRE/RELEASE/ABORT 为零。若计划外共享编译引用要求保留 capability public shape，只能成为零 command/UUID、
   fail-closed compatibility tombstone；authority 只移除 Summon 专属可执行分支。全仓 legacy enum/DTO 删除归后续删除卡，
   本卡不得扩大写集。

### Named-test acceptance

唯一 `SummonSkillTurnContractTest` 必须直接实例化 production `SummonSkillService`、production bound
`TurnGameClient` path 与只读 static policy/assets，不能只测复制 mapper。至少覆盖：

- 三个真实 caller 使用的 `cleanSummonSkillsOnce(request)` public shape，TaskMaintenance/caller source/API 零改；
- 非零和负 window origin、exact HWND/process、wrong current context、metadata missing/mismatch/STOP 的 UUID-before-preflight
  门，断言错误路径 command/UUID=`0`；
- `if8` hit=8、健康 miss=6、template/image/matcher/ROI failure=UNKNOWN，固定 6/8 坐标和
  sealed/unobtained/inactive/occupied 分类；
- 尾部 LOCKED、连续 EMPTY、尾部 OCCUPIED、全 LOCKED、任一 UNKNOWN，尤其
  `O,O,O,E,E,E,L,L -> 3`；断言旧 cached 第 4/7 起点与 extra-slot hover 不能进入 active path；
- 普通/高级/终极 hover 分类、KEEP 不删、普通删除/确认/post-delete、locked boundary、ultimate corner、最多 5 次删除、
  最多 3 次 dialog、40 秒边界和 lightweight cleanup 的 exact 顺序；
- 每个 fixture 的 exact action/UUID 数；只有业务观察后下一步产生新 UUID，同一 action 零重发，transport uncertainty
  零 retry；
- `COMPLETED/FAILED/STOPPED/DUPLICATE_OR_UNCERTAIN`、wrong action/window/step/frame/ROI/SHA/dimension/decode 的
  精确映射，失败不刷新成功 cooldown；
- Summon reachable source gate 对旧 whole-pass/exclusive bridge/scoped retained authority 零引用；compatibility tombstone
  零 command/UUID，且四个永久本地 Service 清单未扩张。

Worker 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；父级待所有 Java writers 稳定后只运行
用户授权 `HTTPS_TURN_CONTRACT_TEST_FAMILY` 的
`mvn -q -Dtest=SummonSkillTurnContractTest test` 与适用 Cloud compile/build。

**无已批准业务差异；按 `696a12b0` 与用户确认的静态格规则、最小 HTTPS JSON turn 等价迁移。**

<!-- TRUE_EOF: TURN-33 parent-frozen-brief -->

## CLAIMED - 2026-07-16T02:16:25.830-04:00

- agent id：`019f6990-dfbb-7373-8580-4944ce8f5c60`；nickname：`Goodall`；角色：TURN-33 implementation worker，非 reviewer；父级是唯一 manager/final reviewer。
- 领取前已读取本固定卡并确认其 parent-frozen brief；继续完整读取其余指定基线后才写 production/test。

### Exact write set

1. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java`。
2. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`。
3. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`。
4. Create `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`。
5. Append only `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-33.md` true EOF。

### 禁令确认

- 两仓其余路径全部只读；尤其 `TaskMaintenanceService`、三个 Task caller、`SummonSkillStaticSlotPolicy`、tail scanner、协议、turn client/action factory/command port、config、POM、Spring configuration、resources、DHXY、旧 DTO/enum、其它测试与报告不得修改。
- 适配当前 dirty source；保护全部 dirty/untracked，不回滚、覆盖、清理、删除、提交、暂存或执行任何 Git mutation，不改写 TURN-22/helper 等并行 owner 内容。
- 不删除计划外共享 legacy 类型，不让其它 dirty source 因本卡失编；不新增 wrapper chain、第二 capture/template authority、自动 transport retry、owner/session/ledger/TTL/durable workflow。
- 不恢复旧 whole-pass remote command、pass-wide local exclusive session、Summon 专属旧 executable bridge、retained invocation 或 `ACQUIRE/RELEASE/ABORT`；如共享编译引用迫使保留 capability public shape，只允许零 command/UUID、fail-closed compatibility tombstone。
- 不改变 parent-frozen 业务合同：保持 `696a12b0` whole-pass/delete/ultimate/dialog/cleanup/最多次数/40 秒/result/cooldown 顺序，加用户确认 live `if8` 与静态倒扫；不恢复旧 hover 6/8 或第 4/7 起点，不新增验证、业务重试、fallback 或 cooldown 成功条件。
- 不运行 Maven/JUnit/compile，不启动 runtime/application/server/Task/UI/capture/input，不执行任何真实 command；仅做源码与测试源码交付及静态/命令证据采集。
- 不写 `APPROVED` 或 `CLOSED`，不冒充 reviewer 或最终验收人；交付后只追加 `SOURCE+TEST DELIVERED`、证据、未运行门、SHA-256 与新的 true EOF，交由父级评审。

<!-- TRUE_EOF: TURN-33 CLAIMED Goodall 019f6990-dfbb-7373-8580-4944ce8f5c60 -->

## REPLACEMENT CLAIMED - 2026-07-16T03:29:47.348-04:00

- replacement agent id：`e46b29ed-516d-4860-994b-c17b2e143db8`；nickname：`Lovelace`；角色：TURN-33 replacement implementation Worker，非 reviewer；父级仍是唯一 manager/final reviewer。
- 原 implementation Worker Goodall `019f6990-dfbb-7373-8580-4944ce8f5c60` 已由父级确认 `not_found`；本 replacement 不回滚、不重抄旧版本，直接适配并保护 Goodall 已落盘的三份 production 与 `SummonSkillTurnContractTest` 半成品。

### 沿用原 exact write set

1. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/SummonSkillService.java`。
2. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudSummonSkillWholePassCapability.java`。
3. Modify `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/remote/CloudTaskExclusiveInteractionAuthority.java`。
4. Create/continue `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`。
5. Append only `D:/mavenProject/DHXY/docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-33.md` true EOF。

### Replacement 禁令确认

- 两仓其余路径全部只读；尤其 `TaskMaintenanceService`、三个真实 Task caller、`SummonSkillStaticSlotPolicy`、tail scanner、protocol、turn client/action factory/command port、DHXY、config、POM、Spring configuration、resources、旧 DTO/enum、其它测试与报告不得修改。
- 保护上述四个现有写集文件的全部半成品以及两仓全部 dirty/untracked；不回滚、覆盖、清理、删除、提交、暂存或执行任何 Git mutation，不干扰 TURN-28P replacement 与其它并行 helper 的互斥写集。
- 不新增 wrapper chain、第二 capture/template authority、自动 retry、session、owner、ledger、TTL 或 durable workflow；不恢复旧 whole-pass remote command、pass-wide local exclusive session、Summon 专属 executable bridge、retained invocation 或 `ACQUIRE/RELEASE/ABORT`。
- 保持真实 public caller、一次 whole pass 内多枚 closed action、每 action UUID 前 latest exact metadata preflight、live `if8` 6/8、静态格尾部倒扫，以及 `696a12b0` delete/ultimate/dialog/cleanup/40s/result/cooldown 顺序与 scoped legacy zero gate；无父级明确批准不得产生业务差异。
- 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行真实 command；交付只追加 `SOURCE+TEST DELIVERED`、逐文件 SHA/行证据/基线/未运行门和新的 true EOF，不写 `APPROVED` 或 `CLOSED`。

<!-- TRUE_EOF: TURN-33 REPLACEMENT CLAIMED Lovelace e46b29ed-516d-4860-994b-c17b2e143db8 -->

## CLAIM IDENTITY CORRECTION - 2026-07-16T03:31:19.753-04:00

- 平台本次 spawn 返回的真实 replacement implementation Worker 身份为 agent id `019f69ce-d84c-7a11-a832-3ce77f8f739a`、nickname `Faraday`；后续 TURN-33 实施、证据与交付均以此身份为准。
- 上一条 `e46b29ed-516d-4860-994b-c17b2e143db8 / Lovelace` 是 replacement claim 时误生成的本地标识，不是平台会话身份；保留原记录、不改写历史。
- 原 Goodall `019f6990-dfbb-7373-8580-4944ce8f5c60` 仍为 `not_found`；原 exact write set、四文件半成品保护、并行写集互斥与全部禁令不变。

<!-- TRUE_EOF: TURN-33 CLAIM IDENTITY CORRECTED Faraday 019f69ce-d84c-7a11-a832-3ce77f8f739a -->

## REPLACEMENT CLAIMED - 2026-07-16T04:02:52.825-04:00

- replacement agent id：`019f69f0-9358-7aa1-b9c2-1dc829d9fe44`；nickname：`Leibniz`；角色：TURN-33 replacement implementation Worker，非 reviewer；父级仍是唯一 manager/final reviewer。
- 前一 replacement Worker Faraday `019f69ce-d84c-7a11-a832-3ce77f8f739a` 已由父级实时确认 `not_found`；保留 Goodall/Faraday 全部历史与已落盘半成品，本 replacement 仅在原 exact write set 内增量接续，不回滚、不覆盖式重建。
- 沿用原卡冻结的四文件 production/test 写集与本报告 append-only 写集；其余两仓全部只读。继续遵守 `696a12b0` whole-pass/delete/ultimate/dialog/cleanup/40s/result/cooldown 顺序、用户确认 live `if8` 6/8 与静态格尾部倒扫、每 action fresh metadata/UUID/closed HTTPS JSON turn、terminal/uncertain fail closed，以及零自动 retry/session/owner/ledger/TTL/durable workflow。
- 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行真实 command；最终仅追加 `SOURCE+TEST DELIVERED`、逐文件证据/SHA/未运行门和新的 true EOF，不写 `APPROVED` 或 `CLOSED`。

<!-- TRUE_EOF: TURN-33 REPLACEMENT CLAIMED Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 -->

## SOURCE+TEST DELIVERED - 2026-07-16T04:17:34.643-04:00

- 实施 Worker：Leibniz `019f69f0-9358-7aa1-b9c2-1dc829d9fe44`，非 reviewer；本节只表示源码与测试源码交付，不表示父级批准、卡片关闭、测试或构建通过。
- replacement 保护并逐文件复核 Goodall/Faraday 已落盘半成品；未回滚、覆盖式重建、清理、删除、提交、暂存或修改 exact write set 外文件。
- 业务核对：已对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 whole-pass、删除/确认、locked-boundary、终极角、最多次数、40 秒、result/cleanup/cooldown 顺序，以及 `docs/业务逻辑.md:170-211` 的 live `if8`、静态格分类和尾部倒扫。无已批准业务差异；按基线等价迁移。

### Production 证据

1. `SummonSkillService.java:207-228` 保持真实 `cleanSummonSkillsOnce(request)` public caller/result/lightweight-cleanup 边界；`:434-477` 使用 live `if8` ROI 和一次静态格 raw-PNG observation；`:543-606` 保持选择、删除、确认和 post-delete 顺序。
2. `SummonSkillService.java:609-752` 保持 locked-boundary 与终极角流程。本 replacement 删除了半成品中三处未获批准的提前 `MAX_DELETE` 短路：locked-boundary deleter 不再因本地计数伪失败，boundary 删除后不再跳过应有终极角，终极角生成普通技能并删除后不再跳过 post-delete 状态确认。主普通删除分支原有 `MAX_DELETE_SKILL_COUNT_PER_RUN=5` 上限保持不变。
3. `SummonSkillService.java:765-904` 每枚 action 在 UUID 前重新核 current context、STOP、latest exact metadata、device/window/HWND/process/windowRect，并把每次 observation/input 作为一枚 fresh closed HTTPS JSON turn；`:944-981` 严格核 raw PNG、ROI、source step、content type、SHA 与尺寸；`:1174-1186` 把 baseline click delay 与同 queue hold 放在同一 input step。
4. `CloudSummonSkillWholePassCapability.java:9-35` 仅保留 public-shape compatibility tombstone，`execute` 恒定 fail closed，构造器不保留 authority/projection/retained state，零 command/UUID。
5. 对 `CloudTaskExclusiveInteractionAuthority.java` 的全文件静态检索中，`SummonSkill`、`SummonSkillWholePass`、`executeSummonSkillWholePass` 均为零命中；其它通用 exclusive authority 保持原样，未扩大删除范围。

### Named-test 源码证据

- 唯一 `SummonSkillTurnContractTest` 直接实例化 production `SummonSkillService`、production bound `TurnGameClient`、只读 static policy 与 packaged assets。
- 测试入口位于 `:129,181,207,254,292,322,363,397,428,468,500,518,545,565,660`，覆盖负/非零窗口原点、6/8 live 布局、静态倒扫、普通/高级/终极 tooltip、删除/确认、locked boundary、终极角生成 keep 与 normal、生成 normal 后 post-delete 验证、最多三次 dialog、40 秒、preflight 零 UUID/command、FAILED/STOPPED/uncertain、action/window/step/frame/ROI/SHA/dimension/decode、cleanup/cooldown source gate 和 legacy tombstone。
- 本 replacement 新增 `ultimateCornerGeneratedNormalIsDeletedAndVerifiedBeforeSuccess`（`:322-360`），直接证明终极角生成普通技能后必须完成删除、确认和再次 hover/capture 验证，不能在计数递增后提前伪成功。
- 现有 Cloud `.gitignore:15` 全局忽略 `src/test/`；named-test 文件仍真实保留在工作区并可由父级点名 Maven 读取。本 Worker 遵守写集与 Git 禁令，未修改 `.gitignore`、未强制暂存。

### SHA-256

- `SummonSkillService.java`：`2be2477b7e496bb5bdd7abd8f92b7aeb7667724aa729dd063bb11a28b776e3cd`（1334 行）。
- `CloudSummonSkillWholePassCapability.java`：`3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d`（123 行）。
- `CloudTaskExclusiveInteractionAuthority.java`：`91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc`（1198 行）。
- `SummonSkillTurnContractTest.java`：`4a86277dad27914bddefb49c724dac4f344db245d4dab2287762c64ce13f6667`（1389 行）。

### 未运行门

- 按本卡 Worker 禁令，未运行 Maven、JUnit、compile/package，也未启动 runtime/application/server/Task/UI/capture/input 或执行任何真实 command。
- 父级需在 Java writers 稳定后独立审查上述源码/测试源码，并运行用户授权的 `mvn -q -Dtest=SummonSkillTurnContractTest test` 与适用 Cloud compile/build；这些门尚未通过，不能写 `APPROVED` 或 `CLOSED`。

<!-- TRUE_EOF: TURN-33 SOURCE+TEST DELIVERED Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 -->

## PARENT SOURCE+TEST SOURCE REVIEW #1 - 2026-07-16T04:27:00-04:00

- 父级已独立逐文件核对本次四文件交付、固定写集、SHA、`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`
  与 `docs/业务逻辑.md:170-211`；Worker 自述不作为批准。交付 SHA 与报告一致，未发现越写集、自动
  transport retry、session/owner/ledger/TTL/durable workflow 或旧 whole-pass executable bridge 回流。
- 审查结论：`P0/P1/P2=0/2/0`，`REPAIR REQUIRED`；本卡不是 SOURCE APPROVED，不能解锁
  `TURN-34A`，也不能进入 named-test/build gate。

### P1-1 - terminal/uncertain/STOP 会绕过基线 lightweight cleanup

- 证据：Cloud `SummonSkillService.java:217-230` 只捕获 `PassDeadlineExceeded`，而
  `cloudUiCleanerPort.cleanLightweightInterruptions(...)` 位于 `try` 之后。action correlation/uncertain 抛出的
  `TaskFatalException` 与 confirmed STOP 抛出的 `TaskStopRequestedException` 会直接越过 cleanup。
- 基线：`696a12b0` 的 `SummonSkillService.java:174-185` 在 exclusive callback 无论正常返回还是由 input worker
  转成 `completed=false` 后，都会执行一次 `uiCleanerService.cleanLightweightInterruptions(...)`。冻结合同也明确要求
  terminal propagation 与 lightweight cleanup 同时保持，不能以传播异常为由丢失后置清理。
- 测试缺口：`SummonSkillTurnContractTest.java:468-515` 对 known FAILED 断言 cleaner=`1`，但所有 fatal/uncertain
  与 STOP fixture 均没有 cleaner 断言，因此当前回归未被命名测试发现。
- 影响：远端结果 uncertain、correlation 损坏或 confirmed STOP 后可能遗留召唤兽面板/轻量中断 UI，与
  `696a12b0` 后置清理顺序不等价；下一次 maintenance 可能从脏 UI 起步。
- Repair #1 条件：仅在本卡原 exact write set 内，使已取得 exact task context 后的所有 whole-pass 退出路径都
  **恰好一次**执行 lightweight cleanup，同时保持 `TaskFatalException` / `TaskStopRequestedException` 原样传播、零自动
  retry、零伪 success；给 known failure、fatal/uncertain、confirmed STOP 增加 production-path cleaner=`1` 断言。

### P1-2 - “最多 5 次删除”验收只读常量，未执行 production 行为

- 证据：`SummonSkillTurnContractTest.java:247-250` 仅通过 reflection 读取
  `MAX_DELETE_SKILL_COUNT_PER_RUN == 5`。它没有构造连续删除场景，也没有断言第 5 次后停止、第 6 次不发 command。
- Production 风险证据：`SummonSkillService.java:264-367` 当前 normal 主路径只处理单个 `selectedIndex`；
  `progress.deletedCount` 在该分支从 0 增至 1 后立即检查上限，当前测试与控制流均不能证明冻结合同中的 whole-pass
  最多 5 次删除仍可达且正确。`TaskMaintenanceService.java:755-766` 又会在本服务返回 success 后刷新长 cooldown，
  因此不能用“常量仍存在”替代行为等价证明。
- 基线/卡片：`696a12b0` `SummonSkillService.java:325-364` 是带删除预算的 tail loop；本卡 Named-test
  acceptance 明确要求直接实例化 production Service 覆盖“最多 5 次删除”，而不是 source/constant guard。
- 影响：当前源码可能在一次选中格处理后提前 success 并刷新 cooldown；即使 production 恰好因用户批准的静态尾扫
  只能触发一次删除，现有测试也没有证明该结论与“只改前置识别、普通删除语义不变”相容。
- Repair #1 条件：新增直接驱动 production `cleanSummonSkillsOnce(request)` 的可执行 fixture，证明完整 pass 的删除
  预算与 `696a12b0` 等价，并断言第 5 次后停止且无第 6 次 action/UUID。若用户批准的静态尾扫合同使该路径客观不可达，
  Worker 不得伪造 helper/DTO/测试专用入口；必须先在本报告写明与 `docs/业务逻辑.md:210-211` 的具体冲突和可选方案，
  等父级裁决后再改 production。

### Repair 边界

- 仅原四文件 production/test 写集与本 append-only 报告；不得改 `TaskMaintenanceService`、static policy、tail scanner、
  protocol、DHXY、POM/config/resources 或任何 Task/caller。
- 不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input；Maxwell 仍是活动 Java writer，构建门由
  writers 稳定后的父级执行。
- 原 Leibniz Worker 负责 Repair #1；交付只追加 `REPAIR #1 SOURCE+TEST DELIVERED` 与新 SHA/行证据/true EOF，
  不得写 `APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-33 PARENT REVIEW #1 P0=0 P1=2 P2=0 REPAIR_REQUIRED 2026-07-16T04:27:00-04:00 -->

## REPAIR #1 P1-2 CONTRACT CONFLICT / BLOCKED - 2026-07-16T04:34:01.965-04:00

- 实施 Worker：Leibniz `019f69f0-9358-7aa1-b9c2-1dc829d9fe44`，非 reviewer。已按 Parent Review #1
  直接追踪 production `cleanSummonSkillsOnce(request)`、只读 `SummonSkillStaticSlotPolicy`、只读
  `SummonSkillTailBoundaryScanner` 与 `696a12b0` tail loop；没有以反射常量、private helper、DTO 或测试专用入口
  代替可执行 production 行为。
- 客观不可达证据：当前 `SummonSkillService::runWholePass` 只消费静态倒扫返回的单个 `actionIndex`，该格完成
  KEEP/EMPTY/LOCKED/NORMAL 分支后均直接 `return`。NORMAL 主路径最多删除 1 次；若 post-delete 变成 LOCKED，
  只读 `SummonSkillTailBoundaryScanner` 在最近前置 NORMAL 处删除 1 次即返回；随后终极角若生成 NORMAL 最多再删除
  1 次。因此当前冻结控制流单 pass 理论上最多 3 次删除，无法通过 production public API 到达第 5 次，更不可能
  诚实断言“第 5 次停止且没有第 6 次 action/UUID”。
- 合同冲突：`696a12b0:SummonSkillService.java:325-364` 的五次预算位于可连续前进的 tail loop；但
  `docs/业务逻辑.md:197-211` 要求静态倒扫先选唯一最后有效格、只有真正待判断的 OCCUPIED 格才 hover，并明确
  不恢复旧第 4/7 起点。把旧 loop 原样接在单个静态 `actionIndex` 后仍遇不到五个 OCCUPIED；改为扫描更早格、
  重复静态扫描或恢复旧起点都会改变已冻结的观察/删除顺序，Worker 无权猜选。

### 待父级裁决方案

1. **保持当前单次静态倒扫合同。** 将“最多 5 次”验收改为当前可达上限及其组合路径，明确本次用户批准的静态
   尾扫替代了旧多格 tail-loop 的五次预算可达性；优点是零新增观察，代价是承认 `docs/业务逻辑.md:210-211`
   所称“普通技能删除语义不变”存在一项批准差异。
2. **显式批准每次成功删除后重新执行 fresh 静态尾扫，最多 5 次。** 每轮仍只 hover 当轮倒扫选中的 OCCUPIED，
   不恢复旧第 4/7 起点；可用 production fixture 证明第 5 次停止及第 6 次零 action/UUID。代价是新增最多四次
   static-slot observation，改变现有冻结的 observation 次数/顺序，必须由父级先修订卡片合同（必要时请用户批准）。

- Worker 推荐方案 2，因为它同时保留 live 静态尾扫和 `696a12b0` 五次删除预算，但在父级明确冻结前不修改
  P1-2 production/test。P1-2 状态：`BLOCKED / PARENT DECISION REQUIRED`。P1-1 的全退出 cleanup 是独立、无业务
  选择的返修项，Worker 将继续在原 exact write set 内关闭；整张 Repair #1 在 P1-2 裁决前不交付、不写
  `REPAIR #1 SOURCE+TEST DELIVERED`。

<!-- TRUE_EOF: TURN-33 REPAIR #1 P1-2 CONTRACT_CONFLICT BLOCKED PARENT_DECISION_REQUIRED Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 -->

## REPAIR #1 PARTIAL UPDATE - P1-1 IMPLEMENTED / P1-2 BLOCKED - 2026-07-16T04:37:36.421-04:00

- 实施 Worker：Leibniz `019f69f0-9358-7aa1-b9c2-1dc829d9fe44`，非 reviewer。本节仅记录 P1-1
  production/test-source 增量，等待父级复审；不表示 Repair #1 完整交付或批准。
- `SummonSkillService.java:207-244`：取得 exact `TaskExecutionContext` 后，whole pass、deadline 投影、结果日志与
  return 统一位于一个 `try/catch/finally` 边界。所有正常/known-failure/deadline/fatal/uncertain/confirmed STOP/
  correlation failure 退出均只经过一个 `UI_CLEAN_LIGHTWEIGHT` 调用点。主 `RuntimeException`/`Error` 原对象直接
  rethrow；若 cleanup 同时失败，只把 cleanup failure 加为 suppressed，不替换原 fatal/STOP，不产生 retry 或伪 success。
- `SummonSkillTurnContractTest.java:454-517,534-545`：直接经 production
  `cleanSummonSkillsOnce(request)` 补齐全部 UUID-before-preflight failure、known FAILED、五类 uncertain、confirmed
  STOP 与十类 action/window/step/frame/ROI/SHA/dimension/decode corruption 的 cleaner=`1` 断言；原 command/UUID
  次数和零 retry 断言保持。
- P1-2 未改 production/test；反射常量断言仍保持原样，等待上一 true EOF 的父级合同裁决。Worker 未伪造
  第 5 次路径、private helper、DTO 或测试专用入口。

### 当前 SHA-256

- `SummonSkillService.java`：`2031315abcd14124acbdc3a3593037648710f79a05b2e91ac2afc093b507e107`（1348 行）。
- `CloudSummonSkillWholePassCapability.java`：`3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d`（123 行，未改）。
- `CloudTaskExclusiveInteractionAuthority.java`：`91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc`（1198 行，未改）。
- `SummonSkillTurnContractTest.java`：`4ab1b4404458b96af5c2e42b26d02c001db1581ee3a7a679cce640b7ce483cc3`（1393 行）。

### 未运行门

- 遵守父级禁令与 Maxwell 活动 writer 门，未运行 Maven/JUnit/compile/package，也未启动 runtime/application/
  server/Task/UI/capture/input，未执行 Git mutation。
- P1-1 仍需父级独立源码/测试源码复审；P1-2 仍为 `BLOCKED / PARENT DECISION REQUIRED`。因此本轮不追加
  `REPAIR #1 SOURCE+TEST DELIVERED`，不写 `APPROVED/CLOSED`。

<!-- TRUE_EOF: TURN-33 REPAIR #1 PARTIAL P1-1_IMPLEMENTED P1-2_BLOCKED Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 -->

## PARENT CONTRACT DECISION FOR REPAIR #1 P1-2 - 2026-07-16T05:00:25-04:00

- 父级已独立复读 `696a12b0` 的 production tail loop、当前 `runWholePass`、只读
  `SummonSkillStaticSlotPolicy`/`SummonSkillTailBoundaryScanner`，并重新核对
  `docs/业务逻辑.md` 的静态格规则。Worker 的“当前单次静态倒扫最多只能删除约三次”事实成立，但不构成把
  原 `MAX_DELETE_SKILL_COUNT_PER_RUN=5` 降级为不可验收常量的理由。
- 权威合同同时要求两件事：以 live `if8` + 静态尾部倒扫替代旧第 4/7 起点和全格 hover；普通技能删除、最多
  次数、终极角、dialog/cleanup 顺序保持原业务语义。因此父级裁决采用 **每次成功删除后 fresh 静态尾扫，
  whole pass 共用最多 5 次删除预算**。这是已批准规则的等价组合，不是新增业务差异，无需恢复旧 hover
  起点或请求新的用户行为授权。
- Repair #1 精确条件：每轮 fresh static scan 只选择当轮唯一最后有效格；只有该轮选中的 `OCCUPIED` 才 hover。
  删除并完成 post-delete 状态处理后，只要 pass 仍可继续且预算未满，就用新的显式 observation action/UUID
  重新静态倒扫。第五次删除后立即停止，第六次删除 action/UUID 必须为 `0`。fresh scan 是 Cloud 明确的后续
  业务 observation，不是 transport retry；每个 action 继续在 UUID 前核 exact context/latest metadata/STOP。
- 保持同一个 40 秒总 deadline、最多三次 dialog、locked boundary、ultimate corner、所有退出 lightweight
  cleanup 恰好一次、terminal/uncertain 原样传播，以及零自动 retry/session/owner/ledger/TTL/durable workflow。
  不得新增测试专用入口、DTO、private helper 计数或 wrapper chain。
- Named test 必须直接驱动 production `cleanSummonSkillsOnce(request)`，构造五次真实可达连续删除，证明每轮
  fresh 静态倒扫、每轮只 hover 选中 OCCUPIED、第五次后停止、第六次零 command/UUID；反射常量只能作为补充，
  不能继续充当行为验收。
- 原 Leibniz Worker 已收到上述裁决并在原四文件 exact write set 内继续 Repair #1。当前状态仍为
  `REPAIR REQUIRED`；未出现新的 `REPAIR #1 SOURCE+TEST DELIVERED` 前，TURN-34A/C 依赖门不开放。

**无已批准业务差异；按 `696a12b0` 与用户确认的 live 静态倒扫规则等价迁移。**

<!-- TRUE_EOF: TURN-33 PARENT P1-2 CONTRACT DECIDED FRESH_STATIC_RESCAN MAX_DELETE_5 2026-07-16T05:00:25-04:00 -->

## PARENT DECISION ACK + REPAIR #1 SOURCE+TEST DELIVERED - 2026-07-16T05:16:34.389-04:00

- 实施 Worker：Leibniz `019f69f0-9358-7aa1-b9c2-1dc829d9fe44`，非 reviewer。已确认并执行父级
  `FRESH_STATIC_RESCAN + MAX_DELETE_5` 裁决：这是 `696a12b0` 普通技能删除/五次预算与用户确认 live 静态
  尾扫规则的等价组合，不是新增业务差异。
- 本次 Repair #1 仅增量修改原 exact write set 中的 `SummonSkillService.java` 与
  `SummonSkillTurnContractTest.java`；`CloudSummonSkillWholePassCapability.java`、
  `CloudTaskExclusiveInteractionAuthority.java` 保持原交付内容和 SHA，未修改其它 production/test、协议、DHXY、
  Task/caller/POM/config/resource。

### P1-1：所有 exact-context pass 退出 lightweight cleanup 恰好一次

- `SummonSkillService.java:207-244`：exact `TaskExecutionContext` 取得后，whole pass 的正常、known failure、
  deadline、fatal/uncertain、confirmed STOP 与 correlation failure 统一经过单一 `finally` cleanup 点；主
  `RuntimeException`/`Error` 原对象直接传播，cleanup 同时失败只作为 suppressed，不替换主失败、不 retry、不伪 success。
- `SummonSkillTurnContractTest.java:500-654`：production public API 路径保留并补齐 known failure、全部 uncertain、
  confirmed STOP、preflight 与 frame/correlation corruption 的 cleaner=`1`、command/UUID 次数和零 retry 断言；
  本次五删除 production fixture 另在 `:294` 断言 cleaner=`1`。

### P1-2：fresh 静态尾扫与五次真实删除预算

- `SummonSkillService.java:263-464`：同一 40 秒 deadline 和同一最多三次 dialog 计数内运行有界 whole-pass 循环。
  每次可继续的成功删除完成 post-delete/ultimate/locked-boundary 处理后，下一轮都由新的
  `scanStaticSlots(...)` HTTPS observation action/UUID 重新选择当下唯一静态尾格；OCCUPIED 只进入该轮
  `selectedIndex` 的 hover，不恢复旧第 4/7 起点或全格 hover。
- `SummonSkillService.java:706-755,842-844`：locked-boundary 删除和 ultimate-generated normal 删除共用同一个
  `progress.deletedCount`；主删除、boundary 删除或 generated 删除到第 5 次均立即成功停止，不再发第 6 次
  action/UUID。40 秒总 deadline、locked boundary、ultimate corner、terminal/uncertain 传播均保持原合同。
- `SummonSkillTurnContractTest.java:258-335`：named test 直接经 production
  `cleanSummonSkillsOnce(request)` 构造五轮可达连续删除。测试源码断言结果 deleted=`5`、fresh static scan=`5`、
  delete-prepare=`5`、总 action/UUID=`31/31`、最后 action 是第 5 次 confirm、脚本回复已消费完且 cleaner=`1`；
  同时逐个排除其它五个 OCCUPIED 格的 hover，保留每轮只操作静态尾扫所选格的证据。反射常量断言仅作补充，
  不再充当五次行为验收。
- 原单删和 ultimate-generated-normal fixture 已显式补入下一轮 fresh static 安全终态，避免以“删一次即返回”的旧
  假设通过；未新增 production 测试入口、helper、DTO、wrapper chain、自动 retry/session/owner/ledger/TTL/
  durable workflow。

### Repair #1 当前 SHA-256

- `SummonSkillService.java`：`57627c405e02d3fb703e3eec55349282e813b49b752eb6c79863aacef51a3651`（1440 行）。
- `CloudSummonSkillWholePassCapability.java`：`3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d`（123 行，未改）。
- `CloudTaskExclusiveInteractionAuthority.java`：`91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc`（1198 行，未改）。
- `SummonSkillTurnContractTest.java`：`e7557ce9b0006e6ce5d7bbfaccc7be74c37f036722225eded91a3d1acf259963`（1482 行）。

### 未运行门与交付状态

- 遵守父级禁令及 Maxwell 活动 Java writer 门，未运行 Maven/JUnit/compile/package，也未启动 runtime/application/
  server/Task/UI/capture/input；未执行 Git mutation。
- Repair #1 production/test source 已交付，等待父级独立逐文件复审及后续适用门。本 Worker 不写
  `APPROVED/CLOSED`，保持在线等待返修意见。

**无已批准业务差异；按 `696a12b0` 与用户确认的 live 静态倒扫规则等价迁移。**

<!-- TRUE_EOF: TURN-33 REPAIR #1 SOURCE+TEST DELIVERED PARENT_DECISION_ACK Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #2 - 2026-07-16T05:23:46-04:00

- 父级已独立逐行读取 Repair #1 production/test、复核 SHA，并对照 `696a12b0` 的 tail loop 与
  `docs/业务逻辑.md`“终极角检查保持原业务语义”。Worker 自述未作为裁决依据。
- 结论：`P0/P1/P2=0/1/0 / REPAIR #2 REQUIRED`。Review #1 的 P1-1（所有 exact-context 退出
  lightweight cleanup 恰好一次）已经关闭；fresh static rescan 与 production 五次预算也已落到 public API 路径。
  但新循环把终极角生成分支错误地变成可重复分支，尚不能 source approve。

### P1-1 - fresh loop 会在同一 pass 重复点击终极角，改变 `696a12b0` 业务语义

- 精确 production 证据：`SummonSkillService.java:299-315,370-386,425-439` 在
  `maybeClickUltimateCorner(...)` 使 `deletedCount` 增长、且结果不是 KEEP 时统一 `continue` fresh loop；
  `SummonSkillService.java:746-858` 在终极角命中后置 `progress.ultimateGenerateClicked=true`，若生成普通技能则
  删除并返回 success，但入口没有“本 pass 已点击过终极角”的门。所以下一轮 static scan 再见 EMPTY 时会再次
  hover/click 终极角，最多随 5-delete budget 重复五次。
- 基线证据：`696a12b0:SummonSkillService.java:370-384,429-443,457-470` 的三个终极角调用点完成后都
  `break` 当前 pass；一次 pass 最多执行一次终极角检查/点击。`docs/业务逻辑.md` 只批准 static locked/empty/
  occupied 前置识别和最后有效格推导，明确终极角检查保持原业务语义。
- Test 证据：`SummonSkillTurnContractTest.java:258-335` 的五删 fixture 每轮先让 post-delete 与 ultimate-hover
  两帧确认同一尾格为 EMPTY，随后没有任何生成动作却把下一轮 static frame 又脚本化为六格全 OCCUPIED。
  它能穿透 production 计数门，但绕开了真实 EMPTY 尾格会再次进入终极角的路径，因此没有发现重复点击。
- 影响：一次 maintenance pass 可能反复触发终极角生成/删除，改变用户已确认的输入次数、业务顺序和风险；
  这不是 fresh static observation 所授权的普通技能继续删除。

### Repair #2 精确条件

- 仅改 `SummonSkillService.java`、`SummonSkillTurnContractTest.java` 与本报告；其余两 production 文件保持
  当前 SHA。不得改 Task/caller/static policy/tail scanner/protocol/DHXY/POM/config/resource。
- fresh static rescan 只用于 pass 仍可继续的普通技能/locked-boundary 删除；任何一次
  `maybeClickUltimateCorner(...)` 实际点击终极角后，无论生成 KEEP、生成 NORMAL 后删除，或生成后的稳定状态，
  都必须按 `696a12b0` 结束当前 pass，不能再发下一次 static scan、终极角 action 或 UUID。不得用 retry/TTL/
  session/owner/ledger/durable workflow 规避。
- Named test 直接驱动 production `cleanSummonSkillsOnce(request)` 增加真实链：普通尾技能删除 -> EMPTY ->
  终极角命中 -> 生成 NORMAL -> 删除 -> EMPTY；断言终极角 click 恰好一次、生成删除恰好一次、随后 static scan/
  command/UUID 均为零、cleanup 恰好一次、result deleted count 精确。现有五次 budget test 继续证明第五次后第六次
  action/UUID 为零，但不得再把其权威 live frame 脚本描述成“终极角可重复”。
- 原 Leibniz Worker 继续 Repair #2；交付前不得运行 Maven/JUnit/compile/runtime/input，不得做 Git mutation，
  不得写 `APPROVED/CLOSED`。无已批准业务差异。

<!-- TRUE_EOF: TURN-33 PARENT REVIEW-2 REPAIR-2 REQUIRED P0=0 P1=1 P2=0 2026-07-16T05:23:46-04:00 -->

## REPAIR #2 CLAIMED - 2026-07-16T05:24:52.954-04:00

- 原 implementation Worker Leibniz `019f69f0-9358-7aa1-b9c2-1dc829d9fe44` 已读取父级
  `PARENT SOURCE+TEST-SOURCE REVIEW #2` 最新 true EOF，并领取 Repair #2；非 reviewer，不自批。
- 本轮只修改 `SummonSkillService.java`、`SummonSkillTurnContractTest.java` 与本 append-only 报告。
  返修目标：fresh rescan 仅延续普通/locked-boundary 删除；任何实际终极角 click 完成后立即结束当前 pass，
  并由 production public API 真实链证明后续 static scan/command/UUID=`0`。
- 遵守禁令：不运行 Maven/JUnit/compile/runtime/application/server/Task/UI/capture/input，不执行 Git mutation。

<!-- TRUE_EOF: TURN-33 REPAIR #2 CLAIMED Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 2026-07-16T05:24:52.954-04:00 -->

## REPAIR #2 SOURCE+TEST DELIVERED - 2026-07-16T05:29:23.505-04:00

- 实施 Worker：Leibniz `019f69f0-9358-7aa1-b9c2-1dc829d9fe44`，非 reviewer。本轮仅增量修改
  `SummonSkillService.java`、`SummonSkillTurnContractTest.java` 与本 append-only 报告；其余两份 TURN-33
  production 文件及全部并行 dirty/untracked 均保持不动。

### Production 返修证据

- `SummonSkillService.java:299-303,361-365`：静态 EMPTY 与 OCCUPIED 动态检查为 EMPTY 的前置终极角入口恢复为
  调用后直接结束 pass，不再保留 Repair #1 的 generated-delete fresh-rescan 死分支。
- `SummonSkillService.java:313,375,414,438`：三个 locked-boundary 返回点及普通删除后 EMPTY 的终极角返回点，
  均在任何 rescan/count 分支前先检查 `progress.ultimateGenerateClicked`。该字段只在终极角 click action mechanics
  成功后于 `:805` 置 true；因此实际点击后，无论生成 KEEP、生成 NORMAL 后删除并稳定为 EMPTY/KEEP，当前 pass
  都立即结束，不再发 static scan、终极角 action 或 UUID。终极角仅 hover/miss、且前面确有普通或
  locked-boundary 删除时仍可按裁决 fresh rescan。
- `SummonSkillService.java:829-832` 保持 generated NORMAL 删除计数与第 5 次立即停止门；40 秒总 deadline、最多三次
  dialog、locked boundary、cleanup 恰好一次、terminal/uncertain 传播及零自动 retry/session/owner/ledger/TTL/
  durable workflow 均未改变。

### Production public API 测试源码证据

- `SummonSkillTurnContractTest.java:258-329` 新增真实链 fixture，直接经 production
  `cleanSummonSkillsOnce(request)` 执行：普通尾技能删除 -> EMPTY -> 终极角命中并 click -> 生成 NORMAL ->
  generated delete -> EMPTY。测试源码断言 result deleted=`2`、ultimate click=`1`、两个 delete-prepare 分别位于
  普通与 generated 路径、总 action/UUID=`14/14`、static scan 总数仅为链首的 `1`、最后 action 是
  post-generated-delete EMPTY capture、后续脚本/command/UUID=`0`、cleanup=`1`。
- 原 selected-EMPTY generated-normal fixture 已移除 Repair #1 追加的第二次 static scan，并恢复 action=`10`、最终
  EMPTY 结果；`SummonSkillTurnContractTest.java:332` 起的五次普通删除预算测试完整保留，并新增
  `ultimateGenerateClicked=false` 断言，继续证明第 5 次后第 6 次 action/UUID=`0`，不再描述终极角可重复。
- 未新增 production 测试入口/helper/DTO/wrapper chain；无已批准业务差异，按 `696a12b0` 终极角单次 pass 终态
  与用户确认的 fresh 静态尾扫规则等价迁移。

### Repair #2 当前 SHA-256

- `SummonSkillService.java`：`d28e62a56c170bc26a6d16035670515e4fb8f55eebf5d8356515d1565f1c1a46`（1428 行）。
- `CloudSummonSkillWholePassCapability.java`：`3ee97295b2d50b052e56347e420eb04c35bea5472b327aec48e02fb015e20a6d`（123 行，未改）。
- `CloudTaskExclusiveInteractionAuthority.java`：`91349697592cd33cf32870e5b6732a21470480c2ce6ef16bca90a3444297abcc`（1198 行，未改）。
- `SummonSkillTurnContractTest.java`：`68312d38cc5d7724aaf0b86495fcf7810540fc70e64263fb1a874b9961b5b7dc`（1553 行）。

### 未运行门与交付状态

- 按父级禁令未运行 Maven/JUnit/compile/package，未启动 runtime/application/server/Task/UI/capture/input，未执行
  Git mutation。
- Repair #2 production/test source 已交付，等待父级独立复审；本 Worker 不写 `APPROVED/CLOSED`，保持在线。

<!-- TRUE_EOF: TURN-33 REPAIR #2 SOURCE+TEST DELIVERED Leibniz 019f69f0-9358-7aa1-b9c2-1dc829d9fe44 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #3 - 2026-07-16T05:32:43-04:00

- 父级未采用 Worker 自述，已独立逐行复核当前 `SummonSkillService.java`、
  `SummonSkillTurnContractTest.java`、Repair #2 SHA、`696a12b0` 三个终极角退出点与
  `docs/业务逻辑.md` 的 static-tail/终极角规则。结论为
  **`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING`**。
- `SummonSkillService.java:299-303,361-365` 的 selected EMPTY 路径在终极角调用后直接结束 pass；
  `:313-315,375-377,414-416,438-440` 的 locked-boundary/普通删除路径在任何 fresh rescan 前先检查
  `progress.ultimateGenerateClicked`。该字段仅在 `:789-805` 的终极角 click mechanics 成功后置 true，故
  hover/miss 仍可在已有普通删除后 fresh scan，而任何真实终极角 click 后均不会再发 static scan/action/UUID。
- `SummonSkillService.java:807-846` 保持 generated KEEP/NORMAL、generated delete、第五次删除预算与 typed
  failure 顺序；没有新增 retry/session/owner/ledger/TTL/durable workflow，也未改变 40 秒 deadline、三次 dialog、
  exact-context preflight、terminal/uncertain 或单一 lightweight cleanup。
- `SummonSkillTurnContractTest.java:258-329` 直接驱动 production public API，真实执行 ordinary delete -> EMPTY ->
  ultimate click -> generated NORMAL -> delete -> EMPTY，并断言 deleted=`2`、ultimate click=`1`、static scan=`1`、
  action/UUID=`14/14`、剩余 scripted replies=`0`、cleanup=`1`；`:332-411` 继续以 production 路径证明第五次
  删除后第六次 command/UUID 为零。当前 SHA 与交付报告一致：Service
  `d28e62a56c170bc26a6d16035670515e4fb8f55eebf5d8356515d1565f1c1a46`，test
  `68312d38cc5d7724aaf0b86495fcf7810540fc70e64263fb1a874b9961b5b7dc`。
- Leibniz implementation owner 释放；本卡下一门为两名非实现者独立 reviewer。Maxwell 仍在 TURN-28P
  Repair #1 写 Java，本轮不运行 Maven/JUnit/compile；source pass 不冒充 `CARD APPROVED/CLOSED`。

**无已批准业务差异；按 `696a12b0` 与用户确认的 live static-tail 规则等价迁移。**

<!-- TRUE_EOF: TURN-33 PARENT REVIEW-3 SOURCE+TEST SOURCE REVIEW PASSED P0P1P2=0/0/0 REVIEW+BUILD PENDING 2026-07-16T05:32:43-04:00 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #4 - 2026-07-16T05:58:26.295-04:00

- 独立 R2 新证据出现后，父级未沿用 Review #3，也未用 reviewer 自述代替裁决；已重新读取当前 production/test、
  `696a12b0` 与 `docs/业务逻辑.md` 的三技能静态尾扫/终极角规则。结论：
  **`P0/P1/P2=0/1/0 / REPAIR #3 REQUIRED`**。Review #3 的 source pass 被本轮最新 P1 覆盖。
- P1 精确证据：Cloud `SummonSkillService.java:823-846` 在终极角生成 NORMAL 后执行删除并增加
  `deletedCount`；当该删除恰为 whole-pass 第 5 次时，`:830-832` 直接返回 success，跳过 `:834-846` 的
  `post-generated-delete-slot-*` observation。`696a12b0:584-604` 在 generated delete 后无条件观察，并且只有
  稳定 `EMPTY_SLOT/KEEP_SKILL` 才闭合；当前分支会把仍为 NORMAL、UNKNOWN 或其它不稳定状态伪装为成功。
- Test gap：`SummonSkillTurnContractTest.java:258-329` 的 generated-normal 仅为第 2 次删除；`:332-411` 的
  第 5 次是普通删除且不点击终极角，因此没有覆盖“generated-normal 恰为第 5 次”的 production 分支。
- Repair #3 exact write set：仅 Cloud `src/main/java/com/bot/dhxy/service/SummonSkillService.java`、
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java` 与本 append-only 原卡。
  第五次 generated-normal 删除后必须仍恰好执行一次 post-delete stability observation；只接受稳定 EMPTY/KEEP，
  不稳定/UNKNOWN 不能 success；完成该观察后结束当前 pass，后续 static scan/action/UUID 均为 0，也不得出现第六次
  删除。不得改协议、DHXY、Task/caller、其它 Service、POM/config/resource，不新增 retry/session/owner/ledger/TTL/
  durable workflow。
- 按用户最新排班，阻塞实现不再退回 Internal。External C 的队首由 TURN-34A 改为本 Repair #3；原 R1 review
  已标记 superseded 并退出。External C 必须先在本卡 true EOF 追加 `REPAIR #3 CLAIMED`，再从当前字节增量返修；
  TURN-34A 作为 C 的下一张卡继续等待。本轮不运行 Maven/JUnit/compile/runtime/input，不执行 Git mutation。

**无已批准业务差异；按 `696a12b0` 与用户确认的 live static-tail/终极角规则等价迁移。**

<!-- TRUE_EOF: TURN-33 PARENT REVIEW-4 REPAIR-3 REQUIRED EXTERNAL-C READY P0=0 P1=1 P2=0 2026-07-16T05:58:26.295-04:00 -->

## EXTERNAL C REPAIR #3 CLAIMED - 2026-07-16T06:05:57.221-04:00

REPAIR #3 CLAIMED | card=TURN-33 | lane=CR271-External-Worker-C | role=implementation-worker(not-reviewer) | claimedAt=2026-07-16T06:05:57.221-04:00 | model=claude-opus-4-8 | platformAgentId=<待父级平台真实 ID 校正> | nickname=<待父级平台 nickname 校正>

- 承接 Parent Review #4 唯一 P1（`SummonSkillService.java:823-846`：generated-normal 删除恰为 whole-pass 第 5 次时，`:830-832` 直接 success，跳过 `:834-846` post-generated-delete observation，把 NORMAL/UNKNOWN 伪装成功，违反 `696a12b0:584-604` 无条件观察 + 仅稳定 EMPTY_SLOT/KEEP_SKILL 才闭合）。
- Exact write set（不越界）：Cloud `src/main/java/com/bot/dhxy/service/SummonSkillService.java`、`src/test/java/com/yueyunfe/dhxy/cloudbrain/service/SummonSkillTurnContractTest.java`、本 append-only 原卡。不改协议/DHXY/Task/caller/其它 Service/POM/config/resource；不新增 retry/session/owner/ledger/TTL/durable workflow。
- 从 Repair #2 当前字节增量返修（Service SHA `d28e62a5...`、test SHA `68312d38...`）；保护两仓全部 dirty/untracked，不执行任何 Git mutation，不运行 Maven/JUnit/compile/runtime/input。
- 交付口径：完成后本卡 true EOF 只写 `SOURCE+TEST DELIVERED`，不写 APPROVED/CLOSED。

## EXTERNAL C REPAIR #3 SOURCE+TEST DELIVERED - 2026-07-16T06:10:45.729-04:00

SOURCE+TEST DELIVERED | card=TURN-33 | repair=#3 | lane=CR271-External-Worker-C | deliveredAt=2026-07-16T06:10:45.729-04:00 | platformAgentId=<待父级平台真实 ID 校正>

### P1 修复（Review #4 唯一 P1）

- `SummonSkillService.java:829-832`（旧）在 generated-normal 删除后 `deletedCount++`，当该删除恰为 whole-pass 第 5 次时直接 `return OperationResult.success("summon skill delete limit reached")`，跳过 `:834-846` 的 `post-generated-delete-slot-*` observation，把仍为 NORMAL/UNKNOWN 的槽伪装成功。
- 修复：删除该预算早退，使流程无条件落入既有唯一 observation。现 `:829` `deletedCount++` 后直接 `inspectSkillSlot("post-generated-delete-slot-" + (index+1))`，仅 `EMPTY_SLOT` → success `"generated normal skill deleted"`、`KEEP_SKILL` → `nextStartIndex=index+1` + success `"generated skill changed to keep after delete check"`，其余（NORMAL/UNKNOWN/不稳定）→ failed `"generated normal delete did not leave a stable slot"`。
- 基线等价证据：`696a12b0:584-604` 的终极角 generated-normal 分支在 `deletedCount++` 后**无条件** `inspectSkillSlot`，分支内**不存在**任何预算早退；`deletedCount >= MAX_DELETE_SKILL_COUNT_PER_RUN` 在基线只出现于主循环（`696a12b0:359-362`，普通删除后停 pass）。
- 预算门分布复核（Cloud 现状）：主循环 `:268`、普通删除后 `:398`、boundary 后 `:721`、终极角入口 `:742` 全部保留（对应基线主循环停 pass 与"预算耗尽则不点终极角"）；终极角 generated 分支内已无早退。第 5 次普通删除仍按基线在 `:398` 立即停 pass、不做 post-delete 观察（未改）。
- 零第 6 次删除证据：`maybeClickUltimateCorner` 的返回被 caller 以 `return buildResult(progress, corner.success(), corner.message())` 直接终结本 pass；且 `progress.ultimateGenerateClicked` 在 `:805` click 成功后置 true，`:313/:375/:414/:438` 据此不再 fresh scan。故该 observation 恒为本 pass 最后一个 action。
- 未新增 retry/session/owner/ledger/TTL/durable workflow；未改 40s deadline、三次 dialog、locked boundary、cleanup 恰好一次、terminal/uncertain 传播、exact-context preflight。

### 测试（补 Review #4 指出的覆盖 gap）

- 新增 `generatedNormalFifthDeleteStillObservesOnceAndEndsPassOnStableEmpty`：4 次普通删除（前 3 次终极角 probe 为 INACTIVE/miss，保留获批 fresh static rescan）→ 第 4 次后 probe 命中 ULTIMATE_HINT → 终极角 click → 生成 NORMAL → generated delete（**恰为第 5 次**）→ 仍恰好观察一次 → EMPTY。断言 success、deletedCount=`5`、message=`generated normal skill deleted`、slot5=`EMPTY_SLOT`。
- 新增 `generatedNormalFifthDeleteWithUnstableSlotFailsInsteadOfBudgetSuccess`：同链但观察结果仍为 NORMAL → 断言 **not success**、message=`generated normal delete did not leave a stable slot`、slot5=`NORMAL_SKILL`、deletedCount=`5`。此用例在旧代码下会因预算早退返回 success，故直接锁住本 P1。
- 共享 `assertGeneratedFifthDeleteShape`：delete-prepare=`5`、ultimate click=`1`（120/700）、static scan=`4`、cleanup=`1`、scripted replies 全部消费为空、action 数与 UUID 数 1:1、action id 唯一、**最后一个 action 恒为 post-generated-delete 的 INPUT/WAIT/CAPTURE 观察**（无第 6 次删除、无后续 scan/action/UUID）。
- 均经 production public API `cleanSummonSkillsOnce(request)` 真实驱动；未新增 production 测试入口/helper/DTO/wrapper chain。既有 `:258-329` generated-normal（第 2 次删除）与五次普通删除预算用例完整保留、未改。

### Repair #3 当前 SHA-256

- `SummonSkillService.java`：`991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`（1431 行）。
- `SummonSkillTurnContractTest.java`：`6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998`（1683 行）。

### 未运行门与写集

- 写集严格限于 Cloud `SummonSkillService.java`、`SummonSkillTurnContractTest.java` 与本 append-only 原卡；未改协议/DHXY/Task/caller/其它 Service/POM/config/resource。
- 按父级禁令未运行 Maven/JUnit/compile/package/runtime/input，未执行任何 Git mutation；两仓全部 dirty/untracked 原样保护。编译门以静态自审替代：本轮自审已发现并修正一处真实编译错误（helper 误用 `harness.window.left()/top()`；`TurnWindowMetadata` 仅有 `windowRect()`，已改为 `harness.window.windowRect()`）。
- 本 Worker 不写 `APPROVED/CLOSED`，保持在线等待父级独立复审；TURN-34A 作为 C 的下一张卡继续等待。

**无已批准业务差异；按 `696a12b0:584-604` 与用户确认的 live static-tail/终极角规则等价迁移。**

<!-- TRUE_EOF: TURN-33 REPAIR #3 SOURCE+TEST DELIVERED EXTERNAL-C 2026-07-16T06:10:45.729-04:00 -->

## PARENT SOURCE+TEST-SOURCE REVIEW #5 - 2026-07-16T06:15:21.151-04:00

- 父级未采用 External C 自述，已独立逐行复核当前 production/test、交付 SHA、
  `696a12b0:584-604` 与 `docs/业务逻辑.md` 的三技能静态尾扫/终极角规则。结论：
  **`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / INDEPENDENT REVIEW+BUILD PENDING`**。
- 唯一 P1 已关闭：`SummonSkillService.java:823-849` 在 generated NORMAL 删除后于 `:829` 增加
  `deletedCount`，不再因第 5 次删除提前 success；`:837-849` 无条件执行一次
  `post-generated-delete-slot-*` observation，并且只让稳定 `EMPTY_SLOT/KEEP_SKILL` 成功，其余状态明确失败。
  这与 `696a12b0:584-604` 的无条件观察和稳定闭口一致。
- 无第六次删除/后续 action：`maybeClickUltimateCorner(...)` 的所有 production caller 都直接返回其结果；
  `progress.ultimateGenerateClicked` 在真实 click 成功后置位，既有 `:313/:375/:414/:438` 也阻止 fresh rescan。
  因而第五次 generated delete 后的 observation 是该 pass 最后一条 action，不会回到主循环或再生成 UUID。
- 测试源码有效：`SummonSkillTurnContractTest.java:420-453` 分别通过 production public API 覆盖第五次
  generated delete 后稳定 EMPTY 成功和仍为 NORMAL 失败；`:461-496` 构造四次普通删除加第五次 generated
  delete；`:504-540` 直接断言 delete-prepare=`5`、ultimate click=`1`、static scan=`4`、cleanup=`1`、
  scripted replies 全消费、action/UUID 1:1 且最后 action 为唯一 post-delete observation。旧代码会在消费最后
  observation 前提前 success，因此负例能抓住本次回归。
- 当前 SHA 与交付一致：Service
  `991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`，test
  `6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998`；另外两份 TURN-33 production SHA
  仍为 `3ee97295...`、`91349697...`，未被本轮改动。写集未扩大，未新增 retry/session/owner/ledger/TTL/
  durable workflow。
- External C 的 TURN-33 implementation owner 释放；下一门为两名非实现者独立 reviewer 与稳定 writer 后的
  named test/适用 compile。External B 仍在 TURN-28P Repair #2 写 Java，本轮不运行 Maven/JUnit/compile；
  本源码通过不代表 `CARD APPROVED/CLOSED`。External C 的下一张卡仍为 TURN-34A，由父级完成最终 brief 冻结后领取。

**无已批准业务差异；按 `696a12b0` 与用户确认的 live static-tail/终极角规则等价迁移。**

<!-- TRUE_EOF: TURN-33 PARENT REVIEW-5 SOURCE+TEST SOURCE REVIEW PASSED P0P1P2=0/0/0 REVIEW+BUILD PENDING OWNER RELEASED 2026-07-16T06:15:21.151-04:00 -->

## PARENT ADJUDICATION OF INDEPENDENT REVIEW R2 - 2026-07-16T06:25:00-04:00

- 父级完整读取 R2 固定报告及其逐行证据，并再次核对当前 reviewed SHA 未漂移。R2 最新轮结论
  `APPROVED / P0/P1/P2=0/0/0` 成立，未发现需要退回 External C 的新源码/测试源码问题。
- 采纳证据：generated-normal 第五次删除后无预算早退、唯一 post-delete observation、只接受 EMPTY/KEEP、
  所有 caller 在真实 ultimate click 后终结本 pass；两个 production-path fixture 会让 Repair #2 旧分支失败，且
  scripted command port 拒绝任何未脚本化第六次 action。
- 独立 review 门当前为 `1/2 APPROVED`；R1 尚未完成，不能以 R2 代替双审。即使双审通过，本卡仍须等所有 Java
  writer 稳定后运行点名 `SummonSkillTurnContractTest` 与适用 Cloud compile/build，才可父级 `CARD APPROVED`。
- R2 报告：`docs/superpowers/plans/reports/2026-07-16-turn-33-repair3-delivery-reviewer-r2.md`；该 reviewer 未改
  implementation/原卡/计划，未运行 Maven/runtime/input/Git mutation。

<!-- TRUE_EOF: TURN-33 PARENT ACCEPTED REVIEW-R2 APPROVED 1-OF-2 P0P1P2=0/0/0 BUILD-PENDING 2026-07-16T06:25:00-04:00 -->

## PARENT ADJUDICATION OF INDEPENDENT REVIEW R1 / DUAL REVIEW COMPLETE - 2026-07-16T06:29:00-04:00

- 父级完整读取 R1 固定报告，并独立重算两份 Repair #3 SHA；与 External C 交付及 Parent Review #5 一致：
  Service=`991db945f7d621e86287d7dadb121bc9154dfe7375f6176cb4ca71434bcaed04`，test=
  `6a755b0fb36152afd90ff59244c74cbeae086360dd8b53bf2f492cc83f968998`。R1 最新轮
  `APPROVED / P0/P1/P2=0/0/0` 成立，无新返修项。
- R1 独立确认 baseline 无条件 post-delete observation、current EMPTY/KEEP 唯一成功、所有真实 corner caller 在
  click 后终结、fixture 通过 production public API 且 Repair #2 旧早退会失败。写集、terminal、零自动 retry/
  session/owner/ledger/TTL/durable workflow 均无漂移。
- 独立 review 门现为 `2/2 APPROVED`：R1=`0/0/0`、R2=`0/0/0`，均已由父级分别采纳。source/test-source 与
  双 reviewer gate 完成。
- 当前仍有 External B TURN-28P 与 External C TURN-34A Java writer；按并发门不运行 Maven。TURN-33 只有在 writer
  稳定后点名 `SummonSkillTurnContractTest` 与适用 Cloud compile/build 通过，父级才可写 `CARD APPROVED`；当前状态
  仍是 `DUAL REVIEW PASSED / BUILD PENDING`，不是 CLOSED。
- R1 报告：`docs/superpowers/plans/reports/2026-07-16-turn-33-repair3-delivery-reviewer-r1.md`。

<!-- TRUE_EOF: TURN-33 PARENT ACCEPTED R1-R2 DUAL-REVIEW APPROVED 2-OF-2 P0P1P2=0/0/0 BUILD-PENDING 2026-07-16T06:29:00-04:00 -->

## PARENT STABLE-WRITER CLOUD BUILD GATE #1 - BLOCKED - 2026-07-16T14:40:21-04:00

- Authorized `mvn -q -Dtest=SummonSkillTurnContractTest test` exited 1 before the named class ran because shared
  Cloud main compilation failed first.
- Representative failures are incomplete whole-card migration owners: `WubeiTask`, `NavigationService`,
  `NpcClickService`, `DialogService`, and `PlayerStateService` still reference DHXY-only collaborators absent
  from Cloud. No Surefire report for `SummonSkillTurnContractTest` was created.
- This blocker is outside TURN-33's accepted frozen write set. TURN-33 is not returned for source repair and remains
  `SOURCE REVIEW PASSED / DUAL REVIEW PASSED 2/2 / CLOUD BUILD BLOCKED / NOT CARD APPROVED`.
- No runtime/application/server/Task/UI/capture/input or Git mutation was run.

<!-- TRUE_EOF: TURN-33 PARENT-STABLE-WRITER-CLOUD-BUILD-GATE-1 MAIN-COMPILE-BLOCKED-EXIT-1 SUMMON-NAMED-TEST-NOT-RUN BLOCKER-OWNED-BY-PLANNED-WHOLE-CARD-PREREQUISITES NO-TURN-33-SOURCE-REPAIR NOT-CARD-APPROVED 2026-07-16T14:40:21-04:00 -->
