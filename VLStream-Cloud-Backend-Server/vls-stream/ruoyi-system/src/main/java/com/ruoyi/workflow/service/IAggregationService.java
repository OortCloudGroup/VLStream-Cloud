/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import java.util.Map;

public interface IAggregationService {
    Map<String, Object> getFormAndAppId(String applicationId,String formType);
}
