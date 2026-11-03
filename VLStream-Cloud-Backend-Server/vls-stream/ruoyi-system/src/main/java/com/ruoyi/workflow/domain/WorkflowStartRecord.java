package com.ruoyi.workflow.domain;

import lombok.Data;

@Data
public class WorkflowStartRecord {
    private String requestHash;
    private String procInsId;
    private String workOrderId;
}
