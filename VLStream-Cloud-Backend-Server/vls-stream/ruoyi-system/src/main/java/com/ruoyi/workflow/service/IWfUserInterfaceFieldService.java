/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

public interface IWfUserInterfaceFieldService {
    String getFieldCodes(String userId, String interfacePath);

    int saveFieldCodes(String userId, String interfacePath,String codes);
}
