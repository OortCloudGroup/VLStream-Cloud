package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Official documented response shapes, synthetic credentials, no live network. */
@Tag("dev")
class AutoDlProStatusContractTest {
    AutoDlProPowerProvider provider;
    ComputeNode node;
    MockRestServiceServer transport;

    @BeforeEach void setup() {
        RestTemplate http=AutoDlProPowerProvider.transport();transport=MockRestServiceServer.bindTo(http).build();
        ComputeCredentialCipher cipher=mock(ComputeCredentialCipher.class);
        when(cipher.decrypt("fixture-cipher")).thenReturn("{\"token\":\"SYNTHETIC_TEST_ONLY\"}");
        provider=new AutoDlProPowerProvider(cipher,true,http);
        node=new ComputeNode();node.setProvider("autodl");node.setProviderProduct("AUTODL_PRO");node.setControlMode("API");
        node.setProviderInstanceId("pro-fixture");node.setProviderCredentialCipher("fixture-cipher");
        ConfiguredComputeRegistry configured=mock(ConfiguredComputeRegistry.class);
        when(configured.providerTokenPresent(any())).thenReturn(true);when(configured.providerToken(any())).thenReturn("SYNTHETIC_TEST_ONLY");
        org.springframework.test.util.ReflectionTestUtils.setField(provider,"configured",configured);
    }
    @AfterEach void verifyTransport(){transport.verify();}
    void state(String value) {
        transport.expect(requestTo(AutoDlProPowerProvider.BASE+"status")).andExpect(method(HttpMethod.GET))
            .andExpect(content().json("{\"instance_uuid\":\"pro-fixture\"}"))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":\""+value+"\"}",MediaType.APPLICATION_JSON));
    }
    String item(String id,String status) {
        return "{\"uuid\":\""+id+"\",\"status\":\""+status+"\",\"req_gpu_amount\":1,\"charge_type\":\"payg\","
            +"\"stopped_at\":{\"Valid\":true,\"Time\":\"2026-10-28T12:00:00+08:00\"}}";
    }
    void page(String... items) {
        transport.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"page_index\":1,\"page_size\":20}"))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":{\"page_index\":1,\"max_page\":1,\"result_total\":"+items.length+",\"list\":["+String.join(",",items)+"]}}",MediaType.APPLICATION_JSON));
    }
    @Test void statusUsesMatchingListMetadataWithoutFetchingTheSecretBearingSnapshot() {
        state("running");page(item("pro-fixture","running"));
        ComputePowerProvider.Snapshot result=provider.status(node);
        assertEquals("RUNNING",result.power);assertEquals(Integer.valueOf(1),result.gpuAmount);assertEquals("payg",result.chargeType);
        assertNull(result.stoppedMode);assertFalse(ComputeBillingPolicy.computeStopConfirmed(node,result));
    }
    @Test void stoppedLiteralAndHistoricalTimestampCannotCreateUndocumentedStopProof() {
        state("stopped");page(item("pro-fixture","stopped"));
        ComputePowerProvider.Snapshot result=provider.status(node);
        assertEquals("UNKNOWN",result.power);assertEquals("payg",result.chargeType);assertNull(result.stoppedMode);
        assertFalse(ComputeBillingPolicy.computeStopConfirmed(node,result));
        assertThrows(RuntimeException.class,()->ComputeBillingPolicy.requireEconomicalPaidOffer(node,System.currentTimeMillis()));
    }
    @Test void documentedCanonicalShutdownConfirmsOnlyOfficialPhysicalStopNotFreeSupplierStorage() {
        state("shutdown");page(item("pro-fixture","shutdown"));node.setConfigurationManaged(true);
        ComputePowerProvider.Snapshot result=provider.status(node);
        assertEquals("STOPPED",result.power);assertEquals("AUTODL_PRO_PHYSICAL_STOPPED",result.stoppedMode);
        assertTrue(ComputeBillingPolicy.computeStopConfirmed(node,result));
        node.setConfigurationManaged(false);assertFalse(ComputeBillingPolicy.computeStopConfirmed(node,result));
    }
    @Test void supplierPrepaymentDoesNotExtendOfficialUserBillingAfterPhysicalShutdown() {
        state("shutdown");page(item("pro-fixture","shutdown").replace("\"payg\"","\"prepaid\""));node.setConfigurationManaged(true);
        ComputePowerProvider.Snapshot result=provider.status(node);
        assertEquals("prepaid",result.chargeType);assertTrue(ComputeBillingPolicy.computeStopConfirmed(node,result));
    }
    @Test void mismatchedShutdownDoesNotBecomeAStopOrAReleaseProof() {
        state("shutdown");page(item("pro-fixture","starting"));
        ComputePowerProvider.Snapshot result=provider.status(node);assertEquals("UNKNOWN",result.power);assertNull(result.stoppedMode);
    }
    @Test void proTransitionalStatesAreSeparateFromRunningAndStopped() {
        state("starting");page(item("pro-fixture","starting"));state("shutting_down");page(item("pro-fixture","shutting_down"));
        assertEquals("STARTING",provider.status(node).power);assertEquals("STOPPING",provider.status(node).power);
    }
    @Test void inconsistentOrMissingInstanceMetadataCannotBorrowAnotherInstancesBillingType() {
        state("running");page(item("pro-other","running"));
        ComputePowerProvider.Snapshot result=provider.status(node);
        assertEquals("RUNNING",result.power);assertNull(result.chargeType);assertNull(result.gpuAmount);
    }
    @Test void conflictingStatusDoesNotSupplyGpuOrChargeTypeEvidence() {
        state("running");page(item("pro-fixture","different"));
        ComputePowerProvider.Snapshot result=provider.status(node);
        assertNull(result.gpuAmount);assertNull(result.chargeType);
    }
    @Test void duplicateIdentityFailsClosedInsteadOfSelectingTheFirstRecord() {
        state("running");page(item("pro-fixture","running"),item("pro-fixture","stopped"));
        assertThrows(ComputeProviderException.class,()->provider.status(node));
    }
    @Test void acceptedPowerOffIsNotAStoppedObservationAndNoBlindRetryIsInstalled() {
        transport.expect(requestTo(AutoDlProPowerProvider.BASE+"power_off")).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":null}",MediaType.APPLICATION_JSON));
        state("not-confirmed");page(item("pro-fixture","not-confirmed"));
        provider.submit(node,"stop","fixture-original-stop");
        assertEquals("UNKNOWN",provider.status(node).power);
    }
    @Test void rawPriceCannotBecomeCurrencyHourlyCostOrAUserLedgerRate() throws Exception {
        transport.expect(requestTo(AutoDlProPowerProvider.BASE+"snapshot")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":{\"payg_price\":1970,\"origin_pay_price\":3030,\"root_password\":\"SYNTHETIC_SENSITIVE_FIELD\"}}",MediaType.APPLICATION_JSON));
        ComputeCostProvider.Observation price=provider.cost(node);
        assertEquals(0,new BigDecimal("1970").compareTo(price.getAmount()));assertFalse(price.isBillableCost());
        assertEquals("UNKNOWN",price.getCurrency());assertEquals("UNKNOWN",price.getAmountUnit());assertEquals("UNKNOWN",price.getResourceScope());
        assertFalse(new ObjectMapper().writeValueAsString(price).contains("SYNTHETIC_SENSITIVE_FIELD"));
    }
}
