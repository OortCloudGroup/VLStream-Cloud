package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationStatusEnum;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class AnnotationMetadataEditTest {
    @Test void renameCannotResetImageStatistics() throws Exception {
        Constructor<?> constructor = VlsAlgorithmAnnotationServiceImpl.class.getConstructors()[0];
        Class<?>[] types = constructor.getParameterTypes();
        Object[] dependencies = new Object[types.length];
        for (int i=0;i<types.length;i++) dependencies[i]=mock(types[i]);
        VlsAlgorithmAnnotationServiceImpl service = spy((VlsAlgorithmAnnotationServiceImpl) constructor.newInstance(dependencies));
        AlgorithmAnnotation existing = new AlgorithmAnnotation();
        existing.setId(10L);existing.setTotalCount(116);existing.setAnnotatedCount(116);
        existing.setProgress(100);existing.setAnnotationStatus(AlgorithmAnnotationStatusEnum.of("completed"));
        doReturn(existing).when(service).getById(10L);
        doReturn(true).when(service).updateById(any(AlgorithmAnnotation.class));
        AlgorithmAnnotation edit = new AlgorithmAnnotation();
        edit.setId(10L);edit.setAnnotationName("新名称");edit.setTotalCount(0);edit.setAnnotatedCount(0);
        edit.setProgress(0);edit.setAnnotationStatus(AlgorithmAnnotationStatusEnum.of("none"));
        assertTrue(service.updateAnnotation(edit));
        assertEquals(116,edit.getTotalCount());assertEquals(116,edit.getAnnotatedCount());
        assertEquals(100,edit.getProgress());assertEquals(existing.getAnnotationStatus(),edit.getAnnotationStatus());
        assertEquals("新名称",edit.getAnnotationName());
    }
}
