package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Existing migration schemas with H2/fake transport only. No network, real wallet or GPU. */
@Tag("dev")
class TrialFundsIsolationTest {
    ComputeFundingIntentTest fundingFixture;
    ComputeBillingSettlementTest f;
    ExternalBillingProperties properties;
    ComputeFundingIntentService funding;
    ComputeBillingSettlementService settlement;
    ComputeRefundIntentService refunds;
    LocalGateway gateway;
    String usage;
    String run = UUID.randomUUID().toString();
    ObjectNode snapshot;
    ObjectNode config;
    final String reserve = "500000";

    @BeforeEach void database() throws Exception {
        fundingFixture = new ComputeFundingIntentTest(); fundingFixture.database();
        f = fundingFixture.fixture; usage = fundingFixture.usage;
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        assertNotNull(root);
        Path migrations = root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration");
        for (String name : new String[]{"V1_2_0_054__compute_corrective_refund_intents.sql", "V1_2_0_056__compute_paid_run_orchestration.sql"}) {
            try (java.sql.Connection c = f.jdbc.getDataSource().getConnection()) {
                ScriptUtils.executeSqlScript(c, new FileSystemResource(migrations.resolve(name).toFile()));
            }
        }
        properties = f.properties; properties.setBaseUrl(ComputeBillingTrialGuard.TARGET);
        properties.setMerchantId("vls"); properties.setAllowWrites(true);
        properties.getTrial().setEnabled(true); properties.getTrial().setRunId(run);
        properties.getTrial().setMaximumWalletCny("1.00");
        properties.getTrial().setExpiresAt(Instant.now().plusSeconds(3600).toString());
        snapshot = (ObjectNode) f.json.readTree(f.jdbc.queryForObject(
            "SELECT accepted_snapshot FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
        snapshot.put("approved_hourly_price", "13.00");
        ((ObjectNode) snapshot.path("accepted_rate")).put("pricePolicy", "VLS_FIXED");
        snapshot.putObject(ComputeBillingTrialGuard.MARKER).put("version", 1).put("run_id", run)
            .put("payer_user_id", "13").put("instance_id", "pro-7920a22db144").put("instance_key", "official-test-key")
            .put("node_id", "-6000").put("merchant_id", "vls").put("base_url", ComputeBillingTrialGuard.TARGET)
            .put("maximum_cny", "1.00").put("maximum_quota", reserve).put("reservation_quota", reserve)
            .put("expires_at", properties.getTrial().getExpiresAt());
        f.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='13',merchant_id='vls',instance_ref='node-server:-6000',accepted_snapshot=? WHERE usage_id=?", snapshot.toString(), usage);
        f.jdbc.update("INSERT INTO vls_compute_paid_run(run_id,node_id,instance_key,tenant_id,opening_user_id,platform_subject,payer_user_id,merchant_id,quote_snapshot,reserve_quota,quote_until_ms,hold_until_ms,maximum_run_seconds,usage_id,start_operation_id,state) VALUES(?,-6000,'official-test-key','tenant-a','actor-test','platform-test','13','vls',?,?,?, ?,600,?,?,'FUNDING')",
            run, snapshot.toString(), reserve, System.currentTimeMillis() + 60000, Long.parseLong(fundingFixture.expiry), usage, UUID.randomUUID().toString());
        config = f.json.createObjectNode().put("settlement_currency", "CNY");
        for (String field : new String[]{"quota_unit", "conversion_version", "effective_at", "valid_until"}) config.set(field, snapshot.get(field));
        config.putObject("quota_per_currency_unit").put("numerator", snapshot.path("numerator").asText())
            .put("denominator", snapshot.path("denominator").asText());
        gateway = new LocalGateway();
        restart();
    }

    void restart() {
        refunds = new ComputeRefundIntentService(f.jdbc, f.manager, f.json, properties,
            (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway);
        funding = new ComputeFundingIntentService(f.jdbc, f.manager, f.json, () -> refunds, properties);
        settlement = new ComputeBillingSettlementService(f.jdbc, f.manager, f.json, properties,
            (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway);
    }

    ComputeFundingRecoveryService recovery() {
        return new ComputeFundingRecoveryService(funding, (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway, properties);
    }

    ComputeFundingIntentService.Intent hold() {
        ComputeFundingIntentService.Intent hold = funding.prepareInitialHold(usage, reserve, fundingFixture.expiry, "GPU_COMPUTE");
        f.jdbc.update("UPDATE vls_compute_paid_run SET hold_intent_id=? WHERE run_id=?", hold.id, run);
        return hold;
    }

    ComputeFundingIntentService.Intent observedHold() {
        ComputeFundingIntentService.Intent hold = hold();
        JsonNode detail = fundingFixture.detail(hold, 1, reserve, "0", "0", reserve);
        return funding.recordHoldDetail(hold.id, hold.requestHash, detail, System.currentTimeMillis());
    }

    String interval(long millis) {
        settlement.recordRunningObservation(usage, Instant.ofEpochMilli(f.start + millis));
        return settlement.prepareInterval(usage, f.start + millis);
    }

    Map<String, Object> slice(String reference) {
        return f.jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=?", reference);
    }

    String settledCharge() {
        observedHold(); String reference = interval(60000);
        settlement.dispatch(reference); settlement.dispatch(reference);
        assertEquals("SETTLED", slice(reference).get("state"));
        return reference;
    }

    void publishOriginalReceipt(String reference) throws Exception {
        ObjectNode response = (ObjectNode) f.json.readTree(String.valueOf(slice(reference).get("request_json")));
        response.put("status", "SUCCEEDED").put("charge_id", "201").put("ledger_entry_id", "301")
            .put("charged_quota", response.path("charge_quota").asText());
        gateway.charges.put(reference, response);
    }

    @Test void changedConversionNeverClaimsOrSendsFirstHold() {
        ComputeFundingIntentService.Intent hold = hold(); config.put("conversion_version", "changed");
        recovery().advance(hold.id, System.currentTimeMillis());
        ComputeFundingIntentService.Intent after = funding.recover(hold.id);
        assertEquals("PREPARED", after.state); assertEquals(hold.key, after.key); assertEquals(hold.body, after.body);
        assertEquals(0, gateway.holds); assertEquals(1, gateway.configs);
    }

    @Test void unknownHoldRestartOnlyQueriesEvenAfterPermitDisabled() {
        ComputeFundingIntentService.Intent hold = hold(); recovery().advance(hold.id, System.currentTimeMillis());
        assertEquals("UNKNOWN", funding.recover(hold.id).state); assertEquals(1, gateway.holds);
        properties.getTrial().setEnabled(false); gateway.configFails = true; restart();
        recovery().advance(hold.id, System.currentTimeMillis());
        assertEquals(1, gateway.holds); assertEquals(1, gateway.configs);
        assertEquals(1, gateway.holdLookups); assertEquals(hold.key, funding.recover(hold.id).key);
    }

    @Test void sameHoldAndKeySurviveRestartAndTopupCannotReplenishTrial() {
        ComputeFundingIntentService.Intent hold = observedHold(); restart();
        assertEquals(hold.key, hold().key);
        assertThrows(IllegalStateException.class, () -> funding.prepareTopup(hold.id, "trial-topup", "1", "GPU_COMPUTE", System.currentTimeMillis()));
        assertEquals(1, f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent", Integer.class));
    }

    @Test void overCapRuntimeIsRecordedButDoesNotInvalidateEarlierAllocatedSlice() {
        observedHold(); String legal = interval(240000); String excess = interval(300000);
        assertEquals("BLOCKED", slice(excess).get("state"));
        assertEquals("TRIAL_UNALLOCATED_BUDGET", slice(excess).get("error_code"));
        assertEquals(f.start + 300000, f.jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session", Long.class));
        assertEquals(1, f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session", Integer.class));
        settlement.dispatch(legal); settlement.dispatch(legal);
        assertEquals("SETTLED", slice(legal).get("state")); assertEquals(1, gateway.captures.size());
        settlement.dispatch(excess); assertEquals(1, gateway.captures.size());
    }

    @Test void unknownCaptureOccupiesGrossBudgetAcrossRestart() {
        observedHold(); String original = interval(240000); gateway.loseCapture = true;
        settlement.dispatch(original); String key = String.valueOf(slice(original).get("idempotency_key"));
        assertEquals("UNKNOWN", slice(original).get("state")); restart();
        String excess = interval(300000);
        assertEquals("TRIAL_UNALLOCATED_BUDGET", slice(excess).get("error_code"));
        settlement.dispatch(original); assertEquals("SETTLED", slice(original).get("state"));
        assertEquals(key, slice(original).get("idempotency_key")); assertEquals(1, gateway.captures.size());
    }

    @Test void changedPermitStillAllowsOriginalCaptureLookupButNeverAnotherPost() {
        observedHold(); String reference = interval(60000); settlement.dispatch(reference);
        properties.getTrial().setEnabled(false); gateway.configFails = true; restart();
        settlement.dispatch(reference);
        assertEquals("SETTLED", slice(reference).get("state")); assertEquals(1, gateway.captures.size());
        assertEquals(1, gateway.configs);
    }

    @Test void expiredPermitTailRetainsMeteringAndNeverSentProofWithoutBlockingOriginalRelease() {
        ComputeFundingIntentService.Intent hold = observedHold();
        properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());
        String tail = interval(60000);
        assertEquals("BLOCKED", slice(tail).get("state"));
        assertEquals("TRIAL_UNALLOCATED_PERMIT", slice(tail).get("error_code"));
        assertNull(slice(tail).get("charge_id")); assertNull(slice(tail).get("ledger_entry_id"));
        assertNull(slice(tail).get("lease_token")); assertEquals(0L, ((Number) slice(tail).get("lease_until_ms")).longValue());
        assertEquals("TRIAL_PERMIT_EXPIRED", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(1, f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session", Integer.class));
        assertEquals(f.start + 60000, f.jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session", Long.class));
        settlement.dispatch(tail); assertEquals(0, gateway.captures.size());
        // Product orchestration separately requires confirmed physical stop. This service test
        // proves that its original funding cleanup does not add a live-permit/config dependency.
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(hold.id, reserve);
        gateway.configFails = true;
        recovery().advance(release.id, System.currentTimeMillis());
        assertEquals(1, gateway.releases); assertEquals(0, gateway.configs);
        assertEquals("UNKNOWN", funding.recover(release.id).state);
    }

    @Test void firstPreparedCaptureWithExpiredHoldIsNeverSentAndCanEnterOriginalCleanup() {
        ComputeFundingIntentService.Intent hold = observedHold(); String reference = interval(60000);
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", System.currentTimeMillis() - 1, usage);
        String key = String.valueOf(slice(reference).get("idempotency_key"));
        settlement.dispatch(reference);
        assertEquals("BLOCKED", slice(reference).get("state"));
        assertEquals("TRIAL_UNALLOCATED_PERMIT", slice(reference).get("error_code"));
        assertEquals("HOLD_EXPIRED", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertNull(slice(reference).get("lease_token"));
        assertEquals(java.util.Collections.singletonList(key), gateway.keyLookups);
        assertEquals(0, gateway.captures.size()); assertEquals(0, gateway.configs);
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(hold.id, reserve);
        recovery().advance(release.id, System.currentTimeMillis());
        assertEquals(1, gateway.releases);
    }

    @Test void alreadyUnknownCaptureWithExpiredHoldKeepsOriginalLookupUntilReceiptArrives() throws Exception {
        observedHold(); String reference = interval(60000);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN',error_code='SUBMISSION_UNCERTAIN' WHERE merchant_charge_id=?", reference);
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", System.currentTimeMillis() - 1, usage);
        String key = String.valueOf(slice(reference).get("idempotency_key"));
        settlement.dispatch(reference);
        assertEquals("UNKNOWN", slice(reference).get("state"));
        assertEquals("SUBMISSION_UNCERTAIN", slice(reference).get("error_code"));
        assertEquals(key, slice(reference).get("idempotency_key"));
        assertEquals(java.util.Collections.singletonList(key), gateway.keyLookups);
        assertEquals(0, gateway.captures.size());
        publishOriginalReceipt(reference); settlement.dispatch(reference);
        assertEquals("SETTLED", slice(reference).get("state"));
        assertEquals(key, slice(reference).get("idempotency_key")); assertEquals(0, gateway.captures.size());
    }

    @Test void rejectedOriginalOperationIsNotNeverSentEvenIfLocalStateSaysPrepared() throws Exception {
        observedHold(); String reference = interval(60000);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "CONCURRENT_BALANCE", null);
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", System.currentTimeMillis() - 1, usage);
        settlement.dispatch(reference);
        assertEquals("UNKNOWN", slice(reference).get("state"));
        assertEquals("HOLD_EXPIRED", slice(reference).get("error_code"));
        assertEquals(0, gateway.captures.size());
        publishOriginalReceipt(reference); settlement.dispatch(reference);
        assertEquals("SETTLED", slice(reference).get("state")); assertEquals(0, gateway.captures.size());
    }

    @Test void insufficientHeldLookupWithExpiredHoldStillReconcilesOriginalReceipt() throws Exception {
        ComputeFundingIntentService.Intent hold = observedHold(); String reference = interval(60000);
        long expiredAt = System.currentTimeMillis() - 1;
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "INSUFFICIENT_HELD_QUOTA", null);
        ObjectNode detail = (ObjectNode) fundingFixture.detail(hold, 2, reserve, "0", "0", reserve);
        detail.put("expires_at", Long.toString(expiredAt)).put("hold_status", "EXPIRED"); gateway.holdDetail = detail;
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", expiredAt, usage);
        settlement.dispatch(reference);
        assertEquals("UNKNOWN", slice(reference).get("state")); assertEquals("HOLD_EXPIRED", slice(reference).get("error_code"));
        assertEquals(0, gateway.captures.size()); assertEquals(0, gateway.configs);
        publishOriginalReceipt(reference); settlement.dispatch(reference);
        assertEquals("SETTLED", slice(reference).get("state")); assertEquals(0, gateway.captures.size());
    }

    @Test void rejectedOriginalOperationAlsoPreventsConfigFailureFromClaimingNeverSent() {
        observedHold(); String reference = interval(60000);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "CONCURRENT_BALANCE", null);
        gateway.configFails = true; settlement.dispatch(reference);
        assertEquals("UNKNOWN", slice(reference).get("state"));
        assertEquals("TRIAL_CONFIG_UNAVAILABLE", slice(reference).get("error_code"));
        assertEquals(0, gateway.captures.size());
    }

    @Test void expiredHoldAtPreparationRetainsUnsentTailProofAndOriginalExpiryRisk() {
        observedHold();
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", System.currentTimeMillis() - 1, usage);
        String reference = interval(60000);
        assertEquals("BLOCKED", slice(reference).get("state"));
        assertEquals("TRIAL_UNALLOCATED_PERMIT", slice(reference).get("error_code"));
        assertEquals("HOLD_EXPIRED", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(1, f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session", Integer.class));
        assertEquals(f.start + 60000, f.jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session", Long.class));
        assertEquals("108333", slice(reference).get("amount_text"));
        assertNull(slice(reference).get("charge_id")); assertNull(slice(reference).get("ledger_entry_id"));
        assertNull(slice(reference).get("lease_token"));
        settlement.dispatch(reference);
        assertTrue(gateway.keyLookups.isEmpty()); assertEquals(0, gateway.captures.size());
    }

    @Test void preparedRefundBlocksLaterCaptureEvenIfHeldQuotaReturns() {
        String original = settledCharge(); String later = interval(120000);
        ComputeRefundIntentService.Intent refund = refunds.prepare(original, "correct-one", "1", "OVERCHARGED");
        assertEquals("PREPARED", refund.state);
        // A current hold remaining counter is deliberately not a spend budget.
        f.jdbc.update("UPDATE vls_compute_funding_hold_observation SET remaining_quota=?", reserve);
        settlement.dispatch(later);
        assertEquals("TRIAL_UNALLOCATED_PERMIT", slice(later).get("error_code"));
        assertEquals("TRIAL_REFUND_PHASE_NO_NEW_CAPTURE", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(1, gateway.captures.size());
    }

    @Test void firstCaptureConfigFailureIsNeverSentAndDoesNotBlockEarlierRefundOrRelease() {
        String original = settledCharge(); String later = interval(120000);
        gateway.configFails = true;
        settlement.dispatch(later);
        assertEquals("BLOCKED", slice(later).get("state"));
        assertEquals("TRIAL_UNALLOCATED_PERMIT", slice(later).get("error_code"));
        assertNull(slice(later).get("lease_token"));
        assertEquals("TRIAL_CONFIG_UNAVAILABLE", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(1, gateway.captures.size());
        ComputeRefundIntentService.Intent refund = refunds.prepare(original, "original-config-correction", "1", "OVERCHARGED");
        refunds.dispatch(refund.id);
        String holdId = f.jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run", String.class);
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(holdId, reserve);
        recovery().advance(release.id, System.currentTimeMillis());
        assertEquals(1, gateway.refunds); assertEquals(1, gateway.releases);
        assertEquals(1, gateway.captures.size());
    }

    @Test void configFailureNeverDowngradesAnAlreadyUnknownCaptureToNeverSent() {
        observedHold(); String reference = interval(60000);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN',error_code='SUBMISSION_UNCERTAIN' WHERE merchant_charge_id=?", reference);
        String key = String.valueOf(slice(reference).get("idempotency_key"));
        gateway.configFails = true; settlement.dispatch(reference);
        assertEquals("UNKNOWN", slice(reference).get("state"));
        assertEquals("SUBMISSION_UNCERTAIN", slice(reference).get("error_code"));
        assertEquals("TRIAL_CONFIG_UNAVAILABLE", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(key, slice(reference).get("idempotency_key"));
        assertEquals(java.util.Collections.singletonList(key), gateway.keyLookups);
        assertEquals(0, gateway.captures.size());
    }

    @Test void unresolvedCaptureMustBeQueriedBeforeBeginningRefundPhase() {
        String original = settledCharge(); String later = interval(120000);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN' WHERE merchant_charge_id=?", later);
        assertThrows(IllegalStateException.class, () -> refunds.prepare(original, "correct-one", "1", "OVERCHARGED"));
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_refund_intent", Integer.class));
    }

    @Test void activeCaptureClaimAndRefundPreparationAreMutuallyExclusive() throws Exception {
        String original = settledCharge(); String later = interval(120000);
        gateway.entered = new CountDownLatch(1); gateway.resume = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> sending = pool.submit(() -> settlement.dispatch(later));
            assertTrue(gateway.entered.await(5, TimeUnit.SECONDS));
            assertEquals("SUBMITTED", slice(later).get("state"));
            assertThrows(IllegalStateException.class, () -> refunds.prepare(original, "concurrent-correction", "1", "OVERCHARGED"));
            gateway.resume.countDown(); sending.get(5, TimeUnit.SECONDS);
            assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_refund_intent", Integer.class));
        } finally { gateway.resume.countDown(); pool.shutdownNow(); }
    }

    @Test void refundAndFinalReleaseIgnoreExpiredTrialConversionButKeepGlobalWriteGate() {
        String original = settledCharge();
        ComputeRefundIntentService.Intent refund = refunds.prepare(original, "original-correction", "1", "OVERCHARGED");
        String holdId = f.jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run", String.class);
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(holdId, reserve);
        int configCalls = gateway.configs;
        properties.getTrial().setEnabled(false); properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());
        gateway.configFails = true; restart();
        refunds.dispatch(refund.id); recovery().advance(release.id, System.currentTimeMillis());
        assertEquals(1, gateway.refunds); assertEquals(1, gateway.releases); assertEquals(configCalls, gateway.configs);
        assertEquals("UNKNOWN", refunds.recover(refund.id).state);
        properties.setAllowWrites(false);
        refunds.dispatch(refund.id); recovery().advance(release.id, System.currentTimeMillis());
        assertEquals(1, gateway.refunds); assertEquals(1, gateway.releases);
    }

    @Test void anotherSelectedRunCannotPrepareReleaseOrRefund() {
        String original = settledCharge();
        String holdId = f.jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run", String.class);
        properties.getTrial().setRunId(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> funding.recover(holdId));
        assertThrows(IllegalStateException.class, () -> funding.prepareFinalRelease(holdId, reserve));
        assertThrows(IllegalStateException.class, () -> refunds.prepare(original, "wrong-run-correction", "1", "OVERCHARGED"));
        assertEquals(0, gateway.releases); assertEquals(0, gateway.refunds);
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_refund_intent", Integer.class));
    }

    @Test void preparedCleanupCannotSendAfterRunSelectionChangesButReadRecoverySurvives() {
        String original = settledCharge();
        String holdId = f.jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run", String.class);
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(holdId, reserve);
        ComputeRefundIntentService.Intent refund = refunds.prepare(original, "prepared-cleanup", "1", "OVERCHARGED");
        properties.getTrial().setRunId(UUID.randomUUID().toString());
        assertEquals("PREPARED", funding.recover(release.id).state);
        assertEquals("PREPARED", refunds.recover(refund.id).state);
        assertEquals("PREPARED", recovery().advance(release.id, System.currentTimeMillis()).state);
        assertThrows(IllegalStateException.class, () -> refunds.dispatch(refund.id));
        assertEquals("PREPARED", refunds.recover(refund.id).state);
        assertEquals(0, gateway.releases); assertEquals(0, gateway.refunds);
    }

    @Test void anotherPayerCannotPrepareCleanupEvenWithLocallyConsistentOriginalBodies() throws Exception {
        String original = settledCharge();
        String holdId = f.jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run", String.class);
        ComputeFundingIntentService.Intent hold = funding.recover(holdId);
        ObjectNode holdBody = (ObjectNode) f.json.readTree(hold.body); holdBody.put("user_id", 14);
        ObjectNode chargeBody = (ObjectNode) f.json.readTree(String.valueOf(slice(original).get("request_json"))); chargeBody.put("user_id", 14);
        f.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='14' WHERE usage_id=?", usage);
        f.jdbc.update("UPDATE vls_compute_funding_intent SET payer_user_id='14',request_json=?,request_hash=? WHERE intent_id=?",
            holdBody.toString(), ComputeBillingSettlementService.hash(holdBody.toString()), holdId);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET request_json=?,request_hash=? WHERE merchant_charge_id=?",
            chargeBody.toString(), ComputeBillingSettlementService.hash(chargeBody.toString()), original);
        assertEquals("14", funding.recover(holdId).payerUserId);
        assertEquals("TRIAL_PERMIT_CHANGED", assertThrows(IllegalStateException.class,
            () -> funding.prepareFinalRelease(holdId, reserve)).getMessage());
        assertEquals("TRIAL_PERMIT_CHANGED", assertThrows(IllegalStateException.class,
            () -> refunds.prepare(original, "wrong-payer-correction", "1", "OVERCHARGED")).getMessage());
        assertEquals(0, gateway.releases); assertEquals(0, gateway.refunds);
    }

    @Test void successfulOriginalReleaseLookupDoesNotPermitReplayForAnotherSelectedRun() {
        ComputeFundingIntentService.Intent hold = observedHold();
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(hold.id, reserve);
        funding.recordUnknown(release.id, release.requestHash);
        long queryStarted = System.currentTimeMillis();
        funding.recordHoldDetail(hold.id, hold.requestHash, fundingFixture.detail(hold, 1, reserve, "0", "0", reserve), queryStarted);
        ObjectNode lookup = f.json.createObjectNode().put("status", "SUCCEEDED").put("idempotency_key", release.key)
            .put("user_id", hold.payerUserId).put("hold_id", hold.holdId);
        properties.getTrial().setRunId(UUID.randomUUID().toString());
        assertThrows(IllegalStateException.class, () -> funding.prepareOriginalReplay(release.id, System.currentTimeMillis(), queryStarted, lookup));
        assertEquals("UNKNOWN", funding.recover(release.id).state);
        assertEquals(0, gateway.releases);
    }

    @Test void disabledBillingHasNoExternalCalls() {
        ComputeFundingIntentService.Intent hold = hold(); properties.setEnabled(false);
        recovery().advance(hold.id, System.currentTimeMillis());
        assertEquals(0, gateway.configs); assertEquals(0, gateway.holds); assertEquals(0, gateway.holdLookups);
    }

    class LocalGateway implements ExternalBillingGateway {
        int configs, holds, holdLookups, releases, refunds;
        boolean configFails, loseCapture;
        CountDownLatch entered, resume;
        List<String> captures = new ArrayList<>();
        List<String> keyLookups = new ArrayList<>();
        Map<String, JsonNode> charges = new LinkedHashMap<>();
        Result operation = Result.of(Status.NOT_FOUND);
        JsonNode holdDetail;
        @Override public JsonNode config() { configs++; if (configFails) throw new IllegalStateException("TEST_ONLY_CONFIG_UNAVAILABLE"); return config.deepCopy(); }
        @Override public Result byKey(String key) { keyLookups.add(key); return operation; }
        @Override public Result byReference(String reference) { return charges.containsKey(reference) ? new Result(Status.SUCCEEDED, "", charges.get(reference)) : Result.of(Status.NOT_FOUND); }
        @Override public Result balance(String userId) { return Result.of(Status.UNKNOWN); }
        @Override public boolean supportsAtomicCapture() { return true; }
        @Override public Result createHold(String key, String body) { holds++; return Result.of(Status.UNKNOWN); }
        @Override public Result holdOperation(String operation, String reference) { holdLookups++; return Result.of(Status.UNKNOWN); }
        @Override public Result holdById(String holdId) { return holdDetail == null ? Result.of(Status.UNKNOWN) : new Result(Status.SUCCEEDED, "", holdDetail); }
        @Override public Result releaseHold(String holdId, String key, String body) { releases++; return Result.of(Status.UNKNOWN); }
        @Override public Result refund(String chargeId, String key, String body) { refunds++; return Result.of(Status.UNKNOWN); }
        @Override public Result capture(String key, String body) {
            captures.add(key);
            if (entered != null) {
                entered.countDown();
                try { if (!resume.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("TEST_ONLY_LATCH_TIMEOUT"); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
            }
            try {
                ObjectNode response = (ObjectNode) f.json.readTree(body);
                response.put("status", "SUCCEEDED").put("charge_id", Integer.toString(200 + captures.size()))
                    .put("ledger_entry_id", Integer.toString(300 + captures.size())).put("charged_quota", response.path("charge_quota").asText());
                charges.put(response.path("merchant_charge_id").asText(), response);
                if (loseCapture) throw new IllegalStateException("TEST_ONLY_RESPONSE_LOST");
                return new Result(Status.SUCCEEDED, "", response);
            } catch (java.io.IOException invalid) { throw new IllegalStateException(invalid); }
        }
    }
}
