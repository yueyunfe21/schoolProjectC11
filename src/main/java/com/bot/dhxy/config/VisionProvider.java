package com.bot.dhxy.config;

import java.awt.image.BufferedImage;

public interface VisionProvider {
    // 老方法：存硬盘
    boolean captureScreen(String savePath);

    // 【新增方法】：内存级局部截图！直接返回图像对象，不存硬盘
    BufferedImage captureRegionInMemory(int x, int y, int width, int height);

    // 【新增】已知左上角(x1, y1)和右下角(x2, y2)，直接在内存中截取该范围
    BufferedImage captureRegionByCoordinates(int x1, int y1, int x2, int y2);

    // 【新增】局部截图并存入硬盘
    boolean captureRegionToFile(String savePath, int x1, int y1, int x2, int y2);
}
