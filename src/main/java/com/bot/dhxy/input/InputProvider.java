package com.bot.dhxy.input;

/**
 * Low-level physical input abstraction.
 *
 * <p>All mouse coordinates are screen-absolute pixels. Production task code should normally access
 * these operations through {@link InputSequences} or the input worker; direct use is reserved for
 * code already running inside an exclusive input callback.</p>
 */
public interface InputProvider {
    /** Left-click at a screen-absolute point and wait the given milliseconds. */
    void clickLeft(int x, int y, int delayMs);

    /** Right-click at a screen-absolute point and wait the given milliseconds. */
    void clickRight(int x, int y, int delayMs);

    /** Double-right-click at a screen-absolute point. */
    void doubleRightClick(int x, int y, int clickDelayMs, int intervalMs);

    /** Move the physical cursor to a screen-absolute point. */
    void moveMouse(int x, int y);

    /** Press and hold Ctrl. */
    void holdCtrl();

    /** Release Ctrl. */
    void releaseCtrl();

    /** Press Ctrl+C once. */
    void pressCtrlC();

    /** Press Ctrl+A once. */
    void pressCtrlA();

    /** Press Alt+1. */
    void pressAlt1();

    /** Press Alt+2. */
    void pressAlt2();

    /** Press Alt+4. */
    void pressAlt4();

    /** Press Alt+6. */
    void pressAlt6();

    /** Press Alt+E. */
    void pressAltE();

    /** Press Alt+Q. */
    void pressAltQ();

    /** Press Alt+A. */
    void pressAltA();

    /** Press Enter. */
    void pressEnter();

    /** Paste text through the active clipboard/input implementation. */
    void pasteText(String text);

    /** Type Unicode text through the active input implementation. */
    void typeTextUnicode(String text);

    /** Scroll mouse wheel down by the given click count. */
    void scrollDown(int clicks);

    /** Press Alt+8. */
    void pressAlt8();

    /** Press Alt+T. */
    void pressAltT();

    /** Press Alt+U. */
    void pressAltU();

    /** Press Alt+O. */
    void pressAltO();

    /** Drag from one screen-absolute point to another. */
    void dragAndDrop(int startX, int startY, int endX, int endY);

    /** Scroll mouse wheel up by the given click count. */
    void scrollUp(int clicks);
}
