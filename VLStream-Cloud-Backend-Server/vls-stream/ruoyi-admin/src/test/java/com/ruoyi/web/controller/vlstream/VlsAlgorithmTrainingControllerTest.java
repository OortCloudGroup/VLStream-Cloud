/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.service.IVlsAlgorithmTrainingService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

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
class VlsAlgorithmTrainingControllerTest {

    @Test
    void exposesFrontendAlgorithmTrainingRoutes() throws Exception {
        Method page = VlsAlgorithmTrainingController.class.getDeclaredMethod(
            "getTrainingPage", Long.class, Long.class, String.class, String.class, String.class, String.class);
        Method byId = VlsAlgorithmTrainingController.class.getDeclaredMethod("getTrainingById", Long.class);
        Method create = VlsAlgorithmTrainingController.class.getDeclaredMethod("createTraining", AlgorithmTraining.class);
        Method update = VlsAlgorithmTrainingController.class.getDeclaredMethod("updateTraining", Long.class, AlgorithmTraining.class);
        Method delete = VlsAlgorithmTrainingController.class.getDeclaredMethod("deleteTraining", Long.class);
        Method start = VlsAlgorithmTrainingController.class.getDeclaredMethod("startTraining", Long.class, Integer.class, Long.class, Integer.class, Integer.class, String.class);
        Method stop = VlsAlgorithmTrainingController.class.getDeclaredMethod("stopTraining", Long.class);
        Method logs = VlsAlgorithmTrainingController.class.getDeclaredMethod("getTrainingLogs", Long.class, String.class, Integer.class);
        Method status = VlsAlgorithmTrainingController.class.getDeclaredMethod("getTrainingStatus", Long.class, String.class);
        Method batchDelete = VlsAlgorithmTrainingController.class.getDeclaredMethod("batchDeleteTraining", List.class);
        Method convert = VlsAlgorithmTrainingController.class.getDeclaredMethod("convertModel", Long.class);
        Method download = VlsAlgorithmTrainingController.class.getDeclaredMethod("downloadModel", String.class, String.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, byId.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/start"}, start.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/stop"}, stop.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/logs"}, logs.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/status"}, status.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batchDelete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/convert-model"}, convert.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/download-model"}, download.getAnnotation(GetMapping.class).value());
    }

    @Test
    void pageReturnsBladePageEnvelopeAndAcceptsFrontendFilters() {
        IVlsAlgorithmTrainingService service = mock(IVlsAlgorithmTrainingService.class);
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController(service);
        AlgorithmTraining training = new AlgorithmTraining();
        training.setId(9L);
        training.setTaskName("Fire training");
        training.setTrainStatus("pending");
        BladePage<AlgorithmTraining> page = BladePage.of(Collections.singletonList(training), 1L, 20L, 2L);

        when(service.getTrainingPage(2L, 20L, "Fire", "pending", "2026-01-01", "2026-01-31")).thenReturn(page);

        BladeResult<BladePage<AlgorithmTraining>> result =
            controller.getTrainingPage(2L, 20L, "Fire", "pending", "2026-01-01", "2026-01-31");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        assertEquals("Fire training", result.getData().getRecords().get(0).getTaskName());
        verify(service).getTrainingPage(2L, 20L, "Fire", "pending", "2026-01-01", "2026-01-31");
    }

    @Test
    void crudAndLifecycleRoutesForwardToService() {
        IVlsAlgorithmTrainingService service = mock(IVlsAlgorithmTrainingService.class);
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController(service);
        AlgorithmTraining training = new AlgorithmTraining();
        training.setId(7L);
        training.setTaskName("Smoke training");
        List<Long> ids = Arrays.asList(1L, 2L);
        Map<String, Object> startResult = new HashMap<String, Object>();
        startResult.put("status", "training");
        startResult.put("logPath", "/logs/training_7.log");
        Map<String, Object> convertResult = new HashMap<String, Object>();
        convertResult.put("status", "submitted");

        when(service.createTraining(training)).thenReturn(training);
        when(service.updateTraining(7L, training)).thenReturn(training);
        when(service.deleteTraining(7L)).thenReturn(true);
        when(service.deleteTrainings(ids)).thenReturn(true);
        when(service.startTraining(7L, 10, 5L, 16, 640, "cache=true")).thenReturn(startResult);
        when(service.stopTraining(7L)).thenReturn(true);
        when(service.convertModel(7L)).thenReturn(convertResult);

        assertEquals(training, controller.createTraining(training).getData());
        assertEquals(training, controller.updateTraining(7L, training).getData());
        assertEquals(Boolean.TRUE, controller.deleteTraining(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteTraining(ids).getData());
        assertEquals(startResult, controller.startTraining(7L, 10, 5L, 16, 640, "cache=true").getData());
        assertEquals(Boolean.TRUE, controller.stopTraining(7L).getData());
        assertEquals(convertResult, controller.convertModel(7L).getData());

        verify(service).createTraining(training);
        verify(service).updateTraining(7L, training);
        verify(service).deleteTraining(7L);
        verify(service).deleteTrainings(ids);
        verify(service).startTraining(7L, 10, 5L, 16, 640, "cache=true");
        verify(service).stopTraining(7L);
        verify(service).convertModel(7L);
    }

    @Test
    void lookupLogsStatusAndDownloadRoutesReturnServiceData() {
        IVlsAlgorithmTrainingService service = mock(IVlsAlgorithmTrainingService.class);
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController(service);
        AlgorithmTraining training = new AlgorithmTraining();
        training.setId(7L);
        training.setTaskName("Smoke training");
        Map<String, Object> logs = new HashMap<String, Object>();
        logs.put("logContent", "training completed");
        logs.put("status", "completed");
        logs.put("modelPath", "/models/smoke.pt");
        Map<String, Object> status = new HashMap<String, Object>();
        status.put("trainStatus", "completed");
        status.put("completed", Boolean.TRUE);
        byte[] modelBytes = "model-content".getBytes();

        when(service.getTrainingById(7L)).thenReturn(training);
        when(service.getTrainingLogs(7L, "/logs/training_7.log", 100)).thenReturn(logs);
        when(service.getTrainingStatus(7L, "/logs/training_7.log")).thenReturn(status);
        when(service.downloadModel("7", "pt")).thenReturn(ResponseEntity.ok(modelBytes));

        assertEquals(training, controller.getTrainingById(7L).getData());
        assertEquals(logs, controller.getTrainingLogs(7L, "/logs/training_7.log", 100).getData());
        assertEquals(status, controller.getTrainingStatus(7L, "/logs/training_7.log").getData());
        ResponseEntity<byte[]> response = controller.downloadModel("7", "pt");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertArrayEquals(modelBytes, response.getBody());

        verify(service).getTrainingById(7L);
        verify(service).getTrainingLogs(7L, "/logs/training_7.log", 100);
        verify(service).getTrainingStatus(7L, "/logs/training_7.log");
        verify(service).downloadModel("7", "pt");
    }
}
