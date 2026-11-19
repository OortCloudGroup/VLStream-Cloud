package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class SmartAnnotationComputeDatasetWriterTest {
    ObjectMapper json=new ObjectMapper();DataMediaStorage storage;ComputeSsh.Connection connection;
    DatasetSnapshot snapshot;AnnotationImage image;CloudTrainingJob job;SmartAnnotationComputeDatasetWriter writer;
    @BeforeEach void setup() throws Exception {
        storage=mock(DataMediaStorage.class);connection=mock(ComputeSsh.Connection.class);
        ByteArrayOutputStream png=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(40,30,BufferedImage.TYPE_INT_RGB),"png",png);
        when(storage.read("source-key")).thenAnswer(c->new ByteArrayInputStream(png.toByteArray()));
        writer=new SmartAnnotationComputeDatasetWriter(storage,new SampleMediaInspector(),json);
        image=new AnnotationImage();image.setId(9L);image.setLocalPath("source-key");image.setOriginalName("sample.png");image.setMediaWidth(40);image.setMediaHeight(30);
        snapshot=new DatasetSnapshot();snapshot.setSamples(Collections.singletonList(image));snapshot.setInstances(Collections.emptyList());
        AnnotationLabel label=new AnnotationLabel();label.setId(1L);label.setName("car");snapshot.setLabels(Collections.singletonList(label));
        job=new CloudTrainingJob();job.setDatasetId(20L);job.setTenantId("tenant");job.setRunDir("/fixture/job");job.setOptionsJson("{\"mode\":\"model\"}");
    }
    @ParameterizedTest @ValueSource(strings={"image_classification","object_detection","instance_segmentation","semantic_segmentation"})
    void inferenceUploadsFrozenImagesWithMatchingManifestAndSharedSupervisor(String kind) throws Exception {
        snapshot.setAnnotationType(kind);
        List<Map<String,Object>> images=writer.upload(job,snapshot,connection);
        assertEquals("9",images.get(0).get("id"));assertEquals("dataset/images/predict/9.png",images.get(0).get("path"));
        verify(connection).putText(eq("/fixture/job/dataset-preparing/vls-dataset.json"),argThat(text->{try{return kind.equals(json.readTree(text).path("annotationType").asText());}catch(Exception e){return false;}}));
        verify(connection).put(eq("/fixture/job/cloud_training_runner.py"),any(InputStream.class));
        verify(connection).put(eq("/fixture/job/smart_annotation_runner.py"),any(InputStream.class));
        verify(connection).put(eq("/fixture/job/gpu_shared_queue.py"),any(InputStream.class));
    }
    @Test void changedSourceContentIsRejectedBeforeAnyImageUpload() throws Exception {
        snapshot.setAnnotationType("object_detection");image.setContentSha256("invalid");
        assertThrows(IOException.class,()->writer.upload(job,snapshot,connection));
        verify(connection,never()).put(anyString(),any(InputStream.class));
    }
    @Test void metadataFilesAreNotReadOrExpectedInPredictionResults() throws Exception {
        snapshot.setAnnotationType("object_detection");
        AnnotationImage sidecar=new AnnotationImage();sidecar.setId(10L);sidecar.setOriginalName("._sample.png");sidecar.setLocalPath("metadata-key");
        snapshot.setSamples(Arrays.asList(image,sidecar));
        assertEquals(1,writer.upload(job,snapshot,connection).size());
        verify(storage,never()).read("metadata-key");
    }

    @Test void newlyConfirmedCopyWithoutHashStaysWithTheExistingValidationImage() throws Exception {
        job.setOptionsJson("{\"mode\":\"active\"}");
        snapshot.setAnnotationType("object_detection");
        AnnotationImage validation = seed(10L, "source-key", "val");
        AnnotationImage confirmedCopy = seed(11L, "source-key", "unassigned");
        AnnotationImage training = seed(12L, "different-source", "train");
        BufferedImage different = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        different.setRGB(0, 0, 0xffffff);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(different, "png", bytes);
        when(storage.read("different-source")).thenAnswer(call -> new ByteArrayInputStream(bytes.toByteArray()));
        snapshot.setSamples(Arrays.asList(image, validation, confirmedCopy, training));
        List<AnnotationInstance> labels = new ArrayList<>();
        for (AnnotationImage sample : Arrays.asList(validation, confirmedCopy, training)) {
            AnnotationInstance instance = new AnnotationInstance();
            instance.setImageId(sample.getId());
            instance.setLabelId(1L);
            instance.setAnnotationType(AlgorithmAnnotationTypeEnum.rect);
            instance.setAnnotationData("{\"x\":2,\"y\":3,\"width\":8,\"height\":9}");
            labels.add(instance);
        }
        snapshot.setInstances(labels);

        assertEquals(1, writer.upload(job, snapshot, connection).size());
        verify(connection).put(eq("/fixture/job/dataset-preparing/images/val/10.png"), any(InputStream.class));
        verify(connection).put(eq("/fixture/job/dataset-preparing/images/val/11.png"), any(InputStream.class));
        verify(connection).put(eq("/fixture/job/dataset-preparing/images/train/12.png"), any(InputStream.class));
        verify(connection).putText(eq("/fixture/job/dataset-preparing/dataset.yaml"), contains("/fixture/job/dataset\""));
        verify(connection).execute(contains("mv -T -- '/fixture/job/dataset-preparing' '/fixture/job/dataset'"), eq(30));
        assertNull(confirmedCopy.getContentSha256());
        assertEquals("unassigned", confirmedCopy.getDatasetSplit());
    }

    private AnnotationImage seed(Long id, String key, String split) {
        AnnotationImage sample = new AnnotationImage();
        sample.setId(id);
        sample.setLocalPath(key);
        sample.setOriginalName("sample.png");
        sample.setDatasetSplit(split);
        return sample;
    }
}
