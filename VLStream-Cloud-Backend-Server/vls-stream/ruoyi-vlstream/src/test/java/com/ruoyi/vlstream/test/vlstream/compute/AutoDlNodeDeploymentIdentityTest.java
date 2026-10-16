package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class AutoDlNodeDeploymentIdentityTest {
    private JdbcTemplate jdbc;
    private ComputeSsh ssh;
    private ComputeCredentialCipher cipher;
    private ComputeNodeOwnershipService ownership;
    private ComputeNodeService service;
    private ComputeRequests.Node request;
    private ComputeApprovedAutoDlSshIdentityTest.Fixture fixture;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId("tenant-a");
        jdbc = mock(JdbcTemplate.class);
        ssh = mock(ComputeSsh.class);
        cipher = mock(ComputeCredentialCipher.class);
        ownership = mock(ComputeNodeOwnershipService.class);
        when(ownership.requireViewer()).thenReturn("owner-a"); when(ownership.administrator()).thenReturn(true);
        service = spy(new ComputeNodeService(jdbc, cipher, ssh, new ObjectMapper()));
        ReflectionTestUtils.setField(service, "ownership", ownership);
        doReturn(new ComputeNode()).when(service).get(anyLong());
        request = new ComputeRequests.Node(); request.setName("offline fixture");
        request.setHost("EXAMPLE.TEST"); request.setPort(33138); request.setAuthMode("DEPLOYMENT_KEY");
        fixture = new ComputeApprovedAutoDlSshIdentityTest.Fixture();
        doAnswer(call -> { fixture.identity.requireConfigured(call.getArgument(0)); return null; }).when(ssh).requireDeploymentIdentity(any());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void trustedServerContextIsUsedAndNoCredentialIsEncrypted() throws Exception {
        service.save(null, request);
        ArgumentCaptor<ComputeNode> captured = ArgumentCaptor.forClass(ComputeNode.class);
        verify(ssh).requireDeploymentIdentity(captured.capture());
        ComputeNode node = captured.getValue();
        assertEquals("tenant-a", node.getTenantId()); assertEquals("owner-a", node.getManagementOwnerSubject());
        assertEquals("PRIVATE", node.getResourceScope()); assertEquals("EXTERNAL", node.getBillingType());
        assertEquals("INSTANCE_EXCLUSIVE", node.getSchedulingMode()); assertNull(node.getPublicInstanceKey());
        verifyNoInteractions(cipher);
        verify(jdbc).update(contains("INSERT INTO vls_compute_node"), any(), eq("tenant-a"), eq("offline fixture"), eq("example.test"), eq(33138), eq("root"), eq("DEPLOYMENT_KEY"), isNull(), any(), any(), any(), any(), eq("owner-a"));
    }

    @Test void publicEndpointAliasCannotUsePrivateAutoDlBinding() throws Exception {
        ComputeNode alias = new ComputeNode(); alias.setTenantId("tenant-a"); alias.setProvider("autodl"); alias.setPublicInstanceKey("public-instance");
        jdbc = mock(JdbcTemplate.class, call -> {
            if (call.getMethod().getName().equals("query")) return call.getArgument(0).toString().contains("public_instance_key IS NOT NULL") ? Collections.singletonList(alias) : Collections.emptyList();
            return RETURNS_DEFAULTS.answer(call);
        });
        service = new ComputeNodeService(jdbc, cipher, ssh, new ObjectMapper());
        ReflectionTestUtils.setField(service, "ownership", ownership);
        ReflectionTestUtils.setField(service, "publicInventory", mock(ComputePublicInventoryService.class));
        assertThrows(ServiceException.class, () -> service.save(null, request));
        assertFalse(mockingDetails(jdbc).getInvocations().stream().anyMatch(call -> call.getMethod().getName().equals("update")));
        verifyNoInteractions(cipher);
    }

    @Test void disabledBindingBlocksRegistrationBeforeDatabaseWrites() throws Exception {
        fixture.properties.setEnabled(false);
        assertThrows(ServiceException.class, () -> service.save(null, request));
        assertFalse(mockingDetails(jdbc).getInvocations().stream().anyMatch(call -> call.getMethod().getName().equals("update")));
        verifyNoInteractions(cipher);
    }

    @Test void nonAdministratorAndPasswordPayloadCannotSelectDeploymentIdentity() {
        when(ownership.administrator()).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.save(null, request));
        when(ownership.administrator()).thenReturn(true); request.setPassword("test-only-payload");
        assertThrows(ServiceException.class, () -> service.save(null, request));
        verifyNoInteractions(ssh, cipher);
    }
}
