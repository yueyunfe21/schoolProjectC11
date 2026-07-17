# Internal Worker AV - TaskTracker exact-window panel mechanics

## Parent Direct Implementation Task - `W-TTPS-RECT-LOCAL-IMP1` - 2026-07-14T00:31:00-04:00

这是直接实现，不写 Design。目标是把 committed `0114604e` 的任务追踪面板锚点窄区匹配，落成只依赖调用方
exact `WindowNativeBinding` 的本地无输入机械叶子，供后续 typed WINDOW_FACT 接线。Cloud 继续拥有面板算法。

### 唯一写集

- New `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\tasktracker\TaskTrackerPanelRectLocalObservationMechanics.java`
- Append-only 本报告

不得修改现有 `TaskTrackerPanelService.java`、handler/wire/schema、Cloud 文件、tests、pom 或任何其它文件。共享工作区
还有 External A/B/C/D 和另一 Internal Worker；不得回滚、覆盖、清理、删除、提交他人 dirty/untracked。

### 实现合同

1. Spring `@Service`、`final` 类；构造注入 `BoundWindowCaptureService`。入口仅接受调用方 exact
   `WindowNativeBinding`，禁止 `GameClientTracker`、全局首窗口、标题搜索、临时文件和输入。
2. 按 committed 常量原样观察：模板 `images/template/task/wubei_tracker_anchor.png`；window-client 搜索区
   `(6,196)-(207,551)`；阈值 `0.82`；面板相对锚点 `left=-96, top=12, right=86, bottom=350`；
   safe anchor 最大 window-client `(164,353)`。
3. 只 capture 一次窄区并只匹配一次。closed state 至少明确区分
   `PRESENT/ABSENT/CAPTURE_UNAVAILABLE/TEMPLATE_UNAVAILABLE/REPOSITION_REQUIRED/MECHANICS_FAILED`。
   `REPOSITION_REQUIRED` 只表示锚点超出 committed safe area；本叶子绝不拖拽。
4. 仅 `PRESENT` 携 window-client anchor、panel rectangle 与 finite matchScore；其它状态不得携这些值。
   校验 match 数组、finite 坐标/score、阈值、面板矩形和 binding 几何，异常 fail closed 到正确状态。
5. frame/template 必须 finally flush。不得新增 retry/TTL/owner/session/permit/ledger/thread，不得发送输入。
6. 为主入口写简洁 JavaDoc，明确 exact binding、window-client 像素、nullability 和无输入边界。

### 门禁与报告

- 完成后运行 DHXY `mvn -q -DskipTests compile`，记录 exit。
- 向本报告 EOF 追加 `CLAIMED`、`Implementation #1`、精确文件、SHA256、构建结果和 self-QA。
- 自审不算父级 Approved；父级会逐行审查并统一复验。

保护全部 dirty/untracked；禁止 Git mutation，禁止启动 application/server/host/Task/UI/capture/input。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - APPROVED - 2026-07-14T00:42:00-04:00

父级按 committed `0114604e` 与 exact-window 本地机械边界逐行复核
`TaskTrackerPanelRectLocalObservationMechanics.java`，结论 `P0=0 / P1=0 / P2=0`：

- 常量精确保持窄区 `(6,196)-(207,551)`、模板 `wubei_tracker_anchor.png`、阈值 `0.82`、面板偏移
  `(-96,+12,+86,+350)` 与安全锚点上限 `(164,353)`。
- 入口只接受调用方 exact `WindowNativeBinding`，只调用一次 `BoundWindowCaptureService.captureRegion(...)`
  和一次 `ImageFinder.find(...)`；无全局窗口搜索、输入、拖拽、fallback、retry、TTL 或新线程。
- 六态闭合；仅 `PRESENT` 携 window-client anchor/panel rectangle 与 finite score，超出安全区仅返回
  `REPOSITION_REQUIRED`，本叶子不执行拖拽。异常、几何和资源生命周期均 fail closed，frame/template 在 finally flush。
- 当前 SHA256 为 `DCC220A0A9464F3F82AD1EFE2D24D9281F4C1F3CB8B52E67B86791AA891C3BCC`；
  Worker DHXY `mvn -q -DskipTests compile` exit 0。父级会在共享 Java 稳定后运行 fresh compile。

本切片 `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED - 2026-07-14T00:45:00-04:00

- Worker: Internal Worker AV
- Task: `W-TTPS-RECT-LOCAL-IMP1`
- Baseline: committed `0114604e`
- Write scope: only the new `TaskTrackerPanelRectLocalObservationMechanics.java` and append-only updates to this report
- Status: `CLAIMED`

## Implementation #1 - 2026-07-14T01:05:00-04:00

- Status: `DONE`
- Exact implementation file: `D:\mavenProject\DHXY\src\main\java\com\bot\dhxy\service\tasktracker\TaskTrackerPanelRectLocalObservationMechanics.java`
- SHA256: `DCC220A0A9464F3F82AD1EFE2D24D9281F4C1F3CB8B52E67B86791AA891C3BCC`
- Build: from `D:\mavenProject\DHXY`, `mvn -q -DskipTests compile` completed with exit `0`.
- Tests: not created or run, per active no-local-test mode and this brief.

### Self-QA

- Baseline parity: copied committed `0114604e` anchor template, narrow window-client search rectangle
  `(6,196)-(207,551)`, threshold `0.82`, panel offsets `(-96,+12,+86,+350)`, and safe anchor maximum
  `(164,353)`. No expanded search, drag, retry, TTL, ownership, session, permit, ledger, or thread was added.
- Exact-window boundary: the Spring `final` service constructor injects only `BoundWindowCaptureService`; public
  `observe(WindowNativeBinding)` uses only the caller-supplied binding. There is no `GameClientTracker`, title search,
  first-window lookup, task context lookup, temporary file, focus, queue, click, keyboard, or mouse dependency.
- Cardinality: source contains exactly one logical `captureRegion(...)` call and one `ImageFinder.find(...)` call;
  there is no fallback capture/match path.
- Closed result: states are `PRESENT`, `ABSENT`, `CAPTURE_UNAVAILABLE`, `TEMPLATE_UNAVAILABLE`,
  `REPOSITION_REQUIRED`, and `MECHANICS_FAILED`. Only `PRESENT` may carry window-client anchor coordinates,
  window-client panel rectangle, and finite score; record invariants reject partial/non-PRESENT observation fields.
- Fail-closed validation: rejects unusable/undersized/overflowing binding geometry, missing capture/template,
  wrong frame/template geometry, malformed/non-finite/out-of-ROI/below-threshold matches, rounded-coordinate overflow,
  and invalid/out-of-binding panel rectangles. A safe-area maximum breach maps only to `REPOSITION_REQUIRED` and
  performs no drag or other input.
- Resource lifecycle: captured frame and decoded template are both flushed from `finally`.
- Workspace safety: no Git mutation, application/server/host/Task/UI/capture/input execution, deletion, cleanup,
  rollback, or edits outside the one new Java file and append-only report were performed.
- Self-QA is implementation evidence only and is not parent/reviewer approval.

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #2 - APPROVED / EOF placement correction - 2026-07-14T00:43:00-04:00

`Parent Source Review #1` 因父级 Markdown 补丁锚点误命中简报末尾，物理位置落在 Implementation #1 之前；
该段不作为收口信号。本条在真实 EOF 重申交付后源码审查结论，且不改写既有日志历史：

- 当前源码 SHA256 仍为 `DCC220A0A9464F3F82AD1EFE2D24D9281F4C1F3CB8B52E67B86791AA891C3BCC`。
- committed 常量、exact binding、单 capture/单 match、六态/null matrix、无输入边界与 finally flush 均通过。
- 最终结论 `SOURCE APPROVED，P0=0 / P1=0 / P2=0`；父级 fresh DHXY compile 仍在整波稳定后执行。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**
