package com.bot.dhxy.update;

public class UpdateCheckServiceTest {

    public static void main(String[] args) {
        reportsDisabledWhenSwitchIsOff();
        reportsNoUpdateWhenVersionsMatch();
        reportsUpdateAvailableWhenManifestIsNewer();
        reportsCheckFailedWhenProviderThrows();
    }

    private static void reportsDisabledWhenSwitchIsOff() {
        UpdateProperties properties = properties("1.0.0");
        properties.setEnabled(false);
        UpdateCheckService service = new UpdateCheckService(properties,
                new AppVersionService(properties),
                () -> new UpdateManifest("1.0.1", "https://example.com/app.zip",
                        "abc", false, "notes", "2026-06-23"));

        UpdateCheckResult result = service.checkForUpdates();

        assertStatus("disabled", UpdateStatus.DISABLED, result.status());
    }

    private static void reportsNoUpdateWhenVersionsMatch() {
        UpdateProperties properties = properties("1.0.0");
        UpdateCheckService service = new UpdateCheckService(properties,
                new AppVersionService(properties),
                () -> new UpdateManifest("1.0.0", "https://example.com/app.zip",
                        "abc", false, "notes", "2026-06-23"));

        UpdateCheckResult result = service.checkForUpdates();

        assertStatus("same version", UpdateStatus.NO_UPDATE, result.status());
    }

    private static void reportsUpdateAvailableWhenManifestIsNewer() {
        UpdateProperties properties = properties("1.0.0");
        UpdateCheckService service = new UpdateCheckService(properties,
                new AppVersionService(properties),
                () -> new UpdateManifest("1.0.1", "https://example.com/app.zip",
                        "abc", true, "notes", "2026-06-23"));

        UpdateCheckResult result = service.checkForUpdates();

        assertStatus("newer version", UpdateStatus.UPDATE_AVAILABLE, result.status());
        if (!"1.0.1".equals(result.manifest().latestVersion())) {
            throw new AssertionError("newer version: manifest not preserved");
        }
    }

    private static void reportsCheckFailedWhenProviderThrows() {
        UpdateProperties properties = properties("1.0.0");
        UpdateCheckService service = new UpdateCheckService(properties,
                new AppVersionService(properties),
                () -> {
                    throw new IllegalStateException("network down");
                });

        UpdateCheckResult result = service.checkForUpdates();

        assertStatus("provider failure", UpdateStatus.CHECK_FAILED, result.status());
    }

    private static UpdateProperties properties(String currentVersion) {
        UpdateProperties properties = new UpdateProperties();
        properties.setEnabled(true);
        properties.setCurrentVersion(currentVersion);
        return properties;
    }

    private static void assertStatus(String caseName, UpdateStatus expected, UpdateStatus actual) {
        if (actual != expected) {
            throw new AssertionError(caseName + ": expected=" + expected + " actual=" + actual);
        }
    }
}
