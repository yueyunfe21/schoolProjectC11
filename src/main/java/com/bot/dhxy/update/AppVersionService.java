package com.bot.dhxy.update;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppVersionService {
    private final String currentVersion;

    public AppVersionService(@Value("${bot.app-version:0.1.0-dev}") String currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * Returns the application version displayed in the JavaFX shell.
     *
     * @return configured local app version; this is separate from Maven packaging until the release pipeline is added.
     */
    public String currentVersion() {
        return currentVersion;
    }
}
