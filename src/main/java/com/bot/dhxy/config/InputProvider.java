package com.bot.dhxy.config;

public interface InputProvider {
    void clickLeft(int x, int y, int delayMs);
    void clickRight(int x, int y, int delayMs);
    void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs);

    void ctrlClickNpcTarget(int npcX, int npcY, int yellowNpcX, int yellowNpcY, int delayMs);
    void moveMouse(int x, int y);
    void holdCtrl();
    void releaseCtrl();

    void pressAlt1();
    void pressAlt2();
    void pressAlt4();
    void pressAltE();
    void pressAltQ();

    void pressEnter();
    void pasteText(String text);
    void typeTextUnicode(String text);
    void scrollDown(int clicks);
    void pressAlt8();
    void dragAndDrop(int startX, int startY, int endX, int endY);
    void scrollUp(int clicks);
}
