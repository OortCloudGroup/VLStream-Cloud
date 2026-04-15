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
 * 视频汇聚配置Service实现类
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
        log.info("启动视频汇聚: {}", id);
        try {
            VideoAggregation aggregation = getById(id);
            if (aggregation == null) {
                log.error("汇聚配置不存在: {}", id);
                return false;
            }

            if (aggregation.getStatus() == 1) {
                log.warn("汇聚已在运行中: {}", id);
                return true;
            }

            // 这里实现实际的启动逻辑
            // TODO: 调用底层视频处理服务启动汇聚

            // 更新状态
            aggregation.setStatus(1);
            aggregation.setUpdateTime(LocalDateTime.now());
            updateById(aggregation);

            log.info("视频汇聚启动成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("启动视频汇聚失败: {}", id, e);
            // 更新为异常状态
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
        log.info("停止视频汇聚: {}", id);
        try {
            VideoAggregation aggregation = getById(id);
            if (aggregation == null) {
                log.error("汇聚配置不存在: {}", id);
                return false;
            }

            if (aggregation.getStatus() == 0) {
                log.warn("汇聚已停止: {}", id);
                return true;
            }

            // 这里实现实际的停止逻辑
            // TODO: 调用底层视频处理服务停止汇聚

            // 更新状态
            aggregation.setStatus(0);
            aggregation.setUpdateTime(LocalDateTime.now());
            updateById(aggregation);

            log.info("视频汇聚停止成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("停止视频汇聚失败: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean restartAggregation(Long id) {
        log.info("重启视频汇聚: {}", id);
        return stopAggregation(id) && startAggregation(id);
    }

    @Override
    public boolean switchStream(Long id, Long targetStreamId) {
        log.info("切换画面: 汇聚ID={}, 目标流ID={}", id, targetStreamId);
        try {
            VideoAggregation aggregation = getById(id);
            if (aggregation == null || aggregation.getStatus() != 1) {
                log.error("汇聚配置不存在或未运行: {}", id);
                return false;
            }

            // 这里实现实际的画面切换逻辑
            // TODO: 调用底层视频处理服务切换画面

            log.info("画面切换成功: 汇聚ID={}, 目标流ID={}", id, targetStreamId);
            return true;
        } catch (Exception e) {
            log.error("画面切换失败: 汇聚ID={}, 目标流ID={}", id, targetStreamId, e);
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
        
        // 验证基本信息
        if (videoAggregation.getAggregationName() == null || videoAggregation.getAggregationName().trim().isEmpty()) {
            errors.add("汇聚名称不能为空");
        }
        
        if (videoAggregation.getAggregationType() == null || 
            videoAggregation.getAggregationType() < 1 || videoAggregation.getAggregationType() > 4) {
            errors.add("汇聚类型无效");
        }
        
        if (videoAggregation.getLayout() == null || videoAggregation.getLayout().trim().isEmpty()) {
            errors.add("画面布局不能为空");
        }
        
        // 验证输出参数
        if (videoAggregation.getOutputResolution() == null || videoAggregation.getOutputResolution().trim().isEmpty()) {
            errors.add("输出分辨率不能为空");
        }
        
        if (videoAggregation.getOutputFrameRate() == null || videoAggregation.getOutputFrameRate() <= 0) {
            errors.add("输出帧率必须大于0");
        }
        
        if (videoAggregation.getOutputBitRate() == null || videoAggregation.getOutputBitRate() <= 0) {
            errors.add("输出比特率必须大于0");
        }
        
        // 验证源流配置
        if (videoAggregation.getSourceStreamIds() == null || videoAggregation.getSourceStreamIds().trim().isEmpty()) {
            errors.add("源流ID列表不能为空");
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