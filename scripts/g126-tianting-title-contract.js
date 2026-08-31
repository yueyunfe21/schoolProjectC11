#!/usr/bin/env node
/*
 * G126 source contracts: "标题里带完成两个字一律作废"的接线不得被静默删除.
 *
 * 事故（2026-08-31 12:37 天庭队长动都不动）：新手号追踪器常驻元任务 完成天庭任务[新手任务]，
 * 标题含与 tianting_title.png 相同的"天庭任务"四字，裸模板 0.9662 假阳性 → titlePresent 恒真 →
 * 接任务入口被堵死，每 1.1 秒一轮 PARK_IDLE。用户拍板：标题含"完成"一律作废，不作为任何判断。
 * 行为由 G126WanchengTitleVoidContractTest（6 条 Java 合同、实测帧 fixtures、3 变异全红）直测；
 * 本合同钉两端生产接线与素材发布。
 *
 * Run: node scripts/g126-tianting-title-contract.js
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

// ── 云端 TaskTrackerPanelService ──────────────────────────────────────────────
const svc = strip(read(path.join(
  CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/TaskTrackerPanelService.java')));

// 全部标题匹配（switch 内 7 个 case + 四大妖王回退）必须吃作废遮蔽后的副本。
check('S1 title-match switch consumes the voided panel',
  /BufferedImage titleSearchPanel = maskWanchengVoidTitles\(panel\);/.test(svc));
{
  const sw = svc.slice(svc.indexOf('TitleMatch titleMatch = switch (taskCode)'),
                       svc.indexOf('case "wubei"') + 60);
  check('S2 every switch case reads titleSearchPanel, none reads the raw panel',
    sw.length > 0 && !/matchTitle\(\s*panel,/.test(sw) && (sw.match(/titleSearchPanel/g) || []).length >= 6);
}
check('S3 four-kings fallback also reads the voided panel',
  /matchTitle\(\s*titleSearchPanel, TIANTING_FOUR_KINGS_TITLE/.test(svc));
// helper 是 package-private static（Java 合同直测），且循环遮蔽有上限。
check('S4 void helper stays package-private static with a bounded loop',
  /\n    static BufferedImage maskWanchengVoidTitles\(BufferedImage panel\)/.test(svc)
  && /WANCHENG_VOID_MASK_MAX_PASSES/.test(svc));
// 无命中必须原图原样返回——老号零影响判据（变异②已证明合同杀得掉）。
check('S5 no-hit path returns the original panel object',
  /BufferedImage work = panel;/.test(svc));

// ── 客户端 UnknownPhasePresenceLocalMechanics（G017 presence 同缺陷）─────────
const mech = strip(read(path.join(CLIENT,
  'src/main/java/com/bot/dhxy/window/observation/UnknownPhasePresenceLocalMechanics.java')));
check('C1 G017 presence consumes the voided tracker crop',
  /presence\(maskWanchengVoidTitles\(tracker\), titleTemplate\(\)\)/.test(mech));
check('C2 client uses the same void template path',
  mech.includes('images/template/tracker/wancheng_title_void.png'));
check('C3 client loop is bounded and no-ops on miss',
  /WANCHENG_MASK_MAX_PASSES/.test(mech) && /BufferedImage work = tracker;/.test(mech));

// ── 素材两端发布（云端 classpath resources / 客户端文件系统）─────────────────
check('T1 void template exists in the client images dir',
  fs.existsSync(path.join(CLIENT, 'images/template/tracker/wancheng_title_void.png')));
check('T2 void template exists in cloud src/main/resources',
  fs.existsSync(path.join(CLOUD, 'src/main/resources/images/template/tracker/wancheng_title_void.png')));

// ── fixtures 与 Java 合同可见性 ───────────────────────────────────────────────
check('F1 incident fixture archived in cloud repo',
  fs.existsSync(path.join(CLOUD, 'images/test-cases/tianting-title-g126/negative_panel_newbie_meta_20260831.png')));
check('F2 coexisting composite fixture archived',
  fs.existsSync(path.join(CLOUD, 'images/test-cases/tianting-title-g126/composite_real_plus_newbie.png')));
check('F3 java contract file present',
  fs.existsSync(path.join(CLOUD, 'src/test/java/com/yueyunfe/dhxy/cloudbrain/G126WanchengTitleVoidContractTest.java')));
check('F4 java contract whitelisted in cloud .gitignore',
  read(path.join(CLOUD, '.gitignore')).includes(
    '!src/test/java/com/yueyunfe/dhxy/cloudbrain/G126WanchengTitleVoidContractTest.java'));

console.log(`\n${passed} passed, ${failed} failed`);
process.exit(failed === 0 ? 0 : 1);
