/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import lombok.Data;

/**
 * @description：表单字段属性
 */
@Data
public class NodeFormProperty {
    private String id;
    private String name;
    private Boolean readonly = false;
    private Boolean hidden;
    private Boolean required;
}
