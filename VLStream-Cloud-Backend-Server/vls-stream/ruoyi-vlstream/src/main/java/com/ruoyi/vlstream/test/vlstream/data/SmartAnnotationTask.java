package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_smart_annotation_task")
public class SmartAnnotationTask extends TenantEntity {
    // Old vision workers cannot claim tasks whose review policy they do not implement.
    public static final String LLM_QUEUED = "LLM_QUEUED";
    public static final String LLM_RUNNING = "LLM_RUNNING";
    static final String PILOT_REVIEW = "PILOT_REVIEW";

    @JsonSerialize(using = ToStringSerializer.class) private Long datasetId;
    @JsonSerialize(using = ToStringSerializer.class) private Long computeNodeId;
    private String taskName;
    private String mode;
    private String annotationType = "object_detection";
    private String sourceType;
    @JsonSerialize(using = ToStringSerializer.class) private Long sourceId;
    private String modelName;
    @JsonIgnore private String llmProviderFingerprint;
    private String llmPrompt;
    private Integer llmPromptVersion;
    private Integer llmPilotSize;
    private Integer llmReviewPercent;
    @JsonIgnore private String modelPath;
    private String taskState;
    private Integer roundNumber;
    private Double confidence;
    private Integer epochs;
    private Integer reviewSize;
    private String errorMessage;
    @JsonIgnore private Long bulkCursor;
    private Integer bulkTotal;
    private Integer bulkProcessed;
    private Integer bulkAccepted;
    private Integer bulkConflicts;
    private String bulkError;

    String llmQueueState() {
        return (llmPilotSize != null && llmPilotSize > 0) || (llmReviewPercent != null && llmReviewPercent > 0)
            ? LLM_QUEUED : "QUEUED";
    }

    String llmRunningState() {
        return LLM_QUEUED.equals(llmQueueState()) ? LLM_RUNNING : "RUNNING";
    }
}
