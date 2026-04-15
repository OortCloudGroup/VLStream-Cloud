package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 算法实体类
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm")
public class Algorithm {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属算法仓库ID
     */
    @TableField("repository_id")
    private Long repositoryId;

    /**
     * 算法名称
     */
    @TableField("name")
    private String name;

    /**
     * 算法类型（人员检测类、人脸识别类、视频分析类等）
     */
    @TableField("category")
    private String category;

    /**
     * 算法类型名称
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 算法描述
     */
    @TableField("description")
    private String description;

    /**
     * 算法图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 算法版本
     */
    @TableField("version")
    private String version;

    /**
     * 模型文件路径
     */
    @TableField("model_file_path")
    private String modelFilePath;

    /**
     * 算法配置参数（JSON格式）
     */
    @TableField("config_params")
    private String configParams;

    /**
     * 输入格式（image、video等）
     */
    @TableField("input_format")
    private String inputFormat;

    /**
     * 输出格式（bbox、mask、keypoint等）
     */
    @TableField("output_format")
    private String outputFormat;

    /**
     * 准确率
     */
    @TableField("accuracy")
    private BigDecimal accuracy;

    /**
     * 处理速度（FPS）
     */
    @TableField("processing_speed")
    private Integer processingSpeed;

    /**
     * 内存使用量（MB）
     */
    @TableField("memory_usage")
    private Integer memoryUsage;

    /**
     * 是否需要GPU：0-否，1-是
     */
    @TableField("gpu_required")
    private Integer gpuRequired;

    /**
     * 部署状态：ready-就绪, deploying-部署中, deployed-已部署, failed-失败
     */
    @TableField("deploy_status")
    private String deployStatus;

    /**
     * 部署次数
     */
    @TableField("deploy_count")
    private Integer deployCount;

    /**
     * 最后部署时间
     */
    @TableField("last_deploy_time")
    private LocalDateTime lastDeployTime;

    /**
     * 创建人
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新人
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
} 