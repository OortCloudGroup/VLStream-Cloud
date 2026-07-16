package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.CameraApplyActionRequest;
import com.ruoyi.vlstream.domain.CameraApplyRecord;
import com.ruoyi.vlstream.domain.CameraApplySubmitRequest;
import com.ruoyi.vlstream.service.IVlsCameraApplyRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Camera-use application routes with real persisted state transitions. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsCameraApply")
public class VlsCameraApplyController {

    private final IVlsCameraApplyRecordService cameraApplyService;

    /** Submit a new pending camera-use application. */
    @PostMapping("/camera-apply/submit")
    public BladeResult<CameraApplyRecord> submitCameraApply(@RequestBody CameraApplySubmitRequest request) {
        try {
            return BladeResult.success(cameraApplyService.submit(request));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Approve a currently pending camera-use application. */
    @PostMapping("/camera-apply/approve")
    public BladeResult<CameraApplyRecord> approveCameraApply(@RequestBody CameraApplyActionRequest request) {
        try {
            return BladeResult.success(cameraApplyService.approve(request));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Reject a currently pending camera-use application. */
    @PostMapping("/camera-apply/reject")
    public BladeResult<CameraApplyRecord> rejectCameraApply(@RequestBody CameraApplyActionRequest request) {
        try {
            return BladeResult.success(cameraApplyService.reject(request));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Complete a previously approved camera-use application. */
    @PostMapping("/camera-apply/complete")
    public BladeResult<CameraApplyRecord> completeCameraApply(@RequestBody CameraApplyActionRequest request) {
        try {
            return BladeResult.success(cameraApplyService.complete(request));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Page persisted applications by device, state and applicant. */
    @GetMapping("/camera-apply/page")
    public BladeResult<BladePage<CameraApplyRecord>> pageCameraApply(
        @RequestParam(required = false) Long current,
        @RequestParam(required = false) Long size,
        @RequestParam(required = false) Long deviceInfoId,
        @RequestParam(required = false) String applyStatus,
        @RequestParam(required = false) String applyUserName) {
        return BladeResult.success(cameraApplyService.page(current, size, deviceInfoId, applyStatus, applyUserName));
    }
}
