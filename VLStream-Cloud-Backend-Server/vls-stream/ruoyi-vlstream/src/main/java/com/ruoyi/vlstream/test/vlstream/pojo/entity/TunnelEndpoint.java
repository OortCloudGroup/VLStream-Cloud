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
@TableName("vls_tunnel_endpoint")
public class TunnelEndpoint {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String tenantId;
    private Long wvpDeviceRowId;
    private String deviceId;
    private String deviceName;
    private String agentInstanceId;
    private String agentTokenHash;
    private String agentVersion;
    private String architecture;
    private String firmwareVersion;
    private String initSystem;
    private String localWebScheme;
    private Integer localWebPort;
    private Integer serverBindPort;
    private String serviceName;
    private Long configGeneration;
    private String desiredState;
    private String agentStatus;
    private String tunnelStatus;
    private String localWebStatus;
    private String routeStatus;
    private Long routeAppliedGeneration;
    private Date lastHeartbeatAt;
    private Date lastReportedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Date revokedAt;
    private String createUser;
    private String createDept;
    private Date createTime;
    private String updateUser;
    private Date updateTime;
    private Integer status;
    private Integer isDeleted;
}
