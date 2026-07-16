package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.MobileSceneGovernance;
import com.ruoyi.vlstream.service.IVlsMobileSceneGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mobile scene-governance routes with real cyclic child-task generation. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsMobileSceneGovernance")
public class VlsMobileSceneGovernanceController {

    private final IVlsMobileSceneGovernanceService governanceService;

    /** List persisted immediate-governance tasks. */
    @GetMapping("/immediate/list")
    public BladeResult<BladePage<MobileSceneGovernance>> listImmediate(
        @RequestParam(required = false) Long current,
        @RequestParam(required = false) Long size) {
        return BladeResult.success(governanceService.listImmediate(current, size));
    }

    /** List persisted loop tasks together with their generated child tasks. */
    @GetMapping("/loop/list")
    public BladeResult<BladePage<MobileSceneGovernance>> listLoop(
        @RequestParam(required = false) Long current,
        @RequestParam(required = false) Long size) {
        return BladeResult.success(governanceService.listLoop(current, size));
    }

    /** Persist an immediate-governance task. */
    @PostMapping("/immediate/save")
    public BladeResult<MobileSceneGovernance> saveImmediate(@RequestBody MobileSceneGovernance governance) {
        try {
            return BladeResult.success(governanceService.saveImmediate(governance));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Persist a loop task and atomically generate its child execution rows. */
    @PostMapping("/loop/save")
    public BladeResult<MobileSceneGovernance> saveLoop(@RequestBody MobileSceneGovernance governance) {
        try {
            return BladeResult.success(governanceService.saveLoop(governance));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }
}
