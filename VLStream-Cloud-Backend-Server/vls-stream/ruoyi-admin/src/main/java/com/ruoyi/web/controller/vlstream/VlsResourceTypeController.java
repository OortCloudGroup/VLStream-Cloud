package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.ResourceType;
import com.ruoyi.vlstream.service.IVlsResourceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Source-compatible CRUD routes over the real resource-type table. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsResourceType")
public class VlsResourceTypeController extends VlsControllerSupport {

    private final IVlsResourceTypeService resourceTypeService;

    /** Return one matching resource type. */
    @GetMapping("/detail")
    public BladeResult<ResourceType> detail(ResourceType filter) {
        return BladeResult.success(resourceTypeService.getOne(query(filter).last("limit 1")));
    }

    /** Return a paged resource-type list using source query fields. */
    @GetMapping("/list")
    public BladeResult<BladePage<ResourceType>> list(ResourceType filter,
                                                      @RequestParam(required = false) Long current,
                                                      @RequestParam(required = false) Long size) {
        Page<ResourceType> result = resourceTypeService.page(
            new Page<ResourceType>(current(current), size(size)),
            query(filter).orderByAsc(ResourceType::getSortOrder).orderByDesc(ResourceType::getId));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist a new resource type. */
    @PostMapping("/save")
    public BladeResult<ResourceType> save(@RequestBody ResourceType resourceType) {
        return persist(resourceType, false);
    }

    /** Update an existing resource type. */
    @PostMapping("/update")
    public BladeResult<ResourceType> update(@RequestBody ResourceType resourceType) {
        try {
            requireId(resourceType);
            return resourceTypeService.updateById(resourceType)
                ? BladeResult.success(resourceTypeService.getById(resourceType.getId()))
                : BladeResult.<ResourceType>fail("Resource type update affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Insert or update a resource type. */
    @PostMapping("/submit")
    public BladeResult<ResourceType> submit(@RequestBody ResourceType resourceType) {
        return persist(resourceType, true);
    }

    /** Logically delete resource types by primary key. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(resourceTypeService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Persist after enforcing the source table's required business fields. */
    private BladeResult<ResourceType> persist(ResourceType resourceType, boolean upsert) {
        try {
            if (resourceType == null || !StringUtils.hasText(resourceType.getTypeName())) {
                throw new IllegalArgumentException("Resource type name is required");
            }
            boolean stored = upsert ? resourceTypeService.saveOrUpdate(resourceType) : resourceTypeService.save(resourceType);
            return stored ? BladeResult.success(resourceTypeService.getById(resourceType.getId()))
                : BladeResult.<ResourceType>fail("Resource type was not persisted");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Build a safe typed query from the source-compatible request object. */
    private LambdaQueryWrapper<ResourceType> query(ResourceType filter) {
        LambdaQueryWrapper<ResourceType> query = new LambdaQueryWrapper<ResourceType>();
        if (filter == null) {
            return query;
        }
        if (filter.getId() != null) {
            query.eq(ResourceType::getId, filter.getId());
        }
        if (StringUtils.hasText(filter.getTypeCode())) {
            query.eq(ResourceType::getTypeCode, filter.getTypeCode().trim());
        }
        if (StringUtils.hasText(filter.getTypeName())) {
            query.like(ResourceType::getTypeName, filter.getTypeName().trim());
        }
        if (filter.getIsActive() != null) {
            query.eq(ResourceType::getIsActive, filter.getIsActive());
        }
        return query;
    }

    /** Require an ID for the explicit update route. */
    private void requireId(ResourceType resourceType) {
        if (resourceType == null || resourceType.getId() == null) {
            throw new IllegalArgumentException("Resource type ID is required");
        }
    }
}
