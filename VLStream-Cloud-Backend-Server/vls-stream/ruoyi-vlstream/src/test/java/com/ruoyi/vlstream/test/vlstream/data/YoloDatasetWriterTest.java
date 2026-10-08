package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class YoloDatasetWriterTest {
    private final YoloDatasetWriter writer = new YoloDatasetWriter(new ObjectMapper());

    @Test void encodesPixelsIncludingSubpixelOriginAndDeduplicatesLabels() {
        AnnotationInstance annotation = shape("rect", "{\"x\":0,\"y\":0,\"width\":20,\"height\":10}");
        assertEquals("0 0.10000000 0.05000000 0.20000000 0.10000000\n", writer.labels(Arrays.asList(annotation, annotation), Collections.singletonMap(1L, 0), 100, 100));
    }

    @Test void convertsCircleToDetectionBox() {
        assertEquals("0 0.50000000 0.50000000 0.20000000 0.20000000\n", writer.labels(Collections.singletonList(shape("circle", "{\"cx\":50,\"cy\":50,\"r\":10}")), Collections.singletonMap(1L, 0), 100, 100));
    }

    @Test void clipsPartialOverlapWithoutChangingSourceAndRejectsMissingClass() {
        AnnotationInstance annotation = shape("rect", "{\"x\":90,\"y\":0,\"width\":20,\"height\":10}");
        String original = annotation.getAnnotationData();
        assertEquals("0 0.95000000 0.05000000 0.10000000 0.10000000\n", writer.labels(Collections.singletonList(annotation), Collections.singletonMap(1L, 0), 100, 100));
        assertEquals(original, annotation.getAnnotationData());
        assertThrows(ServiceException.class, () -> writer.validateForSave(Collections.singletonList(annotation), 100, 100));
        assertThrows(ServiceException.class, () -> writer.labels(Collections.singletonList(annotation), Collections.emptyMap(), 100, 100));
    }
    @Test void rejectsCompletelyOutsideZeroSizeAndNonFiniteBoxes() {
        assertThrows(ServiceException.class, () -> YoloDatasetWriter.intersect(100, 0, 20, 10, 100, 100));
        assertThrows(ServiceException.class, () -> YoloDatasetWriter.intersect(0, 0, 0, 10, 100, 100));
        assertThrows(ServiceException.class, () -> YoloDatasetWriter.intersect(Double.NaN, 0, 10, 10, 100, 100));
        assertThrows(ServiceException.class, () -> YoloDatasetWriter.intersect(Double.MAX_VALUE, 0, Double.MAX_VALUE, 10, 100, 100));
    }
    @Test void clipsNormalizedRectanglesAndCirclesAtMultipleEdges() {
        assertEquals("0 0.45000000 0.50000000 0.90000000 1.00000000\n", writer.labels(Collections.singletonList(shape("rect", "{\"x\":-0.1,\"y\":-0.1,\"width\":1,\"height\":1.2,\"normalized\":true}")), Collections.singletonMap(1L, 0), 100, 100));
        assertEquals("0 0.07500000 0.07500000 0.15000000 0.15000000\n", writer.labels(Collections.singletonList(shape("circle", "{\"cx\":5,\"cy\":5,\"r\":10}")), Collections.singletonMap(1L, 0), 100, 100));
    }

    private AnnotationInstance shape(String type, String data) { AnnotationInstance item = new AnnotationInstance(); item.setLabelId(1L); item.setAnnotationType(AlgorithmAnnotationTypeEnum.valueOf(type)); item.setAnnotationData(data); return item; }
}
