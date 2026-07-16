package com.ruoyi.vlstream.service.impl;

import com.ruoyi.vlstream.domain.AnalysisRequest;
import com.ruoyi.vlstream.mapper.VlsAnalysisRequestMapper;
import com.ruoyi.vlstream.service.IVlsAnalysisRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Real database service for analysis-request lifecycle state. */
@Service
public class VlsAnalysisRequestServiceImpl
    extends AbstractVlsTenantCrudService<VlsAnalysisRequestMapper, AnalysisRequest>
    implements IVlsAnalysisRequestService {

    /** Reset runtime output fields and persist the request in processing state. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisRequest apply(AnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Analysis request is required");
        }
        request.setRequestStatus("processing");
        if (request.getProgress() == null) {
            request.setProgress(Integer.valueOf(0));
        }
        request.setResultPath(null);
        request.setErrorMessage(null);
        request.setStartTime(null);
        request.setCompleteTime(null);
        if (!saveOrUpdate(request)) {
            throw new IllegalStateException("Analysis request was not persisted");
        }
        return getById(request.getId());
    }

    /** Enforce the source status rule and persist cancellation. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisRequest cancel(Long id) {
        AnalysisRequest request = id == null ? null : getById(id);
        if (request == null) {
            throw new IllegalArgumentException("Application does not exist");
        }
        if (!"processing".equals(request.getRequestStatus())) {
            throw new IllegalStateException("Current status does not support cancellation");
        }
        request.setRequestStatus("cancel");
        if (!updateById(request)) {
            throw new IllegalStateException("Analysis request cancellation affected no rows");
        }
        return getById(id);
    }
}
