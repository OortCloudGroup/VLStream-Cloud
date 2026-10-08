package com.ruoyi.vlstream.test.vlstream.compute;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/** Lazy first use keeps deployments without online compute independent of key storage. */
@Service
public class ComputeKeyStore {
    private static final Object JVM_FILE_LOCK = new Object();
    private final String configured;
    private final Path path;
    private final ComputeKeyRegistry registry;
    private volatile byte[] cached;

    public ComputeKeyStore(@Value("${vlstream.compute.encryption-key:${VLSTREAM_COMPUTE_ENCRYPTION_KEY:}}") String configured,
        @Value("${vlstream.compute.encryption-key-file:${user.dir}/data/security/compute-credential.key}") String path,
        ComputeKeyRegistry registry) {
        this.configured = configured;
        this.path = Paths.get(path).toAbsolutePath().normalize();
        this.registry = registry;
    }

    public byte[] key() {
        byte[] value = cached;
        if (value != null) return value.clone();
        synchronized (JVM_FILE_LOCK) {
            if (cached != null) return cached.clone();
            if (configured != null && !configured.isEmpty()) {
                value = checked(configured.getBytes(StandardCharsets.UTF_8));
                registry.validate(value);
                registry.register(value);
                cached = value.clone();
                return value.clone();
            }
            try {
                Path parent = path.getParent();
                boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
                Files.createDirectories(parent, permissions(posix, "rwx------"));
                Path lockPath = parent.resolve(path.getFileName() + ".lock");
                try (FileChannel lockChannel = FileChannel.open(lockPath,
                        java.util.EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE), permissions(posix, "rw-------"));
                     FileLock ignored = lockChannel.lock()) {
                    boolean exists = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
                    if (exists) {
                        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 256) throw new IOException("密钥文件无效");
                        String saved = new String(Files.readAllBytes(path), StandardCharsets.US_ASCII).trim();
                        if (!saved.startsWith("v1:")) throw new IOException("密钥文件格式无效");
                        value = checked(Base64.getDecoder().decode(saved.substring(3)));
                    } else {
                        if (registry.requiresExistingKey()) throw new IllegalStateException("算力密钥文件缺失，请恢复原密钥或挂载已有密钥目录，不能为已有凭据重新生成密钥");
                        value = new byte[32]; new SecureRandom().nextBytes(value);
                    }
                    registry.validate(value);
                    if (!exists) persist(value, posix);
                    registry.register(value);
                    cached = value.clone();
                    return value.clone();
                }
            } catch (IOException | IllegalArgumentException e) {
                throw new IllegalStateException("无法读取或持久保存算力密钥，请检查后端数据目录权限及密钥文件", e);
            }
        }
    }

    static byte[] checked(byte[] value) {
        if (value.length != 16 && value.length != 24 && value.length != 32) throw new IllegalArgumentException("密钥必须是16、24或32字节");
        return value;
    }

    private void persist(byte[] value, boolean posix) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), ".compute-key-", ".tmp", permissions(posix, "rw-------"));
        try {
            byte[] content = ("v1:" + Base64.getEncoder().encodeToString(value) + "\n").getBytes(StandardCharsets.US_ASCII);
            try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) file.write(buffer);
                file.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }

    private static FileAttribute<?>[] permissions(boolean posix, String mode) {
        return posix ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(mode))} : new FileAttribute<?>[0];
    }
}
