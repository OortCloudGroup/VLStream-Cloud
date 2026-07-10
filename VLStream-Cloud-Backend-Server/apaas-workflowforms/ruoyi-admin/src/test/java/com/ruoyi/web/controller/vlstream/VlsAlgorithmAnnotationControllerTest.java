package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmAnnotation;
import com.ruoyi.vlstream.service.IVlsAlgorithmAnnotationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAlgorithmAnnotationControllerTest {

    @Test
    void exposesFrontendAlgorithmAnnotationRoutes() throws Exception {
        Method page = VlsAlgorithmAnnotationController.class.getDeclaredMethod(
            "getAnnotationPage", Long.class, Long.class, String.class, String.class, String.class, String.class, String.class);
        Method byId = VlsAlgorithmAnnotationController.class.getDeclaredMethod("getAnnotationById", Long.class);
        Method create = VlsAlgorithmAnnotationController.class.getDeclaredMethod("createAnnotation", AlgorithmAnnotation.class);
        Method update = VlsAlgorithmAnnotationController.class.getDeclaredMethod("updateAnnotation", Long.class, AlgorithmAnnotation.class);
        Method delete = VlsAlgorithmAnnotationController.class.getDeclaredMethod("deleteAnnotation", Long.class);
        Method batchDelete = VlsAlgorithmAnnotationController.class.getDeclaredMethod("batchDeleteAnnotations", List.class);
        Method start = VlsAlgorithmAnnotationController.class.getDeclaredMethod("startAnnotationTask", Long.class);
        Method complete = VlsAlgorithmAnnotationController.class.getDeclaredMethod("completeAnnotationTask", Long.class);
        Method reset = VlsAlgorithmAnnotationController.class.getDeclaredMethod("resetAnnotationTask", Long.class);
        Method progress = VlsAlgorithmAnnotationController.class.getDeclaredMethod("updateAnnotationProgress", Long.class, Map.class);
        Method export = VlsAlgorithmAnnotationController.class.getDeclaredMethod("exportAnnotationData", Long.class);
        Method importZip = VlsAlgorithmAnnotationController.class.getDeclaredMethod("importAnnotationZip", Long.class, MultipartFile.class);
        Method statistics = VlsAlgorithmAnnotationController.class.getDeclaredMethod("getStatistics");
        Method typeStatistics = VlsAlgorithmAnnotationController.class.getDeclaredMethod("getTypeStatistics");
        Method statusStatistics = VlsAlgorithmAnnotationController.class.getDeclaredMethod("getStatusStatistics");
        Method progressStatistics = VlsAlgorithmAnnotationController.class.getDeclaredMethod("getProgressStatistics");
        Method workloadStatistics = VlsAlgorithmAnnotationController.class.getDeclaredMethod("getWorkloadStatistics");
        Method search = VlsAlgorithmAnnotationController.class.getDeclaredMethod(
            "searchAnnotations", Long.class, Long.class, String.class, String.class, String.class);
        Method batchOperation = VlsAlgorithmAnnotationController.class.getDeclaredMethod("batchOperation", Map.class);
        Method saveDataset = VlsAlgorithmAnnotationController.class.getDeclaredMethod("saveDataset", Long.class, String.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, byId.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batchDelete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/start"}, start.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/complete"}, complete.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/reset"}, reset.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/progress"}, progress.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/export"}, export.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/import-zip"}, importZip.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/statistics"}, statistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/type"}, typeStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/status"}, statusStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/progress"}, progressStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/workload"}, workloadStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/search"}, search.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/batch-operation"}, batchOperation.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/save-dataset"}, saveDataset.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {}, saveDataset.getAnnotation(PostMapping.class).consumes());
    }

    @Test
    void pageAndSearchReturnBladePageEnvelopeAndAcceptFrontendFilters() {
        IVlsAlgorithmAnnotationService service = mock(IVlsAlgorithmAnnotationService.class);
        VlsAlgorithmAnnotationController controller = new VlsAlgorithmAnnotationController(service);
        AlgorithmAnnotation annotation = new AlgorithmAnnotation();
        annotation.setId(9L);
        annotation.setAnnotationName("Fire dataset");
        annotation.setAnnotationType("object_detection");
        annotation.setAnnotationStatus("none");
        BladePage<AlgorithmAnnotation> page = BladePage.of(Collections.singletonList(annotation), 1L, 20L, 2L);

        when(service.getAnnotationPage(2L, 20L, "Fire", "object_detection", "none", "2026-01-01", "2026-01-31")).thenReturn(page);
        when(service.searchAnnotations(1L, 10L, "Fire", "object_detection", "none")).thenReturn(page);

        BladeResult<BladePage<AlgorithmAnnotation>> pageResult =
            controller.getAnnotationPage(2L, 20L, "Fire", "object_detection", "none", "2026-01-01", "2026-01-31");
        BladeResult<BladePage<AlgorithmAnnotation>> searchResult =
            controller.searchAnnotations(1L, 10L, "Fire", "object_detection", "none");

        assertEquals(200, pageResult.getCode());
        assertEquals(page, pageResult.getData());
        assertEquals("Fire dataset", pageResult.getData().getRecords().get(0).getAnnotationName());
        assertEquals(page, searchResult.getData());
        verify(service).getAnnotationPage(2L, 20L, "Fire", "object_detection", "none", "2026-01-01", "2026-01-31");
        verify(service).searchAnnotations(1L, 10L, "Fire", "object_detection", "none");
    }

    @Test
    void crudTaskProgressAndBatchRoutesForwardToService() {
        IVlsAlgorithmAnnotationService service = mock(IVlsAlgorithmAnnotationService.class);
        VlsAlgorithmAnnotationController controller = new VlsAlgorithmAnnotationController(service);
        AlgorithmAnnotation annotation = new AlgorithmAnnotation();
        annotation.setId(7L);
        annotation.setAnnotationName("Smoke dataset");
        List<Long> ids = Arrays.asList(1L, 2L);
        Map<String, Object> progressBody = new HashMap<String, Object>();
        progressBody.put("annotatedCount", 8);
        Map<String, Object> batchBody = new HashMap<String, Object>();
        batchBody.put("operation", "complete");
        batchBody.put("ids", ids);

        when(service.getAnnotationById(7L)).thenReturn(annotation);
        when(service.createAnnotation(annotation)).thenReturn(annotation);
        when(service.updateAnnotation(7L, annotation)).thenReturn(annotation);
        when(service.deleteAnnotation(7L)).thenReturn(true);
        when(service.deleteAnnotations(ids)).thenReturn(true);
        when(service.startAnnotationTask(7L)).thenReturn(true);
        when(service.completeAnnotationTask(7L)).thenReturn(true);
        when(service.resetAnnotationTask(7L)).thenReturn(true);
        when(service.updateAnnotationProgress(7L, 8)).thenReturn(true);
        when(service.batchOperation("complete", ids)).thenReturn(true);
        when(service.saveDataset(7L, "{\"items\":[]}")).thenReturn(true);

        assertEquals(annotation, controller.getAnnotationById(7L).getData());
        assertEquals(annotation, controller.createAnnotation(annotation).getData());
        assertEquals(annotation, controller.updateAnnotation(7L, annotation).getData());
        assertEquals(Boolean.TRUE, controller.deleteAnnotation(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteAnnotations(ids).getData());
        assertEquals(Boolean.TRUE, controller.startAnnotationTask(7L).getData());
        assertEquals(Boolean.TRUE, controller.completeAnnotationTask(7L).getData());
        assertEquals(Boolean.TRUE, controller.resetAnnotationTask(7L).getData());
        assertEquals(Boolean.TRUE, controller.updateAnnotationProgress(7L, progressBody).getData());
        assertEquals(Boolean.TRUE, controller.batchOperation(batchBody).getData());
        assertEquals(Boolean.TRUE, controller.saveDataset(7L, "{\"items\":[]}").getData());

        Map<String, Object> missingOperationBody = new HashMap<String, Object>();
        missingOperationBody.put("ids", ids);
        assertEquals(Boolean.FALSE, controller.batchOperation(missingOperationBody).getData());

        verify(service).getAnnotationById(7L);
        verify(service).createAnnotation(annotation);
        verify(service).updateAnnotation(7L, annotation);
        verify(service).deleteAnnotation(7L);
        verify(service).deleteAnnotations(ids);
        verify(service).startAnnotationTask(7L);
        verify(service).completeAnnotationTask(7L);
        verify(service).resetAnnotationTask(7L);
        verify(service).updateAnnotationProgress(7L, 8);
        verify(service).batchOperation("complete", ids);
        verify(service).batchOperation(null, ids);
        verify(service).saveDataset(7L, "{\"items\":[]}");
    }

    @Test
    void importExportAndStatisticsRoutesReturnServiceData() {
        IVlsAlgorithmAnnotationService service = mock(IVlsAlgorithmAnnotationService.class);
        VlsAlgorithmAnnotationController controller = new VlsAlgorithmAnnotationController(service);
        MockMultipartFile zip = new MockMultipartFile("file", "dataset.zip", "application/zip", "zip".getBytes());
        Map<String, Object> importResult = new HashMap<String, Object>();
        importResult.put("success", true);
        Map<String, Object> statistics = new HashMap<String, Object>();
        statistics.put("total", 3L);
        List<Map<String, Object>> grouped = Collections.singletonList(new HashMap<String, Object>());
        byte[] exportBytes = "dataset".getBytes();

        when(service.exportAnnotationData(7L)).thenReturn(ResponseEntity.ok(exportBytes));
        when(service.importAnnotationZip(7L, zip)).thenReturn(importResult);
        when(service.getStatistics()).thenReturn(statistics);
        when(service.getTypeStatistics()).thenReturn(grouped);
        when(service.getStatusStatistics()).thenReturn(grouped);
        when(service.getProgressStatistics()).thenReturn(grouped);
        when(service.getWorkloadStatistics()).thenReturn(statistics);

        ResponseEntity<byte[]> exportResponse = controller.exportAnnotationData(7L);
        assertTrue(exportResponse.getStatusCode().is2xxSuccessful());
        assertArrayEquals(exportBytes, exportResponse.getBody());
        assertEquals(importResult, controller.importAnnotationZip(7L, zip).getData());
        assertEquals(statistics, controller.getStatistics().getData());
        assertEquals(grouped, controller.getTypeStatistics().getData());
        assertEquals(grouped, controller.getStatusStatistics().getData());
        assertEquals(grouped, controller.getProgressStatistics().getData());
        assertEquals(statistics, controller.getWorkloadStatistics().getData());

        verify(service).exportAnnotationData(7L);
        verify(service).importAnnotationZip(7L, zip);
        verify(service).getStatistics();
        verify(service).getTypeStatistics();
        verify(service).getStatusStatistics();
        verify(service).getProgressStatistics();
        verify(service).getWorkloadStatistics();
    }
}
