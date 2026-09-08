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

    @Test void rejectsMissingClassAndOutOfBoundsAnnotations() {
        AnnotationInstance annotation = shape("rect", "{\"x\":90,\"y\":0,\"width\":20,\"height\":10}");
        assertThrows(ServiceException.class, () -> writer.labels(Collections.singletonList(annotation), Collections.singletonMap(1L, 0), 100, 100));
        assertThrows(ServiceException.class, () -> writer.labels(Collections.singletonList(annotation), Collections.emptyMap(), 100, 100));
    }

    private AnnotationInstance shape(String type, String data) { AnnotationInstance item = new AnnotationInstance(); item.setLabelId(1L); item.setAnnotationType(AlgorithmAnnotationTypeEnum.valueOf(type)); item.setAnnotationData(data); return item; }
}
