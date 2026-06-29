package com.bot.dhxy.update;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot.update")
public class UpdateProperties {
    private boolean enabled = false;
    private String currentVersion = "0.1.0-dev";
    private String provider = "mock";
    private String mockLatestVersion = "0.1.0-dev";
    private String mockDownloadUrl = "https://github.com/your-org/dhxy-release/releases";
    private String mockSha256 = "";
    private boolean mockMandatory = false;
    private String mockReleaseNotes = "当前为更新框架 mock 数据；后续接 public GitHub Release latest.json。";
    private String mockPublishedAt = "";
}
