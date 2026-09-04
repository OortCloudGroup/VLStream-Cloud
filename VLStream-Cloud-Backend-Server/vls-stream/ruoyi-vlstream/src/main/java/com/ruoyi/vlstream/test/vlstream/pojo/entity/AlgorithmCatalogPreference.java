package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_algorithm_catalog_preference")
public class AlgorithmCatalogPreference extends TenantEntity {
    private String settingKey;
    private String viewMode;
}
