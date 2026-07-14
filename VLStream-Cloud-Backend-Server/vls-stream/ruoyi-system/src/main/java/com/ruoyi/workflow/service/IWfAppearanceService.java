/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.workflow.domain.vo.WfAppearanceAllCountVo;

public interface IWfAppearanceService {
    WfAppearanceAllCountVo getAllCount(String token);
}
