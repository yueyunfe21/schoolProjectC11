# AutoCombatPanel Visible Repair #1 Preflight Helper

## Observations

- 本报告是 Delivery Preflight Helper 的非绑定预检，仅核对当前 Cloud
  `AutoCombatPanelService.java:117-263` 与 Internal I7 固定报告中的 Repair #1 条件；未扩展到其它实现路径。
- 初次 `AUTO_COMBAT_PANEL` fact terminal：
  - `OBSERVED + FOUND` 重建 `AutoCombatPanelMatch`，随后保持 `clearAutoPanelMissing -> visible log -> return`。
  - `OBSERVED + NOT_FOUND/CAPTURE_FAILED` 才进入 baseline miss 分支。
  - `UNKNOWN` 在发送 `Alt+8` 前抛 `TaskFatalException`。
  - `NOT_EXECUTED` 在 `Alt+8` 日志和 bundle 前立即返回 `null`，不写 panel-missing。
  - `STOPPED` 先走 `TaskCheckpoint`；未确认 stop 时转 fatal。错误的 `EXECUTED` terminal 被拒绝。
- 唯一 input bundle terminal：
  - bundle 只含有序的 `PRESS_ALT_8 -> SLEEP(waitAfterOpenMs)`。
  - `UNKNOWN` 在普通 input-failed 分支前抛 `TaskFatalException`。
  - `NOT_EXECUTED` 保持 `sent=false -> recordAutoPanelMissing(source + ":input-failed") -> null`，对应 `696a12b0` 的 submit-false 效果。
  - `STOPPED` 保持 checkpoint；错误的 `OBSERVED` terminal 被拒绝；只有 `EXECUTED` 才进行第二次 fact read。
- 重试 `AUTO_COMBAT_PANEL` fact terminal：
  - `UNKNOWN` 在 second-miss 日志和 missing watchdog 前抛 `TaskFatalException`。
  - `NOT_EXECUTED` 在 second-miss 日志和 missing watchdog 前立即返回 `null`。
  - `OBSERVED + NOT_FOUND/CAPTURE_FAILED` 保持 `still-not-found log -> recordAutoPanelMissing(:not-found-after-alt8) -> null`。
  - `OBSERVED + FOUND` 保持 `recordAutoCombatRefresh -> clearAutoPanelMissing -> visible-after-Alt+8 log -> return`。
- 当前方法静态计数为 `readWindowFact=2`、`executeInputBundle=1`、`PRESS_ALT_8=1`、`SLEEP=1`；没有循环、第二个 bundle 或额外 fact read。
- 与 `696a12b0` 对照，业务顺序仍为：首次观察 -> miss 后一次 `Alt+8 + waitAfterOpenMs` -> 第二次观察 -> second miss 或 refresh/clear/return。Repair #1 新增的 transport 分支均位于对应业务负事实或物理输入之前。

## Risks

- `UNKNOWN` 被提升为 fatal 后不会进入 baseline 的普通 missing 分支；这是 Repair #1 的明确条件。父级仍需确认该异常沿现有 task terminal 路径结束当前调用，不被上层转换成普通 `null/false` 后继续。
- 初次/重试 fact 的 `NOT_EXECUTED` 直接返回 `null`，因此既不触发 `Alt+8`，也不累计 missing watchdog；这是 Repair #1 的明确条件。顺序若后续被移动到日志、bundle 或 `recordAutoPanelMissing` 之后，会重新制造未经观察的业务负事实。
- bundle 的 `NOT_EXECUTED` 与 fact 的 `NOT_EXECUTED` 语义刻意不同：前者保留 baseline input-failed missing 状态，后者不写 missing。后续整理 terminal 分支时存在被错误统一的风险。
- 本次按指令未运行 build、test 或 runtime；报告只提供源码顺序与静态计数证据。

## Parent-checklist

- [ ] 确认初次 fact `UNKNOWN` 为 fatal，`NOT_EXECUTED` 位于 `Alt+8` 日志和 bundle 之前。
- [ ] 确认重试 fact `UNKNOWN` 为 fatal，`NOT_EXECUTED` 位于 second-miss 日志和 missing watchdog 之前。
- [ ] 确认 bundle `UNKNOWN` 为 fatal，而 bundle `NOT_EXECUTED` 仍走 baseline input-failed missing 分支。
- [ ] 确认方法内恰有两次 `AUTO_COMBAT_PANEL` read、一次 input bundle，且 bundle 顺序为 `PRESS_ALT_8 -> SLEEP(waitAfterOpenMs)`。
- [ ] 确认首次 found、input-failed、second miss、second found 四条 baseline 分支的 log/state/return 顺序未变。
- [ ] 确认没有新增 retry、TTL、fallback、第二次输入、额外 observation 或 state 字段。
- [ ] 将本报告仅作为父级源码复核输入，不作为状态或记账裁决。
