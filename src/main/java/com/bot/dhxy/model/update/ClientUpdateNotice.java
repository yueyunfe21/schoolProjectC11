package com.bot.dhxy.model.update;

import lombok.Builder;
import lombok.Value;
import java.net.URI;

/** Update information only; this object never authorizes installation or task execution. */
@Value
@Builder
public class ClientUpdateNotice {
    Status status;
    String message;
    String latestVersion;
    URI releasePage;
    boolean authorizedDownload;

    /** A revoked build may need the allowed recommended package even if its version number is lower. */
    public boolean hasInstallableRelease() {
        return (authorizedDownload || releasePage != null) && latestVersion != null && !latestVersion.isBlank() &&
                (status == Status.NEW_RELEASE || status == Status.UPDATE_REQUIRED);
    }

    public enum Status { NO_RELEASE, NO_NEWER_RELEASE, NEW_RELEASE, UPDATE_REQUIRED, DEVELOPMENT_BUILD, UNAVAILABLE }
}
