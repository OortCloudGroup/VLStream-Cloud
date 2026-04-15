package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.VideoAggregation;

import java.util.List;
import java.util.Map;

/**
 * 视频汇聚配置Service接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface VideoAggregationService extends IService<VideoAggregation> {

    /**
     * 分页查询视频汇聚配置
     *
     * @param page 分页对象
     * @param aggregationName 汇聚名称
     * @param aggregationType 汇聚类型
     * @param status 状态
     * @return 分页结果
     */
    IPage<VideoAggregation> getAggregationPage(Page<VideoAggregation> page,
                                               String aggregationName,
                                               Integer aggregationType,
                                               Integer status);

    /**
     * 启动视频汇聚
     *
     * @param id 汇聚配置ID
     * @return 是否成功
     */
    boolean startAggregation(Long id);

    /**
     * 停止视频汇聚
     *
     * @param id 汇聚配置ID
     * @return 是否成功
     */
    boolean stopAggregation(Long id);

    /**
     * 重启视频汇聚
     *
     * @param id 汇聚配置ID
     * @return 是否成功
     */
    boolean restartAggregation(Long id);

    /**
     * 切换画面
     *
     * @param id 汇聚配置ID
     * @param targetStreamId 目标流ID
     * @return 是否成功
     */
    boolean switchStream(Long id, Long targetStreamId);

    /**
     * 获取汇聚状态
     *
     * @param id 汇聚配置ID
     * @return 状态信息
     */
    Map<String, Object> getAggregationStatus(Long id);

    /**
     * 获取状态统计
     *
     * @return 状态统计
     */
    Map<String, Object> getStatusStatistics();

    /**
     * 批量启动汇聚
     *
     * @param ids 汇聚配置ID列表
     * @return 成功启动的数量
     */
    int batchStart(List<Long> ids);

    /**
     * 批量停止汇聚
     *
     * @param ids 汇聚配置ID列表
     * @return 成功停止的数量
     */
    int batchStop(List<Long> ids);

    /**
     * 检查汇聚配置是否有效
     *
     * @param videoAggregation 汇聚配置
     * @return 检查结果
     */
    Map<String, Object> validateConfig(VideoAggregation videoAggregation);

    /**
     * 获取运行中的汇聚列表
     *
     * @return 运行中的汇聚列表
     */
    List<VideoAggregation> getRunningAggregations();
} 