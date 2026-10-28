package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeBillingAccountServiceTest {
    final ObjectMapper json=new ObjectMapper();
    final ComputeBillingAccountService service=new ComputeBillingAccountService();
    BillingPayer payer() throws Exception {
        ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);
        actor.setVlsUserId("actor-test");actor.setVlsTenantId("tenant-test");actor.setPlatformUserId("subject-is-not-payer");
        return BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"success\":true,\"data\":{\"id\":13}}"));
    }
    JsonNode config() throws Exception {return json.readTree("{\"quota_unit\":\"quota\",\"settlement_currency\":\"CNY\",\"conversion_version\":\"v1\",\"quota_per_currency_unit\":{\"numerator\":\"500000\",\"denominator\":\"1\"}}");}
    @Test void explicitPermissionPrecedesAnyPotentialAutoGrantRequest() throws Exception {
        ExternalBillingGateway gateway=mock(ExternalBillingGateway.class);
        assertThrows(IllegalArgumentException.class,()->service.check(payer(),gateway,config(),false));
        verifyNoInteractions(gateway);
    }
    @Test void walletUsesSelfIdAndPreservesLargeValuesWithoutLeakingTheUpstreamBody() throws Exception {
        ExternalBillingGateway gateway=mock(ExternalBillingGateway.class);
        JsonNode body=json.readTree("{\"user_id\":13,\"balance_quota\":\"9007199254740993\",\"held_quota\":\"5\",\"available_quota\":\"9007199254740988\",\"balance_version\":\"10\",\"unexpected_secret\":\"TEST_FIXTURE_ONLY\"}");
        when(gateway.balance("13")).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",body));
        ComputeBillingAccountService.Account account=service.check(payer(),gateway,config(),true);
        assertEquals("13",account.getUserId());assertEquals("9007199254740988",account.getAvailableQuota());
        assertEquals("10",account.getBalanceVersion());assertFalse(json.writeValueAsString(account).contains("unexpected_secret"));
        verify(gateway).balance("13");
    }
    @Test void deniedUnknownForeignAndInconsistentResponsesRemainFailures() throws Exception {
        ExternalBillingGateway gateway=mock(ExternalBillingGateway.class);
        for(ExternalBillingGateway.Status status:new ExternalBillingGateway.Status[]{ExternalBillingGateway.Status.NOT_AUTHORIZED,ExternalBillingGateway.Status.UNKNOWN}){
            when(gateway.balance("13")).thenReturn(ExternalBillingGateway.Result.of(status));
            assertThrows(IllegalStateException.class,()->service.check(payer(),gateway,config(),true));
        }
        for(String body:new String[]{"{\"user_id\":14}","{\"user_id\":13,\"balance_quota\":\"5\",\"held_quota\":\"1\",\"available_quota\":\"5\",\"balance_version\":0}"}){
            when(gateway.balance("13")).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",json.readTree(body)));
            assertThrows(IllegalArgumentException.class,()->service.check(payer(),gateway,config(),true));
        }
    }
}
