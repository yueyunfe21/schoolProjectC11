package com.bot.dhxy.model.tasktracker;

import lombok.Builder;
import lombok.Value;

import java.awt.Point;

/**
 * A clickable green text segment in the left task-tracker panel.
 *
 * @param minX screen-absolute left edge of the segment, in pixels.
 * @param minY screen-absolute top edge of the segment, in pixels.
 * @param maxX screen-absolute right edge of the segment, in pixels.
 * @param maxY screen-absolute bottom edge of the segment, in pixels.
 * @param pixels number of washed green pixels inside this segment.
 * @param targetMapName map name parsed from this green link text, or blank when not recognized.
 * @param targetMapScore confidence-like diagnostic score for {@code targetMapName}.
 * @param targetMapDebugPath optional debug image used for map-name OCR.
 * @param sourceType reader that produced this clickable link.
 */
@Value
@Builder
public class TaskTrackerGreenLink {
    int minX;
    int minY;
    int maxX;
    int maxY;
    int pixels;
    String targetMapName;
    double targetMapScore;
    String targetMapDebugPath;
    @Builder.Default
    TaskTrackerPanelSourceType sourceType = TaskTrackerPanelSourceType.LOCAL;

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public Point centerPoint() {
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int pixels() {
        return pixels;
    }
}
