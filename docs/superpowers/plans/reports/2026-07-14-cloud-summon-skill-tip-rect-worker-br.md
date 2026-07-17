# W-SUMMONSKILL-TIP-RECT-CPU-IMP1 - Internal Worker BR

## CLAIMED

- `task`: `W-SUMMONSKILL-TIP-RECT-CPU-IMP1`
- `claimedAt`: `2026-07-14T04:48:45.0443343-04:00`
- 角色：Internal Worker BR，仅负责实现；不承担 review/approval。
- 唯一 Java 写集：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- 唯一报告：`D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-summon-skill-tip-rect-worker-br.md`

## Pre-Implementation Baseline Conflict - 2026-07-14T04:51:06.7835993-04:00

- 已解析基线为 DHXY `0114604e1ff5f15491d2910959c45252e893d04f`。
- 该 commit 的真实源方法是：

```java
private int[] buildTipRectByHoverPoint(Point hoverAbsPoint) {
    int x1 = hoverAbsPoint.x + HOVER_TIP_OFFSET_X;
    int y1 = hoverAbsPoint.y + HOVER_TIP_OFFSET_Y;
    return new int[]{x1, y1, x1 + HOVER_TIP_AREA_W, y1 + HOVER_TIP_AREA_H};
}
```

- 当前 DHXY 工作树对该文件无 diff，方法仍为相同 `int[]` corner tuple；`git log --all -S"private Rectangle buildTipRectByHoverPoint"` 无命中。
- 派单文字明确要求 `private Rectangle buildTipRectByHoverPoint(Point hoverAbsPoint)` 及按需新增 `java.awt.Rectangle` import，与 committed 源块签名和返回表达式冲突。
- 为避免 Worker 自行把 corner tuple 改写为 `Rectangle(x, y, width, height)` 或擅自忽略派单签名，当前未编辑唯一 Java 写集，等待父级明确选择“逐 token 复制基线 `int[]`”或“批准 `Rectangle` 适配”。

## Parent Source Contract Correction #1 - 2026-07-14T04:52:00-04:00

父级独立复核 confirmed：原派单中 `Rectangle` 签名是父级转录错误，不是已批准的业务差异。
权威合同仍为 committed `0114604e` 真实源块。请原 BR 继续同一任务，只机械迁入：

- 四个已列 `HOVER_TIP_*` 常量；
- `private int[] buildTipRectByHoverPoint(Point hoverAbsPoint)` 完整基线方法；
- 返回值必须仍为 `{x1, y1, x1 + width, y1 + height}` corner tuple，不得改为 `Rectangle`。

不新增 `java.awt.Rectangle` import。其余原任务冻结条件、唯一写集、Cloud `mvn -q compile`
和 Implementation 证据合同不变。**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Implementation #1 - 2026-07-14T04:56:40.1658436-04:00

### 实施范围

- 唯一 Java 写入：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`。
- 仅迁入四个 `HOVER_TIP_*` 常量与 `private int[] buildTipRectByHoverPoint(Point hoverAbsPoint)` 完整方法。
- 复用已有 `java.awt.Point` import；未新增 `java.awt.Rectangle` 或其它 import。
- 方法保持 dormant：目标文件内 `buildTipRectByHoverPoint(` 仅出现 `1` 次，即只有声明，没有 caller。
- 未迁入其它常量、方法、capture/template/OCR、remote、I/O、input、UI、owner/session/ledger/TTL/retry。

### 精确源块

来源：DHXY committed `0114604e1ff5f15491d2910959c45252e893d04f:src/main/java/com/bot/dhxy/service/SummonSkillService.java`。

```java
    private static final int HOVER_TIP_OFFSET_X = 25;
    private static final int HOVER_TIP_OFFSET_Y = 0;
    private static final int HOVER_TIP_AREA_W = 237;
    private static final int HOVER_TIP_AREA_H = 123;
```

```java
    private int[] buildTipRectByHoverPoint(Point hoverAbsPoint) {
        int x1 = hoverAbsPoint.x + HOVER_TIP_OFFSET_X;
        int y1 = hoverAbsPoint.y + HOVER_TIP_OFFSET_Y;
        return new int[]{x1, y1, x1 + HOVER_TIP_AREA_W, y1 + HOVER_TIP_AREA_H};
    }
```

### 精确目标块

目标：Cloud 当前 `src/main/java/com/bot/dhxy/service/SummonSkillService.java`。

```java
    private static final int HOVER_TIP_OFFSET_X = 25;
    private static final int HOVER_TIP_OFFSET_Y = 0;
    private static final int HOVER_TIP_AREA_W = 237;
    private static final int HOVER_TIP_AREA_H = 123;
```

```java
    private int[] buildTipRectByHoverPoint(Point hoverAbsPoint) {
        int x1 = hoverAbsPoint.x + HOVER_TIP_OFFSET_X;
        int y1 = hoverAbsPoint.y + HOVER_TIP_OFFSET_Y;
        return new int[]{x1, y1, x1 + HOVER_TIP_AREA_W, y1 + HOVER_TIP_AREA_H};
    }
```

### Source/Target Block SHA-256

比较方式：分别从 `git show 0114604e:.../SummonSkillService.java` 与 Cloud 目标文件提取上述完整块，仅统一 CRLF/LF，以 UTF-8 对不含尾随换行的块计算 SHA-256，并做 ordinal comparison。

| 块 | Source/Target length | Source SHA-256 | Target SHA-256 | Exact |
|---|---:|---|---|---|
| 四个 `HOVER_TIP_*` 常量 | `212/212` | `b91118b2fd462b5eb33b068496d854db0ea7aa6134f3d19f6034ee34db326a1a` | `b91118b2fd462b5eb33b068496d854db0ea7aa6134f3d19f6034ee34db326a1a` | `True` |
| `buildTipRectByHoverPoint(Point)` | `261/261` | `5b9cd608d4362e3271f5acc6cbf2b036004d9d9889a8b160fc2b57b3d49f0946` | `5b9cd608d4362e3271f5acc6cbf2b036004d9d9889a8b160fc2b57b3d49f0946` | `True` |

四个数值、加法顺序、无 null guard 的基线行为，以及 `{x1, y1, x1 + width, y1 + height}` corner tuple 返回语义均逐 token 保持。

### Java SHA-256

- 实施前：`AB3FEBB579F8130916E5FFE6591C7D86BF65698A24E11EA2FD02B65206664650`。
- Implementation #1：`0E4572B25AB22D7B4190027902C193255B28B3D1D53876ACC071E5616738092A`。

### Compile Gate

- 命令：在 `D:\mavenProject\dhxy-cloud-brain` 执行 `mvn -q compile`。
- 结果：exit `0`，耗时 `3.4s`。
- 未执行 `clean`，未运行 tests；未启动 application/server/host/Task/poller/UI/capture/input。

### 写集反证

- 目标声明计数：四个 `HOVER_TIP_*` 常量恰为 `4`，`buildTipRectByHoverPoint(` 恰为 `1`，`import java.awt.Rectangle;` 为 `0`。
- 从实施后 Java 文本仅移除上述常量块和方法块后，内存重建文件 SHA-256 为 `AB3FEBB579F8130916E5FFE6591C7D86BF65698A24E11EA2FD02B65206664650`，与实施前 Java SHA 完全一致；因此该 Java 文件没有夹带其它 token 改动。
- Cloud scoped status 仅显示既有 untracked 目标：`?? src/main/java/com/bot/dhxy/service/SummonSkillService.java`。
- DHXY scoped status 仅显示本单新报告：`?? docs/superpowers/plans/reports/2026-07-14-cloud-summon-skill-tip-rect-worker-br.md`。
- 本 Worker 的所有 `apply_patch` 目标仅为上述唯一 Java 文件与唯一报告；未编辑 CR271、`ACTIVE_WORK`、迁移矩阵、dashboard 或其它源码/文档，未做任何 Git mutation。

本报告仅记录 Implementation #1 实施与验证证据，不构成 review 或 approval。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**

## Parent Source Review #1 - `W-SUMMONSKILL-TIP-RECT-CPU-IMP1` - 2026-07-14T05:22:00-04:00

**APPROVED，P0/P1/P2=0。** 父级复算目标文件 SHA-256 为
`0e4572b25ab22d7b4190027902c193255b28b3d1d53876acc071e5616738092a`；四常量与
`buildTipRectByHoverPoint(Point)` 的 source/target 完整块 exact，方法恰一处，`Rectangle` import 为零。
Worker fresh Cloud `mvn -q compile` exit 0；无 capture/template/OCR/input/caller。
本 leaf `SOURCE APPROVED`，内部 BR 可关闭。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
