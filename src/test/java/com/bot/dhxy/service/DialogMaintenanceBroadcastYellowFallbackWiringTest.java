package com.bot.dhxy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DialogMaintenanceBroadcastYellowFallbackWiringTest {

    public static void main(String[] args) throws Exception {
        String dialogService = read("src/main/java/com/bot/dhxy/service/DialogService.java");
        String formalHandler = methodBody(dialogService, "private DialogResult handleBusinessOption");
        String fastPath = methodBody(dialogService, "private DialogResult tryClickMaintenanceBroadcastOption");
        String noFocusPrefilter = methodBody(dialogService, "public String detectMaintenanceBroadcastActionNoFocus");

        assertContains(formalHandler, "washMaintenanceBroadcastBusinessOption(rawPath, washedPath, false)",
                "formal maintenance broadcast handling must try green wash first");
        assertContains(formalHandler, "washMaintenanceBroadcastBusinessOption(rawPath, yellowWashedPath, true)",
                "formal maintenance broadcast handling must try yellow wash after green misses");
        assertInOrder(formalHandler,
                "washMaintenanceBroadcastBusinessOption(rawPath, washedPath, false)",
                "washMaintenanceBroadcastBusinessOption(rawPath, yellowWashedPath, true)",
                "yellow fallback must run only after green matching misses");

        assertContains(fastPath, "ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath)",
                "lightweight fixed-strip maintenance broadcast path must try green wash first");
        assertContains(fastPath, "ImagePreprocessor.washYellowText(rawPath, yellowWashedPath)",
                "lightweight fixed-strip maintenance broadcast path must try yellow wash after green misses");
        assertInOrder(fastPath,
                "ImagePreprocessor.washGreenTextToBlackAndWhite(rawPath, washedPath)",
                "ImagePreprocessor.washYellowText(rawPath, yellowWashedPath)",
                "lightweight yellow fallback must reuse the captured small ROI only after green misses");
        assertNotContains(noFocusPrefilter, "washMaintenanceBroadcastBusinessOption(",
                "no-focus maintenance broadcast prefilter must stay single green wash");
        assertNotContains(noFocusPrefilter, "washYellowText",
                "no-focus maintenance broadcast prefilter must not add yellow wash");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static String methodBody(String source, String methodSignaturePrefix) {
        int methodAt = source.indexOf(methodSignaturePrefix);
        if (methodAt < 0) {
            throw new AssertionError("missing method: " + methodSignaturePrefix);
        }
        int braceAt = source.indexOf('{', methodAt);
        if (braceAt < 0) {
            throw new AssertionError("missing method body: " + methodSignaturePrefix);
        }
        int depth = 0;
        for (int i = braceAt; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(braceAt, i + 1);
                }
            }
        }
        throw new AssertionError("unterminated method body: " + methodSignaturePrefix);
    }

    private static void assertContains(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new AssertionError(message + "; missing: " + needle);
        }
    }

    private static void assertNotContains(String source, String needle, String message) {
        if (source.contains(needle)) {
            throw new AssertionError(message + "; forbidden: " + needle);
        }
    }

    private static void assertInOrder(String source, String first, String second, String message) {
        int firstAt = source.indexOf(first);
        int secondAt = source.indexOf(second);
        if (firstAt < 0 || secondAt < 0 || firstAt >= secondAt) {
            throw new AssertionError(message);
        }
    }
}
