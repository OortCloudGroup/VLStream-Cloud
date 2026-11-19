package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ComputeBillingConsentTest {
    final ObjectMapper json = new ObjectMapper();
    final ComputePlatformAuthorization.Context actor = actor();
    final NewApiBillingClient.SelfAuthorization proof = new NewApiBillingClient.SelfAuthorization(actor,"TEST_USER_PROOF","app","TEST_APP","TEST_SECRET");
    ComputePlatformAuthorization.Context actor() {
        ComputePlatformAuthorization.Context value = new ComputePlatformAuthorization.Context();
        value.setAuthorized(true); value.setVlsUserId("actor"); value.setVlsTenantId("tenant");
        value.setPlatformUserId("subject"); value.setPlatformTenantId("platform-tenant"); return value;
    }
    ExternalBillingProperties properties() {
        ExternalBillingProperties value = new ExternalBillingProperties();
        value.setBaseUrl("https://fixture.invalid"); value.setMerchantId("merchant"); value.setApiKey("TEST_MERCHANT_KEY"); return value;
    }
    String grant(String state, String scopes) {
        return "{\"success\":true,\"data\":[{\"user_id\":6,\"merchant_id\":\"merchant\",\"status\":\""+state+"\",\"scopes\":"+scopes+"}]}";
    }
    class Fixture extends NewApiBillingClient {
        String self="{\"success\":true,\"data\":{\"id\":6}}", before="{\"success\":true,\"data\":[]}";
        String after=grant("ACTIVE","\"[QUERY_BALANCE,CHARGE_GPU]\""), posted;
        int reads, writes, grantHttp=200, postHttp=200;
        Fixture() { super(properties(),json); }
        Reply reply(int status,String body) { try { return new Reply(status,json.readTree(body)); } catch(Exception e) { throw new AssertionError(e); } }
        @Override protected Reply readSelf(SelfAuthorization authorization) { assertSame(proof,authorization);return reply(200,self); }
        @Override protected Reply readUserGrants(SelfAuthorization authorization) { assertSame(proof,authorization);return reply(grantHttp,reads++==0?before:after); }
        @Override protected Reply postUserGrant(SelfAuthorization authorization,String body) { assertSame(proof,authorization);writes++;posted=body;return reply(postHttp,"{\"success\":true}"); }
        BillingPayer payer() throws Exception { return BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":6}")); }
        void accept() throws Exception { acceptBillingPermissions(actor,proof,payer()); }
    }
    @Test void missingGrantUsesOnlyConfiguredMerchantAndExactScopesThenRechecks() throws Exception {
        Fixture client=new Fixture();client.accept();
        assertEquals(1,client.writes);assertEquals(2,client.reads);
        assertEquals(json.readTree("{\"merchant_id\":\"merchant\",\"scopes\":[\"QUERY_BALANCE\",\"CHARGE_GPU\"]}"),json.readTree(client.posted));
        assertFalse(client.posted.contains("user_id"));assertFalse(client.posted.contains("TEST_"));
    }
    @ParameterizedTest @ValueSource(strings={"[\"QUERY_BALANCE\",\"CHARGE_GPU\"]","\"[QUERY_BALANCE,CHARGE_GPU]\"","\"QUERY_BALANCE,CHARGE_GPU\""})
    void existingActiveGrantNeverPosts(String scopes) throws Exception {
        Fixture client=new Fixture();client.before=grant("ACTIVE",scopes);client.accept();assertEquals(0,client.writes);
    }
    @ParameterizedTest @ValueSource(strings={"REVOKED","UNKNOWN",""})
    void existingNonActiveGrantIsNeverRestored(String state) {
        Fixture client=new Fixture();client.before=grant(state,"[\"QUERY_BALANCE\",\"CHARGE_GPU\"]");
        assertThrows(IllegalStateException.class,client::accept);assertEquals(0,client.writes);
    }
    @Test void limitedGrantIsNeverSilentlyExpanded() {
        Fixture client=new Fixture();client.before=grant("ACTIVE","[\"QUERY_BALANCE\"]");
        assertEquals("BILLING_GRANT_SCOPES_MISSING",assertThrows(IllegalStateException.class,client::accept).getMessage());assertEquals(0,client.writes);
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"success\":false,\"data\":[]}","{\"success\":true,\"data\":null}","{\"success\":true,\"data\":[{\"user_id\":7,\"merchant_id\":\"merchant\"}]}"})
    void malformedOrForeignGrantListCannotProduceConsent(String body) {
        Fixture client=new Fixture();client.before=body;assertThrows(IllegalStateException.class,client::accept);assertEquals(0,client.writes);
    }
    @ParameterizedTest @ValueSource(ints={0,401,403,500})
    void grantLookupFailureNeverWrites(int status) {
        Fixture client=new Fixture();client.grantHttp=status;assertThrows(IllegalStateException.class,client::accept);assertEquals(0,client.writes);
    }
    @Test void changedPayerOrActorCannotGrant() {
        Fixture client=new Fixture();client.self="{\"success\":true,\"data\":{\"id\":7}}";
        assertThrows(IllegalStateException.class,client::accept);assertEquals(0,client.reads);assertEquals(0,client.writes);
        actor.setPlatformUserId("other-subject");assertThrows(IllegalArgumentException.class,client::accept);assertEquals(0,client.writes);
    }
    @Test void successfulPostWithoutConfirmedGrantIsNotAcceptedOrReposted() {
        Fixture client=new Fixture();client.after=client.before;
        assertThrows(IllegalStateException.class,client::accept);assertEquals(1,client.writes);
    }
    @Test void uncertainPostIsNotRetried() {
        Fixture client=new Fixture();client.postHttp=0;
        assertThrows(IllegalStateException.class,client::accept);assertEquals(1,client.writes);assertEquals(1,client.reads);
    }
    @Test void concurrentRevocationObservedAfterPostIsNotRestoredAgain() {
        Fixture client=new Fixture();client.after=grant("REVOKED","[\"QUERY_BALANCE\",\"CHARGE_GPU\"]");
        assertEquals("BILLING_GRANT_REVOKED",assertThrows(IllegalStateException.class,client::accept).getMessage());assertEquals(1,client.writes);
    }
    @Test void realTransportUsesOnlyVerifiedUserProofForConsentAndNeverMerchantOrLocalToken() throws Exception {
        com.sun.net.httpserver.HttpServer server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        java.util.List<String> requests=java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.concurrent.atomic.AtomicBoolean created=new java.util.concurrent.atomic.AtomicBoolean();
        server.createContext("/api/user/",exchange->{
            String auth=exchange.getRequestHeaders().getFirst("Authorization");
            String method=exchange.getRequestMethod(),path=exchange.getRequestURI().getPath();
            requests.add(method+" "+path+" "+auth+" "+exchange.getRequestHeaders().getFirst("tenantid"));
            String body;
            if(path.equals("/api/user/self"))body="{\"success\":true,\"data\":{\"id\":6}}";
            else if(method.equals("POST")) {
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                byte[] buffer=new byte[1024];int size;while((size=exchange.getRequestBody().read(buffer))!=-1)bytes.write(buffer,0,size);
                requests.add(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
                created.set(true);body="{\"success\":true}";
            } else body=created.get()?grant("ACTIVE","[\"QUERY_BALANCE\",\"CHARGE_GPU\"]"):"{\"success\":true,\"data\":[]}";
            byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);
            try(java.io.OutputStream out=exchange.getResponseBody()){out.write(bytes);}
        });
        server.start();
        try {
            NewApiBillingClient client=new NewApiBillingClient(properties(),json) {
                @Override protected java.net.HttpURLConnection openConnection(java.net.URI target) throws java.io.IOException {
                    return (java.net.HttpURLConnection)new java.net.URL("http://127.0.0.1:"+server.getAddress().getPort()+target.getPath()).openConnection();
                }
            };
            client.acceptBillingPermissions(actor,proof,BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":6}")));
            assertEquals(5,requests.size());
            for(String entry:requests)if(!entry.startsWith("{"))assertTrue(entry.endsWith("Bearer TEST_USER_PROOF platform-tenant"));
            assertFalse(requests.toString().contains("TEST_MERCHANT_KEY"));
            assertEquals(1,requests.stream().filter(value->value.startsWith("POST ")).count());
        } finally {server.stop(0);}
    }
}
