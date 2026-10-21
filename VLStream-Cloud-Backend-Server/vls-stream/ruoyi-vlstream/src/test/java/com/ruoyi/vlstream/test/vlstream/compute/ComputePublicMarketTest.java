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
    private ComputeNode referenceNode() {
        ComputeNode node = new ComputeNode();
        node.setId(792022144331380001L); node.setProvider("autodl");
        node.setServerGroup("autodl-pro-7920a22db144"); node.setProviderGpuAmount(1);
        node.setBillingType("POINTS");
        return node;
    }
    private ComputePublicMarketService.Offer referenceOffer(ComputeNode node) {
        ComputePublicMarketService.Offer offer = new ComputePublicMarketService.Offer();
        offer.setBillingType("POINTS");
        ComputePublicMarketService.referencePrice(offer, node);
        return offer;
    }
    @Test void referenceRequiresExactTrustedIdentityAndNoConfiguredQuota() {
        ComputeNode node = referenceNode(); node.setName("Renamed resource");
        assertEquals(new BigDecimal("12.92"), referenceOffer(node).getReferencePrice());
        for (Long id : new Long[]{null, 9L}) { node = referenceNode(); node.setId(id); assertNull(referenceOffer(node).getReferencePrice()); }
        for (String provider : new String[]{null, "aliyun"}) { node = referenceNode(); node.setProvider(provider); assertNull(referenceOffer(node).getReferencePrice()); }
        for (String group : new String[]{null, "other-instance"}) { node = referenceNode(); node.setServerGroup(group); assertNull(referenceOffer(node).getReferencePrice()); }
        for (Integer amount : new Integer[]{null, 0, 2}) { node = referenceNode(); node.setProviderGpuAmount(amount); assertNull(referenceOffer(node).getReferencePrice()); }
        for (BigDecimal quota : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("6.46")}) {
            node = referenceNode(); node.setPointsPerHour(quota); assertNull(referenceOffer(node).getReferencePrice());
        }
        node = referenceNode(); node.setBillingType("FREE"); assertNull(referenceOffer(node).getReferencePrice());
        ComputePublicMarketService.Offer free = new ComputePublicMarketService.Offer(); free.setBillingType("FREE");
        ComputePublicMarketService.referencePrice(free, referenceNode()); assertNull(free.getReferencePrice());
    }
    @Test void referenceJsonContainsOnlySafeDatedDisplayFields() throws Exception {
        String json = new ObjectMapper().writeValueAsString(referenceOffer(referenceNode()));
        com.fasterxml.jackson.databind.JsonNode body = new ObjectMapper().readTree(json);
        assertEquals(14, body.size());
        assertEquals("12.92", body.path("referencePrice").asText());
        assertEquals("CNY", body.path("referenceCurrency").asText());
        assertEquals("PER_INSTANCE_HOUR", body.path("referenceUnit").asText());
        assertEquals("2026-10-19", body.path("referenceDate").asText());
        for (String forbidden : new String[]{"792022144331380001", "pro-7920a22db144", "provider", "serverGroup", "host", "password", "tenant", "owner", "cost"}) assertFalse(json.contains(forbidden), forbidden);
        assertNull(referenceOffer(referenceNode()).getPrice());
        assertFalse(referenceOffer(referenceNode()).isPaidUseEnabled());
    }

}
