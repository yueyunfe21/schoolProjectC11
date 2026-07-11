package com.bot.dhxy.cloud.decision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CloudDecisionMetricsService {

    private static final Logger log = LoggerFactory.getLogger(CloudDecisionMetricsService.class);
    private static final int DEFAULT_ELAPSED_WINDOW_SIZE = 64;
    private static final int DEFAULT_IMMEDIATE_LOG_SAMPLES = 3;
    private static final int DEFAULT_LOG_EVERY_SAMPLES = 20;
    private static final String UNKNOWN = "unknown";

    private final int maxElapsedSamples;
    private final int immediateLogSamples;
    private final int logEverySamples;
    private final Map<MetricsKey, MetricsBucket> buckets = new ConcurrentHashMap<>();

    public CloudDecisionMetricsService() {
        this(DEFAULT_ELAPSED_WINDOW_SIZE, DEFAULT_IMMEDIATE_LOG_SAMPLES, DEFAULT_LOG_EVERY_SAMPLES);
    }

    CloudDecisionMetricsService(int maxElapsedSamples, int immediateLogSamples, int logEverySamples) {
        this.maxElapsedSamples = Math.max(1, maxElapsedSamples);
        this.immediateLogSamples = Math.max(0, immediateLogSamples);
        this.logEverySamples = Math.max(1, logEverySamples);
    }

    /**
     * Records one cloud-decision sample and emits a bounded summary log only on the configured cadence.
     *
     * @param result completed cloud-decision result; its request supplies the service/mode/task/phase
     *               grouping key, and elapsed time is kept only in a fixed-size recent-sample window.
     * @return current metrics snapshot for the group after the sample is recorded.
     */
    public MetricsSnapshot record(CloudDecisionResult result) {
        MetricsKey key = MetricsKey.from(result);
        MetricsBucket bucket = buckets.computeIfAbsent(key, ignored -> new MetricsBucket());
        MetricsSnapshot snapshot;
        synchronized (bucket) {
            bucket.record(result, maxElapsedSamples);
            snapshot = bucket.snapshot(key, shouldLog(bucket.total));
        }
        if (snapshot.isLogEmitted()) {
            log.info(snapshot.toLogLine());
        }
        return snapshot;
    }

    private boolean shouldLog(long total) {
        return total <= immediateLogSamples || total % logEverySamples == 0L;
    }

    private static boolean fallbackLocal(CloudDecisionResult result) {
        return !result.isExecuted() && Objects.equals(result.getEffectiveDecision(), result.getLocalDecision());
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value;
    }

    private static String enumValue(Object value) {
        return value == null ? UNKNOWN : String.valueOf(value);
    }

    private static long percentile(ArrayDeque<Long> elapsedMs, int percentile) {
        if (elapsedMs.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(elapsedMs);
        sorted.sort(Comparator.naturalOrder());
        int index = (int) Math.ceil((percentile / 100.0d) * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static final class MetricsBucket {
        private long total;
        private long cloudSuccess;
        private long cloudFailure;
        private long agreement;
        private long disagreement;
        private long executed;
        private long fallbackLocal;
        private String lastFailureReason;
        private final ArrayDeque<Long> elapsedMs = new ArrayDeque<>();

        private void record(CloudDecisionResult result, int maxElapsedSamples) {
            total++;
            if (result.isCloudAvailable()) {
                cloudSuccess++;
                if (result.isAgreement()) {
                    agreement++;
                } else {
                    disagreement++;
                }
            } else {
                cloudFailure++;
                lastFailureReason = result.getReason();
            }
            if (result.isExecuted()) {
                executed++;
            }
            if (fallbackLocal(result)) {
                fallbackLocal++;
            }
            elapsedMs.addLast(Math.max(0L, result.getElapsedMs()));
            while (elapsedMs.size() > maxElapsedSamples) {
                elapsedMs.removeFirst();
            }
        }

        private MetricsSnapshot snapshot(MetricsKey key, boolean logEmitted) {
            return new MetricsSnapshot(
                    key.serviceId,
                    key.mode,
                    key.taskCode,
                    key.phase,
                    total,
                    cloudSuccess,
                    cloudFailure,
                    agreement,
                    disagreement,
                    executed,
                    fallbackLocal,
                    total == 0L ? 0.0d : (double) cloudSuccess / (double) total,
                    cloudSuccess == 0L ? 0.0d : (double) agreement / (double) cloudSuccess,
                    percentile(elapsedMs, 50),
                    percentile(elapsedMs, 95),
                    percentile(elapsedMs, 99),
                    elapsedMs.size(),
                    valueOrUnknown(lastFailureReason),
                    logEmitted);
        }
    }

    private record MetricsKey(String serviceId, String mode, String taskCode, String phase) {
        private static MetricsKey from(CloudDecisionResult result) {
            CloudDecisionRequest request = result.getRequest();
            return new MetricsKey(
                    request == null ? UNKNOWN : enumValue(request.getServiceId()),
                    enumValue(result.getMode()),
                    request == null ? UNKNOWN : valueOrUnknown(request.getTaskCode()),
                    request == null ? UNKNOWN : valueOrUnknown(request.getPhase()));
        }
    }

    public static final class MetricsSnapshot {
        private final String serviceId;
        private final String mode;
        private final String taskCode;
        private final String phase;
        private final long total;
        private final long cloudSuccess;
        private final long cloudFailure;
        private final long agreement;
        private final long disagreement;
        private final long executed;
        private final long fallbackLocal;
        private final double successRate;
        private final double agreementRate;
        private final long p50Ms;
        private final long p95Ms;
        private final long p99Ms;
        private final int elapsedSampleCount;
        private final String lastFailureReason;
        private final boolean logEmitted;

        private MetricsSnapshot(
                String serviceId,
                String mode,
                String taskCode,
                String phase,
                long total,
                long cloudSuccess,
                long cloudFailure,
                long agreement,
                long disagreement,
                long executed,
                long fallbackLocal,
                double successRate,
                double agreementRate,
                long p50Ms,
                long p95Ms,
                long p99Ms,
                int elapsedSampleCount,
                String lastFailureReason,
                boolean logEmitted) {
            this.serviceId = serviceId;
            this.mode = mode;
            this.taskCode = taskCode;
            this.phase = phase;
            this.total = total;
            this.cloudSuccess = cloudSuccess;
            this.cloudFailure = cloudFailure;
            this.agreement = agreement;
            this.disagreement = disagreement;
            this.executed = executed;
            this.fallbackLocal = fallbackLocal;
            this.successRate = successRate;
            this.agreementRate = agreementRate;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.p99Ms = p99Ms;
            this.elapsedSampleCount = elapsedSampleCount;
            this.lastFailureReason = lastFailureReason;
            this.logEmitted = logEmitted;
        }

        public String toLogLine() {
            return String.format(Locale.ROOT,
                    "cloud.metrics serviceId=%s mode=%s taskCode=%s phase=%s total=%d successRate=%.3f agreeRate=%.3f success=%d failure=%d agree=%d disagree=%d executed=%d fallback=%d p50Ms=%d p95Ms=%d p99Ms=%d lastReason=%s",
                    serviceId,
                    mode,
                    taskCode,
                    phase,
                    total,
                    successRate,
                    agreementRate,
                    cloudSuccess,
                    cloudFailure,
                    agreement,
                    disagreement,
                    executed,
                    fallbackLocal,
                    p50Ms,
                    p95Ms,
                    p99Ms,
                    UNKNOWN.equals(lastFailureReason) ? "-" : lastFailureReason);
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getMode() {
            return mode;
        }

        public String getTaskCode() {
            return taskCode;
        }

        public String getPhase() {
            return phase;
        }

        public long getTotal() {
            return total;
        }

        public long getCloudSuccess() {
            return cloudSuccess;
        }

        public long getCloudFailure() {
            return cloudFailure;
        }

        public long getAgreement() {
            return agreement;
        }

        public long getDisagreement() {
            return disagreement;
        }

        public long getExecuted() {
            return executed;
        }

        public long getFallbackLocal() {
            return fallbackLocal;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getAgreementRate() {
            return agreementRate;
        }

        public long getP50Ms() {
            return p50Ms;
        }

        public long getP95Ms() {
            return p95Ms;
        }

        public long getP99Ms() {
            return p99Ms;
        }

        public int getElapsedSampleCount() {
            return elapsedSampleCount;
        }

        public String getLastFailureReason() {
            return lastFailureReason;
        }

        public boolean isLogEmitted() {
            return logEmitted;
        }
    }
}
