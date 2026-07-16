/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** Camera-use application with explicit approval-state transitions. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_camera_apply_record")
public class CameraApplyRecord extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private Long deviceInfoId;
    private String applyReason;
    private String applyRemark;
    private String applyUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date applyTime;
    private String applyStatus;
    private String approvalComment;
    private String approveUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approveTime;
    private String completeRemark;
    private String completeUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completeTime;

    @TableField(exist = false)
    private String deviceName;

    @TableField(exist = false)
    private String deviceCode;
}
