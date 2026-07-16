/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import lombok.Data;

/** Input for submitting a camera-use application. */
@Data
public class CameraApplySubmitRequest {
    private Long deviceInfoId;
    private String applyReason;
    private String applyRemark;
    private String applyUserName;
}
