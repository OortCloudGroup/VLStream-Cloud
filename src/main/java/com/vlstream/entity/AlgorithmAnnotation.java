package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 算法标注实体类
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_annotation")
public class AlgorithmAnnotation {

    /**
     * 标注ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标注名称
     */
    @TableField("annotation_name")
    private String annotationName;

    /**
     * 标注类型：object_detection-物体检测,image_classification-图像分类,instance_segmentation-实例分割,semantic_segmentation-语义分割
     */
    @TableField("annotation_type")
    private String annotationType;

    /**
     * 数据集路径
     */
    @TableField("dataset_path")
    private String datasetPath;

    /**
     * 总数量
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 已标注数量
     */
    @TableField("annotated_count")
    private Integer annotatedCount;

    /**
     * 标注状态：none-未标注,partial-部分标注,completed-完成标注
     */
    @TableField("annotation_status")
    private String annotationStatus;

    /**
     * 标注进度百分比
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 标注规则（JSON格式）
     */
    @TableField("annotation_rules")
    private String annotationRules;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建人
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

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