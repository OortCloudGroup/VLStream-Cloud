package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.CameraApplyActionRequest;
import com.ruoyi.vlstream.domain.CameraApplyRecord;
import com.ruoyi.vlstream.domain.CameraApplySubmitRequest;

/** Service contract for real camera-application state transitions. */
public interface IVlsCameraApplyRecordService {

    /** Submit a new pending application after checking the device. */
    CameraApplyRecord submit(CameraApplySubmitRequest request);

    /** Approve a pending application. */
    CameraApplyRecord approve(CameraApplyActionRequest request);

    /** Reject a pending application. */
    CameraApplyRecord reject(CameraApplyActionRequest request);

    /** Complete an approved application. */
    CameraApplyRecord complete(CameraApplyActionRequest request);

    /** Query applications with device details. */
    BladePage<CameraApplyRecord> page(Long current, Long size, Long deviceInfoId, String applyStatus, String applyUserName);
}
