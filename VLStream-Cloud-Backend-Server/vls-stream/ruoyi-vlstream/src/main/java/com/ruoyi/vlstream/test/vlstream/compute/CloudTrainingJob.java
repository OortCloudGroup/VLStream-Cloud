package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;

@Data
public class CloudTrainingJob {
    private String id;
    private String tenantId;
    private Long trainingId;
    private Long nodeId;
    private Long containerRecordId;
    private Long datasetId;
    private Long versionId;
    private String annotationType;
    private String snapshotJson;
    private String baseModel;
    private String optionsJson;
    private String runDir;
    private String jobState;
    private boolean cancelRequested;
    private int progress;
    private int epochCurrent;
    private String message;
    private String logTail;
    public String modelKey() { return "cloud-training/" + nodeId + "/" + id + "/weights/best.pt"; }
}
