package com.bot.dhxy.window.runtime;

import com.bot.dhxy.window.model.WindowNativeBinding;

import java.util.Optional;

/**
 * Source-level behavior test for CR95 live native-title refresh.
 */
public class WindowNativeBindingRefreshTitleTest {

    public static void main(String[] args) {
        WindowNativeBinding original = new WindowNativeBinding(
                "1234",
                "大话西游2经典版 - 江山如画 - 忆叶知秋（ID：451753529）",
                "xy2",
                11L,
                10,
                20,
                1024,
                768);

        WindowNativeBindingRefreshService service = new WindowNativeBindingRefreshService(handle ->
                Optional.of(WindowNativeBindingRefreshService.LiveWindowSnapshot.available(
                        "大话西游2经典版 - 江山如画 - うprinoe大叔（ID：316365558）",
                        "xy2-new",
                        22L,
                        30,
                        40,
                        1024,
                        768)));

        WindowNativeBinding refreshed = service.refreshGeometry(original)
                .orElseThrow(() -> new AssertionError("expected refreshed binding"));
        assertEquals("大话西游2经典版 - 江山如画 - うprinoe大叔（ID：316365558）", refreshed.getTitle(), "title");
        assertEquals("xy2-new", refreshed.getClassName(), "className");
        assertEquals(22L, refreshed.getProcessId(), "processId");
        assertEquals(30, refreshed.getX(), "x");
        assertEquals(40, refreshed.getY(), "y");

        WindowNativeBinding blankTitleRefreshed = new WindowNativeBindingRefreshService(handle ->
                Optional.of(WindowNativeBindingRefreshService.LiveWindowSnapshot.available(
                        "  ",
                        "xy2-new",
                        22L,
                        30,
                        40,
                        1024,
                        768)))
                .refreshGeometry(original)
                .orElseThrow(() -> new AssertionError("expected blank-title refresh to keep geometry"));
        assertEquals("", blankTitleRefreshed.getTitle(), "blank live title must be preserved for runtime commit");

        System.out.println("WindowNativeBindingRefreshTitleTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
