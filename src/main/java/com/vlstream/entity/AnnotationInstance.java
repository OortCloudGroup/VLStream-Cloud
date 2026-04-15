package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 标注实例实体类（具体的标注数据）
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("annotation_instance")
public class AnnotationInstance {

    /**
     * 实例ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的标注项目ID
     */
    @TableField("annotation_id")
    private Long annotationId;

    /**
     * 标签ID
     */
    @TableField("label_id")
    private Long labelId;

    /**
     * 图片id
     */
    @TableField("image_id")
    private Long imageId;

    /**
     * 标注类型：rect-矩形,circle-圆形,polygon-多边形
     */
    @TableField("annotation_type")
    private String annotationType;

    /**
     * 标注坐标数据(JSON格式)
     */
    @TableField("annotation_data")
    private String annotationData;

    /**
     * 置信度
     */
    @TableField("confidence")
    private BigDecimal confidence;

    /**
     * 是否已验证
     */
    @TableField("verified")
    private Boolean verified;

    /**
     * 创建人ID
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