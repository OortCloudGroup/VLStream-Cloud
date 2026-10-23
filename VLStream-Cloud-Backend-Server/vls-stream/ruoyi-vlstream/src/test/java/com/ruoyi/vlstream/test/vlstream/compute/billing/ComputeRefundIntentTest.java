package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Isolated H2 and in-process gateway only; no real authorization, funds or provider is accessed. */
@Tag("dev")
class ComputeRefundIntentTest {
    final ObjectMapper json = new ObjectMapper();
    final String usage = UUID.randomUUID().toString();
    final String chargeReference = "vls:" + usage + ":0";
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    ExternalBillingProperties properties;
    FakeGateway gateway;
    ComputeRefundIntentService service;
    String originalBody;

    @BeforeEach void database() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:refund_" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds); manager = new DataSourceTransactionManager(ds);
        jdbc.execute("CREATE TABLE vls_compute_usage(id VARCHAR(36) PRIMARY KEY)");
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Actual migration root required");
        for (String migration : new String[]{"V1_2_0_048__compute_billing_slices.sql", "V1_2_0_054__compute_corrective_refund_intents.sql"}) {
            Path path = root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/" + migration);
            try (java.sql.Connection c = ds.getConnection()) { ScriptUtils.executeSqlScript(c, new FileSystemResource(path.toFile())); }
        }
        jdbc.update("INSERT INTO vls_compute_billing_session(usage_id,tenant_id,opening_user_id,merchant_id,payer_user_id,instance_ref,quota_unit,accepted_snapshot,metered_until_ms,last_running_ms) VALUES(?,'tenant-test','actor-test','vls-test','123','node-test','quota','{}',1,1)", usage);
        ObjectNode original = json.createObjectNode();
        original.put("merchant_id", "vls-test"); original.put("merchant_charge_id", chargeReference);
        original.put("user_id", 123); original.put("usage_session_id", usage); original.put("hold_id", 101);
        original.put("charge_quota", "1000"); original.put("business_type", "GPU_COMPUTE");
        originalBody = original.toString();
        jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state,charge_id,ledger_entry_id) VALUES(?,?,0,0,1,'original-charge-key',?,?,?,'0','1','SETTLED','201','301')",
            chargeReference, usage, originalBody, ComputeBillingSettlementService.hash(originalBody), "1000");
        properties = new ExternalBillingProperties(); properties.setEnabled(true); properties.setMerchantId("vls-test");
        gateway = new FakeGateway(); service = restarted();
    }

    ComputeRefundIntentService restarted() {
        return new ComputeRefundIntentService(jdbc, manager, json, properties, (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway);
    }
    ComputeRefundIntentService.Intent prepare(String correction, String amount) {
        return service.prepare(chargeReference, correction, amount, "OVERCHARGED");
    }
    JsonNode receipt(String amount, String refundId) throws Exception {
        return json.readTree("{\"operation_id\":\"501\",\"status\":\"SUCCEEDED\",\"refund_id\":\"" + refundId
            + "\",\"ledger_entry_id\":\"401\",\"refunded_quota\":\"" + amount + "\",\"available_quota\":\"0\",\"balance_version\":9}");
    }
    int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_refund_intent", Integer.class); }

    @Test void migrationAddsOnlyEmptyLocalJournalAndWritesRemainOff() {
        assertEquals(0, count());
        assertFalse(new ExternalBillingProperties().isEnabled());
        assertFalse(new ExternalBillingProperties().isAllowWrites());
        assertFalse(new ExternalBillingProperties().isSafetyStopEnabled());
    }

    @Test void refundContractUsesOnlyDefinedFieldsWithExactStrings() {
        ObjectNode body = RefundContracts.request("vls-test", "refund-test", "9007199254740993", "9223372036854775807", "OVERCHARGED");
        assertEquals(5, body.size()); assertFalse(body.has("user_id"));
        assertEquals("9007199254740993", body.path("original_ledger_entry_id").textValue());
        assertEquals("9223372036854775807", body.path("refund_quota").textValue());
        for (String amount : new String[]{"0", "01", "-1", "1.0", "1e3", "9223372036854775808"})
            assertThrows(IllegalArgumentException.class, () -> RefundContracts.request("vls", "refund", "1", amount, "OVERCHARGED"));
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.request("vls", "refund", "1", "1", "overcharged"));
    }

    @Test void receiptRequiresDocumentedExactFieldsAndSucceededStatus() throws Exception {
        ObjectNode valid = (ObjectNode) receipt("400", "9007199254740993");
        assertEquals("9007199254740993", RefundContracts.receipt(valid).path("refund_id").textValue());
        ObjectNode wrapped = json.createObjectNode(); wrapped.put("success", true); wrapped.set("data", valid);
        assertEquals(RefundContracts.receipt(valid), RefundContracts.receipt(wrapped));
        for (String field : new String[]{"operation_id", "refund_id", "ledger_entry_id", "refunded_quota", "available_quota", "balance_version"}) {
            ObjectNode missing = valid.deepCopy(); missing.remove(field);
            assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(missing));
        }
        ObjectNode numericAmount = valid.deepCopy(); numericAmount.put("refunded_quota", 400);
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(numericAmount));
        ObjectNode fractionId = valid.deepCopy(); fractionId.put("refund_id", 1.0);
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(fractionId));
        ObjectNode failed = valid.deepCopy(); failed.put("status", "PROCESSING");
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(failed));
        wrapped.put("success", "true");
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(wrapped));
        ObjectNode contradictory = valid.deepCopy(); contradictory.put("error", "REFUND_EXCEEDED");
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(contradictory));
        ObjectNode failedWrapper = json.createObjectNode(); failedWrapper.put("success", true); failedWrapper.put("error_code", "INVALID_REQUEST"); failedWrapper.set("data", valid);
        assertThrows(IllegalArgumentException.class, () -> RefundContracts.receipt(failedWrapper));
    }

    @Test void immutableOriginalAndRefundSnapshotSurviveRestart() throws Exception {
        ComputeRefundIntentService.Intent original = prepare("correction-1", "400");
        ComputeRefundIntentService.Intent replay = restarted().prepare(chargeReference, "correction-1", "400", "OVERCHARGED");
        assertEquals(original.id, replay.id); assertEquals(original.key, replay.key); assertEquals(original.body, replay.body);
        assertEquals(original.requestHash, replay.requestHash); assertEquals("201", original.chargeId); assertEquals("301", original.originalLedgerId);
        assertEquals("/api/v1/external-billing/charges/201/refunds", original.path);
        assertEquals("123", original.payerUserId); assertNotEquals("original-charge-key", original.key);
        assertEquals(originalBody, jdbc.queryForObject("SELECT request_json FROM vls_compute_billing_slice WHERE merchant_charge_id=?", String.class, chargeReference));
        assertEquals("original-charge-key", jdbc.queryForObject("SELECT idempotency_key FROM vls_compute_billing_slice WHERE merchant_charge_id=?", String.class, chargeReference));
        assertEquals(originalBody, json.readTree(originalBody).toString());
        assertFalse(replay.safeToRetry);
    }

    @Test void sameCorrectionCannotChangeAmountOrReason() {
        ComputeRefundIntentService.Intent original = prepare("correction-1", "400");
        assertThrows(IllegalArgumentException.class, () -> prepare("correction-1", "401"));
        assertThrows(IllegalArgumentException.class, () -> service.prepare(chargeReference, "correction-1", "400", "METERING_CORRECTION"));
        assertEquals(original.body, restarted().recover(original.id).body); assertEquals(1, count());
    }

    @Test void onlySettledOriginalWithMatchingPayerMerchantAndHashCanPrepare() {
        jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN' WHERE merchant_charge_id=?", chargeReference);
        assertThrows(IllegalStateException.class, () -> prepare("correction-1", "400"));
        jdbc.update("UPDATE vls_compute_billing_slice SET state='SETTLED',request_hash='wrong' WHERE merchant_charge_id=?", chargeReference);
        assertThrows(IllegalStateException.class, () -> prepare("correction-1", "400"));
        jdbc.update("UPDATE vls_compute_billing_slice SET request_hash=? WHERE merchant_charge_id=?", ComputeBillingSettlementService.hash(originalBody), chargeReference);
        jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='124' WHERE usage_id=?", usage);
        assertThrows(IllegalStateException.class, () -> prepare("correction-1", "400"));
        jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='123',merchant_id='other' WHERE usage_id=?", usage);
        assertThrows(IllegalStateException.class, () -> prepare("correction-1", "400")); assertEquals(0, count());
    }

    @Test void partialRefundsReserveCumulativeAmountIncludingUnknownOutcomes() {
        ComputeRefundIntentService.Intent first = prepare("correction-1", "600");
        service.recordUnknown(first.id, first.requestHash);
        ComputeRefundIntentService.Intent second = prepare("correction-2", "400");
        assertNotEquals(first.key, second.key); assertNotEquals(first.reference, second.reference);
        assertThrows(IllegalArgumentException.class, () -> prepare("correction-3", "1")); assertEquals(2, count());
    }

    @Test void concurrentPreparationUsesSingleKeyAndEnforcesCumulativeLimit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6); CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ComputeRefundIntentService.Intent>> tasks = new ArrayList<>();
            for (int i = 0; i < 6; i++) tasks.add(pool.submit(() -> { start.await(); return restarted().prepare(chargeReference, "same-correction", "600", "OVERCHARGED"); }));
            start.countDown(); String key = null;
            for (Future<ComputeRefundIntentService.Intent> result : tasks) {
                ComputeRefundIntentService.Intent intent = result.get(10, TimeUnit.SECONDS);
                if (key == null) key = intent.key; else assertEquals(key, intent.key);
            }
            assertEquals(1, count());
            List<Future<Boolean>> competing = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final String ref = "competing-" + i;
                competing.add(pool.submit(() -> { try { restarted().prepare(chargeReference, ref, "400", "OVERCHARGED"); return true; }
                    catch (IllegalArgumentException expected) { return false; } }));
            }
            int accepted = 0; for (Future<Boolean> result : competing) if (result.get(10, TimeUnit.SECONDS)) accepted++;
            assertEquals(1, accepted); assertEquals(2, count());
        } finally { pool.shutdownNow(); }
    }

    @Test void rollbackCannotPublishHalfAnIntent() {
        new TransactionTemplate(manager).execute(tx -> { prepare("correction-1", "400"); tx.setRollbackOnly(); return null; });
        assertEquals(0, count()); assertNotNull(prepare("correction-1", "400").key);
    }

    @Test void defaultWriteGateDoesNotConsultGatewayOrSend() {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400"); AtomicInteger accessed = new AtomicInteger();
        ComputeRefundIntentService guarded = new ComputeRefundIntentService(jdbc, manager, json, properties,
            (java.util.function.Supplier<ExternalBillingGateway>) () -> { accessed.incrementAndGet(); return gateway; });
        guarded.dispatch(intent.id); assertEquals(0, accessed.get()); assertEquals(0, gateway.writes);
        properties.setAllowWrites(true); properties.setEnabled(false); guarded.dispatch(intent.id);
        assertEquals(0, accessed.get()); assertEquals("PREPARED", service.recover(intent.id).state);
    }

    @Test void uncertainSubmissionNeverReplaysOrInventsRefundQueryContract() {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400"); properties.setAllowWrites(true);
        service.dispatch(intent.id); restarted().dispatch(intent.id);
        ComputeRefundIntentService.Intent unknown = service.recover(intent.id);
        assertEquals("UNKNOWN", unknown.state); assertEquals(1, gateway.writes); assertEquals(0, gateway.queries);
        assertEquals(intent.key, gateway.lastKey); assertEquals(intent.body, gateway.lastBody); assertEquals("201", gateway.lastChargeId);
        assertEquals(intent.body, unknown.body); assertFalse(unknown.safeToRetry);
    }

    @Test void receiptRequiresHoldRequeryWithoutClaimingRefundIsAvailable() throws Exception {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400");
        gateway.next = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", receipt("400", "501"));
        properties.setAllowWrites(true); service.dispatch(intent.id);
        ComputeRefundIntentService.Intent recorded = service.recover(intent.id);
        assertEquals("RECEIPT_UNVERIFIED", recorded.state); assertEquals("NEEDS_HOLD_REQUERY", recorded.errorCode);
        assertTrue(recorded.needsHoldRequery); assertEquals("0", json.readTree(recorded.receipt).path("available_quota").asText());
        ComputeRefundIntentService.Intent evidence = service.requireHoldRequery("vls-test", recorded.reference);
        assertEquals("101", evidence.holdId); assertEquals(recorded.receiptObservedAtMs, evidence.receiptObservedAtMs);
        assertTrue(evidence.receiptObservedAtMs > 0);
        assertEquals(recorded.state, service.recordUnknown(intent.id, intent.requestHash).state);
        assertEquals(recorded.receipt, service.recordReceipt(intent.id, intent.requestHash, receipt("400", "501")).receipt);
        restarted().dispatch(intent.id); assertEquals(1, gateway.writes);
    }

    @Test void conflictingReceiptIsStickyAndKeepsBothObservations() throws Exception {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400");
        ComputeRefundIntentService.Intent recorded = service.recordReceipt(intent.id, intent.requestHash, receipt("400", "501"));
        ComputeRefundIntentService.Intent conflict = service.recordReceipt(intent.id, intent.requestHash, receipt("400", "502"));
        assertEquals("CONFLICT", conflict.state); assertEquals(recorded.receipt, conflict.receipt); assertTrue(conflict.conflictingReceipt.contains("502"));
        assertEquals("CONFLICT", service.recordUnknown(intent.id, intent.requestHash).state);
        assertEquals("CONFLICT", service.recordReceipt(intent.id, intent.requestHash, receipt("400", "501")).state);
    }

    @Test void wrongReceiptAmountOrRequestHashCannotBecomeSuccessful() throws Exception {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400");
        assertThrows(IllegalArgumentException.class, () -> service.recordReceipt(intent.id, "wrong-hash", receipt("400", "501")));
        assertEquals("PREPARED", service.recover(intent.id).state);
        assertEquals("CONFLICT", service.recordReceipt(intent.id, intent.requestHash, receipt("401", "501")).state);
    }

    @Test void corruptedRefundOrOriginalAssociationFailsClosed() {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400");
        jdbc.update("UPDATE vls_compute_refund_intent SET original_ledger_entry_id='302' WHERE refund_intent_id=?", intent.id);
        assertThrows(IllegalStateException.class, () -> service.recover(intent.id));
        jdbc.update("UPDATE vls_compute_refund_intent SET original_ledger_entry_id='301',request_json='{}' WHERE refund_intent_id=?", intent.id);
        assertThrows(IllegalStateException.class, () -> service.recover(intent.id));
        jdbc.update("UPDATE vls_compute_refund_intent SET request_json=? WHERE refund_intent_id=?", intent.body, intent.id);
        jdbc.update("UPDATE vls_compute_billing_slice SET ledger_entry_id='303' WHERE merchant_charge_id=?", chargeReference);
        assertThrows(IllegalStateException.class, () -> service.recover(intent.id));
    }

    @Test void explicitRejectionDoesNotReleaseReservationOrRetryNewKey() {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "1000"); properties.setAllowWrites(true);
        gateway.next = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "REFUND_EXCEEDED", null);
        service.dispatch(intent.id); service.dispatch(intent.id);
        assertEquals("REJECTED", service.recover(intent.id).state); assertEquals(1, gateway.writes);
        assertThrows(IllegalArgumentException.class, () -> prepare("correction-2", "1"));
    }

    @Test void holdRequeryEvidenceRejectsUnknownOrdinaryOrContradictoryRefunds() throws Exception {
        ComputeRefundIntentService.Intent intent = prepare("correction-1", "400");
        assertThrows(IllegalStateException.class, () -> service.requireHoldRequery("vls-test", intent.reference));
        service.recordReceipt(intent.id, intent.requestHash, receipt("400", "501"));
        service.recordReceipt(intent.id, intent.requestHash, receipt("400", "502"));
        assertThrows(IllegalStateException.class, () -> service.requireHoldRequery("vls-test", intent.reference));
        ObjectNode ordinary = (ObjectNode) json.readTree(originalBody); ordinary.remove("hold_id");
        jdbc.update("UPDATE vls_compute_billing_slice SET request_json=?,request_hash=? WHERE merchant_charge_id=?",
            ordinary.toString(), ComputeBillingSettlementService.hash(ordinary.toString()), chargeReference);
        // An already prepared capture refund cannot be rebound to an ordinary debit.
        assertThrows(IllegalStateException.class, () -> service.recover(intent.id));
    }

    @Test void refundReleaseRequiresQueryStartedAfterReceiptAndNeverRewritesEarlierFinalRelease() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Actual migration root required");
        for (String migration : new String[]{"V1_2_0_052__compute_funding_intents.sql", "V1_2_0_053__compute_funding_hold_recovery.sql"}) {
            Path source = root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/" + migration);
            try (java.sql.Connection c = jdbc.getDataSource().getConnection()) { ScriptUtils.executeSqlScript(c, new FileSystemResource(source.toFile())); }
        }
        ComputeFundingIntentService funding = new ComputeFundingIntentService(jdbc, manager, json,
            (java.util.function.Supplier<ComputeRefundIntentService>) () -> service);
        ComputeFundingIntentService.Intent hold = funding.prepareInitialHold(usage, "2000", "1799999999999", "GPU_COMPUTE");
        hold = funding.recordHoldDetail(hold.id, hold.requestHash, holdDetail(hold, 1, "1000", "0", "1000"));
        ComputeFundingIntentService.Intent finalRelease = funding.prepareFinalRelease(hold.id, "500");
        ObjectNode released = json.createObjectNode();
        released.put("hold_id", "101"); released.put("held_quota", "500"); released.put("released_quota", "500");
        released.put("remaining_quota", "500"); released.put("hold_status", "PARTIALLY_CAPTURED");
        funding.recordReceipt(finalRelease.id, finalRelease.requestHash, released);
        funding.recordHoldDetail(hold.id, hold.requestHash, holdDetail(hold, 2, "1000", "500", "500"));

        ComputeRefundIntentService.Intent refund = prepare("post-release-correction", "400");
        long oldQueryStartedAt = System.currentTimeMillis() - 1;
        refund = service.recordReceipt(refund.id, refund.requestHash, receipt("400", "501"));
        final ComputeFundingIntentService.Intent originalHold = hold;
        final String refundReference = refund.reference;
        assertThrows(IllegalStateException.class, () -> funding.prepareRefundRelease(originalHold.id, refundReference));

        // This response arrived after the refund receipt, but the request started before it.
        // Changed counters alone cannot establish that the query observed the refund's final state.
        JsonNode afterRefund = holdDetail(hold, 3, "600", "500", "900");
        funding.recordHoldDetail(hold.id, hold.requestHash, afterRefund, oldQueryStartedAt);
        assertTrue(oldQueryStartedAt <= refund.receiptObservedAtMs);
        assertThrows(IllegalStateException.class, () -> funding.prepareRefundRelease(originalHold.id, refundReference));

        Thread.sleep(2);
        long freshQueryStartedAt = System.currentTimeMillis();
        assertTrue(freshQueryStartedAt > refund.receiptObservedAtMs);
        funding.recordHoldDetail(hold.id, hold.requestHash, afterRefund, freshQueryStartedAt);
        ComputeFundingIntentService.Intent refundRelease = funding.prepareRefundRelease(hold.id, refund.reference);
        assertEquals("900", json.readTree(refundRelease.body).path("release_quota").textValue());
        assertNotEquals(finalRelease.id, refundRelease.id); assertNotEquals(finalRelease.reference, refundRelease.reference);
        assertNotEquals(finalRelease.key, refundRelease.key);
        assertEquals(refundRelease.key, funding.prepareRefundRelease(hold.id, refund.reference).key);
        ComputeFundingIntentService.Intent unchanged = funding.recover(finalRelease.id);
        assertEquals(finalRelease.key, unchanged.key); assertEquals(finalRelease.body, unchanged.body);
        assertEquals(finalRelease.requestHash, unchanged.requestHash);
    }

    private JsonNode holdDetail(ComputeFundingIntentService.Intent hold, long version, String captured, String released, String remaining) {
        ObjectNode detail = json.createObjectNode();
        detail.put("hold_id", "101"); detail.put("merchant_hold_id", hold.reference); detail.put("user_id", 123);
        detail.put("hold_quota", "2000"); detail.put("captured_quota", captured); detail.put("released_quota", released);
        detail.put("remaining_quota", remaining); detail.put("hold_status", "PARTIALLY_CAPTURED");
        detail.put("expires_at", "1799999999999"); detail.put("balance_version", version);
        detail.put("created_at", 1790000000); detail.put("updated_at", 1790000000 + version);
        return detail;
    }

    static class FakeGateway implements ExternalBillingGateway {
        int writes, queries; String lastKey, lastBody, lastChargeId;
        Result next = Result.of(Status.UNKNOWN);
        @Override public Result refund(String chargeId, String key, String body) {
            writes++; lastChargeId = chargeId; lastKey = key; lastBody = body;
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            return next;
        }
        @Override public Result byKey(String key) { queries++; return Result.of(Status.UNKNOWN); }
        @Override public Result byReference(String reference) { queries++; return Result.of(Status.UNKNOWN); }
        @Override public Result balance(String userId) { queries++; return Result.of(Status.UNKNOWN); }
        @Override public Result capture(String key, String body) { fail("Refund must not call capture"); return Result.of(Status.UNKNOWN); }
        @Override public boolean supportsAtomicCapture() { return false; }
    }
}
