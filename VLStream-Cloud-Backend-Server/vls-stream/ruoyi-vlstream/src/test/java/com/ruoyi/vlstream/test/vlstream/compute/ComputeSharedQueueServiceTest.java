package com.ruoyi.vlstream.test.vlstream.compute;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeSharedQueueServiceTest {
    private final ObjectMapper json=new ObjectMapper();
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final ComputeSharedQueueProperties properties=new ComputeSharedQueueProperties();
    private ComputeSharedQueueService shared;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId("000000");properties.setEnvironmentId("localhost");
        shared=new ComputeSharedQueueService(properties,mock(ComputeTrustedSelfPolicy.class),jdbc,json) {
            @Override protected String authenticatedUser(){return "alice";}
        };
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    private ComputePublicTaskAccess access() {
        ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class);when(inventory.viewer()).thenReturn("verified-sso-subject");
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(jdbc,inventory);
        ReflectionTestUtils.setField(access,"sharedQueue",shared);return access;
    }
    private Map<String,Object> row(String subject,String options) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("tenant_id","000000");row.put("public_owner_subject",subject);row.put("options_json",options);return row;
    }
    private void currentRow(Map<String,Object> row) {
        when(jdbc.queryForList(anyString(),eq(1L))).thenAnswer(call -> call.getArgument(0).toString().contains("SELECT pj.")?Collections.singletonList(row):Collections.emptyList());
    }
    @Test void nativeSubjectRequiresCurrentUserAndStableEnvironmentAndTenant() {
        String local=shared.requireCreator("alice");assertFalse(local.isEmpty());
        assertThrows(ServiceException.class,()->shared.requireCreator("bob"));
        properties.setEnvironmentId("vm");assertNotEquals(local,shared.viewer());
        TenantContextHolder.setTenantId("100000");assertNotEquals(local,shared.viewer());
        properties.setEnvironmentId("other");assertEquals("",shared.viewer());
        assertThrows(ServiceException.class,()->shared.requireCreator("alice"));
    }
    @Test void sharedRecordsUseNativeSubjectOnlyWhenFrozenSharedModeIsPresent() {
        currentRow(row(shared.viewer(),"{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\"}}"));
        access().training(1L);
        currentRow(row("verified-sso-subject","{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\"}}"));
        assertThrows(ServiceException.class,()->access().training(1L));
        currentRow(row(shared.viewer(),"{}"));assertThrows(ServiceException.class,()->access().training(1L));
    }
    @Test void existingSsoPrivateAndExclusivePublicOwnersRemainRequired() {
        currentRow(row("verified-sso-subject","{}"));access().training(1L);
        currentRow(row("someone-else","{}"));assertThrows(ServiceException.class,()->access().training(1L));
        currentRow(row(shared.viewer(),"invalid-json"));assertThrows(ServiceException.class,()->access().training(1L));
        Map<String,Object> foreign=row(shared.viewer(),"{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\"}}");foreign.put("tenant_id","100000");
        currentRow(foreign);assertThrows(ServiceException.class,()->access().training(1L));
    }
    @Test void listAndContainerPredicatesSeparateNativeAndSsoOwnersFailClosed() {
        QueryWrapper<Object> query=access().filter(new QueryWrapper<>(),"training.id");
        assertTrue(query.getSqlSegment().contains("JSON_VALID"));assertTrue(query.getSqlSegment().contains("CASE WHEN"));
        assertTrue(query.getParamNameValuePairs().containsValue(shared.viewer()));
        assertTrue(query.getParamNameValuePairs().containsValue("verified-sso-subject"));
        QueryWrapper<Object> containers=access().filterContainers(new QueryWrapper<>());
        assertTrue(containers.getSqlSegment().contains("SHARED_GPU_QUEUE"));
        assertTrue(containers.getParamNameValuePairs().containsValue(shared.viewer()));
    }
    @Test void rawOrOtherUsersCheckpointIsRejectedAndOwnVerifiedPoolOutputIsAccepted() {
        assertEquals("@preset/yolov8m",shared.verifyBaseModel("@preset/yolov8m","object_detection",shared.viewer()));
        when(jdbc.queryForList(anyString(),eq("000000"),anyString(),anyString())).thenReturn(Collections.emptyList());
        assertThrows(ServiceException.class,()->shared.verifyBaseModel("uploaded/untrusted.pt","object_detection",shared.viewer()));
        Map<String,Object> source=new LinkedHashMap<>();source.put("annotation_type","object_detection");source.put("options_json","{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\"}}");
        when(jdbc.queryForList(anyString(),eq("000000"),anyString(),anyString())).thenReturn(Collections.singletonList(source));
        String path="cloud-training/1/task/weights/best.pt";
        assertEquals(path,shared.verifyBaseModel(path,"object_detection",shared.viewer()));
        assertThrows(ServiceException.class,()->shared.verifyBaseModel(path,"image_classification",shared.viewer()));
    }
    @Test void onlyGpuBusyMayRetainEarlierVerifiedRuntimeEvidence() throws Exception {
        ComputeNode node=new ComputeNode();node.setSchedulingMode(ComputeSharedQueueService.MODE);
        node.setPythonPath("/data/work/vlstream-gpu/runtime-173/envs/vls-standard/bin/python");
        String runtime="\"python\":\"3.10.22\",\"torch\":\"2.5.1+cu124\",\"torchvision\":\"0.20.1+cu124\",\"cudaVersion\":\"12.4\",\"ultralytics\":\"8.3.240\"";
        node.setProbeJson("{\"contractVersion\":1,\"ready\":true,\"pythonIsolated\":true,\"pythonPrefix\":\"/data/work/vlstream-gpu/runtime-173/envs/vls-standard\",\"gpuUuid\":\"GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac\","+runtime+"}");
        String check="{\"name\":\"GPU 前后向运算与 Torchvision 算子\",\"passed\":false}";
        String busy="{\"contractVersion\":1,\"gpuBusy\":true,\"pythonIsolated\":true,\"pythonPrefix\":\"/data/work/vlstream-gpu/runtime-173/envs/vls-standard\","+runtime+",\"checks\":["+check+"]}";
        assertTrue(shared.retainBusyProof(node,json.readTree(busy)));
        assertFalse(shared.retainBusyProof(node,json.readTree(busy.replace("2.5.1+cu124","2.4.0+cu121"))));
        assertFalse(shared.retainBusyProof(node,json.readTree(busy.replace(check,check+",{\"name\":\"四类基础权重完整性\",\"passed\":false}"))));
        node.setSchedulingMode(ComputeSharedQueueService.EXCLUSIVE);assertFalse(shared.retainBusyProof(node,json.readTree(busy)));
    }
    @Test void ownerHashCannotBeReusedBetweenDifferentPlatformEnvironments() {
        assertEquals(ModelArtifactObjectStore.hash("company-shared-vls-user\nlocalhost\n000000\nalice"),shared.viewer());
    }
}
