package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifact;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifactService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ModelClassFileServiceDatasetArtifactTest {
    private static final String REFERENCE = "vls-dataset://67f3bd17-aa76-41bb-93e7-c3c1b7ef1ee4";
    private static final String PT_PATH = "/runs/task/weights/best.pt";
    private static final String YAML = "nc: 2\r\nnames: {0: 人员, 1: 安全绳}\r\n";

    private final RemoteModelArtifactService remote = mock(RemoteModelArtifactService.class);
    private final ModelClassSnapshotStore snapshots = mock(ModelClassSnapshotStore.class);
    private final ModelArtifactObjectStore models = mock(ModelArtifactObjectStore.class);
    private final TrainingDatasetArtifactService datasets = mock(TrainingDatasetArtifactService.class);
    private final ModelClassFileService service = new ModelClassFileService();
    private final AlgorithmTraining training = new AlgorithmTraining();

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "artifactService", remote);
        ReflectionTestUtils.setField(service, "snapshotStore", snapshots);
        ReflectionTestUtils.setField(service, "objectStore", models);
        ReflectionTestUtils.setField(service, "trainingDatasetArtifactService", datasets);
        training.setId(71L);
        training.setDatasetId(19L);
        training.setTenantId("tenant-a");
        training.setConfigParams("{\"datasetArtifactRef\":\"" + REFERENCE + "\",\"runtimeDatasetPath\":\"/old/run/data.yaml\"}");
        when(remote.resolvePath(training, "pt")).thenReturn(PT_PATH);
        TenantContextHolder.setTenantId("tenant-a");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void readsFrozenArtifactClassesWithoutRemoteFiles() throws Exception {
        when(datasets.require(19L, REFERENCE)).thenAnswer(call -> {
            assertEquals("tenant-a", TenantContextHolder.getTenantId());
            return artifact(YAML);
        });

        ModelClassFileService.ClassFile file = service.prepare(training);

        assertEquals("data.yaml", file.getFileName());
        assertEquals(YAML, file.getContent());
        assertEquals(YAML.getBytes(StandardCharsets.UTF_8).length, file.getFileSize());
        assertEquals(DigestUtils.sha256Hex(YAML.getBytes(StandardCharsets.UTF_8)), file.getSha256());
        verify(datasets).require(19L, REFERENCE);
        verify(datasets, never()).requireReference(anyString());
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void rejectedDatasetOwnershipDoesNotFallBackToSsh() throws Exception {
        ServiceException failure = new ServiceException("训练数据集制品不存在、未就绪或不属于当前数据集");
        when(datasets.require(19L, REFERENCE)).thenThrow(failure);

        assertSame(failure, assertThrows(ServiceException.class, () -> service.prepare(training)));

        verify(datasets).require(19L, REFERENCE);
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void absentReadyArtifactDoesNotFallBackToSsh() throws Exception {
        assertThrows(IOException.class, () -> service.prepare(training));
        verify(datasets).require(19L, REFERENCE);
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void invalidFrozenClassesDoNotFallBackToSsh() throws Exception {
        for (String content : new String[]{null, "names: []", "nc: 2\nnames: [person]", "names: {0: person, 2: car}"}) {
            when(datasets.require(19L, REFERENCE)).thenReturn(artifact(content));
            assertThrows(IOException.class, () -> service.prepare(training));
        }
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void explicitEmptyOrInvalidArtifactReferenceNeverBecomesLegacyFallback() throws Exception {
        for (String value : new String[]{"null", "\"\"", "\"/old/run/data.yaml\"", "19", "{}"}) {
            training.setConfigParams("{\"datasetArtifactRef\":" + value + "}");
            assertThrows(IOException.class, () -> service.prepare(training));
        }
        verifyNoInteractions(datasets);
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void malformedTrainingConfigCannotSelectAnUnverifiedClassSource() throws Exception {
        training.setConfigParams("{\"datasetArtifactRef\":");
        assertThrows(IOException.class, () -> service.prepare(training));
        verifyNoInteractions(datasets);
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void independentModelSnapshotStillWorksAfterDatasetRemoval() throws Exception {
        ModelClassFileService.ClassFile saved = new ModelClassFileService.ClassFile("classes.yaml", YAML,
            YAML.getBytes(StandardCharsets.UTF_8).length, DigestUtils.sha256Hex(YAML));
        when(snapshots.find(training, PT_PATH)).thenReturn(saved);

        assertSame(saved, service.prepare(training));

        verifyNoInteractions(datasets);
        verify(remote, never()).stream(anyString(), any());
    }

    @Test
    void readyModelClassObjectRemainsHigherPriorityThanDatasetArtifact() throws Exception {
        String classPath = ModelClassFileService.storagePath(PT_PATH);
        when(models.find(classPath)).thenReturn(new ModelArtifactObjectStore.StoredArtifact(
            "minio", "models/classes.yaml", "classes.yaml", YAML.getBytes(StandardCharsets.UTF_8).length,
            DigestUtils.sha256Hex(YAML)));
        doAnswer(call -> {
            ((OutputStream) call.getArgument(1)).write(YAML.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(remote).stream(eq(classPath), any());

        assertEquals(YAML, service.prepare(training).getContent());

        verifyNoInteractions(datasets, snapshots);
        verify(remote).stream(eq(classPath), any());
        verify(remote, never()).stream(eq("/runs/task/args.yaml"), any());
    }

    @Test
    void legacyConfigurationWithoutArtifactReferenceStillUsesItsRunMetadata() throws Exception {
        training.setConfigParams("{\"trainType\":\"detect\"}");
        doAnswer(call -> {
            String path = call.getArgument(0);
            String content = "/runs/task/args.yaml".equals(path)
                ? "data: /datasets/frozen/dataset.yaml\n" : YAML;
            ((OutputStream) call.getArgument(1)).write(content.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(remote).stream(anyString(), any());

        assertEquals(YAML, service.prepare(training).getContent());

        verify(remote).stream(eq("/runs/task/args.yaml"), any());
        verify(remote).stream(eq("/datasets/frozen/dataset.yaml"), any());
        verifyNoInteractions(datasets);
    }

    private TrainingDatasetArtifact artifact(String yaml) {
        TrainingDatasetArtifact artifact = new TrainingDatasetArtifact();
        artifact.setDatasetYaml(yaml);
        return artifact;
    }
}
