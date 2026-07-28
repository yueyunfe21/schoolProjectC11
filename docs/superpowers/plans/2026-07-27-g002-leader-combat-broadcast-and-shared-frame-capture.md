# G002 leader combat broadcast for same-team members and leader shared-frame capture

> 云端项目卡号规则：自本卡起云端单脑项目的卡以 G 开头编号（用户 2026-07-27 指定）。本卡 = G002。

## Objective

消除游戏画面卡顿的两大截图源：
1. 同队成员窗口的独立战斗检测（客户端战斗信号采样 + 云端成员雷达捕获 turn）——改为**队长检测、云端广播**；
2. 队长窗口每秒 5–6 次独立区域截图（每次都是整窗 `PrintWindow(PW_RENDERFULLCONTENT)` 强制重绘）——改为**每观察周期整窗抓一次、全部消费者内存裁剪**。

目标效果：整机对游戏窗口的强制重绘从 ~10 次/秒降到 ~1–2 次/秒（用户已用 F11 暂停法实证卡顿源为本程序截图）。

## Existing context

- CR212（`2026-07-07-cr212-tooltip-hash-local-team-gating.md`）已建立：同队关系按玩家 ID 追踪（组哈希/队长玩家 ID/队长窗口/成员清单/队长是否本机控制）；协议层 `TurnWindowMetadata` 携带 `localTeamSessionKey`/`localLeaderWindowId`/`localLeaderPresent`；成员空闲维护广播扫描在"同队+队长本机控制"时已被抑制，仅响应队长开启的能力（TEAM_RETURN/FIRST_AID/SUMMON_SKILL/COMMON_BOX/LEFT_TOP_STATUS），含完整生命周期恢复规则。本卡是 CR212 模式向战斗检测的扩展。
- 2026-07-27 已落地"战斗边沿通用化上报"：客户端 `WindowObservationSampler.publishCombatEdge` 无条件上报 IN_COMBAT/COMBAT_EXITED（凭据仅作 expected/unexpected 标签）；云端 `CloudFastExpectedCombatExitCoordinator.accept()` 收边沿→置 `gameContext` 动作态→发 `COMBAT_STATE_CHANGED`。本卡的广播即在 accept() 上扩展。
- 观察采样是兴趣驱动：`CloudWholeTaskObserver.publishObservationInterests` 发布什么，客户端才采样什么。
- 截图底层：`GameClientTracker.captureToMemory` → `globalInputLock.callWithLock` + `PrintWindow(hwnd, PW_RENDERFULLCONTENT)`（无区域参数，必整窗渲染后裁剪）。战斗信号 3 组 stage 各自独立截图（出战斗时 3 次/周期）+ 寻路坐标条探测 600ms 一次 + 看打探针 + 放大镜锚点（战斗中）。

## Requirements

### W1 成员战斗状态由队长广播

1. 云端 `publishObservationInterests`：窗口满足"成员 + `localLeaderPresent` + 队长窗口在同一 `localTeamSessionKey`"时**不发布 `COMBAT_SIGNAL` 兴趣**（客户端零改动，采样自然停止）。
2. 云端 coordinator `accept()`：接受的战斗边沿来自**队长窗口**（binding 的 metadata：`localLeaderPresent && localLeaderWindowId == windowId`）时，遍历同 `localTeamSessionKey` 的成员 binding，逐个 `withBinding`：置动作态（IN_COMBAT/FREE）+ 发 `COMBAT_STATE_CHANGED`，source 前缀 `leader-broadcast:`。
3. 成员云端雷达抑制：成员（同条件）在 `AutoCombatService.handleCombatTick` 走"仅消费动作态"的短路径（同 `CLIENT_RUNNER_EXIT` 语义：不做雷达扫描/捕获，保留战斗中维护与进战处理），动作态唯一来源=队长广播。
4. 生命周期照抄 CR212：队长停止/异常/外部队长/队伍解散 → 恢复成员自检（兴趣重新发布 + 雷达路径恢复）。队长暂停/恢复不改变广播规则。
5. 不得抑制成员对队长触发能力的响应（维护广播、急救等 CR212 白名单不动）。

### W2 队长单帧共享捕获

1. `WindowObservationSampler` 每观察周期对本窗口整窗 `captureToMemory` **一次**，作为本周期共享帧。
2. 全部本地识别消费者改为从共享帧内存裁剪（scaled rect 语义与现 `coordinateHelper.getScaledRect` 一致）：战斗信号 3 组 stage、放大镜锚点（`map/minimap_visible_anchor.png` ROI 196,65,20×22）、寻路坐标条探测、看打探针。
3. 周期结束 flush 共享帧；帧获取失败时本周期各消费者按 UNAVAILABLE 处理（不判 ABSENT、不产生边沿）。
4. 观察类整窗捕获使用 tryLock（抢不到 `globalInputLock` 即跳过本周期），绝不阻塞输入 worker。
5. 判定语义不变：STOPPED_AWAY 2.2s 边界、战斗信号去抖、放大镜正证据退出全部保留；同帧一致性为增强（消灭跨帧错拍）。

## Out of scope

- 不改模板、阈值、ROI 坐标；不改战斗信号/寻路/看打的判定逻辑本身。
- 不改云端识别算法与 turn 协议。
- 不动 CR212 的维护广播白名单行为。

## Acceptance

- 双仓编译通过；共享协议文件（如有改动）byte-identical。
- 运行验证：成员窗口日志无 combat-signal 采样；队长战斗时成员日志出现 `leader-broadcast:` 唤醒；队长窗口截图频率 ≈1 次/秒（`hwndCapture` 计数增速）。
- 合同测试：W1 兴趣发布条件、广播扇出；W2 共享帧裁剪与 UNAVAILABLE 语义（连同 7-27 通用化上报欠的 validator/gate 测试一起补）。

## Status

- 2026-07-27 卡建立（G002）。
- W1 已实施（cloud 三处：`CloudWholeTaskObserver` 兴趣抑制 / coordinator `broadcastLeaderCombatStateToTeamMembers` / `AutoCombatService.handleCombatTick` leaderBroadcastDriven 短路+广播退出恢复），cloud 编译通过。
- W2 已实施（client：`GlobalInputLock.tryCallWithLock` / `GameClientTracker.captureToMemoryIfIdle` / sampler 共享周期帧 `refreshSharedCycleFrame`+`cropSharedCycleFrame`（collectBound 入口刷新）/ mechanics `bindCycleFrameCropper`（战斗 3 组+放大镜）/ 寻路坐标条探测与 sampleRoi ROI 上传改内存裁剪），client 编译通过。保留直捕的两处一次性捕获：kanda 点击前坐标帧、寻路终态整窗证据帧。
- 欠账：合同测试未补（W1 兴趣条件/广播扇出、W2 共享帧裁剪与 UNAVAILABLE 语义、7-27 通用化上报的 validator/gate）；既有 sampler 合同测试若直接喂 tracker.captureToMemory 的 fixture，寻路探测路径需适配 `captureToMemoryIfIdle`/共享帧。
- 待运行验证（Acceptance 三条）。
