# CR271 TURN-33 Repair #2 Independent Delivery Review R2

## REVIEW CLAIMED - 2026-07-16T05:38:15.0900372-04:00

- Role: independent delivery reviewer R2; not the implementation Worker and not the parent/final reviewer.
- Scope: read-only review of current Cloud `SummonSkillService.java` and `SummonSkillTurnContractTest.java` against the frozen TURN-33 contract and `696a12b0` business baseline.
- Restrictions acknowledged: this report is the only write target; no Java, test, TURN card, or main-document edits; no Maven/runtime/application/input; no Git mutation.

<!-- TRUE_EOF: CR271 TURN-33 REPAIR-2 INDEPENDENT DELIVERY REVIEW R2 CLAIMED 2026-07-16T05:38:15.0900372-04:00 -->

## INDEPENDENT DELIVERY REVIEW R2 - 2026-07-16T05:50:52.6436206-04:00

### Decision

**BLOCKED - P0/P1/P2 = 0/1/0.**

Repair #2 correctly prevents any new static scan/action/UUID after a real ultimate-corner click once
the generated-skill chain reaches a verified stable state. However, one reachable max-budget branch
returns success before that required stable-state verification. This is an unapproved difference from
`696a12b0`, and neither current named fixture covers the boundary combination.

### P1-1 - fifth generated-normal deletion skips the baseline post-delete stability observation

**Exact production evidence**

- Current Cloud `SummonSkillService.java:823-846` marks ultimate generation successful, deletes a
  generated `NORMAL_SKILL`, increments `deletedCount`, and at `:830-832` returns
  `success("summon skill delete limit reached")` as soon as the generated deletion reaches 5.
  The required `post-generated-delete-slot-*` observation and `EMPTY_SLOT` / `KEEP_SKILL` / failure
  classification only occur later at `:834-846`, so this branch skips them.
- The confirmed baseline
  `git show 696a12b0ffb8aa21f7d5dee841a65cecd78be9f7:src/main/java/com/bot/dhxy/service/SummonSkillService.java`
  performs the generated-normal delete at baseline `:584-589`, then unconditionally performs the
  post-generated-delete inspection and stable/failure classification at `:590-604`. There is no
  max-budget early-success gate between those operations.
- The edge is reachable without retries: after three earlier ordinary deletions, a fourth ordinary
  deletion can expose `EMPTY`, the ultimate corner can generate `NORMAL`, and deleting it becomes the
  fifth whole-pass deletion. A selected `EMPTY` reached with four prior deletions is another route.
  The current branch then reports success even if the unobserved slot would still be `NORMAL`, become
  `UNKNOWN`, or otherwise fail to reach the baseline stable `EMPTY/KEEP` terminal.

**Contract evidence**

- TURN-33 Repair #2 card `:419-425` requires the real chain through generated-normal delete and its
  stable state, while forbidding only the *subsequent* static rescan/action/UUID after the ultimate
  chain has terminated. The card also states at `:512` that there is no approved business difference
  from `696a12b0`.
- The parent budget decision at `:319-328` requires no sixth deletion and no fresh static rescan after
  the fifth deletion. It does not approve silently dropping the existing ultimate generated-delete
  verification. If the intended policy is instead that the fifth-delete gate overrides this baseline
  verification, that is a business-semantic conflict requiring explicit user approval; the current
  card expressly records no such approval.

**Test-source gap**

- `SummonSkillTurnContractTest.java:258-329` verifies the post-generated-delete capture only when the
  generated deletion raises the total from 1 to 2. It cannot execute the `deletedCount >= 5` branch.
- `SummonSkillTurnContractTest.java:332-411` reaches five ordinary deletions and explicitly asserts
  `ultimateGenerateClicked=false`; it therefore cannot expose the generated-normal fifth-delete edge.

**Required repair and re-review gate**

- Preserve the baseline generated-normal terminal chain when its deletion reaches the shared budget:
  perform exactly one `post-generated-delete` observation, accept only stable `EMPTY/KEEP`, and then
  end the current pass without any fresh static scan, second ultimate action, or later UUID.
- Keep the fifth-deletion budget as a prohibition on a sixth deletion/fresh pass, not as an implicit
  success classification for an unobserved generated slot. If that interpretation is disputed, stop
  for an explicit parent/user contract decision instead of encoding a new semantic silently.
- Extend the production-public-API named fixture so a generated-normal deletion is exactly the fifth
  deletion. It must prove the final stability capture is consumed, unstable/unknown post-delete state
  cannot return success, and no subsequent static scan/action/UUID occurs after that terminal check.

### Independently confirmed areas with no additional P0/P1/P2

- Real ultimate click: `ultimateGenerateClicked` is set only after successful click mechanics; every
  continuing caller checks it before fresh rescan. Generated `KEEP`, generated `NORMAL` plus verified
  delete, and stable generated outcomes otherwise end the pass with no later static scan/action/UUID.
- Hover/miss: an ultimate-corner hover miss does not set the click flag; after an earlier ordinary or
  locked-boundary deletion, the loop performs a genuinely fresh static observation before selecting
  the next single tail slot.
- Budget outside P1-1: five ordinary reachable deletions share one whole-pass budget and stop before a
  sixth deletion command/UUID.
- Cleanup: the public pass has one `finally` cleanup path; normal, typed failure, STOP, and uncertain
  exits do not create a second cleanup call, and primary fatal/stop failures retain precedence.
- Terminal/uncertain and retry: known failed outcomes fail closed; STOP and uncertain outcomes
  propagate; each explicit business action receives one UUID and there is no automatic retry.
- Exact context: action preflight checks the current bound window, HWND, process id, window rect,
  provider, and STOP before UUID/command emission; invalid preflight paths remain zero UUID/command.
- Forbidden state: no Summon production retry/session/owner/ledger/TTL/durable-workflow control was
  found. The retained whole-pass compatibility capability remains a zero-command/zero-UUID tombstone.

### Review basis and repository state

- Fully read: repository `AGENTS.md`, `docs/DHXY_CONTEXT.md`, current `docs/ACTIVE_WORK.md` top,
  TURN-33 card through its latest true EOF Parent Review #3, plan Sections 14-19, the HTTPS turn
  protocol specification, and the applicable `docs/业务逻辑.md` static-tail/ultimate rules.
- Fully read current Cloud `SummonSkillService.java` (1428 lines, SHA-256
  `d28e62a56c170bc26a6d16035670515e4fb8f55eebf5d8356515d1565f1c1a46`) and
  `SummonSkillTurnContractTest.java` (1553 lines, SHA-256
  `68312d38cc5d7724aaf0b86495fcf7810540fc70e64263fb1a874b9961b5b7dc`).
- Read both complete statuses: DHXY is on `thin-client-design` with 85 status entries; Cloud is on
  `navigation-migration` with 28 status entries. Relevant Cloud production source is untracked and the
  named test is ignored by existing `.gitignore:15`; TURN-33 already records this as a later delivery/
  commit gate, so it is not double-counted as a new R2 severity finding.
- Per reviewer restrictions, no Maven/JUnit/compile/package, runtime/application/server, capture/input,
  or Git mutation was performed. No Java, test, TURN card, or main document was edited; this report is
  the only write target.

<!-- TRUE_EOF: CR271 TURN-33 REPAIR-2 INDEPENDENT DELIVERY REVIEW R2 BLOCKED P0=0 P1=1 P2=0 2026-07-16T05:50:52.6436206-04:00 -->
