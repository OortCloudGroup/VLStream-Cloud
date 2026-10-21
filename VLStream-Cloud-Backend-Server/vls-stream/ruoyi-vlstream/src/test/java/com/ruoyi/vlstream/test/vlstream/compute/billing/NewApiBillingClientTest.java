package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class NewApiBillingClientTest {
    final ObjectMapper json = new ObjectMapper();
    ExternalBillingProperties properties() {
        ExternalBillingProperties properties = new ExternalBillingProperties(); properties.setEnabled(true);
        properties.setMerchantId("test-only-merchant"); properties.setApiKey("TEST_ONLY_NOT_A_REAL_CREDENTIAL"); return properties;
    }
    class Fixture extends NewApiBillingClient {
        int code = 200; String body = "{}", path, bearer;
        Fixture() { super(properties(), json); }
        @Override protected Reply read(String path, String bearer) {
            this.path = path; this.bearer = bearer;
            try { return new Reply(code, json.readTree(body)); } catch (Exception e) { throw new IllegalArgumentException("TEST_ONLY_BAD_FIXTURE"); }
        }
    }
    @Test void documentedOperationStatusesAreTypedAndMalformedOr5xxStayUnknown() {
        Fixture client = new Fixture();
        client.body = "{\"status\":\"PROCESSING\"}"; assertEquals(ExternalBillingGateway.Status.PROCESSING, client.byKey("fixed-key").status);
        client.body = "{\"status\":\"REJECTED\",\"error_code\":\"INSUFFICIENT_AVAILABLE_QUOTA\"}";
        assertEquals("INSUFFICIENT_AVAILABLE_QUOTA", client.byKey("fixed-key").errorCode);
        client.code = 503; assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.byKey("fixed-key").status);
        client.code = 200; client.body = "{\"status\":\"REJECTED\"}"; assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.byKey("fixed-key").status);
    }
    @Test void genericProxy404IsNotFinancialNotFound() {
        Fixture client = new Fixture(); client.code = 404;
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.byReference("fixed-ref").status);
        client.body = "{\"error\":\"CHARGE_NOT_FOUND\"}"; assertEquals(ExternalBillingGateway.Status.NOT_FOUND, client.byReference("fixed-ref").status);
    }
    @Test void revokedAuthorizationAndMerchantIdentityFailureAreDistinctFromMissingOperation() {
        Fixture client = new Fixture();
        for (int status : new int[]{401,403}) { client.code = status; assertEquals(ExternalBillingGateway.Status.NOT_AUTHORIZED, client.byKey("key").status); }
        client.code = 409; assertEquals(ExternalBillingGateway.Status.CONFLICT, client.byReference("ref").status);
    }
    @Test void largeIntegerBalanceIsExactAndMustMatchUserAndHeldArithmetic() {
        Fixture client = new Fixture();
        client.body = "{\"user_id\":123,\"balance_quota\":\"9007199254740993\",\"held_quota\":\"1\",\"available_quota\":\"9007199254740992\"}";
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.balance("123").status);
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.balance("124").status);
        client.body = "{\"user_id\":123,\"balance_quota\":\"10\",\"held_quota\":\"8\",\"available_quota\":\"3\"}";
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.balance("123").status);
    }
    @Test void balanceAmountsMustBeIntegerStrings() {
        Fixture client = new Fixture(); client.body = "{\"user_id\":123,\"balance_quota\":10,\"held_quota\":\"0\",\"available_quota\":\"10\"}";
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.balance("123").status);
    }
    @Test void referenceIsEncodedAsSinglePathSegment() {
        Fixture client = new Fixture(); client.byReference("vls:session:0");
        assertEquals("/api/v1/external-billing/charges/by-reference/vls%3Asession%3A0", client.path);
    }
    @Test void selfUsesUserProofNotMerchantCredentialAndDoesNotAcceptCallerPayer() {
        Fixture client = new Fixture(); client.body = "{\"success\":true,\"data\":{\"id\":123}}";
        ComputePlatformAuthorization.Context actor = new ComputePlatformAuthorization.Context(); actor.setAuthorized(true);
        actor.setVlsUserId("actor-test"); actor.setVlsTenantId("tenant-test"); actor.setPlatformUserId("platform-test");
        BillingPayer payer = client.resolveSelf(actor, "TEST_ONLY_USER_PROOF");
        assertEquals("123", payer.userId); assertEquals("/api/user/self", client.path); assertEquals("TEST_ONLY_USER_PROOF", client.bearer);
    }
    @Test void humanReadableIsNeverUsedAsExchangeRatio() {
        Fixture client = new Fixture(); client.body = "{\"human_readable\":\"1 CNY = 999999 quota\"}";
        JsonNode config = client.config(); assertFalse(config.has("quota_per_currency_unit"));
        assertThrows(IllegalArgumentException.class, () -> ExactQuota.compute(java.math.BigDecimal.ONE, 1000, config.path("numerator").asText(), "1"));
    }
    @Test void realAdapterHasNoFinancialWriteFallbackEvenWhenEnabled() {
        Fixture client = new Fixture(); assertFalse(client.supportsAtomicCapture());
        assertThrows(IllegalStateException.class, () -> client.capture("original-key", "{}")); assertNull(client.path);
    }
    @Test void loopbackHttpContractDoesNotFollowRedirectOrLeakBearerToRedirectTarget() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicInteger redirected = new AtomicInteger(), requests = new AtomicInteger();
        server.createContext("/api/v1/external-billing/operations/by-key/", exchange -> {
            requests.incrementAndGet(); exchange.getResponseHeaders().add("Location", "/redirect-target");
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(302,response.length);
            try (java.io.OutputStream stream = exchange.getResponseBody()) { stream.write(response); }
        });
        server.createContext("/redirect-target", exchange -> { redirected.incrementAndGet(); exchange.sendResponseHeaders(200,0); exchange.close(); });
        server.start();
        try {
            ExternalBillingProperties p = properties(); p.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            assertEquals(ExternalBillingGateway.Status.UNKNOWN, new NewApiBillingClient(p,json).byKey("key-test").status);
            assertEquals(1, requests.get()); assertEquals(0, redirected.get());
        } finally { server.stop(0); }
    }
    @Test void disabledOrMissingIdentityDoesNotAttemptTransport() {
        ExternalBillingProperties p = properties(); p.setEnabled(false);
        assertThrows(IllegalStateException.class, () -> new NewApiBillingClient(p,json).byKey("key-test"));
        p.setEnabled(true); p.setApiKey(null);
        assertThrows(IllegalStateException.class, () -> new NewApiBillingClient(p,json).byKey("key-test"));
    }
}
