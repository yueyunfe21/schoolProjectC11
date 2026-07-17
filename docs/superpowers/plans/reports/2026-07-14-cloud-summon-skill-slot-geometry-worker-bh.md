# W-SUMMONSKILL-SLOT-GEOMETRY-CPU-IMP1 - Internal Worker BH

- `CLAIMED`: `2026-07-14T03:30:06-04:00`
- 角色：Internal Worker BH，仅负责实现；不承担 review/approval。
- 业务基线：DHXY committed `0114604e:src/main/java/com/bot/dhxy/service/SummonSkillService.java`。
- 基线 blob SHA-1：`65d8de26b1f20f17c05935c1ff3fbdeb04f0b769`。
- 开工状态：DHXY `thin-client-design@0114604e1ff5f15491d2910959c45252e893d04f`、Cloud `navigation-migration@3b988caa010254973e03342272e6d1d6a9685b01`；两仓原有 dirty/untracked 均已保护，未回滚、覆盖、清理、提交或执行任何 Git mutation。

## Implementation #1

唯一 Java 写入：

`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`

实施内容：

- 仅新增 `java.awt.Point` import，并更新类 JavaDoc，使其说明纯 CPU 颜色距离、技能槽几何和静态槽位状态职责。
- 从 committed `0114604e` 机械复制 5 个常量、3 个几何方法、tail 起点解析、状态文本方法和原 enum，共 11 块。
- 未新增 caller、public API、wrapper、Spring、本地窗口/capture/template 读取、input/UI/queue、owner/session/ledger/TTL/retry。
- 未修改上一内部波已批准的 `lowTextureTemplateMatchesByColorDistance(...)` 与 `averageColorDistance(...)` 可执行 token。

目标文件 SHA-256：

- 实施前：`7B732678BAE12502BF9D6A6D4191D02BD12C3CB691750615E2C4B66538A0E66D`
- Implementation #1：`F8D6202920DE5B0288C9FC641852642298F047F0B34E8D0F5D50CFBAA912E9B6`

## Eleven-Block Exact Check

比较方式：从 `git show 0114604e:src/main/java/com/bot/dhxy/service/SummonSkillService.java` 和 Cloud 目标文件分别提取声明行或完整 brace block，仅统一 CRLF/LF 后进行 ordinal exact comparison。结果为 `11/11 Exact=True`，即上述十一块 source/target exact diff=`0`。

| # | 基线块 | Source/Target SHA-256 | Exact diff |
|---|---|---|---|
| 1 | `SKILL_SLOT_BOX_SIZE` | `8A0E6A0B2190FC8F92A7B004981474F7AA3B6415EBA7F3D2478F13B79B1D143C` | `0` |
| 2 | `SKILL_SLOT_BOX_HALF_SIZE` | `6088D55C8BA27ADE77DA489B71EA92372A96CB282AF62C0FE4C13D6BACAD997F` | `0` |
| 3 | `STATIC_SLOT_SCAN_PADDING` | `D9C8BC2F0C92E041BAD4139A025E68C79CC34F85C6A307459B58BCE1B53C27D0` | `0` |
| 4 | `GAME_WINDOW_WIDTH` | `9CA7CF15E604EC4884A2D97588E9BD53A1EC6A643C8C7A7D9492DC606906DC0F` | `0` |
| 5 | `GAME_WINDOW_HEIGHT` | `553CD37530980E6A7019CB9AF6C949B7EE4DFF0548BC94994F85CD310F0D72A8` | `0` |
| 6 | `buildSkillSlotRects(...)` | `C6F3C19A17D8E7F91AB1AFD8A77F4EB4B89841B8B8A4DC0C312DB9B2AED86D2A` | `0` |
| 7 | `buildStaticSlotScanRelativeRoi(...)` | `E8136D00E682C1C8D37598460EF9F2B23CDB44D41FF0B7323906CCCDF9F0171E` | `0` |
| 8 | `toScreenRect(...)` | `D3299D5301FBAE9AAB5896A66CC8242AA08ACEFA98D16FC45467550EBA07CF4B` | `0` |
| 9 | `resolveStaticTailStartIndex(...)` | `843B20895091BFF222C730B037B94121C4F579C50F80742CF75705DF58C433E6` | `0` |
| 10 | `statesText(...)` | `D2BBB393E7C42C0813F5385175E360A624843395BFBA80811DAAD0FD35ED5AD9` | `0` |
| 11 | `StaticSkillSlotState` enum | `6C38760164BDB6A3626A917A77EAFF7B434CB8AB3F01A643227A2324DA2E75D5` | `0` |

所有常量值、循环、数组边界、tail 选择条件和 enum 顺序均与 committed `0114604e` 一致。

## Previous Block Preservation

| 已批准旧方法 | 实施前 SHA-256 | 实施后 SHA-256 | 状态 |
|---|---|---|---|
| `lowTextureTemplateMatchesByColorDistance(...)` | `601D24C580463C9C546B7AB20BC8A782DC9FB162F7497FF73D55C3AC9D3486F4` | `601D24C580463C9C546B7AB20BC8A782DC9FB162F7497FF73D55C3AC9D3486F4` | unchanged |
| `averageColorDistance(...)` | `EAEC431DDC5D0F65C5A65AC213F6E74E22CCE5C4B02F015A2A45DEC72EF20B2D` | `EAEC431DDC5D0F65C5A65AC213F6E74E22CCE5C4B02F015A2A45DEC72EF20B2D` | unchanged |

## Compile Gate

- 命令：Cloud 仓根目录执行 `mvn -q compile`。
- 首次结果：exit `0`，耗时 `14.9s`。
- 交付前 fresh 复跑：exit `0`，耗时 `3.4s`。
- 未执行 `clean`，未运行 tests。
- 未启动 application/server/host/Task/UI/capture/input。

本报告仅记录 Implementation #1 实施与验证证据，不构成 review 或 approval。

## Parent Source Review #1 - APPROVED - 2026-07-14T03:32:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取五个常量、三个完整几何方法、tail 起点方法、
状态文本方法和 enum，按 LF 归一化逐字符复核，十一块全部 `exact=True`。规范化长度依次为
`42/42`、`76/76`、`54/54`、`50/50`、`50/50`、`682/682`、`852/852`、`271/271`、
`882/882`、`425/425`、`106/106`；常量、边界、数组/循环顺序、tail 条件与 enum 顺序均无漂移。

父级复算目标 SHA-256 为
`f8d6202920de5b0288c9fc641852642298f047f0b34e8d0f5d50cfbaa912e9b6`，与 BH 交付一致；
BH 的 Cloud `mvn -q compile` exit 0。未接 caller，未加入 capture/template 读取、input 或本地 mechanics。
结论 `P0=0 / P1=0 / P2=0`，本 SummonSkill slot-geometry cohort `SOURCE APPROVED`。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**
