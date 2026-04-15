package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 设备信息实体类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("device_info")
@ApiModel(value = "DeviceInfo对象", description = "设备信息")
public class DeviceInfo {

    @ApiModelProperty(value = "设备ID，主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "设备名称")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty(value = "设备编号，唯一标识")
    @TableField("device_id")
    private String deviceId;

    @ApiModelProperty(value = "视频流地址 (RTSP/HTTP等)")
    @TableField("stream_url")
    private String streamUrl;

    @ApiModelProperty(value = "设备状态")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "设备位置/安装地点")
    @TableField("position")
    private String position;

    @ApiModelProperty(value = "设备类型 (球机监控、云台、枪机等)")
    @TableField("device_type")
    private String deviceType;

    @ApiModelProperty(value = "设备品牌 (海康威视、大华、宇视等)")
    @TableField("brand")
    private String brand;

    @ApiModelProperty(value = "设备型号")
    @TableField("model")
    private String model;

    @ApiModelProperty(value = "IP地址 (支持IPv4和IPv6)")
    @TableField("ip_address")
    private String ipAddress;

    @ApiModelProperty(value = "端口号")
    @TableField("port")
    private Integer port;

    @ApiModelProperty(value = "登录用户名")
    @TableField("username")
    private String username;

    @ApiModelProperty(value = "登录密码")
    @TableField("password")
    private String password;

    @ApiModelProperty(value = "设备描述信息")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "备注信息")
    @TableField("remark")
    private String remark;

    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "更新人")
    @TableField("updated_by")
    private String updatedBy;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "是否删除：0-未删除，1-已删除")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "位置描述")
    @TableField("location")
    private String location;

    @ApiModelProperty(value = "经度")
    @TableField("longitude")
    private String longitude;

    @ApiModelProperty(value = "纬度")
    @TableField("latitude")
    private String latitude;

    @ApiModelProperty(value = "设备图像路径")
    @TableField("image_path")
    private String imagePath;

    @ApiModelProperty(value = "高度位置(高空/地面/地下/其他)")
    @TableField("height_position")
    private String heightPosition;

    @ApiModelProperty(value = "详细地址")
    @TableField("address")
    private String address;

    @ApiModelProperty(value = "区划选择(JSON格式)")
    @TableField("region")
    private String region;

    @ApiModelProperty(value = "厂商")
    @TableField("manufacturer")
    private String manufacturer;

    @ApiModelProperty(value = "RTSP地址")
    @TableField("rtsp_url")
    private String rtspUrl;

    @ApiModelProperty(value = "设备标签")
    @TableField("tag")
    private String tag;

    @ApiModelProperty(value = "视频流路径")
    @TableField("stream_path")
    private String streamPath;

    @ApiModelProperty(value = "视频流路径")
    @TableField("algorithm_id")
    private String algorithmId;

    @ApiModelProperty(value = "创建人")
    @TableField("creator")
    private String creator;

    // 兼容方法：部分字段的别名映射
    @ApiModelProperty(value = "更新人（兼容字段，实际对应updated_by）", hidden = true)
    public String getUpdater() {
        return this.updatedBy;
    }

    public void setUpdater(String updater) {
        this.updatedBy = updater;
    }
} 