package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingSnapshot;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingTrialGuard;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingIntentService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingRecoveryService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExactQuota;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway;
import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

/** Durable product workflow. Supplier HTTP is performed only by the existing power outbox.
 * Quotes do not move funds. Acceptance commits the original payer/rate/hold before transport.
 * Repeated browser actions recover the same run; uncertain operations are never replaced. */
@Service
public class ComputePaidRunService {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private ConfiguredPaidPowerService configuredPower;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private ComputePaidArchiveRecoveryService archiveRecovery;
    @org.springframework.beans.factory.annotation.Autowired
    private ComputeBillingTrialGuard trialGuard;
    @org.springframework.beans.factory.annotation.Autowired
    private ObjectProvider<ExternalBillingGateway> externalGateways;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final ComputeNodeService nodes;
    private final ComputeBillingLedgerService ledger;
    private final ComputeBillingSettlementService settlements;
    private final ComputeBillingLifecycleService lifecycle;
    private final ComputeFundingIntentService funding;
    private final ComputeFundingRecoveryService recovery;
    private final ExternalBillingProperties properties;
    private final ComputeBillingPolicy policy;
    private final ObjectProvider<ComputePowerProvider> providers;

    public ComputePaidRunService(JdbcTemplate jdbc,PlatformTransactionManager manager,ObjectMapper json,ComputeNodeService nodes,
        ComputeBillingLedgerService ledger,ComputeBillingSettlementService settlements,ComputeBillingLifecycleService lifecycle,
        ComputeFundingIntentService funding,ComputeFundingRecoveryService recovery,ExternalBillingProperties properties,
        ComputeBillingPolicy policy,ObjectProvider<ComputePowerProvider> providers) {
        this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.json=json;this.nodes=nodes;this.ledger=ledger;
        this.settlements=settlements;this.lifecycle=lifecycle;this.funding=funding;this.recovery=recovery;
        this.properties=properties;this.policy=policy;this.providers=providers;
    }

    @Getter
    public static final class View {
        private final String runId,nodeId,state,errorCode,usageId,reserveQuota,chargedQuota,remainingQuota;
        private final String hourlyPrice,currency,quotaUnit;
        private final long quoteUntilMs,holdUntilMs;
        private final int maximumRunSeconds;
        private final boolean archiveRecovery,archiveRecoveryAvailable;
        private final String archiveRecoverySourceRunId;
        private final List<String> archiveRecoveryJobIds;
        private View(Map<String,Object> row,BillingSnapshot price,String charged,String remaining,boolean recoveryAvailable,List<String> recoveryJobs,String recoverySource) {
            runId=text(row,"run_id");nodeId=text(row,"node_id");state=text(row,"state");errorCode=text(row,"error_code");
            usageId=text(row,"usage_id");reserveQuota=text(row,"reserve_quota");chargedQuota=charged;remainingQuota=remaining;
            hourlyPrice=price.approvedHourlyPrice.toPlainString();currency=priceCurrency(price);quotaUnit=price.quotaUnit;
            quoteUntilMs=number(row,"quote_until_ms");holdUntilMs=number(row,"hold_until_ms");maximumRunSeconds=(int)number(row,"maximum_run_seconds");
            archiveRecoverySourceRunId=recoverySource;archiveRecoveryJobIds=Collections.unmodifiableList(new ArrayList<>(recoveryJobs));
            archiveRecovery=!recoveryJobs.isEmpty();archiveRecoveryAvailable=recoveryAvailable;
        }
        private static String priceCurrency(BillingSnapshot price) {
            try{return new ObjectMapper().readTree(price.json).path("accepted_rate").path("currency").asText();}
            catch(Exception invalid){throw new IllegalStateException("Invalid accepted currency");}
        }
    }

    /** No balance call here: trusted auto-grant must follow explicit account/paid-use acceptance. */
    public View quote(Long nodeId,BillingPayer payer,JsonNode config) {
        return quote(nodeId,payer,config,null);
    }

    /** Explicitly quoted new paid run for the original retained artifacts; never a restart of an old hold. */
    public View quoteArchiveRecovery(String sourceRunId,BillingPayer payer,JsonNode config) {
        Map<String,Object> context=owned(sourceRunId,payer,false);
        if(!terminal(text(context,"state")))throw new ServiceException("请先等待原开机订单停止并完成结算");
        String source=recoverySource(context);
        Map<String,Object> original=owned(source==null?sourceRunId:source,payer,false);
        if(!"COMPLETED".equals(text(original,"state")))throw new ServiceException("原归档任务的开机订单尚未结清");
        return quote(Long.valueOf(text(original,"node_id")),payer,config,text(original,"run_id"));
    }

    private View quote(Long nodeId,BillingPayer payer,JsonNode config,String recoverySource) {
        enabled();
        trial().requirePayer(payer);
        ComputeNode before=nodes.get(nodeId);
        trial().requireNode(before);
        String fixedRun=trial().requestedRunId();
        if(fixedRun!=null) {
            List<Map<String,Object>> previous=jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE run_id=?",fixedRun);
            if(!previous.isEmpty()&&!renewableUnacceptedQuote(owned(fixedRun,payer,false)))
                return trialQuoteReplay(fixedRun,nodeId,payer,recoverySource);
        }
        if(nodes.officialConfigured(before)) {
            if(configuredPower==null)throw new ServiceException("官方算力电源编排尚未就绪");
            configuredPower.refresh(before);
        }
        try{return tx.execute(status->{
            ComputeNode node=nodes.lockServer(nodeId);eligible(node,recoverySource!=null);
            trial().requireNode(node);
            Map<String,Object> renewal=null;
            if(fixedRun!=null&&!jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE run_id=? FOR UPDATE",fixedRun).isEmpty()) {
                trialQuoteReplay(fixedRun,nodeId,payer,recoverySource); // Recheck original ownership and scope under lock.
                renewal=owned(fixedRun,payer,true);
                if(!renewableUnacceptedQuote(renewal))return view(renewal);
            }
            ComputePlatformAuthorization.Context actor=actor(payer);
            String claimState=nodes.publicInventory().state(node,ComputePublicInventoryService.subject(actor));
            if(!("FREE".equals(claimState)||"MINE".equals(claimState)))throw new ServiceException("实例已被占用或工作空间隔离中");
            if(!jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key=? FOR UPDATE",node.getPublicInstanceKey()).isEmpty())
                throw new ServiceException("该实例已有付费操作，请查看原单");
            List<String> recoveryJobs=recoverySource==null?Collections.emptyList():recoverableJobs(node,owned(recoverySource,payer,true));
            if(recoverySource!=null) {
                for(Map<String,Object> prior:jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE instance_key=? AND tenant_id=? AND opening_user_id=? AND platform_subject=? AND payer_user_id=? AND state='QUOTED' AND quote_until_ms>? ORDER BY create_time FOR UPDATE",node.getPublicInstanceKey(),payer.tenantId,payer.actorId,payer.platformSubject,payer.userId,System.currentTimeMillis()))
                    if(recoverySource.equals(recoverySource(prior))&&recoveryJobs.equals(recoveryJobs(prior)))return view(prior);
            }
            JsonNode actual=config!=null&&config.has("success")?config.path("success").asBoolean()?config.path("data"):null:config;
            ComputeBillingRate configured=nodes.officialConfigured(node)?nodes.configuredFixedRate(node,actual):properties.getRates().get(String.valueOf(node.getId()));
            if(configured==null)throw new ServiceException("当前实例的有效计费价格尚未配置");
            ComputeBillingRate rate=readRate(write(configured));
            if(node.isConfigurationManaged()&&"autodl".equals(node.getProvider())&&!rate.fixedUserPrice())
                throw new ServiceException("官方算力必须使用部署配置的固定用户价格");
            if(rate.fixedUserPrice()&&(!node.isConfigurationManaged()||!java.util.Objects.equals(node.getConfigurationCode(),rate.getFixedPriceConfigurationId())))
                throw new ServiceException("官方固定价格必须绑定同一部署配置供给");
            long now=System.currentTimeMillis();
            long until=Math.addExact(now,Math.multiplyExact((long)properties.getMaximumRunSeconds()+policy.getStopBudgetSeconds()+policy.getPollingSafetySeconds(),1000L));
            rate.validate(Instant.ofEpochMilli(until));
            BillingSnapshot snapshot=BillingSnapshot.fromExistingRate(rate,actual,Instant.ofEpochMilli(now),json);
            snapshot.requireInterval(now,Math.addExact(until,1));
            ExactQuota estimate=ExactQuota.compute(snapshot.approvedHourlyPrice,until-now,snapshot.numerator,snapshot.denominator);
            BigInteger quota=estimate.numerator.add(estimate.denominator).subtract(BigInteger.ONE).divide(estimate.denominator);
            if(quota.signum()<=0||quota.compareTo(ExactQuota.integer(properties.getMaximumHoldQuota(),true))>0)
                throw new ServiceException("所需预冻结超出当前实例的已配置消费上限");
            ObjectNode acceptedSnapshot=(ObjectNode)parse(snapshot.json);
            acceptedSnapshot.putObject("run_budget").put("stop_budget_seconds",policy.getStopBudgetSeconds())
                .put("polling_safety_seconds",policy.getPollingSafetySeconds());
            if(recoverySource!=null) {
                acceptedSnapshot.put("archive_recovery_source_run_id",recoverySource);
                com.fasterxml.jackson.databind.node.ArrayNode saved=acceptedSnapshot.putArray("archive_recovery_job_ids");
                recoveryJobs.forEach(saved::add);
            }
            String trialRun=trial().quoteRunId(node,payer,acceptedSnapshot,until-now,quota);
            String run=trialRun==null?UUID.randomUUID().toString():trialRun;
            if(renewal!=null) {
                // No accepted usage or money intent exists. Refresh only this same order's quote;
                // retain its original power idempotency key and rerun every price/permit check above.
                jdbc.update("UPDATE vls_compute_paid_run SET quote_snapshot=?,reserve_quota=?,quote_until_ms=?,hold_until_ms=?,maximum_run_seconds=?,update_time=NOW() WHERE run_id=?",
                    write(acceptedSnapshot),quota.toString(),now+120000,until,properties.getMaximumRunSeconds(),run);
                return view(row(run,false));
            }
            jdbc.update("INSERT INTO vls_compute_paid_run(run_id,node_id,instance_key,tenant_id,opening_user_id,platform_subject,payer_user_id,merchant_id,quote_snapshot,reserve_quota,quote_until_ms,hold_until_ms,maximum_run_seconds,start_operation_id,state) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'QUOTED')",
                run,node.getId(),node.getPublicInstanceKey(),payer.tenantId,payer.actorId,payer.platformSubject,payer.userId,properties.getMerchantId(),write(acceptedSnapshot),quota.toString(),now+120000,until,properties.getMaximumRunSeconds(),UUID.randomUUID().toString());
            return view(row(run,false));
        });}catch(DuplicateKeyException duplicate) {
            // A racing quote may have won the fixed primary key. Never allocate a replacement.
            if(fixedRun==null)throw duplicate;
            return trialQuoteReplay(fixedRun,nodeId,payer,recoverySource);
        }
    }

    private View trialQuoteReplay(String id,Long nodeId,BillingPayer payer,String source) {
        Map<String,Object> prior=owned(id,payer,false);
        if(!nodeId.toString().equals(text(prior,"node_id"))||!java.util.Objects.equals(source,recoverySource(prior))
            ||!parse(text(prior,"quote_snapshot")).has("trial_guard"))throw new ServiceException("试验订单与原目标不一致");
        // Terminal/accepted orders remain visible and cannot create a replacement order or hold.
        return view(prior);
    }

    private boolean renewableUnacceptedQuote(Map<String,Object> run) {
        return "QUOTED".equals(text(run,"state"))&&number(run,"quote_until_ms")<=System.currentTimeMillis()
            &&text(run,"usage_id")==null&&text(run,"hold_intent_id")==null
            &&text(run,"release_intent_id")==null&&text(run,"active_instance_key")==null
            &&number(run,"worker_lease_until_ms")==0
            &&"PREPARED".equals(text(run,"power_start_state"))&&"PREPARED".equals(text(run,"power_stop_state"))
            &&run.get("power_start_attempt_ms")==null&&run.get("power_stop_attempt_ms")==null;
    }

    /** Controller checks current self + accepted wallet permissions before calling this method. */
    public View accept(String runId,BillingPayer payer) {
        enabled();
        Map<String,Object> before=owned(runId,payer,false);
        if(!"QUOTED".equals(text(before,"state")))return view(before);
        trial().requirePayer(payer);
        trial().requireNode(nodes.get(Long.valueOf(text(before,"node_id"))));
        final JsonNode freshConfig=trial().requested()||parse(text(before,"quote_snapshot")).has("trial_guard")?freshConfig():null;
        tx.execute(status->{
            Map<String,Object> initial=owned(runId,payer,false);
            ComputeNode node=nodes.lockServer(Long.valueOf(text(initial,"node_id")));
            Map<String,Object> row=owned(runId,payer,true);
            if(!"QUOTED".equals(text(row,"state")))return null;
            if(!java.util.Objects.equals(properties.getMerchantId(),text(row,"merchant_id")))throw new ServiceException("计费服务身份已变化，请重新确认报价");
            boolean archive=!recoveryJobs(row).isEmpty();
            eligible(node,archive);
            if(archive)validateRecoverySnapshot(node,row);
            if(System.currentTimeMillis()>=Math.min(number(row,"quote_until_ms"),stopDeadline(row))||!node.getPublicInstanceKey().equals(text(row,"instance_key")))
                throw new ServiceException("价格或实例状态已变化，请重新确认本次开机");
            BillingSnapshot snapshot=BillingSnapshot.read(text(row,"quote_snapshot"),json);
            ComputeBillingRate rate=readRate(parse(snapshot.json).path("accepted_rate").toString());rate.validate(Instant.now());
            if(nodes.officialConfigured(node))nodes.ensureOfficialIdentity(node);
            nodes.publicInventory().claimPaid(node,actor(payer));
            jdbc.update("UPDATE vls_compute_paid_run SET active_instance_key=? WHERE run_id=?",node.getPublicInstanceKey(),runId);
            String usage=ledger.reserve(ComputeBillingLedgerService.serverKey(node.getId()),payer.tenantId,payer.actorId,payer.platformSubject,rate,false,text(row,"start_operation_id"));
            settlements.bindReserved(usage,payer,snapshot);lifecycle.arm(usage,payer);
            jdbc.update("UPDATE vls_compute_paid_run SET usage_id=?,state='FUNDING',update_time=NOW() WHERE run_id=?",usage,runId);
            trial().requireNewUsage(usage,freshConfig);
            ComputeFundingIntentService.Intent hold=funding.prepareInitialHold(usage,text(row,"reserve_quota"),Long.toString(number(row,"hold_until_ms")),"GPU_COMPUTE");
            jdbc.update("UPDATE vls_compute_paid_run SET usage_id=?,hold_intent_id=?,state='FUNDING',update_time=NOW() WHERE run_id=?",usage,hold.id,runId);
            return null;
        });
        return get(runId,payer);
    }

    public View get(String id,BillingPayer payer){return view(owned(id,payer,false));}
    public List<View> list(BillingPayer payer) {
        actor(payer);
        java.util.LinkedHashMap<String,Map<String,Object>> visible=new java.util.LinkedHashMap<>();
        for(Map<String,Object> row:jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE tenant_id=? AND opening_user_id=? AND platform_subject=? AND payer_user_id=? ORDER BY create_time DESC LIMIT 20",payer.tenantId,payer.actorId,payer.platformSubject,payer.userId))visible.put(text(row,"run_id"),row);
        // Retain bounded current-user work independently of recent quote/cancellation history.
        for(Map<String,Object> row:jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE tenant_id=? AND opening_user_id=? AND platform_subject=? AND payer_user_id=? AND active_instance_key IS NOT NULL ORDER BY create_time DESC LIMIT 100",payer.tenantId,payer.actorId,payer.platformSubject,payer.userId))visible.put(text(row,"run_id"),row);
        if(archiveRecovery!=null) {
            String owner=com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash("trusted-platform-user\n"+payer.platformSubject);
            for(Map<String,Object> job:jdbc.queryForList("SELECT id,public_instance_key,options_json FROM vls_cloud_training_job WHERE tenant_id=? AND public_owner_subject=? AND job_state='ARCHIVE_PENDING' ORDER BY update_time DESC,id LIMIT 100",payer.tenantId,owner))try {
                JsonNode checkpoint=parse(text(job,"options_json")).path("archiveRecovery");
                String source=checkpoint.path("originalRunId").asText();
                if(!source.matches("[0-9a-f-]{36}"))continue;
                Map<String,Object> original=owned(source,payer,false);
                if(text(original,"instance_key").equals(text(job,"public_instance_key"))&&text(original,"usage_id").equals(checkpoint.path("originalUsageId").asText()))visible.put(source,original);
            }catch(RuntimeException unverifiable){/* A malformed/foreign checkpoint never widens the current-payer list. */}
        }
        return visible.values().stream().map(this::view).collect(java.util.stream.Collectors.toList());
    }
    public View stop(String id,BillingPayer payer) {
        tx.execute(status->{
            Map<String,Object> row=owned(id,payer,true);String state=text(row,"state");
            if("QUOTED".equals(state)){jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLED' WHERE run_id=?",id);return null;}
            if(terminal(state))return null;
            jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",text(row,"usage_id"));
            if("FUNDING".equals(state)) {
                if(!cancelUnsubmittedHold(row))jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLING' WHERE run_id=?",id);
            }
            return null;
        });return get(id,payer);
    }

    public boolean trainingAvailable(ComputeNode node) {
        try{requireTraining(node);return true;}catch(RuntimeException unavailable){return false;}
    }
    public void requireTraining(ComputeNode node) {
        enabled();ComputePlatformAuthorization.Context actor=nodes.publicInventory().identity();
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE active_instance_key=? AND state='RUNNING'",node.getPublicInstanceKey());
        if(rows.size()!=1)throw new ServiceException("请先完成付费开机，并等待余额与实例状态确认");
        Map<String,Object> run=rows.get(0);
        if(!recoveryJobs(run).isEmpty())throw new ServiceException("本次开机仅恢复原任务归档，不接受新训练");
        if(!java.util.Objects.equals(properties.getMerchantId(),text(run,"merchant_id")))throw new ServiceException("计费服务身份已变化，暂不能开始新训练");
        if(!actor.getPlatformUserId().equals(text(run,"platform_subject"))||!actor.getVlsTenantId().equals(text(run,"tenant_id"))||!actor.getVlsUserId().equals(text(run,"opening_user_id")))
            throw new ServiceException("训练必须使用本次开机付款账户与租户");
        if(System.currentTimeMillis()>=stopDeadline(run))throw new ServiceException("本次开机的绝对运行预算已到期，请等待原单安全停止与结算");
        requireTrialUsage(text(run,"usage_id"));
        settlements.requirePaidAdmission(text(run,"usage_id"));
        if(nodes.officialConfigured(node))nodes.requireOfficialConnection(node);
    }

    @Scheduled(fixedDelay=5000,initialDelay=30000)
    public void scan() {
        if(!configured())return;
        for(String run:jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key IS NOT NULL AND worker_lease_until_ms<? ORDER BY update_time LIMIT 50",String.class,System.currentTimeMillis()))tick(run);
    }
    public void tick(String id) {
        if(!configured())return;
        // Scope selection survives expiry/disable so the original run can still stop/clean up.
        // It must precede the worker lease and every funding/provider recovery side effect.
        if(!trial().selectsRun(id))return;
        long now=System.currentTimeMillis(),leaseUntil=now+30000;
        if(jdbc.update("UPDATE vls_compute_paid_run SET worker_lease_until_ms=? WHERE run_id=? AND active_instance_key IS NOT NULL AND worker_lease_until_ms<?",leaseUntil,id,now)!=1)return;
        try{advance(row(id,false));}
        catch(RuntimeException uncertain){jdbc.update("UPDATE vls_compute_paid_run SET error_code='RECOVERY_PENDING',update_time=NOW() WHERE run_id=?",id);}
        finally{jdbc.update("UPDATE vls_compute_paid_run SET worker_lease_until_ms=0 WHERE run_id=? AND worker_lease_until_ms=?",id,leaseUntil);}
    }
    private void advance(Map<String,Object> run) {
        String id=text(run,"run_id"),usage=text(run,"usage_id"),state=text(run,"state");
        String holdId=text(run,"hold_intent_id");
        if("FUNDING".equals(state)||"CANCELLING".equals(state)) {
            Map<String,Object> pending=jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?",usage);
            boolean expired=System.currentTimeMillis()>=stopDeadline(run);
            if(expired)jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=COALESCE(risk_code,'RUN_WINDOW_EXPIRED') WHERE usage_id=?",usage);
            // Cancel a provably unsent hold before recovery could claim it for transport.
            if((expired||"CANCELLING".equals(state)||number(pending,"stop_requested")!=0)&&cancelUnsubmittedHold(run))return;
        }
        ComputeFundingIntentService.Intent hold=recovery.advance(holdId,System.currentTimeMillis());
        if(ComputeFundingIntentService.REJECTED.equals(hold.state)&&hold.holdId==null&&hold.receipt==null) {
            tx.execute(status->{
                ledger.startFailed(usage);
                jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLED',active_instance_key=NULL,error_code='FUNDING_REJECTED' WHERE run_id=?",id);return null;
            });return;
        }
        if(ComputeFundingIntentService.CONFLICT.equals(hold.state)||ComputeFundingIntentService.REJECTED.equals(hold.state)) {
            jdbc.update("UPDATE vls_compute_paid_run SET error_code='FUNDING_REQUIRES_REVIEW' WHERE run_id=?",id);return;
        }
        Map<String,Object> session=jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?",usage);
        Map<String,Object> observedUsage=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=?",usage);
        boolean budgetExpired=System.currentTimeMillis()>=stopDeadline(run);
        if(budgetExpired&&("FUNDING".equals(state)||"STARTING".equals(state)||"RUNNING".equals(state))) {
            jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=CASE WHEN risk_code IS NULL THEN 'RUN_WINDOW_EXPIRED' ELSE risk_code END WHERE usage_id=?",usage);
            session.put("stop_requested",1);
        }
        if(configuredPower!=null&&("STARTING".equals(state)||"RUNNING".equals(state)||"STOPPING".equals(state))) {
            ComputeNode controlled=lifecycle.controlledNode(usage);
            if(nodes.officialConfigured(controlled)&&(!"STARTING".equals(state)||number(session,"stop_requested")==0||!"PREPARED".equals(text(run,"power_start_state")))) {
                configuredPower.advance(run,controlled);
                run=row(id,false);state=text(run,"state");
                session=jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?",usage);
                observedUsage=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=?",usage);
            }
        }
        if("FUNDING".equals(state)&&number(session,"stop_requested")!=0) {
            jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLING' WHERE run_id=?",id);
            state="CANCELLING";
        }
        if("STARTING".equals(state)&&number(session,"stop_requested")!=0&&observedUsage.get("billing_start_at")==null&&!"START_FAILED".equals(text(observedUsage,"state"))) {
            if(cancelUnsubmittedStart(run))state="CANCELLING";
            else {
                jdbc.update("UPDATE vls_compute_paid_run SET error_code='START_OUTCOME_UNKNOWN' WHERE run_id=?",id);
                lifecycle.requestSafeStop(usage);
                // A possibly submitted start is not a cancellation/no-debit proof.
                // Keep its original outbox and hold until observation resolves it.
                return;
            }
        }
        if("FUNDING".equals(state)&&number(session,"stop_requested")==0) {
            if(!ComputeFundingIntentService.DETAIL_OBSERVED.equals(hold.state))return;
            final JsonNode currentConfig;
            try {currentConfig=requireTrialUsage(usage);}
            catch(RuntimeException denied) {
                jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",usage);
                jdbc.update("UPDATE vls_compute_paid_run SET error_code='TRIAL_GUARD_BLOCKED' WHERE run_id=?",id);
                return;
            }
            tx.execute(status->{
                ComputeNode node=lifecycle.controlledNode(usage);
                Map<String,Object> current=row(id,true);
                if(!"FUNDING".equals(text(current,"state")))return null;
                if(System.currentTimeMillis()>=stopDeadline(current))throw new ServiceException("本次开机预算已到期，等待原冻结释放");
                trial().requireNewUsage(usage,currentConfig);
                settlements.requirePaidAdmission(usage);
                if(nodes.serverRows(node).stream().anyMatch(n->n.getPowerOperationId()!=null))throw new ServiceException("实例已有待确认操作");
                if(recoveryJobs(current).isEmpty())nodes.assertNoActiveJobs(node.getId());else validateRecoverySnapshot(node,current);
                if(!"STOPPED".equals(node.getPowerState())||!node.getPublicInstanceKey().equals(text(current,"instance_key")))throw new ServiceException("实例状态已变化");
                queueStart(node,text(current,"start_operation_id"));
                jdbc.update("UPDATE vls_compute_paid_run SET state='STARTING',error_code=NULL,update_time=NOW() WHERE run_id=?",id);return null;
            });return;
        }
        if(observedUsage.get("billing_start_at")!=null&&observedUsage.get("billing_end_at")==null) {
            jdbc.update("UPDATE vls_compute_paid_run SET state='RUNNING',error_code=NULL WHERE run_id=? AND state='STARTING'",id);
            if(!recoveryJobs(run).isEmpty()&&recoveryAttemptFinished(run)) {
                jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",usage);
                session.put("stop_requested",1);
            }
            long deadline=Math.min(stopDeadline(run),Math.addExact(((Timestamp)observedUsage.get("billing_start_at")).getTime(),Math.multiplyExact(number(run,"maximum_run_seconds"),1000L)));
            if(System.currentTimeMillis()>=deadline)jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",usage);
            if(number(session,"stop_requested")!=0||System.currentTimeMillis()>=deadline)lifecycle.requestSafeStop(usage);
            return;
        }
        boolean neverStarted=("CANCELLING".equals(state)||"START_FAILED".equals(text(observedUsage,"state")))&&observedUsage.get("billing_start_at")==null;
        boolean stopped="STOP_CONFIRMED".equals(text(session,"safety_state"))&&observedUsage.get("billing_end_at")!=null;
        if(!neverStarted&&!stopped)return;
        // Cancellation is not proof that an uncertain hold never committed. Keep the usage
        // claim until the original funding outcome and any required release are confirmed.
        if(stopped&&(number(session,"metered_until_ms")<number(session,"last_running_ms")
            ||!jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? AND state NOT IN ('SETTLED','NO_DEBIT')"
                +" AND (state<>'BLOCKED' OR error_code IS NULL OR error_code NOT IN ('TRIAL_UNALLOCATED_BUDGET','TRIAL_UNALLOCATED_PERMIT')"
                +" OR charge_id IS NOT NULL OR ledger_entry_id IS NOT NULL OR lease_token IS NOT NULL OR lease_until_ms IS NULL OR lease_until_ms<>0)",usage).isEmpty()))return;
        // The two exact unallocated markers prove that the sealed interval never acquired
        // permission to send. Preserve its amount/error/state; release is not a fake settlement.
        // Every unknown/submitted slice, or any residual send/charge evidence, still blocks here.
        if(!ComputeFundingIntentService.DETAIL_OBSERVED.equals(hold.state))return;
        if(neverStarted)jdbc.update("UPDATE vls_compute_usage SET state='START_FAILED' WHERE id=? AND billing_start_at IS NULL AND state IN ('RESERVED','RECONCILE')",usage);
        // Preserve active_server_key while the confirmed hold's original release is unresolved.
        // START_FAILED carries no-power evidence across RELEASING; it does not free the claim.
        // A zero aggregate remaining balance is not evidence that this release completed.
        // Recover its original receipt before freeing the run, even when it already applied.
        if(run.get("release_intent_id")!=null) {
            ComputeFundingIntentService.Intent release=funding.recover(text(run,"release_intent_id"));
            if(!usage.equals(release.usageId)||!"RELEASE".equals(release.operation)||!hold.id.equals(release.parentId))
                throw new IllegalStateException("Final release does not belong to this paid run");
            release=recovery.advance(release.id,System.currentTimeMillis());
            if(!ComputeFundingIntentService.OPERATION_CONFIRMED.equals(release.state)) {
                jdbc.update("UPDATE vls_compute_paid_run SET state='RELEASING',error_code='RELEASE_RECEIPT_PENDING' WHERE run_id=?",id);
                return;
            }
        }
        Map<String,Object> detail=jdbc.queryForMap("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",hold.id);
        String remaining=text(detail,"remaining_quota");
        if(!"0".equals(remaining)) {
            if(run.get("release_intent_id")!=null) {
                // A later refund can return funds to this hold. It needs its own refund-release
                // reference; the old final-release must never become a new release operation.
                jdbc.update("UPDATE vls_compute_paid_run SET state='RELEASING',error_code='REMAINING_AFTER_CONFIRMED_RELEASE' WHERE run_id=?",id);
                return;
            }
            ComputeFundingIntentService.Intent release=funding.prepareFinalRelease(hold.id,remaining);
            jdbc.update("UPDATE vls_compute_paid_run SET release_intent_id=?,state='RELEASING' WHERE run_id=?",release.id,id);
            recovery.advance(release.id,System.currentTimeMillis());return;
        }
        tx.execute(status->{
            jdbc.update("UPDATE vls_compute_paid_run SET state=?,active_instance_key=NULL,error_code=NULL,update_time=NOW() WHERE run_id=?",neverStarted?"CANCELLED":"COMPLETED",id);
            jdbc.update("UPDATE vls_compute_usage SET active_server_key=NULL,state=CASE WHEN billing_start_at IS NULL THEN 'START_FAILED' ELSE state END WHERE id=?",usage);
            return null;
        });
    }
    private long stopDeadline(Map<String,Object> run) {
        long margin=Math.addExact((long)policy.getStopBudgetSeconds(),policy.getPollingSafetySeconds());
        JsonNode saved=parse(text(run,"quote_snapshot")).path("run_budget");
        if(!saved.isMissingNode()) {
            JsonNode stop=saved.path("stop_budget_seconds"),poll=saved.path("polling_safety_seconds");
            if(!stop.canConvertToInt()||!poll.canConvertToInt()||!stop.isIntegralNumber()||!poll.isIntegralNumber()||stop.asInt()<1||poll.asInt()<1)
                throw new IllegalStateException("Invalid accepted run budget");
            // A later local setting cannot silently reduce the accepted safety margin.
            margin=Math.max(margin,Math.addExact(stop.longValue(),poll.longValue()));
        }
        return Math.subtractExact(number(run,"hold_until_ms"),Math.multiplyExact(margin,1000L));
    }
    private boolean cancelUnsubmittedHold(Map<String,Object> run) {
        return Boolean.TRUE.equals(tx.execute(status->{
            Map<String,Object> current=row(text(run,"run_id"),true);
            if(!("FUNDING".equals(text(current,"state"))||"CANCELLING".equals(text(current,"state"))))return false;
            String usage=text(current,"usage_id");
            Map<String,Object> observed=jdbc.queryForMap("SELECT billing_start_at FROM vls_compute_usage WHERE id=? FOR UPDATE",usage);
            if(observed.get("billing_start_at")!=null)return false;
            int cancelled=jdbc.update("UPDATE vls_compute_funding_intent SET state='REJECTED',error_code='CANCELLED_BEFORE_SEND'"
                +" WHERE intent_id=? AND usage_id=? AND operation_kind='HOLD' AND state='PREPARED'"
                +" AND hold_id IS NULL AND receipt_json IS NULL AND conflicting_receipt_json IS NULL",text(current,"hold_intent_id"),usage);
            if(cancelled!=1)return false;
            ledger.startFailed(usage);
            jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLED',active_instance_key=NULL,error_code='CANCELLED_BEFORE_SEND',update_time=NOW() WHERE run_id=?",text(current,"run_id"));
            return true;
        }));
    }
    private boolean cancelUnsubmittedStart(Map<String,Object> run) {
        return Boolean.TRUE.equals(tx.execute(status->{
            String usage=text(run,"usage_id");ComputeNode node=lifecycle.controlledNode(usage);
            if(nodes.officialConfigured(node)) {
                if(configuredPower==null||!configuredPower.cancelUnsubmitted(run,node))return false;
                ledger.startFailed(usage);jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLING',error_code='START_CANCELLED_BEFORE_SEND' WHERE run_id=?",text(run,"run_id"));return true;
            }
            if(!text(run,"start_operation_id").equals(node.getPowerOperationId())||!"start".equals(node.getPowerOperationAction())||node.isPowerOperationSubmitted())return false;
            int changed=jdbc.update("UPDATE vls_compute_node SET power_operation_id=NULL,power_operation_action=NULL,power_operation_started_at=NULL,power_operation_submitted=0 WHERE id=? AND power_operation_id=? AND power_operation_submitted=0",node.getId(),text(run,"start_operation_id"));
            if(changed!=1)return false;
            jdbc.update("UPDATE vls_compute_node SET power_state='UNKNOWN',power_checked_at=NULL,probe_state='UNTESTED',probe_time=NULL,lifecycle_message='本次开机未向厂商发送，已取消；需刷新确认实例状态' WHERE "+ComputeNodeService.SERVER_WHERE,ComputeNodeService.serverArgs(node));
            ledger.startFailed(usage);
            jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLING',error_code='START_CANCELLED_BEFORE_SEND' WHERE run_id=?",text(run,"run_id"));
            return true;
        }));
    }
    private void queueStart(ComputeNode node,String operation) {
        if(!providers.orderedStream().anyMatch(p->p.supports(node)))throw new ServiceException("厂商开机接口不可用");
        if(nodes.officialConfigured(node))return; // STARTING on the original paid run is the committed outbox.
        jdbc.update("UPDATE vls_compute_node SET power_state='STARTING',probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE "+ComputeNodeService.SERVER_WHERE,ComputeNodeService.serverArgs(node));
        jdbc.update("UPDATE vls_compute_node SET power_operation_id=?,power_operation_action='start',power_operation_started_at=NOW(),power_operation_submitted=0,lifecycle_message='预冻结已确认，等待厂商开机' WHERE id=?",operation,node.getId());
    }
    private void eligible(ComputeNode node) {
        eligible(node,false);
    }
    private void eligible(ComputeNode node,boolean archive) {
        if(!node.isEnabled()||!"PLATFORM".equals(node.getResourceScope())||!"POINTS".equals(node.getBillingType())||node.getPublicInstanceKey()==null
            ||!"API".equals(node.getControlMode())||nodes.isCompanyShared(node))throw new ServiceException("当前资源不支持个人付费开机");
        if(!node.getId().equals(nodes.serverRows(node).get(0).getId()))throw new ServiceException("请选择实例的主资源记录");
        ComputeBillingPolicy.requireEconomicalPaidOffer(node,System.currentTimeMillis());
        if(!providers.orderedStream().anyMatch(p->p.supports(node)))throw new ServiceException("当前厂商控制未启用");
        if(nodes.serverRows(node).stream().anyMatch(n->n.getPowerOperationId()!=null))throw new ServiceException("请先等待原实例操作完成");
        if(!archive)nodes.assertNoActiveJobs(node.getId());
    }
    private boolean configured(){return properties.isEnabled()&&properties.isAllowWrites()&&properties.isSafetyStopEnabled()&&properties.isPaidAdmissionEnabled();}
    private ComputeBillingTrialGuard trial(){return trialGuard==null?new ComputeBillingTrialGuard(jdbc,json,properties):trialGuard;}
    private JsonNode freshConfig() {
        ExternalBillingGateway gateway=externalGateways==null?null:externalGateways.getIfAvailable();
        if(gateway==null)throw new ServiceException("试验计费配置无法确认，禁止新增冻结、开机或训练");
        return gateway.config();
    }
    private JsonNode requireTrialUsage(String usage) {
        if(!trial().protectedUsage(usage))return null;
        JsonNode current=freshConfig();trial().requireNewUsage(usage,current);return current;
    }
    private void enabled(){
        if(!configured())throw new ServiceException("当前环境尚未开放付费开机");
        policy.checkedStopBudgetSeconds(properties.getVerifiedProviderStopSeconds());
        if(properties.getHoldStopLeadSeconds()<policy.getStopBudgetSeconds())throw new ServiceException("余额到期停机提前量未覆盖可靠停机预算");
        if(properties.getMaximumRunSeconds()<1||properties.getMaximumRunSeconds()>86400)throw new ServiceException("当前环境尚未配置单次运行时限");
        ExactQuota.integer(properties.getMaximumHoldQuota(),true);
    }
    private ComputePlatformAuthorization.Context actor(BillingPayer payer) {
        ComputePlatformAuthorization.Context actor=nodes.publicInventory().identity();
        if(payer==null||!actor.getPlatformUserId().equals(payer.platformSubject)||!actor.getVlsTenantId().equals(payer.tenantId)||!actor.getVlsUserId().equals(payer.actorId))
            throw new ServiceException("当前付款身份已变化");return actor;
    }
    private Map<String,Object> owned(String id,BillingPayer payer,boolean lock) {
        actor(payer);Map<String,Object> row=row(id,lock);
        if(!payer.userId.equals(text(row,"payer_user_id"))||!payer.platformSubject.equals(text(row,"platform_subject"))||!payer.tenantId.equals(text(row,"tenant_id"))||!payer.actorId.equals(text(row,"opening_user_id")))
            throw new ServiceException("付费操作不存在或无权访问");return row;
    }
    private Map<String,Object> row(String id,boolean lock){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE run_id=?"+(lock?" FOR UPDATE":""),id);
        if(rows.isEmpty())throw new ServiceException("付费操作不存在或无权访问");return rows.get(0);
    }
    private View view(Map<String,Object> row) {
        BigInteger charged=BigInteger.ZERO;String remaining=null;
        if(row.get("usage_id")!=null) {
            for(String amount:jdbc.queryForList("SELECT amount_text FROM vls_compute_billing_slice WHERE usage_id=? AND state='SETTLED'",String.class,text(row,"usage_id")))charged=charged.add(ExactQuota.integer(amount,false));
            List<String> observations=jdbc.queryForList("SELECT remaining_quota FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",String.class,text(row,"hold_intent_id"));
            if(!observations.isEmpty())remaining=observations.get(0);
        }
        List<String> jobs=recoveryJobs(row);
        boolean available=false;
        if(archiveRecovery!=null&&terminal(text(row,"state")))try {
            String source=recoverySource(row);Map<String,Object> original=source==null?row:row(source,false);
            if(samePayer(row,original)&&"COMPLETED".equals(text(original,"state"))
                &&jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key=?",text(original,"instance_key")).isEmpty())
                available=!recoverableJobs(nodes.get(Long.valueOf(text(original,"node_id"))),original).isEmpty();
        }catch(RuntimeException unavailable){available=false;}
        return new View(row,BillingSnapshot.read(text(row,"quote_snapshot"),json),charged.toString(),remaining,available,jobs,recoverySource(row));
    }

    private List<String> recoverableJobs(ComputeNode node,Map<String,Object> source) {
        if(archiveRecovery==null||!nodes.officialConfigured(node)||!"COMPLETED".equals(text(source,"state"))
            ||!node.getPublicInstanceKey().equals(text(source,"instance_key")))throw new ServiceException("仅支持同一官方实例原任务的归档恢复");
        ComputePublicInventoryService.Instance claim=nodes.publicInventory().read(node,true);
        String owner=com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash("trusted-platform-user\n"+text(source,"platform_subject"));
        if(claim==null||!"OCCUPIED".equals(claim.getPublicState())||!owner.equals(claim.getOwnerSubject()))
            throw new ServiceException("原任务实例占用身份已变化，不能恢复");
        List<String> jobs=new ArrayList<>(archiveRecovery.requireRecoverableJobs(node,nodes.queueIds(node.getId()),text(source,"tenant_id"),owner,claim.getClaimId(),String.valueOf(nodes.configurationSnapshot(node).get("targetFingerprint"))));
        Collections.sort(jobs);
        if(jobs.isEmpty()||new java.util.HashSet<>(jobs).size()!=jobs.size())throw new ServiceException("没有可恢复的原归档任务");
        for(String job:jobs) {
            JsonNode proof=parse(jdbc.queryForObject("SELECT options_json FROM vls_cloud_training_job WHERE id=?",String.class,job)).path("archiveRecovery");
            if(!text(source,"run_id").equals(proof.path("originalRunId").asText())||!text(source,"usage_id").equals(proof.path("originalUsageId").asText()))
                throw new ServiceException("归档恢复证明不属于原开机订单");
        }
        return jobs;
    }
    private void validateRecoverySnapshot(ComputeNode node,Map<String,Object> run) {
        String source=recoverySource(run);
        if(source==null)throw new ServiceException("归档恢复原单证明缺失");
        Map<String,Object> original=row(source,true);
        if(!samePayer(run,original)||!recoveryJobs(run).equals(recoverableJobs(node,original)))
            throw new ServiceException("归档任务或付款身份已变化，请重新确认恢复报价");
    }
    private boolean samePayer(Map<String,Object> a,Map<String,Object> b) {
        for(String field:new String[]{"tenant_id","opening_user_id","platform_subject","payer_user_id","merchant_id","instance_key"})
            if(!java.util.Objects.equals(text(a,field),text(b,field)))return false;
        return true;
    }
    private List<String> recoveryJobs(Map<String,Object> run) {
        JsonNode snapshot=parse(text(run,"quote_snapshot")),jobs=snapshot.path("archive_recovery_job_ids");
        if(jobs.isMissingNode()&&!snapshot.has("archive_recovery_source_run_id"))return Collections.emptyList();
        if(!jobs.isArray()||jobs.size()==0||!snapshot.path("archive_recovery_source_run_id").isTextual())throw new ServiceException("归档恢复报价快照无效");
        List<String> result=new ArrayList<>();for(JsonNode job:jobs){if(!job.isTextual()||!job.asText().matches("[A-Za-z0-9-]{1,64}"))throw new ServiceException("归档任务标识无效");result.add(job.asText());}
        Collections.sort(result);if(new java.util.HashSet<>(result).size()!=result.size())throw new ServiceException("归档任务重复");return result;
    }
    private String recoverySource(Map<String,Object> run) {
        JsonNode source=parse(text(run,"quote_snapshot")).path("archive_recovery_source_run_id");
        return source.isMissingNode()?null:source.asText();
    }
    private boolean recoveryAttemptFinished(Map<String,Object> run) {
        for(String id:recoveryJobs(run)) {
            Map<String,Object> job=jdbc.queryForMap("SELECT job_state,options_json FROM vls_cloud_training_job WHERE id=?",id);
            String state=text(job,"job_state");
            if("COMPLETED".equals(state)||"FAILED".equals(state)||"CANCELLED".equals(state))continue;
            JsonNode attempt=parse(text(job,"options_json")).path("archiveAttempt");
            if(!("ARCHIVE_PENDING".equals(state)&&text(run,"run_id").equals(attempt.path("runId").asText())&&attempt.path("paused").asBoolean(false)))return false;
        }
        return true;
    }
    private boolean terminal(String state){return "CANCELLED".equals(state)||"COMPLETED".equals(state);}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception invalid){throw new ServiceException("价格快照无效");}}
    private JsonNode parse(String value){try{return json.readTree(value);}catch(Exception invalid){throw new ServiceException("价格快照无效");}}
    private ComputeBillingRate readRate(String value){try{return json.readValue(value,ComputeBillingRate.class);}catch(Exception invalid){throw new ServiceException("价格快照无效");}}
    private static String text(Map<String,Object> row,String field){Object value=row.get(field);return value==null?null:String.valueOf(value);}
    private static long number(Map<String,Object> row,String field){return ExactQuota.integer(text(row,field),false).longValueExact();}
}
