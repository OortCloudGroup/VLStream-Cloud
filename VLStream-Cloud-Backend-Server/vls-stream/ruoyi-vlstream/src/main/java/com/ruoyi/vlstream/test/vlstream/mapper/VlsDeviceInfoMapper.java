/*
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
 * 设备信息表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsDeviceInfoMapper extends BaseMapper<DeviceInfo> {

	/**
	 * 自定义分页
	 *
	 * @param page          分页参数
	 * @param vlsDeviceInfo 查询参数
	 * @return List<VlsDeviceInfoVO>
	 */
	List<DeviceInfoVO> selectVlsDeviceInfoPage(IPage page, DeviceInfoVO vlsDeviceInfo);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsDeviceInfoExcel>
	 */
	List<VlsDeviceInfoExcel> exportVlsDeviceInfo(@Param("ew") Wrapper<DeviceInfo> queryWrapper);

	/**
	 * 分页查询设备信息
	 *
	 * @param page       分页对象
	 * @param deviceName 设备名称或设备ID
	 * @param tag        设备标签（实际对应device_type字段）
	 * @param status     设备状态
	 * @return 设备信息分页列表
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
	 * 根据设备编号查询设备信息
	 *
	 * @param deviceId 设备编号
	 * @return 设备信息
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
	DeviceInfo selectByDeviceId(@Param("deviceId") String deviceId);

	/**
	 * 根据状态查询设备列表
	 *
	 * @param status 设备状态
	 * @return 设备列表
	 */
	@Select("SELECT * FROM vls_device_info WHERE status = #{status} AND is_deleted = 0")
	List<DeviceInfo> selectByStatus(@Param("status") String status);

	/**
	 * 根据设备类型查询设备列表
	 *
	 * @param deviceType 设备类型
	 * @return 设备列表
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_type = #{deviceType} AND is_deleted = 0")
	List<DeviceInfo> selectByDeviceType(@Param("deviceType") String deviceType);

	/**
	 * 根据标签查询设备列表（实际查询device_type字段）
	 *
	 * @param tag 标签
	 * @return 设备列表
	 */
	@Select("SELECT * FROM vls_device_info WHERE device_type = #{tag} AND is_deleted = 0")
	List<DeviceInfo> selectByTag(@Param("tag") String tag);

	/**
	 * 批量更新设备状态
	 *
	 * @param deviceIds 设备ID列表
	 * @param status    状态
	 * @return 更新数量
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
	 * 获取设备状态统计
	 *
	 * @return 统计结果
	 */
	@Select("SELECT status, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY status")
	List<StatusStatistics> getStatusStatistics();

	/**
	 * 获取设备类型统计
	 *
	 * @return 统计结果
	 */
	@Select("SELECT device_type as type, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY device_type")
	List<TypeStatistics> getTypeStatistics();

	/**
	 * 获取设备品牌统计
	 *
	 * @return 统计结果
	 */
	@Select("SELECT brand, COUNT(*) as count FROM vls_device_info WHERE is_deleted = 0 GROUP BY brand")
	List<BrandStatistics> getBrandStatistics();

	/**
	 * 获取所有设备类型列表（用于标签列表）
	 *
	 * @return 设备类型列表
	 */
	@Select("SELECT DISTINCT device_type FROM vls_device_info WHERE device_type IS NOT NULL AND device_type != '' AND is_deleted = 0")
	List<String> getAllTags();

	/**
	 * 获取所有设备品牌列表
	 *
	 * @return 品牌列表
	 */
	@Select("SELECT DISTINCT brand FROM vls_device_info WHERE brand IS NOT NULL AND brand != '' AND is_deleted = 0")
	List<String> getAllBrands();

	/**
	 * 根据IP地址查询设备
	 *
	 * @param ipAddress IP地址
	 * @return 设备列表
	 */
	@Select("SELECT * FROM vls_device_info WHERE ip_address = #{ipAddress} AND is_deleted = 0")
	List<DeviceInfo> selectByIpAddress(@Param("ipAddress") String ipAddress);

	/**
	 * 根据位置查询设备
	 *
	 * @param position 位置
	 * @return 设备列表
	 */
	@Select("SELECT * FROM vls_device_info WHERE position LIKE CONCAT('%', #{position}, '%') AND is_deleted = 0")
	List<DeviceInfo> selectByPosition(@Param("position") String position);

	/**
	 * 检查设备编号是否存在
	 *
	 * @param deviceId 设备编号
	 * @return 数量
	 */
	@Select("SELECT COUNT(*) FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
	int countByDeviceId(@Param("deviceId") String deviceId);

	/**
	 * 状态统计内部类
	 */
	@Data
	class StatusStatistics {
		private String status;
		private Long count;
	}

	/**
	 * 类型统计内部类
	 */
	@Data
	class TypeStatistics {
		private String type;
		private Long count;
	}

	/**
	 * 品牌统计内部类
	 */
	@Data
	class BrandStatistics {
		private String brand;
		private Long count;
	}

	/**
	 * 设备统计信息内部类
	 */
	@Data
	class DeviceStatistics {
		private Long totalCount;
		private Long onlineCount;
		private Long offlineCount;
		private Long faultCount;
	}

}
