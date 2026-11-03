package com.ruoyi.vlstream.test.vlstream;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.vlstream.test.vlstream.controller.VlsVideoRecordController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDeviceMediaController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsModelHubController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsTagManagementController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsTimeStrategyController;
import com.ruoyi.vlstream.test.vlstream.service.IVlsTagManagementService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsTimeStrategyService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import com.ruoyi.vlstream.test.vlstream.service.DeviceMediaUploadService;
import com.ruoyi.vlstream.test.vlstream.service.ModelHubCatalogService;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.test.vlstream.service.IVlsVideoRecordService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class VideoRecordPermissionTest {
    private SaTokenContext oldContext;
    private SaTokenDao oldDao;
    private StpInterface oldPermissions;
    private SaTokenDaoDefaultImpl memory;
    private Set<String> permissions;
    private IVlsVideoRecordService records;
    private IVlsTagManagementService tags;
    private IVlsTimeStrategyService strategies;
    private IVlsDeviceInfoService devices;
    private VlsMqttPublishService mqtt;
    private DeviceMediaUploadService media;
    private ModelHubCatalogService modelHub;
    private MockMvc mvc;
    private String token;

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        oldContext = SaManager.getSaTokenContext();
        oldDao = SaManager.getSaTokenDao();
        oldPermissions = SaManager.getStpInterface();
        memory = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(memory);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        permissions = new HashSet<>();
        SaManager.setStpInterface(new StpInterface() {
            public List<String> getPermissionList(Object id, String type) { return new ArrayList<>(permissions); }
            public List<String> getRoleList(Object id, String type) { return Collections.emptyList(); }
        });
        context(new MockHttpServletRequest(), new MockHttpServletResponse());
        StpUtil.login("record-fixture");
        token = StpUtil.getTokenValue();
        SaTokenContextForThreadLocalStorage.clearBox();
        records = mock(IVlsVideoRecordService.class);
        tags = mock(IVlsTagManagementService.class);
        strategies = mock(IVlsTimeStrategyService.class);
        devices = mock(IVlsDeviceInfoService.class);
        mqtt = mock(VlsMqttPublishService.class);
        media = mock(DeviceMediaUploadService.class);
        modelHub = mock(ModelHubCatalogService.class);
        when(tags.getTagsByCategory(anyString())).thenReturn(Collections.emptyList());
        when(strategies.saveOrUpdateStrategy(any())).thenReturn(true);
        when(mqtt.publish(anyString(), any())).thenReturn(true);
        when(records.save(any())).thenReturn(true);
        VlsDeviceMediaController mediaController = new VlsDeviceMediaController();
        ReflectionTestUtils.setField(mediaController, "uploadService", media);
        SaInterceptor guard = new SaInterceptor();
        mvc = MockMvcBuilders.standaloneSetup(new VlsVideoRecordController(records), mediaController, new VlsModelHubController(modelHub), new VlsTagManagementController(tags),
            new VlsTimeStrategyController(strategies, devices, mqtt, new VlsMqttProperties(), mock(ObjectProvider.class))).setControllerAdvice(new Errors())
            .addInterceptors(new HandlerInterceptor() {
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                    context(request, response);
                    return guard.preHandle(request, response, handler);
                }
                public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception error) {
                    SaTokenContextForThreadLocalStorage.clearBox();
                }
            }).build();
    }

    private static void context(HttpServletRequest request, HttpServletResponse response) {
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request), new SaResponseForServlet(response), new SaStorageForServlet(request));
    }

    @AfterEach void cleanup() {
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setStpInterface(oldPermissions);
        SaManager.setSaTokenDao(oldDao);
        SaManager.setSaTokenContext(oldContext);
        memory.endRefreshThread();
    }

    @Test void permissionControlsReadingAndWritingAndRevocationIsImmediate() throws Exception {
        mvc.perform(get("/vlsVideoRecord/stream/1").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        mvc.perform(post("/vlsVideoRecord/save").header(SaManager.getConfig().getTokenName(), token)
            .contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden());
        verifyNoInteractions(records);

        permissions.add("vls:menu:video-playback");
        mvc.perform(get("/vlsVideoRecord/stream/1").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isNotFound());
        mvc.perform(post("/vlsVideoRecord/save").header(SaManager.getConfig().getTokenName(), token)
            .contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isOk());
        verify(records).getById(1L);
        verify(records).save(any());

        permissions.clear();
        mvc.perform(get("/vlsVideoRecord/stream/1").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        verifyNoMoreInteractions(records);
    }

    @Test void legacyDevicePageControlsTagTreeAndTimeStrategyWithoutProtocolPageExpansion() throws Exception {
        permissions.add("vls:menu:vlstream:device");
        mvc.perform(get("/vlsTagManagement/tree").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        mvc.perform(post("/vlsTimeStrategy").header(SaManager.getConfig().getTokenName(), token)
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(tags, strategies, mqtt);
        permissions.add("vls:menu:legacy-device-management");
        when(devices.getById(1L)).thenReturn(new DeviceInfo());
        mvc.perform(get("/vlsTagManagement/tree").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isOk());
        mvc.perform(post("/vlsTimeStrategy").header(SaManager.getConfig().getTokenName(), token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"1\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(tags).getTagsByCategory("own");
        verify(tags).getTagsByCategory("public");
        verify(strategies).saveOrUpdateStrategy(any());
        verify(mqtt).publish(anyString(), any());
        permissions.remove("vls:menu:legacy-device-management");
        mvc.perform(get("/vlsTagManagement/tree").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
    }

    @Test void missingOrCrossTenantDeviceCannotWriteStrategyOrPublishMqtt() throws Exception {
        permissions.add("vls:menu:legacy-device-management");
        for (String id : Arrays.asList("2", "invalid", "0")) {
            mvc.perform(post("/vlsTimeStrategy").header(SaManager.getConfig().getTokenName(), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"deviceId\":\"" + id + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        }
        verify(devices).getById(2L);
        verifyNoInteractions(strategies, mqtt);
    }

    @Test void oldStrategyAliasesCannotBypassPermissionOrDeviceOwnership() throws Exception {
        String body = "{\"id\":99,\"deviceId\":\"2\"}";
        for (String path : Arrays.asList("/vlsTimeStrategy/save", "/vlsTimeStrategy/update", "/vlsTimeStrategy/submit")) {
            mvc.perform(post(path).header(SaManager.getConfig().getTokenName(), token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/vlsTimeStrategy/remove").param("ids", "99").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        mvc.perform(delete("/vlsTimeStrategy/2").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        verifyNoInteractions(strategies, devices, mqtt);

        permissions.add("vls:menu:legacy-device-management");
        for (String path : Arrays.asList("/vlsTimeStrategy/save", "/vlsTimeStrategy/update", "/vlsTimeStrategy/submit")) {
            mvc.perform(post(path).header(SaManager.getConfig().getTokenName(), token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(get("/vlsTimeStrategy/remove").param("ids", "99").header(SaManager.getConfig().getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(delete("/vlsTimeStrategy/2").header(SaManager.getConfig().getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(false));
        verify(strategies, never()).save(any());
        verify(strategies, never()).updateById(any());
        verify(strategies, never()).saveOrUpdate(any());
        verify(strategies, never()).saveOrUpdateStrategy(any());
        verify(strategies, never()).deleteLogic(anyList());
        verify(strategies, never()).deleteByDeviceId(anyString());
        verifyNoInteractions(mqtt);
    }

    @Test void oldStrategyAliasesAcceptAnOwnedDeviceAndOwnedStrategy() throws Exception {
        permissions.add("vls:menu:legacy-device-management");
        when(devices.getById(1L)).thenReturn(new DeviceInfo());
        TimeStrategy existing = new TimeStrategy();
        existing.setDeviceId("1");
        when(strategies.getById(99L)).thenReturn(existing);
        when(strategies.save(any())).thenReturn(true);
        when(strategies.updateById(any())).thenReturn(true);
        when(strategies.saveOrUpdate(any())).thenReturn(true);
        when(strategies.deleteLogic(anyList())).thenReturn(true);
        when(strategies.deleteByDeviceId("1")).thenReturn(true);
        String body = "{\"id\":99,\"deviceId\":\"1\"}";
        for (String path : Arrays.asList("/vlsTimeStrategy/save", "/vlsTimeStrategy/update", "/vlsTimeStrategy/submit")) {
            mvc.perform(post(path).header(SaManager.getConfig().getTokenName(), token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }
        mvc.perform(get("/vlsTimeStrategy/remove").param("ids", "99").header(SaManager.getConfig().getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(delete("/vlsTimeStrategy/1").header(SaManager.getConfig().getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(mqtt, times(2)).publish(anyString(), any());
    }

    @Test void eventMediaReadsRequireTheActualCallingPageWithoutChangingPublicUpload() throws Exception {
        when(media.getPrivateViewUrl("fixture", 300)).thenReturn("fixture-view");
        when(media.getPrivateViewUrl("fixture", 300, true)).thenReturn("fixture-download");
        mvc.perform(get("/vlsDeviceMedia/fixture/view-url").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        mvc.perform(get("/vlsDeviceMedia/fixture/download-url").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        verifyNoInteractions(media);
        permissions.add("vls:menu:llm-review-tasks");
        mvc.perform(get("/vlsDeviceMedia/fixture/view-url").header(SaManager.getConfig().getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("fixture-view"));
        mvc.perform(get("/vlsDeviceMedia/fixture/download-url").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        permissions.clear();
        permissions.add("active-safety:events:list");
        mvc.perform(get("/vlsDeviceMedia/fixture/download-url").header(SaManager.getConfig().getTokenName(), token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("fixture-download"));
        mvc.perform(get("/vlsDeviceMedia/fixture/view-url").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        permissions.clear();
        mvc.perform(get("/vlsDeviceMedia/fixture/download-url").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertNotNull(VlsDeviceMediaController.class.getMethod("issueUploadUrl",
            com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceMediaUploadRequest.class).getAnnotation(cn.dev33.satoken.annotation.SaIgnore.class));
        mvc.perform(post("/vlsDeviceMedia/public/upload-url").contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"fixture\",\"fileName\":\"event.jpg\",\"contentType\":\"image/jpeg\",\"fileSize\":1,\"sha256\":\"fixture\"}"))
            .andExpect(status().isOk());
        verify(media).getPrivateViewUrl("fixture", 300);
        verify(media).getPrivateViewUrl("fixture", 300, true);
        verify(media).issueUploadUrl(any());
        verifyNoMoreInteractions(media);
    }

    @Test void modelHubIsSharedOnlyWithItsTwoCallingPagesAndRevocationTakesEffect() throws Exception {
        mvc.perform(get("/vlsModelHub/models").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        mvc.perform(post("/vlsModelHub/download").header(SaManager.getConfig().getTokenName(), token)
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(modelHub);
        for (String permission : Arrays.asList("vls:menu:algorithm-arrangement", "vls:menu:vlstream:device")) {
            permissions.clear();
            permissions.add(permission);
            mvc.perform(get("/vlsModelHub/models").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isOk());
            mvc.perform(post("/vlsModelHub/categories").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isOk());
            mvc.perform(post("/vlsModelHub/files").header(SaManager.getConfig().getTokenName(), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"uid\":\"fixture\"}")).andExpect(status().isOk());
            mvc.perform(post("/vlsModelHub/download").header(SaManager.getConfig().getTokenName(), token)
                .header("X-Model-Hub-Token", "fixture-token").header("X-Model-Hub-Tenant", "fixture-tenant")
                .contentType(MediaType.APPLICATION_JSON).content("{\"uid\":\"fixture\"}")).andExpect(status().isOk());
        }
        verify(modelHub, times(2)).list(1, 12, "", "");
        verify(modelHub, times(2)).categories();
        verify(modelHub, times(2)).files("fixture", null, null);
        verify(modelHub, times(2)).download(eq("fixture"), isNull(), isNull(), eq("fixture-token"), eq("fixture-tenant"), any());
        permissions.clear();
        mvc.perform(get("/vlsModelHub/models").header(SaManager.getConfig().getTokenName(), token)).andExpect(status().isForbidden());
        verifyNoMoreInteractions(modelHub);
    }

    @RestControllerAdvice static class Errors {
        @ExceptionHandler(NotPermissionException.class)
        ResponseEntity<String> denied() { return ResponseEntity.status(403).body("denied"); }
    }
}
