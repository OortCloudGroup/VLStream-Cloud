package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeExecutionCoordinatorTest {
    private final ObjectMapper json = new ObjectMapper();
    private final UnifiedComputeCatalog catalog = mock(UnifiedComputeCatalog.class);
    private final ConfiguredComputeRegistry registry = mock(ConfiguredComputeRegistry.class);
    private JdbcTemplate jdbc;
    private ComputeCatalogProperties properties;
    private ComputeExecutionCoordinator coordinator;
    private ComputeNode node;

    @BeforeEach void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE vls_compute_execution_lease (node_id BIGINT,job_ref VARCHAR(112),app_scope CHAR(64),"
            + "execution_revision BIGINT,acquired INT DEFAULT 0,release_requested INT DEFAULT 0,retry_count INT DEFAULT 0,"
            + "retry_after TIMESTAMP DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(node_id,job_ref))");
        ModelHubProperties deployment = new ModelHubProperties();
        deployment.setBaseUrl("https://deployment.example/");
        deployment.setAppId("prod");
        properties = new ComputeCatalogProperties(deployment);
        coordinator = new ComputeExecutionCoordinator(properties, catalog, registry, jdbc, json);
        node = new ComputeNode();
        node.setId(-173L);
        node.setPlatformCatalogManaged(true);
        node.setConfigurationRevision(8);
    }

    @Test void legacyCatalogKeepsExistingQueueAndNeverCallsTheCoordinator() {
        assertTrue(coordinator.acquire(node, "training:legacy"));
        coordinator.deliverReleases();
        verifyNoInteractions(catalog);
        assertEquals(0, count());
    }

    @Test void acquireWaitsOnAnotherEnvironmentAndResumesTheSameDurableJob() throws Exception {
        properties.enableDeploymentLeases();
        when(catalog.request(eq("executionAcquire"), any())).thenReturn(response("OCCUPIED"), response("ACQUIRED"));
        assertFalse(coordinator.acquire(node, "training:job-a"));
        assertTrue(coordinator.acquire(node, "training:job-a"));
        verify(catalog, times(2)).request(eq("executionAcquire"), argThat(body -> {
            try {
                JsonNode payload = json.readTree(body);
                return "-173".equals(payload.path("nodeId").asText()) && "training:job-a".equals(payload.path("jobRef").asText())
                    && payload.path("executionRevision").asLong() == 8;
            } catch (Exception invalid) { return false; }
        }));
        assertTrue(coordinator.acquire(node, "training:job-a"));
        assertEquals(1, count());
        verifyNoMoreInteractions(catalog);
    }

    @Test void releaseStartsOnlyAfterVerifiedLocalTerminalTransactionCommits() throws Exception {
        properties.enableDeploymentLeases();
        when(catalog.request(eq("executionAcquire"), any())).thenReturn(response("ACQUIRED"));
        when(catalog.request(eq("executionRelease"), any())).thenReturn(response("RELEASED"));
        assertTrue(coordinator.acquire(node, "training:job-a"));
        new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()))
            .execute(status -> {
                coordinator.enqueueRelease(-173L, "training:job-a");
                status.setRollbackOnly();
                return null;
            });
        coordinator.deliverReleases();
        verify(catalog, never()).request(eq("executionRelease"), any());
        coordinator.enqueueRelease(-173L, "training:job-a");
        assertFalse(coordinator.acquire(node, "training:job-a"));
        coordinator.deliverReleases();
        verify(catalog).request(eq("executionRelease"), any());
        assertEquals(0, count());
    }

    @Test void anApplicationCannotResumeOrReleaseAnotherApplicationsLocalBinding() throws Exception {
        properties.enableDeploymentLeases();
        when(catalog.request(eq("executionAcquire"), any())).thenReturn(response("ACQUIRED"));
        assertTrue(coordinator.acquire(node, "cloudai:run-a"));
        properties.getDeployment().setAppId("other");
        coordinator.enqueueRelease(-173L, "cloudai:run-a");
        coordinator.deliverReleases();
        assertThrows(ServiceException.class, () -> coordinator.acquire(node, "cloudai:run-a"));
        verify(catalog, never()).request(eq("executionRelease"), any());
        assertEquals(1, count());
    }

    @Test void aLostReleaseReplyRetainsTheCallbackAndRetriesAfterBackoff() throws Exception {
        properties.enableDeploymentLeases();
        when(catalog.request(eq("executionAcquire"), any())).thenReturn(response("ACQUIRED"));
        when(catalog.request(eq("executionRelease"), any()))
            .thenThrow(new IllegalStateException("fixture: response lost")).thenReturn(response("RELEASED"));
        assertTrue(coordinator.acquire(node, "training:job-a"));
        coordinator.enqueueRelease(-173L, "training:job-a");

        coordinator.deliverReleases();
        assertEquals(1, count());
        assertEquals(1, jdbc.queryForObject("SELECT retry_count FROM vls_compute_execution_lease", Integer.class));
        coordinator.deliverReleases();
        verify(catalog, times(1)).request(eq("executionRelease"), any());

        jdbc.update("UPDATE vls_compute_execution_lease SET retry_after=NOW()");
        coordinator.deliverReleases();
        verify(catalog, times(2)).request(eq("executionRelease"), any());
        assertEquals(0, count());
    }

    @Test void uncertainOccupancyDoesNotLookFreeOrChangeThePaidPrice() {
        properties.enableDeploymentLeases();
        assertTrue(coordinator.occupancy().isEmpty());
        ComputeMarketService.Resource paid = new ComputeMarketService.Resource();
        paid.setBillingType("POINTS");
        paid.setClaimState("FREE");
        paid.setUserHourlyPrice("13.00");
        paid.setPaidAdmissionEnabled(true);
        ComputeMarketService.applyDeploymentOccupancy(paid, "OCCUPIED");
        assertEquals("OTHER", paid.getClaimState());
        assertFalse(paid.isPaidAdmissionEnabled());
        assertEquals("13.00", paid.getUserHourlyPrice());

        paid.setClaimState("MINE");
        ComputeMarketService.applyDeploymentOccupancy(paid, "UNKNOWN");
        assertEquals("MINE", paid.getClaimState());
    }

    private JsonNode response(String state) throws Exception {
        return json.readTree("{\"code\":200,\"data\":{\"state\":\"" + state + "\",\"executionRevision\":8}}");
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_execution_lease", Integer.class);
    }
}
