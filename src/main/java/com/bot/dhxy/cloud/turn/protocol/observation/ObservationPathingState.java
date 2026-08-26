package com.bot.dhxy.cloud.turn.protocol.observation;

/** Mechanical state of the exact local pathing snapshot. */
public enum ObservationPathingState {
    NONE,
    ACTIVE,
    ARRIVED,
    STOPPED_AWAY,
    UNKNOWN,
    /**
     * 2026-08-23 停稳事实重设计（五环首批）：本地字模读值判定"坐标数值已停稳"。
     * 携带读出的 current 坐标与到达帧 lineage；到达/半路的业务判定移交云端。
     */
    STABLE,
    /** 数字框不可读（遮挡/黑帧/定位失败）：第三态，既不算动也不算停。 */
    STRIP_UNAVAILABLE
}
