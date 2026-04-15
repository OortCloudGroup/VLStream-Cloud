package com.vlstream.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.DeviceTagRelation;

import java.util.List;
import java.util.Map;

/**
 * 设备标签关联服务层接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface DeviceTagRelationService extends IService<DeviceTagRelation> {

    /**
     * 设置设备标签（覆盖原有标签）
     *
     * @param deviceId 设备ID
     * @param tagIds 标签ID列表
     * @param createdBy 创建人
     * @return 是否成功
     */
    boolean setDeviceTags(Long deviceId, List<Long> tagIds, String createdBy);

    /**
     * 添加设备标签（追加到现有标签）
     *
     * @param deviceId 设备ID
     * @param tagIds 标签ID列表
     * @param createdBy 创建人
     * @return 是否成功
     */
    boolean addDeviceTags(Long deviceId, List<Long> tagIds, String createdBy);

    /**
     * 移除设备标签
     *
     * @param deviceId 设备ID
     * @param tagIds 标签ID列表
     * @return 是否成功
     */
    boolean removeDeviceTags(Long deviceId, List<Long> tagIds);

    /**
     * 清除设备的所有标签
     *
     * @param deviceId 设备ID
     * @return 是否成功
     */
    boolean clearDeviceTags(Long deviceId);

    /**
     * 获取设备的所有标签
     *
     * @param deviceId 设备ID
     * @return 标签信息列表
     */
    List<DeviceTagRelation> getDeviceTags(Long deviceId);

    /**
     * 获取设备的标签ID列表
     *
     * @param deviceId 设备ID
     * @return 标签ID列表
     */
    List<Long> getDeviceTagIds(Long deviceId);

    /**
     * 获取带有指定标签的设备列表
     *
     * @param tagId 标签ID
     * @return 设备信息列表
     */
    List<Map<String, Object>> getDevicesByTag(Long tagId);

    /**
     * 根据多个标签查询设备（交集 - 必须同时包含所有标签）
     *
     * @param tagIds 标签ID列表
     * @return 设备ID列表
     */
    List<Long> findDevicesByAllTags(List<Long> tagIds);

    /**
     * 根据多个标签查询设备（并集 - 包含任意一个标签）
     *
     * @param tagIds 标签ID列表
     * @return 设备ID列表
     */
    List<Long> findDevicesByAnyTags(List<Long> tagIds);

    /**
     * 批量设置设备标签
     *
     * @param deviceTagMap 设备ID -> 标签ID列表的映射
     * @param createdBy 创建人
     * @return 成功设置的设备数量
     */
    int batchSetDeviceTags(Map<Long, List<Long>> deviceTagMap, String createdBy);

    /**
     * 复制设备标签到其他设备
     *
     * @param sourceDeviceId 源设备ID
     * @param targetDeviceIds 目标设备ID列表
     * @param createdBy 创建人
     * @return 是否成功
     */
    boolean copyDeviceTags(Long sourceDeviceId, List<Long> targetDeviceIds, String createdBy);

    /**
     * 获取设备标签统计信息
     *
     * @return 统计信息
     */
    List<Map<String, Object>> getDeviceTagStatistics();

    /**
     * 获取标签使用统计
     *
     * @return 标签使用统计
     */
    List<Map<String, Object>> getTagUsageStatistics();

    /**
     * 检查设备是否有指定标签
     *
     * @param deviceId 设备ID
     * @param tagId 标签ID
     * @return 是否存在
     */
    boolean hasDeviceTag(Long deviceId, Long tagId);

    /**
     * 获取标签的设备数量
     *
     * @param tagId 标签ID
     * @return 设备数量
     */
    int getTagDeviceCount(Long tagId);

    /**
     * 验证标签ID列表是否有效
     *
     * @param tagIds 标签ID列表
     * @return 验证结果
     */
    Map<String, Object> validateTagIds(List<Long> tagIds);

    /**
     * 获取设备标签的详细信息（包含标签层级结构）
     *
     * @param deviceId 设备ID
     * @return 标签详细信息
     */
    Map<String, Object> getDeviceTagDetails(Long deviceId);

    /**
     * 根据标签类型获取设备列表
     *
     * @param categoryType 标签类型（own/public）
     * @param level 标签层级（1/2）
     * @return 设备信息
     */
    List<Map<String, Object>> getDevicesByTagCategory(String categoryType, Integer level);

    /**
     * 同步标签使用计数
     * 更新tag_management表中的usage_count字段
     *
     * @return 是否成功
     */
    boolean syncTagUsageCount();
} 