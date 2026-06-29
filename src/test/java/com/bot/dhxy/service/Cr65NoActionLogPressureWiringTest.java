package com.bot.dhxy.service;

import java.nio.file.Files;
import java.nio.file.Path;

public class Cr65NoActionLogPressureWiringTest {

    public static void main(String[] args) throws Exception {
        lightweightFallbackDisabledLogIsThrottled();
        refreshDueDeferredLogIsThrottled();
        autoBattleBusinessOptionNoneRequestAndResultAreThrottled();
    }

    private static void lightweightFallbackDisabledLogIsThrottled() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/DialogService.java"));

        assertContains("lightweight fallback has throttle interval",
                source, "LIGHTWEIGHT_FALLBACK_DISABLED_LOG_INTERVAL_MS");
        assertContains("lightweight fallback uses throttle gate",
                source, "shouldLogLightweightFallbackDisabled");
        assertContains("lightweight fallback keeps debug path for suppressed repeats",
                source, "maintenance broadcast lightweight fallback disabled suppressed");
    }

    private static void refreshDueDeferredLogIsThrottled() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/AutoCombatService.java"));

        assertContains("refresh-due defer has throttle interval",
                source, "REFRESH_DUE_DEFERRED_LOG_INTERVAL_MS");
        assertContains("refresh-due defer uses throttle gate",
                source, "logRefreshDueDeferred");
        assertContains("refresh-due defer keeps debug path for suppressed repeats",
                source, "refresh-due panel verify deferred suppressed by log throttle");
    }

    private static void autoBattleBusinessOptionNoneRequestAndResultAreThrottled() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/bot/dhxy/service/DialogService.java"));

        assertContains("auto-battle no-action result has throttle interval",
                source, "LIGHTWEIGHT_BUSINESS_OPTION_NONE_LOG_INTERVAL_MS");
        assertContains("dialog requests go through CR65-aware logger",
                source, "logHandleRequest(request)");
        assertContains("lightweight auto-battle request is downgraded before result is known",
                source, "dialog handle request suppressed by lightweight no-action policy");
        assertContains("no-action result uses throttle gate",
                source, "shouldLogLightweightBusinessOptionNoneResult");
        assertContains("no-action repeated result keeps debug path",
                source, "dialog handle result suppressed by lightweight no-action policy");
    }

    private static void assertContains(String label, String source, String needle) {
        if (!source.contains(needle)) {
            throw new AssertionError(label + " missing: " + needle);
        }
    }
}
