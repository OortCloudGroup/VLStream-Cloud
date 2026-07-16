package com.ruoyi.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.vlstream.domain.AnalysisRequest;

/** Service contract for analysis-request persistence and cancellation. */
public interface IVlsAnalysisRequestService extends IService<AnalysisRequest> {

    /** Submit an analysis request in processing state. */
    AnalysisRequest apply(AnalysisRequest request);

    /** Cancel a request only while it is processing. */
    AnalysisRequest cancel(Long id);
}
