package com.ruoyi.vlstream.test.vlstream.data;

import lombok.Data;

/** A tenant-owned, immutable training package. Object locations are server-side metadata. */
@Data
public class TrainingDatasetArtifact {
    private String id;
    private String tenantId;
    private Long datasetId;
    private Long versionId;
    private String annotationType;
    private String state;
    private String snapshotSha256;
    private String datasetYaml;
    private String storageConfig;
    private String storageBucket;
    private String objectKey;
    private Long fileSize;
    private String sha256;

    public String getReference() { return TrainingDatasetArtifactService.reference(id); }
}
