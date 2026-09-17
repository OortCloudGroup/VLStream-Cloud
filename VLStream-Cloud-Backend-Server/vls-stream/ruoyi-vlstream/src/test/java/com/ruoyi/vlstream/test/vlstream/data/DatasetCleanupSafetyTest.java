package com.ruoyi.vlstream.test.vlstream.data;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class DatasetCleanupSafetyTest {
    @Test void neverTreatsModelsAsDatasetFiles() {
        for(String name:new String[]{"best.pt","best.onnx","best.om","best.rknn","model.bin","unknown"}) assertFalse(DatasetRemoteCleanup.datasetFile(name));
        assertTrue(DatasetRemoteCleanup.datasetFile("image.png")); assertTrue(DatasetRemoteCleanup.datasetFile("dataset.yaml"));
    }
    @Test void requiresExactPositiveProjectRoot() {
        assertThrows(IllegalArgumentException.class,()->DatasetRemoteCleanup.root(null));
        assertThrows(IllegalArgumentException.class,()->DatasetRemoteCleanup.root(0L));
        assertTrue(DatasetRemoteCleanup.root(123L).endsWith("/vls/annotation_123"));
    }
    @Test void rejectsUnknownBucketAndTraversal() {
        assertThrows(RuntimeException.class,()->DatasetCleanupService.key("http://host/other/a.png","images"));
        assertThrows(RuntimeException.class,()->DatasetCleanupService.key("../a.png","images"));
        assertEquals("a.png",DatasetCleanupService.key("http://host/images/a.png?expired=yes","images"));
    }
}
