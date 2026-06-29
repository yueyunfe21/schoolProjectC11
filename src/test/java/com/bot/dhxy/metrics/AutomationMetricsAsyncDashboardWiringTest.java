package com.bot.dhxy.metrics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR129: round-finish metrics must not synchronously rebuild dashboard files on
 * the task thread.
 */
public class AutomationMetricsAsyncDashboardWiringTest {

    public static void main(String[] args) throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "bot", "dhxy", "metrics",
                "AutomationMetricsService.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

        String recordRoundFinished = extractMethod(source, "public void recordRoundFinished(");
        require(recordRoundFinished.contains("queueDashboardWrite(\"round-finished\")"),
                "round finish must queue dashboard persistence asynchronously");
        require(!recordRoundFinished.contains("writeDashboardNow()"),
                "round finish must not force synchronous dashboard persistence");

        String writeDashboardNow = extractMethod(source, "public Path writeDashboardNow()");
        require(writeDashboardNow.contains("writeDashboard();"),
                "manual dashboard write must remain synchronous for UI/debug use");

        require(source.contains("dashboardWriterLoop"),
                "metrics service must own a dashboard background writer");
        require(source.contains("dashboard write coalesced"),
                "background dashboard writes should coalesce bursts");

        System.out.println("AutomationMetricsAsyncDashboardWiringTest passed");
    }

    private static String extractMethod(String source, String signaturePrefix) {
        int start = source.indexOf(signaturePrefix);
        if (start < 0) {
            throw new AssertionError("Method signature not found: " + signaturePrefix);
        }
        int brace = source.indexOf('{', start);
        if (brace < 0) {
            throw new AssertionError("Method body not found: " + signaturePrefix);
        }
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(start, i + 1);
                }
            }
        }
        throw new AssertionError("Method body not closed: " + signaturePrefix);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
