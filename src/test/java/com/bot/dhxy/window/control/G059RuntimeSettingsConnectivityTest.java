package com.bot.dhxy.window.control;

import com.bot.dhxy.cloud.turn.protocol.TurnTaskRuntimeSettings;
import com.bot.dhxy.config.BotProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class G059RuntimeSettingsConnectivityTest {

    @Test
    void uiBackedPropertiesBecomeTheExactRemoteStartSnapshot() {
        BotProperties properties = new BotProperties();
        properties.setSummonSkillCleanEnabled(false);
        properties.setSummonSkillCleanIntervalMs(2_700_000L);
        properties.setXiuluoHealPetMaintenanceIntervalMs(3_600_000L);
        properties.setXiuluoRepairEquipmentMaintenanceIntervalMs(0L);
        properties.setXiuluoMaintenanceRunImmediatelyOnStart(true);
        properties.setLeaderCommonBoxEnabled(false);
        properties.setMemberCommonBoxEnabled(true);
        properties.setTaskStartupPreparationEnabled(false);
        properties.setXiuluoSkipBossEnabled(true);
        properties.setDoubleExperienceClaimEnabled(false);
        properties.setPlayerHpSupplyEnabled(true);
        properties.setPlayerHpSupplyThreshold(70);
        properties.setPlayerMpSupplyEnabled(false);
        properties.setPlayerMpSupplyThreshold(50);
        properties.setPetHpSupplyEnabled(true);
        properties.setPetHpSupplyThreshold(30);
        properties.setPetMpSupplyEnabled(false);
        properties.setPetMpSupplyThreshold(70);

        TurnTaskRuntimeSettings settings =
                WindowTaskControlService.buildRuntimeSettingsSnapshot(properties);

        assertFalse(settings.summonSkillCleanEnabled());
        assertEquals(2_700_000L, settings.summonSkillCleanIntervalMs());
        assertEquals(3_600_000L, settings.healPetMaintenanceIntervalMs());
        assertEquals(0L, settings.repairEquipmentMaintenanceIntervalMs());
        assertTrue(settings.maintenanceRunImmediatelyOnStart());
        assertFalse(settings.leaderCommonBoxEnabled());
        assertTrue(settings.memberCommonBoxEnabled());
        assertFalse(settings.taskStartupPreparationEnabled());
        assertTrue(settings.xiuluoSkipBossEnabled());
        assertFalse(settings.doubleExperienceClaimEnabled());
        assertTrue(settings.playerHpSupplyEnabled());
        assertEquals(70, settings.playerHpSupplyThreshold());
        assertFalse(settings.playerMpSupplyEnabled());
        assertEquals(50, settings.playerMpSupplyThreshold());
        assertTrue(settings.petHpSupplyEnabled());
        assertEquals(30, settings.petHpSupplyThreshold());
        assertFalse(settings.petMpSupplyEnabled());
        assertEquals(70, settings.petMpSupplyThreshold());
    }
}
