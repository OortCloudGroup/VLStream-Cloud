/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Audio alarm-linkage settings persisted per device. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_audio_linkage_mode_setting")
public class AudioLinkageModeSetting extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;
    private Integer conventionalLinkageEnabled;
    private Integer emailLinkageEnabled;
    private Integer uploadCenterLinkageEnabled;
    private Integer alarmOutputLinkageEnabled;
    private String alarmOutputChannel;
    private Integer recordLinkageEnabled;
    private String recordChannel;
    private String remark;
}
