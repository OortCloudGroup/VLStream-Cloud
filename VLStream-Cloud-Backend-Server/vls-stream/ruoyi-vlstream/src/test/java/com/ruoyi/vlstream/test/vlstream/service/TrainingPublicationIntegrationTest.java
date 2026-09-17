package com.ruoyi.vlstream.test.vlstream.service;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named="VLS_TRAINING_TEST_JDBC",matches="jdbc:mysql://127\\.0\\.0\\.1:33319/training_contract_test.*")
class TrainingPublicationIntegrationTest {
    static JdbcTemplate jdbc; static DriverManagerDataSource source;
    TrainingPublicationService service; IVlsAlgorithmModelService create;
    @BeforeAll static void initialize() throws Exception {
        source=new DriverManagerDataSource(System.getenv("VLS_TRAINING_TEST_JDBC"),"root","training-test-only");jdbc=new JdbcTemplate(source);
        for(String table:Arrays.asList("vls_training_publication","vls_algorithm_training","vls_container_instance","vls_dataset_conversion_guard","test_models")) jdbc.execute("DROP TABLE IF EXISTS "+table);
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),is_deleted INT DEFAULT 0,train_status VARCHAR(20),model_output_path TEXT,onnx_model_output_path TEXT,om_model_output_path TEXT,rknn_model_output_path TEXT,int8_rknn_model_output_path TEXT,onnx_conversion_status VARCHAR(20),om_conversion_status VARCHAR(20),start_time DATETIME,end_time DATETIME,epoch_current INT)");
        jdbc.execute("CREATE TABLE vls_container_instance(tenant_id VARCHAR(64),training_task_id BIGINT,instance_status VARCHAR(20))");
        jdbc.execute("CREATE TABLE vls_dataset_conversion_guard(tenant_id VARCHAR(64),training_id BIGINT)");
        jdbc.execute("CREATE TABLE test_models(id BIGINT PRIMARY KEY,model_path TEXT)");
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();while(!Files.exists(root.resolve("BUSINESS_PROCESSES.md")))root=root.getParent();
        String sql=new String(Files.readAllBytes(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_018__training_auto_publish.sql")),StandardCharsets.UTF_8);
        jdbc.execute(sql);
    }
    @BeforeEach void setup() {
        for(String table:Arrays.asList("vls_training_publication","vls_algorithm_training","vls_container_instance","vls_dataset_conversion_guard","test_models"))jdbc.update("DELETE FROM "+table);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,train_status,model_output_path) VALUES(1,'a','completed','/model.pt')");
        jdbc.update("INSERT INTO vls_training_publication(tenant_id,training_id,publication_state) VALUES('a',1,'PENDING')");
        VlsAlgorithmTrainingMapper tasks=mock(VlsAlgorithmTrainingMapper.class);VlsAlgorithmModelMapper models=mock(VlsAlgorithmModelMapper.class);create=mock(IVlsAlgorithmModelService.class);
        AlgorithmTraining task=new AlgorithmTraining();task.setId(1L);task.setTenantId("a");task.setTaskName("helmet");task.setModelOutputPath("/model.pt");task.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
        when(tasks.selectById(1L)).thenReturn(task);when(models.selectList(any())).thenReturn(Collections.emptyList());
        task.setOnnxConversionStatus("completed");task.setOmConversionStatus("completed");
        when(create.createModel(any())).thenAnswer(call->{jdbc.update("INSERT INTO test_models VALUES(10,'/model.pt')");AlgorithmModel m=new AlgorithmModel();m.setId(10L);return m;});
        service=new TrainingPublicationService(jdbc,new DataSourceTransactionManager(source),tasks,models,create);TenantContextHolder.setTenantId("a");
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void scansDurableIntentAndPublishesExactlyOnce() {
        service.scan();service.scan();assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM test_models",Integer.class));assertEquals("COMPLETED",state());
        assertEquals("10",service.status(1L).get("model_id"));
    }
    @Test void failureRollsBackModelAndCanBeRetried() {
        doAnswer(call->{jdbc.update("INSERT INTO test_models VALUES(10,'/model.pt')");throw new IllegalStateException("simulate failure");}).when(create).createModel(any());
        service.scan();service.scan();service.scan();assertEquals("FAILED",state());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM test_models",Integer.class));
        service.retry(1L);assertEquals("PENDING",state());assertEquals(0,service.status(1L).get("attempts"));
    }
    @Test void activeContainerBlocksRestartAndAnotherTenantCannotMutateIntent() {
        jdbc.update("UPDATE vls_training_publication SET publication_state='DISABLED'");jdbc.update("INSERT INTO vls_container_instance VALUES('a',1,'queued')");
        assertThrows(RuntimeException.class,()->service.lockForStart(1L));
        TenantContextHolder.setTenantId("b");assertThrows(RuntimeException.class,()->service.lockForStart(1L));assertEquals("DISABLED",service.status(1L).get("publication_state"));
    }
    @Test void configureClearsOldRunPointersButKeepsIndependentModels() {
        jdbc.update("INSERT INTO test_models VALUES(10,'/model.pt')");service.configure(1L,false);
        assertEquals("DISABLED",state());assertNull(jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1",String.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM test_models",Integer.class));
    }
    String state(){return jdbc.queryForObject("SELECT publication_state FROM vls_training_publication WHERE tenant_id='a' AND training_id=1",String.class);}
}
