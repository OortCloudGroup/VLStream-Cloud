package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.DeviceInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 设备信息服务层接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DeviceInfoService extends IService<DeviceInfo> {

    /**
     * 分页查询设备信息
     *
     * @param page       分页对象
     * @param deviceName 设备名称或设备ID
     * @param tag        设备标签（实际对应device_type字段）
     * @param status     设备状态
     * @return 设备信息分页列表
     */
    IPage<DeviceInfo> getDevicePage(Page<DeviceInfo> page,
                                   String deviceName,
                                   String tag,
                                   String status);

    /**
     * 根据设备编号查询设备信息
     *
     * @param deviceId 设备编号
     * @return 设备信息
     */
    DeviceInfo getByDeviceId(String deviceId);

    /**
     * 新增设备信息
     *
     * @param deviceInfo 设备信息
     * @return 是否成功
     */
    boolean addDevice(DeviceInfo deviceInfo);

    /**
     * 更新设备信息
     *
     * @param deviceInfo 设备信息
     * @return 是否成功
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
     * 删除设备信息
     *
     * @param id 设备ID
     * @return 是否成功
     */
    boolean deleteDevice(Long id);

    /**
     * 批量删除设备信息
     *
     * @param ids 设备ID列表
     * @return 是否成功
     */
    boolean deleteDeviceBatch(List<Long> ids);

    /**
     * 更新设备状态
     *
     * @param id     设备ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateDeviceStatus(Long id, String status);

    /**
     * 批量更新设备状态
     *
     * @param ids    设备ID列表
     * @param status 状态
     * @return 是否成功
     */
    boolean updateDeviceStatusBatch(List<Long> ids, String status);

    /**
     * 根据状态获取设备列表
     *
     * @param status 设备状态
     * @return 设备列表
     */
    List<DeviceInfo> getDevicesByStatus(String status);

    /**
     * 根据设备类型获取设备列表
     *
     * @param deviceType 设备类型
     * @return 设备列表
     */
    List<DeviceInfo> getDevicesByType(String deviceType);

    /**
     * 根据品牌获取设备列表
     *
     * @param brand 设备品牌
     * @return 设备列表
     */
    List<DeviceInfo> getDevicesByBrand(String brand);

    /**
     * 根据位置获取设备列表
     *
     * @param position 设备位置
     * @return 设备列表
     */
    List<DeviceInfo> getDevicesByPosition(String position);

    /**
     * 检查设备编号是否存在
     *
     * @param deviceId 设备编号
     * @return 是否存在
     */
    boolean checkDeviceIdExists(String deviceId);

    /**
     * 测试设备连接
     *
     * @param id 设备ID
     * @return 连接结果
     */
    Map<String, Object> testDeviceConnection(Long id);

    /**
     * 获取设备统计信息
     *
     * @return 统计信息
     */
    Map<String, Object> getDeviceStatistics();

    /**
     * 获取所有设备类型列表（用于标签列表）
     *
     * @return 设备类型列表
     */
    List<String> getAllTags();

    /**
     * 获取所有设备品牌列表
     *
     * @return 品牌列表
     */
    List<String> getAllBrands();

    /**
     * 验证设备配置
     *
     * @param deviceInfo 设备信息
     * @return 验证结果
     */
    Map<String, Object> validateDevice(DeviceInfo deviceInfo);

    /**
     * 刷新设备状态
     *
     * @param deviceId 设备ID
     * @return 刷新结果
     */
    Map<String, Object> refreshDeviceStatus(Long deviceId);

    /**
     * 批量导入设备
     *
     * @param deviceList 设备列表
     * @return 导入结果
     */
    Map<String, Object> batchImportDevices(List<DeviceInfo> deviceList);

    /**
     * 导出设备信息
     *
     * @param deviceIds 设备ID列表，为空时导出所有设备
     * @return 导出数据
     */
    List<DeviceInfo> exportDevices(List<Long> deviceIds);

    /**
     * 获取设备配置参数
     *
     * @param deviceId 设备ID
     * @return 配置参数
     */
    Map<String, Object> getDeviceConfig(Long deviceId);

    /**
     * 更新设备配置参数
     *
     * @param deviceId 设备ID
     * @param config 配置参数
     * @return 是否成功
     */
    boolean updateDeviceConfig(Long deviceId, Map<String, Object> config);

    /**
     * PTZ控制
     *
     * @param deviceId 设备ID
     * @param command PTZ命令
     * @param params 参数
     * @return 控制结果
     */
    Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params);

    /**
     * 获取设备视频流信息
     *
     * @param deviceId 设备ID
     * @return 视频流信息
     */
    Map<String, Object> getVideoStreamInfo(Long deviceId);

    /**
     * 设备统计信息
     */
    @Data
    class DeviceStatistics {
        private Long totalCount;
        private Long onlineCount;
        private Long offlineCount;
        private Long faultCount;
    }

} 
