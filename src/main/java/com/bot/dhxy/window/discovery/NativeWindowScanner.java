package com.bot.dhxy.window.discovery;

import java.util.List;

/**
 * 本机窗口扫描接口。
 */
public interface NativeWindowScanner {

    List<NativeWindowInfo> scanWindows();

    List<NativeWindowInfo> scanGameWindows();
}
