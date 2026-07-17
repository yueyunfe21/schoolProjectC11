# CR271 TURN-22 Repair #3 Source-Start PRECHECK

CLAIMED agent_id=`019f6acc-5f29-7fe1-9a8b-60c780a4e3e5` nickname=`Galileo` role=`CR271 Internal helper` snapshot=`2026-07-16T08:14:18.387-04:00`

本报告是 Internal helper 的只读预检证据，**非 implementation owner、非 reviewer、非父级批准，也不批准或阻断任何卡**。平台身份与本次 helper 分工见 `docs/ACTIVE_WORK.md:3-12`。唯一写入是本 PRECHECK 报告；未修改 Java、POM、测试或现有卡片，未运行 Maven/JUnit/compile/package/runtime/application/server/Task/UI/capture/input，未执行 Git mutation。

## 1. 权威输入快照

| 文件与完整读取范围 | mtime (EDT) | SHA-256 |
|---|---|---|
| `AGENTS.md:1-392` | `2026-07-11T17:24:59.650-04:00` | `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5` |
| `docs/DHXY_CONTEXT.md:1-1349` | `2026-07-16T03:32:25.494-04:00` | `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621` |
| `docs/ACTIVE_WORK.md:1-120`（顶部 CR271；文件共 79,934 行） | `2026-07-16T08:09:54.210-04:00` | `F17DC1659C897A227CA25E92E0081D0EE8DFD2D2CFC43F4B852B1F63A6A02318` |
| `docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:1016-1697`（第 14-19 节） | `2026-07-16T08:09:54.212-04:00` | `D9D65F476200E3C5DD281BD00C239F3954B2A77C18BDDADD41CB45F83D6C3CD8` |
| `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:1-383` | `2026-07-16T03:13:40.474-04:00` | `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB` |
| `docs/业务逻辑.md:1-1426` | `2026-07-11T19:40:58.481-04:00` | `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49` |
| `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-22.md:1-526` | `2026-07-16T08:01:30.395-04:00` | `92A0EBF56440E7DC94846EC348346DACB6653CDDEAC1B27029DB78F879F122BE` |
| `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-28P.md:1-1058` | `2026-07-16T08:05:05.419-04:00` | `0B232D3B3903068C6ACDF90DED2F3CFB2444A437FC47D4D7E7119C494CB3E153` |

适用规则证据：

- `AGENTS.md:293-295` 明确禁止在 input-worker exclusive callback 内再次提交 input queue；唯一 worker 正执行 callback 时嵌套请求会 queue-in-queue deadlock。
- `docs/业务逻辑.md:215-224` 要求未获批准时按基线等价迁移，不得自行改变输入/验证顺序；`:241-254` 继续保护已验证回城事实跨归队/排队不失效。
- 权威计划 `:1269-1272` 固定 TURN-22 Repair #3 三个 Java/test 文件，assembly/Service、mapper/protocol/POM/caller 只读；`:1458-1461` 区分 source-start 与最终 source/review/build；`:1474-1479` 和 `:1684-1697` 固定测试、compile 与卡批准硬门。
- 协议 `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:216-222` 明确要求 `clickDelayMs` 映射到 click、`queueHoldMs` 映射到同一 list 的 sleep，并把**完整 list 一次提交**到 global input queue。

## 2. 两仓只读状态

状态采样时间为 `2026-07-16T08:13:00.339-04:00..08:13:00.360-04:00`；status 摘要 SHA 的算法为 `git status --porcelain=v1 --untracked-files=all` 各行以 UTF-8/LF 连接、末尾不补 LF。

| Repo | branch | HEAD | status entries | untracked | deleted | status SHA-256 |
|---|---|---|---:|---:|---:|---|
| `D:\mavenProject\DHXY` | `thin-client-design` | `0114604e1ff5f15491d2910959c45252e893d04f` | 659 | 615 | 1 | `851BED09984BB5DE7A6AE9F850DDEBFFEF4F694F9C15D23C15134A9968B24079` |
| `D:\mavenProject\dhxy-cloud-brain` | `navigation-migration` | `3b988caa010254973e03342272e6d1d6a9685b01` | 550 | 541 | 0 | `E85B93E8CE8DD62EF5FA8D729E9E723F458BD4A9E8351081636D8DC2BD132C72` |

Cloud named test 位于 `.gitignore:15` 的 `src/test/` 忽略树，因此不出现在普通 Cloud status；当前文件仍可按下表 SHA/mtime 精确保护。两仓全部既有 dirty/untracked 均未被本 helper 改动或清理。

## 3. TURN-22 Review #4 与当前门

1. 最新完整 delivery review 是 `TURN-22.md:446-501`：`P0/P1/P2=0/2/1`，其中 `:454-462` 指出 Cloud test 非法导入 DHXY-only mechanics，`:464-473` 指出 executor 把 frozen coordinates 交给会二次 refresh 的 legacy queue，`:475-481` 指出 empty-to-empty context restore 伪阳性。
2. Review #4 在 `:483-493` 冻结 Repair #3 三文件：Cloud `TeamReturnTurnContractTest.java`、DHXY `TurnInputStepExecutor.java`、DHXY `TurnInputStepExecutorContractTest.java`；`:471-473` 要求完整 `[CLICK_LEFT(delay=150), SLEEP(500)]` list 一次进入 frozen snapshot boundary。
3. 父级后续 source-start reassessment 位于 `:503-526`。它在 `:505-509` 以 callback-shaped frozen API 已落盘、28P 只剩两测试为前提，并在 `:518-519` 再次要求 executor 通过该 public API 一次提交完整 action list。物理 EOF 仍为 `:526` 的 `EXTERNAL-A READY`，截至本快照没有 `EXTERNAL-A REPAIR #3 CLAIMED`。
4. 权威计划同步写成 `:1134` 的 `SOURCE-START READY`，但原计划测试表 `:1632-1634` 仍同时把 `TurnInputStepExecutorContractTest` 列在 TURN-22 与 TURN-28P 合同中；当前并发 modify set 由最新原卡收窄，最终测试归属文字仍需父级消歧。

## 4. TURN-28P 当前 source 与剩余写集

TURN-28P 最新物理 EOF `TURN-28P.md:1028-1058` 是 Internal Euler 的规范 CLAIM。`:1036-1041` 将 modify write set 限定为下列两测试和 TURN-28P 原卡，其他 production/test 文件只读；`:1047-1054` 只要求把两份同步 fake 换成 real queue/worker harness。

### 4.1 冻结 production 字节

下列当前 SHA 与 TURN-28P 交还表 `:927-932` 逐项相同；mtime 均早于 Euler 的 `08:04:28.103` CLAIM，证明当前 28P owner 没有修改 production API。

| DHXY 文件 | 精确接口/实现行 | 行数 | mtime (EDT) | SHA-256 |
|---|---|---:|---|---|
| `src/main/java/com/bot/dhxy/input/InputSequences.java` | `:67-87` | 180 | `2026-07-16T06:03:31.177-04:00` | `2D1768E67A12BF34D58FB64F14102614DC0C597EB41476DC60A49841089F2B6A` |
| `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java` | `:319-365` | 794 | `2026-07-16T06:03:09.683-04:00` | `BCD1E64A523AD258360CAE4110C575E318ACBB824AD1CDC49DD06AC0F3B1ABC4` |
| `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java` | `:245-256,375-417,864-892` | 1,085 | `2026-07-16T06:00:49.409-04:00` | `1CFF61300296EF42A4B6C2CD8CBA89B40BEAA27771178851CF6E52440E29F324` |
| `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java` | `:119-198,314-357,388-438` | 651 | `2026-07-16T06:00:35.251-04:00` | `1359C2361E134829C98ADF193A062019D59239B9642347DFB0BD35063BE032BD` |
| `src/main/java/com/bot/dhxy/input/WindowAwareInputCoordinator.java` | `:148-179` | 243 | `2026-07-16T05:59:32.003-04:00` | `0F22571A5727248C34E26FDD8A7ED930C15B7B0106452050CCFAA3520F67E6B8` |
| `src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java` | `:205-235` | 587 | `2026-07-16T06:04:08.779-04:00` | `5612B067E4A3F16B48845BD50DCC046CEA3E15FC93781888637210E867CE59F0` |

### 4.2 剩余两测试的实时字节

| TURN-28P 当前唯一可改测试 | CLAIM 时证据 (`TURN-28P.md:1042-1046`) | `08:14:18.387` 当前证据 |
|---|---|---|
| `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java` | 649 行；`ED2CD35CD419447D4F0D37CD0DB4D05455115D184354B9469B3FD2F836CC9FE8` | 946 行；mtime `2026-07-16T08:13:59.881-04:00`；SHA `B1B1CF6FC1804EB9CA939D8FE3F0B8DE2167BA2DA057DBFB1213887C568E4692` |
| `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java` | 1,086 行；`20DB8BB7A8FB3E9AE12AD8760C2191E2D570953A756B9F52878E5A291223520B` | 1,086 行；mtime `2026-07-16T06:08:33.100-04:00`；同 SHA |

第一份测试在 Euler CLAIM 后已真实变化，且变化仍严格位于 `TURN-28P.md:1036-1039` 的两测试写集；第二份尚未变化。该事实支持“28P 当前只写两测试”，不证明其测试或最终门已经通过。

## 5. 三文件写集互斥核验

| TURN-22 Repair #3 文件 | 当前行数 / mtime / SHA-256 | 与 Euler 两测试路径交集 |
|---|---|---|
| Cloud `src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java` | 1,755；`2026-07-16T05:07:53.793-04:00`；`CB41A6DD4AC931EABD470E67E25C9A5F653C55E1BBA240F4367E7D267CCF508B` | 无；不同 repo、不同路径 |
| DHXY `src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java` | 229；`2026-07-16T03:41:54.073-04:00`；`0EE95CBD48D3EC76FB9E50385108F9898F2979A33966487B39065352AF1F43FD` | 无；production source 对两 test |
| DHXY `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java` | 394；`2026-07-16T03:55:53.693-04:00`；`BB1CCC432020A8ACD61C82ABE207E13FB7959D94E9F8F6F27DB28B43DAFB738D` | 无；测试类名/路径均不同 |

**文件级结论：当前 concurrent modify write set 精确互斥。** Cloud test 现状仍在 `:3-39,1348-1421` 导入/实例化 DHXY-only mechanics；Cloud `pom.xml:27-82` 没有 DHXY artifact/test dependency（POM 193 行，mtime `2026-07-14T13:11:18.238-04:00`，SHA `F40967034F88E9B73EAF83A348DF199D4BB62CBDF23C3034A950BFE20891A6A3`），所以 Review #4 的 P1-1 仍待 TURN-22 自己在上述 Cloud test 内修复。

## 6. 具体反证：冻结 API 不能承载既定 action-list 目标

文件互斥不等于功能依赖已经满足。当前 production API 的实际形状与 TURN-22 `:471-473,518-519`、协议 `:216-222` 的完整-list目标不相容：

1. `InputSequences.java:67-87` 的唯一 frozen public 方法参数是 `Supplier<Boolean> callback`，没有 `List<InputAction>` 参数；其 Javadoc `:77` 还明确 callback 不得提交 nested input request。
2. `InputActionQueue.java:319-365` 的唯一 frozen public 方法同样只接收 `Supplier<Boolean>`。相对地，接受 `List<InputAction>` 的 legacy `submitAndWait` 在 `:67-80` 会先执行 `refreshAndValidateNativeBinding(...)`，正是 Review #4 `TURN-22.md:464-470` 禁止继续使用的路径。
3. `InputActionRequest.java:245-256` 的 frozen factory 把 actions 固定为 `List.of()`，只保存 exclusive callback；`:375-384` 明确区分 action-list request 与 callback request。
4. `InputActionWorker.java:128-130` 只有 `isFrozenExactWindow() && hasExclusiveCallback()` 才进入 frozen 分支；`:401-438` 在 generation monitor 内执行 callback。普通 action list 由 `:174-198` 循环，但真实 action dispatcher 是 `:317-357` 的 `private execute(...)`，TURN-22 executor 没有可调用的 public direct-action边界。
5. `TurnCaptureStepExecutor.java:214-235` 能使用当前 frozen API，是因为它的 callback 本身持有 capture/keyboard/input collaborators 并直接执行 probe mechanics；这不是一个把现成 `List<InputAction>` 交给 worker replay 的 API。
6. 当前 `TurnInputStepExecutor.java:22-37` 只有 queue/keyboard/context-holder/mapper/key-mapper；`:60-67,125-145` 生成 `List<InputAction>`，`:166-177` 只能通过 context holder 调 legacy `submitAndWait(description, actions)`。该文件当前 229 行，mtime `2026-07-16T03:41:54.073-04:00`，SHA `0EE95CBD48D3EC76FB9E50385108F9898F2979A33966487B39065352AF1F43FD`。
7. 把 legacy `submitAndWait(actions)` 塞进 frozen callback 不是替代方案：`AGENTS.md:293-295` 明确说明唯一 worker 已在 callback 内，嵌套 queue 会死锁。把 `InputProvider` 注入 TURN-22 executor 后复制 `InputActionWorker.java:317-357` 的私有 dispatcher，也不会满足协议“完整 list 一次提交”，且会自造第二套 physical action executor。

因此，截至本快照，**“TURN-28P production API 已冻结”属实，但“该冻结 API 已提供 TURN-22 所需的 exact-window action-list submission boundary”不属实**。这是对“可按现有冻结 API直接开工”的具体源码反证；本 helper 不把它升级为父级 `BLOCKED` 或 reviewer verdict。

## 7. `696a12b0` 基线核验

- Git 对象存在：commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，commit time `2026-06-30T01:43:39-04:00`，subject `chore: remove obsolete debug tooling`；`TeamReturnService.java` blob SHA-1 为 `286c5a85f01d010e883f8c4321ea1793776c932f`。
- `git show 696a12b0...:src/main/java/com/bot/dhxy/service/TeamReturnService.java:65-91` 保持先 observe、incense、refresh、再 click；`:86-89` 是同一次 `InputSequences.submitAndWait` 的 `InputAction.clickLeft(x,y,150)` 后 `InputAction.sleep(500)`。
- Repair #3 没有获准改变该顺序；`TURN-22.md:499,523-524` 和 `docs/业务逻辑.md:215-224` 均要求无批准差异时等价迁移。

## 8. PRECHECK 结论与父级建议

1. **写集互斥：是。** TURN-22 三文件与 Euler 当前两测试没有路径交集；TURN-28P production hashes 仍等于冻结交还表。
2. **不能给出“可开工但最终 source/build 仍挂 28P 测试门”的无条件结论。** 具体反证是 frozen public API 只承载 callback、frozen request 的 actions 恒为空，而 TURN-22/协议要求完整 `[CLICK_LEFT(150), SLEEP(500)]` list 一次通过 exact-window queue boundary。
3. **建议父级在 External-A CLAIM/Java mutation 前重新冻结依赖接口。** 推荐在 TURN-28P ownership 下补一个真正接收 `List<InputAction>` 的 frozen exact-window public boundary，并让 request/worker 在同一 generation monitor 内 replay 该 list；预计至少涉及 `InputSequences.java:67-87`、`InputActionQueue.java:319-365`、`InputActionRequest.java:245-256`、`InputActionWorker.java:128-198,401-438` 及对应 contract tests。该选择会推翻“production API 不再改”的前提，必须由父级重开/扩展 write set 后再冻结，不能由 TURN-22 三文件顺手修改。
4. 不推荐把 TURN-22 改成 direct-input callback：它需要新增 direct input collaborator并复制 private action dispatcher，既不满足协议 `:216-222` 的完整-list submission，也与 `AGENTS.md:293-295` 的 nested-queue禁令相冲突。
5. 无论父级如何处理上述 API 反证，最终门仍不变：TURN-28P 两测试需要正式 delivery/父级复审，TURN-22 自身 source/test review、点名测试与适用 compile 也必须按计划 `:1474-1479,1684-1697` 完成。source-start 不能冒充 source/build approval。
6. 权威计划 `:1634` 对 `TurnInputStepExecutorContractTest` 的 TURN-28P 归属与当前 Euler 两测试 exact modify set 不一致；建议父级在接口重新冻结时一并注明该文件是 TURN-22 writable、TURN-28P 只读消费，或显式调整 owner，避免最终 test gate 再次出现双归属。

**非父级批准：本报告只提供 source-start 前置事实、具体反证与建议，不批准、不阻断、不关闭 TURN-22/TURN-28P。**

<!-- TRUE_EOF: PRECHECK_COMPLETE CR271 TURN-22 REPAIR-3 SOURCE-START helper=Galileo agent_id=019f6acc-5f29-7fe1-9a8b-60c780a4e3e5 non-parent-approval snapshot=2026-07-16T08:14:18.387-04:00 -->
