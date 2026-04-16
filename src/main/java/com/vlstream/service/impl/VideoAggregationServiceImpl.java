package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.VideoAggregation;
import com.vlstream.mapper.VideoAggregationMapper;
import com.vlstream.service.VideoAggregationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Video Aggregation Configuration Service Implementation
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class VideoAggregationServiceImpl extends ServiceImpl<VideoAggregationMapper, VideoAggregation> 
        implements VideoAggregationService {

    @Override
    public IPage<VideoAggregation> getAggregationPage(Page<VideoAggregation> page,
                                                      String aggregationName,
                                                      Integer aggregationType,
                                                      Integer status) {
        return baseMapper.selectAggregationPage(page, aggregationName, aggregationType, status);
    }

    @Override
    @Transactional
    public boolean startAggregation(Long id) {
        log.info("Starting video aggregation: {}", id);
        try {
            VideoAggregation aggregation = getById(id);
            if (aggregation == null) {
                log.error("Aggregation configuration does not exist: {}", id);
                return false;
            }

            if (aggregation.getStatus() == 1) {
                log.warn("Aggregation is already running: {}", id);
                return true;
            }

            // Implement actual start logic here
            // TODO: Call underlying video processing service to start aggregation

            // Update status
            aggregation.setStatus(1);
            aggregation.setUpdateTime(LocalDateTime.now());
            updateById(aggregation);

            log.info("Video aggregation started successfully: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Failed to start video aggregation: {}", id, e);
            // Update to error status
            VideoAggregation aggregation = new VideoAggregation();
            aggregation.setId(id);
            aggregation.setStatus(2);
            aggregation.setUpdateTime(LocalDateTime.now());
            updateById(aggregation);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean stopAggregation(Long id) {
        log.info("Stopping video aggregation: {}", id);
        try {
            VideoAggregation aggregation = getById(id);
            if (aggregation == null) {
                log.error("Aggregation configuration does not exist: {}", id);
                return false;
            }

            if (aggregation.getStatus() == 0) {
                log.warn("Aggregation is already stopped: {}", id);
                return true;
            }

            // Implement actual stop logic here
            // TODO: Call underlying video processing service to stop aggregation

            // Update status
            aggregation.setStatus(0);
            aggregation.setUpdateTime(LocalDateTime.now());
            updateById(aggregation);

            log.info("Video aggregation stopped successfully: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Failed to stop video aggregation: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean restartAggregation(Long id) {
        log.info("Restarting video aggregation: {}", id);
        return stopAggregation(id) && startAggregation(id);
    }

    @Override
    public boolean switchStream(Long id, Long targetStreamId) {
        log.info("Switching stream: aggregation ID={}, target stream ID={}", id, targetStreamId);
        try {
            VideoAggregation aggregation = getById(id);
            if (aggregation == null || aggregation.getStatus() != 1) {
                log.error("Aggregation configuration does not exist or is not running: {}", id);
                return false;
            }

            // Implement actual stream switching logic here
            // TODO: Call underlying video processing service to switch stream

            log.info("Stream switched successfully: aggregation ID={}, target stream ID={}", id, targetStreamId);
            return true;
        } catch (Exception e) {
            log.error("Failed to switch stream: aggregation ID={}, target stream ID={}", id, targetStreamId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getAggregationStatus(Long id) {
        VideoAggregation aggregation = getById(id);
        Map<String, Object> result = new HashMap<>();
        
        if (aggregation == null) {
            result.put("exists", false);
            return result;
        }

        result.put("exists", true);
        result.put("id", aggregation.getId());
        result.put("name", aggregation.getAggregationName());
        result.put("status", aggregation.getStatus());
        result.put("type", aggregation.getAggregationType());
        result.put("layout", aggregation.getLayout());
        result.put("outputUrl", aggregation.getOutputStreamUrl());
        
        // 这里可以添加更多实时状态信息
        // TODO: 从底层服务获取实时状态
        result.put("cpuUsage", 0.0);
        result.put("memoryUsage", 0.0);
        result.put("bandwidth", 0L);
        
        return result;
    }

    @Override
    public Map<String, Object> getStatusStatistics() {
        List<VideoAggregationMapper.StatusStatistics> statistics = baseMapper.getStatusStatistics();
        Map<String, Object> result = new HashMap<>();
        
        long total = 0;
        long running = 0;
        long stopped = 0;
        long error = 0;
        
        for (VideoAggregationMapper.StatusStatistics stat : statistics) {
            total += stat.getCount();
            switch (stat.getStatus()) {
                case 0: stopped = stat.getCount(); break;
                case 1: running = stat.getCount(); break;
                case 2: error = stat.getCount(); break;
            }
        }
        
        result.put("total", total);
        result.put("running", running);
        result.put("stopped", stopped);
        result.put("error", error);
        
        return result;
    }

    @Override
    @Transactional
    public int batchStart(List<Long> ids) {
        int successCount = 0;
        for (Long id : ids) {
            if (startAggregation(id)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    @Transactional
    public int batchStop(List<Long> ids) {
        int successCount = 0;
        for (Long id : ids) {
            if (stopAggregation(id)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public Map<String, Object> validateConfig(VideoAggregation videoAggregation) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        // Validate basic information
        if (videoAggregation.getAggregationName() == null || videoAggregation.getAggregationName().trim().isEmpty()) {
            errors.add("Aggregation name cannot be empty");
        }
        
        if (videoAggregation.getAggregationType() == null || 
            videoAggregation.getAggregationType() < 1 || videoAggregation.getAggregationType() > 4) {
            errors.add("Invalid aggregation type");
        }
        
        if (videoAggregation.getLayout() == null || videoAggregation.getLayout().trim().isEmpty()) {
            errors.add("Layout cannot be empty");
        }
        
        // Validate output parameters
        if (videoAggregation.getOutputResolution() == null || videoAggregation.getOutputResolution().trim().isEmpty()) {
            errors.add("Output resolution cannot be empty");
        }
        
        if (videoAggregation.getOutputFrameRate() == null || videoAggregation.getOutputFrameRate() <= 0) {
            errors.add("Output frame rate must be greater than 0");
        }
        
        if (videoAggregation.getOutputBitRate() == null || videoAggregation.getOutputBitRate() <= 0) {
            errors.add("Output bitrate must be greater than 0");
        }
        
        // Validate source stream configuration
        if (videoAggregation.getSourceStreamIds() == null || videoAggregation.getSourceStreamIds().trim().isEmpty()) {
            errors.add("Source stream ID list cannot be empty");
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        
        return result;
    }

    @Override
    public List<VideoAggregation> getRunningAggregations() {
        return baseMapper.selectByStatus(1);
    }
} 