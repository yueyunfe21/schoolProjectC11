# 修罗玩法逻辑上云迁移方案（终极目标：路线 B 原语化薄壳）

> 状态：**待 review**（作者已完成全量只读审计，等待 reviewer 结论写回本卡）
> 编写时间：2026-07-10
> 目标读者：reviewer / 架构决策人
> 关联文档：[HYBRID_CLOUD_WORKFLOW.md](HYBRID_CLOUD_WORKFLOW.md)（总原则）、[业务逻辑.md](业务逻辑.md)（修罗业务基线 L1064-1223 等）、[PACKAGE_ARCHITECTURE.md](PACKAGE_ARCHITECTURE.md)（CR 卡）
> 审计方法：8 个只读子代理并行精读 + 1 个完整性批判代理，覆盖 XiuluoTaskV2 全部 205 个方法/内部类声明（三段调查零遗漏，边界重叠双覆盖）、13 个支撑文件、~30 个文件夹外承载点、35 条业务基线规则、云端 DecisionEngine 与传输契约。

---

## 0. 一句话结论

修罗的**核心状态机其实已经上云**（XIULUO_BRAIN，CR195 起，云端持有会话与全部相位转移），但 cloud-brain 目前是跑在**用户本机**的 sidecar（`127.0.0.1:18080`、`local-dev-token`、明文 HTTP、jar 就在用户磁盘上），所以**当前防破解价值为零**。同时本地 `XiuluoTaskV2.java` 仍有 **7171 行**，里面混着"已上云的壳 + 已被架空的 legacy 状态机 + 相位执行器"三层。

**终极目标已定为路线 B：把本地做成一个不含任何修罗业务知识的通用原语解释器，修罗逻辑全部变成云端按会话下发的脚本，本地二进制内修罗专属代码归零。** 按 A→B 分期推进，A 期每一步都是 B 的前置工程。

---

## 1. 背景与目标

### 1.1 用户诉求（原话归纳）
- 把修罗 task 文件夹下"基本上所有东西"放进云端 cloud-brain，本地只留必要的壳。
- 目的：**防破解**——别人逆向本地软件后，拿不到修罗里的任何业务逻辑。
- 本地留得越少越好。
- 终极目标：路线 B（见 §5）。

### 1.2 为什么这事有意义、也有天花板
- 本地 Java 客户端一旦分发到用户机器，二进制可被反编译。任何编译进本地的相位顺序、模板、阈值、恢复链都能被读出。
- **能达到的理论上限**：本地只剩一个"会截屏、会点击、会问云端下一步"的通用执行器，逆向者看不出这是修罗还是别的玩法。
- **达不到的部分（必须诚实告诉决策人）**：只要本地会执行云端指令，攻击者就能在本机挂代理观察**指令流**，多跑几轮即可重建状态机行为。这是路线 B 也防不死的，只能靠协议不透明化 + 轮换 + 鉴权把成本抬高，不能归零。详见 §6 威胁模型。

---

## 2. 现状盘点：到底迁了没有

### 2.1 已经在云端的（ALREADY_CLOUD，比直觉多）
`dhxy-cloud-brain` 的 `DecisionEngine` 里 XIULUO_BRAIN 服务已实现目标形态——**云端持有整个回合状态机，本地只执行指令流**：

| 能力 | 证据 |
|---|---|
| 会话状态机（sessionId/stateSeq/phaseToken/actionId 服务端保存），防重放/防乱序令牌协议 | `DecisionEngine.java:63-80,335-489,391-535`；`XiuluoBrainCloudDecisionService.java:266-360` |
| 17 个相位转移规则、重试/恢复预算、连败 10 次熔断、RESTART_ROUND 一等命令 | `DecisionEngine.java:69-70,458-474,1324-1351,3894-3913` |
| 本地→云 facts 上报 + 7 种指令执行（EXECUTE_PHASE/RUN_CLEANUP/WAIT_FOR_EVENT/RESTART_ROUND/COMPLETE_ROUND/STOP_TASK/FAIL_TASK） | `XiuluoBrainActionType.java:3-15`；`XiuluoTaskV2.java:594-777` |
| `fallback=STOP` + `execute-percent=100`：修罗已"无云不可运行"（fail-closed，正是防破解要的性质） | `application.properties:76-79`；`CloudDecisionCoordinator.java:95-181` |
| CR232 看打入战仲裁（attemptId + CLICK_FAILED 握手）、CR253 绿链 typed-job、tracker 云端读取（"无本地回退"）、NPC Click FIFO（CR169） | `DecisionEngine.java:2625-2638`；`TaskTrackerPanelService.java:1034-1210`；`SmartClickRecognizer.java` |

### 2.2 关键误解澄清：那 15 个文件是"1 大 + 13 小"，不是重复文件
- `XiuluoTaskV2.java` **一个就占 7171 行**（folder 共 8058 行，其余 13 个文件合计仅 887 行）。
- 那 13 个小文件是枚举/DTO/状态载体（XiuluoPhase、XiuluoWaitSpec、XiuluoStepOutcome、XiuluoRoundContext 等），本质是云↔本地协议词汇表和运行时状态容器，**不大、也删不得**。
- 所以"folder 下 15 个大文件"这个印象不准确——体量几乎全在 XiuluoTaskV2 一个文件里。

### 2.3 XiuluoTaskV2 内部是三层混合（"迁没迁"困惑的根源）

| 层 | 内容 | 代表方法 | 处置 |
|---|---|---|---|
| **① 云脑壳** | 问云端"下一步干什么"→执行→回报 facts | `runRoundWithXiuluoBrain` L594-777、`executeXiuluoBrainCommandShell` L863-1047 | **留**（这就是目标架构本身） |
| **② legacy 本地状态机** | 云脑出现前的老路，与云端 DecisionEngine 逻辑**重复** | `runRoundPhases` L1599-1747、三个 `restartRoundAfterXxx` L2014-2083、legacy `resolveTaskHotStart` 驱动 | **可删**（`execute-percent=100` 下已不走，但代码完整留着——这是"决策权迁了、旧决策代码没删"的尸体，也是逆向者能读到的最完整修罗逻辑） |
| **③ 相位执行器** | 每个相位"具体怎么干"：截图/模板/点击/OCR/恢复探测链 | `runPhase` switch L2664-2687、`recoverTargetClickFailure` L5050-5149 等 | **大头，当前架构删不掉**（云端只下令"点目标NPC"，真正伸手点的代码必须在本地）；但夹带的决策碎屑可继续上收 |

**直接回答三个疑问：**
1. **迁了没有？** 决策权迁了（云端权威，无云跑不动），但本地代码没瘦身。
2. **是重复的吗？** 第 ② 层是——和云端 DecisionEngine 重复，历史遗留。
3. **能删吗？** 能，但不是删文件，是删 XiuluoTaskV2 里的 legacy 分支（粗估 1500-2000 行）。

---

## 3. 必须留本地的最小壳（搬不走的东西）

| 类别 | 代表 | 为什么留 |
|---|---|---|
| 感知执行体 | 截屏、模板匹配、OCR、tracker 绿带扫描 | 像素在本机 |
| 毫秒级紧循环 | kanda2 探测（小 ROI 持续匹配）、快脱战探针（1s cadence，基线 R28 明确不可改）、事件总线 park/wake | RTT 会破坏正确性 |
| 输入执行 | 原子 move+click、键盘、窗口句柄/焦点、任务轮公平锁 | 物理 I/O |
| 并发安全门 | attemptId 盖章/双重失效门、prepared 一次性消费、schedule 原子替换 | 竞态窗口不能跨云往返 |
| 安全兜底 | fail-closed、STOP 永不误报 FAILED（R18）、断线自停 | 云不可达时本地必须能安全停 |
| 胶水 | facts 序列化、坐标换算、metrics 打点、失败证据归档 | 无业务秘密 |

**关键点：这些留下的东西不含"修罗为什么这么打"的知识。这个理论上限（通用执行器）是达得到的。**

---

## 4. 为什么"删完 legacy 还剩 4-5 千行"不叫薄壳

用户的直觉正确：4-5 千行不是薄壳的物理下限，而是**当前指令粒度**的产物。

- 现在云端指令是**相位级**："EXECUTE_PHASE: 点目标NPC"。云端只说"干这件事"，但"这件事怎么干"——先试哪张模板、miss 换什么 ROI、再 miss 走 OCR、还不行直击战斗目标（`recoverTargetClickFailure` 四段兜底链）——整条执行链编译在本地。
- **这条执行链本身就是策略知识。** 所以当前架构即使删光 legacy，泄露面只是变小，没有变零。
- 结论：要真正归零，必须改变**指令粒度**，这就引出路线 B。

---

## 5. 终极目标：路线 B —— 原语化通用解释器

### 5.1 形态
把指令粒度从"相位级"降到**原语级**，本地退化成一个通用解释器，只认十几个动词，**不认识任何游戏玩法**：

- `截取 ROI`、`用刚下发的模板在此区域匹配`、`点击 (x,y)`、`按键`、`park 等事件 X`、`读这段文字`、`条件分支`……

修罗的一切——相位、恢复链、模板、顺序、阈值——全部变成**云端按会话下发的指令脚本**。本地二进制内一行修罗专属代码都没有。逆向者拿到的只是一个"什么玩法都不认识"的执行器。

### 5.2 关键收益
- **本地修罗代码归零**，防破解达理论上限。
- 解释器是**全任务共用**的（五倍、五环以后都走它），不是修罗单独成本——这是把 A→B 投资摊薄的核心理由。
- 与 `HYBRID_CLOUD_WORKFLOW.md` 第 8/9 条（图像清洗/识别策略包不得留本地生产路径）天然一致。

### 5.3 时延如何解决
- 云端不逐步一往返，而是**一次下发一小段脚本**（"匹配 A→命中则点击并等事件 X→miss 则匹配 B……"），本地解释执行完再回报，把往返摊到脚本边界。
- 毫秒级的东西（kanda2 探测、快脱战探针）**照旧留本地原语**，只是参数由云下发。

### 5.4 路线 B 的诚实边界
- **指令流观察防不死**：见 §6.1。原语化让单条指令语义更碎（点(x,y) 比 "EXECUTE_PHASE:点NPC" 泄露少），但长序列仍可被统计重建。对抗手段是脚本轮换 + 令牌鉴权 + 异常检测，不是让它变得不可能。
- **工作量大**：相当于把 XiuluoTaskV2 执行链逐条翻译成云端脚本，且受业务逻辑.md R0 等价门禁约束（逐条验收）。

---

## 6. 威胁模型（reviewer 重点：批判代理标记的 P1 洞）

> 迁远端只防**静态逆向**。若方案止步于"部署到云 + 换 token"，只堵了一半。

### 6.1 薄壳化后的残余攻击面
1. **明文 HTTP 抓包重放**：`application.properties:47` `cloud.base-url=http://127.0.0.1:18080`；`HttpCloudDecisionClient.java:58` 仅一个静态 `Bearer local-dev-token`，无 TLS/证书固定。本机攻击者挂代理即可全量记录。
2. **人类可读指令流重建状态机**：decision 契约是分号 `k=v` 字符串 + 明文英文相位名（`DecisionEngine.java`）。记录若干轮即可重建完整相位转移表和恢复链。
3. **facts 伪造把云端当 oracle**：本地上报的 facts 无签名，攻击者可构造输入探测云端决策边界，反推规则。
4. **license 与决策通道零集成**：工作区有 `dhxy-license-worker`，但 `grep license` 于 `DHXY/src/main/java/com/bot/dhxy/cloud/**` **零命中**——账号/许可证绑定、限流、异常检测均未接入。
5. **下发资产内存 dump**：云端下发的模板/脚本在本地内存中仍可被 dump（路线 B 也无法根除，靠轮换 + 时效抬高成本）。

### 6.2 路线 B 前置必须补的安全工程
- TLS + 证书固定（pinning）
- license/账号绑定决策端点 + 限流 + 异常检测（复用 dhxy-license-worker）
- 指令词汇不透明化（相位名/动词编码化，非英文语义词）
- facts 签名 / 会话绑定，防伪造探测
- 脚本按会话轮换、设时效

---

## 7. 分期路线（A→B，A 期即 B 前置工程，不白做）

### 第 0 期（前置基建，不动业务代码，但没它一切白做）
- cloud-brain **远端化部署**（现为 `CloudBrainServer.java:36` 绑定 `127.0.0.1`，物理只收本机连接）+ TLS + license 绑定鉴权。
- **会话持久化**（现为内存 `ConcurrentHashMap`，重启即丢，靠 RESET_REQUIRED 自救，无多实例能力）。
- **真实远端时延注入验证**（见 §8 P2：现有全部"RTT 可接受"结论都是 localhost 回环推断，非线上实测）。

### 第 1 期（清尸体 + 常量上云）
- 删 legacy 相位循环 `runRoundPhases` + 三个 `restartRoundAfterXxx` + legacy hot-start 驱动（粗估 1500-2000 行）。单笔收益最大。
- 策略常量改云端 start 响应下发：watchdog 180s、各 `MAX_*`、维护冷却间隔、handoff 延迟、探针参数。注意 `xiuluoMaxRuns` 这类次数上限留本地可被改内存绕过。

### 第 2 期（资产 + 散落规则收编）
- **模板资产云端化**（最大明文泄露面）：16+ 张模板图文件名直接暴露策略（`underfive_confirm`、`underthree_yichangqiangda`、`story_miexiu_confirm`）；另有 `images/template/xiuluo/` 完整对话**源截图**目录 + `BuildXiuluoTemplates.java`/`BuildXiuluoObjectiveTemplates.java` 生产脚本（含裁剪规则）。方案：云端会话内加密下发，或反转为本地上图、云端识别（tracker reader 已是此模式）。
- **散落 profile 规则收编**（~30 个文件夹外承载点）：NavigationService 灵兽村(11,8)直发白名单 `L1931-1943`、TaskTeamAssignmentPolicy 仅队长 `L74-78`、AutoBattleTask 召唤技能预算=2 `L263-268`、AutoCombatService 急救队列模式 `L151`、DialogService 硬编码 kanda2 ROI (264,376)-(305,397)/阈值 0.82 `L221-270`。统一为云端下发的任务 profile。

### 第 3 期（桩服务补齐 + SPLIT 项 + 原语化试点）
- 补齐云端桩服务：TASK_POLICY、TASK_RECOVERY、CAPABILITY_GATE、MAINTENANCE_THRESHOLD、TEAM_RETURN_POLICY 目前在云端只是回显/固定应答（`DecisionEngine.java:324-333,1497-1512`），名义上云、逻辑还在本地。
- SPLIT 项逐个切：hot-start 阶梯（决策上云、探测留本地）、恢复链路由、XiuluoDialogCatalog、XiuluoPhase 枚举改不透明编码。
- **原语化试点**：先挑一条最简单相位（PREPARE_ROUND 或回城）做原语化，验证脚本协议 + 时延，再逐相位搬。

### 第 4 期（全面原语化，达成路线 B）
- 通用原语解释器定型（含原语动词表——**这是整个方案的地基，B 期第一件事就是设计它**）。
- XiuluoTaskV2 执行链逐相位翻译为云端脚本，直至本地修罗专属代码归零。
- 同步把 29 个 wiring 测试迁为 cloud-brain 侧契约测试（见 §8 P2）。

**每期受业务逻辑.md R0 门禁约束**：等价迁移、不得擅自加 TTL/二次验证/park/重试/cleanup/fail-closed 或改 phase/fallback 顺序（`业务逻辑.md:215-224`）；R25 两个已知不一致项（NAVIGATE_TO_TARGET 软重开、FAILED 终态不可达）需**用户裁决后**才能统一。

---

## 8. 批判代理确认的其余缺口（reviewer 需一并裁决）

- **P2 时延全部未验证**：所有"RTT 50-300ms 可接受/已在线上验证"是 localhost 回环推断，`CloudBrainServer.java:36` 证明现网全在本机；恢复链 5-6 步串行在真实 300ms 下的累计放大、`cloud.timeout-ms=60000` 从未按远端调优——需时延注入测试作为迁移前置 gate。
- **P2 团队编排漏分类**：`TaskMaintenanceService.java:264-268,988-1150,747` 的 CR243/244/245 跨窗口共享内存状态机（战后急救队列、维护广播 FIFO、Gate A/B 数据源）被初判为"纯文档提及"，实为修罗团队编排的另一半业务逻辑。多窗口共享进程内状态，上云需云端会话跨窗口聚合，是**迁移里最难啃的一块**。
- **P2 测试资产**：61 个测试文件涉修罗，其中 `DHXY/src/test/java/com/bot/dhxy/task/xiuluo/` 29 个 wiring 测试锁死本地状态机行为，正是 R0"基线等价迁移"的验收执行手段。逻辑迁云即大面积失效，需同步迁为云端契约测试（已有 `XiuluoCloudBrainContractTest` 雏形）。
- **P3 失败案例上传通道**：`DiagnosticCaseUploaderService.java` + 工作区 `dhxy-case-worker`（Cloudflare worker）+ `application.properties:33-40` `case.upload.*`（当前 `enabled=false`）已是现成上传管线雏形，方案应复用而非新建；但上传内容含明文相位词汇（XiuluoRoundTrace 的 summary.md/events.jsonl），与"减少本地泄露"目标的关系需定夺。

---

## 9. 待 reviewer 裁决的开放问题

1. **路线确认**：终极目标 = 路线 B（原语化薄壳），A→B 分期。是否认可？
2. **第 0 期优先级**：远端化部署 + 鉴权 + 会话持久化是否作为一切迁移的硬前置（在它完成前不做业务迁移）？
3. **威胁模型接受度**：指令流观察防不死这一点，是否接受"提高成本、非归零"的定位？安全工程（§6.2）范围是否足够？
4. **团队编排上云**：CR243/244/245 跨窗口共享状态机上云是最难项，是否单列子方案 / 是否愿意接受"云端会话跨窗口聚合"的复杂度？
5. **R25 两个已知不一致项**：是否在本方案内一并裁决统一，还是维持现状迁移？
6. **测试策略**：29 个 wiring 测试迁为云端契约测试的工作量是否纳入分期，作为 R0 验收的替代执行手段？

---

## 10. 附：审计原始产出索引
- 全量分类明细（205 个方法逐条 + 30 个文件夹外承载点 + 35 条业务规则 + 6 条批判发现）见本会话 workflow `xiuluo-cloud-migration-audit` 产出。
- 覆盖核实：XiuluoTaskV2 205 个声明经三段调查逐一对照，零遗漏；与 CR232/CR253 现状一致（`XiuluoTaskV2.java:234` MAX_CLOUD_ENTER_BATTLE_FALLBACKS=3、`:4427-4436` CR253 注释、`CloudBrainServer.java:36` 127.0.0.1 绑定均已核实）。

---

## 11. Reviewer 结论区（待填写）

> reviewer 请在此写入结论（通过 / 需修改 / 驳回）与逐条 finding；作者按 heartbeat 读卡返修。

### 11.1 Codex reviewer 第一轮结论（2026-07-10）

**结论：需修改 / Changes requested。**

我认可“路线 B 作为终局、A→B 分期推进”的方向，也认可先远端化再继续拆本地修罗代码；但当前版本还不能直接作为实施方案。下面 4 个 P1 如果不先收口，实施后要么改变已经确认的修罗业务基线，要么把远端服务变成可以直接控制本机输入的通用脚本入口。

本轮实际核对范围：

- `AGENTS.md` 与 `docs/DHXY_CONTEXT.md`；
- `docs/业务逻辑.md` 的通用云端迁移门禁、修罗与五倍普通怪入战合同、修罗快捷/非快捷路线、失败 fallback 基线；
- DHXY 当前 `XiuluoTaskV2`、`cloud/xiuluo` DTO/客户端、`HttpCloudDecisionClient`、`WindowTaskRunner`、`WindowRuntimeContext` 及相关维护/导航/Runner 边界；
- 外部 `D:\mavenProject\dhxy-cloud-brain` 的 `CloudBrainServer`、`DecisionEngine`、`XiuluoBrainService`、模板资产与本地 OCR 依赖；
- `docs/PACKAGE_ARCHITECTURE.md` 中 CR230、CR232、CR243、CR244、CR245、CR253 的当前结论。

#### P1-1：路线 B 缺少“受限原语安全壳”，目前描述等价于远端通用脚本执行器

§5 允许云端下发“点击、按键、条件分支、等待”等脚本，但没有规定：允许哪些键、坐标必须落在哪个绑定窗口/ROI、单个 bundle 最多多少动作、是否允许循环、脚本能否读写文件/网络/反射、动作如何被 pause/stop 截断、签名与协议版本如何校验。

这与当前项目已确认的安全边界冲突：物理输入必须经本地 input queue，move+click 必须原子化，云端快捷键只能走 allowlist，窗口绑定必须来自当前 `WindowRuntimeContext`。若直接按当前 §5 开工，远端一旦被误配置、劫持或返回越界数据，就能控制用户桌面，而不仅是修罗窗口。

**必须修改为：**本地只解释版本化、强类型、有限长度的 action bundle；不支持任意代码、任意循环或业务条件分支。云端根据上一 bundle 的结构化 outcome 决定下一 bundle。本地保留不可由云覆盖的窗口绑定、坐标/ROI 校验、快捷键 allowlist、动作数/总时长上限、input queue、pause/stop 和 fail-closed。所有输入动作必须带 `sessionId + stateSeq + phaseToken + actionId + windowBindingGeneration`。

#### P1-2：删除 legacy 的顺序过早，且“等价证明”尚未成为可执行 gate

当前 `XiuluoTaskV2` 同时保留云脑路径与本地 `runRoundPhases(...)` 路径；配置虽然是 `xiuluo-brain.execute-percent=100`、`fallback=STOP`，但源码仍有两条执行入口。报告同时承认团队维护、部分桩服务、远端时延和若干 phase 对账仍未闭合，CR245 当前也仍有 P1。

因此第 1 期不能先按粗估行数删除 `runRoundPhases` 和恢复方法。删除只能是每个迁移切片完成后的最后动作，而且必须先证明：

1. 当前 `docs/业务逻辑.md` 对应规则已逐条映射到云端 command/outcome；
2. 本地执行器不再调用该 legacy 分支；
3. 用户 fresh runtime 的日志/截图覆盖该切片成功、失败、stop/pause、迟到结果和断云场景；
4. 没有未解决 P0/P1/P2；
5. 回滚依靠云端上一版策略与协议兼容，不依靠恢复本地业务 fallback。

**必须修改为：**先建立“基线规则 → 云端决策 → 本地原语 → outcome → 下一状态”的逐条迁移账本，再按切片删除；不得把“配置当前 100%”当作“代码已经可删”的证据。

#### P1-3：远端 session 的一致性、重连和多实例语义未定义

当前外部 brain 的 `DecisionEngine` 和 `XiuluoBrainService` 都用进程内 `ConcurrentHashMap` 保存 session，`CloudBrainServer` 绑定 `127.0.0.1`。报告提出“持久化 + 多实例”，但没有规定客户端重试时 action 是否会重复执行、服务重启后旧 token 如何处理、两个实例如何保证同一 `stateSeq` 只有一个权威结果。

**必须补齐的协议：**

- 持久化 key 至少包含 license/account、`windowId`、`taskRunId`、`sessionId`；
- 每次 transition 以 compare-and-set 更新 `stateSeq`，同一 `actionId` 的重复请求返回同一结果；
- action outcome 在云端持久化后才能推进下一状态；
- 多实例通过共享存储/租约保证单写者，不依赖 JVM 内存锁；
- 云端重启、网络超时、重复响应、旧 token、客户端暂停/恢复分别有明确状态表；
- 无法确认某个物理动作是否执行时，不得再次下发同一输入；本地停在可诊断状态，等显式恢复或新 session 决议。

#### P1-4：CR243/244/245 的实时队列不能整体迁成云端跨窗口调度器

报告把战后急救、维护 FIFO、归队 Gate A/B 归为“云端会话跨窗口聚合”。这部分确实含业务政策，但也直接控制本机 task turn、事件唤醒和物理输入串行化。若整个队列状态机远端化，RTT、断线或单个窗口掉线会阻塞全队，并让云端参与毫秒级输入公平性。

**必须采用混合边界：**

- 云端拥有“谁符合资格、优先级、预算、下一业务动作”的策略；
- 本地 `TaskMaintenanceService` / local-team session 继续拥有成员在线事实、task turn、FIFO 实际消费、事件 park/wake、输入互斥和成员失效清理；
- 本地只把结构化队列事实和 outcome 上报云端，不把本机锁或 turn lease 远端化；
- 云端不可达时不产生新的业务动作，但本地必须能释放锁、响应 stop/pause，并保证其他窗口不会永久卡死。

### 11.2 P2/P3 修订意见

1. **P2：脚本“时效”不能直接成为业务 TTL。** `AGENTS.md` 和业务基线明确禁止迁移时擅自新增 TTL。可以使用签名、nonce、协议版本和 action identity 防重放；如果 wall-clock 过期会改变当前轮 phase/fallback，则必须另开业务变更 CR 并由用户批准。
2. **P2：远端部署还缺 OCR 运行时。** cloud-brain 的 `LocalOcrClient` 默认访问 `127.0.0.1:18761`；把 jar 部署到远端后，它访问的是服务器自己的 localhost，不是用户机器。第 0 期必须明确 OCR 是随 cloud-brain 部署、拆成同网服务，还是彻底替换，不能只迁 HTTP server。
3. **P2：生产入口不能直接把 JDK `HttpServer` 暴露到公网。** `CloudBrainServer` 适合作为本地 sidecar/dev server；线上应放在受管运行时/API gateway 后，由 gateway 终止 TLS、鉴权、限流和请求体上限，应用只接受受信内部流量。
4. **P2：原语化不能把识别算法偷偷搬回本地。** 如果本地解释器下载模板并执行条件匹配，本地二进制虽无修罗代码，运行时内存仍有修罗模板和策略。终局应是本地上传受限 ROI、云端完成识别并返回受限动作；只有经用户明确批准的低时延探针可以作为通用本地 detector 例外。
5. **P2：验收策略需服从当前 no-local-test 规则。** 本方案不得默认创建、恢复、运行或引用本地自动化测试作为交付门禁，除非用户明确要求具体测试。默认 gate 是双侧 compile/package、源码/协议 review、日志与截图检查、用户 fresh runtime 证据。报告 §8 的“29 个 wiring 测试迁移”应改为可选专项，不是当前默认前置。
6. **P3：事实数据有轻微漂移。** 当前 `XiuluoTaskV2.java` 为 7249 行，不是 7171 行；这不影响结论，但说明实施前必须重新生成一次代码盘点，不能把本报告中的行号和 2026-07-08 快照当作永久事实。
7. **P3：诊断上传必须脱敏。** case 上传复用现有 worker 是合理方向，但客户端日志中的角色名、窗口标题、HWND、截图和明文 phase 必须按字段分级；服务端保留可读映射，本地发布日志可使用不透明 code。

### 11.3 作者（Claude）第一轮回应（2026-07-10）

**事实核验（作者独立复核，非转述）：**

- ✅ P3-6 属实：`wc -l` 实测 `XiuluoTaskV2.java` 当前 **7249 行**（原报告 7171 为 2026-07-08 快照，已漂移）。接受"实施前重新生成代码盘点"为 Phase 0 硬性动作。
- ✅ P2-2 属实：`DecisionEngine.java:1746/1795` 等多处调用 `LocalOcrClient.readWords(...)`，README 明示依赖 `http://127.0.0.1:18761/ocr/words`。jar 部署远端后该依赖指向服务器自身 localhost，**是原报告 期0 漏掉的真实部署阻塞点**。采纳。

**逐条回应：**

- **P1-1（受限原语安全壳）：同意，全盘采纳。** 原报告 §5 的"约十几个动词"意图与此一致，但确实没有写出强制规格（allowlist、ROI/坐标校验、bundle 上限、禁循环/禁任意分支、identity 五元组）。§12.3 的五消息最小集 + "本地不执行业务条件分支"是对 §5 的正确收口，且不损失防破解目标（本地仍无修罗逻辑）。
- **P1-2（legacy 删除顺序）：同意原则（账本 + 五项门禁），附一条不改变方案的澄清。** legacy phase loop 这个切片大概率是**最早**通过门禁的切片：CR232 已完成 phase 级等价迁移与 fresh 验证（门禁 1、3 有存量证据）、`fallback=STOP` 意味着 legacy 已无产线回退价值（门禁 5 已满足）、且 legacy 本身是本地最大的一块修罗逻辑泄露面（删除直接服务防破解目标）。即：**删除顺序由门禁裁决，不由日历顺序裁决** —— 若 legacy 切片举证齐全（含 CR245 P1 与该切片无关的证明），它可以先删。此为兼容性解读，不要求版本递增。
- **P1-3（session 一致性/重连/多实例）：同意，全盘采纳。** 原报告 期0 只写了"持久化"两字，CAS `stateSeq`、actionId 幂等、单写者租约、"无法确认物理动作是否执行时不得重发输入"均为必要补齐。
- **P1-4（团队编排混合边界）：同意。** 与原报告 §3（必须留本地清单）和 §8（CR243/244/245 误归类自纠）方向一致，§12.5 的表格是正确的最终边界。
- **P2-1（TTL）/P2-3（gateway）/P2-4（识别不回流本地）/P2-5（no-local-test）/P3-7（脱敏）：全部同意。** 其中 P2-4 与 `HYBRID_CLOUD_WORKFLOW.md` 原则 8/9 一致；P2-5 采纳后原报告 §8 的"29 个 wiring 测试迁移"降级为可选专项。

**作者结论：PLAN-R1（§12）作为实施底稿——同意。** §12 完整吸收了原报告的分析（三层结构、必须留本地清单、威胁模型天花板、A→B 分期）并修复了我方案中的全部 4 个 P1 缺口，无一处与 `docs/业务逻辑.md` R0 门禁或已确认基线冲突。

**正式 APPROVE 的唯一保留条件：** 用户已指派第三个独立视角 worker（挑战过度工程、fail-closed 可用性风险、真实公网时延、迁移失败模式）正在 review 中。其结论归并前我的状态记为 `APPROVE_PENDING_3RD`：若该 review 无 P0/P1，本行直接翻为 `APPROVE`（版本不递增）；若有实质发现，由作者合并为 `PLAN-R2` 后双方重审。

### 11.4 独立视角 worker review（用户指派，2026-07-10，由作者转录）

**总体结论：CHANGES_REQUESTED（针对 PLAN-R1）。** worker 明确认可：路线 B 终局目标、A→B 分期、§12 六阶段骨架、五消息受限原语、混合团队边界——均未挑战。并对"混淆 + native 模块 + 加密字节码"轻路径给出否定判决：那只是表示变换而非逻辑移除，JVM 运行时（javaagent/JVMTI dump）可完整还原解密后的类，且不提供 license 服务端校验挂点，**不能替代路线 B，只能作纵深补充**。5 个 P1 阻塞项：

- **P1-A 全侧车耦合未声明。** `CloudBrainServer.java:41-46` 同一网关承载 5 个端点（legacy decision / route-memory outcome / migration / npc-click-smart / xiuluo-brain）；DHXY 侧约 25 个云服务全部 `execute-percent=100 + fallback=STOP`，指向同一 `cloud.base-url`。远端化这一个进程 = **全软件所有玩法**的 fail-closed 命运一起上公网，不只是修罗。PLAN-R1 通篇未声明该爆炸半径。
- **P1-B 时延验证门禁被削弱，两个试点无法暴露真实失效。** Phase 3 的 READ_OBJECTIVE / ACCEPT_TASK_DIALOG 恰是全流程时延容忍度最高的两步（静态对话框，无竞态、无新鲜度压力），验证协议机械、不验证时序。真实失效点：①`OBSERVER_SNAPSHOT_MAX_AGE_MS=3_000`（`XiuluoTaskV2.java:220`，按 localhost 校准）在 300ms RTT + 丢包重传（TCP RTO 1-3s）下被击穿，产生"过期→重观→再过期"churn，喂给 33 次循环护栏触发 RESTART_ROUND 风暴；②云端按过期帧计算的点击坐标错点（localhost 帧龄 ~10ms → 公网 0.5-1s）；③CR232 入战仲裁竞态窗口放大百倍——协议 identity 防重放，防不了"协议合法但语义已过期"；④`业务逻辑.md:238-239` 明文"不要在回程前增加额外确认——会把快路径重新拖慢"，原语化在探针命中→回程点击间插入 ~0.6-1s 云往返，没改任何规则却精确造成基线禁止的效果，**R0 账本按规则对账，对不出物理时延回归**；⑤CR245 5s 跨窗口预算被往返蚕食。另：30s 宝箱 TTL 判定为**非**失效点（预算宽裕），纠正了审核任务书的假设。
- **P1-C 可用性/运维/吞吐预算完全缺失。** "fail-closed 已是现状"有误导性：现状的"云"是客户端自动拉起的本机子进程（`dev-sidecar.auto-start-enabled=true`），可用性≈100%；远端化后 fail 概率变两个数量级，方案通篇无定量。§12.6 两种回滚只覆盖坏部署、不覆盖宕机（宕机 = 全体付费用户停摆无预案）；无 SLO、无宕机产品行为、无 outage 续跑验收、无单人运维 runbook（证书过期/DDoS/封 IP 都在凌晨发生）、性能只测不判（无通过阈值）、云成本零估算（图像上行 + 服务端识别 CPU 随并发线性）。
- **P1-D §12.3 协议无法承载本地探针供给，与 Phase 5 自相矛盾。** §3/§5.3 承诺 kanda2、fast-exit 探针留本地、参数由云下发；Phase 5 第 2 项要求探针模板/阈值云端持有；但五消息里没有任何反向模板下发通道（`OBSERVE_ROI` 是云要图，`READ_LOCAL_FACT` 只读）。kanda2 是持续 1s cadence 小 ROI 模板匹配，模板必须常驻本地内存。要么补下发原语、要么把探针模板列为目标 1 的具名例外——PLAN-R1 两个都没做。
- **P1-E 防破解价值时序倒挂。** Phase 1 拿走 cloud-brain jar 是真收益，但被本地 legacy 层②（同一状态机的旧实现）对冲大半；PLAN-R1 把 legacy 删除与模板移除全押 Phase 5，**比原报告 §7 的排序还靠后**。做到 Phase 2 弃坑的结局 = 本地代码一行没少 + 新增原语 runtime + 持续付费远端基建，严格劣于现状。作者 §11.3 "门禁裁决非日历顺序"的澄清只在回应区，未进 §12.4 规范文本。

**P2/P3 摘录：** 多实例/单写者租约对当前规模超前设计，降为"扩容时再做"（保留"不确认执行不重发"硬约束）；账本写死"识别只允许同模板同算法同阈值的位置搬迁，算法替换单独发业务变更 CR"；补"迁移期与功能开发不并行"条款；RESET_REQUIRED ack 丢失路径（`restartXiuluoBrainAfterSessionReset`，`XiuluoTaskV2.java:700/779`）不得重发已执行物理输入须进 Phase 1 gate；游戏强更后模板热修速度从分钟级变部署周期级是运营硬伤；解释器壳自身混淆 + 协议词汇编码化作纵深；证书 pinning 用带备用公钥方案 + 轮换 runbook；核实客户端是否消费 legacy `ttlMs=1000`。

**作者事实复核（转录前独立验证）：** 5 端点共网关（`CloudBrainServer.java:41-46`）✅；sidecar 自启与 execute-percent/fallback 密度（application.properties 57 处命中）✅；`OBSERVER_SNAPSHOT_MAX_AGE_MS=3_000`（:220）、`MAX_CLOUD_ENTER_BATTLE_FALLBACKS=3`（:234）、reset 路径（:700/:779）✅；`业务逻辑.md:238-239` 原文逐字一致 ✅。**worker 的事实主张全部属实，其 P1 触发 §11.3 预设的版本递增条件，作者据此起草 §12.8 PLAN-R2 增量。**

### 11.5 作者（Claude）第二轮回应：接受 E1–E5，APPROVE PLAN-R3（2026-07-10 23:58）

**事实核验（独立复核 §12.9 的关键主张）：**

- ✅ **E1 属实**：`application.properties:48` 仅有全局 `cloud.base-url=http://127.0.0.1:18080`；`CloudDecisionProperties.java:16` 是单一 `baseUrl` 字段；`HttpCloudDecisionClient.java:120`、`NpcClickSmartCloudDecisionService.java:1055`、`RouteCloudDecisionService.java:464` 全部从同一 `properties.getBaseUrl()` 拼 URI。**按服务选择 endpoint 确为 Phase 1 硬前置**——否则把 base-url 指向远端等于 25 个服务全部陪绑公网，D1 的收窄在当前客户端实现上根本落不了地。
- ✅ **E3 与 CR232 基线一致**：CR232 复审历史上已两次修正"点击成功即清 green-chain schedule / dialogInterest"的错误——detector/schedule 必须跨 fallback 复按存续，仅真实 IN_COMBAT、stop、任务失败/放弃或新 attempt 清理。**E3 纠正了我 D4 的一个真实错误**：D4 原文"以策略版本/会话失效轮换"若照做，brain session reset/网络重连会误清 kanda2 常驻模板，违反基线。detector 生命周期绑业务 `attemptId` 而非 brain session，是唯一正确的写法。

**逐条表态：**

- **E1：接受**，进 Phase 1 compile/package 与发布物盘点 gate；拆分构建必须物理排除修罗类/资源（不只是不注册 endpoint）——同意。
- **E2：接受**。时延注入走用户 fresh runtime 或用户明确批准的专项验证，不借本计划复活本地自动化测试；3s/5s/25s 等常量重校准先判定是否业务时序变更、是则单独发卡——与 no-local-test 和 R0 门禁完全对齐。
- **E3：接受**（D4 按此修正）。预注册 detector 类型 + 参数范围、本地只跑通用 matcher 不接受动态代码、生命周期绑 attempt——全部同意。
- **E4：接受**（D7 按此收紧）。action dispatch ledger（actionId/动作类别/是否含物理输入/outcome/已接受 stateSeq）持久化，**只有 ledger 证明 `NOT_EXECUTED` 才允许重发**，是"不确认执行不重发"硬约束的可执行化；已执行或 UNKNOWN 的物理输入 fail-closed 停在可诊断状态，符合 R18（STOP 不得包装成 FAILED）；进程重启自动续跑不纳入首版、默认用户确认——保守且正确。
- **E5：接受**。我 §11.3 "legacy 大概率最早可删"属报告文字预判，E5 将其降级为"实施时以最新 CR 卡和 fresh 证据为准"——正确，预判不能替代举证。D8 收紧为"隔离开发可用本地实现、不得回流生产发布包、Phase 5 后 cloud-first"——同意；混淆/编码/pinning 不计入"业务逻辑已移除"验收——同意。

**作者结论：对 PLAN-R3（§12.1–12.9）明确 `APPROVE`。E1–E5 无一改变修罗 phase/fallback/retry/park/TTL 基线，全部是迁移协议、部署与本地安全边界的收口；无新增未解决 P0/P1/P2。**

---

## 12. 合并修订方案 PLAN-R1（供两位 reviewer 共同确认）

### 12.1 目标重新表述

路线 B 的可验证目标改为：

1. **发布物静态面：**DHXY 本地发布包中不含修罗 phase 顺序、fallback/retry 策略、识别模板、阈值、目标解析规则和修罗专属执行器。
2. **运行时面：**本地只接收当前一步所需的最小 observation/action；攻击者仍可能观察流量或 dump 当前资产，因此目标是提高重建成本，不宣称运行时信息归零。
3. **安全面：**云端永远不能绕过本地窗口绑定、input queue、坐标/按键 allowlist、pause/stop 和资源上限。
4. **业务面：**除用户单独批准的 CR 外，严格复现 `docs/业务逻辑.md` 与确认基线 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`；不新增 TTL、验证、park、retry、cleanup 或 fallback。

### 12.2 目标架构

```text
Cloud Policy Brain
  - 修罗 session / phase / retry / fallback / team policy
  - 视觉识别、模板、阈值、目标解析
  - 根据结构化 outcome 选择下一条有限 action bundle
            |
            | versioned HTTPS protocol
            v
Local Generic Runtime
  - 绑定窗口截图与受限 ROI 上传
  - 强类型 action bundle 校验
  - input queue 原子执行
  - 本地事件等待、pause/stop、安全释放
  - 结构化 outcome 与证据回报
            |
            v
Bound Game Window Only
```

本地不执行业务条件分支。bundle 内只允许短原子输入序列；命中/未命中、动作成功/失败、战斗/移动事件都作为 outcome 回云端，由云端决定下一步。

### 12.3 协议最小集合

第一版只允许以下强类型消息，不做通用脚本语言：

- `OBSERVE_ROI`：本地按窗口相对 ROI 截图并上传；本地只校验尺寸、格式和窗口绑定。
- `EXECUTE_INPUT_BUNDLE`：有限个 `MOVE_MOUSE`、`CLICK_LEFT/RIGHT`、allowlist `HOTKEY`、`SLEEP`；move+click 必须保持单个 input queue 原子序列。
- `AWAIT_LOCAL_EVENT`：只允许既有事件类型，如战斗变化、pathing terminal、prepared ready；超时语义必须来自既有业务基线。
- `READ_LOCAL_FACT`：只读、安全且已批准留本地的事实，例如绑定 generation、pause/stop、窗口是否存活。
- `REPORT_OUTCOME`：回报 `EXECUTED/NOT_EXECUTED/OBSERVED/UNKNOWN/STOPPED` 与证据摘要。

每条消息必须携带协议版本、策略版本、license/account identity、`windowId/taskRunId/sessionId/stateSeq/phaseToken/actionId`、窗口 binding generation 和幂等键。本地拒绝未知 primitive、未知字段版本、越界 ROI/坐标、非 allowlist 热键、超动作数或跨窗口命令。

### 12.4 分期实施顺序

#### Phase 0：冻结业务合同与当前事实

1. 以当前 `docs/业务逻辑.md` 为唯一业务合同，重新核对 CR230/232/243/244/245/253 的最新结论。
2. 建立逐 phase 迁移账本，字段固定为：基线前置事实、云端决策、允许的本地 primitive、outcome、下一状态、retry/fallback、pause/stop、fresh 证据。
3. 对 `XiuluoTaskV2` 及文件夹外依赖重新分类：`cloud-policy`、`local-safety-runtime`、`transitional-local`。
4. R25 两个争议项在本迁移中**保持当前已批准行为**；如要统一，单独发业务变更卡，不混入迁移。
5. 此阶段不删 Java，不改业务时序。

#### Phase 1：把 cloud-brain 变成真正可远端运行的服务

涉及主要文件/模块：

- cloud：`CloudBrainServer` 的生产替代入口、`DecisionEngine`/`XiuluoBrainService` session store、OCR runtime、部署配置；
- client：`CloudDecisionProperties`、`HttpCloudDecisionClient`、`XiuluoBrainCloudDecisionService`；
- license：复用现有 license worker/account identity，不在客户端硬编码长期 token。

实施项：

1. HTTPS/API gateway、短期会话凭证、license/account 绑定、限流和请求体上限；
2. 持久化 session + CAS `stateSeq` + action/outcome 幂等；
3. 多实例单写者/租约和服务重启恢复；
4. OCR 与模板资产进入远端运行环境，移除对用户本机 `127.0.0.1:18761` 的隐式依赖；
5. 协议版本兼容表与上一版本云端策略回滚；
6. 客户端断云只 fail-closed/可停止，不回本地业务 fallback。

Phase 1 gate：真实远端环境能完成只读 start/step/outcome 往返；重复请求不重复推进 state；服务重启后 session 按协议恢复或明确 reset；暂停/停止不被包装成 FAILED。

#### Phase 2：实现通用受限原语 runtime

涉及主要文件/模块：

- DHXY 新增通用 cloud action contract/executor，放在 `cloud/action` 或等价通用包，不放在 `task/xiuluo`；
- 复用 `InputSequences`、`WindowTaskContextHolder`、`WindowRuntimeContext`、`WindowFocusService`、`WindowReadyEventBus`；
- cloud-brain 增加同版本 action-bundle DTO 与校验器。

实施项：

1. 先落 DTO、schema/version、identity 与本地不可覆盖安全策略；
2. observation 与 input bundle 分离，截图请求绝不隐式发送输入；
3. action bundle 只支持有限 DAG/顺序动作，不支持循环、任意条件表达式、动态类、文件或任意网络；
4. 输入执行前后都检查 pause/stop、binding generation 和当前 action identity；
5. outcome 明确区分“未执行”“已执行但未验证成功”“已观察成功”“未知”，避免超时重放物理输入。

Phase 2 gate：静态 review 证明未知/越界/跨窗口命令全部 fail-closed；Java 修改完成双侧 compile/package；用户 fresh 只验证真实窗口绑定、输入原子性、pause/stop 和断云行为。

#### Phase 3：两个小切片试点，不立即拆完整状态机

1. **只读试点：`READ_OBJECTIVE` observation。** 本地只上传接任务时既有 snapshot ROI，云端解析并返回结构化 objective；必须保持“不补拍、不二次读取、不放权”的基线。
2. **输入试点：`ACCEPT_TASK_DIALOG` 单次 option。** 云端识别并返回受限点击 bundle；本地执行并把 dialog/phase outcome 回报。失败仍按当前 phase 预算，由云端决定，不在本地发明 fallback。

这两个试点分别验证“视觉只在云端”和“受限输入闭环”。试点期间现有 `XIULUO_BRAIN` 仍是 phase 权威；primitive executor 只是 phase 的执行机制，不出现第二个状态机。

#### Phase 4：按风险从低到高迁移执行链

固定顺序：

1. 接任务与 objective；
2. 非快捷路线导航与目标 NPC；
3. tracker 快捷路线、CR232/CR253 入战 prepared/fallback；
4. 战斗退出、回程、归队 Gate A/B；
5. 医宝宝/修装备等维护策略；
6. hot-start、恢复、异常归档和轮重开封底。

每个切片完成后更新迁移账本、编译双侧、做源码/协议 review，并由用户 fresh runtime 验收对应成功/失败/stop/pause/断云路径。只有该切片没有未解决 P0/P1/P2，才删除对应本地业务代码和资产。

#### Phase 5：收尾到“发布物无修罗专属实现”

1. 删除已无入口的 legacy phase loop、恢复链和修罗专属 executor；
2. 将仍需本地运行的探针抽成玩法无关 detector contract，修罗模板/阈值由云端持有；
3. 从 DHXY 发布物移除修罗模板源图、构建脚本、可读 phase/fallback 映射；
4. 本地日志保留可排障的 action/session code，明文映射与策略细节留服务端；
5. 逐项扫描 `task/xiuluo`、`service`、资源目录和配置，确认没有生产可达的本地修罗 fallback；
6. 保留上一版云端策略作为服务器回滚，不把 legacy 重新塞回客户端。

### 12.5 团队维护的最终边界

CR243/244/245 不作为“全部上云”的单体项目，而拆成：

| 能力 | 云端 | 本地 |
| --- | --- | --- |
| 谁需要补给/维护、业务优先级、预算 | 权威 | 只执行 |
| 成员在线、窗口绑定、队长关联 | 读取事实/校验 | 权威采集 |
| task turn、FIFO 物理消费、input queue | 不持锁 | 权威 |
| park/wake 与 pause/stop | 只下达已批准等待语义 | 权威执行与安全释放 |
| outcome 与下一业务动作 | 权威决策 | 结构化上报 |

### 12.6 验收与回滚

默认验收不创建或运行本地自动化测试：

- 所有 Java/协议修改必须完成 DHXY `mvn -q -DskipTests compile`；cloud-brain 按启动路径完成 compile/package；
- 两边独立源码/协议 review 无 P0/P1/P2；
- 用户 fresh runtime 记录成功主链、每类失败恢复、pause/stop、断云、服务重启、旧/重复 action、跨窗口并发；
- 日志能按 session/action identity 还原“云端决定 → 本地是否实际执行 → 验证结果 → 下一状态”；
- 远端性能按 phase 统计 p50/p95/p99，不用 localhost 推断线上 RTT。

回滚只允许两种：服务端切回上一版兼容策略，或客户端 fail-closed 停止并提示协议不兼容。不得为了回滚重新启用本地修罗业务 fallback。

### 12.7 当前推荐裁决

1. **路线 B：同意，但按本节“受限原语 + 云端做业务分支”定义，不采用任意脚本解释器。**
2. **Phase 0/1：作为后续拆本地业务代码的硬前置。**
3. **团队编排：采用云端策略 + 本地实时队列的混合边界。**
4. **R25：迁移期间保持当前已批准行为，不顺手统一。**
5. **测试：服从当前 no-local-test 规则，自动化测试只有用户明确点名时才进入范围。**
6. **安全定位：接受“提高静态逆向和行为重建成本，不能保证运行时不可观察”。**

### 12.8 PLAN-R2 增量修订（相对 PLAN-R1；作者合并 §11.4 独立视角 review 后起草，2026-07-10）

共同方案版本递增为 **PLAN-R2** = §12.1–12.7（PLAN-R1 全文）+ 本节 D1–D9 增量；冲突处以本节为准。§12.1–12.7 原文保持不动（Codex reviewer 撰写），供对照。

- **D1（收 P1-A，改 Phase 1 范围）**：Phase 1 只把 `XIULUO_BRAIN` 端点迁远端；legacy decision / route-memory / npc-click-smart 等其余端点**维持本机 sidecar**，各自独立评估后再排队远端化。为守防破解目标，Phase 1 同时**拆分构建**：本机 sidecar 构建物中移除修罗 brain 模块——修罗状态机代码不再随任何本地分发物落盘（这正是 Phase 1 的核心防破解收益，且不把其余 ~25 个玩法服务的可用性一起绑上公网）。显式声明：未来若远端化其余服务，等于全软件可用性模型改变，须逐个单独决策。
- **D2（收 P1-B，改 Phase 3）**：追加**第三个强制试点**——高时延敏感切片（CR232 入战仲裁 或 tracker 绿链点击，二选一由用户指定），在时延注入（RTT 50/150/300ms + 抖动 + 丢包）下跑真实回合。通过阈值三项：单轮耗时回归上限、3s 新鲜度门击穿率上限、RESTART_ROUND 触发率上限（具体数值 Phase 0 提案、用户批准）。**未过此 gate 不得进入 Phase 4。**同时 Phase 1 核查 `OBSERVER_SNAPSHOT_MAX_AGE_MS` 等 localhost 校准常量在远端 RTT 下的重新校准是否构成业务变更（若是，走单独 CR）。
- **D3（收 P1-C，扩 §12.6 为“可用性与回滚”）**：补齐 SLO 目标（数值待用户定）、宕机时客户端产品行为（提示“云端维护中”并可安全停止，不得包装成 FAILED，守 R18）、outage 恢复后会话续跑验收项、单人运维 runbook（证书轮换、维护窗口、告警值守）、性能门禁**具体通过阈值**（只测不判 → 测且判）、云成本估算（图像上行带宽 + 服务端识别 CPU 随并发用户线性）列为 Phase 0 交付物。
- **D4（收 P1-D，补 §12.3 第六条消息）**：新增 `PROVISION_DETECTOR`——云端向本地下发**已批准低时延探针**（kanda2、fast-exit 等）的模板与参数，带签名、策略版本、会话绑定；以策略版本/会话失效轮换，**不用 wall-clock TTL**（守 no-TTL 门禁）。发布物静态零修罗资产的目标不变；探针模板仅存运行时内存，其可 dump 性已由 §12.1 目标 2 的诚实边界覆盖。Phase 5 第 2 项与本条对齐，消除自相矛盾。
- **D5（收 P1-E，升格为 §12.4 规范文本）**：删除顺序由 P1-2 五项门禁裁决，**不由日历顺序裁决**；legacy 层②在举证齐全（含 CR245 开放 P1 与该切片无关的证明）时允许在 Phase 1/2 阶段即删除；模板资产黑箱化同理允许提前。新增硬原则：**每个 Phase 收尾必须交付可衡量的泄露面削减**（写进各 Phase gate），确保任意点停滞都不劣于现状。
- **D6（收 P2，账本 schema 追加 R0 约束）**：识别迁移只允许“同模板、同算法、同阈值”的**位置搬迁**；识别算法替换须单独发业务变更 CR（比照 R25 处理），不混入迁移。关闭“识别等价性无法逐条证明”的风险。
- **D7（收 P2，降级 Phase 1 多实例，修改 Codex P1-3 原要求）**：多实例/单写者租约/共享存储从 Phase 1 硬前置降为“扩容时再做”；Phase 1 gate 改为：单实例 + 会话持久化 + `actionId` 幂等 + `RESET_REQUIRED` 重启恢复（客户端已实现该协议）。保留硬约束：无法确认物理动作是否执行时不得重发同一输入；并显式加入验收项——ack 丢失触发 `restartXiuluoBrainAfterSessionReset` 路径时不得重发已执行物理输入。**本条修改了 P1-3 原要求，请 Codex reviewer 重审时明确表态。**
- **D8（收 P2，新增开发节奏条款）**：进入 Phase 4 后，修罗新功能开发与迁移**不并行**——新玩法先在本地快速迭代成熟、fresh 验收稳定后再排队原语化。策略参数/阈值类迭代在远端化后走云端下发，属“变快”类别，不受此限。
- **D9（收 P3，纵深与杂项）**：通用解释器壳自身做代码混淆 + 协议词汇编码化（纵深，非替代）；证书 pinning 采用带备用公钥方案，轮换 runbook 列入 Phase 1 交付物；Phase 1 核实客户端是否消费 legacy 决策 `ttlMs=1000` 字段，若消费须按远端 RTT 重新校准；游戏强更后的模板热修速度（分钟级→部署周期级）纳入 §12.6 可用性评估。

### 12.9 PLAN-R3 增量修订（Codex 对 PLAN-R2 的复审收口，2026-07-10）

共同方案版本递增为 **PLAN-R3** = §12.1–12.8 + 本节 E1–E5；冲突处以本节为准。

- **E1（批准 D1，但补远端/本地端点路由硬门）**：Phase 1 只远端化 `XIULUO_BRAIN`，其余约 25 个服务继续指向本机 sidecar，这个收窄正确。但当前 DHXY 只有全局 `cloud.base-url`，`HttpCloudDecisionClient` 对所有 `serviceId` 共用同一 base URL；因此 Phase 1 必须先支持按服务选择 endpoint/transport，并保证 `XIULUO_BRAIN` 的 start/step/action-outcome 三类请求全部走同一个远端身份域，其余服务不被误切公网。cloud-brain 的拆分构建必须从本机 artifact 中物理排除修罗状态机类/资源，不能只是不注册 endpoint。此项进入 Phase 1 compile/package 与发布物盘点 gate。
- **E2（批准 D2/D3，但性能门不得暗改业务）**：第三个高时延敏感试点保留，且应优先选择 CR232 入战仲裁，因为它同时覆盖静态云端识别、prepared action、attemptId、迟到结果和 fallback。RTT/抖动/丢包注入必须作用于用户 fresh runtime 或用户明确批准的专项验证，不因本计划自动恢复本地自动化测试。3s 新鲜度、5s 维护预算、25s probe 闸门等若需要修改，必须先判定是否改变现有业务时序；属于业务变化时单独发卡，不能以“性能校准”名义直接改常量。SLO/成本/runbook 是部署 gate，不是新的 task phase、retry 或 TTL。
- **E3（批准 D4，但 detector 生命周期绑定业务 attempt，而不是普通 session 轮换）**：`PROVISION_DETECTOR` 只允许预注册 detector 类型、ROI 空间、算法版本和参数范围；本地仍执行通用 matcher，不接受云端动态代码。对 CR232 的 kanda2，已 provision 的 detector 必须从当前绿链 `attemptId` 生效，跨云端 fallback 复按继续有效，只在真实 `IN_COMBAT`、stop、任务失败/放弃或新 attempt 明确替换时清理；不得因网络重连、brain session reset、策略轮换或普通 command cycle 提前停止。签名/策略版本用于完整性与替换，不形成 wall-clock TTL。
- **E4（修改 D7：同意单实例首发，拒绝把 RESET_REQUIRED 当作无条件重启恢复）**：多实例、共享租约可推迟到扩容阶段；Phase 1 允许“单实例 + 持久化 session”。但持久化范围必须包含 action dispatch ledger：`actionId`、动作类别、是否含物理输入、客户端 `NOT_EXECUTED/EXECUTED/OBSERVED/UNKNOWN` outcome、已接受 stateSeq。网络 ack 丢失时服务端必须按同一 `actionId` 返回幂等结果或要求客户端查询 ledger；只有能证明动作 `NOT_EXECUTED` 时才允许重新下发。对于已执行或 UNKNOWN 的物理输入，`restartXiuluoBrainAfterSessionReset(...)` 不得重发、不得从 phase 起点猜测恢复；应 fail-closed 停在可诊断/人工恢复状态。`RESET_REQUIRED` 只允许用于“尚无在途物理动作”或“ledger 已证明可安全 reset”的场景。客户端进程重启后的自动续跑若没有本地持久执行凭证，不纳入首版，默认重新启动任务前由用户确认。
- **E5（批准 D5/D6/D9，收紧 D8）**：legacy/模板可在任意 Phase 提前删除，但每个切片仍必须满足 P1-2 五项门禁；当前 CR232/CR245 的历史状态不得被报告文字预判为已满足，实施时以最新 CR 卡和 fresh 证据为准。迁移期间新修罗功能可以在隔离开发环境先用本地实现验证，但不得重新进入面向用户的生产发布包；进入 Phase 5 后一律 cloud-first，不再新增本地修罗业务实现。代码混淆、协议编码和 pinning 仅作纵深，不计入“业务逻辑已移除”的验收。

### 12.10 Codex reviewer 对 PLAN-R2 的第二轮结论（2026-07-10）

**结论：PLAN-R2 需修改；上述 §12.9 已形成 PLAN-R3，Codex reviewer 对 PLAN-R3 明确 `APPROVE`。**

复审依据：独立 worker §11.4、作者 §12.8、当前 `CloudDecisionProperties`/`HttpCloudDecisionClient` 的单 base URL 实现、`XiuluoTaskV2.restartXiuluoBrainAfterSessionReset(...)` 与 action-outcome identity、CR232 detector/schedule 生命周期，以及 no-local-test/R0 门禁。

对 D1–D9 的表态：D1、D2、D3、D4、D5、D6、D9 在 E1–E3/E5 约束下批准；D7 的“单实例首发”批准，但“普通 RESET_REQUIRED 即可安全重启”必须按 E4 改为 ledger 证明后才允许；D8 按 E5 收紧为迁移期隔离开发策略，不能重新污染生产发布物。PLAN-R3 不改变修罗 phase/fallback/retry/park/TTL，只补迁移协议、部署和本地安全边界。

### 12.11 PLAN-R4 增量修订（用户裁决：远端部署推迟到最后，2026-07-11）

共同方案版本递增为 **PLAN-R4** = PLAN-R3 + 本节 U1–U5。本增量由**用户（最终决策人）直接裁决**：软件当前仅用户本人使用、无对外分发，防破解收益不急于兑现，整个迁移期间不购置云服务器。U1–U5 只调整实施顺序，不改任何业务规则与安全边界。

- **U1（裁决内容）**：实际远端部署（VPS 采购、TLS/gateway 上线、license 接入生产、真实公网验收）从 Phase 1 移出，成为**最终期（Phase 6）**。在此之前 cloud-brain 一律以本机 sidecar 形态运行和开发。
- **U2（Phase 1 改为"可远端化改造"）**：原 Phase 1 的工程项保留但对本机开发验证——按服务选择 endpoint（E1 路由）、session 持久化 + action dispatch ledger（E4）、协议版本化、拆分构建（本机 sidecar 构建物物理排除修罗模块的能力先建好，最终部署时直接启用）。目标：最终部署日只是"换个地址 + 开 TLS"，不是新工程。
- **U3（时延风险仍然前移，不等最后）**：D2/E2 的高时延强制试点保留在 Phase 3，改用**本机时延注入**执行（人为 RTT 50/150/300ms + 抖动 + 丢包，不需要真实服务器）；通过阈值照旧须用户批准。真实公网 p50/p95/p99 验收留在 Phase 6。**未过本机注入 gate 不得进入 Phase 4，此约束不因部署推迟而放松。**
- **U4（D5 泄露面原则的知情调整）**：用户独占使用期内，"每期可衡量的泄露面削减"以 **DHXY 发布物中修罗代码/资产的删除**计量；修罗逻辑聚合于本机 cloud-brain jar 是**用户知情接受的过渡状态**（防破解收益在 Phase 6 部署时一次性兑现）。**触发条款：一旦软件开始对外分发，Phase 6 立即升为最高优先级。**
- **U5（其余约束照旧）**：E3 detector 生命周期绑 attempt、E4 ledger 证明 NOT_EXECUTED 才重发、R0 等价迁移、D6 识别只做位置搬迁、no-local-test、fail-closed/R18——均与部署位置无关，全部不变。

---

## 13. 双 reviewer 共识记录

本报告的共同方案版本当前为 **`PLAN-R4`**（= PLAN-R3 + §12.11 U1–U5，用户裁决增量）。只有 Codex reviewer 与 Claude（作者）都对同一个版本明确写入 `APPROVE`，且该版本没有未解决 P0/P1/P2，才算达成共识。

| Reviewer | 针对版本 | 结论 | 说明 |
| --- | --- | --- | --- |
| Codex reviewer | `PLAN-R3` | `APPROVE` | 已复审 §11.4 与 PLAN-R2 D1–D9；E1–E5 补齐服务路由、detector 生命周期、RESET_REQUIRED/action ledger 与生产发布边界后，无待解决 P0/P1/P2。 |
| Claude（作者） | `PLAN-R3` | `APPROVE` | 已复审 §12.9 E1–E5 并逐条接受（见 §11.5）；E1 单 base-url 事实经代码独立核实属实，E3 纠正了 D4 的 detector 生命周期错误。无新增未解决 P0/P1/P2。 |
| 独立视角 worker（用户指派） | `PLAN-R1` | `CHANGES_REQUESTED`（已吸收） | 一次性委托，已完成。P1-A~E 与全部 P2/P3 已转化为 §12.8 D1–D9；worker 明确认可路线 B 终局、六阶段骨架、五消息原语与混合团队边界。 |

若另一 reviewer继续修改方案，应递增 PLAN 版本并列出相对上一版的变化；Codex reviewer heartbeat 只审最新版本，不沿用旧结论。

### ✅ 共识达成记录

**2026-07-10 23:58（本地时间）：Codex reviewer 与 Claude（作者）均对 `PLAN-R3`（= §12.1–12.8 + §12.9 E1–E5）明确写入 `APPROVE`，无未解决 P0/P1/P2 —— 双方共识达成，PLAN-R3 为最终实施底稿。** 演进链：原报告 §1–§10 → Codex 第一轮 4P1+7P2/P3（§11.1–11.2）→ PLAN-R1（§12.1–12.7）→ 独立视角 worker 5P1（§11.4）→ PLAN-R2 增量 D1–D9（§12.8）→ Codex 第二轮 E1–E5（§12.9–12.10）→ PLAN-R3 双 `APPROVE`。后续任何修改须递增 PLAN 版本并重新走双 APPROVE。实施启动前置：Phase 0 的账本、SLO/成本估算与阈值提案均需用户批准后方可动工。

**2026-07-11 用户裁决增量：`PLAN-R4`（§12.11 U1–U5）——远端部署推迟为最终期（Phase 6），迁移全程对本机 cloud-brain 开发，本机时延注入 gate 不放松。** 该增量由用户直接批准生效（用户为最终决策人，且增量不改任何业务规则/安全边界，只调顺序）；是否需要 reviewer 对 U1–U5 补一轮确认，由用户安排。
