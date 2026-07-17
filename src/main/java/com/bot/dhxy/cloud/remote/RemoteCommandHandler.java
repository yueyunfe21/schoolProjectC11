package com.bot.dhxy.cloud.remote;

@FunctionalInterface
public interface RemoteCommandHandler {

    /**
     * Handles one command synchronously and returns its correlated terminal outcome.
     *
     * @param command one validated command envelope for a single mechanical operation
     * @return non-null terminal outcome for the same contract, operation, request, action, and task run
     * @throws Exception when the handler cannot produce a terminal outcome; the polling loop then stops
     */
    RemoteGameOutcomeEnvelope handle(RemoteGameCommand command) throws Exception;
}
