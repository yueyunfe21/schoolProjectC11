#!/usr/bin/env node
/*
 * G129 source contracts: "只有 OPTION 框挡补给"的生产接线不得被静默删除.
 *
 * 事故（2026-08-31 14:14 引妖香）：脱战 +0.5s 天庭布防引妖香 OPTION 探针，+1.7s 自动战斗退出
 * 恢复腿的补给抢跑，点击全被模态框吃掉、三次重试烧光（first-aid ineffective after 3 attempts），
 * +11s 框才被答掉，法力低着进后续流程。用户拍板：只有 OPTION 挡补给，STORY 不拦。
 * 闸行为由 G129OptionDialogGateContractTest（6 条 Java 合同）直测；本合同钉接线。
 *
 * Run: node scripts/g129-supply-option-gate-contract.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const CLOUD = path.resolve(__dirname, '..', '..', 'dhxy-cloud-brain');
const read = p => fs.readFileSync(p, 'utf8');
const strip = s => s.replace(/\/\*[\s\S]*?\*\//g, '').replace(/(^|[^:])\/\/.*$/gm, '$1');

let passed = 0, failed = 0;
const check = (name, ok, detail) => {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
};

// ── 补给侧：三个执行入口全部先问闸 ───────────────────────────────────────────
const pss = strip(read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/service/PlayerStateService.java')));
{
  const defers = (pss.match(/deferSupplyWhileOptionDialogOnScreen\("/g) || []).length;
  check('P1 all three supply entries consult the gate (cached-plan / ordered-heal / single-target)',
    defers === 3
    && pss.includes('deferSupplyWhileOptionDialogOnScreen("cached-first-aid-plan")')
    && pss.includes('deferSupplyWhileOptionDialogOnScreen("ordered-heal")')
    && pss.includes('deferSupplyWhileOptionDialogOnScreen("single-target-supply")'),
    `found ${defers} call sites, expected 3`);
}
// 让路必须有界 + fail-open：到点 WARN 后照常执行，绝不因漏撤防绝食。
check('P2 defer is bounded and fails open on timeout',
  /OPTION_DIALOG_SUPPLY_DEFER_MAX_MS = 12_000L/.test(pss)
  && pss.includes('supply defer timed out; proceeding anyway'));
// 停止请求必须能打断等待。
check('P3 defer wait is stop-aware (TaskSleep gate)',
  /deferSupplyWhileOptionDialogOnScreen[\s\S]{0,1600}?TaskSleep\.sleep\(OPTION_DIALOG_SUPPLY_DEFER_POLL_MS\)/.test(pss));
// 让路与恢复都留痕。
check('P4 defer start and resume are both logged',
  pss.includes('supply deferred: option dialog on screen')
  && pss.includes('supply resumed after option dialog cleared'));

// ── 天庭侧：布防/撤防/答完 三个钩子 ─────────────────────────────────────────
const tt = strip(read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/task/tianting/TiantingTask.java')));
check('T1 probe install arms the gate',
  /dialogProbeInstalled = true;\s*OptionDialogOnScreenGate\.arm\(context\.getWindowId\(\), source\);/.test(tt));
check('T2 probe clear disarms the gate (idempotent, at method head)',
  /void clearDialogProbeInterest\(TaskExecutionContext context, String reason\) \{\s*OptionDialogOnScreenGate\.disarm\(context\.getWindowId\(\), reason\);/.test(tt));
check('T3 yinyao answered path disarms without waiting for clear',
  tt.includes('OptionDialogOnScreenGate.disarm(context.getWindowId(), "tianting:post-combat-yinyao-answered")'));

// ── 闸本体与合同可见性 ───────────────────────────────────────────────────────
const gatePath = path.join(CLOUD, 'src/main/java/com/bot/dhxy/service/dialog/OptionDialogOnScreenGate.java');
check('G1 gate class exists with the 30s self-heal expiry',
  fs.existsSync(gatePath) && /ARM_AUTO_EXPIRE_MS = 30_000L/.test(read(gatePath)));
check('G2 java contract present and whitelisted',
  fs.existsSync(path.join(CLOUD, 'src/test/java/com/bot/dhxy/service/dialog/G129OptionDialogGateContractTest.java'))
  && read(path.join(CLOUD, '.gitignore')).includes(
      '!src/test/java/com/bot/dhxy/service/dialog/G129OptionDialogGateContractTest.java'));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
