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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceInfoExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceInfoVO;

import java.util.List;
import java.util.Map;

/**
 * deviceinfo service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsDeviceInfoService extends BaseService<DeviceInfo> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsDeviceInfo Query parameter
	 * @return IPage<VlsDeviceInfoVO>
	 */
	IPage<DeviceInfoVO> selectVlsDeviceInfoPage(IPage<DeviceInfoVO> page, DeviceInfoVO vlsDeviceInfo);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsDeviceInfoExcel>
	 */
	List<VlsDeviceInfoExcel> exportVlsDeviceInfo(Wrapper<DeviceInfo> queryWrapper);

	/**
	 * Query deviceinfo
	 *
	 * @param page object
	 * @param deviceName device deviceID
	 * @param tag device ( device_typefield)
	 * @param status device
	 * @return deviceinfo
	 */
	IPage<DeviceInfo> getDevicePage(Page<DeviceInfo> page,
									String deviceName,
									String tag,
									String status);

	/**
	 * device Query deviceinfo
	 *
	 * @param deviceId device
	 * @return deviceinfo
	 */
	DeviceInfo getByDeviceId(String deviceId);

	/**
	 * Add deviceinfo
	 *
	 * @param deviceInfo deviceinfo
	 * @return whether successfully
	 */
	boolean addDevice(DeviceInfo deviceInfo);

	/**
	 * new deviceinfo
	 *
	 * @param deviceInfo deviceinfo
	 * @return whether successfully
	 */
	boolean updateDevice(DeviceInfo deviceInfo);

	/**
	 * Dispatch comma separated algorithm ids to a device.
	 *
	 * @param algorithmId
	 * @param deviceIds
	 * @return true if update succeeded
	 */
	boolean dispatchAlgorithms(Long algorithmId, String deviceIds);

	/**
	 * Dispatch the latest completed artifact in the requested format.
	 *
	 * @param algorithmId algorithm ID
	 * @param deviceIds comma-separated device table IDs
	 * @param modelType pt、onnx、rknn、int8-rknn om
	 * @return true when every selected device task was published
	 */
	boolean dispatchAlgorithms(Long algorithmId, String deviceIds, String modelType);

	/**
	 * Delete deviceinfo
	 *
	 * @param id deviceID
	 * @return whether successfully
	 */
	boolean deleteDevice(Long id);

	/**
	 * Batch delete deviceinfo
	 *
	 * @param ids deviceID
	 * @return whether successfully
	 */
	boolean deleteDeviceBatch(List<Long> ids);

	/**
	 * new device
	 *
	 * @param id deviceID
	 * @param status
	 * @return whether successfully
	 */
	boolean updateDeviceStatus(Long id, Integer status);

	/**
	 * new device
	 *
	 * @param ids deviceID
	 * @param status
	 * @return whether successfully
	 */
	boolean updateDeviceStatusBatch(List<Long> ids, String status);

	/**
	 * Get device
	 *
	 * @param status device
	 * @return device
	 */
	List<DeviceInfo> getDevicesByStatus(String status);

	/**
	 * device Get device
	 *
	 * @param deviceType device
	 * @return device
	 */
	List<DeviceInfo> getDevicesByType(String deviceType);

	/**
	 * Get device
	 *
	 * @param position device
	 * @return device
	 */
	List<DeviceInfo> getDevicesByPosition(String position);

	/**
	 * device whether in
	 *
	 * @param deviceId device
	 * @return whether in
	 */
	boolean checkDeviceIdExists(String deviceId);

	/**
	 * device
	 *
	 * @param id deviceID
	 * @return
	 */
	Map<String, Object> testDeviceConnection(Long id);

	/**
	 * Get device info
	 *
	 * @return info
	 */
	Map<String, Object> getDeviceStatistics();

	/**
	 * Get all device ( )
	 *
	 * @return device
	 */
	List<String> getAllTags();

	/**
	 * Get all device
	 *
	 * @return
	 */
	List<String> getAllBrands();

	/**
	 * deviceconfiguration
	 *
	 * @param deviceInfo deviceinfo
	 * @return
	 */
	Map<String, Object> validateDevice(DeviceInfo deviceInfo);

	/**
	 * new device
	 *
	 * @param deviceId deviceID
	 * @return new
	 */
	Map<String, Object> refreshDeviceStatus(Long deviceId);

	/**
	 * Import device
	 *
	 * @param deviceList device
	 * @return Import
	 */
	Map<String, Object> batchImportDevices(List<DeviceInfo> deviceList);

	/**
	 * Export deviceinfo
	 *
	 * @param deviceIds deviceID , is empty Export all device
	 * @return Export data
	 */
	List<DeviceInfo> exportDevices(List<Long> deviceIds);

	/**
	 * Get deviceconfigurationparameter
	 *
	 * @param deviceId deviceID
	 * @return configurationparameter
	 */
	Map<String, Object> getDeviceConfig(Long deviceId);

	/**
	 * new deviceconfigurationparameter
	 *
	 * @param deviceId deviceID
	 * @param config configurationparameter
	 * @return whether successfully
	 */
	boolean updateDeviceConfig(Long deviceId, Map<String, Object> config);

	/**
	 * PTZcontrol
	 *
	 * @param deviceId deviceID
	 * @param command PTZ
	 * @param params parameter
	 * @return control
	 */
	Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params);

	/**
	 * Get device info
	 *
	 * @param deviceId deviceID
	 * @return info
	 */
	Map<String, Object> getVideoStreamInfo(Long deviceId);

	/**
	 * device info
	 */
	@Data
	class DeviceStatistics {
		private Long totalCount;
		private Long onlineCount;
		private Long offlineCount;
		private Long faultCount;
	}


}
