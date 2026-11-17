package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifact;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifactService;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class HubPublicationArtifactsTest {
    @TempDir Path temp;
    private IVlsAlgorithmModelService models;
    private ModelArtifactObjectStore store;
    private DataManagementService data;
    private TrainingDatasetArtifactService datasets;
    private ComputePublicTaskAccess access;
    private HubPublicationArtifacts service;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId("tenant-a");
        models = mock(IVlsAlgorithmModelService.class); store = mock(ModelArtifactObjectStore.class);
        data = mock(DataManagementService.class); datasets = mock(TrainingDatasetArtifactService.class);
        access = mock(ComputePublicTaskAccess.class);
        service = new HubPublicationArtifacts(models, mock(VlsAlgorithmMapper.class), access, store, data, datasets, new ObjectMapper());
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    @Test void metadataRejectsOversizedEmptyAndMissingHash() {
        assertThrows(ServiceException.class, () -> HubPublicationArtifacts.checkMetadata(0L, hash("a")));
        assertThrows(ServiceException.class, () -> HubPublicationArtifacts.checkMetadata(HubPublicationArtifacts.MAX_FILE + 1, hash("a")));
        assertThrows(ServiceException.class, () -> HubPublicationArtifacts.checkMetadata(1L, "bad"));
    }
    @Test void limitedStreamRejectsExcessBeforeWritingIt() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream output = HubPublicationArtifacts.limited(bytes, 3)) {
            output.write(new byte[] {1,2});
            assertThrows(IOException.class, () -> output.write(new byte[] {3,4}));
            output.write(3);
            assertThrows(IOException.class, () -> output.write(4));
        }
        assertArrayEquals(new byte[] {1,2,3}, bytes.toByteArray());
    }
    @Test void modelReadsOnlyFixedReadyObjectsAndCreatesValidatedManifest() throws Exception {
        model();
        artifact("pt", "weights"); artifact("om", "converted"); artifact("pt.vls-classes.yaml", "names: [person]\nnc: 1\nannotation_type: object_detection\n");
        HubPublicationArtifacts.Source source = service.inspect("model", 1L);
        verify(store, never()).stream(any(), any());
        Path directory;
        try (HubPublicationArtifacts.Prepared prepared = service.prepare("model", 1L)) {
            assertEquals(source.getVersion(), prepared.getVersion());
            assertEquals(4, prepared.getFiles().size());
            Path manifest = prepared.getFiles().get("vls-device-manifest.json"); directory = manifest.getParent();
            assertEquals("Hi3519DV500", new ObjectMapper().readTree(Files.readAllBytes(manifest)).get("target_chip").asText());
            assertEquals(hash("converted"), new ObjectMapper().readTree(Files.readAllBytes(manifest)).get("model").get("sha256").asText());
        }
        assertFalse(Files.exists(directory));
        verify(access, atLeastOnce()).model(1L);
    }
    @Test void missingFixedClassesCannotBorrowCurrentDataset() {
        model(); artifact("pt", "weights");
        assertFalse(service.inspect("model", 1L).isPublishable());
        assertNull(service.inspect("model", 1L).getVersion());
        assertThrows(ServiceException.class, () -> service.prepare("model", 1L));
        verifyNoInteractions(data, datasets);
    }
    @Test void deniedModelAccessPreventsReadingAnyArtifacts() {
        model(); doThrow(new ServiceException("denied")).when(access).model(1L);
        assertThrows(ServiceException.class, () -> service.inspect("model", 1L));
        verifyNoInteractions(store);
    }
    @Test void corruptArchiveIsNotPublishable() throws Exception {
        model(); artifact("pt", "weights"); artifact("pt.vls-classes.yaml", "names: [person]\n");
        doAnswer(invocation -> { ((OutputStream) invocation.getArgument(1)).write("wrong!!".getBytes(StandardCharsets.UTF_8)); return null; }).when(store).stream(argThat(a -> a.getFileName().equals("pt")), any());
        assertThrows(IOException.class, () -> service.prepare("model", 1L));
    }
    @Test void datasetUsesLatestFixedArchiveWithoutGeneratingOrMutatingIt() throws Exception {
        AlgorithmAnnotation project = new AlgorithmAnnotation(); project.setAnnotationName("data");
        when(data.project(2L)).thenReturn(project);
        TrainingDatasetArtifact artifact = new TrainingDatasetArtifact(); artifact.setId("fixed"); artifact.setDatasetId(2L);
        artifact.setFileSize(3L); artifact.setSha256(hash("zip")); artifact.setAnnotationType("object_detection");
        when(datasets.latestReady(2L)).thenReturn(artifact);
        Path downloaded = temp.resolve("data.zip"); Files.write(downloaded, "zip".getBytes(StandardCharsets.UTF_8));
        when(datasets.download(artifact)).thenReturn(downloaded);
        try (HubPublicationArtifacts.Prepared prepared = service.prepare("dataset", 2L)) {
            assertEquals(1, prepared.getFiles().size());
            assertEquals("zip", new String(Files.readAllBytes(prepared.getFiles().get("dataset.zip")), StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(downloaded));
        verify(datasets).latestReady(2L); verify(datasets).download(artifact); verifyNoMoreInteractions(datasets);
    }
    private void model() {
        AlgorithmModel model = new AlgorithmModel(); model.setId(1L); model.setTenantId("tenant-a");
        model.setTrainingId(3L); model.setAlgorithmId(4L); model.setAnnotationType("object_detection");
        model.setModelPath("pt"); model.setOmModelOutputPath("om"); model.setModelName("test");
        when(models.getOne(any())).thenReturn(model);
    }
    private void artifact(String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ModelArtifactObjectStore.StoredArtifact artifact = new ModelArtifactObjectStore.StoredArtifact("config", "key", path, bytes.length, hash(content));
        when(store.find(path)).thenReturn(artifact);
        try { doAnswer(invocation -> { ((OutputStream) invocation.getArgument(1)).write(bytes); return null; }).when(store).stream(eq(artifact), any()); }
        catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    private static String hash(String value) { return ModelArtifactObjectStore.hash(value); }
}
