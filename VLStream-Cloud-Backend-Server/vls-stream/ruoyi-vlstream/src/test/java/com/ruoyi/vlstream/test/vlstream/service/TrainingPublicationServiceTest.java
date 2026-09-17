package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class TrainingPublicationServiceTest {
    JdbcTemplate jdbc; VlsAlgorithmTrainingMapper training; VlsAlgorithmModelMapper models;
    IVlsAlgorithmModelService create; TrainingPublicationService service; AlgorithmTraining task;
    @BeforeEach void setup() {
        jdbc=mock(JdbcTemplate.class);training=mock(VlsAlgorithmTrainingMapper.class);models=mock(VlsAlgorithmModelMapper.class);create=mock(IVlsAlgorithmModelService.class);
        PlatformTransactionManager tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service=new TrainingPublicationService(jdbc,tx,training,models,create);
        when(jdbc.queryForList(contains("SELECT id"),eq("tenant-a"),eq(1L))).thenReturn(Collections.singletonList(Collections.singletonMap("id",1L)));
        when(jdbc.queryForList(contains("SELECT publication_state"),eq(String.class),eq("tenant-a"),eq(1L))).thenReturn(Collections.singletonList("PENDING"));
        when(jdbc.queryForObject(contains("COUNT"),eq(Integer.class),eq("tenant-a"),eq(1L))).thenReturn(0);
        task=new AlgorithmTraining();task.setId(1L);task.setTaskName("helmet");task.setAlgorithmId(2L);task.setModelOutputPath("/run/weights/best.pt");task.setTrainStatus(AlgorithmTrainingStatusEnum.completed);
        when(training.selectById(1L)).thenReturn(task);when(models.selectList(any())).thenReturn(Collections.emptyList());
        task.setOnnxConversionStatus("completed");task.setOmConversionStatus("completed");
        AlgorithmModel model=new AlgorithmModel();model.setId(3L);when(create.createModel(any())).thenReturn(model);
        TenantContextHolder.setTenantId("original");
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void publishesWithoutBrowserAndRestoresTenant() {
        service.publish("tenant-a",1L);verify(create).createModel(any());assertEquals("original",TenantContextHolder.getTenantId());
        verify(jdbc).update(contains("publication_state='COMPLETED'"),eq(3L),eq("/run/weights/best.pt"),eq("tenant-a"),eq(1L));
    }
    @Test void repeatedScanDoesNotCreateAnotherModel() {
        when(jdbc.queryForList(contains("SELECT publication_state"),eq(String.class),eq("tenant-a"),eq(1L))).thenReturn(Collections.singletonList("COMPLETED"));
        service.publish("tenant-a",1L);verifyNoInteractions(create);
    }
    @Test void reusesAlreadyCreatedModelForSameArtifact() {
        AlgorithmModel existing=new AlgorithmModel();existing.setId(9L);when(models.selectList(any())).thenReturn(Collections.singletonList(existing));
        service.publish("tenant-a",1L);verifyNoInteractions(create);
        verify(jdbc).update(contains("publication_state='COMPLETED'"),eq(9L),eq("/run/weights/best.pt"),eq("tenant-a"),eq(1L));
    }
    @Test void conversionMustFinishBeforePublication() {
        when(jdbc.queryForObject(contains("COUNT"),eq(Integer.class),eq("tenant-a"),eq(1L))).thenReturn(1);
        service.publish("tenant-a",1L);verifyNoInteractions(create,models);
    }
    @Test void failureRecordsRetryStateAndPreservesTenant() {
        when(create.createModel(any())).thenThrow(new IllegalStateException("offline"));
        service.publish("tenant-a",1L);verify(jdbc).update(contains("attempts=attempts+1"),eq("tenant-a"),eq(1L));assertEquals("original",TenantContextHolder.getTenantId());
    }
}
