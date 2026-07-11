package com.bot.dhxy.cloud.task;

import com.bot.dhxy.cloud.decision.CloudDecisionCoordinator;
import com.bot.dhxy.cloud.decision.CloudDecisionExecutionGate;
import com.bot.dhxy.cloud.decision.CloudDecisionRequest;
import com.bot.dhxy.cloud.decision.CloudDecisionResponse;
import com.bot.dhxy.cloud.decision.CloudDecisionResult;
import com.bot.dhxy.cloud.decision.CloudDecisionServiceId;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * CR208-9: cloud-owned 修罗 objective story-text reader.
 *
 * <p>The client sends only the captured objective panel image; cloud-brain owns green washing, map
 * name/digit template matching, and the map-bounds plausibility guard, and answers with either the
 * recognized "前往 地图(x,y)" target or a deterministic no-result. No local OCR/template matching
 * happens on this path.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectiveTextReaderCloudDecisionService {

    private static final String LOCAL_SHADOW_DECISION = "status=LOCAL_SHADOW;reason=objective-text-cloud-required";
    private static final String DEFAULT_PHASE = "objective-story-text";

    private final CloudDecisionCoordinator coordinator;

    public boolean isActive() {
        return coordinator.isActive(CloudDecisionServiceId.OBJECTIVE_TEXT_READER);
    }

    /** CR247: quest-detail panel OCR reader activation, independent from the template reader. */
    public boolean isQuestDetailActive() {
        return coordinator.isActive(CloudDecisionServiceId.QUEST_DETAIL_READER);
    }

    /**
     * Recognize the objective map/coordinate from the panel image through the cloud reader.
     *
     * @param image objective panel crop; ownership stays with the caller.
     * @param taskCode owning task code such as {@code xiuluo_v2}; blank becomes {@code navigation}.
     * @param source diagnostic source label recorded on the result and in logs.
     * @return recognized objective, or empty on deterministic cloud no-result, transport failure,
     *         or disabled service. Callers keep their existing miss/recovery semantics.
     */
    public Optional<ObjectiveTextResult> read(BufferedImage image, String taskCode, String source) {
        return read(CloudDecisionServiceId.OBJECTIVE_TEXT_READER, DEFAULT_PHASE, image, taskCode, source);
    }

    /**
     * CR247 (CR208 items 5/6): recognize the "前往 地图(x,y)" objective from the Alt+Q quest-detail
     * panel through the cloud OCR reader. Deliberately a separate cloud service so the OCR path
     * stays mechanically independent from the template-matching story path.
     */
    public Optional<ObjectiveTextResult> readQuestDetail(BufferedImage image, String taskCode, String source) {
        return read(CloudDecisionServiceId.QUEST_DETAIL_READER, "quest-detail-ocr", image, taskCode, source);
    }

    private Optional<ObjectiveTextResult> read(CloudDecisionServiceId serviceId,
                                               String phase,
                                               BufferedImage image,
                                               String taskCode,
                                               String source) {
        if (image == null || !coordinator.isActive(serviceId)) {
            return Optional.empty();
        }
        byte[] pngBytes;
        try {
            pngBytes = pngBytes(image);
        } catch (IOException e) {
            log.warn("cloud objective text reader payload encode failed: taskCode={} source={} reason={}",
                    taskCode, source, e.getMessage());
            return Optional.empty();
        }
        String imageSha256 = sha256Hex(pngBytes);
        String normalizedTaskCode = taskCode == null || taskCode.isBlank() ? "navigation" : taskCode;
        Map<String, String> context = new LinkedHashMap<>();
        context.put("imagePayloadBase64", Base64.getEncoder().encodeToString(pngBytes));
        context.put("payloadMimeType", "image/png");
        context.put("imageSha256", imageSha256);
        context.put("source", source == null ? "" : source);
        context.put("phase", phase);
        context.put("windowSize", image.getWidth() + "," + image.getHeight());
        CloudDecisionRequest cloudRequest = CloudDecisionRequest.builder()
                .serviceId(serviceId)
                .traceId(serviceId.name().toLowerCase(Locale.ROOT).replace('_', '-')
                        + ":" + normalizedTaskCode + ":" + phase + ":" + imageSha256)
                .taskCode(normalizedTaskCode)
                .phase(phase)
                .localDecision(LOCAL_SHADOW_DECISION)
                .context(context)
                .build();
        ObjectiveTextResult[] parsedHolder = new ObjectiveTextResult[1];
        CloudDecisionResult cloudResult = coordinator.shadow(cloudRequest, LOCAL_SHADOW_DECISION,
                executionGate(serviceId, parsedHolder, source));
        if (cloudResult.isExecuted() && parsedHolder[0] != null) {
            return Optional.of(parsedHolder[0]);
        }
        log.info("cloud objective reader miss: serviceId={} taskCode={} source={} executed={} reason={}",
                serviceId, normalizedTaskCode, source, cloudResult.isExecuted(), cloudResult.getReason());
        return Optional.empty();
    }

    private CloudDecisionExecutionGate executionGate(CloudDecisionServiceId expectedServiceId,
                                                     ObjectiveTextResult[] parsedHolder,
                                                     String source) {
        return new CloudDecisionExecutionGate() {
            @Override
            public boolean allowsExecution(CloudDecisionServiceId serviceId) {
                return serviceId == expectedServiceId;
            }

            @Override
            public GateResult evaluate(CloudDecisionRequest request,
                                       CloudDecisionResponse response,
                                       String localDecision) {
                String decision = response == null ? null : response.getDecision();
                Map<String, String> fields = fields(decision);
                String status = fields.getOrDefault("status", "");
                if ("NO_RESULT".equalsIgnoreCase(status)) {
                    // Deterministic cloud no-result is an accepted decision; consumers keep their
                    // existing recognition-miss recovery semantics.
                    return GateResult.accepted(decision, "objective-text-no-result");
                }
                if (!"FOUND".equalsIgnoreCase(status)) {
                    return GateResult.rejected("unexpected objective-text status: " + status);
                }
                String mapName = fields.get("objectiveMap");
                Integer x = parseInt(fields.get("objectiveX"));
                Integer y = parseInt(fields.get("objectiveY"));
                if (mapName == null || mapName.isBlank() || x == null || y == null) {
                    return GateResult.rejected("objective-text FOUND missing map/x/y");
                }
                parsedHolder[0] = new ObjectiveTextResult(
                        fields.getOrDefault("objectiveSlug", ""),
                        mapName,
                        x,
                        y,
                        parseDouble(fields.get("score")),
                        source);
                return GateResult.accepted(decision, "objective-text-found");
            }
        };
    }

    private static Map<String, String> fields(String decision) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (decision == null || decision.isBlank()) {
            return fields;
        }
        for (String part : decision.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                fields.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return fields;
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }

    private static byte[] pngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder result = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
