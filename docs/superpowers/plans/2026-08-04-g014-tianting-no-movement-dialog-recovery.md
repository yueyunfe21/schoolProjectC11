# G014 天庭绿链未移动后的 Dialog 恢复

## 状态

`SOURCE+ISOLATED-TEST REVIEW PASSED / P0-P1-P2=0-0-0 / 待 fresh runtime`

## 基线与工作树

- Client：`D:\mavenProject\DHXY-cr271`，`thin-client-design@2f083c14`，与
  `origin/thin-client-design` 同点。
- Cloud：`D:\mavenProject\dhxy-cloud-brain`，`navigation-migration@363d0e3`，与
  `origin/navigation-migration` 同点。
- 两仓开工前已有大量用户及其他 G 卡的 dirty/untracked 改动。本卡只做下列窄写集，不重置、不回退、
  不整文件覆盖。

## 问题定义

天庭绿链点击完成不等于移动已经开始。当前代码在 exact 点击未产生 pathing intent 时，只把角色视为
`stopped`，继续等待原来仅含四张战斗选项的 probe；`引妖/接任务/封妖` 等已知 option 可以挡在画面上，
但该 probe 永远不会认出。等待结束后 Cloud 才走通用 fallback 或再次点击绿链，形成长停顿、重复点击，
甚至错误进入暗雷/回李靖恢复。

## 冻结业务顺序

```text
Cloud 点击 fresh Tracker 绿链，并登记 exact pathing intent
  -> Runner 证明开始移动：保持现有 PARK_PATHING
  -> Runner 在有界窗口内未登记该 intent：切换本地七模板恢复探针
       -> 本地命中 + fresh 复验 + 点击 + retained 事件
            kaida                 -> 等 Runner IN_COMBAT；自动战斗计数已重置
            duoxie                -> 既有本地 fengyao 短窗
            fengyao               -> 既有封妖符坐标分支
            zhuoyue / yaowang     -> 等 Runner IN_COMBAT
            yinyao                -> 引妖恢复语义，不得按默认进战处理
            accept                -> 接任务恢复语义，不得按默认进战处理
       -> 有界本地窗口全部 miss：Cloud 现有 DialogService 第一绿 fallback
            -> fallback 有动作：重读 Runner/Tracker/Dialog 事实
            -> fallback 无动作：才允许重按 Tracker 绿链
```

## 写集

### Client

- `src/main/java/com/bot/dhxy/model/dialog/DialogOperation.java`
- `src/main/java/com/bot/dhxy/window/observation/TiantingDialogLocalMechanics.java`
- `src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java`
- 对应的共享协议镜像（仅在新增 operation 需要时）
- `TiantingDialogLocalMechanicsTest`、`TiantingDialogProbeContractTest`

### Cloud

- `src/main/java/com/bot/dhxy/task/tianting/TiantingTask.java`
- 对应的共享 operation/protocol 镜像
- 天庭 subtask/summary/连通性合同测试

### 文档

- `docs/PACKAGE_ARCHITECTURE.md`
- `docs/ACTIVE_WORK.md`
- `docs/superpowers/specs/2026-07-28-tianting-task-design.md`
- `docs/cr-dashboard-data.js`

## 明确禁止

- 不把 Client Runner 变成业务大脑；本地只识别模板、复验并执行已冻结的模板点击。
- 不改变模板图片、ROI、阈值、候选点击坐标或物理输入原子性。
- 不让移动中的普通 leg 扫描 `accept/yinyao/fengyao`；七模板集合只在 exact 绿链未启动移动后启用。
- 不新增常驻 Cloud 截图请求；all-miss 才进入一次现有 fallback。
- 不把 `zhuoyue/yaowang` 编造成新的 Cloud phase；它们的后续事实是 Runner `IN_COMBAT`。

## 验收门禁

1. 七张模板分别在恢复集合中命中自己，原四张 resident 集合的边界保持不变。
2. 绿链真实启动移动时不得安装恢复探针；未启动移动时必须安装且能经生产 observation event 上报。
3. `kaida/duoxie/fengyao/zhuoyue/yaowang/yinyao/accept` 七种事件都有明确 Cloud 去向；
   `zhuoyue/yaowang` 后续只等 Runner `IN_COMBAT`。
4. 本地有已知命中时不得调用 Cloud fallback；本地有界窗口全部 miss 后 fallback 至多一次，fallback
   无动作才重按绿链。
5. 运行 Client/Cloud 定向测试和生产 compile；共享协议文件保持 byte-identical。
6. Fresh runtime 观察日志顺序必须为 `tracker click -> movement proof`；只有 negative 才出现
   `recovery probe`，随后是准确模板事件或一次 fallback，不能同时出现旧重按链。

## 父级终审结果

- Client：`TiantingDialogLocalMechanicsTest` `6/6`、`TiantingDialogProbeContractTest` `9/9`。
- Cloud：`TiantingSubtaskLoopContractTest` `12/12`。
- 两仓生产 `compile` 通过；共享 observation operation 文件 byte-identical，SHA-256 为
  `DEAE51956D32E4D3CC9B8932A3429BCAB4AA3E3ECB06D400880F703114362712`。
- 全仓 `testCompile` 有本卡外既有断点，测试按定向 `javac + surefire:test` 隔离运行；不能据此宣称全仓
  tests 通过。
- 源码审核结论：`P0/P1/P2=0/0/0`。只剩 fresh runtime 门，不启动游戏或物理输入代替用户验收。
