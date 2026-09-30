package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.*;

public final class ComputeRequests {
    private ComputeRequests() { }

    @Data
    public static class Node {
        @NotBlank @Size(max = 100) private String name;
        @NotBlank @Size(max = 253) private String host;
        @Min(1) @Max(65535) private int port = 22;
        @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]{0,63}") private String username = "root";
        @Size(max = 1000) @ToString.Exclude private String password;
        @NotBlank @Size(max = 500) private String pythonPath = "/root/autodl-tmp/vlstream/envs/vls-standard/bin/python";
        @NotBlank @Size(max = 500) private String workDir = "/root/autodl-tmp/vlstream";
        @Min(0) @Max(15) private int gpuIndex;
        private boolean enabled = true;
    }

    @Data
    public static class Start {
        @Pattern(regexp = "preset|algorithm") private String modelSource;
        @NotNull private Long nodeId;
        @NotNull private Long datasetId;
        private Integer epochs = 10;
        private Integer batchSize = 16;
        private Integer imgSize = 640;
        @Size(max = 1000) private String extraParams;
    }
}
