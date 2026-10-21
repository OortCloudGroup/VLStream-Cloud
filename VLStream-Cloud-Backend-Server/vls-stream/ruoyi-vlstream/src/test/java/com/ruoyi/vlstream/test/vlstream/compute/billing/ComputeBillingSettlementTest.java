package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingRate;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ComputeBillingSettlementTest {
    final ObjectMapper json = new ObjectMapper();
    final long start = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    ExternalBillingProperties properties;
    FakeGateway gateway;
    ComputeBillingSettlementService service;

    @BeforeEach void database() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:billing_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds); manager = new DataSourceTransactionManager(ds);
        jdbc.execute("CREATE TABLE vls_compute_usage(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),opening_user_id VARCHAR(100),payer_account_ref VARCHAR(100),server_key VARCHAR(200),rate_snapshot TEXT,state VARCHAR(20),billing_start_at TIMESTAMP(3),billing_end_at TIMESTAMP(3))");
        jdbc.execute("CREATE TABLE vls_compute_charge(charge_key VARCHAR(80) PRIMARY KEY,usage_id VARCHAR(36))");
        String migration = System.getProperty("billing.migration");
        if (migration == null) {
            java.nio.file.Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            while (root != null && !java.nio.file.Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
            if (root == null) throw new IllegalStateException("Set billing.migration to the actual Flyway source");
            migration = root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_048__compute_billing_slices.sql").toString();
        }
        try (java.sql.Connection connection = ds.getConnection()) { ScriptUtils.executeSqlScript(connection, new FileSystemResource(migration)); }
        properties = new ExternalBillingProperties(); properties.setMerchantId("vls-test"); properties.setEnabled(true);
        gateway = new FakeGateway(); service = service(jdbc);
    }
    ComputeBillingSettlementService service(JdbcTemplate template) { return new ComputeBillingSettlementService(template, manager, json, properties, (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway); }
    ComputeBillingRate rate(String cost) {
        ComputeBillingRate rate = new ComputeBillingRate(); rate.setVersion("accepted-price"); rate.setCostSource("VERIFIED_MANUAL");
        rate.setCurrency("CNY"); rate.setCostScope("INSTANCE_COMPUTE"); rate.setCostUnit("PER_INSTANCE_HOUR"); rate.setCostHourly(new BigDecimal(cost));
        rate.setValidFrom("2026-01-01T00:00:00Z"); rate.setValidUntil("2027-01-01T00:00:00Z"); rate.setQuotaUnit("quota");
        rate.setConversionVersion("accepted-conversion"); rate.setQuotaPerCurrencyUnit(BigDecimal.ONE); rate.setQuotaScale(0); rate.setQuotaRounding(RoundingMode.DOWN);
        rate.setPricePolicy("LOCKED_FOR_RUN"); rate.setProviderPriceLockConfirmed(true); rate.setPriceEvidence("TEST_ONLY_ACCEPTED_EXISTING_PRICE"); return rate;
    }
    ComputePlatformAuthorization.Context actor(String tenant) {
        ComputePlatformAuthorization.Context actor = new ComputePlatformAuthorization.Context(); actor.setAuthorized(true); actor.setVlsUserId("actor-test");
        actor.setVlsTenantId(tenant); actor.setPlatformUserId("platform-test"); return actor;
    }
    BillingSnapshot snapshot(ComputeBillingRate rate, String n, String d) throws Exception {
        JsonNode config = json.readTree("{\"settlement_currency\":\"CNY\",\"quota_unit\":\"quota\",\"conversion_version\":\"accepted-conversion\",\"quota_per_currency_unit\":{\"numerator\":\"" + n + "\",\"denominator\":\"" + d + "\"},\"effective_at\":\"2026-01-01T00:00:00Z\",\"valid_until\":\"2030-01-01T00:00:00Z\"}");
        return BillingSnapshot.fromExistingRate(rate, config, Instant.ofEpochMilli(start), json);
    }
    String usage(String tenant, String cost, String n, String d) throws Exception {
        String id = UUID.randomUUID().toString(); ComputeBillingRate rate = rate(cost);
        jdbc.update("INSERT INTO vls_compute_usage(id,tenant_id,opening_user_id,payer_account_ref,server_key,rate_snapshot,state,billing_start_at) VALUES(?,?,?,?,?,?,'RUNNING',?)", id, tenant, "actor-test", "platform-test", "node-server:" + id, json.writeValueAsString(rate), new Timestamp(start));
        service.bind(id, BillingPayer.fromVerifiedSelf(actor(tenant), json.readTree("{\"success\":true,\"data\":{\"id\":123}}")), snapshot(rate, n, d)); return id;
    }
    String interval(String id, long millis) { service.recordRunningObservation(id, Instant.ofEpochMilli(start + millis)); return service.prepareInterval(id, start + millis); }
    Map<String, Object> row(String ref) { return jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=?", ref); }
    String state(String ref) { return String.valueOf(row(ref).get("state")); }

    @Test void migrationOnlyAddsEmptyLocalTablesAndPreservesLegacyRows() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_slice", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_session", Integer.class));
        assertFalse(new ExternalBillingProperties().isEnabled());
    }
    @Test void payerUsesAuthoritativeSelfAndRejectsMissingProofOrUnsafeTypes() throws Exception {
        BillingPayer p = BillingPayer.fromVerifiedSelf(actor("a"), json.readTree("{\"id\":9007199254740993}")); assertEquals("9007199254740993", p.userId);
        assertThrows(IllegalArgumentException.class, () -> BillingPayer.fromVerifiedSelf(null, json.readTree("{\"id\":123}")));
        for (String body : new String[]{"{\"id\":1.2}", "{\"id\":\"01\"}", "{\"success\":false,\"id\":123}", "{}"})
            assertThrows(IllegalArgumentException.class, () -> BillingPayer.fromVerifiedSelf(actor("a"), json.readTree(body)));
    }
    @Test void bindingCannotReplaceOpeningActorOrExistingSnapshot() throws Exception {
        String id = usage("a", "1", "500000", "1");
        assertThrows(IllegalArgumentException.class, () -> service.bind(id, BillingPayer.fromVerifiedSelf(actor("b"), json.readTree("{\"id\":123}")), snapshot(rate("1"), "500000", "1")));
        assertThrows(IllegalArgumentException.class, () -> service.bind(id, BillingPayer.fromVerifiedSelf(actor("a"), json.readTree("{\"id\":124}")), snapshot(rate("1"), "500000", "1")));
    }
    @Test void existingPriceMultiplierIsPreservedAndSnapshotIsImmutable() throws Exception {
        ComputeBillingRate accepted = rate("6.46"); BillingSnapshot saved = snapshot(accepted, "500000", "1"); accepted.setCostHourly(new BigDecimal("9"));
        assertEquals("12.92", saved.approvedHourlyPrice.toPlainString());
        assertEquals("6460000", ExactQuota.compute(BillingSnapshot.read(saved.json, json).approvedHourlyPrice, 3600000, saved.numerator, saved.denominator).floor());
    }
    @Test void int64AndInvalidMonetaryStringsAreChecked() {
        assertEquals(new BigInteger("9007199254740993"), ExactQuota.integer("9007199254740993", true));
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), ExactQuota.integer(Long.toString(Long.MAX_VALUE), true));
        for (String s : new String[]{"9223372036854775808", "-1", "01", "1e3", "1.2", "0"}) assertThrows(IllegalArgumentException.class, () -> ExactQuota.integer(s, true));
    }
    @Test void exactSecondsEqualAggregateWithoutDecimal128Drift() {
        ExactQuota sum = ExactQuota.ZERO;
        for (int i = 0; i < 3600; i++) sum = sum.add(ExactQuota.compute(new BigDecimal("0.02"), 1000, "7", "3"));
        assertEquals(ExactQuota.compute(new BigDecimal("0.02"), 3600000, "7", "3").toString(), sum.toString());
    }
    @Test void persistentReferenceKeyBodyCursorAndRemainderAreStableOnReplay() throws Exception {
        String id = usage("a", "1", "500000", "1"), ref = interval(id, 60000); Map<String, Object> before = row(ref);
        assertEquals(ref, service(jdbc).prepareInterval(id, start + 60000)); assertEquals(before, row(ref));
        assertEquals("vls:" + id + ":0", ref); assertEquals("16666", before.get("amount_text"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_slice", Integer.class));
        assertEquals("2", jdbc.queryForObject("SELECT numerator_text FROM vls_compute_account_remainder", String.class));
        assertEquals("3", jdbc.queryForObject("SELECT denominator_text FROM vls_compute_account_remainder", String.class));
    }
    @Test void zeroQuotaStaysLocalAndRemaindersSurviveRestartAcrossTenants() throws Exception {
        String a = usage("a", "0.3", "1", "1"), b = usage("b", "0.3", "1", "1");
        String first = interval(a, 3600000); assertEquals("NO_DEBIT", state(first)); service.dispatch(first); assertEquals(0, gateway.captures.size());
        service = service(jdbc); String second = interval(b, 3600000); assertEquals("1", row(second).get("amount_text"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_account_remainder", Integer.class));
        assertEquals("1", jdbc.queryForObject("SELECT numerator_text FROM vls_compute_account_remainder", String.class));
        assertEquals("5", jdbc.queryForObject("SELECT denominator_text FROM vls_compute_account_remainder", String.class));
    }
    @Test void accountRemainderAllocationSerializesConcurrentInstances() throws Exception {
        String a = usage("a", "0.3", "1", "1"), b = usage("b", "0.3", "1", "1");
        service.recordRunningObservation(a, Instant.ofEpochMilli(start + 3600000)); service.recordRunningObservation(b, Instant.ofEpochMilli(start + 3600000));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> one = pool.submit(() -> service.prepareInterval(a, start + 3600000));
            Future<String> two = pool.submit(() -> service(jdbc).prepareInterval(b, start + 3600000));
            BigInteger total = new BigInteger(String.valueOf(row(one.get(10, TimeUnit.SECONDS)).get("amount_text"))).add(new BigInteger(String.valueOf(row(two.get(10, TimeUnit.SECONDS)).get("amount_text"))));
            assertEquals(BigInteger.ONE, total);
        } finally { pool.shutdownNow(); }
    }
    @Test void rollbackDoesNotAdvanceCursorOrLoseRemainder() throws Exception {
        String id = usage("a", "1", "500000", "1"); service.recordRunningObservation(id, Instant.ofEpochMilli(start + 60000));
        JdbcTemplate failing = new JdbcTemplate(jdbc.getDataSource()) {
            @Override public int update(String sql, Object... args) {
                if (sql.startsWith("INSERT INTO vls_compute_billing_slice")) throw new IllegalStateException("TEST_ONLY_INJECTED_FAILURE");
                return super.update(sql, args);
            }
        };
        assertThrows(IllegalStateException.class, () -> service(failing).prepareInterval(id, start + 60000));
        assertEquals(start, jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_account_remainder", Integer.class));
        assertEquals("16666", row(service.prepareInterval(id, start + 60000)).get("amount_text"));
    }
    @Test void wallClockAloneCannotAuthorizeBillingAndIntervalsCannotOverlap() throws Exception {
        String id = usage("a", "1", "1", "1"); assertThrows(IllegalArgumentException.class, () -> service.prepareInterval(id, start + 60000));
        interval(id, 60000); assertThrows(IllegalArgumentException.class, () -> service.prepareInterval(id, start + 30000));
        assertThrows(IllegalArgumentException.class, () -> service.recordRunningObservation(id, Instant.now().plusSeconds(100)));
    }
    @Test void finalShortTailStopsAtConfirmedEndAndDoesNotRebillEarlierSlice() throws Exception {
        String id = usage("a", "1", "500000", "1"); interval(id, 60000);
        jdbc.update("UPDATE vls_compute_usage SET state='STOPPED',billing_end_at=? WHERE id=?", new Timestamp(start + 90000), id);
        String tail = service.prepareInterval(id, start + 90000); assertEquals(1, ((Number) row(tail).get("is_tail")).intValue());
        assertEquals("8334", row(tail).get("amount_text")); assertEquals(2, service.audit(id).size());
        assertThrows(IllegalArgumentException.class, () -> service.prepareInterval(id, start + 100000));
    }
    @Test void disabledBillingMakesZeroExternalCallsAndCannotAdmitPaidWork() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); properties.setEnabled(false);
        service.dispatch(ref); assertEquals(0, gateway.queries); assertEquals(0, gateway.captures.size());
        assertThrows(IllegalStateException.class, () -> service.requirePaidAdmission("unused"));
    }
    @Test void evenSuccessfulFakeCannotEnablePaidAdmission() throws Exception {
        String id = usage("a", "1", "500000", "1"); assertThrows(IllegalStateException.class, () -> service.requirePaidAdmission(id));
    }
    @Test void missingAtomicCaptureBlocksSendAndPersistsStopRequirement() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); gateway.captureSupported = false;
        service.dispatch(ref); assertEquals("BLOCKED", state(ref)); assertEquals(0, gateway.captures.size());
        assertEquals("ATOMIC_CAPTURE_UNAVAILABLE", row(ref).get("error_code")); assertStopRequired();
    }
    @Test void successfulPostStillRequiresVerifiedOriginalChargeDetails() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); service.dispatch(ref); assertEquals("UNKNOWN", state(ref));
        service(jdbc).dispatch(ref); service.dispatch(ref); assertEquals("SETTLED", state(ref)); assertEquals(1, gateway.captures.size());
        assertEquals("charge-test", row(ref).get("charge_id"));
    }
    @Test void serverCommitThenLostResponseRecoversWithoutSecondCapture() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); gateway.loseResponse = true;
        service.dispatch(ref); assertEquals("UNKNOWN", state(ref)); service(jdbc).dispatch(ref);
        assertEquals("SETTLED", state(ref)); assertEquals(1, gateway.captures.size());
    }
    @Test void expiredSubmissionLeaseQueriesBeforeRetryingOriginalBody() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); Map<String, Object> original = row(ref);
        jdbc.update("UPDATE vls_compute_billing_slice SET state='SUBMITTED',lease_token='old-worker',lease_until_ms=1 WHERE merchant_charge_id=?", ref);
        service(jdbc).dispatch(ref); assertTrue(gateway.queries >= 2);
        assertEquals(original.get("idempotency_key"), gateway.captures.get(0).get(0)); assertEquals(original.get("request_json"), gateway.captures.get(0).get(1));
    }
    @Test void processingOrOneUnknownLookupNeverResubmits() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.PROCESSING);
        service.dispatch(ref); assertEquals("PROCESSING", state(ref)); assertEquals(0, gateway.captures.size());
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_FOUND); gateway.referenceUnknown = true;
        service.dispatch(ref); assertEquals("UNKNOWN", state(ref)); assertEquals(0, gateway.captures.size());
    }
    @Test void rechargeUsesOriginalKeyReferenceAndBodyAfterRejection() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); Map<String, Object> original = row(ref);
        gateway.rejectOnce = true; service.dispatch(ref); assertEquals("WAITING_FUNDS", state(ref)); assertStopRequired();
        gateway.available = "1"; service.dispatch(ref); assertEquals(1, gateway.captures.size());
        gateway.available = "1000000"; service(jdbc).dispatch(ref); service.dispatch(ref);
        assertEquals("SETTLED", state(ref)); assertEquals(2, gateway.captures.size());
        assertEquals(gateway.captures.get(0), gateway.captures.get(1));
        assertEquals(original.get("idempotency_key"), row(ref).get("idempotency_key")); assertEquals(original.get("request_json"), row(ref).get("request_json"));
    }
    @Test void authorizationFailurePersistsRiskWithoutEndingRuntimeMetering() throws Exception {
        String id = usage("a", "1", "500000", "1"), ref = interval(id, 60000);
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_AUTHORIZED); service.dispatch(ref);
        assertEquals("NOT_AUTHORIZED", jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class)); assertStopRequired();
        assertThrows(IllegalStateException.class, () -> service.requirePaidAdmission(id));
        String second = interval(id, 120000); assertEquals("BLOCKED", state(second));
        assertEquals(start + 120000, jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session", Long.class));
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_FOUND); service.dispatch(ref); assertEquals(0, gateway.captures.size());
    }
    @Test void explicitRejectionOtherThanBalanceIsBlocked() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "USER_NOT_FOUND", null); service.dispatch(ref);
        assertEquals("BLOCKED", state(ref)); assertEquals(0, gateway.captures.size());
    }
    @Test void successfulReferenceWithWrongAmountNeverSettlesOrRecharges() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); service.dispatch(ref);
        ((ObjectNode) gateway.charges.get(ref)).put("charged_quota", "1"); service.dispatch(ref);
        assertEquals("BLOCKED", state(ref)); assertEquals(1, gateway.captures.size()); assertStopRequired();
    }
    @Test void tamperedPersistedBodyCannotBeSubmitted() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000);
        jdbc.update("UPDATE vls_compute_billing_slice SET request_json='{}' WHERE merchant_charge_id=?", ref); service.dispatch(ref);
        assertEquals("SNAPSHOT_CONFLICT", row(ref).get("error_code")); assertEquals(0, gateway.captures.size());
    }
    @Test void concurrentDispatchersOnlyOneOwnsSubmissionLease() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000); gateway.entered = new CountDownLatch(1); gateway.release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> one = pool.submit(() -> service.dispatch(ref)); assertTrue(gateway.entered.await(5, TimeUnit.SECONDS));
            Future<?> two = pool.submit(() -> service(jdbc).dispatch(ref)); two.get(5, TimeUnit.SECONDS);
            gateway.release.countDown(); one.get(5, TimeUnit.SECONDS); assertEquals(1, gateway.captures.size());
        } finally { gateway.release.countDown(); pool.shutdownNow(); }
    }
    @Test void ioInsideLocalTransactionIsRejectedBeforeAnyNetworkCall() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000);
        assertThrows(IllegalStateException.class, () -> new org.springframework.transaction.support.TransactionTemplate(manager).execute(tx -> {service.dispatch(ref); return null;}));
        assertEquals(0, gateway.queries);
    }
    @Test void legacyWholePeriodIntentCannotCoexistWithSliceBinding() throws Exception {
        String id = UUID.randomUUID().toString(); ComputeBillingRate r = rate("1");
        jdbc.update("INSERT INTO vls_compute_usage(id,tenant_id,opening_user_id,payer_account_ref,server_key,rate_snapshot,state,billing_start_at) VALUES(?,?,?,?,?,?,'RUNNING',?)", id, "a", "actor-test", "platform-test", "node-test", json.writeValueAsString(r), new Timestamp(start));
        jdbc.update("INSERT INTO vls_compute_charge(charge_key,usage_id) VALUES('legacy-test',?)", id);
        assertThrows(IllegalArgumentException.class, () -> service.bind(id, BillingPayer.fromVerifiedSelf(actor("a"), json.readTree("{\"id\":123}")), snapshot(r, "1", "1")));
    }
    @Test void sliceBindingAlsoBlocksLegacyWholePeriodSettlement() throws Exception {
        String id = usage("a", "1", "1", "1");
        assertThrows(RuntimeException.class, () -> new com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLedgerService(jdbc,json).prepareSettlement(id));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_charge", Integer.class));
    }
    @Test void operationConflictIsBlockedWithoutRetrying() throws Exception {
        String ref = interval(usage("a", "1", "500000", "1"), 60000);
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.CONFLICT); service.dispatch(ref);
        assertEquals("BLOCKED", state(ref)); assertEquals(0, gateway.captures.size());
    }
    void assertStopRequired() { assertEquals(1, jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session", Integer.class)); }

    class FakeGateway implements ExternalBillingGateway {
        boolean captureSupported = true, loseResponse, rejectOnce, referenceUnknown;
        int queries;
        String available = "1000000";
        Result operation = Result.of(Status.NOT_FOUND);
        final Map<String, JsonNode> charges = new java.util.concurrent.ConcurrentHashMap<>();
        final List<List<String>> captures = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch entered, release;
        @Override public Result byKey(String key) { queries++; return operation; }
        @Override public Result byReference(String ref) { queries++; return charges.containsKey(ref) ? new Result(Status.SUCCEEDED, "", charges.get(ref)) : Result.of(referenceUnknown ? Status.UNKNOWN : Status.NOT_FOUND); }
        @Override public Result balance(String user) { ObjectNode body = json.createObjectNode(); body.put("available_quota", available); return new Result(Status.SUCCEEDED, "", body); }
        @Override public boolean supportsAtomicCapture() { return captureSupported; }
        @Override public Result capture(String key, String body) {
            captures.add(java.util.Arrays.asList(key, body));
            if (entered != null) { entered.countDown(); try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("TEST_ONLY_LATCH_TIMEOUT"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("TEST_ONLY_INTERRUPTED"); } }
            if (rejectOnce) { rejectOnce = false; operation = new Result(Status.REJECTED, "INSUFFICIENT_AVAILABLE_QUOTA", null); return operation; }
            try {
                ObjectNode charge = (ObjectNode) json.readTree(body); charge.put("status", "SUCCEEDED"); charge.put("charge_id", "charge-test"); charge.put("ledger_entry_id", "ledger-test");
                charge.put("charged_quota", charge.path("charge_quota").asText()); charges.put(charge.path("merchant_charge_id").asText(), charge);
                if (loseResponse) throw new IllegalStateException("TEST_ONLY_RESPONSE_LOST_AFTER_COMMIT");
                return new Result(Status.SUCCEEDED, "", charge);
            } catch (java.io.IOException e) { throw new IllegalStateException("TEST_ONLY_BAD_BODY"); }
        }
    }
}
