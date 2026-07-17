# Cloud NpcClick Pure Helper - Internal Worker AV

## Parent Direct Implementation Task - `W-NPCCLICK-PURE-HELPERS-IMP1` - 2026-07-14T02:03:00-04:00

Internal AV 直接实现，不写 Design。业务权威为 DHXY committed `0114604e`。

### 唯一写集

- New `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
- 本报告仅 append-only

目标写前不存在。其他 Worker 正在并行修改别的文件；不得回滚、覆盖、清理或提交任何 dirty/untracked。

### 精确实现

从 committed `0114604e` 的 `NpcClickService` 机械复制约 `1162-1180` 的五个尚未接 caller 的 private static
纯 CPU helper：`safeDebugName(String)`、`safeValue(String)`、`clamp(int,int,int)`、`hasText(String)`、
`equalsText(String,String)`。保持 CJK/ASCII 文件名清洗正则、null/blank、trim、`unknown`、边界 clamp 与
null-safe equality 逐 token 等价。只加准确类 JavaDoc，明确 helper 尚未接 caller。

不得新增 public API、caller、wrapper、Spring、state/cache/clock/TTL/retry、capture/OCR/template/input/remote、
NPC 坐标/探测/验证逻辑或其它方法。运行 Cloud `mvn -q compile`（不 clean），在真实 EOF 追加 `CLAIMED`
与 `Implementation #1`，列 SHA-256、五方法 source/target diff=0、写集与构建结果。自审仅 QA，不构成
Approved；父级独立源码审查。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## CLAIMED

- Worker: Internal Worker AV
- Task: `W-NPCCLICK-PURE-HELPERS-IMP1`
- Claimed at: `2026-07-14T02:05:31-04:00`
- Status: CLAIMED
- Scope: 仅新建 Cloud `com.bot.dhxy.service.NpcClickService`，并仅向本报告真实 EOF 追加。
- Baseline: committed `0114604e`；无已批准业务差异，按基线等价迁移。

## Parent Scope Clarification #1 - 2026-07-14T02:10:00-04:00

为保持用户要求的 Service 类边界，外层声明必须是基线同样的 `public class NpcClickService`；“不得新增 public API”
是指五个 helper 仍严格为 private static，且不得新增其它 public 方法。其余任务合同不变。

## Internal Worker AV - `W-NPCCLICK-PURE-HELPERS-IMP1` Implementation #1 - 2026-07-14T02:07:10-04:00

### 交付与 SHA-256

- New `D:\\mavenProject\\dhxy-cloud-brain\\src\\main\\java\\com\\bot\\dhxy\\service\\NpcClickService.java`
- SHA-256: `36c3708fe06dfd48ad632717ab9f021af4ef6daa24fc5ed08160f8145e6c8d50`
- 唯一源码写集与 CLAIMED 一致；除此之外只向本报告真实 EOF 追加。

### 实现

- 新建 package-private `NpcClickService`，仅含准确类 JavaDoc 与五个尚未接 caller 的 `private static`
  纯 CPU helper：`safeDebugName`、`safeValue`、`clamp`、`hasText`、`equalsText`。
- 类无字段、显式构造器、Spring、state/cache/clock/TTL/retry、capture/OCR/template/input/remote、
  NPC 坐标/探测/验证逻辑、caller、wrapper、public API 或其它方法。

### 五方法 source/target diff=0

- 从 committed `0114604e` 的 `NpcClickService.java:1162-1180` 按方法边界抽取完整 19 行。
- 从目标 `NpcClickService.java:9-27` 按相同方法边界抽取完整 19 行。
- 内存逐行比较结果：`SOURCE_HELPER_LINES=19`、`TARGET_HELPER_LINES=19`、
  `SOURCE_TARGET_DIFF_COUNT=0`，即五方法签名、可见性、修饰符、方法体及顺序均 identical。
- CJK/ASCII 文件名清洗正则、null/blank、trim、`unknown`、边界 clamp 与 null-safe equality
  均逐 token 等价 committed `0114604e`。

### 构建

- Cloud `mvn -q compile`（未 clean）: **PASS，exit 0**。
- 未运行 tests；未启动 application/host/Task/UI/input。

### Worker QA

- 实际写集：1 个 New Cloud Java 文件 + 本报告 append-only；未回滚、覆盖、清理或提交任何他人
  dirty/untracked。
- 未执行任何 Git mutation。
- **无已批准业务差异；按 `0114604e` 基线等价迁移。**
- 本节仅为 Internal Worker AV 的实现与 QA，**不构成 reviewer approval / Approved**；等待父级独立源码审查。

## Parent Source Review #1 - BLOCKED - 2026-07-14T02:11:00-04:00

结论：`P0=0 / P1=0 / P2=1`。

- 已通过：五个 private static helper 的签名、顺序、CJK/ASCII 清洗正则、null/blank/trim/`unknown`、
  clamp 与 null-safe equality 均与 committed `0114604e` 逐 token 一致；没有越过写集或引入本地能力。
- **P2-1（类边界可见性漂移）：** 当前新文件 `NpcClickService.java:7` 声明为
  `class NpcClickService`，而 committed 基线为 `public class NpcClickService`；这与已在本报告 EOF 写明的
  Parent Scope Clarification #1 不符，并会阻断后续跨 package 原调用形状。

### 精确返修条件

原 AV 只把外层类声明改为 `public class NpcClickService`，并把类 JavaDoc明确为 committed same-path 首刀；
五个 helper 及所有 executable token 不得改变，不新增 caller/annotation/dependency。运行 Cloud `mvn -q compile`
（不 clean），在真实 EOF 追加 `Implementation Repair #1`、新 SHA 和五方法 unchanged 反证。父级随后复审。

**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Internal Worker AV - `W-NPCCLICK-PURE-HELPERS-IMP1` Implementation Repair #1 - 2026-07-14T02:10:39-04:00

### P2-1 精确返修

- 仅把外层声明改为 `public class NpcClickService`，恢复 committed same-path Service 类边界。
- 仅同步类 JavaDoc，明确本文件是 committed same-path first cut，当前只承载五个尚未接 caller
  的 private static 纯 CPU helper。
- 未新增 caller、annotation、dependency、字段、构造器、public 方法或其它 executable member。

### 新 SHA-256

- `D:\\mavenProject\\dhxy-cloud-brain\\src\\main\\java\\com\\bot\\dhxy\\service\\NpcClickService.java`
- Repair #1 SHA-256: `bcdbabf142fe392d5f4e5cb278f4cbb6c8c50a22d6853567d46417e7b20507e8`
- Implementation #1 SHA-256: `36c3708fe06dfd48ad632717ab9f021af4ef6daa24fc5ed08160f8145e6c8d50`
  （SHA 变化仅来自类 JavaDoc和外层 `public` 声明）。

### 五方法 unchanged 反证

- committed `0114604e` 的五方法完整块 `NpcClickService.java:1162-1180`：19 行。
- Repair #1 目标五方法完整块 `NpcClickService.java:11-29`：19 行。
- 内存逐行比较：`HELPER_SOURCE_LINES=19`、`HELPER_TARGET_LINES=19`、
  `HELPER_UNCHANGED_DIFF_COUNT=0`。
- 因此五个 helper 的签名、可见性、修饰符、顺序和全部 executable token 相对 committed
  `0114604e` 完全不变；Implementation #1 已有同一基线 `diff=0` 证据，Repair #1 仍为
  `diff=0`。

### 构建与边界

- Cloud `mvn -q compile`（未 clean）: **PASS，exit 0**。
- 实际返修写集仅为同一 New Cloud `NpcClickService.java` + 本报告真实 EOF append-only；
  未修改任何其它文件。
- 未运行 tests；未启动 application/host/Task/UI/input；未执行 Git mutation。
- **无已批准业务差异；按 `0114604e` 基线等价迁移。**
- 本 Repair #1 是 Internal Worker AV 实现与 QA，**不构成 reviewer approval / Approved**；
  等待父级复审。

## Parent Source Review #2 - APPROVED - 2026-07-14T02:16:00-04:00

父级复核 Repair #1 与当前真实源码，结论 `P0=0 / P1=0 / P2=0`：

- 外层声明已恢复 committed 同路径边界 `public class NpcClickService`，类 JavaDoc准确说明当前首刀范围。
- `safeDebugName`、`safeValue`、`clamp`、`hasText`、`equalsText` 五个 private static helper 对
  `0114604e` 仍逐 token `diff=0`，没有新增 caller、annotation、dependency 或其它方法。
- 父级复算 SHA-256 为
  `bcdbabf142fe392d5f4e5cb278f4cbb6c8c50a22d6853567d46417e7b20507e8`，与 AV Repair 报告一致；
  AV 的 Cloud `mvn -q compile` exit 0。

本 NpcClick pure-helper 首刀 `SOURCE APPROVED`；同名 Service 尚未接 public caller/typed fact，因此批准计数暂不增加。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**
