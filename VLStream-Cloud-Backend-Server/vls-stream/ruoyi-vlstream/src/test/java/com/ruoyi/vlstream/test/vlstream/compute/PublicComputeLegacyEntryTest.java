package com.ruoyi.vlstream.test.vlstream.compute;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmModelServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class PublicComputeLegacyEntryTest {
    private ComputePublicTaskAccess access() {
        ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class);when(inventory.viewer()).thenReturn("verified-subject");
        return new ComputePublicTaskAccess(mock(JdbcTemplate.class),inventory);
    }
    private static void filtered(Object raw) {
        QueryWrapper<?> query=(QueryWrapper<?>)raw;
        assertTrue(query.getSqlSegment().contains("NOT EXISTS"));assertTrue(query.getSqlSegment().contains("public_owner_subject"));assertTrue(query.getParamNameValuePairs().containsValue("verified-subject"));
    }
    @Test void versionLatestAndPopularEntryPointsFilterBeforeReturningPrivatePaths() {
        VlsAlgorithmModelMapper mapper=mock(VlsAlgorithmModelMapper.class);VlsAlgorithmModelServiceImpl service=new VlsAlgorithmModelServiceImpl();
        ReflectionTestUtils.setField(service,"baseMapper",mapper);ReflectionTestUtils.setField(service,"publicAccess",access());
        AlgorithmModel other=new AlgorithmModel();other.setModelPath("other-users-private-path");
        when(mapper.selectByAlgorithmIdAndVersion(anyLong(),anyInt())).thenReturn(other);when(mapper.selectLatestByAlgorithmId(anyLong())).thenReturn(other);when(mapper.selectPopularModels(anyInt())).thenReturn(Collections.singletonList(other));
        when(mapper.selectOne(any())).thenAnswer(call->{filtered(call.getArgument(0));return null;});
        when(mapper.selectList(any())).thenAnswer(call->{filtered(call.getArgument(0));return Collections.emptyList();});
        assertNull(service.getModelByAlgorithmIdAndVersion(1L,1));assertNull(service.getLatestModelByAlgorithmId(1L));assertTrue(service.getPopularModels(10).isEmpty());
        verify(mapper,never()).selectByAlgorithmIdAndVersion(anyLong(),anyInt());verify(mapper,never()).selectLatestByAlgorithmId(anyLong());verify(mapper,never()).selectPopularModels(anyInt());
    }
    @Test void wholePublishBatchIsAuthorizedBeforeAnyReadOrWriteCanPartiallyPublish() {
        VlsAlgorithmModelMapper mapper=mock(VlsAlgorithmModelMapper.class);VlsAlgorithmModelServiceImpl service=new VlsAlgorithmModelServiceImpl();ComputePublicTaskAccess guard=mock(ComputePublicTaskAccess.class);
        ReflectionTestUtils.setField(service,"baseMapper",mapper);ReflectionTestUtils.setField(service,"publicAccess",guard);
        doThrow(new ServiceException("other owner")).when(guard).modelFromHttp(20L);
        assertThrows(ServiceException.class,()->service.batchPublishModel(Arrays.asList(10L,20L)));verify(guard).modelFromHttp(10L);verify(guard).modelFromHttp(20L);verifyNoInteractions(mapper);
    }
    @Test void containerDetailListAndExportAlwaysIncludePublicUserPredicate() {
        IVlsContainerInstanceService records=mock(IVlsContainerInstanceService.class);
        com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController controller=new com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController(records,mock(GpuTrainingSchedulerService.class));ReflectionTestUtils.setField(controller,"publicAccess",access());
        when(records.getOne(any())).thenAnswer(call->{filtered(call.getArgument(0));throw new ServiceException("query captured");});
        when(records.page(any(),any())).thenAnswer(call->{filtered(call.getArgument(1));throw new ServiceException("query captured");});
        when(records.exportVlsContainerInstance(any())).thenAnswer(call->{filtered(call.getArgument(0));throw new ServiceException("query captured");});
        assertThrows(ServiceException.class,()->controller.detail(new ContainerInstance()));
        assertThrows(ServiceException.class,()->controller.list(Collections.emptyMap(),new org.springblade.core.mp.support.Query()));
        assertThrows(ServiceException.class,()->controller.exportVlsContainerInstance(Collections.emptyMap(),null,new org.springframework.mock.web.MockHttpServletResponse()));
        verify(records).getOne(any());verify(records).page(any(),any());verify(records).exportVlsContainerInstance(any());
    }
}
