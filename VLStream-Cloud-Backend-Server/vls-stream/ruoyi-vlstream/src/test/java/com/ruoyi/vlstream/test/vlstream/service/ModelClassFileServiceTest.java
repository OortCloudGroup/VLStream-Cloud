package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class ModelClassFileServiceTest {
    @Test
    void preservedSnapshotWorksAfterTrainingInputsAreGone() throws Exception {
        RemoteModelArtifactService remote = mock(RemoteModelArtifactService.class);
        ModelClassSnapshotStore snapshots = mock(ModelClassSnapshotStore.class);
        ModelClassFileService service = new ModelClassFileService();
        ReflectionTestUtils.setField(service, "artifactService", remote);
        ReflectionTestUtils.setField(service, "snapshotStore", snapshots);
        AlgorithmTraining training = new AlgorithmTraining();
        when(remote.resolvePath(training,"pt")).thenReturn("/runs/train/weights/best.pt");
        String content = "names: [helmet]\n";
        when(snapshots.find(training,"/runs/train/weights/best.pt")).thenReturn(new ModelClassFileService.ClassFile("dataset.yaml",content,content.length(),org.apache.commons.codec.digest.DigestUtils.sha256Hex(content)));
        assertEquals(content,service.prepare(training).getContent());
        verify(remote,never()).stream(anyString(),any());
    }

    @Test
    void readsOriginalRunDatasetAndPreservesUtf8BytesAndOrder() throws Exception {
        RemoteModelArtifactService remote = mock(RemoteModelArtifactService.class);
        ModelClassFileService service = new ModelClassFileService();
        ReflectionTestUtils.setField(service, "artifactService", remote);
        AlgorithmTraining training = new AlgorithmTraining();
        when(remote.resolvePath(training, "pt")).thenReturn("/runs/train24/weights/安全绳.pt");
        String yaml = "nc: 2\r\nnames: {1: 安全绳, 0: '1'}\r\n";
        doAnswer(call -> {
            String path = call.getArgument(0);
            String text = path.equals("/runs/train24/args.yaml")
                ? "data: /datasets/version_1/emgitemsv6.yaml\n" : yaml;
            ((OutputStream) call.getArgument(1)).write(text.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(remote).stream(anyString(), any());
        ModelClassFileService.ClassFile file = service.prepare(training);
        assertEquals("emgitemsv6.yaml", file.getFileName());
        assertEquals(yaml, file.getContent());
        assertEquals(yaml.getBytes(StandardCharsets.UTF_8).length, file.getFileSize());
        assertEquals(org.apache.commons.codec.digest.DigestUtils.sha256Hex(yaml.getBytes(StandardCharsets.UTF_8)), file.getSha256());
        verify(remote).stream(eq("/datasets/version_1/emgitemsv6.yaml"), any());
    }

    @Test
    void rejectsMissingSparseDuplicateOrMismatchedClassesAndUnsafeYaml() {
        for (String yaml : new String[]{"names: []", "names: {0: person, 2: car}",
            "names: {0: person, '0': car}", "names: {0: person, 0: car}",
            "nc: 2\nnames: [person]", "names: [person, null]", "names: [1]",
            "!!javax.script.ScriptEngineManager {}"}) {
            assertThrows(IOException.class, () -> ModelClassFileService.validate(yaml), yaml);
        }
    }

    @Test
    void acceptsListAndIndexedNamesWithoutInventingClasses() throws Exception {
        ModelClassFileService.validate("nc: 2\nnames: ['1', 安全绳]");
        ModelClassFileService.validate("names: {'1': car, '0': person}");
    }

    @Test
    void missingRunMetadataFailsInsteadOfUsingCurrentDataset() throws Exception {
        RemoteModelArtifactService remote = mock(RemoteModelArtifactService.class);
        ModelClassFileService service = new ModelClassFileService();
        ReflectionTestUtils.setField(service, "artifactService", remote);
        AlgorithmTraining training = new AlgorithmTraining();
        when(remote.resolvePath(training, "pt")).thenReturn("/runs/train/weights/a.pt");
        doThrow(new IOException("missing args.yaml")).when(remote).stream(anyString(), any());
        assertThrows(IOException.class, () -> service.prepare(training));
        verify(remote).stream(eq("/runs/train/args.yaml"), any());
        verify(remote).resolvePath(training, "pt");
        verifyNoMoreInteractions(remote);
    }

    @Test
    void oversizedMetadataIsRejectedBeforeParsing() throws Exception {
        RemoteModelArtifactService remote = mock(RemoteModelArtifactService.class);
        ModelClassFileService service = new ModelClassFileService();
        ReflectionTestUtils.setField(service, "artifactService", remote);
        AlgorithmTraining training = new AlgorithmTraining();
        when(remote.resolvePath(training, "pt")).thenReturn("/runs/train/weights/a.pt");
        doAnswer(call -> {
            ((OutputStream) call.getArgument(1)).write(new byte[1024 * 1024 + 1]);
            return null;
        }).when(remote).stream(anyString(), any());
        assertThrows(IOException.class, () -> service.prepare(training));
    }
}
