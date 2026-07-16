/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.SceneGovernance;
import com.ruoyi.vlstream.mapper.VlsSceneGovernanceMapper;
import com.ruoyi.vlstream.service.IVlsSceneGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scene governance routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsSceneGovernance")
public class VlsSceneGovernanceController extends VlsControllerSupport {

    private final IVlsSceneGovernanceService sceneGovernanceService;
    private final VlsSceneGovernanceMapper sceneGovernanceMapper;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsSceneGovernanceController(IVlsSceneGovernanceService sceneGovernanceService) {
        this.sceneGovernanceService = sceneGovernanceService;
        this.sceneGovernanceMapper = null;
    }

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

    /** Return one real scene-governance row by primary key. */
    @GetMapping("/detail")
    public BladeResult<SceneGovernance> detail(@RequestParam Long id) {
        SceneGovernance scene = sceneGovernanceMapper.selectById(id);
        return scene == null ? BladeResult.<SceneGovernance>fail("Scene governance record does not exist")
            : BladeResult.success(scene);
    }

    /** Return the existing real page through the source page route. */
    @GetMapping("/page")
    public BladeResult<BladePage<SceneGovernance>> page(@RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size,
                                                        @RequestParam(required = false) String name,
                                                        @RequestParam(required = false) String startDate,
                                                        @RequestParam(required = false) String endDate) {
        return getSceneGovernanceList(current, size, name, startDate, endDate);
    }

    /** Create a scene-governance row through the source save route. */
    @PostMapping("/save")
    public BladeResult<SceneGovernance> save(@RequestBody SceneGovernance scene) {
        if (scene != null) {
            scene.setId(null);
        }
        return submitSceneGovernance(scene);
    }

    /** Update a scene-governance row through the source update route. */
    @PostMapping("/update")
    public BladeResult<SceneGovernance> update(@RequestBody SceneGovernance scene) {
        if (scene == null || scene.getId() == null) {
            return BladeResult.fail("Scene governance ID is required");
        }
        return submitSceneGovernance(scene);
    }

    /** Delete scene rows through the source GET remove route. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        return BladeResult.success(sceneGovernanceService.removeSceneGovernance(parseIds(ids)));
    }

    /** Export actual scene-governance rows. */
    @GetMapping("/export-vlsSceneGovernance")
    public void exportVlsSceneGovernance(@RequestParam(required = false) String name,
                                         HttpServletResponse response) {
        LambdaQueryWrapper<SceneGovernance> query = new LambdaQueryWrapper<SceneGovernance>();
        if (name != null && !name.trim().isEmpty()) {
            query.like(SceneGovernance::getName, name.trim());
        }
        ExcelUtil.exportExcel(sceneGovernanceMapper.selectList(query.orderByDesc(SceneGovernance::getCreateTime)),
            "Scene Governance", SceneGovernance.class, response);
    }
}
