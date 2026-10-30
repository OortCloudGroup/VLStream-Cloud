package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Real header construction and response parsing, with no sockets or real credentials. */
@Tag("dev")
class NewApiBillingSelfIdentityTest {
    private static ComputePlatformAuthorization.Context actor() {
        ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);
        actor.setMode("single");actor.setVlsUserId("fixture-vls-user");actor.setVlsTenantId("fixture-local-tenant");
        actor.setPlatformUserId("fixture-platform-user");actor.setPlatformTenantId("fixture-platform-tenant");return actor;
    }
    private static NewApiBillingClient.SelfAuthorization authorization(ComputePlatformAuthorization.Context actor) {
        return new NewApiBillingClient.SelfAuthorization(actor,"FIXTURE_SSO_PROOF","app","FIXTURE_APP","FIXTURE_GATEWAY_SECRET");
    }
    private static class Connection extends HttpURLConnection {
        int status=200;
        String body="{\"success\":true,\"data\":{\"id\":123}}";
        boolean disconnected;
        Connection(URI target) throws java.io.IOException { super(target.toURL()); }
        @Override public void connect() { fail("Fixture must not connect to a network"); }
        @Override public void disconnect() { disconnected=true; }
        @Override public boolean usingProxy() { return false; }
        @Override public int getResponseCode() { return status; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)); }
        @Override public InputStream getErrorStream() { return getInputStream(); }
    }
    private static class Client extends NewApiBillingClient {
        final List<Connection> sent=new ArrayList<>();
        int responseStatus=200;
        String responseBody="{\"success\":true,\"data\":{\"id\":123}}";
        Client(ExternalBillingProperties properties) { super(properties,new ObjectMapper()); }
        @Override protected HttpURLConnection openConnection(URI target) throws java.io.IOException {
            Connection connection=new Connection(target);connection.status=responseStatus;connection.body=responseBody;sent.add(connection);return connection;
        }
    }
    private static ExternalBillingProperties properties() {
        ExternalBillingProperties p=new ExternalBillingProperties();p.setEnabled(true);p.setBaseUrl("https://fixture.invalid/bus/apaas-newapi");
        p.setMerchantId("fixture-merchant");p.setApiKey("FIXTURE_MERCHANT_KEY");return p;
    }

    @Test void selfUsesVerifiedSsoBearerAndGatewayContextWithAuthoritativeResponseId() {
        Client client=new Client(properties());ComputePlatformAuthorization.Context actor=actor();
        BillingPayer payer=client.resolveSelf(actor,authorization(actor));
        assertEquals("123",payer.userId);assertEquals("fixture-local-tenant",payer.tenantId);
        Connection sent=client.sent.get(0);
        assertEquals("GET",sent.getRequestMethod());assertEquals("/bus/apaas-newapi/api/user/self",sent.getURL().getPath());
        assertEquals("FIXTURE_SSO_PROOF",sent.getRequestProperty("accesstoken"));
        assertEquals("fixture-platform-tenant",sent.getRequestProperty("tenantid"));
        assertEquals("app",sent.getRequestProperty("requesttype"));assertEquals("FIXTURE_APP",sent.getRequestProperty("appid"));
        assertEquals("FIXTURE_GATEWAY_SECRET",sent.getRequestProperty("secretkey"));
        assertEquals("application/json",sent.getRequestProperty("Accept"));
        assertEquals("Bearer FIXTURE_SSO_PROOF",sent.getRequestProperty("Authorization"));assertNull(sent.getRequestProperty("New-Api-User"));
        assertNull(sent.getRequestProperty("Idempotency-Key"));assertEquals(7,sent.getRequestProperties().size());
        assertTrue(sent.disconnected);assertFalse(sent.getInstanceFollowRedirects());
    }

    @Test void merchantRequestsKeepTheirOwnBearerWithoutAnySsoGatewayHeaders() {
        Client client=new Client(properties());ComputePlatformAuthorization.Context actor=actor();
        client.resolveSelf(actor,authorization(actor));client.config();
        Connection merchant=client.sent.get(1);
        assertEquals("Bearer FIXTURE_MERCHANT_KEY",merchant.getRequestProperty("Authorization"));
        for(String name:new String[]{"accesstoken","tenantid","appid","secretkey","requesttype"})assertNull(merchant.getRequestProperty(name));
        assertEquals("/bus/apaas-newapi/api/v1/external-billing/config",merchant.getURL().getPath());
    }

    @Test void selfDoesNotRequireOrSubstituteAMerchantKey() {
        ExternalBillingProperties p=properties();p.setApiKey(null);p.setMerchantId(null);
        Client client=new Client(p);ComputePlatformAuthorization.Context actor=actor();
        assertEquals("123",client.resolveSelf(actor,authorization(actor)).userId);
        assertEquals("Bearer FIXTURE_SSO_PROOF",client.sent.get(0).getRequestProperty("Authorization"));
    }

    @Test void aChangedActorLocalTenantPlatformUserOrPlatformTenantCannotUseTheSnapshot() {
        for(int change=0;change<5;change++) {
            Client client=new Client(properties());ComputePlatformAuthorization.Context actor=actor();
            NewApiBillingClient.SelfAuthorization auth=authorization(actor);
            if(change==0)actor.setVlsUserId("other-user");if(change==1)actor.setVlsTenantId("other-local-tenant");
            if(change==2)actor.setPlatformUserId("other-platform-user");if(change==3)actor.setPlatformTenantId("other-platform-tenant");
            if(change==4)actor.setAuthorized(false);
            assertThrows(IllegalArgumentException.class,()->client.resolveSelf(actor,auth));assertTrue(client.sent.isEmpty());
        }
    }

    @Test void incompleteOrInjectedGatewayIdentityIsRejectedBeforeTransport() {
        ComputePlatformAuthorization.Context actor=actor();
        for(String value:new String[]{null,""," ","bad\r\nInjected: value"}) {
            assertThrows(IllegalArgumentException.class,()->new NewApiBillingClient.SelfAuthorization(actor,value,"app","FIXTURE_APP","FIXTURE_SECRET"));
            assertThrows(IllegalArgumentException.class,()->new NewApiBillingClient.SelfAuthorization(actor,"FIXTURE_SSO",value,"FIXTURE_APP","FIXTURE_SECRET"));
            assertThrows(IllegalArgumentException.class,()->new NewApiBillingClient.SelfAuthorization(actor,"FIXTURE_SSO","app",value,"FIXTURE_SECRET"));
            assertThrows(IllegalArgumentException.class,()->new NewApiBillingClient.SelfAuthorization(actor,"FIXTURE_SSO","app","FIXTURE_APP",value));
        }
        actor.setPlatformTenantId(null);assertThrows(IllegalArgumentException.class,()->authorization(actor));
    }

    @Test void failureOrRedirectNeverRetriesWithMerchantOrAnotherIdentity() {
        for(int status:new int[]{301,302,401,403,503}) {
            Client client=new Client(properties());client.responseStatus=status;
            ComputePlatformAuthorization.Context actor=actor();
            Exception failure=assertThrows(IllegalArgumentException.class,()->client.resolveSelf(actor,authorization(actor)));
            assertEquals(1,client.sent.size());assertEquals("Bearer FIXTURE_SSO_PROOF",client.sent.get(0).getRequestProperty("Authorization"));
            assertFalse(client.sent.get(0).getInstanceFollowRedirects());assertFalse(failure.getMessage().contains("FIXTURE"));
        }
    }

    @Test void upstreamSelfDiagnosticNeverIncludesResponseTextOrCredentials() {
        Client client=new Client(properties());
        client.responseBody="{\"success\":false,\"message\":\"invalid token FIXTURE_SSO_PROOF FIXTURE_MERCHANT_KEY FIXTURE_GATEWAY_SECRET\"}";
        ComputePlatformAuthorization.Context actor=actor();
        Exception failure=assertThrows(IllegalArgumentException.class,()->client.resolveSelf(actor,authorization(actor)));
        assertTrue(failure.getMessage().contains("success=false"));
        assertTrue(failure.getMessage().contains("reason=IDENTITY_REJECTED"));
        assertFalse(failure.getMessage().contains("FIXTURE"));
        assertTrue(failure.getMessage().contains("invalid token"));
        assertTrue(failure.getMessage().contains("[REDACTED]"));
    }

    @Test void invalidResponseCannotReplacePayerWithSubjectUsernameOrClientIds() {
        for(String body:new String[]{"{\"success\":true,\"data\":{\"username\":\"fixture-user\"}}",
            "{\"success\":true,\"data\":{\"id\":\"not-an-integer\"}}","{\"success\":false,\"data\":{\"id\":123}}",
            "{\"success\":\"true\",\"data\":{\"id\":123}}","{\"id\":123}"}) {
            Client client=new Client(properties());client.responseBody=body;ComputePlatformAuthorization.Context actor=actor();
            assertThrows(IllegalArgumentException.class,()->client.resolveSelf(actor,authorization(actor)));
        }
    }

    @Test void disabledOrNonTlsTargetDoesNotSendTheVerifiedContext() {
        ExternalBillingProperties disabled=properties();disabled.setEnabled(false);
        Client client=new Client(disabled);ComputePlatformAuthorization.Context actor=actor();
        assertThrows(IllegalStateException.class,()->client.resolveSelf(actor,authorization(actor)));assertTrue(client.sent.isEmpty());
        for(String url:new String[]{"http://fixture.invalid","https://user@fixture.invalid","https://fixture.invalid?target=other","https://fixture.invalid#other"}) {
            ExternalBillingProperties p=properties();p.setBaseUrl(url);Client rejected=new Client(p);
            assertThrows(IllegalArgumentException.class,()->rejected.resolveSelf(actor,authorization(actor)));assertTrue(rejected.sent.isEmpty());
        }
    }

    @Test void credentialContainerHasNoPublicBeanAccessorsOrSecretBearingString() {
        NewApiBillingClient.SelfAuthorization auth=authorization(actor());
        assertFalse(auth.toString().contains("FIXTURE"));assertFalse(((Object)auth) instanceof java.io.Serializable);
        for(java.lang.reflect.Method method:auth.getClass().getDeclaredMethods())assertFalse(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }
}
