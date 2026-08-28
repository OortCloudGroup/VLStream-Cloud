/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.node;

import lombok.Data;

/**
 * @description: formfieldproperty
 */
@Data
public class NodeFormProperty {
    private String id;
    private String name;
    private Boolean readonly = false;
    private Boolean hidden;
    private Boolean required;
}
