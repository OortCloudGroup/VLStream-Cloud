package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real local order/claim/checkpoint gates; supplier and wallet remain isolated in-process fixtures. */
@Tag("dev")
class ConfiguredArchiveRecoveryPaidRunTest {
    private ConfiguredOfficialRunIntegrationTest official;
    private ComputePaidRunIntegrationTest f;
    private JdbcTemplate jdbc;
    private ComputePaidArchiveRecoveryService archives;
    private String source,jobId="original-archive-job";

    @BeforeEach void setup() throws Exception {
        official=new ConfiguredOfficialRunIntegrationTest();official.setup();f=official.f;jdbc=f.jdbc;
        jdbc.execute("ALTER TABLE vls_cloud_training_job ADD COLUMN options_json TEXT");
        jdbc.execute("ALTER TABLE vls_cloud_training_job ADD COLUMN run_dir TEXT");
        jdbc.execute("CREATE TABLE vls_model_artifact_storage(tenant_id VARCHAR(64),path_hash CHAR(64),storage_state VARCHAR(32),retry_after TIMESTAMP,sha256 VARCHAR(64),file_size BIGINT)");
        archives=new ComputePaidArchiveRecoveryService(jdbc,f.json,new DataSourceTransactionManager(jdbc.getDataSource()));
        ReflectionTestUtils.setField(f.runs,"archiveRecovery",archives);
        ReflectionTestUtils.setField(f.lifecycle,"paidArchives",archives);
        source=official.accepted();official.running(source);
        ComputePublicInventoryService.Instance claim=official.nodes.publicInventory().read(official.node(),false);
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,node_id,public_instance_key,public_claim_id,public_owner_subject,job_state,options_json,run_dir) VALUES(?,'tenant-test',-6000,?,?,?,'ARCHIVING','{}','/fixture/original-job')",
            jobId,official.node().getPublicInstanceKey(),claim.getClaimId(),claim.getOwnerSubject());
        ObjectNode proof=f.json.createObjectNode();proof.put("version",1);proof.put("state","SUCCEEDED");proof.put("processExited",true);
        proof.put("sha256",repeat('a'));proof.put("resultSha256",repeat('b'));proof.put("size",400);
        archives.recordExit(job(),official.node(),target(),claim.getClaimId(),claim.getOwnerSubject(),proof,System.currentTimeMillis());
        ObjectNode expired=(ObjectNode)f.json.readTree(job().getOptionsJson());
        ((ObjectNode)expired.path("archiveAttempt")).put("deadlineMs",System.currentTimeMillis()-1);
        jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE id=?",expired.toString(),jobId);
        assertTrue(archives.pauseForStop(jobId,official.node(),"tenant-test",claim.getOwnerSubject(),claim.getClaimId(),target(),f.runs.get(source,f.payer).getUsageId(),System.currentTimeMillis()));
        official.finish(source);
        assertEquals("COMPLETED",f.runs.get(source,f.payer).getState());
        assertEquals("ARCHIVE_PENDING",job().getJobState());
        // The existing wallet fixture models one hold at a time. The completed old hold stays
        // in its local journal; a fresh fixture serves only the explicitly accepted new run.
        f.wallet=f.new Wallet();
    }
    @AfterEach void cleanup(){official.cleanup();}
    private CloudTrainingJob job(){return jdbc.queryForObject("SELECT * FROM vls_cloud_training_job WHERE id=?",new BeanPropertyRowMapper<>(CloudTrainingJob.class),jobId);}
    private String target(){return String.valueOf(official.nodes.configurationSnapshot(official.node()).get("targetFingerprint"));}
    private static String repeat(char c){char[] value=new char[64];java.util.Arrays.fill(value,c);return new String(value);}
    private ComputePaidRunService.View quote(){return f.runs.quoteArchiveRecovery(source,f.payer,f.config);}
    private String accepted(){ComputePaidRunService.View quote=quote();f.runs.accept(quote.getRunId(),f.payer);return quote.getRunId();}

    @Test void pendingArchiveKeepsClaimAndBlocksOrdinaryStartWhileRecoveryQuoteIsExplicitAndRepeatable() {
        ComputePaidRunService.View original=f.runs.get(source,f.payer);
        assertTrue(original.isArchiveRecoveryAvailable());assertFalse(original.isArchiveRecovery());
        assertEquals("OCCUPIED",official.nodes.publicInventory().read(official.node(),false).getPublicState());
        assertThrows(RuntimeException.class,()->official.nodes.publicInventory().release(official.node()));
        assertThrows(RuntimeException.class,()->f.runs.quote(-6000L,f.payer,f.config));
        ComputePaidRunService.View recovery=quote();
        assertTrue(recovery.isArchiveRecovery());assertFalse(recovery.isArchiveRecoveryAvailable());
        assertEquals(source,recovery.getArchiveRecoverySourceRunId());
        assertEquals(Collections.singletonList(jobId),recovery.getArchiveRecoveryJobIds());
        assertEquals(recovery.getRunId(),quote().getRunId());
        assertEquals(0,f.wallet.holdPosts);assertEquals("ARCHIVE_PENDING",job().getJobState());
        verify(f.supplier,times(1)).submit(any(),eq("start"),anyString());
    }

    @Test void acceptingRecoveryCreatesNewUsageAndHoldAndOnlyAllowsOriginalArchiveAfterConfirmedStart() {
        String oldUsage=f.runs.get(source,f.payer).getUsageId();
        String oldHold=jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run WHERE run_id=?",String.class,source);
        String recovery=accepted();f.runs.accept(recovery,f.payer);
        String usage=f.runs.get(recovery,f.payer).getUsageId();
        String newHold=jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run WHERE run_id=?",String.class,recovery);
        assertNotEquals(oldUsage,usage);assertNotEquals(oldHold,newHold);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",Integer.class));
        assertFalse(archives.mayArchive(job(),official.node(),target(),System.currentTimeMillis()));
        assertEquals("ARCHIVE_PENDING",job().getJobState());assertEquals(0,f.wallet.holdPosts);
        official.running(recovery);
        assertFalse(f.runs.trainingAvailable(official.node()));
        assertThrows(RuntimeException.class,()->f.runs.requireTraining(official.node()));
        assertTrue(archives.mayArchive(job(),official.node(),target(),System.currentTimeMillis()));
        assertEquals("ARCHIVING",job().getJobState());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
        assertEquals(1,f.wallet.holdPosts);
        verify(f.supplier,times(2)).submit(any(),eq("start"),anyString());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED' WHERE id=?",jobId);
        f.runs.tick(recovery);f.runs.tick(recovery);official.settle(recovery);for(int i=0;i<5;i++)f.runs.tick(recovery);
        assertEquals("COMPLETED",f.runs.get(recovery,f.payer).getState());assertEquals(0,f.wallet.remaining());
        assertFalse(f.runs.get(source,f.payer).isArchiveRecoveryAvailable());
        assertEquals("OCCUPIED",official.nodes.publicInventory().read(official.node(),false).getPublicState());
    }

    @Test void changedClaimOrForeignPayerCannotObtainOrAcceptRecovery() throws Exception {
        BillingPayer other=BillingPayer.fromVerifiedSelf(f.actor,f.json.readTree("{\"id\":14}"));
        assertThrows(RuntimeException.class,()->f.runs.quoteArchiveRecovery(source,other,f.config));
        ComputePaidRunService.View quote=quote();
        jdbc.update("UPDATE vls_compute_public_instance SET claim_id='different-claim'");
        assertThrows(RuntimeException.class,()->f.runs.accept(quote.getRunId(),f.payer));
        assertEquals("QUOTED",f.runs.get(quote.getRunId(),f.payer).getState());assertEquals(0,f.wallet.holdPosts);
    }

    @Test void changedOriginalTaskAfterQuoteCannotCreateAnotherHold() {
        ComputePaidRunService.View quote=quote();
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='RUNNING' WHERE id=?",jobId);
        assertThrows(RuntimeException.class,()->f.runs.accept(quote.getRunId(),f.payer));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",Integer.class));
        assertEquals(0,f.wallet.holdPosts);
    }

    @Test void originalTaskRecheckAfterFundingPreventsPowerStartWithoutReplacingOrder() {
        String recovery=accepted();f.runs.tick(recovery);
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='RUNNING' WHERE id=?",jobId);
        f.runs.tick(recovery);f.runs.tick(recovery);
        assertEquals("FUNDING",f.runs.get(recovery,f.payer).getState());
        assertEquals(1,f.wallet.holdPosts);assertEquals(0,f.wallet.capturePosts);
        assertEquals("RECOVERY_PENDING",f.runs.get(recovery,f.payer).getErrorCode());
        verify(f.supplier,times(1)).submit(any(),eq("start"),anyString());
    }

    @Test void liveStorageCopyLeaseAppearingAfterFundingBlocksRecoveryStartWithoutClearingTheLease() {
        String recovery=accepted();f.runs.tick(recovery);
        java.sql.Timestamp retry=new java.sql.Timestamp(System.currentTimeMillis()+21600000L);
        jdbc.update("INSERT INTO vls_model_artifact_storage(tenant_id,path_hash,storage_state,retry_after) VALUES('tenant-test',?,'COPYING',?)",
            com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash(job().modelKey()),retry);
        f.runs.tick(recovery);f.runs.tick(recovery);
        assertEquals("FUNDING",f.runs.get(recovery,f.payer).getState());
        assertEquals(1,f.wallet.holdPosts);assertEquals(0,f.wallet.capturePosts);
        assertEquals(retry,jdbc.queryForObject("SELECT retry_after FROM vls_model_artifact_storage",java.sql.Timestamp.class));
        verify(f.supplier,times(1)).submit(any(),eq("start"),anyString());
        f.runs.stop(recovery,f.payer);for(int i=0;i<4;i++)f.runs.tick(recovery);
        assertEquals("CANCELLED",f.runs.get(recovery,f.payer).getState());assertEquals(0,f.wallet.remaining());
        assertFalse(f.runs.get(source,f.payer).isArchiveRecoveryAvailable());
    }

    @Test void activeStorageBackoffMakesRecoveryUnavailableBeforeQuoteOrConsent() {
        jdbc.update("INSERT INTO vls_model_artifact_storage(tenant_id,path_hash,storage_state,retry_after) VALUES('tenant-test',?,'FAILED',?)",
            com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash(job().modelKey()),new java.sql.Timestamp(System.currentTimeMillis()+300000L));
        assertFalse(f.runs.get(source,f.payer).isArchiveRecoveryAvailable());
        assertThrows(RuntimeException.class,this::quote);assertEquals(0,f.wallet.holdPosts);
        verify(f.supplier,times(1)).submit(any(),eq("start"),anyString());
    }

    @Test void cancelledRecoveryBeforeFundingDoesNotResumeArchiveAndCanBeQuotedAgainExplicitly() {
        String recovery=accepted();f.runs.stop(recovery,f.payer);f.runs.tick(recovery);
        assertEquals("CANCELLED",f.runs.get(recovery,f.payer).getState());assertEquals(0,f.wallet.holdPosts);
        assertEquals("ARCHIVE_PENDING",job().getJobState());
        assertFalse(archives.mayArchive(job(),official.node(),target(),System.currentTimeMillis()));
        ComputePaidRunService.View next=f.runs.quoteArchiveRecovery(recovery,f.payer,f.config);
        assertNotEquals(recovery,next.getRunId());assertEquals(source,next.getArchiveRecoverySourceRunId());
        assertEquals(0,f.wallet.holdPosts);
    }

    @Test void anotherPausedAttemptCannotAutoResumeUnderTheSamePaidRun() throws Exception {
        String recovery=accepted();official.running(recovery);
        assertTrue(archives.mayArchive(job(),official.node(),target(),System.currentTimeMillis()));
        ObjectNode options=(ObjectNode)f.json.readTree(job().getOptionsJson());
        ((ObjectNode)options.path("archiveAttempt")).put("deadlineMs",System.currentTimeMillis()-1);
        jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE id=?",options.toString(),jobId);
        ComputePublicInventoryService.Instance claim=official.nodes.publicInventory().read(official.node(),false);
        assertTrue(archives.pauseForStop(jobId,official.node(),"tenant-test",claim.getOwnerSubject(),claim.getClaimId(),target(),f.runs.get(recovery,f.payer).getUsageId(),System.currentTimeMillis()));
        assertFalse(archives.mayArchive(job(),official.node(),target(),System.currentTimeMillis()));
        f.runs.tick(recovery);
        assertEquals(1,jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?",Integer.class,f.runs.get(recovery,f.payer).getUsageId()));
        verify(f.supplier,times(2)).submit(any(),eq("start"),anyString());
    }

    @Test void stoppingNewRecoveryBeforeArchiveWorkerResumesCannotTrapTheInstanceRunning() {
        String recovery=accepted();official.running(recovery);
        assertEquals("ARCHIVE_PENDING",job().getJobState());
        official.finish(recovery);
        assertEquals("COMPLETED",f.runs.get(recovery,f.payer).getState());
        assertEquals("ARCHIVE_PENDING",job().getJobState());
        assertEquals(0,f.wallet.remaining());
        assertFalse(archives.mayArchive(job(),official.node(),target(),System.currentTimeMillis()));
        assertTrue(f.runs.get(recovery,f.payer).isArchiveRecoveryAvailable());
        verify(f.supplier,times(2)).submit(any(),eq("stop"),anyString());
    }

    @Test void newerCancelledQuotesCannotHideTheCurrentOwnersOriginalPendingArchive() throws Exception {
        for(int i=0;i<25;i++) {
            String newer=quote().getRunId();f.runs.stop(newer,f.payer);
        }
        jdbc.update("UPDATE vls_compute_paid_run SET create_time=? WHERE run_id=?",new java.sql.Timestamp(System.currentTimeMillis()-86400000L),source);
        java.util.List<ComputePaidRunService.View> listed=f.runs.list(f.payer);
        assertEquals(21,listed.size());
        assertTrue(listed.stream().anyMatch(row->source.equals(row.getRunId())&&row.isArchiveRecoveryAvailable()));
        BillingPayer different=BillingPayer.fromVerifiedSelf(f.actor,f.json.readTree("{\"id\":14}"));
        assertTrue(f.runs.list(different).isEmpty());
        assertEquals(0,f.wallet.holdPosts);
    }
}
