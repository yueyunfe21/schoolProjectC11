package com.bot.dhxy.cloud.turn;

import com.bot.dhxy.cloud.turn.protocol.TurnRequest;
import com.bot.dhxy.cloud.task.NpcClickSmartCloudSession;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueMessage;
import com.bot.dhxy.cloud.task.NpcClickSmartQueueOutcome;

/**
 * Transport boundary for one client-initiated turn exchange and one conditional template download.
 */
public interface TurnClient {

    /**
     * Sends one turn request exactly once.
     *
     * @param request current bound-window request and nullable previous outcome; non-null
     * @param optionalPng nullable raw PNG bytes matching the previous outcome frame metadata; never Base64
     * @return a validated turn response whose acknowledgement state is explicitly accepted
     * @throws TurnTransportException on configuration, serialization, network, HTTP, bound, parse, or contract failure
     */
    TurnExchangeResult exchange(TurnRequest request, byte[] optionalPng) throws TurnTransportException;

    /**
     * Downloads one canonical template key with optional conditional ETag validation.
     *
     * @param templateKey canonical wire key such as {@code images/template/dialog/example.png}; non-blank
     * @param ifNoneMatch nullable exact quoted ETag from an earlier download
     * @return a typed 200 download or 304 not-modified result
     * @throws TurnTransportException on configuration, network, HTTP, bound, content, or hash failure
     */
    TurnTemplateDownload downloadTemplate(String templateKey, String ifNoneMatch)
            throws TurnTransportException;

    default NpcClickSmartCloudSession openNpcArrivalFrame(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId)
            throws TurnTransportException {
        throw new TurnTransportException(
                TurnTransportException.Kind.REQUEST_CONTRACT,
                "NPC arrival-frame FIFO transport is unavailable");
    }

    default NpcClickSmartQueueMessage pollNpcArrivalFrame(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId)
            throws TurnTransportException {
        throw new TurnTransportException(
                TurnTransportException.Kind.REQUEST_CONTRACT,
                "NPC arrival-frame FIFO transport is unavailable");
    }

    default void replaceNpcArrivalFrame(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId,
            long frameId, long generation,
            long capturedAtMs, byte[] pngBytes) throws TurnTransportException {
        throw new TurnTransportException(
                TurnTransportException.Kind.REQUEST_CONTRACT,
                "NPC arrival-frame replacement transport is unavailable");
    }

    default void reportNpcArrivalFrameOutcome(
            String tenantId, String deviceId, String windowId, String hwnd,
            String observationRunId, String businessTaskRunId, String intentId,
            NpcClickSmartQueueMessage message,
            NpcClickSmartQueueOutcome outcome,
            String reason) throws TurnTransportException {
        throw new TurnTransportException(
                TurnTransportException.Kind.REQUEST_CONTRACT,
                "NPC arrival-frame outcome transport is unavailable");
    }
}
