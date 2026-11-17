package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Release the original clean claim without needing a live execution catalog. */
@Tag("dev")
class ComputeCompletedClaimReleaseTest {
    JdbcTemplate jdbc;
    ComputePublicInventoryService inventory;
    @BeforeEach void setup() {
        jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:release_"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE vls_compute_public_instance(instance_key VARCHAR(80),public_state VARCHAR(30),owner_subject VARCHAR(80),claim_id VARCHAR(80),workspace_dirty INT,generation BIGINT)");
        jdbc.execute("CREATE TABLE vls_compute_paid_run(run_id VARCHAR(80),active_instance_key VARCHAR(80))");
        jdbc.execute("CREATE TABLE vls_compute_node(id BIGINT,public_instance_key VARCHAR(80))");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(80),node_id BIGINT,public_instance_key VARCHAR(80),job_state VARCHAR(30))");
        jdbc.update("INSERT INTO vls_compute_public_instance VALUES('original','OCCUPIED',?,'claim',0,1)",ModelArtifactObjectStore.hash("trusted-platform-user\nowner"));
        inventory=new ComputePublicInventoryService(jdbc);
    }
    String state(){return jdbc.queryForObject("SELECT public_state FROM vls_compute_public_instance",String.class);}
    @Test void settledCleanOriginalIsReleasedWithoutCatalogOrNodeRows() {
        inventory.releaseCompleted("original","owner");assertEquals("FREE",state());
        assertNull(jdbc.queryForObject("SELECT owner_subject FROM vls_compute_public_instance",String.class));
        inventory.releaseCompleted("original","owner");
        assertEquals(2,jdbc.queryForObject("SELECT generation FROM vls_compute_public_instance",Integer.class));
    }
    @Test void anotherOwnerCannotBeReleased() {
        inventory.releaseCompleted("original","another");assertEquals("OCCUPIED",state());
    }
    @Test void dirtyWorkspaceStillBlocksRelease() {
        jdbc.update("UPDATE vls_compute_public_instance SET workspace_dirty=1");
        inventory.releaseCompleted("original","owner");assertEquals("OCCUPIED",state());
    }
    @Test void activeOrderStillBlocksRelease() {
        jdbc.update("INSERT INTO vls_compute_paid_run VALUES('active','original')");
        inventory.releaseCompleted("original","owner");assertEquals("OCCUPIED",state());
    }
    @Test void activeTrainingStillBlocksRelease() {
        jdbc.update("INSERT INTO vls_cloud_training_job VALUES('job',-6000,'original','RUNNING')");
        inventory.releaseCompleted("original","owner");assertEquals("OCCUPIED",state());
    }
}
