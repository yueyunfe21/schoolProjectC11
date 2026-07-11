package com.bot.dhxy.model.tasktracker;

import com.bot.dhxy.model.ocr.OcrWindowRegion;
import lombok.Builder;
import lombok.Value;

import java.awt.Point;

/**
 * Window-scoped cache for a successfully parsed task-tracker panel.
 *
 * @param taskCode task family that owns the cache, such as {@code wuhuan}; callers must not reuse it
 *                 across task types.
 * @param panelFingerprint lightweight signature of the current tracker panel crop.
 * @param clickWindowRelative green-link click point in game-window-relative pixels; never store a
 *                            desktop absolute point as reusable truth.
 * @param panelOriginWindowX tracker panel crop origin X in game-window-relative pixels.
 * @param panelOriginWindowY tracker panel crop origin Y in game-window-relative pixels.
 * @param panelWidth tracker panel crop width in pixels.
 * @param panelHeight tracker panel crop height in pixels.
 * @param trackerPanelRegion tracker panel crop in window-relative pixels.
 * @param wuhuanTrackerBlockRegion 五环 task block ROI in window-relative pixels, produced from the
 *                                 same title/task-block read that refreshed this cache.
 * @param updatedAtMs epoch millis when the cache was refreshed.
 * @param source diagnostic source that refreshed the cache.
 */
@Value
@Builder(toBuilder = true)
public class TaskTrackerPanelCacheEntry {
    String taskCode;
    String panelFingerprint;
    Point clickWindowRelative;
    int panelOriginWindowX;
    int panelOriginWindowY;
    int panelWidth;
    int panelHeight;
    OcrWindowRegion trackerPanelRegion;
    OcrWindowRegion wuhuanTrackerBlockRegion;
    long updatedAtMs;
    String source;

    public Point clickWindowRelative() {
        return clickWindowRelative == null ? null : new Point(clickWindowRelative);
    }
}
