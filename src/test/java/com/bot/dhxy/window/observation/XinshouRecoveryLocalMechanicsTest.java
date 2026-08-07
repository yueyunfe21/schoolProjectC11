package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class XinshouRecoveryLocalMechanicsTest {

    @Test
    void noInvocationProducesNoInput() {
        CountingInput input = new CountingInput(true);
        AtomicInteger resolutions = new AtomicInteger();
        new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            return XinshouRecoveryLocalMechanics.TargetResolution.matched(new Point(640, 430));
        }, input);

        assertEquals(0, resolutions.get());
        assertEquals(0, input.totalActions());
    }

    @Test
    void escapeRequestAppliesExactlyOneEscapeWithoutStatefulSuppression() {
        CountingInput input = new CountingInput(true);
        XinshouRecoveryLocalMechanics mechanics = mechanics(
                XinshouRecoveryLocalMechanics.TargetResolution.of(
                        XinshouRecoveryLocalMechanics.ResolutionStatus.TEMPLATE_NOT_MATCHED),
                input);

        assertEquals(
                XinshouRecoveryLocalMechanics.Status.INPUT_APPLIED,
                mechanics.pressEscapeOnce().status());
        assertEquals(1, input.escapeActions.get());
        assertEquals(0, input.templateActions.get());

        assertEquals(
                XinshouRecoveryLocalMechanics.Status.INPUT_APPLIED,
                mechanics.pressEscapeOnce().status());
        assertEquals(2, input.escapeActions.get(),
                "each explicit Cloud request is independent; there is no consumed state");
    }

    @Test
    void templateRequestResolvesOnceAndSubmitsOneAtomicClick() {
        AtomicInteger resolutions = new AtomicInteger();
        CountingInput input = new CountingInput(true);
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            assertEquals("confirm.png", template.templateName());
            return XinshouRecoveryLocalMechanics.TargetResolution.matched(new Point(640, 430));
        }, input);

        XinshouRecoveryLocalMechanics.Result result = mechanics.matchAndClickOnce("confirm.png");

        assertEquals(XinshouRecoveryLocalMechanics.Status.INPUT_APPLIED, result.status());
        assertEquals(1, resolutions.get());
        assertEquals(1, input.templateActions.get());
        assertEquals(new Point(640, 430), input.lastPoint.get());
    }

    @Test
    void skipTemplateUsesTheTopRightUnionAndSubmitsOneAtomicLeftClick() {
        AtomicInteger resolutions = new AtomicInteger();
        CountingInput input = new CountingInput(true);
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            assertEquals("tiaoguo.png", template.templateName());
            assertEquals(870, template.left());
            assertEquals(57, template.top());
            assertEquals(128, template.width());
            assertEquals(49, template.height());
            return XinshouRecoveryLocalMechanics.TargetResolution.matched(new Point(2290, 336));
        }, input);

        XinshouRecoveryLocalMechanics.Result result = mechanics.matchAndClickOnce("tiaoguo.png");

        assertEquals(XinshouRecoveryLocalMechanics.Status.INPUT_APPLIED, result.status());
        assertEquals(1, resolutions.get());
        assertEquals(1, input.templateActions.get());
        assertEquals(new Point(2290, 336), input.lastPoint.get());
    }

    @Test
    void templateMissDoesNotSubmitInputOrRetry() {
        AtomicInteger resolutions = new AtomicInteger();
        CountingInput input = new CountingInput(true);
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            return XinshouRecoveryLocalMechanics.TargetResolution.of(
                    XinshouRecoveryLocalMechanics.ResolutionStatus.TEMPLATE_NOT_MATCHED);
        }, input);

        assertEquals(
                XinshouRecoveryLocalMechanics.Status.TEMPLATE_NOT_MATCHED,
                mechanics.matchAndClickOnce("quedingguan_.png").status());
        assertEquals(1, resolutions.get());
        assertEquals(0, input.totalActions());
    }

    @Test
    void unavailableTemplateDoesNotSubmitInputOrRetry() {
        AtomicInteger resolutions = new AtomicInteger();
        CountingInput input = new CountingInput(true);
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            return XinshouRecoveryLocalMechanics.TargetResolution.of(
                    XinshouRecoveryLocalMechanics.ResolutionStatus.TEMPLATE_UNAVAILABLE);
        }, input);

        assertEquals(
                XinshouRecoveryLocalMechanics.Status.TEMPLATE_UNAVAILABLE,
                mechanics.matchAndClickOnce("confirm.png").status());
        assertEquals(1, resolutions.get());
        assertEquals(0, input.totalActions());
    }

    @Test
    void inputFailureIsReturnedAfterOneAttemptWithoutRetry() {
        AtomicInteger resolutions = new AtomicInteger();
        CountingInput input = new CountingInput(false);
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            return XinshouRecoveryLocalMechanics.TargetResolution.matched(new Point(640, 430));
        }, input);

        assertEquals(
                XinshouRecoveryLocalMechanics.Status.INPUT_FAILED,
                mechanics.matchAndClickOnce("confirm.png").status());
        assertEquals(1, resolutions.get());
        assertEquals(1, input.templateActions.get());
    }

    @Test
    void unsupportedTemplateDoesNotCaptureOrSubmitInput() {
        AtomicInteger resolutions = new AtomicInteger();
        CountingInput input = new CountingInput(true);
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolutions.incrementAndGet();
            return XinshouRecoveryLocalMechanics.TargetResolution.matched(new Point(640, 430));
        }, input);

        assertEquals(
                XinshouRecoveryLocalMechanics.Status.UNSUPPORTED_TEMPLATE,
                mechanics.matchAndClickOnce("../arbitrary.png").status());
        assertEquals(0, resolutions.get());
        assertEquals(0, input.totalActions());
    }

    @Test
    void mechanicsRunSynchronouslyAndOwnNoBackgroundExecutor() {
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> resolverThread = new AtomicReference<>();
        AtomicReference<Thread> inputThread = new AtomicReference<>();
        XinshouRecoveryLocalMechanics mechanics = new XinshouRecoveryLocalMechanics(template -> {
            resolverThread.set(Thread.currentThread());
            return XinshouRecoveryLocalMechanics.TargetResolution.matched(new Point(640, 430));
        }, new CountingInput(true) {
            @Override
            public boolean clickTemplate(String templateName, Point absolutePoint) {
                inputThread.set(Thread.currentThread());
                return super.clickTemplate(templateName, absolutePoint);
            }
        });

        mechanics.matchAndClickOnce("confirm.png");

        assertSame(caller, resolverThread.get());
        assertSame(caller, inputThread.get());
        assertFalse(Arrays.stream(XinshouRecoveryLocalMechanics.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)
                .anyMatch(type -> Thread.class.isAssignableFrom(type)
                        || Executor.class.isAssignableFrom(type)));
    }

    @Test
    void observationRunnerSamplerAndFactoryDoNotReferenceRecoveryMechanics() throws IOException {
        Path oldAutonomousHandler = Path.of(
                "src/main/java/com/bot/dhxy/window/observation/XinshouLocalRecoveryHandler.java");
        String sampler = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java"));
        String runner = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/WindowObservationRunner.java"));
        String factory = Files.readString(Path.of(
                "src/main/java/com/bot/dhxy/window/observation/SpringObservationRunnerFactory.java"));

        assertFalse(Files.exists(oldAutonomousHandler));
        assertFalse(sampler.contains("XinshouRecoveryLocalMechanics"));
        assertFalse(sampler.contains("XinshouLocalRecoveryHandler"));
        assertFalse(runner.contains("XinshouRecoveryLocalMechanics"));
        assertFalse(runner.contains("XinshouLocalRecoveryHandler"));
        assertFalse(factory.contains("XinshouRecoveryLocalMechanics"));
        assertFalse(factory.contains("XinshouLocalRecoveryHandler"));
    }

    private static XinshouRecoveryLocalMechanics mechanics(
            XinshouRecoveryLocalMechanics.TargetResolution resolution,
            XinshouRecoveryLocalMechanics.RecoveryInput input) {
        return new XinshouRecoveryLocalMechanics(template -> resolution, input);
    }

    private static class CountingInput implements XinshouRecoveryLocalMechanics.RecoveryInput {
        private final boolean result;
        private final AtomicInteger escapeActions = new AtomicInteger();
        private final AtomicInteger templateActions = new AtomicInteger();
        private final AtomicReference<Point> lastPoint = new AtomicReference<>();

        private CountingInput(boolean result) {
            this.result = result;
        }

        @Override
        public boolean pressEscape() {
            escapeActions.incrementAndGet();
            return result;
        }

        @Override
        public boolean clickTemplate(String templateName, Point absolutePoint) {
            templateActions.incrementAndGet();
            lastPoint.set(absolutePoint);
            return result;
        }

        private int totalActions() {
            return escapeActions.get() + templateActions.get();
        }
    }
}
