package com.bot.dhxy.window.discovery;

import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeWindowRegistrationMapperIdentityTest {

    @Test
    void independentRegistrationUsesParsedPlayerNameAndPreservesFullNativeTitle() {
        NativeWindowRegistrationMapper mapper = new NativeWindowRegistrationMapper();
        String title = "大话西游2经典版 $Revision: 2039941 - 江山如画 - 乌龟的黑头° (ID: 67555)";
        NativeWindowInfo window = new NativeWindowInfo(
                "305DE", title, "xy2", 42L, 10, 20, 1024, 768);

        List<WindowRegistrationRequest> requests =
                mapper.toIndependentRegistrationRequests(List.of(window), TaskType.XIULUO_V2);

        assertEquals(1, requests.size());
        assertEquals("乌龟的黑头°", requests.get(0).getRoleName());
        assertEquals(title, requests.get(0).getNativeBinding().getTitle());
    }
}
