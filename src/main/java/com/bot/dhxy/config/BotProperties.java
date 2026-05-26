package com.bot.dhxy.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @Valid // 🌟 告诉 Spring：不仅要查外面的参数，还要进去查这个对象里面的参数！
    private OcrConfig ocr = new OcrConfig();

    // ==========================================
    // 🌟 修复：用 @NotNull 替换 @NotBlank，并将 int 改为 Integer
    // ==========================================
    @NotNull(message = "配置缺失: 客户端到地图坐标框滚动的 X 偏移量不能为空")
    private Integer anchor_windowTo_map_scroll_X; // 注意：建议这里的命名和 yaml 里严格保持一致，或者全用驼峰命名

    @NotNull(message = "配置缺失: 客户端到地图坐标框滚动的 Y 偏移量不能为空")
    private Integer anchor_windowTo_map_scroll_Y;

    @NotNull(message = "配置缺失: 客户端到地图坐标框的 Y 偏移量不能为空")
    private Integer anchor_windowTo_map_search_X;
    @NotNull(message = "配置缺失: 客户端到地图坐标框的 Y 偏移量不能为空")
    private Integer anchor_windowTo_map_search_Y;

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
     * 自动战斗挂机维护配置。
     */
    private long autoBattleRefreshIntervalMs = 120_000L;
    private long autoBattleUiCleanIntervalMs = 30_000L;
    private int returnTeamAreaX = 342;
    private int returnTeamAreaY = 57;
    private int returnTeamAreaW = 272;
    private int returnTeamAreaH = 69;
    private double returnTeamMatchRate = 0.85;
    private long returnTeamLeaderWaitTimeoutMs = 120_000L;
    private long returnTeamLeaderWaitPollMs = 3_000L;

    /**
     * 召唤兽尾部普通技能清理配置。
     * 这里只提供能力开关和节流参数，具体任务是否调用由任务层决定。
     */
    private boolean summonSkillCleanEnabled = true;
    private long summonSkillCleanIntervalMs = 20 * 60 * 1000L;
    private boolean summonSkillCleanRunImmediatelyOnStart = false;

    /**
     * Game-task run count limits surfaced by the JavaFX Settings page.
     * A value <= 0 means "keep running until manually stopped" for task code that supports it.
     * Some tasks are not wired to these limits yet; the UI still keeps the values here so task
     * implementations can consume one shared configuration object instead of inventing per-task knobs.
     */
    private int wuhuanMaxRuns = 1;
    private int fivefoldMaxRuns = 1;
    private int tiantingMaxRuns = 1;
    private int zhuaguiMaxRuns = 1;

    /**
     * 修罗任务第一版配置。后续 UI 会把这些值暴露成可选项。
     */
    private int xiuluoMaxRuns = 1;
    private boolean xiuluoAllowUnderFiveMembers = false;
    private long xiuluoReturnVerifyTimeoutMs = 10_000L;
    private long xiuluoReturnVerifyPollMs = 1_000L;

    /**
     * UI debug-only map transform calibration target.
     */
    private String debugMapCalibratorMapName = "";

    @Data
    public static class OcrConfig {
        /**
         * OCR provider:
         * baidu   - current cloud OCR behavior.
         * local   - local OCR sidecar only.
         * compare - return Baidu result, also call local OCR and log differences.
         * hybrid  - try local first; target-matching paths may retry Baidu when local text does not match.
         */
        private String provider = "baidu";

        /**
         * Local OCR sidecar base URL. See scripts/local_ocr_server.py.
         */
        private String localEndpoint = "http://127.0.0.1:18761";

        private int localTimeoutMs = 10_000;
        @NotBlank(message = "OCR AppID 不能为空")
        private String appId;

        @NotBlank(message = "OCR API Key 不能为空")
        private String apiKey;

        @NotBlank(message = "OCR Secret Key 不能为空")
        private String secretKey;
    }
}
