package com.bot.dhxy.input.action;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class InputActionDeadLetter {

    private final List<InputActionRequest> failedRequests = new CopyOnWriteArrayList<>();

    public void record(InputActionRequest request, Throwable error) {
        failedRequests.add(request);
        String message = error == null
                ? (request == null || request.getCancellationReason() == null ? "unknown" : request.getCancellationReason())
                : error.getMessage();
        log.warn("Input action moved to dead letter: windowId={} description={} actions={} exclusive={} reason={}",
                request == null ? "null" : request.getWindowId(),
                request == null ? "null" : request.getDescription(),
                request == null ? -1 : request.getActions().size(),
                request != null && request.hasExclusiveCallback(),
                message);
        if (error != null) {
            log.warn("Input action dead letter stack: windowId={} description={}",
                    request == null ? "null" : request.getWindowId(),
                    request == null ? "null" : request.getDescription(),
                    error);
        }
    }

    public List<InputActionRequest> snapshot() {
        return List.copyOf(failedRequests);
    }
}
