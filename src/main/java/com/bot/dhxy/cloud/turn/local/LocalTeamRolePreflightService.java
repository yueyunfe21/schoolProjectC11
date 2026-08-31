package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.cloud.turn.local.dialog.DialogStoryAdvanceLocalMacroMechanics;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.service.UICleanerService;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.observation.DialogFramePresenceMechanics;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

/**
 * Local-only team-role probe used before a multi-window task starts.
 *
 * <p>This service owns only the fixed visual mechanics: it opens the team panel with {@code Alt+T}, determines
 * solo/grouped state from the normal-world mini-map magnifier, and matches the group panel's local leader buttons.
 * It sends the resulting role as metadata to Cloud; no tooltip image, OCR payload, Cloud probe, or retry loop is
 * involved. Every input/capture is scoped to the supplied native window and serialized as one atomic sequence.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalTeamRolePreflightService {

    private static final int BASE_WINDOW_WIDTH = 1024;
    private static final int BASE_WINDOW_HEIGHT = 768;

    // Calibrated from the verified hwnd-3AB11B6 frame: screen (467,260)-(1210,617) over base (323,27),
    // i.e. physical client ROI (144,233)-(887,590) at 1036x783. Values below are normalized to 1024x768.
    private static final Rect TEAM_PANEL_ROI = new Rect(142, 229, 735, 350);
    private static final Rect DIALOG_ROI = new Rect(250, 312, 529, 208);
    private static final Rect MINIMAP_MAGNIFIER_ROI = new Rect(196, 65, 20, 22);
    private static final String MINIMAP_MAGNIFIER_TEMPLATE = "images/template/map/minimap_visible_anchor.png";
    private static final String DISMISS_TEAM_TEMPLATE = "images/template/team/jiesan.png";
    private static final String TRANSFER_LEADER_TEMPLATE = "images/template/team/transfer_leader_button.png";
    private static final int TEAM_ANCHOR_X = 644;
    private static final int TEAM_ANCHOR_Y = 91;
    private static final int TEAM_ANCHOR_SIZE = 10;
    /**
     * 模板标定基线：全仓所有定尺寸模板都是在游戏窗口 1036x783 下截的，匹配过程不做缩放。
     * ROI 会跟着窗口缩放（见 {@link Rect#scale}），模板不会——窗口一旦偏离这个尺寸，全部模板同时失配。
     */
    private static final int CALIBRATED_WINDOW_WIDTH = 1036;
    private static final int CALIBRATED_WINDOW_HEIGHT = 783;

    /** UI 侧据此把尺寸漂移从普通失败里认出来并弹告警；改文案时必须同步 MainWindowController。 */
    public static final String WINDOW_SIZE_DRIFT_MARKER = "游戏窗口尺寸已偏离模板标定基线";

    private static final double TEMPLATE_THRESHOLD = 0.85D;
    private static final long PANEL_PROBE_TIMEOUT_MS = 5_000L;
    private static final long PANEL_PROBE_INTERVAL_MS = 500L;
    private static final long DISMISS_SETTLE_MS = 220L;
    private static final int TITLE_BAR_ACTIVATION_Y = 7;

    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final BoundWindowCaptureService captureService;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final WindowFocusService windowFocusService;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final UICleanerService uiCleanerService;
    private final DialogFramePresenceMechanics dialogFramePresenceMechanics = new DialogFramePresenceMechanics();
    private final DialogStoryAdvanceLocalMacroMechanics dialogStoryAdvanceLocalMacroMechanics;

    /**
     * Retained only for the existing start API. This local probe is bounded to one panel-open and two dismissal
     * clicks per window, so callers must not wait on a role/OCR deadline.
     *
     * @return a monotonic value compatible with existing callers
     */
    public long newRoleResolutionDeadlineNanos() {
        return Long.MAX_VALUE;
    }

    public Map<String, Preflight> prepareBatch(List<WindowRuntimeContext> contexts, String localTeamSessionKey) {
        return prepareBatch(contexts, localTeamSessionKey, () -> false, Long.MAX_VALUE);
    }

    public Map<String, Preflight> prepareBatch(
            List<WindowRuntimeContext> contexts,
            String localTeamSessionKey,
            BooleanSupplier cancelled) {
        return prepareBatch(contexts, localTeamSessionKey, cancelled, Long.MAX_VALUE);
    }

    /**
     * Resolves roles for a selected local batch. A single selected window retains the normal solo/grouped probe.
     * A multi-window batch restores the historical same-team invariant: the exact {@code 10x10} team anchor is
     * hashed before any panel input, and leader templates are resolved independently inside each equal-hash group.
     *
     * @param contexts exact selected window contexts
     * @param localTeamSessionKey batch identifier retained for the start contract
     * @param cancelled true when the owner paused or stopped this start command
     * @param ignoredDeadlineNanos compatibility argument; the local batch has its own five-second bound
     * @return role metadata keyed by window id; cancellation returns an empty map
     */
    public Map<String, Preflight> prepareBatch(
            List<WindowRuntimeContext> contexts,
            String localTeamSessionKey,
            BooleanSupplier cancelled,
            long ignoredDeadlineNanos) {
        if (contexts == null || contexts.isEmpty() || cancelled.getAsBoolean()) {
            return Map.of();
        }
        List<WindowRuntimeContext> selected = contexts.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(WindowRuntimeContext::getWindowId))
                .toList();
        if (selected.isEmpty()) {
            return Map.of();
        }
        clearStartupUiBlockers(selected, cancelled);
        if (cancelled.getAsBoolean()) {
            return Map.of();
        }
        if (selected.size() == 1) {
            WindowRuntimeContext context = selected.get(0);
            Role role = detectRole(context, cancelled);
            return role == null ? Map.of() : Map.of(context.getWindowId(), Preflight.completed(context.getWindowId(), role));
        }
        return detectGroupedBatch(selected, cancelled);
    }

    /**
     * Restores the clean-screen precondition before {@code Alt+T}. Generic panels are closed through the
     * existing exact-window cleaner; a structurally present story dialog is advanced through the existing
     * baseline mechanical click. Neither operation decides a role or changes the team-panel matcher.
     */
    private void clearStartupUiBlockers(List<WindowRuntimeContext> contexts, BooleanSupplier cancelled) {
        for (WindowRuntimeContext context : contexts) {
            if (cancelled.getAsBoolean()) {
                return;
            }
            windowTaskContextHolder.callWith(context, uiCleanerService::closeAllGenericWindows);
            WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context).orElse(null);
            if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
                throw new PreflightTimeoutException("本地启动清场无法读取窗口绑定，未启动任务");
            }
            BufferedImage dialog = capture(binding, DIALOG_ROI);
            boolean dialogPresent;
            try {
                dialogPresent = dialogFramePresenceMechanics.isPresent(dialog);
            } finally {
                if (dialog != null) {
                    dialog.flush();
                }
            }
            if (!dialogPresent) {
                continue;
            }
            boolean advanced = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                    "team-role:startup-dialog-clear:" + context.getWindowId(), context, binding,
                    () -> dialogStoryAdvanceLocalMacroMechanics.advanceStoryDialog(binding).status()
                            == DialogStoryAdvanceLocalMacroMechanics.Status.ADVANCED).isCompleted();
            if (!advanced || cancelled.getAsBoolean()) {
                throw new PreflightTimeoutException("本地启动对话框清理失败，未启动任务");
            }
            log.info("local team-role startup dialog cleared: windowId={}", context.getWindowId());
        }
    }

    /**
     * Opens every team panel before matching. Physical key presses remain serialized by {@link InputSequences},
     * while HWND captures and template matching deliberately run in parallel. Equal raw anchor hashes establish
     * same-team membership; a leader button may only assign members inside that exact hash group.
     */
    private Map<String, Preflight> detectGroupedBatch(List<WindowRuntimeContext> contexts, BooleanSupplier cancelled) {
        Map<String, ProbeTarget> targets = new LinkedHashMap<>();
        Map<String, List<ProbeTarget>> targetsByGroup = new LinkedHashMap<>();
        try {
            for (WindowRuntimeContext context : contexts) {
                if (cancelled.getAsBoolean()) {
                    return Map.of();
                }
                WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context).orElse(null);
                if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
                    throw new PreflightTimeoutException("本地队伍菜单无法读取窗口绑定，未启动任务");
                }
                String anchorHash = captureAnchorHash(binding);
                if (anchorHash == null) {
                    throw new PreflightTimeoutException("本地队伍头像锚点读取失败，未启动任务");
                }
                String groupHash = "anchor-" + anchorHash;
                boolean opened = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                        "team-role:local-panel-open:" + context.getWindowId(), context, binding, () -> {
                            if (cancelled.getAsBoolean()
                                    || !ensureForegroundForPhysicalShortcut(context, binding)) {
                                return false;
                            }
                            inputProvider.pressAltT();
                            return true;
                        }).isCompleted();
                if (!opened) {
                    throw new PreflightTimeoutException("本地队伍菜单无法激活目标窗口，未启动任务");
                }
                if (cancelled.getAsBoolean()) {
                    return Map.of();
                }
                ProbeTarget target = new ProbeTarget(context, binding, groupHash);
                targets.put(context.getWindowId(), target);
                targetsByGroup.computeIfAbsent(groupHash, ignored -> new java.util.ArrayList<>()).add(target);
            }

            String windowSizeDrift = describeWindowSizeDrift(
                    targets.values().stream().map(ProbeTarget::binding).toList());
            if (windowSizeDrift != null) {
                log.warn("local team-role preflight sees drifted game window size: {}", windowSizeDrift);
            }

            Map<String, AtomicReference<String>> leaderByGroup = new LinkedHashMap<>();
            targetsByGroup.keySet().forEach(groupHash ->
                    leaderByGroup.put(groupHash, new AtomicReference<>()));
            List<CompletableFuture<Void>> probes = targets.values().stream()
                    .map(target -> CompletableFuture.runAsync(() -> waitForLeaderMatch(
                            target, leaderByGroup.get(target.groupHash()), cancelled)))
                    .toList();
            CompletableFuture.allOf(probes.toArray(CompletableFuture[]::new)).join();
            if (cancelled.getAsBoolean()) {
                return Map.of();
            }
            Map<String, Preflight> results = new LinkedHashMap<>();
            boolean leaderResolved = false;
            for (Map.Entry<String, List<ProbeTarget>> entry : targetsByGroup.entrySet()) {
                String groupHash = entry.getKey();
                String winner = leaderByGroup.get(groupHash).get();
                List<String> windowIds = entry.getValue().stream()
                        .map(target -> target.context().getWindowId())
                        .toList();
                if (winner == null) {
                    windowIds.forEach(windowId ->
                            results.put(windowId, Preflight.completed(windowId, Role.MEMBER)));
                    log.info("local anchor group has no selected leader; combat broadcast disabled: groupHash={} windows={}",
                            groupHash, windowIds);
                    continue;
                }
                leaderResolved = true;
                Map<String, Role> roles = assignGroupedRoles(windowIds, winner);
                roles.forEach((windowId, role) ->
                        results.put(windowId, Preflight.completed(windowId, role, groupHash)));
                log.info("local anchor group resolved: groupHash={} leaderWindowId={} memberCount={}",
                        groupHash, winner, roles.size() - 1);
            }
            if (!leaderResolved) {
                String reason = "本地队伍菜单在 5 秒内未命中队长按钮，未启动任务";
                throw new PreflightTimeoutException(
                        windowSizeDrift == null ? reason : reason + "。" + windowSizeDrift);
            }
            return Map.copyOf(results);
        } finally {
            // A losing probe never owns input. Closing is a short, serialized Alt+T sequence after all capture
            // workers have stopped, so it cannot cross a template match or leak an open panel into task startup.
            targets.values().forEach(target -> closePanel(target, cancelled));
        }
    }

    private void waitForLeaderMatch(
            ProbeTarget target,
            AtomicReference<String> leaderWindowId,
            BooleanSupplier cancelled) {
        long deadlineNanos = System.nanoTime() + java.time.Duration.ofMillis(PANEL_PROBE_TIMEOUT_MS).toNanos();
        while (!cancelled.getAsBoolean() && leaderWindowId.get() == null && System.nanoTime() < deadlineNanos) {
            BufferedImage panel = capture(target.binding(), TEAM_PANEL_ROI);
            try {
                double[] dismissMatch = find(panel, DISMISS_TEAM_TEMPLATE);
                double[] transferMatch = find(panel, TRANSFER_LEADER_TEMPLATE);
                persistPanelProbeEvidence(target.context(), panel, dismissMatch, transferMatch);
                if ((dismissMatch != null || transferMatch != null)
                        && leaderWindowId.compareAndSet(null, target.context().getWindowId())) {
                    return;
                }
            } finally {
                if (panel != null) {
                    panel.flush();
                }
            }
            if (!sleep(PANEL_PROBE_INTERVAL_MS, cancelled)) {
                return;
            }
        }
    }

    private void closePanel(ProbeTarget target, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            return;
        }
        inputSequences.submitFrozenExactWindowExclusiveAndWait(
                "team-role:local-panel-close:" + target.context().getWindowId(), target.context(), target.binding(), () -> {
                    if (!ensureForegroundForPhysicalShortcut(target.context(), target.binding())) {
                        return false;
                    }
                    // This branch only runs after a grouped batch elected a leader. A second Alt+T closes the
                    // panel; a missed minimap template must never turn into a world right-click here.
                    inputProvider.pressAltT();
                    return !cancelled.getAsBoolean();
                });
    }

    /**
     * Guarantees that a driver shortcut cannot land on a different foreground window. The ordinary Windows
     * focus API remains the first attempt in the input worker; when Windows rejects it, one physical title-bar
     * click activates the same frozen HWND without sending gameplay input into the client area.
     */
    private boolean ensureForegroundForPhysicalShortcut(
            WindowRuntimeContext context,
            WindowNativeBinding binding) {
        if (windowFocusService.isForeground(binding)) {
            return true;
        }
        int x = binding.getX() + binding.getWidth() / 2;
        int y = binding.getY() + TITLE_BAR_ACTIVATION_Y;
        inputProvider.clickLeft(x, y, (int) DISMISS_SETTLE_MS);
        boolean focused = windowFocusService.isForeground(binding);
        log.info("local team-role physical foreground activation: windowId={} hwnd={} point=({}, {}) focused={}",
                context.getWindowId(), binding.getNativeHandle(), x, y, focused);
        return focused;
    }

    static Map<String, Role> assignGroupedRoles(List<String> windowIds, String leaderWindowId) {
        if (leaderWindowId == null || windowIds == null || !windowIds.contains(leaderWindowId)) {
            throw new IllegalArgumentException("grouped leader must be one of the selected windows");
        }
        Map<String, Role> roles = new LinkedHashMap<>();
        windowIds.forEach(windowId -> roles.put(windowId, windowId.equals(leaderWindowId) ? Role.LEADER : Role.MEMBER));
        return Map.copyOf(roles);
    }

    /**
     * Attach historical raw-anchor groups to roles that were retained or fixed without opening the team panel.
     * Only hashes containing a locally confirmed leader are published, so external-leader members keep their own
     * combat detection and can never receive another team's broadcast.
     *
     * @param contexts exact selected window contexts
     * @param preflights already resolved role facts keyed by window id
     * @param cancelled true when the owning start command has been invalidated
     * @return the same role facts with a group hash only for windows sharing a confirmed local leader anchor
     */
    public Map<String, Preflight> attachConfirmedLeaderAnchorGroups(
            List<WindowRuntimeContext> contexts,
            Map<String, Preflight> preflights,
            BooleanSupplier cancelled) {
        if (preflights == null || preflights.isEmpty()) {
            return Map.of();
        }
        Map<String, String> capturedGroupByWindow = new LinkedHashMap<>();
        for (WindowRuntimeContext context : contexts == null ? List.<WindowRuntimeContext>of() : contexts) {
            if (context == null || cancelled.getAsBoolean()) {
                continue;
            }
            Preflight preflight = preflights.get(context.getWindowId());
            if (preflight == null || preflight.role() == Role.SOLO || preflight.groupHash() != null) {
                continue;
            }
            WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context).orElse(null);
            String anchorHash = binding == null ? null : captureAnchorHash(binding);
            if (anchorHash == null) {
                log.warn("local team anchor unavailable; broadcast remains disabled: windowId={}",
                        context.getWindowId());
                continue;
            }
            capturedGroupByWindow.put(context.getWindowId(), "anchor-" + anchorHash);
        }

        return applyConfirmedLeaderAnchorGroups(preflights, capturedGroupByWindow);
    }

    static Map<String, Preflight> applyConfirmedLeaderAnchorGroups(
            Map<String, Preflight> preflights,
            Map<String, String> capturedGroupByWindow) {
        Set<String> confirmedLeaderGroups = new LinkedHashSet<>();
        preflights.values().stream()
                .filter(preflight -> preflight.role() == Role.LEADER && preflight.groupHash() != null)
                .map(Preflight::groupHash)
                .forEach(confirmedLeaderGroups::add);
        preflights.values().stream()
                .filter(preflight -> preflight.role() == Role.LEADER)
                .map(preflight -> capturedGroupByWindow.get(preflight.windowId()))
                .filter(Objects::nonNull)
                .forEach(confirmedLeaderGroups::add);

        Map<String, Preflight> grouped = new LinkedHashMap<>();
        preflights.forEach((windowId, preflight) -> {
            String groupHash = preflight.groupHash() != null
                    ? preflight.groupHash()
                    : capturedGroupByWindow.get(windowId);
            boolean publishGroup = groupHash != null && confirmedLeaderGroups.contains(groupHash)
                    && preflight.role() != Role.SOLO;
            grouped.put(windowId, publishGroup
                    ? Preflight.completed(windowId, preflight.role(), groupHash)
                    : Preflight.completed(windowId, preflight.role()));
        });
        return Map.copyOf(grouped);
    }

    /** No Cloud-OCR retry exists anymore; retained for the existing rejected-start call site. */
    public Preflight recaptureRepresentative(
            WindowRuntimeContext context,
            Preflight previous,
            BooleanSupplier cancelled,
            long ignoredDeadlineNanos) {
        return previous;
    }

    private Role detectRole(WindowRuntimeContext context, BooleanSupplier cancelled) {
        WindowNativeBinding binding = bindingRefreshService.refreshAndCommit(context).orElse(null);
        if (binding == null || !binding.hasNativeHandle() || !binding.hasGeometry()) {
            log.warn("local team-role panel probe unavailable: windowId={} reason=binding-unavailable", context.getWindowId());
            return Role.MEMBER;
        }
        Role[] resolved = new Role[]{Role.MEMBER};
        boolean[] panelStateResolved = new boolean[1];
        boolean completed = inputSequences.submitFrozenExactWindowExclusiveAndWait(
                "team-role:local-panel:" + context.getWindowId(), context, binding, () -> {
                    if (cancelled.getAsBoolean()) {
                        return false;
                    }
                    inputProvider.pressAltT();
                    Boolean minimapVisible = waitForPanelState(binding, cancelled);
                    BufferedImage panel = null;
                    try {
                        if (minimapVisible == null) {
                            // A capture failure/time-out is not proof that the magnifier disappeared and must
                            // block task startup rather than silently assigning this window as a member.
                            panelStateResolved[0] = false;
                        } else {
                            panelStateResolved[0] = true;
                            if (minimapVisible) {
                                panel = capture(binding, TEAM_PANEL_ROI);
                            }
                            resolved[0] = classifyPanel(minimapVisible, isLeaderPanel(panel));
                        }
                    } finally {
                        if (panel != null) {
                            panel.flush();
                        }
                    }
                    if (resolved[0] == Role.SOLO) {
                        closePanelWithAltTThenRecover(binding, cancelled);
                    } else {
                        inputProvider.pressAltT();
                    }
                    return !cancelled.getAsBoolean();
                }).isCompleted();
        if (cancelled.getAsBoolean()) {
            return null;
        }
        if (!completed || !panelStateResolved[0]) {
            throw new PreflightTimeoutException(PANEL_PROBE_TIMEOUT_MS);
        }
        log.info("local team-role panel resolved: windowId={} role={}", context.getWindowId(), resolved[0]);
        return resolved[0];
    }

    /**
     * Waits up to three seconds for a readable minimap state after Alt+T. A visible magnifier means grouped;
     * an absent magnifier means solo. The first readable frame ends the wait immediately.
     */
    private Boolean waitForPanelState(WindowNativeBinding binding, BooleanSupplier cancelled) {
        long deadlineNanos = System.nanoTime() + java.time.Duration.ofMillis(PANEL_PROBE_TIMEOUT_MS).toNanos();
        while (!cancelled.getAsBoolean() && System.nanoTime() < deadlineNanos) {
            BufferedImage magnifier = capture(binding, MINIMAP_MAGNIFIER_ROI);
            try {
                if (magnifier != null) {
                    return matches(magnifier, MINIMAP_MAGNIFIER_TEMPLATE);
                }
            } finally {
                if (magnifier != null) {
                    magnifier.flush();
                }
            }
            if (!sleep(PANEL_PROBE_INTERVAL_MS, cancelled)) {
                return null;
            }
        }
        return null;
    }

    private void closePanelWithAltTThenRecover(WindowNativeBinding binding, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            return;
        }
        inputProvider.pressAltT();
        if (!sleep(DISMISS_SETTLE_MS, cancelled) || minimapIsVisible(binding)) {
            return;
        }
        dismissPanelUntilMagnifierReturns(binding, cancelled);
    }

    /** Applies the user-approved right-click recovery only when the normal second Alt+T close did not restore UI. */
    private void dismissPanelUntilMagnifierReturns(WindowNativeBinding binding, BooleanSupplier cancelled) {
        for (int attempt = 0; attempt < 2 && !cancelled.getAsBoolean(); attempt++) {
            int jitter = attempt == 0 ? 0 : ThreadLocalRandom.current().nextInt(-24, 25);
            int x = binding.getX() + binding.getWidth() / 2 + jitter;
            int y = binding.getY() + binding.getHeight() / 2 + jitter;
            inputProvider.clickRight(x, y, 0);
            if (!sleep(DISMISS_SETTLE_MS, cancelled)) {
                return;
            }
            if (minimapIsVisible(binding)) {
                return;
            }
        }
        log.warn("local team-role panel may remain open after two right-click dismiss attempts: hwnd={}",
                binding.getNativeHandle());
    }

    private boolean isLeaderPanel(BufferedImage panel) {
        return panel != null && (matches(panel, DISMISS_TEAM_TEMPLATE) || matches(panel, TRANSFER_LEADER_TEMPLATE));
    }

    private boolean minimapIsVisible(WindowNativeBinding binding) {
        BufferedImage magnifier = capture(binding, MINIMAP_MAGNIFIER_ROI);
        try {
            return magnifier != null && matches(magnifier, MINIMAP_MAGNIFIER_TEMPLATE);
        } finally {
            if (magnifier != null) {
                magnifier.flush();
            }
        }
    }

    static Role classifyPanel(boolean minimapVisibleAfterAltT, boolean leaderButtonVisible) {
        if (!minimapVisibleAfterAltT) {
            return Role.SOLO;
        }
        return leaderButtonVisible ? Role.LEADER : Role.MEMBER;
    }

    private boolean matches(BufferedImage source, String templatePath) {
        return find(source, templatePath) != null;
    }

    private double[] find(BufferedImage source, String templatePath) {
        if (source == null) {
            return null;
        }
        BufferedImage template = null;
        try {
            template = ImageIO.read(Path.of(templatePath).toFile());
            return template == null ? null : ImageFinder.find(source, template, TEMPLATE_THRESHOLD);
        } catch (IOException | RuntimeException failure) {
            log.warn("local team-role template match unavailable: template={} reason={}", templatePath, failure.toString());
            return null;
        } finally {
            if (template != null) {
                template.flush();
            }
        }
    }

    /**
     * Persists the exact cropped source used for the local leader-template decision. This is diagnostic evidence
     * only: it never changes matching, role assignment, or input. The latest frame is intentionally overwritten
     * per window so a failed five-second probe leaves one inspectable raw and marked image for every participant.
     */
    private void persistPanelProbeEvidence(
            WindowRuntimeContext context,
            BufferedImage panel,
            double[] dismissMatch,
            double[] transferMatch) {
        if (panel == null || context == null) {
            return;
        }
        String safeWindowId = context.getWindowId().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path directory = Path.of("images", "temp", safeWindowId).toAbsolutePath();
        Path raw = directory.resolve("team_role_panel_latest_raw.png");
        Path marked = directory.resolve("team_role_panel_latest_marked.png");
        BufferedImage evidence = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = evidence.createGraphics();
        try {
            graphics.drawImage(panel, 0, 0, null);
            markTeamTemplate(graphics, dismissMatch, Color.RED, "dismiss");
            markTeamTemplate(graphics, transferMatch, Color.YELLOW, "transfer");
            Files.createDirectories(directory);
            ImageIO.write(panel, "png", raw.toFile());
            ImageIO.write(evidence, "png", marked.toFile());
        } catch (IOException failure) {
            log.warn("local team-role probe evidence write failed: windowId={} reason={}",
                    context.getWindowId(), failure.toString());
        } finally {
            graphics.dispose();
            evidence.flush();
        }
    }

    private static void markTeamTemplate(Graphics2D graphics, double[] match, Color color, String label) {
        if (match == null) {
            return;
        }
        int x = (int) Math.round(match[0]);
        int y = (int) Math.round(match[1]);
        graphics.setColor(color);
        graphics.drawOval(x - 12, y - 12, 24, 24);
        graphics.drawString(label + String.format(" %.3f", match[2]), x + 14, y);
    }

    /**
     * 量一遍窗口尺寸，偏离标定基线就给出可直接展示给用户的告警文案。
     *
     * <p>2026-08-30 01:55 游戏客户端自行把五个窗口从 1036x783 改成 1117x840（放大 7.8%）：位置一动没动、
     * 进程从 08-26 起就没重启过、Windows 事件日志里没有任何显示模式/GPU/驱动/电源事件，纯粹是游戏自己抽了
     * 一下，两小时后又自行变了回来。后果是全部定尺寸模板一起失配，而最先撞上的就是本类的队长按钮探测，
     * 五个窗口全部失败、整批不启动。当时报给用户的却只有"队伍菜单在 5 秒内未命中队长按钮"——完全没指向
     * 真因，看上去像接线断了或云端没起来，最后是靠翻落盘证据图（按钮明明在画面里却没有命中框）再从证据 PNG
     * 的尺寸反推窗口大小才定的案。</p>
     *
     * <p>尺寸是这里唯一免费就能量到的量（绑定里现成的），所以宁可每轮都量一遍，也不要再让人做一次图像取证。
     * 注意这里只诊断、不拦截：游戏偶发抽风时把用户锁死在启动门外，比让他带着告警去试一次更糟。</p>
     *
     * @return 全部窗口都在标定尺寸上时返回 {@code null}，否则返回告警文案（以 {@link #WINDOW_SIZE_DRIFT_MARKER} 开头）。
     */
    static String describeWindowSizeDrift(Collection<WindowNativeBinding> bindings) {
        Map<String, Integer> drifted = new LinkedHashMap<>();
        for (WindowNativeBinding binding : bindings) {
            if (binding == null || !binding.hasGeometry()) {
                continue;
            }
            if (binding.getWidth() == CALIBRATED_WINDOW_WIDTH
                    && binding.getHeight() == CALIBRATED_WINDOW_HEIGHT) {
                continue;
            }
            drifted.merge(binding.getWidth() + "x" + binding.getHeight(), 1, Integer::sum);
        }
        if (drifted.isEmpty()) {
            return null;
        }
        StringBuilder observed = new StringBuilder();
        drifted.forEach((size, count) -> {
            if (observed.length() > 0) {
                observed.append("、");
            }
            observed.append(size).append('(').append(count).append("个窗口)");
        });
        return WINDOW_SIZE_DRIFT_MARKER + "：实测 " + observed + "，标定基线 "
                + CALIBRATED_WINDOW_WIDTH + "x" + CALIBRATED_WINDOW_HEIGHT
                + "。所有定尺寸模板都会失配（队长按钮、对话框、绿链、追踪器、战斗面板等），"
                + "请把游戏窗口调回 " + CALIBRATED_WINDOW_WIDTH + "x" + CALIBRATED_WINDOW_HEIGHT + " 后重试。";
    }

    private BufferedImage capture(WindowNativeBinding binding, Rect rect) {
        Rect scaled = rect.scale(binding.getWidth(), binding.getHeight());
        return captureService.captureRegion(binding, binding.getX(), binding.getY(),
                        binding.getX() + scaled.left(), binding.getY() + scaled.top(),
                        binding.getX() + scaled.right(), binding.getY() + scaled.bottom())
                .map(BoundWindowCaptureService.CaptureResult::image)
                .orElse(null);
    }

    private String captureAnchorHash(WindowNativeBinding binding) {
        BufferedImage anchor = captureService.captureRegion(binding, binding.getX(), binding.getY(),
                        binding.getX() + TEAM_ANCHOR_X, binding.getY() + TEAM_ANCHOR_Y,
                        binding.getX() + TEAM_ANCHOR_X + TEAM_ANCHOR_SIZE,
                        binding.getY() + TEAM_ANCHOR_Y + TEAM_ANCHOR_SIZE)
                .map(BoundWindowCaptureService.CaptureResult::image)
                .orElse(null);
        if (anchor == null) {
            return null;
        }
        try {
            return rawArgbSha256(anchor);
        } finally {
            anchor.flush();
        }
    }

    static String rawArgbSha256(BufferedImage image) {
        ByteBuffer pixels = ByteBuffer.allocate(image.getWidth() * image.getHeight() * Integer.BYTES);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels.putInt(image.getRGB(x, y));
            }
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pixels.array()));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    private static boolean sleep(long millis, BooleanSupplier cancelled) {
        try {
            Thread.sleep(millis);
            return !cancelled.getAsBoolean();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public enum Role { LEADER, MEMBER, SOLO }

    /** Retained only to keep callers source-compatible while tooltip/OCR retry is removed. */
    public static final class PreflightTimeoutException extends IllegalStateException {
        public PreflightTimeoutException(long timeoutMs) {
            super("队长身份识别超时（" + timeoutMs / 1_000L + " 秒），未启动任务");
        }

        public PreflightTimeoutException(String message) {
            super(message);
        }
    }

    /** Empty group/mask fields explicitly mean that Cloud must consume the local role and never OCR a tooltip. */
    public record Preflight(String windowId, Role role, String groupHash, boolean representative, String maskBase64) {
        static Preflight completed(String windowId, Role role) {
            return new Preflight(windowId, role, null, false, null);
        }

        static Preflight completed(String windowId, Role role, String groupHash) {
            return new Preflight(windowId, role, groupHash, false, null);
        }
    }

    private record Rect(int left, int top, int width, int height) {
        int right() { return left + width; }
        int bottom() { return top + height; }

        Rect scale(int actualWidth, int actualHeight) {
            int scaledLeft = Math.round(left * actualWidth / (float) BASE_WINDOW_WIDTH);
            int scaledTop = Math.round(top * actualHeight / (float) BASE_WINDOW_HEIGHT);
            int scaledRight = Math.round(right() * actualWidth / (float) BASE_WINDOW_WIDTH);
            int scaledBottom = Math.round(bottom() * actualHeight / (float) BASE_WINDOW_HEIGHT);
            return new Rect(scaledLeft, scaledTop,
                    Math.max(1, scaledRight - scaledLeft), Math.max(1, scaledBottom - scaledTop));
        }
    }

    private record ProbeTarget(WindowRuntimeContext context, WindowNativeBinding binding, String groupHash) { }
}
