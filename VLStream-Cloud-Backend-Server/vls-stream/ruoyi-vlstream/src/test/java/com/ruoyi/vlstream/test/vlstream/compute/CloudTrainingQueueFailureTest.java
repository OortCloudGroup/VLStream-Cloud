package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import javax.sql.DataSource;
import java.sql.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Arrays;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class CloudTrainingQueueFailureTest {
    @Test @SuppressWarnings("unchecked")
    void invalidDatasetEndsJobAndNextJobCanBeSelectedWhileNetworkErrorsRemainRetryable() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DataSource source = mock(DataSource.class);
        Connection db = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(jdbc.getDataSource()).thenReturn(source);
        when(source.getConnection()).thenReturn(db);
        when(db.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(1);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
        ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeNode node = new ComputeNode(); node.setEnabled(true);
        when(nodes.get(10L)).thenReturn(node);
        ComputeSsh ssh = mock(ComputeSsh.class);
        CloudTrainingDatasetWriter datasets = mock(CloudTrainingDatasetWriter.class);
        CloudTrainingJob first = job("first", 1L), second = job("second", 2L);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(10L)))
            .thenReturn(Collections.singletonList(first), Collections.singletonList(second));
        when(jdbc.queryForObject(contains("cancel_requested"), eq(Boolean.class), any(), any())).thenReturn(false);
        when(datasets.ensureArtifact(first)).thenThrow(new ServiceException("样本 123：标注超出图片范围"));
        when(datasets.ensureArtifact(second)).thenThrow(new IOException("temporary storage outage"));
        CloudTrainingWorker worker = new CloudTrainingWorker(jdbc, transactions, nodes, ssh, datasets,
            mock(DataManagementService.class), mock(ModelArtifactObjectStore.class), mock(RemoteModelArtifactService.class), new ObjectMapper());
        worker.tick(10L);
        verify(jdbc).update(contains("SET job_state=?,message=?"), eq("FAILED"), contains("样本 123"), eq("FAILED"), eq("tenant"), eq("first"));
        worker.tick(10L);
        verify(datasets).ensureArtifact(second);
        verify(jdbc).update(contains("SET message=?,update_time"), anyString(), eq("tenant"), eq("second"));
        verify(jdbc, never()).update(contains("SET job_state=?,message=?"), eq("FAILED"), anyString(), any(), any(), eq("second"));
        verifyNoInteractions(ssh);
    }
    private CloudTrainingJob job(String id, Long training) {
        CloudTrainingJob job = new CloudTrainingJob(); job.setId(id); job.setTenantId("tenant");
        job.setNodeId(10L); job.setTrainingId(training); job.setContainerRecordId(training);
        job.setJobState("PREPARING"); return job;
    }

    @Test @SuppressWarnings("unchecked")
    void foreignEnvironmentAtQueueHeadCannotResolveNodeOrOverwriteOrderAndStatus() throws Exception {
        JdbcTemplate jdbc = lockedJdbc();
        ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeSsh ssh = mock(ComputeSsh.class);
        CloudTrainingJob foreign = job("foreign", 1L);
        foreign.setOptionsJson("{\"configuredResource\":{\"executionEnvironment\":\"localhost\"},"
            + "\"sharedQueue\":{\"environment\":\"localhost\"}}");
        when(jdbc.query(contains("LIMIT 1"), any(RowMapper.class), eq(10L)))
            .thenReturn(Collections.singletonList(foreign));
        CloudTrainingWorker worker = worker(jdbc, nodes, ssh);
        ComputeCatalogProperties catalog = (ComputeCatalogProperties) ReflectionTestUtils.getField(worker, "catalogProperties");
        catalog.assignEnvironment("local-dev");
        ComputeSharedQueueProperties queue = (ComputeSharedQueueProperties) ReflectionTestUtils.getField(worker, "sharedQueueProperties");
        ReflectionTestUtils.setField(queue, "catalog", catalog);
        worker.tick(10L);
        verify(nodes, never()).get(anyLong());
        verifyNoInteractions(ssh);
        verify(jdbc, never()).update(anyString(), any(), any(), any());
    }

    @Test @SuppressWarnings("unchecked")
    void legacyWorkerCannotTakeCatalogJobEvenWhenItsDefaultEnvironmentMatches() throws Exception {
        JdbcTemplate jdbc = lockedJdbc();
        ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeSsh ssh = mock(ComputeSsh.class);
        CloudTrainingJob catalogJob = job("catalog-job", 1L);
        catalogJob.setOptionsJson("{\"configuredResource\":{\"executionEnvironment\":\"localhost\"}}");
        when(jdbc.query(contains("LIMIT 1"), any(RowMapper.class), eq(10L)))
            .thenReturn(Collections.singletonList(catalogJob));
        CloudTrainingWorker worker = worker(jdbc, nodes, ssh);
        ReflectionTestUtils.setField(worker, "catalogProperties", new ComputeCatalogProperties(new ModelHubProperties()));

        worker.tick(10L);
        verify(nodes, never()).get(anyLong());
        verifyNoInteractions(ssh);
        verify(jdbc, never()).update(anyString(), any(), any(), any());
    }

    @Test @SuppressWarnings("unchecked")
    void localWorkerWithMissingCatalogDoesNotOverwriteRunningProgress() throws Exception {
        JdbcTemplate jdbc = lockedJdbc();
        ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeSsh ssh = mock(ComputeSsh.class);
        CloudTrainingJob running = job("running", 1L);
        running.setJobState("RUNNING");
        running.setOptionsJson("{\"configuredResource\":{\"executionEnvironment\":\"localhost\"}}");
        CloudTrainingJob queued = job("queued", 2L);
        queued.setOptionsJson(running.getOptionsJson());
        when(jdbc.query(contains("LIMIT 1"), any(RowMapper.class), eq(10L)))
            .thenReturn(Collections.singletonList(running), Collections.singletonList(queued));
        when(nodes.get(10L)).thenThrow(ConfiguredComputeRegistry.unavailable(ConfiguredComputeRegistry.CATALOG_NODE_UNAVAILABLE));
        CloudTrainingWorker worker = worker(jdbc, nodes, ssh);

        worker.tick(10L);
        verify(jdbc, never()).update(contains("SET message=?"), any(), any(), eq("running"));
        worker.tick(10L);
        verify(jdbc).update(contains("SET message=?"), eq(ConfiguredComputeRegistry.CATALOG_NODE_UNAVAILABLE),
            eq("tenant"), eq("queued"));
        verifyNoInteractions(ssh);
    }

    @Test @SuppressWarnings("unchecked")
    void sharedQueueRegistersOnlyTheLocalPrefixBeforeTheForeignJob() throws Exception {
        JdbcTemplate jdbc = lockedJdbc();
        ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeSsh ssh = mock(ComputeSsh.class);
        ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class);
        ComputeNode node = new ComputeNode();
        when(nodes.get(10L)).thenReturn(node);
        when(nodes.isCompanyShared(node)).thenReturn(true);
        when(ssh.open(node)).thenReturn(connection);
        CloudTrainingJob head = job("local-head", 1L);
        head.setOptionsJson("{\"sharedQueue\":{\"environment\":\"localhost\"}}");
        CloudTrainingJob foreign = job("foreign", 2L);
        foreign.setOptionsJson("{\"sharedQueue\":{\"environment\":\"production\"}}");
        CloudTrainingJob tail = job("local-tail", 3L);
        tail.setOptionsJson(head.getOptionsJson());
        when(jdbc.query(contains("LIMIT 1"), any(RowMapper.class), eq(10L))).thenReturn(Collections.singletonList(head));
        when(jdbc.query(startsWith("SELECT id,tenant_id"), any(RowMapper.class), eq(10L)))
            .thenReturn(Arrays.asList(head, foreign, tail));
        ComputeSharedQueueService queue = mock(ComputeSharedQueueService.class);
        when(queue.isSharedJob(head)).thenReturn(true);
        CloudTrainingWorker worker = worker(jdbc, nodes, ssh);
        ReflectionTestUtils.setField(worker, "sharedQueue", queue);

        worker.tick(10L);
        verify(queue).register(connection, node, Collections.singletonList(head));
        verify(nodes, never()).verifyJob(eq(foreign), any());
        verify(nodes, never()).verifyJob(eq(tail), any());
    }

    private CloudTrainingWorker worker(JdbcTemplate jdbc, ComputeNodeService nodes, ComputeSsh ssh) {
        CloudTrainingWorker worker = new CloudTrainingWorker(jdbc, mock(PlatformTransactionManager.class), nodes, ssh,
            mock(CloudTrainingDatasetWriter.class), mock(DataManagementService.class), mock(ModelArtifactObjectStore.class),
            mock(RemoteModelArtifactService.class), new ObjectMapper());
        ModelHubProperties deployment = new ModelHubProperties();
        deployment.setBaseUrl("http://127.0.0.1:8406");
        ComputeCatalogProperties catalog = new ComputeCatalogProperties(deployment);
        catalog.assignEnvironment("localhost");
        ReflectionTestUtils.setField(worker, "catalogProperties", catalog);
        ComputeSharedQueueProperties shared = new ComputeSharedQueueProperties();
        shared.setEnvironmentId("localhost");
        ReflectionTestUtils.setField(worker, "sharedQueueProperties", shared);
        return worker;
    }

    private JdbcTemplate lockedJdbc() throws SQLException {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DataSource source = mock(DataSource.class);
        Connection db = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(jdbc.getDataSource()).thenReturn(source);
        when(source.getConnection()).thenReturn(db);
        when(db.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(1);
        return jdbc;
    }
}
