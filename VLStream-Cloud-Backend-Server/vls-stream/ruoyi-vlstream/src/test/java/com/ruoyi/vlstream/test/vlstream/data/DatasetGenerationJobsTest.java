package com.ruoyi.vlstream.test.vlstream.data;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class DatasetGenerationJobsTest {
    DataManagementService data; DataTrainingPublisher publisher; DatasetGenerationJobs jobs; TrainingDatasetArtifactService artifacts;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId("tenant-a");
        data=mock(DataManagementService.class); publisher=mock(DataTrainingPublisher.class);
        when(data.tenant()).thenAnswer(call -> TenantContextHolder.getTenantId());
        artifacts=mock(TrainingDatasetArtifactService.class);
        jobs=new DatasetGenerationJobs(data,publisher,mock(DataMediaStorage.class),artifacts);
    }
    @AfterEach void close(){jobs.close(); TenantContextHolder.clear();}
    @Test void submitsImmediatelyDeduplicatesRunningJobsAndPropagatesTrustedTenant() throws Exception {
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);
        when(publisher.generate(1L)).thenAnswer(call -> {
            assertEquals("tenant-a",TenantContextHolder.getTenantId()); started.countDown(); release.await(2,TimeUnit.SECONDS);
            DatasetGenerationReport report=new DatasetGenerationReport(); report.setStatus("READY"); report.setOriginalJson("large snapshot"); done.countDown();return report;
        });
        DatasetGenerationReport first=jobs.submit(1L);
        assertTrue(started.await(1,TimeUnit.SECONDS)); assertEquals(first.getJobId(),jobs.submit(1L).getJobId());
        assertEquals("RUNNING",jobs.get(1L,first.getJobId()).getStatus());
        release.countDown(); assertTrue(done.await(1,TimeUnit.SECONDS));
    }
    @Test void rejectsCrossTenantAndCrossDatasetProgressReads() throws Exception {
        CountDownLatch gate=new CountDownLatch(1);
        when(publisher.generate(1L)).thenAnswer(call -> {gate.await(1,TimeUnit.SECONDS);return new DatasetGenerationReport();});
        String id=jobs.submit(1L).getJobId();
        assertThrows(ServiceException.class,()->jobs.get(2L,id));
        TenantContextHolder.setTenantId("tenant-b"); assertThrows(ServiceException.class,()->jobs.get(1L,id)); gate.countDown();
    }
    @Test void missingProgressExplainsRestartOrExpiry() {
        ServiceException error=assertThrows(ServiceException.class,()->jobs.get(1L,"missing"));
        assertTrue(error.getMessage().contains("服务已重启"));
    }
    @Test void reviewsStoredReportAfterRestartWithoutPublishingANewDataset() throws Exception {
        TrainingDatasetArtifact saved=new TrainingDatasetArtifact(); saved.setId("11111111-1111-1111-1111-111111111111"); saved.setDatasetId(1L);
        when(artifacts.latestReady(1L)).thenReturn(saved);
        DatasetGenerationReport stored=new DatasetGenerationReport(); stored.setStatus("READY"); stored.setDatasetId("1"); stored.setCheckedImages(485);
        when(artifacts.readGenerationReport(1L,saved.getReference())).thenReturn(stored);
        String id=jobs.review(1L).getJobId();
        for(int attempt=0;attempt<100 && !"READY".equals(jobs.get(1L,id).getStatus());attempt++) Thread.sleep(10);
        assertEquals(485,jobs.get(1L,id).getCheckedImages());
        verifyNoInteractions(publisher); verify(data,never()).savePreparedTrainingVersion(anyLong(),any());
    }
    @Test void noStoredReportDoesNotFallBackToGeneratingOrCrossTenantProgress() {
        when(artifacts.latestReady(1L)).thenThrow(new ServiceException("no report"));
        assertThrows(ServiceException.class,()->jobs.review(1L));
        verifyNoInteractions(publisher);
    }
}
