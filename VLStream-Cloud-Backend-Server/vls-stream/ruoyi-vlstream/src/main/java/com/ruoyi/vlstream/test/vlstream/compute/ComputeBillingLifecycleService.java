package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingLifecycleGateway;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Uses the existing cloud-training cancellation flag and ComputePowerWorker outbox.
 * It never calls provider.submit, kills processes, deletes data, or releases ownership. */
@Service
public class ComputeBillingLifecycleService implements BillingLifecycleGateway {
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private ConfiguredPaidPowerService configuredPower;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    private ComputePaidArchiveRecoveryService paidArchives;
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final ObjectProvider<ComputePowerProvider> providers;
    private final TransactionTemplate transaction;
    private final ObjectMapper json;
    private final ExternalBillingProperties properties;
    public ComputeBillingLifecycleService(JdbcTemplate jdbc,ComputeNodeService nodes,ObjectProvider<ComputePowerProvider> providers,
                                          PlatformTransactionManager manager,ObjectMapper json,ExternalBillingProperties properties) {
        this.jdbc=jdbc;this.nodes=nodes;this.providers=providers;this.transaction=new TransactionTemplate(manager);this.json=json;this.properties=properties;
    }
    /** Only called after current-request SSO/self binding and current whole-instance ownership check. */
    public void arm(String usageId,BillingPayer payer) {
        transaction.execute(tx->{
            Map<String,Object> session=session(usageId);
            if(!payer.actorId.equals(session.get("opening_user_id"))||!payer.tenantId.equals(session.get("tenant_id"))||!payer.userId.equals(session.get("payer_user_id")))throw new IllegalArgumentException("Billing actor mismatch");
            ComputeNode node=node(session);
            ComputePublicInventoryService.Instance claim=nodes.publicInventory().requireOwner(node);
            if(!ModelArtifactObjectStore.hash("trusted-platform-user\n"+payer.platformSubject).equals(claim.getOwnerSubject()))throw new IllegalArgumentException("Claim owner mismatch");
            String snapshot=fingerprint(node,claim);
            if(session.get("control_snapshot")!=null&&!snapshot.equals(session.get("control_snapshot")))throw new IllegalArgumentException("Immutable billing control changed");
            jdbc.update("UPDATE vls_compute_billing_session SET control_snapshot=?,safety_state='ARMED' WHERE usage_id=? AND control_snapshot IS NULL",snapshot,usageId);return null;
        });
    }
    @Override public Observation observe(String usageId) {
        if(!properties.isEnabled())throw new IllegalStateException("Billing disabled");
        ComputeNode node=transaction.execute(tx->control(usageId));
        ComputePowerProvider provider=provider(node);
        ComputePowerProvider.Snapshot result=provider.status(node);
        if(nodes.officialConfigured(node)) {
            nodes.recordConfiguredPower(node,result);
            if(result.providerStoppedAt!=null) {
                java.sql.Timestamp started=jdbc.queryForObject("SELECT billing_start_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,usageId);
                if(started==null||result.providerStoppedAt.isBefore(started.toInstant()))result.providerStoppedAt=null;
            }
        }
        Instant now=Instant.now();transaction.execute(tx->{control(usageId);return null;});
        if("STOPPED".equals(result.power)&&!ComputeBillingPolicy.computeStopConfirmed(node,result))
            return new Observation(new ComputePowerProvider.Snapshot("UNKNOWN",result.capacity,"停机模式尚未证实停止计算收费"),now);
        return new Observation(result,now);
    }
    ComputeNode controlledNode(String usageId){return control(usageId);}
    @Override public void requestSafeStop(String usageId) {
        if(!properties.isEnabled()||!properties.isSafetyStopEnabled())return;
        transaction.execute(tx->{
            Map<String,Object> session=session(usageId);
            if(((Number)session.get("stop_requested")).intValue()!=1||"STOP_CONFIRMED".equals(session.get("safety_state")))return null;
            ComputeNode node=control(usageId);provider(node);
            List<ComputeNode> aliases=nodes.serverRows(node);
            String placeholders=aliases.stream().map(n->"?").collect(Collectors.joining(","));
            Object[] ids=aliases.stream().map(ComputeNode::getId).toArray();
            List<Map<String,Object>> jobs=jdbc.queryForList("SELECT id,tenant_id,public_claim_id,public_owner_subject,job_state FROM vls_cloud_training_job WHERE node_id IN ("+placeholders+") AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED') ORDER BY id FOR UPDATE",ids);
            ComputePublicInventoryService.Instance claim=nodes.publicInventory().read(node,true);
            for(Map<String,Object> job:jobs) {
                if(!session.get("tenant_id").equals(job.get("tenant_id"))||!claim.getClaimId().equals(job.get("public_claim_id"))||!claim.getOwnerSubject().equals(job.get("public_owner_subject"))) {
                    phase(usageId,"FOREIGN_JOB_BLOCKED");return null;
                }
            }
            if(!jobs.isEmpty()) {
                boolean blocked=false;
                for(Map<String,Object> job:jobs) {
                    if(nodes.officialConfigured(node)&&paidArchives!=null&&paidArchives.pauseForStop(String.valueOf(job.get("id")),node,
                        String.valueOf(session.get("tenant_id")),claim.getOwnerSubject(),claim.getClaimId(),
                        String.valueOf(nodes.configurationSnapshot(node).get("targetFingerprint")),usageId,System.currentTimeMillis()))continue;
                    blocked=true;
                    if(!"ARCHIVING".equals(job.get("job_state"))&&!ComputePaidArchiveRecoveryService.PENDING.equals(job.get("job_state")))
                        jdbc.update("UPDATE vls_cloud_training_job SET cancel_requested=1,message='计费安全停止请求，等待实例进程确认',update_time=NOW() WHERE id=? AND tenant_id=? AND public_claim_id=? AND public_owner_subject=?",job.get("id"),session.get("tenant_id"),claim.getClaimId(),claim.getOwnerSubject());
                }
                if(blocked){phase(usageId,"CANCEL_REQUESTED");return null;}
            }
            String operation=session.get("stop_operation_id")==null?UUID.randomUUID().toString():String.valueOf(session.get("stop_operation_id"));
            if(aliases.stream().anyMatch(n->n.getPowerOperationId()!=null)) {
                phase(usageId,operation.equals(node.getPowerOperationId())&&"stop".equals(node.getPowerOperationAction())?"STOP_REQUESTED":"WAITING_OPERATION");return null;
            }
            if(nodes.officialConfigured(node)) {
                if(configuredPower==null)throw new IllegalStateException("Official power outbox unavailable");
                jdbc.update("UPDATE vls_compute_billing_session SET stop_operation_id=?,safety_state='STOP_REQUESTED' WHERE usage_id=?",operation,usageId);
                configuredPower.queueStop(usageId,operation);return null;
            }
            jdbc.update("UPDATE vls_compute_node SET power_state='STOPPING',probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE "+ComputeNodeService.SERVER_WHERE,ComputeNodeService.serverArgs(node));
            jdbc.update("UPDATE vls_compute_node SET power_operation_id=?,power_operation_action='stop',power_operation_started_at=NOW(),power_operation_submitted=0,lifecycle_message='计费安全停机已排队，尚未确认停止' WHERE id=?",operation,node.getId());
            jdbc.update("UPDATE vls_compute_billing_session SET stop_operation_id=?,safety_state='STOP_REQUESTED' WHERE usage_id=?",operation,usageId);
            return null;
        });
    }
    private ComputeNode control(String usageId) {
        Map<String,Object> session=session(usageId);ComputeNode node=node(session);
        ComputePublicInventoryService.Instance claim=nodes.publicInventory().read(node,true);
        if(claim==null||session.get("control_snapshot")==null||!fingerprint(node,claim).equals(session.get("control_snapshot")))throw new IllegalStateException("Billing control identity changed");
        return node;
    }
    private Map<String,Object> session(String usageId){return jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE",usageId);}
    private ComputeNode node(Map<String,Object> session) {
        String ref=String.valueOf(session.get("instance_ref"));
        if(!ref.matches("node-server:-?[1-9][0-9]*"))throw new IllegalArgumentException("Canonical physical instance required");
        long id=Long.parseLong(ref.substring("node-server:".length()));
        ComputeNode node=id<0?nodes.get(id):jdbc.queryForObject("SELECT * FROM vls_compute_node WHERE id=? FOR UPDATE",new BeanPropertyRowMapper<>(ComputeNode.class),id);
        if(id<0&&!nodes.officialConfigured(node))throw new IllegalArgumentException("Configured official identity required");
        if(node==null||!"PLATFORM".equals(node.getResourceScope())||!"POINTS".equals(node.getBillingType())||node.getPublicInstanceKey()==null||!"API".equals(node.getControlMode()))throw new IllegalArgumentException("Only bound paid public API instances may use billing control");
        List<ComputeNode> aliases=nodes.serverRows(node);
        if(aliases.isEmpty()||!aliases.get(0).getId().equals(node.getId()))throw new IllegalStateException("Physical instance identity changed");
        return node;
    }
    private String fingerprint(ComputeNode node,ComputePublicInventoryService.Instance claim) {
        ObjectNode value=json.createObjectNode();value.put("id",node.getId());value.put("public_key",node.getPublicInstanceKey());
        value.put("provider",node.getProvider());value.put("instance",node.getProviderInstanceId());value.put("region",node.getProviderRegion());value.put("product",node.getProviderProduct());
        value.put("credential_revision",ModelArtifactObjectStore.hash(String.valueOf(node.getProviderCredentialCipher())));
        if(nodes.officialConfigured(node))value.put("configured_target",String.valueOf(nodes.configurationSnapshot(node).get("targetFingerprint")));
        value.put("claim",claim.getClaimId());value.put("owner",claim.getOwnerSubject());value.put("generation",claim.getGeneration());value.put("state",claim.getPublicState());
        if(!"OCCUPIED".equals(claim.getPublicState())||claim.getClaimId()==null||claim.getOwnerSubject()==null)throw new IllegalStateException("Active owned claim required");
        return value.toString();
    }
    private ComputePowerProvider provider(ComputeNode node){return providers.orderedStream().filter(p->p.supports(node)).findFirst().orElseThrow(()->new IllegalStateException("Verified provider control unavailable"));}
    private void phase(String usageId,String state){jdbc.update("UPDATE vls_compute_billing_session SET safety_state=? WHERE usage_id=?",state,usageId);}
}
