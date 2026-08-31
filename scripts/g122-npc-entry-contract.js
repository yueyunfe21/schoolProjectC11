#!/usr/bin/env node
/*
 * G122 source contracts (review-return repairs, 2026-08-29):
 *   P2-1  fresh in-tolerance arrival must be WIRED inside WindowObservationSampler
 *         .acceptAnalysisResults BEFORE the walking-baseline bypass and the terminal
 *         request-stamp gate, and its hit path must commit ARRIVED + clear pending + continue.
 *         (G122FreshArrivalContractTest only exercises the helper; deleting the wiring would
 *         keep those 8 green — this file makes that deletion red.)
 *   P2-2  client CONTINUATION consumption source shape (behavior itself is pinned by
 *         G122ContinuationClientConsumptionContractTest).
 *   P1    defer wire rename: no report mask on the client; cloud stamps TASK_PHASE_DEFERRED +
 *         success=false for honest DIALOG_OPEN_UNVERIFIED defer reports; pending registration
 *         accepts the honest encoding.
 *   P2-3  arrival-frame evidence is persisted only after every rejection gate.
 * Run: node scripts/g122-npc-entry-contract.js
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
  sampler: read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java')),
  executor: read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/cloud/turn/local/NpcArrivalFrameFifoLocalExecutor.java')),
  queueStore: read(path.join(CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickSmartQueueStore.java')),
  memoryStore: read(path.join(CLOUD, 'src/main/java/com/yueyunfe/dhxy/cloudbrain/NpcClickMemoryStore.java')),
  protoClient: read(path.join(CLIENT, 'src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartQueueMessage.java')),
  protoCloud: read(path.join(CLOUD, 'src/main/java/com/bot/dhxy/cloud/task/NpcClickSmartQueueMessage.java')),
};
const code = Object.fromEntries(Object.entries(files).map(([k, v]) => [k, stripComments(v)]));

let passed = 0, failed = 0;
function check(name, ok, detail) {
  if (ok) { passed++; console.log(`PASS  ${name}`); }
  else { failed++; console.log(`FAIL  ${name}${detail ? ' -- ' + detail : ''}`); }
}

// ---------------------------------------------------------------- P2-1: fresh arrival wiring
const acceptStart = code.sampler.indexOf('void acceptAnalysisResults(');
const acceptEnd = code.sampler.indexOf('private void armGhostKingChangshouFlightAssistFromMovingFrame');
const accept = code.sampler.slice(acceptStart, acceptEnd);
check('acceptAnalysisResults exists ahead of the ghost-king section', acceptStart > 0 && acceptEnd > acceptStart);

const idxFresh = accept.indexOf('if (freshCoordinateProvesArrival(');
const idxWalking = accept.indexOf('if (localPathingBaselinePending');
const idxTerminalGate = accept.indexOf('localPathingCoordinateRequestedChangedAtMs != localPathingLastChangedAtMs');
check('fresh in-tolerance arrival is decided INSIDE acceptAnalysisResults', idxFresh > 0);
check('fresh decision sits BEFORE the walking-baseline bypass', idxFresh > 0 && idxWalking > idxFresh);
check('fresh decision sits BEFORE the terminal request-stamp gate', idxFresh > 0 && idxTerminalGate > idxFresh);

const freshBlock = idxFresh > 0 && idxWalking > idxFresh ? accept.slice(idxFresh, idxWalking) : '';
check('hit path commits WindowPathingState.ARRIVED into the pathing snapshot',
  freshBlock.includes('.state(WindowPathingState.ARRIVED)'));
check('hit path clears the coordinate pending gate', freshBlock.includes('localPathingCoordinatePending = false;'));
check('hit path clears the walking-baseline pending gate', freshBlock.includes('localPathingBaselinePending = false;'));
check('hit path invalidates terminal frame evidence', freshBlock.includes('invalidateTerminalFrameEvidence();'));
check('hit path short-circuits the legacy chains with continue', /continue;\s*\}/.test(freshBlock));
check('the wired decision is the SAME helper the 8 unit contracts pin',
  /static boolean freshCoordinateProvesArrival\(/.test(code.sampler));

// ------------------------------------------------- P2-2: CONTINUATION consumption source shape
const consumeStart = code.executor.indexOf('private SessionResult consumeOne(');
const consume = code.executor.slice(consumeStart, code.executor.indexOf('private NpcClickSmartQueueOutcome executeQueueCandidate('));
const idxContinuation = consume.indexOf('== NpcClickSmartQueueMessage.Type.CONTINUATION');
const idxBudget = consume.indexOf('candidateMessageCount++');
check('CONTINUATION branch exists in the FIFO consume loop', idxContinuation > 0);
check('CONTINUATION is handled BEFORE the candidate budget is charged',
  idxContinuation > 0 && idxBudget > idxContinuation);
const continuationBlock = idxContinuation > 0 ? consume.slice(idxContinuation, idxBudget) : '';
check('CONTINUATION answers SKIPPED and keeps consuming (continue, no slot burn)',
  continuationBlock.includes('NpcClickSmartQueueOutcome.SKIPPED') && continuationBlock.includes('continue;'));
check('session END exhaustion feeds the bounded fresh-frame replacement',
  code.executor.includes('for (int attempt = 1; attempt <= 2; attempt++)')
  && code.executor.includes('if (attempt == 2)')
  && code.executor.includes('if (!replaceWithFreshFrame(arguments, spec, binding))'));

// --------------------------------------------------------------- P1: defer wire rename honesty
check('client report mask wireOutcomeForCloud is deleted', !code.executor.includes('wireOutcomeForCloud'));
check('client reports the local outcome verbatim', code.executor.includes('message, outcome, reason);'));
const enrichStart = code.queueStore.indexOf('ObjectNode enrichArrivalOutcome(');
const enrich = code.queueStore.slice(enrichStart, code.queueStore.indexOf('private static boolean isTerminalOutcome('));
check('cloud recognizes the honest defer encoding (defer demand + DIALOG_OPEN_UNVERIFIED)',
  /deferredUnverified = deferDemanded\s*&&\s*"DIALOG_OPEN_UNVERIFIED"\.equalsIgnoreCase\(result\)/.test(enrich));
check('cloud stamps TASK_PHASE_DEFERRED for the honest defer report',
  /\(verified \|\| deferredUnverified\)[\s\S]{0,200}TASK_PHASE_DEFERRED/.test(enrich));
check('success stays derived from verified only — a defer pending can never be success=true',
  enrich.includes('enriched.put("success", verified);')
  && !enrich.includes('enriched.put("success", true)'));
const register = code.memoryStore.slice(
  code.memoryStore.indexOf('String registerDeferredPending('),
  code.memoryStore.indexOf('String confirmDeferredPending(String windowId)'));
check('pending registration accepts the honest defer encoding',
  register.includes('"DIALOG_OPEN_UNVERIFIED".equalsIgnoreCase(result)'));
check('pending registration still accepts the stamp-proven VERIFIED defer (tianting/ghost-king)',
  register.includes('"VERIFIED".equalsIgnoreCase(result)'));
check('pending registration still requires TASK_PHASE_DEFERRED strength',
  register.includes('"TASK_PHASE_DEFERRED".equalsIgnoreCase(DecisionEngine.text(request, "verificationStrength"))'));
check('protocol NpcClickSmartQueueMessage byte-identical across repos', files.protoClient === files.protoCloud);

// ---------------------------------------------- P2-3: evidence persists only after every gate
const prepStart = code.queueStore.indexOf('private String prepareArrivalFrame(');
const prep = code.queueStore.slice(prepStart, code.queueStore.indexOf('String unlockArrivalFrame('));
const idxSave = prep.indexOf('saveArrivalFrameEvidence(frame, replacement);');
check('evidence save is called exactly once in prepareArrivalFrame',
  idxSave > 0 && prep.indexOf('saveArrivalFrameEvidence(frame, replacement);', idxSave + 1) < 0);
check('evidence save sits after the NO_DEMAND gates',
  idxSave > prep.lastIndexOf('return "status=NO_DEMAND";', idxSave));
check('evidence save sits after the replacement identity gate',
  idxSave > prep.indexOf('return "status=REPLACEMENT_IDENTITY_REJECTED";'));
check('evidence save sits after the idempotent frame gate',
  idxSave > prep.indexOf('return "status=FRAME_ALREADY_PREPARED'));
check('evidence save sits after the attempt gate',
  idxSave > prep.indexOf('return "status=ATTEMPT_REJECTED";'));
check('evidence save sits before the accepted session is created',
  idxSave > 0 && idxSave < prep.indexOf('cancelPreparedSession(demand);')
  && idxSave < prep.indexOf('return "status=FRAME_PREPARING;sessionId=" + sessionId;'));

console.log(`\n${passed}/${passed + failed} PASS${failed ? ` (${failed} FAIL)` : ''}`);
process.exit(failed ? 1 : 0);
