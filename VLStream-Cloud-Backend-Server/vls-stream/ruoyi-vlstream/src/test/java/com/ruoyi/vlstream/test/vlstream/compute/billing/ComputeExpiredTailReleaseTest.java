package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLedgerService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLifecycleService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingPolicy;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeNodeService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePaidRunService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePowerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 and read-only fake evidence only. No external wallet, supplier or device is contacted. */
@Tag("dev")
class ComputeExpiredTailReleaseTest {
    private ComputeBillingSettlementTest f;
    private ReadOnlyGateway gateway;
    private ComputeFundingIntentService funding;
    private ComputeFundingIntentService.Intent hold;
    private String usage, run, tail;
    private long expiry, stoppedAt;
    private String captured, remainderUnit;

    @BeforeEach void database() throws Exception {
        f = new ComputeBillingSettlementTest();
        f.database();
        f.jdbc.execute("ALTER TABLE vls_compute_usage ADD COLUMN active_server_key VARCHAR(200)");
        f.jdbc.execute("ALTER TABLE vls_compute_usage ADD COLUMN start_operation_id VARCHAR(36)");
        f.jdbc.execute("ALTER TABLE vls_compute_usage ADD COLUMN start_observed_at TIMESTAMP(3)");
        f.jdbc.execute("ALTER TABLE vls_compute_usage ADD COLUMN stop_observed_at TIMESTAMP(3)");
        f.jdbc.execute("ALTER TABLE vls_compute_usage ADD COLUMN start_evidence VARCHAR(30)");
        f.jdbc.execute("ALTER TABLE vls_compute_usage ADD COLUMN stop_evidence VARCHAR(30)");
        Path migrations = migrationDirectory();
        try (java.sql.Connection connection = f.jdbc.getDataSource().getConnection()) {
            for (String name : new String[]{"V1_2_0_049__compute_billing_coordination.sql",
                "V1_2_0_052__compute_funding_intents.sql", "V1_2_0_053__compute_funding_hold_recovery.sql",
                "V1_2_0_054__compute_corrective_refund_intents.sql", "V1_2_0_056__compute_paid_run_orchestration.sql",
                "V1_2_0_057__configured_official_power_outbox.sql"})
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(migrations.resolve(name)));
        }
        f.jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36) PRIMARY KEY,node_id BIGINT,public_instance_key VARCHAR(64),tenant_id VARCHAR(64),job_state VARCHAR(32))");
        gateway = new ReadOnlyGateway();
        f.gateway = gateway;
        funding = new ComputeFundingIntentService(f.jdbc, f.manager, f.json);

        long now = System.currentTimeMillis();
        long startedAt = now - 600000L;
        expiry = now - 90000L;
        stoppedAt = startedAt + 270000L;
        usage = f.usage("tenant-test", "1", "500000", "1");
        String startOperation = UUID.randomUUID().toString();
        f.jdbc.update("UPDATE vls_compute_usage SET server_key='node-server:-6000',billing_start_at=?,start_observed_at=?,start_evidence='OBSERVATION',active_server_key='node-server:-6000',start_operation_id=? WHERE id=?",
            new Timestamp(startedAt), new Timestamp(startedAt), startOperation, usage);
        f.jdbc.update("UPDATE vls_compute_billing_session SET instance_ref='node-server:-6000',metered_until_ms=?,last_running_ms=?,control_snapshot='fixture-original-control' WHERE usage_id=?",
            startedAt, startedAt, usage);
        hold = funding.prepareInitialHold(usage, "1000000", Long.toString(expiry), "GPU_COMPUTE");
        hold = funding.recordHoldDetail(hold.id, hold.requestHash, detail("0", "ACTIVE", 1));
        // Earlier slices model the eight original successful captures. Preparing them uses
        // the real immutable-body/remainder code, without executing a capture or refund.
        f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?", now + 600000L, usage);
        BigInteger total = BigInteger.ZERO;
        for (int sequence = 0; sequence < 8; sequence++) {
            long end = startedAt + (sequence + 1) * 30000L;
            f.service.recordRunningObservation(usage, Instant.ofEpochMilli(end));
            String reference = f.service.prepareInterval(usage, end);
            total = total.add(ExactQuota.integer(String.valueOf(f.row(reference).get("amount_text")), true));
            f.jdbc.update("UPDATE vls_compute_billing_slice SET state='SETTLED',error_code=NULL,charge_id=?,ledger_entry_id=?,create_time=? WHERE merchant_charge_id=?",
                Integer.toString(101 + sequence), Integer.toString(201 + sequence), new Timestamp(end), reference);
        }
        captured = total.toString();
        gateway.detail = detail(captured, "EXPIRED", 9);
        hold = funding.recordHoldDetail(hold.id, hold.requestHash, gateway.detail);
        f.jdbc.update("UPDATE vls_compute_usage SET state='STOPPED',billing_end_at=?,stop_observed_at=?,stop_evidence='OBSERVATION' WHERE id=?",
            new Timestamp(stoppedAt), new Timestamp(stoppedAt), usage);
        f.jdbc.update("UPDATE vls_compute_billing_session SET safety_state='STOP_CONFIRMED',physical_stop_observed_ms=?,last_running_ms=?,stop_requested=1 WHERE usage_id=?",
            stoppedAt, stoppedAt, usage);
        tail = f.service.prepareInterval(usage, stoppedAt);
        assertEquals("BLOCKED", f.state(tail));
        assertEquals("HOLD_EXPIRED", f.row(tail).get("error_code"));

        run = UUID.randomUUID().toString();
        long acceptedAt = startedAt - 10000L;
        String accepted = f.jdbc.queryForObject("SELECT accepted_snapshot FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage);
        f.jdbc.update("INSERT INTO vls_compute_paid_run(run_id,node_id,instance_key,active_instance_key,tenant_id,opening_user_id,platform_subject,payer_user_id,merchant_id,quote_snapshot,reserve_quota,quote_until_ms,hold_until_ms,maximum_run_seconds,usage_id,hold_intent_id,start_operation_id,state,power_start_state,power_stop_state,create_time) VALUES(?,-6000,'fixture-original-instance','fixture-original-instance','tenant-test','actor-test','platform-test','123','vls-test',?,'1000000',?,?,600,?,?,?,'STOPPING','ACCEPTED','ACCEPTED',?)",
            run, accepted, acceptedAt + 120000L, expiry, usage, hold.id, startOperation, new Timestamp(acceptedAt));
        f.jdbc.update("UPDATE vls_compute_funding_intent SET create_time=? WHERE intent_id=?", new Timestamp(acceptedAt), hold.id);
        remainderUnit = "origin:" + ComputeBillingSettlementService.hash(BillingLedgerOrigin.namespace(f.properties) + "\nquota");
    }

    @Test void confirmsOnlyExpiredNeverDebitedTailPreservesEightCapturesAndRollsBackItsFraction() throws Exception {
        Map<String, Object> original = new LinkedHashMap<>(f.row(tail));
        List<Map<String, Object>> captures = settled();
        List<Map<String, Object>> fundingBefore = f.jdbc.queryForList("SELECT * FROM vls_compute_funding_intent ORDER BY intent_id");
        String before = priorRemainder(original).toString();
        assertTrue(f.service.confirmExpiredUnsentTail(usage));
        Map<String, Object> after = new LinkedHashMap<>(f.row(tail));
        assertEquals(ComputeBillingSettlementService.EXPIRED_UNSENT_TAIL, after.get("error_code"));
        after.put("error_code", original.get("error_code"));
        assertEquals(original, after);
        assertEquals(captures, settled());
        assertEquals(fundingBefore, f.jdbc.queryForList("SELECT * FROM vls_compute_funding_intent ORDER BY intent_id"));
        assertEquals(before, currentRemainder().toString());
        assertEquals("BLOCKED", f.state(tail));
        assertEquals(0, gateway.writes);
        assertEquals(8, captures.size());
        assertTrue(gateway.references > 0); assertTrue(gateway.keys > 0); assertTrue(gateway.holds > 0);
    }

    @Test void repeatedConfirmationPreservesOriginalKeyBodyAuditAndDoesNotRollbackTwice() {
        assertTrue(f.service.confirmExpiredUnsentTail(usage));
        Map<String, Object> once = new LinkedHashMap<>(f.row(tail));
        String remainder = currentRemainder().toString();
        assertTrue(f.service.confirmExpiredUnsentTail(usage));
        assertEquals(once, f.row(tail));
        assertEquals(remainder, currentRemainder().toString());
        assertEquals(0, gateway.writes);
    }

    @Test void normalTicksConfirmTailReleaseOriginalHoldAndPreserveEightCaptures() {
        List<Map<String, Object>> original = settled(); int[] releases = {0};
        ExternalBillingGateway cleanup = mock(ExternalBillingGateway.class, call -> {
            if (java.util.Arrays.asList("capture", "refund", "createHold", "topupHold").contains(call.getMethod().getName())) throw new AssertionError("Only the original final release is allowed");
            return RETURNS_DEFAULTS.answer(call);
        });
        when(cleanup.holdByReference(anyString())).thenAnswer(call -> new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", gateway.detail.deepCopy()));
        when(cleanup.holdById(anyString())).thenAnswer(call -> new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", gateway.detail.deepCopy()));
        when(cleanup.holdOperation(eq("RELEASE"), anyString())).thenAnswer(call -> new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", f.json.createObjectNode().put("status", "SUCCEEDED").put("hold_id", "101").put("user_id", "123").put("idempotency_key", funding.recover(f.jdbc.queryForObject("SELECT release_intent_id FROM vls_compute_paid_run WHERE run_id=?", String.class, run)).key)));
        when(cleanup.releaseHold(anyString(), anyString(), anyString())).thenAnswer(call -> {
            assertEquals("101", call.getArgument(0)); assertEquals(0, releases[0]++); String amount = gateway.detail.path("remaining_quota").asText();
            JsonNode body = f.json.readTree((String) call.getArgument(2)); assertEquals("vls-release:" + usage + ":final", body.path("merchant_release_id").asText());
            gateway.detail.put("released_quota", amount).put("remaining_quota", "0").put("hold_status", "RELEASED").put("balance_version", "10");
            ObjectNode receipt = f.json.createObjectNode().put("hold_id", "101").put("held_quota", "0").put("released_quota", amount).put("remaining_quota", "0").put("hold_status", "RELEASED");
            return new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED, "", receipt);
        });
        ComputePaidRunService paid = new ComputePaidRunService(f.jdbc, f.manager, f.json, mock(ComputeNodeService.class), mock(ComputeBillingLedgerService.class), f.service, mock(ComputeBillingLifecycleService.class), funding, new ComputeFundingRecoveryService(funding, () -> cleanup, f.properties), f.properties, new ComputeBillingPolicy(), new DefaultListableBeanFactory().getBeanProvider(ComputePowerProvider.class));
        for (int attempt = 0; attempt < 3; attempt++) paid.tick(run);
        assertEquals("COMPLETED", f.jdbc.queryForObject("SELECT state FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
        assertNull(f.jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
        assertEquals(original, settled()); assertEquals(captured, gateway.detail.path("captured_quota").asText()); assertEquals(1, releases[0]); assertEquals(0, gateway.writes);
        assertEquals(ComputeBillingSettlementService.EXPIRED_UNSENT_TAIL, f.row(tail).get("error_code")); assertEquals("BLOCKED", f.state(tail));
    }

    @Test void runWindowReleaseApprovalDoesNotWaiveOrRollBackBeforeTheBarrier() {
        ReleaseBarrierGateway barrier = runWindowTail();
        Map<String, Object> original = new LinkedHashMap<>(f.row(tail)); String fraction = currentRemainder().toString();
        assertTrue(f.service.allowExpiredRunWindowTailRelease(usage));
        assertEquals(original, f.row(tail)); assertEquals(fraction, currentRemainder().toString());
        assertEquals(0, barrier.releaseEffects); assertEquals(0, barrier.writes);
    }

    @Test void stoppedSshFailureRearmsOnlyTheOriginalUnattemptedTailUnderFreshActiveHold() throws Exception {
        sshTail(true);
        Map<String,Object> original=new LinkedHashMap<>(f.row(tail));String remainder=currentRemainder().toString();
        assertTrue(f.service.recoverStoppedSshPreparationTail(usage));
        Map<String,Object> prepared=new LinkedHashMap<>(f.row(tail));assertEquals("PREPARED",prepared.get("state"));
        prepared.put("state",original.get("state"));assertEquals(original,prepared);
        assertEquals("SSH_PREPARATION_FAILED",f.row(tail).get("error_code"));
        assertEquals(remainder,currentRemainder().toString());assertEquals(0,gateway.writes);
        assertFalse(f.service.recoverStoppedSshPreparationTail(usage));
        assertEquals("PREPARED",f.state(tail));assertEquals(remainder,currentRemainder().toString());
    }

    @Test void freshlySealedStoppedSshFailureTailUsesNormalSettlementInsteadOfFinancialBlock() throws Exception {
        sshTail(true);Map<String,Object> original=f.row(tail);ExactQuota before=priorRemainder(original);
        f.jdbc.update("DELETE FROM vls_compute_billing_slice WHERE merchant_charge_id=?",tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET metered_until_ms=?,next_sequence=? WHERE usage_id=?",original.get("interval_start_ms"),original.get("slice_sequence"),usage);
        f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id='123' AND quota_unit=?",before.numerator.toString(),before.denominator.toString(),remainderUnit);
        tail=f.service.prepareInterval(usage,stoppedAt);
        assertEquals("PREPARED",f.state(tail));assertNull(f.row(tail).get("error_code"));assertEquals(original.get("amount_text"),f.row(tail).get("amount_text"));
        assertEquals(0,gateway.writes);
    }

    @Test void sshFailureWithoutRequestedSafeStopStillBlocksNewCapture() throws Exception {
        sshTail(true);Map<String,Object> original=f.row(tail);ExactQuota before=priorRemainder(original);
        f.jdbc.update("DELETE FROM vls_compute_billing_slice WHERE merchant_charge_id=?",tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET safety_state='STOP_PENDING',stop_requested=0,metered_until_ms=?,next_sequence=? WHERE usage_id=?",original.get("interval_start_ms"),original.get("slice_sequence"),usage);
        f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id='123' AND quota_unit=?",before.numerator.toString(),before.denominator.toString(),remainderUnit);
        tail=f.service.prepareInterval(usage,stoppedAt);assertEquals("BLOCKED",f.state(tail));assertEquals("SSH_PREPARATION_FAILED",f.row(tail).get("error_code"));
    }

    @Test void confirmedNonTailStillSettlesWhileArmedSshFailureRequestsShutdown() throws Exception {
        sshTail(true);resetTailForRunningMeter();tail=f.service.prepareInterval(usage,stoppedAt-1000L);
        assertEquals("PREPARED",f.state(tail));assertNull(f.row(tail).get("error_code"));
        assertEquals(0,((Number)f.row(tail).get("is_tail")).intValue());assertEquals(0,gateway.writes);
    }

    @Test void sshFailureWithoutOriginalArmedControlBlocksConfirmedNonTail() throws Exception {
        sshTail(true);resetTailForRunningMeter();f.jdbc.update("UPDATE vls_compute_billing_session SET control_snapshot=NULL WHERE usage_id=?",usage);
        tail=f.service.prepareInterval(usage,stoppedAt-1000L);assertEquals("BLOCKED",f.state(tail));assertEquals("SSH_PREPARATION_FAILED",f.row(tail).get("error_code"));
    }

    @Test void otherFinancialRiskCannotUseArmedSshStopOnlyRule() throws Exception {
        sshTail(true);resetTailForRunningMeter();f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='NOT_AUTHORIZED' WHERE usage_id=?",usage);
        tail=f.service.prepareInterval(usage,stoppedAt-1000L);assertEquals("BLOCKED",f.state(tail));assertEquals("NOT_AUTHORIZED",f.row(tail).get("error_code"));
    }

    @Test void sshRearmRejectsExpiredHoldWithoutChangingTheOriginal() throws Exception {
        sshTail(false);assertSshRearmDenied();
    }

    @Test void sshRearmRejectsUnknownOriginalKeyAndConflictingReference() throws Exception {
        sshTail(true);gateway.key=ExternalBillingGateway.Status.UNKNOWN;assertSshRearmDenied();
        gateway.key=ExternalBillingGateway.Status.NOT_FOUND;gateway.reference=ExternalBillingGateway.Status.CONFLICT;assertSshRearmDenied();
    }

    @Test void sshRearmRejectsAttemptLeaseAndDebitEvidence() throws Exception {
        sshTail(true);f.jdbc.update("UPDATE vls_compute_billing_slice SET attempt_count=1 WHERE merchant_charge_id=?",tail);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_billing_slice SET attempt_count=0,lease_token='another-worker',lease_until_ms=? WHERE merchant_charge_id=?",System.currentTimeMillis()+30000,tail);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_billing_slice SET lease_token=NULL,lease_until_ms=0,charge_id='existing-charge' WHERE merchant_charge_id=?",tail);assertSshRearmDenied();
    }

    @Test void sshRearmRejectsUnknownStateFinancialRiskForeignOriginAndOwnership() throws Exception {
        sshTail(true);f.jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN' WHERE merchant_charge_id=?",tail);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='BLOCKED' WHERE merchant_charge_id=?",tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='FUNDING_OPERATION_CONFLICT' WHERE usage_id=?",usage);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='SSH_PREPARATION_FAILED' WHERE usage_id=?",usage);
        f.jdbc.update("UPDATE vls_compute_paid_run SET tenant_id='other' WHERE run_id=?",run);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_paid_run SET tenant_id='tenant-test' WHERE run_id=?",run);
        f.properties.setBaseUrl("https://other-origin.fixture.invalid");assertSshRearmDenied();
    }

    @Test void sshRearmRejectsStaleHoldEvidenceAndCounterMismatch() throws Exception {
        sshTail(true);f.jdbc.update("UPDATE vls_compute_funding_hold_observation SET observed_at_ms=? WHERE hold_intent_id=?",System.currentTimeMillis()-31000,hold.id);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_funding_hold_observation SET observed_at_ms=? WHERE hold_intent_id=?",System.currentTimeMillis(),hold.id);
        gateway.detail.put("captured_quota",new BigInteger(captured).add(BigInteger.ONE).toString());assertSshRearmDenied();
    }

    @Test void sshRearmRejectsConcurrentOriginalChangeAfterReadOnlyEvidence() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(true);
        barrier.postHoldHook=()->f.jdbc.update("UPDATE vls_compute_billing_slice SET attempt_count=1 WHERE merchant_charge_id=?",tail);
        assertFalse(f.service.recoverStoppedSshPreparationTail(usage));assertEquals("BLOCKED",f.state(tail));assertEquals(1,((Number)f.row(tail).get("attempt_count")).intValue());assertEquals(0,barrier.writes);
    }

    @Test void sshRearmRejectsChangedOriginalRequestAndMissingPhysicalProof() throws Exception {
        sshTail(true);String body=String.valueOf(f.row(tail).get("request_json"));
        f.jdbc.update("UPDATE vls_compute_billing_slice SET request_json='{}' WHERE merchant_charge_id=?",tail);assertSshRearmDenied();
        f.jdbc.update("UPDATE vls_compute_billing_slice SET request_json=? WHERE merchant_charge_id=?",body,tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET safety_state='STOP_PENDING' WHERE usage_id=?",usage);assertSshRearmDenied();
    }

    @Test void sshRearmRejectsHoldExpiredDuringReadOnlyEvidence() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(true);
        barrier.postHoldHook=()->f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?",System.currentTimeMillis()-1,usage);
        assertFalse(f.service.recoverStoppedSshPreparationTail(usage));assertEquals("BLOCKED",f.state(tail));assertEquals(0,barrier.writes);
    }

    @Test void expiredSshTailOnlyWaivesAfterSuccessfulOriginalReleaseBarrier() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);
        Map<String,Object> original=new LinkedHashMap<>(f.row(tail));List<Map<String,Object>> captures=settled();String fraction=currentRemainder().toString();
        assertTrue(f.service.allowExpiredSshPreparationTailRelease(usage));assertEquals(original,f.row(tail));assertEquals(fraction,currentRemainder().toString());
        assertFalse(f.service.confirmReleasedSshPreparationTail(usage));assertEquals(original,f.row(tail));
        assertEquals("OPERATION_CONFIRMED",releaseOriginal(barrier).state);assertTrue(f.service.confirmReleasedSshPreparationTail(usage));
        Map<String,Object> marked=new LinkedHashMap<>(f.row(tail));assertEquals(ComputeBillingSettlementService.RELEASED_SSH_PREPARATION_TAIL,marked.get("error_code"));
        marked.put("error_code",original.get("error_code"));assertEquals(original,marked);assertEquals("BLOCKED",f.state(tail));assertEquals(captures,settled());
        assertEquals(priorRemainder(original).toString(),currentRemainder().toString());String after=currentRemainder().toString();
        assertTrue(f.service.confirmReleasedSshPreparationTail(usage));assertEquals(after,currentRemainder().toString());assertEquals(1,barrier.releaseEffects);assertEquals(0,barrier.writes);
    }

    @Test void expiredSshReleaseCannotHideUnknownOrPartialReleaseAndRefLookup() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);barrier.loseReleaseReply=true;
        assertEquals("UNKNOWN",releaseOriginal(barrier).state);assertSshWaiverDenied();
    }

    @Test void expiredSshReleaseRejectsTailFoundAfterBarrier() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);assertEquals("OPERATION_CONFIRMED",releaseOriginal(barrier).state);
        barrier.tailFound=true;assertSshWaiverDenied();
    }

    @Test void expiredSshReleaseRejectsPartialReleaseAndChangedFraction() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);barrier.partialRelease=true;releaseOriginal(barrier);assertSshWaiverDenied();
    }

    @Test void expiredSshReleaseRejectsFinancialRiskEvenWithZeroRemaining() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);assertEquals("OPERATION_CONFIRMED",releaseOriginal(barrier).state);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='NOT_AUTHORIZED' WHERE usage_id=?",usage);assertSshWaiverDenied();
    }

    @Test void expiredSshReleaseRejectsConcurrentFractionChangeAfterBarrier() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);assertEquals("OPERATION_CONFIRMED",releaseOriginal(barrier).state);
        barrier.postReleaseHoldHook=()->f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text='1',denominator_text='7' WHERE payer_user_id='123' AND quota_unit=?",remainderUnit);
        assertFalse(f.service.confirmReleasedSshPreparationTail(usage));assertEquals("BLOCKED",f.state(tail));
        assertEquals("SSH_PREPARATION_FAILED",f.row(tail).get("error_code"));assertEquals("1/7",currentRemainder().toString());assertEquals(0,barrier.writes);
    }

    @Test void anotherBlockedReasonCannotUseSshReleaseOrRearm() throws Exception {
        sshTail(false);f.jdbc.update("UPDATE vls_compute_billing_slice SET error_code='SUBMISSION_UNCERTAIN' WHERE merchant_charge_id=?",tail);
        Map<String,Object> original=new LinkedHashMap<>(f.row(tail));assertFalse(f.service.allowExpiredSshPreparationTailRelease(usage));
        assertFalse(f.service.confirmReleasedSshPreparationTail(usage));assertFalse(f.service.recoverStoppedSshPreparationTail(usage));
        assertEquals(original,f.row(tail));assertEquals(0,gateway.writes);
    }

    @Test void activeSshHoldCannotUseTheReleaseBarrierWaiver() throws Exception {
        sshTail(true);Map<String,Object> original=new LinkedHashMap<>(f.row(tail));assertFalse(f.service.allowExpiredSshPreparationTailRelease(usage));assertEquals(original,f.row(tail));assertEquals(0,gateway.writes);
    }

    @Test void normalSshTickRearmsValidHoldTailAndWaitsForTheOriginalDispatcher() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(true);Map<String,Object> original=new LinkedHashMap<>(f.row(tail));
        paidService(barrier).tick(run);
        Map<String,Object> prepared=new LinkedHashMap<>(f.row(tail));assertEquals("PREPARED",prepared.get("state"));
        prepared.put("state",original.get("state"));assertEquals(original,prepared);
        assertEquals("STOPPING",f.jdbc.queryForObject("SELECT state FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
        assertEquals(0,barrier.releasePosts);assertEquals(0,barrier.writes);
    }

    @Test void normalSshTicksReleaseExpiredOriginalOnceAndFinalizeWithDistinctWaiverAudit() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);List<Map<String,Object>> captures=settled();Map<String,Object> original=new LinkedHashMap<>(f.row(tail));
        ComputePaidRunService paid=paidService(barrier);for(int attempt=0;attempt<3;attempt++)paid.tick(run);
        assertEquals("COMPLETED",f.jdbc.queryForObject("SELECT state FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
        assertNull(f.jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
        Map<String,Object> marked=new LinkedHashMap<>(f.row(tail));assertEquals(ComputeBillingSettlementService.RELEASED_SSH_PREPARATION_TAIL,marked.get("error_code"));
        marked.put("error_code",original.get("error_code"));assertEquals(original,marked);assertEquals(captures,settled());
        assertEquals(1,barrier.releaseEffects);assertEquals(1,barrier.releasePosts);assertEquals(0,barrier.writes);assertEquals(captured,barrier.detail.path("captured_quota").asText());
    }

    @Test void normalSshTicksDoNotWaiveAnUnknownOriginalRelease() throws Exception {
        ReleaseBarrierGateway barrier=sshTail(false);barrier.releaseUnknownBeforeCommit=true;
        ComputePaidRunService paid=paidService(barrier);for(int attempt=0;attempt<3;attempt++)paid.tick(run);
        assertNotEquals("COMPLETED",f.jdbc.queryForObject("SELECT state FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
        assertNotNull(f.jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
        assertEquals("SSH_PREPARATION_FAILED",f.row(tail).get("error_code"));assertEquals("BLOCKED",f.state(tail));
        assertEquals(0,barrier.releaseEffects);assertEquals(0,barrier.writes);
    }

    @Test void committedOriginalReleaseBarrierAllowsWaiverWithoutClaimingNeverSent() throws Exception {
        ReleaseBarrierGateway barrier = runWindowTail();
        Map<String, Object> original = new LinkedHashMap<>(f.row(tail)); List<Map<String, Object>> captures = settled();
        assertTrue(f.service.allowExpiredRunWindowTailRelease(usage));
        assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        assertTrue(f.service.confirmReleasedRunWindowTail(usage));
        Map<String, Object> marked = new LinkedHashMap<>(f.row(tail));
        assertEquals(ComputeBillingSettlementService.RELEASED_RUN_WINDOW_TAIL, marked.get("error_code"));
        marked.put("error_code", original.get("error_code")); assertEquals(original, marked);
        assertEquals(priorRemainder(original).toString(), currentRemainder().toString()); assertEquals(captures, settled());
        assertEquals(captured, barrier.detail.path("captured_quota").asText()); assertEquals("0", barrier.detail.path("remaining_quota").asText());
        assertEquals(1, barrier.releaseEffects); assertEquals(0, barrier.writes);
        String fraction = currentRemainder().toString(); assertTrue(f.service.confirmReleasedRunWindowTail(usage));
        assertEquals(fraction, currentRemainder().toString()); assertEquals(1, barrier.releaseEffects);
    }

    @Test void releasedRunWindowTailNeedsTheOriginalReceiptEvenWhenHoldIsZero() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.loseReleaseReply = true;
        assertTrue(f.service.allowExpiredRunWindowTailRelease(usage));
        assertEquals("UNKNOWN", releaseOriginal(barrier).state);
        assertEquals("0", barrier.detail.path("remaining_quota").asText());
        assertRunWindowDenied(true); assertEquals(1, barrier.releaseEffects);
    }

    @Test void productionMinimalCaptureDetailsRequireEachOriginalKeyOperationProof() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.minimalCaptureReference = true;
        assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        assertTrue(f.service.confirmReleasedRunWindowTail(usage));
        assertEquals(8, barrier.captureKeyQueries); assertEquals(captured, barrier.detail.path("captured_quota").asText()); assertEquals(0, barrier.writes);
    }

    @Test void expiredTerminalStatusStillRequiresTheSuccessfulOriginalReleaseAndExactCounters() throws Exception {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.terminalExpired = true;
        List<Map<String, Object>> captures = settled(); Map<String, Object> original = new LinkedHashMap<>(f.row(tail));
        ComputeFundingIntentService.Intent release = releaseOriginal(barrier); assertEquals("OPERATION_CONFIRMED", release.state);
        assertEquals("EXPIRED", f.json.readTree(release.receipt).path("hold_status").asText());
        assertEquals("EXPIRED", barrier.detail.path("hold_status").asText()); assertEquals("0", barrier.detail.path("remaining_quota").asText());
        assertTrue(f.service.confirmReleasedRunWindowTail(usage));
        Map<String, Object> marked = new LinkedHashMap<>(f.row(tail)); assertEquals(ComputeBillingSettlementService.RELEASED_RUN_WINDOW_TAIL, marked.get("error_code"));
        marked.put("error_code", original.get("error_code")); assertEquals(original, marked); assertEquals(captures, settled());
        assertEquals(captured, barrier.detail.path("captured_quota").asText()); assertEquals(1, barrier.releaseEffects); assertEquals(0, barrier.writes);
    }

    @Test void normalTicksDoNotResendAConfirmedOriginalReleaseWithExpiredTerminalStatus() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.terminalExpired = true; List<Map<String, Object>> captures = settled();
        assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        ComputePaidRunService paid = new ComputePaidRunService(f.jdbc, f.manager, f.json, mock(ComputeNodeService.class), mock(ComputeBillingLedgerService.class), f.service, mock(ComputeBillingLifecycleService.class), funding, new ComputeFundingRecoveryService(funding, () -> barrier, f.properties), f.properties, new ComputeBillingPolicy(), new DefaultListableBeanFactory().getBeanProvider(ComputePowerProvider.class));
        for (int attempt = 0; attempt < 3; attempt++) paid.tick(run);
        assertEquals("COMPLETED", f.jdbc.queryForObject("SELECT state FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
        assertEquals(ComputeBillingSettlementService.RELEASED_RUN_WINDOW_TAIL, f.row(tail).get("error_code"));
        assertEquals(1, barrier.releasePosts); assertEquals(1, barrier.releaseEffects); assertEquals(captures, settled()); assertEquals(0, barrier.writes);
    }

    @Test void expiredZeroRemainingWithoutTheOriginalReleaseReceiptIsNotProof() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.terminalExpired = true; barrier.loseReleaseReply = true;
        assertEquals("UNKNOWN", releaseOriginal(barrier).state); assertEquals("EXPIRED", barrier.detail.path("hold_status").asText());
        assertEquals("0", barrier.detail.path("remaining_quota").asText()); assertRunWindowDenied(true);
    }

    @Test void expiredStatusCannotReplaceTheActualFullReleaseCounters() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.terminalExpired = true;
        assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        barrier.detail.put("released_quota", "0").put("remaining_quota", new BigInteger("1000000").subtract(new BigInteger(captured)).toString());
        assertRunWindowDenied(true);
    }

    @Test void expiredOriginalReceiptWithTheWrongReleasedAmountIsRejected() throws Exception {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.terminalExpired = true;
        ComputeFundingIntentService.Intent release = releaseOriginal(barrier); assertEquals("OPERATION_CONFIRMED", release.state);
        ObjectNode receipt = (ObjectNode) f.json.readTree(release.receipt);
        receipt.put("released_quota", new BigInteger(receipt.path("released_quota").asText()).subtract(BigInteger.ONE).toString());
        f.jdbc.update("UPDATE vls_compute_funding_intent SET receipt_json=? WHERE intent_id=?", receipt.toString(), release.id);
        assertRunWindowDenied(true);
    }

    @Test void unknownOriginalReleaseCannotWaiveOrReturnTheTailFraction() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.releaseUnknownBeforeCommit = true;
        assertTrue(f.service.allowExpiredRunWindowTailRelease(usage));
        assertEquals("UNKNOWN", releaseOriginal(barrier).state); assertRunWindowDenied(true);
        assertEquals(0, barrier.releaseEffects);
    }

    @Test void runWindowTailCannotBeWaivedBeforeAnyOriginalRelease() {
        runWindowTail(); assertRunWindowDenied(true);
    }

    @Test void captureRefundAfterTheBarrierCannotBeHiddenByTheHoldTotal() {
        ReleaseBarrierGateway barrier = runWindowTail(); assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        barrier.refundedCapture = true; assertRunWindowDenied(true);
    }

    @Test void changedOriginalCaptureAmountAfterTheBarrierCannotBeWaived() {
        ReleaseBarrierGateway barrier = runWindowTail(); assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        barrier.wrongCaptureAmount = true; assertRunWindowDenied(true);
    }

    @Test void changedHoldCaptureCounterAfterReleasePreventsTheWaiver() {
        ReleaseBarrierGateway barrier = runWindowTail(); assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        barrier.detail.put("captured_quota", new BigInteger(captured).add(BigInteger.ONE).toString());
        barrier.detail.put("released_quota", new BigInteger("1000000").subtract(new BigInteger(captured)).subtract(BigInteger.ONE).toString());
        assertRunWindowDenied(true);
    }

    @Test void aLateTailOperationFoundAfterTheBarrierIsNotReclassified() {
        ReleaseBarrierGateway barrier = runWindowTail(); assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        barrier.tailFound = true; assertRunWindowDenied(true);
    }

    @Test void partialOriginalReleaseIsNotACompletedBarrier() {
        ReleaseBarrierGateway barrier = runWindowTail(); barrier.partialRelease = true;
        releaseOriginal(barrier); assertEquals("1", barrier.detail.path("remaining_quota").asText()); assertRunWindowDenied(true);
    }

    @Test void sameAccountSuccessorDuringPostReleaseEvidencePreventsFractionRollback() {
        ReleaseBarrierGateway barrier = runWindowTail(); assertEquals("OPERATION_CONFIRMED", releaseOriginal(barrier).state);
        Map<String, Object> original = new LinkedHashMap<>(f.row(tail)); List<Map<String, Object>> captures = settled(); String fraction = currentRemainder().toString();
        barrier.postReleaseHoldHook = () -> {
            try {
                String successor = f.usage("other-tenant", "1", "500000", "1"); String reference = f.interval(successor, 1000);
                Timestamp created = f.jdbc.queryForObject("SELECT create_time FROM vls_compute_billing_slice WHERE merchant_charge_id=?", Timestamp.class, tail);
                f.jdbc.update("UPDATE vls_compute_billing_slice SET create_time=? WHERE merchant_charge_id=?", created, reference);
                f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id='123' AND quota_unit=?", original.get("remainder_numerator"), original.get("remainder_denominator"), remainderUnit);
            } catch (Exception invalid) { throw new AssertionError(invalid); }
        };
        assertFalse(f.service.confirmReleasedRunWindowTail(usage));
        assertEquals(original, f.row(tail)); assertEquals(captures, settled()); assertEquals(fraction, currentRemainder().toString());
    }

    @Test void ordinaryRunWindowExpiryStillMetersAgainstAnUnexpiredOriginalHold() throws Exception {
        String active = f.usage("future-budget-test", "1", "500000", "1"); long end = f.start + 60000L;
        f.jdbc.update("UPDATE vls_compute_usage SET state='STOPPED',billing_end_at=? WHERE id=?", new Timestamp(end), active);
        f.jdbc.update("UPDATE vls_compute_billing_session SET last_running_ms=?,safety_state='STOP_CONFIRMED',physical_stop_observed_ms=?,stop_requested=1,risk_code='RUN_WINDOW_EXPIRED',funding_required=1,funding_hold_intent_id='fixture-unexpired-hold',funding_hold_id='102',funding_expires_at_ms=? WHERE usage_id=?",
            end, end, System.currentTimeMillis() + 600000L, active);
        String reference = f.service.prepareInterval(active, end);
        assertEquals("PREPARED", f.state(reference)); assertNull(f.row(reference).get("error_code")); assertEquals(0, gateway.writes);
        f.gateway = f.new FakeGateway();
        ComputeBillingSettlementService normal = f.service(f.jdbc);
        normal.dispatch(reference); normal.dispatch(reference);
        assertEquals("SETTLED", f.state(reference)); assertEquals(1, f.gateway.captures.size());
        assertEquals("RUN_WINDOW_EXPIRED", f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?", String.class, active));
        assertEquals(1, f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?", Integer.class, active));
        f.properties.setHoldStopLeadSeconds(400);
        assertEquals("Funding/authorization blocks new work", assertThrows(IllegalStateException.class, () -> normal.requirePaidAdmission(active)).getMessage());
    }

    @Test void normalTicksUseTheReleaseBarrierForTheHistoricalRunWindowTail() {
        ReleaseBarrierGateway barrier = runWindowTail(); List<Map<String, Object>> captures = settled();
        ComputePaidRunService paid = new ComputePaidRunService(f.jdbc, f.manager, f.json, mock(ComputeNodeService.class), mock(ComputeBillingLedgerService.class), f.service, mock(ComputeBillingLifecycleService.class), funding, new ComputeFundingRecoveryService(funding, () -> barrier, f.properties), f.properties, new ComputeBillingPolicy(), new DefaultListableBeanFactory().getBeanProvider(ComputePowerProvider.class));
        for (int attempt = 0; attempt < 4; attempt++) paid.tick(run);
        assertEquals("COMPLETED", f.jdbc.queryForObject("SELECT state FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
        assertNull(f.jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
        assertEquals(ComputeBillingSettlementService.RELEASED_RUN_WINDOW_TAIL, f.row(tail).get("error_code")); assertEquals("BLOCKED", f.state(tail));
        assertEquals(captures, settled()); assertEquals(captured, barrier.detail.path("captured_quota").asText()); assertEquals(1, barrier.releaseEffects); assertEquals(0, barrier.writes);
    }

    @Test void preExpiryCreationCannotClaimNeverDebitedProof() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET create_time=? WHERE merchant_charge_id=?", new Timestamp(expiry - 1), tail);
        assertDeniedUnchanged();
    }

    @Test void creationInsideClockSafetyMarginIsRejected() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET create_time=? WHERE merchant_charge_id=?", new Timestamp(expiry + 1000), tail);
        assertDeniedUnchanged();
    }

    @Test void priorUnknownSubmissionCannotBeReclassified() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN' WHERE merchant_charge_id=?", tail);
        assertDeniedUnchanged();
        assertEquals("EXPIRED_TAIL_UNSETTLED_SLICE_STATE", f.jdbc.queryForObject("SELECT error_code FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
    }

    @Test void conflictingReferenceEvidenceCannotConfirmAbsence() {
        gateway.reference = ExternalBillingGateway.Status.CONFLICT;
        assertDeniedUnchanged();
    }

    @Test void unknownKeyEvidenceCannotConfirmAbsence() {
        gateway.key = ExternalBillingGateway.Status.UNKNOWN;
        assertDeniedUnchanged();
    }

    @Test void processingOriginalOperationCannotConfirmAbsence() {
        gateway.key = ExternalBillingGateway.Status.PROCESSING;
        assertDeniedUnchanged();
    }

    @Test void activeTailLeaseCannotBeAdopted() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET lease_token='active-local-worker',lease_until_ms=? WHERE merchant_charge_id=?", System.currentTimeMillis() + 30000, tail);
        assertDeniedUnchanged();
    }

    @Test void persistedChargeEvidenceCannotBeIgnoredEvenWhenLookupsAreAbsent() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET charge_id='persisted-original-charge' WHERE merchant_charge_id=?", tail);
        assertDeniedUnchanged();
        assertEquals("EXPIRED_TAIL_DEBIT_EVIDENCE", f.jdbc.queryForObject("SELECT error_code FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
    }

    @Test void missingUnsettledTailReportsSpecificReasonWithoutChangingEvidence() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='SETTLED',error_code=NULL,charge_id='999',ledger_entry_id='1999' WHERE merchant_charge_id=?", tail);
        assertDeniedUnchanged();
        assertEquals("EXPIRED_TAIL_MISSING", f.jdbc.queryForObject("SELECT error_code FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
    }

    @Test void earlierSettledCaptureMissingReceiptReportsSpecificReason() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET ledger_entry_id=NULL WHERE usage_id=? AND slice_sequence=0", usage);
        assertDeniedUnchanged();
        assertEquals("EXPIRED_TAIL_SETTLED_RECEIPT_MISSING", f.jdbc.queryForObject("SELECT error_code FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
    }

    @Test void changedOriginalTailBodyCannotAcquireNeverDebitedProof() {
        f.jdbc.update("UPDATE vls_compute_billing_slice SET request_json='{}' WHERE merchant_charge_id=?", tail);
        assertDeniedUnchanged();
    }

    @Test void changedLedgerWithSameMerchantCannotUseOriginalEvidence() {
        f.properties.setBaseUrl("https://another-ledger.fixture.invalid");
        assertDeniedUnchanged();
        assertEquals(0, gateway.references); assertEquals(0, gateway.keys); assertEquals(0, gateway.holds);
    }

    @Test void authoritativeHoldCounterMismatchCannotConfirmTail() {
        gateway.detail.put("captured_quota", new BigInteger(captured).add(BigInteger.ONE).toString());
        gateway.detail.put("remaining_quota", new BigInteger("1000000").subtract(new BigInteger(captured)).subtract(BigInteger.ONE).toString());
        assertDeniedUnchanged();
    }

    @Test void renewedQuoteCannotReuseOriginalCreationClockEvidence() {
        f.jdbc.update("UPDATE vls_compute_paid_run SET quote_until_ms=quote_until_ms+10000 WHERE run_id=?", run);
        assertDeniedUnchanged();
    }

    @Test void changedAccountRemainderCannotBeRewritten() {
        f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text='1',denominator_text='7' WHERE payer_user_id='123' AND quota_unit=?", remainderUnit);
        assertDeniedUnchanged();
    }

    @Test void laterSameAccountSlicePreventsFractionRollbackAcrossUsage() throws Exception {
        String successor = f.usage("tenant-other", "1", "500000", "1");
        String reference = f.interval(successor, 1000);
        Timestamp created = f.jdbc.queryForObject("SELECT create_time FROM vls_compute_billing_slice WHERE merchant_charge_id=?", Timestamp.class, tail);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET create_time=? WHERE merchant_charge_id=?", created, reference);
        Map<String, Object> originalTail = f.row(tail);
        f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id='123' AND quota_unit=?",
            originalTail.get("remainder_numerator"), originalTail.get("remainder_denominator"), remainderUnit);
        assertDeniedUnchanged();
        assertEquals("EXPIRED_TAIL_SUCCESSOR_EXISTS", f.jdbc.queryForObject("SELECT error_code FROM vls_compute_paid_run WHERE run_id=?", String.class, run));
    }

    @Test void databaseClockSkewCannotAuthorizeProof() {
        JdbcTemplate skewed = new JdbcTemplate(f.jdbc.getDataSource()) {
            @Override public <T> T queryForObject(String sql, RowMapper<T> mapper) {
                return super.queryForObject(sql, clockQuery(sql) ? skewed(mapper) : mapper);
            }
            @Override public <T> T queryForObject(String sql, RowMapper<T> mapper, Object... args) {
                return super.queryForObject(sql, clockQuery(sql) ? skewed(mapper) : mapper, args);
            }
            private boolean clockQuery(String sql) {
                String value = sql.toUpperCase(Locale.ROOT);
                return value.contains("NOW(3)") || value.contains("CURRENT_TIMESTAMP");
            }
            private <T> RowMapper<T> skewed(RowMapper<T> mapper) {
                return (rs, row) -> mapper.mapRow((ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, method, args) -> {
                    try {
                        Object value = method.invoke(rs, args);
                        return "getTimestamp".equals(method.getName()) && value instanceof Timestamp
                            ? new Timestamp(((Timestamp) value).getTime() + 60000L) : value;
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                }), row);
            }
        };
        assertDeniedUnchanged(new ComputeBillingSettlementService(skewed, f.manager, f.json, f.properties,
            (java.util.function.Supplier<ExternalBillingGateway>) () -> gateway));
    }

    private void assertDeniedUnchanged() { assertDeniedUnchanged(f.service); }

    private ReleaseBarrierGateway runWindowTail() {
        ReleaseBarrierGateway barrier = new ReleaseBarrierGateway(); barrier.detail = gateway.detail.deepCopy();
        gateway = barrier; f.gateway = barrier;
        f.jdbc.update("UPDATE vls_compute_billing_slice SET error_code='RUN_WINDOW_EXPIRED',create_time=? WHERE merchant_charge_id=?", new Timestamp(expiry - 10000L), tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='RUN_WINDOW_EXPIRED' WHERE usage_id=?", usage);
        return barrier;
    }

    private ReleaseBarrierGateway sshTail(boolean active) throws Exception {
        ReleaseBarrierGateway barrier=runWindowTail();
        f.jdbc.update("UPDATE vls_compute_billing_slice SET error_code='SSH_PREPARATION_FAILED' WHERE merchant_charge_id=?",tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='SSH_PREPARATION_FAILED' WHERE usage_id=?",usage);
        if(active) {
            expiry=System.currentTimeMillis()+600000L;
            JsonNode prior=f.json.readTree(hold.body);
            String body=FundingContracts.hold("vls-test",hold.reference,"123","1000000",Long.toString(expiry),prior.path("reason").asText()).toString();
            f.jdbc.update("UPDATE vls_compute_funding_intent SET request_json=?,request_hash=? WHERE intent_id=?",body,ComputeBillingSettlementService.hash(body),hold.id);
            f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?",expiry,usage);
            f.jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=? WHERE run_id=?",expiry,run);
            barrier.detail=detail(captured,"PARTIALLY_CAPTURED",9);
            String stored=barrier.detail.toString();
            f.jdbc.update("UPDATE vls_compute_funding_hold_observation SET expires_at_ms=?,hold_status='PARTIALLY_CAPTURED',detail_json=?,detail_hash=?,observed_at_ms=? WHERE hold_intent_id=?",expiry,stored,ComputeBillingSettlementService.hash(stored),System.currentTimeMillis(),hold.id);
        }
        return barrier;
    }

    private ComputePaidRunService paidService(ReleaseBarrierGateway barrier) {
        return new ComputePaidRunService(f.jdbc,f.manager,f.json,mock(ComputeNodeService.class),mock(ComputeBillingLedgerService.class),f.service,
            mock(ComputeBillingLifecycleService.class),funding,new ComputeFundingRecoveryService(funding,()->barrier,f.properties),f.properties,
            new ComputeBillingPolicy(),new DefaultListableBeanFactory().getBeanProvider(ComputePowerProvider.class));
    }

    private void resetTailForRunningMeter() throws Exception {
        Map<String,Object> original=f.row(tail);ExactQuota before=priorRemainder(original);
        f.jdbc.update("DELETE FROM vls_compute_billing_slice WHERE merchant_charge_id=?",tail);
        f.jdbc.update("UPDATE vls_compute_billing_session SET safety_state='STOP_PENDING',metered_until_ms=?,next_sequence=? WHERE usage_id=?",original.get("interval_start_ms"),original.get("slice_sequence"),usage);
        f.jdbc.update("UPDATE vls_compute_usage SET state='RUNNING',billing_end_at=NULL WHERE id=?",usage);
        f.jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id='123' AND quota_unit=?",before.numerator.toString(),before.denominator.toString(),remainderUnit);
    }

    private void assertSshRearmDenied() {
        Map<String,Object> original=new LinkedHashMap<>(f.row(tail));String fraction=currentRemainder().toString();
        assertFalse(f.service.recoverStoppedSshPreparationTail(usage));assertEquals(original,f.row(tail));assertEquals(fraction,currentRemainder().toString());assertEquals(0,gateway.writes);
    }

    private void assertSshWaiverDenied() {
        Map<String,Object> original=new LinkedHashMap<>(f.row(tail));String fraction=currentRemainder().toString();List<Map<String,Object>> captures=settled();
        assertFalse(f.service.confirmReleasedSshPreparationTail(usage));assertEquals(original,f.row(tail));assertEquals(fraction,currentRemainder().toString());assertEquals(captures,settled());assertEquals(0,gateway.writes);
    }

    private ComputeFundingIntentService.Intent releaseOriginal(ReleaseBarrierGateway barrier) {
        ComputeFundingIntentService.Intent release = funding.prepareFinalRelease(hold.id, barrier.detail.path("remaining_quota").asText());
        f.jdbc.update("UPDATE vls_compute_paid_run SET release_intent_id=?,state='RELEASING' WHERE run_id=?", release.id, run);
        return new ComputeFundingRecoveryService(funding, () -> barrier, f.properties).advance(release.id, System.currentTimeMillis());
    }

    private void assertRunWindowDenied(boolean afterRelease) {
        Map<String, Object> original = new LinkedHashMap<>(f.row(tail)); List<Map<String, Object>> captures = settled(); String fraction = currentRemainder().toString();
        assertFalse(afterRelease ? f.service.confirmReleasedRunWindowTail(usage) : f.service.allowExpiredRunWindowTailRelease(usage));
        assertEquals(original, f.row(tail)); assertEquals(captures, settled()); assertEquals(fraction, currentRemainder().toString()); assertEquals(0, gateway.writes);
    }

    private void assertDeniedUnchanged(ComputeBillingSettlementService service) {
        List<Map<String, Object>> before = f.jdbc.queryForList("SELECT * FROM vls_compute_billing_slice ORDER BY merchant_charge_id");
        List<Map<String, Object>> remainders = f.jdbc.queryForList("SELECT * FROM vls_compute_account_remainder ORDER BY payer_user_id,quota_unit");
        List<Map<String, Object>> fundingBefore = f.jdbc.queryForList("SELECT * FROM vls_compute_funding_intent ORDER BY intent_id");
        assertFalse(service.confirmExpiredUnsentTail(usage));
        assertEquals(before, f.jdbc.queryForList("SELECT * FROM vls_compute_billing_slice ORDER BY merchant_charge_id"));
        assertEquals(remainders, f.jdbc.queryForList("SELECT * FROM vls_compute_account_remainder ORDER BY payer_user_id,quota_unit"));
        assertEquals(fundingBefore, f.jdbc.queryForList("SELECT * FROM vls_compute_funding_intent ORDER BY intent_id"));
        assertEquals(0, gateway.writes);
    }

    private List<Map<String, Object>> settled() {
        return f.jdbc.queryForList("SELECT * FROM vls_compute_billing_slice WHERE usage_id=? AND state='SETTLED' ORDER BY slice_sequence", usage);
    }

    private ExactQuota currentRemainder() {
        Map<String, Object> row = f.jdbc.queryForMap("SELECT * FROM vls_compute_account_remainder WHERE payer_user_id='123' AND quota_unit=?", remainderUnit);
        return new ExactQuota(new BigInteger(String.valueOf(row.get("numerator_text"))), new BigInteger(String.valueOf(row.get("denominator_text"))));
    }

    private ExactQuota priorRemainder(Map<String, Object> tailRow) throws Exception {
        BillingSnapshot snapshot = BillingSnapshot.read(f.jdbc.queryForObject("SELECT accepted_snapshot FROM vls_compute_billing_session WHERE usage_id=?", String.class, usage), f.json);
        ExactQuota after = new ExactQuota(new BigInteger(String.valueOf(tailRow.get("remainder_numerator"))), new BigInteger(String.valueOf(tailRow.get("remainder_denominator"))));
        ExactQuota cost = ExactQuota.compute(snapshot.approvedHourlyPrice,
            ((Number) tailRow.get("interval_end_ms")).longValue() - ((Number) tailRow.get("interval_start_ms")).longValue(), snapshot.numerator, snapshot.denominator);
        BigInteger amount = new BigInteger(String.valueOf(tailRow.get("amount_text")));
        return new ExactQuota(amount.multiply(after.denominator).add(after.numerator).multiply(cost.denominator)
            .subtract(cost.numerator.multiply(after.denominator)), after.denominator.multiply(cost.denominator));
    }

    private ObjectNode detail(String settled, String status, long version) {
        ObjectNode result = f.json.createObjectNode();
        result.put("hold_id", "101"); result.put("merchant_hold_id", "vls-hold:" + usage + ":initial"); result.put("user_id", "123");
        result.put("hold_quota", "1000000"); result.put("captured_quota", settled); result.put("released_quota", "0");
        result.put("remaining_quota", new BigInteger("1000000").subtract(new BigInteger(settled)).toString());
        result.put("hold_status", status); result.put("balance_version", Long.toString(version)); result.put("expires_at", Long.toString(expiry));
        result.put("created_at", Long.toString((expiry - 600000L) / 1000)); result.put("updated_at", Long.toString((expiry - 1000L) / 1000));
        return result;
    }

    private Path migrationDirectory() {
        String configured = System.getProperty("billing.migration");
        if (configured != null) return Paths.get(configured).toAbsolutePath().getParent();
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Actual Flyway source root required");
        return root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/mainline");
    }

    private class ReadOnlyGateway extends ComputeBillingSettlementTest.FakeGateway {
        ObjectNode detail;
        ExternalBillingGateway.Status reference = ExternalBillingGateway.Status.NOT_FOUND;
        ExternalBillingGateway.Status key = ExternalBillingGateway.Status.NOT_FOUND;
        int references, keys, holds, writes;
        ReadOnlyGateway() { f.super(); }
        @Override public Result byReference(String reference) { references++; assertEquals(tail, reference); return Result.of(this.reference); }
        @Override public Result byKey(String key) { keys++; assertEquals(f.row(tail).get("idempotency_key"), key); return Result.of(this.key); }
        @Override public Result holdById(String id) { holds++; assertEquals("101", id); return new Result(Status.SUCCEEDED, "", detail.deepCopy()); }
        @Override public Result capture(String key, String body) { writes++; throw new AssertionError("Expired-tail proof cannot capture"); }
        @Override public Result createHold(String key, String body) { writes++; throw new AssertionError("Expired-tail proof cannot freeze"); }
        @Override public Result topupHold(String id, String key, String body) { writes++; throw new AssertionError("Expired-tail proof cannot top up"); }
        @Override public Result releaseHold(String id, String key, String body) { writes++; throw new AssertionError("Proof is separate from original final release"); }
        @Override public Result refund(String id, String key, String body) { writes++; throw new AssertionError("Original eight captures must remain intact"); }
    }

    /** The successful original release serializes with capture at the same hold lock. */
    private class ReleaseBarrierGateway extends ReadOnlyGateway {
        final Map<String, String> releaseBodies = new LinkedHashMap<>();
        final Map<String, ObjectNode> releaseReceipts = new LinkedHashMap<>();
        int releasePosts, releaseEffects, captureKeyQueries;
        boolean releaseUnknownBeforeCommit, loseReleaseReply, refundedCapture, wrongCaptureAmount, tailFound, partialRelease, minimalCaptureReference, terminalExpired;
        Runnable postReleaseHoldHook, postHoldHook;
        @Override public Result holdByReference(String reference) { assertEquals(hold.reference, reference); return new Result(Status.SUCCEEDED, "", detail.deepCopy()); }
        @Override public Result holdById(String id) {
            Result result = super.holdById(id);
            if(postHoldHook!=null){Runnable hook=postHoldHook;postHoldHook=null;hook.run();}
            if (releaseEffects > 0 && postReleaseHoldHook != null) { Runnable hook = postReleaseHoldHook; postReleaseHoldHook = null; hook.run(); }
            return result;
        }
        @Override public Result byReference(String reference) {
            if (tail.equals(reference)) return tailFound ? new Result(Status.SUCCEEDED, "", f.json.createObjectNode().put("status", "SUCCEEDED")) : super.byReference(reference);
            List<Map<String, Object>> rows = f.jdbc.queryForList("SELECT * FROM vls_compute_billing_slice WHERE usage_id=? AND merchant_charge_id=? AND state='SETTLED'", usage, reference);
            if (rows.size() != 1) return Result.of(Status.NOT_FOUND);
            Result result = originalCapture(rows.get(0));
            if (minimalCaptureReference) ((ObjectNode) result.charge).remove(java.util.Arrays.asList("merchant_id", "hold_id", "metering_ref", "price_snapshot_id", "conversion_version"));
            return result;
        }
        @Override public Result byKey(String key) {
            if (key.equals(f.row(tail).get("idempotency_key"))) return tailFound ? new Result(Status.PROCESSING, "", null) : super.byKey(key);
            List<Map<String, Object>> rows = f.jdbc.queryForList("SELECT * FROM vls_compute_billing_slice WHERE usage_id=? AND idempotency_key=? AND state='SETTLED'", usage, key);
            if (rows.size() != 1) return Result.of(Status.NOT_FOUND);
            captureKeyQueries++; Result result = originalCapture(rows.get(0));
            ((ObjectNode) result.charge).put("operation_type", "CHARGE").put("idempotency_key", key);
            return result;
        }
        private Result originalCapture(Map<String, Object> slice) {
            try {
                ObjectNode receipt = (ObjectNode) f.json.readTree(String.valueOf(slice.get("request_json")));
                receipt.put("status", "SUCCEEDED").put("charge_id", String.valueOf(slice.get("charge_id"))).put("ledger_entry_id", String.valueOf(slice.get("ledger_entry_id")))
                    .put("charged_quota", wrongCaptureAmount ? new BigInteger(String.valueOf(slice.get("amount_text"))).add(BigInteger.ONE).toString() : String.valueOf(slice.get("amount_text")))
                    .put("refunded_quota", refundedCapture ? "1" : "0");
                return new Result(Status.SUCCEEDED, "", receipt);
            } catch (Exception invalid) { throw new AssertionError(invalid); }
        }
        @Override public Result releaseHold(String id, String key, String body) {
            assertEquals("101", id); releasePosts++;
            if (releaseReceipts.containsKey(key)) { assertEquals(releaseBodies.get(key), body); return new Result(Status.SUCCEEDED, "", releaseReceipts.get(key).deepCopy()); }
            if (releaseUnknownBeforeCommit) return Result.of(Status.UNKNOWN);
            try {
                JsonNode request = f.json.readTree(body); assertEquals("vls-release:" + usage + ":final", request.path("merchant_release_id").asText());
                BigInteger available = new BigInteger(detail.path("remaining_quota").asText());
                String amount = (partialRelease ? available.subtract(BigInteger.ONE) : available).toString(), remaining = partialRelease ? "1" : "0";
                detail.put("released_quota", amount).put("remaining_quota", remaining).put("hold_status", terminalExpired ? "EXPIRED" : partialRelease ? "PARTIALLY_CAPTURED" : "RELEASED").put("balance_version", "10");
                ObjectNode receipt = f.json.createObjectNode().put("hold_id", id).put("held_quota", remaining).put("released_quota", amount).put("remaining_quota", remaining).put("hold_status", detail.path("hold_status").asText());
                releaseBodies.put(key, body); releaseReceipts.put(key, receipt); releaseEffects++;
                return loseReleaseReply ? Result.of(Status.UNKNOWN) : new Result(Status.SUCCEEDED, "", receipt.deepCopy());
            } catch (Exception invalid) { throw new AssertionError(invalid); }
        }
        @Override public Result holdOperation(String operation, String reference) {
            if (!"RELEASE".equals(operation) || !reference.equals("vls-release:" + usage + ":final") || releaseBodies.size() != 1) return Result.of(Status.UNKNOWN);
            String key = releaseBodies.keySet().iterator().next();
            return new Result(Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", f.json.createObjectNode().put("status", "SUCCEEDED").put("hold_id", "101").put("user_id", "123").put("idempotency_key", key));
        }
    }
}
