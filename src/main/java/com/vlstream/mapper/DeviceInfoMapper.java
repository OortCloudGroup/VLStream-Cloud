package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.DeviceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Device Information Data Access Layer Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapper<DeviceInfo> {

    /**
     * Paginated query device information
     *
     * @param page       Pagination object
     * @param deviceName Device name or device ID
     * @param tag        Device tag (actually corresponds to device_type field)
     * @param status     Device status
     * @return Device information pagination list
     */
    @Select("<script>" +
            "SELECT * FROM device_info " +
            "WHERE deleted = 0 " +
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
     * Query device information by device ID
     *
     * @param deviceId Device ID
     * @return Device information
     */
    @Select("SELECT * FROM device_info WHERE device_id = #{deviceId} AND deleted = 0")
    DeviceInfo selectByDeviceId(@Param("deviceId") String deviceId);

    /**
     * Query device list by status
     *
     * @param status Device status
     * @return Device list
     */
    @Select("SELECT * FROM device_info WHERE status = #{status} AND deleted = 0")
    List<DeviceInfo> selectByStatus(@Param("status") String status);

    /**
     * Query device list by device type
     *
     * @param deviceType Device type
     * @return Device list
     */
    @Select("SELECT * FROM device_info WHERE device_type = #{deviceType} AND deleted = 0")
    List<DeviceInfo> selectByDeviceType(@Param("deviceType") String deviceType);

    /**
     * Query device list by tag (actually queries device_type field)
     *
     * @param tag Tag
     * @return Device list
     */
    @Select("SELECT * FROM device_info WHERE device_type = #{tag} AND deleted = 0")
    List<DeviceInfo> selectByTag(@Param("tag") String tag);

    /**
     * Batch update device status
     *
     * @param deviceIds Device ID list
     * @param status    Status
     * @return Update count
     */
    @Update("<script>" +
            "UPDATE device_info SET status = #{status}, update_time = NOW() " +
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
     * @return Statistics result
     */
    @Select("SELECT status, COUNT(*) as count FROM device_info WHERE deleted = 0 GROUP BY status")
    List<StatusStatistics> getStatusStatistics();

    /**
     * Get device type statistics
     *
     * @return Statistics result
     */
    @Select("SELECT device_type as type, COUNT(*) as count FROM device_info WHERE deleted = 0 GROUP BY device_type")
    List<TypeStatistics> getTypeStatistics();

    /**
     * Get device brand statistics
     *
     * @return Statistics result
     */
    @Select("SELECT brand, COUNT(*) as count FROM device_info WHERE deleted = 0 GROUP BY brand")
    List<BrandStatistics> getBrandStatistics();

    /**
     * Get all device type list (for tag list)
     *
     * @return Device type list
     */
    @Select("SELECT DISTINCT device_type FROM device_info WHERE device_type IS NOT NULL AND device_type != '' AND deleted = 0")
    List<String> getAllTags();

    /**
     * Get all device brand list
     *
     * @return Brand list
     */
    @Select("SELECT DISTINCT brand FROM device_info WHERE brand IS NOT NULL AND brand != '' AND deleted = 0")
    List<String> getAllBrands();

    /**
     * Query devices by IP address
     *
     * @param ipAddress IP address
     * @return Device list
     */
    @Select("SELECT * FROM device_info WHERE ip_address = #{ipAddress} AND deleted = 0")
    List<DeviceInfo> selectByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * Query devices by location
     *
     * @param position Location
     * @return Device list
     */
    @Select("SELECT * FROM device_info WHERE position LIKE CONCAT('%', #{position}, '%') AND deleted = 0")
    List<DeviceInfo> selectByPosition(@Param("position") String position);

    /**
     * Check if device ID exists
     *
     * @param deviceId Device ID
     * @return Count
     */
    @Select("SELECT COUNT(*) FROM device_info WHERE device_id = #{deviceId} AND deleted = 0")
    int countByDeviceId(@Param("deviceId") String deviceId);

    /**
     * Status statistics inner class
     */
    class StatusStatistics {
        private String status;
        private Long count;
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * Type statistics inner class
     */
    class TypeStatistics {
        private String type;
        private Long count;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * Brand statistics inner class
     */
    class BrandStatistics {
        private String brand;
        private Long count;
        
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * Device statistics inner class
     */
    class DeviceStatistics {
        private Long totalCount;
        private Long onlineCount;
        private Long offlineCount;
        private Long faultCount;

        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getOnlineCount() { return onlineCount; }
        public void setOnlineCount(Long onlineCount) { this.onlineCount = onlineCount; }
        public Long getOfflineCount() { return offlineCount; }
        public void setOfflineCount(Long offlineCount) { this.offlineCount = offlineCount; }
        public Long getFaultCount() { return faultCount; }
        public void setFaultCount(Long faultCount) { this.faultCount = faultCount; }
    }
} 