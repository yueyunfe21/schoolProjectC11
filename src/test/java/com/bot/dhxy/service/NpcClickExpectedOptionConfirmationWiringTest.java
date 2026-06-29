package com.bot.dhxy.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source-level guard for CR103 generic expected-option proof committing runner-owned smart-click
 * evidence.
 */
public class NpcClickExpectedOptionConfirmationWiringTest {

    public static void main(String[] args) throws Exception {
        String dialogService = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/DialogService.java"),
                StandardCharsets.UTF_8);
        String npcClickService = Files.readString(
                Path.of("src/main/java/com/bot/dhxy/service/NpcClickService.java"),
                StandardCharsets.UTF_8);

        require(dialogService.contains("ObjectProvider<SmartClickEvidenceConfirmationService> smartClickEvidenceConfirmationService"),
                "DialogService must publish expected-option proof through a generic confirmation collaborator");
        require(dialogService.contains("confirmPendingSmartClickIfExpectedOptionProved(request, result);"),
                "DialogService finishRequest must confirm pending smart-click evidence at the option-proof boundary");
        String proofMethod = between(dialogService,
                "private void confirmPendingSmartClickIfExpectedOptionProved(",
                "private boolean isExpectedOptionProof(");
        require(proofMethod.contains("getPendingSmartClickEvidenceProofToken"),
                "DialogService must carry the current window's smart-click proof token into generic confirmation");
        require(proofMethod.contains("confirmationService.confirmExpectedOptionProof("),
                "expected-option proof must call the shared confirmation service");
        String proofStatus = between(dialogService,
                "private boolean isExpectedOptionProof(",
                "private boolean isLightweightBusinessOptionNoneResult(");
        require(proofStatus.contains("DialogResultStatus.GREEN_TEMPLATE_VISIBLE"),
                "template visibility verification must count as expected-option proof");
        require(proofStatus.contains("DialogResultStatus.GREEN_TEMPLATE_CLICKED"),
                "template click consumption must count as expected-option proof");
        require(proofStatus.contains("DialogResultStatus.BUSINESS_OPTION_CLICKED"),
                "maintenance business option consumption must count as expected-option proof");
        require(proofStatus.contains("DialogResultStatus.OPTION_KEYWORD_CLICKED"),
                "OCR/remembered option consumption must count as expected-option proof");

        require(npcClickService.contains("implements SmartClickEvidenceConfirmationService"),
                "NpcClickService must own the shared confirmation collaborator without DialogService depending on NpcClickService");
        require(npcClickService.contains("confirmExpectedOptionProof("),
                "NpcClickService must expose a generic expected-option proof commit point");
        String confirmMethod = between(npcClickService,
                "public void confirmExpectedOptionProof(",
                "private void recordConfirmedSmartClickEvidence(");
        require(confirmMethod.contains("pending.matchesProofToken(proofToken)"),
                "generic proof must require the per-click proof token, not only window id and option template");
        require(confirmMethod.contains("removePendingSmartClickEvidence(key, pending.proofToken"),
                "generic proof must clear only the matching pending token before committing");
        require(confirmMethod.contains("pending.matchesExpectedOptionProof("),
                "generic proof must match pending evidence against the original expected template/action proof");
        require(confirmMethod.contains("recordConfirmedSmartClickEvidence(pending, true"),
                "matched expected-option proof must commit the pending direct-click evidence");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex);
        if (endIndex < 0) {
            throw new AssertionError("Missing source marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
