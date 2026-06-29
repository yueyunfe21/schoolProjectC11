package com.bot.dhxy.service;

/**
 * Commit boundary for smart-click evidence once a later dialog option proves the NPC click worked.
 */
public interface SmartClickEvidenceConfirmationService {

    void confirmExpectedOptionProof(String sourceTask,
                                    String actionKey,
                                    String matchedText,
                                    String proofToken,
                                    String verificationStrength,
                                    String reason);
}
