# TURN-03B Report - Cloud Template GET Handler

## CLAIMED

- 领取时间：`2026-07-15T14:39:29-04:00`
- 状态：`CLAIMED`
- 角色：Internal implementation worker；不是 manager/reviewer，不自批。
- `countUnit`：`N/A (INFRA Cloud template GET handler)`
- `countDelta`：`0`
- `startDependsOn`：`TURN-00`，已 `PARENT APPROVED，P0/P1/P2=0，card CLOSED`。
- `approvalDependsOn`：`TURN-03A`；03A 固定报告与 CR271 已写明
  `SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING；源码 owner 已释放`。
- 精确写集：
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTemplateHttpHandler.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-15-turn-card-TURN-03B.md`
- 只读复用：
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\turn\CloudTemplateCatalog.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\PackagedTemplateAssets.java`
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\yueyunfe\dhxy\cloudbrain\host\CloudTemplateAssets.java`
- 禁止触碰：两仓上述精确写集之外的所有文件；尤其不修改 catalog、assets、`CloudBrainServer.java`、
  routes、gateway、Maven/config、协议 DTO、主计划、CR271、`ACTIVE_WORK.md` 或 dashboard。
- 实施边界：只实现认证后的严格 `GET /api/v1/templates/<url-encoded-templateKey>` 单 key handler；
  只消费 catalog 返回的相同 PNG/hash/ETag，支持精确 `If-None-Match -> 304`，响应大小有硬上限；
  不创建第二 loader、filesystem/path authority、listing/目录枚举、owner/permit/session/ledger/compaction/
  durable workflow、业务 TTL 或自动 retry。

## 领取基线与两仓状态

### DHXY

- 分支/HEAD：`thin-client-design` / `0114604e1ff5f15491d2910959c45252e893d04f`；当前分支无 upstream。
- 领取时仓库继续存在大量既有 config/docs/Maven/Java modified/deleted，以及 plans/reports/spec、模板、
  `cloud/remote/`、`cloud/turn/` 和多个 Service/model 目录 untracked；全部保护，不回滚、不覆盖、不清理、不提交。
- 本卡只新增固定报告，不修改 DHXY 源码或父级文档。

### dhxy-cloud-brain

- 分支/HEAD：`navigation-migration` / `3b988caa010254973e03342272e6d1d6a9685b01`；远端参考
  `origin/main=48e37813913094cacd1143fcae02704033eecb93`。
- 领取时既有 `pom.xml`、server/engine/algorithm/gateway/maps modified，以及 migration、`com/bot/`、host/remote、
  turn、模板等 untracked；全部保护，不回滚、不覆盖、不清理、不提交。
- 目标 `CloudTemplateHttpHandler.java` 在 HEAD 与领取前工作树均不存在。
- 03A catalog 为领取前既有 untracked，只读 SHA-256：
  `D21123B7378826E622979EB5C8A4883F8FF52B63BC1C1EC621DC6FB57417E663`；本卡不得修改。

## 验证约束

- 不运行 runtime/application/server/Task/poller/UI/capture/input/tests。
- 只做源码展开、scoped diff/hash/status、禁用能力扫描与构建 cohort 判断。
- Cloud Java writers 稳定且整仓可构建时运行 `mvn -q clean package`；若既存并发/缺依赖仍阻断，则准确记录，
  不越界修复、不伪造 Maven 通过。
- 实现后仅追加本报告 `DELIVERED` 与证据，停止等待父级独立审查。

## DELIVERED

- 交付时间：`2026-07-15T14:41:42-04:00`
- 状态：`DELIVERED / BUILD COHORT PENDING`
- 完成内容：
  - 新增 `CloudTemplateHttpHandler implements HttpHandler`，构造只接收 03A `CloudTemplateCatalog` 与既有语义的
    bearer token；没有 assets、`ImageIO`、classpath 或 filesystem loader。
  - 只接受大小写精确的 `GET`；其它 method 返回 `405` 与 `Allow: GET`，token 不精确匹配返回 `401`。
  - 只接受 `/api/v1/templates/` 后的一个非空 URL-encoded key suffix，拒绝 query、空 key 与 malformed percent
    encoding；decoded key 只交 `catalog.find(templateKey)`，非法/缺失统一 `404`。
  - 成功内容只取 catalog `TemplateContent.pngBytes()/etag()`；handler 不重新编码 PNG、不重新计算 hash、不建立
    第二权威。ETag 直接携带 catalog 的 `"sha256:<hex>"` content hash，`If-None-Match` 只有与该 ETag 完全相等
    才返回无 body `304`。
  - `200` 固定 `Content-Type: image/png`、catalog ETag 与精确 Content-Length；PNG 超过 `8 MiB` 或为空返回无
    body `413`。其它错误同样无 body，catalog 基础能力异常返回无 body `500`，所有 handler 输出均有硬边界。
  - 无 path/list/walk API，不暴露 filesystem 路径或目录枚举；未增加 owner/permit/session/ledger/compaction/
    durable workflow、业务 TTL、timer、scheduler 或 retry。
- 源码证据：
  - `CloudTemplateHttpHandler.java` SHA-256：
    `86B964AFC1ECBCC0EA7A70FC05FA74FFA8AFE9872C11374248651670C5848B4D`。
  - scoped 禁用能力扫描 `PackagedTemplateAssets|CloudTemplateAssets|ImageIO|java.nio.file|java.io.File|Files.|
    Path.of/get|.list(|.walk(|DirectoryStream|owner|permit|session|ledger|compaction|durable|TTL|@Scheduled|
    @PostConstruct|retry` 无命中。
  - `git diff --no-index --check -- NUL <handler>` 除 Windows LF/CRLF 提示外无 whitespace error。
  - scoped status 仅新增 `CloudTemplateHttpHandler.java`；未修改 server/routes/gateway/Maven/config/protocol。
- 只读权威未变：
  - `CloudTemplateCatalog.java` SHA-256 仍为
    `D21123B7378826E622979EB5C8A4883F8FF52B63BC1C1EC621DC6FB57417E663`；
  - `PackagedTemplateAssets.java` 仍为
    `5A7000814D3735ABF9CF699CE42D5D9A43BE76337D4EC6D892887A784F823DA7`；
  - `host/CloudTemplateAssets.java` 仍为
    `59F44AD14B6D7519388A226FD653C0B9E537A49EAF5EF4BD89A916CB3CB0204E`。
- Maven：本卡未运行。领取后 `TURN-02` 于 `2026-07-15T14:40:09-04:00` 明确 `CLAIMED`，正在独占写
  Cloud `CloudTurnExchange.java`、`CloudTurnCommandPort.java`、`CloudTurnCommandResult.java`、
  `CloudTurnFrame.java`、`CloudTurnActionFactory.java`；Cloud Java writer 未稳定。且 03A 已记录整仓仍有领取前
  incomplete migration sources 的缺依赖编译阻断。本卡不对活动 writer 跑 cohort build，不越界修复、不伪造通过。
- 未运行 runtime/application/server/Task/poller/UI/capture/input/tests；未执行 Git 写操作、回滚、覆盖、清理或提交。
- `countUnit`：`N/A (INFRA Cloud template GET handler)`；`countDelta=0`，不产生 407 ledger 增量。
- 业务差异：无已批准业务差异；按基线等价迁移。
- 下一步：停止实现并等待父级独立源码审查与 Foundation build cohort 门；本 worker 不自批、不写
  `APPROVED/CLOSED`。

## PARENT REVIEW #1 - REPAIR REQUIRED

- 审查时间：`2026-07-15T15:02:00-04:00`
- 结论：`REPAIR REQUIRED`；`P0=0 / P1=1 / P2=0`，`countDelta=0`。
- P1：`CloudTemplateHttpHandler.java:64-77` 只检查 prefix 后直接对整个 raw suffix 调用
  `URLDecoder.decode`。这会同时接受未编码的额外 `/` path segment，并把 raw `+` 按 form encoding 转成空格；
  不符合本卡“单一 URL-encoded templateKey”的严格 route 合同，使多个不同 raw URL 可能映射同一 catalog key。
- 影响：route canonicalization 不唯一，扩大了 handler 可接受的 URL 形状；后续 ETag/cache 与审计无法只靠唯一 encoded key。
- 返修条件：在 decode 前拒绝空 suffix、raw `/` 与 raw `+`；只允许 `%2F`/`%2B` 这类 percent-encoded 字节进入
  canonical key，malformed percent 继续 404。不得改 catalog、routes 或新增 loader。追加新 SHA/scoped check 后等待复审。

## REPAIR #1 DELIVERED

- 返修时间：`2026-07-15T14:48:33-04:00`
- 状态：`REPAIR #1 DELIVERED / PARENT RE-REVIEW PENDING`
- P1 精确修复：
  - 在 `URLDecoder.decode(...)` 前先提取唯一 `rawTemplateKey` suffix；
  - `rawTemplateKey.isEmpty()`、包含字面 `/` 或包含字面 `+` 时立即返回无 body `404`；
  - 只有不含 raw `/`/`+` 的 suffix 才进入一次 UTF-8 percent decode，因此 canonical key 所需的目录分隔符只能由
    `%2F`/`%2f` 进入；`%2B` 等合法 percent byte 可 decode 后继续由 catalog allowlist 判定；
  - `URLDecoder` 的 `IllegalArgumentException` 捕获未变，malformed percent 继续返回无 body `404`。
- 新源码 SHA-256：`CloudTemplateHttpHandler.java` =
  `FC99308AA8A9972DF0B26AA8885D9046811FB8EA87B71A8979BC59C51038D8C0`。
- scoped source check：`rawTemplateKey` 在 `:73` 提取，`:74-76` 完成 empty/raw slash/raw plus 三门，`:83-86`
  只对该 suffix 做一次 decode 并保留 malformed 404；`git diff --no-index --check` 除 Windows LF/CRLF 提示外
  无 whitespace error。
- scoped 禁用能力扫描仍无 `PackagedTemplateAssets|CloudTemplateAssets|ImageIO|java.nio.file|java.io.File|Files.|
  Path.of/get|.list(|.walk(|DirectoryStream|owner|permit|session|ledger|compaction|durable|TTL|@Scheduled|
  @PostConstruct|retry` 命中；未新增 loader/listing/path authority。
- 只读权威 SHA 未变：catalog=
  `D21123B7378826E622979EB5C8A4883F8FF52B63BC1C1EC621DC6FB57417E663`，
  `PackagedTemplateAssets=5A7000814D3735ABF9CF699CE42D5D9A43BE76337D4EC6D892887A784F823DA7`，
  `CloudTemplateAssets=59F44AD14B6D7519388A226FD653C0B9E537A49EAF5EF4BD89A916CB3CB0204E`。
- 严格遵守返修写集：只修改 `CloudTemplateHttpHandler.java` 并追加本报告；未修改 catalog、assets、routes、
  server 或其它文件。
- 按用户明确要求未运行 cohort Maven；未运行 runtime/application/server/Task/poller/UI/capture/input/tests。
- `countDelta=0`；无已批准业务差异；按基线等价迁移。
- 下一步：停止返修并等待父级复审；本 worker 不自批、不写 `APPROVED/CLOSED`。

## PARENT RE-REVIEW #1

- 复审时间：`2026-07-15T15:08:00-04:00`
- 父级独立展开 `CloudTemplateHttpHandler.java:64-89`，确认 decode 前已拒绝 empty/raw `/`/raw `+`，
  malformed percent 仍 404；catalog、ETag、304、body 上限与无 loader/listing 边界未变。
- 结论：`SOURCE APPROVED，P0/P1/P2=0，BUILD PENDING`；`countDelta=0`。原 P1 已关闭，源码 owner 已释放。
- Build gate：Cloud writers 仍活动，等 Foundation cohort 稳定后由父级统一 `mvn -q clean package`；本卡不自跑。

## REPAIR #2 CLAIMED

- 领取时间：`2026-07-15T15:47:30-04:00`。
- 来源：TURN-05 Parent Source Review #1 在 route 激活边界发现上游 TURN-03B handler 的独立认证缺陷；
  TURN-05 两个 Java 文件保持不变，本返修回到原 TURN-03B 写集。
- 状态：`REPAIR #2 CLAIMED / IMPLEMENTING`；`countDelta=0`。
- 唯一 Java 写集：Cloud `src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/CloudTemplateHttpHandler.java`。
- 唯一报告写集：本报告 true EOF 追加。
- P1 目标：读取完整 Authorization values list，只允许恰好一个且精确等于既有 `authorizationHeader`；
  缺失、重复、null、空白或不匹配继续走现有 `401` 与 `WWW-Authenticate: Bearer`。
- 禁止：不修改 TURN-05、catalog、server、routes、ETag、path、Maven/config、协议或其它文件；不运行
  Maven/tests/runtime/application/server/Task/poller/UI/capture/input，不执行 Git mutation，不自批、不领下一卡。

## REPAIR #2 SOURCE DELIVERED / BUILD COHORT PENDING

- 交付时间：`2026-07-15T15:48:01-04:00`。
- 状态：`REPAIR #2 SOURCE DELIVERED / PARENT RE-REVIEW PENDING / BUILD COHORT PENDING`；本 Worker
  只交付源码，不写 `APPROVED/CLOSED`，`countDelta=0`。
- 唯一源码改动：`CloudTemplateHttpHandler.java:59-62` 读取完整
  `exchange.getRequestHeaders().get("Authorization")` values list，并 fail-closed 要求 list 非 null、
  `size()==1` 且唯一值 exact 等于既有 `authorizationHeader`。
- 缺失或 header API 返回 null、重复 values、唯一 null/空白/不匹配值均不能通过 exact equality，统一保留现有
  `401`、`WWW-Authenticate: Bearer`、无 body 与 exchange close 行为。
- 其余 method/path/raw suffix decode/catalog/ETag/If-None-Match/PNG size/response 语义未修改。

### Repair #2 Evidence

- 新源码 SHA-256：`CloudTemplateHttpHandler.java` =
  `5159254E80BD1585BB6FB79A5FF0D60421454E7F1D71A23E449609415F829D38`。
- Authorization `getFirst("Authorization")` 扫描结果：`0`。
- 严格认证谓词位于 `CloudTemplateHttpHandler.java:59-62`：完整 header list、null gate、唯一数量 gate、
  exact expected-value gate。
- trailing-whitespace 扫描无命中；`git diff --no-index --check` 仅返回 Windows LF/CRLF 提示，无 whitespace error。
- 只读 catalog SHA 未变：
  `D21123B7378826E622979EB5C8A4883F8FF52B63BC1C1EC621DC6FB57417E663`。
- TURN-05 保持未修改：`CloudBrainServer.java` SHA 仍为
  `7CC098CF29427B9236D1576872E5930D4B2F1E1BFDCDFE08E1A33C086ADA8B0E`；`CloudTurnRoutes.java` SHA 仍为
  `502DAF66C5485245A964B50092B8C07AEF6444BFD63FAD9EEDBF883110C70D00`。
- scoped status 中目标 handler 仍为本卡原有 untracked 文件；未修改 TURN-05、catalog、server、routes 或其它文件。

### Gates And Handoff

- 按父级指令未运行 Maven/tests/runtime/application/server/Task/poller/UI/capture/input，未执行 Git mutation。
- 下一步仅等待父级独立复审 Repair #2 及后续 writers 稳定后的 Cloud cohort package；本 Worker 停止，不自批、
  不领取下一卡。

**无已批准业务差异；按 `696a12b0` 基线等价迁移。**

## PARENT RE-REVIEW #2

- 复审时间：`2026-07-15T15:58:00-04:00`；父级独立展开当前 handler 与实际 route 调用边界。
- 结论：`SOURCE APPROVED / BUILD COHORT PENDING`；`P0=0 / P1=0 / P2=0`，Repair #2 的 P1 关闭，owner 释放。
- 证据：`CloudTemplateHttpHandler.java:59-62` 已读取完整 Authorization values list，要求恰好一个且 exact
  匹配；缺失、重复、null/空白/不匹配继续走原 401 与 WWW-Authenticate。其余 path/catalog/ETag/body 语义未改；
  当前 SHA 为 `5159254E80BD1585BB6FB79A5FF0D60421454E7F1D71A23E449609415F829D38`。
- 该上游修复同步满足 TURN-05 Parent Review #1 的唯一 integration blocker；Cloud writers 仍活动，本卡不单跑 Maven。
