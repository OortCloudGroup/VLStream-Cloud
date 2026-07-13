/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.SceneGovernance;

import java.util.List;

/**
 * Service for the VLS scene governance frontend compatibility surface.
 */
public interface IVlsSceneGovernanceService {

    /**
     * Return frontend-compatible paged scene governance records.
     */
    BladePage<SceneGovernance> getSceneGovernanceList(Long current, Long size, String name, String startDate, String endDate);

    /**
     * Create or update a scene governance record.
     */
    SceneGovernance submitSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * Remove scene governance records by ID.
     */
    boolean removeSceneGovernance(List<Long> ids);
}
