/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceTagRelationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceTagRelationDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceTagRelation;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceTagRelationVO;

import java.util.List;
import java.util.Map;

/**
 * device service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsDeviceTagRelationService extends BaseService<DeviceTagRelation> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsDeviceTagRelation Query parameter
	 * @return IPage<VlsDeviceTagRelationVO>
	 */
	IPage<DeviceTagRelationVO> selectVlsDeviceTagRelationPage(IPage<DeviceTagRelationVO> page, DeviceTagRelationVO vlsDeviceTagRelation);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsDeviceTagRelationExcel>
	 */
	List<VlsDeviceTagRelationExcel> exportVlsDeviceTagRelation(Wrapper<DeviceTagRelation> queryWrapper);

	/**
	 * Set device ( )
	 *
	 * @param deviceId deviceID
	 * @param tagIds ID
	 * @param createdBy
	 * @return whether successfully
	 */
	boolean setDeviceTags(Long deviceId, List<Long> tagIds, Long createdBy);

	/**
	 * device ( )
	 *
	 * @param deviceId deviceID
	 * @param tagIds ID
	 * @param createdBy
	 * @return whether successfully
	 */
	boolean addDeviceTags(Long deviceId, List<Long> tagIds, Long createdBy);

	/**
	 * device
	 *
	 * @param deviceId deviceID
	 * @param tagIds ID
	 * @return whether successfully
	 */
	boolean removeDeviceTags(Long deviceId, List<Long> tagIds);

	/**
	 * device all
	 *
	 * @param deviceId deviceID
	 * @return whether successfully
	 */
	boolean clearDeviceTags(Long deviceId);

	/**
	 * Get device all
	 *
	 * @param deviceId deviceID
	 * @return info
	 */
	List<DeviceTagRelationDTO> getDeviceTags(Long deviceId);

	/**
	 * Get device ID
	 *
	 * @param deviceId deviceID
	 * @return ID
	 */
	List<Long> getDeviceTagIds(Long deviceId);

	/**
	 * Get device
	 *
	 * @param tagId ID
	 * @return deviceinfo
	 */
	List<Map<String, Object>> getDevicesByTag(Long tagId);

	/**
	 * Query device ( - all )
	 *
	 * @param tagIds ID
	 * @return deviceID
	 */
	List<Long> findDevicesByAllTags(List<Long> tagIds);

	/**
	 * Query device ( - )
	 *
	 * @param tagIds ID
	 * @return deviceID
	 */
	List<Long> findDevicesByAnyTags(List<Long> tagIds);

	/**
	 * Set device
	 *
	 * @param deviceTagMap deviceID -> ID
	 * @param createdBy
	 * @return successfullySet device
	 */
	int batchSetDeviceTags(Map<Long, List<Long>> deviceTagMap, Long createdBy);

	/**
	 * device device
	 *
	 * @param sourceDeviceId deviceID
	 * @param targetDeviceIds deviceID
	 * @param createdBy
	 * @return whether successfully
	 */
	boolean copyDeviceTags(Long sourceDeviceId, List<Long> targetDeviceIds, Long createdBy);

	/**
	 * Get device info
	 *
	 * @return info
	 */
	List<Map<String, Object>> getDeviceTagStatistics();

	/**
	 * Get
	 *
	 * @return
	 */
	List<Map<String, Object>> getTagUsageStatistics();

	/**
	 * devicewhether
	 *
	 * @param deviceId deviceID
	 * @param tagId ID
	 * @return whether in
	 */
	boolean hasDeviceTag(Long deviceId, Long tagId);

	/**
	 * Get device
	 *
	 * @param tagId ID
	 * @return device
	 */
	int getTagDeviceCount(Long tagId);

	/**
	 * ID whether
	 *
	 * @param tagIds ID
	 * @return
	 */
	Map<String, Object> validateTagIds(List<Long> tagIds);

	/**
	 * Get device info ( layer )
	 *
	 * @param deviceId deviceID
	 * @return info
	 */
	Map<String, Object> getDeviceTagDetails(Long deviceId);

	/**
	 * Get device
	 *
	 * @param categoryType (own/public)
	 * @param level layer (1/2)
	 * @return deviceinfo
	 */
	List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level);

	/**
	 *
	 * new tag_management in usage_countfield
	 *
	 * @return whether successfully
	 */
	boolean syncTagUsageCount();

}
