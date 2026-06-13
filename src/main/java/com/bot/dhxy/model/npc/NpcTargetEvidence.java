package com.bot.dhxy.model.npc;

/**
 * How strongly the caller has already confirmed that the requested target exists on screen.
 */
public enum NpcTargetEvidence {
    /**
     * Upstream task logic has confirmed the target through a dialog, tracker hint, or explicit
     * template before asking the smart-click pipeline to click it.
     */
    CONFIRMED,

    /**
     * The caller is only probing a possible target. Smart-click strategies should be allowed to
     * short-circuit cheaply instead of paying for broad OCR/Ctrl fallbacks.
     */
    TENTATIVE
}
