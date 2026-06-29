package com.bot.dhxy.service;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.config.BotProperties;
import com.bot.dhxy.config.WindowIsolationProperties;
import com.bot.dhxy.model.maintenance.CommonBoxRole;
import com.bot.dhxy.runner.context.TaskExecutionContext;
import com.bot.dhxy.window.execution.WindowTaskRunner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Focused CR120 boundary checks that should not depend on live screenshots or input.
 */
public final class CommonBoxBoundaryBehaviorTest {

    private CommonBoxBoundaryBehaviorTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyBusinessRoleComesFromTaskContext();
        verifyTaskRunSequenceIsGlobal();
        System.out.println("CommonBoxBoundaryBehaviorTest passed");
    }

    private static void verifyBusinessRoleComesFromTaskContext() throws Exception {
        Fixture fixture = new Fixture();
        fixture.window.setRole(WindowRole.UNKNOWN);
        fixture.addPending("1", fixture.window.getPlayerIdentityEpoch(), System.currentTimeMillis() + 30_000L);

        require(fixture.service.hasPendingBoxForCurrentWindow(fixture.context("LEADER", 1L), "xiuluo_v2"),
                "Runtime UNKNOWN role must not hide a leader common-box when task context says LEADER");
        require(!fixture.service.hasPendingBoxForCurrentWindow(fixture.context("UNKNOWN", 1L), "xiuluo_v2"),
                "UNKNOWN task context role must fail closed for common-box consume");
    }

    private static void verifyTaskRunSequenceIsGlobal() throws Exception {
        Field field = WindowTaskRunner.class.getDeclaredField("GLOBAL_TASK_RUN_SEQUENCE");
        int modifiers = field.getModifiers();
        require(Modifier.isStatic(modifiers), "Task run sequence must be static/global across runner rebuilds");
        require(Modifier.isFinal(modifiers), "Task run sequence global holder should be final");
        require(AtomicLong.class.equals(field.getType()), "Task run sequence must remain an AtomicLong");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Fixture {
        private final BotProperties properties = new BotProperties();
        private final WindowTaskContextHolder holder = new WindowTaskContextHolder(new WindowIsolationProperties());
        private final CommonBoxService service = new CommonBoxService(properties, null, null, holder);
        private final WindowRuntimeContext window = new WindowRuntimeContext("common-box-window", new GameContext());

        private Fixture() {
            properties.setLeaderCommonBoxEnabled(true);
            properties.setMemberCommonBoxEnabled(true);
            window.setNativeBinding(new WindowNativeBinding("1", "player-title", "xy2", 1L, 100, 200, 1024, 768));
            holder.bind(window);
        }

        private TaskExecutionContext context(String role, long taskRunId) {
            return TaskExecutionContext.builder()
                    .taskCode("xiuluo_v2")
                    .requestedTaskCode("xiuluo_v2")
                    .windowId(window.getWindowId())
                    .windowRole(role)
                    .taskRunId(taskRunId)
                    .build();
        }

        private void addPending(String taskRunKey, long identityEpoch, long expiresAtMs) throws Exception {
            pendingMap().put(pendingKey(taskRunKey), pending(taskRunKey, identityEpoch, expiresAtMs));
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> pendingMap() throws Exception {
            Field field = CommonBoxService.class.getDeclaredField("pendingByKey");
            field.setAccessible(true);
            return (Map<String, Object>) field.get(service);
        }

        private String pendingKey(String taskRunKey) {
            return window.getWindowId() + "|1|" + CommonBoxRole.LEADER + "|xiuluo_v2|" + taskRunKey;
        }
    }

    private static Object pending(String taskRunKey, long identityEpoch, long expiresAtMs) throws Exception {
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
                "xiuluo_v2",
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
}
