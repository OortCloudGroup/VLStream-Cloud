package com.ruoyi.vlstream.test.vlstream.data;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class DetectionPredictionFilterTest {
    private SmartAnnotationRequests.Box box(long label, double x, Double confidence) {
        SmartAnnotationRequests.Box box = new SmartAnnotationRequests.Box();
        box.setLabelId(label); box.setClassName("class-" + label); box.setConfidence(confidence);
        box.setX(x); box.setY(0d); box.setWidth(100d); box.setHeight(100d);
        return box;
    }

    @Test void keepsHighestConfidenceOfSameClassWithoutChangingInput() {
        SmartAnnotationRequests.Box lower = box(1, 0, .5), higher = box(1, 5, .9);
        List<SmartAnnotationRequests.Box> input = Arrays.asList(lower, higher);
        assertEquals(Collections.singletonList(higher), DetectionPredictionFilter.suppressDuplicates(input));
        assertEquals(Arrays.asList(lower, higher), input);
        assertEquals(0d, lower.getX());
    }

    @Test void preservesDifferentObjectsAndDoesNotChainSuppressionThroughRemovedBox() {
        SmartAnnotationRequests.Box first = box(1, 0, .9), middle = box(1, 35, .8), last = box(1, 70, .7);
        assertEquals(Arrays.asList(first, last), DetectionPredictionFilter.suppressDuplicates(Arrays.asList(first, middle, last)));
    }

    @Test void exactScoreTiesKeepOriginalFirstPrediction() {
        SmartAnnotationRequests.Box first = box(1, 0, .8), second = box(1, 0, .8);
        assertEquals(Collections.singletonList(first), DetectionPredictionFilter.suppressDuplicates(Arrays.asList(first, second)));
    }

    @Test void preservesDifferentClassesAndFlagsStrongOverlap() {
        List<SmartAnnotationRequests.Box> boxes = Arrays.asList(box(1, 0, .9), box(2, 0, .8));
        assertEquals(2, DetectionPredictionFilter.suppressDuplicates(boxes).size());
        assertTrue(DetectionPredictionFilter.hasClassConflict(boxes));
    }

    @Test void nearbyDifferentClassesAreNotReportedAsConflict() {
        assertFalse(DetectionPredictionFilter.hasClassConflict(Arrays.asList(box(1, 0, .9), box(2, 30, .8))));
    }

    @Test void sameClassOverlapIsNotClassConflict() {
        assertFalse(DetectionPredictionFilter.hasClassConflict(Arrays.asList(box(1, 0, .9), box(1, 0, .8))));
    }

    @Test void unmappedClassesAreComparedByExactModelName() {
        SmartAnnotationRequests.Box first = box(1, 0, .9), second = box(1, 0, .8);
        first.setLabelId(null); second.setLabelId(null);
        assertEquals(1, DetectionPredictionFilter.suppressDuplicates(Arrays.asList(first, second)).size());
        second.setClassName("other");
        assertEquals(2, DetectionPredictionFilter.suppressDuplicates(Arrays.asList(first, second)).size());
        assertTrue(DetectionPredictionFilter.hasClassConflict(Arrays.asList(first, second)));
    }

    @Test void emptyClassIdentityIsNotGuessed() {
        SmartAnnotationRequests.Box first = box(1, 0, .9), second = box(1, 0, .8);
        first.setLabelId(null); first.setClassName(null); second.setLabelId(null); second.setClassName(null);
        assertEquals(2, DetectionPredictionFilter.suppressDuplicates(Arrays.asList(first, second)).size());
        assertFalse(DetectionPredictionFilter.hasClassConflict(Arrays.asList(first, second)));
    }

    @Test void invalidGeometryOrMissingConfidenceRemainsForValidationAndHumanReview() {
        SmartAnnotationRequests.Box valid = box(1, 0, .9), invalid = box(1, 0, .8), manual = box(1, 0, null);
        invalid.setWidth(Double.NaN);
        assertEquals(3, DetectionPredictionFilter.suppressDuplicates(Arrays.asList(valid, invalid, manual)).size());
    }

    @Test void emptyInputIsSupported() {
        assertTrue(DetectionPredictionFilter.suppressDuplicates(Collections.emptyList()).isEmpty());
        assertFalse(DetectionPredictionFilter.hasClassConflict(Collections.emptyList()));
    }
}
