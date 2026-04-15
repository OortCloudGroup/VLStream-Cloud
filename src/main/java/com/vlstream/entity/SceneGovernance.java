package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

/**
 * 场景治理实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("scene_governance")
@ApiModel(value = "SceneGovernance对象", description = "场景治理")
public class SceneGovernance {

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "场景名称")
    @TableField("name")
    private String name;

    @ApiModelProperty(value = "场景描述/备注")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "关联设备")
    @TableField("devices")
    private String devices;

    @ApiModelProperty(value = "治理规则")
    @TableField("rules")
    private String rules;

    @ApiModelProperty(value = "状态: enabled-启用, disabled-禁用")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "执行类型: daily-每天, alternate-隔天, weekly-每周, monthly-每月")
    @TableField("execute_type")
    private String executeType;

    @ApiModelProperty(value = "选择的天数（JSON数组）")
    @TableField("selected_days")
    private String selectedDays;

    @ApiModelProperty(value = "间隔数量")
    @TableField("interval_num")
    private Integer intervalNum;

    @ApiModelProperty(value = "AI算法")
    @TableField("algorithm")
    private String algorithm;

    @ApiModelProperty(value = "区划地点")
    @TableField("location")
    private String location;

    @ApiModelProperty(value = "摄像头")
    @TableField("cameras")
    private String cameras;

    @ApiModelProperty(value = "开始时间")
    @TableField("start_time")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    @TableField("end_time")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "是否删除：0-未删除，1-已删除")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "更新人")
    @TableField("updated_by")
    private String updatedBy;
} 