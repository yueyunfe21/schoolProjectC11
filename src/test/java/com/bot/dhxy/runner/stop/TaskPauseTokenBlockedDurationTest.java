package com.bot.dhxy.runner.stop;

/**
 * Verifies that cooperative pause checkpoints expose their blocked wall-clock time so task-local
 * timers can compensate user pauses without counting them as business timeout.
 */
public class TaskPauseTokenBlockedDurationTest {

    public static void main(String[] args) throws Exception {
        TaskPauseToken token = new TaskPauseToken();
        token.requestPause("test pause");

        Thread resumeThread = new Thread(() -> {
            try {
                Thread.sleep(80L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            token.resume();
        }, "pause-token-test-resume");
        resumeThread.start();

        long blockedMs = token.waitIfPaused(new TaskStopToken());
        resumeThread.join();

        require(blockedMs >= 40L,
                "pause checkpoint should report the time spent blocked; actual=" + blockedMs);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
