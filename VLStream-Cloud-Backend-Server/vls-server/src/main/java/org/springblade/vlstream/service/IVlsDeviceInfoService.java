package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsDeviceInfoExcel;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.vo.DeviceInfoVO;

import java.util.List;
import java.util.Map;

/**
 * Device Information Table Service Class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsDeviceInfoService extends BaseService<DeviceInfo> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsDeviceInfo query parameters
	 * @return IPage<VlsDeviceInfoVO>
	 */
	IPage<DeviceInfoVO> selectVlsDeviceInfoPage(IPage<DeviceInfoVO> page, DeviceInfoVO vlsDeviceInfo);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsDeviceInfoExcel>
	 */
	List<VlsDeviceInfoExcel> exportVlsDeviceInfo(Wrapper<DeviceInfo> queryWrapper);

	/**
	 * Paginated query for device information
	 *
	 * @param page       pagination object
	 * @param deviceName device name or device ID
	 * @param tag        device tag (actually corresponds to the device_type field)
	 * @param status     device status
	 * @return paginated list of device information
	 */
	IPage<DeviceInfo> getDevicePage(Page<DeviceInfo> page,
									String deviceName,
									String tag,
									String status);

	/**
	 * Query device info by device number
	 *
	 * @param deviceId device code
	 * @return device information
	 */
	DeviceInfo getByDeviceId(String deviceId);

	/**
	 * Add device information
	 *
	 * @param deviceInfo device information
	 * @return whether successful
	 */
	boolean addDevice(DeviceInfo deviceInfo);

	/**
	 * Update device information
	 *
	 * @param deviceInfo device information
	 * @return whether successful
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
	 * Delete device information
	 *
	 * @param id device ID
	 * @return whether successful
	 */
	boolean deleteDevice(Long id);

	/**
	 * Batch delete device information
	 *
	 * @param ids device ID list
	 * @return whether successful
	 */
	boolean deleteDeviceBatch(List<Long> ids);

	/**
	 * Update device status
	 *
	 * @param id     device ID
	 * @param status status
	 * @return whether successful
	 */
	boolean updateDeviceStatus(Long id, Integer status);

	/**
	 * Batch update device status
	 *
	 * @param ids    device ID list
	 * @param status status
	 * @return whether successful
	 */
	boolean updateDeviceStatusBatch(List<Long> ids, String status);

	/**
	 * Get device list by status
	 *
	 * @param status device status
	 * @return device list
	 */
	List<DeviceInfo> getDevicesByStatus(String status);

	/**
	 * Get device list by device type
	 *
	 * @param deviceType device type
	 * @return device list
	 */
	List<DeviceInfo> getDevicesByType(String deviceType);

	/**
	 * Get device list by location
	 *
	 * @param position device position
	 * @return device list
	 */
	List<DeviceInfo> getDevicesByPosition(String position);

	/**
	 * Check if device number exists
	 *
	 * @param deviceId device code
	 * @return whether exists
	 */
	boolean checkDeviceIdExists(String deviceId);

	/**
	 * Test device connection
	 *
	 * @param id device ID
	 * @return Connection result
	 */
	Map<String, Object> testDeviceConnection(Long id);

	/**
	 * Get device statistics
	 *
	 * @return statistics
	 */
	Map<String, Object> getDeviceStatistics();

	/**
	 * Get all device type list (used for label list)
	 *
	 * @return device type list
	 */
	List<String> getAllTags();

	/**
	 * Get all device brand list
	 *
	 * @return brand list
	 */
	List<String> getAllBrands();

	/**
	 * Verify device configuration
	 *
	 * @param deviceInfo device information
	 * @return Verification results
	 */
	Map<String, Object> validateDevice(DeviceInfo deviceInfo);

	/**
	 * Refresh device status
	 *
	 * @param deviceId device ID
	 * @return refresh result
	 */
	Map<String, Object> refreshDeviceStatus(Long deviceId);

	/**
	 * Batch import devices
	 *
	 * @param deviceList device list
	 * @return import result
	 */
	Map<String, Object> batchImportDevices(List<DeviceInfo> deviceList);

	/**
	 * Export device information
	 *
	 * @param deviceIds device ID list; if empty, export all devices
	 * @return exported data
	 */
	List<DeviceInfo> exportDevices(List<Long> deviceIds);

	/**
	 * Get device configuration parameters
	 *
	 * @param deviceId device ID
	 * @return Configuration parameters
	 */
	Map<String, Object> getDeviceConfig(Long deviceId);

	/**
	 * Update device configuration parameters
	 *
	 * @param deviceId device ID
	 * @param config configuration parameters
	 * @return whether successful
	 */
	boolean updateDeviceConfig(Long deviceId, Map<String, Object> config);

	/**
	 * PTZ control
	 *
	 * @param deviceId device ID
	 * @param command PTZ command
	 * @param params parameters
	 * @return control result
	 */
	Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params);

	/**
	 * Get device video stream info
	 *
	 * @param deviceId device ID
	 * @return video stream information
	 */
	Map<String, Object> getVideoStreamInfo(Long deviceId);

	/**
	 * Device Statistics
	 */
	@Data
	class DeviceStatistics {
		private Long totalCount;
		private Long onlineCount;
		private Long offlineCount;
		private Long faultCount;
	}


}
