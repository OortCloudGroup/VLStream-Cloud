package com.ruoyi.vlstream.test.vlstream.compute;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Generates temporary uninstalled keys only; never reads the user's real identity. */
@Tag("dev")
class AutoDlTemporaryEd25519LoadTest {
    @TempDir Path temp;

    @Test void java8CanLoadAndSignWithAnUnencryptedOpenSshEd25519Fixture() throws Exception {
        Path key = generate("");
        ComputeApprovedAutoDlSshIdentityTest.Fixture fixture = fixture(key);
        JSch client = new JSch();
        try {
            fixture.identity.resolve(fixture.node).addIdentity(client);
            assertEquals(1, client.getIdentityRepository().getIdentities().size());
            Identity identity = client.getIdentityRepository().getIdentities().firstElement();
            assertFalse(identity.isEncrypted());
            assertEquals("ssh-ed25519", identity.getAlgName());
            assertNotNull(identity.getSignature("offline-test-only".getBytes(StandardCharsets.US_ASCII)));
        } finally { client.removeAllIdentity(); }
    }

    @Test void encryptedOpenSshFixtureCannotFallBackOrLeakItsPath() throws Exception {
        Path key = generate("test-only-not-a-production-password");
        ComputeApprovedAutoDlSshIdentityTest.Fixture fixture = fixture(key);
        JSch client = new JSch();
        IOException error = assertThrows(IOException.class, () -> fixture.identity.resolve(fixture.node).addIdentity(client));
        assertNull(error.getCause()); assertFalse(error.getMessage().contains(key.toString()));
        assertTrue(client.getIdentityRepository().getIdentities().isEmpty());
    }

    private ComputeApprovedAutoDlSshIdentityTest.Fixture fixture(Path key) throws Exception {
        String[] fields = new String(Files.readAllBytes(key.resolveSibling(key.getFileName() + ".pub")), StandardCharsets.US_ASCII).trim().split("\\s+");
        String fingerprint = "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(fields[1])));
        ComputeApprovedAutoDlSshIdentityTest.Fixture fixture = new ComputeApprovedAutoDlSshIdentityTest.Fixture();
        fixture.binding.setPrivateKeyFile(key.toString()); fixture.binding.setPublicKeyFingerprint(fingerprint);
        return fixture;
    }
    private Path generate(String passphrase) throws Exception {
        Path key = temp.resolve("uninstalled-test-only-key");
        String keygen = System.getProperty("os.name").startsWith("Windows")
            ? Paths.get(System.getenv("WINDIR"), "System32", "OpenSSH", "ssh-keygen.exe").toString() : "ssh-keygen";
        Process process = new ProcessBuilder(keygen, "-q", "-t", "ed25519", "-N", passphrase, "-C", "uninstalled-offline-test", "-f", key.toString())
            .redirectErrorStream(true).redirectOutput(temp.resolve("keygen.log").toFile()).start();
        assertTrue(process.waitFor(20, TimeUnit.SECONDS)); assertEquals(0, process.exitValue());
        AclFileAttributeView acl = Files.getFileAttributeView(key, AclFileAttributeView.class);
        if (acl != null) {
            // Windows sandbox tools can run under a different account than JVM user.name.
            // Run this test as the real user; never relax production owner checks.
            java.nio.file.attribute.UserPrincipal owner = key.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
            assertEquals(owner, acl.getOwner(), "Run temporary-identity tests as the actual JVM user; no owner override is permitted");
            acl.setAcl(Collections.singletonList(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
                .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
        } else Files.setPosixFilePermissions(key, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        return key;
    }
}
