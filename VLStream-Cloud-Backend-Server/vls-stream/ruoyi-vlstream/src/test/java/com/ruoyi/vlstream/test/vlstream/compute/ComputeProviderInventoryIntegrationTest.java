package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named="VLS_AUTODL_TEST_JDBC",matches="jdbc:mysql://127\\.0\\.0\\.1:33329/autodl_contract_test.*")
class ComputeProviderInventoryIntegrationTest {
    CloudComputeIntegrationTest fixture;ComputeProviderInventoryService inventory;AutoDlProPowerProvider provider;MockRestServiceServer mock;
    ComputeCredentialCipher cipher=new ComputeCredentialCipher("0123456789abcdef");ComputePublicInventoryService claims;
    @BeforeAll static void database()throws Exception{CloudComputeIntegrationTest.database();}
    @BeforeEach void setup()throws Exception {
        fixture=new CloudComputeIntegrationTest();fixture.setup();claims=new ComputePublicInventoryService(CloudComputeIntegrationTest.jdbc);
        ComputePlatformAuthorization auth=org.mockito.Mockito.mock(ComputePlatformAuthorization.class);
        when(auth.requireIdentity()).thenAnswer(call->{ComputePlatformAuthorization.Context proof=new ComputePlatformAuthorization.Context();proof.setAuthorized(true);proof.setPlatformUserId("fake-platform");proof.setVlsUserId("fake-user");proof.setVlsTenantId(ComputeNodeService.tenant());return proof;});
        ReflectionTestUtils.setField(claims,"authorization",auth);ReflectionTestUtils.setField(fixture.nodes,"publicInventory",claims);
        register(fixture.nodeId,"account-a");RestTemplate http=AutoDlProPowerProvider.transport();mock=MockRestServiceServer.bindTo(http).build();provider=new AutoDlProPowerProvider(cipher,true,http);
        inventory=new ComputeProviderInventoryService(CloudComputeIntegrationTest.jdbc,fixture.nodes,provider,CloudComputeIntegrationTest.transactions);
    }
    @AfterEach void cleanup(){fixture.cleanup();mock.verify();verifyNoInteractions(fixture.ssh);}
    void register(Long id,String scope) {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_product='AUTODL_PRO',provider_instance_id='pro-source',control_mode='API',provider_credential_cipher=?,resource_scope='PLATFORM',billing_type='FREE' WHERE id=?",cipher.encrypt("{\"token\":\"fake-only-token\"}"),id);
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope(scope);identity.setInstanceId("pro-source");
        new TransactionTemplate(CloudComputeIntegrationTest.transactions).execute(tx->{claims.bind(fixture.nodes.lockServer(id),identity);return null;});
    }
    String item(String id){return "{\"uuid\":\""+id+"\",\"req_gpu_amount\":2,\"gpu_spec_uuid\":\"pro6000-p\",\"status\":\"running\",\"name\":\"do-not-export-secret\",\"root_password\":\"do-not-export-secret\",\"jupyter_token\":\"do-not-export-secret\"}";}
    String page(int index,int max,int total,String...items){return "{\"code\":\"Success\",\"data\":{\"page_index\":"+index+",\"page_size\":20,\"max_page\":"+max+",\"result_total\":"+total+",\"list\":["+String.join(",",items)+"]}}";}
    void expectPage(int index,String response) {
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","fake-only-token"))
            .andExpect(content().json("{\"page_index\":"+index+",\"page_size\":20}" )).andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
    }
    @Test void paginatesDeduplicatesAndImportsIdempotentlyWithoutTouchingNodesOrExportingSecrets()throws Exception {
        Map<String,Object> before=CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_node WHERE id=?",fixture.nodeId);
        for(int repeat=0;repeat<2;repeat++){expectPage(1,page(1,2,2,item("pro-a"),item("pro-b")));expectPage(2,page(2,2,2,item("pro-b")));}
        inventory.sync(fixture.nodeId);inventory.sync(fixture.nodeId);
        List<ComputeProviderInventoryService.Draft> result=inventory.list(fixture.nodeId);assertEquals(2,result.size());assertTrue(result.stream().noneMatch(ComputeProviderInventoryService.Draft::isPublishable));
        String exported=fixture.json.writeValueAsString(result);assertFalse(exported.contains("do-not-export"));assertFalse(exported.contains("root_password"));assertFalse(exported.contains("jupyter_token"));assertFalse(exported.contains("fake-only-token"));
        assertEquals(before,CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_node WHERE id=?",fixture.nodeId));assertEquals(1,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
    }
    @Test void sameSupplierIdInTwoStableAccountScopesHasTwoDraftIdentities() {
        ComputeRequests.Node request=new ComputeRequests.Node();request.setName("other-account");request.setHost("other-account.invalid");request.setPassword("fake-only");Long other=fixture.nodes.save(null,request).getId();register(other,"account-b");
        expectPage(1,page(1,1,1,item("pro-same")));expectPage(1,page(1,1,1,item("pro-same")));inventory.sync(fixture.nodeId);inventory.sync(other);
        assertNotEquals(inventory.list(fixture.nodeId).get(0).getInstanceKey(),inventory.list(other).get(0).getInstanceKey());assertEquals(2,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_provider_inventory",Integer.class));
    }
    @Test void failedLaterPageNeverPartiallyImportsOrDeletesOldStockAndMasksSupplierError() {
        expectPage(1,page(1,1,1,item("pro-old")));expectPage(1,page(1,2,2,item("pro-new")));
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andRespond(withServerError().body("root_password=do-not-export-secret token=do-not-export-secret"));
        inventory.sync(fixture.nodeId);ServiceException error=assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));assertFalse(error.getMessage().contains("do-not-export"));
        assertEquals("pro-old",inventory.list(fixture.nodeId).get(0).getProviderInstanceId());assertEquals(1,inventory.list(fixture.nodeId).size());
    }
    @Test void validEmptySnapshotDoesNotDeletePreviouslyImportedInstances() {
        expectPage(1,page(1,1,1,item("pro-old")));expectPage(1,page(1,0,0));inventory.sync(fixture.nodeId);inventory.sync(fixture.nodeId);assertEquals("pro-old",inventory.list(fixture.nodeId).get(0).getProviderInstanceId());
    }
    @Test void partialOrContradictoryResponsesFailClosed() {
        expectPage(1,page(1,2,2,item("pro-a")));expectPage(2,page(2,2,2));
        expectPage(1,page(1,1,2,item("pro-a")));expectPage(1,page(1,2,1,item("pro-a")));expectPage(2,page(2,2,1,item("pro-a").replace("\"running\"","\"not-confirmed\"")));
        for(int attempt=0;attempt<3;attempt++)assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));assertTrue(inventory.list(fixture.nodeId).isEmpty());
    }
    @Test void claimAndSshEnvironmentSurviveDiscoveryAndUnknownStatusAndUnknownPriceBlocksPublication() {
        new TransactionTemplate(CloudComputeIntegrationTest.transactions).execute(tx->claims.claim(fixture.nodes.lockServer(fixture.nodeId)));
        Map<String,Object> claim=CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_public_instance");Map<String,Object> node=CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_node WHERE id=?",fixture.nodeId);
        expectPage(1,page(1,1,1,item("pro-source")));inventory.sync(fixture.nodeId);ComputeProviderInventoryService.Draft draft=inventory.list(fixture.nodeId).get(0);assertEquals(fixture.nodeId,draft.getNodeId());
        mock.reset();mock.expect(requestTo(AutoDlProPowerProvider.BASE+"status")).andRespond(withSuccess("{\"code\":\"Success\",\"data\":\"not-confirmed\"}",MediaType.APPLICATION_JSON));
        String snapshot="{\"code\":\"Success\",\"data\":{\"payg_price\":1970,\"origin_pay_price\":3030,\"root_password\":\"do-not-export-secret\",\"jupyter_token\":\"do-not-export-secret\"}}";
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"snapshot")).andRespond(withSuccess(snapshot,MediaType.APPLICATION_JSON));mock.expect(requestTo(AutoDlProPowerProvider.BASE+"snapshot")).andRespond(withSuccess(snapshot,MediaType.APPLICATION_JSON));
        inventory.refresh(fixture.nodeId,draft.getInstanceKey());draft=inventory.list(fixture.nodeId).get(0);assertEquals("UNKNOWN",draft.getPowerState());assertEquals("UNKNOWN",draft.getPriceUnit());assertFalse(draft.isPublishable());assertEquals(0,draft.getPaygPrice().compareTo(new java.math.BigDecimal("1970")));
        assertEquals(claim,CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_public_instance"));assertEquals(node,CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_node WHERE id=?",fixture.nodeId));
    }
    @Test void ordinaryDisabledMissingCredentialsAndOtherTenantMakeNoSupplierCall() {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_product=NULL WHERE id=?",fixture.nodeId);assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_product='AUTODL_PRO',provider_credential_cipher=NULL WHERE id=?",fixture.nodeId);assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));
        register(fixture.nodeId,"account-a");inventory=new ComputeProviderInventoryService(CloudComputeIntegrationTest.jdbc,fixture.nodes,new AutoDlProPowerProvider(cipher,false,org.mockito.Mockito.mock(RestTemplate.class)),CloudComputeIntegrationTest.transactions);assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));
        TenantContextHolder.setTenantId("tenant-b");assertThrows(ServiceException.class,()->inventory.list(fixture.nodeId));assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));
    }
    @Test void supplierAuthorizationErrorDuringStatusNeverExportsBodyOrChangesOldObservation() {
        expectPage(1,page(1,1,1,item("pro-a")));inventory.sync(fixture.nodeId);mock.reset();mock.expect(requestTo(AutoDlProPowerProvider.BASE+"status")).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("root_password=do-not-export-secret"));
        ComputeProviderInventoryService.Draft draft=inventory.list(fixture.nodeId).get(0);ServiceException error=assertThrows(ServiceException.class,()->inventory.refresh(fixture.nodeId,draft.getInstanceKey()));assertFalse(error.getMessage().contains("do-not-export"));assertEquals("RUNNING",inventory.list(fixture.nodeId).get(0).getPowerState());
    }
    @Test void credentialRotationDuringHttpDiscardsTheWholeResult() {
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andRespond(request->{CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_credential_cipher=? WHERE id=?",cipher.encrypt("{\"token\":\"fake-rotated\"}"),fixture.nodeId);return withSuccess(page(1,1,1,item("pro-a")),MediaType.APPLICATION_JSON).createResponse(request);});
        assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));assertTrue(inventory.list(fixture.nodeId).isEmpty());
    }
    @Test void managementOwnerChangeDuringHttpDiscardsEvenAdministratorSnapshot() {
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andRespond(request->{
            CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET management_owner_subject=? WHERE id=?",String.join("",Collections.nCopies(64,"b")),fixture.nodeId);
            return withSuccess(page(1,1,1,item("pro-a")),MediaType.APPLICATION_JSON).createResponse(request);
        });
        assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));assertTrue(inventory.list(fixture.nodeId).isEmpty());
    }
    @Test void allDiscoveryEndpointsRequireExistingConfigurePermission()throws Exception {
        Class<?> type=com.ruoyi.vlstream.test.vlstream.controller.VlsComputeController.class;
        for(String method:Arrays.asList("inventory","syncInventory","refreshInventory")) {
            java.lang.reflect.Method target=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals(method)).findFirst().get();
            assertArrayEquals(new String[]{"vls:compute:configure"},target.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class).value());
        }
    }
    @Test void discoveredRunningProWithUnknownPriceUnitsAndStopProofCannotBecomePaidSupply() {
        expectPage(1,page(1,1,1,item("pro-source")));inventory.sync(fixture.nodeId);
        ComputeMarketService market=new ComputeMarketService(CloudComputeIntegrationTest.jdbc,fixture.nodes);ReflectionTestUtils.setField(market,"publicInventory",claims);
        ComputeRequests.Offer request=new ComputeRequests.Offer();request.setPublished(true);request.setBillingType("POINTS");request.setPointsPerHour(java.math.BigDecimal.ONE);
        assertThrows(ServiceException.class,()->new TransactionTemplate(CloudComputeIntegrationTest.transactions).execute(tx->{market.publish(fixture.nodeId,request);return null;}));
        assertEquals("FREE",fixture.nodes.get(fixture.nodeId).getBillingType());assertFalse(inventory.list(fixture.nodeId).get(0).isPublishable());
    }
    @Test void expiredDiscoveryObservationBecomesUnknownAndCannotBePublished() {
        expectPage(1,page(1,1,1,item("pro-a")));inventory.sync(fixture.nodeId);CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_provider_inventory SET synced_at=DATE_SUB(NOW(),INTERVAL 6 MINUTE)");
        ComputeProviderInventoryService.Draft draft=inventory.list(fixture.nodeId).get(0);assertTrue(draft.isObservationStale());assertEquals("UNKNOWN",draft.getPowerState());assertEquals("UNKNOWN",draft.getCapacityState());assertFalse(draft.isPublishable());
    }
    @Test void malformedOfficialFieldsNeverCreatePartialStock() {
        expectPage(1,page(1,1,1,item("pro-a").replace("\"uuid\"","\"unexpected_id\"")));
        expectPage(1,page(1,1,1,item("pro-a").replace("\"req_gpu_amount\":2","\"req_gpu_amount\":4294967296")));
        for(int attempt=0;attempt<2;attempt++)assertThrows(ServiceException.class,()->inventory.sync(fixture.nodeId));assertTrue(inventory.list(fixture.nodeId).isEmpty());
    }
    @Test void paginationPreservesNewerStatusButReplacesExpiredObservationWithoutReusingItsTimestamp() {
        expectPage(1,page(1,1,1,item("pro-a")));inventory.sync(fixture.nodeId);mock.reset();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andRespond(request->{
            CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_provider_inventory SET power_state='UNKNOWN',state_checked_at=NOW(3)");
            return withSuccess(page(1,1,1,item("pro-a")),MediaType.APPLICATION_JSON).createResponse(request);
        });
        inventory.sync(fixture.nodeId);ComputeProviderInventoryService.Draft draft=inventory.list(fixture.nodeId).get(0);
        assertEquals("UNKNOWN",draft.getPowerState());assertNotNull(draft.getStateCheckedAt());
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_provider_inventory SET state_checked_at=DATE_SUB(NOW(),INTERVAL 6 MINUTE)");
        mock.reset();expectPage(1,page(1,1,1,item("pro-a")));inventory.sync(fixture.nodeId);draft=inventory.list(fixture.nodeId).get(0);
        assertNull(draft.getStateCheckedAt());assertFalse(draft.isObservationStale());assertEquals("RUNNING",draft.getPowerState());
    }
    @Test void delayedReadOnlyStatusCannotOverwriteANewerObservation() {
        expectPage(1,page(1,1,1,item("pro-a")));inventory.sync(fixture.nodeId);mock.reset();String key=inventory.list(fixture.nodeId).get(0).getInstanceKey();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"status")).andRespond(request->{
            CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_provider_inventory SET power_state='UNKNOWN',state_checked_at=DATE_ADD(NOW(),INTERVAL 1 MINUTE)");
            return withSuccess("{\"code\":\"Success\",\"data\":\"running\"}",MediaType.APPLICATION_JSON).createResponse(request);
        });
        for(int query=0;query<2;query++)mock.expect(requestTo(AutoDlProPowerProvider.BASE+"snapshot")).andRespond(withSuccess("{\"code\":\"Success\",\"data\":{\"payg_price\":1970}}",MediaType.APPLICATION_JSON));
        assertThrows(ServiceException.class,()->inventory.refresh(fixture.nodeId,key));ComputeProviderInventoryService.Draft draft=inventory.list(fixture.nodeId).get(0);
        assertEquals("UNKNOWN",draft.getPowerState());assertNull(draft.getPaygPrice());assertFalse(draft.isPublishable());
    }
}
