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

    @Data
    public static class OcrConfig {
        @NotBlank(message = "OCR AppID 不能为空")
        private String appId;

        @NotBlank(message = "OCR API Key 不能为空")
        private String apiKey;

        @NotBlank(message = "OCR Secret Key 不能为空")
        private String secretKey;
    }
}