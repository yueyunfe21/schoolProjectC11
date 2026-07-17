# I6 Repair #1 Non-binding Preflight Helper

## Observations

- 核对材料：完整读取 `AGENTS.md`、`docs/DHXY_CONTEXT.md`、I6 固定报告、当前
  `PlayerStateFirstAidLocalMacroMechanics.java`，并以只读 `git show 696a12b0:src/main/java/com/bot/dhxy/service/PlayerStateService.java`
  提取 `performCachedFirstAidPlanDirect(...)` 作为业务基线。
- `696a12b0` 基线先把 `baseX/baseY` 初始化为 stored plan base；只有
  `refreshed && refreshedBaseX != -1 && refreshedBaseY != -1` 时才覆盖。刷新失败或任一坐标为 `-1`
  时只记录 fallback 日志，继续使用 stored base。
- 当前 `executeCachedFirstAidPlanDirect(...)` 在 `PlayerStateFirstAidLocalMacroMechanics.java:204-223`
  同样先保存 `plan.baseX()/plan.baseY()`。覆盖条件为单个 AND 条件：refresh present、
  `hasGeometry()`、`getX() != -1`、`getY() != -1` 全部成立才写入 refreshed base。
- refresh absent、geometry 无效、`x == -1` 或 `y == -1` 都进入原 `else`；该分支没有改写
  `baseX/baseY`，因此后续 safe mouse point 和 ordered right-click 仍以 stored base 计算。
- 目标方法内未见新增 retry、restore、TTL、额外 capture/verification、状态写入、目标重排或 fallback 顺序变化。
  覆盖分支之后仍是一次 mouse-away、按 plan 原顺序逐项 right-click、固定 settle delay，再把线程中断映射为
  `INTERRUPTED`，其余映射为 `COMPLETED`。
- 当前迁移方法相对旧类方法已有 exact-binding `Optional` refresh、`hasGeometry()`、input-worker 边界检查、
  常量化 delay 和 enum terminal 等结构差异；本次 Repair #1 所核目标区域没有在这些既有结构上增加新的业务决策。

## Risks

- stored plan base 本身的有效性仍由上游 claim/precheck 合同保证；本方法与 `696a12b0` 一样，在 refresh 无效时不会
  再次拒绝 stored base。父级应继续把该点视为既有基线前提，不应在本返修中追加新 fail-closed 规则。
- 本 helper 按指令未运行 repository-wide `git status`/`git diff`，因此不能提供全仓 dirty/untracked 写集证明。
  “无写集漂移”在此仅指当前目标方法可见行为及 I6 固定报告声明的 Repair #1 范围，不能替代父级的最终 diff 审计。
- 未运行 build、test 或 runtime；本文只提供源码与固定报告的非绑定 preflight 观察。

## Parent-checklist

- [ ] 父级确认覆盖条件四项必须同时成立：present、geometry、`x != -1`、`y != -1`。
- [ ] 父级分别走读 absent、invalid geometry、invalid x、invalid y 四类分支，确认 `baseX/baseY` 始终保留
  `plan.baseX()/plan.baseY()`。
- [ ] 父级确认覆盖条件之后的 mouse-away、ordered target loop、right-click/delay 和 terminal 映射未发生额外变化。
- [ ] 父级在其允许的流程中独立核对实际 diff/write set，确认 Repair #1 没有越出指定 Java 文件和两项坐标谓词。
- [ ] 父级自行执行其统一源码门与构建门；本 helper 不提供 build/test/runtime 证据。
