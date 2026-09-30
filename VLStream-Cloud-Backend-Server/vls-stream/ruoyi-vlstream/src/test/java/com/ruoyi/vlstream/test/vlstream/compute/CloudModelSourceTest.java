package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.OutputStream;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class CloudModelSourceTest {
    @Test void explicitPresetDoesNotReadLegacyAlgorithmPath() {
        CloudTrainingService service = new CloudTrainingService(null,null,null,null,null,null,null,null);
        ModelArtifactObjectStore objects = mock(ModelArtifactObjectStore.class);
        ReflectionTestUtils.setField(service,"modelObjects",objects);
        assertEquals("@preset/detect", service.resolveBaseModel("preset", "/data/work/old-model.pt", "detect"));
        verifyNoInteractions(objects);
    }
    @Test void cloudCustomModelMustBeReadyInTenantStorageAndCannotFallBackToDevelopmentHost() {
        CloudTrainingService service = new CloudTrainingService(null,null,null,null,null,null,null,null);
        ModelArtifactObjectStore objects = mock(ModelArtifactObjectStore.class);
        ReflectionTestUtils.setField(service,"modelObjects",objects);
        assertThrows(RuntimeException.class, () -> service.resolveBaseModel("algorithm", "/data/work/old-model.pt", "detect"));
        assertThrows(RuntimeException.class, () -> service.resolveBaseModel("algorithm", "", "detect"));
        assertThrows(RuntimeException.class, () -> service.resolveBaseModel(null, "@preset/classify", "detect"));
        when(objects.find("imports/custom.pt")).thenReturn(new ModelArtifactObjectStore.StoredArtifact("test","key","model.pt",4,"hash"));
        assertEquals("imports/custom.pt",service.resolveBaseModel("algorithm","imports/custom.pt","detect"));
        assertEquals("@preset/detect",service.resolveBaseModel(null,null,"detect"));
    }
    @Test void workerCopiesVerifiedMinioBytesAndRejectsCorruptionBeforeSftp() throws Exception {
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while(!Files.exists(root.resolve("BUSINESS_PROCESSES.md")))root=root.getParent();
        Path temp=root.resolve("codex/gpu-onboarding/tmp"); Files.createDirectories(temp);
        ModelArtifactObjectStore objects=mock(ModelArtifactObjectStore.class);
        com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService remote=mock(com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService.class);
        CloudTrainingWorker worker=new CloudTrainingWorker(null,null,null,null,null,null,objects,remote,null);
        ReflectionTestUtils.setField(worker,"tempDir",temp.toString());
        CloudTrainingJob job=new CloudTrainingJob();job.setBaseModel("imports/custom.pt");job.setRunDir("/new/run");
        ComputeSsh.Connection connection=mock(ComputeSsh.Connection.class);
        ModelArtifactObjectStore.StoredArtifact artifact=new ModelArtifactObjectStore.StoredArtifact("test","key","model.pt",4,ModelArtifactObjectStore.hash("data"));
        when(objects.find(job.getBaseModel())).thenReturn(artifact);
        doAnswer(call->{((OutputStream)call.getArgument(1)).write("data".getBytes(java.nio.charset.StandardCharsets.UTF_8));return null;}).when(objects).stream(eq(artifact),any());
        assertEquals("/new/run/base.pt",ReflectionTestUtils.<String>invokeMethod(worker,"prepareBaseModel",job,connection));
        verify(connection).put(eq("/new/run/base.pt"),any()); verifyNoInteractions(remote);
        reset(connection);
        doAnswer(call->{((OutputStream)call.getArgument(1)).write("evil".getBytes(java.nio.charset.StandardCharsets.UTF_8));return null;}).when(objects).stream(eq(artifact),any());
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"prepareBaseModel",job,connection));
        verifyNoInteractions(connection,remote); worker.shutdown();
    }
}
