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
 * Device Tag Relation Data Access Layer Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface DeviceTagRelationMapper extends BaseMapper<DeviceTagRelation> {

    /**
     * Get tag information by device ID
     *
     * @param deviceId Device ID
     * @return Tag information list
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
     * Get device list by tag ID
     *
     * @param tagId Tag ID
     * @return Device tag relation list
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
     * Batch add device tag relations
     *
     * @param deviceId Device ID
     * @param tagIds Tag ID list
     * @param createdBy Creator
     * @return Insert count
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
     * Delete all tag relations for a device
     *
     * @param deviceId Device ID
     * @return Delete count
     */
    @Delete("DELETE FROM device_tag_relation WHERE device_id = #{deviceId}")
    int deleteByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * Delete specified tag relations for a device
     *
     * @param deviceId Device ID
     * @param tagIds Tag ID list
     * @return Delete count
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
     * Get device tag statistics
     *
     * @return Statistics
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
     * Get tag usage statistics
     *
     * @return Tag usage statistics
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
     * Query devices by multiple tags (intersection)
     *
     * @param tagIds Tag ID list
     * @return Device ID list
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
     * Query devices by multiple tags (union)
     *
     * @param tagIds Tag ID list
     * @return Device ID list
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
     * Check if device already has specified tag
     *
     * @param deviceId Device ID
     * @param tagId Tag ID
     * @return Count
     */
    @Select("SELECT COUNT(*) FROM device_tag_relation WHERE device_id = #{deviceId} AND tag_id = #{tagId}")
    int checkDeviceTagExists(@Param("deviceId") Long deviceId, @Param("tagId") Long tagId);

    /**
     * Get tag ID list for a device
     *
     * @param deviceId Device ID
     * @return Tag ID list
     */
    @Select("SELECT tag_id FROM device_tag_relation WHERE device_id = #{deviceId}")
    List<Long> selectTagIdsByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * Get device count for a tag
     *
     * @param tagId Tag ID
     * @return Device count
     */
    @Select("SELECT COUNT(*) FROM device_tag_relation WHERE tag_id = #{tagId}")
    int countDevicesByTagId(@Param("tagId") Long tagId);

    /**
     * Delete all device tag relations for a tag ID
     *
     * @param tagId Tag ID
     * @return Delete count
     */
    @Delete("DELETE FROM device_tag_relation WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);

    /**
     * Get device tag relation list by tag ID (for TagManagementServiceImpl compatibility)
     *
     * @param tagId Tag ID
     * @return Device tag relation list
     */
    @Select("SELECT " +
            "dtr.id, dtr.device_id, dtr.tag_id, dtr.created_by, dtr.create_time " +
            "FROM device_tag_relation dtr " +
            "WHERE dtr.tag_id = #{tagId}")
    List<DeviceTagRelation> selectByTagId(@Param("tagId") Long tagId);
} 