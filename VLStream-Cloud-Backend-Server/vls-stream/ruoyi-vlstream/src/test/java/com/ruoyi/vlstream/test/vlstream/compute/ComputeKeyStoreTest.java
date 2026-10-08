package com.ruoyi.vlstream.test.vlstream.compute;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeKeyStoreTest {
    @TempDir Path directory;

    private ComputeKeyRegistry registry() {
        ComputeKeyRegistry registry = mock(ComputeKeyRegistry.class);
        AtomicReference<byte[]> binding = new AtomicReference<>();
        when(registry.requiresExistingKey()).thenAnswer(call -> binding.get() != null);
        doAnswer(call -> {
            byte[] key = call.getArgument(0);
            if (binding.get() != null && !Arrays.equals(binding.get(), key)) throw new IllegalStateException("fingerprint mismatch");
            return null;
        }).when(registry).validate(any());
        doAnswer(call -> {
            byte[] key = call.getArgument(0);
            if (binding.get() == null) binding.compareAndSet(null, key.clone());
            if (!Arrays.equals(binding.get(), key)) throw new IllegalStateException("fingerprint mismatch");
            return null;
        }).when(registry).register(any());
        return registry;
    }

    @Test void freshDeploymentPersistsKeyAndRestartsCanDecrypt() throws Exception {
        Path file = directory.resolve("security/compute.key");
        ComputeKeyRegistry registry = registry();
        ComputeKeyStore first = new ComputeKeyStore("", file.toString(), registry);
        ComputeCredentialCipher cipher = new ComputeCredentialCipher(first);
        String encrypted = cipher.encrypt("test-ssh-password");
        byte[] saved = Files.readAllBytes(file);
        assertEquals(32, first.key().length);
        assertTrue(encrypted.startsWith("v1:"));
        assertFalse(new String(saved, StandardCharsets.US_ASCII).contains("test-ssh-password"));
        assertEquals("test-ssh-password", new ComputeCredentialCipher(new ComputeKeyStore("",file.toString(),registry)).decrypt(encrypted));
        assertArrayEquals(saved,Files.readAllBytes(file));
        assertNotEquals(encrypted,cipher.encrypt("test-ssh-password"));
    }

    @Test void independentDeploymentsGetDifferentRandomKeys() {
        assertFalse(Arrays.equals(new ComputeKeyStore("",directory.resolve("a/key").toString(),registry()).key(),
            new ComputeKeyStore("",directory.resolve("b/key").toString(),registry()).key()));
    }

    @Test void explicitLegacyKeyDoesNotRequireWritableFileStorage() throws Exception {
        Path blocker = directory.resolve("not-a-directory"); Files.write(blocker,new byte[]{1});
        String key = "0123456789abcdef0123456789abcdef";
        String encrypted = new ComputeCredentialCipher(key).encrypt("legacy-password");
        ComputeKeyStore store = new ComputeKeyStore(key,blocker.resolve("key").toString(),registry());
        assertEquals("legacy-password",new ComputeCredentialCipher(store).decrypt(encrypted));
        assertArrayEquals(new byte[]{1},Files.readAllBytes(blocker));
    }

    @Test void lossOrCorruptionDoesNotSilentlyReplaceTheKey() throws Exception {
        Path file = directory.resolve("key"); ComputeKeyRegistry registry = registry();
        new ComputeKeyStore("",file.toString(),registry).key();
        Files.delete(file);
        assertThrows(IllegalStateException.class, () -> new ComputeKeyStore("",file.toString(),registry).key());
        assertFalse(Files.exists(file));
        Files.write(file,"corrupt".getBytes(StandardCharsets.US_ASCII));
        assertThrows(IllegalStateException.class, () -> new ComputeKeyStore("",file.toString(),registry).key());
        assertEquals("corrupt",new String(Files.readAllBytes(file),StandardCharsets.US_ASCII));
    }

    @Test void legacyCredentialsWithoutTheirOriginalKeyBlockGeneration() {
        ComputeKeyRegistry registry = mock(ComputeKeyRegistry.class); when(registry.requiresExistingKey()).thenReturn(true);
        Path file = directory.resolve("key");
        assertThrows(IllegalStateException.class, () -> new ComputeKeyStore("",file.toString(),registry).key());
        assertFalse(Files.exists(file)); verify(registry,never()).register(any());
    }

    @Test void concurrentReplicasOnSharedStorageUseTheSameKey() throws Exception {
        Path file = directory.resolve("shared/key"); ComputeKeyRegistry registry = registry();
        ExecutorService workers = Executors.newFixedThreadPool(6);
        try {
            List<Callable<byte[]>> calls = new ArrayList<>();
            for (int i=0;i<12;i++) calls.add(() -> new ComputeKeyStore("",file.toString(),registry).key());
            List<Future<byte[]>> keys = workers.invokeAll(calls);
            for (Future<byte[]> key : keys) assertArrayEquals(keys.get(0).get(),key.get());
        } finally { workers.shutdownNow(); }
    }

    @Test void conflictingReplicaCannotUseANewKeyForTheSameDatabase() {
        ComputeKeyRegistry registry = registry();
        new ComputeKeyStore("",directory.resolve("replica-a/key").toString(),registry).key();
        Path other = directory.resolve("replica-b/key");
        assertThrows(IllegalStateException.class, () -> new ComputeKeyStore("",other.toString(),registry).key());
        assertFalse(Files.exists(other));
        assertThrows(IllegalStateException.class, () -> new ComputeKeyStore("another-key-1234",other.toString(),registry).key());
    }

    @Test void invalidConfiguredKeyAndUnwritableStorageFailWithoutBinding() throws Exception {
        ComputeKeyRegistry registry = registry(); Path file = directory.resolve("key");
        assertThrows(IllegalArgumentException.class, () -> new ComputeKeyStore("short",file.toString(),registry).key());
        Files.write(file,new byte[]{1});
        assertThrows(IllegalStateException.class, () -> new ComputeKeyStore("",file.resolve("child").toString(),registry).key());
        verify(registry,never()).register(any());
    }
}
