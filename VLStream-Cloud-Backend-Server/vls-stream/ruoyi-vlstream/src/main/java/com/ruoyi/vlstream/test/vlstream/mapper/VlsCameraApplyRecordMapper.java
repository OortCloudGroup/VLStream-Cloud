/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.CameraApplyQueryDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraApplyRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraApplyRecordVO;

import java.util.List;

/**
 * approvalrecord Mapper interface
 */
public interface VlsCameraApplyRecordMapper extends BaseMapper<CameraApplyRecord> {

	List<CameraApplyRecordVO> selectCameraApplyRecordPage(IPage<CameraApplyRecordVO> page, @Param("query") CameraApplyQueryDTO query);
}
