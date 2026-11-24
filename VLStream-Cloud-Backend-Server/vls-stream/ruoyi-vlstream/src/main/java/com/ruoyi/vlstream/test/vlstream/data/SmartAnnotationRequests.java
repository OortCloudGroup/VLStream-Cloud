package com.ruoyi.vlstream.test.vlstream.data;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

public final class SmartAnnotationRequests {
    private SmartAnnotationRequests() { }

    @Data
    public static class Create {
        @NotNull private Long datasetId;
        private Long computeNodeId;
        @NotBlank @Size(max = 100) private String taskName;
        @NotNull @Pattern(regexp = "active|model|llm") private String mode;
        @Pattern(regexp = "algorithm|model|round|system") private String sourceType;
        private Long sourceId;
        private Long providerId;
        @Size(max = 200) private String modelName;
        @Size(max = 2000) private String prompt;
        @Min(0) @Max(50) private int llmPilotSize;
        @Min(0) @Max(100) private int llmReviewPercent;
        @DecimalMin("0.05") @DecimalMax("0.95") private Double confidence;
        @Min(1) @Max(100) private int epochs = 20;
        @Min(1) @Max(500) private int reviewSize = 50;
    }

    @Data
    public static class PilotPrompt {
        @Size(max = 2000) private String prompt;
    }

    @Data
    public static class Box {
        @NotNull @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) private Long labelId;
        private String className;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        @Size(max = 11200000) private String maskData;
        private Double confidence;
    }

    @Data
    public static class Review {
        @NotNull @Size(max = 1000) @Valid private List<Box> boxes;
        private boolean skip;
    }

    @Data
    public static class ConfirmBatch {
        @NotEmpty @Size(max = 100) private List<@NotNull Long> ids;
    }
}
