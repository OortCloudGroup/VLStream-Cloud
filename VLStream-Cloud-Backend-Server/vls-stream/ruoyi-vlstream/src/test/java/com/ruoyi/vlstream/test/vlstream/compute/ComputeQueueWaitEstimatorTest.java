package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ComputeQueueWaitEstimatorTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ComputeQueueWaitEstimator estimator = new ComputeQueueWaitEstimator();
    private static final String POOL = "physical-gpu-a";
    private static final String PROFILE = repeat('a');
    private static String repeat(char value) { return new String(new char[64]).replace('\0', value); }
    private ObjectNode observation(long at, String key, String phase, String profile, String previous, boolean archived, String... waiting) {
        ObjectNode value = json.createObjectNode();
        value.put("checkedAt", at); value.put("activeTicketKey", key); value.put("activePhase", phase); value.put("activeProfile", profile);
        value.put("previousTicketKey", previous); value.put("previousOutcome", archived ? "ARCHIVED" : null);
        value.put("waitingCount", waiting.length); ArrayNode rows = value.putArray("waitingProfiles");
        for (String item : waiting) rows.add(item);
        return value;
    }
    private ComputeQueueWaitEstimator.Estimate observe(long at, String key, String phase, String profile, String previous, boolean archived, String... waiting) {
        return estimator.observe(POOL, observation(at, key, phase, profile, previous, archived, waiting));
    }
    private void completed(long start, char key, long duration, String profile, boolean archived) {
        String ticket = repeat(key);
        observe(start, null, null, null, null, false);
        observe(start + 10000, ticket, "RUNNING", profile, null, false);
        for (long offset = 30000; offset < duration; offset += 20000) observe(start + offset, ticket, offset < duration - 40000 ? "RUNNING" : "ARCHIVING", profile, null, false);
        observe(start + duration, null, null, null, ticket, archived);
    }
    private long history() {
        completed(0, '1', 150000, PROFILE, true);
        completed(160000, '2', 160000, PROFILE, true);
        completed(330000, '3', 170000, PROFILE, true);
        return 510000;
    }
    @Test void threeMatchedSuccessfulFullLeasesProduceP75IncludingArchiveAndWholeQueue() {
        long at = history(); String current = repeat('4');
        observe(at, null, null, null, null, false);
        ComputeQueueWaitEstimator.Estimate result = observe(at + 10000, current, "RUNNING", PROFILE, null, false, PROFILE, PROFILE);
        assertNotNull(result); assertEquals(510, result.getSeconds()); assertEquals(3, result.getSamples());
        assertEquals(current, estimator.previousKey(POOL));
        result = observe(at + 30000, current, "ARCHIVING", PROFILE, null, false, PROFILE, PROFILE);
        assertNotNull(result); assertEquals(490, result.getSeconds());
    }
    @Test void sparseOrDifferentWorkloadsAndUnpreparedPredecessorsRemainUnknown() {
        completed(0, '1', 150000, PROFILE, true);
        observe(160000, null, null, null, null, false);
        assertNull(observe(170000, repeat('4'), "RUNNING", PROFILE, null, false));
        ComputeQueueWaitEstimator other = new ComputeQueueWaitEstimator();
        assertNull(other.observe(POOL, observation(0, repeat('4'), "RUNNING", PROFILE, null, false)));
    }
    @Test void anyUnpreparedOrDifferentPredecessorMakesTotalWaitUnknownDespiteEnoughSamples() {
        long at = history(); observe(at, null, null, null, null, false);
        assertNull(observe(at + 10000, repeat('5'), "RUNNING", PROFILE, null, false, (String)null));
        assertNull(observe(at + 20000, repeat('5'), "RUNNING", PROFILE, null, false, repeat('b')));
    }
    @Test void firstSightingInMiddleOfTaskCannotInventItsElapsedTime() {
        long at = history();
        // A gap longer than 30s breaks observation continuity despite retained history.
        assertNull(observe(at + 100000, repeat('5'), "RUNNING", PROFILE, null, false));
    }
    @Test void explicitUnavailableAndPollingGapsInvalidateCurrentStartProof() {
        long at = history(); observe(at, null, null, null, null, false);
        assertNotNull(observe(at + 10000, repeat('5'), "RUNNING", PROFILE, null, false));
        estimator.unavailable(POOL);
        assertNull(observe(at + 20000, repeat('5'), "RUNNING", PROFILE, null, false));
    }
    @Test void failedOrCancelledReleaseIsNeverASuccessfulDurationSample() {
        completed(0, '1', 150000, PROFILE, false);
        completed(160000, '2', 160000, PROFILE, true);
        completed(330000, '3', 170000, PROFILE, true);
        observe(510000, null, null, null, null, false);
        assertNull(observe(520000, repeat('4'), "RUNNING", PROFILE, null, false));
    }
    @Test void overrunNeverBecomesZeroWaitAndOutOfOrderResponsesCannotRewindState() {
        long at = history(); observe(at, null, null, null, null, false); String key = repeat('5');
        assertNotNull(observe(at + 10000, key, "RUNNING", PROFILE, null, false));
        for (int offset = 30000; offset < 190000; offset += 20000) observe(at + offset, key, "ARCHIVING", PROFILE, null, false);
        assertNull(observe(at + 190000, key, "ARCHIVING", PROFILE, null, false));
        assertNull(observe(at + 150000, repeat('6'), "RUNNING", PROFILE, null, false));
        assertEquals(key, estimator.previousKey(POOL));
    }
    @Test void highVarianceSamplesAndWrongPhysicalGpuHaveNoEstimate() {
        completed(0, '1', 150000, PROFILE, true);
        completed(160000, '2', 160000, PROFILE, true);
        completed(330000, '3', 350000, PROFILE, true);
        observe(690000, null, null, null, null, false);
        assertNull(observe(700000, repeat('4'), "RUNNING", PROFILE, null, false));
        assertNull(estimator.observe("other-physical-gpu", observation(700000, repeat('4'), "RUNNING", PROFILE, null, false)));
    }
    @Test void dayOldSamplesDoNotBecomeCurrentEvidence() {
        long at = history() + 86400001;
        observe(at, null, null, null, null, false);
        assertNull(observe(at + 10000, repeat('4'), "RUNNING", PROFILE, null, false));
    }
}
