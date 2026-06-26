package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.enums.CameraApplyStatusEnum;
import org.springblade.vlstream.mapper.VlsCameraApplyRecordMapper;
import org.springblade.vlstream.pojo.dto.*;
import org.springblade.vlstream.pojo.entity.CameraApplyRecord;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.vo.CameraApplyRecordVO;
import org.springblade.vlstream.service.IVlsCameraApplyRecordService;
import org.springblade.vlstream.service.IVlsDeviceInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * Camera application approval record service implementation class
 */
@Service
@RequiredArgsConstructor
public class VlsCameraApplyRecordServiceImpl extends BaseServiceImpl<VlsCameraApplyRecordMapper, CameraApplyRecord> implements IVlsCameraApplyRecordService {

	private final IVlsDeviceInfoService vlsDeviceInfoService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean submit(CameraApplySubmitDTO cameraApplySubmitDTO) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getById(cameraApplySubmitDTO.getDeviceInfoId());
		Assert.notNull(deviceInfo, "Device does not exist");

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
		Assert.notNull(cameraApplyRecord, "Application record does not exist");
		Assert.isTrue(CameraApplyStatusEnum.pending.equals(cameraApplyRecord.getApplyStatus()), "Current status does not allow approval");

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
		Assert.notNull(cameraApplyRecord, "Application record does not exist");
		Assert.isTrue(CameraApplyStatusEnum.pending.equals(cameraApplyRecord.getApplyStatus()), "Current status does not allow rejection");

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
		Assert.notNull(cameraApplyRecord, "Application record does not exist");
		Assert.isTrue(CameraApplyStatusEnum.approved.equals(cameraApplyRecord.getApplyStatus()), "Only approved records are allowed to be concluded");

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
