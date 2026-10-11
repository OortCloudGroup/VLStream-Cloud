package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class OmConversionIntegrationTest {
    private JdbcTemplate jdbc;
    private OmConversionService service;
    private IVlsAlgorithmTrainingService trainings;
    private AlgorithmTraining training;
    @BeforeEach void setup() {
        String url=System.getenv("VLS_OM_TEST_JDBC");
        Assumptions.assumeTrue(url != null, "Dedicated test database required");
        DriverManagerDataSource ds=new DriverManagerDataSource(url,"root","om-test-only");
        jdbc=new JdbcTemplate(ds);
        for(String table:new String[]{"vls_om_conversion_job","vls_cloud_training_job","vls_dataset_conversion_guard","vls_algorithm_model","vls_algorithm_training"}) jdbc.execute("DROP TABLE IF EXISTS "+table);
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_id BIGINT,model_output_path VARCHAR(500),config_params JSON,om_conversion_status VARCHAR(20),om_conversion_error VARCHAR(500),om_model_output_path VARCHAR(500),is_deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE vls_algorithm_model(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_id BIGINT,model_path VARCHAR(500),om_model_output_path VARCHAR(500),is_deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE vls_dataset_conversion_guard(tenant_id VARCHAR(64),training_id BIGINT,dataset_id BIGINT,PRIMARY KEY(tenant_id,training_id))");
        jdbc.execute("CREATE TABLE vls_om_conversion_job(id CHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),training_id BIGINT,source_job_id CHAR(36),source_pt_path VARCHAR(500),dataset_id BIGINT,dataset_ref VARCHAR(100),job_state VARCHAR(20) DEFAULT 'QUEUED',message VARCHAR(500),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id CHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),training_id BIGINT,dataset_id BIGINT)");
        jdbc.update("INSERT INTO vls_cloud_training_job VALUES('run','a',1,2)");
        training=new AlgorithmTraining();training.setId(1L);training.setDatasetId(2L);training.setTenantId("a");
        training.setTrainStatus(AlgorithmTrainingStatusEnum.completed);training.setModelOutputPath("cloud-training/a/run/best.pt");
        training.setConfigParams("{\"cloudJobId\":\"run\",\"trainType\":\"detect\",\"datasetArtifactRef\":\"vls-dataset://ref\"}");
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,dataset_id,model_output_path,config_params) VALUES(1,'a',2,?,?)",training.getModelOutputPath(),training.getConfigParams());
        jdbc.update("INSERT INTO vls_algorithm_model(id,tenant_id,training_id,model_path) VALUES(10,'a',1,?),(11,'a',1,'previous.pt'),(12,'b',1,?)",training.getModelOutputPath(),training.getModelOutputPath());
        OmConversionProperties properties=new OmConversionProperties();properties.setEnabled(true);
        trainings=mock(IVlsAlgorithmTrainingService.class);when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(training);
        ModelArtifactObjectStore objects=mock(ModelArtifactObjectStore.class);
        when(objects.find(training.getModelOutputPath())).thenReturn(new ModelArtifactObjectStore.StoredArtifact("config","key","best.pt",1,"sha"));
        service=new OmConversionService(properties,jdbc,new DataSourceTransactionManager(ds),trainings,mock(DataManagementService.class),new DatasetConversionGuard(jdbc),mock(TrainingDatasetArtifactService.class),objects,new ObjectMapper());
        TenantContextHolder.setTenantId("a");
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();if(service!=null)service.shutdown();}
    @Test void repeatEnqueueIsIdempotentAndCompletionUpdatesOnlyMatchingModelVersion() {
        training.setTenantId(null); // Real legacy detail mapper does not return tenant_id.
        String id=service.enqueue(1L,false);assertEquals(id,service.enqueue(1L,true));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_om_conversion_job",Integer.class));
        OmConversionService.Job job=job(id);
        ReflectionTestUtils.invokeMethod(service,"finish",job,"cloud-training/a/run/model.om",null);
        assertEquals("completed",jdbc.queryForObject("SELECT om_conversion_status FROM vls_algorithm_training WHERE id=1",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_algorithm_model WHERE om_model_output_path IS NOT NULL",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_dataset_conversion_guard",Integer.class));
    }
    @Test void failurePreservesPtAndAllowsExplicitRetryWithoutAutomaticRetryLoop() {
        String first=service.enqueue(1L,false);
        ReflectionTestUtils.invokeMethod(service,"finish",job(first),null,"unsupported head");
        assertEquals(training.getModelOutputPath(),jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1",String.class));
        assertNull(service.enqueue(1L,false));
        assertNotEquals(first,service.enqueue(1L,true));
    }
    @Test void foreignTenantIsRejectedBeforeQueueMutation() {
        TenantContextHolder.setTenantId("b");
        assertThrows(ServiceException.class,()->service.enqueue(1L,true));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_om_conversion_job",Integer.class));
    }
    @Test void staleSourceCannotOverwriteNewTrainingOrHistoricalModels() {
        String id=service.enqueue(1L,false);
        jdbc.update("UPDATE vls_algorithm_training SET model_output_path='new.pt' WHERE id=1");
        assertThrows(ServiceException.class,()->ReflectionTestUtils.invokeMethod(service,"finish",job(id),"wrong.om",null));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_algorithm_model WHERE om_model_output_path IS NOT NULL",Integer.class));
    }
    @Test void rejectedHistoricalJobIsRecordedOnceAndCannotOverrideAnActiveRetry() {
        service.recordRejected("a",1L,"数据集已删除");
        service.recordRejected("a",1L,"数据集已删除");
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_om_conversion_job",Integer.class));
        assertEquals("failed",jdbc.queryForObject("SELECT om_conversion_status FROM vls_algorithm_training WHERE id=1",String.class));
        service.enqueue(1L,true);
        service.recordRejected("a",1L,"过时的检查结果");
        assertEquals("converting",jdbc.queryForObject("SELECT om_conversion_status FROM vls_algorithm_training WHERE id=1",String.class));
    }
    private OmConversionService.Job job(String id){OmConversionService.Job j=new OmConversionService.Job();j.setId(id);j.setTenantId("a");j.setTrainingId(1L);j.setDatasetId(2L);j.setSourceJobId("run");j.setSourcePtPath(training.getModelOutputPath());return j;}
}


