/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 摄像头申请审批记录 Mapper 接口
 */
public interface VlsCameraApplyRecordMapper extends BaseMapper<CameraApplyRecord> {

	List<CameraApplyRecordVO> selectCameraApplyRecordPage(IPage<CameraApplyRecordVO> page, @Param("query") CameraApplyQueryDTO query);
}
