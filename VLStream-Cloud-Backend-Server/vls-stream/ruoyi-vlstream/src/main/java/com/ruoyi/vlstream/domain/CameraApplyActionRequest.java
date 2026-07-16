/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import lombok.Data;

/** Input shared by approve, reject and complete camera-application actions. */
@Data
public class CameraApplyActionRequest {
    private Long id;
    private String approveUserName;
    private String approvalComment;
    private String completeUserName;
    private String completeRemark;
}
