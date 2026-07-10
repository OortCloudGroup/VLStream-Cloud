package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AnnotationLabel;
import com.ruoyi.vlstream.service.IVlsAnnotationLabelService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAnnotationLabelControllerTest {

    @Test
    void exposesFrontendAnnotationLabelRoutes() throws Exception {
        Method list = VlsAnnotationLabelController.class.getDeclaredMethod("getLabels", Long.class, String.class);
        Method create = VlsAnnotationLabelController.class.getDeclaredMethod("createLabel", Long.class, AnnotationLabel.class);
        Method update = VlsAnnotationLabelController.class.getDeclaredMethod("updateLabel", Long.class, AnnotationLabel.class);
        Method delete = VlsAnnotationLabelController.class.getDeclaredMethod("deleteLabel", Long.class);

        assertArrayEquals(new String[] {"/{annotationId}/labels"}, list.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[] {"/{annotationId}/labels"}, create.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, update.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[] {"/{id}"}, delete.getAnnotation(DeleteMapping.class).value());
    }

    @Test
    void listReturnsBladeEnvelopeAndAcceptsKeyword() {
        IVlsAnnotationLabelService service = mock(IVlsAnnotationLabelService.class);
        VlsAnnotationLabelController controller = new VlsAnnotationLabelController(service);
        AnnotationLabel label = label(7L, 19L, "fire", "#409eff", 1);
        List<AnnotationLabel> labels = Collections.singletonList(label);

        when(service.getLabels(19L, "fir")).thenReturn(labels);

        BladeResult<List<AnnotationLabel>> response = controller.getLabels(19L, "fir");

        assertEquals(200, response.getCode());
        assertEquals("操作成功", response.getMsg());
        assertSame(labels, response.getData());
        verify(service).getLabels(19L, "fir");
    }

    @Test
    void createAndUpdateReturnSavedLabelForFrontendSelection() {
        IVlsAnnotationLabelService service = mock(IVlsAnnotationLabelService.class);
        VlsAnnotationLabelController controller = new VlsAnnotationLabelController(service);
        AnnotationLabel request = label(null, null, "smoke", "#f56c6c", null);
        AnnotationLabel created = label(8L, 19L, "smoke", "#f56c6c", 2);
        AnnotationLabel updated = label(8L, 19L, "smoke-updated", "#67c23a", 2);

        when(service.createLabel(19L, request)).thenReturn(created);
        when(service.updateLabel(8L, request)).thenReturn(updated);

        BladeResult<AnnotationLabel> createResponse = controller.createLabel(19L, request);
        BladeResult<AnnotationLabel> updateResponse = controller.updateLabel(8L, request);

        assertEquals(200, createResponse.getCode());
        assertSame(created, createResponse.getData());
        assertEquals(8L, createResponse.getData().getId());
        assertSame(updated, updateResponse.getData());
        verify(service).createLabel(19L, request);
        verify(service).updateLabel(8L, request);
    }

    @Test
    void deleteReturnsBooleanResult() {
        IVlsAnnotationLabelService service = mock(IVlsAnnotationLabelService.class);
        VlsAnnotationLabelController controller = new VlsAnnotationLabelController(service);

        when(service.deleteLabel(8L)).thenReturn(true);

        BladeResult<Boolean> response = controller.deleteLabel(8L);

        assertEquals(200, response.getCode());
        assertEquals(Boolean.TRUE, response.getData());
        verify(service).deleteLabel(8L);
    }

    private AnnotationLabel label(Long id, Long annotationId, String name, String color, Integer sortOrder) {
        AnnotationLabel label = new AnnotationLabel();
        label.setId(id);
        label.setAnnotationId(annotationId);
        label.setName(name);
        label.setColor(color);
        label.setSortOrder(sortOrder);
        label.setUsageCount(0);
        return label;
    }
}
