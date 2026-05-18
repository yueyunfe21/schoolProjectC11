package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.runner.WindowTaskSnapshot;
import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 窗口交互诊断服务。
 */
@Service
public class WindowInteractionDiagnostics {

    private final WindowFocusService focusService;

    public WindowInteractionDiagnostics(WindowFocusService focusService) {
        this.focusService = focusService;
    }

    public WindowInteractionReport inspect(WindowTaskSnapshot snapshot) {
        if (snapshot == null) {
            return new WindowInteractionReport(null, null, false, false, "-", false, "窗口快照为空");
        }
        WindowNativeBinding binding = snapshot.getNativeBinding();
        boolean hasHandle = binding != null && binding.hasNativeHandle();
        boolean hasGeometry = binding != null && binding.hasGeometry();
        return new WindowInteractionReport(
                snapshot.getWindowId(),
                binding == null ? null : binding.getNativeHandle(),
                hasHandle,
                hasGeometry,
                binding == null ? "-" : binding.getGeometryText(),
                hasHandle,
                hasHandle && hasGeometry ? "窗口交互信息完整" : "窗口缺少句柄或坐标"
        );
    }

    public List<WindowInteractionReport> inspectAll(List<WindowTaskSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }
        return snapshots.stream().map(this::inspect).toList();
    }

    public WindowInteractionReport focusTest(WindowTaskSnapshot snapshot) {
        WindowInteractionReport report = inspect(snapshot);
        if (!report.isReady()) {
            return report;
        }
        boolean focused = focusService.focus(snapshot.getNativeBinding());
        return new WindowInteractionReport(
                report.getWindowId(),
                report.getNativeHandle(),
                report.isHasNativeHandle(),
                report.isHasGeometry(),
                report.getGeometryText(),
                focused,
                focused ? "窗口激活成功" : "窗口激活失败"
        );
    }

    public List<WindowInteractionReport> focusTestAll(List<WindowTaskSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Collections.emptyList();
        }
        return snapshots.stream().map(this::focusTest).toList();
    }

    public long countReady(List<WindowTaskSnapshot> snapshots) {
        return inspectAll(snapshots).stream().filter(WindowInteractionReport::isReady).count();
    }

    public String summarize(List<WindowTaskSnapshot> snapshots) {
        List<WindowInteractionReport> reports = inspectAll(snapshots);
        long ready = reports.stream().filter(WindowInteractionReport::isReady).count();
        return "窗口交互诊断：ready=" + ready + "/" + reports.size();
    }
}
