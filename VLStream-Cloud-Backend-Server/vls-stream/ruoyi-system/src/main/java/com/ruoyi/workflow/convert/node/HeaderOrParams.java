/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import lombok.Data;

@Data
public class HeaderOrParams {
    private String key;
    private String keyType; // 1 表单， 2 固定
    private String value;
}
