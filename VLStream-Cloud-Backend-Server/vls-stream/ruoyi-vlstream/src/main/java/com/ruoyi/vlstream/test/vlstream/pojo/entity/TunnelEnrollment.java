/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("vls_tunnel_enrollment")
public class TunnelEnrollment {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String tenantId;
    private Long endpointId;
    private String deviceId;
    private String codeHash;
    private Date expiresAt;
    private Date consumedAt;
    private String consumedAgentInstanceId;
    private String createUser;
    private Date createTime;
    private Date updateTime;
    private Integer isDeleted;
}
