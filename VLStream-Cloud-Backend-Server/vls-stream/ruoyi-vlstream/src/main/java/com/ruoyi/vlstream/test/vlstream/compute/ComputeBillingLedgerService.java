package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/** Internal COMPUTE-period usage/outbox, not a wallet or an implemented asset lease lifecycle.
 * The stable SSO opener pays the physical instance period, including idle time.
 * STOPPED/start-failure and clearing a compute-period key are NOT permission to release or transfer stored data.
 * Storage measurement and exclusive asset ownership through confirmed release remain unimplemented.
 * Paid rental stays disabled until the personal gateway, verified costs and safe lease/release contracts exist. */
@Service
@RequiredArgsConstructor
public class ComputeBillingLedgerService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @Transactional(rollbackFor=Exception.class)
    public String reserve(String serverKey, String tenant, String openingUserId, String verifiedAccountRef, ComputeBillingRate rate, boolean free, String startOperation) {
        if (free) return null;
        if (openingUserId==null || openingUserId.trim().isEmpty() || verifiedAccountRef==null || verifiedAccountRef.trim().isEmpty() || tenant==null || serverKey==null || startOperation==null)
            throw new ServiceException("付款人、实例租用归属和开机操作尚未确认");
        rate.validate(Instant.now());
        if (!rate.fixedUserPrice() && !"LOCKED_FOR_RUN".equals(rate.getPricePolicy()))
            throw new ServiceException("该供应商未确认本开机周期锁价，事先告知与分段价格计量尚未接入，收费继续阻断");
        List<Map<String,Object>> duplicate=jdbc.queryForList("SELECT id,tenant_id,server_key,opening_user_id,payer_account_ref FROM vls_compute_usage WHERE start_operation_id=? FOR UPDATE",startOperation);
        if (!duplicate.isEmpty()) {
            Map<String,Object> existing=duplicate.get(0);
            if (!tenant.equals(existing.get("tenant_id"))||!serverKey.equals(existing.get("server_key"))||!openingUserId.equals(existing.get("opening_user_id"))||!verifiedAccountRef.equals(existing.get("payer_account_ref"))) throw new ServiceException("开机操作付款归属不匹配");
            return String.valueOf(existing.get("id"));
        }
        String id=UUID.randomUUID().toString();
        try {
            jdbc.update("INSERT INTO vls_compute_usage(id,tenant_id,server_key,active_server_key,opening_user_id,payer_account_ref,start_operation_id,rate_snapshot,state) VALUES(?,?,?,?,?,?,?,?,'RESERVED')",
                id,tenant,serverKey,serverKey,openingUserId,verifiedAccountRef,startOperation,json.writeValueAsString(rate));
        } catch (org.springframework.dao.DuplicateKeyException e) { throw new ServiceException("收费实例已被独占租用，未授权多人共享或代扣"); }
        catch (java.io.IOException e) { throw new ServiceException("费率快照保存失败"); }
        return id;
    }
    public void confirmedRunning(String usage, Instant observedAt, Instant effectiveAt, String evidence) {
        if (usage==null) return;
        validateEvidence(observedAt,effectiveAt,evidence);
        jdbc.update("UPDATE vls_compute_usage SET state='RUNNING',start_observed_at=?,billing_start_at=?,start_evidence=? WHERE id=? AND state IN ('RESERVED','RECONCILE') AND billing_start_at IS NULL",
            Timestamp.from(observedAt),Timestamp.from(effectiveAt),evidence,usage);
    }
    public void startFailed(String usage) { if(usage!=null) jdbc.update("UPDATE vls_compute_usage SET state='START_FAILED',active_server_key=NULL WHERE id=? AND state IN ('RESERVED','RECONCILE') AND billing_start_at IS NULL",usage); }
    public void unknownOutcome(String usage) { if(usage!=null) jdbc.update("UPDATE vls_compute_usage SET state='RECONCILE' WHERE id=? AND state IN ('RESERVED','RUNNING')",usage); }
    static String serverKey(Long canonicalId) { return "node-server:"+canonicalId; }
    /** Internal lifecycle hook only. It never creates a payer/rate binding or dispatches a debit. */
    void observePower(ComputeNode operation, ComputePowerProvider.Snapshot snapshot, Long canonicalId) {
        String server=serverKey(canonicalId);
        List<Map<String,Object>> matches="start".equals(operation.getPowerOperationAction())
            ? jdbc.queryForList("SELECT id,billing_start_at FROM vls_compute_usage WHERE server_key=? AND start_operation_id=? FOR UPDATE",server,operation.getPowerOperationId())
            : jdbc.queryForList("SELECT id,billing_start_at FROM vls_compute_usage WHERE active_server_key=? FOR UPDATE",server);
        if (matches.isEmpty()) return;
        Map<String,Object> row=matches.get(0); String usageId=String.valueOf(row.get("id")); Instant now=Instant.now();
        if ("start".equals(operation.getPowerOperationAction())) {
            if ("RUNNING".equals(snapshot.power)) confirmedRunning(usageId,now,now,"OBSERVATION");
            else if ("FAILED".equals(snapshot.power)) startFailed(usageId);
            else unknownOutcome(usageId);
        } else if (ComputeBillingPolicy.computeStopConfirmed(operation,snapshot) && row.get("billing_start_at")!=null) confirmedStopped(usageId,now,now,"OBSERVATION");
        else unknownOutcome(usageId);
    }
    @Transactional(rollbackFor=Exception.class)
    public void confirmedStopped(String usage, Instant observedAt, Instant effectiveAt, String evidence) {
        if (usage==null) return;
        validateEvidence(observedAt,effectiveAt,evidence); Map<String,Object> row=usage(usage);
        Timestamp start=(Timestamp)row.get("billing_start_at");
        if (start==null) throw new ServiceException("开机起点尚未对账，不能从训练任务时长推算收费");
        if (effectiveAt.isBefore(start.toInstant())) throw new ServiceException("厂商停止时间早于开机时间，请对账");
        jdbc.update("UPDATE vls_compute_usage SET state='STOPPED',stop_observed_at=?,billing_end_at=?,stop_evidence=?,active_server_key=NULL WHERE id=? AND state IN ('RUNNING','RECONCILE') AND billing_end_at IS NULL",
            Timestamp.from(observedAt),Timestamp.from(effectiveAt),evidence,usage);
    }
    @Transactional(rollbackFor=Exception.class)
    public String prepareSettlement(String usageId) {
        Map<String,Object> row=usage(usageId);
        if ("SLICES".equals(row.get("billing_mode"))) throw new ServiceException("该用量已使用持久计费区间，禁止再生成整周期扣款");
        List<String> existing=jdbc.queryForList("SELECT charge_key FROM vls_compute_charge WHERE usage_id=? FOR UPDATE",String.class,usageId);
        if (!existing.isEmpty()) return existing.get(0);
        if (!"STOPPED".equals(row.get("state"))) throw new ServiceException("实例停止和用量尚未确认，禁止结算");
        if (row.get("opening_user_id")==null) throw new ServiceException("历史用量缺少可信开机用户，请人工对账，不能推断付款归属");
        ComputeBillingRate rate;
        try { rate=json.readValue(String.valueOf(row.get("rate_snapshot")),ComputeBillingRate.class); }
        catch(Exception e) { throw new ServiceException("费率快照无效"); }
        Timestamp start=(Timestamp)row.get("billing_start_at"), end=(Timestamp)row.get("billing_end_at"); rate.validate(start.toInstant());
        String payer=String.valueOf(row.get("payer_account_ref")), tenant=String.valueOf(row.get("tenant_id"));
        jdbc.update("INSERT IGNORE INTO vls_compute_billing_remainder(tenant_id,payer_account_ref,quota_unit,value_text) VALUES(?,?,?,'0')",tenant,payer,rate.getQuotaUnit());
        String tail=jdbc.queryForObject("SELECT value_text FROM vls_compute_billing_remainder WHERE tenant_id=? AND payer_account_ref=? AND quota_unit=? FOR UPDATE",String.class,tenant,payer,rate.getQuotaUnit());
        BigDecimal unrounded=rate.unroundedQuota(end.getTime()-start.getTime()), total=unrounded.add(new BigDecimal(tail));
        BigDecimal amount=total.setScale(rate.getQuotaScale(),rate.getQuotaRounding()), remainder=total.subtract(amount);
        if (amount.signum()<0) throw new ServiceException("尾差需对账，不能产生负扣款");
        String key="compute-usage-"+usageId;
        jdbc.update("INSERT INTO vls_compute_charge(charge_key,usage_id,tenant_id,payer_account_ref,quota_unit,amount_text,unrounded_text,remainder_text,state) VALUES(?,?,?,?,?,?,?,?,?)",
            key,usageId,tenant,payer,rate.getQuotaUnit(),amount.toPlainString(),unrounded.toPlainString(),remainder.toPlainString(),amount.signum()==0?"NO_DEBIT":"PENDING");
        jdbc.update("UPDATE vls_compute_billing_remainder SET value_text=? WHERE tenant_id=? AND payer_account_ref=? AND quota_unit=?",remainder.toPlainString(),tenant,payer,rate.getQuotaUnit());
        return key;
    }
    // Test/future coordinator seam only. No installed adapter calls it and no HTTP controller exposes it.
    void dispatch(String key, ComputeBillingGateway gateway) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Debit dispatch requires a committed outbox reservation");
        Map<String,Object> charge=jdbc.queryForMap("SELECT * FROM vls_compute_charge WHERE charge_key=?",key);
        String state=String.valueOf(charge.get("state")), payer=String.valueOf(charge.get("payer_account_ref")), unit=String.valueOf(charge.get("quota_unit"));
        if ("SETTLED".equals(state)||"FAILED".equals(state)||"NO_DEBIT".equals(state)) return;
        ComputeBillingGateway.Outcome outcome;
        try {
            if (!"PENDING".equals(state)) outcome=gateway.lookup(payer,key);
            else {
                BigDecimal amount=new BigDecimal(String.valueOf(charge.get("amount_text")));
                BigDecimal available=gateway.availableQuota(payer,unit);
                if (available==null) return;
                if (available.compareTo(amount)<0) { jdbc.update("UPDATE vls_compute_charge SET state='FAILED' WHERE charge_key=? AND state='PENDING'",key); return; }
                // Durable CAS precedes debit. Concurrent dispatch and process restart never resubmit an uncertain debit.
                if (jdbc.update("UPDATE vls_compute_charge SET state='SUBMITTED' WHERE charge_key=? AND state='PENDING'",key)!=1) return;
                outcome=gateway.debit(payer,unit,amount,key);
            }
        } catch(Exception e) { outcome=ComputeBillingGateway.Outcome.UNKNOWN; }
        jdbc.update("UPDATE vls_compute_charge SET state=? WHERE charge_key=? AND state IN ('SUBMITTED','UNKNOWN')",
            outcome==ComputeBillingGateway.Outcome.SUCCEEDED?"SETTLED":outcome==ComputeBillingGateway.Outcome.FAILED?"FAILED":"UNKNOWN",key);
    }
    private Map<String,Object> usage(String id) {
        return jdbc.queryForObject("SELECT * FROM vls_compute_usage WHERE id=? FOR UPDATE",(rs,index)-> {
            Map<String,Object> row=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,index);
            // Connector/J may expose DATETIME as LocalDateTime through getObject; getTimestamp honors the configured session zone.
            row.put("billing_start_at",rs.getTimestamp("billing_start_at")); row.put("billing_end_at",rs.getTimestamp("billing_end_at")); return row;
        },id);
    }
    private static void validateEvidence(Instant observed, Instant effective, String evidence) {
        if (observed==null||effective==null||effective.isAfter(observed)||!("PROVIDER_EVENT".equals(evidence)||"OBSERVATION".equals(evidence)))
            throw new ServiceException("缺少可信厂商事件或明确标记的观察时间");
        if ("OBSERVATION".equals(evidence)&&!observed.equals(effective)) throw new ServiceException("观察时间不能冒充厂商事件时间");
    }
}
