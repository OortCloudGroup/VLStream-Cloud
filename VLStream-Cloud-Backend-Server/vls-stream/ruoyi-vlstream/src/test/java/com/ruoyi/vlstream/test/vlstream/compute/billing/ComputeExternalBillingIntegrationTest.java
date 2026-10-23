package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Actual VLS journal/client/settlement across loopback HTTP and H2, never an upstream wallet proof. */
@Tag("dev")
class ComputeExternalBillingIntegrationTest {
    @Test void bareDocumentedHoldResponseAndNumericExpiryReachSameHoldCaptureRecovery() throws Exception {
        ComputeBillingSettlementTest fixture = new ComputeBillingSettlementTest();
        fixture.database();
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        assertNotNull(root);
        Path migrations = root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration");
        try (java.sql.Connection connection = fixture.jdbc.getDataSource().getConnection()) {
            for (String name : new String[]{"V1_2_0_049__compute_billing_coordination.sql",
                "V1_2_0_052__compute_funding_intents.sql", "V1_2_0_053__compute_funding_hold_recovery.sql"})
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(migrations.resolve(name)));
        }
        String usage = fixture.usage("tenant-test", "1", "500000", "1");
        long expiry = System.currentTimeMillis() + 600000;
        Loopback loopback = new Loopback(fixture, expiry);
        try {
            ExternalBillingProperties properties = fixture.properties;
            properties.setAllowWrites(true);
            properties.setBaseUrl("http://127.0.0.1:" + loopback.server.getAddress().getPort());
            properties.setApiKey("TEST_ONLY_LOOPBACK_NOT_A_REAL_CREDENTIAL");
            NewApiBillingClient client = new NewApiBillingClient(properties, fixture.json);
            ComputeFundingIntentService journal = new ComputeFundingIntentService(fixture.jdbc, fixture.manager, fixture.json);
            ComputeFundingRecoveryService recovery = new ComputeFundingRecoveryService(journal,
                (Supplier<ExternalBillingGateway>) () -> client, properties);
            ComputeFundingIntentService.Intent hold = journal.prepareInitialHold(usage, "100", Long.toString(expiry), "GPU_COMPUTE");
            assertEquals("RECEIPT_UNVERIFIED", recovery.advance(hold.id, System.currentTimeMillis()).state);
            assertEquals("DETAIL_OBSERVED", recovery.advance(hold.id, System.currentTimeMillis()).state);
            assertEquals("9007199254740993", journal.recover(hold.id).holdId);

            String reference = fixture.interval(usage, 1000);
            Map<String, Object> original = fixture.row(reference);
            ComputeBillingSettlementService settlements = new ComputeBillingSettlementService(fixture.jdbc,
                fixture.manager, fixture.json, properties, (Supplier<ExternalBillingGateway>) () -> client);
            settlements.dispatch(reference);
            assertEquals("WAITING_FUNDS", fixture.state(reference));
            ComputeFundingIntentService.Intent topup = journal.prepareTopup(hold.id,
                "topup-loopback-1", "300", "GPU_COMPUTE", System.currentTimeMillis());
            assertFalse(topup.body.contains("expires_at"));
            assertEquals("RECEIPT_UNVERIFIED", recovery.advance(topup.id, System.currentTimeMillis()).state);
            settlements.dispatch(reference);
            settlements.dispatch(reference);
            assertEquals("SETTLED", fixture.state(reference));
            assertEquals(2, loopback.captureBodies.size());
            assertEquals(loopback.captureBodies.get(0), loopback.captureBodies.get(1));
            assertEquals(loopback.captureKeys.get(0), loopback.captureKeys.get(1));
            assertEquals(original.get("request_json"), fixture.row(reference).get("request_json"));
            assertEquals(original.get("request_hash"), fixture.row(reference).get("request_hash"));
            assertEquals(expiry, fixture.jdbc.queryForObject(
                "SELECT funding_expires_at_ms FROM vls_compute_billing_session WHERE usage_id=?", Long.class, usage).longValue());
            assertEquals(0, loopback.releases);
            assertThrows(IllegalStateException.class, () -> settlements.requirePaidAdmission(usage));
        } finally {
            loopback.server.stop(0);
        }
    }

    private static final class Loopback {
        final ComputeBillingSettlementTest fixture;
        final HttpServer server;
        final long expiry;
        final List<String> captureBodies = new ArrayList<>(), captureKeys = new ArrayList<>();
        String holdReference;
        int total = 100, captured, version = 1, releases;
        boolean rejected;
        JsonNode charge;

        Loopback(ComputeBillingSettlementTest fixture, long expiry) throws Exception {
            this.fixture = fixture;
            this.expiry = expiry;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        private void handle(HttpExchange exchange) throws java.io.IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                ObjectNode response = fixture.json.createObjectNode();
                int status = 200;
                if ("POST".equals(exchange.getRequestMethod())) {
                    String bytes;
                    try (InputStream input = exchange.getRequestBody(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                        bytes = new String(output.toByteArray(), StandardCharsets.UTF_8);
                    }
                    JsonNode body = fixture.json.readTree(bytes);
                    if (path.endsWith("/holds")) {
                        holdReference = body.path("merchant_hold_id").asText();
                        response.put("hold_id", new java.math.BigInteger("9007199254740993"));
                        response.put("available_quota", "9900"); response.put("held_quota", "100");
                    } else if (path.endsWith("/topup")) {
                        total += body.path("topup_quota").asInt(); version++;
                        response = counters();
                    } else if (path.endsWith("/charges")) {
                        captureBodies.add(bytes);
                        captureKeys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
                        int amount = body.path("charge_quota").asInt();
                        if (amount > total - captured) {
                            rejected = true; status = 422; response.put("error", "INSUFFICIENT_HELD_QUOTA");
                        } else {
                            captured += amount; version++;
                            ObjectNode detail = body.deepCopy();
                            detail.put("charged_quota", body.path("charge_quota").asText());
                            detail.put("status", "SUCCEEDED"); detail.put("charge_id", "11"); detail.put("ledger_entry_id", "22");
                            charge = detail;
                            response.put("status", "SUCCEEDED"); response.put("operation_id", "33");
                            response.put("charge_id", "11"); response.put("ledger_entry_id", "22");
                            response.put("charged_quota", body.path("charge_quota").asText());
                            response.put("available_quota", "9600"); response.put("balance_version", version);
                        }
                    } else {
                        releases++; status = 500;
                    }
                } else if (path.contains("/charges/by-reference/")) {
                    if (charge == null) { status = 404; response.put("error", "CHARGE_NOT_FOUND"); }
                    else response = (ObjectNode) charge;
                } else if (path.contains("/operations/by-key/")) {
                    if (rejected) { response.put("status", "REJECTED"); response.put("error_code", "INSUFFICIENT_HELD_QUOTA"); }
                    else { status = 404; response.put("error", "OPERATION_NOT_FOUND"); }
                } else if (path.contains("/hold-operations/")) {
                    // Deliberately unspecified: this must never be accepted as operation success.
                    response.put("uncontracted", true);
                } else if (path.contains("/holds/")) {
                    response = counters();
                    response.put("merchant_hold_id", holdReference); response.put("user_id", 123);
                    response.put("expires_at", expiry);
                    response.put("created_at", 1760000000); response.put("updated_at", 1760000100);
                } else status = 500;
                byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (java.io.OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
            } finally {
                exchange.close();
            }
        }

        private ObjectNode counters() {
            ObjectNode response = fixture.json.createObjectNode();
            response.put("hold_id", new java.math.BigInteger("9007199254740993"));
            response.put("hold_quota", Integer.toString(total)); response.put("captured_quota", Integer.toString(captured));
            response.put("released_quota", "0"); response.put("remaining_quota", Integer.toString(total - captured));
            response.put("hold_status", captured == 0 ? "ACTIVE" : "PARTIALLY_CAPTURED");
            response.put("balance_version", version);
            return response;
        }
    }
}
