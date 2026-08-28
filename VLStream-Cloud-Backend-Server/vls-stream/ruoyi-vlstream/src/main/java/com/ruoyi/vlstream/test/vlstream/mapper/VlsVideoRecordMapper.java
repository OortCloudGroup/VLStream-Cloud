/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsVideoRecordExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.VideoRecordVO;

import java.util.List;

/**
 * record Mapper interface
 *
 * @author Oort
 * @since 2025-12-25
 */
public interface VlsVideoRecordMapper extends BaseMapper<VideoRecord> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsVideoRecord Query parameter
	 * @return List<VlsVideoRecordVO>
	 */
	List<VideoRecordVO> selectVlsVideoRecordPage(IPage page, VideoRecordVO vlsVideoRecord);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsVideoRecordExcel>
	 */
	List<VlsVideoRecordExcel> exportVlsVideoRecord(@Param("ew") Wrapper<VideoRecord> queryWrapper);

}
