package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComputeProviderStateService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<ComputePowerProvider> providers = new ArrayList<>();

    /** Read-only vendor call; explicit owner recovery requires a freshly confirmed terminal state. */
    @Transactional(rollbackFor = Exception.class)
    public String refresh(Long id, boolean recover) {
        ComputeNode node = nodes.lockServer(id);
        boolean shared=node.getPublicInstanceKey()!=null || "PLATFORM".equals(node.getResourceScope());
        if (shared) nodes.publicInventory().requireOwner(node);
        else if (!ComputeNodeService.tenant().equals(node.getTenantId())) throw new ServiceException("请由算力资源管理员刷新厂商状态");
        List<ComputeNode> server = nodes.serverRows(node);
        ComputeNode control = server.stream().filter(n -> providers.stream().anyMatch(p -> p.supports(n))).findFirst().orElseThrow(() -> new ServiceException("厂商 API 未启用或未配置；请登记控制台观察"));
        ComputeNode pending = server.stream().filter(n -> n.getPowerOperationId()!=null).findFirst().orElse(null);
        ComputePowerProvider provider = providers.stream().filter(p -> p.supports(control)).findFirst().get();
        ComputePowerProvider.Snapshot snapshot;
        try { snapshot = provider.status(control); }
        catch (ComputeProviderException e) { throw new ServiceException(e.getMessage()); }
        boolean done = pending != null && ComputePowerWorker.goal(pending, snapshot);
        if (recover) {
            if (pending == null) throw new ServiceException("没有需要恢复的挂起操作");
            if (pending.getPowerOperationStartedAt() != null && System.currentTimeMillis()-pending.getPowerOperationStartedAt().getTime() < 10*60*1000L)
                throw new ServiceException("实例仍在正常异步确认期限内，请继续刷新");
            if (!"RUNNING".equals(snapshot.power) && !"STOPPED".equals(snapshot.power)) throw new ServiceException("厂商没有返回明确终态，不能解除挂起记录");
            nodes.assertNoActiveJobs(id); done = true;
        }
        if (pending != null && done) jdbc.update("UPDATE vls_compute_node SET power_operation_id=NULL WHERE id=? AND power_operation_id=?", pending.getId(), pending.getPowerOperationId());
        String power = pending != null && !done ? pending.getPowerState() : snapshot.power;
        jdbc.update("UPDATE vls_compute_node SET probe_state=IF(power_state=? AND ?='RUNNING',probe_state,'UNTESTED'),probe_time=IF(power_state=? AND ?='RUNNING',probe_time,NULL),power_state=?,power_checked_at=NOW(),provider_gpu_amount=?,provider_locked=?,actual_stopped_mode=?,provider_charge_type=?,capacity_state=IF(capacity_state='UNAVAILABLE' AND capacity_checked_at>DATE_SUB(NOW(),INTERVAL 5 MINUTE),'UNAVAILABLE','UNKNOWN'),lifecycle_message=?,lifecycle_revision=lifecycle_revision+1 WHERE " + ComputeNodeService.SERVER_WHERE,
            ComputeNodeService.serverParams(node, power,power,power,power,power,snapshot.gpuAmount,snapshot.locked,snapshot.stoppedMode,snapshot.chargeType,
                recover ? "依据厂商当前终态恢复本地记录；未再次执行启停，请重新检查环境；" + snapshot.message : snapshot.message));
        return recover ? "已依据厂商终态解除本地挂起记录；未执行启停，请重新检查环境" : "厂商状态已查询；环境检查与库存仍需独立确认";
    }
}
