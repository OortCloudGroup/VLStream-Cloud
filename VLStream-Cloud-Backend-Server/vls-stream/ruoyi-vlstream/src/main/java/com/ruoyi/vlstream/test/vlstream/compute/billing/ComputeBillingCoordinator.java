package com.ruoyi.vlstream.test.vlstream.compute.billing;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/** Default-off periodic orchestration. No task/instance is admitted or started here. */
@Service
public class ComputeBillingCoordinator {
    private final JdbcTemplate jdbc;
    private final ComputeBillingSettlementService settlements;
    private final ExternalBillingProperties properties;
    private final ObjectProvider<BillingLifecycleGateway> lifecycles;
    private final ComputeBillingReconciliationService reconciliation;
    public ComputeBillingCoordinator(JdbcTemplate jdbc, ComputeBillingSettlementService settlements, ExternalBillingProperties properties,
                                     ObjectProvider<BillingLifecycleGateway> lifecycles, ComputeBillingReconciliationService reconciliation) {
        this.jdbc = jdbc; this.settlements = settlements; this.properties = properties; this.lifecycles = lifecycles; this.reconciliation = reconciliation;
    }
    @Scheduled(fixedDelayString = "${vlstream.compute.external-billing.poll-delay-ms:15000}", initialDelayString = "${vlstream.compute.external-billing.initial-delay-ms:30000}")
    public void scan() {
        if (!properties.isEnabled()) return;
        BillingLifecycleGateway lifecycle = lifecycles.getIfAvailable(); if (lifecycle == null) return;
        if (properties.getSettlementIntervalSeconds() < 1 || properties.getSettlementIntervalSeconds() > 3600) throw new IllegalStateException("Invalid settlement interval");
        List<String> usages = jdbc.queryForList("SELECT u.usage_id FROM vls_compute_billing_session u WHERE control_snapshot IS NOT NULL AND (safety_state<>'STOP_CONFIRMED' OR metered_until_ms<last_running_ms OR EXISTS (SELECT 1 FROM vls_compute_billing_slice s WHERE s.usage_id=u.usage_id AND s.state NOT IN ('SETTLED','NO_DEBIT','BLOCKED'))) ORDER BY next_poll_ms,u.usage_id LIMIT 100", String.class);
        for (String usage : usages) tick(usage, lifecycle);
        reconciliation.tick();
    }
    public void tick(String usageId, BillingLifecycleGateway lifecycle) {
        if (!properties.isEnabled()) return;
        try {
            if(jdbc.queryForList("SELECT id FROM vls_compute_usage WHERE id=? AND billing_start_at IS NOT NULL",usageId).isEmpty())return;
            BillingLifecycleGateway.Observation observation = lifecycle.observe(usageId);
            if ("RUNNING".equals(observation.snapshot.power)) settlements.recordRunningObservation(usageId, observation.observedAt);
            else if ("STOPPED".equals(observation.snapshot.power)) settlements.confirmPhysicalStop(usageId, observation.observedAt);
            else if (!"STOPPING".equals(observation.snapshot.power)) risk(usageId, "PROVIDER_STATE_UNKNOWN");
            Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", usageId);
            long cursor = ((Number) session.get("metered_until_ms")).longValue();
            long proven = ((Number) session.get("last_running_ms")).longValue();
            long interval = Math.multiplyExact((long) properties.getSettlementIntervalSeconds(), 1000L);
            // Topup never extends this original expiry. Request a safe stop early enough to begin
            // shutdown, while continuing to meter actual runtime until a stopped observation.
            if (session.get("funding_expires_at_ms") != null && !"STOPPED".equals(observation.snapshot.power)) {
                long expires = ((Number) session.get("funding_expires_at_ms")).longValue();
                if (properties.getHoldStopLeadSeconds() < 1 || properties.getHoldStopLeadSeconds() > 3600)
                    throw new IllegalStateException("Invalid hold stop lead time");
                long warning = Math.max(Math.multiplyExact((long) properties.getHoldStopLeadSeconds(), 1000L), interval);
                if (expires <= Math.addExact(System.currentTimeMillis(), warning)) risk(usageId, "HOLD_EXPIRING");
            }
            int sealed = 0;
            while (proven - cursor >= interval && sealed++ < 100) { cursor = Math.addExact(cursor, interval); settlements.prepareInterval(usageId, cursor); }
            if ("STOPPED".equals(observation.snapshot.power) && proven > cursor && sealed < 100) settlements.prepareInterval(usageId, proven);
            List<Map<String, Object>> pending = jdbc.queryForList("SELECT merchant_charge_id,attempt_count FROM vls_compute_billing_slice WHERE usage_id=? AND state NOT IN ('SETTLED','NO_DEBIT','BLOCKED') AND next_attempt_ms<=? ORDER BY slice_sequence LIMIT 100", usageId, System.currentTimeMillis());
            for (Map<String, Object> row : pending) {
                settlements.dispatch(String.valueOf(row.get("merchant_charge_id")));
                int attempts = Math.min(((Number) row.get("attempt_count")).intValue() + 1, 30);
                long delay = Math.min(30000L, 1000L << Math.min(attempts - 1, 5));
                jdbc.update("UPDATE vls_compute_billing_slice SET attempt_count=?,next_attempt_ms=? WHERE merchant_charge_id=?", attempts, System.currentTimeMillis() + delay, row.get("merchant_charge_id"));
                if (attempts >= 5 && !jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE merchant_charge_id=? AND state IN ('UNKNOWN','PROCESSING')", row.get("merchant_charge_id")).isEmpty()) risk(usageId, "SETTLEMENT_CONFIRMATION_EXPIRED");
            }
        } catch (Exception e) { risk(usageId, "COORDINATION_UNAVAILABLE"); }
        Map<String, Object> state = jdbc.queryForMap("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?", usageId);
        if (((Number) state.get("stop_requested")).intValue() == 1) {
            if (properties.isSafetyStopEnabled()) {
                try { lifecycle.requestSafeStop(usageId); }
                catch (Exception e) { jdbc.update("UPDATE vls_compute_billing_session SET safety_state='RETRY_REQUIRED' WHERE usage_id=? AND safety_state<>'STOP_CONFIRMED'", usageId); }
            }
        }
        jdbc.update("UPDATE vls_compute_billing_session SET next_poll_ms=? WHERE usage_id=?",System.currentTimeMillis()+15000,usageId);
    }
    private void risk(String usageId, String code) {
        jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=CASE WHEN risk_code='NOT_AUTHORIZED' THEN risk_code WHEN ?='HOLD_EXPIRING' AND risk_code IS NOT NULL THEN risk_code ELSE ? END WHERE usage_id=?", code, code, usageId);
    }
}
