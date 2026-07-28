package com.bot.dhxy.vision;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Executes one archived Git version of GameTextLineOcrService against the fixed route images.
 *
 * <p>The test deliberately uses reflection because the route API evolved across the archived
 * versions. It never substitutes the current implementation for an older one.</p>
 */
public class GitRouteVersionReplayTest {

    public static void main(String[] args) throws Exception {
        new GitRouteVersionReplayTest().replayFixedRouteImages();
    }

    void replayFixedRouteImages() throws Exception {
        Path outputDir = Path.of(required("ROUTE_REPLAY_OUTPUT"));
        Files.createDirectories(outputDir);
        replay(Path.of(required("ROUTE_REPLAY_INPUT_A")), outputDir.resolve("case-a-marked.png"),
                "case-a", "长安");
        replay(Path.of(required("ROUTE_REPLAY_INPUT_B")), outputDir.resolve("case-b-marked.png"),
                "case-b", "长安");
        replay(Path.of(required("ROUTE_REPLAY_INPUT_C")), outputDir.resolve("case-c-marked.png"),
                "case-c", "凤巢七层");
    }

    private void replay(Path input, Path output, String caseName, String expectedDestination) throws Exception {
        BufferedImage raw = ImageIO.read(input.toFile());
        if (raw == null) {
            throw new IllegalArgumentException("Unreadable input: " + input);
        }

        Class<?> serviceType = Class.forName("com.bot.dhxy.vision.GameTextLineOcrService");
        Method verify;
        try {
            verify = serviceType.getMethod("verifyWorldMapRouteDestination", String.class, String.class);
        } catch (NoSuchMethodException unsupported) {
            annotate(raw, output, null, null, false, "UNSUPPORTED: no route destination API");
            writeResult(caseName, "UNSUPPORTED", null, null, null, output);
            return;
        }

        Object service;
        try {
            service = createService(serviceType);
        } catch (ReflectiveOperationException unsupportedArchitecture) {
            annotate(raw, output, null, null, false,
                    "UNSUPPORTED_ARCHITECTURE: " + concise(unsupportedArchitecture));
            writeResult(caseName, "UNSUPPORTED_ARCHITECTURE", null, null,
                    concise(unsupportedArchitecture), output);
            return;
        }

        Object result = verify.invoke(service, input.toString(), expectedDestination);
        boolean matched = booleanValue(result, "matched");
        Integer centerX = integerValue(result, "destinationCenterX");
        Integer centerY = integerValue(result, "destinationCenterY");
        String actual = stringValue(result, "rawActual");
        String status = matched
                ? (centerX != null && centerY != null ? "MATCH_WITH_POINT" : "MATCH_BUT_NO_CLICK_POINT")
                : "MISS";
        annotate(raw, output, centerX, centerY, matched,
                status + " actual=" + (actual == null ? "" : actual));
        writeResult(caseName, status, centerX, centerY, actual, output);
    }

    private Object createService(Class<?> serviceType) throws ReflectiveOperationException {
        Class<?> propertiesType = Class.forName("com.bot.dhxy.config.BotProperties");
        Object properties = propertiesType.getConstructor().newInstance();
        Object ocr = propertiesType.getMethod("getOcr").invoke(properties);
        invokeSetter(ocr, "setProvider", "local");
        // Historical TextRecognizer posts the legacy JSON field accepted by the compatibility port.
        invokeSetter(ocr, "setLocalEndpoint", "http://127.0.0.1:18762");
        invokeSetter(ocr, "setLocalTimeoutMs", 30_000);

        Class<?> recognizerType = Class.forName("com.bot.dhxy.core.TextRecognizer");
        Object recognizer = recognizerType.getConstructor(propertiesType).newInstance(properties);
        for (Constructor<?> constructor : serviceType.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 1
                    && constructor.getParameterTypes()[0].isAssignableFrom(recognizerType)) {
                constructor.setAccessible(true);
                return constructor.newInstance(recognizer);
            }
            if (constructor.getParameterCount() == 2
                    && constructor.getParameterTypes()[0].isAssignableFrom(recognizerType)) {
                constructor.setAccessible(true);
                return constructor.newInstance(recognizer, createArchivedCloudImageProcessor());
            }
        }
        throw new NoSuchMethodException("unsupported archived service constructor");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object createArchivedCloudImageProcessor() throws ReflectiveOperationException {
        Class<?> propertiesType = Class.forName("com.bot.dhxy.cloud.decision.CloudDecisionProperties");
        Object properties = propertiesType.getConstructor().newInstance();
        invokeSetter(properties, "setEnabled", true);
        invokeSetter(properties, "setRealTransportEnabled", true);
        invokeSetter(properties, "setBaseUrl", "http://127.0.0.1:18082");
        invokeSetter(properties, "setEndpointPath", "/api/cloud/decision");
        invokeSetter(properties, "setToken", "local-dev-token");
        invokeSetter(properties, "setTimeoutMs", 60_000L);

        Class<? extends Enum> serviceIdType =
                (Class<? extends Enum>) Class.forName("com.bot.dhxy.cloud.decision.CloudDecisionServiceId");
        Object imagePreprocessId = Enum.valueOf(serviceIdType, "IMAGE_PREPROCESS");
        Object servicePolicy = propertiesType.getMethod("service", serviceIdType)
                .invoke(properties, imagePreprocessId);
        invokeSetter(servicePolicy, "setShadowEnabled", true);
        invokeSetter(servicePolicy, "setExecuteEnabled", true);
        invokeSetter(servicePolicy, "setExecutePercent", 100);

        Class<?> httpClientType = Class.forName("com.bot.dhxy.cloud.decision.HttpCloudDecisionClient");
        Object httpClient = httpClientType.getConstructor(propertiesType).newInstance(properties);
        Class<?> metricsType = Class.forName("com.bot.dhxy.cloud.decision.CloudDecisionMetricsService");
        Object metrics = metricsType.getConstructor().newInstance();
        Class<?> clientInterface = Class.forName("com.bot.dhxy.cloud.decision.CloudDecisionClient");
        Class<?> coordinatorType = Class.forName("com.bot.dhxy.cloud.decision.CloudDecisionCoordinator");
        Object coordinator = coordinatorType
                .getConstructor(propertiesType, clientInterface, metricsType)
                .newInstance(properties, httpClient, metrics);

        Class<?> preprocessServiceType = Class.forName("com.bot.dhxy.cloud.task.ImagePreprocessCloudService");
        Object preprocessService = preprocessServiceType.getConstructor(coordinatorType).newInstance(coordinator);
        Class<?> contextHolderType = Class.forName("com.bot.dhxy.runner.context.TaskExecutionContextHolder");
        Object contextHolder = contextHolderType.getConstructor().newInstance();
        Class<?> washedClientType = Class.forName("com.bot.dhxy.cloud.task.ImagePreprocessWashedImageClient");
        Object washedClient = washedClientType
                .getConstructor(preprocessServiceType, contextHolderType)
                .newInstance(preprocessService, contextHolder);
        Class<?> cloudProcessorType = Class.forName("com.bot.dhxy.cloud.task.CloudImageProcessor");
        return cloudProcessorType.getConstructor(washedClientType).newInstance(washedClient);
    }

    private void invokeSetter(Object target, String name, Object value) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                method.invoke(target, value);
                return;
            }
        }
    }

    private boolean booleanValue(Object result, String methodName) throws ReflectiveOperationException {
        Method method = findNoArgMethod(result.getClass(), methodName);
        return method != null && Boolean.TRUE.equals(method.invoke(result));
    }

    private Integer integerValue(Object result, String methodName) throws ReflectiveOperationException {
        Method method = findNoArgMethod(result.getClass(), methodName);
        if (method == null) {
            return null;
        }
        Object value = method.invoke(result);
        return value instanceof Number number ? number.intValue() : null;
    }

    private String stringValue(Object result, String methodName) throws ReflectiveOperationException {
        Method method = findNoArgMethod(result.getClass(), methodName);
        Object value = method == null ? null : method.invoke(result);
        return value == null ? null : value.toString();
    }

    private Method findNoArgMethod(Class<?> type, String name) {
        return List.of(type.getMethods()).stream()
                .filter(method -> method.getName().equals(name) && method.getParameterCount() == 0)
                .findFirst()
                .orElse(null);
    }

    private void annotate(BufferedImage raw,
                          Path output,
                          Integer centerX,
                          Integer centerY,
                          boolean matched,
                          String label) throws Exception {
        BufferedImage marked = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = marked.createGraphics();
        graphics.drawImage(raw, 0, 0, null);
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(3f));
        if (centerX != null && centerY != null) {
            graphics.drawOval(centerX - 10, centerY - 10, 20, 20);
            graphics.drawLine(centerX - 15, centerY, centerX + 15, centerY);
            graphics.drawLine(centerX, centerY - 15, centerX, centerY + 15);
        } else if (!matched) {
            graphics.drawRect(2, 2, Math.max(1, raw.getWidth() - 5), Math.max(1, raw.getHeight() - 5));
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        int bannerHeight = 24;
        graphics.fillRect(0, Math.max(0, raw.getHeight() - bannerHeight), raw.getWidth(), bannerHeight);
        graphics.setColor(Color.WHITE);
        graphics.drawString(label, 5, raw.getHeight() - 7);
        graphics.dispose();
        ImageIO.write(marked, "png", output.toFile());
    }

    private void writeResult(String caseName,
                             String status,
                             Integer centerX,
                             Integer centerY,
                             String actual,
                             Path output) throws Exception {
        String line = String.join("\t",
                caseName,
                status,
                centerX == null ? "" : centerX.toString(),
                centerY == null ? "" : centerY.toString(),
                actual == null ? "" : actual,
                output.toString());
        Files.writeString(output.resolveSibling(caseName + ".tsv"), line + System.lineSeparator());
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }

    private String concise(Throwable throwable) {
        String text = throwable.getMessage();
        return text == null ? throwable.getClass().getSimpleName() : text.replace('\n', ' ');
    }
}
