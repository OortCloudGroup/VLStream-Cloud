package com.ruoyi.vlstream.test.vlstream.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ModelArtifactObjectStoreTest {
    private JdbcTemplate jdbc;
    private OssClient client;
    private ModelArtifactObjectStore store;
    private Path temp;

    @BeforeEach void setup() throws Exception {
        jdbc = mock(JdbcTemplate.class);
        client = mock(OssClient.class);
        store = spy(new ModelArtifactObjectStore(jdbc));
        doReturn(client).when(store).client(any());
        doReturn(null).when(store).find(anyString(), anyString());
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(1);
        temp = Paths.get(System.getProperty("model.test.dir", "../../codex/model-minio"));
        Files.createDirectories(temp);
        ReflectionTestUtils.setField(store, "tempDir", temp.toAbsolutePath().toString());
        TenantContextHolder.setTenantId("tenant-a");
        when(client.getConfigKey()).thenReturn("minio");
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @Test void publishesOnlyAfterReadbackHashAndSizeMatch() throws Exception {
        byte[] bytes = {1, 2, 3};
        ObjectMetadata metadata = new ObjectMetadata(); metadata.setContentLength(3);
        when(client.getObjectMetadata(anyString())).thenReturn(metadata);
        when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(bytes));
        assertTrue(store.archive("/run/best.pt", "best.pt", local -> Files.write(local, bytes)));
        verify(jdbc).update(contains("storage_state='READY'"), eq("minio"), contains("models/"), eq("best.pt"), eq(3L), eq(ModelArtifactObjectStore.digest(new ByteArrayInputStream(bytes))), eq("tenant-a"), anyString(), anyString());
        try (java.util.stream.Stream<Path> files = Files.list(temp)) { assertFalse(files.anyMatch(p -> p.toString().endsWith(".part"))); }
    }

    @Test void checksumMismatchRemainsRetryableAndDoesNotPublish() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata(); metadata.setContentLength(3);
        when(client.getObjectMetadata(anyString())).thenReturn(metadata);
        when(client.getObjectContent(anyString())).thenReturn(new ByteArrayInputStream(new byte[]{3, 2, 1}));
        assertThrows(IOException.class, () -> store.archive("/run/best.pt", "best.pt", local -> Files.write(local, new byte[]{1, 2, 3})));
        verify(jdbc).update(contains("storage_state='FAILED'"), anyString(), eq("tenant-a"), anyString(), anyString());
        verify(jdbc, never()).update(contains("storage_state='READY'"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test void readyArtifactNeverReadsRemoteAgain() throws Exception {
        doReturn(new ModelArtifactObjectStore.StoredArtifact("minio", "key", "best.pt", 3, "hash")).when(store).find("tenant-a", "/run/best.pt");
        assertTrue(store.archive("/run/best.pt", "best.pt", local -> { throw new IOException("SSH offline"); }));
        verifyNoInteractions(client, jdbc);
    }

    @Test void missingTenantCannotReadOrArchive() {
        TenantContextHolder.clear();
        assertThrows(IllegalStateException.class, () -> store.find("/run/best.pt"));
        assertThrows(IllegalStateException.class, () -> store.archive("/run/best.pt", "best.pt", local -> {}));
    }

    @Test void archivedDownloadsUseMinioAndTaskTenantWithoutSsh() throws Exception {
        RemoteModelArtifactService artifacts = new RemoteModelArtifactService();
        SSHService ssh = mock(SSHService.class);
        ReflectionTestUtils.setField(artifacts, "objectStore", store);
        ReflectionTestUtils.setField(artifacts, "sshService", ssh);
        ModelArtifactObjectStore.StoredArtifact stored = new ModelArtifactObjectStore.StoredArtifact("minio", "key", "best.pt", 3, "abc");
        doReturn(stored).when(store).find("tenant-a", "/run/best.pt");
        when(client.getObjectContent("key")).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        TenantContextHolder.clear();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        artifacts.streamForTenant("tenant-a", "/run/best.pt", "abc", bytes);
        assertArrayEquals(new byte[]{1, 2, 3}, bytes.toByteArray());
        assertThrows(IOException.class, () -> artifacts.streamForTenant("tenant-a", "/run/best.pt", "different", bytes));
        verifyNoInteractions(ssh);
    }

    @Test void activeLeaseDoesNotStartAnotherUpload() throws Exception {
        when(jdbc.update(contains("storage_state='COPYING'"), any(), any(), any())).thenReturn(0);
        assertFalse(store.archive("/run/best.pt", "best.pt", local -> fail("Lease must exclude another worker")));
        verifyNoInteractions(client);
    }

    @Test void archiveContinuesAfterMissingFileAndRestoresTenant() throws Exception {
        ModelArtifactObjectStore archiveStore = mock(ModelArtifactObjectStore.class);
        when(archiveStore.archive(eq("/missing.pt"), anyString(), any())).thenThrow(new IOException("missing"));
        ModelArtifactArchiveService service = new ModelArtifactArchiveService(jdbc, archiveStore, mock(RemoteModelArtifactService.class), mock(ModelClassFileService.class));
        Map<String,Object> row = new HashMap<>();
        row.put("tenant_id", "tenant-b"); row.put("training_id", 1L); row.put("pt", "/missing.pt"); row.put("onnx", "/good.onnx");
        service.archiveRow(row);
        verify(archiveStore).archive(eq("/good.onnx"), eq("good.onnx"), any());
        verify(archiveStore).archive(eq(ModelClassFileService.storagePath("/missing.pt")), eq("classes.yaml"), any());
        assertEquals("tenant-a", TenantContextHolder.getTenantId());
        service.close();
    }

    @Test void browserDownloadStreamsThroughArtifactService() throws Exception {
        ModelFileDownloadService download = new ModelFileDownloadService();
        IVlsAlgorithmTrainingService trainingService = mock(IVlsAlgorithmTrainingService.class);
        RemoteModelArtifactService artifacts = mock(RemoteModelArtifactService.class);
        ReflectionTestUtils.setField(download, "algorithmTrainingService", trainingService);
        ReflectionTestUtils.setField(download, "artifactService", artifacts);
        AlgorithmTraining training = new AlgorithmTraining(); training.setModelOutputPath("/run/best.pt");
        when(trainingService.getById(1L)).thenReturn(training);
        when(artifacts.inspect("/run/best.pt")).thenReturn(new RemoteModelArtifactService.ArtifactMetadata("best.pt", 3, "abc"));
        doAnswer(call -> { ((java.io.OutputStream)call.getArgument(1)).write(new byte[]{1,2,3}); return null; }).when(artifacts).stream(eq("/run/best.pt"), any());
        MockHttpServletResponse response = new MockHttpServletResponse();
        download.downloadTrainingModel(1L, "pt", response);
        assertArrayEquals(new byte[]{1,2,3}, response.getContentAsByteArray());
        assertEquals("3", response.getHeader("Content-Length"));
        assertEquals("abc", response.getHeader("X-Model-SHA256"));
    }
}
