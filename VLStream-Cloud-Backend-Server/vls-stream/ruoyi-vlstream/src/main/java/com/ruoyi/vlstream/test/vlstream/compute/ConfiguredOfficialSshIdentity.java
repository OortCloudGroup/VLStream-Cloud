package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Explicit deployment reference for official supply. No key is read during catalog/quote validation. */
@Component
@RequiredArgsConstructor
public class ConfiguredOfficialSshIdentity {
    private final ConfiguredComputeRegistry configured;
    public Reference resolve(ComputeNode node) throws IOException {
        try {
            configured.verifyOfficial(node);
            if(!"PLATFORM".equals(node.getResourceScope())||!"POINTS".equals(node.getBillingType())||!ComputeDeploymentSshIdentity.KEY.equals(node.getAuthMode()))throw new IllegalArgumentException();
            ConfiguredComputeProperties.Resource resource=configured.definition(node);
            Path file=Paths.get(resource.getPrivateKeyFile());
            if(!file.isAbsolute()||!file.normalize().equals(file)||resource.getPublicKeyFingerprint()==null
                ||!resource.getPublicKeyFingerprint().matches("SHA256:[A-Za-z0-9+/]{43}"))throw new IllegalArgumentException();
            byte[] host=Base64.getDecoder().decode(node.getHostKey());
            if(!"ssh-ed25519".equals(new HostKey(node.getHost(),host).getType()))throw new IllegalArgumentException();
            return new Reference(file,resource.getPublicKeyFingerprint(),node.getHostKey());
        } catch(Exception invalid) {throw new IOException("官方算力SSH部署身份或主机公钥未配置，请管理员核对安全引用");}
    }
    public static final class Reference {
        private final Path file;
        private final String fingerprint,hostKey;
        private Reference(Path file,String fingerprint,String hostKey){this.file=file;this.fingerprint=fingerprint;this.hostKey=hostKey;}
        ComputeTrustedSelfPolicy.Target bind(ComputeTrustedSelfPolicy.Target target) throws IOException {
            if(target.hostKey()!=null&&!target.hostKey().isEmpty()&&!MessageDigest.isEqual(Base64.getDecoder().decode(target.hostKey()),Base64.getDecoder().decode(hostKey)))throw new IOException("官方主机身份不匹配");
            return new ComputeTrustedSelfPolicy.Target(target.addresses(),hostKey,"ssh-ed25519");
        }
        void addIdentity(JSch client) throws IOException {ComputeDeploymentSshIdentity.addVerifiedFiles(client,file,fingerprint);}
    }
}
