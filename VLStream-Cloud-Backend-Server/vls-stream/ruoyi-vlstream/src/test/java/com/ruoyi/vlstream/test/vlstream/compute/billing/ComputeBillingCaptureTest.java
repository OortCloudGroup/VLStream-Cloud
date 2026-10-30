package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePowerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Isolated local intent/state tests; the fake is not evidence of an upstream wallet transaction. */
@Tag("dev")
class ComputeBillingCaptureTest {
    private ComputeBillingSettlementTest f;
    private HoldGateway gateway;
    private long expires;

    @BeforeEach void setup() throws Exception {
        f = new ComputeBillingSettlementTest();
        f.database();
        Path migration = migration();
        try (java.sql.Connection connection = f.jdbc.getDataSource().getConnection()) {
            for (String name : new String[]{"V1_2_0_049__compute_billing_coordination.sql",
                "V1_2_0_052__compute_funding_intents.sql", "V1_2_0_053__compute_funding_hold_recovery.sql"})
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(migration.resolve(name)));
        }
        gateway = new HoldGateway(f);
        f.gateway = gateway;
        expires = System.currentTimeMillis() + 600000;
    }

    private Path migration() {
        String configured = System.getProperty("billing.migration");
        if (configured != null) return Paths.get(configured).toAbsolutePath().getParent();
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !java.nio.file.Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Set billing.migration to the actual Flyway source");
        return root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration");
    }

    private String usage() throws Exception {
        String usage = f.usage("tenant-test", "1", "500000", "1");
        bind(usage);
        return usage;
    }

    private void bind(String usage) {
        // This fixture starts after the funding journal's separately tested authoritative binding.
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_required=1,funding_hold_intent_id='verified-hold-fixture',funding_hold_id='9007199254740993',funding_expires_at_ms=? WHERE usage_id=?", expires, usage);
    }

    @Test void bindingOnlyAffectsNewIntervalsAndNeverRewritesHistoricalRequest() throws Exception {
        String usage = f.usage("tenant-test", "1", "500000", "1");
        String prior = f.interval(usage, 60000);
        Map<String, Object> original = f.row(prior);
        bind(usage);
        String next = f.interval(usage, 120000);
        assertEquals(original, f.row(prior));
        JsonNode request = f.json.readTree(String.valueOf(f.row(next).get("request_json")));
        assertTrue(request.path("hold_id").isIntegralNumber());
        assertEquals("9007199254740993", request.path("hold_id").asText());
        assertFalse(f.json.readTree(String.valueOf(original.get("request_json"))).has("hold_id"));
        assertEquals(next, f.service(f.jdbc).prepareInterval(usage, f.start + 120000));
    }

    @Test void heldInsufficiencyWaitsForSameHoldTopupThenReusesExactKeyAndBody() throws Exception {
        String ref = f.interval(usage(), 60000);
        Map<String, Object> original = f.row(ref);
        gateway.rejectHeldOnce = true;
        f.service.dispatch(ref);
        assertEquals("WAITING_FUNDS", f.state(ref));
        gateway.remaining = "1";
        f.service(f.jdbc).dispatch(ref);
        assertEquals(1, gateway.captures.size());
        gateway.remaining = "1000000";
        f.service(f.jdbc).dispatch(ref);
        f.service.dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals(2, gateway.captures.size());
        assertEquals(gateway.captures.get(0), gateway.captures.get(1));
        assertEquals(original.get("request_hash"), f.row(ref).get("request_hash"));
        assertEquals(original.get("request_json"), f.row(ref).get("request_json"));
        assertEquals(0, gateway.balanceQueries);
        assertEquals(expires, f.jdbc.queryForObject("SELECT funding_expires_at_ms FROM vls_compute_billing_session", Long.class));
        f.assertStopRequired();
    }

    @Test void concurrentBalanceRejectionRetriesOnlyAfterOriginalLookupsWithSameKeyAndBody() throws Exception {
        String ref = f.interval(usage(), 60000);
        Map<String, Object> original = f.row(ref);
        gateway.rejectConcurrentOnce = true;
        f.service.dispatch(ref);
        assertEquals("UNKNOWN", f.state(ref));
        assertEquals("CONCURRENT_BALANCE", f.row(ref).get("error_code"));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(1, gateway.captures.size());
        f.service(f.jdbc).dispatch(ref);
        f.service.dispatch(ref);
        f.service.dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals(Arrays.asList("reference", "key", "capture", "reference", "key", "capture", "reference"), gateway.events);
        assertEquals(2, gateway.captures.size());
        assertEquals(gateway.captures.get(0), gateway.captures.get(1));
        assertEquals(1, gateway.charges.size());
        assertEquals(original.get("idempotency_key"), f.row(ref).get("idempotency_key"));
        assertEquals(original.get("request_json"), f.row(ref).get("request_json"));
        assertEquals(original.get("request_hash"), f.row(ref).get("request_hash"));
        assertEquals(0, gateway.balanceQueries);
    }

    @Test void queriedConcurrentRejectionAfterRestartAllowsOnlyOneOriginalCapture() throws Exception {
        String ref = f.interval(usage(), 60000);
        Map<String, Object> original = f.row(ref);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='SUBMITTED',lease_token='lost-worker',lease_until_ms=1 WHERE merchant_charge_id=?", ref);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "CONCURRENT_BALANCE", null);
        f.service(f.jdbc).dispatch(ref);
        f.service.dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals(Arrays.asList("reference", "key", "capture", "reference"), gateway.events);
        assertEquals(1, gateway.captures.size());
        assertEquals(Arrays.asList(original.get("idempotency_key"), original.get("request_json")), gateway.captures.get(0));
    }

    @Test void concurrentRejectionCannotAuthorizeRetryWhileEitherLookupIsUnknown() throws Exception {
        String ref = f.interval(usage(), 60000);
        gateway.rejectConcurrentOnce = true;
        f.service.dispatch(ref);
        gateway.referenceUnknown = true;
        f.service(f.jdbc).dispatch(ref);
        assertEquals("UNKNOWN", f.state(ref));
        assertEquals("LOOKUP_UNAVAILABLE", f.row(ref).get("error_code"));
        gateway.referenceUnknown = false;
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN);
        f.service.dispatch(ref);
        assertEquals("LOOKUP_UNRESOLVED", f.row(ref).get("error_code"));
        assertEquals(1, gateway.captures.size());
        assertEquals(0, gateway.charges.size());
    }

    @Test void concurrentRejectionStillHonorsTheOriginalHoldExpiry() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "CONCURRENT_BALANCE", null);
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", System.currentTimeMillis() - 1, usage);
        f.service.dispatch(ref);
        assertEquals("BLOCKED", f.state(ref));
        assertEquals("HOLD_EXPIRED", f.row(ref).get("error_code"));
        assertEquals(0, gateway.captures.size());
    }

    @Test void lostRetryResponseRecoversChargeOnceAndReleasesOnlyItsTailBlock() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        gateway.rejectConcurrentOnce = true;
        f.service.dispatch(ref);
        gateway.loseResponse = true;
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SUBMISSION_UNCERTAIN", f.row(ref).get("error_code"));
        f.assertStopRequired();
        f.service.recordRunningObservation(usage, Instant.ofEpochMilli(f.start + 90000));
        f.service.confirmPhysicalStop(usage, Instant.ofEpochMilli(f.start + 95000));
        String tail = f.service.prepareInterval(usage, f.start + 90000);
        Map<String, Object> originalTail = f.row(tail);
        assertEquals("BLOCKED", f.state(tail));
        assertEquals("SUBMISSION_UNCERTAIN", originalTail.get("error_code"));
        gateway.loseResponse = false;
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.assertStopRequired();
        assertEquals("PREPARED", f.state(tail));
        assertEquals(originalTail.get("request_json"), f.row(tail).get("request_json"));
        assertEquals(originalTail.get("idempotency_key"), f.row(tail).get("idempotency_key"));
        assertEquals(originalTail.get("request_hash"), f.row(tail).get("request_hash"));
        assertEquals(originalTail.get("amount_text"), f.row(tail).get("amount_text"));
        f.service.dispatch(tail);
        f.service(f.jdbc).dispatch(tail);
        assertEquals("SETTLED", f.state(tail));
        assertEquals(3, gateway.captures.size()); // one rejected attempt, one original debit, one tail debit
        assertEquals(2, gateway.charges.size());
        assertEquals(1, ((Number) f.row(tail).get("is_tail")).intValue());
    }

    @Test void recoveredChargeCannotClearUnrelatedRiskOrHistoricalUnlabelledBlock() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        gateway.loseResponse = true;
        f.service.dispatch(ref);
        String blocked = f.interval(usage, 90000);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET error_code=NULL WHERE merchant_charge_id=?", blocked);
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals("SUBMISSION_UNCERTAIN", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals("BLOCKED", f.state(blocked));
        f.assertStopRequired();
    }

    @Test void recoveredChargeNeverClearsAuthorizationRisk() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        gateway.loseResponse = true;
        f.service.dispatch(ref);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='NOT_AUTHORIZED' WHERE usage_id=?", usage);
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals("NOT_AUTHORIZED", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.assertStopRequired();
        assertEquals(1, gateway.captures.size());
    }

    @Test void oneRecoveredChargeCannotClearAnotherUncertainOriginalSlice() throws Exception {
        String usage = usage();
        String first = f.interval(usage, 60000), second = f.interval(usage, 120000);
        gateway.loseResponse = true;
        f.service.dispatch(first);
        f.service.dispatch(second);
        assertEquals("UNKNOWN", f.state(first)); assertEquals("UNKNOWN", f.state(second));
        f.service(f.jdbc).dispatch(first);
        assertEquals("SETTLED", f.state(first));
        assertEquals("SUBMISSION_UNCERTAIN", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.service(f.jdbc).dispatch(second);
        assertEquals("SETTLED", f.state(second));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.assertStopRequired();
        assertEquals(2, gateway.captures.size());
    }

    @Test void confirmedFundingChildrenDoNotLeaveRecoveredCapturePermanentlyUncertain() throws Exception {
        for (String operation : new String[]{"TOPUP", "RELEASE"}) {
            String usage = usage(), ref = f.interval(usage, 60000);
            gateway.loseResponse = true;
            f.service.dispatch(ref);
            fundingStateFixture(usage, operation, "OPERATION_CONFIRMED", "{\"hold_id\":\"9007199254740993\"}");
            f.service(f.jdbc).dispatch(ref);
            assertEquals("SETTLED", f.state(ref));
            assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
            assertEquals(1, f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?", Integer.class, usage));
        }
        assertEquals(2, gateway.captures.size());
    }

    @Test void unconfirmedOrWrongKindFundingEvidenceCannotReleaseSubmissionRisk() throws Exception {
        for (String[] state : new String[][]{{"TOPUP", "RECEIPT_UNVERIFIED", "{}"}, {"RELEASE", "UNKNOWN", null},
            {"TOPUP", "CONFLICT", "{}"}, {"TOPUP", "DETAIL_OBSERVED", "{}"},
            {"HOLD", "OPERATION_CONFIRMED", "{}"}, {"RELEASE", "OPERATION_CONFIRMED", null}}) {
            String usage = usage(), ref = f.interval(usage, 60000);
            gateway.loseResponse = true;
            f.service.dispatch(ref);
            fundingStateFixture(usage, state[0], state[1], state[2]);
            f.service(f.jdbc).dispatch(ref);
            f.service.resumeResolvedSubmissionRisk(usage);
            assertEquals("SETTLED", f.state(ref));
            assertEquals("SUBMISSION_UNCERTAIN", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage));
            assertEquals(1, f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?", Integer.class, usage));
        }
        assertEquals(6, gateway.captures.size());
    }

    @Test void lateFundingConfirmationResumesOnlyTheVerifiedOriginalTailWithoutNetworkInTheHelper() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        gateway.loseResponse = true;
        f.service.dispatch(ref);
        fundingStateFixture(usage, "TOPUP", "RECEIPT_UNVERIFIED", "{\"hold_id\":\"9007199254740993\"}");
        f.service.recordRunningObservation(usage, Instant.ofEpochMilli(f.start + 90000));
        f.service.confirmPhysicalStop(usage, Instant.ofEpochMilli(f.start + 95000));
        String tail = f.service.prepareInterval(usage, f.start + 90000);
        Map<String, Object> originalTail = f.row(tail);
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals("BLOCKED", f.state(tail));
        assertEquals("SUBMISSION_UNCERTAIN", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.jdbc.update("UPDATE vls_compute_funding_intent SET state='OPERATION_CONFIRMED' WHERE usage_id=?", usage);
        int queries = gateway.queries, captures = gateway.captures.size();
        f.service(f.jdbc).resumeResolvedSubmissionRisk(usage);
        f.service.resumeResolvedSubmissionRisk(usage);
        assertEquals(queries, gateway.queries); assertEquals(captures, gateway.captures.size());
        assertEquals("PREPARED", f.state(tail));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.assertStopRequired();
        for (String field : new String[]{"request_json", "request_hash", "idempotency_key", "amount_text"})
            assertEquals(originalTail.get(field), f.row(tail).get(field));
        gateway.loseResponse = false;
        f.service.dispatch(tail);
        f.service(f.jdbc).dispatch(tail);
        assertEquals("SETTLED", f.state(tail));
        assertEquals(2, gateway.captures.size());
    }

    @Test void localResumeCannotTreatOnlyBlockedSlicesAsProofOfSuccessfulCapture() throws Exception {
        String usage = usage();
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='SUBMISSION_UNCERTAIN',stop_requested=1 WHERE usage_id=?", usage);
        String ref = f.interval(usage, 60000);
        f.service.resumeResolvedSubmissionRisk(usage);
        assertEquals("BLOCKED", f.state(ref));
        assertEquals("SUBMISSION_UNCERTAIN", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(0, gateway.queries); assertEquals(0, gateway.captures.size());
    }

    private void fundingStateFixture(String usage, String operation, String state, String receipt) {
        // The funding service separately proves its terminal state; this checks only the settlement gate.
        String id = java.util.UUID.randomUUID().toString();
        f.jdbc.update("INSERT INTO vls_compute_funding_intent(intent_id,usage_id,operation_kind,business_reference,merchant_id,payer_user_id,parent_intent_id,idempotency_key,request_path,request_json,request_hash,state,receipt_json,contract_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id, usage, operation, "test-funding:" + id, "vls-test", "123", "verified-hold-fixture", "test-key:" + id,
            "/api/v1/external-billing/holds/9007199254740993", "{}", ComputeBillingSettlementService.hash("{}"), state, receipt, "HOLDS_V3");
    }

    @Test void anotherUserHoldCannotAuthorizeAnInsufficientCaptureRetry() throws Exception {
        String ref = f.interval(usage(), 60000);
        gateway.rejectHeldOnce = true;
        f.service.dispatch(ref);
        gateway.wrongHoldUser = true;
        f.service.dispatch(ref);
        assertEquals("BLOCKED", f.state(ref));
        assertEquals("HOLD_MISMATCH", f.row(ref).get("error_code"));
        assertEquals(1, gateway.captures.size());
    }

    @Test void expiredHoldBlocksNewCaptureWithoutReleasingOrChangingItsSnapshot() throws Exception {
        expires = System.currentTimeMillis() - 1;
        String ref = f.interval(usage(), 60000);
        Map<String, Object> original = f.row(ref);
        f.service.dispatch(ref);
        assertEquals("BLOCKED", f.state(ref));
        assertEquals("HOLD_EXPIRED", f.row(ref).get("error_code"));
        assertEquals(0, gateway.captures.size());
        assertEquals(original.get("request_json"), f.row(ref).get("request_json"));
        f.assertStopRequired();
    }

    @Test void successfulDetailWithoutHoldEvidenceWaitsForOriginalKeyWithoutRecapturing() throws Exception {
        String ref = f.interval(usage(), 60000);
        f.service.dispatch(ref);
        ObjectNode proof = splitCommittedReceipt(ref);
        for (ExternalBillingGateway.Status status : new ExternalBillingGateway.Status[]{ExternalBillingGateway.Status.UNKNOWN,
            ExternalBillingGateway.Status.NOT_FOUND, ExternalBillingGateway.Status.PROCESSING, ExternalBillingGateway.Status.REJECTED}) {
            gateway.operation = ExternalBillingGateway.Result.of(status);
            f.service(f.jdbc).dispatch(ref);
            assertEquals("SUBMISSION_UNCERTAIN", f.row(ref).get("error_code"));
            assertEquals("UNKNOWN", f.state(ref));
            assertEquals(1, gateway.captures.size());
        }
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", proof);
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.assertStopRequired();
        assertEquals(1, gateway.captures.size());
    }

    @Test void complementaryDeployedReceiptsProveTheOriginalCaptureWithoutInventingFields() throws Exception {
        String ref = f.interval(usage(), 60000);
        Map<String, Object> original = f.row(ref);
        f.service.dispatch(ref);
        ObjectNode proof = splitCommittedReceipt(ref);
        assertFalse(gateway.charges.get(ref).has("hold_id"));
        assertFalse(gateway.charges.get(ref).has("merchant_id"));
        assertFalse(proof.has("operation_id"));
        assertFalse(proof.has("metering_ref"));
        assertEquals("9007199254740993", proof.path("hold_id").asText());
        f.service(f.jdbc).dispatch(ref);
        f.service.dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals(Arrays.asList("reference", "key", "capture", "reference", "key"), gateway.events);
        assertEquals(1, gateway.captures.size());
        assertEquals("charge-test", f.row(ref).get("charge_id"));
        assertEquals("ledger-test", f.row(ref).get("ledger_entry_id"));
        for (String field : new String[]{"request_json", "request_hash", "idempotency_key", "amount_text"})
            assertEquals(original.get(field), f.row(ref).get(field));
    }

    @Test void contradictoryOriginalOperationCannotProveAReferenceCharge() throws Exception {
        for (String field : new String[]{"idempotency_key", "operation_type", "merchant_id", "user_id", "merchant_charge_id",
            "usage_session_id", "hold_id", "charge_id", "ledger_entry_id", "charged_quota"}) {
            gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_FOUND);
            String ref = f.interval(usage(), 60000);
            f.service.dispatch(ref);
            ObjectNode proof = splitCommittedReceipt(ref);
            proof.put(field, "different-original-evidence");
            int captures = gateway.captures.size();
            f.service(f.jdbc).dispatch(ref);
            f.service.dispatch(ref);
            assertEquals("BLOCKED", f.state(ref), field);
            assertEquals("BUSINESS_REFERENCE_CONFLICT", f.row(ref).get("error_code"), field);
            assertEquals(captures, gateway.captures.size(), field);
        }
    }

    @Test void unknownJointProofCannotReplaceOrClearAnExistingFundingConflict() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        f.service.dispatch(ref);
        ObjectNode proof = splitCommittedReceipt(ref);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='FUNDING_OPERATION_CONFLICT',stop_requested=1 WHERE usage_id=?", usage);
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN);
        f.service(f.jdbc).dispatch(ref);
        assertEquals("UNKNOWN", f.state(ref));
        assertEquals("FUNDING_OPERATION_CONFLICT", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", proof);
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals("FUNDING_OPERATION_CONFLICT", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals(1, gateway.captures.size());
        f.assertStopRequired();
    }

    @Test void missingHoldOrNumericOperationAmountCannotCompleteTheJointProof() throws Exception {
        for (String field : new String[]{"hold_id", "charged_quota"}) {
            gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_FOUND);
            String ref = f.interval(usage(), 60000);
            f.service.dispatch(ref);
            ObjectNode proof = splitCommittedReceipt(ref);
            if ("hold_id".equals(field)) proof.remove(field);
            else proof.put(field, proof.path(field).asLong());
            int captures = gateway.captures.size();
            f.service(f.jdbc).dispatch(ref);
            assertEquals("BLOCKED", f.state(ref));
            assertEquals(captures, gateway.captures.size());
        }
    }

    @Test void originalOperationCannotOverrideConflictingReferenceMeteringOrPrice() throws Exception {
        for (String field : new String[]{"metering_ref", "price_snapshot_id", "conversion_version", "charged_quota"}) {
            gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_FOUND);
            String ref = f.interval(usage(), 60000);
            f.service.dispatch(ref);
            splitCommittedReceipt(ref);
            ((ObjectNode) gateway.charges.get(ref)).put(field, "different-reference-evidence");
            int queries = gateway.queries, captures = gateway.captures.size();
            f.service(f.jdbc).dispatch(ref);
            assertEquals("BLOCKED", f.state(ref));
            assertEquals(queries + 1, gateway.queries); // A contradictory detail cannot be repaired by another receipt.
            assertEquals(captures, gateway.captures.size());
        }
    }

    @Test void joinedRecoveryAfterLostCaptureResponseUnblocksOnlyItsPersistedTail() throws Exception {
        String usage = usage(), ref = f.interval(usage, 60000);
        gateway.loseResponse = true;
        f.service.dispatch(ref);
        splitCommittedReceipt(ref);
        f.service.recordRunningObservation(usage, Instant.ofEpochMilli(f.start + 90000));
        f.service.confirmPhysicalStop(usage, Instant.ofEpochMilli(f.start + 95000));
        String tail = f.service.prepareInterval(usage, f.start + 90000);
        Map<String, Object> originalTail = f.row(tail);
        assertEquals("BLOCKED", f.state(tail));
        f.service(f.jdbc).dispatch(ref);
        assertEquals("SETTLED", f.state(ref));
        assertEquals("PREPARED", f.state(tail));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        f.assertStopRequired();
        gateway.loseResponse = false;
        gateway.operation = ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_FOUND);
        f.service.dispatch(tail);
        splitCommittedReceipt(tail);
        f.service(f.jdbc).dispatch(tail);
        assertEquals("SETTLED", f.state(tail));
        for (String field : new String[]{"request_json", "request_hash", "idempotency_key", "amount_text"})
            assertEquals(originalTail.get(field), f.row(tail).get(field));
        assertEquals(2, gateway.captures.size());
        assertEquals(2, gateway.charges.size());
    }

    /** Wire-field split observed in the saved, net-zero DEV run; no live request or credentials. */
    private ObjectNode splitCommittedReceipt(String ref) throws Exception {
        JsonNode complete = gateway.charges.get(ref);
        JsonNode request = f.json.readTree(String.valueOf(f.row(ref).get("request_json")));
        ObjectNode reference = f.json.createObjectNode();
        for (String field : new String[]{"status", "charge_id", "ledger_entry_id", "user_id", "merchant_charge_id",
            "usage_session_id", "metering_ref", "price_snapshot_id", "conversion_version", "charged_quota"})
            reference.set(field, complete.get(field));
        reference.put("refunded_quota", "0");
        ObjectNode operation = f.json.createObjectNode();
        operation.put("status", "SUCCEEDED");
        operation.put("operation_type", "CHARGE");
        operation.put("idempotency_key", String.valueOf(f.row(ref).get("idempotency_key")));
        for (String field : new String[]{"merchant_id", "user_id", "merchant_charge_id", "usage_session_id", "hold_id"})
            operation.set(field, request.get(field));
        for (String field : new String[]{"charge_id", "ledger_entry_id", "charged_quota"}) operation.set(field, reference.get(field));
        operation.put("refunded_quota", "0");
        gateway.charges.put(ref, reference);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", operation);
        return operation;
    }

    @Test void ordinaryBalanceCannotReplaceInsufficientFrozenQuota() throws Exception {
        String ref = f.interval(usage(), 60000);
        gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, "INSUFFICIENT_AVAILABLE_QUOTA", null);
        f.service.dispatch(ref);
        assertEquals("BLOCKED", f.state(ref));
        assertEquals(0, gateway.balanceQueries);
        assertEquals(0, gateway.captures.size());
    }

    @Test void aLostConflictingBindingNeverFallsBackToOrdinaryDebit() throws Exception {
        String usage = usage();
        String pending = f.interval(usage, 60000);
        Map<String, Object> original = f.row(pending);
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_hold_intent_id=NULL,funding_hold_id=NULL,funding_expires_at_ms=NULL,risk_code='HOLD_DETAIL_CONFLICT',stop_requested=1 WHERE usage_id=?", usage);
        f.service.dispatch(pending);
        assertEquals("HOLD_MISMATCH", f.row(pending).get("error_code"));
        assertEquals(original.get("request_json"), f.row(pending).get("request_json"));
        String tail = f.interval(usage, 90000);
        assertEquals("BLOCKED", f.state(tail));
        f.service.dispatch(tail);
        assertEquals(0, gateway.captures.size());
        assertEquals(f.start + 90000, f.jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session", Long.class));
    }

    @Test void anOlderUnboundPendingIntervalCannotBeSubmittedAfterFundingIsRequired() throws Exception {
        String usage = f.usage("tenant-test", "1", "500000", "1");
        String ref = f.interval(usage, 60000);
        Map<String, Object> original = f.row(ref);
        bind(usage);
        f.service.dispatch(ref);
        assertEquals("HISTORICAL_FUNDING_UNBOUND", f.row(ref).get("error_code"));
        assertEquals(original.get("request_json"), f.row(ref).get("request_json"));
        assertEquals(0, gateway.captures.size());
    }

    @Test void childFundingConflictBlocksPreparedCaptureButAllowsSuccessfulHistoryQuery() throws Exception {
        String pendingUsage = usage();
        String pending = f.interval(pendingUsage, 60000);
        Map<String, Object> original = f.row(pending);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='FUNDING_OPERATION_CONFLICT',stop_requested=1 WHERE usage_id=?", pendingUsage);
        f.service.dispatch(pending);
        assertEquals("BLOCKED", f.state(pending));
        assertEquals("FUNDING_OPERATION_CONFLICT", f.row(pending).get("error_code"));
        assertEquals(0, gateway.captures.size());
        assertEquals(original.get("request_json"), f.row(pending).get("request_json"));
        assertEquals(original.get("request_hash"), f.row(pending).get("request_hash"));
        assertEquals(original.get("idempotency_key"), f.row(pending).get("idempotency_key"));

        String committedUsage = usage();
        String committed = f.interval(committedUsage, 60000);
        f.service.dispatch(committed);
        assertEquals("UNKNOWN", f.state(committed));
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='FUNDING_OPERATION_CONFLICT',stop_requested=1 WHERE usage_id=?", committedUsage);
        f.service(f.jdbc).dispatch(committed);
        assertEquals("SETTLED", f.state(committed));
        assertEquals(1, gateway.captures.size());
        assertEquals("FUNDING_OPERATION_CONFLICT", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?", String.class, committedUsage));
    }

    @Test void explicitHoldAndHistoricalConversionErrorsKeepOriginalSnapshot() throws Exception {
        for (String error : new String[]{"HOLD_MISMATCH", "HOLD_EXPIRED", "INVALID_REQUEST"}) {
            String ref = f.interval(usage(), 60000);
            Map<String, Object> original = f.row(ref);
            gateway.operation = new ExternalBillingGateway.Result(ExternalBillingGateway.Status.REJECTED, error, null);
            f.service.dispatch(ref);
            assertEquals("BLOCKED", f.state(ref));
            assertEquals(error, f.row(ref).get("error_code"));
            assertEquals(original.get("request_json"), f.row(ref).get("request_json"));
            assertEquals(original.get("idempotency_key"), f.row(ref).get("idempotency_key"));
            assertEquals(original.get("request_hash"), f.row(ref).get("request_hash"));
        }
        assertEquals(0, gateway.captures.size());
    }

    @Test void holdExpiryRequestsStopBeforeAnySliceButStillMetersUntilObservedStop() throws Exception {
        expires = System.currentTimeMillis() + 30000;
        String usage = usage();
        final long[] at = {5000};
        final String[] power = {"RUNNING"};
        final int[] stopRequests = {0};
        BillingLifecycleGateway lifecycle = new BillingLifecycleGateway() {
            @Override public Observation observe(String id) {
                return new Observation(new ComputePowerProvider.Snapshot(power[0], "UNKNOWN", "TEST_ONLY_PROVIDER"), Instant.ofEpochMilli(f.start + at[0]));
            }
            @Override public void requestSafeStop(String id) { stopRequests[0]++; }
        };
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("lifecycle", lifecycle);
        beans.registerSingleton("gateway", gateway);
        ComputeBillingReconciliationService reconciliation = new ComputeBillingReconciliationService(f.jdbc, f.manager, f.properties, beans.getBeanProvider(ExternalBillingGateway.class), f.json);
        ComputeBillingCoordinator coordinator = new ComputeBillingCoordinator(f.jdbc, f.service, f.properties, beans.getBeanProvider(BillingLifecycleGateway.class), reconciliation);
        f.properties.setSafetyStopEnabled(true);
        coordinator.tick(usage, lifecycle);
        assertEquals(1, stopRequests[0]);
        assertEquals(0, f.service.audit(usage).size());
        assertNull(f.jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?", java.sql.Timestamp.class, usage));
        at[0] = 35000;
        coordinator.tick(usage, lifecycle);
        power[0] = "STOPPED";
        at[0] = 45000;
        coordinator.tick(usage, lifecycle);
        assertEquals(f.start + 35000, f.jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?", java.sql.Timestamp.class, usage).getTime());
        assertEquals(1, f.service.audit(usage).size());
        assertEquals(f.start + 35000, ((Number) f.service.audit(usage).get(0).get("interval_end_ms")).longValue());
        assertEquals(1, gateway.captures.size());
        String tail = String.valueOf(f.service.audit(usage).get(0).get("merchant_charge_id"));
        f.service.dispatch(tail);
        assertEquals("SETTLED", f.state(tail));
    }

    @Test void expiryWarningCannotReplaceAnExistingFundingConflict() throws Exception {
        expires = System.currentTimeMillis() + 30000;
        String usage = usage();
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='HOLD_DETAIL_CONFLICT',stop_requested=1 WHERE usage_id=?", usage);
        BillingLifecycleGateway lifecycle = new BillingLifecycleGateway() {
            @Override public Observation observe(String id) {
                return new Observation(new ComputePowerProvider.Snapshot("RUNNING", "UNKNOWN", "TEST_ONLY_PROVIDER"), Instant.ofEpochMilli(f.start + 60000));
            }
            @Override public void requestSafeStop(String id) { }
        };
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("lifecycle", lifecycle);
        beans.registerSingleton("gateway", gateway);
        ComputeBillingReconciliationService reconciliation = new ComputeBillingReconciliationService(f.jdbc, f.manager, f.properties, beans.getBeanProvider(ExternalBillingGateway.class), f.json);
        new ComputeBillingCoordinator(f.jdbc, f.service, f.properties, beans.getBeanProvider(BillingLifecycleGateway.class), reconciliation).tick(usage, lifecycle);
        assertEquals("HOLD_DETAIL_CONFLICT", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session", String.class));
        assertEquals("BLOCKED", f.service.audit(usage).get(0).get("state"));
        assertEquals(0, gateway.captures.size());
    }

    @Test void captureLedgerMatchesOnlyCaptureRequests() throws Exception {
        String ref = f.interval(usage(), 60000);
        f.service.dispatch(ref);
        f.service.dispatch(ref);
        reconcile(ref, "CAPTURE", "-16666");
        assertEquals("MATCHED", f.jdbc.queryForObject("SELECT match_state FROM vls_compute_billing_ledger_seen", String.class));
    }

    @Test void ordinaryDebitLedgerCannotProveAnAtomicCapture() throws Exception {
        String ref = f.interval(usage(), 60000);
        f.service.dispatch(ref);
        f.service.dispatch(ref);
        reconcile(ref, "CHARGE", "-16666");
        assertEquals("CHARGE_TYPE_MISMATCH", f.jdbc.queryForObject("SELECT match_state FROM vls_compute_billing_ledger_seen", String.class));
    }

    @Test void captureRefundIsReviewedWithoutAssumingAvailableFundsOrReleasingAgain() throws Exception {
        String ref = f.interval(usage(), 60000);
        reconcile(ref, "CAPTURE_REFUND", "16666");
        assertEquals("CAPTURE_REFUND_REVIEW", f.jdbc.queryForObject("SELECT match_state FROM vls_compute_billing_ledger_seen", String.class));
        assertEquals(0, gateway.captures.size());
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent", Integer.class));
    }

    @Test void captureLedgerWithPositiveDeltaDoesNotAdvanceCursor() throws Exception {
        String ref = f.interval(usage(), 60000);
        reconcile(ref, "CAPTURE", "16666");
        assertEquals("CONFLICT", f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_reconciliation", String.class));
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_ledger_seen", Integer.class));
    }

    private void reconcile(String ref, String type, String delta) throws Exception {
        ObjectNode entry = f.json.createObjectNode();
        entry.put("ledger_entry_id", "ledger-test");
        entry.put("merchant_ref", ref);
        entry.put("entry_type", type);
        entry.put("quota_delta", delta);
        entry.put("user_id", 123);
        entry.put("reference_id", "charge-test");
        entry.put("balance_before", "1000000");
        entry.put("balance_after", "983334");
        entry.put("created_at", "2026-01-01T00:01:00Z");
        ObjectNode page = f.json.createObjectNode();
        page.putArray("entries").add(entry);
        page.put("has_more", false);
        page.put("next_cursor", "");
        gateway.page = page;
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("gateway", gateway);
        new ComputeBillingReconciliationService(f.jdbc, f.manager, f.properties, beans.getBeanProvider(ExternalBillingGateway.class), f.json).tick();
    }

    private class HoldGateway extends ComputeBillingSettlementTest.FakeGateway {
        boolean rejectHeldOnce, rejectConcurrentOnce, wrongHoldUser;
        final java.util.List<String> events = new java.util.ArrayList<>();
        String remaining = "1000000";
        int balanceQueries;
        JsonNode page;
        HoldGateway(ComputeBillingSettlementTest fixture) { fixture.super(); }
        @Override public Result byReference(String ref) { events.add("reference"); return super.byReference(ref); }
        @Override public Result byKey(String key) { events.add("key"); return super.byKey(key); }
        @Override public Result capture(String key, String body) {
            events.add("capture");
            if (rejectConcurrentOnce) {
                rejectConcurrentOnce = false;
                captures.add(Arrays.asList(key, body));
                operation = new Result(Status.REJECTED, "CONCURRENT_BALANCE", null);
                return operation;
            }
            if (rejectHeldOnce) {
                rejectHeldOnce = false;
                captures.add(Arrays.asList(key, body));
                operation = new Result(Status.REJECTED, "INSUFFICIENT_HELD_QUOTA", null);
                return operation;
            }
            return super.capture(key, body);
        }
        @Override public Result balance(String user) { balanceQueries++; return super.balance(user); }
        @Override public Result holdById(String id) {
            ObjectNode detail = f.json.createObjectNode();
            String usage = f.jdbc.queryForObject("SELECT usage_id FROM vls_compute_billing_session", String.class);
            detail.put("hold_id", id);
            detail.put("merchant_hold_id", "vls-hold:" + usage + ":initial");
            detail.put("user_id", wrongHoldUser ? 124 : 123);
            detail.put("hold_quota", remaining);
            detail.put("captured_quota", "0");
            detail.put("released_quota", "0");
            detail.put("remaining_quota", remaining);
            // The published GET example uses an exact JSON integer for the millisecond expiry.
            detail.put("expires_at", expires);
            detail.put("hold_status", "ACTIVE");
            detail.put("balance_version", 1);
            detail.put("created_at", 1767225600L);
            detail.put("updated_at", 1767225600L);
            return new Result(Status.SUCCEEDED, "", detail);
        }
        @Override public Result ledger(long from, long to, String cursor) { return page == null ? Result.of(Status.UNKNOWN) : new Result(Status.SUCCEEDED, "", page); }
    }
}
