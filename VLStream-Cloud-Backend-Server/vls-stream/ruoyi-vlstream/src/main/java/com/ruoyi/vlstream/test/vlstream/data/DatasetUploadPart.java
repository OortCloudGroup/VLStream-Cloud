package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class DatasetUploadPart {
    private Long jobId;
    private Integer partNumber;
    private Long partSize;
    private String sha256;
    @JsonIgnore private String etag;
}
