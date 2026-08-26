# AGENTS.md — DHXY Project Agent Instructions

This repository is the DHXY desktop automation project. When using Codex or any coding agent, read this file first, then read `docs/DHXY_CONTEXT.md` before making changes.

## 强制规范零：禁止无边界递归扫描

- 禁止从仓库根目录、工作区根目录或更宽目录直接运行 `Get-ChildItem -Recurse`、`dir /s`、`tree /f`、递归目录体积统计，或把全量递归结果装入数组后再过滤。这类命令会遍历 `.git`、构建产物、日志和图片语料，可能把磁盘与内存打满。
- 查代码和文件名优先使用 `rg`、`rg --files`、`git ls-files`；查日志优先读取已知日志文件并用 `rg` 定位，不允许先递归找所有 `log/out/err` 文件。
- 确需扫描时，必须先限定一个明确的小目录，排除 `.git`、`target`、`build`、`node_modules`、日志与图片语料目录，并同时限制结果量和运行时间。预计超过 30 秒的扫描要先告知用户；一旦磁盘队列或内存持续上升，立即停止。

**开工前的四件事,一件都不许跳(详见下面三节):**

1. **读你要改的那个任务的业务文档**(下表对照)。
2. **读 `docs/云端迁移常见错误清单.md`。**
3. **为本卡建立 `docs/rule-traceability/G*.json`，并通过业务规则门禁。**
4. 查完日志定位到错误后,**把结论写回清单**。

用户不应该需要每次口头提醒这三条。

## 强制规范一:先读你要改的那个任务的业务文档(所有 agent 一律遵守)

**动任何一个任务的代码之前,先把那个任务的业务文档读完。** 不是"有空看看",是前置步骤。

| 你要改的任务 | 必读文档 |
|---|---|
| **天庭**(TIANTING / G005) | `docs/天庭任务流程大MD.md` |
| **新手**(XINSHOU / G003) | `docs/新手任务流程草案.md` |
| **五倍**(WUBEI) | `docs/WUBEI_BUSINESS_DIFF_AUDIT.md`、`docs/WUBEI_CLOUD_MIGRATION_BASELINE.md` |
| **修罗**(XIULUO_V2) | `docs/XIULUO_CLOUD_MIGRATION_PLAN.md` |
| **五环**(WUHUAN / FiveRing) | `docs/业务逻辑.md`(见「修罗与五倍普通怪共用」「五倍 Dialog Interest 生命周期规则」等章节) |
| 跨任务共用能力(通用盒子、三技能维护、快脱战回城、Dialog Interest) | `docs/业务逻辑.md` 对应章节 |
| 卡内实施细节、已决议的开放问题 | `docs/superpowers/plans/` 下该卡的 MD |

**为什么这条排在最前面**:业务文档里写死的东西(坐标、模板、优先级、"这一步在本地做还是云端做")是**用户实测拍板的结论**,不是建议。绕过它自己发挥,结果就是把用户已经验证过的东西推翻重做一遍,再被打回来。

具体要求:

1. **改之前读**,不是写完了再回头找依据。
2. 业务文档、当前代码、最新推送基线或拟议实现发生实质冲突时，**任何 Agent 都无权自行选边**。必须立即停止业务实现，在当前 G 卡和追踪文件中写明冲突规则、两种选择、运行影响与建议，然后询问用户。只有用户针对该冲突作出明确决定后，才能先更新权威业务文档和决策记录，再继续代码。一般性的“修好”“完成该卡”“按计划做”不构成冲突项授权。
3. 文档里没写到的地方**才**轮到自己判断;判断了要在回复里点明"这条文档没写,我按 X 做的",让用户能一眼否决。
4. **实测得到的新数值(坐标、尺寸、阈值、行剖面)要写回业务文档**,并注明测量来源。下一个人不该再量一遍。
5. 用户说"参考修罗怎么做的"这类话时,指的是**去读修罗的代码和文档,照抄它的时序**,不是照着印象自己发挥。

## 强制规范二:常见错误清单(所有 agent 一律遵守)

`docs/云端迁移常见错误清单.md` 是这个项目的错误账本。它不是背景阅读,是**必经流程**:

1. **写任何代码之前,先通读 `docs/云端迁移常见错误清单.md`。** 这里记的每一条都是本项目真实踩过、而且**反复踩**的坑(事实链断裂族)。不读就写,等于重踩一遍。
2. **每次查日志找错,查完必须把结论写回这份清单。** 只要从日志里定位到一个错误,就在清单里登记它——不管它看起来多小、多"这次特有"。
3. **清单里已有同类条目时,不要新开一条,给原条目计数 +1**,并补上这次的现场(时间、任务、日志关键行)。同一个坑第二次出现,说明第一次的修法没有覆盖住,这个信息比新增一条更有价值。
4. **重复出现的条目要显式高亮**(标题加 `🔴 重复 xN`),让它在清单里一眼可见。计数越高的条目,越应该被当成架构问题而不是个案。
5. 清单的格式与计数约定见该文件开头的「登记规范」一节。

这条规范优先于任何"赶时间"的判断:**跳过读清单、或者查完日志不登记,都算没做完。**

## 强制规范三:业务规则追踪与冲突停工门禁(所有 agent 一律遵守)

`docs/BUSINESS_RULE_GOVERNANCE.md` 是规则治理流程，`docs/business-rules.json` 只是业务 MD 的索引，
不复制或覆盖业务语义。任何 G 卡准备修改任务行为、Client/Cloud 交互、Runner 事实、输入时序、OCR/template、
导航、恢复、重试、超时或生命周期前，必须：

1. 给被触及的业务 MD 规则分配稳定 `BR-<DOMAIN>-NNN` 编号，并登记到 `docs/business-rules.json`；
2. 建立 `docs/rule-traceability/G*.json`，列出基线、受影响路径、规则编号，以及真实
   `producer -> transport -> consumer -> ordering`；
3. 跨边界规则必须列出会因生产者消失、metadata 丢失、顺序颠倒或 consumer 不可达而失败的连通性合同；
4. 运行 `node scripts/check-business-rule-gate.js --card Gxxx`。门禁不通过不得写业务代码、不得交 worker、
   不得声称 review passed；
5. 若追踪文件存在未决冲突，其状态必须是 `BLOCKED_USER_DECISION`。禁止把冲突写成“Agent 已判断”、
   禁止先改 MD 再把改后的 MD 当成批准证据。

项目采用增量登记：不用一次性给全部历史规则编号，但某条规则一旦被新卡触及，就必须在修改前纳入索引。

## Project goal

## Worktree selection

- `D:\mavenProject\DHXY` is a protected baseline/reference worktree. Do not implement, migrate,
  test, or modify project work there unless the user explicitly names this exact worktree.
- The current active implementation and runtime worktree is `D:\mavenProject\DHXY-cr271` unless
  the user explicitly selects another worktree. Before any edit or command that changes files,
  state the target worktree in the active-work record and verify it from the current shell path.
- If a task was accidentally implemented in the root `DHXY` worktree, first make a narrowly scoped
  rollback of that task's own changes there; never use a broad checkout/reset that could erase
  unrelated dirty work. Then re-implement only in the user-selected worktree.

The project is a Java/Spring/JavaFX automation tool for 大话西游2 classic client workflows. The current major goal is to make the 五环 task run reliably across one or more game windows.

The intended final shape is:

- A JavaFX desktop controller can register multiple game windows.
- Each registered window has its own runtime state, role identity, native hwnd binding, screenshots, temp files, and task state.
- Multiple windows can run tasks concurrently without keyboard/mouse input crossing between windows.
- All real keyboard/mouse actions are serialized through a single input queue where needed, so only one physical input sequence runs at a time.
- Screenshot/OCR/template matching logic must use the correct window binding and correct temp files.
- The code should remain debuggable from local logs, especially `logs/dhxy-console.log` and `logs/tracker-coordinate.log`.

## Current branch

Use the `dev` branch unless the user explicitly says otherwise.

## Work-card naming

- All new project work cards must use the `G` prefix and the next available `G` number.
- Do not create new `CR*` cards. Historical `CR*` records remain historical only and must not establish
  a second active card numbering system.
- A new investigation, repair, implementation, review, or acceptance task must be recorded under its
  `G*` card in `docs/PACKAGE_ARCHITECTURE.md`, with the matching active-work summary when applicable.

## Important user preferences

The user is actively debugging and expects direct, practical changes. Do not hide behind vague explanations. When making changes, explain exactly which file and logic changed.

> **Current no-test override (2026-08-11):** Do not create or run any test of any kind unless the user later explicitly authorizes that specific test or test family. This includes unit, integration, contract, replay, image/testcase, runtime, application, UI, capture, and input tests. This override suspends every standing or automatic test exception below, including the G016 exception. A request to fix, compile, review, or hand off code is not test authorization. State plainly that the change is untested when handing it off.

Important behavior constraints:

1. Do not casually rewrite or bypass user-validated business logic.
   - Example: do not change `GameStateUtil.isMovingByPixelDiff()` just to avoid symptoms.
   - If a validated detector appears wrong, first inspect screenshots, call path, window binding, temp file path, and logs.

2. Before claiming or implementing any sprint card, every agent must compare against the latest pushed code for the touched business path.
   - Treat the latest pushed code as the business-logic baseline. Current local business differences are not trusted during migration and must be restored to the pushed behavior by default.
   - Record the relevant baseline in `docs/ACTIVE_WORK.md` before editing: current branch, latest pushed commit if known, `git status`, and the relevant `git diff` / `git show` evidence for files being touched.
   - Framework migration work may move scheduling, parking, wakeup, ownership, or diagnostics plumbing, but it must not change task business decisions by accident.
   - If a patch would change task phase semantics, prompt interpretation, OCR/template/click/navigation order, fallback order, or when a probe/NPC/dialog is considered resolved, stop and write the conflict in Markdown; do not keep the local behavior as part of migration unless the user explicitly opens a new behavior-change story.
   - Do not convert runner/ready-event negative signals into new business truth unless the latest pushed business logic already did that.

2A. Business-logic baseline gate (mandatory for 五倍 / 修罗).
   - Before investigating, reviewing, or editing any 五倍/修罗 task behavior, every agent must first read
     `docs/业务逻辑.md` and locate the applicable baseline section/table. It is the user-approved business
     contract, not optional background reading.
   - The confirmed pre-cloud local baseline is the default behavioral authority (currently 修罗 uses
     `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` where the business-logic document says so). Migration
     work must reproduce that baseline unless a user-approved CR explicitly authorizes a behavior change.
   - If a CR does not explicitly say it changes business behavior, it may only move implementation ownership
     (local/cloud), scheduling, diagnostics, or plumbing. It must preserve decision conditions, phase order,
     keep-turn/park boundaries, retry and fallback order, verification count, and expiry semantics.
   - In particular, an agent must not add or reintroduce a TTL, extra verification/read, park/yield, retry,
     cleanup, fail-closed rule, or new cloud gate merely because it appears safer. Such a change requires a
     separate CR whose behavior change is explicitly approved by the user.
   - A CR authorizes its stated outcome, not an agent to choose an unapproved business-semantic route to that
     outcome. If the implementation plan has any option that would change the baseline decision, condition,
     phase transition, priority, fallback, timing/expiry, or input/verification order, stop before coding.
     Write the concrete options, affected baseline rule, expected runtime consequence, and recommendation in
     the CR card and ask the user to decide. Do not infer approval from a general request such as “完成 CR” or
     “修好这个问题”. Resume only after the user explicitly selects/approves the behavior change.
   - Before handoff, the CR card must state which `docs/业务逻辑.md` rule/baseline rows were checked and list
     every intentional behavioral difference. If there are none, write `无已批准业务差异；按基线等价迁移`.

2B. Business rules must have an executable connectivity gate.
   - Reading or updating Markdown is not sufficient when a rule crosses Client/Cloud, runner/task, producer/consumer,
     or lifecycle boundaries. Before changing such a path, identify the concrete producer, transport, consumer, and
     the required ordering in production code.
   - An enum, DTO field, interest type, or Cloud consumer that still exists does not prove the feature is connected.
     Verify that a reachable production entry actually creates and carries the fact. Migration review must explicitly
     search for both producer and consumer; a surviving consumer with no producer is a P1 regression.
   - Every repaired cross-boundary rule needs a focused connectivity/ordering contract that fails when an agent removes
     the producer, moves a visible preflight before its gate, drops transport metadata, or leaves the consumer
     unreachable. A business-method return-value test alone does not satisfy this requirement.
   - The work card must cite the controlling Markdown section and name the executable contract. A migration cannot be
     marked reviewed merely because the new code and the document look individually reasonable.
   - The historical G016 standing exception is suspended by the current no-test override above. Do not create or run
     connectivity/ordering contracts unless the user explicitly re-authorizes that specific test scope.

3. Do not delete useful comments unless necessary.
   - Previous work accidentally removed some of the user's comments. Avoid repeating this.

4. Prefer small, targeted commits.
   - The user tests frequently and wants to understand each change.

5. When the user says a point is already tested, treat it as known unless logs contradict it.

6. Use Chinese in conversation with the user unless they switch language.

6A. Image/template communication rule.
   - When explaining, proposing, reviewing, or asking the user to decide about an image, screenshot,
     template, ROI crop, or image file, do not refer only to a filename such as `foo.png`.
   - In the same user-facing message, display the actual local image inline with an absolute filesystem
     path, label what it visually represents, and state whether it is the live incident image, a
     historical/example image, or a template. The user must be able to inspect the pixels before being
     asked to judge a match, ROI, click point, or deletion.
   - If the exact historical image has been overwritten or is unavailable, say so plainly. Do not show a
     later screenshot as if it were the incident image. Still display every remaining relevant template
     and clearly label any illustrative/historical image.
   - Before saying an image/template should be deleted, retained, or replaced, show that image and every
     directly competing image in the same reply, with their roles in the current call path.

7. No-local-test mode is active by default.
   - As of 2026-07-10, do not create, restore, run, or cite local automated tests, source guards, replay tests, testcase images, or generated marked testcase outputs unless the user explicitly asks for that specific test or test family.
   - Do not ask the user to run fresh runtime because a local test passed. Default validation remains code review, log/screenshot inspection, and user-run runtime evidence.
   - If a task normally would require a testcase replay, record the runtime screenshot/log evidence that should be reviewed instead.
   - **Explicit-test exception:** when the user explicitly requests a named test, image/replay test, integration test, source guard, or a retained test suite, create/use only that requested scope and keep it in the repository unless the user later asks to remove it. Before starting the affected application/server or handing the build to the user, the responsible agent must run that explicitly requested test successfully against the current code. It may not bypass that required test with `-DskipTests`, an enforcer skip, a stale jar, or an IDE-only build.
   - Existing misleading cloud/NPC/dialog/brain tests remain removed by default; do not reintroduce them unless the user explicitly requests them.
   - **G016 is currently suspended:** focused business-rule connectivity/ordering contracts must not be created or run
     without new, explicit user authorization for the specific test scope.

8. Java compile gate is mandatory.
   - This is separate from local tests. No-local-test mode does not allow handing off uncompiled Java.
   - After any change to Java source, Maven model/config, generated classes, Lombok model/request/result classes, or cloud/client integration code, the agent who made the change must run the relevant compile/package command and confirm it succeeds before telling the user the build is ready to run.
   - For DHXY main-project Java changes, run `mvn -q -DskipTests compile` from the repository root unless the task explicitly requires a stronger package command. If an explicit-test exception under rule 7 applies, also run the requested test command without test-skipping before startup/handoff; compile success alone is not sufficient.
   - For `dhxy-cloud-brain` Java changes, compile/package that project as required by its startup path before asking the user to run a fresh runtime.
   - If both DHXY and cloud-brain are touched, both sides must be compiled successfully.
   - If compilation fails, do not ask the user to test. Report the compile failure as the current blocker and fix it first.
   - Do not rely on source files, IDE incremental state, stale jars, or previous runs as evidence. The handoff must be based on updated runtime classes/jars being generated successfully.

9. Investigation-first rule for user questions.
   - When the user asks why something happened, asks whether a behavior is correct, asks where/how to change something, or asks for a discussion/plan, do not immediately edit code.
   - First inspect the relevant logs, screenshots, call path, state transitions, and existing implementation. Then explain the likely root cause and a concrete modification plan.
   - Only start code changes after the user clearly approves the proposed plan, for example by saying "可以", "按这个改", "继续做", or an equivalent explicit approval.
   - This rule does not block tiny documentation-only updates requested directly by the user, but it does apply to behavior, navigation, OCR/template matching, runner/watcher, task flow, and input changes.

10. CR log-audit rule.
   - Before auditing runtime logs or writing a run report, read the current sprint/CR board and identify every CR whose status is not clearly Done/Deprecated/Closed.
   - The audit must check each open/review CR against the relevant log evidence, not just the newest visible symptom.
   - If an open/review CR fails in the logs, record the evidence in the CR/report and immediately dispatch a sub-agent to fix that failing CR unless the user explicitly says not to modify code.
   - Do not leave a failed CR as only a chat summary. The repair owner, expected files, and fresh-runtime verification point must be recorded in Markdown.

11. CR dashboard sync rule.
   - When an agent claims, creates, updates, reopens, closes, or changes the status/owner/summary of any CR in `docs/PACKAGE_ARCHITECTURE.md`, it must refresh the static dashboard data before handing off.
   - Before running the dashboard generator, make the CR table row user-facing fields readable in Chinese. The dashboard reads the CR table directly, so `Status`, touched-area text, and especially the summary/description must be a concise Chinese translation/summary of the card, not raw English planning text. Keep technical identifiers, enum names, file names, and log keywords in backticks when useful.
   - Run `node scripts/generate-cr-dashboard-data.js` from the repository root after the Markdown CR table/card update.
   - Include the resulting `docs/cr-dashboard-data.js` change together with the Markdown change, so `docs/CR_DASHBOARD.html` reflects the latest CR status after a browser refresh.
   - If the script cannot run, record that as a blocker in `docs/ACTIVE_WORK.md` and tell the user that the dashboard snapshot is stale.

12. CR card persistence rule.
   - Any sprint/CR task discussion that produces a decision, review opinion, blocker, repair direction, verification result, fresh-runtime acceptance point, or "do not run yet" warning must be written back into the corresponding CR card in `docs/PACKAGE_ARCHITECTURE.md` before handoff.
   - Do not leave CR review conclusions only in chat, a sub-agent reply, terminal output, or `docs/ACTIVE_WORK.md`. `docs/ACTIVE_WORK.md` may summarize the active pass, but the CR card is the durable source of truth for that task.
   - If a review finds a P0/P1/P2 issue, record the severity, evidence, affected files/methods, required repair direction, and verification/fresh-runtime gate inside the CR card.
   - If the CR table row status/owner/summary changes as a result, follow the CR dashboard sync rule and regenerate `docs/cr-dashboard-data.js`.

13. 谢帅 manager/reviewer operating rule.
   - This rule applies only when the current Codex agent is explicitly acting as `谢帅` / business-supervisor for a CR or cloud task.
   - In that mode, 谢帅 must not personally write Java business implementation code. 谢帅 owns scope, cards, acceptance criteria, delegation, review, and final judgment.
   - 谢帅 creates one or more worker sub-agents to implement the business/code changes. The number of worker agents and task split are 谢帅's responsibility.
   - Every worker-delivered CR/code change must receive at least two independent reviewer approvals before it can be considered complete. In 谢帅-managed CR/cloud work, those two approvals must come from two separate review/helper sub-agents unless the user explicitly approves a different review gate. 谢帅's own review is the final business-supervisor judgment and does not replace the two independent reviewer approvals.
   - If either reviewer finds a P0/P1/P2 blocker, the worker's task is not complete. Record the blocker in the CR card, send the worker back for repair, and rerun the two-reviewer approval gate after the fix.
   - 谢帅 should create separate review/helper sub-agents for code review or risk review whenever implementation is non-trivial; do not rely on a single reviewer for CR completion.
   - Worker agents must receive clear file/module ownership, business constraints, no-revert instructions, required tests, and documentation/update expectations.
   - 谢帅 may still edit Markdown/process documentation, CR cards, plans, and review notes directly, and may run/read tests/logs for verification.
   - 谢帅's final handoff must summarize worker output, independent review findings, verification status, and any remaining fresh-runtime gates.

14. User process trigger rule.
   - When the user explicitly says “走流程” / “按流程走” / an equivalent process-trigger phrase, use the full CR workflow:
     create or update the CR card, dispatch a worker, obtain two independent reviewer approvals, then give the manager/reviewer judgment.
   - When the user says “你去做吧” / “去做一下” / “继续做” without saying “走流程”, the current Codex agent should do the work directly; do not dispatch worker sub-agents by default.
   - Even for direct-agent work, create/update the relevant CR card unless the user explicitly says this does not need a card.
   - Do not add source-guard tests by default. Add a source guard only when the user explicitly says to guard against a behavior returning, forbids a specific call/path, or asks for a source guard.
   - Small deletions, small condition changes, and small log/frequency changes do not need new tests by default. Visual/OCR/click-coordinate changes still need screenshot replay and marked output verification.

15. “完成 CR编号”持续审查 heartbeat 规则。
   - 当用户明确说“完成 CR230”“完成 CR-230”或同等表述时，视为要求该 CR 走到可交付结论，不能只完成首版代码后停止。
   - 负责该 CR 的 agent 必须先完成卡片批准范围内的实现、必要编译门禁和 CR 卡更新；随后创建一个仅监控该 CR 的 heartbeat / schedule，默认每 5 分钟检查一次。当前仓库没有可靠的“Markdown 卡片被编辑即通知另一个 agent”的事件订阅机制，因此不得假设会收到即时通知；以该 heartbeat 的定时读卡为准。
   - **默认角色边界：**“完成 CR编号”只授权当前 agent 做实现、编译、写卡和等待外部 review 反馈；它不得自行把自己当 reviewer，也不得自行创建 reviewer/sub-agent 来给自己审批。它只能持续读取卡内由外部 reviewer 写入的反馈，并据此返修。
   - 只有用户明确追加“开 reviewer / 开 worker 做 review / 走流程 / 双 reviewer”，或当前 agent 被用户明确指定为 `谢帅` 业务主管并适用第 13 条时，当前 agent 才可以派 reviewer。即使获准派 reviewer，实现者本人也不能计入 reviewer approval。
   - heartbeat 每轮必须读取对应 CR 卡的最新正文、review 结论、状态和 blocker；不能只看聊天历史或旧日志。
   - 如果卡内出现新的 reviewer 意见、P0/P1/P2、要求返修、状态回退，或明确的未通过结论，agent 必须：
     1. 记录新意见与影响；
     2. 按既有 CR 流程修复代码/文档；
     3. 重新完成适用的编译门禁和双 reviewer gate；
     4. 将新的审查结论写回同一张 CR 卡；
     5. 继续保留 heartbeat，等待下一轮卡片结论。
   - heartbeat 只能在该 CR 卡内出现明确的“通过 / Approved / Done”最终结论，且没有未解决 P0/P1/P2 或返修要求时自动停止并删除自身。**fresh runtime 是 CR 的独立运行验收记录，不是该 heartbeat 的存续或关闭条件。**不能因为代码首次提交、一次 compile 成功、用户暂时没有回复、或 agent 自己认为“差不多”而停止。
   - 若用户明确要求暂停、停止该 heartbeat，或把 CR 改为 Deprecated/Closed，则按用户/卡片结论停止并删除；停止原因必须写回 CR 卡。
   - **Reviewer 写卡责任：**承担 CR review 的 agent 在审查结束时必须把结论写回对应 CR 卡，且结论必须明确二选一：
     - 有 P0/P1/P2、源码/编译证据不足或要求返修：写明“不通过 / Blocked / Review required”、严重级别、证据、修复方向和下一次验收点；
     - 确认没有 P0/P1/P2、无待返修项：写明“通过 / Approved”，并注明审查范围、依据和时间。**fresh runtime 是否待验不影响此处 reviewer 结论，也不阻止 heartbeat 关闭。**
     只在聊天回复“看起来没问题”、只改表格状态、或不写明确通过字样，均不构成关闭 heartbeat 的信号。负责实现的 agent 只能依据卡内这条明确结论停止监控。

16. “review CR编号”持续跟进 heartbeat 规则。
   - 当用户明确说“review CR230”“审核 CR-230”或同等表述时，被指派的 reviewer 不能只做一次静态 review 后退出；必须为该 CR 创建自己的 reviewer heartbeat，默认每 5 分钟读一次对应 CR 卡。
   - 每轮 reviewer 必须检查卡片是否出现 worker 新提交、返修说明、新 diff/编译证据、其它 reviewer 意见或状态变更；一旦有新材料，就按当前材料重新 review，而不是沿用旧结论。fresh runtime 记录可作为补充信息，但不是 reviewer heartbeat 的关闭前提。
   - reviewer 发现 P0/P1/P2、缺证据或返修要求时，必须立即把“不通过 / Blocked / Review required”、证据、影响、修复方向和复验点写回 CR 卡；reviewer heartbeat 继续保留，等待返修后再次审核。
   - reviewer 只有在自己完成最新一轮复审，并已在 CR 卡明确写入“通过 / Approved”、审查范围、依据和时间后，才可以停止并删除自己的 reviewer heartbeat。若该 CR 的总流程仍需要另一名独立 reviewer，则本 reviewer 的通过不代表另一名 reviewer 可以停止或代表卡已完成。
   - 同一 CR 的实施 agent heartbeat 与 reviewer heartbeat 是两个独立但互相衔接的责任：实施 agent 看到所有 required reviewer 已在卡内写入 `通过 / Approved`、且没有待返修项后即可关闭；reviewer 在自己最新一轮复审确认无问题并写入 `通过 / Approved` 后即可关闭。两者都**不等待 fresh runtime**；fresh 是独立运行验收记录。用户明确暂停/停止 review，或卡被 Deprecated/Closed 时，reviewer 才可按原因写卡并停止。

17. 五环业务基线锁定规则。
   - 修改任何五环代码、观察链、phase、Runner/Cloud 调度、Tracker/title/dialog、导航、补给或恢复逻辑前，必须逐段对照受保护基线 `D:\mavenProject\DHXY` 的 commit `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`，并同时核对 CR188、CR229 与 `docs/superpowers/plans/reports/2026-07-20-turn-card-TURN-40E.md` 中冻结的五环语义。
   - 编辑前必须把相关基线方法、当前方法和精确 diff 证据写入 `docs/ACTIVE_WORK.md`。不得只看当前 dirty 代码、当前卡片摘要或迁移后的调用形状来推断业务。
   - 五环热启动的固定业务门是 title-first：title/Tracker 明确存在时接管现有任务；prepared action 尚未生成、action 过期、观察尚未返回或 binding 尚未就绪，只能等待 Runner refresh，绝不能解释成“没有任务”；只有 exact fresh negative 明确证明没有五环 title/Tracker，才能进入接任务。
   - 云端迁移只能替换 capture、transport、park/wakeup、ownership 和输入承载方式，不得改变 phase 顺序、title/dialog/Tracker 判定、fallback 顺序或何时视为已接任务。任何与上述基线不同的业务变化，必须先在对应 G 卡写清旧行为、新行为、证据和影响，并获得用户明确批准；未批准不得实现。
   - 发现当前代码与基线不一致时，默认按回归处理：停止扩大改动，保留所有 unrelated dirty，做最小恢复并补针对该基线不变量的合同测试。不得用 magic timeout、无结果 fallback 或迁移便利性覆盖基线事实。

## Code documentation rule

- Temporary lightweight policy: keep comments useful, but do not turn every small code change into a documentation pass.
- New or modified public APIs only need JavaDoc when the behavior is non-obvious, externally reused, or safety-sensitive. Simple UI handlers, trivial getters/setters, and self-explanatory private helpers do not need forced JavaDoc.
- Main/high-frequency methods must have top-level JavaDoc that explains the method inputs and output. For every parameter, state what it represents and include coordinate space/unit/nullability when relevant. This is mandatory for navigation, OCR, input, window binding, task execution, and UI command entry methods.
- Must document logic that is risky or hard to infer:
  - physical input, focus, HWND/window binding, screenshot provider choice;
  - OCR/template matching fallback order;
  - task stop/pause handling, task transaction/yield decisions, retry policy;
  - persisted data formats, config switches, and debug-only behavior;
  - coordinate-space conversions, especially screen-absolute vs window-relative values.
- For complex multi-stage methods, add short block comments at the important decision points only. The goal is to make the flow reviewable, not to write a full SOP for every branch.
- Comments should explain why the logic exists, what invariant or safety rule matters, and what should not be changed casually. Avoid comments that merely repeat the code.
- When touching undocumented code, add concise comments only for the touched risky section. Do not broaden the edit just to document unrelated old code.
- Do not remove existing useful comments unless the code they describe is removed or the comment is being replaced with a more accurate one.
- Java file layout rule: keep public classes, public APIs, and the main workflow near the top. Private nested helper types (`private class`, `private record`, `private enum`, private interfaces) should be placed at the bottom of the enclosing class/file, after the main public and private workflow methods, unless Java syntax makes that impossible.
- Before adding a new method, class, helper, wrapper, or nested layer, first inspect the existing workflow and decide whether the change can be made by modifying the current method in place. Prefer adjusting the existing flow, return value, or branch structure when that preserves readability and ownership. Add a new function only when it creates a real reusable boundary, removes meaningful duplication, or isolates a distinct policy. Avoid stacked `internal`/wrapper/helper methods and nested call layers whose only purpose is to route to almost the same logic.
- Do not add trivial wrapper layers just to expose a second name for the same operation. If an existing method can naturally return useful data while preserving its side effect, prefer changing that method's return type and letting callers ignore the return value. Add a wrapper only when it enforces a real boundary, policy, or compatibility requirement.
- No "wrapper nesting" anywhere in the codebase. Do not create a method whose main job is to call another same-scope helper which then calls another helper for the same decision. A caller should either show the actual decision inline, or call one clearly named collaborator that owns a real boundary. Do not hide behavior behind chains of one-line helpers, adapter helpers, or "prepare/handle/resolve" nesting when the caller still has to understand all layers to review the flow. This applies to all Java code, not only 五倍.
- Task stop/pause checkpoint rule: use `TaskCheckpoint` directly for task stop/interruption checkpoints. Do not add local wrappers such as `checkpoint(...)`, `checkpointTask(...)`, `throwIfStopRequested(...)`, or ad-hoc `Thread.currentThread().isInterrupted()` exception checks in task/service code. A local helper is allowed only when it adds real domain behavior beyond the standard checkpoint. Boolean loop guards in worker/debug/control loops may still check interruption directly.

## Java / Spring / Lombok / logging conventions

- Use the existing Spring Boot style instead of manual object wiring. Services should normally be Spring beans (`@Service`, `@Component`, `@Configuration`, etc.) and dependencies should be injected through constructors, preferably with Lombok `@RequiredArgsConstructor` when it matches nearby code.
- Do not create ad-hoc singleton/service holders or manually `new` service dependencies inside business code. If a dependency is a real collaborator, inject it.
- Model/request/result classes should live in an appropriate model package, not inside service implementation packages unless they are truly private implementation details.
- Request/result/value objects should follow the existing Lombok style: use `@Value` + `@Builder` for immutable data objects, and use `@Builder.Default` for defaults. Static convenience factories are allowed, but they should build through `builder()` rather than bypassing the builder path.
- Use enums for stable operation/status/policy values instead of hard-coded strings when the value crosses service/task boundaries.
- Use SLF4J logging (`private static final Logger log = LoggerFactory.getLogger(...)`, or the project's existing Lombok logging style if a class already uses it). Do not use `System.out.println` in normal application code; reserve it only for throwaway local debug tools when there is no logger context.
- Logs should include enough structured context to debug multi-window behavior: window id/title when available, source task, target map/NPC/coordinate, result status, elapsed time for high-latency paths, and whether input/screenshot used HWND or focused real input when relevant.

## Safety and scope

This repository controls the user's local desktop/game client. Be cautious with anything that sends input.

- Do not make code that clicks automatically at startup without an explicit debug switch or UI action.
- Debug clicks should be behind configuration or invoked from an explicit task path.
- When adding debug modes, make them easy to turn off.

## Architecture principles that are now settled

### 1. Window binding is the source of truth

For multi-window mode, code must prefer the current `WindowTaskRunner` / `WindowRuntimeContext` binding instead of title-searching the first matching game window.

Important classes:

- `WindowRuntimeContext`
- `WindowTaskContextHolder`
- `WindowRegistrationRequest`
- `WindowNativeBinding`
- `WindowTaskRunner`
- `GameClientTracker`
- `ClientIdentityService`

Where possible, read the current window binding from `WindowTaskContextHolder.rawCurrent()` or `current()` rather than doing a global title search.

### 2. Identity sync should read the bound window title first

`PlayerStateService.syncMyIdentity()` calls `ClientIdentityService.scanAndSyncIdentity(...)`.

`ClientIdentityService` must prefer:

1. `WindowTaskContextHolder.rawCurrent().getNativeBinding().getTitle()`
2. `tracker.getFullWindowTitle()`
3. `tracker.locateWindow()` fallback

Reason: in debug mode or early startup, `GameClientTracker.fullWindowTitle` may not have been refreshed yet, but the current `WindowRuntimeContext.nativeBinding.title` already has the correct title.

### 3. Input queue is for physical input serialization

The system now uses `InputActionQueue` / `InputActionWorker` to serialize keyboard and mouse actions. This was introduced to fix multi-window input crossing.

Important classes:

- `InputAction`
- `InputActionRequest`
- `InputActionQueue`
- `InputActionWorker`
- `InputActionDeadLetter`
- `InputSequences`
- `WindowAwareInputCoordinator`
- `WindowFocusService`

All normal task mouse/keyboard actions should go through `InputSequences.submitAndWait(...)` or `submitExclusiveAndWait(...)`, unless explicitly doing a temporary local debug action.

### 4. Move + click must be one atomic sequence

This is a critical rule confirmed by testing.

Bad pattern:

```java
inputSequences.submitAndWait("move", List.of(
        InputAction.moveMouse(x, y),
        InputAction.sleep(150)
));

inputSequences.submitAndWait("click", List.of(
        InputAction.clickLeft(x, y, 100)
));
```

Why bad: another window can insert focus/move/click between the move and click.

Good pattern:

```java
inputSequences.submitAndWait("moveClick", List.of(
        InputAction.moveMouse(x, y),
        InputAction.sleep(150),
        InputAction.clickLeft(x, y, 100),
        InputAction.sleep(800)
));
```

Use this for:

- NPC first shot clicking
- OCR text click after moving
- Menu item click
- Any workflow where mouse position preparation and click are logically inseparable

### 5. Do not nest input queue calls inside the input worker callback

Do not call `inputSequences.submitAndWait(...)` from inside a callback passed to `submitExclusiveAndWait(...)`, because the only `InputActionWorker` is already executing the callback and cannot consume the newly queued request. This causes a queue-in-queue deadlock.

Inside an exclusive callback, use direct `InputProvider` calls only if you are already inside the serialized input section.

This was specifically fixed in `NpcClickService` for `npcClick:ctrlProbe`.

### 6. Focus is best-effort

Windows `SetForegroundWindow` may return false even when physical input can still work. Do not treat foreground verification failure as a hard failure.

`WindowFocusService` should attempt:

- `ShowWindow`
- `BringWindowToTop`
- `SetForegroundWindow`

Then continue quickly. Avoid repeated heavy `GetForegroundWindow` validation and warning spam.

### 7. Stop must be honored inside loops

Task stop is not enough if inner loops ignore interruption. Long loops must check `Thread.currentThread().isInterrupted()` or the execution context stop token where available.

Known sensitive loops:

- `NpcClickService` dense Ctrl probe loop
- OCR/menu scan loops
- retry loops
- long sleeps

Use interruptible sleeps when possible.

### 8. Temp files must be scoped where possible

Multi-window cannot share the same temp screenshot path unless that path is known safe. Prefer `WindowScopedTempPath.resolve(...)` for debug and OCR intermediate images.

Important temp/log files:

- `logs/dhxy-console.log` — full normal console/business logs
- `logs/tracker-coordinate.log` — tracker coordinate/window diagnostics
- window-scoped temp images such as `debug_npc_firstshot_center_raw.png`, `debug_npc_firstshot_player_washed.png`, `npc_menu_scan.png`, `npc_menu_clean.png`

## Logging expectations

The user often shares screenshots/log snippets. The best diagnostic file is now:

- `logs/dhxy-console.log`

Tracker-only details are in:

- `logs/tracker-coordinate.log`

Do not rely only on tracker logs. Business flow logs are essential.

## Current known stable findings

The recent debug test confirmed:

- The old one-shot NPC first-click debug proved the formula can calculate a correct `FINAL_CLICK_POINT` for 墨意.
- The point lands on the NPC.
- Direct input click can click the NPC.
- Therefore the coordinate formula is probably okay.
- The formal flow problem was the input sequence being split into move and click; this has been changed in `NpcClickService` using a combined move+click sequence.

## Files to inspect first for common issues

For multi-window binding:

- `src/main/java/com/bot/dhxy/core/GameClientTracker.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowTaskContextHolder.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowRegistrationRequest.java`
- `src/main/java/com/bot/dhxy/window/execution/WindowTaskRunner.java`

For physical input:

- `src/main/java/com/bot/dhxy/input/action/InputActionQueue.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionWorker.java`
- `src/main/java/com/bot/dhxy/input/InputSequences.java`
- `src/main/java/com/bot/dhxy/input/InputProvider.java`
- `src/main/java/com/bot/dhxy/window/interaction/WindowFocusService.java`

For 五环 flow:

- `src/main/java/com/bot/dhxy/task/FiveRingTask.java`
- `src/main/java/com/bot/dhxy/service/QuestManagerService.java`
- `src/main/java/com/bot/dhxy/service/NpcClickService.java`
- `src/main/java/com/bot/dhxy/service/NavigationService.java`
- `src/main/java/com/bot/dhxy/service/BagService.java`
- `src/main/java/com/bot/dhxy/service/BattleRadarService.java`
- `src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- `src/main/java/com/bot/dhxy/service/ClientIdentityService.java`

## How to start a new Codex session

Recommended first message to Codex:

> 请先阅读 AGENTS.md 和 docs/DHXY_CONTEXT.md。我们继续 DHXY 项目，现在主要看五环多窗口稳定性、输入队列原子动作、NPC 点击逻辑。不要大改已验证逻辑，先根据日志定位。

