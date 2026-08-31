#!/usr/bin/env node
/*
 * G108 source contracts: 战后限定窗口持续观察"召"并锁存归队事实.
 * Asserts the frozen wiring across BOTH repos:
 *   Client  D:\mavenProject\DHXY-cr271
 *   Cloud   D:\mavenProject\dhxy-cloud-brain
 * Run: node scripts/g108-zhao-watch-contract.js
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

const files = {
  protoClient: read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationFactType.java')),
  protoCloud: read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/cloud/turn/protocol/observation/ObservationFactType.java')),
  mechanics: read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/window/observation/TeamReturnZhaoWatchLocalMechanics.java')),
  sampler: read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java')),
  state: read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/service/TeamReturnZhaoWatchState.java')),
  coordinator: read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/service/LeaderTeamReturnCoordinator.java')),
  observer: read(path.join(CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/turn/runtime/CloudWholeTaskObserver.java')),
};
const code = Object.fromEntries(Object.entries(files).map(([k, v]) => [k, stripComments(v)]));

let passed = 0, failed = 0;
function check(name, ok, detail) {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
}

// 1. Protocol addition is byte-identical across repos and carries the new fact type.
check('protocol enum byte-identical across repos', files.protoClient === files.protoCloud);
check('protocol enum declares TEAM_RETURN_ZHAO_WATCH', /TEAM_RETURN_ZHAO_WATCH\s*\}/.test(code.protoClient));

// 2. One frozen interest key literal on both sides.
check('client interest key frozen', code.mechanics.includes('INTEREST_KEY = "team-return-zhao-watch"'));
check('cloud interest key frozen', code.state.includes('INTEREST_KEY = "team-return-zhao-watch"'));
check('cadence frozen at 1000ms on both sides',
  code.mechanics.includes('SAMPLE_PERIOD_MS = 1_000L') && code.state.includes('SAMPLE_PERIOD_MS = 1_000L'));

// 3. Contract (9): the client clamps the period locally; nothing can accelerate the 1s cadence.
const dutyMethod = code.sampler.slice(code.sampler.indexOf('private void sampleTeamReturnZhaoWatch'));
const dutyBody = dutyMethod.slice(0, dutyMethod.indexOf('\n    }'));
check('client clamps cadence to at least the frozen period',
  /Math\.max\(\s*TeamReturnZhaoWatchLocalMechanics\.SAMPLE_PERIOD_MS,\s*interest\.samplePeriodMs\(\)\)/.test(dutyBody));
check('client samples only behind the shared isDue gate', /isDue\(TeamReturnZhaoWatchLocalMechanics\.INTEREST_KEY/.test(dutyBody));

// 4. Client stays interest-driven and skips combat frames.
check('client samples only while the interest is present', /interest == null \|\| localCombatVisible/.test(dutyBody));
check('generic ROI loop never double-consumes the watch key',
  code.sampler.includes('TeamReturnZhaoWatchLocalMechanics.INTEREST_KEY.equals(interest.interestKey())) {\n                continue;')
  || /TeamReturnZhaoWatchLocalMechanics\.INTEREST_KEY\.equals\(interest\.interestKey\(\)\)\)\s*\{\s*continue;/.test(code.sampler));

// 5. Client publishes only the typed value; ROI pixels never leave.
check('client fact folds the typed value', /ObservationFactType\.TEAM_RETURN_ZHAO_WATCH,\s*teamReturnZhaoWatchDuty\.fold\(/.test(dutyBody));
check('client evidence writes are edge-gated, not per-second',
  /state\.equals\(lastSavedState\) && generation == lastSavedGeneration/.test(code.mechanics));

// 6. Cloud latch semantics: monotonic latch, generation fence, UNKNOWN never ABSENT.
check('latch requires the folded marker and an unfenced generation',
  /parsed\.everPresentInGeneration\(\)\s*&&\s*parsed\.generation\(\) > entry\.closedThroughGeneration/.test(code.state));
check('close fences the highest consumed generation',
  /closedThroughGeneration = Math\.max\(entry\.closedThroughGeneration, entry\.maxSeenGeneration\)/.test(code.state));
check('HUD gate passes only a literal fresh ABSENT', /!"ABSENT"\.equals\(entry\.currentState\)/.test(code.state)
  && code.state.includes('FAIL_STALE') && code.state.includes('FAIL_NO_DATA'));

// 7. Coordinator wiring: latch consulted at entry, HUD third gate at completion, cleanup chain.
check('gate entry consults the zhao-watch latch',
  /watchLatched = TeamReturnZhaoWatchState\.everPresentLatched\(context\)/.test(code.coordinator)
  && /triggerAlreadyLatched\s*\|\|\s*watchLatched\s*\|\|\s*initialSignalPresent/.test(code.coordinator));
check('gate start opens the observation window', /TeamReturnZhaoWatchState\.openWindow\(context, source \+ ":gate-start"\)/.test(code.coordinator));
check('completion runs the HUD third gate fail-closed',
  /hudCompletionGate\(context, System\.nanoTime\(\)\)/.test(code.coordinator)
  && /hudGate != TeamReturnZhaoWatchState\.HudGate\.PASS/.test(code.coordinator));
check('HUD veto reopens the panel like the member-pending veto',
  /hudGate != TeamReturnZhaoWatchState\.HudGate\.PASS[\s\S]{0,200}stages\.put\(key, GateStage\.NEED_PANEL_OPEN\)/.test(code.coordinator));
check('every completion path clears and fences the latch',
  /closeAndClear\(context, source \+ ":not-needed"\)/.test(code.coordinator)
  && /closeAndClear\(context, source \+ ":not-needed-confirmed"\)/.test(code.coordinator)
  && /closeAndClear\(context, source \+ ":complete"\)/.test(code.coordinator));
check('release removes the exact-run static entry (R1 P2)',
  /releaseRun\(context\.getWindowId\(\), context\.getNativeWindowHandle\(\),\s*context\.getTaskRunId\(\), source \+ ":released"\)/.test(code.coordinator));
check('replaced runs retire their watch entry', /TeamReturnZhaoWatchState\.releaseRun\(\s*stale\.windowId\(\), stale\.hwnd\(\), stale\.taskRunId\(\)/.test(code.coordinator));

// R1 P1-1: a gate consult before the first window sample must park, never trust one-shot ABSENT.
check('the window opens before ANY not-needed decision',
  /openWindow\(context, source \+ ":gate-consult"\)[\s\S]{0,600}everPresentLatched\(context\)/.test(code.coordinator));
check('AWAIT_HUD_EVIDENCE stage parks the undecided gate',
  /hudCompletionGate\(context, System\.nanoTime\(\)\)\s*!= TeamReturnZhaoWatchState\.HudGate\.PASS\)\s*\{\s*stages\.put\(key, GateStage\.AWAIT_HUD_EVIDENCE\)/.test(code.coordinator));
check('evidence wait resolves only by latch or fresh-window PASS',
  /stage == GateStage\.AWAIT_HUD_EVIDENCE[\s\S]{0,400}everPresentLatched\(context\)/.test(code.coordinator)
  && /evidence == TeamReturnZhaoWatchState\.HudGate\.PASS/.test(code.coordinator));
check('evidence wait rides the shared 180s fail-stop, no timeout release',
  /failIfGateWaitedTooLong\(context, key, source\);\s*\n\s*if \(stage == GateStage\.AWAIT_HUD_EVIDENCE\)/.test(code.coordinator));

// R1 P1-2: completion accepts only current-epoch, latest-generation, fresh ABSENT samples.
check('window epoch increments on every closed->open transition', /entry\.windowEpoch\+\+/.test(code.state));
check('opening wipes the retained sample so an old window cannot certify the new one',
  /entry\.windowEpoch\+\+;\s*entry\.currentState = null;\s*entry\.currentGeneration = Long\.MIN_VALUE;\s*entry\.sampleEpoch = Long\.MIN_VALUE;\s*entry\.lastFactReceiptNanos = 0L;/.test(code.state));
check('a closed window rejects business state entirely',
  /if \(!entry\.windowOpen\) \{\s*log\.debug\("G108 zhao-watch fact dropped, window closed/.test(code.state));
check('unsolicited samples before any window create no state',
  /Entry entry = ENTRIES\.get\(Key\.from\(context\)\);\s*if \(entry == null\) \{[\s\S]{0,200}return;/.test(code.state.slice(code.state.indexOf('public static void consumeFact'))));
check('completion gate validates window epoch and latest generation',
  /!entry\.windowOpen\s*\|\|\s*entry\.sampleEpoch != entry\.windowEpoch\s*\|\|\s*entry\.currentGeneration != entry\.maxSeenGeneration/.test(code.state)
  && /FAIL_NOT_CURRENT_WINDOW/.test(code.state));

// R1 P2: a garbled seen-marker rejects the whole fact instead of parsing as false.
check('parse rejects any non-literal ever marker',
  /!"true"\.equals\(everLiteral\) && !"false"\.equals\(everLiteral\)/.test(code.state));
check('parse rejects out-of-range generation/sequence',
  /generation < 0L \|\| sequence < 1L/.test(code.state));
check('observer terminal retires the run entry and its client-facing interest',
  /releaseRun\(context\.getWindowId\(\), context\.getNativeWindowHandle\(\),\s*context\.getTaskRunId\(\), "whole-task-observer-terminal"\)/.test(code.observer)
  && /terminalInbox\.removeInterest\(/.test(code.observer));

// 8. Observer wiring: window opens on ANY combat exit and conservatively at task start.
check('combat-exit edge opens the window without task-type filtering',
  /ObservationKeyEventType\.COMBAT_EXITED\)\s*\{\s*maybeOpenTeamReturnZhaoWatchWindow\(context, "combat-exit-edge:"/.test(code.observer));
check('task start opens the window conservatively (hot-start/recovery)',
  /maybeOpenTeamReturnZhaoWatchWindow\(context, "task-start-conservative"\)/.test(code.observer));
check('member windows never open a leader watch',
  /isLocalSupportMemberSession\(context\)\)\s*\{\s*return;\s*\}\s*TeamReturnZhaoWatchState\.openWindow/.test(code.observer));
check('full interest republish carries the open window',
  /TeamReturnZhaoWatchState\.isWindowOpen\(context\)[\s\S]{0,200}TeamReturnZhaoWatchState\.INTEREST_KEY, TeamReturnZhaoWatchState\.SAMPLE_PERIOD_MS/.test(code.observer));
check('interest reconciled open<->published every cycle',
  /syncTeamReturnZhaoWatchInterest\(context, observationRunId\)/.test(code.observer)
  && /removeInterest\(tenantId, deviceId, windowId, observationRunId,\s*TeamReturnZhaoWatchState\.INTEREST_KEY\)/.test(code.observer));
check('facts consumed fail-closed on unreadable values',
  /consumeTeamReturnZhaoWatchFact\(context, observationRunId\)/.test(code.observer)
  && /parsed == null[\s\S]{0,300}return;/.test(code.observer.slice(code.observer.indexOf('consumeTeamReturnZhaoWatchFact(TaskExecutionContext'))));

// 9. The publisher-less legacy placeholder must stay dead in the new G108 surface.
check('no bare TEAM_RETURN_STATE_CHANGED revived in G108 files',
  !code.mechanics.includes('TEAM_RETURN_STATE_CHANGED')
  && !code.state.includes('TEAM_RETURN_STATE_CHANGED')
  && !dutyBody.includes('TEAM_RETURN_STATE_CHANGED'));

console.log(`\n${passed}/${passed + failed} PASS${failed ? ` (${failed} FAIL)` : ''}`);
process.exit(failed ? 1 : 0);
