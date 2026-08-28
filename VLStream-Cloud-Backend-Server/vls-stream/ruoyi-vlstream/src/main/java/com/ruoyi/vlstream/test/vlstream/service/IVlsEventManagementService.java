/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsEventManagementExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * event service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsEventManagementService extends BaseService<EventManagement> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsEventManagement Query parameter
	 * @return IPage<VlsEventManagementVO>
	 */
	IPage<EventManagementVO> selectVlsEventManagementPage(IPage<EventManagementVO> page, EventManagementVO vlsEventManagement);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsEventManagementExcel>
	 */
	List<VlsEventManagementExcel> exportVlsEventManagement(Wrapper<EventManagement> queryWrapper);

	/**
	 * Page query with optional filters.
	 */
	IPage<EventManagement> pageEvents(Page<EventManagement> page,
									  String eventType,
									  String eventStatus,
									  String eventLevel,
									  String keyword,
									  LocalDateTime startTime,
									  LocalDateTime endTime);

	/**
	 * Get one event by id.
	 */
	EventManagement getEventById(Long id);

	/**
	 * Create a new event.
	 */
	boolean createEvent(EventManagement eventManagement);

	/**
	 * Update an existing event.
	 */
	boolean updateEvent(EventManagement eventManagement);

	/**
	 * Logical delete by id.
	 */
	boolean removeEvent(Long id);

	/**
	 * Batch logical delete.
	 */
	boolean removeEvents(List<Long> ids);

}
