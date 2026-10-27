package com.bot.dhxy.model.flight;

import java.util.Objects;

/**
 * 单次飞行调整的纯决策状态；不截图、不发送输入，也不把初筛推断当作确认。
 * 每个窗口业务尝试独占一个实例，跨帧保留；唯一调用方在提交现有串行输入前领取动作，
 * 报告真实执行结果，并继续喂入共享采样帧。不得通过每拍重建实例重置按键预算。
 */
public final class FlightStateAdjustment {
    public static final double INITIAL_CENTER = 0.20;
    public static final double INITIAL_HALF_WIDTH = 0.03;
    // 使用十进制字面量，避免 center +/- halfWidth 的浮点误差改变边界归属。
    public static final double INITIAL_LOW = 0.17;
    public static final double INITIAL_HIGH = 0.23;
    public static final double DEFAULT_CHANGE_THRESHOLD = 0.02;

    public enum Target { FLYING, GROUNDED }
    public enum InitialState { LIKELY_FLYING, LIKELY_GROUNDED, AMBIGUOUS, UNAVAILABLE }
    public enum Status {
        WAITING_INITIAL_FRAME, TOGGLE_REQUESTED, WAITING_INPUT, WAITING_CONFIRMATION,
        ASSUMED_TARGET, CONFIRMED_TARGET, UNCONFIRMED, CANCELLED
    }
    public enum InputOutcome { EXECUTED, NOT_EXECUTED, UNKNOWN }

    private final String scope;
    private final Target target;
    private final double changeThreshold;
    private final long settleMs;
    private final long timeoutMs;
    private final long startedAtMs;
    private long stageStartedAtMs;
    private long lastNowMs;
    private long lastSequence = -1;
    private long lastCaptureMs = -1;
    private long claimedAtMs;
    private long inputFinishedAtMs;
    private double before = Double.NaN;
    private double lastDelta = Double.NaN;
    private int toggleOrdinal;
    private Status status = Status.WAITING_INITIAL_FRAME;

    /**
     * @param scope 非空且唯一的窗口ID/HWND/run/attempt/ROI及缩放坐标系标识；不能跨窗口共享
     * @param target 本次希望达到的飞行状态，非null
     * @param startedAtMs 本次开始时间，毫秒，非负；后续帧/回执/now须使用同一时钟
     * @param changeThreshold 按前后平均饱和度的最小显著差，(0,1]；与初筛半宽独立
     * @param settleMs 实际按键完成后排除动画帧的等待毫秒，非负
     * @param timeoutMs 每阶段等待上限毫秒，必须大于settleMs；不会因重复帧而刷新
     */
    public FlightStateAdjustment(String scope, Target target, long startedAtMs,
                                 double changeThreshold, long settleMs, long timeoutMs) {
        if (scope == null || scope.isBlank() || startedAtMs < 0
                || !Double.isFinite(changeThreshold) || changeThreshold <= 0 || changeThreshold > 1
                || settleMs < 0 || timeoutMs <= settleMs) {
            throw new IllegalArgumentException("Invalid flight adjustment scope, timing or threshold");
        }
        this.scope = scope;
        this.target = Objects.requireNonNull(target, "target");
        this.startedAtMs = startedAtMs;
        this.changeThreshold = changeThreshold;
        this.settleMs = settleMs;
        this.timeoutMs = timeoutMs;
        this.stageStartedAtMs = startedAtMs;
        this.lastNowMs = startedAtMs;
    }

    /**
     * @param saturation 同一飞行ROI平均饱和度，[0,1]；NaN/无限/越界表示不可用
     * @return 仅为初筛推断；两个边界均属于模糊区，不是确认结果
     */
    public static InitialState classifyInitial(double saturation) {
        if (!Double.isFinite(saturation) || saturation < 0 || saturation > 1) {
            return InitialState.UNAVAILABLE;
        }
        if (saturation < INITIAL_LOW) {
            return InitialState.LIKELY_FLYING;
        }
        if (saturation > INITIAL_HIGH) {
            return InitialState.LIKELY_GROUNDED;
        }
        return InitialState.AMBIGUOUS;
    }

    /**
     * 消费既有共享ROI采样，返回当前决策。无效/重复/旧帧不改变基准、不补发按键。
     * @param frameScope 与构造scope完全一致的帧所有者，null或不符时忽略
     * @param sequence 本scope内递增的观察序号，非负；不能使用上传或处理次数
     * @param capturedAtMs 实际采样毫秒；不是上传/消费时间，必须与nowMs同源
     * @param saturation 同一ROI平均饱和度，[0,1]；不可用传NaN
     * @param nowMs 当前毫秒，与构造时间同源
     * @param permitted 当前业务是否仍允许调整；false永久取消本实例
     * @return 当前状态；TOGGLE_REQUESTED仍须唯一领取，不能直接重复执行
     */
    public synchronized Status observe(String frameScope, long sequence, long capturedAtMs,
                                       double saturation, long nowMs, boolean permitted) {
        advanceDeadline(nowMs, permitted);
        if (isTerminal() || !scope.equals(frameScope) || sequence <= lastSequence
                || capturedAtMs <= lastCaptureMs || capturedAtMs < startedAtMs || capturedAtMs > nowMs
                || classifyInitial(saturation) == InitialState.UNAVAILABLE) {
            return status;
        }
        lastSequence = sequence;
        lastCaptureMs = capturedAtMs;
        if (status == Status.WAITING_INITIAL_FRAME) {
            InitialState initial = classifyInitial(saturation);
            if ((target == Target.FLYING && initial == InitialState.LIKELY_FLYING)
                    || (target == Target.GROUNDED && initial == InitialState.LIKELY_GROUNDED)) {
                status = Status.ASSUMED_TARGET;
            } else {
                before = saturation;
                toggleOrdinal = 1;
                status = Status.TOGGLE_REQUESTED;
            }
        } else if (status == Status.TOGGLE_REQUESTED) {
            // 排队期间可刷新按前基准；领取以后冻结，避免把按后的像素混入基准。
            before = saturation;
        } else if (status == Status.WAITING_CONFIRMATION
                && capturedAtMs > inputFinishedAtMs
                && capturedAtMs - inputFinishedAtMs >= settleMs) {
            lastDelta = saturation - before;
            if (Math.abs(lastDelta) < changeThreshold) {
                return status; // 等新帧到超时，不因差分不足重按，也不移动按前基准。
            }
            Target observed = lastDelta < 0 ? Target.FLYING : Target.GROUNDED;
            if (observed == target) {
                status = Status.CONFIRMED_TARGET;
            } else if (toggleOrdinal == 1) {
                before = saturation;
                toggleOrdinal = 2;
                stageStartedAtMs = lastNowMs;
                status = Status.TOGGLE_REQUESTED;
            } else {
                status = Status.UNCONFIRMED;
            }
        }
        return status;
    }

    /**
     * 唯一发布方提交串行输入前调用；true才允许本次Alt+C。实际worker仍须重新验证
     * 窗口/run/停止/进战，不能将Cloud领取当作本地输入许可。
     * 领取后即使回执缺失也不再次领取，避免一次toggle被重复执行。
     * @param actionScope 输入动作的精确scope，null/不符拒绝
     * @param ordinal 之前读取的toggleOrdinal（1或2），旧号拒绝
     * @param nowMs 当前毫秒，与采样同源
     * @param permitted 实际执行时任务/窗口是否仍允许输入
     * @return 是否唯一成功领取本次按键；false不发送输入
     */
    public synchronized boolean claimToggle(String actionScope, int ordinal,
                                            long nowMs, boolean permitted) {
        advanceDeadline(nowMs, permitted);
        if (status != Status.TOGGLE_REQUESTED || !scope.equals(actionScope)
                || ordinal != toggleOrdinal) {
            return false;
        }
        claimedAtMs = lastNowMs;
        stageStartedAtMs = lastNowMs;
        status = Status.WAITING_INPUT;
        return true;
    }

    /**
     * 记录真实输入完成回执；EXECUTED只表示按键已送达，绝不表示已飞行/落地。
     * @param actionScope 原动作scope，null/不符忽略
     * @param ordinal 原动作序号，旧/重复回执忽略
     * @param outcome 真实执行结果，非null；不确定不能冒充EXECUTED
     * @param completedAtMs 按键实际完成毫秒；远端缺少执行时间时可用ACK到达时间作保守上界，
     *                      但后续必须重新登记采样阶段并只收阶段到达Client后新抓的帧
     * @param nowMs 当前毫秒，与采样/完成时间同源
     * @param permitted 本次业务是否仍有效，false终止
     * @return 当前状态；拒绝/未知输入结果以UNCONFIRMED结束，不盲目重发
     */
    public synchronized Status completeToggle(String actionScope, int ordinal, InputOutcome outcome,
                                              long completedAtMs, long nowMs, boolean permitted) {
        advanceDeadline(nowMs, permitted);
        if (status != Status.WAITING_INPUT || !scope.equals(actionScope) || ordinal != toggleOrdinal) {
            return status;
        }
        if (outcome != InputOutcome.EXECUTED || completedAtMs < claimedAtMs || completedAtMs > nowMs) {
            status = Status.UNCONFIRMED;
        } else {
            inputFinishedAtMs = completedAtMs;
            stageStartedAtMs = completedAtMs;
            status = Status.WAITING_CONFIRMATION;
            advanceDeadline(nowMs, permitted);
        }
        return status;
    }

    /**
     * 无采样/回执时也必须由现有调度调用，保证等待最终退出。
     * @param nowMs 当前毫秒，与构造时间同源
     * @param permitted false表示停止、换轮或前置条件失效；终态不能复活
     * @return 当前状态；超时UNCONFIRMED不等于业务任务成功
     */
    public synchronized Status poll(long nowMs, boolean permitted) {
        advanceDeadline(nowMs, permitted);
        return status;
    }

    public synchronized Status getStatus() { return status; }
    public synchronized int getToggleOrdinal() { return toggleOrdinal; }
    public synchronized double getLastDelta() { return lastDelta; }

    public synchronized boolean isTerminal() {
        return status == Status.ASSUMED_TARGET || status == Status.CONFIRMED_TARGET
                || status == Status.UNCONFIRMED || status == Status.CANCELLED;
    }

    private void advanceDeadline(long nowMs, boolean permitted) {
        lastNowMs = Math.max(lastNowMs, nowMs);
        if (!isTerminal()) {
            if (!permitted) {
                status = Status.CANCELLED;
            } else if (lastNowMs - stageStartedAtMs >= timeoutMs) {
                status = Status.UNCONFIRMED;
            }
        }
    }
}
