package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springblade.vlstream.excel.VlsDeviceTagRelationExcel;
import org.springblade.vlstream.pojo.dto.DeviceTagRelationDTO;
import org.springblade.vlstream.pojo.entity.DeviceTagRelation;
import org.springblade.vlstream.pojo.vo.DeviceTagRelationVO;

import java.util.List;
import java.util.Map;

/**
 * Device Tag Association Table Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsDeviceTagRelationMapper extends BaseMapper<DeviceTagRelation> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsDeviceTagRelation query parameters
	 * @return List<VlsDeviceTagRelationVO>
	 */
	List<DeviceTagRelationVO> selectVlsDeviceTagRelationPage(IPage page, DeviceTagRelationVO vlsDeviceTagRelation);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsDeviceTagRelationExcel>
	 */
	List<VlsDeviceTagRelationExcel> exportVlsDeviceTagRelation(@Param("ew") Wrapper<DeviceTagRelation> queryWrapper);

	/**
	 * Get tag info by device ID
	 *
	 * @param deviceId device ID
	 * @return label information list
	 */
	@Select("SELECT " +
		"dtr.id, dtr.device_id, dtr.tag_id, dtr.create_user, dtr.create_time, " +
		"tm.tag_name, tm.category_type, tm.tag_color, tm.tag_icon, tm.level, tm.parent_id " +
		"FROM vls_device_tag_relation dtr " +
		"INNER JOIN vls_tag_management tm ON dtr.tag_id = tm.id " +
		"WHERE dtr.device_id = #{deviceId} AND tm.is_deleted = 0 AND tm.is_active = 1 " +
		"ORDER BY tm.category_type, tm.sort_order")
	List<DeviceTagRelationDTO> selectTagsByDeviceId(@Param("deviceId") Long deviceId);

	/**
	 * Get device list by tag ID
	 *
	 * @param tagId label ID
	 * @return device tag relation list
	 */
	@Select("SELECT " +
		"dtr.id, dtr.device_id, dtr.tag_id, dtr.create_user, dtr.create_time, " +
		"di.device_name, di.device_id as device_code, di.status " +
		"FROM vls_device_tag_relation dtr " +
		"INNER JOIN vls_device_info di ON dtr.device_id = di.id " +
		"WHERE dtr.tag_id = #{tagId} AND di.is_deleted = 0 " +
		"ORDER BY di.device_name")
	List<Map<String, Object>> selectDevicesByTagId(@Param("tagId") Long tagId);

	/**
	 * Batch add device label associations
	 *
	 * @param deviceId device ID
	 * @param tagIds label ID list
	 * @param createdBy creator
	 * @return inserted quantity
	 */
	@Insert("<script>" +
		"INSERT INTO vls_device_tag_relation (device_id, tag_id, create_user) VALUES " +
		"<foreach collection='tagIds' item='tagId' separator=','>" +
		"(#{deviceId}, #{tagId}, #{createdBy})" +
		"</foreach>" +
		"</script>")
	int batchInsertDeviceTags(@Param("deviceId") Long deviceId,
							  @Param("tagIds") List<Long> tagIds,
							  @Param("createdBy") String createdBy);

	/**
	 * Delete all tag associations of the device
	 *
	 * @param deviceId device ID
	 * @return deleted quantity
	 */
	@Delete("DELETE FROM vls_device_tag_relation WHERE device_id = #{deviceId}")
	int deleteByDeviceId(@Param("deviceId") Long deviceId);

	/**
	 * Delete specified tag associations of the device
	 *
	 * @param deviceId device ID
	 * @param tagIds label ID list
	 * @return deleted quantity
	 */
	@Delete("<script>" +
		"DELETE FROM vls_device_tag_relation " +
		"WHERE device_id = #{deviceId} AND tag_id IN " +
		"<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
		"#{tagId}" +
		"</foreach>" +
		"</script>")
	int deleteDeviceTagsBatch(@Param("deviceId") Long deviceId, @Param("tagIds") List<Long> tagIds);

	/**
	 * Get device label statistics
	 *
	 * @return statistics
	 */
	@Select("SELECT " +
		"device_id, " +
		"COUNT(tag_id) as tag_count, " +
		"COUNT(CASE WHEN tm.category_type = 'own' THEN 1 END) as own_tag_count, " +
		"COUNT(CASE WHEN tm.category_type = 'public' THEN 1 END) as public_tag_count " +
		"FROM vls_device_tag_relation dtr " +
		"INNER JOIN vls_tag_management tm ON dtr.tag_id = tm.id " +
		"WHERE tm.is_deleted = 0 AND tm.is_active = 1 " +
		"GROUP BY device_id")
	List<Map<String, Object>> getDeviceTagStatistics();

	/**
	 * Get label usage statistics
	 *
	 * @return label usage statistics
	 */
	@Select("SELECT " +
		"tm.id as tag_id, " +
		"tm.tag_name, " +
		"tm.category_type, " +
		"tm.level, " +
		"tm.tag_color, " +
		"COUNT(dtr.device_id) as device_count " +
		"FROM vls_tag_management tm " +
		"LEFT JOIN vls_device_tag_relation dtr ON tm.id = dtr.tag_id " +
		"WHERE tm.is_deleted = 0 AND tm.is_active = 1 " +
		"GROUP BY tm.id, tm.tag_name, tm.category_type, tm.level, tm.tag_color " +
		"ORDER BY tm.category_type, tm.level, device_count DESC")
	List<Map<String, Object>> getTagUsageStatistics();

	/**
	 * Query devices by multiple labels (intersection)
	 *
	 * @param tagIds label ID list
	 * @return device ID list
	 */
	@Select("<script>" +
		"SELECT device_id " +
		"FROM vls_device_tag_relation " +
		"WHERE tag_id IN " +
		"<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
		"#{tagId}" +
		"</foreach>" +
		"GROUP BY device_id " +
		"HAVING COUNT(DISTINCT tag_id) = #{tagIds.size}" +
		"</script>")
	List<Long> findDevicesByAllTags(@Param("tagIds") List<Long> tagIds);

	/**
	 * Query devices by multiple labels (union)
	 *
	 * @param tagIds label ID list
	 * @return device ID list
	 */
	@Select("<script>" +
		"SELECT DISTINCT device_id " +
		"FROM vls_device_tag_relation " +
		"WHERE tag_id IN " +
		"<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
		"#{tagId}" +
		"</foreach>" +
		"</script>")
	List<Long> findDevicesByAnyTags(@Param("tagIds") List<Long> tagIds);

	/**
	 * Check if device already has the specified tag
	 *
	 * @param deviceId device ID
	 * @param tagId label ID
	 * @return quantity
	 */
	@Select("SELECT COUNT(*) FROM vls_device_tag_relation WHERE device_id = #{deviceId} AND tag_id = #{tagId}")
	int checkDeviceTagExists(@Param("deviceId") Long deviceId, @Param("tagId") Long tagId);

	/**
	 * Get label ID list of device
	 *
	 * @param deviceId device ID
	 * @return label ID list
	 */
	@Select("SELECT tag_id FROM vls_device_tag_relation WHERE device_id = #{deviceId}")
	List<Long> selectTagIdsByDeviceId(@Param("deviceId") Long deviceId);

	/**
	 * Get device count using a specific label
	 *
	 * @param tagId label ID
	 * @return device quantity
	 */
	@Select("SELECT COUNT(*) FROM vls_device_tag_relation WHERE tag_id = #{tagId}")
	int countDevicesByTagId(@Param("tagId") Long tagId);

	/**
	 * Delete all related device tag associations by tag ID
	 *
	 * @param tagId label ID
	 * @return deleted quantity
	 */
	@Delete("DELETE FROM vls_device_tag_relation WHERE tag_id = #{tagId}")
	int deleteByTagId(@Param("tagId") Long tagId);

	/**
	 * Get device tag association list by tag ID (used for TagManagementServiceImpl compatibility)
	 *
	 * @param tagId label ID
	 * @return device tag relation list
	 */
	@Select("SELECT " +
		"dtr.id, dtr.device_id, dtr.tag_id, dtr.create_user, dtr.create_time " +
		"FROM vls_device_tag_relation dtr " +
		"WHERE dtr.tag_id = #{tagId}")
	List<DeviceTagRelation> selectByTagId(@Param("tagId") Long tagId);

}
