package com.ruoyi.workflow.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.mapper.WorkflowOwnershipMapper;
import com.ruoyi.workflow.domain.WorkflowStartRecord;
import com.ruoyi.workorder.domain.WorkOrder;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves request identifiers via Flowable and enforces a server-owned classification. */
@Service
@RequiredArgsConstructor
public class WorkflowOwnershipService {
    private static final String[] WORK_ORDER_PAGES = {
        "active-safety:events:list", "active-safety:workorder:my", "active-safety:workorder:pending",
        "active-safety:workorder:completed", "active-safety:workorder:claimable"
    };

    private final WorkflowOwnershipMapper ownership;
    private final TaskService tasks;
    private final HistoryService history;
    private final RuntimeService runtime;
    private final RepositoryService repository;
    @org.springframework.beans.factory.annotation.Value("${token.tenant-type:${token.tenantType:single}}")
    private String tenantType;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private WorkflowEventAuthority externalEventAuthority;

    public void requireWorkOrderPage() {
        StpUtil.checkPermissionOr(WORK_ORDER_PAGES);
    }

    public boolean canAccessWorkOrders() {
        return StpUtil.hasPermissionOr(WORK_ORDER_PAGES);
    }

    public void requireCurrentTenant(String tenant) {
        requireIdentifier(tenant);
        if (!tenant.equals(currentTenant())) throw new ServiceException("用户租户不一致");
    }

    /** Only the local single-tenant event store is known to participate in this transaction. */
    public void requireVerifiedLocalEvent(String tenant, String externalId) {
        requireIdentifier(tenant);
        requireIdentifier(externalId);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new ServiceException("事件工单创建必须在同一事务内执行");
        }
        if ("multi".equalsIgnoreCase(tenantType)) {
            if (externalEventAuthority == null) throw new ServiceException("多租户事件权威校验不可用");
            externalEventAuthority.requireEvent(tenant, externalId);
            return;
        }
        if (!"single".equalsIgnoreCase(tenantType)) throw new ServiceException("未知租户模式");
        String id = externalId;
        long number = 0;
        String[] parts = externalId.split("-");
        if (parts.length == 3) {
            try {
                number = Long.parseLong(parts[2]);
                id = parts[0] + "-" + parts[1];
            } catch (NumberFormatException ignored) {
                number = 0;
                id = externalId;
            }
        }
        if (ownership.lockLocalEvent(tenant, id, number) == null) {
            throw new ServiceException("事件不存在或不属于当前租户");
        }
    }

    /** A claim and its completion must commit atomically with the new Flowable instance. */
    public WorkflowStartRecord claimStart(String tenant, String user, String request, String hash) {
        requireIdentifier(tenant);
        requireIdentifier(user);
        if (request == null || !request.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw new ServiceException("工单请求标识无效");
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new ServiceException("工单请求必须在同一事务内执行");
        }
        WorkflowStartRecord previous = ownership.lockStart(tenant, user, request);
        if (previous == null) {
            ownership.claimStart(tenant, user, request, hash);
            return null;
        }
        if (!hash.equals(previous.getRequestHash())) throw new ServiceException("工单请求标识已用于不同内容");
        if (StringUtils.isBlank(previous.getProcInsId()) || StringUtils.isBlank(previous.getWorkOrderId())) {
            throw new ServiceException("工单请求状态未知，请联系管理员核实");
        }
        return previous;
    }

    public void completeStart(String tenant, String user, String request, String instance, String order) {
        if (ownership.completeStart(tenant, user, request, instance, order) != 1) {
            throw new ServiceException("工单请求状态冲突");
        }
    }

    public boolean isRecordedWorkOrder(String tenant, String instance) {
        return "WORK_ORDER".equals(ownership.findType(tenant, instance));
    }


    /** Resolves the definition actually started, instead of accepting a request's definition ID. */
    public boolean recordVerifiedRunningOrder(String tenant, String instance, WorkOrder order) {
        requireIdentifier(tenant);
        requireIdentifier(instance);
        ProcessInstance process = runtime.createProcessInstanceQuery().processInstanceTenantId(tenant)
            .processInstanceId(instance).singleResult();
        if (process == null) return false;
        ProcessDefinition definition = repository.createProcessDefinitionQuery().processDefinitionTenantId(tenant)
            .processDefinitionId(process.getProcessDefinitionId()).singleResult();
        return definition != null && recordVerifiedNewOrder(tenant, instance, definition.getCategory(), order);
    }

    /** Explicit legacy scheduler exception: only an instance and order created by that invocation may be marked. */
    public boolean recordNewPeriodicOrder(String tenant, String instance, WorkOrder order) {
        requireIdentifier(tenant);
        requireIdentifier(instance);
        if (order == null || StringUtils.isBlank(order.getId()) || !tenant.equals(order.getTenantId())
            || !instance.equals(order.getProcInsId())
            || runtime.createProcessInstanceQuery().processInstanceTenantId(tenant)
                .processInstanceId(instance).singleResult() == null
            || ownership.isPersistedInstanceOrder(tenant, instance, order.getId()) != 1) return false;
        ownership.recordPeriodicWorkOrder(tenant, instance);
        if (!"PERIODIC_WORK_ORDER".equals(ownership.findType(tenant, instance))) {
            throw new ServiceException("周期工单归属与已有记录冲突");
        }
        return true;
    }

    /** Called with the Flowable-resolved definition and the actual newly persisted order. */
    private boolean recordVerifiedNewOrder(String tenant, String instance, String actualCategory, WorkOrder order) {
        requireIdentifier(tenant);
        requireIdentifier(instance);
        if (order == null || StringUtils.isBlank(actualCategory) || StringUtils.isBlank(order.getId())
            || !tenant.equals(order.getTenantId()) || !instance.equals(order.getProcInsId())
            || !actualCategory.equals(order.getWorkorderId())
            || ownership.isWorkOrderCategory(tenant, actualCategory) != 1
            || ownership.isPersistedWorkOrder(tenant, instance, order.getId(), actualCategory) != 1) {
            return false; // Unproven provenance stays unknown and cannot be accessed as a known workflow.
        }
        ownership.recordWorkOrder(tenant, instance);
        if (!"WORK_ORDER".equals(ownership.findType(tenant, instance))) {
            throw new ServiceException("流程实例归属与已有记录冲突");
        }
        return true;
    }

    public void requireForTask(String taskIds, String requestedInstance) {
        String tenant = currentTenant();
        requireIdentifier(taskIds);
        String resolved = null;
        for (String taskId : taskIds.split(",", -1)) {
            requireIdentifier(taskId);
            Task active = tasks.createTaskQuery().taskTenantId(tenant).taskId(taskId).singleResult();
            String instance;
            if (active != null) {
                instance = active.getProcessInstanceId();
            } else {
                HistoricTaskInstance historic = history.createHistoricTaskInstanceQuery()
                    .taskTenantId(tenant).taskId(taskId).singleResult();
                if (historic == null) throw new ServiceException("任务不存在或不属于当前租户");
                instance = historic.getProcessInstanceId();
            }
            requireIdentifier(instance);
            if (resolved != null && !resolved.equals(instance)) throw new ServiceException("任务属于不同流程实例");
            resolved = instance;
        }
        if (StringUtils.isNotBlank(requestedInstance) && !requestedInstance.equals(resolved)) {
            throw new ServiceException("任务与流程实例不匹配");
        }
        requireResolved(tenant, resolved);
    }

    public void requireForInstance(String instance) {
        requireResolved(currentTenant(), instance);
    }

    public void requireForTaskOrInstance(String taskId, String instance) {
        if (StringUtils.isBlank(taskId)) requireForInstance(instance);
        else requireForTask(taskId, instance);
    }

    public void requireForInstances(String[] instances) {
        if (instances == null || instances.length == 0) throw new ServiceException("流程实例不能为空");
        String tenant = currentTenant();
        for (String instance : instances) requireResolved(tenant, instance);
    }

    /** Apply this set to Flowable queries before count and pagination. */
    public Set<String> allowedInstanceIds(String tenant) {
        requireCurrentTenant(tenant);
        List<String> instances = ownership.selectAllowedInstances(tenant, canAccessWorkOrders());
        requireWithinTemporaryLimit(instances.size());
        return new HashSet<>(instances);
    }

    public static void requireWithinTemporaryLimit(int size) {
        if (size > 10000) throw new ServiceException("流程查询超过临时安全上限，当前查询无法完成");
    }

    private void requireResolved(String tenant, String instance) {
        requireIdentifier(instance);
        boolean current = runtime.createProcessInstanceQuery().processInstanceTenantId(tenant)
            .processInstanceId(instance).singleResult() != null;
        if (!current && history.createHistoricProcessInstanceQuery().processInstanceTenantId(tenant)
            .processInstanceId(instance).singleResult() == null) {
            throw new ServiceException("流程实例不存在或不属于当前租户");
        }
        String type = ownership.findType(tenant, instance);
        if ("WORK_ORDER".equals(type)) {
            StpUtil.checkPermissionOr(WORK_ORDER_PAGES);
        } else if (!"OTHER_WORKFLOW".equals(type) && !"PERIODIC_WORK_ORDER".equals(type)) {
            throw new ServiceException("流程实例归属未知，禁止访问");
        }
    }

    private static String currentTenant() {
        LoginUser login = LoginHelper.getLoginUser();
        String tenant = login == null ? null : login.getTenantId();
        requireIdentifier(tenant);
        return tenant;
    }

    private static void requireIdentifier(String value) {
        if (StringUtils.isBlank(value)) throw new ServiceException("缺少可信流程或租户标识");
    }
}
