package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Calls the real settings and ownership methods; only persistence/platform I/O is synthetic. */
@Tag("dev")
class ComputeProviderSettingsOwnershipTest {
    JdbcTemplate jdbc;
    ComputeNodeService nodes;
    ComputeCredentialCipher cipher;
    ComputeProviderSettingsService settings;
    ComputeNode source;
    ComputeNode target;
    AtomicBoolean administrator=new AtomicBoolean();
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId("tenant-a");jdbc=mock(JdbcTemplate.class);nodes=mock(ComputeNodeService.class);cipher=mock(ComputeCredentialCipher.class);
        ComputePlatformAuthorization authorization=mock(ComputePlatformAuthorization.class);
        ComputePlatformAuthorization.Context identity=new ComputePlatformAuthorization.Context();identity.setAuthorized(true);identity.setPlatformUserId("alice");identity.setVlsUserId("vls-alice");identity.setVlsTenantId("tenant-a");
        when(authorization.requireIdentity()).thenReturn(identity);
        ComputeNodeOwnershipService ownership=new ComputeNodeOwnershipService(authorization){@Override public boolean administrator(){return administrator.get();}};
        doAnswer(call->{ownership.requireManagement(call.getArgument(0));return null;}).when(nodes).requireManagement(any(ComputeNode.class));
        source=node(1L,"tenant-a","alice","vendor-a");target=node(2L,"tenant-a","bob","vendor-b");when(nodes.lockServer(1L)).thenReturn(source);
        when(cipher.encrypt(anyString())).thenReturn("encrypted-synthetic-only");settings=new ComputeProviderSettingsService(jdbc,nodes,cipher,new ObjectMapper());
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    ComputeNode node(Long id,String tenant,String owner,String instance) {
        ComputeNode node=new ComputeNode();node.setId(id);node.setTenantId(tenant);node.setProvider("autodl");node.setProviderProduct("AUTODL_PRO");node.setProviderInstanceId(instance);
        node.setManagementOwnerSubject(owner==null?null:ModelArtifactObjectStore.hash("trusted-platform-user\n"+owner));return node;
    }
    ComputeRequests.ProviderSettings request(String mode) {
        ComputeRequests.ProviderSettings request=new ComputeRequests.ProviderSettings();request.setControlMode(mode);request.setProviderProduct("AUTODL_PRO");request.setProviderInstanceId("vendor-b");request.setToken("synthetic-token-only");return request;
    }
    @SuppressWarnings("unchecked") void matches(List<ComputeNode> targets){when(jdbc.query(anyString(),any(RowMapper.class),eq("autodl"),eq("vendor-b"))).thenReturn(targets);}
    long writes(){return mockingDetails(jdbc).getInvocations().stream().filter(call->call.getMethod().getName().equals("update")).count();}
    @Test void apiRebindToOtherPrivateOwnerIsRejectedBeforeIdleEncryptionOrWrite() {
        matches(Collections.singletonList(target));assertThrows(ServiceException.class,()->settings.save(1L,request("API")));
        verify(nodes).requireManagement(target);verify(nodes,never()).assertIdle(anyLong());verifyNoInteractions(cipher);assertEquals(0,writes());
    }
    @Test void manualVendorAliasHasTheSameOwnerBoundary() {
        matches(Collections.singletonList(target));assertThrows(ServiceException.class,()->settings.save(1L,request("MANUAL")));
        verify(nodes).requireManagement(target);verify(nodes,never()).assertIdle(anyLong());verifyNoInteractions(cipher);assertEquals(0,writes());
    }
    @Test void allAliasesAreAuthorizedBeforeAnyTargetActivityCheck() {
        ComputeNode owned=node(3L,"tenant-a","alice","vendor-b");matches(Arrays.asList(owned,target));
        assertThrows(ServiceException.class,()->settings.save(1L,request("API")));verify(nodes).requireManagement(owned);verify(nodes).requireManagement(target);
        verify(nodes,never()).assertIdle(anyLong());verifyNoInteractions(cipher);assertEquals(0,writes());
    }
    @Test void personalOwnerCannotAdoptUnknownHistoricalTarget() {
        target.setManagementOwnerSubject(null);matches(Collections.singletonList(target));assertThrows(ServiceException.class,()->settings.save(1L,request("API")));assertEquals(0,writes());verifyNoInteractions(cipher);
    }
    @Test void ownPrivateAliasesRetainLegitimateConfigurationPath() {
        target.setManagementOwnerSubject(source.getManagementOwnerSubject());matches(Collections.singletonList(target));settings.save(1L,request("API"));
        verify(nodes).requireManagement(target);verify(nodes).assertIdle(2L);verify(nodes).assertIdle(1L);verify(cipher).encrypt(anyString());assertEquals(1,writes());
    }
    @Test void explicitAdministratorMayManageSameTenantTargetWithoutAssigningAnOwner() {
        administrator.set(true);target.setManagementOwnerSubject(null);matches(Collections.singletonList(target));settings.save(1L,request("API"));
        assertEquals(1,writes());assertNull(target.getManagementOwnerSubject());verify(nodes).requireManagement(target);verify(nodes).assertIdle(2L);
    }
    @Test void administratorStillCannotBindAnotherTenantsVendorInstance() {
        administrator.set(true);target.setTenantId("tenant-b");matches(Collections.singletonList(target));assertThrows(ServiceException.class,()->settings.save(1L,request("API")));
        verify(nodes,never()).assertIdle(anyLong());verifyNoInteractions(cipher);assertEquals(0,writes());
    }
    @Test void publicIdentityCannotBeAdoptedThroughPrivateVendorConfiguration() {
        administrator.set(true);target.setPublicInstanceKey("synthetic-public-identity");matches(Collections.singletonList(target));assertThrows(ServiceException.class,()->settings.save(1L,request("API")));
        verify(nodes).requireManagement(target);verify(nodes,never()).assertIdle(anyLong());assertEquals(0,writes());
    }
}
