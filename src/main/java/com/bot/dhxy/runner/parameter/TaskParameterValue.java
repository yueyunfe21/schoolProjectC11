package com.bot.dhxy.runner.parameter;

import lombok.Builder;
import lombok.Getter;

/**
 * 单个任务参数值。
 */
@Getter
@Builder
public class TaskParameterValue {

    private final String taskCode;
    private final String key;
    private final String value;

    public boolean asBoolean(boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public int asInt(int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long asLong(long defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String asString(String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
