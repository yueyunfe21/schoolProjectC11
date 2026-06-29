package com.bot.dhxy.update;

public record UpdateCheckResult(UpdateStatus status,
                                String currentVersion,
                                UpdateManifest manifest,
                                String message) {

    public static UpdateCheckResult disabled(String currentVersion) {
        return new UpdateCheckResult(UpdateStatus.DISABLED, currentVersion, null, "更新检查未启用");
    }

    public static UpdateCheckResult noUpdate(String currentVersion, UpdateManifest manifest) {
        return new UpdateCheckResult(UpdateStatus.NO_UPDATE, currentVersion, manifest,
                "当前已是最新版本：" + currentVersion);
    }

    public static UpdateCheckResult updateAvailable(String currentVersion, UpdateManifest manifest) {
        return new UpdateCheckResult(UpdateStatus.UPDATE_AVAILABLE, currentVersion, manifest,
                "发现新版本：" + manifest.latestVersion());
    }

    public static UpdateCheckResult checkFailed(String currentVersion, String message) {
        return new UpdateCheckResult(UpdateStatus.CHECK_FAILED, currentVersion, null,
                "更新检查失败：" + message);
    }
}
