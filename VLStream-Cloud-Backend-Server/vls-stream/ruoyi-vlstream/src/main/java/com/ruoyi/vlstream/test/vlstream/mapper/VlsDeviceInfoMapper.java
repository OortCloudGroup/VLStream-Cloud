/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceInfoExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceInfoVO;

import java.util.List;

/**
 * deviceinfo Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsDeviceInfoMapper extends BaseMapper<DeviceInfo> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsDeviceInfo Query parameter
	 * @return List<VlsDeviceInfoVO>
	 */
	List<DeviceInfoVO> selectVlsDeviceInfoPage(IPage page, DeviceInfoVO vlsDeviceInfo);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsDeviceInfoExcel>
	 */
	List<VlsDeviceInfoExcel> exportVlsDeviceInfo(@Param("ew") Wrapper<DeviceInfo> queryWrapper);

	/**
	 * Query deviceinfo
	 *
	 * @param page object
	 * @param deviceName device deviceID
	 * @param tag device ( device_typefield)
	 * @param status device
	 * @return deviceinfo
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
	 * device Query deviceinfo
	 *
	 * @param deviceId device
	 * @return deviceinfo
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
	DeviceInfo selectByDeviceId(@Param("deviceId") String deviceId);

	/**
	 * Query device list
	 *
	 * @param status device
	 * @return device
	 */
	@Select("SELECT * FROM vls_device_info WHERE status = #{status} AND is_deleted = 0")
	List<DeviceInfo> selectByStatus(@Param("status") String status);

	/**
	 * device Query device list
	 *
	 * @param deviceType device
	 * @return device
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_type = #{deviceType} AND is_deleted = 0")
	List<DeviceInfo> selectByDeviceType(@Param("deviceType") String deviceType);

	/**
	 * Query device list ( Query device_typefield)
	 *
	 * @param tag
	 * @return device
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_type = #{tag} AND is_deleted = 0")
	List<DeviceInfo> selectByTag(@Param("tag") String tag);

	/**
	 * new device
	 *
	 * @param deviceIds deviceID
	 * @param status
	 * @return new
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
	 * Get device
	 *
	 * @return
	 */
	@Select("SELECT status, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY status")
	List<StatusStatistics> getStatusStatistics();

	/**
	 * Get device
	 *
	 * @return
	 */
	@Select("SELECT device_type as type, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY device_type")
	List<TypeStatistics> getTypeStatistics();

	/**
	 * Get device
	 *
	 * @return
	 */
	@Select("SELECT brand, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY brand")
	List<BrandStatistics> getBrandStatistics();

	/**
	 * Get all device ( )
	 *
	 * @return device
	 */
	@Select("SELECT DISTINCT device_type FROM vls_device_info WHERE device_type IS NOT NULL AND device_type != '' AND is_deleted = 0")
	List<String> getAllTags();

	/**
	 * Get all device
	 *
	 * @return
	 */
	@Select("SELECT DISTINCT brand FROM vls_device_info WHERE brand IS NOT NULL AND brand != '' AND is_deleted = 0")
	List<String> getAllBrands();

	/**
	 * IP Query device
	 *
	 * @param ipAddress IP
	 * @return device
	 */
	@Select("SELECT * FROM vls_device_info WHERE ip_address = #{ipAddress} AND is_deleted = 0")
	List<DeviceInfo> selectByIpAddress(@Param("ipAddress") String ipAddress);

	/**
	 * Query device
	 *
	 * @param position
	 * @return device
	 */
	@Select("SELECT * FROM vls_device_info WHERE position LIKE CONCAT('%', #{position}, '%') AND is_deleted = 0")
	List<DeviceInfo> selectByPosition(@Param("position") String position);

	/**
	 * device whether in
	 *
	 * @param deviceId device
	 * @return
	 */
	@Select("SELECT COUNT(*) FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
	int countByDeviceId(@Param("deviceId") String deviceId);

	/**
	 *
	 */
	@Data
	class StatusStatistics {
		private String status;
		private Long count;
	}

	/**
	 *
	 */
	@Data
	class TypeStatistics {
		private String type;
		private Long count;
	}

	/**
	 *
	 */
	@Data
	class BrandStatistics {
		private String brand;
		private Long count;
	}

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
