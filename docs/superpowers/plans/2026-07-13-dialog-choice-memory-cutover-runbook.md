# DialogChoiceMemory 生产切换 Runbook（W-DCM-RUNBOOK-IMP1，R1 修订版）

> 依据：`2026-07-13-cloud-dialog-choice-memory-service-worker-a.md` Parent Design Review #3 FINAL APPROVED 的迁移工件门设计与 Parent Runbook Review #1 的四项返修。本文档只定义流程与校验门，**不包含任何生产数据、路径实值或凭据**。执行本 runbook 不修改任何 Java/schema/tests。
>
> **当前可执行性声明（诚实）**：本 runbook 依赖的 trusted resolver/manifest owner **当前不存在**（见 §2 与 blocker 登记）；在该 blocker 交付前，本 runbook 是已批准的流程合同，**不可执行**。

## 0. 范围与不变量

- 迁移对象：manifest 中每个获准 tenant/user scope 恰一个 `dialog_choice_memory.json`（HEAD `0114604e` `MemoryFile` 形状）。
- 业务代码零参与：Cloud `DialogChoiceMemoryService`/`MemoryService`/`CloudServiceConfiguration` bean（`:35-36`）保持逐字实现；本 runbook 是 **host/Task 激活前** 的前置门。
- **no-clobber 不变量**：任何已存在的目标文件绝不被本流程替换或删除（除非能以本次 operation token 证明它是本次流程自己的产物）；**全流程禁止 `REPLACE_EXISTING`**。业务 `save`（`:239-245`）的 live replace 语义只属于已激活的业务运行期，与首次迁移发布无关。
- 绝不双写；任何失败该 scope 不放行激活。

## 1. canonical 源文件选择与 scope 归属

| 情形 | 决策 |
|---|---|
| DHXY 运行目录存在 `config/dialog_choice_memory.json` | **canonical 源**（新格式）。**归属语义：该文件是单机全局 bootstrap seed（本地实现无 tenant/user 概念）——复制到 manifest 中每个获准 scope**；若未来有证据证明其只归属单一 scope，由父级在 manifest 层裁定收窄，本 runbook 不自行推断 |
| 仅存在 legacy `config/transfer_choice_memory.json` | **fail-closed，中止**。登记切换前 blocker `BLOCKER-DCM-LEGACY-CONVERT`：需独立可信转换工具（HEAD `migrateLegacyRouteKeys` 等价规则、父级批准）先离线产出新格式文件，之后重入本 runbook |
| 两者皆无 | **无源新租户**：跳过预置，审计记 `NO_SOURCE_SKIP`，照常放行激活（空库=基线 load 缺失文件行为）|
| 两者皆存在 | 以 `dialog_choice_memory.json` 为 canonical，legacy 忽略（基线 load 优先序）|

## 2. trusted resolver / manifest owner（唯一路线；当前为 blocker）

- **事实**：`CloudServiceStorage.hashScope` 为 private，`establishRealScopeRoot/resolveWithinRealScope` 为 host package-private；不存在任何现成的切换编排调用入口或只读运维入口。**因此目标解析当前没有可落地执行者。**
- **唯一路线（非可选项）**：一个**已认证 host/control-plane 侧的 trusted resolver/manifest owner**（host 包内实现，复用生产 `hashScope`/containment 代码路径）从 control-plane inventory 读取获准 scope 清单，产出**不可手改的 cutover manifest**，每行：`operationId（唯一）+ scope(tenantId,userId) + resolved target 绝对路径`。操作者只消费 manifest，**零手工输入 scope、零手工拼路径**——消除"同一全局文件放错 scope"的手填面。
- **blocker 登记**：`BLOCKER-DCM-TRUSTED-RESOLVER`——该 resolver/manifest owner 交付并经父级批准前，禁止执行 §3 及之后步骤、禁止任何 scope 激活放行。

## 3. 每 scope 复制与校验门（按 manifest 逐行执行）

0. **cutover lease / activation freeze**：对该 scope 取得排他 cutover lease（由 manifest owner/control-plane 发放，与 `operationId` 绑定）；lease 有效期内**冻结该 scope 的 host/Task 激活与任何其它写入者**。无 lease 能力 → fail-closed（`ABORT_NO_LEASE`）。
1. **lease 内目标预检查**：目标文件已存在 → `ABORT_TARGET_EXISTS`（绝不覆盖、绝不删除，人工裁定）。
2. **源快照**：记录源绝对路径、字节数、SHA-256。
3. **shape 校验（源）**：**用当前构建产物中的 `DialogChoiceMemoryService.MemoryFile`/`DialogChoiceEntry` 类与同配置 ObjectMapper（`FAIL_ON_UNKNOWN_PROPERTIES=false`）做真实绑定**（不手工维护字段清单/计数——当前实现为 18 个持久字段，以类定义为唯一权威），并额外校验：`entries != null`、每个 entry value 非 null、字符串/坐标/计数字段类型可绑定；未知字段容忍（基线）。失败 → `ABORT_SOURCE_SHAPE`。
4. **operationId 唯一 staging**：复制源 → 同目录 `dialog_choice_memory.json.<operationId>.staging`（唯一名，与旧残留/并发运行零冲突）；写完执行 flush + force（`FileChannel.force(true)` 语义）落盘；随后重新打开 staging 复验字节数+SHA-256 与源一致、shape 校验通过。失败 → 删除本 operationId 的 staging → `ABORT_STAGING`。
5. **原子 no-replace 发布**：lease 内**重验目标仍不存在**（`ABORT_TARGET_EXISTS`），然后以**原子 create-if-absent 语义**发布：`ATOMIC_MOVE`（**不带** `REPLACE_EXISTING`），要求平台在目标已存在时原子失败；**若平台无法证明"原子且 no-replace"两性质，fail-closed（`ABORT_NO_ATOMIC_NOREPLACE`），无任何 fallback**。发布后重新打开目标复验 bytes+SHA-256+shape。复验失败 → `ABORT_PUBLISH_VERIFY`（目标此时可被删除——它由本 operationId 在本 lease 内创建，归属可证明）。
6. **放行**：全部通过 → 审计记 `PLACED`，释放 lease，该 scope 允许激活。

## 4. 失败清理、崩溃恢复与回滚点

- **清理归属规则**：任何清理只能删除**由本次 `operationId` 证明拥有**的文件（唯一命名的 staging；或 §3.5 内本次创建且复验失败的目标）。非本 operation 的任何文件（含预检查发现的既有目标）一律不动。
- **进程崩溃恢复**：残留 staging/未完成 operation 仅能通过同一 manifest 的 `operationId` 识别；恢复流程=按 manifest 重新进入 §3.0（重取 lease）→ 清理本 operationId 残留 → 从 §3.1 重跑。恢复完成前**该 scope 激活门保持关闭**。
- **回滚点**：激活前删除本次产物=完全回滚（该 scope 无活体写入）；**激活后不存在 runbook 级回滚**（活体状态归业务 durable 文件，修复走正常运维，不得用旧源重放）。

## 5. 审计证据模板（每 scope 一条）

```
operationId: <uuid>
scope: tenantId=<t> userId=<u>（来源=manifest，非手填）
operator / timestamp(America/New_York): <op> / <ts>
lease: ACQUIRED@<ts> RELEASED@<ts> | ABORT_NO_LEASE
source: path=<p> bytes=<n> sha256=<hex> | NONE
sourceShapeCheck(build-artifact binding): PASS | ABORT_SOURCE_SHAPE(<detail>)
staging: name=dialog_choice_memory.json.<operationId>.staging forced=YES reopenVerify=PASS|ABORT_STAGING
target: path=<from-manifest> preCheck=ABSENT|ABORT_TARGET_EXISTS releaseCheck=ABSENT|ABORT_TARGET_EXISTS
publish: ATOMIC_NOREPLACE=PASS | ABORT_NO_ATOMIC_NOREPLACE ; postPublishReopenVerify: PASS | ABORT_PUBLISH_VERIFY
decision: PLACED | NO_SOURCE_SKIP | ABORT_* | BLOCKED(BLOCKER-DCM-LEGACY-CONVERT | BLOCKER-DCM-TRUSTED-RESOLVER)
activationReleased: YES | NO
```

## 6. 登记的切换前 blocker（当前全部未交付）

- `BLOCKER-DCM-TRUSTED-RESOLVER`：host/control-plane 侧 trusted resolver + 不可手改 manifest（operationId+scope+resolved target）+ scope cutover lease 能力——交付前本 runbook 整体不可执行。
- `BLOCKER-DCM-LEGACY-CONVERT`：legacy-only scope 的独立可信转换工具——交付前该类 scope 不得预置、不得激活。

—— 本 runbook 到此为止；不实际复制生产数据、不含凭据。
