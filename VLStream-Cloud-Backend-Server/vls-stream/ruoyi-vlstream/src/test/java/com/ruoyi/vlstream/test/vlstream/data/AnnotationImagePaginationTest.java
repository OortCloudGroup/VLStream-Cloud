package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.DataSampleMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.DatasetVersionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationLabelMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class AnnotationImagePaginationTest {
    private final VlsAlgorithmAnnotationMapper projects = mock(VlsAlgorithmAnnotationMapper.class);
    private final DataSampleMapper samples = mock(DataSampleMapper.class);
    private final VlsAnnotationLabelMapper labels = mock(VlsAnnotationLabelMapper.class);
    private final VlsAnnotationInstanceMapper instances = mock(VlsAnnotationInstanceMapper.class);
    private final DataManagementService data = new DataManagementService(projects, samples, labels, instances,
        mock(DatasetVersionMapper.class), new ObjectMapper(), mock(DataMediaStorage.class));

    @BeforeEach void prepare() {
        TenantContextHolder.setTenantId("tenant-a");
        when(projects.selectOne(any())).thenReturn(new AlgorithmAnnotation());
        when(labels.selectList(any())).thenReturn(Collections.emptyList());
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void loadsInstancesOnlyForCurrentPageAndPreservesTotalAndLongIds() {
        AnnotationImage image = new AnnotationImage();
        image.setId(2118653742822842369L);
        image.setImageName("same-name.jpg");
        AnnotationInstance instance = new AnnotationInstance();
        instance.setImageId(image.getId());
        instance.setAnnotationData("{}");
        when(samples.selectPage(any(), any())).thenAnswer(call -> {
            IPage<AnnotationImage> page = call.getArgument(0);
            assertEquals(2, page.getCurrent());
            assertEquals(10, page.getSize());
            return page.setTotal(330).setRecords(Collections.singletonList(image));
        });
        when(instances.selectList(any())).thenReturn(Collections.singletonList(instance));
        DataRequests.SampleQuery query = new DataRequests.SampleQuery();
        query.setPage(2); query.setSize(10);
        IPage<Map<String, Object>> page = data.annotationImages(10L, query, null);
        assertEquals(330, page.getTotal());
        assertEquals("2118653742822842369", page.getRecords().get(0).get("id"));
        assertEquals(Collections.singletonList(instance), page.getRecords().get(0).get("instances"));
        ArgumentCaptor<QueryWrapper> capture = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(instances).selectList(capture.capture());
        QueryWrapper<?> condition = capture.getValue();
        assertTrue(condition.getSqlSegment().contains("image_id IN"));
        assertTrue(condition.getParamNameValuePairs().values().containsAll(Arrays.asList("tenant-a", 10L, image.getId())));
    }

    @Test void emptyPageNeverQueriesAllProjectInstances() {
        emptyPage();
        data.annotationImages(10L, new DataRequests.SampleQuery(), null);
        verify(instances, never()).selectList(any());
    }

    @Test void filtersStatusAndLabelBeforePaginationWithTrustedTenant() {
        emptyPage();
        DataRequests.SampleQuery query = new DataRequests.SampleQuery();
        query.setLabelId(12L); query.setAnnotationStatus("unannotated");
        data.annotationImages(10L, query, null);
        ArgumentCaptor<QueryWrapper> capture = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(samples).selectPage(any(), capture.capture());
        QueryWrapper<?> condition = capture.getValue();
        String sql = condition.getSqlSegment();
        assertTrue(sql.contains("li.label_id ="));
        assertTrue(sql.contains("NOT EXISTS"));
        assertTrue(sql.contains("ORDER BY id DESC"));
        assertTrue(condition.getParamNameValuePairs().values().containsAll(Arrays.asList("tenant-a", "image", "excluded", 12L)));
    }

    @Test void locatesGenerationRepairImageWithoutLoadingEarlierPages() {
        emptyPage();
        when(samples.selectOne(any())).thenReturn(new AnnotationImage());
        when(samples.selectCount(any())).thenReturn(25L);
        DataRequests.SampleQuery query = new DataRequests.SampleQuery(); query.setSize(10);
        data.annotationImages(10L, query, 50L);
        assertEquals(3, query.getPage());
        verify(samples, times(1)).selectPage(any(), any());
    }

    @Test void rejectsUnownedProjectBeforeReadingImages() {
        when(projects.selectOne(any())).thenReturn(null);
        assertThrows(ServiceException.class, () -> data.annotationImages(10L, new DataRequests.SampleQuery(), null));
        verify(samples, never()).selectPage(any(), any());
        verifyNoInteractions(instances);
    }

    @Test void validatesPageBoundsAndSupportedPageSizes() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DataRequests.SampleQuery query = new DataRequests.SampleQuery();
            for (int size : Arrays.asList(10, 20, 50, 100)) {
                query.setSize(size);
                assertTrue(factory.getValidator().validate(query).isEmpty());
            }
            query.setSize(101);
            assertFalse(factory.getValidator().validate(query).isEmpty());
            query.setSize(10); query.setPage(0);
            assertFalse(factory.getValidator().validate(query).isEmpty());
        }
    }

    private void emptyPage() {
        when(samples.selectPage(any(), any())).thenAnswer(call -> {
            IPage<AnnotationImage> page = call.getArgument(0);
            return page.setTotal(0).setRecords(Collections.emptyList());
        });
    }
}
