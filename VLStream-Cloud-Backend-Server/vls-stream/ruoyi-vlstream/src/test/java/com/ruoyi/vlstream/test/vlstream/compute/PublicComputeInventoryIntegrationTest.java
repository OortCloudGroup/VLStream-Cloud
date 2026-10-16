package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Dedicated localhost-only MySQL and fabricated identities, never a provider/SSO call. */
@Tag("dev")
@EnabledIfEnvironmentVariable(named="VLS_AUTODL_TEST_JDBC",matches="jdbc:mysql://127\\.0\\.0\\.1:33329/autodl_contract_test.*")
class PublicComputeInventoryIntegrationTest {
    private CloudComputeIntegrationTest fixture;
    private ComputePublicInventoryService inventory;
    private ComputeMarketService market;
    private final ThreadLocal<String> platformUser=ThreadLocal.withInitial(()->"platform-a");
    private ComputePlatformAuthorization auth;
    @BeforeAll static void database() throws Exception { CloudComputeIntegrationTest.database(); }
    @BeforeEach void setup() throws Exception {
        fixture=new CloudComputeIntegrationTest();fixture.setup();
        inventory=new ComputePublicInventoryService(CloudComputeIntegrationTest.jdbc);
        auth=mock(ComputePlatformAuthorization.class);
        when(auth.requireIdentity()).thenAnswer(call->{ComputePlatformAuthorization.Context p=new ComputePlatformAuthorization.Context();p.setAuthorized(true);p.setPlatformUserId(platformUser.get());p.setVlsTenantId(ComputeNodeService.tenant());p.setVlsUserId("vls-user");return p;});
        ReflectionTestUtils.setField(inventory,"authorization",auth);
        ReflectionTestUtils.setField(fixture.nodes,"publicInventory",inventory);
        market=new ComputeMarketService(CloudComputeIntegrationTest.jdbc,fixture.nodes);
        ReflectionTestUtils.setField(market,"publicInventory",inventory);
    }
    @AfterEach void cleanup() { fixture.cleanup();platformUser.remove(); }
    private <T> T tx(java.util.function.Supplier<T> action) { return new TransactionTemplate(CloudComputeIntegrationTest.transactions).execute(status->action.get()); }
    private void bind(Long node,String account,String instance) {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_instance_id=?,resource_scope='PLATFORM',billing_type='FREE' WHERE id=?",instance,node);
        ComputeRequests.PublicIdentity r=new ComputeRequests.PublicIdentity();r.setAccountScope(account);r.setInstanceId(instance);
        tx(()->{market.publicIdentity(node,r);return null;});
    }
    private Long secondGpu() {
        ComputeRequests.Node r=new ComputeRequests.Node();r.setName("second-gpu");r.setHost(fixture.nodes.get(fixture.nodeId).getHost());r.setPassword("fake-only");r.setGpuIndex(1);
        Long id=tx(()->fixture.nodes.save(null,r).getId());
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET billing_type='FREE' WHERE id=?",id);
        return id;
    }
    @Test void registeredPublicStockIsVisibleToEveryTenantWithoutOwnerOrTaskDetails() throws Exception {
        bind(fixture.nodeId,"account-a","instance-a");
        assertEquals("FREE",market.list(false).get(0).getClaimState());
        tx(()->market.claim(fixture.nodeId));
        TenantContextHolder.setTenantId("tenant-b");platformUser.set("platform-b");
        List<ComputeMarketService.Resource> list=market.list(false);
        assertEquals(1,list.size());assertEquals("OTHER",list.get(0).getClaimState());assertNull(list.get(0).getTaskCount());assertNull(list.get(0).getMessage());assertFalse(list.get(0).isCanOperate());assertFalse(list.get(0).isSchedulable());
        String data=new ObjectMapper().writeValueAsString(list);
        for(String secret:Arrays.asList("account-a","instance-a","platform-a","ownerSubject","claimId","host","tenantId","password","publicInstanceKey")) assertFalse(data.contains(secret),secret);
        assertTrue(fixture.nodes.list().isEmpty());
    }
    @Test void sameTrustedPlatformUserAcrossTenantsKeepsTheSameClaimAfterServiceRestart() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        String claim=CloudComputeIntegrationTest.jdbc.queryForObject("SELECT claim_id FROM vls_compute_public_instance",String.class);
        TenantContextHolder.setTenantId("tenant-b");
        ComputePublicInventoryService restarted=new ComputePublicInventoryService(CloudComputeIntegrationTest.jdbc);ReflectionTestUtils.setField(restarted,"authorization",auth);
        tx(()->restarted.requireOwner(fixture.nodes.lockServer(fixture.nodeId)));
        assertTrue(tx(()->market.claim(fixture.nodeId)).contains("没有重复"));
        assertEquals(claim,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT claim_id FROM vls_compute_public_instance",String.class));
        assertEquals("MINE",market.list(false).get(0).getClaimState());
        platformUser.set("platform-b");assertThrows(ServiceException.class,()->tx(()->restarted.requireOwner(fixture.nodes.lockServer(fixture.nodeId))));
    }
    @Test void twoUsersRacingOnDifferentGpusHaveExactlyOnePersistentWholeInstanceOwner() throws Exception {
        Long second=secondGpu();bind(fixture.nodeId,"account-a","instance-a");
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',billing_type='FREE' WHERE id=?",second);
        assertEquals(fixture.nodes.get(fixture.nodeId).getPublicInstanceKey(),fixture.nodes.get(second).getPublicInstanceKey());
        CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            java.util.function.BiFunction<Long,String,Future<Boolean>> contender=(node,user)->pool.submit(()->{
                TenantContextHolder.setTenantId(user.equals("platform-a")?"tenant-a":"tenant-b");platformUser.set(user);
                try { ready.countDown();assertTrue(go.await(10,TimeUnit.SECONDS));try {tx(()->market.claim(node));return true;}catch(ServiceException e){return false;} }
                finally {TenantContextHolder.clear();platformUser.remove();}
            });
            Future<Boolean> a=contender.apply(fixture.nodeId,"platform-a"),b=contender.apply(second,"platform-b");
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertEquals(1,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance WHERE public_state='OCCUPIED' AND generation=1",Integer.class));
        } finally {go.countDown();pool.shutdownNow();}
    }
    @Test void providerAccountScopePreventsConflatingIdenticalIdsFromDifferentAccounts() {
        bind(fixture.nodeId,"account-a","same-id");
        ComputeRequests.Node r=new ComputeRequests.Node();r.setName("another-account");r.setHost("another.invalid");r.setPassword("fake-only");
        Long other=tx(()->fixture.nodes.save(null,r).getId());bind(other,"account-b","same-id");
        assertNotEquals(fixture.nodes.get(other).getPublicInstanceKey(),fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());
        tx(()->market.claim(fixture.nodeId));platformUser.set("platform-b");tx(()->market.claim(other));
        assertEquals(2,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance WHERE public_state='OCCUPIED'",Integer.class));
    }
    @Test void unusedClaimCanBeCancelledButNoCloudOrDeletionActionIsInvoked() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        assertTrue(tx(()->market.release(fixture.nodeId)).contains("未使用"));
        assertEquals("FREE",market.list(false).get(0).getClaimState());verifyNoInteractions(fixture.ssh);
        platformUser.set("platform-b");assertDoesNotThrow(()->tx(()->market.claim(fixture.nodeId)));
    }
    @Test void usedWorkspaceRemainsIsolatedAfterCompletionInsteadOfPretendingCleanup() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        tx(()->inventory.training(fixture.nodes.lockServer(fixture.nodeId)));
        assertTrue(tx(()->market.release(fixture.nodeId)).contains("隔离"));
        assertEquals("ISOLATED",market.list(false).get(0).getClaimState());
        platformUser.set("platform-b");assertThrows(ServiceException.class,()->tx(()->market.claim(fixture.nodeId)));verifyNoInteractions(fixture.ssh);
    }
    @Test void activeAndArchivingJobsPreventReleaseAcrossAllGpus() {
        Long second=secondGpu();bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        CloudComputeIntegrationTest.jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,node_id,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir,job_state) VALUES('active','tenant-a',1,?,1,3,4,'object_detection','{}','@preset/detect','{}','/fake', 'ARCHIVING')",second);
        assertThrows(ServiceException.class,()->tx(()->market.release(fixture.nodeId)));
        ComputeRequests.Observation observation=new ComputeRequests.Observation();observation.setPowerState("RUNNING");observation.setCapacityState("UNKNOWN");observation.setNote("mock-only");
        assertThrows(ServiceException.class,()->tx(()->{market.observe(fixture.nodeId,observation);return null;}));
        assertEquals("MINE",market.list(false).get(0).getClaimState());
    }
    @Test void staleSelectionAndResourceManagerRoleCannotOperateAnotherUsersClaim() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        platformUser.set("platform-b");
        assertThrows(ServiceException.class,()->tx(()->market.power(fixture.nodeId,"start")));
        assertThrows(ServiceException.class,()->tx(()->market.power(fixture.nodeId,"stop")));
        assertThrows(ServiceException.class,()->tx(()->fixture.nodes.requireReady(fixture.nodeId)));
        assertThrows(ServiceException.class,()->tx(()->market.release(fixture.nodeId)));
        ComputeRequests.Offer r=new ComputeRequests.Offer();r.setPublished(false);r.setBillingType("FREE");assertThrows(ServiceException.class,()->tx(()->{market.publish(fixture.nodeId,r);return null;}));
        verifyNoInteractions(fixture.ssh);
    }
    @Test void paidAndMixedCardSupplyCannotUseFreeClaimToBypassPaymentGate() {
        Long second=secondGpu();bind(fixture.nodeId,"account-a","instance-a");
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET billing_type='POINTS' WHERE id=?",second);
        assertThrows(ServiceException.class,()->tx(()->market.claim(fixture.nodeId)));
        assertEquals("FREE",CloudComputeIntegrationTest.jdbc.queryForObject("SELECT public_state FROM vls_compute_public_instance",String.class));
    }
    @Test void legacyInventoryAndHistoricWorkspaceNeverGetSyntheticIdentityOrCleanupEvidence() {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',billing_type='FREE' WHERE id=?",fixture.nodeId);
        assertEquals("ISOLATED",market.list(false).get(0).getClaimState());assertThrows(ServiceException.class,()->tx(()->market.claim(fixture.nodeId)));
        CloudComputeIntegrationTest.jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,node_id,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir,job_state) VALUES('old','tenant-a',1,?,1,3,4,'object_detection','{}','@preset/detect','{}','/fake','COMPLETED')",fixture.nodeId);
        bind(fixture.nodeId,"account-a","instance-a");assertEquals("ISOLATED",market.list(false).get(0).getClaimState());
    }
    @Test void invalidOrUntrustedIdentityNeverChangesAClaim() {
        bind(fixture.nodeId,"account-a","instance-a");when(auth.requireIdentity()).thenThrow(new ServiceException("mock rejected",47001));
        assertThrows(ServiceException.class,()->tx(()->market.claim(fixture.nodeId)));
        assertEquals("FREE",CloudComputeIntegrationTest.jdbc.queryForObject("SELECT public_state FROM vls_compute_public_instance",String.class));
    }
    @Test void publicTrainingRequiresBothClaimAndTheCurrentUsersTaskAndPersistsOwnerEvidence() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        fixture.task.setCreateUser("different-vls-user");ComputeRequests.Start r=new ComputeRequests.Start();r.setNodeId(fixture.nodeId);r.setDatasetId(3L);
        assertThrows(ServiceException.class,()->tx(()->fixture.training.start(1L,r)));
        assertEquals(0,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
        fixture.task.setCreateUser("vls-user");tx(()->fixture.training.start(1L,r));
        assertTrue(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT workspace_dirty FROM vls_compute_public_instance",Boolean.class));
        assertNotNull(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT public_claim_id FROM vls_cloud_training_job",String.class));
        fixture.task.setConfigParams(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT config_params FROM vls_algorithm_training WHERE id=1",String.class));
        assertDoesNotThrow(()->fixture.training.current(fixture.task));platformUser.set("platform-b");
        assertThrows(ServiceException.class,()->fixture.training.current(fixture.task));assertThrows(ServiceException.class,()->fixture.training.stop(fixture.task));
    }
    @Test void publicRecordFiltersAndDirectModelDownloadsRejectOtherUsersButPreservePrivateRecords() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        fixture.task.setCreateUser("vls-user");ComputeRequests.Start request=new ComputeRequests.Start();request.setNodeId(fixture.nodeId);request.setDatasetId(3L);
        tx(()->fixture.training.start(1L,request));
        org.springframework.jdbc.core.JdbcTemplate jdbc=CloudComputeIntegrationTest.jdbc;
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_algorithm_model(id BIGINT PRIMARY KEY,training_id BIGINT)");jdbc.update("DELETE FROM vls_algorithm_model");
        jdbc.update("INSERT INTO vls_algorithm_model(id,training_id) VALUES(10,1)");
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,train_status) VALUES(2,'tenant-a','pending')");
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(jdbc,inventory);
        jdbc.update("UPDATE vls_algorithm_training SET create_user='vls-user' WHERE id IN (1,2)");
        MyTrainingTasksService mine=new MyTrainingTasksService(jdbc);ReflectionTestUtils.setField(mine,"publicAccess",access);
        assertEquals(2,mine.query("tenant-a","vls-user",1,10,null,null,null,null).getTotal());
        String sql="SELECT COUNT(*) FROM vls_algorithm_training WHERE "+ComputePublicTaskAccess.predicate("vls_algorithm_training.id").replace("{0}","?");
        assertEquals(2,jdbc.queryForObject(sql,Integer.class,access.viewer()));
        assertDoesNotThrow(()->access.training(1L));assertDoesNotThrow(()->access.model(10L));
        platformUser.set("platform-b");assertEquals(1,jdbc.queryForObject(sql,Integer.class,access.viewer()));
        assertEquals(1,mine.query("tenant-a","vls-user",1,10,null,null,null,null).getTotal());
        assertThrows(ServiceException.class,()->access.training(1L));assertThrows(ServiceException.class,()->access.model(10L));
        // This fixture has SSO proof but no local LoginHelper actor. A stored creator alone is not authorization.
        assertThrows(ServiceException.class,()->access.training(2L));
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
        try {
            com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmModelServiceImpl models=new com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmModelServiceImpl();
            ReflectionTestUtils.setField(models,"publicAccess",access);
            assertThrows(ServiceException.class,()->models.getById(10L));assertThrows(ServiceException.class,()->models.getModelById(10L));
            com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmTrainingServiceImpl trainings=new com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmTrainingServiceImpl();
            ReflectionTestUtils.setField(trainings,"publicAccess",access);
            assertThrows(ServiceException.class,()->trainings.getById(1L));assertThrows(ServiceException.class,()->trainings.selectAlgorithmTrainingById(1L));
        } finally {org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();}
        TenantContextHolder.setTenantId("tenant-b");platformUser.set("platform-a");assertThrows(ServiceException.class,()->access.training(1L));
        verifyNoInteractions(fixture.ssh);
    }
    @Test void personalStatisticsBindDistinctRegularAndSharedSubjectsInSqlOrder() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));fixture.task.setCreateUser("vls-user");
        ComputeRequests.Start request=new ComputeRequests.Start();request.setNodeId(fixture.nodeId);request.setDatasetId(3L);tx(()->fixture.training.start(1L,request));
        org.springframework.jdbc.core.JdbcTemplate jdbc=CloudComputeIntegrationTest.jdbc;jdbc.update("UPDATE vls_algorithm_training SET create_user='vls-user' WHERE id=1");
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(jdbc,inventory);ComputeSharedQueueService shared=mock(ComputeSharedQueueService.class);
        when(shared.viewer()).thenReturn("synthetic-distinct-shared-owner-a");ReflectionTestUtils.setField(access,"sharedQueue",shared);
        MyTrainingTasksService mine=new MyTrainingTasksService(jdbc);ReflectionTestUtils.setField(mine,"publicAccess",access);
        assertEquals(1,mine.query("tenant-a","vls-user",1,10,null,null,null,null).getTotal());
        platformUser.set("platform-b");assertEquals(0,mine.query("tenant-a","vls-user",1,10,null,null,null,null).getTotal());
        jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject='synthetic-distinct-shared-owner-a',options_json='{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\"}}'");
        assertEquals(1,mine.query("tenant-a","vls-user",1,10,null,null,null,null).getTotal());
        when(shared.viewer()).thenReturn("synthetic-distinct-shared-owner-b");assertEquals(0,mine.query("tenant-a","vls-user",1,10,null,null,null,null).getTotal());
    }
    @Test void unknownOwnerOfHistoricalPublicJobIsHiddenWithoutGuessingUserMapping() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));
        fixture.task.setCreateUser("vls-user");ComputeRequests.Start request=new ComputeRequests.Start();request.setNodeId(fixture.nodeId);request.setDatasetId(3L);
        tx(()->fixture.training.start(1L,request));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_cloud_training_job SET public_instance_key=NULL,public_owner_subject=NULL,public_claim_id=NULL");
        fixture.task.setConfigParams(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT config_params FROM vls_algorithm_training WHERE id=1",String.class));
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);
        assertThrows(ServiceException.class,()->access.training(1L));assertThrows(ServiceException.class,()->fixture.training.current(fixture.task));
        assertThrows(ServiceException.class,()->fixture.training.stop(fixture.task));verifyNoInteractions(fixture.ssh);
    }
    @Test void editingAnEndpointCannotKeepAnOldPublicIdentityOrBypassAnotherInstancesOwner() {
        bind(fixture.nodeId,"account-a","source");String source=fixture.nodes.get(fixture.nodeId).getPublicInstanceKey();String host=fixture.nodes.get(fixture.nodeId).getHost();
        ComputeRequests.Node targetRequest=new ComputeRequests.Node();targetRequest.setName("target");targetRequest.setHost("target.invalid");targetRequest.setPassword("fake-only");
        Long target=tx(()->fixture.nodes.save(null,targetRequest).getId());bind(target,"account-a","target");platformUser.set("platform-b");tx(()->market.claim(target));platformUser.set("platform-a");
        ComputeRequests.Node edit=new ComputeRequests.Node();edit.setName("edited");edit.setHost("target.invalid");edit.setGpuIndex(1);
        assertThrows(ServiceException.class,()->tx(()->fixture.nodes.save(fixture.nodeId,edit)));
        assertEquals(host,fixture.nodes.get(fixture.nodeId).getHost());assertEquals(source,fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());
        edit.setHost("different.invalid");assertThrows(ServiceException.class,()->tx(()->fixture.nodes.save(fixture.nodeId,edit)));
        ComputeRequests.Node privateRequest=new ComputeRequests.Node();privateRequest.setName("private");privateRequest.setHost("private.invalid");privateRequest.setPassword("fake-only");
        Long privateId=tx(()->fixture.nodes.save(null,privateRequest).getId());edit.setHost("target.invalid");
        assertThrows(ServiceException.class,()->tx(()->fixture.nodes.save(privateId,edit)));assertNull(fixture.nodes.get(privateId).getPublicInstanceKey());assertEquals("private.invalid",fixture.nodes.get(privateId).getHost());
        verifyNoInteractions(fixture.ssh);
    }
    private ComputeProviderSettingsService providerSettings() {
        return new ComputeProviderSettingsService(CloudComputeIntegrationTest.jdbc,fixture.nodes,new ComputeCredentialCipher("0123456789abcdef"),fixture.json);
    }
    private ComputeRequests.ProviderSettings providerRequest(String instance) {
        ComputeRequests.ProviderSettings request=new ComputeRequests.ProviderSettings();request.setControlMode("MANUAL");request.setProviderProduct("AUTODL_PRO");request.setProviderInstanceId(instance);return request;
    }
    private void publicProviderFixture() {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_product='AUTODL_PRO' WHERE id=?",fixture.nodeId);
        bind(fixture.nodeId,"account-a","source");
    }
    @Test void providerConfigurationCannotRebindTwoIdlePublicInstancesWithDifferentKeys() {
        publicProviderFixture();String source=fixture.nodes.get(fixture.nodeId).getPublicInstanceKey();
        ComputeRequests.Node target=new ComputeRequests.Node();target.setName("idle target");target.setHost("idle-target.invalid");target.setPassword("fake-only");
        Long targetId=tx(()->fixture.nodes.save(null,target).getId());bind(targetId,"account-a","target");String targetKey=fixture.nodes.get(targetId).getPublicInstanceKey();assertNotEquals(source,targetKey);
        for(String mode:Arrays.asList("MANUAL","API")) {
            ComputeRequests.ProviderSettings request=providerRequest("target");request.setControlMode(mode);request.setToken("fake-only-not-used");
            assertThrows(ServiceException.class,()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
        }
        assertEquals("source",fixture.nodes.get(fixture.nodeId).getProviderInstanceId());assertEquals(source,fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());
        assertEquals("target",fixture.nodes.get(targetId).getProviderInstanceId());assertEquals(targetKey,fixture.nodes.get(targetId).getPublicInstanceKey());verifyNoInteractions(fixture.ssh);
    }
    @Test void everyPublicIdentityUpdateEntryRejectsInstanceProductRegionProviderAndAccountChanges() {
        publicProviderFixture();String key=fixture.nodes.get(fixture.nodeId).getPublicInstanceKey();
        for(int kind=0;kind<3;kind++) {
            ComputeRequests.ProviderSettings request=providerRequest("source");
            if(kind==0)request.setProviderInstanceId(null);else if(kind==1)request.setProviderProduct("ECS");else request.setProviderRegion("cn-hangzhou");
            assertThrows(ServiceException.class,()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
        }
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("account-b");identity.setInstanceId("source");
        assertThrows(ServiceException.class,()->tx(()->{market.publicIdentity(fixture.nodeId,identity);return null;}));
        identity.setAccountScope("account-a");identity.setInstanceId("target");assertThrows(ServiceException.class,()->tx(()->{market.publicIdentity(fixture.nodeId,identity);return null;}));
        ComputeRequests.Node edit=new ComputeRequests.Node();edit.setName("same endpoint");edit.setHost(fixture.nodes.get(fixture.nodeId).getHost());edit.setProvider("aliyun");
        assertThrows(ServiceException.class,()->tx(()->fixture.nodes.save(fixture.nodeId,edit)));
        assertEquals(key,fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());assertEquals("source",fixture.nodes.get(fixture.nodeId).getProviderInstanceId());
        assertEquals("AUTODL_PRO",fixture.nodes.get(fixture.nodeId).getProviderProduct());assertNull(fixture.nodes.get(fixture.nodeId).getProviderRegion());
        assertEquals(1,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance",Integer.class));verifyNoInteractions(fixture.ssh);
    }
    @Test void cleanPublicInstanceAllowsNonIdentityConfigurationWithoutChangingItsKey() {
        publicProviderFixture();String key=fixture.nodes.get(fixture.nodeId).getPublicInstanceKey();
        ComputeCredentialCipher cipher=new ComputeCredentialCipher("0123456789abcdef");String encrypted=cipher.encrypt("{\"token\":\"fake-only-token\"}");
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_credential_cipher=? WHERE id=?",encrypted,fixture.nodeId);
        ComputeRequests.ProviderSettings request=providerRequest("source");request.setControlMode("API");request.setRequestedStoppedMode("KeepCharging");
        assertDoesNotThrow(()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
        assertEquals("API",fixture.nodes.get(fixture.nodeId).getControlMode());assertEquals(encrypted,fixture.nodes.get(fixture.nodeId).getProviderCredentialCipher());
        request.setControlMode("MANUAL");request.setClearCredentials(true);assertDoesNotThrow(()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
        assertEquals("MANUAL",fixture.nodes.get(fixture.nodeId).getControlMode());assertNull(fixture.nodes.get(fixture.nodeId).getProviderCredentialCipher());
        assertEquals(key,fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());assertEquals("source",fixture.nodes.get(fixture.nodeId).getProviderInstanceId());
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("account-a");identity.setInstanceId("source");
        assertDoesNotThrow(()->tx(()->{market.publicIdentity(fixture.nodeId,identity);return null;}));assertEquals(key,fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());verifyNoInteractions(fixture.ssh);
    }
    @Test void claimedAndIsolatedPublicInstancesRejectBothIdentityAndOtherProviderEdits() {
        publicProviderFixture();tx(()->market.claim(fixture.nodeId));String key=fixture.nodes.get(fixture.nodeId).getPublicInstanceKey();
        for(String expected:Arrays.asList("MINE","ISOLATED")) {
            assertEquals(expected,market.list(false).get(0).getClaimState());
            for(String instance:Arrays.asList("source","target")) {
                ComputeRequests.ProviderSettings request=providerRequest(instance);request.setRequestedStoppedMode("KeepCharging");
                assertThrows(ServiceException.class,()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
            }
            ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("account-b");identity.setInstanceId("source");
            assertThrows(ServiceException.class,()->tx(()->{market.publicIdentity(fixture.nodeId,identity);return null;}));
            if("MINE".equals(expected)){tx(()->inventory.training(fixture.nodes.lockServer(fixture.nodeId)));tx(()->market.release(fixture.nodeId));}
        }
        assertEquals(key,fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());assertEquals("source",fixture.nodes.get(fixture.nodeId).getProviderInstanceId());verifyNoInteractions(fixture.ssh);
    }
    @Test void anUnboundLegacyAliasCannotRebindAnExistingPublicGpuMembersAccountScope() {
        Long second=secondGpu();publicProviderFixture();String key=fixture.nodes.get(second).getPublicInstanceKey();
        // Simulate an old partial registration, without modifying any real database.
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET public_instance_key=NULL WHERE id=?",fixture.nodeId);
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("account-b");identity.setInstanceId("source");
        assertThrows(ServiceException.class,()->tx(()->{market.publicIdentity(fixture.nodeId,identity);return null;}));
        assertNull(fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());assertEquals(key,fixture.nodes.get(second).getPublicInstanceKey());
        assertEquals(1,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance",Integer.class));verifyNoInteractions(fixture.ssh);
    }
    @Test void privateProviderConfigurationCannotCreateAnotherControlIdentityForAPublicInstance() {
        publicProviderFixture();ComputeRequests.Node request=new ComputeRequests.Node();request.setName("private source");request.setHost("private-source.invalid");request.setPassword("fake-only");
        Long privateId=tx(()->fixture.nodes.save(null,request).getId());ComputeRequests.ProviderSettings settings=providerRequest("source");settings.setControlMode("API");settings.setToken("fake-only-not-used");
        assertThrows(ServiceException.class,()->tx(()->{providerSettings().save(privateId,settings);return null;}));
        assertNull(fixture.nodes.get(privateId).getPublicInstanceKey());assertNull(fixture.nodes.get(privateId).getProviderInstanceId());assertEquals("MANUAL",fixture.nodes.get(privateId).getControlMode());verifyNoInteractions(fixture.ssh);
    }
    @Test void privateProviderIdentityRemainsEditableWithReplacementTestCredentials() {
        ComputeRequests.ProviderSettings request=providerRequest("private-a");request.setControlMode("API");request.setToken("fake-only-a");
        assertDoesNotThrow(()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
        String first=fixture.nodes.get(fixture.nodeId).getProviderCredentialCipher();request.setProviderInstanceId("private-b");request.setToken("fake-only-b");
        assertDoesNotThrow(()->tx(()->{providerSettings().save(fixture.nodeId,request);return null;}));
        assertEquals("private-b",fixture.nodes.get(fixture.nodeId).getProviderInstanceId());assertNull(fixture.nodes.get(fixture.nodeId).getPublicInstanceKey());
        assertNotNull(fixture.nodes.get(fixture.nodeId).getProviderCredentialCipher());assertNotEquals(first,fixture.nodes.get(fixture.nodeId).getProviderCredentialCipher());verifyNoInteractions(fixture.ssh);
    }
    @Test void claimAfterRealLockWaitReadsAnotherGpusNewPaidStateDespiteAnOldRepeatableReadSnapshot() throws Exception {
        Long second=secondGpu();bind(fixture.nodeId,"account-a","instance-a");
        ExecutorService pool=Executors.newSingleThreadExecutor();CountDownLatch snapshot=new CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<Future<String>> result=new java.util.concurrent.atomic.AtomicReference<>();
        try {
            tx(()->{fixture.nodes.lockServer(second);result.set(pool.submit(()->{TenantContextHolder.setTenantId("tenant-a");try{return tx(()->{
                assertEquals("FREE",CloudComputeIntegrationTest.jdbc.queryForObject("SELECT billing_type FROM vls_compute_node WHERE id=?",String.class,second));snapshot.countDown();
                try{market.claim(fixture.nodeId);return "wrongly-claimed";}catch(ServiceException e){return e.getMessage();}
            });}finally{TenantContextHolder.clear();platformUser.remove();}}));
                try{assertTrue(snapshot.await(10,TimeUnit.SECONDS));assertThrows(java.util.concurrent.TimeoutException.class,()->result.get().get(200,TimeUnit.MILLISECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET billing_type='POINTS' WHERE id=?",second);return null;});
            assertTrue(result.get().get(10,TimeUnit.SECONDS).contains("收费"));assertEquals("FREE",CloudComputeIntegrationTest.jdbc.queryForObject("SELECT public_state FROM vls_compute_public_instance",String.class));
        }finally{pool.shutdownNow();}
    }
    @Test void registrationAfterRealLockWaitSeesCommittedHistoricWorkspaceInsteadOfTheOldEmptySnapshot() throws Exception {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_instance_id='instance-a',resource_scope='PLATFORM' WHERE id=?",fixture.nodeId);
        ExecutorService pool=Executors.newSingleThreadExecutor();CountDownLatch snapshot=new CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<Future<?>> result=new java.util.concurrent.atomic.AtomicReference<>();
        try {
            tx(()->{fixture.nodes.lockServer(fixture.nodeId);result.set(pool.submit(()->{TenantContextHolder.setTenantId("tenant-a");try{return tx(()->{
                assertEquals(0,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));snapshot.countDown();ComputeRequests.PublicIdentity r=new ComputeRequests.PublicIdentity();r.setAccountScope("account-a");r.setInstanceId("instance-a");market.publicIdentity(fixture.nodeId,r);return null;
            });}finally{TenantContextHolder.clear();}}));
                try{assertTrue(snapshot.await(10,TimeUnit.SECONDS));assertThrows(java.util.concurrent.TimeoutException.class,()->result.get().get(200,TimeUnit.MILLISECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}
                CloudComputeIntegrationTest.jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,node_id,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir,job_state) VALUES('historic','tenant-a',1,?,1,3,4,'object_detection','{}','@preset/detect','{}','/fake','COMPLETED')",fixture.nodeId);return null;});
            result.get().get(10,TimeUnit.SECONDS);assertEquals("ISOLATED",market.list(false).get(0).getClaimState());assertTrue(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT workspace_dirty FROM vls_compute_public_instance",Boolean.class));
        }finally{pool.shutdownNow();}
    }
    @Test void containerIdsCannotExposePublicLogsDetailsOrMutationsToAnotherUser() {
        bind(fixture.nodeId,"account-a","instance-a");tx(()->market.claim(fixture.nodeId));fixture.task.setCreateUser("vls-user");ComputeRequests.Start r=new ComputeRequests.Start();r.setNodeId(fixture.nodeId);r.setDatasetId(3L);tx(()->fixture.training.start(1L,r));
        Long id=CloudComputeIntegrationTest.jdbc.queryForObject("SELECT container_record_id FROM vls_cloud_training_job",Long.class);
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_cloud_training_job SET log_tail='owner-only-log'");
        assertEquals("owner-only-log",fixture.training.containerLogs(id));
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);
        String sql="SELECT COUNT(*) FROM vls_container_instance WHERE "+ComputePublicTaskAccess.containerPredicate("vls_container_instance.id","vls_container_instance.training_task_id").replace("{0}","?");
        assertEquals(1,CloudComputeIntegrationTest.jdbc.queryForObject(sql,Integer.class,access.viewer()));platformUser.set("platform-b");
        assertEquals(0,CloudComputeIntegrationTest.jdbc.queryForObject(sql,Integer.class,access.viewer()));assertThrows(ServiceException.class,()->fixture.training.containerLogs(id));
        com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService records=mock(com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService.class);
        com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService scheduler=mock(com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService.class);
        com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController controller=new com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController(records,scheduler);ReflectionTestUtils.setField(controller,"publicAccess",access);
        com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance input=new com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance();input.setId(id);
        assertThrows(ServiceException.class,()->controller.getById(id));assertThrows(ServiceException.class,()->controller.logs(id,100));assertThrows(ServiceException.class,()->controller.save(input));assertThrows(ServiceException.class,()->controller.update(input));assertThrows(ServiceException.class,()->controller.submit(input));assertThrows(ServiceException.class,()->controller.remove(id.toString()));
        verifyNoInteractions(records,scheduler,fixture.ssh);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
        try{com.ruoyi.vlstream.test.vlstream.service.impl.VlsContainerInstanceServiceImpl service=new com.ruoyi.vlstream.test.vlstream.service.impl.VlsContainerInstanceServiceImpl();ReflectionTestUtils.setField(service,"publicAccess",access);assertThrows(ServiceException.class,()->service.getById(id));assertThrows(ServiceException.class,()->service.updateMonitoringData(id,null,null,null));}finally{org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();}
        assertThrows(ServiceException.class,()->access.container(999L)); // Missing records cannot establish an owner.
    }
    @Test void providerQueryAndRecoveryBelongToTheVerifiedClaimantRatherThanTheConfigurationRole() {
        bind(fixture.nodeId,"account-a","instance-a");assertFalse(market.list(false).get(0).isCanQueryProvider());assertTrue(market.list(false).get(0).isCanManage());tx(()->market.claim(fixture.nodeId));
        assertTrue(market.list(false).get(0).isCanQueryProvider());assertFalse(market.list(false).get(0).isCanManage());
        ComputePowerProvider provider=mock(ComputePowerProvider.class);when(provider.supports(any())).thenReturn(true);when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","mock"));
        ComputeProviderStateService service=new ComputeProviderStateService(CloudComputeIntegrationTest.jdbc,fixture.nodes);ReflectionTestUtils.setField(service,"providers",java.util.Collections.singletonList(provider));
        TenantContextHolder.setTenantId("tenant-b");assertDoesNotThrow(()->tx(()->service.refresh(fixture.nodeId,false)));verify(provider,times(1)).status(any());platformUser.set("platform-b");
        assertFalse(market.list(false).get(0).isCanQueryProvider());assertThrows(ServiceException.class,()->tx(()->service.refresh(fixture.nodeId,false)));assertThrows(ServiceException.class,()->tx(()->service.refresh(fixture.nodeId,true)));verify(provider,times(1)).status(any());
        platformUser.set("platform-a");CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET power_operation_id='mock-op',power_operation_action='stop',power_operation_started_at=DATE_SUB(NOW(),INTERVAL 11 MINUTE) WHERE id=?",fixture.nodeId);
        assertDoesNotThrow(()->tx(()->service.refresh(fixture.nodeId,true)));assertNull(fixture.nodes.get(fixture.nodeId).getPowerOperationId());verify(provider,never()).submit(any(),anyString(),anyString());
        tx(()->inventory.training(fixture.nodes.lockServer(fixture.nodeId)));tx(()->market.release(fixture.nodeId));assertFalse(market.list(false).get(0).isCanQueryProvider());assertThrows(ServiceException.class,()->tx(()->service.refresh(fixture.nodeId,false)));
    }
}
