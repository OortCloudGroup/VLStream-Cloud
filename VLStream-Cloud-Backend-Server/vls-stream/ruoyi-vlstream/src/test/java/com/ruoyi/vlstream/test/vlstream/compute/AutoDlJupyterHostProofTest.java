package com.ruoyi.vlstream.test.vlstream.compute;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AutoDlJupyterHostProofTest {
    String key(){byte[] type="ssh-ed25519".getBytes(StandardCharsets.US_ASCII);ByteBuffer b=ByteBuffer.allocate(51);b.putInt(type.length).put(type).putInt(32).put(new byte[32]);return Base64.getEncoder().encodeToString(b.array());}
    @Test void exactProviderOriginOnly() throws Exception {assertEquals("https://fixture.seetacloud.com:8443",AutoDlJupyterHostProof.origin("fixture.seetacloud.com:8443"));for(String bad:new String[]{"autodl.com.evil.test","127.0.0.1","user@fixture.autodl.com","fixture.autodl.com/path","fixture.autodl.com?token=secret","fixture.autodl.com:8080","fixture..autodl.com"})assertThrows(java.io.IOException.class,()->AutoDlJupyterHostProof.origin(bad));}
    @Test void onlyCompletedNonceDelimitedPublicKeyIsAccepted() throws Exception {String text="echoed printf '\\nNONCE\\n'\r\nNONCE\r\nssh-ed25519 "+key()+" root@fixture\r\nNONCE_END\r\n";assertEquals(key(),AutoDlJupyterHostProof.extract(text,"NONCE"));assertNull(AutoDlJupyterHostProof.extract(text,"DIFFERENT_NONCE"));}
    @Test void echoedCommandAndPartialResponseAreNotProof() throws Exception {assertNull(AutoDlJupyterHostProof.extract("printf '\\nNONCE\\n'; cat /etc/ssh/ssh_host_ed25519_key.pub","NONCE"));assertNull(AutoDlJupyterHostProof.extract("\nNONCE\nssh-ed25519 "+key(),"NONCE"));}
    @Test void missingDuplicateOrMalformedKeysFailClosed(){for(String value:new String[]{"no key","ssh-ed25519 AAAA","ssh-ed25519 "+key()+"\nssh-ed25519 "+key()})assertThrows(java.io.IOException.class,()->AutoDlJupyterHostProof.extract("\nNONCE\n"+value+"\nNONCE_END","NONCE"));}
    com.fasterxml.jackson.databind.node.ObjectNode snapshot(){return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("jupyter_domain","fixture.seetacloud.com:8443").put("jupyter_token","SYNTHETIC_TOKEN");}
    @Test void originTokenAndPublicKeyValidationHaveFixedSafeStages() {
        assertStage("JUPYTER_ORIGIN",()->AutoDlJupyterHostProof.origin("https://secret.invalid/?token=SYNTHETIC_TOKEN"));
        AutoDlJupyterHostProof proof=new AutoDlJupyterHostProof();
        assertStage("JUPYTER_TOKEN",()->proof.read(snapshot().put("jupyter_token","")));
        assertStage("JUPYTER_HOST_KEY",()->AutoDlJupyterHostProof.extract("\nNONCE\nno key\nNONCE_END","NONCE"));
    }
    @Test void terminalHttpFailurePreservesStageAndTypeWithoutResponseBody() {
        AutoDlJupyterHostProof proof=new AutoDlJupyterHostProof();org.springframework.web.client.RestTemplate http=mock(org.springframework.web.client.RestTemplate.class);
        org.springframework.test.util.ReflectionTestUtils.setField(proof,"http",http);
        org.springframework.web.client.HttpClientErrorException rejected=org.springframework.web.client.HttpClientErrorException.create(
            org.springframework.http.HttpStatus.FORBIDDEN,"SYNTHETIC_TOKEN",org.springframework.http.HttpHeaders.EMPTY,"SYNTHETIC_PASSWORD".getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8);
        when(http.exchange(anyString(),eq(org.springframework.http.HttpMethod.POST),any(org.springframework.http.HttpEntity.class),eq(String.class))).thenThrow(rejected);
        AutoDlRuntimeSsh.PreparationException failure=assertThrows(AutoDlRuntimeSsh.PreparationException.class,()->proof.read(snapshot()));
        assertEquals("JUPYTER_TERMINAL_CREATE",failure.getMessage());assertEquals("JUPYTER_TERMINAL_CREATE",AutoDlRuntimeSsh.safeDiagnostic(failure));
        assertEquals("HTTP",AutoDlRuntimeSsh.safeFailureType(failure));assertSanitized(failure);
        verifyNoMoreInteractionsAfterCreate(http);
    }
    @Test void malformedTerminalReplyNeverOpensAWebsocket() {
        javax.websocket.WebSocketContainer socket=mock(javax.websocket.WebSocketContainer.class);
        AutoDlJupyterHostProof proof=new AutoDlJupyterHostProof(){@Override protected javax.websocket.WebSocketContainer container(){return socket;}};
        org.springframework.web.client.RestTemplate http=mock(org.springframework.web.client.RestTemplate.class);org.springframework.test.util.ReflectionTestUtils.setField(proof,"http",http);
        when(http.exchange(anyString(),eq(org.springframework.http.HttpMethod.POST),any(org.springframework.http.HttpEntity.class),eq(String.class)))
            .thenReturn(org.springframework.http.ResponseEntity.ok("{\"name\":\"../SYNTHETIC_TOKEN\"}"));
        assertStage("JUPYTER_TERMINAL_RESPONSE",()->proof.read(snapshot()));verifyNoInteractions(socket);
        verifyNoMoreInteractionsAfterCreate(http);
    }
    @Test void websocketFailureIsClassifiedAndOnlyItsOwnedTerminalIsDeleted() throws Exception {
        javax.websocket.WebSocketContainer socket=mock(javax.websocket.WebSocketContainer.class);
        AutoDlJupyterHostProof proof=new AutoDlJupyterHostProof(){@Override protected javax.websocket.WebSocketContainer container(){return socket;}};
        org.springframework.web.client.RestTemplate http=mock(org.springframework.web.client.RestTemplate.class);org.springframework.test.util.ReflectionTestUtils.setField(proof,"http",http);
        when(http.exchange(anyString(),eq(org.springframework.http.HttpMethod.POST),any(org.springframework.http.HttpEntity.class),eq(String.class)))
            .thenReturn(org.springframework.http.ResponseEntity.ok("{\"name\":\"fixture-terminal\"}"));
        javax.websocket.DeploymentException rejected=new javax.websocket.DeploymentException("SYNTHETIC_TOKEN wss://secret.invalid");
        when(socket.connectToServer(any(javax.websocket.Endpoint.class),any(javax.websocket.ClientEndpointConfig.class),any(java.net.URI.class))).thenThrow(rejected);
        AutoDlRuntimeSsh.PreparationException failure=assertThrows(AutoDlRuntimeSsh.PreparationException.class,()->proof.read(snapshot()));
        assertEquals("JUPYTER_WEBSOCKET",AutoDlRuntimeSsh.safeDiagnostic(failure));assertEquals("WEBSOCKET",AutoDlRuntimeSsh.safeFailureType(failure));assertSanitized(failure);
        verify(http).exchange(eq("https://fixture.seetacloud.com:8443/api/terminals/fixture-terminal"),eq(org.springframework.http.HttpMethod.DELETE),any(org.springframework.http.HttpEntity.class),eq(Void.class));
    }
    void verifyNoMoreInteractionsAfterCreate(org.springframework.web.client.RestTemplate http){verify(http).exchange(anyString(),eq(org.springframework.http.HttpMethod.POST),any(org.springframework.http.HttpEntity.class),eq(String.class));verifyNoMoreInteractions(http);}
    void assertStage(String stage,org.junit.jupiter.api.function.Executable action){AutoDlRuntimeSsh.PreparationException failure=assertThrows(AutoDlRuntimeSsh.PreparationException.class,action);assertEquals(stage,failure.getMessage());assertEquals(stage,AutoDlRuntimeSsh.safeDiagnostic(failure));}
    void assertSanitized(Throwable failure){assertNull(failure.getCause());java.io.StringWriter text=new java.io.StringWriter();failure.printStackTrace(new java.io.PrintWriter(text));for(String secret:new String[]{"SYNTHETIC_PASSWORD","SYNTHETIC_TOKEN","secret.invalid","https://","wss://"})assertFalse(text.toString().contains(secret));}
}
