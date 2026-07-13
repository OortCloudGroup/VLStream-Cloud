/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import java.util.Map;

public interface IAggregationService {
    Map<String, Object> getFormAndAppId(String applicationId,String formType);
}
