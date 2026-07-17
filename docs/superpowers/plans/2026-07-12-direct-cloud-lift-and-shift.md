# DHXY 全量云端直迁实施计划

> 状态：执行中  
> 目标：保持现有 `Task` / `Service` 类边界与业务行为，把业务实现整体迁入 `dhxy-cloud-brain`；DHXY 本地最终只保留窗口、截图、输入、UI 与安全拒绝。

## 1. 已冻结边界

- 云端拥有任务阶段、业务判断、OCR/模板解释、导航决策、重试与 fallback 顺序。
- 本地拥有窗口绑定、HWND 截图、物理输入队列、动作执行、UI 和错窗/停止安全拒绝。
- 迁移不顺带改变 phase、retry、fallback、click、navigation、sleep、stop/pause checkpoint 语义。
- 开发可以按依赖顺序逐类迁移；生产只在全量完成、对照验收通过后整体原子切换。

## 2. 目标调用形态

```text
Cloud Task/Service
  -> RemoteGameClientPort（同步）
     -> cloud command broker
        -> DHXY local poller
           -> bound-window capture / window fact / InputActionQueue
        <- structured terminal outcome
     <- typed outcome
  -> 原 Service 后续判断继续执行
```

`RemoteGameClientPort` 首版只有三类机械能力：

1. `capture(...)`
2. `readWindowFact(...)`
3. `executeInputBundle(...)`

本地不得在这三类操作中选择 NPC、任务阶段、重试、后继动作或 fallback。

## 3. 实施阶段

### 阶段 0：基线与合同

- 固定五倍业务基线 `3f0a2e7`、修罗业务基线 `696a12b0`。
- 固定通信字段、窗口绑定门、动作幂等、UNKNOWN 与断线恢复原则。
- 完整数据库 DDL、跨区灾备和容量优化不作为开工前置。

### 阶段 1：双向通信桥

- Cloud Brain：类型化端口、按设备命令队列、同步等待、poll/outcome HTTP 端点。
- DHXY：长轮询 transport、命令 handler、显式启停生命周期。
- 第一轮只证明命令可往返，不自动启动、不发送真实输入。

### 阶段 2：本地机械执行器

- `CAPTURE` 接到当前 `WindowRuntimeContext` / `WindowNativeBinding`。
- `WINDOW_FACT` 只返回绑定、几何、焦点与停止事实。
- `EXECUTE_INPUT_BUNDLE` 一次进入现有 `InputActionQueue`，保持 move+click 等原子 bundle。
- 实现错窗、stop、timeout、幂等与结构化 outcome；输入 worker 返回真实 started/completed step，
  远程 deadline 到达后不再开始下一步；不加入业务判断。

### 阶段 3：云端 Service 宿主

- 保持现有 Service 的同步调用风格，通过构造注入 `RemoteGameClientPort`。
- 先迁无 UI、无物理输入的纯值对象/工具，再迁截图解释与共享 Service，最后迁 Task。
- poller 只由显式云端 task-run 会话启动：云端先返回 tenant/device/session/taskRun/window/stopEpoch
  绑定，本地注册成功后才接受该 run 的机械命令；未知 run 一律拒绝，不从首条输入命令反推授权。
- 原子切换后的 UI 启动入口必须二选一：云端模式调用 remote task-run lifecycle，不再同时调用
  `WindowTaskRunner.submit(...)`、`taskFactory.createTask(...)` 或本地 `task.execute(...)`。本地
  `MultiWindowTaskManager` 只继续提供已注册窗口的 `WindowRuntimeContext`/runner 查找，不推进业务 phase。
- 最小生命周期为幂等 `prepare -> local register + poller ready -> activate -> stop/complete`；activate 响应
  不确定时不能擅自启动第二个 run，stop 首先本地 fail-closed 置 inactive，再请求云端停止。pause/resume
  仍由同一云端 run 的调度状态承载，不通过启停 poller 或创建本地 Task 模拟。
- 每迁一条调用链都对照 pushed baseline，记录 `无已批准业务差异；按基线等价迁移`。

### 阶段 4：按依赖波次搬类

1. 公共模型、枚举、配置读取与纯工具。
2. vision/OCR/template 解释代码及共享记忆读写。
3. 窗口事实消费者、截图消费者和输入 bundle 构造器。
4. 通用 Service：导航、NPC、对话、背包、战斗、玩家状态与维护。
5. Task 与调度状态：五环、五倍、修罗及其共享 task runtime。
6. 云端启动装配、租户/设备/窗口/taskRun 隔离与恢复。

### 阶段 5：本地瘦身

- 只有云端替代链已编译并完成对照后，才删除对应本地业务实现。
- 本地保留 UI、注册窗口、截图、输入、日志、安全拒绝和云端连接状态。
- 不保留断云本地业务 fallback，避免双重权威。

### 阶段 6：验证与整体切换

- 双侧编译和 Cloud Brain 既有强制测试持续通过。
- shadow 对照同一输入下的阶段、动作 bundle、顺序和结果。
- 验收错窗、消息乱序、超时 UNKNOWN、断线重连、stop/pause、多窗口隔离。
- 通过后形成整体切换/回滚方案；生产切换需单独执行，不在编码阶段自动发生。

## 4. 当前执行切片

- 已完成：Cloud Brain 类型化 `RemoteGameClientPort`、broker、poll/outcome endpoint；DHXY HTTP transport、
  polling loop、精确 HWND 的 CAPTURE/WINDOW_FACT/INPUT 机械 handler。polling loop 仍默认不启动。
- 进行中：现有 `InputActionQueue` 增加仅供远程请求使用的可选单调 deadline 与结构化 step 结果；旧本地
  boolean API 和所有业务调用保持原行为。
- 并行进行：筛选并原样复制下一批无 UI、无 HWND、无物理输入的 leaf Service 到 Cloud Brain；复制后
  先编译但不激活，避免用户私有 memory 在 tenant 隔离完成前被单例共享。
- 随后：实现显式 task-run 会话握手/注册生命周期，再把首条完整 Service 调用链接到
  `RemoteGameClientPort`，进入 shadow 对照。

## 5. 通信桥切片完成条件

- 双边 DTO 字段和 operation 枚举一致。
- 云端可排队命令、被指定设备 poll、接收 outcome 并解除同步等待。
- 同 requestId 同 digest 不重复执行；不同 digest 明确冲突。
- 超时/断线不被伪装成成功或 `NOT_EXECUTED`。
- 输入 outcome 带真实 queue request id、started step 与 last-completed step；未开始和开始后不确定必须可区分。
- 本地 polling loop 默认不启动且没有截图/输入副作用。
- Cloud Brain `mvn -q package` 通过；DHXY `mvn -q -DskipTests compile` 通过。
