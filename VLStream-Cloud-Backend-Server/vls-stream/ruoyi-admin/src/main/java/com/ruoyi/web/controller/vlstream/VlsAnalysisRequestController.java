package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnalysisRequest;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.service.IVlsAnalysisRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/** Source-compatible analysis-request lifecycle over the real request table. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAnalysisRequest")
public class VlsAnalysisRequestController extends VlsControllerSupport {

    private final IVlsAnalysisRequestService analysisRequestService;
    private final VlsDeviceInfoMapper deviceInfoMapper;

    /** Submit an analysis application in processing state. */
    @PostMapping("/apply")
    public BladeResult<AnalysisRequest> apply(@RequestBody AnalysisRequest request) {
        try {
            return BladeResult.success(hydrate(analysisRequestService.apply(request)));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Cancel an application only while its persisted state is processing. */
    @GetMapping("/cancel")
    public BladeResult<AnalysisRequest> cancel(@RequestParam Long id) {
        try {
            return BladeResult.success(hydrate(analysisRequestService.cancel(id)));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Return one matching analysis request with resolved camera names. */
    @GetMapping("/detail")
    public BladeResult<AnalysisRequest> detail(AnalysisRequest filter) {
        return BladeResult.success(hydrate(analysisRequestService.getOne(query(filter).last("limit 1"))));
    }

    /** Return a real database page with resolved camera names. */
    @GetMapping("/list")
    public BladeResult<BladePage<AnalysisRequest>> list(AnalysisRequest filter,
                                                        @RequestParam(required = false) Long current,
                                                        @RequestParam(required = false) Long size) {
        Page<AnalysisRequest> result = analysisRequestService.page(
            new Page<AnalysisRequest>(current(current), size(size)),
            query(filter).orderByDesc(AnalysisRequest::getCreateTime).orderByDesc(AnalysisRequest::getId));
        for (AnalysisRequest request : result.getRecords()) {
            hydrate(request);
        }
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist a new analysis-request row without claiming that analysis completed. */
    @PostMapping("/save")
    public BladeResult<AnalysisRequest> save(@RequestBody AnalysisRequest request) {
        return persist(request, false);
    }

    /** Update an existing analysis-request row. */
    @PostMapping("/update")
    public BladeResult<AnalysisRequest> update(@RequestBody AnalysisRequest request) {
        try {
            requireId(request);
            return analysisRequestService.updateById(request)
                ? BladeResult.success(hydrate(analysisRequestService.getById(request.getId())))
                : BladeResult.<AnalysisRequest>fail("Analysis request update affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Insert or update an analysis-request row. */
    @PostMapping("/submit")
    public BladeResult<AnalysisRequest> submit(@RequestBody AnalysisRequest request) {
        return persist(request, true);
    }

    /** Logically delete analysis requests by primary key. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(analysisRequestService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual matching analysis-request rows. */
    @GetMapping("/export-vlsAnalysisRequest")
    public void exportVlsAnalysisRequest(AnalysisRequest filter, HttpServletResponse response) {
        List<AnalysisRequest> rows = analysisRequestService.list(query(filter));
        for (AnalysisRequest row : rows) {
            hydrate(row);
        }
        ExcelUtil.exportExcel(rows, "Analysis Requests", AnalysisRequest.class, response);
    }

    /** Validate and persist a request row. */
    private BladeResult<AnalysisRequest> persist(AnalysisRequest request, boolean upsert) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Analysis request is required");
            }
            boolean stored = upsert ? analysisRequestService.saveOrUpdate(request) : analysisRequestService.save(request);
            return stored ? BladeResult.success(hydrate(analysisRequestService.getById(request.getId())))
                : BladeResult.<AnalysisRequest>fail("Analysis request was not persisted");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Resolve camera names from either device primary keys or device codes. */
    private AnalysisRequest hydrate(AnalysisRequest request) {
        if (request == null || !StringUtils.hasText(request.getDeviceIds())) {
            return request;
        }
        List<String> names = new ArrayList<String>();
        for (String token : request.getDeviceIds().split("[,，]")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            String value = token.trim();
            DeviceInfo device = null;
            try {
                device = deviceInfoMapper.selectById(Long.valueOf(value));
            } catch (NumberFormatException ignored) {
                // Non-numeric source values are device codes.
            }
            if (device == null) {
                device = deviceInfoMapper.selectByDeviceId(value);
            }
            if (device != null && StringUtils.hasText(device.getDeviceName())) {
                names.add(device.getDeviceName());
            }
        }
        request.setCameraName(String.join(",", names));
        return request;
    }

    /** Build a safe typed request query. */
    private LambdaQueryWrapper<AnalysisRequest> query(AnalysisRequest filter) {
        LambdaQueryWrapper<AnalysisRequest> query = new LambdaQueryWrapper<AnalysisRequest>();
        if (filter == null) {
            return query;
        }
        if (filter.getId() != null) {
            query.eq(AnalysisRequest::getId, filter.getId());
        }
        if (StringUtils.hasText(filter.getAnalysisName())) {
            query.like(AnalysisRequest::getAnalysisName, filter.getAnalysisName().trim());
        }
        if (StringUtils.hasText(filter.getAnalysisType())) {
            query.eq(AnalysisRequest::getAnalysisType, filter.getAnalysisType().trim());
        }
        if (StringUtils.hasText(filter.getRequestStatus())) {
            query.eq(AnalysisRequest::getRequestStatus, filter.getRequestStatus().trim());
        }
        return query;
    }

    /** Require a primary key for the explicit update route. */
    private void requireId(AnalysisRequest request) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("Analysis request ID is required");
        }
    }
}
