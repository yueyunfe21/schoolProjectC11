# Cloud Phase 3 Passive OCR/Window Models - Internal CJ3

## Parent Task Brief #1 - 2026-07-14T12:08:00-04:00

Task: `W-696-PASSIVE-OCR-WINDOW-1`

只做 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 原字节机械复制。父级已确认以下 Cloud 目标均不存在：

- `src/main/java/com/bot/dhxy/model/ocr/LearnedNpcClickPoint.java`
- `src/main/java/com/bot/dhxy/model/ocr/OcrLineResult.java`
- `src/main/java/com/bot/dhxy/model/ocr/OcrWordResult.java`
- `src/main/java/com/bot/dhxy/model/ocr/PlayerAnchorMatch.java`
- `src/main/java/com/bot/dhxy/model/ocr/TargetOcrResult.java`
- `src/main/java/com/bot/dhxy/model/ocr/TextCandidate.java`
- `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanResult.java`
- `src/main/java/com/bot/dhxy/window/runtime/WindowHandleParser.java`
- `src/main/java/com/bot/dhxy/window/model/WindowNativeBinding.java`

唯一 Java 写集为以上 9 个 Cloud 新文件；唯一文档写集为本报告。先在本报告追加 `CLAIMED`，再逐文件从
DHXY commit object 读取并以 create-new 语义落盘。禁止改 package/import/注释/逻辑/换行，禁止触碰任何
Service、pom、shared remote/schema 或其它报告；禁止 build/test/runtime/Git mutation。若任一目标已出现，立即
`BLOCKED`，不得覆盖。交付必须记录每个 source/target 的 bytes、Git blob、SHA-256，并证明全部相同。

Worker 自审只算 QA；父级独立复核后才可 APPROVED。

## Parent Source Review #1 - 2026-07-14T12:19:30-04:00

**APPROVED，P0/P1/P2=0。** 父级直接从 `696a12b0` commit object 重算 source blob/bytes，并对九个
Cloud target 独立计算 blob/bytes/SHA-256。结果 `TOTAL=9 EXACT=9 BAD=0 TOTAL_BYTES=19043`；逐项 blob：

- `LearnedNpcClickPoint` `0fea7680f869641e9abcba840bcabd90d87fffc7`
- `OcrLineResult` `b49fa30a2c93df3dd424caba742bf4d70cd8044f`
- `OcrWordResult` `5ac97a5114cb8d74f2fbc46cd854abd4a4636a80`
- `PlayerAnchorMatch` `200267215b4ff3a55949de0fa54a69d39a6bb751`
- `TargetOcrResult` `7471b838a7c853fc68f4829a96ed924ae8931029`
- `TextCandidate` `5fb5d8dc832961e213ac612b332b1ff2cbd8698c`
- `TextCandidateScanResult` `4c9e57cf39fd9f5dab28ea21670ba49e0fc68c00`
- `WindowHandleParser` `2fa1b12228f38c954234b0be6dd20a67e0941d89`
- `WindowNativeBinding` `bd1ce25599ffc3a713ab28f99acf519da2344f37`

本结论只批准 Phase 3 passive exact-copy；不把模型存在计作整类迁移完成。统一构建等待其它 Java writer
稳定后由父级执行。

## CLAIMED - 2026-07-14T12:10:18-04:00

- Worker: Internal CJ3`r
- Task: W-696-PASSIVE-OCR-WINDOW-1`r
- Scope: 仅九个 Cloud create-new 被动 OCR/window 类型与本 append-only 报告。

## DELIVERED / QA - 2026-07-14T12:14:18-04:00

- Worker: `Internal CJ3`（实现 Worker；本节仅记录自审 QA，不构成 reviewer `APPROVED`）。
- Task: `W-696-PASSIVE-OCR-WINDOW-1`。
- Source: 仅通过 `git rev-parse` / `git cat-file` 读取 DHXY commit object `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:<path>`；未读取当前 dirty 工作树中的源文件。
- Create gate: 写入前两次逐项检查 9 个 Cloud target 均不存在；写入使用 `.NET FileMode.CreateNew`，未覆盖任何文件。
- Fresh QA summary: `TOTAL=9 EXACT=9 BAD=0 TOTAL_BYTES=19043`。每行均满足 source bytes = target bytes、source Git blob = target `git hash-object`、source SHA-256 = target SHA-256。

| Path | Source / target bytes | Source blob / target `git hash-object` | Source / target SHA-256 |
|---|---:|---|---|
| `src/main/java/com/bot/dhxy/model/ocr/LearnedNpcClickPoint.java` | `1370 / 1370` | `0fea7680f869641e9abcba840bcabd90d87fffc7 / 0fea7680f869641e9abcba840bcabd90d87fffc7` | `2dca025b197c563626f68a58b219314bbeda2f659ac68071c0526b0b57cc5872 / 2dca025b197c563626f68a58b219314bbeda2f659ac68071c0526b0b57cc5872` |
| `src/main/java/com/bot/dhxy/model/ocr/OcrLineResult.java` | `2299 / 2299` | `b49fa30a2c93df3dd424caba742bf4d70cd8044f / b49fa30a2c93df3dd424caba742bf4d70cd8044f` | `8da226772080e12b84815c38152e1aa427a9b5927163f29885eb4e23be2ee43b / 8da226772080e12b84815c38152e1aa427a9b5927163f29885eb4e23be2ee43b` |
| `src/main/java/com/bot/dhxy/model/ocr/OcrWordResult.java` | `3338 / 3338` | `5ac97a5114cb8d74f2fbc46cd854abd4a4636a80 / 5ac97a5114cb8d74f2fbc46cd854abd4a4636a80` | `f816d51690f7a8f949d7456ef85c973db6945f2e5104cc0ef972de705d667fac / f816d51690f7a8f949d7456ef85c973db6945f2e5104cc0ef972de705d667fac` |
| `src/main/java/com/bot/dhxy/model/ocr/PlayerAnchorMatch.java` | `1647 / 1647` | `200267215b4ff3a55949de0fa54a69d39a6bb751 / 200267215b4ff3a55949de0fa54a69d39a6bb751` | `654931e666ebd0c446487384c02471de66cdd6d855498d4a3fe5990acd9ed38b / 654931e666ebd0c446487384c02471de66cdd6d855498d4a3fe5990acd9ed38b` |
| `src/main/java/com/bot/dhxy/model/ocr/TargetOcrResult.java` | `1439 / 1439` | `7471b838a7c853fc68f4829a96ed924ae8931029 / 7471b838a7c853fc68f4829a96ed924ae8931029` | `0dd25c41d59f8aa829101661c32131891563d9253d82a0d7614e6a7613d46d61 / 0dd25c41d59f8aa829101661c32131891563d9253d82a0d7614e6a7613d46d61` |
| `src/main/java/com/bot/dhxy/model/ocr/TextCandidate.java` | `1587 / 1587` | `5fb5d8dc832961e213ac612b332b1ff2cbd8698c / 5fb5d8dc832961e213ac612b332b1ff2cbd8698c` | `655110f1fa7d94b1688ff6cff2ce7b8217a7ebcf75f4c13f56dc1c57e1974e62 / 655110f1fa7d94b1688ff6cff2ce7b8217a7ebcf75f4c13f56dc1c57e1974e62` |
| `src/main/java/com/bot/dhxy/model/ocr/TextCandidateScanResult.java` | `2330 / 2330` | `4c9e57cf39fd9f5dab28ea21670ba49e0fc68c00 / 4c9e57cf39fd9f5dab28ea21670ba49e0fc68c00` | `9e1f68b40c7297cca6691695a1808fb6be837703988d97874eb9a7e62dc63034 / 9e1f68b40c7297cca6691695a1808fb6be837703988d97874eb9a7e62dc63034` |
| `src/main/java/com/bot/dhxy/window/runtime/WindowHandleParser.java` | `1469 / 1469` | `2fa1b12228f38c954234b0be6dd20a67e0941d89 / 2fa1b12228f38c954234b0be6dd20a67e0941d89` | `1492b60aa93822ec75ae4ad2895012e0900a12bafc9f90defc22e9510daba4b9 / 1492b60aa93822ec75ae4ad2895012e0900a12bafc9f90defc22e9510daba4b9` |
| `src/main/java/com/bot/dhxy/window/model/WindowNativeBinding.java` | `3564 / 3564` | `bd1ce25599ffc3a713ab28f99acf519da2344f37 / bd1ce25599ffc3a713ab28f99acf519da2344f37` | `afb76bfdd1fec1a5b63e4438d6767019c810e0dc7a9a0108f20d1baeaf2d139a / afb76bfdd1fec1a5b63e4438d6767019c810e0dc7a9a0108f20d1baeaf2d139a` |

### 写集

- Cloud create-new Java：仅上表 9 个文件。
- DHXY 文档：仅本 append-only 报告。
- 未改任何 Service、`pom.xml`、remote/schema 或其它报告；未执行 build、test、runtime；未执行 Git add/commit/checkout/reset/clean 等 mutation。校验使用的 `git rev-parse`、`git cat-file`、`git hash-object` 均为只读。
- Append-only 格式说明：上方首次 `CLAIMED` 的 Worker/Task 行因 shell 引号留下字面量 `` `r `` 后缀；未回写或删除既有内容，本节为该领取记录的权威字段确认。

QA 结论：九个 create-new 文件与指定 commit object 原字节一致；等待父级独立复核，本 Worker 不写 `APPROVED`。
