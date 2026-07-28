package com.bot.dhxy.model.dialog;

/**
 * Frozen 696a12b0 green-option template and click-offset policy.
 */
public record GreenTemplateClickSpec(
        String name,
        String templatePath,
        int minOffsetX,
        int maxOffsetX,
        int randomRadiusY) {
}
