package com.bot.dhxy.runner.parameter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Task parameter registry service.
 *
 * Stores parameter schemas by task code.
 */
@Component
public class TaskParameterService {

    private final Map<String, TaskParameterSchema> schemaMap = new HashMap<>();

    public Optional<TaskParameterSchema> getSchema(String taskCode) {
        if (taskCode == null || taskCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(schemaMap.get(taskCode.trim()));
    }

    public List<TaskParameterSchema> getSchemas(List<String> taskCodes) {
        if (taskCodes == null || taskCodes.isEmpty()) {
            return List.of();
        }
        return taskCodes.stream()
                .map(this::getSchema)
                .flatMap(Optional::stream)
                .toList();
    }

    public void registerSchema(TaskParameterSchema schema) {
        if (schema == null || schema.getTaskCode() == null || schema.getTaskCode().isBlank()) {
            return;
        }
        schemaMap.put(schema.getTaskCode().trim(), schema);
    }

    public void clearSchemas() {
        schemaMap.clear();
    }
}
