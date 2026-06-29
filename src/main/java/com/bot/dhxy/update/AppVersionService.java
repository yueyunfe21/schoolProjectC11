package com.bot.dhxy.update;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppVersionService {
    private final UpdateProperties properties;

    /**
     * Returns the application version used by update checks.
     *
     * @return configured local app version; this is separate from Maven packaging until the release pipeline is added.
     */
    public String currentVersion() {
        return properties.getCurrentVersion();
    }
}
