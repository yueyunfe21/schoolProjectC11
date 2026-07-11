package com.bot.dhxy.model.tasktracker;

import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.ocr.OcrWindowRegion;
import lombok.Builder;
import lombok.Value;

/**
 * Runner preparation result for a task-tracker read.
 *
 * @param preparedAction positive green-link action prepared by the Runner; nullable.
 * @param negativeResult fresh no-action tracker result from a successful read; nullable.
 * @param trackerPanelRegion tracker panel crop in window-relative pixels; nullable when unavailable.
 * @param wuhuanTrackerBlockRegion 五环 task block ROI in window-relative pixels; nullable when the
 *                                 current read did not positively locate the 五环 task block.
 */
@Value
@Builder
public class TaskTrackerPanelPrepareResult {
    PreparedDialogAction preparedAction;
    TaskTrackerPanelNegativeResult negativeResult;
    OcrWindowRegion trackerPanelRegion;
    OcrWindowRegion wuhuanTrackerBlockRegion;

    public static TaskTrackerPanelPrepareResult action(PreparedDialogAction action) {
        return action(action, null, null);
    }

    public static TaskTrackerPanelPrepareResult action(PreparedDialogAction action,
                                                       OcrWindowRegion trackerPanelRegion,
                                                       OcrWindowRegion wuhuanTrackerBlockRegion) {
        return TaskTrackerPanelPrepareResult.builder()
                .preparedAction(action)
                .trackerPanelRegion(trackerPanelRegion)
                .wuhuanTrackerBlockRegion(wuhuanTrackerBlockRegion)
                .build();
    }

    public static TaskTrackerPanelPrepareResult negative(TaskTrackerPanelNegativeResult negativeResult) {
        return negative(negativeResult, null, null);
    }

    public static TaskTrackerPanelPrepareResult negative(TaskTrackerPanelNegativeResult negativeResult,
                                                        OcrWindowRegion trackerPanelRegion,
                                                        OcrWindowRegion wuhuanTrackerBlockRegion) {
        return TaskTrackerPanelPrepareResult.builder()
                .negativeResult(negativeResult)
                .trackerPanelRegion(trackerPanelRegion)
                .wuhuanTrackerBlockRegion(wuhuanTrackerBlockRegion)
                .build();
    }

    public static TaskTrackerPanelPrepareResult empty() {
        return TaskTrackerPanelPrepareResult.builder().build();
    }

    public boolean hasAction() {
        return preparedAction != null;
    }

    public boolean hasNegative() {
        return negativeResult != null;
    }
}
