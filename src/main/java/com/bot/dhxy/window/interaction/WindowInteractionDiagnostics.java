package com.bot.dhxy.window.interaction;

import com.bot.dhxy.window.runner.WindowTaskSnapshot;
import com.bot.dhxy.window.runtime.WindowNativeBinding;
import org.springframework.stereotype.Service;

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
}
