# G144 图像判断全链路取证缺口审计

日期：2026-09-02  
状态：审计完成，生产代码未修改，待用户批准修复方案。

## 用户要求

凡是程序读取图片并据此作出匹配、状态判断、任务分流或点击决定，该次判断使用的像素都必须可追溯。不能再用“日志里写了未命中”代替图片，也不能用下一拍、另一个窗口或已被覆盖的 `latest` 图片代替当时那一拍。

## 审计范围

- Client：`D:\mavenProject\DHXY-cr271\src\main\java` 全部生产源码。
- Cloud：`D:\mavenProject\dhxy-cloud-brain\src\main\java` 的远程图片接收、OCR、模板匹配和判定证据边界。
- 不把测试、手工 replay 工具和模板资产本身算作运行时取证。

Client 静态扫描基数：

- `ImageFinder.find/findAll/isMatch/bestMatchScore`：46 个调用，分布在 25 个生产文件。
- `captureToMemory/captureToFile/captureRegion/captureWindow` 等截图调用：48 个。
- 显式 `MatchEvidenceStore` 调用：30 个；其中 23 个直接传 `windowId=null`。

这些数字不能直接相减，因为还有颜色、结构、字模和 OCR 判定；但足以证明“截图入口存在”不等于“每次判断有证据”。

## 一、系统级硬漏洞

### P0-1 成员窗口被总截图证据池明确排除

`WindowCaptureEvidenceStore.persist(...)` 遇到 `WindowRole.MEMBER` 直接返回。多开时成员窗口恰恰是最容易出现“只有这个号不动”的对象；它们的普通截图不会进入 `images/captures`。这不是偶发漏图，是代码规定不保存。

### P0-2 总截图池只是五分钟抽样，不是判定证据

同一窗口 `PER_WINDOW_MIN_INTERVAL_MS=300000`，一次 burst 最多两张。五分钟内可能发生数百次视觉判断，但通常只留下某次无关截图。该图片不能证明具体判断。

### P0-3 两个异步写盘池都允许静默丢证据

- `WindowCaptureEvidenceStore` pending 达 64 时丢弃；而限流槽已先消耗，可能随后五分钟都不再保存。
- `MatchEvidenceStore` 队列 64，满时丢弃。`saveOnChange` 又在入队前更新状态；若关键翻转帧被丢，后续相同状态不会补存。
- `MatchEvidenceStore` 使用 daemon writer，没有“每个业务决定已持久化”的确认边界；进程退出时尾部任务可丢。

### P0-4 高频 `saveOnChange` 默认只存状态翻转，不存每次判断

默认关闭周期 latest 刷新；同一 PRESENT/ABSENT 状态持续期间的所有后续判断帧都不落盘。日志里某一拍出问题时，目录中的图片可能是很早以前的首次翻转帧。

### P0-5 远程判定帧没有统一的本地 durable archive

`ExactWindowPreparedFrameCapture`、`TurnPngCodec`、终点帧和若干 observation ROI 只编码进内存/HTTP。它们可能被 Cloud 判断，但 Client 没有按 `demandId/generation/observerSeq/actionId` 永久保存 exact PNG。Cloud 也只有若干业务专用 dump，没有覆盖所有 OCR/模板/颜色判断的强制入口。

### P1-1 证据身份不完整，甚至会跨窗口覆盖

23 个显式匹配证据调用传 `windowId=null`。`MatchEvidenceStore` 只能从观察线程名提取 hwnd；输入 worker、turn/local-service 等线程会落到 `unknown`。多个窗口同一 site 会共享状态键和 `latest_unknown.png`，互相覆盖。

现有文件名通常还缺：`taskRunId`、任务名/阶段、HWND、capture provider、绝对/窗口相对 ROI、模板路径及 hash、threshold、observerSeq、actionId、最终点击点。因此“找到一张图”仍不能证明它对应哪条日志和哪次动作。

### P1-2 多种证据目录会覆盖或停止保存

- `latest_vision.png`、`roi_scan.png`、`bag_scan.png`、team-role latest、team-return latest、tracker absent 等固定文件名会覆盖。
- `BagScanMissDump`、`DialogMatchMissDump`、`WuhuanTitleMatchMissDump` 各只写前 20 张；达到上限后当前 JVM 余生永久静默。
- 召回观察、五环完成故事等使用 40/60 张滚动池，会覆盖旧事故。
- `images/captures` 只保留今天和前两天。

### P1-3 有些“证据图”不是实际参与判定的图

`UICleanerService.isDarkCenterOverlayPresent()` 用新抓的中央 ROI 判定是否允许中心点击，随后保存的却是 `latest_vision.png`。该文件可能是旧帧，也不是那块 ROI；所以即便文件存在，也不能证明暗色门为何放行。

### P1-4 只存处理后图，原始像素链断裂

绿色/白色文字洗图、二值化、mask、glyph 分割等路径常把处理图交给匹配证据，却没有同时绑定原图、处理参数和模板版本。出现“洗错了还是模板错了”时无法重放。

### P1-5 匹配框不等于最终点击证明

`MatchEvidenceStore` 最多画模板框；实际业务还会做 ROI→窗口→屏幕坐标换算、固定偏移和随机偏移。大量路径没有把最终 click point 画在同一张原图上，因此只能证明“匹配过”，不能证明“点到了哪里”。

## 二、已确认没有逐判断图片的生产路径

| 严重度 | 业务路径 | 当前判断 | 当前证据缺口 |
|---|---|---|---|
| P0 | 自动战斗面板 watcher | `WindowObservationSampler.watchAutoPanelDuringCombat` 在共享整窗上匹配，miss 会触发 Alt+8 | 触发修复的那张 exact frame/模板/分数没有保存；这正覆盖“某窗口一直不补 Auto+8/为什么没匹配到”的事故类型 |
| P0 | 寻路坐标数字判稳 | `LocalCoordinateStripReader.read` 逐拍清洗、分字、识别坐标，决定 ACTIVE/STABLE/STRIP_UNAVAILABLE | 没有保存 raw strip、clean strip、glyph boxes、识别值/分数；稳定进入/撤销/不可读都只能看文字日志 |
| P0 | 五环对话框存在性 | `WuhuanPresenceLocalMechanics` 用 `DialogFramePresenceMechanics` 的像素结构判定 | title 有边沿图，completion 有滚动图，但 `dialogPresent` 自身没有原图和检测框 |
| P1 | 通用对话框框体结构 | `DialogFramePresenceMechanics` 扫中性像素横/竖边 | 无通用证据接口；调用者若未另存，判断像素立即 flush |
| P1 | 左上角运行/关闭状态 | `LeftTopStatusLocalOperationExecutor` 比较 open/closed 模板 | 代码注释明确写着取证已移除，只留分数日志 |
| P1 | 本地移动像素判定 | `LocalMovementFactMechanics` 比较坐标条和左右边沿前后帧 | before/after 均不成对保存，命中后立即 flush |
| P1 | Turn 像素变化探针 | `TurnCaptureStepExecutor` 在 Ctrl/move 前后用 `ImageFinder.isMatch` 判 changed | protocol 可返回 after frame，但没有强制保存 before+after+threshold+结果的本地证据对 |
| P1 | 任务列表高亮 | `QuestManagerService.isTextGlowing` 统计亮色像素，直接决定要不要点击任务 | 80x20 ROI、亮点数量、阈值和结果不落判定图 |
| P1 | UI 中央暗遮挡门 | `UICleanerService.isDarkCenterOverlayPresent` 统计 dark fraction，放行高风险中心点击 | 实际 ROI 不存；保存的是可能陈旧的 `latest_vision.png`，且没有最终点击点标记 |
| P1 | 天庭飞行状态 | `FlyingSaturationLocalMechanics.classify` 按平均饱和度决定 FLYING/NOT_FLYING/UNKNOWN | 只发数值事实，不保存该 ROI 和阈值标注 |
| P1 | 队伍身份预检 | `LocalTeamRolePreflightService` 由解散/移交/成员标记判断 LEADER/MEMBER/SOLO | 只覆盖写 latest；marked 图没有画 `memberMatch`，而它正是 MEMBER/SOLO 分界；无逐次历史 |
| P1 | 包裹多目标扫描 | `BagService.searchItemsInTabOnly` 用 `findAll` 决定物品位置 | miss 只有前 20 张；hit 不生成带所有候选框和最终选中点的证据图 |
| P1 | Tracker anchor/panel | cached ROI 与 full-window anchor 决定拖拽及面板捕获 | cached evidence 仅翻转存；full-window absent 固定名覆盖；最终送 Cloud 的 panel PNG没有按 action/demand 留本地历史 |
| P1 | 截图空白/降级判定 | `BoundWindowCaptureService.probeBlank` 决定接受 PrintWindow 还是 BitBlt/fallback | rejected provider 的图片不保证保存，且总池可能因成员/五分钟/满队列跳过，无法对比两个 provider |
| P2 | 任务栏/对话框/战斗/维护等 `saveOnChange` 路径 | 已有状态边沿证据 | 不满足“每一次判断”；缺 exact run/action/ROI/template identity，队列满仍会丢 |

## 三、已有但只能算部分覆盖的路径

- 通用 Turn 模板匹配、DialogService、部分 Bag 当前页、战斗信号、新手阶段、天庭对话选项、未知阶段 title、五环 title、召回信号等已经调用 `MatchEvidenceStore`。
- Quest detail 会保存 latest 加时间戳历史；五环 completion 每次判断保存 60 槽滚动图；Team Return panel 有 raw/marked latest；部分 NPC 结果在协议中携带 raw/mask/hash。
- 这些机制能帮助定位部分问题，但受窗口身份、覆盖、限流、滚动、异步丢弃和缺少最终点击标记影响，不能宣称“全链路有证据”。

## 四、建议的统一修复合同

不要给上述每个业务类继续各写一个临时 dump。应建立一条统一、可强制检查的证据通道：

1. **内容寻址图片仓**：按 PNG SHA-256 保存原图/ROI/处理图/运行时可变模板；同一图片只存一份，避免五窗每拍重复写大图。
2. **每次判断一条 append-only manifest**：即便像素相同，每个判断仍写一条轻量 JSONL，包含 decisionId、capturedAt、windowId、HWND、role、taskRunId、task/phase、source、observerSeq/actionId/demandId/generation、ROI 两套坐标、provider、原图 hash、处理图 hash、模板 key/hash、threshold、score、verdict、候选框和最终 click point。
3. **不可静默丢**：业务决定特别是会触发点击/按键/状态终局的证据必须在不可逆动作前完成 durable enqueue；队列满不能把证据当作可丢诊断。可采用 WAL/spool，不能只 WARN 后继续。
4. **原图 + 派生图成链**：清洗、mask、二值化、OCR/glyph 必须引用同一 raw hash 和算法版本；不能只留 washed 图。
5. **点击必须画终点**：匹配框、所有候选、选择理由、坐标变换和最终实际点击点写进 manifest，并能生成 marked PNG。
6. **Client/Cloud 共用 correlation id**：Client 保存发送的 exact PNG；Cloud 保存 OCR/模板输出并引用同一 hash/decisionId，确保“哪张图导致哪条 Cloud 决策”可闭合。
7. **不再固定 20 张后沉默**：旧 dump 改为统一通道；保留策略按 run/day/quota 管理，删除必须有可审计清单，不能无声覆盖事故。
8. **测试门**：静态合同扫描所有生产图像判断 API，新增调用没有 evidence envelope 就失败；行为合同覆盖多窗口、队列满、进程退出、同图多判定、processed/raw 配对及最终点击标记。

## 五、建议实施批次（尚未批准，未写生产代码）

- 批次 A：统一证据底座与 manifest/WAL，先解决成员排除、五分钟抽样、unknown 窗口、队列丢失和关联字段。
- 批次 B：接入会触发物理输入或终局判断的 P0/P1 路径：自动战斗面板、坐标判稳、对话框存在、中心点击、任务高亮、移动/像素探针。
- 批次 C：接入剩余模板/OCR/颜色判断，补 raw→processed→template→verdict→click 全链，并建立 Client/Cloud correlation。
- 批次 D：迁移/淘汰各类 latest、固定 20 张和零散滚动池；做长跑容量与性能验证。

任何批次开始前都必须先给出磁盘预算与保留策略。历史实测表明无去重地保存每一张整窗 PNG 可达到约 32GB/天；本卡建议用“图片按 hash 去重 + 每判定轻量 manifest”满足全量取证，而不是恢复旧的全量重复 PNG 风暴。
