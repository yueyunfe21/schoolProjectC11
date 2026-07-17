package com.bot.dhxy.service.npc;

/**
 * Closed set of local post-click verification modes that actually issue an {@code NPC_LOCAL_VERIFY}.
 *
 * <p>These are the four verifier paths from HEAD {@code 0114604e} {@code NpcClickService}:
 * a single expected-dialog template, a raw expected-dialog template, an ordered list of
 * expected-dialog templates, and the direct-combat state check. The HEAD
 * {@code deferDialogVerificationToTask()} path stays owned by the task phase and must not send this
 * remote verify operation, so it is deliberately not a fifth mode. Concrete template identities
 * remain locally resolved closed resource keys; no raw path or template bytes are transmitted.</p>
 */
public enum NpcVerifyMode {
    DIALOG_TEMPLATE,
    RAW_DIALOG_TEMPLATE,
    DIALOG_TEMPLATE_LIST,
    COMBAT_STATE
}
