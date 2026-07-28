# CR271 Post-696 本地差异云端迁移计划

> 状态：`TURN-40F PASSED / TURN-41 REAL DATA CUTOVER PASSED / TEST READY / USER FRESH RUNTIME REQUIRED`

> 2026-07-20 20:54 EDT：production scope固定为tenant `dhxy-local`、当前Windows SID与仓库外
> `%LOCALAPPDATA%\DHXY\cloud-brain\state`。Inspect/DryRun/Apply及独立post-read均PASS，counts=
> `22/460/600/1000/80`且四SHA精确；backup保留。TURN-41现TEST READY，仅待用户fresh runtime验收。

> 2026-07-20 20:49 EDT：TURN-41 Repair #1父级终审=`0/0/0 PASSED`，父级复跑named tests `7+1`及
> Cloud compile均PASS，Huygens owner释放。唯一剩余门为真实tenantId/userId/绝对持久化stateRoot及实际
> Inspect/DryRun/Apply/post-read；成功前仍NOT TEST READY。

> 2026-07-20 20:40 EDT：TURN-41 Parent Review #1=`0/1/1 / REPAIR REQUIRED`。Apply在backup后发生
> 中途写入/post-read失败时缺少自动四文件恢复，且backup路径未在首个mutation前暴露；Huygens同卡返修
> transaction rollback、failure-injection contracts及安全JavaDoc。真实scope仍未知且禁止猜测，尚不可测试。

> 2026-07-20 18:32 EDT：TURN-40F Repair #5两个focused gates父级验证`0/0/0`并复跑PASS；whole-card final review仍继续，TURN-41仍BLOCKED，尚不可用户测试。

> 2026-07-20 18:37 EDT：whole-card Review #7新发现角色预检baseline drift=`0/2/0 / REPAIR #6 REQUIRED`；owner保留，TURN-41仍BLOCKED。

> 2026-07-20 18:41 EDT：Huygens已ACK Repair #6并进入`SOURCE ACTIVE`；通信正常，TURN-41仍BLOCKED。

> 2026-07-20 18:47 EDT：Repair #6 focused gates父级`0/0/0`，30 tests及Cloud compile复跑PASS；whole-card终审继续。
>
> 2026-07-20 18:52 EDT：TURN-41数据门完成分类审计。六份dirty runtime JSON=三个`config` canonical stores +
> 三个旧`data` sidecar/compatibility stores，禁止六份盲拷贝；Cloud exact tenant/user/stateRoot尚未确定且目标文件不存在，
> TURN-41继续BLOCKED。
>
> 2026-07-20 18:59 EDT：whole-card Review #9=`0/1/0 / REPAIR #7 REQUIRED`。Cloud map-survey correction
> 简化为历史error反距离平均，未迁入baseline current-base exact delta及屏幕聚类/加权仿射/奇异和残差拒绝；owner保留。
>
> 2026-07-20 19:04 EDT：Review #9 addendum=`0/2/1`。Cloud真实map-label consumer缺当前baseline新增
> `铁匠屋.png`，且另有61张生产零引用重复目录；Repair #7扩大到asset manifest/SHA与重复资源删除。
>
> 2026-07-20 19:14 EDT：Huygens对Repair #7两个消息连续两轮无ACK，且超过10分钟无事件/源码变化；标记
> `COMMUNICATION_STALE + ACTIVE_STALE`。owner/写集保留，不另派Worker，TURN-41仍BLOCKED。
>
> 2026-07-20 19:39 EDT：只读baseline出现外部新增`?? .codex-audit-CQWebGame/`独立浅克隆，dirty 93 -> 94；
> 与DHXY业务无关，保护且不纳入迁移/清理。Repair #7 stale与TURN-41状态不变。
>
> 2026-07-20 19:44 EDT：baseline又新增外部`.codex-audit-legend-web/`独立工作树（12,368项），dirty
> 94 -> 95；同样FOREIGN/PROTECTED并排除迁移。Repair #7/TURN-41不变。
>
> 2026-07-20 19:49 EDT：baseline新增第三个外部`.codex-audit-h5-mir/`独立工作树（445项），dirty
> 95 -> 96；带独立`.git`，同样FOREIGN/PROTECTED且排除迁移。Repair #7/TURN-41不变。
>
> 2026-07-20 19:59 EDT：用户要求立即恢复实施；Huygens已重新唤醒并接收同卡Repair #7固定返修，
> 状态恢复为`SOURCE ACTIVE / OWNER RETAINED`。TURN-41继续BLOCKED，等待canonical re-delivery及父级终审。
>
> 2026-07-20 20:03 EDT：Huygens已ACK全部三个Repair #7 parent message，stale正式解除并保持SOURCE ACTIVE。
> 同时baseline新增第四个外部`.codex-audit-legendary-game/`独立仓库（111项），dirty 96 -> 97；继续保护并排除迁移。
>
> 2026-07-20 20:14 EDT：Repair #7父级终审=`P0/P1/P2=0/0/0 / PASSED`，focused tests 8项及Cloud
> compile复跑PASS，owner释放。TURN-40F source review闭合；TURN-41仍因精确数据cutover阻断，尚不可用户测试。
>
> 2026-07-20 20:19 EDT：用户要求立即推进；Huygens已接收TURN-41 pre-runtime数据cutover任务，正在创建
> 固定原卡并确定实际scope。合同拆为Worker数据导入验证后，再进入用户fresh runtime；当前仍NOT TEST READY。
>
> 2026-07-20 20:22 EDT：Huygens已在`reports/2026-07-20-turn-card-TURN-41.md`canonical claim并ACK，
> 状态=`SOURCE+TEST DATA-CUTOVER SOURCE ACTIVE / OWNER RETAINED`；用户runtime仍blocked。
>
> 记录时间：2026-07-20 03:25 EDT
>
> 用户裁定：`D:\mavenProject\DHXY` 当前 workspace 中的业务逻辑全部需要保留并进入 CR271 最终形态；
> 本轮只做只读审计和计划合同，不改本地基线、不改 Java、不启动 runtime/input/capture。

> Repair #7 re-delivery：Cloud facade 的 Wubei/Xiuluo accept-time snapshot 入口已从 anchor-required
> `analyzeFullWindow` 改为既有 direct `analyzeSnapshot` 主干，absolute origin 不变；Cloud compile exit 0。
> live/local mechanics/algorithm/protocol/resource/task phase 均未改，等待 Parent Review #3。

> 2026-07-20 delivery：Repairs #1-#5 与 LD-01..LD-10 source 已闭合，DHXY-cr271
> `mvn -q -DskipTests compile`、Cloud `mvn -q compile` 均 exit 0。无 named tests 授权，零测试执行；
> TURN-41 继续 BLOCKED，等待父级 source review，未写 Approved。

> Repair #6 re-delivery：Review #1 `0/1/2` 已按冻结写集返修。Cloud tracker 唯一算法完整吸收当前
> baseline raw-title/geometry/detail/green-link/progress-tail 差异，新增指定 Wuhuan yellow-title asset；
> 双仓 frame-purpose byte-identical，facade 只修 ownership JavaDoc。双端 compile exit 0，等待 Review #2。
>
> Parent Review #2：`P0/P1/P2=0/1/0`。Repair #6 三项均闭合，但 Cloud facade 的五倍/修罗
> `...FromSnapshot(...)` 仍调用 anchor-required `analyzeFullWindow(...)`；本地基线及修罗接任务生产 caller
> 均要求直接在 supplied full-window snapshot 中寻找任务标题。Repair #7 仅在同一 facade 内复用现有
> `analyzeSnapshot(...)`，保留 absolute origin，不改算法、协议、mechanics 或 task phase。
>
> Parent Review #3 final：Repair #7 单文件复审通过，两个 accept-time snapshot 入口均恢复 direct-title
> analysis，坐标原点原样保留；Review #1/#2 全部 finding 关闭。父级在稳定源码上现场完成 Cloud
> `mvn -q compile` 与 DHXY-cr271 `mvn -q -DskipTests compile`，均 exit 0。最终
> `P0/P1/P2=0/0/0`、owner released；TURN-41 开放为用户 fresh runtime gate。无 named tests 或 Agent runtime。
>
> Completion audit correction：上述结论只覆盖 post-696 差异吸收，不覆盖默认生产入口切换和客户端厚 Task
> 退役。真实 UI 仍调用 local `start(...)`，DefaultTaskFactory/WindowTaskRunner 仍执行客户端修罗/五环/五倍
> phase machine；remote start API 零 production caller。TURN-41 READY 已撤销，新增 TURN-40F 做完整 call-graph/
> write-set audit、remote-default cutover 与 thick-task 零引用退役。

> 2026-07-20 parent review #1：`P0/P1/P2=0/1/2`，delivery 不通过。Cloud tracker 未吸收当前本地
> raw title asset/wash、panel/detail geometry、compact pathing 与 progress-tail 解析；双仓 frame-purpose
> 物理字节不一致。原 Worker owner retained 返修，TURN-41 继续 BLOCKED。

## 1. 权威边界

| 角色 | 路径 | 分支 / HEAD | 本计划权限 |
|---|---|---|---|
| 用户当前运行基线 | `D:\mavenProject\DHXY` | `codex/baseline-696a12b0` / `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` | 严格只读；不得切分支、写文件或清理 dirty/untracked |
| CR271 权威工作树 | `D:\mavenProject\DHXY-cr271` | `thin-client-design` / `59b85e0bb494f43ad7e7434f3d2170deb373c6ef` | 只写计划、卡片和状态；实施须另经用户批准 |
| Cloud 源码仓 | `D:\mavenProject\dhxy-cloud-brain` | `navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01` | 本轮只读；实施时按本计划固定写集修改 |

本轮起始 dirty/untracked 逐项计数为 128 / 81 / 588。三个工作区的现有 dirty/untracked 全部受保护，
不得用 checkout/reset/clean、整目录复制或整文件覆盖消除。

## 2. 迁移目标与非目标

目标不是把旧 workspace 的类机械复制到 Cloud，而是把每一条 post-696 行为放入 CR271 已冻结的唯一属主：

- Cloud 唯一拥有任务 phase、业务决策、识别策略、候选排序、重试/回退语义和跨 turn 状态。
- DHXY 本地唯一拥有绑定 HWND 截图、物理输入、输入队列、窗口级短期缓存和本地复合原子动作。
- `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService` 继续是永久本地 Service；Cloud 只能请求强类型本地操作。
- `WindowTaskRunner` 继续本地运行，只观察事实、消费准备动作和维护窗口级时序，不成为第二业务状态机。
- `WindowRuntimeContext` 继续是窗口运行状态唯一属主；禁止在 Cloud 再建镜像 store。
- 不复制 OCR、模板识别、黄点筛选、tracker 分段或任务算法形成 local/cloud 双实现。
- runtime JSON 是现场学习数据，不等于源码合同；先冻结 schema 和导入规则，禁止直接覆盖 Cloud 数据。
- debug/replay/evidence 图片保留来源清单，但只有生产调用路径引用的资产才进入 runtime 包。

## 3. 本地差异总清单

相对 `696a12b0`，本地存在 22 个 tracked production Java 改动、1 个新 production Java、1 个资源配置、
3 个 tracked runtime JSON、11 个 tracked 图片、5 个 tracked tests，以及 55 个 untracked 图片、16 个
untracked tests/debug mains、3 个 untracked runtime JSON。所有差异必须进入以下四态之一并留下证据：

1. `ALREADY_EQUIVALENT`：CR271 当前实现已完整吸收，需方法级证据和 SHA/调用链，不得凭文件名判断。
2. `MIGRATE`：按唯一属主写入 CR client / protocol / Cloud。
3. `DATA_ONLY`：只做 schema、备份和一次性导入设计，不进入源码算法。
4. `EVIDENCE_ONLY`：保留审计来源，不进入 production runtime。

任何源差异未分类、任何 `MIGRATE` 未有验收证据时，TURN-41 都不得恢复 READY。

## 4. 行为簇与目标属主

| 簇 | 本地来源 | 必须保留的语义 | CR271 最终属主 |
|---|---|---|---|
| LD-01 输入暂停与检查点 | `InputActionQueue/Request/Scope/Worker`、`TaskPauseToken`，Bag/Quest/UI/NPC/Player 调用点 | 排队请求同时携带 pause/stop；暂停在 focus/action 前阻塞；恢复同一请求；暂停时长不计 120s waiter；stop/身份漂移仍取消；exclusive callback 可暂停 | 本地 input 基础设施；只吸收 CR 当前缺失项 |
| LD-02 UI 与无限次数 | `MainWindowController`、`application.properties` | 修罗/五倍次数 `0=无限`、不设正数上限、UI 显示“无限”；五环规则保持独立 | 本地 UI 输入与显示；共享协议允许 0；Cloud loop 解释 0 |
| LD-03 Tracker | `TaskTrackerPanelService`、`WindowTaskRunner/RuntimeContext`、新 `WindowTrackerAnchorMemory` | 新 raw 标题资产、窗口级 anchor cache、局部 ROI 优先、masked 全窗 fallback、拖到固定位置、紧凑新字体绿链、五环 title-only、既有 phase/order 不变 | 本地 capture/rect/cache/drag；Cloud 标题识别、绿链分段/排序/结果物化 |
| LD-04 NPC 黄点与直接战斗 | `NpcClickService`、`GameTextLineOcrService` | 灵兽使者/白龙马/降魔守卫 profile；profile mask；玩家紫色 anchor 上方 50px 候选；同一 Alt+A session 先 direct candidate 后 Ctrl/FIFO；全部策略耗尽才退出 | 本地截图/anchor observation/原子点击与 Ctrl；Cloud `SmartClickRecognizer` 唯一持有 profile 和候选顺序 |
| LD-05 维护、对话框与补给 | `TaskMaintenanceService`、`DialogService/Request`、`PlayerStateService`、`UICleanerService` | raw 广播 ROI/模板/阈值；删除通用 dialog 兜底；只做目标化关闭；仅真实补血蓝点击后移鼠标且同一原子序列；confirm-only 不移 | Cloud 维护/对话策略；本地 capture、模板允许边界和强类型 click/close；本地 supply 原子动作 |
| LD-06 召唤兽技能 | `SummonSkillService` | 静态槽位 LOCKED/EMPTY/OCCUPIED/UNKNOWN；只 hover OCCUPIED；删除后静态复核；ultimate 生成等待 2500ms；不做 broad cleanup | Cloud 静态槽识别与 tail policy；本地面板截图、hover/delete 复合动作 |
| LD-07 五环 Runner/Task | `FiveRingTaskV2`、`WindowTaskRunner` | Runner 拥有 tracker scan/click；`RUNNER_PREPARED_NOT_READY`；title-only 热启动；stale action 等 Runner 刷新；战斗恢复清旧 intent | Cloud task phase；本地 Runner 事实/准备动作消费；协议承载强类型 outcome |
| LD-08 修罗 | `XiuluoTaskV2` | 维护 hook 上限 2；维护失败不重置 round；选择性关闭对话；删除 generic accept fallback/broad cleanup；世界地图黄字 mini-map pathing 成功可打开既有 FIRST_AID window | Cloud 修罗 phase/重试/恢复语义；本地事实与强类型关闭/维护操作 |
| LD-09 自动战斗面板 | `AutoCombatPanelService` | 使用 `auto_remaining.png` 原图阈值 0.80、中心偏移 +43/+28，保留 Alt+8 retry | 先判 `ALREADY_EQUIVALENT`；识别策略归 Cloud、拖动/输入归本地 |
| LD-10 数据与资产 | runtime JSON、模板和 debug 图片 | 不丢学习数据；生产资产按真实调用路径打包；证据图不激活 | 单一 schema + 一次性 import；client/cloud 各自只打包其真实消费者所需资产 |
| LD-11 集成与门禁 | 全部 | 双侧 compile、授权 contract tests、资产 SHA、协议兼容、受控 fresh runtime | 父级集成与最终审核；用户执行 runtime |

## 5. 固定实施波次

### Wave 0：证据冻结与分类

1. 为 23 个 production Java 路径生成 method-level delta ledger。
2. 为图片生成 `production / evidence / unrelated-future` 三类 manifest，记录 SHA256 和调用点。
3. 为六份 runtime JSON 记录 schema、字段属主、敏感性和导入方向，不复制数据。其中
   `config/dialog_choice_memory.json`、`config/vision_memory.json`、
   `config/world_map_route_result_memory.json`为canonical候选；`data/npc-click-memory.json`、
   `data/route-memory.json`、`data/vision_memory.json`仅作旧sidecar/兼容合并证据，不得形成第二权威store。
4. 每项标记 `ALREADY_EQUIVALENT/MIGRATE/DATA_ONLY/EVIDENCE_ONLY`。

验收：128 项本地状态中的每个源码/资源/数据差异均可追溯；零工作树 mutation。

### Wave 1：本地执行连续性与共享协议

实施 LD-01、LD-02，只补 CR271 当前缺失语义，不回退已经完成的 turn/input 架构。

候选 client 写集：

- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionRequest.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionScope.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/runner/stop/TaskPauseToken.java`
- `src/main/java/com/bot/dhxy/ui/MainWindowController.java`
- 受影响的 shared request/result model（实施前冻结精确路径）

候选 Cloud 写集：任务启动参数校验和 loop count 解释路径（实施前从真实符号冻结，不允许新建第二启动协议）。

验收：暂停/stop/identity 语义与本地一致；count=0 双端同义；正数行为不变。

### Wave 2：Tracker 观察与识别拆分

1. 本地新增窗口级 `WindowTrackerAnchorMemory`，只保存 HWND 绑定的短期几何事实。
2. 本地机械层完成 cached ROI capture、masked fallback capture、drag 与窗口相对 rect 物化。
3. Cloud 唯一完成 raw 标题识别、绿链 segmentation/ranking 和 task-specific materialize。
4. 协议只传 frame/rect/taskKey/cached-anchor fact/action outcome，不传第二份业务状态。
5. 接入新的五环、修罗生产标题资产；旧资产只在证据 manifest 留痕。

候选 client 写集：`TaskTrackerPanelService` 的现有 local mechanics、`WindowTaskRunner`、
`WindowRuntimeContext`、`WindowTrackerAnchorMemory`、tracker request/result model 与生产资产。

候选 Cloud 写集：现有 `TaskTrackerPanelService`、DecisionEngine tracker reader 分支和既有 payload；
禁止复制旧 `GameTextLineOcrService`。

验收：同一窗口 cache 不串窗；cached miss 才 full fallback；拖动不改变 task phase；新字体和 title-only 有合同证据。

### Wave 3：NPC profile 与 direct-combat 候选

1. 把四类黄点 profile、阈值、mask、allow/ignore region 全部并入 Cloud `SmartClickRecognizer` 唯一策略。
2. 本地只上传绑定 frame、targetName、mask mode 和玩家 anchor observation。
3. Cloud 返回有序 typed candidate；本地在同一 Alt+A input transaction 中执行 direct candidate、Ctrl/FIFO。
4. direct candidate 是玩家紫色 anchor 上方 50px；不得恢复本地 OCR/筛选兜底。

验收：四 profile 逐项对齐；错误窗口/过期 session 零输入；断云 fail-closed；候选顺序与本地一致。

### Wave 4：维护、对话、补给与召唤技能

实施 LD-05、LD-06。先复用 CR271 现有 maintenance/summon turn surface，再补缺失策略；不新增 service id、store
或 wrapper 链来绕开现有协议。

验收：

- 维护 raw ROI `(260,373)-(378,413)`、阈值 0.85、150ms/800ms 时序一致。
- `DialogHandleRequest` 不再携带 broad maintenance fallback。
- 只有真实 HP/MP 点击后才做 safe mouse move，且在同一原子输入序列。
- 召唤槽四态、IF8 ROI、阈值 0.80、inactive 色距 12、2500ms ultimate 等待全部闭合。

### Wave 5：任务 phase 等价

先完成 Tracker/NPC/Maintenance/Summon source gates，再实施 LD-07、LD-08：

- 五环 Cloud task 吸收 prepared-not-ready、title-only hot start、stale refresh 和 combat-recovery intent cleanup。
- 修罗 Cloud task 吸收 2 次 maintenance hook、best-effort round preservation、selective dialog close 和
  world-map yellow destination pathing success。
- 本地 Runner 只保留 2500ms prepared freshness、窗口/task/run ownership、优先级和事实发布；不得复制 task phase。

验收：逐条对照 `docs/业务逻辑.md` 与本地 post-696 diff；记录
`无未批准业务差异；按用户确认的本地 workspace 逻辑等价迁移`。

### Wave 6：数据、资产、构建与 TURN-41

1. 生产资产按真实消费者放入 client 或 Cloud resources，并校验 SHA256；同一算法不得两边各打包一套。
2. runtime JSON 只在 schema 兼容和单一属主确认后执行一次性备份/import；不覆盖用户当前现场数据。
   导入目标必须由实际启动参数的`tenantId/userId/stateRoot`解析到同一哈希私有作用域。导入前备份Cloud现存目标；
   dialog/vision/world-map route分别核对22/460/80 entries及vision 600 NPC samples、1000 target samples。
   `transfer_choice_memory.json`的14个key已全量并入dialog canonical store，`ocr_roi_memory.json`只作为legacy fallback；
   `map_camera_bounds.json`以SHA-256
   `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`进入同一scope。
3. Java writer 全部稳定后，运行 DHXY `mvn -q -DskipTests compile` 与 Cloud 启动路径要求的 compile/package。
4. 仅运行用户明确授权的 named contract test family；本地 16 个 debug/replay main 不自动迁移或执行。
5. 父级完成逐文件 source+test review，P0/P1/P2 必须为 0/0/0。
6. 以上全部闭合后才把 TURN-41 恢复为 `READY / USER FRESH RUNTIME GATE`。

## 6. 依赖与碰撞约束

- Wave 1 可先做，但必须先完成 Wave 0 ledger，避免覆盖 CR271 已有 input turn 语义。
- Tracker 与 NPC 共用 frame/payload/recognizer 基础，不能并行改同一 DecisionEngine/payload 路径。
- Maintenance 与 Summon 可能碰撞 `TaskMaintenanceService` 或 shared local operation，须分卡串行。
- FiveRing/Xiuluo 只能在基础 source gates 通过后改 phase；禁止任务内临时补 local fallback。
- 每张实施卡必须冻结 whole-card SOURCE+TEST 写集；未在写集中的 dirty 文件只读保护。
- 本计划不自动开放实施卡、不派 owner。用户批准计划后，再从 Wave 0 起固定整卡。

## 7. 测试与运行结论

当前不能直接进入 fresh runtime。此前 TURN-40B/C/D 的通过只证明 CR271 原冻结范围闭合，不能证明用户
post-696 本地逻辑已全部进入新架构。当前 TURN-41 必须回退为：

`BLOCKED / POST-696 LOCAL DELTA MIGRATION REQUIRED`

最早可测试条件：Wave 0-6 全部完成、双端编译成功、用户授权的 named tests 成功、父级最终 review 为
0/0/0、生产资产与数据导入门闭合。届时由用户在 IntelliJ 受控切换后执行 fresh runtime；Agent 不启动
application/server/Task/UI/capture/input。

## 8. 2026-07-20 实施阻断：次数未进入 HTTPS start contract

TURN-40E Worker 已 canonical claim，但在 Wave 0 的 LD-02 对账中发现冻结写集不完整：本地 UI 的修罗/五倍
次数只写本地 `BotProperties`，现有 `TurnTaskStartRequest` 只携带 ordered `taskCodes` 和 failure policy；Cloud
prototype Task 随后读取 Cloud 进程全局 `BotProperties`。因此 exact window/taskRun 的 `0=无限` 与正数值没有
跨端传递，多窗口也不能通过修改 Cloud 全局配置补救。

状态：`PLAN-CONTRACT BLOCKED / TURN-40E-PCB-01`。父级必须先把双仓 byte-identical start request/validator、
DHXY `WindowTaskControlService`、Cloud `CloudTurnTaskRuntime`/`CloudTurnTaskFactory` 与 `WubeiTask` 纳入冻结
写集，并裁定与 ordered queue element 精确关联的 count 表示。仍须复用同一个 HTTPS start protocol；禁止第二
protocol/store、Cloud 全局 mutation、默认 1 或恒 null。阻断前 production/resource 零写入，未运行 Maven。

## 9. 2026-07-20 实施阻断：Tracker 本地 cache/mechanics 无 turn-native caller

Repair #1 已按冻结表示实现并 compile。继续 LD-03 时确认：Cloud
`TaskTrackerPanelService.observe` 只调用 generic `TurnGameClient.capture/execute`，在 Cloud
`pendingRepositions` 保存 anchor/reposition 状态；DHXY 的 `TaskTrackerPanelCaptureLocalMechanics.capturePanel`
没有 production caller。现有 dormant `TASK_TRACKER_PANEL_RECT` 属于旧 remote WindowFact 路径，turn-native
`TaskExecutionContext` 不具备该 authority，复活它会形成第二协议。

状态：`PLAN-CONTRACT BLOCKED / TURN-40E-PCB-02`。父级须冻结 cached ROI -> masked full-window fallback ->
必要 drag -> post-drag panel capture 作为现有 HTTPS turn 中的唯一强类型闭包，并补齐双仓 protocol、DHXY
dispatcher/executor、Cloud client/facade 写集；`WindowRuntimeContext` 仍是唯一 anchor cache，Cloud 只拥有标题、
绿链 segmentation/ranking。Worker 不自行选择新 local-service operation 或新 TurnStep 表示。
