package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_dataset_import_job")
public class DatasetImportJob extends TenantEntity {
    private Long datasetId;
    private Long sourceId;
    private String sourceName;
    private String importType;
    private String annotationFormat;
    private String filename;
    private Long fileSize;
    private String expectedSha256;
    @JsonIgnore private String storageConfig;
    @JsonIgnore private String storageBucket;
    @JsonIgnore private String objectKey;
    @JsonIgnore private String multipartId;
    @JsonIgnore private String remotePath;
    private Integer chunkSize;
    private String jobState;
    private Long transferredBytes;
    private Integer totalFiles;
    private Integer importedFiles;
    private Integer skippedFiles;
    private Integer failedFiles;
    private String resultJson;
    private String errorMessage;
    private String progressMessage;
    @JsonIgnore private String workerId;
    private Date heartbeatAt;
    private Date expiresAt;
}
