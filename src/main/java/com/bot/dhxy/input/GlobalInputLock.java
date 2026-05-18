package com.bot.dhxy.input;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 真实鼠标/键盘是全局资源。
 *
 * 多窗口并行时，截图识别可以并行，但真实点击、按键、拖动必须经过这个全局锁，
 * 否则多个窗口会互相抢鼠标。
 */
@Component
public class GlobalInputLock {

    private final ReentrantLock lock = new ReentrantLock(true);

    public void runWithLock(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public <T> T callWithLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public boolean isLocked() {
        return lock.isLocked();
    }
}
