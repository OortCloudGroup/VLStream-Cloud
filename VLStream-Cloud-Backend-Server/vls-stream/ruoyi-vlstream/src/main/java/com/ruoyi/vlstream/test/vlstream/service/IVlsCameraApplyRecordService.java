/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraApplyRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraApplyRecordVO;

/**
 * approvalrecord service
 */
public interface IVlsCameraApplyRecordService extends BaseService<CameraApplyRecord> {

	Boolean submit(CameraApplySubmitDTO cameraApplySubmitDTO);

	Boolean approve(CameraApplyApproveDTO cameraApplyApproveDTO);

	Boolean reject(CameraApplyRejectDTO cameraApplyRejectDTO);

	Boolean complete(CameraApplyCompleteDTO cameraApplyCompleteDTO);

	IPage<CameraApplyRecordVO> page(IPage<CameraApplyRecordVO> page, CameraApplyQueryDTO cameraApplyQueryDTO);
}
