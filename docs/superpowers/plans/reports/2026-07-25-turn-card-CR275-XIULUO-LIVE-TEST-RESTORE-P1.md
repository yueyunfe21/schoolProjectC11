# CR275 Xiuluo Live-Test Override Removal

## Status

`SOURCE REVIEW COMPLETE / P0-P1-P2=0-0-0 / CLIENT+CLOUD COMPILE 0 / FRESH RESTART REQUIRED`

## Incident

The temporary Xiuluo heal-pet live verification mode was not fully removed after its runtime check.
Normal Cloud defaults still forced maintenance due immediately and disabled the validated startup
preparation chain. Client, Cloud, and launcher source also retained a dedicated environment/system
property path.

## Baseline And Approved Restoration

- `docs/业务逻辑.md` Xiuluo startup/maintenance rules require maintenance to run only when its
  existing due condition is true. A test must not make it due in normal production.
- `config/ui-game-settings.properties` carries the production values:
  `taskStartupPreparationEnabled=true` and
  `xiuluoMaintenanceRunImmediatelyOnStart=false`.
- Remove only the `DHXY_XIULUO_HEAL_PET_LIVE_TEST` /
  `dhxy.xiuluoHealPetLiveTest` test plumbing and its dedicated launcher.
- Preserve all production Xiuluo task, maintenance, pathing, Runner, OCR, and NPC ClickSmart logic.
- `D:\mavenProject\DHXY` remains read-only.

## Write Set

### Client

- `AutoBot`
- `WindowTaskControlService`
- `scripts/run-cloud-brain-server.ps1`
- delete `scripts/run-xiuluo-heal-pet-live-test.ps1`

### Cloud

- `BotProperties`
- `CloudServiceHost`

## Verification

- Client and Cloud compile are mandatory.
- Client `mvn -q -DskipTests compile`: exit `0`.
- Cloud `mvn -q -DskipTests=false compile`: exit `0`.
- Static scan confirms no remaining `DHXY_XIULUO_HEAL_PET_LIVE_TEST` or
  `dhxy.xiuluoHealPetLiveTest` production/launcher references.
- Cloud defaults match the production UI settings:
  `taskStartupPreparationEnabled=true` and
  `xiuluoMaintenanceRunImmediatelyOnStart=false`.
- Parent source review: `P0/P1/P2=0/0/0`.
- No runtime/UI/capture/input and no local tests.

## Delivered

- Normal Client headless startup again uses
  `scanRegisterAndStartIndependentWindows(...)`; no heal-pet test registration branch remains.
- Remote turn metadata always uses the canonical preflight/current role source.
- Cloud host no longer mutates Xiuluo maintenance/startup properties from a test system property.
- The Cloud launcher no longer forwards a live-test JVM property.
- The dedicated `run-xiuluo-heal-pet-live-test.ps1` launcher was removed.
- No production Xiuluo phase or maintenance implementation was changed.

<!-- TRUE_EOF: CR275 SOURCE-REVIEW-COMPLETE FRESH-RESTART-REQUIRED 2026-07-25 -->
