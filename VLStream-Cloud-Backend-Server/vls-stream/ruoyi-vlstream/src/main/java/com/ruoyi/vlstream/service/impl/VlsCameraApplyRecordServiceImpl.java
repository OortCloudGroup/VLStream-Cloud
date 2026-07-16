package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.CameraApplyActionRequest;
import com.ruoyi.vlstream.domain.CameraApplyRecord;
import com.ruoyi.vlstream.domain.CameraApplySubmitRequest;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsCameraApplyRecordMapper;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.service.IVlsCameraApplyRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Real camera-application workflow persisted through explicit state transitions. */
@Service
public class VlsCameraApplyRecordServiceImpl
    extends AbstractVlsTenantCrudService<VlsCameraApplyRecordMapper, CameraApplyRecord>
    implements IVlsCameraApplyRecordService {

    private final VlsDeviceInfoMapper deviceInfoMapper;

    /** Inject the relation's real persistence mappers. */
    public VlsCameraApplyRecordServiceImpl(VlsDeviceInfoMapper deviceInfoMapper) {
        this.deviceInfoMapper = deviceInfoMapper;
    }

    /** Submit a pending application only for an existing device. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CameraApplyRecord submit(CameraApplySubmitRequest request) {
        if (request == null || request.getDeviceInfoId() == null) {
            throw new IllegalArgumentException("Device primary key ID cannot be empty");
        }
        if (!StringUtils.hasText(request.getApplyReason()) || !StringUtils.hasText(request.getApplyUserName())) {
            throw new IllegalArgumentException("Application reason and applicant are required");
        }
        if (deviceInfoMapper.selectById(request.getDeviceInfoId()) == null) {
            throw new IllegalArgumentException("Device does not exist");
        }
        CameraApplyRecord record = new CameraApplyRecord();
        record.setDeviceInfoId(request.getDeviceInfoId());
        record.setApplyReason(request.getApplyReason());
        record.setApplyRemark(request.getApplyRemark());
        record.setApplyUserName(request.getApplyUserName());
        record.setApplyTime(new Date());
        record.setApplyStatus("pending");
        if (!save(record)) {
            throw new IllegalStateException("Camera application was not persisted");
        }
        return getById(record.getId());
    }

    /** Transition a pending application to approved. */
    @Override
    public CameraApplyRecord approve(CameraApplyActionRequest request) {
        requireText(request == null ? null : request.getApproveUserName(), "Approver is required");
        return transition(request, "pending", "approved", false);
    }

    /** Transition a pending application to rejected with a reason. */
    @Override
    public CameraApplyRecord reject(CameraApplyActionRequest request) {
        requireText(request == null ? null : request.getApproveUserName(), "Approver is required");
        requireText(request == null ? null : request.getApprovalComment(), "Rejection reason is required");
        return transition(request, "pending", "rejected", false);
    }

    /** Transition an approved application to completed. */
    @Override
    public CameraApplyRecord complete(CameraApplyActionRequest request) {
        requireText(request == null ? null : request.getCompleteUserName(), "Completed by is required");
        return transition(request, "approved", "completed", true);
    }

    /** Query and hydrate real device information for application rows. */
    @Override
    public BladePage<CameraApplyRecord> page(Long current, Long size, Long deviceInfoId,
                                             String applyStatus, String applyUserName) {
        Page<CameraApplyRecord> page = new Page<CameraApplyRecord>(normalize(current, 1L), normalize(size, 10L));
        LambdaQueryWrapper<CameraApplyRecord> query = new LambdaQueryWrapper<CameraApplyRecord>();
        if (deviceInfoId != null) {
            query.eq(CameraApplyRecord::getDeviceInfoId, deviceInfoId);
        }
        if (StringUtils.hasText(applyStatus)) {
            query.eq(CameraApplyRecord::getApplyStatus, applyStatus.trim());
        }
        if (StringUtils.hasText(applyUserName)) {
            query.like(CameraApplyRecord::getApplyUserName, applyUserName.trim());
        }
        query.orderByDesc(CameraApplyRecord::getApplyTime).orderByDesc(CameraApplyRecord::getId);
        Page<CameraApplyRecord> result = page(page, query);
        hydrateDevices(result.getRecords());
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    /** Persist a guarded state transition and its action metadata. */
    @Transactional(rollbackFor = Exception.class)
    protected CameraApplyRecord transition(CameraApplyActionRequest request, String expected,
                                            String target, boolean completion) {
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("Application record ID cannot be empty");
        }
        CameraApplyRecord record = getById(request.getId());
        if (record == null) {
            throw new IllegalArgumentException("Application record does not exist");
        }
        if (!expected.equals(record.getApplyStatus())) {
            throw new IllegalStateException("Current status does not allow this operation");
        }
        record.setApplyStatus(target);
        if (completion) {
            record.setCompleteUserName(request.getCompleteUserName());
            record.setCompleteRemark(request.getCompleteRemark());
            record.setCompleteTime(new Date());
        } else {
            record.setApproveUserName(request.getApproveUserName());
            record.setApprovalComment(request.getApprovalComment());
            record.setApproveTime(new Date());
        }
        if (!updateById(record)) {
            throw new IllegalStateException("Camera application transition affected no rows");
        }
        return getById(record.getId());
    }

    /** Resolve device display fields without fake placeholder values. */
    private void hydrateDevices(List<CameraApplyRecord> records) {
        Map<Long, DeviceInfo> devices = new LinkedHashMap<Long, DeviceInfo>();
        for (CameraApplyRecord record : records) {
            if (record.getDeviceInfoId() != null && !devices.containsKey(record.getDeviceInfoId())) {
                devices.put(record.getDeviceInfoId(), deviceInfoMapper.selectById(record.getDeviceInfoId()));
            }
        }
        for (CameraApplyRecord record : records) {
            DeviceInfo device = devices.get(record.getDeviceInfoId());
            if (device != null) {
                record.setDeviceName(device.getDeviceName());
                record.setDeviceCode(device.getDeviceId());
            }
        }
    }

    /** Require a nonblank action operator or reason. */
    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Normalize pagination values. */
    private long normalize(Long value, long fallback) {
        return value == null || value.longValue() < 1L ? fallback : value.longValue();
    }
}
