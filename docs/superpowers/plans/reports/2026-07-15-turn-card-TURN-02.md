# TURN-02 Report - Cloud 单槽 exchange 与 command result

## CLAIMED

- 领取时间：`2026-07-15T14:40:09-04:00`
- 角色：`Internal implementation worker`，不是 manager/reviewer。
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA Cloud single-slot turn exchange)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`（已 CLOSED）
- `approvalDependsOn`：`TURN-01D`
- 上一卡释放证据：TURN-08A 报告已由父级写入
  `SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING；源码 owner 已释放，可领取下一张 READY。`
- 唯一 Cloud Java 写集：
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnExchange.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnCommandPort.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnCommandResult.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnFrame.java`
  - `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTurnActionFactory.java`
  - 本报告（只追加领取、交付和父级审查记录）
- 只读：两仓 `cloud/turn/protocol/**`、Cloud `CloudTemplateCatalog.java` 及其它 Foundation 源码。
- 禁止触碰：上述五个 Cloud Java 文件和本报告之外的两仓全部文件；尤其不得修改协议、catalog、server、routes、
  主计划、CR271、ACTIVE_WORK 或 dashboard。

## 两仓 status 与基线（领取瞬间）

### DHXY

- 当前分支：`thin-client-design`
- 当前 HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- 可用远端基线：`origin/dev=e543d024bf900853944b36d27d0f736005d9eeb9`；当前分支无远端跟踪引用。
- 工作区已有大量他人 dirty/untracked，包括配置、CR/上下文文档、thin-client plans/specs、`pom.xml`、
  Service/Task/input/window 源码、`cloud/remote/` 与 `cloud/turn/`。
- 本卡在 DHXY 只新增本报告；不回滚、不覆盖、不清理、不提交任何既有 dirty/untracked。

### dhxy-cloud-brain

- 当前分支：`navigation-migration`
- 当前 HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- 可用远端基线：`origin/main=48e37813913094cacd1143fcae02704033eecb93`；当前分支无远端跟踪引用。
- 五个本卡 Java 路径在 HEAD、领取前 working-tree diff 与当前 turn 目录中均不存在。
- 工作区已有 `pom.xml`、server/gateway/算法/config 修改，以及 `logs/`、迁移备份、`com/bot/` protocol、
  host/remote、`CloudTemplateCatalog.java` 与模板等未跟踪内容；全部视为他人写集并保护。

## 实施边界

- 每个 `deviceId + windowId` 只保留一个未决 action 槽；不保留 action/outcome history。
- command-first 与 wait-first 都交付同一个 action 实例；第二个未决 command 返回 typed `BUSY`。
- previous outcome 只按相同 actionId 接收一次；HTTP 响应丢失后的重复提交按相同 canonical hash 幂等接受。
- command timeout/interruption 返回 typed uncertain/interrupted result，但 action 继续 fenced，迟到 outcome 仍只完成原 command。
- HTTP long-wait interruption只结束该次 wait，不清除/替换未决 action；下一次 turn 取得同一 action，避免重复物理执行。
- frame 必须与 previous outcome 的 frame metadata、SHA-256、尺寸和 action 相关性一致；frame bytes 不进入 JSON/hash。
- `CloudTurnCommandResult` 表达 Cloud 等待结果，不伪造业务 `TurnOutcome`。
- 不新增 history、timer、cleanup、retry executor、owner、permit、session、ledger、TTL、持久化或自动业务 retry。
- 不运行 runtime/application/server/Task/poller/UI/capture/input/tests；只在 cohort DTO/validator 稳定时运行规定 Cloud package。
- 若 protocol cohort 缺 DTO/validator 导致构建不可用，交付标记 `SOURCE DELIVERED / BUILD COHORT PENDING`。
- 交付后停止等待父级独立审查；本 worker 不自批、不写 APPROVED/CLOSED。

## DELIVERED

- 交付时间：`2026-07-15T14:46:27-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- `countUnit`：`N/A (INFRA Cloud single-slot turn exchange)`
- `countDelta`：`0`
- 完成内容：
  - `CloudTurnExchange` 按 `deviceId + windowId` 使用一个 `WindowTurnState`；状态字段严格只有
    `currentAction`、`currentOutcome`、`lastAcceptedActionId`、`lastAcceptedOutcomeSha256`、
    `lastResponseAction`，没有 action/outcome history 或清理时钟。
  - command-first：`execute(...)` 原子发布 action/future 后 `notifyAll`；wait-first：HTTP `exchange(...)` 在同一
    state monitor 上有界 `timedWait`，command 到达后返回同一个 `currentAction` 实例。
  - 未决 action 存在时第二个 command 返回 typed `BUSY`；刚接收完成的相同 actionId 返回
    `DUPLICATE_ACTION_ID`，不会重新发布。
  - command timeout/interruption 分别返回 `TIMED_OUT_UNCERTAIN` / `INTERRUPTED_UNCERTAIN`，不清除 action/future；
    interruption 恢复线程 interrupt 标志。迟到 outcome 仍只完成原 future 并释放原槽。
  - HTTP wait 中断只退出该次 monitor wait，不修改 action；当前 action 可由下次 turn 原样取得。已下发 action 保留在槽内，
    直到匹配 previous outcome 被接收，不会在 transport 不确定时生成替代 action。
  - previous outcome 必须匹配 request device/window、已实际下发的 current actionId；首次接收记录 canonical JSON SHA-256，
    丢失 HTTP 响应后的同 id/同 hash 重交幂等接受，同 id/异 hash 返回 conflict。
  - `CloudTurnFrame` 防御性复制 raw PNG；exchange 校验 outcome metadata 相等、`image/png`、SHA-256、region/尺寸及
    PNG decode 尺寸。frame bytes 不进入 JSON 或 canonical outcome hash。
  - `CloudTurnCommandResult` 独立表达 `COMPLETED/BUSY/DUPLICATE_ACTION_ID/TIMED_OUT_UNCERTAIN/
    INTERRUPTED_UNCERTAIN`；只有 `COMPLETED` 可携带真实 DHXY `TurnOutcome`，不伪造业务 outcome。
  - `CloudTurnActionFactory` 按冻结协议创建 capture、ordered input/mechanics、closed local-Service action；actionId 由 caller
    提供，不生成 id、不 retry，并调用共享 validator。
- 源码证据：
  - SHA-256：`CloudTurnExchange=DE1CDF302930215715A817B4A6370C77BF623BFA1ED2F8C0B409DCCA4AB3E8A3`，
    `CloudTurnCommandPort=20196ED20FCDDDAE64E93308D1F959A7C465E0E9B8D3C57F989433155B6FFADA`，
    `CloudTurnCommandResult=10AE705CCF00665CDC253A0078D260D1FC73A218196B4C8F83F0426E737F0A48`，
    `CloudTurnFrame=60DC01307002775D10CC1171831A67E417332F9740622D6D34534FF8B81CD51B`，
    `CloudTurnActionFactory=F978FB23985D4B0C4F8694EADB4EADD75F2408771FD291546D05E92CB372996A`。
  - 必需路径扫描确认五个单槽字段、`notifyAll/timedWait`、typed command result、重复 outcome mismatch、
    delivery-before-outcome、frame SHA 校验与共享 validator 调用均存在。
  - 禁用机制扫描无 `ScheduledExecutor`、`ExecutorService`、`Timer`、`@Scheduled`、`@PostConstruct`、旧 broker/ledger、
    owner/session/permit/TTL 字段或 Base64 调用；唯一 `Base64` 文本仅为 `CloudTurnFrame` JavaDoc 的禁止说明。
  - 五个 Java 文件 trailing-whitespace 扫描为 `NO_TRAILING_WHITESPACE`；Cloud scoped status 只出现本卡五个新文件。
- Maven 门：
  - `mvn -q -DskipTests clean package` 在编译前被仓库 `require-tests-enabled` enforcer 拒绝；由于本卡明确禁止运行 tests，
    未绕过 enforcer、未继续 package。
  - 随后运行天然不进入 test phase 的 `mvn -q compile`，exit `1`。领取前已有未跟踪迁移源码存在大量缺失依赖；
    对本卡 `cloudbrain/turn` 的过滤输出只报告并行 TURN-01D 尚未交付的 `TurnProtocolValidator`。action/outcome/frame 等
    TURN-01A/B/C DTO 已核对并与本卡构造/访问签名一致。
  - 因 protocol validator 与 Cloud working tree 均未稳定，本卡按合同标记 `BUILD COHORT PENDING`；最终 fresh package
    归父级稳定 cohort 门。
- 边界确认：未运行 tests/runtime/application/server/Task/poller/UI/capture/input；未修改协议、catalog、server、routes、
  主计划、CR271、ACTIVE_WORK 或 dashboard；未执行 Git 写操作。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 待父级：独立源码审查及稳定 cohort package。当前 worker 在 `DELIVERED` 停止，不自批、不写 APPROVED/CLOSED。

## PARENT REVIEW #1 - REPAIR REQUIRED

- 审查时间：`2026-07-15T15:05:00-04:00`
- 结论：单槽、fencing、同 action 重发、previous outcome canonical hash、future 完成与 frame 校验算法可保留；
  `P0=0 / P1=1 / P2=0`，`countDelta=0`。
- P1：本卡源码绑定了父级已判定错误并要求 TURN-01C 恢复的 wire API：
  - `CloudTurnExchange.java:104,129` 调用 `TurnRequest/TurnOutcome.windowMetadata()`，冻结 exact component 是 `window()`；
  - `CloudTurnExchange.java:112-113` 调用三参数 `TurnResponse(contractVersion,status,action)`，冻结 exact record 是
    `TurnResponse(Status status, TurnAction action)`。
- 影响：TURN-01C 按父级 Review #1 修复后，本卡将无法编译；若保留则会反向迫使 wire 合同继续漂移。
- 返修条件：只把上述 accessors/constructors 改到 Foundation Step 5 exact API；同步 JavaDoc/错误字段名中的
  `windowMetadata` 为 `window`。不得改单槽状态机、frame/hash 算法或其它文件；追加新 SHA/scoped check 后等待复审。

## REPAIR #1 DELIVERED

- 返修时间：`2026-07-15（父级 Review #1 后）`
- 状态：`SOURCE REPAIR DELIVERED / BUILD COHORT PENDING`
- `countDelta`：`0`
- P1 修复：
  - `TurnRequest` 与 `TurnOutcome` 均改用冻结 accessor `window()`；请求非空错误字段同步为 `request.window`。
  - `TurnResponse` 改用冻结二参数构造 `new TurnResponse(Status, action)`；删除因此不再使用的本地
    `CONTRACT_VERSION` 常量。
  - `CloudTurnExchange.java` scoped 扫描确认 `windowMetadata`、`CONTRACT_VERSION`、三参数
    `TurnResponse` 构造均为零残留；`validated.window()` 与二参数构造在预期四处命中。
- 边界校验：单槽五字段、action 发布/等待、previous outcome canonical hash、future 完成及 frame 校验路径仍在原位置；
  未修改状态机、frame/hash 算法或本卡其余四个 Java 文件。
- 新 SHA-256：
  - `CloudTurnExchange=0F005B9D394F893F5BD818F8807FEFCDADF44A7D75CCACE0C4AFFC240FB83A3A`
  - `CloudTurnCommandPort=20196ED20FCDDDAE64E93308D1F959A7C465E0E9B8D3C57F989433155B6FFADA`
  - `CloudTurnCommandResult=10AE705CCF00665CDC253A0078D260D1FC73A218196B4C8F83F0426E737F0A48`
  - `CloudTurnFrame=60DC01307002775D10CC1171831A67E417332F9740622D6D34534FF8B81CD51B`
  - `CloudTurnActionFactory=F978FB23985D4B0C4F8694EADB4EADD75F2408771FD291546D05E92CB372996A`
- scoped status：Cloud 侧仍只显示本卡五个未跟踪 Java 写集文件；DHXY 侧仍只显示本卡未跟踪报告。
- 门禁：按父级指令未运行 cohort Maven；未运行 tests/runtime/application/server/Task/poller/UI/capture/input，未执行
  Git 写操作，未修改其它文件。
- 待父级：复审本次 wire API 返修。worker 停止于 `REPAIR DELIVERED`，不自批、不写 `APPROVED/CLOSED`。

## PARENT RE-REVIEW #1

- 复审时间：`2026-07-15T15:15:00-04:00`
- 父级独立展开 `CloudTurnExchange`，确认 accessor/constructor 已对齐冻结 `window()` 与二参数
  `TurnResponse`，旧 `windowMetadata`/本地 `CONTRACT_VERSION`/三参数构造零残留；单槽状态机与其它四文件未改。
- 结论：`SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING`；`countDelta=0`。原 P1 关闭，源码 owner 释放。
- Build/CLOSED：等 `TURN-01D` 与 Cloud Foundation cohort 稳定后由父级统一 package；本卡不占实现槽等待。
