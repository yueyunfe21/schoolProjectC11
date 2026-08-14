package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskRuntimeSettings;
import com.bot.dhxy.config.BotProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class G064DoubleExperienceClaimSwitchConnectivityContract {

    public static void main(String[] args) throws Exception {
        BotProperties properties = new BotProperties();
        require(properties.isDoubleExperienceClaimEnabled(), "the UI-backed setting must default to enabled");
        properties.setDoubleExperienceClaimEnabled(false);
        TurnTaskRuntimeSettings runtimeSettings =
                WindowTaskControlService.buildRuntimeSettingsSnapshot(properties);
        require(!runtimeSettings.doubleExperienceClaimEnabled(),
                "the disabled UI value must enter the exact remote-start snapshot");

        String ui = read("src/main/java/com/bot/dhxy/ui/MainWindowController.java");
        String store = read("src/main/java/com/bot/dhxy/ui/GameUiSettingsStore.java");
        String snapshot = read("src/main/java/com/bot/dhxy/window/control/WindowTaskControlService.java");
        String protocol = read("src/main/java/com/bot/dhxy/cloud/turn/protocol/TurnTaskRuntimeSettings.java");
        String ghostKing = read("../dhxy-cloud-brain/src/main/java/com/bot/dhxy/task/guiwang/GhostKingTask.java");
        String playerState = read("../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/PlayerStateService.java");
        String maintenance = read("../dhxy-cloud-brain/src/main/java/com/bot/dhxy/service/TaskMaintenanceService.java");

        require(ui.contains("new CheckBox(\"领取双倍\")"), "UI switch is missing");
        require(store.contains("DOUBLE_EXPERIENCE_CLAIM_ENABLED"), "UI persistence is missing");
        require(snapshot.contains("botProperties.isDoubleExperienceClaimEnabled()"),
                "remote-start snapshot is missing the UI value");
        require(protocol.contains("boolean doubleExperienceClaimEnabled"),
                "shared runtime protocol is missing the switch");
        require(ghostKing.contains(
                "beginDoubleExperienceObservation(runtimeSettings.doubleExperienceClaimEnabled())"),
                "GhostKing must bind observation to the exact-run switch");
        require(ghostKing.contains("if (!runtimeSettings.doubleExperienceClaimEnabled())"),
                "GhostKing must reject stale pending work while disabled");

        int observationGate = playerState.indexOf("if (!shouldObserveDoubleExperience(state, now))");
        int recognizer = playerState.indexOf(
                "IncenseStatusRecognizer.isDoubleExperienceRemainingBelowTwentyMinutes(statusImage)",
                observationGate);
        require(observationGate >= 0 && recognizer > observationGate,
                "the disabled observation gate must run before double-experience recognition");

        int runtimeGate = maintenance.indexOf(
                "!context.getRuntimeSettings().doubleExperienceClaimEnabled()");
        int navigation = maintenance.indexOf("navigationService.navigateViaWorldMapLabel", runtimeGate);
        require(runtimeGate >= 0 && navigation > runtimeGate,
                "normal disabled claim must return before Chang'an navigation");
        require(maintenance.contains(
                "triggerPolicy != DoubleExperienceTriggerPolicy.FORCE_FOR_MANUAL_ACCEPTANCE"),
                "the explicit manual G056 acceptance must remain available");
        System.out.println("G064 double-experience switch connectivity: PASS");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
