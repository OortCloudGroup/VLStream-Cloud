/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.RuoYiApplication;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workorder.domain.WorkOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Tag("dev")
@SpringBootTest(classes = RuoYiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class LocationTaskCameraWorkOrderFlowTest {

    private static final String TENANT_ID = "codex-camera-flow-test";
    private static final String SETTING_KEY = "workflow_v2_config:" + TENANT_ID + ":1:2";

    @Autowired
    private LocationTaskCompatService taskService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private IWfProcessService processService;

    /**
     * Remove any interrupted prior run before each flow test.
     */
    @BeforeEach
    void prepareIsolatedRows() {
        cleanupRows();
        reset(processService);
    }

    /**
     * Remove every event and setting created by this test.
     */
    @AfterEach
    void removeIsolatedRows() {
        cleanupRows();
    }

    /**
     * Verify that a disabled global switch keeps the event only, while an enabled switch starts and links a work order.
     */
    @Test
    void globalActiveSafetySwitchControlsAutomaticWorkOrderCreation() {
        assertEquals(200, taskService.reportCameraEvent(cameraEvent("codex-camera-off")).getCode());
        verify(processService, after(1000).never())
            .startProcessByDefId(any(ProcessStartBo.class), any());
        assertEquals(0, eventWorkOrderStatus("codex-camera-off"));

        jdbc.update("INSERT INTO `oort_task_setting` (`key`,`val`) VALUES (?,CAST(? AS JSON))",
            SETTING_KEY,
            "{\"tenant_id\":\"" + TENANT_ID + "\",\"user_id\":\"workflow-user\","
                + "\"process_id\":\"process-definition\",\"app_id\":\"events-app\","
                + "\"auto_to_work\":true}");
        doAnswer(invocation -> {
            ProcessStartBo request = invocation.getArgument(0);
            WorkOrder workOrder = new WorkOrder();
            workOrder.setId("codex-work-order");
            request.setWorkOrder(workOrder);
            return "codex-process-instance";
        }).when(processService).startProcessByDefId(any(ProcessStartBo.class), any());

        assertEquals(200, taskService.reportCameraEvent(cameraEvent("codex-camera-on")).getCode());
        ArgumentCaptor<ProcessStartBo> processRequest = ArgumentCaptor.forClass(ProcessStartBo.class);
        verify(processService, timeout(5000))
            .startProcessByDefId(processRequest.capture(), any());
        assertEquals(0, ((Number) processRequest.getValue().getVariables().get("msg_source")).intValue());
        assertEquals("com.oort-event.demo",
            processRequest.getValue().getVariables().get("app_package"));
        assertEquals("/event-detail", processRequest.getValue().getVariables().get("jump_path"));
        awaitWorkOrderStatus("codex-camera-on", 1, 5000L);
    }

    /**
     * Verify that a switch-only update preserves the configured process and application.
     */
    @Test
    void automaticDispatchTogglePreservesGlobalWorkflowSelection() {
        LocationTaskCompatService.UserContext user = new LocationTaskCompatService.UserContext(
            "local-token", TENANT_ID, "workflow-user", "workflow-user", "web");
        LinkedHashMap<String, Object> initial = new LinkedHashMap<String, Object>();
        initial.put("mod_type", 2);
        initial.put("group_type", 1);
        initial.put("process_id", "process-definition");
        initial.put("app_id", "events-app");
        initial.put("auto_to_work", false);
        assertEquals(200, taskService.workflowConfigSet(initial, user).getCode());

        LinkedHashMap<String, Object> toggle = new LinkedHashMap<String, Object>();
        toggle.put("mod_type", 2);
        toggle.put("group_type", 1);
        toggle.put("auto_to_work", true);
        assertEquals(200, taskService.workflowConfigSet(toggle, user).getCode());

        LocationTaskResult<?> result = taskService.workflowConfigGet(toggle, user);
        Map<?, ?> config = (Map<?, ?>) result.getData();
        assertEquals("process-definition", config.get("process_id"));
        assertEquals("events-app", config.get("app_id"));
        assertEquals(Boolean.TRUE, config.get("auto_to_work"));
    }

    /**
     * Build one valid active-safety camera payload with an image so automatic dispatch is eligible.
     */
    private static Map<String, Object> cameraEvent(String deviceId) {
        LinkedHashMap<String, Object> point = new LinkedHashMap<String, Object>();
        point.put("lng", 114.0579D);
        point.put("lat", 22.5431D);
        point.put("address", "Codex smoke test");
        LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("name", "主动安全");
        body.put("describe", "翻越栏杆");
        body.put("item", "翻越栏杆");
        body.put("point", point);
        body.put("pics", Arrays.asList("https://example.invalid/camera.jpg"));
        body.put("video", Arrays.asList("https://example.invalid/camera.mp4"));
        body.put("device_id", deviceId);
        body.put("device_name", "CodexCamera");
        body.put("device_tag", "test");
        body.put("device_tenant_id", TENANT_ID);
        return body;
    }

    /**
     * Read the current automatic-work-order status for one isolated device event.
     */
    private int eventWorkOrderStatus(String deviceId) {
        Integer status = jdbc.queryForObject(
            "SELECT work_order_status FROM `oort_task_event` WHERE tenant_id = ? AND device_id = ? LIMIT 1",
            Integer.class, TENANT_ID, deviceId);
        return status == null ? 0 : status;
    }

    /**
     * Wait for the fire-and-forget hook to persist its work-order link.
     */
    private void awaitWorkOrderStatus(String deviceId, int expected, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (eventWorkOrderStatus(deviceId) == expected) {
                return;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for automatic work-order status", exception);
            }
        }
        assertEquals(expected, eventWorkOrderStatus(deviceId));
    }

    /**
     * Delete only rows carrying the dedicated test tenant identifier.
     */
    private void cleanupRows() {
        jdbc.update("DELETE FROM `oort_task_event` WHERE tenant_id = ?", TENANT_ID);
        jdbc.update("DELETE FROM `oort_task_setting` WHERE `key` = ?", SETTING_KEY);
    }
}
