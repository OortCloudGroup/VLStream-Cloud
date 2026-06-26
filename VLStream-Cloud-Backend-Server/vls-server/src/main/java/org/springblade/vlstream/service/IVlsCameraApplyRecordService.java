package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.pojo.dto.*;
import org.springblade.vlstream.pojo.entity.CameraApplyRecord;
import org.springblade.vlstream.pojo.vo.CameraApplyRecordVO;

/**
 * Camera application approval record service class
 */
public interface IVlsCameraApplyRecordService extends BaseService<CameraApplyRecord> {

	Boolean submit(CameraApplySubmitDTO cameraApplySubmitDTO);

	Boolean approve(CameraApplyApproveDTO cameraApplyApproveDTO);

	Boolean reject(CameraApplyRejectDTO cameraApplyRejectDTO);

	Boolean complete(CameraApplyCompleteDTO cameraApplyCompleteDTO);

	IPage<CameraApplyRecordVO> page(IPage<CameraApplyRecordVO> page, CameraApplyQueryDTO cameraApplyQueryDTO);
}
