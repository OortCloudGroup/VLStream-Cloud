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
 * OpenAPI-only request models for the migrated location-task endpoints.
 *
 * <p>The controller deliberately keeps map binding so its runtime behavior and
 * response structure remain compatible with the former Go service. These
 * models document that wire contract without changing deserialization.</p>
 */
public final class LocationTaskCompatApiModels {

    /** Prevent utility-class instantiation. */
    private LocationTaskCompatApiModels() {
    }

    /** Common filters accepted by tenant and personal event lists. */
    @Schema(name = "LocationTaskEventListRequest", description = "事件分页与筛选条件")
    public static class EventListRequest {
        @Schema(description = "页码，从1开始", example = "1", defaultValue = "1")
        public Integer page;

        @Schema(description = "每页数量，最多100", example = "10", defaultValue = "10")
        public Integer pagesize;

        @Schema(description = "模块类型：1事件拍传，2主动安全；0或不传时按兼容规则查询事件拍传", example = "2")
        @JsonProperty("mod_type")
        public Integer modType;

        @Schema(description = "主动安全确认筛选：0全部，1待确认，2已确认。它不是数据库mod_status原始值", example = "1")
        @JsonProperty("mod_status")
        public Integer modStatus;

        @Schema(description = "事件状态：1已完成，2待处理", example = "2")
        public Integer status;

        @Schema(description = "图片筛选：0全部，1有图片，2无图片", example = "0")
        @JsonProperty("had_pic")
        public Integer hadPic;

        @Schema(description = "事件类型", example = "未戴安全帽")
        public String item;

        @Schema(description = "任务ID")
        @JsonProperty("task_id")
        public String taskId;

        @Schema(description = "发起人或设备ID")
        public String uuid;

        @Schema(description = "部门ID")
        @JsonProperty("dept_id")
        public String deptId;

        @Schema(description = "设备分组ID")
        @JsonProperty("group_uid")
        public String groupUid;

        @Schema(description = "开始时间，格式yyyy-MM-dd HH:mm:ss", example = "2026-07-01 00:00:00")
        @JsonProperty("start_at")
        public String startAt;

        @Schema(description = "结束时间，格式yyyy-MM-dd HH:mm:ss", example = "2026-07-31 23:59:59")
        @JsonProperty("end_at")
        public String endAt;

        @Schema(description = "事件类型关键字")
        public String keyword;

        @Schema(description = "兼容请求体令牌；浏览器请求优先使用Authorization请求头", deprecated = true)
        public String accessToken;
    }

    /** Personal event-list request with a relationship scope. */
    @Schema(name = "LocationTaskMyEventListRequest", description = "我的事件分页与筛选条件")
    public static class MyEventListRequest extends EventListRequest {
        @Schema(description = "事件关系：1我执行的，2我发布的，3我反馈过的，4与我相关的全部", example = "1")
        @JsonProperty("exec_status")
        public Integer executionStatus;
    }

    /** Event feedback and optional work-order conversion request. */
    @Schema(name = "LocationTaskEventFeedbackRequest", description = "事件处理反馈")
    public static class EventFeedbackRequest {
        @Schema(description = "事件业务ID", example = "event-20260720-1", required = true)
        public String id;

        @Schema(description = "事件状态：1已完成，2待处理", example = "1", required = true)
        public Integer status;

        @Schema(description = "主动安全原始状态：0待确认，1真实告警，2维保，3误报", example = "1")
        @JsonProperty("mod_status")
        public Integer modStatus;

        @Schema(description = "转工单状态：0未转，1已转", example = "0")
        @JsonProperty("work_order_status")
        public Integer workOrderStatus;

        @Schema(description = "处理说明，最多1024个字符", maxLength = 1024)
        public String describe;

        @Schema(description = "反馈坐标")
        public Map<String, Object> point;

        @ArraySchema(schema = @Schema(type = "string"))
        public List<String> pics;

        @Schema(description = "转工单时保存的工单数据")
        @JsonProperty("work_order_data")
        public Map<String, Object> workOrderData;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }

    /** One user or vehicle assigned as an event executor. */
    @Schema(name = "LocationTaskEventExecutor", description = "事件执行对象")
    public static class EventExecutor {
        @Schema(description = "用户或车辆ID", required = true)
        public String uuid;

        @Schema(description = "执行对象类型：1用户，2车辆；不传时默认为1", example = "1")
        @JsonProperty("u_type")
        public Integer userType;
    }

    /** Event executor assignment request. */
    @Schema(name = "LocationTaskEventExecutorRequest", description = "添加事件执行对象")
    public static class EventExecutorRequest {
        @Schema(description = "事件业务ID", required = true)
        public String id;

        @ArraySchema(arraySchema = @Schema(description = "执行对象列表", required = true),
            schema = @Schema(implementation = EventExecutor.class))
        public List<EventExecutor> uuids;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }

    /** V2 event-group list request. */
    @Schema(name = "LocationTaskEventGroupListRequest", description = "V2事件分组查询")
    public static class EventGroupListRequest {
        @Schema(description = "应用ID", required = true)
        @JsonProperty("app_id")
        public String appId;

        @Schema(description = "分组维度：1区域，2分组，3标签", example = "1", required = true)
        @JsonProperty("group_type")
        public Integer groupType;

        @Schema(description = "父分组ID；为空时查询授权根分组")
        public String puid;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }

    /** V2 event-group detail request. */
    @Schema(name = "LocationTaskEventGroupInfoRequest", description = "V2事件分组详情查询")
    public static class EventGroupInfoRequest {
        @Schema(description = "分组ID", required = true)
        public String uid;

        @Schema(description = "应用ID", required = true)
        @JsonProperty("app_id")
        public String appId;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }

    /** Workflow navigation and automatic-work-order configuration. */
    @Schema(name = "LocationTaskWorkflowConfig", description = "事件转工单配置")
    public static class WorkflowConfig {
        @Schema(description = "流程ID")
        @JsonProperty("process_id")
        public String processId;

        @Schema(description = "工单应用ID")
        @JsonProperty("app_id")
        public String appId;

        @Schema(description = "工单应用包名")
        @JsonProperty("app_package")
        public String appPackage;

        @Schema(description = "工单跳转路径")
        @JsonProperty("jump_path")
        public String jumpPath;

        @Schema(description = "工单跳转参数")
        @JsonProperty("jump_params")
        public String jumpParams;

        @Schema(description = "是否自动转工单；仅接受JSON Boolean", example = "true")
        @JsonProperty("auto_to_work")
        public Boolean autoToWork;
    }

    /** Global workflow-configuration lookup request. */
    @Schema(name = "LocationTaskWorkflowConfigGetRequest", description = "查询全局事件转工单配置")
    public static class WorkflowConfigGetRequest {
        @Schema(description = "模块类型：0通用，1事件拍传，2主动安全", example = "2")
        @JsonProperty("mod_type")
        public Integer modType;

        @Schema(description = "分组维度：0通用，1区域，2分组，3标签", example = "1")
        @JsonProperty("group_type")
        public Integer groupType;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }

    /** Global workflow-configuration update request. */
    @Schema(name = "LocationTaskWorkflowConfigSetRequest", description = "保存全局事件转工单配置")
    public static class WorkflowConfigSetRequest extends WorkflowConfig {
        @Schema(description = "模块类型：0通用，1事件拍传，2主动安全", example = "2")
        @JsonProperty("mod_type")
        public Integer modType;

        @Schema(description = "分组维度：0通用，1区域，2分组，3标签", example = "1")
        @JsonProperty("group_type")
        public Integer groupType;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }

    /** V2 group workflow-configuration update request. */
    @Schema(name = "LocationTaskEventGroupSettingRequest", description = "保存V2分组转工单配置")
    public static class EventGroupSettingRequest extends EventGroupInfoRequest {
        @Schema(description = "转工单配置", required = true)
        public WorkflowConfig config;
    }

    /** Automatic-work-order status request. */
    @Schema(name = "LocationTaskAutoWorkStatusRequest", description = "修改自动转工单开关")
    public static class AutoWorkStatusRequest extends EventGroupInfoRequest {
        @Schema(description = "是否自动转工单；仅接受JSON Boolean", example = "true", required = true)
        @JsonProperty("auto_to_work")
        public Boolean autoToWork;
    }

    /** Event statistics request. */
    @Schema(name = "LocationTaskEventStatisticsRequest", description = "事件统计条件")
    public static class EventStatisticsRequest {
        @Schema(description = "展示维度：dept部门、user用户", allowableValues = {"dept", "user"})
        @JsonProperty("user_type")
        public String userType;

        @Schema(description = "时间粒度", allowableValues = {"year", "month", "day"}, example = "day")
        @JsonProperty("time_granularity")
        public String timeGranularity;

        @Schema(description = "统计口径：1按执行人，2按发起人", example = "1")
        @JsonProperty("stat_type")
        public Integer statisticsType;

        @Schema(description = "模块类型：0全部，1事件拍传，2主动安全", example = "2")
        @JsonProperty("mod_type")
        public Integer modType;

        @Schema(description = "部门ID")
        @JsonProperty("dept_id")
        public String deptId;

        @Schema(description = "开始时间，格式yyyy-MM-dd HH:mm:ss")
        @JsonProperty("start_time")
        public String startTime;

        @Schema(description = "结束时间，格式yyyy-MM-dd HH:mm:ss")
        @JsonProperty("end_time")
        public String endTime;

        @Schema(description = "兼容请求体令牌", deprecated = true)
        public String accessToken;
    }
}
