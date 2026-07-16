package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.DeviceTagRelation;
import com.ruoyi.vlstream.service.IVlsDeviceTagRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/** Source-compatible CRUD over persistent device/tag relations. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsDeviceTagRelation")
public class VlsDeviceTagRelationController extends VlsControllerSupport {

    private final IVlsDeviceTagRelationService relationService;

    /** Return one matching device/tag relation. */
    @GetMapping("/detail")
    public BladeResult<DeviceTagRelation> detail(DeviceTagRelation filter) {
        return BladeResult.success(relationService.getOne(query(filter).last("limit 1")));
    }

    /** Return a SpringBlade-compatible relation page. */
    @GetMapping("/list")
    public BladeResult<BladePage<DeviceTagRelation>> list(DeviceTagRelation filter,
                                                          @RequestParam(required = false) Long current,
                                                          @RequestParam(required = false) Long size) {
        return pageResult(filter, current, size);
    }

    /** Return the source custom-page route over the real relation table. */
    @GetMapping("/page")
    public BladeResult<BladePage<DeviceTagRelation>> page(DeviceTagRelation filter,
                                                          @RequestParam(required = false) Long current,
                                                          @RequestParam(required = false) Long size) {
        return pageResult(filter, current, size);
    }

    /** Persist a new device/tag relation. */
    @PostMapping("/save")
    public BladeResult<DeviceTagRelation> save(@RequestBody DeviceTagRelation relation) {
        return persist(relation, false);
    }

    /** Update an existing device/tag relation. */
    @PostMapping("/update")
    public BladeResult<DeviceTagRelation> update(@RequestBody DeviceTagRelation relation) {
        try {
            requireId(relation);
            return relationService.updateById(relation)
                ? BladeResult.success(relationService.getById(relation.getId()))
                : BladeResult.<DeviceTagRelation>fail("Device tag relation update affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Insert or update a device/tag relation. */
    @PostMapping("/submit")
    public BladeResult<DeviceTagRelation> submit(@RequestBody DeviceTagRelation relation) {
        return persist(relation, true);
    }

    /** Logically delete relation rows by primary key. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(relationService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual matching relation rows as an Excel workbook. */
    @GetMapping("/export-vlsDeviceTagRelation")
    public void exportVlsDeviceTagRelation(DeviceTagRelation filter, HttpServletResponse response) {
        List<DeviceTagRelation> rows = relationService.list(query(filter).orderByDesc(DeviceTagRelation::getCreateTime));
        ExcelUtil.exportExcel(rows, "Device Tag Relations", DeviceTagRelation.class, response);
    }

    /** Execute the shared real-database paging query. */
    private BladeResult<BladePage<DeviceTagRelation>> pageResult(DeviceTagRelation filter, Long current, Long size) {
        Page<DeviceTagRelation> result = relationService.page(
            new Page<DeviceTagRelation>(current(current), size(size)),
            query(filter).orderByDesc(DeviceTagRelation::getCreateTime).orderByDesc(DeviceTagRelation::getId));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist after validating both sides of the relation. */
    private BladeResult<DeviceTagRelation> persist(DeviceTagRelation relation, boolean upsert) {
        try {
            if (relation == null || relation.getDeviceId() == null || relation.getTagId() == null) {
                throw new IllegalArgumentException("Device ID and tag ID are required");
            }
            boolean stored = upsert ? relationService.saveOrUpdate(relation) : relationService.save(relation);
            return stored ? BladeResult.success(relationService.getById(relation.getId()))
                : BladeResult.<DeviceTagRelation>fail("Device tag relation was not persisted");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Build a safe typed query from the source-compatible relation filter. */
    private LambdaQueryWrapper<DeviceTagRelation> query(DeviceTagRelation filter) {
        LambdaQueryWrapper<DeviceTagRelation> query = new LambdaQueryWrapper<DeviceTagRelation>();
        if (filter == null) {
            return query;
        }
        if (filter.getId() != null) {
            query.eq(DeviceTagRelation::getId, filter.getId());
        }
        if (filter.getDeviceId() != null) {
            query.eq(DeviceTagRelation::getDeviceId, filter.getDeviceId());
        }
        if (filter.getTagId() != null) {
            query.eq(DeviceTagRelation::getTagId, filter.getTagId());
        }
        return query;
    }

    /** Require an ID for the explicit update route. */
    private void requireId(DeviceTagRelation relation) {
        if (relation == null || relation.getId() == null) {
            throw new IllegalArgumentException("Device tag relation ID is required");
        }
    }
}
