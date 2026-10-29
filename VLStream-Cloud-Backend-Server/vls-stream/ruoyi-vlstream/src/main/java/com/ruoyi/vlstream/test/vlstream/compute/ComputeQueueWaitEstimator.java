package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Bounded in-memory observations only. No task, registry, database or timer writes. */
@Service
public class ComputeQueueWaitEstimator {
    private static final long MAX_GAP_MS = 30000;
    private static final int MIN_SAMPLES = 3;
    private static final long HISTORY_MAX_AGE_MS = 86400000;
    private final Map<String, Pool> pools = new LinkedHashMap<String, Pool>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Pool> entry) { return size() > 64; }
    };

    public static final class Estimate {
        private final long seconds;
        private final int samples;
        Estimate(long seconds, int samples) { this.seconds = seconds; this.samples = samples; }
        public long getSeconds() { return seconds; }
        public int getSamples() { return samples; }
    }

    private static final class Pool {
        long checkedAt = -1;
        String activeKey;
        String profile;
        Long startLower;
        Long startUpper;
        final Map<String, Deque<Sample>> durations = new LinkedHashMap<String, Deque<Sample>>(16, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, Deque<Sample>> entry) { return size() > 64; }
        };
    }

    private static final class Sample {
        final long duration;
        final long at;
        Sample(long duration, long at) { this.duration = duration; this.at = at; }
    }

    public synchronized String previousKey(String poolKey) {
        Pool pool = pools.get(poolKey);
        return pool == null ? null : pool.activeKey;
    }

    public synchronized void unavailable(String poolKey) {
        Pool pool = pools.get(poolKey);
        if (pool != null) { pool.checkedAt = -1; pool.startLower = null; pool.startUpper = null; }
    }

    public synchronized Estimate observe(String poolKey, JsonNode snapshot) {
        long at = snapshot.path("checkedAt").asLong(-1);
        String key = hash(snapshot.path("activeTicketKey"));
        String profile = hash(snapshot.path("activeProfile"));
        JsonNode waiting = snapshot.path("waitingProfiles");
        if (key == null && !snapshot.path("activePhase").isNull() || key != null && snapshot.path("activePhase").isNull()) {
            unavailable(poolKey); return null;
        }
        Pool pool = pools.computeIfAbsent(poolKey, ignored -> new Pool());
        // Concurrent reads can arrive out of order. They never replace a newer observation.
        if (at <= pool.checkedAt) return null;
        boolean continuous = pool.checkedAt >= 0 && at - pool.checkedAt <= MAX_GAP_MS;
        boolean changed = !java.util.Objects.equals(key, pool.activeKey);
        if (changed && continuous && pool.activeKey != null && pool.profile != null && pool.startLower != null
            && pool.activeKey.equals(snapshot.path("previousTicketKey").asText()) && "ARCHIVED".equals(snapshot.path("previousOutcome").asText())) {
            long duration = at - pool.startLower;
            if (duration >= 4 * MAX_GAP_MS && duration <= 7L * 86400000) {
                Deque<Sample> history = pool.durations.computeIfAbsent(pool.profile, ignored -> new ArrayDeque<>());
                history.addLast(new Sample(duration, at));
                if (history.size() > 20) history.removeFirst();
            }
        }
        if (!continuous || changed || !java.util.Objects.equals(profile, pool.profile)) {
            // First sighting midway through a task cannot establish its true start.
            pool.startLower = changed && continuous && key != null ? pool.checkedAt : null;
            pool.startUpper = changed && continuous && key != null ? at : null;
        }
        pool.checkedAt = at; pool.activeKey = key; pool.profile = profile;
        if (key == null || profile == null || pool.startUpper == null || !waiting.isArray()
            || waiting.size() != snapshot.path("waitingCount").asInt(-1) || waiting.size() > 100) return null;
        List<String> predecessors = new ArrayList<>(); predecessors.add(profile);
        for (JsonNode value : waiting) {
            String fingerprint = hash(value);
            if (fingerprint == null) return null; // Unknown or unprepared predecessor makes total wait unknown.
            predecessors.add(fingerprint);
        }
        long total = 0;
        int minimumSamples = Integer.MAX_VALUE;
        for (int index = 0; index < predecessors.size(); index++) {
            Deque<Sample> history = pool.durations.get(predecessors.get(index));
            if (history == null || history.size() < MIN_SAMPLES) return null;
            List<Long> sorted = new ArrayList<>();
            for (Sample sample : history) if (at - sample.at <= HISTORY_MAX_AGE_MS) sorted.add(sample.duration);
            if (sorted.size() < MIN_SAMPLES) return null;
            Collections.sort(sorted);
            // Highly variable workloads do not support a useful point estimate.
            if (sorted.get(sorted.size() - 1) > sorted.get(0) * 2) return null;
            long predicted = sorted.get((int)Math.ceil(sorted.size() * .75) - 1);
            if (index == 0) {
                predicted -= at - pool.startUpper;
                // An overrun/blocked archive is not zero remaining time.
                if (predicted <= 0) return null;
            }
            total = Math.addExact(total, predicted);
            minimumSamples = Math.min(minimumSamples, sorted.size());
        }
        return new Estimate((total + 999) / 1000, minimumSamples);
    }

    private static String hash(JsonNode value) {
        String text = value.asText("");
        return text.matches("[a-f0-9]{64}") ? text : null;
    }
}
