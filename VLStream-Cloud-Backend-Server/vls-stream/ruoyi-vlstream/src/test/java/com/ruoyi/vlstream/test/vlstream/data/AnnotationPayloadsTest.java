package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AnnotationPayloadsTest {
    static SmartAnnotationRequests.Box mask(long label, int... indices) {
        SmartAnnotationRequests.Box region = new SmartAnnotationRequests.Box(); region.setLabelId(label);
        region.setX(0d); region.setY(0d); region.setWidth(2d); region.setHeight(2d);
        BitSet pixels = new BitSet(4); for (int index : indices) pixels.set(index);
        region.setMaskData(AnnotationMask.encode(pixels, 2, 2)); return region;
    }

    @Test void singleLabelClassificationRejectsSpatialAndMultipleLabels() {
        SmartAnnotationRequests.Box classification = new SmartAnnotationRequests.Box(); classification.setLabelId(1L);
        assertDoesNotThrow(() -> AnnotationPayloads.validate("image_classification", Collections.singletonList(classification), 2, 2));
        assertThrows(ServiceException.class, () -> AnnotationPayloads.validate("image_classification", Arrays.asList(classification, classification), 2, 2));
        classification.setX(0d);
        assertThrows(ServiceException.class, () -> AnnotationPayloads.validate("image_classification", Collections.singletonList(classification), 2, 2));
    }

    @Test void instanceMasksCanOverlapButSemanticPixelsMustHaveExactlyOneClass() {
        List<SmartAnnotationRequests.Box> overlapping = Arrays.asList(mask(1, 0, 1), mask(2, 1, 2, 3));
        assertDoesNotThrow(() -> AnnotationPayloads.validate("instance_segmentation", overlapping, 2, 2));
        assertThrows(ServiceException.class, () -> AnnotationPayloads.validate("semantic_segmentation", overlapping, 2, 2));
        assertThrows(ServiceException.class, () -> AnnotationPayloads.validate("semantic_segmentation", Collections.singletonList(mask(1, 0, 1)), 2, 2));
        assertDoesNotThrow(() -> AnnotationPayloads.validate("semantic_segmentation", Arrays.asList(mask(1, 0, 1), mask(2, 2, 3)), 2, 2));
    }

    @Test void semanticTrainingPngContainsStableClassIndices() throws Exception {
        AnnotationLabel a = new AnnotationLabel(); a.setId(1L); a.setName("road"); AnnotationLabel b = new AnnotationLabel(); b.setId(2L); b.setName("vehicle");
        List<AnnotationInstance> instances = new ArrayList<>(); ObjectMapper json = new ObjectMapper();
        for (SmartAnnotationRequests.Box region : Arrays.asList(mask(1, 0, 1), mask(2, 2, 3))) {
            AnnotationInstance instance = new AnnotationInstance(); instance.setLabelId(region.getLabelId()); instance.setAnnotationType(AlgorithmAnnotationTypeEnum.mask);
            instance.setAnnotationData(json.writeValueAsString(AnnotationPayloads.content("semantic_segmentation", region))); instances.add(instance);
        }
        TrainingDatasetLayout layout = new TrainingDatasetLayout("semantic_segmentation", Arrays.asList(b, a));
        byte[] encoded = layout.annotations(50L, "train", instances, 2, 2).get("masks/50.png");
        java.awt.image.BufferedImage result = ImageIO.read(new ByteArrayInputStream(encoded));
        assertArrayEquals(new int[]{0, 0, 1, 1}, result.getRaster().getSamples(0, 0, 2, 2, 0, (int[]) null));
        assertTrue(layout.yaml("/test").contains("[\"road\",\"vehicle\"]"));
    }
}
