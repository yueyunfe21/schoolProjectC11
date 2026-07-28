# A-1 全库方法级 Service 迁移矩阵（THIN_CLIENT_V1）

> **2026-07-24 local pathing direct ingress：** 寻路状态只由 Client Runner
> 机械判断；Cloud observation 入站完成 exact-run 校验后直接发布 ready event。
> coordinator、Cloud pathing observer 与 coordinate-strip 上传均退出寻路链路。
> Cloud pathing state 仅缓存 Client 原始 fact，不再具备 Cloud overwrite 能力。
>
> **2026-07-24 local combat tri-state：** 单一本地 Runner 使用双正向证据：
> 小地图锚点命中为 `WORLD_CONFIRMED`，战斗模板命中为 `COMBAT_CONFIRMED`，
> 两边均未命中或采集不可用为 `UNKNOWN`。仅世界态发布 `COMBAT_EXITED`；
> 未命中不反推相反状态，未知态保持当前状态并重试。
>
> **2026-07-24 local minimap exit cutover：** 修罗/五倍不再区分快速/普通两套脱战裁决。
> Client `WindowObservationSampler` 在本地匹配小地图锚点并独占 `COMBAT_EXITED`；Cloud 仅消费
> exact event。旧 avatar fast probe、miss/readability 回包、专用 observation identity 和 Cloud
> fast Radar/gate 均删除。五环/挂机普通 Radar 保留。Client `11/11`、Cloud `18/18`、双
> test-compile 与协议同字节门通过；fresh runtime 待验。
>
> **2026-07-24 pause/resume lifecycle repair：** 不改变 Service 边界。Client pause exchange
> 改为零等待，恢复时先重启同一 observation runner，再进入普通 Cloud 长轮询；由此恢复既有
> Leader `Alt+8` 与 combat-exit 事件链。父级 `0/0/0`，测试/compile 通过，fresh runtime 待验。
>

> **2026-07-24 local runner authority final source pass：** Repair #1-#4 / Parent Review #6
> `P0/P1/P2=0/0/0`。所有战斗机械 fast exit 在本地，incidental 零 Cloud 业务边沿；expected
> exact claim、异步 retained replay、stop/replacement checkpoint、typed terminal 与修罗/五倍
> Cloud fallback 已闭合。named families/双 compile 通过，协议 `7/7` 一致；fresh runtime 待验收。
>
> **2026-07-23 local runner authority reopened：** 当前 strict taskRun equality 横跨两种身份格式，
> replay 不可达；失败路径无 Cloud terminal handoff，且本地宏阻塞 observation sampler。状态改为
> `REPAIR REQUIRED / P0-P1-P2=0/2/2`，补 identity/async replay/terminal/test 合同前不验收。
>
> **2026-07-23 local runner authority cutover：** 本地新增 per-window retained return replay
> 协调器与 combat temporal trigger；只保留修罗/五倍 post-combat 回程动作，exact taskRun/HWND/
> size 栅栏，平移只修正 screen-origin delta。Cloud 仅发送一次 `WHOLE_TASK_RETURN_HOME_REPLAY_ARM`
> 并保持 `RETURN_HOME`；本地成功重放后才允许发布退出事件。OCR、地图、NPC、业务决策仍在
> Cloud。双仓 compile exit `0`、共享协议 byte-identical，fresh runtime gate 未关闭。
>
> **2026-07-23 fast-exit pending/visible race repair：** 本地 fast edge 已实跑到达 Cloud，失败点是
> no-turn observer 在任务消费前用重叠 visible 样本复活同一 combat generation。Cloud 现将 exact
> fast-exit pending 到消费设为原子边界；消费后真实新战斗仍可进入。合同 `49/49`、compile/diff-check
> 通过，待重启 fresh 验收。
>
> **2026-07-23 exact combat identity repair：** 完整雷达已在 fresh run 两次确认脱战，实际卡点是
> Fast Exit identity A 与随后 confirmed combat-enter identity B 不一致，导致有效退出信号被 exact
> wait/generation 栅栏拒绝。Cloud 现让 `PENDING/ARMED` 在 combat-enter 后 reconcile；同 identity
> 幂等，变化时替换 exact wait/interest。合同 `48/48`、compile 通过，待重启 fresh 验收。
>
> **2026-07-23 COMBAT-OBS fresh P1 repair：** Fast Exit 授权已从错误的 enum name 改为生产
> `taskCode=xiuluo_v2/wubei`；无 exact observation binding 时显式走完整雷达 fallback。
> `coordinate-strip`/`xiuluo-dialog` 动态 interest 改为内容幂等，停止 revision 互相追逐。
> Cloud 定向 `32/32`、compile 通过；需重启 Cloud 验证真实 fast event 和无 165 秒 pathing timeout。
>
> **2026-07-23 TURN-40G dialog demand amendment：** local kanda2 生产重新启用并在 Client
> observation 同轮先手；Cloud `529x208` dialog ROI 只由 route/full-dialog、显式非 probe 确认或
> tracker terminal 后 Client-clock `+3000ms` fallback 动态请求。probe-only miss 零 Cloud 竞争，
> stopped-static 只消费严格新帧。状态 `SOURCE+TEST DELIVERED / AWAITING PARENT REVIEW`。
>
> **2026-07-23 COMBAT-OBS-P1 source gate：** 父级 `P0/P1/P2=0/0/0`。`BattleRadarService`
> 的三组正向入战模板成为 Client per-window Observer 的纯机械匹配并输出既有 `COMBAT_SIGNAL`；
> Cloud 保留状态机、连续 miss、防抖、phase/wakeup 与坐标退战解释。`coordinate-strip` 仅在
> exact active pathing 或 combat-exit fallback 时上传；Fast Expected Exit 不变。双仓命名测试、
> compile 与 DTO 同字节门通过，fresh runtime 待验。
>
> **2026-07-23 TURN-40G Stage 6 FINAL PASS：** 父级 Review #27
> `P0/P1/P2=0/0/0 / SOURCE+TEST PASSED / OWNER RELEASED`。Observer 已对 task turn、
> `TurnGameClient` 与 runtime local-service 零引用；同序 observation 配对、旧序列/旧 intent 拒绝和
> repeated-CURRENT 保留 Cloud observed stationary timing 均有生产合同覆盖。Cloud `70/70`、
> consumers/replay `3/3`、Client `8/8`、双 compile、DTO `16/16` 全绿。fresh runtime 尚待用户验收。
>
> **2026-07-23 TURN-40G Stage 5/6：** Stage 5 task-owned consequence 经父级 Review #24
> `0/0/0` 通过；Observer 对 gate/settlement/prepared-clear 零调用，精确合同与双 compile 全绿。
> Stage 6 只移除 Observer turn/command/local-service wrapper，禁止新增第二观察器、store、协议或业务差异。
>
> **2026-07-22 GameStateUtil owner closure：** 以 `696a12b0` 的 12 个公开职责逐项审计后，不恢复
> 832 行混合工具类。移动/飞行/direct-combat/fresh-map/movement-intent 已有明确 owner，5 个无活调用
> 接口不恢复；`isSameMapName` / `isNearCoordinate` 收口到 Cloud `NavigationService` 唯一纯规则。
> 空 target map 只比坐标、负容差钳 0；修罗/五倍删除语义副本，五环仅无条件转发。父级
> `P0/P1/P2=0/0/0`、Cloud compile 通过，fresh 近点 NPC gate 待验。

> **2026-07-23 TURN-40G Observation/task-turn Stage 4/5：** Stage 4 已由 Review #23 `0/0/0`
> 通过，dialog/tracker preparation 只消费 exact uploaded ROI，精确测试与双 compile 全绿。Stage 5
> 正把 target-map gate、路线学习 settlement、prepared destructive consume 和 supplemental action
> 收回 owning task 持-turn 流程；Observer 继续复用既有 inbox/prepared/ready store 且不执行 consequence。
>
> **2026-07-22 TURN-40G post-terminal frame closure：** stopped-static只消费严格晚于首次terminal的独立
> observation帧；同批/缓存帧只defer，不得形成`CLOUD_NO_ACTION`。Cloud模板/阈值不变；local-kanda生产默认
> false，关闭时零matcher/input/event，CLICKED状态桥保留但不启用。双端SHA诊断用于fresh-runtime同图核验。
>
> **2026-07-22 TURN-40G prepared-action wake closure：** Cloud业务识别仍归Cloud，Client只执行返回的
> 机械输入。Cloud Observer必须把非空`PreparedDialogAction`落入exact-window唯一槽并发布
> `PREPARED_ACTION_READY`，不能只发通用attention；operation/target为
> `XIULUO_ENTER_BATTLE / xiuluo.enterBattle`并一次性消费。源码门通过，fresh runtime未通过。

> **2026-07-22 CR212/Observation execution repair：** 角色分配仍由Cloud CR212预检独占；四个队员已确认
> 为`MEMBER`，仅修Cloud `AutoBattleTask`原型构造与effective-task ACK/UI投影。Client观察器仍只做本地
> 采集/kanda机械动作，Cloud仍做dialog业务识别；queue-run/child-run栅栏与`640KiB`单ROI边界闭合后，
> 双端需重启fresh验收。

> **Observation transport envelope closed（2026-07-22）：** Client/Cloud完整JSON上限已对称4MiB，协议原有
> 数量/尺寸边界不变；单图上限后续由真实dialog帧证据修订为`640KiB`。业务placement、识别和输入职责
> 零变化。Client `6/6`、Cloud `19/19`通过。

> **Observation transport envelope reopen（2026-07-21）：** placement不变；Client采集、Cloud识别仍是唯一
> 路径。当前阻断仅为双端HTTP整包256KiB小于协议允许的多ROI Base64载荷，需有界对称修复后fresh重验。

> **Observation transport placement repair（2026-07-21）：** Client仍只负责按Cloud interest采集并POST ROI，
> Cloud仍拥有observer业务判定。此次仅修Cloud HTTP接入并发：阻塞turn请求不能饿死独立observation endpoint；
> Client只增加有界诊断。没有把OCR、pathing terminal或NPC决策迁回本地，也没有新增第二协议/store。

> **NAV Observer post-intent/cadence repair（2026-07-21）：** Cloud仍拥有pathing终态判定，但每个exact
> intent先以当前正`observerSeq`建立栅栏，仅消费更新的post-intent坐标帧；`coordinate-strip`恢复696基线
> 2000ms最小probe节奏，其他观察ROI仍为1000ms。阈值、ARRIVED与修罗phase不变，相关`19/19`通过。

> **NAV Observer placement repair（2026-07-21）：** `CloudWholeTaskObserver.probePathing`仍在Cloud拥有终态
> 判定，但输入事实按exact intent单调`observerSeq`消费，静止时钟取Client `capturedAtMs`。修罗
> `isNearCoordinate`仅消费Cloud已有map/x/y；target map为空时按696只比较坐标，非空时才要求canonical
> 同图，负容差钳0，且不占Client command slot。2026-07-22已收口至`NavigationService`唯一owner。

> **NAV regression Review #1（2026-07-21）：** `NavigationService`当前地图handoff仍缺exact intent deadline gate，
> mini-map首轮关闭失败后的fallback偏离696基线。状态`P0/P1/P2=0/2/2 REPAIR REQUIRED`。

> **TURN-40G Review #4（2026-07-21）：** local observation主体通过，但sampler尚未绑定authoritative
> observation taskRunId；旧run迟到matcher必须在截图识别前即零动作，claim/release也须完整schedule身份校验。

> **TURN-40G Review #3（2026-07-21）：** String taskRunId链已闭合；剩余阻断是Client runtime必须提供
> paired dialog-interest/green-chain-schedule原子更新，而非两个AtomicReference顺序写。协议round>0及partial非法
> 零状态变化合同仍待实现。

> **TURN-40G production-wiring P1（2026-07-21）：** local-kanda实现存在但生产不可达；必须从真实修罗
> shortcut入口传递probe-only与原25秒anchor，并在Client为相同taskRun/round/attempt原子打开
> `XiuluoGreenChainSchedule`。仅isolated fixture点亮matcher不算迁移完成。

> **TURN-40G Step 5 owner correction（2026-07-21）：** `kanda2` matcher只在Client；Cloud不新增模板资产，
> stopped-static通过observation复用Cloud既有完整dialog识别。Cloud `XiuluoTaskV2`仅有限接线verdict和CR232
> 三次成功重按预算，不复制Client matcher或改变其他phase。

> **TURN-40G placement correction（2026-07-21）：** 用户批准恢复本地常驻
> `WindowObservationRunner`，但不恢复第二套本地任务状态机。observation改走独立HTTP平面，Cloud继续拥有
> phase/OCR/dialog/memory；修罗按Git CR232/253/256恢复唯一`local-kanda`本地输入例外。固定原卡
> `reports/2026-07-21-turn-card-TURN-40G.md`为`READY / ZERO OWNER`；TURN-42M以后删除清理暂时阻塞。

> **TURN-40F all P1 source/test passed（2026-07-21 05:46 EDT）：** parent final review `0/0/0`; all ten P1
> gaps are closed. Parent reran 49 focused contracts, both tests-enabled compiles, and both diff checks successfully.
> TURN-41 data cutover evidence is active again and formal user runtime testing may begin; remaining P2 cleanup is not
> represented as migration completion.

> **TURN-41 canonical claim（2026-07-20 20:22 EDT）：** Huygens owns the fixed data-cutover card and is
> source active. Actual scope and safe tooling/contracts are in progress; user runtime remains blocked.

> **TURN-41 cutover started（2026-07-20 20:19 EDT）：** Huygens is implementing non-destructive scoped
> data backup/merge/verification before the user runtime gate. Actual tenant/user/stateRoot must be proven, never
> guessed; legacy sidecars remain evidence only. Status is not test ready.

> **TURN-40F Repair #7 parent pass（2026-07-20 20:14 EDT）：** parent review `0/0/0`; correction math,
> exact 62-file production asset manifest, zero duplicate root/reference, focused tests `6+2`, and Cloud compile
> all pass. Owner is released; TURN-41 remains data-cutover blocked.

> **Repair #7 ACK / fourth protected subtree（2026-07-20 20:03 EDT）：** Huygens acknowledged all three
> messages and remains active. `.codex-audit-legendary-game/` is another unrelated independent repository
> (111 items); baseline dirty count is 97 and all four `.codex-audit-*` trees remain excluded/untouched.

> **TURN-40F Repair #7 resumed（2026-07-20 19:59 EDT）：** Huygens is active again on the same frozen
> Cloud correction/test/map-label scope. No client/baseline Java write is allowed; TURN-41 remains blocked.

> **Third protected foreign subtree（2026-07-20 19:49 EDT）：** `.codex-audit-h5-mir/` is an unrelated
> independent repository (445 items). All three `.codex-audit-*` subtrees are excluded and untouched;
> baseline dirty count is now 96.

> **Second protected foreign subtree（2026-07-20 19:44 EDT）：** `.codex-audit-legend-web/` is another
> unrelated nested worktree (12,368 items). It and `.codex-audit-CQWebGame/` are excluded and untouched;
> baseline dirty count is now 95.

> **Protected baseline foreign subtree（2026-07-20 19:39 EDT）：** `.codex-audit-CQWebGame/` is a new,
> unrelated nested shallow clone under the read-only baseline. It is excluded from every DHXY migration inventory
> and must not be deleted or moved. Baseline dirty count is now 94.

> **TURN-40F Repair #7 worker state（2026-07-20 19:14 EDT）：** Huygens is
> `COMMUNICATION_STALE + ACTIVE_STALE` after two missed ACK rounds and over ten minutes without event/source
> movement. Owner and frozen correction/asset scope remain; no replacement worker or build was started.

> **TURN-40F Review #9 map-survey correction gate（2026-07-20）：** Cloud `correctionAt` is not baseline
> equivalent: exact pins must recompute delta from the current projected base, while non-exact points require the
> baseline screen-clustered weighted affine fit with singular/residual rejection. Inverse-distance averaging of
> stored errors is forbidden. Repair #7 and production-path tests are required; TURN-41 remains blocked.
> The same repair must add baseline `铁匠屋.png` to the sole production resource root `templates/map_label`
> (SHA-256 `8BF1850437D74B6783CA32B10092EE45C0534D331BA56D1FE5673BB2254D2CFC`) and remove the zero-reference
> duplicate `resources/images/template/map_label` tree after a source/manifest proof.

> **TURN-41 data-cutover classification（2026-07-20）：** dirty runtime JSON is three canonical `config`
> stores (dialog/vision/world-map route) plus three legacy `data` sidecar stores. Only schema-compatible merged
> canonical state may enter the exact hashed tenant/user scope; `map_camera_bounds.json` SHA-256 is
> `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`. TURN-41 remains blocked.

> **TURN-40F Review #4 resource-owner gate（2026-07-20）：** Cloud's real tracker/task consumers still load
> six stale Wubei resource bytes, while four templates already deleted by the current baseline remain packaged
> without production callers. Repair #4 must synchronize the six live assets and remove the four obsolete assets
> plus stale README after a zero-reference proof.

> **TURN-40F route-OCR delta gate（2026-07-20）：** Cloud is the correct owner, but its wrapped-route path still
> uses the OCR continuation-box average center and omits the current baseline's retained-yellow-pixel center
> correction. Migrate the geometry without restoring client OCR ownership. Current review is `P0/P1/P2=0/5/2`.

> **TURN-40F startup ownership expansion（2026-07-20）：** the deleted startup Service/initializer contract
> includes Alt+1 tracking/auto-close/open-fly, Alt+U expand, Alt+5/6, flying, role/task skip policy, background
> UNKNOWN handling, queue idempotence, identity/position and left-top ordering. Rebuild policy in Cloud and only
> typed shortcut/capture/click mechanics in the client; do not restore a fifth local Service.

> **TURN-40F TeamRoleDetection disposition correction（2026-07-20）：** the baseline feature is enabled, so
> deleting the client Service is valid only after Cloud owns the live tooltip/OCR/status decision and pre-dispatch
> task reassignment through the existing turn. Client registration-order role is provisional metadata, not an
> equivalent replacement.
>
> **TURN-40F prepared-action owner disposition（2026-07-20）：** run ownership, freshness, priority, and
> combat/pathing/dialog eligibility are Cloud business policy. The client may report only exact-HWND typed facts and execute
> mechanics through the existing turn. The current baseline's tracker prepare fence and stale-dialog republish rules remain
> mandatory; deleting the local Runner is not equivalent without them. Current review is `P0/P1/P2=0/7/2`.

> **TURN-40F client residual audit（2026-07-20）：** CR271 client production contains `586` Java files /
> `124,233` lines, including Service `30,843`, Vision/OCR `3,612`, old remote `18,627`, and old local
> Cloud-decision client `10,918` lines beyond the thick Tasks. The full disposition and seven-wave cutover are
> frozen in `reports/2026-07-20-turn-40f-client-residual-audit.md`. Business facades/OCR/sidecars retire; HWND
> capture, fixed preprocessing, input, window runtime, HTTPS turn executors, and permanent local services are
> protected. Implementation has not started and TURN-41 remains blocked.

> **TURN-40E completion correction / TURN-40F（2026-07-20）：** client Xiuluo/FiveRing thick Tasks remain
> executable production components. UI starts local mode through DefaultTaskFactory/WindowTaskRunner; both remote
> start APIs have zero production callers. TURN-41 is blocked again. TURN-40F must freeze the full call graph,
> switch the sole default start path to HTTPS turn, and retire the zero-reference client phase machines.

> **TURN-40E Parent Review #3 final（2026-07-20）：** Repair #7 single-facade change passed
> `P0/P1/P2=0/0/0`; both accept-time snapshot APIs now reuse the direct snapshot recognition trunk and preserve
> the supplied absolute origin. All earlier findings are closed, final Cloud/client compile both exit 0, owner is
> released, and TURN-41 is `READY / USER FRESH RUNTIME GATE`. No named tests or Agent runtime ran.

> **TURN-40E Repair #7 re-delivery（2026-07-20）：** accept-time Wubei/Xiuluo snapshot 直接复用
> Cloud 唯一 `analyzeSnapshot` title/detail/green-link 主干，不再要求 anchor；absolute origin 保留。
> 唯一 facade 文件 compile exit 0，等待 Parent Review #3。

> **TURN-40E Repair #6 re-delivery（2026-07-20）：** Cloud tracker 唯一 owner 已同步 Wuhuan/Xiuluo
> raw title、panel/detail geometry、green-link/progress-tail 全部 Review #1 指定差异；新增 yellow-title asset。
> 协议双仓 byte-identical，facade 仅修说明；双端 compile exit 0，等待 Parent Review #2。

> **TURN-40E parent source review #1 BLOCKED（2026-07-20）：** `P0/P1/P2=0/1/2`。Cloud tracker
> title/detail/green-link 算法与 raw 五环标题资产仍不等价于当前本地 workspace；协议 frame-purpose 物理
> 字节不一致。原 Worker owner retained 返修，TURN-41 继续 BLOCKED。

> **TURN-40E whole-card delivery（2026-07-20）：** Repairs #1-#5、23 路径和 10 行为簇已闭合；
> DHXY-cr271 `mvn -q -DskipTests compile` 与 Cloud `mvn -q compile` 均 exit 0。未授权 tests 零运行，
> 状态 `SOURCE+TEST DELIVERED / PARENT REVIEW PENDING`，TURN-41 仍 BLOCKED。

> **TURN-40E Repair #5（2026-07-20）：** thin client 新增与 Cloud byte-identical 的 pure/stateless
> `OcrWindowScanService` default-mask subset；tracker 与两处 NPC local mechanics 共用，重复 masks 删除。
> 完整旧 OCR/capture Service 不回迁；main compile 修复、Cloud compile 与 delivery 待闭合。

> **TURN-40E Repair #4（2026-07-20）：** Cloud first-aid port 删除 capture-before pointer clear；仅非空
> 真实补给 command 在全部 `CLICK_RIGHT -> WAIT(800)` 后追加一次随机安全点 `MOVE_MOUSE -> WAIT(300)`。
> 同一 command、零第二 submit/retry/store；owner retained，SOURCE ACTIVE。

> **TURN-40E Repair #3（2026-07-20）：** Alt+A direct-combat 的
> `allowProfileRegionMasks=false` 通过 `NpcClickService -> SmartClickRecognizer` 传到唯一 Cloud
> `ImageAlgorithms.npcYellowTargetMask`；旧入口默认 true，算法其余部分不变。Owner retained，SOURCE ACTIVE。

> **TURN-40E Repair #2（2026-07-20）：** tracker 唯一通道冻结为现有 turn `LOCAL_SERVICE`。DHXY 独占
> `WindowRuntimeContext` relative anchor cache、cached ROI、masked full-window fallback、drag/post-drag capture；
> Cloud 独占 title/green/fingerprint/ranking。原 Worker owner retained 并恢复 SOURCE ACTIVE；禁止第二协议/cache。

> **TURN-40E PCB-02（2026-07-20）：** Repair #1 `taskMaxRuns` 已实现并双端 compile。LD-03 发现 Cloud
> tracker 仍通过 generic full-window turn capture 并持有 `pendingRepositions`，本地 tracker cache/mechanics 无
> production caller。状态 `PLAN-CONTRACT BLOCKED`；待父级冻结现有 HTTPS turn 的唯一强类型闭包，禁止第二协议。

> **TURN-40E PCB-01（2026-07-20 00:13 EDT）：** Worker 已 claim；LD-02 对账发现本地修罗/五倍次数只写
> 本地 `BotProperties`，未进入 `TurnTaskStartRequest`，Cloud Task 错读 Cloud 全局配置。状态改为
> `PLAN-CONTRACT BLOCKED`；须先冻结 ordered per-run count 的双仓协议/validator、DHXY caller、Cloud
> runtime/factory 与 `WubeiTask` 写集。production/resource 零写入，未运行 Maven。

> **TURN-40E（2026-07-20 00:05 EDT）：** 已建立 post-696 等价迁移整卡，要求后台 Worker 对本地 23 个
> production Java 差异逐项建立源方法→CR/Cloud 唯一属主 ledger；当前 READY/ZERO OWNER，TURN-41 blocked。

> **CR271 post-696 local delta gate（2026-07-20 03:25 EDT）：** 用户确认当前只读基线 workspace
> 的业务逻辑全部需要保留。已识别 input/tracker/NPC/maintenance/summon/FiveRing/Xiuluo/Runner/UI 等
> 行为簇，须按唯一属主逐方法吸收，禁止整类复制和第二算法。TURN-41 回退 BLOCKED，详见
> `2026-07-20-cr271-post-696-local-delta-cloud-migration-plan.md`。

> **CR271 TURN-40D pass ACK closed（2026-07-19 22:56 EDT）：** A 已 ACK Review #3 pass/release，
> owner-free、communication recovered/terminal；TURN-41 用户 fresh runtime 门保持 READY。

> **CR271 TURN-40D Review #3 PASSED / TURN-41 READY（2026-07-19 22:48 EDT）：** `0/0/0`；父级
> compile=0、authorized isolated family=22/22。A owner released；TURN-41 用户 fresh runtime 门开放。

> **CR271 TURN-40D Repair #2 source active（2026-07-19 22:41 EDT）：** guard=`53BD6055`/241L、
> loop test=`0FD0324A`/922L 已出现 selected entry 与 cleanup policy WIP；尚非交付，communication stale 保留。

> **CR271 TURN-40D Parent Review #2 BLOCKED（2026-07-19 22:26 EDT，P0/P1/P2=0/1/0）：** Repair #1
> 已补 18T 中多数 guard/control lifecycle，但没有测试调用 public `startRemoteSelectedTask`，且 start-failure
> exact-loop registry cleanup 仍无 executable proof。按 §19.6 原九路径返修；A owner 与 communication stale 保留。

> **CR271 TURN-40D repair source active / communication stale（2026-07-19 22:16 EDT）：** A 连续两轮未具名
> ACK Review #1，现标 `COMMUNICATION_STALE`；owner 保留。control test=`03030069`/201L、loop test=
> `29C96D0A`/733L 已推进，故非 `ACTIVE_STALE`。仍待 ACK、完整返修、named family、compile 与 fresh delivery。

> **CR271 TURN-40D Parent Review #1 BLOCKED（2026-07-19 22:00 EDT，P0/P1/P2=0/1/1）：** 12T 实际只测
> mapping/metadata 与 loop stop；未调用 public remote control entry 或 4-arg guard start，故 mutex、start
> attach/ack、pause/resume、guard stop/unregister、failure cleanup 合同无行为证明。旧 test JavaDoc 也未同步 R2。
> main compile exit0；named Maven 被既有全局 testCompile 债阻断。A owner retained，须同卡返修重交。

> **CR271 TURN-40D R2 ACK / ninth path active（2026-07-19 21:55 EDT）：** A 已 ACK 三消息，R2 冻结解除。
> `WindowTurnLoopContractTest=0085BCB8`/334L 已加入 interrupt long-wait -> single stop turn -> zero returned
> input/capture -> stopped -> unregister WIP 证明；当前 6/9 source active。Java writer active，未跑 Maven/runtime。

> **CR271 TURN-40D communication recovered / R2 ACK pending（2026-07-19 21:45 EDT）：** 物理顺序上 A 的
> 21:45 event 已 ACK `...2111+2121`，communication stale 清除；R2 `...2141` 尚待 ACK。Q1 已由 R2 闭合到
> existing loop test，Q2 三个路径保持 clean；5/9 frozen。compile exit0 与 control 9/9 isolate-run 仅作 WIP 证据，
> 两类 named family 尚未运行。

> **CR271 TURN-40D plan-contract repair R2（2026-07-19 21:41 EDT）：** control authority test 已落但 package
> 无法直接复用 loop harness。审计确认 clean `WindowTurnLoopContractTest=E91B5E2A`/273L 是唯一现成行为测试
> owner；原卡扩为 9 路径、named family=2 类，当前 5/9 frozen。A 须 ACK 三消息，未跑 Maven/runtime。
>
> **CR271 TURN-40D stop-action source repaired（2026-07-19 21:27 EDT）：** loop=`19B69135`/417L 已在
> response ACTION dispatch 前检查 stopCheckpoint，final stop turn 零执行源码门闭合。named proof 与
> `...2111+2121` ACK 仍待；communication stale、4/8、owner 不变，未跑 Maven。
>
> **CR271 TURN-40D communication stale / source active（2026-07-19 21:21 EDT）：** A 连续两事件未 ACK
> stop-action `...2111`，现须 ACK `...2111+2121`。control=`3E2A0D06`/712L 仍活跃，故非 active stale；
> owner/4-of-8/gates 不变，未跑 Maven/runtime。
>
> **CR271 TURN-40D control source started（2026-07-19 21:17 EDT）：** control=`B7BE569E`/497L active WIP，
> guard=`44770301`/233L 已加入 live-loop pause/resume；整卡 4/8。A writer active，stop-action gate ACK 待回，
> 未跑 Maven/runtime。
>
> **CR271 TURN-40D loop repair / stop action gate（2026-07-19 21:13 EDT）：** loop=`868E4BC5`/412L、
> guard=`E9FD87AE`/200L 已落 loop-owned pause/stop、flag union 与 graceful requestStop，整卡仍 3/8。final response dispatch 尚未检查 stop checkpoint，
> 必须证明 stop turn 单次发送、零返回 action 执行、之后 unregister；A sole owner，消息待 ACK，未跑 Maven。
>
> **CR271 TURN-40D R1 ACK / repair active（2026-07-19 21:00 EDT）：** A 已具名 ACK 修约并撤回
> authority 默认、第二 map、immediate-stop；sole owner 在原八文件按真实 696 authority 与 loop-owned
> checkpoint 返修。仍 3/8，未交付/未运行 Maven。

> **CR271 TURN-40D plan-contract repair（2026-07-19 20:55 EDT）：** 3/8 WIP 冻结。remote start
> 不得把 role/team/startup authority 默认化；SAME_TASK 复用 696 batch team 事实，SELECTED_TASK 逐窗口解析。
> pause/stop state 归 live loop，禁止 control-side 第二 map；stop-bearing turn 必须先于 stopped/unregister。
> guard 同秒竞态增量 `4AEF9A83`/198L 仅登记为待修 WIP，不增加 3/8 完成度。

> **CR271 TURN-40D first source batch（2026-07-19 20:40 EDT）：** loop+registry+guard 三路径已落；immutable
> start request 由 registry 在 stopped loop start 前 set-once，uncertain transport 原样重送，matching ack
> 后停附。guard 复用同一 mutex/gate/cleanup；无第二协议/factory 扩写集，其余 5 路径待续，尚非交付。

> **CR271 TURN-40D canonical claim（2026-07-19 20:25 EDT）：** External A 已从固定原卡 EOF 自领 8-path
> whole card，父级确认 physically-earliest sole owner；C anti-race 未竞争。当前 source active/recon，迁移
> 边界仍只限 control/lifecycle ownership，无业务差异，尚无 Java/build 变化。

> **CR271 TURN-40C Review #2 passed / TURN-40D READY（2026-07-19 20:14 EDT）：** 40C Repair #1
> `0/0/0`，35T/compile/15-path 全闭合并释放 owner。40D 的 7 production + 1 new test 固定写集已核无
> collision，开放 `READY / ZERO OWNER`；只迁移 control/lifecycle ownership，不改变业务决策。

> **CR271 TURN-40C repair source active（2026-07-19 20:14 EDT）：** runtime bean 已明确
> `destroyMethod=""`（`FBB02200`），activation test 新增第 8 个 annotation assertion（`8B1E11C3`）。
> repair-active stale 清除；communication stale 因无 ACK 保留。35T family/build/re-delivery 待续。

> **CR271 TURN-40C communication + repair active stale（2026-07-19 19:59 EDT）：** Review #1 repair
> 连续两轮无 A ACK，config/test 超 10 分钟保持 `4E91D53E/7B418DF0`，标 communication 与 repair-active
> stale；A owner/15-path 保留。P1/repair gate 不变，父级未跑 Maven。

> **CR271 TURN-40C parent source+test review #1 blocked（2026-07-19 19:47 EDT）：** canonical 15-path
> delivery 终审为 `P0/P1/P2=0/1/0`。runtime bean 默认 `@Bean` 会推断 public `close()`；Server 显式
> runtime close 后 host context 再 close，破坏 exact-once ownership/order。A owner 保留；限原写集补
> `destroyMethod=""` 与可识别重复 close 的 activation 证明，重跑 34-test family/build 后重交。

> **CR271 TURN-40C communication stale / source active（2026-07-19 19:30 EDT）：** R5+R1 连续两轮无
> A STATUS ACK，现标 communication stale、owner/WIP 保留。activation test 继续到 `864BFC9F`/369L/7T，
> 因最近源码变化不标 active stale；refresh/named/build/delivery 仍待，父级不并发 Maven。

> **CR271 TURN-40C R5 assembly source landed（2026-07-19 19:28 EDT）：** Host=`05FB55E9` 保旧
> overload 并注册 exact engine，Server=`CA2A1EF4` 将唯一 route `DecisionEngine` 传入 host，activation test=
> `3922CBAA`/7T 使用新 seam；scoped OCR bean=`4E91D53E`。ACK/refresh/named/build/delivery 仍待，非 review。

> **CR271 TURN-40C R5 scoped bean landed / registry restored（2026-07-19 19:26 EDT）：** runtime config
> `4E91D53E`/173 physical lines 已使用现有 `CloudServiceStorage` scope root 构造真实
> `OcrRoiMemoryService(Path)`，无 direct import/global store。R5 ACK 第一轮 pending；同一 DecisionEngine
> host seam 与 build/delivery 待续。第 16 节补回 C1-C4 四张既有 fixed card，恢复 88 行，不重开 owner。

> **CR271 TURN-40C plan-contract repaired R5（2026-07-19 19:24 EDT）：** R5 supersedes R4。OCR memory
> 不直接 import/global `config/`；由 runtime config 使用现有 `CloudServiceStorage` scope root 构造真实
> `OcrRoiMemoryService(Path)` 单例。Server 同一 `DecisionEngine` host 注册不变；15-path/scan/store 均不扩大。

> **CR271 TURN-40C plan-contract repaired R4（2026-07-19 19:18 EDT）：** 完整 constructor DAG 已收敛，
> 现有 dialog/UI imports 保留；仅再 import 真实 `OcrRoiMemoryService`，并由 host 注册 Server 已创建的同一
> `DecisionEngine` 实例以保留 `routeClickOverride`。15-path/scan 均不扩大，不改两个依赖源码、不建第二算法。
> A 已 ACK R3，通信 current；ACK R4 后恢复 host refresh 与冻结测试族。

> **CR271 TURN-40C communication recovered / audit continues（2026-07-19 19:05 EDT）：** A 已三重 ACK，
> stale 清除并暂停 imports；R3 竞态待 ACK。父级正分类 broad service scan 的 eager whole graph 与四 task
> 真实依赖，不向用户抛方向选择，卡继续 blocked、Java/Maven 停止。

> **CR271 TURN-40C plan-contract blocked / full import closure（2026-07-19 19:04 EDT）：** core R2 已 ACK，
> `TurnGameClient` 修复有效；config=`15E6F1E7` 逐层引入 dialog/UI ports 后又露非扫描 `OcrRoiMemoryService`。
> A owner/WIP 保留并暂停 Java/Maven；父级一次性审完整 prototype constructor DAG，不再逐缺口补 import。

> **CR271 TURN-40C communication stale（2026-07-19 18:59 EDT）：** A 连续两轮未 ACK Repair R2 原消息与
> R1 reminder；现标 `COMMUNICATION_STALE`，sole owner 与 `TurnGameClient=1B203987`/15-path WIP 保留。
> 下一事件须 ACK 三个 id；未达 `ACTIVE_STALE`，父级不跑 Maven。

> **CR271 TURN-40C fifteenth path landed / ACK pending R1（2026-07-19 18:55 EDT）：**
> `TurnGameClient=1B203987`/221L 已完成生产 ctor `@Autowired`，seam/`bind()`/业务不变；15-path source active。
> A 第一轮未 ACK Repair R2，未达 stale；host refresh/named/build/delivery 待，父级不跑 Maven。

> **CR271 TURN-40C plan-contract repair #2（2026-07-19 18:48 EDT）：** 14-path 已实现并通过 Cloud
> compile/test-compile；factory/runtime tests=`FEFB6DC2`/`DB3A486A`。父级全量核对两个 component-scan
> root 后确认唯一遗漏为 `TurnGameClient` 三构造器选择，合同扩为 15-path，仅准生产 ctor 加 `@Autowired`；
> seam/`bind()`/业务不变。A owner 保留，待具名 ACK 后恢复；尚非 delivery/review。foundation family 另有
> 7 个卡外 untracked `remote/run` decimal-HWND fixture collision，均早于 40C seam，不扩本卡写集。

> **CR271 TURN-40C runtime test WIP（2026-07-19 18:34 EDT）：** runtime contract test=`1D9D32D3`/825L；
> 追加 7 路径 6/7 已有增量，仅 factory test 未变。test gates/delivery 待，父级不跑 Maven。

> **CR271 TURN-40C production mechanics complete / main compile EXIT0（2026-07-19 18:32 EDT）：**
> factory=`3B511EE8` 与 runtime=`8368ED7E` 已把 fixed descriptor、exact context pre-provider bind、全 prototype
> pre-ack materialization/identity validation 与 same-context execution 落盘。追加路径 5/7；tests 待，尚非交付。

> **CR271 TURN-40C source active / mechanics 1+2+4 landed（2026-07-19 18:27 EDT）：** authority=
> `C651BD8D`、assembly=`69A51B55`、config=`E59C20B8` 已形成唯一共享 authority + fresh holder-backed handle；
> factory=`3B511EE8` 已加入固定 descriptor。repair batch 5 文件有增量、追加 7 路径 4/7；runtime/tests 待。

> **CR271 TURN-40C source active / first repair-batch increment（2026-07-19 18:17 EDT）：**
> `PlayerStateService=1E932914` 已完成生产构造器 Spring selection；
> 原 7 路径 `CloudTurnRuntimeConfiguration=64A54422` 已导入真实基础 bean 并增加 exact-context、unbound
> fail-closed 的 prototype startup gate。该时点追加 7 路径仅 PlayerState 有增量；尚非 delivery/review。

> **CR271 TURN-40C implementation resumed（2026-07-19 18:07 EDT）：** A 已 ACK 1757 并恢复完整
> 14-path 整卡；当前重读追加 7 路径，尚无新 Java 字节。A active，父级不跑 Maven；baseline-696 与零业务
> 差异合同不变。

> **CR271 TURN-40C plan contract repaired（2026-07-19 17:57 EDT）：** baseline-696 完整 14-path
> Spring/runtime 装配闭包已冻结；A 保持 owner 并可恢复。exact context 先于真实 prototype，startup gate 与
> turn coordination 复用现有唯一权威；无用户选择、无业务差异、无 stub/第二 store/authority。

> **CR271 TURN-40C communication recovered（2026-07-19 17:22 EDT）：** A 已双 ACK 1705/R2-1706，
> 确认 Java/Maven 停止、7-path WIP 冻结；stale 清除。计划合同仍 BLOCKED，完整装配闭包按 696 审计，
> 下一拍仅补 terminal recovery ACK，无用户选择。

> **CR271 TURN-40C communication stale（2026-07-19 17:16 EDT）：** External A 连续两轮未 ACK 父级
> 1705/R2-1706 合同裁定；A owner 保留、7-path WIP 不转交、Java/Maven 继续暂停。完整装配闭包仍按 696
> 审计；18:22 terminal recovery ACK 已闭合全部父级消息，无用户选择。

> **CR271 TURN-40C plan-contract blocked（2026-07-19 17:05 EDT）：** 7/7 authored，main compile/
> test-compile exit 0，named 2 PASS/5 host-refresh ERROR。完整闭包含 Player ctor selection、per-run startup
> authority、唯一 turn coordination、非扫描 Task bean 与 factory-before-context 顺序；父级按 696 审计，
> 无用户选择，禁止只补首类或用 stub/scan narrowing。
>
> **CR271 TURN-40C build progress（2026-07-19 16:45 EDT）：** Server=`9A3B17AB`/195L 已完成，6/7；
> activation test absent。Cloud main compile `EXIT 0`，test-compile/named/delivery 未发生；无用户选择。
>
> **CR271 TURN-40C source progress（2026-07-19 16:38 EDT）：** Routes=`063DE4FC`/94L 已加入批次，
> 当前 5/7；Server 未变、test absent。仍非 delivery/review；A writer active，父级不跑 Maven。
>
> **CR271 TURN-40C source progress（2026-07-19 16:37 EDT）：** Handler=`01DE94A2`/399L 已完成当前
> active batch，当前固定写集 4/7 有增量；Server/Routes 未变、test absent。仍非 delivery/review；
> A writer active，父级不跑 Maven，无用户选择。
>
> **CR271 TURN-40C source progress（2026-07-19 16:25 EDT）：** 固定写集已有 3/7 增量：Application=
> `5711BC3E`/112L、Host=`E90F22C8`/103L、RuntimeConfiguration=`D4636072`/105L；其余三 production
> 未变、activation test absent。仍非 delivery/review；A writer active，父级不跑 Maven，无用户选择。
>
> **CR271 TURN-40C source progress（2026-07-19 16:19 EDT）：** A 已创建固定写集第 1/7 个文件
> `CloudTurnRuntimeConfiguration=D4636072`/105L；五个 MODIFY SHA 未变，activation test absent。尚未整卡
> delivery/review；A writer active，父级不跑 Maven；baseline-A/696 与无业务差异合同不变。
>
> **CR271 TURN-40C claimed（2026-07-19 16:04 EDT）：** External A 是原卡 physical-earliest claimant；
> C 后到 claim 已自撤且零源码。40C=`SOURCE_ACTIVE / A SOLE OWNER`，固定 7-path 仍在 pre-claim 基线。
> C 已精确补 ACK recovery id+stale reminder，通信恢复；C owner-free，不影响 A 的 40C 写集。

> **CR271 runtime/factory Review PASSED / TURN-40C READY（2026-07-19 15:51 EDT）：** re-delivery #3
> 已把 aggregate test-compile 阻断清单修正为父级实测完整 12 个卡外 dirty tests；最终 `0/0/0`，C owner
> 释放。40C 经依赖、真实路径、collision 审计开放为 `READY / ZERO OWNER / UNASSIGNED`；无业务选择。

> **CR271 re-delivery #2 evidence P2（2026-07-19 15:39 EDT）：** 两项源码 P1 已关闭，父级 Cloud
> test-compile 与 CR main compile exit 0。CR aggregate test-compile 卡外失败清单误写 5 个，实测 12 个；
> C 仅修完整证据后 re-deliver。`0/0/1 BLOCKED`，40C 不开放，无业务选择。

> **CR271 runtime/factory repair ACK（2026-07-19 15:19 EDT）：** C 已精确 ACK Parent Review #1，
> 原整卡进入 `SOURCE_ACTIVE / REPAIR`。返修仍限定原 17-path 的 `0/3/1` findings；尚无 re-delivery，
> 审核与 40C 均保持 BLOCKED，无业务选择。

> **CR271 runtime/factory delivery Review #1 BLOCKED（2026-07-19 15:02 EDT）：** 父级终审
> `P0/P1/P2=0/3/1`。queue skip/create failure 前 stale context、runtime duplicate ack 前未验 authority、冻结
> Maven build gate 未闭合；runtime test 当前实际 22T，delivery 24T 口径需修。C 原整卡返修，40C BLOCKED。

> **CR271 all six implementation steps complete（2026-07-19 14:52 EDT）：** runtime 24/24、handler 7/7，
> producer/validator/lifecycle/core 保持绿。仍待 full family、shared byte audit、双 compile、17-path evidence
> 和 canonical delivery；尚未整卡 review，40C BLOCKED。

> **CR271 producer test gate passed（2026-07-19 14:42 EDT）：** `DE50232B` 17/17 经父级 Review #4
> `0/0/0 PASSED`，runner order/双 detach/atomic/current handle+context 各单读/六事实正负例闭合。
> consumer step⑤⑥开放，整卡尚未 delivery，40C BLOCKED。

> **CR271 detach passed / projection repair（2026-07-19 14:37 EDT）：** 三 ACK 齐、stale 清。17/17 已
> 闭合 runner order/双 detach/atomic；projection 仍缺 executionContext 单读计数与六 authority 字段全空
> negative。consumer/40C 继续 BLOCKED。

> **CR271 producer repair communication stale（2026-07-19 14:32 EDT）：** C 两轮未 ACK 1404，现 stale、
> 非 active stale。17/17 projection WIP 可保留，但 detach-order P1 未闭合；`BA515A97` 观察字段尚未接线。
> producer/consumer/40C 继续 BLOCKED。

> **CR271 runner order green / detach test repair（2026-07-19 14:22 EDT）：** `2E73E978` 14/14 真实
> runner 已证明 clear<update<publish<execute；但返回后双 null 不能证明 clear-before-detach 相对顺序。
> 同路径 RecordingHandle 须在 clear 瞬间验证仍 attached；projection/consumer/40C 继续 BLOCKED。

> **CR271 producer-test communication recovered / harness corrected（2026-07-19 14:04 EDT）：** 三 exact ACK
> 已齐；`D7B1143E` 13/13 仅真实闭合第二 detach。无需用户选择/seam：`AUTO_BATTLE` 驱内层生产排序，
> 空队列驱外层 runQueue terminal finally，resolveForAction 驱单快照六事实；consumer/40C 继续 BLOCKED。

> **CR271 producer-test repair communication stale / recon active（2026-07-19 14:00 EDT）：** 两拍未
> 精确 ACK original/reminder1，现 stale；C 已停 consumer 回 test recon，无新字节，非 active stale。BareRunner
> projection 方案仅一半，仍须 runner production order + dual detach proof。Cloud runtime 未审，40C BLOCKED。

> **CR271 producer-test repair first missed / consumer WIP out of order（2026-07-19 13:56 EDT）：** C 未
> ACK Repair #1 即写 Cloud runtime=`53FE8363`；第一拍漏回执未达 stale。consumer compile 仅未审 WIP，不能
> 越过 false-positive producer test gate。须先同路径补 production wiring proof，再恢复 consumer；40C BLOCKED。

> **CR271 communication recovered / producer-test Repair #1（2026-07-19 13:50 EDT）：** 三 exact ACK
> 已齐，stale 清除。`7F0DCA39` 12/12 只手工测 RunningTaskHandle，不覆盖 runner production 顺序或双 detach，
> 是 false-positive gate。须在同一既有 test path 补 production wiring + 单快照投影证明，再进 consumer；
> source acceptance 保持，未 delivery，40C BLOCKED。

> **CR271 A#5 source corrected / communication stale（2026-07-19 13:42 EDT）：** runner=`CE4DDA83`
> 已闭合 clear-before-update 与两处 clear-before-detach，TurnExecutionWindow=`8AF1BED9` 单快照保持；但 C
> 连续两拍未精确 ACK original+reminder1，现 communication stale。producer test 的 no-prior-context proof
> 仍缺，先补该门再进 consumer；未 delivery，40C BLOCKED。

> **CR271 A#5 first missed ACK / producer WIP noncompliant（2026-07-19 13:37 EDT）：** C 未 ACK A#5
> 即改 producer。实际 `WindowTaskRunner` 仍 update→publish，且第二 detach 点未 clear，未满足固定的
> clear→update→publish→execute / terminal clear-before-detach。第一拍漏回执未达 stale；owner retained，
> reminder1 已发，未 delivery，40C BLOCKED。

> **CR271 A#4 done / producer transition repair（2026-07-19 13:32 EDT）：** A#4 双仓 source/fixture
> 修正且 5/5+7/7+18/18。RunningTaskHandle 首增量后发现 queue transition stale-context race；A#5 固定
> clear→update→publish→execute、terminal clear-before-detach，写集17。待 ACK，40C BLOCKED。

> **CR271 communication recovered / A#4 first missed（2026-07-19 13:26 EDT）：** C 已精确 ACK
> A#3+reminder2，stale 清除；A#4 后首拍未 ACK，未达 stale。pathingSnapshot NON_NULL/fixture remove-null
> 尚未实施；17-path 未 delivery，40C BLOCKED。

> **CR271 A#3 ACKed / pathing-null compatibility repair（2026-07-19 13:24 EDT）：** lifecycle 5/5、core
> 7/7、validator 18/18；但 request-start fixture 越界加入 pathingSnapshot:null。A#4 固定双仓 metadata
> nullable pathingSnapshot `NON_NULL` 并删 fixture null，写集仍17、无业务差异。待 ACK，40C BLOCKED。

> **CR271 A#3 communication stale / source active（2026-07-19 13:21 EDT）：** C 连续两拍未 ACK A#3，
> 已 stale；validator test 双仓 `C32D4522` 同形、isolated 18/18，故非 active stale。17-path lifecycle 修复
> 尚未 delivery，reminder2 已发，40C BLOCKED。

> **CR271 A#2 communication recovered / A#3 first missed（2026-07-19 13:10 EDT）：** C 已 ACK A#2
> original+reminder；A#3 后首条 event 未 ACK，未达 stale。17-path lifecycle golden 修复合同已固定，待下一拍
> ACK 后实施；无 delivery，40C BLOCKED。

> **CR271 lifecycle golden contract repair（2026-07-19 13:08 EDT）：** shared task-start authority validator
> 保留；双仓 lifecycle golden test + `request-start.json` 纳入写集，13→17，用合法非默认 authority 测试事实
> 修正过期输入。core helper/非 task-start fixtures 继续只读。C 已 ACK A#2 original、core 7/7；A#3 待 ACK，40C BLOCKED。

> **CR271 contract repair #2 physical correction（2026-07-19 13:06 EDT）：** 双仓 metadata=`D22B62D9`
> 已闭合 boxed Boolean/六 NON_NULL/legacy 六 null，validator=`56383C98` 已显式拒绝 missing booleans，且
> 两仓字节同形。C 尚未 ACK original+reminder、未报 build/test 或 delivery；第一次漏回执未达 stale，40C BLOCKED。

> **CR271 contract repair #2 first missed ACK（2026-07-19 13:02 EDT）：** C 未 ACK 即产生 validator
> `4799662C`，该 WIP 仍基于 primitive booleans，不能证明 missing rejection。记第一次漏回执，owner retained；
> 须先修 metadata 再重做，40C BLOCKED。

> **CR271 runtime metadata contract repair #2（2026-07-19 12:56 EDT）：** 首增量 primitive boolean 无法
> 区分 missing/false，旧 ctor 也会改变严格 JSON。修订为 boxed Boolean + 六项 NON_NULL；双仓 core golden
> tests/fixtures 只读且必须原样通过。C owner retained，待 ACK；40C BLOCKED。

> **CR271 runtime/factory baseline amendment ACK（2026-07-19 12:51 EDT）：** C 已精确 ACK 13 路径
> baseline-A 合同，P1-5 解锁并恢复 SOURCE_ACTIVE；A idle。尚无 amendment source/delivery，父级不跑 Maven，
> `TURN-40C` 继续 BLOCKED。

> **CR271 runtime/factory baseline authority decision（2026-07-19 12:35 EDT）：** 用户明确旧代码行为即
> 唯一合同，A/B 分叉固定为 A；B 未批准。DHXY 从 exact active `TaskExecutionContext` 透传 role/team/startup
> 事实，Cloud 严格消费，缺失时 ack/materialize 前拒绝。C owner retained，DHXY 7 + Cloud 6 路径待实施；
> `TURN-40C` 继续 BLOCKED。

> **CR271 runtime/factory repair complete except P1-5（2026-07-19 12:28 EDT）：** 当前七文件隔离编译
> exit 0，factory 2/2 + runtime lifecycle 20/20=`22/22`；P1-1..4/P2-1 已闭合但仍非 canonical
> re-delivery/review。P1-5 继续等待用户 A（推荐）/B。
> P1-5 仍待用户 A/B 决策，未决前禁止 re-deliver；父级不跑 Maven，TURN-40C 继续 BLOCKED。

> **CR271 runtime/factory Review #1 blocked（2026-07-19 11:47 EDT）：** C 的 7/7 delivery 已由父级
> 审核为 `P0/P1/P2=0/5/1 BLOCKED / REPAIR REQUIRED`。named 11/11 与 Cloud testCompile/compile EXIT0
> 保留为构建证据；prototype、ack/worker、exact device+window、exception/aggregate/close、role/team/startup
> authority 与 test matrix 均未闭合。C owner 保留；待用户 A（推荐 protocol facts）/B（批准默认差异）裁决，
> `TURN-40C` 继续 BLOCKED。

> **CR271 runtime/factory runtime WIP updated（2026-07-19 11:42 EDT）：** production runtime 从
> `30128CFD` 更新为 `704650C7`/9646B/201L；当前 runtime/test=`704650C7`+`598FD192`。物理仍 7/7，
> 但当前字节尚无 build/test 回执与 delivery/review。父级不跑 Maven，40C blocked。

> **CR271 runtime/factory final-test WIP updated（2026-07-19 11:39 EDT）：** runtime contract test 从
> `C0C81975` 更新为 `598FD192`/19002B/427L；物理仍 7/7，但尚无 final build/test 回执与 delivery/review。
> 父级不跑 Maven，40C blocked。

> **CR271 runtime/factory physical source+test 7/7（2026-07-19 11:37 EDT）：** final runtime contract test=
> `C0C81975`/18083B/412L 已落盘，七固定路径全部存在。final test 尚无 Worker build/test 回执；未
> delivery/review，父级不跑 Maven，40C blocked。

> **CR271 runtime/factory source+test increment 6/7（2026-07-19 11:32 EDT）：** factory allowlist test=
> `F274A975`/6379B/129L 已落盘并由 C 报告通过 1/1。仅 runtime contract test absent；未 delivery/review，
> 父级不跑 Maven，40C blocked。

> **CR271 runtime/factory production build activity（2026-07-19 11:21 EDT）：** C 回执五 production
> 联合 compile EXIT0；当前 5/7，两 test absent。未 delivery/review/整卡 Maven，父级不跑 Maven，40C blocked。

> **CR271 runtime/factory increment 5/7（2026-07-19 11:20 EDT）：** runtime=`30128CFD`/9403B/197L
> 已落盘，五 production 全部存在，只剩两 test absent。runtime/control port 暂无 Worker build 回执；
> 未 delivery/review，父级不跑 Maven，40C blocked。

> **CR271 runtime/factory increment 4/7（2026-07-19 11:18 EDT）：** registry=`576B2DEA` 已有 Worker
> compile EXIT0 回执；control port=`56DA5571`/1806B/42L 新落盘但暂无 build 回执。runtime 与两 test absent；
> 未 delivery/review，父级不跑 Maven，40C blocked。

> **CR271 runtime/factory increment 3/7（2026-07-19 11:15 EDT）：** C sole owner 已新增
> registry=`576B2DEA`/3237B/73L；factory/start-result 的 individual compile EXIT0 报告保持，registry 暂无
> Worker build 回执。余四路径 absent，未 delivery/review，父级不跑 Maven，40C blocked。

> **CR271 runtime/factory build activity（2026-07-19 11:10 EDT）：** C 报告 factory=`B2839BE9` 与
> start-result=`BE8A15BF` 均单文件 compile EXIT0；五路径 absent，未 delivery/review/整卡 Maven，40C blocked。

> **CR271 runtime/factory increment 2/7（2026-07-19 11:08 EDT）：** C 已创建 factory=`B2839BE9`
> 与 start-result=`BE8A15BF`；其余五路径 absent。未 delivery/review，父级不并发 Maven，40C blocked。

> **CR271 runtime/factory increment 1/7（2026-07-19 11:05 EDT）：** C sole owner 已创建
> `CloudTurnTaskFactory=B2839BE9`/2482B/53L，报告单文件 compile EXIT0；其余六路径 absent。未 delivery/review，
> C 写入期间父级不跑 Maven，`TURN-40C` blocked。

> **CR271 runtime/factory claimed（2026-07-19 10:59 EDT）：** External C 是原卡 physical EOF 唯一最早
> whole-card owner，现 `SOURCE_ACTIVE`；A ACK 后 idle，无冲突。七个 CREATE 路径仍 absent，父级不跑 Maven；
> `TURN-40C` 保持 BLOCKED。

> **CR271 P-COMPILE 终审通过 / runtime-factory 开放（2026-07-19 10:49 EDT）：** 父级 Review #2
> `P0/P1/P2=0/0/0`，A owner 释放且通信 stale 清除；full testCompile EXIT0、四 WholeTask `67/67`、
> Cloud compile EXIT0。既有五 production + 两 test 的 `TURN-40B/RUNTIME-FACTORY` 整卡现为
> `READY / ZERO OWNER / UNASSIGNED`；`TURN-40C` 仍 BLOCKED。

> **CR271 P-COMPILE COMMUNICATION_STALE（2026-07-19 09:19 EDT）：** A 连续两轮未 ACK stale
> 状态询问，现为 `ACTIVE_STALE / COMMUNICATION_STALE`。Maven 存活但 08:56 后无新报告/源码；owner 保持，
> testCompile clean/production frozen 不回退。父级不终止进程、不并发 Maven；runtime/factory/40C blocked。

> **CR271 P-COMPILE named-test ACTIVE_STALE（2026-07-19 09:09 EDT）：** 5-test Maven 仍存活，
> 但 08:56 后无 FiveRing/Xiuluo/tracker 报告或测试源码增量，超过 10 分钟。A owner 保持，已定向询问
> 并待 ACK；testCompile clean/production frozen 不回退，runtime/factory/40C blocked。

> **CR271 P-COMPILE testCompile clean（2026-07-19 08:59 EDT）：** Review #1 的 27 testCompile errors
> 已清零。A 正运行四 WholeTask + tracker named tests，并处理 runtime fixtures；production frozen，尚无
> re-delivery/review，runtime/factory/40C blocked。

> **CR271 P-COMPILE testCompile progress（2026-07-19 08:54 EDT）：** 8/10 fixed test files now
> compile clean; only `FiveRingTaskTrackerTurnContractTest` remains with 14 errors. WholeTask runtime failures,
> four named tests and re-delivery remain pending; production frozen and runtime/factory/40C blocked.

> **CR271 P-COMPILE Review #1 ACK（2026-07-19 08:39 EDT）：** A 已具名 ACK 固定 10-test
> test-only repair 并开始返修，sole owner 保持；C idle、通信正常。production/main compile EXIT=0 冻结，
> 尚无 re-delivery；runtime/factory 与 40C blocked，deprecated Navigation 排除。

> **CR271 P-COMPILE Review #1 blocked（2026-07-19 08:34 EDT）：** production/compile 无 finding；
> SOURCE+TEST 因 27 testCompile errors + 8 isolate failures 为 `0/1/0 BLOCKED`。A owner retained，production
> frozen，固定 10-test harness/compile-only repair；runtime/factory 与 40C blocked，deprecated Navigation 排除。

> **CR271 P-COMPILE main compile green（2026-07-19 08:27 EDT）：** 固定四文件修复已落盘，Cloud
> main compile EXIT=0，原六错清零。A 继续 sole owner 做适用 named tests 隔离验证；full-tree testCompile
> 被写集外测试债阻断，尚无交付/review。runtime/factory tail 与 40C blocked，deprecated Navigation 排除。

> **CR271 P-COMPILE claim reconciliation（2026-07-19 08:16 EDT）：** A 的原卡 claim 物理在前，
> 为 sole owner / SOURCE_ACTIVE；C 后 claim 已撤回并确认零源码写入。四文件/六错误合同不变，Java writer
> active，不跑 Maven；runtime/factory tail 与 40C blocked，deprecated Navigation 旧链排除。

> **CR271 aggregate compile recheck（2026-07-19 08:01 EDT）：** 完整 Cloud javac 将旧 7-file debt
> 收敛为 4 production files/6 errors。`TURN-40B/P-COMPILE` 现为 `READY / ZERO OWNER / UNASSIGNED`，只做
> String taskRunId、record factory、terminal intent 与 null-context Cloud 边界适配；无业务差异，不触碰
> deprecated Navigation 旧链。runtime/factory tail 与 40C 继续 blocked。

> **CR271 P-NAV closure ACKed（2026-07-19 07:56 EDT）：** C 已精确 ACK Review #2 与 owner release，
> P-NAV CLOSED/PASSED，A+C idle available。无 READY 卡；aggregate 7-file debt、runtime/factory tail BLOCKED/
> ZERO OWNER 与 TURN-40C blocked 不变。

> **CR271 P-NAV Review #2 passed（2026-07-19 07:46 EDT）：** `D56DEAFD` + `87C6BC45`/23T
> 审核 `0/0/0 PASSED`；四个 current-yellow tests 闭合旧链 P1，C owner released。isolate 528-file 0 error +
> 23/23。aggregate 7-file blocker 未清，runtime/factory tail BLOCKED/ZERO OWNER，TURN-40C blocked。

> **CR271 P-NAV repair content complete（2026-07-19 07:41 EDT）：** test=`87C6BC45`/23T，四个
> deprecated direct tests 已全换 current yellow；旧 helper 调用清零，LEGACY 仅负断言。production
> `D56DEAFD` frozen；等待 ACK/isolated verification/re-delivery，不跑 Maven。

> **CR271 P-NAV repair active（2026-07-19 07:36 EDT）：** test=`65DEF10A`；首个 legacy test 已改为
> current yellow memory + no-`LEGACY_GREEN_LINK` proof，余 3 个 deprecated direct tests 在修。production
> `D56DEAFD` frozen；C ACK pending，Java writer active，不跑 Maven；aggregate 7-file blocker 不变。

> **CR271 P-NAV Review #1 blocked（2026-07-19 07:19 EDT）：** delivery `D56DEAFD` +
> `2FDB2D02`/23T 为 `0/1/0 BLOCKED`。production 无新增 finding；4T 直接调用已排除的 deprecated 旧 helper
> 并使用 `LEGACY_GREEN_LINK` fixture，current yellow destination + mini-map 缺直接覆盖。C owner retained，
> test-only repair；不改 deprecated production、不重开 P-PROTO/P-CLIENT。aggregate build 仍受写集外 7-file debt 阻断。

> **CR271 P-CLIENT Review #2 blocked（2026-07-19 01:42 EDT）：** Repair #1 production accepted；33T 的
> failed replace fixture 被 validator 前置拒绝，非成功 outbound/smuggle negative 与 reason nonblank JavaDoc 未闭合。
> verdict=`0/1/2`，C owner retained，同两文件/33T Repair #2；P-NAV 不开放。
>
> **CR271 P-CLIENT repair progress（2026-07-19 01:37 EDT）：** client=`AC14E006`/520L 已闭合
> result-kind/shape/JavaDoc；test=`D827B8D8`/529L/33T 正补 outbound mapping。无 re-delivery，P-NAV 不开放。
>
> **CR271 P-CLIENT communication recovered（2026-07-19 01:32 EDT）：** C 双 ACK review+stale，清除 stale 并
> REPAIR_ACTIVE。client=`AC14E006`/520L，test 仍 `541B4D14`/33T；同两文件合同不变，P-NAV 不开放。
>
> **CR271 P-CLIENT communication stale（2026-07-19 01:27 EDT）：** C 连续两轮未 ACK Review #1 `0/2/1`
> 返修消息，标 `COMMUNICATION_STALE`。owner、delivery SHA、原两文件与 33T 合同不变；P-NAV 不开放。
>
> **CR271 P-CLIENT Review #1 blocked（2026-07-19 01:22 EDT）：** delivery `FFEB7679`+`541B4D14`/33T
> 审核为 `0/2/1`。新 op 缺 result-kind，pending route field 缺严格 shape closure；测试未证明 outbound
> operation/payload/reason 且 replacement fixture 违反 validator routeMode。C owner retained，同两文件/33T 返修。
>
> **CR271 P-CLIENT source progress（2026-07-19 01:17 EDT）：** client production 已完成为
> `FFEB7679`/481L；test 正在写入，最近观测 `73D44A6D`/420L/27T，尚无 33T/canonical delivery。
> C 保持 SOURCE_ACTIVE；P-NAV 继续只等 P-CLIENT，父级不跑 Maven。
>
> **CR271 A ACK / P-CLIENT recon（2026-07-19 01:07 EDT）：** A 已 ACK P-LOCAL pass/release 并 idle available。
> C 已完成 P-CLIENT builder/arguments/result/client 范式 recon，所需变更仍闭合于冻结 client+test 两文件；
> 两文件 SHA 未变、无 delivery。P-NAV 继续只等 P-CLIENT。
>
> **CR271 P-CLIENT claimed（2026-07-19 01:02 EDT）：** External C 已在 P2 原卡 physical EOF canonical 自领
> P-CLIENT 并进入 SOURCE_ACTIVE；Cloud client/test 仍为 `59BF77E8`/414L 与 `0A248C8B`/417L/27T 基线字节，
> 尚无增量。P-NAV 继续仅等待 P-CLIENT；Java writer active，本轮不跑 Maven。
>
> **CR271 P-LOCAL Review #2 passed（2026-07-19 00:59 EDT）：** A 并发 ACK/Repair #1 re-delivery 后，父级
> review=`0/0/0 PASSED`。22T 已证明 two-runtime exact binding 与 queued second identity/reason，同时保留 first
> live outcome；production/runner 未改，A owner 释放且通信恢复。P-PROTO/P-OCR/P-LOCAL passed；P-NAV 仅等 P-CLIENT。
>
> **CR271 communication audit（2026-07-19 00:57 EDT）：** C 已 ACK P-OCR `0/0/0`、owner release 与冻结边界，
> 当前 idle available。A 连续两轮未 ACK P-LOCAL `0/1/0` test-only repair，标 `COMMUNICATION_STALE`；owner、
> blocker 与 22T 条件保留，源码未变化且尚不标 ACTIVE_STALE。P-CLIENT READY/ZERO OWNER；下游 blocked。
>
> **CR271 P-OCR Review #3 passed / P-LOCAL Review #1 blocked（2026-07-19 00:47 EDT）：** P-OCR 为
> `0/0/0 PASSED`，目标 OCR 分支证据与批准差异注释闭合，C owner 释放。P-LOCAL production bridge 接受，
> 但 22T 缺 exact-window key isolation 与 queued replacement identity/reason，review=`0/1/0 BLOCKED`，A owner
> retained 做 test-only repair。P-CLIENT READY/ZERO OWNER，P-NAV/runtime/factory/40C blocked；父级未跑 Maven。

> **CR271 P-OCR Review #1 ACKed / Java held（2026-07-18 23:26 EDT）：** C 已具名接受 `0/3/1` 全部
> finding，owner 保留、通信正常，交付 blobs 冻结不变，等用户 A/B 决策后再返修。A 的 P-PROTO 八个双仓
> 同字节 source/test 文件已齐但未 canonical delivery；无 Maven/runtime/input。

> **CR271 P-OCR Review #1 blocked（2026-07-18 23:15 EDT）：** C 的三文件交付经父级审核为
> `P0/P1/P2=0/3/1`。blank expected-name allow 语义反转；696 配置的 hybrid local-first/Baidu matcher fallback
> 被降成不重试 `LocalOcrClient`；7T 未证明 packed/wrapped/green-link/raw fallback；public OCR JavaDoc 不完整。
> C owner 保留，P-OCR/P-NAV blocked。唯一待用户决策：扩计划保留 hybrid（推荐），或批准 Cloud 单 provider 差异。

> **CR271 P-PROTO Amendment #6 ACKed / unblocked（2026-07-18 22:55 EDT）：** A 已具名 ACK 精确
> 9/11 字段 mirror 与独立 replacement reason；双仓 `TurnPendingTransferChoice`=`5CAF8C15`、
> `TurnPendingRouteOutcome`=`B3C9B713` 同字节，P-PROTO 恢复 source active。Args/Result/Validator/tests 未完成，
> 无 delivery/新 READY；C 继续 P-OCR，双 writer 不跑 Maven。

> **CR271 P-PROTO payload contract repaired（2026-07-18 22:49 EDT）：** 父级批准两个双仓同字节纯协议
> mirror record，分别精确承载 local transfer-choice 9 字段与 route-outcome 11 字段；`routeMode` 仅传 enum name。
> Arguments 另增独立 replacement reason，禁止复用公共 diagnostics source。A owner 保留、ACK 前合同阻断；
> C 的 P-OCR enum `F67FDF75` 已完成 1/3。无业务差异；双 writer 时不跑 Maven。

> **CR271 P-OCR unblocked / dual source active（2026-07-18 22:44 EDT）：** C 已具名 ACK 完整
> DecisionEngine+enum+7T 边界，临时合同阻断关闭并 source active。A 的两仓 `TurnLocalOperation` 三 op 已
> byte-identical=`D199953C`。双 Java writer 活动中，未跑 Maven；下游门保持 blocked。

> **CR271 claims / P-OCR contract correction（2026-07-18 22:39 EDT）：** A 已领 P-PROTO 并 source active。
> C 已领 P-OCR，但 claim 漏掉 Review #5 并入的 `TextCandidateScanStatus`；owner 保留，P-OCR 在 C 具名 ACK
> 完整 DecisionEngine+enum+7T 三文件边界前 PLAN-CONTRACT BLOCKED。当前 OCR 源码未变；无 Maven/runtime/input。

> **CR271 P2 Review #5 passed / two READY boundaries（2026-07-18 22:26 EDT）：** re-delivery #4 父级终审
> `P0/P1/P2=0/0/0`，A 的 report owner 释放。父级把无测试 enum 微卡并入完整 P-OCR，并纠正 standalone
> jar 为 `2,680,679B/A1DE5578`。`TURN-40B/P-PROTO` 与 `TURN-40B/P-OCR` 现为 READY/ZERO OWNER；
> LOCAL/CLIENT/NAV/runtime 与 TURN-40C 仍按依赖阻断。无 Java/Maven/runtime/input。

> **CR271 P2 Review #2 communication recovered（2026-07-18 21:41 EDT）：** A 已 double-ACK Review #2 与 stale
> 消息，确认误把 frozen baseline 当 current authority，并切回 `DHXY-cr271` 的 `PendingRouteOutcome` lifecycle
> 返修。清除 `COMMUNICATION_STALE`；A 保留 report-only owner 与 `0/4/1` 范围。尚无 re-delivery #2 或实现卡
> READY，TURN-40C 继续 BLOCKED。
>
> **CR271 P2 Review #2 communication stale（2026-07-18 21:31 EDT）：** A 在 Review #2 定向消息之后连续
> 两个 physical STATUS EVENT 未 ACK 且误报卡内无 Review #2，按合同标 `COMMUNICATION_STALE`。A 保留 report-only
> owner 与 `0/4/1` 返修范围；不撤卡、不重派、无实现卡 READY，TURN-40C 继续 BLOCKED。
>
> **CR271 P2 Parent Review #2 blocked（2026-07-18 21:28 EDT）：** 正式 re-delivery 复审为
> `P0/P1/P2=0/4/1`，A 保留 report-only owner。报告仍依据 retired world-map pending-memory API；current CR
> authority 已是 `PendingRouteOutcome` replacement/abandonment/report-delivery。dialog-request liveness、唯一 OCR
> owner、逐卡 literal 写集与 exact test gate 仍需返修。无实现卡 READY，TURN-40C 继续 BLOCKED。
>
> **CR271 P2 direction ACK / communication recovered（2026-07-18 21:19 EDT）：** External A 已具名 ACK
> Cloud-slots 否决消息，撤回第二状态库方向，并改为 typed cross-repo `LOCAL_SERVICE` 写回唯一 local
> `WindowRuntimeContext`。shared operation/arguments/validator、Cloud client、DHXY executor/dispatcher 与双边
> tests 均须进入完整 cohort；本地 watcher/CAS/get-and-set 语义保留。尚无正式 re-delivery 或实现卡 READY，
> TURN-40C 继续 BLOCKED。
>
> **CR271 P2 Cloud runtime slots rejected（2026-07-19）：** `WindowTaskRunner` 在本地消费 dialog preparation
> request 并结算 pending transfer/route-result memory，现有 dialog-runtime fact 也读取同一 `WindowRuntimeContext`。
> 新 Cloud slots 会形成双权威且 watcher 看不到。P2 必须改为 typed `LOCAL_SERVICE` 写回本地唯一 owner，并冻结
> shared protocol、Cloud client、DHXY executor 与双边 tests；无实现卡 READY，TURN-40C 继续 BLOCKED。
>

> **CR271 P2 two owner gaps confirmed（2026-07-19）：** 除 exact-window runtime-state owner 缺口外，逐方法
> 审计确认 raw `LocalOcrClient` 与 yellow-only `DecisionEngine` 均不拥有完整路线 OCR；typed destination/coord、
> green fallback、packed/wrapped/same-row/raw fallback 仍缺 canonical owner。原两文件 trivial rewire 结论失效；
> 无实现卡 READY，TURN-40C 继续 BLOCKED。
>

> **CR271 P2 runtime-state gap confirmed（2026-07-19）：** 父级源码审计确认 Cloud 仅有只读
> `WHOLE_TASK_DIALOG_RUNTIME_READ` 与 prepared-action slot；`DialogPreparationRequest` update/clear 及 pending
> transfer/route-result 两个窗口态槽均无已证明的 Cloud owner。P2 必须冻结一个 canonical exact-window owner
> 及 CAS/get-and-set/clear/key 测试；不得造第二 store。无实现卡 READY，TURN-40C 继续 BLOCKED。
>

> **CR271 P2 Review #1 ACK / repair active（2026-07-19）：** External A 已具名 ACK
> `PARENT-TURN40B-P2-REVIEW1-REPAIR-20260719`，通信正常并保留同一 report-only owner。`0/4/1` 五项合同缺口
> 正在同卡返修；无实现卡 READY，TURN-40C 继续 BLOCKED，未运行 Java/Maven/runtime/input。
>

> **CR271 P2 Review #1（2026-07-19）：** External A 的 report delivery 父级终审为
> `P0/P1/P2=0/4/1 / BLOCKED / REPAIR REQUIRED`。必须补全 `WindowRuntimeContext` 全状态 owner、路线 OCR
> preprocessing/fallback 单一 owner、受影响 tests、pre-build/runtime DAG 与正确 Cloud build gate。A owner retained；
> 无实现卡 READY，TURN-40C 继续 BLOCKED。
>

> **CR271 2026-07-19 physical-EOF audit：** External A 已具名双 ACK 2017/2047，并在固定 P2 原卡 EOF
> 完成唯一 canonical self-claim；C 随后双 ACK 并正确不竞争，双 lane 通信均恢复。`TURN-40B-P2` 现为 `REPORT_ACTIVE / OWNER A`，
> 只写报告并闭合 33-error 两族的完整传递依赖与最小实施 cohort。TURN-40C 仍 BLOCKED/NOT READY。
>

> **CR271 2026-07-19 00:47 UTC：** TURN-40B-P2 仍 READY/ZERO OWNER；A/C 多轮漏读发布消息并继续误报
> NO-CLAIMABLE，双 lane 标 `COMMUNICATION_STALE`。父级已发 2047 恢复消息；不派卡、不撤卡，首次原卡 EOF
> anti-race claim 仍为唯一 owner 依据，40C gate 不变。
>

> **CR271 2026-07-19 00:17 UTC：** 父级纠正 DAG：C1-C4 不是 TURN-40B runtime/factory 本体，40C 仍
> BLOCKED/NOT READY。fresh Cloud compile 的 33 errors 仅归两族：缺 `TextCandidateScanStatus`，以及
> `NavigationService` 仍依赖九个 DHXY-local 类型。固定 report-only 原卡 `TURN-40B-P2` 已公开
> READY/ZERO OWNER/UNASSIGNED，用于闭合完整 live/dead、turn-native owner、精确实施写集与 aggregate compile
> 点；不写 Java、不派卡、不准 stub/复制算法/第二协议或 store。
>

> **CR271 2026-07-18 23:32 UTC：** 父级 stable-window Cloud compile 复证仍失败：OCR/status、tracker/input、
> navigation helper、window runtime 等类型不在 Cloud source set，故 Navigation/old-facade/Wubei 三个授权
> named tests 未进入执行。该 aggregate blocker 不回退 C2，也不授权 stub/复制业务逻辑。
>
> **CR271 2026-07-18 23:22 UTC：** TURN-40B-C2 父级 Review #7=`P0/P1/P2=0/0/0 / PASSED`，A owner
> released，Repair #6 收口。生产 `77692F3F`、只读测试 `16B93D61`；父级 compile EXIT0，隔离 named test
> 14/14 全绿（typed STOPPED、零 mouse queue、零 capture）。全局历史 testCompile 债独立保留。
>
> **CR271 2026-07-18 23:17 UTC：** TURN-40B-C2 已由 External A 在原卡 EOF canonical self-claim，状态改为
> `OWNER A / SOURCE ACTIVE`。唯一生产写集仍是 `TurnExecutionWindow.java`（当前 `77692F3F`），只读测试仍为
> `16B93D61`；父级未派卡，Java writer 活跃期间不跑 Maven。
>
> **CR271 2026-07-18 23:02 UTC：** TURN-40B-C2 isolated red 根因是 `TurnExecutionWindow` resolve 阶段
> current handle 双读，metadata 读消耗 original 后 snapshot 错冻 successor。原卡已修为 READY/zero owner，
> 单 production 文件做 single-snapshot 修正，existing replacement-race test read-only；无 bag/input/业务差异。

> **CR271 2026-07-18 22:57 UTC：** TURN-39C1 Review #2=`P0/P1/P2=0/0/0 PASSED`，A owner released，
> communication recovered。OCR/scroll 直接边界证明与全 production retired-type 扫描闭合；production
> `B57ECC50` 不变、无业务差异。TURN-39 source closed；named/compile 仍 shared debt blocked。

> **CR271 2026-07-18 22:52 UTC：** A 连续两轮漏 ACK Review #1 message 2240，标
> `COMMUNICATION_STALE`；两份授权 test 已有 fresh repair bytes，故非 `ACTIVE_STALE`。production SHA 不变，
> owner/`0/2/0` test-only scope/build blocker 不变，无换卡/Maven/runtime/input。

> **CR271 2026-07-18 22:40 UTC：** TURN-39C1 canonical delivery 父级 Review #1=`P0/P1/P2=0/2/0 / BLOCKED`。
> 20 个 checkpoint 迁移与五文件删除未发现业务差异；阻断仅为 Navigation OCR/scroll 直接测试证明缺口，及
> retired-five-type 扫描错误跳过 TURN-44A SCC 17 文件。A owner retained 同卡 test-only repair；build pending。

> **CR271 2026-07-18 22:05 UTC：** External A 已从 TURN-39C1 原卡 physical EOF canonical claim，唯一最早
> owner，状态 `SOURCE ACTIVE`；本轮相关 Cloud 源码/测试 SHA 未变。External C 已具名 ACK C4 Review #10
> 并释放转 idle。无 owner collision/派卡；A 为 Java writer，父级不跑 Maven，build 仍 BLOCKED/PENDING。

> **CR271 2026-07-18 21:51 UTC：** TURN-40B-C4 final parent review passed `P0/P1/P2=0/0/0`; C owner
> released. A full symbol audit corrected TURN-39C1: `NavigationService` is the sole active external owner of
> `InputActionScope`, so 39C1 must first migrate those exact checkpoints to existing `TaskCheckpoint`/turn outcomes,
> then delete the five-file cohort. The fixed card is READY/ZERO OWNER/UNASSIGNED; build remains BLOCKED/PENDING.

> **CR271 2026-07-18 21:41 UTC：** C4 communication/activity recovered on fresh comment-only re-delivery. Review #9
> is `P0/P1/P2=0/0/1`; only `closeMapSearchInputAfterRouteClick` stale exclusive-callback JavaDoc remains. Owner C,
> behavior, write set, build blocker, and 39C1 NOT READY state are unchanged.

> **CR271 2026-07-18 21:41 UTC：** TURN-40B-C4 owner C has missed the two-round ACK gate and the ten-minute fresh
> activity gate after Review #8; mark `COMMUNICATION_STALE / ACTIVE_STALE`. The `0/0/3` comment/ACK-only repair,
> source hashes, owner, and input architecture remain unchanged. TURN-39C1 stays NOT READY.

> **CR271 2026-07-18 21:28 UTC：** TURN-40B-C4 functional migration/tests are complete, but parent Review #8 is
> blocked at `P0/P1/P2=0/0/3` for comment/ACK traceability repair only. Owner C remains; no behavior change is
> authorized. Named tests did not execute because shared Cloud missing-type compilation debt blocks the test phase;
> build is BLOCKED/PENDING and TURN-39C1 remains NOT READY.

> **CR271 2026-07-18 21:08 UTC：** C 已 ACK 2031/2041，communication 恢复；2051 首轮待 ACK。stub 已改
> 普通构造并移除 Unsafe subclass allocation。2081 仍仅 capture-failure/no-input，1070 仍仅 invalid-prepared/
> no-input，故 TEST REPAIR REQUIRED / NO DELIVERY 保持；writer active，未跑 Maven。

> **CR271 2026-07-18 20:51 UTC：** C4 测试违反 Repair #5，仍用 `Unsafe.allocateInstance(subclass)` 跳过
> 可调用的 null-super 构造；2081 仅测 capture-failure/no-input，未闭合成功 OCR/click，1070 仍缺 real runtime/
> prepared 证据。现为 TEST REPAIR REQUIRED / COMMUNICATION_STALE / NO DELIVERY；2051 待 ACK，未跑 Maven。

> **CR271 2026-07-18 20:41 UTC：** C4 连续两轮漏 ACK 2031，现 communication-stale 但非 active-stale；
> 2041 要求双 ACK。普通 test subclass 须正常构造，禁止 Unsafe 跳构造/source-only 降级。无 delivery/Maven。

> **CR271 2026-07-18 20:31 UTC：** C4 tests 4/8；剩余五 caller 用普通 test-only coordinate/tracker/OCR/
> memory subclass + real WindowRuntimeContext 闭合，真实 caller/turn observation 不变。拒绝递归 Unsafe 与
> source-only 降级；2031 待 ACK，无 delivery/Maven，39C1 NOT READY。

> **CR271 2026-07-18 20:16 UTC：** C 已具名 ACK C4 test contract 2004，communication normal，scope 阻断
> 解除；production complete、all-eight frozen tests 实施中、无 delivery。未跑 Maven，39C1 仍 NOT READY。

> **CR271 2026-07-18 20:04 UTC：** C4 test 合同已修复：拒绝 production seam 与 scope 收敛；八行均须
> 真实 turn 观察。mini-map visible/retry 用真实 packaged 模板构造 patterned capture 并沿用 production OpenCV；
> compile-debt 阻断入口时仅允许 test-only reflection 调真实方法。C 仍 active，2004 待 ACK，未跑 Maven。

> **CR271 2026-07-19 05:34 EDT：** C 已双 ACK 0416/0432，communication 恢复；C4 仍 6/8
> source-active、无 blocker，并接受 dead 2334 删除、active 696 retry transfer 与 1968 独立测试。A 已 ACK
> 且 39W 3/4；双 writer 正常推进。

> **CR271 2026-07-19 05:32 EDT：** A 已具名 ACK 39W 的 4/4 caller 合同并推进至 3/4，仅剩 prepared
> GREEN caller，无 blocker、无 delivery。C4 保持 6/8 source-active、communication-stale；双 writer 活跃，
> 父级不跑 Maven。

> **CR271 2026-07-19 05:28 EDT：** C4 已迁 6/8，剩 1450/2334；共享 helpers 已解包，文件恢复
> coherent，`pressAlt1ForMiniMap` 的 focused keyboard fallback 已删除。C source-active 非 ACTIVE_STALE，
> 但 0416/0432 未 ACK，COMMUNICATION_STALE 保持。39W 仍须 4/4 caller proofs。

> **CR271 2026-07-19 05:22 EDT：** 39W frozen test contract 必须 4/4 caller-level proofs；2 个代表性
> keyboard/mouse 证明不能替代剩余 tracker/prepared-dialog caller 的真实分支与时序取证。C4 已推进 5/8，
> 剩 1450/1968/2334；C source-active 非 ACTIVE_STALE，但 0416/0432 未 ACK，COMMUNICATION_STALE 保持。

> **CR271 2026-07-19 04:48 EDT：** A 已 ACK 并进入 39W test-source active；C 仍 communication-stale 但
> NavigationService WIP 持续。C4 八行与共享 helpers 必须原子迁移，禁止 turn execute 嵌入旧 exclusive
> callback；无 owner/写集/键鼠并行合同变化。

> **CR271 2026-07-19 04:32 EDT：** C4 owner C 因连续两轮未 ACK 标 COMMUNICATION_STALE，但已有
> NavigationService builders WIP，非 ACTIVE_STALE。39W 必须先完成 frozen test source 才可 SOURCE+TEST
> delivery；Cloud build gate 可因写集外在飞迁移保持 BLOCKED/PENDING，writer 活跃期不再跑 Maven。

> **CR271 2026-07-19 04:16 EDT C4 dead-row transfer：** 删除零调用 legacy
> `closeMiniMapIfOpen@2334`，不复制死算法；其 696 close/recheck/retry 责任转移到唯一活跃
> `closeMiniMapIfOpenTurn`，并在既有 Navigation test 内独立于 1968 取证。写集与 owner 不变。

> **CR271 2026-07-19 03:50 EDT C4 ACK：** External C 已具名接受精确八行合同，包含独立的
> `closeMiniMapIfOpen@2334`；`1968`/`2334` 分别取证，共享 `pressAlt1ForMiniMap` 不得保留前台键盘 fallback。
> C owner/source-active 不变，A 的 TURN-39W 写集仍与其互斥。

> **CR271 2026-07-19 03:47 EDT C4 exact-eight clarification：** Navigation Cloud 物理源码的八行是
> `1070 + 1450/1674/1968/2081/2218/2231/2334`；当前 C census 漏 `closeMiniMapIfOpen@2334`。原卡已要求
> 1968/2334 独立 observe-act-retry 证明，并关闭共享 `pressAlt1ForMiniMap` 的 focused-input fallback。
> 零写集/业务/protocol 扩张；C owner retained，ACK pending；双 writer active，父级不跑 Maven。

> **CR271 2026-07-19 03:15 EDT parallel claims / registry repair：** 原卡 EOF 显示 A 已领取 TURN-39W，
> C 已领取 TURN-40B-C4；Wubei 四 caller+test/Javadoc 与 Navigation 八 caller+两 test 写集互斥，可并行。
> A 已 ACK 39K Review #2；C 03:10 STATUS EVENT 已确认 C4 SOURCE_ACTIVE。第 16 节恢复 88 张 Sprint 主卡，
> 39K/39W/39C1 归并进 TURN-39 主行但固定原卡不变。Java writer 活动期父级不跑 Maven。

> **CR271 2026-07-19 03:05 EDT TURN-39K Review #2：** Repair #2 的 4 production + 2 tests 经父级
> `P0/P1/P2=0/0/0` 终审，owner released。生产 action snapshot 冻结 exact stop/pause token，executor 无
> holder 依赖；per-window generation admission、跨窗 exact-HWND 后台键盘并行与 KEY_UP cleanup 闭合。
> main compile GREEN；named Maven family 因写集外 dirty testCompile 保持 BLOCKED/PENDING。固定原卡
> `TURN-39W`、`TURN-40B-C4` 现为 READY/ZERO OWNER/UNASSIGNED，C2 bag regression 债不变。

> **CR271 2026-07-19 02:25 EDT TURN-39K delivery reconciliation：** 01:18 holder-based 五工件 delivery
> 物理晚于 Repair #2 且未实施新合同；当前 `TurnExecutionWindow` 新字节也未入 manifest，故 delivery 已
> superseded/not reviewable。等待 A 按 4 production+2 tests、exact action stop/pause token freeze 稳定重交付。

> **CR271 2026-07-19 02:20 EDT TURN-39K Repair #2：** `WindowTurnLoop` 的独立 turn thread 不绑定
> `TaskExecutionContextHolder`，故 holder-null fallback 不构成生产 pause/stop 门禁。39K 增加
> `TurnExecutionWindow.java`，在 exact action handle snapshot 内冻结 pause token + 既有 stop token；executor
> 只消费该快照，tests 必须走 production resolve seam。A owner retained，P1/Maven named gate 仍 open/blocked。

> **CR271 2026-07-19 01:55 EDT TURN-39K source review:** `P0=0/P1=1/P2=0 / REPAIR REQUIRED`。
> exact-HWND 后台键盘和跨窗口并行方向保留；direct keyboard 必须在不可逆 post 前补 live stop、pause 与
> exact frozen binding-generation admission，并以 late-stop/pause/value-equal A->B->A 三类 keyboard 零投递
> deterministic tests 闭合。A owner retained；C4/39W 不开放；Maven named gate 仍 BLOCKED/PENDING。

> **CR271 2026-07-19 01:13 EDT TURN-39K test gate 裁定：** test-1 隔离运行 16/16 passed，main compile
> green；标准 Maven named-test 被五个写集外 dirty tests 的 testCompile 错误阻断。隔离结果可作为 source-test
> review evidence，test-2 完成后允许 canonical source delivery；Maven named gate 仍为 `BLOCKED/PENDING`，
> 禁止修改无关测试或宣称 Maven tests passed。A test-2 writer active，父级不运行 Maven；无 runtime/input。

> **CR271 2026-07-19 00:58 EDT TURN-39K test-1 active：** `TurnInputStepExecutorContractTest`
> 已开始 unsupported negatives、background call recording 与 deterministic two-window concurrency barrier 增量；
> 文件仍在活动写入。第二份 test 尚无 39K 新字节，整卡未 delivery。A test writer active，父级不运行
> Maven/JUnit/compile；无 runtime/input。

> **CR271 2026-07-19 00:54 EDT TURN-39K production done / intermediate compile green：** A 报告
> 三 production 完成，DHXY `mvn -q -DskipTests compile` exit 0。两 named test 尚无本卡新字节，当前 test
> writer active；该 compile 不是 delivery/review，父级不并发运行 Maven。A sole owner/communication normal；
> C 已 self-withdraw、双 ACK、idle/no-reclaim。无 runtime/input。

> **CR271 2026-07-19 00:53 EDT TURN-39K 三 production 增量：** `TurnInputStepExecutor=77f184a1...`
> 已将 KEY_TAP/DOWN/UP/TEXT_INPUT 直接路由到 exact-HWND background service；keyboard 不入 mouse queue、
> 不 focus，mouse branch 保持。三 production 现均有增量，两 named test 待续。C 已双 ACK 0034+0045、
> self-withdraw/idle；A sole owner/communication normal。Java writer active，不运行 Maven/JUnit/compile，
> 无 runtime/input/build。

> **CR271 2026-07-19 00:52 EDT TURN-39K 首批 production 增量：** External A 已修改
> `BoundWindowKeyboardService`（`0c29980c...`，exact-HWND Ctrl chord/Enter/ordered WM_CHAR）与
> `TurnKeyMapper`（`57d9a645...`，Ctrl/Enter/modifier 闭合 mapping）。executor 与两份本卡 test 尚无新字节；
> C2 既有 test delta 保留。A sole owner、communication normal；Java writer active，不运行 Maven/JUnit/compile，
> 无 runtime/input/build。C4/39W 仍等待 39K source review pass。

> **CR271 2026-07-19 00:46 EDT TURN-39K 通信竞态纠正：** 父级 00:45 状态块与 A/C ledger
> `STATUS EVENT` 并发落盘；二者均已正式 ACK 0034，stale 判定作废、communication normal，0045 仅
> pending round 1。A 为 sole owner 并已完成 gap recon，尚无 Java/test 新字节；C 已自撤且 idle/no-reclaim。
> `LocalTurnActionExecutorContractTest` 领取前已有 C2 累积 delta，父级 00:34“全写集 clean”说明不准确；
> 必须保留并扩写。无 Maven/runtime/input/build 变化。

> **CR271 2026-07-19 00:45 EDT TURN-39K 领取裁决：** 原卡 physical append order 显示 External A claim
> 先落；External C 并发 claim 后已 canonical self-withdraw，故 39K 唯一 owner=A、C 零 Java/test 写入。
> 3 production+2 test SHA/mtime 尚无变化；键盘 exact-HWND 后台跨窗口并行、仅鼠标前台全局串行的合同不变。
> A/C 连续两轮未在 ledger 追加强制 `STATUS EVENT`，现标 communication/status stale 并已定向修复；
> C4/39W 仍等待 39K source pass，39C1 等三前置 active-zero。无 Maven/runtime/input/build 变化。

> **CR271 2026-07-19 00:34 EDT TURN-39 键盘合同纠正：** 用户确认键盘按窗口 exact-HWND 后台并行，
> 只有鼠标前台且进入全局串行队列。此前 foreground-keyboard user gate 与 keyboard-global-queue 假设作废。
> `TURN-39K` 固定原卡已 `READY / ZERO OWNER`（DHXY 3 production+2 test，无 wire/Cloud 写集）；
> DAG=`39K->{C4,39W}->39C1`。A/C 已收到非分派式 READY 通知，按原卡 EOF anti-race 自主 claim；无 Java/Maven/runtime/input。

> **CR271 2026-07-18 23:15 EDT TURN-39P1 Review #15 PASSED：** Repair #14 终审
> `P0/P1/P2=0/0/0`；双仓 validator/test、`PRESS_CTRL_A` empty-field shape、byte-identical 与双 compile gate
> 闭合；A 23:17 已具名 ACK、report closed、owner released 并转 idle/available，23:37 又 ACK C2 stale 状态纠正并确认 C2 Review #4 passed/owner released/closed/no-reclaim。TURN-39 改为六 facade READ-ONLY umbrella，唯一 DAG
> `[用户 gate]->39K->{C4,39W}->39C1` 冻结。四实施卡均 `NOT READY / ZERO OWNER`，只待用户前台键盘能力决策；
> 无 Java/Maven/runtime/input/build 变化。

> **CR271 2026-07-18 23:05 EDT TURN-39P1 Review #14：** Repair #13 复审为
> `P0/P1/P2=0/1/0 / REPAIR REQUIRED`。source-true 六 facade 与唯一 39K/C4/39W/39C1 DAG 已闭合；39K
> 同时修改双仓 input enum，却漏 Cloud byte-identical exhaustive validator/contract test 和双仓 compile gate，
> Cloud 侧将无法编译且无 `PRESS_CTRL_A` 空字段形状验收。A retained、communication normal；所有实施卡未开放，
> 前台键盘能力仍是唯一待用户语义决策。

> **CR271 2026-07-18 22:49 EDT TURN-39P1 Review #13：** Repair #12 复审为
> `P0/P1/P2=0/2/0 / REPAIR REQUIRED`。Navigation 三路径已唯一归 C4；但 `(K)/(W)/TURN-39C1` 仍无
> section-16 id、固定原卡及无条件 write/test/compile 合同，不能 canonical claim。TURN-39 所称六文件实列
> 七个 stale `turn` 包路径，与 Cloud 当前 live `turn/client`+四 remote facade 不一致。A retained、communication
> normal；所有实施卡未开放，前台键盘能力仍是唯一待用户语义决策。

> **CR271 2026-07-18 22:27 EDT TURN-39P1 Review #12：** Repair #11 复审为
> `P0/P1/P2=0/1/0 / REPAIR REQUIRED`。Wubei whole-task test 漏项已闭合；TURN-39 最终 manifest 与已父级
> 通过的 TURN-40B-C4 双重占有 Cloud `NavigationService.java`、`NavigationTurnContractTest.java`，缺单 owner
> DAG、C4 `FiveRingTaskTrackerTurnContractTest.java` 归并与唯一 compile/test 点。A retained；22:29 keepalive
> 为旧快照竞态，ACK pending round 1。TURN-39/C4 均未开放，前台键盘能力仍是唯一待用户语义决策。

> 2026-07-18 21:15 EDT：TURN-39P1 Repair #6 Review #7=`0/2/0 BLOCKED`。单 queue/worker 与 44A
> 17-SCC 归属已接受；per-INPUT request 会拆开基线 branch-level exclusive 输入，`LocalTurnActionExecutor`
> grouping、唯一 Ctrl+A cleanup 和对应 tests 未冻结。四个 external input 文件 owner/compile closure 仍缺。
> A retained、communication normal；TURN-39 未开放，无 Java/Maven/runtime/input/build 变化。

> 2026-07-18 20:55 EDT：TURN-39P1 Repair #5 Review #6=`0/2/0 BLOCKED`。能力写集未接入现有唯一
> exact-window queue/worker；五个 44A 17-SCC 成员被误归 TURN-44，四个外部 input 文件 owner 未闭合。
> A retained；stale 消息连续两事件未 ACK，communication stale 但 active 正常。TURN-39 未开放。

> 2026-07-18 20:50 EDT：External A 已具名 ACK Review #5 并完成 close-world-map、scroll、Alt+1 fallback、
> binary decision 与 12-path owner 取证，`COMMUNICATION_STALE` 解除，Repair #5 组装中。stale 通知本身
> pending round 1；无 canonical delivery/source/build 变化，TURN-39 仍 BLOCKED/未开放。

> 2026-07-18 20:40 EDT：External A 连续两轮未 ACK TURN-39P1 Review #5 返修消息，标记
> `COMMUNICATION_STALE`；owner A/`0/2/1 REPAIR REQUIRED` 不变。最近事件 20:35，尚未达到
> `ACTIVE_STALE`；无 delivery/source/build 变化，已追加定向双 ACK 消息。

> 2026-07-18 20:35 EDT：TURN-39P1 Repair #4 父级 Review #5=`0/2/1 BLOCKED`。Navigation 全闭包仍漏
> close-world-map helper 与 scroll focus-click/循环/wait，Alt+1 focused fallback 不得作为“固有差异”删除；行2
> old-facade 例外违反零 active-reference 门。retained allowlist 须逐文件修正 owner/disposition；A retained，TURN-39 未开放。

> 2026-07-18 20:25 EDT：A 已 ACK TEXT_INPUT blocker 并完成其余取证；Unicode/Enter/Alt+1 focused
> fallback 归同一前台键盘能力决策，四 remote facade 在 input scope 为 byte-read-only，active-zero/
> 44A-retained 双 allowlist 已枚举。Repair #4 组装中，无 delivery；TURN-39 BLOCKED/未开放。

> 2026-07-18 20:20 EDT：父级确认 TURN-39 活跃 world-map fallback 含 Ctrl+A+Unicode text；TURN-09
> executor 按固定合同返回 `BACKGROUND_KEY_UNSUPPORTED` 且禁止前台 fallback。唯一待用户决策为是否另批
> exact-window、全局串行的前台键盘能力。A 继续其余全闭包报告审计；TURN-39 BLOCKED/未开放。

> 2026-07-18 20:15 EDT：A 已具名 ACK TURN-39P1 Review #4 `0/2/1` 并进入六 helper+传递 callee
> call-graph 取证；communication normal、无 delivery、目标源码 SHA/mtime 无变化。TURN-39 保持
> PLAN-CONTRACT BLOCKED/未开放；无 Maven/runtime/input，build 状态不变。

> 2026-07-18 20:07 EDT：TURN-39P1 Review #4=`0/2/1 BLOCKED`。RETAIN 撤回、12 direct caller、六 facade
> disposition、三个 service guard 与 44A definition 边界已接受；但 Navigation callback 的 helper 全传递闭包、
> 四 remote facade 精确 symbol delta、`OldFacadeRemovalContractTest` 的 active-zero/44A-retained 双 allowlist
> 未冻结。A owner retained，TURN-39 不开放；无 Maven/runtime/input。

> 2026-07-18 19:54 EDT：TURN-39P1 Review #3=`0/2/1 BLOCKED`。12 caller/C2 EOF 已接受；RETAIN 因保留五个
> active old-facade consumer 而违反 typed-only/zero-active-old-facade 固定门，已裁为不合规。仅 RETIRE 可实施，
> 旧定义物理字节留 44A；A 须修正 service guard disposition、active-token gate 与六 facade disposition 后重交。

> 2026-07-18 19:49 EDT：A 已 ACK 39P1 Review #2，owner/report repair active，尚无 re-delivery；
> C 已 ACK C2 Review #4，C2 closed/owner released/no-reclaim，C idle。A/C communication normal，build 状态无变化。

> 2026-07-18 19:38 EDT：TURN-39P1 Review #2=`0/1/2 BLOCKED`。26 项 active/dead 映射已通过，
> 但完整 TURN-39 facade 六文件/44A 边界、必建 `OldFacadeRemovalContractTest` 与 retire/retain 两套精确写集仍未闭合；
> caller 总数应为 12。C2 Review #4=`0/0/0 PASSED`，三项直接 testCompile 错误消失、owner C released；
> named family 仍被卡外历史测试树阻断，DHXY main compile 通过，Cloud 未运行。

> 2026-07-18 19:11 EDT：授权 DHXY named family 在 `testCompile` 失败；C2-owned 三份测试存在旧 `PlayerStateService` 构造与缺失 import 的直接错误。新 build 证据覆盖 Review #2 release，C2=`P0/P1/P2=0/1/0 / REPAIR REQUIRED`、owner C restored；Cloud tests/compile 未运行。39P1 仍独立 blocked。
> C 19:16 误按较早 Review #2 声明关闭，漏读物理更晚 Review #3；该关闭不成立，Review #3 ACK pending round 1。A 已 ACK 39P1 Review #1 并继续 report repair。

> 2026-07-18 19:09 EDT：TURN-40B-C2 Amendment #1 re-delivery 父级 Review #2=`P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。supply 真实 inherited guarded/session 链已证明单 session/精确顺序/三字段，bag-open failure/success 矩阵闭合；三处 protected seam 方法体/签名/生产序不变，owner C released。39P1 独立保持 Review #1 blocked。

> 2026-07-18 19:06 EDT：TURN-39P1 canonical report 父级 Review #1=`P0/P1/P2=0/2/1 / BLOCKED / REPAIR REQUIRED`。现有 turn spec/validator 无法承载旧 double-right-click interval，DHXY executor 拒绝 KEY_DOWN/KEY_UP/TEXT_INPUT；报告的零 protocol/executor 变更、实施写集和 named tests 均不成立，旧 enum 亦误计 27（实为 26）。A owner retained 同卡返修；C2 test writer active，无 Maven/runtime/input。

> 2026-07-18 18:43 EDT：C 已精确双 ACK 1814+1836、communication recovered。C2 三处 BagService
> visibility-only seam 已落盘，SHA-256=`CE0EA995...`；两份测试仍为旧 blob `2ebf4997`/`6916ab39`，
> P1/P2 repair 待写、无 re-delivery。A 已完成 39P1 item-4/6，进入报告组装。writer active，无 Maven/runtime/input。

> 2026-07-18 18:36 EDT：A 已双 ACK 1759+1822、communication recovered，并完成 39P1 item-5 七块体审计。
> C2 原卡已返回 `PLAN-CONTRACT BLOCKED`；父级 Amendment #1 冻结为原写集内 `BagService.java` 三处
> visibility-only seam：`ensureBagOpened`/`closeBagIfNeeded`/`countItemUpToInOpenMainBag` 仅 `private -> protected`，
> 实现体/生产调用序不变、生产树零子类。既有两测试必须执行 inherited guarded/session 真路径闭合 Review #1。
> C 连续两轮未用精确 ID ACK `PARENT-C-TURN40B-C2-REVIEW1-REPAIR-1814`，标 `COMMUNICATION_STALE`；owner retained，
> writer active，不运行 Maven/runtime/input。

> 2026-07-18 18:14 EDT：C2 27-file canonical delivery 父级 Review #1=`P0/P1/P2=0/1/1 / REPAIR REQUIRED`。
> P1=缺 supply 单 guarded session、incense→checkpoint→count 与三字段结果的执行测试；P2=guarded BagService
> test 缺 bag-open failure 及成功 open/close exactly-once。C owner retained；A 的 39P1 report audit 正常推进；无 Maven/runtime/input。
> A 已完成 39P1 item-2 caller census（NavigationService 7× exclusive 残留），但 18:02/18:07 连续漏 ACK 1759，
> 现标 `COMMUNICATION_STALE`；有真实 report 进展，不标 active stale。C 返修 ACK pending round 1。

> 2026-07-18 17:59 EDT：External A 已合法 canonical claim report-only `TURN-39P1`，父级接受其 sole owner；
> 原卡 prior READY/zero owner、唯一 claim、Cloud HEAD 与单文件写集均闭合。C2 两份 whole-task tests 已完成，
> tests 10/11，blob=`7168b543`/`b54396be`，仅余 executor contract test；C writer 活动期间无 Maven/runtime/input。

> 2026-07-18 17:41 EDT：C3 Review #1 同文件返修经父级 Review #2=`P0/P1/P2=0/0/0 PASSED`；
> fake OCR available/unavailable、endpoint 清理和 missing-windowRect fallback 全闭合，production 无漂移，A owner released。
> A 17:44 已具名 ACK Review #2 通过消息并转 `IDLE / AVAILABLE`，当前不持卡且无可领 READY/ZERO-OWNER 原卡。
> C2 `BagServiceGuardedAdmissionTest` 与 Cloud `CloudBagLocalServiceClientContractTest` 已完成，tests 8/11，
> 最新 SHA-256=`A8F88F12...`，余 3 tests；C writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 17:25 EDT：External C 已完成 C2 `BagLocalOperationExecutorContractTest`，累计 6/11，
> SHA-256=`A25E919C...`，余 5 tests。A 的 17:19 keepalive 与 C3 Review #1 消息并发，现为 ACK pending
> round 1；Review #1 `0/1/1`/owner retained 不变。双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 17:17 EDT：C3 canonical delivery 父级 Review #1=`P0/P1/P2=0/1/1 / REPAIR REQUIRED`。
> P1=非空 OCR test 直连默认 loopback sidecar、依赖外部进程且无 available words 正向映射；须同 test 文件
> 使用确定性 fake endpoint/响应覆盖 available+unavailable。P2=补 `windowRect` 缺失 approach fallback。
> A owner retained；C2 仍 tests 5/11。未运行 Maven/runtime/input。

> 2026-07-18 17:12 EDT：External C 已完成 C2 `LocalServiceStepDispatcherContractTest` 重写，累计 5/11，
> SHA-256=`0F3661F8...`，余 6 tests。C3 whole-task test 已出现真实写入，SHA-256=`99FFC4B8...`，
> 但尚无 File 3/3 完成事件或 delivery，只记 active bytes。双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 17:04 EDT：External C 已完成 C2 双仓 mirrored action + envelope golden tests，累计 4/11；
> LF-normalized SHA-256=`61D842FB...`/`15D2691F...`，四个协议测试跨仓内容一致，余 7 tests。
> External A 已完成 C3 `XiuluoTaskV2` production 重写、进度 File 2/3；父级实测文件仍在继续写入，当前
> SHA-256=`6B90ECD1...`，whole-task test 待完成。两卡无 delivery；双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:58 EDT：External C 已完成 C2 双仓 mirrored core golden + validator tests，进度 2/11，
> SHA-256=`3737B04C...`/`2407D499...`。同一共享测试补齐 C1 遗漏的 METRIC×3 closed-enum
> 字面量，作为 C2 delivery 披露项，不回退 C1 source gate。C2/C3 均无 delivery；未运行 Maven/runtime/input。

> 2026-07-18 16:54 EDT：External A 已具名 ACK C3 Amendment #2 并确认 #1 superseded；File 1/3
> `ObjectiveTextRecognizer` 按 public plausibility + defensive-copy transform 落盘，SHA-256=`1D97D996...`。
> External C 已完成 Wubei rewire，C2 production 13/13 全落，当前进入 11 文件测试；两卡均未 delivery，
> 双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:42 EDT：C3 Q1 addendum 证明 approach 也依赖 `mapTransform`。父级 Amendment #2
> supersedes 未 ACK 的 #1：`coordinatePlausible` 改 `public static`；`mapTransform` 改 `public static` 且必须
> 返回 defensive copy，禁止泄露内部可变 snapshot、第二 loader 或 resolver wrapper；精确写集仍 3 文件。
> C2 同轮 FiveRing rewire 完成，production 12/13；双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:32 EDT：父级完成 C3 plausibility 全符号/调用审计，确认无其它 public 可达路径；
> Amendment #1 批准 `ObjectiveTextRecognizer.coordinatePlausible` 仅由 package-private 改 `public static`，
> 精确写集扩为 3 文件，算法/margin-80/单 maps.json owner 零变化，待 A 具名 ACK。C2 同轮 11/13；
> 双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:23 EDT：External A 已从 `696a12b0` 精确提取 C3 randomize `(1,1)` 对称偏移、
> approach offset=2/龙窟凤巢短路与 margin-80 plausibility 语义；C3 目标源码/测试未变化，无 delivery。
> A/C communication normal，C2 同轮 10/13；双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:22 EDT：External C 已落盘 C2 Cloud `CloudBagLocalServiceClient` 三个 strict typed 方法，
> SHA-256=`2F361F49...`，production 进度升至 10/13；剩余 Cloud 3 个 production 文件与 11 个测试，
> 尚无 canonical delivery。A 的 C3 仍处 recon 且两目标文件未变化；双 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:16 EDT：External A 已具名双 ACK 1600+1602，确认旧 TURN-38B3 用户授权问题废弃，
> communication normal；C3 仍由 A sole-owned/source-active recon，尚无 delivery。External C 已完成 C2
> DHXY 侧 6 个 production 文件，总进度 9/13 production；尚无 canonical delivery。C2/C3 写集不交，
> 两名 writer 活动期间未运行 Maven/runtime/input。

> 2026-07-18 16:02 EDT：External A 已 canonical claim 正确的 TURN-40B-C3，父级接受其 sole owner/source active；
> 16:00 READY 快照与 claim 并发已纠正。A communication stale 待 STATUS ACK 1600+1602，当前未满 10 分钟且
> 零源码变化，不标 active stale。A(C3) 与 C(C2 5/13 WIP) 写集不交并行；未运行 Maven/runtime/input。

> 2026-07-18 16:00 EDT：用户截图证明 A 仍误把已通过/owner released 的 TURN-38B3 当 READY 并索权；
> 父级已定向要求废弃 prompt。当前 READY 是独立 TURN-40B-C3，仍零 owner/未分派。C2 已落 5/13 production
> WIP，C sole owner/source active；尚无 delivery，active writer 期间未运行 Maven/runtime/input。

> 2026-07-18 15:49 EDT：External C 已双具名 ACK 1534+1542，communication normal；C2 双仓
> `TurnLocalOperation`/`TurnBagOperationArguments` 已同 SHA 真实推进，sole owner/source active。尚无 delivery，
> C3 仍 READY/零 owner；active writer 期间未运行 Maven/runtime/input。

> 2026-07-18 15:44 EDT：External C 15:43 已用 STATUS EVENT 合规 ACK 1534，COMMUNICATION_STALE 解除；
> C2 维持 sole owner/source active，C3 仍 READY/零 owner。并发后 1542 为 ACK pending round 1；尚无 delivery，
> active writer 期间未运行 Maven/runtime/input。

> 2026-07-18 15:42 EDT：External C 15:40 canonical claim C2，经父级核验 accepted 为 sole owner/source active；
> C3 仍 READY/零 owner。claim 已引用 1534，但无 STATUS EVENT 合规 ACK，连续两轮后标 COMMUNICATION_STALE；
> 因真实 claim/source 活动不标 ACTIVE_STALE、不撤 owner。active writer 期间未运行 Maven/runtime/input。

> 2026-07-18 15:34 EDT：TURN-40B-C1 repair re-delivery 经父级 Review #2=`P0/P1/P2=0/0/0 PASSED`，
> phase/status closure、zero-record fail-closed 与 retained negatives 闭合，owner 释放。C1→C2 串行 gate
> 满足，C2 固定原整卡与 C3 均公开 READY/零 owner/未分派；C4 等 39P1。聚合 build 延后，未运行 Maven。

> 2026-07-18 15:18 EDT：External C 15:17 已具名 ACK C1 Review #1 返修消息，状态为 `OWNER C /
> SOURCE_ACTIVE / REPAIRING`；尚无 re-delivery。C2 继续等待 source review passed，C3 仍 READY/零 owner，
> A 仍 COMMUNICATION_STALE。active repair writer 期间父级不运行 Maven/runtime/input。

> 2026-07-18 15:13 EDT：TURN-40B-C1 15:01 canonical delivery 父级 Review #1=`P0/P1/P2=0/1/0
> BLOCKED`。双仓 shared validator 未关闭 STARTED/FINISHED 的 failure-only `phase`，unknown FINISHED
> status 又会由 DHXY executor 静默降为 `INFO`；须在 validator 边界闭合 exact shape/status 并补 mirrored
> negatives 后同卡 re-delivery。C2 继续等待；C3 仍 READY/零 owner；A 仍 COMMUNICATION_STALE。未运行 Maven。

> 2026-07-18 13:52 EDT：C1 双仓 `TurnLocalOperation` 已有同 SHA 真实变化，Cloud 新增
> `TurnMetricEventPayload`，External C sole owner/source active；active writer 期间父级不跑 Maven。
> External A 对 13:38 具名消息连续两轮无 ACK，标 `COMMUNICATION_STALE` 并于 ledger EOF 定向重发。
> C3 仍 READY/零 owner/未分派，不因 stale 被关闭、派发或预留。

> 2026-07-18 13:47 EDT：External C 于 C1 原卡 13:42 canonical claim，父级核验 prior READY EOF、零 earlier
> claim、13:45 具名 ACK 与回读证据后接受 C sole owner，C1 metrics wire/persistence seam=`SOURCE_ACTIVE`。
> C3 Xiuluo coordinate/OCR 仍 `READY / ZERO OWNER / UNASSIGNED` 且与 C1 写集不相交；A ACK pending 第一轮。
> C active Java writer，父级不运行 Maven/runtime/input。

> 2026-07-18 13:36 EDT：TURN-40BP1 13:22 re-delivery #6 父级 Review #7=`P0/P1/P2=0/0/0 PASSED`，
> report owner released。rejection flag→queue 后转抛→Bag adapter 唯一 typed STOPPED mapping 与四 outcome
> retained contract 已闭合。已开放 `TURN-40B-C1` metrics wire/seam 与 `TURN-40B-C3` Xiuluo coordinate/OCR
> Cloud-form 两张互不碰撞实施整卡为 READY/零 owner，供两名 Worker 自行 canonical claim 并行，非派卡。
> C2 等 C1 source review passed；C4 等 TURN-39P1 parent report review。无 Java/Maven/runtime 变化。

> 2026-07-18 13:13 EDT：TURN-40BP1 13:04 re-delivery #5 父级 Review #6=`P0/P1/P2=0/1/0 BLOCKED`。
> live predicate 位置与 STOPPED iff 已通过；guarded generic `T` 未冻结 admission/token 拒绝如何穿过会吞异常并
> 只返回 false 的 legacy queue boundary。须冻结 callback flag/local outcome→adapter typed STOPPED，且普通
> queue/open failure 保持非 STOPPED；四 outcome retained test。待 C ACK；Java 卡不得开放，39P1 READY/零 owner。

> 2026-07-18 12:53 EDT：TURN-40BP1 12:44 re-delivery #4 父级 Review #5=`P0/P1/P2=0/2/0 BLOCKED`。
> 提前计算的 task-current boolean 在 queue submit/callback 间可陈旧，须在 Bag exclusive callback 内且
> `ensureBagOpened` 前 live 验 exact captured identity；`stopRequested` 也须与 `code=STOPPED` 建立双向 iff，
> 禁止 arbitrary code 洗成 STOPPED 或 generic failure 冒用 STOPPED。待 C ACK；Java 卡不得开放，39P1 仍
> READY/零 owner。

> 2026-07-18 12:33 EDT：TURN-40BP1 12:19 canonical re-delivery #3 父级 Review #4=`P0/P1/P2=0/2/0 BLOCKED`。
> 每次重读 current task 会把旧 action 错绑后继 stop token，须冻结 exact action-owning handle/token 并验证
> handle/run identity；现有 `LocalServiceExecution`/step status 无可表示 STOPPED，须冻结 local stop discriminator
> 并显式映射 `TurnStepExecution.stopped`。metrics 真身 retained test 已闭合旧 P2。C 保持 report owner；Java 卡
> 不得开放，39P1 仍 READY/零 owner。

> 2026-07-18 12:10 EDT：C 已具名 ACK TURN-40BP1 Review #3 `0/1/1`，当前同一报告 repair active。
> 返修范围=production live stop-token/context bridge、typed STOPPED mapping 与 `recordWireEvent` 真身 retained
> persistence/dashboard test。ACK 不关闭 finding，Java 卡不得开放；39P1 仍 READY/零 owner。

> 2026-07-18 12:04 EDT：TURN-40BP1 11:49 canonical re-delivery #2 父级 Review #3=`P0/P1/P2=0/1/1 BLOCKED`。
> specialized Bag op 尚无 production live `TaskExecutionContext/TaskStopToken` bridge，13 项矩阵也未验证
> `recordWireEvent` 真身 persistence/dashboard seam。C 保持 report owner，具名 ACK 后返修；Java 卡不得开放，
> 39P1 仍 READY/零 owner。

> 2026-07-18 11:39 EDT：C 已具名 ACK TURN-40BP1 Review #2 `0/2/2`，当前同一报告 repair active。
> 返修范围=metrics 完整 identity/full `caseDir` 与 local authority wire、dispatcher 分流避免 Bag queue-in-queue、
> 精确 production/constructor/test 路径和 retained gate。ACK 不关闭 finding，Java 卡不得开放；39P1 仍 READY/零 owner。

> 2026-07-18 11:32 EDT：TURN-40BP1 11:19 canonical 返修重交父级 Review #2=`P0/P1/P2=0/2/2 BLOCKED`。
> metrics route-B 缺 task/window identity、full `caseDir`、mirrored `TurnLocalServiceCall` 与 dispatcher；FiveRing
> supply op 沿现 Bag dispatcher 会 queue-in-queue。C1/C2 写集/test gate 仍漏 dispatcher/constructor/exact test，
> C 保持 report owner 返修，Java 卡不得开放；TURN-39P1 仍 READY/零 owner。

> 2026-07-18 09:53 EDT：C 已具名 ACK TURN-38C Review #1，卡 closed、owner released，C 转 available。
> 2026-07-18 11:04 EDT：C 已具名 ACK TURN-40BP1 Review #1 全部 `0/2/2` finding，当前仅同一报告
> repair active。返修方向=真实 metrics 持久化权威、专用 FiveRing 单会话 supply-check typed op/result、
> C1-C4 精确路径与完整 mirrored protocol/test gate；ACK 不关闭 finding，Java 卡不得开放。
>
> 2026-07-18 10:55 EDT：TURN-40BP1 报告父级 Review #1=`P0/P1/P2=0/2/2 BLOCKED`。纯内存 metrics
> 丢失既有持久化合同；FiveRing `withMainBagOpen` 的单会话补香+计鞋闭包、复合结果、stop/input 原子性未被
> C2 的闭集 op 表达；C2/C3 精确写集与协议测试门未冻结。C 保持 report owner 返修，Java 卡不得开放；
> TURN-39P1 仍 READY/零 owner。
>
> 2026-07-18 10:11 EDT：External C 已 canonical claim TURN-40BP1，父级核验冻结 SHA/HEAD/零 prior claim
> 后接受 sole owner，当前只写 shared compile closure 固定报告。TURN-39P1 继续 READY/零 owner/未分派，
> 两报告路径无碰撞；无 Java/Maven/runtime 状态变化。

> 2026-07-18 10:03 EDT CR271 plan-contract repair：最新 source ref 证明 38A-C caller-zero 尚未成立，
> `CloudTaskRunAuthorityAssembly` 仍有两个 legacy context 构造点且 old SCC 保留到 TURN-44A。已并行开放
> TURN-39P1（InputSequences/Navigation exclusive callback/39-44A 边界）与 TURN-40BP1
>（metrics/coordinate/OCR 全传递 compile closure）两张 report-only 固定整卡，均 READY/零 owner/未分派；
> 两卡只写各自报告、零碰撞，A/C 可同时 canonical claim。Java/runtime gate 未改变。

> 四文件 reviewed SHA/mtime 无漂移；build 继续归 TURN-40B shared Cloud main compile debt。当前无新 READY 卡，
> TURN-39 仍待 active refs/InputSequences owner/metadata authority/test ownership 合同闭合。

> 2026-07-18 09:43 EDT：TURN-38C 四文件 canonical delivery 已由父级 Review #1 `P0/P1/P2=0/0/0`
> 通过并释放 C owner。turn-native context-local single bit、legacy delegate、8T/9T/11T 合同闭合，
> `LeftTopStatusSwitchService`/callers 零字节且无业务差异。授权 named family 在 JUnit 前被 TURN-40B shared
> Cloud main compile debt 阻断，错误未指向 38C 写集；source gate 不回退。

> 2026-07-18 09:16 EDT：External C 已在 TURN-38C 原卡完成唯一 canonical claim 并 ACK
> `PARENT-TURN38C-READY-0905`；父级 claim audit accepted，当前 `SOURCE_ACTIVE / C SOLE OWNER`。固定
> `TaskExecutionContext` + 3 tests，五个 old target 全只读；active writer 期间不跑 Maven/runtime。

> 2026-07-18 09:05 EDT：TURN-38M 父级分类已冻结完成，五个 old-authority 文件全部 `DELETE` 并保持字节到
> TURN-44A。`GameContext.State` replacement 归 TURN-40B concrete runtime；LeftTop pending replacement 归
> TURN-38C turn-native `TaskExecutionContext` context-local single bit。TURN-38C 固定 1 production + 3 tests，
> 已开放 `READY / ZERO OWNER`，A/C 可自行 canonical claim；无派卡、无 session/ledger/TTL/第二 store、无业务差异。

> 2026-07-18 08:46 EDT：C 已具名 ACK `PARENT-TURN38B4-REVIEW3-PASSED-0840` 并转
> `AVAILABLE / IDLE POOL-SCAN`。B4 保持 `SOURCE+TEST SOURCE REVIEW PASSED / OWNER RELEASED`；shared Cloud
> build blocker 仍归 TURN-40B，无 B4 源码返修或业务差异。

> 2026-07-18 08:40 EDT：TURN-38B4 四文件 canonical re-delivery 已完成父级 Review #3，结论
> `P0/P1/P2=0/0/0`。sealed store-private id、atomic move/no-replace failure + target preservation、close-only
> restart discovery 与 15T 闭合；`24+8` token、16-field exact identity、双 cleanup、governor chain、byte-exact
> 与 sibling/other-scope preservation 无漂移，C owner 释放。named test 仍在 test-compile 前被 TURN-40B shared
> Cloud main compile debt 阻断，错误未指向 B4 文件，不回退 source gate。

> 2026-07-18 08:15 EDT：C 已 ACK B4 Review #2，状态=`SOURCE_ACTIVE / REVIEW #2 REPAIRING /
> EXTERNAL-C OWNER`。sealed id/store-private issuance、窄 move/no-replace seam、close-only existing-scope
> discovery 按同四文件实施，报无需第五文件；尚无新字节，不跑 Maven/runtime。

> 2026-07-18 08:05 EDT：B4 07:56 canonical re-delivery Review #2=`P0/P1/P2=0/1/2`。同包
> `ArtifactId.issue` 仍可铸；ACL 仅测 CREATE_NEW，未测 move/no-replace+预存 target；restart 先 write，未证
> recreated-but-unused close 清旧文件。同四文件窄 test seam/close-only discovery 已开放，C owner 保持。

> 2026-07-18 07:49 EDT：B4 Review #1 四项返修字节已完成，named test=`E33E84D5...`/580L/13T，
> 当前 `REDELIVERY PREP / EXTERNAL-C OWNER`。原卡尚无新 canonical whole-card delivery，父级未复审，
> `P0/P1/P2=0/1/3` 继续有效；C active writer 期间不跑 Maven/runtime。

> 2026-07-18 07:42 EDT：C 已具名 ACK B4 Review #1，状态=`SOURCE_ACTIVE / REVIEW #1 REPAIRING /
> EXTERNAL-C OWNER`。non-mintable id 与 lazy/reconcile close 的 production 返修已落盘；atomic failure、完整
> identity tuple、unused/restart close 的 named-test 证据仍待闭合。固定四文件、`24+8`、双 cleanup 不变。

> 2026-07-18 07:31 EDT：B4 父级 Source+Test Review #1=`P0/P1/P2=0/1/3`，整卡返修。公开可铸
> `ArtifactId` 不能拒绝 forged metadata；atomic-failure、完整 identity tuple、unused/restart close 验收未闭合。
> C 保持 owner；需要第五路径则先 `PLAN-CONTRACT BLOCKED`。named test 被既有 Cloud main compile debt 前置阻断。

> 2026-07-18 07:22 EDT：B4 四文件 delivery 已形成但被并发父级 ACK 块占用后续 physical EOF；按原卡 EOF
> 唯一权威，状态暂为 `SOURCE+TEST DELIVERY REASSERT REQUIRED / EXTERNAL-C OWNER`。C 仅需在 EOF 原样重申，
> canonical 后父级立即 source review；不要求源码返工。

> 2026-07-18 06:56 EDT：C 已具名 ACK `PARENT-TURN38B4-AMENDMENT1-0652`，撤回 `16+16` 并接受
> `24+8` token、exact-task sibling preservation 与 quiescent host-close whole-scope cleanup。B4 hold
> 解除，当前 `SOURCE_ACTIVE / EXTERNAL-C OWNER / AMENDMENT #1 ACKED`；固定四文件内继续实施。

> 2026-07-18 06:52 EDT：B4 Amendment #1 在零 Java 字节时修复 token/cleanup 歧义。token 固定
> `af1-<24 hex exact-context digest><8 hex nonce>`；exact-task cleanup 保留同 scope sibling，host close 仅在
> 40C whole-scope quiescence 后清本 scope、保留其它 tenant/user scope。C owner 保持，ACK 前 hold。

> 2026-07-18 06:46 EDT：External C 于 B4 原卡 06:43 canonical whole-card claim，06:45 具名 ACK；
> 状态=`SOURCE_ACTIVE / EXTERNAL-C OWNER`。三 production 领取点 SHA 与 freeze 一致、test absent，固定四文件
> 以外只读；real caller/activation 仍归 40B/40C。A 仍 `COMMUNICATION_STALE`，active writer 期间不跑 Maven。

> 2026-07-18 06:38 EDT：TURN-38B4 完整传递审计完成并开放 `READY / ZERO OWNER`。固定三 production
> + one named test；turn-native exact-context、raw `CloudTurnFrame` byte/metadata、atomic no-replace、
> context-bound opaque id、task/host cleanup capability 已冻结。governor 只作 capacity accounting 且不改写；
> 40B 负责真实 terminal caller wiring。C 已实质 ACK B3 review 并 idle，A 仍 `COMMUNICATION_STALE`。

> 2026-07-18 06:27 EDT：TURN-38B3 三文件 parent source+test review=`P0/P1/P2=0/0/0`。
> turn-native 16 元 exact context、显式双 factory、原角色矩阵、STOP/PAUSE、零 action/UUID 与 11T 源码闭合，
> 无业务差异，C owner 释放。授权 named test 在 test-compile 前被 TURN-40B shared main compile debt 阻断；
> 错误未指向 B3 文件。A 仍 `COMMUNICATION_STALE`。

> 2026-07-18 06:02 EDT：C 已 ACK 并收官 TURN-38B2，随后于 05:57 唯一 canonical claim
> TURN-38B3。B3 现为 `SOURCE_ACTIVE / EXTERNAL-C OWNER`；领取点两 production SHA 与 freeze 一致，
> named test 仍 ABSENT，固定三文件写集未漂移。A 继续 `COMMUNICATION_STALE`，C 写作期间不跑 Maven/runtime。

> 2026-07-18 05:47 EDT：TURN-38B2 五文件 Review #2=`P0/P1/P2=0/0/0`，External C 通信恢复并
> 释放 owner。per-host Spring owner、shutdown close 与 retained TURN-14 exact owner harness 均闭合；无业务
> 差异。named test 仍被 TURN-40B shared Cloud main compile debt 阻断。A 仍 stale，B3 READY/零 owner。

> 2026-07-18 05:34 EDT：External C 对 B2 05:24 五文件返修消息连续两轮未 ACK，标
> `COMMUNICATION_STALE`；B2 owner、两个 P1 与五文件修复合同不变。External A 仍 stale，B3 仍
> `READY / ZERO OWNER`。无新源码漂移或 build 变化。

> 2026-07-18 05:24 EDT：TURN-38B2 父级 review=`P0/P1/P2=0/2/0`。owner 缺 per-scope Spring
> bean/lifecycle 接线，且 retained TURN-14 test 仍调用旧单参 service constructor；B2 固定写集扩为三个
> production、named test、retained TURN-14 test 共五文件。named test 当前先被 TURN-40B shared Cloud main
> compile debt 阻断。B3 继续 READY/零 owner，与 B2 修复写集零碰撞；External A 标 `COMMUNICATION_STALE`。

> 2026-07-18 05:08 EDT：A 已 ACK TURN-38B3 READY，但因自身旧 heartbeat prompt 仍含 38 族禁令而
> HOLD。用户直接要求 A/C 并行的原话已固化进原卡；B3 仍 `READY / ZERO OWNER` 且与 B2 零碰撞。
> 若下一拍仍拒领，唯一阻点为 A automation instruction 更新，不是迁移 DAG 或 source contract。

> 2026-07-18 05:00 EDT：用户明确要求 A/C 并行，External A 的 38 族禁令解除。TURN-38B3 已冻结为
> 真实 `task/startup` 两 production + one test 并开放 `READY / ZERO OWNER`，与 C active B2 零文件碰撞。
> exact turn-native context、显式 policy factory、UNKNOWN/STOP/PAUSE 冻结；role/team wire/activation 留 40B/40D。

> 2026-07-18 04:50 EDT：TURN-38B2 原卡 physical EOF 确认 External C 于 04:47 canonical whole-card
> claim，状态=`SOURCE_ACTIVE / EXTERNAL-C OWNER`；领取点三 production protected SHA 未漂移，test 仍
> ABSENT。A 受 38 族 lane 禁令不能竞争，B/D 任务已删除，故当前 38 族实际并行度为 1；C 写作期间不跑 Maven/runtime。

> 2026-07-18 04:36 EDT：TURN-38B2 parent contract freeze 后开放 `READY / ZERO OWNER`。旧 owner/workflow
> 是 disconnected dormant core；固定写集扩为 live `ReturnItemPrescanService` + owner + workflow + one test，
> 删除 service 第二 map。保持 696/TURN-14 public API、随机时序、fallback/terminal、five-field screen-absolute
> cache 与一 UUID/command；不接 dormant capacity/permit/TTL/retry。

> 2026-07-18 04:28 EDT：TURN-38B1 Review #2=`P0/P1/P2=0/0/0`。constructor owner-only、native
> drift 原子 exact +1 与 tenant/user/generation 14T 矩阵闭合；通信恢复，C owner 释放。授权 named test
> 在执行前被写集外 Cloud main compile 共享缺类阻断，归 TURN-40B，不回退 B1 source gate。

> 2026-07-17 23:46 EDT：TURN-37 canonical zero-byte re-delivery 保持 production/test
> `2d4bc1a0...`/`d809700a...`；父级 Review #2=`P0/P1/P2=0/0/0`。17 phase、双路、失败表、
> 消息/次数/顺序与 `696a12b0` 逐方法等价，layered gate 闭合，C owner 释放。S1-S3 compile/assembly
> 仍归 40B，fresh runtime 归 41；本轮未运行 Maven/runtime。

> 2026-07-17 23:34 EDT：TURN-37 Amendment #4 采用与 35/36 一致的 layered source-test gate；撤销
> 不可构造的 26-collaborator public full-loop harness，现有 component/专项测试 + 父级 696 逐方法源审闭合。
> S1 `AutomationMetricsService`（三 Task 7 caller）、S2 `CoordinateHelper`（Navigation×9+Xiuluo×3）、
> S3 `TextRecognizer`（Xiuluo OCR）完整候选写集登记 40B；合同未冻结前 40B NOT READY。

> 2026-07-17 23:17 EDT：C 已具名 ACK TURN-37 whole-card test P1，进入 public execute path harness
> recon；唯一 test `d809700a...` 尚无返修字节，production `2d4bc1a0...` 冻结，owner 保持，不跑 Maven。

> 2026-07-17 23:12 EDT：TURN-37 whole-card delivery Parent Review #1=`P0/P1/P2=0/1/0 BLOCKED`。
> Xiuluo production `2d4bc1a0...` 4/4 与 GAP#2/#3/#4 冻结；唯一 test `d809700a...`/522L/7T
> scope-out public execute/phase loop，缺 `BC4+BASE+TASK+IMG+LS`、shortcut/non-shortcut、四 terminal、
> raw PNG/closed service/UUID-command 和失败处理矩阵。C owner 保持，只返修 test；当前不跑 Maven。

> 2026-07-17 18:01 EDT：TURN-35 Review #1 返修已有真实 source bytes：Cloud client=`1cd35eae`，双仓
> result DTO=`1b9ae100` byte-identical；tests/ACK/re-delivery 尚未到，caller gate 继续关闭，不审 WIP。

> 2026-07-17 17:45 EDT：TURN-36 Review #4=`0/0/0 SOURCE+TEST SOURCE REVIEW PASSED`。null-context
> catch/finally、18T caller/consumer 与 696 diff 闭合，C owner 释放；startup integration 仍归 38B3/40B，
> named test/Cloud compile 仍待稳定 writer build gate。

> 2026-07-17 17:45 EDT：TURN-35 Amendment #12 foundation delivery Review #1=`0/2/1 REPAIR REQUIRED`。
> 新 dialog fact 未纳入旧 result kinds 的反向 exactly-one closure；preparation matrix 缺 REQUESTED/PREPARING/
> 非 blocking/absent；result DTO JavaDoc 过期。A owner 保持、caller gate 关闭，返修重交前不接 4 caller。

> 2026-07-17 17:45 EDT：TURN-36 blocker 已 canonical 落卡，17:47 EOF-missing 旧结论作废。Parent Amendment #12
> 撤销本卡 BASE 全环大 harness 与真实 startup authority battery：现有 18T caller/consumer battery + 696 diff
> 作为 FiveRing source 验收；startup dual-path/context/construction/integration 归 TURN-38B3/40B。C 可 ACK 后重交；
> 38B3 仍受 metadata authority、construction seam 与 40B assembly 阻断，未开放。

> 2026-07-17 17:47 EDT：C 已 ACK TURN-36 Review #3，null-context/P2 WIP 已落；但其 BASE/startup
> `PLAN_CONTRACT_BLOCKED` 尚未出现在原卡 physical EOF，父级要求先 canonical 补齐完整缺口，暂不修合同。
> A Amendment #12 foundation source complete、tests active；两 writer 活动，本轮不跑 Maven。
>

> 2026-07-17 17:40 EDT：TURN-36 whole-card delivery 父级 Review #3=`P0/P1/P2=0/2/1 REPAIR REQUIRED`。
> `execute(null)` 在 try/finally 外抛，与两项 FAILED+forceRelease test 矛盾；BASE phase-loop/startup-check 仍缺，
> test 类 shared-foundation blocker 说明已失真。External C owner 保持，等待 ACK 后整卡返修；A active，不跑 Maven。
>

> 2026-07-17 17:31 EDT：A 已 ACK Amendment #12，Wubei route 去旧=`675b8405`/4442L，双仓 dialog fact
> DTO=`4704b65d`/22L，foundation WIP。C 已补 A4 caller battery，prod=`7d493c5d`/3023L、test=
> `6b64ce32`/935L/17T；BASE/startup 原卡门仍保留，未闭合前不得 whole-card delivery。
>

> 2026-07-17 17:23 EDT：TURN-35 Amendment #12 冻结。残留纠正为 5 caller：3 visible-dialog、1 prep-status
> 统一走一个 closed typed LOCAL_SERVICE read；不扩 metadata/mirror/store。旧 route-result consume 由当前 DHXY
> `PendingRouteOutcome`/Runner 在 clear 后唯一上报 `ABANDONED`，Wubei 删除旧二次记录。A foundation active。
> TURN-36 prod=`9ff98487`/3013L、test=`467a3f19`/872L/16T，battery 继续，非 delivery。
>

> 2026-07-17 17:07 EDT：TURN-36 batch8 production migration complete，FiveRing SHA-256=`8406450c`/3003L。
> runExclusive 已切 exact local accept op并删除 Cloud duplicate callback；non-executed fatal 映射获父级确认，
> 仍待 BC4/A4/A1-positive/BASE/startup battery 与一次 whole-card delivery。
>

> 2026-07-17 17:02 EDT：TURN-36 通信恢复，batch7 SHA-256=`bdf2dee6`/3045L（Git blob=`ec7d3941`）。
> FiveRing 5 处 unconditional clear 与 direct runtime block 已迁；runExclusive exact enum 映射、cleanup/battery
> 尚待，当前非 whole-card delivery。TURN-35 Wubei 同步推进至 batch6=`ca581731`/4450L。
>

> 2026-07-17 16:47 EDT：TURN-35 Amendment #11 Repair #1 source+test-source Review #2=`0/0/0 PASSED`；
> 13 个 unconditional clear caller gate 开放。TURN-36 runExclusive 按 Amendment #6 冻结实现闭合：DHXY local op
> 独占执行原 accept body并返回三值 enum，Cloud 只映射，不复制 callback/点击/日限算法。
>

> 2026-07-17 16:36 EDT：TURN-35 Amendment #11 source+test-source Review #1=`0/1/0 REPAIR REQUIRED`。
> 双仓 `TurnProtocolValidator.requireLocalService` 顶层 whole-task case 漏 `WHOLE_TASK_PATHING_CLEAR`；13 caller
> gate 保持关闭。A 修 validator/contract test 后 re-delivery；C 仅 FiveRing A3×5 partial block，其余继续。
>

> 2026-07-17 15:56 EDT：TURN-35 Amendment #10 采用 option (a)：wrong-window/HWND fail-closed 归既有
> `TurnExecutionWindow.resolveForAction` upstream owner；LOCAL_SERVICE executor 不新增第二绑定协议。
> `FDC1B555`/11T 闭合 P1-2 本层范围，但仍待 canonical foundation Repair #1 re-delivery/父级复审，gate 关闭。
>
> 2026-07-17 15:54 EDT：TURN-35 P1-2 test 已实盘推进至 `FDC1B555`/11T，覆盖 bound register、clear
> mismatch/match cleanup 与 prefix fence；A source active 未 stale。wrong-window/HWND 证据尚待，非 delivery，
> foundation/caller gate 继续关闭。
>
> 2026-07-17 15:49 EDT：TURN-35 Review #1 P1-1 已闭合：双仓 payload internal exactly-one 与 Cloud
> operation-specific result shape 证据=`AE41CA9F`/`67677F59`/`687188E1`/`8285C206`。P1-2 DHXY
> bound-runtime exact-binding/clear-side-effect fixture 仍待，foundation 非 delivery，TURN-36 继续阻断。
>
> 2026-07-17 15:44 EDT：External A 已 ACK TURN-35 Foundation Review #1 `0/2/0`，恢复
> `REPAIR_ACTIVE / COMMUNICATION NORMAL`。双仓 validator/test `AE41CA9F`/`67677F59` 已完成 internal
> exactly-one 首批返修；Cloud result-shape 与 DHXY bound-runtime tests 尚未闭合，caller gate 继续关闭。
>
> 2026-07-17 15:40 EDT：External C 已具名 ACK TURN-36 partial-supersession/foundation-block 两条父级消息，
> 通信恢复正常；卡保持 `REPAIR_ACTIVE / FOUNDATION BLOCKED`，三份 SHA 无漂移。TURN-35 仍为 Review #1
> `0/2/0 / REPAIR REQUIRED / A ACK PENDING`，caller gate 关闭。
>
> 2026-07-17 15:34 EDT：TURN-35 Amendment #6/#7 foundation 父级 Review #1=`P0/P1/P2=0/2/0 / REPAIR REQUIRED`。
> 17 operation payload 尚未执行 internal exactly-one，Cloud typed result shape 未按 operation 封闭，DHXY
> exact-binding/clear-side-effect 固定测试缺失；35/36/37 caller gate 继续关闭。TURN-36 partial delivery 已校正为
> `REPAIR_ACTIVE / FOUNDATION BLOCKED`，不启动 Review #3，现有 IMG/A1-negative 字节保护。
>
> 2026-07-17 15:26 EDT：External C 已 ACK 三条旧消息并恢复 TURN-36 `REPAIR_ACTIVE`；tryEnter adapter、
> 正确 physical evidence 与新 source 字节已落。A4/A1-positive 当前冻结依赖经 TURN-35 typed `LOCAL_SERVICE`
> foundation 唯一闭合，禁止把 `WindowRuntimeContext`/`GameStateUtil` 复制进 Cloud。C 先完成 IMG consumer 与
> A1-negative，foundation 通过后续接全部 caller；不得部分 canonical delivery。

> 2026-07-17 15:17 EDT：External C 已重注册 TURN-36 heartbeat `778801ea`，因此清除 `ACTIVE_STALE`；
> 但新事件未 ACK Parent Review #2/Amendment #8/stale 且误报 `AWAITING_PARENT_REVIEW`。原卡 physical EOF
> 继续裁决为 `REPAIR REQUIRED / COMMUNICATION_STALE`，owner/WIP 保留，等待真实 caller battery、tryEnter
> test adapter 与正确 physical evidence 返修交付。

> 2026-07-17 15:11 EDT：TURN-36 Review #2 后超过 10 分钟无 ACK/事件/源码变化，C 标记
> `COMMUNICATION_STALE + ACTIVE_STALE`。sole owner/WIP 保留，不撤卡、不重派；恢复后继续 caller/public-path
> battery、Amendment #8 tryEnter test adapter 与 physical evidence 返修。A active writer，未运行 Maven。

> 2026-07-17 15:07 EDT：TURN-35 shared foundation 已推进到 Cloud 3/4。`CloudTaskTurnHandle` 为 authority
> 文件内嵌类，写集有效；abstract `tryEnter` 对 TURN-36 recording test seam 的机械适配已冻结为 Amendment #8，
> 仅改 C 既有固定 test 文件。C 连续两轮未 ACK Review #2，标 `COMMUNICATION_STALE`，owner/字节保留；
> 尚未达到 active-stale 阈值。A active writer，未运行 Maven。

> 2026-07-17 14:55 EDT：External C 已 ACK stale inquiry 并 canonical re-delivery；Review #2=`0/1/1 / REPAIR
> REQUIRED`。A4 nonblank fence 与 deterministic stale 已闭合，但 test 仍直接调用 protected seam，未经过
> A4 三 caller、A1 两 caller、IMG actual consumer；physical 行数实测 `2,987/719/350`，交付记录仍错误。
> C owner 保持返修；A active Java writer 期间不运行 Maven。

> 2026-07-17 14:36 EDT：External C 虽尚未在 STATUS EVENT ACK stale inquiry，但真实 named test 已继续变化为
> 38,315B / `f3dc6d20...` / 12 `@Test`，IMG 正负 battery 已落盘；据源码事实清除 `ACTIVE_STALE`，恢复
> `REPAIR_ACTIVE`。仍待 ACK 与 canonical re-delivery，父级不提前 Review #2。

> 2026-07-17 14:45 EDT：External A 已 ACK Amendment #7 与 stale inquiry，protocol 5/5 经父级实盘核验两仓
> byte-identical，DHXY `WholeTaskRuntimeLocalOperationExecutor` 已新建并继续写入；A 清除 stale、恢复
> `SOURCE_ACTIVE`。External C 尚未 ACK 14:31 inquiry，TURN-36 继续 `ACTIVE_STALE`。

> 2026-07-17 14:31 EDT：A 连续两轮未 ACK Amendment #7 且 protocol 4/5 超过 10 分钟无变化，标记
> `COMMUNICATION_STALE + ACTIVE_STALE`；C Repair Batch 2 后超过 10 分钟无 IMG/re-delivery 或源码变化，标记
> `ACTIVE_STALE`。TURN-35/36 owner 与现有字节均保护、不重派；总账已分别发定向 inquiry。

> 2026-07-17 14:20 EDT：TURN-35 Amendment #6 protocol 4/5 已实盘核验双仓 byte-identical；完整传递审计补出
> baseline `startOrdinaryEnterBattleTargetMapGate`，Amendment #7 将 closed operations 从 16 修为 17，仍复用
> 原协议五文件/executor/dispatcher/client/tests，不扩大写集。TURN-36 Repair Batch 2 已落 A1/A4 主体，实盘
> named test=10，IMG battery 与 canonical re-delivery 尚待完成。

> 2026-07-17 14:10 EDT：External A 已 ACK TURN-35 Amendment #6 并恢复 `SOURCE_ACTIVE`，当前完成合同/路径
> 核对但首个 foundation 文件尚无新字节；External C 已 ACK TURN-36 Review #1 并恢复 `REPAIR_ACTIVE`，
> nonblank/stale/seam 第一批已落盘，A1/A4/IMG batteries 仍待完成后 canonical re-delivery。两名 Java writer
> active，不运行 Maven/runtime/input。

> 2026-07-17 14:04 EDT：共享 whole-task foundation 已冻结为 TURN-35 Amendment #6，唯一使用既有 HTTPS
> `LOCAL_SERVICE`。A 负责 foundation source：pathing register/exact clear/prefix clear、movement/map/near/flying、
> pre-battle timer、dialog interest、progress/startup flying、authority nonblocking tryRun，以及五环 accept 的单个
> local exclusive operation；禁止第二 store/session、poll/sleep、Task shadow。TURN-36 Review #1=`0/2/2`，C 只返修
> A4 nonblank fence、A1/A4/IMG public tests、stale case 与交付证据，不碰 foundation 写集。

> 2026-07-17 15:15 EDT：TURN-38A-F 已出现三个 production 首增量（prepared=`1c608b88...`、
> coordination=`83092623...`、ready-event=`b9d34113...`），test 仍 absent、无 delivery。新 External A heartbeat
> `dea947fe` 已上线并合规等待；38A-F 仍由 C sole owner。

> 2026-07-17 15:05 EDT：External C 已 canonical claim TURN-38A-F，sole owner；领取快照与冻结 SHA/absent
> 一致，当前尚无首字节。只允许四项 exact write set，不碰 38A-C cleanup 或三大 Task；source review 通过后
> TURN-35/36/37 同时开放。

> 2026-07-17 15:02 EDT：父级修复 `35/36/37 -> 38A -> 35/36/37` 依赖环。TURN-38A-F 现为
> `FOUNDATION SOURCE-START READY / ZERO OWNER`，只建立 prepared peek、ready-event state 与既有 fair-turn
> authority 的唯一 Cloud 边界；后置 38A-C cleanup 不再阻塞 Whole Task source start。38A-F source review
> 通过后 TURN-35/36/37 同时转 READY。零业务差异，基线仍为 `696a12b0`。

> 2026-07-17 14:55 EDT：用户确认 External A/B/D 任务均已删除。TURN-35 的 A claim 零字节归还；TURN-37
> 的 D claim 归还并保护 `XiuluoTaskV2` WIP `c0125a49...`，无 delivery/review。TURN-35/36/37 均为
> `PLAN-CONTRACT BLOCKED / ZERO OWNER / NO READY`，故 C 没有合法卡可领。

> 2026-07-17 14:52 EDT：用户直接命令后 External d canonical claim TURN-37，父级接受 sole owner。
> Audit A exact API 机械迁移继续；prepared/event/park/map-OCR 四缺口在 Amendment #3 前 hard-fenced，禁止
> stub/恒空/第二 store/poll/sleep/local copy 与整卡 delivery。TURN-35/36 仍 blocked/zero-owner。
> External B 已于 14:42 完成 ledger-first scope/路径 ACK；A/B/C/D 通信路径均恢复。

> 2026-07-17 14:38 EDT：A/C 已从 CR worktree ACK 并恢复准确 idle；B cwd 已修复但监控仍局限于已通过
> TURN-26/TURN-23，尚待 ledger-first/第16节/候选原卡 scope ACK；D 待 ACK。当前无 READY/owner。
> 14:40 更新：D 已双 ACK，现仅 B scope/ACK 待闭合。

> 2026-07-17 14:34 EDT：CR271 唯一权威 worktree 为 `D:\mavenProject\DHXY-cr271` / `thin-client-design`；
> `D:\mavenProject\DHXY` 保持用户 IntelliJ baseline `codex/baseline-696a12b0`，禁止 Worker 切分支。heartbeat
> 报 `ledger missing` 是 cwd 错误，已通过总账 EOF 纠正。

> 2026-07-17 10:18 EDT：TURN-27 Repair #1 Parent Review #2 `P0/P1/P2=0/0/0`，owner released。
> 固定 test 路径、mirror negative、candidate 次序/独立 UUID/零 retry、exact metadata 投影/错绑拒绝闭合；
> NAV macro=0。named test 仍被共享 main compile 缺类阻断，build=`BLOCKED`。

> 2026-07-17 10:06 EDT：External C 持久化 Claude scheduled task
> `cr271-turn27-external-c-repair1-heartbeat` 已独立核验在盘，heartbeat 改为 `ACTIVE / VERIFIED`。
> 每 5 分钟继续 Repair #1 与共享总账回执；当前尚无 Java source 增量，`0/2/0` 与 build blocked 保持。

> 2026-07-17 10:02 EDT：新任务按同一 `External C` 身份连续接替 TURN-27 sole owner，并已 ACK
> 09:44 Repair #1 与 09:51 stale；状态恢复为 `REPAIR_ACTIVE`，`COMMUNICATION_STALE + ACTIVE_STALE`
> 解除。NAV/test 尚无返修增量，build 仍 `BLOCKED`；真实 heartbeat 为 `REGISTRATION_PENDING`。

> 2026-07-17 09:51 EDT：TURN-27 Repair #1 消息连续两轮无 C ACK，NAV/test 无返修增量，标
> `COMMUNICATION_STALE + ACTIVE_STALE`。External C sole owner 与 source review `0/2/0` 保持；真实 heartbeat 缺失。

> 2026-07-17 09:44 EDT：TURN-27 whole-card 父级 source review `P0/P1/P2=0/2/0`，进入 Repair #1。
> named test 缺 candidate 顺序、mirror negative、exact metadata，且越出固定 Create 路径；授权测试命令在
> main compile 缺迁移类失败，test 未执行。NAV active macro=0，production turn/local-proof mirror 边界无新增 finding。

> 2026-07-17 05:21 EDT：C 已 ACK TURN-27 Amendment #3；factory/client typed intent overload 已落盘，
> down-dispatch gap closed，旧 caller 零改动。TURN-27 继续 SOURCE_ACTIVE，尚无 whole-card delivery。

> 2026-07-17 05:16 EDT：TURN-27 Amendment #3 补齐唯一 down-dispatch。Cloud `TurnGameClient` 与
> `CloudTurnActionFactory` 新增 typed intent 兼容 overload；旧 overload 委托并传 null，所有既有 caller 零改动。
> Navigation 禁止手工构造 TurnAction、绕开 factory/port 或创建第二 dispatch。

> 2026-07-17 04:51 EDT：External C 已 ACK TURN-27 Amendment #2，继续 sole-owner `SOURCE_ACTIVE`；本地完整
> 2026-07-17 14:24 EDT：TURN-27 已 source passed，但 TURN-35/36/37 的整任务迁移仍缺 prepared non-destructive
> read、event-wait/yield、Task-facing map/coordinate 与 exact OCR/vision typed contracts；40B 反设前置会经
> 38A 形成 DAG 环。三卡保持 PLAN-CONTRACT BLOCKED/ZERO OWNER，不再误写为“等待 TURN-27”。

> 2026-07-17 04:44 EDT：TURN-27 Amendment #2 冻结导航起步证明边界。Cloud 只发携 typed intent 的动作 JSON；
> DHXY 本地按 `696a12b0` 原顺序完成 `GameStateUtil` pixel fast-edge，再在未命中时做既有小地图坐标变化兜底，
> positive 后才登记 runner watcher。不得复用 Ctrl-menu probe、COMPLETED 直接登记或在 Cloud 重建 detector/watcher。

> 2026-07-17 03:59 EDT：External C 已 ACK 并执行 TURN-27 `JAVA HALT`；Navigation 冻结
> 2810L/`90f5ea17`，Cloud watcher 重建设计作废。C owner 暂保留并只读等待 27/35-37/38-43 完整传递合同
> 修复；本地 detector/runner typed-fact 边界保持。

> 2026-07-17 03:55 EDT：TURN-27 原合同错误地下沉 DHXY runner/pathing watcher。用户纠正且父级源码确认：
> 本地 exact-window detector/runner 产生 movement、arrival、stopped-away typed facts；Cloud 只消费事实并决定下一
> JSON action。TURN-27 现 `PLAN-CONTRACT BLOCKED / JAVA HALT`，C owner 暂保留，等待 27/35-37/38-43
> 完整传递审计；active `NAVIGATE_IN_CURRENT_MAP` 零调用不变。

> 2026-07-17 03:44 EDT：External C 已 ACK TURN-27 active navigation macro 合同叫停；其余 68 个
> input/capture 站点尚未照错误模式迁移。现恢复 Cloud 业务循环与 exact-bound `TurnGameClient` 逐显式 action；
> C sole owner 保持，错误调用实际归零前 finding 继续开放，TURN-35/36/37 等待。

> 2026-07-17 03:40 EDT：TURN-27 当前 WIP `NavigationService.java:563-568` active 调用
> `LocalMacroKind.NAVIGATE_IN_CURRENT_MAP`，违反冻结合同的 active-path 零调用与逐显式 JSON action 边界。
> 已标 `PLAN-CONTRACT BLOCKING FINDING` 并要求 External C 下一拍 ACK、恢复 exact-bound `TurnGameClient`
> 逐 action；C sole owner 保留，TURN-35/36/37 继续等待。

> 2026-07-17 03:24 EDT：TURN-27 当前 WIP 为 Navigation 2804L/`ca064bf2` 与
> `CloudNavigationPathingState` 202L/`bb4ccebd`；External C sole owner、通信正常。三 resolver 仍为
> 领取 SHA，named test absent；Java writer active，35/36/37 继续等待 source pass。

> 2026-07-17 03:19 EDT：TURN-27 当前 WIP 为 Navigation 2803L/`8623fc4a` 与
> `CloudNavigationPathingState` 202L/`bb4ccebd`；External C sole owner、通信正常。三 resolver 仍为
> 领取 SHA，named test absent；Java writer active，35/36/37 继续等待 source pass。

> 2026-07-17 03:09 EDT：TURN-27 External C 已回执父级 owner 裁决与 stale 消息，
> `COMMUNICATION_STALE` 解除；C sole owner 不变。pathing state 保持 196L/`c3b68771`，
> Navigation 已更新为 `84ad42f8`；Java writer active，35/36/37 继续等待 source pass。

> 2026-07-17 02:59 EDT：TURN-27 External C 已新增 `CloudNavigationPathingState.java`
> 196L/`c3b68771`，源码活动恢复，不标 `ACTIVE_STALE`。C 仍未回执父级 owner 裁决与 stale 消息，
> 故 `COMMUNICATION_STALE` 保持；sole owner 不变，35/36/37 继续等待 source pass。

> 2026-07-17 02:54 EDT：TURN-27 由 External C sole owner 持有；父级 owner 裁决连续两轮未获回执，
> 四个既有 production SHA/mtime 仍为领取快照、两个 create 目标 absent，故标
> `SOURCE ACTIVE / COMMUNICATION_STALE`。owner 保留，不撤卡、不拆卡、不双派；35/36/37 继续等待。

> 2026-07-17 01:35 EDT：External C 已 canonical 零字节归还 TURN-36，owner 释放；TURN-35/36/37 统一
> `PLAN-CONTRACT BLOCKED / ZERO OWNER`，等待 TURN-26 -> TURN-27。

> 2026-07-17 01:32 EDT：接受 TURN-37 canonical 零字节合同归还；四类 local runtime 缺口同样影响
> TURN-35/36。三卡恢复等待 TURN-26/27；TURN-27 新增唯一 exact-context、无 TTL 的 Cloud pathing state。
> TURN-35/37=`PLAN-CONTRACT BLOCKED / ZERO OWNER`，TURN-36 要求 C 零字节 canonical 归还，归还前不双派。

> 2026-07-17 01:28 EDT：TURN-26 Repair #2 已升级 `ACTIVE_STALE + COMMUNICATION_STALE`：B 连续未回执，
> 且 Dialog/prepared/test 字节自 01:12 后无变化；owner 暂不撤销，已要求下一拍回执或 canonical 整卡归还。
> TURN-36/37 已回报迁移勘察、源码尚未漂移；TURN-35 保持 READY/ZERO OWNER。

> 2026-07-17 01:05 EDT：TURN-26 Parent Review #3=`P0/P1/P2=0/2/1 BLOCKED`。prepared state 仅测试
> publish、production 零 publisher；window/HWND/intent fence 晚于 CAS consume；objective/proof 正负矩阵不足。
> 同一卡返 External B 做 Build Repair #2，TURN-27 继续等待。

> 2026-07-17 00:42 EDT：启用 `CR271_EXTERNAL_WORKER_STATUS.md` 统一状态总账。A/B/C/D 在 heartbeat、
> claim、首字节、delivery/return/repair 与 capacity/idle 变化时向 EOF 追加标准事件；父级每 5 分钟将其与
> 原卡 canonical EOF、88 卡注册表和源码 SHA/mtime 交叉核对。总账不派卡、不替代 owner claim。
> 2026-07-17 00:52 EDT：总账增加双向 `PARENT MESSAGE` / `ack_parent_message` 协议；父级与 A/B/C/D 的状态、
> review、repair 和阻断沟通由 heartbeat 自行落盘确认，不再经过用户转发。

> 2026-07-17 00:40 EDT：External B 于 00:36:41 canonical 自领完整 TURN-26 Build Repair #1，成为
> sole owner/source-active。`DialogService` 与新 prepared-action state 已有真实 production 增量；三测试仍为
> 领取快照。父级不审中途 WIP，B 写作期间不跑 Maven；TURN-27 继续等待 TURN-26 source/final API。

> 2026-07-17 00:32 EDT：TURN-28 Repair #5 父级 Review #3 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE
> REVIEW PASSED`，d owner 释放。第三 typed recognizer seam、yellow HIT/retry 与 Spring production constructor
> 均闭合。named test 在进入本卡前被共享 Cloud main compile 债阻断；source pass 保持。TURN-26 前置 gate
> 自动满足，现为 `WHOLE-CARD BUILD REPAIR #1 READY / ZERO OWNER`；TURN-27 只继续等待 TURN-26。

> 2026-07-17 00:09 EDT：TURN-28 Repair #4 经父级 Review #2 为 `P0/P1/P2=0/2/0`。yellow-name HIT
> 仍无 executable public-path coverage；Repair #5 允许在 `NpcClickService` 内增加第三个 package-private typed
> recognizer seam，production 逐次绑定真实 `SmartClickRecognizer::findYellowTarget`。public 6 参生产构造须
> 显式 `@Autowired`，test 构造保持 package-private。整卡返 External d；TURN-26/27 继续等待 source pass。

> 2026-07-16 23:36 EDT：父级确认 TURN-28 seam visibility finding，Amendment #5 将唯一 named test
> 迁至 `src/test/java/com/bot/dhxy/service/NpcClickTurnContractTest.java` 同包，旧路径删除；seam/8参构造
> 保持 package-private。两个重复 `PipelineHarness` 必须合一，`StubDialogService` 按当前真实九参构造修复。
> External d 保持 sole owner/source-active，无需归还重领；写作期间不运行 Maven。

> 2026-07-16 23:29 EDT：External d 于 23:24:30 canonical 自领完整 TURN-28 Build Repair #4，领取点
> 九 production SHA 与 test `1c4a9474...`/34 tests 均和父级冻结快照一致；`NpcClickService.java` 已产生
> 首窗真实 seam WIP。d sole owner/source-active；父级不审中途 WIP、不双派，写作期间不运行 Maven，
> TURN-26/27 继续等待 source pass。

> 2026-07-16 23:20 EDT：父级接受 External C canonical 归还 TURN-28 Repair #3，释放 owner；九 production
> SHA 冻结并保留 34 tests/七个 public-path 维度/exact-origin WIP。Amendment #4 修复不可执行 test-only 合同：
> 仅允许 `NpcClickService` 内两个 package-private OCR-word/status-observation 叶子 seam，production 构造仍逐次
> 委托真实 `LocalOcrClient.readWords` 与 `PlayerStateService` mode probe；不启 sidecar/server、不降低 P1-1
> public pipeline 全矩阵。状态 `WHOLE-CARD BUILD REPAIR #4 READY / ZERO OWNER`；TURN-26/27 继续等待。

> 2026-07-16 22:33 EDT：External C 于 22:29:29 canonical 自领 TURN-28 Build Repair #3。
> 领取点 test `83214018...`、OcrRoiMemory `22e12c52...` 与 A 归还快照一致；九 production 文件冻结，
> C 只补唯一 named test 的完整 public NpcClick pipeline matrix 与 exact-metadata real-path origin。
> C sole owner/source-active；TURN-26/27 继续等待，不跑 Maven。

> 2026-07-16 22:26 EDT：TURN-34C 授权 named test 命令 `exit 1`，在 test 前被共享 Cloud main compile
> 债阻断；首错 `TextCandidateScanStatus` 缺失，随后 Wubei/Navigation/FiveRing 缺未迁移本地类型。错误未指向
> 34C 文件，source pass/owner release 保持，构建状态记录为 shared-debt blocked。

> 2026-07-16 22:25 EDT：父级接受 External A canonical 归还 TURN-28 Repair #3 并释放 owner。
> `OcrRoiMemoryService` test-only path seam/`@TempDir` 与 mask 四边 WIP 保留；完整 public NpcClick pipeline
> matrix 和 exact-metadata real-path origin 仍待完成。状态 `BUILD REPAIR #3 READY / ZERO OWNER`；
> TURN-26/27 继续等待 source pass，无派卡、拆卡或额外 reviewer。

> 2026-07-16 22:23 EDT：TURN-34C Build Repair #1 父级 Review #1 为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。production `e1879ed9...` 与唯一同 package test
> `fa20cd29...`/32T 锁定完整 AutoBattle task orchestration；d owner 已释放。真实 startup authority/runtime
> 仍归 TURN-38B3/40B；named test/Cloud compile 等稳定 Java writer 门，无额外 reviewer。

> 2026-07-16 22:06 EDT：TURN-34C 接受 d 零字节 PLAN-CONTRACT BLOCKED 归还并完成 DAG 解环。
> AutoBattleTask 可用 package-private scripted startup collaborator seam 验 task orchestration，public constructor
> 仍绑定真实 service；真实 startup dual-path authority/runtime 留 TURN-38B3/40B。同 package named test + 删除
> legacy-only diagnostic call 后恢复 `WHOLE-CARD BUILD REPAIR #1 READY / ZERO OWNER`。

> 2026-07-16 22:16 EDT：TURN-34C 原卡 EOF 已由 d 于 22:07:20 canonical 领取，production 已从 294 行
> `e13bfff7...` 增量到 326 行 `e1879ed9...`；注册表 stale `READY / ZERO OWNER` 已纠正为
> `SOURCE-ACTIVE / EXTERNAL-d OWNER`。唯一 named test 仍 ABSENT，中途 WIP 不审、不跑 Maven。
>
> 2026-07-16 22:15 EDT：TURN-28 Repair #2 经父级 Review #1 为 `P0/P1/P2=0/2/1 / REPAIR #3 REQUIRED`。
> 九 production SHA 冻结；唯一 named test 缺 `clickNpcSmart` 主矩阵、会移动真实 vision-memory，mask 边界和
> exact-metadata origin 接线也未证明。同卡返原 A；TURN-26/27 继续等待。d 仍写 TURN-34C，未跑 Maven。
>
> 2026-07-16 21:57 EDT：External A 已 canonical 领取 TURN-28 Build Repair #2，External d 已 canonical
> 领取 TURN-34C；两卡写集互斥、各自 sole owner/source-active。中途 WIP 不审，等待整卡 delivery/return。

> 2026-07-16 21:50 EDT：全注册表审计后并行开放两卡。TURN-28 Amendment #3 新增 Cloud
> `OcrWindowScanService` 的 baseline-exact 纯静态 full-window/mask/copy 子集，禁止 DHXY
> tracker/capture/context 实例面，状态 `WHOLE-CARD BUILD REPAIR #2 READY / ZERO OWNER`。TURN-34C 六项
> source gate 已通过并创建固定卡，独占 `AutoBattleTask.java` + 唯一 named test，状态
> `WHOLE-CARD SOURCE-START READY / ZERO OWNER`。两卡可由 External Worker 分别自行领取。

> 2026-07-16 21:12 EDT：TURN-28 计划合同已修复。External C 于 21:04 canonical 整卡归还，owner
> 释放，WIP 保留但不视为交付。Cloud 写集补齐 `OcrRoiMemoryService`、`LearnedNpcClickPoint`、
> `ResolvedNpcClickRegion`、`RecordResult`；按 `696a12b0` typed vision-memory 机械移植，只允许 caller
> 传入 exact `TurnWindowMetadata.windowRect.left/top`，禁止 tracker/context fallback、stub、恒 null、第二
> store 或 JsonNode 替代。TURN-28 现 `WHOLE-CARD BUILD REPAIR #1 READY / ZERO OWNER`，Worker 自领；
> source pass 后 TURN-26 自动转 READY。

> 2026-07-16 20:23 EDT：TURN-23 Repair #4 Parent Review #5 为 `P0/P1/P2=0/0/0`，状态
> `SOURCE+TEST SOURCE REVIEW PASSED / BUILD PENDING / ZERO OWNER`。三 production SHA 冻结；真实
> template/OCR/canonicalization/plausibility 链、FAILED exactly-one-step、完整 terminal/frame negative matrix
> 及逐案 command/UUID=`1`/旧位置不变均闭合。External B 释放，仅待 stable-writer named test/Cloud compile。

> 2026-07-16 20:15 EDT：TURN-23 Repair #3 Parent Review #4 为 `P0/P1/P2=0/1/0`，状态
> `WHOLE-CARD BUILD REPAIR #4 REQUIRED / EXTERNAL-B OWNER`。production seam、真实 template/OCR 链、FAILED
> exactly-one-step 已闭合；唯一 named test 仍缺 wrong step index/status、decoded PNG dimension mismatch
> 行为负例及失败用例逐案 UUID=`1` 断言。三 production SHA 冻结，同卡补测试矩阵。

> 2026-07-16 19:50 EDT：TURN-23 Repair #2 Parent Review #3 为 `P0/P1/P2=0/2/1`，状态
> `WHOLE-CARD BUILD REPAIR #3 REQUIRED / EXTERNAL-B OWNER`。正向 location tests 通过 public
> `RawLocation` seam 绕过 production template/OCR 链；FAILED terminal 未强制 exactly-one step，冻结
> correlation/frame negative matrix 不全；新增 seam 依赖 private reflection。同卡返修，若 fixture/OCR
> sidecar 不可达则 canonical `PLAN-CONTRACT BLOCKED` 交父级修计划，不接受假结果占位。

> 2026-07-16 19:18 EDT：TURN-23 计划合同已修复：current-location 复用现有 generic exact-window raw-PNG
> CAPTURE；Cloud 新增 location capture port 与同包 recognizer，复用现有 MiniMap recognition/map-transform
> 资产，不新增第二协议、不改 DHXY。卡恢复为 `WHOLE-CARD BUILD REPAIR #2 READY / ZERO OWNER`，外部 Worker
> 自领。TURN-28P Euler Repair #2 已获 Parent Review #4 `0/0/0`，owner 释放，只待 named tests/compile。

> 2026-07-16 18:07 EDT：TURN-26 External B 完整交付经父级 Review #2 判定
> `P0/P1/P2=0/6/0 / WHOLE-CARD REPAIR #1 REQUIRED`。DHXY-only mechanics refs 已移除，但 story objective
> 恒 fatal、prepared consume 恒旁路、SmartClick proof token 恒 null、DPI story-click 漂移、两旧 contract test
> 构造失配及唯一 test 覆盖缺口阻断；同一整卡返 B，不拆卡。用户取消额外 reviewer。

> 2026-07-16 17:57 EDT：用户批准将 TURN-34A/34B 归一化为完整父卡自领模式。TURN-34A 撤销 AT 分片
> implementation assignment，冻结 production `532e6f84...` / test `bf7a671f...`，现为
> `WHOLE-CARD SOURCE-START READY / ZERO OWNER`；TURN-34B 释放无 active task/无 Repair claim 的旧 C owner，
> 保留 Parent Review #1 `0/5/1` 与 production `8d79d198...`，现为
> `WHOLE-CARD REPAIR #1 READY / ZERO OWNER`。父级不发卡，External Worker 只在原卡 canonical 自领。

> 2026-07-16 17:41 EDT：TURN-22 完整 Repair #3 经父级 whole-card Review #5
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。五个 production/test SHA 与 TURN-22C1、
> TURN-22D1 及原卡既有通过证据一致，A owner 已释放；用户取消额外 reviewer，Parent Review #5 即为
> 完整源码审核结论。named tests/适用 compile 留 stable-writer 门。

> 2026-07-16 15:26 EDT：TURN-34B sole writer C 已创建唯一 named test，首个真实 test-source 为 119 行 /
> `0e2b40c4...`；production 保持 1,400 行 / `8d79d198...`。当前是 source-active WIP，非 delivery；不审
> 中途内容、不双派，writer 活动期间不运行 Maven。

> 2026-07-16 15:21 EDT：External C 已在 TURN-34B 原卡 canonical claim + 规范 TRUE_EOF，成为完整父卡
> 唯一 owner。领取时 `TaskMaintenanceService.java` 1,400 行 / `8d79d198...`、唯一 named test 缺失；首个
> 五分钟 source-start 窗进行中，中途 WIP 不审，C 写作期间不运行 Maven。

> 2026-07-16 15:18 EDT：完整 TURN-34B 已重新续派 External C。此前 malformed claim 已撤销且零 WIP；
> C 须原卡 canonical claim + 规范 TRUE_EOF 后才成为 owner，首窗须真实 production/test 增量、完整交付或
> 整卡归还。BP1/BP2 接受字节保留，不恢复子卡/tranche。

> 2026-07-16 15:14 EDT：External D 已 canonical 归还完整 TURN-28，三 production SHA 与领取点一致、唯一
> named test 仍缺失，零 WIP。父级释放 owner；完整卡恢复 replacement required / zero owner，保留 28P/Q/S
> 接受字节，不恢复 fragment/tranche。A/B/D 旧会话均已明确容量不足，当前不能冒充 replacement。

> 2026-07-16 15:10 EDT：External D 已在 TURN-28 原卡 canonical claim 完整父卡，正式成为唯一
> implementation owner。领取时 `NpcClickService.java` 3,527 行 / SHA `aa50ae7c...`，唯一 named test 尚缺；
> 首个五分钟 source-start 窗正在进行，中途 WIP 不审，D 写作期间不运行 Maven。

> 2026-07-16 15:08 EDT：External D 已在 lane true EOF 说明漏领检测已修且可即时承接；父级把同一完整
> TURN-28 重新续派 D。原卡 canonical claim 前仍零 owner；claim 后负责全部 production/test/report/integration，
> 保留28P/Q/S接受字节且不恢复拆分。

> 2026-07-16 15:02 EDT：整卡 owner 审计纠正：A/B 分别 canonical 零字节归还完整 TURN-23/TURN-26；
> TURN-23 因 exact current-location typed producer 不在冻结写集改为计划合同阻断，TURN-26 确认仍有 45 处
> active DHXY mechanics、等待整卡 replacement。C 的 TURN-34B claim 无 canonical TRUE_EOF 且首窗零增量，
> D 的 TURN-28 无 claim，二者 assignment 已撤销。四卡当前零 implementation owner；保留全部已接受字节，
> 不恢复 tranche/fragment/子卡派工。

> 2026-07-16 14:55 EDT：External B 已在 TURN-26 原卡 true EOF canonical claim 完整 whole-card build
> repair；领取时 `DialogService.java` 2,850 行 / SHA `9088644e...`。B 负责原 production/test/report 全合同，
> 首个五分钟 source-start 窗刚开始。A=`TURN-23`、C=`TURN-34B`、D=`TURN-28` 仍 READY/零 owner；
> Internal 0/2，B 写作期间不运行 Maven。
>

> 2026-07-16 14:47 EDT：首轮 stable-writer Cloud compile blocker 已按权威 DAG 归回四张完整既有卡，禁止再拆：
> External A=`TURN-23`（PlayerState 完整 build repair）、B=`TURN-26`（Dialog 完整 build repair）、
> D=`TURN-28`（NpcClick 完整父卡）、C=`TURN-34B`（TaskMaintenance 完整父卡）。28P/Q/S 与 BP1/BP2
> 已接受字节只作为父卡冻结证据，不再作为 implementation assignment。四卡原写集互斥；Internal 0/2。
>

> 2026-07-16 12:01 EDT：External C 的 BP2 production 已继续增量到 1289 行 / `02da7473...`，仍是受保护
> sole provisional source-active writer；claim 段规范 `TRUE_EOF` 未补，父级已写回 lane 纠偏。当前不释放、
> 不双派、不审 WIP，writer 活动中不运行 Maven；A/B/D 仍 fresh READY / 零 owner。
>
> 2026-07-16 11:55 EDT：TURN-34BP1 独立 R1/R2 最新轮均 `APPROVED 0/0/0`，父级复算报告 SHA 与
> 冻结 production/test SHA 后登记双审 `2/2`；仅剩 stable-writer named test/Cloud compile。External C 已
> 将 BP2 `TaskMaintenanceService.java` 首窗增量到 1261 行 / `c37a0186...`，继续保护 provisional
> source-active；claim 真尾与 canonical delivery 仍待，当前不审 WIP、不运行 Maven。
>
> 2026-07-16 11:47 EDT：External C 已在 TURN-34BP2 子卡末尾写 CLAIMED 正文，冻结 source/BP1 SHA
> 均逐项一致；但领取段缺规范 `TRUE_EOF` 终止，`TaskMaintenanceService.java` 仍为 `963b028c...`、零
> source 增量。父级仅保护 provisional 单 writer，等待下一 5 分钟窗补 canonical 真尾与 source-start、
> delivery 或 `OWNER RETURNED`；A/B/D 仍 fresh READY / 零 owner。
>
> 2026-07-16 11:36 EDT：TURN-34BP1 Repair #2 父级 source/test-source Review #3 `P0/P1/P2=0/0/0`。
> exact-window monotonic latch 的 production 逻辑未变；named test 已锁累计一读一槽、exact-positive 零 UUID/
> action/exhaustion 与同一 initial-A context 的 A0-B-A'。BP1 转双独立 review+build；固定 TURN-34BP2 从
> `TaskMaintenanceService.java` SHA `963b028c...` 单文件迁四个共享字符串 key 为 scoped typed keys，由 C NEXT。
>
> 2026-07-16 11:26 EDT：TURN-34BP1 Repair #1 父级 Review #2 `P0/P1/P2=0/1/2`。production 的
> per-context generation latch 已通过冻结；named test 因绝对 `metadataReads==1` 在 B 步确定性失败，并缺
> exact-positive/A0-A' 两组断言。C 直接续同卡 Repair #2；A/B/D 仍 fresh READY、零 owner。
>
> 2026-07-16 11:23 EDT：External C 已 true-EOF claim TURN-34BP1 Repair #1，并在首窗内同时产生
> production/test 增量（观察 SHA `f278460b...` / `2ed5d845...`），现为唯一 owner；中途 WIP 不冒充交付。
> A/B/D 仍分别是 Q Repair #3、S2、AT1 Repair #3 fresh claim-required，零 owner。
>
> 2026-07-16 11:15 EDT：TURN-34BP1 delivery 父级 Review #1 为 `P0/P1/P2=0/1/1`：stateless exact-native
> equality 允许同一 initial-A context 在拒绝 B 后接受 value-equal A'，且测试未执行该历史/锁零 UUID-action。
> fresh C 接同一两文件 Repair #1。桌面任务索引确认旧 A/B/C/D task 均不可发现；四路 fresh restart 后分别
> 直接实施 Q Repair #3、S2、BP1 Repair #1、AT1 Repair #3，均是写集互斥 prerequisite，不等最终 build 门。
>
> 2026-07-16 11:03 EDT：External 四线按物理 owner 纠偏。A/B/D 的旧任务均不算在线 owner，fresh A 接
> 2026-07-16 13:22 EDT：TURN-34AT1 完整 Repair #3 test-source 经父级 Review #5 `0/0/0` 通过；
> S2 R2 发现 Wubei generic catch 吞 fatal 风险；父级确认风险真实但归完整 TURN-35（`WubeiTask.java`）
> whole-task terminal 验收，S2 callers 只读且 Service existing fatal path 已闭合，原 R2 按冻结边界复审。
> TURN-28Q Repair #4 双 reviewer 均 `0/2/0`，父级确认 Unsafe/private reflection 与 queue polling，整卡
> 退 A Repair #5。TURN-28S2 Repair #1 父级 Review #2 `0/0/0`，FAILED 校验后必 fatal，进入双整卡 review。
> TURN-34AT1 Repair #4 父级 Review #7 `0/0/0`，private-field reflection 已删除，等待 fresh 双 review。
> `FAILED` 必须 fatal。AT1 R1 Approved、R2 Blocked；父级裁决 R2 的 private-field reflection P1 成立，
> 完整 AT1 退 D Repair #4。A/B/C/D writers 活动。
>
> 2026-07-16 13:18 EDT：TURN-28Q 整卡 Repair #3 delivery 经父级 Review #7 判为 `0/2/0` 并整卡退 A：
> production typed-order 通过；测试未穿透 worker taken/preamble，且仍用 polling sleep。B replacement 接
> 零 WIP TURN-28S2；D 已领取完整 TURN-34AT1 Repair #3；C 继续完整 TURN-34BP2。
>
> TURN-28Q Parent Review #6 `0/2/0` typed-order Repair #3，fresh B 接零 WIP TURN-28S2，fresh D 接
> TURN-34AT1 双审合并 `0/3/0` 单测试 Repair #3。C 已于 `10:56` true-EOF claim TURN-34BP1，并在
> `11:01` 产生 production SHA `05bbfda3...` 的真实增量，保持唯一 owner。四片写集互斥、均不以最终
> review/build 门阻止 source-start；旧 heartbeat/报告不得冒充 owner。
>
> 2026-07-16 10:43 EDT：TURN-34AT1 Repair #2 父级 Review #3 `0/0/0`；七 terminal + 一 completed
> Stage-1 在同一真实 service 上精确形成 8 commands/8 canonical distinct UUID，AT1 owner 释放并进入双审。
> D 从未 claim BP1、两目标 SHA 未动，父级改派在线 C 领取该写集互斥 prerequisite，避免同测试 review drift 与
> External 空等。A 已 true-EOF claim TURN-28S2，仍在首个 source-start 窗。

> 2026-07-16 10:38 EDT：TURN-28Q integrated source/test-source 父级 Review #5 `0/0/0`，进入双 reviewer+
> build pending。B 从未 claim TURN-28S2、目标仍 `cce8f020...`；父级改派在线 A 立即 source-start，B 旧
> assignment 撤销。C 继续 AT1 Repair #2；D 仍需 fresh restart。

> 2026-07-16 10:31 EDT：TURN-34AT1 Repair #1 已闭合 CAPTURE null shape 与七个 terminal UUID；父级
> Review #2 `0/1/0`，因共享 freshness 用例没有实际包含它声称的 positive completed capture。C 只补同序列
> 第八个 positive invocation；production 冻结。A 的 TURN-28QP1 待 claim。

> 2026-07-16 10:27 EDT：TURN-28QT1 四个 test finding 已闭合，但 frozen
> `InputActionRequest.java:458` 暴露未导入 `Objects.equals` 静态编译 P1；父级拆单行 TURN-28QP1 给 A
> 立即 source-start，综合仍 `0/1/0`。C 的 AT1 Repair #1 并行 active；B/D 仍需 fresh restart。

> 2026-07-16 10:23 EDT：TURN-34AT1 已正式交付 test SHA `6be1f3bf...`，production 保持
> `532e6f84...`；父级 Test-Source Review #1 `P0/P1/P2=0/2/0`，只退单测试 Repair #1：锁完整 CAPTURE
> null shape，并对七个 terminal case 证明 canonical/fresh UUID，而不是单元素 `distinct()==1`。A 继续 QT1
> Repair #1；B/D 仍需 fresh restart。

> 2026-07-16 10:13 EDT：TURN-28QT1 已交付但父级 Review #1 为 `P0/P1/P2=0/3/1`：缺
> `assertSame` import、缺 `attempted=false` fallback、frozen focus 缺逐次 binding-object identity 证据；A 只返修
> 单测试，production 冻结。C 的 TURN-34AT1 已 claim/增量写入；B/D 的 TURN-28S2/TURN-34BP1 仍需 fresh restart。

> 2026-07-16 10:00 EDT：TURN-34AT0 Repair #1 父级复审 `0/0/0`，test SHA `4b8460b0...`、production
> `532e6f84...`。C 立即续单测试 TURN-34AT1（Stage-1 battle flag + command/UUID/raw-PNG + terminal no
> fallback）；A/B/D 等 fresh claim TURN-28QT1/TURN-28S2/TURN-34BP1。

> 2026-07-16 09:56 EDT：A 已真实改 TURN-28Q 四文件并归还 owner；三份 production WIP 冻结，剩余 pause
> proof P1 与五组 acceptance 拆为单测试 `TURN-28QT1`。B=TURN-28S2、C=TURN-34AT0 Repair #1、D=TURN-34BP1，
> 四路均可直接开工；A/B/D 需 fresh task claim，C 由现有 heartbeat 续修。TURN-22D1 双 reviewer 已通过。

> 2026-07-16 09:50 EDT：A 已领取 TURN-28Q Repair #2，仍在首个 source-start 窗；B/D 对 TURN-28S2/
> TURN-34BP1 尚无 claim，按需 fresh restart。C 的 TURN-34AT0 已真实交付 test SHA `98e65586...`，但父级
> Review #1 为 `0/1/0`：两个 LocalServiceClient import 仍指向不存在的 `.remote`，C 只修为 `.turn.client`
> 后再交付。四路不再使用“最终门未开”作为互斥 prerequisite 的停工理由。

> 2026-07-16 09:38 EDT：External 掉线/上下文不足不再占 owner。A 的 TURN-22D1 Repair #1 父级 source review
> `0/0/0`，转 TURN-28Q Repair #2（最新父级 `0/4/0`）；B 归还未领取 TURN-34BT1，转一文件 TURN-28S2；
> C 归还 TURN-34A 763 行 WIP，转 test-only TURN-34AT0；D 旧 TURN-34BP1 零 claim 撤销后按 fresh replacement
> 重领。四片写集互斥，都是可直接 source-start 的真实 prerequisite；首个 5 分钟窗无源码/测试增量即归还。

> 2026-07-16 09:26 EDT：External 四 lane 改为可执行小片。A=TURN-22D1 public-resolver test Repair #1；
> B=TURN-34BT1 test tranche final claim window；C=TURN-34A test delivery/return window；D=新共享 prerequisite
> TURN-34BP1，补 `TaskExecutionContext` latest title/HWND/process exact-generation checkpoint。TURN-34B retained
> production 父级 Review #1 为 `P0/P1/P2=0/2/1`，原 WIP 不冒充通过。最终门不再阻止互斥 prerequisite
> source-start；claim 一个 5 分钟窗口内必须产生源码/测试增量或归还 owner。

> 2026-07-16 08:06 EDT：父级纠正“最终门=禁止任何下游开工”的排班错误。External D 的 TURN-28P assignment
> 因逾期零 claim/零字节变化已撤销，Internal Euler 接续最后两测试；External A/B/D 分别获得正式
> `TURN-22 Repair #3`、strict-696 `TURN-28`、`TURN-34B` SOURCE-START READY 卡，C 继续 TURN-34A unique owner。
> 四条 External 写集互斥；A/B/D 最终 source/build 仍挂上游测试/集成门，提前开始不构成批准。旧 Internal 六会话
> 全部 not_found，已重建 1 implementation + 5 PRECHECK 的内部 `6/6`，不再引用旧 UUID。

> 2026-07-16 07:38 EDT：External A 因剩余上下文不足已在 TURN-28P 原卡 true EOF 规范归还 owner；父级独立
> 重算确认 11 文件逐项等于领取前 SHA，零 Java 写入。当前同一剩余两测试已安全改派 External D，D 必须先在
> 原卡 true EOF CLAIMED 才可写；领取前 TURN-28P 为零 owner，绝不并发双写。External C 仍是 TURN-34A 唯一 writer。

> 2026-07-16 07:32 EDT：TURN-28P External B 已于 07:25 true EOF 归还 owner；External A 于 07:31 在原卡
> true EOF 正式领取 replacement，只改两份 DHXY contract test + 原卡，其余 9 文件按交还 SHA 只读，当前无双
> owner。TURN-34A External C 仍是唯一 owner，named test 持续写入但尚无正式 delivery。Internal 的 TURN-36、
> TURN-38M DELETE cohort、TURN-45B residual PRECHECK 已交付待父级审计，并已立即续派 TURN-38B4/38C/40D。

> 2026-07-16 06:29 EDT：TURN-33 Repair #3 独立 R1 亦交 `APPROVED / P0/P1/P2=0/0/0`，父级重算 SHA
> 并采纳；R1/R2 双审门现为 `2/2 APPROVED`。External B/C 仍写 TURN-28P/TURN-34A，故本卡只剩 writer 稳定后
> named test 与 Cloud compile/build，尚非 CARD APPROVED/CLOSED。

> 2026-07-16 06:26 EDT：External C 已在 TURN-34A 固定卡 true EOF 真实 `CLAIMED`，当前唯一 Java 写集为 Cloud
> `AutoCombatService.java`、新 `AutoCombatServiceTurnContractTest.java` 与原卡；不得改 caller、TaskMaintenance、
> protocol 或 DHXY。External B 同时仍写 TURN-28P，故 Maven/JUnit/compile 继续等待 writer 稳定。

> 2026-07-16 06:25 EDT：TURN-33 Repair #3 独立 R2 已交 `APPROVED / P0/P1/P2=0/0/0`；父级完整读取
> production/test/baseline 证据后采纳，当前 independent review 门为 `1/2`，R1 与稳定 writer 后 named test/Cloud
> compile 仍待完成。本结论不冒充双审或 CARD APPROVED。

> 2026-07-16 06:18 EDT：父级已独立核对 AutoCombat current source、四 Task caller、六个并行
> TaskMaintenance API 与 `696a12b0`，并冻结 `TURN-34A` 固定卡为 `READY / EXTERNAL-C NEXT`。唯一 production
> 写集为 Cloud `AutoCombatService.java`，点名测试为 `AutoCombatServiceTurnContractTest`；External C 必须在原卡
> true EOF CLAIMED 后才可写。旧 holder/coordinator 只迁 exact-context state/orchestration，业务条件、顺序、延时、
> 返回与动作数零变化。

> 2026-07-16 06:15 EDT：TURN-33 Repair #3 已由 External C true EOF 交付，并经父级独立 production/test-source/
> 基线复审为 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED / REVIEW+BUILD PENDING`。generated-normal
> 第五次删除后现在无条件观察且只接受稳定 EMPTY/KEEP，两个 production-path 用例证明不稳定 NORMAL 失败、
> 第六次删除及后续 scan/action/UUID 为零。External C owner 释放，下一张 TURN-34A 进入父级 brief freeze；
> External B 仍写 TURN-28P，当前不运行 Maven。

> 2026-07-16 05:58 EDT：关键阻塞 implementation 改由 External 优先承接。TURN-28P Repair #2 已由 Maxwell
> 零源码变化释放并被 External B true EOF `CLAIMED`。TURN-33 独立 R2 发现 generated-normal 第五次删除在
> post-delete stability observation 前提前 success；父级独立复核为 `P0/P1/P2=0/1/0 / REPAIR #3 REQUIRED`，
> External C 已于 06:05:57 在 TURN-33 原卡 true EOF `CLAIMED` 本卡两文件返修，TURN-34A 顺延。Internal 六槽主要用于 review/readiness/preflight；
> External A 等 28P 后做 TURN-22 Repair #3，D 等 TURN-22/33 后做 TURN-34B。

> 2026-07-16 05:48 EDT：TURN-28P Repair #1 父级独立复审为
> `P0/P1/P2=0/2/1 / REPAIR #2 REQUIRED`。零二次 refresh、started callback cleanup barrier 与 Ctrl-UP typed
> release 已闭合；仍需在同一 context monitor 内冻结 generation 并贯穿 focus/callback/finally，frozen facade 复用
> structured result 保留 `STOP_REQUESTED`，并补 public+real queue/worker、A->B->A drift、outer-worker UP 与 Cloud
> code-only/frame-only uncertainty 测试。TURN-22/28 source 门继续等最终 API。
>
> 2026-07-16 05:38 EDT：TURN-22 双 reviewer 阻断经父级独立确认，合并
> `P0/P1/P2=0/2/1 / REPAIR #3 REQUIRED / BLOCKED BY TURN-28P Repair #1`。Cloud named test 直接导入
> DHXY-only executor/queue/window 类且无 Maven 依赖，无法 test-compile；真实 executor 仍走会二次 refresh 的
> legacy queue；context restore 为 empty-to-empty 伪阳性。28P frozen API 落盘后，Cloud 保留业务/JSON 测试，
> DHXY executor/test 闭合 exact snapshot、sentinel restore、一次 150/500 queue 与 drift 零 input。

> 2026-07-16 05:32 EDT：TURN-33 Repair #2 父级复审 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW
> PASSED / INDEPENDENT REVIEW+BUILD PENDING`。终极角真实 click 后结束当前 pass，hover/miss 不伪装 click；
> production API fixture 证明 ultimate click=`1`、generated delete=`1`、后续 static scan/command/UUID=`0`，
> 五次普通删除 budget 保持。Leibniz owner 已释放，待双 reviewer 与稳定 writer 后构建门。

> 2026-07-16 05:23 EDT：TURN-33 Repair #1 父级复审 `P0/P1/P2=0/1/0 / REPAIR #2`。all-exit
> lightweight cleanup 与 fresh static scan/五次 production budget 已关闭；剩余 P1 是 generated-normal 终极角分支
> 会重新进入 fresh loop，导致同 pass 重复终极角，违反 `696a12b0` 单次语义。Leibniz 仅在原 Service/test 修复并
> 补“终极角点击后后续 static scan/command/UUID=0”生产路径负例；TURN-34A/C 继续 blocked。

> 2026-07-16 05:14 EDT：TURN-28P 两名独立 reviewer 与父级复核确认 `P0/P1/P2=0/2/2`，旧 source 初审
> `0/0/0` 已被新证据覆盖。Repair #1 由 Maxwell 实施 frozen exact-window queue、drift fail-closed、started callback
> cooperative cancellation/finally completion barrier、probe uncertainty 与 Ctrl-UP typed failure；不改 JSON
> protocol/Cloud OCR/业务。TURN-28 继续等该 source/test-source 门。
>
> External A 的 TURN-22 Repair #2 已由父级 Review #3 `P0/P1/P2=0/0/0`：本卡 emitted typed click 已穿过
> production executor，并由 recording queue 直接证明一次 `CLICK_LEFT(150)->SLEEP(500)` submission。当前进入
> Faraday/Peirce 两名独立 reviewer + build pending。TURN-33 按父级裁决执行“每次删除后 fresh 静态尾扫、whole
> pass 最多 5 次”；Leibniz 仍在原写集返修。Java writers 活动，不运行 Maven。

> 2026-07-16 04:27 EDT：TURN-33 Leibniz 四文件交付经父级独立审查为
> `P0/P1/P2=0/2/0 / REPAIR REQUIRED`。当前 Cloud Summon pass 的 fatal/uncertain/confirmed STOP 会越过
> baseline lightweight cleanup；named test 的“最多 5 次删除”仅 reflection 常量，未执行 production budget。
> Repair #1 已退原 Worker，TURN-34A 继续 gated。External A/B/C/D 均已上线但 startDependsOn 未满足；四 lane
> heartbeat 按用户确认每 5 分钟且无变化静默，父级审查 heartbeat 保持每 1 分钟。
>
> 2026-07-16 03:29 EDT：原 TURN-28P Raman、TURN-33 Goodall 及 TURN-35/36/37、TURN-34C readiness
> 六个会话均经父级轮询确认为 `not_found`。已在原卡/各自唯一报告 true EOF 完成 replacement 领取：TURN-28P
> Locke `019f69ce-9359-71a1-8402-cb7ee7d34404`、TURN-33 Faraday
> `019f69ce-d84c-7a11-a832-3ce77f8f739a`、TURN-35 Pauli、TURN-36 James、TURN-37 Feynman、TURN-34C
> Franklin。TURN-33 继续保护原四文件半成品，TURN-28P 沿用父级冻结写集；四条 readiness 只写 PRECHECK，
> 不批准、不写 Java。Java writers 活动，本轮不运行 Maven。

> 2026-07-16 03:08 EDT：TURN-28P helper 已 true EOF 完成，父级独立复核真实 protocol/executor 与
> `696a12b0` 后冻结为共享 countDelta=0 prerequisite。双仓 `TurnInputSpec` 增加仅 CLICK_LEFT/RIGHT 可用的
> nullable `clickDelayMs/queueHoldMs`，由 DHXY production mapper 一次 queue submission 执行；双仓
> `TurnCaptureSpec` 增加单 CAPTURE `pixelChangeProbe`，DHXY 在一次 exclusive callback 内对同一 HWND/ROI 执行
> before/Ctrl DOWN/MOVE/after/finally UP，只回 changed/unchanged 与唯一 after raw PNG。Cloud 保留 OCR/FIFO/业务；
> 无 retry/session/ledger/TTL。Raman `019f69c4-3ef0-7ff3-a5db-ebfc7c541130` 已派发，待固定原卡 true EOF
> CLAIMED；通过后先返修 TURN-22，再解锁 TURN-28。

> 2026-07-16 02:47 EDT：TURN-27 非绑定 readiness 已 `PRECHECK_COMPLETE` 并关闭。四 production + 一 named
> test 写集、`696a12b0` route ladder、两次 world-map fallback、X2 同 action 与测试矩阵已整理；但
> `navigateToNPC` 必须保持纯导航、NPC 点击留 Task 后续 phase，且 TURN-28 final public API 未落盘，故当前仍
> `BLOCKED BY TURN-28 FINAL API / NOT CLAIMABLE`。释放容量已用于 TURN-35/36/37 readiness。

> 2026-07-16 02:40 EDT：TURN-22 首次父级 production/test-source/真实 DHXY executor 审查为
> `P0/P1/P2=0/1/0 / REPAIR PREREQUISITE BLOCKED`。现 `CLICK_LEFT -> WAIT150 -> WAIT500` 不能保持
> `696a12b0` 的 150ms click delay 与 500ms 同 queue ownership；Averroes owner 已释放，先补通用
> queue-owned post-click mechanics，再返修原 assembly/test。TURN-28 readiness 同时确认后台 Alt+A/Alt+C 与
> Ctrl capture/finally-release 缺口；TURN-28P 正做非绑定预检，父级冻结前不可领取。TURN-33 继续实施。

> 2026-07-16 02:13 EDT：TURN-26 六份 production/test source 已由父级独立复审 `P0/P1/P2=0/0/0`，同帧
> OCR/white-story、typed OCR client、terminal/correlation 与 source gate 闭合；named test/build 留 stable-writer
> cohort。该 source gate 已解锁 TURN-33，父级已冻结真实实施卡；Goodall
> `019f6990-dfbb-7373-8580-4944ce8f5c60` 已于 `02:16:25` 在原卡 true EOF 真实领取并进入实施。
>
> 2026-07-16 02:00 EDT：TURN-33 helper 报告已由父级独立对照当前 Summon/maintenance/旧 authority 源码、
> `696a12b0` 与用户确认的静态格子规则复核。组合基线、三文件写集、每 action exact-window/新 UUID/局部
> queue 原子和 scoped legacy gate 已预冻结；不恢复 session/owner/ledger/exclusive acquire-release。状态仍是
> `PARENT PRECHECK REVIEWED / WAITING TURN-26 SOURCE GATE / NOT READY`，未派 Java。
>
> 2026-07-16 01:51 EDT：父级已按 `696a12b0`、真实 TeamReturn caller、双仓模板 SHA 与 typed turn
> 合同冻结 TURN-22；Averroes `019f6979-7699-7fc2-b50b-0c35c1d3ace2` 已在固定报告 true EOF 真实领取。
> 唯一 active path 为 exact `272x69` raw PNG 上云、Cloud member/leader match，以及成员基线顺序和单 JSON
> `CLICK_LEFT/WAIT150/WAIT500`；TURN-26 同时在互斥写集实施，Java writer 活动期间不运行 Maven。
>
> 2026-07-16 01:34 EDT：TURN-23 Repair #1 经父级独立 production/test-source/真实 caller/baseline 复审为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。open-main-bag session 保持至 TURN-36；confirmed
> CAPTURE failure 后一次 Bag action；两 port initial HWND/process pre-command fence 与四个零 command/UUID
> 负例闭合。owner 释放，TURN-22 source 前置解除；named test/build 留 stable-writer cohort。

> 2026-07-16 01:24 EDT：TURN-23 首版经父级独立 production/test-source/真实 caller/`696a12b0` 审查为
> `P0/P1/P2=0/3/0 / REPAIR #1 REQUIRED`：保留 FiveRing open-main-bag session 至 TURN-36；confirmed CAPTURE
> failure 后仍只发一次 Bag action；first-aid/incense port 在 UUID/action 前补 initial HWND/process fence。
> Mill 在原写集返修，零自动 retry/session/ledger/TTL。
>
> 2026-07-16 01:24 EDT：TURN-26 已由 Ptolemy 于 `01:18:28` 在固定报告 true EOF 真实领取。完整 DAG
> 当前没有第三张依赖满足且写集互斥的 READY Java 卡；父级并行派非绑定 helper 只读预审 TURN-22/33，帮助
> TURN-23/26 通过后立即冻结下一波，但 helper 不能写 Java或自批。

> 2026-07-16 00:55 EDT：TURN-T04 经 TURN-10CR 四态合同后的父级 integration recheck 为
> `P0/P1/P2=0/1/0 / TEST REPAIR #1 READY`。`LocalServiceStepDispatcherContractTest` 仍 override 旧 boolean Give
> API，必须改为四态 whole API 并经 production dispatcher 逐态断言 exact JSON、一次 exclusive/whole call、
> legacy direct 零调用；只改该 test 与原报告，不改 production。
>
> 2026-07-16 00:52 EDT：TURN-19 Repair #1 经父级独立 production/test-source/bound-client 复审为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。observe/click 均在 port 前建立 exact bound view；
> wrong-current-context 零 metadata/execute，OPEN click 同一 command 精确为 `MOVE/WAIT120/CLICK/WAIT250`。
> owner 释放，named test/Cloud build 留 stable-writer cohort。
>
> 2026-07-16 00:49 EDT：TURN-25 父级独立 production/test-source/context/baseline 审查为
> `P0/P1/P2=0/3/0 / REPAIR #1 REQUIRED`：prepared validation 必须传播 port 的 fatal uncertain/correlation；
> 两个 port 对 latest STOP 必须在 UUID/command 前 checkpoint；exact binding 必须同时核 immutable HWND/process。
> Repair 只改原三 production、唯一 named test 与报告，保持一次 action/一 raw PNG frame、Cloud-only 算法和零 retry。
>
> 2026-07-16 00:43 EDT：TURN-21 父级独立 production/test-source/caller/baseline 审查为
> `P0/P1/P2=0/3/0 / REPAIR #1 REQUIRED`：补 exact client pre-port bind、同 command 尾随 `WAIT(120)`，并以
> current exact metadata 关闭 turn-native initial-title stale pending。Repair 保持原 Service/assembly/test/report
> 写集，零新 transport command/retry/session/ledger/cache/TTL。

> 2026-07-16 00:37 EDT：TURN-19 父级独立 production/test-source/caller/baseline 审查为
> `P0/P1/P2=0/2/0 / REPAIR #1 REQUIRED`。Repair 仅补 exact invocation context 的 pre-port bound client，及
> 同一 click JSON command 的尾随 `WAIT(250)`；并补 wrong-current-context 零 port 调用负例。零新 command、
> UUID、retry/session/ledger/TTL，TURN-21/25/23 继续互斥写入。

> 2026-07-16 00:29 EDT：TURN-23 brief 在 production 写入前按 `696a12b0` 纠正 incense fallback：cached
> icon offset 首先请求 computed `48x34` narrow raw PNG；template miss 后 Cloud 显式发第二个 `123x34` full-panel
> action。每个 probe 一 command/新 UUID；第二 action 是既有 business fallback，不是 transport auto-retry。

> 2026-07-16 00:27 EDT：TURN-23 已由 Mill `019f692a-4148-7ac0-a064-ca68d8cc7f8d` 在 fixed-report
> true EOF 真实领取；与 TURN-19/21/25 写集互斥，四槽满载。唯一 countUnit 绑定 AutoCombat first-aid caller；
> raw bars/incense PNG 上云、Cloud 判断、ordered JSON input，本地只 mechanics 与既有 BagService。

> 2026-07-16 00:11 EDT：TURN-23P 经父级独立 production + named-test source 审查为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。双仓 capture spec/validator 与 protocol tests byte parity；
> pointer 只在 inclusive padded ROI 内触发单 queue MOVE+WAIT，成功后同 HWND requested capture，terminal 不执行
> requested ROI capture。owner 释放，named tests/双仓 compile 待 stable-writer cohort；TURN-23 source 前置已解除。

> 2026-07-16 00:02 EDT：TURN-23P/19/21/25 原 implementation 会话均 `not_found`；父级保护全部落盘半成品，
> 已按原卡 replacement 为 Anscombe/Leibniz/Boole/McClintock，四张固定报告 true EOF 均已领取。TURN-21
> replacement claim 的 package 路径抄写错误已在任何 production/test 写入前纠正，错误路径零创建、零修改；
> 四卡 exact write set 继续互斥，业务与测试合同不变。

> 2026-07-15 23:47 EDT：TURN-16/30/31/32 均经父级独立 production + named-test source 审查为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`，owner 已释放，构建 cohort 待稳定 writers。四个
> 互斥实现槽已滚入 TURN-23P/19/21/25：前三卡分别闭合条件式 pointer-clear、LeftTop raw ROI 与 CommonBox
> raw ROI；TURN-25 固定为一次 HTTPS JSON action 回传 raw dialog PNG，Cloud 同帧分类，prepared validation
> 只 fresh capture 一次并在 Cloud 完成 wash/fingerprint/distance。无本地 Dialog OCR/分类、无自动 retry。

> 2026-07-15 23:31 EDT：父级已冻结 TURN-23P。CAPTURE JSON 新增可选
> `clearPointerIfOverRegion={paddingPx,targetX,targetY,settleMs}`；Cloud 提供 exact unscaled target，DHXY 仅在
> pointer 位于 padded ROI 内时单 queue MOVE+WAIT，随后对同一 HWND 后台截图，pointer null/outside 零 input。
> TURN-23 固定沿用 `696a12b0` 的 12px/300ms，不得总是 move-away；本卡为 countDelta=0 真实共享前置。

> 2026-07-15 23:12 EDT：TURN-19/21 parent brief 已冻结。两卡均采用真实 window origin、严格不缩放 raw PNG
> ROI、Cloud same-frame match 和 TURN-09R 原子 move/wait/click；TURN-21 继续保持 role/task-run/window/identity、
> priority 与 30 秒 pending。port/result records、DHXY/caller/Task/protocol 全部只读，首个互斥槽释放即滚入。
> 无已批准业务差异。

> 2026-07-15 23:10 EDT：TURN-10CR Repair #1 经父级独立逐文件复审为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。四态 closed enum/JSON 精确恢复 entry miss、
> interrupted、give failure 与 success 的真实 caller 分支；旧 boolean API、机械顺序、单 command 与零 retry 未改。
> owner 释放，TURN-16 立即滚入；named tests/DHXY compile 待 stable-writer cohort。无已批准业务差异。

> 2026-07-15 22:56 EDT：TURN-10CR mechanics 源码门仍通过，但父级追到真实 FiveRing caller 后发现 `given`
> boolean 会合并 `GIVE_OPTION_NOT_FOUND/GIVE_ITEM_FAILED/INTERRUPTED`，而 caller 分别执行 cleanup+resync、错误累计
> 和 stop。Parent Integration Review #2 为 `P0/P1/P2=0/1/0`；Repair #1 固定为同一 JSON 中的四态 state，零
> 新命令/重试。TURN-16 已按同一合同冻结，可与 DHXY repair 跨仓并行。

> 2026-07-15 22:50 EDT：TURN-10CR 经父级独立逐文件审查为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。永久本地 `GiveItemService` 已在一次既有 exclusive
> callback 内完整闭合 `give-entry -> item-select -> final give`，旧 public API、短路顺序与单 command 合同不变；
> 两份 named-test source 直接覆盖 adapter 与 production Service。owner 释放，TURN-16 解锁；测试/compile 待
> stable-writer cohort，不冒充 CARD CLOSED。

> 2026-07-15 22:45 EDT：TURN-09R Repair #1 经父级独立复审为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。双仓十一项 input golden contract 同字节，DHXY
> regression 证明 `MOVE -> WAIT -> CLICK` 只有一次 queue submission，尾随 WAIT 保持原 step 后再 capture。
> TURN-19/21 共享前置已解除并进入父级 brief freeze；TURN-23 仍等 pointer-over-ROI 条件式等价合同。named
> tests/dual compile 待 stable-writer cohort；无已批准业务差异。

> 2026-07-15 22:36 EDT：TURN-29 经父级独立源码/测试源码审查为 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE
> REVIEW PASSED`。十个 Cloud production 文件闭合同帧 TaskTracker core；八张 live template 与 DHXY SHA-256
> 一致，exact action/window/step/frame、单 command、terminal fail-closed 与 cache scope 均成立。named test/build
> 待 stable-writer cohort；TURN-30/31/32 三个真实 Task caller 现已解锁并可互斥并行。

> 2026-07-15 22:27 EDT：TURN-20 与 TURN-24A Repair #1 均经父级独立复审为 `P0/P1/P2=0/0/0 / SOURCE+TEST
> SOURCE REVIEW PASSED`。前者恢复 known FAILED 的 null/false/re-observe fallback 并回到 canonical
> `LocalOcrClient`；后者传播 confirmed stop、保留 unconfirmed IN_COMBAT 与单 capture。两卡仍等待 named test/
> Cloud build，不冒充 CARD CLOSED；无已批准业务差异。

> 2026-07-15 22:23 EDT：TURN-09R Parent Review #1 为 `P0/P1/P2=0/1/1`。production 的
> screen-absolute `MOVE_MOUSE` 与闭合 mouse/WAIT 单 queue transaction 通过；双仓既有 core golden test 仍固定
> 十项而会在十一项 enum 越界，且缺 trailing WAIT 留在 transaction 外的直接回归。Repair #1 仅改三项测试源与
> 原报告；TURN-19/21/23 在其通过前继续阻断。无已批准业务差异。

> 2026-07-15 22:12 EDT：CR271 改为最多七条 implementation 动态滚动。父级发现
> `GIVE_ITEM_FROM_OPEN_DIALOG` 当前 adapter 只覆盖物品选择与最终给予按钮，不能证明从已打开 option dialog
> 开始的完整 local macro；已新增互斥 Foundation repair `TURN-10CR`。它只在永久本地 `GiveItemService` 增加
> `give-entry match/click -> existing direct give` 闭环并更新 adapter/点名测试；旧 public give API 与 local
> Dialog 继续保持，TURN-16 改为等待 10CR。无第二 command、无自动 retry、无第五本地 Service。

> 2026-07-15 21:35 EDT：TURN-14 在固定报告 true EOF 交付后，父级独立源码/测试源码审查为
> `P0/P1/P2=0/1/0 / REPAIR #1 REQUIRED`。三种 Bag intent、ordinary incense、prescan 顺序、terminal、raw PNG、
> strict JSON 与 `696a12b0` 主链均无其它问题；唯一 P1 是 FOUND cache point 未与本次请求模板做精确等值关联，
> 坏响应可能被缓存并在回程点击错误物品。Repair #1 已退原 McClintock，仅补 client 关联校验与 mismatch 负例，
> 保持一个 UUID/command、零 retry；当前四槽为 TURN-14 Repair、TURN-20、TURN-24A、TURN-29。

> 2026-07-15 21:16 EDT：`TURN-15 Repair #1` 经父级独立完整复审为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。production mapper 已拒 duplicate、numeric enum、
> string boolean、null creator/scalar coercion；新增负例均严格一 UUID/一 command/零 retry，四 operation、
> checkpoint、terminal 与 X2 单宏无漂移。Mill owner 已释放；named test/Cloud build 待稳定 cohort。释放槽已冻结
> TURN-29 的十文件 TaskTracker core 与唯一 named test，并派给 Galileo
> `019f6880-b69e-77a1-9fbe-ce084910ae99`；已于 `2026-07-15T21:20:07-04:00` 在固定报告 true EOF 真实领取。

> 2026-07-15 21:05 EDT：`TURN-17` 父级独立审查为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`；Quest activate/detail 保持单 command、严格 terminal、
> 同 command raw PNG 与绝对 region，真实 caller 仍留 TURN-37。`TURN-15` 因结果 JSON mapper 未拒 numeric enum、
> string boolean、duplicate key/scalar coercion 被父级记 `P1=1 / Repair #1`，原 Mill 精确返修 client/test/report。
> 释放槽 `TURN-24A` 已由 Pauli 于 `21:07:08 EDT` 在固定报告真实 EOF 领取；唯一覆盖键为
> `AutoCombatService -> BattleRadarService::checkAndSyncCombatState`，当前四槽为 TURN-14、TURN-15 Repair、
> TURN-20、TURN-24A，production 写集互斥。

> 2026-07-15 20:52 EDT：`TURN-18` 经父级独立逐文件审查为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。`ClientIdentityService` 只读 exact-bound latest
> `TurnWindowMetadata`，missing/blank/malformed/错绑定均不改角色；named test 对每条路径断言 metadata read=1、
> execute=0。Maven/Cloud compile 待 writers 稳定，不是 CARD CLOSED。Chandrasekhar owner 已释放；TURN-20
> 已由 Plato 于 `20:54:42 EDT` 在 fixed report true EOF 领取唯一 Service/test 写集，R2 恢复 `4/4 CLAIMED`。

> 2026-07-15 20:33 EDT：TURN-13C 经父级逐文件源码/测试源码审查为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`，owner 释放。named test 与正常 compile 均在本卡
> 代码执行前被写集外 Cloud 旧 Service/Task 缺 DHXY-only 类阻断；不是本卡返修项，也不是 CARD CLOSED。
> TURN-14/15/17/18 已按 `turn/client` 真实路径和唯一 testWriteSet 转 READY：17 只交付 Quest client、caller 留
> TURN-37；18 为零 action `STATE+BASE` latest metadata 读取，不造第二 cache/type/action。
> 当前 R2 四槽已全部在固定报告 true EOF 真实领取：TURN-14/15/17/18 为 `4/4 CLAIMED / IMPLEMENTING`。

> 2026-07-15：`TURN-13C` 已由 Hume `019f683d-88bc-7262-8afa-476882b6d791` 在固定报告 true EOF 领取，
> 唯一写集为父级冻结的七个 Cloud production 文件、`TaskExecutionContextTurnContractTest` 与原报告；当前单一
> Java writer 活动期间不运行 Maven，交付后由父级独立逐文件审查。

> 2026-07-15 20:03 EDT：`TURN-13C` 原五文件写集经双 helper 预检确认缺 provider 闭环；父级已冻结纠偏：
> 增加 `LegacyTaskExecutionTurnContextProvider` 与 bound `TurnGameClient`，新 context 无 `RemoteTaskRun*` 构造，
> Holder 不注入 client，错线程/错嵌套在 port 前 fail-closed；production Task factory 仍留 TURN-40B。

> 2026-07-15 20:00 EDT：`TURN-T03B Repair #1` 经父级独立复审为 `P0/P1/P2=0/0/0 / TEST SOURCE
> REVIEW PASSED`。raw ROI PNG uncertain retention/ACK clear/defensive copy、确定性 monitor BLOCKED+owner、
> mechanics 顺序、bounded cleanup 与 exchange 后 stop/remove 永久退役均闭合；原 named Maven/compile 门待
> stable-writer cohort，本结论不冒充 card approved。

> 2026-07-15 19:46 EDT：`TURN-13H Repair #1` 经父级独立复读为 `P0/P1/P2=0/0/0 / SOURCE+TEST
> SOURCE REVIEW PASSED`。真实 Spring host refresh/open/close 前后比较全部 alive thread ID，零 allowlist；同一
> commandPort/catalog、required host、窄 scan 与零调用断言保留，production 无漂移。Maven/Cloud compile 待
> writers 稳定；owner 释放，`TURN-13C` 依赖解锁并转 READY。

> 2026-07-15 19:41 EDT：T03A Repair #3 lazy reusable client 经父级全类源码审查 `P0/P1/P2=0/0/0`，
> named tests/compile 待跑。T03B 初次 test-source 通过被 Review #2 覆盖为 `0/2/3 / Repair #1`：补 raw PNG
> uncertain retention/ACK clear/defensive copy、确定性 monitor BLOCKED、mechanics event order、线程 finally
> cleanup 与 concurrent stop/remove；原 Nash 仅写四 test+报告。

> 2026-07-15 19:38 EDT：`TURN-13H` 四 production 源码的同一 commandPort/catalog、required host 注入与
> 窄 scan 经父级保留；唯一 test 未在 context refresh/close 前后执行零新增 live-thread 断言，父级记
> `P0/P1/P2=0/1/0 / Repair #1`。原 Kuhn 仅改 test+报告，production 全只读。

> 2026-07-15 19:30 EDT：Kuhn 已真实领取 `TURN-13H` 四 production + 一 capability-test 写集，要求同一
> commandPort/catalog 注入 dormant host、零 activation；Ampere 并行实施 DHXY lazy HttpClient Repair #3，
> 两 production 写集互斥。Java writers 稳定前不运行 Maven。

> 2026-07-15 19:29 EDT：`TURN-T03B` 六测试经父级独立源码/production 交叉审查，test-source
> `P0/P1/P2=0/0/0`；但 inert wiring 测试有效揭出 `HttpsTurnClient` 构造即启动 JDK selector thread，
> covered TURN-06/13 production 记 `P1=1`。T03A Repair #3 只做单 reusable client 的 thread-safe lazy init，
> 禁止 per-request client/retry；六 named tests 仍 selected 0，不能写 card approved。

> 2026-07-15 19:24 EDT：`TURN-T01 Repair #1` 父级独立复审为 `P0/P1/P2=0/0/0 / TEST SOURCE
> REVIEW PASSED`。strict test mapper 命名、三类 invalid step index 与 Quest+capture 单帧冲突均闭合；父级
> 重算双仓 12/12 test/fixture 与 29/29 production protocol 全部同字节。十条标准 Maven 仍 selected 0，
> Cloud test-tree retention 亦待闭合，故不是 card approved；Ohm owner 已释放。

> 2026-07-15 19:20 EDT：`TURN-T03A Repair #2` 与 `TURN-13G Repair #2` 经父级独立源码/测试源复审均为
> `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`。真实 DHXY client parser 四类 malformed 200 均
> typed fail-closed 且单 POST；13G 保持所有 status exact action/window、仅 COMPLETED/FAILED full step
> correlation，并闭合真实 PNG 与 canonical empty uncertain/no-retry。标准 Maven/Cloud compile 仍受共享债
> 阻断，不能伪写 card approved；13G owner 释放，13H 依赖解锁。

> 2026-07-15 19:14 EDT：`TURN-13G Repair #1` 关闭真实 PNG 与 COMPLETED mismatch 两项原 P1，但
> full step correlation 错误扩到 STOPPED/UNCERTAIN，会拒绝 canonical empty-step uncertain；父级按 TURN-01D
> 冻结语义退 `Repair #2 / P1=1`，只允许 COMPLETED/FAILED full correlation，其他 status 仍 exact action/window。

> 2026-07-15 19:09 EDT：`TURN-T01` 父级首审 `P0/P1/P2=0/1/2 / REPAIR #1`；双仓 12/12
> test/fixture 与 29/29 production protocol 虽完全同字节，但测试私有 strict mapper 不得冒充真实 parser，
> 且缺 invalid step index、Quest+capture 双帧负例。交叉审查另重开 `TURN-T03A Repair #2 / P1=1`，补真实
> `HttpsTurnClient` 的 null/numeric-enum/float/scalar fail-closed 与单 POST 断言。

> 2026-07-15 19:02 EDT：`TURN-13G` 父级独立源码/测试首审为 `P0/P1/P2=0/2/0 / REPAIR #1`。
> 一次 UUID/一次 command/no-retry 主链保留；返修只补 submitted action 与 outcome stepResults 的数量/类型
> exact correlation，以及把 signature+ASCII 伪 PNG 改为可解码 2x2 raw PNG 并覆盖 mismatch fail-closed。
> 原 Kuhn 继续返修；`TURN-T03B` 已由 Nash true EOF 领取，写集互斥。

> 2026-07-15 18:56 EDT：`TURN-T03A Repair #1` 经父级独立复审 `P0/P1/P2=0/0/0 / TEST SOURCE
> REVIEW PASSED`；HTTP interrupt、capture pixels/binding drift、WAIT success/interrupted 三项 P1 关闭。
> 五个标准 Maven 命令仍被写集外 stale testCompile 债阻断，故不是 card approved；owner 释放，T03B 已派。

> 2026-07-15 18:52 EDT：`TURN-T02` 六个 Cloud contract tests 经父级独立源码/production 交叉审查为
> `P0/P1/P2=0/0/0 / TEST SOURCE REVIEW PASSED`；126-byte `2x2` PNG 与六个 Java hash 已重算吻合。
> 六个标准 Maven 命令仍在选中测试前被写集外 Cloud production compile 债阻断，故不是 card approved；
> T02 test owner 已释放，下一槽滚入互斥 T03B。

> 2026-07-15 18:47 EDT：`TURN-T03A` 五个测试经父级独立断言审查为 `P0/P1/P2=0/3/0 / REPAIR #1`：
> 补 `HttpsTurnClient` interrupt typed mapping/no retry、capture 真实 pixel 与 binding drift 隔离、WAIT 正常完成及
> interrupted `STOPPED`。25 个 isolated pass 仅作诊断，五个标准 Maven 命令仍未越过旧 `testCompile` 债。
> T01/T02 的重复写集已按最早领取纠正；T03B 六测试与返修互斥，可并行续派。

> 2026-07-15 18:36 EDT：`TURN-T04` 五个 test source 经父级独立断言审查 `P0/P1/P2=0/0/0`；19 个
> isolated pass 只作诊断，五个标准 Maven 命令仍被写集外 stale testCompile 债阻断，故状态为 `TEST SOURCE
> REVIEW PASSED / MAVEN GATE BLOCKED`。Kuhn 槽已立即滚入关键链 `TURN-13G`，当前四槽
> `T01/T02/T03A/13G`。

> 2026-07-15 18:33 EDT：`TURN-40A` 双仓 8 对 lifecycle protocol 经父级重算 SHA 与逐行复审，结论
> `P0/P1/P2=0/0/0 / SOURCE REVIEW PASSED / TEST+CLOUD BUILD PENDING`。计划内 lifecycle 测试名歧义已统一为
> `TurnTaskLifecycleProtocolGoldenJsonTest`；Nash 同槽立即滚入 T01。当前四槽为 `T01/T02/T03A/T04`。

> 2026-07-15 18:30 EDT：`TURN-02R` production 经父级独立源码审查 `P0/P1/P2=0/0/0 / SOURCE REVIEW
> PASSED / TEST+BUILD PENDING`。outcome+raw PNG 同 future、exact-window latest metadata 与无图片历史合同闭合；
> 原 owner 释放后 Pasteur 已立即滚入 `TURN-T02`。四槽继续为 `T02/40A/T03A/T04`，没有规划空档。

> 2026-07-15 最小 JSON turn 已再次确认并恢复滚动实施。后台并发上限实测为 4，当前 `02R/40A/T03/T04`
> 四槽全满；下一队列为 `T01/T02/T03B/13G`，任一交付通过父级审查后立即续派，不等整批。

> 2026-07-15 四路已在开工前按用户要求暂停：零 Java/test/fixture/Maven，只有 T01/T04 两份领取报告。等待用户
> 重确认最小 HTTPS JSON turn 后原路恢复；没有第二协议、poller/broker/session/ledger 或本地自动业务 retry。

> 2026-07-15 用户恢复四路实施：Pasteur=`02R+T02`、Nash=`40A+T01`、Ampere=`T03`、Kuhn=`T04`。
> 四路 production/test 写集互斥，测试债使用 start/approval 双门；Worker 不自批，父级逐份审源码、断言、命名
> 测试与 compile。当前仅 CLAIMED/开工，尚无交付或批准。

> 2026-07-15 HTTPS turn 测试验收门已获用户批准：历史 `SOURCE APPROVED` 不再等同测试通过。权威计划第 19 节
> 为每张 Java 卡冻结命名 unit/contract test、成功/失败/停止/不确定 outcome、raw PNG bytes、actionId 去重及
> 696a12b0 等价断言；已交付 Foundation 由 `TURN-T01..T04` 补债。只有 `PARENT SOURCE REVIEW + PARENT TEST
> REVIEW + NAMED TESTS exit 0 + compile exit 0` 才能批准卡片。实施仍暂停，未运行测试或应用。

> 2026-07-15 全卡两轮计划审计：implementation 已暂停，旧“TURN-13H 后直接重发 14/15/16”不再是完整
> 领取条件。权威前置链改为 `TURN-02R -> TURN-13G -> TURN-13H -> TURN-13C`；只有 Cloud command result
> 能把 exact PNG bytes 交回原 Task、统一 gateway/actionId 和 turn-native Task context 均可构造后，业务卡才
> 进入 READY。Task 启动另由 `TURN-40A/B/C/D` 闭合 ordered queue/start ack/Cloud runtime/双端 lifecycle。
> 三大 Task 完成后再清 old context/facade；删除固定为 DHXY `43A -> 42A -> 43B`、Cloud
> `45A -> 44A -> 45B`。历史 `189/407` 继续仅作旧 caller 防重/查漏快照，不是 runtime ledger 或主进度。
> 完整卡关系以 `2026-07-15-https-turn-complete-migration-card-plan.md` 第 14..18 节为准。

> 2026-07-15 17:08 EDT：`TURN-14/15/16` 均已真实领取并返回同一 write-set prerequisite；父级独立源码审查
> `P0/P1/P2=0/1/0 / BLOCKED`。真实 `/turn` exchange 尚未作为同源 capability 注入 dormant
> `CloudServiceHost`，三卡不得创建第二 exchange、stub 或 source-only facade。新增 `TURN-13H` 惰性 host wiring
> 前置；其通过后原样并行重发三卡，用户可见 activation 仍只在 TURN-40。

> 2026-07-15 17:01 EDT：`TURN-13 Repair #1` 父级复审 `P0/P1/P2=0/0/0 / SOURCE APPROVED`；
> missing/shutdown/running exact runner 三门闭合，DHXY compile exit 0。Cloud clean compile 仍被写集外 legacy
> whole Service/Task 的 DHXY-only 类型引用阻断，BUILD cohort 留待后续 cutover；TURN-14/15/16 后续因共同
> command-capability prerequisite 转为 BLOCKED。

> 2026-07-15 16:55 EDT：`TURN-13` 首次源码交付父级审查为 `P0/P1/P2=0/1/2 / REPAIR #1`。
> inert wiring 与 local submit 互斥成立，但 remote start 会接受不存在或已 shutdown 的 exact runner；原 Curie
> 仅在 `TurnModeGuard.java` 补 registered/open/idle 三门。Cloud `-DskipTests` package 被 enforcer 拒绝，尚无
> Foundation build evidence。

> 2026-07-15 16:38 EDT：`TURN-12 Repair #1` 父级复审 `P0/P1/P2=0/0/0 / SOURCE APPROVED`；
> start/stop 与 permanent retire/remove 两个竞态关闭，旧 ACK/previous/actionId cache 合同未漂移。Foundation 至
> TURN-12 共 23 张已关闭或源码批准，已冻结并续派 TURN-13 exact integration brief。

> 2026-07-15 16:32 EDT：`TURN-12` 首次父级源码审查 `P0/P1/P2=0/2/0 / REPAIR #1`。ACK、previous
> retention 与 actionId cache 主链成立，但 start/stop 竞态可丢显式 stop，remove 后旧 loop 可重启并与新 loop
> 同窗并跑；已退回原 Internal 三文件修复。Helper-R1 `PRECHECK_CLEAR` 仅为非绑定证据，不构成批准。

> 2026-07-15 16:19 EDT：`TURN-11 Repair #1` 父级复审 `P0/P1/P2=0/0/0 / SOURCE APPROVED`；
> exact-window context 与 failure-frame replacement 两项 P1 均关闭。Foundation 当前 22 张已关闭或源码批准，
> 原 Internal lane 已立即续派 `TURN-12`，主进度继续按卡片状态报告。

> 2026-07-15 16:15 EDT：`TURN-11` 首次交付父级审查为 `P0/P1/P2=0/2/0 / REPAIR #1`。LOCAL_SERVICE
> 未在 action exact-window context 下调用，且 failure-evidence capture 异常会泄漏 prior success frame；已退回
> 原 Internal 仅在 TURN-11 写集修复。当前主进度继续按卡片状态报告，不使用旧覆盖快照作 headline。

> 2026-07-15 16:12 EDT：用户批准进度口径纠偏。历史 `189/407` 降级为旧 caller 覆盖审计快照，
> 不是 HTTPS turn 运行时 ledger，也不再作为 heartbeat/CR271 主进度。当前按卡片报告：Foundation 至
> `TURN-10E` 共 21 张已关闭或源码批准，`TURN-11` 实施中；本矩阵只在后续 caller cutover 时用于防重和查漏。

> 2026-07-15 16:10 EDT：`TURN-10E` dispatcher 已父级源码通过：Bag/Give 各一次 exclusive，UI/Quest
> queue 外调用，无重跑/fallback/第五 Service。依赖解锁后已立即续派四文件 `TURN-11` action executor；hard
> coverage snapshot 当时仍记录为 `189/407`，现已按上条降级为历史审计项。

> 2026-07-15 16:09 EDT：`TURN-10C` 与 `TURN-10D`（含 absolute origin 前置）均已父级源码通过；Quest
> capture 同帧只产生一个 truthful absolute `QUEST_DETAIL` frame。四 adapter 已解锁 `TURN-10E` dispatcher 并
> 立即由原 Internal lane 领取。hard ledger 仍 `189/407`。

> 2026-07-15 16:00 EDT：`TURN-10B Repair #1` 混合 queue ownership 已闭合并父级源码通过；等待
> `TURN-10C/10D` 后解锁 dispatcher。hard ledger 仍 `189/407`。

> 2026-07-15 15:58 EDT：`TURN-03B/05` 上游 auth P1 已关闭并源码通过；`TURN-10B` 因混合 queue
> ownership 退回 Repair #1；Internal 已续派 `TURN-10C/10D`。均为 `countDelta=0`，hard ledger `189/407`。

> 2026-07-15 15:55 EDT：`TURN-08B/10A` 已父级源码通过；`TURN-05` 因上游 template handler 重复
> Authorization P1 退回 `TURN-03B Repair #2`；External `TURN-10B` 路径歧义已纠正并恢复实施。
> 全部仍为 `countDelta=0`，hard ledger `189/407` 不变。

> 2026-07-15 15:50 EDT：`TURN-01D Repair #1` 已由父级源码复审通过，P1 关闭并进入 build cohort；
> External 林明随即续派 `TURN-10B`。当前 Internal `TURN-08B/05/10A` 加 External `TURN-10B` 的写集互斥，
> 均为 `countDelta=0`，hard ledger 保持 `189/407`。

> 2026-07-15 15:47 EDT：HTTPS turn Foundation 的 `TURN-04` 与 `TURN-10P` Repair #1 已由父级源码通过，
> `TURN-01D` validator 因 outcome step-result fail-closed 不完整退回 External 林明 Repair #1。Internal 当前
> 三条互斥线为 `TURN-08B/05/10A`；均是 `countDelta=0` 基础设施卡，hard ledger 保持 `189/407`，
> writers 活动中未运行 Maven。

> 2026-07-15 10:29 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十七领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十八窗，`claimBy=10:49:25`；B 第十七窗截止 `10:36:57`，C/D 已领取在途。

> 2026-07-15 10:16 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十六领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十七窗，`claimBy=10:36:57`；A 第十七窗截止 `10:29:20`，C/D 已领取在途。

> 2026-07-15 10:09 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十六领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十七窗，`claimBy=10:29:20`；B 第十六窗截止 `10:16:52`，C/D 已领取在途。

> 2026-07-15 09:56 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十五领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十六窗，`claimBy=10:16:52`；A 第十六窗截止 `10:09:14`，C/D 已领取在途。

> 2026-07-15 09:49 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十五领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十六窗，`claimBy=10:09:14`；B 第十五窗截止 `09:56:47`，C/D 已领取在途。

> 2026-07-15 09:36 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十四领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十五窗，`claimBy=09:56:47`；A 第十五窗截止 `09:49:09`，C/D 已领取在途。

> 2026-07-15 09:29 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十四领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十五窗，`claimBy=09:49:09`；B 第十四窗截止 `09:36:42`，C/D 已领取在途。

> 2026-07-15 09:16 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十三领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十四窗，`claimBy=09:36:42`；A 第十四窗截止 `09:29:04`，C/D 已领取在途。

> 2026-07-15 09:09 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十三领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十四窗，`claimBy=09:29:04`；B 第十三窗截止 `09:16:37`，C/D 已领取在途。

> 2026-07-15 08:56 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十二领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十三窗，`claimBy=09:16:37`；A 第十三窗截止 `09:08:29`，C/D 已领取在途。

> 2026-07-15 08:48 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十二领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十三窗，`claimBy=09:08:29`；B 第十二窗截止 `08:55:55`，C/D 已领取在途。

> 2026-07-15 08:35 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十一领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十二窗，`claimBy=08:55:55`；A 第十二窗截止 `08:47:52`，C/D 已领取在途。

> 2026-07-15 08:27 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十一领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十二窗，`claimBy=08:47:52`；B 第十一窗截止 `08:35:23`，C/D 已领取在途。

> 2026-07-15 08:15 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第十领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十一窗，`claimBy=08:35:23`；A 第十一窗截止 `08:27:09`，C/D 已领取在途。

> 2026-07-15 08:07 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第十领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十一窗，`claimBy=08:27:09`；B 第十窗截止 `08:14:43`，C/D 已领取在途。

> 2026-07-15 07:54 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第九领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第十窗，`claimBy=08:14:43`；A 第十窗截止 `08:06:18`，C/D 已领取在途。

> 2026-07-15 07:46 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第九领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第十窗，`claimBy=08:06:18`；B 第九窗截止 `07:54:02`，C/D 已领取在途。

> 2026-07-15 07:34 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第八领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第九窗，`claimBy=07:54:02`；A 第九窗截止 `07:45:43`，C/D 已领取在途。

> 2026-07-15 07:25 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第八领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第九窗，`claimBy=07:45:43`；B 第八窗截止 `07:33:35`，C/D 已领取在途。

> 2026-07-15 07:13 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第七领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第八窗，`claimBy=07:33:35`；A 第八窗截止 `07:25:05`，C/D 已领取在途。

> 2026-07-15 07:05 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第七领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第八窗，`claimBy=07:25:05`；B 第七窗截止 `07:12:40`，C/D 已领取在途。

> 2026-07-15 06:52 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第六领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第七窗，`claimBy=07:12:40`；A 第七窗截止 `07:04:40`，C/D 已领取在途。

> 2026-07-15 06:44 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第六领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第七窗，`claimBy=07:04:40`；B 第六窗截止 `06:52:10`，C/D 已领取在途。

> 2026-07-15 06:32 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第五领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第六窗，`claimBy=06:52:10`；A 第六窗截止 `06:44:10`，C/D 已领取在途。

> 2026-07-15 06:24 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第五领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第六窗，`claimBy=06:44:10`；B 第五窗截止 `06:31:35`，C/D 已领取在途。

> 2026-07-15 06:11 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第四领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第五窗，`claimBy=06:31:35`；A 第五窗截止 `06:23:40`，C/D 已领取在途。

> 2026-07-15 06:03 EDT：hard ledger `189/407`、去重待构建池 `53`。A 的
> `AutoBattleTask::isFollowerSupportMode` 第四领取窗到期仍无 true EOF `CLAIMED`，已只向原 A
> 原样开启第五窗，`claimBy=06:23:40`；B 第四窗截止 `06:11:10`，C/D 已领取在途。

> 2026-07-15 05:51 EDT：hard ledger `189/407`、去重待构建池 `53`。B 的
> `ObjectiveTextRecognitionService::recognize(raw,source)` 第三领取窗到期仍无 true EOF `CLAIMED`，
> 已只向原 B 原样开启第四窗，`claimBy=06:11:10`；A 第四窗截止 `06:03:15`，C/D 已领取在途。

> 2026-07-15 05:39 EDT：hard ledger `189/407`，去重待构建池 `53`。新增 source-approved 为
> `WorldMapRouteResultMemoryService::recordAbandoned` 的 active Navigation second-navigation 整链；
> `PlayerStateService::syncMyIdentity` 因缺 active `syncAll` caller 与 exact-context `me` projection 保持
> `countDelta=0`。H7 返回 `NONE`：当前剩余近似项均不通过 runnable/public/de-dup/互斥硬门。A 第三窗未领后
> 第四次原样重发；B 第三窗、C/D 在途。

> 2026-07-15 05:34 EDT：hard ledger `189/407`，去重待构建池 `52`。新增 source-approved 为
> TaskMaintenance start initialize 与 world-map route failure；BattleRadar expected-exit consume 因唯一 caller
> 仍在 non-compiling whole Task 保持 `countDelta=0`。Internal 当前 identity sync、route abandoned；A/B 第三领取窗，
> C/D 在途。writers 稳定前不跑 Maven。

> 2026-07-15 05:18 EDT：hard ledger `189/407`，去重待构建池 `50`。I40/I41/I42 因不可达后基线
> helper/缺 typed event producer/重复 UI cleanup owner 均 `countDelta=0`；内部三线已换发 I43
> TaskMaintenance initialize、I44 BattleRadar expected-exit consume、I45R world-map route failure。重复的
> AutoPanel ensure-visible 已在领取前纠正，因为 I7 已 source-approved；writer 稳定前不跑 Maven。

> 2026-07-15 04:13 EDT：hard ledger 仍 `189/407`；去重后 `44` 个 count unit 为 `SOURCE APPROVED /
> COUNT PENDING BUILD`。新增放行为 I22 role-toggle、A combat-enter、D repaired smart-click sourceTask、I23
> panel alignment、I25 left-top state、I27 ultimate-corner。External 当前 A poll interval、B incense whole、
> C TaskTracker 29-Java whole read、D AutoCombat initialize；Internal I30 world-map clean memory、I31 radar
> state transition、I32 summon tail direct。重复/helper、无 caller、缺 closed transport 的 I24/I26/I28/I29
> 均未计数。

> 2026-07-15 03:42 EDT：hard ledger `189/407`；去重后 `38` 个 count unit 为 `SOURCE APPROVED /
> COUNT PENDING BUILD`。D smart-click proof 因 pending 未保存/校验 sourceTask 被父级撤销假通过并交原 D R1；
> I18 的未批准空步骤 lifecycle 改动已撤销后按 baseline 放行，I20 CommonBox role-wide clear 放行。当前七条
> writer 为 A AutoCombat enter、B incense whole、C TaskTracker 29-Java whole read、D smart-click R1，Internal
> checkFiveRing/CommonBox role-toggle/AutoPanel align。

> 2026-07-15 03:10 EDT：hard ledger `189/407`；32 个去重 count unit 为 `SOURCE APPROVED / COUNT PENDING
> BUILD`。新增放行 BattleRadar dynamic polling（真实方法返回 4s/2s/10s，父级 brief 的 1s 已纠正）、Navigation
> current-map closed macro、AutoCombat exit recovery、AutoCombatPanel typed refresh、TaskMaintenance summon
> due/cache。AutoBattle return-self-check 因 TaskMaintenance CR244 coordination owner 缺失 `BLOCKED P1=1/countDelta=0`。

> 2026-07-15 03:03 EDT：hard ledger `189/407`；新增 AutoCombat combat-maintenance 后，27 个去重 count unit
> `SOURCE APPROVED / COUNT PENDING BUILD`。TaskTracker whole read 首轮因 Cloud 与 DHXY 双端 dormant
> `BLOCKED P1=2/countDelta=0`，父级已把同一 `+1` 单扩成完整 21-Java 双仓 scope，禁止拆成 codec/mechanics 零计数前置。

> 2026-07-15 02:47 EDT：hard ledger 仍 `189/407`；去重后 26 个完整 count unit 为
> `SOURCE APPROVED / COUNT PENDING BUILD`。新增放行为 AutoCombat follower first-aid、AutoCombatPanel record-exit；
> BattleRadar stale-discard 与 TaskMaintenance private maybeClean 分别因不属于独立矩阵行、与 opportunistic maintenance
> 同 bullet 而 `COUNT BOUNDARY BLOCKED/countDelta=0`。当前 External 为 BattleRadar dynamic polling、Navigation
> current-map macro、TaskTracker whole read、AutoCombat combat-maintenance；writer 稳定前不运行 Maven。

> 2026-07-15 02:33 EDT：hard ledger 仍 `189/407`；24 个互不重复整链为
> `SOURCE APPROVED / COUNT PENDING BUILD`。父级新增重复计数门：已批准 public caller chain 内的 private helper/policy
> 不能再次 `+1`；因此 `CommonBoxService::detectBox` 与 `LeftTopStatusSwitchService::resolveTaskCode` 均
> `COUNT BOUNDARY BLOCKED P1=1/countDelta=0`。当前七线改为 External BattleRadar stale-exit、Navigation current-map
> active macro、TaskTracker whole read、AutoCombat follower first-aid，以及 Internal CommonBox review closure、
> TaskMaintenance summon-clean、AutoCombatPanel record-exit。所有 writer 稳定前不运行 Maven。

> 2026-07-15 01:55 EDT：hard ledger 仍 `189/407`；19 个真实 caller -> Cloud Service -> typed DHXY
> mechanics -> closed terminal 单位已父级 `SOURCE APPROVED / COUNT PENDING BUILD`。最新 6 项为 MapName
> canonicalize、Dialog prepared-validation/detect-type、BattleRadar baseline-refresh、LeftTop follower-safe、
> CommonBox has-pending。当前七条实现线为 External A BattleRadar expected-exit consume、B Dialog green-template、
> C LeftTop supported-task、D AutoCombat pending CommonBox，Internal I6 cached-first-aid、I7 panel ensure-visible、
> I9 CommonBox detectBox。TaskMaintenance capability gate 因无 active session/role/capability producer 精确阻塞，
> 不计数、不猜测放行。所有 Java writer 稳定前不运行 Maven。

> 2026-07-15 00:58 EDT：hard count ledger 仍 `189/407`。source-approved / pending-build：
> `TaskMaintenanceService::runOpportunisticMaintenance`、`SummonSkillService::cleanSummonSkillsOnce`、
> `BattleRadarService::checkAndSyncCombatState`、`LeftTopStatusSwitchService::checkAndMaybeClose`、
> `CommonBoxService::consumePendingBoxIfAllowed`。CommonBox 生产 bean 图已由原 I1 精确 import 修复并父级通过；
> TeamReturn wait/precheck 因 caller/async 时序 `BLOCKED P1=2`；
> NpcClick 因专属 port 尚未进入 shared allowlist/codec/handler `BLOCKED P1=1`。External 当前 A AutoCombatPanel、
> B Dialog handle、C AutoCombat、D Navigation；Internal 当前 I1 CommonBox repair、I4 Player first-aid、I6
> ClientIdentity；I1 已转入可达的 TeamReturn member click count unit，七条实现线无内部接管、无空槽。所有 writer
> 稳定前不运行 Maven，不提前增加 ledger。

> **AUTHORITATIVE EXECUTION OVERRIDE — 2026-07-14:** 用户选择 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 作为迁云前完整源码基线。严格顺序为：先把 32 个 `service/**` 文件完整原样放入 Cloud，再从 active Cloud 删除用户指定永久本地的 `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService`，再依据编译错误补最小 Cloud -> local typed boundary，最后统一抽离其余 Service 的本地 mechanics。禁止在整类复制阶段同时拆动作。本文既有方法级条目只作依赖/边界索引，不再作为完成计数依据；Runner 不迁 Cloud。
>
> 完整证据镜像已落在 `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\**`：`32/32` 文件、`1,110,791` bytes、Git blob equality `BAD=0`、Runner `0`。镜像不参与 Maven 编译，也不计 active Service 完成。
> active whole-Service promotion 已父级复核 `32/32 exact`；随后只从 active Cloud 删除永久本地的 `BagService`、`UICleanerService`、`GiveItemService`、`QuestManagerService`，当前为 `28 exact / 4 expected-missing / 0 divergent`，证据镜像仍完整保留 32 文件。
> 2026-07-15 00:42 EDT：七条 `countDelta=+1` 实现线全开。External A/B/C/D 已分别持有 BattleRadar、Dialog、PlayerState、Navigation 整链；Internal 活动槽为 I2 TeamReturn、I4 LeftTop、I5 SummonSkill。I3 TaskMaintenance 与 I1 CommonBox 已父级 `SOURCE APPROVED，P0/P1/P2=0` 并等待统一构建；I1 保留完整 30s TTL、role/window/identity/taskRun 陈旧闸、成功才清 pending 与单原子 InputBundle。ledger 仍为 `189/407`，fresh Maven 双门通过才按批准顺序原子递增。
>
> 2026-07-15 00:12 EDT：A/C/D NPC 专用合同首版经 helper 非绑定预检和父级独立源码审查后均已原 Worker 返修并领取。A `BLOCKED P1=1`，仅恢复 tooltip null/blank/empty 的基线 closed-terminal 接受域；C `BLOCKED P1=1/P2=1`，删除 reachable throwing stub 并恢复两侧 PNG decode/actual dimensions/SHA invariant；D `BLOCKED P1=2/P2=1`，撤销未注册 sealed/kind 与 Cloud→DHXY mechanics 源码依赖，改真正 standalone contract 和纯 port interface。B 已采纳 white-story 19 Java amendment，继续完整 caller→typed transport→handler→mechanics→terminal 链。四路均在途，不运行 Maven；专用合同不计整链，approved same-path 仍为 `189/407`。
> 2026-07-14 23:25 EDT：C same-frame option OCR R1 经 helper 非绑定预检与父级独立复核仍 `BLOCKED，P0=0/P1=3/P2=1`。supplied SHA request identity、`SUPPLIED/FRESH_AT_RECT/FRESH_DEFAULT` 与 RAW/green/yellow caller 主路由已形成；剩余阻断是 Cloud port 对 optional null 变体无条件算 SHA、local wash 运行异常未按 696 基线降为 raw/保-green、supplied SHA 门仍在 mechanics 内而未前移到 handler，另有 public contract 注释过时。原 C 独占 shared slot 做 R2，截止 `23:45:42`；Queue #26 已备妥 B white-story -> D player-anchor caller -> A tooltip caller 完整链。不运行 Maven，approved same-path 保持 `189/407`。
> 2026-07-14 22:54 EDT：C 完整 same-frame option OCR 双仓链经 helper 非绑定预检与父级独立源码对照后 `BLOCKED，P0=0/P1=3/P2=0`：supplied frame bytes 无 request SHA；三图全成功合同丢失 696 的 raw OCR 与 yellow-wash 失败保留 green words；无 supplied image 时 detection rect 被丢弃并改抓默认大框。原 C 独占 shared slot 做 R1，一次补 SHA、closed partial availability/RAW pass 与 rect-only exact fresh capture；B white-story、D player-anchor caller、A tooltip caller 继续按序等待。不运行 Maven，approved same-path 保持 `189/407`。
> 2026-07-14 22:15 EDT：B prepared-action R2 已父级 `SOURCE APPROVED，P0/P1/P2=0`，完整 white-story 链等待 shared slot。C OCR words R1 已通过并于 22:04 领取完整 same-frame option OCR 双仓 caller chain，当前独占 generic enum/codec/digest/handler 写集。A tooltip 因 empty-match 绕过 post-capture binding 重验而 `BLOCKED，P1=1`，A 已于 22:13:52 领取同文件 R2。D player-anchor R1 已闭合全部 P1，只余 try 前 native acquisition 与 exclusive scanRect 上界两个 P2，原 D 同文件 R2 截止 22:35:36。writers 活动，不运行 Maven，计数仍 `189/407`。
> 2026-07-14 21:47 EDT：D yellow-target R2 已父级 `SOURCE APPROVED，P0/P1/P2=0`，masked-copy 异常/成功 owner handoff 闭合，已续派完整 player-anchor observation。B prepared-action R1 的 refresh/handle、8/16、三层 measured invariant 通过，但 null washMode 仍被 frozen codec/digest 提前拦截，父级判 `BLOCKED，P1=1` 并改为 Cloud caller 规范化 null -> TEMPLATE_SPECIFIC 的三文件 R2。C OCR words 因五态异常出口、null variant、strict PNG/long rect 未闭合，`BLOCKED，P1=1/P2=2`，原 C 同文件 R1。A tooltip 在途；计数仍 `189/407`。
> 2026-07-14 21:31 EDT：A prepared-point R1 已父级 `SOURCE APPROVED，P0/P1/P2=0`，0/1 retry 与 terminal/clickProduced 自证闭合，已立即续派完整 task-tooltip continuous macro。D yellow-target 四项主返修均通过，仅 masked-copy helper 异常出口存在 owner P2，原 D 同文件 R2。B 20 文件 Dialog prepared-action validation 主链、四个 exact add-back、key/digest parity 与正常 8/16 路径成立，但 refresh runtime/handle gate 与 null washMode/closed DTO 接受域仍 `BLOCKED，P1=2/P2=2`，原 B 五文件 R1。C option OCR words 在途；计数仍 `189/407`。
> 2026-07-14 20:57 EDT：A R2 的 frame/rect iff 与 validation finally 已通过，但 supplied decode/dimension RuntimeException 仍可在 raw-owner finally 前逸出，父级判 `BLOCKED，P2=1` 并退原 A 同文件 R3。C R2 增加同帧权威：`DialogDetection` 不结构性绑定 rawPath/image，故 supplied/fallback 都从唯一 frameImage 物化 window-scoped raw 后再 wash/match。B 20 文件真链与 D yellow-target 完整 mechanics 继续，计数仍 `189/407`。
> 2026-07-14 20:49 EDT：A/C R1 父级复审均仍 `BLOCKED，P1=1/P2=1`。A supplied intent 仍允许 rect-only 后静默 fresh capture，validation decode owner 未 finally；C MATCHED evidence 未重算 SHA/绑定 PNG-dimensions-rect-相对/绝对点，fresh collaborators 异常可逸出 closed entry。原 A/C 各自只做同类窄 R2；B/D 完整链继续，计数仍 `189/407`。
> 2026-07-14 20:43 EDT：D 的 NPC yellow-target mechanics 零 Java prerequisite 已由父级定源，不进入新 Design。纯本地 shape-candidate 权威为 Git 中 `696a12b0:GameTextLineOcrService.findYellowTextCandidateResult` 的 strict-yellow mask、shadow、connected-component、line/gap、score/sort 闭包；输入必须保留同次 capture 的 baseline default-window mask/`skipDefaultMask` 分支。禁止调用 Cloud `CloudImageProcessor`，禁止以当前 yellow wash 替代候选算法。原 D 同一单文件任务继续，Cloud 保留 NPC OCR/target/click/verify/fallback；计数仍 `189/407`。
> 2026-07-14 20:36 EDT：A 的 Dialog option OCR image whole observation 首版父级 `BLOCKED，P1=1/P2=3`。首 pass 错用同时保留 green/highlighted-yellow 的 template wash，偏离 696 的 green-only OCR；三 PNG/rect/dimensions/hash authority、non-CAPTURED exact shape 与 refresh exception terminal 尚未闭合。R1 仍由原 A 只修同一大类；B `ImagePreprocessor` 写集冻结不触。A/C 各一返修、B/D 完整链在途，计数仍 `189/407`。
> 2026-07-14 20:31 EDT：C 的 white-story template whole observation 首版经父级对照 `696a12b0` 判 `BLOCKED，P1=3/P2=2`。缺失 no-supplied/unusable-supplied 的单次 fresh dialog detection fallback，有效 supplied frame 前新增 binding 门，nullable template name 命中被改成 failure，且 typed result/Mat owner 未闭合。R1 仍由原 C 只修同一大类，复用现有 `DialogDetectionLocalMechanics`，不拆 wire/owner/设计轮。A/B/D 各自完整 mechanics/双端链继续在途；计数保持 `189/407`，writers 稳定前不运行 Maven。
> 2026-07-14 14:30 EDT：LeftTop、CommonBox、TeamReturn member-button 当前源码均父级 `P0/P1/P2=0`。fresh Cloud package 的 compile 失败已收敛为 7 个 Service：BattleRadar、ClientIdentity、Dialog、Navigation、NpcClick、PlayerState、TaskTrackerPanel；计数仍 `189/407`。父级已把其中四个可并行整类边界在真实 EOF 发给 External A/B/C/D：A BattleRadar、B NpcClick、C PlayerState、D Dialog，统一领取截止 14:48；TaskTracker context/capture 前置、ClientIdentity 用户决策与 Navigation X2 closed macro 单独保留，不伪造完成。
> Phase 3 首个完整边界 `UI_CLEAN` 已四路父级源码通过：A 三个 Service 的 8 caller、B DHXY handler、C Cloud contract/facade/wire、D DHXY DTO/codec/digest 均 `P0/P1/P2=0`，跨端 canonical parity 已批准。fresh DHXY compile exit 0；fresh Cloud package 在 compile 阶段仍因 20 个 desktop/window/capture/OCR/input collaborator、`BagService`/`GiveItemService` 类型与 `BotProperties` 依赖闭包而 exit 1，未出现 UI_CLEAN 错误。approved same-path 继续 `189/407`。
> 2026-07-14 起，内部两个槽位改为流水线 helper：一个对 External A/B/C/D 新交付做非绑定预检并列候选风险，另一个提前维护四个互斥下一实现单；父级仍是唯一 manager/reviewer 和最终 `APPROVED/BLOCKED` 来源。helper 不计完成、不替代源码/构建门，也不擅自决定本地/Cloud 有歧义的落点。
> Phase 3 首批后续闭包已父级源码通过：POM 的基线 validation/annotation 依赖、`CloudBagUseIncensePort` 与 byte-exact `SheyaoxiangDigitTemplateReader`。后续 fresh Cloud package 已不再报告 `BotProperties` 或 digit-template 缺失，但仍因 desktop/window/capture/OCR/input collaborator 与 `BagService`/`GiveItemService` caller type 失败；整体构建门仍开放，计数仍为 `189/407`。
> 首轮 helper 预检经父级独立复核后新增 Phase 4 集成门：`NavigationService` 三个 X2 caller 位于 baseline exclusive-input callback，而 DHXY `UI_CLEAN` X2 handler 会再次进入 remote exclusive queue；这三点 `INTEGRATION BLOCKED P1=1`，须与成功后 mouse-away 及外围 direct-input 序列合成同一个 closed local macro。其余五个 UI_CLEAN caller 和双侧合同继续 source-approved，不重开 wire。下一任务 helper 的 AutoCombatPanel/TeamReturn 整类 READY 判断被父级否决并要求收窄；LeftTop/CommonBox/TaskTracker panel-rect 候选可继续。
> 父级已把修正队列真实发布给四个 External Worker：A `LeftTopStatusSwitchService` 完整 typed boundary，B 仅 `TaskTrackerPanelService` panel-rect typed boundary，C `CommonBoxService` 完整 typed observation/input boundary，D 仅 `TeamReturnService.clickReturnTeamIfPresent` member-button chain。四个发布前 active blob 均等于 `696a12b0`，写集互斥；A/B/C 领取截止 13:55 EDT，D 真实 EOF 重发截止 13:56 EDT。20 分钟只查 CLAIMED，任务绝不内部接管。
> 领取实时证据：A 13:36:03、C 13:36:55、D 13:37:13、B 13:40:00 均已在截止前 CLAIMED。领取只证明写集所有权，不以 20 分钟检查交付完成；截至 13:41:36 四路尚无本轮 Implementation/Repair，继续等待真实 EOF 新材料，不提前构建。
> B 于 13:44:04 报告 panel-rect 前置不可达且 Java 零改动；父级独立确认 `TaskTrackerPanelService` 当前调用图没有 fact/context 输入，`WINDOW_CLIENT_PX` 几何也不能单独替代 baseline 的 screen-absolute captured artifact。原 B 单已 `CURRENT TASK SUPERSEDED / PARENT PREREQUISITE BLOCKED，P1=1/P2=1`，不算 B 实现缺陷、不改整类计数；正确闭包须先有 caller-reachable typed panel observation/artifact，算法仍留 Cloud。
> 2026-07-14 15:59 EDT：DHXY BattleRadar R1 与 TaskTracker drag+same-call panel capture 已父级 `SOURCE APPROVED，P0/P1/P2=0`；Npc Ctrl-probe mechanics 因缺 exact binding、用 Java callback 冒充 closed intent、payload 可变且非 Spring bean，被父级 `BLOCKED，P1=2/P2=2` 并交原 A 单文件返修。PlayerState first-aid 的两条 baseline 路径保持，但 helper 提示后父级独立确认 `HealOutcome:525-531` 允许单边 click 坐标，纠正为 `BLOCKED，P2=1` 并让原 C 先做 compact-constructor-only R2，incense Java 暂停。B Dialog detection、D story advance 已领取；C/D 首次 brief 落入旧历史段后已在物理 EOF 权威重发。上述仍是 Phase 3/4 prerequisite，不增加 `189/407`，fresh 双构建等待新 writer 稳定。

工件编号：**A-1**（终审 Final #1 工件计划 / 方法级底账实现物）
来源共识：**§12.1**（Q1 六项验收）、Final #1、A-3 v3（tier 与归属术语）
基线锚定：**navigation-migration@4a116bde**（本轮方法级扫描的代码基线；批量提交后以此 commit 冻结为 inventory 种子）
诚实声明：本文是**方法级底账的机械化汇总产物**——覆盖 §12.1 REQ-M-* 的「方法级 inventory + tier 标注 + 隐式状态穷举」维度，来源为按业务包 fan-out 的逐类扫描 JSON。它**已越过类级骨架**，但仍非 Q1 完整 PASS：反向静态扫描（零业务语义命中）的**机械化规则实现与 allowlist 构建证据**尚未落地，本文仅给出**反向扫描候选清单**（见 §1.2）作为该规则的输入种子。继承/lambda/监听器/条件注册的字节码级闭包校验（javap/ASM）仍属实施期工作。

> 统计口径：共 **191** 类 / **450** 个 keyMethods（安全/业务决策关键方法，非全量方法）/ **337** 条隐式状态；按业务包分 **11** 节。

---

## 0. Q1 六项验收对照表

| # | 验收项 | 状态 | 本工件覆盖说明 |
|---|---|---|---|
| 1 | 方法级 inventory 全覆盖 | **PARTIAL_COVERED** | 本轮已覆盖 **191 类 / 450 keyMethods**，逐类含 tier+role+权威归属；全量私有/lambda/继承方法闭包仍需 ASM 扫描 |
| 2 | 入口可达闭包无未知节点 | NOT_EVALUATED | 需从 Runner/TaskFactory 入口做可达性闭包，本工件按包枚举非按可达图 |
| 3 | 配置/资源零未归属 | PARTIAL | 隐式状态清单（每节后）已穷举 timer/memory/cache/lock/fallback/other 共 337 条；resources 树（模板/JSON/别名字典）尚未逐文件归属 |
| 4 | Thin Client 产物 allowlist | NOT_EVALUATED | 有 local-non-xiuluo-brain profile 先例；本工件的 tier=D + localRetained=无 集合可作 allowlist 候选 |
| 5 | 反向扫描零业务语义命中 | **CANDIDATE_LISTED** | §1.2 列出 **56** 个 tier=D 但含游戏语义字符串/枚举的可疑类作为反向扫描输入；扫描规则机械化实现待建 |
| 6 | 人工按业务流反向抽查 | NOT_EVALUATED | 留实施期按修罗/五倍/五环/自动战斗四流手工走查 |

## 1. 全库 tier 分布统计

tier 定义：**A**=状态/协议/身份/lease/输入/stop-pause 安全层；**B**=影响 phase/动作/retry/fallback/timeout/memory 的业务决策；**C**=视觉解释/OCR/模板；**D**=纯搬运/DTO/枚举/契约。

### 1.1 类级 & 方法级分布

| tier | 类数 | keyMethods 数 |
|---|---|---|
| A | 44 | 133 |
| B | 32 | 200 |
| C | 22 | 62 |
| D | 93 | 55 |
| **合计** | **191** | **450** |

隐式状态按 kind 分布（全库 337 条）：

| kind | 条数 |
|---|---|
| fallback | 98 |
| timer | 63 |
| memory | 58 |
| other | 57 |
| cache | 38 |
| lock | 23 |

### 1.2 反向扫描候选（tier=D 但含游戏语义字符串/枚举）

规则种子：一个类被标为 tier=D（应为纯搬运/DTO/枚举）却**携带隐式状态**或 role/note/detail 中**命中游戏语义关键词**（fallback/降级/语义/策略/reroll/黄袍/摄妖香/看打/归队/绿字 …），即为反向静态扫描的重点复核对象——须证明其无本地业务分支（否则是伪装成 DTO 的本地大脑残留）。命中 **56** 个：

| className | 业务包 | 命中原因 |
|---|---|---|
| `DefaultTaskFactory` | task/(直下) + task/hotstart + | 含隐式状态[other] |
| `TaskStartupCheckResult` | task/(直下) + task/hotstart + | 游戏语义:STOP/FAILED |
| `TaskStepResult` | task/(直下) + task/hotstart + | 游戏语义:STOP/FAILED |
| `TaskYieldPolicy` | task/(直下) + task/hotstart + | 游戏语义:STOP |
| `TaskTransactionOutcome` | task/(直下) + task/hotstart + | 游戏语义:yield |
| `XiuluoWaitSpec` | task/xiuluo (修罗 XiuluoTaskV2 | 含隐式状态[other]；游戏语义:语义/策略/pathing |
| `XiuluoRouteMode` | task/xiuluo (修罗 XiuluoTaskV2 | 含隐式状态[other]；游戏语义:combat/route/routeMode |
| `XiuluoCombatSource` | task/xiuluo (修罗 XiuluoTaskV2 | 含隐式状态[other]；游戏语义:看打/combat/combatSource |
| `XiuluoStepOutcome` | task/xiuluo (修罗 XiuluoTaskV2 | 含隐式状态[other]；游戏语义:combat/yield/STOP/FAILED |
| `XiuluoDialogCatalog` | task/xiuluo (修罗 XiuluoTaskV2 | 含隐式状态[other]；游戏语义:看打/对话 |
| `DialogHandleRequest` | service/ A–M (THIN_CLIENT_V1 | 含隐式状态[other]；游戏语义:fallback/策略/对话 |
| `DialogOptionClickResult` | service/ A–M (THIN_CLIENT_V1 | 游戏语义:对话 |
| `DialogOperation` | service/ A–M (THIN_CLIENT_V1 | 游戏语义:对话 |
| `DialogOptionPolicy` | service/ A–M (THIN_CLIENT_V1 | 游戏语义:策略 |
| `DialogStoryPolicy` | service/ A–M (THIN_CLIENT_V1 | 游戏语义:策略 |
| `DialogFallbackPolicy` | service/ A–M (THIN_CLIENT_V1 | 含隐式状态[fallback]；游戏语义:语义/策略/对话 |
| `DialogOperation (enum)` | service (N–Z) + service/dial | 游戏语义:对话 |
| `DialogOptionPolicy (enum)` | service (N–Z) + service/dial | 游戏语义:策略 |
| `DialogStoryPolicy (enum)` | service (N–Z) + service/dial | 游戏语义:策略/对话 |
| `DialogFallbackPolicy (enum)` | service (N–Z) + service/dial | 游戏语义:策略/对话 |
| `DialogHandleRequest (DTO/builder)` | service (N–Z) + service/dial | 游戏语义:fallback/策略/对话 |
| `DialogOptionClickResult (DTO)` | service (N–Z) + service/dial | 游戏语义:对话 |
| `model/navigation/NavigationRequest` | vision/ (全部) + model/navigat | 含隐式状态[other]；游戏语义:语义 |
| `model/navigation/TemplateLocationInfo` | vision/ (全部) + model/navigat | 含隐式状态[other] |
| `model/navigation/ObjectiveTextResult` | vision/ (全部) + model/navigat | 游戏语义:对话 |
| `model/navigation/PendingTransferChoiceMemory` | vision/ (全部) + model/navigat | 含隐式状态[memory]；游戏语义:语义/对话 |
| `model/navigation/PendingRouteOutcome` | vision/ (全部) + model/navigat | 含隐式状态[memory]；游戏语义:pathing/route |
| `TaskTrackerPanelSourceType` | model.tasktracker (全部) + mod | 含隐式状态[other] |
| `TaskTrackerPanelPrepareResult` | model.tasktracker (全部) + mod | 游戏语义:tracker |
| `MockCloudDecisionClient` | cloud.decision | 含隐式状态[fallback] |
| `CloudDecisionRequest` | cloud.decision | 含隐式状态[other] |
| `CloudDecisionResponse` | cloud.decision | 含隐式状态[cache]；游戏语义:fallback |
| `TrackerLinkRankerCloudDecision` | cloud/task | 游戏语义:tracker |
| `RouteMemoryOutcomeIngestResult` | cloud/task | 游戏语义:route |
| `TaskPolicyCloudDecision` | cloud/task | 含隐式状态[fallback]；游戏语义:语义/yield |
| `CapabilityGateCloudDecision` | cloud/task | 游戏语义:DENY |
| `MaintenanceThresholdCloudDecision` | cloud/task | 游戏语义:ALLOW |
| `TeamReturnPolicyCloudDecision` | cloud/task | 游戏语义:归队/DENY |
| `ImagePreprocessWashedImageClient` | cloud/task | 含隐式状态[fallback,other] |
| `DialogPolicyPreClickCloudDecision` | cloud/task | 游戏语义:对话 |
| `NpcClickSmartCloudSession` | cloud/task | 含隐式状态[memory] |
| `TrackerPanelReaderCloudDecision` | cloud/task | 游戏语义:tracker |
| `NpcClickSmartQueueOutcome` | cloud/task | 游戏语义:FAILED |
| `SheyaoxiangStatusCloudDecision` | cloud/task | 含隐式状态[timer]；游戏语义:语义 |
| `SheyaoxiangStatusCloudRequest` | cloud/task | 含隐式状态[timer] |
| `RouteMemoryOutcomeReport` | cloud/task | 游戏语义:route |
| `TrackerPanelReaderCloudRequest` | cloud/task | 游戏语义:tracker |
| `DialogPolicyPreClickCloudRequest` | cloud/task | 游戏语义:对话 |
| `RuntimeDecisionShadowService` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback,fallback,memory,other]；游戏语义:语义 |
| `XiuluoBrainStartRequest` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback] |
| `XiuluoBrainStepRequest` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback] |
| `XiuluoBrainActionOutcomeRequest` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback]；游戏语义:yield |
| `XiuluoBrainDecision` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback] |
| `XiuluoBrainActionOutcomeDecision` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback] |
| `XiuluoBrainActionType` | cloud/xiuluo + cloud/runtime | 游戏语义:STOP |
| `XiuluoBrainResponse` | cloud/xiuluo + cloud/runtime | 含隐式状态[fallback] |

---

## 2. 分业务包迁移矩阵

### 2.1 task/(直下) + task/hotstart + task/pause + task/startup + task/model + task/template + task/transaction

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `AutoBattleTask` | 单窗口后台自动战斗巡逻循环，含成员/跟随支援态维护、归队自检与动态轮询 | B | 本地 AutoBattleTask 循环持有 phase 分支、idle 维护授权与轮询节奏；下游 service 持有各自安全门 | 云端 AutoBattlePolicy/TaskPolicyService（tick 后 phase 决策、idle 维护授权、支援态判定、轮询间隔选择） | 本地仅保留：combat tick 调用触发、输入点击执行、归队标记截图探测（capture/executor） |
| `GameTask` | 统一任务接口，Runner 唯一依赖的契约（getTaskCode/getTaskName/execute/stop） | D | 本地接口契约，无状态 | N/A（契约保留，云端实现 execute 决策） | 接口本身保留本地；execute(context) default 委托 execute() |
| `SleepComputerTask` | 显式队列任务：延时后请求 Windows 睡眠（需用户主动入队，STOP_ON_FAILURE 保护） | B | 本地任务：stop 令牌检查 + 延时 + 委托 SystemPowerService 执行真实睡眠动作 | 云端任务队列决定是否调度该任务；睡眠动作本身是本机不可迁移副作用 | 本地保留 SystemPowerService.sleepComputer 系统电源执行器（真实机器动作） |
| `TaskFactory` | 按窗口上下文+TaskType 创建任务实例的工厂接口（多窗口须独立实例） | D | 本地接口契约 | N/A（本地实例装配保留；任务决策迁云端） | 工厂接口本地保留 |
| `DefaultTaskFactory` | TaskType→原型任务 Provider 的 switch 路由装配 | D | 本地 Spring ObjectProvider 装配，无业务状态 | N/A（本地保留原型 bean 装配；被装配任务的决策迁云端） | 本地保留原型 bean 获取（getObject）与类型路由 |
| `TaskHotStartService` | 热启动时对当前屏面拍快照：战斗中/选项对话/剧情对话/无（决定任务热启动 fallback 起点） | C | 本地：battleRadarService 战斗态 + dialogService 对话类型的视觉/识别判定合成 screen state | 云端 HotStartSnapshot/StateService（依据云端理解决定热启动 phase 起点） | 本地保留战斗雷达截图与对话识别（capture/OCR），把原始事实上送 |
| `TaskHotStartScreenState` | 热启动屏面枚举 NONE/OPTION_DIALOG/STORY_DIALOG/IN_COMBAT | D | 枚举常量 | N/A（共享模型） | 无 |
| `TaskHotStartSnapshot` | 热启动快照值对象（taskCode/source/state/dialogType + hasDialog） | D | 不可变 DTO | N/A（共享 DTO，可随协议上送云端） | 无 |
| `TaskPauseResumeReconciler` | CR160 暂停/恢复对账：比对暂停前后轻量运行事实，匹配则补偿本地定时器龄期续跑原 phase，不匹配则清易失态并要求任务热启动 fallback | B | 本地：捕获指纹(phase/actionState/preparedAction/visibleDialog/pathing) 并决定 matched vs fallback，驱动定时器补偿与易失态清除 | 云端 PauseResumeReconcile/SessionStateService（对账决策与 phase 续跑/回退判定） | 本地保留运行时事实快照采集（窗口态/对话/寻路 capture） |
| `TaskStartupCheckService` | 任务启动前的游戏内队伍身份门（五环需 leader/自动战斗需 member），阻断不支持的角色×任务组合 | A | 本地：依据 TeamTaskProperties 配置门 + context 已下传 role（或 TeamRoleDetection 实时检测）放行/skip | 云端 StartupGate/RoleGateService（身份×任务准入裁决） | 本地保留 role 实时检测的截图/hover 采集（仅五环走实时检测；自动战斗刻意不实时检测以免抢前台） |
| `TaskStartupCheckResult` | 启动前置判断结果 DTO（allowed + 被阻断时的 TaskRunResult + reason） | D | 不可变结果 DTO | N/A（共享结果模型） | 无 |
| `TaskTeamAssignmentPolicy` | 把用户请求任务按检测到的角色重映射为窗口真正执行的任务（成员踢回自动战斗、solo/未知禁跑 leader-only） | A | 本地：依据 role 与任务类别决定有效任务类型及是否需启动前实时角色检测 | 云端 TaskAssignment/DispatchPolicyService（身份×请求→有效任务裁决） | 本地保留启动前角色检测采集（当 shouldDetectRoleBeforeStart 为真） |
| `TaskType` | 任务类型枚举（wuhuan_v2/wubei/xiuluo/xiuluo_v2/auto_battle/sleep_computer/unknown + code/displayName） | D | 枚举常量 | N/A（共享模型） | 无 |
| `BaseTaskTemplate` | 步骤式任务抽象模板：驱动 before/steps/after 生命周期、stop 令牌检查与 gameContext 状态机迁移 | A | 本地：持有 stop 令牌轮询点、botStatus/actionState 状态机迁移、step→TaskRunResult 转换与异常收敛 | 云端 TaskPhase/OrchestrationService（步骤序列与 phase 迁移决策） | 本地保留步骤执行器循环、窗口激活/截图、sleep 执行（executor/capture） |
| `TaskStep` | 任务步骤函数式接口（execute + getStepName） | D | 契约 | N/A | 无 |
| `TaskStepExecutor` | 单步骤执行器：stop 检查 + 执行 + 按重试策略重试/回退 + 结果日志 | B | 本地：持有 attemptedRetries 计数、canRetry 判定、重试延时与异常→FAILED 收敛 | 云端 RetryPolicy/StepPolicyService（是否重试/延时/最终失败裁决） | 本地保留步骤动作调用与 stop 检查（executor） |
| `TaskStepResult` | 步骤结果枚举 SUCCESS/FAILED/SKIPPED/STOPPED | D | 枚举常量 | N/A（共享模型） | 无 |
| `TaskTurnCoordinator` | 跨窗口任务回合的公平锁协调：一个窗口在 CONTINUE_CHAIN 期间持回合，达到 yield 态时释放，公平锁保证排队窗口可预期获得维护机会 | A | 本地：公平 ReentrantLock 持有回合归属(lease)、按 shouldYield 决定保留/释放、ThreadLocal 持有深度与持有者身份 | 云端 TurnCoordination/LeaseService（跨窗口回合/租约仲裁与排队顺序） | 本地仅保留锁的物理获取/释放执行；仲裁语义迁云端后本地退化为租约执行器 |
| `TaskTransactionRunner` | 在任务回合归属下运行任务级事务（run/runDynamic/runExclusive），exclusive 额外独占物理输入队列 | A | 本地：enter/leave 包裹回合、独占输入 lease、异常→STOPPED/FAILED 收敛、上报指标 | 云端 TaskTransaction/PolicyExecuteService（runDynamic 的 result+yield 决策由云端 oracle 计算后回传） | 本地保留独占输入执行(submitExclusiveAndWait)与直接输入执行器 |
| `TaskYieldPolicy` | 调用方对事务后回合去留的偏好枚举 MUST_YIELD/MAY_YIELD/CONTINUE_CHAIN/RETRY_LATER/STOP_CHAIN | D | 枚举常量（最终去留还取决于 TaskTransactionResult） | N/A（共享模型） | 无 |
| `TaskTransactionResult` | 任务事务业务结果枚举，被 TaskTurnCoordinator 用于回合去留判定（描述任务进度非输入成败） | D | 枚举常量 | N/A（共享模型） | 无 |
| `TaskTransactionOutcome` | 单次任务事务不可变结果值对象（name/expected/yieldPolicy/result/completed + reachedExpectedResult） | D | 不可变 DTO | N/A（共享模型） | 无 |

<details><summary><b>隐式状态清单</b>（29 条，按 kind 分类）</summary>

**timer** (7)

- `AutoBattleTask` — FREE_PATROL_INTERVAL_MS=3000ms 空闲巡逻间隔（本地硬编码定时器）
- `AutoBattleTask` — PENDING_FIRST_AID_POLL_INTERVAL_MS=500ms 待急救 或 处于开放维护广播队列时的加速轮询
- `AutoBattleTask` — 战斗态使用 autoCombatService.getDynamicPollingIntervalMs() 动态雷达间隔（间隔权威在下游 combat service）
- `SleepComputerTask` — BEFORE_SLEEP_LOG_FLUSH_MS=1500ms 睡眠前日志刷盘延时（sleepOrStop 可被 stop 打断）
- `TaskPauseResumeReconciler` — compensateVolatileAutomationTimersAfterPause 按 pauseBlockedMs 补偿自动化易失定时器龄期（切换日关键：漏补=定时器误触发）
- `TaskStepExecutor` — delayBeforeRetry 按 retryPolicy.getDelayMillis 的重试等待（sleepOrStop 可打断）
- `TaskTurnCoordinator` — SLOW_TURN_THRESHOLD_MS=3000ms 慢持有/慢获取 watchdog 阈值（仅告警，非强制释放）

**memory** (5)

- `AutoBattleTask` — gameContext botStatus(RUNNING/IDLE)+currentActionState(FREE/IN_COMBAT) 作为循环存续与分支的本地记忆
- `TaskPauseResumeReconciler` — clearPauseResumeVolatileState 匹配失败时清除本地易失态（preparedAction/pathing 等）
- `BaseTaskTemplate` — gameContext.botStatus(RUNNING/IDLE/ERROR)+currentActionState(FREE) 由 before/afterTask 迁移，作为任务态本地记忆
- `TaskStepExecutor` — attemptedRetries 本地重试计数（每次 execute 独立）
- `TaskTurnCoordinator` — ThreadLocal holdDepth/heldWindowId/heldStartedAt/optionalTryRunHold 本地线程持有态；volatile lastReleaseAt/WindowId/Transaction/Result/QueuedWaiters 上次释放记忆

**cache** (2)

- `TaskHotStartService` — checkAndSyncCombatState 会同步 gameContext.currentActionState（读取即写回本地态）
- `TaskPauseResumeReconciler` — VISIBLE_DIALOG_FINGERPRINT_MAX_AGE_MS=8000ms 可见对话快照最大龄期（超龄不入指纹）

**lock** (2)

- `AutoBattleTask` — consumeMaintenanceBroadcastQueueTurnIfHead 依赖 leader 开启的维护广播队列头部顺序（排队授权，权威在 TaskMaintenanceService）
- `TaskTurnCoordinator` — fair ReentrantLock(true) 排队顺序——公平锁保证 leader 释放回合后排队窗口可预期抢到（fair-lock 排队权威，迁移日切换点）

**fallback** (8)

- `AutoBattleTask` — TickResult.EXIT_RECOVERED 且 FREE 时走本任务分支内的成员同回合归队自检（CR242 reopen）
- `TaskHotStartService` — combat 优先于 dialog：先查战斗态命中即返回 IN_COMBAT，否则再 inspect 对话；这是热启动 fallback 分支起点的隐式优先级
- `TaskPauseResumeReconciler` — 任一指纹字段不匹配→fallback(fallbackTaskHotStart=true) 要求任务进入热启动；pauseBlockedMs<=0 视为未暂停
- `TaskStartupCheckService` — 自动战斗刻意不做实时队伍检测（避免抢前台/战斗中误判结束任务）；role 未知时按 allowAutoBattleWhenRoleUnknown 配置放行或 skip
- `TaskTeamAssignmentPolicy` — member + (五环 或 leader-only) → 降级 AUTO_BATTLE；solo/未知 + leader-only → UNKNOWN(不派发)；UNKNOWN/AUTO_BATTLE 原样放行
- `BaseTaskTemplate` — steps 为空→SKIPPED；TaskStopRequestedException→STOPPED；其他异常→FAILED（afterTask 据此落态）
- `TaskStepExecutor` — 异常且不可重试→FAILED；TaskStopRequestedException→STOPPED；null 结果→SUCCESS
- `TaskTransactionRunner` — safeRun/safeRunDecision：null→FAILED；TaskStopRequestedException 或线程中断→STOPPED；exclusive 未完成→interruptedResult(中断=STOPPED 否则 FAILED)

**other** (5)

- `AutoBattleTask` — summonSkillBudgetForRequestedTask：xiuluo_v2=2 否则=1，每队伍回合召唤技清理预算（本地阈值）
- `DefaultTaskFactory` — XIULUO 与 XIULUO_V2 均路由到 xiuluoTaskV2Provider（旧编码兼容映射）；UNKNOWN 返回 null
- `TaskStartupCheckService` — 多个配置开关(isFiveRingRequiresLeader/isAutoBattleRequiresMember/isAllowAutoBattleWhenRoleUnknown) 决定门是否启用（准入权威受配置驱动）
- `TaskTurnCoordinator` — input-worker 线程名含 dhxy-input-action-worker 时 enter/leave/tryRun 全部空转（回合须在进入独占输入前已持有）
- `TaskTransactionRunner` — input-worker 线程内直接执行以避免队中队死锁；runDynamic 供云端 task-policy 在本地 oracle 计算后决定 result 与 yield

</details>

### 2.2 task/wubei (五倍) — THIN_CLIENT_V1 A-1 方法级迁移矩阵底账

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `WubeiTask` | 五倍队长任务的本地单体大脑：驱动 14-态相位机、持有全部下一步/重试/恢复/维护/任务分类/暗雷reroll/显形镜探测/黄袍连战/回程判定，并直接调度截图/点击/导航/战斗执行。 | A | 本地 WubeiTask 权威。相位机+所有业务分支写死在本地；TASK_POLICY/TASK_RECOVERY 只是'本地先决策、云端可否决/覆盖'的影子语义(applyTaskPolicyCloudDecision:690, decideTaskRecovery:771)，与云端单脑相反=双脑。OCR/tracker读取/dest-hint 已云端权威。 | WUBEI_BRAIN（待建）：dhxy-cloud-brain DecisionEngine 的 wubeiBrain start/step/actionOutcome + 有状态 WubeiBrainSession + 每 phase 一个 *Next，据结构化 facts 决策；客户端只留 runRoundWithWubeiBrain 壳+facts。 | 纯 executor/capture：截图与3帧ROI采样(captureTrackerDestinationHint)、模板匹配点击(clickTaskTrackerGreen/tryClickTrackerCombatTargetSmart/tryDirectCombatFromTrackerHint)、navigationService 寻路、npcClickService.clickNpcSmart、autoCombatService.handleCombatTick/probeWindowCombatStateReadOnly、bag 返回/探测道具(useReturnItem/returnItemPrescanService)、dialog 模板消费。 |
| `WubeiPhase` | 五倍相位机的相位枚举（14工作态+ROUND_DONE/FAILED/STOPPED 三终态）及 isTerminal 协议。 | A | 本地：相位集与终态判定是本地相位机协议。 | WUBEI_BRAIN：迁后 phase 大体沿用，云端 *Next 决策据 facts 给出下一 phase；终态映射 WubeiBrainActionType(COMPLETE_ROUND/FAIL_TASK/STOP_TASK)。 | 作为客户端壳的 phase 标识可保留（对标 XiuluoPhase），但'下一步是哪个 phase'不再本地决定。 |
| `WubeiRoundContext` | 一轮五倍的不可变-by-copy 状态载体：phase/round/source/phaseRetryCount/recoveryCount/waitingPathing/waitingAcceptDialog 及各种 next/retry/recover/wait 迁移子。 | A | 本地：本轮状态机的唯一权威承载，所有 state.next()/retrySamePhase()/recoverTo() 硬编码相位跃迁在此定型。 | WUBEI_BRAIN 的 WubeiBrainRoundState（云端 session 内类，含 loop guard/事件 park/pre-battle watchdog）；retry/recovery 计数迁为云端 facts。 | 客户端可保留只读的相位/round 标识随 facts 上报；phaseRetryCount/recoveryCount 的语义归云端。 |
| `WubeiStepOutcome` | 单个 phase 执行结果载体：nextState + TaskTransactionResult + TaskYieldPolicy + message + 可选 WubeiWaitSpec，含 continueTo/pathingStarted/sharedState/failed/stopped 工厂。 | B | 本地：承载相位机产出的转移结果与让出/park 策略；failed()/stopped() 直接置 FAILED/STOPPED 相位。 | WUBEI_BRAIN 的 WubeiBrainDecision/WubeiBrainActionOutcomeDecision（云端下发 action+结果码），客户端只回填执行 outcome。 | 客户端壳保留把云端指令翻成本地 turn 结果(transactionResult/yieldPolicy)的载体。 |
| `WubeiWaitSpec` | phase 让出 turn 后可安全 park 的调度策略值：reason + wakeTypes + timeoutMs + minParkMs + currentWindowOnly + allowOpportunisticMaintenance。 | A | 本地：park/唤醒调度策略，契约要求'绝不编码业务成败'。 | 保留客户端（对标 XiuluoWaitSpec）：迁移文档明列'复用/扩展 WubeiWaitSpec/WubeiWaitReason'，调度不上云；WAIT_FOR_EVENT 语义由云端 action 触发但 park 本地执行。 | 全部保留本地——parkAfterYieldIfNeeded 消费它做窗口事件/超时唤醒。 |
| `WubeiWaitReason` | 解释 phase 在等何种外部状态的调度-only 原因枚举（WAIT_PATHING_TERMINAL/ACCEPT_NPC_ROUTE/PREPARED_DIALOG/COMBAT_STATE_CHANGE/TEAM_ATTENTION/RETRY_TIMER）。 | A | 本地：契约明文'不得编码业务成败'，仅供 park+唤醒。 | 保留客户端（对标 XiuluoWaitReason）；不上云。 | 全部保留本地。 |
| `WubeiDialogCatalog` | 五倍对话模板/动作键共享目录：接任务 chumoweiguo、进战斗三模板(消灭它/证明实力/魁星归位)、显形镜 story koukou/wrong-position 及其 GreenTemplateClickSpec/WhiteTemplateSpec 与点击偏移。 | C | 本地：模板路径与点击偏移策略的单一属主，watcher 与 task 共用避免漂移。 | 视觉解释若上云由 DecisionEngine 识别器持有模板；点击偏移/spec 属本地执行 catalog。 | 保留本地：模板路径+GreenTemplateClickSpec 偏移(接任务32,78,3；进战斗-6,18,4)是纯执行资产。 |
| `WubeiDialogPreparationProvider` | 五倍专属对话准备器（@Component，在窗口 runner 后台准备接任务/进战斗/显形镜 story 的可点击动作），仅按 task interest 的 operation 准备。 | C | 本地：window runner 侧的对话模板准备执行者；接任务用 MemoryService 稳定选择记忆命中，story 用云端白字模板或 absent。 | 视觉识别部分对齐 cloud recognizer；准备/记忆命中/absent 门属客户端 runner 执行层。 | 保留本地：模板准备+记忆坐标复用+absentAllowed 门（纯执行/capture）。 |

<details><summary><b>隐式状态清单</b>（29 条，按 kind 分类）</summary>

**timer** (10)

- `WubeiTask` — PROBE_ENTER_BATTLE_TIMEOUT_MS=300_000 探测任务开战超时(currentProbeTaskStartedAt→timeoutProbeTaskBeforeBattleIfNeeded:1579)：超时清全部runtime并ROUTE_TO_MAIN_TASK重接。
- `WubeiTask` — WAIT_BATTLE_TIMEOUT_MS=180_000 等战斗超时(waitBattleStartedAt:4475)→FAILED；含无战斗6s/3s回点enter-battle重试(waitBattleNextTrackerRetryAt:4481)。
- `WubeiTask` — runner发布的 PRE_BATTLE_TIMEOUT 180s全局开战预算(getOrdinaryPreBattleStartedAtMs)：pendingPreBattleBudgetTimeoutEvent:896/consumePreBattleBudgetTimeout:920/throwPreBattleBudgetTimeoutIfNeeded:952，超时清全runtime→重接，outranks所有内层等待。
- `WubeiTask` — runner发布的 POST_COMBAT_IDLE_TIMEOUT(lastPostCombatIdleTimeoutConsumedSeq:348)→consumePostCombatIdleTimeout:848 清全runtime并ROUTE_TO_MAIN_TASK重接。
- `WubeiTask` — enterBattle 6s 重试节流(enterBattleStartedAt/enterBattleNextRetryAt:4231)；含smart/direct点击后 now+6000 冷却。
- `WubeiTask` — 医宝宝/修装备维护冷却(lastHealPetMaintenanceAt/lastRepairEquipmentMaintenanceAt:337, isHealPetMaintenanceDue:2000/isRepairEquipmentMaintenanceDue:2009，interval 来自 BotProperties)。
- `WubeiTask` — WUBEI_MAINTENANCE_PATHING_HARD_TIMEOUT_MS=180_000 维护寻路硬超时(continueIfMaintenanceNavigationStillPathing:1976)。
- `WubeiTask` — dest-hint 3帧固定采样偏移{500,1000,1500}ms(TRACKER_DEST_HINT_CAPTURE_OFFSETS_MS:211) ROI(350,370,679,463)；probe story wait(currentProbeStoryWaitStartedAt:329)；broadcast handoff 3s(MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS)/chained 首援5s。
- `WubeiTask` — 暂停补偿：PAUSE_TIMER_COMPENSATION_THRESHOLD_MS=1000 阈值+compensateProbe/EnterBattle/WaitBattle/FormalMaintenanceTimers(:1607-1656) 在暂停后回加各计时器。
- `WubeiWaitSpec` — timeoutMs park 最大时长(负值=等 runner 事件/中断，WUBEI_WAIT_UNTIL_RUNNER_EVENT_MS=-1)；minParkMs 最小 park 防抖。

**memory** (7)

- `WubeiTask` — 探测局部记忆 currentProbeSegments/currentProbeUsed/currentProbeItemAttempts/currentProbeIndex:324-327 (MAX_PROBE_ITEM_ATTEMPTS_PER_LINK=2) 记录每绿字显形镜用/剩次数。
- `WubeiTask` — 维护连续失败计数 consecutiveHealPet/RepairEquipmentMaintenanceFailures:339(限3=MAX_CONSECUTIVE_MAINTENANCE_HOOK_FAILURES)；lastLeaderPathingSummonAttemptRound:341；黄袍续战计数 currentRoundChainedCombatContinueCount/RecoveryBroadcastCount。
- `WubeiTask` — 接任务对话选择记忆经 MemoryService(findStableTaskDialogChoice) 复用绿字选项相对坐标。
- `WubeiRoundContext` — phaseRetryCount 本地重试计数(retrySamePhase 累加)。
- `WubeiRoundContext` — recoveryCount 本轮广义重启计数(recoverTo 累加，>=3→FAILED 由 WubeiTask 判)。
- `WubeiRoundContext` — waitingPathing/waitingAcceptDialog 本地寻路/接任务对话等待布尔记忆。
- `WubeiDialogPreparationProvider` — MemoryService.findStableTaskDialogChoice('wubei','acceptTask','降魔侍卫') 接任务选项相对坐标本地记忆。

**cache** (3)

- `WubeiTask` — verifiedReturnHomeLocation:347 跨round保留的已验证回宝象国位置事实，喂 route 的 freshCurrent* 免重读地图。
- `WubeiTask` — 黄袍连战绿字快速通道缓存 currentRoundChainedTrackerFastAction/currentRoundChainedTrackerCacheAttempted:318：首次战后全读tracker建缓存，之后只验小区域，miss=链结束不再fallback全读(returnHomeAfterCombatOrContinueSpecialTarget:4712)。
- `WubeiTask` — currentTrackerPanel/currentTrackerDestinationHint tracker快照缓存；postAcceptTrackerPanelFuture:321 接任务后异步预读tracker。

**lock** (1)

- `WubeiTask` — TaskTurnCoordinator/TaskTransactionRunner 公平回合队列：MUST_YIELD/SHARED_STATE/PATHING_STARTED 释放 task turn 并 park，供其它窗口按队补血/归队；yieldAfterMustYield/parkAfterYieldIfNeeded:1144-1226 决定让出顺序。

**fallback** (6)

- `WubeiTask` — pause-resume fallback 任务热启动(resolvePauseResumeTaskHotStart:642)：指纹不匹配即清 volatile state 回 HOT_START_DETECT。
- `WubeiTask` — 热启动返回道具兜底(runHotStartDetectPhase:1707)：after-combat-exit-startup 且 tracker 未命中时用返回道具验证起始地图。
- `WubeiTask` — STOPPED_AWAY 且无移动事实(movementObservedAtMs<=0)→重导航同一绿字而非进战斗(runResolveAfterPathingPhase:2175)。
- `WubeiTask` — 回程验证失败→trusted 战斗只读探测纠偏，仍在战斗则回 WAIT_BATTLE_FINISH(correctExpectedReturnFailureIfStillInCombat:4864/resumeWaitBattleAfterTrustedReturnCorrection:4889)。
- `WubeiTask` — near-destination 目的地黄字提示兜底：runner 位置接近 hint 时 smart→direct 战斗点击(tickEnterBattle:4311-4342)。
- `WubeiTask` — recoverRoundAfterFailure:659 recoveryCount>=3 才 FAILED，否则 recoverTo(ROUTE_TO_MAIN_TASK) 广义重启本轮；phaseLoopGuard>32 也触发同一恢复。

**other** (2)

- `WubeiStepOutcome` — yieldPolicy(MUST_YIELD/CONTINUE_CHAIN)+transactionResult(PATHING_STARTED/SHARED_STATE_TRIGGERED/FAILED/STOPPED) 决定是否释放公平 turn 与 park，属 stop/pause/turn 安全语义。
- `WubeiDialogPreparationProvider` — interest.isAbsentAllowed(now) 决定显形镜 story 是否允许 STORY_ABSENT——影响 probe 状态机分支(非纯D)。

</details>

### 2.3 task.wuhuan (五环 FiveRingTaskV2 相位状态机组)

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `FiveRingPhase` | 五环 V2 工作流的显式阶段枚举，定义 PREPARE→BUY_SHOES→HANDOVER_DETECT→ACCEPT_TASK→WAIT_PATHING→HANDLE_DIALOG→SYNC_TASK_PANEL 及三个终态，并提供终态判定。 | A | 本地：枚举即相位机的状态协议定义，FiveRingTaskV2 依它 switch 分支 | 云端 FiveRing 编排 Service 的相位状态协议（应与云端状态机枚举一一对应） | 无（纯状态协议定义，随相位机迁云端；本地无需保留） |
| `FiveRingStepOutcome` | 单个阶段执行结果的不可变封装：nextState、transactionResult、yieldPolicy、terminalTask、message；工厂方法把逻辑结果映射为事务结果+让出策略。 | A | 本地：承载 turn-yield 协议（MUST_YIELD/CONTINUE_CHAIN）与终态标志，是相位机与 TaskTransactionRunner 之间的结果协议边界 | 云端相位机的 StepOutcome/让出协议（yield policy 与 terminalTask 语义需在云端复刻） | 无（结果协议载体，迁云端） |
| `FiveRingCompletionPolicy` | 纯函数策略：依据 configuredRuns、currentRound、final/once 完成模板是否可见，决定 STOP_ALL_RUNS / FINISH_CURRENT_RUN / NO_MATCH。 | B | 本地静态策略：五环'是否完成/是否停全部轮'的业务判定权 | 云端 FiveRing 完成判定 Service（把 round/配置轮数/模板可见性作为输入，返回同枚举决策） | 无（纯决策，迁云端；本地仅保留模板可见性的 OCR/模板识别 capture） |
| `FiveRingPhaseContext` | 单次五环 run 的不可变（copy-on-write）状态快照：相位、轮次、鞋袋页/购买数、taskAccepted 身份、tracker ROI 缓存、寻路时间戳/intent 协议、战斗基线截图、retry/uiError 计数、cleanTransition 启动标志；含 pause/resume 定时器补偿。 | A | 本地：五环单轮全部权威状态载体（身份 taskAccepted、寻路 lease/intent、定时器、缓存、记忆计数都在此） | 云端 FiveRing run session 状态（相位机运行态需整体迁云端持有） | wuhuanTrackerCombatBaselineImage 是本地 BufferedImage 截图产物（capture 能力本地）；其余状态字段迁云端 |
| `FiveRingTaskV2` | 五环任务的显式相位状态机主控（准备/买鞋/交接检测/接任务/寻路等待/对话处理/任务追踪同步 + 战斗门控 + 跨窗口 ready-event 优先仲裁 + turn lease 管理 + pause/resume 对账），复用旧服务执行导航/对话/给物/买鞋/战斗恢复。这是本组的核心业务大脑。 | B | 本地：完整持有五环相位决策、重试/回退/超时逻辑、寻路 intent 仲裁、粗粒度 turn lease、完成判定、暂停恢复对账——即待证明的'本地业务大脑'本体 | 云端 FiveRing 单脑编排 Service（相位机 + 全部 B 类决策 + A 类 turn/pause/stop 安全层需迁云端） | 本地仅保留纯 executor/capture：NavigationService 导航点击、NpcClickService NPC 点击、DialogService 对话模板点击、BagService 开袋/找物、InputSequences 输入序列执行、GameClientTracker 截图、TaskTrackerPanelService/OCR 模板识别、UICleanerService 关窗、AutoCombatService 战斗 tick 执行 |

<details><summary><b>隐式状态清单</b>（25 条，按 kind 分类）</summary>

**timer** (5)

- `FiveRingPhaseContext` — pathingStartedAtMs：寻路等待起点时间戳，驱动 90s 硬超时/2s grace/2.5s fast-wait 的看门狗；next(WAIT_PATHING) 时置为 now，其它相位置 0
- `FiveRingPhaseContext` — pauseInternalAutomationTimers(blockedMs)：暂停恢复时把 pathingStartedAtMs 加上 blockedMs 做定时器补偿（fingerprint 匹配时），是切换日/暂停安全的关键
- `FiveRingTaskV2` — 寻路看门狗：PATHING_TARGET_WAIT_TIMEOUT_MS=90s 硬超时→转 SYNC_TASK_PANEL 并 increaseUiErrorCount；PATHING_RECHECK_GRACE_MS=2s；PATHING_OBSERVER_FAST_WAIT_MS=2.5s；OBSERVER_SNAPSHOT_MAX_AGE_MS=3s 快照新鲜度；PATHING_INTENT_CREATED_AT_GRACE_MS=1s intent 陈旧守卫
- `FiveRingTaskV2` — prepared/route 动作时效：PREPARED_TRACKER_ACTION_MAX_AGE_MS=2.5s、PREPARED_ROUTE_DIALOG_CLICK_MAX_AGE_MS=10s、TRACKER_NEGATIVE_MAX_AGE_MS=2.5s；ready 事件 settle=80ms / pending-warn=3s / priority-yield-delay=180ms / task-turn-handoff=900ms / pathing-handoff=250ms
- `FiveRingTaskV2` — 买鞋/进店/返回时序：SHOE_SHOP_ENTRY_CONFIRM_TIMEOUT_MS=10s、门确认 2s、下坐骑后确认 1.5s、下坐骑沉降 1s、快速返回校验 2.5s/250ms 轮询(容差0.35)；启动预检 UI-clean/first-aid 各 60s max-age

**memory** (7)

- `FiveRingPhaseContext` — shoeBagIndex：记忆鞋在主袋的页码；shoePurchaseCount：本轮需买鞋数（clamp 1..2）
- `FiveRingPhaseContext` — taskAccepted：本轮任务已被左侧五环追踪标题确认的身份标志（点击接受不算，须 title 可见）
- `FiveRingPhaseContext` — phaseRetryCount / uiErrorCount：retrySamePhase/increaseUiErrorCount 累加的重试与 UI 错误计数记忆，驱动清理/失败阈值
- `FiveRingPhaseContext` — pathingIntentExpected + pathingIntentSource + pathingMovementObserved + combatObservedSincePathing + waitingAcceptNpcPathing：寻路 intent 协议/观测记忆位
- `FiveRingTaskV2` — 重试与阈值记忆：MAX_ACCEPT_RETRY=5、MAX_UI_ERROR_BEFORE_CLEANUP=3、MAX_GIVE_ITEM_FAILURE_BEFORE_FAIL=6、MAX_TRACKER_NOT_FOUND_BEFORE_FAIL=9、MAX_PHASE_LOOP_GUARD=80（相位空转保护，PATHING/SHARED 结果清零计数）
- `FiveRingTaskV2` — pause/resume 指纹对账（TaskPauseResumeReconciler）：每相位 capture 指纹，恢复时 fingerprintMatched 则补偿定时器，mismatch/fallbackTaskHotStart 则 pauseResumeHotStart 回到 PREPARE——暂停语义记忆
- `FiveRingTaskV2` — GameContext 全局状态权威：setBotStatus(RUNNING/IDLE/ERROR) 与 currentActionState(IN_COMBAT/FREE)；isWindowCombatActive 读该状态做战斗门控，releaseWindowCombatStateAfterWuhuanEvidence 主动纠正为 FREE

**cache** (4)

- `FiveRingPhaseContext` — wuhuanTrackerCombatBaselineImage + wuhuanTrackerCombatBaselineCapturedAtMs：战斗中五环 tracker ROI 基线截图缓存，用于战斗退出的像素变化判定；替换时 flush 旧图
- `FiveRingPhaseContext` — trackerPanelRegion / wuhuanTrackerBlockRegion：缓存的任务追踪面板/五环任务块窗口相对 ROI，跨相位复用避免重扫
- `FiveRingTaskV2` — acceptSetupPositionPrewarm：CompletableFuture 异步预热接任务 NPC 位置（ACCEPT_SETUP_POSITION_PREWARM_MAX_AGE_MS=20s 过期丢弃），volatile 字段跨相位复用一次坐标读
- `FiveRingTaskV2` — 战斗退出 tracker ROI 像素比对（WUHUAN_TRACKER_COMBAT_ROI_SAME_TOLERANCE=0.08）：以 FiveRingPhaseContext 基线截图对比当前 ROI，变化后再用 trusted battle state / prepared tracker / 完成对话交叉验证，防误判

**lock** (2)

- `FiveRingTaskV2` — TaskTransactionRunner 粗粒度任务 turn（fair-lock 排队）：run/runExclusive 获取，forceReleaseTurn 在 outside-enter/outside-yield/execute-finished 释放；WAIT_PATHING/BUY_SHOES/ACCEPT_TASK/HANDLE_DIALOG/SYNC_TASK_PANEL 在 turn 外跑以免长时 OCR/寻路占锁——切换日多窗口公平性关键
- `FiveRingTaskV2` — 跨窗口 ready-event 优先仲裁（WindowReadyEventBus）：checkReadyPriorityBeforeOutsidePhase 让位于其它窗口 fresh prepared-action / pathing-terminal（READY_EVENT_PRIORITY_MAX_AGE_MS=3s），本窗 retrySamePhase 让出——多窗口调度顺序

**fallback** (5)

- `FiveRingPhaseContext` — cleanTransitionStartup：干净排队跨任务转场标志，nextAfterPreparation 据此跳过 HANDOVER_DETECT 直接 ACCEPT_TASK（fallback 分支）
- `FiveRingTaskV2` — 买鞋回退链：quickBuyShoe 失败→shop-owner 买鞋流程；返回长安失败(3次)→导航洛阳城李道宗(324,109)修理NPC 兜底；买按钮模板未命中→相对坐标(627,493)兜底点击
- `FiveRingTaskV2` — 接任务回退：交接时 tracker 不可读→转 ACCEPT_TASK 而非失败；导航/NPC点击/接受失败→tryAcceptInitialTaskFromCurrentScreen 就地接任务兜底；already-has-task 对话→清理后转 SYNC
- `FiveRingTaskV2` — 追踪缺失回退：SYNC_TASK_PANEL 中 title 不可见+allowFinished 时走 returned-dialog 兜底(already-has-task / 完成故事)；tracker 连续未找到达 9 次→直接 FAILED（不退回 ACCEPT，防重复接任务死循环）
- `FiveRingTaskV2` — 战斗回退：waitPathing 中 combatObservedSincePathing 后 handleCombatTick 恢复；若无退出信号仍继续 tracker sync（留 warn 不死循环）；stopped-away tracker intent 在非给物图直接清 intent 转 SYNC

**other** (2)

- `FiveRingStepOutcome` — finished/finishedTerminal/failed/stopped 工厂在构造时通过 state.next(...) 强制把 nextState 相位改写为对应终态——终态跃迁隐含在 outcome 构造里，不在调用点
- `FiveRingTaskV2` — shoePurchaseCount 归一化 normalizeShoePurchaseCount：>=2 一律取 2、否则 1；requiredShoeCountForRun 按 configuredRuns-round+1 计算（unlimited 时恒 1）

</details>

### 2.4 task/xiuluo (修罗 XiuluoTaskV2 及其状态/DTO/枚举，THIN_CLIENT_V1 A-1 方法级迁移矩阵)

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `XiuluoTaskV2` | 修罗任务客户端薄壳兼当前唯一业务大脑宿主：跑云脑命令循环，但整段 phase 执行、进战前 watchdog、retry/recovery 预算、event-park 调度、维护 FIFO、CR220 返回道具兜底仍在本地。 | A | 本地 XiuluoTaskV2（云端仅下发 phase/actionType；phase 执行、watchdog、retry 预算、park 时序、维护队列、返回道具兜底均本地权威） | DecisionEngine(XIULUO_BRAIN nextXiuluoBrainCommand/各 phase next) + XiuluoBrainService(start 钩子) | 纯 executor/capture：inputSequences 物理点击、npcClickService NPC 点击、navigationService 寻路执行、uiCleanerService 清理、autoCombatService 战斗 tick、boundWindowCaptureService/OCR 截图、bagService 返回道具使用、dialog 绿模板 prepare |
| `XiuluoRoundContext` | 单轮不可变状态载体：当前 phase、objective、路线/战斗来源、tracker 点击记忆、进战前 watchdog 时间锚、各类 retry/recovery 计数。 | A | 本地（承载 watchdog 时间锚 preCombatStartedAtMs 与 retry 预算计数，均本地权威） | DecisionEngine（phase 转移权威）；retry/recovery 预算与 watchdog 计时应随迁云端 | tracker 点击坐标(shortcutTrackerClickX/Y 及 window-relative)等本地 capture 结果可留本地 |
| `XiuluoBrainRoundState` | 单个 XIULUO_BRAIN 轮次的本地状态承接：current context、pendingWaitSpec 暂存、热循环计数、CR220 返回道具一次性记忆。 | A | 本地（热循环 guard 计数、waitSpec 暂存、返回道具试过标记均本地） | DecisionEngine（phase 转移仍云端；本类只保存本地 phase handler 产物） | waitSpec 暂存与 consume 属本地调度承接，可留本地 |
| `XiuluoHotStartResolver` | 从当前屏幕状态判定修罗 phase 机的安全入点（IN_COMBAT/STORY_DIALOG/OPTION_DIALOG/NONE→对应 phase），不执行任务动作。 | B | 本地（hot-start phase 决策；仅在云脑 loop 关闭的旧路径使用，brain 路径由云端 start 钩子决定 tracker hot-start） | DecisionEngine start 钩子 / XiuluoBrainService（hot-start 初始 phase 决策） | 屏幕分类委托 TaskHotStartService/DialogService（模板/OCR capture）属本地 capture |
| `XiuluoPhase` | 修罗工作流 phase 枚举（17 可执行 + ROUND_DONE/FAILED/STOPPED 终态），phase 是唯一决定'从何处恢复'的地方。 | A | 共享协议词表（客户端可执行白名单 XT vs 云端 switch DE 双侧同为 17 可执行 phase） | DecisionEngine（phase 转移权威定义） | 枚举常量与 isTerminal() 纯 helper 留本地 |
| `XiuluoWaitReason` | 释放任务 turn 后 park 修罗 phase 的 scheduling-only 原因枚举（5 值），显式声明非业务成败态。 | A | 共享协议词表（shell WAIT_FOR_EVENT 白名单按 reason 放行同 phase park） | DecisionEngine（将 reason 映射为 WAIT_FOR_EVENT 命令） | 枚举定义留本地 |
| `XiuluoWaitSpec` | 修罗让权后 park 的 scheduling 策略 DTO：reason、wakeTypes、afterSequence（事件丢失防护锚）、timeoutMs、pathing intent 过滤字段。 | D | 本地（park 调度载体；timeoutMs/afterSequence 由 XiuluoTaskV2 设置） | DecisionEngine（WAIT_FOR_EVENT 协议）；本 DTO 为承载器 | park 调度参数属本地 executor 层，可留本地 |
| `XiuluoRouteMode` | 当前轮路线族枚举：OBJECTIVE_NAVIGATION / TRACKER_SHORTCUT。 | D | 本地（路线选择逻辑在 XiuluoTaskV2/云端，枚举仅词表） | DecisionEngine（结合 routeMode 事实决策） | 枚举定义留本地 |
| `XiuluoCombatSource` | 当前修罗战斗如何进入的枚举：NONE / TRACKER_CONFIRM（本方看打）/ INCIDENTAL（途中遭遇）。 | D | 本地（分类逻辑在 waitCombat/context，枚举仅词表） | DecisionEngine（combatSource 事实驱动 next phase） | 枚举定义留本地 |
| `XiuluoStepOutcome` | 单 phase 执行结果 DTO：nextState、transactionResult、yieldPolicy、message、waitSpec、云端结构化 facts。 | D | 本地（phase handler 产出的上报载体） | DecisionEngine（消费 transactionResult/yieldPolicy/facts） | 作为本地→云端事实上报载体 |
| `XiuluoActionExecutionResult` | 执行一个 XIULUO_BRAIN 请求动作的事实结果 DTO（actionId/actionType/Status/facts/evidencePaths），不带后继业务 phase。 | D | 本地（客户端尝试与观察结果上报） | DecisionEngine（phase 权威在云端，本 DTO 只报事实） | 本地执行事实与证据路径载体 |
| `XiuluoPhaseReport` | 一个 phase 的事实报告 DTO（phase/status/actionResult/waitSpec/facts/evidencePaths），刻意不编码后继业务 phase。 | D | 本地（事实报告载体） | DecisionEngine（phase 决策在云端） | 本地事实/证据载体 |
| `XiuluoDialogPreparationProvider` | 修罗自有的看打进战选项 watcher 准备器（实现 WindowDialogPreparationProvider），为窗口观察者预备 XIULUO_ENTER_BATTLE 绿模板点击。 | C | 本地（模板 prepare/capture 原语；进战仲裁权归云端 arbitration） | DecisionEngine/绿链 arbitration（何时点击的决策） | prepareGreenTemplateOption 绿模板准备属本地纯 capture/prepare 原语，保留本地 |
| `XiuluoDialogCatalog` | 修罗自有对话模板常量目录（TASK_CODE、看打进战选项名与模板路径、enterBattleSpecs），前台任务与 watcher 共用。 | D | 本地（模板资产常量） | 无（D6：同模板同算法同阈值仅位置搬迁，识别资产留本地） | 模板路径与 GreenTemplateClickSpec（偏移 -6,6,4）属本地 capture 资产，保留本地 |

<details><summary><b>隐式状态清单</b>（38 条，按 kind 分类）</summary>

**timer** (10)

- `XiuluoTaskV2` — PRE_COMBAT_WATCHDOG_TIMEOUT_MS=180000 进战前 watchdog；锚 preCombatStartedAtMs（shortcut 用 firstTrackerGreenClickAtMs）；仅 shouldApplyPreCombatWatchdog 白名单 13 phase 生效（排除 WAIT_COMBAT/RETURN_HOME/NAVIGATE_BACK_TO_START/WAIT_TEAM_RETURN/终态）；被 turn-wait/pause/maintenance park 补偿。超时→phase-FAILED+watchdogTimeout 事实→云端 RESTART_ROUND
- `XiuluoTaskV2` — TEAM_RETURN_STATE_WAKE_TIMEOUT_MS=20000 CR244 归队 park 保险超时；超时只重读 member-owned set，绝不判定为已归队、不 busy-poll
- `XiuluoTaskV2` — MAINTENANCE_BROADCAST_QUEUE_CAP_MS=5000 CR245 队长维护 FIFO 队列 cap；到点=按设计释放（丢弃剩余成员，下个冷却轮重触发），与 CR244 语义相反
- `XiuluoTaskV2` — MAINTENANCE_NO_LOCAL_MEMBER_COURTESY_WAIT_MS=3000 无本地成员时给程序外真人队友的固定礼貌等待（无 fact 可等）
- `XiuluoTaskV2` — MAINTENANCE_SELF_CONFIRM_PROBE_TTL_MS=10000 队长自确认后台探测点新鲜度；probeAge>2×TTL 视为陈旧丢弃 pendingMaintenanceQueueHook
- `XiuluoTaskV2` — ENTER_BATTLE_LOCAL_PROBE_DELAY_MS=20000 CR232 本地 kanda2 小 ROI probe 起始延迟，锚在首次 tracker 绿字点击（≈accept+25s 语义）
- `XiuluoTaskV2` — WAIT_COMBAT_MAINTENANCE_WAKE min500/max10000ms 战斗中维护唤醒；WAIT_TARGET_PATHING_TERMINAL_TIMEOUT_MS=-1 无限等寻路终态；RUNNER_PATHING_HARD_TIMEOUT_MS=180000
- `XiuluoTaskV2` — ACCEPT_DIALOG_CLOUD_FALLBACK_TTL_MS=180000 + POLL_MS=200 接任务对话云端兜底 future 的 TTL/轮询
- `XiuluoRoundContext` — preCombatStartedAtMs：进战前 watchdog 锚，跨同轮 retry/recovery 保留；pausePreCombatTimer 按 blockedMs 平移（阈值 PRE_COMBAT_PAUSE_COMPENSATION_THRESHOLD_MS=500 以下忽略）
- `XiuluoRoundContext` — firstTrackerGreenClickAtMs：首次 shortcut 绿字点击时间，启动 shortcut 进战前 watchdog，withShortcutTrackerClick 不因重点击重置

**memory** (9)

- `XiuluoTaskV2` — lastHealPetMaintenanceAt / lastRepairEquipmentMaintenanceAt：维护冷却时钟（isHealPetMaintenanceDue/isRepairEquipmentMaintenanceDue），execute 启动时按 xiuluoMaintenanceRunImmediatelyOnStart 置 0 或 now
- `XiuluoTaskV2` — startupIncenseChecked/startupIncensePending：首轮 hot-start 前摄妖香一次性 guard（真实 cooldown 规则仍属 PlayerStateService）
- `XiuluoTaskV2` — lastPostCombatIdleTimeoutConsumedSeq：POST_COMBAT_IDLE_TIMEOUT 事件按 sequence 去重消费，消费一次→recoverTo ACCEPT_TASK_NAVIGATE_TO_NPC
- `XiuluoTaskV2` — pendingMaintenanceQueueHook + maintenanceSelfConfirmProbeFuture/StartedAtMs：CR245 当前 draining 的 hook 及后台自确认探测点（volatile 跨命令周期记忆）
- `XiuluoTaskV2` — acceptDialogCloudFallbackFuture/acceptDialogCloudFallbackRound：接任务对话云端兜底后台 future 及其所属 round
- `XiuluoTaskV2` — XiuluoBrainRoundState.startupReturnItemTriedAndUnverified：CR220 返回道具本轮只试一次的记忆（未验证不再重试）
- `XiuluoRoundContext` — retry 计数：phaseRetryCount / enterBattleConfirmRetryCount / recoveryCount / shortcutTrackerRetryCount——分别驱动 phase 重试、看打确认重试、恢复跳转、shortcut 复按预算，next() 会清 phaseRetryCount
- `XiuluoRoundContext` — waitingPathing/startExitPrepathStarted/enteredBattleByXiuluo/routeMode/combatSource：路线与进战状态标志，决定 watchdog 适用性、post-combat recovery policy、是否广播队伍战斗
- `XiuluoBrainRoundState` — startupReturnItemTriedAndUnverified：CR220 hot-start 返回道具本轮已试且未验证的记忆，阻止重复用道具

**cache** (2)

- `XiuluoTaskV2` — lastStartMapVerifiedLocation/lastStartMapVerifiedAtMs：确认在灵兽村的位置同步缓存，供下次 accept-NPC 导航复用为 caller-fresh 位置（NavigationService 仅信 3s 新鲜窗口）
- `XiuluoTaskV2` — 绿链 attempt 身份（openXiuluoGreenChainSchedule 写 WindowRuntimeContext 的 XiuluoGreenChainSchedule.attemptId）：本地 runtime 记忆，门控 4 类 typed prepared job；原子替换即作废旧 attempt 的 job/prepared

**lock** (3)

- `XiuluoTaskV2` — 公平锁排队顺序：yieldAfterMustYield 在共享态(pathing/combat)让权后插入 TASK_TURN_HANDOFF_DELAY_MS=900ms handoff 延迟，防止 leader 线程立即重夺 fair task-turn 锁而饿死 follower auto-battle tryLock；维护另有 MAINTENANCE_BROADCAST_HANDOFF_DELAY_MS=3000
- `XiuluoTaskV2` — XIULUO_FAILURE_CASE_REPORT_MONITOR 静态对象锁，序列化 failure-case 报告文件追加
- `XiuluoBrainRoundState` — consecutiveImmediateLoopCount：热循环/立即让权 guard 计数，noteRealEventWaitCompleted 在真实 event park 完成时清零（防长战斗多次唤醒误 trip）

**fallback** (7)

- `XiuluoTaskV2` — CR220 返回道具兜底：guardCloudAcceptNavigationWithStartupReturn 仅 hot-start round==1 且不在灵兽村时，用返回道具→WAIT_TEAM_RETURN/WAIT_COMBAT/FAILED/放行；round>1 跳过（基线对齐，省 syncMyPosition）
- `XiuluoTaskV2` — 本地 yield guard：executeXiuluoBrainCommandShell 内 while<8 inline 重执行上限，超出=REJECTED（防同 phase 内联死循环）
- `XiuluoTaskV2` — 热循环 guard：noteCommandCycleAndCheckExceeded 阈值 33（基线>32 等价）；真实 WAIT_FOR_EVENT park 完成会重置计数；trip→loopGuardTripped 事实→云端 RESTART_ROUND
- `XiuluoTaskV2` — shortcut→objective 兜底 fallbackFromShortcut：MAX_ENTER_BATTLE_CONFIRM_RETRIES=2 / MAX_CLOUD_ENTER_BATTLE_FALLBACKS=3 耗尽后放弃 tracker 快捷路线走 recovery 链
- `XiuluoBrainRoundState` — pendingWaitSpec：phase 产出的 scheduling-only waitSpec 暂存，由 WAIT_FOR_EVENT 命令 consumePendingWaitSpec 取用
- `XiuluoHotStartResolver` — STORY_DIALOG 经白模板 xiuluo_story_miexiu_confirm 验证：命中→READ_OBJECTIVE，未命中→回退 XiuluoRoundContext.start(round) 走正常接任务链（避免对非 objective 截图长时 OCR）
- `XiuluoHotStartResolver` — NONE 状态不证明有未完成修罗任务，一律从 start(round) 接任务链起步（不 hot-jump READ_OBJECTIVE/NAVIGATE_TO_TARGET）

**other** (7)

- `XiuluoTaskV2` — retry/recovery 预算常量：MAX_PHASE_RETRY=1、MAX_RECOVERY_COUNT=2、MAX_CONSECUTIVE_ROUND_FAILURES=10（CR230 后仅诊断不再终止任务）、MAX_MAINTENANCE_HOOK_ATTEMPTS=5、ENTER_BATTLE_CONFIRM_NONE_TICKS=4
- `XiuluoWaitReason` — 5 个 park reason 即本地→云端的 event-park 白名单锚：WAIT_COMBAT_STATE_CHANGE / WAIT_TARGET_PATHING_TERMINAL / WAIT_TRACKER_SHORTCUT_PATHING / WAIT_TEAM_RETURN_STATE_CHANGE / WAIT_MAINTENANCE_BROADCAST_QUEUE
- `XiuluoWaitSpec` — afterSequence 语义关键：须在返回 wait outcome 前捕获 bus sequence，避免让权与 park 之间的事件丢失（CR244/245 review P2 依赖此字段）——本身仅承载，值在 XiuluoTaskV2 里设
- `XiuluoRouteMode` — routeMode 值改变 watchdog 锚、post-combat recovery policy、shortcut 复按/放弃分支——枚举本身无逻辑，但作为 B 决策输入不可视作纯搬运
- `XiuluoCombatSource` — combatSource 区分决定：是否广播队伍战斗相(CR252)、post-combat recovery policy(FAST_EXPECTED_EXIT vs FULL_RECOVERY)、退战后回 shortcut 还是 RETURN_HOME/unknown-exit——是 B 决策输入
- `XiuluoStepOutcome` — facts map 承载云端 keying 事实（watchdogTimeout/combatObserved/returnGate/maintenanceQueueWaitSpecArmed 等）；failed()/stopped() 工厂把 nextState phase 置 FAILED/STOPPED
- `XiuluoDialogCatalog` — ENTER_BATTLE_TEMPLATE=xiuluo_enter_battle_kanda.png，被 XiuluoTaskV2/PreparationProvider 共同引用；识别阈值/偏移属 D6 位置搬迁范畴

</details>

### 2.5 service/ A–M (THIN_CLIENT_V1 A-1 方法级迁移矩阵底账)

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `AutoCombatPanelService` | 自动战斗面板的可见性/对齐/剩余回合估算与刷新的所有权者（Alt+8 无OCR刷新） | B | 本地：面板对齐状态 + 回合估算计数器（存于 GameContext）+ 面板缺失看门狗 + 队伍刷新突发闸 | 云端 AutoCombat/Combat 编排 Service（回合估算与刷新时机决策） | Alt+8 按键 executor + auto_remaining 模板截图定位 + 面板拖拽 executor |
| `AutoCombatService` | 每窗口自动战斗单tick编排：战斗探测授权(lease)、队长/队员覆盖状态机、退出恢复、急救FIFO队列 | A | 本地：战斗探测临时授权(CR252 lease)、队员覆盖 epoch/read-only 粘滞、各类 pending 恢复标志、身份 epoch 漂移失效 | 云端 AutoCombat/Combat 编排 Service（tick 决策、恢复策略、队伍相位消费） | 无独立本地能力——全部委托 BattleRadar 截图/AutoCombatPanel 的 Alt+8 executor；本类只做决策编排 |
| `BagService` | 背包开/找/选/用物品：模板匹配 + 分页导航 + 独占输入会话；含物品页缓存 | C | 本地：物品所在页/背包锚点缓存、分页搜索结果 | 多为本地 executor 保留；'用物品X'指令可由云端任务 Service 下发 | 背包 executor（Alt+E 开包、tab点击、物品/给予点点击）+ 背包模板截图匹配 —— 纯搬运执行层 |
| `BattleRadarService` | 战斗状态探测与 enter/exit 一次性信号状态机：多信号确认+保守退战+快速预期退战头像diff | A | Client 仅保留 exact-window 纯机械能力：三组正向模板在内存匹配并产出 `COMBAT_SIGNAL`；Fast Expected Exit 头像 diff 仍是既有本地快速事实 | Cloud 唯一持有 `BattleRuntimeState`、enter/exit、连续 miss、防抖、phase/wakeup 与坐标退战解释 | `combat-flag/selection/top` 不再上传 PNG；`coordinate-strip` 仅在 active pathing 或 combat-exit fallback 时按需上传；头像 ROI 保持既有本地探针 |
| `ClientIdentityService` | 从绑定原生窗口标题解析 server/name/id 同步进玩家状态 | A | 本地：绑定窗口标题（多窗口优先当前 WindowRuntimeContext binding） | 身份/会话 Service（或因标题为OS原生而保留本地读取） | 原生窗口标题读取（native capture）+ WindowTitleIdentityParser 解析 |
| `CommonBoxService` | CR120 通用宝箱探测 + 短时 pending-click 所有权者（探测与点击分离，异步ROI匹配） | B | 本地：pendingByKey 每窗口/句柄/角色/任务/run 的短时 pending 宝箱点击记录 | 云端 Maintenance/Combat Service | 小ROI截图 + 模板匹配 + 点击 executor |
| `DialogChoiceMemoryService` | 可复用选项对话点击的持久记忆（对话相对坐标，非屏幕绝对像素）+ 路由转移键策略 | B | 本地 JSON：config/dialog_choice_memory.json（+ 旧 transfer_choice_memory.json 迁移） | 云端 Dialog 记忆 / DialogPolicy 决策 Service | 无——纯记忆存储，整体应迁云 |
| `DialogService` | 对话总控：STORY/OPTION/NONE 分类、云端预点击委托、修罗进战本地模板回退、剧情目标OCR、绿/白模板校验 | B | 混合：选项决策已委托云端 DialogPolicyCloudDecisionService；对话类型分类与修罗进战本地模板仍在本地 | 云端 DIALOG_POLICY 决策 Service（dialogPolicyCloudDecisionService 已接入） | 对话ROI截图 + 点击 executor + 剧情目标裁剪 + 图像预处理wash |
| `MemoryService` | 持久化自动化记忆的单一门面（当前仅转发 DialogChoiceMemoryService） | D | 委托 DialogChoiceMemoryService（本地 JSON） | 云端 Dialog 记忆 Service | 无——纯门面转发 |
| `GiveItemService` | 物品给予业务流程：背包选中目标物品 + 点击给予按钮（含 input-worker 线程独占分支） | C | 本地流程编排（依赖 BagService 选物） | 本地 executor 保留 / 或云端任务 Service 下发'给予物品X' | BagService 选物 + btn_give 模板匹配点击 executor |
| `LeftTopStatusSwitchService` | 左上状态开关探测与关闭（仅修罗/五倍/五环 启动与战斗维护），仅 OPEN 模板才点击 | A | 本地：开关状态 + 每窗口 leftTopStatusSwitchClosePending 标志(存于 WindowRuntimeContext) | 云端 Maintenance/Combat Service | 窗口相对ROI截图 + OpenCV 模板匹配 + 屏幕绝对点点击 executor |
| `MapNameCanonicalizer` | 将 OCR 读取的地图名对本地已知地图名字典做规范化纠正 | C | 本地：map_label 模板目录 + TRANSFORM_ONLY_MAP_NAMES 常量字典 | 云端 Vision/Map Service（云端已持有 maps.json transform 快照） | 字符串编辑距离匹配器 + 本地 images/template/map_label 目录读取 |
| `DialogHandleRequest` | 对话处理请求 DTO：承载 operation + story/option/fallback 三策略 + 模板/关键词/记忆点等入参 | D | 调用方构造（静态工厂预设策略组合） | 随 DialogService 云端 DIALOG_POLICY（策略预设应上移为云端策略表） | 无（DTO） |
| `DialogOptionClickResult` | 对话选项点击结果 DTO（含相对/绝对点、匹配文本、可复用 PreparedDialogAction） | D | DialogService 产出 | 随 DialogService 云端 | 无（DTO） |
| `DialogOperation` | 对话操作枚举（INSPECT/GIVE_ITEM/CLICK_KEYWORD/ROUTE_TRANSFER/WUBEI_*/XIULUO_*等业务动作词表） | D | 枚举常量 | 随 DialogService 云端策略词表 | 无（枚举） |
| `DialogOptionPolicy` | 选项处理策略枚举（IGNORE/VERIFY/CLICK_KEYWORD/CLICK_REMEMBERED_POINT/FALLBACK_FIRST/LAST 等） | D | 枚举常量 | 随 DialogService 云端策略 | 无（枚举） |
| `DialogStoryPolicy` | 剧情处理策略枚举（IGNORE / CLICK_THROUGH） | D | 枚举常量 | 随 DialogService 云端策略 | 无（枚举） |
| `DialogFallbackPolicy` | 对话兜底策略枚举（RETURN_UNRESOLVED / CLICK_FIRST_OPTION） | D | 枚举常量 | 随 DialogService 云端策略 | 无（枚举） |

<details><summary><b>隐式状态清单</b>（43 条，按 kind 分类）</summary>

**timer** (9)

- `AutoCombatPanelService` — AUTO_PANEL_MISSING_ATTENTION_MS=10分钟 面板连续未识别看门狗→error+人工注意；AUTO_PANEL_MISSING_ATTENTION_REPEAT_MS=60s 重复节流
- `AutoCombatPanelService` — REFRESH_DUE_TEAM_BURST_GUARD_MS=30s TeamRefreshDueBurstGuard 按 teamKey 防抖，防止全队同刷
- `AutoCombatPanelService` — AUTO_PANEL_REFRESH_WAIT_MS=1000 刷新后等待；refreshIntervalMs 来自 botProperties.autoBattleRefreshIntervalMs（定期刷新）
- `AutoCombatService` — COMBAT_ENTRY_MAINTENANCE_DELAY_MS=4s 进战后延迟入场维护；COMBAT_UI_CLEAN_INTERVAL_MS=40s；REFRESH_DUE_PANEL_VERIFY_GUARD_MS=30s；URGENT_ROUNDS_PANEL_VERIFY_RETRY_MS=30s；REFRESH_DUE_DEFERRED_LOG_INTERVAL_MS=10s
- `BagService` — 开包/渲染 settle：BAG_OPEN_WAIT_MS=1200、BAG_LATE_RENDER_WAIT_MS=700、BAG_TAB_CLICK_WAIT_MS=500
- `BattleRadarService` — REQUIRED_COMBAT_EXIT_MISSES=2 连续缺失阈值 + 小地图坐标可读才允许退战（双重保守闸）
- `BattleRadarService` — 快速预期退战：PROBE_DELAY=15s 起测、PROBE_INTERVAL=1s、FULL_RADAR_INTERVAL=4s 回退全雷达、AVATAR_ROI=20px、DIFF_RATIO=0.35
- `CommonBoxService` — PENDING_TTL_MS=30s pending 过期；pruneExpiredPending 每次清理
- `GiveItemService` — 流程 settle：起始 800ms、给予点击后 1000ms

**memory** (7)

- `AutoCombatPanelService` — runtimeStates ConcurrentHashMap 按 windowId 存 panelAligned/autoPanelMissingSinceAt/lastAutoPanelMissingAttentionAt
- `AutoCombatService` — 队员覆盖状态机：memberCoveredByLeader / memberReadOnlySelfObserve(粘滞，仅新entry广播epoch可解除) / lastLeaderCombatPhaseEpochId
- `AutoCombatService` — pendingCombatEntryMaintenanceAt / pendingFollowerFirstAid / pendingLeaderPostCombatRecovery / expectedCombatExitWaitArmed / fastExpectedExitWatchArmed（runtimeStates 按 windowId）
- `BattleRadarService` — BattleRuntimeState：battleCount、combatEnterPending/combatExitPending、combatExitPendingAtMs/armedAtMs 陈旧信号边界、combatExitAfterUnconsumedEnter*、combatExitObservedDuringPause*（暂停期观察）
- `CommonBoxService` — pendingByKey ConcurrentHashMap（PendingCommonBox 记录，含 templateX/Y、clickX/Y、identityEpoch、taskRunKey）
- `DialogChoiceMemoryService` — JSON 文件 + 内存 cache（scope\|action\|contextKey 键，对话相对 relativeX/Y）；load 时旧 transfer 键迁移
- `LeftTopStatusSwitchService` — WindowRuntimeContext.leftTopStatusSwitchClosePending 每窗口关闭待办标志（probe标记/consume消费/mark重挂）

**cache** (8)

- `AutoCombatPanelService` — 回合估算计数器：DEFAULT_ESTIMATED_ROUNDS=25，每次战斗退出 recordCombatExit 减 3，低于 LOW_ROUNDS_REFRESH_THRESHOLD=10 触发刷新；存于 GameContext.autoCombatEstimatedRounds + lastAutoCombatRefreshAt（无OCR的隐式回合模型）
- `AutoCombatService` — playerIdentityEpoch 漂移检测：身份变化即整块重置该窗口 runtime state
- `BagService` — itemPageCache（物品→页记忆）、visiblePageCache、lastMainBagAnchorCache（按窗口的主背包锚点记忆），加速下次定位
- `BattleRadarService` — fastExpectedExitBaselineImage 头像ROI基线图（可被 trusted in-combat 刷新替换）
- `CommonBoxService` — cachedTemplate 双检锁缓存宝箱模板
- `DialogChoiceMemoryService` — consecutiveSuccessCount/consecutiveFailureCount/successCount/failCount/disabled 计数决定 isUsable/isStableTaskChoice
- `DialogService` — 图像 wash / cloudBinaryFingerprint 指纹距离 + preparedDialogFingerprintMaxDistance 用于 prepared action 复用前重校验
- `MapNameCanonicalizer` — cachedMapNames AtomicReference 懒加载后内存缓存（模板名 + 常量）

**lock** (2)

- `AutoCombatService` — combatDetectionAuthorized(volatile) 是 lease 式临时探测授权：仅 xiuluo_v2/wubei 需授权，enter-battle 动作成功后授权、return-home 验证后 revoke，下一轮须重新授权（mayRunBattleRadar 门禁）
- `AutoCombatService` — taskTurnCoordinator 公平 task-turn 队列：pending 急救 / common-box 阻塞式入队按序执行；RefreshDuePanelVerifyGate 按队伍30s公平闸

**fallback** (9)

- `AutoCombatPanelService` — 拖拽后重新找不到面板时用 drag-target-fallback 假设面板已在目标点（AutoCombatPanelMatch drag-target-fallback）
- `AutoCombatService` — leader-paused-fallback：队长暂停/停止/绑定丢失时队员退化为纯只读自雷达（不消费enter、不入场维护、不发Alt+8）；deferLeaderRecovery 延迟队长恢复(FAST_EXPECTED_EXIT)
- `BagService` — 主背包锚点回退链：anchor_huanzhuang → MAIN_BAG_TAB_FALLBACK_TEMPLATES → anchor_cunkuan；找不到即认包未开
- `BattleRadarService` — IN_COMBAT 时任一区域截图失败→保持 IN_COMBAT 防误退（select/top 两处）
- `ClientIdentityService` — 标题解析优先级回退链：当前 window binding → tracker 缓存标题 → locateWindow() 重定位；均空则跳过同步
- `DialogService` — tryHandleXiuluoEnterBattleLocalTemplate 在云端决策之前先跑本地模板并可直接点击——关键本地大脑残留(迁移必须处理)
- `DialogService` — DialogFallbackPolicy.CLICK_FIRST_OPTION / DialogOptionPolicy.FALLBACK_FIRST_OPTION/LAST_OPTION 兜底点击策略；acceptTask 等预设组合
- `MapNameCanonicalizer` — TRANSFORM_ONLY_MAP_NAMES=[天宫,御马监] 常量兜底：无 map_label 模板但云端有 transform 的地图，供删除本地 maps.json 后仍能纠错
- `DialogFallbackPolicy` — CLICK_FIRST_OPTION 是显式兜底点击语义，实际兜底行为在 DialogService 消费该值时发生

**other** (8)

- `CommonBoxService` — 陈旧性多闸：expired/staleWindow(hwnd)/staleIdentity(epoch)/staleTaskRun 任一命中即弃；探测走 CompletableFuture.runAsync 异步边界(CR235)
- `DialogChoiceMemoryService` — 禁用/稳定阈值：MAX_FAILURES_BEFORE_DISABLE=3 连败禁用；DEFAULT_STABLE_SUCCESS_STREAK=3 任务选项需连续成功streak才可复用（严于路由记忆）
- `DialogService` — 本地视觉分类决策：hasStoryInUpperHalf/hasOptionInLowerHalf/hasDialogMask OCR启发式判 STORY vs OPTION vs NONE（影响后续A/B分支，非纯观察）
- `GiveItemService` — isInputWorkerThread 按线程名 'dhxy-input-action-worker' 切换独占直连 executor vs 提交序列化输入队列
- `LeftTopStatusSwitchService` — 匹配阈值：MATCH_RATE=0.90 + MARGIN=0.02；仅 OPEN 且分领先才可点，CLOSED/UNKNOWN/CAPTURE_FAILED 不点
- `LeftTopStatusSwitchService` — 支持任务闸：仅 xiuluo_v2/wubei/wuhuan_v2，其余 SKIPPED
- `MapNameCanonicalizer` — 安全纠正阈值：距离≤1直接改；wubei-tracker-green-map 源特例(len≥3,dist≤2,次优领先1)；否则 len≥4 允许dist2 且次优领先2，模糊则返回原文
- `DialogHandleRequest` — 静态工厂编码策略预设映射：如 acceptTask→CLICK_THROUGH+FALLBACK_FIRST_OPTION+allowFallback、handleKeywordOption 按 allowFallback 选 CLICK_FIRST_OPTION vs RETURN_UNRESOLVED——策略即数据表，迁移时应随云端策略搬移

</details>

### 2.6 service (N–Z) + service/dialog — THIN_CLIENT_V1 A-1 method-level migration ledger

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `NavigationService` | 跨图/本图寻路的输入执行 + 寻路意图注册 + 身份/lease 时效闸门；六段路线阶梯已交云端编排 | A | 路线阶梯顺序与终局=云端 NavigationRoutePlanCloudDecisionService；mini-map 候选点=NavigationPointCloudDecisionService；本地持有：pathing-intent 注册权、身份/lease 时效闸门、stop-checkpoint、terminal-fact-gate、幂等执行台账 | NavigationRoutePlanCloudDecisionService(路线阶梯) + NavigationPointCloudDecisionService(本图mini-map候选) + RouteCloudDecisionService/NavigationRoutePlan(世界地图搜索) | 世界地图输入序列执行(exclusive)、mini-map 点击执行、截图/OCR capture、pathing-intent 注册、terminal-fact-gate 本地事实复核、stop-pause 检查点 |
| `NpcClickService` | TURN-28 HTTPS turn 迁移中的 Cloud 业务 service；当前父级 Review #1 `0/2/1`，production 冻结、唯一 named test Repair #3 | A | Cloud 持有 strict-696 learned-memory/tooltip/yellow/purple/Ctrl 顺序、验证与 proof；不再使用 legacy session/queue/full fallback | 每个 capture/input 通过 exact-window HTTPS turn action；source pass 后供 TURN-26/27 只读 shared API | DHXY 仅保留 exact-HWND capture/input executor、原子 move+click、Ctrl pixel probe 与背景按键 mechanics |
| `PlayerStateService` | 当前窗身份/位置/血法补给/摄妖香状态维护；HP/MP 补给阈值判定仍为本地像素分析 | B | 摄妖香在场/剩余/刷新=云端 SheyaoxiangStatusCloudDecisionService；但 HP/MP 血法条阈值判定、急救计划、no-focus 预检=本地像素分析(本地大脑) | SheyaoxiangStatusCloudDecisionService(摄妖香)；HP/MP 补给判定尚无云端 owner——迁移缺口 | 身份/位置 sync(identityService+locationRadar)、血法条截图/像素健康度分析、exclusive 补给点击执行、摄妖香 quiet-period 计算与用香执行、安全鼠标移开 |
| `QuestManagerService` | 任务情报面板读取与激活/详情截图；本地模板匹配+高亮像素判定 | B | 本地(无云端)：任务标签模板匹配、glow 高亮判定决定激活分支、面板开合/滚动/详情截图全在本地 | 无(尚未迁移)——任务标签识别属可迁 OCR/模板，激活分支属业务判断 | Alt+Q 开面板、当前任务页 tab 选择、任务标签模板匹配、isTextGlowing 高亮判定、激活点击、详情 ROI 截图与落盘 |
| `ReturnItemPrescanService` | 回城前每轮回程物品预扫调度器：随机策略选择+多时机(绿字后/后台寻路/战斗中)+缓存点击点+降级兜底 | B | 本地(无云端)：策略随机选择、战斗定时、缓存/降级状态机全在本地(纯执行的截图/匹配委托 BagService) | 无——策略选择/时机/降级属业务决策与定时，是迁移重点缺口 | 调用 BagService 截图/匹配/缓存点击(本身为纯调度状态机) |
| `SmartClickEvidenceConfirmationService` | 接口：智能点击证据的提交边界(后续对话选项证明 NPC 点击有效后 commit proof) | A | 接口本身无实现；语义=证据/proofToken/verificationStrength 的提交权威(实现方另处，多半云端记忆) | 实现推测归属云端 NPC 记忆/证据服务(需另查实现类确认) | 无(仅接口契约) |
| `SummonSkillService` | 召唤兽技能尾槽清理本地大脑：开面板/静态槽扫描/删普通技能/绝技角标生成/锁定边界回扫 | B | 本地主导：槽位状态分类、删除决策、绝技生成、边界回扫、6/8槽布局检测全本地；仅 inspectPostDeleteSlot 可选走云端 | SummonSkillCloudDecisionService(仅 post-delete 槽静态分类，且 isActive 才用)；主清理决策链未迁移 | Alt+O 开面板/拖面板/点技能页、静态槽模板/色距分类、yellow tip hover 判级、删除+确认点击、绝技角标 hover+点击生成、锁定尾边界回扫 |
| `SummonSkillTailBoundaryScanner` | 无状态算法：锁定尾槽的向后边界规则(找到最近开启槽并判删/保/绝技检查/安全停) | B | 纯规则算法(输入 inspector/deleter/abort 回调)——业务规则内嵌本地 | 无——规则语义可随 SummonSkill 决策链一起上云 | 纯算法本体(无 I/O、无状态) |
| `SummonSkillTailBoundaryScanner.Result` | 边界回扫结果 record(success/nextStartIndex/inspected/deleted/ultimateCheckIndex/message) | D | 纯值对象 | N/A | 无 |
| `SystemPowerService` | 宿主级电源动作执行器(请求 Windows 睡眠)，仅显式用户任务调用 | D | 纯本地宿主命令，无业务决策；不含状态 | 不迁移——本地宿主 side-effect 执行器 | 整个类(rundll32 SetSuspendState 执行) |
| `TaskMaintenanceService` | 多窗队伍维护调度中枢：召唤兽队列/战后急救FIFO/维护广播队列/本地队伍会话/战斗phase/待归队集合 的公平锁与冷却编排 | A | 本地持有大量跨窗协议/公平锁/lease/队列顺序权威；仅阈值/能力门/召唤兽清理走云端 shadow/decision | MaintenanceThresholdCloudDecisionService(维护阈值) + CapabilityGateCloudDecisionService(能力门) + RuntimeDecisionShadowService(影子)；召唤兽清理委托 SummonSkillService；队列/会话编排未迁移 | 各类公平锁队列 FIFO 顺序、TTL 缓存、冷却计时、本地队伍会话状态机、维护广播 ROI 点击执行 |
| `TaskTrackerPanelService` | 左侧任务追踪面板只读器：截图+模板/OCR 定位绿字链，从不发输入 | C | 面板文本/绿字链读取已交云端 TrackerPanelReaderCloudDecisionService；本地做 anchor 定位、裁剪、指纹缓存命中 | TrackerPanelReaderCloudDecisionService(面板读取) + TaskClassifierCloudShadowService(分类影子) + ImageProcessorService(预处理) | 面板 anchor 模板定位、面板/详情 ROI 截图与裁剪、指纹计算、准备 PreparedDialogAction 供任务层稍后经队列消费 |
| `TeamReturnService` | 队伍归队信号检测与处理：队员见按钮点归、队长等信号消失(截图匹配,点击走队列) | B | 本地(无云端)：归队按钮/信号模板匹配、队员点/队长等 phase 决策、tri-state marker 探测全本地 | 无——归队信号识别属可迁 OCR/模板，点/等 phase 属业务决策 | 归队按钮/信号区截图匹配、点击前 ensureSheYaoXiang、点击执行、异步 pre-return 截图分析 |
| `UICleanerService` | 非任务 UI 打断清理：关世界地图/通用X窗口/对话框兜底(story 快点门控+fallback 末选项) | B | 本地(委托 DialogService)：清理策略、story 快点门控(member+combat)、fallback 末选项决策本地 | 对话处理委托 DialogService(其内部含云端)；清理编排/门控本地未迁 | 世界地图/通用关闭模板匹配与点击、CleanupPass 单帧缓存、story 快点门控、Alt+1 关地图执行 |
| `DialogOperation (enum)` | 对话处理操作类型枚举(INSPECT/CLICK_KEYWORD/ROUTE_TRANSFER/WUBEI_*/ACCEPT_TASK 等) | D | 纯枚举类型;语义(路由分支)由 DialogService/调用方消费,属 B 级但类型本身是协议常量 | 语义随 DialogService 决策链上云;枚举作协议 DTO 保留 | 枚举定义 |
| `DialogOptionPolicy (enum)` | 选项处理策略枚举(CLICK_KEYWORD/CLICK_REMEMBERED_POINT/VERIFY_*/FALLBACK_* 等) | D | 纯枚举;编码 B 级选项决策路由,但类型本身为协议常量 | 语义归 DialogService/云端 | 枚举定义 |
| `DialogStoryPolicy (enum)` | 剧情对话策略枚举(IGNORE/CLICK_THROUGH) | D | 纯枚举协议常量 | 语义归 DialogService/云端 | 枚举定义 |
| `DialogFallbackPolicy (enum)` | 对话兜底策略枚举(RETURN_UNRESOLVED/CLICK_FIRST_OPTION) | D | 纯枚举协议常量;编码 fallback 语义(B级)但类型本身为常量 | 语义归 DialogService/云端 | 枚举定义 |
| `DialogHandleRequest (DTO/builder)` | 对话处理请求 DTO：承载 operation/story/option/fallback 策略+关键词/模板/记忆点等调用意图 | D | 纯请求 DTO(含大量静态工厂封装各调用场景的策略组合) | 作为请求协议保留;其策略语义由 DialogService/云端消费 | DTO+builder 工厂 |
| `DialogOptionClickResult (DTO)` | 对话选项点击结果 DTO(status+relative/absolute 坐标+matchedText+preparedAction) | D | 纯值对象;hasLearnableClickPoint 只是判定 helper | N/A | DTO |

<details><summary><b>隐式状态清单</b>（54 条，按 kind 分类）</summary>

**timer** (12)

- `NavigationService` — 大量 route-dialog 时效阈值常量：ROUTE_DIALOG_REQUESTED_YIELD_MAX_MS=3s、PREPARING=30s、VISIBLE_GATE=10s、ACTIVE_INTENT_GATE=60s、ARRIVAL_CONFIRM_TIMEOUT=2.5s、MINI_MAP_PATHING_CONFIRM_TIMEOUT=1.5s、LING_SHOU_ROUTE_CONFIRM=20s
- `NavigationService` — navigateInCurrentMap 60s 总超时 + keep-turn 短寻路 min(10s) 等待 + 200ms 重试轮转 + 250ms keep-turn 轮询
- `NpcClickService` — NPC_CLICK_SMART_QUEUE_WAIT_TIMEOUT_MS=30s WAIT 超时；WAIT sleep=100ms；候选预算 NPC_CLICK_SMART_QUEUE_CANDIDATE_LIMIT=12
- `NpcClickService` — NPC_CLICK_SMART_STORY_BLOCKER_RESTART_LIMIT=3 剧情阻挡快点重启上限，超限 fail-closed 回既有恢复链
- `PlayerStateService` — 摄妖香 quiet-period：INCENSE_DURATION=59min、REFRESH_REMAINING=20min、QUIET_MARGIN=2min，lastIncenseUsedTime>0 时静默期跳过全部云端检查
- `PlayerStateService` — MAX_CHECKS_BETWEEN_BATTLES=1 战后每空闲期只查一次；HEAL_TIME_INTERVAL=5000ms 间隔；HEAL_CONFIRM_DELAY=350ms;补给点击后 800ms settle
- `QuestManagerService` — 节奏常量 SLOW=800/MID=500/FAST=200ms；最多 3 页滚动查找(for p<3)
- `ReturnItemPrescanService` — 战斗时机 combatDueAtMs = now + COMBAT_ENTRY_MAINTENANCE_MS(4s) + random(8s~18s) 抖动
- `SummonSkillService` — CLEAN_ONCE_TIMEOUT_MS=40s 单次清理绝对 deadline；多处 isCleanDeadlineExceeded 检查
- `SummonSkillService` — 大量 hover/settle 常量：ULTIMATE_CORNER_CLICK_WAIT=2.5s、SKILL_HOVER=700ms、OPEN_PANEL=1s、DRAG=600ms、DELETE_DIALOG=600ms 等
- `TaskMaintenanceService` — 冷却/计时：lastSummonSkillCleanAtByWindow、SUMMON_SKILL_DUE_LEAD_TIME=90s、summonSkillUnknownRetryAfterByWindow(unknown 退避)、各类 NO_ACTION/NOT_DUE 日志节流(60s)
- `TeamReturnService` — 队长等待 DEFAULT_LEADER_WAIT_TIMEOUT=120s + POLL=3s；waitForMembersReturnIfNeeded 轮询到信号消失或超时

**memory** (8)

- `NavigationService` — runtimeStates(ConcurrentHashMap<windowKey,NavigationRuntimeState>) 世界地图路线结果点击/待决 route outcome 记忆(rememberPendingRouteOutcome/PendingRouteOutcome、PendingTransferChoiceMemory)
- `NavigationService` — CloudMiniMapBatchState 预取候选批 cursor + intentBaselineId(外部 pathing-intent 变化即作废批) + identityEpochBaseline
- `NpcClickService` — story-blocker 事件序列消费：storyEventAnchorSequence(每次智能点击抓一次) + lastConsumedStorySequence(每序列只消费一次)
- `PlayerStateService` — runtimeStates(ConcurrentHashMap<windowKey,PlayerRuntimeState>)：lastIncenseUsedTime、checksDoneThisRound、lastCombatExitTime、pendingNoFocusFirstAidPlan、startupFirstAidPrecheckResult/AtMs
- `QuestManagerService` — 详情图落两份 latest+带时间戳 history(windowScopedTempPath)
- `ReturnItemPrescanService` — states(ConcurrentHashMap<PrescanKey,PrescanState>)，key=taskCode\|windowId\|hwnd\|taskRunId\|round\|template；含 cachePoint、done、inProgress、combatFallback、combatDueAtMs、strategy
- `TaskMaintenanceService` — localTeamSessions(会话状态机)、activeTeamRoundByKey、teamMaintenanceWindowStateByRound、pending team return 集合、team combat phase(openTeamCombatPhaseForLeader/memberTeamCombatPhase)
- `TeamReturnService` — per-window map：lastNoMatchLogAtByWindow(10s 节流)、lastReturnButtonFoundAtByWindow、lastReturnButtonClickedAtByWindow

**cache** (10)

- `NavigationService` — routePlanExecutionLedger(每步 windowId\|hwnd\|taskRunId\|routePlanRequestId\|stepId 幂等重放台账，仅本次 navigateToMap 生命周期，不落盘)
- `NavigationService` — miniMapClickExecutionLedger(每次云解析 mini-map 点击的幂等台账，仅本次导航调用生命周期)
- `NavigationService` — confirmCurrentMapFromRecentPathingSnapshot 复用 WindowPathingSnapshot，RECENT_PATHING_SNAPSHOT_MAX_AGE_MS=1500ms 新鲜度窗口
- `PlayerStateService` — pendingNoFocusFirstAidPlan：no-focus 预计算的血法补给计划(targets+baseX/Y+createdAt)，供后续 focused turn 直接消费
- `PlayerStateService` — startupFirstAidPrecheck：启动急救 no-focus 预检结果 + maxAge 新鲜度门
- `ReturnItemPrescanService` — cachePoint(ReturnItemCachePoint) 学到的背包物品点，供 useCached 复用；失败即 invalidate
- `TaskMaintenanceService` — TTL 缓存：SUMMON_SKILL_TAIL_SAFE_CACHE_TTL=2h、COUNT_CACHE_TTL=2h、COMPLETED_LOCAL_TEAM_SESSION_TTL=2h(最多256墓碑)、IDLE_BROADCAST_SUPPRESS_TTL=30s
- `TaskTrackerPanelService` — wuhuan 面板指纹缓存存于 WindowRuntimeContext.getTaskTrackerPanelCache()(TaskTrackerPanelCacheEntry：taskCode/fingerprint/panelOrigin/size/click)，per-window 命中免重扫
- `TeamReturnService` — beginLeaderSignalPrecheck：pre-return 区截图 + 异步 CompletableFuture 分析，scoped(windowId/hwnd/taskRunId) 消费,stale/未完/失败=inconclusive 回退实时
- `UICleanerService` — CleanupPass.screenPath 单次清理帧缓存(关窗后 invalidateFrame 失效)，避免同 pass 重复截图

**lock** (8)

- `NavigationService` — inputSequences.submitExclusiveAndWait 世界地图搜索独占输入段，保证一个窗口点完再让其它窗口点
- `NavigationService` — isRoutePlanIdentityStale：请求前与执行前双重 windowId/hwnd/taskRunId/epoch 身份闸门(fail-closed STALE_REJECTED)，同 hwnd relog 的 epoch 漂移检测
- `PlayerStateService` — healAll/performCachedFirstAidPlan 走 submitExclusiveAndWait 独占段，人物/宝宝 HP/MP 一次点完不被别窗插入
- `SummonSkillService` — isInputWorkerThread 判定：worker 内走 Direct，否则 submitExclusiveAndWait 独占；避免嵌套队列死锁
- `TaskMaintenanceService` — postCombatFirstAidMonitor + postCombatFirstAidQueueByScope：战后急救 per-team FIFO 队列(头窗先补)，scope=会话key或窗口key
- `TaskMaintenanceService` — summonSkillQueueMonitor + summonSkillQueue(ArrayDeque) + summonSkillQueueKeys：召唤兽清理公平排队，含 retry-backoff 移尾
- `TaskMaintenanceService` — 维护广播队列(openMaintenanceBroadcastQueue/consumeIfHead/isDrained)：头窗消费轮次的公平锁
- `UICleanerService` — canFastClickStoryDialog：member 仅在 IN_COMBAT 才允许快点剧情(队员安全门)

**fallback** (8)

- `NavigationService` — 云端不可用/echo 不符/terminal 无本地事实支撑 => 一律 MAP_NOT_REACHED 结构化失败(fail-closed)
- `NpcClickService` — 云端 inactive/不可用/无可执行动作/校验失败/FIFO END(CLOUD_NO_ACTION) 全部 fail-closed，不回退旧本地策略链；仅 CLOUD_NO_ACTION 映射 normalFifoConsumedUnverified 授权 direct-combat
- `PlayerStateService` — no-focus 截图不可读 => UNKNOWN 缓存 conservative plan(补给所有启用条)；bars 判定 near-threshold/inconsistent-sample 多重像素兜底
- `ReturnItemPrescanService` — chooseStrategy 从可用候选(绿字后/后台寻路/战斗随机/SKIP)随机抽一个；后台机会错过或先前策略失败=>降级 combatFallback 进战斗时机
- `SummonSkillService` — 槽状态 UNKNOWN 一律 fail-closed 不刷新长冷却；静态槽扫描任一 UNKNOWN 即失败
- `TaskMaintenanceService` — cloudRequiredMaintenanceFailure：阈值云端 required 失败即结构化失败；能力门/特性旗标走 shadow 上报
- `TeamReturnService` — probeMemberReturnMarker tri-state：仅成功截图+分析确认才 ABSENT；capture/analysis 失败=UNKNOWN 保持 pending 不变(防误判归队完成)
- `UICleanerService` — forceCloseDialog：STORY_IGNORED->canFastClickStoryDialog 门控才快点;否则 fallbackLastOption 末选项兜底

**other** (8)

- `NpcClickService` — combatClickVerifier：点击后 4 次×350ms 轮询 checkAndSyncCombatState 判定战斗可见
- `QuestManagerService` — isTextGlowing：40x20 ROI 中 RGB>220 像素计数>GLOW_TARGET=15 判已高亮(视觉阈值)
- `ReturnItemPrescanService` — MAIN_BAG_TASK_PAGE 模式异步 CompletableFuture 匹配；completeRound 清 key
- `SummonSkillService` — MAX_DELETE_SKILL_COUNT_PER_RUN=5 单次删除上限；handledBusinessDialogs<3 期间让维护广播弹窗优先
- `TaskMaintenanceService` — windowReadyEventBus 软唤醒总线发布队伍归队/队列/phase 状态变更(field-injection 可空守卫)
- `TaskTrackerPanelService` — WUHUAN_PANEL_CACHE_MAX_FINGERPRINT_DISTANCE=1 仅接受完全相同/极轻噪点；面板几何变化即 miss；WUBEI_CHAINED_FAST_FINGERPRINT_MAX_DISTANCE=8 链式快匹配
- `TaskTrackerPanelService` — 五备各任务黄标题模板表、绿字链像素分割阈值(MIN_PIXELS=20/SPLIT_GAP=8/坐标字形阈值)等视觉常量
- `UICleanerService` — closeAllGenericWindows 最多 3 次循环关 X 窗；关闭模板序 x1/x2/x3/npc_busy_cancel

</details>

### 2.7 vision/ (全部) + model/navigation/ (全部) — THIN_CLIENT_V1 A-1 方法级迁移矩阵底账

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `vision/MapSurveyService` | 手动镜头标定/勘察工具：录制镜头边界、中心锚点、修正点，并用本地变换把地图坐标反算成屏幕点、移动鼠标校验 | B | 本地：config/map_camera_bounds.json 持久化标定 + 内存 undo 栈 + 本地坐标→屏幕点变换math（WORLD_TILE_PIXEL_X/Y、边界插值、最小二乘修正拟合）全部在本地 | 迁后应由云端地图变换/标定 Service 持有（maps.json 变换快照，同 CHECK_COORDINATE_PLAUSIBLE/NavigationPoint 所在服务）；标定读写与坐标→屏幕投影上云 | 仅应保留：截取坐标条 + 提交鼠标移动 executor(InputSequences.submitAndWait moveMouse) + 录制交互；当前非法保留本地变换math与标定持久化，需迁云 |
| `vision/MiniMapCoordinateReader` | 小地图坐标条截图 + 全部识别(坐标/地图名/标签规整)委托云端 MiniMapLocation 决策；本地只做截图与 PNG 编解码搬运 | D | 云端 MiniMapLocationCloudDecisionService（所有 recognize 操作 READ_COORDINATE/READ_LOCATION/EXTRACT_MAP_LABEL/NORMALIZE/RECOGNIZE_* 均上云） | MiniMapLocationCloudDecisionService | captureCoordinateStrip(tracker.captureToMemory 固定 ROI 46,59,178,35 经 scaleRect) + PNG 编码/sha256/base64/云返回标签图解码 —— 纯 capture/搬运 |
| `vision/OcrTextMatcher` | 共享的短游戏名 OCR 模糊匹配纯工具：归一化、编辑距离、最长公共子串、命中评分 | C | 本地静态工具（无状态，无 capture，无输入）；命中规则/阈值内嵌本地 | 理想应并入云端 OCR/文本匹配 Service（ObjectiveTextReader/MiniMapLocation 云端识别内）以统一匹配规则 | 若保留则仅作纯 CPU 字符串助手，但其命中阈值须与云端匹配规则一致，否则本地会独立解释 OCR 名字 |
| `vision/LocationVisionService` | 位置视觉编排：截图经云端小地图识别取当前地图名+坐标；OCR 回退时本地纠名+云端可信度复核，拒绝样本本地归档 | B | 混合：云端拥有识别与坐标 plausibility；本地拥有 canonicalize-先纠名-再校验的顺序、接受/拒绝/归档决策 | MiniMapLocationCloudDecisionService(识别) + NavigationPointCloudDecisionService(checkCoordinatePlausible)；MapNameCanonicalizer 纠名亦应迁云 | 截图/焦点编排 + stop checkpoint + 失败样本归档 —— 属 capture/executor |
| `vision/ObjectiveTextRecognitionService` | 任务目标面板绿字识别：云端 active 时整链上云(洗字+模板+可信度)；云端 inactive 时回退本地 legacy 模板管线 | C | 云端(active时 ObjectiveTextReaderCloudDecisionService)；云端 inactive 时本地 legacy 模板匹配管线仍在(离线/dev/回滚用) | ObjectiveTextReaderCloudDecisionService(整链) + ImageProcessorService(绿字洗白) + NavigationPointCloudDecisionService(坐标 plausibility) | 读图/截图 + (legacy)本地模板匹配回退；legacy 管线属应删的本地 OCR 大脑 |
| `model/navigation/NavigationRequest` | 导航到地图上单个交互目标的请求 DTO | D | N/A（协议数据，字段由调用方构造） | 随导航协议迁移，作为云端导航请求 DTO | 无（纯 DTO） |
| `model/navigation/NavigationResult` | 导航 API 返回结果 DTO（状态+诊断消息） | D | N/A（协议数据） | 随导航协议迁移为云端返回 DTO | 无（纯 DTO） |
| `model/navigation/NavigationResultStatus` | 粗粒度导航结果状态枚举（ARRIVED/PATHING_STARTED/DIALOG_PREPARING 等） | D | N/A（枚举协议） | 随导航协议迁移 | 无（枚举） |
| `model/navigation/TemplateLocationInfo` | 小地图坐标+地图名快识别结果 DTO，含 ocrFallback 标志 | D | N/A（数据，由 MiniMapCoordinateReader 从云端决策构造） | MiniMapLocationCloudDecisionService 返回映射 | 无（纯 DTO） |
| `model/navigation/MiniMapSnapshot` | 小地图标签+坐标识别快照 DTO | D | N/A（数据） | MiniMapLocation 云端返回映射 | 无（纯 DTO） |
| `model/navigation/MapLabelTemplateMatch` | 小地图名模板匹配候选 DTO（mapName+score） | D | N/A（数据；接受阈值由调用方持有） | 云端识别返回映射 | 无（纯 DTO） |
| `model/navigation/ObjectiveTextResult` | 任务目标面板/剧情对话解析出的目的地 DTO | D | N/A（数据） | ObjectiveTextReader 云端返回映射 | 无（纯 DTO） |
| `model/navigation/PathingResult` | 触发原生任务面板寻路动作的结果枚举（SUCCESS/FINISHED/UI_ERROR） | D | N/A（枚举） | 随寻路协议迁移 | 无（枚举） |
| `model/navigation/WorldMapRouteResultMode` | 世界地图路线结果点击记忆的 UI 目标区分枚举（单值 YELLOW_DESTINATION_MINI_MAP） | D | N/A（枚举） | 随路线决策协议迁移 | 无（枚举） |
| `model/navigation/PendingTransferChoiceMemory` | 待 watcher 确认的路线对话选项点击的挂起记忆 DTO | D | N/A（数据载体；真正的挂起记忆生命周期由持有它的 runtime 管理） | 路线/传送决策云端 Service（记忆确认应云端仲裁） | 无（纯 DTO） |
| `model/navigation/PendingRouteOutcome` | 单次云端授权世界地图路线点击的 window-runtime 活证据 DTO（用后即弃，非缓存） | D | N/A（数据；显式声明'不是路线缓存、绝不本地选点'，结算由云端 routeDecisionId 拥有） | 路线决策云端 Service（routeDecisionId/intentId 结算归云端） | 无（纯 DTO） |

<details><summary><b>隐式状态清单</b>（15 条，按 kind 分类）</summary>

**timer** (1)

- `vision/MapSurveyService` — BOUNDARY/CENTER/CORRECTION_MOUSE_PREPARE_MS=3000ms 录制前 TaskSleep 等待人工移鼠标；修正点录制在等待后重读坐标

**memory** (3)

- `vision/MapSurveyService` — undoHistoryByMap: 每地图内存撤销栈(pushUndo/popUndo)，进程重启即丢，切换日会丢失未落盘的撤销链
- `model/navigation/PendingTransferChoiceMemory` — 承载 fromMap/coord→targetMap 的挂起点击记忆 + createdAtMs 时间戳(用于陈旧判定)，语义为'watcher 确认前不可复用'，实际记忆容器由 runtime 持有
- `model/navigation/PendingRouteOutcome` — window-runtime 作用域的路线点击活记录，含 usedMemory/routeDecisionId/intentId/createdAtMs，被 pathing watcher 上报后消费一次即弃

**cache** (4)

- `vision/MapSurveyService` — config/map_camera_bounds.json: 本地持久化标定（左右上下边界+插值样本、中心锚点、修正点样本），loadCameraBounds/saveCameraBounds 原子写
- `vision/LocationVisionService` — images/failure-cases/location: 本地拒绝坐标样本归档目录(archiveRejectedLocationSample 写 tmp_pos.png+metadata)
- `vision/ObjectiveTextRecognitionService` — templateBundle volatile + double-checked-locking 本地模板缓存(map名模板 manifest.tsv + green_digits 数字模板)，loadTemplates/doLoadTemplates 懒加载
- `vision/ObjectiveTextRecognitionService` — images/template/objective/{map_names,green_digits} 磁盘模板文件；MAP_NAME_MATCH_THRESHOLD=0.82/DIGIT=0.45/SLIDING=0.82 等本地阈值

**fallback** (4)

- `vision/MapSurveyService` — leftCameraXAt 等边界插值 + localFitCorrection 本地加权最小二乘修正拟合(3~8样本, 18格距/220px聚类/95残差阈)，作为无精确修正点时的本地兜底变换
- `vision/LocationVisionService` — ocrFallback 时先 MapNameCanonicalizer 本地模糊纠名，纠名改变地图则再调云端 isCloudPlausibleAfterCanonicalize；云端不可用 fail-closed 直接拒绝并归档
- `vision/LocationVisionService` — 无 window context 时走 legacy focused：bringWindowToFront 聚焦后再截图；有 context 走 no-focus
- `vision/ObjectiveTextRecognitionService` — objectiveTextReaderCloudDecisionService.isActive()==false 时走 recognizeLocallyLegacy 本地整套模板匹配/坐标解析/修复(@Deprecated CR208-9)

**other** (3)

- `vision/MapSurveyService` — MouseInfo.getPointerInfo() 读实时系统鼠标位置作为标定输入；CORRECTION_LARGE_ERROR_THRESHOLD=500px 误差过大则拒绝落盘
- `model/navigation/NavigationRequest` — freshCurrentLocationPhaseBound/freshCurrentLocationAtMs: 携带任务级新鲜位置快照标志与时间戳，语义(是否当作 phase 事实而非几秒失效缓存)由消费方解释，非本类逻辑
- `model/navigation/TemplateLocationInfo` — ocrFallback 布尔驱动消费方施加 OCR 纪律(纠名+plausibility)，但本类无逻辑

</details>

### 2.8 model.tasktracker (全部) + model 根目录三文件 (非 navigation) — A-1 方法级迁移矩阵

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `TaskTrackerFastMatchResult` | 小区域追踪缓存指纹校验的结果 DTO（matched/distance/maxDistance/score/耗时/debug 路径/reason） | C | 本地指纹快路径比较器产出该结果；maxDistance 为本地快路径接受阈值 | 云端 TrackerPanel/缓存校验 Service（迁后由云端做指纹距离判定，本地不再持有阈值决策） | 无（纯结果 DTO，实际缓存状态在 CacheEntry） |
| `TaskTrackerTitleTemplate` | 左侧追踪面板黄字标题模板定义（taskKey/displayName/templatePath/threshold） | C | 本地模板库路径 + 硬编码默认阈值 0.82 | 云端模板匹配 Service 持有模板集合与匹配阈值 | 无 |
| `TaskTrackerPanelSourceType` | 标识某个追踪面板结果/链接由哪个 reader 产出的枚举（LOCAL / CLOUD_TRACKER_PANEL_READER） | D | 由产出方在构造结果时打标；枚举本身无逻辑 | 云端 reader 产出时统一标 CLOUD_TRACKER_PANEL_READER；作为迁移 provenance 开关的共享契约 | 无 |
| `TaskTrackerGreenLink` | 追踪面板中一段可点绿字（屏幕绝对 bbox + 从绿字 OCR 出的 targetMapName/score + sourceType） | C | 本地或云 reader 产出坐标与 targetMapName 的 OCR 解释 | 云端 TrackerPanelReader Service 做绿字分割 + 目标地图名 OCR | 按 centerPoint 执行点击的 executor（纯动作） |
| `TaskTrackerPanelReadResult` | 读单个任务块的完整结果（found/titleTemplate/greenLinks/selectedGreenLink 云选中点/greenBandWidth/probeObjective 形状提示） | C | reader 产出全部字段；selectedGreenLink 注释即'cloud-selected 生产点击候选'，probeObjective 为双链目标形状提示 | 云端 TrackerPanelReader Service 产出含选点(selectedGreenLink)与 probeObjective 提示 | 无（消费选点去点击属 executor） |
| `TaskTrackerPanelPrepareResult` | Runner 对一次追踪读的准备结果封装（preparedAction 正向 / negativeResult 负向 / trackerPanelRegion / wuhuanTrackerBlockRegion） | D | 本地 Runner 封装 action 或 negative 二选一 + 两个 window-relative ROI | 云端 tracker Service 产出 action/negative；Runner 侧仅做封装搬运 | 无（region 为窗口相对裁剪坐标，属 capture 元数据） |
| `TaskTrackerPanelNegativeResult` | Runner 持有的追踪读'无动作/未命中'负结果（windowId/taskType/taskCode/Status/reason/region/observedAtMs/sequence） | B | 本地：observedAtMs 用本地时钟；freshWithin 与 matches 在本地门控'该负结果是否可被消费' | 云端 tracker Service 产出负结果与 reason/Status；新鲜度窗口与跨任务消费护栏语义应上移为云端权威 | 无（结果搬运）；当前时间戳与新鲜度判定仍在本地是需迁移点 |
| `TaskTrackerPanelCacheEntry` | 窗口级'已成功解析的追踪面板'缓存（taskCode/panelFingerprint/clickWindowRelative 复用点击点/面板原点+尺寸/region/updatedAtMs/source） | B | 本地窗口级缓存持有可复用的 click truth 与面板指纹（快路径命中即跳过重读直接复用历史点击） | 云端 TrackerPanelReader Service 应持有解析与选点权威；本地仅保留面板裁剪/截图 | 面板裁剪 capture（panelOrigin/尺寸、window-relative 裁剪） |
| `MapCoordinate` | 简单地图坐标 DTO（x,y） | D | 本地构造，无权威语义 | 任意坐标传输 DTO，云端可直接复用同结构 | 无 |
| `PlayerCharacter` | 角色身份 + 动态位置状态持有者（name/id/gameServerName 身份 + currentMapName/x/y 动态状态） | A | 本地对象持有角色身份三要素与当前地图/坐标状态（可变，后续 set） | 云端角色/会话状态注册表持有身份与位置权威状态 | 无（身份+位置状态应上移）；本地至多保留最近一次读位的 capture 快照 |
| `TaskRunResult` | 单次任务执行结果协议枚举（SUCCESS/FAILED/STOPPED/SKIPPED） | A | 由 GameTask.execute() 返回；协议本身无逻辑，但 STOPPED 承载 stop 安全语义、SKIPPED 承载跳过语义 | 云端任务编排以该协议判定推进/停止/跳过；枚举契约两端共享 | 无 |

<details><summary><b>隐式状态清单</b>（10 条，按 kind 分类）</summary>

**timer** (1)

- `TaskTrackerPanelNegativeResult` — freshWithin(nowMs,maxAgeMs) 为 TTL 新鲜度窗口：负结果过期即不可消费；maxAgeMs<0 视为永久有效——切换日若两端时钟/窗口不一致会误判

**memory** (3)

- `TaskTrackerPanelNegativeResult` — observedAtMs 默认 System.currentTimeMillis()，本地时钟记忆的观测时刻
- `TaskTrackerPanelNegativeResult` — sequence 为存储时赋的 runtime-local 序号（本地排序记忆），零表示未存储
- `PlayerCharacter` — currentMapName/x/y 为本地可变位置记忆，无 TTL；切换日若不随会话迁移会造成角色位置状态漂移/错判

**cache** (2)

- `TaskTrackerPanelCacheEntry` — 整类即窗口级面板解析缓存；panelFingerprint 为快路径签名，命中则走 FastMatch 复用而不重新读取
- `TaskTrackerPanelCacheEntry` — updatedAtMs 缓存陈旧度时钟，配合快路径判定是否仍可信

**lock** (1)

- `TaskTrackerPanelNegativeResult` — matches(windowId,taskType,taskCode) 跨任务消费护栏，防止一个任务的负结果被另一任务错误消费；taskCode 经 normalize 归一

**fallback** (1)

- `TaskTrackerPanelCacheEntry` — clickWindowRelative 为指纹命中时复用的历史点击点（window-relative 存储，避免复用桌面绝对坐标）——切换日若指纹校验放行但面板已变会点错位置

**other** (2)

- `TaskTrackerTitleTemplate` — threshold 默认 0.82 为硬编码模板匹配阈值(config-like)，迁云时需连同模板一起上移，避免本地阈值与云端不一致
- `TaskTrackerPanelSourceType` — 本身无状态，但下游可能按 sourceType 分支信任/路由；迁后本地路径应彻底不再产出 LOCAL 结果，否则出现本地脑残留

</details>

### 2.9 cloud.decision

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `CloudDecisionClient` | 云决策传输契约接口，单方法 decide(request)->response 定义 local<->cloud 边界 | A | 本地 @Primary HttpCloudDecisionClient 实现持有该协议 | 云端决策 HTTP 端点（/api/cloud/decision） | 传输 executor（发起 HTTP 调用） |
| `CloudDecisionCoordinator` | 本地决策大脑集成点：对每次请求做 shadow/execute 门控、schema 校验、灰度采样、fallback/STOP 停机裁决并回落 local | A | 本地 Coordinator 持有是否让云决策覆盖 local、是否停机、agreement 判定的最终权威 | 云端各 serviceId 决策 Service + 云端灰度/门控编排 | 迁后本地仅应保留：发起 decide 传输调用 + STOP 停机安全兜底执行；灰度/agreement/execute 门控须上收云端 |
| `CloudDecisionExecutionGate` | 服务级执行门控接口：把 schema 合法的云响应映射为本地安全的 effectiveDecision，或拒绝并给原因 | B | 本地各服务 Gate 实现（当前仅 TASK_CLASSIFIER 类）持有 cloud->local 安全映射与业务边界 | 云端服务侧直接产出可执行 effectiveDecision（Gate 职责应上收） | 本地保留把云响应落地为安全本地动作的映射 executor |
| `HttpCloudDecisionClient` | @Primary 真实 HTTP 传输实现：序列化请求、Bearer 鉴权、超时看门狗、解析响应 | A | 本地持有传输、超时、Bearer token 鉴权、端点拼装 | 云端端点接收；token 身份由云端签发/校验 | HTTP 传输 executor + 本地看门狗超时（纯搬运，可保留） |
| `MockCloudDecisionClient` | 非 @Primary 测试/无传输替身：回显 localDecision 作为云决策，总是 agree | D | 本地 mock 兜底 Bean（@Primary 的 Http 生产环境胜出） | 无（迁后由真实云服务取代） | 无（可弃） |
| `CloudDecisionMode` | 运行模式枚举 DISABLED/SHADOW/EXECUTE | A | 本地由 properties.service(shadowEnabled/executeEnabled) 推导 | 云端可下发 mode/rollout 状态 | 本地读取模式做门控 |
| `CloudDecisionRequest` | 决策请求 DTO：serviceId/trace/task/phase/window/localDecision/context(含图片)/createdAt | D | 本地组装（输入采集） | 云端消费 | 本地采集 window/task/phase/image 输入 capture |
| `CloudFallbackMode` | 回落模式枚举 LOCAL/STOP/SHADOW_ONLY | A | 本地配置 defaultFallback / service.fallback | 云端可下发 fallback 策略 | 本地保留 STOP 停机安全语义执行 |
| `CloudDecisionResponse` | 决策响应 DTO：decision/confidence/ttlMs/policyVersion/fallbackReason/diagnostics | D | 云端产出 | 云端决策 Service | 本地解析消费 |
| `CloudDecisionClientException` | 传输层运行时异常，标识 cloud 不可用触发回落 | D | 本地传输层抛出 | 无 | 本地传输错误信号，Coordinator 据此走 unavailable 回落 |
| `CloudDecisionMetricsService` | 本地决策指标聚合：分组累计成功/一致/执行/回落，定长窗口算 p50/p95/p99，按节流 cadence 打日志 | C | 本地持有指标聚合、采样窗口、日志节流 | 云端 METRICS_INGEST Service（serviceId 已预留） | 本地保留原始采样 capture 后上报 METRICS_INGEST；聚合/分位可上收云端 |
| `CloudDecisionResult` | local-vs-cloud 决策结果信封 DTO，含 isRequiredExecuteFailure 停机判定 | A | 本地 Coordinator 产出 | 云端可产出等价 result 语义 | 本地保留 isRequiredExecuteFailure 停机判定供上游消费 |
| `CloudDecisionProperties` | cloud.* 配置：开关/传输/token/超时/默认回落/每服务灰度/DevSidecar 拉起云脑 | A | 本地配置文件持有 enabled/realTransport/baseUrl/token/timeout/灰度/sidecar 权威 | 云端应下发 enabled/mode/executePercent/fallback/policyVersion 等策略（FEATURE_FLAG/POLICY_VERSION serviceId 已预留） | 本地保留 baseUrl/token/timeout/DevSidecar 拉起 sidecar 的 executor 配置 |
| `CloudDecisionServiceId` | 30 个云服务 id 枚举 = 迁移目标态云服务目录/路由标识 | A | 本地枚举定义全部服务身份 | 每个枚举值对应一个云端 Service（含 METRICS_INGEST/FEATURE_FLAG/POLICY_VERSION/LEARNED_MEMORY/CAPABILITY_GATE 等基础设施类） | 本地保留 serviceId 作为路由/标识键 |

<details><summary><b>隐式状态清单</b>（22 条，按 kind 分类）</summary>

**timer** (3)

- `HttpCloudDecisionClient` — connectTimeout 与 per-request timeout 均 = properties.timeoutMs（默认 60000ms）；HttpTimeoutException->CloudDecisionClientException，本地超时看门狗
- `CloudDecisionMetricsService` — 日志节流 cadence：total<=3(immediateLogSamples) 立即，其后每 20(logEverySamples) 条一次
- `CloudDecisionProperties` — timeoutMs 默认 60000；DevSidecar.startupTimeoutMs 默认 60000

**memory** (3)

- `CloudDecisionCoordinator` — 每次结果写入 metricsService.record() 落本地内存桶（见 CloudDecisionMetricsService）
- `CloudDecisionMetricsService` — buckets ConcurrentHashMap<MetricsKey,MetricsBucket> 进程内累计计数(total/cloudSuccess/agreement/executed/fallbackLocal/lastFailureReason)，不持久，重启清零
- `CloudDecisionProperties` — services EnumMap.computeIfAbsent 惰性建 Service，本地按需记忆每服务开关

**cache** (2)

- `CloudDecisionResponse` — ttlMs 字段为缓存时效提示，随响应传输；本组内未见本地据 ttl 做缓存的逻辑（消费方需核实）
- `CloudDecisionMetricsService` — 每 bucket elapsedMs ArrayDeque 定长 64(DEFAULT_ELAPSED_WINDOW_SIZE) 滑动窗口，超限 removeFirst，供 p50/p95/p99

**lock** (1)

- `CloudDecisionMetricsService` — synchronized(bucket) 串行化单桶 record+snapshot

**fallback** (6)

- `CloudDecisionCoordinator` — EXECUTABLE_SERVICES 硬编码 allowlist=EnumSet.of(TASK_CLASSIFIER)；DEFAULT_EXECUTION_GATE 仅放行 TASK_CLASSIFIER 进 execute，其余服务即便配 execute 也永远 keep local——切换日漏配即静默不执行
- `CloudDecisionCoordinator` — fallbackMode(): service.getFallback() ?? properties.defaultFallback(LOCAL)；unavailable/disabled 时用 LOCAL/STOP/SHADOW_ONLY 决定回落还是停机
- `CloudDecisionExecutionGate` — GateResult.rejected(reason) / accepted 缺 effectiveDecision 时 Coordinator 回落 local——Gate 是 execute 前最后一道本地业务闸
- `HttpCloudDecisionClient` — endpointPath 缺省硬编码兜底 '/api/cloud/decision'
- `MockCloudDecisionClient` — 缺省 decision='LOCAL'、policyVersion='mock-local'；回显 localDecision 使云端恒 agree——若误作 active Bean 会让 execute 采样执行回显的 local（与 local 等价，低风险）
- `CloudDecisionProperties` — defaultFallback=LOCAL；每 Service 默认 shadowEnabled=false/executeEnabled=false/executePercent=0/fallback=LOCAL——默认全关或纯 shadow

**other** (7)

- `CloudDecisionCoordinator` — executePercentHit(): Math.floorMod(hash(traceId\|serviceId\|taskCode\|phase),100)<percent 的确定性本地灰度桶——本地持有 rollout 采样顺序与 key 语义，上云须保持同一 key 否则分桶漂移
- `CloudDecisionCoordinator` — effectiveDecisionFor(): EXECUTE+STOP+!executed 时返回 null(=停机/stop-pause 语义)——本地隐式停机开关，null 会向上游传递为不动作
- `CloudDecisionRequest` — @Builder.Default context=Map.of()、createdAt=Instant.now()——本地时钟戳，非业务决策
- `CloudFallbackMode` — STOP 语义驱动 Coordinator.effectiveDecisionFor 返回 null（停机）；SHADOW_ONLY/LOCAL 决定回落行为——枚举值本身即停机安全开关
- `CloudDecisionResult` — isRequiredExecuteFailure()=EXECUTE+STOP+!executed，供调用方据此停机——停机安全谓词，上游漏判即带病继续
- `CloudDecisionProperties` — DevSidecar.autoStartEnabled=true + 硬编码 brainProjectPath 'D:\mavenProject\dhxy-cloud-brain' + scriptPath——本地隐式自动拉起云脑 sidecar 进程，切换日环境依赖点
- `CloudDecisionServiceId` — 当前 Coordinator 硬编码仅 TASK_CLASSIFIER 可 execute，其余 29 个 serviceId 即使配置也只能 shadow——目录已全，执行放行是分批的

</details>

### 2.10 cloud/task

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `TaskClassifierCloudShadowService` | 影子上报本地 task-tracker 标题分类给云端 TASK_CLASSIFIER，只观测不执行 | C | 本地（shadow-only，云端仅观测比对，本地分类仍权威） | TASK_CLASSIFIER | 本地标题模板分类（title-template taskKey）仍在本地产出并使用 |
| `TrackerLinkRankerCloudDecision` | tracker 绿链点击 shadow/execute 结果信封（本地/云执行/云拒绝无点击） | D | 信封本身无权威，承载 TrackerLinkRankerCloudShadowService 的结论 | TRACKER_LINK_RANKER | 承载本地 selectedIndex/selectedLink 作为 passthrough 时的执行值 |
| `RouteMemoryOutcomeIngestResult` | route-memory 结局上报的 ingest 状态 DTO | D | 无（纯状态回执） | route-memory (RouteCloudDecisionService 上报端) | 无 |
| `RouteCloudDecision` | 世界地图路线候选云决策信封（本地/云执行/云无点击/云拒绝） | D | 承载 RouteCloudDecisionService 结论 | ROUTE_CANDIDATE | 承载本地 window-relative 点作 passthrough |
| `DialogPolicyCloudDecision` | DIALOG_POLICY execute 信封，只允许本地安全结果或显式失败结果 | D | 承载 DialogPolicyCloudDecisionService 结论 | DIALOG_POLICY | effectiveResult 只能是原本地 DialogResult 或 failed，无云坐标 |
| `TaskPolicyCloudDecision` | 泛型任务 phase-outcome execute 信封，云只能替换 result/yield/next 三枚举 | D | 承载 TaskPolicyCloudDecisionService 结论 | TASK_POLICY | localResult/localYield/localNextPhase 作 passthrough 与失败回退 |
| `TaskRecoveryCloudDecision` | TASK_RECOVERY execute 信封，云只能授权本地已构造的恢复候选 | D | 承载 TaskRecoveryCloudDecisionService 结论 | TASK_RECOVERY | effectiveAction/effectiveNextPhase 始终=本地候选，云不能改 |
| `CapabilityGateCloudDecision` | 本地支援能力门 allow/deny 信封 | D | 承载 CapabilityGateCloudDecisionService 结论 | CAPABILITY_GATE | localAllowed；allowed=local AND cloud |
| `CapabilityGateCloudDecisionService` | 团队支援能力门云必需 execute：本地与云取与，必需失败即拒绝 | A | 云 execute（在本地已 allow 前提下收紧）；未激活时本地 localAllowed 权威 | CAPABILITY_GATE | 本地 session/capability 门产出的 localAllowed 作为输入下界 |
| `MaintenanceThresholdCloudDecisionService` | 维护阈值云必需门：决定本次维护是否可调用下游维护动作 | B | 云 execute（本地 ALLOW 可被云降级为 SKIP/NO_ACTION，本地非 ALLOW 不可被云提权）；未激活时本地 action 权威 | MAINTENANCE_THRESHOLD | 本地 planned action(ALLOW/SKIP/NO_ACTION) 作输入 |
| `TeamReturnPolicyCloudDecisionService` | 组队归队行为门（成员点击/队长等待/队长预检）云必需 allow/deny | A | 云 execute（本地 allow 前提下收紧）；未激活时本地 localAllowed 权威 | TEAM_RETURN_POLICY | 本地策略 localAllowed 作输入；携带 windowRole/leader 上下文 |
| `TaskRecoveryCloudDecisionService` | 任务恢复候选云必需授权：云须回显与本地一致的 action+next 才放行 | B | 云 execute 授权；服务未激活或非必需失败时本地恢复候选 passthrough | TASK_RECOVERY | 本地已构造的恢复候选(action+nextPhase) 是唯一可授权对象 |
| `MaintenanceThresholdCloudDecision` | 维护阈值决策 DTO（ALLOW/SKIP/NO_ACTION/REQUIRED_FAILURE） | D | 承载 service 结论 | MAINTENANCE_THRESHOLD | 无 |
| `TeamReturnPolicyCloudDecision` | 组队归队 allow/deny DTO | D | 承载 service 结论 | TEAM_RETURN_POLICY | localAllowed |
| `TaskPolicyCloudDecisionService` | 任务 phase-outcome 云决策：云可替换本地 result/yield/next，本地 STOPPED 保护 | B | 云 execute（phase 结局）；本地 STOPPED 时本地权威；必需失败→终局 FAILED phase | TASK_POLICY | 本地已算出的 result/yield/next 作 passthrough；STOPPED 停机安全在本地 |
| `TrackerLinkRankerCloudShadowService` | tracker 绿链选择：execute 接受云 window-relative 点击（左侧 tracker ROI 内） | B | 云 execute 点击；否则本地 selectedIndex/link passthrough | TRACKER_LINK_RANKER | 本地排名/选中的绿链（screen-absolute）作 passthrough 与 shadow 证据 |
| `SummonSkillCloudRequest` | 召唤兽技能 tooltip/静态槽图像识别请求 DTO（payload+ROI+window） | D | 无 | SUMMON_SKILL | 无 |
| `SummonSkillCloudDecision` | 召唤兽技能槽状态决策 DTO | D | 承载 service 结论 | SUMMON_SKILL | 无 |
| `ImagePreprocessCloudRequest` | 图像预处理请求 DTO（raw payload、operation、ROI、window、parameters） | D | 无 | IMAGE_PREPROCESS | 无 |
| `ImagePreprocessCloudDecision` | 图像预处理结果 DTO（washed image、候选框/点、resultValues） | D | 承载 service 结论 | IMAGE_PREPROCESS | 承载云返回的洗白图/坐标 |
| `ImagePreprocessWashedImageClient` | 图像预处理传输客户端：编码 raw PNG→云洗白→校验解码→(可选)写盘 | D | 传输/capture executor，识别权威在云 | IMAGE_PREPROCESS | PNG 编解码、SHA256、磁盘写入、窗口几何取值（纯 executor/capture） |
| `ImagePreprocessOperation` | 图像预处理操作枚举（WASH_*/COUNT_*/FINGERPRINT/ROUTE_* 等 20 项） | D | 无 | IMAGE_PREPROCESS | 无 |
| `ImageProcessorService` | 图像处理服务接口（洗白/计数/指纹/文本候选等契约）+ 结果 record | C | 接口，实现权威见 CloudImageProcessor | IMAGE_PREPROCESS | 结果 record 的 hasRequiredOutput 按 operation 判定必需输出（本地契约校验） |
| `CloudImageProcessor` | ImageProcessorService 云实现：所有视觉/OCR 预处理操作委托云 washed-image client，fail-closed | C | 云（IMAGE_PREPROCESS）；本地无回退计算 | IMAGE_PREPROCESS | 仅解析云 resultValues 成结构（无本地视觉算法） |
| `ImagePreprocessCloudService` | IMAGE_PREPROCESS execute gate：校验 washed 图 sha/尺寸、候选框/点 ROI/窗口边界、坐标空间、置信度 | C | 云 execute；本地仅做输入安全校验 | IMAGE_PREPROCESS | ROI/窗口边界与坐标空间校验、ROI_RELATIVE→WINDOW_RELATIVE 换算（安全层） |
| `MiniMapLocationCloudRequest` | 小地图坐标条/地图标签识别请求 DTO（payload、operation、ROI、requiresCoordinate/MapName） | D | 无 | MINIMAP_LOCATION | 无 |
| `NpcClickStrategyCloudDecision` | 遗留 NPC_CLICK_STRATEGY 桥的结果信封（永远 no-click） | D | 无（恒拒绝） | NPC_CLICK_SMART（已取代） | 无 |
| `NpcClickStrategyCloudDecisionService` | 遗留 NPC 策略桥，已退役为显式 no-click 守卫（不再是生产权威） | B | 无——恒拒绝本地策略执行，强制 NPC_CLICK_SMART | NPC_CLICK_SMART | 无（不调用云，直接拒绝） |
| `DialogPolicyPreClickCloudDecision` | 对话预点击云决策 DTO（CLICK/NO_ACTION/ABORT + window-relative 点） | D | 承载 DialogPolicyCloudDecisionService 结论 | DIALOG_POLICY | 承载云点击点 |
| `NpcClickSmartCloudDecision` | NPC 智能点击云决策 DTO（CLICK/CTRL_CLICK/PRESS_HOTKEY 等 + window-relative 点） | D | 承载 NpcClickSmartCloudDecisionService 结论 | NPC_CLICK_SMART | 承载云点击点/hotkey/actionId |
| `NpcClickSmartCloudSession` | NPC 智能点击 FIFO 会话状态信封（STARTED/DISABLED/REQUIRED_FAILURE） | D | 承载会话协议状态 | NPC_CLICK_SMART | sessionId/windowId/taskRunId 身份三元 |
| `TrackerPanelReaderCloudDecision` | tracker 面板读取云决策 DTO（FOUND/NO_ACTION + click/taskKey/links） | D | 承载 TrackerPanelReaderCloudDecisionService 结论 | TRACKER_PANEL_READER | 承载云 window-relative click 与 links |
| `NpcClickSmartQueueMessage` | NPC 点击 FIFO 队列消息 DTO（MEMORY/TOOLTIP/YELLOW/PURPLE/CTRL/WAIT/END/INVALID） | D | 承载云队列指令 | NPC_CLICK_SMART | 承载 window-relative 点/candidateBox/ctrlProbePoints |
| `TeamRoleTooltipCloudDecision` | 组队角色 tooltip 云决策 DTO（LEADER/MEMBER + leaderClientId） | D | 承载 TeamRoleTooltipCloudDecisionService 结论 | TEAM_ROLE_TOOLTIP | 承载 role/leaderClientId 身份 |
| `NpcClickSmartQueueOutcome` | NPC 点击队列结局枚举（VERIFIED/VERIFICATION_FAILED/SAFETY_REJECTED 等 9 态） | D | 无 | NPC_CLICK_SMART | 本地 verifier 判定的结局标签 |
| `SheyaoxiangStatusCloudDecision` | 麝药香状态/动作云决策 DTO（CAPTURE_STATUS/USE_INCENSE/RETRY_LATER/FAIL_CLOSED + remainingMs） | D | 承载 SheyaoxiangStatusCloudDecisionService 结论 | SHEYAOXIANG_STATUS | 承载云给的 remainingMs 计时与 iconBox |
| `SheyaoxiangStatusCloudRequest` | 麝药香请求 DTO（TICK/STATUS_IMAGE/OUTCOME hook + 本地计时事实） | D | 无 | SHEYAOXIANG_STATUS | 承载本地 nowMs/lastIncenseUsedTimeMs/nextIncenseRetryTimeMs 计时事实作输入 |
| `SheyaoxiangStatusCloudDecisionService` | 云 owned 香策略/识别器：TICK/图像/结局三 hook，fail-closed 无本地回退 | B | 云（SHEYAOXIANG_STATUS）owned 香策略与识别；本地无 OCR/学习/猜测回退 | SHEYAOXIANG_STATUS | 仅状态栏 ROI 截图 capture 与输入执行；计时事实上报 |
| `TeamRoleTooltipCloudRequest` | 组队角色 tooltip 识别请求 DTO（mask PNG + player/window 上下文） | D | 无 | TEAM_ROLE_TOOLTIP | 无 |
| `TeamRoleTooltipCloudDecisionService` | 云商业视觉判定悬停队伍 tooltip 的队长/队员角色与 leaderClientId | A | 云（TEAM_ROLE_TOOLTIP）——队长/成员身份与 leaderClientId 由云裁定；本地无 OCR | TEAM_ROLE_TOOLTIP | 仅 mask tooltip 截图 capture；fallback 策略归调用方 |
| `SummonSkillCloudDecisionService` | 召唤兽技能槽 tooltip/静态槽云必需识别，fail-closed UNKNOWN | C | 云（SUMMON_SKILL）识别槽状态；本地无黄字洗白/模板回退 | SUMMON_SKILL | 仅槽 ROI 截图 capture |
| `RouteMemoryOutcomeReport` | 路线记忆结局观测 DTO（routeDecisionId/from-target map/click/observed/result） | D | 无 | route-memory | 本地 watcher 观测事实 |
| `TrackerPanelReaderCloudRequest` | tracker 面板读取请求 DTO（raw PNG + 窗口原点 + taskKey + selectionPolicy） | D | 无 | TRACKER_PANEL_READER | 本地已确立 taskKey 作输入 |
| `TrackerPanelReaderCloudDecisionService` | 云 owned 解析左侧任务追踪面板，返回 window-relative click/taskKey/links，fail-closed | B | 云（TRACKER_PANEL_READER）owned 图像解析与选链；本地不得从图推点 | TRACKER_PANEL_READER | 仅面板截图 capture 与云点校验 |
| `RouteCloudDecisionService` | 路线候选 execute（route-result ROI 内 window-relative click）+ route-memory 结局 HTTP 上报 | B | 云（ROUTE_CANDIDATE）execute 路线点击；route-memory 结局权威在云 | ROUTE_CANDIDATE + route-memory | 本地 shadow 决策(oracle)、点击 executor、窗口上下文 enrich |
| `DialogPolicyPreClickCloudRequest` | 对话预点击请求 DTO（raw/ROI 图像 payload + 对话请求 + 窗口/身份） | D | 无 | DIALOG_POLICY | 无 |
| `DialogPolicyCloudDecisionService` | DIALOG_POLICY execute + pre-click + white-template：云决点击，本地重校验坐标/actionId 白名单 | A | 云（DIALOG_POLICY）决对话动作/点击；本地做输入安全与协议校验并 fail-closed | DIALOG_POLICY | 点击 executor、截图 capture、ROI/窗口/坐标空间/actionId 白名单校验（安全层） |
| `ObjectiveTextReaderCloudDecisionService` | 云 OCR 读取目标/任务详情/五倍目的地提示文本（三独立云服务），miss 返回 empty | C | 云（OBJECTIVE_TEXT_READER/QUEST_DETAIL_READER/WUBEI_DEST_HINT_READER）owned OCR/模板/边界守卫 | OBJECTIVE_TEXT_READER / QUEST_DETAIL_READER / WUBEI_DEST_HINT_READER | PNG 编码+SHA、3 帧采样时序调度（capture 调度归本地） |
| `MiniMapLocationCloudDecisionService` | 小地图坐标条/地图标签云必需识别，requiresCoordinate/MapName 控制 fail-closed 契约 | C | 云（MINIMAP_LOCATION）识别坐标/地图名；本地无数字/模板回退 | MINIMAP_LOCATION | 仅 ROI 截图 capture + 坐标合理性边界（0..999） |
| `MiniMapLocationCloudDecision` | 小地图识别决策 DTO（coordinate/mapName/label payload/ocrFallbackReason） | D | 承载 service 结论 | MINIMAP_LOCATION | 承载 label 图 payload 供后续 |
| `NavigationPointCloudDecisionService` | CR258 云评估小地图变换：点击候选批(prefetch)+坐标合理性+接近坐标；五字段 binding-echo 门 fail-closed | A | 云（MINIMAP_LOCATION）owned 变换表/候选生成；本地仅按 binding-echo 校验身份后执行 | MINIMAP_LOCATION | 点击 executor、身份快照(hwnd/windowId/taskRunId)、clientFrame 缩放、candidate 单次 token 执行 |
| `NpcClickSmartCloudDecisionService` | NPC_CLICK_SMART：FIFO 会话协议(start/poll/outcome)+直战授权+单次 decide；ROI/hotkey/置信度校验+HTTP 上报 | A | 云（NPC_CLICK_SMART）owned 点击动作与队列；本地仅校验坐标+执行输入+回报结局 | NPC_CLICK_SMART | 点击/输入 executor、截图 capture、会话身份、坐标/ROI/scan-region 校验、结局 HTTP 上报 |
| `NpcClickSmartCloudRequest` | NPC 智能点击请求 DTO（image payload、ROI、scanRegions、window、target facts、directCombat 事实） | D | 无 | NPC_CLICK_SMART | 承载本地 target/player/tune/directCombat 事实作输入 |
| `NavigationRoutePlanCloudDecisionService` | CR260 云 NAVIGATION_ROUTE_PLAN 编排器网络层：逐步上报观测事实，返回一条 ACTION/TERMINAL 指令；五字段 echo 门 | A | 云（NAVIGATION_ROUTE_PLAN）owned 逐步编排决策；本地 shell 执行动作与终局事实门，本地不建 NavigationResult | NAVIGATION_ROUTE_PLAN | 梯子观测事实由本地同款 helper 计算作输入；动作执行归 shell |

<details><summary><b>隐式状态清单</b>（57 条，按 kind 分类）</summary>

**timer** (5)

- `SheyaoxiangStatusCloudDecision` — remainingMs/remainingSource 承载香剩余时间（计时语义由云给出）
- `SheyaoxiangStatusCloudRequest` — nowMs/lastIncenseUsedTimeMs/nextIncenseRetryTimeMs 本地时间事实——切换日须确认这些计时全部作为云输入、本地不自行重试
- `SheyaoxiangStatusCloudDecisionService` — 本地把 nowMs/lastIncenseUsedTimeMs/nextIncenseRetryTimeMs 作事实喂云，云决定 USE/RETRY_LATER；本地不得自行定时补香
- `RouteCloudDecisionService` — HttpClient connectTimeout 与请求 timeout = properties.getTimeoutMs
- `NpcClickSmartCloudDecisionService` — HttpClient connectTimeout 与结局 POST timeout=properties.timeoutMs

**memory** (5)

- `NpcClickSmartCloudSession` — accepted() 要求 sessionId+windowId+taskRunId 齐备——会话身份门
- `RouteCloudDecisionService` — submittedOutcomeKeys(ConcurrentHashMap.newKeySet) 本地幂等去重集——重复结局 DUPLICATE_SKIPPED；失败时会 remove 回滚
- `NavigationPointCloudDecisionService` — attemptedCandidateIds 已尝试候选环，耗尽→EXHAUSTED
- `NpcClickSmartCloudDecisionService` — 会话 sessionId/windowId/taskRunId 身份三元贯穿 FIFO；legacy action 一律拒绝
- `NavigationRoutePlanCloudDecisionService` — priorAction/priorContext/priorOutcome 携带上一步结果供云续编排

**cache** (1)

- `DialogPolicyCloudDecisionService` — TeleportConfig.MAP_ALIASES 本地地图别名字典喂 targetKeywordAliases

**lock** (3)

- `NavigationPointCloudDecisionService` — batchExpiresAtMs 必须=本地 navigationDeadlineMs 回显(租约/lease)，否则整批作废（60s 时钟）
- `NavigationPointCloudDecisionService` — currentIdentity 用实时 native hwnd 快照——窗口重绑即失效在途批次
- `NavigationRoutePlanCloudDecisionService` — binding-echo 五字段(windowId/hwnd/taskRunId/routePlanRequestId/clientFrame)校验(身份租约/lease)，失配即失败

**fallback** (34)

- `TaskClassifierCloudShadowService` — 服务未激活时直接返回 null，本地分类完全保留；这是纯 shadow，切换执行时需另建 execute 路径
- `TaskPolicyCloudDecision` — cloudRequiredFailure 硬编码 effectiveResult=RETRYABLE_ERROR、yield=MUST_YIELD 指向 failurePhase——本地终局回退语义
- `CapabilityGateCloudDecisionService` — coordinator 未激活 → localOnly(localAllowed)，本地能力门仍权威
- `CapabilityGateCloudDecisionService` — 云返回非 ALLOW/DENY 或必需失败 → requiredFailureDeny 一律拒绝（fail-closed）
- `MaintenanceThresholdCloudDecisionService` — 未激活 → localOnly(localAction)
- `MaintenanceThresholdCloudDecisionService` — 本地非 ALLOW 时 effectiveAction 强制 NO_ACTION——云不能把本地 no-action 提权
- `MaintenanceThresholdCloudDecisionService` — 云 REQUIRED_FAILURE/非法 action → requiredFailure 不允许下游动作
- `TeamReturnPolicyCloudDecisionService` — 未激活 → localOnly
- `TeamReturnPolicyCloudDecisionService` — 云必需失败 → requiredFailureDeny 拒绝（不本地续跑）
- `TaskRecoveryCloudDecisionService` — 未激活 → localPassthrough 本地恢复候选仍执行（切换日风险点：此时恢复不受云约束）
- `TaskRecoveryCloudDecisionService` — keepsLocalPassthrough：非 EXECUTE 或非必需失败 → 保留本地候选
- `TaskPolicyCloudDecisionService` — runnerResult/localResult==STOPPED → 保留本地（停机安全，云不能覆盖 STOPPED）
- `TaskPolicyCloudDecisionService` — keepsLocalPassthrough：percent 未命中 gate → 保留本地
- `TaskPolicyCloudDecisionService` — 云必需失败 → failurePhase(枚举 FAILED 或本地 next) 终局失败
- `TrackerLinkRankerCloudShadowService` — 硬编码 WINDOW 1024x768 与 TRACKER_ROI(0,180)-(260,620) 边界在本地校验云点
- `TrackerLinkRankerCloudShadowService` — keepsLocalPassthrough：percent 未命中 → 本地绿链点击
- `ImagePreprocessWashedImageClient` — 无 native window geometry 时 windowWidth/Height 回退为 raw 图尺寸
- `CloudImageProcessor` — requireCloudOutput：非 CLOUD_EXECUTED 或缺必需输出 → REQUIRED_FAILURE（禁止本地视觉回退）
- `ImagePreprocessCloudService` — MIN_EXECUTE_CONFIDENCE=0.50 本地阈值门
- `ImagePreprocessCloudService` — 未激活/未提供 payload → DISABLED/REQUIRED_FAILURE 禁止本地洗白
- `SheyaoxiangStatusCloudDecisionService` — 未激活/校验失败/云非法 → FAIL_CLOSED，禁止本地 OCR/模板学习/猜测补香
- `TeamRoleTooltipCloudDecisionService` — 未激活/校验失败/云非法 → REQUIRED_FAILURE（无本地 OCR 回退）
- `SummonSkillCloudDecisionService` — MIN_EXECUTE_CONFIDENCE=0.70 本地阈值
- `SummonSkillCloudDecisionService` — 未激活/校验失败/云非法 → UNKNOWN REQUIRED_FAILURE，禁本地黄字/模板回退
- `TrackerPanelReaderCloudDecisionService` — 硬编码 WINDOW 1024x768 校验云点/links
- `TrackerPanelReaderCloudDecisionService` — 未激活/AMBIGUOUS/ERROR/云非法 → REQUIRED_FAILURE 显式 no-click，禁本地扫链
- `RouteCloudDecisionService` — 硬编码 WINDOW 1024x768 与 ROUTE_RESULT_ROI(348,376)-(671,514) 校验云点；未激活→localOnly 本地点；percent 未命中→passthrough
- `DialogPolicyCloudDecisionService` — MIN_EXECUTE_CONFIDENCE=0.50 / MIN_PRE_CLICK_CONFIDENCE=0.70 本地阈值
- `DialogPolicyCloudDecisionService` — keepsAfterLocalSafeNoActionPassthrough：本地 NO_DIALOG/STORY_IGNORED 安全无动作可 passthrough（非云成功）
- `ObjectiveTextReaderCloudDecisionService` — 云 miss/传输失败/未激活 → Optional.empty，调用方保留既有 miss/recovery 语义
- `MiniMapLocationCloudDecisionService` — MIN_EXECUTE_CONFIDENCE=0.50、MAX_REASONABLE_COORDINATE=999 本地阈值
- `MiniMapLocationCloudDecisionService` — 未激活→DISABLED；校验/云非法→REQUIRED_FAILURE，禁本地数字/模板续跑
- `NpcClickSmartCloudDecisionService` — MIN_EXECUTE_CONFIDENCE=0.70；hotkey 白名单 ALT_4/ALT_C/ALT_A；未激活→DISABLED，校验/云非法→REQUIRED_FAILURE 无本地回退
- `NavigationRoutePlanCloudDecisionService` — 无绑定/未激活/echo 失配/畸形 → 结构化失败，caller fail-closed(MAP_NOT_REACHED)

**other** (9)

- `TaskRecoveryCloudDecisionService` — ALLOWED_ACTIONS 白名单硬编码（retry-current-phase/recover-to-main-task/loop-guard 等）在本地
- `ImagePreprocessWashedImageClient` — ROI 硬编码为 (0,0, min(rawW,winW), min(rawH,winH)) 全图
- `ImagePreprocessCloudService` — returnMode 按 operation 硬编码分类（RETURN_WASHED_IMAGE/RESULT_VALUES/CANDIDATES）
- `NpcClickStrategyCloudDecisionService` — 激活与否都返回 cloudRejectedNoClick——防止遗留本地策略被当成功路径
- `TrackerPanelReaderCloudDecisionService` — wubei 且 CLICK/REROLL 时强制要求 taskKey——任务码约束在本地
- `RouteCloudDecisionService` — transport-disabled(未启用/缺 baseUrl/token) → 结局 skipped；CR208 兼容旧 mode= 键
- `DialogPolicyCloudDecisionService` — 硬编码模板路径 OPTION_GIVE_TEXT、XIULUO_ENTER_BATTLE(看打)、rawCoordinateField 黑名单在本地
- `NavigationPointCloudDecisionService` — 每次调用 UUID request id + clientFrame 走 navigationRequestId 回显门；无绑定=结构化失败(fail-closed)
- `NpcClickSmartCloudDecisionService` — transport-disabled(未启用/缺 baseUrl/token) → 结局不提交(返回 false)，不改已验证点击；directCombat 三事实喂云授权

</details>

### 2.11 cloud/xiuluo + cloud/runtime（修罗云端 + 运行时决策 shadow 参照层）

| className | role | tier | currentAuthority | cloudOwner | localRetained |
|---|---|---|---|---|---|
| `XiuluoBrainCloudDecisionService` | 修罗脑的本地 fail-closed 瘦客户端适配器：把 start/step/actionOutcome 请求转成 CloudDecisionRequest，交 CloudDecisionCoordinator.shadow 决策，并用身份门校验云返回的命令后才放行。 | A | 云端持有全部相位/动作/retry/cleanup 决策；本地仅持有身份匹配安全门（windowId/taskRunId/sessionId/stateSeq 单调/phaseToken/actionId）与 fail-closed 判定，无本地业务决策。 | 修罗云端 XiuluoBrain 决策 Service（经 CloudDecisionCoordinator/CloudDecisionServiceId.XIULUO_BRAIN 路径） | 身份/协议安全门 + fail-closed 信封翻译（CloudDecisionExecutionGate 二次校验）。本地无 executor、无 capture、不产生任何相位或动作决策——这是'本地无剩余大脑'的核心证据类。 |
| `RuntimeDecisionShadowService` | 运行时决策的 fire-and-forget 影子上报器（CR-HC-011 shadow 波）：调用方传入'已由本地业务做出的 localDecision'，本类补齐窗口元数据后 delegate 给 coordinator.shadow，故意不返回结果。 | D | 本地既有业务代码持有 localDecision 权威（本类不改不消费）；云端仅作影子采样对照。 | CloudDecisionCoordinator shadow 框架（对应各 CR-HC CloudDecisionServiceId） | 仅窗口上下文捕获/富化（windowId/role/selectedTaskType/hwnd/nativeTitle）与 trace 拼装；不返回 effectiveDecision，业务侧无法误消费云结果。纯 capture + 上报，无决策。 |
| `XiuluoBrainStartRequest` | start 请求 DTO（@Value/@Builder）：承载 taskCode/source/windowId/taskRunId/initialPhase/context。 | D | 纯输入载体，无逻辑；其中 windowId/taskRunId 是 A 层身份门的输入（tier 定义中'输入'属 A，但本类本身无决策）。 | 修罗云端 XiuluoBrain Service（作为请求契约） | 无（DTO） |
| `XiuluoBrainStepRequest` | step 请求 DTO：承载身份三件套 + sessionId/stateSeq/phaseToken/phase/lastActionId/context。 | D | 纯输入载体；sessionId/stateSeq/phaseToken 为 A 层时序/身份门输入，本类无判断。 | 修罗云端 XiuluoBrain Service（请求契约） | 无（DTO） |
| `XiuluoBrainActionOutcomeRequest` | 动作结果上报 DTO：身份 + actionId/outcome/transactionResult/yieldPolicy/localOutcomeNextPhase/message/evidencePaths。 | D | 纯输入载体；localOutcomeNextPhase 按 Service javadoc 明确为'诊断证据，非本地转移权威'，不得当作本地相位决策。 | 修罗云端 XiuluoBrain Service（outcome 契约） | 无（DTO） |
| `XiuluoBrainDecision` | step/start 决策结果信封 DTO：Status(ACCEPTED_CLOUD_COMMAND/LOCAL_SAFETY_DENIED/CLOUD_REQUIRED_FAILURE) + cloudResult + response + rejectReason。 | D | 结果载体；仅暴露 Status 判定谓词，无业务分支。 | 修罗云端 XiuluoBrain Service（结果契约） | 无（DTO） |
| `XiuluoBrainActionOutcomeDecision` | outcome ack 结果信封 DTO：Status(ACCEPTED_OUTCOME/DUPLICATE_REPLAY/RESET_REQUIRED/LOCAL_SAFETY_DENIED/CLOUD_REQUIRED_FAILURE) + outcomeStatus/rejectReason/resetReason。 | D | 结果载体；isAcceptedOutcome 把 DUPLICATE_REPLAY 视同接受、isResetRequired 谓词，均为读取，无决策。 | 修罗云端 XiuluoBrain Service（outcome ack 契约） | 无（DTO） |
| `XiuluoBrainActionType` | 修罗云命令动作协议枚举：EXECUTE_PHASE/RUN_CLEANUP/WAIT_FOR_EVENT/COMPLETE_ROUND/RESTART_ROUND/FAIL_TASK/STOP_TASK。 | D | 协议词表；由云端下发、本地 parseDecision 校验，本枚举自身无逻辑。 | 修罗云端 XiuluoBrain Service（动作协议） | 无（枚举常量） |
| `XiuluoBrainResponse` | 解析后的云命令 DTO：windowId/taskRunId/sessionId/stateSeq/phaseToken/acceptedPhaseToken/phase/actionType/actionId/cleanupType/retryKey/attempt/maxAttempts/reason/diagnostics。 | D | 云命令载体，由 parseDecision 填充；字段全部为云端权威值，本地只读。 | 修罗云端 XiuluoBrain Service（命令契约） | 无（DTO） |

<details><summary><b>隐式状态清单</b>（15 条，按 kind 分类）</summary>

**memory** (1)

- `RuntimeDecisionShadowService` — 读取 windowTaskContextHolder.rawCurrent()（当前线程/窗口运行时上下文）做富化——只读取活体 capture，不自持跨请求记忆。

**fallback** (10)

- `XiuluoBrainCloudDecisionService` — decide()/actionOutcome() 在 !coordinator.isActive(XIULUO_BRAIN) 时 fail-closed 返回 cloudRequiredFailure("service inactive")——不执行任何本地兜底动作，纯拒绝。
- `XiuluoBrainCloudDecisionService` — 本地默认值：taskCode 缺失→"xiuluo_v2"，source 缺失→"xiuluo-brain"（normalize 兜底）。仅是标签默认，不影响决策。
- `RuntimeDecisionShadowService` — serviceId==null 或 !coordinator.isActive(serviceId) 时静默 return，采样被丢弃（fail-open 诊断语义，不影响业务）。
- `RuntimeDecisionShadowService` — 本地默认：taskCode 缺失→"unknown"，phase 缺失→"runtime-decision"。
- `XiuluoBrainStartRequest` — @Builder.Default taskCode="xiuluo_v2"、context=Map.of()、createdAt=Instant.now()（本地时间戳）。
- `XiuluoBrainStepRequest` — @Builder.Default taskCode="xiuluo_v2"、context=Map.of()、createdAt=Instant.now()。
- `XiuluoBrainActionOutcomeRequest` — @Builder.Default taskCode="xiuluo_v2"、context=Map.of()、evidencePaths=List.of()、createdAt=Instant.now()。
- `XiuluoBrainDecision` — @Builder.Default status=CLOUD_REQUIRED_FAILURE——fail-closed 默认：未显式接受即视为需云端/失败。
- `XiuluoBrainActionOutcomeDecision` — @Builder.Default status=CLOUD_REQUIRED_FAILURE——fail-closed 默认。
- `XiuluoBrainResponse` — @Builder.Default diagnostics=Map.of()。

**other** (4)

- `XiuluoBrainCloudDecisionService` — isRecoverableSessionResetReason(): RESET_REQUIRED 仅当 reason 含 "sessionid not found" 才判为可恢复，否则拒绝——这是唯一的本地恢复策略闸门，切换日需云端复刻。
- `XiuluoBrainCloudDecisionService` — stateSeq 单调推进校验（parse: command.stateSeq 必须 > expected.stateSeq；parseOutcome: 必须 ==）是本地防重放/防回退的隐式时序约束。
- `XiuluoBrainCloudDecisionService` — 无本地定时器/watchdog/超时、无本地缓存、无跨请求记忆、无 fair-lock 排队——服务完全无状态，身份全部来自入参。
- `RuntimeDecisionShadowService` — context 中 key=="source" 且入参 source 非空时改写为 "callerSource" 防覆盖；selectedTaskType 被强制改写为 activeTaskType（按当前请求任务而非窗口陈旧 UI 选择）——数据整形，非决策。

</details>

---

## 3. 方法级 tier 明细（附录）

切换必须逐一验证的是 **A（安全层）** 与 **B（业务决策）** 档方法：A 档漏迁=停机/身份/回合/暂停安全破面；B 档漏迁=phase/retry/fallback/超时/记忆决策落在本地=双脑。C/D 档仅计数（视觉解释与纯搬运，按位置搬迁/schema 校验处理）。

| tier | keyMethods 数 | 处理要求 |
|---|---|---|
| A | 133 | 切换必验：状态/协议/身份/lease/stop-pause |
| B | 200 | 切换必验：phase/动作/retry/fallback/timeout/memory 决策 |
| C | 62 | 计数：视觉/OCR/模板解释，capture 留本地 |
| D | 55 | 计数：纯搬运/DTO/枚举/契约，schema 校验 |

> 2026-07-15 03:25 EDT：hard ledger 仍 `189/407`；37 个去重 count unit 已父级源码放行、等待统一 fresh
> Maven。TaskTracker read/materialize 同一 count unit 最终采用 29 Java 双仓完整 scope；新增 port/handler/
> broker/final-consumed 文件不得拆成独立计数。

### 3.1 tier A 方法清单（133 个，安全层，切换必验）


**task/(直下) + task/hotstart + task/pause + task/startup + task**

- `AutoBattleTask::stop` — 安全层：置 botStatus=IDLE/actionState=FREE 以退出循环，但仅改本地状态需循环轮询点观察
- `TaskStartupCheckService::checkFiveRing` — 配置门关→放行；否则实时检测角色，shouldRunFiveRing 判定 skip/allow
- `TaskStartupCheckService::checkAutoBattle` — 身份准入：member 放行 / leader skip / 未知按配置放行或 skip（不实时检测）
- `TaskStartupCheckService::roleFromContext` — 仅从 context.windowRole 映射 MEMBER/LEADER/UNKNOWN（身份输入解析）
- `TaskTeamAssignmentPolicy::resolveTaskForRole` — 身份→有效任务重映射的核心裁决（成员/单人/未知的准入分支）
- `TaskTeamAssignmentPolicy::shouldDetectRoleBeforeStart` — 五环或 leader-only 任务需启动前实时角色检测（决定是否触发抢前台采集）
- `BaseTaskTemplate::execute(TaskExecutionContext)` — 生命周期主控：beforeTask→逐步 throwIfStopRequested+执行+结果转换→afterTask；异常收敛为 STOPPED/FAILED（stop 安全层）
- `BaseTaskTemplate::beforeTask/afterTask` — 状态机迁移：before 置 RUNNING；after 按结果置 IDLE+FREE 或 ERROR
- `BaseTaskTemplate::stop` — 安全层：置 IDLE+FREE 请求停止
- `BaseTaskTemplate::sleepSafely` — stop 感知的可打断休眠（TaskSleep.sleepOrStop）
- `TaskTurnCoordinator::enter` — 获取回合：同线程深度+1；否则 lockInterruptibly（中断→TaskStopRequestedException），记录 handoff 延迟
- `TaskTurnCoordinator::leave` — outcome==null 或 shouldYield→releaseAll，否则降深度保留回合（回合释放安全层）
- `TaskTurnCoordinator::tryRun` — tryLock 抢到才跑短维护动作，finally 强制释放（非阻塞回合尝试）
- `TaskTurnCoordinator::forceRelease/releaseAll` — 释放全部持有深度、清 ThreadLocal、unlock 并记录释放事实
- `TaskTransactionRunner::run` — enter→safeRun→构 outcome→finally 上报指标+leave（回合安全包裹）
- `TaskTransactionRunner::runExclusive` — 同时持回合与独占输入 worker；completed=false 表示独占输入失败/被中断
- `TaskTransactionRunner::forceReleaseTurn` — 清理/错误退出时强制释放回合

**2026-07-17 01:22 CR271 source-start gate：** TURN-35/36/37 三张完整 Task 卡写集互斥；TURN-26/27
调整为三卡最终 `approvalDependsOn`，不再作为 source-start 阻断。External C 已领取 TURN-36，External d 已
领取 TURN-37，TURN-35 仍 `READY / ZERO OWNER`；保持整卡单 owner、自领、不拆 fragment。最终批准仍等待
26/27、Foundation T01-T04、named tests 与 Cloud compile。TURN-26 B owner 保持 repair-active，但因连续两轮
未回执父级 review 消息暂标 `COMMUNICATION_STALE`；源码增量证明并未停写。

**task/wubei (五倍) — THIN_CLIENT_V1 A-1 方法级迁移矩阵底账**

- `WubeiTask::execute(TaskExecutionContext):366` — 任务生命周期：maxRuns 轮循环、启动首援/摄妖香、维护初始化、stop/异常→STOPPED/FAILED、finally 强制释放 turn。
- `WubeiTask::runRoundPhases:537` — 轻量相位机主循环：pause-resume reconcile、stop 检查、动态事务(turn)、loop guard>32、失败→recover、park/yield 调度。
- `WubeiTask::runPhase:1658` — phase switch 分派 + 捕获 PreBattleBudgetTimeoutSignal 统一消费；入口先跑 probe 300s 超时闸。
- `WubeiTask::resolvePauseResumeTaskHotStart:642` — 暂停恢复指纹不匹配的隐式 fallback 热启动，清 volatile 探测/战斗 runtime。
- `WubeiTask::checkReadyPriorityBeforePhase:817` — phase 前优先闸：先消 post-combat/pre-battle 超时、当前 prepared 动作、其它窗口 prepared 让位。
- `WubeiPhase::isTerminal:27` — ROUND_DONE/FAILED/STOPPED 终止相位机循环的协议判定。
- `WubeiRoundContext::next(WubeiPhase,String):47` — 清零 phaseRetryCount/等待标志、保留 recoveryCount 的正常相位跃迁。
- `WubeiRoundContext::waitForPathing/clearPathingWait/waitForAcceptDialog:60-70` — 寻路/接任务对话等待布尔的置位/清除（调度状态）。
- `WubeiRoundContext::hotStart/normalStart/routeToAcceptNpc:34-42` — 本轮起始状态工厂（热启动/正常/清队过渡强制接任务）。
- `WubeiStepOutcome::failed:58 / stopped:67` — 把状态置 FAILED/STOPPED 相位并给 MUST_YIELD——stop/pause 安全表达。

**task.wuhuan (五环 FiveRingTaskV2 相位状态机组)**

- `FiveRingPhase::isTerminal` — 判定 FINISHED/FAILED/STOPPED 三终态，决定 runPhases 主循环是否停止——状态协议的收敛条件
- `FiveRingStepOutcome::pathingStarted / sharedState` — 映射为 MUST_YIELD + PATHING_STARTED/SHARED_STATE_TRIGGERED，触发让出粗粒度 turn，属 turn 安全协议
- `FiveRingStepOutcome::continueTo` — CONTINUE_CHAIN 保持 turn 不让出，链式继续
- `FiveRingPhaseContext::next` — 相位跃迁：清空 waiting/intent/movement/combat/baseline，进 WAIT_PATHING 时打新寻路时间戳，保留 uiErrorCount——状态转移的时序核心
- `FiveRingPhaseContext::pauseInternalAutomationTimers` — 暂停恢复的定时器补偿，属 stop-pause 安全层
- `FiveRingPhaseContext::pauseResumeHotStart` — 热启动重置为 PREPARE 并清空易失态，保留 round/cleanTransition
- `FiveRingPhaseContext::withPathingStarted / withNewWatcherPathingStarted` — 打寻路时间戳与 watcher intent 期望源，forceNewStart 控制是否重置起点——寻路协议/lease
- `FiveRingPhaseContext::withTaskAccepted` — 置本轮已接任务身份标志
- `FiveRingTaskV2::runPhases` — 相位主循环：每轮 pause/resume 对账+定时器补偿+stop 检查、按相位选 in-turn 或 outside-turn 执行、MAX_PHASE_LOOP_GUARD 空转保护、据 outcome 判 STOPPED/FAILED/MUST_YIELD/terminal
- `FiveRingTaskV2::checkReadyPriorityBeforeOutsidePhase` — 进 outside-turn 相位前的跨窗口 ready-event 优先仲裁：先消费本窗 prepared tracker/route，再让位其它窗口 fresh prepared/terminal——多窗口调度安全
- `FiveRingTaskV2::runPhaseWithoutTaskTurn / shouldReleaseTurnOnOutsidePhaseEnter / releaseHeldTurnAfterOutsidePhaseYield` — turn lease 管理：长 OCR/寻路相位前释放粗粒度 turn、据 source 判是否保留继承 turn、让出后强制释放继承锁——压测让手模型核心
- `FiveRingTaskV2::isUsablePathingSnapshot / isExpectedPathingSource / isExpectedPathingTarget` — 寻路 intent 协议校验：源前缀匹配+目标类型(UNTARGETED_TRACKER/长安坐标)匹配+createdAt grace 守卫，防消费上一次导航的陈旧快照
- `FiveRingTaskV2::markTaskIdle / markTaskFailed` — 写 GameContext BotStatus 与 ActionState(FREE)，终态清理

**task/xiuluo (修罗 XiuluoTaskV2 及其状态/DTO/枚举，THIN_CLIENT_V1 A-1**

- `XiuluoTaskV2::execute(TaskExecutionContext)` — 任务入口：轮次循环、maxRuns、启动摄妖香/急救、BotStatus、stop 短路、finally 强制释放 turn
- `XiuluoTaskV2::runRoundWithXiuluoBrain` — 云脑命令循环：session/stateSeq/phaseToken/actionId 协议、outcome 上报、RESET_REQUIRED resync、热循环 guard(33)、失败→restartAfterFailure
- `XiuluoTaskV2::executeXiuluoBrainCommandShell` — 命令 shell：白名单可执行 phase + WAIT_FOR_EVENT park reason（WAIT_COMBAT/WAIT_TEAM_RETURN/两维护check/WAIT_TRACKER_SHORTCUT_PATHING）；本地 yield guard<8；分发 EXECUTE/CLEANUP/WAIT/RESTART/COMPLETE/STOP/FAIL
- `XiuluoTaskV2::waitForXiuluoBrainEvent` — 真实 event park 协议：consume pendingWaitSpec→yieldAfterMustYield，realParkCompleted 判定，WAIT_TRACKER_SHORTCUT_PATHING 醒后直连消费本地 kanda prepared(CR256)
- `XiuluoTaskV2::restartXiuluoBrainAfterSessionReset` — RESET_REQUIRED：以 localOutcomeNextPhase 为 resumePhase 重新 start；终态 phase 拒绝 reset
- `XiuluoTaskV2::checkPreCombatWatchdogTimeout / shouldApplyPreCombatWatchdog` — 180s 进战前 watchdog 判定与 phase 白名单；超时清 pathing/绿链 schedule/prepared 并报 watchdogTimeout
- `XiuluoTaskV2::boundedPreCombatWaitTimeoutMs / remainingPreCombatWatchdogBudgetMs` — park 前把请求超时钳到 watchdog 剩余预算；预算耗尽→立即 watchdog timeout outcome
- `XiuluoTaskV2::parkAfterYieldIfNeeded` — event park：awaitNewer/awaitNewerPathingTerminal、pause 指纹 reconcile→hot-start fallback、CR244/245 member-fact park 时间补偿、醒后再核 watchdog 预算
- `XiuluoTaskV2::yieldAfterMustYield` — 让权：有 waitSpec 走 maintenance-before-park 或 park；无则插入公平锁 handoff 延迟防 leader 立即重夺锁
- `XiuluoTaskV2::parkForMaintenanceBroadcastQueue` — arm WAIT_MAINTENANCE_BROADCAST_QUEUE waitSpec（afterSequence 在读 drained 前捕获，防事件丢失），5s cap
- `XiuluoRoundContext::pausePreCombatTimer` — 维护/暂停后平移 watchdog 起点，<500ms 或无 active timer 不动
- `XiuluoRoundContext::withShortcutTrackerClick` — 记录 shortcut 点击并设 firstTrackerGreenClickAtMs 作为 shortcut watchdog 锚
- `XiuluoBrainRoundState::noteCommandCycleAndCheckExceeded / noteImmediateLoopAndCheckExceeded / noteRealEventWaitCompleted` — 热循环 guard(阈值 33)计数与重置，真实 event park 完成清零
- `XiuluoBrainRoundState::recordOutcome / executionStateFor / consumePendingWaitSpec` — 承接 phase outcome 的 nextState 与 waitSpec 暂存/取用
- `XiuluoPhase::isTerminal` — ROUND_DONE/FAILED/STOPPED 终态判定，多处 shell 白名单与 watchdog 排除依赖

**service/ A–M (THIN_CLIENT_V1 A-1 方法级迁移矩阵底账)**

- `AutoCombatPanelService::recordAutoPanelMissing` — 缺失连续超10分钟升级为人工注意告警（安全层监控）
- `AutoCombatService::handleCombatTick` — 主编排：队员相位消费 vs 只读退化 vs 自雷达；进战/退战/恢复/维护全在此分支
- `AutoCombatService::authorizeCombatDetectionAfterEnterBattleAction` — lease 授予——仅真实进战物理动作成功可调，识别/导航不得调用
- `AutoCombatService::revokeCombatDetectionAuthority` — lease 撤销，下一轮须重新授权
- `AutoCombatService::mayRunBattleRadar` — 探测门禁：未授权且非IN_COMBAT的xiuluo/wubei窗口禁跑模板雷达
- `AutoCombatService::reconcileReturnHomeVerifiedCombatState` — 地图证明 > 陈旧watcher状态：清IN_COMBAT、广播队伍退战、撤销授权
- `AutoCombatService::initializeForCurrentWindow` — 任务起始重置计数器与覆盖历史，失效队长相位
- `BattleRadarService::checkAndSyncCombatState` — 4阶段探测(autoFlag/命令按钮/顶部图标/保守退战)驱动 ActionState 权威转换
- `BattleRadarService::updateCombatState` — 状态跃迁核心：置 IN_COMBAT/FREE 并发 enter/exit 一次性信号
- `BattleRadarService::applyExternalCombatStateVerdict` — 吃外部权威(本地队长广播)裁决替代自身模板雷达——迁移锚点
- `ClientIdentityService::scanAndSyncIdentity` — 解析标题写入 me 的 server/name/id（身份权威落地）
- `ClientIdentityService::resolveCurrentWindowTitle` — 多窗口标题优先级回退解析
- `LeftTopStatusSwitchService::checkAndMaybeClose` — 探测+仅OPEN且allowClick才点击的核心安全闸

**service (N–Z) + service/dialog — THIN_CLIENT_V1 A-1 method-l**

- `NavigationService::navigateToNPC` — map->currentMap 两段编排；每段前后 stop-checkpoint；到达后不清对话交任务层
- `NavigationService::navigateToMapCloudPlan` — 路线阶梯 shell：观测布尔上报云端->执行单条指令->terminal-fact-gate；finally 三守卫决定是否注册 pathing-intent
- `NavigationService::executeRoutePlanAction` — 把云端单条指令映射到本地既有复合执行器(消费预备路线对话/确认当前图/清陈旧准备/世界地图点击/关搜索面板)
- `NavigationService::buildBackedRoutePlanTerminal` — 终局事实闸门：每个 messageKey 必须有本地已建立事实支撑，否则返回 null=>失败
- `NavigationService::navigateInCurrentMap` — 60s 循环消费云端 mini-map 候选批；点击确认起步后注册 intent；keep-turn 语义;IN_COMBAT 打断
- `NavigationService::executeWorldMapPrepareAndClick` — 一次世界地图 attempt：CLICKED 时原子注册 intent(带坐标)+route 记忆
- `NavigationService::isRoutePlanIdentityStale/currentRoutePlanIdentity` — 身份/lease 时效闸门，防跨窗/relog 误点
- `NavigationService::registerWindowPathingIntent` — 本地保留的 pathing-intent 注册权(watcher 据此接管移动/到达/stopped-away)
- `NpcClickService::clickNpcSmartWithOutcome` — 唯一生产入口；打包结构化 terminal，区分 fifoConsumedUnverified 供 direct-combat 授权
- `NpcClickService::tryClickNpcSmartViaCloud` — inactive fail-closed；抓 story anchor；story-blocker 重启循环(限3)
- `NpcClickService::consumeNpcClickSmartCloudSession` — FIFO 消费：WAIT/END/INVALID/MEMORY/普通/CTRL 分派；边界处一次 in-memory story 事件读
- `NpcClickService::executeNpcClickSmartQueueCandidate` — safety-shell ROI 校验后原子 move+sleep+click；结果 verifier 校验
- `NpcClickService::executeCtrlMenuProbeDirect/scanCtrlMenuAndVerifyKeywordDirect` — Ctrl 按住+小环偏移 hover+模板匹配点击执行(纯执行+视觉确认)
- `NpcClickService::isWindowRelativePointInsideAllowedRegion` — 本地安全壳：拒绝云端越界点击坐标
- `SmartClickEvidenceConfirmationService::confirmExpectedOptionProof` — 提交 sourceTask/actionKey/matchedText/proofToken/verificationStrength 证据——学习记忆的 commit 边界
- `TaskMaintenanceService::openPostCombatFirstAidQueue/isPostCombatFirstAidHeadWindow/completePostCombatFirstAidAttempt` — 战后急救 FIFO 公平锁：谁是头窗、谁先补、完成后关队列
- `TaskMaintenanceService::openMaintenanceBroadcastQueue/consumeMaintenanceBroadcastQueueTurnIfHead/isDrained` — 维护广播队列头窗轮次公平锁
- `TaskMaintenanceService::enqueueSummonSkillIfAbsent/peekEligibleSummonSkillHead/moveRetryBackoffSummonSkillHeadsToTail` — 召唤兽清理排队+退避移尾(顺序权威)
- `TaskMaintenanceService::attachExistingLocalTeamSessionForMember/registerLocalTeamSessionCandidate/markLocalTeamLeaderDetected` — 本地队伍会话身份/leader 归属状态机
- `TaskMaintenanceService::openTeamCombatPhaseForLeader/memberTeamCombatPhase/invalidateTeamCombatPhaseForLeader` — 队伍战斗 phase 协议(leader 开/member 视图/失效)
- `TaskMaintenanceService::markPendingTeamReturnWindow/pendingTeamReturnWindowCount` — 待归队窗口集合(切换日事故点)
- `UICleanerService::canFastClickStoryDialog` — 队员剧情快点安全门(member 仅战斗中)

**cloud.decision**

- `CloudDecisionClient::decide(CloudDecisionRequest)` — local->cloud 协议入口，唯一契约方法
- `CloudDecisionCoordinator::shadow(request,localDecision,executionGate)` — 核心安全管道：enabled->serviceId->shadow/execute mode->client.decide->schema->percent->gate.evaluate->fallback/stop 全链裁决
- `CloudDecisionCoordinator::effectiveDecisionFor()` — EXECUTE+STOP+!executed->null，实现停机安全语义
- `CloudDecisionCoordinator::schemaMismatch()` — 校验响应 serviceId/traceId 与请求一致且 decision 非空，防串包/错配执行
- `CloudDecisionCoordinator::unavailable()/disabled()` — 构造安全回落信封（cloudAvailable=false, executed=false, 保持 local 或停机）
- `HttpCloudDecisionClient::decide(request)` — 发请求，处理 timeout/interrupt/IO/非2xx/空体，抛 CloudDecisionClientException
- `HttpCloudDecisionClient::ensureEnabled()` — 传输闸：realTransportEnabled+baseUrl+token 缺一即禁传输
- `CloudDecisionResult::isRequiredExecuteFailure()` — 必执行却失败的强停机信号

**cloud/task**

- `CapabilityGateCloudDecision::cloudExecuted` — allowed = localAllowed && cloudAllowed，云只能收紧
- `CapabilityGateCloudDecision::requiredFailureDeny` — 云必需失败一律 DENY
- `CapabilityGateCloudDecisionService::decide(...)` — 打包 capability/timeout/localReason 请求云，effectiveAllow=local&&cloud
- `CapabilityGateCloudDecisionService::capabilityGateExecutionGate` — 校验云 action∈{ALLOW,DENY}，否则 rejected
- `TeamReturnPolicyCloudDecisionService::decide(context,phase,source,localAllowed,...)` — effectiveAllow=local&&cloud，按 phase(member/leader) 打包
- `TeamReturnPolicyCloudDecisionService::teamReturnExecutionGate` — 校验 ALLOW/DENY
- `TeamReturnPolicyCloudDecision::cloudExecuted` — allowed=local&&cloud
- `TeamReturnPolicyCloudDecision::requiredFailureDeny` — fail-closed DENY
- `TaskPolicyCloudDecisionService::taskPolicyExecutionGate` — 云结果 STOPPED 不允许、本地 STOPPED 保留本地——停机安全层
- `TrackerLinkRankerCloudShadowService::windowRelativeClickExecutionGate` — 校验 diagnostics.action/coordinateSpace 与窗口/ROI 边界
- `ImagePreprocessCloudService::imagePreprocessExecutionGate` — 置信度+operation 匹配+washed 图 sha 校验+候选坐标边界
- `ImagePreprocessCloudService::parseWashedImage` — base64 解码+SHA 比对+PNG 尺寸校验，防伪造洗白图
- `NpcClickSmartCloudSession::accepted` — STARTED 且身份三元齐全才算有效会话
- `TeamRoleTooltipCloudDecisionService::detect(request)` — 返回 CLOUD_FOUND(role+leaderClientId)/NO_RESULT/REQUIRED_FAILURE
- `TeamRoleTooltipCloudDecisionService::parse` — FOUND 必须 role∈{LEADER,MEMBER} 且有 leaderClientId——身份完整性门
- `RouteCloudDecisionService::routeExecutionGate/parse` — routeMode 规范化+CLICKED 须 click+routeDecisionId+ROI 校验
- `DialogPolicyCloudDecisionService::decidePreClick(request)` — 覆盖路径预点击：云 CLICK 须 plain-left+WINDOW_RELATIVE+ROI 内+actionId 白名单匹配
- `DialogPolicyCloudDecisionService::preClickActionIdValidationError` — 按 optionPolicy 逐类校验 actionId 与 targetKeyword/模板名一致——防越权点击
- `NavigationPointCloudDecisionService::resolveMiniMapClickBatch(request)` — 取有序候选批，binding-echo+batchExpiresAtMs 校验
- `NavigationPointCloudDecisionService::checkCoordinatePlausible / resolveApproachCoordinate` — 标量替代本地 CoordinateHelper，同一 echo 门，失败→empty fail-closed
- `NavigationPointCloudDecisionService::echoMismatch` — windowId/hwnd/taskRunId/navigationRequestId/clientFrame 五字段回显校验
- `NpcClickSmartCloudDecisionService::decide(request)` — 单次问云要 NPC 点击/no-action，坐标须 WINDOW_RELATIVE+scan region 内
- `NpcClickSmartCloudDecisionService::startSession/pollNext/reportOutcome` — FIFO 会话协议：起会话/取消息/回报结局
- `NpcClickSmartCloudDecisionService::parse/queueMessageGate` — 动作枚举+修饰键+ROI/scan-region 边界校验
- `NavigationRoutePlanCloudDecisionService::decideNextStep(request)` — 上报梯子布尔事实+上一步结局，取一条 directive
- `NavigationRoutePlanCloudDecisionService::executionGate/echoMismatch` — status=HIT 且五字段 echo 通过；ACTION 须 action，TERMINAL 须 terminalStatus+messageKey

**cloud/xiuluo + cloud/runtime（修罗云端 + 运行时决策 shadow 参照层）**

- `XiuluoBrainCloudDecisionService::start(XiuluoBrainStartRequest)` — 构造 start 上下文并走 decide；无本地 initialPhase 决策，phase 仅作 trace/context 证据。
- `XiuluoBrainCloudDecisionService::step(XiuluoBrainStepRequest)` — 先 invalidStepIdentity 本地必填身份前置门（缺 windowId/taskRunId/sessionId/stateSeq>0/phaseToken 直接 localSafetyDenied），再走云决策。
- `XiuluoBrainCloudDecisionService::actionOutcome(XiuluoBrainActionOutcomeRequest)` — 上报执行事实的 ack；invalidActionOutcomeIdentity 校验 actionId/outcome 必填，inactive 时 fail-closed；不推进 phase/state。
- `XiuluoBrainCloudDecisionService::decide(...)` — 统一 fail-closed 入口：inactive→拒绝，否则 coordinator.shadow + executionGate，再 toDecision。
- `XiuluoBrainCloudDecisionService::parse(response, expected) / parseDecision(decision)` — 核心安全门：校验 windowId/taskRunId 相等、sessionId 相等、stateSeq 单调、phaseToken/acceptedPhaseToken 相等，并做 action↔phase 协议一致性（COMPLETE_ROUND⇒ROUND_DONE、STOP_TASK⇒STOPPED、RESTART_ROUND⇒PREPARE_ROUND、FAIL_TASK⇒FAILED、EXECUTE_PHASE/RUN_CLEANUP 非终态且可执行、RUN_CLEANUP 必带 cleanupType/retryKey/attempt/maxAttempts）。全是对云命令的校验而非自决。
- `XiuluoBrainCloudDecisionService::parseOutcomeDecision(decision, expected)` — outcome ack 身份全等校验 + status∈{ACCEPTED,DUPLICATE_REPLAY,RESET_REQUIRED} + RESET_REQUIRED 可恢复性校验。
- `XiuluoBrainCloudDecisionService::isCloudExecutableCommandPhase(XiuluoPhase)` — 可执行相位白名单（PREPARE_ROUND…WAIT_TEAM_RETURN）；EXECUTE_PHASE/RUN_CLEANUP 目标相位必须命中，属协议约束。
- `XiuluoBrainCloudDecisionService::xiuluoBrainExecutionGate / xiuluoBrainOutcomeExecutionGate` — CloudDecisionExecutionGate 实现：仅 serviceId==XIULUO_BRAIN 放行，evaluate 内再跑 parse/parseOutcomeAck，未过则 rejected。
- `XiuluoBrainCloudDecisionService::toDecision / toActionOutcomeDecision` — 把 CloudDecisionResult 翻译成本地信封；未执行则区分 localSafetyDenied（reason 含 "local safety denied"）与 cloudRequiredFailure。

### 3.2 tier B 方法清单（200 个，业务决策，切换必验）


**task/(直下) + task/hotstart + task/pause + task/startup + task**

- `AutoBattleTask::execute(TaskExecutionContext)` — 前置 startupCheck 门 + 启动急救/维护/combat 初始化 + while 主循环按 tickResult 与 actionState 分支决定 idle 维护与 sleep 节奏
- `AutoBattleTask::maybeRunIdleMaintenance` — idle 维护决策核心：归队自检→维护广播队列头→待本地 leader 检测挂起判定→本地支援会话/leader 暂停门→跟随支援态与队伍寻路门→runOpportunisticMaintenance 组装请求
- `AutoBattleTask::tryRunLocalTeamReturnSelfCheck` — CR244 成员归队自检：标记探测 UNKNOWN 保持不动、ABSENT 才清挂起、PRESENT 先消费 common-box 再点归队（fallback 与本地记忆集）
- `AutoBattleTask::isFollowerSupportMode` — 判定该实例是否为其他 leader 任务的静默成员助手，影响 summon-skill/寻路门
- `AutoBattleTask::getPollingIntervalMs` — 选择下一 tick 轮询间隔：FREE 下依据待急救/开放广播队列切换 500ms 或 3000ms，战斗态用动态雷达间隔
- `AutoBattleTask::getRetryPolicy` — 覆盖为 TaskRetryPolicy.none()（本任务不重试）
- `SleepComputerTask::execute(TaskExecutionContext)` — stop 检查→1500ms 可打断延时→调用系统睡眠；执行前的 stop 门是安全关键
- `TaskPauseResumeReconciler::capture` — 采集暂停前/后指纹（含 8s 龄期对话过滤），构成对账的本地记忆基准
- `TaskPauseResumeReconciler::reconcileAfterPause` — 决策核心：matched→补偿定时器续跑；否则清易失态返回 fallback 热启动
- `TaskPauseResumeReconciler::mismatchReason` — 按 windowId→phase→actionState→preparedAction→visibleDialog→pathing 顺序判定首个不匹配点（分支时序权威）
- `BaseTaskTemplate::getRetryPolicy` — 解析 context 重试策略，缺省 none
- `TaskStepExecutor::execute(context,step,overridePolicy)` — while 重试循环：stop 检查→执行→异常时 canRetry 则延时重试否则 FAILED（retry/fallback 决策）
- `TaskStepExecutor::resolveRetryPolicy` — override→context→none 的重试策略优先级
- `TaskStepExecutor::delayBeforeRetry` — 重试前可打断延时
- `TaskTurnCoordinator::shouldYield` — 据 result(STOPPED/FAILED/RETRYABLE/TASK_FINISHED/PATHING_STARTED/SHARED_STATE_TRIGGERED)+yieldPolicy 决定是否释放（业务让渡决策）
- `TaskTransactionRunner::runDynamic` — 回调返回 result+effective yield（云端策略决策后决定回合去留），fallbackYieldPolicy 兜底
- `TaskTransactionRunner::safeRun/safeRunDecision` — 异常→结果收敛的 fallback 映射（STOPPED/FAILED），RuntimeException/Error 透传

**task/wubei (五倍) — THIN_CLIENT_V1 A-1 方法级迁移矩阵底账**

- `WubeiTask::consumePostCombatIdleTimeoutBeforeNormalPhase:848` — POST_COMBAT_IDLE_TIMEOUT watchdog→清全runtime回ROUTE重接（超时后动作决策）。
- `WubeiTask::consumePreBattleBudgetTimeout:920 / pendingPreBattleBudgetTimeoutEvent:896` — 180s 全局开战预算超时最高优先级消费→清全runtime重接。
- `WubeiTask::recoverRoundAfterFailure:659` — 失败恢复：recoveryCount>=3→FAILED，否则 recoverTo(ROUTE_TO_MAIN_TASK) 重启本轮；调用 decideTaskRecovery 影子。
- `WubeiTask::applyTaskPolicyCloudDecision:690` — TASK_POLICY 影子：本地决出 outcome 后云端可否决/覆盖下一 phase（双脑，迁移须切云端权威）。
- `WubeiTask::decideTaskRecovery:771` — TASK_RECOVERY 影子：失败恢复上报云端，可 cloud.required 拒绝→FAILED。
- `WubeiTask::runHotStartDetectPhase:1694` — 热启动分支：有活动任务→READ_TRACKER；after-combat 返回道具兜底→WAIT_TEAM_RETURN；否则 ROUTE。
- `WubeiTask::runReadTrackerPhase:2018` — 权威快照边界：读tracker、判暗雷reroll、置chained/probe标志、启probe计时(据云端OCR title-template key)。
- `WubeiTask::isTrackerDarkThunderTask/ProbeTask/ChainedCombatTask/isTrackerTask:2813-2830` — 基于云端 title-template taskKey(dianqian_xianyi/baoxiang_miqing/zhidou_huangpao) 做本地任务分类分支。
- `WubeiTask::startDarkThunderAcceptNpcReroute:2074` — 暗雷怪 reroll：当前地图重导航到接任务NPC换目标（wubei独有）。
- `WubeiTask::runAfterAcceptMaintenanceCheck:1722 / runBeforeTrackerPathingMaintenanceCheck:1741` — 接任务后/寻路前两段维护 phase 门（医宝宝 vs 修装备），含 common-box TTL 消费。
- `WubeiTask::triggerHealPet/RepairEquipment/MaintenanceBroadcastBeforeTracker:1753-1808` — 维护判定：cooldown due + 连续失败限3 + 5次hook尝试；含 closeLeftoverMaintenanceDialogBeforeSkip 绿字前清残留对话安全。
- `WubeiTask::runTrackerPathingPhase:2094` — probe→startProbeTrackerPathing 双绿字，否则 triggerCombatTrackerPathing 普通/黄袍。
- `WubeiTask::runResolveAfterPathingPhase:2125` — 寻路终态解析：ACTIVE等待、STOPPED_AWAY无移动→重导航、否则→ENTER_BATTLE。
- `WubeiTask::resolveProbeAfterPathing:2947` — 显形镜探测状态机：用道具→等story→target-ready点击/wrong-position重寻/absent重试/切下一绿字/耗尽FAILED（wubei独有）。
- `WubeiTask::timeoutProbeTaskBeforeBattleIfNeeded:1579` — 探测任务 300s 未开战超时→清runtime回ROUTE重接。
- `WubeiTask::tickEnterBattle:4229` — 进战斗解析：IN_COMBAT→WAIT，consume prepared，known dialog点击，near-destination smart/direct兜底，enter-battle-retry回点绿字；6s节流。
- `WubeiTask::tickWaitBattleFinish:4411` — 等战斗：EXIT_RECOVERED→POST_BATTLE(chained分流)，IN_COMBAT广播+返回道具预扫，180s超时→FAILED，无战斗回enter-battle。
- `WubeiTask::returnHomeAfterCombatOrContinueSpecialTarget:4676` — 回程/黄袍续战核心：普通用返回道具验证；chained据战后tracker(快缓存/全读)判继续或回程（wubei独有）。
- `WubeiTask::continueChainedCombatFromTracker:4913 / clickCachedChainedTrackerGreen:4931` — 黄袍续战：绿字/快照缓存点击拉起下一场；经 TrackerLinkRanker 影子云决策。
- `WubeiTask::correctExpectedReturnFailureIfStillInCombat:4864 / resumeWaitBattleAfterTrustedReturnCorrection:4889` — 回程验证失败→trusted 战斗只读探测纠偏回 WAIT_BATTLE_FINISH。
- `WubeiTask::runWaitTeamReturnPhase:2274` — 归队等待：leader signal precheck→让位/结束；team-return-signal 属共享状态安全。
- `WubeiTask::runAcceptTaskPhase:2432 / afterAcceptTaskSucceeded:2512` — 接任务对话+成功后启异步tracker预读与prepath；点击本身为executor。
- `WubeiTask::shadowTrackerLinkSelectionIfLocal:3225 / clickTaskTrackerGreen:3247` — 绿字排序 TrackerLinkRanker 影子云决策(非authoritative)+本地绝对坐标点击执行。
- `WubeiRoundContext::retrySamePhase:51` — 同相位重试并 +1 retryCount，保留 pathing/accept 等待标志（重试决策承载）。
- `WubeiRoundContext::recoverTo:56` — recoveryCount+1 的广义本轮重启承载。
- `WubeiStepOutcome::pathingStarted:40 / sharedState:49` — 标记寻路/共享状态触发并 MUST_YIELD 让出 turn（调度决策承载）。
- `WubeiStepOutcome::withWaitSpec:76` — 附加 park-only 等待策略。

**task.wuhuan (五环 FiveRingTaskV2 相位状态机组)**

- `FiveRingStepOutcome::finishedTerminal vs finished` — terminalTask=true/false 决定是结束全部配置轮次还是仅结束本轮，影响任务级终止决策
- `FiveRingCompletionPolicy::decide` — final 模板可见→STOP_ALL_RUNS；once 模板可见且 round==1 时按 configuredRuns 决定停全部还是仅结束本轮；round>1 的 once 视为 NO_MATCH——完成语义的全部业务分支
- `FiveRingPhaseContext::nextAfterPreparation` — 据 cleanTransitionStartup 分支到 ACCEPT_TASK 或 HANDOVER_DETECT——准备后走向的业务决策
- `FiveRingPhaseContext::withWuhuanTrackerCombatBaseline / flushWuhuanTrackerCombatBaselineIfReplacing` — 战斗基线截图缓存写入/回收，供战斗退出证据判定
- `FiveRingPhaseContext::retrySamePhase / increaseUiErrorCount / resetUiErrorCount` — 重试与错误计数记忆，直接决定 syncTaskPanel 的清理/失败阈值行为
- `FiveRingTaskV2::execute(TaskExecutionContext)` — 主入口：读 wuhuanMaxRuns、startup 阻塞检查、按轮循环 runPhases、终态设置 BotStatus、finally forceReleaseTurn；round metrics 记录
- `FiveRingTaskV2::prepare` — 启动清屏(可跳过预检)、急救、一次开袋查摄妖香+鞋、鞋不足则 quickBuy 否则转 BUY_SHOES；成功后 nextAfterPreparation 分支
- `FiveRingTaskV2::buyShoes` — 已在店内则店主买鞋+返回；否则据 watcher 快照判进店寻路/到门下坐骑/已在店，含到达后注册 watcher intent 而非前台完成
- `FiveRingTaskV2::handleShoeShopDoorAfterArrival` — 到门未进店的下坐骑重试时序：Alt+C 一次→检测 flying→确认飞行再 Alt+C，各带确认超时
- `FiveRingTaskV2::detectHandover` — 交接检测：先试点 tracker link，title 可见→SYNC，tracker 不可读→ACCEPT 兜底，否则 ACCEPT 初次设置
- `FiveRingTaskV2::acceptTask` — 接任务重试环(MAX_ACCEPT_RETRY=5)：清 pathing 等待、消费 prepared tracker、已接则转 SYNC、就近直点或导航云游大师、接受选项、各失败分支就地兜底
- `FiveRingTaskV2::continueIfAcceptNpcNavigationStillPathing` — 接任务导航寻路等待：据 watcher 快照判 ARRIVED/near/STOPPED_AWAY/超时(90s)/still-active 多分支决定继续等或重试
- `FiveRingTaskV2::waitPathing` — 寻路等待核心：战斗门控(基线截图+ROI比对+证据交叉验证)、watcher 终态 ARRIVED→HANDLE_DIALOG / STOPPED_AWAY→(prepared route 延后/非给物图转SYNC/HANDLE_DIALOG)、90s 超时转 SYNC、grace/fast-wait 让出
- `FiveRingTaskV2::tryResolvePostCombatPositiveEvidence` — 战斗退出正向证据：prepared/fresh tracker link 点击→PATHING，或完成故事→终态；无证据回 null 继续等
- `FiveRingTaskV2::handleDialog` — 给物图判定→完成故事→给鞋→故事忽略(战斗则等/否则SYNC)→无对话转SYNC→give-option未找到清理→give 失败计数达 6 则 FAILED
- `FiveRingTaskV2::syncTaskPanel` — 左侧追踪同步主决策：点 tracker green link→PATHING；title gate + Runner-not-ready/no-green/no-link/click-failed/not-found 多状态×错误计数×清理/兜底/失败(9次)阈值
- `FiveRingTaskV2::resolveStoppedAwayTrackerIntentBeforeSync` — 同步前 stopped-away tracker intent 恢复：非给物图清 intent；给物图则给鞋/清理/失败计数各分支
- `FiveRingTaskV2::tryClickWuhuanTrackerLink / clickPreparedWuhuanTrackerGreen` — 点 tracker：prepared 动作时效校验后点击并注册 UNTARGETED_TRACKER intent+recordMovementIntent；否则消费 Runner negative 映射状态
- `FiveRingTaskV2::acceptInitialDialogAndTriggerPathing` — runExclusive 内点接受选项(2次)，检测'今日次数已完'故事→TASK_ALREADY_FINISHED，否则 verified→NEEDS_SYNC（点击不等于确认）
- `FiveRingTaskV2::resolveFiveRingCompletionStory` — 调 FiveRingCompletionPolicy：先验 finished 模板再验 finished-once，据 configuredRuns/round 出停全部/结束本轮决策

**task/xiuluo (修罗 XiuluoTaskV2 及其状态/DTO/枚举，THIN_CLIENT_V1 A-1**

- `XiuluoTaskV2::guardCloudAcceptNavigationWithStartupReturn` — CR220 接任务导航前返回道具门：仅 hot-start round1 不在起始图时用道具，产出 returnGate 结构化事实
- `XiuluoTaskV2::executeXiuluoBrainCommandedRoundRestart` — 云端 RESTART_ROUND 客户端半：归档失败 case→cleanUpAll→清 dialog/prepared/pathing→同 round 重建 start(round)，stop 恒先赢
- `XiuluoTaskV2::restartXiuluoBrainAfterFailure` — 可恢复失败→同轮 accept 链重启：先归档后清理、handoff 延迟、重采 hot-start facts、initialPhase=PREPARE_ROUND
- `XiuluoTaskV2::consumePostCombatIdleTimeoutBeforePhase` — 消费 POST_COMBAT_IDLE_TIMEOUT（按 seq 去重）→清 dialog/prepared/tracker→recoverTo 重接任务流
- `XiuluoTaskV2::runMaintenanceBroadcastAttempt` — CR245 队长维护：导航点击 NPC→开 FIFO 广播队列→冻结成员 FIFO→即刻让权→park 等成员确认→drain/5s cap 后自确认；re-entry 复用 pending hook
- `XiuluoTaskV2::finishMaintenanceSelfConfirm / finishNoLocalMemberMaintenanceBroadcast` — 队长自确认：新鲜后台探测点一次点击或一次 live rescan；无本地成员则即刻自确认+固定礼貌等待
- `XiuluoTaskV2::waitCombat` — 战斗状态机：EXIT_RECOVERED 分 incidental(回 shortcut)/未进战(unknown-exit)/已进战(RETURN_HOME)；IN_COMBAT 分类 combatSource 并广播队伍战斗相；看打点击未进战的 entry-confirm 窗口(4 tick)与 shortcut 重注册/放弃
- `XiuluoTaskV2::waitTrackerShortcutPathing` — 绿链等待：incidental 战斗→WAIT_COMBAT；依次消费 4 类 typed prepared(本地 kanda prepared/云端看打 job/TRACKER_GREEN_RETRY/summon cleanup)，否则无限 park 等 prepared 或战斗态变化
- `XiuluoTaskV2::clickCloudEnterBattlePoint` — 点击云端看打坐标；点击成功≠进战（保留绿链 schedule 至 IN_COMBAT）；点击失败→awaitCloudFallbackAfterClickFailure
- `XiuluoTaskV2::awaitCloudFallbackAfterClickFailure` — CR232：上报 CLICK_FAILED，仅云端 CLOUD_NO_ACTION(FALLBACK) 才发布 TRACKER_GREEN_RETRY job+PREPARED_ACTION_READY wake（afterSequence 发布前捕获），物理复按不在 wake 链内
- `XiuluoTaskV2::repressSavedGreenOnCloudFallback` — CR232 云端授权复按：仅实际执行的复按计入 shortcutTrackerRetryCount，3 次无进战→放弃 shortcut
- `XiuluoTaskV2::tryTrackerShortcutWithPanel / resolveShortcutTrackerPanel` — tracker 快捷路线判定：读绿链面板、云端 link ranker、注册 pathing intent、开绿链 schedule
- `XiuluoTaskV2::resolveTaskHotStart / resolvePauseResumeTaskHotStart` — hot-start/暂停恢复 phase 解析：战斗中>看打对话>tracker绿字>回程道具>objective记忆>重接
- `XiuluoTaskV2::returnHome / useReturnItemAndVerifyStartMap / tryUseStartupReturnItemOnce` — 返回道具使用+验证回到起始图（RETURN_ITEM_VERIFY_ATTEMPTS=2，500ms 延迟），STILL_IN_COMBAT/VERIFIED/FAILED 分流
- `XiuluoTaskV2::recover*(AcceptNavigation/AcceptNpcClick/AcceptDialog/TargetNavigation/TargetClick/EnterBattleConfirm/ReturnHome)Failure` — 各 phase 失败恢复跳转：cleanUpAll/toggle mount/phaseRetry(1)/recoverTo(recoveryCount 2)/耗尽重开轮
- `XiuluoTaskV2::resolveUnknownCombatExit / attemptVerifiedReturnAfterUnknownCombat / suppressUnknownCombatExitIfActiveCombat` — 非本方看打战斗退出的判定与验证回程（UNKNOWN_COMBAT_TARGET_DISTANCE_TOLERANCE=10）
- `XiuluoTaskV2::navigationOutcome / continueIfNavigationStillPathing` — 导航结果→outcome：PATHING_STARTED/DIALOG/POINT_NOT_REACHED/ARRIVED 到 waitSpec/retry/continue 的映射与观察者确认
- `XiuluoTaskV2::isObjectivePlausibleByCloud` — objective 目标 NPC 合理性云端校验（navigation-migration 并发新增）
- `XiuluoRoundContext::retrySamePhase / recoverTo / recoverToWithObjective / incrementEnterBattleConfirmRetry / incrementShortcutTrackerRetry` — 递增各 retry/recovery 预算计数，驱动 fallback 时机
- `XiuluoRoundContext::withPendingEnterBattleConfirm / withXiuluoBattleStarted / withCombatSource` — 区分看打点击(pending 未进战)与已确认进战，控制 enteredBattleByXiuluo

## 2026-07-12 实施进度：叶子状态类波次 1

- 已按 DHXY `thin-client-design@0114604e` 原字节复制到 Cloud Brain 并通过 fresh package：
  `TaskHotStartSnapshot`、`TaskStartupCheckResult`、`WubeiDialogCatalog`、`WubeiStepOutcome`、
  `WubeiWaitSpec`、`FiveRingPhaseContext`、`FiveRingStepOutcome`、`XiuluoBrainRoundState`、
  `XiuluoDialogCatalog`、`XiuluoRoundContext`、`XiuluoStepOutcome`。
- 本波只建立 dormant Cloud 源码宿主，不删除本地副本、不启动 Task、不改变任何 phase/retry/fallback/wait/状态
  语义。Cloud `mvn -q clean package`：4 suites/21 tests，0 failures/errors/skipped。
- `SystemPowerService` 不在迁云清单：Windows 睡眠是本地机械能力，最终应由本地受控端口执行，而非 Cloud
  进程直接执行。

## 2026-07-12 实施进度：纯配置与图像工具波次 2

- 已按 DHXY `thin-client-design@0114604e` 原字节迁入并保留 7 个 dormant Cloud 依赖：
  `TeleportConfig`、`VisionProvider`、`TeamRoleStatus`、`ImageFinder`、`OpenCvNativeLoader`、
  `ImagePreprocessor`、`LatencyMetrics`；父级复核源目标 length/SHA256 全一致（`BAD=0`）。
- `BotProperties`、`TaskRunProperties`、`TeamTaskProperties` 依赖 Spring Boot
  `@ConfigurationProperties` 与 Jakarta Validation，而当前 Cloud Brain 是轻量 Spring/HttpServer 宿主。本波不为
  三个未激活配置类引入整套 Boot/Validation；其新建目标副本已撤下，延期到 Service host 配置绑定波次统一决定
  Boot binder 或 plain Spring adapter。
- Cloud `mvn -q clean package`：4 suites/21 tests，0 failures/errors/skipped。未注册配置 binder、Task host、
  poller 或执行入口；未删除本地副本，也未改变图像算法、候选顺序或配置含义。
- 当前迁移计数为 407 个 DHXY Java 文件中 Cloud 已存在 143 个、剩余 264 个。下一依赖瓶颈不是叶子 DTO，
  而是 `GameTask`/`TaskStep` 所依赖的 `TaskExecutionContext` 仍绑定本地 `WindowRuntimeContext` 与 pause/stop
  机械权威；必须先建立 Cloud execution-context 兼容边界，禁止直接把本地窗口权威复制进云端。

## 2026-07-12 主体 Service 编译阻塞面扫描

- Cloud 尚未迁入的 20 个顶层 Service 为：`AutoCombatPanelService`、`AutoCombatService`、`BagService`、
  `BattleRadarService`、`ClientIdentityService`、`CommonBoxService`、`DialogService`、`GiveItemService`、
  `LeftTopStatusSwitchService`、`NavigationService`、`NpcClickService`、`PlayerStateService`、
  `QuestManagerService`、`ReturnItemPrescanService`、`SummonSkillService`、`SystemPowerService`、
  `TaskMaintenanceService`、`TaskTrackerPanelService`、`TeamReturnService`、`UICleanerService`。
- 除明确本地保留的 `SystemPowerService` 外，这些类共同受以下机械/宿主边界阻塞，并非继续复制 DTO 即可安全
  编译：
  1. task-run 上下文：`TaskExecutionContext`、`TaskExecutionContextHolder`、`WindowRuntimeContext` 与 holders；
  2. 截图/窗口事实：`GameClientTracker`、native binding refresh、window discovery；
  3. 物理输入：`InputSequences`、`InputProvider`、`InputAction*`、`WindowAwareInputCoordinator`；
  4. 坐标/窗口几何：`CoordinateHelper` 与 window-relative/screen-absolute 转换；
  5. 调试/中间图：`WindowScopedTempPath`，云端不得把本地文件路径当共享权威；
  6. cooperative stop/pause/sleep：`TaskCheckpoint`、`TaskSleep` 与本地 token/线程语义；
  7. 宿主配置/运行态：`BotProperties`、`TeamTaskProperties`、ready-event bus、旧 decision Service beans。
- 实施顺序因此固定为：先形成 per-taskRun Cloud context + retained remote-port 适配层，再提供 capture/input/geometry/
  scoped-storage/checkpoint 机械 facade，随后按 Service 依赖闭包迁移，最后才迁 `GameTask` 主体和激活 host。任何一步
  都不得通过复制本地 `WindowRuntimeContext`、Input queue、HWND discovery 或 pause token 来“让编译先过”。
- 该扫描只整理 imports/ownership，不改业务源码。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：Cloud task 被动 DTO 波次 3/4

- Cloud Brain `com.bot.dhxy.cloud.task` 新增 29 个对 DHXY HEAD 干净的 request/decision/result DTO/enum：
  10 个 request/session/outcome 类型与 19 个 decision 类型。完整文件清单见 `docs/ACTIVE_WORK.md` 顶部对应波次。
- 两个 worker 的写集互斥，目标此前均不存在；父级独立复核源目标 Length/SHA256 `29/29` 一致，`BAD=0`。
  未迁任何 `*DecisionService` bean、HTTP client、endpoint、host、input/window capability 或业务 Task。
- Cloud fresh `mvn -q clean package` exit 0：4 suites/21 tests，0 failures/errors/skipped。字段、Lombok builder/
  default、enum 和 Point/ROI 坐标语义未变；这些类型只作为后续 Service 编译闭包，不构成新业务权威。
- 当前文件级计数：DHXY Java `407`，Cloud 同路径存在 `172`，剩余 `235`。该数包含最终应留本地的窗口、输入、
  UI、driver 等机械文件，不能把“剩余 235”直接理解为全部都应复制上云。
- `XiuluoBrainRoundState::mustReportBeforeLocalYield` — WAIT_COMBAT/WAIT_TEAM_RETURN 共享态 MUST_YIELD 须先上报再让权，防同 phase 内联重试饿死 member 归队流
- `XiuluoBrainRoundState::mayRequestCloudStepAfter` — 判定 outcome 是否允许链式请求下一 cloud step（READY_TO_CONTINUE/TASK_FINISHED 且非 MUST_YIELD/RETRY_LATER/STOP_CHAIN）
- `XiuluoHotStartResolver::resolve` — screenState switch→对应 hot-start phase；allowTaskPanelFallback 仅真实启动时才信任任务面板 objective
- `XiuluoHotStartResolver::resolveStoryDialogHotStart` — 白模板确认修罗剧情 objective 才进 READ_OBJECTIVE，否则回退接任务链

**service/ A–M (THIN_CLIENT_V1 A-1 方法级迁移矩阵底账)**

- `AutoCombatPanelService::resolveRoundsRefreshReason` — static 纯决策：由估算回合/上次刷新/间隔判定 UNKNOWN/LOW_ROUNDS/REFRESH_DUE/null —— 回合刷新时机大脑
- `AutoCombatPanelService::refreshAutoCombatRoundsIfNeeded` — 按 reason 发 Alt+8 无OCR刷新，成功后重置估算为25
- `AutoCombatPanelService::recordCombatExit` — 战斗退出后回合估算 -3（隐式回合消耗模型）
- `AutoCombatPanelService::alignPanelIfNeeded` — 面板中心距目标 >20px 才拖拽对齐
- `AutoCombatPanelService::verifyAndAlignPanel` — ENTRY_MAINTENANCE 只验证不刷新 / VERIFY_AND_REFRESH 验证并刷新两种模式
- `AutoCombatPanelService::ensurePanelVisible` — 找不到面板→Alt+8 打开重试，两次失败记入缺失看门狗
- `AutoCombatPanelService::TeamRefreshDueBurstGuard.reserveIfAllowed` — REFRESH_DUE 按队伍30s公平预留，防突发同刷
- `AutoCombatService::isMemberReadOnlyDegrade` — 队员是否退化只读的判定（暂停/停止/覆盖丢失语义）
- `AutoCombatService::consumeExitAndRecover` — 退战信号消费后的恢复分支：deferLeaderRecovery / 急救队列模式 / 摄妖香 / common-box
- `AutoCombatService::runPendingFollowerFirstAidIfAllowed` — 急救 FIFO 队列头消费 + 本地/队伍能力闸 + 公平 task-turn
- `AutoCombatService::maybeHandleCombatEnter` — 消费enter信号→排4s入场维护+开面板
- `AutoCombatService::consumePendingLeaderPostCombatRecoveryIfAllowed` — 延迟队长恢复消费（快速预期退战后安全点）
- `AutoCombatService::maybeRunCombatMaintenance` — 入场维护/UI清理/回合刷新压力计算
- `BattleRadarService::checkFastExpectedCombatExitByAvatarDiff` — 15s后头像diff快速退战短路
- `BattleRadarService::armExpectedCombatExitWait` — 预期退战武装时间戳=陈旧信号边界，含当轮未消费enter例外
- `BattleRadarService::consumeCombatExitSignalForExpectedWait` — 仅消费武装后产生的当轮退战，陈旧丢弃
- `BattleRadarService::markCombatExitObservedDuringPause` — 暂停只读观察者标记当轮退战为当前有效
- `BattleRadarService::getDynamicPollingIntervalMs` — 按 ActionState 选轮询节奏(战斗4s/导航2s/空闲10s)
- `CommonBoxService::consumePendingBoxIfAllowed` — TTL+多陈旧闸校验后点击并清 pending，失败保留至TTL
- `CommonBoxService::hasPendingBoxForCurrentWindow` — 只读闸：队员安全turn前判定是否有可消费宝箱
- `CommonBoxService::detectBox` — 角色/窗口/run 同步校验后异步入场探测
- `CommonBoxService::isRoleEnabled` — 按 botProperties leader/member 开关闸
- `CommonBoxService::clearPendingForRole` — UI关关/跳过路径清角色 pending 防陈旧延迟点击
- `DialogChoiceMemoryService::findUsable` — 按键查可用记忆（isUsable：未禁用+有成功+失败<3）
- `DialogChoiceMemoryService::findStableTaskChoice` — 任务选项须连续成功streak≥3 才复用，防旧记录反复复用
- `DialogChoiceMemoryService::recordSuccess` — 成功记录并重置失败计数+连续成功++
- `DialogChoiceMemoryService::recordFailure` — 失败++，连败≥3禁用该键
- `DialogChoiceMemoryService::findUsableRoute / recordRouteSuccess` — 路由转移 navigation\|routeTransfer\|from->target 键策略
- `DialogService::handleDialog` — 总控：initialClick→修罗本地模板→分类→STORY/OPTION 按 policy 分支
- `DialogService::tryHandleXiuluoEnterBattleLocalTemplate` — 云端前的本地模板兜底点击（本地决策残留）
- `DialogService::tryHandleCloudPreClickOption` — 委托云端 DIALOG_POLICY 预点击决策
- `DialogService::validatePreparedDialogActionForConsume` — 复用前指纹重校验绑定与相似度
- `DialogService::decideXiuluoEnterBattleStopStatic` — CR232 静态图上云取显式裁决，无本地模板/点击
- `LeftTopStatusSwitchService::handleLeaderStartup` — 队长启动检查并关闭(允许点击)
- `LeftTopStatusSwitchService::probeMemberStartup` — 队员启动无点击探测，OPEN 则标记 pending
- `LeftTopStatusSwitchService::consumeFollowerSafeWindow` — 队伍寻路维护窗内消费/重检 pending
- `LeftTopStatusSwitchService::handleCombatMaintenance` — 战斗中稀疏关闭检查
- `LeftTopStatusSwitchService::resolveState` — 由 open/closed 分判 OPEN/CLOSED/UNKNOWN

**service (N–Z) + service/dialog — THIN_CLIENT_V1 A-1 method-l**

- `NavigationService::observeRoutePlanFacts` — 用本地基线 helper 计算 11 个阶梯观测布尔(纯读，无输入)
- `NpcClickService::pollFreshStoryBlockerEvent` — 非阻塞读最新 STORY_DIALOG_VISIBLE，须 opt-in+同task+序列更新
- `PlayerStateService::ensureSheYaoXiangActive` — quiet-period 本地计时短路云端；否则云端 TICK/capture 决策；USE_INCENSE 本地执行用香
- `PlayerStateService::probeFirstAidSupplyFromBars/inspectSupplyTargetsFromSnapshot/probeFirstAidBar` — 本地像素健康度判定血法条是否需补(filled-ratio 阈值+near-threshold+一致性兜底)=本地补给大脑
- `PlayerStateService::performCachedFirstAidPlanNow/performCachedFirstAidPlanDirect` — 消费预计算计划，exclusive 右键补给；刷新窗口 base 防跨窗坐标
- `PlayerStateService::healAllDirect` — 按 config 启用项逐条截图判定并补给
- `PlayerStateService::syncMyIdentity/syncMyPosition` — 写 GameContext.getMe() 身份与地图坐标(全局记忆)
- `QuestManagerService::activateTaskIfPresent(Direct)` — 找到标签->glow 判定分支(已高亮跳过/否则点击激活)；找标题则点击展开；否则滚动
- `QuestManagerService::captureCurrentQuestDetailForTaskDirect` — 激活->定位 anchor->右侧详情 ROI 截图(供上层/云端解析)
- `QuestManagerService::ensurePanel(Direct)` — 找 anchor 失败则 Alt+Q 开面板并选当前任务页
- `ReturnItemPrescanService::afterTrackerGreen/whilePathing/whileInCombat` — 三种时机入口，按每轮所选 strategy 决定是否真扫
- `ReturnItemPrescanService::chooseStrategy` — 随机公平选择预扫时机(fair 抽签)
- `ReturnItemPrescanService::useCached/hasCached/invalidate` — 缓存点击点复用与失效
- `ReturnItemPrescanService::finishPrescan` — 成功写 cachePoint；失败置 combatFallback
- `SummonSkillService::cleanTailNormalSkillsDirect` — 主状态机：静态扫描定起点->逐槽判级->NORMAL 删除/EMPTY 绝技角标/LOCKED 回扫/KEEP 停
- `SummonSkillService::maybeClickUltimateCorner` — 绝技角标 hover 判 yellow+模板命中->点击生成->再判级删除
- `SummonSkillService::scanLockedBoundary` — 委托 TailBoundaryScanner 做锁定尾边界回扫规则
- `SummonSkillService::handleBusinessDialogDuringSkillClean` — 长独占段内让队长维护广播弹窗优先(复用维护服务轻量匹配)
- `SummonSkillTailBoundaryScanner::scanLockedBoundary` — lockedIndex 向前逐槽：NORMAL删/KEEP停/EMPTY需绝技检查/LOCKED继续/UNKNOWN失败；超时不刷冷却
- `TaskMaintenanceService::runOpportunisticMaintenance/handleMaintenanceBroadcast/maybeCleanSummonSkill` — 机会维护：云端阈值->召唤兽清理冷却/缓存更新决策
- `TaskMaintenanceService::shouldSuppressIdleMaintenanceBroadcast` — 空闲广播抑制缓存(30s TTL)
- `TaskMaintenanceService::isSummonSkillCleanDueForCurrentWindow/updateSummonSkillWindowState` — 召唤兽清理是否到期+尾安全/槽数缓存维护
- `TeamReturnService::clickReturnTeamIfPresent` — 队员：见按钮->ensureSheYaoXiang->复查仍在->点击(队列)
- `TeamReturnService::waitForMembersReturnIfNeeded` — 队长：见信号则等 120s/3s 轮询直到消失(phase 决策)
- `TeamReturnService::probeMemberReturnMarker` — tri-state 归队标记探测(自截图防 miss 误判)
- `TeamReturnService::beginLeaderSignalPrecheck/consumeLeaderSignalPrecheck` — 开背包前异步预分析归队信号,scoped 消费
- `UICleanerService::cleanUpAll` — 地图->forceCloseDialog->关通用窗 组合清理
- `UICleanerService::probeStartupCleanNoInput` — 无输入探测启动是否已干净(供前台跳过重扫)
- `UICleanerService::forceCloseDialog` — story 门控快点 or fallback 末选项决策
- `UICleanerService::cleanLightweightInterruptions` — 轻量：业务选项 or 通用关窗

**vision/ (全部) + model/navigation/ (全部) — THIN_CLIENT_V1 A-1 方**

- `vision/MapSurveyService::projectCurrentPlayerPoint / buildProjectionContext` — 核心本地变换：地图坐标→镜头坐标(边界clamp/插值)→屏幕相对点，可选叠加修正delta，越界则判失败——本地坐标变换大脑
- `vision/MapSurveyService::recordCameraBoundary` — 3s定时后读坐标+鼠标相对位，算 cameraX/Y 落盘边界样本，压 undo
- `vision/MapSurveyService::recordPlayerPointCorrectionByCurrentMap` — 3s定时+等待后重读，算 base 与真实点误差，>500px拒绝，否则落盘修正样本
- `vision/MapSurveyService::recordCenterAnchor` — 3s定时后记录中心锚点(默认512,384)
- `vision/MapSurveyService::undoLastMapSurveyRecordByCurrentMap` — 识别当前地图后弹内存 undo 栈回滚标定
- `vision/MapSurveyService::CameraBounds.correctionAt / localFitCorrection` — 精确修正点优先，否则本地最小二乘拟合预测屏幕点——本地修正兜底
- `vision/LocationVisionService::scanCurrentLocation` — 云端命中即返回，miss 无本地 OCR 回退(CR246/CR257 已删本地 OCR)；决定 no-focus vs legacy focused
- `vision/LocationVisionService::scanByMiniMapTemplate` — 云端结果转 LocationInfo；ocrFallback 分支触发本地纠名+plausibility 复核，决定是否丢弃
- `vision/LocationVisionService::isCloudPlausibleAfterCanonicalize` — 纠名后地图变化时调云端复核，verdict 为空=fail-closed 拒绝并归档
- `vision/LocationVisionService::canonicalizeOcrLocation` — 调 MapNameCanonicalizer 本地纠名(编辑距离修正)——本地名字权威点，须迁云
- `vision/ObjectiveTextRecognitionService::recognize(raw,source)` — 云端 active 则整帧上云返回 FOUND/NO_RESULT；否则回退本地 legacy——cloud/local 切换点
- `vision/ObjectiveTextRecognitionService::selectPlausibleCoordinate` — 坐标经 isCoordinatePlausible 校验，不合理时尝试去首位数字修复(如778,64→78,64)

**model.tasktracker (全部) + model 根目录三文件 (非 navigation) — A-1 方**

- `TaskTrackerPanelNegativeResult::matches()` — 跨任务/跨窗口消费护栏，决定负结果能否被当前上下文采信
- `TaskTrackerPanelNegativeResult::freshWithin()` — TTL 新鲜度判定，决定过期负结果是否失效

**cloud.decision**

- `CloudDecisionCoordinator::shadow(request,localDecision)` — 便捷入口，套用 DEFAULT_EXECUTION_GATE(仅 TASK_CLASSIFIER 可执行)
- `CloudDecisionCoordinator::executePercentHit()` — 确定性本地灰度分桶，percent<=0 关、>=100 全执行
- `CloudDecisionCoordinator::decisionsAgree()/taskPolicyBehaviorFieldsAgree()` — TASK_POLICY 按 result;yield;next 字段级比对，其余服务整串 equals
- `CloudDecisionCoordinator::fallbackMode()` — 服务级 fallback 覆盖全局 defaultFallback
- `CloudDecisionExecutionGate::allowsExecution(serviceId)` — 服务白名单闸，决定该服务能否进 execute
- `CloudDecisionExecutionGate::evaluate(request,response,localDecision)` — cloud->local 安全映射，返回 accepted+effectiveDecision 或 rejected+reason
- `HttpCloudDecisionClient::endpointUri()` — baseUrl+path 拼装，含斜杠归一与默认路径兜底
- `CloudDecisionProperties::service(serviceId)` — 惰性取/建每服务配置，驱动 shadow/execute 门控

**cloud/task**

- `TaskPolicyCloudDecision::cloudExecuted(AppliedOutcome)` — 用云 outcome 替换 phase 结果三元组
- `TaskPolicyCloudDecision::cloudRequiredFailure` — 构造 RETRYABLE_ERROR+MUST_YIELD 终局失败
- `TaskRecoveryCloudDecision::isRecoveryAllowed` — 仅 CLOUD_EXECUTED 才允许恢复动作
- `MaintenanceThresholdCloudDecisionService::decide(context,request,localAction,...)` — shouldRunMaintenance 仅在最终 action=ALLOW 时为真
- `MaintenanceThresholdCloudDecisionService::maintenanceExecutionGate` — 解析云 action 并按本地 ALLOW 下界收敛
- `TaskRecoveryCloudDecisionService::decide(...phaseType,context)` — 云 action 与 next 必须精确匹配本地候选，否则 rejected
- `TaskRecoveryCloudDecisionService::taskRecoveryExecutionGate` — parse+action 匹配+next 枚举匹配
- `MaintenanceThresholdCloudDecision::shouldRunMaintenance` — action==ALLOW 才跑维护
- `MaintenanceThresholdCloudDecision::isRequiredFailure` — 必需失败判定
- `TaskPolicyCloudDecisionService::decide(...currentPhase,runnerResult,localResult,localYield,localNextPhase,phaseType)` — 主入口，产出 cloudExecuted/passthrough/requiredFailure/rejectedLocal
- `TaskPolicyCloudDecisionService::parse` — 解析云 result/yield/next 三枚举，缺一 rejected
- `TrackerLinkRankerCloudShadowService::shadowTrackerLinkSelection(...)` — 打包候选/选中链，接受云点或 no-click
- `NpcClickStrategyCloudDecisionService::authorizeStrategy(request,strategy,verificationMode)` — 恒 no-click，仅记诊断日志
- `SheyaoxiangStatusCloudDecision::shouldUseIncense/shouldCaptureStatus/failClosed` — 按 action 决定本地执行动作
- `SheyaoxiangStatusCloudDecisionService::decide(request)` — 三 hook 主入口，产出 action(CAPTURE/USE_INCENSE/RETRY_LATER/FAIL_CLOSED)
- `SheyaoxiangStatusCloudDecisionService::parse` — USE_INCENSE 必须带 decisionId，否则 rejected
- `TrackerPanelReaderCloudDecisionService::read(request)` — 返回 CLOUD_FOUND(click/links)/NO_ACTION/REQUIRED_FAILURE
- `TrackerPanelReaderCloudDecisionService::parse(taskCode,response)` — 状态机 FOUND/NOT_FOUND/AMBIGUOUS/ERROR + 坐标空间与窗口校验
- `RouteCloudDecisionService::decideRouteCandidate(...)` — 打包路线上下文，返回云点/no-click/passthrough
- `RouteCloudDecisionService::reportRouteMemoryOutcome(report)` — 幂等去重后 POST 结局；LEARN_CANDIDATE 可无 routeDecisionId
- `DialogPolicyCloudDecisionService::decide(request,localResult)` — after-local 钩：SELECT_CANDIDATE 仅能选本地候选，REJECT/必需失败处理
- `NpcClickSmartCloudDecisionService::authorizeDirectCombat` — CR267 仅凭结构化事实授权 Alt+A 直战场景切换(无截图)

---

## 4. 完成路径 / 未尽事项

本 workflow 覆盖 **方法级 inventory**（191 类 / 450 keyMethods / 337 条隐式状态 / tier 标注 / 权威归属）与 **反向扫描候选清单**（§1.2，56 个）。仍需实施期落地的缺口：

1. **反向静态扫描规则的机械化实现**：把 §1.2 候选转成可执行的 grep/ASM 规则（禁本地业务枚举分支、禁本地阈值常量、禁 tier=D 类持有 fallback/memory 语义），对全库跑到**零命中**才算 Q1#5 PASS。
2. **Thin Client 产物 allowlist 构建证据**（Q1#4）：以 `tier=D 且 localRetained=无` 集合为起点，产出打包后应存在的类白名单，并用 local-non-xiuluo-brain profile 先例校验实际产物。
3. **入口可达闭包**（Q1#2）与 **resources 树零未归属**（Q1#3 剩余）：从 Runner/TaskFactory 做可达性闭包、逐一归属模板/JSON/别名字典等资源文件。
4. **人工按业务流反向抽查**（Q1#6）：修罗/五倍/五环/自动战斗四流手工走查，确认无 A/B 决策静默留在本地。

> 在上述缺口闭合前，A-1 提供**完备的方法级底账与反向扫描输入**，但 A Final PASS 仍需反向扫描零命中 + allowlist 证据。

## 2026-07-12 实施进度：Task/Service context + 波次 5/6

- Cloud Task/Service 最小 context 边界已在唯一协作日志获得本地明确 `APPROVED`，`P0/P1/P2=0`：
  exact scope/taskRun/window/stopEpoch/runRevision 来自 coordinator snapshot；effective `taskCode` 精确绑定
  coordinator `taskType`，原始 `requestedTaskCode` 可在成员转 `AUTO_BATTLE` 时不同。未复制
  `WindowRuntimeContext`、`TaskPauseToken`、HWND/geometry/Input queue/runner 权威，机械事实/动作仍只允许
  future retained-authority `RemoteGameClientPort` adapter。
- 修罗 protocol DTO 波次 5 的 6 个 `XiuluoBrain*` request/response/decision 类型已 exact-source copy 并通过
  fresh Cloud package；未复制 `XiuluoBrainCloudDecisionService` 或执行修罗 phase/action。
- 契约波次 6 exact-source copy `GameTask` 与 `TaskStep` 两个无状态接口，只建立后续业务类编译参数边界；
  `TaskFactory`、`TaskStepExecutor`、Task 实现及 checkpoint/sleep/stop 运行语义仍未迁入。
- 最新 fresh Cloud `mvn -q clean package` exit 0：4 suites/21 tests，0 failures/errors/skipped。当前文件级计数为
  DHXY Java `407`、Cloud 同路径存在 `181`、剩余 `226`；剩余集合包含最终明确保留本地的窗口、输入、UI、
  driver 等能力，不作为机械复制待办。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：retained action + typed Service port 设计返修

- Design #1 的 opaque handle、single-ledger、exact-context 与 fact/capture/atomic-input 类型化边界方向成立，但
  父级源码核对发现一项 P1：broker late final outcome 仅写 `lateResolution`，重复同字节请求仍返回已完成的
  `UNKNOWN`；action ledger 同时禁止 `UNKNOWN -> 最终态`。因此该设计暂未批准实施。
- 同一 worker 正按固定日志返修：broker 必须在相同 requestId+digest 重入时返回已存 late resolution（无自动
  redispatch），ledger 只允许 `unrecorded -> UNKNOWN -> one exact final state`，且 renewal 仍只接受 executor
  验证记录的 `NOT_EXECUTED`。host/poller/UI/capture/input 与业务 Service/Task 继续 dormant。
  **无已批准业务差异；按基线等价迁移。**

- **Design Repair #1 已获父级 `DESIGN APPROVED`，P0/P1/P2=0。** 实现范围固定为结构化 retained action state、
  operation-specific opaque handle、唯一 public typed Service port，以及 broker/ledger 的 late-resolution 收敛；
  不迁任何业务 Service/Task，不触碰 DHXY Java。fresh Cloud clean package 与源码可达性/可见性证据完成前仍不算
  实现通过。**无已批准业务差异；按基线等价迁移。**
- **Implementation #1 已获父级 `APPROVED`，P0/P1/P2=0。** retained action state、operation-specific opaque
  handles、typed `WINDOW_FACT`/`CAPTURE`/atomic-input facade、broker late-resolution 读取与 ledger
  `UNKNOWN -> one exact final state` 已落地；raw authority 保持包内且当前无 host/runtime caller。父级 fresh Cloud
  clean package 为 4 suites/21 tests 全绿，`src/test` 与 DHXY Java 未触碰。下一关键路径是 persisted action catalog/
  pause-resume rehydration 规则与不依赖本地窗口/输入权威的 Service 叶子。当前同路径计数仍为 `181/407`，因为本切片
  新类位于 Cloud authority 包而非 DHXY 同路径复制集合。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：Cloud checkpoint + pause/resume rehydration 设计切片

- 已批准 context/retained-action/image processor 后，主体 Task/Service 的下一兼容缺口是 checkpoint/sleep 与
  pause/resume rehydration。本地 `TaskPauseToken`、ThreadLocal holder、`WindowRuntimeContext` reconciler 继续是
  本地机械权威，不能原样搬云；Cloud 旧 revision context/handle 永不复活。
- 唯一日志固定为 `docs/superpowers/plans/reports/2026-07-12-cloud-checkpoint-rehydration.md`，由同一外部 worker
  先设计 typed checkpoint outcome、持久 phase/action catalog、stable-ID/UNKNOWN/final 原子性与本地 reconcile
  fact handoff。父级 `DESIGN APPROVED` 前不改 Java，host/Task 继续 dormant。
  **无已批准业务差异；按基线等价迁移。**
- **Design #1 父级审查 `BLOCKED`，P0=0/P1=3/P2=0。** 当前无 durable coordinator/broker/ledger backend；本切片
  收窄为 same-process typed checkpoint classifier + explicit-context utility，不实现/宣称 crash rehydration，不改
  action ledger/broker/ID。resume reconcile fact 与 confirmation 的原子跨仓合同、durable catalog/WAL、mid-sleep crash
  continuation 分别作为后续独立 gate；host/cohort 继续 dormant。
- **Design Repair #1 已获父级 `DESIGN APPROVED`，P0/P1/P2=0。** 只实现 same-process typed classifier 与
  explicit-context checkpoint/sleep；durable/action/wire/DHXY 继续零改。父级强制 Cloud 不提供 pause boolean，stop
  boolean 对除 CURRENT/STOPPED 外的全部 outcome typed unwind；PAUSED 只 park，newer-unconfirmed 为 DENIED，避免旧
  stack 静默推进。host/Task/Service cohort 仍 dormant。
- **Implementation #1 已获父级 `APPROVED`，P0/P1/P2=0。** coordinator structured classifier、Cloud
  `TaskExecutionContext` 的 fail-closed stop API、explicit-context `TaskCheckpoint` 与单次 interruptible `TaskSleep`
  已落地；public API 无 pause boolean/raw sleep/holder/token overload，classifier 无 map mutation 或文本 reason 解析。
  父级 fresh Cloud clean package 为 4 suites/21 tests 全绿。此结论只关闭 same-process compatibility；durable crash
  rehydration、resume reconcile-confirm 原子跨仓合同与 host transition 仍是后续 gate，业务 cohort 继续 dormant。
  当前同路径计数仍为 `181/407`，因为本切片新增的是 Cloud runtime 兼容/权威类型。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：并行叶子波次 7 与 artifact/template adapter

- `TaskStepExecutor` 已 exact-source copy 至 Cloud，源目标 3437 bytes/SHA256/逐字节一致；父级 fresh Cloud package
  21/21 全绿，`P0/P1/P2=0，APPROVED`。retry/canRetry/sleep/异常映射保持基线，类继续 dormant。
- `TeamTaskProperties` + `TaskTeamAssignmentPolicy` exact-copy 因 Cloud 不含 Spring Boot
  `ConfigurationProperties` 编译失败，新增目标已由原 Worker 完整回滚，不计入迁移；后续改走不引 Boot 的
  Cloud-native configless policy adaptation。
- artifact/template adapter Design #1 方向成立但父级 `BLOCKED，P0=0/P1=3/P2=0`：需补 exact current context+
  revision 的 write/read/delete 门、root-wide 磁盘与编码并发预算，以及可实现的 stateRoot/scopeRoot real-path 锚点。
  caller/cohort 继续 dormant。Cloud 同路径计数由 `181/407` 增至 `182/407`。
- **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：Cloud resume reconcile-confirm 原子合同设计切片

- same-process typed checkpoint/sleep 已父级 `APPROVED`，但 resume 后新 context 仍必须等 DHXY exact local
  registration/reconcile fact 与 Cloud current ACTIVE revision execution confirmation 原子成功后才能重建。
- 唯一追加式日志固定为
  `docs/superpowers/plans/reports/2026-07-12-cloud-resume-reconcile-confirm.md`；继续使用同一外部 worker，只先设计
  双侧 typed fact/schema/digest、原子 validate+record、幂等/乱序/断线/租户/容量/运维矩阵及最小写集。父级
  `DESIGN APPROVED` 前不改 Java/Maven/resources/tests，不启动 host/Task/Service cohort。
- 当前同路径计数仍为 `181/407`；该设计门关闭前，依赖 pause/resume rehydration 的业务 cohort 不进入 active set。
  **无已批准业务差异；按基线等价迁移。**
- 同步叶子重扫未找到新的安全 exact-copy Service：`GiveItemService` 已依赖本地 input/raw sleep，
  `LeftTopStatusSwitchService`/`LocationVisionService` 依赖 tracker、runtime holder 或本地 Path，其余缺失 Service 闭包
更大。因此维持 `181/407`，不以复制本地机械依赖换取表面计数；先完成 reconcile-confirm 与 typed adapter gate。

## 2026-07-12 实施进度：resume-confirm 与 configless assignment policy 收口

- resume executor-readiness Repair #1 已经父级源码复审和 fresh 双构建 `APPROVED，P0/P1/P2=0`。完整 request digest
  权威已进入 Cloud coordinator 原子写门；DHXY receipt taskRunId CAS、ledger terminal/revision 原子发布与 readiness 固定
  10 秒 timeout 均闭环。该批准不激活 host/cohort。
- `TaskTeamAssignmentPolicy` 已以 configless Cloud adaptation 落地：仅删除源中从未参与任何方法决策的
  `TeamTaskProperties` 字段/构造依赖，四个方法的条件顺序、fallback、日志与返回值保持 DHXY HEAD 等价。父级 fresh
  Cloud `mvn -q clean package` 4 suites / 21 tests 全绿。
- Cloud 同路径计数由 `182/407` 增至 `183/407`；`TaskStartupCheckService` 的配置门与本地 role 采集仍未迁移，不能由
  本 policy 的存在推断 startup cohort 可激活。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：Cloud-safe BaseTaskTemplate 设计批准

- Worker F Design #1 经父级 `DESIGN APPROVED，P0/P1/P2=0`：后续同步 Task 继续使用显式
  `execute(TaskExecutionContext)`、源 `before -> checkpoint -> steps -> after` 顺序、原 retry 次数/delay 与 GameContext
  结果落态；PAUSED/stale/completed/denied 只做 typed unwind，不降级为 `FAILED` 或 retry。
- 实现写集固定为 Cloud `BaseTaskTemplate` 1 new + `TaskStepExecutor` 1 modify。禁止复制
  `WindowRuntimeContext`/holder/`TaskWindowRuntimeService` 或 standalone focus；无参/null context typed fail-closed。
- 父级明确禁止在源 `beforeTask` 前新增 checkpoint，并保留 `stop()` 的源 GameContext cleanup 语义；实际 step 的
  public executor 路径在既有 checkpoint 位置强制 explicit context。host/concrete Task 继续 dormant，当前计数仍为
  `183/407`，待实现与 fresh package 通过后再计。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：artifact/template Design Repair #1 仍 BLOCKED

- typed `CloudTaskServiceExecutionContext` + current ACTIVE + tenant/user + taskRunId/runRevision owner 门与可信 root 写集已通过。
- 容量仍有两个 P1：bytes/count 双 CAS、per-scope FIFO 与 global admission 锁序无法原子化；重启扫描会把未注册 scope
  artifact 计入 totals，却没有 store callback 可回收，导致 global budget 可永久锁死。
- Worker B 只需 Repair #2 提交 budgetLock 下 tuple reservation handle、exact-once rollback、startup orphan 有界可信
  reclaim index，并收窄 reparse 威胁声明/可见性。Java 与三个 caller cohort 继续冻结，计数保持 `183/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：AutoCombatService 整类迁云设计启动

- 外部 Worker A 前一 resume-confirm 实现已收口，下一复杂切片固定日志
  `2026-07-12-cloud-auto-combat-service-worker-a.md`，首轮只做 `AutoCombatService` HEAD caller/方法/机械依赖完整设计。
- 迁移必须保持整类 public API 与全部业务条件、状态、日志、sleep/retry/fallback/stop/输入顺序；截图/窗口事实/输入 bundle
  只走 existing retained typed Service port，禁止复制 runtime holder/tracker/input queue/TaskTurnCoordinator 本地权威。
- 该设计与 artifact/Base/startup role gate 并行且零写集交叉；批准前 Java 与 host/cohort 冻结，计数保持 `183/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：BaseTaskTemplate APPROVED / per-run GameContext owner 启动

- Cloud `BaseTaskTemplate` 1 new + `TaskStepExecutor` typed adaptation 已父级源码审查和 fresh package
  `APPROVED，P0/P1/P2=0`。源 before/checkpoint/steps/after、retry/delay/log/result 与 GameContext cleanup 保持，
  pause/stale/completed/denied typed unwind；host/concrete Task 仍 dormant。
- Cloud 同路径计数 `183/407 -> 184/407`。下一内部切片 H 只设计 exact per-run `GameContext.State` owner：pause/resume
  保留同 run 状态、旧 revision 禁止 bind、terminal 释放、restart fail-closed，不冒充 durable rehydration。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：artifact/template Design Repair #2 仍 BLOCKED

- 已通过：exact typed context owner/revision、package-private governor、单 budgetLock tuple authority、统一 scope monitor、
  orphan 直接 reclaim、受信 stateRoot threat boundary。
- 剩余 P1=2：victim 物理删除前已扣账会把未释放空间重复授予；startup scan 达上限即停会留下未计账/未索引文件。
- Repair #3 只重写 `EVICTING 仍计账 -> delete -> settle -> reserve` 与 saturated scan 的自动批次回收或运维
  fail-closed。Java/caller cohort 继续冻结，计数保持 `184/407`。**无已批准业务差异；按基线等价迁移。**
- 当前事实链已核：DHXY `confirmExecution(...)` API client 存在但 main 零调用；resume 在 Cloud 新 ACTIVE 后只做本地
  registry 发布/唤醒 pause token，Cloud coordinator 仍仅保存内存 confirmed revision，无 reconcile fact/digest。
  因此所有 cohort 继续 dormant，Design #1 必须处理 local-publish 后网络失败的相同请求幂等收敛。
- **Design #1 父级审查 `BLOCKED`，P0=0/P1=2/P2=1。** 本地 reconciler 的 continue/hot-start、phase/action/dialog/
  pathing fingerprint 不能成为 Cloud execution-confirmation 业务权威，且其仅有 caller 是未来会迁走的本地业务 Task；
  该设计既双脑又不可达。返修必须只传 exact executor readiness（binding/revision/local registration generation/
  operation drain），由 registry/ledger/现有 transport 机械链产生，并明确 one-per-run stable request owner/清理。
  Cloud business rehydration 继续后置，host/cohort 仍 dormant。
- **Design Repair #1 父级审查仍 `BLOCKED`，P0=0/P1=2/P2=1。** 业务双脑已移除，但机械 producer 尚未闭合：
  registry/ledger/poller 当前没有 Repair 所假设的锁与 collaborator，confirm timeout 还会落入 poller 外层 catch 并停机；
  新 DHXY action 又缺 action/request/response/digest 必改文件，Cloud 表仍矛盾保留已删除的 decision/mismatch。另需以
  generation/request handle CAS 丢弃迟到 receipt，并将 exact-session readiness send 限为 bounded batch，防止阻塞正常
  command poll。Design Repair #2 前 Java/host/cohort 继续冻结，同路径计数维持 `181/407`。
- **并发完成后的完整 Repair #1 复审为 `BLOCKED`，P0=0/P1=0/P2=1。** 后续追加内容已关闭上一条中间态 P1，
  producer/wire/poller/bounded-send 现在可实现；旧 Review #2 标记 superseded。唯一剩余门是锁外 confirm 的迟到
  success/reject 必须凭 exact entry/slot generation + request/toRevision handle CAS 回写，stale handle 只记审计、不得清除
  新 resume slot。Repair #2 只补此项，Java/host/cohort 继续冻结，同路径计数仍为 `181/407`。
- **Design Repair #2 已获父级 `DESIGN APPROVED`，P0/P1/P2=0。** exact send handle/CAS、stale-result 丢弃、
  exact-session 每边界最多一条和 bounded timeout 已闭合。父级绑定 factDigest/requestDigest 两步无环公式、双仓 typed
  error-code 文件和可编译的 `RemoteTaskRunClientException`/`RuntimeException` 分层 catch，并要求 ledger 统计 exact
  identity 下所有旧 revision。现由同一 worker 实施 Cloud 4 new+9 modify、DHXY 3 new+10 modify；host/cohort/business
  rehydration 仍 dormant/后置，同路径计数暂维持 `181/407`。
- **Implementation #1 父级审查 `BLOCKED`，P0=0/P1=1/P2=3。** 双构建门已通过，但 coordinator 原子写门未自行
  重算 outer request digest，registry receipt CAS 漏验 taskRunId，ledger terminal publication 与 revision 非原子，且
  readiness HTTP 仍复用任意 lifecycle timeout 而非批准的独立固定 10s 上限。Worker A 只按固定日志四项返修；完成前
  resume confirmation 与依赖它的 business rehydration/cohort 继续 dormant，同路径计数维持 `181/407`。

## 2026-07-12 实施进度：Cloud-native ImageProcessorService 设计切片

- 叶子依赖审查确认 DHXY `CloudImageProcessor` 不能 exact-source copy：它是
  `ImagePreprocessWashedImageClient -> ImagePreprocessCloudService` 的本地到云端 transport wrapper，并读取
  `TaskExecutionContextHolder`、本地 HWND/geometry/Path 元数据。搬入 Cloud 会形成 self-HTTP 与错误的本地运行时权威。
- Cloud 已有 `ImageProcessorService` 契约以及唯一算法属主 `ImageAlgorithms`。下一切片使用唯一追加式日志
  `docs/superpowers/plans/reports/2026-07-12-cloud-native-image-processor-service.md`，由一个外部 worker 先设计
  in-process 实现；父级批准前不改 Java。设计必须覆盖全部 operation/result、路径所有权、坐标/排序/失败等价、
  并发/内存/tenant 风险和 dormant Spring reachability。**无已批准业务差异；按基线等价迁移。**
- **Design #1 父级审查 `BLOCKED`，P0=0/P1=1/P2=0。** 原方案会把既有
  `addImageDiagnostics`/algorithm output 全面重构为 typed records，超过本切片必要范围并扩大 HTTP 路径回归面。
  返修必须保留现有 helper 主体，仅新增最小 canonical result 分派；父级已批准 Cloud `washToPath` 固定 fail-closed、
  三个 Path caller 在 artifact/template adapter 前不得激活，并要求 in-process `decision=null` 不伪造 transport provenance。
- **Design Repair #1 已获父级 `DESIGN APPROVED`，P0/P1/P2=0。** 现有 `addImageDiagnostics`、`wash`、全部
  low-level helper 主体保持原样；只新增最小 canonical process result 供 HTTP 与 in-process 共用。实现写集限定为
  新 `CloudNativeImageProcessor` 及 `ImageAlgorithms`、`DecisionEngine.imagePreprocess`、
  `CloudServiceConfiguration` 三处定点修改，host/runtime 继续 dormant。
- **Implementation #1 已获父级 `APPROVED`，P0/P1/P2=0。** Cloud-native in-process
  `ImageProcessorService` 已使用唯一 `ImageAlgorithms.process`，原 HTTP path 与新 typed projection 共享 canonical
  diagnostics 且无 self-HTTP。父级 fresh Cloud package 为 4 suites/21 tests 全绿；processor 无 filesystem/
  local-runtime/input/capture 权威，host 继续 dormant。三个 `washToPath` caller 在 tenant artifact/template adapter
  完成前不得进入 active cohort。当前同路径计数仍为 `181/407`，因为新实现位于 Cloud runtime package。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：Cloud startup role gate 设计批准

- G Design #1 经父级 `DESIGN APPROVED，P0/P1/P2=0`，实现写集固定 Cloud `TaskStartupCheckService` + package-private
  `CloudStartupGateAuthority` 两个新文件；本地 hover/panel/OCR/input detector 不迁。
- 五环/自动战斗全部 gate-disabled/enabled/role/UNKNOWN/SOLO 真值表与 reason/result 保持；policy 只在明确
  `NO_OVERRIDE` 后 seed，role fact 只从 exact context 投影，每次 check 单次 typed checkpoint 后 exact compare。
- host/producer/concrete Task 继续 dormant，当前计数仍 `184/407`，待实现与 fresh package 通过再计。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：AutoCombatService Design #1 BLOCKED

- 方法/caller/间接机械调用盘点通过，但设计错误假设 Cloud 有 `getStopToken`；当前 PAUSED context 会 typed unwind，无法保留
  HEAD `probePausedWindowCombatStateReadOnly` 的 stop-only observer 语义。
- 另有 9 个 public 方法被改签名，以及 7 collaborator/turn/config 未形成可编译 closure。Repair #1 必须补 typed PAUSED
  read-only observer 前置、per-run context-bound Service 保留 API、确定 dependency DAG 与 monotonic timer 映射。
- Java/host/cohort 继续冻结，计数保持 `184/407`。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：重启 cold review 与并行恢复

- heartbeat 和两仓落盘状态完整，旧内部 G/H 进程会话丢失但文件未丢。G 的两个 startup 文件已落盘，父级实现审查仅发现
  `parseRole.trim()` 一项 P1；新 G2 按原始标签 exact case-insensitive 返修，fresh package 前不计数。
- H same-process per-run `GameContext.State` owner 获父级 Design APPROVED；不在 PREPARED reservation/建 State，只在
  current confirmed ACTIVE 后原子 admission + newState，pause/resume 同对象、terminal exact release、restart fail-closed。
  新 H2 实施 Cloud 1 new + assembly 1 modify。
- A AutoCombat Repair #1 仍有 P1=2：current revision context 与 same-run runtime state 保留未闭合，run-terminal verify-gate
  cleanup 改变 30 秒跨 run 基线；等待 Repair #2，Java 冻结。B artifact Repair #3 已 Design APPROVED，等待 5-new+2-modify
  实施，绑定 counter overflow/no-progress fail-closed。
- 当前已批准同路径计数仍为 `184/407`；只有 G2 源码复审 + fresh Cloud package 通过后才可增至 `185/407`。H/B 为 Cloud
  runtime 新边界，不以同路径文件数虚增。所有 host/concrete Task/caller cohort 继续 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：AutoCombat Repair #2 设计批准 / G2 返修待构建

- A Repair #2 经父级 `DESIGN APPROVED，P0/P1/P2=0`：per-taskRun Service/runtime state 保留，current-context slot 只安装
  full-key exact current confirmed ACTIVE revision；PAUSED/stale 仅 typed unwind，不 close，只有 exact terminal 关闭。
  `RefreshDuePanelVerifyGate` 维持 tenant host 生命周期和 HEAD 跨 run 30 秒语义。
- AutoCombat 仍不得实施：下一前置是 W0 PAUSED read-only observer 的双仓 typed authorization 设计，随后 turn/config/H owner
  与七 collaborator final contract 逐波闭合。生产不形成长期双权威。
- G2 已修复 startup role parser 的 whitespace 放宽；源码条件已满足，等待 H2 并行写入稳定后统一 fresh Cloud package。
  当前计数仍 `184/407`，package + 父级最终实现审查通过后才计 `185/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：startup gate APPROVED / State owner 最小返修

- G2 startup role parser 已恢复原始标签边界，父级 source review + fresh Cloud clean package 通过：4 suites/21 tests，
  0 failures/errors/skipped；同路径计数 `184/407 -> 185/407`。该类仍无 role/config producer/host/caller。
- H2 per-run `GameContext.State` owner 可编译且主要设计成立，但父级 `BLOCKED，P0=0/P1=0/P2=2`：顶层 projection 前
  重复 coordinator reads，terminal release 成功后 exact retry 不幂等。H3 只改 owner 文件，收敛为最终一次 typed gate和
  `ALREADY_RELEASED`；assembly/其它边界冻结。
- 空出的第二内部槽进入 AutoCombat W0 configless `CloudAutoBattleProperties` Design #1；只设计 authenticated tenant/user
  immutable snapshot 与单 getter，父级批准前不改 Java。H runtime owner 和该新 config 边界不计同路径文件数。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：State owner 收口 / AutoCombat W0 四路推进

- H3 已关闭上一轮两个 P2：initial/resume/projection 各仅一次最终 typed current gate，terminal exact release 的成功重试
  稳定 `ALREADY_RELEASED`。父级 fresh Cloud clean package 最终 4 suites/21 tests 全绿，Implementation Review #2
  `APPROVED，P0/P1/P2=0`；该 Cloud runtime owner 不增加同路径计数，仍为 `185/407`。
- A 的 PAUSED read-only observer Design #1 因复用 confirmed-ACTIVE context、稳定 observation identity/禁止 renewal 未落入
  authority、DHXY raw String 与显式 null canonical 缺口而 `BLOCKED，P0=0/P1=2/P2=2`；W0 Repair #1 前双仓 Java冻结。
- B artifact 5-new+2-modify 已落盘且同一 fresh package 可编译，但源码审查
  `BLOCKED，P0=0/P1=5/P2=2`：编码 permit 在编码后、business delete 与 eviction 可重复结算 ghost、governor map 缺 scope、
  startup budgetLock 内 I/O、失败写 cleanup 可留下未计账文件；另需 deterministic tie 与 canonical template id。原 B 返修。
- I 的 configless `CloudAutoBattleProperties` 2-new 已父级 `APPROVED，P0/P1/P2=0`，fresh package 21/21 全绿；signed
  `120000ms`/override、exact scope 与 immutable CAS 保持，无 producer/bean/caller，Cloud-specific 文件不增加同路径计数。
- I 关闭后的槽立即续派 K，只设计 non-mintable `CloudTaskRunCurrentContextSlot` 的 full stable key、每次 typed current gate、
  monotonic revision install 与 exact terminal close；J 继续并行设计 `CloudTaskTurnCoordination`。二者是 AutoCombat W0 前置，
  host/Task/caller 保持 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：A/K 设计返修与 J/L 并行续接

- A 的 PAUSED observer Repair #1 关闭独立 capability 和 authority-owned no-renewal 方向，但仍
  `BLOCKED，P0=0/P1=1/P2=2`：ledger identity 的 ACTIVE/OBSERVATION typed owner/key 模式闭包不可编译，DHXY unknown/null
  实际落 `DESERIALIZATION` 而非报告声称的 schema code，且协议 schema 漏出写集。A 继续 Repair #2，Java冻结。
- J 的 `CloudTaskTurnCoordination` 设计获父级 `DESIGN APPROVED，P0/P1/P2=0`；FIFO/exact handle/reentry/forceRelease
  合同成立，但两文件实施硬依赖 K slot Implementation APPROVED，故 J 暂关等待，不造 surrogate。
- K 的 current slot 设计为 `BLOCKED，P0=0/P1=1/P2=1`：resume 不能 new 第二份 per-run retained action state；Repair #1
  必须让同一 state 跨 revision 复用，并让新 context/port 与 matching retained state 作为一个 slot generation 原子安装。
- 新 Worker L 已启动 `BattleRadarService` 整类迁云设计，先闭合 AutoCombat W1 的 capture/template/timer/state collaborator，
  与 A/B/K/J 写集零交叉。B 尚无新返修材料，不重复写 verdict。当前计数 `185/407`，全部 caller/host dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：PAUSED observer Repair #2 设计批准

- A 已将 PAUSED observer identity 与 ACTIVE retained identity 完全分型，ACTIVE path 零改；observation records 独立、
  capability reference/operation/paused revision exact，同 taskRun/business key 跨 mode fail-closed，combined retained quota 无
  eviction。DHXY canonical 分类固定 unknown/null=`DESERIALIZATION`、operation mismatch=`SCHEMA_MISMATCH`，协议 schema 入写集。
- 父级 `DESIGN APPROVED，P0/P1/P2=0`，放行 A 实施 Cloud 2-new+6-modify、DHXY 1-new+4-Java-modify+schema doc，
  之后跑 Cloud clean package 与 DHXY compile。K/L 继续并行，J 等 K 实现批准，B 等原返修。计数仍 `185/407`，host/caller
  dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：artifact 与 current-slot 新一轮父级 BLOCKED

- B 的七项原返修多数成立，但 Implementation Repair #1 仍 `BLOCKED，P0=0/P1=2/P2=2`：new full key 未查
  byKey/evicting/pending collision，写失败 cleanup 可删除旧 target；startup tmp 清理又越权匹配共享 scope 中所有
  `*.png.tmp`。Repair #2 还需清失败 owner ledger，并恢复批准的 CJK template id 字符范围。
- K 的 retained-state 跨 revision 方向成立，但 Repair #1 仍 `BLOCKED，P0=0/P1=2/P2=1`：K/A 都改 action ledger，必须
  在 A Implementation APPROVED 后顺序合并；resume 不得接受新 business metadata，broker miss renewal 只允许可证明同
  authority 尚未入 broker。A/L 继续，J 暂关。计数 `185/407`，host/caller dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：BattleRadar L1 设计批准 / L2 BLOCKED

- `BattleRadarService` HEAD 14 个 public API、四 stage ROI/模板/阈值/顺序、连续 miss=2、15s/1s/4s、20x20/0.35、
  enter/exit signal 与 epoch-ms 同毫秒规则已形成完整迁移矩阵。
- L1 三个 Cloud-only 叶子获父级 `DESIGN APPROVED，P0/P1/P2=0`：signed team hover 配置 authority（baseline
  `644/91`）和 canonical in-process minimap coordinate readability seam；零 host/producer/caller，不增加同路径计数。
- L2 主体 `BLOCKED，P0=0/P1=3/P2=0`：unresolved capture 必须 typed unwind 而非 remembered boolean；长期观察须先
  落双仓 final-consumer ack + monotonic compaction frontier；Stage 2/3 capture failure 的 stale-temp 行为差异未经批准。
  `BattleRadarService`/capture port/factory/assembly/caller 继续冻结，当前计数仍 `185/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：artifact/L1 APPROVED，observer/current-slot 单 P1

- artifact/template Repair #2 父级 `APPROVED，P0/P1/P2=0`：full-key collision reservation、owned cleanup、canonical
  adapter tmp、owner publication 与 CJK template allowlist 收口。BattleRadar L1 三个 Cloud-only 叶子同时获
  `APPROVED，P0/P1/P2=0`。fresh Cloud package 21/21 全绿；二者均 dormant，不增加 `185/407` 计数。
- PAUSED observer 实现仍 `BLOCKED，P1=1`：观察命令不能继承 normal ACTIVE 的 pause-progress deadline extension，
  否则暂停不结束时机械 timeout 永不发生。current-slot Repair #2 仍 `BLOCKED，P1=1`：H State handle/activation 与 slot
  runtime publish 必须同一不可裂 generation transition。
- 新内部 Worker M 只设计 broker/action/local 三账本 final-consumed ack、单调 occurrence/frontier 与 bounded compaction；
  这是 BattleRadar L2 长期 capture 的 R0 前置。当前计数仍 `185/407`，host/caller 全冻结。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：PAUSED observer Implementation Repair APPROVED

- A 已定点关闭最后一个 P1：带 `observationMode` 的 dispatched request 保持 dispatch 时建立的 wall-clock operation
  deadline，`refreshPauseDeadlineLocked` 不再累计 PAUSED progress，deadline 到期直接进入既有
  `UNKNOWN/TIMEOUT` 终态；普通 ACTIVE request 的 pause-freeze 路径未改。
- 父级 source review 与 fresh Cloud `mvn -q clean package` 通过：4 suites/21 tests，0 failures/errors/skipped。
  Repair 未触碰 DHXY Java、coordinator、ledger、schema、caller/host；W0 observer 切片收口但保持 dormant。
- 当前同路径计数仍为 `185/407`。下一并行门保持 K current-slot Repair #3、M R0 compaction 设计与外部 B
  `AutoCombatPanelService` Design #1。**无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：current-slot Repair #3 DESIGN APPROVED

- K 已把 exact H `StateActivationHandle` 纳入 full runtime，并用 slot-owned transition lock/opaque generation handle 在 H 前
  锁定 expected generation、完成 context/port/runtime 全量预分配；H 成功后只做 direct handle attach + unconditional volatile
  publish，不再存在“先 H 后普通 CAS”或补偿窗口。
- 父级对照 `CloudGameContextStateOwner.activateResumed` 确认异常、interrupt、typed gate、overflow 和 handle construction
  全部发生在 owner assignment 前；H 无需改动。A observer 已获最终 Implementation APPROVED，K 的 A-first 实施门满足。
- 现放行同一 K 按 1 New + 7 Modify 实施；必须冻结 A Observation/no-renewal/combined quota/mode conflict、H owner、
  DHXY 与 host/caller。实施后再做父级源码审查和 fresh Cloud clean package。计数仍 `185/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：TaskMaintenanceService 外部 A 设计启动

- observer 切片收口后的外部 A 已续派 HEAD `TaskMaintenanceService` 3136 行整类迁云 Design #1；该 Service 同时拥有
  per-window/per-team queue、first-aid/broadcast/summon-skill 状态、TTL/cooldown、soft wake 与机械 dialog/input 委托，是
  AutoCombat/五倍/修罗共同前置。
- A 先交完整 API/caller/state authority/mechanical typed-port/tenant-capacity/restart/compatibility 矩阵；不得复制本地
  WindowRuntimeContext/InputSequences/ReadyEventBus/shadow transport，不得把 UNKNOWN 或 ready-event negative 变业务真值。
  父级 DESIGN APPROVED 前零 Java，写集与 B/K/M 隔离。当前计数仍 `185/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-12 实施进度：TaskMaintenance / R0 compaction 首轮设计返修

- `TaskMaintenanceService` Design #1 为 `BLOCKED，P0=0/P1=6/P2=1`：必须补 retained action handle owner、exact
  taskRun/revision 状态入口、两次 fresh ROI capture 与绝对坐标合同、五项 maintenance config authority、基线 wall-clock、
  typed soft-wake subscriber route 和 `SummonSkillService` 前置，才可形成可编译 W-TMS-1/W-TMS-2 闭包。
- R0 final-consumed/compaction Design #1 为 `BLOCKED，P0=0/P1=4/P2=2`：必须补 business consume transaction、
  PAUSED observation occurrence authority、bounded control lane/outbox send state，以及 semantic address 的 accepted/late/
  compacted 全路径完整性。M 继续 Design Repair #1，实施顺序仍为 `A -> K -> M -> L2`。
- 两条线都只有文档设计，无 Java 写入；K current-slot Repair #3 继续独立实施，B panel 尚无新材料。计数保持
  `185/407`，全部 host/Task/caller dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：maintenance 配置/模板与 current-slot APPROVED

- A `W-TMS-0A` 已交付并获父级 `APPROVED，P0/P1/P2=0`：Cloud 新增五项 maintenance config interface/authority，
  exact-copy 两张维护模板；未接 assembly/host/caller。整类 TMS 仍按 `P1=5/P2=1` 做 Design Repair #2 Delta。
- K current-context slot Implementation Repair #1 已获父级 `APPROVED，P0/P1/P2=0`：terminal close revision 从“任意更大”
  收紧为当前 ACTIVE context 的 exact next revision。父级合并态 fresh Cloud clean package 为 4 suites/21 tests 全绿。
- K 已关闭，内部槽续派 J2 `019f59bf-10a8-74c1-9517-58c49e174720`，只实施已批准的
  `CloudTaskTurnCoordination` 与 `CloudTaskTurnAuthority` 两个 new-only Cloud 文件；与 M 写集不重叠。
- B `AutoCombatPanelService` Design #1 为 `BLOCKED，P0=0/P1=5/P2=1`，但已放行纯决策叶子 `W-ACP-0` 直接编码；
  主体必须先闭合 retained handle、UNRESOLVED、真实 GEOMETRY/screen-absolute、DPI scale、H projection 与 warning sink。
- M R0 Repair #1 仍 `BLOCKED，P1=3`，下一轮只补 control lane permit/admission 与 PAUSED canonical slot factory delta。
  同路径计数保持 `185/407`；运行面全部 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：W-ACP-0 APPROVED / M0 DTO 放行 / 重启后双槽恢复

- B `W-ACP-0` 的 `AutoCombatPanelDecision` 已父级源码复核为 HEAD 纯决策等价，并通过 fresh Cloud clean package
  4 suites/21 tests；该新 Cloud helper 不激活 caller、不增加同路径计数。主体 Design Repair #2 仍
  `BLOCKED，P1=4/P2=1`：run 固定五 handle 粒度错误、UNRESOLVED resolver 不存在、STOPPED false 映射矛盾、H ctx-only
  投影丢失 State handle且 warning concrete owner缺失；
  `systemScaleRatio` 固定由 DHXY capture owner 随同一帧进入 typed wire/digest，另立前置避免并发改 M wire。
- A 的 maintenance config/templates 叶子批准不变；整类 Repair #3 仍 `BLOCKED，P1=4/P2=1`：ledger 错误按
  runRevision 重建并重置 frontier，删除上层引用不能回收底层 retained records，UNKNOWN resolver 不存在，capability 又引用
  不存在的 `REGISTERING/SUBMIT/TransitionEvidence`；A 只补 Repair #4 Delta，Java冻结。
- M fixed-slot/pre-claim/permit 设计通过，只放行双仓 8 New final-consumed DTO 的 M0 叶子；Full R0 因不存在 retained
  lifecycle activation adapter 保持 `BLOCKED，P1=1`。J/M 重启 replacement 分别为
  `019f59d5-7f2f-7df2-b553-abcf6d085f13` 与 `019f59d5-9366-7a41-baba-45a42286ddce`，正在以互斥写集实施。
- 当前同路径计数仍 `185/407`；父级将在两方写入稳定后统一跑 Cloud clean package 与 DHXY compile，host/Task/caller/
  poller 继续 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：领取回执门禁、W-TMS-0B/W-ACP-1 与 SummonSkill W-SS-0

- 外部 Worker 门禁固定为任务发布后 20 分钟内写 `CLAIMED`；20 分钟只判断领取，不限制交付时长。A/B 分别在
  `01:45:57` / `01:49:33` 领取 `01:43:56` 发布的 W-TMS-0B/W-ACP-1，均无需内部接管。
- W-TMS-0B Cloud 2 New 已交付并通过父级源码审查：maintenance probe 仅 MATCH/MISS/STOPPED，unresolved 仅 typed unwind；
  TMS 主体仍 `BLOCKED，P1=2`。W-ACP-1 1 Modify 已通过父级源码审查：missing streak 四分支、10min/60s、普通 long
  算术与 clear 归零保持 HEAD；统一 fresh package 待当前并行写入稳定后执行，Panel 主体仍 `BLOCKED，P1=2/P2=1`。
- M0 双仓 DTO 与 J task-turn 已获父级最终 APPROVED；M lifecycle adapter 1 New+1 Modify 现由内部 P 实施。
  SummonSkillService 主体因整 pass exclusive interaction 与 capture-time `systemScaleRatio` 两项 P1 冻结；独立纯 CPU
  `SummonSkillStaticSlotPolicy` W-SS-0 已 DESIGN APPROVED，由内部 N 实施。
- 当前同路径计数仍 `185/407`；所有新增能力继续无 producer/host/Task/caller。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：lifecycle adapter 与 A/B/N 叶子最终收口

- P lifecycle adapter Cloud 1 New+1 Modify 已父级 `APPROVED，P0/P1/P2=0`：stable entry、bounded admission、exact
  generation resume、opaque PAUSED capability、terminal release 后 retirement 均落盘；fresh Cloud clean package 为
  4 suites/21 tests 全绿，JAR SHA-256 `DE29FD5A...F2EE870F`。
- 同一构建最终批准 A `MaintenanceProbeResult/MaintenanceUnresolvedException`、B
  `AutoCombatPanelDecision` missing-state transition、N `SummonSkillStaticSlotPolicy`。它们均为 dormant leaf，不增加同路径
  计数，主体前置不绕过。
- TMS cleanup 设计收缩：不新增 cleanup action ledger/tri-state/`cleanupFinalized`，纯进程内 cleanup 保持 HEAD 一次调用与
  异常传播；A 只补 W-TMS-D8 短 Delta。Panel warning 仍须闭合 retained notification identity、真实 typed transport 与本地
  warning+metrics 消费 ledger；B 只补 W-ACP-WARN-D2。
- Q 已领取 capture-time typed `systemScaleRatio` 双仓设计；Scale 必须由同帧本地 capture owner 产生，不默认 1.0。当前
  `185/407` 不变，host/Task/caller/poller 继续 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：scale-wire 开始实施 / warning D3 返修 / Full R0 与 whole-pass exclusive 并行

- A 的 maintenance D8 已父级 DESIGN APPROVED；A 在 `02:37:55` 按时领取 `Q-SCALE-WIRE-IMP1`，仅修改批准的双仓
  8 文件，把 capture-time typed `systemScaleRatio` 纳入同帧 payload、strict schema、digest 与 observed-window correlation。
- B 在 `02:36:38` 按时领取并交付 warning D3，但父级仍 `BLOCKED，P1=4/P2=1`：必须补完整 notification response
  variant、DHXY bounded pending-ack owner/明确 settle receipt、candidate occurrence 原子 commit，以及 exact
  per-run/tenant/global budget；“lastMessage exactly-once”更正为 idempotent final-state effect。D4 已发布。
- P 在 `02:37:55` 领取 Full R0 reconciliation，基于已批准 lifecycle adapter 收口 final-consumed/frontier 的最终实施
  Delta；只写固定日志，等待 A scale-wire 稳定后才能合并 remote/digest/schema。内部 R 同时只设计 retained typed whole-pass
  exclusive interaction，保持 SummonSkill HEAD 整 pass 物理输入独占，不搬 HWND/queue、不开放 raw token。
- 当前同路径计数仍 `185/407`；本轮没有新 Java 交付，最近 fresh Cloud package 仍为 21/21。所有 host/Task/caller/poller
  dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：scale-wire/WARN-0 APPROVED，QuestManager 与两项返修续派

- capture-time `systemScaleRatio` 双仓 wire/digest Repair 已父级 `FINAL APPROVED，P0/P1/P2=0`。两仓使用 RFC 8785
  `NumberToJSON.serializeNumber(double)`，Cloud clean package 21/21 与 DHXY compile 均通过；普通 request/非 capture
  outcome 的合法 numeric shape 未变化。
- warning `WARN-0` 两个 Cloud immutable 类型已父级 `FINAL APPROVED`；transport D5 因 ACK poison、outbox/route 双 owner
  与单 FIFO fairness 继续 `BLOCKED，P1=2/P2=1`，B 只做 D6 design Delta。W-ACP-0/1 与 WARN-0 不回退。
- A 转入 `QuestManagerService` 整类 Cloud lift 的 `W-QM-D1`：先完整冻结 HEAD 三页扫描、label/title fallback、glow
  threshold、exclusive detail capture、finally close 与 artifact 语义，父级批准前零 Java。
- 重启丢失的 P/R 内部会话由 P2/R2 接管各自追加式日志；P2 关闭 Full R0 构造/admission/restart/control/counter 五项，
  R2 关闭 SummonSkill exclusive boundary、pause continuation、RELEASE/ABORT stable bytes 与 R-X0 可达状态机四项。
  当前同路径仍 `185/407`，运行面全部 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：QuestManager D2、warning D7 与 SummonSkill R-X0

- A 已按时领取 `W-QM-D2`，只修设计 Delta：同帧 capture 的 scale/window 坐标合同、每次 invocation/probe/action 的
  单调 occurrence、复用现有 `QuestDetailCapture` 的 typed 本地 artifact 结果，以及 HEAD close/finally 和两种输入边界。
  Design #1 当前仍 `BLOCKED，P1=4/P2=1`，Java/resources/schema 冻结。
- B 已按时领取并交付 `W-ACP-WARN-D7`。ACK whole receipt 不重开；D7 提交真实 `ReentrantLock + Condition`
  wake/select、唯一锁序、selector counter reset 与 post-selection failure restore，等待父级复审。
- R2 的 SummonSkill whole-pass exclusive Repair 已父级 `DESIGN APPROVED，P0/P1/P2=0`；`R-X0-IMP1` 已领取，
  写集仅一个 package-private Cloud 状态叶子。P2 的 Full R0 Repair 仍在设计阶段，Java 冻结。
- 当前同路径计数仍 `185/407`；host/Task/caller/poller/UI/capture/input 全部 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：R-X0 APPROVED / Full R0 implementation / BagService 并行

- `CloudTaskExclusiveInteractionState` 已父级 FINAL APPROVED：package-private immutable stable key/cursor/state policy，fresh Cloud
  clean package 4 suites/21 tests 全绿；不接 host/caller，不增加同路径计数。
- Full R0 selector 的全退出 level re-arm 与 route retirement 竞态已关闭，最终设计 `P0/P1/P2=0`；P2 转入批准的 Cloud
  `1 New+16 Modify`、DHXY `12 Java Modify+1 schema` 双仓原子实现，B warning 与其重叠文件继续顺序化。
- QuestManager D2 保留坐标/ROI/resource/exclusive 主边界，但仍须恢复每 candidate/title 的 HEAD fresh capture，补 typed 本地
  artifact intent、跨 slot workflow owner 与 direct failure matrix；A 只做 D3 Delta。warning D8 仍须补 notification-only route 与
  无线程 resend-due wake；B 只做 D9 Delta。
- Internal S 已领取 BagService HEAD 基线整类设计，唯一写集为独立报告。当前同路径仍 `185/407`，运行面全部 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：QuestManager D3 / warning D9 父级复审

- QuestManager D3 已恢复 HEAD 的逐 candidate/title/glow fresh capture，并闭合 Direct lane 对 false/null/close 的真实消费；
  这两部分 PASS。主体仍 `BLOCKED，P1=4/P2=1`：artifact intent 要补全 Cloud request/digest-rebuild/envelope/gate/executor/
  port 链，task code 改 closed allowlist，workflow owner 跨 revision 且同 invocation 重入不铸新 ID，artifact diagnostic 选定唯一合同。
- warning D9 的 notification-first 单一 RouteState 与无线程 due wait 方向 PASS；主体仍 `BLOCKED，P1=3/P2=1`：route
  creation 与 outbox admission 需原子 commit/rollback，ACK/terminal 后必须真实 retirement，claimed due entry 必须从 nextDue
  谓词排除，resend pacing 需 exact transport 配置与 overflow-safe monotonic 算术。
- A/B 均在 20 分钟领取窗内写了 CLAIMED 并完成交付；D4/D10 已写回各自固定日志。P2 Full R0 实施和 S BagService
  设计继续占用两个内部槽，尚无稳定新 Java 可构建。当前同路径仍 `185/407`，host/Task/caller/poller/UI/capture/input dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：warning transport Design Approved / LeftTop 新切片

- warning D10 以 deferred commit 原子关闭 notification-first route/outbox admission，三处 eligibility 共用未 claim 谓词，
  resend pacing 固定为 transport `Limits` 的 1 秒字段并使用饱和 monotonic 算术；父级绑定 retirement 的
  `stateLock -> routeLock -> outboxLock` 一致快照后判定 `DESIGN APPROVED，P0/P1/P2=0`。
- warning Java 与 P2 Full R0 的 broker/routes/transport 写集继续顺序化；B 不等待该依赖，转入独立
  `LeftTopStatusSwitchService` HEAD 整类迁云设计 `W-LTSS-D1`，领取截止 `05:29:08`。
- A 已按时领取并交付 QuestManager D4。closed task code、跨 revision key、UNKNOWN identity 与空 path 方向通过；仍
  `BLOCKED，P1=4/P2=1`：统一 `artifactIntent` digest/wire 键、补齐 DHXY strict DTO/optional field、把 workflow owner
  真正挂入 assembly/runtime generation，并固定 stale/timeout fence 后的本地写盘。D5 已发布。
- B 已于 `05:15:08` 在截止前领取 `W-LTSS-D1`。S 的 BagService D1 inventory/业务矩阵 PASS，但职责切分
  `BLOCKED，P1=3/P2=2`：cache 全部归 Cloud、只复用三操作 port、context 绑定 per-runtime，删除 coordinate 伪 delta 与
  trivial policy leaf；D2 Repair 已交回 S。P2 Full R0 原子实施继续，尚无稳定新 Java 构建点。
- 当前同路径仍 `185/407`，host/Task/caller/poller/UI/capture/input 全部 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：QuestManager D5 通过，LeftTop/CommonBox/Bag 分波推进

- QuestManager D5 已关闭 optional `artifactIntent` strict wire、跨 revision workflow owner、opaque generation 与
  stale/timeout 后 artifact 写盘门，父级 `DESIGN APPROVED，P0/P1/P2=0`；因与 Full R0 重叠，Java 实现继续排在 P2 之后。
- LeftTopStatusSwitch 的纯判定叶子 `LeftTopStatusDecision` 已父级源码 `APPROVED`；D3 又把 pending 业务权威迁入
  Cloud retained state 并复用两仓既有 `WINDOW_CLIENT_PX` enum，整体 `DESIGN APPROVED，P0/P1/P2=0`。主体实现排在
  P2 后；B 转入 `ReturnItemPrescanService` Design #1。
- CommonBox D3 已用一个 governor lock 原子串行 toggle revision publish、role clear 与 async detect final write；但
  assembly 单实例仍只有全 JVM 一份 toggle/revision，跨 tenant 串权威且无 hard cap，当前 `BLOCKED，P1=1/P2=1`，D4
  必须改 bounded per-`CloudServiceScope` state。
- BagService D3 已父级 `DESIGN APPROVED，P0/P1/P2=0`；`ImageFinder.findAll(...,maxMatches)` 单文件 CPU 叶子
  SOURCE APPROVED，fresh package 待 P2 稳定。S 已关闭，内部空槽续派 T 设计 `GiveItemService`。P2 继续 Full R0 双仓
  原子实现，当前无稳定构建点；同路径计数仍 `185/407`，运行面全部 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Runner 本地观察边界与 CommonBox/ReturnItem 叶子放行

- 用户确认本地 `Runner` 必须继续实时监控移动、dialog、战斗、窗口存活等机械事实。迁移边界固定为：DHXY 持续观察/capture/template fact/observer wake/input safety；Cloud 持 phase、timer、fallback、pending 与业务解释。任何 Service 设计不得把监控线程或 watcher 搬成 Cloud host。
- CommonBox D4 的 bounded per-`CloudServiceScope` governor 关闭租户串权威和无界增长；父级只放行 `CloudCommonBoxProperties` + `CommonBoxStateGovernor` 两个状态叶子，异步 detect 改由本地 retained observer 产生 typed observation，完整 service/port/caller 后置。
- ReturnItemPrescan 的 HEAD inventory 与纯策略叶子通过；完整设计须修复 exact window tuple、round/template/occurrence identity、boolean false 与 uncertain 重复动作、observer placement 及 explicit capacity。B 可先实现单文件纯 decision leaf。
- P2 Full R0 继续双仓原子写入，T 继续 GiveItem 设计；当前同路径仍 `185/407`，host/Task/caller/poller/UI/capture/input 均未启动。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：UI clean 本地能力边界

- `UICleanerService` 整体归入本地 retained mechanical/safety capability：exact bound-window capture、地图/checkbox/通用关闭模板匹配、dialog inspect、同 pass 帧复用与动作后重新截图、多层安全关闭和 input queue 执行均保留 DHXY。
- Cloud 只在业务 phase 边界发 typed cleanup intent，并消费 `CLEANED/ALREADY_CLEAN/UNKNOWN/STOPPED`；不得接收 raw screenshot 后自行轮询 UI，不得把 template miss 变成业务 phase 真值，也不得在断线时由本地 cleanup 推进业务状态。
- 迁移计数中的本地保留项不得再被当作“尚未迁云”的欠账：`GameClientTracker` capture、`ImageFinder`/模板资源、`DialogService` 本地探测、`UICleanerService` 和 input queue 是薄客户端执行面的一部分。现有 HEAD cleanup 调用时点、模板顺序、阈值、三层关闭上限、帧失效和点击时序不变。
- ReturnItem 纯 decision 叶子首版因新增 HEAD 不存在的 `SKIP` strategy 与饱和 wall-clock 加法被父级 `BLOCKED`；同一 B 返修。A 已按时领取并交付 CommonBox 两个状态叶子，但父级复核又发现 stale authority fence、并发双 reservation/boolean uncertain、expired-entry cap leak 与 scope-retirement 缺口，当前 `BLOCKED，P1=4/P2=1`，同一 A 定点返修。P2 Full R0 仍在写入。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Full R0 双账本返修 / CommonBox SOURCE APPROVED / TeamReturn 续派

- Full R0 Implementation #1 的 wire、semantic address、本地 admission/input fence 与 receipt outbox 主体通过；父级仍发现
  `P1=2`：callback-success 后 ACK/publish 失败和 `LOCAL_APPLIED` 后 action compaction 失败均可留下 broker/action 半提交。
  原 P2 已于 `06:36:31` 领取 `FULL-R0-IMP1-R1`，只返修三份 Cloud transaction owner，wire/digest/version 与 DHXY 冻结。
- CommonBox Repair #2 已关闭 `CLAIMED/SEALED` 被观察覆盖、短 isolation key 与 retire ABA 三条 P1；两个 dormant Cloud
  状态叶子父级 `SOURCE APPROVED，P0/P1/P2=0`，完整 adapter/Service/observer/caller 仍冻结，最终 package 等 P2 稳定。
- ReturnItemPrescan 纯 decision 叶子已恢复 HEAD 三项 strategy 与普通 long 计算，源码 `APPROVED`；assembly-owned bounded
  state registry 合同已固定。TeamReturn D2 已拆开 legacy wait 与 Wubei live-yield，并修正 poll admission/member occurrence/
  queue boundary；父级 Review #2 仍 `BLOCKED，P1=4/P2=1`：live UNKNOWN/STOPPED、state recreate ABA、precheck artifact
  cap/release 与 legacy no-thread owner 尚未闭合，主体 exact 文件表待 Full R0 稳定。D3 只修设计，Java冻结。
- GiveItem 设计已批准并关闭，但无可脱离 Full R0、Bag、whole-pass 与 client-pixel 前置的独立实现叶子。当前同路径仍
  `185/407`，Runner/UI clean 的本地 capture/template/dialog/soft-wake/input 边界保持，运行面全部 dormant。
- External A 已于 `06:52:46` 领取 `W-NAV-D1`：以 committed HEAD `0114604e` 和五倍/修罗业务基线拆分 3453 行 NavigationService，
  Cloud 保持 route/point/plan 业务权威，本地保留 pathing watcher、capture/template、输入与 terminal fact gate。Internal U
  `019f5b15-70fd-7ee1-91f6-380a5d73552f` 已于 `06:50:17` 领取，只设计 clean HEAD 的 TaskTrackerPanelService local capture / Cloud OCR
  最终边界；两个设计日志与 P2/B Java 写集互不重叠。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Navigation Design #1 父级复审

- External A 的 Navigation 总边界方向通过，但 Design #1 为 `BLOCKED，P0=0/P1=6/P2=2`。HEAD 全树实际只有
  12 个 caller（Wubei 4、FiveRingTaskV2 3、XiuluoTaskV2 5），没有 AutoBattle/direct `navigateToMap`；下一版必须
  逐 caller 固定 result/keep-turn/业务基线映射。
- Cloud 持 semantic occurrence/action，DHXY local ledger 持 exact mechanical execution outcome；断线重投必须只回报、
  不能再执行物理输入。HEAD `NavigationRuntimeState` 的坐标/匹配/decision diagnostic 也不得被整体包装成虚构的 Cloud
  phase store。本地 wake 与 Cloud continuation 必须保持每个 baseline due、read/sleep/check 顺序，route-dialog freshness
  与 mini-map input settle 分开归属。
- `W-NAV-D2` 已发布，只补设计；主体 DTO/store/adapter/Java 继续冻结。P2 Full R0 三文件事务返修仍在写入，B TeamReturn
  D3 和 U TaskTracker 设计并行；当前同路径仍 `185/407`，所有运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：TeamReturn D3 父级复审

- D3 已通过 live `UNKNOWN/STOPPED`、click EXECUTED 非完成、跨 state recreate monotonic frontier 和零 caller legacy
  wait 留 DHXY dormant；整体仍 `BLOCKED，P0=0/P1=2/P2=1`。
- TeamReturn semantic identity 必须直接复用 Full R0 的 `phaseCode/actionSlot/occurrence/attempt`，不能把 current
  `runRevision` 塞入地址后在 pause/resume 重铸身份；revision 只属于 request/context 三道执行 fence。DHXY precheck
  owner 还须为异步 IN_FLIGHT replacement/abandon 提供唯一 flush/release、generation-CAS 与有据 capacity，旧 worker
  不得写入新 slot。
- `W-TEAMRETURN-D4` 已发布只修上述两项，主体 Java/file table 继续等待 Full R0 stable；其它运行面与同路径计数不变。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Full R0 事务返修最终通过

- `FULL-R0-IMP1-R1` 已父级 `FINAL APPROVED，P0/P1/P2=0`。callback-success 后的 ACK/control publication 先完成
  ledger/broker prevalidate/precompute，再确定性提交 `NOTICE_PENDING+QUEUED`；失败则确定性收口 UNKNOWN 并释放 reservation/
  quota，不再留下两账本半提交。
- receipt 在 broker `LOCAL_APPLIED` 后出现 action compact 异常时保留 exact witness 与 pending action-commit marker；同一
  receipt bytes 重投只续办 action compact，不重放 business callback。Cloud 非 2xx/断连到 DHXY delivery-uncertain 的端到端
  路径已复审。
- 父级 fresh Cloud `mvn -q clean package` 通过 4 suites/21 tests、0 failures/errors/skipped；DHXY
  `mvn -q -DskipTests compile` 通过。Full R0 前置门解除，依赖它的 Service 主体可按写集顺序进入实施；同路径仍
  `185/407`，host/Task/poller/UI/capture/input 继续 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Navigation D2 / TaskTracker D1 复审

- Navigation D2 已确认 12 个真实 caller、Cloud semantic action + DHXY mechanical outcome 双层防重、本地 runtime state 与
  “无独立叶子”；仍 `BLOCKED，P1=4/P2=1`：每 caller exact status 分支、逐副作用四态矩阵、本地 dialog 最终 freshness fence、
  以及 Full R0 stable 后的 exact DAG/file/API/cap 表尚未闭合。`W-NAV-D3` 仅修设计，Java冻结。
- TaskTracker D1 的 local capture/template/ROI/fingerprint/prepared-action safety 与 Cloud OCR/text-chain/classification 边界通过；
  仍 `BLOCKED，P1=5/P2=2`：不能用 read-only CAPTURE 发布 prepared state/wake，revision 不能重铸 action identity，local
  release 必须按 Cloud final-consumed control apply 而非“收到 receipt”，Cloud broker encoded outcome bytes要计容，Runner
  不得新增 tracker poll。U 继续 D2 设计返修。
- Full R0 前置已解除；下一版不得再延期真实文件/API 表。当前同路径仍 `185/407`，运行面全部 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Navigation D3 / TeamReturn D4 复审与 Client-PX 实施

- Navigation D3 仍 `BLOCKED，P1=2/P2=2`：caller 表仍错误合并 Wubei/Xiuluo 不同入口，且拟让业务 adapter 直取 raw
  ledger/opaque bundle；D4 必须改用 assembly-owned retained facade + closed typed port，并补 method/line exact matrix/wire。
- TeamReturn D4 已修正 semantic address/revision 分层，仍 `BLOCKED，P1=2/P2=1`：RESERVED cancel 与晚启动 worker 的
  frame release 权冲突、same-key acquire 行为未唯一化，且 Full R0 stable 后 exact file/method/cap 表仍缺。D5 只修设计。
- Internal V 已领取准备实施通用 `WINDOW_CLIENT_PX` input wire，唯一代码面为 Cloud `InputBundleRequest`、DHXY strict
  codec/handler（必要时 mapper）与 schema；转换发生在 current exact binding/revision fence 后，普通
  `SCREEN_ABSOLUTE_PX` canonical bytes/digest/行为不变。U 并行返修 TaskTracker，写集零交叉。
- 当前同路径仍 `185/407`，host/Task/poller/UI/capture/input 未启动。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Client-PX 最终通过与 LeftTop 匹配叶子实施

- 通用 `WINDOW_CLIENT_PX` input wire 已父级最终 `APPROVED，P0/P1/P2=0`：Cloud strict validator 与 DHXY codec
  接受既有 coordinate enum；DHXY 用副作用前 current exact binding 把 client point 转 screen point，并让 worker admission/
  safety supplier 复用同一 geometry snapshot。普通 screen-absolute canonical bytes/digest/行为零变化。
- 父级 fresh Cloud clean package 通过 4 suites/21 tests，DHXY compile 通过；V2 已关闭。该波不增加 DHXY/Cloud 同路径
  文件数，计数保持 `185/407`。
- A 已领取 Navigation D5；B 收到 TeamReturn D7；U2 已领取 TaskTracker D3。第二内部槽派 W 实施已批准的
  `CloudLeftTopTemplateMatcher` 1 New：只对同一 observed PNG 运行 open/closed `TM_CCOEFF_NORMED` 并返回 image-local
  中心，不接 capture/input/pending/retained state/host/caller。左上主体和本地 `UI clean` retained capability 均未激活。
- **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Navigation D4 / TeamReturn D5 复审与重启接管

- Navigation D4 仍 `BLOCKED，P1=2/P2=1`：业务包无法访问 package-private retained state/ActionAddress/retain API，
  现有 public context 只给需要 opaque handle 的 `CloudTaskServicePort`；12 caller 与逐机械 failure 表仍留实现期占位，
  observer transport 也没有当前 exact API。D5 只补真实可编译 retained 入口、完整分支表和依赖波次。
- TeamReturn D5 仍 `BLOCKED，P1=3/P2=1`：`IN_FLIGHT` retire 只增 generation 不落终态，same-key 会永久复用 stale
  handle；独立 frame registry 没有 `RemoteTaskRunRegistry` 的原子 admission/entry-generation/unregister hook；HEAD 单次
  capture 到 async analysis 的 reserve/submit failure 所有权也未定义。D6 只修这些状态机与真实集成点。
- 重启丢失的 U/V 会话已由 U2/V2 接管原 fixed log。U2 仅做 TaskTracker Design Repair；V2 仅实现既有
  `WINDOW_CLIENT_PX` validator/strict codec/current-binding conversion/schema，普通 screen-absolute wire/digest/行为不变。
  两者写集互斥；Java稳定前父级不跑并发构建。
- 当前同路径仍 `185/407`，host/Task/poller/UI/capture/input 未启动。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Navigation 配置与 PlayerState Y0 最终通过

- External A 的 `CloudNavigationProperties` + `CloudNavigationPropertiesAuthority` 两个 Cloud-only 叶子已父级
  `FINAL APPROVED，P0/P1/P2=0`，baseline 560/370/348/376 与 scope-bound immutable snapshot 保持；父级 fresh Cloud
  package 21/21 全绿。后续 `NavigationWorkflowState` D2 已删除 Full R0 外的 commit 真值，terminal cleanup 使用 exact
  assembly terminal handle，固定 5-slot `EnumMap` 提供结构上界并以 enum/equality 拒绝伪槽/错 payload；父级
  `DESIGN APPROVED，P0/P1/P2=0`，A 已获准实施 Cloud 1 New + 2 Modify。
- Internal Y 的 `PlayerFirstAidDecision` Y0 纯判定叶子已父级 `FINAL APPROVED，P0/P1/P2=0`；四字段固定顺序、
  30/50/70、enabled/disabled state、all-disabled/UNKNOWN/部分可读聚合与 `List.copyOf` 精确保持 baseline。父级 fresh
  Cloud package 4 suites/21 tests 全绿；主体 Y1-Y5 仍只修 `runRevision` digest fence、capacity typed owner 与 exact-context
  equality，不越过 wire/receipt-ready/state-owner 门。
- External B 的 TeamReturn D11 已关闭 duplicate begin 与诚实计数边界，但 `RemoteTaskRunRegistry.unregister` 全树零 caller，
  真实 local-capacity 释放是 `consumeTerminal -> releaseTerminalPublication -> releaseTerminal`；同时 session 必须由 trusted
  handler/lifecycle scope 提供，不能反向暴露给 Task context。owner 仍 `BLOCKED，P1=2`，只续派 B 修设计，绝不内部接管。
  Internal Z 并行设计 SummonSkill whole-pass exclusive owner。当前同路径保持 `185/407`；本地 capture/template/OCR、
  continuous watcher、dialog/pathing/movement/battle observation、UICleaner 与输入安全边界不变，运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Navigation workflow FINAL APPROVED / Quest artifact 类型开工

- External A 的 `W-NAV-WF-IMP1` 已父级逐文件 `FINAL APPROVED，P0/P1/P2=0`。`NavigationWorkflowState` 仅冻结
  5 个固定输入槽的 command 参数，结构上界为 5；同 key payload 必须 exact-equal，旧 occurrence/attempt fail-closed，
  新 key 原位覆盖，terminal exact handle 幂等清理。Full R0 仍是唯一业务提交真值。
- 父级 fresh Cloud `mvn -q clean package` exit 0，4 suites/21 tests、0 failures/errors/skipped。A 已续派已批准
  QuestManager D5 的 `W-QM-ARTIFACT-TYPES-IMP1`，先落两仓 closed `XIULUO` enum 与 immutable intent DTO；共享
  Capture wire/codec/digest/assembly 暂不修改。
- TeamReturn owner D2 已关闭 terminal retry/deferred-cancel 外锁，仍缺真实 capture/analyze/frame-result mechanics owner，
  且 registry 必须单一 Move；PlayerState D2 仅剩 active projection 下 epoch replacement 一项 P1；SummonSkill D2 已关闭
  finish cleanup 与 H 后 publication，仅剩 local handle 的 `ACTIVE -> PAUSED -> ACTIVE` snapshot publication 一项 P1。
  B/Y/Z 均由原 Worker 定点返修，不互相接管。
- 同路径计数仍 `185/407`；host/Task/caller/poller/UI/capture/input 保持 dormant，本地 retained mechanics 边界不变。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：PlayerState / SummonSkill 设计收口并进入代码波

- PlayerState D3 已父级 `DESIGN APPROVED，P0/P1/P2=0`。可信 zero proof 仅由 governor projection ledger、三类
  counter、mutation version 与 exact replacement reservation 形成；different epoch replacement 不穿越任何旧 projection，
  session release/rollback/quota 清账有唯一锁序。Y 已转入 `W-PSS-Y3A-STATE-CORE-IMP1`，只写两个 Cloud New。
- SummonSkill D3 已父级 `DESIGN APPROVED，P0/P1/P2=0`。local continuation 在 registry `mutationLock` 内完成
  ACTIVE/PAUSED 双向 publication，token request/resume 永远最后，terminal/replacement 先 invalidate；Z 已领取
  `W-SS-X1-IMP1` 实施 R4.1-R4.4 dormant 双仓原子波。
- A 的 Quest artifact 四类型任务首轮未在 20 分钟内 CLAIMED，已只重发给 External A，绝不内部接管。B 的 TeamReturn
  mechanics D3 因 reserve/capture 顺序、真实 registry 方法签名与 closed handle variant 三项仍
  `BLOCKED，P0=0/P1=2/P2=1`，D4 只由 B 返修设计。
- 当前同路径仍 `185/407`；本地 capture/template/OCR、watcher、dialog/pathing/movement/battle observation、UICleaner 与
  input safety 继续永久保留，全部运行面 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：TeamReturn D4 错窗门与三路实施领取

- TeamReturn D4 已关闭 permit/capture 顺序、typed settle 与 closed handle，但 mechanics 仍把
  `GameClientTracker.captureToMemory` 当远程 handler 的 capture owner，绕开现有 exact
  `BoundWindowCaptureService + WindowNativeBinding`；父级判 `BLOCKED，P1=1/P2=2`，D5 只修 bound-capture capability、
  HEAD ROI/threshold 依赖和单一 `supplyAsync/bindFuture` 时序，Java冻结。
- Quest artifact 四类型任务仍处第二领取窗，绝不内部接管。PlayerState Y 已领取两个 Cloud state-core New；SummonSkill Z
  已领取 R4.1-R4.4 dormant 双仓原子波。两名内部 Worker 均在工作中、尚无稳定交付，本轮不跑并发 clean/package。
- 当前同路径仍 `185/407`，本地 retained mechanics 与全部运行面边界不变。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Quest 四类型已领取并落盘

- External A 已于 `14:12:31` 在重发领取窗内 CLAIMED；两仓 closed `XIULUO` enum 与 immutable intent DTO 共四文件已落盘，
  当前等待 Worker 双构建与正式交付，父级尚未 FINAL APPROVE。External B 的 D5 仍在 `14:29:30` 领取窗内。
- PlayerState Y 已开始 state owner 落码；SummonSkill Z 正连续修改 payload/codec/ledger/digest 等已批准原子波文件。共享 Java
  尚未稳定，本轮不跑并发 Maven。
- 当前同路径仍 `185/407`，本地 retained mechanics 与运行面边界不变。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Quest 类型源码通过 / TeamReturn 叶子实施 / Y-Z 定点返修

- Quest artifact 两仓四类型父级源码 `P0/P1/P2=0`，Cloud package 21/21；DHXY final compile 等待同 remote package 的
  SummonSkill 原子波闭合后统一复验。A 新接 DialogChoice memory 设计，但现有 Cloud exact-copy Service/MemoryService/
  scoped bean 已是批准基线，且远程计数 mutation 仍缺稳定幂等 identity、bootstrap 仍缺首调用线性化；Design #1
  `BLOCKED P1=3/P2=1`，只做 Delta 返修。
- TeamReturn D5 已父级 `DESIGN APPROVED`，B 已领取本地 dormant mechanics 叶子：registry 单一 Move + 5 New；exact
  bound capture capability、HEAD ROI/template/threshold 与单一 async future 保持，handler/lifecycle/caller 暂不接线。
- PlayerState Y state-core 首版因 committed preparation 在 resume 后可返回缓存旧 handle 而 `BLOCKED P1=1`；原 Y 仅修
  governor current-record fence。SummonSkill Z 因 R4 表漏列 `RemoteCommandOutcomeEnvelope` 无-default switch 而
  `BLOCKED P1=1`；已只扩权该文件并恢复原 Z 继续完整双仓原子波。当前同路径仍 `185/407`，运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：PlayerState 源码通过 / TeamReturn 与 DialogChoice 定点返修

- PlayerState state-core Repair 已父级 `SOURCE APPROVED，P0/P1/P2=0`：committed preparation 复用前重新验证 exact current
  entry/generation/preparation/handle、ACTIVE status 与 OPEN session，resume/terminal 后旧 handle 不再复活；Internal Y 已关闭。
- TeamReturn dormant mechanics 1 Move + 5 New 已落盘且 worker DHXY compile 通过；registry 反向还原 SHA 与批准源一致、全树
  单一定义。父级源码仍 `BLOCKED，P1=1`：FRESH 后 capture 前异常与 pickup 后异常退出缺 total settle，会泄漏 frame/permit；
  External B 只修 `LeaderPrecheckMechanics.java`，其余文件冻结。
- DialogChoice memory 当前 Cloud exact-copy Service/MemoryService/scoped bean 已是主体代码迁移结果。A 的 Repair #1 仍错误新增
  per-mutation remote DTO 与 256 FIFO dedupe 第二账本，且内存 bootstrap marker 不具 restart 证据；父级
  `BLOCKED，P1=2/P2=1`，只允许收缩为 Cloud 同进程 facade 直调和生产切换前可信 state artifact 预置/digest 验收门。
- Internal Z 继续 SummonSkill 原子实现，Internal AA 并行设计 TaskTransactionRunner；当前同路径 `185/407`，Java 共享写入稳定前
  不跑父级 clean/package，运行面保持 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：DialogChoice FINAL APPROVED / TeamReturn SOURCE APPROVED

- DialogChoice memory 已确认无需新 Java：Cloud 现有 exact-copy `DialogChoiceMemoryService`、`MemoryService` 与 tenant/user scoped
  bean 是单一业务权威；8 个 caller 随 Task cohort 迁云后仍同进程直调，不建 remote mutation/dedupe ledger，不双写。父级
  `FINAL APPROVED，P0/P1/P2=0`。下一步只形成切换前 canonical JSON 预置、SHA/shape 校验、失败不激活 runbook。
- TeamReturn dormant mechanics Repair 已让 rect/capture/null-attempt/unattached-frame 的异常退出 total release FRESH permit，并让
  pickup 后无 typed result 的 throwable 在 `finally` 经 `completeFailed` settle；父级 `SOURCE APPROVED，P0/P1/P2=0`。
  最终 fresh build 等 Internal Z 同 remote package 原子波稳定；B 下一步只设计 handler/lifecycle 的 exact owner 挂载。
- A/B 新任务均已写入原固定日志并设 20 分钟 CLAIMED 门，绝不内部接管。Internal Z/AA 继续实现/设计，当前同路径仍
  `185/407`，host/Task/caller/poller/UI/capture/input 保持 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Cutover/挂载复审与双内部构建

- DialogChoice runbook 当前 `BLOCKED，P1=2/P2=2`：首次迁移禁止 `REPLACE_EXISTING`，须有 scope cutover lease、
  no-clobber publication 与 operation-token cleanup；tenant/user 目标必须来自真实 authenticated inventory + immutable
  manifest，现有 private/package-private storage 方法和不存在的运维入口不能冒充可执行 resolver。JSON shape 应绑定当前
  `MemoryFile/DialogChoiceEntry`（18 字段），staging 须唯一且具 crash/reopen 证据。
- TeamReturn mount 当前 `BLOCKED，P1=2/P2=1`：`getScaledRect` 是 `[x1,y1,x2,y2]` 且读取 tracker base，不能在 handler
  exact binding 门后继续作为 ROI 权威。B 必须让 bound capability 用 verified binding 生成并返回 exact corner rect，
  mechanics 同帧复用该 rect；Z 稳定后再锚唯一真实 composition root。
- `W-TTR-0` 已落 `leave(outcome)` 与 committed depth/yield 决策，父级只退回 release history 缺 `yieldPolicy` 一项；AA
  正定点修复并 package。Z 的 whole-pass 原子波进入双构建阶段。计数仍 `185/407`，运行面全部 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Cutover runbook 通过 / exact leaf 开工 / whole-pass 定点返修

- DialogChoice cutover runbook Repair #1 已父级 `APPROVED，P0/P1/P2=0`：首次发布 no-clobber、operation-owned cleanup、
  current-class JSON binding 与 crash recovery 均闭合；生产切换仍等待 authenticated inventory 与统一 activation lifecycle。
  Resolver D1 当前 `BLOCKED，P1=3/P2=1`，A 已领取 D2，只补认证 capability、durable recovery-blocked、平台 no-replace blocker
  与正数容量上界，Java冻结。
- TeamReturn exact-window immutable corner leaf 设计已通过，B 只可修改 bound capture capability 与 mechanics 两个 dormant
  leaf；handler/lifecycle 的真实 mount 仍等待 closed typed leader-precheck operation + retained owner，当前
  `BLOCKED，P1=1/P2=1`。截图所示 Move+5 Java 授权是已完成的旧节点，不得重复执行。
- SummonSkill whole-pass 原子波父级复审发现两项 P1：cleanup message null/empty 双仓 digest 分叉，以及
  UNKNOWN-before-pause 后 resume 未 handoff revision/generation。原 Z 已收到真实物理 EOF 的 `W-SS-X1-IMP1-R2`，只改
  Cloud authority 与 DHXY outcome payload 两文件后复跑双构建。Internal AB 已领取 generic retained exclusive projection
  Design #1，为完整 `TaskTransactionRunner` 主体补唯一缺失前置。
- 当前同路径计数仍 `185/407`；本地永久保留 capture/template/OCR、continuous watcher、dialog/pathing/movement/battle
  observation、UICleaner 与 input safety；host/Task/caller/poller/UI/capture/input 均未启动。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：TeamReturn leaf 与 SummonSkill whole-pass 最终通过

- TeamReturn dormant leaf 的 `CaptureAttempt` 已把成功正面积 corner、失败零 corner/非空白 reason 固化到唯一 canonical
  constructor；父级 fresh DHXY compile 通过，`W-TEAMRETURN-MECH-LEAF-IMP2-R1 FINAL APPROVED`。真实 mount 仍等待
  closed typed leader-precheck operation 与 retained owner，不因叶子通过而解冻。
- SummonSkill whole-pass 已统一 cleanup message 的双仓 canonical empty string，并让 UNKNOWN-before-pause 在唯一 transition
  lock 内 park/re-hold，resume 复用既有 handoff 推进 runRevision/bindingGeneration；父级 Cloud package 21/21 与 DHXY
  compile 均通过，`W-SS-X1-IMP1-R2 FINAL APPROVED`，Internal Z 已关闭。
- Generic retained exclusive projection Design #1 仍 `BLOCKED，P1=4`，须保留 committed 120 秒非暂停预算、删除独立
  REBIND、为同一 request 分离 admitted/terminal completion，并消费 retained task/phase 提供的不可铸造稳定 action handle。
  A resolver D2 与原 AB D2 并行设计，Java冻结。当前同路径仍 `185/407`，本地 retained mechanics 和运行面边界不变。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Resolver/TeamReturn/GiveItem 下一设计波

- Resolver D2 已关闭 audit-only manifest、restart FREE、rename no-replace 与未知 cap 四个原问题，但仍缺 active-host/lease
  同锁互斥、durable write-ahead journal 顺序和真实 nested non-mintable capability，父级 `BLOCKED，P1=2/P2=1`；A 只做 D3。
- TeamReturn leaf 已通过后，B 续做 `W-TEAMRETURN-MOUNT-D21`：只设计 closed operation、retained semantic identity、
  handler/capability/mechanics/registry 退出矩阵与精确 DAG；与 AB RX3 共享 remote 文件时必须 RX3 先行，Java冻结。
- Internal AC 已启动 GiveItem 整类迁云 Design #1：Cloud 仅编排，window/capture/template/bag UI/coordinate/input/single worker
  永久本地；重点闭合 normal/exclusive 同义、queue-in-queue、BagService/RX3 依赖和 stable retained action identity。
- 当前同路径 `185/407`，A/B/AB/AC 日志写集互不重叠，所有运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Generic retained exclusive RX3 获准实施

- RX3 D2 已父级 `DESIGN APPROVED，P0/P1/P2=0`：local session 从唯一 request enqueue 起保留 committed 120 秒
  unpaused budget，PAUSE 只做现有累计补偿；到期按 started evidence 产 NOT_EXECUTED/UNKNOWN 并释放单一 worker。
- Wire control 仅 `ACQUIRE/RELEASE/ABORT`，独立 REBIND 已删除；resume 唯一走 registry mutation lock 与 H/K handoff。
  同一 request 的 admitted/terminal 双 completion 允许同步 poll loop 在 ACQUIRE 后继续交付后续 step，且不新增线程/queue。
- `TaskTransactionAction` 由 retained task/phase state 提供 stable address+explicit occurrence；same occurrence exact reuse，下一
  occurrence 仅在上层明确推进且前一 terminal final-consumed/compacted 后接受。Internal AB 正实施 dormant 双仓 RX3；
  runner/Task/caller/host 保持冻结，最终门为 Cloud clean package + DHXY compile。
- 当前同路径仍 `185/407`，B TeamReturn mount 只能设计并声明 RX3 先行，AC GiveItem 设计与 AB 代码写集互不重叠。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Resolver 停驻 / NpcClick 设计开工

- DialogChoice resolver 条件设计不再继续纸面返修：inventory/activation owner 两 seam 不存在，且实施前仍须关闭 public
  create bypass、两阶段 close、force-uncertain recovery-blocked 与 journal 历史容量。状态 `PARKED/BLOCKED，P1=3/P2=1`；
  DialogChoice 代码/runbook 通过结论不回退，生产切换继续禁止。
- External A 转入 `W-NPC-D1`：以 `0114604e` 与 `docs/业务逻辑.md` 为权威，只设计 Cloud business orchestration；
  本地永久保留 HWND/capture/OCR/template/coordinate/Ctrl probe/focus/UICleaner/input。设计须先给 1-3 文件可实施
  W-NPC-0，再列 RX3 后主体，避免继续只产宏观设计。
- AB 正实施 RX3，B 正领取 TeamReturn mount D21，AC 已领取 GiveItem D1；四路写集互不重叠，当前同路径 `185/407`。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：NpcClick 已领取 / GiveItem exact-HWND 返修

- External A 已在 `16:14:53` CLAIMED `W-NPC-D1`，继续 design-only；本地永久保留 NPC 的 exact window capture、OCR/
  template、坐标、Ctrl probe、UICleaner 与输入安全，Cloud 只设计业务阶段和 retained identity。
- GiveItem Design #1 的业务顺序与 normal/direct 调度边界正确，但 W-GIVE-F0 拟从远程 handler 调
  `CoordinateHelper.findImageAbsoluteCoordinate`，仍会经 tracker/current latest vision 形成非 command-bound 截图权威；父级
  `BLOCKED，P0=0/P1=1/P2=1`。原 AC 只补 exact `BindingAccess + BoundWindowCaptureService` 单次 capture、稳定
  systemScaleRatio 到 `WINDOW_CLIENT_PX`、唯一 frame flush owner，并把 RX3 状态改为 design approved/implementation in-flight。
- AB 正写 RX3 双仓 Java，稳定前不并发 clean；B 的 TeamReturn mount 仍只设计并受 20 分钟领取门。当前同路径 `185/407`，
  host/Task/caller/poller/UI/capture/input 未启动。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：NpcClick/ GiveItem 定点返修，TeamReturn 真实 EOF 重发

- NpcClick D1 对 committed queue boundary 的设计偏移已阻断：normal 不能把 Cloud FIFO poll/WAIT/verifier 包进 whole-pass；
  只有调用方本来已持 RX3 handle 时才走 session-bound step。A 当前 `W-NPC-D2` 还需补完整机械 identity 表、双仓 closed
  protocol/file table、NPC Click FIFO 权威 rows 与真实无冲突叶子，`BLOCKED P1=3/P2=2`，Java冻结。
- GiveItem D2 的 exact binding、单次 capture、stable scale 与 flush owner 已通过；仍缺 Cloud/DHXY 对称
  `WindowFactKind`/sealed fact/parser/handler/codec/schema 文件表并写错本地 package，`BLOCKED P1=1/P2=1`。原 AC 只做
  `W-GIVE-D3` Design Repair #2 Delta。
- TeamReturn D21 首发块误落历史中段导致 B 未领取，现已在真实 EOF 记 UNCLAIMED 并原样重发给 B，领取截止
  `16:41`，不内部接管。AB RX3 implementation 继续在途，稳定交付前不跑并发 Maven。当前同路径 `185/407`，运行面
  dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：NpcClick D2 / TeamReturn D21 复审返修

- NpcClick D2 已关闭 session-wide exclusive、机械动作枚举和 baseline rows 缺口；仍因 Ctrl probe 原子 exclusive 被拆成
  三条 command、本地 template/OCR/verifier 无 closed typed wire、Cloud FIFO engine 无可编译 typed facade，以及 stable
  session/decision/candidate owner 不完整而 `BLOCKED P1=3/P2=1`。A 当前 `W-NPC-D3` 仅设计返修，Java冻结。
- TeamReturn D21 已由 B 在真实 EOF 领取并交付；仍缺 BEGIN/CONSUME 间保存 exact `LeaderPrecheckHandle` 的本地 retained
  owner，且误用 UNKNOWN/EXECUTED、重复 envelope identity 并引用 lifecycle request，父级 `BLOCKED P1=3/P2=1`。B 当前
  `W-TEAMRETURN-MOUNT-D22` 仅设计返修；RX3 继续先行，不内部接管。
- GiveItem D3 尚待 AC 补双仓 closed fact 表；AB RX3 implementation 尚未正式交付，Java连续写入期不跑并发 build。当前
  同路径仍 `185/407`，运行面 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：NpcClick D3 复审，generation/typed graph 继续返修

- NpcClick D3 已撤回 Ctrl 三 command 与零新 wire，但仍把 baseline 同一 exclusive callback 内的完整 verifier 延迟到
  `CLICKED_VERIFY_PENDING` 后执行，且未冻结一个 cloud probe candidate 内全部 local small-ring offsets 的共同 RX3 owner；
  父级要求 `CTRL_MENU_PROBE` 在 release 前直接返回完整 baseline 终局。
- 新增 operation 尚缺 common execution state 与业务 payload 的 exact matrix、closed keyword/template enums、strict
  allowed-keys/null/digest 与两仓全部 sealed/builder/parser/handler 文件；typed facade 也未接入当前唯一
  `CloudBrainServer/RemoteTaskRunRoutes/CloudTaskRunAuthorityAssembly` graph。父级 `BLOCKED，P1=4/P2=1`，A 当前
  `W-NPC-D4` 只做设计 Delta、Java冻结。
- 当前 queue 仅以 `sessionId` 建索引，旧 generation terminal 可删除新 generation；现有 producer result 也没有
  `candidateId`。D4 必须给 generation-specific exact identity、late-old no-op 矩阵与唯一 candidate mint/retire owner。
  B 的 D22 仍在领取窗；AB 已恢复 CLAIMED RX3 实施，AC 尚无新稿。当前同路径仍 `185/407`，运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：TeamReturn D22 复审，retained handle 与 closed wire 继续返修

- TeamReturn D22 已给出 BEGIN/CONSUME 和本地 handle owner，但只 retain Live 并把 capture-failed 在 BEGIN 直接
  OBSERVED；这不等价于 baseline 保存 completed-failed handle、稍后统一 CONSUME 的时序。owner 的容量也必须先于
  mechanics I/O 原子占位，不能先 capture/submit 后发现 owner 满或 terminal 已胜出。
- outcome payload 在两仓都必须始终为 closed object；D22 的 null matrix、开放 reason/source、缺 Cloud sealed
  request/outcome/assembly/service-port 真实文件表均被阻断。duplicate request 继续由现有本地 operation ledger exact
  重放，handle owner不得复制 delivery ledger 职责。父级 `BLOCKED，P1=4/P2=1`，B 当前 `W-TEAMRETURN-MOUNT-D23`
  仅设计返修，Java冻结。
- A 已领取 NpcClick D4；AB RX3 实施继续在途，AC GiveItem D3 新稿进入父级复审。当前同路径仍 `185/407`，共享 Java
  稳定前不跑并发 build，运行面 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：外部并发扩为 A/B/C/D

- 新增 External C 的 `W-BAG-C0-IMP1`：基于已父级批准的 Bag D3/C0 合同，只新建 Cloud
  `CloudBagStateOwner` 与 `BagWorkflowState` 两个 state-core 文件；不接 assembly/remote/host/caller，不与 AB RX3 写集重叠。
- 新增 External D 的 `W-RIPS-C0-D1`：只在独立日志一次闭合 ReturnItemPrescan exact state key、global `1000` / per-run
  `64` 容量、跨 revision/terminal/乱序矩阵与最小文件表；父级批准前零 Java。
- C/D 均有独立 append-only 日志与 20 分钟 CLAIMED 门，未领取只重发原 C/D，不交内部 Worker。A/B 原任务和 AB RX3
  保持不变；当前同路径仍 `185/407`，运行面 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：NpcClick D4 / TeamReturn D23 继续定点返修，C/D 已领取

- NpcClick D4 已恢复 complete Ctrl probe 原子边界，但 Cloud request 仍能传入 click/sleep timing；现 queue 的
  `sessions.put`、破坏性 `queue.poll` 与缺 delivered-unconsumed state 不能保证同 generation START/candidate 重投
  幂等。public raw poll/report DTO/facade、operation common-state 与实际 canonical digest tree 也未闭合。父级
  `BLOCKED，P1=4/P2=1`，A 当前 `W-NPC-D5` 只做设计 Delta，Java冻结。
- TeamReturn D23 的 all-handle owner/PENDING 方向正确，但两条 child command 没有共同 parent operation identity；
  terminal-before-begin 的 post-I/O loser cleanup、poll exception 后 sealed UNKNOWN 与完整 request/outcome digest tree
  尚缺。父级 `BLOCKED，P1=4/P2=1`，B 当前 `W-TEAMRETURN-MOUNT-D24` 只做设计 Delta，RX3 继续先行。
- C 已 CLAIMED Bag state-core 两文件实施，D 已 CLAIMED ReturnItemPrescan state/owner 设计；二者均按时领取且与
  A/B/AB 写集独立。AB RX3 仍在连续实施，稳定前不并发 clean。当前同路径 `185/407`，运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：A/B/D 新 Delta 复审，C state-core 源码返修

- NpcClick D5 通过 local timing、state/digest、terminal matrix；剩余 START key/fingerprint 矛盾、candidate 与 ledger
  final-consumed 未桥接、package-private core 跨包注入不可编译，`BLOCKED P1=3/P2=1`，A 转 D6。
- TeamReturn D24 通过 loser cleanup/UNKNOWN/digest；剩余 Cloud-before-dispatch parent mint、full window/revision、
  CHECKED_OUT terminal CAS 与 opaque retained port，`BLOCKED P1=4/P2=1`，B 转 D25。
- Bag C0 两文件已落盘但尚未批准：public raw action id mint、pending stale clear、workflow flow-generation 缺失会破坏
  retained authority；C 只改原两文件。ReturnItemPrescan D1 因 package-private 依赖、attempt correlation、downgrade
  extra return、caller random/clock 与未定 mechanics 被 `BLOCKED P1=5/P2=1`，D 转 D2。
- 四个 external worker 均已真实领取过并交付，当前不是 worker 停滞；AB RX3 仍在实施。计数 `185/407`，运行面 dormant。
  **无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：NpcClick D7、TeamReturn D26、Bag R1、ReturnItemPrescan D2 复审

- NpcClick D8 已闭合完整 receipt/candidate correlation、`DELIVERED_UNCONSUMED → COMPACTED_ADVANCED` 两阶段、
  legacy/retained隔离、真实 codec表与 HEAD timing；父级 `DESIGN APPROVED`。A 的 `W-NPC-0-IMP1` 单文件 timing
  leaf已 `SOURCE APPROVED，P0/P1/P2=0`，统一 Maven门待 AB稳定；A继续 `W-NPC-ENUM-D1` 冻结可独立落码的 closed
  enum/value types，主体 shared remote波仍待 AB。
- TeamReturn D27 已修正 pause/resume occurrence、cleanup/final 分离、nested handle visibility 与 cap 镜像；但单一
  `ActionHandle` 不能同时绑定 BEGIN/CONSUME 两份 request，BOUND/UNKNOWN request也不能因 resume铸 successor，sealed
  UNKNOWN又没有真实 late-final publication入口。父级 `BLOCKED P1=3/P2=0`，B 转 `W-TEAMRETURN-MOUNT-D28`，Java冻结。
- Bag C0 R2 已修 stale revision/raw slot，但公开 cache仍接 caller-mintable scope/window；final-consumption proof仅做
  `instanceof`且 finish无 proof，BOUND_BASE/teardown mint跨包不可接，public record仍暴露 CUSTOM。父级
  `BLOCKED P1=4/P2=2`，C 转 `W-BAG-C0-IMP1-R3`，只改原两文件。ReturnItemPrescan D3 已关闭 D2 的六项
  blocker；父级绑定固定 `1000/64` 容量及 exact run-handle+terminal-binding 后 `DESIGN APPROVED，P0/P1/P2=0`，
  D 转两文件 dormant state-core 实施 `W-RIPS-C0-IMP-A`。
- Internal AB 继续 RX3 双仓实施；Internal AE 已领取 CommonBox 集成设计。共享 Java 稳定前不并发 clean；当前同路径仍
  `185/407`，运行面 dormant。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：Npc/TeamReturn 类型叶子放行，Bag/Prescan 权威返修

- NpcClick 首批只放行三类输入侧 closed enum：scan rect、menu tag 与 verifier mode，两仓共 6 New；结果 enum/record
  仍因 `DIALOG_OPEN_UNVERIFIED` 丢失、不可证明的 `NO_MENU`、有序 template-list/combat null 矩阵与零坐标边界而
  `BLOCKED P1=2/P2=1`。A 已收到 `W-NPC-ENUM-IMP1A+D2`，shared remote/codec/schema继续冻结。
- TeamReturn D28 已把 transaction parent 与 BEGIN/CONSUME 两 child identity、UNKNOWN late-final 和 pause/resume renewal
  闭合，父级 `DESIGN APPROVED`。两仓 4 个 source/disposition enum 已 `SOURCE APPROVED`；B 续做单文件
  `LeaderPrecheckAction`，retained-state child mint仍等 AB。
- Bag R3 与 ReturnItemPrescan R1 的 dormant state-core 都被父级阻断在 non-mintable authority：前者仍有 public owner/
  可替换 permit/pending clear，后者仍用 public binding/records/enum充当 terminal/final proof。C/D 已分别领取 R4/R2，
  严格只改各自两个自建文件。
- CommonBox D1 同样因 occurrence 复用、exact late-final sink、跨 revision mutation 与不可编译首切片而
  `BLOCKED P1=4/P2=2`，AE 已领取 D2。AB RX3 尚未正式交付，当前不运行并发 clean；同路径仍 `185/407`，
  本地 retained mechanics 与所有 dormant 边界不变。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：A/B/C/D 源码叶子通过并续派

- NpcClick 三个输入 enum 的 Cloud/DHXY 六文件镜像已 `SOURCE APPROVED`；结果矩阵修复通过，下一波只落
  `CtrlMenuProbeTerminal` 四值和 `NpcLocalVerifyResult` 三值的双仓镜像，record/sealed/codec仍等 RX3。
- TeamReturn transaction parent `LeaderPrecheckAction` 已 `SOURCE APPROVED`；BEGIN/CONSUME child derivation 因需
  修改 AB 正在收口的 retained state/port，只先做 post-RX3 implementation-ready Delta，不并发写共享文件。
- Bag state owner/workflow R5 已绑定 exact owner-instance + 完整 run tuple；ReturnItemPrescan R3 已把构造、finish、
  complete 与 resolution 全部收为 private/zero-factory，并在 round completion 首写前拒绝 open/UNKNOWN custody。
  两组均 `SOURCE APPROVED，P0/P1/P2=0`，下一波只设计 trusted assembly/settlement seam。
- CommonBox local exact-HWND observation leaf 已落盘、待 AE 正式 D3 交付；AB RX3 仍在最终竞态与 digest parity 审计。
  当前不并发 clean/package，同路径计数仍 `185/407`，本地 capture/template/OCR/watcher/dialog/pathing/movement/
  battle observation/UICleaner/input safety 永久保留。**无已批准业务差异；按基线等价迁移。**

## 2026-07-13 实施进度：用户收缩为原样迁移与原子 InputBundle

- 迁移单位恢复为现有 `Task`/`Service`：类边界、方法调用、条件、顺序、delay、fallback 和返回值以 committed
  `0114604e` 为准，Cloud 不重新设计业务状态机。
- 源码里每一处鼠标/键盘提交只做机械分类：`ONE_BUNDLE`、`LOCAL_MACRO`、`LOCAL_RESIDENT`、
  `NO_PHYSICAL_INPUT`。`ONE_BUNDLE` 使用现有 `RemoteGameClientPort.executeInputBundle`；需要在 held-key/连续输入
  中间做 capture/template/OCR 的流程使用现有本地方法形成一个 `LOCAL_MACRO`，不把内部步骤拆成多条远程命令。
- 共享层只保留所有 Service 共用的错窗/过期 revision/重复执行安全和单 input queue；停止新增 per-Service owner、
  permit、ledger、durable workflow、TTL 或自动 retry。A/B/C/D 已改派 19 个输入调用文件的机械盘点，盘点后直接
  分批迁移和编译。执行计划见 `docs/superpowers/plans/2026-07-13-direct-service-input-bundle-migration.md`。
- 已写复杂源码不在本轮破坏性删除或回滚，先由父级按“共享必需 / 无调用 dormant / 应冻结”分类。
  **无已批准业务差异；按 `0114604e` 基线等价迁移。**
- committed `0114604e` 的 Service/Task 只实际调用 12 种 `InputAction` factory：`clickLeft`、`clickRight`、
  `dragAndDrop`、`moveMouse`、`pressAlt1`、`pressAlt4`、`pressAlt8`、`pressAltA`、`pressAltC`、
  `pressAltQ`、`scrollDown`、`sleep`。现有 `RemoteInputActionType`/`RemoteInputActionMapper` 已逐项覆盖，
  因此当前迁移不需要扩展按键枚举、codec 或 schema；后续工作只剩按原顺序组装 bundle。

## 2026-07-13 实施进度：清单收口并进入首批共享 Java

- A/B/D 输入清单父级通过：已确认 `ONE_BUNDLE` 32 项（A 11 + D 21）；B 的识别后点击按“类型化本地事实
  -> 单 bundle”处理，只有输入中途观察/held-key 流程才是本地宏。C 首轮未领取，已原样重发 C，未内部接管。
- AG/AH 的 20 Service copy-readiness 报告通过但已纠正所有权：机械实现常驻不等于整个业务 Service 留本地；
  Cloud facade 保持原 public 业务调用形状，DHXY 提供事实/宏。
- 首批代码不再做纸面设计：`W-GCF-IMP1` 落共享 `CloudGameClient`（window fact/capture/input bundle）；
  `W-LTS-FACT-IMP1` 落 `LEFT_TOP_STATUS` closed fact 的双仓 wire/handler。二者均禁止 per-Service owner/permit/
  ledger/TTL/retry，完成后由父级双构建并直接迁首个 Service facade。
- 计数仍暂记 `185/407`，待新同名 Service Java 通过 package 后再增量更新。所有运行面 dormant。
  **无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-13 实施进度：首批两个 Service 直接迁云并通过 fresh 双仓构建

- `LeftTopStatusSwitchService` 与 `AutoCombatPanelService` 已完成父级源码审查，均为
  `SOURCE APPROVED，P0/P1/P2=0`。两者保持 `0114604e` 的 public API、判断、顺序、delay、fallback 与状态更新，
  不新增 Service 专属 owner/permit/ledger/TTL/retry。
- Cloud 业务只经 per-run `CloudGameClient` 读取 `LEFT_TOP_STATUS` / `AUTO_COMBAT_PANEL` closed fact，并经
  `InputSequences` 发送原顺序 bundle；DHXY 继续持有 exact-window capture/template 与单一 input queue。
  `UNKNOWN/NOT_EXECUTED` 不会降级成视觉未命中后发送额外输入。
- 父级 fresh DHXY `mvn -q -DskipTests compile` exit 0；fresh Cloud `mvn -q clean package` exit 0，
  4 suites / 21 tests，0 failures/errors/skipped。运行面仍 dormant。
- 父级批准计数增至 `187/407`。机械扫描虽有 `197/407` 个 Cloud 同路径文件，但其中 10 个为暂停/未批准在途件，
  仅保护和分类，不计入已批准迁移进度。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
- RX3 file inventory 已对 AB exact write list 闭合：37 个文件中 7 个纯 generic-exclusive New 标为
  `OBSOLETE_BY_SIMPLIFICATION`，30 个 mixed files 整体保护。后续 cohort 不得引用 transaction parent、generic
  ACQUIRE/RELEASE/ABORT 或 retained-session lane；ordinary scope/window/runRevision fence、stable request/action、
  WINDOW_FACT/CAPTURE/INPUT_BUNDLE、digest/dedupe/terminal 和单 input queue 继续作为共享安全基线。

## 2026-07-13 实施进度：CommonBoxService 直接迁云 FINAL APPROVED

- Cloud `CommonBoxService` 以 committed `0114604e` 迁入，五个 public API、角色开关、异步探测边界、30 秒
  pending TTL、窗口/身份/taskRun 陈旧闸、点击成功清理与失败保留均保持基线。
- 本地永久保留 fixed ROI/template mechanics 与单一输入队列；`COMMON_BOX` closed fact 只报告五态、屏幕绝对
  命中点、score、match timestamp。capture/template/mechanics failure 与 transport UNKNOWN 均不折成 NOT_MATCHED。
- 父级源码/协议审查 `P0/P1/P2=0`；fresh DHXY compile exit 0；fresh Cloud clean package exit 0，
  4 suites / 21 tests 全绿。父级批准计数 `187/407 -> 188/407`；机械存在 `198/407` 中另 10 个未批准件不计完成。
- 下一依赖波为 Bag：复用 shared retained identity 和单一 queue，只新增 closed local-macro 请求/结果；禁止恢复
  逐 Service owner/permit/ledger/TTL/retry 或多轮纸面 Design。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 TRUE EOF 实施进度：TaskMaintenance 29/30、Navigation macro R1、AutoCombat 12 defs

- TaskMaintenance 五族已有 29 个 public API 父级源码通过；queue-head consumer 仍需 shared closed local
  maintenance operation，FIFO 业务状态不回迁本地。B 暂停等待 D 释放写集。
- NAVIGATE_IN_CURRENT_MAP 第一版 wire 因完整 request、terminal status 与 input-queue 执行边界三处不等价而
  `BLOCKED P1=3/P2=1`。返修复用现有 DHXY NavigationService，不再新建第二套 mechanics。
- AutoCombat 累计 12 public definitions 与必要 private closure 可保留，仍缺 6 definitions/真实 caller；CH 暂停
  等 CI/D typed adapters，状态 `PARTIAL SOURCE APPROVED / BLOCKED P1=1`。
- same-path approved count 保持 `189/407`；完整链与 fresh 双构建前不增加。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：废止排除型 cohort，六条完整 public chain 开工

- 上一波真实产出为 TaskTracker 1 方法、SummonSkill 3 方法；TaskMaintenance、Navigation、NpcClick、
  PlayerState 为 zero-Java。父级确认瓶颈来自“缺 collaborator 即排除”的错误任务合同，而不是源码本身；该规则
  已废止，两个 zero-Java 内部 Worker 已关闭。
- TaskTracker 实际新增方法通过父级基线复核。SummonSkill 新增 `matchYellowTemplateInScan` 因在 Cloud 直接调用
  template matcher 被 `BLOCKED P1=1`，由原 C 精确撤回；`readImage/saveImage` 保留。
- External A/B/C/D 已在固定日志真实 EOF 收到互斥完整实现：TaskTracker DecisionEngine public chain、
  TaskMaintenance 三组 public queue/window chain、SummonSkill public whole-pass facade、Navigation 三 public route
  entry chain。必要 Service 自有 state/config/passive closure 同单准入，禁止再交排除清单。
- Internal CE/CF 分别实现 TeamReturn public typed fact/InputBundle/leader-precheck chain 与 BattleRadar
  Cloud state/signal/timer public chain。六路均不迁本地 HWND/capture/template/OCR/watcher/pathing/battle observation/
  physical queue，不新增 owner/session/ledger/business TTL/auto retry。
- 验收标准改为 reachable public call graph + committed behavior + compile/fresh package；private dormant helper 不再
  作为吞吐目标。当前 approved same-path count 仍为 `189/407`，待完整链收口后再增加。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：四个完整算法 cohort + 两个内部适配器已启动

- 父级独立比较 confirmed：TaskMaintenance threshold decision、TaskTracker 五环内存绿链、SummonSkill IF8
  判定、Navigation route-pending freshness gate 均与 committed `0114604e` 对应块 exact，四项
  `SOURCE APPROVED，P0/P1/P2=0`；D 的权威结论已补在其固定日志真实 EOF。
- External A/B/C/D 已直接领取门待认领的新一波：TaskMaintenance summon cache state cohort、TaskTracker
  Xiuluo green-link scan、PlayerState in-memory four-bar summary、Navigation fire-and-handoff/intent/task-code policy。
  四份 Java 写集互斥；20 分钟只检查 `CLAIMED`，绝不内部接管。
- Internal BX/BY 已分别启动 TeamReturn typed button-point fact adapter 与 AutoCombat 两个 pure policy mapper；
  不迁本地 capture/template/OCR/input，不新增 owner/session/ledger/TTL/retry。Internal BV 的
  `BAG_USE_INCENSE` closed local macro 已父级源码通过，等待 consolidated 双构建门。
- 当前仍有并行 Java writer，故暂不运行 `clean`；完整同名 Service/caller 链尚未闭合，approved same-path count
  保持 `189/407`。运行面继续 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A-D 完整算法/Service 收口波已发布

- 上一波四个 private dependency leaf 已父级 `APPROVED，P0/P1/P2=0`，fresh Cloud package
  `4 suites / 21 tests` 全绿，所有旧写集释放。
- External A 当前收口 Cloud `AutoCombatPanelService` 全公开流程；External B 补 Cloud
  `TaskTrackerPanelService` 的 wash -> bands -> pick 内存链；External C 补 `SummonSkillService` 静态技能槽
  LOCKED/EMPTY/OCCUPIED 分类器；External D 补 `PlayerStateService` 四目标补给计划与一次性不可变 settings。
- 四组写集互斥，统一 `06:36` 领取截止；只检查 `CLAIMED`，绝不内部接管。任务均为直接实施，不写 Design，
  不新增 owner/session/ledger/TTL/retry，不启动 capture/input/caller/host。
- approved same-path count 暂保持 `189/407`，待父级源码审查与 fresh package 后按完整闭合结果更新。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：四个外部纯叶子通过 fresh package 并续派

- 父级从 committed `0114604e` 与 Cloud 当前源码独立抽取复核：NpcClick metadata cohort、TaskTracker
  classifier projection、SummonSkill slot offsets 与 PlayerState supply-target helpers 均完整块 exact，文件 SHA
  与 Worker 交付一致，四项 `SOURCE APPROVED，P0/P1/P2=0`。
- 所有 Java writer 稳定后父级 fresh Cloud `mvn -q clean package` exit 0，Surefire
  `4 suites / 21 tests` 全绿并重建 shaded JAR；本波未改 DHXY Java。
- External A/B/C/D 已在固定日志真实 EOF 收到 current-queue identity、image metadata builder、cleanup-result
  builder、first-aid bar probe 四个互斥直接实现单；B/C 已领取，A/D 仍在 `06:12` 截止的领取窗内。
- 四项均是 private dormant CPU/value 依赖，不迁 capture/template/OCR/input，不接 caller，不新增 workflow
  machinery。完整 public/caller/typed local primitive 链未闭合，approved same-path count 保持 `189/407`，
  运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A-D 第二个直接代码波全部通过

- A-D 均在领取窗内领取并交付；父级从 committed `0114604e` 与 Cloud 当前源码独立抽取确认 NpcClick
  current-message identity、TaskTracker image metadata、SummonSkill cleanup-result builder、PlayerState
  first-aid bar probe 均完整块 exact、定义数 1、文件 SHA 一致，四项 `APPROVED，P0/P1/P2=0`。
- D 按真实 committed 8 参方法纠正父级 brief 的近似 6 参描述，没有适配或业务漂移。
- 父级 fresh Cloud `mvn -q clean package` exit 0，Surefire `4 suites / 21 tests` 全绿并重建 shaded JAR；
  本波未改 DHXY Java。
- 四项仍是 private dormant dependencies，完整 public/caller/typed local primitive 链未闭合，approved
  same-path count 保持 `189/407`。下一批优先完整可编译算法 cohort，不再为凑并发切一行 helper，也不把
  matrix 已判定为本地的 transport/window/input 基础设施误迁 Cloud。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：迁移粒度改为六条可计数 public Service 链

- External A 的 TaskTracker 同路径入口因非 detail 模式回归与 baseline diagnostics 缺失被父级
  `BLOCKED，P1=2/P2=1`，保留已正确的 detail 基础链并由原 A 定点修复分流/字段。External B 的
  TaskMaintenance queue/window cohort 已部分通过，仅补 baseline startup cooldown 初始化，`P1=1`。
- External C 的 SummonSkill public local-pass facade 已 `SOURCE APPROVED，P0/P1/P2=0`，下一单为完整
  NpcClick 四 public 智能点击链。External D 的 Navigation zero-Java 不计成果，下一单扩大为双仓
  `NAVIGATE_IN_CURRENT_MAP` closed local macro 真链，capture/OCR/movement/input 交错 mechanics 整体留本地。
- Internal CG 已领取完整 PlayerState supply/first-aid/incense public chain；Internal CH 已领取完整
  AutoCombat 非 host public orchestration chain。六条写集互斥，父级是唯一 reviewer。
- 以后矩阵只以“public caller -> 同路径 Service -> typed local primitive/terminal result”闭合作为整类计数依据；
  private helper、value record、依赖清单和 zero-Java 不增加 `189/407`。所有 Java writer 稳定后统一 fresh Cloud
  clean package；D 写 DHXY Java，因此同轮另跑 DHXY compile。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A/B 首链通过并续派 11/30 public API 大 cohort

- TaskTracker 无回归 mode routing 与五环/修罗 detail exact `links` 已父级功能通过；一处过时 JavaDoc 并入
  External A 下一单，A 直接补齐同路径剩余 11 public API。TaskMaintenance startup cooldown/默认窗口文档已
  `SOURCE APPROVED，P0/P1/P2=0`，External B 直接补齐剩余 30 个纯业务协调 public API。
- External C/D 已分别领取 NpcClick 四 public 智能点击链和双仓 `NAVIGATE_IN_CURRENT_MAP` local macro；Internal
  CG/CH 已领取 PlayerState 与 AutoCombat 全公开链。六路 Java 写集互斥，外部任务仍不内部接管。
- D 的 remote local-macro 结构在途导致 B non-clean compile 出现临时构造器 arity 错误，B 文件零错误；父级不在
  并发写入期运行 clean。完整 Service/caller/typed-local 链经父级复审与 fresh 双门前，计数保持 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE EOF：六条完整 public Service chain 已启动

- 上一波真实产出只有 TaskTracker 1 方法、SummonSkill 3 方法；TaskMaintenance、Navigation、NpcClick、
  PlayerState 为 zero-Java。父级确认“缺 collaborator 即排除”的任务合同是吞吐瓶颈并已废止；CA/CB 已关闭，
  zero-Java 不计迁移成果。
- TaskTracker 实际方法经父级复核通过；SummonSkill 新增 `matchYellowTemplateInScan` 因在 Cloud 直接执行
  template matching 被 `BLOCKED P1=1`，原 C 只撤该方法/import 后继续 public facade。
- External A/B/C/D 的真实 EOF 已发布四条互斥实现链：TaskTracker DecisionEngine caller、TaskMaintenance 三组
  public queue/window API、SummonSkill 两个 public local-pass API、Navigation 三个 public route API。
- Internal CE/CF 分别实施 TeamReturn public typed fact/InputBundle/leader-precheck 与 BattleRadar Cloud-owned
  state/signal/timer APIs。六单允许纳入各 Service committed 自有 state/config/passive closure，不再搬孤立 helper。
- 本地 HWND/capture/template/OCR/watcher/pathing/battle observation/physical queue 永久不迁；禁止新增
  owner/session/ledger/business TTL/auto retry。验收改为 reachable public call graph + 基线等价 + 编译/fresh package。
- 当前 approved same-path count 保持 `189/407`，完整链通过后再增加。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：detail/canonical/log/fact 四块通过并续派

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取确认 TaskTracker supplied-artifact detail crop、
  Navigation canonical map name、AutoCombat deferred-log throttle 与 PlayerState incense fact apply 满足完整块
  合同，四项 `APPROVED，P0/P1/P2=0`；PlayerState 仅保留批准的显式 `windowId` 日志投影。
- 所有 Java writer 稳定后父级 fresh Cloud `mvn -q clean package` exit 0，Surefire
  `4 suites / 21 tests` 全绿，shaded JAR 为 `120458926` bytes；本波未改 DHXY Java。
- A/B/C/D 已在固定日志真实 EOF 收到 TaskTracker 修罗标记图、TaskMaintenance 既有 not-due 诊断节流、
  SummonSkill supplied-path image payload 与 BattleRadar action-state polling interval 四个互斥直接实现单，
  领取截止 `09:02:43`；D 已领取，A/B/C 仍在领取窗内。
- 四项不写 Design、不接 caller/host、不执行本地 capture/template/OCR/input，不新增 workflow machinery。
  它们仍是 dormant prerequisites，完整 public/caller/typed local primitive 链未闭合，approved same-path count
  保持 `189/407`，运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 TRUE EOF 实施进度：从单 helper 调整为六路大 cohort

- 用户确认单 helper 拆分吞吐过低，父级已停止该粒度。A/B/C/D 当前小单源码通过后，立即续派
  TaskTracker pure artifact/image/result、TaskMaintenance summon queue/window-state、SummonSkill
  image/artifact、Navigation route-policy 四个同文件大 cohort；每项至少 6 个完整方法或一条完整算法链。
- Internal CA/CB 同时分别实施 NpcClick pure request/result/metadata 与 PlayerState pure
  snapshot/request/result/policy cohort；六个 Java 写集完全互斥，Worker 不承担 reviewer。
- committed 方法若直接依赖目标缺失 collaborator，只把该候选记录为 `SOURCE_DEPENDENCY_EXCLUDED` 并继续其余
  方法；禁止为了编译适配业务语义或新增 seam/owner/session/ledger/TTL/retry。
- C/D 与 CA/CB 已真实 `CLAIMED`；A/B 仍在 `09:15` 领取窗。所有 writer 稳定后由父级统一 fresh Cloud
  package。完整 public/caller/typed local primitive 链未闭合，approved same-path count 保持 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：队列/算法波通过双构建并续派四个互斥切片

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取确认 TaskMaintenance summon queue 三字段/六方法、
  TaskTracker prepared-action 72 行算法、PlayerState conservative first-aid plan、SummonSkill clean deadline
  常量/方法均完整块 exact、定义数与 SHA 一致，四项 `APPROVED，P0/P1/P2=0`。
- writer 稳定后父级 fresh Cloud `mvn -q clean package` 通过，Surefire `4 suites / 21 tests` 全绿并生成
  `120456347` bytes shaded JAR；DHXY `mvn -q -DskipTests compile` 同轮通过。
- A/B/C/D 新波分别迁 TaskTracker supplied-artifact detail crop、Navigation canonical map name、AutoCombat
  deferred-log throttle、PlayerState incense fact application，四个 Cloud Service 文件互斥，领取截止 `08:39`。
- 新波不写 Design、不接 caller/host、不执行本地 capture/input、不新增 workflow machinery；External 任务逾期
  只重发原 Worker，绝不内部接管。上一波仍为 dormant prerequisites，approved count 保持 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：队列/算法波通过双构建并续派四个互斥切片

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取确认：TaskMaintenance summon queue 三字段/六方法、
  TaskTracker prepared-action 72 行算法、PlayerState conservative first-aid plan、SummonSkill clean deadline
  常量/方法均完整块 exact、定义数与 SHA 一致，四项 `APPROVED，P0/P1/P2=0`。
- 所有 Java writer 稳定后，父级 fresh Cloud `mvn -q clean package` 通过，Surefire `4 suites / 21 tests`
  全绿并生成 `120456347` bytes shaded JAR；DHXY `mvn -q -DskipTests compile` 同轮通过。
- A/B/C/D 新一波分别迁 TaskTracker supplied-artifact detail crop、Navigation canonical map name、AutoCombat
  deferred-log throttle、PlayerState incense fact application；四个 Cloud Service 文件互斥，领取截止 `08:39`。
- 新波只搬 committed 算法/状态投影，不接 caller/host，不执行本地 capture/input，不新增 owner/session/ledger/
  TTL/retry。External 任务逾期只重发原 Worker，绝不内部接管。
- 上一波仍是 dormant prerequisites；完整同名 Service/caller/typed local primitive 链未闭合，approved
  same-path count 保持 `189/407`，运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A-D 完整算法/Service 收口波已发布

- 上一波四个 private dependency leaf 已父级 `APPROVED，P0/P1/P2=0`，fresh Cloud package
  `4 suites / 21 tests` 全绿，所有旧写集释放。
- External A 当前收口 Cloud `AutoCombatPanelService` 全公开流程；External B 补 Cloud
  `TaskTrackerPanelService` 的 wash -> bands -> pick 内存链；External C 补 `SummonSkillService` 静态技能槽
  LOCKED/EMPTY/OCCUPIED 分类器；External D 补 `PlayerStateService` 四目标补给计划与一次性不可变 settings。
- 四组写集互斥，统一 `06:36` 领取截止；只检查 `CLAIMED`，绝不内部接管。任务均为直接实施，不写 Design，
  不新增 owner/session/ledger/TTL/retry，不启动 capture/input/caller/host。
- approved same-path count 暂保持 `189/407`，待父级源码审查与 fresh package 后按完整闭合结果更新。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF：A-D 与 BQ 新叶子通过，BR/BS 续派

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取复核：AutoCombat timing constants、NpcClick
  `pngBytes`、TaskTracker chained-fast result、TeamReturn no-match value 与 PlayerState
  `isSupplyNeededFromSnapshot` 均完整块 `exact=True`，文件 SHA 与 Worker 交付一致，五项
  `SOURCE APPROVED，P0/P1/P2=0`。Navigation duplicate-source helper 在发单前已 exact 存在，按 no-op
  通过且不重复计成果。
- Internal BQ 已关闭。Internal BR 的父级 brief 把真实 `int[]` 误写成 `Rectangle`，BR 在零 Java 写入时阻断；
  父级已按真实基线纠正为 corner tuple并让原 BR 继续。Internal BS 已领取 PlayerState `transferablePng`
  内存编码 leaf。
- External A/B/C/D 已在固定日志真实 EOF 续派 NpcClick template specs、TaskTracker expanded anchor、
  TaskMaintenance first-aid group hash 与 TeamReturn leader-precheck result value；20 分钟门只检查
  `CLAIMED`，绝不内部接管。
- 这些仍是 partial dependency blocks；完整 public/caller/typed local primitive 链未闭合，approved same-path count
  保持 `189/407`。共享 Java 稳定后父级再运行 fresh Cloud clean package，运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A-D 与 BR/BS 六份源码通过并续派

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取复核：NpcClick 三个 template-spec method、TaskTracker
  expanded-anchor、TaskMaintenance first-aid group hash、TeamReturn leader-precheck result、SummonSkill tip rect 与
  PlayerState `transferablePng` 均完整块 exact，目标 SHA 与 Worker 交付一致，六项
  `SOURCE APPROVED，P0/P1/P2=0`；Internal BR/BS 已关闭。
- External A/B/C/D 已在固定日志真实 EOF 收到四个互斥直接实现单：NpcClick metadata cohort、TaskTracker
  classifier projection、SummonSkill slot-offset metadata 与 PlayerState supply-target CPU helper；统一在 `05:42`
  前只需追加 `CLAIMED`，领取后允许持续实施，逾期仅重发原 Worker、绝不内部接管。
- 四波只迁 committed metadata/CPU/result projection，不执行 capture/template/OCR/input，不接 caller，不新增
  owner/session/ledger/TTL/retry。完整 public/caller/typed local primitive 链仍未闭合，approved same-path count
  保持 `189/407`；源码稳定后父级统一运行 fresh Cloud clean package，运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 TRUE EOF 实施进度：A-D 新纯块通过并保持六路代码并行

- 父级独立比较 confirmed：NpcClick outcome/path 五 helper、TaskTracker local-failure + 五 value records、
  TaskMaintenance 四 result mapper、PlayerState incense/summary/SHA、SummonSkill 三 value records 均与
  committed `0114604e` 对应块 `exact=True`，目标 SHA 与 Worker 交付一致，`P0/P1/P2=0`；Internal BJ 已关闭。
- External A/B/C/D 已在固定日志真实 EOF 续派互斥直接实现：NpcClick scan/terminal/SHA、TaskTracker link/band
  conversion、TaskMaintenance pure key mapper、dormant partial AutoBattleTask 三方法。20 分钟门只检查领取，
  外部任务绝不由内部接管。
- Internal BK/BL 已分别领取 SummonSkill 剩余 value records 与 TeamReturn pure diagnostic/geometry helpers，
  写集和四个 external 完全互斥。所有任务继续禁止 per-Service owner/session/ledger/TTL/retry 与运行面。
- 这些仍是 partial dependency blocks，完整 public/caller/typed local primitive 链未闭合；approved same-path count
  保持 `189/407`。共享 Java 稳定后再运行父级 fresh Cloud clean package。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：Service 纯 CPU/value blocks 连续通过并续派

- 父级按 committed `0114604e` 独立逐块复核，Navigation request/cleanup gates、TaskMaintenance remaining-state 与
  no-action/key、NpcClick window geometry、TaskTracker 绿字/五环分段和 image/text、PlayerState bar-pixel、
  SummonSkill color-distance/slot-geometry/payload-text、BattleRadar state core、PlayerRuntimeState 均
  `SOURCE APPROVED，P0/P1/P2=0`。完成的内部 Worker 已关闭，未以 Worker 自审替代父级结论。
- External A/B/C/D 已连续收到互不重叠的直接实施单：NpcClick outcome/path helpers、TaskTracker value records、
  TaskMaintenance request/result mappers、PlayerState pure summary helpers；20 分钟只检查 CLAIMED，逾期仅重发原
  Worker，绝不内部接管。A 对两个基线方法的 `SOURCE-ABSENT` 是当前工作树 grep 误判，父级已用 commit 对象
  `NpcClickService.java:1090/1131` 纠正并让原 A 继续。
- Internal BJ 正迁 SummonSkill 三个纯 value records；本轮未新增 per-Service owner/permit/session/ledger/TTL/retry，
  未搬 capture/template/I/O/input mechanics。完整 public/caller/typed local primitive 链尚未闭合，approved same-path
  count 保持 `189/407`；Java 稳定后再统一 fresh Cloud clean package。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE EOF 实施进度：A-D 与 BK/BL 通过并续派下一波

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取复核：NpcClick scan-rect/terminal/SHA、TaskTracker
  link/band conversion、TaskMaintenance key mappers、AutoBattle dormant task identity/budget、SummonSkill
  remaining value types、TeamReturn diagnostic helpers 均完整块 `exact=True`，目标 SHA 与 Worker 交付一致，
  六项 `SOURCE APPROVED，P0/P1/P2=0`。Internal BK/BL 已关闭。
- External A/B/C/D 已在固定日志真实 EOF 续派 tooltip-path、title-family、identity-index leaf、AutoBattle
  retry-policy leaf 四个互斥直接实施单；20 分钟门只检查 `CLAIMED`，逾期仅原样重发原 Worker，绝不内部接管。
- Internal BM/BN 已分别领取 PlayerState taskRunId 投影与 TeamReturn pathing 文本格式化，唯一 Java 写集互斥，
  不迁本地 capture/template/OCR/input/runtime authority，不新增 owner/session/ledger/TTL/retry。
- 本轮仍为 partial dependency migration；完整 public/caller/typed local primitive 链未闭合，approved same-path count
  保持 `189/407`。共享 Java 稳定后父级统一运行 fresh Cloud `mvn -q clean package`，运行面继续 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 TRUE EOF 实施进度：A-D 新纯块通过并保持六路代码并行

- 父级独立比较 confirmed：NpcClick outcome/path 五 helper、TaskTracker local-failure + 五 value records、
  TaskMaintenance 四 result mapper、PlayerState incense/summary/SHA、SummonSkill 三 value records 均与
  committed `0114604e` 对应块 `exact=True`，目标 SHA 与 Worker 交付一致，`P0/P1/P2=0`；Internal BJ 已关闭。
- External A/B/C/D 已在固定日志真实 EOF 续派互斥直接实现：NpcClick scan/terminal/SHA、TaskTracker link/band
  conversion、TaskMaintenance pure key mapper、dormant partial AutoBattleTask 三方法。20 分钟门只检查领取，
  外部任务绝不由内部接管。
- Internal BK/BL 已分别领取 SummonSkill 剩余 value records 与 TeamReturn pure diagnostic/geometry helpers，
  写集和四个 external 完全互斥。所有任务继续禁止 per-Service owner/session/ledger/TTL/retry 与运行面。
- 这些仍是 partial dependency blocks，完整 public/caller/typed local primitive 链未闭合；approved same-path count
  保持 `189/407`。共享 Java 稳定后再运行父级 fresh Cloud clean package。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-13 实施进度：TaskTracker 算法首刀通过 / TeamReturn 按钮 fact 开工

- Cloud `TaskTrackerPanelService` 已从 `DecisionEngine.TRACKER_PANEL_READER` 等价抽出完整 panel/detail 几何、
  标题/detail crop、绿链分割、候选选择、五倍 OCR、diagnostics 与 flush/finally；router 只保留单次委派。
  父级源码复核 `P0/P1/P2=0`，fresh Cloud clean package 为 4 suites / 21 tests 全绿。exact-window typed capture/
  tracker drag 端口仍待后续切片，故当前批准计数保持 `189/407`。
- 下一直接依赖选取 committed `TeamReturnService.findReturnTeamButton()` 的纯机械观察，不迁其业务判断：External A
  只新增 exact-binding ROI/template mechanics，B 只增 Cloud `TEAM_RETURN_BUTTON` closed fact，C 只增 DHXY
  镜像 fact/handler 投影，D 只同步 schema。状态固定为 PRESENT/ABSENT/CAPTURE_UNAVAILABLE/
  TEMPLATE_UNAVAILABLE/MECHANICS_FAILED；仅 PRESENT 携 screen-absolute 点和 score。Cloud 后续仍按原顺序负责
  摄妖香、二次观察与单 click bundle。本地不得自行点击或推进业务。
- 四单均为直接实现，不写 Design #N，不新增 owner/session/ledger/TTL/retry；运行面保持 dormant。
  **无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-13 用户确定 TaskTrackerPanelService / QuestManagerService 落点

- `TaskTrackerPanelService` 迁 Cloud。panel/detail 几何、绿链像素分割、fingerprint/cache 命中、候选排序、
  任务分类与结果组织均属于算法，不能因输入是截图而留在 DHXY。Cloud 同名 Service 保持 `0114604e` 的 public
  调用形状、阈值、顺序、cache/fallback 与返回值；DHXY 仅提供 exact-window capture、本地 template/OCR 原语、
  tracker 拖拽/InputBundle 与 closed typed observation。
- `QuestManagerService` 留 DHXY。调用方已给定 task；本类只在 exact task panel 内模板/高亮匹配、滚动、固定点击、
  详情截图并返回结果，不选择任务 phase、优先级或跨 Service 策略。Cloud 业务调用方的既有判断保持不变。
- 当前仅更新所有权矩阵，不改变 Java 或批准计数；approved same-path count 保持 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-13 实施进度：ReturnItemPrescanService + Bag 本地宏 FINAL APPROVED

- Cloud `ReturnItemPrescanService` 保持 committed `0114604e` 的随机策略、4 秒 maintenance 门、8..18 秒 combat due、
  cache/invalidation、fallback 与 complete-round 语义；真实 `BagService` 继续是 `LOCAL_RESIDENT_SERVICE`，只在本地
  单一输入队列中执行任务页预扫、从后往前预扫和使用缓存点三项 closed mechanics。
- `LOCAL_MACRO/BAG_RETURN_ITEM` 已闭合 Cloud types、DHXY strict wire/digest、exact-window handler/mechanics、
  transport/final-ack 与 schema。terminal flat payload 恰四键；DHXY 与 Cloud 均只允许
  `EXECUTED/NOT_EXECUTED/STOPPED/UNKNOWN`，拒绝 `OBSERVED/null`，typed result 仅 `EXECUTED` 存在。
- 父级源码/协议审查 `P0/P1/P2=0`；fresh DHXY compile exit 0；fresh Cloud clean package exit 0，
  4 suites / 21 tests，0 failures/errors/skipped。批准计数 `188/407 -> 189/407`，运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-13 用户冻结的本地 Service 落点

- `BagService`：`LOCAL_RESIDENT_SERVICE`。真实类与全部截图、模板匹配、翻页、输入队列和点击实现留 DHXY；
  Cloud 不建同名业务副本。Cloud 调用者只经 closed typed local macro 请求三项已选择操作并读取 typed result。
- `UICleanerService`：`LOCAL_RESIDENT_SERVICE`。UI 观察、模板/OCR 和清理动作全部留 DHXY；需要它的 Cloud 业务
  只能请求闭合本地操作，不得复制 UI 清理实现。
- `GiveItemService`：`LOCAL_RESIDENT_SERVICE`。committed `0114604e` 唯一调用点是 `DialogService` 已进入
  input-worker 独占段后的 `executeGiveDirectForExclusive(...)`；选物继续委托本地 `BagService`，随后本地匹配“给予”
  按钮并固定点击。Cloud 不建同名副本，当前也不新增独立 `GIVE_ITEM` wire；未来 Dialog closed local macro 必须把
  这段既有调用整体包住，不能在选物与点击之间插入网络往返。
- 对其它“本地与 Cloud 均可行”的 Service，不再由 agent 根据方便程度判定。父级必须先列出两个落点、依赖、
  延迟/离线影响和推荐，再由用户明确选择后更新本矩阵。
- `LOCAL_RESIDENT_SERVICE` 判据：Service 只完成 exact-window 观察、图片/模板/OCR 匹配、固定输入序列和 typed
  result，不决定任务 phase、业务策略、跨 Service 编排或业务 retry/fallback。固定 mechanics delay、安全拒绝和
  cleanup 仍属于本地机械能力。具有任何业务脑子的 Service 迁 Cloud；边界不清由用户逐项决定。
- 当前 Bag 波次因此只迁 `ReturnItemPrescanService` 业务编排并建设 shared `LOCAL_MACRO/BAG_RETURN_ITEM`；
  approved same-path count 仍为 `188/407`，待源码与双构建门通过后再增量。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：TeamReturn 按钮 fact FINAL APPROVED / leader 与 TaskTracker 续波

- `TEAM_RETURN_BUTTON` exact-window mechanics、Cloud/DHXY closed fact、handler 与 schema 已完成父级源码/协议
  审查，`P0/P1/P2=0`。fresh DHXY compile exit 0；fresh Cloud clean package exit 0，4 suites / 21 tests 全绿。
- Cloud `TeamReturnService` 的 member-marker public cohort 已按 committed `0114604e` 迁入并通过父级源码审查：
  只读一次 typed fact，`PRESENT/ABSENT` 直映射，其余事实态、transport 非 OBSERVED、类型不符与 interrupt 均为
  `UNKNOWN`；shared occurrence 在 final consumption 后递增，下一 idle tick 不复用旧截图。
- leader 续波由 External B/C/D 分别实现 Cloud closed fact、DHXY mirror/handler 与 exact-window `zhao.png`
  mechanics；全部已在领取窗内 `CLAIMED`，不写 Design、不内部接管。Internal AU/AV 同时实施 TaskTracker 的
  typed Cloud request 入口与只读面板锚点/矩形 mechanics，写集与外部 TeamReturn 完全互斥。
- 当前 approved same-path count 保持 `189/407`；TeamReturn 同名 Service 其余 public cohort 与 TaskTracker typed
  capture/drag 端到端链闭合后再按父级 fresh 双构建结果计数。运行面仍 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：TeamReturn leader wire / TaskTracker rect typed 链源码通过

- TeamReturn leader 的 Cloud `TEAM_RETURN_LEADER_SIGNAL`、DHXY mirror/handler 与 exact-window `zhao.png`
  mechanics 已父级 `SOURCE APPROVED，P0/P1/P2=0`；本地只做一次 capture/template match，失败态不折为
  ABSENT，不发输入。
- TaskTracker Cloud 主算法现先经 typed request；本地新增 exact-window panel anchor/rectangle 单 capture/单
  match 六态 mechanics，双端 `TASK_TRACKER_PANEL_RECT` closed fact 已通过源码审查。panel/detail 几何、绿链
  分割、fingerprint/cache、候选排序、分类、OCR 与结果组织仍只在 Cloud。
- A/B/C/D 已收到下一批互斥直接实施单：Cloud leader public probe、协议 schema、typed Point origin、DHXY rect
  handler。禁止 Design #N、per-Service owner/session/ledger/TTL/retry；20 分钟只检查 CLAIMED。
- approved same-path count 保持 `189/407`；等待外部 Java 稳定后的父级 fresh DHXY compile / Cloud clean package
  和 TaskTracker capture/drag 调用链闭合。运行面全部 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：TeamReturn leader / TaskTracker rect 续波通过 fresh 双仓门

- External A/B/C/D 已完成下一批互斥直接实施：Cloud `isReturnTeamSignalPresent()`、两个 WINDOW_FACT schema、
  TaskTracker typed `Point` origin、DHXY rect enum/handler；四份父级源码/协议结论均为
  `APPROVED，P0/P1/P2=0`。
- TeamReturn leader 每次只读取一个 `TEAM_RETURN_LEADER_SIGNAL` fact；TaskTracker rect 在 exact binding 上只做
  单 capture/单 match，六态和 window-client 坐标原样投影。两条路径都不在 DHXY 推进业务或发送输入。
- 父级 fresh DHXY compile exit 0；fresh Cloud clean package exit 0，4 suites / 21 tests 全绿，新 shaded JAR
  已生成。TeamReturn 与 TaskTracker 同名 Service 仍有 public cohort/capture-drag 链待迁，approved same-path count
  保持 `189/407`，不提前计数。运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：A-D 与 BB/BC 六路基线等价续波

- 上一批 Navigation route ROI、TaskTracker rect parser、TaskMaintenance session attach、AutoCombat first-aid
  mapper、PlayerState probe models 与 NpcClick request DTO 均已父级源码审查通过，`P0/P1/P2=0`。
- External A/B/C/D 已在固定日志真实 EOF 收到新的直接实现单：Navigation 八个 route nested model、
  TaskTracker 绿色文字分割纯 CPU 内核、TaskMaintenance first-aid queue 两个 committed 状态类型、AutoCombat
  committed runtime state 形状。四项写集互斥；只检查 20 分钟内 `CLAIMED`，绝不内部接管。
- Internal BB/BC 已分别领取 NpcClick 四个结果模型和 PlayerState 三个 remaining first-aid model；两项与四个
  external 写集互斥。父级是唯一 reviewer，稳定后统一执行 fresh Cloud clean package。
- 当前这些同名 Service 尚未闭合完整 public 调用链，approved same-path count 诚实保持 `189/407`；运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 实施进度：route/runtime model 四项通过并续派

- 父级独立抽取比较确认：Navigation 八个 route nested model、AutoCombat 完整 runtime-state shape、NpcClick
  四个 result model、PlayerState 三个 remaining first-aid record 均逐块 `exact=True`，文件 SHA 与 Worker 交付
  一致；四项 `APPROVED，P0/P1/P2=0`，两个完成的内部 Worker 已关闭。
- External A/D 已直接续派同文件下一批 baseline 代码；External B 已领取 TaskTracker 绿字分割纯 CPU 内核；
  External C 等待领取 summon state types。四个外部任务只检查 20 分钟内 `CLAIMED`，绝不内部接管。
- 新 Internal BD/BE 分别实施 BattleRadar 的 exit-signal state core 与 PlayerState runtime-state shape，写集与
  A/B/C/D 互斥。Java 连续写入稳定后再由父级统一跑 fresh Cloud clean package。
- 当前同名 Service 的 public/caller 链仍未闭合，approved same-path count 保持 `189/407`；运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 TRUE EOF 实施进度：Service 纯 CPU/value blocks 连续通过并续派

- 父级按 committed `0114604e` 独立逐块复核，Navigation request/cleanup gates、TaskMaintenance remaining-state 与
  no-action/key、NpcClick window geometry、TaskTracker 绿字/五环分段和 image/text、PlayerState bar-pixel、
  SummonSkill color-distance/slot-geometry/payload-text、BattleRadar state core、PlayerRuntimeState 均
  `SOURCE APPROVED，P0/P1/P2=0`。完成的内部 Worker 已关闭，未以 Worker 自审替代父级结论。
- External A/B/C/D 已连续收到互不重叠的直接实施单：NpcClick outcome/path helpers、TaskTracker value records、
  TaskMaintenance request/result mappers、PlayerState pure summary helpers；20 分钟只检查 CLAIMED，逾期仅重发原
  Worker，绝不内部接管。A 对两个基线方法的 `SOURCE-ABSENT` 是当前工作树 grep 误判，父级已用 commit 对象
  `NpcClickService.java:1090/1131` 纠正并让原 A 继续。
- Internal BJ 正迁 SummonSkill 三个纯 value records；本轮未新增 per-Service owner/permit/session/ledger/TTL/retry，
  未搬 capture/template/I/O/input mechanics。完整 public/caller/typed local primitive 链尚未闭合，approved same-path
  count 保持 `189/407`；Java 稳定后再统一 fresh Cloud clean package。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF：A-D 与 BK/BL 通过，下一波已启动

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取复核：NpcClick scan-rect/terminal/SHA、TaskTracker
  link/band conversion、TaskMaintenance key mappers、AutoBattle dormant task identity/budget、SummonSkill
  remaining value types、TeamReturn diagnostic helpers 均完整块 `exact=True`，目标 SHA 与交付一致，六项
  `SOURCE APPROVED，P0/P1/P2=0`；Internal BK/BL 已关闭。
- External A/B/C/D 已在固定日志真实 EOF 续派 tooltip-path、title-family、identity-index leaf、AutoBattle
  retry-policy leaf 四个互斥直接实施单；20 分钟门只检查 `CLAIMED`，逾期仅原样重发原 Worker，绝不内部接管。
- Internal BM 首次机械复制暴露 Cloud `taskRunId:String` 与本地 `taskRunId:long` 的真实类型不兼容，父级
  `BLOCKED P1=1`，已要求原 BM 只撤销自己未批准且导致编译失败的增量，禁止擅自解析改语义。Internal BN 的
  `pathingText` 方法体正确，但签名多一个空格且编译被 BM 同期错误污染，父级 `P2=1`，已交原 BN 定点修复和复跑。
- 完整 public/caller/typed local primitive 链仍未闭合，approved same-path count 保持 `189/407`；两项内部返修与
  外部 Java 稳定后父级统一运行 fresh Cloud `mvn -q clean package`，运行面继续 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE PHYSICAL EOF：A-D 第二波与 BN 通过，BM 撤回，六路续派

- 父级按 committed `0114604e` 独立逐字符复核：NpcClick tooltip-path、TaskTracker 五倍/修罗 title-family、
  TaskMaintenance identity index、AutoBattle retry-policy 与 TeamReturn `pathingText` 均完整块 `exact=True`，
  文件 SHA 与交付一致，五项 `SOURCE APPROVED，P0/P1/P2=0`。
- BM 的 PlayerState `taskRunId` 机械复制未通过类型门：本地 context 为 `long`，Cloud context 为 `String`。
  未批准任何 parse/default/数值比较语义；BM 已仅撤回自己的 import、JavaDoc 与 helper，Cloud compile 恢复通过，
  本项不计迁移成果。BN 已通过并关闭。
- External A/B/C/D 当前直接实现 verification-outcome mapper、五环 title value、first-aid participant projection、
  AutoBattle polling constants；20 分钟只检查 `CLAIMED`，绝不内部接管。Internal BO/BP 当前分别实现
  PlayerState `calculateX` 与 LeftTop `resolveState`，六份 Java 写集互斥。
- 这些仍是 partial helper/value 依赖；同名 Service/Task 的完整 public API、caller 与 typed local primitive 链未闭合，
  approved same-path count 保持 `189/407`。共享 Java 稳定后父级再运行 fresh Cloud clean package。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A-D 与 BQ 新叶子通过，BR/BS 续派

- 父级从 committed `0114604e` 与当前 Cloud 独立抽取复核：AutoCombat timing constants、NpcClick
  `pngBytes`、TaskTracker chained-fast result、TeamReturn no-match value 与 PlayerState
  `isSupplyNeededFromSnapshot` 均完整块 `exact=True`，文件 SHA 与 Worker 交付一致，五项
  `SOURCE APPROVED，P0/P1/P2=0`。Navigation duplicate-source helper 在发单前已 exact 存在，按 no-op
  通过且不重复计成果。
- Internal BQ 已关闭。Internal BR 的父级 brief 把真实 `int[]` 误写成 `Rectangle`，BR 在零 Java 写入时阻断；
  父级已按真实基线纠正为 corner tuple 并让原 BR 继续。Internal BS 已领取 PlayerState `transferablePng`
  内存编码 leaf。
- External A/B/C/D 已在固定日志真实 EOF 续派 NpcClick template specs、TaskTracker expanded anchor、
  TaskMaintenance first-aid group hash 与 TeamReturn leader-precheck result value；20 分钟门只检查
  `CLAIMED`，绝不内部接管。
- 这些仍是 partial dependency blocks；完整 public/caller/typed local primitive 链未闭合，approved same-path count
  保持 `189/407`。共享 Java 稳定后父级再运行 fresh Cloud clean package，运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：四个外部纯叶子通过 fresh package 并续派

- 父级从 committed `0114604e` 与 Cloud 当前源码独立抽取复核：NpcClick metadata cohort、TaskTracker
  classifier projection、SummonSkill slot offsets 与 PlayerState supply-target helpers 均完整块 exact，文件 SHA
  与 Worker 交付一致，四项 `SOURCE APPROVED，P0/P1/P2=0`。
- 所有 Java writer 稳定后父级 fresh Cloud `mvn -q clean package` exit 0，Surefire
  `4 suites / 21 tests` 全绿并重建 shaded JAR；本波未改 DHXY Java。
- External A/B/C/D 已在固定日志真实 EOF 收到 current-queue identity、image metadata builder、cleanup-result
  builder、first-aid bar probe 四个互斥直接实现单；B/C 已领取，A/D 仍在 `06:12` 截止的领取窗内。
- 四项均是 private dormant CPU/value 依赖，不迁 capture/template/OCR/input，不接 caller，不新增 workflow
  machinery。完整 public/caller/typed local primitive 链未闭合，approved same-path count 保持 `189/407`，
  运行面 dormant。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：A-D 第二个直接代码波全部通过

- A-D 均在领取窗内领取并交付；父级从 committed `0114604e` 与 Cloud 当前源码独立抽取确认 NpcClick
  current-message identity、TaskTracker image metadata、SummonSkill cleanup-result builder、PlayerState
  first-aid bar probe 均完整块 exact、定义数 1、文件 SHA 一致，四项 `APPROVED，P0/P1/P2=0`。
- D 按真实 committed 8 参方法纠正父级 brief 的近似 6 参描述，没有适配或业务漂移。
- 父级 fresh Cloud `mvn -q clean package` exit 0，Surefire `4 suites / 21 tests` 全绿并重建 shaded JAR；
  本波未改 DHXY Java。
- 四项仍是 private dormant dependencies，完整 public/caller/typed local primitive 链未闭合，approved
  same-path count 保持 `189/407`。下一批优先完整可编译算法 cohort，不再为凑并发切一行 helper，也不把
  matrix 已判定为本地的 transport/window/input 基础设施误迁 Cloud。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
## 2026-07-14 10:14 - AutoCombat public-chain implementation progress

- `AutoCombatService`：10 个支撑 public API 经父级源码复核可保留；8 个 committed orchestration/read-only/
  reconcile 主入口与真实 caller 尚缺，状态 `PARTIAL SOURCE APPROVED / BLOCKED (P1=1)`。
- 缺口依赖：TaskMaintenance team-phase/session public surface、DHXY continuous battle watcher typed verdict、DHXY-local
  UICleaner closed operation。不得把本地 capture/template/HWND/input worker 搬入 Cloud，也不得以固定终态绕过。
- same-path approved count 仍为 `189/407`；完整 caller -> Service -> typed terminal 链和 fresh package 通过后才计数。

## 2026-07-14 TRUE EOF 实施进度：A typed contract、CG 顺序阻断与 CI BattleRadar 三入口

- TaskTracker 剩余 11 个 public API 采用单一 passive typed artifact：本地只产 capture/template/OCR primitive，
  Cloud 保留几何、绿链、fingerprint/cache、候选排序、分类和结果构造算法，并返回 window-relative 结果。
- PlayerState 首版因 `healAll` 与 `healPlayer/healPet` 的 capture/input 原子边界和逐目标顺序不等价而
  `BLOCKED，P1=2`。原 CG 已安全暂停，等待 D 的 shared local macro 写集释放；已通过部分保留。
- 新 Internal CI 只改 Cloud `BattleRadarService.java`，直接实施 full-radar、fast-avatar-exit、trusted-baseline
  refresh 三个 typed-consumer public 入口。DHXY battle watcher/observation 不迁，Cloud 不读取 HWND/image path。
- 六路写集保持互斥；完整链与 fresh build 未闭合前 same-path approved count 仍为 `189/407`。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## 2026-07-14 AUTHORITATIVE TRUE PHYSICAL EOF：整类复制成为唯一计数门

- `0114604e` 仅作为当前结构/迁移起点；涉及五倍、修罗业务判断时，以 `docs/业务逻辑.md` 指定的用户已验证
  提交为行为权威。两者冲突时停止该行为改动并请用户裁定，不得把架构提交冒充唯一业务基线。
- 默认实施方式是复制适用业务基线的完整 `Task` / `Service`：原 public/private 调用图、判断、顺序、delay、
  fallback 与状态更新不拆，只替换原地的本地 mechanics 调用。方法/helper/value/zero-Java 不再作为整类成果
  或进度代理。
- 已落方法级代码全部保留为整类拼回时的可复用块，不回滚他人在途改动。Internal CI 的 BattleRadar 三个 typed
  consumer 已父级源码通过但 caller 未闭合，故不增计数；External A 的 TaskTracker 首版因四个 P1 typed
  contract/业务字段缺口被阻断并交原 A 一次性返修。
- Navigation closed macro 必须传完整 `NavigationRequest` 与完整结果状态，并在 input queue 外调用既有 DHXY
  `NavigationService.navigateInCurrentMap`；不复制、不缩短 60 秒 loop、候选、watcher、keep-turn 或 cleanup。
- 当前 approved same-path 仍为 `189/407`。只有完整 public caller -> 同路径 Service -> typed local primitive/
  terminal result 经父级整类对照和 fresh 构建通过后才增加。

**无已批准业务差异；按适用的用户已验证业务基线等价迁移。**

## 2026-07-14 11:44 - 696a12b0 whole-Service active promotion 到 31/32

- Cloud 证据镜像继续保持 `32/32`、Git blob `BAD=0`、Runner `0`；16 个旧 active divergent 版本已完整保存，
  promotion 不丢失任何在途实现。
- Internal CI2/CJ2 的 12 文件 promotion 已父级通过。External A/B/D 又分别把
  `TaskTrackerPanelService.java`、`TaskMaintenanceService.java`、`NavigationService.java` 原字节替换为
  `696a12b0` baseline；父级独立复核 preservation preflight 与 active/baseline 后置 blob，三项
  `APPROVED，P0/P1/P2=0`。
- active Service tree 当前 `32` 文件全部存在、`31/32` 与 baseline byte-exact；唯一差异是 External C 持有的
  `NpcClickService.java`。C 首轮 20 分钟暂停确认窗未响应，父级已在 C 固定日志真实 EOF 记录
  `UNACKNOWLEDGED` 并原样重发同一暂停指令，第二截止 `12:03:50`；绝不内部接管。
- 当前 approved same-path 保持 `189/407`。只有 `32/32` active exact 后才进入删除四个永久本地重复 Service，
  之后才运行 Cloud 编译并补最小 typed boundary；当前不提前拆动作、不运行构建。

**无已批准业务差异；按 `696a12b0` 原字节完整 Service 基线推进。**

## 2026-07-14 18:24 EDT - 整类审查与大 cohort 连续调度

- active 事实保持 `28 exact / 4 expected-missing / 0 divergent`；`189/407` 不因交付速度、文件存在、
  DTO/helper 或零 Java 证据上调。
- `AutoCombatService`：父级按 27 个完整方法和真实 `AutoBattleTask -> AutoCombat -> BattleRadar` 链
  `SOURCE APPROVED，P0/P1/P2=0`；仅保留两个已批准 UI_CLEAN port substitution。下一项已直接续派
  `TaskMaintenanceService + SummonSkillService` 两整类 whole-pass chain。
- `DialogService`/DIALOG_DETECTION：B 的三文件 Repair #1 已闭合 fixed identity、baseline stop/finally、
  wait/hide 后 live HWND refresh、maintenance public caller 与 decoded-image ownership，父级判返修源码
  `APPROVED，P0/P1/P2=0`；真实 handler runtime owner 尚未接线，因此整条仍 integration-pending。
- `NpcClickService` Ctrl-probe mechanics：A R2 因 expected-template generic OPTION 绕过/raw-frame matching 与
  黄字 mask 缺 696 OpenCV cleanup/polarity而 `BLOCKED，P1=2`，原 A 负责 R3 exact-baseline repair。
- `DialogChoiceMemoryService + WorldMapRouteResultMemoryService + MemoryService`：源码内容与 696 一致，
  `CloudServiceConfiguration` 唯一注入两个 tenant/user scoped private paths，父级
  `SOURCE APPROVED，P0/P1/P2=0`；等待统一 fresh Cloud package。
- 下一任务 helper 只允许给空闲 B/C 生成现有 typed producer 已真实存在的完整可达 Service cohort；缺 observation
  contract 的候选继续标 blocked，不用小单填空。无已批准业务差异；按 `696a12b0` 等价迁移。

## 2026-07-14 15:40 - BattleRadar Cloud 源码通过，四路真实 mechanics 并发

- Cloud `BATTLE_RADAR_*` 七 kind/三 fact 合同与 A 的 `BattleRadarService` R2 均经父级源码复核
  `P0/P1/P2=0`。Service 保持 696 的 Stage 1/2/3/4、两次 miss、15s/1s/4s、poll cadence、enter/exit/state，
  七种请求使用七个稳定 slot；task-entry exact context producer 尚未接通，故整类仍 `INTEGRATION PENDING`。
- DHXY BattleRadar producer 首版 `BLOCKED，P1=3`：selection/top 必须恢复真实 OR/AND 短路，capture exception
  必须映 MECHANICS_FAILED，永久 template cache 必须删除。原 D 已领取一文件 R1；Cloud/DHXY fact/handler 其余
  部分可保留。
- TeamReturn leader live fact 的三 caller 与 baseline wait 顺序保留；B R1 把 checkpoint 限定到 STOPPED 后父级
  `SOURCE APPROVED，P0/P1/P2=0`。B 随即领取 TaskTracker exact-window narrow/expanded/必要 drag 后同帧 panel
  capture mechanics。
- A 已领取 NpcClick Ctrl-probe 单 input-worker 连续 mechanics；C 的 first-aid 首版因合并 no-focus probe 与
  heal-all direct、缺 input-worker 门、新增目标间 stop gate、结果顺序/状态失真而 `BLOCKED，P1=3/P2=2`，原 C
  已领取 R1 恢复 696 两条原路径。四路写集互斥，Java 稳定前不构建，approved count 保持 `189/407`。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线等价迁移。**

## 2026-07-14 14:57 - BattleRadar 双侧 typed fact cohort / NpcClick 整类在途

- `BattleRadarService` 首版整类适配经父级独立审查为 `BLOCKED，P0=0/P1=3/P2=1`。开放项是恢复
  baseline 三个 public 签名和 18 个方法职责、在 Stage 1/2/3/4 原调用点按需读取 closed fact、使用显式
  per-run `TaskExecutionContext`，并禁止 `"default"`/global state fallback。
- External C 已领取 Cloud `BATTLE_RADAR_*` 7 kind + signal/minimap/avatar 3 fact 合同；External D 已在
  TRUE EOF 收到 DHXY 镜像 fact、exact-window mechanics 与 handler producer 实现单。图像、模板、minimap
  与 avatar baseline 均留 DHXY，Cloud 只消费 closed typed observation。
- `PlayerStateService` 与 `DialogService` 当前仍与 `696a12b0` byte-exact，因真实 typed producer/local macro
  前置缺失而保持 `PREREQUISITE BLOCKED`，没有以零 Java 或伪结果计整类完成。External B 已领取并继续
  `NpcClickService` 整类适配。
- 四路 Java 稳定前不运行构建；approved same-path 保持 `189/407`。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线等价迁移。**

## 2026-07-14 14:12 - LeftTop/TeamReturn 源码通过，CommonBox 返修，TaskTracker 双侧前置重排

- External A 的 `LeftTopStatusSwitchService` Repair #1 已父级 `SOURCE APPROVED，P0/P1/P2=0`：fact/input
  terminal、interrupt unwind、诚实空 rect 与原 move/click bundle 顺序闭合，等待统一 Cloud package。
- External D 的 `TeamReturnService.clickReturnTeamIfPresent` 已父级 `SOURCE APPROVED，P0/P1/P2=0`：两次
  fresh button fact、香检查位置、第二次点位、随机偏移、两步 bundle、timestamp/log 与 terminal 路径均对齐
  `696a12b0`；不计整类完成。
- External C 的 `CommonBoxService` 首版为 `BLOCKED，P1=3/P2=1`，由原 C 返修 terminal 分流、以本地真实
  `matchedAtEpochMs` 锚定既有 30 秒 TTL，并恢复 baseline 原方法图；不内部接管。
- External B 的单文件 TaskTracker rect 单因 fact 不可达、坐标与同帧 artifact 不闭合而 supersede；首个
  TaskMaintenance 替代候选又因 Cloud context holder 无 producer 在发单前撤销，Java 零改动。排班 helper 正准备
  B/D 双侧 TaskTracker cohort：DHXY 做 exact-window 定位/必要拖拽/同帧 capture，Cloud 保留全部算法。
- Navigation X2 nested-exclusive `P1=1` 仍开放；approved same-path 保持 `189/407`，writer 稳定后再跑 fresh build。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线等价迁移。**

## 2026-07-14 12:43 - `UI_CLEAN` DHXY wire source-approved，B 依赖解除

- External D 已交付 DHXY remote 2 New + 5 Modify。父级逐文件复核 command/result closed enum-state matrix、
  exact 四键 terminal、all-terminal `OBSERVED` 拒绝、非 EXECUTED 三字段显式 null，以及 request/outcome nested
  canonical tree；既有 BAG/NAV 分支均保留，`git diff --check` 无错误。结论
  `SOURCE APPROVED，P0/P1/P2=0`。
- 父级已在 External B 固定日志追加 dependency release；B 不再等待 D DTO，可直接实施唯一 handler 文件。
  D 的 source approval 不替代 C 的 Cloud 镜像 parity 与最终 DHXY compile / Cloud package。
- External C 已领取后被明确要求选择“现在完整实现”，一次完成 3 New + 7 Modify；禁止 Worker 自跑 build/test
  只是并发协调门，不冻结 Java。A 继续等待 C 的 `CloudUiCleanerPort`。

approved same-path 仍为 `189/407`；无已批准业务差异。

## 2026-07-14 12:48 - `UI_CLEAN` handler 首版逻辑通过、注释归属 P2 返修

- External B 已交付 DHXY `LocalRemoteGameCommandHandler.java`。父级确认三种 self-queued operation 在
  input queue 外用 exact-context `callWith`，X2 direct operation 在既有 remote exclusive callback 内，且
  deadline/pause/safety/runRevision fence、4 operation / 7 state、四键 terminal 和 terminal 分流均符合合同。
- 唯一问题是原 `NAVIGATE_IN_CURRENT_MAP variant` JavaDoc 被新 UI_CLEAN block 隔在 Navigation 方法之外；
  `BLOCKED，P0=0/P1=0/P2=1`。原 B 只需恢复两个 JavaDoc 与各自方法的直接相邻关系，禁止改运行逻辑。
- D source approval 已在 D 日志真实 EOF 再确认；C 仍在实施 3 New + 7 Modify，A 等待 C facade。

approved same-path 保持 `189/407`；无已批准业务差异。

## 2026-07-14 12:51 - C facade 落盘，A caller 依赖解除

- External C 已实际连续写入 `UI_CLEAN` Cloud contract；`CloudUiCleanerPort.java` 于 `12:50:56` 落入约定
  remote 路径。父级因此在 A 固定日志真实 EOF 解除依赖等待，A 可直接实施三个 Service 的八个 caller 替换；
  A/C 写集互斥。
- C 尚未追加 `Implementation #1`，当前只确认依赖文件存在，不将其视为源码审批。B 仍只返修父级 P2
  JavaDoc 归属，D 已 source-approved。Java writer 未稳定前不并发运行 Maven。

approved same-path 保持 `189/407`；无已批准业务差异。

## 2026-07-14 12:37 - Phase 3 ReturnItemPrescan 通过 / `UI_CLEAN` 四路全部领取

- Internal CK3 已按父级返修条件删除 `ReturnItemPrescanService` 的非基线 public `hasCached`，保留已批准的
  exact-context identity 表示适配与三个 `BAG_RETURN_ITEM` macro 替换。父级复核 active blob
  `61b6190f0ab5e49b82ed8c6281ffc619e66b03e5`、SHA-256
  `3d78417e2834ad332fce26037e72116224ffc4727914c732287299caf81e21bd`、7/7 public 方法与 operation 唯一性，
  结论 `APPROVED，P0/P1/P2=0`；CK3 已关闭。
- `UI_CLEAN` 四个 External 实现单均已在领取截止前追加真实 EOF `CLAIMED`：A `12:33:59` 只改 Cloud
  三个 Service caller，B `12:36:26` 只改 DHXY handler，C `12:37:24` 只改 Cloud closed contract/facade/wire，
  D `12:35:27` 只改 DHXY payload/codec/digest。A 等 C facade、B 等 D payload，写集互斥且没有内部接管。
- 合同继续限定四个 operation、closed terminal state、flat `macroKind/operation/state/cachePoint` 四键 payload
  和 `cachePoint=null`。前三种本地方法在 input queue 外以 exact context 调用，
  `CLOSE_MAP_SEARCH_INPUT_BY_X2` 只在既有 remote exclusive callback 内调用 direct 方法。
- 四路 Java 尚在写入，本轮不并发运行构建；approved same-path 保持 `189/407`。待逐项父级源码审查通过后，
  统一运行 Cloud `mvn -q clean package` 与 DHXY `mvn -q -DskipTests compile`。

**无已批准业务差异；按 `696a12b0` 原字节完整 Service 基线推进。**

## 2026-07-14 12:00 - whole-Service 32/32 完成、四个本地重复类已删除、Phase 3 开始

- External C 已完成 `NpcClickService.java` preservation-gated 原字节 promotion，父级结论
  `APPROVED，P0/P1/P2=0`。最终 active 审计为 `TOTAL=32 EXACT=32 MISSING=0 DIFF=0`，至此
  `696a12b0` 的完整 32-Service 调用图已先进入 active Cloud，Runner 仍为 0。
- 父级删除前逐类复核 blob，只从 active Cloud 删除用户指定永久本地的 `BagService`、
  `UICleanerService`、`GiveItemService`、`QuestManagerService`。Cloud 证据镜像和 DHXY 真正实现均保留；
  当前 active 对镜像为 `28 exact / 4 expected-missing / 0 divergent`。
- fresh Cloud `mvn -q clean package` exit 1。错误不是业务逻辑缺失证明，而是完整类第一次同时暴露其依赖：
  passive shared DTO/config/model、四个本地 Service 调用边界、以及待 Phase 4 抽离的 HWND/window/capture/
  template/OCR/input collaborator。Phase 3 先原字节补 passive 类型，再补四个最小 typed local boundary；
  不复制桌面 mechanics、不缩短 Service、不提前改动作顺序。
- approved same-path 保持 `189/407`。Phase 2 完成与依赖类型复制均不单独计整类完成。

## 2026-07-14 16:31 EDT - 完整链批次替代快速小单

- Phase 1/2 已完成：Cloud 先保全 `696a12b0` Service `32/32 exact`，再只删除四个永久本地重复类；active
  审计仍为 `28 exact / 4 expected-missing / 0 divergent`。这一事实与 private helper/DTO 数量无关。
- A 的 Npc Ctrl-probe R1 为 `BLOCKED，P1=5/P2=2`，等待 B/C local prerequisite；A 已收到互斥 3 文件
  `AutoBattleTask + BaseTaskTemplate + TaskStepExecutor` 完整 context/lifecycle chain，不再以单 leaf 充当进度。
- B 的 Dialog detection 为 `BLOCKED，P1=1/P2=4`，原 B 修 exception/unavailable、window-scoped debug、owned
  image 与 closed record；C 的 FirstAid 已通过并领取 4 文件 OCR -> incense observation cohort；D 的
  StoryAdvance 为 `BLOCKED，P1=1` 且已领取 exact-binding 输入前门返修。
- 后续外部任务默认必须为 2-5 Java 文件的 reachable caller -> same-path logic -> typed local terminal cohort，
  或完整大型类/生命周期。单 DTO/helper/一行替换不增加 approved same-path；当前仍为 `189/407`，等待源码终审与
  writer 稳定后的 fresh 双构建。无已批准业务差异；按 `696a12b0` 等价迁移。

**无已批准业务差异；按 `696a12b0` 原字节完整 Service 基线推进。**

## 2026-07-14 19:04 EDT - PlayerState 双侧整链在途，Summon whole-pass 源码通过

- active 事实仍为 `28 exact / 4 expected-missing / 0 divergent`；approved same-path 仍为 `189/407`。
  领取或快速交付本身不计进度，只有 complete caller -> same-path Service -> typed local terminal 与 fresh 构建
  同时通过才允许上调。
- External A 的 Npc Ctrl-probe R3 已父级 `SOURCE APPROVED，P0/P1/P2=0`，但仍只算 `NpcClickService`
  的本地 mechanics 前置；A 写集释放，下一单必须是与当前三路互斥的完整大型 Service/生命周期或实质 mechanics
  cohort，不再以 DTO/helper/no-op 填槽。
- External B/C 已领取 PlayerState first-aid 完整双侧 active chain：B 为 DHXY 9 Java closed
  command/result/codec/digest/handler/mechanics，C 为 Cloud 10 Java mirror contract/port/terminal 与
  `AutoBattleTask/AutoCombatService -> PlayerStateService` caller。两侧尚未交付 Implementation/Repair。
- External D 的两 Service whole-pass Repair #1 经 helper 预检后由父级独立判
  `REPAIR SOURCE APPROVED，P0/P1/P2=0`：exact current context、单次 existing capability、四 intent、九 result、
  五枚举、四 terminal 与 baseline finally/state/order 均闭合。等待 B/C writer 稳定后的 fresh Cloud package。
- 两个 internal helper 仅做非绑定交付预检与 READY_NOW 排班；父级是唯一 reviewer，不用 helper 的建议替代源码
  裁决。无已批准业务差异；按 `696a12b0` 等价迁移。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-15 05:10 EDT - whole Task source preservation

- `WubeiTask`, `FiveRingTaskV2`, and `XiuluoTaskV2` now exist in active Cloud as byte-exact `696a12b0`
  whole-class copies. This restores source completeness but does not advance the hard ledger.
- Their execute units remain `BLOCKED_MISSING_TYPED_BOUNDARIES/countDelta=0` until task runtime/turn/event and
  retained local mechanics are connected through existing single typed owners.
- Current Internal count units are `NavigationService::observeRoutePlanFacts`,
  `NpcClickService::pollFreshStoryBlockerEvent`, and `UICleanerService::cleanLightweightInterruptions`.

**无已批准业务差异；按 `696a12b0` 完整 Task/Service 基线推进。**

## 2026-07-15 05:03 EDT - missing Task caller recovery

- `FiveRingPhase::isTerminal` and `WubeiPhase::isTerminal` are not countable yet because active Cloud lacks their
  production Task loops; the attempted units remain `countDelta=0`.
- Three disjoint whole-task units are now active: `WubeiTask::execute(TaskExecutionContext)`,
  `FiveRingTaskV2::execute(TaskExecutionContext)`, and `XiuluoTaskV2::execute(TaskExecutionContext)`. Each owns
  one complete baseline Task file and must retain its entire public/private phase graph and closed result path.
- Hard ledger remains `189/407`; source-approved pending-build pool remains `50`.

**无已批准业务差异；按 `696a12b0` 完整 Task/Service 基线推进。**

## 2026-07-15 04:40 EDT - count-unit checkpoint

- Approved same-path hard ledger remains `189/407` pending fresh Maven; deduplicated
  `SOURCE APPROVED / COUNT PENDING BUILD` pool is `50` after six new parent approvals.
- `PlayerStateService::ensureSheYaoXiangActive` is not counted: the current Cloud implementation still performs
  mouse-away on the Cloud host before typed capture. It is parked as `P1=1/countDelta=0` until the same unit can use
  the existing exact-binding DHXY incense observation mechanics after C's shared lane stabilizes.
- Active count lanes: `AutoBattleTask::isFollowerSupportMode`, `WubeiPhase::isTerminal`,
  `TaskTrackerPanelService::read`, `XiuluoPhase::isTerminal`,
  `SummonSkillService::handleBusinessDialogDuringSkillClean`, `FiveRingPhase::isTerminal`, and
  `WubeiRoundContext::next`. Each remains a unique `countDelta=+1` unit and must prove an active caller and closed
  terminal before parent approval.

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-15 01:30 EDT - 十三项待构建，Navigation current-map 未接 active macro

- approved same-path ledger 仍为 `189/407`；父级源码通过的 count-pending-build 单位现为 13 个，统一 fresh
  Cloud package / DHXY compile 通过后才按批准顺序原子递增。
- `BattleRadarService::checkFastExpectedCombatExitByAvatarDiff` 与
  `LeftTopStatusSwitchService::handleCombatMaintenance` 的 real caller、typed DHXY mechanics、closed terminal/state
  已父级确认 `P0/P1/P2=0`。
- `NavigationService::navigateInCurrentMap` 报告引用了已注册 local macro，但 active Cloud 方法没有调用该 macro，
  因此父级判 `BLOCKED P1=1`，不得把旁路 handler 证据计作 caller chain。七条实现线已重新排成互斥 +1 单。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-15 01:19 EDT - 七条 count unit 续派与九项 build-pending

- approved same-path ledger 仍为 `189/407`，不以 source-only 提前记账。新增父级源码通过：
  `NavigationService::navigateToNPC`、`PlayerStateService::performStartupFirstAidCheck`、
  `TeamReturnService::clickReturnTeamIfPresent`、`ClientIdentityService::scanAndSyncIdentity`；与既有五项合计九项
  `COUNT PENDING BUILD`。
- 当前 External count units：A `AutoCombatPanelService::alignPanelIfNeeded`，B
  `DialogService::handleDialog`，C `BattleRadarService::checkFastExpectedCombatExitByAvatarDiff`，D
  `NavigationService::navigateInCurrentMap`。当前 Internal count units：I1
  `CommonBoxService::detectMemberBoxAfterCombatExit`，I4
  `LeftTopStatusSwitchService::handleCombatMaintenance`，I6
  `ClientIdentityService::resolveCurrentWindowTitle`。七项写集互斥；每项父级源码审查 + fresh build 同轮才 `+1`。
- `AutoCombatPanelService::verifyAndAlignPanel` 因 rounds shared fact 缺口保留 `BLOCKED P1=1`；
  `AutoCombatService::handleCombatTick` 因 panel 与 incense typed terminal 缺口保留 `BLOCKED P1=2`，不让 A/C
  等待、不内部接管、不拆 DTO/helper 填充任务。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-15 00:24 EDT - `189/407` 计数型任务门

- 用户明确否决前置合同、DTO、helper、单个 adapter 完成但计数不变的派单方式。从本段起，每张新实现单必须
  唯一绑定矩阵中的一个 `countUnit`，声明 `countDelta=+1`，并包含使该单位从真实 public caller 经 Cloud
  Service、typed DHXY primitive/mechanics 到 closed terminal/result 可达的全部必要工作。
- 父级源码审查和适用 fresh Maven 门是同一完成条件；通过当轮 ledger 原子执行 `before -> before+1`。文件存在、
  source-only、DTO/codec/handler、helper/mechanics 或 compile-only 都不能单独写完成，也不能继续产生零计数 follow-up。
- 发现写集外前置时必须 `BLOCKED`，不得改成 stub、伪 terminal 或另拆零计数 filler。旧 A/C/D 合同返修只作一次性
  遗留收口；放行后的所有后续任务受本门约束。
- 实现并行固定为 External A/B/C/D 四线加 Internal I1/I2/I3 三线；审核/排班 helper 可另行运行但不占实现线，
  不替代父级最终裁决。当前七个目标计数单位为：
  `BattleRadarService::checkAndSyncCombatState`、
  `DialogService::prepareWhiteStoryTemplateOrAbsent`、
  `PlayerStateService::ensureSheYaoXiangActive`（C 遗留 R2 放行后）、
  `NavigationService::navigateToNPC`、
  `CommonBoxService::consumePendingBoxIfAllowed`、
  `TeamReturnService::waitForMembersReturnIfNeeded`、
  `TaskMaintenanceService::runOpportunisticMaintenance`。
- 当前 ledger 仍为 `189/407`；下一次任何“完成”必须同时把它改为 `190/407`，此后逐项 `+1`。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 23:47 EDT - C option-OCR 放行，shared-only 串行取代 whole-chain 串行

- C option-OCR R2 已父级 `SOURCE APPROVED，P0/P1/P2=0`；optional SHA、green/yellow wash 异常降级、
  handler supplied-SHA 门与 command/result 合同均闭合，C 原 7 文件释放。
- 父级确认 A/B/D 完成旧单后分别空转约一小时。排班由“整条 caller chain 等 shared slot”改为“只有 generic
  shared 12 文件串行”：B 直接实施 white-story 完整 18 Java 双端链；A/C/D 并行实施 tooltip+prepared-point、
  yellow-target、player-anchor 的 10/5/5 个专用合同文件，彼此及 B 写集互斥。
- 三个专用合同 cohort 只作为后续完整链的大块前置，不增加整类计数；B 释放后由单一 integration 波闭合 shared
  registration/codec/digest/handler 与 Cloud `NpcClickService` caller。active 仍为
  `28 exact / 4 expected-missing / 0 divergent`，approved same-path 仍为 `189/407`。
- A/D 首次 brief 的历史中段误插不构成领取门，真实 EOF 已权威重发；External 任务逾期只原样重发同一 Worker，
  不内部接管。A/B/C/D 已于 `23:49:09/23:49:40/23:50:00/23:52:00` 全部真实 CLAIMED；writers 稳定前
  不运行 Maven。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 22:26 EDT - A/D 完整本地 mechanics 定点返修通过

- A task-tooltip：每次 `findImagesInRegion` 后先重验同 normalized HWND、有效且相同 geometry，再进入
  empty/nonempty 分支；nonempty 只用该 post-capture binding 计算 learned ROI。input-worker、0.82/36、
  move/click/delay/verify、Y+90/ROI、七态均父级确认未漂移，`SOURCE APPROVED，P0/P1/P2=0`。
- D player-anchor：六个 OpenCV owner 均 null-first、try 内逐项 acquisition、finally nonnull 恰一次 release；
  inclusive blob 的 right/bottom 对 exclusive scanRect 使用 `>=` 拒绝上界，settle 注释与 false->INTERRUPTED
  行为一致。其余 interruption/capture/mask/HSV/blob/evidence/Cloud-local 边界冻结，`SOURCE APPROVED，P0/P1/P2=0`。
- A/D 仍只是完整 local mechanics，未闭合 caller/typed transport，不增加 `189/407`。共享次序继续为
  `C option-OCR -> B white-story -> D player-anchor caller -> A tooltip caller`；C 当前实施完整同帧 option-OCR
  双仓链，B 等待 shared slot，writers 稳定前不运行 Maven。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 21:19 EDT - C white-story R3 通过，A prepared-point 定点返修

- active Service 审计保持 `28 exact / 4 expected-missing / 0 divergent`，approved same-path 保持 `189/407`。
- C white-story R3 已父级 `SOURCE APPROVED，P0/P1/P2=0`：selected supplied/fallback frame 总是写入新的
  window-scoped raw artifact，evidence、wash 与 template match 同帧；C 已领取 Dialog option OCR words 单色
  variant 的完整本地 observation mechanics。
- A prepared-point mechanics 的 direct input-worker move/click/delay/verify 与零 queue-in-queue 已保持，但
  intent 允许 `maxRetries>=2`，超过 696 四个真实 caller 的 0/1 输入集；result 也未锁 terminal/clickProduced
  组合，父级判 `BLOCKED，P1=1/P2=1`，原 A 只修同一文件。
- B 20 Java Dialog validation 双端真链和 D yellow-target R1 已交付，helper 正做非绑定预检；父级独立终审和
  所有 writer 稳定前不运行 fresh 构建。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 21:06 EDT - A OCR-image 源码通过，C/D 同文件返修

- active Service 审计保持 `28 exact / 4 expected-missing / 0 divergent`，approved same-path 保持 `189/407`。
- A `DialogOptionOcrImageLocalObservationMechanics` R3 已父级 `SOURCE APPROVED，P0/P1/P2=0`，并续派
  prepared-point click/verify 完整本地 continuous mechanics。
- C white-story R2 因 supplied path/evidence 像素权威可跨帧而 `BLOCKED，P1=1/P2=1`；D yellow-target I1
  因 default-mask/skip、refresh terminal、单入口 interruption 与 result invariant 而
  `BLOCKED，P1=3/P2=1`。两者均只返修原文件，不新增 Design/wire。
- B 的 20 Java Dialog validation caller-to-terminal 双端链继续在途；A/C/D 写集均不触 B shared family。
  所有 writer 稳定后才运行 fresh Cloud package 与 DHXY compile。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 20:22 EDT - A/D 大 mechanics 通过，B 真链扩为 20 文件继续

- active Service 审计仍为 `28 exact / 4 expected-missing / 0 divergent`；approved same-path 仍为
  `189/407`。
- A AutoCombatPanel rounds 连续观察与 D Dialog green-template R3 均父级
  `SOURCE APPROVED，P0/P1/P2=0`。A/D 已分别续派完整 Dialog option OCR image preparation / NPC
  yellow-target observation mechanics，写集互斥且均不触 shared wire。
- B 的 Dialog prepared-action validation 完整双端链实施中确认 DHXY 缺四个 696 image/fingerprint 方法；
  父级已在同一任务加入 `ImagePreprocessor.java`，只允许 exact add-back 四方法。该链现为 20 Java 文件，仍须
  一次交付 caller -> Cloud Service -> port/transport -> DHXY handler/mechanics -> typed terminal -> return。
- C white-story mechanics 已领取在途；writers 稳定前不并发构建，不以 mechanics/source prerequisite 提升计数。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 20:14 EDT - PlayerState 双侧源码通过，下一波含 19 文件完整双端链

- A AutoCombatPanel visibility/align R1 与 B/C PlayerState first-aid 双侧源码均父级
  `SOURCE APPROVED，P0/P1/P2=0`；fresh 双构建前 approved same-path 仍为 `189/407`。
- D green-template nullable candidate continuation 已通过，剩余两个 P2 只涉及 supplied frame/rect 尺寸绑定与
  evidence decoded image flush，由原 D 同文件返修。
- B 下一单为 Dialog prepared-action validation 19 Java 文件完整
  `Cloud caller -> Service -> port/contract -> DHXY handler/mechanics -> terminal -> Service return` 链；A/C
  分别并行 rounds/white-story 完整连续 mechanics。下一波不再是四个 prerequisite 小单。
- active Service 审计保持 `28 exact / 4 expected-missing / 0 divergent`；writer 稳定前不运行并发 build。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 20:10 EDT - PlayerState 半链通过，AutoCombatPanel/Dialog 继续精确基线返修

- active Service 图保持 `28 exact / 4 expected-missing / 0 divergent`；approved same-path 保持 `189/407`。
- PlayerState first-aid：DHXY producer/result/codec/digest/handler/mechanics 半链已父级 `SOURCE APPROVED`；
  Cloud 半链仅剩 constructor 多加的 `observedBaseY != -1` 门，原 C 删除后再做跨仓终审。完整 caller 到
  terminal 与 fresh 双构建通过前不计整类完成。
- AutoCombatPanel：A 的 visibility/align 连续 mechanics 已覆盖三 operation、anchor/green fallback、Alt+8、
  drag 与 typed terminal，但首次 capture-unavailable fallback 和 drag settle false 后复查仍与 696 不同，R1
  由原 A 修同一文件。
- Dialog green-template：supplied frame、click fresh capture、unreadable continuation、fresh geometry、best
  diagnostic 与 evidence invariant 已保留；R2 只补 null spec/null path/invalid-path 三种 caller-order continue。
- 下一波矩阵排班至少包含一条完整双端 reachable chain，禁止四路均只迁 helper/DTO/local prerequisite。
  Java writers 稳定前不运行并发 build。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**

## 2026-07-14 19:38 EDT - 四路大 cohort 首版全部完成父级基线审查

- active 仍为 `28 exact / 4 expected-missing / 0 divergent`，approved same-path 仍为 `189/407`；本轮不按
  A/D 单大类或 B/C 9+10 文件数量计完成。
- A story-objective mechanics：`BLOCKED，P0=0/P1=3/P2=1`；原 A 已领取同文件 R1，修 fresh geometry、
  detection-frame fallback、基线中断时点与 image-result invariant。
- B PlayerState DHXY half：`BLOCKED，P0=0/P1=1/P2=2`；原 B 已领取 R1，修 input-worker 内 HEAL fresh
  geometry 与两端 constructor parity。C 审查触发同波 scope amendment：PROBE 必须返回与 bars frame 同一次
  geometry 的 `observedBaseX/Y`，并严格锁定四 bar identity/order。
- C PlayerState Cloud half：`BLOCKED，P0=0/P1=1/P2=1`；移除独立 pre-probe GEOMETRY fact，从 typed
  PROBE result 建 stored plan base，未知/重复/错序 bar 不得静默映为宝宝法力。
- D green-template mechanics：`BLOCKED，P0=0/P1=4/P2=2`；必须分别复现 696 prepare/click 两条 capture
  时序，恢复 supplied detection、坏模板继续后选、fresh exact binding、best-match 诊断与 evidence 自校验。
- Summon whole-pass 既有 `SOURCE APPROVED，P0/P1/P2=0` 不回退。A/B/C/D writer 稳定前不跑 clean；
  Delivery Preflight Helper 继续只给非绑定候选，Next-Task Queue Helper 已备好 B 下一条完整 mechanics 单。

**无已批准业务差异；按 `696a12b0` 完整 Service 基线推进。**
## 2026-07-15 22:05 EDT - CR271 TURN-20/24A 父级审查

- TURN-20 `P0/P1/P2=0/2/0`：已知 input FAILED 必须保留 `696a12b0` 的 open-null、refresh-false、drag 后
  re-observe/fallback；Cloud OCR 必须复用 canonical `LocalOcrClient`，不得在 Service 内新建第二 HttpClient/codec。
- TURN-24A `P0/P1/P2=0/1/0`：confirmed STOPPED/interrupted 必须经 `TaskCheckpoint` 传播，不能包装成 radar
  unavailable；未确认 stop 仍保守保持 IN_COMBAT，不新增 capture/retry。
- TURN-23 增加 TURN-09R 前置，确保 first-aid 多点击 closed action 只产生一次全局 input-queue submission。
> 2026-07-16 13:46 EDT：TURN-28Q Repair #5 父级 Review #10 为 `0/1/0`，完整卡退 A Repair #6：
> Unsafe/private reflection/polling 已清零，但冻结 public resolver->real queue/worker callback 覆盖被
> direct-context 绕过，须合法恢复。TURN-28S2 R1/R2 最新轮均 Approved、双审 `2/2`；TURN-34AT1 R1
> Approved、fresh R2 reviewing；TURN-34BP2 仍为 C sole-writer WIP。无已批准业务差异，基线 `696a12b0`。
>
> 2026-07-16 13:52 EDT：A 已 canonical 归还完整 TURN-28Q Repair #6（零本轮字节）；父级把同一完整卡
> 改派空闲 External B。B claim 后承担全部四文件/test/report/返修，不拆卡；验收仍是合法恢复五个
> public resolver->real queue/worker + exactly-one-refresh，且 Unsafe/private reflection/source scan/polling 为零。
>
> 2026-07-16 13:54 EDT：TURN-34AT1 Repair #4 fresh R1/R2 最新轮均 Approved，双独立整卡 review
> `2/2`；冻结 production/test SHA 不变。仅剩 stable-writer named test/Cloud compile，C 写 BP2 期间不跑 Maven。
>
> 2026-07-16 13:58 EDT：B 在 claim 前拒绝完整 TURN-28Q Repair #6，零 owner/零字节；同一完整卡已
> 改派 External D。仍须恢复五个 public resolver->real queue/worker + exactly-one-refresh，不拆卡。
>
> 2026-07-16 14:06 EDT：D 已 canonical claim 完整 TURN-28Q Repair #6。C canonical 交付完整
> TURN-34BP2 后，父级 Review #1 为 `P0/P1/P2=0/1/0`：四 typed shared maps、19 public 与业务边界已接受；
> 但非空 effective context 缺 scope/invocation 时仍降级为共享 `ExecutionScope.NONE`，违反仅 null 参数 + empty
> holder 可用 no-context key 的冻结合同。完整 BP2 退同一 C Repair #1，不拆卡。无已批准业务差异，基线 `696a12b0`。
>
> 2026-07-16 14:12 EDT：TURN-34BP2 Repair #1 父级 Review #2 `P0/P1/P2=0/0/0`。非空
> effective context 缺 scope/invocation 已改为 fail closed，`ExecutionScope.NONE` 仅 supplied-null +
> empty-holder 可达；完整 typed-key/public/business 边界保持，C owner 释放，转双独立整卡 review+build pending。
>
> 2026-07-16 14:19 EDT：TURN-28Q Repair #6 父级 Review #11 `P0/P1/P2=0/0/0`，五个 public resolver ->
> real queue/worker、单次 refresh、合法构造与禁用模式闭合，D owner 释放，转双 review+build。TURN-34BP2
> R1 Approved、R2 Blocked，父级裁决 R2 的 no-session formal window isolation P1 成立；完整 BP2 退同一 C
> Repair #2，以 typed explicit-session-or-exact-window discriminator 修 formal state/claim namespace，不拆卡。
>
> 2026-07-16 14:26 EDT：External C 已 canonical claim 完整 TURN-34BP2 Repair #2，起始 SHA
> `d97e1572...` 未变；整卡修 scope 后 typed explicit-session-or-exact-window formal coordination address。
> Q 双独立整卡 review 仍在进行；C writer 活动期间不运行 Maven。
>
> 2026-07-16 14:29 EDT：TURN-34BP2 Repair #2 父级 Review #4 `P0/P1/P2=0/0/0`；latest SHA
> `8d79d198...`。typed session-or-window discriminator 已一致贯穿 formal state/claim/prune，C owner 释放，
> 等 Q reviewer 释放 Internal 两槽后启动 BP2 fresh 双独立整卡 review+Cloud compile。
>
> 2026-07-16 14:30 EDT：TURN-28Q Repair #6 R1/R2 latest 均 Approved，双整卡 review `2/2`，仅剩
> named test/DHXY compile。Internal 两槽已启动 BP2 Repair #2 latest-SHA fresh R1 Rawls/R2 Galileo。
>
> 2026-07-16 14:35 EDT：TURN-34BP2 Repair #2 fresh R1 Rawls/R2 Galileo latest 均 Approved，双整卡
> review `2/2`；冻结 production 1,400 行 / SHA `8d79d198...`。Internal 已回到 `0/2`，BP2 仅待
> stable-writer Cloud compile，尚非 CARD APPROVED。
>
> 2026-07-16 14:40 EDT：stable-writer build gate #1：DHXY main compile 通过，但授权 named tests 被共享
> stale test-source 的 reactor-wide testCompile 挡住；Cloud main compile 被未完成整卡迁移的
> Wubei/Navigation/NpcClick/Dialog/PlayerState 源码挡住。已通过卡不退修，阻断归对应计划内完整卡。
>
> 2026-07-16 15:31: TURN-34B complete-card sole owner C remains source-active; named test is now 161 lines / `9721e2e0...`, production remains accepted `8d79d198...`. No delivery/review yet.
> 2026-07-16 15:32: TURN-34B named test is 164 lines / `9770816d...`; whole-card C ownership remains source-active, not delivered.
> 2026-07-16 15:41: TURN-34B named test is 203 lines / `3b7c4531...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 15:46: TURN-34B named test is 269 lines / `cca30a77...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 15:51: TURN-34B named test is 305 lines / `b20e06df...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:01: TURN-34B named test is 401 lines / `298a0554...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:06: TURN-34B named test is 480 lines / `36bf7da3...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:11: TURN-34B named test is 564 lines / `f8b38cac...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:16: TURN-34B named test is 638 lines / `f87a3ced...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:21: TURN-34B named test is 702 lines / `00c188fb...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:26: TURN-34B named test is 753 lines / `d732ca08...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:31: TURN-34B named test is 812 lines / `a57bb165...`; C remains whole-card source-active sole owner, not delivered.
> 2026-07-16 16:36: A/B/D have no discoverable active implementation task; lane text is not a worker. TURN-34B test is 816 lines / `5c987d4f...`, still C whole-card WIP.
> 2026-07-16 17:31: TURN-34B canonical whole-card delivery received Parent Review #1
> 2026-07-16 18:24: TURN-34B Repair #1 canonical delivery received Parent Review #2 `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`; production remains 1,400 lines / `8d79d198...`, sole named test is 1,377 lines / `471cd324...` with 52 tests. External A's 18:00:20 claim is the valid first claim; External d's later duplicate claim is revoked. No extra reviewers; named test/Cloud compile remain pending.
> 2026-07-16 18:49: parent repaired the TURN-26/28 plan-contract cycle without dispatching a Worker. TURN-28 keeps External C as active whole-card owner, drops TURN-26 as a source-start prerequisite, and must publish the canonical typed objective result plus exact-window pending proof-token read API. TURN-26 is zero-owner and waits for that source gate; its repaired write set adds exact-window prepared-action state plus both stale constructor tests and the named contract test. HTTPS coordinates remain unscaled screen-absolute, so fixed `bottom-40` is accepted and no DPI field is added.

> 2026-07-17 01:58 CR271 status refresh: TURN-26 Repair #2 resumed real test-source writing under External B
> (`DialogOptionTurnContractTest` 1,916 lines / `d208c1d2...`). No canonical delivery or parent source review exists;
> communication remains stale. TURN-27 and whole Tasks 35/36/37 continue to wait for TURN-26 source pass.
> 2026-07-16 19:00: ownership correction: External A currently owns only TURN-34A (claimed 18:24:36). TURN-34B's accepted A snapshot `471cd324...` was overwritten by revoked/never-owner d to `0edfb55c...`; TURN-34B is therefore post-review byte-drift repair-blocked with zero owner, is not open to another Worker, and waits until A releases TURN-34A before the original delivery owner may self-claim the same-card repair. No dispatch and no Java write occurred.
> 2026-07-16 19:08: TURN-34A whole-card delivery Parent Review #2 is `P0/P1/P2=0/3/1 / REPAIR #1 REQUIRED`. Production remains frozen `532e6f84...`; test `8133f2db...` copies caller reducers instead of executing four real callers, explicitly does not execute required 15s/4s/40s/10s expiry branches, and omits the named-test legacy source gate; delivery also misstates public reflection as zero reflection. Same complete card stays with External A; no extra reviewer or Maven run.
> 2026-07-16 19:18: TURN-34A Repair #1 Parent Review #3 is `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`. Test `a88d2943...` removes copied caller reducers, matches the ten-collaborator production constructor, and corrects reflection claims. Parent adjudication assigns real caller consumption to existing TURN-34C/35/36/37 TASK cards, accepts deadline/gate evidence where no clock seam exists, and keeps source-string scan prohibited in favor of constructor/public-surface gates plus static review. A is released; TURN-34B byte-drift repair is now same-card ready for the original valid delivery owner to self-claim. No dispatch, extra reviewer, Java write, or Maven run.
> 2026-07-16 19:27: External A re-delivered TURN-34B actual disk test `0edfb55c...` / 1,547 lines / 59 tests. Parent Review #3 is `P0/P1/P2=0/0/1 / REPAIR #2 REQUIRED`: line 173 still uses `TaskMaintenanceService.class.getDeclaredMethods()` while claiming zero private reflection. Same card stays with A for the single-test repair to use public `getMethods()` plus a declaring-class filter and remove all `getDeclared*`; production `8d79d198...` remains frozen. No extra reviewer or Maven run.
> 2026-07-16 19:32: TURN-34B Repair #2 Parent Review #4 is `P0/P1/P2=0/0/0 / SOURCE+TEST SOURCE REVIEW PASSED`. Test `1c344e48...` / 1,551 lines / 59 tests now counts the 19 declarations through public `getMethods()` plus a declaring-class filter, with all `getDeclared*`/`setAccessible`/`Unsafe` references zero. Production `8d79d198...` remains frozen; A is released. No extra reviewer or Maven run; named test/Cloud compile remain at the stable-writer gate.
> `P0/P1/P2=0/5/1 / REPAIR #1 REQUIRED`. Production `8d79d198...` remains accepted; sole named-test repair must
> close broadcast priority, complete Summon gates, exact metadata mismatch zero effects, deterministic tail-cache,
> full capability lifecycle/isolation and remove unnecessary private reflection. Same complete card remains with C.

> 2026-07-17 02:03: TURN-26 Build Repair #2 received Parent Review #4 `P0/P1/P2=0/1/2 / BLOCKED`.
> The real producer publishes a bound clone but returns the unbound original; the no-clear matrix omits wrong
> window/HWND/intent; the objective READ positive bypasses public `handleDialog` through private reflection.
> Whole-card Build Repair #3 returns to the same External B owner. TURN-27 and 35/36/37 remain gated.

> 2026-07-17 02:14: TURN-26 Repair #3 is `COMMUNICATION_STALE + ACTIVE_STALE`; two parent cycles have no
> External B ack and all five write-set files remain unchanged. B's canonical whole-card ownership is preserved;
> no reassignment. TURN-27 and 35/36/37 remain gated.

> 2026-07-17 02:19: TURN-26 Repair #3 source activity recovered (`DialogService` `b28b1335...`, named test
> `2e35148f...`), so `ACTIVE_STALE` is cleared. `COMMUNICATION_STALE` remains pending External B's ledger ack;
> this is protected WIP, not a delivery or review.

> 2026-07-17 02:24: TURN-26 Repair #3 Parent Review #5 is `P0/P1/P2=0/0/2 / BLOCKED`.
> Functional repairs pass source review; two safety JavaDocs still describe the old behavior and require a
> comment-only Repair #4 by the same External B owner. The authorized named test was blocked before execution by
> cross-card compile debt, not a TURN-26 finding. TURN-27 and 35/36/37 remain gated.

> 2026-07-17 02:29: TURN-26 comment-only Repair #4 is source-active (`DialogService` `5d175fd8...`, state
> `169d4382...`); all three test SHAs are unchanged. No canonical delivery yet, so parent review remains pending.

> 2026-07-17 02:34: TURN-26 Parent Review #6 passed `P0/P1/P2=0/0/0`; owner released. The named test remains blocked only by cross-card compile debt. TURN-27 is now whole-card source-start READY / ZERO OWNER for self-claim; TURN-35/36/37 continue to wait for TURN-27 source pass.

> 2026-07-17 02:44: TURN-27 claim race was resolved by physical append order and D's canonical cession. External C is the sole whole-card owner; External d withdrew. Both wrote zero write-set bytes during the race. TURN-27 is source-active; TURN-35/36/37 remain gated.

> 2026-07-17 08:03: TURN-27 Amendment #1 repaired the pathing boundary. Cloud owns navigation decisions and
> explicit JSON actions; DHXY keeps the existing movement detector, `WindowTaskRunner` watcher,
> `WindowRuntimeContext` pathing state, and `WindowPathing*` as `KEEP_LOCAL_RUNTIME`. A typed intent may accompany
> the start action, is registered only after positive local proof, and the authoritative local snapshot returns in
> turn metadata. Cloud keeps only an exact-context read mirror. External C resumes the same whole card.

> 2026-07-17 08:15: External C ACKed TURN-27 Amendment #1 and resumed `SOURCE_ACTIVE` as sole owner. The
> resume-point bytes remain Navigation 2810L/`90f5ea17` and pathing state 202L/`bb4ccebd`; active macro removal
> precedes the typed bridge implementation. No Maven runs while the Java writer is active.

> 2026-07-17 06:06: TURN-27 Amendment #4 freezes the Cloud-internal resolver seam. Cloud `NavigationService`
> may directly call the write-set-owned `MiniMapPointResolver.resolveMinimapClick(JsonNode)` through one narrow
> additive public method; it must not round-trip through `DecisionEngine.decisionResponse`, expose any other resolver
> method, or move the transform table outside Cloud. Existing DecisionEngine dispatch remains unchanged.

> 2026-07-17 06:15: External C ACKed Amendment #4. Cloud `NavigationService` advanced to `a4630010...` with
> typed MOVE/WAIT/CLICK step builders. TURN-27 remains source-active whole-card WIP; no delivery or source approval.

> 2026-07-17 06:28: TURN-27 Amendment #5 consolidates all four prior amendments into the original card's single
> final checklist. This is a contract clarification only: Cloud owns navigation decisions and explicit actions;
> DHXY owns movement proof/watcher facts; active macro and Cloud-side movement observation remain prohibited.

> 2026-07-17 06:42: External C ACKed the final Amendment #5 checklist and scope enforcement, withdrawing
> `expanded scope`. Only mini-map UI seams reached by active current-map/world-map paths may migrate; the other
> 68 input/capture/OCR sites remain untouched. TURN-27 stays source-active with no whole-card delivery.

> 2026-07-17 07:01: TURN-27 is `ACTIVE_STALE`: External C last reported at 06:42 and Cloud Navigation last
> changed at 06:43:33 (`4fb434fe...`, 181,096 bytes). C remains sole owner; no reassignment. A directed parent
> status message is pending. Active macro count is still one and no whole-card delivery exists.

> 2026-07-17 07:17: CR271's authoritative worktree moved to `D:\mavenProject\DHXY-cr271`
> (`thin-client-design`, snapshot `59b85e0b`). `D:\mavenProject\DHXY` remains the user's read-only IntelliJ
> baseline worktree. The parent heartbeat now reads/writes only the relocated CR worktree; Cloud path is unchanged.

> 2026-07-17 07:25: TURN-27 source activity recovered. Cloud Navigation advanced at 11:24:06Z to
> `56cde7e7...` / 182,443 bytes with a typed capture-step foundation, clearing `ACTIVE_STALE`. Macro count remains
> one, handoff helper remains absent, and no delivery exists. C's status/worktree-relocation ledger ACK is pending.

> 2026-07-17 07:36: TURN-27 is again `ACTIVE_STALE` because Cloud Navigation has not changed since 11:24:06Z,
> and `COMMUNICATION_STALE` because C did not ACK the 07:17 authoritative-worktree relocation for more than two
> audit rounds. C remains sole owner; no reassignment. NAV=`56cde7e7...`, macro=1, handoff=0, no delivery.

> 2026-07-17 08:07: External C read the relocated `DHXY-cr271` ledger and ACKed all three pending parent
> messages. Cloud Navigation advanced to `67b33848...` / 191,112 bytes at 12:06:58Z, clearing both
> `COMMUNICATION_STALE` and `ACTIVE_STALE`. C remains sole owner; macro=1, handoff=0, no whole-card delivery.

> 2026-07-17 08:26: TURN-27 is again `ACTIVE_STALE`. Communication remains healthy, but Cloud Navigation
> has not changed since 12:08:54Z for more than ten minutes. Current NAV=`fdb34206...` / 194,778 bytes,
> macro=1, handoff=1, no delivery. C remains sole owner and has a directed status message pending.

> 2026-07-17 08:33: TURN-27 source activity resumed. Cloud Navigation is `225953c5...` / 201,805 bytes;
> active macro count is zero and handoff references are two, closing the macro finding. Amendment #5 settles the
> Xiuluo fire-and-handoff question: it is local-positive-proof-gated; no optimistic Cloud registration or second variant.

> 2026-07-17 08:43: The TURN-27 world-map coordinate assumption is closed by source evidence.
> `CoordinateHelper.getScaledRect()` currently performs `windowBase + offset` only, with no DPI multiplication.
> Remembered points are `windowRect + stored relative`; OCR points are `mapRect + crop center`. Both are unscaled
> screen-absolute coordinates; a second scale or a second window offset is forbidden.

> 2026-07-17 08:53: TURN-27 is `ACTIVE_STALE + COMMUNICATION_STALE`. Cloud Navigation has not changed
> since 12:41:39Z for more than ten minutes, and C has not ACKed the 08:43 proof/coordinate decision for two
> audit rounds. Current NAV=`81222914...` / 202,587 bytes, macro=0, handoff=2, no delivery; owner is preserved.

> 2026-07-17 09:03: TURN-27 has an explicit continuous-delivery execution contract. External C must resume on
> subsequent automatic heartbeats until canonical whole-card `SOURCE+TEST DELIVERED`; an intermediate helper,
> checklist item, turn, or heartbeat completion is not a stop condition. Questions are routed only to the parent
> through the shared ledger. Only a precise blocker or canonical owner return permits an early stop.

> 2026-07-17 09:07: TURN-27 source activity recovered. Cloud Navigation is SHA-256 `037c5f45...` /
> 182,230 bytes, macro=0 and handoff=2; the old violating dead cluster is removed. External C proceeds to the
> sole named test and whole-card SHA delivery under the continuous-delivery contract. Communication remains stale
> pending explicit ACK of the parent messages.

> 2026-07-17 09:12: External C violated the continuous-delivery execution contract by stopping after the dead-cluster
> stage and asking the user whether to continue or wait. This is procedural, not a business decision. C must proceed
> immediately to the named test and canonical whole-card delivery; genuine blockers go only to the parent ledger.

> 2026-07-17 09:15: scheduler audit proves External C's claimed heartbeat `5379f59b` is not registered in the
> real Codex automations table. No 09:13 implementation wakeup occurred. TURN-27 is corrected to
> `HEARTBEAT_MISSING + COMMUNICATION_STALE`; C's canonical owner is preserved and no delivery exists.

> 2026-07-17 09:23: the user explicitly authorized C to register a real heartbeat in C's current task. C's
> statement that authorization was absent is incorrect. Registration must happen now with registry evidence,
> followed by the focused named test and delivery. TURN-27 has no arbitrary 2,200-line test quota.

> 2026-07-17 09:33: TURN-27 test-source activity recovered. `NavigationTurnContractTest.java` now exists at
> 455 lines / SHA-256 `b9272375...`; this is WIP, not whole-card delivery. C's real heartbeat remains missing.

## 2026-07-17 11:35 EDT - TURN-38A-F Review Gate

- `CloudDialogPreparedActionState.peek`、`CloudTaskTurnCoordination.run`、`CloudWholeTaskReadyEventState` 的 source 对照未发现 P0/P1/P2。
- `CloudWholeTaskFoundationContractTest` 尚未覆盖 production `CloudTaskTurnAuthority` FIFO；父级 Review #1 为 `0/1/0 REPAIR REQUIRED`。35/36/37 继续等待此 foundation gate。

> 2026-07-17 11:42 EDT：TURN-38A-F named test 已增至 35,053B / `f515373e...`，FIFO repair active；
> 尚无 canonical re-delivery，whole-task 35/36/37 gate 不变。

> 2026-07-17 11:47 EDT：production FIFO 已覆盖；Review #2=`0/0/1`，named test 尚需 bounded acquire 与
> finally 线程清理。38A-F gate 未通过，35/36/37 继续等待。
## CR271 TURN-36 Consolidated Contract Audit（2026-07-17 12:54 EDT）

- tracker-green intent 只允许与成功点击的同一 turn action 携带，并继续受 DHXY positive movement proof gate；
  fresh route prepared read 必须保留 exact window/HWND、10s age、intentId 或 normalized target 关联。
- FiveRing UI cleanup 在 baseline 两分支均执行 close，Cloud 固定一次无条件 close，不新增 recommendation 协议字段。
- cached ARRIVED intent-only、pathing terminal consume/clear、movement/map/coordinate/flying、runtime progress 与
  `runExclusive` 整段 input 独占尚无等价落点，统一冻结进 35/36/37 共享 foundation Amendment。禁止 Task-local
  consumed-id、WAIT-only optimistic intent、镜像 setter、假 clear 或逐 action 序列化冒充整段 exclusive callback。
- 12:59 follow-up：C 在裁决并发到达前已把 baseline `runExclusive` 单点换成 `run`；该 WIP 必须恢复 exclusive
  authority 或冻结未迁移，其余四个 baseline run 与两个 forceRelease 映射不回退。TURN-35 同类冻结族未闭合前
  不得以 blocker 清单替代 whole-card delivery。
- 13:04 TURN-35 Parent Review #1=`P0/P1/P2 0/2/1`：冻结 production caller/compile 与 BASE/IMG/LS
  public-path acceptance 未完成；现 test 主要直接调用 foundation owner，不构成 whole-task matrix。状态回退
  repair-required，待共享 Amendment 后补齐并重新 delivery。
- 13:23 TURN-35 Amendment #5：三个 baseline raw prepared-slot read 不得套用 consume intent fence，否则会改变
  park/defer。唯一 `CloudDialogPreparedActionState` 增加 exact-slot/window/HWND-bound 的非消费 raw-read view；只供
  Wubei 三个既有判断读取，FiveRing exact peek 与所有 consume fence 不变。写集增加 owner + 既有 foundation test，
  不新增 store/TTL/clear/validation 或业务差异。

# CR271 Foundation Gate Update（2026-07-17 12:12 EDT）

- `TURN-38A-F`：父级 Source Review #3 `P0/P1/P2=0/0/0`，prepared peek、ready-event state 与 fair-turn
  coordination foundation source gate 通过。
- `TURN-35/36/37`：依无环 Amendment #3 同时转 `READY / ZERO OWNER`，分别保持 Wubei/FiveRing/Xiuluo
  whole-task 固定写集；TURN-37 继续保护 `c0125a49...` WIP。
- 构建门仍为共享 main compile debt 阻断，named test 未执行；source review 通过不等于 build/test 通过。

- `2026-07-17 12:21 EDT`：External A 已 canonical claim `TURN-35`，Wubei whole-task 进入 `SOURCE_ACTIVE`；
  `TURN-36/37` 保持 `READY / ZERO OWNER`，TURN-38A-C 仍为 External C 后置等待。

- `2026-07-17 12:25 EDT` DAG 修复：38A-F 已关闭并释放 C owner；38A-C 改为 caller-zero 后独立
  `DEFERRED / ZERO OWNER / NOT READY` cleanup，38B/38M dependency 改为 38A-F。C 不再空等，可自行领取
  TURN-36/37。

- `2026-07-17 12:38 EDT`：C 已 claim TURN-36，与 A 的 TURN-35 并行。TURN-35 pre-battle timer 保留
  typed owner/producer，dialog interest 保留 typed owner/bridge；禁止 Task-local timer/self-check 或用同步调用吸收。
- `2026-07-17 12:49 EDT`：TURN-36 写集批准增加 ReadyEventState 的
  `latestOtherFreshPathingTerminal` 纯加法查询，复用 exact lane/slot，覆盖 own-window/异 lane/超龄/非终态/
  taskType 失配负例；不新增第二 store。

- `2026-07-17 12:33 EDT`：TURN-35 Question #2 的 pathing 写/清、movement intent、progress、startup flying、
  dialog watcher 经跨卡扫描确认同样存在于 TURN-36/37。它们与 Question #1 的 timer/interest/tryRun 合并为共享
  foundation Amendment 审计；必须一次冻结唯一 owner、双仓 bridge/API/write set、exact binding/proof/no-op/
  cleanup 和三张 whole-task named-test matrix，禁止 Task-local state、第二 store 或给 TURN-27 只读镜像加 setter。

## CR271 TURN-35 Foundation Review #2 / Caller Gate Open（2026-07-17 16:09 EDT）

- shared whole-task foundation source+test-source 复审=`P0/P1/P2=0/0/0 PASSED`：17 operation 的 payload/result
  shape 封闭、exact-bound runtime、clear identity/pending cleanup 与 upstream HWND owner 均闭合。
- TURN-35 External A 与 TURN-36 External C 保持各自 sole owner，caller source gate 同时开放；A 续 Wubei，C 续
  FiveRing，均须完整 whole-card canonical delivery，不得把 foundation pass 视为整卡批准。
- DHXY compile exit 0；DHXY named tests 与 Cloud named tests/compile 仍被既有共享迁移债阻断，test/build gate 独立 pending。

> 2026-07-17 16:18 EDT：External C 已 ACK 并恢复 TURN-36 caller 写入；FiveRing progress×2 经 typed
> LOCAL_SERVICE 落盘，prod=`cfe008e8`/3006L。30s request wait 在整卡 review 时须证明为 transport-only、
> uncertainty 向上且无 auto retry/业务 TTL；当前 WIP 非 delivery，不阻断其余 caller 迁移。

> 2026-07-17 16:19 EDT：External A 亦已 ACK Review #2 并恢复 TURN-35 caller 写入；Wubei progress×2
> 经 injected typed LOCAL_SERVICE 落盘，SHA-256=`89392990`/4403L，沿用 10s local-service wait。A/C 双 caller
> 并行，无 blocker、无 delivery，active writers 期间不运行 Maven。

## CR271 Amendment #11 - Unconditional Pathing Clear（2026-07-17 16:23 EDT）

- 696 基线与当前三 Task 共 13 个无条件 clear caller（Wubei 1/FiveRing 5/Xiuluo 7）；intent-id/prefix 条件 clear
  会产生 race 或语义收窄，禁止替代。
- shared foundation 增第18个 `WHOLE_TASK_PATHING_CLEAR`：现有 source 作 reason，DHXY exact bound runtime 直接一次
  `clearPathingSignal`，boolean completed result；terminal uncertainty 不转业务 false、不 retry。完整 protocol/
  executor/client/golden/validator/dispatcher/client-test 写集归 A，一次闭合后父级 source review。
- A/C 继续不受影响 caller，C 仅暂停 A3×5/direct runtime block；TURN-37 的7个对应站点也依赖本 amendment。

> 16:24 并发补记：A 已完成 Wubei caller batch2=`B1059116`/4402L（timer clear×5+target-map-gate）；该轮扫描
> 早于 Amendment #11 append，ACK 顺延下一有效 heartbeat，不计 stale，unconditional caller 仍冻结。

> 16:32：C 已 ACK Amendment #11 并完成 FiveRing confirm×6+detectFlying×1，prod=`6d801e2b`/3036L；A3
> partial block 不变。A 虽未事件 ACK，但 amendment 的双仓 protocol/validator、DHXY executor、Cloud client/tests
> 均持续写入至 16:30，判定 active/not-stale，等待完整 foundation delivery 后复审。
> 2026-07-17 18:06 EDT：TURN-35 Amendment #12 foundation Repair #1 Parent Review #2=`P0/P1/P2=0/0/0
> PASSED`；result inverse closure、完整 preparation phase/absent matrix 与双仓 DTO 合同闭合，A caller gate
> 开放。External C 已从 TURN-37 原卡 READY/ZERO OWNER canonical 自领整卡，保护 `c0125a49...` WIP 并按
> 14:52 hard-gap fence 审计；A/C active writer，未运行 Maven。
> 2026-07-17 18:16 EDT：External C 已完成 TURN-37 hard-gap audit 并进入 Audit-A 机械迁移，Cloud Xiuluo
> WIP=`cb1db7c...`/4,222L；hard fence 保持，非 delivery。External A 连续两轮未 ACK TURN-35 Review #2
> pass 消息，标记 `COMMUNICATION_STALE`，但 Wubei caller WIP 已到 `37cad3f...`/4,468L；owner/caller gate
> 保持，A/C active writer，未运行 Maven。
> 2026-07-17 18:26 EDT：External A 已具名 ACK TURN-35 Review #2/stale inquiry，`COMMUNICATION_STALE`
> 清除；4 dialog caller + 5 真死 param 清理后 Wubei WIP=`839b1e3a...`/4,464L，whole-card test 待。
> External C 的 TURN-37 Audit-A batch 2 已闭合 taskRunId + uiCleaner×21，Xiuluo WIP=`f0319233...`/4,222L。
> 两边均非 delivery，A/C active writer，未运行 Maven。
> 2026-07-17 18:43 EDT：TURN-35 P1-2 full-loop battery scope 采用父级 option B；不为 private caller glue
> 新建约 10 个重协作者 scripted harness。整卡证据固定为 Amendment #12 foundation Review #2、可驱 public/
> component tests 与 canonical delivery 后逐 caller `696a12b0` source review；完整 production assembly/全环
> `execute` 归 TURN-40B/TURN-41。A 保持 sole owner，ACK 后直接收口交付。
> 2026-07-17 18:48 EDT：TURN-37 Amendment #3 Bag 缺口已冻结。新增唯一
> `ReturnItemIntent.FIND_AND_USE_TASK_PAGE` 并复用 BAG_RETURN_ITEM/executeReturnItem；DHXY 在同一 remote
> exclusive callback 内执行 baseline find+USE，禁止 prescan→cached-use 两段、第二 cache/store/retry/wrapper。
> `UNKNOWN` uncertainty-upward。共享 protocol 待 A TURN-35 owner release 后由 C 写；C 同时继续其它 Audit-A/B。
> 2026-07-17 19:03 EDT：TURN-35 canonical whole-card delivery 已完成父级 SOURCE+TEST Review #1，结论
> `P0/P1/P2=0/2/1 BLOCKED / REPAIR REQUIRED`。P1 为泛型 catch 吞 `TaskFatalException`，以及 Cloud 仍依赖
> 四个不存在的 DHXY local runtime/turn 类型并保留 18 个 runtime guard；P2 为 production/test 说明过时。
> A 保持 sole owner 返修，Option B 不变；C 的 Bag 共享写集继续等 A release，其它 TURN-37 审计继续。
> 2026-07-17 19:12 EDT：TURN-35 Review #1 返修消息连续两轮无 External A STATUS EVENT 具名 ACK，标
> `COMMUNICATION_STALE`；A 最近事件尚未超过 10 分钟，不标 `ACTIVE_STALE`，owner 不释放。External C
> 已完成 TURN-37 input×3 exact-window-bound turn 迁移，production=`1695379e...`/4,335L；Bag 共享阻塞不变。
> 2026-07-17 19:13 EDT：External A 已具名 ACK TURN-35 Review #1 并开始 P1-2 返修，首个 priority
> runtime guard 已删除，Wubei WIP=`06a0562a...`。19:12 stale 标记与回执同秒并发，现清除
> `COMMUNICATION_STALE`；owner 与 `0/2/1` 返修门不变，Bag 仍等 A release。
> 2026-07-17 19:44 EDT：TURN-35 Repair #1 Parent Review #2=`P0/P1/P2=0/0/1`。fatal 重抛、四 absent
> 类型/runtime 清零、24 参数构造与 tryRun 语义已通过；唯一 P2 为 test JavaDoc 把 direct coordination
> test 过度写成 public execute coverage。Repair #2 仅改说明，A 保持 owner，Bag 阻塞不变。
> 2026-07-17 19:54 EDT：TURN-35 Repair #2 Parent Review #3=`P0/P1/P2=0/0/0 PASSED`。JavaDoc 现准确
> 区分唯一 `execute(null)`、四处 direct coordination run 与父级 private-caller source-review 层；production
> `52e88c68...` 冻结，test=`43e491e2...`/523L/11T。A owner 释放，TURN-37 Bag Amendment #3 共享 protocol
> 写集碰撞门解除；C active，named test/compile 仍待稳定 writer 窗口。
> 2026-07-17 20:02 EDT：A 已具名 ACK Review #3 并 idle；C 连续两轮未 ACK TURN-35 释放消息且最新事件仍
> 写 Bag 等 A，标 `COMMUNICATION_STALE`。TURN-37 owner/WIP 保留，不标 `ACTIVE_STALE`；Bag Amendment #3
> 碰撞门已解除，已定向要求 C 双 ACK。active writer 期间仍不运行 Maven。
> 2026-07-17 20:04 EDT：C 的具名 ACK 与 stale 记录并发落盘，现接受有效回执并清除
> `COMMUNICATION_STALE`；失效 stale inquiry 不计漏 ACK。C 已开始 Bag Amendment #3，双仓 enum/validator
> byte-identical 落盘；TURN-37 owner、完整 fixed write set 与 pathing proposal gate 保持，仍非 delivery/build passed。
> 2026-07-17 21:28 EDT：TURN-37 Amendment #3 Freeze #2 已闭合 pathing/park 合同。新增唯一 typed
> late-target-map upgrade 与 exact intent/route filtered await；fresh route 由现有 prepared/pathing owner 做
> exact binding+freshness+active/terminal intent-or-target 组合；background parse 继续 async exact-context，
> dependent future 必须传播 UNKNOWN/NOT_EXECUTED/STOPPED。无第二 store/job/poll/sync parse 或业务差异。
> 2026-07-17 21:38 EDT：C 已 ACK Freeze #2 并开始 GAP#2。双仓 operation/validator SHA-256
> `85ffa009...`/`60da55b2...` byte-identical，strict intentId/targetMap shape 在位；其余 executor/client/test、
> GAP#3 filtered await 与 GAP#4/#5 caller 仍为 WIP，未形成 delivery/build 结论。
> 2026-07-17 21:43 EDT：GAP#2 production path 已闭合到 DHXY dispatcher/executor 与 Cloud client；父级实盘
> 核对 SHA-256=`546301ba...`/`3820bde5...`/`59bf77e8...`，保持单 typed turn 与 baseline atomic upgrade。
> GAP#2 tests、dependent-future terminal 映射及 GAP#3/#4/#5 仍待，当前非 delivery/build passed。
<!-- CR271 TURN-37 2026-07-17 21:49 EDT: LocalServiceStepDispatcherContractTest closure is fixed as an explicit
     nine-operation permanent-service set plus a dedicated exact-bound pathing-upgrade route test. This is test
     contract repair only; no business-path or operation ownership change. -->
<!-- CR271 TURN-37 2026-07-17 21:55 EDT: pathing-upgrade validator valid/missing-field/extra-field coverage is
     byte-identical across mirrors; dispatcher route repair remains open before GAP#2 test closure. -->
<!-- CR271 TURN-37 2026-07-17 22:03 EDT: GAP#2 dispatcher route test requires repair: permanent adapter count
     is zero, while matched runtime mutation/mismatch no-op and zero input are the route/ownership evidence. -->
<!-- CR271 TURN-37 2026-07-17 22:09 EDT: GAP#3 exact intent/route filtered await code is in place on the
     existing Cloud ready-event state owner; no second bus/store/poll. Foundation test remains pending. -->
<!-- CR271 TURN-37 2026-07-17 22:18 EDT: GAP#2 production and test closure passed its focused parent
     re-review 0/0/0; GAP#3 filtered-await foundation test remains pending. -->
<!-- CR271 TURN-37 2026-07-17 22:27 EDT: GAP#3 code remains baseline-equivalent, but its specialized
     foundation matrix is blocked until unrelated prepared/newest/interrupt/stop cases are covered. -->
<!-- CR271 TURN-37 2026-07-17 22:39 EDT: GAP#3 focused Review #2 passed 0/0/0. The specialized exact-await
     matrix now covers wrong/non-route prepared events, both sequence-winner orders, bounded interrupt with
     flag preservation, and typed stop (test SHA-256 9c35897e...). Whole-card delivery/build remain pending. -->
<!-- CR271 TURN-37 2026-07-17 22:41 EDT: tryTrackerShortcut now uses typed registerPathing plus exact-context
     async upgradePathingTargetMap (protected WIP SHA-256 1de14739...). Three of four Xiuluo methods are in
     place; continueIfNav/frozen-field cleanup and the whole-card test remain. -->
<!-- CR271 TURN-37 2026-07-17 22:53 EDT: all four Xiuluo production methods are present (protected WIP
     SHA-256 2d4bc1a0...); rawCurrent and frozen runtime owners are zero. The unique whole-card test and
     canonical delivery remain before source review/build. -->
# 2026-07-18 01:34 EDT - CR271 TURN-38B1 Communication Stale

- Review #1 后连续两轮无 C 具名 ACK，三文件无返修漂移；标 `COMMUNICATION_STALE`。
- C owner 与原三文件修复合同保持，不撤卡、不改派；未运行 Maven/runtime。

# 2026-07-18 01:26 EDT - CR271 TURN-38B1 Review #1

- 父级整卡 source+test review=`P0/P1/P2=0/1/2 BLOCKED`：`BagWorkflowState` 构造面须只归 owner；
  native identity drift 必须先完整计算再一次提交且 generation exact +1；named test 补 tenant/user 单维隔离。
- C owner 保持，仅原三文件返修重交；未运行 Maven/runtime。

# 2026-07-18 01:04 EDT - CR271 TURN-38B1 Amendment #1 ACK

- C 已 ACK 并恢复 implementing；observation typed handle/custody/sequence fence 与 title/handle/process native
  identity 合同锁定，HOLD 期间无源码漂移。

# 2026-07-18 01:01 EDT - CR271 TURN-38B1 Amendment #1

- 旧 remote permit/ledger 删除不包含 observation stale fence：保留 turn-native owner-issued typed handle、
  pending custody、单调 sequence 与 `< / == / >` replay/conflict 规则；native key 仅 title/handle/process。

# 2026-07-18 00:54 EDT - CR271 TURN-38B1 owner

- External C 已于原卡 canonical claim TURN-38B1，A ACK 后保持 idle；当前 `SOURCE_ACTIVE / OWNER-C`。
  protected production 尚无新字节，唯一 named test 仍 ABSENT；runtime assembly 仍归 40B。

# 2026-07-18 00:41 EDT - CR271 TURN-38B1 readiness

- TURN-38B1 已从 stale `NOT READY` 修复为 `READY / ZERO OWNER`：Bag state 由一个 scope-bound
  `CloudBagStateOwner` 持有，workflow 按 exact tenant/user/device/window/taskRun 隔离并在三 terminal 释放；
  visible/item/anchor/geometry cache 不随 task terminal 清除，host close 才清 scope。40B 负责最终 assembly。
> **CR271 2026-07-18 21:30 EDT TURN-39P1 Review #8：** Repair #7 复审为
> `P0/P1/P2=0/2/0 / REPAIR REQUIRED`。branch-level 单请求与显式无 hold `PRESS_CTRL_A` 方向接受；
> 仍须分离 DHXY/Cloud 同名 input 类型、闭合 Cloud 五文件 legacy cohort、冻结无条件 DHXY worker +
> 双仓协议/Cloud Navigation producer 写集，以及 Alt+1 后台资格与 terminal/focused fallback tests。
> A owner retained，TURN-39 未开放；无 Java/Maven/runtime/input/build 变化。
> **CR271 2026-07-18 21:45 EDT TURN-39P1 Review #9：** Repair #8 复审为
> `P0/P1/P2=0/2/0 / REPAIR REQUIRED`。repo-qualified Cloud 五文件 cohort/`TURN-39C1` 前置形状、镜像
> wire/Cloud producer、Enter=`KEY_TAP` 与 Alt+1 独立请求接受；仍须补齐 `TurnInputStepExecutor`、
> `TurnKeyMapper`、mapper 完整 keyboard/text 职责，以及真实 worker `InputActionFrozenExclusiveContractTest`
> 的 Alt+1 三分支/`PRESS_CTRL_A`/取消断言。A owner retained，TURN-39 未开放；无构建状态变化。
> **CR271 2026-07-18 22:17 EDT TURN-39P1 Review #11：** Repair #10 复审为
> `P0/P1/P2=0/1/0 / REPAIR REQUIRED`。Review #10 两个 manifest 漏项已闭合；最终 test manifest 仍漏
> `WubeiWholeTaskTurnContractTest.java`，无法验收 Wubei 四 caller/order/timing 与 `InputSequences`
> constructor dependency 退役。A owner retained、communication normal；TURN-39 未开放，无构建状态变化。
> **CR271 2026-07-18 22:05 EDT TURN-39P1 Review #10：** Repair #9 复审为
> `P0/P1/P2=0/2/0 / REPAIR REQUIRED`。三个 turn execution owner、完整 mapper、generic frozen sequence、
> unsupported key 与 real-worker test 职责接受；最终 production/test manifest 仍分别漏
> `LocalTurnActionExecutor.java` 与 `TurnInputStepExecutorContractTest.java`。A 21:56 双 ACK 后 communication
> normal；owner retained，TURN-39 未开放，无构建状态变化。
> **CR271 2026-07-18 21:51 EDT TURN-39P1 communication：** Review #9 返修消息连续两轮无具名 ACK，
> External A 标记 `COMMUNICATION_STALE`；最后事件 21:44，源码无变化但尚未达到十分钟 `ACTIVE_STALE`
> 门。owner A 与 `0/2/0 REPAIR REQUIRED` 不变；TURN-39 未开放，无构建状态变化。
# 2026-07-19 01:17 EDT - TURN-39K Test-2 Contract Repair

- TURN-39K production done，test-1 isolated 16/16 green。test-2 已加入 same-window mixed mouse+keyboard order proof；
  其旧 queue test double 未覆盖 TURN-22D1 frozen exact-window API，父级批准仅在冻结 test 写集内升级并恢复既有
  mouse matrix。无 production/Cloud/业务语义扩张。
- 隔离取证完成后可 source delivery；Maven named gate 仍被写集外 dirty testCompile 阻断。
# 2026-07-19 01:25 EDT - TURN-39K Delivery Scope / C2 Direct Regression

- 39K test-1 16/16；mixed mouse+keyboard order 与七个既有 mouse regressions green，允许 canonical source
  delivery。唯一 remaining red 是 C2 bag admission/typed-STOPPED 测试，零 mouse queue，独立于 39K。
- C2 标记 `DIRECT ISOLATED TEST REGRESSION / ZERO OWNER / ROOT-CAUSE AUDIT / NOT CLAIMABLE`；先审完整调用链与
  harness 次数，不扩 39K、不派卡。Maven named gate 仍 blocked/pending。
> **CR271 2026-07-18 15:46 EDT TURN-39W Review #1：** A 的 canonical 四文件 delivery 已由父级审核为
> `P0/P1/P2=0/2/2 / BLOCKED / REPAIR REQUIRED`，A 保持 owner。四个 production caller 与 696 顺序/时序
> 等价；测试须修 randomized tracker 成功终态进入 null maintenance collaborator，以及缺失的 caller-level
> terminal truth；另修 JavaDoc 邻接和测试说明。C4 仍 6/8 active，未运行 Maven。
> **CR271 2026-07-18 15:48 EDT C4 active-row closure：** 七个 active row
> `1070/1450/1674/1968/2081/2218/2231` 全迁，production `inputProvider.*=0`；仅 dead 2334 删除、active
> mini-map helper 的 696 re-observe/retry-once 转移、退休 ownership 与两项独立测试待闭合。C active、无
> blocker/无 delivery；A 的 39W repair ACK pending 第一轮，未运行 Maven。
> **CR271 2026-07-18 15:55 EDT C4 production complete / A stale：** C4 七 active row 迁移、dead 2334 删除，
> active helper 承接 696 re-observe/retry-once，legacy input ownership=0；C 转两 test、未 delivery。A 的
> 39W repair bytes 活跃但连续两轮未 ACK Review #1，标 COMMUNICATION_STALE、非 ACTIVE_STALE。未跑 Maven。
> **CR271 2026-07-18 16:01 EDT TURN-39W Review #2 passed：** A 双 ACK 后通信恢复；四 fresh blobs 复审
> `P0/P1/P2=0/0/0`，Review #1 全闭合，source owner 释放。四 caller exact steps/timing/terminal truth 与 696
> 等价。C4 test active，build/named test 仍 BLOCKED/PENDING，39C1 NOT READY，未跑 Maven。

> **CR271 TURN-40E Parent Review #2（2026-07-20）：** `P0/P1/P2=0/1/0 BLOCKED`。Repair #6
> recognition/asset/protocol-byte/JavaDoc 已闭合；Cloud 五倍/修罗 accept-time snapshot 仍错误依赖 tracker
> anchor。Repair #7 只允许 facade 两入口复用既有 direct snapshot title/detail 分析并保留 absolute origin；
> 本地 mechanics、Cloud algorithm、protocol/task phase 冻结，TURN-41 继续 blocked。
# TURN-40B-P2 Review #3 note - 2026-07-18 21:56 EDT

Report re-delivery #2 is blocked `0/2/1`: runtime ownership direction is accepted, but exact OCR API/result types,
canonical original-card write sets and executable test commands/counts remain unfrozen. No implementation READY gate
opens; TURN-40C stays blocked.
# TURN-40B-P2 Review #3 ACK - 2026-07-18 22:06 EDT

A acknowledged the `0/2/1` repair. OCR literal API is drafted; canonical card boundaries and exact test gates remain.
No READY gate opens and TURN-40C stays blocked.
# TURN-40B-P2 Review #4 - 2026-07-18 22:11 EDT

Re-delivery #3 remains blocked `0/1/1`. OCR literal contract is accepted; fixed original-card boundaries, real test
artifacts and executable per-repo commands/counts remain. No READY gate opens; TURN-40C stays blocked.
# TURN-40B-P2 Review #4 ACK - 2026-07-18 22:21 EDT

Actual tests/counts are audited; fixed TURN-40B sub-boundaries and literal dual-repo commands remain.
No READY gate opens; TURN-40C stays blocked.
# CR271 P-PROTO Review #1 - 2026-07-18 23:46 EDT

P-PROTO canonical delivery 已审为 `0/2/1 BLOCKED`：replacement reason 未强制非空，routeMode 未限定为本地
唯一稳定枚举名且测试使用非法值，新 nullable result/compat ctor 未做 strict-mapper 覆盖。A 保留 owner；
P-LOCAL/P-CLIENT 继续 blocked。P-OCR/P-NAV 仍等待用户 hybrid-vs-single-provider 决策。

# CR271 P-PROTO Review #2 - 2026-07-19 00:01 EDT

Repair #1 复审为 `0/1/1 BLOCKED`。nonblank reason 与 result carrier 语义已接受；shared routeMode 必须按
DHXY 接收端闭集为唯一 `YELLOW_DESTINATION_MINI_MAP`，拒绝 Cloud legacy/unknown。golden coverage 须折回
现有方法并恢复冻结 7T。A owner retained，P-LOCAL/P-CLIENT blocked；P-OCR 用户 A/B 决策不变。

# CR271 P-PROTO Review #3 - 2026-07-19 00:11 EDT

Repair #2 复审为 `0/0/1 BLOCKED`。production exact allowlist 与冻结 validator/golden `17+7` 已闭合；现有
validator test 仅拒绝 blank/Cloud legacy，缺一个任意未知非空 routeMode 负例。A owner retained 做 test-only
Repair #3；P-LOCAL/P-CLIENT blocked，P-OCR 用户决策不变。

# CR271 P-PROTO Review #4 Passed - 2026-07-19 00:21 EDT

Repair #3 review=`0/0/0 PASSED`；final eight files 双仓同字节，validator/golden=`17+7`，A owner released。
P-LOCAL/P-CLIENT 现为 public READY/ZERO OWNER。用户已选 P-OCR B，批准 single-provider/no-Baidu 差异，
C source-active；P-NAV 仍等待三张前置 source review。

# CR271 P-CLIENT Review #2 - 2026-07-19 01:42 EDT

P-PROTO、P-OCR、P-LOCAL source gates 已通过。P-CLIENT Repair #1 production 已接受，但 test/doc 复审为
`0/1/2 BLOCKED`：failed-replace fixture 必须使用 validator-valid `YELLOW_DESTINATION_MINI_MAP`，现有六方法须
在不增加 33T 数量的前提下闭合 non-success/empty outbound source/payload/reason 与 pending-route smuggle
negative，replacement reason JavaDoc 必须声明 nonblank。External C owner retained；P-NAV 只等待 P-CLIENT
通过，runtime/factory/40C 不开放。

# CR271 P-CLIENT Review #3 Passed / P-NAV Ready - 2026-07-19 01:52 EDT

P-CLIENT final client/test=`087D053F`/521L + `4892F1D9`/604L/33T，父级 source+test review=`0/0/0 PASSED`；
External C owner released，无业务差异。P-PROTO/P-OCR/P-LOCAL/P-CLIENT 全部 source-gate complete，P-NAV 的
冻结 NavigationService + NavigationTurnContractTest 边界现为 public `READY / ZERO OWNER / UNASSIGNED`。
runtime/factory/40C 与 aggregate Cloud build 仍 blocked。

# CR271 P-NAV Double-Claim Resolution - 2026-07-19 02:02 EDT

P-NAV physical claim order is C lines 1567-1594 before A lines 1596-1605. External C is the sole canonical owner and
may resume the frozen NavigationService + NavigationTurnContractTest closure. A has canonically withdrawn the later
claim and confirmed zero source writes. Both baselines remain `B57ECC50` + `79D48FE0`/23T, so no Java collision landed.
Runtime/factory/40C and aggregate Cloud build remain blocked.

# CR271 P-NAV Legacy Route Outcome Contract Block - 2026-07-19 03:19 EDT

父级对照 696 与当前双仓源码确认，legacy/map-only route 会记录并复用 `LEGACY_GREEN_LINK` pending outcome；
删除记录会取消后续 memory fast path。P-NAV 仅该子项 `PLAN-CONTRACT BLOCKED`，其它 final cluster 仍由 C
继续。唯一待用户决策：保持 696 并扩 P-PROTO/P-CLIENT（推荐），或明确批准退役该 legacy 记录。未决前
runtime/factory/40C 与 aggregate build 保持 blocked。

## 2026-07-19 03:39 EDT - P-NAV Active Stale

C 超过 10 分钟无 STATUS EVENT 且 NavigationService 保持 `C7A7CF00`/3076L，标 `ACTIVE_STALE`；sole owner
不释放，legacy route-outcome 用户决策阻断不变。已通过总账定向要求下一 heartbeat ACK 与精确进度回报。

# CR271 P-NAV First Source Increment - 2026-07-19 02:14 EDT

External C ACKed sole ownership and resumed the frozen P-NAV boundary. The first real increment removes the dead,
zero-reference `InputProvider` import/field from NavigationService: `B57ECC50`/3155L -> `3C12E5E4`/3153L.
NavigationTurnContractTest remains `79D48FE0`/1470L/23T. Remaining rewires are WIP; no delivery or review exists yet.
Runtime/factory/40C and aggregate build remain blocked.
> **CR271 P-NAV deprecated legacy decision（2026-07-19 03:49 EDT）：** 用户确认
> `clickRememberedWorldMapRouteResult(...)` 的 legacy green-link route-result memory 是 deprecated 旧链；不迁、
> 不续接、不改方法体，也不为它重开 P-PROTO/P-CLIENT。原 legacy 子项 `PLAN-CONTRACT BLOCKED` 解除，P-NAV
> 仅完成 current yellow 路径。C 连续两轮未 ACK stale 消息，现为 `COMMUNICATION_STALE / ACTIVE_STALE`，owner 保留。
>
> **CR271 P-NAV stale recovered / deprecated scope clarified（2026-07-19 03:59 EDT）：** C 双 ACK 并报告
> 精确状态，NavigationService 随后恢复为 `4915DEC5`/3078L；清除 communication/active stale。deprecated 方法
> 从现在起不再修改、扩协议或补测试；此前机械 collaborator 替换不为字节还原而回滚，final review 仅核无业务差异
> 且 current yellow 路径不依赖旧链。C sole owner / SOURCE_ACTIVE，尚无 delivery。
>
## CR271 P-NAV build evidence - 2026-07-19 05:54 EDT

- `NavigationService` migration source now contributes zero errors to aggregate compile (`77E56B2D`/3066L).
- Aggregate Cloud compile remains blocked by out-of-card Task/shared-debt files; P-NAV is still source-active pending
  isolated 23-test evidence and canonical delivery. Deprecated legacy route-result flow remains excluded.

## 2026-07-19 06:04 EDT - P-NAV Isolated Verification Active Stale

- External C remains sole owner, but no C event or source/test byte change occurred for more than 10 minutes during
  isolated verification. P-NAV is `ACTIVE_STALE` pending the directed heartbeat ACK; no delivery/review exists.

## 2026-07-19 06:14 EDT - P-NAV Source Recovery / Communication Stale

- Test source changed to `D1B124DB` at 06:11, clearing `ACTIVE_STALE`; C remains sole owner and writer-active.
- Two audit rounds passed without the required C ACK, so P-NAV is `COMMUNICATION_STALE` pending dual-message ACK.

## 2026-07-19 06:34 EDT - P-NAV Isolated Compile/Test Milestone

- Isolated harness compiles 528 main files at zero errors and the named test currently passes 13/23. It found and
  repaired the navigation timeout contract mismatch (30 seconds to 120 seconds).
- Aggregate build remains blocked outside the card, no delivery/review exists, and communication stale remains pending
  ACK of both directed parent messages.

## 2026-07-19 06:44 EDT - P-NAV Communication Recovered

- C explicitly ACKed both pending parent messages, clearing communication stale. C remains sole owner/source-active;
  isolated build/test evidence remains 528-file clean and 13/23, with no delivery/review yet.

## 2026-07-19 07:09 EDT - P-NAV Isolated Tests 20 Of 23

- C reports the isolated named test advanced from 13/23 to 20/23 after aligning typed local-fact and click-proof
  choreography. The remaining three failures require content-bearing dialog/world-map/template capture fixtures.
- `NavigationService` remains `D56DEAFD`; the test file is still changing, so C remains source-active with no
  delivery/review. Aggregate build remains blocked by out-of-write-set shared debt.

## 2026-07-19 09:52 EDT - P-COMPILE Source Activity Recovered / Communication Stale

- A's fixed test-only write-set resumed changing (`3D2CEEFE` / `AC90360A` / `244F71C5` / `F32E8972` at 09:54:59), clearing
  `ACTIVE_STALE`; A remains sole owner and production remains frozen.
- The old Maven/Powershell exited without complete reports or exit evidence. Named-test acceptance remains
  `BLOCKED / PENDING`, no re-delivery exists, and `COMMUNICATION_STALE` remains until A names the standing ACK.

## 2026-07-19 10:08 EDT - P-COMPILE Authorized Ten-Test Rerun Active

- Tracker test advanced to `B8EA0515`; A started the fixed ten-test Maven at 10:07 and the first three test reports
  are present. Status is `NAMED TEST RUNNING / SOURCE ACTIVE / COMMUNICATION_STALE` with A's sole ownership retained.
- Final exit and remaining reports are pending; no canonical re-delivery/review exists. Parent runs no Maven.

## 2026-07-19 10:09 EDT - P-COMPILE Ten-Test Rerun Failed

- Fixed ten-test result is 239 tests / 22 failures / 67 errors. DialogDetection and SummonSkill are green; eight
  classes remain red on context binding, strict JSON and local-action fixtures within the approved test-only write-set.
- Status is `BUILD FAILED / REPAIR ACTIVE / COMMUNICATION_STALE`; A sole owner and frozen production remain.

## 2026-07-19 10:19 EDT - P-COMPILE Q1-Q4 Plan-Contract Ruling

- Runtime acceptance is the four named WholeTask tests only; the other six require testCompile clean. Strict result
  fixture alignment is authorized only in those four against current production result shapes.
- Summon's exact 38-value enum golden is accepted; DialogOption runtime and tracker PT20S are excluded. Production 30s
  remains frozen. Plan blocker is cleared and A continues the bounded test-only repair.

## 2026-07-19 10:27 EDT - P-COMPILE Ruling ACK / Repair Resumed

- A named-ACKed Q1-Q4 and resumed strict-result fixture work in the four named WholeTask tests. No new Maven or
  delivery exists; A retains sole ownership and production remains frozen.
- Communication stale remains only because the older 09:09 message is still not named in A's ACK field.

## 2026-07-19 10:37 EDT - P-COMPILE Bounded Test Byte Progress

- Runtime-gate test snapshots are `244F71C5/C4939131/2CA1F71C/D0BA4DAB`; FiveRing, Wubei and Xiuluo changed
  consecutively after the ruling ACK, while Summon and all four production files remain frozen.
- There is no Maven, canonical re-delivery or review verdict. A retains sole ownership, Review #1 remains open,
  communication stale remains pending the exact old-message ACK, and runtime/factory/TURN-40C stay blocked.
> **CR271 TURN-40C activation 7/7 passed / communication recovering（2026-07-19 19:40 EDT）：** A 已 ACK R5 `1924`，
> host graph 完整 refresh 0 bean error；R1 `1926` 与 stale `1930` 仍待 ACK，故通信转 recovering。
> activation 修后源码为 `7B418DF0`/376L/7T，点名测试 `7/7 PASS / EXIT 0`；全 family/build/delivery 待续；
> A owner、15-path 与 foundation collision 边界不变，非 active stale，父级未跑 Maven。

## TURN-40F Repair #2 Final Service Matrix Delta - 2026-07-20

| Client owner | Final disposition | Cloud/business owner |
|---|---|---|
| `BagService` | KEEP：固定背包几何、单队列/open-session、capture/template facts、物理 use/count | Cloud task + `PlayerStateService` 决定香、phase、count/terminal |
| `UICleanerService` | KEEP：通用窗口/X2 机械关闭 | Cloud `CloudUiCleanerPort` + `DialogService` 决定业务对话/候选/fallback |
| `GiveItemService` | KEEP：give-entry/item/button 单独占物理宏 | Cloud task 决定何时/给什么/终态 |
| `QuestManagerService` | KEEP：任务栏固定点击与 raw detail capture | Cloud OCR/task owner 解释 detail |
| 其余 59 个 Service / 7 vision | DELETE/MOVE；业务迁 Cloud，机械移至 capture/input/window/turn-local/host | Cloud-only phase/OCR/rank/retry/fallback/terminal |
| `SystemPowerService` | DELETE；非 Service `HostLocalOperationExecutor` 保留 Windows side effect | Cloud `HostSleepTask` + HTTPS v1 `HOST_SLEEP_COMPUTER` |

当前 client service exact count=4；vision=0；旧 cloud stack=0。授权测试与双 compile 已通过，visual replay 因
缺 repo-local 五环对话 raw testcase而 blocked，等待父级审核，未 Approved。

## TURN-40F Repair #3 Ready Event Matrix Delta - 2026-07-20

| Baseline producer | Final owner | Client boundary |
|---|---|---|
| combat transition | Cloud observer + `CloudWholeTaskReadyEventState` | HWND raw combat/status facts only |
| ordinary pre-battle timeout | Cloud observer timer/transition | no timer/business event store |
| visible dialog attention | Cloud dialog observer | raw fixed-region capture only |
| prepared action ready | Cloud dialog/task-tracker preparation owner | execute approved typed click only |
| pathing terminal | Cloud navigation observer/state | raw mini-map capture/pixel mechanics only |

本地 `WindowTaskRunner` 不恢复 watcher；event bus/sequence/store保持 Cloud 唯一。Repair #3 完成前 TURN-41 blocked。

### Repair #3 Parent Review #1

- `PATHING_TERMINAL` WIP 主判定对齐，但另外四个 event owner仍未迁入 Cloud。
- Cloud observer/task thread 的同窗单 action slot 必须由现有 authority 形成 task-priority/park-only admission；禁止
  第二 endpoint、第二 action consumer 或本地 event bus。
- 当前 `P0/P1/P2=0/2/1 / REPAIR REQUIRED / NOT READY FOR USER TEST`。

### Repair #3 Parent Review #2

- FAIL：combat transition必须清 client authoritative pathing prefix slot，不能只清 Cloud mirror；Wubei gate/cleanup同收敛。
- FAIL：parked observer必须真实执行 dialog preparation chain；prepared-state direct publish hook不算 producer closure。
- REPAIR：pre-battle typed fact保留 target与atomic single-publish fence。

### Repair #3 Parent Review #3

`P0/P1/P2=0/3/1 / REPAIR REQUIRED`：observer prepared-action顺序/current fence、Wubei gate+interest原子提交、
pathing terminal后的transfer/world-map memory settlement仍未等价迁云；helper/direct producer测试不能替代observer生产链。
永久本地Service仍exact four，client不恢复watcher/bus；TURN-41继续blocked。

### Parent Review #4 Full Baseline Gap Audit

- terminal settlement需在后续terminal observation重试未消费slot。
- `TaskStartupWindowPreparationService` 的map tracking/Alt+5/Alt+6/flying guards必须由Cloud policy + client typed mechanics承接。
- `MapSurveyService`既有矩阵裁决未执行：Cloud仍缺标定math/persistence/undo/project owner，client UI也被删除；Repair #4必须恢复功能。
- Plan-contract Repair #4A：批准同一HTTPS turn v1的typed command/result/ack和exact-window pointer sample；manual
  survey不伪装为普通task start。Cloud持有唯一session/math/persistence，client仅UI/capture/pointer/move mechanics。
- `FiveRingTaskV2::waitPathing / clearTrackerPathingIntentAfterCombatRecovery`：当前baseline在tracker pathing中经历战斗且
  recovery完成后，先清`wuhuan-v2:prepared-tracker-panel-click:`，未命中再清
  `wuhuan-v2:tracker-green-click:`，随后同步任务栏。Cloud当前缺该定向cleanup owner；须使用现有typed runtime clear
  在Cloud task policy中闭合，client不得恢复业务判断或新增store/bus。
- `TaskMaintenanceService::handleMaintenanceBroadcast` / `DialogService::CLICK_BUSINESS_OPTION`：当前baseline已把维护广播
  收敛为固定raw strip ROI两模板，且删除整块dialog绿/黄洗图business-option fallback。Cloud当前仍从maintenance与
  `CloudUiCleanerPort.cleanLightweightInterruptions`调用旧fallback；须删除该生产可达业务分支，保留Cloud raw-template
  policy与client exact-HWND capture/click mechanics。

## 2026-07-20 TURN-40F Review #5 Verification Gap

- wrapped-route center correction仍未通过真实world-map raw testcase和production入口replay；synthetic色块/private helper
  reflection不计验收。
- FiveRing post-combat prefix cleanup production已落盘，但唯一测试只做source-string guard；须执行`waitPathing`和typed
  local-service client，覆盖prepared hit、legacy fallback、foreign保留、no-combat与stop/pause零调用。
- Repair #5 WIP：route raw/public入口验证已开始但尚未稳定；FiveRing production harness仍未落盘，gate保持FAIL。
## 2026-07-20 TURN-40F Repair #5 Parent Verification

Repair #5真实route replay与FiveRing cleanup production harness focused=`0/0/0`且父级复跑PASS；TURN-40F整卡终审未完成，TURN-41 learned-memory/`map_camera_bounds` cutover仍BLOCKED。

## 2026-07-20 TURN-40F Role Preflight Gap

状态=`REPAIR #6 REQUIRED`：Cloud须恢复live UNKNOWN -> existing window role assignment fallback，以及tooltip probe随机半径/1秒retry settle，方可满足当前dirty baseline等价。

Huygens已ACK并进入`SOURCE ACTIVE`；owner保留。

Repair #6 focused parent verification=`0/0/0`，production fallback与tooltip mechanics已通过；whole-card终审仍未完成。
> **TURN-41 Parent Review #1（2026-07-20 20:40 EDT）：** cutover source is delivered but not accepted:
> `P0/P1/P2=0/1/1`. Transactional auto-restore, pre-mutation backup visibility, failure-injection tests, and
> safety JavaDoc are under same-card Repair #1. Exact scope and fresh runtime remain blocked.
> **TURN-41 Repair #1 parent pass（2026-07-20 20:49 EDT）：** transactional cutover source and tests
> passed `0/0/0`; named contracts `7+1` and Cloud compile were parent-rerun PASS. Zero owner; exact production
> scope and real cutover remain the only pre-runtime gate, so the migration is not yet test ready.
> **TURN-41 real cutover passed（2026-07-20 20:54 EDT）：** exact local production scope has been
> established outside all repositories. Inspect/DryRun/Apply/post-read and canonical counts/hashes passed;
> backup is retained. Data cutover is closed and user fresh runtime may start; migration is not final until it passes.

> **TURN-40F runtime equivalence reopen（2026-07-21 04:09 EDT）：** start-exit mini-map close is repaired,
> but a four-way method-level audit found `P0/P1/P2=0/10/3`. Stable-window metadata, accept same-frame,
> maintenance dialog close, transient recovery, turn/input and other service parity remain open. Status is
> `REPAIR REQUIRED / NOT READY FOR FORMAL USER TEST`; the earlier data cutover pass remains valid but is not a
> substitute for source equivalence.
> **TURN-40F Repair #8 parent review #11（2026-07-21 05:29 EDT）：** first delivery remains blocked at
> `P0/P1/P2=0/2/0`: observer-owned STORY close lacks the baseline member/non-combat safety gate, and the
> stable-window parameterized test does not invoke the fourteen consumer production validation entries. Lorentz
> retains the same-card owner; formal user testing remains blocked.
# 2026-07-21 CR271 Joint Review Delta

- Cloud task runtime registry is now source-reviewed for five-window isolation (`0/0/0`); this closes the shared
  process-slot source defect but not the fresh `5/5 ACK` runtime gate.
- Local Observation Runner/TURN-40G is not accepted: task-run identity currently crosses Cloud/Client as a lossy
  `long hashCode`, and partial schedule payloads can install defaulted identities. Required cutover is one exact String
  identity end-to-end plus fail-closed atomic schedule validation; no projected second identity/store.
> **2026-07-21 lifecycle correction:** Cloud whole-task Observer改为exact-run context绑定，checkpoint元数据读取
> 不再依赖worker ThreadLocal；stop中断exact worker，冲突启动返回typed 409。业务Service决策与输入顺序未变，
> source+test通过，fresh runtime待验。
> **2026-07-23 Fast Exit 消费顺序修复：** 本地 exact fast edge 已到 Cloud 后，
> `AutoCombatService` 必须先消费 one-shot，再允许 sparse/full radar fallback。该修复只调整
> Cloud 消费顺序，不迁移新 service，不改变 ROI、模板、阈值、导航或输入边界。聚焦回归
> `1/1`、既有 fast/observer 合同 `49/49`、Cloud compile 通过；待重启 fresh 验收。
>
> **2026-07-26 CR277：** 五环 Tracker/Dialog/NPC 后台 prepared-event 迁移经父级
> source+test 终审通过（`P0/P1/P2=0/0/0`）。exact window/task demand、message+unlock
> Ready、既有 SmartClick 流式 FIFO、消费前 coarse task turn 和同 intent 10 秒 re-wake
> 均有当前源码合同覆盖；Client `4/4`、Cloud `18/18 + 2/2`、双仓 compile `0`、
> wire `5/5 byte-identical`。仍等待 fresh 五窗口运行验收。
>
> **2026-07-26 CR277 Fresh Repair #2：** 五环 handover 已补 exact fresh Tracker negative
> 消费：not-found 进入接任务，found-but-no-link 进入同步，正候选优先，无结果才 park。
> 父级 current-production focused `3/3`、Cloud compile `0`；旧 direct-in-turn Tracker
> fixture 仍为 stale `0/7`，未作为当前门禁。等待 Cloud 重启后的 fresh runtime。
>
