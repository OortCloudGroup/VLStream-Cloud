/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.bo;

import lombok.Data;

import java.util.List;
@Data
public class WfUserInterfaceFieldBo {
    private String userId;
    private String interfacePath;
    private List<String> fieldCodes;
}
