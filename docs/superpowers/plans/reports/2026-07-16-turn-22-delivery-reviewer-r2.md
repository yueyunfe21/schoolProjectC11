# TURN-22 Repair #2 Delivery Review - R2

## REVIEW CLAIMED - 2026-07-16 EDT

- 角色：独立 delivery reviewer R2；非实现者、非父级 final reviewer。
- 审查范围：当前 `CloudTeamReturnPortAssembly`、`TeamReturnTurnContractTest`、production `TurnInputActionMapper` / `TurnInputStepExecutor` / `InputActionQueue`，并对照 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 的 `TeamReturnService`。
- 操作约束：只读源码；不等待或运行 Maven、runtime、application、input；不修改 Java、测试、TURN-22 原卡或主文档；不做 Git mutation。

<!-- TRUE_EOF: TURN-22 delivery-reviewer-r2 REVIEW-CLAIMED 2026-07-16 -->

## 独立审查基线

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、权威计划第 14-19 节及 TURN-22 原卡截至其最新 true EOF；父级 Review #3 与 Worker 自述未作为通过依据。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/TeamReturnService.java`。其中第 86-89 行是一次 `submitAndWait`，内部顺序为 `clickLeft(..., 150)`（第 87 行）后 `sleep(500)`（第 88 行）。
- 本轮最终字节快照：
  - cloud `CloudTeamReturnPortAssembly.java`：538 行，SHA-256 `4435B30C4BFC923E222B12DE3CDA5BE9AEEC766AA1F826F26EA534BC1A5CFD66`；
  - cloud `TeamReturnTurnContractTest.java`：1755 行，SHA-256 `CB41A6DD4AC931EABD470E67E25C9A5F653C55E1BBA240F4367E7D267CCF508B`；
  - DHXY `TurnInputActionMapper.java`：149 行，SHA-256 `B5C6F173BA9A5C40774E24446E6726108701AB47A89A0C80434F15415319303A`；
  - DHXY `TurnInputStepExecutor.java`：229 行，SHA-256 `0EE95CBD48D3EC76FB9E50385108F9898F2979A33966487B39065352AF1F43FD`；
  - DHXY `InputActionQueue.java`：775 行，SHA-256 `95572C202D1CFF73732FECEBFB7710AA07DC770A27940B3A85577C212031866E`。

## Findings

### P1-1：cloud named test 当前无法解析其声称穿透的 DHXY production 类

**证据**

1. `D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/TeamReturnTurnContractTest.java:3-6` 直接导入 DHXY 的 `TurnExecutionWindow`、`TurnInputActionMapper`、`TurnInputStepExecutor`、`TurnKeyMapper`；第 20-22、38-39 行又导入 `InputAction`、`InputActionQueue`、`InputActionType`、`WindowRuntimeContext`、`WindowTaskContextHolder`。
2. 同一测试第 1348-1374 行实例化上述 production executor/mapper/queue，并以此声称验证“一次真实 queue submission”。
3. `D:/mavenProject/dhxy-cloud-brain` 当前源码树不存在 `TurnInputActionMapper.java`、`TurnInputStepExecutor.java`、`InputActionQueue.java`、`WindowTaskContextHolder.java` 等类；当前 `target` 也不存在这些对应 `.class`。
4. `D:/mavenProject/dhxy-cloud-brain/pom.xml:27-82` 的依赖仅覆盖 OpenCV、Jackson、JCS、Spring、Lombok、SLF4J、JUnit，没有 DHXY artifact；第 84 行起的 build 配置也没有把 `D:/mavenProject/DHXY/src/main/java` 加为 test source/classpath 的配置。

**影响**

当前 cloud 模块的 `TeamReturnTurnContractTest` 在 test-compile 阶段没有这些 production 类型的来源，因此其 Repair #2 queue-penetration 断言不是可执行交付证据。此结论不是因为本 reviewer 没有运行 Maven，而是由当前源码图和 Maven 模型直接确定；也不能用 sibling repo 的陈旧 `target/classes` 或把 mapper/executor 复制进测试来替代 production 穿透。

**修复门**

需由父级重新冻结可复现的测试归属/依赖写集：要么把该 named contract 放到能直接编译 DHXY production executor/mapper/queue 的 DHXY 模块，并以同一 emitted spec 驱动；要么为 cloud test 明确增加受管、可复现的 DHXY test dependency/source-root。完成后必须证明 clean test-compile 不依赖 sibling `target` 残留。当前 Repair #2 仅改本测试文件无法合法补齐该 classpath。

### P2-1：named test 没有实际证明 exact `WindowTaskContext` 穿透及“原 context 恢复”

**证据**

1. `TeamReturnTurnContractTest.java:1350` 创建一个初始为空的 `WindowTaskContextHolder`；第 1373-1374 行仅断言执行后仍为空。即使 production 完全不绑定 context，这个断言也会通过。
2. 第 1380-1396 行分别创建 `WindowRuntimeContext context` 和 `WindowNativeBinding binding`，但没有执行 `context.setNativeBinding(binding)`。
3. 第 1403-1414 行的 `RecordingInputQueue` 覆盖 `submitAndWait` 后只复制 actions 并返回 `true`，从未在 queue 边界读取/记录 `contextHolder.rawCurrent()`，故无法证明提交时看到的是 exact window id/HWND/process。
4. production 实现本身是正确的：`D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java:169-171` 用 `contextHolder.callWith(window.context(), ...)` 包住 queue 调用，`WindowTaskContextHolder.java:106-117` 在 `finally` 恢复旧 binding；缺口在 Repair #2 测试证据，而不是此处已发现 production 缺陷。

**修复门**

测试应给 exact runtime context 安装 exact native binding；让 recording queue 在 `submitAndWait` 内记录并断言 window id/HWND/process；执行前预绑一个不同的 sentinel context，并在执行后断言恢复为 sentinel。这样才能同时击穿“未绑定也通过”和“只验证 empty -> empty”两类伪阳性，且不需要启动 worker 或发送 input。

## 已确认的 production 链路

- `CloudTeamReturnPortAssembly.java:122-136` 只构造一个 `CLICK_LEFT` step，并且只调用一次 `boundClient.execute`；`clickDelayMs=150`、`queueHoldMs=500` 同属该 spec。
- 同文件第 137-166 行把非 `COMPLETED` command、`DUPLICATE_OR_UNCERTAIN` 映射为 `UNKNOWN`，不重发；第 147-149 行拒绝 input frame；`FAILED` 不伪装成 executed，只有完整 `COMPLETED` 才返回 `EXECUTED`。
- `TurnInputActionMapper.java:39-47` 将同一 spec 映射为同一有序 `List`：`clickLeft(..., 150)` 后 `sleep(500)`。
- `TurnInputStepExecutor.java:60-67,166-177` 使用真实 mapper，将整份 actions 一次交给 queue，并且仅 queue 正常完成时返回 `COMPLETED/OK`。
- `InputActionQueue.java:67-80` 从当前 exact context 构造一个 `InputActionRequest`；第 626-630 行只 `offer` 一次。其 production worker 在一个 input transaction 内顺序执行该 request 的全部 actions，因此 150/500 段之间没有跨窗口插队点。
- 本链路未发现额外 frame、retry、session、ledger、TTL、第二 UUID、第二 command、park/yield 或业务判断差异；就 production 源码而言，Repair #2 已保持 `696a12b0` 的一次提交与 150/500 顺序。

## 最终结论

- `P0 = 0`
- `P1 = 1`
- `P2 = 1`
- **BLOCKED**

production 队列链路本身未发现阻断缺陷，但当前交付缺少可 test-compile 的 production-through named test，且 exact-context/恢复断言存在明确伪阳性。两项修复并基于最新字节重新独立复审前，不可写 `APPROVED`。

本轮按指令未运行 Maven、runtime、application、server、input，未等待或引用 R1 结论，未做 Git mutation，也未修改 Java、测试、TURN-22 原卡、权威计划或任何主文档。

<!-- TRUE_EOF: TURN-22 delivery-reviewer-r2 BLOCKED P0=0 P1=1 P2=1 2026-07-16 -->
