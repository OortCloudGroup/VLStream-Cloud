/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.workflow.service.IWfProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Verify stable numeric contracts and reject invalid values before JDBC access. */
class LocationTaskEventContractTest {

    private DataSource dataSource;
    private LocationTaskCompatService service;
    private LocationTaskCompatService.UserContext user;

    /** Create a service whose datasource records whether validation leaked into persistence. */
    @BeforeEach
    void setUp() {
        dataSource = mock(DataSource.class);
        service = new LocationTaskCompatService(
            dataSource,
            new ObjectMapper(),
            mock(IWfProcessService.class),
            mock(LocationTaskWorkflowProperties.class));
        user = new LocationTaskCompatService.UserContext(
            "token", "000000", "user-1", "事件处理人", "web");
    }

    /** Preserve the Go event-status codes used by database rows and frontend mappings. */
    @Test
    void preservesCanonicalEventStatusCodes() {
        assertEquals(1, LocationTaskEventContracts.EventStatus.COMPLETED.getCode());
        assertEquals("已完成", LocationTaskEventContracts.EventStatus.COMPLETED.getLabel());
        assertEquals(2, LocationTaskEventContracts.EventStatus.PENDING.getCode());
        assertEquals("待处理", LocationTaskEventContracts.EventStatus.PENDING.getLabel());
    }

    /** Keep raw alarm status 2 distinct from request filter 2. */
    @Test
    void distinguishesPersistedAlarmStatusFromAlarmFilter() {
        assertTrue(LocationTaskEventContracts.contains(
            LocationTaskEventContracts.AlarmStatus.values(), 3, false));
        assertFalse(LocationTaskEventContracts.contains(
            LocationTaskEventContracts.AlarmFilter.values(), 3, false));
        assertEquals("已确认", LocationTaskEventContracts.AlarmFilter.CONFIRMED.getLabel());
    }

    /** Preserve the executor and creator statistics codes. */
    @Test
    void preservesStatisticsTypeCodes() {
        assertEquals(1, LocationTaskEventContracts.StatisticsType.BY_EXECUTOR.getCode());
        assertEquals("按执行人", LocationTaskEventContracts.StatisticsType.BY_EXECUTOR.getLabel());
        assertEquals(2, LocationTaskEventContracts.StatisticsType.BY_CREATOR.getCode());
        assertEquals("按发起人", LocationTaskEventContracts.StatisticsType.BY_CREATOR.getLabel());
    }

    /** Reject an unknown personal-event scope before constructing a JDBC template connection. */
    @Test
    void rejectsInvalidExecutionScopeBeforeQueryingEvents() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("exec_status", 9);

        LocationTaskResult<?> result = service.myEventList(body, user);

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 exec_status只能为1、2、3或4", result.getMsg());
        verifyNoInteractions(dataSource);
    }

    /** Reject the request-only alarm-filter value 3 before querying event rows. */
    @Test
    void rejectsInvalidEventListFiltersBeforeQueryingEvents() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("mod_status", 3);

        LocationTaskResult<?> result = service.eventList(body, user);

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 mod_status筛选值只能为0、1或2", result.getMsg());
        verifyNoInteractions(dataSource);
    }

    /** Reject a nonnumeric status instead of silently treating it as the all-status filter. */
    @Test
    void rejectsNonnumericEventStatusBeforeQueryingEvents() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", "unknown");

        LocationTaskResult<?> result = service.eventList(body, user);

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 status只能为1或2", result.getMsg());
        verifyNoInteractions(dataSource);
    }

    /** Reject an invalid feedback status before attempting to load its event. */
    @Test
    void rejectsInvalidFeedbackStatusBeforeLoadingEvent() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", "event-20260717-1");
        body.put("status", 9);

        LocationTaskResult<?> result = service.eventBackAdd(body, user);

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 status只能为1或2", result.getMsg());
        verifyNoInteractions(dataSource);
    }

    /** Validate every executor type before loading the target event. */
    @Test
    void rejectsInvalidExecutorTypeBeforeLoadingEvent() {
        Map<String, Object> executor = new LinkedHashMap<String, Object>();
        executor.put("uuid", "vehicle-1");
        executor.put("u_type", 9);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", "event-20260717-1");
        body.put("uuids", Arrays.<Map<String, Object>>asList(executor));

        LocationTaskResult<?> result = service.eventAddUser(body, user);

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 u_type只能为1或2", result.getMsg());
        verifyNoInteractions(dataSource);
    }

    /** Reject an unsupported statistics relationship before querying events. */
    @Test
    void rejectsInvalidStatisticsTypeBeforeQueryingEvents() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("stat_type", 3);

        LocationTaskResult<?> result = service.eventStatistics(body, user);

        assertEquals(4101, result.getCode());
        assertEquals("参数错误 stat_type只能为1或2", result.getMsg());
        verifyNoInteractions(dataSource);
    }
}
