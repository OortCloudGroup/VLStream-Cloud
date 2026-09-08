package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_dataset_version")
public class DatasetVersion extends TenantEntity {
    private Long annotationId;
    private Integer versionNumber;
    private String versionName;
    private String description;
    @JsonIgnore private String snapshotJson;
    private Integer sampleCount;
    private Integer trainCount;
    private Integer validationCount;
}
