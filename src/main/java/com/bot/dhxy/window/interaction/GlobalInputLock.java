package com.bot.dhxy.window.interaction;

import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 全局真实鼠标/键盘输入锁。
 *
 * 真实鼠标是全局资源，多窗口并发时必须串行化输入操作。
 */
@Component
public class GlobalInputLock {

    private final ReentrantLock lock = new ReentrantLock(true);

    public void runLocked(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public <T> T callLocked(Callable<T> action) {
        lock.lock();
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("全局输入操作执行失败", e);
        } finally {
            lock.unlock();
        }
    }

    public boolean isLocked() {
        return lock.isLocked();
    }
}
