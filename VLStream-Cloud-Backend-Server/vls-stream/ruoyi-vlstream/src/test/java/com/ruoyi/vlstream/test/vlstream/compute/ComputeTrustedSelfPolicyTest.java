package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.data.PublicDatasetDownloader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeTrustedSelfPolicyTest {
    @Test void ed25519HostVerificationWorksOnTheActualJava8Classpath() throws Exception {
        // RFC8032 section7.1 public verification vector; no private key or credential is generated.
        byte[] publicKey = hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a");
        byte[] signature = hex("e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b");
        com.jcraft.jsch.SignatureEdDSA verifier = (com.jcraft.jsch.SignatureEdDSA)
            Class.forName(com.jcraft.jsch.JSch.getConfig("ssh-ed25519")).getDeclaredConstructor().newInstance();
        verifier.init(); verifier.setPubKey(publicKey); verifier.update(new byte[0]);
        assertTrue(verifier.verify(signatureBlob(signature)));
        verifier.init(); verifier.setPubKey(publicKey); verifier.update(new byte[]{1});
        assertFalse(verifier.verify(signatureBlob(signature)));
    }
    @Test void emptyDeploymentTrustRejectsApprovedAddressesBeforeCredentialsOrNetwork() {
        ComputeCredentialCipher cipher = mock(ComputeCredentialCipher.class);
        ComputeSsh ssh = new ComputeSsh(cipher, new ComputeTrustedSelfPolicy(new ComputeTrustedSelfAssetsProperties()));
        assertThrows(IOException.class, () -> ssh.open(node("192.168.88.173")));
        assertThrows(IOException.class, () -> ssh.open(node("192.168.88.99")));
        verifyNoInteractions(cipher);
    }

    @Test void matchesOnlyCompleteApprovedAssetAndRequiresStrictPinnedAlgorithm() throws Exception {
        Fixture fixture = new Fixture("192.168.88.173");
        ComputeTrustedSelfPolicy.Target target = fixture.policy.resolve(fixture.node);
        assertEquals("192.168.88.173", target.addresses()[0].getHostAddress());
        assertEquals(fixture.asset.getHostKey(), target.hostKey());
        assertEquals("ssh-ed25519", target.requiredAlgorithm());
        fixture.node.setHostKey(fixture.asset.getHostKey());
        assertEquals(target.hostKey(), fixture.policy.resolve(fixture.node).hostKey());
    }

    @Test void legacyApprovedHostsRequireTheSameCompletePublicPinAndExactEndpoint() throws Exception {
        Fixture fixture = new Fixture("192.168.88.173");
        ComputeTrustedSelfPolicy.Target target = fixture.policy.resolveLegacy("192.168.88.173", 22, "oort");
        assertEquals(fixture.asset.getHostKey(), target.hostKey());
        assertEquals("ssh-ed25519", target.requiredAlgorithm());
        assertThrows(IOException.class, () -> fixture.policy.resolveLegacy("192.168.88.173", 2222, "oort"));
        assertThrows(IOException.class, () -> fixture.policy.resolveLegacy("192.168.88.173", 22, "root"));
        fixture.asset.setHostKey(null);
        assertThrows(IOException.class, () -> fixture.policy.resolveLegacy("192.168.88.173", 22, "oort"));
    }

    @Test void refusesTenantProviderUserAndPortChanges() {
        Fixture fixture = new Fixture("192.168.88.99");
        fixture.node.setTenantId("other-tenant"); assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.node.setTenantId("asset-tenant"); fixture.node.setProvider("autodl"); assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.node.setProvider("self"); fixture.node.setUsername("root"); assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.node.setUsername("oort"); fixture.node.setPort(2222); assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
    }

    @Test void neitherConfiguredExceptionNorSelfProviderAllowsOtherInternalAddresses() {
        Fixture fixture = new Fixture("192.168.88.173");
        for (String host : Arrays.asList("192.168.88.100", "192.168.088.173", "127.0.0.1", "169.254.169.254", "10.0.0.1", "::ffff:192.168.88.173")) {
            fixture.node.setHost(host);
            assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node), host);
        }
        fixture.asset.setHost("192.168.88.100"); fixture.node.setHost("192.168.88.173");
        assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
    }

    @Test void publicDatasetPolicyNeverUsesComputeExceptions() {
        new Fixture("192.168.88.173");
        assertThrows(IOException.class, () -> PublicDatasetDownloader.publicAddresses("192.168.88.173"));
        assertThrows(IOException.class, () -> PublicDatasetDownloader.publicAddresses("192.168.88.99"));
    }

    @Test void missingMalformedOrChangedKeyNeverOverridesPersistedIdentity() {
        Fixture fixture = new Fixture("192.168.88.173");
        fixture.node.setHostKey(hostKey((byte) 2));
        assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.node.setHostKey(null); fixture.asset.setHostKey(null);
        assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.asset.setHostKey("not-base64"); assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.asset.setHostKey(Base64.getEncoder().encodeToString(new byte[]{0, 0, 0, (byte) 255}));
        assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
    }

    @Test void duplicateOrIncompleteDeploymentIdentityFailsClosed() {
        Fixture fixture = new Fixture("192.168.88.99");
        fixture.properties.setAssets(Arrays.asList(fixture.asset, fixture.asset));
        assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
        fixture.properties.setAssets(Collections.singletonList(fixture.asset)); fixture.asset.setAssetId("");
        assertThrows(IOException.class, () -> fixture.policy.resolve(fixture.node));
    }

    @Test void publicNodeKeepsExistingAddressAndPinRules() throws Exception {
        Fixture fixture = new Fixture("192.168.88.173");
        fixture.node.setHost("8.8.8.8"); fixture.node.setProvider("autodl"); fixture.node.setHostKey(hostKey((byte) 2));
        ComputeTrustedSelfPolicy.Target target = fixture.policy.resolve(fixture.node);
        assertEquals("8.8.8.8", target.addresses()[0].getHostAddress());
        assertEquals(fixture.node.getHostKey(), target.hostKey()); assertNull(target.requiredAlgorithm());
    }

    private static ComputeNode node(String host) {
        ComputeNode node = new ComputeNode(); node.setHost(host); node.setPort(22); node.setUsername("oort");
        node.setProvider("self"); node.setTenantId("asset-tenant"); return node;
    }
    private static byte[] hex(String value) {
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(value.substring(2 * i, 2 * i + 2), 16);
        return bytes;
    }
    private static byte[] signatureBlob(byte[] signature) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes);
        byte[] algorithm = "ssh-ed25519".getBytes(StandardCharsets.US_ASCII);
        data.writeInt(algorithm.length); data.write(algorithm); data.writeInt(signature.length); data.write(signature);
        return bytes.toByteArray();
    }
    /** A fixed public wire-format fixture, never a generated/login private credential. */
    private static String hostKey(byte value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream data = new DataOutputStream(bytes);
            byte[] algorithm = "ssh-ed25519".getBytes(StandardCharsets.US_ASCII); data.writeInt(algorithm.length); data.write(algorithm);
            byte[] publicBytes = new byte[32]; Arrays.fill(publicBytes, value); data.writeInt(publicBytes.length); data.write(publicBytes);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException impossible) { throw new AssertionError(impossible); }
    }
    private static class Fixture {
        final ComputeTrustedSelfAssetsProperties properties = new ComputeTrustedSelfAssetsProperties();
        final ComputeTrustedSelfAssetsProperties.Asset asset = new ComputeTrustedSelfAssetsProperties.Asset();
        final ComputeNode node;
        final ComputeTrustedSelfPolicy policy;
        Fixture(String host) {
            node = node(host); asset.setHost(host); asset.setTenantId("asset-tenant"); asset.setUsername("oort");
            asset.setAssetId("company-asset"); asset.setHostKey(hostKey((byte) 1));
            properties.setAssets(Collections.singletonList(asset)); policy = new ComputeTrustedSelfPolicy(properties);
        }
    }
}
