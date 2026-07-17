# CR271 TURN-34AT1 R1 两项 P1 父级返修预检 Helper

## 身份、范围与结论

- 身份：`CR271 Internal helper`，不是实现者、reviewer、父级批准者，也不替代独立 review。
- 范围：只核实 TURN-34AT1 独立 R1 中除 `FAILED` fixture 外的两项 P1：同窗 30 秒 gate 与
  minimal CAPTURE 两个内层机械字段。
- 事实核实结论：**R1 P1-2、P1-3 均成立。** 这是返修前置事实，不是 `Approved` 或卡片通过结论。
- `FAILED` fixture 的合法形状不在本 helper 的裁决范围；关闭本文两项也不能单独关闭父级当前
  `P0/P1/P2=0/3/0`。

## 冻结输入与只读纪律

完整读取并交叉核对：

1. `AGENTS.md:1-392`、`docs/DHXY_CONTEXT.md:1-1349`。
2. `docs/ACTIVE_WORK.md` 顶部 CR271 当前段及最新 `11:03` 条目；当前明确记录无已批准业务差异、唯一业务
   基线为 `696a12b0`。
3. `docs/superpowers/plans/reports/2026-07-16-turn-card-TURN-34AT1.md:1-248`，包括原冻结卡、交付、三轮父级
   test-source review，以及最新 Parent Review #4。
4. `docs/superpowers/plans/reports/2026-07-16-turn-34at1-independent-review-r1.md:1-75`。
5. `docs/superpowers/specs/2026-07-15-https-turn-thin-client-protocol-design.md:1-383`、当前
   `TurnStep.java`、`TurnCaptureSpec.java` 与相关 validator/factory 调用面。
6. `docs/业务逻辑.md:1-1426`；其中 `:215-224` 要求未获批准时按基线等价迁移，不得自行改变判断、条件、
   时序或过期语义。
7. Cloud 当前 `AutoCombatService.java:1-852`、`AutoCombatServiceTurnContractTest.java:1-1026`，以及
   `migration-baseline/696a12b0/.../AutoCombatService.java:1-835` 和 DHXY Git 对象
   `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7`。

本次核实快照：

| 对象 | 身份 |
| --- | --- |
| DHXY | branch `thin-client-design`，HEAD `0114604e1ff5f15491d2910959c45252e893d04f` |
| Cloud | branch `navigation-migration`，HEAD `3b988caa010254973e03342272e6d1d6a9685b01` |
| Cloud production | 852 行，SHA-256 `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9` |
| Cloud test | 1026 行，SHA-256 `b5438da588b8c572babc65fa3d6d3f1a93e7f1880da67975c843d960516c5292` |
| `TurnCaptureSpec.java` | SHA-256 `216c8f51b7b08702365e7c9ca8f2e2f43e4f9f12aa6e63febbad495fd545472c` |
| `CloudTurnActionFactory.java` | SHA-256 `81331bcbfc1d4046956af72c6e8aea7cae6a4ed0ac4e296d1d94c6424779e5cb` |
| `696a12b0` AutoCombat Git blob | `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a` |

Cloud `migration-baseline/696a12b0` 副本仅因 CRLF 与 Git blob 的 LF 字节长度不同；内存归一化换行后计算出的
Git blob 仍为 `b1c2d48e89ed6b2ca90b1639df841dd7a97d691a`，与权威 commit 对象一致。

两仓既有 dirty/untracked 全部原样保护。未运行 Maven/JUnit/compile/package、runtime/application/server、
Task/UI/capture/input；未执行 Git add/commit/checkout/reset/clean/stash/switch/merge/rebase 或其它 Git mutation。

## P1-2：同窗 30 秒 gate

### 精确证据

1. 当前 Cloud production `AutoCombatService.java:33` 固定 guard 为 `30_000L`。
2. 当前 production `AutoCombatService.java:815-827` 的 key 计算为：非空 `teamKey` 优先，只有 team key 为空时
   才回退 `windowId`。因此传入 `team-1` 后，同窗或异窗都使用同一个 `team-1` map key。
3. 当前 production `:818-824` 对同 key 计算 `age=now-lastAt`；只要 `0 <= age < 30_000` 就返回
   `deferred(30_000-age, age)`。同窗第二次 `now+10ms` 的确定结果是
   `deferred=true, retryAfterMs=29_990, lastTeamRefreshAgeMs=10`。
4. 权威 Git `696a12b0:AutoCombatService.java:33,817-833` 与上述逻辑一致；本地 baseline 副本对应
   `:33,817-833` 也一致。DHXY 当前同名 production `AutoCombatService.java:1197-1212` 仍保持同一逻辑。
5. 当前 test `AutoCombatServiceTurnContractTest.java:630-650` 已正确证明异窗同 team 在 `+29_999ms` 被
   deferred、到 `+30_000ms` 才允许。
6. 但同一 test `:653-661` 又把同 team、同 window 的 `+10ms` 第二次 reserve 断言为
   `deferred()==false`。该 JavaDoc、方法名和第 661 行断言均与 production、基线及同文件前一用例直接相反。
7. TURN-34A 父卡 `:63` 原冻结语义是“30 秒 team-sharing 规则不变”；其交付文字 `:304` 的“含同窗不自锁”
   没有 `696a12b0` 源码依据。最新 AT1 Parent Review #4 `:234-236` 已按 production/基线裁决：同窗第二次必须
   deferred，禁止添加 same-window exception。

### 核实判断

**R1 P1-2 成立。** 当前失败点是测试写反，不是 production 缺少同窗豁免。AT1 是 test-only 且无已批准业务
差异；正确返修是修正测试，不得改 `RefreshDuePanelVerifyGate`。

## P1-3：minimal CAPTURE 内层字段

### 精确证据

1. 当前 positive test `AutoCombatServiceTurnContractTest.java:404-416` 已锁定 index/type，并断言
   `inputAction/input/waitMs/match/localService` 五个 `TurnStep` 外层非 CAPTURE union 字段为 null；随后只检查
   `capture.region` 和 `capture.resultMode`。
2. 该区间没有断言 `step.capture().clearPointerIfOverRegion()` 或
   `step.capture().pixelChangeProbe()`。
3. 当前 `TurnStep.java:3-11` 表明 `capture` 是一个内层 `TurnCaptureSpec`，外层 union 为纯 CAPTURE 并不代表
   capture spec 内没有附加机械策略。
4. 当前 `TurnCaptureSpec.java:9-18` 明确定义两个可选协议字段：
   `clearPointerIfOverRegion` 与 `pixelChangeProbe`；`:20-22` 的二参数构造器才显式把两者设为 null。
5. 当前 `CloudTurnActionFactory.java:30-37` 确实使用二参数构造器，所以**当前 production 发出的 Stage-1
   CAPTURE 两项目前均为 null**。R1 指出的是测试保护缺口，不是当前 production 已经发错 payload。
6. 当前 `TurnProtocolValidator.java:225-260` 会接受满足约束的非 null pointer-clear 或 pixel-change probe，
   只要求两者互斥并校验各自区域、延时与阈值。也就是说，未来 production 若合法附加其中一项，现有
   `:404-416` 仍可全部通过。
7. 协议设计 `https-turn-thin-client-protocol-design.md:242-254` 明确说明 `pixelChangeProbe` 会执行 exact-HWND
   前截图、Ctrl down、鼠标移动、后截图、像素比较和 Ctrl up；它不是普通 minimal capture 的无动作元数据。
   `TurnCaptureSpec.java:9` 同样说明 pointer-clear 会引入 pointer read/input。
8. 本测试的 `ScriptedCommandPort` 在命令边界直接返回脚本 outcome，并不执行客户端 CAPTURE mechanics；因此
   缺少这两个显式 null 断言时，测试不会通过运行副作用偶然发现回归。

### 核实判断

**R1 P1-3 成立。** 当前 factory 行为正确，但“one minimal CAPTURE”合同尚未冻结当前协议的完整内层 null
shape。外层 union null 断言不能代替内层机械字段断言。

## 最小单测试文件返修

两项 P1 的 Java 返修只需要修改：

`D:/mavenProject/dhxy-cloud-brain/src/test/java/com/yueyunfe/dhxy/cloudbrain/service/AutoCombatServiceTurnContractTest.java`

不新增测试文件，不改 production、protocol、factory、POM、resources 或其它测试。

### 1. 补 CAPTURE 内层 null

紧接当前 `:416` 后增加：

```java
assertNull(step.capture().clearPointerIfOverRegion(),
        "a minimal Stage-1 capture performs no pointer read or input");
assertNull(step.capture().pixelChangeProbe(),
        "a minimal Stage-1 capture performs no Ctrl-hover pixel probe");
```

### 2. 修正同窗 gate 用例

就地改当前 `:653-662`，不另建重复用例：

```java
/** The 30s team gate also applies when the same window re-reserves for the same team. */
@Test
void refreshDueGateDefersTheSameTeamAndSameWindowInsideThirtySeconds() {
    AutoCombatService.RefreshDuePanelVerifyGate gate =
            new AutoCombatService.RefreshDuePanelVerifyGate();
    long now = 2_000_000L;

    AutoCombatService.RefreshDuePanelVerifyDecision first =
            gate.reserveIfAllowed("team-1", "window-34a", now);
    AutoCombatService.RefreshDuePanelVerifyDecision sameWindowTooSoon =
            gate.reserveIfAllowed("team-1", "window-34a", now + 10L);

    assertFalse(first.deferred());
    assertTrue(sameWindowTooSoon.deferred());
    assertEquals(29_990L, sameWindowTooSoon.retryAfterMs());
    assertEquals(10L, sameWindowTooSoon.lastTeamRefreshAgeMs());
}
```

方法名、JavaDoc 和断言必须一起改，避免源码继续宣称 same-window exemption。精确 age/retry 断言直接冻结
`696a12b0` 的 30 秒边界，且不会引入新的业务语义。

## 返修验收

1. Diff 的 Java 写集只包含上述唯一 test；production SHA 必须仍为
   `532e6f840e0847381de2cef68153cbcac563b11bd5de9ccdfd0570c6b84aa6e9`。
2. Positive Stage-1 用例同时显式断言全部五个外层非 CAPTURE union 字段以及两个内层 capture mechanics 字段
   为 null；原 exact ROI、`UPLOAD_IMAGE`、120 秒、metadata/raw PNG/SHA 与单 command 断言保留。
3. 同 team/同 window 的 `+10ms` 第二次 reserve 必须断言 `deferred=true`、`retryAfterMs=29_990`、
   `lastTeamRefreshAgeMs=10`；不得出现 production same-window exemption。
4. 新 test SHA 冻结后，由父级确认 writers 已停止并开放 test gate，再在 Cloud 仓运行唯一 named command：

```powershell
cd D:\mavenProject\dhxy-cloud-brain
mvn -q -Dtest=AutoCombatServiceTurnContractTest test
```

   必须使用当前 source、exit `0`，不得加 `-DskipTests` 或引用 stale class/jar；Cloud compile/package、父级复审与
   两名最新独立 reviewer 仍按权威计划执行。本 helper 未运行该命令，也不预判返修后通过。

## Parent Handoff

- P1-2：**确认成立；测试与 current production、`696a12b0` 基线相反。**
- P1-3：**确认成立；当前协议下漏锁 `clearPointerIfOverRegion` 与 `pixelChangeProbe` 两个内层 null。**
- 推荐动作：只返修同一 named test 的上述两个位置；production/protocol 保持冻结。
- 本报告仅提供父级返修 preflight 事实，不是 review、approval、owner claim 或 Java 修改授权。

<!-- TRUE_EOF: TURN-34AT1 R1 FINDINGS PARENT PREFLIGHT HELPER P1-2=CONFIRMED P1-3=CONFIRMED NON-REVIEW NON-APPROVAL -->
