package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.JSch;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeApprovedAutoDlSshIdentityTest {
    @Test void springStartsWithAnEmptyDisabledBindingAndRejectsAutoDlUse() {
        new ApplicationContextRunner().withUserConfiguration(DefaultBindingConfiguration.class).run(context -> {
            assertNull(context.getStartupFailure());
            ComputeApprovedAutoDlSshProperties properties = context.getBean(ComputeApprovedAutoDlSshProperties.class);
            assertFalse(properties.isEnabled()); assertTrue(properties.getBindings().isEmpty());
            assertThrows(IOException.class, () -> context.getBean(ComputeApprovedAutoDlSshIdentity.class).requireConfigured(new Fixture().node));
        });
    }

    @Test void defaultOffAutoDlBeanDoesNotInterceptTheExistingCompanyIdentity() throws Exception {
        ComputeDeploymentSshIdentity company = mock(ComputeDeploymentSshIdentity.class);
        ComputeApprovedAutoDlSshIdentity autoDl = spy(new ComputeApprovedAutoDlSshIdentity(new ComputeApprovedAutoDlSshProperties()));
        ComputeSsh ssh = new ComputeSsh(mock(ComputeCredentialCipher.class), mock(ComputeTrustedSelfPolicy.class));
        ReflectionTestUtils.setField(ssh, "deploymentIdentity", company);
        ReflectionTestUtils.setField(ssh, "approvedAutoDlIdentity", autoDl);
        ComputeNode p100 = new ComputeNode(); p100.setProvider("self"); p100.setAuthMode("DEPLOYMENT_KEY");
        p100.setHost("192.168.88.173"); p100.setUsername("oort");
        ssh.requireDeploymentIdentity(p100);
        verify(company).requireConfigured(p100); verifyNoInteractions(autoDl);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ComputeApprovedAutoDlSshProperties.class)
    static class DefaultBindingConfiguration {
        @Bean ComputeApprovedAutoDlSshIdentity autoDlIdentity(ComputeApprovedAutoDlSshProperties properties) {
            return new ComputeApprovedAutoDlSshIdentity(properties);
        }
    }

    @Test void disabledAndMissingConfigurationFailBeforeReadingAKey() throws Exception {
        Fixture f = new Fixture();
        assertNotNull(f.identity.resolve(f.node)); // Nonexistent file is not opened during validation.
        f.properties.setEnabled(false); reject(f);
        f.properties.setEnabled(true); f.properties.setEnvironmentId("vm"); reject(f);
        f.properties.setEnvironmentId("localhost"); f.properties.setBindings(Collections.emptyList()); reject(f);
        assertFalse(new ComputeApprovedAutoDlSshProperties().isEnabled());
    }

    @Test void serverOwnerTenantEndpointAndProInstanceMustMatch() throws Exception {
        changed(n -> n.setTenantId("other-tenant"));
        changed(n -> n.setManagementOwnerSubject("other-owner"));
        changed(n -> n.setManagementOwnerSubject(null));
        changed(n -> n.setHost("other.example.test"));
        changed(n -> n.setPort(22));
        changed(n -> n.setUsername("oort"));
        changed(n -> n.setProvider("self"));
        changed(n -> n.setProviderProduct("ECS"));
        changed(n -> n.setProviderInstanceId("pro-aaaaaaaaaaaa"));
        changed(n -> n.setHostKey(wireKey((byte) 2)));
        changed(n -> n.setAuthMode("PASSWORD"));
    }

    @Test void publicAliasesSharedQueuesAndAllPlatformBillingFailClosed() throws Exception {
        changed(n -> n.setResourceScope("PLATFORM"));
        changed(n -> n.setPublicInstanceKey("public-alias"));
        changed(n -> n.setBillingType("FREE"));
        changed(n -> n.setBillingType("POINTS"));
        changed(n -> n.setSchedulingMode("SHARED_GPU_QUEUE"));
        changed(n -> n.setResourceScope(null));
    }

    @Test void invalidOrDuplicateBindingsFailEvenWhenAnotherBindingMatches() throws Exception {
        Fixture f = new Fixture();
        f.properties.setBindings(Arrays.asList(f.binding, f.binding)); reject(f);
        f.properties.setBindings(Collections.singletonList(f.binding));
        f.binding.setPrivateKeyFile("relative-key"); reject(f);
        f.binding.setPrivateKeyFile(Paths.get("missing-test-only-key").toAbsolutePath().toString());
        f.binding.setHostKey("AAAA"); reject(f);
        f.binding.setHostKey(wireKey((byte) 1));
        f.binding.setPublicKeyFingerprint("SHA256:invalid"); reject(f);
    }

    @Test void immutableReferencePinsFirstConnectionAndPreservesValidatedAddresses() throws Exception {
        Fixture f = new Fixture();
        ComputeApprovedAutoDlSshIdentity.Reference reference = f.identity.resolve(f.node);
        f.binding.setHostKey(wireKey((byte) 2));
        InetAddress ip = InetAddress.getByAddress(new byte[]{93, (byte) 184, (byte) 216, 34});
        ComputeTrustedSelfPolicy.Target target = reference.bind(new ComputeTrustedSelfPolicy.Target(new InetAddress[]{ip}, null, null));
        assertEquals(wireKey((byte) 1), target.hostKey());
        assertEquals("ssh-ed25519", target.requiredAlgorithm());
        assertArrayEquals(new InetAddress[]{ip}, target.addresses());
        InetAddress[] exposed = target.addresses(); exposed[0] = InetAddress.getLoopbackAddress();
        assertEquals(ip, target.addresses()[0]);
        assertThrows(IOException.class, () -> reference.bind(new ComputeTrustedSelfPolicy.Target(new InetAddress[]{ip}, wireKey((byte) 2), null)));
        JSch client = new JSch();
        IOException error = assertThrows(IOException.class, () -> reference.addIdentity(client));
        assertFalse(error.getMessage().contains("missing-test-only-key"));
        assertNull(error.getCause());
        assertTrue(client.getIdentityRepository().getIdentities().isEmpty());
    }

    @Test void autoDlNeverFallsThroughToCompanyIdentityOrDecryptsPasswords() throws Exception {
        Fixture f = new Fixture();
        ComputeCredentialCipher cipher = mock(ComputeCredentialCipher.class);
        ComputeDeploymentSshIdentity company = mock(ComputeDeploymentSshIdentity.class);
        ComputeTrustedSelfPolicy policy = mock(ComputeTrustedSelfPolicy.class);
        when(policy.resolve(f.node)).thenReturn(new ComputeTrustedSelfPolicy.Target(new InetAddress[0], null, null));
        ComputeSsh ssh = new ComputeSsh(cipher, policy);
        ReflectionTestUtils.setField(ssh, "deploymentIdentity", company);
        assertThrows(IOException.class, () -> ssh.open(f.node));
        ReflectionTestUtils.setField(ssh, "approvedAutoDlIdentity", f.identity);
        f.properties.setEnabled(false);
        assertThrows(IOException.class, () -> ssh.open(f.node));
        verifyNoInteractions(cipher, company);
        verify(policy, times(2)).resolve(f.node);
    }

    private void changed(Consumer<ComputeNode> change) throws Exception {
        Fixture f = new Fixture(); change.accept(f.node); reject(f);
    }
    private void reject(Fixture f) {
        assertThrows(IOException.class, () -> f.identity.resolve(f.node));
    }
    static String wireKey(byte marker) {
        byte[] bytes = new byte[32]; Arrays.fill(bytes, marker);
        ByteBuffer wire = ByteBuffer.allocate(51);
        wire.putInt(11).put("ssh-ed25519".getBytes(StandardCharsets.US_ASCII)).putInt(32).put(bytes);
        return Base64.getEncoder().encodeToString(wire.array());
    }
    static final class Fixture {
        final ComputeApprovedAutoDlSshProperties properties = new ComputeApprovedAutoDlSshProperties();
        final ComputeApprovedAutoDlSshProperties.Binding binding = new ComputeApprovedAutoDlSshProperties.Binding();
        final ComputeNode node = new ComputeNode();
        final ComputeApprovedAutoDlSshIdentity identity = new ComputeApprovedAutoDlSshIdentity(properties);
        Fixture() {
            properties.setEnabled(true); properties.setEnvironmentId("localhost");
            properties.setBindings(Collections.singletonList(binding));
            binding.setId("test-only"); binding.setTenantId("tenant-a"); binding.setManagementOwnerSubject("owner-a");
            binding.setInstanceId("pro-0123456789ab"); binding.setHost("example.test"); binding.setPort(33138);
            binding.setUsername("root"); binding.setHostKey(wireKey((byte) 1));
            binding.setPublicKeyFingerprint("SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            binding.setPrivateKeyFile(Paths.get("missing-test-only-key").toAbsolutePath().toString());
            node.setTenantId("tenant-a"); node.setManagementOwnerSubject("owner-a");
            node.setProvider("autodl"); node.setHost("example.test"); node.setPort(33138); node.setUsername("root");
            node.setAuthMode("DEPLOYMENT_KEY"); node.setResourceScope("PRIVATE"); node.setBillingType("EXTERNAL");
            node.setSchedulingMode("INSTANCE_EXCLUSIVE");
        }
    }
}
