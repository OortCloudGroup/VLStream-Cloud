package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.TimeStrategy;
import com.ruoyi.vlstream.service.IVlsTimeStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Time strategy routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsTimeStrategy")
public class VlsTimeStrategyController {

    private final IVlsTimeStrategyService timeStrategyService;

    /**
     * Return the strategy for a device.
     */
    @GetMapping("/{deviceId}")
    public BladeResult<TimeStrategy> getTimeStrategy(@PathVariable String deviceId) {
        return BladeResult.success(timeStrategyService.getTimeStrategy(deviceId));
    }

    /**
     * Save or update the strategy for a device.
     */
    @PostMapping("")
    public BladeResult<TimeStrategy> saveTimeStrategy(@RequestBody TimeStrategy timeStrategy) {
        try {
            return BladeResult.success(timeStrategyService.saveTimeStrategy(timeStrategy));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Delete the strategy for a device.
     */
    @DeleteMapping("/{deviceId}")
    public BladeResult<Boolean> deleteTimeStrategy(@PathVariable String deviceId) {
        return BladeResult.success(timeStrategyService.deleteTimeStrategy(deviceId));
    }
}
