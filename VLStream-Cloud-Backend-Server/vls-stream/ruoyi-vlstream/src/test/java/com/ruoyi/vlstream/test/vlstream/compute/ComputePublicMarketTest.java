package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;
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
    @Test void originalCompanyOfferShowsFixedPriceEvenWithoutExecutionCatalog() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
            "jdbc:h2:mem:legacy_company_price_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE vls_compute_node(id BIGINT PRIMARY KEY, name VARCHAR(100), gpu_model VARCHAR(100), "
            + "provider VARCHAR(32), provider_product VARCHAR(32), provider_instance_id VARCHAR(100), "
            + "resource_scope VARCHAR(32), tenant_id VARCHAR(64), billing_type VARCHAR(32), points_per_hour DECIMAL(18,4))");
        jdbc.execute("CREATE TABLE vls_company_compute_offer(offer_code VARCHAR(100), node_id BIGINT, provider VARCHAR(32), "
            + "provider_instance_id VARCHAR(100), published INT)");
        jdbc.update("INSERT INTO vls_compute_node VALUES "
            + "(792022144331380001,'Renamed resource','NVIDIA RTX 6000D','autodl','AUTODL_PRO','pro-7920a22db144','PLATFORM','000000','POINTS',NULL),"
            + "(9,'Another RTX 6000D','NVIDIA RTX 6000D','autodl','AUTODL_PRO','pro-123456abcdef','PLATFORM','000000','POINTS',NULL)");
        jdbc.update("INSERT INTO vls_company_compute_offer VALUES ("
            + "'a-original',792022144331380001,'autodl','pro-7920a22db144',1),('b-other',9,'autodl','pro-123456abcdef',1)");
        CompanyComputeOfferService service = new CompanyComputeOfferService(jdbc, mock(CompanyComputeOfferAuthority.class));
        List<ComputeMarketService.Resource> resources = service.marketResources(20, 0);
        assertEquals("13.00", resources.get(0).getUserHourlyPrice());
        assertEquals("CNY", resources.get(0).getPriceCurrency());
        assertEquals("PER_INSTANCE_HOUR", resources.get(0).getPriceUnit());
        assertEquals("VLS_FIXED", resources.get(0).getPricePolicy());
        assertEquals("PENDING_CONFIGURATION", resources.get(0).getClaimState());
        assertFalse(resources.get(0).isPaidAdmissionEnabled());
        assertFalse(resources.get(0).isSchedulable());
        assertNull(resources.get(1).getUserHourlyPrice());

        List<ComputePublicMarketService.Offer> offers = service.publicMarketOffers(20, 0);
        assertEquals(new BigDecimal("13.00"), offers.get(0).getPrice());
        assertEquals("CNY", offers.get(0).getPriceCurrency());
        assertEquals("PAID_DISABLED", offers.get(0).getAvailability());
        assertFalse(offers.get(0).isPaidUseEnabled());
        assertNull(offers.get(1).getPrice());
        String json = new ObjectMapper().writeValueAsString(offers.get(0));
        for (String forbidden : new String[]{"792022144331380001", "pro-7920a22db144", "provider", "serverGroup", "host", "password", "tenant", "owner", "cost"}) assertFalse(json.contains(forbidden), forbidden);

        jdbc.update("UPDATE vls_compute_node SET points_per_hour=8 WHERE id=9");
        ComputePublicMarketService.Offer other = service.publicMarketOffers(20, 0).get(1);
        assertEquals(0, new BigDecimal("8").compareTo(other.getPrice()));
        assertEquals("POINTS_PER_HOUR", other.getPriceUnit());
    }

    private ComputeNode configuredNode(String billingType, String schedulingMode) {
        ComputeNode node = new ComputeNode();
        node.setName("Configured GPU"); node.setGpuModel("RTX 6000D");
        node.setBillingType(billingType); node.setSchedulingMode(schedulingMode);
        node.setPowerState("RUNNING"); node.setPowerCheckedAt(new java.util.Date());
        node.setProbeState("READY"); node.setProbeTime(new java.util.Date());
        return node;
    }

    private ComputeNode officialNode() {
        ComputeNode node = configuredNode("POINTS", "INSTANCE_EXCLUSIVE");
        node.setPricePolicy("VLS_FIXED"); node.setPriceUnit("PER_INSTANCE_HOUR");
        node.setPriceCurrency("CNY"); node.setUserHourlyPrice("13.00");
        node.setPointsPerHour(new BigDecimal("999999"));
        return node;
    }

    private ComputePublicMarketService.Page configuredOffers(ComputeNode... nodes) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ConfiguredComputeRegistry registry = mock(ConfiguredComputeRegistry.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(jdbc.query(anyString(), any(RowMapper.class), eq(20), eq(0))).thenReturn(new ArrayList<ComputePublicMarketService.Offer>());
        when(registry.allIds()).thenReturn(Collections.emptyList());
        when(registry.published()).thenReturn(Arrays.asList(nodes));
        ComputePublicMarketService service = new ComputePublicMarketService(jdbc);
        ReflectionTestUtils.setField(service, "configured", registry);
        ComputePublicMarketService.Page result = service.list(1, 20);
        verify(jdbc).queryForObject(anyString(), eq(Long.class));
        verify(jdbc).query(anyString(), any(RowMapper.class), eq(20), eq(0));
        verifyNoMoreInteractions(jdbc);
        return result;
    }

    @Test void configuredOfficialPriceCoexistsWithTwoFreeQueuesWithoutEnablingPaidUse() {
        ComputeNode freeOne = configuredNode("FREE", ComputeSharedQueueService.MODE);
        ComputeNode freeTwo = configuredNode("FREE", ComputeSharedQueueService.MODE);
        ComputePublicMarketService.Page page = configuredOffers(freeOne, freeTwo, officialNode());
        assertEquals(3, page.getTotal()); assertEquals(3, page.getItems().size());
        for (int i = 0; i < 2; i++) {
            ComputePublicMarketService.Offer free = page.getItems().get(i);
            assertEquals("FREE", free.getBillingType()); assertEquals(ComputeSharedQueueService.MODE, free.getSchedulingMode());
            assertEquals("QUEUE", free.getAvailability()); assertNull(free.getPrice()); assertNull(free.getPricePolicy());
        }
        ComputePublicMarketService.Offer official = page.getItems().get(2);
        assertEquals("POINTS", official.getBillingType()); assertEquals("INSTANCE_EXCLUSIVE", official.getSchedulingMode());
        assertEquals(new BigDecimal("13.00"), official.getPrice()); assertEquals("13.00", official.getUserHourlyPrice());
        assertEquals("CNY", official.getPriceCurrency()); assertEquals("PER_INSTANCE_HOUR", official.getPriceUnit());
        assertEquals("VLS_FIXED", official.getPricePolicy()); assertEquals("PAID_DISABLED", official.getAvailability());
        assertFalse(official.isPaidUseEnabled()); assertNull(official.getReferencePrice());
    }

    @Test void missingOrInvalidOfficialPriceRemainsPaidWithoutInventingAFreeOrQuotaPrice() {
        for (String invalid : new String[]{null, "", "0", "-1", "NaN", "13.0000000000001"}) {
            ComputeNode node = officialNode(); node.setUserHourlyPrice(invalid);
            ComputePublicMarketService.Offer offer = configuredOffers(node).getItems().get(0);
            assertEquals("POINTS", offer.getBillingType()); assertEquals("PAID_DISABLED", offer.getAvailability());
            assertNull(offer.getPrice()); assertNull(offer.getUserHourlyPrice()); assertNull(offer.getPricePolicy());
            assertFalse(offer.isPaidUseEnabled());
        }
        ComputeNode wrongPolicy = officialNode(); wrongPolicy.setPricePolicy("SUPPLIER_COST");
        ComputeNode wrongUnit = officialNode(); wrongUnit.setPriceUnit("PER_GPU_HOUR");
        ComputeNode wrongCurrency = officialNode(); wrongCurrency.setPriceCurrency("invalid");
        for (ComputeNode node : new ComputeNode[]{wrongPolicy, wrongUnit, wrongCurrency})
            assertNull(configuredOffers(node).getItems().get(0).getPrice());
    }

    @Test void configuredOfficialJsonOnlyAddsPublicFixedPriceFields() throws Exception {
        ComputeNode node = officialNode(); node.setHost("private-host.example.internal"); node.setUsername("private-user");
        node.setTenantId("private-tenant"); node.setProviderInstanceId("private-instance"); node.setHostKey("private-host-key");
        String json = new ObjectMapper().writeValueAsString(configuredOffers(node).getItems().get(0));
        com.fasterxml.jackson.databind.JsonNode body = new ObjectMapper().readTree(json);
        assertEquals(13, body.size());
        assertEquals("13.00", body.path("userHourlyPrice").asText()); assertEquals("CNY", body.path("priceCurrency").asText());
        for (String forbidden : new String[]{"private-", "host", "username", "tenant", "instance", "provider", "token", "password", "cost"})
            assertFalse(json.contains(forbidden), forbidden);
    }

}
