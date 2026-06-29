package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for CR131: leader team-return detection is captured before bag interaction and then
 * consumed after return verification.
 */
public class TeamReturnPrecheckWiringTest {

    public static void main(String[] args) throws Exception {
        String teamReturn = Files.readString(Path.of("src", "main", "java", "com", "bot", "dhxy",
                "service", "TeamReturnService.java"), StandardCharsets.UTF_8);
        String precheck = extractMethod(teamReturn, "public LeaderSignalPrecheck beginLeaderSignalPrecheck(");
        require(precheck.contains("captureToMemory"),
                "precheck must capture a fixed screenshot before bag interaction");
        require(precheck.contains("CompletableFuture.supplyAsync"),
                "precheck analysis must run in the background");
        require(!precheck.contains("inputSequences"),
                "precheck must be read-only and must not send input");

        assertTaskPrecheckBeforeBag("XiuluoTaskV2.java",
                Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "xiuluo", "XiuluoTaskV2.java"),
                "private boolean useReturnItemAndVerifyStartMap(",
                "bagService.findAndUseMainBagTaskPageItem");
        assertTaskPrecheckBeforeBag("WubeiTask.java",
                Path.of("src", "main", "java", "com", "bot", "dhxy", "task", "wubei", "WubeiTask.java"),
                "private boolean useReturnItem(",
                "bagService.findAndUseItemFromBack");

        System.out.println("TeamReturnPrecheckWiringTest passed");
    }

    private static void assertTaskPrecheckBeforeBag(String label,
                                                    Path path,
                                                    String methodSignature,
                                                    String bagCall) throws Exception {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        String method = extractMethod(source, methodSignature);
        int precheckIndex = method.indexOf("beginLeaderSignalPrecheck(");
        int bagIndex = method.indexOf(bagCall);
        require(precheckIndex >= 0, label + " must launch team-return precheck");
        require(bagIndex >= 0, label + " bag call not found");
        require(precheckIndex < bagIndex, label + " must launch precheck before bag interaction");
        require(source.contains("consumeLeaderSignalPrecheck("),
                label + " must consume the precomputed team-return result after return verification");
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
