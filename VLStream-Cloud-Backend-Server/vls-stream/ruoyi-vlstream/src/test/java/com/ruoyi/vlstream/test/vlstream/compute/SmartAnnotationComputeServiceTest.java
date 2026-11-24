package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
        jdbc.execute("CREATE TABLE vls_smart_annotation_task(id BIGINT,tenant_id VARCHAR(64),task_state VARCHAR(20),error_message VARCHAR(500),"
            + "bulk_cursor BIGINT,bulk_total INT,bulk_processed INT,bulk_accepted INT,bulk_conflicts INT,bulk_error VARCHAR(500))");
        jdbc.execute("CREATE TABLE vls_smart_annotation_candidate(round_id BIGINT,tenant_id VARCHAR(64),review_state VARCHAR(20),review_error VARCHAR(500),is_deleted INT)");
        jdbc.update("INSERT INTO vls_smart_annotation_round(id,tenant_id,task_id) VALUES(2,'tenant',1)");
        jdbc.update("INSERT INTO vls_smart_annotation_task(id,tenant_id,task_state) VALUES(1,'tenant','QUEUED')");
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
        service.enqueue(task,round,new DatasetSnapshot(),"7");
        CloudTrainingJob job=job();assertNull(job.getTrainingId());assertEquals(2L,job.getSmartRoundId());assertEquals("owner",job.getPublicOwnerSubject());
        assertEquals(kind,job.getAnnotationType());assertTrue(job.isAnnotation());
        assertTrue(new ObjectMapper().readTree(job.getOptionsJson()).path("autoConfirm").asBoolean());
        assertEquals(ComputeSharedQueueService.MODE,new ObjectMapper().readTree(job.getOptionsJson()).path("sharedQueue").path("mode").asText());
        verify(nodes).lockServer(10L);verify(shared).verifyBaseModel(task.getModelPath(),kind,"owner");
    }
    @Test void unavailableOrUnauthorizedNodeNeverCreatesJob() {
        when(nodes.requireReady(10L)).thenThrow(new ServiceException("node unavailable"));
        assertThrows(ServiceException.class,()->service.enqueue(task,round,new DatasetSnapshot(),"7"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
    }
    @Test void privateSpecifiedModelTaskUsesTheSelectedNodeWithoutSharedLeaseIdentity() throws Exception {
        node.setResourceScope("PRIVATE");when(nodes.isCompanyShared(node)).thenReturn(false);
        when(nodes.trainingSubject(node,"7")).thenReturn(null);task.setMode("model");
        service.enqueue(task,round,new DatasetSnapshot(),"7");
        assertNull(job().getPublicOwnerSubject());
        assertEquals("model",new ObjectMapper().readTree(job().getOptionsJson()).path("mode").asText());
        assertFalse(new ObjectMapper().readTree(job().getOptionsJson()).has("sharedQueue"));
        verifyNoInteractions(shared);
    }
    @Test void sharedUntrustedWeightsAreRejectedBeforeCreatingExecution() {
        task.setModelPath("/untrusted.pt");when(shared.verifyBaseModel(anyString(),anyString(),anyString())).thenThrow(new ServiceException("untrusted"));
        assertThrows(ServiceException.class,()->service.enqueue(task,round,new DatasetSnapshot(),"7"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
    }
    @Test void cancellationIsDurableAndCannotBeOverwrittenByRunningPhase() {
        service.enqueue(task,round,new DatasetSnapshot(),"7");CloudTrainingJob job=job();
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='CANCEL_REQUESTED'");
        service.cancel(task,2L);service.phase(job,"RUNNING","running");
        assertTrue(job().isCancelRequested());assertEquals("CANCEL_REQUESTED",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
        service.finished(job,"COMPLETED",null,true);
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
    }
    @Test void resultBecomesReviewableOnlyAtQueueCompletion() {
        service.enqueue(task,round,new DatasetSnapshot(),"7");CloudTrainingJob job=job();
        service.phase(job,"ARCHIVING","archiving");assertEquals("RUNNING",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
        service.finished(job,"COMPLETED",null,false);assertEquals("REVIEW",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
    }
    @Test void onlyEligibleCurrentRoundPredictionsStartAutomaticConfirmation() {
        service.enqueue(task,round,new DatasetSnapshot(),"7");CloudTrainingJob job=job();
        jdbc.update("INSERT INTO vls_smart_annotation_candidate VALUES "
            + "(2,'tenant','PENDING',NULL,0),(2,'tenant','PENDING','low confidence',0),"
            + "(2,'tenant','ACCEPTED',NULL,0),(2,'tenant','PENDING',NULL,1),"
            + "(2,'other','PENDING',NULL,0),(3,'tenant','PENDING',NULL,0)");
        service.finished(job,"COMPLETED",null,true);
        assertEquals("AUTO_CONFIRMING",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT bulk_total FROM vls_smart_annotation_task",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT bulk_processed FROM vls_smart_annotation_task",Integer.class));
    }
    @Test void noEligiblePredictionsLeavesTaskOpenForManualReview() {
        service.enqueue(task,round,new DatasetSnapshot(),"7");CloudTrainingJob job=job();
        jdbc.update("INSERT INTO vls_smart_annotation_candidate VALUES (2,'tenant','PENDING','low confidence',0)");
        service.finished(job,"COMPLETED",null,true);
        assertEquals("REVIEW",jdbc.queryForObject("SELECT task_state FROM vls_smart_annotation_task",String.class));
    }

    @Test void teammateRequeueUsesOwnSubjectAndKeepsExecutionDiagnosticsPrivate() throws Exception {
        when(nodes.trainingSubject(node,"8")).thenReturn("teammate-owner");
        when(shared.verifyBaseModel(anyString(),anyString(),eq("teammate-owner"))).thenAnswer(call -> call.getArgument(0));
        service.enqueue(task,round,new DatasetSnapshot(),"8");
        verify(nodes).trainingSubject(node,"8");
        verify(nodes, never()).trainingSubject(node,"7");
        assertEquals("7", task.getCreateUser());
        assertEquals("teammate-owner", job().getPublicOwnerSubject());
        assertEquals("8", new ObjectMapper().readTree(job().getOptionsJson()).get("executionUserId").asText());
        jdbc.update("UPDATE vls_cloud_training_job SET message=?, log_tail=?", "private path", "private execution log");

        SaTokenContext previous = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        MockHttpServletRequest request = new MockHttpServletRequest();
        try {
            SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request), new SaResponseForServlet(new MockHttpServletResponse()), new SaStorageForServlet(request));
            SaHolder.getStorage().set(LoginHelper.USER_KEY,"7");
            assertEquals("QUEUED", service.execution(task,2L).get("state"));
            assertFalse(service.execution(task,2L).containsKey("message"));
            assertThrows(ServiceException.class, () -> service.logs(task,2L));
            SaHolder.getStorage().set(LoginHelper.USER_KEY,"8");
            assertEquals("private path", service.execution(task,2L).get("message"));
            assertEquals("private execution log", service.logs(task,2L));
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
            SaManager.setSaTokenContext(previous);
        }
    }
}
