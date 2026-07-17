# TURN-13 Config Delivery Preflight Helper-R3

## PRECHECK

- 角色：CR271 非绑定 Helper-R3；本报告只做 TURN-13 配置/bean 装配 second-look，不构成最终审查结论。
- 材料：完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、HTTPS turn
  主计划、TURN-13 原报告；逐行读取 `TurnClientProperties.java`、`TurnConfiguration.java`、
  `application.properties`，并交叉核对 `HttpsTurnClient`、`TurnTemplateCache`、`TurnMatchStepExecutor`、
  `TurnLoopFactory`、`TurnLoopRegistry` 的构造合同与直接 bean 依赖。
- 未修改 Java/计划/CR/dashboard，未运行 Maven/tests/runtime/Git。
- 交付哈希与 TURN-13 原报告一致：
  - `TurnClientProperties.java`：`434AC07507E3796E746D816AB04EB8FDC67CB6424F77D6F39A8D7C94E24F401D`
  - `TurnConfiguration.java`：`33F2F81F72CCE681759CC79C932E794F02B926E3198C233659D452B5A1EFEE86`
  - `application.properties`：`729D27F17365532E104A5E5D5DD0613E58811494CBA1B8BF0162DB87A249594E`

## P0/P1/P2 风险候选

- P0 候选：`0`。
- P1 候选：`0`。
- P2 候选：`0`。

### 精确源码证据

1. `TurnClientProperties.java:13-18/:76-93` 提供独立 `cloud.turn.*` 默认值并校验 base URI/token/模板根
   非空、三个 timeout 为正、`requestTimeoutMs > longWaitTimeoutMs`。`application.properties:43-50` 的实际
   turn 配置满足该约束：`3000 / 65000 / 60000`，模板根为既有 `images/template`；只读检查确认该目录当前存在。
2. `TurnConfiguration.java:14-23` 将配置精确映射到 `HttpsTurnClient(URI,String,Duration,Duration,ObjectMapper)`；
   构造合同见 `HttpsTurnClient.java:56-85`。构造阶段只建立可复用 HTTP client，不发送请求；非 loopback 明文
   HTTP 的 fail-closed 仍由 `HttpsTurnClient` 自身 URI 合同负责。
3. `TurnConfiguration.java:25-29` 以同一个 `TurnClient` 和 existing template root 构造
   `TurnTemplateCache`，与 `TurnTemplateCache.java:37-46` 的参数顺序、existing-root 前置一致；缓存只在显式
   `resolveTemplate` 时按需下载，没有 bean 初始化期网络调用。
4. `TurnConfiguration.java:31-35` 为没有组件注解的 `TurnMatchStepExecutor` 提供唯一 bean，并传入
   `TurnTemplateCache` 与已有 `@Component TurnCaptureStepExecutor`；构造合同与
   `TurnMatchStepExecutor.java:24-33` 一致。`LocalTurnActionExecutor.java:22-49` 已是组件并按类型消费该 bean。
5. `TurnConfiguration.java:37-45` 先由唯一 `TurnClient` 与已有 `LocalTurnActionExecutor` 构造 inert
   `TurnLoopFactory`，再构造 `TurnLoopRegistry`；分别匹配 `TurnLoopFactory.java:14-16` 与
   `TurnLoopRegistry.java:17-18`，配置类没有调用 `registry.create(...)` 或 `loop.start()`。
6. `TurnConfiguration.java:47-53` 只把正数 long-wait 值、已有 `MultiWindowTaskManager` 组件和 registry 交给
   `TurnModeGuard`。`application.properties:63` 的 legacy `cloud.dev-sidecar.auto-start-enabled=true` 没有被
   `TurnClientProperties` 或 `TurnConfiguration` 读取，不能经该装配链启动 turn。
7. `TurnConfiguration.java` 只声明 `TurnClient`、`TurnTemplateCache`、`TurnMatchStepExecutor`、
   `TurnLoopFactory`、`TurnLoopRegistry`、`TurnModeGuard` 六类 inert bean；只读扫描未发现 `@PostConstruct`、
   scheduler、poller、retry/reconnect、per-window loop create/start 或 server 启动调用。

## 返修建议与父级复验点

- 当前未形成需要返修的 P0/P1/P2 候选。
- 父级在最终源码审查中仍应确认 TURN-13 其余授权文件没有引入第二份同类型 bean，且
  `WindowTaskControlService` 真实入口只消费这里的单例 `TurnModeGuard`；本报告只预检配置/bean 交付。
- 父级 build cohort 应验证 Spring 编译装配；本 Helper 按限制未运行 Maven、tests 或应用上下文。
- 若发布环境不以项目根为 working directory，父级应在部署配置中把 `cloud.turn.template-root` 覆盖为实际既有
  template root；当前相对路径与仓库现有运行约定一致，不单列风险候选。
- 本报告不得被解释为 `APPROVED`、`BLOCKED` 或 `CLOSED`。
