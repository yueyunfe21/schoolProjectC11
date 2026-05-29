package com.bot.dhxy.model.dialog;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

import java.awt.image.BufferedImage;

/**
 * Snapshot result for one dialog-frame detection pass.
 *
 * @param type detected dialog type; {@link DialogType#NONE} means no known dialog shape was
 *             confirmed.
 * @param dialogRect screen-absolute rectangle captured for detection, formatted as
 *                   {@code [left, top, right, bottom]}; null when no screenshot was available.
 * @param rawPath optional debug path for the captured raw dialog image.
 * @param image in-memory dialog screenshot owned by the caller; null when capture failed.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class DialogDetection {
    DialogType type;
    int[] dialogRect;
    String rawPath;
    BufferedImage image;

    public static DialogDetection none() {
        return DialogDetection.builder()
                .type(DialogType.NONE)
                .build();
    }

    public DialogDetection withType(DialogType newType) {
        return DialogDetection.builder()
                .type(newType)
                .dialogRect(dialogRect)
                .rawPath(rawPath)
                .image(image)
                .build();
    }
}
