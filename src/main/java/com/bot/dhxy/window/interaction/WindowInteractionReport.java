package com.bot.dhxy.window.interaction;

/**
 * 窗口交互能力检查结果。
 */
public class WindowInteractionReport {

    private final String windowId;
    private final String nativeHandle;
    private final boolean hasNativeHandle;
    private final boolean hasGeometry;
    private final String geometryText;
    private final boolean focusSupported;
    private final String message;

    public WindowInteractionReport(String windowId,
                                   String nativeHandle,
                                   boolean hasNativeHandle,
                                   boolean hasGeometry,
                                   String geometryText,
                                   boolean focusSupported,
                                   String message) {
        this.windowId = windowId;
        this.nativeHandle = nativeHandle;
        this.hasNativeHandle = hasNativeHandle;
        this.hasGeometry = hasGeometry;
        this.geometryText = geometryText == null ? "-" : geometryText;
        this.focusSupported = focusSupported;
        this.message = message == null ? "" : message;
    }

    public String getWindowId() { return windowId; }

    public String getNativeHandle() { return nativeHandle; }

    public boolean isHasNativeHandle() { return hasNativeHandle; }

    public boolean isHasGeometry() { return hasGeometry; }

    public String getGeometryText() { return geometryText; }

    public boolean isFocusSupported() { return focusSupported; }

    public String getMessage() { return message; }

    public boolean isReady() {
        return hasNativeHandle && hasGeometry;
    }

    public String toSummaryText() {
        return "window=" + safe(windowId)
                + ", hwnd=" + safe(nativeHandle)
                + ", geometry=" + geometryText
                + ", ready=" + isReady()
                + ", focus=" + focusSupported
                + ", message=" + message;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
