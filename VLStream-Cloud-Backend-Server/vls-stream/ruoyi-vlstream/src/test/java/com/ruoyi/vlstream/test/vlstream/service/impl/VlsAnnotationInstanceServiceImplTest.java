package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationImageService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationLabelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class VlsAnnotationInstanceServiceImplTest {

    private static final Long ANNOTATION_ID = 10L;
    private static final Long IMAGE_ID = 20L;

    private VlsAnnotationInstanceMapper instanceMapper;
    private VlsAlgorithmAnnotationMapper annotationMapper;
    private VlsAnnotationImageMapper imageMapper;
    private VlsAnnotationInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        instanceMapper = mock(VlsAnnotationInstanceMapper.class);
        annotationMapper = mock(VlsAlgorithmAnnotationMapper.class);
        imageMapper = mock(VlsAnnotationImageMapper.class);
        service = new VlsAnnotationInstanceServiceImpl();

        ReflectionTestUtils.setField(service, "baseMapper", instanceMapper);
        ReflectionTestUtils.setField(service, "algorithmAnnotationMapper", annotationMapper);
        ReflectionTestUtils.setField(service, "annotationImageMapper", imageMapper);
        ReflectionTestUtils.setField(service, "annotationLabelService", mock(IVlsAnnotationLabelService.class));
        ReflectionTestUtils.setField(service, "annotationImageService", mock(IVlsAnnotationImageService.class));
        ReflectionTestUtils.setField(service, "dataManagementService", mock(com.ruoyi.vlstream.test.vlstream.data.DataManagementService.class));

        AlgorithmAnnotation annotation = new AlgorithmAnnotation();
        annotation.setId(ANNOTATION_ID);
        annotation.setTotalCount(4);
        when(annotationMapper.selectById(ANNOTATION_ID)).thenReturn(annotation);
        when(annotationMapper.updateById(any(AlgorithmAnnotation.class))).thenReturn(1);
        when(imageMapper.countActiveImages(ANNOTATION_ID)).thenReturn(4);
        when(instanceMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(instanceMapper.insert(any(AnnotationInstance.class))).thenReturn(1);
    }

    @Test
    void batchSaveCountsAnnotatedImagesInsteadOfBoundingBoxes() {
        AnnotationInstance firstBox = annotation(101L);
        AnnotationInstance secondBox = annotation(102L);
        when(instanceMapper.countDistinctAnnotatedImages(ANNOTATION_ID)).thenReturn(1);

        service.batchSaveAnnotations(ANNOTATION_ID, IMAGE_ID, Arrays.asList(firstBox, secondBox));

        AlgorithmAnnotation update = captureProgressUpdate();
        assertEquals(1, update.getAnnotatedCount());
        assertEquals(4, update.getTotalCount());
        assertEquals(25, update.getProgress());
        assertEquals(AlgorithmAnnotationStatusEnum.partial, update.getAnnotationStatus());
    }

    @Test
    void clearingImageAnnotationsResetsProgressWhenNoAnnotatedImagesRemain() {
        when(instanceMapper.countDistinctAnnotatedImages(ANNOTATION_ID)).thenReturn(0);

        service.batchSaveAnnotations(ANNOTATION_ID, IMAGE_ID, Collections.emptyList());

        AlgorithmAnnotation update = captureProgressUpdate();
        assertEquals(0, update.getAnnotatedCount());
        assertEquals(0, update.getProgress());
        assertEquals(AlgorithmAnnotationStatusEnum.none, update.getAnnotationStatus());
    }

    private AlgorithmAnnotation captureProgressUpdate() {
        ArgumentCaptor<AlgorithmAnnotation> captor = ArgumentCaptor.forClass(AlgorithmAnnotation.class);
        verify(annotationMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private AnnotationInstance annotation(Long labelId) {
        AnnotationInstance instance = new AnnotationInstance();
        instance.setAnnotationId(ANNOTATION_ID);
        instance.setImageId(IMAGE_ID);
        instance.setLabelId(labelId);
        return instance;
    }
}
