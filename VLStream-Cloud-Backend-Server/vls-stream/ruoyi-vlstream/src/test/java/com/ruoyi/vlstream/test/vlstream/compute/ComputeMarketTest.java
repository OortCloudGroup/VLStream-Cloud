package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class ComputeMarketTest {
    private final long now = System.currentTimeMillis();
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    private ComputeNode ready() {
        ComputeNode node = new ComputeNode(); node.setId(1L); node.setEnabled(true); node.setProbeState("READY");
        node.setProbeTime(new Date(now)); node.setPowerState("RUNNING"); node.setCapacityState("UNKNOWN");
        return node;
    }
    @Test void unknownInventoryDoesNotMeanNoInventoryButStaleHealthBlocksScheduling() {
        ComputeNode node = ready(); assertTrue(ComputeAvailability.ready(node, now));
        assertFalse(ComputeAvailability.noCapacity(node, now));
        node.setProbeTime(new Date(now - ComputeAvailability.FRESHNESS_MS - 1));
        assertFalse(ComputeAvailability.ready(node, now));
    }
    @Test void confirmedNoStockIsFilteredOnlyWhileEvidenceIsFresh() {
        ComputeNode node = ready(); node.setCapacityState("UNAVAILABLE"); node.setCapacityCheckedAt(new Date(now));
        assertTrue(ComputeAvailability.noCapacity(node, now)); assertFalse(ComputeAvailability.ready(node, now));
        assertFalse(ComputeAvailability.noCapacity(node, now + ComputeAvailability.FRESHNESS_MS + 1));
    }
    @Test void poweredOffAndTransitionsCannotReuseAnOldSuccessfulProbe() {
        for (String state : new String[]{"STOPPED", "STARTING", "STOPPING"}) {
            ComputeNode node = ready(); node.setPowerState(state); assertFalse(ComputeAvailability.ready(node, now));
        }
    }
    @Test void failedGpuInspectionOnlyFiltersExplicitAbsentGpuNotQueryErrorsOrBusyCards() {
        ComputeNode node = ready(); node.setProbeJson("{\"contractVersion\":1,\"gpuAvailable\":false}");
        assertTrue(ComputeAvailability.noGpu(node, now));
        node.setProbeJson("{\"contractVersion\":1,\"gpuAvailable\":true,\"gpuBusy\":true}");
        assertFalse(ComputeAvailability.noGpu(node, now));
        node.setProbeJson("{\"message\":\"timeout\"}"); assertFalse(ComputeAvailability.noGpu(node, now));
    }
    @Test void anotherTenantCannotStopSharedCapacity() {
        TenantContextHolder.setTenantId("consumer");
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeNode node = ready(); node.setTenantId("operator"); when(nodes.get(1L)).thenReturn(node);
        when(nodes.lockServer(1L)).thenReturn(node); when(nodes.serverRows(node)).thenReturn(java.util.Collections.singletonList(node));
        assertThrows(ServiceException.class, () -> new ComputeMarketService(jdbc, nodes).power(1L, "stop"));
        verify(nodes, never()).assertIdle(anyLong()); verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void busyNodeStopIsRejectedBeforeProviderOperation() {
        TenantContextHolder.setTenantId("operator");
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeNode node = ready(); node.setTenantId("operator"); when(nodes.get(1L)).thenReturn(node);
        when(nodes.lockServer(1L)).thenReturn(node); when(nodes.serverRows(node)).thenReturn(java.util.Collections.singletonList(node));
        doThrow(new ServiceException("busy")).when(nodes).assertIdle(1L);
        assertThrows(ServiceException.class, () -> new ComputeMarketService(jdbc, nodes).power(1L, "stop"));
    }
    @Test void unsupportedControlIsExplicitAndDoesNotChangeRunningState() {
        TenantContextHolder.setTenantId("operator");
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputeNodeService nodes = mock(ComputeNodeService.class);
        ComputeNode node = ready(); node.setProvider("autodl"); when(nodes.get(1L)).thenReturn(node);
        when(nodes.lockServer(1L)).thenReturn(node); when(nodes.serverRows(node)).thenReturn(java.util.Collections.singletonList(node));
        assertTrue(new ComputeMarketService(jdbc, nodes).power(1L, "start").startsWith("未执行云端操作"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void marketDtoHasNoConnectionOrCredentialFields() throws Exception {
        String value = new ObjectMapper().writeValueAsString(new ComputeMarketService.Resource());
        assertFalse(value.contains("host")); assertFalse(value.contains("password")); assertFalse(value.contains("tenantId"));
    }
    @Test void paidPublishingRejectsOrdinaryStoppedModeBeforeChangingTheOffer() {
        TenantContextHolder.setTenantId("operator"); JdbcTemplate jdbc=mock(JdbcTemplate.class); ComputeNodeService nodes=mock(ComputeNodeService.class);
        ComputeNode node=ready(); node.setTenantId("operator"); node.setProvider("aliyun"); node.setProviderProduct("ECS"); node.setPowerState("STOPPED"); node.setActualStoppedMode("KeepCharging");
        when(jdbc.query(anyString(),any(org.springframework.jdbc.core.RowMapper.class),eq("operator"),eq(1L))).thenReturn(java.util.Collections.singletonList(node));
        ComputeRequests.Offer offer=new ComputeRequests.Offer(); offer.setPublished(true); offer.setBillingType("POINTS"); offer.setPointsPerHour(java.math.BigDecimal.ONE);
        assertThrows(ServiceException.class,()->new ComputeMarketService(jdbc,nodes).publish(1L,offer));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void unpublishingPaidAndPublishingFreeDoNotRequireACloudEconomicalStop() {
        TenantContextHolder.setTenantId("operator"); JdbcTemplate jdbc=mock(JdbcTemplate.class); ComputeNodeService nodes=mock(ComputeNodeService.class);
        ComputeNode node=ready(); node.setTenantId("operator"); node.setProvider("self");
        when(jdbc.query(anyString(),any(org.springframework.jdbc.core.RowMapper.class),eq("operator"),eq(1L))).thenReturn(java.util.Collections.singletonList(node));
        ComputeRequests.Offer offer=new ComputeRequests.Offer(); offer.setPublished(false); offer.setBillingType("POINTS"); offer.setPointsPerHour(java.math.BigDecimal.ONE);
        ComputeMarketService market=new ComputeMarketService(jdbc,nodes); assertDoesNotThrow(()->market.publish(1L,offer));
        offer.setPublished(true); offer.setBillingType("FREE"); assertDoesNotThrow(()->market.publish(1L,offer));
        verify(jdbc).update(contains("SET resource_scope"),eq("PRIVATE"),eq("POINTS"),eq(java.math.BigDecimal.ONE),eq("operator"),eq(1L));
        verify(jdbc).update(contains("SET resource_scope"),eq("PLATFORM"),eq("FREE"),isNull(),eq("operator"),eq(1L));
    }
    @Test void unconfiguredPaidSupplyCannotStartCloudPowerEvenWithAPermissionAndDisplayedReferencePrice() {
        TenantContextHolder.setTenantId("operator"); JdbcTemplate jdbc=mock(JdbcTemplate.class); ComputeNodeService nodes=mock(ComputeNodeService.class);
        ComputeNode node=ready(); node.setTenantId("operator"); node.setProvider("autodl"); node.setResourceScope("PLATFORM"); node.setBillingType("POINTS");
        ComputePlatformAuthorization.Context verified=new ComputePlatformAuthorization.Context(); verified.setAuthorized(true); when(nodes.authorizePaidOperation(1L)).thenReturn(verified);
        when(nodes.lockServer(1L)).thenReturn(node); when(nodes.serverRows(node)).thenReturn(java.util.Collections.singletonList(node));
        ServiceException blocked=assertThrows(ServiceException.class,()->new ComputeMarketService(jdbc,nodes).power(1L,"start")); assertTrue(blocked.getMessage().contains("工作空间成本由平台承担")); assertTrue(blocked.getMessage().contains("停机不代表可安全复用")); assertFalse(blocked.getMessage().contains("存储计量")); verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void callerCannotSetPersistedTaskOwnershipThroughJson() throws Exception {
        com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining task = new ObjectMapper().readValue("{\"taskName\":\"test\",\"createUser\":\"spoof-user\",\"tenantId\":\"spoof-tenant\"}", com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining.class);
        assertNull(task.getCreateUser()); assertNull(task.getTenantId()); assertEquals("test",task.getTaskName());
    }
    @Test void missingAuthenticatedUserCannotEnumerateTenantTasks() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        MyTrainingTasksService tasks = new MyTrainingTasksService(jdbc) { @Override protected String authenticatedUser() { return null; } };
        assertThrows(ServiceException.class, () -> tasks.page(1,20,null,null,null,null));
        verifyNoInteractions(jdbc);
    }
    @Test void timeoutDoesNotSubmitAndNeverClaimsNoStock() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputePowerProvider provider = mock(ComputePowerProvider.class);
        ComputeNode node = ready(); node.setPowerOperationId("op"); node.setPowerOperationStartedAt(new Date(now - 11 * 60 * 1000));
        new ComputePowerWorker(jdbc).poll(node, provider);
        verifyNoInteractions(provider);
        verify(jdbc).update(contains("power_state='UNKNOWN'"), eq(1L), eq("op"));
    }
    @Test void providerAcceptanceRequiresPollingAndExplicitFailureIsRecorded() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputePowerProvider provider = mock(ComputePowerProvider.class);
        ComputeNode node = ready(); node.setPowerState("STARTING"); node.setPowerOperationId("op"); node.setPowerOperationAction("start"); node.setPowerOperationStartedAt(new Date(now));
        when(jdbc.update(contains("submitted=1"), eq(1L), eq("op"))).thenReturn(1);
        when(provider.status(node)).thenReturn(new ComputePowerProvider.Snapshot("FAILED", "UNAVAILABLE", "GPU 无库存"));
        new ComputePowerWorker(jdbc).poll(node, provider);
        verify(provider).submit(node, "start", "op");
        verify(jdbc).update(contains("power_operation_id=IF"), eq("FAILED"), eq("UNAVAILABLE"), isNull(), isNull(), isNull(), isNull(), eq("GPU 无库存"), eq(true), eq(1L), eq("op"));
    }
    @Test void submittedOperationIsPolledWithoutDuplicateSubmission() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputePowerProvider provider = mock(ComputePowerProvider.class);
        ComputeNode node = ready(); node.setPowerState("STARTING"); node.setPowerOperationId("op"); node.setPowerOperationAction("start"); node.setPowerOperationStartedAt(new Date(now)); node.setPowerOperationSubmitted(true);
        when(provider.status(node)).thenReturn(new ComputePowerProvider.Snapshot("UNKNOWN", "UNKNOWN", "查询失败"));
        new ComputePowerWorker(jdbc).poll(node, provider);
        verify(provider, never()).submit(any(), anyString(), anyString());
        verify(jdbc).update(contains("power_operation_id=IF"), eq("STARTING"), eq("UNKNOWN"), isNull(), isNull(), isNull(), isNull(), eq("查询失败"), eq(false), eq(1L), eq("op"));
    }
    @Test void onlyDocumentedNoStockCodesBecomeUnavailableNotSaleHintsOrUnrelatedErrors() {
        assertEquals(ComputeProviderException.Kind.NO_STOCK, ComputeProviderException.aliyunKind("OperationDenied.NoStock"));
        assertEquals(ComputeProviderException.Kind.NO_STOCK, ComputeProviderException.aliyunKind("Invalid.PrivatePoolOptions.NoStock"));
        assertEquals(ComputeProviderException.Kind.UNKNOWN, ComputeProviderException.aliyunKind("LackResource"));
        assertEquals(ComputeProviderException.Kind.BALANCE, ComputeProviderException.aliyunKind("InsufficientBalance"));
        JdbcTemplate jdbc = mock(JdbcTemplate.class); ComputeNode node = ready(); node.setPowerOperationId("op");
        ComputeProviderException error = new ComputeProviderException(ComputeProviderException.Kind.NO_STOCK);
        new ComputePowerWorker(jdbc).recordFailure(node, error);
        verify(jdbc).update(contains("power_operation_id=IF"), eq("UNAVAILABLE"), eq(error.getMessage()), eq(true), eq(1L), eq("op"));
    }
}
