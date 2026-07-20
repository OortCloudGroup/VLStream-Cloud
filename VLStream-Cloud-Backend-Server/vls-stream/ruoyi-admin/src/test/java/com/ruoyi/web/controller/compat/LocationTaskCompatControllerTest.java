/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class LocationTaskCompatControllerTest {

    /**
     * Verify that the serialized response contains only the original Go envelope fields.
     */
    @Test
    void serializesTheGoResponseEnvelopeWithoutBladeFields() {
        JsonNode result = new ObjectMapper().valueToTree(LocationTaskResult.success());

        assertEquals(200, result.get("code").asInt());
        assertEquals("成功", result.get("msg").asText());
        assertFalse(result.has("data"));
        assertFalse(result.has("success"));
    }

    /**
     * Verify that an accessToken body value resolves to the local Java user session.
     */
    @Test
    void delegatesAuthenticatedRequestsWithTheResolvedTenant() {
        LocationTaskCompatService service = mock(LocationTaskCompatService.class);
        BladeTokenUserStore tokenStore = mock(BladeTokenUserStore.class);
        LocationTaskCompatController controller = new LocationTaskCompatController(service, tokenStore);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("accessToken", "legacy-token");
        LocationTaskResult<Map<String, Object>> expected = LocationTaskResult.success(body);

        SysUser user = new SysUser();
        user.setUserId("user-1");
        user.setTenantId("tenant-1");
        user.setUserName("事件处理人");
        when(tokenStore.get("legacy-token")).thenReturn(user);
        doReturn(expected).when(service)
            .eventList(any(Map.class), any(LocationTaskCompatService.UserContext.class));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("requesttype", "app");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        LocationTaskResult<?> actual;
        try {
            actual = controller.eventList(body);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        assertSame(expected, actual);
        ArgumentCaptor<LocationTaskCompatService.UserContext> contextCaptor =
            ArgumentCaptor.forClass(LocationTaskCompatService.UserContext.class);
        verify(service).eventList(any(Map.class), contextCaptor.capture());
        assertEquals("tenant-1", contextCaptor.getValue().getTenantId());
        assertEquals("app", contextCaptor.getValue().getClient());
    }

    /**
     * Verify that an authenticated header wins over a stale legacy body token.
     */
    @Test
    void prefersAuthenticatedHeaderOverLegacyBodyToken() throws Exception {
        LocationTaskCompatService service = mock(LocationTaskCompatService.class);
        BladeTokenUserStore tokenStore = mock(BladeTokenUserStore.class);
        LocationTaskCompatController controller = new LocationTaskCompatController(service, tokenStore);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("accessToken", "expired-body-token");

        SysUser user = new SysUser();
        user.setUserId("user-1");
        user.setUserName("admin");
        when(tokenStore.get("current-header-token")).thenReturn(user);
        doReturn(LocationTaskResult.success()).when(service)
            .eventList(any(Map.class), any(LocationTaskCompatService.UserContext.class));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer current-header-token");
        AuthorizationInterceptor interceptor = new AuthorizationInterceptor();
        interceptor.preHandle(servletRequest, null, null);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        try {
            assertEquals(200, controller.eventList(body).getCode());
        } finally {
            interceptor.afterCompletion(servletRequest, null, null, null);
            RequestContextHolder.resetRequestAttributes();
        }

        verify(tokenStore).get("current-header-token");
        verify(tokenStore, never()).get("expired-body-token");
    }

    /**
     * Verify that the Go-compatible validation error is returned before service execution.
     */
    @Test
    void rejectsRequestsWithoutAnAccessToken() {
        LocationTaskCompatService service = mock(LocationTaskCompatService.class);
        BladeTokenUserStore tokenStore = mock(BladeTokenUserStore.class);
        LocationTaskCompatController controller = new LocationTaskCompatController(service, tokenStore);

        LocationTaskResult<?> result = controller.eventList(new LinkedHashMap<String, Object>());

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 accessToken不能为空", result.getMsg());
        verify(tokenStore, never()).get(any(String.class));
        verify(service, never()).eventList(any(Map.class), any(LocationTaskCompatService.UserContext.class));
    }

    /**
     * Verify that an unknown local token keeps the historical Go error contract.
     */
    @Test
    void rejectsUnknownAccessTokens() {
        LocationTaskCompatService service = mock(LocationTaskCompatService.class);
        BladeTokenUserStore tokenStore = mock(BladeTokenUserStore.class);
        LocationTaskCompatController controller = new LocationTaskCompatController(service, tokenStore);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("accessToken", "expired-token");

        LocationTaskResult<?> result = controller.eventList(body);

        assertEquals(4004, result.getCode());
        assertEquals("无效的accesstoken", result.getMsg());
        verify(service, never()).eventList(any(Map.class), any(LocationTaskCompatService.UserContext.class));
    }

    /**
     * Verify that deprecated V1 group endpoints bypass authentication as in Go.
     */
    @Test
    void deprecatedV1GroupEndpointsDoNotAuthenticate() {
        LocationTaskCompatService service = mock(LocationTaskCompatService.class);
        BladeTokenUserStore tokenStore = mock(BladeTokenUserStore.class);
        LocationTaskCompatController controller = new LocationTaskCompatController(service, tokenStore);
        doReturn(LocationTaskResult.deprecated()).when(service)
            .eventGroupSaveV1(any(Map.class), any());
        doReturn(LocationTaskResult.deprecated()).when(service)
            .eventGroupDeleteV1(any(Map.class), any());

        assertEquals(404, controller.eventGroupSave(null).getCode());
        assertEquals(404, controller.eventGroupDelete(null).getCode());
        verify(tokenStore, never()).get(any(String.class));
    }

    /**
     * Verify that camera device reports keep the Go service's public, token-free contract.
     */
    @Test
    void cameraReportEndpointsDoNotAuthenticate() {
        LocationTaskCompatService service = mock(LocationTaskCompatService.class);
        BladeTokenUserStore tokenStore = mock(BladeTokenUserStore.class);
        LocationTaskCompatController controller = new LocationTaskCompatController(service, tokenStore);
        doReturn(LocationTaskResult.success()).when(service).reportCameraEvent(any(Map.class));
        doReturn(LocationTaskResult.success()).when(service).reportCameraEvents(any(Map.class));

        assertEquals(200, controller.reportCameraEvent(null).getCode());
        assertEquals(200, controller.reportCameraEvents(null).getCode());
        verify(tokenStore, never()).get(any(String.class));
    }

    /**
     * Verify all routes imported from the frontend API wrapper.
     */
    @Test
    void exposesAllFrontendLocationTaskRoutes() throws Exception {
        Map<String, String> routes = new LinkedHashMap<String, String>();
        routes.put("addEvent", "/task/v1/mytask_updata");
        routes.put("reportCameraEvent", "/task/v1/event_report_camera");
        routes.put("reportCameraEvents", "/task/v1/event_report_cameras");
        routes.put("eventItemList", "/task/v1/event_item_list");
        routes.put("eventItemDelete", "/task/v1/event_item_del");
        routes.put("eventItemSave", "/task/v1/event_item_save");
        routes.put("eventList", "/task/v1/event_list");
        routes.put("eventBackAdd", "/task/v1/event_back_add");
        routes.put("eventInfo", "/task/v1/event_info");
        routes.put("eventBackList", "/task/v1/event_back_list");
        routes.put("eventAddUser", "/task/v1/event_add_user");
        routes.put("eventDelete", "/task/v1/event_del");
        routes.put("myEventList", "/task/v2/myevent_list");
        routes.put("eventGroupList", "/task/v2/event_group_list");
        routes.put("eventGroupSave", "/task/v1/event_group_save");
        routes.put("eventGroupDelete", "/task/v1/event_group_del");
        routes.put("eventItemSettingSave", "/task/v1/event_item_setting_save");
        routes.put("eventItemStatus", "/task/v1/event_item_status");
        routes.put("eventGroupInfo", "/task/v2/event_group_info");
        routes.put("workflowConfigGet", "/task/v1/workflowConfigGet");
        routes.put("workflowConfigSet", "/task/v1/workflowConfigSet");
        routes.put("eventGroupDeptUserSave", "/task/v1/event_group_deptuser_save");
        routes.put("eventGroupDeptUserList", "/task/v1/event_group_deptuser_list");
        routes.put("eventGroupDeptUserStatus", "/task/v1/event_group_deptuser_status");
        routes.put("eventGroupSettingSave", "/task/v2/event_group_setting_save");
        routes.put("eventGroupStatus", "/task/v2/event_group_status");
        routes.put("eventStatistics", "/task/v1/event_statistics");

        assertEquals(27, routes.size());
        for (Map.Entry<String, String> route : routes.entrySet()) {
            assertPostRoute(route.getKey(), route.getValue());
        }
    }

    /**
     * Verify one controller method's PostMapping path.
     */
    private static void assertPostRoute(String methodName, String path) throws Exception {
        Method method = LocationTaskCompatController.class.getDeclaredMethod(methodName, Map.class);

        assertArrayEquals(new String[] {path}, method.getAnnotation(PostMapping.class).value());
    }
}
