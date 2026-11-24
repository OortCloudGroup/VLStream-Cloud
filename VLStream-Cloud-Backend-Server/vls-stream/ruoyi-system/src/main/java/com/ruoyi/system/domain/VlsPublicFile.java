package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("vls_public_file")
public class VlsPublicFile {

    @TableId(type = IdType.INPUT)
    private String fileId;

    private String tenantId;
    private String uploaderId;
    private String originalName;
    private long fileSize;
    private String storageConfigKey;
}
