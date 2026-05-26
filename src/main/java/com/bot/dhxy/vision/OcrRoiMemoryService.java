package com.bot.dhxy.vision;

import com.bot.dhxy.core.TextRecognizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent vision-memory store for OCR ROI, player anchors, and NPC click observations.
 *
 * <p>The file format is append-style diagnostic evidence plus a small amount of derived policy
 * such as {@code recommendedRoi}. Business code must treat recommendations as hints only: every
 * learned ROI/direct NPC point still needs a full fallback path and positive verification before it
 * changes task behavior. The JSON file is written through a temp file and atomic replace when the
 * filesystem supports it.</p>
 */
@Slf4j
@Service
public class OcrRoiMemoryService {

    private static final Path MEMORY_PATH = Path.of("config", "vision_memory.json");
    private static final Path LEGACY_MEMORY_PATH = Path.of("config", "ocr_roi_memory.json");
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 768;
    private static final int MAX_SAMPLES_PER_KEY = 50;
    private static final int MAX_GLOBAL_SAMPLES = 600;
    private static final int MAX_OCR_ATTEMPTS = 1000;
    private static final int MAX_NPC_CLICK_SAMPLES = 600;
    private static final int CAMERA_DELTA_THRESHOLD = 40;
    private static final int MIN_LEARNED_NPC_SUCCESS_SAMPLES = 3;
    private static final int MAX_LEARNED_NPC_RECENT_SAMPLES = 12;
    private static final int MAX_LEARNED_NPC_POINT_SPREAD_PX = 45;
    private static final int NPC_COORD_BUCKET_SIZE = 10;
    private static final int NPC_COORD_NEIGHBOR_RADIUS = 1;
    private static final int MAX_TARGET_CANDIDATE_SAMPLES = 1000;
    private static final int ROI_POLICY_FAILURE_STALE_THRESHOLD = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Read the current learned OCR region for a memory key.
     *
     * @param key stable OCR memory key; null/blank disables lookup.
     * @return valid 1024x768 window-relative ROI, or empty when no learned region exists.
     */
    public synchronized Optional<OcrWindowRegion> recommendedRoi(String key) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            return Optional.empty();
        }
        MemoryFile memory = loadMemory();
        RoiPolicy policy = memory.policies == null ? null : memory.policies.roiPolicies.get(normalizedKey);
        if (isUsablePolicy(policy)) {
            OcrWindowRegion policyRegion = policy.recommendedRoi.toRegion().clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
            if (policyRegion.isValid()) {
                return Optional.of(policyRegion);
            }
        }
        MemoryEntry entry = memory.entries.get(normalizedKey);
        if (entry == null || entry.recommendedRoi == null) {
            return Optional.empty();
        }
        OcrWindowRegion region = entry.recommendedRoi.toRegion().clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
        return region.isValid() ? Optional.of(region) : Optional.empty();
    }

    /**
     * Recommend OCR scan regions for an NPC/monster click request from learned vision memory.
     *
     * <p>The policy deliberately handles fixed NPCs and roaming combat targets differently. Fixed
     * NPCs can learn by map/name/exact coordinate. Combat targets learn by map plus coordinate
     * bucket so nearby 修罗/怪物 targets can reuse a scan window without requiring the OCR-visible
     * monster name to be stable. Returned regions are ordered hints in 1024x768 window-relative
     * pixels; callers must still verify the expected dialog/battle signal after clicking.</p>
     *
     * @param mapName target map name if known; nullable.
     * @param mapX target logical in-game X coordinate if known; nullable.
     * @param mapY target logical in-game Y coordinate if known; nullable.
     * @param targetName NPC name or task keyword; nullable for roaming combat targets.
     * @param roamingTarget true when the target coordinate came from a refreshed task objective.
     * @return ordered, de-duplicated OCR regions in window-relative pixels. Learned/sample-derived
     * regions come first, followed by the default full-window masked fallback source from
     * {@link OcrWindowScanService}.
     */
    public synchronized List<OcrWindowRegion> recommendNpcClickRegions(String mapName,
                                                                       Integer mapX,
                                                                       Integer mapY,
                                                                       String targetName,
                                                                       boolean roamingTarget) {
        /*
         * Normalize the target identity first. Fixed NPCs require a concrete name because their ROI
         * key is map + NPC name + exact coordinate. Roaming Xiuluo-style targets may use "any-name"
         * because the task can know the coordinate while the OCR-visible monster name is unstable.
         */
        String target = normalizeTargetName(targetName, roamingTarget);
        if (target == null) {
            return List.of();
        }

        /*
         * Load the complete vision memory once. The recommendation is assembled from derived policy
         * first, then weaker raw observations. Keeping this order means a stable policy wins, but
         * old/raw samples can still help while a policy is not mature yet.
         */
        MemoryFile memory = loadMemory();
        List<OcrWindowRegion> regions = new ArrayList<>();

        /*
         * Primary source: v2 ROI policies created from verified yellow-name OCR observations.
         * Fixed NPCs use an exact coordinate key; roaming targets try the current coordinate bucket
         * and nearby buckets so a nearby real sample can seed the first crop.
         */
        for (String key : npcTargetRoiPolicyKeys(mapName, mapX, mapY, target, roamingTarget)) {
            addPolicyRegion(regions, memory, key);
        }

        /*
         * Secondary source: verified NPC click samples. They do not prove the yellow text rectangle,
         * but a verified click point is close enough to propose a broad crop around that target.
         */
        addNpcClickSampleRegion(regions, memory, mapName, mapX, mapY, target, roamingTarget);

        /*
         * Compatibility source: older entries that only stored MemoryEntry.recommendedRoi. This is
         * read-only compatibility for existing JSON data, not a place to introduce new hardcoded
         * regions.
         */
        for (String key : npcClickRegionMemoryKeys(mapName, mapX, mapY, target)) {
            addLegacyRoiRegion(regions, memory, key);
        }

        /*
         * Final fallback: capture the whole game client and let the scan path apply the same masks
         * as OcrWindowScanService's full-window OCR. The region marks the source image; it is not a
         * request to scan raw UI noise.
         */
        addUniqueRegion(regions, OcrWindowScanService.defaultMaskedWindowRegion());

        log.info("[ocr-roi-memory] npc click regions target={} map={} coord=({}, {}) roaming={} regions={}",
                target, safe(mapName), mapX, mapY, roamingTarget, summarizeRegions(regions));
        return List.copyOf(regions);
    }

    /**
     * Record a yellow-name OCR observation from NPC/monster smart-click flow.
     *
     * <p>This is the bridge between click verification and OCR-region learning. Only observations
     * that both match a text candidate and verify the expected dialog/battle path update the derived
     * ROI policy. Misses and unverified candidates are still stored as raw evidence and may mark the
     * current policy stale after repeated failures, but they do not widen the recommendation by
     * themselves.</p>
     *
     * @param source diagnostic source such as {@code NPC_YELLOW_TARGET}.
     * @param mapName target map name; nullable.
     * @param targetMapX logical target X coordinate; nullable.
     * @param targetMapY logical target Y coordinate; nullable.
     * @param targetName expected NPC name or task keyword; nullable for roaming combat targets.
     * @param roamingTarget true for combat/roaming targets that should learn by coordinate bucket.
     * @param scanRegion window-relative OCR region scanned.
     * @param textRect matched text rectangle in window-relative pixels; nullable on miss.
     * @param clickPoint window-relative click/probe point; nullable when no concrete candidate was clicked.
     * @param matched true when yellow OCR matched the expected target text.
     * @param verified true when the subsequent click opened the expected dialog/battle path.
     * @param observedText normalized or raw OCR text observed for diagnostics.
     * @param message short outcome detail for logs and JSON inspection.
     * @return record result with the primary policy key, or skipped when the target lacks enough identity.
     */
    public synchronized RecordResult recordNpcTargetOcrObservation(String source,
                                                                   String mapName,
                                                                   Integer targetMapX,
                                                                   Integer targetMapY,
                                                                   String targetName,
                                                                   boolean roamingTarget,
                                                                   OcrWindowRegion scanRegion,
                                                                   OcrWindowRegion textRect,
                                                                   Point clickPoint,
                                                                   boolean matched,
                                                                   boolean verified,
                                                                   String observedText,
                                                                   String message) {
        String target = normalizeTargetName(targetName, roamingTarget);
        if (target == null) {
            return RecordResult.skipped(null, "missing NPC target OCR identity");
        }

        MemoryFile memory = loadMemory();
        memory.memoryType = "vision-memory-v2";
        memory.updatedAt = LocalDateTime.now().toString();

        List<String> keys = npcTargetRoiPolicyKeys(mapName, targetMapX, targetMapY, target, roamingTarget);
        String primaryKey = keys.isEmpty() ? null : keys.get(0);
        TargetCandidateSample sample = TargetCandidateSample.from(
                primaryKey,
                source,
                mapName,
                targetMapX,
                targetMapY,
                target,
                roamingTarget,
                scanRegion,
                textRect,
                clickPoint,
                matched,
                verified,
                observedText,
                message);
        memory.targetCandidateSamples.add(sample);
        trimList(memory.targetCandidateSamples, MAX_TARGET_CANDIDATE_SAMPLES);

        for (String key : keys) {
            updateRoiPolicy(memory, key, sample, matched, verified);
        }
        saveMemory(memory);

        String summary = "key=" + safe(primaryKey)
                + " source=" + safe(source)
                + " target=" + safe(target)
                + " map=" + safe(mapName)
                + " coord=" + nullablePoint(targetMapX, targetMapY)
                + " roaming=" + roamingTarget
                + " matched=" + matched
                + " verified=" + verified
                + " textRect=" + (textRect == null ? "-" : textRect.toShortText())
                + " clickPoint=" + pointText(PointData.from(clickPoint));
        log.info("[vision-memory] NPC target OCR observation recorded: {}", summary);
        return new RecordResult(true, primaryKey, summary, "-");
    }

    /**
     * Record a simple OCR/click success and recompute the rolling recommended ROI.
     *
     * @param key stable OCR memory key.
     * @param textRect matched OCR text rectangle in window-relative pixels; nullable.
     * @param clickPoint click point in window-relative pixels; nullable.
     */
    public synchronized void recordSuccess(String key, OcrWindowRegion textRect, Point clickPoint) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null || (textRect == null && clickPoint == null)) {
            return;
        }

        // Persist the raw sample first; ROI is always derived from recent raw samples.
        MemoryFile memory = loadMemory();
        memory.memoryType = "vision-memory-v2";
        memory.updatedAt = LocalDateTime.now().toString();
        MemoryEntry entry = memory.entries.computeIfAbsent(normalizedKey, ignored -> new MemoryEntry());
        entry.attemptCount++;
        entry.successCount++;
        entry.lastAttemptAt = memory.updatedAt;
        entry.lastSuccessAt = memory.updatedAt;
        entry.samples.add(MemorySample.from(textRect, clickPoint));
        while (entry.samples.size() > MAX_SAMPLES_PER_KEY) {
            entry.samples.remove(0);
        }
        // Recompute a conservative ROI after each success so future scans can try a smaller region.
        OcrWindowRegion recommended = computeRecommendedRoi(entry);
        entry.recommendedRoi = recommended == null ? null : RegionData.from(recommended);
        saveMemory(memory);

        log.info("[ocr-roi-memory] success key={} successCount={} recommendedRoi={}",
                normalizedKey, entry.successCount, recommended == null ? "-" : recommended.toShortText());
    }

    public synchronized RecordResult recordPlayerAnchorSuccess(String key,
                                                               String source,
                                                               LocationVisionService.PlayerAnchorMatch match,
                                                               TextRecognizer.LocationInfo location,
                                                               int windowWidth,
                                                               int windowHeight,
                                                               String provider,
                                                               String preprocessVariant,
                                                               String imagePath,
                                                               String secondaryImagePath,
                                                               String locationSource) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null || match == null || match.anchor() == null) {
            return RecordResult.skipped(normalizedKey, "missing key or player anchor match");
        }

        // Player-anchor samples are used both as raw diagnostics and as ROI evidence.
        MemoryFile memory = loadMemory();
        memory.memoryType = "vision-memory-v2";
        memory.updatedAt = LocalDateTime.now().toString();

        MemoryEntry entry = memory.entries.computeIfAbsent(normalizedKey, ignored -> new MemoryEntry());
        entry.attemptCount++;
        entry.successCount++;
        entry.lastAttemptAt = memory.updatedAt;
        entry.lastSuccessAt = memory.updatedAt;

        MemorySample sample = MemorySample.fromPlayerAnchor(
                normalizedKey,
                source,
                match,
                location,
                windowWidth,
                windowHeight,
                provider,
                preprocessVariant,
                imagePath,
                secondaryImagePath,
                locationSource
        );
        entry.samples.add(sample);
        while (entry.samples.size() > MAX_SAMPLES_PER_KEY) {
            entry.samples.remove(0);
        }
        OcrWindowRegion recommended = computeRecommendedRoi(entry);
        entry.recommendedRoi = recommended == null ? null : RegionData.from(recommended);
        entry.lastCameraState = sample.cameraState;
        entry.lastMapName = sample.mapName;
        entry.lastMapX = sample.mapX;
        entry.lastMapY = sample.mapY;
        entry.lastAnchor = sample.anchor;
        entry.lastAnchorDelta = sample.anchorDelta;

        memory.playerAnchorSamples.add(sample);
        while (memory.playerAnchorSamples.size() > MAX_GLOBAL_SAMPLES) {
            memory.playerAnchorSamples.remove(0);
        }
        saveMemory(memory);

        String summary = "key=" + normalizedKey
                + " source=" + safe(source)
                + " map=" + safe(sample.mapName)
                + " coord=" + nullablePoint(sample.mapX, sample.mapY)
                + " anchor=" + pointText(sample.anchor)
                + " delta=" + pointText(sample.anchorDelta)
                + " cameraState=" + safe(sample.cameraState)
                + " textRect=" + (sample.textRect == null ? "-" : sample.textRect.toRegion().toShortText())
                + " recommendedRoi=" + (recommended == null ? "-" : recommended.toShortText());
        log.info("[vision-memory] player anchor recorded: {}", summary);
        return new RecordResult(true, normalizedKey, summary,
                recommended == null ? "-" : recommended.toShortText());
    }

    public synchronized RecordResult recordOcrAttempt(String key,
                                                      String purpose,
                                                      String regionType,
                                                      OcrWindowRegion scanRegion,
                                                      String targetText,
                                                      List<TextRecognizer.OcrWordResult> words,
                                                      boolean matched,
                                                      String message,
                                                      int windowWidth,
                                                      int windowHeight,
                                                      String provider,
                                                      String preprocessVariant,
                                                      String rawPath,
                                                      String maskedPath,
                                                      String overlayPath,
                                                      String roiPath) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            return RecordResult.skipped(null, "missing OCR memory key");
        }

        // Store both the full OCR attempt and, when matched, the concrete word rectangle that can
        // tighten the next ROI recommendation.
        TextRecognizer.OcrWordResult matchedWord = matched
                ? findMatchedWord(words, targetText)
                : null;
        OcrWindowRegion textRect = wordToRegion(matchedWord);
        Point clickPoint = wordToPoint(matchedWord);

        MemoryFile memory = loadMemory();
        memory.memoryType = "vision-memory-v2";
        memory.updatedAt = LocalDateTime.now().toString();

        MemoryEntry entry = memory.entries.computeIfAbsent(normalizedKey, ignored -> new MemoryEntry());
        entry.attemptCount++;
        entry.lastAttemptAt = memory.updatedAt;
        if (matched) {
            entry.successCount++;
            entry.lastSuccessAt = memory.updatedAt;
            if (textRect != null || clickPoint != null) {
                MemorySample sample = MemorySample.from(textRect, clickPoint);
                sample.key = normalizedKey;
                sample.source = safe(regionType);
                sample.matchedText = matchedWord == null ? null : matchedWord.getText();
                sample.score = matchedWord == null ? 0.0 : matchedWord.getScore();
                sample.windowWidth = windowWidth > 0 ? windowWidth : IMAGE_WIDTH;
                sample.windowHeight = windowHeight > 0 ? windowHeight : IMAGE_HEIGHT;
                entry.samples.add(sample);
                while (entry.samples.size() > MAX_SAMPLES_PER_KEY) {
                    entry.samples.remove(0);
                }
            }
        } else {
            entry.failureCount++;
        }

        OcrWindowRegion recommended = computeRecommendedRoi(entry);
        entry.recommendedRoi = recommended == null ? null : RegionData.from(recommended);
        TargetCandidateSample genericPolicySample = TargetCandidateSample.from(
                normalizedKey,
                regionType,
                null,
                null,
                null,
                targetText,
                false,
                scanRegion,
                textRect,
                clickPoint,
                matched,
                matched,
                matchedWord == null ? null : matchedWord.getText(),
                message);
        updateRoiPolicy(memory, normalizedKey, genericPolicySample, matched, matched);

        // Keep the full attempt stream separate from per-key samples so false positives/negatives
        // can be inspected later without trusting the derived policy.
        OcrAttemptSample attempt = OcrAttemptSample.from(
                normalizedKey,
                purpose,
                regionType,
                scanRegion,
                targetText,
                words,
                matched,
                matchedWord,
                message,
                windowWidth,
                windowHeight,
                provider,
                preprocessVariant,
                rawPath,
                maskedPath,
                overlayPath,
                roiPath
        );
        memory.ocrAttempts.add(attempt);
        trimList(memory.ocrAttempts, MAX_OCR_ATTEMPTS);
        saveMemory(memory);

        String summary = "key=" + normalizedKey
                + " purpose=" + safe(purpose)
                + " regionType=" + safe(regionType)
                + " provider=" + safe(provider)
                + " preprocess=" + safe(preprocessVariant)
                + " matched=" + matched
                + " words=" + (words == null ? 0 : words.size())
                + " scanRegion=" + (scanRegion == null ? "-" : scanRegion.toShortText())
                + " textRect=" + (textRect == null ? "-" : textRect.toShortText())
                + " recommendedRoi=" + (recommended == null ? "-" : recommended.toShortText())
                + " message=" + safe(message);
        log.info("[vision-memory] OCR attempt recorded: {}", summary);
        return new RecordResult(true, normalizedKey, summary,
                recommended == null ? "-" : recommended.toShortText());
    }

    public synchronized RecordResult recordNpcClickAttempt(String source,
                                                           String mapName,
                                                           Integer playerMapX,
                                                           Integer playerMapY,
                                                           String npcName,
                                                           Integer targetMapX,
                                                           Integer targetMapY,
                                                           Point windowBase,
                                                           Point playerAnchorAbs,
                                                           Point predictedClickAbs,
                                                           Point actualClickAbs,
                                                           Integer tuneX,
                                                           Integer tuneY,
                                                           String formulaVersion,
                                                           boolean clicked,
                                                           boolean success,
                                                           String outcome,
                                                           String verification,
                                                           boolean actualClickMeasured,
                                                           String actualClickSource,
                                                           String verificationStrength) {
        if (!hasAnyTargetIdentity(mapName, npcName, targetMapX, targetMapY)) {
            return RecordResult.skipped(null, "missing NPC click identity");
        }

        String key = buildNpcClickKey(mapName, npcName, targetMapX, targetMapY);
        MemoryFile memory = loadMemory();
        memory.memoryType = "vision-memory-v2";
        memory.updatedAt = LocalDateTime.now().toString();

        NpcClickSample sample = new NpcClickSample();
        sample.createdAt = memory.updatedAt;
        sample.key = key;
        sample.source = safe(source);
        sample.mapName = mapName;
        sample.playerMapX = playerMapX;
        sample.playerMapY = playerMapY;
        sample.npcName = npcName;
        sample.targetMapX = targetMapX;
        sample.targetMapY = targetMapY;
        sample.deltaMapX = playerMapX == null || targetMapX == null ? null : targetMapX - playerMapX;
        sample.deltaMapY = playerMapY == null || targetMapY == null ? null : targetMapY - playerMapY;
        sample.windowBase = PointData.from(windowBase);
        sample.playerAnchorAbs = PointData.from(playerAnchorAbs);
        sample.playerAnchorRel = relativeToBase(playerAnchorAbs, windowBase);
        sample.predictedClickAbs = PointData.from(predictedClickAbs);
        sample.predictedClickRel = relativeToBase(predictedClickAbs, windowBase);
        sample.actualClickAbs = PointData.from(actualClickAbs);
        sample.actualClickRel = relativeToBase(actualClickAbs, windowBase);
        sample.tuneX = tuneX;
        sample.tuneY = tuneY;
        sample.formulaVersion = formulaVersion;
        sample.clicked = clicked;
        sample.success = success;
        sample.outcome = outcome;
        sample.verification = verification;
        sample.actualClickMeasured = actualClickMeasured;
        sample.actualClickSource = actualClickSource;
        sample.verificationStrength = verificationStrength;

        memory.npcClickSamples.add(sample);
        trimList(memory.npcClickSamples, MAX_NPC_CLICK_SAMPLES);

        MemoryEntry entry = memory.entries.computeIfAbsent(key, ignored -> new MemoryEntry());
        entry.attemptCount++;
        entry.lastAttemptAt = memory.updatedAt;
        entry.lastNpcClickAt = memory.updatedAt;
        entry.lastNpcClickOutcome = outcome;
        entry.lastPredictedClick = sample.predictedClickRel;
        if (success) {
            entry.successCount++;
            entry.lastSuccessAt = memory.updatedAt;
        } else {
            entry.failureCount++;
        }
        saveMemory(memory);

        String summary = "key=" + key
                + " source=" + safe(source)
                + " map=" + safe(mapName)
                + " player=" + nullablePoint(playerMapX, playerMapY)
                + " targetNpc=" + safe(npcName)
                + " target=" + nullablePoint(targetMapX, targetMapY)
                + " predictedRel=" + pointText(sample.predictedClickRel)
                + " clicked=" + clicked
                + " success=" + success
                + " outcome=" + safe(outcome)
                + " actualClickMeasured=" + actualClickMeasured
                + " verificationStrength=" + safe(verificationStrength);
        log.info("[vision-memory] NPC click attempt recorded: {}", summary);
        return new RecordResult(true, key, summary, "-");
    }

    /**
     * Recommend a direct window-relative click point for a known NPC target from prior verified runs.
     *
     * <p>This is intentionally conservative. Normal task samples are predictions plus dialog
     * verification, not independently measured NPC centers, so the recommendation is returned only
     * when the most recent sample for the same map/NPC/target succeeded, at least three recent
     * successful samples agree, and the successful points are tightly clustered. Callers must still
     * verify the expected dialog after clicking and fall back to OCR/formula strategies on failure.</p>
     *
     * @param mapName logical map name for the NPC/monster target; may be blank but stronger keys are
     *                safer.
     * @param npcName NPC or monster name used in the click strategy.
     * @param targetMapX logical in-game target X coordinate.
     * @param targetMapY logical in-game target Y coordinate.
     * @return learned click point in 1024x768 window-relative pixels, or empty when memory is too
     * weak, unstable, or the latest attempt failed.
     */
    public synchronized Optional<LearnedNpcClickPoint> recommendedNpcClickPoint(String mapName,
                                                                                String npcName,
                                                                                Integer targetMapX,
                                                                                Integer targetMapY) {
        if (!hasAnyTargetIdentity(mapName, npcName, targetMapX, targetMapY)) {
            return Optional.empty();
        }

        String key = buildNpcClickKey(mapName, npcName, targetMapX, targetMapY);
        MemoryFile memory = loadMemory();
        List<NpcClickSample> sameTarget = memory.npcClickSamples.stream()
                .filter(sample -> sample != null && key.equals(sample.key))
                .toList();
        if (sameTarget.isEmpty()) {
            return Optional.empty();
        }

        NpcClickSample latest = sameTarget.get(sameTarget.size() - 1);
        if (!latest.clicked || !latest.success) {
            log.info("[vision-memory] learned NPC point skipped: key={} reason=latest-sample-failed", key);
            return Optional.empty();
        }

        List<NpcClickSample> recent = sameTarget.subList(
                Math.max(0, sameTarget.size() - MAX_LEARNED_NPC_RECENT_SAMPLES),
                sameTarget.size());
        List<Point> successfulPoints = recent.stream()
                .filter(sample -> sample.clicked && sample.success && hasStrongNpcVerification(sample))
                .map(this::npcSampleClickPoint)
                .filter(point -> point != null)
                .toList();
        if (successfulPoints.size() < MIN_LEARNED_NPC_SUCCESS_SAMPLES) {
            log.info("[vision-memory] learned NPC point skipped: key={} reason=insufficient-success count={}",
                    key, successfulPoints.size());
            return Optional.empty();
        }

        int averageX = (int) Math.round(successfulPoints.stream().mapToInt(point -> point.x).average().orElse(0));
        int averageY = (int) Math.round(successfulPoints.stream().mapToInt(point -> point.y).average().orElse(0));
        Point average = new Point(averageX, averageY);
        int spread = successfulPoints.stream()
                .mapToInt(point -> (int) Math.round(point.distance(average)))
                .max()
                .orElse(0);
        if (spread > MAX_LEARNED_NPC_POINT_SPREAD_PX) {
            log.info("[vision-memory] learned NPC point skipped: key={} reason=unstable spread={} count={}",
                    key, spread, successfulPoints.size());
            return Optional.empty();
        }

        Point clamped = new Point(
                Math.max(0, Math.min(IMAGE_WIDTH - 1, average.x)),
                Math.max(0, Math.min(IMAGE_HEIGHT - 1, average.y)));
        LearnedNpcClickPoint result = new LearnedNpcClickPoint(
                key, clamped.x, clamped.y, successfulPoints.size(), spread, latest.outcome);
        log.info("[vision-memory] learned NPC point ready: {}", result.toSummaryText());
        return Optional.of(result);
    }

    private OcrWindowRegion computeRecommendedRoi(MemoryEntry entry) {
        if (entry == null || entry.samples == null || entry.samples.isEmpty()) {
            return null;
        }

        int minX = IMAGE_WIDTH;
        int minY = IMAGE_HEIGHT;
        int maxX = -1;
        int maxY = -1;
        for (MemorySample sample : entry.samples) {
            if (sample == null) {
                continue;
            }
            if (sample.textRect != null) {
                OcrWindowRegion rect = sample.textRect.toRegion().clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
                if (rect.isValid()) {
                    minX = Math.min(minX, rect.x1());
                    minY = Math.min(minY, rect.y1());
                    maxX = Math.max(maxX, rect.x2());
                    maxY = Math.max(maxY, rect.y2());
                }
            }
            if (sample.clickPoint != null) {
                minX = Math.min(minX, sample.clickPoint.x);
                minY = Math.min(minY, sample.clickPoint.y);
                maxX = Math.max(maxX, sample.clickPoint.x);
                maxY = Math.max(maxY, sample.clickPoint.y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return null;
        }

        int successCount = Math.max(0, entry.successCount);
        int padX = successCount >= 10 ? 110 : successCount >= 3 ? 150 : 220;
        int padY = successCount >= 10 ? 80 : successCount >= 3 ? 110 : 160;
        int minWidth = successCount >= 10 ? 220 : successCount >= 3 ? 300 : 420;
        int minHeight = successCount >= 10 ? 160 : successCount >= 3 ? 220 : 300;

        OcrWindowRegion expanded = new OcrWindowRegion(minX, minY, maxX + 1, maxY + 1)
                .expand(padX, padY, IMAGE_WIDTH, IMAGE_HEIGHT);
        return enforceMinimumSize(expanded, minWidth, minHeight);
    }

    private void addPolicyRegion(List<OcrWindowRegion> regions, MemoryFile memory, String key) {
        if (memory == null || memory.policies == null || key == null) {
            return;
        }
        RoiPolicy policy = memory.policies.roiPolicies.get(key);
        if (!isUsablePolicy(policy)) {
            return;
        }
        OcrWindowRegion region = policy.recommendedRoi.toRegion().clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
        if (region.isValid()) {
            addUniqueRegion(regions, region);
        }
    }

    private void addLegacyRoiRegion(List<OcrWindowRegion> regions, MemoryFile memory, String key) {
        if (memory == null || key == null) {
            return;
        }
        MemoryEntry entry = memory.entries.get(key);
        if (entry == null || entry.recommendedRoi == null) {
            return;
        }
        OcrWindowRegion region = entry.recommendedRoi.toRegion().clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
        if (region.isValid()) {
            addUniqueRegion(regions, region);
        }
    }

    private void addNpcClickSampleRegion(List<OcrWindowRegion> regions,
                                         MemoryFile memory,
                                         String mapName,
                                         Integer mapX,
                                         Integer mapY,
                                         String targetName,
                                         boolean roamingTarget) {
        OcrWindowRegion region = regionFromNpcClickSamples(memory, mapName, mapX, mapY, targetName, roamingTarget);
        if (region != null && region.isValid()) {
            addUniqueRegion(regions, region);
        }
    }

    /*
     * NPC click samples are a second, weaker source of OCR region hints. They do not prove where the
     * yellow name is, but a verified click point is usually close enough to seed a broad OCR crop.
     * Combat targets use nearby coordinate samples; fixed NPCs use the exact legacy click key.
     */
    private OcrWindowRegion regionFromNpcClickSamples(MemoryFile memory,
                                                      String mapName,
                                                      Integer mapX,
                                                      Integer mapY,
                                                      String targetName,
                                                      boolean roamingTarget) {
        if (memory == null || memory.npcClickSamples == null || memory.npcClickSamples.isEmpty()) {
            return null;
        }
        List<Point> points = new ArrayList<>();
        for (int i = memory.npcClickSamples.size() - 1; i >= 0 && points.size() < MAX_LEARNED_NPC_RECENT_SAMPLES; i--) {
            NpcClickSample sample = memory.npcClickSamples.get(i);
            if (!isCompatibleNpcClickSample(sample, mapName, mapX, mapY, targetName, roamingTarget)) {
                continue;
            }
            Point point = npcSampleClickPoint(sample);
            if (point != null) {
                points.add(point);
            }
        }
        if (points.isEmpty()) {
            return null;
        }
        return regionAroundPoints(points, roamingTarget);
    }

    private boolean isCompatibleNpcClickSample(NpcClickSample sample,
                                               String mapName,
                                               Integer mapX,
                                               Integer mapY,
                                               String targetName,
                                               boolean roamingTarget) {
        if (sample == null || !sample.clicked || !sample.success || !hasStrongNpcVerification(sample)) {
            return false;
        }
        if (!sameNormalized(sample.mapName, mapName)) {
            return false;
        }
        if (roamingTarget) {
            return sample.targetMapX != null
                    && sample.targetMapY != null
                    && mapX != null
                    && mapY != null
                    && coordinateDistance(sample.targetMapX, sample.targetMapY, mapX, mapY)
                    <= NPC_COORD_BUCKET_SIZE * (NPC_COORD_NEIGHBOR_RADIUS + 1);
        }
        return sameNormalized(sample.npcName, targetName)
                && sample.targetMapX != null
                && sample.targetMapY != null
                && sample.targetMapX.equals(mapX)
                && sample.targetMapY.equals(mapY);
    }

    private OcrWindowRegion regionAroundPoints(List<Point> points, boolean roamingTarget) {
        int minX = IMAGE_WIDTH;
        int minY = IMAGE_HEIGHT;
        int maxX = -1;
        int maxY = -1;
        for (Point point : points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        if (maxX < minX || maxY < minY) {
            return null;
        }
        int count = points.size();
        int padX = roamingTarget
                ? count >= 10 ? 140 : count >= 3 ? 190 : 260
                : count >= 10 ? 110 : count >= 3 ? 150 : 220;
        int padY = roamingTarget
                ? count >= 10 ? 100 : count >= 3 ? 140 : 190
                : count >= 10 ? 80 : count >= 3 ? 110 : 160;
        int minWidth = roamingTarget
                ? count >= 10 ? 260 : count >= 3 ? 380 : 520
                : count >= 10 ? 220 : count >= 3 ? 300 : 420;
        int minHeight = roamingTarget
                ? count >= 10 ? 190 : count >= 3 ? 260 : 360
                : count >= 10 ? 160 : count >= 3 ? 220 : 300;
        OcrWindowRegion expanded = new OcrWindowRegion(minX, minY, maxX + 1, maxY + 1)
                .expand(padX, padY, IMAGE_WIDTH, IMAGE_HEIGHT);
        return enforceMinimumSize(expanded, minWidth, minHeight);
    }

    private void updateRoiPolicy(MemoryFile memory,
                                 String key,
                                 TargetCandidateSample sample,
                                 boolean matched,
                                 boolean verified) {
        if (memory == null || key == null || sample == null) {
            return;
        }
        RoiPolicy policy = memory.policies.roiPolicies.computeIfAbsent(key, ignored -> new RoiPolicy());
        policy.key = key;
        policy.targetKind = sample.roamingTarget ? "clickable-target" : "fixed-npc";
        policy.taskType = sample.source;
        policy.mapName = sample.mapName;
        policy.targetName = sample.targetName;
        policy.targetMapX = sample.targetMapX;
        policy.targetMapY = sample.targetMapY;
        policy.coordinateBucket = sample.coordinateBucket;
        policy.lastAttemptAt = memory.updatedAt;
        policy.lastMessage = sample.message;
        policy.attemptCount++;

        if (matched && verified && (sample.textRect != null || sample.clickPoint != null)) {
            policy.successCount++;
            policy.failureStreak = 0;
            policy.stale = false;
            policy.lastSuccessAt = memory.updatedAt;
            if (sample.textRect != null) {
                policy.recentRects.add(sample.textRect);
                trimList(policy.recentRects, MAX_SAMPLES_PER_KEY);
            }
            if (sample.clickPoint != null) {
                policy.recentPoints.add(sample.clickPoint);
                trimList(policy.recentPoints, MAX_SAMPLES_PER_KEY);
            }
            OcrWindowRegion recommended = computeRecommendedRoi(policy);
            policy.recommendedRoi = recommended == null ? null : RegionData.from(recommended);
            policy.stage = roiStage(policy.successCount);
            policy.confidence = Math.min(1.0, Math.max(0.1, policy.successCount / 10.0));
        } else {
            policy.failureCount++;
            policy.failureStreak++;
            if (policy.failureStreak >= ROI_POLICY_FAILURE_STALE_THRESHOLD) {
                policy.stale = true;
            }
        }
    }

    private OcrWindowRegion computeRecommendedRoi(RoiPolicy policy) {
        if (policy == null) {
            return null;
        }
        int minX = IMAGE_WIDTH;
        int minY = IMAGE_HEIGHT;
        int maxX = -1;
        int maxY = -1;
        for (RegionData data : policy.recentRects) {
            if (data == null) {
                continue;
            }
            OcrWindowRegion rect = data.toRegion().clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
            if (rect.isValid()) {
                minX = Math.min(minX, rect.x1());
                minY = Math.min(minY, rect.y1());
                maxX = Math.max(maxX, rect.x2());
                maxY = Math.max(maxY, rect.y2());
            }
        }
        for (PointData point : policy.recentPoints) {
            if (point == null) {
                continue;
            }
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        if (maxX < minX || maxY < minY) {
            return null;
        }

        boolean roaming = "clickable-target".equals(policy.targetKind);
        return regionAroundPoints(List.of(new Point(minX, minY), new Point(maxX, maxY)), roaming);
    }

    private boolean isUsablePolicy(RoiPolicy policy) {
        return policy != null
                && policy.recommendedRoi != null
                && !policy.stale
                && policy.failureStreak < ROI_POLICY_FAILURE_STALE_THRESHOLD;
    }

    private String roiStage(int successCount) {
        if (successCount >= 10) {
            return "tight";
        }
        if (successCount >= 3) {
            return "stable";
        }
        if (successCount > 0) {
            return "coarse";
        }
        return "cold";
    }

    private List<String> npcTargetRoiPolicyKeys(String mapName,
                                                Integer mapX,
                                                Integer mapY,
                                                String targetName,
                                                boolean roamingTarget) {
        /*
         * V2 keys are intentionally namespaced by target kind. Fixed NPCs should not borrow from
         * roaming monsters, and roaming targets should not require a stable OCR-visible name.
         */
        String map = safe(mapName);
        String target = safe(targetName);
        if (!roamingTarget) {
            return List.of("roi|fixed-npc|npc-click|" + map + "|" + target + "|"
                    + nullablePoint(mapX, mapY) + "|1024x768|yellow-name");
        }

        /*
         * For roaming targets, include neighboring coordinate buckets. The task-panel coordinate can
         * be slightly stale by the time the leader arrives, so nearby learned crops are still useful
         * as ordered hints.
         */
        List<String> keys = new ArrayList<>();
        for (String bucket : nearbyCoordinateBuckets(mapX, mapY)) {
            keys.add("roi|clickable-target|npc-click|" + map + "|" + bucket + "|any-name|1024x768|yellow-name");
        }
        return List.copyOf(keys);
    }

    private List<String> nearbyCoordinateBuckets(Integer mapX, Integer mapY) {
        if (mapX == null || mapY == null) {
            return List.of("bucket:unknown");
        }
        /*
         * Bucket size is in logical map-coordinate units, not pixels. The small 3x3 neighborhood is
         * a compromise: broad enough for small target movement, but not broad enough to mix unrelated
         * areas of the same map.
         */
        int bucketX = Math.floorDiv(mapX, NPC_COORD_BUCKET_SIZE);
        int bucketY = Math.floorDiv(mapY, NPC_COORD_BUCKET_SIZE);
        List<String> buckets = new ArrayList<>();
        for (int dy = -NPC_COORD_NEIGHBOR_RADIUS; dy <= NPC_COORD_NEIGHBOR_RADIUS; dy++) {
            for (int dx = -NPC_COORD_NEIGHBOR_RADIUS; dx <= NPC_COORD_NEIGHBOR_RADIUS; dx++) {
                buckets.add("bucket:" + (bucketX + dx) + "," + (bucketY + dy));
            }
        }
        return List.copyOf(buckets);
    }

    private String primaryCoordinateBucket(Integer mapX, Integer mapY) {
        return coordinateBucketText(mapX, mapY);
    }

    private static String coordinateBucketText(Integer mapX, Integer mapY) {
        if (mapX == null || mapY == null) {
            return "bucket:unknown";
        }
        return "bucket:" + Math.floorDiv(mapX, NPC_COORD_BUCKET_SIZE)
                + "," + Math.floorDiv(mapY, NPC_COORD_BUCKET_SIZE);
    }

    private List<String> npcClickRegionMemoryKeys(String mapName, Integer mapX, Integer mapY, String targetName) {
        /*
         * Legacy keys predate v2 RoiPolicy. Keep reading them so existing JSON remains useful, but
         * new learning should write through recordNpcTargetOcrObservation/updateRoiPolicy.
         */
        String map = safe(mapName);
        String target = safe(targetName);
        String coordinate = mapX == null || mapY == null ? "unknown" : mapX + "," + mapY;
        return List.of(
                "npc-click-region:" + map + ":" + target + ":" + coordinate,
                "npc-click-region:" + map + ":" + target,
                "npc-yellow-window:" + target,
                "npc-yellow-window:" + map + ":" + target
        );
    }

    private void addUniqueRegion(List<OcrWindowRegion> regions, OcrWindowRegion candidate) {
        if (candidate != null && !regions.contains(candidate)) {
            regions.add(candidate);
        }
    }

    private String summarizeRegions(List<OcrWindowRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return "[]";
        }
        return regions.stream().map(OcrWindowRegion::toShortText).toList().toString();
    }

    private OcrWindowRegion enforceMinimumSize(OcrWindowRegion region, int minWidth, int minHeight) {
        if (region == null || !region.isValid()) {
            return region;
        }
        int width = Math.max(region.width(), minWidth);
        int height = Math.max(region.height(), minHeight);
        int centerX = (region.x1() + region.x2()) / 2;
        int centerY = (region.y1() + region.y2()) / 2;
        return new OcrWindowRegion(
                centerX - width / 2,
                centerY - height / 2,
                centerX - width / 2 + width,
                centerY - height / 2 + height
        ).clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private MemoryFile loadMemory() {
        Path readPath = Files.exists(MEMORY_PATH) ? MEMORY_PATH : LEGACY_MEMORY_PATH;
        if (!Files.exists(readPath)) {
            return new MemoryFile();
        }
        try {
            MemoryFile file = objectMapper.readValue(readPath.toFile(), MemoryFile.class);
            if (file.entries == null) {
                file.entries = new LinkedHashMap<>();
            }
            if (file.playerAnchorSamples == null) {
                file.playerAnchorSamples = new ArrayList<>();
            }
            if (file.ocrAttempts == null) {
                file.ocrAttempts = new ArrayList<>();
            }
            if (file.npcClickSamples == null) {
                file.npcClickSamples = new ArrayList<>();
            }
            if (file.targetCandidateSamples == null) {
                file.targetCandidateSamples = new ArrayList<>();
            }
            if (file.policies == null) {
                file.policies = new VisionPolicies();
            }
            if (file.policies.roiPolicies == null) {
                file.policies.roiPolicies = new LinkedHashMap<>();
            }
            if (file.policies.clickPolicies == null) {
                file.policies.clickPolicies = new LinkedHashMap<>();
            }
            if (file.memoryType == null || file.memoryType.isBlank()) {
                file.memoryType = "vision-memory-v2";
            }
            return file;
        } catch (Exception e) {
            log.warn("[ocr-roi-memory] load failed: path={} reason={}", readPath, e.getMessage(), e);
            return new MemoryFile();
        }
    }

    private void saveMemory(MemoryFile memory) {
        try {
            Path parent = MEMORY_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = MEMORY_PATH.resolveSibling(MEMORY_PATH.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), memory);
            try {
                Files.move(tmp, MEMORY_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(tmp, MEMORY_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.warn("[ocr-roi-memory] save failed: path={} reason={}", MEMORY_PATH, e.getMessage(), e);
        }
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeTargetName(String targetName, boolean roamingTarget) {
        /*
         * A blank fixed-NPC name is unsafe: different NPCs on the same map/coordinate could share a
         * crop. Roaming targets are different because Xiuluo-like tasks use the coordinate as the
         * primary identity and may not have a reliable monster name.
         */
        String target = normalizeKey(targetName);
        if (target != null) {
            return target;
        }
        return roamingTarget ? "any-name" : null;
    }

    private boolean sameNormalized(String a, String b) {
        String left = normalizeKey(a);
        String right = normalizeKey(b);
        if (left == null || right == null) {
            return left == right;
        }
        return left.equals(right);
    }

    private int coordinateDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public static class MemoryFile {
        public String memoryType = "vision-memory-v2";
        public String updatedAt;
        public Map<String, MemoryEntry> entries = new LinkedHashMap<>();
        public List<MemorySample> playerAnchorSamples = new ArrayList<>();
        public List<OcrAttemptSample> ocrAttempts = new ArrayList<>();
        public List<NpcClickSample> npcClickSamples = new ArrayList<>();
        public List<TargetCandidateSample> targetCandidateSamples = new ArrayList<>();
        public VisionPolicies policies = new VisionPolicies();
    }

    public static class VisionPolicies {
        public Map<String, RoiPolicy> roiPolicies = new LinkedHashMap<>();
        public Map<String, ClickPolicy> clickPolicies = new LinkedHashMap<>();
    }

    public static class RoiPolicy {
        public String key;
        public String targetKind;
        public String taskType;
        public String mapName;
        public String targetName;
        public Integer targetMapX;
        public Integer targetMapY;
        public String coordinateBucket;
        public int attemptCount;
        public int failureCount;
        public int successCount;
        public int failureStreak;
        public boolean stale;
        public double confidence;
        public String stage;
        public String lastAttemptAt;
        public String lastSuccessAt;
        public String lastMessage;
        public RegionData recommendedRoi;
        public List<RegionData> recentRects = new ArrayList<>();
        public List<PointData> recentPoints = new ArrayList<>();
    }

    public static class ClickPolicy {
        public String key;
        public PointData point;
        public int sampleCount;
        public int spreadPx;
        public boolean stale;
        public String lastOutcome;
    }

    public static class MemoryEntry {
        public int attemptCount;
        public int failureCount;
        public int successCount;
        public String lastAttemptAt;
        public String lastSuccessAt;
        public RegionData recommendedRoi;
        public List<MemorySample> samples = new ArrayList<>();
        public String lastCameraState;
        public String lastMapName;
        public Integer lastMapX;
        public Integer lastMapY;
        public PointData lastAnchor;
        public PointData lastAnchorDelta;
        public String lastNpcClickAt;
        public String lastNpcClickOutcome;
        public PointData lastPredictedClick;
    }

    public static class MemorySample {
        public String createdAt;
        public String key;
        public String source;
        public String mapName;
        public Integer mapX;
        public Integer mapY;
        public RegionData textRect;
        public PointData clickPoint;
        public PointData anchor;
        public PointData centerAnchor;
        public PointData anchorDelta;
        public String cameraState;
        public String matchedText;
        public String matchedFragment;
        public String matchMode;
        public int compensationX;
        public double score;
        public Integer windowWidth;
        public Integer windowHeight;
        public String provider;
        public String preprocessVariant;
        public String imagePath;
        public String secondaryImagePath;
        public String locationSource;

        static MemorySample from(OcrWindowRegion textRect, Point clickPoint) {
            MemorySample sample = new MemorySample();
            sample.createdAt = LocalDateTime.now().toString();
            sample.textRect = textRect == null ? null : RegionData.from(textRect);
            sample.clickPoint = clickPoint == null ? null : PointData.from(clickPoint);
            return sample;
        }

        static MemorySample fromPlayerAnchor(String key,
                                             String source,
                                             LocationVisionService.PlayerAnchorMatch match,
                                             TextRecognizer.LocationInfo location,
                                             int windowWidth,
                                             int windowHeight,
                                             String provider,
                                             String preprocessVariant,
                                             String imagePath,
                                             String secondaryImagePath,
                                             String locationSource) {
            MemorySample sample = from(match.textRect(), match.anchor());
            sample.key = key;
            sample.source = source == null || source.isBlank() ? "UNKNOWN" : source.trim();
            sample.mapName = location == null ? null : location.mapName;
            sample.mapX = location == null ? null : location.x;
            sample.mapY = location == null ? null : location.y;
            sample.anchor = PointData.from(match.anchor());
            int safeWidth = windowWidth > 0 ? windowWidth : IMAGE_WIDTH;
            int safeHeight = windowHeight > 0 ? windowHeight : IMAGE_HEIGHT;
            sample.windowWidth = safeWidth;
            sample.windowHeight = safeHeight;
            Point center = new Point(safeWidth / 2, safeHeight / 2);
            sample.centerAnchor = PointData.from(center);
            Point delta = new Point(match.anchor().x - center.x, match.anchor().y - center.y);
            sample.anchorDelta = PointData.from(delta);
            sample.cameraState = classifyCameraState(delta);
            sample.matchedText = match.matchedText();
            sample.matchedFragment = match.matchedFragment();
            sample.matchMode = match.matchMode();
            sample.compensationX = match.compensationX();
            sample.score = match.score();
            sample.provider = provider;
            sample.preprocessVariant = preprocessVariant;
            sample.imagePath = imagePath;
            sample.secondaryImagePath = secondaryImagePath;
            sample.locationSource = locationSource;
            return sample;
        }
    }

    public static class OcrAttemptSample {
        public String createdAt;
        public String key;
        public String purpose;
        public String regionType;
        public RegionData scanRegion;
        public String targetText;
        public boolean matched;
        public String matchedText;
        public RegionData textRect;
        public PointData clickPoint;
        public double score;
        public int wordCount;
        public String message;
        public Integer windowWidth;
        public Integer windowHeight;
        public String provider;
        public String preprocessVariant;
        public String rawPath;
        public String maskedPath;
        public String overlayPath;
        public String roiPath;

        static OcrAttemptSample from(String key,
                                     String purpose,
                                     String regionType,
                                     OcrWindowRegion scanRegion,
                                     String targetText,
                                     List<TextRecognizer.OcrWordResult> words,
                                     boolean matched,
                                     TextRecognizer.OcrWordResult matchedWord,
                                     String message,
                                     int windowWidth,
                                     int windowHeight,
                                     String provider,
                                     String preprocessVariant,
                                     String rawPath,
                                     String maskedPath,
                                     String overlayPath,
                                     String roiPath) {
            OcrAttemptSample sample = new OcrAttemptSample();
            sample.createdAt = LocalDateTime.now().toString();
            sample.key = key;
            sample.purpose = purpose;
            sample.regionType = regionType;
            sample.scanRegion = scanRegion == null ? null : RegionData.from(scanRegion);
            sample.targetText = targetText;
            sample.matched = matched;
            sample.matchedText = matchedWord == null ? null : matchedWord.getText();
            OcrWindowRegion textRect = wordToRegion(matchedWord);
            sample.textRect = textRect == null ? null : RegionData.from(textRect);
            sample.clickPoint = PointData.from(wordToPoint(matchedWord));
            sample.score = matchedWord == null ? 0.0 : matchedWord.getScore();
            sample.wordCount = words == null ? 0 : words.size();
            sample.message = message;
            sample.windowWidth = windowWidth > 0 ? windowWidth : IMAGE_WIDTH;
            sample.windowHeight = windowHeight > 0 ? windowHeight : IMAGE_HEIGHT;
            sample.provider = provider;
            sample.preprocessVariant = preprocessVariant;
            sample.rawPath = rawPath;
            sample.maskedPath = maskedPath;
            sample.overlayPath = overlayPath;
            sample.roiPath = roiPath;
            return sample;
        }
    }

    public static class NpcClickSample {
        public String createdAt;
        public String key;
        public String source;
        public String mapName;
        public Integer playerMapX;
        public Integer playerMapY;
        public String npcName;
        public Integer targetMapX;
        public Integer targetMapY;
        public Integer deltaMapX;
        public Integer deltaMapY;
        public PointData windowBase;
        public PointData playerAnchorAbs;
        public PointData playerAnchorRel;
        public PointData predictedClickAbs;
        public PointData predictedClickRel;
        public PointData actualClickAbs;
        public PointData actualClickRel;
        public Integer tuneX;
        public Integer tuneY;
        public String formulaVersion;
        public boolean clicked;
        public boolean success;
        public String outcome;
        public String verification;
        public boolean actualClickMeasured;
        public String actualClickSource;
        public String verificationStrength;
    }

    public static class TargetCandidateSample {
        public String createdAt;
        public String key;
        public String source;
        public String mapName;
        public Integer targetMapX;
        public Integer targetMapY;
        public String coordinateBucket;
        public String targetName;
        public boolean roamingTarget;
        public RegionData scanRegion;
        public RegionData textRect;
        public PointData clickPoint;
        public boolean matched;
        public boolean verified;
        public String observedText;
        public String message;

        static TargetCandidateSample from(String key,
                                          String source,
                                          String mapName,
                                          Integer targetMapX,
                                          Integer targetMapY,
                                          String targetName,
                                          boolean roamingTarget,
                                          OcrWindowRegion scanRegion,
                                          OcrWindowRegion textRect,
                                          Point clickPoint,
                                          boolean matched,
                                          boolean verified,
                                          String observedText,
                                          String message) {
            TargetCandidateSample sample = new TargetCandidateSample();
            sample.createdAt = LocalDateTime.now().toString();
            sample.key = key;
            sample.source = source;
            sample.mapName = mapName;
            sample.targetMapX = targetMapX;
            sample.targetMapY = targetMapY;
            sample.coordinateBucket = roamingTarget ? coordinateBucketText(targetMapX, targetMapY) : null;
            sample.targetName = targetName;
            sample.roamingTarget = roamingTarget;
            sample.scanRegion = scanRegion == null ? null : RegionData.from(scanRegion);
            sample.textRect = textRect == null ? null : RegionData.from(textRect);
            sample.clickPoint = PointData.from(clickPoint);
            sample.matched = matched;
            sample.verified = verified;
            sample.observedText = observedText;
            sample.message = message;
            return sample;
        }
    }

    public static class RegionData {
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        static RegionData from(OcrWindowRegion region) {
            RegionData data = new RegionData();
            data.x1 = region.x1();
            data.y1 = region.y1();
            data.x2 = region.x2();
            data.y2 = region.y2();
            return data;
        }

        OcrWindowRegion toRegion() {
            return new OcrWindowRegion(x1, y1, x2, y2);
        }
    }

    public static class PointData {
        public int x;
        public int y;

        static PointData from(Point point) {
            if (point == null) {
                return null;
            }
            PointData data = new PointData();
            data.x = point.x;
            data.y = point.y;
            return data;
        }
    }

    public record RecordResult(boolean recorded, String key, String summary, String recommendedRoi) {
        static RecordResult skipped(String key, String reason) {
            return new RecordResult(false, key, reason == null ? "" : reason, "-");
        }
    }

    /**
     * Conservative learned direct-click recommendation for one NPC target.
     *
     * @param key stable vision-memory key used to derive the point.
     * @param x window-relative X coordinate in the 1024x768 game client.
     * @param y window-relative Y coordinate in the 1024x768 game client.
     * @param sampleCount number of recent successful samples used.
     * @param spreadPx maximum distance in pixels from the averaged point.
     * @param lastOutcome last recorded NPC-click outcome for diagnostics.
     */
    public record LearnedNpcClickPoint(String key,
                                       int x,
                                       int y,
                                       int sampleCount,
                                       int spreadPx,
                                       String lastOutcome) {
        public String toSummaryText() {
            return "key=" + key
                    + " point=(" + x + "," + y + ")"
                    + " samples=" + sampleCount
                    + " spreadPx=" + spreadPx
                    + " lastOutcome=" + safe(lastOutcome);
        }
    }

    private static TextRecognizer.OcrWordResult findMatchedWord(List<TextRecognizer.OcrWordResult> words,
                                                                String targetText) {
        if (words == null || words.isEmpty()) {
            return null;
        }
        String target = normalizeTextStatic(targetText);
        if (target.isBlank()) {
            return words.stream().filter(word -> word != null && word.getText() != null).findFirst().orElse(null);
        }
        for (TextRecognizer.OcrWordResult word : words) {
            String text = normalizeTextStatic(word == null ? null : word.getText());
            if (!text.isBlank() && (text.contains(target) || target.contains(text) && text.length() >= 2)) {
                return word;
            }
        }
        return null;
    }

    private static OcrWindowRegion wordToRegion(TextRecognizer.OcrWordResult word) {
        if (word == null) {
            return null;
        }
        return new OcrWindowRegion(
                word.getLeft(),
                word.getTop(),
                word.getLeft() + Math.max(1, word.getWidth()),
                word.getTop() + Math.max(1, word.getHeight())
        ).clamp(IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private static Point wordToPoint(TextRecognizer.OcrWordResult word) {
        if (word == null) {
            return null;
        }
        return new Point(word.getX(), word.getY());
    }

    private static PointData relativeToBase(Point absolute, Point base) {
        if (absolute == null || base == null) {
            return null;
        }
        return PointData.from(new Point(absolute.x - base.x, absolute.y - base.y));
    }

    private static <T> void trimList(List<T> list, int maxSize) {
        if (list == null) {
            return;
        }
        while (list.size() > maxSize) {
            list.remove(0);
        }
    }

    private static boolean hasAnyTargetIdentity(String mapName, String npcName, Integer targetMapX, Integer targetMapY) {
        return (mapName != null && !mapName.isBlank())
                || (npcName != null && !npcName.isBlank())
                || targetMapX != null
                || targetMapY != null;
    }

    private boolean hasStrongNpcVerification(NpcClickSample sample) {
        return sample != null
                && ("DIALOG_OPTION".equalsIgnoreCase(sample.verificationStrength)
                || "DIALOG_TEMPLATE".equalsIgnoreCase(sample.verificationStrength)
                || sample.actualClickMeasured);
    }

    private Point npcSampleClickPoint(NpcClickSample sample) {
        if (sample == null) {
            return null;
        }
        PointData point = sample.actualClickMeasured && sample.actualClickRel != null
                ? sample.actualClickRel
                : sample.predictedClickRel;
        return point == null ? null : new Point(point.x, point.y);
    }

    private static String buildNpcClickKey(String mapName, String npcName, Integer targetMapX, Integer targetMapY) {
        StringBuilder builder = new StringBuilder("npc-click");
        builder.append("|").append(safe(mapName));
        builder.append("|").append(safe(npcName));
        builder.append("|").append(nullablePoint(targetMapX, targetMapY));
        return builder.toString();
    }

    private static String normalizeTextStatic(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private static String classifyCameraState(Point delta) {
        if (delta == null) {
            return "UNKNOWN";
        }
        String horizontal = "";
        String vertical = "";
        if (delta.x <= -CAMERA_DELTA_THRESHOLD) {
            horizontal = "LEFT";
        } else if (delta.x >= CAMERA_DELTA_THRESHOLD) {
            horizontal = "RIGHT";
        }
        if (delta.y <= -CAMERA_DELTA_THRESHOLD) {
            vertical = "UP";
        } else if (delta.y >= CAMERA_DELTA_THRESHOLD) {
            vertical = "DOWN";
        }
        if (horizontal.isBlank() && vertical.isBlank()) {
            return "CENTERED";
        }
        if (horizontal.isBlank()) {
            return vertical;
        }
        if (vertical.isBlank()) {
            return horizontal;
        }
        return horizontal + "_" + vertical;
    }

    private static String pointText(PointData point) {
        return point == null ? "-" : point.x + "," + point.y;
    }

    private static String nullablePoint(Integer x, Integer y) {
        return x == null || y == null ? "-" : x + "," + y;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
