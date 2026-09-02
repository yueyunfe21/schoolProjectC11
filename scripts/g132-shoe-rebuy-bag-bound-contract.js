#!/usr/bin/env node
/*
 * G132 source contracts（用户拍板 2026-09-01）:
 * ① 五环V3买鞋:验证 1 次没验到=购买没成功,清残留UI后立刻重买;重买有总上限,到点热重启 PREPARE.
 *   事故=2026-09-01 00:00 两窗(468599906/468600378)买鞋失败后无限"翻包裹验证",永不重买.
 * ② 包裹翻页:已标定任务页时以它为最后一页,之后的页位不再盲点(实测此前每轮点到界外 page 5).
 *
 * Run: node scripts/g132-shoe-rebuy-bag-bound-contract.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const CLIENT = path.resolve(__dirname, '..');
const CLOUD = path.resolve(CLIENT, '..', 'dhxy-cloud-brain');
const read = p => fs.readFileSync(p, 'utf8');
const strip = s => s.replace(/\/\*[\s\S]*?\*\//g, '').replace(/(^|[^:])\/\/.*$/gm, '$1');

let passed = 0, failed = 0;
const check = (name, ok, detail) => {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
};

// ── ① 五环V3 ──────────────────────────────────────────────────────────────────
const v3 = strip(read(path.join(
  CLOUD, 'src/main/java/com/bot/dhxy/task/wuhuan/v3/FiveRingTaskV3.java')));

// 旧死循环形状必须消失:验证失败绝不再"只重试验证".
check('S1 the verify-only infinite loop is gone',
  !v3.includes('retry verification only'));
// 1 次没验到 → 清 pending → 清UI → 重买(retrySamePhase shoe-rebuy).
check('S2 one failed verification clears pending, cleans UI, and re-buys',
  /verifiedPage == null\) \{\s*shoePurchaseVerificationPending = false;\s*shoeRebuyRounds\+\+;/.test(v3)
  && v3.includes('cloudUiCleanerPort.cleanUpAll(TASK_CODE, "shoe-purchase-not-verified-rebuy")')
  && v3.includes('retrySamePhase("shoe-rebuy")'));
// 重买有总上限,到点热重启 PREPARE(与 runner-silent 同款出口),不无限买.
check('S3 re-buy is bounded and exits to PREPARE when exhausted',
  /SHOE_REBUY_MAX_ROUNDS = 3;/.test(v3)
  && /shoeRebuyRounds > SHOE_REBUY_MAX_ROUNDS[\s\S]{0,600}?FiveRingV3Phase\.PREPARE, "shoe-rebuy-exhausted"/.test(v3));
// 计数器在三处复位:验证成功×2 + 轮完成,防跨轮误累积.
{
  const resets = (v3.match(/shoeRebuyRounds = 0;/g) || []).length;
  check('S4 rebuy counter resets on success and round completion (>=3 sites incl. exhaustion)',
    resets >= 4, `found ${resets} resets`);
}

// ── ② BagService 翻页界 ───────────────────────────────────────────────────────
const bag = strip(read(path.join(
  CLIENT, 'src/main/java/com/bot/dhxy/service/BagService.java')));

// 扫描上界来自任务页标定;未标定回退旧的 5 页.
check('B1 scan bound derives from the calibrated task tab with legacy fallback',
  /lastSearchablePageBound\(\)[\s\S]{0,400}?calibratedMainBagTaskTabIndex\.get\(\)/.test(bag)
  && /\? taskTab : LAST_SEARCHABLE_PAGE_INDEX;/.test(bag));
// 枚举循环必须用界,不再写死 LAST_SEARCHABLE_PAGE_INDEX.
check('B2 the scan loop iterates to the bound, not the hardcoded last page',
  /int pageBound = lastSearchablePageBound\(\);/.test(bag)
  && /for \(int i = FIRST_SEARCHABLE_PAGE_INDEX; i <= pageBound; i\+\+\)/.test(bag));
// preferred 起始页越界时同样不放行.
check('B3 a preferred page beyond the bound is not scanned',
  /preferredPageIndex <= pageBound/.test(bag));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
