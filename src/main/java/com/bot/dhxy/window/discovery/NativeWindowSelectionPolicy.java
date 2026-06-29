package com.bot.dhxy.window.discovery;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Chooses which native game windows should be registered when the scanner sees more windows than
 * the configured automation capacity.
 *
 * <p>Windows enumeration order is z-order on Windows. That gives a practical "recently focused"
 * signal without adding a foreground polling hook: the current foreground/top windows are seen
 * first. Minimized windows are intentionally pushed behind every normal window, even if Windows
 * still reports them near the top of z-order.</p>
 */
@Component
public class NativeWindowSelectionPolicy {

    public List<NativeWindowInfo> sortByRegistrationPriority(List<NativeWindowInfo> windows) {
        if (windows == null || windows.isEmpty()) {
            return List.of();
        }
        return windows.stream()
                .filter(Objects::nonNull)
                .sorted(registrationPriorityComparator())
                .toList();
    }

    public List<NativeWindowInfo> selectForCapacity(List<NativeWindowInfo> windows, int maxCount) {
        if (maxCount <= 0) {
            return List.of();
        }
        List<NativeWindowInfo> sorted = sortByRegistrationPriority(windows);
        if (sorted.size() <= maxCount) {
            return sorted;
        }
        return List.copyOf(sorted.subList(0, maxCount));
    }

    private Comparator<NativeWindowInfo> registrationPriorityComparator() {
        return Comparator
                .comparing(NativeWindowInfo::isMinimized)
                .thenComparing(window -> !window.isForeground())
                .thenComparingInt(NativeWindowInfo::getZOrderIndex)
                .thenComparing(NativeWindowInfo::getTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(NativeWindowInfo::getHandle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
