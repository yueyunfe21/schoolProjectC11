# A-5 主验收矩阵（THIN_CLIENT_V1）

> **LOCAL PATHING DIRECT WAKE GATE（2026-07-24）：** Client 日志出现 exact
> `Local pathing terminal observed` 后，Cloud 必须立即为同一 intent 发布一次
> `PATHING_TERMINAL` 并唤醒 owning task；禁止等待 periodic observer 或独立 pathing
> timeout。Client/Cloud compile 均通过，fresh 双端重启待验。
>
> **2026-07-24 LOCAL COMBAT TRI-STATE：** 战斗中本地观察必须区分
> `WORLD_CONFIRMED / COMBAT_CONFIRMED / UNKNOWN`。小地图锚点与战斗模板均只提供
> 正向证据；模板 miss 或 capture unavailable 不得制造相反事实，未知态保持当前状态按秒重试。
>
> **LOCAL MINIMAP EXIT SOURCE GATE（2026-07-24）：** Parent Review #17
> `P0/P1/P2=0/0/0 / OWNER RELEASED`。修罗/五倍本地 Runner 看到小地图锚点即发布唯一
> `COMBAT_EXITED`，Cloud 无 Radar/readability/二次确认。真实画面生产匹配 `11/11`、Cloud
> observation `18/18`、双 test-compile 和协议同字节通过。Fresh 必须重启双端，验收锚点出现后
> 一个采样周期内进入战后流程。
>
> **PAUSE/RESUME LONG-POLL SOURCE GATE（2026-07-24）：** Parent Review #9
> `P0/P1/P2=0/0/0 / OWNER RELEASED`。pause exchange 零等待且同一 Runner 在普通长轮询前恢复；
> 聚焦 family 与 Client compile 通过。Fresh 必须验证队长数秒内恢复、`Alt+8` 可补齐、
> 脱战不再等待 180 秒。
>

> **LOCAL RUNNER AUTHORITY FINAL SOURCE GATE（2026-07-24）：** Parent Review #6
> `P0/P1/P2=0/0/0 / OWNER RELEASED`。expected/incidental 本地机械 fast exit 分层、exact enter
> claim、retained replay lifecycle、typed terminal 和修罗/五倍失败 fallback 均有行为合同；
> Cloud/Client named families 与双 compile 通过，协议 `7/7` byte-identical。仅剩 fresh-runtime
> 验收，不得把源码门通过表述为端到端运行完成。
>
> **LOCAL RUNNER AUTHORITY GATE REOPENED（2026-07-23）：** `P0/P1/P2=0/2/2`。
> 必须统一 observation/business run 身份、补 replay success/failure typed terminal、移除 sampler
> 线程上的同步背包宏并补齐合同测试；此前 source pass 无效，禁止 fresh runtime。
>
> **LOCAL RUNNER AUTHORITY SOURCE GATE（2026-07-23）：** 修罗/五倍本地 Runner 独占战斗
> 进出与路径时序，Cloud 不再用回滚纠正 false fast exit。验收要求：一次 arm、同 taskRun 本地
> 真脱战后只重放一次原回程命令、Cloud 全程保持 `RETURN_HOME`，重放失败不发布退出边沿；
> 同尺寸窗口平移后缓存点击按 delta 命中，HWND/尺寸变化必须 fail-closed。源码审查
> `P0/P1/P2=0/0/0`、双仓 compile exit `0`，仍需 fresh runtime。
>
> **FAST EXIT PENDING/VISIBLE RACE SOURCE+TEST PASS（2026-07-23）：** fresh run 已证明 fast
> edge 发布并在 `313ms` 唤醒任务；旧 visible 样本在 one-shot 消费前复活战斗的竞态已封闭。
> 聚焦 `49/49`、compile/diff-check exit `0`。验收仍要求重启 Cloud 后 fast edge 直接推进
> post-combat，且无同战 generation 复活或 `discard stale`。
>
> **FAST EXIT EXACT-IDENTITY SOURCE+TEST PASS（2026-07-23）：** fresh run 中完整雷达两次确认
> 脱战，但 identity A/B 漂移令任务拒绝有效退出信号并停在 `WAIT_COMBAT`。Cloud 已对
> `PENDING/ARMED` 统一执行 confirmed combat identity reconcile；聚焦 `48/48`、compile exit `0`。
> 验收仍要求重启 Cloud 后真实脱战推进 post-combat，并单独确认本地 `20x20` fast edge。
>
> **COMBAT-OBS FRESH P1 SOURCE+TEST PASS（2026-07-23）：** 修复 production taskCode 导致 Fast
> Exit 永不 arm，以及 coordinate/dialog 动态 interest revision 自激导致 pathing terminal 长等待。
> 父级 `P0/P1/P2=0/0/0`，Cloud `32/32`、compile 通过。fresh runtime 仍须证明真实本地 exit
> 推进 post-combat，且到达 NPC 后不再等待约 165 秒。
>
> **TURN-40G DIALOG-DEMAND SOURCE+TEST DELIVERED（2026-07-23）：** Client local kanda2 first
> refusal 与 Cloud on-demand dialog fallback 已实现；`3000ms` Client-clock gate、exact identity、
> strict-new-frame、probe-only no-competition 和 demand revoke 均有双仓合同测试。Client `27/27`、
> Cloud `45/45`、双 compile/DTO/diff-check 通过。父级 review 与 fresh runtime 仍是开放门。
>
> **COMBAT-OBS-P1 SOURCE+TEST PASS（2026-07-23）：** 父级 Review #2
> `P0/P1/P2=0/0/0 / OWNER RELEASED`。Client 三组入战机械匹配不上传 ROI，Cloud 保持唯一
> 战斗业务状态机；动态 `coordinate-strip` 的 active-pathing/combat-exit 需求并集与三重 fresh
> 栅栏已有生产合同覆盖。Client `29/29+1/1`、Cloud `49/49`、双 compile、DTO `16/16` 通过。
> fresh runtime 需验证真实进战、普通退战、快速退战、导航终态和多窗口无串扰。
>
> **TURN-40G Stage 6 SOURCE+TEST PASS（2026-07-23）：** 父级最终 Review #27
> `P0/P1/P2=0/0/0 / OWNER RELEASED`。最终零引用门、同序 fact/ROI、replacement stale fence、
> repeated-CURRENT stationary clock 及三任务消费回归均通过；Cloud `70/70`、consumers/replay
> `3/3`、Client `8/8`、双 compile、DTO `16/16`。源码测试验收已关闭，fresh runtime 仍须验证
> pathing terminal、prepared action、真实 `IN_COMBAT` 与多窗口无串扰。
>
> **TURN-40G Stage 5 PASS / Stage 6 gate（2026-07-23）：** 父级 Review #24
> `P0/P1/P2=0/0/0`；Cloud `68/68`、consumers `2/2`、tracker replay `1/1`、Client `8/8`
> 与双 compile 通过。最终关闭要求 `CloudWholeTaskObserver` 对 task turn、`TurnGameClient` 和
> runtime local-service 为零引用，同时 pathing/combat/timer/dialog/tracker 事实与三任务消费回归全绿。
>
> **GAMESTATE-OWNER-P1 source gate（2026-07-22）：** `696a12b0` 的 `isSameMapName` /
> `isNearCoordinate` 已收口到 Cloud `NavigationService` 唯一 owner；修罗/五倍无语义副本，五环只转发。
> 空 target map 坐标比较与负容差钳0恢复，父级`P0/P1/P2=0/0/0`、Cloud compile通过。fresh验收需证明
> `灵兽村(117,70)` 对 `(117,69)`/tolerance 5 直接进入 NPC smart-click，不得重开小地图。

> **TURN-40G Stage 4/5 gate（2026-07-23）：** Stage 4 已由父级 Review #23 以 `0/0/0`、
> Client `8/8`、Cloud `60/60`、consumers `2/2`、tracker replay `1/1` 与双 compile 关闭。
> Stage 5 必须证明所有 destructive consume、gate、settlement、补抓/drag/input 只在 exact owning task
> 持 turn 后执行；Observer 只发布事实/候选，uncertain/BUSY 保持可重试且确认后恰好一次。
>
> **TURN-40G Review #11 gate（2026-07-22）：** 模板命中不等于执行链闭合。fresh验收必须依次出现
> `prepared=true`、`PREPARED_ACTION_READY operation=XIULUO_ENTER_BATTLE target=xiuluo.enterBattle`、
> prepared槽一次消费、Client原子move/wait/click及`IN_COMBAT`。源码合同`7/7 + 19/19`通过，runtime门开放。

> **CR212 member / Xiuluo enter-battle source gate（2026-07-22）：** Cloud日志证明四个非代表窗口均为
> `MEMBER`，失败点是`AutoBattleTask` Spring构造而非角色判断；构造器、effective-task ACK/UI与终态原因
> 已闭合。修罗观察栅栏接受exact queue-run的colon-delimited child run，单ROI上限为`640KiB`。源码门
> `P0/P1/P2=0/0/0`；fresh gate要求重启双端后四队员显示/运行自动战斗且修罗dialog产生实际点击。

> **Observation payload repair passed（2026-07-22）：** 双端整包包络4MiB、单ROI后续修订为`640KiB`、最多8张；
> 超旧256KiB的五图真实HTTP往返成功，超4MiB声明长度返回413。源码测试门`0/0/0`，重启双端并确认
> ROI不再报`exceeds 262144 bytes`、Cloud发布`PATHING_TERMINAL`后才关闭fresh gate。

> **Observation payload runtime reopen（2026-07-21）：** 五ROI拍全部因完整Base64 JSON超过旧`256KiB`
> 在Client fail-closed，空heartbeat成功不能算frame成功。双端整包边界与8张×单图256KiB合同闭合并通过
> 真实超旧上限正向、超新上限负向HTTP测试前，状态为`P1 REPAIR PROPOSED / NOT FRESH READY`。

> **Observation HTTP starvation gate（2026-07-21）：** Cloud HTTP执行器不得让阻塞式turn长轮询占满4个
> core后把observation POST压入队列。现改为有界`32/32`执行器并补真实HTTP五ROI及并发饥饿合同；Client
> `6/6`、Cloud `19/19`通过。必须重启双端JVM并证明观察发送成功、Cloud不再持续
> `observation frame unavailable`、流程进入`NPC_CLICK_SMART`，才算fresh runtime通过。

> **NAV observer fresh-frame gate（2026-07-21）：** pathing静止证据必须来自不同`observerSeq`的Client采集帧，
> elapsed使用`capturedAtMs`，不得使用Cloud排队时间；重复latest ROI零状态推进、零terminal发布。定向`31/31`
> 已通过，仍须重启Cloud后fresh证明移动中不再二次开图补点。

> **NAV regression Review #1 gate（2026-07-21）：** exact-window proof与关图时序接受；keep-turn handback须
> exact intent匹配，关图失败兜底须恢复generic-window close而非二次toggle。两P1及TURN-40G taskRun栅栏关闭前不可formal fresh。

> **TURN-40G Review #4 gate（2026-07-21）：** Client `26/26`、Cloud `17/17`和五窗口registry/runtime
> `31/31`均已通过；剩余P1是旧Observation Runner缺exact taskRun输入栅栏。restart-overlap零matcher/input/event
> 与旧run不能释放新claim的合同关闭前，不可formal fresh。

> **TURN-40G Review #3 gate（2026-07-21）：** exact String identity已通过，但local-kanda启用必须由单一
> paired interest+schedule原子转换发布；round必须大于0。碰撞隔离、partial拒绝零mutation和replacement并发
> 测试缺一不可，测试数未增加不得过门。

> **TURN-40G local-kanda reachability gate（2026-07-21）：** 验收必须由真实Cloud
> `XiuluoTaskV2` shortcut入口驱动probe-only interest、25秒anchor和同attempt schedule，并证明anchor前零输入、
> anchor后current-attempt单次CAS可达。手工fixture设置interest/schedule不能单独过门。

> **TURN-40G Step 5 acceptance correction（2026-07-21）：** 验收必须证明本地`kanda2`与Cloud完整dialog
> 兜底是两条不同识别路径；Cloud仓kanda资产新增数=0，local matcher复制数=0，Cloud verdict仍保留
> coordinate / explicit `CLOUD_NO_ACTION` / unavailable三态和三次实际成功重按预算。

> **TURN-40G acceptance gate（2026-07-21）：** 正式thin-client验收新增本地Observation Runner门：
> observation不得占普通turn action slot；每窗口单in-flight/ACK/backpressure/stale fencing必须闭合；修罗
> `local-kanda`只在current attempt复验+CAS后进入唯一输入队列，普通miss零Cloud，terminal才单次fallback，
> `ENTER_BATTLE_CLICKED`不得替代`IN_COMBAT`。本卡source review和五窗口fresh runtime通过前不得称迁移完成。

> **TURN-40F parent final pass / formal runtime ready（2026-07-21 05:46 EDT）：** source review
> `P0/P1/P2=0/0/0`; 49 focused tests, both tests-enabled compiles, and both diff checks pass. The prior TURN-41
> production data cutover gate is no longer blocked by TURN-40F. Formal user runtime testing is open, while remaining
> P2 cleanup and fresh-runtime observations still prevent a claim that the whole migration is complete.

> **TURN-41 source active（2026-07-20 20:22 EDT）：** canonical claim and parent ACK are complete. No test
> readiness is implied until exact target data verification and parent review pass.

> **TURN-41 ordered gates（2026-07-20 20:19 EDT）：** worker data cutover is active first; only after exact
> scope, backup/merge, counts `22/460/80`, samples `600/1000`, map-bounds SHA, and real-owner reads pass may the
> user fresh-runtime gate open. Current status remains not test ready.

> **TURN-40F source review closed（2026-07-20 20:14 EDT）：** Repair #7 parent result is `0/0/0`; focused
> tests `6+2` and Cloud compile PASS, 62/62 assets match baseline, and duplicate files/references are zero.
> User testing remains blocked until TURN-41 exact scoped data cutover is completed and verified.

> **Repair #7 communication recovered（2026-07-20 20:03 EDT）：** all three parent messages are ACKed and
> implementation is active. The new unrelated `.codex-audit-legendary-game/` baseline subtree is protected and
> excluded; it does not alter Repair #7 or TURN-41 acceptance.

> **Repair #7 resumed（2026-07-20 19:59 EDT）：** implementation is active again under the existing owner.
> Acceptance remains blocked until canonical re-delivery closes every correction and asset gate below and the
> parent final review returns `0/0/0`.

> **Baseline protection update（2026-07-20 19:49 EDT）：** `.codex-audit-h5-mir/` is a third unrelated
> independent repository under the read-only baseline. It is excluded/protected and does not change Repair #7
> or TURN-41 acceptance gates.

> **Baseline protection update（2026-07-20 19:44 EDT）：** `.codex-audit-legend-web/` is also unrelated,
> excluded and protected. Neither nested worktree changes CR271 acceptance.

> **Baseline protection note（2026-07-20 19:39 EDT）：** the unrelated nested
> `.codex-audit-CQWebGame/` repository is not acceptance input and must remain untouched. It does not change
> Repair #7 or TURN-41 acceptance gates.

> **TURN-40F Repair #7 worker state（2026-07-20 19:14 EDT）：** communication and activity are stale;
> acceptance remains blocked with the same owner and write set. No Maven/runtime/input verification was run.

> **TURN-40F Repair #7 acceptance（2026-07-20）：** tests must prove exact-pin recomputation after boundary/
> center changes, a non-constant affine correction field, screen-space outlier exclusion, singular-fit rejection,
> and weighted-residual rejection through the Cloud production owner. Symmetric samples alone are insufficient.
> Asset acceptance additionally requires 62/62 baseline map labels under the real `templates/map_label` consumer,
> exact `铁匠屋.png` SHA, and zero packaged files under the duplicate zero-reference map-label root.

> **TURN-41 data gate（2026-07-20）：** before fresh runtime, resolve the actual `tenantId/userId/stateRoot`,
> back up any existing scoped targets, import the three canonical stores with schema-compatible merge, and verify
> dialog/vision/route entry counts `22/460/80`, vision samples `600/1000`, and map-bounds SHA-256
> `4428F7F998C11AC787A27C1DEE98D186DEB97D9A24307F2E1BD4224FB8E8A74B`. Legacy `data` stores are evidence only.

> **TURN-40F Review #4 resource gate（2026-07-20）：** six Wubei production templates in Cloud must match
> the current read-only baseline bytes. Four templates deleted by that baseline are still packaged in Cloud with
> zero production references and a stale README; remove them only after re-proving zero references. Review is
> `P0/P1/P2=0/4/2`; TURN-41 remains blocked.

> **TURN-40F wrapped-route geometry gate（2026-07-20）：** Cloud must absorb the current baseline's
> post-match yellow-pixel correction for wrapped route destination click centers. Text concatenation alone is not
> equivalent. A marked testcase replay and production-entry contract are required. Review is now
> `P0/P1/P2=0/5/2`.

> **TURN-40F startup full-chain gate（2026-07-20）：** acceptance covers all three Alt+1 options, Alt+U
> unchecked, Alt+5/Alt+6 verification, flying status, FiveRing background-first/UNKNOWN behavior, queue
> idempotence, and leader/member/debug/identity/left-top ordering. Cloud owns policy; the client exposes only
> exact-window typed mechanics.

> **TURN-40F live-role preflight gate（2026-07-20）：** `role-detection-enabled=true` must have a production
> turn consumer before task dispatch. Cloud must classify tooltip/OCR/status facts and apply the baseline member/
> leader/solo assignment policy; registration order is not live role truth.
>
> **TURN-40F prepared-action freshness gate（2026-07-20）：** Cloud must preserve the current baseline's
> same-run owner fence around tracker preparation and its combat/pathing/probe/priority suppressions. Stale task-dialog
> actions may be republished only for the same exact window and matching visible dialog while stationary, with the narrow
> Wubei no-pathing exception and 1000ms cooldown. Review is `P0/P1/P2=0/7/2`.

> **TURN-40F residual acceptance expansion（2026-07-20）：** thin-client acceptance now explicitly covers
> client Service and OCR business owners plus old remote/decision sidecars, not only the three thick Tasks.
> TURN-41 stays blocked until Cloud-default start, client phase retirement, Service/OCR thinning, dual-stack
> removal, dual compile, authorized tests, and parent source review all pass.

> **TURN-40E completion correction / TURN-40F（2026-07-20）：** source-delta compile/review evidence remains,
> but thin-client completion is not accepted: production UI still starts the local thick task path, and remote start
> has no caller. TURN-41 is `BLOCKED / TURN-40F REQUIRED`; runtime acceptance must not begin until the Cloud-default
> start path and client thick-task retirement pass source/compile review.

> **TURN-40E Parent Review #3 final（2026-07-20）：** Repair #7 passed at
> `P0/P1/P2=0/0/0`. Wubei/Xiuluo accept-time snapshots use the shared direct-recognition path with the caller's
> absolute origin, closing every prior finding. Stable-source Cloud and client compile both exited 0; no named
> tests ran. The owner is released and TURN-41 is ready for user-initiated fresh runtime acceptance.

> **TURN-40E Repair #7 re-delivery（2026-07-20）：** Review #2 `0/1/0` 唯一 P1 已闭合；两个
> `...FromSnapshot` 均 direct analyze supplied snapshot、无 anchor gate，坐标 origin 原样传递。Cloud compile
> exit 0，named tests 零运行；当前 `PARENT REVIEW #3 PENDING`，TURN-41 仍 BLOCKED。

> **TURN-40E Repair #6 re-delivery（2026-07-20）：** Review #1 `P0/P1/P2=0/1/2` 的 tracker 算法/
> asset P1 与 frame-purpose/Javadoc P2 已闭合；Wuhuan asset SHA 和双仓 177-byte protocol 已复核，双端
> compile exit 0。当前仅 `PARENT REVIEW #2 PENDING`，TURN-41 仍 BLOCKED，未写 Approved。

> **TURN-40E parent source review #1 BLOCKED（2026-07-20）：** `P0/P1/P2=0/1/2`。tracker Cloud
> recognition/asset equivalence 与双仓协议 byte-identical 门未满足；即使双端 compile 已成功，也不得进入
> TURN-41 fresh runtime。原 Worker owner retained 返修。

> **TURN-40E whole-card delivery（2026-07-20）：** 23 路径 ledger、10 行为簇、双仓 SHA/mtime、
> asset/data 分类和零卡外写入证据已写入原卡；双端 main compile exit 0。named tests 未授权且零运行，
> 当前仅 `SOURCE+TEST DELIVERED / PARENT REVIEW PENDING`，不得视为 Approved 或放行 TURN-41。

> **TURN-40E Repair #5（2026-07-20）：** 验收要求双仓 pure mask owner byte-identical，client tracker/NPC
> mechanics 无重复 `DEFAULT_MASKS/copyWithDefaultMasks`；不得出现 Spring/capture/OCR/ROI/temp dependency。
> main/Cloud compile 均成功后才可 delivery；TURN-41 继续 BLOCKED。

> **TURN-40E Repair #4（2026-07-20）：** first-aid 验收必须证明 bars capture/空 targets 零鼠标动作；
> 非空 targets 单 command 内保持逐 target 右键+800ms，末尾恰好一次安全点 move+300ms，且 forbidden region
> 永不被选择。当前 SOURCE ACTIVE，TURN-41 BLOCKED。

> **TURN-40E Repair #3（2026-07-20）：** 验收必须证明 ordinary yellow-target 仍应用 profile region masks，
> 且仅 Alt+A direct-combat 关闭它；Cloud 唯一 mask 算法不得复制，profile/threshold/component/order 不变。
> 当前 SOURCE ACTIVE，尚非交付；TURN-41 继续 BLOCKED。

> **TURN-40E Repair #2（2026-07-20）：** 已用现有 turn `LOCAL_SERVICE` 冻结 tracker local mechanics 的
> 强类型闭包及精确 cache/ROI/masked fallback/drag/post-drag capture 顺序；Cloud second cache 必须删除。
> 当前 SOURCE ACTIVE，尚非 delivery/review；TURN-41 继续 BLOCKED。

> **TURN-40E PCB-02（2026-07-20）：** run-count 同协议闭包与 `0=无限` UI 已实现，双端 compile 成功；
> tracker 的 `WindowRuntimeContext` 唯一 cache、cached ROI、masked full fallback 仍无 turn-native 调用闭包。
> TURN-40E/41 保持 BLOCKED；不得以 Cloud map 或 dormant remote fact 代替唯一属主架构。

> **TURN-40E PCB-01（2026-07-20 00:13 EDT）：** canonical claim 后的 Wave 0 发现 remote start contract
> 不携带修罗/五倍 run count，Cloud prototype Task 读取 Cloud 全局 `BotProperties`，无法满足 exact
> window/taskRun 的 `0=无限`/正数语义。TURN-40E 为 `PLAN-CONTRACT BLOCKED`；先补冻结写集，当前
> production/resource 零写入、Maven/runtime/tests 均未执行。

> **TURN-40E（2026-07-20 00:05 EDT）：** post-696 本地逻辑等价迁移整卡已建立。验收要求 23 路径
> ledger 无遗漏、10 行为簇等价、无第二算法/store/protocol、双端 compile 成功、父级 P0/P1/P2=0/0/0；
> 未通过前 TURN-41 不得 fresh runtime。

> **CR271 post-696 local delta gate（2026-07-20 03:25 EDT）：** TURN-40B/C/D 原 review 不回退；但用户
> 当前本地 workspace 的 23 个 production Java 路径及资产/数据差异尚未全部证明已进入 CR271，故 TURN-41
> 现为 `BLOCKED / POST-696 LOCAL DELTA MIGRATION REQUIRED`。Wave 0-6、双端构建、授权 tests 和父级
> 0/0/0 终审全部闭合后，方可恢复用户 fresh runtime gate。

> **CR271 TURN-40D pass ACK closed（2026-07-19 22:56 EDT）：** A 已确认 owner release，通信恢复终态；
> 40B/C/D 链全 PASSED，当前唯一下一门为 TURN-41 用户 fresh runtime。

> **CR271 TURN-40D Review #3 PASSED / TURN-41 READY（2026-07-19 22:48 EDT）：** selected-entry、
> cleanup policy、22T 与 compile 全闭合；Maven aggregate 卡外债保留。下一验收为用户 fresh runtime。

> **CR271 TURN-40D Repair #2 source active（2026-07-19 22:41 EDT）：** Review #2 对症源码已开始落盘，
> 但 Java writer 仍 active、A 未 ACK `2216+2226`、无 canonical re-delivery；TURN-41 继续关闭。

> **CR271 TURN-40D Parent Review #2 BLOCKED（2026-07-19 22:26 EDT，0/1/0）：** Repair #1 的 JavaDoc、
> mutex、start attach/ack/resend、pause/resume、same-task control 与 stop/unregister 可接受；selected-task public
> entry 和 §19.5 failure cleanup 仍缺 executable proof。九路径内返修，不开放 TURN-41 user gate。

> **CR271 TURN-40D repair source active / communication stale（2026-07-19 22:16 EDT）：** Review #1 消息连续
> 两轮未 ACK，通信 stale；但两个 test 路径已变为 `03030069`/201L、`29C96D0A`/733L，故 writer active、
> owner 保留且不标 active stale。当前仍不可验收；须 ACK、fresh re-delivery、父级复审及稳定后 build gate。

> **CR271 TURN-40D Parent Review #1 BLOCKED（2026-07-19 22:00 EDT，0/1/1）：** 当前 12T 不满足 §19
> 声称的 control/lifecycle 覆盖：public start/pause/resume/stop、4-arg start attach/ack、mutex、failure cleanup
> 均未被执行。main compile 通过不能替代缺失行为证；修复后须重跑授权 family、compile、canonical delivery。

> **CR271 TURN-40D R2 ACK / ninth path active（2026-07-19 21:55 EDT）：** 三消息全部 ACK，卡恢复推进。
> existing loop test=`0085BCB8`/334L 已落所需 final-stop WIP 场景，整卡 6/9；尚非验收或 delivery。
> Java 稳定后须两类 named family、compile、canonical 9-path 交付和父级逐文件终审。

> **CR271 TURN-40D communication recovered / R2 ACK pending（2026-07-19 21:45 EDT）：** A 已 ACK
> `...2111+2121`，但 R2 `...2141` 尚未回执；整卡保持 5/9 frozen。A 报 compile exit0 与 control test 9/9
> isolate-run，不构成 R2 验收：必须在 ACK 后完成 `WindowRemoteTurnControlContractTest,WindowTurnLoopContractTest`
> named family，再执行适用 compile 并 canonical delivery。

> **CR271 TURN-40D plan-contract repair R2（2026-07-19 21:41 EDT）：** stop lifecycle proof 不能留作 control-test
> prose；新增 tracked-clean `WindowTurnLoopContractTest` 为第 9 路径，使用既有 observable harness 验证
> single stop-bearing exchange、zero returned action execution、then unregister。named family=2 类，5/9 frozen。
>
> **CR271 TURN-40D stop-action source repaired（2026-07-19 21:27 EDT）：** loop=`19B69135`/417L 的 final
> stop-bearing response 已在 ACTION dispatch 前被 stopCheckpoint 截断。源码缺陷闭合；named single-send/
> zero-action/then-unregister proof 与 ACK 仍待，尚不可进入验收。
>
> **CR271 TURN-40D communication stale / source active（2026-07-19 21:21 EDT）：** 两个连续 A event 未 ACK
> `...2111`，通信标 stale；control=`3E2A0D06`/712L 仍活跃，故不标 active stale。A owner、4/8 与 stop-action
> 验收门保持，下一事件须 ACK `...2111+2121`。
>
> **CR271 TURN-40D control source started（2026-07-19 21:17 EDT）：** control=`B7BE569E`/497L、
> guard=`44770301`/233L，distinct progress=4/8；仍为 active WIP，不进入 test/review，stop-action gate ACK 待回。
>
> **CR271 TURN-40D loop repair / stop action gate（2026-07-19 21:13 EDT）：** loop=`868E4BC5`/412L、
> guard=`E9FD87AE`/200L，checkpoint/final exchange/graceful stop 已落但仍不可验收：stop-bearing response 若为 `ACTION` 目前可能进入 executor。
> 交付前必须补 stop checkpoint 响应门及 single-send/zero-action/then-unregister named proof；整卡仍 3/8。
>
> **CR271 TURN-40D R1 ACK / repair active（2026-07-19 21:00 EDT）：** communication recovered；A 正按
> 六项真实 authority、single loop lifecycle owner、exactly-one final stop turn 硬门返修。当前 3/8 不验收。

> **CR271 TURN-40D plan-contract repair（2026-07-19 20:55 EDT）：** 当前 3/8 不可进入验收。
> 新增硬门：六项 authority 必须来自 exact-window/batch baseline 事实；registry/live loop 是唯一 lifecycle owner；
> pause/resume 不重启，stop 必须恰好一次上送 stop metadata 后才停止并 unregister。竞态 guard
> `4AEF9A83`/198L 的直接 stop/await/remove 未过该门，保留待修。

> **CR271 TURN-40D first source batch（2026-07-19 20:40 EDT）：** loop=`569E9F01`、registry=`5315553F`、
> guard=`5DBB924D`。父级初核 start request set-before-start、uncertain resend、matching ack
> correlation；其余 lifecycle/control/test/build 验收未完成，整卡仍 source-active WIP。

> **CR271 TURN-40D canonical claim（2026-07-19 20:25 EDT）：** External A 已在原卡 EOF 完成唯一最早
> whole-card self-claim，当前 `SOURCE ACTIVE / RECON`。固定 8-path 验收不变；C anti-race 退出，暂无
> Java/Maven/build 新证据，父级等待 canonical source+test delivery。

> **CR271 TURN-40C Review #2 passed / TURN-40D READY（2026-07-19 20:14 EDT）：** 40C 终审
> `0/0/0`，35T 与 compile 证据接受。40D 验收冻结为：exact-window local/remote mutex；stable startRequest
> 原样重送至 matching ack；pause/resume 只影响 Cloud checkpoint；stop 单次传递、不等业务 retry，loop stopped
> 后才 unregister；start/failure cleanup 无残留 registry。固定 8-path，READY/ZERO OWNER。

> **CR271 TURN-40C repair source active（2026-07-19 20:14 EDT）：** P1 两项源码条件已落盘：runtime bean
> 禁用 destroy inference，activation 第 8 test 断言 annotation 空 destroy method。尚非通过：需 A ACK、
> 35T family（8+3+24）、compile/test-compile 与 canonical 15-path re-delivery 后父级复审。

> **CR271 TURN-40C communication + repair active stale（2026-07-19 19:59 EDT）：** Review #1 repair
> 连续两轮无 ACK 且 15-path 返修源码超 10 分钟无变化；A owner 保留，不释放。当前仍不满足 TURN-40C
> source gate，TURN-40D/41 不因 stale 自动开放；no-second-close proof、34-test family/build 仍为唯一恢复门。

> **CR271 TURN-40C parent source+test review #1 blocked（2026-07-19 19:47 EDT）：** `P0/P1/P2=0/1/0`。
> Spring 默认 bean destroy inference 会让 host context 第二次调用 runtime `close()`，所以当前 7T 未证明
> 冻结的 exact-once runtime→host→server→executor close ownership/order。验收须明确禁用 runtime bean
> destroy inference，并以 activation contract test 捕获重复 close；34-test family/build 复绿后再审。

> **CR271 TURN-40C communication stale / source active（2026-07-19 19:30 EDT）：** R5/R1 ACK 连续两轮
> 缺失，communication stale；activation test=`864BFC9F`/7T 的新字节证明 source active，故不标 active stale。
> owner/15-path 不变；canonical delivery 前不作最终 source/test verdict，也不运行父级 Maven。

> **CR271 TURN-40C R5 assembly source landed（2026-07-19 19:28 EDT）：** 源码已显示 Server 唯一
> `DecisionEngine` 同时服务 legacy routes 与 host，Host refresh 前注册 exact object 且保旧 overload；activation
> test 已走新 seam。仍须 canonical delivery 证明 same-instance assertion、完整 refresh、冻结 named family 与 build。

> **CR271 TURN-40C R5 scoped bean landed / registry restored（2026-07-19 19:26 EDT）：** 已核实
> `CloudTurnRuntimeConfiguration=4E91D53E` 通过现有 scope root 构造真实 OCR memory bean，满足 no-global-
> config/no-second-store 验收；R5 ACK、同一 DecisionEngine host registration、refresh/named/build/delivery 仍待。
> 第 16 节按 C1-C4 原卡最终 verdict 补回四行，恢复 88 张且不改变 owner/gate。

> **CR271 TURN-40C plan-contract repaired R5（2026-07-19 19:24 EDT）：** R5 取代 R4。验收除同一
> `DecisionEngine` 实例外，必须证明 OCR memory bean 由现有 `CloudServiceStorage` tenant/user scope root
> 构造，不落进程全局 `config/`、不产生第二 store。仍为 15-path；A ACK R5 后恢复。

> **CR271 TURN-40C plan-contract repaired R4（2026-07-19 19:18 EDT）：** 父级全图审计确认完整闭包只余
> `OcrRoiMemoryService` import 与同一 `DecisionEngine` host registration。验收要求 host refresh 越过完整图，
> `NavigationService` 注入的必须是 Server route 使用的同一实例；不得扩大 scan、修改依赖源码或创建第二算法。
> 仍为 15-path，A ACK 后恢复；named family/compile/test-compile/delivery 尚待，foundation 7 error 继续按卡外
> decimal-HWND fixture collision 单列。

> **CR271 TURN-40C communication recovered / audit continues（2026-07-19 19:05 EDT）：** A 已三重 ACK
> `1848+1855+1859` 并暂停 imports，stale 清除；R3 竞态待 ACK。父级一次性分类 broad scan eager graph
> 与四 task 真实依赖，卡继续 blocked、不恢复 Java/Maven、不向用户索要业务选择。
>
> **CR271 TURN-40C plan-contract blocked / full import closure（2026-07-19 19:04 EDT）：** A 已 ACK core R2，
> `TurnGameClient` 修复有效，通信 recovering；但 config=`15E6F1E7` 逐层 imports 后又露非扫描
> `OcrRoiMemoryService`，证明现有 constructor DAG 合同未闭合。A owner/WIP 保留、暂停 Java/Maven；父级
> 一次性审完整 bean/import closure。无业务选择，禁 stub/scan 扩大/复制业务。
>
> **CR271 TURN-40C communication stale（2026-07-19 18:59 EDT）：** A 连续两轮未 ACK Repair R2 原消息与
> R1 reminder，现标 `COMMUNICATION_STALE`；owner/15-path WIP 保留，下一事件须三重 ACK。源码变化不足
> 10 分钟，未标 `ACTIVE_STALE`；无 delivery/review，父级不跑 Maven。
>
> **CR271 TURN-40C fifteenth path landed / ACK pending R1（2026-07-19 18:55 EDT）：**
> `TurnGameClient=1B203987`/221L 已完成 production ctor `@Autowired`，test seam/private ctor/`bind()`/业务不变。
> A 第一轮漏 Repair R2 ACK，未达 stale；15-path source active，host refresh/named/build/delivery 待。
>
> **CR271 TURN-40C plan-contract repair #2（2026-07-19 18:48 EDT）：** 14-path 已实现且 Cloud
> compile/test-compile `EXIT 0`，factory/runtime tests=`FEFB6DC2`/`DB3A486A`。父级核完两个 scan root，
> 确认 `TurnGameClient` 是唯一未闭合多构造器 bean；原卡扩为 15-path，仅生产 ctor 加 `@Autowired`，
> seam/`bind()`/业务不变。A ACK 后恢复 host refresh 与授权 family；无用户选择，尚非 delivery/review。
> foundation 23/30 的 7 ERROR 已核为卡外 untracked `remote/run` decimal-HWND 校验/fixture collision，发生于
> 40C authority seam 前；作为独立 build gate 保留，不扩大 15-path 合同。
>
> **CR271 TURN-40C runtime test WIP（2026-07-19 18:34 EDT）：** runtime contract test 已变为
> `1D9D32D3`/825L；factory test 未变。test-compile/named family/delivery 均未形成，不能给验收结论。

> **CR271 TURN-40C production mechanics complete / main compile EXIT0（2026-07-19 18:32 EDT）：** runtime
> 实盘顺序已核为 descriptor exact context→provider 前 bind→全 materialize/identity check→install/worker/ack→
> same-context execution。A 报 main compile EXIT0；test updates/test-compile/named family/canonical delivery 待。

> **CR271 TURN-40C source active / mechanics 1+2+4 landed（2026-07-19 18:27 EDT）：** authority/assembly/
> config 已按 sole-authority + fresh holder-backed handle 合同落盘，factory fixed descriptor 已开始；runtime 与
> tests 未变。当前只是源码增量，尚未形成 canonical delivery 或验收结论；A active 时父级不跑 Maven。

> **CR271 TURN-40C source active / first repair-batch increment（2026-07-19 18:17 EDT）：**
> `PlayerStateService=1E932914` 与 `CloudTurnRuntimeConfiguration=64A54422` 已按冻结 mechanics 落盘；
> 当前仅证明构造器选择、真实基础 bean 与 exact-context prototype startup gate 的源码增量，尚未形成
> canonical delivery 或验收结论。其余 acceptance 与授权 test family 不变；A active 时父级不跑 Maven。

> **CR271 TURN-40C implementation resumed（2026-07-19 18:07 EDT）：** A 已具名 ACK repaired contract，
> 状态转 `IMPLEMENTING_WHOLE_CARD`。当前只做 14-path 重读，验收与授权 test family 不变；active writer
> 期间父级不跑 Maven。

> **CR271 TURN-40C plan contract repaired（2026-07-19 17:57 EDT）：** 父级已冻结 14-path 完整闭包并
> 解除 A 的 Java/Maven 停令。验收固定为 exact context-before-prototype、真实 startup gate、唯一共享 turn
> authority、host refresh、四 prototype pre-ack 构造但 startup 零执行；无用户选择、无业务差异。

> **CR271 TURN-40C communication recovered（2026-07-19 17:22 EDT）：** A 已回执核心合同裁定，
> Java/Maven 保持停止、7-path WIP 冻结、owner 保留；stale 清除。主验收仍等待按 696 冻结完整装配闭包，
> 18:22 terminal recovery ACK 已闭合全部父级消息；无用户选择。

> **CR271 TURN-40C communication stale（2026-07-19 17:16 EDT）：** External A 连续两轮未 ACK 父级
> 合同裁定；canonical owner 不释放，7-path WIP 保留，Java/Maven 继续暂停。主验收仍等待按 696 冻结
> 完整 Spring/runtime 装配闭包，不需要用户选择。

> **CR271 TURN-40C plan-contract blocked（2026-07-19 17:05 EDT）：** 7/7 authored，main compile/
> test-compile exit 0；named 7 个为 2 PASS/5 host-refresh ERROR。主验收等待父级按 696 冻结完整 Spring/runtime
> 装配闭包；不是用户业务选择，不得以 stub、scan narrowing 或弱化 exact authority 放行。
>
> **CR271 TURN-40C build progress（2026-07-19 16:45 EDT）：** Server=`9A3B17AB`/195L 后 production
> 6/6 齐，activation test absent；Cloud main compile `EXIT 0`。主验收仍等 test/delivery/review，无用户选择。
>
> **CR271 TURN-40C source progress（2026-07-19 16:38 EDT）：** Routes=`063DE4FC`/94L 加入批次，
> 当前 5/7；Server 未变、test absent。主验收仍等 delivery/review，父级不跑 Maven。
>
> **CR271 TURN-40C source progress（2026-07-19 16:37 EDT）：** Handler=`01DE94A2`/399L 完成当前批次，
> 当前 4/7 有增量；Server/Routes 未变、test absent。主验收继续等待 delivery/review；A writer active，
> 父级不跑 Maven，无用户选择。
>
> **CR271 TURN-40C source progress（2026-07-19 16:25 EDT）：** 固定写集当前 3/7 有增量：Application=
> `5711BC3E`、Host=`E90F22C8`、RuntimeConfiguration=`D4636072`；其余三 production 未变、activation
> test absent。主验收仍等 canonical delivery/review；A writer active，父级不跑 Maven，无用户选择。
>
> **CR271 TURN-40C source progress（2026-07-19 16:19 EDT）：** 固定写集第 1/7 个文件
> `CloudTurnRuntimeConfiguration=D4636072`/105L 已创建；五个 MODIFY SHA 未变，activation test absent。
> 主验收继续等待 canonical delivery + 父级 review；无业务选择，A writer active 时父级不跑 Maven。
>
> **CR271 TURN-40C claimed（2026-07-19 16:04 EDT）：** A 为 Cloud activation 固定 7-path 唯一 owner；
> C 的后到 claim 已按 physical order 自撤、零源码。实现尚未产生字节，主验收等待 A canonical delivery
> 与父级 source+test review；无业务选择。

> **CR271 runtime/factory Review PASSED / TURN-40C READY（2026-07-19 15:51 EDT）：** re-delivery #3
> 证据与父级实测一致，`TURN-40B/RUNTIME-FACTORY` 最终 `P0/P1/P2=0/0/0`，owner 释放。CR aggregate
> test-compile 仍如实 BLOCKED 于 12 个卡外 dirty tests；40C 固定 7-path 合同现 `READY / ZERO OWNER`。

> **CR271 re-delivery #2 Review `0/0/1`（2026-07-19 15:39 EDT）：** 源码修复通过，但 aggregate
> test-compile 卡外阻断证据不完整：交付写 5 个，父级实测 12 个唯一失败测试文件。只需修证据并
> re-deliver，不得改源码；40C/主验收继续 BLOCKED，无用户选择。

> **CR271 runtime/factory repair active（2026-07-19 15:19 EDT）：** External C 已 ACK `0/3/1`
> Review #1 并保留 owner 返修原 17-path。当前没有 canonical re-delivery，故原 BLOCKED verdict 与 40C
> 主验收阻断均不变；无需用户选择。

> **CR271 runtime/factory canonical delivery 未通过（2026-07-19 15:02 EDT）：** Parent Review #1=
> `P0/P1/P2=0/3/1`。必须先让所有 queue transition（含 skip/create failure）暴露 null authority、让 runtime
> 在任何 retained ack 前校验六事实、完成冻结 Maven test-compile/compile，并把 runtime 计数修为真实 22/22。
> 无业务选择/差异；40C 与主验收继续 BLOCKED。

> **CR271 implementation complete / delivery gates pending（2026-07-19 14:52 EDT）：** 六 step 均有 green
> named evidence，consumer runtime=24/24、handler=7/7。full HTTPS family、五 shared byte 同形、双 compile、
> 17-path 物理清单和 canonical delivery 尚未完成，故主验收/40C 继续 BLOCKED。

> **CR271 producer test Review #4 PASSED（2026-07-19 14:42 EDT）：** `DE50232B` 17/17 的 counting handle
> 证明 currentTask/getExecutionContext 各一次，第二次 context 会返回 distinct authority；正例六事实、负例
> 六字段全空。producer 子门 `0/0/0`，consumer 可继续，整卡/40C 尚未通过。

> **CR271 producer projection Review #3（2026-07-19 14:37 EDT）：** detach relative order 已过；投影测试
> 只计 currentTask read，不能证明 executionContext 恰读一次。须 counting handle 让第二读返回不同事实并
> 断 contextReadCount=1；null-context negative 须六字段全 null。producer/consumer 继续 BLOCKED。

> **CR271 producer repair communication stale（2026-07-19 14:32 EDT）：** 17:40/18:04 两轮未 ACK 1404。
> `7A7EABB5` 17/17 不关闭 detach relative-order P1；当前 `BA515A97` 仅为观察字段 WIP。两 detach 必须
> attached-at-clear 真实接线并通过后，才接受 projection/full producer gate。

> **CR271 producer detach-order test repair（2026-07-19 14:22 EDT）：** AUTO_BATTLE 真实 runner order 已绿，
> 但 terminal/getActiveTaskHandle 两测试只看返回后状态，交换 clear/detach 仍绿。验收要求 RecordingHandle
> 在 clear 调用当下观察 runner.currentTask 仍指向自身；两处均闭合后再做 resolveForAction projection。

> **CR271 producer-test communication recovered / executable harness contract（2026-07-19 14:04 EDT）：** C
> 已精确 ACK 三条消息，stale 清除。验收固定为同路径三段真实生产证明：`AUTO_BATTLE` 内层排序、空队列
> 外层 terminal clear-before-detach、resolveForAction 单 handle/context 六事实。`D7B1143E` 13/13 只完成第二
> detach；不得用会启动 observer 的 `WUHuan_V2` 或把内层方法误当 terminal。consumer/40C BLOCKED。

> **CR271 producer-test repair communication stale（2026-07-19 14:00 EDT）：** C 已回到 test recon，但
> 连续两拍未精确 ACK，标 stale。验收仍要求同路径同时证明 runner clear<update<publish<execute、双 detach
> clear 与 TurnExecutionWindow 单快照六事实；仅 projection recon 不足。consumer/40C BLOCKED。

> **CR271 consumer out-of-order WIP（2026-07-19 13:56 EDT）：** Cloud runtime=`53FE8363` 在 producer-test
> Repair #1 未 ACK/未绿时产生，isolated compile 不关闭前置验收。记第一拍漏回执，consumer 不接受为 done；
> 同 test path production order/detach/projection gate 仍先决，40C BLOCKED。

> **CR271 producer-test gate false positive（2026-07-19 13:50 EDT）：** communication 已恢复，A#5 source
> shape 保持 accepted WIP；但 `7F0DCA39` 仅按预期顺序手工调用 handle API，不能证明生产 runner 实际保持
> clear-before-update/publish-before-execute、双 detach clear 或 TurnExecutionWindow 单快照。P1 repair pending，
> consumer/40C 继续阻断。

> **CR271 A#5 source corrected / producer test pending（2026-07-19 13:42 EDT）：** physical source 已为
> clear-before-update + dual terminal clear，单快照投影不变；isolated compile exit0 仅 bounded evidence。
> 连续两拍未精确 ACK 已 communication stale，且现有 test 尚未证明 previous context 不可复用；consumer
> 继续受阻，40C BLOCKED。

> **CR271 A#5 first missed ACK / producer gate not met（2026-07-19 13:37 EDT）：** C 的 producer WIP
> 未在 queue item replacement 前 clear 旧 context，第二 detach 点也未 clear；isolated compile exit0 不能替代
> A#5 stale-authority gate。第一拍漏回执未达 stale，须 ACK+返修后才可进入 consumer；40C BLOCKED。

> **CR271 A#4 done / producer transition repair（2026-07-19 13:32 EDT）：** A#4 gates 5/5+7/7+18/18
> 保持。A#5 要求同 handle queue replacement 先 clear old context，再 update/publish/execute，terminal clear-before-
> detach，并以现有 producer test 证明无 stale authority；写集17，待 ACK，40C BLOCKED。

> **CR271 communication recovered / A#4 first missed（2026-07-19 13:26 EDT）：** A#3+reminder2 精确
> ACK，stale 清除；A#4 第一拍未 ACK、未达 stale。source-level pathing-null 修复未落，17-path 无 delivery，
> 40C BLOCKED。

> **CR271 A#3 ACKed / pathing-null compatibility repair（2026-07-19 13:24 EDT）：** A#3 bounded gates
> 5/5+7/7+18/18；fixture 显式 pathingSnapshot:null 不获接受。A#4 用 metadata `NON_NULL` 保持基线 omission，
> fixture 删除 null，写集仍17。待 ACK/实施，40C BLOCKED。

> **CR271 A#3 communication stale / source active（2026-07-19 13:21 EDT）：** 两轮未 ACK A#3，已
> communication stale；双仓 validator test=`C32D4522`、18/18 是 WIP evidence，非 delivery/review。17-path
> gate 未闭合，40C BLOCKED。

> **CR271 A#2 communication recovered / A#3 first missed（2026-07-19 13:10 EDT）：** C 已 ACK A#2
> original+reminder，core 7/7；A#3 首拍未 ACK、未达 stale。17-path 合同待 ACK/实施，无 delivery，40C BLOCKED。

> **CR271 lifecycle golden contract repair（2026-07-19 13:08 EDT）：** 不撤 shared validator authority 门；
> 双仓 lifecycle test/`request-start.json` 扩入写集，总数 17，以合法 MEMBER/team/non-NORMAL facts 修正 task-start
> 测试输入。core helper/其它 fixtures 只读。C 已报 core 7/7，A#3 尚待 ACK，40C BLOCKED。

> **CR271 contract repair #2 physical correction（2026-07-19 13:06 EDT）：** metadata=`D22B62D9` 与
> validator=`56383C98` 已在双仓按 Amendment #2 修正并逐字节一致；但 C 尚未 ACK 两条消息，也没有当前
> build/test/canonical delivery，故不构成验收通过。第一次漏回执未达 stale，40C BLOCKED。

> **CR271 contract repair #2 first missed ACK（2026-07-19 13:02 EDT）：** `4799662C` validator 是未读
> Amendment #2 后的 WIP，不能作为 missing-vs-false 验收证据。第一次漏回执，C owner retained，40C BLOCKED。

> **CR271 TURN-40B metadata contract repair #2（2026-07-19 12:56 EDT）：** authority booleans 必须为
> boxed Boolean，六项仅 non-null 时序列化；旧 ctor 六项全 null，合法 false 必须在线。双仓 core golden
> test/fixtures 只读并原样通过。C owner retained，40C BLOCKED。

> **CR271 TURN-40B/RUNTIME-FACTORY baseline amendment ACK（2026-07-19 12:51 EDT）：** C 已具名 ACK
> DHXY 7 + Cloud 6 路径与 baseline authority 门，P1-5 转 SOURCE_ACTIVE；尚无 amended delivery。A idle，
> 父级不跑 Maven，40C BLOCKED。

> **CR271 TURN-40B/RUNTIME-FACTORY baseline authority decision（2026-07-19 12:35 EDT）：** 用户明确
> 迁移必须保持旧代码行为，A/B 分叉固定为 A，B 未批准。exact active `TaskExecutionContext` 的 role/team/
> startup 事实须经既有 shared metadata 双仓同形透传并由 Cloud 严格消费；缺失须在 ack/materialize 前拒绝。
> 扩展整卡尚未交付，C owner retained，40C BLOCKED。

> **CR271 TURN-40B/RUNTIME-FACTORY repair complete except P1-5（2026-07-19 12:28 EDT）：** 当前七文件
> 隔离编译 exit 0，factory 2/2 + runtime lifecycle 20/20=`22/22`；P1-1..4/P2-1 已闭合但尚未 canonical
> re-delivery/review。P1-5 待用户 A（推荐）/B，未决前不得 re-deliver。

> **CR271 TURN-40B/RUNTIME-FACTORY Review #1（2026-07-19 11:47 EDT）：** 7/7 delivery 的 build
> evidence 为 named 11/11、Cloud testCompile/compile EXIT0，但 parent source+test verdict=
> `P0/P1/P2=0/5/1 BLOCKED / REPAIR REQUIRED`。prototype isolation、pre-terminal exact ack/worker、exact
> device+window dedupe、exception/aggregate/close cleanup、role/team/startup authority 与完整 LIFE/TASK/STATE tests
> 均为 BLOCKED；C owner retained，TURN-40C BLOCKED。唯一用户决策：A（推荐 protocol facts）或 B（批准默认差异）。

> **CR271 / runtime-factory runtime WIP updated (2026-07-19 11:42 EDT):** Production runtime advanced
> from `30128CFD` to `704650C7` (9646 bytes, 201 lines); current runtime/test is `704650C7` + `598FD192`.
> Physical scope remains 7/7, but current-byte build/test evidence and delivery/review are absent; 40C remains blocked.

> **CR271 / runtime-factory final-test WIP updated (2026-07-19 11:39 EDT):** Runtime contract test advanced
> from intermediate `C0C81975` to `598FD192` (19002 bytes, 427 lines). Physical scope remains 7/7, but final
> build/test evidence and delivery/review are absent; no parent Maven, and 40C remains blocked.

> **CR271 / runtime-factory physical source+test 7/7 (2026-07-19 11:37 EDT):** Final runtime contract test
> `C0C81975` (18083 bytes, 412 lines) appeared and all seven fixed paths now exist. The final test has no Worker
> build/test report; no delivery/review, no parent Maven, and 40C remains blocked.

> **CR271 / runtime-factory source+test increment 6/7 (2026-07-19 11:32 EDT):** Factory allowlist test
> `F274A975` (6379 bytes, 129 lines) appeared and C reports it passes 1/1. Only the runtime contract test remains
> absent; no delivery/review, no parent Maven, and 40C remains blocked.

> **CR271 / runtime-factory production build activity (2026-07-19 11:21 EDT):** C reports all five
> production files compile clean together with exit 0. State remains 5/7 with two tests absent; no delivery/review,
> no parent Maven, and 40C remains blocked.

> **CR271 / runtime-factory increment 5/7 (2026-07-19 11:20 EDT):** Core runtime `30128CFD`
> (9403 bytes, 197 lines) appeared; all five production paths now exist and only two tests remain absent.
> Runtime/control-port build evidence is pending; no delivery/review, no parent Maven, 40C blocked.

> **CR271 / runtime-factory increment 4/7 (2026-07-19 11:18 EDT):** Registry `576B2DEA` now has a
> Worker-reported single-file compile EXIT0; control port `56DA5571` (1806 bytes, 42 lines) appeared without a
> Worker build report. Runtime and two tests remain absent; no delivery/review, no parent Maven, 40C blocked.

> **CR271 / runtime-factory increment 3/7 (2026-07-19 11:15 EDT):** C sole owner created registry
> `576B2DEA` (3237 bytes, 73 lines). Factory/start-result retain reported individual compile EXIT0 evidence;
> registry has no Worker build report yet. Four paths remain absent; no delivery/review, no parent Maven, 40C blocked.

> **CR271 / runtime-factory build activity (2026-07-19 11:10 EDT):** C reports factory `B2839BE9` and
> start-result `BE8A15BF` each single-file compile EXIT0. Five paths remain absent; no delivery/review or whole-card
> Maven evidence exists, and TURN-40C stays blocked.

> **CR271 / runtime-factory increment 2/7 (2026-07-19 11:08 EDT):** C created factory `B2839BE9` and
> start-result `BE8A15BF`; five paths remain absent. No delivery/review exists, parent runs no concurrent Maven,
> and TURN-40C stays blocked.

> **CR271 / runtime-factory increment 1/7 (2026-07-19 11:05 EDT):** C created
> `CloudTurnTaskFactory=B2839BE9` (2482 bytes, 53 lines) and reports single-file compile EXIT0; the other six paths
> remain absent. This is not delivery/review or the whole-card build gate. Parent runs no Maven; TURN-40C stays blocked.

> **CR271 / runtime-factory claimed (2026-07-19 10:59 EDT):** External C holds the only earliest canonical
> whole-card claim and is `SOURCE_ACTIVE`; A ACKed and remains idle, with no collision. All seven CREATE paths are
> still absent at reconciliation. Parent runs no Maven while C is the sole Cloud Java writer; TURN-40C stays blocked.

> **CR271 / P-COMPILE passed and runtime-factory ready (2026-07-19 10:49 EDT):** Parent Review #2 is
> `P0/P1/P2=0/0/0`; A owner is released and stale communication is cleared. Parent verification is full
> testCompile EXIT0, four WholeTask named tests `67/67`, and Cloud compile EXIT0. The existing five-production,
> two-test `TURN-40B/RUNTIME-FACTORY` whole card is now `READY / ZERO OWNER / UNASSIGNED`; TURN-40C remains blocked.

> **CR271 / P-COMPILE COMMUNICATION_STALE (2026-07-19 09:19 EDT):** A missed two consecutive ACK
> windows for the stale-status message and is now both `ACTIVE_STALE` and `COMMUNICATION_STALE`. Ownership,
> clean testCompile and frozen production remain; the parent neither terminates the process nor runs concurrent Maven.

> **CR271 / P-COMPILE named-test ACTIVE_STALE (2026-07-19 09:09 EDT):** the five-test Maven process
> remains alive, but no FiveRing/Xiuluo/tracker report or test-source increment appeared after 08:56 for over ten
> minutes. A retains ownership and has been asked to ACK the exact blocked test/wait point; no re-delivery exists.

> **CR271 / P-COMPILE testCompile clean (2026-07-19 08:59 EDT):** all 27 Review #1 testCompile errors
> are cleared. A is running the four WholeTask tests plus the tracker test and repairing remaining runtime fixtures;
> production is frozen and no canonical re-delivery or review exists yet.

> **CR271 / P-COMPILE testCompile progress (2026-07-19 08:54 EDT):** eight of ten fixed test files
> are compile-clean and only `FiveRingTaskTrackerTurnContractTest` remains with 14 errors. WholeTask runtime
> failures, all four named tests and canonical re-delivery remain pending; production stays frozen.

> **CR271 / P-COMPILE Review #1 ACK (2026-07-19 08:39 EDT):** A acknowledged the fixed ten-test,
> test-only repair and remains sole owner with repair active; C is idle and communication is healthy. Production
> and main compile EXIT=0 remain frozen; no re-delivery exists and runtime/factory/40C remain blocked.

> **CR271 / P-COMPILE Review #1 blocked (2026-07-19 08:34 EDT):** production and main compile have no
> finding, but SOURCE+TEST fails its gate with 27 testCompile errors and eight isolate failures. Verdict is `0/1/0
> BLOCKED`; A retains owner for a fixed ten-test harness/compile-only repair while production remains frozen.

> **CR271 / P-COMPILE main compile green (2026-07-19 08:27 EDT):** the fixed four-file repair now
> passes full Cloud main compile with exit 0 and clears all six javac errors. A remains sole owner while producing
> isolated named-test evidence; full-tree testCompile is blocked by out-of-write-set test debt. Delivery/review and
> runtime/factory/40C remain pending or blocked.

> **CR271 / P-COMPILE claim reconciliation (2026-07-19 08:16 EDT):** A's original-card claim is
> physically earlier and is the sole owner / SOURCE_ACTIVE. C withdrew the later claim and confirmed zero source
> writes. The fixed four-file/six-error contract is unchanged; runtime/factory tail and TURN-40C remain blocked.

> **CR271 / aggregate compile recheck (2026-07-19 08:01 EDT):** full Cloud javac replaces the stale
> seven-file description with four production files and six compile errors. `TURN-40B/P-COMPILE` is now
> `READY / ZERO OWNER / UNASSIGNED` for mechanical API-drift closure only; deprecated Navigation, protocol/client,
> stores and context models remain out of scope. Runtime/factory tail and TURN-40C remain blocked.

> **CR271 / P-NAV closure ACKed (2026-07-19 07:56 EDT):** C exact-ACKed Review #2 and owner release;
> P-NAV is CLOSED/PASSED and A+C are idle available. There is no READY card. The aggregate seven-file debt,
> runtime/factory tail BLOCKED/ZERO OWNER and TURN-40C blocked states are unchanged.

> **CR271 / P-NAV Review #2 passed (2026-07-19 07:46 EDT):** `D56DEAFD` + `87C6BC45`/23T is
> `P0/P1/P2=0/0/0 SOURCE+TEST SOURCE REVIEW PASSED`; four current-yellow tests close Review #1 and C owner is
> released. Isolated evidence is 528 main files at zero errors plus 23/23. The aggregate seven-file blocker remains,
> so runtime/factory tail stays BLOCKED/ZERO OWNER and TURN-40C remains blocked.

> **CR271 / P-NAV repair content complete (2026-07-19 07:41 EDT):** test=`87C6BC45`/23T; all four
> deprecated direct-call tests are replaced by current-yellow hit/miss/capture-fail/OCR-fail-closed proof. No old
> helper call remains and LEGACY appears only in negative assertions. Production `D56DEAFD` is frozen; ACK,
> isolated verification and canonical re-delivery remain pending.

> **CR271 / P-NAV repair active (2026-07-19 07:36 EDT):** test changed to `65DEF10A`; the first legacy
> remembered-route test now covers current yellow memory and proves no `LEGACY_GREEN_LINK` query. Three deprecated
> destination-helper direct tests remain in repair. Production `D56DEAFD` is frozen; C ACK is pending, Java writer
> is active, and the aggregate seven-file blocker is unchanged.

> **CR271 / P-NAV Review #1 blocked (2026-07-19 07:19 EDT):** delivery `D56DEAFD` +
> `2FDB2D02`/23T is `P0/P1/P2=0/1/0 BLOCKED`. Production has no new finding, but four tests directly invoke
> the user-excluded deprecated legacy helpers and retain a `LEGACY_GREEN_LINK` fixture while the current yellow
> destination + mini-map path lacks direct equivalent proof. C retains owner for test-only repair; production is
> frozen, P-PROTO/P-CLIENT stay closed, and aggregate build remains blocked by the out-of-write-set seven-file debt.

> **CR271 / P-CLIENT Review #2 blocked (2026-07-19 01:42 EDT):** Repair #1 production is accepted, but the
> failed-replace fixture is rejected by the validator before NOT_EXECUTED, unsuccessful outbound proof and a pending
> route smuggle negative remain absent, and reason nullability JavaDoc contradicts the nonblank protocol. Verdict is
> `0/1/2`; C retains owner for same-two-file, still-33T Repair #2. P-NAV is not opened.
>
> **CR271 / P-CLIENT repair progress (2026-07-19 01:37 EDT):** client `AC14E006`/520L closes result-kind,
> strict shape and JavaDoc findings. Test `D827B8D8`/529L/33T is adding outbound mapping proof. There is no
> re-delivery and P-NAV is not opened.
>
> **CR271 / P-CLIENT communication recovered (2026-07-19 01:32 EDT):** C double-ACKed review and stale
> messages, cleared stale and entered REPAIR_ACTIVE. Client is `AC14E006`/520L while test remains
> `541B4D14`/33T. The same-two-file contract is unchanged and P-NAV is not opened.
>
> **CR271 / P-CLIENT communication stale (2026-07-19 01:27 EDT):** C missed the Review #1 `0/2/1` repair
> ACK for two consecutive audits and is `COMMUNICATION_STALE`. Ownership, delivery hashes, the same-two-file repair
> boundary and exactly 33 tests remain unchanged. P-NAV is not opened.
>
> **CR271 / P-CLIENT Review #1 blocked (2026-07-19 01:22 EDT):** delivery `FFEB7679` +
> `541B4D14`/33T is `P0/P1/P2=0/2/1 BLOCKED`. New operations are absent from result-kind dispatch and the pending
> route field is absent from strict result-shape closure. Tests do not prove outbound operation/payload/reason and use
> a validator-rejected replacement route mode. C retains owner for a same-two-file, still-33T repair.
>
> **CR271 / P-CLIENT source progress (2026-07-19 01:17 EDT):** client production is now
> `FFEB7679`/481L. The test file is actively being written and was last observed at `73D44A6D`/420L/27T; there is
> no frozen 33T or canonical delivery yet. C remains SOURCE_ACTIVE and P-NAV still waits only for P-CLIENT.
>
> **CR271 / A ACK and P-CLIENT recon (2026-07-19 01:07 EDT):** A named-ACKed the P-LOCAL pass/release and is
> idle available. C completed P-CLIENT builder/arguments/result/client reconnaissance; the identified work remains
> inside the frozen client/test files, whose hashes are unchanged. There is no delivery; P-NAV still waits only for
> P-CLIENT.
>
> **CR271 / P-CLIENT claimed (2026-07-19 01:02 EDT):** External C canonically claimed P-CLIENT at the P2 card
> physical EOF and is now sole owner / SOURCE_ACTIVE. The frozen Cloud client/test remain baseline
> `59BF77E8`/414L and `0A248C8B`/417L/27T with no source increment yet. P-NAV still waits only for P-CLIENT;
> no Maven while the Java writer is active.
>
> **CR271 P-LOCAL Review #2 passed (2026-07-19 00:59 EDT):** A's concurrent ACK/Repair #1 re-delivery closes at
> `P0/P1/P2=0/0/0 PASSED`. The 22T suite proves two-runtime exact binding and queued second identity/reason while the
> first live outcome remains; production/runner are unchanged. A owner is released and communication recovered.
> P-PROTO/P-OCR/P-LOCAL are passed; P-NAV now waits only for P-CLIENT.
>
> **CR271 communication audit (2026-07-19 00:57 EDT):** C ACKed P-OCR `0/0/0`, owner release and the frozen
> boundary; C is idle available. A missed the P-LOCAL test-only repair ACK for two consecutive audits and is
> `COMMUNICATION_STALE`; owner, `0/1/0` blocker and 22T repair contract remain. Source is unchanged and not ACTIVE_STALE.
>
> **CR271 / P-OCR Review #3 passed; P-LOCAL Review #1 blocked (2026-07-19 00:47 EDT):** P-OCR is
> `P0/P1/P2=0/0/0 PASSED`; its branch-isolating proof and approved-difference wording are closed, and C owner is
> released. P-LOCAL production is accepted, but its 22T do not prove exact-window key isolation or queued replacement
> identity/reason; review=`0/1/0 BLOCKED`, A owner retained for test-only repair. Downstream gates remain blocked.

> **CR271 / P-OCR Review #1 ACKed, Java held (2026-07-18 23:26 EDT):** C accepted all `0/3/1`
> findings, retains owner and holds the unchanged delivery pending the user's A/B provider decision. P-PROTO now has
> eight byte-identical source/test files but no canonical delivery. No Maven/runtime/input.

> **CR271 / P-OCR Review #1 blocked (2026-07-18 23:15 EDT):** C's three-file delivery is
> `P0/P1/P2=0/3/1`. Blank-name allow semantics are reversed; configured hybrid local-first/Baidu matcher fallback is
> removed; seven tests do not prove packed/wrapped/green-link/raw fallback; public OCR JavaDoc is incomplete. C keeps
> owner and P-OCR/P-NAV remain blocked. User must choose baseline hybrid preservation (recommended) or approve the
> Cloud single-provider difference.

> **CR271 / P-PROTO Amendment #6 ACKed and unblocked (2026-07-18 22:55 EDT):** A accepted the exact mirror
> contract and separate replacement reason. Both repos now carry byte-identical mirrors `5CAF8C15`/`B3C9B713`;
> P-PROTO is source active. Args/Result/Validator/tests remain pending, so no delivery/new READY/build change.
> C continues P-OCR; no Maven with dual Java writers.

> **CR271 / P-PROTO payload contract repaired (2026-07-18 22:49 EDT):** parent approved two byte-identical
> pure protocol mirror records with exact 9/11 local-state fields, enum-name route mode and a separate replacement
> reason. This preserves the sole DHXY runtime owner and watcher lifecycle with no business difference. A retains
> owner pending ACK; C has completed the P-OCR enum `F67FDF75` (1/3). No Maven with dual Java writers.

> **CR271 / P-OCR unblocked and dual source active (2026-07-18 22:44 EDT):** C named-ACKed the complete
> DecisionEngine + enum + seven-test boundary, closing its temporary contract blocker. A's three protocol operations
> are now byte-identical `D199953C` in both repos. No Maven/runtime/input ran with both Java writers active.

> **CR271 / implementation claims and P-OCR correction (2026-07-18 22:39 EDT):** A canonically owns P-PROTO
> and is source active. C canonically owns P-OCR, but its claim omitted the Review #5 merged enum; retain C owner and
> block implementation until it ACKs the full DecisionEngine + enum + seven-test boundary. OCR bytes remain
> unchanged/absent as applicable; no Maven/runtime/input ran.

> **CR271 / P2 Review #5 passed and implementation gates opened (2026-07-18 22:26 EDT):** parent verdict is
> `P0/P1/P2=0/0/0`; External A's report owner is released. The enum-only micro-card is merged into P-OCR and the
> verified standalone jar evidence is corrected to `2,680,679B/A1DE5578`. `TURN-40B/P-PROTO` and
> `TURN-40B/P-OCR` are READY/ZERO OWNER; dependent LOCAL/CLIENT/NAV/runtime and TURN-40C remain blocked. No
> Java/Maven/runtime/input ran.

> **CR271 / P2 Review #2 communication recovered (2026-07-18 21:41 EDT):** External A double-ACKed Review #2
> and stale messages, acknowledged the frozen-baseline/current-authority mix-up, and resumed repair against current
> `DHXY-cr271` `PendingRouteOutcome` semantics. Clear `COMMUNICATION_STALE`; retain A's report-only owner and
> `0/4/1` scope. No re-delivery #2 or implementation card is READY; TURN-40C remains BLOCKED.
>
> **CR271 / P2 Review #2 communication stale (2026-07-18 21:31 EDT):** External A missed the directed Review #2
> message in two consecutive physical status events and incorrectly reported no review. Mark
> `COMMUNICATION_STALE`; retain A's report-only owner and `0/4/1` repair scope. No card is withdrawn/reallocated,
> no implementation card is READY, and TURN-40C remains BLOCKED.
>
> **CR271 / P2 Parent Review #2 blocked (2026-07-18 21:28 EDT):** the formal re-delivery is
> `P0/P1/P2=0/4/1`; A retains report-only ownership. It maps stale world-map pending-memory methods instead of the
> current `PendingRouteOutcome` replacement/abandonment/report-delivery lifecycle. Dialog-request liveness, one exact
> OCR owner, literal card write/test sets and exact test commands/counts remain open. No implementation card is READY;
> TURN-40C remains BLOCKED.
>
> **CR271 / P2 direction ACK and communication recovery (2026-07-18 21:19 EDT):** External A named-ACKed the
> Cloud-slots rejection, withdrew the second-store direction, and switched the cohort to typed cross-repo
> `LOCAL_SERVICE` operations writing the sole local `WindowRuntimeContext`. Shared protocol/validator, Cloud client,
> DHXY executor/dispatcher and both-side tests are required while the local watcher and CAS/get-and-set semantics
> remain local. No formal re-delivery or implementation card is READY; TURN-40C remains BLOCKED.
>
> **CR271 / P2 Cloud runtime slots rejected (2026-07-19):** local `WindowTaskRunner` is the consumer and
> settlement owner for preparation requests and pending transfer/route-result memory. A Cloud mirror would split
> authority from the local runtime already read by `WHOLE_TASK_DIALOG_RUNTIME_READ`. P2 must use typed existing
> `LOCAL_SERVICE` operations and freeze shared protocol, Cloud client, DHXY executor and both-side tests. No
> implementation card is READY; TURN-40C remains BLOCKED.
>

> **CR271 / P2 two owner gaps confirmed (2026-07-19):** method-level audit confirms that complete route-OCR
> ownership is missing in addition to exact-window runtime-state ownership. Raw `LocalOcrClient` and the yellow-only
> `DecisionEngine` subset do not preserve typed destination/coordinate, green, wrapped-row, same-row and raw-image
> fallback behavior. The earlier trivial rewire cohort is invalid; no implementation card is READY and TURN-40C
> remains BLOCKED.
>

> **CR271 / P2 runtime-state gap confirmed (2026-07-19):** parent source audit confirms that the Cloud has the
> read-only dialog-runtime fact and prepared-action slot but no update/clear owner for `DialogPreparationRequest`,
> and no proven exact-window owner for pending transfer/route-result state. P2 must freeze one canonical owner and
> CAS/get-and-set/clear/key tests. No implementation card is READY and TURN-40C remains BLOCKED.
>

> **CR271 / P2 Review #1 ACK and report repair active (2026-07-19):** External A named-ACKed
> `PARENT-TURN40B-P2-REVIEW1-REPAIR-20260719`; communication is normal and the same report-only owner remains.
> All five `0/4/1` findings are under same-card repair. No implementation card is READY, TURN-40C remains BLOCKED,
> and no Java, Maven, runtime or input ran.
>

> **CR271 / P2 Review #1 blocked (2026-07-19):** parent verdict is `P0/P1/P2=0/4/1`. The report must repair the
> complete runtime-state owner table, route-OCR preprocessing/fallback owner, affected tests, pre-build versus
> runtime/factory DAG, and Cloud build commands. A retains the report-only card; no implementation card is READY
> and TURN-40C remains BLOCKED.
>

> **CR271 / TURN-40B-P2 claimed (physical-EOF audit, 2026-07-19):** External A explicitly ACKed messages 2017
> and 2047 and holds the sole canonical P2 claim. C then double-ACKed and correctly declined the later claim, so
> both communication lanes are recovered. The card is report-only active and
> must freeze the complete transitive implementation cohort. TURN-40C remains BLOCKED/NOT READY, with no Java,
> Maven, runtime, UI, capture or input authorized by this claim.
>

> **CR271 / P2 communication stale (2026-07-19 00:47 UTC):** TURN-40B-P2 remains READY/ZERO OWNER, but A/C
> repeatedly missed publication message 2017 and kept reporting no claimable card. Both communication lanes are
> stale; recovery message 2047 requires a fresh ledger/card EOF read. No assignment, card withdrawal or 40C gate
> change occurred.
>

> **CR271 / TURN-40B-P2 READY (2026-07-19 00:17 UTC):** the parent corrected the DAG: C1-C4 are supporting
> source closures, while the planned TURN-40B runtime/factory is still absent; TURN-40C is therefore BLOCKED/NOT
> READY. Fresh Maven compile has exactly 33 errors in two families (`TextCandidateScanStatus` and nine DHXY-local
> Navigation collaborators). The fixed P2 card is public READY/ZERO OWNER for a report-only full transitive audit;
> no Java, assignment, stub, copied algorithm, second protocol/store/queue or business difference is authorized.
>

> **CR271 Cloud build recheck (2026-07-18 23:32 UTC):** shared missing-type compile debt remains. Cloud javac fails
> before the authorized Navigation/old-facade/Wubei named tests can execute. C2's accepted DHXY compile and isolated
> 14/14 evidence remain valid; no stub, copied algorithm, or runtime workaround is authorized.
>
> **CR271 / TURN-40B-C2 Review #7 passed (2026-07-18 23:22 UTC):** parent verdict `P0/P1/P2=0/0/0`,
> owner A released and Repair #6 closed. Production `77692F3F` and read-only test `16B93D61` are accepted; parent
> compile passed and isolated named test is 14/14 green with typed STOPPED, zero mouse queue and zero capture.
> Unrelated global historical testCompile debt remains a separate aggregate blocker.
>
> **CR271 / TURN-40B-C2 claimed (2026-07-18 23:17 UTC):** original-card EOF confirms External A's canonical
> self-claim and `OWNER A / SOURCE ACTIVE`. The sole production file is now `77692F3F`; the read-only test remains
> `16B93D61`. No Maven runs while the Java writer is active; acceptance remains 14/14 typed STOPPED with zero mouse
> queue and zero capture.
>
> **CR271 / TURN-40B-C2 repair #6 (2026-07-18 23:02 UTC):** root cause is two resolve-time current-handle reads,
> allowing metadata and captured ownership to refer to different tasks. The original card is READY/ZERO OWNER for a
> one-file single-snapshot fix; the existing replacement-race test stays read-only and must become 14/14 green.

> **CR271 / TURN-39C1 Review #2 (2026-07-18 22:57 UTC):** final source/test review passed `0/0/0`; owner A
> released and communication recovered. Boundary-specific OCR/scroll proofs and the all-production retired-type scan
> close Review #1. Production remains baseline-equivalent; named Maven/Cloud compile are still shared-debt pending.

> **CR271 / TURN-39C1 communication (2026-07-18 22:52 UTC):** Review #1 message 2240 has no named ACK across
> two audits, so A is communication-stale. Fresh authorized test bytes show repair activity and production is stable;
> this is not active-stale. Owner, `0/2/0` findings, and blocked/pending build gate remain unchanged.

> **CR271 / TURN-39C1 Review #1 (2026-07-18 22:40 UTC):** delivery is blocked at `P0/P1/P2=0/2/0`.
> Production is baseline-equivalent; acceptance still needs direct post-capture/OCR and scroll-loop checkpoint
> proofs and a retired-five-type scan across all production Java files without the TURN-44A SCC exemption. External
> A retains owner for test-only repair; named Maven/compile remain BLOCKED/PENDING on shared debt.

> **CR271 / 39C1 claim (2026-07-18 22:05 UTC):** External A is the sole canonical owner from the fixed card's
> physical EOF and has entered SOURCE ACTIVE. C ACKed C4 Review #10 and is released/idle. Acceptance scope is
> unchanged; no source delivery or build transition exists yet, and parent Maven remains prohibited while A writes.

> **CR271 / C4 final + 39C1 repair (2026-07-18 21:51 UTC):** C4 passed final source review `0/0/0` and C is
> released. 39C1 acceptance now includes direct `InputActionScope`-to-`TaskCheckpoint` migration at all current
> Navigation prepare/OCR/scroll checkpoints, zero active legacy symbol references, same-batch five-file deletion,
> `OldFacadeRemovalContractTest`, the named Navigation/Wubei tests, and Cloud compile. Card is READY/ZERO OWNER;
> current shared compile debt keeps build evidence BLOCKED/PENDING.

> **CR271 / C4 Review #9 (2026-07-18 21:41 UTC):** Fresh repair clears stale flags and closes two prior P2 comment
> findings plus ACK traceability. One stale JavaDoc ownership statement remains (`0/0/1`), so source review and 39C1
> stay blocked; owner C is retained for comment-only repair and build remains blocked by shared debt.

> **CR271 / C4 stale audit (2026-07-18 21:41 UTC):** Review #8 message 2128 is unacknowledged for two rounds and
> no relevant C activity/source change exists beyond ten minutes. C4 is `COMMUNICATION_STALE / ACTIVE_STALE` with
> owner C retained for the three P2 documentation/ACK repairs. Build remains blocked and 39C1 remains NOT READY.

> **CR271 / C4 Review #8 (2026-07-18 21:28 UTC):** Functional source/test acceptance is complete, including the
> user input invariant, but final parent source review is blocked on three P2 documentation/ACK defects. Comment-only
> redelivery is required from owner C. Required named tests have not executed because shared Cloud missing-type
> compilation debt blocks Maven; TURN-39C1 remains NOT READY.

> **CR271 / C4 communication recovered (2026-07-18 21:08 UTC):** C ACKed 2031/2041; message 2051 is pending
> its first ACK round. Stub subclasses now use ordinary construction and no Unsafe allocation bypass. TEST REPAIR
> REQUIRED remains because row 2081 is capture-failure/no-input only and row 1070 is invalid-prepared/no-input only.
> Owner C remains active / not ACTIVE_STALE / no delivery. No Maven has run.

> **CR271 / C4 test repair required (2026-07-18 20:51 UTC):** The test source violates Repair #5 by using
> `Unsafe.allocateInstance(subclass)` despite callable null-super constructors. Row 2081 proves only capture-failure/
> no-input, not the required successful OCR-to-click path, and row 1070 still lacks the real runtime/prepared proof.
> Status is OWNER C / TEST REPAIR REQUIRED / COMMUNICATION_STALE / NO DELIVERY; message 2051 awaits ACK. No Maven.

> **CR271 / C4 communication stale (2026-07-18 20:41 UTC):** C missed message 2031 for two status rounds and is
> COMMUNICATION_STALE but not ACTIVE_STALE. Message 2041 requires a double ACK and ordinary subclass construction,
> with no Unsafe constructor bypass or source-only downgrade. No delivery/Maven; TURN-39C1 remains NOT READY.

> **CR271 / C4 remaining-five repair (2026-07-18 20:31 UTC):** Tests are 4/8. The remaining callers use ordinary
> test-only subclasses for coordinate/tracker/OCR/memory observations and a real WindowRuntimeContext, while real
> production callers and real turn-command observation remain mandatory. Recursive Unsafe injection and source-only
> downgrade are rejected. Message 2031 awaits ACK; no delivery or Maven; TURN-39C1 remains NOT READY.

> **CR271 / C4 ACK (2026-07-18 20:16 UTC):** External C acknowledged test-contract message 2004. Communication
> is normal, the test-scope blocker is cleared, production is complete, and all-eight frozen-test implementation is
> active with no delivery. No Maven has run and TURN-39C1 remains NOT READY.

> **CR271 / C4 test-contract repair (2026-07-18 20:04 UTC):** Production seams and scope reduction are rejected.
> All seven active legacy callers plus active finish-cleanup still require real turn observation. The visible/retry
> path uses a test-only patterned capture built from the real packaged template and production OpenCV; test-only
> reflection may reach a real production method only where collaborator compile debt blocks the public entry.
> C remains active, message 2004 awaits ACK, no Maven has run, and TURN-39C1 remains NOT READY.

> **CR271 / C communication recovered (2026-07-19 05:34 EDT):** C double-ACKed the 0416/0432 parent messages.
> C4 remains source-active at 6/8 with no blocker and accepts dead-2334 deletion, active 696 retry transfer, and a
> test independent from row 1968. Both A and C now have normal communication.

> **CR271 / 39W ACK and 3-of-4 (2026-07-19 05:32 EDT):** A acknowledged the all-four caller contract and has
> completed the third real caller proof. Only prepared GREEN remains; source is progressing with no blocker and no
> delivery. C4 remains source-active at 6/8 and communication-stale.

> **CR271 / C4 coherent 6-of-8 (2026-07-19 05:28 EDT):** C4 has migrated six rows, with 1450/2334 remaining.
> Shared helpers are unpacked, no turn executes inside a legacy exclusive callback, and the focused Alt+1 fallback
> is removed. C remains source-active and communication-stale; TURN-39W still requires all four caller proofs.

> **CR271 / 39W test contract and C4 progress (2026-07-19 05:22 EDT):** TURN-39W requires caller-level proof
> for all four frozen callers; two representative keyboard/mouse proofs are insufficient for SOURCE+TEST delivery.
> C4 is source-active at 5/8 with 1450/1968/2334 remaining; it is not ACTIVE_STALE, but COMMUNICATION_STALE remains.

> **CR271 / writer status (2026-07-19 04:48 EDT):** A acknowledged the TURN-39W delivery/build ruling and is
> implementing frozen test proofs. C remains communication-stale but source-active. C4 must migrate its eight-row
> cohort and shared helpers atomically; no turn execution may remain nested inside a legacy exclusive callback.

> **CR271 / active writers (2026-07-19 04:32 EDT):** C4 is source-active but COMMUNICATION_STALE after two
> unacknowledged parent-message rounds; fresh NavigationService WIP means it is not ACTIVE_STALE. TURN-39W may
> deliver only after its frozen test source is complete; external in-flight compile failures remain a separate
> BLOCKED/PENDING build gate and do not authorize a production-only delivery.

> **CR271 / C4 dead-row transfer (2026-07-19 04:16 EDT):** remove the unreferenced legacy
> `closeMiniMapIfOpen@2334`; transfer its 696 observe/Alt+1/WAIT300/re-observe/retry-once acceptance to the sole
> active `closeMiniMapIfOpenTurn` and prove that finish-cleanup separately from row 1968. No focused-key fallback.

> **CR271 / C4 clarification ACK (2026-07-19 03:50 EDT):** External C accepted the exact eight-row census,
> independent proofs for rows 1968 and 2334, and zero reachable focused-keyboard fallback through
> `pressAlt1ForMiniMap`. C remains source-active owner; the frozen write set is unchanged.

> **CR271 / C4 exact-eight clarification (2026-07-19 03:47 EDT):** the physical Cloud census is
> `moveAndClickLeft@1070` plus exclusive callers `1450/1674/1968/2081/2218/2231/2334`. C's 7/8 recon omitted
> `closeMiniMapIfOpen@2334`. Acceptance now explicitly requires separate 1968 and 2334 observe/retry proofs and
> zero focused-keyboard fallback reachable through shared `pressAlt1ForMiniMap`. No write-set or behavior expansion.

> **CR271 / parallel claims and registry repair (2026-07-19 03:15 EDT):** original-card EOFs establish
> External A as TURN-39W owner and External C as TURN-40B-C4 owner. Their Cloud source/test sets do not overlap and
> may proceed in parallel. A ACKed the 39K review; C's 03:10 ledger event confirms C4 SOURCE_ACTIVE. Section 16 is
> restored to 88 Sprint rows by representing 39K/39W/39C1 as fixed TURN-39 implementation subcards rather than
> extra Sprint rows. No Maven runs while Java writers are active.

> **CR271 / TURN-39K Review #2 (2026-07-19 03:05 EDT):** parent source+test source review passed
> `P0/P1/P2=0/0/0`; owner released. The production action snapshot supplies exact stop/pause tokens without a
> holder dependency, and per-window admission preserves binding generation, cross-window background-keyboard
> concurrency and KEY_UP cleanup. Main compile is GREEN; the named Maven family remains BLOCKED/PENDING by
> unrelated dirty testCompile. Fixed cards TURN-39W and TURN-40B-C4 are READY/ZERO OWNER/UNASSIGNED.

> **CR271 / TURN-39K delivery reconciliation (2026-07-19 02:25 EDT):** the 01:18 five-artifact holder-based
> delivery is superseded and not reviewable because it was appended after Repair #2 without implementing it and
> current `TurnExecutionWindow` bytes are absent from its manifest. Acceptance awaits a stable 4-production + 2-test
> delivery using exact action stop/pause tokens. Owner A retained; C4/39W closed; Maven named gate blocked.

> **CR271 / TURN-39K Repair #2 (2026-07-19 02:20 EDT):** acceptance must obtain live stop/pause from the exact
> action snapshot in `TurnExecutionWindow`, not `TaskExecutionContextHolder`, because production turn threads do not
> bind that holder. The write set is 4 production + 2 tests; tests must resolve the production window and exercise
> its captured tokens. P1 remains open, owner A retained, C4/39W closed, Maven named gate BLOCKED/PENDING.

> **CR271 / TURN-39K gate (2026-07-19 01:55 EDT):** source review is `P1 REPAIR REQUIRED`. Background keyboard
> exact-HWND delivery and cross-window concurrency remain the accepted architecture, but acceptance additionally
> requires zero keyboard delivery on late stop, pause, and value-equal A->B->A binding-generation drift, with the
> admission atomic against the context generation monitor. Owner remains External A; C4/39W are not yet open and
> the Maven named family remains BLOCKED/PENDING.

工件编号：A-5（终审 Final #1 工件计划）
来源共识：Q7 全部收口 + B Final#1 P1-1（完整追踪 §3-§10 与 Q1-Q7）
状态：设计工件 v1 —— 需求追踪骨架完整；方法级行（REQ-M-*）待 A-1 交付后注入，该分区显式 NOT_EVALUATED 并阻塞切换
规则：状态 ∈ NOT_EVALUATED / BLOCKED / PASS / APPROVED_DIFFERENCE（后者必须链接用户裁决）；证据必须绑定不可变 release identity（buildHash/allowlistHash/baseline commit/policy+asset+quotaProfile+normalizer version/环境/时间/证据 content hash）；依赖项变化自动使受影响行回 NOT_EVALUATED。

---

## 0. 图例

| 列 | 说明 |
|---|---|
| requirementId | 稳定 ID，永不复用 |
| 源 | 草案章节/分题/终审条目 |
| 证据类型 | INV=不变量+故障注入(A档) / SEM=normalized 语义序列比对(B档) / CON=typed 契约+容差(C档) / SCH=schema+round-trip+allowlist(D档) / LOAD=负载压测 / DRILL=演练 / HUMAN=真人真机 / AUDIT=审计链抽验 / STATIC=静态扫描 |
| 环境 | LOCAL / STAGING / PROD-SWITCH（切换日）|
| 状态 | 初始一律 NOT_EVALUATED |

## 1. 硬边界（§3-§10）

| requirementId | 需求 | 源 | 证据类型 | 环境 |
|---|---|---|---|---|
| REQ-HB-001 | 本地无第二业务状态机；云端超时/失败本地不自选 phase/fallback | §3.1 | STATIC+INV | STAGING |
| REQ-HB-002 | 本地组件职责/禁止清单逐项符合 §4.1 表 | §3.2/§4.1 | STATIC+人工双审 | LOCAL |
| REQ-HB-003 | LocalSafetyGate 只拒绝不修正（六类拒因全覆盖） | §3.3 | INV | STAGING |
| REQ-HB-004 | 云端 Service 保持原业务边界（非微服务化） | §3.4 | STATIC | LOCAL |
| REQ-HB-005 | 无逐 Service 生产切换；运行时无单 Service 本地回退 | §3.5 | STATIC+SEM | STAGING |
| REQ-HB-006 | 双通道通信 + 消息按 scope 携带身份（A-2 v2 §3/§6 修正版） | §5.1 | SCH | STAGING |
| REQ-HB-007 | 本地不洗图/不 OCR/不选模板 | §5.2 | STATIC | LOCAL |
| REQ-HB-008 | ActionPlan 原子边界 + 观察点交回 | §5.3 | INV+SEM | STAGING |
| REQ-HB-009 | MATCH_AND_CLICK/REPORT 无业务语义（A-2 v2 §8.3 时序） | §5.4 | INV | STAGING |
| REQ-HB-010 | 机械事实/业务解释分离（IN_COMBAT 等只在云端） | §5.5 | STATIC | LOCAL |
| REQ-HB-011 | task turn 决策 100% 云端；本地锁仅竞态保护 | §6 | SEM+INV | STAGING |
| REQ-HB-012 | 断云=CLOUD_SUSPENDED 非 FAILED；重连云端定夺 | §7.1 | INV+HUMAN | STAGING+PROD-SWITCH |
| REQ-HB-013 | pause/stop 本地立即生效且不污染 FAILED | §7.2 | INV+HUMAN | STAGING+PROD-SWITCH |
| REQ-HB-014 | 数据归属（云权威/本地五项） | §8.1 | STATIC | LOCAL |
| REQ-HB-015 | 多用户记忆规则（私有隔离/可信发布/3 次自动发布不改人工） | §8.2 | AUDIT+INV | STAGING |
| REQ-HB-016 | 生产形态（单体+Redis/PG/对象存储+隔离） | §9 | LOAD | STAGING |
| REQ-HB-017 | 认证方向（短 token/设备密钥/无共享永久 token） | §9.1 | INV+STATIC | STAGING |
| REQ-HB-018 | 图片保留策略六级 | §10 | AUDIT | STAGING |

## 2. Q1 门（迁移完备）——分母依赖 A-1

| requirementId | 需求 | 证据类型 | 依赖 |
|---|---|---|---|
| REQ-Q1-001 | 方法级 inventory 全覆盖（含继承/lambda/监听器/条件注册） | STATIC(工具) | **A-1** |
| REQ-Q1-002 | 生产入口可达闭包无未知节点 | STATIC | A-1 |
| REQ-Q1-003 | 配置/资源零未归属（三分法） | STATIC | A-1 |
| REQ-Q1-004 | Thin Client 产物 allowlist（只含 §4.1 映射包） | STATIC(构建) | A-1 |
| REQ-Q1-005 | 反向扫描零未映射业务语义命中 | STATIC | A-1 |
| REQ-Q1-006 | 人工按业务流反向抽查无断点 | 人工双审 | A-1 |
| REQ-Q1-007 | 矩阵绑定冻结基线（repo/branch/commit/worktree-diff/时间） | AUDIT | A-1 |
| REQ-M-* | 每方法行 tier 标注与对应档证据 | 按 A/B/C/D | **A-1 注入，当前整区 NOT_EVALUATED，阻塞切换** |

## 3. Q2/Q3 门（协议与状态）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q2-001 | E4 重试白名单三条件（含 STOPPED 不重放） | INV |
| REQ-Q2-002 | sequence 域 (fence,direction,streamKey)；gap 停+RESYNC | INV |
| REQ-Q2-003 | bootstrap→FENCED；客户端永不猜 fence | INV |
| REQ-Q2-004 | 精确字节帧签名；未知字段 fail-closed（EXEC） | SCH+INV |
| REQ-Q2-005 | stale-frame 全字段栅栏 + 本地单调时钟 | INV |
| REQ-Q2-006 | messageId/actionId/requestId 三分幂等 | INV |
| REQ-Q3-001 | lease PG 权威+设备 lane 作用域+单飞 | INV |
| REQ-Q3-002 | HELD→REVOKING→HELD 换手屏障（drain proof 完整限定） | INV |
| REQ-Q3-003 | fence 换代复用排空屏障（首条副作用 plan 等待） | INV |
| REQ-Q3-004 | T0-T6/T1'/T-receipt/T4'/T5' 事务原子性 | INV(crash 注入) |
| REQ-Q3-005 | UNKNOWN 证据不可改写；恢复另记 resyncDecision | AUDIT |
| REQ-Q3-006 | window incarnation/显式 rebind（永不凭 HWND 挂回） | INV |
| REQ-Q3-007 | tombstone/压缩后旧 actionId 不变"未见过" | INV |

## 4. Q4 门（记忆）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q4-001 | verifier 前态→后态转移证明（before 已满足=INCONCLUSIVE） | SEM |
| REQ-Q4-002 | canonical 无损/可逆/幂等导入（唯一键防重） | SCH+AUDIT |
| REQ-Q4-003 | 三池独立生命周期；普通用户永不入公共池 | INV+AUDIT |
| REQ-Q4-004 | run-level 聚合防刷票；发布事务唯一键防双发布 | INV |
| REQ-Q4-005 | scorePolicyVersion=1 公式与常数一致 | SEM |
| REQ-Q4-006 | DEMOTED/QUARANTINED 状态机与恢复路径 | INV+AUDIT |
| REQ-Q4-007 | 资产 REVOKED 强路径（停重投+REVOKING+drain） | INV |
| REQ-Q4-008 | 证据删除后 digest 锚点永存 | AUDIT |

## 5. Q5 门（容量）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q5-001 | 分层预算（global→…→window，多维度） | LOAD |
| REQ-Q5-002 | 关键控制流保留通道永不 THROTTLED | INV+LOAD |
| REQ-Q5-003 | 解压后大图全链防护（grant/尺寸/时限/native 信号量） | INV |
| REQ-Q5-004 | class pool + in-flight cap + DRR；deadline 真取消 | LOAD |
| REQ-Q5-005 | 三存储故障=可用性事件（含已下发动作闭环） | DRILL |
| REQ-Q5-006 | fenced delivery（enqueue≠delivered；签名 receipt/同 digest outcome） | INV |
| REQ-Q5-007 | 负载矩阵三零标准（零静默丢/零跨租户/零过载业务 FAIL） | LOAD |
| REQ-Q5-008 | quotaProfile 版本化；收紧只挡新 admission | AUDIT |

## 6. Q6 门（建设与切换）

| requirementId | 需求 | 证据类型 | 环境 |
|---|---|---|---|
| REQ-Q6-001 | 生产同构 staging（Shadow/故障注入/容量至少一轮） | DRILL | STAGING |
| REQ-Q6-002 | shadow 同帧 tee + 双端硬拒 + 禁学习 | INV | STAGING |
| REQ-Q6-003 | A/B/C/D 证据分档（observation 影响 A/B 不得自称 D） | 人工双审 | LOCAL |
| REQ-Q6-004 | S6 覆盖清单（未覆盖=NOT_EVALUATED=阻塞） | SEM | STAGING |
| REQ-Q6-005 | 零未解释分歧（每条=bug 或用户裁决差异） | SEM+AUDIT | STAGING |
| REQ-Q6-006 | S7 quiesce/版本握手/singleton 双保险/dirty-drain 不切换 | DRILL+HUMAN | PROD-SWITCH |
| REQ-Q6-007 | 二段式导入 + cutover manifest 逐项核对 | DRILL | PROD-SWITCH |
| REQ-Q6-008 | S8 回滚预演（干净环境 checksum+authority transfer+健康门） | DRILL | STAGING |
| REQ-Q6-009 | 回滚 fresh run + cutover journal 对账 | DRILL | STAGING |
| REQ-Q6-010 | S9 稳定门（7 天/每类 20 run/3 fresh 启动/降低须批准） | AUDIT | 切换后 |

## 7. Q7 门（验收与监控自身）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-Q7-001 | 证据绑定 release identity + 依赖变化自动失效 | AUDIT |
| REQ-Q7-002 | 因果元数据 100%（采样仅 payload/span）；因果图自动校验 | INV |
| REQ-Q7-003 | S6 前冻结数值 SLO profile（单调钟分段测量） | LOAD |
| REQ-Q7-004 | 零不变量清单 + 自动遏制（非零=事故响应） | INV |
| REQ-Q7-005 | split-brain 四要素证明 | INV |
| REQ-Q7-006 | stop 归因按 canonical trigger（非链上出现） | INV |
| REQ-Q7-007 | 真人真机七项集（四骨架+多窗口竞争+每任务族全程+真实重启） | HUMAN |
| REQ-Q7-008 | telemetry/正确性双路分离 | INV |
| REQ-Q7-009 | 告警可运营（severity/owner/runbook/自动动作） | AUDIT |
| REQ-Q7-010 | observability 租户隔离与最小披露 | AUDIT |

## 8. 终审跨章节决定（Final #2-#8 + 工件审查修正）

| requirementId | 需求 | 证据类型 |
|---|---|---|
| REQ-F-001 | wire framing 唯一（binary frame+detached 签名+base64 payload） | SCH |
| REQ-F-002 | bootstrap 无 fence 握手（业务零 dispatch） | INV |
| REQ-F-003 | 硬 expiry 执行截止 + 本地预算重算门 | INV |
| REQ-F-004 | 租户隔离数据库强制（复合键+RLS） | INV(跨租户探针) |
| REQ-F-005 | protocol_fact append-only + causation 引用存在性 | INV |
| REQ-F-006 | authority append-only + 单 ACTIVE writer（partial unique） | INV+DRILL |
| REQ-F-007 | correctness RPO=0（同步副本延迟监控+恢复八步序列） | DRILL |
| REQ-F-008 | 密钥三域分治 + compromise 撤销影响矩阵 | DRILL+AUDIT |
| REQ-F-009 | 恢复后强制序列（fence 换代/UNKNOWN 标记/逐窗口重开） | DRILL |

## 9. 用户裁决登记区（P2-2：仍需用户拍板事项）

| 决策 | owner | 最迟裁决阶段 |
|---|---|---|
| S9 删除门槛数值确认 | 用户 | S9 前 |
| SLO/quotaProfile 冻结数值批准 | 用户 | S6 门 |
| 真人真机集执行安排 | 用户 | S6-S7 间 |
| 切换日 S7 最终放行签字 | 用户 | S7 |
| 迁移矩阵产出后的删除清单批准 | 用户 | S9 |

除上述五项无其他未决产品决策；实现层数值由证据门产生，降低门槛仍需用户批准。

## 10. 当前状态汇总

全部行初始 NOT_EVALUATED。REQ-M-* 分区（方法级）待 A-1 注入——**该分区空缺本身即阻塞切换项**。本矩阵为骨架 v1；A-1 交付后生成完整版并重登 hash。
> **CR271 / TURN-39W parent review #1 (2026-07-18 15:46 EDT):** canonical four-file delivery is
> `BLOCKED / REPAIR REQUIRED (P0/P1/P2=0/2/2)` with External A retained as owner. Production action order and
> timing match baseline 696; the randomized tracker harness reaches a null maintenance collaborator after its
> default completed terminal, and caller-level terminal truth is missing for tracker/prepared results. JavaDoc and
> test-description drift also require repair. C4 remains source-active 6/8; no Maven was run.
> **CR271 / C4 active rows complete (2026-07-18 15:48 EDT):** all seven active legacy rows are migrated and
> production `inputProvider.*` is zero. Remaining closure is deletion of dead 2334, transfer of the baseline-696
> mini-map re-observe/retry-once sequence to the active helper, retired ownership cleanup, and two independent tests.
> C is source-active with no blocker/delivery; A's TURN-39W repair ACK is pending round one. No Maven was run.
> **CR271 / C4 production complete and A communication stale (2026-07-18 15:55 EDT):** all eight C4 production
> dispositions are closed, including dead-2334 deletion and baseline-696 retry transfer to the active mini-map
> helper; legacy input ownership is zero and two frozen tests remain. TURN-39W repair bytes are active, but A missed
> explicit Review #1 ACK across two parent rounds, so communication is stale while activity is not. No Maven ran.
> **CR271 / TURN-39W Review #2 passed (2026-07-18 16:01 EDT):** A double-ACKed and communication recovered.
> The four canonical blobs pass parent source/test re-review at `P0/P1/P2=0/0/0`; every Review #1 finding is
> closed, all four caller step/timing/terminal mappings are baseline-equivalent, and the source owner is released.
> C4 tests remain active, so build/named test is pending and 39C1 is not ready. No Maven ran.

> **CR271 / TURN-40E Parent Review #2（2026-07-20）：** delivery remains blocked at
> `P0/P1/P2=0/1/0`. Repair #6 closed tracker recognition constants/branches, the production asset, mirrored
> protocol bytes, and ownership JavaDoc. The Cloud accept-time Wubei/Xiuluo snapshot APIs still require a tracker
> anchor unlike the current read-only local baseline and its production Xiuluo accept caller. Repair #7 is limited
> to routing those two APIs through the existing direct snapshot analysis while preserving absolute coordinates.
# TURN-40B-P2 Review #3 note - 2026-07-18 21:56 EDT

Parent verdict remains BLOCKED (`P0/P1/P2=0/2/1`). Accepted direction does not constitute an implementation gate:
literal OCR contracts, canonical card boundaries and exact test commands/counts must pass re-review first.
TURN-40C remains BLOCKED / NOT READY.
# TURN-40B-P2 Review #3 ACK - 2026-07-18 22:06 EDT

Communication is current and report repair continues. No re-delivery #3 or implementation READY exists;
TURN-40C remains BLOCKED.
# TURN-40B-P2 Review #4 - 2026-07-18 22:11 EDT

Parent verdict `0/1/1 BLOCKED`: proposed card/test/command artifacts are not yet canonical or executable.
No implementation READY and TURN-40C remains BLOCKED.
# TURN-40B-P2 Review #4 ACK - 2026-07-18 22:21 EDT

Communication is current and repair continues. No re-delivery #4, implementation READY or build change exists;
TURN-40C remains BLOCKED.
# CR271 P-PROTO Review #1 - 2026-07-18 23:46 EDT

TURN-40B/P-PROTO delivery review=`P0/P1/P2=0/2/1 BLOCKED`。同一八文件边界须修复 nonblank replacement
reason、`YELLOW_DESTINATION_MINI_MAP` closed routeMode 以及 Result present/null/compat 序列化证据；在父级
复审 `0/0/0` 前，P-LOCAL/P-CLIENT 不进入 READY。

# CR271 P-PROTO Review #2 - 2026-07-19 00:01 EDT

Repair #1 review=`P0/P1/P2=0/1/1 BLOCKED`。nonblank reason 与 result carrier 语义已闭合；routeMode 仍须
限制为 DHXY receiver 唯一 wire 值 `YELLOW_DESTINATION_MINI_MAP` 并拒绝 legacy/unknown，golden suite 须从
8T 恢复冻结 7T。A owner retained；P-LOCAL/P-CLIENT 仍不进入 READY。

# CR271 P-PROTO Review #3 - 2026-07-19 00:11 EDT

Repair #2 review=`P0/P1/P2=0/0/1 BLOCKED`。production sole-value allowlist 与 `17+7` counts 已接受；
validator test 尚缺一个任意未知非空 routeMode 的独立拒绝断言。A owner retained 做 test-only Repair #3；
P-LOCAL/P-CLIENT 仍不进入 READY。

# CR271 P-PROTO Review #4 Passed - 2026-07-19 00:21 EDT

Repair #3 review=`P0/P1/P2=0/0/0 PASSED`。final dual-repo protocol boundary 与 `17+7` evidence 通过，A owner
released；P-LOCAL/P-CLIENT 进入 public READY/ZERO OWNER。用户选择 P-OCR B 并批准 single-provider/no-Baidu
差异，C source-active；P-NAV/runtime/40C 仍 blocked。

# CR271 P-CLIENT Review #2 - 2026-07-19 01:42 EDT

The P-PROTO, P-OCR and P-LOCAL source gates are closed. P-CLIENT Repair #1 production mapping is accepted, but
the parent source/test review remains `P0/P1/P2=0/1/2 BLOCKED`: the failed replacement fixture is validator-invalid,
non-success/empty outbound proof and a pending-route smuggle negative are incomplete, and replacement reason JavaDoc
must say nonblank. External C retains the same two-file, exactly-33-test Repair #2 boundary. P-NAV, runtime/factory and
TURN-40C remain blocked.

# CR271 P-CLIENT Review #3 Passed / P-NAV Ready - 2026-07-19 01:52 EDT

P-CLIENT final source/test `087D053F`/521L and `4892F1D9`/604L/33T passed parent review at
`P0/P1/P2=0/0/0`; External C's owner is released and no business difference was introduced. All four pre-build source
gates are now complete, so the frozen P-NAV NavigationService/test boundary is public
`READY / ZERO OWNER / UNASSIGNED`. Runtime/factory, TURN-40C and aggregate Cloud build remain blocked.

# CR271 P-NAV Double-Claim Resolution - 2026-07-19 02:02 EDT

The original-card physical order places External C's P-NAV claim before External A's later claim. C is the sole
canonical owner and P-NAV is `SOURCE_ACTIVE`; A has canonically withdrawn its zero-source claim. Both baselines remain
`B57ECC50` + `79D48FE0`/23T, with no Java collision. Runtime/factory, TURN-40C and aggregate build stay blocked.

# CR271 P-NAV First Source Increment - 2026-07-19 02:14 EDT

External C ACKed the sole-owner resume message. NavigationService now has its first WIP increment, deleting the dead
`InputProvider` import/field (`B57ECC50`/3155L -> `3C12E5E4`/3153L); NavigationTurnContractTest remains
`79D48FE0`/1470L/23T. There is no canonical delivery or source review yet. Runtime/factory, TURN-40C and aggregate
build stay blocked.

# CR271 P-NAV Legacy Route Outcome Contract Block - 2026-07-19 03:19 EDT

The parent verified that baseline 696 records and later consumes `LEGACY_GREEN_LINK` pending route outcomes. Removing
that write would remove a navigation memory fast path and is not a behavior-neutral migration. Only this P-NAV
sub-item is `PLAN-CONTRACT BLOCKED`; C continues unrelated final-cluster work. User decision A preserves 696 by
reopening/extending P-PROTO and P-CLIENT (recommended); decision B explicitly approves retiring the legacy record.
Runtime/factory, TURN-40C and aggregate build remain blocked.

## 2026-07-19 03:39 EDT - P-NAV Active Stale

External C has no STATUS EVENT or NavigationService change for more than 10 minutes; source remains
`C7A7CF00`/3076L. P-NAV is `ACTIVE_STALE` with C's sole ownership retained. The legacy route-outcome user-decision
block remains separate, and a directed ACK/progress report is required next heartbeat.
> **CR271 / P-NAV deprecated legacy decision (2026-07-19 03:49 EDT):** the user confirms that
> `clickRememberedWorldMapRouteResult(...)` and its legacy green-link route-result memory are deprecated retained code.
> They are excluded from migration: do not reconnect or modify the deprecated method and do not reopen P-PROTO/P-CLIENT.
> The legacy sub-item contract block is cleared. C missed two stale-message ACK rounds and remains sole owner with
> `COMMUNICATION_STALE / ACTIVE_STALE`; current yellow-route completion is still required.
>
> **CR271 / P-NAV stale recovered and deprecated scope clarified (2026-07-19 03:59 EDT):** C named-ACKed both
> parent messages and source activity resumed at `4915DEC5`/3078L, clearing communication and active stale. No further
> deprecated-method edits, protocol expansion or dedicated tests are allowed. Already-landed mechanical collaborator
> substitutions are not reverted solely for byte identity; final review must prove no business-semantic change and no
> current-yellow-path dependency on the deprecated chain. C remains sole owner / SOURCE_ACTIVE; no delivery exists.
>
## CR271 P-NAV build evidence - 2026-07-19 05:54 EDT

- P-NAV production class compile evidence advanced to `NavigationService=0 errors`; this does not satisfy aggregate
  Cloud build or P-NAV source-review acceptance because out-of-write-set shared debt remains and 23-test isolated
  verification/canonical delivery are still pending.

## 2026-07-19 06:04 EDT - P-NAV Isolated Verification Active Stale

- No new C STATUS EVENT or frozen source/test change was observed for more than 10 minutes. P-NAV is marked
  `ACTIVE_STALE` with C's sole ownership retained and a directed ACK required; all prior build gates remain unchanged.

## 2026-07-19 06:14 EDT - P-NAV Source Recovery / Communication Stale

- `NavigationTurnContractTest` resumed changing at 06:11 (`D1B124DB`), clearing active stale. Required heartbeat ACK
  is still absent after two rounds, so communication is stale; ownership and all build/review gates remain unchanged.

## 2026-07-19 06:34 EDT - P-NAV Isolated Compile/Test Milestone

- Isolated evidence advanced to 528 main files compile-clean and 13/23 named tests; timeout was corrected from 30s to
  the frozen 120s contract. This does not pass aggregate build or source review, and communication stale persists.

## 2026-07-19 06:44 EDT - P-NAV Communication Recovered

- Both directed parent messages are now ACKed; communication is healthy. Ownership, 528-file compile-clean/13-of-23
  evidence, aggregate-build blocker and no-delivery/no-review gates remain otherwise unchanged.

## 2026-07-19 07:09 EDT - P-NAV Isolated Tests 20 Of 23

- Isolated named-test evidence advanced from 13/23 to 20/23. The remaining three failures are content-bearing capture
  fixture alignment; no additional production change is reported and the test writer remains active.
- This does not pass aggregate build or source review. C retains sole ownership; canonical delivery is still absent.

## 2026-07-19 09:52 EDT - P-COMPILE Source Activity Recovered / Communication Stale

- Four contracted test files resumed byte activity, so P-COMPILE is now `SOURCE ACTIVE / COMMUNICATION_STALE` with
  A's sole ownership retained; `ACTIVE_STALE` is cleared.
- The former named-test process exited with no complete report set or exit-code evidence. The gate remains
  `BLOCKED / PENDING`, with no canonical re-delivery/review and runtime/factory/TURN-40C still blocked.

## 2026-07-19 10:08 EDT - P-COMPILE Authorized Ten-Test Rerun Active

- A advanced the tracker fixture to `B8EA0515` and launched the fixed ten-test Maven; the first fresh reports are
  present and the build remains active. This is not yet a passed acceptance gate or canonical delivery.
- `COMMUNICATION_STALE` remains pending A's named ACK. Runtime/factory/TURN-40C stay blocked and parent runs no Maven.

## 2026-07-19 10:09 EDT - P-COMPILE Ten-Test Rerun Failed

- The authorized ten-test gate completed at 239 tests / 22 failures / 67 errors. Two classes are fully green and
  eight remain red; this is `BUILD FAILED / REPAIR ACTIVE`, not source delivery or acceptance.
- A retains sole ownership and the fixed test-only repair. Production/main compile evidence remains frozen; downstream
  runtime/factory/TURN-40C stay blocked and communication remains stale pending A's named ACK.

## 2026-07-19 10:19 EDT - P-COMPILE Q1-Q4 Plan-Contract Ruling

- Re-delivery gate is testCompile clean plus Wubei/FiveRing/Summon/Xiuluo WholeTask tests green; six transitive
  compile-repair tests are not runtime blockers. Four-test strict fixtures must mirror current production result shapes.
- Summon 38-value exact golden is accepted; DialogOption runtime and tracker PT20S are excluded, while production 30s
  stays frozen. Plan-contract block is cleared; A retains owner and downstream gates remain blocked pending delivery.

## 2026-07-19 10:27 EDT - P-COMPILE Ruling ACK / Repair Resumed

- A accepted the bounded four-WholeTask gate and resumed authorized test-only fixture work. Current snapshots are
  `244F71C5/42CFFE0D/A7A985C6/B7D5BF03`; no new build or canonical delivery exists.
- The Q1-Q4 plan wait is closed. Communication stale remains pending explicit ACK of the older 09:09 message.

## 2026-07-19 10:37 EDT - P-COMPILE Bounded Test Byte Progress

- The four runtime-gate tests now hash to `244F71C5/C4939131/2CA1F71C/D0BA4DAB`; the latter three advanced in
  sequence, confirming bounded test-only repair activity without changing frozen production.
- No Maven, re-delivery or review verdict exists. Review #1 and A ownership remain active; communication stale and
  downstream runtime/factory/TURN-40C blockers remain unchanged.
> **CR271 TURN-40C activation 7/7 passed / communication recovering（2026-07-19 19:40 EDT）：** R5 host graph 已完整
> refresh 且 0 bean error；A 已 ACK R5 `1924`，R1/stale 两条尾部消息仍待 ACK。activation 两项
> test-expectation 修正源码为 `7B418DF0`/376L/7T，点名测试已 `7/7 PASS / EXIT 0`；冻结 named family、
> compile/test-compile 与 canonical 15-path delivery 尚待，不提前作 source review，父级未跑 Maven。

## TURN-40F Repair #2 Acceptance Delta - 2026-07-20

- PASS：exact-four client Service；client vision/old-stack 零文件；点名厚 Service 零生产引用。
- PASS：同 HTTPS turn v1、同 action、同 bag-exclusive callback 一次开包 continuation；失败不数鞋/不 fallback。
- PASS：Windows sleep 显式 UI 选择、STOP_ON_FAILURE、checkpoint/interruptible wait/log/Windows command，且 client
  owner 是 non-Service host executor。
- PASS：双仓 T01 44/44、Cloud T02 30、client T03+T04 41；双端 compile exit 0；protocol/fixture normalized parity。
- BLOCKED：五环 accept/story visual replay + marked output；仓内无 raw testcase且禁止 live capture。
- REVIEW：父级是唯一 final reviewer；Worker 不标 Approved，TURN-41 仍 blocked。

## TURN-40F Repair #3 Acceptance Delta - 2026-07-20

- BLOCKED：Cloud `CloudWholeTaskReadyEventState` 尚无 production publisher，五类 soft wake 无闭环。
- REQUIRED：Cloud observer 在 task await 期间通过现有 HTTPS turn v1 action 获取 raw HWND facts并发布五类 event；
  same-window terminal 必须真实 signal，不能等 60 秒 metadata/Cloud wait timeout。
- REQUIRED：client Runner保持薄，Service exact four、OCR/vision/旧 stack为零，无第二 Bus/store/endpoint/transport。
- 状态：`REPAIR #3 READY / ZERO OWNER / TURN-41 BLOCKED / NOT READY FOR USER TEST`。

## TURN-40F Repair #3 Parent Review #1 Delta - 2026-07-20

- PARTIAL：`PATHING_TERMINAL` WIP 的 1000ms/2200ms/tolerance/UNTARGETED 主判定与当前只读本地 Runner 对齐。
- FAIL：另外四类 production publisher缺失。
- FAIL：observer/task同窗单 action slot 无 task-priority/park-only admission 与 BUSY/真唤醒并发证明。
- FAIL：exact task lifecycle、stop/pause、wrong scope/intent、duplicate、miss/timeout、other-window tests缺失。
- 状态：`P0/P1/P2=0/2/1 / REPAIR REQUIRED / TURN-41 BLOCKED / NOT READY FOR USER TEST`。

## TURN-40F Repair #3 Parent Review #2 Delta - 2026-07-20

- FAIL：combat后 client authoritative intent仍存活并可回灌 Cloud mirror。
- FAIL：visible attention未链到真实 parked prepared-action production。
- FAIL：pre-battle event缺 target/atomic publish fence。
- REQUIRED TEST：combat local-read idle/non-revival、interest+visible -> prepared slot+event+bounded wake、timer duplicate/reset。

## TURN-40F Repair #3 Parent Review #3 Delta - 2026-07-20

- `P0/P1/P2=0/3/1 / REPAIR REQUIRED`；cleanup/pre-battle/authority closure接受，但业务等价门未通过。
- 必须证明Xiuluo-only priority与其余route-first、operation/target/intent current-stale fence、原子gate+interest，以及
  terminal publish后两类route-memory settlement的真实observer生产链。
- wrong scope、stop/pause、miss-no-truth和失败重试均需named tests；完成前不得进入TURN-41 fresh runtime。

## TURN-40F Parent Review #4 Full Baseline Gap Delta - 2026-07-20

- FAIL：terminal settlement在第一次HTTPS失败后不会重试。
- FAIL：enabled startup-window preparation的map tracking/Alt+5/Alt+6生产链缺失。
- FAIL：map-survey UI及Cloud calibration/persistence/undo/project能力缺失。
- REQUIRED：真实observer retry/prepare harness、Cloud startup policy、map-survey完整用户能力；完成前TURN-41 blocked。
- DATA CUTOVER REQUIRED：当前本地六份learned-memory JSON和`map_camera_bounds.json`仍为零import；TURN-41前须
  对exact tenant/user Cloud storage完成备份、schema兼容、一次性导入及计数/hash验收，不得覆盖本地基线。
- MAPSURVEY PROTOCOL：同一turn v1必须有command ACCEPTED/terminal result/ack重发去重、exact pointer sample、
  wrong-window/active-task fail-closed证据；不得借用普通task start或新增endpoint/store。
- ASSET BYTE FAIL：Cloud生产Wubei tracker anchor及五张yellow title仍为旧SHA；须与当前只读baseline字节一致并
  由真实Cloud consumer/resource-load test证明，不能只按文件名验收。
- FIVE-RING POST-COMBAT FAIL：tracker pathing期间经历战斗并完成恢复后，Cloud必须通过既有typed runtime state
  按`wuhuan-v2:prepared-tracker-panel-click:`优先、`wuhuan-v2:tracker-green-click:`回退定向清理旧intent，再进入
  task-panel同步；测试须证明匹配intent被清、foreign/route intent不被误清、无combat-observed路径不额外clear。
- MAINTENANCE BROADCAST FAIL：当前baseline只在window-relative `260,373..378,413` raw ROI中匹配
  `maintenance_heal_all_repair_raw.png`/`maintenance_repair_confirm_raw.png`（threshold `0.85`），fixed miss不再进入
  full-dialog green/yellow OCR/template fallback。Cloud必须移除`TaskMaintenanceService`及轻量清窗生产链对旧fallback的
  可达调用，并证明raw hit点击、raw miss no-action、普通OPTION不误点。

## 2026-07-20 TURN-40F Review #5 Gate

- FAIL：wrapped-route correction测试不得用synthetic mask替代真实游戏raw screenshot，也不得只reflection调用private helper；
  必须走production入口并产出标记原OCR框、校正黄字框、最终点击点的可审阅图。
- FAIL：FiveRing post-combat cleanup不得只做source guard；必须由production-path harness证明prefix顺序、匹配/回退、
  foreign/route保留、no-combat及stop/pause零调用。
- WIP不是PASS：route探索测试仍故意抛错且marked output未更新；FiveRing测试尚无新字节。
## 2026-07-20 TURN-40F Repair #5 Parent Verification

Focused source/test gates与Cloud compile已PASS；whole-card final source review和TURN-41数据cutover gate未完成，因此总体仍`NOT READY FOR USER TEST`。

## 2026-07-20 TURN-40F Parent Review #7

角色预检存在2个P1 baseline drift，TURN-40F=`REPAIR #6 REQUIRED`；整体继续`NOT READY FOR USER TEST`。

18:41 EDT Worker已ACK且`SOURCE ACTIVE`；验收状态不变。

18:47 EDT Repair #6 focused source/tests/compile PASS；总体仍`NOT READY FOR USER TEST`，等待whole-card终审与TURN-41。
> **TURN-41 Parent Review #1（2026-07-20 20:40 EDT）：** `P0/P1/P2=0/1/1 / REPAIR REQUIRED`.
> Cutover Apply must auto-restore the exact four-file pre-state on every post-backup failure and expose/preserve
> the backup before mutation. Huygens owns Repair #1; no real scope Apply or user runtime is approved yet.
> **TURN-41 Repair #1 parent pass（2026-07-20 20:49 EDT）：** `P0/P1/P2=0/0/0`; parent named
> contracts `7+1` and Cloud compile PASS. Owner is released. Exact production scope plus a successful real
> Inspect/DryRun/Apply/post-read remains mandatory before user fresh runtime and TEST READY.
> **TURN-41 real cutover passed（2026-07-20 20:54 EDT）：** production scope is fixed under
> `%LOCALAPPDATA%\DHXY\cloud-brain\state`; Inspect/DryRun/Apply and independent post-read passed exact
> `22/460/600/1000/80` counts and all four hashes, with rollback backup retained. Status is now
> `TEST READY / USER FRESH RUNTIME READY`; final completion still requires fresh runtime acceptance.

> **TURN-40F runtime equivalence reopen（2026-07-21 04:09 EDT）：** the mini-map close defect is fixed and
> compiled, but the parent plus three read-only workers found `P0/P1/P2=0/10/3` after deduplication. Formal user
> testing is blocked until the method-level source-equivalence repairs in TURN-40F §65 pass focused contracts.
> **TURN-40F Repair #8 parent review #11（2026-07-21 05:29 EDT）：** source/test acceptance remains
> `P0/P1/P2=0/2/0 / REPAIR REQUIRED`. The remaining gates are baseline-equivalent member/non-combat STORY
> suppression and real production-entry coverage for all fourteen stable-window consumers. TURN-41 cutover
> evidence remains valid but does not make the system formal-test-ready.
# 2026-07-21 CR271 Joint Review Delta

- Five-window Cloud runtime source gate: `P0/P1/P2=0/0/0`; exact `(deviceId,windowId)` RunSlot isolation and
  independent duplicate/conflict/stop/terminal/restart contracts passed. Parent Cloud combined gate=`48/48`.
- Fresh deployment still must prove `5/5` task-start ACK before runtime acceptance closes.
- TURN-40G observation runner remains `P0/P1/P2=0/2/0 REPAIR REQUIRED`: exact String taskRunId must not be reduced
  to `hashCode`, and the schedule tuple must validate all-or-none with zero mutation on malformed payloads.
> **2026-07-21 STOP-RESTART fresh repair #2:** Observer跨线程上下文、停止5秒超时和ACK-less restart已在
> Cloud源码合同闭合；runtime/activation/observer测试族通过。必须重启Cloud JVM后验证
> `PATHING_TERMINAL -> NPC_CLICK_SMART`及stop -> immediate restart，当前不宣称fresh runtime通过。
> **2026-07-22 TURN-40G Review #12：** Cloud stopped-static必须等待严格晚于首次pathing terminal的
> `xiuluo-dialog`帧；同批/缓存帧不可终结attempt。生产local-kanda保持关闭，Cloud模板不变。源码/测试通过，
> fresh gate为Client/Cloud PNG SHA一致、Cloud坐标点击及真实`IN_COMBAT`。
>
> **2026-07-23 Fast Exit fresh gate：** run
> `remote-turn-005a7151-e207-47af-bcfa-29bb4ef233f0` 已证明 Client fast edge、Cloud 接收和
> observer visible 栅栏均工作；失败点是 Cloud task 在 exact one-shot 前先跑 full radar。
> 修复后验收要求：fast event 后不得出现同 tick radar 复写 `IN_COMBAT` 或
> `discard stale combat-exit signal while still IN_COMBAT`，并应直接进入 deferred
> post-combat。源码回归 `1/1 + 49/49`，待 fresh runtime。
>
> **2026-07-26 CR277 acceptance：** source+test gate 已由父级以
> `P0/P1/P2=0/0/0` 关闭并释放 owner。Client focused `4/4`、Cloud CR277
> `18/18`、Ready recovery `2/2`、双仓 compile `0`、共享 observation wire
> `5/5 byte-identical`。最终运行门仍要求 fresh 五窗口证明：各窗口后台准备不串 identity，
> 首候选/terminal Ready 能即时唤醒，真实输入受 coarse task turn 排他保护，同 intent
> `STOPPED_AWAY` 10 秒仅 re-wake 一次。
>
> **2026-07-26 CR277 runtime reopen/repair：** 首次 fresh 验收失败于启动 handover 永久
> park；源码现已消费 exact Tracker negative，并通过父级 focused `3/3` 与 Cloud compile。
> 下一 fresh 门要求：重启 Cloud 后，`TASK_NOT_FOUND` 必须立即进入 `ACCEPT_TASK`，日志不得
> 继续每 `900ms` 重复 `background tracker preparation not ready`。
>
