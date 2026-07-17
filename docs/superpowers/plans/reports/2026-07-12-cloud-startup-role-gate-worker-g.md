# Worker G：Cloud-native startup role gate

## Parent Task Brief #1 - 2026-07-12

### 目标

为 Cloud 侧后续 `AutoBattleTask` / `FiveRingTaskV2` cohort 设计可实施的 startup role gate，保持 DHXY
`TaskStartupCheckService` 的准入语义，同时明确把本地 hover/面板截图采集留在 thin client。首轮只做设计和源码事实盘点；
父级写入 `DESIGN APPROVED` 前不得修改 Java、resources、pom 或测试。

### 必读与基线

- `D:\mavenProject\DHXY\AGENTS.md`
- `D:\mavenProject\DHXY\docs\DHXY_CONTEXT.md`
- `D:\mavenProject\DHXY\docs\业务逻辑.md`
- `D:\mavenProject\DHXY\docs\ACTIVE_WORK.md` 顶部 CR271
- `D:\mavenProject\DHXY\docs\superpowers\specs\2026-07-12-service-migration-matrix.md`
- DHXY HEAD 的 `TaskStartupCheckService`、`TaskStartupCheckResult`、`TeamTaskProperties`、
  `TeamRoleDetectionService`、`TaskExecutionContext`
- Cloud 当前的 `TaskTeamAssignmentPolicy`、`TaskStartupCheckResult`、Cloud `TaskExecutionContext` 和 remote authority/context

### 设计不变量

1. `checkFiveRing`：`fiveRingRequiresLeader=false` 直接 allow；启用时只基于可信 role fact 按原
   `shouldRunFiveRing` 语义 allow/skip。不得在 Cloud 执行 hover、面板截图、OCR 或窗口访问。
2. `checkAutoBattle`：`autoBattleRequiresMember=false` 直接 allow；启用时 MEMBER allow、LEADER skip、UNKNOWN
   严格按 `allowAutoBattleWhenRoleUnknown`；不得为了判断身份触发实时采集。
3. Cloud 不得复制 `TeamRoleDetectionService`、HWND、`WindowRuntimeContext`、holder、tracker、input、Path 或本地配置 bean。
4. 配置必须有一个明确的 Cloud 权威来源和 tenant/user 隔离；不得靠硬编码当前默认值掩盖可配置合同，也不得引入
   Spring Boot `ConfigurationProperties`（Cloud 当前不是 Boot）。
5. role fact 必须绑定 exact tenant/user/device/session/taskRun/window/stopEpoch/runRevision，并在调用时由当前
   `TaskExecutionContext.revalidate()` 兜底；不得接受裸 role string + 裸 taskRunId 形成可伪造 bypass。
6. 不开放 public raw request/poll/outcome/path；host/cohort 保持 dormant；不新增 TTL/retry/fallback/线程/测试。
7. 与 Worker F 的 `BaseTaskTemplate` 写集完全分离；本切片不得修改 template、checkpoint、remote broker、host 或 DHXY Java。

### Design #1 必须交付

- 逐方法兼容表：本地输入/依赖/输出/日志与 Cloud 替代来源，注明每条条件与 fallback 是否逐句保持。
- 现有 Cloud context 能否承载可信 role/config；不能时列最小新 value type/API 与非 public mint owner。
- tenant/user 配置 authority、默认值、更新/读取时序、容量和运维方式；不允许双重配置权威。
- exact 文件写集、构造/依赖图、future caller 映射；说明为何不会与 Worker F/B/A 写集重叠。
- stale revision、错 tenant/window、缺 role、配置缺失、暂停/停止、断线/重放威胁矩阵及 fail-closed/基线行为。
- 推荐方案、备选方案和所有未决 P0/P1/P2。若实现需要改变任何 baseline 分支，必须 BLOCKED，不得自行选择。

### Worker 规则

- 你不是 reviewer；自审只算 QA。只向本文件追加 `External/Internal Worker G - Design #1`，不得改已有段落。
- 你不是独自在仓库工作；保护全部 dirty/untracked，不回滚、不覆盖、不提交，不修改其它 Worker 报告。
- 不运行 Maven/测试，不启动 application/server/host/poller/UI/capture/OCR/input。

## Internal Worker G - Design #1 - 2026-07-12

### 0. 角色、范围与停止门

- 本轮仅完成源码事实盘点和 Cloud-native startup role gate 设计；不是 reviewer，自审不构成批准。
- 本轮唯一写入是本报告。未修改 Java、resources、pom、tests、CR 卡或 dashboard；未运行 Maven/测试；未启动
  application/server/host/poller/UI/capture/OCR/input；未执行 Git mutation。
- 父级未在本报告明确写入 `DESIGN APPROVED` 前，Java/resources/pom/tests 全部冻结。
- 设计目标只覆盖未来 Cloud `AutoBattleTask` / `FiveRingTaskV2` 初次启动时的准入门；不迁任务主体，不装配 host，
  不改变 resume/rehydration、phase、retry、fallback、park/yield 或输入/验证顺序。
- **无已批准业务差异；按基线等价迁移。**

### 1. 必读基线与两仓现状

#### 1.1 已完整读取

- 固定报告的 `Parent Task Brief #1`。
- `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/业务逻辑.md`、`docs/ACTIVE_WORK.md` 顶部 CR271。
- `docs/superpowers/specs/2026-07-12-service-migration-matrix.md` 全文，重点核对
  `TaskStartupCheckService`、`TaskStartupCheckResult`、`TaskTeamAssignmentPolicy`、`AutoBattleTask`、
  `FiveRingTaskV2`、context/remote authority 与 host/config 阻塞条目。
- DHXY HEAD 的 `TaskStartupCheckService`、`TaskStartupCheckResult`、`TeamTaskProperties`、
  `TeamRoleDetectionService`、`TaskExecutionContext`，以及两个真实 caller 和 runner role-preflight 调用链。
- Cloud 当前的 `TaskTeamAssignmentPolicy`、`TaskStartupCheckResult`、Cloud `TaskExecutionContext`、
  `CloudTaskServiceMetadata`、`CloudTaskServiceExecutionContext`、`CloudTaskRunExecutionContext`、
  `CloudTaskRunAuthorityAssembly`、`CloudTaskRunExecutionGate`、`RemoteTaskRunCoordinator`、scope/window binding 与
  dormant host/config 现状。

#### 1.2 Git/dirty 快照

| 仓库 | 分支 / HEAD | dirty 事实 | 本切片处理 |
|---|---|---|---|
| DHXY | `thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f` | 大量用户/并行 dirty 与 `docs/superpowers/plans/reports/**` untracked；本切片涉及的 startup/context/caller 源文件 scoped status 均为空 | 全部保护；只追加本报告，不改 DHXY Java |
| Cloud Brain | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | `pom.xml`、Cloud Brain 主类有 dirty，`com/bot/**`、`remote/**`、`host/**` 等大量并行 untracked | 全部保护；两个推荐 new target 当前均不存在；批准前不写 |

DHXY 相关 HEAD-clean 源码 SHA-256：

- `TaskStartupCheckService.java`：`F62C9074313C39F333FFB670BA1588AD952B6EF2AFC747F8D7CED751D4E51BB3`。
- `TaskStartupCheckResult.java`：`691E567E581E7480E28135CD49B47CD05A18901A35EFD99DB8AFDD42B7295AB0`。
- `TeamTaskProperties.java`：`037A1E24B961F1E06280163B19CC19CB84E82BC591769A1FBF9FE437F8142816`。
- `TeamRoleDetectionService.java`：`D729778704C71B4569304E1BD3A1F879BEE4B56A3E2AA098A3FB0C2AED190B5D`。
- `TaskExecutionContext.java`：`1BD6AAC819449A557C94FE8BD0A258254E99AE5F2E95BCF386B408133219BB88`。

Cloud `TaskStartupCheckResult.java` 与 DHXY 源 SHA-256 完全相同；Cloud configless
`TaskTeamAssignmentPolicy.java` 已由 Worker E 获父级 `APPROVED`，本设计只消费其既定 effective-task 结果，不修改它。

#### 1.3 当前事实结论

1. DHXY `checkFiveRing` 在门开启时会调用 `TeamRoleDetectionService.detectCurrentRole(context)`；该 Service 的
   1318 行实现持有 hover、Alt+T、截图、OCR、HWND/window holder/tracker/input/`Path` 和本地配置，绝不能复制到 Cloud。
2. DHXY `checkAutoBattle` **刻意不做实时检测**；只读 `context.windowRole`，且仅把 `MEMBER`/`LEADER` 识别为
   对应身份，`SOLO`、null、未知字符串均投影成 `UNKNOWN`。
3. `TeamTaskProperties` 当前四个相关默认值为：
   `fiveRingRequiresLeader=false`、`autoBattleRequiresMember=false`、
   `allowFiveRingWhenRoleUnknown=true`、`allowAutoBattleWhenRoleUnknown=true`；resources 未覆盖这些值。
4. Cloud 不是 Spring Boot，仅有 `spring-context`；此前 exact-copy `TeamTaskProperties` 已因
   `ConfigurationProperties`/Validation 缺失撤回。不得以新增 Boot 依赖解决。
5. Cloud `TaskExecutionContext` 已有 exact scope、taskRunId、taskType、window tuple、stopEpoch、runRevision 和
   `revalidate()`；其构造链最终受 package-private `CloudTaskRunAuthorityAssembly` 控制，外部不能只凭 metadata
   构造有效 context。
6. Cloud context 的 `windowRole` 是不可公开铸造 context 内的 immutable business metadata，但它没有独立的
   exact role-fact value type，也不承载 tenant/user 配置。当前 Task host/concrete Task/activation adapter 均不存在，
   因而该 metadata 仍不可达，不能据此宣称 cohort 已可激活。

### 2. 逐方法兼容表

#### 2.1 `TaskStartupCheckService`

| 源方法 | DHXY 输入/依赖 | DHXY 条件、输出与日志面 | Cloud 替代来源 | 等价结论 |
|---|---|---|---|---|
| 构造器 `(TeamTaskProperties, TeamRoleDetectionService)` | Boot 配置 bean + 本地实时识别 Service | 只保存依赖，无业务输出 | package-private `CloudStartupGateAuthority` + 该 authority 铸造的 exact role fact | 构造依赖 Cloud-native 化；不复制配置 bean/检测 Service。public check API 不变 |
| `checkFiveRing(context)` | `fiveRingRequiresLeader`；开启时 `detectCurrentRole` + `shouldRunFiveRing(role)` | 门关：直接 `allow(role=UNKNOWN,"role gate disabled")`。门开：LEADER allow；UNKNOWN 仅在 `allowFiveRingWhenRoleUnknown` 时 allow；MEMBER/SOLO skip。Service 自身不打业务日志，caller 记录 reason | 一次 immutable tenant/user policy snapshot + 与 context exact 绑定的 role fact；Cloud 不采集画面 | **逐分支保持**。门关不读取 role。门开严格复刻 `shouldRunFiveRing`，无新 fallback/检测/TTL/retry |
| `checkAutoBattle(context)` | `autoBattleRequiresMember`；`roleFromContext`；`allowAutoBattleWhenRoleUnknown` | 门关：直接 allow UNKNOWN。门开：MEMBER allow；LEADER skip；其余全部 UNKNOWN，再按 unknown 开关 allow/skip；绝不实时检测 | 同一 policy snapshot + exact role fact 的 auto-battle 投影 | **逐分支保持**。特别保留 `SOLO -> UNKNOWN`，不能误用 `shouldRunAutoBattle(SOLO)` 改掉 unknown-allow 语义 |
| `roleFromContext(context)` | nullable context + nullable/string `windowRole` | case-insensitive MEMBER/LEADER；其余 UNKNOWN | authority 在 role fact 铸造时解析一次；保留原始 full role 供五环，另提供 auto-battle projection | 条件保持；不新增裸 role 入参或 public parser |
| `buildReason(context,taskName,role,message)` | context log prefix；nullable role | null context 用 taskName；null role 用 UNKNOWN；字符串格式 `prefix + " | role=" + role + " | " + message` | Cloud exact context `getLogPrefix()` | 字节级格式与英文 message 保持；不把 policy revision/tenant 写进业务 reason |

`checkFiveRing` 逐条件真值表：

| requiresLeader | role | allowUnknown | 结果 |
|---|---|---|---|
| false | 不读取 | 任意 | allow |
| true | LEADER | 任意 | allow |
| true | UNKNOWN | true | allow |
| true | UNKNOWN | false | skip |
| true | MEMBER / SOLO | 任意 | skip |

`checkAutoBattle` 逐条件真值表：

| requiresMember | context role | 投影 role | allowUnknown | 结果 |
|---|---|---|---|---|
| false | 不读取 | UNKNOWN（仅 reason） | 任意 | allow |
| true | MEMBER | MEMBER | 任意 | allow |
| true | LEADER | LEADER | 任意 | skip |
| true | SOLO / null / 非法字符串 / UNKNOWN | UNKNOWN | true | allow |
| true | SOLO / null / 非法字符串 / UNKNOWN | UNKNOWN | false | skip |

#### 2.2 `TaskStartupCheckResult`

Cloud 已 exact-source copy，SHA 与 DHXY 相同，本切片零改：

| 方法组 | 兼容性 |
|---|---|
| `allow(reason)` / `allow()` | 仍为 `allowed=true`、`blockedResult=SUCCESS`；默认 reason `允许执行` |
| `skip(reason)` | 仍为 `allowed=false`、`blockedResult=SKIPPED` |
| `fail(reason)` | 仍为 `allowed=false`、`blockedResult=FAILED`；startup role gate 正常分支不新增该结果 |
| `stop(reason)` | 仍为 `allowed=false`、`blockedResult=STOPPED`；Cloud lifecycle deny 不伪装成该业务结果 |
| `isAllowed/isBlocked/getBlockedResult/getReason` | 原样；null blockedResult 仍归一 SKIPPED，blank reason 仍归一 `-` |

#### 2.3 不复制的 `TeamRoleDetectionService` 语义投影

| 本地方法/事实 | Cloud 处理 |
|---|---|
| `detectCurrentRole(...)` 及全部 hover/panel/OCR fallback | 整体留 thin client；Cloud 无方法、无 dependency、无模板/ROI/输入副本 |
| `shouldRunFiveRing(role)` | 仅将其 4 条纯布尔条件写入 Cloud gate；不调用/复制 detector 类 |
| `shouldRunAutoBattle(role)` | **不作为 `checkAutoBattle` 的替代**，因为本地 startup 方法先把 SOLO 投影 UNKNOWN；Cloud 按 startup 方法本身复刻 |
| detection disabled/misconfigured -> UNKNOWN | 作为 thin-client preflight 产出的 UNKNOWN role metadata；Cloud 不尝试修复或重采 |

### 3. 可信 role/config authority

#### 3.1 现有 Cloud context 能与不能承载什么

| 能力 | 现状 | 结论 |
|---|---|---|
| exact tenant/user/device/session | `TaskExecutionContext.getScope()` | 可直接使用 |
| exact taskRun/taskType/window/stopEpoch/runRevision | context projection + coordinator `revalidate()` | 可直接使用，且任何 lifecycle revision 变化使旧 context 永久失效 |
| role label | `CloudTaskServiceMetadata.windowRole -> context.getWindowRole()`；context 不可由 public metadata 单独铸造 | 可作为 trusted activation metadata 的输入，但必须由非 public owner 封成 exact role fact，不能暴露 `(role,taskRunId)` overload |
| role 采集 provenance | 当前无 Task activation adapter/caller；role label 尚无可达 producer | 属 future cohort activation 前置门；未闭合前 host/cohort 保持 dormant |
| tenant/user startup config | context 无此字段；Cloud 无 `TeamTaskProperties` | 不能承载；必须新增唯一 Cloud authority，不能回读 DHXY properties/env 形成双权威 |

#### 3.2 推荐 authority 与最小 value types

推荐新增 package-private `CloudStartupGateAuthority`，一个实例只绑定一个 `CloudServiceScope(tenantId,userId)`，并
拥有以下 private/package-private immutable value types（可作为该类底部 nested records，避免扩写公共面）：

1. `PolicySnapshot`
   - exact `tenantId/userId`；
   - monotonic `policyRevision`；
   - 四个配置布尔；
   - `source=BASELINE_DEFAULT|CONTROL_PLANE`；
   - 全量原子替换，不允许逐字段写造成混合 revision。
2. `StartupRoleFact`
   - `RemoteTaskRunScope` 全四项；
   - exact `taskRunId/taskType`；
   - exact `windowId/nativeHandle/processId/playerIdentityEpoch`；
   - exact `stopEpoch/runRevision`；
   - parsed `TeamRoleStatus`；
   - 不含 observedAt TTL，不含本地路径/截图/HWND 对象。
3. `Evaluation`
   - 同一次 check 使用的一份 `PolicySnapshot + StartupRoleFact`；
   - 仅 authority 产生，`TaskStartupCheckService` 不接受外部拼装。

authority 和 role fact 的 constructor/factory/update 均 package-private；无 public role/config mint API。对外业务面只保留：

```text
TaskStartupCheckResult checkFiveRing(TaskExecutionContext context)
TaskStartupCheckResult checkAutoBattle(TaskExecutionContext context)
```

`TaskStartupCheckService` 由 authority 针对一个 exact task-run/revision 构造，内部保存不可变 role fact；caller 不能传 role、
config、taskRunId 或 windowId 覆盖它。每次 check 的机械顺序固定为：

```text
require exact same context identity as bound role fact
context.revalidate() exactly once on the allowed path
if denied: use existing typed checkpoint only to unwind STOPPED/PAUSED/stale; never return business fail/skip
require authority tenant/user == context scope tenant/user
read one immutable policy snapshot
if task gate disabled: direct allow, do not consume role branch
else evaluate the exact baseline truth table
```

正常 allowed path 不做第二次 revalidate；后续任何机械 action 仍由既有 retained port 自己 revalidate。这样不把 role gate
变成额外业务验证循环。

#### 3.3 配置的唯一权威、默认、更新与读取

- **唯一权威**：每个 tenant/user Cloud service scope 的 `CloudStartupGateAuthority.PolicySnapshot`。禁止同时读取
  DHXY `application.properties`、环境变量、System property、Spring Boot binder 或另一张 per-task map。
- **默认 revision 0**：四个值精确采用 DHXY baseline：`false,false,true,true`。默认只定义在 authority；
  `TaskStartupCheckService` 不写任何默认常量，故默认不会掩盖可配置合同。
- **无 override 与 provider 失败严格区分**：authenticated control plane 明确返回 `NO_OVERRIDE` 时使用 revision 0；
  control-plane 读取失败/签名或 scope 校验失败时不构造 authority、不激活 cohort，不能静默退回默认。
- **更新**：future authenticated control-plane adapter 只能调用 package-private full-snapshot CAS：
  `replace(expectedPolicyRevision,nextFourValues)`；revision 不匹配/非法 scope 直接拒绝，不内部 retry。
- **读取**：每次初次 startup check 只 `get()` 一次 immutable snapshot；一次判断不会混用两个 revision。更新仅影响
  后续新 task run；已开始 run 不重新执行 startup gate。
- **pause/resume**：startup gate 不是 resume business gate。未来 rehydration 恢复已开始 run 时不得从 `execute()`
  顶部重跑本门；若创建新 initial run，必须用新 exact context/role fact 重新检查。
- **容量**：authority 是 per-host/per-tenant-user O(1) 状态，只保留当前 immutable snapshot 和一个 revision；不新增
  tenant map、history、TTL、清理线程或后台 reload。全进程 host 数量仍由 future host registry 的独立容量门负责，
  本切片不偷偷建立第二 host registry。
- **运维**：结构化日志仅记录 policy revision/source、taskCode、windowId、taskRunId/revision 和结果；tenant/user 使用
  既有 scope digest/受控审计，不输出 role screenshot、Path、token 或原始配置载荷。CAS conflict、provider failure、
  invalid role label 分别计数，不自动修复。

### 4. 推荐构造/依赖图与 future caller 映射

```text
Thin client role preflight (hover/panel/OCR/input, local only)
  -> authenticated future activation adapter
  -> TaskTeamAssignmentPolicy resolves effective task before PREPARE
  -> package-private CloudTaskRunAuthorityAssembly mints exact TaskExecutionContext
  -> package-private CloudStartupGateAuthority binds context role into StartupRoleFact
  -> per-run TaskStartupCheckService
       -> checkFiveRing(context) OR checkAutoBattle(context)
       -> context.revalidate()
       -> one tenant/user PolicySnapshot
       -> existing TaskStartupCheckResult
  -> future AutoBattleTask / FiveRingTaskV2 initial execute path
```

当前到 future 的 caller 映射：

| caller | DHXY 当前位置 | Future Cloud 位置/调用时机 | 约束 |
|---|---|---|---|
| `FiveRingTaskV2.execute(context)` | startup check 位于设置 `GameContext.RUNNING`、round state 与任何任务动作之前 | concrete Task cohort 初次 execute 的同一相对位置 | blocked 返回原 `SKIPPED`；不得触发 capture/role refresh |
| `AutoBattleTask.execute(context)` | log context 后、设置 RUNNING/启动补给/auto-combat init 之前 | concrete Task cohort 初次 execute 的同一相对位置 | 门关闭直接 allow；门开启只读 role fact |
| role assignment | `WindowTaskRunner.resolveTaskTypeBeforeStart` 本地实时检测后调用 `TaskTeamAssignmentPolicy` | future authenticated activation adapter 在 Cloud PREPARE 前调用已迁 configless policy | effective task 必须先确定；不得 requested/effective 双 run |
| resume | 本地原栈不重跑 startup check | future rehydration owner 恢复 phase，不调用本 gate | 不让配置更新变成运行中行为变化 |

本设计不创建 future activation adapter、Task host 或 concrete Task；上图最后三段在这些独立门完成前不可达。

### 5. Exact 最小文件写集与零交集证明

#### 5.1 父级批准后的推荐写集

Cloud Brain `2 new`，DHXY Java/resources/pom/tests 零改：

| 文件 | 类型 | 目的 |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\TaskStartupCheckService.java` | New | 保留两个 public check API、原 reason/result/分支；不注册 host，不依赖本地 detector/config |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\CloudStartupGateAuthority.java` | New | package-private tenant/user policy authority、exact role fact mint/validation、full-snapshot CAS；nested value types 全非 public |
| `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-12-cloud-startup-role-gate-worker-g.md` | Append | Design/批准后 implementation 证据唯一协作文件 |

明确 zero diff：现有 `TaskStartupCheckResult`、`TaskTeamAssignmentPolicy`、Cloud `TaskExecutionContext`、
`CloudTaskServiceMetadata`、全部 `remote/**`/`remote/run/**`、`host/**`、`CloudServiceConfiguration`、server/endpoint、
template/checkpoint/sleep、concrete Task、DHXY Java/resources/pom/tests。

#### 5.2 与并行 Worker 的写集关系

| Owner | 当前写集 | 与 G 推荐 Java 写集 |
|---|---|---|
| Worker F | Cloud `task/template/BaseTaskTemplate.java` + `TaskStepExecutor.java` | **零交集**；G 不 import/修改 template/executor/checkpoint |
| 外部 Worker A | Cloud `remote/**`、`remote/run/**`、endpoint/digest/error；DHXY `cloud/remote/**` | **零交集**；G 只消费 public exact context API，不编辑 authority |
| 外部 Worker B | Cloud `host/CloudArtifactStore`、`ScopedPngArtifactStore`、`CloudTemplateAssets`、`PackagedTemplateAssets`、capacity governor，及 `CloudServiceStorage/Configuration` | **零交集**；G 不改 host/storage/config，不用 artifact/Path |
| Worker E | Cloud `task/startup/TaskTeamAssignmentPolicy.java` | **零交集**；同目录不同文件，只读其 effective-task contract |

若批准实施前任一 new target 已出现，Worker G 必须停住并在本报告记录冲突，不覆盖、不合并猜测。

### 6. 威胁矩阵

| 威胁/故障 | 检测门 | 处理 | 业务语义 |
|---|---|---|---|
| stale runRevision | bound role fact 与 context exact identity 比对 + `context.revalidate()` | typed stale/newer unwind；不返回 allow/skip/fail | 旧 revision 永不复活；无新 retry/fallback |
| future/unconfirmed revision | coordinator authorization deny | typed lifecycle unwind，cohort 不推进 | 等 A executor-readiness/confirmation；不把 deny 当 role UNKNOWN |
| 错 tenant/user | authority scope 与 context scope 不同，或 coordinator scope mismatch | fail closed；不泄露 policy/role | 不读取其它 tenant 配置 |
| 错 device/session | role fact full scope mismatch或 coordinator deny | fail closed | replacement session 不能接管旧 run |
| 错 taskRun/taskType | role fact exact run/type mismatch | fail closed | 不能用 auto-battle fact 绕五环或跨 taskRun 复用 |
| 错 windowId/HWND/process/player epoch | exact window tuple mismatch | fail closed | 不接触 HWND 对象；只比对 normalized identity tuple |
| stopEpoch 变化 | fact/context mismatch或 coordinator deny | `TaskStopRequestedException` typed STOP unwind | 不映射 FAILED/SKIPPED，不执行角色分支 |
| PAUSED | coordinator checkpoint/authorization deny | typed PAUSED transition，future host park | 不把暂停当 UNKNOWN/skip，不重试 |
| STOPPED | coordinator deny + typed stop | STOP unwind | 不执行 gate 后任务动作 |
| COMPLETED | coordinator deny | typed completed/stale unwind | 不重新启动已完成 run |
| role 缺失/非法 | authority 解析为 `UNKNOWN`，仍绑定 exact context | 五环/自动战斗按各自 unknown 开关；自动战斗绝不采集 | 精确保留 baseline fallback |
| role=SOLO | fact 保留 SOLO；auto-battle 投影 UNKNOWN | 五环门开时 skip；auto 门开时按 unknown 开关 | 保留两个源方法之间的细微差异 |
| 伪造裸 role + taskRunId | public API 不接受；role fact constructor/mint owner 非 public；context 不可由 metadata 单独构造 | 编译/类型边界拒绝 | 无 bypass overload |
| 未配置 tenant override | authority 明确 `NO_OVERRIDE` | 使用 revision 0 baseline snapshot | 这是权威默认，不是 read failure fallback |
| config provider 失败/签名失败 | activation owner 无有效 seed | 不构造 authority、不激活 cohort | 不静默用默认掩盖控制面故障 |
| 并发 config 更新 | full snapshot CAS + immutable read | 一次 check 固定一个 policyRevision；CAS conflict 拒绝 | 无混合字段、无内部 retry |
| 断线 | exact clientSession/current lifecycle 仍由 coordinator 管理 | 无新 public poll/endpoint；旧 context 不能换 session | 不自动接管、不本地 fallback |
| 同请求/role fact 重放 | role fact 仅存于 per-run service，且 exact revision/window/scope 校验 | 同 exact context 的只读重复判断幂等；跨 revision/session 拒绝 | 不 mint action、不产生副作用 |
| Cloud 进程重启 | 本 slice 无 durable role/config state | cohort 保持 dormant；future activation 从可信 control plane 重新 seed，并按 lifecycle stop/new run/rehydration 合同处理 | 不冒充 crash recovery |
| 容量攻击 | per-scope authority O(1)，无 role/config map/history/thread | 无增长面；future host registry 另设容量门 | 不新增 TTL/清理策略 |

### 7. 方案比较

#### 方案 A：exact-context role fact + host-scope policy authority（推荐）

- 保留原 public check API 和全部 baseline 分支；Cloud 不碰图像/窗口/input。
- role fact 由不可公开铸造 context 投影并 exact 绑定；config 是 tenant/user 单一 authority，读取原子、可版本化。
- 写集仅 Cloud startup 两个新文件；不改 A remote、B host、F template、E policy。
- 代价是 gate 按 per-run 构造，future activation/rehydration owner 必须显式装配；这正好维持 dormant 安全门。

#### 方案 B：把 role/config 写入 `RemoteTaskRunBinding`/PREPARE（后置，不选）

- 优点：role/config 与 lifecycle binding 同 record 持久化看起来直接。
- 问题：必须修改 Worker A 的 coordinator/request/endpoint/digest/context 写集，把业务配置塞进机械 lifecycle owner；还会扩大
  resume-confirm 原子合同和 idempotency schema，当前没有批准。
- 结论：本切片拒绝。未来若业务 checkpoint 持久化需要，可另开 CR，不作为 startup gate 实现捷径。

#### 方案 C：复制 `TeamTaskProperties`/`TeamRoleDetectionService` 或直接读常量（拒绝）

- exact-copy 配置要求 Boot/Validation；detector 会搬入 HWND/holder/tracker/input/Path/OCR；硬编码常量又没有更新合同，
  会形成隐性第二权威。
- 结论：违反 Parent 不变量 2/3/4/5，拒绝。

#### 方案 D：只读 `context.getWindowRole()` 并让 public 方法接受 policy booleans（拒绝）

- 表面文件最少，但裸 role/boolean/taskRun 参数可被任意 caller 拼装，缺 tenant/user authority、policy revision 与 exact window/
  stop/revision 绑定。
- 结论：属于可伪造 bypass，拒绝。

### 8. 未决项、QA 结论与批准后门禁

#### 8.1 Design #1 自审

- `P0=0`：无业务基线分支变化，无 public raw bypass，无 local window/input authority 搬云。
- `P1=0`：role exact binding、tenant/user config authority、provider-failure 与 default 区分、pause/stop/stale、构造可达性和
  并行零交集均已给出完整设计。
- `P2=0`：SOLO/UNKNOWN 细微差异、reason 格式、配置更新时序、容量/运维和 future caller 位置均已明确。
- 该结论仅是 Worker G QA：**DESIGN READY FOR PARENT REVIEW**，不是 `DESIGN APPROVED`。

非 severity 的 activation 前置门：

1. Future authenticated activation adapter 必须把 thin-client preflight role 作为 trusted metadata 交给既有
   package-private context assembly；未实现前 gate/Task cohort 不可达。
2. Future control-plane adapter 必须明确产生 `NO_OVERRIDE` 或可信 initial snapshot；provider failure 不得激活。
3. Future Task host/rehydration owner 必须保证 startup gate 只在新 run 初次 execute 调用，resume 不从方法顶部重跑。
4. Worker F BaseTaskTemplate、Worker A resume-confirm、Worker B artifact/template 与 concrete Task dependency closure 各自通过前，
   `AutoBattleTask`/`FiveRingTaskV2` cohort 继续 dormant。

#### 8.2 父级批准后的实现门

1. 仅写 §5.1 的 Cloud `2 new` 与本报告追加；发现并行冲突立即停止。
2. imports/源码零命中：`TeamRoleDetectionService`、`TeamTaskProperties`、`WindowRuntimeContext`、holder、tracker、
   HWND/JNA、input、capture/OCR、`Path`、Spring Boot `ConfigurationProperties`、remote raw broker/request/poll/outcome。
3. `TaskStartupCheckService` 不提供 `(role,taskRunId)`、config booleans、public fact/policy constructor 或 public update API。
4. 两个 gate-disabled 分支必须在 role 条件前直接 allow；auto-battle 不得调用任何 detect 方法。
5. `TaskStartupCheckResult`、`TaskTeamAssignmentPolicy`、context/remote/host/template/checkpoint/DHXY 全部零 diff。
6. 不新增/恢复/修改测试；不启动 application/server/host/poller/UI/capture/OCR/input。
7. Java 实现后按项目门禁由实施 Worker 运行 Cloud fresh `mvn -q clean package`（不 skip）并记录现有 suite 结果；
   本 Design #1 阶段未运行 Maven/测试。

### 9. 父级决策点

- `DESIGN APPROVED`：授权 Worker G 严格按 §5.1 的 Cloud `2 new` 实施，并执行 §8.2 门禁。
- `BLOCKED`：请在本报告追加具体 P0/P1/P2、受影响方法和精确返修条件；Worker G 不自行扩大写集。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker G - Implementation #1 - 2026-07-12

### 1. 授权、写集与当前状态

本轮依据紧邻上文的 `Parent Design Review #1 - DESIGN APPROVED` 实施，Java 写集严格为 Cloud Brain `2 new`：

| 文件 | 实施结果 | SHA-256 / 行数 |
|---|---|---|
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\CloudStartupGateAuthority.java` | 新增 package-private tenant/user policy authority、exact-context role fact 与 immutable per-run evaluation | `C2D135597369B47BCDAE7ABD6EB63B9272EDEAE89388E8E999A4F406FCF84E3D` / 274 |
| `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\TaskStartupCheckService.java` | 新增无 Spring/host/caller 的 Cloud-native startup role gate | `289E3930E6CF3A935443A41CAEA02A70377AF9ECF10B521093AF56A0856638B1` / 116 |

除本固定报告追加外，没有修改 DHXY Java/resources/pom/tests。没有修改 Cloud context/remote/host、
`TaskTeamAssignmentPolicy`、`TaskStartupCheckResult`、concrete Task、template/checkpoint 或 tests；没有覆盖并行 dirty。

### 2. 父级覆盖条件落实

| 父级条件 | 实施证据 |
|---|---|
| baseline 只能由 future adapter 明确 `NO_OVERRIDE` 后 seed | authority 无 public/default constructor；唯一 baseline 入口为 package-private `seedBaselineNoOverride(CloudServiceScope)`，revision 固定 `0`；另有 package-private authenticated control-plane seed，provider failure 时没有可构造 authority/evaluation 的 fallback |
| role fact 只来自 exact context | 唯一 package-private `bind(TaskExecutionContext)` 从同一对象投影 scope、taskRunId、taskType、windowId、native handle/process、playerIdentityEpoch、stopEpoch、runRevision、windowRole；无裸 role/run/window factory 或 overload |
| service 只收 package-private immutable `Evaluation` | 字节码确认唯一构造器为包级 `TaskStartupCheckService(CloudStartupGateAuthority$Evaluation)`；service 只有一个 final evaluation 字段，无 public producer |
| 每次 check 一次 typed current gate 后 exact compare | 两个 public check 均只经 `requireCurrentContext`；源码合计仅一处 `throwIfStopRequested()` 调用点，每次 check 恰好执行一次，随后 `Evaluation.requireExactContext` 做 tenant/user + 完整 role-fact 字段比较；`revalidate` 命中 `0` |
| gate disabled 不读取 role 条件 | 两个 disabled 分支均在 current/exact gate 后立即 `allow`，固定 reason role 为 `UNKNOWN`；分支前不调用 `evaluation.role()`、不做 leader/member/unknown 条件判断 |
| authority/value/mint/update 非 public | `CloudStartupGateAuthority`、`Evaluation`、policy/fact/source、seed/bind/replacePolicy 全非 public；`javap -p` 显示 authority class 与全部入口均为 package-private/private |
| 不接 Spring/host/caller | 无 Spring annotation/import；未新增 bean、configuration、server、host、endpoint、caller 或 producer，cohort 继续 dormant |
| full snapshot update | `replacePolicy` 对 immutable complete snapshot 做单次 `AtomicReference.compareAndSet`；revision 必须递增，冲突直接拒绝，无 retry/history/thread/TTL |

静态禁用项扫描 `0` 命中：local `TeamRoleDetectionService`/`TeamTaskProperties`、`WindowRuntimeContext`/holder/tracker、
HWND/JNA、input、capture/OCR、`Path`、Spring。没有复制或引用本地运行时、窗口、输入和文件路径 authority。

### 3. 逐方法兼容结果

| Cloud 方法 | DHXY 兼容合同 | 实施结果 |
|---|---|---|
| `checkFiveRing(TaskExecutionContext)` | gate off 直接 allow；gate on 时 LEADER allow，UNKNOWN 由 `allowFiveRingWhenRoleUnknown` 决定，MEMBER/SOLO skip | 原样保留；不检测 live role |
| `checkAutoBattle(TaskExecutionContext)` | gate off 直接 allow；gate on 时 MEMBER allow、LEADER skip、SOLO 投影 UNKNOWN，UNKNOWN 由 `allowAutoBattleWhenRoleUnknown` 决定 | 原样保留；不检测 live role |
| `requireCurrentContext(TaskExecutionContext)` | typed stop/current gate 后 exact identity/revision 验证，不产出 startup result | 一次 `throwIfStopRequested()`，随后 exact compare；异常原样 unwind |
| `shouldRunFiveRing(TeamRoleStatus)` | LEADER true；UNKNOWN 按开关；其余 false | 原样保留 |
| `autoBattleRole(TeamRoleStatus)` | MEMBER/LEADER 保留；SOLO/null/其它均为 UNKNOWN | 原样保留 |
| `buildReason(...)` | `<context.getLogPrefix()> | role=<ROLE> | <message>`；null context fallback 仍保留 | 格式和英文 message 原样保留 |

结果对象继续直接使用既有 `TaskStartupCheckResult.allow/skip`，该文件零改。reason message 保持：
`role gate disabled`、`current role should skip five-ring`、`allowed`、`allowed by preflight role`、
`leader should skip auto-battle`、`allowed because live role detection is skipped`、
`role unknown and live role detection is skipped`。

### 4. 可信 authority、隔离与 exact gate

- Policy authority 按 `CloudServiceScope(tenantId,userId)` 构造；bind 时拒绝不同 tenant/user，evaluation 复核 policy scope。
- Role fact 不对外暴露且不可裸造；只绑定一个 exact `TaskExecutionContext`，检查 task run/type、window tuple、process/player epoch、
  stop epoch、run revision 与 normalized role，跨 tenant/run/window/revision 复用会 fail closed。
- Policy snapshot 与 role fact 均 immutable；evaluation 固定一次 policy revision 和一次 exact context 投影。
- 本 slice 不建立 host/caller，因此没有伪造 public role/config 的入口，也不声称 control plane 或 preflight 已接通。

### 5. 写集、依赖与 caller 结论

依赖方向保持单向：future authenticated activation adapter -> package-private seed -> package-private `bind(exact context)` ->
package-private service constructor -> public startup checks。当前没有实现箭头起点的 adapter/host/caller；concrete Tasks 零改，代码保持 dormant。

与并行写集仍零交集：Worker F `BaseTaskTemplate`/executor、外部 A remote/context assembly、外部 B host/artifact/config、
Worker E `TaskTeamAssignmentPolicy` 均未编辑。受保护相邻文件实施前后 SHA-256 一致：

- `TaskStartupCheckResult.java`: `691E567E581E7480E28135CD49B47CD05A18901A35EFD99DB8AFDD42B7295AB0`
- `TaskTeamAssignmentPolicy.java`: `1685661DB94F2BCC90B6E2D94D04D0AFE1EA46A17CC2FDC60B07E35C9B233375`

### 6. 构建与完整性证据

- 命令：在 `D:\mavenProject\dhxy-cloud-brain` 运行 `mvn -q clean package`，未使用 skip。
- 结果：exit `0`，耗时 `74.8s`；Surefire `4` suites / `21` tests，failures `0`、errors `0`、skipped `0`。
- 可运行 JAR：`target/dhxy-cloud-brain-0.1.0-SNAPSHOT.jar`，119,523,420 bytes，SHA-256
  `2CCDAF27EB7EBCF4F4C2328F365C85FBAB0C57338383F396D170511A7B875A96`。
- 未新增、恢复或修改测试；只运行仓库现有 package 门禁。
- 未启动 application/server/host/Task/poller/UI/capture/OCR/input；未执行 Git add/commit/reset/checkout。

### 7. Implementation #1 QA 与停点

- `P0=0`：无 public bypass、无跨 tenant/window/revision 复用、无本地 role/config/window/input authority 搬云。
- `P1=0`：父级 baseline seed、exact context、single checkpoint、gate-disabled、non-public authority/value/update 条件均已落实。
- `P2=0`：真值表、SOLO->auto UNKNOWN、reason/result 与 dependency/caller dormant 边界均保持。
- Activation 前置项仍按 Design #1：future authenticated role/config adapters 与 host/Task dependency closure 不在本写集，不能据此启动 cohort。

Worker G 自审不构成批准。当前状态：**IMPLEMENTATION #1 COMPLETE; CLOUD CLEAN PACKAGE PASSED; WAITING FOR PARENT SOURCE REVIEW**。

**无已批准业务差异；按基线等价迁移。**

## Parent Design Review #1 - DESIGN APPROVED - 2026-07-12

父级已核对两仓 context/metadata/authority assembly、DHXY 两个 startup caller 与全部真值表。推荐的 Cloud `2 new`、
configless tenant/user policy authority、exact context role fact 与本地 detector 全留 thin client 的方向成立。结论：
**DESIGN APPROVED，P0/P1/P2=0**，以下父级约束覆盖 Design #1 的实施细节：

1. `PolicySnapshot` 不得由构造器或 Service 静默自动采用 baseline。只允许 future authenticated activation adapter 在明确
   得到 `NO_OVERRIDE` 后调用 package-private `seedBaselineNoOverride(scope)`；provider failure/未 seed 时 authority 不可构造
   evaluation，cohort 保持 dormant。full-snapshot CAS 仍 package-private、O(1)、无 history/thread/TTL。
2. `StartupRoleFact` 不接受任何裸 role/taskRunId/window tuple factory。唯一 package-private bind 必须接收 non-mintable exact
   `TaskExecutionContext`，从该对象一次投影完整 scope/run/type/window/stopEpoch/revision 与 role；service constructor 只接受
   package-private immutable `Evaluation`，不提供 public role/config overload。
3. 每次 `checkFiveRing/checkAutoBattle` 对实际传入 context 只调用一次 `context.throwIfStopRequested()` 作为 typed
   current-confirmed gate，再做 context 与 bound fact 的逐字段 exact compare；不得先 `revalidate()` 后 deny 时再次 checkpoint，
   避免两次 coordinator read。STOP/PAUSED/stale/completed/denied 原样 typed unwind，不映射 startup result。
4. gate-disabled 分支仍在上述 exact/current 安全门之后直接 allow，但不得读取 role 条件；两个 baseline 真值表、reason 英文
   message/格式、SOLO→auto UNKNOWN 与 `TaskStartupCheckResult` 保持。
5. `CloudStartupGateAuthority`、nested policy/fact/evaluation 与 mint/update API 全 package-private；
   `TaskStartupCheckService` 保留 public class/public check 方法，但不加 Spring bean/host/server/caller。无 producer 时代码保持
   dormant，不声称 role/config 已接通。

现授权同一 G 只新增 §5.1 两个 Cloud Java 文件并追加 Implementation #1，运行 Cloud `mvn -q clean package`（不 skip）。
不得修改 context/remote/host/TaskTeamAssignmentPolicy/TaskStartupCheckResult/concrete Task/DHXY Java/tests。完成后父级源码复审。
**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #1 - BLOCKED - 2026-07-12

重启后父级对落盘的两个 Java 文件重新做 cold source review。写集、可见性、single typed checkpoint、exact context、
gate-disabled 直返、SOLO -> auto UNKNOWN、dormant host 等其余门禁均成立；当前仅剩一项基线差异。结论：
**BLOCKED，P0=0/P1=1/P2=0**。

### P1-1：role parser 的 `trim()` 把本地非法标签升级成可信角色

- 证据：Cloud `CloudStartupGateAuthority.parseRole` 第 136-144 行使用
  `TeamRoleStatus.valueOf(role.trim().toUpperCase(Locale.ROOT))`；DHXY HEAD
  `TaskStartupCheckService.roleFromContext` 只对原始字符串做 `MEMBER/LEADER` 的 `equalsIgnoreCase`，不 trim。当前实现会把
  `" MEMBER "` 解析成 `MEMBER`，而基线应为 `UNKNOWN`。当 `autoBattleRequiresMember=true` 且
  `allowAutoBattleWhenRoleUnknown=false` 时，结果会从基线 skip 变成 allow。
- 影响：非法/未规范化 activation metadata 可改变 startup admission，违反“invalid role label -> UNKNOWN”和无业务差异门。
- 精确返修条件：只修改 `CloudStartupGateAuthority.parseRole`，去掉 whitespace normalization；仅对原始字符串中
  `MEMBER/LEADER/SOLO/UNKNOWN` 的 exact case-insensitive 名称映射相应 enum，其余（含任何前后空白）一律
  `UNKNOWN`。不得修改 public API、真值表、checkpoint、policy、authority 可见性、另一 Java 文件或任何测试。
  返修后运行 fresh Cloud `mvn -q clean package`（不 skip），向本文件追加 `Implementation Repair #1`。

旧内部 Worker G 会话因桌面重启已不存在；父级将使用一个新的内部实现 Worker 继承同一固定写集完成该最小返修。
在父级 fresh source/build 复审通过前，同路径计数保持 `184/407`，host/concrete Task 继续 dormant。
**无已批准业务差异；按基线等价迁移。**

## Internal Worker G2 - Implementation Repair #1 - 2026-07-12

### 1. 返修范围与精确 diff

- 唯一 Java 改动：
  `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\task\startup\CloudStartupGateAuthority.java`。
- 仅修改 `parseRole`：删除 `role.isBlank()` 与
  `TeamRoleStatus.valueOf(role.trim().toUpperCase(Locale.ROOT))` 的 whitespace/case normalization 和异常分支，改为对
  原始字符串依次执行 `MEMBER`、`LEADER`、`SOLO`、`UNKNOWN` 的 `equalsIgnoreCase` 精确名称匹配；其余值（含 null、
  blank、任何前后空白与非法标签）统一返回 `TeamRoleStatus.UNKNOWN`。
- 删除因此不再使用的 `java.util.Locale` import；未改方法签名、可见性、调用点或其它 authority/policy/context 逻辑。

### 2. 完整性与静态证据

- 返修后 `CloudStartupGateAuthority.java`：278 行，SHA-256
  `5648EEA3F47665ABF8DD0ED680DC57A698F75D00DC5D33BD49D95FD80B5397ED`。
- 相邻受保护 `TaskStartupCheckService.java` SHA-256 仍为
  `289E3930E6CF3A935443A41CAEA02A70377AF9ECF10B521093AF56A0856638B1`，与 Implementation #1 记录一致。
- 对返修文件扫描 `.trim(`、`.strip(`、`Locale`、`toUpperCase`、`toLowerCase`、`normalize`：`0` 命中。
- 对返修文件扫描 `TeamRoleDetectionService`、`TeamTaskProperties`、`WindowRuntimeContext`/holder/tracker、HWND/JNA、
  input、capture/OCR、`Path`、Spring/`ConfigurationProperties`：`0` 命中。
- 语义收口：`"member"` 仍映射 `MEMBER`；`" MEMBER "`、`"\tLEADER"`、空白及其它非法标签均为
  `UNKNOWN`，恢复 Parent Implementation Review #1 要求的原始字符串边界。

### 3. 未触碰范围与停点

- 未修改 `TaskStartupCheckService`、public API、policy、checkpoint、context/remote/host、DHXY Java、resources、pom、
  tests 或其它 Worker 文件；除本固定报告追加外无其它文档写入。
- 按父级任务要求未运行 Maven/测试；父级将在并行 Cloud 写槽稳定后统一执行 fresh package。
- 未启动 application/server/host/Task/poller/UI/capture/OCR/input；未执行 Git add/commit/reset/checkout/clean 等 mutation。

Worker G2 自审仅作实现 QA，不构成批准。当前停点：**IMPLEMENTATION REPAIR #1 COMPLETE; WAITING FOR PARENT SOURCE/BUILD REVIEW**。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #2 - APPROVED - 2026-07-12

父级已复核 G2 最终源码：`parseRole` 仅对原始 `MEMBER/LEADER/SOLO/UNKNOWN` 做 exact case-insensitive 映射，
null/blank/前后空白/其它非法标签均保持 `UNKNOWN`；`Locale`/trim/normalize 链已删除。`TaskStartupCheckService`、public API、
policy、single typed checkpoint、exact fact/context、host/caller/tests 均无额外变化。

父级在 G2/H2 写入稳定后运行 fresh Cloud `mvn -q clean package`（未 skip），exit 0，4 suites / 21 tests，failures 0、
errors 0、skipped 0；JAR SHA-256 `60CEF8E6AD0035C4ECEB7464D3A5E7E6C60F29CB17862EDF1CC6FD3E8EAD3377`。
结论：**APPROVED，P0/P1/P2=0**。startup role gate 两个 dormant 文件可计入同路径迁移，`184/407 -> 185/407`；
role/config producer、host/concrete Task 仍未接通，本批准不授权运行或切换。
**无已批准业务差异；按基线等价迁移。**
