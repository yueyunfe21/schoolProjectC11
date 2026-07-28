# START-STOP-CONTROL-P1

## Status

`SOURCE REPAIR COMPLETE / P0-P1-P2=0/0/0 / FRESH CLIENT RESTART REQUIRED`

## Runtime Evidence

- `09:29:03` first Start began Cloud sidecar readiness.
- Cloud became ready around `09:30:41`, about 98 seconds later.
- Repeated Start actions created seven `window-task-ui-worker` threads.
- F12 called `stopAll()` before any `WindowTurnLoop` existed and therefore reported `0/5`.
- Pending startup workers were not cancelled and later continued through preflight and loop creation.

## Repair Contract

- One remote start batch may be in flight.
- Pause or stop invalidates the exact pending start epoch before acting on existing loops.
- Cancellation is checked during sidecar readiness, local team-role preflight, before/after loop creation,
  and after start ACK.
- F11 toggles pause/resume; F12 is hard stop; repeated hotkey messages are debounced.
- UI pause/stop remain executable while a normal command is waiting.
- No Xiuluo, Wubei, Cloud task phase, OCR, navigation, or click policy changes are permitted.

## Verification

- Client compile: exit `0`.
- Focused cancellation/preflight tests: `4/4`.
- No runtime, UI, capture, or physical input was executed by the reviewer.
- Acceptance requires a fresh Client process and a stop during sidecar/preflight wait; no old batch may
  create or resume a loop afterward.

## 2026-07-26 FRESH RUNTIME REOPEN / PAUSE-TO-STOP REPAIR

结论：`P0/P1/P2=0/0/0 / P1 REPAIRED / CLIENT COMPILE PASSED /
FRESH CLIENT+CLOUD RESTART REQUIRED`。

Fresh 时间线：

- `22:46:08.783`：两个窗口收到 pause，observer 正常 suspend。
- `22:51:59.199`：stop 到达；两个 `WindowTurnLoop` 同时输出
  `stopRequested=false stopCheckpoint=true` 并退出。
- 此后没有 matching `Cloud task terminal accepted`；registry 为防止重复 run 保留 stopped
  loop，runner 仍投影为 `remoteRunning=true / PAUSED`。
- `22:52:12` 起每次启动均错误进入 `resume paused selected windows`；后续 stop 只反复
  invalidates start epoch，无法终结已经退出的旧 loop。

根因与修复：

1. `WindowTurnLoop.runTurns()` 在暂停等待的 `awaitResumeRequest()` 被 stop interrupt 后
   直接 `return`，绕过方法尾部唯一的 final stop-bearing exchange。现改为 `break`，
   清除 control interrupt 后执行既有 `exchangeOnce()`，发送
   `stopRequested=true` 并等待同响应的 Cloud terminal。
2. `TurnModeGuard.pauseRemote/resumeRemote` 现在要求注册 loop 仍为 running；已停止但因
   terminal 未确认而保留的对象不得再被 UI 报成暂停/恢复成功。

冻结：未修改 Cloud、五环/修罗/五倍 phase、Runner 观察、截图/OCR、导航或物理输入。

验证：Client `mvn -q -DskipTests compile` -> `exit 0`；`git diff --check` 无错误。
本轮未运行 runtime/UI/capture/input。由于当前旧 run 已经在错误路径中退出且未通知 Cloud，
fresh 验收必须重启 Client 并清理 Cloud 旧 slot；预期 pause 后 stop 出现 matching terminal、
registry remove、runner `STOPPED`，再次启动必须创建新 run 而不是 resume。

<!-- TRUE_EOF: START-STOP-CONTROL-P1 PAUSE-TO-STOP-REPAIRED SOURCE-REVIEW-P0-P1-P2=0-0-0 CLIENT-COMPILE-0 FRESH-CLIENT-CLOUD-RESTART-REQUIRED 2026-07-26 -->

## 2026-07-27 F11 TOGGLE CONTRACT RESTORE

- Runtime evidence confirmed every second `Ctrl+Shift+F11` press was received, but the hotkey
  handler called `pauseAll()` again, so a paused task could never resume from the hotkey.
- Restore the validated baseline behavior: the debounced F11 handler calls
  `togglePauseResumeAll()`. All-live-paused resumes; mixed/running state pauses.
- Keep the 500ms debounce and F12 hard-stop behavior unchanged.
- Update the JavaFX hotkey label to `暂停/恢复 Ctrl+Shift+F11`.
- Verification: Client `mvn -q -DskipTests compile` passed. The existing
  `WindowRemoteTurnControlContractTest` ran `7/11`; its four failures concern pre-existing
  task-type acceptance and team-role metadata expectations, not the F11 hotkey path.
- Fresh acceptance requires restarting the Client so Windows re-registers the hotkey with the
  repaired handler.

<!-- TRUE_EOF: START-STOP-CONTROL-P1 F11-TOGGLE-RESTORED SOURCE-REPAIR-COMPLETE CLIENT-COMPILE-0 CONTROL-TEST-7-OF-11-PREEXISTING-FAILURES FRESH-CLIENT-RESTART-REQUIRED 2026-07-27 -->
