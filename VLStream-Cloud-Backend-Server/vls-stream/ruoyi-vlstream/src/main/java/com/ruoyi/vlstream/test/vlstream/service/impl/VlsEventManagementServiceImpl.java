/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.enums.EventLevelEnum;
import com.ruoyi.vlstream.test.vlstream.enums.EventStatusEnum;
import com.ruoyi.vlstream.test.vlstream.excel.VlsEventManagementExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsEventManagementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 事件管理表 服务实现类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsEventManagementServiceImpl extends BaseServiceImpl<VlsEventManagementMapper, EventManagement> implements IVlsEventManagementService {

	@Override
	public IPage<EventManagementVO> selectVlsEventManagementPage(IPage<EventManagementVO> page, EventManagementVO vlsEventManagement) {
		return page.setRecords(baseMapper.selectVlsEventManagementPage(page, vlsEventManagement));
	}

	@Override
	public List<VlsEventManagementExcel> exportVlsEventManagement(Wrapper<EventManagement> queryWrapper) {
		List<VlsEventManagementExcel> vlsEventManagementList = baseMapper.exportVlsEventManagement(queryWrapper);
		//vlsEventManagementList.forEach(vlsEventManagement -> {
		//	vlsEventManagement.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsEventManagementEntity.getType()));
		//});
		return vlsEventManagementList;
	}

	@Override
	public IPage<EventManagement> pageEvents(Page<EventManagement> page,
											 String eventType,
											 String eventStatus,
											 String eventLevel,
											 String keyword,
											 LocalDateTime startTime,
											 LocalDateTime endTime) {
		LambdaQueryWrapper<EventManagement> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(StringUtils.isNotBlank(eventType), EventManagement::getEventType, eventType);
		wrapper.eq(StringUtils.isNotBlank(eventStatus), EventManagement::getEventStatus, eventStatus);
		wrapper.eq(StringUtils.isNotBlank(eventLevel), EventManagement::getEventLevel, eventLevel);
		wrapper.ge(startTime != null, EventManagement::getReportTime, startTime);
		wrapper.le(endTime != null, EventManagement::getReportTime, endTime);
		if (StringUtils.isNotBlank(keyword)) {
			wrapper.and(w -> w.like(EventManagement::getEventDesc, keyword)
				.or().like(EventManagement::getReportLocation, keyword)
				.or().like(EventManagement::getReportDevice, keyword));
		}
		wrapper.orderByDesc(EventManagement::getReportTime);
		return page(page, wrapper);
	}

	@Override
	public EventManagement getEventById(Long id) {
		if (id == null) {
			return null;
		}
		return getById(id);
	}

	@Override
	public boolean createEvent(EventManagement eventManagement) {
		if (eventManagement == null) {
			return false;
		}
		if (Objects.nonNull(eventManagement.getEventLevel())) {
			eventManagement.setEventLevel(EventLevelEnum.medium);
		}
		if (Objects.nonNull(eventManagement.getEventStatus())) {
			eventManagement.setEventStatus(EventStatusEnum.pending);
		}
		if (eventManagement.getReportTime() == null) {
			eventManagement.setReportTime(new Date());
		}
		boolean saved = save(eventManagement);
		return saved;
	}

	@Override
	public boolean updateEvent(EventManagement eventManagement) {
		return eventManagement != null && updateById(eventManagement);
	}

	@Override
	public boolean removeEvent(Long id) {
		if (id == null) {
			return false;
		}
		return removeById(id);
	}

	@Override
	public boolean removeEvents(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return false;
		}
		return removeByIds(ids);
	}

}
