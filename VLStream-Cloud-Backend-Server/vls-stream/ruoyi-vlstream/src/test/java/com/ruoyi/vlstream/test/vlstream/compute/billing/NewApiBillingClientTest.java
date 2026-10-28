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

    class WriteFixture extends NewApiBillingClient {
        int code = 200, reads, writes; String response = "{}", path, key, original;
        WriteFixture() { super(writeProperties(), json); }
        @Override protected Reply read(String path, String bearer) { reads++; this.path = path; return reply(); }
        @Override protected Reply post(String path, String bearer, String key, String body) {
            writes++; this.path = path; this.key = key; original = body; return reply();
        }
        Reply reply() { try { return new Reply(code, json.readTree(response)); } catch (Exception e) { throw new IllegalArgumentException("bad fixture"); } }
    }
    ExternalBillingProperties writeProperties() { ExternalBillingProperties p = properties(); p.setAllowWrites(true); return p; }
    String captureBody() { return "{ \"merchant_id\":\"test-only-merchant\",\"merchant_charge_id\":\"charge-1\",\"user_id\":123,"
        + "\"hold_id\":9007199254740993,\"business_type\":\"GPU_COMPUTE\",\"usage_session_id\":\"session-1\","
        + "\"charge_quota\":\"11\",\"conversion_version\":\"v1-test\" }"; }
    String captureReceipt() { return "{\"status\":\"SUCCEEDED\",\"operation_id\":\"1\",\"charge_id\":\"2\",\"ledger_entry_id\":\"3\","
        + "\"charged_quota\":\"11\",\"available_quota\":\"99\",\"balance_version\":4}"; }
    String holdDetail() { return "{\"hold_id\":101,\"merchant_hold_id\":\"hold-1\",\"user_id\":123,\"hold_quota\":\"20\","
        + "\"captured_quota\":\"5\",\"released_quota\":\"0\",\"remaining_quota\":\"15\",\"hold_status\":\"PARTIALLY_CAPTURED\","
        + "\"expires_at\":1700000000001,\"balance_version\":4,\"created_at\":1700000000,\"updated_at\":1700000001}"; }

    @Test void fundsMovementRequiresSeparateDefaultOffPermission() {
        ExternalBillingProperties defaults = new ExternalBillingProperties(); assertFalse(defaults.isEnabled()); assertFalse(defaults.isAllowWrites());
        defaults.setEnabled(true); defaults.setMerchantId("test-only-merchant");
        NewApiBillingClient client = new NewApiBillingClient(defaults, json);
        assertFalse(client.supportsAtomicCapture());
        assertThrows(IllegalStateException.class, () -> client.capture("key-1", captureBody()));
        assertThrows(IllegalStateException.class, () -> client.createHold("key-1", "{}"));
        assertThrows(IllegalStateException.class, () -> client.releaseHold("101", "key-1", "{}"));
        assertThrows(IllegalStateException.class, () -> client.topupHold("101", "key-1", "{}"));
        assertThrows(IllegalStateException.class, () -> client.refund("101", "key-1", "{}"));
    }

    @Test void captureUsesOnlyAtomicChargePathAndPreservesOriginalKeyAndBytes() {
        WriteFixture client = new WriteFixture(); client.response = captureReceipt();
        assertTrue(client.supportsAtomicCapture());
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.capture("same-original-key", captureBody()).status);
        assertEquals("/api/v1/external-billing/charges", client.path); assertEquals(captureBody(), client.original); assertEquals("same-original-key", client.key);
        assertEquals(0, client.reads); assertEquals(1, client.writes);
        for (String invalid : new String[]{captureBody().replace("\"hold_id\":9007199254740993,", ""), captureBody().replace("9007199254740993", "0"),
            captureBody().replace("9007199254740993", "1.0"), captureBody().replace("test-only-merchant", "other-merchant")})
            assertThrows(IllegalArgumentException.class, () -> client.capture("same-original-key", invalid));
        assertEquals(1, client.writes);
    }

    @Test void fundingErrorsRemainTypedAndAllServerFailuresRemainUnknown() {
        WriteFixture client = new WriteFixture();
        for (String error : new String[]{"INSUFFICIENT_HELD_QUOTA", "HOLD_EXPIRED", "HOLD_MISMATCH", "INVALID_REQUEST"}) {
            client.code = 422; client.response = "{\"error\":\"" + error + "\"}";
            ExternalBillingGateway.Result result = client.capture("key-1", captureBody());
            assertEquals(ExternalBillingGateway.Status.REJECTED, result.status); assertEquals(error, result.errorCode);
        }
        client.code = 409; client.response = "{\"error\":\"BUSINESS_REFERENCE_CONFLICT\"}";
        assertEquals("BUSINESS_REFERENCE_CONFLICT", client.capture("key-1", captureBody()).errorCode);
        assertEquals(ExternalBillingGateway.Status.CONFLICT, client.capture("key-1", captureBody()).status);
        for (int code : new int[]{500,502,503}) { client.code = code; assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.capture("key-1", captureBody()).status); }
    }

    @Test void topupAndReleaseCarryOwnBusinessReferencesAndRejectLegacyReleaseSending() {
        WriteFixture client = new WriteFixture();
        client.response = "{\"hold_id\":101,\"hold_quota\":\"20\",\"captured_quota\":\"5\",\"released_quota\":\"0\",\"remaining_quota\":\"15\",\"hold_status\":\"PARTIALLY_CAPTURED\",\"balance_version\":2}";
        String topup = FundingContracts.topup("test-only-merchant", "topup-1", "123", "10", "reserve").toString();
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.topupHold("101", "topup-key", topup).status);
        assertEquals(topup, client.original); assertEquals("/api/v1/external-billing/holds/101/topup", client.path);
        client.response = "{\"hold_id\":101,\"held_quota\":\"0\",\"released_quota\":\"15\",\"remaining_quota\":\"0\",\"hold_status\":\"RELEASED\"}";
        String release = FundingContracts.release("test-only-merchant", "release-1", "123", "999").toString();
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.releaseHold("101", "release-key", release).status);
        assertEquals(release, client.original); assertEquals("release-key", client.key);
        assertThrows(IllegalArgumentException.class, () -> client.releaseHold("101", "release-key", FundingContracts.release("test-only-merchant", "123", "999").toString()));
        assertEquals(2, client.writes);
        client.response = client.response.replace("101", "102");
        assertEquals(ExternalBillingGateway.Status.CONFLICT, client.releaseHold("101", "release-key", release).status);
    }

    @Test void holdQueriesVerifyDetailIdentityArithmeticAndExactExpiry() {
        WriteFixture client = new WriteFixture(); client.response = holdDetail();
        ExternalBillingGateway.Result result = client.holdById("101");
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, result.status);
        assertEquals("1700000000001", result.charge.path("expires_at").asText());
        assertEquals(ExternalBillingGateway.Status.CONFLICT, client.holdById("102").status);
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.holdByReference("hold-1").status);
        assertEquals(ExternalBillingGateway.Status.CONFLICT, client.holdByReference("other-reference").status);
        client.response = holdDetail().replace("\"15\"", "\"16\"");
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.holdById("101").status);
    }

    @Test void undefinedHoldOperationResponseNeverFabricatesSuccessOrNotFound() {
        WriteFixture client = new WriteFixture(); client.response = "{\"status\":\"SUCCEEDED\",\"hold_id\":101}";
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.holdOperation("RELEASE", "vls:release:1").status);
        assertEquals("/api/v1/external-billing/hold-operations/RELEASE/vls%3Arelease%3A1", client.path);
        client.code = 404; client.response = "{\"error\":\"OPERATION_NOT_FOUND\"}";
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.holdOperation("RELEASE", "vls:release:1").status);
        assertThrows(IllegalArgumentException.class, () -> client.holdOperation("CAPTURE", "ref"));
    }

    @Test void holdCreationAcceptsDocumentedResponseButNotErrorDisguisedAsSuccess() {
        WriteFixture client = new WriteFixture();
        String request = FundingContracts.hold("test-only-merchant", "hold-1", "123", "20", "1700000000001", "reserve").toString();
        client.response = "{\"hold_id\":101,\"available_quota\":\"80\",\"held_quota\":\"20\"}";
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.createHold("hold-key", request).status);
        client.response = "{\"error\":\"BAD_STATE\",\"hold_id\":101,\"available_quota\":\"80\",\"held_quota\":\"20\"}";
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.createHold("hold-key", request).status);
        client.response = "{\"success\":true,\"data\":{\"error\":\"BAD_STATE\",\"hold_id\":101,\"available_quota\":\"80\",\"held_quota\":\"20\"}}";
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.createHold("hold-key", request).status);
    }

    @Test void malformedSuccessEnvelopeCannotConfirmCaptureOrAuthoritativeSelf() {
        WriteFixture client = new WriteFixture();
        for (String bad : new String[]{"{\"status\":\"SUCCEEDED\"}", "{\"success\":\"true\",\"data\":" + captureReceipt() + "}",
            captureReceipt().replace("\"11\"", "11")}) {
            client.response = bad; assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.capture("key-1", captureBody()).status);
        }
        Fixture self = new Fixture();
        for (String bad : new String[]{"{\"id\":123}", "{\"success\":\"true\",\"data\":{\"id\":123}}", "{\"success\":true,\"id\":123}"}) {
            self.body = bad; assertThrows(IllegalArgumentException.class, () -> self.resolveSelf(new ComputePlatformAuthorization.Context(), "TEST_ONLY_PROOF"));
        }
    }

    @Test void refundUsesOriginalChargePathAndIndependentPersistedKeyWithoutAssumingAvailableBalance() {
        WriteFixture client = new WriteFixture();
        String body = "{\"merchant_id\":\"test-only-merchant\",\"merchant_refund_id\":\"refund-1\",\"original_ledger_entry_id\":\"3\",\"refund_quota\":\"5\",\"reason_code\":\"OVERCHARGED\"}";
        client.response = "{\"status\":\"SUCCEEDED\",\"operation_id\":\"10\",\"refund_id\":\"20\",\"ledger_entry_id\":\"30\",\"refunded_quota\":\"5\",\"available_quota\":\"99\",\"balance_version\":11}";
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.refund("2", "new-refund-key", body).status);
        assertEquals("/api/v1/external-billing/charges/2/refunds", client.path);
        assertEquals(body, client.original); assertEquals("new-refund-key", client.key);
        client.code = 422; client.response = "{\"error\":\"REFUND_EXCEEDED\"}";
        assertEquals("REFUND_EXCEEDED", client.refund("2", "new-refund-key", body).errorCode);
    }

    @Test void refundLedgerAndAmountsStayStringsAboveJavascriptPrecisionAndNumericInputIsNotRewritten() {
        WriteFixture client = new WriteFixture();
        String body = RefundContracts.request("test-only-merchant", "refund-wide", "9007199254740993",
            "9223372036854775807", "OVERCHARGED").toString();
        client.response = "{\"status\":\"SUCCEEDED\",\"operation_id\":\"10\",\"refund_id\":\"20\",\"ledger_entry_id\":\"30\","
            + "\"refunded_quota\":\"9223372036854775807\",\"available_quota\":\"0\",\"balance_version\":11}";
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.refund("9007199254740993", "refund-wide-key", body).status);
        assertEquals(body, client.original);
        assertTrue(body.contains("\"original_ledger_entry_id\":\"9007199254740993\""));
        assertTrue(body.contains("\"refund_quota\":\"9223372036854775807\""));
        String numericId = body.replace("\"original_ledger_entry_id\":\"9007199254740993\"", "\"original_ledger_entry_id\":9007199254740993");
        String numericQuota = body.replace("\"refund_quota\":\"9223372036854775807\"", "\"refund_quota\":9223372036854775807");
        assertThrows(IllegalArgumentException.class, () -> client.refund("9007199254740993", "refund-wide-key", numericId));
        assertThrows(IllegalArgumentException.class, () -> client.refund("9007199254740993", "refund-wide-key", numericQuota));
        assertEquals(1, client.writes);
    }

    @Test void missingZeroOverflowOrFloatingReceiptIdsRemainUnknownWithoutResendingCapture() {
        WriteFixture client = new WriteFixture();
        for (String field : new String[]{"operation_id", "charge_id", "ledger_entry_id"}) {
            for (String value : new String[]{"0", "9223372036854775808", "1.5"}) {
                client.response = captureReceipt().replaceAll("\\\"" + field + "\\\":\\\"[0-9]+\\\"", "\"" + field + "\":\"" + value + "\"");
                assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.capture("original-capture-key", captureBody()).status);
            }
        }
        client.response = captureReceipt().replace("\"operation_id\":\"1\",", "");
        assertEquals(ExternalBillingGateway.Status.UNKNOWN, client.capture("original-capture-key", captureBody()).status);
        assertEquals(10, client.writes); assertEquals(0, client.reads);
    }

    @Test void loopbackCapturePostsExactBodyAndIdempotencyHeaderWithoutReleaseIntermediate() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        java.util.List<String> bodies = new java.util.ArrayList<>(), keys = new java.util.ArrayList<>();
        server.createContext("/", exchange -> {
            assertEquals("/api/v1/external-billing/charges", exchange.getRequestURI().getPath()); assertEquals("POST", exchange.getRequestMethod());
            keys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            try (java.io.InputStream input = exchange.getRequestBody(); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024]; int size; while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
                bodies.add(new String(output.toByteArray(), StandardCharsets.UTF_8));
            }
            byte[] response = captureReceipt().getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200,response.length);
            try (java.io.OutputStream stream = exchange.getResponseBody()) { stream.write(response); }
        });
        server.start();
        try {
            ExternalBillingProperties p = writeProperties(); p.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            NewApiBillingClient client = new NewApiBillingClient(p, json);
            assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.capture("fixed-key", captureBody()).status);
            assertEquals(ExternalBillingGateway.Status.SUCCEEDED, client.capture("fixed-key", captureBody()).status);
            assertEquals(java.util.Arrays.asList(captureBody(), captureBody()), bodies);
            assertEquals(java.util.Arrays.asList("fixed-key", "fixed-key"), keys);
        } finally { server.stop(0); }
    }
}
