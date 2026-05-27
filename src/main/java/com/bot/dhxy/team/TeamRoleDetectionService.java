package com.bot.dhxy.team;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;


import com.bot.dhxy.model.ocr.OcrWordResult;
import com.bot.dhxy.runner.stop.TaskSleep;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.TeamTaskProperties;
import com.bot.dhxy.core.GameClientTracker;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.input.InputProvider;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.tools.ImagePreprocessor;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.interaction.WindowFocusService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private static final int MIN_TOOLTIP_ID_DIGITS = 4;
    private static final Pattern WINDOW_TITLE_ID_PATTERN = Pattern.compile("ID\\D{0,4}(\\d+)");
    private static final Pattern DIGIT_SEQUENCE_PATTERN = Pattern.compile("\\d+");

    private final TeamTaskProperties teamTaskProperties;
    private final BotProperties botProperties;
    private final GameClientTracker tracker;
    private final CoordinateHelper coordinateHelper;
    private final TextRecognizer textRecognizer;
    private final InputProvider inputProvider;
    private final InputSequences inputSequences;
    private final WindowTaskContextHolder windowTaskContextHolder;
    private final WindowFocusService windowFocusService;
    private final WindowScopedTempPath windowScopedTempPath;

    /**
     * Create the team-role detector.
     *
     * @param teamTaskProperties role-detection coordinates, thresholds, templates, and switches.
     * @param botProperties shared game-task configuration; this service reads the return-team
     *                      status area as a screen-region fallback after tooltip probing fails.
     * @param tracker screenshot provider for the currently bound window.
     * @param coordinateHelper coordinate scaler/randomizer for configured 1024x768 client points.
     * @param textRecognizer OCR adapter used after tooltip screenshots are saved. It is not called
     *                       while the exclusive input callback is holding the physical input queue.
     * @param inputProvider direct input provider used only inside exclusive input callbacks.
     * @param inputSequences global input queue used to serialize hover and Alt+T probing.
     * @param windowTaskContextHolder current window binding lookup.
     * @param windowFocusService best-effort foreground helper for probe phases that require mouse input.
     * @param windowScopedTempPath per-window debug image path resolver.
     */
    public TeamRoleDetectionService(TeamTaskProperties teamTaskProperties,
                                    BotProperties botProperties,
                                    GameClientTracker tracker,
                                    CoordinateHelper coordinateHelper,
                                    TextRecognizer textRecognizer,
                                    InputProvider inputProvider,
                                    InputSequences inputSequences,
                                    WindowTaskContextHolder windowTaskContextHolder,
                                    WindowFocusService windowFocusService,
                                    WindowScopedTempPath windowScopedTempPath) {
        this.teamTaskProperties = teamTaskProperties;
        this.botProperties = botProperties;
        this.tracker = tracker;
        this.coordinateHelper = coordinateHelper;
        this.textRecognizer = textRecognizer;
        this.inputProvider = inputProvider;
        this.inputSequences = inputSequences;
        this.windowTaskContextHolder = windowTaskContextHolder;
        this.windowFocusService = windowFocusService;
        this.windowScopedTempPath = windowScopedTempPath;
    }

    /**
     * Detect the current role using normal feature switches.
     *
     * @param context current window execution context; may be null for legacy/debug calls.
     * @return SOLO, LEADER, MEMBER, or UNKNOWN. UNKNOWN means detection is disabled, misconfigured,
     * or panel matching failed after retries.
     */
    public TeamRoleStatus detectCurrentRole(TaskExecutionContext context) {
        return detectCurrentRole(context, false);
    }

    /**
     * Detect the current role even when normal role detection is disabled.
     *
     * @param context current window execution context; may be null.
     * @return detected role or UNKNOWN. This path still performs real hover/Alt+T input.
     */
    public TeamRoleStatus detectCurrentRoleForDebug(TaskExecutionContext context) {
        return detectCurrentRole(context, true);
    }

    private TeamRoleStatus detectCurrentRole(TaskExecutionContext context, boolean force) {
        if (!force && !teamTaskProperties.isRoleDetectionEnabled()) {
            if (context != null && context.hasWindow()) {
                log.debug("team role detection disabled: {}", context.getLogPrefix());
            }
            return TeamRoleStatus.UNKNOWN;
        }

        if (!hasConfiguredHoverProbe()) {
            log.warn("team role detection missing hover/tooltip config, return UNKNOWN");
            return TeamRoleStatus.UNKNOWN;
        }

        /*
         * A visible tooltip proves the role is grouped, but if OCR cannot read the leader ID and the
         * panel fallback is also inconclusive, the whole sequence is repeated once from the hover
         * stage. This is stricter than the no-tooltip/stddev branch because a real tooltip means the
         * client is grouped and silently returning SOLO would hide a broken role detector.
         */
        for (int pass = 1; pass <= TOOLTIP_OCR_FULL_RETRY_LIMIT; pass++) {
            RoleDetectionPassResult passResult = detectCurrentRolePass(context, pass);
            if (!passResult.retryWholeFlow()) {
                return passResult.role();
            }
            if (pass < TOOLTIP_OCR_FULL_RETRY_LIMIT) {
                log.warn("team role detection retrying full flow: pass={}/{} reason={}",
                        pass, TOOLTIP_OCR_FULL_RETRY_LIMIT, passResult.reason());
                continue;
            }
            throw new IllegalStateException("Team role detection failed after tooltip OCR and panel fallback: "
                    + passResult.reason());
        }
        return TeamRoleStatus.UNKNOWN;
    }

    /**
     * Run one complete role-detection pass.
     *
     * <p>The pass has three ordered exits: tooltip ID OCR, no-tooltip standard-deviation fallback,
     * and Alt+T panel fallback. Tooltip OCR can request a full retry because the tooltip proves the
     * account is grouped; no-tooltip fallback can safely return SOLO when both tooltip and status
     * strip are flat.</p>
     *
     * @param context current task context, used only to resolve the bound window title and player ID.
     * @param pass one-based outer pass index used in debug image names and logs.
     * @return a terminal role result, or a retry request when tooltip OCR and panel fallback both
     *         failed after a tooltip was positively detected.
     */
    private RoleDetectionPassResult detectCurrentRolePass(TaskExecutionContext context, int pass) {
        Optional<TeamTooltipProbe> tooltip = hoverAndCaptureTeamTooltipWithRetries(pass);
        if (tooltip.isPresent()) {
            Optional<TeamRoleStatus> roleFromTooltipId = detectGroupedRoleFromTooltipLeaderId(context, tooltip.get());
            if (roleFromTooltipId.isPresent()) {
                return RoleDetectionPassResult.done(roleFromTooltipId.get(), "tooltip-id");
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
            focusCurrentWindowForProbe("hover");
            inputProvider.moveMouse(hoverPoint.x, hoverPoint.y);
            TaskSleep.sleep(Math.max(0, teamTaskProperties.getTeamHoverDelayMs()));
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
                int white = countWhitePixels(image);
                int purple = countPurplePixels(image);
                TextDistribution distribution = measureTextDistribution(image);
                boolean detected = white >= teamTaskProperties.getTeamTooltipWhitePixelThreshold()
                        && purple >= teamTaskProperties.getTeamTooltipPurplePixelThreshold()
                        && isTextLikeDistribution(distribution, image.getWidth());
                result[0] = new TeamTooltipProbe(detected, rawPath, purplePath, white, purple, distribution);
                log.info("team role tooltip probe: pass={} attempt={} hover=({}, {}) raw={} white={} purple={} distribution={} thresholds=({}, {}, rows>={}, cols>={}, transitions>={}, maxRowCoverage<={}%)",
                        pass, attempt, hoverPoint.x, hoverPoint.y, rawPath, white, purple, distribution,
                        teamTaskProperties.getTeamTooltipWhitePixelThreshold(),
                        teamTaskProperties.getTeamTooltipPurplePixelThreshold(),
                        teamTaskProperties.getTeamTooltipTextMinRows(),
                        teamTaskProperties.getTeamTooltipTextMinColumns(),
                        teamTaskProperties.getTeamTooltipTextMinTransitions(),
                        teamTaskProperties.getTeamTooltipTextMaxRowCoveragePercent());
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
            ImagePreprocessor.washPurpleTextToBlackAndWhite(probe.rawPath(), probe.purplePath());
        }
        return probe;
    }

    /**
     * Classify leader/member by reading the leader ID from a detected team tooltip.
     *
     * <p>Live testing showed that the team hover tooltip is the leader card. That means the numeric
     * ID in the tooltip is enough to decide the bound window's role: equal to the current window
     * title ID means LEADER, different means MEMBER. Local OCR is used directly and no Baidu
     * fallback is attempted here; OCR failure simply falls back to the older Alt+T panel probe.</p>
     *
     * @param context current task context, used to resolve the bound window title first.
     * @param probe detected tooltip probe containing the saved raw screenshot path.
     * @return LEADER/MEMBER when both IDs are available, otherwise empty so the caller can use the
     *         panel fallback.
     */
    private Optional<TeamRoleStatus> detectGroupedRoleFromTooltipLeaderId(TaskExecutionContext context,
                                                                          TeamTooltipProbe probe) {
        Optional<String> currentPlayerId = resolveCurrentPlayerId(context);
        if (currentPlayerId.isEmpty()) {
            log.warn("team role tooltip ID skipped: current player ID unavailable");
            return Optional.empty();
        }

        Optional<String> leaderId = extractLeaderIdFromTooltipOcr(probe);
        if (leaderId.isEmpty()) {
            log.warn("team role tooltip ID OCR missed: currentPlayerId={} raw={}",
                    currentPlayerId.get(), probe.rawPath());
            return Optional.empty();
        }

        TeamRoleStatus role = leaderId.get().equals(currentPlayerId.get())
                ? TeamRoleStatus.LEADER
                : TeamRoleStatus.MEMBER;
        log.info("team role detection: tooltip leader id={} currentPlayerId={} role={} raw={}",
                leaderId.get(), currentPlayerId.get(), role, probe.rawPath());
        return Optional.of(role);
    }

    /**
     * Read the numeric leader ID from a saved tooltip image.
     *
     * <p>The full tooltip crop is OCR'd because the runtime cost is dominated by RapidOCR startup
     * and inference overhead rather than the small difference between the full tooltip and a narrow
     * ID strip. The selector ignores level text such as "195级" and prefers 4+ digit sequences below
     * the level row, which matches the observed tooltip layout.</p>
     *
     * @param probe tooltip probe with a saved raw image path.
     * @return selected leader ID, or empty when local OCR is unavailable or no reliable ID candidate
     *         is found.
     */
    private Optional<String> extractLeaderIdFromTooltipOcr(TeamTooltipProbe probe) {
        if (probe == null || probe.rawPath() == null || probe.rawPath().isBlank()) {
            return Optional.empty();
        }

        List<OcrWordResult> words = textRecognizer.getAllTextResultsLocalOnly(probe.rawPath());
        if (words.isEmpty()) {
            log.warn("team role tooltip OCR returned no words: raw={}", probe.rawPath());
            return Optional.empty();
        }

        OptionalIntBox levelTop = findLevelRowTop(words);
        List<TooltipIdCandidate> candidates = new ArrayList<>();
        for (OcrWordResult word : words) {
            String text = word.getText() == null ? "" : word.getText().trim();
            if (text.isBlank() || text.contains("级")) {
                continue;
            }
            Matcher matcher = DIGIT_SEQUENCE_PATTERN.matcher(text);
            while (matcher.find()) {
                String id = matcher.group();
                if (id.length() >= MIN_TOOLTIP_ID_DIGITS) {
                    candidates.add(new TooltipIdCandidate(id, word.getTop(), word.getLeft(), text));
                }
            }
        }

        Comparator<TooltipIdCandidate> comparator = Comparator
                .comparing((TooltipIdCandidate candidate) -> isBelowLevelRow(candidate, levelTop)).reversed()
                .thenComparing(Comparator.comparingInt((TooltipIdCandidate candidate) -> candidate.id().length()).reversed())
                .thenComparingInt(TooltipIdCandidate::top)
                .thenComparingInt(TooltipIdCandidate::left);
        Optional<TooltipIdCandidate> selected = candidates.stream().sorted(comparator).findFirst();
        log.info("team role tooltip OCR words: raw={} selectedId={} levelTop={} candidates={} words={}",
                probe.rawPath(),
                selected.map(TooltipIdCandidate::id).orElse("-"),
                levelTop.present() ? Integer.toString(levelTop.value()) : "-",
                summarizeIdCandidates(candidates),
                summarizeOcrWords(words));
        return selected.map(TooltipIdCandidate::id);
    }

    /**
     * Resolve the current player ID from the bound window title.
     *
     * @param context task context supplied by the window runner; may be null.
     * @return player ID parsed from context title, current runtime binding, or tracker title.
     */
    private Optional<String> resolveCurrentPlayerId(TaskExecutionContext context) {
        Optional<String> fromContext = extractPlayerIdFromTitle(context == null ? null : context.getNativeWindowTitle());
        if (fromContext.isPresent()) {
            return fromContext;
        }

        Optional<WindowRuntimeContext> current = windowTaskContextHolder.rawCurrent();
        if (current.isPresent()) {
            WindowNativeBinding binding = current.get().getNativeBinding();
            Optional<String> fromBinding = extractPlayerIdFromTitle(binding == null ? null : binding.getTitle());
            if (fromBinding.isPresent()) {
                return fromBinding;
            }
        }
        return extractPlayerIdFromTitle(tracker.getFullWindowTitle());
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

    private OptionalIntBox findLevelRowTop(List<OcrWordResult> words) {
        return words.stream()
                .filter(word -> word.getText() != null && word.getText().contains("级"))
                .mapToInt(OcrWordResult::getTop)
                .min()
                .stream()
                .mapToObj(OptionalIntBox::present)
                .findFirst()
                .orElseGet(OptionalIntBox::empty);
    }

    private boolean isBelowLevelRow(TooltipIdCandidate candidate, OptionalIntBox levelTop) {
        return !levelTop.present() || candidate.top() > levelTop.value();
    }

    private String summarizeOcrWords(List<OcrWordResult> words) {
        if (words == null || words.isEmpty()) {
            return "[]";
        }
        int limit = Math.min(8, words.size());
        List<String> summary = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            OcrWordResult word = words.get(i);
            summary.add("'" + word.getText() + "'@(" + word.getLeft() + "," + word.getTop()
                    + "," + word.getWidth() + "x" + word.getHeight() + ")");
        }
        if (words.size() > limit) {
            summary.add("...+" + (words.size() - limit));
        }
        return summary.toString();
    }

    private String summarizeIdCandidates(List<TooltipIdCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "[]";
        }
        return candidates.stream()
                .limit(6)
                .map(candidate -> candidate.id() + "@(" + candidate.left() + "," + candidate.top()
                        + ") from='" + candidate.sourceText() + "'")
                .toList()
                .toString();
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
            double stddev = ImagePreprocessor.getImageStandardDeviation(image, debugPath);
            boolean grouped = stddev > TEAM_STATUS_GROUPED_STDDEV_THRESHOLD;
            log.info("team role status deviation fallback: stddev={} threshold={} grouped={} rect=({}, {})-({}, {}) gray={}",
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
            focusCurrentWindowForProbe("panel");
            inputProvider.pressAltT();
            TaskSleep.sleep(Math.max(0, teamTaskProperties.getTeamPanelOpenDelayMs()));
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

    private int countWhitePixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (isTooltipWhite(rgb)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countPurplePixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if (isTooltipPurple(rgb)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Measure whether white/purple tooltip pixels look like text rather than a broad background patch.
     *
     * @param image tooltip crop captured after hover.
     * @return row/column/transition metrics used by {@link #isTextLikeDistribution(TextDistribution, int)}.
     */
    private TextDistribution measureTextDistribution(BufferedImage image) {
        int rows = 0;
        int[] columnCounts = new int[image.getWidth()];
        int transitions = 0;
        int maxRowPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            int rowPixels = 0;
            boolean previousTextPixel = false;
            for (int x = 0; x < image.getWidth(); x++) {
                boolean textPixel = isTooltipTextPixel(image.getRGB(x, y));
                if (textPixel) {
                    rowPixels++;
                    columnCounts[x]++;
                }
                if (textPixel && !previousTextPixel) {
                    transitions++;
                }
                previousTextPixel = textPixel;
            }
            if (rowPixels >= 2) {
                rows++;
            }
            maxRowPixels = Math.max(maxRowPixels, rowPixels);
        }

        int columns = 0;
        for (int columnCount : columnCounts) {
            if (columnCount >= 1) {
                columns++;
            }
        }
        return new TextDistribution(rows, columns, transitions, maxRowPixels);
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

    private boolean isTooltipTextPixel(int rgb) {
        return isTooltipWhite(rgb) || isTooltipPurple(rgb);
    }

    private boolean isTooltipWhite(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r >= 210 && g >= 210 && b >= 210;
    }

    private boolean isTooltipPurple(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r >= 120 && b >= 140 && g <= 120 && b > g + 40;
    }

    /**
     * Result of one complete role-detection pass.
     *
     * @param role terminal role for the caller when {@code retryWholeFlow} is false.
     * @param retryWholeFlow true only for the tooltip-present path where OCR and panel fallback both
     *                       failed and the whole hover-to-panel sequence should be retried once.
     * @param reason short diagnostic reason logged before retrying or throwing.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class RoleDetectionPassResult {

        TeamRoleStatus role;

        boolean retryWholeFlow;

        String reason;

        private static RoleDetectionPassResult done(TeamRoleStatus role, String reason) {
            return new RoleDetectionPassResult(role == null ? TeamRoleStatus.UNKNOWN : role, false, reason);
        }

        private static RoleDetectionPassResult retry(String reason) {
            return new RoleDetectionPassResult(TeamRoleStatus.UNKNOWN, true, reason);
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
     * Candidate numeric ID extracted from a tooltip OCR word.
     *
     * @param id normalized digit sequence.
     * @param top image-local top pixel of the source OCR word.
     * @param left image-local left pixel of the source OCR word.
     * @param sourceText full OCR word text that contained the digit sequence.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class TooltipIdCandidate {

        String id;

        int top;

        int left;

        String sourceText;

    }

    /**
     * Tiny optional-int value object used to avoid nullable boxed integers in comparator code.
     *
     * @param present whether a value exists.
     * @param value integer value when present.
     */
    @Value

    @Builder

    @AllArgsConstructor(access = AccessLevel.PUBLIC)

    @Accessors(fluent = true)

    private static class OptionalIntBox {

        boolean present;

        int value;

        private static OptionalIntBox present(int value) {
            return new OptionalIntBox(true, value);
        }

        private static OptionalIntBox empty() {
            return new OptionalIntBox(false, 0);
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
