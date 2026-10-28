package com.ruoyi.web.controller.compat.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLifecycleService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePaidRunService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeBillingProductControllerTest {
    ComputeBillingBindingController controller;ComputePlatformAuthorizationService authorization;
    NewApiBillingClient client;ExternalBillingProperties properties;ComputePaidRunService paid;
    ComputeBillingAccountService accounts;BillingPayer payer;ObjectMapper json=new ObjectMapper();
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        authorization=mock(ComputePlatformAuthorizationService.class);client=mock(NewApiBillingClient.class);
        ObjectProvider<NewApiBillingClient> clients=mock(ObjectProvider.class);when(clients.getIfAvailable()).thenReturn(client);
        controller=new ComputeBillingBindingController(authorization,clients,mock(ComputeBillingSettlementService.class),mock(ComputeBillingLifecycleService.class));
        properties=new ExternalBillingProperties();paid=mock(ComputePaidRunService.class);accounts=mock(ComputeBillingAccountService.class);
        ReflectionTestUtils.setField(controller,"properties",properties);ReflectionTestUtils.setField(controller,"paidRuns",paid);ReflectionTestUtils.setField(controller,"accounts",accounts);
        ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);actor.setVlsUserId("current-actor");actor.setVlsTenantId("current-tenant");actor.setPlatformUserId("subject-not-payer-id");
        payer=BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":13}"));when(authorization.billingPayer(client)).thenReturn(payer);
    }
    @Test void capabilityReadDoesNotContactWalletOrResolveAnotherIdentity() {
        assertEquals(false,controller.capabilities().getData().get("accountAvailable"));
        verifyNoInteractions(client,authorization,accounts,paid);
        assertNotNull(ComputeBillingBindingController.class.getAnnotation(cn.dev33.satoken.annotation.SaCheckLogin.class));
    }
    @Test void absentConsentStopsBeforeSelfConfigOrBalance() {
        assertThrows(ServiceException.class,()->controller.account(new ComputeBillingBindingController.AccountCheck()));
        assertThrows(ServiceException.class,()->controller.accept("run-test",new ComputeBillingBindingController.AccountCheck()));
        verifyNoInteractions(client,authorization,accounts,paid);
    }
    @Test void quoteUsesBackendSelfAndConfigurationWithoutAnImplicitBalanceCall() throws Exception {
        com.fasterxml.jackson.databind.JsonNode config=json.readTree("{\"conversion_version\":\"fixture\"}");when(client.config()).thenReturn(config);
        controller.quote(1L);verify(paid).quote(1L,payer,config);verify(client,never()).balance(anyString());verifyNoInteractions(accounts);
    }
    @Test void originalRunQueriesAndStopUseTheCurrentVerifiedPayer() {
        controller.run("original-run");controller.stop("original-run");
        verify(paid).get("original-run",payer);verify(paid).stop("original-run",payer);verify(client,never()).capture(anyString(),anyString());
    }
}
