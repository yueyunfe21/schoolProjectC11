package com.bot.dhxy.model;

import lombok.Data;

@Data
public class MapCoordinate {
    private int x;
    private int y;

    public MapCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
