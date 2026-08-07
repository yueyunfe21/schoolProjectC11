# G009 修罗看打本地探测同步证据写盘阻塞诊断

状态：`DELIVERED`

## 现场

- worktree：`D:\mavenProject\DHXY-cr271`；本卡不触碰受保护根仓 `D:\mavenProject\DHXY`。
- 2026-08-03 `22:49`，修罗队长 `hwnd-10523A6`（火鸡味锅巴°，ID `443075411`）已于 `22:49:17.339` 到达大雁塔四层。
- `22:49:18.946`，`WindowObservationSampler.sampleXiuluoLocalKanda(...)` 开始本地看打匹配；直到用户 `22:49:36.580` 暂停前，没有任何命中、重验证、入队点击或进入战斗事件。
- 该次 probe 的最后证据文件直到 `22:49:44-46` 才落盘：`images/temp/hwnd-10523A6/local_kanda_xiuluo_v2_latest_{roi,template_used,raw,marked}.png`。标记图显示模板候选分仅 `0.0217`，当时帧中没有可命中的“看打”模板。

## 根因

`DialogService.findTaskEnterBattleLocalTemplate(...)` 在调用 `ImageFinder.find(...)` 之前，先同步两次执行 `persistLocalEnterBattleProbeEvidence(...)`。该方法在观察线程内写入 ROI、模板、整窗 raw、marked 四张 PNG；整窗 raw/marked 均约 1 MB。此次从“probe matching”到第一个证据文件完成写入约 25 秒，因此毫秒级本地看打循环被诊断证据 I/O 阻塞，尚未进入真正的模板匹配或点击阶段。

## 已批准的修复边界

用户已明确确认该本地看打路径测试完成，不再需要保存 raw/ROI/template/marked 调试 PNG。

1. 删除 `DialogService.findTaskEnterBattleLocalTemplate(...)` 中两次
   `persistLocalEnterBattleProbeEvidence(...)` 调用及其专用落盘实现。
2. 不改变看打模板、ROI、阈值 `0.82`、两次本地匹配、命中后的 fresh revalidation、点击顺序或 Cloud fallback 语义。
3. 不新增后台写盘或其他诊断链路；探测路径只执行既有本地内存裁剪、模板加载与匹配。
4. Java compile 为交付门；fresh runtime 由用户验证 dialog 出现后的点击及时性。

## 交付证据

- 已删除 `DialogService.findTaskEnterBattleLocalTemplate(...)` 中两次 probe PNG 持久化及其专用
  `persistLocalEnterBattleProbeEvidence(...)` 实现；同时删除仅用于标注 PNG 的无阈值第二次匹配。
- 最终热路径只保留一次 `ImageFinder.find(roi, template, 0.82)`，以及既有命中坐标计算、consume-time
  fresh revalidation 和输入链路。
- 编译门：`mvn -q -DskipTests compile`，2026-08-03 通过。
- 未运行本地测试或应用；用户已说明不需保留这段测试证据，下一次实机修罗 dialog 是 fresh 验收。
