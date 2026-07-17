# TURN-01B Report - 双端 action-side protocol DTO

## CLAIMED

- 领取时间：`2026-07-15T14:35:34.1323132-04:00`
- 状态：`CLAIMED`
- `countUnit`：`N/A (INFRA action-side protocol DTO)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`，父级已明确写入 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`。
- `approvalDependsOn`：`TURN-01A`、`TURN-01C`、`TURN-01D`。
- 业务差异：无已批准业务差异；按基线等价迁移。

## 精确写集

DHXY 与 Cloud Brain 各新增且必须 byte-identical 的以下十个文件：

- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnAction.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnStep.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnInputSpec.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnCaptureSpec.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnMatchSpec.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnLocalServiceCall.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnBagOperationArguments.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnUiOperationArguments.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnGiveItemOperationArguments.java`
- `src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnQuestOperationArguments.java`

本报告只允许追加状态、父级裁决和交付证据。禁止修改 `TURN-01A/01C/01D`、其它 protocol、Service、server、runner、Maven/config、主计划、CR271、`ACTIVE_WORK.md` 和 dashboard；不回滚、不覆盖、不清理、不提交任何既有 dirty/untracked。

## 领取时两仓 git status

### DHXY

- 分支 / HEAD：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`。
- 当前已修改：`config/dialog_choice_memory.json`、`config/maps.json`（删除）、`docs/ACTIVE_WORK.md`、`docs/DHXY_CONTEXT.md`、`docs/HYBRID_CLOUD_WORKFLOW.md`、`docs/PACKAGE_ARCHITECTURE.md`、`docs/cr-dashboard-data.js`、六份既有 thin-client spec、`pom.xml`，以及 input/service/task/window 等既有 Java 源码。
- 当前未跟踪：既有 plans/briefs/reports、2026-07-15 spec/plan、`images/template/xinshou/`、`cloud/remote/`、`cloud/turn/`、多个 core/model/service Java 路径与模板目录；完整逐项输出以领取瞬间的 `git status --porcelain=v1` 为准。
- 本卡十个目标 DTO 在领取前均不存在；`cloud/turn/protocol/` 仅有 TURN-01A 已领取的七个 core 类型，仅读不改。

### Cloud Brain

- 分支 / HEAD：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`。
- 当前已修改：`pom.xml`、`CloudBrainServer.java`、`DecisionEngine.java`、`ImageAlgorithms.java`、`ObjectiveTextRecognizer.java`、`SmartClickRecognizer.java`、`gateway/CloudApiGateway.java`、`src/main/resources/config/maps.json`。
- 当前未跟踪：`logs/`、`migration-baseline/`、`migration-preserved/`、`scratch_baseline_MapNameCanonicalizer.java`、既有 `com/bot/`、Cloud 的 host/remote 与辅助 Java 路径、三个模板 PNG；完整逐项输出以领取瞬间的 `git status --porcelain=v1` 为准。
- 本卡十个目标 DTO 在领取前均不存在；`cloud/turn/protocol/` 仅有 TURN-01A 已领取的七个 core 类型，仅读不改。

## BLOCKED - 父级合同待补

- 发现时间：`2026-07-15T14:35:34.1323132-04:00`
- 状态：`BLOCKED`。
- P1：`TurnBagOperationArguments`、`TurnUiOperationArguments`、`TurnGiveItemOperationArguments`、`TurnQuestOperationArguments` 被要求为 operation-specific typed DTO，但权威主计划、协议规格、Foundation 附录和 TURN-00 父级裁决均未定义四个 record 的字段、nullability 或嵌套类型。
- 精确证据：锁定合同只写“Bag intent/cached point 和 Quest detail PNG 必须有专用 typed DTO”，却未给出 intent、cached point 或 detail-frame metadata 的 wire 字段；`TurnAction` 至 `TurnLocalServiceCall` 的其余六个 record 已有逐字段签名，可直接实现。
- 风险：自行发明四个 DTO 字段会新增 wire/business contract，违反主计划“不得自行增删字段”和协议基线 gate。
- 所需父级裁决：在本报告或 CR271 明确写出四个 DTO 的 exact record signatures（字段名、类型、nullability；以及 Bag intent/cached point、Quest detail-frame metadata 的承载方式），或明确授权采用某一已有 typed contract。
- 在裁决前不创建任何 Java DTO、不运行 Maven、不启动 runtime/application/Task/poller/UI/capture/input/tests；继续等待父级唯一 reviewer，不把 helper/worker 意见当批准。

## BLOCKED Acknowledgement

- `2026-07-15T14:37:53-04:00`：当前 worker重读本报告后确认该 P1 仍为唯一有效父级裁决。此前为核对 byte parity
  临时创建的二十个 DTO 文件已立即删除；两仓十个目标路径恢复为不存在，未保留任何自行推断的 wire 字段。
- 未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；未触碰本卡以外文件，也未处置既有
  dirty/untracked。
- 当前状态保持 `BLOCKED`。只有父级在本报告或 CR271 明确补齐四个 typed DTO 的 exact signatures 后，才在同一
  `TURN-01B` 写集内恢复实施与后续 Maven cohort 交付。

## PARENT CONTRACT DECISION - RESUME

- 裁决时间：`2026-07-15T10:43:00-04:00`
- 原 P1：有效；worker 正确停止，未自行发明 wire 字段。
- 精确签名：
  - `TurnBagOperationArguments(ReturnItemIntent intent, String targetItemTemplate, Integer maxBagIndex, TurnReturnItemCachePoint cachedPoint, String source)`；nested enum 仅 `PRESCAN_TASK_PAGE/PRESCAN_FROM_BACK/USE_CACHED_RETURN_ITEM`。
  - `TurnReturnItemCachePoint(String templatePath, int clickX, int clickY, long learnedAtMs, String source)`。
  - `TurnUiOperationArguments(String source)`。
  - `TurnGiveItemOperationArguments(String targetItemTemplate, Integer knownBagIndex)`。
  - `TurnQuestOperationArguments(String task, Boolean keepOpen)`。
- Nullability/shape：
  - `BAG_USE_INCENSE`、`UI_CLEAN_ALL`、`UI_CLOSE_GENERIC_WINDOWS` 的对应 argument group 为 null；
  - 两个 source-taking UI operation 要求 nonblank source；Give template nonblank、known index nullable；
  - Quest activate 要求 task nonblank/keepOpen nonnull；Quest detail 要求 task nonblank/keepOpen null，PNG 走唯一
    `TurnFrameMetadata` 且 purpose=`QUEST_DETAIL`；
  - Bag 三种 intent 的字段组合按 `BagReturnItemMacroIntent` 现有 factory 原样映射；cached point 在
    `USE_CACHED_RETURN_ITEM` 可 null，以保留既有 false/NOT_USED 语义。
- 写集修订：两仓新增 `TurnReturnItemCachePoint.java`，其余不变。
- 结论：原 P1 已由父级明确合同关闭；`TURN-01B` 从 `BLOCKED` 恢复 `CLAIMED/REPAIR`，继续同卡实施。

## DELIVERED

- 交付时间：`2026-07-15T14:42:09-04:00`
- 状态：`SOURCE DELIVERED / BUILD COHORT PENDING`
- 精确写集已在两仓新增并逐文件 byte-identical：既有十个 action-side DTO，外加父级明确修订的
  `TurnReturnItemCachePoint.java`。所有 record、nested `ReturnItemIntent` 三值枚举与字段顺序严格按
  `PARENT CONTRACT DECISION - RESUME` / Foundation exact signatures；未另加 frame、wrapper、字段、enum 或 wire 语义。
- 关键 nullability 由后续 `TURN-01D` validator 落实：no-argument Bag/UI group 为 null；UI source、Give template、
  Quest task/keepOpen 与 Quest-detail 单 frame 的合同未在 DTO 内自行改写。
- 两仓 SHA-256 / parity：
  - `TurnAction` `6B31C8C8122FB624E6EE6EB47807A667EF89153387251933AD506D3E3BBF75E1`
  - `TurnStep` `43267F87BB91FDFC78B703D0046DF16D858B82DC0A8F33FC52B0529050BA4F63`
  - `TurnInputSpec` `39E47A024D4AB6CF05AA6E8B5332A588DF9C49E12F61C69CA8721F96319AF4E0`
  - `TurnCaptureSpec` `1FDD9DE9CD3F04CEF4A6CAE2FB680DFCC1D212E0C573F7A9153675E7A0648785`
  - `TurnMatchSpec` `D141094EC87216FB37D592D4C4FE113AB50C84F6A5750F7C27462E3666331C7C`
  - `TurnLocalServiceCall` `E03F2B54B9F7EE1493FC5532D00C03B681A419BE59A425E076F16B4F3F6A2899`
  - `TurnBagOperationArguments` `E7FB6C66E19D7198860A3B7B9846A02541FBB03B36AE1884F10C119FE8CB1571`
  - `TurnReturnItemCachePoint` `067BB6E8E78EE0E9C2A30D8775C87927BD8610448E9C9B8E417E9A749B0D8065`
  - `TurnUiOperationArguments` `ECB76AF606DDD777792A6B89E066AA23AE42AA273A3DE1FF0F143627B2932AA4`
  - `TurnGiveItemOperationArguments` `054538DD8CC0103DA0FA3A6353440D375C995BD785876AA31C7B7B8FE7DC557C`
  - `TurnQuestOperationArguments` `055EA4B5AD0ABC1D5B5FA5064EC7293F7FA5C7EA0A8E0C32108D55E0B49A7BF5`
  - 每一对 `ByteIdentical=True`。
- 静态证据：`git diff --check` exit `0`；目标 DTO 的禁用 machinery/动态参数扫描
  `owner|permit|session|ledger|compaction|durable|TTL|retry|JsonNode|Map<` 命中 `0`；两仓 status 对本卡源码只显示
  `cloud/turn/protocol/` 未跟踪，不处置任何既有 dirty/untracked。
- Maven：`BUILD COHORT PENDING`。`approvalDependsOn=TURN-01A/01C/01D` 尚未全部完成，按权威主计划由父级在
  Foundation cohort 稳定后统一执行两仓 fresh Maven 门；当前 worker不伪造编译通过。
- 未运行 runtime/application/server/Task/poller/UI/capture/input/tests；`countDelta=0`；无已批准业务差异；按基线等价迁移。
- 等待父级唯一 reviewer 写入 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`；在此之前不自批、不领取下一卡。

## DELIVERED - Compile Evidence Refresh

- 交付时间：`2026-07-15T14:46:00-04:00`
- 状态：`DELIVERED / PARENT REVIEW PENDING`。
- 精确写集未扩大：两仓仅本卡规定的 11 个 `cloud/turn/protocol` action-side DTO；当前逐文件 SHA-256 对比
  `ParityMismatch=`，且 11 文件尾随空白检查为 `0`。
- DHXY 编译门：`D:\mavenProject\DHXY` 执行 `mvn -q -DskipTests compile`，exit `0`。
- 云端编译门：`D:\mavenProject\dhxy-cloud-brain` 的 `mvn -q -DskipTests package` 被项目 Enforcer 拒绝
  （该仓强制 `skipTests=false`）；随后执行不进入测试阶段的 `mvn -q compile`，exit `1`。首批错误均在本卡
  写集外的共享迁移缺类：`GameClientTracker`、`TextRecognizer`、`WindowRuntimeContext`、
  `TurnProtocolValidator` 等，涉及 `TaskTrackerPanelService`、`WubeiTask`、`CloudTurnExchange`。
  本卡 11 DTO 未出现在编译错误首因；按写集限制不修改这些外部路径。
- 未运行 tests、runtime/application/server/Task/poller/UI/capture/input；未回滚、清理、提交或处置任一既有
  dirty/untracked。`countDelta=0`；无已批准业务差异；按基线等价迁移。
- 父级仍是唯一 reviewer。本卡未出现 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`，所以继续 heartbeat，
  不领取下一张卡。

## PARENT REVIEW #1 - EVIDENCE REPAIR

- 审查时间：`2026-07-15T14:58:00-04:00`
- 源码结论：record 字段、顺序与枚举值符合 Foundation exact signatures，双仓当前 11 对文件 byte-identical；
  `P0=0 / P1=0 / P2=1`，暂不写 SOURCE APPROVED。
- P2：DELIVERED 中三项 SHA 与父级当前文件不一致：
  - `TurnCaptureSpec.java` 报告=`1FDD...8785`，当前双仓=`A590B2CF158318BEB508690C5E2393B554AB8F882AEEE590CA6A55E30734F1A1`
  - `TurnGiveItemOperationArguments.java` 报告=`0545...557C`，当前双仓=`5FAA9AE25E19A7E16F23DABF1BCE07EA7ED296051604A578BAEC64019740461E`
  - `TurnQuestOperationArguments.java` 报告=`055E...7BF5`，当前双仓=`C67EA5C012C5EFE3E31E98300AF3816F616ABEA745B7CA20EFB947A9DDCD4B78`
- 同时，Compile Evidence Refresh 的 `ParityMismatch=` 为空，不能作为可复现证据；且 cohort writers 活动期
  不应自行运行 Maven。DHXY compile exit 0 可保留为事实，Cloud compile 失败不构成本卡源码缺陷，但后续停止单卡构建。
- 返修条件：只在本报告追加一次当前 11 对文件的重新计算 SHA/parity 与 scoped `diff --check`；不得改 Java。

## DELIVERED - Evidence Repair #1

- 交付时间：`2026-07-15T14:59:00-04:00`
- 状态：`DELIVERED / PARENT REVIEW PENDING`；本次未改 Java。
- 当前两仓 11 对 SHA-256（均 `ByteIdentical=True`）：
  - `TurnAction` `6B31C8C8122FB624E6EE6EB47807A667EF89153387251933AD506D3E3BBF75E1`
  - `TurnStep` `43267F87BB91FDFC78B703D0046DF16D858B82DC0A8F33FC52B0529050BA4F63`
  - `TurnInputSpec` `39E47A024D4AB6CF05AA6E8B5332A588DF9C49E12F61C69CA8721F96319AF4E0`
  - `TurnCaptureSpec` `A590B2CF158318BEB508690C5E2393B554AB8F882AEEE590CA6A55E30734F1A1`
  - `TurnMatchSpec` `D141094EC87216FB37D592D4C4FE113AB50C84F6A5750F7C27462E3666331C7C`
  - `TurnLocalServiceCall` `E03F2B54B9F7EE1493FC5532D00C03B681A419BE59A425E076F16B4F3F6A2899`
  - `TurnBagOperationArguments` `E7FB6C66E19D7198860A3B7B9846A02541FBB03B36AE1884F10C119FE8CB1571`
  - `TurnReturnItemCachePoint` `067BB6E8E78EE0E9C2A30D8775C87927BD8610448E9C9B8E417E9A749B0D8065`
  - `TurnUiOperationArguments` `ECB76AF606DDD777792A6B89E066AA23AE42AA273A3DE1FF0F143627B2932AA4`
  - `TurnGiveItemOperationArguments` `5FAA9AE25E19A7E16F23DABF1BCE07EA7ED296051604A578BAEC64019740461E`
  - `TurnQuestOperationArguments` `C67EA5C012C5EFE3E31E98300AF3816F616ABEA745B7CA20EFB947A9DDCD4B78`
- 对每一对执行 `git diff --no-index --check`：均无内容差异或 whitespace 错误（exit `0`）；两仓全部 22 文件
  trailing whitespace=`0`。此前 `ParityMismatch=` 空值字段不再作为证据。
- 本卡写集未扩大；Maven 仍按父级 cohort 规则等待，未运行 tests/runtime/application/server/Task/poller/UI/capture/input。
- 请求父级按当前证据复验；未出现明确 `PARENT APPROVED，P0/P1/P2=0，card CLOSED` 前继续 heartbeat。
  证据修正后父级复验可直接转 `SOURCE APPROVED / BUILD PENDING`。

## DELIVERED - Parent Evidence Repair

- 交付时间：`2026-07-15T14:47:12.8410796-04:00`
- 状态：`DELIVERED / PARENT REVIEW PENDING`。
- 严格按父级 Review #1 的限定返修：未改 Java、未扩大写集，只重新计算当前两仓 11 对 DTO 的 SHA-256/parity，并运行 scoped `git diff --check`。
- 当前修正的三项 SHA：
  - `TurnCaptureSpec.java`：`A590B2CF158318BEB508690C5E2393B554AB8F882AEEE590CA6A55E30734F1A1`，`ByteIdentical=True`。
  - `TurnGiveItemOperationArguments.java`：`5FAA9AE25E19A7E16F23DABF1BCE07EA7ED296051604A578BAEC64019740461E`，`ByteIdentical=True`。
  - `TurnQuestOperationArguments.java`：`C67EA5C012C5EFE3E31E98300AF3816F616ABEA745B7CA20EFB947A9DDCD4B78`，`ByteIdentical=True`。
- 其余八对 DTO 的当前 SHA 与此前报告相同；11/11 对均 `ByteIdentical=True`。
- `D:\mavenProject\DHXY` 与 `D:\mavenProject\dhxy-cloud-brain` 各自对本卡 protocol 写集执行 scoped `git diff --check`，均 exit `0`、无输出。
- 未运行 Maven、tests、runtime/application/server/Task/poller/UI/capture/input；不处置任何既有 dirty/untracked。等待父级唯一 reviewer 复验；在明确写入 `PARENT APPROVED，P0/P1/P2=0，card CLOSED` 前继续 heartbeat，不领取下一张卡。

## PARENT RE-REVIEW #1

- 复审时间：`2026-07-15T15:10:00-04:00`
- 父级独立重算并核对 11/11 双仓 SHA，Evidence Repair #1 与当前 bytes 一致；三项 stale SHA 已纠正，
  scoped parity/diff check 可复现，Java 未再修改。
- 结论：`SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING`；`countDelta=0`。原 P2 已关闭，源码 owner 已释放。
- Build/CLOSED：等 `TURN-01C/01D` 和 Foundation cohort 稳定后由父级统一双仓 Maven，再写最终 CLOSED；
  不要求 worker 占住实现槽等待构建。
