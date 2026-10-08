package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import javax.sql.DataSource;
import java.sql.*;
import java.io.IOException;
import java.util.Collections;
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
}
