package com.bot.dhxy.model.tasktracker;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Result of reading one task block from the left task-tracker panel.
 *
 * @param found true when a known task title was matched and the task block was cropped.
 * @param titleTemplate matched task title template; null when {@code found=false}.
 * @param detailRawPath window-scoped raw cropped task block image path.
 * @param detailYellowPath window-scoped yellow-washed task block image path used for OCR.
 * @param yellowText OCR text read from yellow title/content in the cropped task block.
 * @param greenLinks clickable green text segments in screen-absolute pixels.
 * @param greenBandWidth width of the selected green text band, in pixels.
 * @param probeObjective true when the green links look like the two-link 显形镜 style.
 */
@Value
@Builder
public class TaskTrackerPanelReadResult {
    boolean found;
    TaskTrackerTitleTemplate titleTemplate;
    String detailRawPath;
    String detailYellowPath;
    String yellowText;

    @Builder.Default
    List<TaskTrackerGreenLink> greenLinks = List.of();

    int greenBandWidth;
    boolean probeObjective;

    public static TaskTrackerPanelReadResult empty() {
        return TaskTrackerPanelReadResult.builder()
                .found(false)
                .yellowText("")
                .greenLinks(List.of())
                .greenBandWidth(0)
                .probeObjective(false)
                .build();
    }
}
