package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingSnapshot;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingIntentService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingRecoveryService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExactQuota;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
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

/** Durable product workflow. Supplier HTTP is performed only by the existing power outbox.
 * Quotes do not move funds. Acceptance commits the original payer/rate/hold before transport.
 * Repeated browser actions recover the same run; uncertain operations are never replaced. */
@Service
public class ComputePaidRunService {
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
        private View(Map<String,Object> row,BillingSnapshot price,String charged,String remaining) {
            runId=text(row,"run_id");nodeId=text(row,"node_id");state=text(row,"state");errorCode=text(row,"error_code");
            usageId=text(row,"usage_id");reserveQuota=text(row,"reserve_quota");chargedQuota=charged;remainingQuota=remaining;
            hourlyPrice=price.approvedHourlyPrice.toPlainString();currency=priceCurrency(price);quotaUnit=price.quotaUnit;
            quoteUntilMs=number(row,"quote_until_ms");holdUntilMs=number(row,"hold_until_ms");maximumRunSeconds=(int)number(row,"maximum_run_seconds");
        }
        private static String priceCurrency(BillingSnapshot price) {
            try{return new ObjectMapper().readTree(price.json).path("accepted_rate").path("currency").asText();}
            catch(Exception invalid){throw new IllegalStateException("Invalid accepted currency");}
        }
    }

    /** No balance call here: trusted auto-grant must follow explicit account/paid-use acceptance. */
    public View quote(Long nodeId,BillingPayer payer,JsonNode config) {
        enabled();
        return tx.execute(status->{
            ComputeNode node=nodes.lockServer(nodeId);eligible(node);
            ComputePlatformAuthorization.Context actor=actor(payer);
            String claimState=nodes.publicInventory().state(node,ComputePublicInventoryService.subject(actor));
            if(!("FREE".equals(claimState)||"MINE".equals(claimState)))throw new ServiceException("实例已被占用或工作空间隔离中");
            if(!jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key=? FOR UPDATE",node.getPublicInstanceKey()).isEmpty())
                throw new ServiceException("该实例已有付费操作，请查看原单");
            ComputeBillingRate configured=properties.getRates().get(String.valueOf(node.getId()));
            if(configured==null)throw new ServiceException("当前实例的有效计费价格尚未配置");
            ComputeBillingRate rate=readRate(write(configured));
            long now=System.currentTimeMillis();
            long until=Math.addExact(now,Math.multiplyExact((long)properties.getMaximumRunSeconds()+policy.getStopBudgetSeconds()+policy.getPollingSafetySeconds(),1000L));
            rate.validate(Instant.ofEpochMilli(until));
            JsonNode actual=config!=null&&config.has("success")?config.path("success").asBoolean()?config.path("data"):null:config;
            BillingSnapshot snapshot=BillingSnapshot.fromExistingRate(rate,actual,Instant.ofEpochMilli(now),json);
            snapshot.requireInterval(now,Math.addExact(until,1));
            ExactQuota estimate=ExactQuota.compute(snapshot.approvedHourlyPrice,until-now,snapshot.numerator,snapshot.denominator);
            BigInteger quota=estimate.numerator.add(estimate.denominator).subtract(BigInteger.ONE).divide(estimate.denominator);
            if(quota.signum()<=0||quota.compareTo(ExactQuota.integer(properties.getMaximumHoldQuota(),true))>0)
                throw new ServiceException("所需预冻结超出当前实例的已配置消费上限");
            String run=UUID.randomUUID().toString();
            jdbc.update("INSERT INTO vls_compute_paid_run(run_id,node_id,instance_key,tenant_id,opening_user_id,platform_subject,payer_user_id,merchant_id,quote_snapshot,reserve_quota,quote_until_ms,hold_until_ms,maximum_run_seconds,start_operation_id,state) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'QUOTED')",
                run,node.getId(),node.getPublicInstanceKey(),payer.tenantId,payer.actorId,payer.platformSubject,payer.userId,properties.getMerchantId(),snapshot.json,quota.toString(),now+120000,until,properties.getMaximumRunSeconds(),UUID.randomUUID().toString());
            return view(row(run,false));
        });
    }

    /** Controller checks current self + accepted wallet permissions before calling this method. */
    public View accept(String runId,BillingPayer payer) {
        enabled();
        tx.execute(status->{
            Map<String,Object> initial=owned(runId,payer,false);
            ComputeNode node=nodes.lockServer(Long.valueOf(text(initial,"node_id")));
            Map<String,Object> row=owned(runId,payer,true);
            if(!"QUOTED".equals(text(row,"state")))return null;
            if(!java.util.Objects.equals(properties.getMerchantId(),text(row,"merchant_id")))throw new ServiceException("计费服务身份已变化，请重新确认报价");
            eligible(node);
            if(System.currentTimeMillis()>=number(row,"quote_until_ms")||!node.getPublicInstanceKey().equals(text(row,"instance_key")))
                throw new ServiceException("价格或实例状态已变化，请重新确认本次开机");
            BillingSnapshot snapshot=BillingSnapshot.read(text(row,"quote_snapshot"),json);
            ComputeBillingRate rate=readRate(parse(snapshot.json).path("accepted_rate").toString());rate.validate(Instant.now());
            nodes.publicInventory().claimPaid(node,actor(payer));
            jdbc.update("UPDATE vls_compute_paid_run SET active_instance_key=? WHERE run_id=?",node.getPublicInstanceKey(),runId);
            String usage=ledger.reserve(ComputeBillingLedgerService.serverKey(node.getId()),payer.tenantId,payer.actorId,payer.platformSubject,rate,false,text(row,"start_operation_id"));
            settlements.bindReserved(usage,payer,snapshot);lifecycle.arm(usage,payer);
            ComputeFundingIntentService.Intent hold=funding.prepareInitialHold(usage,text(row,"reserve_quota"),Long.toString(number(row,"hold_until_ms")),"GPU_COMPUTE");
            jdbc.update("UPDATE vls_compute_paid_run SET usage_id=?,hold_intent_id=?,state='FUNDING',update_time=NOW() WHERE run_id=?",usage,hold.id,runId);
            return null;
        });
        return get(runId,payer);
    }

    public View get(String id,BillingPayer payer){return view(owned(id,payer,false));}
    public List<View> list(BillingPayer payer) {
        actor(payer);
        return jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE tenant_id=? AND opening_user_id=? AND platform_subject=? AND payer_user_id=? ORDER BY create_time DESC LIMIT 20",payer.tenantId,payer.actorId,payer.platformSubject,payer.userId)
            .stream().map(this::view).collect(java.util.stream.Collectors.toList());
    }
    public View stop(String id,BillingPayer payer) {
        tx.execute(status->{
            Map<String,Object> row=owned(id,payer,true);String state=text(row,"state");
            if("QUOTED".equals(state)){jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLED' WHERE run_id=?",id);return null;}
            if(terminal(state))return null;
            jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",text(row,"usage_id"));
            if("FUNDING".equals(state)) {
                int cancelled=jdbc.update("UPDATE vls_compute_funding_intent SET state='REJECTED',error_code='CANCELLED_BEFORE_SEND' WHERE intent_id=? AND state='PREPARED'",text(row,"hold_intent_id"));
                if(cancelled==1) {
                    ledger.startFailed(text(row,"usage_id"));
                    jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLED',active_instance_key=NULL WHERE run_id=?",id);
                } else jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLING' WHERE run_id=?",id);
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
        if(!java.util.Objects.equals(properties.getMerchantId(),text(run,"merchant_id")))throw new ServiceException("计费服务身份已变化，暂不能开始新训练");
        if(!actor.getPlatformUserId().equals(text(run,"platform_subject"))||!actor.getVlsTenantId().equals(text(run,"tenant_id"))||!actor.getVlsUserId().equals(text(run,"opening_user_id")))
            throw new ServiceException("训练必须使用本次开机付款账户与租户");
        settlements.requirePaidAdmission(text(run,"usage_id"));
    }

    @Scheduled(fixedDelay=5000,initialDelay=30000)
    public void scan() {
        if(!configured())return;
        for(String run:jdbc.queryForList("SELECT run_id FROM vls_compute_paid_run WHERE active_instance_key IS NOT NULL AND worker_lease_until_ms<? ORDER BY update_time LIMIT 50",String.class,System.currentTimeMillis()))tick(run);
    }
    public void tick(String id) {
        if(!configured())return;
        long now=System.currentTimeMillis(),leaseUntil=now+30000;
        if(jdbc.update("UPDATE vls_compute_paid_run SET worker_lease_until_ms=? WHERE run_id=? AND active_instance_key IS NOT NULL AND worker_lease_until_ms<?",leaseUntil,id,now)!=1)return;
        try{advance(row(id,false));}
        catch(RuntimeException uncertain){jdbc.update("UPDATE vls_compute_paid_run SET error_code='RECOVERY_PENDING',update_time=NOW() WHERE run_id=?",id);}
        finally{jdbc.update("UPDATE vls_compute_paid_run SET worker_lease_until_ms=0 WHERE run_id=? AND worker_lease_until_ms=?",id,leaseUntil);}
    }
    private void advance(Map<String,Object> run) {
        String id=text(run,"run_id"),usage=text(run,"usage_id"),state=text(run,"state");
        String holdId=text(run,"hold_intent_id");
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
        if("FUNDING".equals(state)&&System.currentTimeMillis()+policy.getStopBudgetSeconds()*1000L>=number(run,"hold_until_ms")) {
            jdbc.update("UPDATE vls_compute_paid_run SET state='CANCELLING' WHERE run_id=?",id);
            jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",usage);state="CANCELLING";
        }
        if("FUNDING".equals(state)&&number(session,"stop_requested")==0) {
            if(!ComputeFundingIntentService.DETAIL_OBSERVED.equals(hold.state))return;
            tx.execute(status->{
                ComputeNode node=lifecycle.controlledNode(usage);
                Map<String,Object> current=row(id,true);
                if(!"FUNDING".equals(text(current,"state")))return null;
                settlements.requirePaidAdmission(usage);
                if(nodes.serverRows(node).stream().anyMatch(n->n.getPowerOperationId()!=null))throw new ServiceException("实例已有待确认操作");
                nodes.assertNoActiveJobs(node.getId());
                if(!"STOPPED".equals(node.getPowerState())||!node.getPublicInstanceKey().equals(text(run,"instance_key")))throw new ServiceException("实例状态已变化");
                queueStart(node,text(run,"start_operation_id"));
                jdbc.update("UPDATE vls_compute_paid_run SET state='STARTING',error_code=NULL,update_time=NOW() WHERE run_id=?",id);return null;
            });return;
        }
        if(observedUsage.get("billing_start_at")!=null&&observedUsage.get("billing_end_at")==null) {
            jdbc.update("UPDATE vls_compute_paid_run SET state='RUNNING',error_code=NULL WHERE run_id=? AND state='STARTING'",id);
            long deadline=Math.addExact(((Timestamp)observedUsage.get("billing_start_at")).getTime(),number(run,"maximum_run_seconds")*1000L);
            if(System.currentTimeMillis()>=deadline)jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",usage);
            if(number(session,"stop_requested")!=0||System.currentTimeMillis()>=deadline)lifecycle.requestSafeStop(usage);
            return;
        }
        boolean neverStarted=("CANCELLING".equals(state)||"START_FAILED".equals(text(observedUsage,"state")))&&observedUsage.get("billing_start_at")==null;
        boolean stopped="STOP_CONFIRMED".equals(text(session,"safety_state"))&&observedUsage.get("billing_end_at")!=null;
        if(!neverStarted&&!stopped)return;
        if(neverStarted)ledger.startFailed(usage);
        if(stopped&&(number(session,"metered_until_ms")<number(session,"last_running_ms")
            ||!jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? AND state NOT IN ('SETTLED','NO_DEBIT')",usage).isEmpty()))return;
        if(!ComputeFundingIntentService.DETAIL_OBSERVED.equals(hold.state))return;
        Map<String,Object> detail=jdbc.queryForMap("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",hold.id);
        String remaining=text(detail,"remaining_quota");
        if(!"0".equals(remaining)) {
            ComputeFundingIntentService.Intent release=run.get("release_intent_id")==null?funding.prepareFinalRelease(hold.id,remaining):funding.recover(text(run,"release_intent_id"));
            jdbc.update("UPDATE vls_compute_paid_run SET release_intent_id=?,state='RELEASING' WHERE run_id=?",release.id,id);
            recovery.advance(release.id,System.currentTimeMillis());return;
        }
        tx.execute(status->{
            jdbc.update("UPDATE vls_compute_paid_run SET state=?,active_instance_key=NULL,error_code=NULL,update_time=NOW() WHERE run_id=?",neverStarted?"CANCELLED":"COMPLETED",id);
            jdbc.update("UPDATE vls_compute_usage SET active_server_key=NULL,state=CASE WHEN billing_start_at IS NULL THEN 'START_FAILED' ELSE state END WHERE id=?",usage);
            return null;
        });
    }
    private void queueStart(ComputeNode node,String operation) {
        if(!providers.orderedStream().anyMatch(p->p.supports(node)))throw new ServiceException("厂商开机接口不可用");
        jdbc.update("UPDATE vls_compute_node SET power_state='STARTING',probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE "+ComputeNodeService.SERVER_WHERE,ComputeNodeService.serverArgs(node));
        jdbc.update("UPDATE vls_compute_node SET power_operation_id=?,power_operation_action='start',power_operation_started_at=NOW(),power_operation_submitted=0,lifecycle_message='预冻结已确认，等待厂商开机' WHERE id=?",operation,node.getId());
    }
    private void eligible(ComputeNode node) {
        if(!node.isEnabled()||!"PLATFORM".equals(node.getResourceScope())||!"POINTS".equals(node.getBillingType())||node.getPublicInstanceKey()==null
            ||!"API".equals(node.getControlMode())||nodes.isCompanyShared(node))throw new ServiceException("当前资源不支持个人付费开机");
        if(!node.getId().equals(nodes.serverRows(node).get(0).getId()))throw new ServiceException("请选择实例的主资源记录");
        ComputeBillingPolicy.requireEconomicalPaidOffer(node,System.currentTimeMillis());
        if(!providers.orderedStream().anyMatch(p->p.supports(node)))throw new ServiceException("当前厂商控制未启用");
        if(nodes.serverRows(node).stream().anyMatch(n->n.getPowerOperationId()!=null))throw new ServiceException("请先等待原实例操作完成");
        nodes.assertNoActiveJobs(node.getId());
    }
    private boolean configured(){return properties.isEnabled()&&properties.isAllowWrites()&&properties.isSafetyStopEnabled()&&properties.isPaidAdmissionEnabled();}
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
        return new View(row,BillingSnapshot.read(text(row,"quote_snapshot"),json),charged.toString(),remaining);
    }
    private boolean terminal(String state){return "CANCELLED".equals(state)||"COMPLETED".equals(state);}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception invalid){throw new ServiceException("价格快照无效");}}
    private JsonNode parse(String value){try{return json.readTree(value);}catch(Exception invalid){throw new ServiceException("价格快照无效");}}
    private ComputeBillingRate readRate(String value){try{return json.readValue(value,ComputeBillingRate.class);}catch(Exception invalid){throw new ServiceException("价格快照无效");}}
    private static String text(Map<String,Object> row,String field){Object value=row.get(field);return value==null?null:String.valueOf(value);}
    private static long number(Map<String,Object> row,String field){return ExactQuota.integer(text(row,field),false).longValueExact();}
}
