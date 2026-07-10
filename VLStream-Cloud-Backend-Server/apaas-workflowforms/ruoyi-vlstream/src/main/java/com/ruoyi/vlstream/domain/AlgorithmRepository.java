package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * VLS algorithm repository entity mapped to vls_algorithm_repository.
 */
@Data
@TableName("vls_algorithm_repository")
public class AlgorithmRepository implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String name;

    private Integer algorithmCount;

    private String repositoryType;

    private String remark;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private String createDept;

    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    private Date updateTime;

    private Integer status;

    @TableLogic
    private Integer isDeleted;
}
