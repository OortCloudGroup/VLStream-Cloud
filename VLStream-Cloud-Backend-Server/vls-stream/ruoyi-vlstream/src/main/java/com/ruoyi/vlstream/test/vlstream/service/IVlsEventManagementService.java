/*
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
 * 事件管理表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsEventManagementService extends BaseService<EventManagement> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsEventManagement 查询参数
	 * @return IPage<VlsEventManagementVO>
	 */
	IPage<EventManagementVO> selectVlsEventManagementPage(IPage<EventManagementVO> page, EventManagementVO vlsEventManagement);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
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
