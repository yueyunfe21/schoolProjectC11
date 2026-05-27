package com.bot.dhxy.model.ocr;

/**
 * Conservative learned direct-click recommendation for one NPC target.
 *
 * @param key stable vision-memory key used to derive the point.
 * @param x window-relative X coordinate in the 1024x768 game client.
 * @param y window-relative Y coordinate in the 1024x768 game client.
 * @param sampleCount number of recent successful samples used.
 * @param spreadPx maximum distance in pixels from the averaged point.
 * @param lastOutcome last recorded NPC-click outcome for diagnostics.
 */
public record LearnedNpcClickPoint(String key,
                                   int x,
                                   int y,
                                   int sampleCount,
                                   int spreadPx,
                                   String lastOutcome) {
    /**
     * @return compact diagnostic text for logs and strategy result messages.
     */
    public String toSummaryText() {
        return "key=" + key
                + " point=(" + x + "," + y + ")"
                + " samples=" + sampleCount
                + " spreadPx=" + spreadPx
                + " lastOutcome=" + safe(lastOutcome);
    }

    private static String safe(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }
}
