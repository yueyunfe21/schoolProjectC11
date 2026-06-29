package com.bot.dhxy.service;

import com.bot.dhxy.model.dialog.DialogType;

public class DialogMaintenanceFallbackPolicyTest {

    public static void main(String[] args) {
        assertFalse("STORY must not enter maintenance option matching",
                DialogService.shouldRunMaintenanceBusinessOptionFallback(DialogType.STORY));
        assertFalse("NONE must not enter maintenance option matching",
                DialogService.shouldRunMaintenanceBusinessOptionFallback(DialogType.NONE));
        assertTrue("OPTION may enter maintenance option matching",
                DialogService.shouldRunMaintenanceBusinessOptionFallback(DialogType.OPTION));
    }

    private static void assertTrue(String label, boolean value) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertFalse(String label, boolean value) {
        if (value) {
            throw new AssertionError(label + " expected false");
        }
    }
}
