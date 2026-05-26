package com.bot.dhxy.task;

import com.bot.dhxy.core.TextRecognizer;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import com.bot.dhxy.service.QuestManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DebugXiuluoTaskPanelObjectiveTask implements GameTask {

    private static final String TASK_CODE = "xiuluo";
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "([\\p{IsHan}]+)[\\(\\[（]\\s*(\\d{1,3})\\s*[,，]\\s*(\\d{1,3})\\s*[\\)\\]）]");

    private final QuestManagerService questManagerService;
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;
    private final TextRecognizer ocr;

    @Override
    public String getTaskCode() {
        return "debug_xiuluo_task_panel_objective";
    }

    @Override
    public String getTaskName() {
        return "\u4fee\u7f57\u4efb\u52a1\u680f\u76ee\u6807\u6d4b\u8bd5";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        String prefix = context == null ? "[window=unknown]" : context.getLogPrefix();
        long startedAt = System.currentTimeMillis();
        log.info("{} [debug-xiuluo-task-panel-objective] start: will open Alt+Q once, activate Xiuluo, capture detail panel, then run template+OCR probes", prefix);

        QuestManagerService.QuestDetailCapture capture = questManagerService.captureCurrentQuestDetailForTask(TASK_CODE);
        try {
            Optional<ObjectiveTextRecognitionService.ObjectiveTextResult> templateResult = runTemplateProbe(prefix, capture);
            OcrProbeResult ocrResult = runOcrProbe(prefix, capture);
            if (templateResult.isPresent()) {
                ObjectiveTextRecognitionService.ObjectiveTextResult value = templateResult.get();
                log.info("{} ✅ [XIULUO_TASK_PANEL_OBJECTIVE][OK] template=OK map={} slug={} coord=({}, {}) score={} ocr={} elapsedMs={}",
                        prefix, value.mapName(), value.mapSlug(), value.x(), value.y(), value.mapScore(),
                        ocrResult.toSummary(), System.currentTimeMillis() - startedAt);
                return TaskRunResult.SUCCESS;
            }
            if (ocrResult.hit()) {
                log.info("{} ✅ [XIULUO_TASK_PANEL_OBJECTIVE][OK] template=MISS ocr={} elapsedMs={}",
                        prefix, ocrResult.toSummary(), System.currentTimeMillis() - startedAt);
                return TaskRunResult.SUCCESS;
            }
            log.warn("{} ❌ [XIULUO_TASK_PANEL_OBJECTIVE][MISS] template=MISS ocr={} elapsedMs={}",
                    prefix, ocrResult.toSummary(), System.currentTimeMillis() - startedAt);
            return TaskRunResult.FAILED;
        } finally {
            release(capture);
        }
    }

    private Optional<ObjectiveTextRecognitionService.ObjectiveTextResult> runTemplateProbe(
            String prefix, QuestManagerService.QuestDetailCapture capture) {
        BufferedImage detailImage = capture.image();
        if (detailImage == null) {
            log.warn("{} [debug-xiuluo-task-panel-objective] TEMPLATE_MISS: detail capture failed or Xiuluo task not found", prefix);
            return Optional.empty();
        }

        Optional<ObjectiveTextRecognitionService.ObjectiveTextResult> result =
                objectiveTextRecognitionService.recognize(detailImage, "debug-xiuluo-task-panel-objective");
        if (result.isEmpty()) {
            log.warn("{} [debug-xiuluo-task-panel-objective] TEMPLATE_MISS", prefix);
            return Optional.empty();
        }

        ObjectiveTextRecognitionService.ObjectiveTextResult value = result.get();
        log.info("{} [debug-xiuluo-task-panel-objective] TEMPLATE_HIT map={} slug={} coord=({}, {}) score={} source={}",
                prefix, value.mapName(), value.mapSlug(), value.x(), value.y(), value.mapScore(), value.source());
        return result;
    }

    private OcrProbeResult runOcrProbe(String prefix, QuestManagerService.QuestDetailCapture capture) {
        String imagePath = capture.imagePath();
        if (imagePath == null || imagePath.isBlank()) {
            log.warn("{} [debug-xiuluo-task-panel-objective] OCR_MISS: empty image path", prefix);
            return OcrProbeResult.miss("empty_image_path");
        }

        java.util.List<TextRecognizer.OcrWordResult> results = ocr.getAllTextResults(imagePath);
        if (results == null || results.isEmpty()) {
            log.warn("{} [debug-xiuluo-task-panel-objective] OCR_MISS: empty text", prefix);
            return OcrProbeResult.miss("empty");
        }

        StringBuilder fullText = new StringBuilder();
        for (TextRecognizer.OcrWordResult word : results) {
            if (word.getText() != null) {
                fullText.append(word.getText());
            }
        }
        String text = fullText.toString();
        if (text.isBlank()) {
            log.warn("{} [debug-xiuluo-task-panel-objective] OCR_MISS: empty text", prefix);
            return OcrProbeResult.miss("empty");
        }

        String normalized = text.replaceAll("\\s+", "");
        Matcher matcher = COORD_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            log.warn("{} [debug-xiuluo-task-panel-objective] OCR_MISS raw={}", prefix, normalized);
            return OcrProbeResult.miss(normalized);
        }

        log.info("{} [debug-xiuluo-task-panel-objective] OCR_HIT mapCandidate={} coord=({}, {}) raw={}",
                prefix, matcher.group(1), matcher.group(2), matcher.group(3), normalized);
        return OcrProbeResult.hit(matcher.group(1), matcher.group(2), matcher.group(3), normalized);
    }

    @Override
    public void stop() {
        // One-shot debug task; nothing to stop.
    }

    private void release(QuestManagerService.QuestDetailCapture capture) {
        if (capture != null && capture.image() != null) {
            capture.image().flush();
        }
    }

    private record OcrProbeResult(boolean hit, String mapCandidate, String x, String y, String raw) {
        private static OcrProbeResult hit(String mapCandidate, String x, String y, String raw) {
            return new OcrProbeResult(true, mapCandidate, x, y, raw);
        }

        private static OcrProbeResult miss(String raw) {
            return new OcrProbeResult(false, "", "", "", raw);
        }

        private String toSummary() {
            if (!hit) {
                return "MISS raw=" + raw;
            }
            return "OK mapCandidate=" + mapCandidate + " coord=(" + x + "," + y + ")";
        }
    }
}
