package com.bot.dhxy;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class WindowScanner {
    public static void main(String[] args) {
        System.out.println("📡 正在全盘扫描当前系统可见窗口...\n");

        // 调用 Win32 API 遍历所有窗口
        User32.INSTANCE.EnumWindows(new User32.WNDENUMPROC() {
            @Override
            public boolean callback(HWND hWnd, Pointer arg1) {
                // 过滤掉那些隐藏的、不可见的系统后台窗口
                if (User32.INSTANCE.IsWindowVisible(hWnd)) {
                    // 准备一个字符数组来接标题
                    char[] windowText = new char[512];
                    User32.INSTANCE.GetWindowText(hWnd, windowText, 512);

                    // 转成 Java 字符串并去掉头尾空白
                    String wText = Native.toString(windowText).trim();

                    // 只打印有名字的窗口
                    if (!wText.isEmpty()) {
                        System.out.println("🎯 发现窗口: [" + wText + "]");
                    }
                }
                return true; // return true 表示继续找下一个
            }
        }, null);

        System.out.println("\n✅ 扫描完毕！请在上面的列表里找到大话西游，把中括号里的字原封不动复制进代码里。");
    }
}