package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.data.DatasetSnapshot;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class CloudComputeContractTest {
    @Test void missingCloudArtifactCannotFallBackToTheDefaultSshHost() {
        com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService service = new com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService();
        com.ruoyi.vlstream.test.vlstream.service.SSHService ssh = org.mockito.Mockito.mock(com.ruoyi.vlstream.test.vlstream.service.SSHService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "sshService", ssh);
        assertThrows(java.io.FileNotFoundException.class, () -> service.inspect("cloud-training/a/weights/best.pt"));
        assertThrows(java.io.FileNotFoundException.class, () -> service.stream("cloud-training/a/weights/best.pt", new java.io.ByteArrayOutputStream()));
        org.mockito.Mockito.verifyNoInteractions(ssh);
    }
    @Test void credentialsNeverAppearInNodeJsonOrDiagnosticString() throws Exception {
        ComputeNode node = new ComputeNode(); node.setId(123L); node.setPasswordCipher("sensitive-cipher"); node.setHostKey("sensitive-key");
        String response = new ObjectMapper().writeValueAsString(node);
        assertFalse(response.contains("sensitive")); assertFalse(node.toString().contains("sensitive"));
        assertTrue(response.contains("\"id\":\"123\"")); assertTrue(response.contains("\"passwordConfigured\":true"));
    }
    @Test void rejectsCommandSubstitutionAndTraversalInExecutableAndWorkDirectory() {
        for (String path : Arrays.asList("/tmp/../root", "/tmp/$(id)", "/tmp/x;whoami", "python", "/tmp/a b"))
            assertThrows(RuntimeException.class, () -> ComputeNodeService.validatePath(path));
        assertDoesNotThrow(() -> ComputeNodeService.validatePath("/root/miniconda3/bin/python"));
        assertEquals("'a'\"'\"'b'", ComputeSsh.quote("a'b"));
    }
    @Test void cloudStatusIsExplicitAndUntrustedOutputNeedsTheProtocolMarker() throws Exception {
        AlgorithmTraining training = new AlgorithmTraining(); training.setConfigParams("{}");
        assertFalse(CloudTrainingService.isCloud(training));
        training.setConfigParams("{\"cloudJobId\":\"test\"}"); assertTrue(CloudTrainingService.isCloud(training));
        assertThrows(java.io.IOException.class, () -> CloudTrainingWorker.remoteStatus("Training complete"));
        assertEquals("RUNNING", CloudTrainingWorker.remoteStatus("warning\nVLS_CLOUD={\"state\":\"RUNNING\"}").path("state").asText());
    }
    @Test void frozenDatasetRejectsLeakedImagesAndUnsplitSamples() {
        DatasetSnapshot snapshot = snapshot();
        assertEquals(2, CloudTrainingDatasetWriter.members(snapshot).size());
        snapshot.getSamples().get(1).setContentSha256("train-hash");
        DatasetSnapshot leaked = snapshot;
        assertThrows(RuntimeException.class, () -> CloudTrainingDatasetWriter.members(leaked));
        snapshot = snapshot(); snapshot.getSamples().get(1).setDatasetSplit(null);
        DatasetSnapshot unsplit = snapshot;
        assertThrows(RuntimeException.class, () -> CloudTrainingDatasetWriter.members(unsplit));
    }
    @Test void credentialsUseRandomAuthenticatedEncryptionAndRejectMissingOrWrongKeys() {
        ComputeCredentialCipher cipher = new ComputeCredentialCipher("0123456789abcdef0123456789abcdef");
        String first = cipher.encrypt("test-password");
        assertNotEquals(first, cipher.encrypt("test-password"));
        assertEquals("test-password", cipher.decrypt(first));
        assertThrows(RuntimeException.class, () -> new ComputeCredentialCipher("").encrypt("test-password"));
        assertThrows(RuntimeException.class, () -> new ComputeCredentialCipher("fedcba9876543210fedcba9876543210").decrypt(first));
    }
    static DatasetSnapshot snapshot() {
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setAnnotationType("object_detection");
        AnnotationLabel label = new AnnotationLabel(); label.setId(1L); label.setName("helmet"); snapshot.setLabels(Collections.singletonList(label));
        List<AnnotationImage> samples = new ArrayList<>(); List<AnnotationInstance> labels = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            AnnotationImage image = new AnnotationImage(); image.setId((long) i + 1); image.setDatasetSplit(i == 0 ? "train" : "val"); image.setContentSha256(i == 0 ? "train-hash" : "val-hash"); samples.add(image);
            AnnotationInstance annotation = new AnnotationInstance(); annotation.setImageId(image.getId()); annotation.setLabelId(1L); labels.add(annotation);
        }
        snapshot.setSamples(samples); snapshot.setInstances(labels); return snapshot;
    }
}
