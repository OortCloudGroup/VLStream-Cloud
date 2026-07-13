/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.workflow.domain.vo.WfAppearanceAllCountVo;

public interface IWfAppearanceService {
    WfAppearanceAllCountVo getAllCount(String token);
}
