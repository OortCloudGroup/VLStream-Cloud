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
 * Video Aggregation Configuration Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface VideoAggregationMapper extends BaseMapper<VideoAggregation> {

    /**
     * Paginated query video aggregation configuration
     *
     * @param page Pagination object
     * @param aggregationName Aggregation name
     * @param aggregationType Aggregation type
     * @param status Status
     * @return Pagination result
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
     * Query aggregation configuration list by status
     *
     * @param status Status
     * @return Aggregation configuration list
     */
    @Select("SELECT * FROM vls_video_aggregation WHERE status = #{status} AND deleted = 0")
    List<VideoAggregation> selectByStatus(@Param("status") Integer status);

    /**
     * Get status statistics
     *
     * @return Status statistics result
     */
    @Select("SELECT status, COUNT(*) as count FROM vls_video_aggregation WHERE deleted = 0 GROUP BY status")
    List<StatusStatistics> getStatusStatistics();

    /**
     * Status statistics inner class
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