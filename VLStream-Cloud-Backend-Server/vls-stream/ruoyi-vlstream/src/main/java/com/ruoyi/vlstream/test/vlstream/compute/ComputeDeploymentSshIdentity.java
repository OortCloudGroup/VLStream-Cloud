package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/** A company asset may use one explicitly approved deployment identity, never an API-supplied key. */
@Component
@RequiredArgsConstructor
public class ComputeDeploymentSshIdentity {
    public static final String PASSWORD = "PASSWORD";
    public static final String KEY = "DEPLOYMENT_KEY";
    static final String LOCAL_FILE = "C:/Users/oort/.ssh/id_rsa";
    static final String VM_FILE = "/run/secrets/vls-company-gpu-vm/id_ed25519";
    static final String LOCAL_FINGERPRINT = "SHA256:+XWudHQwt/rn8y7mI0RAa1x+VJgpQKLg2QhkoIGJQPI";
    static final String VM_FINGERPRINT = "SHA256:ja1WEMu8vL4WuHyUZ/rq5DdpX+tS7K/10tyIFKo3BmU";
    private final ComputeSshIdentityProperties properties;
    private final ComputeTrustedSelfPolicy policy;
    private final ComputeSharedQueueProperties shared;

    /** Pure public configuration validation. Does not open a private key or contact a host. */
    public void requireConfigured(ComputeNode node) throws IOException { reference(node); }

    private ComputeSshIdentityProperties.Identity reference(ComputeNode node) throws IOException {
        if (!KEY.equals(node.getAuthMode()) || !properties.isEnabled())
            throw new IOException("后台部署SSH身份未启用");
        String environment = properties.getEnvironmentId();
        if (!("localhost".equals(environment) || "vm".equals(environment))
            || !environment.equals(shared.getEnvironmentId()))
            throw new IOException("后台部署SSH身份与调度环境不一致");
        ComputeTrustedSelfAssetsProperties.Asset asset = policy.requireAsset(node);
        if (!asset.isCompanyPool() || !"oort".equals(node.getUsername()))
            throw new IOException("后台部署SSH身份只用于已批准公司资产的oort账户");
        Set<String> seen = new HashSet<>();
        ComputeSshIdentityProperties.Identity selected = null;
        if (properties.getIdentities() != null) for (ComputeSshIdentityProperties.Identity identity : properties.getIdentities()) {
            if (identity == null || identity.getAssetId() == null || !seen.add(identity.getAssetId()))
                throw new IOException("后台部署SSH身份配置缺失或重复");
            if (asset.getAssetId().equals(identity.getAssetId())) selected = identity;
        }
        String requiredFile = "localhost".equals(environment) ? LOCAL_FILE : VM_FILE;
        if (selected == null || !requiredFile.equals(selected.getPrivateKeyFile())
            || selected.getPublicKeyFingerprint() == null
            || !selected.getPublicKeyFingerprint().matches("SHA256:[A-Za-z0-9+/]{43}")
            || ("localhost".equals(environment) && !LOCAL_FINGERPRINT.equals(selected.getPublicKeyFingerprint()))
            || ("vm".equals(environment) && !VM_FINGERPRINT.equals(selected.getPublicKeyFingerprint())))
            throw new IOException("后台部署SSH身份未匹配批准的文件和公开指纹");
        return selected;
    }

    public void addIdentity(ComputeNode node, JSch client) throws IOException {
        ComputeSshIdentityProperties.Identity identity = reference(node);
        addVerifiedFiles(client, Paths.get(identity.getPrivateKeyFile()), identity.getPublicKeyFingerprint());
    }

    /** Package visibility permits isolated temporary fixtures, never a request/controller entry point. */
    static void addVerifiedFiles(JSch client, Path privateFile, String expectedFingerprint) throws IOException {
        byte[] privateBytes = null;
        try {
            if (!client.getIdentityRepository().getIdentities().isEmpty()) throw new IOException("身份仓库必须为空");
            Path absolute = privateFile.toAbsolutePath().normalize();
            for (Path current = absolute; current != null; current = current.getParent())
                if (Files.isSymbolicLink(current) || !current.toRealPath(LinkOption.NOFOLLOW_LINKS).equals(current))
                    throw new IOException("身份路径不能包含链接或重解析路径");
            requirePrivatePermissions(absolute);
            byte[] publicBytes = readRegular(absolute.resolveSibling(absolute.getFileName() + ".pub"), 16384);
            String line = new String(publicBytes, StandardCharsets.US_ASCII).trim();
            String[] fields = line.split("\\s+", 3);
            if (fields.length < 2 || line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) throw new IOException("公开身份格式无效");
            byte[] publicBlob = Base64.getDecoder().decode(fields[1]);
            String algorithm = new HostKey("deployment", publicBlob).getType();
            if (!fields[0].equals(algorithm) || !("ssh-rsa".equals(algorithm) || "ssh-ed25519".equals(algorithm)
                || algorithm.startsWith("ecdsa-sha2-nistp"))) throw new IOException("公开身份算法无效");
            String actual = "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(publicBlob));
            if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII), expectedFingerprint.getBytes(StandardCharsets.US_ASCII)))
                throw new IOException("公开身份不匹配批准指纹");
            privateBytes = readRegular(absolute, 65536);
            client.addIdentity("vls-company-deployment", privateBytes, null, null);
            java.util.Vector<Identity> identities = client.getIdentityRepository().getIdentities();
            if (identities.size() != 1 || identities.get(0).isEncrypted()
                || !MessageDigest.isEqual(publicBlob, identities.get(0).getPublicKeyBlob()))
                throw new IOException("身份不能非交互加载或公私身份不匹配");
            requirePrivatePermissions(absolute);
        } catch (Exception error) {
            try { client.removeAllIdentity(); } catch (Exception ignored) { }
            // Do not expose file contents, paths, parser diagnostics or a password fallback.
            throw new IOException("后台部署SSH身份校验失败，请管理员核对文件权限、公开指纹及非交互加载方式");
        } finally { if (privateBytes != null) Arrays.fill(privateBytes, (byte) 0); }
    }

    private static byte[] readRegular(Path path, int maximum) throws IOException {
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.isSymbolicLink() || before.size() < 1 || before.size() > maximum)
            throw new IOException("身份文件格式或大小无效");
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS) != null
            && !Integer.valueOf(1).equals(Files.getAttribute(path, "unix:nlink", LinkOption.NOFOLLOW_LINKS)))
            throw new IOException("身份文件不能为硬链接");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ComputeIdentityFile identity=ComputeIdentityFile.open(path,before);
             SeekableByteChannel input = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (input.read(buffer) > 0) {
                if (bytes.size() + buffer.position() > maximum) throw new IOException("身份文件过大");
                bytes.write(buffer.array(), 0, buffer.position()); buffer.clear();
            }
            BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!after.isRegularFile() || !identity.matches(path,after)
                || before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime()))
                throw new IOException("身份文件在读取中发生变化");
        }
        return bytes.toByteArray();
    }

    private static void requirePrivatePermissions(Path path) throws IOException {
        String user = System.getProperty("user.name");
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posix != null) {
            PosixFileAttributes file = posix.readAttributes();
            Set<PosixFilePermission> forbidden = new HashSet<>(Arrays.asList(PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));
            if (!file.owner().getName().equals(user) || file.permissions().stream().anyMatch(forbidden::contains))
                throw new IOException("私有身份权限或所有者不匹配");
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (acl == null) throw new IOException("无法验证私有身份ACL");
        String owner = acl.getOwner().getName();
        if (!(owner.equalsIgnoreCase(user) || owner.toLowerCase(java.util.Locale.ROOT).endsWith("\\" + user.toLowerCase(java.util.Locale.ROOT))))
            throw new IOException("私有身份所有者不匹配");
        Set<AclEntryPermission> sensitive = new HashSet<>(Arrays.asList(AclEntryPermission.READ_DATA,
            AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA, AclEntryPermission.DELETE,
            AclEntryPermission.WRITE_ACL, AclEntryPermission.WRITE_OWNER));
        for (AclEntry entry : acl.getAcl()) if (entry.type() == AclEntryType.ALLOW && entry.permissions().stream().anyMatch(sensitive::contains)) {
            String principal = entry.principal().getName();
            if (!(principal.equalsIgnoreCase(owner) || principal.equalsIgnoreCase("NT AUTHORITY\\SYSTEM")
                || principal.equalsIgnoreCase("BUILTIN\\Administrators") || principal.equals("S-1-5-18") || principal.equals("S-1-5-32-544")))
                throw new IOException("私有身份ACL包含非批准账户");
        }
    }
}
