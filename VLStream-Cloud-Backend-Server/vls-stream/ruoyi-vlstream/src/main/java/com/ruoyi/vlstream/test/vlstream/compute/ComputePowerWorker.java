package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.List;

/** One operation per physical server, submitted once; ambiguous outcomes are reconciled read-only. */
@Service
@RequiredArgsConstructor
public class ComputePowerWorker {
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<ComputePowerProvider> providers = new ArrayList<>();
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PlatformTransactionManager transactions;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeBillingLedgerService billing;

    @Scheduled(fixedDelay = 15000, initialDelay = 30000)
    public void scan() {
        List<ComputeNode> pending = jdbc.query("SELECT * FROM vls_compute_node WHERE power_operation_id IS NOT NULL ORDER BY power_operation_started_at LIMIT 100", new BeanPropertyRowMapper<>(ComputeNode.class));
        for (ComputeNode node : pending) tick(node);
    }
    void tick(ComputeNode node) {
        ComputePowerProvider provider = providers.stream().filter(p -> p.supports(node)).findFirst().orElse(null);
        if (provider == null) return;
        Long serverId = jdbc.queryForObject("SELECT MIN(id) FROM vls_compute_node WHERE " + ComputeNodeService.SERVER_WHERE, Long.class, ComputeNodeService.serverArgs(node));
        try (java.sql.Connection connection = java.util.Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            String lock = "vls-compute-power-" + serverId;
            if (!lock(connection, "SELECT GET_LOCK(?,0)", lock)) return;
            try {
                ComputeNode current = transaction().execute(tx -> {
                    List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE " + ComputeNodeService.SERVER_WHERE + " ORDER BY id FOR UPDATE", new BeanPropertyRowMapper<>(ComputeNode.class), ComputeNodeService.serverArgs(node));
                    return rows.stream().filter(n -> node.getId().equals(n.getId()) && node.getPowerOperationId().equals(n.getPowerOperationId())).findFirst().orElse(null);
                });
                if (current != null) {
                    try { poll(current, provider); }
                    catch (ComputeProviderException e) { recordFailure(current, e); }
                }
            } finally { lock(connection, "SELECT RELEASE_LOCK(?)", lock); }
        } catch (Exception e) {
            jdbc.update("UPDATE vls_compute_node SET capacity_state='UNKNOWN',capacity_checked_at=NULL,lifecycle_message='厂商查询暂不可用，将只读重试；结果尚未确认' WHERE id=? AND power_operation_id=?", node.getId(), node.getPowerOperationId());
        }
    }
    void poll(ComputeNode node, ComputePowerProvider provider) {
        boolean expired = node.getPowerOperationStartedAt() == null || System.currentTimeMillis() - node.getPowerOperationStartedAt().getTime() > 10 * 60 * 1000L;
        if (!node.isPowerOperationSubmitted() && expired) {
            withServerLock(node, () -> {
                if (jdbc.update("UPDATE vls_compute_node SET power_state='UNKNOWN',capacity_state='UNKNOWN',capacity_checked_at=NULL,power_operation_id=NULL,lifecycle_message='等待操作超时，尚未向厂商提交；请刷新状态后重试' WHERE id=? AND power_operation_id=? AND power_operation_submitted=0", node.getId(), node.getPowerOperationId()) > 0) resetServer(node, "UNKNOWN");
            }); return;
        }
        if (!node.isPowerOperationSubmitted()) {
            if ("ECS".equals(node.getProviderProduct())) SnapshotPreflight.check(provider.status(node), node.getPowerOperationAction(), node.getGpuIndex());
            // Durability precedes HTTP. Ambiguous send/worker crash cannot cause a second submission.
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
                throw new IllegalStateException("Submission requires a committed reservation");
            int reserved = jdbc.update("UPDATE vls_compute_node SET power_operation_submitted=1 WHERE id=? AND power_operation_id=? AND power_operation_submitted=0", node.getId(), node.getPowerOperationId());
            if (reserved != 1) return;
            provider.submit(node, node.getPowerOperationAction(), node.getPowerOperationId());
        }
        ComputePowerProvider.Snapshot snapshot = provider.status(node);
        boolean done = goal(node, snapshot);
        String state = done ? snapshot.power : expired ? snapshot.power : node.getPowerState();
        String message = done ? snapshot.message : expired
            ? "操作超过确认时限，继续只读查询；未再次启停。管理员可查询厂商终态后恢复本地挂起记录" : snapshot.message;
        withServerLock(node, () -> {
            int changed = jdbc.update("UPDATE vls_compute_node SET power_state=?,capacity_state=?,capacity_source='PROVIDER_API',capacity_checked_at=NOW(),power_checked_at=NOW(),provider_gpu_amount=?,provider_locked=?,actual_stopped_mode=?,provider_charge_type=?,lifecycle_message=?,power_operation_id=CASE WHEN ? THEN NULL ELSE power_operation_id END WHERE id=? AND power_operation_id=?",
                state, snapshot.capacity, snapshot.gpuAmount, snapshot.locked, snapshot.stoppedMode, snapshot.chargeType, message, done, node.getId(), node.getPowerOperationId());
            if (changed > 0) {
                jdbc.update("UPDATE vls_compute_node SET provider_gpu_amount=?,provider_locked=?,actual_stopped_mode=?,provider_charge_type=? WHERE " + ComputeNodeService.SERVER_WHERE,
                    ComputeNodeService.serverParams(node,snapshot.gpuAmount,snapshot.locked,snapshot.stoppedMode,snapshot.chargeType));
                if (done) resetServer(node, snapshot.power);
                recordUsageObservation(node, snapshot);
            }
        });
    }
    static boolean goal(ComputeNode node, ComputePowerProvider.Snapshot snapshot) {
        return "FAILED".equals(snapshot.power) || ("start".equals(node.getPowerOperationAction()) ? "RUNNING".equals(snapshot.power) : "STOPPED".equals(snapshot.power));
    }
    void resetServer(ComputeNode node, String power) {
        jdbc.update("UPDATE vls_compute_node SET power_state=?,power_checked_at=NOW(),probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE " + ComputeNodeService.SERVER_WHERE,
            ComputeNodeService.serverParams(node, power));
    }
    void recordFailure(ComputeNode node, ComputeProviderException exception) {
        boolean known = exception.getKind() != ComputeProviderException.Kind.UNKNOWN;
        withServerLock(node, () -> {
            int changed = jdbc.update("UPDATE vls_compute_node SET power_state='UNKNOWN',capacity_state=?,capacity_source='PROVIDER_API',capacity_checked_at=NOW(),probe_state='UNTESTED',probe_time=NULL,lifecycle_message=?,power_operation_id=CASE WHEN ? THEN NULL ELSE power_operation_id END WHERE id=? AND power_operation_id=?",
                exception.getKind() == ComputeProviderException.Kind.NO_STOCK ? "UNAVAILABLE" : "UNKNOWN", exception.getMessage(), known, node.getId(), node.getPowerOperationId());
            if (changed > 0) {
                resetServer(node, "UNKNOWN");
                jdbc.update("UPDATE vls_compute_node SET capacity_state=?,capacity_source='PROVIDER_API',capacity_checked_at=NOW(),lifecycle_message=? WHERE " + ComputeNodeService.SERVER_WHERE,
                    ComputeNodeService.serverParams(node,exception.getKind()==ComputeProviderException.Kind.NO_STOCK ? "UNAVAILABLE" : "UNKNOWN",exception.getMessage()));
                recordUsageObservation(node, new ComputePowerProvider.Snapshot("start".equals(node.getPowerOperationAction()) && exception.getKind()==ComputeProviderException.Kind.NO_STOCK ? "FAILED" : "UNKNOWN", "UNKNOWN", "内部对账观察"));
            }
        });
    }
    private void recordUsageObservation(ComputeNode operation, ComputePowerProvider.Snapshot snapshot) {
        if (billing==null) return;
        Long canonicalId=jdbc.queryForObject("SELECT MIN(id) FROM vls_compute_node WHERE "+ComputeNodeService.SERVER_WHERE,Long.class,ComputeNodeService.serverArgs(operation));
        billing.observePower(operation,snapshot,canonicalId);
    }
    static final class SnapshotPreflight {
        static void check(ComputePowerProvider.Snapshot snapshot, String action, int gpuIndex) {
            if (Boolean.TRUE.equals(snapshot.locked)) throw new ComputeProviderException(ComputeProviderException.Kind.LOCKED);
            if ("start".equals(action) && snapshot.gpuAmount != null && snapshot.gpuAmount <= gpuIndex) throw new ComputeProviderException(ComputeProviderException.Kind.WRONG_STATE);
            if (!("start".equals(action) ? "STOPPED".equals(snapshot.power) : "RUNNING".equals(snapshot.power)))
                throw new ComputeProviderException("UNKNOWN".equals(snapshot.power) ? ComputeProviderException.Kind.UNKNOWN : ComputeProviderException.Kind.WRONG_STATE);
        }
    }
    private TransactionTemplate transaction() { return new TransactionTemplate(transactions != null ? transactions : new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource())); }
    private void withServerLock(ComputeNode node, Runnable action) {
        if (jdbc.getDataSource() == null) { action.run(); return; } // mock seam, never a production datasource
        transaction().execute(tx -> {
            jdbc.queryForList("SELECT id FROM vls_compute_node WHERE " + ComputeNodeService.SERVER_WHERE + " ORDER BY id FOR UPDATE", ComputeNodeService.serverArgs(node));
            action.run(); return null;
        });
    }
    private boolean lock(java.sql.Connection connection, String sql, String key) throws java.sql.SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (java.sql.ResultSet result = statement.executeQuery()) { return result.next() && result.getInt(1) == 1; }
        }
    }
}
