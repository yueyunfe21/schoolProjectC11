package com.bot.dhxy.update;

public interface UpdateManifestProvider {

    /**
     * Fetches the latest update metadata from the configured source.
     *
     * @return latest manifest metadata; never mutates local files or launches an updater.
     */
    UpdateManifest fetchLatestManifest();
}
