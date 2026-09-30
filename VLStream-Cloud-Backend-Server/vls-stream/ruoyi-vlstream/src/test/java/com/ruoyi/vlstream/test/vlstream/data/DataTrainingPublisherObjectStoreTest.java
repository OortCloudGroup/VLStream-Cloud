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
    @BeforeEach void setUp() throws Exception {
        data = mock(DataManagementService.class); storage = mock(TrainingDatasetArtifactService.class); transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setAnnotationType("object_detection"); snapshot.setSamples(java.util.Collections.emptyList());
        when(data.snapshot(1L)).thenReturn(snapshot);
        version = new DatasetVersion(); version.setId(2L);
        when(data.saveVersion(eq(1L), any(DataRequests.Version.class))).thenReturn(version);
        artifact = new TrainingDatasetArtifact(); artifact.setId("11111111-1111-1111-1111-111111111111");
        when(storage.ensure(1L, version)).thenReturn(artifact);
        publisher = new DataTrainingPublisher(data, storage, transactions);
    }
    @Test void publishesReferenceOnlyAfterFrozenVersionCommittedAndObjectVerified() throws Exception {
        assertTrue(publisher.publish(1L));
        org.mockito.InOrder order = inOrder(data, transactions, storage);
        order.verify(data).lockDatasetForTask(1L);
        order.verify(data).saveVersion(eq(1L), any(DataRequests.Version.class));
        order.verify(transactions).commit(any());
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
}
