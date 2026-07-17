# CR271 TURN-28S1 independent delivery review R1

## Conclusion

**APPROVED**

- P0: 0
- P1: 0
- P2: 0

本结论仅批准 TURN-28S1 的一文件源码切片，不代表 TURN-28 整卡、named test、integration、compile 或 build 已通过。

## Independent scope

- Current: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\NpcClickService.java`
- Authority baseline: `D:\mavenProject\dhxy-cloud-brain\migration-baseline\696a12b0\src\main\java\com\bot\dhxy\service\NpcClickService.java`
- Contract: TURN-28 / TURN-28S1 fixed cards、权威计划 14-19、HTTPS turn 协议及 `docs/业务逻辑.md`。
- 审查未采用 External B 自述或父级结论作为源码事实；以下结论来自当前文件与 authority baseline 的直接比较及当前调用链检查。

## Exact evidence

1. **整文件与 authority baseline 逐字节一致。** 两文件均为 `3374` 行、`175367` bytes，SHA-256 均为 `cce8f0203ac90a0d39f7cff99dda8d9a616656a55467ed4ae3aa053ad0923441`；独立 byte sequence comparison 为 `True`。因此当前交付相对 `696a12b0` 没有格式化、无关清理、替代门或其它行为漂移。

2. **未批准的 pending `sourceTask` gate 已不存在。** 当前 `PendingSmartClickEvidence` 字段及构造接线位于 `NpcClickService.java:2056-2124`，其中没有 `sourceTask`；全文件 `matchesSourceTask`、`normalizeSourceTask`、`java.util.Locale` 均为 `0` 次。`confirmExpectedOptionProof(String sourceTask, ...)` public 签名仍在 `:2271-2276`，`sourceTask` 仅保留为 proof 检查后的诊断字段。

3. **request-level Wubei `sourceTask` 业务分支未变。** 当前与 baseline 均有 `request.sourceTask()` `16` 次、`TaskType.WUBEI` `3` 次：`:810-814` 排除 Wubei early-memory，`:848-854` 保留 Wubei tooltip-first，`:893-896` 仅让非 Wubei 进入后置 normal-tooltip。三处分支与 baseline 字节相同。

4. **pending 与 proof fences 保持 baseline 原样。** pending 仍由请求 map/name/coords、click evidence 与当前 window base 构造（`:1300-1356`, `:2106-2124`）；map/name/coords exact match 保留（`:2141-2145`）；proof token exact match 保留（`:2157-2160`）；expected action/text/template proof 保留（`:2148-2155`）。

5. **window、mismatch-removal 与 confirmed-memory fences 未弱化。** pending 仍按当前 `WindowRuntimeContext.windowId` 取 key（`:1351-1353`, `:2344-2347`）。显式确认的 map/name/coords mismatch 仍先移除 pending（`:2253-2267`）；expected-option 路径仍依次执行 current-window key、proof-token、option proof，option mismatch 时移除，全部通过后才移除并记录 confirmed memory（`:2277-2294`）。上述区域与 authority baseline 整文件字节一致。

## Review boundary

- 未发现 P0/P1/P2。
- 无已批准业务差异；该切片把 pending-proof 条件精确恢复为 `696a12b0`。
- 按用户禁令未运行 Maven/JUnit/compile/package、runtime/application/server/Task/UI/capture/input，未执行 Git mutation，未修改 Java、原卡或其它文档。

TRUE_EOF
