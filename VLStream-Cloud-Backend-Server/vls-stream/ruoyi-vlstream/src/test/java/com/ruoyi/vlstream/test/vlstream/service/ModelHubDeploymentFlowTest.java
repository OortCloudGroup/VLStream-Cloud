package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsModelDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** No real HTTP, Redis, object storage, WVP or MQTT clients are used by this flow test. */
@Tag("dev")
class ModelHubDeploymentFlowTest {
    final String commit=String.join("",Collections.nCopies(40,"a"));
    final String operation="71873e01-d291-4e86-b32d-f9f2b0861950";
    final byte[] model=new byte[]{1,2,3,4};
    final byte[] classes="names: [person]\nnc: 1\nannotation_type: object_detection\n".getBytes(StandardCharsets.UTF_8);
    final RestTemplate rest=new RestTemplate();
    final MockRestServiceServer http=MockRestServiceServer.createServer(rest);
    final ModelHubDeploymentClient client=new ModelHubDeploymentClient(ModelHubCatalogServiceTest.config(),rest);
    final ModelArtifactObjectStore storage=mock(ModelArtifactObjectStore.class);
    final ModelDispatchAccess access=mock(ModelDispatchAccess.class);
    final VlsAlgorithmMapper algorithms=mock(VlsAlgorithmMapper.class);
    final ModelDispatchTaskService tasks=mock(ModelDispatchTaskService.class);
    final VlsMqttBusService mqtt=mock(VlsMqttBusService.class);
    final RemoteModelArtifactService artifacts=mock(RemoteModelArtifactService.class);
    final ModelDispatchService dispatch=new ModelDispatchService();
    final Map<String,ModelDispatchTask> rows=new HashMap<>();
    final ModelHubDeploymentService flow=new ModelHubDeploymentService(client,storage,access,algorithms,dispatch,tasks);
    String modelHash,classesHash;
    @BeforeEach void setup() throws Exception {
        modelHash=ModelArtifactObjectStore.digest(new ByteArrayInputStream(model));
        classesHash=ModelArtifactObjectStore.digest(new ByteArrayInputStream(classes));
        when(access.tenant()).thenReturn("local-tenant");when(access.owner()).thenReturn("local-user");
        Algorithm algorithm=new Algorithm();algorithm.setCategory(AlgorithmCategoryEnum.detect);
        when(algorithms.selectOne(any())).thenReturn(algorithm);
        DeviceInfo device=new DeviceInfo();device.setId(17L);device.setDeviceId("device-1");device.setTenantId("local-tenant");
        when(access.requireDevice("device-1","om",true)).thenReturn(device);
        when(tasks.getByRequestId(anyString())).thenAnswer(i->rows.get(i.getArgument(0)));
        doAnswer(i->{ModelDispatchTask t=i.getArgument(0);rows.put(t.getRequestId(),t);return null;}).when(tasks).create(any());
        doAnswer(i->{rows.get(i.getArgument(0)).setDispatchStatus("PUBLISHED");return null;}).when(tasks).markPublished(anyString(),anyString());
        when(storage.archive(anyString(),eq("model.om"),any())).thenAnswer(i->{
            Path target=Files.createTempFile("hub-test-archive-",".om");
            try {((ModelArtifactObjectStore.Source)i.getArgument(2)).write(target);assertArrayEquals(model,Files.readAllBytes(target));}
            finally {Files.deleteIfExists(target);}return true;
        });
        when(artifacts.inspect(anyString())).thenReturn(new RemoteModelArtifactService.ArtifactMetadata("model.om",(long)model.length,modelHash));
        VlsModelDispatchProperties properties=new VlsModelDispatchProperties();properties.setPublicBaseUrl("http://download.test");properties.setSigningSecret("isolated-unit-test-only");
        ModelDownloadSignatureService signatures=new ModelDownloadSignatureService();ReflectionTestUtils.setField(signatures,"properties",properties);
        ReflectionTestUtils.setField(dispatch,"dispatchAccess",access);
        ReflectionTestUtils.setField(dispatch,"dispatchProperties",properties);
        ReflectionTestUtils.setField(dispatch,"artifactService",artifacts);
        ReflectionTestUtils.setField(dispatch,"taskService",tasks);
        ReflectionTestUtils.setField(dispatch,"mqttService",mqtt);
        ReflectionTestUtils.setField(dispatch,"signatureService",signatures);
        ReflectionTestUtils.setField(dispatch,"dispatchLocks",new ModelDispatchLocks(null){
            @Override public <T>T run(String key,Supplier<T> work){return work.get();}
        });
    }
    JSONObject manifest() {
        return new JSONObject().set("schema_version",1).set("model_format","om").set("target_chip","Hi3519DV500")
            .set("runtime","svp-acl").set("algorithm_type","detection")
            .set("model",new JSONObject().set("path","model.om").set("size",model.length).set("sha256",modelHash))
            .set("classes",new JSONObject().set("path","classes.yaml").set("size",classes.length).set("sha256",classesHash));
    }
    void detail(String version) {
        http.expect(requestTo("http://hub.test/bus/apaas-deployment-service/aiModel/v1/modelDetail"))
            .andExpect(header("accesstoken","real-user-token")).andExpect(header("tenantid","remote-tenant"))
            .andRespond(withSuccess(new JSONObject().set("code",200).set("data",new JSONObject().set("published_commit",version)
                .set("device_manifest_path","vls-device-manifest.json")).toString(),MediaType.APPLICATION_JSON));
    }
    void download(String path,byte[] bytes,MediaType type) {
        http.expect(requestTo("http://hub.test/bus/apaas-deployment-service/aiModel/v1/downloadFile"))
            .andExpect(content().json(new JSONObject().set("uid","hub-id").set("branch",commit).set("path",path).set("accessToken","real-user-token").toString(),true))
            .andRespond(withSuccess(bytes,type));
    }
    void files(byte[] modelBytes,byte[] classBytes) {
        download("vls-device-manifest.json",manifest().toString().getBytes(StandardCharsets.UTF_8),MediaType.APPLICATION_JSON);
        download("model.om",modelBytes,MediaType.APPLICATION_OCTET_STREAM);
        download("classes.yaml",classBytes,MediaType.APPLICATION_OCTET_STREAM);
    }
    List<Map<String,Object>> invoke() throws Exception {
        return flow.deploy("hub-id",commit,23L,Collections.singletonList("device-1"),operation,"real-user-token","remote-tenant");
    }
    @Test void fixedCommitHashesReachSignedPayloadAndOriginalOperationRecoversWithoutRepublish() throws Exception {
        detail(commit);files(model,classes);detail("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertEquals("PUBLISHED",invoke().get(0).get("status"));
        ArgumentCaptor<Object> payload=ArgumentCaptor.forClass(Object.class);verify(mqtt).publish(anyString(),payload.capture());
        Map<?,?> body=(Map<?,?>)((Map<?,?>)payload.getValue()).get("payload");
        assertEquals(modelHash,body.get("sha256"));assertEquals(classesHash,body.get("classFileSha256"));
        assertFalse(body.containsKey("trainingId"));
        assertTrue(body.get("modelUrl").toString().contains("signature="));
        ModelDispatchTask saved=rows.values().iterator().next();assertNull(saved.getTrainingId());
        assertEquals(classesHash,saved.getClassFileSha256());assertTrue(saved.getRemotePath().contains(commit));
        saved.setDispatchStatus("UNKNOWN");
        assertEquals("UNKNOWN",invoke().get(0).get("status"));
        verify(mqtt,times(1)).publish(anyString(),any());verify(storage,times(1)).archive(anyString(),anyString(),any());
        verify(access,times(3)).requireDevice("device-1","om",true);
        http.verify();
    }
    @Test void tamperedModelNeverArchivesOrPublishes() throws Exception {
        detail(commit);files(new byte[]{4,3,2,1},classes);
        assertThrows(ServiceException.class,this::invoke);verifyNoInteractions(storage,mqtt);assertTrue(rows.isEmpty());http.verify();
    }
    @Test void tamperedClassesNeverArchivesOrPublishes() throws Exception {
        detail(commit);byte[] changed=classes.clone();changed[0]=(byte)'x';files(model,changed);
        assertThrows(ServiceException.class,this::invoke);verifyNoInteractions(storage,mqtt);assertTrue(rows.isEmpty());http.verify();
    }
    @Test void deniedDeviceStopsBeforeAnyHubRequest() throws Exception {
        when(access.requireDevice("device-1","om",true)).thenThrow(new ServiceException("denied"));
        assertThrows(ServiceException.class,this::invoke);verifyNoInteractions(storage,mqtt);http.verify();
    }
    @Test void changedPublishedCommitCannotDispatchOldFiles() throws Exception {
        detail("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        assertThrows(ServiceException.class,this::invoke);verifyNoInteractions(storage,mqtt);http.verify();
    }
}
