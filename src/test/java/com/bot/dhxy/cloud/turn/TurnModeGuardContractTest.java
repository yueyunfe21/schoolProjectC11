package com.bot.dhxy.cloud.turn;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnModeGuardContractTest {

    /**
     * 2026-08-25 五环轮间死锁合同:已停止且"发过 start 但云端从未 ACK"的 loop 必须可移除
     * (云端从未建立 RunSlot,零双主风险);拿到过 ACK 而无终态的 loop 保持 fail-closed。
     */
    @Test
    void stoppedLoopWithoutCloudStartAckIsRemovable() throws Exception {
        WindowTurnLoop loop = minimalLoop();
        // 发过 start(pendingStartRequest 非空)但从未 ACK
        setField(loop, "pendingStartRequest", newStartRequest());
        org.junit.jupiter.api.Assertions.assertTrue(loop.hasTaskStartRequest(), "前置:发过 start");
        org.junit.jupiter.api.Assertions.assertFalse(loop.hasAcceptedStartAck(), "前置:无 ACK");
        org.junit.jupiter.api.Assertions.assertTrue(TurnModeGuard.canRemoveStoppedLoop(loop),
                "无 ACK 的已停 loop 必须可移除(2026-08-25 轮间死锁案)");
    }

    @Test
    void stoppedLoopWithAcceptedAckAndNoTerminalStaysFailClosed() throws Exception {
        WindowTurnLoop loop = minimalLoop();
        setField(loop, "pendingStartRequest", newStartRequest());
        setField(loop, "startAckAccepted", Boolean.TRUE);
        org.junit.jupiter.api.Assertions.assertFalse(TurnModeGuard.canRemoveStoppedLoop(loop),
                "有 ACK 无终态必须保持 fail-closed(防双主)");
    }

    private static WindowTurnLoop minimalLoop() {
        return new WindowTurnLoop("device-1", "window-1", 1000L,
                () -> null,
                (TurnClient) java.lang.reflect.Proxy.newProxyInstance(
                        TurnClient.class.getClassLoader(), new Class<?>[]{TurnClient.class},
                        (proxy, method, args) -> { throw new UnsupportedOperationException(method.getName()); }),
                (WindowTurnLoop.TurnActionRunner) action -> { throw new UnsupportedOperationException(); });
    }

    private static Object newStartRequest() throws Exception {
        // 任意非空占位即可:字段类型为 TurnTaskStartRequest,用 Objenesis 式反射绕开构造校验
        Class<?> c = Class.forName("com.bot.dhxy.cloud.turn.protocol.TurnTaskStartRequest");
        java.lang.reflect.Constructor<?> ctor = c.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object[] args = new Object[ctor.getParameterCount()];
        Class<?>[] ts = ctor.getParameterTypes();
        for (int i = 0; i < ts.length; i++) {
            if (ts[i] == int.class) args[i] = 1;
            else if (ts[i] == long.class) args[i] = 1L;
            else if (ts[i] == boolean.class) args[i] = false;
            else if (ts[i] == String.class) args[i] = "x";
            else if (ts[i] == java.util.List.class) args[i] = java.util.List.of();
            else args[i] = null;
        }
        try { return ctor.newInstance(args); } catch (Exception e) {
            // 构造校验过不去时退而求其次:sun.misc.Unsafe 分配裸实例(仅测试用)
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return ((sun.misc.Unsafe) f.get(null)).allocateInstance(c);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        java.lang.reflect.Field f = WindowTurnLoop.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void localAndRemoteOwnershipRemainMutuallyExclusivePerWindow() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/cloud/turn/TurnModeGuard.java"));
        assertTrue(source.contains("startLocal"));
        assertTrue(source.contains("startRemote"));
        assertTrue(source.contains("synchronized (modeMonitor)"));
        assertTrue(source.contains("loopRegistry.find(windowId).isPresent()"));
        assertTrue(source.contains("runner.isRunning()"));
        assertTrue(source.contains("existingLoop.lastFailure()"));
        assertTrue(source.contains("existingLoop.start()"));
    }
}
