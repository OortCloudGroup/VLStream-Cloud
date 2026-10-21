package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.ruoyi.vlstream.test.vlstream.compute.ComputePowerProvider;
import java.time.Instant;

/** Backend-only bridge to existing provider and training/power workers. */
public interface BillingLifecycleGateway {
    final class Observation {
        public final ComputePowerProvider.Snapshot snapshot;
        public final Instant observedAt;
        public Observation(ComputePowerProvider.Snapshot snapshot, Instant observedAt) { this.snapshot = snapshot; this.observedAt = observedAt; }
    }
    Observation observe(String usageId);
    void requestSafeStop(String usageId);
}
