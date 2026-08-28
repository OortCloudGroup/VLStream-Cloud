/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsEventManagementExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;

import java.util.List;

/**
 * event Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsEventManagementMapper extends BaseMapper<EventManagement> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsEventManagement Query parameter
	 * @return List<VlsEventManagementVO>
	 */
	List<EventManagementVO> selectVlsEventManagementPage(IPage page, EventManagementVO vlsEventManagement);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsEventManagementExcel>
	 */
	List<VlsEventManagementExcel> exportVlsEventManagement(@Param("ew") Wrapper<EventManagement> queryWrapper);

	@InterceptorIgnore(tenantLine = "true")
	@Update("UPDATE vls_event_management SET is_report = 1, update_time = #{now} "
		+ "WHERE id = #{eventId} AND tenant_id = #{tenantId} AND is_deleted = 0")
	int markReported(@Param("eventId") Long eventId, @Param("tenantId") String tenantId,
					 @Param("now") java.util.Date now);

}
