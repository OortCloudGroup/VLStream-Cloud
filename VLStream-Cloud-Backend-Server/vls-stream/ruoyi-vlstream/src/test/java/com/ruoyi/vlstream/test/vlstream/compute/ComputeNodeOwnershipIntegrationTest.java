package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.dao.*;
import cn.dev33.satoken.servlet.model.*;
import cn.dev33.satoken.stp.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual services/MySQL/current Sa permission roles, synthetic identities and no SSH/vendor operations. */
@Tag("dev")
@EnabledIfEnvironmentVariable(named="VLS_AUTODL_TEST_JDBC",matches="jdbc:mysql://127\\.0\\.0\\.1:33329/autodl_contract_test.*")
class ComputeNodeOwnershipIntegrationTest {
    CloudComputeIntegrationTest f;
    ComputeNodeOwnershipService ownership;
    ComputePublicInventoryService inventory;
    ComputeMarketService market;
    ComputePlatformAuthorization authorization;
    AtomicReference<String> actor=new AtomicReference<>("alice");
    String local="vls-alice";
    Set<String> permissions=new HashSet<>();
    cn.dev33.satoken.context.SaTokenContext previousContext;
    SaTokenDao previousDao;
    StpInterface previousPermissions;
    SaTokenDaoDefaultImpl memory;
    @BeforeAll static void schema()throws Exception{CloudComputeIntegrationTest.database();}
    @BeforeEach void setup()throws Exception {
        f=new CloudComputeIntegrationTest();f.setup();
        previousContext=SaManager.getSaTokenContext();previousDao=SaManager.getSaTokenDao();previousPermissions=SaManager.getStpInterface();
        memory=new SaTokenDaoDefaultImpl();SaManager.setSaTokenDao(memory);SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        MockHttpServletRequest request=new MockHttpServletRequest();MockHttpServletResponse response=new MockHttpServletResponse();
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),new SaResponseForServlet(response),new SaStorageForServlet(request));
        SaManager.setStpInterface(new StpInterface(){public List<String> getPermissionList(Object id,String type){return new ArrayList<>(permissions);}public List<String> getRoleList(Object id,String type){return Collections.emptyList();}});
        StpUtil.login("owner-fixture-only");permissions.add("vls:compute:configure");setActor("alice");
        authorization=mock(ComputePlatformAuthorization.class);
        when(authorization.requireIdentity()).thenAnswer(call->{ComputePlatformAuthorization.Context value=new ComputePlatformAuthorization.Context();value.setAuthorized(true);value.setPlatformUserId(actor.get());value.setVlsUserId(LoginHelper.getUserId());value.setVlsTenantId(ComputeNodeService.tenant());return value;});
        ownership=new ComputeNodeOwnershipService(authorization);inventory=new ComputePublicInventoryService(CloudComputeIntegrationTest.jdbc);
        ReflectionTestUtils.setField(inventory,"authorization",authorization);ReflectionTestUtils.setField(f.nodes,"ownership",ownership);ReflectionTestUtils.setField(f.nodes,"publicInventory",inventory);
        market=new ComputeMarketService(CloudComputeIntegrationTest.jdbc,f.nodes);ReflectionTestUtils.setField(market,"publicInventory",inventory);
        CloudComputeIntegrationTest.jdbc.update("DELETE FROM vls_compute_node");
        f.nodeId=create("alice.invalid");ready(f.nodeId);f.task.setCreateUser(local);
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_algorithm_training SET create_user=? WHERE id=1",local);
    }
    @AfterEach void cleanup(){f.cleanup();SaTokenContextForThreadLocalStorage.clearBox();SaManager.setStpInterface(previousPermissions);SaManager.setSaTokenDao(previousDao);SaManager.setSaTokenContext(previousContext);memory.endRefreshThread();}
    void setActor(String value){actor.set(value);local="vls-"+value;cn.dev33.satoken.context.SaHolder.getStorage().set(LoginHelper.USER_KEY,local);}
    Long create(String host){ComputeRequests.Node value=f.request();value.setHost(host);return f.nodes.save(null,value).getId();}
    void ready(Long id){CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET probe_state='READY',probe_time=NOW(),power_state='RUNNING',power_checked_at=NOW(),probe_json='{\"contractVersion\":1,\"ready\":true}' WHERE id=?",id);}
    Map<String,Object> row(Long id){return CloudComputeIntegrationTest.jdbc.queryForMap("SELECT * FROM vls_compute_node WHERE id=?",id);}
    <T>T tx(java.util.function.Supplier<T> action){return new TransactionTemplate(CloudComputeIntegrationTest.transactions).execute(status->action.get());}
    void denied(Runnable action){assertThrows(ServiceException.class,action::run);}
    @Test void sameTenantUsersOnlyListAndReadTheirOwnNodesAndCannotSupplyAnOwner()throws Exception {
        String aliceOwner=f.nodes.get(f.nodeId).getManagementOwnerSubject();setActor("bob");Long bob=create("bob.invalid");
        assertEquals(Collections.singletonList(bob),ids(f.nodes.list()));denied(()->f.nodes.getManaged(f.nodeId));
        setActor("alice");assertEquals(Collections.singletonList(f.nodeId),ids(f.nodes.list()));assertEquals("MINE",f.nodes.list().get(0).getManagementOwnership());denied(()->f.nodes.getManaged(bob));
        ComputeRequests.Node attack=f.json.copy().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false).readValue("{\"name\":\"attack\",\"host\":\"attack.invalid\",\"password\":\"fake-only\",\"managementOwnerSubject\":\"victim\"}",ComputeRequests.Node.class);
        // Runtime Jackson normally ignores unknown DTO fields; the strict fixture must reject them or omit them.
        assertNull(Arrays.stream(ComputeRequests.Node.class.getDeclaredFields()).filter(field->field.getName().equals("managementOwnerSubject")).findFirst().orElse(null));
        assertEquals(aliceOwner,f.nodes.save(null,attack).getManagementOwnerSubject());
        String json=f.json.writeValueAsString(f.nodes.list());assertFalse(json.contains(aliceOwner));assertFalse(json.contains("encrypted-test-secret"));
    }
    static List<Long> ids(List<ComputeNode> nodes){List<Long> ids=new ArrayList<>();for(ComputeNode node:nodes)ids.add(node.getId());return ids;}
    @Test void otherOwnerMutationProbeAliasesAndAllManagementServicesFailBeforeSideEffects() {
        Map<String,Object> before=row(f.nodeId);setActor("bob");ComputeRequests.Node edit=f.request();edit.setHost("alice.invalid");
        denied(()->tx(()->f.nodes.save(f.nodeId,edit)));denied(()->tx(()->{f.nodes.delete(f.nodeId);return null;}));denied(()->f.nodes.probe(f.nodeId));
        edit.setGpuIndex(1);denied(()->tx(()->f.nodes.save(null,edit)));
        ComputeRequests.Offer offer=new ComputeRequests.Offer();offer.setBillingType("FREE");offer.setPublished(true);denied(()->tx(()->{market.publish(f.nodeId,offer);return null;}));
        ComputeRequests.Observation observed=new ComputeRequests.Observation();observed.setPowerState("RUNNING");observed.setCapacityState("UNKNOWN");observed.setNote("fixture");denied(()->tx(()->{market.observe(f.nodeId,observed);return null;}));
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("fixture-account");identity.setInstanceId("fixture-instance");denied(()->tx(()->{market.publicIdentity(f.nodeId,identity);return null;}));
        ComputeRequests.ProviderSettings settings=new ComputeRequests.ProviderSettings();settings.setControlMode("MANUAL");
        ComputeProviderSettingsService configure=new ComputeProviderSettingsService(CloudComputeIntegrationTest.jdbc,f.nodes,mock(ComputeCredentialCipher.class),f.json);denied(()->tx(()->{configure.save(f.nodeId,settings);return null;}));
        ComputeCostObservationService cost=new ComputeCostObservationService(CloudComputeIntegrationTest.jdbc,f.nodes,f.json);denied(()->tx(()->cost.query(f.nodeId)));
        ComputeProviderStateService state=new ComputeProviderStateService(CloudComputeIntegrationTest.jdbc,f.nodes);denied(()->tx(()->state.refresh(f.nodeId,false)));denied(()->tx(()->market.power(f.nodeId,"stop")));
        AutoDlProPowerProvider adapter=mock(AutoDlProPowerProvider.class);ComputeProviderInventoryService drafts=new ComputeProviderInventoryService(CloudComputeIntegrationTest.jdbc,f.nodes,adapter,CloudComputeIntegrationTest.transactions);
        denied(()->drafts.list(f.nodeId));denied(()->drafts.sync(f.nodeId));denied(()->drafts.refresh(f.nodeId,String.join("",Collections.nCopies(64,"a"))));
        assertEquals(before,row(f.nodeId));verifyNoInteractions(f.ssh,adapter);
    }
    ComputeRequests.ProviderSettings vendorSettings(String mode) {
        ComputeRequests.ProviderSettings request=new ComputeRequests.ProviderSettings();request.setControlMode(mode);request.setProviderProduct("AUTODL_PRO");request.setProviderInstanceId("owner-fixture-vendor");request.setToken("synthetic-owner-test-only");return request;
    }
    Long vendorTarget(String host) {
        Long target=create(host);CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET provider_product='AUTODL_PRO',provider_instance_id='owner-fixture-vendor' WHERE id=?",target);return target;
    }
    @Test void anotherUsersVendorInstanceCannotBeBoundThroughApiOrManualAlias() {
        setActor("bob");Long target=vendorTarget("bob-vendor.invalid");Map<String,Object> targetBefore=row(target);setActor("alice");Map<String,Object> sourceBefore=row(f.nodeId);
        ComputeCredentialCipher cipher=spy(new ComputeCredentialCipher("0123456789abcdef"));ComputeProviderSettingsService settings=new ComputeProviderSettingsService(CloudComputeIntegrationTest.jdbc,f.nodes,cipher,f.json);
        for(String mode:Arrays.asList("API","MANUAL"))denied(()->tx(()->{settings.save(f.nodeId,vendorSettings(mode));return null;}));
        assertEquals(sourceBefore,row(f.nodeId));assertEquals(targetBefore,row(target));verify(cipher,never()).encrypt(anyString());verifyNoInteractions(f.ssh);
    }
    @Test void ownVendorAliasesRemainConfigurableAndPreserveBothManagementOwners() {
        Long target=vendorTarget("alice-vendor.invalid");String owner=f.nodes.get(target).getManagementOwnerSubject();
        ComputeProviderSettingsService settings=new ComputeProviderSettingsService(CloudComputeIntegrationTest.jdbc,f.nodes,new ComputeCredentialCipher("0123456789abcdef"),f.json);
        tx(()->{settings.save(f.nodeId,vendorSettings("API"));return null;});assertEquals("owner-fixture-vendor",f.nodes.get(f.nodeId).getProviderInstanceId());
        assertEquals(owner,f.nodes.get(f.nodeId).getManagementOwnerSubject());assertEquals(owner,f.nodes.get(target).getManagementOwnerSubject());assertTrue(f.nodes.get(f.nodeId).isProviderCredentialConfigured());
        assertEquals(2,f.nodes.serverRows(f.nodes.get(f.nodeId)).size());verifyNoInteractions(f.ssh);
    }
    @Test void explicitAdministratorMayConfigureHistoricalVendorTargetWithoutAssigningItsOwner() {
        setActor("bob");Long target=vendorTarget("legacy-vendor.invalid");CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET management_owner_subject=NULL WHERE id=?",target);
        setActor("administrator");permissions.add("vls:compute:manage-all");String sourceOwner=f.nodes.get(f.nodeId).getManagementOwnerSubject();
        ComputeProviderSettingsService settings=new ComputeProviderSettingsService(CloudComputeIntegrationTest.jdbc,f.nodes,new ComputeCredentialCipher("0123456789abcdef"),f.json);
        tx(()->{settings.save(f.nodeId,vendorSettings("API"));return null;});assertNull(f.nodes.get(target).getManagementOwnerSubject());assertEquals(sourceOwner,f.nodes.get(f.nodeId).getManagementOwnerSubject());verifyNoInteractions(f.ssh);
    }
    @Test void administratorVendorRebindingDoesNotCrossTenantBoundary() {
        TenantContextHolder.setTenantId("tenant-b");setActor("bob");Long target=vendorTarget("other-tenant-vendor.invalid");Map<String,Object> before=row(target);
        TenantContextHolder.setTenantId("tenant-a");setActor("administrator");permissions.add("vls:compute:manage-all");
        ComputeCredentialCipher cipher=spy(new ComputeCredentialCipher("0123456789abcdef"));ComputeProviderSettingsService settings=new ComputeProviderSettingsService(CloudComputeIntegrationTest.jdbc,f.nodes,cipher,f.json);
        denied(()->tx(()->{settings.save(f.nodeId,vendorSettings("API"));return null;}));assertEquals(before,row(target));assertNull(f.nodes.get(f.nodeId).getProviderInstanceId());verify(cipher,never()).encrypt(anyString());verifyNoInteractions(f.ssh);
    }
    @Test void explicitAdministratorKeepsLegacyManagementWithoutClaimingItOrGainingPrivateTraining() {
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET management_owner_subject=NULL WHERE id=?",f.nodeId);
        assertTrue(f.nodes.list().isEmpty());denied(()->f.nodes.getManaged(f.nodeId));
        setActor("administrator");permissions.add("vls:compute:manage-all");assertEquals("UNASSIGNED",f.nodes.list().get(0).getManagementOwnership());
        ComputeRequests.Node edit=f.request();edit.setHost("alice.invalid");edit.setPassword(null);tx(()->f.nodes.save(f.nodeId,edit));assertNull(f.nodes.get(f.nodeId).getManagementOwnerSubject());
        ready(f.nodeId);denied(()->tx(()->f.nodes.requireReady(f.nodeId)));
        TenantContextHolder.setTenantId("tenant-b");assertTrue(f.nodes.list().isEmpty());denied(()->f.nodes.getManaged(f.nodeId));
    }
    @Test void existingSuperAdministratorAndWildcardScopeRemainExplicit() {
        setActor("administrator");cn.dev33.satoken.context.SaHolder.getStorage().set(LoginHelper.USER_KEY,"1");assertTrue(ownership.administrator());assertEquals(1,f.nodes.list().size());
        cn.dev33.satoken.context.SaHolder.getStorage().set(LoginHelper.USER_KEY,local);assertFalse(ownership.administrator());permissions.add("*:*:*");assertTrue(ownership.administrator());assertEquals(1,f.nodes.list().size());
    }
    @Test void sameSsoInAnotherTenantDoesNotGainPrivateManagement() {
        String hash=f.nodes.get(f.nodeId).getManagementOwnerSubject();TenantContextHolder.setTenantId("tenant-b");assertTrue(f.nodes.list().isEmpty());denied(()->f.nodes.getManaged(f.nodeId));
        Long second=create("alice-tenant-b.invalid");assertNotEquals(hash,f.nodes.get(second).getManagementOwnerSubject());assertEquals(Collections.singletonList(second),ids(f.nodes.list()));
    }
    @Test void marketCompanyPublicVisibilityIsIndependentOfManagementOwnerAndPrivateSelectionIsPersonal() {
        setActor("bob");Long bob=create("bob.invalid");ready(bob);CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',management_owner_subject=NULL WHERE id=?",bob);
        Long privateBob=create("bob-private.invalid");ready(privateBob);setActor("alice");
        List<ComputeMarketService.Resource> publicRows=market.list(false);assertEquals(1,publicRows.size());assertEquals(bob.toString(),publicRows.get(0).getId());assertFalse(publicRows.get(0).isCanManage());
        List<ComputeMarketService.Resource> selection=market.list(true);assertEquals(2,selection.size());assertTrue(selection.stream().noneMatch(value->value.getId().equals(privateBob.toString())));
        TenantContextHolder.setTenantId("tenant-b");assertEquals(bob.toString(),market.list(false).get(0).getId());assertEquals(1,market.list(true).size());
    }
    @Test void publicConsumerClaimDoesNotBecomeResourceManagerAndPaidSupplyRemainsBlocked() {
        setActor("bob");Long offered=create("offered.invalid");CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',billing_type='FREE',provider_instance_id='instance-fixture' WHERE id=?",offered);
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("account-fixture");identity.setInstanceId("instance-fixture");tx(()->{market.publicIdentity(offered,identity);return null;});
        setActor("alice");tx(()->market.claim(offered));denied(()->f.nodes.getManaged(offered));assertEquals("MINE",inventory.state(f.nodes.get(offered),inventory.viewer()));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET billing_type='POINTS' WHERE id=?",offered);Map<String,Object> before=row(offered);denied(()->tx(()->market.claim(offered)));assertEquals(before,row(offered));
    }
    @Test void privateTrainingSubjectAndLogsArtifactsContainersStayWithActualUser() {
        tx(()->{f.start();return null;});
        assertNull(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT public_owner_subject FROM vls_cloud_training_job",String.class));
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);access.training(1L);
        setActor("bob");denied(()->tx(()->f.nodes.requireReady(f.nodeId)));denied(()->f.training.logs(f.task));denied(()->f.training.progress(f.task));denied(()->f.training.stop(f.task));denied(()->access.training(1L));
        Long container=CloudComputeIntegrationTest.jdbc.queryForObject("SELECT container_record_id FROM vls_cloud_training_job",Long.class);denied(()->access.container(container));denied(()->f.training.containerLogs(container));
        assertEquals(0,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));verifyNoInteractions(f.ssh);
        setActor("alice");access.training(1L);assertEquals(1,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));
    }
    @Test void localLoginWithoutPlatformProofCanConnectManageAndTrainOnlyItsPrivateNode() {
        when(authorization.requireIdentity()).thenThrow(new ServiceException("fixture platform proof absent",47001));
        ComputeRequests.Node request=f.request();request.setHost("local-self.invalid");
        Long own=tx(()->f.nodes.saveOwn(null,request)).getId();
        assertTrue(ids(f.nodes.list()).contains(own));assertEquals(own,f.nodes.getOwned(own).getId());
        assertTrue(f.nodes.canUsePrivate(f.nodes.get(own)));assertNull(f.nodes.trainingSubject(f.nodes.get(own),local));
        ready(own);tx(()->{f.start();return null;});f.training.logs(f.task);
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);access.training(1L);
        assertEquals(1,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));
        setActor("bob");assertTrue(f.nodes.list().isEmpty());denied(()->f.nodes.getOwned(own));denied(()->tx(()->f.nodes.saveOwn(own,request)));denied(()->access.training(1L));
        setActor("alice");TenantContextHolder.setTenantId("tenant-b");assertTrue(f.nodes.list().isEmpty());denied(()->f.nodes.getOwned(own));denied(()->access.training(1L));verifyNoInteractions(f.ssh);
    }
    @Test void selfServiceCannotBindKeysPublicAliasesOrUseAdministratorScopeAsOwnership() {
        ComputeRequests.Node request=f.request();request.setHost("self-guard.invalid");request.setAuthMode("DEPLOYMENT_KEY");
        denied(()->tx(()->f.nodes.saveOwn(null,request)));verifyNoInteractions(f.ssh);
        request.setAuthMode("PASSWORD");CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',billing_type='POINTS' WHERE id=?",f.nodeId);
        request.setHost("alice.invalid");denied(()->tx(()->f.nodes.saveOwn(null,request)));denied(()->f.nodes.getOwned(f.nodeId));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET resource_scope='PRIVATE',public_instance_key='fixture-public' WHERE id=?",f.nodeId);
        denied(()->f.nodes.getOwned(f.nodeId));denied(()->tx(()->f.nodes.saveOwn(null,request)));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET public_instance_key=NULL WHERE id=?",f.nodeId);
        setActor("admin");permissions.add("vls:compute:manage-all");assertTrue(f.nodes.canManage(f.nodes.get(f.nodeId)));denied(()->f.nodes.getOwned(f.nodeId));
    }
    @Test void historicalPlatformOwnedRowsKeepTheirOriginalVerifiedOwner() {
        String legacy=ownership.legacyViewer();CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET management_owner_subject=? WHERE id=?",legacy,f.nodeId);
        assertEquals(Collections.singletonList(f.nodeId),ids(f.nodes.list()));assertEquals(legacy,f.nodes.getOwned(f.nodeId).getManagementOwnerSubject());
        when(authorization.requireIdentity()).thenThrow(new ServiceException("fixture no platform identity",47001));
        assertTrue(f.nodes.list().isEmpty());denied(()->f.nodes.getOwned(f.nodeId));assertEquals(legacy,f.nodes.get(f.nodeId).getManagementOwnerSubject());
        ComputeRequests.Node request=f.request();request.setHost("new-local.invalid");assertNotEquals(legacy,tx(()->f.nodes.saveOwn(null,request)).getManagementOwnerSubject());
    }
    @Test void selfServiceProbeAndDeleteRecheckLockedCurrentVisibilityBeforeSshOrDeletion() {
        Map<String,Object> before=row(f.nodeId);
        ComputeNodeService operations=spy(f.nodes);
        ComputeNode current=f.nodes.get(f.nodeId);current.setResourceScope("PLATFORM");
        // The earlier visible row was private, but the row obtained after waiting for the lock is now public.
        doReturn(current).when(operations).lockServer(f.nodeId);
        denied(()->tx(()->operations.probeOwn(f.nodeId)));
        denied(()->tx(()->{operations.deleteOwn(f.nodeId);return null;}));
        ComputeRequests.Node request=f.request();denied(()->tx(()->operations.saveOwn(f.nodeId,request)));
        assertEquals(before,row(f.nodeId));verifyNoInteractions(f.ssh);
    }
    @Test void resourceAdministratorDoesNotReceiveAnotherUsersKnownTaskData() {
        tx(()->{f.start();return null;});setActor("administrator");permissions.add("vls:compute:manage-all");assertEquals(1,f.nodes.list().size());
        denied(()->new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory).training(1L));denied(()->f.training.logs(f.task));
    }
    @Test void legacyPrivateTaskKeepsOnlyItsConfirmedLocalAuthorWithoutAssigningSso() {
        tx(()->{f.start();return null;});CloudComputeIntegrationTest.jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=NULL");
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);access.training(1L);f.training.logs(f.task);
        Long container=CloudComputeIntegrationTest.jdbc.queryForObject("SELECT container_record_id FROM vls_cloud_training_job",Long.class);f.training.containerLogs(container);
        setActor("bob");denied(()->access.training(1L));denied(()->f.training.logs(f.task));denied(()->f.training.containerLogs(container));
        permissions.add("vls:compute:manage-all");denied(()->access.training(1L));
        setActor("alice");CloudComputeIntegrationTest.jdbc.update("UPDATE vls_algorithm_training SET create_user=NULL WHERE id=1");denied(()->access.training(1L));
        assertNull(CloudComputeIntegrationTest.jdbc.queryForObject("SELECT public_owner_subject FROM vls_cloud_training_job",String.class));
    }
    int filteredCount(String table,com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?> query) {
        String raw=query.getSqlSegment();java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([^}]+)\\}").matcher(raw);
        StringBuffer sql=new StringBuffer();List<Object> args=new ArrayList<>();
        while(matcher.find()){args.add(query.getParamNameValuePairs().get(matcher.group(1)));matcher.appendReplacement(sql,"?");}matcher.appendTail(sql);
        return CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+sql,Integer.class,args.toArray());
    }
    @Test void actualMysqlTrainingAndContainerFiltersPreserveOnlyConfirmedLegacyAuthor() {
        tx(()->{f.start();return null;});CloudComputeIntegrationTest.jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=NULL");
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);
        assertEquals(1,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));
        assertEquals(1,filteredCount("vls_container_instance",access.filterContainers(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>())));
        setActor("bob");assertEquals(0,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));
        assertEquals(0,filteredCount("vls_container_instance",access.filterContainers(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>())));
        setActor("alice");CloudComputeIntegrationTest.jdbc.update("UPDATE vls_algorithm_training SET create_user=NULL WHERE id=1");
        assertEquals(0,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));
    }
    @Test void removingCompletedNodeNeverDropsPersistedTaskOwnershipOrLegacyAuthorGuard() {
        tx(()->{f.start();return null;});CloudComputeIntegrationTest.jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'");
        tx(()->{f.nodes.delete(f.nodeId);return null;});ComputePublicTaskAccess access=new ComputePublicTaskAccess(CloudComputeIntegrationTest.jdbc,inventory);
        access.training(1L);setActor("bob");denied(()->access.training(1L));
        assertEquals(0,filteredCount("vls_algorithm_training",access.filter(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>(),"vls_algorithm_training.id")));
        assertEquals(0,filteredCount("vls_container_instance",access.filterContainers(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>())));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=NULL");denied(()->access.training(1L));
        setActor("alice");access.training(1L);CloudComputeIntegrationTest.jdbc.update("UPDATE vls_algorithm_training SET create_user=NULL WHERE id=1");denied(()->access.training(1L));
    }
    @Test void forgedTaskCreatorIsRejectedBeforeQueueOrSshWork() {
        f.task.setCreateUser("someone-else");denied(()->tx(()->{f.start();return null;}));assertEquals(0,CloudComputeIntegrationTest.jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));verifyNoInteractions(f.ssh);
    }
    @Test void ownerChangeDuringProbeCannotMarkAnotherOwnersSnapshotReady()throws Exception {
        ComputeNode snapshot=f.nodes.getManaged(f.nodeId);when(f.connection.execute(anyString(),anyInt())).thenAnswer(call->{CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET management_owner_subject=? WHERE id=?",com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash("trusted-platform-user\nbob"),f.nodeId);return "VLS_PROBE={\"contractVersion\":1,\"ready\":true}";});
        assertFalse(f.nodes.probeSnapshot(snapshot));
        CloudComputeIntegrationTest.jdbc.update("UPDATE vls_compute_node SET management_owner_subject=? WHERE id=?",snapshot.getManagementOwnerSubject(),f.nodeId);
        denied(()->f.nodes.probe(f.nodeId));
    }
    @Test void migrationPreservesExistingUnownedRowsAndRegistersOnlyPermissionCatalogue()throws Exception {
        org.springframework.jdbc.core.JdbcTemplate jdbc=CloudComputeIntegrationTest.jdbc;
        jdbc.execute("DROP TABLE IF EXISTS owner_compat_node");jdbc.execute("DROP TABLE IF EXISTS owner_compat_menu");jdbc.execute("CREATE TABLE owner_compat_node LIKE vls_compute_node");
        jdbc.execute("ALTER TABLE owner_compat_node DROP INDEX idx_compute_management_owner,DROP COLUMN management_owner_subject");jdbc.execute("CREATE TABLE owner_compat_menu LIKE sys_menu");
        jdbc.update("INSERT INTO owner_compat_node(id,tenant_id,name,host,port,username,password_cipher,python_path,work_dir,gpu_index,enabled) VALUES(99,'tenant-a','legacy','legacy.invalid',22,'root','fake-only','/fake/python','/fake/work',0,1)");
        Map<String,Object> before=jdbc.queryForMap("SELECT * FROM owner_compat_node WHERE id=99");String sql=new String(Files.readAllBytes(CloudComputeIntegrationTest.root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_041__compute_node_management_owner.sql")),StandardCharsets.UTF_8).replace("vls_compute_node","owner_compat_node").replace("sys_menu","owner_compat_menu");
        for(String statement:sql.split(";"))if(!statement.trim().isEmpty())jdbc.execute(statement);
        Map<String,Object> after=jdbc.queryForMap("SELECT * FROM owner_compat_node WHERE id=99");assertNull(after.remove("management_owner_subject"));assertEquals(before,after);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM owner_compat_menu WHERE perms='vls:compute:manage-all'",Integer.class));assertFalse(sql.contains("sys_role_menu"));
        jdbc.execute("DROP TABLE owner_compat_node");jdbc.execute("DROP TABLE owner_compat_menu");
    }
}
