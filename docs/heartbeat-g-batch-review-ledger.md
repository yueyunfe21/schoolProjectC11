# G 批次修复/评审 heartbeat 台账（2026-08-29 起）

维护者：Claude（本会话 heartbeat）。用途：持续检测 8 卡写集的修复推进与评审状态变化。
**tick 纪律：只读 git / 禁 mvn / 禁进程操作；有变化才报告，无变化静默。**

## 被监视的卡与关注点

| 卡 | 当前状态（2026-08-29 外部评审 PASS P0=0/P1=0/P2=3） | heartbeat 关注 |
|---|---|---|
| G115 | 完成+现场验证（01:10:58 clamp 实锤） | 状态回退 / 文件哈希变动 |
| G107 | 代码完成、合同 12/12，缺正式 fresh | fresh 结果；哈希变动；evidence-wait 现场日志首例 |
| G122 P1-3/P1-4 | 完成、合同全绿；defer 编码正名待拍板（清单#2） | 正名同单动工；NpcClickMemoryStore/SmartQueueStore 变动 |
| E70 | 代码完成 **未上线**（运行 class 无 isQuietMemberFrameSuppressed） | 重启后字节级复查；首个 MOVEMENT_OBSERVED |
| E71 | 代码完成（death-marker-gone 清 pending） | pending 单个注销首例日志 |
| G116 | 核心返修完成；G121 基线红另立卡 | 卡收尾/关闭；G121 修复动工 |
| G117 | 已上线（class 字节验证 template= ×5）；追认待拍板（清单#3） | 首个 template= 命中行；追认结果 |
| G108 | RE-DELIVERED 待 Codex REVIEW #2；P1-2 判据偏离待裁（清单#6） | REVIEW #2 结论落卡；三生产文件哈希变动 |

## 评审快照基线（哈希变 = 有新修复落地 = 需要重审）

```
f1589473e902601d dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/AutoBattleTask.java
8b950eaeadbb8719 dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeaderDeathRecoveryService.java
42f412be67edd174 dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java
8ec869bbe145316d dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/guiwang/GhostKingTask.java
f31325627082cb3b dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/dalisi/DalisiQuizTask.java
25b76e4bc1a4d69e dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java
aefa45a3c2f27662 dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationHttpHandler.java
f096970e25133e5d dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java
937f17c39e8e1051 dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java
b134ab9d472e2e6c dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/LeaderTeamReturnCoordinator.java
d595abc13d054dd8 dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TeamReturnZhaoWatchState.java
6a38910099af2d29 dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java
a8f538739a851135 DHXY-cr271/src/main/java/com/bot/dhxy/service/DialogService.java
a85045b908a79ade DHXY-cr271/src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java
54223b1fdf3b2713 DHXY-cr271/src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java
2790d0dda48e7e0a DHXY-cr271/src/main/java/com/bot/dhxy/service/UICleanerService.java
885f840d5e46f457 dhxy-cloud-brain/src/test/java/com/bot/dhxy/task/G107MemberDeathEvidenceGateContractTest.java
e90315e7d59936c0 dhxy-cloud-brain/src/test/java/com/bot/dhxy/service/G107LeaderDeathEvidenceWaitContractTest.java
cebdabe31c1c3118 dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/NpcArrivalFrameUnlockContractTest.java
85b9418c45a1efe2 dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/G115TrackerPanelEdgeClampContractTest.java
4b9027b63c7bd2e1 dhxy-cloud-brain/src/test/java/com/bot/dhxy/service/TeamReturnZhaoWatchGateContractTest.java
9d04246151758a6b dhxy-cloud-brain/src/test/java/com/bot/dhxy/task/dalisi/G122OfficialDialogEntryContractTest.java
aa7eaa479db5be67 DHXY-cr271/src/test/java/com/bot/dhxy/cloud/turn/local/G122DeferredNpcClickVerificationContractTest.java
fd1734bd9225c788 DHXY-cr271/src/test/java/com/bot/dhxy/window/observation/MemberReturnLegSharedFrameContractTest.java
```

## 文档基线（mtime/行数变 = 有新评审/交付记录）

- docs/PACKAGE_ARCHITECTURE.md
- docs/ACTIVE_WORK.md
- docs/云端迁移常见错误清单.md

## 待拍板事项监视（HANDOFF 清单，变化即报）

1. lib-machine-paths.ps1 未入 git；2. defer 编码正名；3. G117 追认+npc_busy_cancel.png 去留；
4. MemberReturnPending 孤儿策略；5. G106 换机验收门；6. G108 P1-2 判据偏离。

## Tick 记录

- 2026-08-29 建账。基线=上表。运行实例：cloud PID 40840 / client PID 71264（11:37 起，禁碰）。
- 2026-08-29 三条 P2 由 mavenproject-45 关闭。P2-1 修复致 TaskMaintenanceService.java
  `da4dc6ea18947be5→42f412be67edd174`（2526L→2538L），系评审处方本身（wait 分片 1s，deadline/while/
  唤醒语义零改动，capability-wait 惯例未动并注明另开卡）；评审人已亲核 hunk 并独立复跑 G107 两套
  合同 7/7+6/6 全绿，**该漂移已批准并纳入基线**。P2-2/P2-3 为纯文档落地（G116 卡补记、G122 卡追加
  论据）。另据 45：今日栈被第三方重启六次，最近 11:37，该批产物含 G107/E71/G115/G117，E70 仍未上线。
- 2026-08-29 heartbeat（cron 453d25d0）**由用户明令关闭**。台账保留作为评审快照与状态存档。
- 2026-08-29 用户改组协作模式：heartbeat 已关，改为**双人事件驱动闭环**——mavenproject-45 实现、
  mavenproject-3d 评审，SendMessage 互通直至清单完成。范围（用户口述）：G122 到达帧与假成功 /
  G121 基线红调查 / G118（方案先送用户批准）/ G113 新根因 / G107 二审两 P1 返修 / G099 fresh 返修；
  **G065 用户裁定暂不做**。每卡交付→隔离复审（哈希绑定+diff+连通性+复跑合同）→findings 回传→
  循环到 0/0/0。工单 msg_id=2b5d24c3。
- 2026-08-29 G107 二审两 P1 复审（mavenproject-3d）：**双双关闭**。P1-1=reportReturnInFlight 于
  clickReturnButton 之前（TeamReturnService:191），仅同腿移动/到达清除，NO_MATCH/CLICKED_NO_MOVEMENT/
  超时/异常均保留 pending；MemberReturnInFlightTimingContractTest 4/4 独立复跑绿。P1-2=acceptMissFrame
  纯时间间隔（900ms 稳定期+250ms 分片），指纹只记日志不拦计数；HUD 交叉门 PRESENT 证伪清零/
  UNREADABLE fail-closed；TeamReturnPanelProbeDecisionContractTest 9/9 绿（同窗口静止正例+跨窗帧仅负例）。
  **新开 P2（非阻断）：CaptureResult 无 generation/capturedAt——整条 PrintWindow 管线冻结时面板与 HUD
  两腿会同时读到陈旧像素而误收门；原退修文"可审计新鲜 capture 身份"严格未做到。建议补单调捕获代
  并要求两张负帧携不同代；在 G107 fresh 验收前补齐，或由用户/Codex 明示豁免。**
  卡片滞后属实（今日第三例），已要求 45 更新 G107 卡状态。
- 2026-08-29 用户台账重组（356 条，PACKAGE_ARCHITECTURE.md:12294）：G065/G067 **DEPRECATED**（由
  G078 面板完成门 + G108 HUD 持续观察门替代——"恢复 veto"路线正式作废，E70/E71 作为独立缺陷保留）；
  G107 收窄为"死亡位置晚到+20s 事件等待"（本台账上一条的两 P1 复审结论恰好覆盖此范围；**新开的
  capture 身份 P2 随半帧范围划转，应记在 G078 名下**）；G113 保留原范围，NPC 入口划归 G122（升格
  "大理寺NPC入口四重失真"）；G099 截图风暴归 G103，不再返修；G116 基线红由 G121 接管；G114 外溢
  两问题归 G119/G120。**G118/G119/G120 用户已批准、必须完成**——G118 的"批准前不许动生产代码"
  禁令解除。双人闭环队列更新为：G122(四重失真) → G113 → G121 → G118 → G119 → G120。
- 2026-08-29 傍晚：实现方会话（45/73）已关闭、闭环断裂三小时，用户令 mavenproject-3d 接手实现。
  **G122 P1-1+P1-2+合同门⑤取证通道已交付**（细节/SHA 见 PACKAGE_ARCHITECTURE §G122 交付记录）：
  P1-1=fresh 入容差短路（8/8）；P1-2=CONTINUATION 显式续帧状态（协议双仓 `2840743b`）+attemptIndex
  有界+FIFO 不烧槽（6/6）；取证=到达帧滚动落盘池 40。回归：unlock 6/6、store 家族 11/12（G121 红
  未恶化）、client defer 9/9+E70 5/5+duty 6/6。**角色冲突声明：本交付实现方=原评审方，自审不算
  Approved，待 Codex/返场队友外部评审。** 队列剩余：G113 → G121 → G118 → G119 → G120。
- 2026-08-29 用户纠正分工：mavenproject-3d **只做评审,绝不再实现**(G122 P1-1/P1-2 越界交付待处置:
  用户二选一=保留待第三方评审 / 令撤则精确回退)。新 heartbeat `5185346c`(*/7min)职责=盯实现方:
  ①发现新实现方会话即送工单 ②每拍问进度/疑惑/所需帮助(asked-pending 不连环催) ③差分两仓+文档,
  有交付立即隔离复审到 0/0/0 ④台账记 Tick,实质进展或需拍板才报用户。队列:G113→G121→G118→G119→G120。
- Tick 18:29 quiet：实现方缺位（仅 19 在线,18:2x 的状态询问 asked-pending 不重催）；两仓 dirty 基数
  cloud=45/client=127 条（含既有各卡改动,作为下拍差分基线）；文档无新交付（PKG 17:13=我自己的 G122 记录）。
- Tick 18:36 quiet：无新实现方；19 仍未回复（已承诺不再催它,等用户开新会话）；两仓 dirty 45/127 与
  基线一致,文档无变化。
- Tick 18:43：**新实现方 mavenproject-29 上线(18:39 起)**,完整工单已送(msg 64721e4f:队列 G113→G121→
  G118→G119→G120+铁律+交付格式+G122 勿重复修),等认领回执;两仓 dirty 45/127 无变化。
- Tick 18:5x：**29 号认领并交付 G121,复审 PASS(0/0/0)**。结论=合同错非生产错:用例把 countDown 排在
  会 park 的生产者屏障 push 之后,自锁死;修=入口 countDown(与兄弟用例同惯例)+300ms 有界静置窗口
  ("入队了也不许醒",断言更强)。独立复证:test `26850d105e1288a7`/753L、.gitignore `037261959cbd3080`、
  生产 store `52856af2` 零改动、check-ignore 退出 1、**12/12 首次全绿**。观察项(非阻断):变异 B 未得
  结论,如实记录。**白名单坑第四例=被吞的是既有未跟踪文件**,复审判据已扩展:改动过的既有测试也查可见性。
  **G113 核销确认**:卡状态 REVIEW#5 APPROVED/P0=P1=P2=0,唯一新根因已随 G122 完成,队列删除。
  29 获准进 G118。
- Tick 18:51 quiet：29 在做 G118（数分钟前刚获准，不打扰）；cloud dirty 45→46（+1=其 G118 在途改动，
  正常），client 127 持平；无新交付。
- Tick 19:0x：**29 交付 G118,复审 PASS(P0=0/P1=0/P2=1)**。实现确认本已完整(修A :3066/修B :2931/
  helper :3309),缺的跨轮时序合同已补(迟到 rearm 不得夺回下一链已激活的唯一槽)。独立复证:test
  `67ea9312147844d4`/204L、observer `937f17c3` 与评审快照逐字节一致、check-ignore 1、**3/3 全绿**、
  变异双向全杀。**P2(覆盖边界裁定)**:不要求重脚手架驱动 observer 生产链;按仓内既有 node 源码合同
  技法(g103/g108 先例)加轻量断言钉住 :2931/:3066 两处调用点在源码中存在,防"删调用点漏杀"。
  卡片滞后第五例(正文落后源表)已由 29 订正。29 获准进 G119(P2 可与 G119 并行补)。
- Tick 18:56 quiet：29 在做 G119+并行 G118-P2（刚审结派活，不打扰）；dirty 46/127 持平；无新交付。
- Tick 19:01 quiet：29 在做 G119（client dirty 127→128,+1=在途改动,修罗任务在云仓但合同/脚本可能落
  client,正常）；无新交付,不打扰。
- Tick 19:08 quiet：29 在做 G119（PKG 卡文 +3.8KB=正在写调查/交付记录）；dirty 46/128 稳定；未收到
  交付消息,不打扰。
- Tick 19:15 **阻塞：mavenproject-29 会话消失**（G119 进行中未交付；其 G119 卡文调查记录 +3.8KB 与
  dirty 46/128 均留在盘上,工作未丢,只丢了工人）。已报用户请重开实现方会话;heartbeat 发现新会话
  将自动送工单+G119 续作上下文。
- 19:2x 用户指示评审方自行派 agent,不再依赖外部会话:已起后台实现子代理接手 G119(续作前任留在
  卡文/工作树的半成品)。**防重复:G119 现由子代理持有——若有新外部会话出现,工单从 G120 起派,
  勿再派 G119。**子代理完成报告到达后照常独立复审。
- 19:3x：**G122 外部复审 RETURN(P1×1+P2×3,原文在 §G122 line~96)**——P1=defer 上报仍掩码成
  VERIFIED+success=true 违合同门④(即此前"待拍板#2"的正名,复审强制拍板);P2=fresh 合同只测 helper/
  CONTINUATION 缺客户端消费面/证据池在拒绝门前写入。已派第二个实现子代理返修(跨仓正名同单,
  含旧编码消费点核查清单);修完交回原外部评审人二审,我不自审本卡。**在飞子代理:G119 一个、
  G122 返修一个,文件面无交集(已在工单声明范围纪律)。**
- 19:5x：**子代理交付 G119,复审 PASS(P0=0/P1=0/P2=0)**。核实:前任仅留调查未留代码,续作声明属实;
  新 XiuluoIncidentalCombatBudgetClock `4a3494cd`/185L(边沿事件差计时/latch-once/上限续期/宁少补),
  XiuluoTaskV2 `ca0dfbc1`/5707L 四处接线(退战边沿先结算,UI清理/分流后置;run+accept 双复位),
  expected 永不上闩沿用 Runner 权威前缀;test `38f21365`/317L(含事故逐毫秒回放 82 086ms),
  check-ignore 1;**xiuluo 整包 34/34 独立复跑绿**,变异 4/4(交付方报,抽核逻辑成立)。
  **待用户拍板:每次接取的补偿上限 300 000ms(事故用量 3.6 倍,改值一常量)。**
  G120 已派给同一子代理续作。
- 20:0x：**子代理交付 G120,复审 PASS(P0=0/P1=0/P2=0)**。核实:XiuluoTaskV2 `af86209e`/5769L(+62),
  三出口单一决策源+record 构造器 XOR 不变量(零载体=事故/双载体=禁止的二次恢复,构造期即拒);
  两个 resume 出口就地 ensure(复用既有 ensureSheYaoXiangActiveForLeaderTask,语义零改),title-absent
  出口沿用 CLIENT_RUNNER_EXIT 延迟恢复(安全点恰好一次);共享服务 diff=0;test `cb0685a7`/155L,
  check-ignore 1;**xiuluo 整包 40/40 独立复跑绿**;.gitignore 与 G122 代理并发交错干净。
  **待用户拍板(形状取舍):**当前=严格"三出口各恰好一次香检"(ensure 在 ≤2s title 探测之后);
  鬼王同款="退战第一件事查香"但 title-absent 出口会二次探查(或需重开已撤回的 AutoCombatService
  标志)。二选一,均小改。队列本体全部完成,仅剩 G122 返修在飞。
- 20:3x：**G122 四条返修交付,QA 抽验全绿**(我回避正式评审,QA 不算 Approved):P1 正名=掩码删除、
  客户端如实报 DIALOG_OPEN_UNVERIFIED、云端 enrich 打 TASK_PHASE_DEFERRED+success=false(连带封死
  success 陷阱)、registerDeferredPending 认新编码且保留天庭打戳 VERIFIED 真证据路径;六个旧编码消费点
  全清单核查;P2×3=node 接线合同 29/29+客户端消费面真执行器合同 2/2+证据池移到拒绝门后 1/1。
  QA 复现:cloud 28/28(抽)、client 9/9+2/2、node 29/29、协议双仓仍 2840743b。附带收获:
  NpcArrivalFrameQueueStoreContractTest 12/12(G121 修复后持续绿)。**QA 自坑记录:复跑他人改动时
  自己 scratch 的旧同名类未重编译+编译 -cp 漏挂 scratch → 协议类解析到运行面旧版 → 编译失败留旧
  字节码假红;grep 计错误数还会被乱码骗过。判据:跑前 grep 关键符号在 .class 字节里的存在性。**
  待办:G122 整卡交回原外部评审人二审;待拍板清单见用户汇报。
- Tick 20:03 quiet：队列全收官(G113核销/G121/G118/G119/G120 PASS/G122返修QA绿)。dirty 50/130 与
  两个子代理的已审交付一致,无新动静。**等待面:①用户触发 G122 外审二审 ②三项拍板(G119上限300s/
  G120香检时序/枚举EOL归一) ③重启双端→统一 fresh。**heartbeat 转入守望模式:只看新会话/新改动/外审结论落卡。
- Tick 20:08 quiet：守望无变化(dirty 50/130 持平,无新会话)。
- Tick 20:15 quiet（连续第3拍无变化）。
- Tick 20:22 quiet（连续第4拍）。
- 20:3x：**G122 二审 PASS(P0=P1=P2=0,原 P1×1+P2×3 全关)**——外审独立复跑接线合同 29/29;defer 不再
  伪装 VERIFIED/success=false、结算链未断、接线被钉、消费面+有界换帧覆盖、拒收帧不进取证池。整树
  testCompile 被旧测试编译错挡是既有写集外漂移(外审如实登记,非 G122)。**至此本批全部卡代码面+评审面
  双收官**:G113核销/G121/G118/G119/G120/G122(二审) 全 PASS。剩余=统一 fresh(需重启双端)+三项拍板
  (G119上限300s/G120香检时序/枚举EOL)。heartbeat 守望至 fresh。
- 20:4x：用户授权实施 **G123**(恢复 forceCloseDialog/同帧快照清障,19:46 事故卡,方案冻结六步+合同门
  八条)。已派实现子代理,工单含:冻结名字原名恢复、G019 覆盖裁决与 G117 保留裁决、XiuluoTaskV2 在
  G119/G120 终态(af86209e)上增改且不得动其代码、事故帧只复制不移动(images 铁律)、嵌套 turn 边界。
  交付后我独立复审。
- 22:1x：**G123 交付(时间盒催收后诚实交卷),复审 PASS(P0=0/P1=0/P2=2,均非阻断)**。独立复证:
  13 文件哈希绑定(抽 9 全中)、四个云端新/改测试 check-ignore 全 1、G123SuppliedFallbackLastOption
  7/7、CloudUiCleanerStoryPages 11/11、client 执行器 6/6+G122 适配 2/2、**修罗整包 40/40 零退化**
  (G119/G120 代码 9 处标记完好)、双仓编译 0 错。fixture 四张全复制零移动(images 铁律遵守)。
  连通性三问有实据:旧 handled 无视觉消费方、协议零改、无嵌套 turn;写集外增改(unknown-phase 双
  presence 兴趣改全任务无条件发布)属"兴趣没发布"清单①类的正确修复,已申报——**fresh 观察项:
  G103 采样节奏不得因此回潮**。报告所列全部红=基线既有(逐类与基线对照,未恶化),含 1 项
  UiCleanerTurnContractTest 既有挂死(修复量~1h,另行处理)。
  **P2×2(转拍板):①19:46 型单行居中提示框 supplied 分类=NONE——不误点不放行(事故语义已修)但
  不会被自动关,任务将停在清障循环,自动关需另卡调分类器;②6 行高接任务框的 optionRowsRect 下沿
  不含视觉最后一行,fallback 会点行区内最后行(业务选项)而非礼貌退出,建议另卡扩行区或关闭词优先。**
- Tick 22:00 quiet：G123 已审结,dirty 57/132 与其交付一致;七卡全收官。等待面:用户拍板(G119上限/
  G120时序/EOL/G123两条P2)+重启双端统一 fresh。
- 22:2x：**G123 用户终审:两条 P2 升级为 P0×2,打回重做**。用户判据(新冻结标准):①单行居中提示框
  就是 story dialog,story 检测必须全框识别并点掉(废除 y345..387 窄带假设);②最后选项必须从
  dialog 框体最下沿向上找(废除 optionRowsRect 下沿假设;G019 安全靠框体边界保证,3511 负回归必须
  仍红变)。返修工单已下达原实现子代理:两块判定逻辑删掉重写,链路骨架(零CAPTURE/自然拍/NPC门/
  G117门/冻结名)保留;判据达标才交(事故帧关得掉/6行框点到"没事路过")。
- 22:3x 评审标准自纠(用户指正):**清障卡的验收=障碍被清除,不是"没乱点"**——"安全地卡住"不是及格线,
  此前把两条关不掉框的缺陷判 P2 属评审错误。思路层判据同步修正:废除一切"固定坐标带/固定行区"布局
  假设(与 E67/G111 同病:假设代替观察),一律"真实框体为界、内容自证":框底向上找绿行=OPTION 点最底行,
  无绿行有白字=STORY 框内点,护栏=框体边界而非行区。G123 返修按此验收。
- 22:4x：**用户否决 G123 实施,不再重做**。回退令已下达原子代理(最后一单):13 个文件精确撤销,
  5 个关键文件按动手前哈希验收(UICleanerService 2790d0dd/XiuluoTaskV2 af86209e/Observer 937f17c3/
  G122消费测试 749b9c30/.gitignore 6e9985f7),新增测试/fixture/replay 整体删除,卡状态回
  NO CODE / IMPLEMENTATION REJECTED;回退后验证=哈希吻合+双仓编译0错+修罗40/40+G122抽跑。
  回退报告到达后我逐项复核。**已收官七卡(G113/G118/G119/G120/G121/G122二审)不受影响。**
- 23:0x：**G123 回退完成并经评审方独立复核通过**:5 个关键哈希逐一亲测吻合(2790d0dd/af86209e/
  937f17c3/6e9985f7/749b9c30),两仓 images 零 g123 残留,新增测试/replay 目录已删,修罗整包
  **40/40 回退后复跑全绿**(G119/G120 无损),双仓编译 0 错。卡状态=NO CODE / IMPLEMENTATION
  REJECTED(卡文规格保留,用户两条判据已记录备将来重做)。G123 子代理任务终止。
  **最终盘面:G113核销/G118/G119/G120/G121/G122(二审)六卡收官有效;G123 否决出局。**
- Tick 22:16 quiet：G123 回退后盘面稳定(dirty 50/130,与回退终态一致);六卡收官有效;等待用户拍板+fresh。
- Tick 22:22 quiet（连续第2拍）。
- Tick 22:29：新会话 mavenproject-4e 上线。队列已空,未派工单;已送盘面通报(六卡收官/G123 否决勿碰/
  铁律),询问其任务线,asked-pending。仓面 50/130 稳定。
- Tick 22:36：又一新会话 mavenproject-77 上线,已送同款盘面通报(asked-pending);4e 尚未回话(不重催)。
  仓面 50/130 稳定。
- 22:4x：77(自称原名 2e,G117/G118 原实施者)回话:无在途任务、确认 G118 闭环、G123 勿碰已记、
  保持只读不撞车;其悬置三项确认已被体系消化。备查资源:G117/G118 逐 hunk 边界与事故帧语料出处
  可找它。4e 仍 asked-pending。
- 22:5x：**heartbeat(5185346c)由用户明令停止**。台账封存。终态:G113核销/G118/G119/G120/G121/
  G122(二审) 六卡收官;G123 否决已回退;待用户拍板三项+重启 fresh。
- 23:0x 封存后补录(被动收件):19 号(G114 线主)迟到回话,三条只读旁证——G114 回退保持(四文件
  diff=0)、XiuluoTaskV2=af86209e 且方案B/G109 完好、**G119/G120 实现对已撤回并发设施
  (nextExactCombatEntry/ackCombatEntry/CombatEdgeOwner/Overflow)零命中**=第三方确认未复活。
  其建议"未复活撤回设施"列为修罗线复审固定判据——已采纳。已告知其全面停止令,本会话静默待命。
- 23:2x：**G124(G077 覆盖漏项通用修复)交付,复审 PASS(P0=0/P1=0/P2=1)**。用户硬要求"必须通用"已达成:
  新框架包 runner.progress(Ledger/Report/Guard),恢复基线**只由框架从 context 读一次**(生产侧
  getInitialCompletedRuns 调用:任务包 0 次、全仓仅 TaskProgressReport+定义处),累计与"本进程首轮"
  拆成两个独立字段(修罗/鬼王/抓鬼/新手各 5 处 completedRuns==0 兼任已拆、私有 shouldStartNextRound
  删除、上限改累计比 maxRuns);**编译期护栏**=裸整数 updateProgress 重载删除,七任务 15 处上报全部
  只能走 TaskProgressReport;运行期护栏=低于基线抬回+ERROR(不抛,五环 allRunsExhausted 既有注释支持)。
  天庭双计已消除(自读基线段删除,注释保留字面词不构成调用)。独立复证:生产隔离编译 0 错、
  **G124 三套 34+11+36 与 G077 1 = 82/82 全绿**、**修罗整包 40/40 零退化**、G119/G120 标记 9 处完好、
  三个新测试 check-ignore 全 1。**P2(转拍板):五环单次 execute 内轮次上限仍由客户端调度器负责,
  是否纳入云端账本上限待定。**其余未尽项(七任务未端到端驱动 Spring execute、账本非线程安全沿用既有
  单线程约定、写集外六个既有漂移测试)均如实申报且与基线一致。**fresh 必看:恢复后首次上报=13/90 不再变 0。**
- 2026-08-30 用户指派 **G125 五环买鞋:战斗中误开快捷购买 + 自动进牛记布店后遗留 STOPPED_AWAY**
  (汇报必带业务标题,用户明令)。已派实现子代理:两条 P1 一起修(战斗硬门 / 跨图进店事实先结算清 intent),
  四条冻结业务要求为硬边界(禁泛化 STOPPED_AWAY、禁放宽同图容差、禁改模板与点位),五条合同门全落,
  塑形两案须比较后择一。回归面含 G124 三套+修罗 40/40(防误伤新收官卡)。交付后我独立复审。
- 2026-08-30 **G125 五环买鞋:战斗中误开快捷购买 + 自动进牛记布店后遗留 STOPPED_AWAY —— 已完成并复核**。
  用户中途令停所有后台 agent,其半成品经检查实为**实现+合同均已完成**(仅缺最终报告),故由评审方
  接手验收而非重做。独立复证:生产隔离编译 exit 0、合同 16/16、五环 19/19、修罗 40/40、G124 81/81
  全绿;冻结四要求逐条核验通过——判定全留任务层(CloudObservationHttpHandler 本卡零改动,其唯一
  diff 行属既有 G116)、作用域由 SHOE_SHOP_ENTRY_NAV_SOURCE 前缀限死 14 处、模板/容差/点位未动。
  终态 FiveRingTaskV3 `b6a1f15f4d503176`/3803L、合同 `7bf8a704edc5416b`/357L、fixture `bf1f6067d7bd0ede`。
  卡文+源表+dashboard(359 行)已更新。**fresh 未跑(禁启停),待用户重启。**
