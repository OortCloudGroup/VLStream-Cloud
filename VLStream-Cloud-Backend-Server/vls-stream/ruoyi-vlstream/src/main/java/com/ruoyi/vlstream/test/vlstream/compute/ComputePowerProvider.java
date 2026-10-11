package com.ruoyi.vlstream.test.vlstream.compute;

/** Implementations must use a verified product-specific API. No provider is enabled by default. */
public interface ComputePowerProvider {
    boolean supports(ComputeNode node);
    /** operationId is the stable retry/idempotency key; acceptance is not running. */
    void submit(ComputeNode node, String action, String operationId);
    Snapshot status(ComputeNode node);

    final class Snapshot {
        public final String power;
        public final String capacity;
        public final String message;
        public Integer gpuAmount;
        public String stoppedMode;
        public String chargeType;
        public Boolean locked;
        public Snapshot(String power, String capacity, String message) {
            if (!java.util.Arrays.asList("UNKNOWN", "STARTING", "RUNNING", "STOPPING", "STOPPED", "FAILED").contains(power)
                || !java.util.Arrays.asList("UNKNOWN", "AVAILABLE_HINT", "UNAVAILABLE").contains(capacity))
                throw new IllegalArgumentException("Unknown normalized provider state");
            this.power = power; this.capacity = capacity; this.message = message;
        }
    }
}
