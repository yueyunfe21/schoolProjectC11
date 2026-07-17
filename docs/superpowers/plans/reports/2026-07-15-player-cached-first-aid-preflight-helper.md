# Player Cached First-Aid Delivery Preflight Helper

## Preflight Identity

- role: `Delivery Preflight Helper`；仅做非绑定预检，不是 reviewer，父级保留最终裁决。
- delivery: Internal I6 `PlayerStateService::performCachedFirstAidPlanNow (+1)` / `NO_CODE_CHANGE`。
- result: `PREFLIGHT_RISK`
- restrictions observed: 仅只读检查源码、业务基线与 I6 报告；未修改 I6 报告、CR271 或源码；未运行 build/test/runtime/Git。

## Inputs Read

- I6 报告真实 EOF：`docs/superpowers/plans/reports/2026-07-15-cloud-player-position-count-unit-worker-i6.md:290`；末段 `:215-290` 是父级选择 696 等价口径后的 `NO_CODE_CHANGE` 交付。
- 用户批准的基线入口：`docs/业务逻辑.md:215-223,1255+`；修罗迁移前业务权威指向 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- 本机只读迁移前基线快照：`D:/mavenProject/DHXY-local-baseline/src/main/java/com/bot/dhxy/service/PlayerStateService.java:307-374`、`AutoCombatService.java:378-588`。其目标方法与 caller 内容同 I6 报告 `:152-170` 所列 696 行为一致。
- Cloud：`PlayerStateService.java:269-352,500-529,1400-1422`、`AutoCombatService.java:380-405,442-473,560-589`、`CloudPlayerStateFirstAidPort.java:68-113` 及 closed command/result/outcome。
- DHXY：`LocalRemoteGameCommandHandler.java:206-503,1858-2018`、`PlayerStateFirstAidLocalMacroMechanics.java:96-145,181-235,263-269,640-659`、双侧 payload 与 `WindowNativeBindingRefreshService` / `WindowNativeBinding`。

## Baseline-Equivalent Checks

1. **入口先 claim/clear，失败不恢复**：696 基线 `PlayerStateService:307-337` 在 checkpoint 后读取 plan 并立即清 slot；active Cloud `:319-352` 同序。absent/empty/invalid-base 返回 false 时 slot 已清；valid plan 的 `NOT_EXECUTED`、typed `INTERRUPTED`、异常/stop unwind 均没有恢复赋值。
2. **caller retry 归 caller**：active Cloud 四个调用仅位于 `AutoCombatService:399,464,573,577`，与迁移前快照 `:398,463,572,576` 同形。只有 follower `:573` 返回 false 后由 caller 在 `:574-577` 重做一次 probe/consume；Service、port、transport、handler 均未添加 retry。
3. **ordered targets / captured base**：Cloud `PlayerStateService:523-529` 按原 list 顺序逐项映射 `name/relX/relY/threshold`；command 和 DHXY payload 均 `List.copyOf`，handler `:1962-1971` 顺序重建，mechanics `:225-231` 顺序点击。plan base 原样进入 command/handler/local plan。
4. **four-bar 固定顺序**：DHXY mechanics `:263-269` 固定为人物血、人物法、宝宝血、宝宝法；双侧 result constructor 都校验四项、唯一且同序。Cloud 从 observation 顺序生成 target，未排序或复扫。
5. **checksDoneThisRound / boolean**：active Cloud valid-plan 路径仅 `PlayerStateService:349` 增加一次并在 `:352` 返回 true；absent/empty/invalid-base 不计数并返回 false。全 Cloud Java 搜索显示该方法只有上述四个 caller；follower 第二次调用只在第一次 false 后发生，因此不会对同一个 valid claim 重复计数。
6. **closed terminal**：`CloudPlayerStateFirstAidPort:91-107` 将 `EXECUTED` 解成 operation-matched typed result、`NOT_EXECUTED` 解成 empty、其余 `STOPPED/UNKNOWN` 进入 fatal unwind。typed `INTERRUPTED` 在 Service 只形成 `completed=false` warning，随后仍计次/true；所有 terminal 都保持 plan 已清且无自动 retry。
7. **active exact-context / exact-window 链**：Cloud task lifecycle 通过 `TaskExecutionContextHolder.callWith(...)` 绑定当前 context，port `:83-89` 从该 holder 取当前 `CloudGameClient`。DHXY handler 在 `:457-461` 先校验 registration、bound window 和 run revision；cached operation 在 `:1896-1913` 以 `access.context()` 进入一次 `submitRemoteExclusiveAndWaitDetailed`，callback 内直接调用 mechanics，无 queue-in-queue。DHXY 旧本地 task/service caller 仍存在，但不在本次 Cloud remote command 链内，也不修改 Cloud `checksDoneThisRound`。

## Delivery Risk

### Captured-base fallback is not fully equivalent to 696

- 696 基线 `PlayerStateService:346-360` 只有在 `refreshWindowState()` 成功且 `refreshedBaseX != -1 && refreshedBaseY != -1` 时，才用刷新坐标覆盖 plan captured base；否则保留 captured base。
- 当前 DHXY `PlayerStateFirstAidLocalMacroMechanics:206-220` 只要求 refresh result present 且 `hasGeometry()`，随后无条件用 refreshed X/Y 覆盖 captured base，没有 696 的 X/Y `-1` 门禁。
- `WindowNativeBindingRefreshService:46-63` 仅拒绝 snapshot 缺失或宽高非正；会原样接受 `live.x()/live.y()`。`WindowNativeBinding:59` 的 `hasGeometry()` 也只检查宽高，因此“宽高有效、X 或 Y 为 -1”的 refreshed binding 能进入覆盖分支。
- 运行后果：Cloud 入口 plan 可是 valid（captured base 非 `-1`），但 input-worker 刷新若得到 X/Y `-1`，当前 mechanics 会从 `-1 + rel` 计算物理点击；696 会保留 captured base。该差异直接违背 I6 `:267-268` 所述“刷新不可用时使用 plan captured base”，使 `NO_CODE_CHANGE` 的严格 696 等价证据不闭合。

## Parent Handoff

- preflight result: `PREFLIGHT_RISK`
- clear evidence: claim/clear、不恢复、caller-owned retry、ordered targets/four-bar、terminal、boolean/count 与 active Cloud caller 均已核清。
- risk evidence: DHXY cached mechanics 缺少 696 的 refreshed X/Y `-1` fallback 门禁；父级应在最终交付裁决前处理或取得明确的业务差异口径。
- verification performed: 静态源码/报告逐行核对；按禁令未运行 build、test、runtime 或 Git。
