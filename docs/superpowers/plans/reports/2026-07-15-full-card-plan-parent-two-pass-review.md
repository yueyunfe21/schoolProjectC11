# CR271 HTTPS Turn 全卡计划父级两轮审查

> 结论：`PARENT PLAN APPROVED / IMPLEMENTATION PAUSED`
>
> 审查范围：`TURN-00..47`、全部 A/B/C/D/E/P/M/R 子卡、权威协议、真实双仓源码引用图、运行入口、
> Task 构造/继续执行、pause/stop/unregister、旧链删除和最终构建门。

## 1. 审查方法

第一轮逐卡检查输入、唯一产物、精确写集、真实 caller、验收、直接 predecessor 和同文件 owner；第二轮分别从
用户可见运行入口与最终删旧终点逆向回查。四份 helper 报告仅作为非绑定 PRECHECK，父级重新读取源码并独立
裁决。两轮共同发现 `P0/P1/P2=0/8/1`，均已在权威计划第 14..18 节修复。

## 2. 已关闭的八个 P1

1. PNG 在 Cloud ingress 校验后被 `CloudTurnExchange` 丢弃：新增 `TURN-02R`，command result 原子返回
   outcome + exact frame bytes。
2. Server 的真实 exchange/catalog 无法注入 dormant host：新增 `TURN-13H`，只建立同源 capability，不激活。
3. actionId/device/window/result mapping 无统一入口：新增 `TURN-13G`，每次显式调用只生成一个 UUID，uncertain
   向上返回，不自动执行第二次业务动作。
4. 业务 caller 早于 turn-native Task context：新增前置 `TURN-13C`；`TURN-38A` 只做后期 old-authority 清理。
5. `TurnRequest` 无 Task queue/start ack：`TURN-40A` 冻结 ordered task codes、failure policy、stable
   startRequestId/ack 与 pause/stop metadata。
6. Cloud 无真实 Task factory/runtime/host caller：拆为 `TURN-40B` runtime、`TURN-40C` Cloud activation、
   `TURN-40D` DHXY control lifecycle。
7. 原 context/facade 与三大 Task 存在前后循环：13G/13C 前移，35/36/37 后接，38/39 仅清旧。
8. provider-first 删除会产生不可编译中间树：DHXY 固定 `43A -> 42A -> 43B`，Cloud 固定
   `45A -> 44A -> 45B`，前置三个 exact hash/reference manifest。

P2 文档漂移也已关闭：协议示例不再使用不存在的 `CLICK/KEY_PRESS` step，而是实际
`INPUT + TurnInputAction + TurnInputSpec` 结构。

## 3. 权威 DAG

```text
已完成 Foundation 00..13
  -> (02R || 40A)
  -> 13G -> 13H -> 13C
  -> 14..29 Service/algorithm waves
  -> 30/31/32 + 33 -> 34A/34B -> 34C
  -> 35/36/37
  -> 38A -> (38B1/B2/B3/B4 || 38M) -> 38C -> 39
  -> 40B -> 40C -> 40D
  -> 双仓 compile -> 41 user fresh-runtime gate
  -> (42M || 43M || 44M45M)
  -> DHXY 43A -> 42A -> 43B
  -> Cloud 45A -> 44A -> 45B
  -> 46 -> 47
```

业务波次保持最多三条互斥 Internal implementation；删除阶段每仓只有一条 Java 删除线，两个仓可并行，第三槽
只做只读 manifest/zero-ref 复核。任何卡若需要写第 17 节之外的文件，必须先 BLOCKED 并由父级修订计划。

## 4. 已冻结的运行合同

- 一个 Cloud server 进程配置一个 tenant/user/stateRoot；deviceId+windowId 是执行身份，不是私有状态 scope。
- start request 使用 ordered task codes 与现有 failure policy；`SLEEP_COMPUTER` 不允许远程启动。
- 相同 startRequestId 的传输重送只相关同一内存 ack，不启动第二个 Task；不引入 session、ledger、TTL 或 durable
  workflow。
- pause 只暂停 Cloud Task checkpoint，DHXY long-wait 继续；stop 后 unregister 才移除 loop/window binding。
- 四个永久本地 Service 不变；其它 OCR、像素、候选、业务 fallback 全在 Cloud。
- 无已批准业务差异；所有业务卡按 `696a12b0ffb8aa21f7d5dee841a65cecd78be9f7` 等价迁移。

## 5. 构建与用户门

本次只改文档，没有 Java writer，因此未运行 Maven、测试、runtime、server、Task、UI、capture 或 input。
后续 DHXY cohort 使用 `mvn -q -DskipTests compile`；Cloud source cohort 使用 `mvn -q clean compile`。最终
`mvn -q clean package` 会被 enforcer 要求运行现有测试，默认 no-local-test 下必须在执行前取得用户对该次
package/test run 的明确授权，不能改 POM 绕过或伪造成功。

## 6. 父级结论

权威计划第 14..18 节现已覆盖每张卡的状态、直接依赖、唯一写集、互斥 owner、验收边界、并行波次和删旧顺序；
不存在已知依赖环或同写集并发安排。计划审查通过，但 implementation 继续暂停，直到用户确认按修订版发出
首波 `TURN-02R + TURN-40A`。
