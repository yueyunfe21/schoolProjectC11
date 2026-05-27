package com.bot.dhxy.model.dialog;

/**
 * Template-click descriptor for green option text inside the standard dialog rectangle.
 *
 * @param name diagnostic label written to logs.
 * @param templatePath template image path under the project image folder.
 * @param minOffsetX minimum randomized X offset from the template anchor, in screen pixels.
 * @param maxOffsetX maximum randomized X offset from the template anchor, in screen pixels.
 * @param randomRadiusY randomized Y radius from the template anchor, in screen pixels.
 */
public record GreenTemplateClickSpec(String name,
                                     String templatePath,
                                     int minOffsetX,
                                     int maxOffsetX,
                                     int randomRadiusY) {
}
