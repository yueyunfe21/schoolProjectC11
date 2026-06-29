package com.bot.dhxy.update;

public record UpdateManifest(String latestVersion,
                             String downloadUrl,
                             String sha256,
                             boolean mandatory,
                             String releaseNotes,
                             String publishedAt) {
}
