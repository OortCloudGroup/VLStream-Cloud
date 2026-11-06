package com.ruoyi.web.controller.compat.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingLifecycleService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingPolicy;
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
    ComputeBillingSettlementService settlements;ComputeBillingLifecycleService lifecycle;
    ObjectProvider<NewApiBillingClient> clients;
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        authorization=mock(ComputePlatformAuthorizationService.class);client=mock(NewApiBillingClient.class);
        clients=mock(ObjectProvider.class);when(clients.getIfAvailable()).thenReturn(client);
        settlements=mock(ComputeBillingSettlementService.class);lifecycle=mock(ComputeBillingLifecycleService.class);
        controller=new ComputeBillingBindingController(authorization,clients,settlements,lifecycle);
        properties=new ExternalBillingProperties();paid=mock(ComputePaidRunService.class);accounts=mock(ComputeBillingAccountService.class);
        configureBilling();
        ReflectionTestUtils.setField(controller,"properties",properties);ReflectionTestUtils.setField(controller,"paidRuns",paid);ReflectionTestUtils.setField(controller,"accounts",accounts);
        ReflectionTestUtils.setField(controller,"billingPolicy",new ComputeBillingPolicy());
        ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);actor.setVlsUserId("current-actor");actor.setVlsTenantId("current-tenant");actor.setPlatformUserId("subject-not-payer-id");
        payer=BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":13}"));when(authorization.billingPayer(client)).thenReturn(payer);
    }
    @Test void capabilityReadDoesNotContactWalletOrResolveAnotherIdentity() {
        properties.setBaseUrl(null);
        java.util.Map<String,Object> view=controller.capabilities().getData();
        assertEquals(false,view.get("identityAvailable"));
        assertEquals(false,view.get("accountAvailable"));
        assertEquals(false,view.get("paidAdmissionEnabled"));
        assertEquals("平台计费缺配置：base-url 服务地址",view.get("unavailableReason"));
        assertFalse(view.containsKey("mode"));
        verifyNoInteractions(client,authorization,accounts,paid);
        assertNotNull(ComputeBillingBindingController.class.getAnnotation(cn.dev33.satoken.annotation.SaCheckLogin.class));
    }
    @Test void realMerchantAndSafetyConfigurationMakesBillingAvailableWithoutDeploymentSwitches() {
        configureBilling();
        java.util.Map<String,Object> view=controller.capabilities().getData();
        assertEquals(true,view.get("identityAvailable"));
        assertEquals(true,view.get("accountAvailable"));
        assertEquals(true,view.get("paidAdmissionEnabled"));
        assertNull(view.get("unavailableReason"));
        assertFalse(view.containsKey("mode"));
        verifyNoInteractions(client,authorization,accounts,paid);
    }
    @Test void invalidNewRunSafetyPolicyKeepsAccountAndOriginalRunAccess() {
        configureBilling();
        properties.setHoldStopLeadSeconds(120);
        java.util.Map<String,Object> view=controller.capabilities().getData();
        assertEquals(true,view.get("accountAvailable"));
        assertEquals(false,view.get("paidAdmissionEnabled"));
        assertEquals("余额到期停机提前量未覆盖可靠停机预算",view.get("unavailableReason"));
        controller.runs();controller.stop("original-run");
        verify(paid).list(payer);verify(paid).stop("original-run",payer);
        verifyNoInteractions(client,accounts);
    }
    @Test void missingClientCannotAdvertiseAccountOrPaidAvailability() {
        configureBilling();when(clients.getIfAvailable()).thenReturn(null);
        java.util.Map<String,Object> view=controller.capabilities().getData();
        assertEquals(false,view.get("accountAvailable"));
        assertEquals(false,view.get("paidAdmissionEnabled"));
        assertEquals("平台计费客户端不可用，请检查服务启动错误",view.get("unavailableReason"));
        assertThrows(ServiceException.class,()->controller.quote(1L));
        verifyNoInteractions(client,authorization,accounts,paid);
    }
    @Test void baseOnlyConfigurationRetainsVerifiedIdentityAndOriginalRunAccessWithoutMerchantCredentials() {
        properties.setMerchantId(null);properties.setApiKey(null);properties.setMaximumRunSeconds(0);
        java.util.Map<String,Object> view=controller.capabilities().getData();
        assertEquals(true,view.get("identityAvailable"));
        assertEquals(false,view.get("accountAvailable"));assertEquals(false,view.get("paidAdmissionEnabled"));
        assertSame(payer,controller.identity().getData());
        controller.runs();controller.run("original-run");controller.stop("original-run");
        verify(authorization,times(4)).billingPayer(client);
        verify(paid).list(payer);verify(paid).get("original-run",payer);verify(paid).stop("original-run",payer);
        assertEquals("13",payer.userId);assertEquals("subject-not-payer-id",payer.platformSubject);
        assertEquals("current-tenant",payer.tenantId);assertEquals("current-actor",payer.actorId);
        verifyNoInteractions(client,accounts,settlements,lifecycle);
    }
    @Test void baseOnlyConfigurationStillRejectsEveryNewMerchantOperationBeforeIdentityOrWalletCalls() {
        properties.setMerchantId(null);properties.setApiKey(null);
        for(org.junit.jupiter.api.function.Executable operation:newOperations())assertThrows(ServiceException.class,operation);
        verifyNoInteractions(client,authorization,accounts,paid,settlements,lifecycle);
    }
    @Test void absentIdentityServiceAddressCannotResolveSelfOrReadOrStopOriginalRuns() {
        properties.setBaseUrl(null);properties.setMerchantId(null);properties.setApiKey(null);
        assertThrows(ServiceException.class,()->controller.identity());
        assertThrows(ServiceException.class,()->controller.runs());
        assertThrows(ServiceException.class,()->controller.run("original-run"));
        assertThrows(ServiceException.class,()->controller.stop("original-run"));
        verifyNoInteractions(client,authorization,accounts,paid);
    }
    @Test void missingMerchantCredentialsNeverBypassFailedCurrentSelfVerificationForOriginalRuns() {
        properties.setMerchantId(null);properties.setApiKey(null);
        ServiceException unavailable=new ServiceException("Current authoritative self unavailable");
        when(authorization.billingPayer(client)).thenThrow(unavailable);
        assertSame(unavailable,assertThrows(ServiceException.class,()->controller.identity()));
        assertSame(unavailable,assertThrows(ServiceException.class,()->controller.runs()));
        assertSame(unavailable,assertThrows(ServiceException.class,()->controller.run("original-run")));
        assertSame(unavailable,assertThrows(ServiceException.class,()->controller.stop("original-run")));
        verify(authorization,times(4)).billingPayer(client);
        verifyNoInteractions(client,accounts,paid);
    }
    private void configureBilling() {
        properties.setBaseUrl("https://billing-fixture.invalid");properties.setMerchantId("fixture-merchant");
        properties.setApiKey("fixture-merchant-token");properties.setMaximumHoldQuota("1000000");
        properties.setMaximumRunSeconds(600);properties.setVerifiedProviderStopSeconds(60);
        properties.setHoldStopLeadSeconds(300);
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
    @Test void archiveRecoveryQuoteUsesOnlyCurrentSelfAndTheOriginalRunWithoutFunding() throws Exception {
        com.fasterxml.jackson.databind.JsonNode config=json.readTree("{\"conversion_version\":\"fixture-recovery\"}");
        when(client.config()).thenReturn(config);
        controller.archiveRecoveryQuote("original-run");
        verify(paid).quoteArchiveRecovery("original-run",payer,config);
        verify(paid,never()).accept(anyString(),any());
        verify(client,never()).balance(anyString());
        verifyNoInteractions(accounts);
        org.springframework.web.bind.annotation.PostMapping route=ComputeBillingBindingController.class
            .getMethod("archiveRecoveryQuote",String.class).getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
        assertArrayEquals(new String[]{"/runs/{sourceRunId}/archive-recovery-quote"},route.value());
    }
    @Test void failedCurrentIdentityPreventsArchiveRecoveryQuoteAndConfigurationRead() {
        when(authorization.billingPayer(client)).thenThrow(new ServiceException("Current identity unavailable"));
        assertThrows(ServiceException.class,()->controller.archiveRecoveryQuote("original-run"));
        verifyNoInteractions(paid,accounts);
        verify(client,never()).config();
    }
    @Test void devTargetWithoutAnEnabledTrialRejectsEveryNewOperationBeforeAnyWalletCall() {
        properties.setBaseUrl("https://workup-dev.myoumuamua.com:6433/bus/apaas-newapi");
        assertNewOperationsDenied();
    }
    @Test void enabledButIncompleteTrialCannotFallBackToNormalAdmission() {
        properties.setBaseUrl("https://billing-fixture.invalid");
        properties.getTrial().setEnabled(true);
        assertNewOperationsDenied();
    }
    @Test void trialRunMarkerCannotBypassScopeByDisablingTheTrialFlag() {
        properties.setBaseUrl("https://billing-fixture.invalid");
        properties.getTrial().setRunId("19cc5e81-70cd-4f96-8959-665bc67c945d");
        assertNewOperationsDenied();
    }
    @Test void validTrialRejectsAnotherAuthoritativeSelfBeforeAnyNewAccountOrRunOperation() throws Exception {
        validTrial();usePayer("14");
        assertNewOperationsDenied();
    }
    @Test void validTrialAllowsOnlyTheVerifiedPayerThroughAllFiveNewOperationEntrypoints() throws Exception {
        validTrial();
        assertNewOperationsAllowed();
    }
    @Test void expiredTrialDoesNotPreventOriginalRunLookupListStopOrIdentityRead() {
        validTrial();properties.getTrial().setExpiresAt("2000-01-01T00:00:00Z");
        assertNewOperationsDenied();
        controller.runs();controller.run("original-run");controller.stop("original-run");
        assertSame(payer,controller.identity().getData());
        verify(paid).list(payer);verify(paid).get("original-run",payer);verify(paid).stop("original-run",payer);
        verifyNoInteractions(client,accounts,settlements,lifecycle);
    }
    @Test void invalidTrialDoesNotReplaceNormalSelfVerificationOrItsFailure() {
        properties.getTrial().setEnabled(true);
        ServiceException unavailable=new ServiceException("Current authoritative self unavailable");
        when(authorization.billingPayer(client)).thenThrow(unavailable);
        for(org.junit.jupiter.api.function.Executable operation:newOperations()){
            assertSame(unavailable,assertThrows(ServiceException.class,operation));
        }
        verify(authorization,times(5)).billingPayer(client);
        verifyNoInteractions(client,accounts,paid,settlements,lifecycle);
    }
    @Test void normalNonDevConfigurationKeepsExistingBehaviorForOtherVerifiedPayers() throws Exception {
        properties.setBaseUrl("https://billing-fixture.invalid");usePayer("37");
        assertNewOperationsAllowed();
    }
    private void validTrial(){
        properties.setBaseUrl("https://workup-dev.myoumuamua.com:6433/bus/apaas-newapi");
        properties.setMerchantId("vls");
        properties.getTrial().setEnabled(true);
        properties.getTrial().setRunId("19cc5e81-70cd-4f96-8959-665bc67c945d");
        properties.getTrial().setMaximumWalletCny("3.47");
        properties.getTrial().setExpiresAt(java.time.Instant.now().plusSeconds(3600).toString());
    }
    private void usePayer(String id) throws Exception {
        ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();
        actor.setAuthorized(true);actor.setVlsUserId("current-actor");actor.setVlsTenantId("current-tenant");
        actor.setPlatformUserId("subject-not-payer-id");
        payer=BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"data\":{\"id\":\""+id+"\"},\"success\":true}"));
        when(authorization.billingPayer(client)).thenReturn(payer);
    }
    private ComputeBillingBindingController.AccountCheck consent(){
        ComputeBillingBindingController.AccountCheck request=new ComputeBillingBindingController.AccountCheck();
        request.acceptBillingPermissions=true;return request;
    }
    private org.junit.jupiter.api.function.Executable[] newOperations(){
        return new org.junit.jupiter.api.function.Executable[]{
            ()->controller.account(consent()),()->controller.quote(1L),
            ()->controller.archiveRecoveryQuote("original-run"),
            ()->controller.accept("quoted-run",consent()),()->controller.bind("usage-test")
        };
    }
    private void assertNewOperationsDenied(){
        for(org.junit.jupiter.api.function.Executable operation:newOperations()){
            assertThrows(RuntimeException.class,operation);
        }
        verify(authorization,times(5)).billingPayer(client);
        verifyNoInteractions(client,accounts,paid,settlements,lifecycle);
    }
    private void assertNewOperationsAllowed() throws Exception {
        com.fasterxml.jackson.databind.JsonNode config=json.readTree("{\"conversion_version\":\"controller-fixture\",\"quota_unit\":\"quota\",\"settlement_currency\":\"CNY\",\"quota_per_currency_unit\":{\"numerator\":\"500000\",\"denominator\":\"1\"}}");
        when(client.config()).thenReturn(config);
        ExternalBillingGateway fixture=mock(ExternalBillingGateway.class);
        com.fasterxml.jackson.databind.JsonNode balance=json.readTree("{\"user_id\":\""+payer.userId+"\",\"balance_quota\":\"5\",\"held_quota\":\"0\",\"available_quota\":\"5\",\"balance_version\":\"0\"}");
        when(fixture.balance(payer.userId)).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"fixture",balance));
        ComputeBillingAccountService.Account account=new ComputeBillingAccountService().check(payer,fixture,config,true);
        when(accounts.check(payer,client,config,true)).thenReturn(account);
        ComputePaidRunService.View quote=quotedView();
        when(paid.get("quoted-run",payer)).thenReturn(quote);
        controller.account(consent());controller.quote(1L);controller.archiveRecoveryQuote("original-run");
        controller.accept("quoted-run",consent());controller.bind("usage-test");
        verify(authorization,times(5)).billingPayer(client);
        verify(accounts,times(2)).check(payer,client,config,true);
        verify(paid).quote(1L,payer,config);verify(paid).quoteArchiveRecovery("original-run",payer,config);
        verify(paid).accept("quoted-run",payer);verify(settlements).bindAcceptedUsage("usage-test",payer,config);
        verify(lifecycle).arm("usage-test",payer);
        verify(client,never()).balance(anyString());
    }
    private ComputePaidRunService.View quotedView() throws Exception {
        java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();
        row.put("run_id","quoted-run");row.put("node_id","1");row.put("state","QUOTED");row.put("reserve_quota","5");
        row.put("quote_until_ms","0");row.put("hold_until_ms","0");row.put("maximum_run_seconds","60");
        BillingSnapshot snapshot=BillingSnapshot.read("{\"price_snapshot_id\":\"fixture\",\"conversion_version\":\"controller-fixture\",\"quota_unit\":\"quota\",\"approved_hourly_price\":\"13\",\"numerator\":\"500000\",\"denominator\":\"1\",\"effective_at\":\"2020-01-01T00:00:00Z\",\"valid_until\":\"2030-01-01T00:00:00Z\",\"accepted_rate\":{\"currency\":\"CNY\"}}",json);
        java.lang.reflect.Constructor<ComputePaidRunService.View> constructor=ComputePaidRunService.View.class.getDeclaredConstructor(
            java.util.Map.class,BillingSnapshot.class,String.class,String.class,boolean.class,java.util.List.class,String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(row,snapshot,"0","0",false,java.util.Collections.emptyList(),null);
    }
}
