package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 算法模型实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("algorithm_model")
@ApiModel(value = "AlgorithmModel对象", description = "算法模型")
public class AlgorithmModel {

    @ApiModelProperty(value = "模型ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "模型名称")
    @TableField("model_name")
    private String modelName;

    @ApiModelProperty(value = "算法ID")
    @TableField("algorithm_id")
    private Long algorithmId;

    @ApiModelProperty(value = "训练任务ID")
    @TableField("training_id")
    private Long trainingId;

    @ApiModelProperty(value = "模型版本")
    @TableField("version")
    private Integer version;

    @ApiModelProperty(value = "模型格式：ONNX,PyTorch,TensorFlow")
    @TableField("model_format")
    private String modelFormat;

    @ApiModelProperty(value = "模型大小")
    @TableField("model_size")
    private String modelSize;

    @ApiModelProperty(value = "模型文件路径")
    @TableField("model_path")
    private String modelPath;

    @ApiModelProperty(value = "模型准确率")
    @TableField("accuracy")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "状态：draft-草稿,testing-测试中,published-已发布")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "模型描述")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "下载次数")
    @TableField("download_count")
    private Integer downloadCount;

    @ApiModelProperty(value = "部署次数")
    @TableField("deploy_count")
    private Integer deployCount;

    @ApiModelProperty(value = "发布时间")
    @TableField("publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

    @ApiModelProperty(value = "创建人")
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    @ApiModelProperty(value = "是否删除：0-否，1-是")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    // 虚拟字段，用于关联查询
    @ApiModelProperty(value = "算法名称")
    @TableField(exist = false)
    private String algorithmName;

    @ApiModelProperty(value = "训练任务名称")
    @TableField(exist = false)
    private String trainingTaskName;

    @ApiModelProperty(value = "创建人姓名")
    @TableField(exist = false)
    private String createdByName;

    @ApiModelProperty(value = "状态描述")
    @TableField(exist = false)
    private String statusDesc;

    // 业务逻辑方法

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case "draft":
                return "草稿";
            case "testing":
                return "测试中";
            case "published":
                return "已发布";
            default:
                return "未知";
        }
    }

    /**
     * 检查模型是否可以发布
     */
    public boolean canPublish() {
        return "draft".equals(status) || "testing".equals(status);
    }

    /**
     * 检查模型是否可以删除
     */
    public boolean canDelete() {
        return "draft".equals(status) || "testing".equals(status);
    }

    /**
     * 检查模型是否可以下载
     */
    public boolean canDownload() {
        return "published".equals(status);
    }

    /**
     * 检查模型是否可以部署
     */
    public boolean canDeploy() {
        return "published".equals(status);
    }

    /**
     * 增加下载次数
     */
    public void increaseDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    /**
     * 增加部署次数
     */
    public void increaseDeployCount() {
        this.deployCount = (this.deployCount == null ? 0 : this.deployCount) + 1;
    }

    /**
     * 获取格式化的准确率
     */
    public String getFormattedAccuracy() {
        if (accuracy == null) {
            return "未知";
        }
        return accuracy.multiply(new BigDecimal("100")).setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * 获取模型格式描述
     */
    public String getModelFormatDesc() {
        if (modelFormat == null) {
            return "未知";
        }
        switch (modelFormat.toLowerCase()) {
            case "onnx":
                return "ONNX格式";
            case "pytorch":
                return "PyTorch格式";
            case "tensorflow":
                return "TensorFlow格式";
            default:
                return modelFormat;
        }
    }
} 