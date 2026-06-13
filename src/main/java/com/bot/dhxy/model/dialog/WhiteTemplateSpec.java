package com.bot.dhxy.model.dialog;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * White story-template descriptor inside the standard dialog rectangle.
 *
 * @param name action key returned when the template is visible.
 * @param templatePath template image path under the project image folder.
 */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Accessors(fluent = true)
public class WhiteTemplateSpec {
    String name;
    String templatePath;
}
