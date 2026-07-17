# Worker I：Cloud tenant-scoped auto-battle properties

## Parent Task Brief #1 - 2026-07-12

### 目标与阶段

为 `AutoCombatService` W0 设计最小 Cloud-native、tenant/user 隔离的只读 `CloudAutoBattleProperties`，当前只覆盖 HEAD
`BotProperties.getAutoBattleRefreshIntervalMs()` 的等价取值合同。首轮只追加 `Internal Worker I - Design #1`；父级明确
`DESIGN APPROVED` 前不得修改 Java/Maven/resources/tests。

### 必读与基线

- `D:\mavenProject\DHXY\AGENTS.md`、`docs\DHXY_CONTEXT.md`、`docs\业务逻辑.md`、`docs\ACTIVE_WORK.md` 顶部 CR271、
  `docs\superpowers\specs\2026-07-12-service-migration-matrix.md`。
- 本报告、A 固定日志 `2026-07-12-cloud-auto-combat-service-worker-a.md` 最新 Design Review #3。
- DHXY HEAD `0114604e` 的 `BotProperties`、`AutoCombatService` 全部该 getter caller、resources 实际 override 与配置装配。
- Cloud `CloudServiceScope`、现有 configless `TaskTeamAssignmentPolicy` / startup policy authority、host/config 当前写集。

### 设计不变量

1. 只迁当前 getter 的既有值/单位/缺失语义，不新增配置项、默认值、clamp、TTL、reload、fallback 或 timer 规则；不得改变
   AutoCombat 的任何时长与判断。
2. Cloud 不复制 Spring Boot `ConfigurationProperties`、本地 properties 文件/Path/env/System property reader，不形成 DHXY +
   Cloud 双权威。
3. 配置必须绑定 authenticated tenant/user，full immutable snapshot 原子 seed/update；明确 `NO_OVERRIDE` 与 provider failure
   的不同处理。provider failure 不得静默退回 baseline 并激活 cohort。
4. public 业务面只允许读该一个 typed value；seed/update/mint 全非 public，不开放 map/raw key/string config/host endpoint。
5. 无 ThreadLocal、线程、poller、后台刷新、history、TTL、自动 retry；host/Task/AutoCombat 继续 dormant。
6. 写集不得触碰 H `remote/**`、B `host/CloudServiceStorage/Configuration` 与 artifact 文件、A PAUSED observer 双仓文件、
   G startup 文件或 DHXY Java。

### Design #1 必交付

- HEAD getter 字段、默认/override、单位、合法范围、所有 caller 与 exact 行为证据。
- 推荐 public read API、package-private authority/snapshot/update 可见性，tenant/user exact binding、revision/CAS、容量/运维。
- `NO_OVERRIDE`、provider failure、错 tenant、并发更新、restart、缺失 seed 的矩阵。
- exact Cloud 文件写集与 future host/AutoCombat 构造图；优先 1-2 个 new 文件且当前目标不存在，不修改现有 host 配置。
- P0/P1/P2、自审与批准后 package 门。自审不是批准。

### Worker 规则

- 你是实现 Worker，不是 reviewer；只向本文件 append Design #1，父级批准前不改代码。
- 保护全部 dirty/untracked，不回滚、不覆盖、不提交；不运行 Maven/测试，不启动 application/server/host/Task/poller/UI/
  capture/OCR/input。

**无已批准业务差异；按基线等价迁移。**

## Parent Implementation Review #1 - APPROVED - 2026-07-12

父级已逐行复核两个新文件并运行当前 Cloud 树 fresh `mvn -q clean package`，exit 0，4 suites / 21 tests，
failures=0、errors=0、skipped=0；JAR `dhxy-cloud-brain-0.1.0-SNAPSHOT.jar` 119573405 bytes，SHA-256
`2EAAB5767FD8A5CA0A7B43905C00DE8B8AB285170C4533D05F8A3ED23F6F0534`。

源码结论：public 面仅 `long getAutoBattleRefreshIntervalMs()`；authority/seed/replace/Snapshot/Source 全非 public且未注册
Spring bean。`NO_OVERRIDE` 精确返回 `120000L`，override 原样保留任意 signed long；negative provider revision 由 Snapshot
拒绝。exact `CloudServiceScope`、expected revision、strictly newer revision 与单次 CAS 均落实，冲突显式失败且无 retry。
无 Boot/env/System property/Path reader、clamp/fallback、ThreadLocal、history、TTL、poller、后台刷新或 durable restart 声明；
写集为 2 New / 0 Modify，未触碰其它 Worker 文件。

结论：**APPROVED，P0/P1/P2=0**。该 configless read capability 可供后续 authenticated provider/host 切片接线，但当前无
producer/bean/caller，AutoCombat/host 继续 dormant；Cloud-specific 新类型不增加同路径迁移计数。**无已批准业务差异；
按基线等价迁移。**

## Parent Design Review #1 - DESIGN APPROVED - 2026-07-12

父级已用 DHXY HEAD `0114604e` 复核 `BotProperties` 字段默认 `120_000L`、`application.yml` 同值 override、
`AutoCombatService` 两个 getter caller 与 `AutoCombatPanelService` 一个 caller；确认源值是未校验的 signed `long`，三个 caller
分别保留各自 `>0` / `Math.max(0L, value)` 业务判断。目标两个 Cloud 文件当前均不存在。结论：
**DESIGN APPROVED，P0/P1/P2=0**，实施受以下边界约束：

1. 写集严格为 Cloud `CloudAutoBattleProperties.java` 与 `CloudAutoBattlePropertiesAuthority.java` 两个 New，零 Modify；不得触碰
   A/H/B/G 文件、host/config 接线、DHXY Java、Maven/resources/tests。
2. public 面只有 `long getAutoBattleRefreshIntervalMs()`；authority、seed/update/CAS、Snapshot/source enum 全非 public，
   不注册 Spring bean，不暴露 raw map/key/scope/revision/update capability。
3. exact `CloudServiceScope(tenantId,userId)` 绑定、authenticated explicit seed、NO_OVERRIDE=`120000L` 与 provider failure 不 seed
   必须按设计实现；override 原样保留任意 signed long，不加 clamp/正数校验/default fallback。
4. full immutable snapshot 单次 CAS；expected mismatch、non-advancing revision、CAS conflict 均显式拒绝且无内部 retry。无
   file/env/System property reader、ThreadLocal、history、TTL、poller、后台刷新或 durable restart 恢复声明。
5. Worker I 完成后只向本报告追加 `Implementation #1` 与精确 bytes/SHA/静态证据，不自行运行 Maven；父级统一运行 fresh
   Cloud `mvn -q clean package` 并做源码复审。host/Task/AutoCombat cohort 继续 dormant。

同一 Worker I 现可实施上述 `2 New / 0 Modify`。**无已批准业务差异；按基线等价迁移。**

## Internal Worker I - Design #1 - 2026-07-12

本设计仅覆盖 `BotProperties.getAutoBattleRefreshIntervalMs()` 这一个已有取值合同。证据基线为
DHXY HEAD `0114604e1ff5f15491d2910959c45252e893d04f`；当前工作区 `AutoCombatService.java` 为 dirty，本设计
只采信 HEAD，没有修改 Java/Maven/resources/tests，没有运行 Maven/测试/应用。

### 1. HEAD 字段、默认、override 与全部生产 caller

| 项 | HEAD 证据 | 等价合同 |
|---|---|---|
| 字段/getter | `BotProperties.java:52` 是 `private long autoBattleRefreshIntervalMs = 120_000L`；getter 由 Lombok `@Data` 生成 | 类型为 Java signed `long`，单位为毫秒 |
| 代码默认 | `120_000L` | Cloud baseline 必须精确为 `120000ms` |
| 仓库配置 override | `application.yml:64` 显式写 `bot.dhxy.auto-battle-refresh-interval-ms: 120000`，与字段默认相同；`application.properties` 无该 key | 仓库内没有不同值 override |
| 装配 | `BotProperties` 为 `@Component` + `@ConfigurationProperties(prefix="bot.dhxy")`，由 `@SpringBootApplication` 组件扫描绑定 | 本地最终值理论上可被 Spring 外部 property/env/command-line 优先级替换；Cloud 不读这些本地源，而由 authenticated provider 传入已解析的 tenant 值 |
| UI/runtime override | main 中无 `setAutoBattleRefreshIntervalMs` caller；`GameUiSettingsStore` 不读写该项 | 不迁移 UI 存储或 Path reader |
| 合法范围 | 该字段没有 `@Min`/自定义校验 | 接受并保留任意 signed `long`；不新增正数校验、clamp 或 fallback |

HEAD main 的 getter caller 只有下列 3 处：

1. `AutoCombatService.nextCombatMaintenanceDelayMs` (`:460`)：只在值 `> 0` 时把定时面板刷新纳入下次
   wake deadline；`<= 0` 不增加该 deadline。
2. `AutoCombatService.maybeRunCombatMaintenance` (`:994`)：只在值 `> 0` 时计算
   `UNKNOWN/LOW_ROUNDS/REFRESH_DUE`压力；`<= 0` 在已有 UI cleanup/entry verify 后直接返回，不进入
   `VERIFY_AND_REFRESH`。
3. `AutoCombatPanelService.refreshAutoCombatRoundsIfNeeded` (`:157`)：先 `Math.max(0L, value)` 再交给
   `resolveRoundsRefreshReason`。因此 `<= 0` 只关闭时间到期的 `REFRESH_DUE`；`UNKNOWN` 与
   `LOW_ROUNDS` 仍按 HEAD 立即刷新。

上述三处对 `0`/负值的不同使用方式是业务基线，Cloud properties 只返回原值，不将 caller
的判定合并进配置层。

### 2. 推荐边界：public 单 getter + package-private authority

批准后只新建两个 Cloud 文件：

1. `src/main/java/com/bot/dhxy/config/CloudAutoBattleProperties.java`
   - `public interface CloudAutoBattleProperties`；
   - public 业务面只有 `long getAutoBattleRefreshIntervalMs()`；
   - 不暴露 scope/revision/source/map/raw key/string config/update/mint。
2. `src/main/java/com/bot/dhxy/config/CloudAutoBattlePropertiesAuthority.java`
   - package-private `final class`，实现上述接口；constructor 为 private；
   - scope-bound seed factory、full-snapshot replace/CAS 方法全部 package-private；
   - 内部仅持有 `CloudServiceScope` 与 `AtomicReference<Snapshot>`；`Snapshot` 为 private immutable
     record，完整携带 `scope + configRevision + autoBattleRefreshIntervalMs + source`；
   - `source` 只有 `BASELINE_NO_OVERRIDE` 与 `CONTROL_PLANE_OVERRIDE`，为 private enum；
   - 不使用 `@Component`/自动扫描，不读文件/env/System property，未来 authenticated adapter 正式
     seed 前该类不可达。

`getAutoBattleRefreshIntervalMs()` 只做一次 `snapshot.get()` 并返回其 long，不做失败回退、转换、
clamp、reload 或延迟计算。这使 `AutoCombatService` 与 `AutoCombatPanelService` 只依赖一个
typed read capability，而更新权威不进入业务 package 外的 public 面。

### 3. seed/update/CAS 不变量

1. **tenant/user exact binding**：authority 创建时固定一个 `CloudServiceScope(tenantId,userId)`；每次
   package-private update 都必须携带并精确等于该 scope，错 tenant/user 立即拒绝，不受理任何值。
2. **explicit seed only**：只有未来 authenticated provider adapter 能在同 package 调用：
   - `seedNoOverride(scope, providerRevision)`：安装 `120000L` + `BASELINE_NO_OVERRIDE`；
   - `seedOverride(scope, providerRevision, exactLongValue)`：安装 provider 的 signed long 原值。
   `providerRevision` 必须非负；不自动默认 revision，不接收未认证 scope。
3. **full immutable replace**：`replaceNoOverride`/`replaceOverride` 都必须提供 exact scope、
   `expectedRevision` 与严格更新的 `nextRevision`；先构建完整 immutable Snapshot，再对
   `AtomicReference` 做一次 CAS。
4. **conflict is visible**：expected 不匹配、next 不严格前进或 CAS 竞争失败均拒绝，不内部 retry；
   上层必须重读 provider/当前 revision 后另走新操作。
5. **constant memory**：不保留 history、TTL、timer、poller 或后台刷新；无论更新多少次，每个 host
   仅一个当前 Snapshot。

### 4. `NO_OVERRIDE` / provider failure / restart 决策矩阵

| 场景 | 裁决 | 业务可见结果 |
|---|---|---|
| 首次 authenticated `NO_OVERRIDE(revision)` | 允许 `seedNoOverride` | getter 返回 HEAD baseline `120000ms` |
| 首次 authenticated `OVERRIDE(revision,value)` | 允许 `seedOverride` | getter 原样返回 value，包括 `0`/负值 |
| 首次 provider timeout/error/schema/auth failure | **不 seed** | 不创建 properties bean/不激活依赖 cohort；不静默改用 `120000` |
| 缺失 seed 却请求业务 getter | public 构造/mint 不存在，宿主装配失败 | fail closed，不得产生一个隐式 baseline 实例 |
| 已 seed 后 provider 查询失败 | 不调用 replace，保留最后一个已认证 Snapshot | 不回退 baseline，不发布新 revision；类本身不自动 retry |
| 新 revision 显式 `NO_OVERRIDE` | CAS 安装 baseline Snapshot | 从该原子边界起返回 `120000` |
| 新 revision 显式 `OVERRIDE` | CAS 安装 exact value Snapshot | 新 read 全部看到新完整值，无半更新 |
| 错 tenant/user | 在 CAS 前拒绝 | 原 Snapshot 不变 |
| stale/equal revision | 拒绝 | 原 Snapshot 不变 |
| 同 expected revision 并发更新 | 最多一个 CAS 成功 | 败者显式 conflict，无内部重试/覆盖 |
| 进程重启 | 内存 Snapshot 不恢复；必须重新访问 authenticated provider | provider 成功后才按 NO_OVERRIDE/OVERRIDE seed；provider 失败则 cohort 保持 dormant |

### 5. 容量、隔离与运维

- 每个 `CloudServiceHost` 只容许一个与其 `CloudServiceScope` 精确一致的 authority/read view；不建
  全局 tenant map，不把 device/clientSession/taskRun 错当配置所有者。
- 本两文件的单 host 内存是 O(1)。未来 host admission 仍必须用既定 tenant/user host 容量上限；容量满
  时拒绝新 host，不共享其他 tenant 的 authority，不驱逐正在使用的 Snapshot。
- host close 时整个 Spring context 释放 authority；不单独写盘，不持久化过期配置。restart 失效矩阵按上节执行。
- future adapter 记录 seed/update/conflict/provider-failure 的 scope-safe 审计（tenant/user 使用既有安全标识）、
  revision、source 和结果；不记录 raw config map/凭据。provider failure 必须可告警，但告警不能触发
  baseline fallback 或后台 retry。

### 6. Future provider/host/AutoCombat 构造图

```text
authenticated tenant config provider
    -> future package-local adapter in com.bot.dhxy.config
       -> NO_OVERRIDE / OVERRIDE / FAILURE 三态裁决
       -> CloudAutoBattlePropertiesAuthority (one full immutable Snapshot)
          -> expose only CloudAutoBattleProperties read view
             -> future exact-scope CloudServiceHost bean registration
                -> AutoCombatService(..., CloudAutoBattleProperties)
                -> AutoCombatPanelService(..., CloudAutoBattleProperties)
```

- 本轮批准后的 exact 写集仍只是上述 2 New，0 Modify；两个目标当前都不存在。
- provider adapter、`CloudServiceHost`/`CloudServiceConfiguration` 接线与 AutoCombat/Panel constructor 改造属后续切片；
  本轮不触碰 B 正在拥有的 host/artifact/config 写集。
- 切换前 provider 必须为每个 authenticated tenant/user 发布已解析值或明确 `NO_OVERRIDE`；不会将
  DHXY `BotProperties`、properties/yml/Path/env/System property reader 复制进 Cloud，因而不形成两个运行时权威。
- host/Task/AutoCombat cohort 在 provider adapter、H state owner、A PAUSED observer/slot/turn 和 W1 collaborator 全部
  就绪前保持 dormant；本 properties 边界的存在不代表可以启动 Task。

### 7. P0/P1/P2 与验收门

- P0：0。
- P1：0。`NO_OVERRIDE` 与 provider failure 已分离；tenant exact binding、full-snapshot CAS、restart fail-closed
  与无双权威均有明确边界。
- P2：0。未来 provider/host/AutoCombat 接线是明确的后续依赖，不是本两类内的隐含 fallback；未接线前
  全部 dormant。
- 父级若 `DESIGN APPROVED`，实施仅允许 2 New 并由父级统一运行 Cloud
  `mvn -q clean package`（不 skip）。本切片不新增/恢复测试，不运行 DHXY 本地测试。

自审：已核对 HEAD 三个 main caller、默认/override/无验证范围，并检查报告无待定占位项、
无 public raw config/update、无本地资源 reader、无 TTL/reload/retry/history。自审仅为 QA，不构成批准。

**无已批准业务差异；按基线等价迁移。**

## Internal Worker I - Implementation #1 - 2026-07-12

已按 `Parent Design Review #1 - DESIGN APPROVED` 实施，写集严格为 Cloud `2 New / 0 Modify`；没有修改
DHXY Java、Maven、resources、tests 或任何既有 Cloud 文件，没有运行 Maven/测试，也没有启动任何运行时或执行 Git
mutation。

### 精确文件与校验

| Cloud 新文件 | bytes | SHA-256 |
|---|---:|---|
| `src/main/java/com/bot/dhxy/config/CloudAutoBattleProperties.java` | 559 | `026D86C19B8A7A404BC85E6CC40DCECD1797B50BB6F4334E3FC7258B41EE8D10` |
| `src/main/java/com/bot/dhxy/config/CloudAutoBattlePropertiesAuthority.java` | 4706 | `025FB85D4741B4D2ED8CEA6F8ABC3A91C93B34A9E45951661382DB4D73DF310A` |

### 实现摘要

- `CloudAutoBattleProperties` 是 public read-only typed API，唯一业务方法为
  `long getAutoBattleRefreshIntervalMs()`；无 scope/revision/source/raw key/map/update/mint public 面。
- `CloudAutoBattlePropertiesAuthority` 为 package-private `final class`，private constructor；两个 seed factory 与两个
  replace 方法均为 package-private，private `Snapshot` record 完整保存
  `CloudServiceScope + configRevision + signed long value + Source`，private `Source` 只含
  `BASELINE_NO_OVERRIDE`/`CONTROL_PLANE_OVERRIDE`。
- `seedNoOverride` 精确安装 HEAD baseline `120_000L`；`seedOverride` 与 `replaceOverride` 原样保存任意 signed
  `long`，没有正数校验或 clamp。provider failure 没有对应 seed/fallback API，因此不能隐式创建 baseline authority。
- replace 在副作用前精确校验 authority `CloudServiceScope`、非负 expected revision、expected=current 与
  next>current；先构建完整 immutable Snapshot，再执行唯一一次 `compareAndSet`。冲突显式抛出，无内部 retry/history。
- getter 只做一次 `snapshot.get()` 并返回原始 long；无 Spring 注册、配置文件/env/System property reader、持久化、TTL、
  poller、线程或后台刷新。

### 静态证据

- scoped `git status` 对批准目录只显示上述两个 `??` 新文件；目标实施前均不存在，未覆盖既有文件。
- authority 中 `public` 方法扫描仅命中 getter；seed/update/CAS/Snapshot/source/constructor 均非 public。
- `compareAndSet` 扫描计数为 `1`；baseline 常量为唯一 `120_000L` 定义，并只用于 NO_OVERRIDE seed/replace。
- 禁止模式扫描零命中：`@Component`、`@Configuration`、`ConfigurationProperties`、`ThreadLocal`、线程/Executor/
  Scheduled/Timer、TTL/poller/retry/history、`Files`/`Path`、`System.getProperty/getenv`、Map/HashMap、
  `Math.max/min`。
- 文件行数：API `17` 行，authority `129` 行。未运行 Maven、javac 或测试；fresh Cloud package 由父级执行。

### 未触碰范围

- 未触碰 A PAUSED observer/current-context/AutoCombat 文件，H `remote/**`，B host/artifact/config，G startup 文件。
- 未触碰 `CloudServiceHost`、`CloudServiceConfiguration`、provider/endpoint/caller 接线、DHXY 任意 Java、Maven/resources/tests。
- host/Task/AutoCombat cohort 保持 dormant；本实现不声明 durable restart recovery。

Worker I 自审 `P0=0/P1=0/P2=0`，仅为 QA，不构成父级批准。现停止等待父级源码审查与统一 fresh package。
**无已批准业务差异；按基线等价迁移。**
