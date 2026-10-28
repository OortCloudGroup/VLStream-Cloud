package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.detection.DetectionSessionSupport;
import com.ruoyi.vlstream.test.vlstream.mapper.EventReportOutboxMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsEventManagementMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventReportOutbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class VlsDeviceEventReportServiceTest {
    final IVlsEventManagementService store = mock(IVlsEventManagementService.class);
    final EventReportOutboxMapper outbox = mock(EventReportOutboxMapper.class);
    final IFileUploadService files = mock(IFileUploadService.class);
    final WvpVlStreamDeviceResolver devices = mock(WvpVlStreamDeviceResolver.class);
    final VlsEventReportApplicationService application = new VlsEventReportApplicationService(store,
        mock(VlsEventManagementMapper.class),outbox,files);
    final VlsDeviceEventReportService reports = new VlsDeviceEventReportService(application,devices);

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(application,"tenantType","multi");
        AtomicLong ids = new AtomicLong(100);
        when(store.createEvent(any())).thenAnswer(call -> {EventManagement e=call.getArgument(0);e.setId(ids.incrementAndGet());return true;});
        when(outbox.insert(any(EventReportOutbox.class))).thenReturn(1);
        when(devices.resolveEventDevice("CAM-1")).thenReturn(device("CAM-1","tenant-a"));
        when(devices.resolveEventDevice("CAM-2")).thenReturn(device("CAM-2","tenant-b"));
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}

    @Test void allEightDetectionCategoriesEnterOutboxUsingWvpOwnerAndRestoreContext() {
        when(store.reportDeviceEvent(any(),any())).thenAnswer(call -> reports.reportDetection(call.getArgument(0),call.getArgument(1),store));
        DeviceInfo snapshot=device("CAM-1","stale-legacy-owner");
        TenantContextHolder.setTenantId("ui-tenant");
        for(String type:Arrays.asList("face","classify","detect","person","pose","semseg","instance","obb")){
            Algorithm algorithm=new Algorithm();algorithm.setName(type);
            DetectionSessionSupport.createEvent(store,snapshot,algorithm,"description",Collections.singletonMap("kind",type),"https://media.invalid/image.jpg");
            assertEquals("ui-tenant",TenantContextHolder.getTenantId());
        }
        ArgumentCaptor<EventReportOutbox> tasks=ArgumentCaptor.forClass(EventReportOutbox.class);
        verify(outbox,times(8)).insert(tasks.capture());
        for(EventReportOutbox task:tasks.getAllValues()){
            assertEquals("tenant-a",task.getTenantId());
            assertEquals("tenant-a",JSONUtil.parseObj(task.getPayloadJson()).getStr("device_tenant_id"));
        }
    }

    @Test void singleDetectionPreservesOriginalCreateWithoutWvpOrOutbox() {
        ReflectionTestUtils.setField(application,"tenantType","single");
        assertTrue(reports.reportDetection(new EventManagement(),device("CAM-1","000000"),store));
        verify(store).createEvent(any());verifyNoInteractions(devices,outbox);
    }

    @Test void springWiresTheLazyDeviceBoundaryWithoutAnApplicationServiceCycle() {
        org.springframework.context.annotation.AnnotationConfigApplicationContext context=new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,"token.tenant-type=multi");
        VlsEventManagementMapper mapper=mock(VlsEventManagementMapper.class);
        when(mapper.insert(any(EventManagement.class))).thenAnswer(call->{((EventManagement)call.getArgument(0)).setId(301L);return 1;});
        context.getBeanFactory().registerSingleton("vlsEventManagementMapper",mapper);
        context.getBeanFactory().registerSingleton("eventReportOutboxMapper",outbox);
        context.getBeanFactory().registerSingleton("fileUploadService",files);
        context.getBeanFactory().registerSingleton("wvpDeviceResolver",devices);
        context.register(VlsEventReportApplicationService.class,VlsDeviceEventReportService.class,
            com.ruoyi.vlstream.test.vlstream.service.impl.VlsEventManagementServiceImpl.class);
        try{
            context.refresh();EventManagement event=new EventManagement();event.setReportDevice("CAM-1");event.setEventType("detect");
            assertTrue(context.getBean(IVlsEventManagementService.class).reportDeviceEvent(event,device("CAM-1","stale")));
            assertEquals("tenant-a",event.getTenantId());verify(outbox).insert(any(EventReportOutbox.class));
        }finally{context.close();}
    }

    @Test void unknownOwnershipAndDeviceMismatchNeverPersist() {
        when(devices.resolveEventDevice("CAM-1")).thenReturn(device("CAM-1",null));
        EventManagement event=new EventManagement();event.setReportDevice("CAM-1");
        assertThrows(ServiceException.class,()->reports.reportDetection(event,device("CAM-1","legacy"),store));
        assertThrows(ServiceException.class,()->reports.reportDetection(event,device("OTHER","legacy"),store));
        verifyNoInteractions(store,outbox);
    }

    @Test void publicCameraBatchUsesEachAuthoritativeTenantAndPreservesMediaAndType() {
        TenantContextHolder.setTenantId("ui-tenant");
        List<EventManagement> result=reports.reportCameras(Arrays.asList(camera("CAM-1"),camera("CAM-2")));
        assertEquals(2,result.size());assertEquals("ui-tenant",TenantContextHolder.getTenantId());
        ArgumentCaptor<EventReportOutbox> tasks=ArgumentCaptor.forClass(EventReportOutbox.class);
        verify(outbox,times(2)).insert(tasks.capture());
        assertEquals("tenant-a",tasks.getAllValues().get(0).getTenantId());
        assertEquals("tenant-b",tasks.getAllValues().get(1).getTenantId());
        JSONObject payload=JSONUtil.parseObj(tasks.getAllValues().get(0).getPayloadJson());
        assertEquals("IEV1805",payload.getStr("item"));assertEquals("alarm",payload.getStr("name"));
        assertEquals("https://media.invalid/clip.mp4",payload.getJSONArray("video").getStr(0));
        assertNull(result.get(0).getMqttMessageId());assertNull(result.get(0).getMediaId());
    }

    @Test void validatesWholeBatchAndRejectsForgedTenantOrPrivateReferencesBeforeWrites() {
        Map<String,Object> invalid=camera("CAM-2");invalid.remove("point");
        assertThrows(IllegalArgumentException.class,()->reports.reportCameras(Arrays.asList(camera("CAM-1"),invalid)));
        verifyNoInteractions(devices,store,outbox);
        Map<String,Object> forged=camera("CAM-1");forged.put("device_tenant_id","ui-tenant");
        assertThrows(IllegalArgumentException.class,()->reports.reportCameras(Collections.singletonList(forged)));
        Map<String,Object> privateMedia=camera("CAM-1");privateMedia.put("pics",Collections.singletonList("vls-media://unverified"));
        assertThrows(IllegalArgumentException.class,()->reports.reportCameras(Collections.singletonList(privateMedia)));
        verifyNoInteractions(store,outbox);
    }

    @Test void multipartMultiRequiresCurrentTenantBeforeUploadAndReportsVideoAsVideo() {
        EventManagement event=new EventManagement();event.setReportDevice("CAM-1");event.setEventType("IEV1805");
        MockMultipartFile video=new MockMultipartFile("file","clip.mp4","video/mp4",new byte[]{1});
        TenantContextHolder.setTenantId("tenant-b");
        assertThrows(ServiceException.class,()->reports.reportMultipart(event,video));verifyNoInteractions(files);
        TenantContextHolder.setTenantId("tenant-a");
        when(files.multipartFileToFile(any())).thenReturn(new java.io.File("synthetic-only.mp4"));
        FileResponseDto uploaded=new FileResponseDto();uploaded.setUrl("https://media.invalid/clip.mp4");
        when(files.uploadFile(anyString(),anyString(),any())).thenReturn(uploaded);
        assertTrue(reports.reportMultipart(event,video));
        ArgumentCaptor<EventReportOutbox> task=ArgumentCaptor.forClass(EventReportOutbox.class);verify(outbox).insert(task.capture());
        JSONObject payload=JSONUtil.parseObj(task.getValue().getPayloadJson());
        assertEquals(0,payload.getJSONArray("pics").size());assertEquals(1,payload.getJSONArray("video").size());
    }

    private DeviceInfo device(String id,String tenant){DeviceInfo d=new DeviceInfo();d.setDeviceId(id);d.setDeviceName("camera");d.setTenantId(tenant);return d;}
    private Map<String,Object> camera(String id){Map<String,Object> e=new LinkedHashMap<>();e.put("device_id",id);e.put("device_name","client camera");e.put("item","IEV1805");e.put("name","alarm");e.put("describe","a full description longer than ten characters");e.put("point",Collections.singletonMap("address","capture location"));e.put("pics",Collections.singletonList("https://media.invalid/image.jpg"));e.put("video",Collections.singletonList("https://media.invalid/clip.mp4"));return e;}
}
