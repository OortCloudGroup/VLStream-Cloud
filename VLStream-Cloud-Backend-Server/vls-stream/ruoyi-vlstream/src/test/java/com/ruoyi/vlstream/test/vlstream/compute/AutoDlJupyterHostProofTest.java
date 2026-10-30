package com.ruoyi.vlstream.test.vlstream.compute;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AutoDlJupyterHostProofTest {
    String key(){byte[] type="ssh-ed25519".getBytes(StandardCharsets.US_ASCII);ByteBuffer b=ByteBuffer.allocate(51);b.putInt(type.length).put(type).putInt(32).put(new byte[32]);return Base64.getEncoder().encodeToString(b.array());}
    @Test void exactProviderOriginOnly() throws Exception {assertEquals("https://fixture.seetacloud.com:8443",AutoDlJupyterHostProof.origin("fixture.seetacloud.com:8443"));for(String bad:new String[]{"autodl.com.evil.test","127.0.0.1","user@fixture.autodl.com","fixture.autodl.com/path","fixture.autodl.com?token=secret","fixture.autodl.com:8080","fixture..autodl.com"})assertThrows(java.io.IOException.class,()->AutoDlJupyterHostProof.origin(bad));}
    @Test void onlyCompletedNonceDelimitedPublicKeyIsAccepted() throws Exception {String text="echoed printf '\\nNONCE\\n'\r\nNONCE\r\nssh-ed25519 "+key()+" root@fixture\r\nNONCE_END\r\n";assertEquals(key(),AutoDlJupyterHostProof.extract(text,"NONCE"));assertNull(AutoDlJupyterHostProof.extract(text,"DIFFERENT_NONCE"));}
    @Test void echoedCommandAndPartialResponseAreNotProof() throws Exception {assertNull(AutoDlJupyterHostProof.extract("printf '\\nNONCE\\n'; cat /etc/ssh/ssh_host_ed25519_key.pub","NONCE"));assertNull(AutoDlJupyterHostProof.extract("\nNONCE\nssh-ed25519 "+key(),"NONCE"));}
    @Test void missingDuplicateOrMalformedKeysFailClosed(){for(String value:new String[]{"no key","ssh-ed25519 AAAA","ssh-ed25519 "+key()+"\nssh-ed25519 "+key()})assertThrows(java.io.IOException.class,()->AutoDlJupyterHostProof.extract("\nNONCE\n"+value+"\nNONCE_END","NONCE"));}
}
