package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Device Information Entity Class
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("device_info")
@ApiModel(value = "DeviceInfo object", description = "Device information")
public class DeviceInfo {

    @ApiModelProperty(value = "Device ID, primary key")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Device name")
    @TableField("device_name")
    private String deviceName;

    @ApiModelProperty(value = "Device number, unique identifier")
    @TableField("device_id")
    private String deviceId;

    @ApiModelProperty(value = "Video stream URL (RTSP/HTTP, etc.)")
    @TableField("stream_url")
    private String streamUrl;

    @ApiModelProperty(value = "Device status")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "Device position/installation location")
    @TableField("position")
    private String position;

    @ApiModelProperty(value = "Device type (dome camera, PTZ, bullet camera, etc.)")
    @TableField("device_type")
    private String deviceType;

    @ApiModelProperty(value = "Device brand (Hikvision, Dahua, Uniview, etc.)")
    @TableField("brand")
    private String brand;

    @ApiModelProperty(value = "Device model")
    @TableField("model")
    private String model;

    @ApiModelProperty(value = "IP address (supports IPv4 and IPv6)")
    @TableField("ip_address")
    private String ipAddress;

    @ApiModelProperty(value = "Port number")
    @TableField("port")
    private Integer port;

    @ApiModelProperty(value = "Login username")
    @TableField("username")
    private String username;

    @ApiModelProperty(value = "Login password")
    @TableField("password")
    private String password;

    @ApiModelProperty(value = "Device description")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "Remark")
    @TableField("remark")
    private String remark;

    @ApiModelProperty(value = "Creator")
    @TableField("created_by")
    private String createdBy;

    @ApiModelProperty(value = "Updater")
    @TableField("updated_by")
    private String updatedBy;

    @ApiModelProperty(value = "Creation time")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ApiModelProperty(value = "Update time")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "Deleted: 0-Not deleted, 1-Deleted")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty(value = "Location description")
    @TableField("location")
    private String location;

    @ApiModelProperty(value = "Longitude")
    @TableField("longitude")
    private String longitude;

    @ApiModelProperty(value = "Latitude")
    @TableField("latitude")
    private String latitude;

    @ApiModelProperty(value = "Device image path")
    @TableField("image_path")
    private String imagePath;

    @ApiModelProperty(value = "Height position (high altitude/ground/underground/other)")
    @TableField("height_position")
    private String heightPosition;

    @ApiModelProperty(value = "Detailed address")
    @TableField("address")
    private String address;

    @ApiModelProperty(value = "Region selection (JSON format)")
    @TableField("region")
    private String region;

    @ApiModelProperty(value = "Manufacturer")
    @TableField("manufacturer")
    private String manufacturer;

    @ApiModelProperty(value = "RTSP URL")
    @TableField("rtsp_url")
    private String rtspUrl;

    @ApiModelProperty(value = "Device tag")
    @TableField("tag")
    private String tag;

    @ApiModelProperty(value = "Video stream path")
    @TableField("stream_path")
    private String streamPath;

    @ApiModelProperty(value = "Algorithm ID")
    @TableField("algorithm_id")
    private String algorithmId;

    @ApiModelProperty(value = "Creator")
    @TableField("creator")
    private String creator;

    // Compatibility methods: alias mapping for some fields
    @ApiModelProperty(value = "Updater (compatibility field, actually corresponds to updated_by)", hidden = true)
    public String getUpdater() {
        return this.updatedBy;
    }

    public void setUpdater(String updater) {
        this.updatedBy = updater;
    }
} 