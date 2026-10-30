package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** In-memory H2/fake evidence only; no external ledger or provider is exercised. */
@Tag("dev")
class ComputeFundingIntentTest {
    ComputeBillingSettlementTest fixture;
    ComputeFundingIntentService service;
    String usage;
    final String expiry = "1799999999999";

    @BeforeEach void database() throws Exception {
        fixture = new ComputeBillingSettlementTest();
        fixture.database();
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Actual migration source root required");
        Path migration = root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_052__compute_funding_intents.sql");
        try (java.sql.Connection c = fixture.jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource(migration.toFile()));
            ScriptUtils.executeSqlScript(c, new FileSystemResource(migration.resolveSibling("V1_2_0_053__compute_funding_hold_recovery.sql").toFile()));
        }
        usage = fixture.usage("tenant-a", "1", "500000", "1");
        service = restarted();
    }
    ComputeFundingIntentService restarted() { return new ComputeFundingIntentService(fixture.jdbc, fixture.manager, fixture.json); }
    ComputeFundingIntentService.Intent hold() { return service.prepareInitialHold(usage, "1000", expiry, "GPU_COMPUTE"); }
    JsonNode receipt(String id) throws Exception {
        return fixture.json.readTree("{\"success\":true,\"data\":{\"hold_id\":" + id + ",\"available_quota\":\"9000\",\"held_quota\":\"1000\"}}");
    }
    ComputeFundingIntentService.Intent acknowledged() throws Exception {
        ComputeFundingIntentService.Intent h = hold();
        return service.recordReceipt(h.id, h.requestHash, receipt("9007199254740993"));
    }
    JsonNode releaseReceipt(String id, String actual, String remaining) throws Exception {
        return fixture.json.readTree("{\"hold_id\":\"" + id + "\",\"held_quota\":\"" + remaining
            + "\",\"released_quota\":\"" + actual + "\",\"remaining_quota\":\"" + remaining + "\",\"hold_status\":\"RELEASED\"}");
    }
    JsonNode detail(ComputeFundingIntentService.Intent h, long version, String total, String captured, String released, String remaining) {
        com.fasterxml.jackson.databind.node.ObjectNode node = fixture.json.createObjectNode();
        node.put("hold_id", h.holdId == null ? "101" : h.holdId); node.put("merchant_hold_id", h.reference); node.put("user_id", h.payerUserId);
        node.put("hold_quota", total); node.put("captured_quota", captured); node.put("released_quota", released); node.put("remaining_quota", remaining);
        node.put("hold_status", "ACTIVE"); node.put("balance_version", version); node.put("expires_at", expiry);
        node.put("created_at", 1790000000); node.put("updated_at", 1790000000 + version);
        return node;
    }
    ComputeFundingIntentService.Intent observed() {
        ComputeFundingIntentService.Intent h = hold();
        return service.recordHoldDetail(h.id, h.requestHash, detail(h, 1, "1000", "0", "0", "1000"));
    }

    @Test void migrationOnlyCreatesEmptyLocalJournalAndDefaultBillingIsOff() {
        assertEquals(0, fixture.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent", Integer.class));
        assertFalse(new ExternalBillingProperties().isEnabled());
        assertFalse(new ExternalBillingProperties().isSafetyStopEnabled());
    }
    @Test void originalHoldSurvivesRestartWithoutChangingExpiryKeyBodyOrReference() {
        ComputeFundingIntentService.Intent h = hold();
        ComputeFundingIntentService.Intent replay = restarted().prepareInitialHold(usage, "1000", expiry, "GPU_COMPUTE");
        assertEquals(h.id, replay.id); assertEquals(h.key, replay.key); assertEquals(h.body, replay.body);
        assertEquals(h.requestHash, replay.requestHash); assertEquals(h.reference, replay.reference);
        assertEquals(h.path, replay.path); assertTrue(h.body.contains("\"expires_at\":\"" + expiry + "\""));
        assertEquals("PREPARED", replay.state); assertFalse(replay.safeToSend);
    }
    @Test void sameBusinessOperationRejectsChangedAmountExpiryAndReason() {
        ComputeFundingIntentService.Intent h = hold();
        assertThrows(IllegalArgumentException.class, () -> service.prepareInitialHold(usage, "1001", expiry, "GPU_COMPUTE"));
        assertThrows(IllegalArgumentException.class, () -> service.prepareInitialHold(usage, "1000", "1800000000000", "GPU_COMPUTE"));
        assertThrows(IllegalArgumentException.class, () -> service.prepareInitialHold(usage, "1000", expiry, "DIFFERENT_REASON"));
        assertEquals(h.body, service.recover(h.id).body); assertEquals(1, count());
    }
    @Test void payerAndMerchantComeFromVerifiedSessionAndCannotChangeExistingRequest() {
        ComputeFundingIntentService.Intent h = hold();
        assertEquals("123", h.payerUserId); assertEquals("vls-test", h.merchantId);
        fixture.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='124' WHERE usage_id=?", usage);
        assertThrows(IllegalStateException.class, () -> service.recover(h.id));
        assertThrows(IllegalStateException.class, () -> hold());
        fixture.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='123',merchant_id='other' WHERE usage_id=?", usage);
        assertThrows(IllegalStateException.class, () -> service.recover(h.id));
    }
    @Test void persistedLargeUserIdIsNeverConvertedThroughDouble() throws Exception {
        fixture.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='9223372036854775807' WHERE usage_id=?", usage);
        ComputeFundingIntentService.Intent h = hold();
        assertTrue(h.body.contains("\"user_id\":9223372036854775807"));
        assertEquals("9223372036854775807", fixture.json.readTree(service.recover(h.id).body).path("user_id").asText());
    }
    @Test void outOfRangeInputsLeaveNoPartialIntent() {
        for (String amount : new String[]{"9223372036854775808", "0", "01", "-1", "1.0", "1e3"})
            assertThrows(IllegalArgumentException.class, () -> service.prepareInitialHold(usage, amount, expiry, "GPU_COMPUTE"));
        assertThrows(IllegalArgumentException.class, () -> service.prepareInitialHold(usage, "1000", "9223372036854775808", "GPU_COMPUTE"));
        assertEquals(0, count());
    }
    @Test void concurrentPreparationCreatesOneCommittedKeyAndSnapshot() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1); List<Future<ComputeFundingIntentService.Intent>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < 6; i++) tasks.add(pool.submit(() -> { start.await(); return restarted().prepareInitialHold(usage, "1000", expiry, "GPU_COMPUTE"); }));
            start.countDown(); String key = null;
            for (Future<ComputeFundingIntentService.Intent> f : tasks) {
                ComputeFundingIntentService.Intent result = f.get(10, TimeUnit.SECONDS);
                if (key == null) key = result.key; else assertEquals(key, result.key);
            }
            assertEquals(1, count());
        } finally { pool.shutdownNow(); }
    }
    @Test void transactionRollbackDoesNotPublishHalfAnIntent() {
        new TransactionTemplate(fixture.manager).execute(tx -> { hold(); tx.setRollbackOnly(); return null; });
        assertEquals(0, count());
        assertNotNull(hold().key);
    }
    @Test void unknownRecoveryPreservesOriginalAndCannotReleaseGuessedHold() {
        ComputeFundingIntentService.Intent h = hold(); service.recordUnknown(h.id, h.requestHash);
        ComputeFundingIntentService.Intent unknown = restarted().recover(h.id);
        assertEquals("UNKNOWN", unknown.state); assertEquals(h.key, unknown.key); assertEquals(h.body, unknown.body);
        assertFalse(unknown.safeToSend);
        assertThrows(IllegalStateException.class, () -> service.prepareFinalRelease(h.id, "1000"));
        assertEquals(1, count());
    }
    @Test void receiptIsUnverifiedAndLateTimeoutCannotDowngradeIt() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged();
        assertEquals("RECEIPT_UNVERIFIED", h.state); assertEquals("9007199254740993", h.holdId); assertFalse(h.safeToSend);
        ComputeFundingIntentService.Intent timeout = service.recordUnknown(h.id, h.requestHash);
        assertEquals(h.state, timeout.state); assertEquals(h.receipt, timeout.receipt);
        assertEquals(h.receipt, restarted().recordReceipt(h.id, h.requestHash, receipt("\"9007199254740993\"")).receipt);
    }
    @Test void unknownMayAdvanceToReceiptButNeverToAssumedSuccess() throws Exception {
        ComputeFundingIntentService.Intent h = hold(); service.recordUnknown(h.id, h.requestHash);
        assertEquals("RECEIPT_UNVERIFIED", restarted().recordReceipt(h.id, h.requestHash, receipt("101")).state);
        assertFalse(restarted().recover(h.id).safeToSend);
    }
    @Test void conflictingReceiptIsStickyAndPreservesBothObservations() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged();
        ComputeFundingIntentService.Intent conflict = service.recordReceipt(h.id, h.requestHash, receipt("102"));
        assertEquals("CONFLICT", conflict.state); assertEquals(h.receipt, conflict.receipt);
        assertTrue(conflict.conflictingReceipt.contains("102"));
        assertEquals("CONFLICT", service.recordUnknown(h.id, h.requestHash).state);
        assertEquals("CONFLICT", service.recordReceipt(h.id, h.requestHash, receipt("9007199254740993")).state);
        assertThrows(IllegalStateException.class, () -> service.prepareFinalRelease(h.id, "1000"));
    }
    @Test void responseMustMatchPersistedRequestHash() throws Exception {
        ComputeFundingIntentService.Intent h = hold();
        assertThrows(IllegalArgumentException.class, () -> service.recordReceipt(h.id, "wrong", receipt("101")));
        assertThrows(IllegalArgumentException.class, () -> service.recordUnknown(h.id, "wrong"));
        assertEquals("PREPARED", service.recover(h.id).state);
    }
    @Test void invalidReceiptRollsBackWithoutPretendingTheHoldFailedOrSucceeded() throws Exception {
        ComputeFundingIntentService.Intent h = hold();
        for (String value : new String[]{"0", "9223372036854775808", "101.0", "1e2"})
            assertThrows(IllegalArgumentException.class, () -> service.recordReceipt(h.id, h.requestHash, receipt(value)));
        assertEquals("PREPARED", service.recover(h.id).state);
    }
    @Test void releaseUsesOriginalHoldAndOnlyDocumentedFieldsButRemainsBlocked() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged();
        ComputeFundingIntentService.Intent r = service.prepareFinalRelease(h.id, "900");
        assertEquals("{\"merchant_id\":\"vls-test\",\"merchant_release_id\":\"vls-release:" + usage + ":final\",\"user_id\":123,\"release_quota\":\"900\"}", r.body);
        assertEquals("/api/v1/external-billing/holds/9007199254740993/release", r.path);
        assertEquals(h.id, r.parentId); assertEquals("PREPARED", r.state); assertFalse(r.safeToSend);
        assertNotEquals(h.key, r.key);
        assertEquals(r.key, restarted().prepareFinalRelease(h.id, "900").key);
        assertThrows(IllegalArgumentException.class, () -> service.prepareFinalRelease(h.id, "901"));
        assertThrows(IllegalArgumentException.class, () -> service.prepareFinalRelease(h.id, "1001"));
    }
    @Test void releaseResponseNeverClearsContractBlockOrMutatesTheHold() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged();
        ComputeFundingIntentService.Intent r = service.prepareFinalRelease(h.id, "1000");
        JsonNode releaseResponse = releaseReceipt(h.holdId, "1000", "0");
        ComputeFundingIntentService.Intent done = service.recordReceipt(r.id, r.requestHash, releaseResponse);
        assertEquals("RECEIPT_UNVERIFIED", done.state); assertFalse(done.safeToSend);
        assertEquals(h.receipt, service.recover(h.id).receipt); assertEquals(h.holdId, service.recover(h.id).holdId);
        assertEquals(r.body, service.recordUnknown(r.id, r.requestHash).body);
    }
    @Test void releaseReceiptForAnotherHoldIsAStickyConflict() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged();
        ComputeFundingIntentService.Intent r = service.prepareFinalRelease(h.id, "1000");
        ComputeFundingIntentService.Intent conflict = service.recordReceipt(r.id, r.requestHash, releaseReceipt("102", "1000", "0"));
        assertEquals("CONFLICT", conflict.state); assertNull(conflict.receipt);
        assertTrue(conflict.conflictingReceipt.contains("102")); assertFalse(conflict.safeToSend);
        assertEquals("CONFLICT", service.recordUnknown(r.id, r.requestHash).state);
        assertEquals("CONFLICT", service.recordReceipt(r.id, r.requestHash, releaseReceipt("9007199254740993", "1000", "0")).state);
    }
    @Test void aLaterParentConflictBlocksAnAlreadyPreparedReleaseWithoutChangingItsRequest() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged();
        ComputeFundingIntentService.Intent r = service.prepareFinalRelease(h.id, "1000");
        service.recordReceipt(h.id, h.requestHash, receipt("102"));
        ComputeFundingIntentService.Intent recovered = restarted().recover(r.id);
        assertEquals("CONFLICT", recovered.state); assertEquals(r.key, recovered.key); assertEquals(r.body, recovered.body);
        assertFalse(recovered.safeToSend);
        assertEquals("CONFLICT", service.recordUnknown(r.id, r.requestHash).state);
    }
    @Test void existingChargeSliceBytesNeverChangeWhenFundingEvidenceIsRecorded() throws Exception {
        String ref = fixture.interval(usage, 3600000);
        Map<String, Object> before = fixture.row(ref);
        ComputeFundingIntentService.Intent h = acknowledged();
        ComputeFundingIntentService.Intent r = service.prepareFinalRelease(h.id, "1000");
        service.recordUnknown(r.id, r.requestHash);
        Map<String, Object> after = fixture.row(ref);
        for (String key : new String[]{"request_json", "request_hash", "idempotency_key", "merchant_charge_id", "amount_text", "state"})
            assertEquals(before.get(key), after.get(key));
        assertFalse(String.valueOf(after.get("request_json")).contains("hold_id"));
        assertEquals(0, fixture.gateway.captures.size());
    }
    @Test void corruptedBodyHashOrAssociationStopsRecovery() {
        ComputeFundingIntentService.Intent h = hold();
        fixture.jdbc.update("UPDATE vls_compute_funding_intent SET request_json=? WHERE intent_id=?", h.body.replace("1000", "1001"), h.id);
        assertThrows(IllegalStateException.class, () -> service.recover(h.id));
        fixture.jdbc.update("UPDATE vls_compute_funding_intent SET request_json=?,request_path='/wrong' WHERE intent_id=?", h.body, h.id);
        assertThrows(IllegalStateException.class, () -> service.recover(h.id));
    }
    @Test void recomputedHashCannotHideAnAddedPayerOrCaptureField() {
        ComputeFundingIntentService.Intent h = hold();
        String changed = h.body.substring(0, h.body.length() - 1) + ",\"hold_id\":101}";
        fixture.jdbc.update("UPDATE vls_compute_funding_intent SET request_json=?,request_hash=? WHERE intent_id=?", changed, ComputeBillingSettlementService.hash(changed), h.id);
        assertThrows(IllegalStateException.class, () -> service.recover(h.id));
    }
    @Test void databaseAlsoRejectsASecondKeyForTheSameBusinessReference() {
        ComputeFundingIntentService.Intent h = hold();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> fixture.jdbc.update(
            "INSERT INTO vls_compute_funding_intent(intent_id,usage_id,operation_kind,business_reference,merchant_id,payer_user_id,idempotency_key,request_path,request_json,request_hash,state) VALUES('different',?,'HOLD',?,'vls-test','123','different-key',?,?,?,'CONTRACT_BLOCKED')",
            usage, h.reference, h.path, h.body, h.requestHash));
        assertEquals(h.key, service.recover(h.id).key);
    }

    @Test void lostHoldResponseRecoversByOriginalReferenceAndBindsOnlyExactDetail() {
        ComputeFundingIntentService.Intent h = hold(); service.recordUnknown(h.id, h.requestHash);
        ComputeFundingIntentService.Intent recovered = service.recordHoldDetail(h.id, h.requestHash, detail(h, 1, "1000", "0", "0", "1000"));
        assertEquals("DETAIL_OBSERVED", recovered.state); assertEquals("101", recovered.holdId);
        Map<String, Object> session = fixture.jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", usage);
        assertEquals(h.id, session.get("funding_hold_intent_id")); assertEquals("101", session.get("funding_hold_id"));
        assertEquals(Long.parseLong(expiry), ((Number) session.get("funding_expires_at_ms")).longValue());
        assertEquals(h.body, restarted().recover(h.id).body); assertFalse(recovered.safeToSend);
    }
    @Test void wrongUserReferenceExpiryOrInitialAmountCannotEstablishHoldBinding() {
        for (String field : new String[]{"user_id", "merchant_hold_id", "expires_at", "hold_quota"}) {
            ComputeFundingIntentService.Intent h = hold();
            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) detail(h, 1, "1000", "0", "0", "1000");
            node.put(field, "user_id".equals(field) ? "124" : "merchant_hold_id".equals(field) ? "other" : "expires_at".equals(field) ? "1800000000000" : "999");
            if ("hold_quota".equals(field)) node.put("remaining_quota", "999");
            assertEquals("CONFLICT", service.recordHoldDetail(h.id, h.requestHash, node).state);
            assertNull(fixture.jdbc.queryForObject("SELECT funding_hold_id FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
            fixture.jdbc.update("DELETE FROM vls_compute_funding_intent WHERE intent_id=?", h.id);
        }
    }
    @Test void sameVersionDifferentCountersIsStickyAndClearsCaptureBindingWithStopRisk() {
        ComputeFundingIntentService.Intent h = observed();
        assertEquals("CONFLICT", service.recordHoldDetail(h.id, h.requestHash, detail(h, 1, "1000", "100", "0", "900")).state);
        Map<String, Object> session = fixture.jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", usage);
        assertNull(session.get("funding_hold_id")); assertNotNull(session.get("risk_code"));
        assertEquals(1, ((Number) session.get("stop_requested")).intValue());
        assertEquals("CONFLICT", service.recordHoldDetail(h.id, h.requestHash, detail(h, 2, "1000", "0", "0", "1000")).state);
    }
    @Test void delayedOlderDetailCannotRollBackLatestCountersOrExpiry() {
        ComputeFundingIntentService.Intent h = observed();
        service.recordHoldDetail(h.id, h.requestHash, detail(h, 3, "1000", "200", "0", "800"));
        service.recordHoldDetail(h.id, h.requestHash, detail(h, 2, "1000", "0", "0", "1000"));
        assertEquals("800", fixture.jdbc.queryForObject("SELECT remaining_quota FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?", String.class, h.id));
    }
    @Test void multipleTopupsAreIndependentAndNeverRewriteExpiryOrInitialHold() {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent one = service.prepareTopup(h.id, "topup-1", "50", "METERING", 1790000000000L);
        ComputeFundingIntentService.Intent two = service.prepareTopup(h.id, "topup-2", "60", "METERING", 1790000000000L);
        assertNotEquals(one.key, two.key); assertFalse(one.body.contains("expires_at")); assertEquals(h.body, service.recover(h.id).body);
        assertEquals(one.key, restarted().prepareTopup(h.id, "topup-1", "50", "METERING", Long.parseLong(expiry) + 1).key);
        assertThrows(IllegalArgumentException.class, () -> service.prepareTopup(h.id, "topup-1", "51", "METERING", 1790000000000L));
        assertEquals(3, count());
    }
    @Test void expiredHoldRejectsNewTopupAndCaptureClaimButStillAllowsReleasePreparation() {
        ComputeFundingIntentService.Intent h = observed();
        assertThrows(IllegalStateException.class, () -> service.prepareTopup(h.id, "late-topup", "10", "METERING", Long.parseLong(expiry)));
        ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "2000");
        assertNotNull(service.claimInitialSend(release.id, Long.parseLong(expiry) + 1));
        assertEquals("2000", fixture.json.valueToTree(parseBody(release.body)).path("release_quota").asText());
    }
    @Test void actualClippedReleaseIsRecordedWithoutClaimingTheRequestedAmountReleased() throws Exception {
        ComputeFundingIntentService.Intent h = observed(); ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "2000");
        ComputeFundingIntentService.Intent done = service.recordReceipt(release.id, release.requestHash, releaseReceipt(h.holdId, "1000", "0"));
        assertTrue(done.receipt.contains("\"released_quota\":\"1000\"")); assertTrue(done.body.contains("\"release_quota\":\"2000\""));
        assertEquals("RECEIPT_UNVERIFIED", done.state); assertFalse(done.safeToSend);
    }
    @Test void contradictoryHoldReceiptAfterDetailClearsBindingAndKeepsStickyConflict() throws Exception {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent conflict = service.recordReceipt(h.id, h.requestHash, receipt("102"));
        assertEquals("CONFLICT", conflict.state);
        assertNull(fixture.jdbc.queryForObject("SELECT funding_hold_id FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
        assertEquals("CONFLICT", restarted().recover(h.id).state);
    }
    @Test void preparationClaimPersistsUncertaintyAndOnlyOneConcurrentSenderWins() throws Exception {
        ComputeFundingIntentService.Intent h = hold();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<ComputeFundingIntentService.Intent>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) futures.add(pool.submit(() -> restarted().claimInitialSend(h.id, 1790000000000L)));
            int wins = 0; for (Future<ComputeFundingIntentService.Intent> f : futures) if (f.get(10, TimeUnit.SECONDS) != null) wins++;
            assertEquals(1, wins); assertEquals("UNKNOWN", service.recover(h.id).state); assertEquals(h.key, service.recover(h.id).key);
        } finally { pool.shutdownNow(); }
    }
    @Test void recoveryCoordinatorDefaultOffNeedsNoGatewayAndNeverClaimsOrSends() {
        ComputeFundingIntentService.Intent h = hold();
        ComputeFundingRecoveryService coordinator = new ComputeFundingRecoveryService(service,
            (java.util.function.Supplier<ExternalBillingGateway>) () -> { throw new AssertionError("Disabled gateway must not be resolved"); }, new ExternalBillingProperties());
        assertEquals("PREPARED", coordinator.advance(h.id, 1790000000000L).state);
    }
    @Test void disabledSpringContextStartsWithoutExternalGatewayBean() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(ComputeFundingIntentService.class, () -> service);
            context.registerBean(ExternalBillingProperties.class, ExternalBillingProperties::new);
            context.register(ComputeFundingRecoveryService.class);
            context.refresh();
            assertEquals("PREPARED", context.getBean(ComputeFundingRecoveryService.class).advance(hold().id, 1790000000000L).state);
            assertTrue(context.getBeansOfType(ExternalBillingGateway.class).isEmpty());
        }
    }
    @Test void sendResponseLostThenOriginalReferenceQueryRecoversWithoutASecondHold() {
        ComputeFundingIntentService.Intent h = hold();
        FundingGateway gateway = new FundingGateway(); gateway.resource = detail(h, 1, "1000", "0", "0", "1000");
        ComputeFundingRecoveryService coordinator = coordinator(gateway);
        assertEquals("UNKNOWN", coordinator.advance(h.id, 1790000000000L).state);
        assertEquals(1, gateway.writes); assertEquals(h.key, gateway.lastKey); assertEquals(h.body, gateway.lastBody);
        assertEquals("DETAIL_OBSERVED", coordinator.advance(h.id, 1790000000001L).state);
        assertEquals(1, gateway.writes); assertEquals(1, gateway.operations); assertEquals(1, gateway.references);
        assertEquals(h.body, restarted().recover(h.id).body);
    }
    @Test void changedResourceCountersCannotResolveAnUnknownTopupOperation() {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent topup = service.prepareTopup(h.id, "topup-unknown", "100", "METERING", 1790000000000L);
        FundingGateway gateway = new FundingGateway(); gateway.resource = detail(h, 2, "1100", "0", "0", "1100");
        ComputeFundingRecoveryService coordinator = coordinator(gateway);
        assertEquals("UNKNOWN", coordinator.advance(topup.id, 1790000000000L).state);
        assertEquals("UNKNOWN", coordinator.advance(topup.id, 1790000000001L).state);
        assertEquals(1, gateway.writes); assertEquals(1, gateway.operations); assertEquals(1, gateway.ids);
        assertEquals("1100", fixture.jdbc.queryForObject("SELECT remaining_quota FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?", String.class, h.id));
        assertEquals(topup.body, service.recover(topup.id).body); assertFalse(service.recoveryPlan(topup.id).operationResponseSpecified);
    }
    @Test void fundingTransportRejectsAnOuterTransactionBeforeClaimOrHttp() {
        ComputeFundingIntentService.Intent h = hold(); FundingGateway gateway = new FundingGateway();
        ComputeFundingRecoveryService coordinator = coordinator(gateway);
        new TransactionTemplate(fixture.manager).execute(tx -> {
            assertThrows(IllegalStateException.class, () -> coordinator.advance(h.id, 1790000000000L)); return null;
        });
        assertEquals(0, gateway.writes); assertEquals(0, gateway.operations); assertEquals("PREPARED", service.recover(h.id).state);
    }
    @Test void conflictingChildReceiptStopsPreviouslyPreparedTopupButKeepsParentForCleanup() throws Exception {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent first = service.prepareTopup(h.id, "topup-first", "100", "METERING", 1790000000000L);
        ComputeFundingIntentService.Intent second = service.prepareTopup(h.id, "topup-second", "100", "METERING", 1790000000000L);
        com.fasterxml.jackson.databind.node.ObjectNode wrong = (com.fasterxml.jackson.databind.node.ObjectNode) detail(h, 2, "1100", "0", "0", "1100");
        wrong.put("hold_id", "102");
        assertEquals("CONFLICT", service.recordReceipt(first.id, first.requestHash, wrong).state);
        assertEquals("FUNDING_OPERATION_CONFLICT", fixture.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
        FundingGateway gateway = new FundingGateway();
        assertEquals("PREPARED", coordinator(gateway).advance(second.id, 1790000000001L).state);
        assertEquals(0, gateway.writes); assertEquals(h.holdId, service.recover(h.id).holdId);
        assertNotNull(service.prepareFinalRelease(h.id, "1000").key);
    }
    @Test void knownRevocationBlocksPreparedTopupAndPermitsOnlyCleanupRelease() {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent topup = service.prepareTopup(h.id, "topup-before-revocation", "100", "METERING", 1790000000000L);
        fixture.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='NOT_AUTHORIZED',stop_requested=1 WHERE usage_id=?", usage);
        FundingGateway gateway = new FundingGateway();
        assertEquals("PREPARED", coordinator(gateway).advance(topup.id, 1790000000001L).state); assertEquals(0, gateway.writes);
        ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "1000");
        assertNotNull(service.claimInitialSend(release.id, Long.parseLong(expiry) + 1));
    }
    @Test void legacyReleaseKeepsOriginalBodyHashAndCannotBeClaimedByNewDispatcher() throws Exception {
        ComputeFundingIntentService.Intent h = acknowledged(); ComputeFundingIntentService.Intent r = service.prepareFinalRelease(h.id, "900");
        String legacy = FundingContracts.release(h.merchantId, h.payerUserId, "900").toString();
        fixture.jdbc.update("UPDATE vls_compute_funding_intent SET contract_version='LEGACY_V2',state='CONTRACT_BLOCKED',request_json=?,request_hash=? WHERE intent_id=?",
            legacy, ComputeBillingSettlementService.hash(legacy), r.id);
        ComputeFundingIntentService.Intent recovered = restarted().prepareFinalRelease(h.id, "900");
        assertEquals(legacy, recovered.body); assertEquals(r.key, recovered.key); assertNull(service.claimInitialSend(r.id, 1790000000000L));
    }

    @Test void lostTopupReceiptReplaysExactOriginalOnceAndConfirmsOneFinancialEffect() {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent topup = service.prepareTopup(h.id, "topup-replay", "100", "METERING", 1790000000000L);
        ReplayGateway gateway = new ReplayGateway(h); gateway.loseFirstReply = true;
        ComputeFundingRecoveryService recovery = coordinator(gateway);
        assertEquals("UNKNOWN", recovery.advance(topup.id, 1790000000000L).state);
        assertEquals(1, gateway.effects); assertEquals(1, gateway.writes);
        assertEquals("OPERATION_CONFIRMED", recovery.advance(topup.id, 1790000000001L).state);
        assertEquals(2, gateway.writes); assertEquals(1, gateway.effects);
        assertEquals(topup.key, gateway.lastKey); assertEquals(topup.body, gateway.lastBody);
        assertEquals(topup.path, restarted().recover(topup.id).path);
        recovery.advance(topup.id, 1790000000002L); assertEquals(2, gateway.writes);
    }

    @Test void lostReleaseReceiptAfterRefundReturnsOriginalResultWithoutReleasingReturnedFunds() {
        ComputeFundingIntentService.Intent h = observed();
        service.recordHoldDetail(h.id, h.requestHash, detail(h, 2, "1000", "400", "0", "600"));
        ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "600");
        ReplayGateway gateway = new ReplayGateway(h); gateway.captured = 400; gateway.version = 2; gateway.loseFirstReply = true;
        ComputeFundingRecoveryService recovery = coordinator(gateway);
        assertEquals("UNKNOWN", recovery.advance(release.id, 1790000000000L).state);
        assertEquals(600, gateway.released); assertEquals(0, gateway.remaining());
        gateway.captured = 200; gateway.version++; // A separately proved refund returns 200 to this hold.
        assertEquals("OPERATION_CONFIRMED", recovery.advance(release.id, 1790000000001L).state);
        assertEquals(2, gateway.writes); assertEquals(1, gateway.effects); assertEquals(200, gateway.remaining());
        assertEquals("600", parseBody(service.recover(release.id).receipt).path("released_quota").asText());
        assertEquals(release.key, gateway.lastKey); assertEquals(release.body, gateway.lastBody);
        recovery.advance(release.id, 1790000000002L); assertEquals(2, gateway.writes); assertEquals(200, gateway.remaining());
    }

    @Test void existingChildReceiptNeedsFreshParentQueryButNeverAnotherPost() {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "1000");
        ReplayGateway gateway = new ReplayGateway(h); gateway.failDetail = true;
        ComputeFundingRecoveryService recovery = coordinator(gateway);
        assertEquals("RECEIPT_UNVERIFIED", recovery.advance(release.id, 1790000000000L).state);
        assertEquals("RECEIPT_UNVERIFIED", recovery.advance(release.id, 1790000000001L).state);
        assertEquals(1, gateway.writes);
        gateway.failDetail = false;
        assertEquals("OPERATION_CONFIRMED", recovery.advance(release.id, 1790000000002L).state);
        assertEquals(1, gateway.writes); assertEquals(0, gateway.operations);
    }

    @Test void explicitOperationStatesBlockOriginalReplay() throws Exception {
        for (ExternalBillingGateway.Status status : new ExternalBillingGateway.Status[]{ExternalBillingGateway.Status.PROCESSING,
            ExternalBillingGateway.Status.REJECTED, ExternalBillingGateway.Status.CONFLICT, ExternalBillingGateway.Status.NOT_AUTHORIZED}) {
            database(); ComputeFundingIntentService.Intent h = observed();
            ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "1000");
            service.recordUnknown(release.id, release.requestHash);
            ReplayGateway gateway = new ReplayGateway(h); gateway.lookup = new ExternalBillingGateway.Result(status, "TEST_REJECTED", null);
            ComputeFundingIntentService.Intent result = coordinator(gateway).advance(release.id, 1790000000000L);
            assertEquals(0, gateway.writes);
            assertEquals(status == ExternalBillingGateway.Status.PROCESSING ? "UNKNOWN"
                : status == ExternalBillingGateway.Status.CONFLICT ? "CONFLICT" : "REJECTED", result.state);
            if (status == ExternalBillingGateway.Status.NOT_AUTHORIZED)
                assertEquals("NOT_AUTHORIZED", fixture.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
        }
    }

    @Test void partialLookupWrongOwnerOrHoldBlocksReplayWithoutInventingMissingSchema() throws Exception {
        for (String field : new String[]{"idempotency_key", "user_id", "hold_id"}) {
            database(); ComputeFundingIntentService.Intent h = observed();
            ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "1000");
            service.recordUnknown(release.id, release.requestHash);
            ReplayGateway gateway = new ReplayGateway(h);
            com.fasterxml.jackson.databind.node.ObjectNode evidence = fixture.json.createObjectNode(); evidence.put(field, "999"); evidence.put("status", "SUCCEEDED");
            gateway.lookup = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", evidence);
            assertEquals("CONFLICT", coordinator(gateway).advance(release.id, 1790000000000L).state);
            assertEquals(0, gateway.writes);
        }
    }

    @Test void expiredOrRevokedUnknownTopupAndWrongHoldCannotReplay() throws Exception {
        ComputeFundingIntentService.Intent h = observed();
        ComputeFundingIntentService.Intent topup = service.prepareTopup(h.id, "topup-expired-replay", "100", "METERING", 1790000000000L);
        service.recordUnknown(topup.id, topup.requestHash); ReplayGateway gateway = new ReplayGateway(h);
        gateway.lookup = associatedLookup(topup, h);
        assertEquals("UNKNOWN", coordinator(gateway).advance(topup.id, Long.parseLong(expiry) + 1).state); assertEquals(0, gateway.writes);
        fixture.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='NOT_AUTHORIZED',stop_requested=1 WHERE usage_id=?", usage);
        assertEquals("UNKNOWN", coordinator(gateway).advance(topup.id, 1790000000000L).state); assertEquals(0, gateway.writes);
        database(); h = observed(); ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "1000");
        service.recordUnknown(release.id, release.requestHash); gateway = new ReplayGateway(h); gateway.wrongDetailOwner = true;
        gateway.lookup = associatedLookup(release, h);
        assertEquals("CONFLICT", coordinator(gateway).advance(release.id, 1790000000000L).state); assertEquals(0, gateway.writes);
    }

    @Test void missingOrUnspecifiedLookupCannotAuthorizeReplayEvenWithFreshZeroRemaining() throws Exception {
        for (String missing : new String[]{"all", "status", "idempotency_key", "user_id", "hold_id"}) {
            database(); ComputeFundingIntentService.Intent h = observed();
            ComputeFundingIntentService.Intent release = service.prepareFinalRelease(h.id, "1000");
            service.recordUnknown(release.id, release.requestHash); ReplayGateway gateway = new ReplayGateway(h);
            gateway.released = 1000; gateway.version = 2;
            com.fasterxml.jackson.databind.node.ObjectNode partial = (com.fasterxml.jackson.databind.node.ObjectNode) associatedLookup(release, h).charge;
            partial.remove(missing);
            gateway.lookup = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", "all".equals(missing) ? null : partial);
            assertEquals("UNKNOWN", coordinator(gateway).advance(release.id, 1790000000000L).state);
            assertEquals(0, gateway.writes); assertNull(service.recover(release.id).receipt);
        }
    }

    ExternalBillingGateway.Result associatedLookup(ComputeFundingIntentService.Intent intent, ComputeFundingIntentService.Intent hold) {
        com.fasterxml.jackson.databind.node.ObjectNode partial = fixture.json.createObjectNode();
        partial.put("status", "SUCCEEDED"); partial.put("idempotency_key", intent.key);
        partial.put("user_id", intent.payerUserId); partial.put("hold_id", hold.holdId);
        return new ExternalBillingGateway.Result(ExternalBillingGateway.Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", partial);
    }
    JsonNode parseBody(String body) { try { return fixture.json.readTree(body); } catch (Exception e) { throw new IllegalStateException(e); } }
    ComputeFundingRecoveryService coordinator(ExternalBillingGateway gateway) {
        ExternalBillingProperties properties = new ExternalBillingProperties(); properties.setEnabled(true); properties.setAllowWrites(true);
        return new ComputeFundingRecoveryService(service, (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway, properties);
    }
    class FundingGateway implements ExternalBillingGateway {
        int writes, operations, references, ids;
        String lastKey, lastBody;
        JsonNode resource;
        @Override public Result byKey(String key) { return Result.of(Status.UNKNOWN); }
        @Override public Result byReference(String reference) { return Result.of(Status.UNKNOWN); }
        @Override public Result balance(String userId) { return Result.of(Status.UNKNOWN); }
        @Override public Result capture(String key, String body) { throw new AssertionError("No capture in funding recovery"); }
        @Override public boolean supportsAtomicCapture() { return true; }
        @Override public Result createHold(String key, String body) { writes++; lastKey = key; lastBody = body; throw new IllegalStateException("TEST_ONLY_LOST_RESPONSE"); }
        @Override public Result topupHold(String id, String key, String body) { return createHold(key, body); }
        @Override public Result releaseHold(String id, String key, String body) { return createHold(key, body); }
        @Override public Result holdOperation(String op, String ref) { operations++; return new Result(Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", null); }
        @Override public Result holdByReference(String ref) { references++; return new Result(Status.SUCCEEDED, "", resource); }
        @Override public Result holdById(String id) { ids++; return new Result(Status.SUCCEEDED, "", resource); }
    }
    /** A local model of the documented same-key + same-body result cache. HTTP attempts and
     * wallet effects are deliberately separate counters; no real wallet is contacted. */
    class ReplayGateway extends FundingGateway {
        final ComputeFundingIntentService.Intent hold;
        final Map<String, JsonNode> receipts = new java.util.LinkedHashMap<>();
        final Map<String, String> bodies = new java.util.LinkedHashMap<>();
        int total = 1000, captured, released, version = 1, effects;
        boolean loseFirstReply, failDetail, wrongDetailOwner;
        Result lookup = Result.of(Status.UNKNOWN);
        ReplayGateway(ComputeFundingIntentService.Intent hold) { this.hold = hold; }
        int remaining() { return total - captured - released; }
        @Override public Result topupHold(String id, String key, String body) { return apply(id, key, body, true); }
        @Override public Result releaseHold(String id, String key, String body) { return apply(id, key, body, false); }
        Result apply(String id, String key, String body, boolean topup) {
            assertEquals(hold.holdId, id); assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            writes++; lastKey = key; lastBody = body;
            if (receipts.containsKey(key)) {
                assertEquals(bodies.get(key), body); return new Result(Status.SUCCEEDED, "", receipts.get(key));
            }
            JsonNode request = parseBody(body); int actual = 0;
            if (topup) total += request.path("topup_quota").asInt();
            else { actual = Math.min(remaining(), request.path("release_quota").asInt()); released += actual; }
            version++; effects++;
            JsonNode receipt = currentDetail();
            if (!topup) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) receipt).put("released_quota", Integer.toString(actual));
                ((com.fasterxml.jackson.databind.node.ObjectNode) receipt).put("held_quota", Integer.toString(remaining()));
            }
            bodies.put(key, body); receipts.put(key, receipt);
            return loseFirstReply ? Result.of(Status.UNKNOWN) : new Result(Status.SUCCEEDED, "", receipt);
        }
        JsonNode currentDetail() {
            com.fasterxml.jackson.databind.node.ObjectNode result = (com.fasterxml.jackson.databind.node.ObjectNode)
                detail(hold, version, Integer.toString(total), Integer.toString(captured), Integer.toString(released), Integer.toString(remaining()));
            if (wrongDetailOwner) result.put("user_id", "999");
            return result;
        }
        @Override public Result holdById(String id) { ids++; return failDetail ? Result.of(Status.UNKNOWN) : new Result(Status.SUCCEEDED, "", currentDetail()); }
        @Override public Result holdOperation(String op, String ref) {
            operations++;
            if (lookup.status != Status.UNKNOWN || lookup.charge != null || lastKey == null) return lookup;
            com.fasterxml.jackson.databind.node.ObjectNode partial = fixture.json.createObjectNode();
            partial.put("idempotency_key", lastKey); partial.put("hold_id", hold.holdId); partial.put("user_id", hold.payerUserId); partial.put("status", "SUCCEEDED");
            return new Result(Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", partial);
        }
    }
    int count() { return fixture.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent", Integer.class); }
}
