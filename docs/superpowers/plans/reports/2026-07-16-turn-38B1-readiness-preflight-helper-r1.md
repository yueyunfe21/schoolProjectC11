# CR271 TURN-38B1 Bag workflow-state rewire exact-boundary readiness preflight R1

## 0. 角色、口径与当前结论

- 角色：CR271 Internal 非绑定 readiness helper。本文只提供源码、依赖、写集、测试边界和风险证据。
- 快照时间：2026-07-16T07:22:33-04:00。
- readinessEvidence：完整。
- implementationStart：存在下列 REAL_START_BLOCKER 事实，尚需父级冻结；本文不改变卡片状态，不领取实现，不作父级批准或阻断裁决。
- `PRECHECK_COMPLETE` 只表示本轮只读证据闭合，不表示 TURN-38B1 可开工。
- 无已批准业务差异；必须按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`、exact-window generation 与最小 HTTPS JSON turn 等价迁移。

## 1. 已读取的权威材料

本 worker 已完整读取并按下列优先级使用：

1. `D:/mavenProject/DHXY/AGENTS.md`，SHA-256
   `AD737D5652E7ABDFFBD626A8E617077D5475DF49D5433CF249E92757BBDD2FC5`。
2. `D:/mavenProject/DHXY/docs/DHXY_CONTEXT.md`，SHA-256
   `8A7838763CE04B12A2C62E09624896827FDEC6BE5D07AC99B71357C644557621`。
3. `D:/mavenProject/DHXY/docs/ACTIVE_WORK.md` 顶部 CR271；07:22 快照 SHA-256
   `17E8CE72098E1998FFB2C68BBCA4E8F3E1AA1AF0AB20375F68D0430F6A0C84D3`。
4. `D:/mavenProject/DHXY/docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md`
   第 14 至 19 节。完整读取后又针对 07:21 并发更新重读 `:1138-1165`；07:22 快照 SHA-256
   `BF9FC9D0D528C1F1769990A89CB8653D53A52098EC2680E8692243C14800DC13`。
5. `D:/mavenProject/DHXY/docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md`，
   SHA-256 `13D441A0436F1607A36F127C48A802B081BEA3143133E40542E5B49CCC45C3CB`。
6. `D:/mavenProject/DHXY/docs/业务逻辑.md`，SHA-256
   `46A7CAE771A100C1C00E33997FF354B620E0A313036BB2811FEAE21CBB469C49`。
7. TURN-14、TURN-35、TURN-36、TURN-37 与最新 TURN-38A 报告：
   - `2026-07-15-turn-card-TURN-14.md`
   - `2026-07-16-turn-35-readiness-preflight-helper.md`
   - `2026-07-16-turn-35-latest-dependency-readiness-helper-r1.md`
   - `2026-07-16-turn-36-readiness-preflight-helper.md`
   - `2026-07-16-turn-37-readiness-preflight-helper.md`
   - `2026-07-16-turn-38A-readiness-preflight-helper.md`
8. 两仓相关 production/test 源码、所有 `CloudBagStateOwner|BagWorkflowState` Java 引用、两仓 status。

权威覆盖规则来自计划 `:1035-1043`：第 16 至 18 节覆盖旧段；`PLANNED` 不可领取；若实现需要第 17 节之外的文件，必须先由父级修订计划，worker 不得临场扩写集。

## 2. 两仓 dirty/untracked 快照

### 2.1 DHXY

- root：`D:/mavenProject/DHXY`
- branch：`thin-client-design`
- HEAD：`0114604e1ff5f15491d2910959c45252e893d04f`
- `git status --porcelain=v1 --untracked-files=all` 计数：`D=1, M=43, ??=606`
- 活动写者：ACTIVE_WORK 顶部记录 External B 的 TURN-28P Repair #2、External C 的 TURN-34A，以及互斥的 Internal PRECHECK 报告写者。

### 2.2 dhxy-cloud-brain

- root：`D:/mavenProject/dhxy-cloud-brain`
- branch：`navigation-migration`
- HEAD：`3b988caa010254973e03342272e6d1d6a9685b01`
- status 计数：`M=9, ??=541`
- TURN-38B1 两个目标 production 文件均为既有 untracked 文件：
  - `?? src/main/java/com/bot/dhxy/service/bag/CloudBagStateOwner.java`
  - `?? src/main/java/com/bot/dhxy/service/bag/BagWorkflowState.java`
- 本轮未覆盖、删除、恢复、暂存或格式化任何 dirty/untracked 文件。

计数只是 07:22:33 快照；共享工作树有并发 writer，领取前必须重新核对 full status、mtime、SHA 和 owner。

## 3. 权威卡、依赖与波次

### 3.1 TURN-38B1 权威合同

- 计划 `:1155`：`TURN-38B1 | PLANNED / READINESS ACTIVE | S=14+38A`。
- 计划 `:1314-1323`：四张 38B 卡写集互斥，B1 只允许两个 Cloud production 文件，且不得顺带并入 authority-bound remote state。
- 计划 `:1430-1433`：`35/36/37 -> 38A -> 38B1/B2/B3/B4 -> 38C -> 39`。
- 计划 `:1497` 的 `STATE` profile：tenant/user 私有、device/window exact、pause/resume 同 state、stale reject、terminal/restart release、无 TTL。
- 计划 `:1646`：唯一 named test 为 `service/bag/BagWorkflowStateTurnTest`。

### 3.2 startDependsOn 实况

| predecessor | 当前事实 | 对 B1 的含义 |
|---|---|---|
| TURN-14 | 原卡末尾为 production/test source review passed；named test 与 Cloud compile/build pending。Typed client 已固定 `BAG_RETURN_ITEM` 与 `BAG_USE_INCENSE`，没有 B1 state 引用 | source/API 侧已有稳定证据；最终 build 债仍需父级按全局门处理 |
| TURN-38A | 最新 PRECHECK `:8,317-342` 为 REAL_BLOCKER evidence；权威计划 `:1154` 仍为 parent audit pending | 直接 start dependency 未满足 |
| TURN-35 | latest helper 记录 TURN-22/27/28/34A/34B source gates 未满足 | 38A 的传递前置未稳定 |
| TURN-36 | readiness 报告记录 27/28/34A 与 open-main-bag typed boundary 未闭合 | 38A 的传递前置未稳定 |
| TURN-37 | readiness 报告记录 28P -> 22/28、27、34A/34B 等依赖待闭合 | 38A 的传递前置未稳定 |
| TURN-34C | 计划 `:1145` 仍为 PLANNED；34A 当前只是 resumed active，34B 仍等 TURN-22 | 38A 的直接前置未满足 |

因此，当前唯一字面 direct gate 中，`S=38A` 已足以阻止实现 claim；35/36/37/34C 是它背后的真实未稳定链。本文不把 TURN-14 的 build pending 擅自解释成新的 source-start gate，也不绕过最终测试/构建门。

## 4. exact write set 与 named-test ownership

计划 `:1181-1182` 明确 `C:` 是 `D:/mavenProject/dhxy-cloud-brain/src/main/java/`，不是 DHXY 仓。

### 4.1 唯一 production write set

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/bag/BagWorkflowState.java`
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/bag/CloudBagStateOwner.java`

当前实物：

| file | bytes | SHA-256 | mtime |
|---|---:|---|---|
| `CloudBagStateOwner.java` | 34,823 | `EAFA0D7E4B98C6545A954867629603D402F3EBB10B4CC497F0130A24C4396AC1` | 2026-07-13 18:54:19.2912107 |
| `BagWorkflowState.java` | 41,156 | `34EAD25E28BD640BDAEDCB51840940CA4D3009B896343A078FA981AD2BE5FFD8` | 2026-07-13 18:54:47.9870475 |

### 4.2 唯一 test write set

计划 `:1566-1569` 将 `C_TEST` 固定为
`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain`。因此 B1 的物理 test 路径应为：

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/bag/BagWorkflowStateTurnTest.java`

当前该文件不存在；备用的 `src/test/java/com/bot/dhxy/service/bag/BagWorkflowStateTurnTest.java` 也不存在。Cloud `.gitignore:15` 忽略整个 `src/test/`，所以未来交付不能只依赖普通 `git status`，必须使用 `Test-Path`、mtime 与 SHA。

B1 不拥有且不得修改：

- TURN-13C 的 `runner/context/TaskExecutionContextTurnContractTest`
- TURN-38A 的 `runner/context/TaskExecutionContextOldAuthorityRemovalTest`
- TURN-14 的 `service/ReturnItemPrescanTurnContractTest`
- TURN-35/36/37 的三个 whole-task tests
- 任何 production Task、runtime、host configuration、protocol、facade 或 DHXY `BagService`

若父级认为验收必须修改上述任一文件，当前 exact write set 不足，需先修订计划。

## 5. 当前真实 owner、construction source 与 caller

### 5.1 当前 live 机械/cache owner 在 DHXY

`D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/service/BagService.java:40-73` 是 Spring `@Component`，实例字段实际持有：

- `visiblePageCache`
- `itemPageCache`
- `lastMainBagAnchorCache`

`:1277-1313` 显示真实 cache key 由当前 `windowId + layout` 组成，Holder 缺失时退回 `global`；item key 再加 template。它是当前运行代码的实际 cache owner，但它是永久本地机械 Service，且不在 B1 写集。协议 `:354-356` 与 DHXY_CONTEXT `:45-66` 都要求 `BagService` 留在 DHXY，Cloud 只能通过闭合 typed operation/result 调用，不能复制本地截图/input 机械实现。

### 5.2 CloudBagStateOwner 只是 dormant definition

`CloudBagStateOwner.java:15-26` 自述未接 host/assembly/Service；`:77-79` 构造器为 private，且没有 factory。`:66-70` 内部有 visible/item/anchor/geometry 四张 map，但当前没有运行实例。

全 Cloud `src/main/java + src/test/java` 扫描结果：

- `CloudBagStateOwner|BagWorkflowState` 共 21 条 Java 行命中，全部在这两个定义文件自身。
- external production reference：0。
- external test reference：0。
- `new CloudBagStateOwner(...)`：0。
- `new BagWorkflowState(...)`：0。
- 两类都没有 `@Component/@Service/@Bean` construction。

因此当前不存在“真实 Cloud owner instance”，也不存在生产 registry、factory、terminal callback 或 teardown caller。

### 5.3 BagWorkflowState 也不可构造

`BagWorkflowState.java:101-115` 的 public constructor 要求：

- 一个无法从外部构造的 `CloudBagStateOwner`
- `RemoteTaskRunScope`
- `taskRunId`
- `RemoteTaskRunWindow`
- `stopEpoch`

即使 constructor 本身 public，owner 不可得，且参数全依赖待移除 old authority；当前 production/test 构造计数仍为 0。

### 5.4 typed Bag path 不消费 B1 state

`CloudBagLocalServiceClient.java:56-125` 只经 `TurnGameClient.localService(...)` 发送
`BAG_RETURN_ITEM` 或 `BAG_USE_INCENSE`，没有 B1 state field、constructor 或 method reference。TURN-14 因此提供了 permanent-local Bag mechanics 的 typed client，但没有提供 B1 owner/lifecycle。

### 5.5 production runtime construction 仍缺席

- Cloud production 中 `TaskExecutionContext.turnNative(...)` caller 为 0；现有命中都在 tests。
- Cloud production 中 `new CloudTaskServiceMetadata(...)` caller 为 0；现有命中都在 tests。
- 计划 `:1355-1362` 的 `turn/runtime/CloudTurnTaskFactory|Runtime|Registry|StartResult|ControlPort` 目录当前不存在，属于后继 TURN-40B。
- `CloudServiceHost.create(...)` 当前只有 test callers；production caller 为 0。

这意味着 B1 不能以“现有 runtime 会负责构造/释放”为事实前提。

## 6. 当前 state 内容与 public API

### 6.1 CloudBagStateOwner

Public surface：

- bound-base observation：`:98` begin、`:142` accept
- visible-page cache：`:199` read、`:214` write
- item-page cache：`:232` read、`:251` write
- MAIN_BAG anchor：`:274` read、`:302` write
- full-scope teardown：`:348`
- public nested layout/geometry/result/permit/handle types：`:496-670`

Current state semantics：

- searchable page 只允许 `0..4`，见 `:60-62,445-450`
- visible/item hint 旧设计 key 是 old full scope + logical window + closed layout；item 再加 canonical template，见 `:194-265,686-694`
- anchor/geometry key 是 old scope + `RemoteTaskRunWindow` + geometry generation，见 `:274-332,696-705`
- observation sequence 是 per scope/window 的严格递增进程内 fence，无 TTL，见 `:707-715`

### 6.2 BagWorkflowState

Public surface：

- old identity constructor/getters：`:101-130`
- workflow transaction begin/finish：`:143,179`
- open flow begin/read/advance/finish：`:213,237,256,297`
- page pass freeze/read/advance/finish：`:326,362,379,412`
- session ordinal read/advance：`:438,456`
- frozen point：`:489`
- pending observation record/read/clear：`:531,557,574`

Retained fields `:57-82` 包括 current/last transaction、open stage、page order/cursor、session ordinal、frozen points 与 pending observation。旧注释 `:22-27` 的目标是 pause/resume 跨 revision 保持同一对象，但当前没有任何实际 owner 或 caller 来兑现。

## 7. old authority 引用与新 metadata 候选来源

### 7.1 两文件内当前 old coupling

`CloudBagStateOwner`：

- executable imports：`RemoteTaskRunAuthorization/Scope/Window`，`:5-7`
- Javadoc-only old facade import：`CloudTaskServicePort`，`:4,617-622`
- old getters/revalidation：`getScope`、`getStopEpoch`、`getRunRevision`、`getPlayerIdentityEpoch`、`revalidate`，见 `:98-160,194-348,393-442`

`BagWorkflowState`：

- executable imports/fields/API：`RemoteTaskRunAuthorization/Scope/Window`，`:4-6,60-63,101-130`
- exact-run check 直接调用 old `getScope/getStopEpoch/getPlayerIdentityEpoch/getRunRevision/revalidate`，`:607-623`
- non-mintable `VerifiedTransactionPermit` 与 `FinalConsumptionPermit` 仍按 old scope/window/stop/transaction authority 建模，`:759-828`
- JavaDoc 仍要求未来 `.remote` retained authority/ledger 与 `TaskTransactionAction` mint permit，`:759-765,791-807`

TURN-38A 报告 `:145-155` 已精确列出这些后继 caller，并指出若 38A 先删除 old API，B1 尚未改写时 Cloud main source 会立即失去符号。

### 7.2 turn-native 可用但尚未冻结给 B1 的来源

Cloud `TaskExecutionContext.java` 当前 SHA-256 为
`6D4E4A20A6FB4B6DBA6A59CB45E95DD39C78A0415B9B2A650D75F9704151D003`。可见候选来源如下：

| state identity | 现有 turn-native source | 证据 |
|---|---|---|
| tenant/user | `getTurnServiceScope()` | `TaskExecutionContext.java:229-236`；`CloudServiceScope` 只含 tenant/user |
| device/window | `getTurnInvocationContext()` | `:238-244`；`TurnInvocationContext` 只含 device/window |
| logical window | `getWindowId()` | `:132-135` |
| initial native fingerprint | `getNativeWindowTitle/Handle/ProcessId` | `:142-161` |
| in-memory task identity | `getTaskRunId()` | `:189-192` |
| current pause/stop | bound `TurnGameClient.latestWindowMetadata()` | `:278-306,392-429` |
| business task/role/team/retry/startup/time | `CloudTaskServiceMetadata` | record fields；它的 JavaDoc 明确不授予 lifecycle/window authority |

`TurnWindowMetadata` 只有 device/window/title/nativeHandle/processId/rect/pause/stop；没有
`playerIdentityEpoch`、`stopEpoch` 或 `runRevision`。B1 必须移除旧三项依赖，不能用新名字重建同类 retained authority。

上述只是现有 source inventory，不是本 helper 对 key 或 construction contract 的冻结。

## 8. 必须由父级区分的两种生命周期

当前源码与 `STATE` profile 存在需要显式拆分的语义：

1. **per-task-run workflow state**  
   open stage、page cursor、session ordinal、frozen point、pending observation 应在 pause/resume 保持同一对象；terminal/restart 应释放旧 run state。
2. **per-scope/window Bag hints**  
   旧 `CloudBagStateOwner.java:35-39,335-343` 明确 visible/item hints 不加 TTL、跨 task terminal 与 binding change 保留；anchor 只在 native tuple/geometry generation 失配时失效。

计划 `:1497` 的“terminal/restart release”没有写清是仅释放 workflow，还是连 host/window cache 一起释放。若把二者合成一个 terminal clear，会改变旧 cache lifetime；若全部保留，又不能满足 workflow restart fresh。worker 不得临场选择。

同样需要父级冻结：

- terminal event 集合是 SUCCESS/FAILED/STOPPED，还是还包含 unregister/host close；
- restart 的新 `taskRunId` 是否只新建 workflow，cache 是否复用；
- title/handle/process drift 是否只失效 geometry/anchor，还是替换整个 state；
- windowRect/pause flag 变化不得意外形成新 logical key；
- scope teardown 与 task terminal 必须是两个不同 API/事件；
- 不得增加 TTL、定时 cleanup、LRU、durable storage、compaction、自动 retry 或 transport-driven business reset。

## 9. named test 的 exact 验收边界

### 9.1 pause/resume

唯一 named test 应通过最终 production construction API 获得 owner/state，不以 reflection、test-only registry 或复制 reducer 作为主要证明。

必须断言：

1. 在一个 exact `CloudServiceScope + TurnInvocationContext + taskRunId` 上推进 open stage、page cursor、session ordinal、frozen point 与 pending slot 中最终保留的字段。
2. fake latest metadata 切到 `pauseRequested=true` 时，checkpoint 等待但不新建 state、不推进 cursor、不重抽随机点、不发 UUID/command。
3. 切回 `pauseRequested=false` 后得到同一个 workflow object，所有 cursor/value 保持。
4. pause 中 `stopRequested=true` 形成 typed stop，并按父级冻结的 terminal hook 只释放一次。
5. state/owner lock 不跨越 250ms pause poll、port wait、callback 或 I/O；否则同 window control/terminal 会被锁死。

### 9.2 terminal/restart

必须断言：

1. SUCCESS、FAILED、STOPPED 各自经过同一个 production lifecycle API；release 幂等。
2. terminal 后旧 state/handle/reference 不能再写；同 key 新 taskRunId 得到 fresh workflow。
3. 新 workflow 不继承旧 open/page/session/frozen/pending 状态。
4. visible/item/anchor/geometry 是否保留，严格按父级对“workflow vs cache lifetime”的单独冻结，不得由 test 猜测。
5. 不使用时间推进证明释放；无 TTL。
6. `startRequestId` 去重与 runtime/unregister 的真实 wiring 属 TURN-40B `LIFE` test。B1 test 可以验证其 public release contract，但不能用 test-only fake runtime 冒充后继集成已闭合。

### 9.3 scope isolation 与 stale reject

至少覆盖：

1. tenant/user 不同但 device/window 相同，互不可读写。
2. tenant/user 相同但 device/window 不同，互不可读写。
3. request body 或 mutable metadata 不能选择 tenant/user；scope 只能来自 host-bound `CloudServiceScope`。
4. wrong device/window、foreign state、stale taskRunId、native fingerprint drift 在第一次 mutation 前拒绝。
5. pause flag或 windowRect 变化不产生第二 logical state。
6. 若保留 geometry stream，继续证明 observation sequence 的 `< / == / >` 接受规则、generation invalidation 与同序幂等；不得把 sequence 变成 TTL/retry count。
7. visible/item page 仍只缓存 `0..4`，page 5 不写 cache；canonical template key 与 first-scan-hint 语义不变。

### 9.4 测试隔离

- 只允许 fake `TurnGameClient`/metadata，不启动 server/application/Task/runtime，不触发 capture/input/OCR。
- state test 应断言 command/UUID count 为 0。
- 静态 old-symbol 零引用可作补充，但不能替代真实 production API 的状态行为断言。
- 本 preflight 按用户要求未运行 `C(BagWorkflowStateTurnTest)`、Maven、JUnit 或 compile。

## 10. 与其它 lane 的写集关系

| lane/card | physical overlap | 真实关系 |
|---|---|---|
| External B TURN-28P Repair #2 | 无 | active Java writer；不应与本 helper并发 clean/build |
| External C TURN-34A | 无 | `AutoCombatService.java` + named test；当前 resumed active |
| TURN-38B2 | 无 | 两个 `service/returnitem` 文件；共享 38A predecessor，但 state key/lifecycle 口径应一致 |
| TURN-38B3 | 无 | startup authority + Service；共享 38A predecessor |
| TURN-38B4 | 无 | artifact store/configuration；共享 38A predecessor |
| TURN-38A | 无直接文件重叠 | 强 API/compile 顺序冲突：38A 要删 B1 当前调用的 old context API |
| TURN-35/36/37 | 无直接文件重叠 | 前置 whole Tasks 可能成为 B1 首批 caller；当前尚未交付，必须在其 source stable 后重扫 |
| TURN-39 | 无直接文件重叠 | B1 当前仍 import/Javadoc 引用 `CloudTaskServicePort`；39 删除 old facade 前 B1 必须清零 |
| TURN-40B | 无直接文件重叠 | 后继 runtime 是 production lifecycle consumer；因 40B 不能回写 B1，两文件的 construction/release API 必须先冻结可消费 |
| TURN-44A | 无当前写集重叠 | 最终 old authority SCC 删除依赖 B1 不再引用 `RemoteTaskRun*`/permit authority |
| DHXY BagService | 禁止重叠 | 永久本地 mechanics/cache owner；B1 不得修改或复制其 capture/input 实现 |

物理冲突目前为 0，不等于依赖/API 冲突为 0。

## 11. REAL_START_BLOCKER 事实

这些是可复验事实，不是本 helper 对卡片状态的裁决：

| id | 事实 | 开工前必须闭合的证据 |
|---|---|---|
| B1-DEP | direct `S=38A` 未满足；38A 又等 34C/35/36/37 | 父级记录 predecessor source-stable 结果并重新扫描 |
| B1-ORDER | 38A 要删除 old getters/delegate，但两个 B1 文件仍编译级调用；B1 又被排在 38A 后 | 父级冻结可编译的串行/原子 cohort 或精确 compatibility shell，不能让 worker猜 |
| B1-OWNER | Cloud owner constructor private、无 bean/factory、无 production/test caller | 冻结唯一 construction owner、lifetime 与防第二 owner 规则 |
| B1-CONSUMER | 35/36/37 尚未提供最终 caller，40B runtime 尚不存在 | predecessor 交付后列出全部 caller；冻结 B1 public API 给后继使用 |
| B1-LIFE | workflow terminal release 与 cache terminal persistence 未区分 | 冻结两类 state 的 terminal/restart/scope-teardown事件矩阵 |
| B1-SCOPE | tenant/user/device/window/native fingerprint 的 exact key 与 rebind 规则未写入卡合同 | 冻结 key、stale fence、geometry invalidation 与 host-scope来源 |
| B1-AUTH | 当前 mutation 全靠 non-mintable old permit/retained authority/ledger；当前协议禁止重建该体系 | 冻结不含 permit/session/ledger 的替代 public mutation/lifecycle API |
| B1-TEST | 唯一 named test 不存在，且当前没有 production construction/release seam 可供它真实调用 | 冻结 test path、owner 与 production API 后再创建测试 |
| B1-WRITESET | 若方案需要改 Task/context/runtime/config/protocol/client 或既有 tests，现有两 production + 一 test 写集不足 | 父级先修计划，不能由 B1 worker扩写 |
| B1-DIRTY | 两个核心文件均为未跟踪文件，且共享树有活动 writers | claim 前重核 full status、SHA、mtime、引用和唯一 owner |

## 12. 父级开卡前冻结清单

1. 确认 TURN-14 source contract 仍稳定，并让 TURN-38A 及其 34C/35/36/37 source predecessor 达到可消费状态。
2. 对 38A/B1 的编译顺序只冻结一种方案：重排、原子 cohort，或精确 source-compatible shell；同时固定 38A old-symbol test 的 allowlist。
3. 在两文件内冻结唯一 owner construction：例如由现有 host component scan 创建一个 scope-bound state container，或由未来 40B 通过受控 factory 获取；二者只能选定一种，不能产生 per-call/第二 owner。
4. 冻结 owner 是否构造绑定 `CloudServiceScope`，以及 API 如何只接受 exact context 而不允许 caller 传任意 tenant/user/raw key。
5. 冻结 logical key：tenant/user、device/window、taskRunId、native title/handle/process、geometry generation 各自属于哪一级 state；明确 rect/pause 不是 key。
6. 冻结 workflow state 与 visible/item/anchor/geometry cache 的两套 lifetime；逐项列 SUCCESS/FAILED/STOPPED/restart/unregister/host-close 的 release 或 retain。
7. 冻结 public API：state obtain/reuse、mutation、terminal release、scope teardown、stale reject；明确删除 old `RemoteTaskRun*`、`revalidate`、`CloudTaskServicePort` 与 permit/ledger seam。
8. 在 35/36/37 source stable 后重跑 production/test caller scan，列出真实 creator、holder、terminal caller；不能沿用当前 0-caller 快照。
9. 冻结唯一 test 文件的物理路径和 assertion matrix；不得修改 13C、38A、14 或 whole-task tests 来替代 B1 自己的证明。
10. 若任一冻结结果需要两 production 文件及唯一 named test之外的路径，先由父级修订第 17/19 节。
11. claim 前再次记录两目标文件 SHA/mtime/status，保护全部 dirty/untracked，并确认没有第二 writer。
12. 卡内写明：`无已批准业务差异；按 696a12b0 基线等价迁移`，并禁止 TTL、额外 retry/read/verification、自动 cleanup、session、ledger、durable workflow 或新的业务 truth。

## 13. 只读执行记录

- 只读使用了 `Get-Content`、`rg`、`git status`、`git branch --show-current`、`git rev-parse HEAD`、`Get-FileHash`、`Get-Item`、`Test-Path` 与 `Get-Date`。
- 未运行 Maven、JUnit、compile/package。
- 未启动 runtime/application/server/Task/UI/capture/input。
- 未执行 add/commit/checkout/reset/clean/stash/rebase/merge 或任何 Git mutation。
- 未修改 Java、测试、计划、CR271、ACTIVE_WORK 或其它报告。
- 本轮唯一写入是本 PRECHECK 报告。

PRECHECK_COMPLETE
<!-- TRUE_EOF: CR271 TURN-38B1 READINESS PREFLIGHT PRECHECK_COMPLETE -->
