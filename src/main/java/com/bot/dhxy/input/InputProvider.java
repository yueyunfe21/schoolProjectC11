package com.bot.dhxy.input;

/**
 * Low-level physical input abstraction.
 *
 * <p>All mouse coordinates are screen-absolute pixels. Production task code should normally access
 * these operations through {@link InputSequences} or the input worker; direct use is reserved for
 * code already running inside an exclusive input callback.</p>
 */
public interface InputProvider {
    /**
     * Whether ordinary keyboard actions require the same focused transaction as mouse input.
     * FakerInput returns true; the legacy WinAPI provider returns false so existing routing stays unchanged.
     */
    default boolean requiresForegroundKeyboard() {
        return false;
    }

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


    /** Press Ctrl+U once. */
    void pressCtrlU();

    /** Press Ctrl+A once. */
    void pressCtrlA();

    /** Press Alt+1. */
    void pressAlt1();

    /** Press Alt+2. */
    void pressAlt2();

    /** Press Alt+4. */
    void pressAlt4();

    /** Press Alt+5. */
    default void pressAlt5() {
        throw new UnsupportedOperationException("Alt+5 is not supported by this input provider");
    }

    /** Press Alt+6. */
    void pressAlt6();

    /** Press Alt+E. */
    void pressAltE();

    /** Press Alt+Q. */
    void pressAltQ();

    /** Press Alt+A. */
    void pressAltA();

    /** Press Alt+B. */
    default void pressAltB() {
        throw new UnsupportedOperationException("Alt+B is not supported by this input provider");
    }

    /** Press Alt+C. */
    void pressAltC();

    /** Press Enter. */
    void pressEnter();

    /** Press Escape. */
    default void pressEscape() {
        throw new UnsupportedOperationException("Escape is not supported by this input provider");
    }

    /** Paste text through the active clipboard/input implementation. */
    void pasteText(String text);

    /** Type Unicode text through the active input implementation. */
    void typeTextUnicode(String text);

    /** Type lowercase ASCII letters/digits as physical keyboard usages; no clipboard or Unicode path. */
    void typeTextAscii(String text);

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

    /** Start and retain the new-player hold-sweep with the left button still down. */
    default void holdSweepWithoutRelease(
            int startX, int startY, int leftX, int rightX, int endY, int rowStepPx) {
        throw new UnsupportedOperationException("Retained hold-sweep is not supported by this input provider");
    }

    /** Continue a retained new-player sweep without changing the held left-button state. */
    default void sweepWhileLeftHeld(
            int startX, int startY, int leftX, int rightX, int endY, int rowStepPx) {
        throw new UnsupportedOperationException("Retained sweep is not supported by this input provider");
    }

    /** Release a left button retained by a previous sweep. */
    default void releaseLeftButton() {
        throw new UnsupportedOperationException("Retained left-button release is not supported by this input provider");
    }

    /** Release any virtual input state retained by the provider after a request abort or completion. */
    default void releaseAllInput() {
    }
}
