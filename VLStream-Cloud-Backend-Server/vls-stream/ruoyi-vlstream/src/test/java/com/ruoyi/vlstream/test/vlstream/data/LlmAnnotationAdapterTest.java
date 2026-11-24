package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class LlmAnnotationAdapterTest {
    private final LlmAnnotationAdapter adapter = new LlmAnnotationAdapter(new ObjectMapper());

    @Test
    void imageGeometryRejectsExifRotatedJpegBeforeItCanBeSubmitted() throws Exception {
        java.io.ByteArrayOutputStream file = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(30, 20, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpeg", file);
        byte[] plain = file.toByteArray();
        assertEquals(30, LlmAnnotationImage.inspect(plain).getWidth());
        assertEquals(20, LlmAnnotationImage.inspect(plain).getHeight());

        byte[] exif = {(byte) 0xff, (byte) 0xe1, 0, 34, 'E', 'x', 'i', 'f', 0, 0,
            'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 1, 18, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0, 0, 0, 0, 0};
        java.io.ByteArrayOutputStream rotated = new java.io.ByteArrayOutputStream();
        rotated.write(plain, 0, 2);
        rotated.write(exif);
        rotated.write(plain, 2, plain.length - 2);
        ServiceException rejected = assertThrows(ServiceException.class, () -> LlmAnnotationImage.inspect(rotated.toByteArray()));
        assertTrue(rejected.getMessage().contains("EXIF 旋转"));
    }

    @Test
    void classificationMapsOneFrozenLabelAndKeepsOptionalConfidence() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.CLASSIFICATION);
        String prompt = adapter.prompt(snapshot, "用中文描述", 20, 12);
        assertTrue(prompt.contains("\"key\":\"L1\",\"name\":\"红\""));
        assertTrue(prompt.contains("\"taskInstructions\":\"用中文描述\""));
        assertTrue(prompt.contains("原图宽 20、高 12"));

        LlmAnnotationAdapter.Result result = adapter.parse(snapshot, "```json\n{\"label\":\"L2\",\"confidence\":0.8}\n```", 20, 12);
        assertEquals(1, result.getBoxes().size());
        SmartAnnotationRequests.Box region = result.getBoxes().get(0);
        assertEquals(Long.valueOf(9223372036854000002L), region.getLabelId());
        assertEquals("蓝", region.getClassName());
        assertEquals(Double.valueOf(0.8), region.getConfidence());
        assertNull(region.getWidth());
        assertNull(adapter.parse(snapshot, "{\"label\":\"L1\"}", 20, 12).getBoxes().get(0).getConfidence());
        assertNull(adapter.parse(snapshot, "{\"label\":\"L1\",\"confidence\":null}", 20, 12).getBoxes().get(0).getConfidence());
    }

    @Test
    void invalidModelRepliesFailInsteadOfInventingUsableResults() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.CLASSIFICATION);
        for (String reply : Arrays.asList(
            "{\"label\":null}", "{\"label\":\"L99\"}", "{\"label\":1}", "{\"label\":[\"L1\",\"L2\"]}",
            "{\"label\":\"红\"}", "{\"label\":\"L1\",\"label\":\"L2\"}", "Here: {\"label\":\"L1\"}",
            "{\"label\":\"L1\"} {}", "{\"label\":\"L1\",\"confidence\":\"0.9\"}",
            "{\"label\":\"L1\",\"confidence\":1.5}", "{\"label\":\"L1\",\"confidence\":-0.1}",
            "{\"label\":\"L1\",\"confidence\":1e999}", "{\"label\":\"L1\",\"unrecognized\":3}",
            "{\"label\":[[[[[[[[[[[[[[\"L1\"]]]]]]]]]]]]]]}")) {
            assertThrows(ServiceException.class, () -> adapter.parse(snapshot, reply, 20, 20), reply);
        }
    }

    @Test
    void detectionUsesHalfOpenPixelCoordinatesAndRejectsInvalidBoxes() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.DETECTION);
        List<SmartAnnotationRequests.Box> regions = adapter.parse(
            snapshot, "{\"boxes\":[{\"label\":\"L1\",\"box\":[3,4,20,10],\"confidence\":0.71}]}", 20, 10).getBoxes();
        SmartAnnotationRequests.Box region = regions.get(0);
        assertEquals(Double.valueOf(3), region.getX());
        assertEquals(Double.valueOf(4), region.getY());
        assertEquals(Double.valueOf(17), region.getWidth());
        assertEquals(Double.valueOf(6), region.getHeight());
        assertEquals(Double.valueOf(0.71), region.getConfidence());
        AnnotationPayloads.validate(snapshot.getAnnotationType(), regions, 20, 10);
        for (String invalid : Arrays.asList("[3,4,21,10]", "[3,4,3,10]", "[3.5,4,20,10]", "[-1,4,20,10]", "[3,4,20]")) {
            String reply = "{\"boxes\":[{\"label\":\"L1\",\"box\":" + invalid + "}]}";
            assertThrows(ServiceException.class, () -> adapter.parse(snapshot, reply, 20, 10), invalid);
        }
        assertThrows(ServiceException.class, () -> adapter.parse(snapshot, "{\"boxes\":[]}", 20, 10));
    }

    @Test
    void instanceMasksPreserveHolesDisconnectedPartsAndOverlappingInstances() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.INSTANCE_SEGMENTATION);
        String outer = part(0, 0, 8, 8, "[" + rect(2, 2, 4, 4) + "]");
        String piece = part(9, 2, 10, 3, "[]");
        String reply = "{\"regions\":[" + region("L1", "0.82", outer + "," + piece) + ","
            + region("L1", "null", part(5, 5, 9, 9, "[]")) + "]}";
        List<SmartAnnotationRequests.Box> masks = adapter.parse(snapshot, reply, 12, 12).getBoxes();
        assertEquals(2, masks.size());
        AnnotationPayloads.validate(snapshot.getAnnotationType(), masks, 12, 12);
        assertEquals(61, pixels(masks.get(0)).cardinality());
        assertFalse(pixels(masks.get(0)).get(2 * 10 + 2));
        assertTrue(pixels(masks.get(0)).get(2 * 10 + 9));
        assertEquals(16, pixels(masks.get(1)).cardinality());
    }

    @Test
    void semanticSameClassRegionsMergeAndCrossClassPixelsStayUnassigned() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.SEMANTIC_SEGMENTATION);
        String reply = "{\"regions\":[" + region("L1", "0.88", part(0, 0, 4, 2, "[]")) + ","
            + region("L1", "null", part(1, 0, 3, 2, "[]")) + ","
            + region("L2", "0.98", part(2, 1, 6, 3, "[]")) + "]}";
        LlmAnnotationAdapter.Result result = adapter.parse(snapshot, reply, 6, 4);
        assertEquals(2, result.getBoxes().size());
        assertEquals(2, result.getConflictingPixels());
        assertEquals(12, result.getUnassignedPixels());
        assertEquals(6, pixels(result.getBoxes().get(0)).cardinality());
        assertEquals(6, pixels(result.getBoxes().get(1)).cardinality());
        assertNull(result.getBoxes().get(0).getConfidence());
        assertThrows(ServiceException.class, () -> AnnotationPayloads.validate(snapshot.getAnnotationType(), result.getBoxes(), 6, 4));
    }

    @Test
    void completeSemanticMaskMatchesFormalPayloadAndKeepsClassMinimumConfidence() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.SEMANTIC_SEGMENTATION);
        String reply = "{\"regions\":[" + region("L1", "0.8", part(0, 0, 2, 2, "[]")) + ","
            + region("L1", "0.4", part(2, 0, 4, 2, "[]")) + ","
            + region("L2", "0.9", part(0, 2, 4, 4, "[]")) + "]}";
        LlmAnnotationAdapter.Result result = adapter.parse(snapshot, reply, 4, 4);
        assertEquals(2, result.getBoxes().size());
        assertEquals(0, result.getConflictingPixels());
        assertEquals(0, result.getUnassignedPixels());
        assertEquals(Double.valueOf(0.4), result.getBoxes().get(0).getConfidence());
        AnnotationPayloads.validate(snapshot.getAnnotationType(), result.getBoxes(), 4, 4);
    }

    @Test
    void masksRejectOutOfBoundsOutsideHoleAndSelfIntersection() {
        DatasetSnapshot snapshot = snapshot(AnnotationTaskType.INSTANCE_SEGMENTATION);
        for (String part : Arrays.asList(
            part(0, 0, 11, 5, "[]"),
            part(0, 0, 5, 5, "[" + rect(4, 4, 6, 6) + "]"),
            "{\"outer\":[[1,1],[7,7],[2,6],[8,2]],\"holes\":[]}")) {
            String reply = "{\"regions\":[" + region("L1", "0.9", part) + "]}";
            assertThrows(ServiceException.class, () -> adapter.parse(snapshot, reply, 10, 10));
        }
    }

    private static DatasetSnapshot snapshot(AnnotationTaskType type) {
        DatasetSnapshot snapshot = new DatasetSnapshot();
        snapshot.setAnnotationType(type.getCode());
        snapshot.setLabels(Arrays.asList(label(9223372036854000001L, "红"), label(9223372036854000002L, "蓝")));
        return snapshot;
    }

    private static AnnotationLabel label(long id, String name) {
        AnnotationLabel label = new AnnotationLabel();
        label.setId(id);
        label.setName(name);
        return label;
    }

    private static String region(String label, String confidence, String parts) {
        return "{\"label\":\"" + label + "\",\"confidence\":" + confidence + ",\"parts\":[" + parts + "]}";
    }

    private static String part(int left, int top, int right, int bottom, String holes) {
        return "{\"outer\":" + rect(left, top, right, bottom) + ",\"holes\":" + holes + "}";
    }

    private static String rect(int left, int top, int right, int bottom) {
        return "[[" + left + "," + top + "],[" + right + "," + top + "],[" + right + "," + bottom + "],[" + left + "," + bottom + "]]";
    }

    private static BitSet pixels(SmartAnnotationRequests.Box box) {
        return AnnotationMask.decode(box.getMaskData(), box.getWidth().intValue(), box.getHeight().intValue());
    }
}
