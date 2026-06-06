package com.bot.dhxy.ui;

import com.bot.dhxy.config.BotProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Persists user-editable JavaFX game settings across app restarts.
 *
 * <p>The Spring {@code application.properties} file remains the project default. Runtime UI edits
 * are stored separately under {@code config/ui-game-settings.properties}, so local preferences do
 * not churn the main config file while tasks can still read the live {@link BotProperties} object.
 */
@Slf4j
@Component
public class GameUiSettingsStore {

    private static final Path SETTINGS_PATH = Path.of("config", "ui-game-settings.properties");

    private static final String XIULUO_MAX_RUNS = "xiuluoMaxRuns";
    private static final String WUHUAN_MAX_RUNS = "wuhuanMaxRuns";
    private static final String FIVEFOLD_MAX_RUNS = "fivefoldMaxRuns";
    private static final String TIANTING_MAX_RUNS = "tiantingMaxRuns";
    private static final String ZHUAGUI_MAX_RUNS = "zhuaguiMaxRuns";
    private static final String SUMMON_SKILL_CLEAN_ENABLED = "summonSkillCleanEnabled";
    private static final String SUMMON_SKILL_CLEAN_INTERVAL_MS = "summonSkillCleanIntervalMs";
    private static final String XIULUO_HEAL_PET_MAINTENANCE_INTERVAL_MS = "xiuluoHealPetMaintenanceIntervalMs";
    private static final String XIULUO_REPAIR_EQUIPMENT_MAINTENANCE_INTERVAL_MS = "xiuluoRepairEquipmentMaintenanceIntervalMs";
    private static final String XIULUO_MAINTENANCE_RUN_IMMEDIATELY_ON_START = "xiuluoMaintenanceRunImmediatelyOnStart";
    private static final String TASK_STARTUP_PREPARATION_ENABLED = "taskStartupPreparationEnabled";
    private static final String PLAYER_HP_SUPPLY_ENABLED = "playerHpSupplyEnabled";
    private static final String PLAYER_HP_SUPPLY_THRESHOLD = "playerHpSupplyThreshold";
    private static final String PLAYER_MP_SUPPLY_ENABLED = "playerMpSupplyEnabled";
    private static final String PLAYER_MP_SUPPLY_THRESHOLD = "playerMpSupplyThreshold";
    private static final String PET_HP_SUPPLY_ENABLED = "petHpSupplyEnabled";
    private static final String PET_HP_SUPPLY_THRESHOLD = "petHpSupplyThreshold";
    private static final String PET_MP_SUPPLY_ENABLED = "petMpSupplyEnabled";
    private static final String PET_MP_SUPPLY_THRESHOLD = "petMpSupplyThreshold";

    /**
     * Load saved UI settings into the shared runtime properties object.
     *
     * @param botProperties mutable runtime configuration read by UI controls and tasks.
     */
    public void loadInto(BotProperties botProperties) {
        if (botProperties == null || !Files.isRegularFile(SETTINGS_PATH)) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(SETTINGS_PATH)) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.warn("UI game settings load failed: path={} reason={}", SETTINGS_PATH, e.getMessage(), e);
            return;
        }

        botProperties.setXiuluoMaxRuns(readInt(properties, XIULUO_MAX_RUNS, botProperties.getXiuluoMaxRuns()));
        botProperties.setWuhuanMaxRuns(readInt(properties, WUHUAN_MAX_RUNS, botProperties.getWuhuanMaxRuns()));
        botProperties.setFivefoldMaxRuns(readInt(properties, FIVEFOLD_MAX_RUNS, botProperties.getFivefoldMaxRuns()));
        botProperties.setTiantingMaxRuns(readInt(properties, TIANTING_MAX_RUNS, botProperties.getTiantingMaxRuns()));
        botProperties.setZhuaguiMaxRuns(readInt(properties, ZHUAGUI_MAX_RUNS, botProperties.getZhuaguiMaxRuns()));
        botProperties.setSummonSkillCleanEnabled(readBoolean(properties, SUMMON_SKILL_CLEAN_ENABLED, botProperties.isSummonSkillCleanEnabled()));
        botProperties.setSummonSkillCleanIntervalMs(readLong(properties, SUMMON_SKILL_CLEAN_INTERVAL_MS, botProperties.getSummonSkillCleanIntervalMs()));
        botProperties.setXiuluoHealPetMaintenanceIntervalMs(readLong(properties, XIULUO_HEAL_PET_MAINTENANCE_INTERVAL_MS, botProperties.getXiuluoHealPetMaintenanceIntervalMs()));
        botProperties.setXiuluoRepairEquipmentMaintenanceIntervalMs(readLong(properties, XIULUO_REPAIR_EQUIPMENT_MAINTENANCE_INTERVAL_MS, botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs()));
        botProperties.setXiuluoMaintenanceRunImmediatelyOnStart(readBoolean(properties, XIULUO_MAINTENANCE_RUN_IMMEDIATELY_ON_START, botProperties.isXiuluoMaintenanceRunImmediatelyOnStart()));
        botProperties.setTaskStartupPreparationEnabled(readBoolean(properties, TASK_STARTUP_PREPARATION_ENABLED, botProperties.isTaskStartupPreparationEnabled()));
        botProperties.setPlayerHpSupplyEnabled(readBoolean(properties, PLAYER_HP_SUPPLY_ENABLED, botProperties.isPlayerHpSupplyEnabled()));
        botProperties.setPlayerHpSupplyThreshold(readInt(properties, PLAYER_HP_SUPPLY_THRESHOLD, botProperties.getPlayerHpSupplyThreshold()));
        botProperties.setPlayerMpSupplyEnabled(readBoolean(properties, PLAYER_MP_SUPPLY_ENABLED, botProperties.isPlayerMpSupplyEnabled()));
        botProperties.setPlayerMpSupplyThreshold(readInt(properties, PLAYER_MP_SUPPLY_THRESHOLD, botProperties.getPlayerMpSupplyThreshold()));
        botProperties.setPetHpSupplyEnabled(readBoolean(properties, PET_HP_SUPPLY_ENABLED, botProperties.isPetHpSupplyEnabled()));
        botProperties.setPetHpSupplyThreshold(readInt(properties, PET_HP_SUPPLY_THRESHOLD, botProperties.getPetHpSupplyThreshold()));
        botProperties.setPetMpSupplyEnabled(readBoolean(properties, PET_MP_SUPPLY_ENABLED, botProperties.isPetMpSupplyEnabled()));
        botProperties.setPetMpSupplyThreshold(readInt(properties, PET_MP_SUPPLY_THRESHOLD, botProperties.getPetMpSupplyThreshold()));
        log.info("UI game settings loaded: path={}", SETTINGS_PATH);
    }

    /**
     * Save current UI-backed game settings.
     *
     * @param botProperties mutable runtime configuration after UI normalization.
     */
    public void save(BotProperties botProperties) {
        if (botProperties == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty(XIULUO_MAX_RUNS, String.valueOf(botProperties.getXiuluoMaxRuns()));
        properties.setProperty(WUHUAN_MAX_RUNS, String.valueOf(botProperties.getWuhuanMaxRuns()));
        properties.setProperty(FIVEFOLD_MAX_RUNS, String.valueOf(botProperties.getFivefoldMaxRuns()));
        properties.setProperty(TIANTING_MAX_RUNS, String.valueOf(botProperties.getTiantingMaxRuns()));
        properties.setProperty(ZHUAGUI_MAX_RUNS, String.valueOf(botProperties.getZhuaguiMaxRuns()));
        properties.setProperty(SUMMON_SKILL_CLEAN_ENABLED, String.valueOf(botProperties.isSummonSkillCleanEnabled()));
        properties.setProperty(SUMMON_SKILL_CLEAN_INTERVAL_MS, String.valueOf(botProperties.getSummonSkillCleanIntervalMs()));
        properties.setProperty(XIULUO_HEAL_PET_MAINTENANCE_INTERVAL_MS, String.valueOf(botProperties.getXiuluoHealPetMaintenanceIntervalMs()));
        properties.setProperty(XIULUO_REPAIR_EQUIPMENT_MAINTENANCE_INTERVAL_MS, String.valueOf(botProperties.getXiuluoRepairEquipmentMaintenanceIntervalMs()));
        properties.setProperty(XIULUO_MAINTENANCE_RUN_IMMEDIATELY_ON_START, String.valueOf(botProperties.isXiuluoMaintenanceRunImmediatelyOnStart()));
        properties.setProperty(TASK_STARTUP_PREPARATION_ENABLED, String.valueOf(botProperties.isTaskStartupPreparationEnabled()));
        properties.setProperty(PLAYER_HP_SUPPLY_ENABLED, String.valueOf(botProperties.isPlayerHpSupplyEnabled()));
        properties.setProperty(PLAYER_HP_SUPPLY_THRESHOLD, String.valueOf(botProperties.getPlayerHpSupplyThreshold()));
        properties.setProperty(PLAYER_MP_SUPPLY_ENABLED, String.valueOf(botProperties.isPlayerMpSupplyEnabled()));
        properties.setProperty(PLAYER_MP_SUPPLY_THRESHOLD, String.valueOf(botProperties.getPlayerMpSupplyThreshold()));
        properties.setProperty(PET_HP_SUPPLY_ENABLED, String.valueOf(botProperties.isPetHpSupplyEnabled()));
        properties.setProperty(PET_HP_SUPPLY_THRESHOLD, String.valueOf(botProperties.getPetHpSupplyThreshold()));
        properties.setProperty(PET_MP_SUPPLY_ENABLED, String.valueOf(botProperties.isPetMpSupplyEnabled()));
        properties.setProperty(PET_MP_SUPPLY_THRESHOLD, String.valueOf(botProperties.getPetMpSupplyThreshold()));
        try {
            Path parent = SETTINGS_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream outputStream = Files.newOutputStream(SETTINGS_PATH)) {
                properties.store(outputStream, "DHXY JavaFX UI game settings");
            }
        } catch (IOException e) {
            log.warn("UI game settings save failed: path={} reason={}", SETTINGS_PATH, e.getMessage(), e);
        }
    }

    private int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long readLong(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }
}
