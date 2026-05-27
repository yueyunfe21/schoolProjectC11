package com.bot.dhxy.model.quest;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

/**
 * Captured task-detail panel image used by objective recognizers and debug probes.
 *
 * @param image captured detail-panel image; caller owns and should flush it after use.
 * @param imagePath debug image path written during capture, or blank when unavailable.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class QuestDetailCapture {
    BufferedImage image;
    String imagePath;

    /**
     * @return an empty failed capture result.
     */
    public static QuestDetailCapture empty() {
        return new QuestDetailCapture(null, "");
    }

    /**
     * @return true when a usable image was captured.
     */
    public boolean hasImage() {
        return image != null;
    }

}
