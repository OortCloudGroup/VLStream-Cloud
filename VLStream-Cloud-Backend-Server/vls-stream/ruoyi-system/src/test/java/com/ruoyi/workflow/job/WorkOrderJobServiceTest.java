package com.ruoyi.workflow.job;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workflow.service.WorkflowOwnershipService;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.bo.WorkOrderBo;
import com.ruoyi.workorder.service.IWorkOrderService;
import com.xxl.job.core.context.XxlJobContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class WorkOrderJobServiceTest {
    private static final String BODY = "{\"sysUser\":{\"tenantId\":\"tenant\",\"userId\":\"user\"},"
        + "\"processStartBo\":{\"workOrderBo\":{\"id\":\"old\",\"procInsId\":\"old-instance\"}},\"workOrderJobFlag\":1}";

    @Test void reusesOrderAlreadyCreatedByTheSameNewFlow() throws Exception {
        IWfProcessService flows = mock(IWfProcessService.class);
        IWorkOrderService orders = mock(IWorkOrderService.class);
        WorkflowOwnershipService ownership = mock(WorkflowOwnershipService.class);
        WorkOrder created = new WorkOrder();
        doAnswer(call -> {
            ProcessStartBo request = call.getArgument(0);
            assertNull(request.getWorkOrderBo().getId());
            assertNull(request.getWorkOrderBo().getProcInsId());
            request.setWorkOrder(created);
            return "new-instance";
        }).when(flows).startProcessByDefId(any(), any());
        when(ownership.recordNewPeriodicOrder("tenant", "new-instance", created)).thenReturn(true);
        XxlJobContext.setXxlJobContext(new XxlJobContext(1L, BODY, "fixture-log", 0, 1));
        try {
            new WorkOrderJobService(flows, orders, ownership).createWorkOrderJob();
            verify(orders, never()).insertByBo(any(), any());
            verify(ownership).recordNewPeriodicOrder("tenant", "new-instance", created);
            assertNotNull(WorkOrderJobService.class.getMethod("createWorkOrderJob").getAnnotation(Transactional.class));
        } finally { XxlJobContext.setXxlJobContext(null); }
    }

    @Test void failedOwnershipVerificationRaisesErrorForTransactionRollback() {
        IWfProcessService flows = mock(IWfProcessService.class);
        IWorkOrderService orders = mock(IWorkOrderService.class);
        WorkflowOwnershipService ownership = mock(WorkflowOwnershipService.class);
        when(flows.startProcessByDefId(any(ProcessStartBo.class), any(SysUser.class))).thenReturn("new-instance");
        when(flows.getTaskId(eq("new-instance"), any())).thenReturn("task");
        when(orders.insertByBo(any(WorkOrderBo.class), any())).thenReturn(new WorkOrder());
        XxlJobContext.setXxlJobContext(new XxlJobContext(1L, BODY, "fixture-log", 0, 1));
        try {
            assertThrows(IllegalStateException.class, () -> new WorkOrderJobService(flows, orders, ownership).createWorkOrderJob());
            verify(orders, times(1)).insertByBo(any(), any());
        } finally { XxlJobContext.setXxlJobContext(null); }
    }

    @Test void springProxyReflectionInvocationStartsAndRollsBackTransaction() throws Exception {
        IWfProcessService flows = mock(IWfProcessService.class);
        IWorkOrderService orders = mock(IWorkOrderService.class);
        WorkflowOwnershipService ownership = mock(WorkflowOwnershipService.class);
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return "new-instance";
        }).when(flows).startProcessByDefId(any(), any());
        when(flows.getTaskId(eq("new-instance"), any())).thenReturn("task");
        when(orders.insertByBo(any(), any())).thenReturn(new WorkOrder());
        CountingTransactions tx = new CountingTransactions();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TxConfig.class);
        context.getBeanFactory().registerSingleton("transactionManager", tx);
        context.registerBean(WorkOrderJobService.class, () -> new WorkOrderJobService(flows, orders, ownership));
        context.refresh();
        XxlJobContext.setXxlJobContext(new XxlJobContext(1L, BODY, "fixture-log", 0, 1));
        try {
            WorkOrderJobService proxy = context.getBean(WorkOrderJobService.class);
            assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> proxy.getClass().getMethod("createWorkOrderJob").invoke(proxy));
            assertEquals(1, tx.rollbacks);
            assertEquals(0, tx.commits);
        } finally { XxlJobContext.setXxlJobContext(null); context.close(); }
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TxConfig { }

    static class CountingTransactions extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object transaction, TransactionDefinition definition) { }
        protected void doCommit(DefaultTransactionStatus status) { commits++; }
        protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
