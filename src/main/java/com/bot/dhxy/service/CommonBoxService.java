package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.maintenance.CommonBoxRole;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common CR120 box detector and pending-click owner.
 *
 * <p>Detection is intentionally separated from clicking. Task code calls the role-specific detect
 * methods at safe business milestones, this service stores one short-lived pending record per bound
 * window/task/role, and later maintenance/task-turn hooks consume it through the serialized input
 * queue. All coordinates stored in pending records are screen-absolute pixels derived from the
 * current bound window base.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonBoxService {

    static final int ROI_LEFT = 623;
    static final int ROI_TOP = 590;
    static final int ROI_RIGHT = 682;
    static final int ROI_BOTTOM = 618;
    static final long PENDING_TTL_MS = 30_000L;

    private static final String TEMPLATE_PATH = "images/template/common/leader_box_marker.png";
    private static final double TEMPLATE_THRESHOLD = 0.86;
    private static final String TASK_XIULUO = "xiuluo_v2";
    private static final String TASK_WUBEI = "wubei";
    private static final int CLICK_SETTLE_MS = 80;
    private static final int CLICK_DELAY_MS = 120;

    private final BotProperties botProperties;
    private final GameClientTracker tracker;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder windowTaskContextHolder;

    private final Map<String, PendingCommonBox> pendingByKey = new ConcurrentHashMap<>();
    private volatile BufferedImage cachedTemplate;

    /**
     * Run the tiny leader box ROI detection after a verified task return-home.
     *
     * @param context current leader task context.
     * @param sourceTask task code such as {@code xiuluo_v2} or {@code wubei}.
     * @param source diagnostic source for logs.
     */
    public void detectLeaderBoxAfterReturnHome(TaskExecutionContext context, String sourceTask, String source) {
        detectBox(context, sourceTask, CommonBoxRole.LEADER, source);
    }

    /**
     * Run the tiny member box ROI detection after a combat-exit signal.
     *
     * @param context current member task context.
     * @param sourceTask task code such as {@code xiuluo_v2} or {@code wubei}.
     * @param source diagnostic source for logs.
     */
    public void detectMemberBoxAfterCombatExit(TaskExecutionContext context, String sourceTask, String source) {
        detectBox(context, sourceTask, CommonBoxRole.MEMBER, source);
    }

    /**
     * Consume the current window's unexpired pending box before other maintenance work.
     *
     * @param context current task context, used for stop checks and diagnostics.
     * @param sourceTask task code that owns this consume opportunity.
     * @param source diagnostic source for logs.
     * @return true when a pending box was clicked and cleared.
     */
    public boolean consumePendingBoxIfAllowed(TaskExecutionContext context, String sourceTask, String source) {
        if (context != null) {
            context.throwIfStopRequested();
        }
        pruneExpiredPending();
        String taskKey = normalizeSupportedTask(sourceTask);
        if (taskKey == null) {
            return false;
        }
        String taskRunKey = taskRunKey(context);
        if (taskRunKey == null) {
            log.info("[common-box] consume skipped: source={} task={} reason=invalid task run",
                    source, sourceTask);
            return false;
        }
        Optional<WindowRuntimeContext> runtime = windowTaskContextHolder.rawCurrent();
        if (runtime.isEmpty()) {
            log.debug("[common-box] consume skipped: source={} task={} reason=no-window-runtime",
                    source, sourceTask);
            return false;
        }
        WindowRuntimeContext window = runtime.get();
        Optional<CommonBoxRole> maybeRole = roleFor(context);
        if (maybeRole.isEmpty()) {
            log.info("[common-box] consume skipped: source={} task={} windowId={} reason=unknown-context-role",
                    source, taskKey, window.getWindowId());
            return false;
        }
        CommonBoxRole role = maybeRole.get();
        if (!isRoleEnabled(role)) {
            clearPendingForRole(role, "switch-off:" + source);
            log.info("[common-box] consume skipped by role toggle: source={} task={} role={} windowId={}",
                    source, taskKey, role, window.getWindowId());
            return false;
        }

        String key = pendingKey(window, role, taskKey, taskRunKey);
        PendingCommonBox pending = pendingByKey.get(key);
        if (pending == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean expired = pending.expiresAtMs() <= now;
        boolean staleWindow = !sameWindow(window, pending);
        boolean staleIdentity = pending.identityEpoch() != window.getPlayerIdentityEpoch();
        boolean staleTaskRun = !pending.taskRunKey().equals(taskRunKey);
        if (expired || staleWindow || staleIdentity || staleTaskRun) {
            pendingByKey.remove(key);
            log.info("[common-box] pending expired/cleared before consume: source={} task={} role={} windowId={} ageMs={} expired={} staleWindow={} staleIdentity={} staleTaskRun={}",
                    source, taskKey, role, window.getWindowId(), now - pending.detectedAtMs(),
                    expired, staleWindow, staleIdentity, staleTaskRun);
            return false;
        }

        boolean clicked = inputSequences.moveAndClickLeft(
                "commonBox:" + role + ":" + taskKey + ":" + source,
                pending.clickX(), pending.clickY(), CLICK_SETTLE_MS, CLICK_DELAY_MS);
        if (clicked) {
            pendingByKey.remove(key);
            log.info("[common-box] COMMON_BOX_CLICKED source={} task={} role={} windowId={} hwnd={} template=({}, {}) click=({}, {}) ageMs={}",
                    source, taskKey, role, window.getWindowId(), pending.nativeWindowHandle(),
                    pending.templateX(), pending.templateY(), pending.clickX(), pending.clickY(),
                    now - pending.detectedAtMs());
            return true;
        }
        log.warn("[common-box] click failed; keep pending until TTL: source={} task={} role={} windowId={} click=({}, {})",
                source, taskKey, role, window.getWindowId(), pending.clickX(), pending.clickY());
        return false;
    }

    /**
     * Check whether the current bound window has a valid pending common-box click.
     *
     * <p>This method is read-only and exists so member safe-turn maintenance can avoid taking the
     * task turn when no box is pending. It uses the same task/run/role/window identity gates as the
     * consumer, but never sends input.</p>
     *
     * @param context current task execution context, used for the strict task-run id.
     * @param sourceTask task code that owns the pending box.
     * @return true when a pending box can be consumed by the current window/task/run.
     */
    public boolean hasPendingBoxForCurrentWindow(TaskExecutionContext context, String sourceTask) {
        pruneExpiredPending();
        String taskKey = normalizeSupportedTask(sourceTask);
        if (taskKey == null) {
            return false;
        }
        String taskRunKey = taskRunKey(context);
        if (taskRunKey == null) {
            return false;
        }
        Optional<WindowRuntimeContext> runtime = windowTaskContextHolder.rawCurrent();
        if (runtime.isEmpty()) {
            return false;
        }
        WindowRuntimeContext window = runtime.get();
        Optional<CommonBoxRole> maybeRole = roleFor(context);
        if (maybeRole.isEmpty()) {
            return false;
        }
        CommonBoxRole role = maybeRole.get();
        if (!isRoleEnabled(role)) {
            return false;
        }
        PendingCommonBox pending = pendingByKey.get(pendingKey(window, role, taskKey, taskRunKey));
        if (pending == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return pending.expiresAtMs() > now
                && sameWindow(window, pending)
                && pending.identityEpoch() == window.getPlayerIdentityEpoch()
                && pending.taskRunKey().equals(taskRunKey);
    }

    /**
     * Clear pending boxes for one role. UI switch-off paths and detect/consume skip paths use this
     * so toggles do not leave stale delayed clicks behind.
     *
     * @param role role whose pending records should be removed.
     * @param source diagnostic source for logs.
     */
    public void clearPendingForRole(CommonBoxRole role, String source) {
        if (role == null) {
            return;
        }
        int cleared = 0;
        Iterator<Map.Entry<String, PendingCommonBox>> iterator = pendingByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingCommonBox> entry = iterator.next();
            if (entry.getValue().role() == role) {
                iterator.remove();
                cleared++;
            }
        }
        if (cleared > 0) {
            log.info("[common-box] pending cleared by role: role={} source={} count={}", role, source, cleared);
        }
    }

    private void detectBox(TaskExecutionContext context,
                           String sourceTask,
                           CommonBoxRole role,
                           String source) {
        pruneExpiredPending();
        String taskKey = normalizeSupportedTask(sourceTask);
        Optional<WindowRuntimeContext> runtime = windowTaskContextHolder.rawCurrent();
        if (taskKey == null || runtime.isEmpty()) {
            return;
        }
        String taskRunKey = taskRunKey(context);
        if (taskRunKey == null) {
            log.info("[common-box] detection skipped: source={} task={} role={} reason=invalid task run",
                    source, taskKey, role);
            return;
        }
        WindowRuntimeContext window = runtime.get();
        if (!isRoleEnabled(role)) {
            clearPendingForRole(role, "switch-off:" + source);
            log.info("[common-box] detection skipped by role toggle: source={} task={} role={} windowId={}",
                    source, taskKey, role, window.getWindowId());
            return;
        }
        Optional<CommonBoxRole> maybeActualRole = roleFor(context);
        if (maybeActualRole.isEmpty()) {
            pendingByKey.remove(pendingKey(window, role, taskKey, taskRunKey));
            log.info("[common-box] detection skipped: source={} task={} requestedRole={} windowId={} reason=unknown-context-role",
                    source, taskKey, role, window.getWindowId());
            return;
        }
        CommonBoxRole actualRole = maybeActualRole.get();
        if (actualRole != role) {
            pendingByKey.remove(pendingKey(window, role, taskKey, taskRunKey));
            log.info("[common-box] detection skipped: requested role does not match task context role: source={} task={} requestedRole={} actualRole={} windowId={}",
                    source, taskKey, role, actualRole, window.getWindowId());
            return;
        }
        windowTaskContextHolder.runWith(window,
                () -> detectAndRecord(context, window, taskKey, taskRunKey, role, source));
    }

    private void detectAndRecord(TaskExecutionContext context,
                                 WindowRuntimeContext window,
                                 String taskKey,
                                 String taskRunKey,
                                 CommonBoxRole role,
                                 String source) {
        if (context != null && context.isStopRequested()) {
            return;
        }
        WindowNativeBinding binding = window.getNativeBinding();
        if (binding == null || !binding.hasGeometry()) {
            log.info("[common-box] detection skipped: source={} task={} role={} windowId={} reason=no-geometry",
                    source, taskKey, role, window.getWindowId());
            return;
        }
        int x1 = binding.getX() + ROI_LEFT;
        int y1 = binding.getY() + ROI_TOP;
        int x2 = binding.getX() + ROI_RIGHT;
        int y2 = binding.getY() + ROI_BOTTOM;
        BufferedImage raw = tracker.captureToMemory("common-box:" + source, x1, y1, x2, y2);
        if (raw == null) {
            log.info("[common-box] detection missed: source={} task={} role={} windowId={} reason=capture-null roi=({}, {})-({}, {})",
                    source, taskKey, role, window.getWindowId(), ROI_LEFT, ROI_TOP, ROI_RIGHT, ROI_BOTTOM);
            return;
        }
        try {
            BufferedImage template = cachedTemplate();
            if (template == null) {
                log.warn("[common-box] detection skipped: source={} task={} role={} windowId={} reason=template-unavailable path={}",
                        source, taskKey, role, window.getWindowId(), TEMPLATE_PATH);
                return;
            }
            double[] match = ImageFinder.find(raw, template, TEMPLATE_THRESHOLD);
            if (match == null) {
                log.info("[common-box] detection missed: source={} task={} role={} windowId={} roi=({}, {})-({}, {}) threshold={}",
                        source, taskKey, role, window.getWindowId(), ROI_LEFT, ROI_TOP, ROI_RIGHT, ROI_BOTTOM,
                        TEMPLATE_THRESHOLD);
                return;
            }
            int templateX = x1 + (int) Math.round(match[0]);
            int templateY = y1 + (int) Math.round(match[1]);
            long now = System.currentTimeMillis();
            PendingCommonBox pending = new PendingCommonBox(
                    window.getWindowId(),
                    binding.getNativeHandle(),
                    taskKey,
                    taskRunKey,
                    role,
                    now,
                    now + PENDING_TTL_MS,
                    templateX,
                    templateY,
                    templateX,
                    templateY,
                    window.getPlayerIdentityEpoch(),
                    source);
            pendingByKey.put(pendingKey(window, role, taskKey, taskRunKey), pending);
            log.info("[common-box] pending created: source={} task={} role={} windowId={} hwnd={} taskRun={} score={} template=({}, {}) click=({}, {}) expiresInMs={}",
                    source, taskKey, role, window.getWindowId(), binding.getNativeHandle(), taskRunKey, match[2],
                    templateX, templateY, pending.clickX(), pending.clickY(), PENDING_TTL_MS);
        } catch (Exception e) {
            log.warn("[common-box] detection failed: source={} task={} role={} windowId={} reason={}",
                    source, taskKey, role, window.getWindowId(), e.getMessage(), e);
        } finally {
            raw.flush();
        }
    }

    private BufferedImage cachedTemplate() {
        BufferedImage template = cachedTemplate;
        if (template != null) {
            return template;
        }
        synchronized (this) {
            if (cachedTemplate != null) {
                return cachedTemplate;
            }
            try {
                cachedTemplate = ImageIO.read(Path.of(TEMPLATE_PATH).toFile());
                return cachedTemplate;
            } catch (Exception e) {
                log.warn("[common-box] template load failed: path={} reason={}", TEMPLATE_PATH, e.getMessage(), e);
                return null;
            }
        }
    }

    private Optional<CommonBoxRole> roleFor(TaskExecutionContext context) {
        if (context == null || context.getWindowRole() == null) {
            return Optional.empty();
        }
        String role = context.getWindowRole().trim();
        if ("LEADER".equalsIgnoreCase(role)) {
            return Optional.of(CommonBoxRole.LEADER);
        }
        if ("MEMBER".equalsIgnoreCase(role)) {
            return Optional.of(CommonBoxRole.MEMBER);
        }
        return Optional.empty();
    }

    private boolean isRoleEnabled(CommonBoxRole role) {
        if (role == CommonBoxRole.MEMBER) {
            return botProperties.isMemberCommonBoxEnabled();
        }
        return botProperties.isLeaderCommonBoxEnabled();
    }

    private String normalizeSupportedTask(String taskCode) {
        if (taskCode == null) {
            return null;
        }
        String normalized = taskCode.trim().toLowerCase();
        if (TASK_XIULUO.equals(normalized) || TASK_WUBEI.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String pendingKey(WindowRuntimeContext window, CommonBoxRole role, String taskKey, String taskRunKey) {
        String windowId = window == null ? "unknown" : window.getWindowId();
        String hwnd = window == null || window.getNativeBinding() == null
                ? "none"
                : String.valueOf(window.getNativeBinding().getNativeHandle());
        return windowId + "|" + hwnd + "|" + role + "|" + taskKey + "|" + taskRunKey;
    }

    private String taskRunKey(TaskExecutionContext context) {
        if (context == null) {
            return null;
        }
        long taskRunId = context.getTaskRunId();
        if (taskRunId <= 0L) {
            return null;
        }
        return String.valueOf(taskRunId);
    }

    private void pruneExpiredPending() {
        long now = System.currentTimeMillis();
        int cleared = 0;
        Iterator<Map.Entry<String, PendingCommonBox>> iterator = pendingByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingCommonBox> entry = iterator.next();
            if (entry.getValue().expiresAtMs() <= now) {
                iterator.remove();
                cleared++;
            }
        }
        if (cleared > 0) {
            log.info("[common-box] expired pending pruned count={}", cleared);
        }
    }

    private boolean sameWindow(WindowRuntimeContext window, PendingCommonBox pending) {
        if (window == null || pending == null) {
            return false;
        }
        String currentHandle = window.getNativeBinding() == null ? null : window.getNativeBinding().getNativeHandle();
        return pending.windowId().equals(window.getWindowId())
                && String.valueOf(pending.nativeWindowHandle()).equals(String.valueOf(currentHandle));
    }

    private record PendingCommonBox(String windowId,
                                    String nativeWindowHandle,
                                    String sourceTask,
                                    String taskRunKey,
                                    CommonBoxRole role,
                                    long detectedAtMs,
                                    long expiresAtMs,
                                    int templateX,
                                    int templateY,
                                    int clickX,
                                    int clickY,
                                    long identityEpoch,
                                    String source) {
    }
}
