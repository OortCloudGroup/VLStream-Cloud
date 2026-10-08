package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.*;

/** IDs stay strings in browser responses, including image and annotation issue lists. */
@Data
public class DatasetGenerationReport {
    private String status = "CHECKED";
    private String datasetId;
    private String reference;
    private String jobId;
    private String message;
    private int checkedImages;
    private boolean repartitioned;
    private List<Map<String, Object>> corrections = new ArrayList<>();
    private List<Map<String, Object>> errors = new ArrayList<>();
    @JsonIgnore private String originalJson;
    @JsonIgnore private DatasetSnapshot prepared;
}
