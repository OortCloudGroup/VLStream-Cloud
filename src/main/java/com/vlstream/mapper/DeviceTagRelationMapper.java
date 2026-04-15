package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.DeviceTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

import java.util.List;
import java.util.Map;

/**
 * 设备标签关联数据访问层接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface DeviceTagRelationMapper extends BaseMapper<DeviceTagRelation> {

    /**
     * 根据设备ID获取标签信息
     *
     * @param deviceId 设备ID
     * @return 标签信息列表
     */
    @Select("SELECT " +
            "dtr.id, dtr.device_id, dtr.tag_id, dtr.created_by, dtr.create_time, " +
            "tm.tag_name, tm.category_type, tm.tag_color, tm.tag_icon, tm.level, tm.parent_id " +
            "FROM device_tag_relation dtr " +
            "INNER JOIN tag_management tm ON dtr.tag_id = tm.id " +
            "WHERE dtr.device_id = #{deviceId} AND tm.deleted = 0 AND tm.is_active = 1 " +
            "ORDER BY tm.category_type, tm.sort_order")
    List<DeviceTagRelation> selectTagsByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 根据标签ID获取设备列表
     *
     * @param tagId 标签ID
     * @return 设备标签关联列表
     */
    @Select("SELECT " +
            "dtr.id, dtr.device_id, dtr.tag_id, dtr.created_by, dtr.create_time, " +
            "di.device_name, di.device_id as device_code, di.status " +
            "FROM device_tag_relation dtr " +
            "INNER JOIN device_info di ON dtr.device_id = di.id " +
            "WHERE dtr.tag_id = #{tagId} AND di.deleted = 0 " +
            "ORDER BY di.device_name")
    List<Map<String, Object>> selectDevicesByTagId(@Param("tagId") Long tagId);

    /**
     * 批量添加设备标签关联
     *
     * @param deviceId 设备ID
     * @param tagIds 标签ID列表
     * @param createdBy 创建人
     * @return 插入数量
     */
    @Insert("<script>" +
            "INSERT INTO device_tag_relation (device_id, tag_id, created_by) VALUES " +
            "<foreach collection='tagIds' item='tagId' separator=','>" +
            "(#{deviceId}, #{tagId}, #{createdBy})" +
            "</foreach>" +
            "</script>")
    int batchInsertDeviceTags(@Param("deviceId") Long deviceId, 
                             @Param("tagIds") List<Long> tagIds, 
                             @Param("createdBy") String createdBy);

    /**
     * 删除设备的所有标签关联
     *
     * @param deviceId 设备ID
     * @return 删除数量
     */
    @Delete("DELETE FROM device_tag_relation WHERE device_id = #{deviceId}")
    int deleteByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 删除设备的指定标签关联
     *
     * @param deviceId 设备ID
     * @param tagIds 标签ID列表
     * @return 删除数量
     */
    @Delete("<script>" +
            "DELETE FROM device_tag_relation " +
            "WHERE device_id = #{deviceId} AND tag_id IN " +
            "<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
            "#{tagId}" +
            "</foreach>" +
            "</script>")
    int deleteDeviceTagsBatch(@Param("deviceId") Long deviceId, @Param("tagIds") List<Long> tagIds);

    /**
     * 获取设备标签统计信息
     *
     * @return 统计信息
     */
    @Select("SELECT " +
            "device_id, " +
            "COUNT(tag_id) as tag_count, " +
            "COUNT(CASE WHEN tm.category_type = 'own' THEN 1 END) as own_tag_count, " +
            "COUNT(CASE WHEN tm.category_type = 'public' THEN 1 END) as public_tag_count " +
            "FROM device_tag_relation dtr " +
            "INNER JOIN tag_management tm ON dtr.tag_id = tm.id " +
            "WHERE tm.deleted = 0 AND tm.is_active = 1 " +
            "GROUP BY device_id")
    List<Map<String, Object>> getDeviceTagStatistics();

    /**
     * 获取标签使用统计
     *
     * @return 标签使用统计
     */
    @Select("SELECT " +
            "tm.id as tag_id, " +
            "tm.tag_name, " +
            "tm.category_type, " +
            "tm.level, " +
            "tm.tag_color, " +
            "COUNT(dtr.device_id) as device_count " +
            "FROM tag_management tm " +
            "LEFT JOIN device_tag_relation dtr ON tm.id = dtr.tag_id " +
            "WHERE tm.deleted = 0 AND tm.is_active = 1 " +
            "GROUP BY tm.id, tm.tag_name, tm.category_type, tm.level, tm.tag_color " +
            "ORDER BY tm.category_type, tm.level, device_count DESC")
    List<Map<String, Object>> getTagUsageStatistics();

    /**
     * 根据多个标签查询设备（交集）
     *
     * @param tagIds 标签ID列表
     * @return 设备ID列表
     */
    @Select("<script>" +
            "SELECT device_id " +
            "FROM device_tag_relation " +
            "WHERE tag_id IN " +
            "<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
            "#{tagId}" +
            "</foreach>" +
            "GROUP BY device_id " +
            "HAVING COUNT(DISTINCT tag_id) = #{tagIds.size}" +
            "</script>")
    List<Long> findDevicesByAllTags(@Param("tagIds") List<Long> tagIds);

    /**
     * 根据多个标签查询设备（并集）
     *
     * @param tagIds 标签ID列表
     * @return 设备ID列表
     */
    @Select("<script>" +
            "SELECT DISTINCT device_id " +
            "FROM device_tag_relation " +
            "WHERE tag_id IN " +
            "<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
            "#{tagId}" +
            "</foreach>" +
            "</script>")
    List<Long> findDevicesByAnyTags(@Param("tagIds") List<Long> tagIds);

    /**
     * 检查设备是否已有指定标签
     *
     * @param deviceId 设备ID
     * @param tagId 标签ID
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM device_tag_relation WHERE device_id = #{deviceId} AND tag_id = #{tagId}")
    int checkDeviceTagExists(@Param("deviceId") Long deviceId, @Param("tagId") Long tagId);

    /**
     * 获取设备的标签ID列表
     *
     * @param deviceId 设备ID
     * @return 标签ID列表
     */
    @Select("SELECT tag_id FROM device_tag_relation WHERE device_id = #{deviceId}")
    List<Long> selectTagIdsByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 获取使用某个标签的设备数量
     *
     * @param tagId 标签ID
     * @return 设备数量
     */
    @Select("SELECT COUNT(*) FROM device_tag_relation WHERE tag_id = #{tagId}")
    int countDevicesByTagId(@Param("tagId") Long tagId);

    /**
     * 根据标签ID删除所有相关的设备标签关联
     *
     * @param tagId 标签ID
     * @return 删除数量
     */
    @Delete("DELETE FROM device_tag_relation WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);

    /**
     * 根据标签ID获取设备标签关联列表（用于TagManagementServiceImpl兼容）
     *
     * @param tagId 标签ID
     * @return 设备标签关联列表
     */
    @Select("SELECT " +
            "dtr.id, dtr.device_id, dtr.tag_id, dtr.created_by, dtr.create_time " +
            "FROM device_tag_relation dtr " +
            "WHERE dtr.tag_id = #{tagId}")
    List<DeviceTagRelation> selectByTagId(@Param("tagId") Long tagId);
} 