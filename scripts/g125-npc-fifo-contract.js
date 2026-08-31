#!/usr/bin/env node
/*
 * G125 source contracts: NPC 到达 FIFO 的两处结构性修复不得被静默删除.
 *
 * 事故（2026-08-30，角色火鸡味锅巴，队长 hwnd-1B10DC4）：
 *   修 A —— 20:29:58 云端一边发布 FIFO 开会话动作、一边翻页去 maintenance check，生产者与
 *           INSPECT 互相等死；客户端 FIFO 收 WAIT 只睡 500ms 再问、无总时限、零日志，turn 线程
 *           被扣 13.5 分钟，队长全部取图命令 TIMED_OUT_UNCERTAIN，整跑 FAILED。
 *   修 C —— 医宝宝三连（20:12/20:16/20:19）：demand 登记成功但该程零停稳帧，会话永远建不成，
 *           强制开会话拒开 session-missing，任务当 exhausted 跳过，每轮白跑一趟巫医。
 *
 * 客户端执行器依赖重、不宜搭 Java 脚手架（g118 复审既定裁定），用轻量源码断言钉死接线；
 * 云端 store 行为已有 G125NpcArrivalFirstFrameContractTest（8 条 Java 合同）直测。
 *
 * Run: node scripts/g125-npc-fifo-contract.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const CLIENT = path.resolve(__dirname, '..');
const CLOUD = path.resolve(CLIENT, '..', 'dhxy-cloud-brain');

function read(p) { return fs.readFileSync(p, 'utf8'); }
function stripComments(src) {
  return src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/(^|[^:])\/\/.*$/gm, '$1');
}

const EXEC = path.join(
  CLIENT, 'src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java');
const execRaw = read(EXEC);
const exec = stripComments(execRaw);

const STORE = path.join(
  CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java');
const store = stripComments(read(STORE));

let passed = 0, failed = 0;
function check(name, ok, detail) {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
}

// ── 修 A：会话静默总时限 ──────────────────────────────────────────────────────
check('A1 quiet limit constant exists (60s)',
  /private static final long SESSION_QUIET_LIMIT_MS = 60_000L;/.test(exec));

// 到点必须：WARN 留痕 + 上报 FINAL_FAILED + 交还 turn 线程（terminal）。三者缺一，
// 下次死锁要么无声、要么云端不知情、要么线程还是人质。
check('A2 quiet expiry logs a WARN with identities',
  execRaw.includes('NPC arrival FIFO quiet limit exceeded; releasing turn thread'));
check('A3 quiet expiry reports FINAL_FAILED to cloud',
  /quietMs > SESSION_QUIET_LIMIT_MS[\s\S]{0,900}?FINAL_FAILED[\s\S]{0,300}?return SessionResult\.terminal\(\);/.test(exec));

// 静默计时必须被"当前会话的非 WAIT 消息"重置——否则一次长识别链会被误杀。
check('A4 quiet clock resets when a real message arrives',
  /quietSinceMs = System\.currentTimeMillis\(\);\s*\n\s*log\.info\("NPC arrival FIFO message received/.test(exec));

// 检查点必须在轮询循环内部（while 之后），否则只在进场量一次等于没有。
{
  const loop = exec.indexOf('while (candidateMessageCount < CANDIDATE_LIMIT)');
  const gate = exec.indexOf('quietMs > SESSION_QUIET_LIMIT_MS');
  check('A5 quiet gate sits inside the poll loop', loop >= 0 && gate > loop,
    `loopAt=${loop} gateAt=${gate}`);
}

// ── 修 C：拒开补首帧重试 ──────────────────────────────────────────────────────
check('C1 session-missing rejection helper matches the cloud reason string',
  /isSessionMissingRejection[\s\S]{0,400}?contains\("session-missing"\)/.test(exec));

// 重试仅限第一次尝试 + 必须先补帧成功才 continue —— 防无界重开。
check('C2 retry is gated on attempt 1 and a successful fresh-frame upload',
  /attempt == 1 && isSessionMissingRejection\(session\)\s*&& replaceWithFreshFrame\(arguments, spec, binding\)/.test(exec));
check('C3 retry leaves an attributable WARN',
  execRaw.includes('NPC arrival FIFO sent fresh first frame after session-missing; retrying open'));

// 补帧重试必须在拒开分支内、且落在既有 2-attempt for 循环里（continue 重开，不新增循环）。
{
  const rejected = exec.indexOf('NPC arrival FIFO open rejected');
  const retry = exec.indexOf('attempt == 1 && isSessionMissingRejection');
  const forLoop = exec.indexOf('for (int attempt = 1; attempt <= 2; attempt++)');
  check('C4 retry lives inside the rejection branch of the existing 2-attempt loop',
    forLoop >= 0 && rejected > forLoop && retry > rejected && retry - rejected < 2000,
    `forAt=${forLoop} rejectedAt=${rejected} retryAt=${retry}`);
}

// ── 云端接线（行为细节由 Java 合同覆盖，这里只钉存在性防整段删除）──────────────
check('S1 cloud store admits a first frame via the replacement channel',
  /boolean firstFrameViaReplacement = replacement && demand\.preparedAttempts == 0;/.test(store));
check('S2 first-frame path arms the true-replacement chain (arrivedUnlocked)',
  /firstFrameViaReplacement\)\s*\{[\s\S]{0,600}?demand\.arrivedUnlocked = true;/.test(store));
check('S3 first-frame session creation leaves an attributable log',
  read(STORE).includes('G125 arrival session created from client first fresh frame'));

// Java 合同必须在版本控制里可见（.gitignore 白名单已五次踩坑，判据=check-ignore 退出码，
// 此处等价断言：白名单行存在 + 文件在盘）。
const JAVA_CONTRACT = path.join(
  CLOUD, 'src/test/java/com/yueyunfe/dhxy/cloudbrain/G125NpcArrivalFirstFrameContractTest.java');
check('S4 cloud java contract file is present on disk', fs.existsSync(JAVA_CONTRACT));
check('S5 cloud java contract is whitelisted in cloud .gitignore',
  read(path.join(CLOUD, '.gitignore')).includes(
    '!src/test/java/com/yueyunfe/dhxy/cloudbrain/G125NpcArrivalFirstFrameContractTest.java'));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
