package com.bot.dhxy.core.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Caps bytes while receiving, before an HTTP response can grow an unbounded in-memory buffer. */
public final class BoundedHttpBodyHandler implements HttpResponse.BodyHandler<byte[]> {
    private final int maximumBytes;

    /** @param maximumBytes positive response-body byte limit, excluding HTTP headers. */
    public BoundedHttpBodyHandler(int maximumBytes) {
        if (maximumBytes <= 0) throw new IllegalArgumentException("maximumBytes must be positive");
        this.maximumBytes = maximumBytes;
    }

    @Override public HttpResponse.BodySubscriber<byte[]> apply(HttpResponse.ResponseInfo info) {
        return new LimitedSubscriber(maximumBytes);
    }

    private static final class LimitedSubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private final int maximumBytes;
        private Flow.Subscription subscription;
        private long received;
        private boolean terminal;

        private LimitedSubscriber(int maximumBytes) { this.maximumBytes = maximumBytes; }
        @Override public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        @Override public void onSubscribe(Flow.Subscription incoming) {
            subscription = incoming;
            delegate.onSubscribe(incoming);
        }
        @Override public void onNext(List<ByteBuffer> items) {
            if (terminal) return;
            for (ByteBuffer buffer : items) received += buffer.remaining();
            if (received > maximumBytes) {
                terminal = true;
                subscription.cancel();
                delegate.onError(new IOException("HTTP response exceeds authorized byte limit"));
                return;
            }
            delegate.onNext(items);
        }
        @Override public void onError(Throwable error) {
            if (!terminal) { terminal = true; delegate.onError(error); }
        }
        @Override public void onComplete() {
            if (!terminal) { terminal = true; delegate.onComplete(); }
        }
    }
}
