package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.JSch;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Local, explicitly approved AutoDL private-node identities; no public compute admission. */
@Component
@RequiredArgsConstructor
public class ComputeApprovedAutoDlSshIdentity {
    private final ComputeApprovedAutoDlSshProperties properties;

    public void requireConfigured(ComputeNode node) throws IOException { resolve(node); }

    /** Validate public configuration only, then snapshot it for one SSH connection. */
    public Reference resolve(ComputeNode node) throws IOException {
        if (!properties.isEnabled() || !"localhost".equals(properties.getEnvironmentId())
            || !ComputeDeploymentSshIdentity.KEY.equals(node.getAuthMode()) || !"autodl".equals(node.getProvider())
            || !"root".equals(node.getUsername()) || !"PRIVATE".equals(node.getResourceScope())
            || !"EXTERNAL".equals(node.getBillingType()) || node.getPublicInstanceKey() != null
            || !"INSTANCE_EXCLUSIVE".equals(node.getSchedulingMode())
            || !opaque(node.getTenantId()) || !opaque(node.getManagementOwnerSubject())
            || (node.getProviderProduct() != null && !"AUTODL_PRO".equals(node.getProviderProduct())))
            throw invalid();
        Set<String> ids = new HashSet<>();
        Set<String> endpoints = new HashSet<>();
        Reference selected = null;
        if (properties.getBindings() != null) for (ComputeApprovedAutoDlSshProperties.Binding binding : properties.getBindings()) {
            if (binding == null || binding.getId() == null || !binding.getId().matches("[A-Za-z0-9_.:-]{1,128}")
                || !ids.add(binding.getId()) || !opaque(binding.getTenantId()) || !opaque(binding.getManagementOwnerSubject())
                || binding.getInstanceId() == null || !binding.getInstanceId().matches("pro-[a-f0-9]{12}")
                || binding.getHost() == null || !binding.getHost().matches("[a-z0-9][a-z0-9.-]{0,252}")
                || binding.getHost().contains("..") || binding.getPort() < 1 || binding.getPort() > 65535
                || !"root".equals(binding.getUsername()) || !endpoints.add(binding.getHost() + ":" + binding.getPort())
                || binding.getPublicKeyFingerprint() == null
                || !binding.getPublicKeyFingerprint().matches("SHA256:[A-Za-z0-9+/]{43}")) throw invalid();
            byte[] hostKey = hostKey(binding.getHostKey());
            Path privateFile;
            try {
                privateFile = Paths.get(binding.getPrivateKeyFile());
                if (!privateFile.isAbsolute() || !privateFile.normalize().equals(privateFile)) throw invalid();
            } catch (RuntimeException error) { throw invalid(); }
            if (binding.getHost().equals(node.getHost()) && binding.getPort() == node.getPort()
                && binding.getTenantId().equals(node.getTenantId())
                && binding.getManagementOwnerSubject().equals(node.getManagementOwnerSubject())) {
                if (node.getProviderInstanceId() != null && !binding.getInstanceId().equals(node.getProviderInstanceId())) throw invalid();
                if (node.getHostKey() != null && !node.getHostKey().isEmpty()
                    && !MessageDigest.isEqual(hostKey, hostKey(node.getHostKey()))) throw invalid();
                selected = new Reference(privateFile, binding.getPublicKeyFingerprint(), binding.getHostKey());
            }
        }
        if (selected == null) throw invalid();
        return selected;
    }

    private static boolean opaque(String value) {
        return value != null && !value.trim().isEmpty() && value.length() <= 255
            && value.chars().noneMatch(character -> Character.isISOControl((char) character));
    }

    private static IOException invalid() { return new IOException("AutoDL本地部署身份未匹配明确授权的私有实例配置"); }

    private static byte[] hostKey(String encoded) throws IOException {
        try {
            if (encoded == null || encoded.length() > 512) throw invalid();
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (!Base64.getEncoder().encodeToString(bytes).equals(encoded)) throw invalid();
            ByteBuffer wire = ByteBuffer.wrap(bytes);
            int typeLength = wire.getInt();
            if (typeLength != 11 || wire.remaining() != 11 + 4 + 32) throw invalid();
            byte[] type = new byte[typeLength]; wire.get(type);
            if (!"ssh-ed25519".equals(new String(type, StandardCharsets.US_ASCII)) || wire.getInt() != 32 || wire.remaining() != 32) throw invalid();
            return bytes;
        } catch (RuntimeException error) { throw invalid(); }
    }

    /** Immutable per-connection reference; never serialized or returned to a controller. */
    public static final class Reference {
        private final Path privateFile;
        private final String fingerprint;
        private final String hostKey;
        private Reference(Path privateFile, String fingerprint, String hostKey) {
            this.privateFile = privateFile; this.fingerprint = fingerprint; this.hostKey = hostKey;
        }
        ComputeTrustedSelfPolicy.Target bind(ComputeTrustedSelfPolicy.Target target) throws IOException {
            if (target.hostKey() != null && !target.hostKey().isEmpty()
                && !MessageDigest.isEqual(hostKey(hostKey), hostKey(target.hostKey()))) throw invalid();
            return new ComputeTrustedSelfPolicy.Target(target.addresses(), hostKey, "ssh-ed25519");
        }
        void addIdentity(JSch client) throws IOException {
            ComputeDeploymentSshIdentity.addVerifiedFiles(client, privateFile, fingerprint);
        }
    }
}
