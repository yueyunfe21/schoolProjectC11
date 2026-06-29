package com.bot.dhxy.window.discovery;

import java.util.List;

public class NativeWindowSelectionPolicyTest {

    private final NativeWindowSelectionPolicy policy = new NativeWindowSelectionPolicy();

    public static void main(String[] args) {
        NativeWindowSelectionPolicyTest test = new NativeWindowSelectionPolicyTest();
        test.selectForCapacityPrefersNonMinimizedWindowsBeforeMinimizedWindows();
        test.sortByRegistrationPriorityKeepsFocusedZOrderBeforeOlderWindows();
        System.out.println("NativeWindowSelectionPolicyTest passed");
    }

    private void selectForCapacityPrefersNonMinimizedWindowsBeforeMinimizedWindows() {
        NativeWindowInfo minimizedTopWindow = window("100", "最小化窗口", 0, true, true);
        NativeWindowInfo firstNormalWindow = window("101", "正常窗口1", 1, false, false);
        NativeWindowInfo secondNormalWindow = window("102", "正常窗口2", 2, false, false);
        NativeWindowInfo thirdNormalWindow = window("103", "正常窗口3", 3, false, false);
        NativeWindowInfo fourthNormalWindow = window("104", "正常窗口4", 4, false, false);
        NativeWindowInfo fifthNormalWindow = window("105", "正常窗口5", 5, false, false);

        List<NativeWindowInfo> selected = policy.selectForCapacity(List.of(
                minimizedTopWindow,
                fifthNormalWindow,
                secondNormalWindow,
                fourthNormalWindow,
                firstNormalWindow,
                thirdNormalWindow
        ), 5);

        assertEquals("minimized windows should be selected after all normal windows", List.of(
                firstNormalWindow,
                secondNormalWindow,
                thirdNormalWindow,
                fourthNormalWindow,
                fifthNormalWindow
        ), selected);
    }

    private void sortByRegistrationPriorityKeepsFocusedZOrderBeforeOlderWindows() {
        NativeWindowInfo olderWindow = window("200", "较早窗口", 4, false, false);
        NativeWindowInfo foregroundWindow = window("201", "当前窗口", 0, false, true);
        NativeWindowInfo recentWindow = window("202", "最近窗口", 1, false, false);

        List<NativeWindowInfo> sorted = policy.sortByRegistrationPriority(List.of(
                olderWindow,
                recentWindow,
                foregroundWindow
        ));

        assertEquals("foreground/z-order should decide normal window priority",
                List.of(foregroundWindow, recentWindow, olderWindow), sorted);
    }

    private NativeWindowInfo window(String handle,
                                    String title,
                                    int zOrderIndex,
                                    boolean minimized,
                                    boolean foreground) {
        return new NativeWindowInfo(
                handle,
                title,
                "GameWindow",
                1000L,
                100,
                100,
                800,
                600,
                minimized,
                foreground,
                zOrderIndex
        );
    }

    private static void assertEquals(String label, List<NativeWindowInfo> expected, List<NativeWindowInfo> actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + ids(expected) + " actual=" + ids(actual));
        }
    }

    private static List<String> ids(List<NativeWindowInfo> windows) {
        return windows.stream().map(NativeWindowInfo::toWindowId).toList();
    }
}
