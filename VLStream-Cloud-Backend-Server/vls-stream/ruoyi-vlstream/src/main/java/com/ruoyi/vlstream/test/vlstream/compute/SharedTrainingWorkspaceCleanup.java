package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
class SharedTrainingWorkspaceCleanup {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final ComputeSharedQueueService shared;
    private final ComputeNodeService nodes;
    private final ComputeSsh ssh;
    private final ComputeExecutionCoordinator coordinator;
    private final ComputeCatalogProperties catalog;
    private final ObjectMapper json;

    private final AtomicBoolean running = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "shared-training-workspace-cleanup");
        thread.setDaemon(true);
        return thread;
    });

    /** Joins the transaction that makes the batch job terminal. */
    void enqueue(CloudTrainingJob job, JsonNode acknowledgement, Long releaseNodeId) {
        String appScope = releaseNodeId == null ? null : coordinator.scopeFingerprint();
        jdbc.update("INSERT INTO vls_shared_training_workspace_cleanup "
                + "(job_id,execution_environment,acknowledgement_json,release_node_id,release_app_scope) VALUES(?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE job_id=job_id",
            job.getId(), shared.executionEnvironment(), acknowledgement.toString(), releaseNodeId, appScope);
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 30000)
    void schedule() {
        if (!running.compareAndSet(false, true)) return;
        executor.execute(() -> {
            try {
                drain();
            } catch (RuntimeException unavailable) {
                log.warn("Shared GPU workspace cleanup scan failed: {}", unavailable.getClass().getSimpleName());
            } finally {
                running.set(false);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    void drain() {
        String environment = shared.executionEnvironment();
        if (environment == null) return;
        String appScope = catalog.configured() ? coordinator.scopeFingerprint() : null;
        BeanPropertyRowMapper<CloudTrainingJob> jobs = new BeanPropertyRowMapper<>(CloudTrainingJob.class);
        List<PendingCleanup> pending = jdbc.query(
            "SELECT j.id,j.tenant_id,j.node_id,j.options_json,j.run_dir,j.public_owner_subject,j.job_state,"
                + "c.acknowledgement_json,c.retry_count,c.release_node_id "
                + "FROM vls_shared_training_workspace_cleanup c JOIN vls_cloud_training_job j ON j.id=c.job_id "
                + "WHERE c.execution_environment=? AND c.retry_after<=NOW(3) "
                + "AND (c.release_app_scope IS NULL OR c.release_app_scope=?) "
                + "AND j.job_state IN ('COMPLETED','FAILED','CANCELLED') ORDER BY c.retry_after,c.job_id LIMIT 8",
            (row, index) -> new PendingCleanup(jobs.mapRow(row, index), row.getString("acknowledgement_json"),
                row.getInt("retry_count"), row.getObject("release_node_id", Long.class)),
            environment, appScope);
        for (PendingCleanup request : pending) {
            int claimed = jdbc.update("UPDATE vls_shared_training_workspace_cleanup "
                    + "SET retry_count=retry_count+1,retry_after=? "
                    + "WHERE job_id=? AND retry_count=? AND retry_after<=NOW(3)",
                new Timestamp(System.currentTimeMillis() + 300_000L), request.job.getId(), request.retryCount);
            if (claimed == 1) cleanup(request);
        }
    }

    private void cleanup(PendingCleanup request) {
        CloudTrainingJob job = request.job;
        String previousTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(job.getTenantId());
        try {
            JsonNode acknowledgement = json.readTree(request.acknowledgement);
            String expected = "COMPLETED".equals(job.getJobState()) ? "ARCHIVED" : job.getJobState();
            if (!shared.isSharedJob(job) || !expected.equals(acknowledgement.path("state").asText()))
                throw new IOException("共享 GPU 工作目录清理终态不匹配");
            ComputeNode node = nodes.get(job.getNodeId());
            if (!nodes.isCompanyShared(node)) throw new IOException("工作目录不属于共享 GPU");
            nodes.verifyJob(job, node);
            try (ComputeSsh.Connection connection = ssh.open(node)) {
                shared.cleanupWorkspace(connection, node, job, acknowledgement);
            }
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                int removed = jdbc.update("DELETE FROM vls_shared_training_workspace_cleanup WHERE job_id=? AND retry_count=?",
                    job.getId(), request.retryCount + 1);
                if (removed == 1 && request.releaseNodeId != null)
                    coordinator.enqueueRelease(request.releaseNodeId, "training:" + job.getId());
            });
        } catch (IOException | RuntimeException failure) {
            log.warn("Shared GPU workspace cleanup deferred: job={}, cause={}", job.getId(), failure.getClass().getSimpleName());
        } finally {
            if (previousTenant == null) TenantContextHolder.clear();
            else TenantContextHolder.setTenantId(previousTenant);
        }
    }

    @RequiredArgsConstructor
    private static class PendingCleanup {
        private final CloudTrainingJob job;
        private final String acknowledgement;
        private final int retryCount;
        private final Long releaseNodeId;
    }
}
