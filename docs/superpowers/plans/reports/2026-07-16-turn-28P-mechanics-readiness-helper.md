# TURN-28P generic mechanics prerequisite helper precheck

- 日期：2026-07-16（America/New_York）
- 角色：CR271 非绑定 prerequisite helper；本报告只提供 PRECHECK、源码证据、风险和候选写集，不承担审查结论。
- 业务基线：`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。
- 仓库：DHXY `D:/mavenProject/DHXY`（`thin-client-design`）；Cloud `D:/mavenProject/dhxy-cloud-brain`（`navigation-migration`）。
- 用户已确认边界：最小 HTTPS JSON turn；截图和键盘优先 HWND 后台；仅鼠标需要前台；只有 payload 明确要求时，本地才可做机械图像比较。
- 执行限制：本轮只读源码、基线、协议和测试源码；未修改 Java，未运行 Maven/JUnit，未启动 runtime/application/server/Task/UI/capture/input，未执行 Git mutation。
- 唯一写入：本报告。

## 1. PRECHECK 摘要

TURN-28 当前两个 prerequisite 缺口均真实存在，但不需要新增 Service、第二套 runtime 或新的业务协议：

1. `Alt+A`、`Alt+C` 已经存在完整 HWND `PostMessage` 快捷键实现，只因
   `BoundWindowKeyboardService.AltShortcut` 的 `backgroundHwndSupported=false` 被
   `TurnKeyMapper` 拒绝。最小修复是开放这两个既有枚举，并让 turn 输入使用本 action 已冻结的 exact binding；
   不新增 key step、不允许 foreground keyboard fallback。
2. Ctrl probe 不能拆成普通 `KEY_DOWN -> MOVE -> CAPTURE -> KEY_UP` 多 step。现有 free-form
   `KEY_DOWN/KEY_UP` 明确不支持，且拆分后无法保证异常、停止或截图失败时一定执行 Ctrl up。最小方案是在现有
   `CAPTURE` 的 `TurnCaptureSpec` 增加一个可选 `pixelChangeProbe` 机械规格，由
   `TurnCaptureStepExecutor` 在一次 `InputSequences.submitExclusiveAndWait` 回调内完成
   `before capture -> Ctrl down -> wait -> mouse move -> wait -> after capture -> pixel diff -> finally Ctrl up`。
3. 两帧都只存在于 DHXY 内存。`before` 不编码、不上传；`after` 是成功 turn 唯一允许返回的 raw PNG。
   本地只返回 `PIXELS_CHANGED` 或 `PIXELS_UNCHANGED` typed code，不做 OCR、模板、NPC 候选、story blocker、
   点击目标选择或下一步决策。
4. 方案不改变 `696a12b0` 的 Ctrl-before-move 顺序、80/280/100 ms 时序和 `0.05` 变化阈值；Cloud 的
   probe FIFO、OCR/template、验证和业务 fallback 仍留在 TURN-28。

父级需要先冻结本报告第 5 至 10 节的合同，随后才能把 TURN-28P 变成真实 implementation 写集。

## 2. 权威边界

### 2.1 当前 HTTPS turn 规格

`docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md` 已明确：

- `:56-64`：一个 action 是有序 JSON mechanics；后台 HWND 键盘不支持时必须 typed failure，不能静默回退前台。
- `:68-73`：一个 turn 只有一个 frame slot；图片是 raw PNG multipart，不是 Base64 JSON。
- `:108-113`：网络不确定不能重新执行 action；扩大 ROI 或 full-window 是 Cloud 新建 `actionId` 的显式动作。
- `:155-161`：每个 payload 有唯一 `actionId`，重复或不确定身份不能再次执行物理输入。
- `:199-203`：`CAPTURE` 是后台 bound-window/ROI 截图；keyboard 使用后台 provider。
- `:220-232`：Cloud 可以要求上传图片，也可以显式选择本地机械匹配；本地不能自行选择计算归属。
- `:251-270`：图片使用 `multipart/form-data` 的单个 `image/png` part；坐标为 screen-absolute、unscaled。
- `:276-285`：失败证据和失败 step 返回 Cloud，由 Cloud 决定下一 action；DHXY 不选择业务 retry。
- `:319-330`：Cloud 拥有业务顺序、OCR/template 和 retry/fallback；DHXY 只拥有 capture、keyboard、mouse、
  payload 明确请求的本地机械比较和 typed physical result。
- `:354-360`：验收要求仍是一个 JSON payload、raw PNG、精确不缩放坐标、后台截图/键盘、仅鼠标前台。

`docs/superpowers/plans/2026-07-15-https-turn-complete-migration-card-plan.md:680-685` 对 TURN-28 的边界是：
Cloud 持有 NPC 候选 FIFO、OCR/template、验证和 story blocker；DHXY 只做 capture 与原子 mouse mechanics。
本 prerequisite 只补足这条边界依赖的通用 Ctrl probe，不把 NPC 业务下沉本地。

### 2.2 `696a12b0` Ctrl probe 等价点

`git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/NpcClickService.java`
的 `clickNpcByCtrlMenuScan(...)` 提供以下精确顺序：

| 基线位置 | 已确认行为 | 本方案保持方式 |
|---|---|---|
| `:369-380` | 整个 probe 进入一次 `submitExclusiveAndWait`，callback 内不得嵌套 queue | 一个 CAPTURE probe 只提交一次 exclusive callback |
| `:380-384` | 建 ROI，先截 `frameBefore`，随后 Ctrl down | exact ROI 后台 before capture，随后 exact HWND Ctrl down |
| `:386-394` | 必须先 Ctrl，再 wait 80 ms，再移动鼠标，再 wait 280 ms | payload 明示 `keyDownSettleMs=80`、`afterMoveSettleMs=280` |
| `:395-400` | 停止检查后截 `frameAfter`，以 `ImageFinder.isMatch(..., 0.05)` 判断变化 | 同 ROI after capture；固定 RGB channel tolerance 15，payload ratio `0.05` |
| `:401-409` | 两帧释放；无变化不是 transport failure | before/after 均 flush；返回 `PIXELS_UNCHANGED` completed observation |
| `:411-424` | 旧代码随后在本地做 OCR、目标判断和验证 | 不迁入 prerequisite；after raw PNG 上传 Cloud 后由 TURN-28 处理 |
| `:425-428` | `finally` 必须 Ctrl up，再 wait 100 ms | Ctrl up 位于无条件 finally；`keyUpSettleMs=100` |

`ImageFinder.java:150-178` 的机械算法是：RGB 任一 channel 差值大于固定 15 则该 pixel 变化，
`diffRatio = changedPixels / totalPixels`，`diffRatio <= tolerance` 视为同图。基线传入 `0.05`；本方案不新增
OCR、特征、模板或业务判断算法。

## 3. 现有双仓/本地事实

### 3.1 协议 parity 与单帧限制

审计时以下文件 DHXY/Cloud SHA-256 相同：

| 文件 | parity | 审计 SHA-256 前缀 |
|---|---:|---|
| `TurnCaptureSpec.java` | true | `CE41292AC4B9...` |
| `TurnProtocolValidator.java` | true | `E2F81039B638...` |
| `TurnActionGoldenJsonTest.java` | true | `4C68DF634B4A...` |
| `TurnProtocolValidatorContractTest.java` | true | `D3AD730C1BAC...` |
| `action-input-capture.json` | true | `17D27AC802DB...` |
| `outcome-completed.json` | true | `06576660CFEF...` |

`TurnProtocolValidator.java:75-82` 统计 `UPLOAD_IMAGE` 并强制每 action 最多一帧。`TurnCaptureSpec` 当前只有
`region`、`resultMode` 和 TURN-23P 已加入的 `clearPointerIfOverRegion`。新增字段必须在两仓保持字节一致，且必须
保留现有二参数/三参数构造器和 pointer-clear 行为。

这些 protocol/turn 文件在两仓当前均为既有 dirty/untracked 迁移内容。后续 owner 只能在当前内容上增量修改，
不得从其它 commit 重建、覆盖或清理。

### 3.2 Alt+A / Alt+C 缺口

- `BoundWindowKeyboardService.java:61-108` 已实现 HWND `WM_SYSKEYDOWN/WM_SYSKEYUP` 的 Alt down、key down、
  key up、Alt up，并返回 typed `ShortcutAttempt`。
- `BoundWindowKeyboardService.java:236-237` 唯独把 `ALT_A`、`ALT_C` 标为 `false`；其它已使用 Alt shortcut
  多数为 `true`。
- `TurnKeyMapper.java:19-33` 会遍历现有枚举，但只返回 `backgroundHwndSupported()==true` 的项。
- `TurnInputStepExecutor.java:70-94` 因此对 Alt+A/C 返回 `BACKGROUND_KEY_UNSUPPORTED`。
- `TurnInputStepExecutorContractTest.java:141-146` 当前把 Alt+A 拒绝写成旧预期。
- `WindowIdentityDriftP2WiringTest.java:70-71` 还以源码字符串断言 Alt+A/C 必须为 `false`；开放能力时必须同步
  更新这两条陈旧断言，否则完整 testCompile/test suite 会保留错误合同。

仅翻转两个枚举值可以解除 mapper 拒绝，但 turn 当前调用 `pressShortcut(shortcut)` 时会在
`BoundWindowKeyboardService.java:73-90` 再次刷新 binding。为了遵守 `TurnExecutionWindow` 的“一 action 只解析/
刷新一次 exact binding”，最小实现应增加 exact-binding overload，并让 `TurnInputStepExecutor` 传入
`window.binding()`；旧 public API 保留供非 turn caller 使用。

### 3.3 Ctrl probe 缺口

- `TurnInputStepExecutor.java:70-73` 对普通 `KEY_DOWN/KEY_UP` 一律返回 `BACKGROUND_KEY_UNSUPPORTED`。
- `BoundWindowKeyboardService` 只有完整 Alt shortcut，没有 exact HWND Ctrl hold/release transition。
- `TurnCaptureStepExecutor.java:71-124` 目前只支持普通 capture 和可选 pointer-clear；没有 before/after、modifier
  fail-safe 或 pixel-change typed result。
- `TurnCaptureStepExecutor.java:151-169` 已能从 immutable `TurnExecutionWindow.binding()` 后台抓 exact ROI 并
  编码 raw PNG；这个 capture authority 应复用，不应复制第二套截图 Service。
- `InputSequences.java:59-60` 和 `InputActionQueue.java:295-314` 已提供 single input worker exclusive callback。
  `InputActionQueue` 文档明确 callback 内使用 direct input API 且禁止嵌套 queue。
- `InputActionWorker.java:147-156` 在单 worker 上执行 callback。鼠标 move 可在这个边界内使用前台 provider；
  Ctrl 和截图仍走 exact HWND 后台 API。

因此，不能用多个普通 step 拼装 Ctrl probe，也不能让 Cloud 先收 before 再发第二条 action：两种做法都会丢失
Ctrl finally-release 原子边界或把一次机械 probe 扩成跨网络状态机。

## 4. 最小协议扩展

不新增 step type，不新增 Service。只在现有 `CAPTURE` 的 `TurnCaptureSpec` 增加可空字段：

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
PixelChangeProbe pixelChangeProbe
```

建议共享 typed shape（嵌套在 `TurnCaptureSpec`，两仓完全一致）：

```java
public record PixelChangeProbe(
        ModifierKey modifierKey,
        int targetX,
        int targetY,
        int keyDownSettleMs,
        int afterMoveSettleMs,
        int keyUpSettleMs,
        double maxUnchangedPixelRatio) {
}

public enum ModifierKey {
    CONTROL
}

public enum ResultCode {
    OK,
    PIXELS_CHANGED,
    PIXELS_UNCHANGED
}
```

字段语义：

| 字段 | 机械语义 |
|---|---|
| `modifierKey` | V1 只接受 `CONTROL`；不开放任意 VK/code |
| `targetX/targetY` | screen-absolute、unscaled 鼠标目标，参考当前窗口真实左上角，不是 `(0,0)` 相对坐标 |
| `keyDownSettleMs` | Ctrl down 后、mouse move 前等待；TURN-28 传 `80` |
| `afterMoveSettleMs` | mouse move 后、after capture 前等待；TURN-28 传 `280` |
| `keyUpSettleMs` | finally Ctrl up 后清理等待；TURN-28 传 `100` |
| `maxUnchangedPixelRatio` | `ImageFinder.isMatch` ratio；TURN-28 传 `0.05` |

V1 validator 规则：

1. `pixelChangeProbe == null` 时，所有 legacy CAPTURE JSON 和行为不变。
2. probe 要求 non-null `region` 且 `resultMode=UPLOAD_IMAGE`。
3. probe 与 `clearPointerIfOverRegion` 互斥；V1 不定义两种 mouse policy 的组合顺序。
4. `modifierKey` 只允许 `CONTROL`。
5. `targetX/targetY` 必须位于 requested ROI 内；executor 还必须校验 ROI 和 target 都位于同一个 refreshed
   `windowRect` 内。
6. 三个 settle 值都在 `[0, 5000]`；ratio 必须 finite 且在 `[0.0, 1.0]`。
7. action 仍最多一个 `UPLOAD_IMAGE`；probe 不占第二个 frame slot。
8. JSON 缺 primitive 字段或显式 null 必须失败，不使用隐藏默认值。

保留 `TurnCaptureSpec` 现有二参数和三参数构造器，使 legacy fixture、普通 capture、TURN-23P pointer-clear 不发生
源码/JSON漂移。

## 5. 冻结候选 JSON

### 5.1 Cloud action

一次 probe 是一个 action、一个 CAPTURE step、一个新 UUID/actionId：

```json
{
  "contractVersion": 1,
  "actionId": "f9fb4aa8-1117-47da-9d9b-458d70953910",
  "deviceId": "device-alpha",
  "windowId": "window-2",
  "steps": [
    {
      "index": 0,
      "type": "CAPTURE",
      "inputAction": null,
      "input": null,
      "waitMs": null,
      "capture": {
        "region": {
          "x": 1050,
          "y": 330,
          "width": 600,
          "height": 420
        },
        "resultMode": "UPLOAD_IMAGE",
        "clearPointerIfOverRegion": null,
        "pixelChangeProbe": {
          "modifierKey": "CONTROL",
          "targetX": 1320,
          "targetY": 540,
          "keyDownSettleMs": 80,
          "afterMoveSettleMs": 280,
          "keyUpSettleMs": 100,
          "maxUnchangedPixelRatio": 0.05
        }
      },
      "match": null,
      "localService": null
    }
  ],
  "fullWindowFailureEvidence": true
}
```

Cloud 根据 `696a12b0` 的 probe origin/FIFO 生成 ROI 与 target。DHXY 不生成 probe 列表、不扩大 ROI、不循环多个
target。若 Cloud 需要下一个 probe，它发送新 `actionId`；这是基线业务遍历，不是 transport auto retry。

### 5.2 DHXY completed outcome

机械变化与机械未变化都属于一次成功完成的 observation；区别只在 typed step code：

```json
{
  "contractVersion": 1,
  "actionId": "f9fb4aa8-1117-47da-9d9b-458d70953910",
  "window": {
    "deviceId": "device-alpha",
    "windowId": "window-2",
    "windowTitle": "Classic Client - Alpha",
    "nativeHandle": "0x000000000001A2B3",
    "processId": 4242,
    "windowRect": {
      "left": 120,
      "top": 80,
      "width": 1280,
      "height": 720
    },
    "pauseRequested": false,
    "stopRequested": false
  },
  "status": "COMPLETED",
  "failedStepIndex": null,
  "code": "ACTION_COMPLETED",
  "message": "all ordered steps completed",
  "stepResults": [
    {
      "index": 0,
      "type": "CAPTURE",
      "status": "COMPLETED",
      "code": "PIXELS_CHANGED",
      "match": null,
      "localResultJson": null
    }
  ],
  "frame": {
    "purpose": "CAPTURE",
    "contentType": "image/png",
    "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "width": 600,
    "height": 420,
    "region": {
      "x": 1050,
      "y": 330,
      "width": 600,
      "height": 420
    },
    "sourceStepIndex": 0
  }
}
```

HTTP body 仍是现有 multipart：

```text
metadata: application/json   # 上述 outcome
frame:    image/png          # after frame 的原始 PNG bytes，恰好一个 part
```

`before` frame 仅为本地 `BufferedImage`，不得编码成 PNG、写 JSON、写临时业务文件或上传。`after` 不缩放、不
重采样，metadata SHA-256/width/height/region 必须来自同一份 bytes。

## 6. 本地原子执行合同

`TurnCaptureStepExecutor` 对 probe 的唯一合法顺序：

```text
validate immutable TurnExecutionWindow + ROI + target
contextHolder.callWith(window.context(),
  inputSequences.submitExclusiveAndWait(one description, callback))

callback:
  before = backgroundCapture(exact window.binding, exact ROI)
  try:
    post CONTROL DOWN to exact HWND
    wait keyDownSettleMs
    foreground mouse move(targetX, targetY)
    wait afterMoveSettleMs
    check stop/interruption
    after = backgroundCapture(the same HWND, the same ROI)
    changed = !ImageFinder.isMatch(before, after, maxUnchangedPixelRatio)
    retain typed code + after image
    return true for both CHANGED and UNCHANGED
  finally:
    post CONTROL UP to the exact same HWND after every DOWN attempt
    wait keyUpSettleMs

outside callback, only after successful release:
  encode after once as raw PNG
  flush before and after in all exits
  return ResultCode + one TurnFrame
```

关键不变量：

- `PIXELS_UNCHANGED` 不是 queue failure；callback 必须返回 true，并由 typed code 表达观察值。
- Ctrl down 只要被尝试过，finally 就必须尝试一次 Ctrl up；down 结果不确定也不能跳过 up。
- interruption/stop 不能绕过 Ctrl up。up 先执行，随后向外投影 STOPPED/FAILED。
- Ctrl up 未确认成功时不能返回 `PIXELS_CHANGED/PIXELS_UNCHANGED` completed result。
- before/after capture 都使用 action 起点冻结的同一 `WindowNativeBinding`、HWND、process 和 screen-absolute
  `windowRect`；probe 中途不得再次 locate/refresh/title-search。
- callback 内不能调用任何 `submitAndWait`/`submitExclusiveAndWait`，避免 queue-in-queue deadlock。
- screenshot 与 Ctrl transition 使用后台 HWND；只有 mouse move 使用既有前台 input provider。
- 本卡不包含 click。Cloud 收到 after PNG 并完成 OCR/template/候选选择后，另发原子 move+click action。

为传递 typed code，`TurnCaptureStepExecutor.execute(...)` 建议返回一个小型 immutable result（frame +
`TurnCaptureSpec.ResultCode`），`LocalTurnActionExecutor.executeCapture(...)` 再把 `.name()` 放入现有
`TurnStepResult.code`。不修改 `TurnStepResult` DTO，不使用 `localResultJson`，不新增 facade/wrapper chain。

## 7. 后台键盘最小扩展

只扩展现有 `BoundWindowKeyboardService`：

1. `ALT_A`、`ALT_C` 的 `backgroundHwndSupported` 改为 `true`。现有 `TurnKeyMapper` 自动识别
   `Alt+A/ALT_A` 与 `Alt+C/ALT_C`，无需修改 mapper。
2. 增加 exact-binding shortcut overload，供 `TurnInputStepExecutor` 使用：

```java
ShortcutAttempt pressShortcut(
        WindowNativeBinding binding,
        String windowId,
        AltShortcut shortcut)
```

旧 `pressShortcut(AltShortcut)` 保持 public compatibility，并可在完成现有 context refresh 后委托 exact overload；
turn path 不做第二次 refresh。

3. 增加同一 Service 内的 typed modifier transition，不创建新 Service：

```java
KeyTransitionAttempt transitionModifier(
        WindowNativeBinding binding,
        String windowId,
        ModifierKey key,
        KeyTransition transition)
```

V1 只允许 `ModifierKey.CONTROL` 与 `KeyTransition.DOWN/UP`，使用：

```text
WM_KEYDOWN = 0x0100
WM_KEYUP   = 0x0101
VK_CONTROL = 0x11
SCAN_CONTROL = 0x1D
```

该 API 只向传入 binding 的 HWND 发一次 transition 并返回 attempted/success/reason；不读取全局首窗口、不刷新
binding、不回退 `InputProvider.holdCtrl()`、不自动 retry。`UP` 路径即使当前线程已有 interruption，也必须允许
best-effort PostMessage，以免留下 Ctrl held state。

## 8. Terminal 与 correlation 合同

| 情况 | DHXY terminal | step code/frame | Cloud 处理 |
|---|---|---|---|
| before/down/move/waits/after/compare/up 全部完成且变化 | `COMPLETED` | `PIXELS_CHANGED` + after CAPTURE PNG | 校验 correlation 后进行 Cloud OCR/template/业务判断 |
| 全部机械步骤完成但无变化 | `COMPLETED` | `PIXELS_UNCHANGED` + after CAPTURE PNG | 作为 closed observation；Cloud 决定下一个 probe |
| before capture 失败 | `FAILED` | typed capture failure；无 probe code | 不伪造 unchanged；Cloud 决定新 action |
| Ctrl down、move、wait、after capture 或 compare 失败 | `FAILED` 或真实 `STOPPED` | finally up 后才返回；不得带 probe completed code | 不继续 OCR/点击，不自动重发 |
| Ctrl up 失败或结果不确定 | `FAILED` | `CTRL_RELEASE_FAILED`；不得带 probe completed code | 视为 terminal mechanics failure |
| stop/interruption 在 Ctrl held 期间发生 | `STOPPED` | finally up 后投影；不得带 probe completed code | 不把 stop 当 NPC/business false |
| command busy/duplicate/transport uncertain | `DUPLICATE_OR_UNCERTAIN` | 无 fabricated outcome/frame | 同 actionId 不重执行；Cloud 不推导业务成功 |
| action 要求 `fullWindowFailureEvidence=true` 且 probe 失败 | `FAILED` | Ctrl up 尝试完成后，至多一个现有 `FAILURE_EVIDENCE` PNG 替换未返回的 after | 仍遵守 one-frame slot |

Cloud `TurnInvocationResult.java:70-106` 当前只核对 actionId、device/window、step count/index/type。针对
`pixelChangeProbe`，action-aware correlation 还必须增加：

1. completed probe 的 code 必须恰为 `PIXELS_CHANGED` 或 `PIXELS_UNCHANGED`。
2. completed probe 必须有 raw frame 和 metadata；plain CAPTURE 不得伪用 probe code。
3. `frame.purpose=CAPTURE`、`sourceStepIndex` 等于 probe step index。
4. frame region/width/height 必须精确等于 action requested ROI，并位于 outcome 的 exact `windowRect`。
5. metadata 与 multipart bytes 的 contentType/SHA-256/dimensions 必须相符。
6. outcome 的 device/window 必须等于 action；outcome window 的 nativeHandle/process 是 DHXY 本 action 实际冻结的
   binding 证据，Cloud caller 不得把另一窗口 frame 混入。
7. 任一 correlation 不一致都是 fatal contract error；不得降级为 unchanged、普通 failure 或新 action 自动 retry。

## 9. 精确 production 写集候选

父级若冻结此方案，generic prerequisite 应只有以下 production 文件。

### 9.1 DHXY

1. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
   - 增加可空 `pixelChangeProbe`、closed enums/records、兼容构造器。
2. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
   - 增加 V1 probe validator、pointer-clear 互斥、单帧不变量。
3. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/driver/BoundWindowKeyboardService.java`
   - 开放 Alt+A/C；增加 exact-binding shortcut overload 和 Ctrl DOWN/UP typed transition。
4. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutor.java`
   - turn Alt shortcut 改走 immutable exact-binding overload；普通 free-form KEY_DOWN/UP 继续不支持。
5. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutor.java`
   - 实现一次 exclusive callback 的 before/down/move/after/compare/finally-up；成功只编码 after。
6. `D:/mavenProject/DHXY/src/main/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutor.java`
   - 把 capture typed result code/frame投影到现有 `TurnStepExecution`。

明确零修改：`TurnKeyMapper.java`、`TurnStepResult.java`、`TurnOutcome.java`、`TurnFrame.java`、
`ImageFinder.java`、`InputSequences.java`、`InputActionQueue.java`、`InputActionWorker.java`、任何 Service/Task/caller。

### 9.2 Cloud

1. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
   - 与 DHXY byte-identical。
2. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidator.java`
   - 与 DHXY byte-identical。
3. `D:/mavenProject/dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnInvocationResult.java`
   - 增加 probe action/result/frame exact correlation；不加入业务 OCR/NPC 决策。

不在 TURN-28P 修改 Cloud `NpcClickService`、NPC model/decision、Task、host 或 application。那些仍属于 TURN-28
consumer 卡，且必须等 generic prerequisite source/test 门完成后再接线。

## 10. 精确 test/fixture 写集与命名验收

以下仅是后续 implementation 的测试合同；本 helper 没有创建或运行测试。

### 10.1 两仓 byte-identical protocol tests/fixtures

- Modify both copies of
  `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnActionGoldenJsonTest.java`。
- Modify both copies of
  `src/test/java/com/bot/dhxy/cloud/turn/protocol/TurnProtocolValidatorContractTest.java`。
- Create byte-identical
  `src/test/resources/cloud-turn/v1/action-capture-pixel-change.json` in both repos。
- Create byte-identical
  `src/test/resources/cloud-turn/v1/outcome-capture-pixel-change.json` in both repos。

Acceptance：legacy fixture 缺 `pixelChangeProbe` 仍 round-trip；新 action/outcome fixture 精确 round-trip；所有缺字段、
null primitive、越界 timing/ratio、full-window、pointer-clear 共存、非 UPLOAD_IMAGE、target 不在 ROI、第二 uploaded
frame 都被拒绝；两仓四份镜像文件分别 SHA-256 相同。

### 10.2 DHXY named tests

- Modify `src/test/java/com/bot/dhxy/cloud/turn/TurnInputStepExecutorContractTest.java`。
  - Alt+A/C 使用 exact HWND 后台 API返回完成；unknown shortcut 与普通 KEY_DOWN/UP 仍 typed failure；零前台
    keyboard fallback。
- Create `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePixelChangeProbeContractTest.java`。
  - 用 fake capture/keyboard/input trace 证明唯一顺序：before、DOWN、80、MOVE、280、after、compare、UP、100。
  - changed/unchanged 都只提交一次 queue、只编码 after、只返回一份 PNG。
  - before/after exact ROI、同 HWND/process、screen-absolute/unscaled。
  - 在 DOWN 后每个异常点、stop/interruption、after capture failure、compare failure、UP failure 中，UP 尝试恰好一次；
    无 completed probe code 泄漏。
  - down PostMessage 返回失败/不确定也执行 UP；callback 内零 nested queue。
- Modify `src/test/java/com/bot/dhxy/cloud/turn/LocalTurnActionExecutorContractTest.java`。
  - `PIXELS_CHANGED/UNCHANGED` code 和 after frame进入现有 outcome；mechanics failure/stop 不伪 completed。
- Keep and run `src/test/java/com/bot/dhxy/cloud/turn/TurnCapturePointerClearContractTest.java`。
  - 证明新增 probe 不回归 TURN-23P pointer-clear；二者共存由 validator 拒绝。
- Keep and run `src/test/java/com/bot/dhxy/cloud/turn/TurnCaptureStepExecutorContractTest.java`。
  - 证明 legacy two-arg capture constructor、full-window/ROI raw PNG 保持。
- Modify `src/test/java/com/bot/dhxy/window/runtime/WindowIdentityDriftP2WiringTest.java`。
  - 删除 Alt+A/C 必须为 false 的陈旧字符串断言，改为精确后台能力/无 foreground fallback 断言。

### 10.3 Cloud named tests

- Create
  `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnCapturePixelChangeInvocationContractTest.java`。
  - valid changed/unchanged outcome + raw PNG通过；缺 frame、错 code、错 step index/type、错 ROI/dimensions/SHA、
    plain capture 伪 probe code均 fatal。
- Modify `src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/client/TurnGameClientContractTest.java`。
  - 新 fixture 通过真实 multipart parser；metadata + 恰好一个 raw `image/png`；无 Base64、无第二 frame part。

建议父级把本 prerequisite 的显式命名门冻结为：

```text
DHXY:
TurnActionGoldenJsonTest
TurnProtocolValidatorContractTest
TurnInputStepExecutorContractTest
TurnCapturePixelChangeProbeContractTest
LocalTurnActionExecutorContractTest
TurnCapturePointerClearContractTest
TurnCaptureStepExecutorContractTest
WindowIdentityDriftP2WiringTest

Cloud:
TurnActionGoldenJsonTest
TurnProtocolValidatorContractTest
TurnCapturePixelChangeInvocationContractTest
TurnGameClientContractTest
```

这些测试均只能使用 fake capture/keyboard/input 与内存 PNG；不得启动真实应用、server、Task、UI、桌面截图或输入。

## 11. 活跃写集冲突审计

| 当前已知活动/待返修工作 | 精确 production 写集 | 与 TURN-28P 候选冲突 |
|---|---|---|
| TURN-33 Goodall | Cloud `SummonSkillService.java`、`CloudSummonSkillWholePassCapability.java`、`CloudTaskExclusiveInteractionAuthority.java` | 无文件冲突 |
| TURN-22 generic-mechanics 返修待前置 | Cloud `TeamReturnService.java`、`CloudTeamReturnPortAssembly.java` | 无文件冲突；语义依赖 TURN-28P 的通用 atomic input/capture，但不得在 TURN-22 私有写集复制 mechanics |
| TURN-28 readiness helper | 仅其固定 Markdown 报告 | 无文件冲突 |
| TURN-34A readiness helper | 仅其固定 Markdown 报告 | 无文件冲突 |

需要特别保护的共享 dirty 内容：

1. 双仓 `TurnCaptureSpec`、`TurnProtocolValidator` 和 protocol tests/fixtures 是当前未跟踪迁移文件，且含已完成的
   TURN-23P pointer-clear；未来 owner 必须先读取当前字节再增量编辑。
2. DHXY `TurnCaptureStepExecutor`、`LocalTurnActionExecutor`、turn tests 同样是未跟踪迁移内容；不能 checkout、
   reset、重抄旧版本或清理。
3. 若 TURN-28 consumer 已开始修改同一 protocol/capture 文件，必须先释放或合并成唯一 owner；不得同文件双写。
4. TURN-33 当前是已知 Java writer。按父级规则，writer 活动时不运行共享 Maven；这不妨碍冻结/派发互斥源码卡，
   但 build cohort 必须等待 writers 稳定。

## 12. 风险与父级冻结点

### Risk-1：Ctrl stuck

最高风险不是 pixel diff，而是任何异常绕过 Ctrl up。验收必须逐阶段注入异常，并证明 DOWN 尝试后 UP 恰好一次。
不得用普通 step executor 拼装，也不得依赖 Cloud 后续 action 清理 Ctrl。

### Risk-2：exact binding 漂移

现有 shortcut API会刷新 binding。turn mechanics 必须传 immutable action binding；probe 期间禁止再次 refresh/locate。
否则 before、Ctrl、mouse、after 可能跨 HWND/process。

### Risk-3：changed 与 failure 混淆

`PIXELS_UNCHANGED` 是成功观察，不是执行失败。capture null、尺寸不一致、Ctrl transition 失败、compare 异常或 release
失败也不能伪装为 unchanged。

### Risk-4：一帧协议被暗中扩张

before 只能留内存。若为 debug/测试写出第二 PNG、Base64 或第二 multipart part，就破坏 one-frame contract。

### Risk-5：本地业务回流

本地只计算 pixel changed bit。不得把旧 `scanMenuAndVerifyKeywordDirect`、OCR/fuzzy、tag template、候选 FIFO、
story blocker、click decision 或 probe loop搬进 executor。

### Risk-6：将 Cloud 下一 probe误写成自动 retry

Cloud 可以按 `696a12b0` 业务 FIFO 发下一 target，每次必须新 UUID/action。transport 层不得自动重发并重新执行旧
action；不新增 owner、permit、session、ledger、TTL、compaction 或 durable workflow。

父级冻结时还应明确：

1. typed code 使用本报告的 `OK/PIXELS_CHANGED/PIXELS_UNCHANGED`，不另建 DTO payload。
2. fixed RGB channel tolerance 继续为 `ImageFinder` 现有 15，仅 ratio 由 payload 明示。
3. probe 与 pointer-clear 在 V1 互斥。
4. release failure 一律不产生 completed probe code。
5. failure evidence 只能在 Ctrl up 尝试后获取，且继续占唯一 frame slot。
6. Alt+A/C 与 Ctrl 均使用 exact HWND 后台 API；mouse move 是唯一前台 mechanics。

## 13. PRECHECK 结论

最小通用实现可以收敛为：双仓一个可选 `CAPTURE.pixelChangeProbe` 协议字段，DHXY 复用现有 capture executor、
single input queue、existing `ImageFinder` 和 `BoundWindowKeyboardService`，Cloud 只补 action-aware correlation。
它不需要新 Service、第二帧、跨请求 probe state、业务 retry 或本地 NPC 逻辑。

当前材料足以让父级冻结 TURN-28P 的 exact production/test 写集。父级冻结前，本报告不授权任何 owner 修改上述
Java；冻结后应先完成 generic prerequisite 的源码与命名测试门，再让 TURN-28 consumer 接入 Ctrl candidate flow。

<!-- TRUE_EOF: TURN-28P generic-mechanics-prerequisite PRECHECK COMPLETE 2026-07-16 -->
