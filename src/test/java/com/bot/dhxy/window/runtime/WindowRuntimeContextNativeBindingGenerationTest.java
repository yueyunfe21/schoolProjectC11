package com.bot.dhxy.window.runtime;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.window.model.WindowNativeBinding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class WindowRuntimeContextNativeBindingGenerationTest {

    @Test
    void equivalentRefreshPreservesExactGenerationButGeometryChangeReplacesIt() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-generation", new GameContext());
        WindowNativeBinding original = binding(10, 20, 1024, 768);
        context.setNativeBinding(original);

        context.setNativeBinding(binding(10, 20, 1024, 768));
        assertSame(original, context.getNativeBinding(),
                "an equivalent capture refresh must preserve the frozen input generation");

        context.setNativeBinding(binding(11, 20, 1024, 768));
        assertNotSame(original, context.getNativeBinding(),
                "a real geometry change must create a new generation and invalidate stale input");
    }

    @Test
    void firstNativeBindingImmediatelyPopulatesPlayerIdentity() {
        WindowRuntimeContext context = new WindowRuntimeContext("window-identity", new GameContext());

        context.setNativeBinding(new WindowNativeBinding(
                "4379326",
                "大话西游2经典版 $Revision: 2039941 - 江山如画 - 乌龟的黑头° (ID: 67555)",
                "xy2",
                42L,
                0,
                0,
                1024,
                768));

        assertEquals("乌龟的黑头°", context.getGameState().getMe().getName());
        assertEquals("江山如画", context.getGameState().getMe().getGameServerName());
        assertEquals("67555", context.getGameState().getMe().getId());
    }

    private static WindowNativeBinding binding(int x, int y, int width, int height) {
        return new WindowNativeBinding(
                "4379326",
                "大话西游2经典版 - 测试窗口",
                "xy2",
                42L,
                x,
                y,
                width,
                height);
    }
}
