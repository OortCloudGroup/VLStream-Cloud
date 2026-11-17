package com.ruoyi.vlstream.test.vlstream.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.dao.*;
import cn.dev33.satoken.servlet.model.*;
import cn.dev33.satoken.stp.*;
import cn.hutool.json.JSONObject;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class HubPublicationServiceTest {
    SaTokenContext oldContext; SaTokenDao oldDao; StpInterface oldPermissions; SaTokenDaoDefaultImpl memory;
    Set<String> permissions=new HashSet<>();
    MemoryJdbc jdbc; DataManagementService data; HubPublicationArtifacts artifacts; HubPublicationClient client; HubPublicationService service;
    HubPublicationArtifacts.Source source=new HubPublicationArtifacts.Source("name","description","category","object_detection","version-1");
    @BeforeEach void setup() throws Exception {
        oldContext=SaManager.getSaTokenContext();oldDao=SaManager.getSaTokenDao();oldPermissions=SaManager.getStpInterface();
        memory=new SaTokenDaoDefaultImpl();SaManager.setSaTokenDao(memory);SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        MockHttpServletRequest request=new MockHttpServletRequest();
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),new SaResponseForServlet(new MockHttpServletResponse()),new SaStorageForServlet(request));
        permissions.add("vls:menu:data-management");
        SaManager.setStpInterface(new StpInterface(){public List<String> getPermissionList(Object id,String type){return new ArrayList<>(permissions);}public List<String> getRoleList(Object id,String type){return Collections.emptyList();}});
        StpUtil.login("publication-fixture");SaHolder.getStorage().set(LoginHelper.USER_KEY,"local-fixture");
        jdbc=new MemoryJdbc();data=mock(DataManagementService.class);when(data.tenant()).thenReturn("tenant-a");
        artifacts=mock(HubPublicationArtifacts.class);when(artifacts.inspect("dataset",1L)).thenReturn(source);
        when(artifacts.prepare("dataset",1L)).thenAnswer(i->new HubPublicationArtifacts.Prepared(source,Files.createTempDirectory("publication-test-")));
        client=mock(HubPublicationClient.class);
        when(client.receipt(anyString(),anyString(),anyString(),anyString())).thenAnswer(i->receipt("NOT_FOUND"));
        when(client.publish(anyString(),any(),eq(false),anyString(),any(),anyString(),anyString())).thenReturn(new JSONObject().set("uid","remote").set("published_commit",commit()));
        service=new HubPublicationService(jdbc,data,artifacts,client);
    }
    @AfterEach void cleanup() {
        SaTokenContextForThreadLocalStorage.clearBox();SaManager.setSaTokenDao(oldDao);SaManager.setSaTokenContext(oldContext);SaManager.setStpInterface(oldPermissions);memory.endRefreshThread();
    }
    @Test void localStarPreservesReceiptStateTimestampAndNeverCallsRemote() {
        confirmed(); Map<String,Object> before=new LinkedHashMap<>(jdbc.row);
        Map<String,Object> result=service.featured("dataset",1L,true);
        assertEquals(true,result.get("featured"));assertFalse(result.containsKey("destination"));
        for(String key:Arrays.asList("publish_state","remote_uid","published_commit","published_at","publish_key","version_hash","metadata_hash")) assertEquals(before.get(key),jdbc.row.get(key));
        verifyNoInteractions(client);assertFalse(jdbc.starSql.contains("publish_state="));
    }
    @Test void starCannotRefreshActivePublicationLease() {
        jdbc.row.put("publish_state","PUBLISHING");
        assertThrows(ServiceException.class,()->service.featured("dataset",1L,true));verifyNoInteractions(client);
        assertFalse((Boolean)jdbc.row.get("featured"));
    }
    @Test void sameConfirmedVersionDoesNotUploadOrChangeRemoteFeaturedOrPublishedTime() {
        confirmed();jdbc.row.put("featured",true);Object time=jdbc.row.get("published_at");
        service.publish("dataset",1L,null,"user","tenant");
        verifyNoInteractions(client);assertEquals("remote",jdbc.row.get("remote_uid"));assertEquals(commit(),jdbc.row.get("published_commit"));assertEquals(time,jdbc.row.get("published_at"));
    }
    @Test void newPublicationIgnoresLocalStarAndHasStableKey() {
        String expected=key();jdbc.row.put("featured",true);
        service.publish("dataset",1L,null,"user","tenant");
        verify(client).publish(eq("dataset"),any(),eq(false),eq(expected),any(JSONObject.class),eq("user"),eq("tenant"));
        jdbc.row.clear();jdbc.defaults();service.publish("dataset",1L,null,"user","tenant");
        verify(client,times(2)).publish(eq("dataset"),any(),eq(false),eq(expected),any(JSONObject.class),eq("user"),eq("tenant"));
    }
    @Test void legacyUncertainRequestsOnlyLookupOriginalKeyWithoutPreparingOrUploading() throws Exception {
        for(String old:Arrays.asList(legacy(false),legacy(true),key())) {
            jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",old);jdbc.row.put("featured",true);
            when(client.receipt("dataset",old,"user","tenant")).thenReturn(receipt("PUBLISHED"));
            Map<String,Object> result=service.publish("dataset",1L,null,"user","tenant");
            assertEquals("PUBLISHED",result.get("publishState"));assertEquals("remote",result.get("remoteUid"));assertEquals(old,jdbc.row.get("publish_key"));
        }
        verify(artifacts,never()).prepare(anyString(),anyLong());verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void nonFinalOrMissingReceiptNeverUploadsOrChangesUncertainState() throws Exception {
        for(String state:Arrays.asList("NOT_FOUND","PROCESSING","FAILED")) {
            jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
            when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt(state));
            Map<String,Object> result=service.publish("dataset",1L,null,"user","tenant");
            assertEquals(state.equals("FAILED")?"REMOTE_FAILED":"UNCERTAIN",result.get("publishState"));assertEquals(state,result.get("receiptState"));assertEquals(key(),jdbc.row.get("publish_key"));
        }
        verify(artifacts,never()).prepare(anyString(),anyLong());verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void changedSourceCannotBeMistakenForPreviouslyUploadedVersion() throws Exception {
        jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key","old-content-key");
        when(client.receipt("dataset","old-content-key","user","tenant")).thenReturn(receipt("PUBLISHED"));
        Map<String,Object> result=service.reconcile("dataset",1L,null,"user","tenant");
        assertEquals("PUBLISHED",result.get("receiptState"));assertEquals("UNCERTAIN",result.get("publishState"));assertNull(jdbc.row.get("version_hash"));assertNull(jdbc.row.get("remote_uid"));
        verify(artifacts,never()).prepare(anyString(),anyLong());
    }
    @Test void acceptedAsyncCommitIsProcessingAndSubsequentPublishOnlyReconciles() throws Exception {
        when(client.publish(anyString(),any(),eq(false),anyString(),any(),anyString(),anyString())).thenReturn(new JSONObject().set("uid","remote").set("status",4));
        Map<String,Object> result=service.publish("dataset",1L,null,"user","tenant");
        assertEquals("PROCESSING",result.get("publishState"));assertEquals("remote",result.get("remoteUid"));assertNull(result.get("publishedAt"));assertNull(result.get("publishedCommit"));
        assertEquals("version-1",jdbc.row.get("version_hash"));
        when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("PROCESSING"));
        assertEquals("PROCESSING",service.publish("dataset",1L,null,"user","tenant").get("publishState"));
        when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("PUBLISHED"));
        assertEquals("PUBLISHED",service.reconcile("dataset",1L,null,"user","tenant").get("publishState"));
        verify(artifacts,times(1)).prepare("dataset",1L);verify(client,times(1)).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void explicitResumeUsesOriginalKeyOnlyAfterNotFound() throws Exception {
        bindRequest();
        for(String original:Arrays.asList(key(),legacy(false))) {
            jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",original);
            when(client.receipt("dataset",original,"user","tenant")).thenReturn(receipt("NOT_FOUND"));
            service.resume("dataset",1L,null,"user","tenant");
            verify(client).publish(eq("dataset"),any(),eq(false),eq(original),any(JSONObject.class),eq("user"),eq("tenant"));
        }
    }
    @Test void resumeNeverUploadsWhenRemoteHasAnyReceipt() throws Exception {
        bindRequest();
        for(String state:Arrays.asList("PUBLISHED","PROCESSING","FAILED")) {
            jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
            when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt(state));
            assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));
            assertEquals("UNCERTAIN",jdbc.row.get("publish_state"));
        }
        verify(artifacts,never()).prepare(anyString(),anyLong());verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void resumeRejectsChangedSourceLegacyFeaturedAndConcurrentOrActiveClaim() throws Exception {
        bindRequest();
        for(String original:Arrays.asList("changed-content",legacy(true))) {
            jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",original);
            assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));
        }
        verifyNoInteractions(client);
        for(String state:Arrays.asList("UNCERTAIN","PUBLISHING")) {
            jdbc.row.put("publish_state",state);jdbc.row.put("publish_key",key());jdbc.rejectClaim=true;
            when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("NOT_FOUND"));
            assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));
        }
        verify(artifacts,never()).prepare(anyString(),anyLong());verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void resumeRevalidatesFixedVersionAfterPreparingBeforeAnyUpload() throws Exception {
        bindRequest();
        jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
        when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("NOT_FOUND"));
        when(artifacts.prepare("dataset",1L)).thenAnswer(i->new HubPublicationArtifacts.Prepared(new HubPublicationArtifacts.Source("name","description","category","object_detection","changed-version"),Files.createTempDirectory("publication-test-")));
        assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));
        assertEquals(key(),jdbc.row.get("publish_key"));assertEquals("UNCERTAIN",jdbc.row.get("publish_state"));
        verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void oldUnboundNotFoundCannotResumeButPositiveReceiptBindsVerifiedIdentity() throws Exception {
        jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
        assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));verifyNoInteractions(client);
        when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("PUBLISHED"));
        service.reconcile("dataset",1L,null,"user","tenant");
        assertEquals("platform-user",jdbc.row.get("request_remote_user_id"));assertEquals("platform-tenant",jdbc.row.get("request_remote_tenant_id"));
        verify(artifacts,never()).prepare(anyString(),anyLong());
    }
    @Test void changedPlatformOwnerOrTenantNeverReconcilesOrResumesExistingRequest() throws Exception {
        bindRequest();jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
        for(String field:Arrays.asList("user_id","tenant_id")) {
            JSONObject response=receipt("NOT_FOUND");response.getJSONObject("principal").set(field,"different");
            when(client.receipt("dataset",key(),"user","tenant")).thenReturn(response);
            assertThrows(ServiceException.class,()->service.reconcile("dataset",1L,null,"user","tenant"));
            assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));
        }
        assertEquals("UNCERTAIN",jdbc.row.get("publish_state"));verify(artifacts,never()).prepare(anyString(),anyLong());
        verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    @Test void requestIdentityAndMetadataArePersistedBeforePreparingFilesWithoutTokens() throws Exception {
        when(artifacts.prepare("dataset",1L)).thenAnswer(i->{
            assertEquals("platform-user",jdbc.row.get("request_remote_user_id"));assertEquals("platform-tenant",jdbc.row.get("request_remote_tenant_id"));
            assertEquals("version-1",jdbc.row.get("request_version_hash"));assertEquals(key(),jdbc.row.get("publish_key"));
            JSONObject frozen=cn.hutool.json.JSONUtil.parseObj((String)jdbc.row.get("request_metadata_json"));
            assertEquals(new HashSet<>(Arrays.asList("name","description","category")),frozen.keySet());
            return new HubPublicationArtifacts.Prepared(source,Files.createTempDirectory("publication-test-"));
        });
        service.publish("dataset",1L,null,"user","tenant");
    }
    @Test void persistedSnapshotRecoversOriginalVersionEvenIfCurrentSourceChanged() throws Exception {
        bindRequest();jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
        when(artifacts.inspect("dataset",1L)).thenReturn(new HubPublicationArtifacts.Source("renamed","changed","category","object_detection","version-2"));
        when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("PUBLISHED"));
        Map<String,Object> result=service.reconcile("dataset",1L,null,"user","tenant");
        assertEquals("CHANGED",result.get("publishState"));assertEquals("version-1",jdbc.row.get("version_hash"));
        verify(artifacts,never()).prepare(anyString(),anyLong());
    }
    @Test void resumeAllowedRequiresIdentitySnapshotUnchangedSourceAndNoActiveUpload() {
        jdbc.row.put("publish_state","UNCERTAIN");jdbc.row.put("publish_key",key());
        assertEquals(false,service.reconcile("dataset",1L,null,"user","tenant").get("resumeAllowed"));
        bindRequest();assertEquals(true,service.reconcile("dataset",1L,null,"user","tenant").get("resumeAllowed"));
        jdbc.row.put("publish_state","PUBLISHING");assertEquals(false,service.reconcile("dataset",1L,null,"user","tenant").get("resumeAllowed"));
        jdbc.stale=true;assertEquals(true,service.reconcile("dataset",1L,null,"user","tenant").get("resumeAllowed"));
        jdbc.row.put("request_version_hash","changed");assertEquals(false,service.reconcile("dataset",1L,null,"user","tenant").get("resumeAllowed"));
    }
    @Test void remoteFailureIsTerminalAndPublishCannotAutomaticallyRetryIt() throws Exception {
        bindRequest();jdbc.row.put("publish_state","PROCESSING");jdbc.row.put("publish_key",key());
        when(client.receipt("dataset",key(),"user","tenant")).thenReturn(receipt("FAILED"));
        assertEquals("REMOTE_FAILED",service.reconcile("dataset",1L,null,"user","tenant").get("publishState"));
        assertEquals("REMOTE_FAILED",service.publish("dataset",1L,null,"user","tenant").get("publishState"));
        assertThrows(ServiceException.class,()->service.resume("dataset",1L,null,"user","tenant"));
        verify(artifacts,never()).prepare(anyString(),anyLong());verify(client,never()).publish(anyString(),any(),anyBoolean(),anyString(),any(),anyString(),anyString());
    }
    private void bindRequest(){jdbc.row.put("request_remote_user_id","platform-user");jdbc.row.put("request_remote_tenant_id","platform-tenant");jdbc.row.put("request_version_hash","version-1");jdbc.row.put("request_metadata_json",new JSONObject().set("name","name").set("description","description").set("category","category").toString());}
    private JSONObject receipt(String state){return new JSONObject().set("state",state).set("principal",new JSONObject().set("user_id","platform-user").set("tenant_id","platform-tenant")).set("resource",new JSONObject().set("uid","remote").set("status",1).set("published_commit",commit()));}
    @Test void sourceAccessDeniedBeforeAnyPublicationMutation() {
        when(artifacts.inspect("dataset",1L)).thenThrow(new ServiceException("not owner"));
        assertThrows(ServiceException.class,()->service.featured("dataset",1L,true));
        assertTrue(jdbc.statements.isEmpty());verifyNoInteractions(client);
    }
    @Test void missingMenuPermissionNeverReadsOrMutatesSource() {
        permissions.clear();assertThrows(RuntimeException.class,()->service.status("dataset",1L));
        verifyNoInteractions(artifacts,client);assertTrue(jdbc.statements.isEmpty());
    }
    @Test void explicitAuthenticationRejectionRestoresDraftOrPublishedAndOriginalReceipt() {
        for(String state:Arrays.asList("DRAFT","PUBLISHED","UNCERTAIN","PUBLISHING")) {
            jdbc.row.clear();jdbc.defaults();
            if(state.equals("PUBLISHED")) {confirmed();jdbc.row.put("version_hash","older-version");}
            jdbc.row.put("publish_state",state);
            if(state.equals("UNCERTAIN") || state.equals("PUBLISHING")) jdbc.row.put("publish_key",key());
            Object originalKey=jdbc.row.get("publish_key"), originalTime=jdbc.row.get("published_at"), originalUid=jdbc.row.get("remote_uid");
            doThrow(HubPublicationClient.authenticationRejected()).when(client).publish(anyString(),any(),eq(false),anyString(),any(),anyString(),anyString());
            doThrow(HubPublicationClient.authenticationRejected()).when(client).receipt(anyString(),anyString(),anyString(),anyString());
            ServiceException ex=assertThrows(ServiceException.class,()->service.publish("dataset",1L,null,"user","tenant"));
            assertEquals(Integer.valueOf(4401),ex.getCode());
            assertEquals(state,jdbc.row.get("publish_state"));
            assertEquals(originalKey,jdbc.row.get("publish_key"));assertEquals(originalTime,jdbc.row.get("published_at"));assertEquals(originalUid,jdbc.row.get("remote_uid"));
        }
    }
    @Test void uploadAuthenticationRejectionRestoresPriorIdentitySnapshotAndKey() {
        for(String state:Arrays.asList("DRAFT","PUBLISHED")) {
            jdbc.row.clear();jdbc.defaults();
            if(state.equals("PUBLISHED")){confirmed();jdbc.row.put("version_hash","older-version");}
            Object originalKey=jdbc.row.get("publish_key");
            doThrow(HubPublicationClient.authenticationRejected()).when(client).publish(anyString(),any(),eq(false),anyString(),any(),anyString(),anyString());
            ServiceException ex=assertThrows(ServiceException.class,()->service.publish("dataset",1L,null,"user","tenant"));
            assertEquals(Integer.valueOf(4401),ex.getCode());assertEquals(state,jdbc.row.get("publish_state"));assertEquals(originalKey,jdbc.row.get("publish_key"));
            assertNull(jdbc.row.get("request_remote_user_id"));assertNull(jdbc.row.get("request_metadata_json"));
        }
    }
    @Test void unknownRemoteFailuresStillBecomeUncertain() {
        for(RuntimeException failure:Arrays.asList(new ServiceException("upstream500"),new RuntimeException("timeout"))) {
            jdbc.row.clear();jdbc.defaults();
            doThrow(failure).when(client).publish(anyString(),any(),eq(false),anyString(),any(),anyString(),anyString());
            assertThrows(ServiceException.class,()->service.publish("dataset",1L,null,"user","tenant"));
            assertEquals("UNCERTAIN",jdbc.row.get("publish_state"));assertEquals(key(),jdbc.row.get("publish_key"));assertEquals("platform-user",jdbc.row.get("request_remote_user_id"));
        }
    }
    private static String commit(){return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";}
    private String key(){return HubPublicationService.publicationKey("tenant-a","dataset",1L,"version-1","name","description","category");}
    private String legacy(boolean flag){return HubPublicationService.legacyKey("tenant-a","dataset",1L,"version-1",flag,"name","description","category");}
    private void confirmed(){jdbc.row.put("publish_state","PUBLISHED");jdbc.row.put("remote_uid","remote");jdbc.row.put("published_commit",commit());jdbc.row.put("published_at","original-time");jdbc.row.put("version_hash","version-1");jdbc.row.put("metadata_hash",HubPublicationService.hash("name\ndescription\ncategory"));jdbc.row.put("publish_key",legacy(true));}
    static class MemoryJdbc extends JdbcTemplate {
        final Map<String,Object> row=new LinkedHashMap<>();final List<String> statements=new ArrayList<>();String starSql;boolean rejectClaim;boolean stale;
        MemoryJdbc(){defaults();}void defaults(){row.put("featured",false);row.put("publish_state","DRAFT");}
        @Override public List<Map<String,Object>> queryForList(String sql,Object... args){assertEquals("tenant-a",args[0]);assertEquals("dataset",args[1]);assertEquals(1L,args[2]);return Collections.singletonList(new LinkedHashMap<>(row));}
        @Override public <T> T queryForObject(String sql,Class<T> type,Object... a){return type.cast(stale?Integer.valueOf(1):Integer.valueOf(0));}
        @Override public int update(String sql,Object... a) {
            statements.add(sql);
            if(sql.startsWith("INSERT"))return 0;
            if(sql.contains("SET publish_state='REMOTE_FAILED'")){row.put("publish_state","REMOTE_FAILED");row.put("error_message",a[0]);return 1;}
            if(sql.contains("SET featured=")){starSql=sql;if("PUBLISHING".equals(row.get("publish_state")))return 0;row.put("featured",a[0]);return 1;}
            if(sql.contains("SET publish_state='PUBLISHING'")){if(rejectClaim)return 0;row.put("publish_state","PUBLISHING");row.put("publish_key",a[0]);row.put("request_remote_user_id",a[1]);row.put("request_remote_tenant_id",a[2]);row.put("request_version_hash",a[3]);row.put("request_metadata_json",a[4]);return 1;}
            if(sql.contains("SET publish_key=")){row.put("publish_key",a[0]);if(sql.contains("request_remote_user_id=?")){row.put("request_remote_user_id",a[1]);row.put("request_remote_tenant_id",a[2]);row.put("request_version_hash",a[3]);row.put("request_metadata_json",a[4]);}return 1;}
            if(sql.contains("SET publish_state='PROCESSING'")){row.put("publish_state","PROCESSING");row.put("version_hash",a[0]);row.put("metadata_hash",a[1]);row.put("remote_uid",a[2]);row.put("published_commit",null);return 1;}
            if(sql.contains("version_hash=?")){row.put("publish_state","PUBLISHED");row.put("version_hash",a[0]);row.put("metadata_hash",a[1]);row.put("remote_uid",a[2]);row.put("published_commit",a[3]);row.put("published_at","new-time");if(sql.contains("request_remote_user_id=?")){row.put("request_remote_user_id",a[4]);row.put("request_remote_tenant_id",a[5]);row.put("request_version_hash",a[6]);row.put("request_metadata_json",a[7]);}return 1;}
            if(sql.contains("SET publish_state='PUBLISHED'")){row.put("publish_state","PUBLISHED");return 1;}
            if(sql.contains("SET publish_state=?")){row.put("publish_state",a[0]);return 1;}
            throw new AssertionError("Unexpected update: "+sql);
        }
    }
}
