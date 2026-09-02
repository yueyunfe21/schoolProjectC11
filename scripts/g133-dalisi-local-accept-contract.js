#!/usr/bin/env node
/*
 * G133 source contracts（用户拍板 2026-09-01）: 大理寺答题入口照其他任务的本地匹配管线做.
 *
 * 事故=2026-09-01 00:00 五窗点开 NPC 后全部卡死:旧入口靠云端快照证明 accept.png,而 G113 原子
 * 快照的像素 ROI(unknown-phase-dialog-frame)在收件箱通用门前永远进不来 → 探针永远 "not fresh".
 * 修复=①入口走鬼王同款本地探针(布防 DALISI_QUIZ_ACCEPT→客户端配 hints/accept.png→本地点击→
 * DALISI_DIALOG_ANSWERED 唤醒任务);②收件箱给快照像素 ROI 开伴生入账通道(答题读题面仍需快照).
 *
 * Run: node scripts/g133-dalisi-local-accept-contract.js
 */
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const CLIENT = path.resolve(__dirname, '..');
const CLOUD = path.resolve(CLIENT, '..', 'dhxy-cloud-brain');
const read = p => fs.readFileSync(p, 'utf8');
const sha = p => crypto.createHash('sha1').update(fs.readFileSync(p)).digest('hex');

let passed = 0, failed = 0;
const check = (name, ok, detail) => {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
};

// ── 协议：两仓字节一致 + 新值都在 ────────────────────────────────────────────
for (const f of ['ObservationDialogOperation.java', 'ObservationKeyEventType.java']) {
  const rel = 'src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/' + f;
  check(`P byte-identical protocol: ${f}`,
    sha(path.join(CLIENT, rel)) === sha(path.join(CLOUD, rel)));
}
check('P new enum values present',
  read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationDialogOperation.java')).includes('DALISI_QUIZ_ACCEPT')
  && read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationKeyEventType.java')).includes('DALISI_DIALOG_CLICKED'));

// ── 客户端管线 ────────────────────────────────────────────────────────────────
const mech = read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/window/observation/TiantingDialogLocalMechanics.java'));
check('C1 accept template + action key + map entry + matcher',
  mech.includes('images/template/dialog/dalisi/hints/accept.png')
  && mech.includes('ACTION_DALISI_ACCEPT_QUIZ = "dalisi.acceptQuiz"')
  && mech.includes('Map.entry(DALISI_ACCEPT, ACTION_DALISI_ACCEPT_QUIZ)')
  && /matchDalisiAcceptOption\(BufferedImage roi\) \{\s*return matchFirstOf\(roi, List\.of\(DALISI_ACCEPT\)\);/.test(mech));
check('C1b accept.png exists on disk',
  fs.existsSync(path.join(CLIENT, 'images/template/dialog/dalisi/hints/accept.png')));

const smp = read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java'));
check('C2 option set wired (enum entry + supports gate + match case)',
  smp.includes('DALISI_ACCEPT(DialogOperation.DALISI_QUIZ_ACCEPT)')
  && smp.includes('case DALISI_ACCEPT -> taskType == TaskType.DALISI_QUIZ;')
  && smp.includes('case DALISI_ACCEPT -> TiantingDialogLocalMechanics.matchDalisiAcceptOption(roi);'));
check('C3 answered edge publishes DALISI_DIALOG_CLICKED on both success and failure paths',
  (smp.match(/ObservationKeyEventType\.DALISI_DIALOG_CLICKED/g) || []).length === 2
  && (smp.match(/"dalisi-dialog-option"/g) || []).length === 2);

// ── 云端映射 + 任务侧 ─────────────────────────────────────────────────────────
const obs = read(path.join(CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java'));
check('S1 observer maps DALISI_DIALOG_CLICKED -> DALISI_DIALOG_ANSWERED (taskType DALISI_QUIZ)',
  /DALISI_DIALOG_CLICKED[\s\S]{0,800}?WindowReadyEventType\.DALISI_DIALOG_ANSWERED[\s\S]{0,200}?TaskType\.DALISI_QUIZ/.test(obs));
check('S2 ready event enum carries DALISI_DIALOG_ANSWERED',
  read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/window/model/WindowReadyEventType.java')).includes('DALISI_DIALOG_ANSWERED'));

const dq = read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/task/dalisi/DalisiQuizTask.java'));
check('S3 entry arms the probe BEFORE the NPC click and clears it on every exit',
  /armEntryProbe\(context, "dalisi:entry-probe:" \+ attempt\)/.test(dq)
  && dq.includes('clearEntryProbe(context, "dalisi:entry-npc-click-failed")')
  && dq.includes('clearEntryProbe(context, "dalisi:entry-answer-consumed")'));
check('S4 entry waits on DALISI_DIALOG_ANSWERED with a bound, gated on executed=true',
  /awaitNewer\(context,[\s\S]{0,200}?DALISI_DIALOG_ANSWERED\),\s*entryProbeAfterSequence, ENTRY_ANSWER_WAIT_MS\)/.test(dq)
  && dq.includes('.contains("executed=true")'));
check('S5 the dead snapshot entry path is no longer consulted for entry',
  !/awaitOfficialAcceptEntry\(context, acceptTemplate\)/.test(dq));

// ── 收件箱伴生通道（答题读题面）──────────────────────────────────────────────
const inbox = read(path.join(CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudWindowObservationInbox.java'));
check('I1 unknown-phase dialog-frame ROI has its companion ingestion lane',
  /"unknown-phase-dialog-frame"\.equals\(roi\.roiKey\(\)\)[\s\S]{0,300}?"unknown-phase-dialog-presence"\.equals\(interest\.interestKey\(\)\)/.test(inbox)
  && inbox.includes('&& !unknownPhaseDialogFrame) {'));
check('I2 lane is geometry-pinned to the published (200,250,640,300) crop',
  /roi\.left\(\) == 200 && roi\.top\(\) == 250\s*&& roi\.width\(\) == 640 && roi\.height\(\) == 300/.test(inbox));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
