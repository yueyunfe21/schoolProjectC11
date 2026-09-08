# -*- coding: utf-8 -*-
"""G151: Windows.Graphics.Capture 采集 sidecar。

PrintWindow(PW_RENDERFULLCONTENT) 会向游戏窗口投递重绘请求——全系统唯一"物理接触"游戏进程的
采集路径（2026-07 性能调查实证整窗重绘）。WGC 从 DWM 合成缓冲取帧，目标进程零参与。
2026-09-04 对 3519 实测：WGC 帧与窗口 DC 像素零偏移、帧契约同构（1036x783 含标题栏），
771 个模板零改动直接换底。

协议（localhost TCP，单行请求 + 原始帧响应）：
  请求:  CAP <hwnd十进制> <freshMs> <timeoutMs>\n
  成功:  OK <width> <height> <byteLen>\n  后随 byteLen 字节 BGRA 顶行在前
  失败:  ERR <reason>\n
  探活:  PING\n -> PONG\n

会话管理：按 hwnd 懒建（cursor_capture=False, draw_border=False,
minimum_update_interval=MIN_UPDATE_MS 限流），缓存最新帧+单调时间戳；
窗口关闭(on_closed)或 IDLE_EVICT_S 无人问津即拆会话。
"""
import ctypes
import socket
import sys
import threading
import time
from ctypes import wintypes

from windows_capture import WindowsCapture, Frame, InternalCaptureControl

_user32 = ctypes.windll.user32


def _title_of(hwnd):
    """hwnd→当前精确标题。库的 window_hwnd 路对本游戏窗口抛
    "Failed to convert item to GraphicsCaptureItem"（2026-09-04 实测），window_name 路可用；
    游戏标题含唯一角色 ID，精确标题匹配等效按句柄。标题读不到=窗口已死。"""
    if not _user32.IsWindow(hwnd):
        return None
    length = _user32.GetWindowTextLengthW(hwnd)
    if length <= 0:
        return None
    buf = ctypes.create_unicode_buffer(length + 1)
    _user32.GetWindowTextW(hwnd, buf, length + 1)
    return buf.value or None

HOST = "127.0.0.1"
PORT = 47831
MIN_UPDATE_MS = 400
IDLE_EVICT_S = 120.0

_sessions = {}
_lock = threading.Lock()


class _Session:
    def __init__(self, hwnd):
        self.hwnd = hwnd
        self.frame = None          # (bytes BGRA, w, h)
        self.frame_at = 0.0        # monotonic
        self.last_used = time.monotonic()
        self.closed = False
        self.cond = threading.Condition()
        title = _title_of(hwnd)
        if title is None:
            raise ValueError("window-dead-or-untitled")
        capture = WindowsCapture(
            cursor_capture=False,
            draw_border=False,
            minimum_update_interval=MIN_UPDATE_MS,
            window_name=title,
        )

        @capture.event
        def on_frame_arrived(frame: Frame, control: InternalCaptureControl):
            buf = frame.frame_buffer  # HxWx4 BGRA
            data = buf.tobytes()
            with self.cond:
                self.frame = (data, frame.width, frame.height)
                self.frame_at = time.monotonic()
                self.cond.notify_all()

        @capture.event
        def on_closed():
            with self.cond:
                self.closed = True
                self.cond.notify_all()

        self.control = capture.start_free_threaded()

    def grab(self, fresh_ms, timeout_ms):
        deadline = time.monotonic() + timeout_ms / 1000.0
        with self.cond:
            while True:
                self.last_used = time.monotonic()
                if self.closed:
                    return None
                if self.frame is not None \
                        and (time.monotonic() - self.frame_at) * 1000.0 <= fresh_ms:
                    return self.frame
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    # 超时兜底：有旧帧给旧帧（窗口静止时 WGC 不推新帧是正常现象）
                    return self.frame
                self.cond.wait(min(0.25, remaining))

    def stop(self):
        try:
            self.control.stop()
        except Exception:
            pass


def _session_for(hwnd):
    with _lock:
        session = _sessions.get(hwnd)
        if session is not None and not session.closed:
            return session
        if session is not None:
            session.stop()
        session = _Session(hwnd)
        _sessions[hwnd] = session
        return session


def _evictor():
    while True:
        time.sleep(30.0)
        now = time.monotonic()
        with _lock:
            for hwnd in list(_sessions):
                session = _sessions[hwnd]
                if session.closed or now - session.last_used > IDLE_EVICT_S:
                    session.stop()
                    del _sessions[hwnd]


def _serve(conn):
    conn.settimeout(30.0)
    reader = conn.makefile("rb")
    try:
        while True:
            line = reader.readline()
            if not line:
                return
            parts = line.decode("ascii", "replace").strip().split()
            if not parts:
                continue
            if parts[0] == "PING":
                conn.sendall(b"PONG\n")
                continue
            if parts[0] != "CAP" or len(parts) != 4:
                conn.sendall(b"ERR bad-request\n")
                continue
            try:
                hwnd = int(parts[1])
                fresh_ms = max(1, int(parts[2]))
                timeout_ms = max(1, int(parts[3]))
            except ValueError:
                conn.sendall(b"ERR bad-args\n")
                continue
            try:
                session = _session_for(hwnd)
                result = session.grab(fresh_ms, timeout_ms)
            except Exception as failure:  # noqa: BLE001 会话建立失败=窗口无效等
                conn.sendall(("ERR session:%s\n" % type(failure).__name__).encode())
                continue
            if result is None:
                conn.sendall(b"ERR window-closed\n")
                continue
            data, width, height = result
            conn.sendall(("OK %d %d %d\n" % (width, height, len(data))).encode())
            conn.sendall(data)
    except (ConnectionError, socket.timeout, OSError):
        pass
    finally:
        try:
            conn.close()
        except OSError:
            pass


def main():
    threading.Thread(target=_evictor, daemon=True).start()
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen(8)
    print("wgc-sidecar listening on %s:%d (minUpdate=%dms)" % (HOST, PORT, MIN_UPDATE_MS), flush=True)
    while True:
        conn, _ = server.accept()
        threading.Thread(target=_serve, args=(conn,), daemon=True).start()


if __name__ == "__main__":
    sys.exit(main())
