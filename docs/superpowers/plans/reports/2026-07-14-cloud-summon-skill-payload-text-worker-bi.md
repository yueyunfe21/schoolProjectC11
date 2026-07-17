# W-SUMMONSKILL-PAYLOAD-TEXT-CPU-IMP1

## CLAIMED

- claimedAt: `2026-07-14T03:37:30-04:00`
- task: `W-SUMMONSKILL-PAYLOAD-TEXT-CPU-IMP1`
- role: `Internal Worker BI`（只实现，不承担 review/approval）
- baseline: `D:\mavenProject\DHXY` committed `0114604e`
- unique Java write set: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- unique report write set: `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-summon-skill-payload-text-worker-bi.md`
- frozen: caller/public API/wrapper/Spring/capture/template/I/O/window/input/UI/queue/owner/session/ledger/TTL/retry，以及既有颜色距离、槽位几何、tail、enum 块

## Implementation #1

- implementedAt: `2026-07-14T03:40:11-04:00`
- 唯一 Java 写入：`D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\SummonSkillService.java`
- 从 committed `0114604e` 机械复制：
  - `sha256Hex(byte[])`
  - `roiText(SummonSkillCloudRequest.Roi)`
  - `rectText(int[])`
- 仅加入直接需要的 `SummonSkillCloudRequest`、`MessageDigest`、`NoSuchAlgorithmException` imports，并更新类 JavaDoc，明确这些纯 CPU helper 尚未接 caller。
- 未搬 `readImage`、`readImagePayload`、capture/template/I/O、caller、public API、wrapper、Spring、本地窗口/input/UI/queue、owner/session/ledger/TTL/retry。

### New Three-Block Exact Check

比较方式：从 `git show 0114604e:src/main/java/com/bot/dhxy/service/SummonSkillService.java` 与 Cloud 目标分别抽取完整 brace block，仅统一 CRLF/LF 后 ordinal comparison。

| 块 | Source/Target length | Source/Target SHA-256 | Exact diff |
|---|---:|---|---:|
| `sha256Hex(byte[])` | `538/538` | `b41098550184aaffaa2798daff3d5c9471d883fdf6424b8ec1c77cc12792e7d1` | `0` |
| `roiText(SummonSkillCloudRequest.Roi)` | `223/223` | `dd3611cbcba5db779e8b169be9fb269b646f52ed8abf4130a452b981b11fea78` | `0` |
| `rectText(int[])` | `202/202` | `b859a9e17198f0cb3eab9529a66cb4373b25dbfac3b8224993b43db62a030a1f` | `0` |

三块异常处理、字符串格式、null 与数组长度判断逐 token 等同 `0114604e`。

### Existing Thirteen-Block Preservation

同一独立脚本重新抽取已批准的 5 个常量与 8 个方法/类型，结果 `13/13 exact=True`，均与 `0114604e` exact diff=`0`：

| 已批准块 | Source/Target length | 状态 |
|---|---:|---|
| `SKILL_SLOT_BOX_SIZE` | `42/42` | unchanged |
| `SKILL_SLOT_BOX_HALF_SIZE` | `76/76` | unchanged |
| `STATIC_SLOT_SCAN_PADDING` | `54/54` | unchanged |
| `GAME_WINDOW_WIDTH` | `50/50` | unchanged |
| `GAME_WINDOW_HEIGHT` | `50/50` | unchanged |
| `lowTextureTemplateMatchesByColorDistance(...)` | `819/819` | unchanged |
| `averageColorDistance(...)` | `1029/1029` | unchanged |
| `buildSkillSlotRects(...)` | `682/682` | unchanged |
| `buildStaticSlotScanRelativeRoi(...)` | `852/852` | unchanged |
| `toScreenRect(...)` | `271/271` | unchanged |
| `resolveStaticTailStartIndex(...)` | `882/882` | unchanged |
| `statesText(...)` | `425/425` | unchanged |
| `StaticSkillSlotState` | `106/106` | unchanged |

### Artifact And Compile

- 目标文件 SHA-256：`de58448bdc76fea488a9318ef36c4b17986e6e99915085a28bc39c3c5dba26ca`
- 目标文件大小：`6941` bytes
- Cloud 命令：`mvn -q compile`
- 结果：exit `0`，耗时 `15.4s`
- 未执行 `clean`，未运行 tests，未启动 application/server/host/Task/poller/UI/capture/input。
- 未执行 Git mutation；全部既有 dirty/untracked 保持不动。

本报告仅记录 Internal Worker BI 的 Implementation #1，不构成 review 或 approval。

## Parent Source Review #1 - APPROVED - 2026-07-14T03:44:00-04:00

父级从 committed `0114604e` 与当前 Cloud 独立抽取三个完整方法并按 LF 归一化逐字符复核，结论
`P0=0 / P1=0 / P2=0`：`sha256Hex`、`roiText`、`rectText` 均 `exact=True`，规范化长度分别为
`538/538`、`223/223`、`202/202`。异常字符串、digest 循环、ROI 字段顺序与 null/数组长度 fallback 均无漂移。

父级复算目标 SHA-256 为
`de58448bdc76fea488a9318ef36c4b17986e6e99915085a28bc39c3c5dba26ca`，与 BI 交付一致；
BI 的 Cloud `mvn -q compile` exit 0。没有 caller、I/O、capture/template、remote/input 或本地 mechanics。
本 SummonSkill payload/text cohort `SOURCE APPROVED`。
**无已批准业务差异；按 `0114604e` 基线等价迁移。**
