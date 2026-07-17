# CR271 Sheyaoxiang Decision Facade - Internal Worker BW

## 2026-07-14T06:49:07-04:00 - CLAIMED

- Worker: `Internal Worker BW`，只做实现，不承担 reviewer。
- 业务基线：DHXY committed `0114604e`；无已批准业务差异，按基线等价迁移。
- 必读已完成：`D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、
  `docs\ACTIVE_WORK.md` 顶部 CR271、
  `docs\superpowers\plans\2026-07-13-direct-service-input-bundle-migration.md` 简化计划。
- DHXY 当前分支为 `thin-client-design`，工作区已有大量 dirty/untracked 并行改动；Cloud 当前分支为
  `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01`，同样已有大量
  dirty/untracked 并行改动。全部视为他人所有并保护，不回滚、不覆盖、不清理、不做 Git mutation。
- Cloud 当前 `DecisionEngine.java` SHA-256：
  `6b39d974fe5f8c317cdd775ac31cee3bd7a5b83bfe56e5250095a37e25ad5a37`。
- 当前 `sheyaoxiangStatus(...)` 方法体 SHA-256：
  `731b67fb46ab8375ba790e81bf905907fd26fc59f32b11021217e985d2a05d42`；与 Cloud HEAD
  同方法体 SHA 完全一致。实施只把该方法 `private` 改为 package-private，方法体零改。

### 精确写集

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`
   - 仅 `sheyaoxiangStatus(JsonNode, String)` 可见性由 `private` 改为 package-private；方法签名其余部分和方法体不变。
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\SheyaoxiangStatusDecisionFacade.java`
   - 新建同 package 单层 typed facade；只持有并委托调用方传入的现有唯一 `DecisionEngine`，不自行构造 engine，
     不创建第二 owner/queue/session/ledger/TTL/retry。
3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-sheyaoxiang-decision-facade-worker-bw.md`
   - 本 append-only 实施/编译证据报告。

### 装配判断

- 本切片不需要修改现有 Cloud assembly/constructor：facade 可独立编译并保持 dormant，后续 caller/host
  cohort 可把已经由 Cloud 同 package 创建的唯一 `DecisionEngine` 实例注入该 facade。
- 本 Worker 不接 caller、不改 host/startup、不启动 runtime；因此不会修改 `CloudBrainServer`、
  `CloudTaskRunAuthorityAssembly` 或任何 remote wire。

## 2026-07-14T06:53:59-04:00 - IMPLEMENTED / AWAITING PARENT REVIEW

### 实施结果

- `DecisionEngine.sheyaoxiangStatus(JsonNode, String)` 已由 `private` 收窄改为 package-private；返回类型、
  参数、异常行为及六行方法体均未修改。
- 新增 `SheyaoxiangStatusDecisionFacade`：
  - public constructor 只接收已经存在的 `DecisionEngine`，唯一实例字段为该 engine 引用；
  - public `decide(SheyaoxiangStatusCloudRequest)` 直接调用同一 engine 的 canonical
    `sheyaoxiangStatus(...)`；
  - 沿用 committed `0114604e` 的 typed request validation、context/trace 投影与 typed decision 解析形状；
  - facade 不构造 `DecisionEngine`，不持有业务状态，不创建 owner/queue/session/ledger/TTL/retry，
    不执行 capture/input，不接 caller/host/remote wire。
- assembly/constructor 未改：本切片编译不需要接线；全 Cloud `rg` 结果中 facade 只有自身声明与构造位置，
  没有 caller、host 或 startup 引用，保持 dormant。

### 方法体零改证据

- 编辑前当前工作树方法体 SHA-256：
  `731b67fb46ab8375ba790e81bf905907fd26fc59f32b11021217e985d2a05d42`。
- Cloud HEAD `3b988caa010254973e03342272e6d1d6a9685b01` 同方法体 SHA-256：
  `731b67fb46ab8375ba790e81bf905907fd26fc59f32b11021217e985d2a05d42`。
- 编辑后方法体 SHA-256：
  `731b67fb46ab8375ba790e81bf905907fd26fc59f32b11021217e985d2a05d42`。
- 三者完全一致；`DecisionEngine.java` 的本 Worker delta 仅为该方法声明去掉 `private`。该文件其余既有
  dirty 差异属于并行工作，本 Worker 未改写或回滚。

### 编译门禁

- 在 `D:\mavenProject\dhxy-cloud-brain` 执行：`mvn -q compile`（未执行 `clean`）。
- 结果：exit `0`，wall time `17.9s`。
- 按指令未新增、恢复或运行任何测试；未启动 application/server/host/Task/poller/UI/capture/input。

### 最终源码 SHA-256

- `DecisionEngine.java`：
  `1b44cf7113c54816724170707452c3f1d7668f02ca6222c9f3997e6a267019c3`。
- `SheyaoxiangStatusDecisionFacade.java`：
  `71fb6ef1a987ab836d6ffaec83dd521e1df63513c8e5ed6434148d3be7203591`。

### Changed files（本 Worker）

1. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\DecisionEngine.java`
2. `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\SheyaoxiangStatusDecisionFacade.java`
3. `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-sheyaoxiang-decision-facade-worker-bw.md`

- 未修改 DHXY Java、`PlayerStateService`、`TeamReturnService`、`NpcClickService`、remote wire、assembly、
  constructor、caller、host、Maven、资源或测试。
- 未执行 reset/checkout/clean/delete/stage/commit/branch/worktree 或其它 Git mutation；所有既有
  dirty/untracked/并行改动保持原状。
- Internal Worker BW 不给出 reviewer approval；交付父级源码审查。
- **无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - 2026-07-14T07:00:00-04:00

**SOURCE APPROVED，P0/P1/P2=0；最终 consolidated fresh package 待并行 Java 写入稳定后统一执行。**

父级完整读取 `SheyaoxiangStatusDecisionFacade.java`、`DecisionEngine.sheyaoxiangStatus(...)` 和 committed
`0114604e` 的 `SheyaoxiangStatusCloudDecisionService`。facade 的 request validation、context 字段及顺序、trace、
action/present/remaining/icon/confidence/reason/decisionId 解析和 required-failure 投影，与既有 typed HTTP client 语义一致；
差异仅是同进程调用 canonical `DecisionEngine`，不经过 coordinator mode/execution-percent gate，符合本 slice 的 typed
same-process facade 目标。

父级复算 `DecisionEngine.java` SHA-256 为
`1b44cf7113c54816724170707452c3f1d7668f02ca6222c9f3997e6a267019c3`，facade 为
`71fb6ef1a987ab836d6ffaec83dd521e1df63513c8e5ed6434148d3be7203591`；`git diff` 在该方法声明处仅显示
`private Decision` -> package-private `Decision`，六行方法体未改。全 Cloud 搜索确认 facade 只有自身声明/构造，未接
assembly/caller/host，也未创建 engine、状态、owner/queue/session/ledger/TTL/retry。Worker Cloud `mvn -q compile`
exit 0。源码门通过；待 BV 与 External A/B/C/D 写入稳定后由父级统一跑 `mvn -q clean package`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
