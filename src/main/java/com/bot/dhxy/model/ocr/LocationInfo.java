package com.bot.dhxy.model.ocr;

/**
 * Parsed in-game map location.
 *
 * <p>The coordinates are logical game map coordinates read from the map label, not screen pixels or
 * minimap click coordinates.</p>
 */
public class LocationInfo {
    /** OCR map name as displayed by the game; not normalized by this DTO. */
    public String mapName;
    /** Logical in-game map X coordinate, not a screen pixel. */
    public int x;
    /** Logical in-game map Y coordinate, not a screen pixel. */
    public int y;

    /**
     * Create a parsed map location.
     *
     * @param mapName OCR map name; may contain provider OCR mistakes when callers accept a fuzzy
     *                match.
     * @param x logical in-game map X coordinate.
     * @param y logical in-game map Y coordinate.
     */
    public LocationInfo(String mapName, int x, int y) {
        this.mapName = mapName;
        this.x = x;
        this.y = y;
    }

    /**
     * Format the parsed location for logs.
     *
     * @return human-readable map and logical coordinate text.
     */
    @Override
    public String toString() {
        return String.format("当前位置: 地图=%s | X=%d | Y=%d", mapName, x, y);
    }
}
