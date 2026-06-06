package com.bot.dhxy.debug;

import com.bot.dhxy.AutoBot;
import com.bot.dhxy.model.ocr.LocationInfo;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.vision.LocationVisionService;
import com.bot.dhxy.window.discovery.NativeWindowInfo;
import com.bot.dhxy.window.discovery.NativeWindowScanner;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.model.WindowRole;
import com.bot.dhxy.window.runtime.WindowRegistrationRequest;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * Live no-input probe for the mini-map label/template path.
 *
 * <p>This diagnostic binds each scanned game window as a normal window runtime context and then
 * calls {@link LocationVisionService#scanCurrentLocation()}. It only captures screenshots/OCR; it
 * must not send keyboard or mouse input.</p>
 */
public class MiniMapLabelLiveProbeDebugMain {

    private static final List<String> TARGET_NAMES = List.of("『忍者』影", "刑部ㄨ忍者", "うprinoe大叔");

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AutoBot.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .properties(
                        "bot.run.show-ui=false",
                        "bot.run.auto-start=false"
                )
                .run(args)) {

            NativeWindowScanner scanner = context.getBean(NativeWindowScanner.class);
            LocationVisionService locationVisionService = context.getBean(LocationVisionService.class);
            WindowTaskContextHolder holder = context.getBean(WindowTaskContextHolder.class);
            com.bot.dhxy.core.GameContext gameContext = context.getBean(com.bot.dhxy.core.GameContext.class);

            List<NativeWindowInfo> windows = scanner.scanGameWindows();
            System.out.println("MINIMAP_PROBE windows=" + windows.size());
            for (String targetName : TARGET_NAMES) {
                NativeWindowInfo window = windows.stream()
                        .filter(value -> value.getTitle().contains(targetName))
                        .findFirst()
                        .orElse(null);
                if (window == null) {
                    System.out.println("MINIMAP_PROBE target=" + targetName + " found=false");
                    continue;
                }

                WindowRuntimeContext runtime = new WindowRuntimeContext(window.toWindowId(), gameContext);
                runtime.applyRegistration(WindowRegistrationRequest.of(
                        window.toWindowId(),
                        WindowRole.UNKNOWN,
                        window.toDisplayName(),
                        TaskType.UNKNOWN,
                        toBinding(window)
                ), true);

                LocationInfo location = holder.callWith(runtime, locationVisionService::scanCurrentLocation);
                System.out.println("MINIMAP_PROBE target=" + targetName
                        + " windowId=" + window.toWindowId()
                        + " title=" + window.getTitle()
                        + " result=" + (location == null ? "null" : location));
            }
        }
    }

    private static WindowNativeBinding toBinding(NativeWindowInfo window) {
        return new WindowNativeBinding(
                "0x" + window.getHandle(),
                window.getTitle(),
                window.getClassName(),
                window.getProcessId(),
                window.getX(),
                window.getY(),
                window.getWidth(),
                window.getHeight()
        );
    }
}
