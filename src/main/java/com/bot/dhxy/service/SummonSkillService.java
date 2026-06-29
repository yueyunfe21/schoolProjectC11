package com.bot.dhxy.service;

import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.model.dialog.DialogResultStatus;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupRequest;
import com.bot.dhxy.model.maintenance.SummonSkillCleanupResult;
import com.bot.dhxy.model.maintenance.SummonSkillSlotStatus;
import com.bot.dhxy.runner.stop.TaskSleep;
import com.bot.dhxy.service.dialog.DialogHandleRequest;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
@RequiredArgsConstructor
public class SummonSkillService {

    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputSequences inputSequences;
    private final InputProvider inputProvider;
    private final UICleanerService uiCleanerService;
    private final DialogService dialogService;
    private final WindowScopedTempPath windowScopedTempPath;

    /**
     * Captured tooltip crop and washed yellow-text debug image.
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
    private static final long OPEN_SKILL_PANEL_WAIT_MS = 800L;
    private static final long DRAG_PANEL_WAIT_MS = 600L;
    private static final long SKILL_HOVER_WAIT_MS = 700L;
    private static final long ULTIMATE_CORNER_CLICK_WAIT_MS = 700L;
    private static final long CLEAN_ONCE_TIMEOUT_MS = 40_000L;

    private static final String ZHS_ATTRIBUTE_ANCHOR_PATH = "images/template/zhaohuanshou/ZHS_shuxing.png";
    private static final double ANCHOR_MATCH_RATE = 0.85;
    private static final double SKILL_STATUS_MATCH_RATE = 0.78;

    private static final Point[] SIX_SKILL_SLOT_OFFSETS = new Point[]{
            new Point(414, 387),
            new Point(329, 432),
            new Point(332, 515),
            new Point(420, 561),
            new Point(503, 514),
            new Point(502, 435)
    };

    private static final Point[] EIGHT_SKILL_SLOT_OFFSETS = new Point[]{
            new Point(410, 367),
            new Point(338, 406),
            new Point(312, 471),
            new Point(341, 544),
            new Point(408, 581),
            new Point(474, 541),
            new Point(500, 474),
            new Point(474, 402)
    };

    private static final int GAME_WINDOW_WIDTH = 1024;
    private static final int MIN_ANCHOR_DISTANCE_TO_RIGHT = 337;
    private static final int DRAG_TARGET_BASE_X = 518;
    private static final int DRAG_TARGET_BASE_Y = 428;
    private static final int DRAG_TARGET_RANDOM_X = 45;
    private static final int DRAG_TARGET_RANDOM_Y = 35;
    private static final int SKILL_BUTTON_OFFSET_X = 287;
    private static final int SKILL_BUTTON_OFFSET_Y = 213;

    private static final int EXTRA_SKILL_SLOT_HOVER_X = 501;
    private static final int EXTRA_SKILL_SLOT_HOVER_Y = 469;
    private static final int HOVER_TIP_OFFSET_X = 25;
    private static final int HOVER_TIP_OFFSET_Y = 0;
    private static final int HOVER_TIP_AREA_W = 237;
    private static final int HOVER_TIP_AREA_H = 123;
    private static final int ULTIMATE_CORNER_OFFSET_X = 26;
    private static final int ULTIMATE_CORNER_OFFSET_Y = -26;

    private static final int MIN_YELLOW_PIXEL_COUNT = 120;
    private static final int MAX_DELETE_SKILL_COUNT_PER_RUN = 5;
    private static final int STRONG_EXTRA_SLOT_YELLOW_PIXEL_COUNT = 500;

    private static final String SKILL_STATUS_NORMAL_PATH = "images/template/zhaohuanshou/status_normal.png";
    private static final String SKILL_STATUS_HIGH_PATH = "images/template/zhaohuanshou/status_high.png";
    private static final String SKILL_STATUS_ULTIMATE_PATH = "images/template/zhaohuanshou/status_ultimate.png";
    private static final String SKILL_STATUS_SEALED_PATH = "images/template/zhaohuanshou/status_sealed.png";
    private static final String SKILL_STATUS_UNOBTAINED_PATH = "images/template/zhaohuanshou/status_unobtained.png";
    private static final String SKILL_STATUS_INACTIVE_PATH = "images/template/zhaohuanshou/status_inactive.png";
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
        inputProvider.pressAltO();
        TaskSleep.sleep(900);

        Point anchor = findAttributeAnchor();
        if (anchor == null) {
            log.warn("summon skill clean: attribute anchor not found after Alt+O");
            return false;
        }

        if (dragPanelIfNeeded(anchor)) {
            log.info("summon skill clean: panel dragged, locate anchor again");
            anchor = findAttributeAnchor();
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
        inputProvider.clickLeft(skillButton.x, skillButton.y, 150);
        TaskSleep.sleep(OPEN_SKILL_PANEL_WAIT_MS);
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
        int defaultStartIndex = getTailCheckStartIndex(skillCount);
        boolean skillCountChanged = request.getExpectedSkillCount() != null
                && request.getExpectedSkillCount() != skillCount;
        SummonSkillCleanupRequest effectiveRequest = skillCountChanged
                ? request.toBuilder().skipUltimateCornerCheck(false).build()
                : request;
        int index = resolveStartIndex(request, skillCount, defaultStartIndex, slots.length);
        int nextStartIndex = index;
        int deletedCount = 0;
        int inspectedCount = 0;
        int handledBusinessDialogs = 0;
        boolean ultimateGenerateClicked = false;
        boolean ultimateGenerateSucceeded = false;
        Map<Integer, SummonSkillSlotStatus> observedStatuses = new HashMap<>();

        log.info("summon skill clean: detected {} skill slots, start at slot {} defaultStart={} expectedSkillCount={} skipUltimateCorner={}",
                skillCount, index + 1, defaultStartIndex + 1, request.getExpectedSkillCount(),
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
                SummonSkillSlotStatus status = inspectSkillSlot(slotAbsPoint);
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

                SummonSkillSlotStatus afterDeleteStatus = inspectSkillSlot(slotAbsPoint);
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
                index++;
                continue;
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
     * 这里只调用统一的 {@link DialogService#handleDialog(DialogHandleRequest)} 轻量维护广播入口；
     * 它只扫固定小区域里的医宝宝/修装备模板，命中后继续当前 slot。</p>
     */
    private boolean handleBusinessDialogDuringSkillClean(String stage) {
        DialogResultStatus status = dialogService
                .handleDialog(DialogHandleRequest.handleMaintenanceBroadcastOption("summon-skill:" + stage))
                .getStatus();
        if (status == DialogResultStatus.BUSINESS_OPTION_CLICKED) {
            log.info("summon skill clean: handled business dialog during pass stage={}", stage);
            return true;
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
        inputProvider.moveMouse(cornerPoint.x, cornerPoint.y);
        TaskSleep.sleep(SKILL_HOVER_WAIT_MS);
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
        inputProvider.clickLeft(cornerPoint.x, cornerPoint.y, 120);
        TaskSleep.sleep(ULTIMATE_CORNER_CLICK_WAIT_MS);
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
            SummonSkillSlotStatus afterGeneratedDelete = inspectSkillSlot(slotAbsPoint);
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
     * @return {@code 8} when the extra slot tooltip looks valid, otherwise {@code 6}. The detection
     * hovers a window-relative slot and captures a yellow-text tooltip crop.
     */
    public int detectSummonSkillSlotCount() {
        if (hoverExtraSkillSlotLooksValid()) {
            log.info("summon skill clean: extra slot tooltip is valid, treat as 8-slot layout");
            return 8;
        }
        log.info("summon skill clean: extra slot tooltip is not valid, treat as 6-slot layout");
        return 6;
    }

    private boolean hoverExtraSkillSlotLooksValid() {
        if (isInputWorkerThread()) {
            return hoverExtraSkillSlotLooksValidDirect();
        }
        final boolean[] result = new boolean[]{false};
        boolean ok = inputSequences.submitExclusiveAndWait("summonSkill:hoverExtraSlot", () -> {
            result[0] = hoverExtraSkillSlotLooksValidDirect();
            return true;
        });
        return ok && result[0];
    }

    private boolean hoverExtraSkillSlotLooksValidDirect() {
        Point hoverPoint = randomizeHoverPoint(toAbsolutePoint(new Point(EXTRA_SKILL_SLOT_HOVER_X, EXTRA_SKILL_SLOT_HOVER_Y)),
                "extra skill slot hover");
        inputProvider.moveMouse(hoverPoint.x, hoverPoint.y);
        TaskSleep.sleep(SKILL_HOVER_WAIT_MS);

        YellowTipScan scan = captureAndWashYellowTipOnce("extra_slot", buildTipRectByHoverPoint(hoverPoint));
        if (scan == null || scan.yellowCount < MIN_YELLOW_PIXEL_COUNT) {
            return false;
        }

        return matchAnySkillStatusTemplate(scan)
                || scan.yellowCount >= STRONG_EXTRA_SLOT_YELLOW_PIXEL_COUNT;
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
     * Hover one screen-absolute slot point and classify the visible yellow tooltip.
     */
    private SummonSkillSlotStatus inspectSkillSlotDirect(Point slotAbsPoint) {
        Point hoverPoint = randomizeHoverPoint(slotAbsPoint, "skill slot hover");
        inputProvider.moveMouse(hoverPoint.x, hoverPoint.y);
        TaskSleep.sleep(SKILL_HOVER_WAIT_MS);
        return inspectCurrentHoverTip(hoverPoint);
    }
    private SummonSkillSlotStatus inspectCurrentHoverTip(Point hoverAbsPoint) {
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
        inputProvider.clickLeft(slotClickPoint.x, slotClickPoint.y, 120);
        TaskSleep.sleep(SELECT_SKILL_WAIT_MS);

        Point deleteButton = randomizeClickPoint(toAbsolutePoint(new Point(DELETE_SKILL_BUTTON_X, DELETE_SKILL_BUTTON_Y)),
                NORMAL_CLICK_RANDOM_X, NORMAL_CLICK_RANDOM_Y, "delete skill button");
        inputProvider.clickLeft(deleteButton.x, deleteButton.y, 120);
        TaskSleep.sleep(DELETE_DIALOG_WAIT_MS);

        Point confirmButtonPoint = findForgetConfirmButton();
        if (confirmButtonPoint == null) {
            log.warn("summon skill clean: forget confirm button not found; delete failed");
            return false;
        }

        Point confirmClickPoint = randomizeClickPoint(confirmButtonPoint, CONFIRM_CLICK_RANDOM_X, CONFIRM_CLICK_RANDOM_Y,
                "forget confirm button");
        inputProvider.clickLeft(confirmClickPoint.x, confirmClickPoint.y, 120);
        TaskSleep.sleep(FORGET_DONE_WAIT_MS);
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

    private Point findAttributeAnchor() {
        tracker.updateGlobalVision();
        Point anchor = coordinateHelper.findImageAbsoluteCoordinateByImagePath(
                ZHS_ATTRIBUTE_ANCHOR_PATH,
                tracker.getLatestVisionPath(),
                ANCHOR_MATCH_RATE
        );
        if (anchor == null) {
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
        inputProvider.dragAndDrop(dragFrom.x, dragFrom.y, dragTo.x, dragTo.y);
        TaskSleep.sleep(DRAG_PANEL_WAIT_MS);
        return true;
    }

    private boolean matchAnySkillStatusTemplate(YellowTipScan scan) {
        return matchYellowTemplateInScan(SKILL_STATUS_SEALED_PATH, scan, "sealed")
                || matchYellowTemplateInScan(SKILL_STATUS_UNOBTAINED_PATH, scan, "unobtained")
                || matchYellowTemplateInScan(SKILL_STATUS_INACTIVE_PATH, scan, "inactive")
                || matchYellowTemplateInScan(SKILL_STATUS_NORMAL_PATH, scan, "normal")
                || matchYellowTemplateInScan(SKILL_STATUS_HIGH_PATH, scan, "high")
                || matchYellowTemplateInScan(SKILL_STATUS_ULTIMATE_PATH, scan, "ultimate");
    }

    /**
     * Capture a tooltip rectangle and create a yellow-text-only debug image.
     *
     * @param tag safe-ish label for the per-window debug image names.
     * @param tipRect screen-absolute rectangle {@code [x1,y1,x2,y2]} around the tooltip.
     * @return scan metadata, or null when screenshot capture fails.
     */
    private YellowTipScan captureAndWashYellowTipOnce(String tag, int[] tipRect) {
        String safeTag = tag.replaceAll("[^a-zA-Z0-9_-]", "_");
        String rawPath = windowScopedTempPath.resolve(safeTag + "_yellow_raw.png");
        String washedPath = windowScopedTempPath.resolve(safeTag + "_yellow_washed.png");

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

        BufferedImage rawImage = ImagePreprocessor.pathToBufferedImage(rawPath);
        int yellowCount = ImagePreprocessor.countYellowPixels(rawImage);
        if (rawImage != null) {
            rawImage.flush();
        }
        ImagePreprocessor.washYellowText(rawPath, washedPath);
        log.info("summon skill clean: yellow tooltip captured tag={} rect={} yellowCount={} raw={} washed={}",
                tag, ImagePreprocessor.rectToString(tipRect), yellowCount, rawPath, washedPath);
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

    private int[] buildTipRectByHoverPoint(Point hoverAbsPoint) {
        int x1 = hoverAbsPoint.x + HOVER_TIP_OFFSET_X;
        int y1 = hoverAbsPoint.y + HOVER_TIP_OFFSET_Y;
        return new int[]{x1, y1, x1 + HOVER_TIP_AREA_W, y1 + HOVER_TIP_AREA_H};
    }

    private Point[] getSkillSlotOffsets(int skillCount) {
        return skillCount == 8 ? EIGHT_SKILL_SLOT_OFFSETS : SIX_SKILL_SLOT_OFFSETS;
    }

    private int getTailCheckStartIndex(int skillCount) {
        return skillCount == 8 ? 6 : 3;
    }

    static int resolveStartIndex(SummonSkillCleanupRequest request,
                                 int detectedSkillCount,
                                 int defaultStartIndex,
                                 int slotLength) {
        Integer expectedSkillCount = request.getExpectedSkillCount();
        Integer cachedStartIndex = request.getStartSlotIndex();
        if (expectedSkillCount == null
                || expectedSkillCount != detectedSkillCount
                || cachedStartIndex == null) {
            return defaultStartIndex;
        }
        if (cachedStartIndex < 0) {
            return 0;
        }
        if (cachedStartIndex > slotLength) {
            return slotLength;
        }
        return cachedStartIndex;
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
        return findAttributeAnchor();
    }

    /**
     * Debug helper for panel-drag decision.
     *
     * @return true when the detected panel anchor is too close to the right edge and should be dragged.
     */
    public boolean debugNeedDragOnly() {
        Point anchor = findAttributeAnchor();
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
