package com.bot.dhxy.window.observation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G108 client contracts: the exact wire value the Runner folds for each 召-watch sample. The
 * per-generation seen-marker is what keeps the Cloud's latest-only poll lossless, so its
 * accumulation rules are frozen here against the real production {@code Duty}.
 */
class TeamReturnZhaoWatchDutyContractTest {

    @Test
    void frozenIdentityMatchesTheCard() {
        assertEquals("team-return-zhao-watch", TeamReturnZhaoWatchLocalMechanics.INTEREST_KEY);
        assertEquals(1_000L, TeamReturnZhaoWatchLocalMechanics.SAMPLE_PERIOD_MS,
                "G108 froze the cadence at exactly one sample per second");
    }

    @Test
    void wireValueCarriesStateGenerationSequenceMarkerAndScore() {
        TeamReturnZhaoWatchLocalMechanics.Duty duty = new TeamReturnZhaoWatchLocalMechanics.Duty();
        assertEquals("PRESENT|gen=5|seq=1|ever=true|score=0.831",
                duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_PRESENT, 5L, 0.8305D));
    }

    /** Contract (4): one PRESENT marks every later sample of the same generation. */
    @Test
    void markerPersistsThroughAbsentAndUnknownSamples() {
        TeamReturnZhaoWatchLocalMechanics.Duty duty = new TeamReturnZhaoWatchLocalMechanics.Duty();
        duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_PRESENT, 5L, 0.9D);
        assertEquals("ABSENT|gen=5|seq=2|ever=true|score=0.100",
                duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_ABSENT, 5L, 0.1D),
                "a shout-covered ABSENT must still carry the seen-marker");
        assertEquals("UNKNOWN|gen=5|seq=3|ever=true|score=nan",
                duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_UNKNOWN, 5L, Double.NaN),
                "an unreadable frame must neither set nor clear the marker");
    }

    /** Contract (5): a new combat generation starts with a clean marker. */
    @Test
    void generationChangeClearsTheMarker() {
        TeamReturnZhaoWatchLocalMechanics.Duty duty = new TeamReturnZhaoWatchLocalMechanics.Duty();
        duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_PRESENT, 5L, 0.9D);
        assertEquals("ABSENT|gen=6|seq=2|ever=false|score=0.200",
                duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_ABSENT, 6L, 0.2D),
                "generation 5's marker must never leak into generation 6");
    }

    @Test
    void unknownAloneNeverSetsTheMarker() {
        TeamReturnZhaoWatchLocalMechanics.Duty duty = new TeamReturnZhaoWatchLocalMechanics.Duty();
        String value = duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_UNKNOWN, 3L, Double.NaN);
        assertTrue(value.contains("|ever=false|"), value);
    }

    @Test
    void sequenceIsMonotonicAndResetStartsARun() {
        TeamReturnZhaoWatchLocalMechanics.Duty duty = new TeamReturnZhaoWatchLocalMechanics.Duty();
        assertTrue(duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_ABSENT, 1L, 0.1D).contains("|seq=1|"));
        assertTrue(duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_ABSENT, 1L, 0.1D).contains("|seq=2|"));
        duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_PRESENT, 1L, 0.9D);
        duty.reset();
        String afterReset = duty.fold(TeamReturnZhaoWatchLocalMechanics.STATE_ABSENT, 1L, 0.1D);
        assertEquals("ABSENT|gen=1|seq=1|ever=false|score=0.100", afterReset,
                "a runner reset starts a fresh run: sequence and marker must not survive it");
    }
}
