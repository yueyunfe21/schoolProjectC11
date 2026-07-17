package com.bot.dhxy.cloud.remote;

/** Explicit typed client for the inactive remote task-run lifecycle endpoint. */
public interface RemoteTaskRunApiClient {

    RemoteTaskRunBinding prepare(
            RemoteTaskRunScope scope,
            String startRequestId,
            String taskType,
            RemoteTaskRunWindow window);

    RemoteTaskRunBinding status(RemoteTaskRunScope scope, String taskRunId);

    RemoteTaskRunBinding activate(RemoteTaskRunScope scope, String taskRunId, long expectedRevision);

    /**
     * Confirms execution for the exact active task-run binding published by the local client.
     *
     * @param scope exact tenant, user, device, and client-session scope
     * @param taskRunId canonical cloud task-run id
     * @param expectedRevision non-negative active run revision
     * @param window exact windowId/nativeHandle/processId/playerIdentityEpoch tuple
     * @return unchanged current remote task-run binding
     */
    RemoteTaskRunBinding confirmExecution(
            RemoteTaskRunScope scope,
            String taskRunId,
            long expectedRevision,
            RemoteTaskRunWindow window);

    RemoteTaskRunReceipt confirmResumedExecutorReady(RemoteTaskRunActionRequest request);

    RemoteTaskRunBinding pause(RemoteTaskRunScope scope, String taskRunId, long expectedRevision);

    RemoteTaskRunBinding resume(RemoteTaskRunScope scope, String taskRunId, long expectedRevision);

    RemoteTaskRunBinding stop(RemoteTaskRunScope scope, String taskRunId, long expectedRevision);

    RemoteTaskRunBinding complete(RemoteTaskRunScope scope, String taskRunId, long expectedRevision);

    RemoteTaskRunBinding findReplacement(RemoteTaskRunScope replacementScope, String startRequestId);

    RemoteTaskRunBinding stopReplacement(
            RemoteTaskRunScope replacementScope,
            String taskRunId,
            long expectedRevision);
}
