package com.ruoyi.vlstream.test.vlstream.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeTrustedSelfAssetsProperties;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeTrustedSelfPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class SSHServiceTrustedPinTest {
    @Test void bothApprovedLegacyEndpointsUseStrictExactHostPinsWithoutConnecting() throws Exception {
        ComputeTrustedSelfAssetsProperties properties = new ComputeTrustedSelfAssetsProperties();
        properties.setAssets(Arrays.asList(asset("192.168.88.173",
            "AAAAC3NzaC1lZDI1NTE5AAAAIFPDQ1tTAtxQA4RaY80xYoaNyacC5nyWSsw0LSO3E9EG"),
            asset("192.168.88.99", "AAAAC3NzaC1lZDI1NTE5AAAAIDKYrwg2aRuJOPykxcwpdIh3RkH0fHbd1vA0rNxN76sp")));
        SSHService service = new SSHService();
        ReflectionTestUtils.setField(service, "trustedSelfPolicy", new ComputeTrustedSelfPolicy(properties));
        for (String host : Arrays.asList("192.168.88.173", "192.168.88.99")) {
            JSch client = new JSch();
            Session session = service.session(client, host, 22, "oort", "test-only");
            assertEquals("yes", session.getConfig("StrictHostKeyChecking"));
            assertEquals("ssh-ed25519", session.getConfig("server_host_key"));
            assertEquals(host + ":22", session.getHostKeyAlias());
            assertEquals(1, client.getHostKeyRepository().getHostKey(host + ":22", "ssh-ed25519").length);
            assertFalse(session.isConnected());
        }
        assertThrows(Exception.class, () -> service.session(new JSch(), "192.168.88.173", 2222, "oort", "test-only"));
        assertThrows(Exception.class, () -> service.session(new JSch(), "192.168.88.99", 22, "root", "test-only"));
        ReflectionTestUtils.setField(service, "trustedSelfPolicy", null);
        assertThrows(Exception.class, () -> service.session(new JSch(), "192.168.88.173", 22, "oort", "test-only"));
    }

    private static ComputeTrustedSelfAssetsProperties.Asset asset(String host, String pin) {
        ComputeTrustedSelfAssetsProperties.Asset asset = new ComputeTrustedSelfAssetsProperties.Asset();
        asset.setAssetId(host); asset.setTenantId("asset-tenant"); asset.setHost(host);
        asset.setPort(22); asset.setUsername("oort"); asset.setHostKey(pin); asset.setCompanyPool(true);
        return asset;
    }
}
