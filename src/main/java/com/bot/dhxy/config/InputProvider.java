package com.bot.dhxy.config;

public interface InputProvider {
    void clickLeft(int x, int y, int delayMs);
    void clickRight(int x, int y, int delayMs);
    void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs);

    void pressAlt2();

    void pressEnter();

    void pasteText(String text);

    void typeTextUnicode(String text);

    // 🌟 新增：鼠标向下滚动
    // 参数 clicks 代表滚动的格数 (通常一格就是滚动一下)
    void scrollDown(int clicks);

    // (可选) 顺手把向上滚动也加上以备不时之需
    void scrollUp(int clicks);
}
