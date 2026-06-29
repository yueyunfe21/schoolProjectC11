package com.bot.dhxy.update;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MockUpdateManifestProvider implements UpdateManifestProvider {
    private final UpdateProperties properties;

    @Override
    public UpdateManifest fetchLatestManifest() {
        return new UpdateManifest(
                properties.getMockLatestVersion(),
                properties.getMockDownloadUrl(),
                properties.getMockSha256(),
                properties.isMockMandatory(),
                properties.getMockReleaseNotes(),
                properties.getMockPublishedAt());
    }
}
