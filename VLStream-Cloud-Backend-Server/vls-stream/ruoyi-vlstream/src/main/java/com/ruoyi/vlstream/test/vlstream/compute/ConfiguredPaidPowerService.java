package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingTrialGuard;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** The existing paid order is the durable power outbox. Catalog data stays in YAML.
 * No API retry token is assumed: after SUBMITTED, only status queries are automatic. */
@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class ConfiguredPaidPowerService {
    static final String REJECTED_NO_STOCK="REJECTED_NO_STOCK";
    @org.springframework.beans.factory.annotation.Autowired
    private ComputeBillingTrialGuard trialGuard;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private AutoDlRuntimeSsh runtimeSsh;
    @org.springframework.beans.factory.annotation.Autowired
    private ObjectProvider<ExternalBillingGateway> externalGateways;
    private final JdbcTemplate jdbc;
    private final ConfiguredComputeRegistry configured;
    private final ObjectProvider<ComputePowerProvider> providers;
    private final ComputeBillingLedgerService ledger;
    private final com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService settlements;

    ComputeNode refresh(ComputeNode node) {
        configured.verifyOfficial(node);configured.recordPower(node,provider(node).status(node));return configured.resolve(node.getId());
    }
    private ComputePowerProvider provider(ComputeNode node) {
        return providers.orderedStream().filter(p->p.supports(node)).findFirst().orElseThrow(()->ConfiguredComputeRegistry.unavailable("官方电源接口尚未配置"));
    }
    void advance(Map<String,Object> run,ComputeNode verified) {
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Power HTTP requires a committed outbox");
        configured.verifyOfficial(verified);String id=text(run,"run_id"),usage=text(run,"usage_id");
        boolean stop="STOPPING".equals(text(run,"state"))||!"PREPARED".equals(text(run,"power_stop_state"));
        String column=stop?"power_stop_state":"power_start_state",attempt=stop?"power_stop_attempt_ms":"power_start_attempt_ms";
        String phase=text(run,column),operation=stop?jdbc.queryForObject("SELECT stop_operation_id FROM vls_compute_billing_session WHERE usage_id=?",String.class,usage):text(run,"start_operation_id");
        ComputePowerProvider supplier=provider(verified);
        if("PREPARED".equals(phase)) {
            if(!stop) {
                try {
                    // An accepted order may have waited across a restart or configuration
                    // change. Recheck its original held funds before the first power write.
                    settlements.requirePaidAdmission(usage);
                } catch(RuntimeException denied) {
                    blockUnsentStart(id,usage,"ADMISSION_UNAVAILABLE");
                    return;
                }
                try { requireTrialStart(run,verified); }
                catch(RuntimeException denied) {
                    // No power submission has happened. Preserve the original hold for safe cancellation.
                    blockUnsentStart(id,usage,"TRIAL_GUARD_BLOCKED");
                    return;
                }
                ComputeNode preflight=refresh(verified);
                if(!"STOPPED".equals(preflight.getPowerState()))return;
                ComputeBillingPolicy.requireEconomicalPaidOffer(preflight,System.currentTimeMillis());
            }
            if(operation==null)throw new IllegalStateException("Original power operation required");
            long now=System.currentTimeMillis();
            if(jdbc.update("UPDATE vls_compute_paid_run SET "+column+"='SUBMITTED',"+attempt+"=?,update_time=NOW() WHERE run_id=? AND "+column+"='PREPARED'",now,id)!=1)return;
            configured.pending(verified,operation,stop?"stop":"start",true);
            try {
                supplier.submit(verified,stop?"stop":"start",operation);
                jdbc.update("UPDATE vls_compute_paid_run SET "+column+"='ACCEPTED',error_code=NULL WHERE run_id=? AND "+column+"='SUBMITTED'",id);
            } catch(ComputeProviderException rejected) {
                log.warn("Official power operation unresolved run={} action={} operation={} kind={} http={} diagnostic={} providerRequestId={}",
                    id,stop?"stop":"start",operation,rejected.getKind(),rejected.getHttpStatus(),rejected.getDiagnostic(),rejected.getProviderRequestId());
                if(!stop&&rejected.getKind()!=ComputeProviderException.Kind.UNKNOWN) {
                    boolean noStock=rejected.getKind()==ComputeProviderException.Kind.NO_STOCK;
                    // The original outbox retains the trusted start cause independently
                    // of temporary funding/release errors on the public order status.
                    jdbc.update("UPDATE vls_compute_paid_run SET power_start_state=?,error_code=? WHERE run_id=?",
                        noStock?REJECTED_NO_STOCK:"REJECTED",noStock?"NO_STOCK":"START_REJECTED",id);
                    rejectedStart(usage,verified);return;
                }
                jdbc.update("UPDATE vls_compute_paid_run SET "+column+"='UNKNOWN',error_code=? WHERE run_id=?",stop?"STOP_OUTCOME_UNKNOWN":"START_OUTCOME_UNKNOWN",id);
            } catch(RuntimeException unknown) {
                jdbc.update("UPDATE vls_compute_paid_run SET "+column+"='UNKNOWN',error_code=? WHERE run_id=?",stop?"STOP_OUTCOME_UNKNOWN":"START_OUTCOME_UNKNOWN",id);
            }
            run=jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id=?",id);
        } else if("CANCELLED".equals(phase)||"REJECTED".equals(phase)||REJECTED_NO_STOCK.equals(phase)) {
            // Also complete a rejection journaled before a process restart. This
            // does not resend power or free the original unresolved funding claim.
            if(!stop&&!"CANCELLED".equals(phase))rejectedStart(usage,verified);
            return;
        }
        else if(stop||"STARTING".equals(text(run,"state")))configured.pending(verified,operation,stop?"stop":"start",true);

        ComputePowerProvider.Snapshot observed=supplier.status(verified);configured.recordPower(verified,observed);
        Instant now=Instant.now();
        Map<String,Object> usageRow=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=?",usage);
        if("RUNNING".equals(observed.power)) {
            if(usageRow.get("billing_start_at")==null) {
                Instant event=validStartEvent(run,observed,now);
                ledger.confirmedRunning(usage,now,event==null?now:event,event==null?"OBSERVATION":"PROVIDER_EVENT");
            }
            if(!stop) {
                boolean connected=true;
                if(runtimeSsh!=null&&runtimeSsh.dynamic(verified))try {runtimeSsh.prepare(id,verified);}
                catch(Exception pending) {
                    connected=false;
                    jdbc.update("UPDATE vls_compute_paid_run SET error_code='SSH_PREPARATION_PENDING' WHERE run_id=?",id);
                    Map<String,Object> latest=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=?",usage);
                    Object started=latest.get("billing_start_at");
                    if(started instanceof java.sql.Timestamp&&System.currentTimeMillis()-((java.sql.Timestamp)started).getTime()>=30000L)
                        jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code='SSH_PREPARATION_FAILED' WHERE usage_id=?",usage);
                }
                if(connected)jdbc.update("UPDATE vls_compute_paid_run SET state='RUNNING',error_code=NULL WHERE run_id=? AND state='STARTING'",id);
            }
        } else if(ComputeBillingPolicy.computeStopConfirmed(verified,observed)) {
            if(runtimeSsh!=null)runtimeSsh.clear(verified);
            if(usageRow.get("billing_start_at")==null) {
                Instant start=validStartEvent(run,observed,now);
                if(start==null||observed.providerStoppedAt==null||observed.providerStoppedAt.isBefore(start)) {
                    // A plain stopped observation cannot resolve an unknown start.
                    // An acknowledged original stop followed by physical shutdown
                    // does resolve current control. No observed running interval
                    // means there is no evidence from which to invent a debit.
                    if(stop&&"ACCEPTED".equals(text(run,"power_stop_state"))&&run.get("power_stop_attempt_ms") instanceof Number) {
                        jdbc.update("UPDATE vls_compute_usage SET state='START_FAILED' WHERE id=? AND billing_start_at IS NULL AND state IN ('RESERVED','RECONCILE')",usage);
                    }
                    return;
                }
                ledger.confirmedRunning(usage,now,start,"PROVIDER_EVENT");
                usageRow=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=?",usage);
            }
            Instant start=((java.sql.Timestamp)usageRow.get("billing_start_at")).toInstant();
            Instant event=observed.providerStoppedAt;
            if(event!=null&&(event.isBefore(start)||event.isAfter(now)))event=null;
            ledger.confirmedStopped(usage,now,event==null?now:event,event==null?"OBSERVATION":"PROVIDER_EVENT");
        }
    }
    private void requireTrialStart(Map<String,Object> run,ComputeNode node) {
        String usage=text(run,"usage_id");
        if(trialGuard==null) {
            // Direct construction is retained for existing non-trial fixtures; a persisted marker
            // must never lose its guard because a dependency was omitted after restart.
            try {
                if(new com.fasterxml.jackson.databind.ObjectMapper().readTree(text(run,"quote_snapshot")).has("trial_guard"))
                    throw new IllegalStateException("Trial guard unavailable for original paid run");
            } catch(java.io.IOException invalid) {throw new IllegalStateException("Invalid paid-run snapshot",invalid);}
            return;
        }
        if(!trialGuard.protectedUsage(usage))return;
        trialGuard.requireNode(node);
        ExternalBillingGateway gateway=externalGateways==null?null:externalGateways.getIfAvailable();
        if(gateway==null)throw new IllegalStateException("Fresh trial conversion configuration required before power submission");
        trialGuard.requireNewUsage(usage,gateway.config());
    }
    private Instant validStartEvent(Map<String,Object> run,ComputePowerProvider.Snapshot observed,Instant now) {
        Object attempt=run.get("power_start_attempt_ms");Instant event=observed.providerStartedAt;
        return attempt instanceof Number&&event!=null&&!event.isBefore(Instant.ofEpochMilli(((Number)attempt).longValue()))&&!event.isAfter(now)?event:null;
    }
    private void rejectedStart(String usage,ComputeNode verified) {
        jdbc.update("UPDATE vls_compute_usage SET state='START_FAILED' WHERE id=? AND billing_start_at IS NULL AND state IN ('RESERVED','RECONCILE')",usage);
        configured.cancelled(verified);
    }
    boolean cancelUnsubmitted(Map<String,Object> run,ComputeNode verified) {
        configured.verifyOfficial(verified);
        if(jdbc.update("UPDATE vls_compute_paid_run SET power_start_state='CANCELLED' WHERE run_id=? AND power_start_state='PREPARED'",text(run,"run_id"))!=1)return false;
        configured.cancelled(verified);return true;
    }
    void queueStop(String usage,String operation) {
        jdbc.update("UPDATE vls_compute_paid_run SET state='STOPPING',error_code=NULL WHERE usage_id=? AND active_instance_key IS NOT NULL AND power_stop_state='PREPARED'",usage);
    }
    private void blockUnsentStart(String id,String usage,String code) {
        // A competing sender may already have claimed this original. A stale worker must
        // neither resubmit it nor turn its obsolete preflight failure into a new stop.
        jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=? AND EXISTS (SELECT 1 FROM vls_compute_paid_run WHERE run_id=? AND power_start_state='PREPARED')",usage,id);
        jdbc.update("UPDATE vls_compute_paid_run SET error_code=? WHERE run_id=? AND power_start_state='PREPARED'",code,id);
    }
    private static String text(Map<String,Object> row,String key){Object value=row.get(key);return value==null?null:String.valueOf(value);}
}
