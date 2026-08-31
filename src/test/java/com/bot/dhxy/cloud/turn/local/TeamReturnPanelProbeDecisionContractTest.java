package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.GameContext;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real {@link TeamReturnPanelLocalOperation} decision with real captured frames.
 *
 * <p>Every assertion goes through the production method, so deleting the settle window, the
 * distinct-fingerprint rule, or the HUD refutation makes these fail. G107 (2026-08-26 22:43:24) is
 * the frozen half-frame case: the leader pressed Alt+T, sampled 106 ms later while the member cards
 * were still undrawn, and closed the gate on that single miss. G065 (2026-08-12) is why the HUD
 * marker alone can never confirm completion: it clears the moment a member clicks the return
 * button, long before that member is actually back.</p>
 */
class TeamReturnPanelProbeDecisionContractTest {

    private static final Path FIXTURES = Path.of("images", "test-cases", "team-return", "g107");
    private static final String RUN_ID = "remote-turn-test:0:GHOST_KING";
    private static final String WINDOW_ID = "hwnd-170892";
    private static final String HWND = "1509522";

    /* Comfortably past the production settle window (900 ms) and frame gap (250 ms). */
    private static final long PAST_SETTLE_MS = 1_000L;
    private static final long PAST_FRAME_GAP_MS = 300L;

    @Test
    void theG107IncidentFrameNeverClosesTheGate() throws Exception {
        Harness harness = new Harness();
        // Exactly the incident state: four teammates still carried the HUD recall marker.
        harness.hudMarkerPresent = true;
        assertEquals(TeamReturnPanelLocalOperation.OpenResult.OPENED, harness.operation.openPanel(RUN_ID));

        BufferedImage half = read("leader_panel_half_rendered_roi.png");
        Thread.sleep(PAST_SETTLE_MS);
        for (int sample = 1; sample <= 5; sample++) {
            harness.panelFrame = half;
            assertNotEquals(TeamReturnPanelLocalOperation.ProbeResult.ALL_RETURNED,
                    harness.operation.probeAndCloseIfComplete(RUN_ID),
                    "sample " + sample + " of the incident frame must never report everyone back");
            Thread.sleep(PAST_FRAME_GAP_MS);
        }
        assertFalse(harness.closedPanel(), "the incident frame must never close the panel");
    }

    @Test
    void twoDistinctFramesInsideTheSettleWindowStillConcludeNothing() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = false;
        harness.operation.openPanel(RUN_ID);

        /*
         * Both frames are distinct, spaced past the frame gap, and carry no recall marker — the only
         * thing standing between them and a conclusion is the settle window. This is the G107 timing:
         * the incident sampled at 106 ms, and a panel that young has not finished drawing whatever it
         * happens to show.
         */
        harness.panelFrame = read("all_returned_roi_a.png");
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID));

        Thread.sleep(PAST_FRAME_GAP_MS);
        harness.panelFrame = read("all_returned_roi_b.png");
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID),
                "nothing sampled inside the settle window may conclude the gate");
        assertFalse(harness.closedPanel());
    }

    @Test
    void twoDistinctFramesSampledBackToBackAreNotTwoObservations() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = false;
        harness.operation.openPanel(RUN_ID);
        Thread.sleep(PAST_SETTLE_MS);

        /*
         * A panel drawing progressively produces frames that differ from each other while all of them
         * still lack the recall marker, so distinct content alone is not enough — counted frames must
         * also be spaced apart in time.
         */
        harness.panelFrame = read("all_returned_roi_a.png");
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID));
        harness.panelFrame = read("all_returned_roi_b.png");
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID),
                "a back-to-back sample is not an independent second observation");
        assertFalse(harness.closedPanel());
        assertFalse(harness.closedPanel());
    }

    @Test
    void aStaticAllReturnedPanelFromOneWindowStillCompletesNormally() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = false;
        harness.operation.openPanel(RUN_ID);

        /*
         * One window, one finished panel, byte-identical between samples — a real team panel may sit
         * perfectly still once drawn. The normal completion path has to survive that, which is why
         * negative evidence must not require the content to differ between frames.
         */
        BufferedImage settled = read("all_returned_roi_a.png");
        assertEquals(digestOf(settled), digestOf(read("all_returned_roi_a.png")),
                "this fixture must be re-read identically to model a still panel");

        Thread.sleep(PAST_SETTLE_MS);
        harness.panelFrame = settled;
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID),
                "one negative frame is not a confirmation");

        Thread.sleep(PAST_FRAME_GAP_MS);
        harness.panelFrame = settled;
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.ALL_RETURNED,
                harness.operation.probeAndCloseIfComplete(RUN_ID),
                "a still, fully drawn, marker-free panel must be able to finish the gate");
        assertTrue(harness.closedPanel(), "confirming all-returned must physically close the panel");
    }

    @Test
    void hudMarkerStillPresentRefusesCompletionAndKeepsThePanelOpen() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = true;
        harness.operation.openPanel(RUN_ID);

        Thread.sleep(PAST_SETTLE_MS);
        harness.panelFrame = read("all_returned_roi_a.png");
        harness.operation.probeAndCloseIfComplete(RUN_ID);
        Thread.sleep(PAST_FRAME_GAP_MS);
        harness.panelFrame = read("all_returned_roi_b.png");

        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.NOT_RETURNED_YET,
                harness.operation.probeAndCloseIfComplete(RUN_ID));
        assertFalse(harness.closedPanel(), "a teammate still marked for recall must keep the panel open");
    }

    @Test
    void fullyDrawnNotReturnedPanelReportsNotReturned() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = true;
        harness.operation.openPanel(RUN_ID);
        harness.panelFrame = read("not_returned_roi.png");

        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.NOT_RETURNED_YET,
                harness.operation.probeAndCloseIfComplete(RUN_ID));
    }

    @Test
    void captureFailureDropsAccumulatedNegativeEvidence() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = false;
        harness.operation.openPanel(RUN_ID);

        Thread.sleep(PAST_SETTLE_MS);
        harness.panelFrame = read("all_returned_roi_a.png");
        harness.operation.probeAndCloseIfComplete(RUN_ID);

        Thread.sleep(PAST_FRAME_GAP_MS);
        harness.panelFrame = null;
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID));

        Thread.sleep(PAST_FRAME_GAP_MS);
        harness.panelFrame = read("all_returned_roi_b.png");
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID),
                "the streak restarted, so this is only the first negative frame again");
        assertFalse(harness.closedPanel());
    }

    @Test
    void stoppingTheWindowForgetsPanelOwnershipAndEvidence() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = false;
        harness.operation.openPanel(RUN_ID);
        Thread.sleep(PAST_SETTLE_MS);
        harness.panelFrame = read("all_returned_roi_a.png");
        harness.operation.probeAndCloseIfComplete(RUN_ID);

        harness.operation.forgetWindowRealityMemory(WINDOW_ID);

        Thread.sleep(PAST_FRAME_GAP_MS);
        harness.panelFrame = read("all_returned_roi_b.png");
        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete(RUN_ID),
                "a stopped window owns no panel, so nothing may be concluded or closed");
        assertFalse(harness.closedPanel());
    }

    @Test
    void anotherTaskRunMayNotConcludeOnThisRunsPanel() throws Exception {
        Harness harness = new Harness();
        harness.hudMarkerPresent = false;
        harness.operation.openPanel(RUN_ID);
        Thread.sleep(PAST_SETTLE_MS);
        harness.panelFrame = read("all_returned_roi_a.png");

        assertEquals(TeamReturnPanelLocalOperation.ProbeResult.UNKNOWN,
                harness.operation.probeAndCloseIfComplete("remote-turn-other:0:GHOST_KING"));
    }

    private static String digestOf(BufferedImage image) {
        StringBuilder text = new StringBuilder();
        for (int y = 0; y < image.getHeight(); y += 7) {
            for (int x = 0; x < image.getWidth(); x += 7) {
                text.append(image.getRGB(x, y)).append(',');
            }
        }
        return Integer.toHexString(text.toString().hashCode());
    }

    private static BufferedImage read(String name) throws Exception {
        Path path = FIXTURES.resolve(name);
        assertTrue(Files.isReadable(path), "missing fixture: " + path);
        BufferedImage image = ImageIO.read(new File(path.toString()));
        assertTrue(image != null, "unreadable fixture: " + path);
        return image;
    }

    /** Real production object wired to stubs that only supply frames, geometry and input results. */
    private static final class Harness {

        private final TeamReturnPanelLocalOperation operation;
        private final List<String> submitted = new ArrayList<>();
        private BufferedImage panelFrame;
        private boolean hudMarkerPresent;

        private boolean closedPanel() {
            return submitted.stream().anyMatch(name -> name.contains("close"));
        }

        private Harness() throws Exception {
            WindowRuntimeContext context = new WindowRuntimeContext(WINDOW_ID, new GameContext());
            WindowNativeBinding binding =
                    new WindowNativeBinding(HWND, "title", "class", 1L, 0, 0, 1036, 783);
            BufferedImage hudPresent = read("hud_leader_signal_present.png");
            BufferedImage hudAbsent = read("all_returned_roi_a.png").getSubimage(0, 0, 272, 69);

            WindowTaskContextHolder contextHolder = new WindowTaskContextHolder(null) {
                @Override
                public Optional<WindowRuntimeContext> rawCurrent() {
                    return Optional.of(context);
                }
            };
            WindowNativeBindingRefreshService refreshService = new WindowNativeBindingRefreshService() {
                @Override
                public Optional<WindowNativeBinding> refreshAndCommit(WindowRuntimeContext ignored) {
                    return Optional.of(binding);
                }
            };
            BoundWindowCaptureService captureService = new BoundWindowCaptureService(null) {
                @Override
                public Optional<BoundWindowCaptureService.CaptureResult> captureRegion(
                        WindowNativeBinding ignoredBinding, int baseX, int baseY,
                        int x1, int y1, int x2, int y2) {
                    boolean hudRegion = (x2 - x1) == 272 && (y2 - y1) == 69;
                    BufferedImage frame = hudRegion
                            ? (hudMarkerPresent ? hudPresent : hudAbsent)
                            : panelFrame;
                    return frame == null
                            ? Optional.empty()
                            : Optional.of(new BoundWindowCaptureService.CaptureResult(frame, null));
                }
            };
            InputSequences inputSequences = new InputSequences(null) {
                @Override
                public boolean submitAndWait(String description, List<InputAction> actions) {
                    submitted.add(description);
                    return true;
                }
            };
            Path tempRoot = Files.createTempDirectory("team-return-probe-test");
            WindowScopedTempPath tempPath = new WindowScopedTempPath(contextHolder, null) {
                @Override
                public String resolve(String fileName) {
                    return tempRoot.resolve(fileName).toString();
                }
            };
            this.operation = new TeamReturnPanelLocalOperation(
                    contextHolder, refreshService, captureService, inputSequences, tempPath);
        }
    }
}
