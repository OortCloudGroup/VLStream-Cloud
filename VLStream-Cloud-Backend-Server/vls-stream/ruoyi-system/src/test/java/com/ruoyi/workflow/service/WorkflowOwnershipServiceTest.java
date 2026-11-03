package com.ruoyi.workflow.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.workflow.mapper.WorkflowOwnershipMapper;
import com.ruoyi.workflow.domain.WorkflowStartRecord;
import com.ruoyi.workorder.domain.WorkOrder;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class WorkflowOwnershipServiceTest {
    private final WorkflowOwnershipMapper mapper = mock(WorkflowOwnershipMapper.class);
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final RepositoryService repository = mock(RepositoryService.class);
    private final WorkflowOwnershipService service = new WorkflowOwnershipService(mapper, mock(TaskService.class),
        mock(HistoryService.class), runtime, repository);

    @Test void localEventMustBeSameTenantAndReadUnderAnActiveTransaction() throws Exception {
        Field mode = WorkflowOwnershipService.class.getDeclaredField("tenantType");
        mode.setAccessible(true);
        mode.set(service, "single");
        assertThrows(ServiceException.class, () -> service.requireVerifiedLocalEvent("tenant", "event"));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            when(mapper.lockLocalEvent("tenant", "a-b", 7L)).thenReturn("a-b");
            service.requireVerifiedLocalEvent("tenant", "a-b-7");
            assertThrows(ServiceException.class, () -> service.requireVerifiedLocalEvent("other", "a-b-7"));
            mode.set(service, "multi");
            assertThrows(ServiceException.class, () -> service.requireVerifiedLocalEvent("tenant", "a-b-7"));
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test void onlyPersistedMatchingOrderAndActualTenantDefinitionCanBeRegistered() {
        ProcessInstanceQuery instances = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        ProcessInstance instance = mock(ProcessInstance.class);
        ProcessDefinitionQuery definitions = mock(ProcessDefinitionQuery.class, RETURNS_SELF);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(runtime.createProcessInstanceQuery()).thenReturn(instances);
        doReturn(instance).when(instances).singleResult();
        when(instance.getProcessDefinitionId()).thenReturn("definition");
        when(repository.createProcessDefinitionQuery()).thenReturn(definitions);
        doReturn(definition).when(definitions).singleResult();
        when(definition.getCategory()).thenReturn("category");
        WorkOrder order = new WorkOrder();
        order.setId("order"); order.setTenantId("tenant"); order.setProcInsId("instance");
        order.setWorkorderId("category");
        when(mapper.isWorkOrderCategory("tenant", "category")).thenReturn(1);
        when(mapper.isPersistedWorkOrder("tenant", "instance", "order", "category")).thenReturn(1);
        when(mapper.findType("tenant", "instance")).thenReturn("WORK_ORDER");
        assertTrue(service.recordVerifiedRunningOrder("tenant", "instance", order));
        verify(mapper).recordWorkOrder("tenant", "instance");
        order.setProcInsId("different-instance");
        assertFalse(service.recordVerifiedRunningOrder("tenant", "instance", order));
        verify(mapper, times(1)).recordWorkOrder("tenant", "instance");
    }

    @Test void requestClaimRejectsConflictingOrIncompleteReplayAndKeepsCompletedReplay() {
        String id = "a85e4fbd-10a8-48e7-9032-477b849028c1";
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertNull(service.claimStart("tenant", "user", id, "same"));
            verify(mapper).claimStart("tenant", "user", id, "same");
            WorkflowStartRecord previous = new WorkflowStartRecord();
            previous.setRequestHash("same"); previous.setProcInsId("instance"); previous.setWorkOrderId("order");
            when(mapper.lockStart("tenant", "user", id)).thenReturn(previous);
            assertSame(previous, service.claimStart("tenant", "user", id, "same"));
            assertThrows(ServiceException.class, () -> service.claimStart("tenant", "user", id, "different"));
            previous.setWorkOrderId(null);
            assertThrows(ServiceException.class, () -> service.claimStart("tenant", "user", id, "same"));
            verify(mapper, times(1)).claimStart("tenant", "user", id, "same");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test void periodicExceptionOnlyRecordsTheExactNewTenantInstanceAndPersistedOrder() {
        ProcessInstanceQuery instances = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        when(runtime.createProcessInstanceQuery()).thenReturn(instances);
        doReturn(mock(ProcessInstance.class)).when(instances).singleResult();
        WorkOrder order = new WorkOrder();
        order.setId("order"); order.setTenantId("tenant"); order.setProcInsId("new-instance");
        when(mapper.isPersistedInstanceOrder("tenant", "new-instance", "order")).thenReturn(1);
        when(mapper.findType("tenant", "new-instance")).thenReturn("PERIODIC_WORK_ORDER");
        assertTrue(service.recordNewPeriodicOrder("tenant", "new-instance", order));
        assertFalse(service.recordNewPeriodicOrder("tenant", "other-instance", order));
        assertFalse(service.recordNewPeriodicOrder("other-tenant", "new-instance", order));
        verify(mapper, times(1)).recordPeriodicWorkOrder("tenant", "new-instance");
    }
}
