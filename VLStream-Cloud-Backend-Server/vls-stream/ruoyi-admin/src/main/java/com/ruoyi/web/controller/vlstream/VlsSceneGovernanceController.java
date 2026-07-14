/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.SceneGovernance;
import com.ruoyi.vlstream.service.IVlsSceneGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scene governance routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsSceneGovernance")
public class VlsSceneGovernanceController {

    private final IVlsSceneGovernanceService sceneGovernanceService;

    /**
     * Return a page of scene governance records.
     */
    @GetMapping("/list")
    public BladeResult<BladePage<SceneGovernance>> getSceneGovernanceList(@RequestParam(required = false) Long current,
                                                                          @RequestParam(required = false) Long size,
                                                                          @RequestParam(required = false) String name,
                                                                          @RequestParam(required = false) String startDate,
                                                                          @RequestParam(required = false) String endDate) {
        return BladeResult.success(sceneGovernanceService.getSceneGovernanceList(current, size, name, startDate, endDate));
    }

    /**
     * Remove scene governance records by comma-separated IDs.
     */
    @PostMapping("/remove")
    public BladeResult<Boolean> removeSceneGovernance(@RequestParam String ids) {
        return BladeResult.success(sceneGovernanceService.removeSceneGovernance(toLongList(ids)));
    }

    /**
     * Create or update a scene governance record.
     */
    @PostMapping("/submit")
    public BladeResult<SceneGovernance> submitSceneGovernance(@RequestBody SceneGovernance sceneGovernance) {
        try {
            return BladeResult.success(sceneGovernanceService.submitSceneGovernance(sceneGovernance));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    private List<Long> toLongList(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        String[] parts = ids.split(",");
        for (String part : parts) {
            try {
                result.add(Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed IDs from optional batch UI state.
            }
        }
        return result;
    }
}
