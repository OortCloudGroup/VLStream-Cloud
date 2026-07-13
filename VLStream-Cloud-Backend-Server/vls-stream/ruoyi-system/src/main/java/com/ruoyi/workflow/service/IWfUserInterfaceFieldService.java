/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

public interface IWfUserInterfaceFieldService {
    String getFieldCodes(String userId, String interfacePath);

    int saveFieldCodes(String userId, String interfacePath,String codes);
}
