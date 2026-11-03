package com.ruoyi.workflow.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.flowable.utils.ProcessUtils;
import com.ruoyi.workflow.service.WorkflowOwnershipService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@Tag("dev")
class ProcessTimePermissionIntersectionTest {
    @Test void runtimeTaskWindowIntersectsExistingPermissionIds() {
        Fixture f = new Fixture("allowed", "other");
        TaskQueryImpl query = new TaskQueryImpl();
        query.processInstanceIdIn(Arrays.asList("allowed", "authorized-outside-window"));
        ProcessUtils.buildProcessSearch(query, f.filter, f.engine, f.user);
        assertEquals(Collections.singletonList("allowed"), query.getProcessInstanceIds());
    }

    @Test void historyTaskNoIntersectionUsesContradictoryPredicates() {
        Fixture f = new Fixture("other");
        HistoricTaskInstanceQueryImpl query = new HistoricTaskInstanceQueryImpl();
        query.processInstanceIdIn(Collections.singletonList("allowed"));
        ProcessUtils.buildProcessSearch(query, f.filter, f.engine, f.user);
        assertEquals("0", query.getProcessInstanceId());
        assertEquals(Collections.singletonList("1"), query.getProcessInstanceIds());
    }

    @Test void timeWindowAboveTemporaryLimitFailsClosed() {
        Fixture f = new Fixture();
        ProcessInstance record = f.instance("id");
        when(f.query.listPage(0, 10001)).thenReturn(Collections.nCopies(10001, record));
        assertThrows(ServiceException.class,
            () -> ProcessUtils.buildProcessSearch(new TaskQueryImpl(), f.filter, f.engine, f.user));
    }

    @Test void temporaryCandidateLimitRejectsOverflowRatherThanTruncating() {
        assertDoesNotThrow(() -> WorkflowOwnershipService.requireWithinTemporaryLimit(10000));
        assertThrows(ServiceException.class, () -> WorkflowOwnershipService.requireWithinTemporaryLimit(10001));
    }

    private static class Fixture {
        final ProcessEngine engine = mock(ProcessEngine.class);
        final RuntimeService runtime = mock(RuntimeService.class);
        final ProcessInstanceQuery query = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        final SysUser user = new SysUser();
        final ProcessQuery filter = new ProcessQuery();
        Fixture(String... ids) {
            user.setTenantId("tenant-a");
            filter.setProStartBeginTime(new Date(1));
            filter.setProStartEndTime(new Date(2));
            when(engine.getRuntimeService()).thenReturn(runtime);
            when(runtime.createProcessInstanceQuery()).thenReturn(query);
            ProcessInstance[] records = Arrays.stream(ids).map(this::instance).toArray(ProcessInstance[]::new);
            when(query.listPage(anyInt(), anyInt())).thenReturn(Arrays.asList(records));
        }
        ProcessInstance instance(String id) {
            ProcessInstance result = mock(ProcessInstance.class);
            when(result.getId()).thenReturn(id);
            return result;
        }
    }
}
