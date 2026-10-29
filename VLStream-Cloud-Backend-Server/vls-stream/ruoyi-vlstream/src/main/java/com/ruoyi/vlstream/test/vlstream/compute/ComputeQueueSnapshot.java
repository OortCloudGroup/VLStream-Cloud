package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;

/** Public queue facts only; no job identifiers, owners, paths or credentials. */
@Data
public class ComputeQueueSnapshot {
    private String state = "UNKNOWN";
    private String scope = "REGISTERED_SHARED_FIFO";
    private String externalOccupancy = "UNKNOWN";
    private Integer waitingCount;
    private String activePhase;
    private String activeTaskName;
    private Long checkedAt;
    private Long estimatedWaitSeconds;
    private Integer estimationSampleCount;
    private String estimationBasis;
}
