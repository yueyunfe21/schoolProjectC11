package com.bot.dhxy.team;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;


import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.cloud.task.ImageProcessorService;
import com.bot.dhxy.cloud.task.ImageProcessorService.ImageProcessorResult;
import com.bot.dhxy.cloud.task.ImageProcessorService.RequestMetadata;
import com.bot.dhxy.cloud.task.ImageProcessorService.TeamTooltipTextStats;
import com.bot.dhxy.cloud.task.TeamRoleTooltipCloudDecision;
import com.bot.dhxy.cloud.task.TeamRoleTooltipCloudDecisionService;
import com.bot.dhxy.cloud.task.TeamRoleTooltipCloudRequest;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputActionScope;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the current in-game team role for the bound window.
 *
 * <p>Window registration role is only UI/runtime metadata. Real leader/member decisions should go
 * through this service. Detection has two stages: first hover over the configured team tooltip area
 * and validate text-like white/purple pixels to decide grouped-vs-solo. When a tooltip is present,
 * the tooltip is treated as the leader card: OCR reads its numeric ID and compares that ID with the
 * bound window title. Alt+T panel template matching is now a fallback for tooltip OCR misses and
 * for hot-start cases where dialogs suppress hover tooltips. Hover/panel probing uses exclusive
 * input because it moves the physical mouse and presses Alt+T; OCR runs after screenshots are saved
 * so slow local OCR does not hold the global input turn.</p>
 */
@Slf4j
@Service
public class TeamRoleDetectionService {

    /**
     * High-variance cutoff for the team-status strip after tooltip probing fails.
     *
     * <p>The value comes from the user's live capture of the member/status area: a real grouped
     * strip measured about 69.55, while an empty/flat area should be lower. This is deliberately
     * only a grouped-vs-solo fallback; leader/member identity still comes from the existing Alt+T
     * panel templates.</p>
     */
    private static final double TEAM_STATUS_GROUPED_STDDEV_THRESHOLD = 60.0;
    private static final int TOOLTIP_OCR_FULL_RETRY_LIMIT = 2;
    private static final int TEAM_TOOLTIP_RETRY_DELAY_MS = 1000;
    private static final int BATCH_TOOLTIP_DECISION_CACHE_MAX_ENTRIES = 512;
    private static final Pattern WINDOW_TITLE_ID_PATTERN = Pattern.compile("ID\\D{0,4}(\\d+)");

    private final TeamTaskProperties teamTaskProperties;
    private final BotProperties botProperties;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowFocusService windowFocusService;
    private final WindowScopedTempPath windowScopedTempPath;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final ImageProcessorService imageProcessorService;
    private final TeamRoleTooltipCloudDecisionService teamRoleTooltipCloudDecisionService;
    private final Map<String, CompletableFuture<TeamRoleTooltipCloudDecision>> batchTooltipDecisionByHash =
            new ConcurrentHashMap<>();

    /**
     * Create the team-role detector.
     *
     * @param teamTaskProperties role-detection coordinates, thresholds, templates, and switches.
     * @param botProperties shared game-task configuration; this service reads the return-team
     *                      status area as a screen-region fallback after tooltip probing fails.
     * @param tracker screenshot provider for the currently bound window.
     * @param coordinateHelper coordinate scaler/randomizer for configured 1024x768 client points.
     * @param inputProvider direct input provider used only inside exclusive input callbacks.
     * @param inputSequences global input queue used to serialize hover and Alt+T probing.
     * @param windowTaskContextHolder current window binding lookup.
     * @param windowFocusService best-effort foreground helper for probe phases that require mouse input.
     * @param windowScopedTempPath per-window debug image path resolver.
     * @param bindingRefreshService live HWND/title refresh helper used before reading current title.
     * @param imageProcessorService unified cloud image-processor facade for tooltip text metrics,
     *                              purple washing, and status-strip stddev.
     * @param teamRoleTooltipCloudDecisionService cloud business-vision endpoint for classifying the
     *                                            hovered team tooltip by leader ID.
     */
    public TeamRoleDetectionService(TeamTaskProperties teamTaskProperties,
                                    BotProperties botProperties,
                                    GameClientTracker tracker,
                                    CoordinateHelper coordinateHelper,
                                    InputProvider inputProvider,
                                    InputSequences inputSequences,
                                    WindowTaskContextHolder windowTaskContextHolder,
                                    WindowFocusService windowFocusService,
                                    WindowScopedTempPath windowScopedTempPath,
                                    WindowNativeBindingRefreshService bindingRefreshService,
                                    ImageProcessorService imageProcessorService,
                                    TeamRoleTooltipCloudDecisionService teamRoleTooltipCloudDecisionService) {
        this.teamTaskProperties = teamTaskProperties;
        this.botProperties = botProperties;
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.inputProvider = inputProvider;
        this.inputSequences = inputSequences;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowFocusService = windowFocusService;
        this.windowScopedTempPath = windowScopedTempPath;
        this.bindingRefreshService = bindingRefreshService;
        this.imageProcessorService = imageProcessorService;
        this.teamRoleTooltipCloudDecisionService = teamRoleTooltipCloudDecisionService;
    }

    /**
     * Detect the current role using normal feature switches.
     *
     * @param context current window execution context; may be null for legacy/debug calls.
     * @return SOLO, LEADER, MEMBER, or UNKNOWN. UNKNOWN means detection is disabled, misconfigured,
     * or panel matching failed after retries.
     */
    public TeamRoleStatus detectCurrentRole(TaskExecutionContext context) {
        return detectCurrentRoleWithEvidence(context).role();
    }

    private TeamRoleStatus detectCurrentRole(TaskExecutionContext context, boolean force) {
        return detectCurrentRoleWithEvidence(context, force).role();
    }

    public TeamRoleDetectionResult detectCurrentRoleWithEvidence(TaskExecutionContext context) {
        return detectCurrentRoleWithEvidence(context, false);
    }

    private TeamRoleDetectionResult detectCurrentRoleWithEvidence(TaskExecutionContext context, boolean force) {
        if (!force && !teamTaskProperties.isRoleDetectionEnabled()) {
            if (context != null && context.hasWindow()) {
                log.debug("team role detection disabled: {}", context.getLogPrefix());
            }
            return TeamRoleDetectionResult.unknown();
        }

        if (!hasConfiguredHoverProbe()) {
            log.warn("team role detection missing hover/tooltip config, return UNKNOWN");
            return TeamRoleDetectionResult.unknown();
        }

        /*
         * A visible tooltip proves the role is grouped, but if OCR cannot read the leader ID and the
         * panel fallback is also inconclusive, the whole sequence is repeated once from the hover
         * stage. This is stricter than the no-tooltip/stddev branch because a real tooltip means the
         * client is grouped and silently returning SOLO would hide a broken role detector.
         */
        for (int pass = 1; pass <= TOOLTIP_OCR_FULL_RETRY_LIMIT; pass++) {
            RoleDetectionPassResult passResult = detectCurrentRolePass(context, pass, force);
            if (!passResult.retryWholeFlow()) {
                return new TeamRoleDetectionResult(passResult.role(), passResult.tooltipGroupEvidence());
            }
            if (pass < TOOLTIP_OCR_FULL_RETRY_LIMIT) {
                log.warn("team role detection retrying full flow: pass={}/{} reason={}",
                        pass, TOOLTIP_OCR_FULL_RETRY_LIMIT, passResult.reason());
                continue;
            }
            throw new IllegalStateException("Team role detection failed after tooltip OCR and panel fallback: "
                    + passResult.reason());
        }
        return TeamRoleDetectionResult.unknown();
    }

    /**
     * Run one complete role-detection pass.
     *
     * <p>The pass has three ordered exits: tooltip ID OCR, no-tooltip standard-deviation fallback,
     * and the optional Alt+T panel fallback. The panel fallback opens the real team panel, so normal
     * task startup must not use it; only explicit debug detection may enable it.</p>
     *
     * @param context current task context, used only to resolve the bound window title and player ID.
     * @param pass one-based outer pass index used in debug image names and logs.
     * @param allowPanelProbe true only for explicit debug probes that may safely open the team panel.
     * @return a terminal role result, or a retry request when tooltip OCR and panel fallback both
     *         failed after a tooltip was positively detected.
     */
    private RoleDetectionPassResult detectCurrentRolePass(TaskExecutionContext context, int pass, boolean allowPanelProbe) {
        Optional<TeamTooltipProbe> tooltip = hoverAndCaptureTeamTooltipWithRetries(pass);
        if (tooltip.isPresent()) {
            Optional<TooltipRoleResult> roleFromTooltipId = detectGroupedRoleFromTooltipLeaderId(context, tooltip.get());
            if (roleFromTooltipId.isPresent()) {
                TooltipRoleResult result = roleFromTooltipId.get();
                return RoleDetectionPassResult.done(result.role(), result.tooltipGroupEvidence(), "tooltip-id");
            }

            if (!allowPanelProbe) {
                log.warn("team role detection: tooltip OCR missed; skip Alt+T panel fallback during normal startup");
                return RoleDetectionPassResult.done(TeamRoleStatus.UNKNOWN, "tooltip-ocr-miss-panel-disabled");
            }
            if (!hasConfiguredPanelProbe()) {
                log.warn("team role detection missing team panel template/rect config after tooltip OCR miss");
                return RoleDetectionPassResult.done(TeamRoleStatus.UNKNOWN, "tooltip-ocr-miss-panel-config-missing");
            }

            TeamRoleStatus panelRole = detectGroupedRoleFromPanelOnce(pass);
            if (!panelRole.isUnknown()) {
                return RoleDetectionPassResult.done(panelRole, "tooltip-ocr-panel-fallback");
            }
            return RoleDetectionPassResult.retry("tooltip detected but OCR ID and panel marker both failed");
        }

        /*
         * Tooltip missing means either solo or a blocked hover state. Preserve the old behavior:
         * retry hover first, then use the high-variance status strip as permission to try Alt+T.
         * If that panel path is still inconclusive, return SOLO as the user requested.
         */
        if (!detectGroupedByTeamStatusDeviationFallback()) {
            log.info("team role detection: no team tooltip/status signal detected, role=SOLO");
            return RoleDetectionPassResult.done(TeamRoleStatus.SOLO, "no-tooltip-low-deviation");
        }
        if (!allowPanelProbe) {
            log.warn("team role detection: status deviation suggested grouped; skip Alt+T panel fallback during normal startup");
            return RoleDetectionPassResult.done(TeamRoleStatus.UNKNOWN, "status-deviation-panel-disabled");
        }
        if (!hasConfiguredPanelProbe()) {
            log.warn("team role detection missing team panel template/rect config, return UNKNOWN");
            return RoleDetectionPassResult.done(TeamRoleStatus.UNKNOWN, "status-deviation-panel-config-missing");
        }

        TeamRoleStatus panelRole = detectGroupedRoleFromPanelWithRetries("status-deviation");
        if (!panelRole.isUnknown()) {
            return RoleDetectionPassResult.done(panelRole, "status-deviation-panel");
        }
        log.warn("team role detection: status deviation suggested grouped, but panel fallback failed; role=SOLO");
        return RoleDetectionPassResult.done(TeamRoleStatus.SOLO, "status-deviation-panel-unknown");
    }

    /**
     * Decide whether the bound window may run Five Ring after live role detection.
     *
     * @param context current window context.
     * @return true when policy allows Five Ring for the detected role.
     */
    public boolean shouldRunFiveRing(TaskExecutionContext context) {
        TeamRoleStatus role = detectCurrentRole(context);
        return shouldRunFiveRing(role);
    }

    /**
     * Decide whether Five Ring is allowed for a known role.
     *
     * @param role detected role; null is treated as UNKNOWN.
     * @return true when the task is allowed by configuration.
     */
    public boolean shouldRunFiveRing(TeamRoleStatus role) {
        if (!teamTaskProperties.isFiveRingRequiresLeader()) {
            return true;
        }
        TeamRoleStatus safeRole = role == null ? TeamRoleStatus.UNKNOWN : role;
        if (safeRole.isLeader()) {
            return true;
        }
        return safeRole.isUnknown() && teamTaskProperties.isAllowFiveRingWhenRoleUnknown();
    }

    /**
     * Decide whether the bound window may run auto-battle after live role detection.
     *
     * @param context current window context.
     * @return true when policy allows auto-battle for the detected role.
     */
    public boolean shouldRunAutoBattle(TaskExecutionContext context) {
        TeamRoleStatus role = detectCurrentRole(context);
        return shouldRunAutoBattle(role);
    }

    /**
     * Decide whether auto-battle is allowed for a known role.
     *
     * @param role detected role; null is treated as UNKNOWN.
     * @return true when the task is allowed by configuration.
     */
    public boolean shouldRunAutoBattle(TeamRoleStatus role) {
        if (!teamTaskProperties.isAutoBattleRequiresMember()) {
            return true;
        }
        TeamRoleStatus safeRole = role == null ? TeamRoleStatus.UNKNOWN : role;
        if (safeRole.isMember()) {
            return true;
        }
        return safeRole.isUnknown() && teamTaskProperties.isAllowAutoBattleWhenRoleUnknown();
    }

    /**
     * Decide whether lightweight member cleanup may run after live role detection.
     *
     * @param context current window context.
     * @return true when policy allows lightweight cleanup for the detected role.
     */
    public boolean shouldRunLightweightCleanup(TaskExecutionContext context) {
        TeamRoleStatus role = detectCurrentRole(context);
        return shouldRunLightweightCleanup(role);
    }

    /**
     * Decide whether lightweight cleanup is allowed for a known role.
     *
     * @param role detected role; null is treated as UNKNOWN.
     * @return true when cleanup is allowed by configuration.
     */
    public boolean shouldRunLightweightCleanup(TeamRoleStatus role) {
        if (!teamTaskProperties.isLightweightCleanupRequiresMember()) {
            return true;
        }
        TeamRoleStatus safeRole = role == null ? TeamRoleStatus.UNKNOWN : role;
        if (safeRole.isMember()) {
            return true;
        }
        return safeRole.isUnknown() && teamTaskProperties.isAllowLightweightCleanupWhenRoleUnknown();
    }

    /**
     * Detect and format the current role for UI/log display.
     *
     * @param context current window context.
     * @return enum name of the detected role.
     */
    public String describeCurrentRole(TaskExecutionContext context) {
        return detectCurrentRole(context).name();
    }

    /**
     * Retry the hover-only tooltip probe before deciding that the current role might be SOLO.
     *
     * <p>This intentionally does not open the team panel. Missing the hover tooltip usually means
     * the mouse landed on a bad edge pixel, an existing dialog suppressed hover rendering, or
     * startup focus was still settling. Each failed attempt waits briefly before trying again; only
     * after all hover attempts miss does the caller consider the standard-deviation fallback.</p>
     *
     * @param pass one-based outer pass index used to keep debug screenshots readable.
     * @return detected tooltip metadata and image paths, or empty when all hover attempts miss.
     */
    private Optional<TeamTooltipProbe> hoverAndCaptureTeamTooltipWithRetries(int pass) {
        int maxAttempts = Math.max(1, teamTaskProperties.getTeamTooltipProbeMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            TeamTooltipProbe probe = hoverAndCaptureTeamTooltipOnce(pass, attempt);
            if (probe.detected()) {
                return Optional.of(probe);
            }
            log.warn("team role tooltip probe missed, will retry if possible: pass={} attempt={}/{}",
                    pass, attempt, maxAttempts);
            if (attempt < maxAttempts) {
                TaskSleep.sleep(TEAM_TOOLTIP_RETRY_DELAY_MS);
            }
        }
        return Optional.empty();
    }

    /**
     * Hover the configured avatar/team area once, save the tooltip crop, and classify it.
     *
     * <p>The input-sensitive part is limited to focus, mouse move, hover wait, and screenshot
     * capture. The saved raw path is later consumed by local OCR outside the exclusive input queue.
     * Tooltip classification combines white/purple pixel counts with a text-distribution check to
     * reduce false positives from dark game backgrounds.</p>
     *
     * @param pass one-based outer retry pass.
     * @param attempt one-based hover retry index inside the pass.
     * @return captured tooltip probe information. {@link TeamTooltipProbe#detected()} is false when
     * capture failed or the crop did not look like a team tooltip.
     */
    private TeamTooltipProbe hoverAndCaptureTeamTooltipOnce(int pass, int attempt) {
        int[] tooltipRect = coordinateHelper.getScaledRect(
                teamTaskProperties.getTeamTooltipRectX(),
                teamTaskProperties.getTeamTooltipRectY(),
                teamTaskProperties.getTeamTooltipRectW(),
                teamTaskProperties.getTeamTooltipRectH());
        int[] hoverRect = coordinateHelper.getScaledRect(
                teamTaskProperties.getTeamHoverX(),
                teamTaskProperties.getTeamHoverY(),
                1,
                1);
        java.awt.Point hoverPoint = coordinateHelper.getRandomizedPoint(
                hoverRect[0],
                hoverRect[1],
                teamTaskProperties.getTeamHoverRandomRadiusX(),
                teamTaskProperties.getTeamHoverRandomRadiusY());
        TeamTooltipProbe[] result = {TeamTooltipProbe.missed(null, null)};
        boolean completed = inputSequences.submitExclusiveAndWait("teamRole:hoverAndCaptureTooltip:pass" + pass + ":attempt" + attempt, () -> {
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            focusCurrentWindowForProbe("hover");
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.moveMouse(hoverPoint.x, hoverPoint.y);
            if (!TaskSleep.sleep(Math.max(0, teamTaskProperties.getTeamHoverDelayMs()))
                    || !InputActionScope.checkpoint()) {
                return false;
            }
            BufferedImage image = tracker.captureToMemory("team-role-tooltip",
                    tooltipRect[0], tooltipRect[1], tooltipRect[2], tooltipRect[3]);
            if (image == null) {
                return true;
            }
            try {
                String rawPath = windowScopedTempPath.resolve("team_role_tooltip_raw_pass" + pass
                        + "_attempt" + attempt + ".png");
                String purplePath = windowScopedTempPath.resolve("team_role_tooltip_purple_pass" + pass
                        + "_attempt" + attempt + ".png");
                saveBufferedImage(image, rawPath);
                result[0] = TeamTooltipProbe.missed(rawPath, purplePath);
                log.info("team role tooltip probe captured: pass={} attempt={} hover=({}, {}) raw={}",
                        pass, attempt, hoverPoint.x, hoverPoint.y, rawPath);
                return true;
            } finally {
                image.flush();
            }
        });
        if (!completed) {
            log.warn("team role tooltip probe did not complete");
            return TeamTooltipProbe.missed(null, null);
        }
        TeamTooltipProbe probe = result[0];
        if (probe.rawPath() != null && probe.purplePath() != null) {
            probe = inspectTeamTooltipProbe(pass, attempt, hoverPoint, probe);
        }
        return probe;
    }

    private TeamTooltipProbe inspectTeamTooltipProbe(int pass,
                                                     int attempt,
                                                     java.awt.Point hoverPoint,
                                                     TeamTooltipProbe probe) {
        try {
            BufferedImage image = ImageIO.read(Path.of(probe.rawPath()).toFile());
            if (image == null) {
                log.warn("team role tooltip probe inspect skipped: raw unreadable path={}", probe.rawPath());
                return probe;
            }
            try {
                RequestMetadata metadata = imageProcessorMetadata(
                        probe.rawPath(),
                        "team-role-tooltip-text",
                        "team-tooltip-pass" + pass + "-attempt" + attempt);
                ImageProcessorResult statsResult = imageProcessorService.measureTeamTooltipText(image, metadata);
                TeamTooltipTextStats stats = statsResult.teamTooltipTextStats();
                if (stats == null) {
                    log.info("team role tooltip cloud metrics missed: pass={} attempt={} raw={} status={} reason={}",
                            pass, attempt, probe.rawPath(), statsResult.status(), statsResult.reason());
                    return probe;
                }
                TextDistribution distribution = new TextDistribution(
                        stats.rows(), stats.columns(), stats.transitions(), stats.maxRowPixels());
                boolean detected = stats.whitePixels() >= teamTaskProperties.getTeamTooltipWhitePixelThreshold()
                        && isTextLikeDistribution(distribution, image.getWidth());
                TeamTooltipProbe inspected = new TeamTooltipProbe(
                        detected,
                        probe.rawPath(),
                        probe.purplePath(),
                        stats.whitePixels(),
                        stats.purplePixels(),
                        distribution);
                log.info("team role tooltip probe: pass={} attempt={} hover=({}, {}) raw={} white={} purple={} distribution={} thresholds=(white>={}, rows>={}, cols>={}, transitions>={}, maxRowCoverage<={}%)",
                        pass, attempt, hoverPoint.x, hoverPoint.y, probe.rawPath(), stats.whitePixels(),
                        stats.purplePixels(), distribution,
                        teamTaskProperties.getTeamTooltipWhitePixelThreshold(),
                        teamTaskProperties.getTeamTooltipTextMinRows(),
                        teamTaskProperties.getTeamTooltipTextMinColumns(),
                        teamTaskProperties.getTeamTooltipTextMinTransitions(),
                        teamTaskProperties.getTeamTooltipTextMaxRowCoveragePercent());

                return inspected;
            } finally {
                image.flush();
            }
        } catch (IOException e) {
            log.warn("team role tooltip probe inspect failed: raw={} reason={}",
                    probe.rawPath(), e.getMessage(), e);
            return probe;
        }
    }

    /**
     * Classify leader/member by reading the leader ID from a detected team tooltip.
     *
     * <p>Live testing showed that the team hover tooltip is the leader card. That means the numeric
     * ID in the tooltip is enough to decide the bound window's role: equal to the current window
     * title ID means LEADER, different means MEMBER. The normal path now sends a high-contrast mask
     * of the tooltip image to the cloud business-vision service. The old local OCR selector is kept
     * as deprecated code only and is not used by the production cloud path.</p>
     *
     * @param context current task context, used to resolve the bound window title first.
     * @param probe detected tooltip probe containing the saved raw screenshot path.
     * @return LEADER/MEMBER when both IDs are available, otherwise empty so the caller can use the
     *         panel fallback.
     */
    private Optional<TooltipRoleResult> detectGroupedRoleFromTooltipLeaderId(TaskExecutionContext context,
                                                                             TeamTooltipProbe probe) {
        Optional<String> currentPlayerId = resolveCurrentPlayerId(context);
        if (currentPlayerId.isEmpty()) {
            log.warn("team role tooltip ID skipped: current player ID unavailable");
            return Optional.empty();
        }

        if (teamRoleTooltipCloudDecisionService.isActive()) {
            TeamRoleDetectionResult roleFromCloud = detectGroupedRoleFromCloudTooltip(context, probe, currentPlayerId.get());
            if (!roleFromCloud.role().isUnknown()) {
                return Optional.of(new TooltipRoleResult(roleFromCloud.role(), roleFromCloud.tooltipGroupEvidence()));
            }
            log.warn("team role cloud tooltip did not return a usable role; skip local OCR while cloud service is active: currentPlayerId={} raw={}",
                    currentPlayerId.get(), probe == null ? null : probe.rawPath());
            return Optional.empty();
        }

        log.warn("team role cloud tooltip disabled; deprecated local tooltip OCR is not used: currentPlayerId={} raw={}",
                currentPlayerId.get(), probe == null ? null : probe.rawPath());
        return Optional.empty();
    }

    private TeamRoleDetectionResult detectGroupedRoleFromCloudTooltip(TaskExecutionContext context,
                                                                      TeamTooltipProbe probe,
                                                                      String currentPlayerId) {
        TeamRoleTooltipCloudRequest request = buildCloudTooltipRequest(context, probe, currentPlayerId);
        String tooltipGroupSignature = tooltipTextSignature(probe);
        if (tooltipGroupSignature == null) {
            tooltipGroupSignature = tooltipMaskSignature(probe == null ? null : probe.rawPath());
        }
        if (tooltipGroupSignature == null && request != null) {
            tooltipGroupSignature = normalize(request.getImageSha256());
        }
        return detectGroupedRoleFromCloudTooltipRequest(context, request, tooltipGroupSignature);
    }

    TeamRoleDetectionResult detectGroupedRoleFromCloudTooltipRequest(TaskExecutionContext context,
                                                                     TeamRoleTooltipCloudRequest request) {
        return detectGroupedRoleFromCloudTooltipRequest(
                context,
                request,
                request == null ? null : normalize(request.getImageSha256()));
    }

    private TeamRoleDetectionResult detectGroupedRoleFromCloudTooltipRequest(TaskExecutionContext context,
                                                                            TeamRoleTooltipCloudRequest request,
                                                                            String tooltipGroupSignature) {
        if (request == null) {
            return TeamRoleDetectionResult.unknown();
        }
        String groupHash = normalize(tooltipGroupSignature);
        if (groupHash == null) {
            groupHash = normalize(request.getImageSha256());
        }
        TeamRoleTooltipCloudDecision decision = detectCloudTooltipDecision(context, request, groupHash);
        if (!decision.isFound()) {
            log.warn("team role cloud tooltip missed: status={} role={} currentPlayerId={} raw={} reason={} debugToken={}",
                    decision.getStatus(), decision.getRole(), request.getCurrentPlayerId(),
                    request.getRawImagePath(), decision.getReason(), decision.getDebugToken());
            return TeamRoleDetectionResult.unknown();
        }

        String leaderClientId = normalize(decision.getLeaderClientId());
        String currentPlayerId = normalize(request.getCurrentPlayerId());
        if (leaderClientId == null || currentPlayerId == null) {
            log.warn("team role cloud tooltip returned incomplete IDs: leaderClientId={} currentPlayerId={} raw={} reason={}",
                    decision.getLeaderClientId(), request.getCurrentPlayerId(),
                    request.getRawImagePath(), decision.getReason());
            return TeamRoleDetectionResult.unknown();
        }
        TeamRoleStatus role = leaderClientId.equals(currentPlayerId)
                ? TeamRoleStatus.LEADER
                : TeamRoleStatus.MEMBER;
        TeamTooltipGroupEvidence evidence = new TeamTooltipGroupEvidence(
                groupHash,
                leaderClientId,
                currentPlayerId,
                normalize(request.getWindowId()),
                normalize(request.getRawImagePath()),
                decision.getReason(),
                decision.getDebugToken());
        log.info("team role detection: cloud tooltip derived role from leader id: groupHash={} leaderClientId={} currentPlayerId={} role={} raw={} reason={} debugToken={}",
                evidence.groupHash(), leaderClientId, currentPlayerId, role,
                request.getRawImagePath(), decision.getReason(), decision.getDebugToken());
        return new TeamRoleDetectionResult(role, evidence);
    }

    private TeamRoleTooltipCloudDecision detectCloudTooltipDecision(TaskExecutionContext context,
                                                                    TeamRoleTooltipCloudRequest request,
                                                                    String tooltipGroupSignature) {
        return detectCloudTooltipDecision(context, request, tooltipGroupSignature, true);
    }

    private TeamRoleTooltipCloudDecision detectCloudTooltipDecision(TaskExecutionContext context,
                                                                    TeamRoleTooltipCloudRequest request,
                                                                    String tooltipGroupSignature,
                                                                    boolean retryAfterNoResult) {
        String groupHash = normalize(tooltipGroupSignature);
        String cacheKey = batchTooltipDecisionCacheKey(context, groupHash);
        if (cacheKey == null) {
            return teamRoleTooltipCloudDecisionService.detect(request);
        }
        pruneBatchTooltipDecisionCacheIfNeeded();
        CompletableFuture<TeamRoleTooltipCloudDecision> created = new CompletableFuture<>();
        CompletableFuture<TeamRoleTooltipCloudDecision> existing =
                batchTooltipDecisionByHash.putIfAbsent(cacheKey, created);
        if (existing == null) {
            try {
                TeamRoleTooltipCloudDecision decision = teamRoleTooltipCloudDecisionService.detect(request);
                created.complete(decision);
                if (!decision.isFound()) {
                    batchTooltipDecisionByHash.remove(cacheKey, created);
                }
                log.info("team role cloud tooltip hash group representative executed: session={} groupHash={} payloadImageSha256={} windowId={} currentPlayerId={} status={} leaderClientId={}",
                        context.getLocalTeamSessionKey(), groupHash, request.getImageSha256(),
                        request.getWindowId(), request.getCurrentPlayerId(),
                        decision.getStatus(), decision.getLeaderClientId());
                return decision;
            } catch (RuntimeException e) {
                created.completeExceptionally(e);
                batchTooltipDecisionByHash.remove(cacheKey, created);
                throw e;
            }
        }
        try {
            TeamRoleTooltipCloudDecision decision = existing.join();
            if (!decision.isFound() && retryAfterNoResult) {
                batchTooltipDecisionByHash.remove(cacheKey, existing);
                log.info("team role cloud tooltip hash group no-result not reused; retry as representative: session={} groupHash={} payloadImageSha256={} windowId={} currentPlayerId={} status={}",
                        context.getLocalTeamSessionKey(), groupHash, request.getImageSha256(),
                        request.getWindowId(), request.getCurrentPlayerId(), decision.getStatus());
                return detectCloudTooltipDecision(context, request, groupHash, false);
            }
            log.info("team role cloud tooltip hash group reused representative: session={} groupHash={} payloadImageSha256={} windowId={} currentPlayerId={} status={} leaderClientId={}",
                    context.getLocalTeamSessionKey(), groupHash, request.getImageSha256(),
                    request.getWindowId(), request.getCurrentPlayerId(),
                    decision.getStatus(), decision.getLeaderClientId());
            return decision;
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private String batchTooltipDecisionCacheKey(TaskExecutionContext context,
                                                String tooltipGroupSignature) {
        if (context == null || !context.hasLocalTeamSession()
                || tooltipGroupSignature == null || tooltipGroupSignature.isBlank()) {
            return null;
        }
        return context.getLocalTeamSessionKey().trim() + "::" + tooltipGroupSignature.trim();
    }

    private void pruneBatchTooltipDecisionCacheIfNeeded() {
        if (batchTooltipDecisionByHash.size() <= BATCH_TOOLTIP_DECISION_CACHE_MAX_ENTRIES) {
            return;
        }
        batchTooltipDecisionByHash.keySet().removeIf(key -> batchTooltipDecisionByHash.size()
                > BATCH_TOOLTIP_DECISION_CACHE_MAX_ENTRIES / 2);
    }

    private TeamRoleTooltipCloudRequest buildCloudTooltipRequest(TaskExecutionContext context,
                                                                 TeamTooltipProbe probe,
                                                                 String currentPlayerId) {
        if (probe == null || probe.rawPath() == null || probe.rawPath().isBlank()) {
            return null;
        }
        byte[] imageBytes;
        BufferedImage rawImage;
        try {
            imageBytes = Files.readAllBytes(Path.of(probe.rawPath()));
            rawImage = ImageIO.read(Path.of(probe.rawPath()).toFile());
        } catch (IOException e) {
            log.warn("team role cloud tooltip payload read failed: raw={} reason={}",
                    probe.rawPath(), e.getMessage(), e);
            return null;
        }
        if (imageBytes.length == 0 || rawImage == null) {
            log.warn("team role cloud tooltip payload empty: raw={}", probe.rawPath());
            return null;
        }
        byte[] maskBytes;
        try {
            maskBytes = encodeTooltipMaskPng(rawImage);
        } catch (IOException e) {
            log.warn("team role cloud tooltip mask payload encode failed: raw={} reason={}",
                    probe.rawPath(), e.getMessage(), e);
            return null;
        } finally {
            rawImage.flush();
        }
        if (maskBytes.length == 0) {
            log.warn("team role cloud tooltip mask payload empty: raw={}", probe.rawPath());
            return null;
        }

        WindowNativeBinding binding = windowTaskContextHolder.rawCurrent()
                .map(WindowRuntimeContext::getNativeBinding)
                .orElse(null);
        int windowWidth = context != null && context.getNativeWindowWidth() > 0
                ? context.getNativeWindowWidth()
                : binding != null ? binding.getWidth() : 0;
        int windowHeight = context != null && context.getNativeWindowHeight() > 0
                ? context.getNativeWindowHeight()
                : binding != null ? binding.getHeight() : 0;
        return TeamRoleTooltipCloudRequest.builder()
                .imagePayloadBase64(Base64.getEncoder().encodeToString(maskBytes))
                .payloadMimeType("image/png")
                .imageSha256(sha256Hex(maskBytes))
                .rawImagePath(probe.rawPath())
                .debugImageId("team-role-tooltip-mask:" + currentPlayerId)
                .roi(TeamRoleTooltipCloudRequest.Roi.builder()
                        .x(teamTaskProperties.getTeamTooltipRectX())
                        .y(teamTaskProperties.getTeamTooltipRectY())
                        .width(teamTaskProperties.getTeamTooltipRectW())
                        .height(teamTaskProperties.getTeamTooltipRectH())
                        .build())
                .windowWidth(windowWidth)
                .windowHeight(windowHeight)
                .currentPlayerId(currentPlayerId)
                .taskCode(context == null ? "startup" : context.getTaskCode())
                .source("team-role-detection")
                .phase("tooltip-id")
                .windowId(context == null ? null : context.getWindowId())
                .taskRunId(context == null ? null : Long.toString(context.getTaskRunId()))
                .policyVersion("team-role-tooltip-cloud-v1")
                .hwnd(binding == null ? context == null ? null : context.getNativeWindowHandle() : binding.getNativeHandle())
                .build();
    }

    private byte[] encodeTooltipMaskPng(BufferedImage rawImage) throws IOException {
        BufferedImage mask = new BufferedImage(rawImage.getWidth(), rawImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        try {
            for (int y = 0; y < rawImage.getHeight(); y++) {
                for (int x = 0; x < rawImage.getWidth(); x++) {
                    mask.setRGB(x, y, isTooltipSignaturePixel(rawImage.getRGB(x, y)) ? 0xFFFFFF : 0x000000);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(mask, "png", output)) {
                return new byte[0];
            }
            return output.toByteArray();
        } finally {
            mask.flush();
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return hex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String tooltipTextSignature(TeamTooltipProbe probe) {
        if (probe == null || !probe.detected() || probe.distribution() == null) {
            return null;
        }
        TextDistribution distribution = probe.distribution();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigestInt(digest, 1);
            updateDigestInt(digest, probe.whitePixels());
            updateDigestInt(digest, probe.purplePixels());
            updateDigestInt(digest, distribution.rows());
            updateDigestInt(digest, distribution.columns());
            updateDigestInt(digest, distribution.transitions());
            updateDigestInt(digest, distribution.maxRowPixels());
            String signature = "tooltip-text-" + hex(digest.digest());
            log.debug("team role tooltip text signature prepared: groupHash={} raw={} white={} purple={} distribution={}",
                    signature, probe.rawPath(), probe.whitePixels(), probe.purplePixels(), distribution);
            return signature;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String tooltipMaskSignature(String rawPath) {
        String normalizedPath = normalize(rawPath);
        if (normalizedPath == null) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(Path.of(normalizedPath).toFile());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigestInt(digest, image.getWidth());
            updateDigestInt(digest, image.getHeight());
            int maskPixels = 0;
            int packed = 0;
            int packedBits = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    boolean mask = isTooltipSignaturePixel(image.getRGB(x, y));
                    if (mask) {
                        maskPixels++;
                    }
                    packed = (packed << 1) | (mask ? 1 : 0);
                    packedBits++;
                    if (packedBits == 8) {
                        digest.update((byte) packed);
                        packed = 0;
                        packedBits = 0;
                    }
                }
            }
            if (packedBits > 0) {
                digest.update((byte) (packed << (8 - packedBits)));
            }
            updateDigestInt(digest, maskPixels);
            String signature = "mask-" + hex(digest.digest());
            log.debug("team role tooltip mask signature prepared: groupHash={} raw={} width={} height={} maskPixels={}",
                    signature, normalizedPath, image.getWidth(), image.getHeight(), maskPixels);
            return signature;
        } catch (IOException | RuntimeException | NoSuchAlgorithmException e) {
            log.warn("team role tooltip mask signature failed; fallback to raw hash: raw={} reason={}",
                    normalizedPath, e.getMessage(), e);
            return null;
        }
    }

    private static boolean isTooltipSignaturePixel(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        boolean whiteText = r >= 155 && g >= 155 && b >= 155;
        boolean greenText = g >= 105 && g - r >= 18 && g - b >= 18;
        boolean purpleText = r >= 95 && b >= 105 && r - g >= 18 && b - g >= 18;
        boolean yellowText = r >= 125 && g >= 95 && b <= 125 && r - b >= 25 && g - b >= 15;
        return whiteText || greenText || purpleText || yellowText;
    }

    private static void updateDigestInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static String hex(byte[] digest) {
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }


    /**
     * Resolve the current player ID from the bound window title.
     *
     * @param context task context supplied by the window runner; may be null.
     * @return player ID parsed from current runtime binding, context title, or tracker title.
     */
    private Optional<String> resolveCurrentPlayerId(TaskExecutionContext context) {
        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isPresent()) {
            WindowRuntimeContext runtime = current.get();
            bindingRefreshService.refreshAndCommit(runtime);
            WindowNativeBinding binding = runtime.getNativeBinding();
            Optional<String> fromBinding = extractPlayerIdFromTitle(binding == null ? null : binding.getTitle());
            if (fromBinding.isPresent()) {
                return fromBinding;
            }
        }
        Optional<String> fromContext = extractPlayerIdFromTitle(context == null ? null : context.getNativeWindowTitle());
        if (fromContext.isPresent()) {
            return fromContext;
        }
        return extractPlayerIdFromTitle(tracker.getFullWindowTitle());
    }

    private RequestMetadata imageProcessorMetadata(String rawPath, String phase, String debugLabel) {
        RequestMetadata.RequestMetadataBuilder builder = RequestMetadata.builder()
                .rawImagePath(rawPath)
                .debugImageId("team-role:" + (debugLabel == null || debugLabel.isBlank() ? phase : debugLabel))
                .source("team-role-detection")
                .taskCode("team-role")
                .phase(phase);
        windowTaskContextHolder.rawCurrent().ifPresent(context -> {
            builder.windowId(context.getWindowId());
            WindowNativeBinding binding = context.getNativeBinding();
            if (binding != null) {
                builder.hwnd(binding.getNativeHandle());
            }
        });
        return builder.build();
    }

    private Optional<String> extractPlayerIdFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = WINDOW_TITLE_ID_PATTERN.matcher(title);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }


    private void saveBufferedImage(BufferedImage image, String path) {
        try {
            Path output = Path.of(path);
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(image, "png", new File(path));
        } catch (IOException e) {
            log.warn("team role tooltip debug image write failed: path={} reason={}", path, e.getMessage(), e);
        }
    }

    /**
     * Detect grouped state from the configured team/member status strip after tooltip probing fails.
     *
     * <p>This fallback captures {@link BotProperties#getReturnTeamAreaX()} /
     * {@link BotProperties#getReturnTeamAreaY()} with scaled window-relative dimensions and computes
     * grayscale standard deviation. It does not focus the window and does not send input. A high
     * deviation only means "the status strip looks populated enough to try Alt+T"; the method never
     * decides LEADER/MEMBER by itself, so the existing panel template checks remain the source of
     * truth for the final role.</p>
     *
     * @return true when the status strip standard deviation is above the grouped threshold; false
     * when capture fails, the configured area is invalid, or the strip looks flat/empty.
     */
    private boolean detectGroupedByTeamStatusDeviationFallback() {
        if (botProperties.getReturnTeamAreaW() <= 0 || botProperties.getReturnTeamAreaH() <= 0) {
            log.warn("team role status deviation fallback skipped: invalid return-team area config w={} h={}",
                    botProperties.getReturnTeamAreaW(), botProperties.getReturnTeamAreaH());
            return false;
        }

        int[] rect = coordinateHelper.getScaledRect(
                botProperties.getReturnTeamAreaX(),
                botProperties.getReturnTeamAreaY(),
                botProperties.getReturnTeamAreaW(),
                botProperties.getReturnTeamAreaH());
        BufferedImage image = tracker.captureToMemory("team-role-status-deviation",
                rect[0], rect[1], rect[2], rect[3]);
        if (image == null) {
            log.warn("team role status deviation fallback capture failed: rect=({}, {})-({}, {})",
                    rect[0], rect[1], rect[2], rect[3]);
            return false;
        }

        try {
            String debugPath = windowScopedTempPath.resolve("team_role_status_deviation_gray.png");
            ImageProcessorResult result = imageProcessorService.measureStddev(
                    image,
                    imageProcessorMetadata(debugPath, "team-role-status-stddev", "status-deviation"));
            if (result.stddev() == null) {
                log.info("team role status deviation fallback unavailable: status={} reason={} rect=({}, {})-({}, {})",
                        result.status(), result.reason(), rect[0], rect[1], rect[2], rect[3]);
                return false;
            }
            double stddev = result.stddev();
            boolean grouped = stddev > TEAM_STATUS_GROUPED_STDDEV_THRESHOLD;
            log.info("team role status deviation fallback: stddev={} threshold={} grouped={} rect=({}, {})-({}, {}) debug={}",
                    String.format("%.3f", stddev),
                    TEAM_STATUS_GROUPED_STDDEV_THRESHOLD,
                    grouped,
                    rect[0], rect[1], rect[2], rect[3],
                    debugPath);
            return grouped;
        } finally {
            image.flush();
        }
    }

    /**
     * Retry the existing Alt+T panel template probe.
     *
     * @param reason short branch label, for example {@code status-deviation}, used only in logs.
     * @return LEADER or MEMBER when any panel attempt matches; UNKNOWN when every configured panel
     *         attempt failed to match both the leader and member markers.
     */
    private TeamRoleStatus detectGroupedRoleFromPanelWithRetries(String reason) {
        int maxAttempts = Math.max(1, teamTaskProperties.getTeamPanelRoleDetectionMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            TeamRoleStatus role = detectGroupedRoleFromPanelOnce(attempt);
            if (!role.isUnknown()) {
                return role;
            }
            log.warn("team role panel detection unknown, will retry if possible: reason={} attempt={}/{}",
                    reason, attempt, maxAttempts);
        }
        return TeamRoleStatus.UNKNOWN;
    }

    /**
     * Open the team panel once and classify leader/member from configured templates.
     *
     * @param attempt one-based retry index used for logging and debug screenshots.
     * @return LEADER, MEMBER, or UNKNOWN. The team panel is closed in the same exclusive callback.
     */
    private TeamRoleStatus detectGroupedRoleFromPanelOnce(int attempt) {
        TeamRoleStatus[] detected = {TeamRoleStatus.UNKNOWN};
        boolean probed = inputSequences.submitExclusiveAndWait("teamRole:panelProbe:attempt" + attempt, () -> {
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            focusCurrentWindowForProbe("panel");
            if (!InputActionScope.checkpoint()) {
                return false;
            }
            inputProvider.pressAltT();
            if (!TaskSleep.sleep(Math.max(0, teamTaskProperties.getTeamPanelOpenDelayMs()))
                    || !InputActionScope.checkpoint()) {
                return false;
            }
            try {
                if (detectTransferLeaderButton()) {
                    detected[0] = TeamRoleStatus.LEADER;
                    log.info("team role detection: grouped role=LEADER attempt={}", attempt);
                    return true;
                }
                if (detectMemberMarker()) {
                    detected[0] = TeamRoleStatus.MEMBER;
                    log.info("team role detection: grouped role=MEMBER attempt={}", attempt);
                    return true;
                }
                log.warn("team role detection: panel opened but no leader/member marker matched: attempt={}", attempt);
                return true;
            } finally {
                closeTeamPanelAfterProbeInsideExclusive(attempt);
            }
        });
        if (!probed) {
            log.warn("team role detection panel probe did not complete: attempt={}", attempt);
            return TeamRoleStatus.UNKNOWN;
        }
        return detected[0];
    }

    private boolean detectTransferLeaderButton() {
        int[] rect = coordinateHelper.getScaledRect(
                teamTaskProperties.getTeamPanelTransferLeaderRectX(),
                teamTaskProperties.getTeamPanelTransferLeaderRectY(),
                teamTaskProperties.getTeamPanelTransferLeaderRectW(),
                teamTaskProperties.getTeamPanelTransferLeaderRectH());
        boolean matched = matchTemplateInRect("team-role-transfer-leader",
                windowScopedTempPath.resolve("team_role_transfer_leader_scan.png"),
                rect,
                teamTaskProperties.getTeamPanelTransferLeaderTemplate(),
                teamTaskProperties.getTeamPanelTransferLeaderMatchRate());
        log.info("team role transfer-leader probe: matched={} template={} rect=({}, {})-({}, {})",
                matched, teamTaskProperties.getTeamPanelTransferLeaderTemplate(),
                rect[0], rect[1], rect[2], rect[3]);
        return matched;
    }

    private boolean detectMemberMarker() {
        int[] rect = coordinateHelper.getScaledRect(
                teamTaskProperties.getTeamPanelMemberMarkerRectX(),
                teamTaskProperties.getTeamPanelMemberMarkerRectY(),
                teamTaskProperties.getTeamPanelMemberMarkerRectW(),
                teamTaskProperties.getTeamPanelMemberMarkerRectH());
        boolean matched = matchTemplateInRect("team-role-member-marker",
                windowScopedTempPath.resolve("team_role_member_marker_scan.png"),
                rect,
                teamTaskProperties.getTeamPanelMemberMarkerTemplate(),
                teamTaskProperties.getTeamPanelMemberMarkerMatchRate());
        log.info("team role member-marker probe: matched={} template={} rect=({}, {})-({}, {})",
                matched, teamTaskProperties.getTeamPanelMemberMarkerTemplate(),
                rect[0], rect[1], rect[2], rect[3]);
        return matched;
    }

    /**
     * Capture a configured window-relative rectangle and match one template inside it.
     *
     * @param captureName logical capture label for logs.
     * @param path window-scoped debug image path to write.
     * @param rect scaled screen-absolute rectangle as {@code [x1,y1,x2,y2]}.
     * @param templatePath template image path.
     * @param matchRate minimum match rate.
     * @return true when the template is found in the captured rectangle.
     */
    private boolean matchTemplateInRect(String captureName, String path, int[] rect, String templatePath, double matchRate) {
        if (!tracker.captureToFile(captureName, path, rect[0], rect[1], rect[2], rect[3])) {
            return false;
        }
        double[] result = ImageFinder.find(path, templatePath, matchRate);
        return result != null && result.length >= 2;
    }

    private void closeTeamPanelAfterProbeInsideExclusive(int attempt) {
        if (!teamTaskProperties.isCloseTeamPanelAfterRoleDetection()) {
            return;
        }
        if (!InputActionScope.checkpoint()) {
            return;
        }
        inputProvider.pressAltT();
        TaskSleep.sleep(Math.max(0, teamTaskProperties.getTeamPanelCloseDelayMs()));
    }

    private void focusCurrentWindowForProbe(String phase) {
        Optional<WindowRuntimeContext> contextOptional = windowTaskContextHolder.rawCurrent();
        if (contextOptional.isEmpty()) {
            return;
        }
        WindowRuntimeContext context = contextOptional.get();
        if (!context.hasNativeBinding()) {
            return;
        }
        boolean focused = windowFocusService.focusWithoutLock(context.getNativeBinding());
        log.debug("team role {} probe focus requested: windowId={} focused={}",
                phase, context.getWindowId(), focused);
    }

    private boolean hasConfiguredHoverProbe() {
        return teamTaskProperties.getTeamTooltipRectW() > 0
                && teamTaskProperties.getTeamTooltipRectH() > 0;
    }

    private boolean hasConfiguredPanelProbe() {
        return teamTaskProperties.getTeamPanelTransferLeaderRectW() > 0
                && teamTaskProperties.getTeamPanelTransferLeaderRectH() > 0
                && teamTaskProperties.getTeamPanelTransferLeaderTemplate() != null
                && !teamTaskProperties.getTeamPanelTransferLeaderTemplate().isBlank()
                && teamTaskProperties.getTeamPanelMemberMarkerRectW() > 0
                && teamTaskProperties.getTeamPanelMemberMarkerRectH() > 0
                && teamTaskProperties.getTeamPanelMemberMarkerTemplate() != null
                && !teamTaskProperties.getTeamPanelMemberMarkerTemplate().isBlank();
    }

    private boolean isTextLikeDistribution(TextDistribution distribution, int width) {
        if (distribution.rows < teamTaskProperties.getTeamTooltipTextMinRows()) {
            return false;
        }
        if (distribution.columns < teamTaskProperties.getTeamTooltipTextMinColumns()) {
            return false;
        }
        if (distribution.transitions < teamTaskProperties.getTeamTooltipTextMinTransitions()) {
            return false;
        }
        int maxAllowedRowPixels = Math.max(1, width * teamTaskProperties.getTeamTooltipTextMaxRowCoveragePercent() / 100);
        return distribution.maxRowPixels <= maxAllowedRowPixels;
    }

    /**
     * Result of one complete role-detection pass.
     *
     * @param role terminal role for the caller when {@code retryWholeFlow} is false.
     * @param tooltipGroupEvidence cloud tooltip hash/leader evidence for local-team session updates.
     * @param retryWholeFlow true only for the tooltip-present path where OCR and panel fallback both
     *                       failed and the whole hover-to-panel sequence should be retried once.
     * @param reason short diagnostic reason logged before retrying or throwing.
     */
    public record TeamRoleDetectionResult(TeamRoleStatus role,
                                          TeamTooltipGroupEvidence tooltipGroupEvidence) {
        private static TeamRoleDetectionResult unknown() {
            return new TeamRoleDetectionResult(TeamRoleStatus.UNKNOWN, null);
        }
    }

    public record TeamTooltipGroupEvidence(String groupHash,
                                           String leaderPlayerId,
                                           String currentPlayerId,
                                           String windowId,
                                           String rawImagePath,
                                           String reason,
                                           String debugToken) {
    }

    private record TooltipRoleResult(TeamRoleStatus role,
                                     TeamTooltipGroupEvidence tooltipGroupEvidence) {
    }

    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class RoleDetectionPassResult {

        TeamRoleStatus role;

        TeamTooltipGroupEvidence tooltipGroupEvidence;

        boolean retryWholeFlow;

        String reason;

        private static RoleDetectionPassResult done(TeamRoleStatus role, String reason) {
            return done(role, null, reason);
        }

        private static RoleDetectionPassResult done(TeamRoleStatus role,
                                                    TeamTooltipGroupEvidence evidence,
                                                    String reason) {
            return new RoleDetectionPassResult(
                    role == null ? TeamRoleStatus.UNKNOWN : role, evidence, false, reason);
        }

        private static RoleDetectionPassResult retry(String reason) {
            return new RoleDetectionPassResult(TeamRoleStatus.UNKNOWN, null, true, reason);
        }
    

    }

    /**
     * Metadata from one saved tooltip screenshot.
     *
     * @param detected whether the crop passed white/purple and text-distribution checks.
     * @param rawPath window-scoped raw tooltip PNG path used by OCR; null if capture failed.
     * @param purplePath window-scoped purple-only debug PNG path; null if capture failed.
     * @param whitePixels number of white tooltip text pixels.
     * @param purplePixels number of purple tooltip text pixels.
     * @param distribution text-like distribution metrics from the raw crop.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class TeamTooltipProbe {

        boolean detected;

        String rawPath;

        String purplePath;

        int whitePixels;

        int purplePixels;

        TextDistribution distribution;

        private static TeamTooltipProbe missed(String rawPath, String purplePath) {
            return new TeamTooltipProbe(false, rawPath, purplePath, 0, 0,
                    new TextDistribution(0, 0, 0, 0));
        }
    

    }

    /**
     * Compact metrics for text-like tooltip pixel distribution.
     *
     * @param rows number of rows containing at least two text-colored pixels.
     * @param columns number of columns containing text-colored pixels.
     * @param transitions horizontal starts of text-colored runs.
     * @param maxRowPixels widest text-colored row, used to reject solid backgrounds.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class TextDistribution {

        int rows;

        int columns;

        int transitions;

        int maxRowPixels;

    }
}
