package com.bot.dhxy.cloud.turn.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnCoreProtocolGoldenJsonTest {

    @Test
    void closedEnumsRemainExact() {
        assertArrayEquals(new String[]{"CAPTURE", "MATCH_TEMPLATE", "INPUT", "WAIT", "LOCAL_SERVICE"},
                names(TurnStepType.values()));
        assertArrayEquals(new String[]{"MOVE_MOUSE", "CLICK_LEFT", "CLICK_RIGHT", "DOUBLE_CLICK_LEFT",
                        "DOUBLE_CLICK_RIGHT", "DRAG_LEFT", "SCROLL", "KEY_TAP", "KEY_DOWN", "KEY_UP",
                        "TEXT_INPUT"},
                names(TurnInputAction.values()));
        assertArrayEquals(new String[]{"BAG_RETURN_ITEM", "BAG_USE_INCENSE", "UI_CLEAN_ALL",
                        "UI_CLOSE_GENERIC_WINDOWS", "UI_CLEAN_LIGHTWEIGHT", "UI_CLOSE_MAP_SEARCH_INPUT_BY_X2",
                        "GIVE_ITEM_FROM_OPEN_DIALOG", "QUEST_ACTIVATE", "QUEST_CAPTURE_DETAIL"},
                names(TurnLocalOperation.values()));
        assertArrayEquals(new String[]{"CAPTURE", "MATCH_EVIDENCE", "QUEST_DETAIL", "FAILURE_EVIDENCE"},
                names(TurnFramePurpose.values()));
        assertArrayEquals(new String[]{"UPLOAD_IMAGE", "NO_IMAGE"},
                names(TurnCaptureSpec.ResultMode.values()));
        assertArrayEquals(new String[]{"NONE", "CLICK"}, names(TurnMatchSpec.OnMatch.values()));
        assertArrayEquals(new String[]{"RETURN_MATCH_RESULT", "RETURN_MATCH_RESULT_AND_IMAGE"},
                names(TurnMatchSpec.ResultMode.values()));
        assertArrayEquals(new String[]{"COMPLETED", "FAILED", "STOPPED", "DUPLICATE_OR_UNCERTAIN"},
                names(TurnOutcome.Status.values()));
        assertArrayEquals(new String[]{"COMPLETED", "FAILED", "NOT_RUN"},
                names(TurnStepResult.Status.values()));
        assertArrayEquals(new String[]{"ACTION", "IDLE"}, names(TurnResponse.Status.values()));
        assertArrayEquals(new String[]{"WUHUAN_V2", "WUBEI", "XIULUO_V2", "AUTO_BATTLE"},
                names(TurnTaskCode.values()));
        assertArrayEquals(new String[]{"CONTINUE_ON_FAILURE", "STOP_ON_FAILURE"},
                names(TurnTaskQueueFailurePolicy.values()));
        assertArrayEquals(new String[]{"PRESCAN_TASK_PAGE", "PRESCAN_FROM_BACK", "USE_CACHED_RETURN_ITEM"},
                names(TurnBagOperationArguments.ReturnItemIntent.values()));
    }

    @Test
    void allFiveStepKindsAndElevenInputActionsRoundTripThroughStrictContractMapper() throws IOException {
        List<TurnInputAction> inputActions = List.of(TurnInputAction.values());
        List<TurnInputSpec> inputSpecs = List.of(
                new TurnInputSpec(100, 200, null, null, null, null, null),
                new TurnInputSpec(101, 201, null, null, null, null, null),
                new TurnInputSpec(102, 202, null, null, null, null, null),
                new TurnInputSpec(103, 203, null, null, null, null, null),
                new TurnInputSpec(104, 204, null, null, null, null, null),
                new TurnInputSpec(105, 205, 305, 405, null, null, null),
                new TurnInputSpec(106, 206, null, null, -3, null, null),
                new TurnInputSpec(null, null, null, null, null, "ALT_E", null),
                new TurnInputSpec(null, null, null, null, null, "CTRL", null),
                new TurnInputSpec(null, null, null, null, null, "CTRL", null),
                new TurnInputSpec(null, null, null, null, null, null, "Changan"));

        List<TurnStep> steps = new ArrayList<>();
        for (int index = 0; index < inputActions.size(); index++) {
            steps.add(TurnProtocolGoldenSupport.inputStep(index, inputActions.get(index), inputSpecs.get(index)));
        }
        steps.add(TurnProtocolGoldenSupport.waitStep(11, 800L));
        steps.add(TurnProtocolGoldenSupport.captureStep(12,
                new TurnCaptureSpec(new TurnRegion(20, 30, 640, 480), TurnCaptureSpec.ResultMode.NO_IMAGE)));
        steps.add(TurnProtocolGoldenSupport.matchStep(13,
                new TurnMatchSpec(new TurnRegion(25, 35, 80, 40), "dialog/example", TurnProtocolGoldenSupport.SHA_A,
                        0.92D, TurnMatchSpec.OnMatch.NONE, TurnMatchSpec.ResultMode.RETURN_MATCH_RESULT)));
        steps.add(TurnProtocolGoldenSupport.localStep(14,
                new TurnLocalServiceCall(TurnLocalOperation.BAG_USE_INCENSE, null, null, null, null)));

        TurnAction action = TurnProtocolGoldenSupport.action("core-all-step-kinds", steps);
        assertSame(action, TurnProtocolValidator.requireValid(action));
        assertEquals(EnumSet.allOf(TurnStepType.class), action.steps().stream()
                .map(TurnStep::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TurnStepType.class))));
        assertEquals(inputActions, action.steps().subList(0, 11).stream().map(TurnStep::inputAction).toList());

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);
        assertEquals(action, roundTripped);
        TurnProtocolValidator.requireValid(roundTripped);
    }

    @Test
    void pointerClearRetainsSignedScreenCoordinatesWithoutAnyScaleField() throws IOException {
        TurnCaptureSpec.ClearPointerIfOverRegion clear =
                new TurnCaptureSpec.ClearPointerIfOverRegion(128, -700, 400, 5_000);
        TurnAction action = TurnProtocolGoldenSupport.action(
                "core-pointer-clear-negative-monitor",
                List.of(TurnProtocolGoldenSupport.captureStep(
                        0,
                        new TurnCaptureSpec(
                                new TurnRegion(-1_500, -200, 100, 80),
                                TurnCaptureSpec.ResultMode.UPLOAD_IMAGE,
                                clear))));

        TurnAction roundTripped = TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class);

        TurnProtocolValidator.requireValid(roundTripped);
        TurnCaptureSpec.ClearPointerIfOverRegion actual =
                roundTripped.steps().get(0).capture().clearPointerIfOverRegion();
        assertEquals(128, actual.paddingPx());
        assertEquals(-700, actual.targetX());
        assertEquals(400, actual.targetY());
        assertEquals(5_000, actual.settleMs());
        JsonNode clearJson = TurnProtocolGoldenSupport.STRICT_CONTRACT_MAPPER
                .valueToTree(roundTripped)
                .path("steps")
                .path(0)
                .path("capture")
                .path("clearPointerIfOverRegion");
        assertFalse(clearJson.has("systemScaleRatio"));
    }

    @Test
    void allNineLocalOperationsRetainTheirTypedArgumentUnion() throws IOException {
        List<TurnLocalServiceCall> calls = List.of(
                new TurnLocalServiceCall(TurnLocalOperation.BAG_RETURN_ITEM,
                        new TurnBagOperationArguments(TurnBagOperationArguments.ReturnItemIntent.USE_CACHED_RETURN_ITEM,
                                null, -1, new TurnReturnItemCachePoint(
                                "items/return.png", 1320, 760, 123_456_789L, "golden"), "golden"),
                        null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.BAG_USE_INCENSE, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLEAN_ALL, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLOSE_GENERIC_WINDOWS, null, null, null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLEAN_LIGHTWEIGHT, null,
                        new TurnUiOperationArguments("golden"), null, null),
                new TurnLocalServiceCall(TurnLocalOperation.UI_CLOSE_MAP_SEARCH_INPUT_BY_X2, null,
                        new TurnUiOperationArguments("golden"), null, null),
                new TurnLocalServiceCall(TurnLocalOperation.GIVE_ITEM_FROM_OPEN_DIALOG, null, null,
                        new TurnGiveItemOperationArguments("items/shoe.png", 2), null),
                new TurnLocalServiceCall(TurnLocalOperation.QUEST_ACTIVATE, null, null, null,
                        new TurnQuestOperationArguments("wuhuan", Boolean.TRUE)),
                new TurnLocalServiceCall(TurnLocalOperation.QUEST_CAPTURE_DETAIL, null, null, null,
                        new TurnQuestOperationArguments("wuhuan", null)));

        List<TurnStep> steps = new ArrayList<>();
        for (int index = 0; index < calls.size(); index++) {
            steps.add(TurnProtocolGoldenSupport.localStep(index, calls.get(index)));
        }
        TurnAction action = TurnProtocolGoldenSupport.action("core-all-local-operations", steps);
        TurnProtocolValidator.requireValid(action);
        assertEquals(List.of(TurnLocalOperation.values()), action.steps().stream()
                .map(step -> step.localService().operation())
                .toList());
        assertEquals(action, TurnProtocolGoldenSupport.roundTrip(action, TurnAction.class));
    }

    @Test
    void nonZeroWindowOriginAndPauseStopFlagsRoundTripExactly() throws IOException {
        TurnWindowMetadata window = TurnProtocolGoldenSupport.window(true, false);
        TurnRequest request = new TurnRequest(1, window, 25_000L, null, null);

        TurnRequest roundTripped = TurnProtocolGoldenSupport.roundTrip(request, TurnRequest.class);
        TurnProtocolValidator.requireValid(roundTripped);
        assertEquals(120, roundTripped.window().windowRect().left());
        assertEquals(80, roundTripped.window().windowRect().top());
        assertEquals(1280, roundTripped.window().windowRect().width());
        assertEquals(720, roundTripped.window().windowRect().height());
        assertEquals(true, roundTripped.window().pauseRequested());
        assertFalse(roundTripped.window().stopRequested());
    }

    @Test
    void lifecycleTaskCodeEnumDoesNotContainSleepComputer() {
        assertThrows(IllegalArgumentException.class, () -> TurnTaskCode.valueOf("SLEEP_COMPUTER"));
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }
}

final class TurnProtocolGoldenSupport {

    static final String SHA_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    static final String SHA_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    static final ObjectMapper STRICT_CONTRACT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private TurnProtocolGoldenSupport() {
    }

    static TurnWindowMetadata window(boolean pauseRequested, boolean stopRequested) {
        return new TurnWindowMetadata(
                "device-alpha",
                "window-2",
                "Classic Client - Alpha",
                "0x000000000001A2B3",
                4242L,
                new TurnWindowRect(120, 80, 1280, 720),
                pauseRequested,
                stopRequested);
    }

    static TurnAction action(String actionId, List<TurnStep> steps) {
        return new TurnAction(1, actionId, "device-alpha", "window-2", List.copyOf(steps), true);
    }

    static TurnStep inputStep(int index, TurnInputAction action, TurnInputSpec input) {
        return new TurnStep(index, TurnStepType.INPUT, action, input, null, null, null, null);
    }

    static TurnStep waitStep(int index, long waitMs) {
        return new TurnStep(index, TurnStepType.WAIT, null, null, waitMs, null, null, null);
    }

    static TurnStep captureStep(int index, TurnCaptureSpec capture) {
        return new TurnStep(index, TurnStepType.CAPTURE, null, null, null, capture, null, null);
    }

    static TurnStep matchStep(int index, TurnMatchSpec match) {
        return new TurnStep(index, TurnStepType.MATCH_TEMPLATE, null, null, null, null, match, null);
    }

    static TurnStep localStep(int index, TurnLocalServiceCall localService) {
        return new TurnStep(index, TurnStepType.LOCAL_SERVICE, null, null, null, null, null, localService);
    }

    static <T> T readFixture(String name, Class<T> type) throws IOException {
        return STRICT_CONTRACT_MAPPER.readValue(fixtureBytes(name), type);
    }

    static <T> T assertFixtureRoundTrip(String name, Class<T> type) throws IOException {
        byte[] bytes = fixtureBytes(name);
        JsonNode expected = STRICT_CONTRACT_MAPPER.readTree(bytes);
        T value = STRICT_CONTRACT_MAPPER.readValue(bytes, type);
        JsonNode actual = STRICT_CONTRACT_MAPPER.readTree(STRICT_CONTRACT_MAPPER.writeValueAsBytes(value));
        assertEquals(expected, actual, "strict contract mapper changed canonical fixture fields");
        return value;
    }

    static <T> T roundTrip(T value, Class<T> type) throws IOException {
        return STRICT_CONTRACT_MAPPER.readValue(STRICT_CONTRACT_MAPPER.writeValueAsBytes(value), type);
    }

    static byte[] fixtureBytes(String name) throws IOException {
        String resource = "cloud-turn/v1/" + name;
        ClassLoader loader = TurnProtocolGoldenSupport.class.getClassLoader();
        try (InputStream input = Objects.requireNonNull(loader.getResourceAsStream(resource),
                "missing fixture " + resource)) {
            return input.readAllBytes();
        }
    }
}
