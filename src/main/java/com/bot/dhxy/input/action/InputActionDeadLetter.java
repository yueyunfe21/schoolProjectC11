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
        String message = error == null ? "unknown" : error.getMessage();
        log.warn("Input action moved to dead letter: windowId={} description={} reason={}",
                request == null ? "null" : request.getWindowId(),
                request == null ? "null" : request.getDescription(),
                message);
    }

    public List<InputActionRequest> snapshot() {
        return List.copyOf(failedRequests);
    }
}
