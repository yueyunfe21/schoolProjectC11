package com.bot.dhxy.task;

import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.TaskRunResult;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.vision.MiniMapCoordinateReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DebugCoordinateTask implements GameTask {

    private static final long SAMPLE_INTERVAL_MS = 1000;

    private final MiniMapCoordinateReader miniMapCoordinateReader;
    private volatile boolean stopped;

    public DebugCoordinateTask(MiniMapCoordinateReader miniMapCoordinateReader) {
        this.miniMapCoordinateReader = miniMapCoordinateReader;
    }

    @Override
    public String getTaskCode() {
        return "debug_coordinate";
    }

    @Override
    public String getTaskName() {
        return "坐标调试";
    }

    @Override
    public TaskRunResult execute() {
        return execute(null);
    }

    @Override
    public TaskRunResult execute(TaskExecutionContext context) {
        String prefix = context == null ? "[window=unknown]" : context.getLogPrefix();
        log.info("{} [坐标调试] 开始持续读取小地图坐标，间隔={}ms；需要结束时请停止该窗口任务", prefix, SAMPLE_INTERVAL_MS);

        int success = 0;
        int count = 0;
        while (true) {
            if (stopped || Thread.currentThread().isInterrupted()) {
                log.info("{} [坐标调试] 已停止，成功读取 {}/{}", prefix, success, count);
                return TaskRunResult.STOPPED;
            }
            if (context != null) {
                context.throwIfStopRequested();
            }

            count++;
            MiniMapCoordinateReader.MiniMapSnapshot snapshot = miniMapCoordinateReader.readCurrentLocationSnapshot();
            String mapLabelPath = snapshot.mapLabelPath() == null || snapshot.mapLabelPath().isBlank()
                    ? "-"
                    : snapshot.mapLabelPath();
            MapCoordinate coordinate = snapshot.coordinate();
            if (coordinate != null) {
                success++;
                log.info("{} [坐标调试] 第 {} 次：mapLabelPath={} coord={},{}",
                        prefix, count, mapLabelPath, coordinate.getX(), coordinate.getY());
            } else {
                log.info("{} [坐标调试] 第 {} 次：mapLabelPath={} coord=未识别", prefix, count, mapLabelPath);
            }

            sleep(context, SAMPLE_INTERVAL_MS);
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }

    private void sleep(TaskExecutionContext context, long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (context != null) {
            context.throwIfStopRequested();
        }
    }
}
