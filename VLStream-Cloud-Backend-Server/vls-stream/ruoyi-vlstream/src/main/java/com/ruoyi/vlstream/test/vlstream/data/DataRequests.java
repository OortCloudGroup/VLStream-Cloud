package com.ruoyi.vlstream.test.vlstream.data;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

/** The writable API contract deliberately excludes ownership, object keys and audit fields. */
public final class DataRequests {
    private DataRequests() { }

    @Data
    public static class Project {
        @NotBlank @Size(max = 100) private String annotationName;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") private String projectCode;
        @NotBlank @Size(max = 64) private String projectType = "general";
        @NotBlank private String annotationType = "object_detection";
        @Size(max = 1000) private String remark = "";
        @Size(max = 4000) private String annotationRules = "";
    }

    @Data
    public static class SampleEdit {
        @NotBlank @Size(max = 200) private String imageName;
        @NotBlank @Size(max = 128) private String sampleSource;
        @NotNull @Size(max = 30) private List<@NotBlank @Size(max = 64) String> tags = new ArrayList<>();
    }

    @Data
    public static class Batch {
        @NotEmpty @Size(max = 500) private List<@NotNull Long> ids;
        @NotBlank private String action;
        @Size(max = 1000) private String note = "";
    }

    @Data
    public static class SampleQuery {
        @Min(1) private int page = 1;
        @Min(1) @Max(100) private int size = 20;
        @Size(max = 200) private String keyword;
        @Size(max = 64) private String tag;
        @Size(max = 128) private String source;
        private String mediaType;
        private String qualityStatus;
        private String annotationStatus;
        private String datasetSplit;
    }

    @Data
    public static class Split {
        @NotBlank private String mode = "random";
        @Min(1) @Max(99) private int trainPercent = 80;
        private long seed = 42;
        @NotNull @Size(max = 10000) private List<@NotNull Long> validationIds = new ArrayList<>();
        @NotBlank @Size(max = 100) private String versionName = "数据集划分";
    }

    @Data
    public static class Version {
        @NotBlank @Size(max = 100) private String name;
        @Size(max = 1000) private String description = "";
    }
}
