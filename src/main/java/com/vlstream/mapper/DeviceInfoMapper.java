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
 * 设备信息数据访问层接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapper<DeviceInfo> {

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
     * 根据设备编号查询设备信息
     *
     * @param deviceId 设备编号
     * @return 设备信息
     */
    @Select("SELECT * FROM device_info WHERE device_id = #{deviceId} AND deleted = 0")
    DeviceInfo selectByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 根据状态查询设备列表
     *
     * @param status 设备状态
     * @return 设备列表
     */
    @Select("SELECT * FROM device_info WHERE status = #{status} AND deleted = 0")
    List<DeviceInfo> selectByStatus(@Param("status") String status);

    /**
     * 根据设备类型查询设备列表
     *
     * @param deviceType 设备类型
     * @return 设备列表
     */
    @Select("SELECT * FROM device_info WHERE device_type = #{deviceType} AND deleted = 0")
    List<DeviceInfo> selectByDeviceType(@Param("deviceType") String deviceType);

    /**
     * 根据标签查询设备列表（实际查询device_type字段）
     *
     * @param tag 标签
     * @return 设备列表
     */
    @Select("SELECT * FROM device_info WHERE device_type = #{tag} AND deleted = 0")
    List<DeviceInfo> selectByTag(@Param("tag") String tag);

    /**
     * 批量更新设备状态
     *
     * @param deviceIds 设备ID列表
     * @param status    状态
     * @return 更新数量
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
     * 获取设备状态统计
     *
     * @return 统计结果
     */
    @Select("SELECT status, COUNT(*) as count FROM device_info WHERE deleted = 0 GROUP BY status")
    List<StatusStatistics> getStatusStatistics();

    /**
     * 获取设备类型统计
     *
     * @return 统计结果
     */
    @Select("SELECT device_type as type, COUNT(*) as count FROM device_info WHERE deleted = 0 GROUP BY device_type")
    List<TypeStatistics> getTypeStatistics();

    /**
     * 获取设备品牌统计
     *
     * @return 统计结果
     */
    @Select("SELECT brand, COUNT(*) as count FROM device_info WHERE deleted = 0 GROUP BY brand")
    List<BrandStatistics> getBrandStatistics();

    /**
     * 获取所有设备类型列表（用于标签列表）
     *
     * @return 设备类型列表
     */
    @Select("SELECT DISTINCT device_type FROM device_info WHERE device_type IS NOT NULL AND device_type != '' AND deleted = 0")
    List<String> getAllTags();

    /**
     * 获取所有设备品牌列表
     *
     * @return 品牌列表
     */
    @Select("SELECT DISTINCT brand FROM device_info WHERE brand IS NOT NULL AND brand != '' AND deleted = 0")
    List<String> getAllBrands();

    /**
     * 根据IP地址查询设备
     *
     * @param ipAddress IP地址
     * @return 设备列表
     */
    @Select("SELECT * FROM device_info WHERE ip_address = #{ipAddress} AND deleted = 0")
    List<DeviceInfo> selectByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * 根据位置查询设备
     *
     * @param position 位置
     * @return 设备列表
     */
    @Select("SELECT * FROM device_info WHERE position LIKE CONCAT('%', #{position}, '%') AND deleted = 0")
    List<DeviceInfo> selectByPosition(@Param("position") String position);

    /**
     * 检查设备编号是否存在
     *
     * @param deviceId 设备编号
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM device_info WHERE device_id = #{deviceId} AND deleted = 0")
    int countByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 状态统计内部类
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
     * 类型统计内部类
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
     * 品牌统计内部类
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
     * 设备统计信息内部类
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