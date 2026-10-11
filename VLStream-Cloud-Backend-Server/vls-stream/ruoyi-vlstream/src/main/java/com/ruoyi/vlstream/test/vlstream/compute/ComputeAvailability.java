package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.Date;

/** A probe is evidence of readiness, never evidence of provider restart inventory. */
public final class ComputeAvailability {
    private ComputeAvailability() { }
    public static final long FRESHNESS_MS = 5 * 60 * 1000L;
    public static boolean fresh(Date checked, long now) {
        return checked != null && checked.getTime() <= now && now - checked.getTime() <= FRESHNESS_MS;
    }
    public static boolean noCapacity(ComputeNode node, long now) {
        return "UNAVAILABLE".equals(node.getCapacityState()) && fresh(node.getCapacityCheckedAt(), now);
    }
    public static boolean ready(ComputeNode node, long now) {
        return node.isEnabled() && "READY".equals(node.getProbeState())
            && !Boolean.TRUE.equals(node.getProviderLocked())
            && fresh(node.getProbeTime(), now) && !noCapacity(node, now)
            && !"STOPPED".equals(node.getPowerState()) && !"STARTING".equals(node.getPowerState())
            && !"STOPPING".equals(node.getPowerState()) && !"FAILED".equals(node.getPowerState()) && !noGpu(node, now);
    }
    public static boolean noGpu(ComputeNode node, long now) {
        if (fresh(node.getPowerCheckedAt(), now) && node.getProviderGpuAmount() != null && node.getProviderGpuAmount() <= node.getGpuIndex()) return true;
        if (!fresh(node.getProbeTime(), now)) return false;
        try {
            com.fasterxml.jackson.databind.JsonNode result = new com.fasterxml.jackson.databind.ObjectMapper().readTree(node.getProbeJson() == null ? "{}" : node.getProbeJson());
            return result.path("contractVersion").asInt() == 1 && result.path("gpuAvailable").isBoolean() && !result.path("gpuAvailable").asBoolean();
        } catch (java.io.IOException e) { return false; }
    }
}
