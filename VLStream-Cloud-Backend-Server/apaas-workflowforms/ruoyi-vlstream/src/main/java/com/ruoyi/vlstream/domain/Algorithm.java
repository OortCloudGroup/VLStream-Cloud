package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * VLS algorithm entity mapped to vls_algorithm.
 */
@Data
@TableName("vls_algorithm")
public class Algorithm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long repositoryId;

    private String name;

    private String category;

    private String description;

    private String imageUrl;

    private String ptModelFilePath;

    private String onnxModelFilePath;

    private String configParams;

    private String inputFormat;

    private String outputFormat;

    private Integer gpuRequired;

    private Integer isSystem;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private String createDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String repositoryName;

    @TableField(exist = false)
    private String type;

    @TableField(exist = false)
    private String deployStatus;

    @TableField(exist = false)
    private String version;
}
