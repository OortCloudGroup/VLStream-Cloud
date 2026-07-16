package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmOrchestration;
import com.ruoyi.vlstream.service.IVlsAlgorithmOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/** Source-compatible CRUD over persisted algorithm orchestrations. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAlgorithmOrchestration")
public class VlsAlgorithmOrchestrationController extends VlsControllerSupport {

    private final IVlsAlgorithmOrchestrationService orchestrationService;

    /** Return one matching orchestration definition. */
    @GetMapping("/detail")
    public BladeResult<AlgorithmOrchestration> detail(AlgorithmOrchestration filter) {
        return BladeResult.success(orchestrationService.getOne(query(filter).last("limit 1")));
    }

    /** Return the source list route as a real database page. */
    @GetMapping("/list")
    public BladeResult<BladePage<AlgorithmOrchestration>> list(AlgorithmOrchestration filter,
                                                               @RequestParam(required = false) Long current,
                                                               @RequestParam(required = false) Long size) {
        return pageResult(filter, current, size);
    }

    /** Return the source custom-page route as a real database page. */
    @GetMapping("/page")
    public BladeResult<BladePage<AlgorithmOrchestration>> page(AlgorithmOrchestration filter,
                                                               @RequestParam(required = false) Long current,
                                                               @RequestParam(required = false) Long size) {
        return pageResult(filter, current, size);
    }

    /** Persist a new orchestration definition. */
    @PostMapping("/save")
    public BladeResult<AlgorithmOrchestration> save(@RequestBody AlgorithmOrchestration orchestration) {
        return persist(orchestration, false);
    }

    /** Update an existing orchestration definition. */
    @PostMapping("/update")
    public BladeResult<AlgorithmOrchestration> update(@RequestBody AlgorithmOrchestration orchestration) {
        try {
            requireId(orchestration);
            return orchestrationService.updateById(orchestration)
                ? BladeResult.success(orchestrationService.getById(orchestration.getId()))
                : BladeResult.<AlgorithmOrchestration>fail("Orchestration update affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Insert or update an orchestration definition. */
    @PostMapping("/submit")
    public BladeResult<AlgorithmOrchestration> submit(@RequestBody AlgorithmOrchestration orchestration) {
        return persist(orchestration, true);
    }

    /** Logically delete orchestration definitions by ID. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(orchestrationService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual matching orchestration rows. */
    @GetMapping("/export-vlsAlgorithmOrchestration")
    public void exportVlsAlgorithmOrchestration(AlgorithmOrchestration filter, HttpServletResponse response) {
        ExcelUtil.exportExcel(orchestrationService.list(query(filter)), "Algorithm Orchestrations",
            AlgorithmOrchestration.class, response);
    }

    /** Execute the shared typed page query. */
    private BladeResult<BladePage<AlgorithmOrchestration>> pageResult(AlgorithmOrchestration filter,
                                                                      Long current, Long size) {
        Page<AlgorithmOrchestration> result = orchestrationService.page(
            new Page<AlgorithmOrchestration>(current(current), size(size)),
            query(filter).orderByDesc(AlgorithmOrchestration::getUpdateTime)
                .orderByDesc(AlgorithmOrchestration::getId));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Validate and persist an orchestration row. */
    private BladeResult<AlgorithmOrchestration> persist(AlgorithmOrchestration orchestration, boolean upsert) {
        try {
            if (orchestration == null || !StringUtils.hasText(orchestration.getOrchestrationName())
                || !StringUtils.hasText(orchestration.getAlgorithmSteps())) {
                throw new IllegalArgumentException("Orchestration name and algorithm steps are required");
            }
            boolean stored = upsert ? orchestrationService.saveOrUpdate(orchestration) : orchestrationService.save(orchestration);
            return stored ? BladeResult.success(orchestrationService.getById(orchestration.getId()))
                : BladeResult.<AlgorithmOrchestration>fail("Orchestration was not persisted");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Build a safe typed query from source-compatible request fields. */
    private LambdaQueryWrapper<AlgorithmOrchestration> query(AlgorithmOrchestration filter) {
        LambdaQueryWrapper<AlgorithmOrchestration> query = new LambdaQueryWrapper<AlgorithmOrchestration>();
        if (filter == null) {
            return query;
        }
        if (filter.getId() != null) {
            query.eq(AlgorithmOrchestration::getId, filter.getId());
        }
        if (StringUtils.hasText(filter.getOrchestrationName())) {
            query.like(AlgorithmOrchestration::getOrchestrationName, filter.getOrchestrationName().trim());
        }
        if (StringUtils.hasText(filter.getTriggerType())) {
            query.eq(AlgorithmOrchestration::getTriggerType, filter.getTriggerType().trim());
        }
        if (StringUtils.hasText(filter.getOrchestrationStatus())) {
            query.eq(AlgorithmOrchestration::getOrchestrationStatus, filter.getOrchestrationStatus().trim());
        }
        return query;
    }

    /** Require a primary key for the update route. */
    private void requireId(AlgorithmOrchestration orchestration) {
        if (orchestration == null || orchestration.getId() == null) {
            throw new IllegalArgumentException("Orchestration ID is required");
        }
    }
}
