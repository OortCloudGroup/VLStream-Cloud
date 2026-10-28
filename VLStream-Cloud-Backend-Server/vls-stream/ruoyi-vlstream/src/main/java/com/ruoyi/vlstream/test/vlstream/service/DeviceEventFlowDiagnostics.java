package com.ruoyi.vlstream.test.vlstream.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Process-local counts only: never retains an identity, topic, payload or exception message. */
@Component
public class DeviceEventFlowDiagnostics {
    public enum Counter {
        MQTT_RECEIVED, MQTT_DEVICE_ENVELOPE, MQTT_DEVICE_STATE, MQTT_AI_FACE, MQTT_AI_STRUCT,
        MQTT_IGNORED_ENVELOPE, MQTT_TOPIC_REJECTED, MQTT_UNSUPPORTED, MQTT_DISPATCH_ERROR,
        EVENT_DUPLICATE, EVENT_REVIEW_QUEUED, EVENT_BUSINESS_COMMITTED,
        EVENT_REJECT_ENVELOPE, EVENT_REJECT_DEVICE, EVENT_REJECT_MEDIA, EVENT_REJECT_REVIEW, EVENT_REJECT_PERSIST
    }

    private static final class Count {
        private final AtomicLong total = new AtomicLong();
        private final AtomicLong latest = new AtomicLong();
    }
    private final String sinceUtc = Instant.now().toString();
    private final Map<Counter, Count> counts = new EnumMap<>(Counter.class);

    public DeviceEventFlowDiagnostics() {
        for (Counter counter : Counter.values()) counts.put(counter, new Count());
    }

    public void record(Counter counter) {
        Count count = counts.get(counter);
        count.total.incrementAndGet(); count.latest.set(System.currentTimeMillis());
    }

    public void afterCommit(Counter counter) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { record(counter); }
            });
        } else {
            record(counter);
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sinceUtc", sinceUtc);
        Map<String, Object> values = new LinkedHashMap<>();
        for (Counter counter : Counter.values()) {
            Count count = counts.get(counter);
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("count", count.total.get());
            long latest = count.latest.get();
            value.put("latestUtc", latest == 0 ? null : Instant.ofEpochMilli(latest).toString());
            values.put(counter.name(), value);
        }
        result.put("counters", values);
        return result;
    }
}
