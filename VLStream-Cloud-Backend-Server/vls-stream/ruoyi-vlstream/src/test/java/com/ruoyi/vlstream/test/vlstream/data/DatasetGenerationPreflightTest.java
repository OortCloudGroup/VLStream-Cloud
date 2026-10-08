package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import org.junit.jupiter.api.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class DatasetGenerationPreflightTest {
    private final ObjectMapper json = new ObjectMapper();
    private DataMediaStorage media;
    private DatasetGenerationPreflight preflight;
    private DatasetSnapshot snapshot;
    @BeforeEach void setup() throws Exception {
        media = mock(DataMediaStorage.class);
        preflight = new DatasetGenerationPreflight(media, new SampleMediaInspector(), json);
        snapshot = new DatasetSnapshot(); snapshot.setAnnotationType("object_detection");
        AnnotationLabel label = new AnnotationLabel(); label.setId(1L); label.setName("smoking");
        snapshot.setLabels(Collections.singletonList(label)); snapshot.setSamples(new ArrayList<>()); snapshot.setInstances(new ArrayList<>());
        when(media.preview(anyString())).thenReturn("/signed-preview");
    }
    @Test void repairsLegacyDuplicatePartitionsAndReportsClippingWhilePreservingSource() throws Exception {
        add(2096777217777467393L, "train", 1, "{\"x\":90,\"y\":5,\"width\":20,\"height\":15}");
        add(2096777217777467394L, "val", 1, valid());
        add(2096777217777467395L, "train", 2, valid());
        String before = json.writeValueAsString(snapshot);
        DatasetGenerationReport report = preflight.check(1L, snapshot);
        assertEquals("CHECKED", report.getStatus()); assertTrue(report.isRepartitioned());
        assertEquals(1, report.getCorrections().size()); assertTrue(report.getErrors().isEmpty());
        assertEquals("2096777217777467393", report.getCorrections().get(0).get("imageId"));
        assertEquals(report.getPrepared().getSamples().get(0).getDatasetSplit(), report.getPrepared().getSamples().get(1).getDatasetSplit());
        assertEquals(before, json.writeValueAsString(snapshot));
        assertEquals(snapshot.getInstances().get(0).getAnnotationData(), report.getPrepared().getInstances().get(0).getAnnotationData());
    }
    @Test void reportsEveryInvalidImageAndDoesNotSilentlyDropAnnotations() throws Exception {
        add(1L, "train", 1, "{\"x\":110,\"y\":0,\"width\":10,\"height\":10}");
        add(2L, "val", 2, "{\"x\":0,\"y\":0,\"width\":0,\"height\":10}");
        DatasetGenerationReport report = preflight.check(1L, snapshot);
        assertEquals("BLOCKED", report.getStatus()); assertEquals(2, report.getErrors().size());
        assertEquals(2, report.getCheckedImages()); assertEquals(2, report.getPrepared().getInstances().size());
    }
    @Test void preservesAlreadyDisjointManualPartitions() throws Exception {
        add(1L, "train", 1, valid()); add(2L, "val", 2, valid());
        DatasetGenerationReport report = preflight.check(1L, snapshot);
        assertFalse(report.isRepartitioned()); assertTrue(report.getCorrections().isEmpty());
    }
    @Test void mapsUnreadableObjectsToTheAffectedImageWithoutLeakingStorageErrors() throws Exception {
        add(1L, "train", 1, valid()); add(2L, "val", 2, valid());
        when(media.read("1")).thenThrow(new RuntimeException("secret storage credentials"));
        DatasetGenerationReport report = preflight.check(1L, snapshot);
        assertEquals("BLOCKED", report.getStatus()); assertEquals("1", report.getErrors().get(0).get("imageId"));
        assertFalse(json.writeValueAsString(report).contains("secret"));
    }
    private String valid() { return "{\"x\":5,\"y\":5,\"width\":20,\"height\":15}"; }
    private void add(Long id, String split, int color, String box) throws Exception {
        BufferedImage image = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB); image.setRGB(50,50,color);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(image,"png",bytes);
        byte[] raw = bytes.toByteArray(); when(media.read(id.toString())).thenAnswer(call -> new ByteArrayInputStream(raw));
        AnnotationImage sample = new AnnotationImage(); sample.setId(id); sample.setOriginalName(id+".png"); sample.setLocalPath(id.toString());
        sample.setDatasetSplit(split); sample.setMediaType("image"); sample.setQualityStatus("pending"); sample.setFileSize((long)raw.length);
        snapshot.getSamples().add(sample);
        AnnotationInstance annotation = new AnnotationInstance(); annotation.setId(id); annotation.setImageId(id); annotation.setLabelId(1L);
        annotation.setAnnotationType(AlgorithmAnnotationTypeEnum.rect); annotation.setAnnotationData(box); snapshot.getInstances().add(annotation);
    }
}
