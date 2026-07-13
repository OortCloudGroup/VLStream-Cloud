/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * VLS device entity mapped to vls_device_info.
 */
@Data
@TableName("vls_device_info")
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String deviceName;

    private String deviceId;

    private String streamUrl;

    private String imagePath;

    private String deviceType;

    private String remark;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String heightPosition;

    private String address;

    private String region;

    private String creator;

    private String tag;

    private String algorithmId;

    private String pushUrl;

    private Integer isPublic;

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
