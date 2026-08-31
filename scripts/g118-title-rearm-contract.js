#!/usr/bin/env node
/*
 * G118 source contracts: 五环旧 title-rearm 在新对话链激活时必须退休并撤回已占槽动作.
 *
 * 为什么需要源码合同（G118 复审 P2）：Java 侧的 G118WuhuanTitleRearmCrossChainContractTest
 * 直测 package-private 判定 helper 与真实 CloudDialogPreparedActionState，覆盖了
 *   ① 六向真值表（armed+active / 无限期 = 退；null / inactive / expired / 未 arm = 不退）
 *   ② 占槽 → 撤回 → 交槽
 *   ③ 跨轮时序：round1 收尾 → rearm 迟到 → round2 链已激活时不得夺回槽
 * 但它**没有驱动 observer 的生产链本身**。若有人把生产侧的两个调用点整段删掉、而 helper 与
 * state 都不动，那三条 Java 合同一条都杀不掉。给 observer 搭完整脚手架性价比不对（复审裁定），
 * 所以在这里用与 g103/g108 同样的轻量源码断言把接线钉死。
 *
 * Run: node scripts/g118-title-rearm-contract.js
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
function countOccurrences(haystack, needle) {
  let n = 0, i = 0;
  for (;;) {
    const at = haystack.indexOf(needle, i);
    if (at < 0) return n;
    n++;
    i = at + needle.length;
  }
}

const OBSERVER = path.join(
  CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java');
const observerRaw = read(OBSERVER);
const observer = stripComments(observerRaw);

let passed = 0, failed = 0;
function check(name, ok, detail) {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
}

const HELPER = 'shouldRetireWuhuanTitleRearmForActiveDialogChain';

// 1. 判定必须存在，且是 package-private static —— 合同要直测它，不能被改成 private 或实例方法。
check('retire decision exists as a package-private static helper',
  /\n    static boolean shouldRetireWuhuanTitleRearmForActiveDialogChain\(/.test(observer));

// 2. 恰好两个生产调用点。这是本脚本存在的理由：删掉任一个，Java 合同都杀不掉。
//    总出现数 = 1 次声明 + 2 次调用 = 3。
const total = countOccurrences(observer, HELPER);
check('retire decision has exactly two production call sites (plus its declaration)',
  total === 3, `found ${total} occurrences, expected 3 (1 declaration + 2 call sites)`);

// 3. 两个调用点各自守一端，语义相反，缺一不可：
//    · 收回路径（修 B / 存量）：!shouldRetire(...) 时提前 return —— 不该退休就别动已占的槽。
//    · 发布前判定（修 A / 增量）：shouldRetire(...) 时作废迟到的 rearm —— 拦住后来的。
//    修 B 只能收回已经占住的，拦不住后来的；判定必须在发布之前就说"别发"。
check('withdrawal path guards on the negated decision (修 B: existing slot holder)',
  observer.includes(`if (!${HELPER}(`));
check('pre-publish path voids a late rearm on the positive decision (修 A: incoming)',
  new RegExp(`if \\(${HELPER}\\(`).test(observer));

// 4. 两条路径都必须留下可归因的日志 —— 否则下次卡死又是零证据。
check('withdrawal logs its action id and reason',
  observerRaw.includes('G118 wuhuan title rearm action withdrawn for active dialog chain'));
check('retirement logs the presence sequence and interest id',
  observerRaw.includes('G118 wuhuan title rearm retired for active dialog chain'));

// 5. Java 合同必须在版本控制里可见（本项目已四次栽在 .gitignore 白名单上）。
const JAVA_CONTRACT = path.join(
  CLOUD, 'src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/G118WuhuanTitleRearmCrossChainContractTest.java');
check('java contract file is present on disk', fs.existsSync(JAVA_CONTRACT));
const ignore = read(path.join(CLOUD, '.gitignore'));
check('java contract is whitelisted in cloud .gitignore',
  ignore.includes('!src/test/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/G118WuhuanTitleRearmCrossChainContractTest.java'));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
