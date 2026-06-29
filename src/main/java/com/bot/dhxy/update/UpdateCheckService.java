package com.bot.dhxy.update;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCheckService {
    private final UpdateProperties properties;
    private final AppVersionService appVersionService;
    private final UpdateManifestProvider manifestProvider;

    /**
     * Checks whether a newer app version exists.
     *
     * @return typed result for UI display; this method only reads metadata and never downloads or replaces files.
     */
    public UpdateCheckResult checkForUpdates() {
        String currentVersion = appVersionService.currentVersion();
        if (!properties.isEnabled()) {
            return UpdateCheckResult.disabled(currentVersion);
        }
        try {
            UpdateManifest manifest = manifestProvider.fetchLatestManifest();
            if (manifest == null || manifest.latestVersion() == null || manifest.latestVersion().isBlank()) {
                return UpdateCheckResult.checkFailed(currentVersion, "manifest 缺少 latestVersion");
            }
            int comparison = UpdateVersionComparator.compare(manifest.latestVersion(), currentVersion);
            if (comparison > 0) {
                return UpdateCheckResult.updateAvailable(currentVersion, manifest);
            }
            return UpdateCheckResult.noUpdate(currentVersion, manifest);
        } catch (RuntimeException e) {
            return UpdateCheckResult.checkFailed(currentVersion, e.getMessage());
        }
    }
}
