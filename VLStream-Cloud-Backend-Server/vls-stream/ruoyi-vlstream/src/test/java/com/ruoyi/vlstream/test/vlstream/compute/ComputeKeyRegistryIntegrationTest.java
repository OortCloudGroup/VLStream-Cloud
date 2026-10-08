package com.ruoyi.vlstream.test.vlstream.compute;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeKeyRegistryIntegrationTest {
    private static DriverManagerDataSource source;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    @TempDir Path directory;

    @BeforeAll static void database() {
        String url = System.getenv("VLS_COMPUTE_KEY_TEST_JDBC");
        Assumptions.assumeTrue(url != null, "Dedicated test database required");
        assertTrue(url.startsWith("jdbc:mysql://127.0.0.1:33339/compute_key_test"), "Never run this fixture against a business database");
        source = new DriverManagerDataSource(url,"root","");
    }

    @BeforeEach void schema() {
        jdbc = new JdbcTemplate(source); transactions = new DataSourceTransactionManager(source);
        jdbc.execute("DROP TABLE IF EXISTS vls_compute_key_registry");
        jdbc.execute("DROP TABLE IF EXISTS vls_cloud_training_job");
        jdbc.execute("DROP TABLE IF EXISTS vls_compute_node");
        Path migrations = Paths.get(System.getProperty("compute.key.migration-dir"));
        new ResourceDatabasePopulator(new FileSystemResource(migrations.resolve("V1_2_0_025__autodl_ssh_compute.sql")),
            new FileSystemResource(migrations.resolve("V1_2_0_028__compute_key_fingerprint.sql"))).execute(source);
    }

    private ComputeKeyRegistry registry() { return new ComputeKeyRegistry(jdbc,transactions); }
    private ComputeKeyStore store(String key, String name) { return new ComputeKeyStore(key,directory.resolve(name).toString(),registry()); }
    private void node(String encrypted) {
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,host,port,username,password_cipher,python_path,work_dir) VALUES(1,'test','test','example.com',22,'root',?,'/python','/work')",encrypted);
    }

    @Test void freshMigrationSupportsZeroConfigurationAndRestart() {
        ComputeCredentialCipher cipher = new ComputeCredentialCipher(store("","key"));
        String encrypted = cipher.encrypt("test-password"); node(encrypted);
        assertEquals("test-password",new ComputeCredentialCipher(store("","key")).decrypt(encrypted));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_key_registry",Integer.class));
        assertTrue(jdbc.queryForObject("SELECT key_sha256 FROM vls_compute_key_registry",String.class).matches("[a-f0-9]{64}"));
        assertThrows(IllegalStateException.class,() -> store("","other-replica/key").key());
    }

    @Test void springWiringUsesAutomaticStoreWhenConfigurationIsBlank() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            Map<String,Object> properties = new HashMap<>();
            properties.put("vlstream.compute.encryption-key", "");
            properties.put("vlstream.compute.encryption-key-file", directory.resolve("spring/key").toString());
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test-key", properties));
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.registerBean(org.springframework.transaction.PlatformTransactionManager.class, () -> transactions);
            context.register(ComputeKeyRegistry.class,ComputeKeyStore.class,ComputeCredentialCipher.class);
            context.refresh();
            ComputeCredentialCipher cipher = context.getBean(ComputeCredentialCipher.class);
            assertEquals("spring-password", cipher.decrypt(cipher.encrypt("spring-password")));
            assertTrue(Files.isRegularFile(directory.resolve("spring/key")));
        }
    }

    @Test void adoptsCorrectLegacyKeyAndRejectsMissingOrIncorrectLegacyKey() {
        String original = "0123456789abcdef0123456789abcdef";
        String encrypted = new ComputeCredentialCipher(original).encrypt("legacy-password"); node(encrypted);
        assertThrows(IllegalStateException.class, () -> store("","auto").key());
        assertThrows(IllegalStateException.class, () -> store("abcdef0123456789abcdef0123456789","wrong").key());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_key_registry",Integer.class));
        assertEquals("legacy-password",new ComputeCredentialCipher(store(original,"unused")).decrypt(encrypted));
        assertFalse(Files.exists(directory.resolve("unused")));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_key_registry",Integer.class));
    }

    @Test void nodeRollbackCannotRollBackFingerprintForAnAlreadyPersistedKey() {
        AtomicReference<String> encrypted = new AtomicReference<>();
        new TransactionTemplate(transactions).execute(status -> {
            encrypted.set(new ComputeCredentialCipher(store("","key")).encrypt("rollback-password"));
            node(encrypted.get()); status.setRollbackOnly(); return null;
        });
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_key_registry",Integer.class));
        assertEquals("rollback-password",new ComputeCredentialCipher(store("","key")).decrypt(encrypted.get()));
    }

    @Test void lostFileCannotBeRegeneratedEvenWhenNodeSaveNeverCommitted() throws Exception {
        store("","key").key(); Files.delete(directory.resolve("key"));
        assertThrows(IllegalStateException.class, () -> store("","key").key());
        assertFalse(Files.exists(directory.resolve("key")));
    }

    @Test void concurrentTransactionsAgreeOnExactlyOneFingerprint() throws Exception {
        for (boolean sameKey : new boolean[]{false,true}) {
            jdbc.execute("DELETE FROM vls_compute_key_registry");
            CountDownLatch validated = new CountDownLatch(3);
            ExecutorService pool = Executors.newFixedThreadPool(3);
            try {
                List<Callable<Boolean>> calls = new ArrayList<>();
                for (int index=0;index<3;index++) {
                    ComputeKeyRegistry candidate = spy(registry());
                    doAnswer(call -> { call.callRealMethod(); validated.countDown(); assertTrue(validated.await(5,TimeUnit.SECONDS)); return null; }).when(candidate).validate(any());
                    byte[] key = new byte[32]; Arrays.fill(key,(byte)(sameKey ? 1 : index+1));
                    calls.add(() -> { try { candidate.register(key); return true; } catch (IllegalStateException expected) { return false; } });
                }
                int successes = 0;
                for (Future<Boolean> result : pool.invokeAll(calls)) if (result.get()) successes++;
                assertEquals(sameKey ? 3 : 1,successes);
                assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_key_registry",Integer.class));
            } finally { pool.shutdownNow(); }
        }
    }
}
