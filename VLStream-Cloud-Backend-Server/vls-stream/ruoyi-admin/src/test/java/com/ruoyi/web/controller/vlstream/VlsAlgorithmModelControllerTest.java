/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmModel;
import com.ruoyi.vlstream.service.IVlsAlgorithmModelService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
class VlsAlgorithmModelControllerTest {

    @Test
    void exposesFrontendAlgorithmModelRoutes() throws Exception {
        Method page = VlsAlgorithmModelController.class.getDeclaredMethod(
            "getModelPage", Long.class, Long.class, String.class, Long.class, Long.class, String.class, String.class, String.class);
        Method byId = VlsAlgorithmModelController.class.getDeclaredMethod("getModelById", Long.class);
        Method create = VlsAlgorithmModelController.class.getDeclaredMethod("createModel", AlgorithmModel.class);
        Method update = VlsAlgorithmModelController.class.getDeclaredMethod("updateModel", AlgorithmModel.class);
        Method delete = VlsAlgorithmModelController.class.getDeclaredMethod("deleteModel", Long.class);
        Method batchDelete = VlsAlgorithmModelController.class.getDeclaredMethod("batchDeleteModel", List.class);
        Method byAlgorithm = VlsAlgorithmModelController.class.getDeclaredMethod("getModelsByAlgorithmId", Long.class);
        Method byTraining = VlsAlgorithmModelController.class.getDeclaredMethod("getModelsByTrainingId", Long.class);
        Method byStatus = VlsAlgorithmModelController.class.getDeclaredMethod("getModelsByStatus", String.class);
        Method publish = VlsAlgorithmModelController.class.getDeclaredMethod("publishModel", Long.class);
        Method unpublish = VlsAlgorithmModelController.class.getDeclaredMethod("unpublishModel", Long.class);
        Method batchPublish = VlsAlgorithmModelController.class.getDeclaredMethod("batchPublishModel", List.class);
        Method download = VlsAlgorithmModelController.class.getDeclaredMethod("downloadModel", Long.class);
        Method deploy = VlsAlgorithmModelController.class.getDeclaredMethod("deployModel", Long.class);
        Method statistics = VlsAlgorithmModelController.class.getDeclaredMethod("getModelStatistics");
        Method checkNameVersion = VlsAlgorithmModelController.class.getDeclaredMethod("checkModelNameAndVersion", String.class, Integer.class, Long.class);
        Method byAlgorithmVersion = VlsAlgorithmModelController.class.getDeclaredMethod("getModelByAlgorithmIdAndVersion", Long.class, Integer.class);
        Method latest = VlsAlgorithmModelController.class.getDeclaredMethod("getLatestModelByAlgorithmId", Long.class);
        Method popular = VlsAlgorithmModelController.class.getDeclaredMethod("getPopularModels", Integer.class);
        Method countCreator = VlsAlgorithmModelController.class.getDeclaredMethod("countModelsByCreatedBy", Long.class);
        Method totalSize = VlsAlgorithmModelController.class.getDeclaredMethod("getTotalModelSize");

        assertArrayEquals(new String[] {"/page"}, page.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, byId.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/create"}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/update"}, update.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/batch"}, batchDelete.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/algorithm/{algorithmId}"}, byAlgorithm.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/training/{trainingId}"}, byTraining.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/status/{status}"}, byStatus.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/publish/{id}"}, publish.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/unpublish/{id}"}, unpublish.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/batch-publish"}, batchPublish.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/download/{id}"}, download.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/deploy/{id}"}, deploy.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/statistics"}, statistics.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/check-name-version"}, checkNameVersion.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/algorithm/{algorithmId}/version/{version}"}, byAlgorithmVersion.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/algorithm/{algorithmId}/latest"}, latest.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/popular"}, popular.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/count/creator/{createdBy}"}, countCreator.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/total-size"}, totalSize.getAnnotation(GetMapping.class).value());
    }

    @Test
    void pageReturnsBladePageEnvelopeAndAcceptsFrontendFilters() {
        IVlsAlgorithmModelService service = mock(IVlsAlgorithmModelService.class);
        VlsAlgorithmModelController controller = new VlsAlgorithmModelController(service);
        AlgorithmModel model = new AlgorithmModel();
        model.setId(9L);
        model.setModelName("Fire detector model");
        BladePage<AlgorithmModel> page = BladePage.of(Collections.singletonList(model), 1L, 20L, 2L);

        when(service.getModelPage(2L, 20L, "Fire", 3L, 4L, "published", "2026-01-01", "2026-01-31")).thenReturn(page);

        BladeResult<BladePage<AlgorithmModel>> result =
            controller.getModelPage(2L, 20L, "Fire", 3L, 4L, "published", "2026-01-01", "2026-01-31");

        assertEquals(200, result.getCode());
        assertEquals(page, result.getData());
        assertEquals("Fire detector model", result.getData().getRecords().get(0).getModelName());
        verify(service).getModelPage(2L, 20L, "Fire", 3L, 4L, "published", "2026-01-01", "2026-01-31");
    }

    @Test
    void crudPublishDownloadAndDeployRoutesForwardToService() {
        IVlsAlgorithmModelService service = mock(IVlsAlgorithmModelService.class);
        VlsAlgorithmModelController controller = new VlsAlgorithmModelController(service);
        AlgorithmModel model = new AlgorithmModel();
        model.setId(7L);
        model.setModelName("Smoke model");
        List<Long> ids = Arrays.asList(1L, 2L);

        when(service.createModel(model)).thenReturn(model);
        when(service.updateModel(model)).thenReturn(model);
        when(service.deleteModel(7L)).thenReturn(true);
        when(service.deleteModels(ids)).thenReturn(true);
        when(service.publishModel(7L)).thenReturn(true);
        when(service.unpublishModel(7L)).thenReturn(true);
        when(service.publishModels(ids)).thenReturn(true);
        when(service.downloadModel(7L)).thenReturn("/models/smoke.pt");
        when(service.deployModel(7L)).thenReturn(true);

        assertEquals(model, controller.createModel(model).getData());
        assertEquals(model, controller.updateModel(model).getData());
        assertEquals(Boolean.TRUE, controller.deleteModel(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteModel(ids).getData());
        assertEquals(Boolean.TRUE, controller.publishModel(7L).getData());
        assertEquals(Boolean.TRUE, controller.unpublishModel(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchPublishModel(ids).getData());
        assertEquals("/models/smoke.pt", controller.downloadModel(7L).getData());
        assertEquals(Boolean.TRUE, controller.deployModel(7L).getData());

        verify(service).createModel(model);
        verify(service).updateModel(model);
        verify(service).deleteModel(7L);
        verify(service).deleteModels(ids);
        verify(service).publishModel(7L);
        verify(service).unpublishModel(7L);
        verify(service).publishModels(ids);
        verify(service).downloadModel(7L);
        verify(service).deployModel(7L);
    }

    @Test
    void lookupStatisticsAndUtilityRoutesReturnServiceData() {
        IVlsAlgorithmModelService service = mock(IVlsAlgorithmModelService.class);
        VlsAlgorithmModelController controller = new VlsAlgorithmModelController(service);
        AlgorithmModel model = new AlgorithmModel();
        model.setId(7L);
        List<AlgorithmModel> models = Collections.singletonList(model);
        Map<String, Object> statistics = new HashMap<String, Object>();
        statistics.put("total", 3L);

        when(service.getModelById(7L)).thenReturn(model);
        when(service.getModelsByAlgorithmId(3L)).thenReturn(models);
        when(service.getModelsByTrainingId(4L)).thenReturn(models);
        when(service.getModelsByStatus("published")).thenReturn(models);
        when(service.getModelStatistics()).thenReturn(statistics);
        when(service.checkModelNameAndVersion("m", 1, 7L)).thenReturn(true);
        when(service.getModelByAlgorithmIdAndVersion(3L, 1)).thenReturn(model);
        when(service.getLatestModelByAlgorithmId(3L)).thenReturn(model);
        when(service.getPopularModels(5)).thenReturn(models);
        when(service.countModelsByCreatedBy(9L)).thenReturn(2L);
        when(service.getTotalModelSize()).thenReturn(1024L);

        assertEquals(model, controller.getModelById(7L).getData());
        assertEquals(models, controller.getModelsByAlgorithmId(3L).getData());
        assertEquals(models, controller.getModelsByTrainingId(4L).getData());
        assertEquals(models, controller.getModelsByStatus("published").getData());
        assertEquals(statistics, controller.getModelStatistics().getData());
        assertEquals(Boolean.TRUE, controller.checkModelNameAndVersion("m", 1, 7L).getData());
        assertEquals(model, controller.getModelByAlgorithmIdAndVersion(3L, 1).getData());
        assertEquals(model, controller.getLatestModelByAlgorithmId(3L).getData());
        assertEquals(models, controller.getPopularModels(5).getData());
        assertEquals(Long.valueOf(2L), controller.countModelsByCreatedBy(9L).getData());
        assertEquals(Long.valueOf(1024L), controller.getTotalModelSize().getData());

        verify(service).getModelById(7L);
        verify(service).getModelsByAlgorithmId(3L);
        verify(service).getModelsByTrainingId(4L);
        verify(service).getModelsByStatus("published");
        verify(service).getModelStatistics();
        verify(service).checkModelNameAndVersion("m", 1, 7L);
        verify(service).getModelByAlgorithmIdAndVersion(3L, 1);
        verify(service).getLatestModelByAlgorithmId(3L);
        verify(service).getPopularModels(5);
        verify(service).countModelsByCreatedBy(9L);
        verify(service).getTotalModelSize();
    }
}
