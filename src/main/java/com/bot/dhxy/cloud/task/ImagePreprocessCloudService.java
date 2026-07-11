package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionMode;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImagePreprocessCloudService {

    private static final Logger log = LoggerFactory.getLogger(ImagePreprocessCloudService.class);
    private static final String DEFAULT_TASK_CODE = "unknown";
    private static final String DEFAULT_PHASE = "image-preprocess";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_NO_RESULT = "NO_RESULT";
    private static final String COORDINATE_SPACE_KEY = "coordinateSpace";
    private static final String COORDINATE_SPACE_ROI_RELATIVE = "ROI_RELATIVE";
    private static final String COORDINATE_SPACE_WINDOW_RELATIVE = "WINDOW_RELATIVE";
    private static final String RETURN_WASHED_IMAGE = "RETURN_WASHED_IMAGE";
    private static final String RETURN_RESULT_VALUES = "RETURN_RESULT_VALUES";
    private static final String RETURN_CANDIDATES = "RETURN_CANDIDATES";
    private static final double MIN_EXECUTE_CONFIDENCE = 0.50d;

    private final CloudDecisionCoordinator coordinator;

    public ImagePreprocessCloudService(CloudDecisionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Sends a transferable raw image payload or ROI preprocess request to the cloud-required image
     * authority.
     *
     * <p>Coordinates returned by cloud diagnostics are accepted only when they stay inside the
     * request ROI/window safety bounds. Any unavailable cloud response, invalid schema, low
     * confidence, or unsafe coordinate becomes {@link ImagePreprocessCloudDecision.Status#REQUIRED_FAILURE};
     * callers must not continue by invoking local image washing or template preprocessing.</p>
     *
     * @param request image preprocess request. {@code imagePayloadBase64} carries the raw image
     *                bytes to cloud; {@code payloadMimeType} and {@code imageSha256} describe that
     *                payload; {@code rawImagePath} and {@code debugImageId} are debug references
     *                only. {@code roi} is an optional window-relative rectangle in pixels, and
     *                {@code windowWidth/windowHeight} must be explicit positive current game-window
     *                pixel bounds.
     * @return parsed cloud preprocess decision, explicit no-result, disabled, or required failure.
     */
    public ImagePreprocessCloudDecision preprocess(ImagePreprocessCloudRequest request) {
        if (request == null || request.getOperation() == null) {
            return requiredFailure(null, null, "missing image preprocess request/operation", List.of(), List.of());
        }
        if (!coordinator.isActive(CloudDecisionServiceId.IMAGE_PREPROCESS)) {
            return ImagePreprocessCloudDecision.builder()
                    .status(ImagePreprocessCloudDecision.Status.DISABLED)
                    .operation(request.getOperation())
                    .reason("service disabled")
                    .build();
        }
        String validationError = requestValidationError(request);
        if (validationError != null) {
            return requiredFailure(request.getOperation(), null, validationError, List.of(), List.of());
        }

        String returnMode = returnMode(request.getOperation());
        String localDecision = "status=LOCAL_SHADOW;operation=" + request.getOperation().name()
                + ";returnMode=" + returnMode;
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(CloudDecisionServiceId.IMAGE_PREPROCESS)
                .traceId(traceId(request))
                .taskCode(normalize(request.getTaskCode(), DEFAULT_TASK_CODE))
                .phase(normalize(request.getPhase(), DEFAULT_PHASE))
                .windowId(request.getWindowId())
                .taskRunId(request.getTaskRunId())
                .policyVersion(request.getPolicyVersion())
                .localDecision(localDecision)
                .context(context(request))
                .build();

        List<ImagePreprocessCloudDecision.CandidateBox> candidateBoxes = new ArrayList<>();
        List<ImagePreprocessCloudDecision.CandidatePoint> candidatePoints = new ArrayList<>();
        WashedImageHolder washedImageHolder = new WashedImageHolder();
        CloudDecisionResult cloudResult = coordinator.shadow(
                cloudRequest,
                localDecision,
                imagePreprocessExecutionGate(request, candidateBoxes, candidatePoints, washedImageHolder));

        if (cloudResult.isExecuted()) {
            Map<String, String> fields = fields(cloudResult.getResponse().getDecision());
            String status = upper(fields.get("status"));
            if (STATUS_NO_RESULT.equals(status)) {
                return ImagePreprocessCloudDecision.builder()
                        .status(ImagePreprocessCloudDecision.Status.NO_RESULT)
                        .operation(request.getOperation())
                        .cloudResult(cloudResult)
                        .confidence(cloudResult.getResponse().getConfidence())
                        .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                        .debugToken(debugToken(cloudResult.getResponse()))
                        .resultValues(resultValues(cloudResult.getResponse()))
                        .candidateBoxes(List.copyOf(candidateBoxes))
                        .candidatePoints(List.copyOf(candidatePoints))
                        .build();
            }
            WashedImage washedImage = washedImageHolder.value();
            return ImagePreprocessCloudDecision.builder()
                    .status(ImagePreprocessCloudDecision.Status.CLOUD_EXECUTED)
                    .operation(request.getOperation())
                    .cloudResult(cloudResult)
                    .confidence(cloudResult.getResponse().getConfidence())
                    .reason(firstText(fields.get("reason"), cloudResult.getReason()))
                    .debugToken(debugToken(cloudResult.getResponse()))
                    .washedImagePayloadBase64(washedImage == null ? null : washedImage.payloadBase64())
                    .washedPayloadMimeType(washedImage == null ? null : washedImage.mimeType())
                    .washedImageSha256(washedImage == null ? null : washedImage.sha256())
                    .washedWidth(washedImage == null ? 0 : washedImage.width())
                    .washedHeight(washedImage == null ? 0 : washedImage.height())
                    .resultValues(resultValues(cloudResult.getResponse()))
                    .candidateBoxes(List.copyOf(candidateBoxes))
                    .candidatePoints(List.copyOf(candidatePoints))
                    .build();
        }

        if (cloudResult.getMode() == CloudDecisionMode.DISABLED) {
            return ImagePreprocessCloudDecision.builder()
                    .status(ImagePreprocessCloudDecision.Status.DISABLED)
                    .operation(request.getOperation())
                    .cloudResult(cloudResult)
                    .reason(cloudResult.getReason())
                    .build();
        }

        log.warn("cloud.execute serviceId=IMAGE_PREPROCESS accepted=false required-failure taskCode={} phase={} "
                        + "operation={} reason={} cloudDecision={}",
                cloudRequest.getTaskCode(),
                cloudRequest.getPhase(),
                request.getOperation(),
                cloudResult.getReason(),
                cloudResult.getResponse() == null ? null : cloudResult.getResponse().getDecision());
        return requiredFailure(request.getOperation(), cloudResult, cloudResult.getReason(),
                candidateBoxes, candidatePoints);
    }

    private CloudDecisionExecutionGate imagePreprocessExecutionGate(
            ImagePreprocessCloudRequest imageRequest,
            List<ImagePreprocessCloudDecision.CandidateBox> candidateBoxes,
            List<ImagePreprocessCloudDecision.CandidatePoint> candidatePoints,
            WashedImageHolder washedImageHolder) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == CloudDecisionServiceId.IMAGE_PREPROCESS;
            }

            @Override
            public CloudDecisionExecutionGate.GateResult evaluate(
                    CloudDecisionRequest request,
                    CloudDecisionResponse response,
                    String localDecision) {
                if (response.getConfidence() < MIN_EXECUTE_CONFIDENCE) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "low confidence: " + response.getConfidence());
                }
                Map<String, String> fields = fields(response.getDecision());
                String operation = fields.get("operation");
                if (!imageRequest.getOperation().name().equals(operation)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "operation mismatch: expected=" + imageRequest.getOperation().name()
                                    + " actual=" + safe(operation));
                }
                String status = upper(fields.get("status"));
                if (!STATUS_EXECUTED.equals(status) && !STATUS_NO_RESULT.equals(status)) {
                    return CloudDecisionExecutionGate.GateResult.rejected(
                            "unsupported status: " + safe(status));
                }
                WashedImageParseResult washedImage = STATUS_EXECUTED.equals(status)
                        ? parseWashedImage(response)
                        : WashedImageParseResult.empty();
                if (!washedImage.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(washedImage.reason());
                }
                CoordinateParseResult parsed = parseCandidates(imageRequest, response);
                if (!parsed.accepted()) {
                    return CloudDecisionExecutionGate.GateResult.rejected(parsed.reason());
                }
                candidateBoxes.clear();
                candidateBoxes.addAll(parsed.boxes());
                candidatePoints.clear();
                candidatePoints.addAll(parsed.points());
                washedImageHolder.set(washedImage.image());
                return CloudDecisionExecutionGate.GateResult.accepted(
                        response.getDecision(),
                        STATUS_NO_RESULT.equals(status)
                                ? "execute percent gate hit; image preprocess cloud returned no-result"
                                : "execute percent gate hit; using image preprocess cloud result");
            }
        };
    }

    private static CoordinateParseResult parseCandidates(
            ImagePreprocessCloudRequest request,
            CloudDecisionResponse response) {
        Map<String, String> diagnostics = response.getDiagnostics() == null ? Map.of() : response.getDiagnostics();
        String candidateBoxesText = diagnostics.get("candidateBoxes");
        String candidatePointsText = diagnostics.get("candidatePoints");
        boolean hasCoordinates = hasText(candidateBoxesText) || hasText(candidatePointsText);
        String coordinateSpace = diagnostics.get(COORDINATE_SPACE_KEY);
        if (hasCoordinates
                && !COORDINATE_SPACE_ROI_RELATIVE.equals(coordinateSpace)
                && !COORDINATE_SPACE_WINDOW_RELATIVE.equals(coordinateSpace)) {
            return CoordinateParseResult.rejected(
                    "diagnostics.coordinateSpace must be ROI_RELATIVE or WINDOW_RELATIVE when candidates are present");
        }

        List<ImagePreprocessCloudDecision.CandidateBox> boxes = parseBoxes(candidateBoxesText);
        List<ImagePreprocessCloudDecision.CandidatePoint> points = parsePoints(candidatePointsText);
        if (boxes == null) {
            return CoordinateParseResult.rejected("candidateBoxes must parse as x,y,width,height list");
        }
        if (points == null) {
            return CoordinateParseResult.rejected("candidatePoints must parse as x,y list");
        }

        for (ImagePreprocessCloudDecision.CandidateBox box : boxes) {
            String reject = validateBox(request, coordinateSpace, box);
            if (reject != null) {
                return CoordinateParseResult.rejected(reject);
            }
        }
        for (ImagePreprocessCloudDecision.CandidatePoint point : points) {
            String reject = validatePoint(request, coordinateSpace, point);
            if (reject != null) {
                return CoordinateParseResult.rejected(reject);
            }
        }
        return CoordinateParseResult.accepted(
                toWindowRelativeBoxes(request, coordinateSpace, boxes),
                toWindowRelativePoints(request, coordinateSpace, points));
    }

    private static WashedImageParseResult parseWashedImage(CloudDecisionResponse response) {
        Map<String, String> diagnostics = response.getDiagnostics() == null ? Map.of() : response.getDiagnostics();
        String payloadBase64 = diagnostics.get("washedImagePayloadBase64");
        String mimeType = diagnostics.get("washedPayloadMimeType");
        String sha256 = diagnostics.get("washedImageSha256");
        String widthText = diagnostics.get("washedWidth");
        String heightText = diagnostics.get("washedHeight");
        boolean hasWashedField = hasText(payloadBase64)
                || hasText(mimeType)
                || hasText(sha256)
                || hasText(widthText)
                || hasText(heightText);
        if (!hasWashedField) {
            return WashedImageParseResult.empty();
        }
        if (!"image/png".equals(mimeType)) {
            return WashedImageParseResult.rejected("washed image mime must be image/png");
        }
        Integer width = parsePositiveInt(widthText);
        Integer height = parsePositiveInt(heightText);
        if (width == null || height == null) {
            return WashedImageParseResult.rejected("washed image size must be positive");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payloadBase64.trim());
        } catch (IllegalArgumentException e) {
            return WashedImageParseResult.rejected("washed image payload must be valid base64");
        }
        String actualSha = sha256Hex(bytes);
        if (!actualSha.equalsIgnoreCase(safe(sha256).trim())) {
            return WashedImageParseResult.rejected("washed image sha mismatch");
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return WashedImageParseResult.rejected("washed image payload must decode as PNG");
            }
            try {
                if (image.getWidth() != width || image.getHeight() != height) {
                    return WashedImageParseResult.rejected("washed image decoded size mismatch");
                }
            } finally {
                image.flush();
            }
        } catch (Exception e) {
            return WashedImageParseResult.rejected("washed image payload must decode as PNG");
        }
        return WashedImageParseResult.accepted(new WashedImage(
                payloadBase64.trim(), "image/png", safe(sha256).trim().toLowerCase(Locale.ROOT), width, height));
    }

    private static List<ImagePreprocessCloudDecision.CandidateBox> toWindowRelativeBoxes(
            ImagePreprocessCloudRequest request,
            String coordinateSpace,
            List<ImagePreprocessCloudDecision.CandidateBox> boxes) {
        if (!COORDINATE_SPACE_ROI_RELATIVE.equals(coordinateSpace) || boxes.isEmpty()) {
            return boxes;
        }
        ImagePreprocessCloudRequest.Roi roi = request.getRoi();
        List<ImagePreprocessCloudDecision.CandidateBox> converted = new ArrayList<>();
        for (ImagePreprocessCloudDecision.CandidateBox box : boxes) {
            converted.add(new ImagePreprocessCloudDecision.CandidateBox(
                    roi.getX() + box.x(), roi.getY() + box.y(), box.width(), box.height()));
        }
        return converted;
    }

    private static List<ImagePreprocessCloudDecision.CandidatePoint> toWindowRelativePoints(
            ImagePreprocessCloudRequest request,
            String coordinateSpace,
            List<ImagePreprocessCloudDecision.CandidatePoint> points) {
        if (!COORDINATE_SPACE_ROI_RELATIVE.equals(coordinateSpace) || points.isEmpty()) {
            return points;
        }
        ImagePreprocessCloudRequest.Roi roi = request.getRoi();
        List<ImagePreprocessCloudDecision.CandidatePoint> converted = new ArrayList<>();
        for (ImagePreprocessCloudDecision.CandidatePoint point : points) {
            converted.add(new ImagePreprocessCloudDecision.CandidatePoint(
                    roi.getX() + point.x(), roi.getY() + point.y()));
        }
        return converted;
    }

    private static String validateBox(
            ImagePreprocessCloudRequest request,
            String coordinateSpace,
            ImagePreprocessCloudDecision.CandidateBox box) {
        if (box.width() <= 0 || box.height() <= 0) {
            return "candidate box must have positive size: box=" + boxText(box);
        }
        if (COORDINATE_SPACE_ROI_RELATIVE.equals(coordinateSpace)) {
            ImagePreprocessCloudRequest.Roi roi = request.getRoi();
            if (roi == null || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
                return "ROI_RELATIVE candidates require a positive ROI";
            }
            if (box.x() < 0 || box.y() < 0
                    || box.x() + box.width() > roi.getWidth()
                    || box.y() + box.height() > roi.getHeight()) {
                return "candidate box outside ROI: box=" + boxText(box)
                        + " roi=0,0," + roi.getWidth() + "," + roi.getHeight();
            }
            return null;
        }
        if (!insideWindow(request, box.x(), box.y())
                || !insideWindow(request, box.x() + box.width() - 1, box.y() + box.height() - 1)) {
            return "candidate box outside window: box=" + boxText(box)
                    + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
        }
        return null;
    }

    private static String validatePoint(
            ImagePreprocessCloudRequest request,
            String coordinateSpace,
            ImagePreprocessCloudDecision.CandidatePoint point) {
        if (COORDINATE_SPACE_ROI_RELATIVE.equals(coordinateSpace)) {
            ImagePreprocessCloudRequest.Roi roi = request.getRoi();
            if (roi == null || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
                return "ROI_RELATIVE candidates require a positive ROI";
            }
            if (point.x() < 0 || point.x() >= roi.getWidth()
                    || point.y() < 0 || point.y() >= roi.getHeight()) {
                return "candidate point outside ROI: point=" + pointText(point)
                        + " roi=0,0," + roi.getWidth() + "," + roi.getHeight();
            }
            return null;
        }
        if (!insideWindow(request, point.x(), point.y())) {
            return "candidate point outside window: point=" + pointText(point)
                    + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
        }
        return null;
    }

    private static boolean insideWindow(ImagePreprocessCloudRequest request, int x, int y) {
        return x >= 0
                && y >= 0
                && x < request.getWindowWidth()
                && y < request.getWindowHeight();
    }

    private static Map<String, String> context(ImagePreprocessCloudRequest request) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("hook", "image-preprocess-execute");
        context.put("operation", request.getOperation().name());
        context.put("returnMode", returnMode(request.getOperation()));
        context.put("imagePayloadBase64", request.getImagePayloadBase64().trim());
        context.put("payloadMimeType", request.getPayloadMimeType().trim());
        context.put("imageSha256", request.getImageSha256().trim());
        context.put("rawImagePath", safe(request.getRawImagePath()));
        context.put("debugImageId", safe(request.getDebugImageId()));
        context.put("source", safe(request.getSource()));
        context.put("phase", normalize(request.getPhase(), DEFAULT_PHASE));
        context.put("taskCode", normalize(request.getTaskCode(), DEFAULT_TASK_CODE));
        context.put("windowId", safe(request.getWindowId()));
        context.put("hwnd", safe(request.getHwnd()));
        context.put("windowSize", request.getWindowWidth() + "," + request.getWindowHeight());
        context.put("roi", roiText(request.getRoi()));
        if (request.getParameters() != null) {
            for (Map.Entry<String, String> entry : request.getParameters().entrySet()) {
                if (hasText(entry.getKey())) {
                    context.put("param." + entry.getKey().trim(), safe(entry.getValue()));
                }
            }
        }
        return Map.copyOf(context);
    }

    private static String requestValidationError(ImagePreprocessCloudRequest request) {
        if (!hasText(request.getImagePayloadBase64())) {
            return "missing transferable image payload: imagePayloadBase64 is required";
        }
        if (!hasText(request.getPayloadMimeType())) {
            return "missing transferable image payload metadata: payloadMimeType is required";
        }
        if (!hasText(request.getImageSha256())) {
            return "missing transferable image payload metadata: imageSha256 is required";
        }
        if (request.getWindowWidth() <= 0 || request.getWindowHeight() <= 0) {
            return "window size must be explicit positive pixels";
        }
        ImagePreprocessCloudRequest.Roi roi = request.getRoi();
        if (roi == null) {
            return null;
        }
        if (roi.getX() < 0 || roi.getY() < 0 || roi.getWidth() <= 0 || roi.getHeight() <= 0) {
            return "ROI must be window-relative pixels with non-negative origin and positive size";
        }
        long maxX = (long) roi.getX() + roi.getWidth();
        long maxY = (long) roi.getY() + roi.getHeight();
        if (maxX > request.getWindowWidth() || maxY > request.getWindowHeight()) {
            return "ROI outside window: roi=" + roiText(roi)
                    + " window=0,0," + request.getWindowWidth() + "," + request.getWindowHeight();
        }
        return null;
    }

    private static String returnMode(ImagePreprocessOperation operation) {
        return switch (operation) {
            case WASH_YELLOW, WASH_GREEN, WASH_WHITE, WASH_PURPLE, WASH_DIALOG_OPTION_TEMPLATE,
                    WASH_AUTO_COMBAT_ROUND_RED_DIGITS, ROUTE_PACKED_LINE_MASK ->
                    RETURN_WASHED_IMAGE;
            case FIND_GREEN_TEXT_BANDS, PICK_GREEN_TEXT_BAND, TEXT_CANDIDATES, ROUTE_DESTINATION_SEGMENTS ->
                    RETURN_CANDIDATES;
            case FINGERPRINT, COUNT_YELLOW_PIXELS, COUNT_GREEN_PIXELS_HSV, COUNT_THIN_WHITE_PIXELS_HSV,
                    BUILD_BINARY_FINGERPRINT, BINARY_FINGERPRINT_DISTANCE,
                    DETECT_THIN_WHITE_TEXT_LINE_PATTERN, MEASURE_STDDEV, MEASURE_TEAM_TOOLTIP_TEXT ->
                    RETURN_RESULT_VALUES;
        };
    }

    private static ImagePreprocessCloudDecision requiredFailure(
            ImagePreprocessOperation operation,
            CloudDecisionResult cloudResult,
            String reason,
            List<ImagePreprocessCloudDecision.CandidateBox> candidateBoxes,
            List<ImagePreprocessCloudDecision.CandidatePoint> candidatePoints) {
        return ImagePreprocessCloudDecision.builder()
                .status(ImagePreprocessCloudDecision.Status.REQUIRED_FAILURE)
                .operation(operation)
                .cloudResult(cloudResult)
                .reason(safe(reason))
                .candidateBoxes(List.copyOf(candidateBoxes))
                .candidatePoints(List.copyOf(candidatePoints))
                .build();
    }

    private static List<ImagePreprocessCloudDecision.CandidateBox> parseBoxes(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        List<ImagePreprocessCloudDecision.CandidateBox> boxes = new ArrayList<>();
        for (String item : value.split("\\|", -1)) {
            if (!hasText(item)) {
                continue;
            }
            int[] parts = parseIntParts(item, 4);
            if (parts == null) {
                return null;
            }
            boxes.add(new ImagePreprocessCloudDecision.CandidateBox(parts[0], parts[1], parts[2], parts[3]));
        }
        return boxes;
    }

    private static List<ImagePreprocessCloudDecision.CandidatePoint> parsePoints(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        List<ImagePreprocessCloudDecision.CandidatePoint> points = new ArrayList<>();
        for (String item : value.split("\\|", -1)) {
            if (!hasText(item)) {
                continue;
            }
            int[] parts = parseIntParts(item, 2);
            if (parts == null) {
                return null;
            }
            points.add(new ImagePreprocessCloudDecision.CandidatePoint(parts[0], parts[1]));
        }
        return points;
    }

    private static int[] parseIntParts(String value, int expectedLength) {
        String[] rawParts = value.split(",", -1);
        if (rawParts.length != expectedLength) {
            return null;
        }
        int[] parts = new int[expectedLength];
        try {
            for (int i = 0; i < expectedLength; i++) {
                parts[i] = Integer.parseInt(rawParts[i].trim());
            }
            return parts;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parsePositiveInt(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
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
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!hasText(decision)) {
            return result;
        }
        String[] parts = decision.split(";", -1);
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = part.substring(0, separator).trim();
            if (!key.isEmpty()) {
                result.put(key, part.substring(separator + 1).trim());
            }
        }
        return result;
    }

    private static String debugToken(CloudDecisionResponse response) {
        return response.getDiagnostics() == null ? null : response.getDiagnostics().get("debugToken");
    }

    private static Map<String, String> resultValues(CloudDecisionResponse response) {
        if (response == null || response.getDiagnostics() == null || response.getDiagnostics().isEmpty()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>(response.getDiagnostics());
        values.remove("washedImagePayloadBase64");
        values.remove("candidateBoxes");
        values.remove("candidatePoints");
        values.remove(COORDINATE_SPACE_KEY);
        values.remove("debugToken");
        return Map.copyOf(values);
    }

    private static String traceId(ImagePreprocessCloudRequest request) {
        return "image-preprocess:"
                + safeTracePart(request.getTaskCode())
                + ":" + safeTracePart(request.getPhase())
                + ":" + request.getOperation().name()
                + ":" + safeTracePart(firstText(request.getDebugImageId(), request.getRawImagePath()));
    }

    private static String roiText(ImagePreprocessCloudRequest.Roi roi) {
        if (roi == null) {
            return "";
        }
        return roi.getX() + "," + roi.getY() + "," + roi.getWidth() + "," + roi.getHeight();
    }

    private static String boxText(ImagePreprocessCloudDecision.CandidateBox box) {
        return box.x() + "," + box.y() + "," + box.width() + "," + box.height();
    }

    private static String pointText(ImagePreprocessCloudDecision.CandidatePoint point) {
        return point.x() + "," + point.y();
    }

    private static String normalize(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private static String upper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String firstText(String first, String second) {
        return hasText(first) ? first : safe(second);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safeTracePart(String value) {
        return normalize(value, "unknown").replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private record CoordinateParseResult(
            boolean accepted,
            List<ImagePreprocessCloudDecision.CandidateBox> boxes,
            List<ImagePreprocessCloudDecision.CandidatePoint> points,
            String reason) {
        static CoordinateParseResult accepted(
                List<ImagePreprocessCloudDecision.CandidateBox> boxes,
                List<ImagePreprocessCloudDecision.CandidatePoint> points) {
            return new CoordinateParseResult(true, List.copyOf(boxes), List.copyOf(points), null);
        }

        static CoordinateParseResult rejected(String reason) {
            return new CoordinateParseResult(false, List.of(), List.of(), reason);
        }
    }

    private static final class WashedImageHolder {
        private WashedImage value;

        private WashedImage value() {
            return value;
        }

        private void set(WashedImage value) {
            this.value = value;
        }
    }

    private record WashedImage(
            String payloadBase64,
            String mimeType,
            String sha256,
            int width,
            int height) {
    }

    private record WashedImageParseResult(boolean accepted, WashedImage image, String reason) {
        static WashedImageParseResult empty() {
            return new WashedImageParseResult(true, null, null);
        }

        static WashedImageParseResult accepted(WashedImage image) {
            return new WashedImageParseResult(true, image, null);
        }

        static WashedImageParseResult rejected(String reason) {
            return new WashedImageParseResult(false, null, reason);
        }
    }
}
