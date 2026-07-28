package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.window.observation.DeferredReturnHomeReplayCoordinator;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WholeTaskRuntimeLocalOperationExecutorSpringWiringTest {

    @Test
    void productionConstructorIsTheOnlyAutowiredInjectionCandidate() {
        Constructor<?>[] injectionConstructors = Arrays.stream(
                        WholeTaskRuntimeLocalOperationExecutor.class.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .toArray(Constructor<?>[]::new);

        assertEquals(1, injectionConstructors.length);
        assertTrue(injectionConstructors[0].canAccess(null));
        assertArrayEquals(new Class<?>[]{
                        WindowTaskContextHolder.class,
                        LocalMovementFactMechanics.class,
                        FiveRingAcceptDialogLocalOperation.class,
                        DeferredReturnHomeReplayCoordinator.class,
                        NpcArrivalFrameFifoLocalExecutor.class,
                        ObjectMapper.class
                },
                injectionConstructors[0].getParameterTypes());
    }
}
