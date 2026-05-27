package com.bot.dhxy.task;

import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.model.navigation.ObjectiveTextResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.service.DialogService;
import com.bot.dhxy.vision.ObjectiveTextRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Optional;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DebugXiuluoStoryObjectiveTask implements GameTask {

    private final DialogService dialogService;
    private final ObjectiveTextRecognitionService objectiveTextRecognitionService;

    @Override
    public String getTaskCode() {
        return "debug_xiuluo_story_objective";
    }

    @Override
    public String getTaskName() {
        return "\u4fee\u7f57Story\u76ee\u6807\u6d4b\u8bd5";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        String prefix = context == null ? "[window=unknown]" : context.getLogPrefix();
        long startedAt = System.currentTimeMillis();
        log.info("{} [debug-xiuluo-story-objective] start: current screen must already show Xiuluo accept story dialog", prefix);

        BufferedImage storyImage = dialogService.captureCurrentStoryImage("debug-xiuluo-story-objective");
        if (storyImage == null) {
            log.warn("{} ❌ [XIULUO_STORY_OBJECTIVE][MISS] reason=not_story_or_capture_failed elapsedMs={}",
                    prefix, System.currentTimeMillis() - startedAt);
            return TaskRunResult.FAILED;
        }

        try {
            Optional<ObjectiveTextResult> result =
                    objectiveTextRecognitionService.recognize(storyImage, "debug-xiuluo-story-objective");
            if (result.isEmpty()) {
                log.warn("{} ❌ [XIULUO_STORY_OBJECTIVE][MISS] reason=template_not_matched elapsedMs={}",
                        prefix, System.currentTimeMillis() - startedAt);
                return TaskRunResult.FAILED;
            }

            ObjectiveTextResult value = result.get();
            log.info("{} ✅ [XIULUO_STORY_OBJECTIVE][OK] map={} slug={} coord=({}, {}) score={} elapsedMs={}",
                    prefix, value.mapName(), value.mapSlug(), value.x(), value.y(), value.mapScore(),
                    System.currentTimeMillis() - startedAt);
            return TaskRunResult.SUCCESS;
        } finally {
            storyImage.flush();
        }
    }

    @Override
    public void stop() {
        // One-shot debug task; nothing to stop.
    }
}
