package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class DatasetPartitionerTest {
    @Test void randomPartitionIsDisjointDeterministicAndUsesExactRatio() {
        List<AnnotationImage> samples = samples(10);
        DataRequests.Split request = new DataRequests.Split();
        Map<Long, String> result = DatasetPartitioner.partition(samples, Collections.emptyMap(), request);
        assertEquals(10, result.size()); assertEquals(8, result.values().stream().filter("train"::equals).count());
        assertEquals(2, result.values().stream().filter("val"::equals).count());
        Collections.reverse(samples);
        assertEquals(result, DatasetPartitioner.partition(samples, Collections.emptyMap(), request));
    }

    @Test void manualPartitionRejectsForeignAndIneligibleIds() {
        DataRequests.Split request = new DataRequests.Split(); request.setMode("manual"); request.setValidationIds(Collections.singletonList(999L));
        assertThrows(ServiceException.class, () -> DatasetPartitioner.partition(samples(4), Collections.emptyMap(), request));
        request.setValidationIds(Arrays.asList(1L, 2L, 3L, 4L));
        assertThrows(ServiceException.class, () -> DatasetPartitioner.partition(samples(4), Collections.emptyMap(), request));
        request.setValidationIds(Collections.singletonList(2L));
        assertEquals("val", DatasetPartitioner.partition(samples(4), Collections.emptyMap(), request).get(2L));
    }

    @Test void duplicateContentNeverLeaksAcrossPartitions() {
        List<AnnotationImage> samples = samples(4); samples.get(0).setContentSha256("same"); samples.get(1).setContentSha256("same");
        DataRequests.Split request = new DataRequests.Split();
        Map<Long, String> result = DatasetPartitioner.partition(samples, Collections.emptyMap(), request);
        assertEquals(result.get(1L), result.get(2L));
        request.setMode("manual"); request.setValidationIds(Collections.singletonList(1L));
        assertThrows(ServiceException.class, () -> DatasetPartitioner.partition(samples, Collections.emptyMap(), request));
    }

    @Test void singletonClassesStillProduceTwoNonEmptySets() {
        DataRequests.Split request = new DataRequests.Split(); request.setMode("stratified"); request.setTrainPercent(99);
        Map<Long, String> classes = new HashMap<>(); classes.put(1L, "car"); classes.put(2L, "person");
        Map<Long, String> result = DatasetPartitioner.partition(samples(2), classes, request);
        assertTrue(result.containsValue("train")); assertTrue(result.containsValue("val"));
    }

    @Test void rejectsInvalidModeAndInsufficientUniqueSamples() {
        assertThrows(ServiceException.class, () -> DatasetPartitioner.partition(samples(1), Collections.emptyMap(), new DataRequests.Split()));
        DataRequests.Split request = new DataRequests.Split(); request.setMode("invalid");
        assertThrows(ServiceException.class, () -> DatasetPartitioner.partition(samples(3), Collections.emptyMap(), request));
    }

    private List<AnnotationImage> samples(int count) {
        List<AnnotationImage> result = new ArrayList<>();
        for (long id = 1; id <= count; id++) { AnnotationImage sample = new AnnotationImage(); sample.setId(id); sample.setContentSha256("hash" + id); result.add(sample); }
        return result;
    }
}
