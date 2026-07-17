# W-COUNT-DIALOG-MEMORY-FIND-USABLE-1 - Internal I24 Worker Report

## CLAIMED

- Worker: `Internal I24 implementation-only Worker`（非 reviewer）
- Claimed at: `2026-07-15 03:51:40 -04:00`
- countUnit: `DialogChoiceMemoryService::findUsable`
- requested countDelta: `+1`
- 最终状态: `BLOCKED`
- 最终 countDelta: `0`
- 记账门: 父级源码审查与 fresh Maven 之前不得记账；本报告没有申请提前记账。

## 前置材料与工作树

- 已完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、`docs/ACTIVE_WORK.md` 顶部 CR271、
  `docs/业务逻辑.md` 适用基线、`2026-07-14-696a12b0-whole-service-first-migration.md`，并核对
  `2026-07-12-service-migration-matrix.md` 的 Service 总表、隐式状态和方法级底账。
- 业务基线：`docs/业务逻辑.md` 的五倍/修罗 baseline gate 与修罗
  `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 失败/fallback 对账规则；本单不得引入新 TTL、
  retry、owner/session、额外读取或改变 reuse/fallback 顺序。
- DHXY: branch `thin-client-design`, HEAD `0114604e1ff5f15491d2910959c45252e893d04f`，存在大量
  dirty/untracked 并行迁移文件。
- Cloud: branch `navigation-migration`, HEAD `3b988caa010254973e03342272e6d1d6a9685b01`，存在大量
  dirty/untracked 并行迁移文件。
- 两仓既有 dirty/untracked 全部保护；未 checkout/reset/clean/add/commit 或修改 Git 状态。

## 696 源码等价证据

- 目标文件当前 328 行；与
  `git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/DialogChoiceMemoryService.java`
  做 LF 归一化全文比较，结果 `EqualText=True`，`BaselineLines=328`，`CurrentLines=328`。
- `MemoryService.java` 同样与 696 全文等价。
- 因此目标方法本体没有可在唯一 Java 写集内修复的偏差。

## 逐跳证据

1. **真实 caller 搜索断点**
   - Cloud 全树对 `findUsableDialogChoice(...)`、`findStableTaskDialogChoice(...)`、
     `findUsableRouteDialogChoice(...)` 的引用只有：`MemoryService` 三个 facade 定义，以及
     `NavigationService.java:762` 对 route facade 的真实调用。
   - `DialogService` 与 `NpcClickService` 均不调用 `findUsableDialogChoice(...)`；通用 facade
     `MemoryService.java:39-41` 没有外部 caller，故不存在本单要求的真实 Dialog/Npc caller。
   - 696 commit 的 `git grep` 结果相同：通用 facade 只有定义；任务接取调用的是独立
     `findStableTaskDialogChoice(...)`，路由调用的是独立 `findUsableRouteDialogChoice(...)`。

2. **entry key**
   - `DialogChoiceMemoryService.java:62-63` 调用 `findByKey(key(scope, action, contextKey))`。
   - `DialogChoiceMemoryService.java:251-259` 对三个字段分别 trim/判空，任一为空返回 null；有效 key 为
     `scope|action|contextKey`。该逻辑与 696 完全一致。

3. **isUsable 条件**
   - `DialogChoiceMemoryService.java:195-203`：null key、无 entry 或 `!entry.isUsable()` 均返回
     `Optional.empty()`；只有可用 entry 返回 `Optional.of(entry)`。
   - `DialogChoiceMemoryService.java:316-319`：`!disabled && successCount > 0 && failCount < 3`。
     与矩阵 `DialogChoiceMemoryService::findUsable` 行完全一致。

4. **Optional 结果与 reuse/fallback**
   - 通用 facade `MemoryService.java:39-41` 只把内部 entry 映射成 facade DTO；因为没有真实外部 caller，
     `Optional` 没有继续驱动 Dialog/Npc reuse 或原 fallback，闭环在 facade 后断开。
   - route 链确实完整：`NavigationService.java:760-778 -> MemoryService.findUsableRouteDialogChoice ->
     DialogChoiceMemoryService.findUsableRoute -> findUsable`；有值时填入 remembered relative point/text，
     空值时三个 remembered 字段为 null，后续仍走现有 watcher/NPC click/route-dialog fallback。
   - 但迁移矩阵把 `findUsableRoute / recordRouteSuccess` 单列为另一 count unit；用这条 route 链给
     `findUsable` 本单记账会重复 route 单位，不能作为本单 `+1` 证据。

5. **明确排除的重复单位**
   - 未修改、未重复申领 `recordSuccess`、`recordFailure`、`findStableTaskChoice`。
   - 未用 `findUsableRoute / recordRouteSuccess` 的独立链冒充本单新增闭环。

## BLOCKED 结论

- `BLOCKED P1=1 / countDelta=0`。
- 根因：目标 Service 内部实现正确且与 696 等价，但本单要求的真实 Dialog/Npc caller 不存在；断点位于
  唯一 Java 写集之外的 caller 层。仅修改 `DialogChoiceMemoryService.java` 无法制造“真实 caller”，除非新增
  自调用/wrapper/第二条策略入口，这既不真实，也违反 no-wrapper、不得新增 owner/session/retry 和基线等价约束。
- 解除阻塞需要父级二选一，不能由本 Worker擅自决定：
  1. 扩写集到一个用户批准、基线已有语义的真实 Dialog/Npc caller，并明确其 reuse/fallback；或
  2. 将本单改绑到现存 route count unit，同时先确认不会与矩阵
     `findUsableRoute / recordRouteSuccess` 重复记账。
- 在父级裁决前，不应修改 Java、不应 build，也不应把该单位加入 ledger。

## Changed Files

- Java: `无`（目标 Java 文件保持 696 等价，NO CODE CHANGE）
- Report: `docs/superpowers/plans/reports/2026-07-15-cloud-dialog-memory-find-usable-count-unit-worker-i24.md`

## 执行限制确认

- 未运行 build/test/runtime；未启动 Task/UI/capture/input。
- 未进行 Git mutation。
- 无已批准业务差异；按 `696a12b0` 基线等价审计。

## Parent Source Review #1 - 2026-07-15T03:55:00-04:00

父级复核 `DialogChoiceMemoryService` 与 696 全文等价，也确认 active 通用
`MemoryService.findUsableDialogChoice` 只有 facade 定义、没有真实 Dialog/Npc caller；唯一 reachable route caller
属于矩阵单列的 `findUsableRoute / recordRouteSuccess` 单位，不能借来重复记账。结论：**P0=0/P1=1/P2=0，
BLOCKED / countDelta=0**。阻塞条件是 countUnit 本身缺真实 caller，不是实现错误；不得造 wrapper/self-call，
本内部槽关闭并改派其它互斥 `+1` 单。无已批准业务差异。
