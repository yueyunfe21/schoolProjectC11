# Cloud Phase 3 Passive Dependency Cohort - Internal CI3

## Parent Task Brief #1 - 2026-07-14T12:08:00-04:00

Task: `W-696-PASSIVE-CONFIG-NAV-1`

只做 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 原字节机械复制。父级已确认以下 Cloud 目标均不存在：

- `src/main/java/com/bot/dhxy/config/BotProperties.java`
- `src/main/java/com/bot/dhxy/config/TeamTaskProperties.java`
- `src/main/java/com/bot/dhxy/input/action/InputActionScope.java`
- `src/main/java/com/bot/dhxy/model/navigation/WorldMapRouteResultMemoryEntry.java`
- `src/main/java/com/bot/dhxy/model/navigation/WorldMapRouteResultPendingMemory.java`

唯一 Java 写集为以上 5 个 Cloud 新文件；唯一文档写集为本报告。先在本报告追加 `CLAIMED`，再逐文件从
DHXY commit object 读取并以 create-new 语义落盘。禁止改 package/import/注释/逻辑/换行，禁止触碰任何
Service、pom、shared remote/schema 或其它报告；禁止 build/test/runtime/Git mutation。若任一目标已出现，立即
`BLOCKED`，不得覆盖。交付必须记录每个 source/target 的 bytes、Git blob、SHA-256，并证明全部相同。

Worker 自审只算 QA；父级独立复核后才可 APPROVED。

## Parent Source Review #1 - 2026-07-14T12:15:30-04:00

**APPROVED，P0/P1/P2=0。** 父级直接从 `696a12b0` commit object 重新计算 source blob/bytes，并对
Cloud target 独立计算 blob/bytes/SHA-256；五项均 `sourceBlob == targetBlob`、bytes 相同，汇总 `TOTAL=5 BAD=0`：

| 文件 | Git blob | bytes | SHA-256 |
|---|---:|---:|---:|
| `BotProperties.java` | `51ad90c0f87f5c14fd1b2368055b31e4fd4d6410` | 5773 | `D56521FDF71CDCA8B2CAA660E4398330CF64A631D135E1863F8727498FD9BB87` |
| `TeamTaskProperties.java` | `1fb19020fe4c12db20f044e09223bcc8b261aaaf` | 4328 | `03691D44A7DD8808DEB01C6694BF69E603E1C696E47629DD3BB37F106CE18255` |
| `InputActionScope.java` | `7bd2596fac4206e894cb751de81bf404a59aca07` | 1656 | `0091A249189E8196C07EDB60184BC9FF49F11331AEDF7794E932E1CA89DD66AE` |
| `WorldMapRouteResultMemoryEntry.java` | `df577bd3ad08e2d3b3572d1477a1cd3866eb11e2` | 2108 | `A8E9BEA38B00A48277AD0C2D481D293E95BA1C8AF2FFD416373879CED4AE01E4` |
| `WorldMapRouteResultPendingMemory.java` | `d77496a4fac94dd90ac32f619555b87ed526fa0b` | 1321 | `4405BED15EEA252C49348E886FA3CF9F4988A945A327E2CD53EA1A63E8DDC4C3` |

本结论只批准 Phase 3 passive exact-copy；不把 DTO/config 存在计作整类迁移完成。统一构建等待另一并发
Java writer 稳定后由父级执行。

## Worker Claim - 2026-07-14T12:09:56-04:00

`CLAIMED` - Internal CI3 已领取 `W-696-PASSIVE-CONFIG-NAV-1`；严格按本报告限定写集执行。

## Worker Delivery - 2026-07-14T12:12:05-04:00

`QA PASS` - 五个 target 在复制前逐项确认不存在；随后均直接读取 DHXY commit object
`696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:<path>`，并以 `.NET FileMode.CreateNew` 原字节写入
`D:\mavenProject\dhxy-cloud-brain\<path>`。未读取 DHXY dirty 工作树文件作为复制源。

| Path | Source bytes | Target bytes | Source Git blob | Target `git hash-object --no-filters` | Source SHA-256 | Target SHA-256 |
|---|---:|---:|---|---|---|---|
| `src/main/java/com/bot/dhxy/config/BotProperties.java` | 5773 | 5773 | `51ad90c0f87f5c14fd1b2368055b31e4fd4d6410` | `51ad90c0f87f5c14fd1b2368055b31e4fd4d6410` | `d56521fdf71cdca8b2caa660e4398330cf64a631d135e1863f8727498fd9bb87` | `d56521fdf71cdca8b2caa660e4398330cf64a631d135e1863f8727498fd9bb87` |
| `src/main/java/com/bot/dhxy/config/TeamTaskProperties.java` | 4328 | 4328 | `1fb19020fe4c12db20f044e09223bcc8b261aaaf` | `1fb19020fe4c12db20f044e09223bcc8b261aaaf` | `03691d44a7dd8808deb01c6694bf69e603e1c696e47629dd3bb37f106ce18255` | `03691d44a7dd8808deb01c6694bf69e603e1c696e47629dd3bb37f106ce18255` |
| `src/main/java/com/bot/dhxy/input/action/InputActionScope.java` | 1656 | 1656 | `7bd2596fac4206e894cb751de81bf404a59aca07` | `7bd2596fac4206e894cb751de81bf404a59aca07` | `0091a249189e8196c07edb60184bc9ff49f11331aedf7794e932e1ca89dd66ae` | `0091a249189e8196c07edb60184bc9ff49f11331aedf7794e932e1ca89dd66ae` |
| `src/main/java/com/bot/dhxy/model/navigation/WorldMapRouteResultMemoryEntry.java` | 2108 | 2108 | `df577bd3ad08e2d3b3572d1477a1cd3866eb11e2` | `df577bd3ad08e2d3b3572d1477a1cd3866eb11e2` | `a8e9bea38b00a48277ad0c2d481d293e95ba1c8af2ffd416373879ced4ae01e4` | `a8e9bea38b00a48277ad0c2d481d293e95ba1c8af2ffd416373879ced4ae01e4` |
| `src/main/java/com/bot/dhxy/model/navigation/WorldMapRouteResultPendingMemory.java` | 1321 | 1321 | `d77496a4fac94dd90ac32f619555b87ed526fa0b` | `d77496a4fac94dd90ac32f619555b87ed526fa0b` | `4405bed15eea252c49348e886fa3cf9f4988a945a327e2cd53ea1a63e8ddc4c3` | `4405bed15eea252c49348e886fa3cf9f4988a945a327e2cd53ea1a63e8ddc4c3` |

验证汇总：`TOTAL=5 BAD=0`；每项 source/target 的 bytes、Git blob/hash-object、SHA-256 全相同。

本 Worker 实际写集：

- Cloud create-new Java：上述五个 `D:\mavenProject\dhxy-cloud-brain\<path>` 文件。
- DHXY append-only 文档：本报告 `docs/superpowers/plans/reports/2026-07-14-cloud-passive-dependency-cohort-worker-ci3.md`。

未改任何 Service、`pom.xml`、remote/schema 或其它报告；未执行 build、test、runtime，也未执行任何 Git mutation。
