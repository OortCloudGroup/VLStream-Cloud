package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class DataTrainingPublisherObjectStoreTest {
    DataManagementService data;
    TrainingDatasetArtifactService storage;
    PlatformTransactionManager transactions;
    DatasetVersion version;
    TrainingDatasetArtifact artifact;
    DataTrainingPublisher publisher;
    DatasetGenerationPreflight preflight;
    DatasetGenerationReport report;
    @BeforeEach void setUp() throws Exception {
        data = mock(DataManagementService.class); storage = mock(TrainingDatasetArtifactService.class); transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setAnnotationType("object_detection"); snapshot.setSamples(java.util.Collections.emptyList());
        when(data.snapshot(1L)).thenReturn(snapshot);
        version = new DatasetVersion(); version.setId(2L);
        when(data.savePreparedTrainingVersion(eq(1L), any(DatasetGenerationReport.class))).thenReturn(version);
        artifact = new TrainingDatasetArtifact(); artifact.setId("11111111-1111-1111-1111-111111111111");
        when(storage.ensure(1L, version)).thenReturn(artifact);
        preflight = mock(DatasetGenerationPreflight.class); report = new DatasetGenerationReport();
        when(preflight.check(eq(1L), any())).thenReturn(report);
        publisher = new DataTrainingPublisher(data, storage, preflight);
    }
    @Test void publishesReferenceOnlyAfterFrozenVersionCommittedAndObjectVerified() throws Exception {
        assertTrue(publisher.publish(1L));
        org.mockito.InOrder order = inOrder(data, preflight, storage);
        order.verify(data).snapshot(1L);
        order.verify(preflight).check(eq(1L), any());
        order.verify(data).savePreparedTrainingVersion(1L, report);
        order.verify(storage).ensure(1L, version);
        order.verify(data).recordPublishedDataset(1L, 2L, artifact.getReference());
    }
    @Test void failedArchiveDoesNotMakeDatasetReady() throws Exception {
        when(storage.ensure(1L, version)).thenThrow(new IOException("upload failed"));
        assertThrows(ServiceException.class, () -> publisher.publish(1L));
        verify(data, never()).recordPublishedDataset(anyLong(), anyLong(), anyString());
    }
    @Test void concurrentDatasetEditDoesNotReplaceTheCurrentReference() {
        doThrow(new ServiceException("changed working set")).when(data).recordPublishedDataset(1L, 2L, artifact.getReference());
        assertThrows(ServiceException.class, () -> publisher.publish(1L));
        verify(storage, never()).listForDataset(anyLong());
    }
    @Test void blockedPreflightReturnsAllIssuesWithoutFreezingOrUploading() throws Exception {
        report.setStatus("BLOCKED"); report.getErrors().add(DataManagementService.map("imageId", "2096777217777467393"));
        assertEquals("BLOCKED", publisher.generate(1L).getStatus());
        verify(data, never()).savePreparedTrainingVersion(anyLong(), any());
        verify(storage, never()).ensure(anyLong(), any());
    }
}
