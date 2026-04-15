package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.entity.VideoAggregation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 视频汇聚配置Mapper接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface VideoAggregationMapper extends BaseMapper<VideoAggregation> {

    /**
     * 分页查询视频汇聚配置
     *
     * @param page 分页对象
     * @param aggregationName 汇聚名称
     * @param aggregationType 汇聚类型
     * @param status 状态
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT * FROM vls_video_aggregation " +
            "WHERE deleted = 0 " +
            "<if test='aggregationName != null and aggregationName != \"\"'>" +
            "AND aggregation_name LIKE CONCAT('%', #{aggregationName}, '%') " +
            "</if>" +
            "<if test='aggregationType != null'>" +
            "AND aggregation_type = #{aggregationType} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    IPage<VideoAggregation> selectAggregationPage(Page<VideoAggregation> page,
                                                  @Param("aggregationName") String aggregationName,
                                                  @Param("aggregationType") Integer aggregationType,
                                                  @Param("status") Integer status);

    /**
     * 根据状态查询汇聚配置列表
     *
     * @param status 状态
     * @return 汇聚配置列表
     */
    @Select("SELECT * FROM vls_video_aggregation WHERE status = #{status} AND deleted = 0")
    List<VideoAggregation> selectByStatus(@Param("status") Integer status);

    /**
     * 获取状态统计
     *
     * @return 状态统计结果
     */
    @Select("SELECT status, COUNT(*) as count FROM vls_video_aggregation WHERE deleted = 0 GROUP BY status")
    List<StatusStatistics> getStatusStatistics();

    /**
     * 状态统计内部类
     */
    class StatusStatistics {
        private Integer status;
        private Long count;
        
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
} 