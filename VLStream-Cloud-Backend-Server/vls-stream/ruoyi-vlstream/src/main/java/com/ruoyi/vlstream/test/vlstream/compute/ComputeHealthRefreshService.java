package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Consumer refresh: published running nodes only, pinned SSH and fixed check, no credentials in response. */
@Service
@RequiredArgsConstructor
public class ComputeHealthRefreshService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final PlatformTransactionManager transactions;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeSharedQueueService sharedQueue;

    public String refresh(Long id) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Health refresh requires its own committed reservation");
        ComputeNode visible = nodes.get(id);
        if (!"PLATFORM".equals(visible.getResourceScope())) throw new ServiceException("仅可刷新平台发布的算力环境");
        Long serverId = visible.isConfigurationManaged()?visible.getId():jdbc.queryForObject("SELECT MIN(id) FROM vls_compute_node WHERE " + ComputeNodeService.SERVER_WHERE, Long.class, ComputeNodeService.serverArgs(visible));
        String lock = "vls-compute-health-" + serverId;
        try (Connection connection = java.util.Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            if (!lock(connection, "SELECT GET_LOCK(?,0)", lock)) return "此服务器正在检查环境，请稍后刷新列表";
            try {
                ComputeNode snapshot = new TransactionTemplate(transactions).execute(tx -> {
                    ComputeNode node = nodes.lockServer(id);
                    if(nodes.isCompanyShared(node)) {
                        if(sharedQueue==null || !sharedQueue.authenticated())throw new ServiceException("请登录后刷新共享GPU环境");
                        sharedQueue.requireTarget(node);
                    } else nodes.publicInventory().requireOwner(node);
                    if (!"PLATFORM".equals(node.getResourceScope()) || !node.isEnabled()) throw new ServiceException("算力已停止发布或停用，请刷新列表");
                    if (nodes.serverRows(node).stream().anyMatch(n -> n.getPowerOperationId() != null) || (!node.isConfigurationManaged() && !"RUNNING".equals(node.getPowerState())))
                        throw new ServiceException("仅可检查明确运行且无挂起启停操作的实例；请先开机或确认厂商状态");
                    if (node.getHostKey() == null || node.getHostKey().isEmpty()) throw new ServiceException("资源管理员须先检查连接并确认 SSH 主机身份");
                    long now = System.currentTimeMillis();
                    if ("READY".equals(node.getProbeState()) && ComputeAvailability.fresh(node.getProbeTime(), now)) return null;
                    if (node.getProbeRequestedAt() != null && now - node.getProbeRequestedAt().getTime() < 60000L)
                        throw new ServiceException("此服务器检查请求过于频繁，请一分钟后重试");
                    nodes.assertNoActiveJobs(id);
                    if(node.isConfigurationManaged())nodes.requestedProbe(node);
                    if(!node.isConfigurationManaged())jdbc.update("UPDATE vls_compute_node SET probe_requested_at=NOW() WHERE " + ComputeNodeService.SERVER_WHERE, ComputeNodeService.serverArgs(node));
                    if(!nodes.isCompanyShared(node))jdbc.update("UPDATE vls_compute_node SET probe_state='CHECKING',probe_time=NULL WHERE id=?", id);
                    return node;
                });
                if (snapshot == null) return "已使用五分钟内的环境检查结果，请刷新列表查看";
                if (!nodes.probeSnapshot(snapshot)) return "检查期间实例状态或发布配置已变化，旧结果已丢弃，请刷新列表";
                String state = snapshot.isConfigurationManaged()?nodes.get(id).getProbeState():jdbc.queryForObject("SELECT probe_state FROM vls_compute_node WHERE id=?", String.class, id);
                return "READY".equals(state) ? "环境检查通过，请刷新列表后选择训练" : "环境检查未通过，请资源管理员核对依赖、GPU 和基础模型缓存";
            } finally { lock(connection, "SELECT RELEASE_LOCK(?)", lock); }
        } catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException("环境检查暂不可用，请稍后重试或联系资源管理员"); }
    }
    private boolean lock(Connection connection, String sql, String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) { return result.next() && result.getInt(1) == 1; }
        }
    }
}
