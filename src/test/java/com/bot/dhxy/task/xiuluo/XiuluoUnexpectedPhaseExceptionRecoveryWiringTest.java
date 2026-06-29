package com.bot.dhxy.task.xiuluo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Source guard for 修罗 unexpected phase exceptions.
 *
 * <p>Unexpected business/runtime exceptions inside one 修罗 phase must not end the whole
 * {@code xiuluoMaxRuns} task. They should be treated like a failed phase attempt and recover through
 * the same reaccept path, while explicit stop/fatal task exceptions remain designed exits.</p>
 */
public class XiuluoUnexpectedPhaseExceptionRecoveryWiringTest {

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        String task = Files.readString(root.resolve(
                "src/main/java/com/bot/dhxy/task/xiuluo/XiuluoTaskV2.java"), StandardCharsets.UTF_8);

        String phaseLoop = between(task,
                "private TaskRunResult runRoundPhases(",
                "private void ensureStartupIncenseBeforeHotStart(");
        require(phaseLoop.contains("catch (RuntimeException e)"),
                "runRoundPhases must catch ordinary RuntimeException at the phase boundary");
        require(phaseLoop.contains("restartRoundAfterUnexpectedPhaseException("),
                "ordinary phase RuntimeException must recover/reaccept the same round");
        require(phaseLoop.contains("e instanceof TaskFatalException"),
                "TaskFatalException must stay a designed exit instead of being swallowed");
        require(phaseLoop.contains("e instanceof TaskStopRequestedException"),
                "explicit stop must stay STOPPED instead of being converted to recovery");

        String recovery = between(task,
                "private XiuluoRoundContext restartRoundAfterUnexpectedPhaseException(",
                "private XiuluoRoundContext restartRoundAfterPhaseFailure(");
        require(recovery.contains("XiuluoStepOutcome.failed("),
                "unexpected exception recovery must be represented as a failed phase outcome");
        require(recovery.contains("restartRoundAfterPhaseFailure("),
                "unexpected exception recovery must reuse the existing reaccept path and failure limit");
        require(recovery.contains("phase exception: "),
                "unexpected exception recovery must log/record the exception class");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        if (startIndex < 0) {
            throw new AssertionError("Missing source marker: " + start);
        }
        int endIndex = source.indexOf(end, startIndex + start.length());
        if (endIndex < 0) {
            throw new AssertionError("Missing source end marker: " + end);
        }
        return source.substring(startIndex, endIndex);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
