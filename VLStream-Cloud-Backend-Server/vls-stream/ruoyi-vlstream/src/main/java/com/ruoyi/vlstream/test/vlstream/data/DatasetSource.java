package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springblade.core.mp.base.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_dataset_source")
public class DatasetSource extends TenantEntity {
    private String name;
    private String sourceType;
    private String endpoint;
    private String region;
    private String bucketName;
    private String keyPrefix;
    @JsonIgnore @ToString.Exclude private String credentialsCipher;
    private String description;
    public boolean isCredentialsConfigured() { return credentialsCipher != null && !credentialsCipher.isEmpty(); }
}
