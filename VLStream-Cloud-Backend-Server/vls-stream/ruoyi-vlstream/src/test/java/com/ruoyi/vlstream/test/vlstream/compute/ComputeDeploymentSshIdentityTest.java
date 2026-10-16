package com.ruoyi.vlstream.test.vlstream.compute;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeDeploymentSshIdentityTest {
    @Test void onlyApprovedEnvironmentAssetAndFixedLocalReferenceMayUseDeploymentKey() throws Exception {
        Fixture fixture = new Fixture();
        fixture.properties.setEnabled(true);
        fixture.properties.setEnvironmentId("localhost");
        fixture.shared.setEnvironmentId("localhost");
        fixture.reference.setPrivateKeyFile(ComputeDeploymentSshIdentity.LOCAL_FILE);
        fixture.reference.setPublicKeyFingerprint(ComputeDeploymentSshIdentity.LOCAL_FINGERPRINT);
        fixture.identity.requireConfigured(fixture.node);

        fixture.properties.setEnvironmentId("vm");
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.properties.setEnvironmentId("localhost");
        fixture.reference.setPrivateKeyFile("C:/temp/other-key");
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.reference.setPrivateKeyFile(ComputeDeploymentSshIdentity.LOCAL_FILE);
        fixture.reference.setPublicKeyFingerprint("SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.reference.setPublicKeyFingerprint(ComputeDeploymentSshIdentity.LOCAL_FINGERPRINT);
        fixture.asset.setCompanyPool(false);
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.asset.setCompanyPool(true);
        fixture.node.setUsername("another-user");
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
    }

    @Test void disabledMissingAndDuplicateReferencesFailBeforeAnyCredentialRead() throws Exception {
        Fixture fixture = new Fixture();
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.properties.setEnabled(true);
        fixture.properties.setEnvironmentId("vm"); fixture.shared.setEnvironmentId("vm");
        fixture.reference.setPrivateKeyFile(ComputeDeploymentSshIdentity.VM_FILE);
        fixture.reference.setPublicKeyFingerprint(ComputeDeploymentSshIdentity.VM_FINGERPRINT);
        fixture.identity.requireConfigured(fixture.node);
        fixture.reference.setPublicKeyFingerprint(ComputeDeploymentSshIdentity.LOCAL_FINGERPRINT);
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.reference.setPublicKeyFingerprint(ComputeDeploymentSshIdentity.VM_FINGERPRINT);
        fixture.properties.setIdentities(Arrays.asList(fixture.reference, fixture.reference));
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
        fixture.properties.setIdentities(Collections.emptyList());
        assertThrows(IOException.class, () -> fixture.identity.requireConfigured(fixture.node));
    }

    @Test void keyModeNeverDecryptsPasswordWhenDeploymentConfigurationIsMissing() {
        Fixture fixture = new Fixture();
        ComputeCredentialCipher cipher = mock(ComputeCredentialCipher.class);
        ComputeSsh ssh = new ComputeSsh(cipher, fixture.policy);
        ReflectionTestUtils.setField(ssh, "deploymentIdentity", fixture.identity);
        assertThrows(IOException.class, () -> ssh.open(fixture.node));
        verifyNoInteractions(cipher);
    }

    private static final class Fixture {
        final ComputeSshIdentityProperties properties = new ComputeSshIdentityProperties();
        final ComputeSharedQueueProperties shared = new ComputeSharedQueueProperties();
        final ComputeTrustedSelfAssetsProperties trust = new ComputeTrustedSelfAssetsProperties();
        final ComputeTrustedSelfAssetsProperties.Asset asset = new ComputeTrustedSelfAssetsProperties.Asset();
        final ComputeSshIdentityProperties.Identity reference = new ComputeSshIdentityProperties.Identity();
        final ComputeNode node = new ComputeNode();
        final ComputeTrustedSelfPolicy policy;
        final ComputeDeploymentSshIdentity identity;
        Fixture() {
            node.setAuthMode(ComputeDeploymentSshIdentity.KEY); node.setTenantId("asset-tenant");
            node.setProvider("self"); node.setHost("192.168.88.173"); node.setPort(22); node.setUsername("oort");
            asset.setAssetId("asset-173"); asset.setTenantId("asset-tenant");
            asset.setHost("192.168.88.173"); asset.setPort(22); asset.setUsername("oort"); asset.setCompanyPool(true);
            // A fixed public ED25519 wire blob. No test or production private credential is read.
            asset.setHostKey("AAAAC3NzaC1lZDI1NTE5AAAAIFPDQ1tTAtxQA4RaY80xYoaNyacC5nyWSsw0LSO3E9EG");
            trust.setAssets(Collections.singletonList(asset)); policy = new ComputeTrustedSelfPolicy(trust);
            reference.setAssetId("asset-173"); properties.setIdentities(Collections.singletonList(reference));
            identity = new ComputeDeploymentSshIdentity(properties, policy, shared);
        }
    }
}
