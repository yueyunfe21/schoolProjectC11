#!/usr/bin/env node
/*
 * G137 source contracts（用户拍板 2026-09-01"1-2"）: 封妖 17:21 卡死双修.
 *
 * 事故链=多谢点完光标停在选项行 → 悬停变色让绿字模板 9 秒配不上 → 云端 5 秒宽限重按绿链换代
 * 口令 → 清障腿碰巧挪走鼠标后封妖符被真点掉 → 答案带旧口令被 G053 丢弃 → 云端不知符已用,
 * 死循环到手动停.
 * 修①(client)=选项点击 executed 后独立请求三步滑出 no-park 带(不挂点击请求尾,滑步失败不得
 * 改写点击结果);修②(cloud)=G053 围栏对"已执行的使用封妖符"定向豁免.
 *
 * Run: node scripts/g137-fengyao-hover-fence-contract.js
 */
'use strict';
const fs = require('fs');
const path = require('path');

const CLIENT = path.resolve(__dirname, '..');
const CLOUD = path.resolve(CLIENT, '..', 'dhxy-cloud-brain');
const read = p => fs.readFileSync(p, 'utf8');

let passed = 0, failed = 0;
const check = (name, ok, detail) => {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
};

// ── 客户端：移出悬停滑步 ─────────────────────────────────────────────────────
const smp = read(path.join(CLIENT,
    'src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java'));
check('C1 executed 分支里点击确认后排滑步（同一 if (executed) 块）',
  /if \(executed\) \{\s*\/\/ NPC[\s\S]{0,200}?markTaskDialogOptionAnswered\(validated\.actionKey\(\)\);[\s\S]{0,300}?submitDialogOptionUnhoverGlide\(optionX, optionY\);\s*\}/.test(smp));
check('C2 滑步是独立请求：unhover 标签 + submitAsync + binding 几何守卫',
  smp.includes('inputSequences.submitAsync("tianting:dialog-option:unhover", glide);')
  && /private void submitDialogOptionUnhoverGlide\(int clickX, int clickY\) \{[\s\S]{0,400}?binding == null \|\| !binding\.hasGeometry\(\)/.test(smp));
check('C3 滑步构造走 no-park 带判定 + 最近边出口',
  /static List<InputAction> dialogOptionUnhoverGlide\(int clickX, int clickY,[\s\S]{0,600}?DialogMouseNoParkZone\.containsScreenPoint\([\s\S]{0,200}?return List\.of\(\);[\s\S]{0,400}?DialogMouseNoParkZone\.nearestExitTarget\(/.test(smp));
check('C4 点击请求本体未被加长（滑步不得挂在 moveAndClickLeftAsync 尾部）',
  /moveAndClickLeftAsync\(\s*dalisiAccept \? "dalisi:dialog-option"[\s\S]{0,200}?optionX, optionY, 80, 150\)/.test(smp)
  && !/moveAndClickLeftAsync\([^)]*unhover/.test(smp));

// ── 云端：G053 围栏封妖豁免 ──────────────────────────────────────────────────
const tt = read(path.join(CLOUD,
    'src/main/java/com/bot/dhxy/task/tianting/TiantingTask.java'));
check('S1 围栏先算 correlationCurrent 再算封妖豁免（真实解析器喂入）',
  /boolean correlationCurrent = armedProbeCorrelation != null\s*&& Objects\.equals\(armedProbeCorrelation, eventCorrelation\);/.test(tt)
  && /boolean fengyaoExecutedBypass = executedFengyaoAnswerBypassesCorrelationFence\(\s*optionNameFrom\(event\.getSummary\(\)\), clickExecuted\(event\.getSummary\(\)\)\);/.test(tt));
check('S2 只在两者皆否时拒收，G053 拒收日志保留',
  /if \(!correlationCurrent && !fengyaoExecutedBypass\) \{\s*log\.info\("\{\} G053 ignored stale or foreign dialog answer/.test(tt));
check('S3 跨代放行时落 G137 日志（留取证面）',
  /if \(!correlationCurrent\) \{\s*log\.info\("\{\} G137 天庭 executed 封妖符 answer accepted across correlation swap/.test(tt));
check('S4 豁免判据=使用封妖符 且 executed，缺一不可',
  /static boolean executedFengyaoAnswerBypassesCorrelationFence\(String actionKey, boolean executed\) \{\s*return TiantingDialogCatalog\.ACTION_FENGYAO\.equals\(actionKey\) && executed;\s*\}/.test(tt));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
