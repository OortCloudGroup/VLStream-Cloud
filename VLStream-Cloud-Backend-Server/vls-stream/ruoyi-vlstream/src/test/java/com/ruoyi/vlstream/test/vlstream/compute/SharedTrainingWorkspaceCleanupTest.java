package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SharedTrainingWorkspaceCleanupTest {
    private static final long NODE_ID = -173L;
    private final ObjectMapper json = new ObjectMapper();
    private final ModelHubProperties deployment = new ModelHubProperties();
    private final ComputeSharedQueueService shared = mock(ComputeSharedQueueService.class);
    private final ComputeNodeService nodes = mock(ComputeNodeService.class);
    private final ComputeSsh ssh = mock(ComputeSsh.class);
    private final ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class);
    private final ComputeNode node = new ComputeNode();
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private ComputeExecutionCoordinator coordinator;
    private SharedTrainingWorkspaceCleanup cleanup;

    @BeforeEach
    void setUp() throws IOException {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE vls_cloud_training_job (id CHAR(36) PRIMARY KEY,tenant_id VARCHAR(32),node_id BIGINT,"
            + "options_json TEXT,run_dir VARCHAR(255),public_owner_subject CHAR(64),job_state VARCHAR(24))");
        jdbc.execute("CREATE TABLE vls_shared_training_workspace_cleanup (job_id CHAR(36) PRIMARY KEY,execution_environment VARCHAR(64),"
            + "acknowledgement_json TEXT,release_node_id BIGINT,release_app_scope CHAR(64),retry_count INT DEFAULT 0,"
            + "retry_after TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3))");
        jdbc.execute("CREATE TABLE vls_compute_execution_lease (node_id BIGINT,job_ref VARCHAR(112),app_scope CHAR(64),"
            + "release_requested INT DEFAULT 0,PRIMARY KEY(node_id,job_ref))");
        DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
        transaction = new TransactionTemplate(manager);
        deployment.setBaseUrl("https://deployment.example/");
        deployment.setAppId("formal");
        ComputeCatalogProperties catalog = new ComputeCatalogProperties(deployment);
        coordinator = new ComputeExecutionCoordinator(catalog, mock(UnifiedComputeCatalog.class),
            mock(ConfiguredComputeRegistry.class), jdbc, json);
        cleanup = new SharedTrainingWorkspaceCleanup(jdbc, manager, shared, nodes, ssh, coordinator, catalog, json);
        when(shared.executionEnvironment()).thenReturn("localhost");
        when(shared.isSharedJob(any())).thenReturn(true);
        when(nodes.get(NODE_ID)).thenReturn(node);
        when(nodes.isCompanyShared(node)).thenReturn(true);
        when(ssh.open(node)).thenReturn(connection);
    }

    @AfterEach
    void tearDown() {
        cleanup.shutdown();
        TenantContextHolder.clear();
    }

    @Test
    void onlyTerminalJobsCanHaveTheirWorkspaceCleaned() throws Exception {
        CloudTrainingJob active = job("PREPARING");
        CloudTrainingJob completed = job("COMPLETED");
        cleanup.enqueue(active, acknowledgement("CANCELLED"), null);
        cleanup.enqueue(completed, acknowledgement("ARCHIVED"), null);

        cleanup.drain();

        verify(shared).cleanupWorkspace(same(connection), same(node), argThat(j -> completed.getId().equals(j.getId())),
            eq(acknowledgement("ARCHIVED")));
        assertEquals(1, pendingCount());
        assertEquals(0, retryCount(active));

        jdbc.update("UPDATE vls_cloud_training_job SET job_state='CANCELLED' WHERE id=?", active.getId());
        cleanup.drain();

        verify(shared).cleanupWorkspace(same(connection), same(node), argThat(j -> active.getId().equals(j.getId())),
            eq(acknowledgement("CANCELLED")));
        assertEquals(0, pendingCount());
    }

    @Test
    void lostCleanupReplyKeepsDeploymentLeaseUntilConfirmedRetry() throws Exception {
        CloudTrainingJob completed = job("COMPLETED");
        JsonNode archived = acknowledgement("ARCHIVED");
        recordLease(completed);
        cleanup.enqueue(completed, archived, NODE_ID);
        doAnswer(call -> {
            assertEquals(completed.getTenantId(), TenantContextHolder.getTenantId());
            throw new IOException("fixture: cleanup reply lost");
        }).doAnswer(call -> {
            assertEquals(completed.getTenantId(), TenantContextHolder.getTenantId());
            return null;
        }).when(shared).cleanupWorkspace(same(connection), same(node), any(), eq(archived));
        TenantContextHolder.setTenantId("outer-tenant");

        cleanup.drain();
        cleanup.drain();

        assertEquals("outer-tenant", TenantContextHolder.getTenantId());
        verify(shared, times(1)).cleanupWorkspace(same(connection), same(node), any(), eq(archived));
        assertEquals(1, retryCount(completed));
        assertEquals(0, releaseRequested(completed));

        makeDue(completed);
        cleanup.drain();

        assertEquals("outer-tenant", TenantContextHolder.getTenantId());
        verify(shared, times(2)).cleanupWorkspace(same(connection), same(node), any(), eq(archived));
        assertEquals(0, pendingCount());
        assertEquals(1, releaseRequested(completed));
    }

    @Test
    void anotherEnvironmentOrApplicationCannotClaimOrStarveLocalCleanup() throws Exception {
        CloudTrainingJob foreignEnvironment = job("FAILED");
        when(shared.executionEnvironment()).thenReturn("vm");
        cleanup.enqueue(foreignEnvironment, acknowledgement("FAILED"), NODE_ID);
        makeDue(foreignEnvironment);
        when(shared.executionEnvironment()).thenReturn("localhost");
        deployment.setAppId("other");
        for (int index = 0; index < 9; index++) {
            CloudTrainingJob foreignApp = job("FAILED");
            cleanup.enqueue(foreignApp, acknowledgement("FAILED"), NODE_ID);
            makeDue(foreignApp);
        }
        deployment.setAppId("formal");
        CloudTrainingJob local = job("FAILED");
        recordLease(local);
        cleanup.enqueue(local, acknowledgement("FAILED"), NODE_ID);

        cleanup.drain();

        verify(shared).cleanupWorkspace(same(connection), same(node), argThat(j -> local.getId().equals(j.getId())),
            eq(acknowledgement("FAILED")));
        verify(ssh, times(1)).open(node);
        assertEquals(10, pendingCount());
        assertEquals(0, jdbc.queryForObject("SELECT SUM(retry_count) FROM vls_shared_training_workspace_cleanup", Integer.class));
        assertEquals(1, releaseRequested(local));
    }

    @Test
    void rolledBackBatchCompletionDoesNotLeaveAnOutboxEntry() {
        CloudTrainingJob job = job("ARCHIVING");
        recordLease(job);

        transaction.executeWithoutResult(status -> {
            jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED' WHERE id=?", job.getId());
            cleanup.enqueue(job, acknowledgement("ARCHIVED"), NODE_ID);
            assertEquals(1, pendingCount());
            status.setRollbackOnly();
        });
        cleanup.drain();

        assertEquals("ARCHIVING", jdbc.queryForObject("SELECT job_state FROM vls_cloud_training_job WHERE id=?",
            String.class, job.getId()));
        assertEquals(0, pendingCount());
        assertEquals(0, releaseRequested(job));
        verifyNoInteractions(ssh);
    }

    @Test
    void failedDeploymentReleaseEnqueueRollsBackRemovalOfCleanupIntent() throws Exception {
        CloudTrainingJob job = job("FAILED");
        recordLease(job);
        cleanup.enqueue(job, acknowledgement("FAILED"), NODE_ID);
        jdbc.execute("ALTER TABLE vls_compute_execution_lease ADD CONSTRAINT fixture_reject_release CHECK (release_requested=0)");

        cleanup.drain();

        assertEquals(1, retryCount(job));
        assertEquals(0, releaseRequested(job));
        jdbc.execute("ALTER TABLE vls_compute_execution_lease DROP CONSTRAINT fixture_reject_release");
        makeDue(job);
        cleanup.drain();

        verify(shared, times(2)).cleanupWorkspace(same(connection), same(node), any(), eq(acknowledgement("FAILED")));
        assertEquals(0, pendingCount());
        assertEquals(1, releaseRequested(job));
    }

    private CloudTrainingJob job(String state) {
        CloudTrainingJob job = new CloudTrainingJob();
        job.setId(UUID.randomUUID().toString());
        job.setTenantId("fixture-tenant");
        job.setNodeId(NODE_ID);
        job.setJobState(state);
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,node_id,job_state) VALUES(?,?,?,?)",
            job.getId(), job.getTenantId(), job.getNodeId(), job.getJobState());
        return job;
    }

    private JsonNode acknowledgement(String state) {
        return json.createObjectNode().put("state", state);
    }

    private void recordLease(CloudTrainingJob job) {
        jdbc.update("INSERT INTO vls_compute_execution_lease(node_id,job_ref,app_scope) VALUES(?,?,?)",
            NODE_ID, "training:" + job.getId(), coordinator.scopeFingerprint());
    }

    private void makeDue(CloudTrainingJob job) {
        jdbc.update("UPDATE vls_shared_training_workspace_cleanup SET retry_after=? WHERE job_id=?",
            Timestamp.valueOf("2000-01-01 00:00:00"), job.getId());
    }

    private int pendingCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM vls_shared_training_workspace_cleanup", Integer.class);
    }

    private int retryCount(CloudTrainingJob job) {
        return jdbc.queryForObject("SELECT retry_count FROM vls_shared_training_workspace_cleanup WHERE job_id=?",
            Integer.class, job.getId());
    }

    private int releaseRequested(CloudTrainingJob job) {
        return jdbc.queryForObject("SELECT release_requested FROM vls_compute_execution_lease WHERE node_id=? AND job_ref=?",
            Integer.class, NODE_ID, "training:" + job.getId());
    }
}
