/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.TimeStrategy;
import com.ruoyi.vlstream.mapper.VlsTimeStrategyMapper;
import com.ruoyi.vlstream.service.IVlsTimeStrategyService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Time strategy routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsTimeStrategy")
public class VlsTimeStrategyController extends VlsControllerSupport {

    private final IVlsTimeStrategyService timeStrategyService;
    private final VlsTimeStrategyMapper timeStrategyMapper;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsTimeStrategyController(IVlsTimeStrategyService timeStrategyService) {
        this.timeStrategyService = timeStrategyService;
        this.timeStrategyMapper = null;
        this.mqttPublishService = null;
        this.mqttProperties = null;
    }

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
            TimeStrategy stored = timeStrategyService.saveTimeStrategy(timeStrategy);
            mqttPublishService.publishOrThrow(mqttProperties.getVlsTimeStrategyTopic(), stored);
            return BladeResult.success(stored);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /**
     * Delete the strategy for a device.
     */
    @DeleteMapping("/{deviceId}")
    public BladeResult<Boolean> deleteTimeStrategy(@PathVariable String deviceId) {
        return operationResult(timeStrategyService.deleteTimeStrategy(deviceId), "Time strategy was not deleted");
    }

    /** Return a persisted strategy through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<TimeStrategy> detail(@RequestParam(required = false) Long id,
                                            @RequestParam(required = false) String deviceId) {
        TimeStrategy strategy = id == null ? null : timeStrategyMapper.selectById(id);
        if (strategy == null && deviceId != null) {
            strategy = timeStrategyService.getTimeStrategy(deviceId);
        }
        return strategy == null ? BladeResult.<TimeStrategy>fail("Time strategy does not exist")
            : BladeResult.success(strategy);
    }

    /** Query real strategy rows through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<TimeStrategy>> list(@RequestParam(required = false) Long current,
                                                     @RequestParam(required = false) Long size,
                                                     @RequestParam(required = false) String deviceId,
                                                     @RequestParam(required = false) Integer status) {
        Page<TimeStrategy> page = new Page<TimeStrategy>(current(current), size(size));
        LambdaQueryWrapper<TimeStrategy> query = strategyQuery(deviceId, status).orderByDesc(TimeStrategy::getUpdateTime);
        Page<TimeStrategy> result = timeStrategyMapper.selectPage(page, query);
        for (TimeStrategy strategy : result.getRecords()) {
            strategy.fillFrontendAliases();
        }
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Return the same persisted page through the SpringBlade custom page route. */
    @GetMapping("/page")
    public BladeResult<BladePage<TimeStrategy>> page(@RequestParam(required = false) Long current,
                                                     @RequestParam(required = false) Long size,
                                                     @RequestParam(required = false) String deviceId,
                                                     @RequestParam(required = false) Integer status) {
        return list(current, size, deviceId, status);
    }

    /** Persist and publish a strategy through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<TimeStrategy> save(@RequestBody TimeStrategy strategy) {
        return saveTimeStrategy(strategy);
    }

    /** Persist and publish a strategy through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<TimeStrategy> update(@RequestBody TimeStrategy strategy) {
        if (strategy == null || strategy.getId() == null) {
            return BladeResult.fail("Time strategy ID is required");
        }
        TimeStrategy existing = timeStrategyMapper.selectById(strategy.getId());
        if (existing == null) {
            return BladeResult.fail("Time strategy does not exist");
        }
        if (strategy.getDeviceId() == null) {
            strategy.setDeviceId(existing.getDeviceId());
        }
        return saveTimeStrategy(strategy);
    }

    /** Insert or update and publish through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<TimeStrategy> submit(@RequestBody TimeStrategy strategy) {
        return strategy != null && strategy.getId() != null ? update(strategy) : saveTimeStrategy(strategy);
    }

    /** Logically delete actual strategy rows by primary key. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(timeStrategyMapper.deleteBatchIds(parsed) > 0);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export actual strategy rows. */
    @GetMapping("/export-vlsTimeStrategy")
    public void exportVlsTimeStrategy(@RequestParam(required = false) String deviceId,
                                      @RequestParam(required = false) Integer status,
                                      HttpServletResponse response) {
        List<TimeStrategy> rows = timeStrategyMapper.selectList(strategyQuery(deviceId, status));
        for (TimeStrategy strategy : rows) {
            strategy.fillFrontendAliases();
        }
        ExcelUtil.exportExcel(rows, "VLS Time Strategies", TimeStrategy.class, response);
    }

    /** Build the common strategy filter used by list and export. */
    private LambdaQueryWrapper<TimeStrategy> strategyQuery(String deviceId, Integer status) {
        LambdaQueryWrapper<TimeStrategy> query = new LambdaQueryWrapper<TimeStrategy>();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            query.eq(TimeStrategy::getDeviceId, deviceId.trim());
        }
        if (status != null) {
            query.eq(TimeStrategy::getStatus, status);
        }
        return query;
    }
}
