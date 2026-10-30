package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Local H2 journal only; no training, SSH, object storage, power or wallet calls. */
@Tag("dev")
class ComputePaidArchiveRecoveryTest {
    JdbcTemplate jdbc; ObjectMapper json; ComputePaidArchiveRecoveryService service;
    ComputeNode node; CloudTrainingJob job;
    final long now = 1000000L;
    final String target = ModelArtifactObjectStore.hash("target"), owner = ModelArtifactObjectStore.hash("trusted-platform-user\nplatform");

    @BeforeEach void setup() {
        DriverManagerDataSource data = new DriverManagerDataSource("jdbc:h2:mem:archive-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(data); json = new ObjectMapper(); service = new ComputePaidArchiveRecoveryService(jdbc, json, new DataSourceTransactionManager(data));
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),node_id BIGINT,public_instance_key VARCHAR(64),public_claim_id VARCHAR(36),public_owner_subject VARCHAR(64),run_dir VARCHAR(1024),options_json TEXT,job_state VARCHAR(20),message VARCHAR(500),update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_compute_paid_run(run_id VARCHAR(36) PRIMARY KEY,usage_id VARCHAR(36),active_instance_key VARCHAR(64),instance_key VARCHAR(64),tenant_id VARCHAR(64),platform_subject VARCHAR(64),state VARCHAR(20),quote_snapshot TEXT,hold_until_ms BIGINT)");
        jdbc.execute("CREATE TABLE vls_compute_billing_session(usage_id VARCHAR(36) PRIMARY KEY,stop_requested INT)");
        jdbc.execute("CREATE TABLE vls_model_artifact_storage(tenant_id VARCHAR(64),path_hash VARCHAR(64),storage_state VARCHAR(20),retry_after TIMESTAMP,sha256 VARCHAR(64),file_size BIGINT)");
        node = new ComputeNode(); node.setId(-6000L); node.setConfigurationManaged(true); node.setProvider("autodl");
        node.setResourceScope("PLATFORM"); node.setBillingType("POINTS"); node.setControlMode("API"); node.setPublicInstanceKey("instance");
        job = new CloudTrainingJob(); job.setId("job"); job.setTenantId("tenant"); job.setNodeId(node.getId()); job.setRunDir("/fixture/original/job");
        job.setPublicInstanceKey("instance"); job.setPublicClaimId("claim"); job.setPublicOwnerSubject(owner); job.setOptionsJson("{}"); job.setJobState("ARCHIVING");
        jdbc.update("INSERT INTO vls_cloud_training_job VALUES(?,?,?,?,?,?,?,?,?,NULL,NOW())", job.getId(), "tenant", node.getId(), "instance", "claim", owner, job.getRunDir(), "{}", "ARCHIVING");
        addRun("original", "usage-original", quote(), now + 400000L);
    }

    ObjectNode quote() { ObjectNode q=json.createObjectNode(); q.putObject("run_budget").put("stop_budget_seconds",300).put("polling_safety_seconds",60); return q; }
    void addRun(String id,String usage,ObjectNode quote,long until) {
        jdbc.update("INSERT INTO vls_compute_billing_session VALUES(?,0)", usage);
        jdbc.update("INSERT INTO vls_compute_paid_run VALUES(?,?, 'instance','instance','tenant','platform','RUNNING',?,?)", id,usage,quote.toString(),until);
    }
    ObjectNode proof() { ObjectNode p=json.createObjectNode();p.put("version",1);p.put("state","SUCCEEDED");p.put("processExited",true);p.put("sha256",ModelArtifactObjectStore.hash("pt"));p.put("resultSha256",ModelArtifactObjectStore.hash("result"));p.put("size",4);p.put("childPid",101);p.put("childStart","12");p.put("supervisorPid",102);p.put("supervisorStart","13");return p; }
    void record() { service.recordExit(job,node,target,"claim",owner,proof(),now); }
    JsonNode options() throws Exception { return json.readTree(jdbc.queryForObject("SELECT options_json FROM vls_cloud_training_job WHERE id='job'",String.class)); }
    String state() { return jdbc.queryForObject("SELECT job_state FROM vls_cloud_training_job WHERE id='job'",String.class); }
    boolean pause(long time) { return service.pauseForStop("job",node,"tenant",owner,"claim",target,"usage-original",time); }
    void resumeRun(String id) {
        jdbc.update("UPDATE vls_compute_paid_run SET active_instance_key=NULL,state='COMPLETED'");
        ObjectNode q=quote();q.put("archive_recovery_source_run_id","original");q.putArray("archive_recovery_job_ids").add("job");
        addRun(id,"usage-"+id,q,now+900000L);
    }

    @Test void deadlineUsesAcceptedBudgetAndPendingSurvivesBackendRestart() throws Exception {
        record(); assertEquals(now+40000,options().path("archiveAttempt").path("deadlineMs").asLong());
        assertFalse(pause(now+39999));assertEquals("ARCHIVING",state());
        assertTrue(pause(now+40000));assertEquals("ARCHIVE_PENDING",state());
        ComputePaidArchiveRecoveryService restarted=new ComputePaidArchiveRecoveryService(jdbc,json,new DataSourceTransactionManager(jdbc.getDataSource()));
        assertEquals(Collections.singletonList("job"),restarted.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim",target));
        assertFalse(restarted.mayArchive(job,node,target,now+50000));
        assertEquals("original",options().path("archiveRecovery").path("originalRunId").asText());
    }

    @Test void unknownExitOrWrongCurrentClaimNeverCreatesAStopProof() throws Exception {
        ObjectNode unknown=proof();unknown.put("processExited",false);
        assertThrows(IllegalStateException.class,()->service.recordExit(job,node,target,"claim",owner,unknown,now));
        assertThrows(IllegalStateException.class,()->service.recordExit(job,node,target,"different-claim",owner,proof(),now));
        assertFalse(options().has("archiveRecovery"));assertFalse(pause(now+100000));assertEquals("ARCHIVING",state());
    }

    @Test void changedOwnerTargetOrFrozenResultRemainsBlocked() throws Exception {
        record();pause(now+40000);String original=options().path("archiveRecovery").toString();
        assertThrows(IllegalStateException.class,()->service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"other",owner,"claim",target));
        assertThrows(IllegalStateException.class,()->service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"other",target));
        assertThrows(IllegalStateException.class,()->service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim","different-target"));
        ObjectNode changed=proof();changed.put("sha256",ModelArtifactObjectStore.hash("changed same size PT"));
        assertThrows(IllegalStateException.class,()->service.recordExit(job,node,target,"claim",owner,changed,now+1));
        assertEquals(original,options().path("archiveRecovery").toString());assertEquals("ARCHIVE_PENDING",state());
    }

    @Test void explicitNewRunCanResumeOnceAndItsTimeoutNeverResumesItAgain() throws Exception {
        record();pause(now+40000);String original=options().path("archiveRecovery").toString();resumeRun("recovery");
        assertTrue(service.mayArchive(job,node,target,now+100000));assertEquals("ARCHIVING",state());
        assertEquals("recovery",options().path("archiveAttempt").path("runId").asText());
        long deadline=options().path("archiveAttempt").path("deadlineMs").asLong();
        jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id='usage-recovery'");
        assertFalse(service.mayArchive(job,node,target,deadline));assertEquals("ARCHIVE_PENDING",state());
        assertFalse(service.mayArchive(job,node,target,deadline+1000));
        assertTrue(service.pauseForStop("job",node,"tenant",owner,"claim",target,"usage-recovery",deadline));
        assertEquals(original,options().path("archiveRecovery").toString());
    }

    @Test void differentRunWithoutExplicitOriginalJobListCannotResume() {
        record();pause(now+40000);jdbc.update("UPDATE vls_compute_paid_run SET active_instance_key=NULL,state='COMPLETED'");
        addRun("unrelated","usage-unrelated",quote(),now+900000L);
        assertFalse(service.mayArchive(job,node,target,now+100000));assertEquals("ARCHIVE_PENDING",state());
    }

    @Test void finishedUploadIsNotChangedBackToPendingAndUnprovedQueueStillBlocksRecovery() {
        record();jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED' WHERE id='job'");
        assertFalse(pause(now+100000));assertEquals("COMPLETED",state());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='RUNNING' WHERE id='job'");
        assertThrows(IllegalStateException.class,()->service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim",target));
    }

    @Test void sharedFreeAndUnconfiguredInstancesNeverUsePaidArchiveException() {
        node.setSchedulingMode(ComputeSharedQueueService.MODE);
        assertThrows(IllegalStateException.class,()->service.recordExit(job,node,target,"claim",owner,proof(),now));
        node.setSchedulingMode(ComputeSharedQueueService.EXCLUSIVE);node.setBillingType("FREE");
        assertThrows(IllegalStateException.class,()->service.recordExit(job,node,target,"claim",owner,proof(),now));
        node.setBillingType("POINTS");node.setConfigurationManaged(false);
        assertThrows(IllegalStateException.class,()->service.recordExit(job,node,target,"claim",owner,proof(),now));
    }

    @Test void futureCopyLeaseAndFailedBackoffBlockPaidResumeWithoutChangingStorage() {
        record();pause(now+40000);String key=ModelArtifactObjectStore.hash(job.modelKey());
        java.sql.Timestamp future=new java.sql.Timestamp(System.currentTimeMillis()+600000);
        for(String state:new String[]{"COPYING","FAILED"}) {
            jdbc.update("DELETE FROM vls_model_artifact_storage");
            jdbc.update("INSERT INTO vls_model_artifact_storage VALUES('tenant',?,?,?,NULL,NULL)",key,state,future);
            assertThrows(com.ruoyi.common.exception.ServiceException.class,()->service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim",target));
            assertEquals(state,jdbc.queryForObject("SELECT storage_state FROM vls_model_artifact_storage",String.class));
            assertEquals(future,jdbc.queryForObject("SELECT retry_after FROM vls_model_artifact_storage",java.sql.Timestamp.class));
        }
        jdbc.update("UPDATE vls_model_artifact_storage SET storage_state='READY'");
        assertThrows(com.ruoyi.common.exception.ServiceException.class,()->service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim",target));
        jdbc.update("UPDATE vls_model_artifact_storage SET sha256=?,file_size=4",proof().path("sha256").asText());
        assertEquals(Collections.singletonList("job"),service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim",target));
        jdbc.update("UPDATE vls_model_artifact_storage SET storage_state='FAILED',retry_after=?",new java.sql.Timestamp(1));
        assertEquals(Collections.singletonList("job"),service.requireRecoverableJobs(node,Collections.singletonList(node.getId()),"tenant",owner,"claim",target));
    }

    @Test void jobRecoveryDoesNotTakeRunLocksInReverseOrder() throws Exception {
        record();
        java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            new org.springframework.transaction.support.TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())).execute(status->{
                jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id='original' FOR UPDATE");
                jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id='usage-original' FOR UPDATE");
                try { assertTrue(executor.submit(()->service.mayArchive(job,node,target,now+1)).get(2,java.util.concurrent.TimeUnit.SECONDS)); }
                catch(Exception failure) { throw new AssertionError("Archive recovery acquired run/session locks after its job lock",failure); }
                return null;
            });
        } finally {executor.shutdownNow();}
    }
}
