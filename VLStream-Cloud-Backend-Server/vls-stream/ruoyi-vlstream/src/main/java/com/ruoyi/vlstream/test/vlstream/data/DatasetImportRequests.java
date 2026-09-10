package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.*;

public final class DatasetImportRequests {
    private DatasetImportRequests() { }
    @Data public static class Upload {
        @NotNull private Long datasetId;
        @NotBlank @Size(max=200) private String filename;
        @Min(1) private long fileSize;
        @NotBlank @Pattern(regexp="[a-f0-9]{64}") private String sha256;
        @NotBlank @Size(max=128) private String sourceName = "local";
        @NotNull @Pattern(regexp="none|vls|yolo") private String annotationFormat = "none";
    }
    @Data public static class Source {
        @NotBlank @Size(max=100) private String name;
        @NotNull @Pattern(regexp="s3|public") private String sourceType = "s3";
        @NotBlank @Size(max=1000) private String endpoint;
        @Size(max=100) private String region = "us-east-1";
        @Size(max=255) private String bucketName;
        @Size(max=1000) private String keyPrefix = "";
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @ToString.Exclude @Size(max=500) private String accessKey;
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @ToString.Exclude @Size(max=1000) private String secretKey;
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @ToString.Exclude @Size(max=4000) private String sessionToken;
        private boolean anonymous;
        @Size(max=1000) private String description = "";
    }
    @Data public static class Remote {
        @NotNull private Long datasetId;
        @NotNull private Long sourceId;
        @Size(max=2000) private String path = "";
        private boolean directory;
        @NotNull @Pattern(regexp="none|vls|yolo") private String annotationFormat = "none";
    }
}
