/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.enums.CameraApplyStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsCameraApplyRecordMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraApplyRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraApplyRecordVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsCameraApplyRecordService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * 摄像头申请审批记录 服务实现类
 */
@Service
@RequiredArgsConstructor
public class VlsCameraApplyRecordServiceImpl extends BaseServiceImpl<VlsCameraApplyRecordMapper, CameraApplyRecord> implements IVlsCameraApplyRecordService {

	private final IVlsDeviceInfoService vlsDeviceInfoService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean submit(CameraApplySubmitDTO cameraApplySubmitDTO) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getById(cameraApplySubmitDTO.getDeviceInfoId());
		Assert.notNull(deviceInfo, "设备不存在");

		CameraApplyRecord cameraApplyRecord = new CameraApplyRecord();
		cameraApplyRecord.setDeviceInfoId(cameraApplySubmitDTO.getDeviceInfoId());
		cameraApplyRecord.setApplyReason(cameraApplySubmitDTO.getApplyReason());
		cameraApplyRecord.setApplyRemark(cameraApplySubmitDTO.getApplyRemark());
		cameraApplyRecord.setApplyUserName(cameraApplySubmitDTO.getApplyUserName());
		cameraApplyRecord.setApplyTime(new Date());
		cameraApplyRecord.setApplyStatus(CameraApplyStatusEnum.pending);
		return save(cameraApplyRecord);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean approve(CameraApplyApproveDTO cameraApplyApproveDTO) {
		CameraApplyRecord cameraApplyRecord = getById(cameraApplyApproveDTO.getId());
		Assert.notNull(cameraApplyRecord, "申请记录不存在");
		Assert.isTrue(CameraApplyStatusEnum.pending.equals(cameraApplyRecord.getApplyStatus()), "当前状态不允许审批通过");

		CameraApplyRecord updateCameraApplyRecord = new CameraApplyRecord();
		updateCameraApplyRecord.setId(cameraApplyRecord.getId());
		updateCameraApplyRecord.setApplyStatus(CameraApplyStatusEnum.approved);
		updateCameraApplyRecord.setApproveUserName(cameraApplyApproveDTO.getApproveUserName());
		updateCameraApplyRecord.setApprovalComment(cameraApplyApproveDTO.getApprovalComment());
		updateCameraApplyRecord.setApproveTime(new Date());
		return updateById(updateCameraApplyRecord);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean reject(CameraApplyRejectDTO cameraApplyRejectDTO) {
		CameraApplyRecord cameraApplyRecord = getById(cameraApplyRejectDTO.getId());
		Assert.notNull(cameraApplyRecord, "申请记录不存在");
		Assert.isTrue(CameraApplyStatusEnum.pending.equals(cameraApplyRecord.getApplyStatus()), "当前状态不允许驳回");

		CameraApplyRecord updateCameraApplyRecord = new CameraApplyRecord();
		updateCameraApplyRecord.setId(cameraApplyRecord.getId());
		updateCameraApplyRecord.setApplyStatus(CameraApplyStatusEnum.rejected);
		updateCameraApplyRecord.setApproveUserName(cameraApplyRejectDTO.getApproveUserName());
		updateCameraApplyRecord.setApprovalComment(cameraApplyRejectDTO.getApprovalComment());
		updateCameraApplyRecord.setApproveTime(new Date());
		return updateById(updateCameraApplyRecord);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean complete(CameraApplyCompleteDTO cameraApplyCompleteDTO) {
		CameraApplyRecord cameraApplyRecord = getById(cameraApplyCompleteDTO.getId());
		Assert.notNull(cameraApplyRecord, "申请记录不存在");
		Assert.isTrue(CameraApplyStatusEnum.approved.equals(cameraApplyRecord.getApplyStatus()), "仅审批通过记录允许完结");

		CameraApplyRecord updateCameraApplyRecord = new CameraApplyRecord();
		updateCameraApplyRecord.setId(cameraApplyRecord.getId());
		updateCameraApplyRecord.setApplyStatus(CameraApplyStatusEnum.completed);
		updateCameraApplyRecord.setCompleteUserName(cameraApplyCompleteDTO.getCompleteUserName());
		updateCameraApplyRecord.setCompleteRemark(cameraApplyCompleteDTO.getCompleteRemark());
		updateCameraApplyRecord.setCompleteTime(new Date());
		return updateById(updateCameraApplyRecord);
	}

	@Override
	public IPage<CameraApplyRecordVO> page(IPage<CameraApplyRecordVO> page, CameraApplyQueryDTO cameraApplyQueryDTO) {
		return page.setRecords(baseMapper.selectCameraApplyRecordPage(page, cameraApplyQueryDTO));
	}
}
