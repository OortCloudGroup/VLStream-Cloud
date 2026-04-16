package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.DeviceInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Device Info Service Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DeviceInfoService extends IService<DeviceInfo> {

    /**
     * Query device info with pagination
     *
     * @param page       Pagination object
     * @param deviceName Device name or device ID
     * @param tag        Device tag (actually corresponds to device_type field)
     * @param status     Device status
     * @return Device info pagination list
     */
    IPage<DeviceInfo> getDevicePage(Page<DeviceInfo> page,
                                   String deviceName,
                                   String tag,
                                   String status);

    /**
     * Query device info by device ID
     *
     * @param deviceId Device ID
     * @return Device info
     */
    DeviceInfo getByDeviceId(String deviceId);

    /**
     * Add device info
     *
     * @param deviceInfo Device info
     * @return Whether successful
     */
    boolean addDevice(DeviceInfo deviceInfo);

    /**
     * Update device info
     *
     * @param deviceInfo Device info
     * @return Whether successful
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
     * Delete device info
     *
     * @param id Device ID
     * @return Whether successful
     */
    boolean deleteDevice(Long id);

    /**
     * Batch delete device info
     *
     * @param ids Device ID list
     * @return Whether successful
     */
    boolean deleteDeviceBatch(List<Long> ids);

    /**
     * Update device status
     *
     * @param id     Device ID
     * @param status Status
     * @return Whether successful
     */
    boolean updateDeviceStatus(Long id, String status);

    /**
     * Batch update device status
     *
     * @param ids    Device ID list
     * @param status Status
     * @return Whether successful
     */
    boolean updateDeviceStatusBatch(List<Long> ids, String status);

    /**
     * Get device list by status
     *
     * @param status Device status
     * @return Device list
     */
    List<DeviceInfo> getDevicesByStatus(String status);

    /**
     * Get device list by device type
     *
     * @param deviceType Device type
     * @return Device list
     */
    List<DeviceInfo> getDevicesByType(String deviceType);

    /**
     * Get device list by brand
     *
     * @param brand Device brand
     * @return Device list
     */
    List<DeviceInfo> getDevicesByBrand(String brand);

    /**
     * Get device list by position
     *
     * @param position Device position
     * @return Device list
     */
    List<DeviceInfo> getDevicesByPosition(String position);

    /**
     * Check if device ID exists
     *
     * @param deviceId Device ID
     * @return Whether exists
     */
    boolean checkDeviceIdExists(String deviceId);

    /**
     * Test device connection
     *
     * @param id Device ID
     * @return Connection result
     */
    Map<String, Object> testDeviceConnection(Long id);

    /**
     * Get device statistics
     *
     * @return Statistics
     */
    Map<String, Object> getDeviceStatistics();

    /**
     * Get all device type list (for tag list)
     *
     * @return Device type list
     */
    List<String> getAllTags();

    /**
     * Get all device brand list
     *
     * @return Brand list
     */
    List<String> getAllBrands();

    /**
     * Validate device configuration
     *
     * @param deviceInfo Device info
     * @return Validation result
     */
    Map<String, Object> validateDevice(DeviceInfo deviceInfo);

    /**
     * Refresh device status
     *
     * @param deviceId Device ID
     * @return Refresh result
     */
    Map<String, Object> refreshDeviceStatus(Long deviceId);

    /**
     * Batch import devices
     *
     * @param deviceList Device list
     * @return Import result
     */
    Map<String, Object> batchImportDevices(List<DeviceInfo> deviceList);

    /**
     * Export device info
     *
     * @param deviceIds Device ID list, export all devices when empty
     * @return Export data
     */
    List<DeviceInfo> exportDevices(List<Long> deviceIds);

    /**
     * Get device configuration parameters
     *
     * @param deviceId Device ID
     * @return Configuration parameters
     */
    Map<String, Object> getDeviceConfig(Long deviceId);

    /**
     * Update device configuration parameters
     *
     * @param deviceId Device ID
     * @param config Configuration parameters
     * @return Whether successful
     */
    boolean updateDeviceConfig(Long deviceId, Map<String, Object> config);

    /**
     * PTZ control
     *
     * @param deviceId Device ID
     * @param command PTZ command
     * @param params Parameters
     * @return Control result
     */
    Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params);

    /**
     * Get device video stream info
     *
     * @param deviceId Device ID
     * @return Video stream info
     */
    Map<String, Object> getVideoStreamInfo(Long deviceId);

    /**
     * Device statistics
     */
    @Data
    class DeviceStatistics {
        private Long totalCount;
        private Long onlineCount;
        private Long offlineCount;
        private Long faultCount;
    }

} 
