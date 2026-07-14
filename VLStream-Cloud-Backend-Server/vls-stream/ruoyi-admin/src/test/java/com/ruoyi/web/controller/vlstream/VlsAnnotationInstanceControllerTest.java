/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnnotationInstance;
import com.ruoyi.vlstream.service.IVlsAnnotationInstanceService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAnnotationInstanceControllerTest {

    @Test
    void exposesFrontendAnnotationInstanceRoutes() throws Exception {
        Method byImage = VlsAnnotationInstanceController.class.getDeclaredMethod("getAnnotationInstances", Long.class, String.class);
        Method all = VlsAnnotationInstanceController.class.getDeclaredMethod("getAllAnnotationInstances", Long.class);
        Method batchSave = VlsAnnotationInstanceController.class.getDeclaredMethod("batchSaveAnnotationInstances", Long.class, Map.class);
        Method deleteOne = VlsAnnotationInstanceController.class.getDeclaredMethod("deleteAnnotationInstance", Long.class);
        Method deleteBatch = VlsAnnotationInstanceController.class.getDeclaredMethod("batchDeleteAnnotationInstances", List.class);
        Method deleteByImage = VlsAnnotationInstanceController.class.getDeclaredMethod("deleteAnnotationInstancesByImage", Map.class);

        assertArrayEquals(new String[] {"/{annotationId}/instances"}, byImage.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{annotationId}/instances/all"}, all.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{annotationId}/instances/batch"}, batchSave.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/instances/{instanceId}"}, deleteOne.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/instances/batch"}, deleteBatch.getAnnotation(DeleteMapping.class).value());
        assertArrayEquals(new String[] {"/instances/by-image"}, deleteByImage.getAnnotation(DeleteMapping.class).value());
    }

    @Test
    void listRoutesReturnBladeEnvelope() {
        IVlsAnnotationInstanceService service = mock(IVlsAnnotationInstanceService.class);
        VlsAnnotationInstanceController controller = new VlsAnnotationInstanceController(service);
        AnnotationInstance instance = instance(7L, 19L, 23L, 361L);
        List<AnnotationInstance> instances = Collections.singletonList(instance);

        when(service.getAnnotationInstances(19L, "361")).thenReturn(instances);
        when(service.getAllAnnotationInstances(19L)).thenReturn(instances);

        BladeResult<List<AnnotationInstance>> byImage = controller.getAnnotationInstances(19L, "361");
        BladeResult<List<AnnotationInstance>> all = controller.getAllAnnotationInstances(19L);

        assertEquals(200, byImage.getCode());
        assertSame(instances, byImage.getData());
        assertSame(instances, all.getData());
        verify(service).getAnnotationInstances(19L, "361");
        verify(service).getAllAnnotationInstances(19L);
    }

    @Test
    void batchSaveParsesFrontendBody() {
        IVlsAnnotationInstanceService service = mock(IVlsAnnotationInstanceService.class);
        VlsAnnotationInstanceController controller = new VlsAnnotationInstanceController(service);
        Map<String, Object> first = new HashMap<String, Object>();
        first.put("labelId", "23");
        first.put("annotationType", "rect");
        first.put("annotationData", "{\"x\":1}");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("imageId", "361");
        body.put("instances", Collections.singletonList(first));

        when(service.batchSaveAnnotationInstances(eq(19L), eq("361"), org.mockito.ArgumentMatchers.<AnnotationInstance>anyList())).thenReturn(true);

        BladeResult<Boolean> response = controller.batchSaveAnnotationInstances(19L, body);

        ArgumentCaptor<List<AnnotationInstance>> captor = ArgumentCaptor.forClass(List.class);
        verify(service).batchSaveAnnotationInstances(eq(19L), eq("361"), captor.capture());
        assertEquals(200, response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
        assertEquals(23L, captor.getValue().get(0).getLabelId());
        assertEquals("rect", captor.getValue().get(0).getAnnotationType());
        assertEquals("{\"x\":1}", captor.getValue().get(0).getAnnotationData());
    }

    @Test
    void deleteRoutesForwardIdsAndImageNames() {
        IVlsAnnotationInstanceService service = mock(IVlsAnnotationInstanceService.class);
        VlsAnnotationInstanceController controller = new VlsAnnotationInstanceController(service);
        List<Long> instanceIds = Arrays.asList(7L, 8L);
        Map<String, Object> byImageBody = new HashMap<String, Object>();
        byImageBody.put("annotationId", "19");
        byImageBody.put("imageIds", Arrays.asList("train_1.jpg", "361"));

        when(service.deleteAnnotationInstance(7L)).thenReturn(true);
        when(service.batchDeleteAnnotationInstances(instanceIds)).thenReturn(true);
        when(service.deleteAnnotationInstancesByImage(19L, Arrays.asList("train_1.jpg", "361"))).thenReturn(true);

        assertEquals(Boolean.TRUE, controller.deleteAnnotationInstance(7L).getData());
        assertEquals(Boolean.TRUE, controller.batchDeleteAnnotationInstances(instanceIds).getData());
        assertEquals(Boolean.TRUE, controller.deleteAnnotationInstancesByImage(byImageBody).getData());
        verify(service).deleteAnnotationInstance(7L);
        verify(service).batchDeleteAnnotationInstances(instanceIds);
        verify(service).deleteAnnotationInstancesByImage(19L, Arrays.asList("train_1.jpg", "361"));
    }

    private AnnotationInstance instance(Long id, Long annotationId, Long labelId, Long imageId) {
        AnnotationInstance instance = new AnnotationInstance();
        instance.setId(id);
        instance.setAnnotationId(annotationId);
        instance.setLabelId(labelId);
        instance.setImageId(imageId);
        instance.setAnnotationType("rect");
        instance.setAnnotationData("{\"x\":1}");
        return instance;
    }
}
