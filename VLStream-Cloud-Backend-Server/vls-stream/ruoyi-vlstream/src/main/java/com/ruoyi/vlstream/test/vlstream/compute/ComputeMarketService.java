package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Public offers contain resource metadata only. SSH and provider credentials remain owner-only. */
@Service
@RequiredArgsConstructor
public class ComputeMarketService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<ComputePowerProvider> powerProviders = new ArrayList<>();
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputePublicInventoryService publicInventory;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeSharedQueueService sharedQueue;
    private ComputePublicInventoryService inventory() {
        if (publicInventory==null) throw new ServiceException("公共整实例占用服务未就绪");
        return publicInventory;
    }
    @Transactional(rollbackFor = Exception.class)
    public void publicIdentity(Long id, ComputeRequests.PublicIdentity request) {
        ComputeNode node=nodes.lockServer(id);
        nodes.requireManagement(node);
        if(nodes.isCompanyShared(node))throw new ServiceException("公司共享GPU使用任务租约，不绑定个人整实例身份");
        if (!ComputeNodeService.tenant().equals(node.getTenantId())) throw new ServiceException("无权登记公共实例身份");
        nodes.assertIdle(id); inventory().bind(node,request);
    }
    @Transactional(rollbackFor = Exception.class)
    public String claim(Long id) { ComputeNode node=nodes.lockServer(id);if(nodes.isCompanyShared(node))throw new ServiceException("公司共享GPU无需占用整实例，请在训练时选择并排队");return inventory().claim(node); }
    @Transactional(rollbackFor = Exception.class)
    public String release(Long id) { ComputeNode node=nodes.lockServer(id);if(nodes.isCompanyShared(node))throw new ServiceException("公司共享GPU在任务归档或取消确认后自动释放租约");return inventory().release(node); }

    @Data
    public static class Resource {
        private String id;
        private String name;
        private String provider;
        private String scope;
        private String serverGroup;
        private String gpuModel;
        private int gpuIndex;
        private String billingType;
        private BigDecimal pointsPerHour;
        private String powerState;
        private String capacityState;
        private String capacitySource;
        private java.util.Date capacityCheckedAt;
        private String healthState;
        private Integer taskCount;
        private String claimState;
        private boolean requiresClaim;
        private boolean canClaim;
        private boolean canOperate;
        private boolean canQueryProvider;
        private boolean canRefreshEnvironment;
        private String schedulingMode;
        private String waitingEstimate = "暂不可估算";
        private boolean schedulable;
        private boolean stopped;
        private String message;
        private boolean canManage;
        private boolean apiControl;
        private boolean operationPending;
        private Integer providerGpuAmount;
        private String actualStoppedMode;
        private String providerChargeType;
    }

    public List<Resource> list(boolean selection) {
        List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE resource_scope='PLATFORM'" +
            (selection ? " OR (tenant_id=? AND resource_scope='PRIVATE')" : "") + " ORDER BY name,id",
            new BeanPropertyRowMapper<>(ComputeNode.class), selection ? new Object[]{ComputeNodeService.tenant()} : new Object[]{});
        List<Resource> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        String viewer = publicInventory==null ? null : publicInventory.viewer();
        for (ComputeNode node : rows) {
            if(selection && !"PLATFORM".equals(node.getResourceScope()) && !nodes.canUsePrivate(node))continue;
            if (selection && (!node.isEnabled() || ComputeAvailability.noCapacity(node, now) || ComputeAvailability.noGpu(node, now))) continue;
            Resource resource = new Resource();
            boolean company=nodes.isCompanyShared(node);
            boolean shared="PLATFORM".equals(node.getResourceScope()) || node.getPublicInstanceKey()!=null;
            String claim=shared ? publicInventory==null ? "ISOLATED" : publicInventory.state(node,viewer) : null;
            resource.setRequiresClaim(shared); resource.setClaimState(claim);
            resource.setSchedulingMode(node.getSchedulingMode());
            resource.setCanClaim(shared && "FREE".equals(claim) && "FREE".equals(node.getBillingType()) && node.isEnabled());
            resource.setCanOperate(shared ? "MINE".equals(claim) : ComputeNodeService.tenant().equals(node.getTenantId()));
            resource.setCanQueryProvider(shared ? "MINE".equals(claim) : ComputeNodeService.tenant().equals(node.getTenantId()));
            resource.setCanRefreshEnvironment(resource.isCanOperate());
            resource.setId(String.valueOf(node.getId())); resource.setName(node.getName());
            resource.setProvider(node.getProvider()); resource.setScope(node.getResourceScope());
            resource.setServerGroup(node.getServerGroup()); resource.setGpuModel(node.getGpuModel()); resource.setGpuIndex(node.getGpuIndex());
            resource.setBillingType(node.getBillingType()); resource.setPointsPerHour(node.getPointsPerHour());
            resource.setPowerState(node.getPowerState());
            resource.setCapacityState(ComputeAvailability.fresh(node.getCapacityCheckedAt(), now) ? node.getCapacityState() : "UNKNOWN");
            resource.setCapacitySource(node.getCapacitySource()); resource.setCapacityCheckedAt(node.getCapacityCheckedAt());
            resource.setHealthState("CHECKING".equals(node.getProbeState()) && node.getProbeRequestedAt()!=null && now-node.getProbeRequestedAt().getTime()<240000L ? "CHECKING" : ComputeAvailability.fresh(node.getProbeTime(), now) ? node.getProbeState() : "STALE");
            resource.setTaskCount(jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job WHERE node_id=? AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED')", Integer.class, node.getId()));
            if (shared && !"MINE".equals(claim)) resource.setTaskCount(null);
            resource.setStopped("STOPPED".equals(node.getPowerState()));
            resource.setSchedulable(ComputeAvailability.ready(node, now) && (!shared || "MINE".equals(claim) && "FREE".equals(node.getBillingType())));
            resource.setMessage(shared && !"MINE".equals(claim) ? null : node.getLifecycleMessage()); result.add(resource);
            resource.setCanManage(nodes.canManage(node) && (!shared || "FREE".equals(claim)));
            if(company) {
                boolean admitted=sharedQueue!=null && sharedQueue.authenticated();
                resource.setRequiresClaim(false);resource.setClaimState("SHARED_QUEUE");resource.setCanClaim(false);
                resource.setCanOperate(false);resource.setCanQueryProvider(false);resource.setCanRefreshEnvironment(admitted);
                resource.setCanManage(nodes.canManage(node));resource.setMessage(null);
                resource.setSchedulable(admitted && sharedQueue.ready(node));
                resource.setHealthState(sharedQueue!=null && sharedQueue.ready(node)?"READY":resource.getHealthState());
            }
            List<ComputeNode> server = jdbc.query("SELECT * FROM vls_compute_node WHERE " + ComputeNodeService.SERVER_WHERE, new BeanPropertyRowMapper<>(ComputeNode.class), ComputeNodeService.serverArgs(node));
            resource.setApiControl(server.stream().anyMatch(n -> powerProviders.stream().anyMatch(p -> p.supports(n))));
            resource.setOperationPending(server.stream().anyMatch(n -> n.getPowerOperationId() != null));
            resource.setProviderGpuAmount(node.getProviderGpuAmount()); resource.setActualStoppedMode(node.getActualStoppedMode()); resource.setProviderChargeType(node.getProviderChargeType());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, ComputeRequests.Offer request) {
        nodes.lockServer(id);
        List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE tenant_id=? AND id=? FOR UPDATE",
            new BeanPropertyRowMapper<>(ComputeNode.class), ComputeNodeService.tenant(), id);
        if (rows.isEmpty()) throw new ServiceException("算力实例不存在或无权管理");
        nodes.requireManagement(rows.get(0));
        nodes.assertIdle(id);
        ComputeNode offered=rows.get(0);
        String scheduling=request.getSchedulingMode()==null?offered.getSchedulingMode():request.getSchedulingMode();
        if(sharedQueue!=null)sharedQueue.requireOffering(offered,request,scheduling);
        else if(ComputeSharedQueueService.MODE.equals(scheduling))throw new ServiceException("共享GPU调度未配置");
        if (request.isPublished() && "POINTS".equals(request.getBillingType()))
            ComputeBillingPolicy.requireEconomicalPaidOffer(rows.get(0), System.currentTimeMillis());
        if ("POINTS".equals(request.getBillingType()) && request.getPointsPerHour() == null)
            throw new ServiceException("请配置价格；积分结算未接入前收费资源仅展示");
        jdbc.update("UPDATE vls_compute_node SET resource_scope=?,billing_type=?,points_per_hour=?,scheduling_mode=? WHERE tenant_id=? AND id=?",
            request.isPublished() ? "PLATFORM" : "PRIVATE", request.getBillingType(),
            "POINTS".equals(request.getBillingType()) ? request.getPointsPerHour() : null, scheduling, ComputeNodeService.tenant(), id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void observe(Long id, ComputeRequests.Observation request) {
        nodes.lockServer(id);
        List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE tenant_id=? AND id=? FOR UPDATE", new BeanPropertyRowMapper<>(ComputeNode.class), ComputeNodeService.tenant(), id);
        if(!rows.isEmpty())nodes.requireManagement(rows.get(0));
        if (rows.isEmpty()) throw new ServiceException("算力实例不存在或无权管理");
        if (rows.get(0).getPublicInstanceKey()!=null) inventory().requireEditable(rows.get(0));
        if (nodes.serverRows(rows.get(0)).stream().anyMatch(n -> n.getPowerOperationId()!=null)) throw new ServiceException("厂商异步操作尚未确认，不能覆盖运行状态");
        if (!"RUNNING".equals(request.getPowerState())) nodes.assertIdle(id);
        jdbc.update("UPDATE vls_compute_node SET power_state=?,power_checked_at=NOW(),capacity_state=?,capacity_source='OPERATOR_CONSOLE',capacity_checked_at=NOW(),probe_state='UNTESTED',probe_time=NULL,lifecycle_message=? WHERE tenant_id=? AND id=?",
            request.getPowerState(), request.getCapacityState(), "管理员登记厂商控制台观察：" + request.getNote(), ComputeNodeService.tenant(), id);
        ComputeNode node = rows.get(0);
        jdbc.update("UPDATE vls_compute_node SET power_state=?,power_checked_at=NOW(),capacity_state=?,capacity_source='OPERATOR_CONSOLE',capacity_checked_at=NOW(),probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE " + ComputeNodeService.SERVER_WHERE,
            ComputeNodeService.serverParams(node,request.getPowerState(),request.getCapacityState()));
    }

    @Transactional(rollbackFor = Exception.class)
    public String power(Long id, String action) {
        if (!"start".equals(action) && !"stop".equals(action)) throw new ServiceException("无效的实例操作");
        ComputePlatformAuthorization.Context authorization="start".equals(action)?nodes.authorizePaidOperation(id):null;
        ComputeNode node = nodes.lockServer(id);
        boolean shared="PLATFORM".equals(node.getResourceScope()) || node.getPublicInstanceKey()!=null;
        if(!shared)nodes.requireManagement(node);
        if (shared && !("start".equals(action) && "POINTS".equals(node.getBillingType()))) inventory().requireOwner(node);
        if ("start".equals(action) && "PLATFORM".equals(node.getResourceScope()) && "POINTS".equals(node.getBillingType()) && authorization==null)
            throw new ServiceException("算力计费配置已变化，请刷新后授权登录平台",47001);
        List<ComputeNode> server = nodes.serverRows(node);
        if ("stop".equals(action)) {
            // Public capacity is controlled by its verified whole-instance owner.
            if (!shared && !ComputeNodeService.tenant().equals(node.getTenantId())) throw new ServiceException("无权关闭平台算力");
        }
        if (server.stream().anyMatch(n -> n.getPowerOperationId() != null || "STARTING".equals(n.getPowerState()) || "STOPPING".equals(n.getPowerState())))
            return "实例操作正在处理中，请刷新查看结果，请勿重复操作";
        if ("stop".equals(action)) { if(shared) nodes.assertNoActiveJobs(id); else nodes.assertIdle(id); }
        if ("self".equals(node.getProvider())) throw new ServiceException("自有服务器不支持云厂商开关机，请由管理员处理");
        if (Boolean.TRUE.equals(node.getProviderLocked())) throw new ServiceException("厂商实例已锁定，请管理员刷新确认");
        if ("start".equals(action) && ComputeAvailability.noCapacity(node, System.currentTimeMillis()))
            throw new ServiceException("厂商最近确认 GPU 无库存，请稍后刷新重试");
        if ("start".equals(action) && "PLATFORM".equals(node.getResourceScope()) && "POINTS".equals(node.getBillingType()))
            throw new ServiceException("收费实例的个人 New API 扣款、可信 GPU 计算成本与整机占用/产物安全回存清理尚未完成接入，暂不能通过平台启动；工作空间成本由平台承担，停机不代表可安全复用");
        if (ComputeAvailability.fresh(node.getPowerCheckedAt(), System.currentTimeMillis()) && ("start".equals(action) ? "RUNNING".equals(node.getPowerState()) : "STOPPED".equals(node.getPowerState())))
            return "实例最近已确认处于目标状态，没有重复执行云端操作";
        ComputeNode controlled = server.stream().filter(n -> "API".equals(n.getControlMode()) && powerProviders.stream().anyMatch(p -> p.supports(n))).findFirst().orElse(null);
        if (controlled != null) {
            String operation = java.util.UUID.randomUUID().toString();
            jdbc.update("UPDATE vls_compute_node SET power_state=?,power_checked_at=NOW(),probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE " + ComputeNodeService.SERVER_WHERE,
                ComputeNodeService.serverParams(node, "start".equals(action) ? "STARTING" : "STOPPING"));
            // One operation row per physical instance; every GPU row is blocked by the shared relation.
            jdbc.update("UPDATE vls_compute_node SET power_state=?,probe_state='UNTESTED',probe_time=NULL,power_operation_id=?,power_operation_action=?,power_operation_started_at=NOW(),power_operation_submitted=0,lifecycle_message='等待厂商受理，尚未完成' WHERE id=?",
                "start".equals(action) ? "STARTING" : "STOPPING", operation, action, controlled.getId());
            return "已记录实例操作，等待厂商受理；请刷新确认最终结果，受理不代表开机成功";
        }
        return "未执行云端操作：请由实例管理员在厂商控制台" + ("start".equals(action) ? "开机" : "关机")
            + "。当前实例未接入适用产品的管理接口或后端总开关未启用；未执行云操作。开机可能因 GPU 库存不足失败，关机不保证停止计费。开机后请重新检查连接与环境。";
    }
}
