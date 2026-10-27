# G176 五倍意外退战续接：待授权合同规格

这是非可执行的验证规格；用户尚未授权本卡测试，不创建或运行自动测试、抓图或游戏输入。编译不代替这些断言。

- 生产Runner unexpected entry -> observation DTO -> coordinator保留source -> exact ready state -> 五倍清理确认 -> exact exit -> 任务仍在 -> 查香一次 -> 归队完成 -> 原任务绿链。任意metadata丢失/consumer被绕过应失败；完成计数不增加、无回城输入。
- entry重试不重复初始化；entry后exit已先到也能续接；新run/新轮/失败恢复不带旧分类、中断点、归队续接状态。
- 同轮连续偶遇战；在归队/重接路上遇怪；黄袍目标战和显形镜点位进度不受影响；死亡仍等待归队后重接。不是用两台窗的图凑新鲜性。
- title消失才延迟维护后回程；现查与延迟查不能各一次；一场exit不重复查香。
- USED_START_MAP_UNVERIFIED且非死亡 -> FAILED -> 清理/有界恢复/ROUTE_TO_MAIN_TASK；无armReturnHomeReplay或等待不存在的退战。死亡仍走原死亡出口，找到道具/地图验证的原边界不变。

后续经用户授权须使用真实生产接线/状态机驱动，不能只搜字符串或预置最终分类；现无PASS结论。
