package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.ResourceSpec;
import com.ruoyi.vlstream.service.IVlsResourceSpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Source-compatible CRUD routes over the real resource-specification table. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsResourceSpec")
public class VlsResourceSpecController extends VlsControllerSupport {

    private final IVlsResourceSpecService resourceSpecService;

    /** Return one matching resource specification. */
    @GetMapping("/detail")
    public BladeResult<ResourceSpec> detail(ResourceSpec filter) {
        return BladeResult.success(resourceSpecService.getOne(query(filter).last("limit 1")));
    }

    /** Return a paged resource-specification list. */
    @GetMapping("/list")
    public BladeResult<BladePage<ResourceSpec>> list(ResourceSpec filter,
                                                      @RequestParam(required = false) Long current,
                                                      @RequestParam(required = false) Long size) {
        Page<ResourceSpec> result = resourceSpecService.page(
            new Page<ResourceSpec>(current(current), size(size)),
            query(filter).orderByAsc(ResourceSpec::getSortOrder).orderByDesc(ResourceSpec::getId));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist a new resource specification. */
    @PostMapping("/save")
    public BladeResult<ResourceSpec> save(@RequestBody ResourceSpec resourceSpec) {
        return persist(resourceSpec, false);
    }

    /** Update an existing resource specification. */
    @PostMapping("/update")
    public BladeResult<ResourceSpec> update(@RequestBody ResourceSpec resourceSpec) {
        try {
            requireId(resourceSpec);
            return resourceSpecService.updateById(resourceSpec)
                ? BladeResult.success(resourceSpecService.getById(resourceSpec.getId()))
                : BladeResult.<ResourceSpec>fail("Resource specification update affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Insert or update a resource specification. */
    @PostMapping("/submit")
    public BladeResult<ResourceSpec> submit(@RequestBody ResourceSpec resourceSpec) {
        return persist(resourceSpec, true);
    }

    /** Logically delete resource specifications by primary key. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(resourceSpecService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Persist after enforcing the source table's required foreign key and name. */
    private BladeResult<ResourceSpec> persist(ResourceSpec resourceSpec, boolean upsert) {
        try {
            if (resourceSpec == null || resourceSpec.getResourceTypeId() == null
                || !StringUtils.hasText(resourceSpec.getSpecName())) {
                throw new IllegalArgumentException("Resource type ID and specification name are required");
            }
            boolean stored = upsert ? resourceSpecService.saveOrUpdate(resourceSpec) : resourceSpecService.save(resourceSpec);
            return stored ? BladeResult.success(resourceSpecService.getById(resourceSpec.getId()))
                : BladeResult.<ResourceSpec>fail("Resource specification was not persisted");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Build a safe typed query from the source-compatible request object. */
    private LambdaQueryWrapper<ResourceSpec> query(ResourceSpec filter) {
        LambdaQueryWrapper<ResourceSpec> query = new LambdaQueryWrapper<ResourceSpec>();
        if (filter == null) {
            return query;
        }
        if (filter.getId() != null) {
            query.eq(ResourceSpec::getId, filter.getId());
        }
        if (filter.getResourceTypeId() != null) {
            query.eq(ResourceSpec::getResourceTypeId, filter.getResourceTypeId());
        }
        if (StringUtils.hasText(filter.getSpecName())) {
            query.like(ResourceSpec::getSpecName, filter.getSpecName().trim());
        }
        if (filter.getIsActive() != null) {
            query.eq(ResourceSpec::getIsActive, filter.getIsActive());
        }
        return query;
    }

    /** Require an ID for the explicit update route. */
    private void requireId(ResourceSpec resourceSpec) {
        if (resourceSpec == null || resourceSpec.getId() == null) {
            throw new IllegalArgumentException("Resource specification ID is required");
        }
    }
}
