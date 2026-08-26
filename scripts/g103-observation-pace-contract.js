// G103-CR 合同（2026-08-25）：观察循环起搏纪律的源码级硬禁令。
// 覆盖退修单三项：快速回包不得驱动采样、interest 加速必须即时唤醒、周期末 wake 后按绝对
// deadline 收缩等待；附带两条写盘热点禁令（ImageFinder 滚动池 / MatchEvidenceStore latest 周期重写）。
// paceWaitMs / shouldWakeForAppliedInterests 的数值语义由 JUnit
// WindowObservationPaceContractTest 覆盖，本脚本只锁结构。
const fs = require('fs');
const path = require('path');

const clientRoot = path.resolve(__dirname, '..');
const read = relative => fs.readFileSync(path.join(clientRoot, relative), 'utf8');
const checks = [];
const check = (name, condition) => {
  checks.push({name, condition});
  console.log(`${condition ? 'PASS' : 'FAIL'} ${name}`);
};

const runner = read('src/main/java/com/bot/dhxy/window/observation/WindowObservationRunner.java');
const sampler = read('src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java');
const imageFinder = read('src/main/java/com/bot/dhxy/core/ImageFinder.java');
const evidenceStore = read('src/main/java/com/bot/dhxy/core/MatchEvidenceStore.java');

// ① 快速回包合同：transport 线程收尾（finally 块）不得无条件唤醒采样循环。
// 注释里允许提及该方法名（事故说明），因此先剥掉注释再查可执行代码。
const stripComments = source => source
  .replace(/\/\*[\s\S]*?\*\//g, '')
  .replace(/\/\/[^\n]*/g, '');
const transportFinally = stripComments(runner.slice(
  runner.indexOf('private void startTransportCycle()'),
  runner.indexOf('"dhxy-observe-transport-" + windowId')));
check('transport cycle completion never wakes the sampling loop unconditionally (fast responses cannot drive capture rate)',
  transportFinally.includes('transportCycleRunning.set(false)')
    && !transportFinally.includes('wakeForLocalStateChange()')
    && /followupAfterTransport\.compareAndSet\(true, false\)\) \{\s*wakePending = true;/.test(transportFinally));

// ①b wake 竞态合同（CR3 P1）：transport 期间到达的定点唤醒必须置 followupAfterTransport，
// 由 finally 在清 running 之后有条件补一次唤醒（上一条已断言），不得丢失也不得变成无条件唤醒。
check('followup check/set and clear/consume share the pacer synchronization boundary (atomic, no stale flag)',
  /synchronized \(pacer\) \{\s*if \(transportCycleRunning\.get\(\)\) \{\s*followupAfterTransport\.set\(true\);\s*\}\s*wakePending = true;/.test(stripComments(runner))
    && /synchronized \(pacer\) \{\s*transportCycleRunning\.set\(false\);\s*if \(followupAfterTransport\.compareAndSet\(true, false\)\)/.test(stripComments(runner)));

// ② interest 加速合同（CR2/CR3 收窄）：只有【真实有效节奏】（含 parked/寻路车道/MIN 钳制）变快才唤醒。
check('only effective-cadence acceleration (parked/pathing-lane/MIN-clamp aware) triggers a targeted wake',
  /shouldWakeForAppliedInterests\(previousInterests, interests,\s*parkedHeartbeatPeriodMs, localLanePeriodMs\(\)\)\)\s*\{\s*wakeForLocalStateChange\(\);/.test(runner)
    && /effectiveCadenceMs\(appliedInterests, parkedHeartbeatPeriodMs, localLanePeriodMs\)\s*< effectiveCadenceMs\(previousInterests, parkedHeartbeatPeriodMs, localLanePeriodMs\)/.test(runner)
    && /return effectiveCadenceMs\(interests, parkedHeartbeatPeriodMs, localLanePeriodMs\(\)\);/.test(runner));

// ②b prepared demand 收窄合同（CR2）：同一 demand 反复出现不唤醒，只有新到/换代才唤醒。
check('only a new or regenerated prepared-frame demand triggers a targeted wake',
  /isNewPreparedFrameDemand\(previousDemand, nextDemand\)\)\s*\{[\s\S]{0,700}?wakeForLocalStateChange\(\);/.test(runner)
    && !/if \(nextDemand != null\) \{[\s\S]{0,900}?wakeForLocalStateChange\(\);/.test(stripComments(runner)));

// ③ 绝对 deadline 合同：pace 的等待时长来自 paceWaitMs（上次捕获+周期），不是固定整周期。
check('pace waits toward the absolute capture deadline on a monotonic clock, not a fixed full period',
  runner.includes('static long paceWaitMs(')
    && /paceWaitMs\(periodMs, sampler\.sharedCycleFrameCapturedAtMs\(\),\s*WindowObservationSampler\.monotonicMillis\(\)\)/.test(runner)
    && sampler.includes('static long monotonicMillis()')
    && sampler.includes('sharedCycleFrameCapturedAtMs = monotonicMillis();')
    && /monotonicMillis\(\) - sharedCycleFrameCapturedAtMs < periodMs\) \{\s*return;\s*\}/.test(sampler)
    && /if \(waitMs <= 0L\) \{[\s\S]{0,200}?return;/.test(runner));

// ③c 双锚合同（CR3）：pace 等待取"捕获 deadline"与"parked 心跳 deadline（上次成功发送锚）"较早者，
// 防止捕获锚 tick 恒早于心跳判据几十毫秒被 skip-send 拦下、把 parked 心跳拖向 2×周期。
check('pace heartbeat deadline: excluded while transport is in flight, anchored to max(success, failure) after completion',
  runner.includes('static long heartbeatWaitMs(')
    && /if \(!transportCycleRunning\.get\(\)\) \{\s*long completionAnchorMs = Math\.max\(lastSuccessfulSendAtMs, lastTransportFailureAtMs\);\s*waitMs = Math\.min\(waitMs,\s*heartbeatWaitMs\(parkedHeartbeatPeriodMs, completionAnchorMs, System\.currentTimeMillis\(\)\)\);/.test(runner));

// ⑤b 发送门锚（CR6 P1）：sendOnce 的 heartbeatDue 必须锚 max(成功, 失败)——pace 锚管不住
// tick 与 transport 完成的时序竞态（实测失败→下一请求 15-17ms 连发），backoff 门要在 client.send 之前。
check('the send gate itself anchors on max(lastSuccessfulSendAtMs, lastTransportFailureAtMs)',
  /long sendCompletionAnchorMs = Math\.max\(lastSuccessfulSendAtMs, lastTransportFailureAtMs\);\s*boolean heartbeatDue = sendCompletionAnchorMs <= 0L\s*\|\| nowMs - sendCompletionAnchorMs >= parkedHeartbeatPeriodMs;/.test(runner)
    && !/boolean heartbeatDue = lastSuccessfulSendAtMs <= 0L/.test(stripComments(runner)));

// ⑥ 低 acceptedSeq 禁令（CR5）：不覆盖请求的回包不得触发即时唤醒重试，重试归节奏/失败锚 deadline。
check('an uncovered (low acceptedObserverSeq) response never wakes an immediate retry',
  (() => {
    const src = stripComments(runner);
    const at = src.indexOf('Observation response did not cover request');
    if (at < 0) return false;
    const slice = src.slice(at, at + 900);
    return slice.includes('return;') && !slice.includes('wakeForLocalStateChange()');
  })());

// ⑦ WUBEI 车道统一（CR5）：currentPeriodMs 不再 early-return，五倍/寻路都进 localLanePeriodMs。
check('WUBEI 100ms lane is unified into localLanePeriodMs/effectiveCadenceMs (no early-return)',
  /lane = WindowObservationSampler\.WUBEI_PREPARE_PERIOD_MS;/.test(runner)
    && /lane = Math\.min\(lane, WindowObservationSampler\.LOCAL_PATHING_SAMPLE_PERIOD_MS\);/.test(runner)
    && !/return WindowObservationSampler\.WUBEI_PREPARE_PERIOD_MS;\s*\}\s*return effectiveCadenceMs/.test(stripComments(runner)));

// ③b 共享帧周期门合同：周期内的额外 tick 复用现帧且跳过位置条带刷新。
check('shared cycle frame is period-gated and within-period ticks skip the position strip refresh',
  sampler.includes('setEffectiveSharedFramePeriodMs')
    && /monotonicMillis\(\) - sharedCycleFrameCapturedAtMs < periodMs\) \{\s*return;\s*\}\s*\n\s*refreshSharedCycleFrame\(\);/.test(sampler)
    && runner.includes('sampler.setEffectiveSharedFramePeriodMs(currentPeriodMs())'));

// ④ 写盘热点禁令：ImageFinder 滚动池默认关闭（同步写在感知线程上，曾 126-200 张/分）。
check('ImageFinder rolling evidence pool is debug-gated (dhxy.matchEvidence.rollingPool)',
  imageFinder.includes('EVIDENCE_ROLLING_POOL_ENABLED')
    && /if \(!EVIDENCE_ROLLING_POOL_ENABLED\) \{\s*return;\s*\}/.test(imageFinder));

// ⑤ 写盘热点禁令：MatchEvidenceStore latest 周期重写默认关闭（翻转/决策点存档保留）。
check('MatchEvidenceStore periodic latest rewrite is debug-gated while flip/decision saves stay on',
  evidenceStore.includes('LATEST_PERIODIC_REFRESH_ENABLED')
    && /if \(!LATEST_PERIODIC_REFRESH_ENABLED\) \{\s*return;\s*\}/.test(evidenceStore)
    && evidenceStore.includes('boolean writeStamped = !throttled || flipped;'));

const passed = checks.filter(c => c.condition).length;
console.log(`${passed}/${checks.length} PASS`);
if (passed !== checks.length) {
  process.exit(1);
}
