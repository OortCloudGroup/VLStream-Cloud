package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class SmartAnnotationComputeServiceTest {
    JdbcTemplate jdbc; ComputeNodeService nodes; ComputeSharedQueueService shared;
    SmartAnnotationComputeService service; ComputeNode node; SmartAnnotationTask task; SmartAnnotationRound round;
    @BeforeEach void setup() {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:smart"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36),tenant_id VARCHAR(64),training_id BIGINT,smart_round_id BIGINT,node_id BIGINT,container_record_id BIGINT,dataset_id BIGINT,version_id BIGINT,annotation_type VARCHAR(32),snapshot_json CLOB,base_model VARCHAR(2000),options_json CLOB,run_dir VARCHAR(2000),public_instance_key VARCHAR(200),public_claim_id VARCHAR(100),public_owner_subject VARCHAR(100),job_state VARCHAR(20) DEFAULT 'QUEUED',cancel_requested INT DEFAULT 0,progress INT DEFAULT 0,message VARCHAR(500),log_tail CLOB,create_time TIMESTAMP DEFAULT NOW(),update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_smart_annotation_round(id BIGINT,tenant_id VARCHAR(64),task_id BIGINT,work_directory VARCHAR(2000),round_state VARCHAR(20),error_message VARCHAR(500))");
        jdbc.execute("CREATE TABLE vls_smart_annotation_task(id BIGINT,tenant_id VARCHAR(64),task_state VARCHAR(20),error_message VARCHAR(500))");
        jdbc.update("INSERT INTO vls_smart_annotation_round(id,tenant_id,task_id) VALUES(2,'tenant',1)");
        jdbc.update("INSERT INTO vls_smart_annotation_task VALUES(1,'tenant','QUEUED',NULL)");
        nodes=mock(ComputeNodeService.class);shared=mock(ComputeSharedQueueService.class);
        IVlsContainerInstanceService containers=mock(IVlsContainerInstanceService.class);
        when(containers.saveTaskRecord(any())).thenAnswer(call->{com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance c=call.getArgument(0);c.setId(3L);return true;});
        service=new SmartAnnotationComputeService(jdbc,nodes,shared,containers,new ObjectMapper(),mock(ModelArtifactObjectStore.class));
        node=new ComputeNode();node.setId(10L);node.setWorkDir("/fixture/localhost");node.setResourceScope("PLATFORM");
        when(nodes.requireReady(10L)).thenReturn(node);when(nodes.isCompanyShared(node)).thenReturn(true);
        when(nodes.trainingSubject(node,"7")).thenReturn("owner");
        when(shared.verifyBaseModel(anyString(),anyString(),eq("owner"))).thenAnswer(c->c.getArgument(0));
        when(shared.configuration(eq(node),anyString(),eq("owner"))).thenReturn(java.util.Collections.singletonMap("mode",ComputeSharedQueueService.MODE));
        task=new SmartAnnotationTask();task.setId(1L);task.setTenantId("tenant");task.setCreateUser("7");task.setDatasetId(20L);task.setComputeNodeId(10L);task.setMode("active");task.setModelPath("@preset/detect");task.setEpochs(10);
        round=new SmartAnnotationRound();round.setId(2L);round.setVersionId(30L);
    }
    CloudTrainingJob job() { return jdbc.queryForObject("SELECT * FROM vls_cloud_training_job",new BeanPropertyRowMapper<>(CloudTrainingJob.class)); }
    @ParameterizedTest @ValueSource(strings={"image_classification","object_detection","instance_segmentation","semantic_segmentation"})
    void fourTypesUseDurableNodeQueueAndKeepFormalTrainingSeparate(String kind) throws Exception {
        task.setAnnotationType(kind);task.setModelPath("@preset/"+AnnotationTaskType.of(kind).getModelTask());
        service.enqueue(task,round,new DatasetSnapshot());
        CloudTrainingJob job=job();assertNull(job.getTrainingId());assertEquals(2L,job.getSmartRoundId());assertEquals("owner",job.getPublicOwnerSubject());
        assertEquals(kind,job.getAnnotationType());assertTrue(job.isAnnotation());
        assertEquals(ComputeSharedQueueService.MODE,new ObjectMapper().readTree(job.getOptionsJson()).path("sharedQueue").path("mode").asText());
        verify(nodes).lockServer(10L);verify(shared).verifyBaseModel(task.getModelPath(),kind,"owner");
    }
    @Test void unavailableOrUnauthorizedNodeNeverCreatesJob() {
        when(nodes.requireReady(10L)).thenThrow(new ServiceException("node unavailable"));
        assertThrows(ServiceException.class,()->service.enqueue(task,round,new DatasetSnapshot()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
    }
    @Test void privateSpecifiedModelTaskUsesTheSelectedNodeWithoutSharedLeaseIdentity() throws Exception {
        node.setResourceScope("PRIVATE");when(nodes.isCompanyShared(node)).thenReturn(false);
        when(nodes.trainingSubject(node,"7")).thenReturn(null);task.setMode("model");
        service.enqueue(task,round,new DatasetSnapshot());
        assertNull(job().getPublicOwnerSubject());
        assertEquals("model",new ObjectMapper().readTree(job().getOptionsJson()).path("mode").asText());
        assertFalse(new ObjectMapper().readTree(job().getOptionsJson()).has("sharedQueue"));
        verifyNoInteractions(shared);
    }
    @Test void sharedUntrustedWeightsAreRejectedBeforeCreatingExecution() {
        task.setModelPath("/untrusted.pt");when(shared.verifyBaseModel(anyString(),anyString(),anyString())).thenThrow(new ServiceException("untrusted"));
        assertThrows(ServiceException.class,()->service.enqueue(task,round,new DatasetSnapshot()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
    }
    @Test void cancellationIsDurableAndCannotBeOverwrittenByRunningPhase() {
        service.enqueue(task,round,new DatasetSnapshot());CloudTrainingJob job=job();
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='CANCEL_REQUESTED'");
        service.cancel(task,2L);service.phase(job,"RUNNING","running");
        assertTrue(job().isCancelRequested());assertEquals("CANCEL_REQUESTED",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
        service.finished(job,"COMPLETED",null);
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
    }
    @Test void resultBecomesReviewableOnlyAtQueueCompletion() {
        service.enqueue(task,round,new DatasetSnapshot());CloudTrainingJob job=job();
        service.phase(job,"ARCHIVING","archiving");assertEquals("RUNNING",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
        service.finished(job,"COMPLETED",null);assertEquals("REVIEW",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
    }
}
