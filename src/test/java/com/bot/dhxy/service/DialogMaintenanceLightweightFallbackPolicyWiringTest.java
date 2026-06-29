package com.bot.dhxy.service;

import com.bot.dhxy.model.maintenance.TaskMaintenanceRequest;
import com.bot.dhxy.service.dialog.DialogHandleRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DialogMaintenanceLightweightFallbackPolicyWiringTest {

    public static void main(String[] args) throws Exception {
        String requestModel = read("src/main/java/com/bot/dhxy/model/maintenance/TaskMaintenanceRequest.java");
        String dialogRequest = read("src/main/java/com/bot/dhxy/service/dialog/DialogHandleRequest.java");
        String maintenanceService = read("src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java");
        String autoBattleTask = read("src/main/java/com/bot/dhxy/task/AutoBattleTask.java");
        String dialogService = read("src/main/java/com/bot/dhxy/service/DialogService.java");

        assertContains(requestModel, "boolean allowFullMaintenanceBroadcastFallback",
                "TaskMaintenanceRequest must carry the CR78 explicit fallback policy");
        assertContains(dialogRequest, "boolean allowFullMaintenanceBroadcastFallback",
                "DialogHandleRequest must carry the CR78 explicit fallback policy");
        assertContains(autoBattleTask, ".allowFullMaintenanceBroadcastFallback(false)",
                "auto-battle lightweight idle maintenance must disable full dialog fallback");
        assertContains(maintenanceService, "DialogHandleRequest.handleMaintenanceBroadcastOption(",
                "TaskMaintenanceService must call the maintenance broadcast factory");
        assertContains(maintenanceService, "safeRequest.isAllowFullMaintenanceBroadcastFallback()",
                "TaskMaintenanceService must propagate the request policy into DialogHandleRequest");
        assertContains(dialogService, "if (!request.isAllowFullMaintenanceBroadcastFallback())",
                "DialogService must return before detectDialogSnapshotDirect when fallback is disabled");
        assertContains(dialogService, "maintenance broadcast lightweight fallback disabled",
                "DialogService must log the CR78 no-fallback skip reason");

        assertTrue("formal maintenance request must default to full fallback",
                TaskMaintenanceRequest.builder().sourceTask("formal").build()
                        .isAllowFullMaintenanceBroadcastFallback());
        assertTrue("formal dialog request must default to full fallback",
                DialogHandleRequest.handleMaintenanceBroadcastOption("formal")
                        .isAllowFullMaintenanceBroadcastFallback());
        assertFalse("explicit lightweight dialog request must disable full fallback",
                DialogHandleRequest.handleMaintenanceBroadcastOption("auto-battle", false)
                        .isAllowFullMaintenanceBroadcastFallback());
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + "; missing: " + needle);
        }
    }

    private static void assertTrue(String label, boolean value) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertFalse(String label, boolean value) {
        if (value) {
            throw new AssertionError(label + " expected false");
        }
    }
}
