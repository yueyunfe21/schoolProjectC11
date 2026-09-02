package com.bot.dhxy.cloud.turn.local;

import com.bot.dhxy.core.MatchEvidenceStore;
import com.bot.dhxy.core.ImageFinder;
import com.bot.dhxy.driver.BoundWindowCaptureService;
import com.bot.dhxy.input.InputSequences;
import com.bot.dhxy.input.action.InputAction;
import com.bot.dhxy.window.model.WindowNativeBinding;
import com.bot.dhxy.window.runtime.WindowNativeBindingRefreshService;
import com.bot.dhxy.window.runtime.WindowRuntimeContext;
import com.bot.dhxy.window.runtime.WindowScopedTempPath;
import com.bot.dhxy.window.runtime.WindowTaskContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Owns the exact-window leader Alt+T and local-only team-return panel matcher. */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TeamReturnPanelLocalOperation {

    /*
     * 2026-08-17 现场证据（images/temp/hwnd-E11FDA、hwnd-C721C7E 判定图 + 同分钟双窗对比测量）：
     * 同尺寸无边框窗口里，不同角色客户端把 Alt+T 队伍面板画在相差约 27px 的高度上——旧 ROI
     * (y=279,h=40) 按 C721C7E 标定，换 E11FDA 当队长时"召回"落在 ROI 上沿之外（y≈255-268），
     * 探测把 miss 判成全员归队导致队长提前开下一轮。顶边上移 50px、底边不变，罩住两种面板位置。
     */
    private static final int ROI_X = 314;
    private static final int ROI_Y = 229;
    private static final int ROI_WIDTH = 561;
    private static final int ROI_HEIGHT = 90;
    private static final double MATCH_THRESHOLD = 0.85D;
    private static final Path NOT_RETURNED_TEMPLATE =
            Path.of("images", "template", "team", "not_returned_yet.png");

    /*
     * 2026-08-26 22:43:24 事故（G107，用户确认"位置没问题，是时间的问题"）：Alt+T 输入确认后仅
     * 106ms 就截图，队伍面板只画出了标题栏和职业竖签，人物卡片整片还是黑的，"召回"红标根本没
     * 绘制；单张 miss（score=0.327）被当成全员归队，面板被关掉、归队门只维持 819ms 就结束，四名
     * 队员没有任何机会执行归队输入。同一个"miss 即成功"的推断在 2026-08-17 已经害过一次（见上方
     * ROI 注释），当时只堵了 ROI 偏移这一个成因。这里取消该推断本身：miss 必须同时满足
     * ①距开面板已过稳定期 ②分时两张负帧 ③HUD 上的绿色"召"确实消失，才允许判全员归队。
     */
    private static final long PANEL_SETTLE_MIN_MS = 900L;
    private static final int MISS_CONFIRM_FRAMES = 2;
    private static final long MISS_FRAME_MIN_GAP_MS = 250L;
    /*
     * 2026-08-26 二审 P1：曾经要求"两张负帧指纹必须不同"，但画完之后完全静止的正常面板也会一直
     * 拿到同一张指纹，那样每一轮都凑不满、180 秒后停任务——这条规则不能进 fresh，已撤下判定路径。
     * 指纹仍然每帧记录进日志：同一窗口连续捕获的指纹序列正是建立"截图新鲜性合同"所需的数据，
     * 等 fresh 跑出真实序列后再据此立判据（G107 卡遗留项）。
     * 时间门一律走 System.nanoTime()：System.currentTimeMillis() 会被系统校时前后拨动门槛。
     */
    private static final long PANEL_SETTLE_MIN_NANOS = PANEL_SETTLE_MIN_MS * 1_000_000L;
    private static final long MISS_FRAME_MIN_GAP_NANOS = MISS_FRAME_MIN_GAP_MS * 1_000_000L;

    /*
     * 与云端 CloudTeamReturnPortAssembly 的 leader-signal 判据同源（同模板/同 ROI/同阈值 0.75，
     * 该阈值有 2026-08-20 全天 4848 次实测背书）。它读的是 HUD 队员头像上的绿色"召"，跟队伍面板
     * 渲染时序完全解耦——只要还有人挂着"召"，"全员归队"就是可证伪的。
     */
    private static final Path LEADER_SIGNAL_TEMPLATE =
            Path.of("images", "template", "status", "zhao.png");
    private static final double LEADER_SIGNAL_THRESHOLD = 0.75D;
    private static final int SIGNAL_ROI_X = 342;
    private static final int SIGNAL_ROI_Y = 57;
    private static final int SIGNAL_ROI_WIDTH = 272;
    private static final int SIGNAL_ROI_HEIGHT = 69;

    private final WindowTaskContextHolder contextHolder;
    private final WindowNativeBindingRefreshService bindingRefreshService;
    private final BoundWindowCaptureService captureService;
    private final InputSequences inputSequences;
    private final WindowScopedTempPath windowScopedTempPath;
    private final Map<WindowBindingKey, PanelOwner> locallyOpenedPanels = new ConcurrentHashMap<>();
    private final Map<WindowBindingKey, ProbeState> probeStates = new ConcurrentHashMap<>();

    /** 2026-08-23 用户契约（停止=彻底清空）：清该窗口停止时可能半开着的归队面板认领。 */
    public void forgetWindowRealityMemory(String windowId) {
        if (windowId != null && !windowId.isBlank()) {
            locallyOpenedPanels.keySet().removeIf(key -> windowId.equals(key.windowId()));
            probeStates.keySet().removeIf(key -> windowId.equals(key.windowId()));
        }
    }

    /**
     * Opens the bound leader's team panel without capturing a frame.
     *
     * @return {@code OPENED} only after the serialized Alt+T input completed
     */
    public synchronized OpenResult openPanel(String businessTaskRunId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null
                ? null : bindingRefreshService.refreshAndCommit(context).orElse(null);
        PanelOwner requested = owner(context, binding, businessTaskRunId);
        if (requested == null) {
            return OpenResult.UNKNOWN;
        }
        PanelOwner existing = locallyOpenedPanels.get(requested.binding());
        if (requested.equals(existing)) {
            log.info("[team-return-panel] open already acknowledged: owner={}", requested);
            return OpenResult.OPENED;
        }
        if (existing != null) {
            if (!closePhysical(existing, "run-replaced")) {
                return OpenResult.UNKNOWN;
            }
            locallyOpenedPanels.remove(requested.binding(), existing);
            probeStates.remove(requested.binding());
        }
        boolean opened = inputSequences.submitAndWait(
                "team-return:leader-panel-open", List.of(InputAction.pressAltT()));
        if (opened) {
            locallyOpenedPanels.put(requested.binding(), requested);
            // 稳定期从 Alt+T 输入确认时刻起算：这一刻游戏才开始画面板。
            probeStates.put(requested.binding(), new ProbeState(System.nanoTime()));
        }
        log.info("[team-return-panel] open input: owner={} result={}", requested, opened);
        return opened ? OpenResult.OPENED : OpenResult.UNKNOWN;
    }

    /**
     * Probes the already-open exact-window panel and closes it only after a confirmed marker miss.
     *
     * @return fail-closed panel result; no image bytes cross the Client/Cloud boundary
     */
    public synchronized ProbeResult probeAndCloseIfComplete(String businessTaskRunId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null
                ? null : bindingRefreshService.refreshAndCommit(context).orElse(null);
        PanelOwner requested = owner(context, binding, businessTaskRunId);
        if (requested == null || !requested.equals(locallyOpenedPanels.get(requested.binding()))
                || !binding.hasGeometry()) {
            return ProbeResult.UNKNOWN;
        }
        BufferedImage roi = captureService.captureRegion(
                        binding, binding.getX(), binding.getY(),
                        binding.getX() + ROI_X, binding.getY() + ROI_Y,
                        binding.getX() + ROI_X + ROI_WIDTH, binding.getY() + ROI_Y + ROI_HEIGHT)
                .map(BoundWindowCaptureService.CaptureResult::image)
                .orElse(null);
        ProbeState state = probeStates.get(requested.binding());
        if (roi == null) {
            // 截图异常不是证据：清空负帧计数，绝不让它凑数。
            if (state != null) {
                state.resetMissStreak();
            }
            return ProbeResult.UNKNOWN;
        }
        BufferedImage template = null;
        try {
            template = ImageIO.read(NOT_RETURNED_TEMPLATE.toFile());
            if (template == null || template.getWidth() <= 0 || template.getHeight() <= 0
                    || template.getWidth() > roi.getWidth() || template.getHeight() > roi.getHeight()) {
                if (state != null) {
                    state.resetMissStreak();
                }
                return ProbeResult.UNKNOWN;
            }
            double[] strongest = ImageFinder.find(roi, template, -1.0D);
            if (strongest == null || strongest.length < 3 || !Double.isFinite(strongest[2])) {
                if (state != null) {
                    state.resetMissStreak();
                }
                return ProbeResult.UNKNOWN;
            }
            boolean notReturnedYet = strongest[2] >= MATCH_THRESHOLD;
            saveEvidence(roi, template, strongest, notReturnedYet);
            if (notReturnedYet) {
                if (state != null) {
                    state.resetMissStreak();
                }
                log.info("[team-return-panel] probe: windowId={} result=NOT_RETURNED_YET score={} threshold={} "
                                + "roi=({},{} {}x{})",
                        context.getWindowId(), strongest[2], MATCH_THRESHOLD,
                        ROI_X, ROI_Y, ROI_WIDTH, ROI_HEIGHT);
                return ProbeResult.NOT_RETURNED_YET;
            }
            if (state == null) {
                log.info("[team-return-panel] probe miss without a known open time; stay open: windowId={} score={}",
                        context.getWindowId(), strongest[2]);
                return ProbeResult.UNKNOWN;
            }
            long nowNanos = System.nanoTime();
            long sinceOpenMs = (nowNanos - state.openedAtNanos()) / 1_000_000L;
            if (nowNanos - state.openedAtNanos() < PANEL_SETTLE_MIN_NANOS) {
                // G107 根因帧就落在这里：面板还没画完，缺"召回"不代表人已归队。
                log.info("[team-return-panel] probe miss ignored before settle: windowId={} score={} "
                                + "sinceOpenMs={} settleMs={}",
                        context.getWindowId(), strongest[2], sinceOpenMs, PANEL_SETTLE_MIN_MS);
                return ProbeResult.UNKNOWN;
            }
            String fingerprint = fingerprint(roi);
            boolean counted = state.acceptMissFrame(nowNanos, MISS_FRAME_MIN_GAP_NANOS, fingerprint);
            if (!counted) {
                log.info("[team-return-panel] probe miss too close to the previous frame; keep waiting: "
                                + "windowId={} score={} missFrames={} fingerprint={} repeatsPrevious={}",
                        context.getWindowId(), strongest[2], state.missFrames(),
                        fingerprint, fingerprint.equals(state.lastMissFingerprint()));
                return ProbeResult.UNKNOWN;
            }
            if (state.missFrames() < MISS_CONFIRM_FRAMES) {
                log.info("[team-return-panel] probe miss pending confirmation: windowId={} score={} "
                                + "missFrames={}/{} fingerprint={} sinceOpenMs={}",
                        context.getWindowId(), strongest[2], state.missFrames(),
                        MISS_CONFIRM_FRAMES, fingerprint, sinceOpenMs);
                return ProbeResult.UNKNOWN;
            }
            LeaderSignalCheck signal = readLeaderSignal(binding);
            if (signal == LeaderSignalCheck.PRESENT) {
                // HUD 还挂着"召"：全员归队被直接证伪，面板留着继续等。
                state.resetMissStreak();
                log.warn("[team-return-panel] all-returned refuted by HUD leader signal; keep panel open: "
                                + "windowId={} panelScore={} sinceOpenMs={}",
                        context.getWindowId(), strongest[2], sinceOpenMs);
                return ProbeResult.NOT_RETURNED_YET;
            }
            if (signal == LeaderSignalCheck.UNREADABLE) {
                state.resetMissStreak();
                log.warn("[team-return-panel] leader signal unreadable; refuse all-returned: windowId={}",
                        context.getWindowId());
                return ProbeResult.UNKNOWN;
            }
            log.info("[team-return-panel] all-returned confirmed: windowId={} panelScore={} missFrames={} "
                            + "sinceOpenMs={} hudSignal=ABSENT",
                    context.getWindowId(), strongest[2], state.missFrames(), sinceOpenMs);
            boolean closed = closePhysical(requested, "all-returned");
            if (closed) {
                locallyOpenedPanels.remove(requested.binding(), requested);
                probeStates.remove(requested.binding());
            }
            return closed ? ProbeResult.ALL_RETURNED : ProbeResult.UNKNOWN;
        } catch (IOException | RuntimeException failure) {
            if (state != null) {
                state.resetMissStreak();
            }
            log.warn("[team-return-panel] probe failed: windowId={} reason={}",
                    context.getWindowId(), failure.getMessage(), failure);
            return ProbeResult.UNKNOWN;
        } finally {
            if (template != null) {
                template.flush();
            }
            roi.flush();
        }
    }

    /**
     * Closes only the panel physically owned by the exact window/HWND/task run.
     *
     * @param businessTaskRunId exact Cloud business task-run identity; never null or blank
     * @return idempotent close result; another run's panel is never toggled
     */
    public synchronized CloseResult closePanel(String businessTaskRunId) {
        WindowRuntimeContext context = contextHolder.rawCurrent().orElse(null);
        WindowNativeBinding binding = context == null
                ? null : bindingRefreshService.refreshAndCommit(context).orElse(null);
        PanelOwner requested = owner(context, binding, businessTaskRunId);
        if (requested == null) {
            return CloseResult.UNKNOWN;
        }
        PanelOwner existing = locallyOpenedPanels.get(requested.binding());
        if (existing == null) {
            return CloseResult.CLOSED;
        }
        if (!requested.equals(existing)) {
            log.info("[team-return-panel] close ignored for non-owner: requested={} actual={}",
                    requested, existing);
            return CloseResult.NOT_OWNED;
        }
        if (!closePhysical(requested, "lifecycle-release")) {
            return CloseResult.UNKNOWN;
        }
        locallyOpenedPanels.remove(requested.binding(), requested);
        probeStates.remove(requested.binding());
        return CloseResult.CLOSED;
    }

    private boolean closePhysical(PanelOwner owner, String reason) {
        boolean closed = inputSequences.submitAndWait(
                "team-return:leader-panel-close:" + reason, List.of(InputAction.pressAltT()));
        log.info("[team-return-panel] close input: owner={} reason={} result={}", owner, reason, closed);
        return closed;
    }

    private static PanelOwner owner(WindowRuntimeContext context,
                                    WindowNativeBinding binding,
                                    String businessTaskRunId) {
        if (context == null || binding == null || !binding.hasNativeHandle()
                || businessTaskRunId == null || businessTaskRunId.isBlank()) {
            return null;
        }
        return new PanelOwner(
                new WindowBindingKey(context.getWindowId(), binding.getNativeHandle()),
                businessTaskRunId.trim());
    }

    private void saveEvidence(BufferedImage roi, BufferedImage template, double[] strongest, boolean matched) {
        Path raw = Path.of(windowScopedTempPath.resolve("team_return_roi_latest.png"));
        Path marked = Path.of(windowScopedTempPath.resolve("team_return_roi_latest_marked.png"));
        try {
            Files.createDirectories(raw.getParent());
            ImageIO.write(roi, "png", raw.toFile());
            BufferedImage evidence = new BufferedImage(roi.getWidth(), roi.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = evidence.createGraphics();
            try {
                graphics.drawImage(roi, 0, 0, null);
                graphics.setColor(matched ? Color.RED : Color.ORANGE);
                int left = (int) Math.round(strongest[0] - template.getWidth() / 2.0D);
                int top = (int) Math.round(strongest[1] - template.getHeight() / 2.0D);
                graphics.drawRect(left, top, template.getWidth(), template.getHeight());
                graphics.fillOval((int) Math.round(strongest[0]) - 2,
                        (int) Math.round(strongest[1]) - 2, 5, 5);
            } finally {
                graphics.dispose();
            }
            try {
                ImageIO.write(evidence, "png", marked.toFile());
            } finally {
                evidence.flush();
            }
        } catch (IOException evidenceFailure) {
            log.warn("[team-return-panel] evidence save failed: path={} reason={}",
                    raw, evidenceFailure.getMessage());
        }
    }

    /**
     * Reads the HUD teammate recall marker with the exact same template/ROI/threshold the Cloud
     * leader-signal observation uses, so the two never disagree about what "needs recall" means.
     *
     * @return PRESENT when any teammate still carries the marker, ABSENT when none does, and
     *         UNREADABLE when capture or matching could not produce a usable verdict.
     */
    private LeaderSignalCheck readLeaderSignal(WindowNativeBinding binding) {
        BufferedImage roi = captureService.captureRegion(
                        binding, binding.getX(), binding.getY(),
                        binding.getX() + SIGNAL_ROI_X, binding.getY() + SIGNAL_ROI_Y,
                        binding.getX() + SIGNAL_ROI_X + SIGNAL_ROI_WIDTH,
                        binding.getY() + SIGNAL_ROI_Y + SIGNAL_ROI_HEIGHT)
                .map(BoundWindowCaptureService.CaptureResult::image)
                .orElse(null);
        if (roi == null) {
            return LeaderSignalCheck.UNREADABLE;
        }
        BufferedImage template = null;
        try {
            template = ImageIO.read(LEADER_SIGNAL_TEMPLATE.toFile());
            if (template == null || template.getWidth() <= 0 || template.getHeight() <= 0
                    || template.getWidth() > roi.getWidth() || template.getHeight() > roi.getHeight()) {
                return LeaderSignalCheck.UNREADABLE;
            }
            double[] strongest = ImageFinder.find(roi, template, -1.0D);
            if (strongest == null || strongest.length < 3 || !Double.isFinite(strongest[2])) {
                return LeaderSignalCheck.UNREADABLE;
            }
            boolean leaderSignalPresent = strongest[2] >= LEADER_SIGNAL_THRESHOLD;
            MatchEvidenceStore.saveOnChange("team-return-leader-signal", null, roi, template,
                    leaderSignalPresent ? strongest : null);
            return leaderSignalPresent
                    ? LeaderSignalCheck.PRESENT : LeaderSignalCheck.ABSENT;
        } catch (IOException | RuntimeException failure) {
            log.warn("[team-return-panel] leader signal read failed: reason={}", failure.getMessage());
            return LeaderSignalCheck.UNREADABLE;
        } finally {
            if (template != null) {
                template.flush();
            }
            roi.flush();
        }
    }

    private enum LeaderSignalCheck { PRESENT, ABSENT, UNREADABLE }

    /**
     * Content fingerprint of one captured ROI. Two negative frames only count as two independent
     * observations when their fingerprints differ — an unchanged frame is the same picture sampled
     * twice, which is exactly how a frozen half-rendered panel faked a confirmation in G107.
     */
    private static String fingerprint(BufferedImage roi) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            for (int y = 0; y < roi.getHeight(); y++) {
                for (int x = 0; x < roi.getWidth(); x++) {
                    buffer.clear();
                    buffer.putInt(roi.getRGB(x, y));
                    digest.update(buffer.array());
                }
            }
            byte[] hash = digest.digest();
            StringBuilder text = new StringBuilder(16);
            for (int index = 0; index < 8; index++) {
                text.append(String.format("%02x", hash[index]));
            }
            return text.toString();
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }

    /** Per-panel probe bookkeeping: open instant plus the distinct negative frames seen since. */
    private static final class ProbeState {
        private final long openedAtNanos;
        private int missFrames;
        private long lastMissAtNanos;
        private String lastMissFingerprint;

        private ProbeState(long openedAtNanos) {
            this.openedAtNanos = openedAtNanos;
        }

        private long openedAtNanos() {
            return openedAtNanos;
        }

        private int missFrames() {
            return missFrames;
        }

        private String lastMissFingerprint() {
            return lastMissFingerprint;
        }

        /**
         * Counts one miss only when it is spaced from the previous one, so a rapid re-sample of the
         * same moment cannot manufacture a confirmation. The content fingerprint is recorded for the
         * capture-freshness contract still to be built from real same-window sequences; it does not
         * gate the decision, because a finished panel may legitimately stay pixel-identical.
         *
         * @param nowNanos monotonic reading for this capture
         * @param minGapNanos minimum spacing between two counted frames
         * @param fingerprint content fingerprint of this capture; never null
         * @return true when this frame was counted as negative evidence
         */
        private boolean acceptMissFrame(long nowNanos, long minGapNanos, String fingerprint) {
            if (missFrames > 0 && nowNanos - lastMissAtNanos < minGapNanos) {
                return false;
            }
            missFrames++;
            lastMissAtNanos = nowNanos;
            lastMissFingerprint = fingerprint;
            return true;
        }

        private void resetMissStreak() {
            missFrames = 0;
            lastMissAtNanos = 0L;
            lastMissFingerprint = null;
        }
    }

    public enum OpenResult { OPENED, UNKNOWN }

    public enum ProbeResult { NOT_RETURNED_YET, ALL_RETURNED, UNKNOWN }

    public enum CloseResult { CLOSED, NOT_OWNED, UNKNOWN }

    private record WindowBindingKey(String windowId, String hwnd) {}

    private record PanelOwner(WindowBindingKey binding, String businessTaskRunId) {}
}
