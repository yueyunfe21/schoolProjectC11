package com.bot.dhxy.service;

import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.model.maintenance.CommonBoxRole;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Behavior checks for CR120 pending common-box lifetime gates.
 */
public final class CommonBoxPendingBehaviorTest {

    private CommonBoxPendingBehaviorTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyPendingRequiresSameTaskRun();
        verifyPendingRequiresSameIdentityEpoch();
        verifyExpiredPendingIsPruned();
        verifySwitchOffClearsPending();
        System.out.println("CommonBoxPendingBehaviorTest passed");
    }

    private static void verifyPendingRequiresSameTaskRun() throws Exception {
        Fixture fixture = new Fixture();
        fixture.addPending("1", fixture.window.getPlayerIdentityEpoch(), System.currentTimeMillis() + 30_000L);

        require(fixture.service.hasPendingBoxForCurrentWindow(fixture.context(1L), "xiuluo_v2"),
                "Pending box should be visible to its own task run");
        require(!fixture.service.hasPendingBoxForCurrentWindow(fixture.context(2L), "xiuluo_v2"),
                "Pending box must not be visible to a different task run");
    }

    private static void verifyPendingRequiresSameIdentityEpoch() throws Exception {
        Fixture fixture = new Fixture();
        fixture.addPending("1", fixture.window.getPlayerIdentityEpoch(), System.currentTimeMillis() + 30_000L);
        fixture.window.setNativeBinding(binding("2", "new-player-title"));

        require(!fixture.service.hasPendingBoxForCurrentWindow(fixture.context(1L), "xiuluo_v2"),
                "Pending box must not survive player identity drift");
    }

    private static void verifyExpiredPendingIsPruned() throws Exception {
        Fixture fixture = new Fixture();
        fixture.addPending("1", fixture.window.getPlayerIdentityEpoch(), System.currentTimeMillis() - 1L);

        require(!fixture.service.hasPendingBoxForCurrentWindow(fixture.context(1L), "xiuluo_v2"),
                "Expired pending box must not be visible");
        require(fixture.pendingMap().isEmpty(), "Expired pending box should be pruned during lookup");
    }

    private static void verifySwitchOffClearsPending() throws Exception {
        Fixture fixture = new Fixture();
        fixture.addPending("1", fixture.window.getPlayerIdentityEpoch(), System.currentTimeMillis() + 30_000L);
        fixture.properties.setLeaderCommonBoxEnabled(false);

        require(!fixture.service.consumePendingBoxIfAllowed(fixture.context(1L), "xiuluo_v2", "cr120-test"),
                "Switch-off consume must fail closed");
        require(fixture.pendingMap().isEmpty(), "Switch-off path must clear pending boxes for the role");
    }

    private static final class Fixture {
        private final BotProperties properties = new BotProperties();
        private final WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        private final CommonBoxService service = new CommonBoxService(properties, null, null, holder);
        private final WindowRuntimeContext window = new WindowRuntimeContext("common-box-window", new GameContext());

        private Fixture() {
            properties.setLeaderCommonBoxEnabled(true);
            properties.setMemberCommonBoxEnabled(true);
            window.setRole(WindowRole.LEADER);
            window.setNativeBinding(binding("1", "old-player-title"));
            holder.bind(window);
        }

        private TaskExecutionContext context(long taskRunId) {
            return TaskExecutionContext.builder()
                    .taskCode("xiuluo_v2")
                    .requestedTaskCode("xiuluo_v2")
                    .windowId(window.getWindowId())
                    .windowRole("LEADER")
                    .taskRunId(taskRunId)
                    .build();
        }

        private void addPending(String taskRunKey, long identityEpoch, long expiresAtMs) throws Exception {
            Object pending = pending("xiuluo_v2", taskRunKey, identityEpoch, expiresAtMs);
            String key = pendingKey(taskRunKey);
            pendingMap().put(key, pending);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> pendingMap() throws Exception {
            Field field = CommonBoxService.class.getDeclaredField("pendingByKey");
            field.setAccessible(true);
            return (Map<String, Object>) field.get(service);
        }

        private String pendingKey(String taskRunKey) throws Exception {
            Method method = CommonBoxService.class.getDeclaredMethod(
                    "pendingKey", WindowRuntimeContext.class, CommonBoxRole.class, String.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, window, CommonBoxRole.LEADER, "xiuluo_v2", taskRunKey);
        }
    }

    private static Object pending(String taskKey, String taskRunKey, long identityEpoch, long expiresAtMs)
            throws Exception {
        Class<?> pendingClass = null;
        for (Class<?> nested : CommonBoxService.class.getDeclaredClasses()) {
            if ("PendingCommonBox".equals(nested.getSimpleName())) {
                pendingClass = nested;
                break;
            }
        }
        if (pendingClass == null) {
            throw new AssertionError("PendingCommonBox record not found");
        }
        Constructor<?> constructor = pendingClass.getDeclaredConstructor(
                String.class,
                String.class,
                String.class,
                String.class,
                CommonBoxRole.class,
                long.class,
                long.class,
                int.class,
                int.class,
                int.class,
                int.class,
                long.class,
                String.class);
        constructor.setAccessible(true);
        long now = System.currentTimeMillis();
        return constructor.newInstance(
                "common-box-window",
                "1",
                taskKey,
                taskRunKey,
                CommonBoxRole.LEADER,
                now,
                expiresAtMs,
                10,
                10,
                10,
                10,
                identityEpoch,
                "cr120-test");
    }

    private static WindowNativeBinding binding(String handle, String title) {
        return new WindowNativeBinding(handle, title, "xy2", 1L, 100, 200, 1024, 768);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
