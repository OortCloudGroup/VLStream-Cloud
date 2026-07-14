/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.Algorithm;
import com.ruoyi.vlstream.service.IVlsAlgorithmService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAlgorithmControllerTest {

    @Test
    void exposesFrontendAlgorithmRoutes() throws Exception {
        Method page = VlsAlgorithmController.class.getDeclaredMethod(
            "getAlgorithmPage", Long.class, Long.class, Long.class, String.class, String.class, String.class, String.class);
        Method byRepository = VlsAlgorithmController.class.getDeclaredMethod("getAlgorithmsByRepositoryId", Long.class);
        Method byCategory = VlsAlgorithmController.class.getDeclaredMethod("getAlgorithmsByCategory", String.class);
        Method byId = VlsAlgorithmController.class.getDeclaredMethod("getAlgorithmById", Long.class);
        Method create = VlsAlgorithmController.class.getDeclaredMethod("createAlgorithm", Algorithm.class);
        Method update = VlsAlgorithmController.class.getDeclaredMethod("updateAlgorithm", Long.class, Algorithm.class);
        Method delete = VlsAlgorithmController.class.getDeclaredMethod("deleteAlgorithm", Long.class);
        Method batchDelete = VlsAlgorithmController.class.getDeclaredMethod("batchDeleteAlgorithms", List.class);
        Method deployStatus = VlsAlgorithmController.class.getDeclaredMethod("updateDeployStatus", Long.class, String.class);
        Method batchDeployStatus = VlsAlgorithmController.class.getDeclaredMethod("batchUpdateDeployStatus", List.class, String.class);
        Method deploy = VlsAlgorithmController.class.getDeclaredMethod("deployAlgorithmToDevices", Long.class, List.class);
        Method evaluate = VlsAlgorithmController.class.getDeclaredMethod("evaluateAlgorithm", Long.class);
        Method categoryStatistics = VlsAlgorithmController.class.getDeclaredMethod("getCategoryStatistics");
        Method typeStatistics = VlsAlgorithmController.class.getDeclaredMethod("getTypeStatistics");
        Method deployStatusStatistics = VlsAlgorithmController.class.getDeclaredMethod("getDeployStatusStatistics");
        Method countByRepository = VlsAlgorithmController.class.getDeclaredMethod("countByRepositoryId", Long.class);

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/repository/{repositoryId}"}, byRepository.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/category/{category}"}, byCategory.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, byId.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batchDelete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/{id}/deploy-status"}, deployStatus.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/batch/deploy-status"}, batchDeployStatus.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{algorithmId}/deploy"}, deploy.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{algorithmId}/evaluate"}, evaluate.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/category"}, categoryStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/type"}, typeStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/statistics/deploy-status"}, deployStatusStatistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/count/repository/{repositoryId}"}, countByRepository.getAnnotation(GetMapping.class).value());
    }

    @Test
    void pageReturnsBladePageEnvelopeAndAcceptsFrontendFilters() {
        IVlsAlgorithmService service = mock(IVlsAlgorithmService.class);
        VlsAlgorithmController controller = new VlsAlgorithmController(service);
        Algorithm algorithm = new Algorithm();
        algorithm.setId(9L);
        algorithm.setName("Fire detector");
        BladePage<Algorithm> page = BladePage.of(Collections.singletonList(algorithm), 1L, 20L, 2L);

        when(service.getAlgorithmPage(2L, 20L, 3L, "Fire", "detect", "image", "deployed")).thenReturn(page);

        BladeResult<BladePage<Algorithm>> result =
            controller.getAlgorithmPage(2L, 20L, 3L, "Fire", "detect", "image", "deployed");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        assertEquals("Fire detector", result.getData().getRecords().get(0).getName());
        verify(service).getAlgorithmPage(2L, 20L, 3L, "Fire", "detect", "image", "deployed");
    }

    @Test
    void crudAndDeployRoutesForwardToService() {
        IVlsAlgorithmService service = mock(IVlsAlgorithmService.class);
        VlsAlgorithmController controller = new VlsAlgorithmController(service);
        Algorithm algorithm = new Algorithm();
        algorithm.setName("Smoke detector");
        List<Long> ids = Arrays.asList(1L, 2L);
        Map<String, Object> deployResult = new HashMap<String, Object>();
        deployResult.put("algorithmId", 7L);
        deployResult.put("deviceIds", ids);

        when(service.createAlgorithm(algorithm)).thenReturn(algorithm);
        when(service.updateAlgorithm(7L, algorithm)).thenReturn(algorithm);
        when(service.deleteAlgorithm(7L)).thenReturn(true);
        when(service.deleteAlgorithms(ids)).thenReturn(true);
        when(service.updateDeployStatus(7L, "deployed")).thenReturn(true);
        when(service.updateDeployStatus(ids, "offline")).thenReturn(true);
        when(service.deployAlgorithmToDevices(7L, ids)).thenReturn(deployResult);

        assertEquals(algorithm, controller.createAlgorithm(algorithm).getData());
        assertEquals(algorithm, controller.updateAlgorithm(7L, algorithm).getData());
        assertEquals(Boolean.TRUE, controller.deleteAlgorithm(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteAlgorithms(ids).getData());
        assertEquals(Boolean.TRUE, controller.updateDeployStatus(7L, "deployed").getData());
        assertEquals(Boolean.TRUE, controller.batchUpdateDeployStatus(ids, "offline").getData());
        assertEquals(deployResult, controller.deployAlgorithmToDevices(7L, ids).getData());

        verify(service).createAlgorithm(algorithm);
        verify(service).updateAlgorithm(7L, algorithm);
        verify(service).deleteAlgorithm(7L);
        verify(service).deleteAlgorithms(ids);
        verify(service).updateDeployStatus(7L, "deployed");
        verify(service).updateDeployStatus(ids, "offline");
        verify(service).deployAlgorithmToDevices(7L, ids);
    }

    @Test
    void lookupStatisticsAndEvaluationRoutesReturnServiceData() {
        IVlsAlgorithmService service = mock(IVlsAlgorithmService.class);
        VlsAlgorithmController controller = new VlsAlgorithmController(service);
        Algorithm algorithm = new Algorithm();
        algorithm.setId(7L);
        List<Algorithm> algorithms = Collections.singletonList(algorithm);
        List<Map<String, Object>> statistics = Collections.singletonList(new HashMap<String, Object>());
        Map<String, Object> evaluation = new HashMap<String, Object>();
        evaluation.put("status", "completed");

        when(service.getAlgorithmsByRepositoryId(3L)).thenReturn(algorithms);
        when(service.getAlgorithmsByCategory("detect")).thenReturn(algorithms);
        when(service.getAlgorithmById(7L)).thenReturn(algorithm);
        when(service.getCategoryStatistics()).thenReturn(statistics);
        when(service.getTypeStatistics()).thenReturn(statistics);
        when(service.getDeployStatusStatistics()).thenReturn(statistics);
        when(service.countByRepositoryId(3L)).thenReturn(5L);
        when(service.evaluateAlgorithm(7L)).thenReturn(evaluation);

        assertEquals(algorithms, controller.getAlgorithmsByRepositoryId(3L).getData());
        assertEquals(algorithms, controller.getAlgorithmsByCategory("detect").getData());
        assertEquals(algorithm, controller.getAlgorithmById(7L).getData());
        assertEquals(statistics, controller.getCategoryStatistics().getData());
        assertEquals(statistics, controller.getTypeStatistics().getData());
        assertEquals(statistics, controller.getDeployStatusStatistics().getData());
        assertEquals(Long.valueOf(5L), controller.countByRepositoryId(3L).getData());
        assertEquals(evaluation, controller.evaluateAlgorithm(7L).getData());

        verify(service).getAlgorithmsByRepositoryId(3L);
        verify(service).getAlgorithmsByCategory("detect");
        verify(service).getAlgorithmById(7L);
        verify(service).getCategoryStatistics();
        verify(service).getTypeStatistics();
        verify(service).getDeployStatusStatistics();
        verify(service).countByRepositoryId(3L);
        verify(service).evaluateAlgorithm(7L);
    }
}
