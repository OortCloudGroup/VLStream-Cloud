/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI-only models for the active-safety hardware reporting contract.
 *
 * <p>The compatibility controller continues to accept maps so its runtime
 * binding behavior remains identical to the former Go service. These models
 * describe that stable wire contract without changing request processing.</p>
 */
public final class LocationTaskCameraApiModels {

    /**
     * Coordinate information reported by a hardware device.
     */
    @Schema(name = "LocationTaskCameraPoint", description = "事件坐标")
    public static class CameraPoint {

        @Schema(description = "经度", example = "114.24779", required = true)
        public Double lng;

        @Schema(description = "纬度", example = "22.71991", required = true)
        public Double lat;

        @Schema(description = "地址信息", example = "深圳市福田区松岭路57号")
        public String address;

        @Schema(description = "原始坐标系：1-WGS84，2-GCJ02，3-BD09", example = "1")
        @JsonProperty("coord_system_type")
        public Integer coordSystemType;

        @Schema(description = "转换后的经度", example = "114.24779")
        @JsonProperty("lng_change")
        public Double lngChange;

        @Schema(description = "转换后的纬度", example = "22.71991")
        @JsonProperty("lat_change")
        public Double latChange;

        @Schema(description = "转换后的坐标系：1-WGS84，2-GCJ02，3-BD09", example = "2")
        @JsonProperty("coord_system_type_change")
        public Integer coordSystemTypeChange;
    }

    /**
     * One active-safety event reported by a hardware device.
     */
    @Schema(name = "LocationTaskCameraEventRequest", description = "单个主动安全事件")
    public static class CameraEventRequest {

        @Schema(description = "事件标题，最多10个字符；为空时响应中的name使用item", example = "未戴安全帽", maxLength = 10)
        public String name;

        @Schema(description = "事件描述；item为空时作为事件类型，此时最多10个字符", example = "检测到人员未戴安全帽")
        public String describe;

        @Schema(description = "事件类型，最多10个字符；为空时使用describe", example = "未戴安全帽", maxLength = 10)
        public String item;

        @Schema(description = "事件坐标", required = true, implementation = CameraPoint.class)
        public CameraPoint point;

        @ArraySchema(arraySchema = @Schema(description = "图片URL列表；服务端去重并移除空字符串"),
            schema = @Schema(example = "https://example.com/event/1.jpg"))
        public List<String> pics;

        @ArraySchema(arraySchema = @Schema(description = "视频URL列表"),
            schema = @Schema(example = "https://example.com/event/1.mp4"))
        public List<String> video;

        @Schema(description = "设备ID", example = "camera-001", required = true)
        @JsonProperty("device_id")
        public String deviceId;

        @Schema(description = "设备名称", example = "东门摄像机", required = true)
        @JsonProperty("device_name")
        public String deviceName;

        @Schema(description = "设备标签，用于主动安全分组匹配", example = "园区东门")
        @JsonProperty("device_tag")
        public String deviceTag;

        @Schema(description = "兼容字段；后台固定租户并忽略客户端传值", example = "000000", deprecated = true)
        @JsonProperty("device_tenant_id")
        public String deviceTenantId;
    }

    /**
     * Batch active-safety event request.
     */
    @Schema(name = "LocationTaskCameraEventsRequest", description = "批量主动安全事件")
    public static class CameraEventsRequest {

        @ArraySchema(arraySchema = @Schema(description = "事件列表，至少一条", required = true),
            schema = @Schema(implementation = CameraEventRequest.class))
        @JsonProperty("event_report")
        public List<CameraEventRequest> eventReport;
    }

    /**
     * Coordinate data returned for a created event.
     */
    @Schema(name = "LocationTaskCameraPointResponse", description = "事件坐标响应")
    public static class CameraPointResponse extends CameraPoint {
    }

    /**
     * Created event data returned by the single-report endpoint.
     */
    @Schema(name = "LocationTaskCameraEventData", description = "已创建的主动安全事件")
    public static class CameraEventData {

        @Schema(description = "事件业务ID", example = "dongmensexiangji-20260715-1")
        public String id;

        @Schema(description = "租户ID", example = "000000")
        @JsonProperty("tenant_id")
        public String tenantId;

        @Schema(description = "上报设备ID", example = "camera-001")
        public String uuid;

        @Schema(description = "事件标题", example = "未戴安全帽")
        public String name;

        @Schema(description = "事件描述", example = "检测到人员未戴安全帽")
        public String describe;

        @Schema(description = "事件坐标", implementation = CameraPointResponse.class)
        public CameraPointResponse point;

        @ArraySchema(schema = @Schema(type = "string"))
        public List<String> pics;

        @ArraySchema(schema = @Schema(type = "string"))
        @JsonProperty("send_pics")
        public List<String> sendPics;

        @ArraySchema(schema = @Schema(type = "string"))
        public List<String> video;

        @Schema(description = "事件状态，设备上报固定为2", example = "2")
        public Integer status;

        @Schema(description = "事件类型", example = "未戴安全帽")
        public String item;

        @Schema(description = "来源客户端，设备上报固定为camera", example = "camera")
        public String client;

        @ArraySchema(schema = @Schema(implementation = Map.class))
        public List<Map<String, Object>> uuids;

        @Schema(description = "事件模块类型，主动安全固定为2", example = "2")
        @JsonProperty("mod_type")
        public Integer modType;

        @Schema(description = "模块处理状态", example = "0")
        @JsonProperty("mod_status")
        public Integer modStatus;

        @Schema(description = "设备ID", example = "camera-001")
        @JsonProperty("device_id")
        public String deviceId;

        @Schema(description = "设备名称", example = "东门摄像机")
        @JsonProperty("device_name")
        public String deviceName;

        @Schema(description = "设备标签", example = "园区东门")
        @JsonProperty("device_tag")
        public String deviceTag;

        @Schema(description = "转工单状态：0-未转，1-已转", example = "0")
        @JsonProperty("work_order_status")
        public Integer workOrderStatus;

        @Schema(description = "创建时间", example = "2026-07-15 14:30:00")
        @JsonProperty("created_at")
        public String createdAt;

        @Schema(description = "更新时间", example = "2026-07-15 14:30:00")
        @JsonProperty("updated_at")
        public String updatedAt;

        @Schema(description = "完成时间；未完成时与更新时间一致", example = "2026-07-15 14:30:00")
        @JsonProperty("finish_at")
        public String finishAt;

        @Schema(description = "图片数量", example = "1")
        @JsonProperty("pic_len")
        public Long picLen;
    }

    /**
     * Single-report legacy response envelope.
     */
    @Schema(name = "LocationTaskCameraEventResponse", description = "单个上报响应")
    public static class CameraEventResponse {

        @Schema(description = "业务状态码", example = "200")
        public Integer code;

        @Schema(description = "业务消息", example = "成功")
        public String msg;

        @Schema(description = "已创建事件", implementation = CameraEventData.class)
        public CameraEventData data;
    }

    /**
     * Batch-report legacy response envelope.
     */
    @Schema(name = "LocationTaskCameraEventsResponse", description = "批量上报响应")
    public static class CameraEventsResponse {

        @Schema(description = "业务状态码", example = "200")
        public Integer code;

        @Schema(description = "业务消息", example = "成功")
        public String msg;
    }
}
