const fs = require('fs');
const path = require('path');

const clientRoot = path.resolve(__dirname, '..');
const cloudRoot = path.resolve(clientRoot, '..', 'dhxy-cloud-brain');
const read = (root, relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const checks = [];
const check = (name, condition) => {
  checks.push({name, condition});
  console.log(`${condition ? 'PASS' : 'FAIL'} ${name}`);
};

const observer = read(cloudRoot,
  'src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java');
const task = read(cloudRoot,
  'src/main/java/com/bot/dhxy/task/wuhuan/v3/FiveRingTaskV3.java');
const fifo = read(clientRoot,
  'src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java');
const sampler = read(clientRoot,
  'src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java');

check('G017 contains no fake Wuhuan delegation outcomes',
  !observer.includes('DELEGATED_WUHUAN_')
    && observer.includes('ACKNOWLEDGED_WUHUAN_STARTUP'));
check('FiveRing accept phase skips NPC click for an existing exact dialog fact',
  task.includes('boolean dialogAlreadyOpen = latestFreshCurrentWuhuanDialogPresence(context)')
    && task.includes('&& !dialogAlreadyOpen)')
    && task.includes('interactAcceptDialog(context, current)'));
// 复查第 4 项修正（2026-08-24）：defer 五环点击后的真实控制流是
//   verify(..., defer=true) 立即返回 DEFERRED_TO_TASK（verified=true）
//   → FIFO 以 VERIFIED 结束会话、不取第二候选
//   → 任务侧以统一 structural presence（DialogFramePresenceMechanics）武装
//   → present 后 Cloud demand 完整帧做内容分类。
// DIALOG_OPEN_UNVERIFIED 只是非 defer 路径的兼容出口，不再当作 defer 交接的证据。
const dialogSvcForDefer = read(clientRoot, 'src/main/java/com/bot/dhxy/service/DialogService.java');
check('defer click ends the FIFO session as VERIFIED without a second candidate',
  dialogSvcForDefer.includes('"DEFERRED_TO_TASK"')
    && fifo.includes('spec.deferDialogVerificationToTask()')
    && fifo.includes('localOutcome == NpcClickSmartQueueOutcome.VERIFIED')
    && fifo.includes('return SessionResult.verified();'));
check('task side arms the unified structural presence after the click',
  read(clientRoot, 'src/main/java/com/bot/dhxy/window/observation/WuhuanPresenceLocalMechanics.java')
    .includes('new DialogFramePresenceMechanics()'));
check('terminal-frame capture has no automatic UI cleanup hook',
  !sampler.includes('ensureCleanSceneBeforeTerminalFrame')
    && !sampler.includes('Terminal frame scene probe'));

// G102 收口硬禁令（2026-08-24 review P1）：
// 1) FIFO 严禁把 defer 硬编码回 false —— 3465 事故根因（五环被踹回旧方差门判定）。
//    验证调用必须逐参传 spec.deferDialogVerificationToTask()。
const fifoVerifyCalls = fifo.split('verifyNpcArrivalExpectedDialog(').slice(1);
check('FIFO never hardcodes defer=false into verifyNpcArrivalExpectedDialog',
  fifoVerifyCalls.length > 0 && fifoVerifyCalls.every((tail) => {
    const args = tail.slice(0, 300);
    return args.includes('spec.deferDialogVerificationToTask()')
      && !/,\s*false\s*,/.test(args.split('verificationSource')[0] ?? args);
  }));

// 2) 旧的第二套 Dialog presence 判定（方差门）严禁在生产代码重现。
const dialogService = read(clientRoot, 'src/main/java/com/bot/dhxy/service/DialogService.java');
const preprocessor = read(clientRoot, 'src/main/java/com/bot/dhxy/tools/ImagePreprocessor.java');
check('legacy variance-gate dialog presence is deleted and stays deleted',
  !dialogService.includes('private boolean isOptionDialog(')
    && !dialogService.includes('public Boolean probeOptionDialogPresent(')
    && !preprocessor.includes('public static double getImageStandardDeviation(')
    && !sampler.includes('maintainScenePresenceCache')
    && !sampler.includes('scenePresenceDialog'));

// 3) 唯一 presence 判定 = DialogFramePresenceMechanics。
check('DialogService presence checks route through DialogFramePresenceMechanics',
  dialogService.includes('DialogFramePresenceMechanics')
    && dialogService.split('dialogFramePresence.isPresent(').length >= 3);

const failed = checks.filter((item) => !item.condition);
console.log(`${checks.length - failed.length}/${checks.length} PASS`);
if (failed.length > 0) {
  process.exitCode = 1;
}
