/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.web.controller.compat.LocationTaskCompatService.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Java-compatible entry point for the task APIs formerly served by
 * apaas-location-service.
 */
@RestController
public class LocationTaskCompatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationTaskCompatController.class);

    private final LocationTaskCompatService taskService;
    private final BladeTokenUserStore tokenUserStore;

    @Value("${vls.tenant.id:000000}")
    private String singleTenantId = "000000";

    /**
     * Create the compatibility controller with its task and token dependencies.
     */
    public LocationTaskCompatController(LocationTaskCompatService taskService,
                                        BladeTokenUserStore tokenUserStore) {
        this.taskService = taskService;
        this.tokenUserStore = tokenUserStore;
    }

    /**
     * Report a new event, preserving the historical mytask_updata route name.
     */
    @PostMapping("/task/v1/mytask_updata")
    public LocationTaskResult<?> addEvent(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated event-report logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.addEvent(request, user);
            }
        });
    }

    /**
     * Accept one unauthenticated camera report using the original device-facing contract.
     */
    @Operation(
        tags = "主动安全设备上报",
        summary = "单个上报主动安全事件",
        description = "硬件设备公开接口，无需登录令牌。HTTP状态通常为200，请以响应体code判断业务结果。",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCameraApiModels.CameraEventRequest.class))),
        responses = @ApiResponse(
            responseCode = "200",
            description = "code=200成功；code=4101参数错误；code=5003数据库错误；code=500服务器内部错误",
            content = @Content(schema = @Schema(
                implementation = LocationTaskCameraApiModels.CameraEventResponse.class))))
    @SecurityRequirements
    @PostMapping("/task/v1/event_report_camera")
    public LocationTaskResult<?> reportCameraEvent(@RequestBody(required = false) Map<String, Object> body) {
        return executePublic(body, new PublicOperation() {
            /** Delegate the public device request to the migrated single-report logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request) {
                return taskService.reportCameraEvent(request);
            }
        });
    }

    /**
     * Accept multiple unauthenticated camera reports using the original device-facing contract.
     */
    @Operation(
        tags = "主动安全设备上报",
        summary = "批量上报主动安全事件",
        description = "硬件设备公开接口，无需登录令牌。先校验全部事件，再逐条写入；返回成功时不返回逐条结果。",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCameraApiModels.CameraEventsRequest.class))),
        responses = @ApiResponse(
            responseCode = "200",
            description = "code=200成功；code=4101参数错误；code=5003数据库错误；code=500服务器内部错误",
            content = @Content(schema = @Schema(
                implementation = LocationTaskCameraApiModels.CameraEventsResponse.class))))
    @SecurityRequirements
    @PostMapping("/task/v1/event_report_cameras")
    public LocationTaskResult<?> reportCameraEvents(@RequestBody(required = false) Map<String, Object> body) {
        return executePublic(body, new PublicOperation() {
            /** Delegate the public device request to the migrated batch-report logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request) {
                return taskService.reportCameraEvents(request);
            }
        });
    }

    /**
     * Return the configured event-item list.
     */
    @PostMapping("/task/v1/event_item_list")
    public LocationTaskResult<?> eventItemList(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated item-list logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventItemList(request, user);
            }
        });
    }

    /**
     * Delete an event-item definition.
     */
    @PostMapping("/task/v1/event_item_del")
    public LocationTaskResult<?> eventItemDelete(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated item-delete logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventItemDelete(request, user);
            }
        });
    }

    /**
     * Create or update an event-item definition.
     */
    @PostMapping("/task/v1/event_item_save")
    public LocationTaskResult<?> eventItemSave(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated item-save logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventItemSave(request, user);
            }
        });
    }

    /**
     * Return the tenant event list.
     */
    @Operation(tags = "主动安全事件", summary = "查询事件列表",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventListRequest.class))))
    @PostMapping("/task/v1/event_list")
    public LocationTaskResult<?> eventList(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated event-list logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventList(request, user);
            }
        });
    }

    /**
     * Add handling feedback to an event.
     */
    @Operation(tags = "主动安全事件", summary = "提交事件反馈并可选转工单",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventFeedbackRequest.class))))
    @PostMapping("/task/v1/event_back_add")
    public LocationTaskResult<?> eventBackAdd(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated feedback-write logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventBackAdd(request, user);
            }
        });
    }

    /**
     * Return one event and its associated details.
     */
    @PostMapping("/task/v1/event_info")
    public LocationTaskResult<?> eventInfo(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated event-detail logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventInfo(request, user);
            }
        });
    }

    /**
     * Return an event's feedback history.
     */
    @PostMapping("/task/v1/event_back_list")
    public LocationTaskResult<?> eventBackList(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated feedback-list logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventBackList(request, user);
            }
        });
    }

    /**
     * Assign users to an event.
     */
    @Operation(tags = "主动安全事件", summary = "添加事件执行对象",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventExecutorRequest.class))))
    @PostMapping("/task/v1/event_add_user")
    public LocationTaskResult<?> eventAddUser(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated event-assignment logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventAddUser(request, user);
            }
        });
    }

    /**
     * Soft-delete an event.
     */
    @PostMapping("/task/v1/event_del")
    public LocationTaskResult<?> eventDelete(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated event-delete logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventDelete(request, user);
            }
        });
    }

    /**
     * Return events visible to the current user.
     */
    @Operation(tags = "主动安全事件", summary = "查询与当前用户相关的事件",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.MyEventListRequest.class))))
    @PostMapping("/task/v2/myevent_list")
    public LocationTaskResult<?> myEventList(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated personal-event logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.myEventList(request, user);
            }
        });
    }

    /**
     * Return authorized V2 event groups.
     */
    @Operation(tags = "主动安全配置", summary = "查询已授权的V2事件分组",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventGroupListRequest.class))))
    @PostMapping("/task/v2/event_group_list")
    public LocationTaskResult<?> eventGroupList(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated V2 group-list logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupListV2(request, user);
            }
        });
    }

    /**
     * Preserve the Go service's deprecated V1 group-save response.
     */
    @PostMapping("/task/v1/event_group_save")
    public LocationTaskResult<?> eventGroupSave(@RequestBody(required = false) Map<String, Object> body) {
        return taskService.eventGroupSaveV1(normalize(body), null);
    }

    /**
     * Preserve the Go service's deprecated V1 group-delete response.
     */
    @PostMapping("/task/v1/event_group_del")
    public LocationTaskResult<?> eventGroupDelete(@RequestBody(required = false) Map<String, Object> body) {
        return taskService.eventGroupDeleteV1(normalize(body), null);
    }

    /**
     * Save the workflow configuration for an event item.
     */
    @PostMapping("/task/v1/event_item_setting_save")
    public LocationTaskResult<?> eventItemSettingSave(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated item-setting logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventItemSettingSave(request, user);
            }
        });
    }

    /**
     * Change an event item's enabled status.
     */
    @PostMapping("/task/v1/event_item_status")
    public LocationTaskResult<?> eventItemStatus(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated item-status logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventItemStatus(request, user);
            }
        });
    }

    /**
     * Return one authorized V2 event group.
     */
    @Operation(tags = "主动安全配置", summary = "查询V2事件分组详情",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventGroupInfoRequest.class))))
    @PostMapping("/task/v2/event_group_info")
    public LocationTaskResult<?> eventGroupInfo(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated V2 group-detail logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupInfoV2(request, user);
            }
        });
    }

    /**
     * Return a workflow configuration.
     */
    @Operation(tags = "主动安全配置", summary = "查询全局转工单配置",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.WorkflowConfigGetRequest.class))))
    @PostMapping("/task/v1/workflowConfigGet")
    public LocationTaskResult<?> workflowConfigGet(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated workflow-read logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.workflowConfigGet(request, user);
            }
        });
    }

    /**
     * Save a workflow configuration.
     */
    @Operation(tags = "主动安全配置", summary = "保存全局转工单配置",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.WorkflowConfigSetRequest.class))))
    @PostMapping("/task/v1/workflowConfigSet")
    public LocationTaskResult<?> workflowConfigSet(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated workflow-write logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.workflowConfigSet(request, user);
            }
        });
    }

    /**
     * Save a department or user event-group association.
     */
    @PostMapping("/task/v1/event_group_deptuser_save")
    public LocationTaskResult<?> eventGroupDeptUserSave(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated association-write logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupDeptUserSave(request, user);
            }
        });
    }

    /**
     * Return department and user event-group associations.
     */
    @PostMapping("/task/v1/event_group_deptuser_list")
    public LocationTaskResult<?> eventGroupDeptUserList(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated association-list logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupDeptUserList(request, user);
            }
        });
    }

    /**
     * Change a department or user event-group association status.
     */
    @PostMapping("/task/v1/event_group_deptuser_status")
    public LocationTaskResult<?> eventGroupDeptUserStatus(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated association-status logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupDeptUserStatus(request, user);
            }
        });
    }

    /**
     * Save an authorized V2 event group's task settings.
     */
    @Operation(tags = "主动安全配置", summary = "保存V2分组转工单配置",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventGroupSettingRequest.class))))
    @PostMapping("/task/v2/event_group_setting_save")
    public LocationTaskResult<?> eventGroupSettingSave(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated V2 group-setting logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupSettingSaveV2(request, user);
            }
        });
    }

    /**
     * Change an authorized V2 event group's status.
     */
    @Operation(tags = "主动安全配置", summary = "修改V2分组自动转工单开关",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.AutoWorkStatusRequest.class))))
    @PostMapping("/task/v2/event_group_status")
    public LocationTaskResult<?> eventGroupStatus(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated V2 group-status logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventGroupStatusV2(request, user);
            }
        });
    }

    /**
     * Return event statistics using the original grouping rules.
     */
    @Operation(tags = "主动安全事件", summary = "统计事件处理情况",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(
                implementation = LocationTaskCompatApiModels.EventStatisticsRequest.class))))
    @PostMapping("/task/v1/event_statistics")
    public LocationTaskResult<?> eventStatistics(@RequestBody(required = false) Map<String, Object> body) {
        return execute(body, new AuthenticatedOperation() {
            /** Delegate the authenticated request to the migrated statistics logic. */
            @Override
            public LocationTaskResult<?> execute(Map<String, Object> request, UserContext user) {
                return taskService.eventStatistics(request, user);
            }
        });
    }

    /**
     * Authenticate a legacy request and translate infrastructure failures to Go codes.
     */
    private LocationTaskResult<?> execute(Map<String, Object> body, AuthenticatedOperation operation) {
        Map<String, Object> request = normalize(body);
        String token = stringValue(AuthorizationInterceptor.getToken());
        if (token.isEmpty()) {
            token = stringValue(request.get("accessToken"));
        }
        if (token.isEmpty()) {
            return LocationTaskResult.error(4101, "参数错误 accessToken不能为空");
        }

        SysUser currentUser;
        try {
            currentUser = tokenUserStore.get(token);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to resolve the migrated location task token", exception);
            return LocationTaskResult.error(5002, "操作REDIS失败");
        }
        if (currentUser == null) {
            return LocationTaskResult.error(4004, "无效的accesstoken");
        }
        String userName = stringValue(currentUser.getUserName());
        if (userName.isEmpty()) {
            userName = stringValue(currentUser.getLoginId());
        }
        UserContext user = new UserContext(token, singleTenantId, stringValue(currentUser.getUserId()),
            userName, currentClient());
        try {
            return operation.execute(request, user);
        } catch (DataAccessException exception) {
            LOGGER.error("Failed to execute migrated location task SQL", exception);
            return LocationTaskResult.error(5003, "操作MYSQL失败");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to execute migrated location task logic", exception);
            return LocationTaskResult.error(500, "服务器内部错误");
        }
    }

    /**
     * Execute a legacy public device operation and preserve the Go infrastructure-error envelope.
     */
    private LocationTaskResult<?> executePublic(Map<String, Object> body, PublicOperation operation) {
        try {
            return operation.execute(normalize(body));
        } catch (DataAccessException exception) {
            LOGGER.error("Failed to execute migrated public location task SQL", exception);
            return LocationTaskResult.error(5003, "操作MYSQL失败");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to execute migrated public location task logic", exception);
            return LocationTaskResult.error(500, "服务器内部错误");
        }
    }

    /**
     * Normalize a nullable request body into a stable mutable map.
     */
    private static Map<String, Object> normalize(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return new LinkedHashMap<String, Object>(Collections.<String, Object>emptyMap());
        }
        return new LinkedHashMap<String, Object>(body);
    }

    /**
     * Convert an optional request or session value to a trimmed string.
     */
    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * Resolve the legacy login-client marker from the current request without requiring a servlet in unit tests.
     */
    private static String currentClient() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return "";
        }
        return stringValue(((ServletRequestAttributes) attributes).getRequest().getHeader("requesttype"));
    }

    /**
     * Operation executed only after the legacy token has resolved to a local user.
     */
    private interface AuthenticatedOperation {

        /**
         * Execute one authenticated compatibility operation.
         */
        LocationTaskResult<?> execute(Map<String, Object> request, UserContext user);
    }

    /**
     * Represent one public camera-report operation.
     */
    private interface PublicOperation {
        /** Execute the operation with its normalized request body. */
        LocationTaskResult<?> execute(Map<String, Object> request);
    }
}
