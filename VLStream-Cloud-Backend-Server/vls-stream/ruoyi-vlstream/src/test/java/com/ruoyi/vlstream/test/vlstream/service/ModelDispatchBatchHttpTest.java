package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.controller.VlsModelDispatchController;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real HTTP controller -> batch -> deployment -> real task mapper -> simulated device reply. */
@Tag("dev")
class ModelDispatchBatchHttpTest {
    ModelDispatchPersistenceTest store;
    ModelDispatchBatchService batches;
    VlsMqttBusService bus;
    MockMvc http;
    ObjectMapper json = new ObjectMapper();
    LoginUser actor;
    @BeforeEach void setup() throws Exception {
        store = new ModelDispatchPersistenceTest(); store.setup();
        store.jdbc.update("DELETE FROM vls_model_dispatch_task");
        String migration = new String(Files.readAllBytes(Paths.get("../ruoyi-admin/src/main/resources/db/migration/V1_2_0_055__model_dispatch_batches.sql")), StandardCharsets.UTF_8)
            .replace("CHARACTER SET ascii COLLATE ascii_bin", "").replaceAll("(?s)\\) ENGINE=.*", ")");
        store.jdbc.execute(migration);
        TenantContextHolder.setTenantId("tenant-a"); actor = new LoginUser(); actor.setTenantId("tenant-a"); actor.setUserId("alice");
        WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
        when(resolver.resolveOnline(anyString())).thenAnswer(call -> {
            DeviceInfo device = new DeviceInfo(); device.setId(101L); device.setDeviceId(call.getArgument(0)); device.setTenantId("tenant-a"); return device;
        });
        ModelDispatchAccess access = new ModelDispatchAccess(resolver, new WvpVlStreamDeviceProperties()) {
            @Override protected LoginUser actor() { return actor; }
            @Override protected JSONObject authorizedDetail(Long row) {
                return new JSONObject().set("device", new JSONObject().set("deviceId", requestedDevice.get())
                    .set("online", !"CAM-B".equals(requestedDevice.get())).set("deviceModel", "Hi3519DV500").set("capabilitiesJson", "[\"aiInfer\"]"));
            }
            @Override public DeviceInfo requireDevice(String deviceId, String type, boolean strict) {
                requestedDevice.set(deviceId); return super.requireDevice(deviceId, type, strict);
            }
            final ThreadLocal<String> requestedDevice = new ThreadLocal<>();
        };
        ModelDispatchLocks locks = mock(ModelDispatchLocks.class);
        when(locks.run(any(), any())).thenAnswer(call -> ((java.util.function.Supplier<?>)call.getArgument(1)).get());
        ModelDispatchService dispatch = new ModelDispatchService();
        IVlsAlgorithmTrainingService trainingService = mock(IVlsAlgorithmTrainingService.class);
        AlgorithmTraining training = new AlgorithmTraining(); training.setId(70L); training.setAlgorithmId(7L); training.setTenantId("tenant-a"); training.setConfigParams("{\"trainType\":\"detect\"}");
        when(trainingService.list(any())).thenReturn(Collections.singletonList(training));
        RemoteModelArtifactService artifacts = mock(RemoteModelArtifactService.class);
        when(artifacts.normalizeType(any())).thenReturn("om"); when(artifacts.resolvePath(any(), any())).thenReturn("fixture.om");
        when(artifacts.inspect("fixture.om")).thenReturn(new RemoteModelArtifactService.ArtifactMetadata("fixture.om", 10L, store.task.getSha256()));
        ModelClassFileService classes = mock(ModelClassFileService.class);
        when(classes.prepare(any())).thenReturn(new ModelClassFileService.ClassFile("data.yaml", "names: [person]\n", 16L, store.task.getClassFileSha256()));
        VlsModelDispatchProperties config = new VlsModelDispatchProperties(); config.setPublicBaseUrl("http://fixture.invalid"); config.setSigningSecret("fixture-only");
        ModelDownloadSignatureService signatures = mock(ModelDownloadSignatureService.class); when(signatures.sign(any(), anyLong())).thenReturn("fixture-signature");
        bus = mock(VlsMqttBusService.class);
        set(dispatch,"trainingService",trainingService);set(dispatch,"artifactService",artifacts);set(dispatch,"classFileService",classes);
        set(dispatch,"dispatchProperties",config);set(dispatch,"signatureService",signatures);set(dispatch,"mqttService",bus);
        set(dispatch,"taskService",store.service);set(dispatch,"dispatchAccess",access);set(dispatch,"dispatchLocks",locks);
        batches = new ModelDispatchBatchService(store.jdbc,json,dispatch,store.service,access,locks);
        VlsModelDispatchController controller = new VlsModelDispatchController(); set(controller,"batches",batches);
        http = MockMvcBuilders.standaloneSetup(controller).build();
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); if(store!=null)store.cleanup(); }
    void set(Object bean,String name,Object value)throws Exception {Field field=bean.getClass().getDeclaredField(name);field.setAccessible(true);field.set(bean,value);}
    String body(String operation) { return "{\"operationId\":\"" + operation + "\",\"algorithmId\":7,\"modelType\":\"om\",\"deviceIds\":[\"CAM-A\",\"CAM-B\"]}"; }
    JsonNode submit(String operation) throws Exception {
        return json.readTree(http.perform(post("/vlsModelDispatch/batches").contentType("application/json").content(body(operation)))
            .andReturn().getResponse().getContentAsString()).path("data");
    }
    @Test void httpPartialFailureRecoveryAndHardwareConfirmationUseTheSameDurableTask() throws Exception {
        String operation=UUID.randomUUID().toString(); JsonNode first=submit(operation);
        assertEquals(1,first.path("pending").asInt());assertEquals(1,first.path("failed").asInt());assertEquals(0,first.path("succeeded").asInt());
        String request=first.path("items").get(0).path("requestId").asText(); assertFalse(request.isEmpty());
        assertEquals(request,submit(operation).path("items").get(0).path("requestId").asText());
        verify(bus,times(1)).publish(eq("vlstream/v2.2/dev/CAM-A/bus"),any());
        store.session.clearCache(); com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask task=store.service.getByRequestId(request);
        assertTrue(store.service.applyHardwareReply(task.getMqttMessageId(), request,"CAM-A","SUCCESS", task.getSha256(),task.getClassFileSha256(),"fixture device completed","{}"));
        store.session.clearCache(); JsonNode restored=json.readTree(http.perform(get("/vlsModelDispatch/batches/"+operation)).andReturn().getResponse().getContentAsString()).path("data");
        assertEquals(1,restored.path("succeeded").asInt());assertEquals(1,restored.path("failed").asInt());
        String retry="{\"retryId\":\""+UUID.randomUUID()+"\",\"deviceIds\":[\"CAM-B\"]}";
        http.perform(post("/vlsModelDispatch/batches/"+operation+"/retry").contentType("application/json").content(retry));
        http.perform(post("/vlsModelDispatch/batches/"+operation+"/retry").contentType("application/json").content(retry));
        store.session.clearCache(); assertEquals(2,batches.get(operation).getItems().get(1).getAttempt());
        assertEquals(1,store.jdbc.queryForObject("SELECT COUNT(*) FROM vls_model_dispatch_task",Integer.class));
        verify(bus,times(1)).publish(any(),any());
    }
    @Test void anotherBatchCannotPublishToBusyDeviceAndOwnerChangeCannotRecover() throws Exception {
        String operation=UUID.randomUUID().toString();submit(operation);store.session.clearCache();
        JsonNode second=submit(UUID.randomUUID().toString());assertEquals(2,second.path("failed").asInt());
        verify(bus,times(1)).publish(any(),any());
        actor.setUserId("bob");assertThrows(com.ruoyi.common.exception.ServiceException.class,()->batches.get(operation));
        assertTrue(batches.recent().isEmpty());
    }
}
