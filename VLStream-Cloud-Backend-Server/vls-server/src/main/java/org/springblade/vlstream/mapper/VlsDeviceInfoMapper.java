package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.vlstream.excel.VlsDeviceInfoExcel;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.vo.DeviceInfoVO;

import java.util.List;

/**
 * Device Information Table Mapper Interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsDeviceInfoMapper extends BaseMapper<DeviceInfo> {

	/**
	 * Custom paging
	 *
	 * @param page          pagination parameters
	 * @param vlsDeviceInfo query parameters
	 * @return List<VlsDeviceInfoVO>
	 */
	List<DeviceInfoVO> selectVlsDeviceInfoPage(IPage page, DeviceInfoVO vlsDeviceInfo);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsDeviceInfoExcel>
	 */
	List<VlsDeviceInfoExcel> exportVlsDeviceInfo(@Param("ew") Wrapper<DeviceInfo> queryWrapper);

	/**
	 * Paginated query for device information
	 *
	 * @param page       pagination object
	 * @param deviceName device name or device ID
	 * @param tag        device tag (actually corresponds to the device_type field)
	 * @param status     device status
	 * @return paginated list of device information
	 */
	@Select("<script>" +
		"SELECT * FROM vls_device_info " +
		"WHERE is_deleted = 0 " +
		"<if test='deviceName != null and deviceName != \"\"'>" +
		"AND (device_name LIKE CONCAT('%', #{deviceName}, '%') OR device_id LIKE CONCAT('%', #{deviceName}, '%')) " +
		"</if>" +
		"<if test='tag != null and tag != \"\"'>" +
		"AND device_type = #{tag} " +
		"</if>" +
		"<if test='status != null and status != \"\"'>" +
		"AND status = #{status} " +
		"</if>" +
		"ORDER BY id DESC" +
		"</script>")
	IPage<DeviceInfo> selectDevicePage(Page<DeviceInfo> page,
									   @Param("deviceName") String deviceName,
									   @Param("tag") String tag,
									   @Param("status") String status);

	/**
	 * Query device info by device number
	 *
	 * @param deviceId device code
	 * @return device information
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
	DeviceInfo selectByDeviceId(@Param("deviceId") String deviceId);

	/**
	 * Query device list by status
	 *
	 * @param status device status
	 * @return device list
	 */
	@Select("SELECT * FROM vls_device_info WHERE status = #{status} AND is_deleted = 0")
	List<DeviceInfo> selectByStatus(@Param("status") String status);

	/**
	 * Query device list by device type
	 *
	 * @param deviceType device type
	 * @return device list
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_type = #{deviceType} AND is_deleted = 0")
	List<DeviceInfo> selectByDeviceType(@Param("deviceType") String deviceType);

	/**
	 * Query device list by tag (actually queries the device_type field)
	 *
	 * @param tag label
	 * @return device list
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_type = #{tag} AND is_deleted = 0")
	List<DeviceInfo> selectByTag(@Param("tag") String tag);

	/**
	 * Batch update device status
	 *
	 * @param deviceIds device ID list
	 * @param status    status
	 * @return updated quantity
	 */
	@Update("<script>" +
		"UPDATE vls_device_info SET status = #{status}, update_time = NOW() " +
		"WHERE id IN " +
		"<foreach collection='deviceIds' item='id' open='(' separator=',' close=')'>" +
		"#{id}" +
		"</foreach>" +
		"</script>")
	int updateStatusBatch(@Param("deviceIds") List<Long> deviceIds,
						  @Param("status") String status);

	/**
	 * Get device status statistics
	 *
	 * @return statistical results
	 */
	@Select("SELECT status, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY status")
	List<StatusStatistics> getStatusStatistics();

	/**
	 * Get device type statistics
	 *
	 * @return statistical results
	 */
	@Select("SELECT device_type as type, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY device_type")
	List<TypeStatistics> getTypeStatistics();

	/**
	 * Get device brand statistics
	 *
	 * @return statistical results
	 */
	@Select("SELECT brand, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY brand")
	List<BrandStatistics> getBrandStatistics();

	/**
	 * Get all device type list (used for label list)
	 *
	 * @return device type list
	 */
	@Select("SELECT DISTINCT device_type FROM vls_device_info WHERE device_type IS NOT NULL AND device_type != '' AND is_deleted = 0")
	List<String> getAllTags();

	/**
	 * Get all device brand list
	 *
	 * @return brand list
	 */
	@Select("SELECT DISTINCT brand FROM vls_device_info WHERE brand IS NOT NULL AND brand != '' AND is_deleted = 0")
	List<String> getAllBrands();

	/**
	 * Query device by IP address
	 *
	 * @param ipAddress IP address
	 * @return device list
	 */
	@Select("SELECT * FROM vls_device_info WHERE ip_address = #{ipAddress} AND is_deleted = 0")
	List<DeviceInfo> selectByIpAddress(@Param("ipAddress") String ipAddress);

	/**
	 * Query device by location
	 *
	 * @param position position
	 * @return device list
	 */
	@Select("SELECT * FROM vls_device_info WHERE position LIKE CONCAT('%', #{position}, '%') AND is_deleted = 0")
	List<DeviceInfo> selectByPosition(@Param("position") String position);

	/**
	 * Check if device number exists
	 *
	 * @param deviceId device code
	 * @return quantity
	 */
	@Select("SELECT COUNT(*) FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
	int countByDeviceId(@Param("deviceId") String deviceId);

	/**
	 * Inner class for status statistics
	 */
	@Data
	class StatusStatistics {
		private String status;
		private Long count;
	}

	/**
	 * Type statistics inner class
	 */
	@Data
	class TypeStatistics {
		private String type;
		private Long count;
	}

	/**
	 * Brand statistics internal class
	 */
	@Data
	class BrandStatistics {
		private String brand;
		private Long count;
	}

	/**
	 * Device statistics inner class
	 */
	@Data
	class DeviceStatistics {
		private Long totalCount;
		private Long onlineCount;
		private Long offlineCount;
		private Long faultCount;
	}

}
