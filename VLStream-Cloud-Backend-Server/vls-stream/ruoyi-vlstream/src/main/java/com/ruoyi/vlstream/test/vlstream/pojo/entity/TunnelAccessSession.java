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
@TableName("vls_tunnel_access_session")
public class TunnelAccessSession {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String tenantId;
    private String sessionId;
    private Long endpointId;
    private String deviceId;
    private String userId;
    private String accessTokenHash;
    private Date expiresAt;
    private Date openedAt;
    private Date revokedAt;
    private Date createTime;
    private Date updateTime;
}
