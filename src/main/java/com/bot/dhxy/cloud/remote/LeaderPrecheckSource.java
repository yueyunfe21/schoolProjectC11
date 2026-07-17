package com.bot.dhxy.cloud.remote;

/**
 * W-TEAMRETURN-TYPES-IMP1 (CR TeamReturnService lift-and-shift): closed set of the leader-precheck
 * business call sites. Mirrors the two baseline {@code beginLeaderSignalPrecheck} phases; carries no
 * diagnostic text (that stays in the bounded envelope message). The Cloud mirror
 * {@code com.yueyunfe.dhxy.cloudbrain.remote.LeaderPrecheckSource} keeps the same names and order.
 */
public enum LeaderPrecheckSource {
    CACHED_RETURN_VERIFIED,
    RETURN_HOME_VERIFIED
}
