package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.VideoAggregation;

import java.util.List;
import java.util.Map;

/**
 * Video aggregation configuration service interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface VideoAggregationService extends IService<VideoAggregation> {

    /**
     * Query video aggregation configuration with pagination
     *
     * @param page Pagination object
     * @param aggregationName Aggregation name
     * @param aggregationType Aggregation type
     * @param status Status
     * @return Pagination result
     */
    IPage<VideoAggregation> getAggregationPage(Page<VideoAggregation> page,
                                               String aggregationName,
                                               Integer aggregationType,
                                               Integer status);

    /**
     * Start video aggregation
     *
     * @param id Aggregation configuration ID
     * @return Whether successful
     */
    boolean startAggregation(Long id);

    /**
     * Stop video aggregation
     *
     * @param id Aggregation configuration ID
     * @return Whether successful
     */
    boolean stopAggregation(Long id);

    /**
     * Restart video aggregation
     *
     * @param id Aggregation configuration ID
     * @return Whether successful
     */
    boolean restartAggregation(Long id);

    /**
     * Switch stream
     *
     * @param id Aggregation configuration ID
     * @param targetStreamId Target stream ID
     * @return Whether successful
     */
    boolean switchStream(Long id, Long targetStreamId);

    /**
     * Get aggregation status
     *
     * @param id Aggregation configuration ID
     * @return Status information
     */
    Map<String, Object> getAggregationStatus(Long id);

    /**
     * Get status statistics
     *
     * @return Status statistics
     */
    Map<String, Object> getStatusStatistics();

    /**
     * Batch start aggregation
     *
     * @param ids Aggregation configuration ID list
     * @return Number of successfully started
     */
    int batchStart(List<Long> ids);

    /**
     * Batch stop aggregation
     *
     * @param ids Aggregation configuration ID list
     * @return Number of successfully stopped
     */
    int batchStop(List<Long> ids);

    /**
     * Check if aggregation configuration is valid
     *
     * @param videoAggregation Aggregation configuration
     * @return Check result
     */
    Map<String, Object> validateConfig(VideoAggregation videoAggregation);

    /**
     * Get running aggregation list
     *
     * @return Running aggregation list
     */
    List<VideoAggregation> getRunningAggregations();
} 