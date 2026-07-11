package com.bot.dhxy.service;

import com.bot.dhxy.cloud.task.ImageProcessorService;
import com.bot.dhxy.cloud.task.SummonSkillCloudDecision;
import com.bot.dhxy.cloud.task.SummonSkillCloudDecisionService;
import com.bot.dhxy.cloud.task.SummonSkillCloudRequest;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.model.maintenance.TaskMaintenanceResult;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Focused summon-skill maintenance service for the current bound game window.
 *
 * <p>The service opens the summon panel, moves it into a detectable position if needed, inspects the
 * tail skill slots by hovering yellow tooltips, and forgets low-tier normal skills. All public entry
 * points either submit one exclusive input sequence or detect that they are already running inside
 * the input worker and then use direct {@link InputProvider} calls to avoid nested queue deadlock.</p>
 */
@Slf4j
@Service
public class SummonSkillService {

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final ObjectProvider<TaskMaintenanceService> taskMaintenanceServiceProvider;
    private final WindowScopedTempPath windowScopedTempPath;
    private final ImageProcessorService imageProcessorService;
    private final SummonSkillCloudDecisionService summonSkillCloudDecisionService;
    private final WindowTaskContextHolder windowTaskContextHolder;

    @Autowired
    public SummonSkillService(GameClientTracker tracker,
                              CoordinateHelper coordinateHelper,
                              InputSequences inputSequences,
                              InputProvider inputProvider,
                              UICleanerService uiCleanerService,
                              DialogService dialogService,
                              ObjectProvider<TaskMaintenanceService> taskMaintenanceServiceProvider,
                              WindowScopedTempPath windowScopedTempPath,
                              ImageProcessorService imageProcessorService,
                              SummonSkillCloudDecisionService summonSkillCloudDecisionService,
                              WindowTaskContextHolder windowTaskContextHolder) {
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.inputSequences = inputSequences;
        this.inputProvider = inputProvider;
        this.uiCleanerService = uiCleanerService;
        this.dialogService = dialogService;
        this.taskMaintenanceServiceProvider = taskMaintenanceServiceProvider;
        this.windowScopedTempPath = windowScopedTempPath;
        this.imageProcessorService = imageProcessorService;
        this.summonSkillCloudDecisionService = summonSkillCloudDecisionService;
        this.windowTaskContextHolder = windowTaskContextHolder;
    }

    public SummonSkillService(GameClientTracker tracker,
                              CoordinateHelper coordinateHelper,
                              InputSequences inputSequences,
                              InputProvider inputProvider,
                              UICleanerService uiCleanerService,
                              DialogService dialogService,
                              WindowScopedTempPath windowScopedTempPath,
                              ImageProcessorService imageProcessorService,
                              SummonSkillCloudDecisionService summonSkillCloudDecisionService,
                              WindowTaskContextHolder windowTaskContextHolder) {
        this(tracker,
                coordinateHelper,
                inputSequences,
                inputProvider,
                uiCleanerService,
                dialogService,
                new ObjectProvider<>() {
                    @Override
                    public TaskMaintenanceService getObject(Object... args) {
                        return null;
                    }

                    @Override
                    public TaskMaintenanceService getObject() {
                        return null;
                    }

                    @Override
                    public TaskMaintenanceService getIfAvailable() {
                        return null;
                    }

                    @Override
                    public TaskMaintenanceService getIfUnique() {
                        return null;
                    }
                },
                windowScopedTempPath,
                imageProcessorService,
                summonSkillCloudDecisionService,
                windowTaskContextHolder);
    }

    /**
     * Captured tooltip crop and cloud-washed yellow-text debug image.
     */
    private static class YellowTipScan {
        private final int[] rect;
        private final String washedPath;
        private final int yellowCount;

        private YellowTipScan(int[] rect, String washedPath, int yellowCount) {
            this.rect = rect;
            this.washedPath = washedPath;
            this.yellowCount = yellowCount;
        }
    }

    private static final int DELETE_SKILL_BUTTON_X = 484;
    private static final int DELETE_SKILL_BUTTON_Y = 602;
    private static final int CONFIRM_BUTTON_SEARCH_X = 552;
    private static final int CONFIRM_BUTTON_SEARCH_Y = 494;
    private static final int CONFIRM_BUTTON_SEARCH_W = 102;
    private static final int CONFIRM_BUTTON_SEARCH_H = 15;
    private static final double FORGET_CONFIRM_MATCH_RATE = 0.82;

    private static final int NORMAL_CLICK_RANDOM_X = 4;
    private static final int NORMAL_CLICK_RANDOM_Y = 3;
    private static final int SKILL_SLOT_CLICK_RANDOM_X = 3;
    private static final int SKILL_SLOT_CLICK_RANDOM_Y = 3;
    private static final int CONFIRM_CLICK_RANDOM_X = 6;
    private static final int CONFIRM_CLICK_RANDOM_Y = 4;
    private static final int HOVER_RANDOM_X = 2;
    private static final int HOVER_RANDOM_Y = 2;
    private static final int DRAG_POINT_RANDOM_X = 3;
    private static final int DRAG_POINT_RANDOM_Y = 3;

    private static final long SELECT_SKILL_WAIT_MS = 300L;
    private static final long DELETE_DIALOG_WAIT_MS = 600L;
    private static final long FORGET_DONE_WAIT_MS = 900L;
    private static final long OPEN_SKILL_PANEL_WAIT_MS = 1_000L;
    private static final long PANEL_ANCHOR_RETRY_WAIT_MS = 800L;
    private static final long DRAG_PANEL_WAIT_MS = 600L;
    private static final long SKILL_HOVER_WAIT_MS = 700L;
    // The generated skill tooltip may arrive later than the click animation; wait before re-hovering its slot.
    private static final long ULTIMATE_CORNER_CLICK_WAIT_MS = 2_500L;
    private static final long CLEAN_ONCE_TIMEOUT_MS = 40_000L;

    private static final String ZHS_ATTRIBUTE_ANCHOR_PATH = "images/template/zhaohuanshou/ZHS_shuxing.png";
    private static final String IF8_LAYOUT_TEMPLATE_PATH = "images/template/zhaohuanshou/if8.png";
    private static final double ANCHOR_MATCH_RATE = 0.85;
    private static final double SKILL_STATUS_MATCH_RATE = 0.78;
    private static final double IF8_LAYOUT_MATCH_RATE = 0.80;
    private static final double STATIC_INACTIVE_COLOR_DISTANCE_THRESHOLD = 12.0;
    static final int[] IF8_LAYOUT_ROI = new int[]{505, 508, 532, 555};
    static final int SKILL_SLOT_BOX_SIZE = 52;
    private static final int SKILL_SLOT_BOX_HALF_SIZE = SKILL_SLOT_BOX_SIZE / 2;
    private static final int STATIC_SLOT_SCAN_PADDING = 8;

    private static final Point[] SIX_SKILL_SLOT_OFFSETS = new Point[]{
            new Point(416, 384),
            new Point(334, 430),
            new Point(335, 511),
            new Point(420, 561),
            new Point(500, 511),
            new Point(500, 432)
    };

    private static final Point[] EIGHT_SKILL_SLOT_OFFSETS = new Point[]{
            new Point(405, 364),
            new Point(339, 407),
            new Point(311, 475),
            new Point(338, 541),
            new Point(406, 584),
            new Point(475, 540),
            new Point(503, 474),
            new Point(474, 406)
    };

    private static final int GAME_WINDOW_WIDTH = 1024;
    private static final int GAME_WINDOW_HEIGHT = 768;
    private static final int MIN_ANCHOR_DISTANCE_TO_RIGHT = 337;
    private static final int DRAG_TARGET_BASE_X = 518;
    private static final int DRAG_TARGET_BASE_Y = 428;
    private static final int DRAG_TARGET_RANDOM_X = 45;
    private static final int DRAG_TARGET_RANDOM_Y = 35;
    private static final int SKILL_BUTTON_OFFSET_X = 287;
    private static final int SKILL_BUTTON_OFFSET_Y = 213;

    private static final int HOVER_TIP_OFFSET_X = 25;
    private static final int HOVER_TIP_OFFSET_Y = 0;
    private static final int HOVER_TIP_AREA_W = 237;
    private static final int HOVER_TIP_AREA_H = 123;
    private static final int ULTIMATE_CORNER_OFFSET_X = 26;
    private static final int ULTIMATE_CORNER_OFFSET_Y = -26;

    private static final int MIN_YELLOW_PIXEL_COUNT = 120;
    private static final int MAX_DELETE_SKILL_COUNT_PER_RUN = 5;

    private static final String SKILL_STATUS_NORMAL_PATH = "images/template/zhaohuanshou/status_normal.png";
    private static final String SKILL_STATUS_HIGH_PATH = "images/template/zhaohuanshou/status_high.png";
    private static final String SKILL_STATUS_ULTIMATE_PATH = "images/template/zhaohuanshou/status_ultimate.png";
    private static final String SKILL_STATUS_SEALED_PATH = "images/template/zhaohuanshou/status_sealed.png";
    private static final String SKILL_STATUS_UNOBTAINED_PATH = "images/template/zhaohuanshou/status_unobtained.png";
    private static final String SKILL_STATUS_INACTIVE_PATH = "images/template/zhaohuanshou/status_inactive.png";
    private static final String STATIC_SKILL_STATUS_SEALED_PATH = "images/template/zhaohuanshou/status_sealed1.png";
    private static final String STATIC_SKILL_STATUS_UNOBTAINED_PATH = "images/template/zhaohuanshou/status_unobtained1.png";
    private static final String STATIC_SKILL_STATUS_INACTIVE_PATH = "images/template/zhaohuanshou/status_inactive1.png";
    private static final String SKILL_ULTIMATE_CLICK_HINT_PATH = "images/template/zhaohuanshou/click_ultimate_template.png";
    private static final String FORGET_CONFIRM_BUTTON_PATH = "images/template/zhaohuanshou/forget_confirm_button.png";

    /**
     * Run one complete summon-skill cleanup pass.
     *
     * @return true only when the panel opened and the tail-slot cleanup finished successfully. False
     * means the caller should not update its maintenance cooldown, so a later idle tick can retry.
     * This method submits focused mouse/keyboard input through the exclusive input queue and runs UI
     * cleanup after the exclusive section releases.
     */
    public boolean cleanSummonSkillsOnce() {
        return cleanSummonSkillsOnce(SummonSkillCleanupRequest.defaults()).isSuccess();
    }

    /**
     * Run one summon-skill cleanup pass with maintenance-layer scan memory.
     *
     * @param request cached scan hints owned by {@link TaskMaintenanceService}; null means start
     *                from the default tail slot and allow the right-corner "点击可" check.
     * @return structured result used by maintenance scheduling to remember the next start slot and
     * the long cooldown for successful "点击可" generation.
     */
    public SummonSkillCleanupResult cleanSummonSkillsOnce(SummonSkillCleanupRequest request) {
        SummonSkillCleanupRequest safeRequest = request == null ? SummonSkillCleanupRequest.defaults() : request;
        if (isInputWorkerThread()) {
            return cleanSummonSkillsOnceDirect(safeRequest);
        }
        final SummonSkillCleanupResult[] result = new SummonSkillCleanupResult[]{
                SummonSkillCleanupResult.failed("summon skill clean not completed")
        };
        long startedAt = System.currentTimeMillis();
        boolean completed = inputSequences.submitExclusiveAndWait("summonSkill:cleanOnce", () -> {
            result[0] = cleanSummonSkillsOnceDirect(safeRequest);
            return true;
        });
        SummonSkillCleanupResult finalResult = completed
                ? result[0]
                : SummonSkillCleanupResult.failed("summon skill exclusive input did not complete");
        boolean success = completed && finalResult.isSuccess();
        log.info("summon skill clean: exclusive pass finished completed={} success={} elapsedMs={}",
                completed, success, System.currentTimeMillis() - startedAt);
        uiCleanerService.cleanLightweightInterruptions("summon-skill:finish");
        return finalResult;
    }

    /**
     * Direct implementation for a full cleanup pass, called only inside serialized input ownership.
     */
    private SummonSkillCleanupResult cleanSummonSkillsOnceDirect(SummonSkillCleanupRequest request) {
        long deadlineAtMs = System.currentTimeMillis() + CLEAN_ONCE_TIMEOUT_MS;
        long startedAt = System.currentTimeMillis();
        log.warn("summon skill clean: start one complete pass timeoutMs={}", CLEAN_ONCE_TIMEOUT_MS);

        if (!openSummonSkillPanelDirect()) {
            log.warn("summon skill clean: failed to open skill panel");
            return SummonSkillCleanupResult.failed("failed to open summon skill panel");
        }
        if (isCleanDeadlineExceeded(deadlineAtMs, "after-open-panel")) {
            return SummonSkillCleanupResult.failed("timeout after opening summon skill panel");
        }

        SummonSkillCleanupResult result = cleanTailNormalSkillsDirect(deadlineAtMs, request);
        if (!result.isSuccess()) {
            log.warn("summon skill clean: pass did not complete successfully; cooldown should not be updated");
            return result;
        }

        log.warn("summon skill clean: complete pass finished elapsedMs={}",
                System.currentTimeMillis() - startedAt);
        return result;
    }
    /**
     * Open the summon skill tab for the current window.
     *
     * @return true when the attribute anchor and skill tab were found/clicked. This may press Alt+O,
     * drag the panel, and click the skill tab using serialized physical input.
     */
    public boolean openSummonSkillPanel() {
        if (isInputWorkerThread()) {
            return openSummonSkillPanelDirect();
        }
        return inputSequences.submitExclusiveAndWait("summonSkill:openPanel", this::openSummonSkillPanelDirect);
    }

    private boolean openSummonSkillPanelDirect() {
        log.info("summon skill clean: press Alt+O to open summon panel");
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.pressAltO();
        if (!TaskSleep.sleep(900) || !InputActionScope.checkpoint()) {
            return false;
        }

        Point anchor = findAttributeAnchor(true);
        if (anchor == null) {
            log.info("summon skill clean: attribute anchor not found after Alt+O, retry once");
            if (!TaskSleep.sleep(PANEL_ANCHOR_RETRY_WAIT_MS) || !InputActionScope.checkpoint()) {
                return false;
            }
            anchor = findAttributeAnchor(true);
            if (anchor == null) {
                log.warn("summon skill clean: attribute anchor not found after Alt+O retry");
                return false;
            }
        }

        if (dragPanelIfNeeded(anchor)) {
            log.info("summon skill clean: panel dragged, locate anchor again");
            anchor = findAttributeAnchor(true);
            if (anchor == null) {
                log.warn("summon skill clean: anchor not found after panel drag");
                return false;
            }
        }

        Point skillButton = randomizeClickPoint(
                new Point(anchor.x + SKILL_BUTTON_OFFSET_X, anchor.y + SKILL_BUTTON_OFFSET_Y),
                NORMAL_CLICK_RANDOM_X,
                NORMAL_CLICK_RANDOM_Y,
                "skill entry"
        );
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(skillButton.x, skillButton.y, 150);
        if (!TaskSleep.sleep(OPEN_SKILL_PANEL_WAIT_MS) || !InputActionScope.checkpoint()) {
            return false;
        }
        Point remainingAttributeAnchor = findAttributeAnchor(false);
        if (remainingAttributeAnchor != null) {
            log.info("summon skill clean: attribute anchor still visible after skill tab click at ({}, {}), retry once",
                    remainingAttributeAnchor.x, remainingAttributeAnchor.y);
            if (!TaskSleep.sleep(PANEL_ANCHOR_RETRY_WAIT_MS) || !InputActionScope.checkpoint()) {
                return false;
            }
            remainingAttributeAnchor = findAttributeAnchor(false);
            if (remainingAttributeAnchor != null) {
                log.warn("summon skill clean: skill tab did not open after retry; attribute anchor still visible at ({}, {})",
                        remainingAttributeAnchor.x, remainingAttributeAnchor.y);
                return false;
            }
        }
        log.info("summon skill clean: skill tab clicked");
        return true;
    }
    /**
     * Clean only the tail normal summon skills, assuming the summon skill panel is already usable.
     *
     * @return true when the tail pass completes. False indicates interruption, timeout, unknown slot
     * state, or a failed forget confirmation.
     */
    public boolean cleanTailNormalSkills() {
        if (isInputWorkerThread()) {
            return cleanTailNormalSkillsDirect().isSuccess();
        }
        final SummonSkillCleanupResult[] result = new SummonSkillCleanupResult[]{
                SummonSkillCleanupResult.failed("summon skill clean tail not completed")
        };
        boolean completed = inputSequences.submitExclusiveAndWait("summonSkill:cleanTail", () -> {
            result[0] = cleanTailNormalSkillsDirect();
            return true;
        });
        if (completed) {
            uiCleanerService.cleanUpAll();
        }
        return completed && result[0].isSuccess();
    }

    private SummonSkillCleanupResult cleanTailNormalSkillsDirect() {
        return cleanTailNormalSkillsDirect(
                System.currentTimeMillis() + CLEAN_ONCE_TIMEOUT_MS,
                SummonSkillCleanupRequest.defaults()
        );
    }

    /**
     * Inspect and delete normal skills from the configured tail slots.
     *
     * @param deadlineAtMs absolute wall-clock deadline in milliseconds for this pass.
     * @return structured result when the scan reaches a conservative stop condition without failed
     * input; failed result on timeout, interruption, or failed deletion.
     */
    private SummonSkillCleanupResult cleanTailNormalSkillsDirect(long deadlineAtMs,
                                                                 SummonSkillCleanupRequest request) {
        Integer expectedSkillCount = request.getExpectedSkillCount();
        int skillCount;
        if (request.isTrustExpectedSkillCount() && (expectedSkillCount == 6 || expectedSkillCount == 8)) {
            skillCount = expectedSkillCount;
            log.info("summon skill clean: use trusted cached skill slot count {}", skillCount);
        } else {
            skillCount = detectSummonSkillSlotCount();
        }
        Point[] slots = getSkillSlotOffsets(skillCount);
        boolean skillCountChanged = request.getExpectedSkillCount() != null
                && request.getExpectedSkillCount() != skillCount;
        SummonSkillCleanupRequest effectiveRequest = skillCountChanged
                ? request.toBuilder().skipUltimateCornerCheck(false).build()
                : request;
        int deletedCount = 0;
        int inspectedCount = 0;
        int handledBusinessDialogs = 0;
        boolean ultimateGenerateClicked = false;
        boolean ultimateGenerateSucceeded = false;
        Map<Integer, SummonSkillSlotStatus> observedStatuses = new HashMap<>();

        StaticSlotScanResult staticScan = scanStaticSkillSlots(skillCount);
        observedStatuses.putAll(staticScan.observedStatuses());
        if (!staticScan.success()) {
            return buildCleanupResult(false, skillCount, slots.length, observedStatuses,
                    false, false, inspectedCount, deletedCount, staticScan.message());
        }
        int index = staticScan.actionIndex();
        int nextStartIndex = index;
        if (index < 0 || index >= slots.length) {
            log.info("summon skill clean: static slot scan found no actionable tail slot skillCount={} states={}",
                    skillCount, staticScan.statesText());
            return buildCleanupResult(true, skillCount, slots.length, observedStatuses,
                    ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                    "summon skill static tail scan found no actionable slot");
        }

        log.info("summon skill clean: detected {} skill slots, static start slot {} states={} expectedSkillCount={} skipUltimateCorner={}",
                skillCount, index + 1, staticScan.statesText(), request.getExpectedSkillCount(),
                effectiveRequest.isSkipUltimateCornerCheck());

        while (index < slots.length) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("summon skill clean: interrupted, stop current pass");
                return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                        ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                        "interrupted");
            }
            if (isCleanDeadlineExceeded(deadlineAtMs, "inspect-loop")) {
                return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                        ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                        "timeout during inspect loop");
            }
            if (handledBusinessDialogs < 3 && handleBusinessDialogDuringSkillClean("before-slot-" + (index + 1))) {
                handledBusinessDialogs++;
                continue;
            }

            Point slotAbsPoint = toAbsolutePoint(slots[index]);
            StaticSkillSlotState staticState = staticScan.stateAt(index);
            SummonSkillSlotStatus status = staticState == StaticSkillSlotState.EMPTY
                    ? SummonSkillSlotStatus.EMPTY_SLOT
                    : inspectSkillSlot(slotAbsPoint);
            inspectedCount++;
            observedStatuses.put(index, status);
            log.info("summon skill clean: slot {} status {}", index + 1, status);

            if (status == SummonSkillSlotStatus.NORMAL_SKILL) {
                if (isCleanDeadlineExceeded(deadlineAtMs, "before-delete")) {
                    return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                            ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                            "timeout before delete");
                }
                if (!deleteSkillAtSlot(slotAbsPoint)) {
                    return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                            ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                            "delete normal skill failed");
                }
                deletedCount++;

                if (deletedCount >= MAX_DELETE_SKILL_COUNT_PER_RUN) {
                    log.warn("summon skill clean: delete limit {} reached, stop this pass", MAX_DELETE_SKILL_COUNT_PER_RUN);
                    break;
                }

                SummonSkillSlotStatus afterDeleteStatus = inspectPostDeleteSlot(slotAbsPoint, index);
                inspectedCount++;
                observedStatuses.put(index, afterDeleteStatus);
                log.info("summon skill clean: slot {} status after delete {}", index + 1, afterDeleteStatus);
                if (afterDeleteStatus == SummonSkillSlotStatus.EMPTY_SLOT) {
                    UltimateCornerResult cornerResult = maybeClickUltimateCorner(
                            index, slotAbsPoint, effectiveRequest, deadlineAtMs, deletedCount, inspectedCount,
                            observedStatuses);
                    deletedCount = cornerResult.deletedCount;
                    inspectedCount = cornerResult.inspectedCount;
                    ultimateGenerateClicked = cornerResult.clicked;
                    ultimateGenerateSucceeded = cornerResult.succeeded;
                    nextStartIndex = cornerResult.nextStartIndex;
                    if (!cornerResult.completed) {
                        return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                                ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                                cornerResult.message);
                    }
                    break;
                }
                if (afterDeleteStatus == SummonSkillSlotStatus.KEEP_SKILL) {
                    nextStartIndex = index + 1;
                    break;
                }
                if (afterDeleteStatus == SummonSkillSlotStatus.LOCKED_SLOT) {
                    SummonSkillTailBoundaryScanner.Result boundaryResult = scanLockedBoundary(
                            index, slots, deadlineAtMs, observedStatuses);
                    inspectedCount += boundaryResult.inspectedCount();
                    deletedCount += boundaryResult.deletedCount();
                    nextStartIndex = boundaryResult.nextStartIndex();
                    if (!boundaryResult.success()) {
                        return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                                ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                                boundaryResult.message());
                    }
                    if (boundaryResult.ultimateCheckIndex() != null) {
                        UltimateCornerResult cornerResult = maybeClickUltimateCorner(
                                boundaryResult.ultimateCheckIndex(), toAbsolutePoint(slots[boundaryResult.ultimateCheckIndex()]),
                                effectiveRequest, deadlineAtMs, deletedCount, inspectedCount, observedStatuses);
                        deletedCount = cornerResult.deletedCount;
                        inspectedCount = cornerResult.inspectedCount;
                        ultimateGenerateClicked = cornerResult.clicked;
                        ultimateGenerateSucceeded = cornerResult.succeeded;
                        nextStartIndex = cornerResult.nextStartIndex;
                        if (!cornerResult.completed) {
                            return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                                    ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                                    cornerResult.message);
                        }
                    }
                    break;
                }
                return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                        ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                        "slot status unknown after deleting normal skill");
            }

            if (status == SummonSkillSlotStatus.KEEP_SKILL) {
                nextStartIndex = index + 1;
                break;
            }

            if (status == SummonSkillSlotStatus.EMPTY_SLOT) {
                UltimateCornerResult cornerResult = maybeClickUltimateCorner(
                        index, slotAbsPoint, effectiveRequest, deadlineAtMs, deletedCount, inspectedCount,
                        observedStatuses);
                deletedCount = cornerResult.deletedCount;
                inspectedCount = cornerResult.inspectedCount;
                ultimateGenerateClicked = cornerResult.clicked;
                ultimateGenerateSucceeded = cornerResult.succeeded;
                nextStartIndex = cornerResult.nextStartIndex;
                if (!cornerResult.completed) {
                    return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                            ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                            cornerResult.message);
                }
                break;
            }

            if (status == SummonSkillSlotStatus.LOCKED_SLOT) {
                SummonSkillTailBoundaryScanner.Result boundaryResult = scanLockedBoundary(
                        index, slots, deadlineAtMs, observedStatuses);
                inspectedCount += boundaryResult.inspectedCount();
                deletedCount += boundaryResult.deletedCount();
                nextStartIndex = boundaryResult.nextStartIndex();
                if (!boundaryResult.success()) {
                    return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                            ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                            boundaryResult.message());
                }
                if (boundaryResult.ultimateCheckIndex() != null) {
                    UltimateCornerResult cornerResult = maybeClickUltimateCorner(
                            boundaryResult.ultimateCheckIndex(), toAbsolutePoint(slots[boundaryResult.ultimateCheckIndex()]),
                            effectiveRequest, deadlineAtMs, deletedCount, inspectedCount, observedStatuses);
                    deletedCount = cornerResult.deletedCount;
                    inspectedCount = cornerResult.inspectedCount;
                    ultimateGenerateClicked = cornerResult.clicked;
                    ultimateGenerateSucceeded = cornerResult.succeeded;
                    nextStartIndex = cornerResult.nextStartIndex;
                    if (!cornerResult.completed) {
                        return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                                ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                                cornerResult.message);
                    }
                }
                break;
            }

                log.warn("summon skill clean: slot {} status unknown, stop conservatively", index + 1);
                if (handledBusinessDialogs < 3 && handleBusinessDialogDuringSkillClean("unknown-slot-" + (index + 1))) {
                    handledBusinessDialogs++;
                    continue;
                }
                return buildCleanupResult(false, skillCount, nextStartIndex, observedStatuses,
                        ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                        "slot status unknown");
        }
        if (index >= slots.length) {
            nextStartIndex = slots.length;
        }
        log.info("summon skill clean: tail skill pass finished skillCount={} inspected={} deleted={} startSlot={} nextStartSlot={} ultimateClicked={} ultimateSucceeded={}",
                skillCount, inspectedCount, deletedCount, index + 1, nextStartIndex + 1,
                ultimateGenerateClicked, ultimateGenerateSucceeded);
        return buildCleanupResult(true, skillCount, nextStartIndex, observedStatuses,
                ultimateGenerateClicked, ultimateGenerateSucceeded, inspectedCount, deletedCount,
                "summon skill tail pass finished");
    }

    private SummonSkillTailBoundaryScanner.Result scanLockedBoundary(int lockedIndex,
                                                                     Point[] slots,
                                                                     long deadlineAtMs,
                                                                     Map<Integer, SummonSkillSlotStatus> observedStatuses) {
        return SummonSkillTailBoundaryScanner.scanLockedBoundary(
                lockedIndex,
                previousIndex -> {
                    Point previousSlotAbsPoint = toAbsolutePoint(slots[previousIndex]);
                    SummonSkillSlotStatus previousStatus = inspectSkillSlot(previousSlotAbsPoint);
                    observedStatuses.put(previousIndex, previousStatus);
                    log.info("summon skill clean: locked boundary previous slot {} status {}",
                            previousIndex + 1, previousStatus);
                    return previousStatus;
                },
                previousIndex -> {
                    Point previousSlotAbsPoint = toAbsolutePoint(slots[previousIndex]);
                    return deleteSkillAtSlot(previousSlotAbsPoint);
                },
                () -> Thread.currentThread().isInterrupted()
                        || isCleanDeadlineExceeded(deadlineAtMs, "locked-boundary-backward-scan"));
    }

    /**
     * Let lightweight maintenance broadcast dialogs win over the long summon-skill pass.
     *
     * <p>删技能运行在独占输入段里，队长广播的医宝宝/修装备弹窗可能刚好覆盖召唤兽面板。
     * 这里复用维护服务里的轻量 ROI 模板匹配，避免回到 DialogService 洗字/云端策略链。</p>
     */
    private boolean handleBusinessDialogDuringSkillClean(String stage) {
        TaskMaintenanceService taskMaintenanceService = taskMaintenanceServiceProvider.getIfAvailable();
        if (taskMaintenanceService == null) {
            return false;
        }
        TaskMaintenanceResult result = taskMaintenanceService.handleMaintenanceBroadcast(null,
                TaskMaintenanceRequest.builder()
                        .sourceTask("summon-skill:" + stage)
                        .handleMaintenanceBroadcast(true)
                        .cleanSummonSkill(false)
                        .build());
        if (result.isBroadcastHandled()) {
            log.info("summon skill clean: handled business dialog during pass stage={}", stage);
            return true;
        }
        if (result.getStatus() == com.bot.dhxy.model.maintenance.TaskMaintenanceStatus.INTERRUPTED) {
            log.info("summon skill clean: maintenance broadcast handler interrupted stage={} message={}",
                    stage, result.getMessage());
        }
        return false;
    }

    private UltimateCornerResult maybeClickUltimateCorner(int index,
                                                          Point slotAbsPoint,
                                                          SummonSkillCleanupRequest request,
                                                          long deadlineAtMs,
                                                          int deletedCount,
                                                          int inspectedCount,
                                                          Map<Integer, SummonSkillSlotStatus> observedStatuses) {
        if (request.isSkipUltimateCornerCheck()) {
            log.info("summon skill clean: skip ultimate corner check by cooldown slot={}", index + 1);
            return UltimateCornerResult.completed(index, false, false, deletedCount, inspectedCount,
                    "ultimate corner skipped by cooldown");
        }
        if (isCleanDeadlineExceeded(deadlineAtMs, "before-ultimate-corner")) {
            return UltimateCornerResult.failed(index, false, false, deletedCount, inspectedCount,
                    "timeout before ultimate corner");
        }

        Point cornerPoint = randomizeHoverPoint(
                new Point(slotAbsPoint.x + ULTIMATE_CORNER_OFFSET_X, slotAbsPoint.y + ULTIMATE_CORNER_OFFSET_Y),
                "ultimate corner hover"
        );
        if (!InputActionScope.checkpoint()) {
            return UltimateCornerResult.failed(index, false, false, deletedCount, inspectedCount,
                    "interrupted before ultimate corner hover");
        }
        inputProvider.moveMouse(cornerPoint.x, cornerPoint.y);
        if (!TaskSleep.sleep(SKILL_HOVER_WAIT_MS) || !InputActionScope.checkpoint()) {
            return UltimateCornerResult.failed(index, false, false, deletedCount, inspectedCount,
                    "interrupted during ultimate corner hover");
        }
        YellowTipScan scan = captureAndWashYellowTipOnce("ultimate_corner", buildTipRectByHoverPoint(cornerPoint));
        if (scan == null) {
            return UltimateCornerResult.failed(index, false, false, deletedCount, inspectedCount,
                    "ultimate corner tooltip capture failed");
        }
        if (scan.yellowCount < MIN_YELLOW_PIXEL_COUNT) {
            log.info("summon skill clean: ultimate corner no yellow tip slot={} yellowCount={}",
                    index + 1, scan.yellowCount);
            return UltimateCornerResult.completed(index, false, false, deletedCount, inspectedCount,
                    "ultimate corner no yellow tip");
        }
        if (!matchYellowTemplateInScan(SKILL_ULTIMATE_CLICK_HINT_PATH, scan, "ultimate-click-hint")) {
            log.info("summon skill clean: ultimate corner click hint not matched slot={}", index + 1);
            return UltimateCornerResult.completed(index, false, false, deletedCount, inspectedCount,
                    "ultimate corner hint not matched");
        }

        log.info("summon skill clean: ultimate corner hint matched slot={} click=({}, {})",
                index + 1, cornerPoint.x, cornerPoint.y);
        if (!InputActionScope.checkpoint()) {
            return UltimateCornerResult.failed(index, false, false, deletedCount, inspectedCount,
                    "interrupted before ultimate corner click");
        }
        inputProvider.clickLeft(cornerPoint.x, cornerPoint.y, 120);
        if (!TaskSleep.sleep(ULTIMATE_CORNER_CLICK_WAIT_MS) || !InputActionScope.checkpoint()) {
            return UltimateCornerResult.failed(index, true, false, deletedCount, inspectedCount,
                    "interrupted after ultimate corner click");
        }
        SummonSkillSlotStatus generatedStatus = inspectSkillSlot(slotAbsPoint);
        inspectedCount++;
        observedStatuses.put(index, generatedStatus);
        log.info("summon skill clean: slot {} status after ultimate corner click {}", index + 1, generatedStatus);

        if (generatedStatus == SummonSkillSlotStatus.NORMAL_SKILL) {
            if (!deleteSkillAtSlot(slotAbsPoint)) {
                return UltimateCornerResult.failed(index, true, true, deletedCount, inspectedCount,
                        "delete generated normal skill failed");
            }
            deletedCount++;
            SummonSkillSlotStatus afterGeneratedDelete = inspectPostDeleteSlot(slotAbsPoint, index);
            inspectedCount++;
            observedStatuses.put(index, afterGeneratedDelete);
            log.info("summon skill clean: slot {} status after generated normal delete {}",
                    index + 1, afterGeneratedDelete);
            if (afterGeneratedDelete == SummonSkillSlotStatus.EMPTY_SLOT) {
                return UltimateCornerResult.completed(index, true, true, deletedCount, inspectedCount,
                        "generated normal skill deleted");
            }
            if (afterGeneratedDelete == SummonSkillSlotStatus.KEEP_SKILL) {
                return UltimateCornerResult.completed(index + 1, true, true, deletedCount, inspectedCount,
                        "generated skill changed to keep after delete check");
            }
            return UltimateCornerResult.failed(index, true, true, deletedCount, inspectedCount,
                    "generated normal delete did not leave a stable slot");
        }

        if (generatedStatus == SummonSkillSlotStatus.KEEP_SKILL) {
            return UltimateCornerResult.completed(index + 1, true, true, deletedCount, inspectedCount,
                    "generated keep skill");
        }
        if (generatedStatus == SummonSkillSlotStatus.EMPTY_SLOT || generatedStatus == SummonSkillSlotStatus.LOCKED_SLOT) {
            return UltimateCornerResult.failed(index, true, false, deletedCount, inspectedCount,
                    "ultimate corner clicked but no skill generated");
        }
        return UltimateCornerResult.failed(index, true, false, deletedCount, inspectedCount,
                "ultimate corner generated unknown status");
    }

    private SummonSkillCleanupResult buildCleanupResult(boolean success,
                                                        int skillCount,
                                                        int nextStartIndex,
                                                        Map<Integer, SummonSkillSlotStatus> observedStatuses,
                                                        boolean ultimateGenerateClicked,
                                                        boolean ultimateGenerateSucceeded,
                                                        int inspectedCount,
                                                        int deletedCount,
                                                        String message) {
        return SummonSkillCleanupResult.builder()
                .success(success)
                .skillCount(skillCount)
                .nextStartIndex(nextStartIndex)
                .observedStatusesByIndex(Map.copyOf(observedStatuses))
                .ultimateGenerateClicked(ultimateGenerateClicked)
                .ultimateGenerateSucceeded(ultimateGenerateSucceeded)
                .inspectedCount(inspectedCount)
                .deletedCount(deletedCount)
                .message(message)
                .build();
    }

    /**
     * Detect whether the current summon skill layout has six or eight slots.
     *
     * @return {@code 8} when the fixed IF8 layout marker matches in the current bound window,
     * otherwise {@code 6}. This avoids the old hover-tooltip probe so slot-count detection does not
     * depend on tooltip latency or yellow-text washing.
     */
    public int detectSummonSkillSlotCount() {
        double[] match = matchIf8LayoutMarker();
        int skillCount = detectSkillCountFromIf8Match(match);
        log.info("summon skill clean: if8 layout marker result={} score={} roi={} template={} skillCount={}",
                match == null ? "MISS" : "HIT",
                match == null || match.length < 3 ? "-" : String.format("%.6f", match[2]),
                rectText(IF8_LAYOUT_ROI),
                IF8_LAYOUT_TEMPLATE_PATH,
                skillCount);
        return skillCount;
    }

    static int detectSkillCountFromIf8Match(double[] match) {
        return match == null ? 6 : 8;
    }

    private double[] matchIf8LayoutMarker() {
        if (!new File(IF8_LAYOUT_TEMPLATE_PATH).exists()) {
            log.warn("summon skill clean: if8 layout template missing path={}", IF8_LAYOUT_TEMPLATE_PATH);
            return null;
        }

        int x1 = tracker.getWindowBaseX() + IF8_LAYOUT_ROI[0];
        int y1 = tracker.getWindowBaseY() + IF8_LAYOUT_ROI[1];
        int x2 = tracker.getWindowBaseX() + IF8_LAYOUT_ROI[2];
        int y2 = tracker.getWindowBaseY() + IF8_LAYOUT_ROI[3];
        String rawPath = uniqueDebugImagePath("summon_skill_if8_layout_roi", ".png");
        boolean captured = tracker.captureToFile("summon-skill-if8-layout", rawPath, x1, y1, x2, y2);
        if (!captured) {
            log.warn("summon skill clean: if8 layout roi capture failed rect={} base=({}, {}) path={}",
                    rectText(new int[]{x1, y1, x2, y2}),
                    tracker.getWindowBaseX(), tracker.getWindowBaseY(), rawPath);
            return null;
        }

        double[] match = ImageFinder.find(rawPath, IF8_LAYOUT_TEMPLATE_PATH, IF8_LAYOUT_MATCH_RATE);
        if (match != null && match.length >= 3) {
            log.info("summon skill clean: if8 layout template matched roiPoint=({}, {}) score={} raw={}",
                    Math.round(match[0]), Math.round(match[1]), String.format("%.6f", match[2]), rawPath);
        }
        return match;
    }

    private StaticSlotScanResult scanStaticSkillSlots(int skillCount) {
        StaticSkillSlotTemplates templates = loadStaticSkillSlotTemplates();
        if (templates == null) {
            return StaticSlotScanResult.failed("static skill slot template missing or unreadable");
        }

        String rawPath = uniqueDebugImagePath("summon_skill_static_slots_raw", ".png");
        int baseX = tracker.getWindowBaseX();
        int baseY = tracker.getWindowBaseY();
        Point[] slotOffsets = getSkillSlotOffsets(skillCount);
        int[] relativeRoi = buildStaticSlotScanRelativeRoi(slotOffsets);
        boolean captured = tracker.captureToFile(
                "summon-skill-static-slots",
                rawPath,
                baseX + relativeRoi[0],
                baseY + relativeRoi[1],
                baseX + relativeRoi[2],
                baseY + relativeRoi[3]
        );
        if (!captured) {
            log.warn("summon skill clean: static slot relative roi capture failed roi={} screenRoi={} base=({}, {}) path={}",
                    rectText(relativeRoi), rectText(toScreenRect(relativeRoi, baseX, baseY)), baseX, baseY, rawPath);
            return StaticSlotScanResult.failed("static skill slot screenshot failed");
        }

        BufferedImage rawImage = readImage(rawPath);
        if (rawImage == null) {
            return StaticSlotScanResult.failed("static skill slot screenshot unreadable");
        }

        int[][] slotRects = buildSkillSlotRects(
                slotOffsets,
                baseX,
                baseY,
                baseX + relativeRoi[0],
                baseY + relativeRoi[1]
        );
        StaticSkillSlotState[] states = new StaticSkillSlotState[slotRects.length];
        Map<Integer, SummonSkillSlotStatus> observedStatuses = new HashMap<>();
        for (int i = 0; i < slotRects.length; i++) {
            states[i] = classifyStaticSkillSlot(rawImage, slotRects[i], templates, i);
            if (states[i] == StaticSkillSlotState.UNKNOWN) {
                log.warn("summon skill clean: static slot {} is UNKNOWN, fail closed", i + 1);
                return StaticSlotScanResult.failed(states, observedStatuses,
                        "static skill slot scan unknown at slot " + (i + 1));
            }
            if (states[i] == StaticSkillSlotState.LOCKED) {
                observedStatuses.put(i, SummonSkillSlotStatus.LOCKED_SLOT);
            } else if (states[i] == StaticSkillSlotState.EMPTY) {
                observedStatuses.put(i, SummonSkillSlotStatus.EMPTY_SLOT);
            }
        }

        int actionIndex = resolveStaticTailStartIndex(states);
        log.info("summon skill clean: static slot scan states={} actionSlot={} roi={} screenRoi={} raw={}",
                statesText(states), actionIndex < 0 ? "-" : actionIndex + 1,
                rectText(relativeRoi), rectText(toScreenRect(relativeRoi, baseX, baseY)), rawPath);
        return StaticSlotScanResult.success(actionIndex, states, observedStatuses);
    }

    private StaticSkillSlotTemplates loadStaticSkillSlotTemplates() {
        BufferedImage sealed = readTemplateImage(STATIC_SKILL_STATUS_SEALED_PATH);
        BufferedImage unobtained = readTemplateImage(STATIC_SKILL_STATUS_UNOBTAINED_PATH);
        BufferedImage inactive = readTemplateImage(STATIC_SKILL_STATUS_INACTIVE_PATH);
        if (sealed == null || unobtained == null || inactive == null) {
            return null;
        }
        return new StaticSkillSlotTemplates(sealed, unobtained, inactive);
    }

    private BufferedImage readTemplateImage(String path) {
        if (!new File(path).exists()) {
            log.warn("summon skill clean: static slot template missing path={}", path);
            return null;
        }
        return readImage(path);
    }

    private StaticSkillSlotState classifyStaticSkillSlot(BufferedImage rawImage,
                                                        int[] slotRect,
                                                        StaticSkillSlotTemplates templates,
                                                        int slotIndex) {
        try {
            if (slotRect[0] < 0 || slotRect[1] < 0
                    || slotRect[2] > rawImage.getWidth()
                    || slotRect[3] > rawImage.getHeight()
                    || slotRect[2] <= slotRect[0]
                    || slotRect[3] <= slotRect[1]) {
                log.warn("summon skill clean: static slot rect invalid slot={} rect={} image={}x{}",
                        slotIndex + 1, rectText(slotRect), rawImage.getWidth(), rawImage.getHeight());
                return StaticSkillSlotState.UNKNOWN;
            }

            BufferedImage slotImage = rawImage.getSubimage(
                    slotRect[0],
                    slotRect[1],
                    slotRect[2] - slotRect[0],
                    slotRect[3] - slotRect[1]
            );
            if (templateMatches(slotImage, templates.sealed())
                    || templateMatches(slotImage, templates.unobtained())) {
                return StaticSkillSlotState.LOCKED;
            }
            if (templateMatches(slotImage, templates.inactive())
                    || lowTextureTemplateMatchesByColorDistance(
                            slotImage, templates.inactive(), STATIC_INACTIVE_COLOR_DISTANCE_THRESHOLD)) {
                return StaticSkillSlotState.EMPTY;
            }
            return StaticSkillSlotState.OCCUPIED;
        } catch (RuntimeException e) {
            log.warn("summon skill clean: static slot classification failed slot={} rect={} error={}",
                    slotIndex + 1, rectText(slotRect), e.toString());
            return StaticSkillSlotState.UNKNOWN;
        }
    }

    private boolean templateMatches(BufferedImage slotImage, BufferedImage templateImage) {
        return ImageFinder.find(slotImage, templateImage, SKILL_STATUS_MATCH_RATE) != null;
    }

    private static boolean lowTextureTemplateMatchesByColorDistance(BufferedImage source,
                                                                    BufferedImage target,
                                                                    double maxAverageDistance) {
        if (source == null || target == null
                || source.getWidth() < target.getWidth()
                || source.getHeight() < target.getHeight()) {
            return false;
        }
        for (int y = 0; y <= source.getHeight() - target.getHeight(); y++) {
            for (int x = 0; x <= source.getWidth() - target.getWidth(); x++) {
                if (averageColorDistance(source, target, x, y) <= maxAverageDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double averageColorDistance(BufferedImage source,
                                               BufferedImage target,
                                               int offsetX,
                                               int offsetY) {
        int width = target.getWidth();
        int height = target.getHeight();
        if (width <= 0 || height <= 0) {
            return Double.MAX_VALUE;
        }
        long count = 0L;
        double total = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbA = source.getRGB(offsetX + x, offsetY + y);
                int rgbB = target.getRGB(x, y);
                int dr = ((rgbA >> 16) & 0xFF) - ((rgbB >> 16) & 0xFF);
                int dg = ((rgbA >> 8) & 0xFF) - ((rgbB >> 8) & 0xFF);
                int db = (rgbA & 0xFF) - (rgbB & 0xFF);
                total += Math.sqrt(dr * dr + dg * dg + db * db);
                count++;
            }
        }
        return total / count;
    }

    private int[][] buildSkillSlotRects(Point[] offsets, int baseX, int baseY, int originX, int originY) {
        int[][] rects = new int[offsets.length][4];
        for (int i = 0; i < offsets.length; i++) {
            Point offset = offsets[i];
            int screenX = baseX + offset.x;
            int screenY = baseY + offset.y;
            rects[i] = new int[]{
                    screenX - SKILL_SLOT_BOX_HALF_SIZE - originX,
                    screenY - SKILL_SLOT_BOX_HALF_SIZE - originY,
                    screenX + SKILL_SLOT_BOX_HALF_SIZE - originX,
                    screenY + SKILL_SLOT_BOX_HALF_SIZE - originY
            };
        }
        return rects;
    }

    private int[] buildStaticSlotScanRelativeRoi(Point[] offsets) {
        int minX = GAME_WINDOW_WIDTH;
        int minY = GAME_WINDOW_HEIGHT;
        int maxX = 0;
        int maxY = 0;
        for (Point offset : offsets) {
            minX = Math.min(minX, offset.x - SKILL_SLOT_BOX_HALF_SIZE - STATIC_SLOT_SCAN_PADDING);
            minY = Math.min(minY, offset.y - SKILL_SLOT_BOX_HALF_SIZE - STATIC_SLOT_SCAN_PADDING);
            maxX = Math.max(maxX, offset.x + SKILL_SLOT_BOX_HALF_SIZE + STATIC_SLOT_SCAN_PADDING + 1);
            maxY = Math.max(maxY, offset.y + SKILL_SLOT_BOX_HALF_SIZE + STATIC_SLOT_SCAN_PADDING + 1);
        }
        return new int[]{
                Math.max(0, minX),
                Math.max(0, minY),
                Math.min(GAME_WINDOW_WIDTH, maxX),
                Math.min(GAME_WINDOW_HEIGHT, maxY)
        };
    }

    private int[] toScreenRect(int[] relativeRoi, int baseX, int baseY) {
        return new int[]{
                baseX + relativeRoi[0],
                baseY + relativeRoi[1],
                baseX + relativeRoi[2],
                baseY + relativeRoi[3]
        };
    }

    static int resolveStaticTailStartIndex(StaticSkillSlotState[] states) {
        if (states == null || states.length == 0) {
            return -1;
        }
        for (StaticSkillSlotState state : states) {
            if (state == null || state == StaticSkillSlotState.UNKNOWN) {
                return -1;
            }
        }
        for (int i = states.length - 1; i >= 0; i--) {
            StaticSkillSlotState state = states[i];
            if (state == StaticSkillSlotState.LOCKED) {
                continue;
            }
            if (state == StaticSkillSlotState.EMPTY) {
                int firstEmpty = i;
                while (firstEmpty > 0 && states[firstEmpty - 1] == StaticSkillSlotState.EMPTY) {
                    firstEmpty--;
                }
                return firstEmpty;
            }
            return i;
        }
        return -1;
    }

    private static String statesText(StaticSkillSlotState[] states) {
        if (states == null || states.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < states.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i + 1).append('=').append(states[i]);
        }
        return sb.toString();
    }

    private SummonSkillSlotStatus inspectSkillSlot(Point slotAbsPoint) {
        if (isInputWorkerThread()) {
            return inspectSkillSlotDirect(slotAbsPoint);
        }
        final SummonSkillSlotStatus[] result = new SummonSkillSlotStatus[]{SummonSkillSlotStatus.UNKNOWN};
        boolean ok = inputSequences.submitExclusiveAndWait("summonSkill:inspectSlot", () -> {
            result[0] = inspectSkillSlotDirect(slotAbsPoint);
            return true;
        });
        return ok ? result[0] : SummonSkillSlotStatus.UNKNOWN;
    }

    /**
     * Re-check one slot after deleting a normal skill without re-hovering it.
     *
     * <p>When the cloud service is active, the method captures only the complete 52-by-52 slot ROI
     * and lets the cloud classify the static empty/locked/occupied state. This avoids reopening the
     * yellow tooltip merely to confirm that the delete produced an empty slot. The old hover path is
     * retained only while the cloud service is disabled.</p>
     */
    private SummonSkillSlotStatus inspectPostDeleteSlot(Point slotAbsPoint, int slotIndex) {
        if (summonSkillCloudDecisionService == null || !summonSkillCloudDecisionService.isActive()) {
            return inspectSkillSlot(slotAbsPoint);
        }

        int[] slotRect = new int[]{
                slotAbsPoint.x - SKILL_SLOT_BOX_HALF_SIZE,
                slotAbsPoint.y - SKILL_SLOT_BOX_HALF_SIZE,
                slotAbsPoint.x + SKILL_SLOT_BOX_HALF_SIZE,
                slotAbsPoint.y + SKILL_SLOT_BOX_HALF_SIZE
        };
        String rawPath = uniqueDebugImagePath("post_delete_slot_cloud_raw", ".png");
        boolean captured = tracker.captureToFile(
                "summon-skill-post-delete-slot",
                rawPath,
                slotRect[0],
                slotRect[1],
                slotRect[2],
                slotRect[3]
        );
        if (!captured) {
            log.warn("summon skill cloud: post-delete slot raw capture failed slot={} rect={}",
                    slotIndex + 1, rectText(slotRect));
            return SummonSkillSlotStatus.UNKNOWN;
        }

        ImagePayload payload = readImagePayload(rawPath);
        if (payload == null) {
            return SummonSkillSlotStatus.UNKNOWN;
        }

        SummonSkillCloudRequest.Roi roi = toWindowRelativeRoi(slotRect);
        SummonSkillCloudDecision decision = summonSkillCloudDecisionService.inspectCurrentHoverTip(
                SummonSkillCloudRequest.builder()
                        .imagePayloadBase64(payload.base64())
                        .payloadMimeType("image/png")
                        .imageSha256(payload.sha256())
                        .rawImagePath(rawPath)
                        .debugImageId("post-delete-slot-cloud")
                        .roi(roi)
                        .windowWidth(GAME_WINDOW_WIDTH)
                        .windowHeight(GAME_WINDOW_HEIGHT)
                        .slotIndex(slotIndex)
                        .taskCode(currentTaskCode())
                        .phase("summon-skill-post-delete-slot")
                        .source("summon-skill:postDeleteSlot")
                        .windowId(currentWindowId())
                        .hwnd(currentHwnd())
                        .build());
        log.info("summon skill cloud: post-delete slot={} status={} accepted={} reason={} raw={} roi={}",
                slotIndex + 1, decision.getSlotStatus(), decision.isCloudExecuted(), decision.getReason(),
                rawPath, roiText(roi));
        return decision.getSlotStatus();
    }

    /**
     * Hover one screen-absolute slot point and classify the visible yellow tooltip.
     */
    private SummonSkillSlotStatus inspectSkillSlotDirect(Point slotAbsPoint) {
        Point hoverPoint = randomizeHoverPoint(slotAbsPoint, "skill slot hover");
        if (!InputActionScope.checkpoint()) {
            return SummonSkillSlotStatus.UNKNOWN;
        }
        inputProvider.moveMouse(hoverPoint.x, hoverPoint.y);
        if (!TaskSleep.sleep(SKILL_HOVER_WAIT_MS) || !InputActionScope.checkpoint()) {
            return SummonSkillSlotStatus.UNKNOWN;
        }
        return inspectCurrentHoverTip(hoverPoint);
    }
    private SummonSkillSlotStatus inspectCurrentHoverTip(Point hoverAbsPoint) {
        if (summonSkillCloudDecisionService != null && summonSkillCloudDecisionService.isActive()) {
            return inspectCurrentHoverTipWithCloud(hoverAbsPoint);
        }

        YellowTipScan scan = captureAndWashYellowTipOnce("slot_tip", buildTipRectByHoverPoint(hoverAbsPoint));
        if (scan == null) {
            return SummonSkillSlotStatus.UNKNOWN;
        }

        log.info("summon skill clean: hover tip yellow pixel count={}", scan.yellowCount);
        if (scan.yellowCount < MIN_YELLOW_PIXEL_COUNT) {
            return SummonSkillSlotStatus.UNKNOWN;
        }

        if (matchYellowTemplateInScan(SKILL_STATUS_SEALED_PATH, scan, "sealed")
                || matchYellowTemplateInScan(SKILL_STATUS_UNOBTAINED_PATH, scan, "unobtained")) {
            return SummonSkillSlotStatus.LOCKED_SLOT;
        }
        if (matchYellowTemplateInScan(SKILL_STATUS_INACTIVE_PATH, scan, "inactive-empty")) {
            return SummonSkillSlotStatus.EMPTY_SLOT;
        }
        if (matchYellowTemplateInScan(SKILL_STATUS_NORMAL_PATH, scan, "normal")) {
            return SummonSkillSlotStatus.NORMAL_SKILL;
        }
        if (matchYellowTemplateInScan(SKILL_STATUS_HIGH_PATH, scan, "high")
                || matchYellowTemplateInScan(SKILL_STATUS_ULTIMATE_PATH, scan, "ultimate")) {
            return SummonSkillSlotStatus.KEEP_SKILL;
        }
        return SummonSkillSlotStatus.UNKNOWN;
    }

    private SummonSkillSlotStatus inspectCurrentHoverTipWithCloud(Point hoverAbsPoint) {
        int[] tipRect = buildTipRectByHoverPoint(hoverAbsPoint);
        String rawPath = uniqueDebugImagePath("slot_tip_cloud_raw", ".png");
        boolean captured = tracker.captureToFile(
                "summon-skill-cloud-slot-tip",
                rawPath,
                tipRect[0],
                tipRect[1],
                tipRect[2],
                tipRect[3]
        );
        if (!captured) {
            log.warn("summon skill cloud: raw tooltip capture failed rect={}", rectText(tipRect));
            return SummonSkillSlotStatus.UNKNOWN;
        }

        ImagePayload payload = readImagePayload(rawPath);
        if (payload == null) {
            return SummonSkillSlotStatus.UNKNOWN;
        }

        SummonSkillCloudRequest.Roi roi = toWindowRelativeRoi(tipRect);
        SummonSkillCloudDecision decision = summonSkillCloudDecisionService.inspectCurrentHoverTip(
                SummonSkillCloudRequest.builder()
                        .imagePayloadBase64(payload.base64())
                        .payloadMimeType("image/png")
                        .imageSha256(payload.sha256())
                        .rawImagePath(rawPath)
                        .debugImageId("slot-tip-cloud")
                        .roi(roi)
                        .windowWidth(GAME_WINDOW_WIDTH)
                        .windowHeight(GAME_WINDOW_HEIGHT)
                        .taskCode(currentTaskCode())
                        .phase("summon-skill-slot-status")
                        .source("summon-skill:inspectCurrentHoverTip")
                        .windowId(currentWindowId())
                        .hwnd(currentHwnd())
                        .build());
        log.info("summon skill cloud: slot status={} accepted={} reason={} raw={} roi={}",
                decision.getSlotStatus(), decision.isCloudExecuted(), decision.getReason(), rawPath, roiText(roi));
        return decision.getSlotStatus();
    }

    private boolean deleteSkillAtSlot(Point slotAbsPoint) {
        if (isInputWorkerThread()) {
            return deleteSkillAtSlotDirect(slotAbsPoint);
        }
        return inputSequences.submitExclusiveAndWait("summonSkill:deleteSlot",
                () -> deleteSkillAtSlotDirect(slotAbsPoint));
    }

    /**
     * Forget the skill in one screen-absolute slot.
     *
     * <p>The method clicks the slot, clicks the delete button, template-matches the confirm button,
     * and clicks confirm. It returns false before updating any cooldown when confirmation cannot be
     * found.</p>
     */
    private boolean deleteSkillAtSlotDirect(Point slotAbsPoint) {
        Point slotClickPoint = randomizeClickPoint(slotAbsPoint, SKILL_SLOT_CLICK_RANDOM_X, SKILL_SLOT_CLICK_RANDOM_Y,
                "skill slot");
        log.info("summon skill clean: delete normal skill start slot=({}, {}) click=({}, {})",
                slotAbsPoint.x, slotAbsPoint.y, slotClickPoint.x, slotClickPoint.y);
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(slotClickPoint.x, slotClickPoint.y, 120);
        if (!TaskSleep.sleep(SELECT_SKILL_WAIT_MS) || !InputActionScope.checkpoint()) {
            return false;
        }

        Point deleteButton = randomizeClickPoint(toAbsolutePoint(new Point(DELETE_SKILL_BUTTON_X, DELETE_SKILL_BUTTON_Y)),
                NORMAL_CLICK_RANDOM_X, NORMAL_CLICK_RANDOM_Y, "delete skill button");
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(deleteButton.x, deleteButton.y, 120);
        if (!TaskSleep.sleep(DELETE_DIALOG_WAIT_MS) || !InputActionScope.checkpoint()) {
            return false;
        }

        Point confirmButtonPoint = findForgetConfirmButton();
        if (confirmButtonPoint == null) {
            log.warn("summon skill clean: forget confirm button not found; delete failed");
            return false;
        }

        Point confirmClickPoint = randomizeClickPoint(confirmButtonPoint, CONFIRM_CLICK_RANDOM_X, CONFIRM_CLICK_RANDOM_Y,
                "forget confirm button");
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.clickLeft(confirmClickPoint.x, confirmClickPoint.y, 120);
        if (!TaskSleep.sleep(FORGET_DONE_WAIT_MS) || !InputActionScope.checkpoint()) {
            return false;
        }
        if (!dialogService.fastClickKnownSmallStoryDialog("summon-skill:delete-story")) {
            return false;
        }
        log.info("summon skill clean: delete normal skill confirmed slot=({}, {}) confirm=({}, {})",
                slotAbsPoint.x, slotAbsPoint.y, confirmClickPoint.x, confirmClickPoint.y);
        return true;
    }

    private Point findForgetConfirmButton() {
        if (!new File(FORGET_CONFIRM_BUTTON_PATH).exists()) {
            log.warn("summon skill clean: forget confirm template missing: {}", FORGET_CONFIRM_BUTTON_PATH);
            return null;
        }

        int[] rect = coordinateHelper.getScaledRect(
                CONFIRM_BUTTON_SEARCH_X,
                CONFIRM_BUTTON_SEARCH_Y,
                CONFIRM_BUTTON_SEARCH_W,
                CONFIRM_BUTTON_SEARCH_H
        );
        return coordinateHelper.findImageInRegion(FORGET_CONFIRM_BUTTON_PATH, rect, FORGET_CONFIRM_MATCH_RATE);
    }

    private Point findAttributeAnchor(boolean logMissing) {
        tracker.updateGlobalVision();
        Point anchor = coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                ZHS_ATTRIBUTE_ANCHOR_PATH,
                tracker.getLatestVisionPath(),
                ANCHOR_MATCH_RATE
        );
        if (anchor == null && logMissing) {
            log.info("summon skill clean: attribute anchor not found: {}", ZHS_ATTRIBUTE_ANCHOR_PATH);
        }
        return anchor;
    }

    private boolean needDragByAnchor(Point anchor) {
        int gameRightX = tracker.getWindowBaseX() + GAME_WINDOW_WIDTH;
        int distanceToRight = gameRightX - anchor.x;
        boolean needDrag = distanceToRight < MIN_ANCHOR_DISTANCE_TO_RIGHT;
        log.info("summon skill clean: panel anchor distanceToRight={} threshold={} needDrag={}",
                distanceToRight, MIN_ANCHOR_DISTANCE_TO_RIGHT, needDrag);
        return needDrag;
    }

    private boolean dragPanelIfNeeded(Point anchor) {
        if (!needDragByAnchor(anchor)) {
            return false;
        }

        Point dragFrom = randomizeClickPoint(anchor, DRAG_POINT_RANDOM_X, DRAG_POINT_RANDOM_Y, "summon panel drag from");
        Point dragTo = randomizeClickPoint(
                new Point(tracker.getWindowBaseX() + DRAG_TARGET_BASE_X, tracker.getWindowBaseY() + DRAG_TARGET_BASE_Y),
                DRAG_TARGET_RANDOM_X,
                DRAG_TARGET_RANDOM_Y,
                "summon panel drag to"
        );
        if (!InputActionScope.checkpoint()) {
            return false;
        }
        inputProvider.dragAndDrop(dragFrom.x, dragFrom.y, dragTo.x, dragTo.y);
        return TaskSleep.sleep(DRAG_PANEL_WAIT_MS) && InputActionScope.checkpoint();
    }

    private ImagePayload readImagePayload(String rawPath) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(rawPath));
            return new ImagePayload(Base64.getEncoder().encodeToString(bytes), sha256Hex(bytes));
        } catch (IOException e) {
            log.warn("summon skill cloud: failed to read raw payload path={} reason={}",
                    rawPath, e.getClass().getSimpleName());
            return null;
        }
    }

    private SummonSkillCloudRequest.Roi toWindowRelativeRoi(int[] screenRect) {
        int x = screenRect[0] - tracker.getWindowBaseX();
        int y = screenRect[1] - tracker.getWindowBaseY();
        int width = Math.max(0, screenRect[2] - screenRect[0]);
        int height = Math.max(0, screenRect[3] - screenRect[1]);
        return SummonSkillCloudRequest.Roi.builder()
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .build();
    }

    private String currentTaskCode() {
        return currentRuntime()
                .map(WindowRuntimeContext::getSelectedTaskType)
                .filter(taskType -> taskType != null)
                .map(taskType -> taskType.getCode())
                .orElse("maintenance");
    }

    private String currentWindowId() {
        return currentRuntime()
                .map(WindowRuntimeContext::getWindowId)
                .orElse(null);
    }

    private String currentHwnd() {
        return currentRuntime()
                .map(WindowRuntimeContext::getNativeBinding)
                .filter(WindowNativeBinding::hasNativeHandle)
                .map(WindowNativeBinding::getNativeHandle)
                .orElse(null);
    }

    private String uniqueDebugImagePath(String prefix, String suffix) {
        return windowScopedTempPath.resolve(prefix + "_" + System.currentTimeMillis() + "_" + System.nanoTime() + suffix);
    }

    private Optional<WindowRuntimeContext> currentRuntime() {
        return windowTaskContextHolder == null ? Optional.empty() : windowTaskContextHolder.rawCurrent();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String roiText(SummonSkillCloudRequest.Roi roi) {
        if (roi == null) {
            return "";
        }
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    /**
     * Capture a tooltip rectangle and request cloud yellow-text preprocessing.
     *
     * @param tag safe-ish label for the per-window debug image names.
     * @param tipRect screen-absolute rectangle {@code [x1,y1,x2,y2]} around the tooltip.
     * @return scan metadata, or null when screenshot capture, cloud processing, or debug image write fails.
     */
    private YellowTipScan captureAndWashYellowTipOnce(String tag, int[] tipRect) {
        String safeTag = tag.replaceAll("[^a-zA-Z0-9_-]", "_");
        String debugPrefix = safeTag + "_" + System.currentTimeMillis() + "_" + System.nanoTime();
        String rawPath = windowScopedTempPath.resolve(debugPrefix + "_yellow_raw.png");
        String washedPath = windowScopedTempPath.resolve(debugPrefix + "_yellow_washed.png");

        boolean captured = tracker.captureToFile(
                "summon-skill-yellow-tip-" + tag,
                rawPath,
                tipRect[0],
                tipRect[1],
                tipRect[2],
                tipRect[3]
        );
        if (!captured) {
            log.warn("summon skill clean: yellow tooltip capture failed tag={}", tag);
            return null;
        }

        BufferedImage rawImage = readImage(rawPath);
        if (rawImage == null) {
            log.warn("summon skill clean: yellow tooltip raw image unreadable tag={} raw={}", tag, rawPath);
            return null;
        }

        ImageProcessorService.ImageProcessorResult countResult;
        ImageProcessorService.ImageProcessorResult washResult;
        try {
            countResult = imageProcessorService.countYellowPixels(rawImage,
                    imageProcessorMetadata(rawPath, "summon-skill-yellow-count", "summon-skill-yellow-tip-" + safeTag));
            if (!countResult.hasPixelCount()) {
                log.warn("summon skill clean: yellow tooltip count unavailable tag={} status={} reason={} raw={}",
                        tag, countResult.status(), countResult.reason(), rawPath);
                return null;
            }
            washResult = imageProcessorService.washYellowText(rawImage,
                    imageProcessorMetadata(rawPath, "summon-skill-yellow-wash", "summon-skill-yellow-tip-" + safeTag));
            if (!washResult.hasImage()) {
                log.warn("summon skill clean: yellow tooltip wash unavailable tag={} status={} reason={} raw={}",
                        tag, washResult.status(), washResult.reason(), rawPath);
                return null;
            }
            BufferedImage washedImage = washResult.image();
            try {
                saveImage(washedImage, washedPath);
            } catch (IOException e) {
                log.warn("summon skill clean: yellow tooltip washed image write failed tag={} path={} reason={}",
                        tag, washedPath, e.getClass().getSimpleName());
                return null;
            } finally {
                washedImage.flush();
            }
        } finally {
            rawImage.flush();
        }
        int yellowCount = countResult.pixelCount();
        log.info("summon skill clean: yellow tooltip captured tag={} rect={} yellowCount={} raw={} washed={}",
                tag, rectText(tipRect), yellowCount, rawPath, washedPath);
        return new YellowTipScan(tipRect, washedPath, yellowCount);
    }

    private boolean matchYellowTemplateInScan(String templatePath, YellowTipScan scan, String name) {
        if (!new File(templatePath).exists()) {
            log.debug("summon skill clean: status template missing name={} path={}", name, templatePath);
            return false;
        }

        double[] result = ImageFinder.find(scan.washedPath, templatePath, SKILL_STATUS_MATCH_RATE);
        if (result != null && result.length >= 2) {
            int absoluteX = scan.rect[0] + (int) Math.round(result[0]);
            int absoluteY = scan.rect[1] + (int) Math.round(result[1]);
            log.info("summon skill clean: status template matched name={} point=({}, {})", name, absoluteX, absoluteY);
            return true;
        }
        return false;
    }

    private ImageProcessorService.RequestMetadata imageProcessorMetadata(
            String rawPath,
            String phase,
            String debugImageId) {
        return ImageProcessorService.RequestMetadata.builder()
                .rawImagePath(rawPath)
                .debugImageId(debugImageId)
                .source("summon-skill")
                .taskCode(currentTaskCode())
                .phase(phase)
                .windowId(currentWindowId())
                .hwnd(currentHwnd())
                .build();
    }

    private static BufferedImage readImage(String imagePath) {
        try {
            return ImageIO.read(Path.of(imagePath).toFile());
        } catch (IOException e) {
            return null;
        }
    }

    private static void saveImage(BufferedImage image, String imagePath) throws IOException {
        if (image == null || imagePath == null || imagePath.isBlank()) {
            throw new IOException("missing image/output path");
        }
        Path path = Path.of(imagePath);
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", path.toFile())) {
            throw new IOException("no PNG writer accepted output path: " + imagePath);
        }
    }

    private static String rectText(int[] rect) {
        if (rect == null || rect.length < 4) {
            return "";
        }
        return rect[0] + "," + rect[1] + "," + rect[2] + "," + rect[3];
    }

    private int[] buildTipRectByHoverPoint(Point hoverAbsPoint) {
        int x1 = hoverAbsPoint.x + HOVER_TIP_OFFSET_X;
        int y1 = hoverAbsPoint.y + HOVER_TIP_OFFSET_Y;
        return new int[]{x1, y1, x1 + HOVER_TIP_AREA_W, y1 + HOVER_TIP_AREA_H};
    }

    private Point[] getSkillSlotOffsets(int skillCount) {
        return skillCount == 8 ? EIGHT_SKILL_SLOT_OFFSETS : SIX_SKILL_SLOT_OFFSETS;
    }

    private Point toAbsolutePoint(Point relativePoint) {
        int[] rect = coordinateHelper.getScaledRect(relativePoint.x, relativePoint.y, 1, 1);
        return new Point(rect[0], rect[1]);
    }

    private Point randomizeClickPoint(Point basePoint, int maxRadiusX, int maxRadiusY, String actionName) {
        Point randomized = coordinateHelper.getRandomizedPoint(basePoint, maxRadiusX, maxRadiusY);
        log.info("summon skill clean: randomized point action={} base=({}, {}) randomized=({}, {}) range={}x{}",
                actionName, basePoint.x, basePoint.y, randomized.x, randomized.y, maxRadiusX, maxRadiusY);
        return randomized;
    }

    private Point randomizeHoverPoint(Point basePoint, String actionName) {
        return randomizeClickPoint(basePoint, HOVER_RANDOM_X, HOVER_RANDOM_Y, actionName);
    }

    /**
     * Debug helper that only template-matches the summon attribute anchor.
     *
     * @return screen-absolute anchor point, or null when the template is not found.
     */
    public Point debugFindAttributeAnchorOnly() {
        return findAttributeAnchor(true);
    }

    /**
     * Debug helper for panel-drag decision.
     *
     * @return true when the detected panel anchor is too close to the right edge and should be dragged.
     */
    public boolean debugNeedDragOnly() {
        Point anchor = findAttributeAnchor(true);
        return anchor != null && needDragByAnchor(anchor);
    }

    /**
     * Debug helper for slot-count detection.
     *
     * @return detected summon skill slot count.
     */
    public int debugDetectSkillCountOnly() {
        return detectSummonSkillSlotCount();
    }

    /**
     * Debug helper that invokes tail cleanup directly from the UI/debug path.
     *
     * <p>This can send focused input, so it should only be called by an explicit debug action.</p>
     */
    public void debugCleanTailNormalSkillsOnly() {
        cleanTailNormalSkills();
    }

    private boolean isInputWorkerThread() {
        return Thread.currentThread().getName().contains("dhxy-input-action-worker");
    }

    private boolean isCleanDeadlineExceeded(long deadlineAtMs, String stage) {
        if (System.currentTimeMillis() <= deadlineAtMs) {
            return false;
        }
        log.warn("summon skill clean: timeout after {} ms at stage={}, abort current pass",
                CLEAN_ONCE_TIMEOUT_MS, stage);
        return true;
    }

    private enum StaticSkillSlotState {
        LOCKED,
        EMPTY,
        OCCUPIED,
        UNKNOWN
    }

    private record StaticSkillSlotTemplates(BufferedImage sealed,
                                            BufferedImage unobtained,
                                            BufferedImage inactive) {
    }

    private record StaticSlotScanResult(boolean success,
                                        int actionIndex,
                                        StaticSkillSlotState[] states,
                                        Map<Integer, SummonSkillSlotStatus> observedStatuses,
                                        String message) {
        private static StaticSlotScanResult success(int actionIndex,
                                                    StaticSkillSlotState[] states,
                                                    Map<Integer, SummonSkillSlotStatus> observedStatuses) {
            return new StaticSlotScanResult(true, actionIndex, states, observedStatuses, "ok");
        }

        private static StaticSlotScanResult failed(String message) {
            return failed(new StaticSkillSlotState[0], new HashMap<>(), message);
        }

        private static StaticSlotScanResult failed(StaticSkillSlotState[] states,
                                                   Map<Integer, SummonSkillSlotStatus> observedStatuses,
                                                   String message) {
            return new StaticSlotScanResult(false, -1, states, observedStatuses, message);
        }

        private StaticSkillSlotState stateAt(int index) {
            if (index < 0 || index >= states.length) {
                return StaticSkillSlotState.UNKNOWN;
            }
            return states[index];
        }

        private String statesText() {
            return SummonSkillService.statesText(states);
        }
    }

    private record ImagePayload(String base64, String sha256) {
    }

    private static class UltimateCornerResult {
        private final boolean completed;
        private final int nextStartIndex;
        private final boolean clicked;
        private final boolean succeeded;
        private final int deletedCount;
        private final int inspectedCount;
        private final String message;

        private UltimateCornerResult(boolean completed,
                                     int nextStartIndex,
                                     boolean clicked,
                                     boolean succeeded,
                                     int deletedCount,
                                     int inspectedCount,
                                     String message) {
            this.completed = completed;
            this.nextStartIndex = nextStartIndex;
            this.clicked = clicked;
            this.succeeded = succeeded;
            this.deletedCount = deletedCount;
            this.inspectedCount = inspectedCount;
            this.message = message;
        }

        private static UltimateCornerResult completed(int nextStartIndex,
                                                      boolean clicked,
                                                      boolean succeeded,
                                                      int deletedCount,
                                                      int inspectedCount,
                                                      String message) {
            return new UltimateCornerResult(true, nextStartIndex, clicked, succeeded, deletedCount, inspectedCount,
                    message);
        }

        private static UltimateCornerResult failed(int nextStartIndex,
                                                   boolean clicked,
                                                   boolean succeeded,
                                                   int deletedCount,
                                                   int inspectedCount,
                                                   String message) {
            return new UltimateCornerResult(false, nextStartIndex, clicked, succeeded, deletedCount, inspectedCount,
                    message);
        }
    }

}
