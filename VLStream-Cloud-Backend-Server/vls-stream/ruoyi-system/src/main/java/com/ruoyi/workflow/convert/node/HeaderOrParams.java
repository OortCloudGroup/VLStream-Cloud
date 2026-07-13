/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
