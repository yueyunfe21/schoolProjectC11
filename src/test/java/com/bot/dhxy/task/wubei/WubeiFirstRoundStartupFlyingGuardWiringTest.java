package com.bot.dhxy.task.wubei;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for the first 五倍 post-accept prepath when startup already confirmed flying.
 *
 * <p>The startup Alt+U panel is already opened to inspect the expand option. That same panel must
 * record the current flying/mounted state so the first accepted 五倍 task does not press Alt+C again
 * and accidentally cancel an already-in-flight movement. The marker is first-round/one-shot only.</p>
 */
public final class WubeiFirstRoundStartupFlyingGuardWiringTest {

    private WubeiFirstRoundStartupFlyingGuardWiringTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String runtime = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/runtime/WindowRuntimeContext.java"), StandardCharsets.UTF_8);
        String startup = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/window/startup/TaskStartupWindowPreparationService.java"), StandardCharsets.UTF_8);
        String wubei = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/wubei/WubeiTask.java"), StandardCharsets.UTF_8);

        runtimeStoresOneShotStartupFlying(runtime);
        startupRecordsFlyingWhileAltUIsOpen(startup);
        firstWubeiRoundSkipsAltCOnlyWhenStartupFlying(wubei);

        System.out.println("WubeiFirstRoundStartupFlyingGuardWiringTest passed");
    }

    private static void runtimeStoresOneShotStartupFlying(String runtime) {
        require(runtime.contains("markTaskQueueStartupFlyingState("),
                "runtime must record startup flying state from the Alt+U probe");
        require(runtime.contains("consumeTaskQueueStartupFlyingState("),
                "runtime must expose one-shot consumption for the first task round");
        require(runtime.contains("taskQueueStartupFlyingState.getAndSet(null)"),
                "startup flying state must be consumed once, not reused by later rounds");
        require(runtime.contains("clearTaskQueueStartupFlyingState("),
                "runtime must be able to clear stale startup flying state");
    }

    private static void startupRecordsFlyingWhileAltUIsOpen(String startup) {
        String backgroundProbe = methodBody(startup, "private StartupProbeResult probeExpandOptionBackground(");
        String foregroundProbe = methodBody(startup, "private boolean ensureExpandOptionUncheckedDirect(");
        String record = methodBody(startup, "private void recordStartupFlyingStateFromOpenStatusPanel(");

        require(backgroundProbe.contains("recordStartupFlyingStateFromOpenStatusPanel(\"background-expand-probe\")"),
                "background Alt+U expand probe must also record flying state");
        require(foregroundProbe.contains("recordStartupFlyingStateFromOpenStatusPanel(\"foreground-expand-probe\")"),
                "foreground Alt+U expand probe must also record flying state");
        require(startup.contains("images/template/status/flying.png"),
                "startup flying probe must match the existing flying template");
        require(startup.contains("images/template/status/unflying.png"),
                "startup flying probe must match the existing non-flying template");
        require(record.contains("FLYING_STATUS_TEMPLATE"),
                "startup flying probe must use the flying template constant");
        require(record.contains("UNFLYING_STATUS_TEMPLATE"),
                "startup flying probe must use the non-flying template constant");
        require(record.contains("windowTaskContextHolder.rawCurrent()"),
                "startup flying result must be stored on the bound window runtime");
        require(record.contains("markTaskQueueStartupFlyingState("),
                "startup flying probe must write the runtime marker");
    }

    private static void firstWubeiRoundSkipsAltCOnlyWhenStartupFlying(String wubei) {
        String prepath = methodBody(wubei, "private void startPostAcceptPrepath(");
        String consume = methodBody(wubei, "private boolean shouldSkipPostAcceptAltCForStartupFlying(");
        int skipDecision = indexOf(prepath, "boolean skipAltC = shouldSkipPostAcceptAltCForStartupFlying(state)");
        int altC = indexOf(prepath, "InputAction.pressAltC()");
        int navigate = indexOf(prepath, "navigationService.navigateInCurrentMap");

        require(skipDecision < altC, "prepath must decide whether to skip Alt+C before submitting Alt+C");
        require(altC < navigate, "normal prepath must still press Alt+C before mini-map navigation");
        require(consume.contains("state.round() != 1"),
                "only the first 五倍 round may consume startup flying state");
        require(consume.contains("consumeTaskQueueStartupFlyingState("),
                "first round must consume the startup flying marker");
        require(consume.contains("GameStateUtil.FlyingState.FLYING"),
                "only a confirmed FLYING startup state may skip Alt+C");
    }

    private static String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        if (signatureIndex < 0) {
            throw new AssertionError("Missing method signature: " + signature);
        }
        int bodyStart = source.indexOf('{', signatureIndex);
        if (bodyStart < 0) {
            throw new AssertionError("Missing method body for: " + signature);
        }
        int depth = 0;
        for (int i = bodyStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(bodyStart, i + 1);
                }
            }
        }
        throw new AssertionError("Unclosed method body for: " + signature);
    }

    private static int indexOf(String source, String needle) {
        int index = source.indexOf(needle);
        if (index < 0) {
            throw new AssertionError("Missing source marker: " + needle);
        }
        return index;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
