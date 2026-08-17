package com.bot.dhxy.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated // 🌟 必须加这个注解，上面的 @NotNull 等校验才会生效
@ConfigurationProperties(prefix = "bot.dhxy")
public class BotProperties {
    /**
     * 窗口识别关键字
     */
    @NotBlank(message = "配置缺失: bot.dhxy.window-keyword 不能为空")
    private String windowKeyword;

    /**
     * 人物/宝宝血法补给开关与阈值。
     * 阈值会规整到 30 / 50 / 70，方便后续 UI 直接做固定选项。
     */
    private boolean playerHpSupplyEnabled = true;
    private int playerHpSupplyThreshold = 70;
    private boolean playerMpSupplyEnabled = true;
    private int playerMpSupplyThreshold = 70;
    private boolean petHpSupplyEnabled = true;
    private int petHpSupplyThreshold = 70;
    private boolean petMpSupplyEnabled = true;
    private int petMpSupplyThreshold = 70;


    /**
     * 召唤兽尾部普通技能清理配置。
     * 这里只提供能力开关和节流参数，具体任务是否调用由任务层决定。
     */
    private boolean summonSkillCleanEnabled = true;
    private long summonSkillCleanIntervalMs = 20 * 60 * 1000L;
    private long xiuluoHealPetMaintenanceIntervalMs = 30 * 60 * 1000L;
    private long xiuluoRepairEquipmentMaintenanceIntervalMs = 55 * 60 * 1000L;
    private boolean xiuluoMaintenanceRunImmediatelyOnStart = false;
    /** Skip a newly accepted Xiuluo boss assignment when its STORY contains enough red warning pixels. */
    private boolean xiuluoSkipBossEnabled = false;

    /**
     * CR120 通用盒子开关。
     *
     * 队长默认处理盒子；队员默认不处理，避免首次接入时成员战后抢输入。
     */
    private boolean leaderCommonBoxEnabled = true;
    private boolean memberCommonBoxEnabled = false;

    /**
     * Development switch for the slow pre-task UI preparation chain.
     *
     * <p>When disabled, leader tasks skip startup-only checks such as Alt+1 map options, Alt+U
     * expand/zoom state, and Alt+6 visibility setup. Keep this off during fast local debugging, and
     * enable it when validating real multi-window runs.</p>
     */
    private boolean taskStartupPreparationEnabled = true;

    /** Whether normal task runs may detect low double-experience time and claim another session. */
    private boolean doubleExperienceClaimEnabled = true;

    /**
     * Game-task run count limits surfaced by the JavaFX Settings page.
     * A value <= 0 means "keep running until manually stopped" for task code that supports it.
     */
    private int wuhuanMaxRuns = 1;
    private int fivefoldMaxRuns = 1;
    /** G004 running duration in minutes; zero means unlimited. */
    private int wildBattleDurationMinutes = 0;
    /** Standalone auto-battle duration in minutes; zero means unlimited. */
    private int autoBattleDurationMinutes = 0;
    /** G005 天庭 round count, one round being six sub-quests; zero means unlimited. */
    private int tiantingMaxRuns = 1;

    /**
     * 修罗任务第一版配置。后续 UI 会把这些值暴露成可选项。
     */
    private int xiuluoMaxRuns = 1;
    private int xinshouTrainingMaxRuns = 1;
    private int catchGhostMaxRuns = 1;
    private int ghostKingMaxRuns = 1;

    /**
     * UI map-survey target name used by the manual map-template/camera-boundary tools.
     */
    private String mapSurveyMapName = "";

}
