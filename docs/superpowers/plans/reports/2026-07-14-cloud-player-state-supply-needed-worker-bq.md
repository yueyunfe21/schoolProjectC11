# W-PLAYERSTATE-SUPPLY-NEEDED-CPU-IMP1 Worker Report

## CLAIMED

- task: `W-PLAYERSTATE-SUPPLY-NEEDED-CPU-IMP1`
- claimedAt: `2026-07-14T04:48:42.022-04:00`
- uniqueWriteSet:
  - `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
  - `D:\mavenProject\DHXY\docs\superpowers\plans\reports\2026-07-14-cloud-player-state-supply-needed-worker-bq.md`

## Implementation #1

- workerStatus: `IMPLEMENTED_AWAITING_PARENT_REVIEW`（仅实现与 self-QA，不构成 review/Approved）
- completedAt: `2026-07-14T04:57:19.691-04:00`
- baseline: committed DHXY `0114604e:src/main/java/com/bot/dhxy/service/PlayerStateService.java`
- target: `D:\mavenProject\dhxy-cloud-brain\src\main\java\com\bot\dhxy\service\PlayerStateService.java`
- target Java SHA-256: `17e98407cecbb7bf364d6532929f41fe3d215ea3bf62189361107f5beeeff10a`
- pre-edit target Java SHA-256: `46137b5ae1457d7a015f45f1b8dc90cd21c9119ae57fd45cd348d7c28cec51c1`

### Exact Source Block

Selected constant declarations are committed source lines `89`, `90`, `92`, and `102`. The complete method block is
committed source lines `1054-1088`, including the single separator blank line used by the hash extraction.

```java
    private static final int CHAR_BAR_LEFT_X = 949;
    private static final int CHAR_BAR_RIGHT_X = 1020;
    private static final int PET_BAR_RIGHT_X = 876;
    private static final int HIGHER_HEALTH_PROBE_OFFSET = 10;

    private boolean isSupplyNeededFromSnapshot(BufferedImage bars, String name, int relX, int relY,
                                               boolean expectRed, int threshold) {
        if (bars == null) {
            return false;
        }
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            log.warn("first-aid no-focus sample out of bounds: name={} rel=({}, {}) snapshot={}x{}",
                    name, relX, relY, bars.getWidth(), bars.getHeight());
            return false;
        }
        if (isHealthyInSnapshotArea(bars, relX, relY, expectRed)) {
            return false;
        }

        int normalizedThreshold = normalizeThreshold(threshold);
        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(name.contains("宝宝") ? PET_BAR_LEFT_X : CHAR_BAR_LEFT_X,
                name.contains("宝宝") ? PET_BAR_RIGHT_X : CHAR_BAR_RIGHT_X, higherThreshold);
        if (isHealthyInSnapshotArea(bars, higherRelX, relY, expectRed)) {
            log.info("[{}] no-focus precheck: {}% sample low but {}% still healthy, skip supply",
                    name, normalizedThreshold, higherThreshold);
            return false;
        }

        int rgb = bars.getRGB(localX, localY);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        log.warn("[{}] no-focus precheck: below {}%, supply needed rgb=({}, {}, {})",
                name, normalizedThreshold, r, g, b);
        return true;
    }
```

### Exact Target Block

Selected target declarations are lines `45`, `46`, `48`, and `54`. The complete target method block is lines
`139-173`, including the same separator blank line used by the hash extraction.

```java
    private static final int CHAR_BAR_LEFT_X = 949;
    private static final int CHAR_BAR_RIGHT_X = 1020;
    private static final int PET_BAR_RIGHT_X = 876;
    private static final int HIGHER_HEALTH_PROBE_OFFSET = 10;

    private boolean isSupplyNeededFromSnapshot(BufferedImage bars, String name, int relX, int relY,
                                               boolean expectRed, int threshold) {
        if (bars == null) {
            return false;
        }
        int localX = relX - BARS_SCAN_LEFT_X;
        int localY = relY - BARS_SCAN_TOP_Y;
        if (localX < 0 || localY < 0 || localX >= bars.getWidth() || localY >= bars.getHeight()) {
            log.warn("first-aid no-focus sample out of bounds: name={} rel=({}, {}) snapshot={}x{}",
                    name, relX, relY, bars.getWidth(), bars.getHeight());
            return false;
        }
        if (isHealthyInSnapshotArea(bars, relX, relY, expectRed)) {
            return false;
        }

        int normalizedThreshold = normalizeThreshold(threshold);
        int higherThreshold = Math.min(95, normalizedThreshold + HIGHER_HEALTH_PROBE_OFFSET);
        int higherRelX = calculateX(name.contains("宝宝") ? PET_BAR_LEFT_X : CHAR_BAR_LEFT_X,
                name.contains("宝宝") ? PET_BAR_RIGHT_X : CHAR_BAR_RIGHT_X, higherThreshold);
        if (isHealthyInSnapshotArea(bars, higherRelX, relY, expectRed)) {
            log.info("[{}] no-focus precheck: {}% sample low but {}% still healthy, skip supply",
                    name, normalizedThreshold, higherThreshold);
            return false;
        }

        int rgb = bars.getRGB(localX, localY);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        log.warn("[{}] no-focus precheck: below {}%, supply needed rgb=({}, {}, {})",
                name, normalizedThreshold, r, g, b);
        return true;
    }
```

### Block SHA-256

All block hashes use UTF-8 after CRLF/LF normalization to LF.

- selected four-constant manifest: source/target exact `true`; source SHA-256 = target SHA-256 =
  `dc312fbde6478e4d860034d901496015cabe21e260fcba1db0106c7fa108dd7f`
- complete method block: source/target exact `true`; source SHA-256 = target SHA-256 =
  `5f8331409d5cb4d0747ee49caf24035d5f3761e6560333980f70e7135bd052f7`
- combined constant manifest + blank line + complete method block: source/target exact `true`;
  source SHA-256 = target SHA-256 = `b414ce6e15839fbc314972d75e47aea2c0dcdbc6ecdbf5fe100a09afd1e4b59f`

### Baseline Descriptor Resolution

- The committed file contains exactly one `isSupplyNeededFromSnapshot(BufferedImage,String,int,int,boolean,int)`.
  Its authoritative parameter names are `bars, name, relX, relY, expectRed, threshold`; those exact committed tokens
  were copied instead of renaming them to the semantic labels in the task description.
- Neither committed `0114604e` nor the pre-edit/current Cloud target contains `BARS_SCAN_Y_OFFSET` or
  `BARS_SCAN_HEIGHT` (`0` occurrences). The authoritative method instead depends on the already-present Cloud
  `BARS_SCAN_LEFT_X` and `BARS_SCAN_TOP_Y`; no alias or extra constant was invented.
- Existing Cloud `PET_BAR_LEFT_X`, `BARS_SCAN_LEFT_X`, `BARS_SCAN_TOP_Y`,
  `isHealthyInSnapshotArea(...)`, `normalizeThreshold(...)`, `calculateX(...)`, the single `@Slf4j`, and its existing
  import were reused. No duplicate was added.

### Compile And Dormant Evidence

- cwd: `D:\mavenProject\dhxy-cloud-brain`
- command: `mvn -q compile`
- constraints: no `clean`; no test command; no application/server/host/Task/poller/UI/capture/input started
- compile exit: `0` (fresh run, wall time `25.5s`)
- target scan: method declaration count `1`; total `isSupplyNeededFromSnapshot(` token count `1`, so no caller was added
- target scan: each requested constant and reused dependency declaration occurs exactly once; `@Slf4j` and its import
  each occur exactly once
- exact added executable block has `0` matches for capture, remote, `ImageIO`/`java.io`, input actions/queues,
  `TaskExecutionContext`, state/owner/session/ledger/TTL/retry, thread, or clock APIs

### Write-Set Counterevidence

- Before the claim, both repository statuses were read in full. Existing user/parallel dirty and untracked files were
  preserved; no reset, checkout, clean, deletion, stage, commit, branch, worktree, or other Git mutation was run.
- The only manual write operations were `apply_patch` calls against the two paths in `uniqueWriteSet`; no shell or
  Python file write was used. Maven wrote only normal ignored build outputs.
- Targeted final Cloud status: `?? src/main/java/com/bot/dhxy/service/PlayerStateService.java` on existing branch
  `navigation-migration`; targeted final DHXY status: the unique report is `??` on existing branch
  `thin-client-design`. The Cloud target was already under the shared untracked `src/main/java/com/bot/**` tree before
  this task; the report did not exist before the claim.
- No CR271, `docs/ACTIVE_WORK.md`, migration matrix, dashboard, test, config, schema, caller, or other Java file was
  modified by this Worker.

无已批准业务差异；按 committed `0114604e` 基线等价迁移。等待父级独立 review。

## Parent Source Review #1 - APPROVED - 2026-07-14T05:00:00-04:00

父级独立从 committed `0114604e` 与当前 Cloud 抽取复核，结论
`P0=0 / P1=0 / P2=0`：四个常量声明逐 token exact；完整
`isSupplyNeededFromSnapshot(BufferedImage,String,int,int,boolean,int)` source/target SHA-256 均为
`d2960858df93f51625cdbef7e1c5393541dbd43b27da0bc7efe763d9e4dd9ef7`，方法定义恰一处。
父级确认原派单中的 `BARS_SCAN_Y_OFFSET/BARS_SCAN_HEIGHT` 是描述错误，真实基线使用已在目标存在的
`BARS_SCAN_LEFT_X/BARS_SCAN_TOP_Y`；BQ 没有发明 alias，而是忠实复制基线 token。文件 SHA-256 为
`17e98407cecbb7bf364d6532929f41fe3d215ea3bf62189361107f5beeeff10a`，与 BQ 交付一致；
Cloud `mvn -q compile` exit 0。没有 caller、capture/I/O、remote/input、owner/session/ledger/TTL/retry。

本 supply-needed CPU leaf `SOURCE APPROVED`。**无已批准业务差异；按 `0114604e` 基线等价迁移。**
