package com.bot.dhxy.window.control;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G156（2026-09-05 用户裁定"以前不是有可能停下了的状态么，现在的问题是判停不对"）：
 * 恢复"半路停下"的两档判定与中间观察态。
 *
 * <p>旧本地判定 {@code classifyRecognizedPathingState} 是两档：到达 600ms 即认，
 * 半路停下必须静止 ≥2200ms（三条件齐备），两者之间落 ACTIVE＝"可能停下了但还不算数"。
 * 08-22 值判稳重设计只写了单一 900ms 门，这一档没被带过来；G130 推广到全部任务后，
 * 无目标绿链腿的每次 900ms 静止都直接成终局。2026-09-05 06:14~06:21 实测队长窗 6 次判停
 * 被有效变值撤回，5 次真实静止 &lt; 2200ms。</p>
 */
class G156StoppedAwayObservationGateContractTest {

    private static final Path HANDLER = Path.of(
            "../dhxy-cloud-brain/src/main/java/com/yueyunfe/dhxy/cloudbrain/observation/CloudObservationHttpHandler.java");
    private static final Path SAMPLER = Path.of(
            "src/main/java/com/bot/dhxy/window/observation/WindowObservationSampler.java");

    @Test
    void stoppedAwayRequiresTheOldTwoPointTwoSecondStillness() throws Exception {
        String source = Files.readString(HANDLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("STOPPED_AWAY_STATIONARY_MS = 2_200L"),
                "半路停下的静止门必须是旧本地判定的 2.2s 原值");
        int gate = source.indexOf("if (mapped == ObservationPathingState.STOPPED_AWAY) {");
        assertTrue(gate > 0, "找不到半路停下的静止时长门；改了结构就更新本合同");
        String body = source.substring(gate, Math.min(source.length(), gate + 1200));
        assertTrue(body.contains("Math.max(fact.locationChangedAtMs(), fact.pathingStartedAtMs())"),
                "静止起点必须取 max(最后一次坐标变化, 本腿起点)——同时覆盖旧门的 stableMs 与 intentAgeMs");
        assertTrue(body.contains("stationaryMs < STOPPED_AWAY_STATIONARY_MS"),
                "静止不足必须降回观察态");
        assertTrue(body.contains("mapped = ObservationPathingState.ACTIVE"),
                "观察态就是旧判定未达门槛时落的 ACTIVE");
    }

    @Test
    void arrivedStaysOnTheFastPathAndKeepsLineage() throws Exception {
        String source = Files.readString(HANDLER, StandardCharsets.UTF_8);
        int gate = source.indexOf("if (mapped == ObservationPathingState.STOPPED_AWAY) {");
        String body = source.substring(gate, Math.min(source.length(), gate + 1200));
        assertFalse(body.contains("ObservationPathingState.ARRIVED"),
                "到达路不许被静止门拖慢——它的证据是坐标与目标重合，不是静止时长");
        assertTrue(source.contains("mapped == ObservationPathingState.ARRIVED,\n                resolvedMapName)")
                        || source.contains("mapped == ObservationPathingState.ARRIVED"),
                "终局帧 lineage 仍只给 ARRIVED");
    }

    @Test
    void observingActiveCarriesNoCoordinatesSoTheBatchIsNotRejected() throws Exception {
        String source = Files.readString(HANDLER, StandardCharsets.UTF_8);
        int method = source.indexOf("private static ObservationPathingFact withObservingActiveState(");
        assertTrue(method > 0, "找不到观察态构造；改了结构就更新本合同");
        String body = source.substring(method, Math.min(source.length(), method + 900));
        assertTrue(body.contains("ObservationPathingState.ACTIVE"), "观察态必须是 ACTIVE");
        assertTrue(body.contains("null, null, null, fact.locationChangedAtMs()"),
                "观察态必须不带图名与坐标——校验器的带坐标豁免只认 STABLE/STOPPED_AWAY"
                        + "（2026-08-23 22:4x 整批 400 拒收实锤）");
        assertTrue(body.contains("null, null);"),
                "观察态不许携带终局帧 lineage");
    }

    @Test
    void theOldTwoTierThresholdsRemainTheDocumentedSource() throws Exception {
        String sampler = Files.readString(SAMPLER, StandardCharsets.UTF_8);
        assertTrue(sampler.contains("LOCAL_PATHING_STOPPED_AWAY_MS = 2_200L"),
                "2.2s 出处（旧本地判定常量）不许悄悄改动；改了就要同步云端门与本合同");
        assertTrue(sampler.contains("LOCAL_PATHING_ARRIVAL_STATIONARY_MS = 600L"),
                "到达档常量同为出处");
    }
}
