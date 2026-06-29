package com.bot.dhxy.window.dialog;

import com.bot.dhxy.model.dialog.PreparedDialogAction;
import com.bot.dhxy.model.dialog.DialogDetection;
import com.bot.dhxy.service.dialog.DialogOperation;
import com.bot.dhxy.task.model.TaskType;
import com.bot.dhxy.window.model.WindowDialogInterest;

import java.util.Optional;

/**
 * Task-owned dialog preparation adapter used by {@code WindowTaskRunner}.
 *
 * <p>The runner owns window observation, but task modules own business templates. Providers bridge
 * that boundary by preparing a click/action only for interests explicitly registered by the running
 * task. Implementations must not send mouse/keyboard input; they only return a prepared action.</p>
 */
public interface WindowDialogPreparationProvider {

    /**
     * @param taskType selected task for the observed window.
     * @param operation dialog operation requested by the task interest.
     * @return true when this provider owns the task/operation combination.
     */
    boolean supports(TaskType taskType, DialogOperation operation);

    /**
     * Prepare one dialog action for a visible dialog snapshot.
     *
     * @param interest current task interest. Its task and operation list are already scoped to one
     *                 bound window.
     * @param operation operation to prepare.
     * @param source log/debug source to stamp into prepared action metadata.
     * @return prepared action, or empty when the visible dialog does not match this operation.
     */
    Optional<PreparedDialogAction> prepare(WindowDialogInterest interest,
                                           DialogOperation operation,
                                           String source);

    /**
     * Prepare one dialog action using a tick-scoped detection that was already captured by the
     * runner. Implementations may ignore it by delegating to the legacy method, but should reuse it
     * when it matches the requested operation's dialog type.
     *
     * @param interest current task interest.
     * @param operation operation to prepare.
     * @param source log/debug source to stamp into prepared action metadata.
     * @param suppliedDetection optional detection captured earlier in the same watcher tick.
     * @return prepared action, or empty when the visible dialog does not match this operation.
     */
    default Optional<PreparedDialogAction> prepare(WindowDialogInterest interest,
                                                   DialogOperation operation,
                                                   String source,
                                                   DialogDetection suppliedDetection) {
        return prepare(interest, operation, source);
    }
}
