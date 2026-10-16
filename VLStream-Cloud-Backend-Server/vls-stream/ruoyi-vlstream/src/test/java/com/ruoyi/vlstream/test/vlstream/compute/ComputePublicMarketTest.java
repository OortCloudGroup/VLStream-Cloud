package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class ComputePublicMarketTest {
    @Test void onlyPublishedEnabledSupplyAndExplicitColumnsAreQueried() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet row = mock(ResultSet.class);
        when(row.getString("name")).thenReturn("Public GPU");
        when(row.getString("gpu_model")).thenReturn("Tesla P100");
        when(row.getString("billing_type")).thenReturn("FREE");
        when(row.getString("scheduling_mode")).thenReturn("SHARED_GPU_QUEUE");
        when(row.getString("power_state")).thenReturn("RUNNING");
        when(row.getString("probe_state")).thenReturn("READY");
        when(row.getTimestamp(anyString())).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(jdbc.query(anyString(), any(RowMapper.class), eq(20), eq(0))).thenAnswer(call -> {
            String sql = call.getArgument(0);
            assertTrue(sql.contains("n.resource_scope='PLATFORM' AND n.enabled=1"));
            for (String forbidden : new String[]{"SELECT *", "tenant_id", "owner_subject", "password", "host", "provider_cost", "vls_cloud_training_job"})
                assertFalse(sql.contains(forbidden), forbidden);
            RowMapper<ComputePublicMarketService.Offer> mapper = call.getArgument(1);
            return Collections.singletonList(mapper.mapRow(row, 0));
        });
        ComputePublicMarketService.Page result = new ComputePublicMarketService(jdbc).list(1, 20);
        assertEquals("QUEUE", result.getItems().get(0).getAvailability());
        String json = new ObjectMapper().writeValueAsString(result);
        assertFalse(json.contains("tenant")); assertFalse(json.contains("token")); assertFalse(json.contains("task"));
        assertEquals(10, new ObjectMapper().readTree(json).path("items").get(0).size());
    }
    @Test void staleUnknownAndPaidOffersCannotClaimAvailability() {
        ComputePublicMarketService.Offer offer = new ComputePublicMarketService.Offer();
        offer.setBillingType("FREE"); offer.setPowerState("UNKNOWN"); offer.setHealthState("READY");
        assertEquals("UNKNOWN", ComputePublicMarketService.availability(offer, false, "FREE", false));
        offer.setPowerState("RUNNING"); offer.setSchedulingMode("INSTANCE_EXCLUSIVE");
        assertEquals("BUSY", ComputePublicMarketService.availability(offer, false, "OCCUPIED", true));
        assertEquals("UNAVAILABLE", ComputePublicMarketService.availability(offer, false, "FREE", true));
        assertEquals("AVAILABLE", ComputePublicMarketService.availability(offer, false, "FREE", false));
        offer.setBillingType("POINTS");
        assertEquals("PAID_DISABLED", ComputePublicMarketService.availability(offer, false, "FREE", false));
    }
    @Test void boundsAreCheckedBeforeDatabaseAccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ComputePublicMarketService service = new ComputePublicMarketService(jdbc);
        for (int[] args : new int[][]{{0,20},{101,20},{1,0},{1,21}})
            assertThrows(IllegalArgumentException.class, () -> service.list(args[0],args[1]));
        verifyNoInteractions(jdbc);
    }
    @Test void namesCannotContainAddressesOrCredentialSyntax() {
        for (String value : new String[]{"GPU 192.168.1.1", "ssh://host", "token=secret", "user@host", "example.internal", "fe80::1"})
            assertEquals("GPU", ComputePublicMarketService.publicText(value, "GPU"));
        assertEquals("公司 P100", ComputePublicMarketService.publicText("公司 P100", "GPU"));
    }
}
