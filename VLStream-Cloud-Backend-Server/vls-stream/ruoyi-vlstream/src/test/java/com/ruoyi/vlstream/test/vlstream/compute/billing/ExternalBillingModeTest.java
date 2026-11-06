package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Configuration readiness replaces deployment activation modes. No network is used. */
@Tag("dev")
class ExternalBillingModeTest {
    static ExternalBillingProperties configured() {
        ExternalBillingProperties p=new ExternalBillingProperties();
        p.setBaseUrl("https://billing.example.test");p.setMerchantId("fixture");p.setApiKey("unit-test-placeholder-not-real");
        p.setMaximumRunSeconds(60);p.setMaximumHoldQuota("100");p.setVerifiedProviderStopSeconds(30);return p;
    }
    @Test void absentMerchantConfigurationDoesNotAuthorizeAnyTransportOrMoney() {
        ExternalBillingProperties p=new ExternalBillingProperties();assertDoesNotThrow(p::validateDeployment);
        assertFalse(p.hasIdentityConfiguration());assertFalse(p.hasMerchantConfiguration());assertFalse(p.paidConfigured());assertTrue(p.unavailableReason().contains("缺配置"));
        p.setApiKey("unit-test-placeholder-not-real");assertFalse(p.hasMerchantConfiguration());
        NewApiBillingClient client=new NewApiBillingClient(p,new ObjectMapper());
        assertFalse(client.supportsAtomicCapture());assertThrows(IllegalStateException.class,client::config);
        assertThrows(IllegalStateException.class,()->client.createHold("original-key","{}"));
    }
    @Test void completeConfigurationMakesPaidAdmissionAvailableWithoutAnActivationFlag() {
        ExternalBillingProperties p=configured();assertDoesNotThrow(p::validateDeployment);
        assertTrue(p.hasMerchantConfiguration());assertTrue(p.paidConfigured());assertNull(p.unavailableReason());
        assertFalse(p.getTrial().isEnabled());
    }
    @Test void firstBootstrapCanUseExplicitBudgetsWithoutPriorSupplierStopObservations() {
        ExternalBillingProperties p=configured();p.setVerifiedProviderStopSeconds(null);
        assertDoesNotThrow(p::validateDeployment);assertTrue(p.hasMerchantConfiguration());assertTrue(p.paidConfigured());assertNull(p.unavailableReason());
        assertNull(p.getVerifiedProviderStopSeconds()); // Missing evidence is not a claimed stop SLA.
    }
    @Test void missingStopObservationCannotBypassIndependentRuntimeMoneyOrStopLeadBounds() {
        for(int invalid:new int[]{0,-1,3601}) {
            ExternalBillingProperties p=configured();p.setVerifiedProviderStopSeconds(null);p.setHoldStopLeadSeconds(invalid);
            assertPolicyUnavailable(p,"hold-stop-lead-seconds");
        }
        ExternalBillingProperties noRuntime=configured();noRuntime.setVerifiedProviderStopSeconds(null);noRuntime.setMaximumRunSeconds(0);
        assertPolicyUnavailable(noRuntime,"maximum-run-seconds");
        ExternalBillingProperties noMoney=configured();noMoney.setVerifiedProviderStopSeconds(null);noMoney.setMaximumHoldQuota(null);
        assertPolicyUnavailable(noMoney,"maximum-hold-quota");
        ExternalBillingProperties noSettlement=configured();noSettlement.setVerifiedProviderStopSeconds(null);noSettlement.setSettlementIntervalSeconds(0);
        assertPolicyUnavailable(noSettlement,"settlement-interval-seconds");
    }
    @Test void providedStopObservationMustBePositiveAndFitTheIndependentStopLead() {
        for(Integer invalid:new Integer[]{0,-1}) {
            ExternalBillingProperties p=configured();p.setVerifiedProviderStopSeconds(invalid);
            assertPolicyUnavailable(p,"verified-provider-stop-seconds");
        }
        ExternalBillingProperties longerThanLead=configured();longerThanLead.setVerifiedProviderStopSeconds(61);
        assertPolicyUnavailable(longerThanLead,"hold-stop-lead-seconds");
    }
    @Test void merchantConnectivityDoesNotAuthorizeNewOrdersWithMissingSafetyPolicy() {
        ExternalBillingProperties p=configured();p.setMaximumRunSeconds(0);p.setMaximumHoldQuota(null);
        assertDoesNotThrow(p::validateDeployment);assertTrue(p.hasMerchantConfiguration());assertFalse(p.paidConfigured());
        assertTrue(p.unavailableReason().contains("maximum-run-seconds"));
        // The documented original-operation adapter remains usable for safe recovery.
        assertTrue(new NewApiBillingClient(p,new ObjectMapper()).supportsAtomicCapture());assertNull(p.merchantConfigurationReason());
    }
    @Test void invalidMerchantConfigurationNeverContainsCredentialValuesAndDoesNotBreakStartup() {
        ExternalBillingProperties p=configured();p.setMerchantId(null);
        assertDoesNotThrow(p::validateDeployment);assertFalse(p.hasMerchantConfiguration());
        assertTrue(p.unavailableReason().contains("merchant-id"));assertFalse(p.unavailableReason().contains(p.getApiKey()));
        p=configured();p.setApiKey(null);assertTrue(p.unavailableReason().contains("api-key"));
        p=configured();p.setApiKey("bad\r\nInjected: value");assertFalse(p.hasMerchantConfiguration());
    }
    @Test void trustedMerchantUrlMustBeHttpsWithoutEmbeddedIdentityQueryOrFragment() {
        for(String url:new String[]{"http://billing.example.test","http://127.0.0.1:9000","https://user@billing.example.test",
            "https://billing.example.test?query=1","https://billing.example.test#fragment","https://billing.example.test:0",
            "https://billing.example.test:-1","https://billing.example.test:-2","https://billing.example.test:70000","not-a-url"}) {
            ExternalBillingProperties p=configured();p.setBaseUrl(url);assertDoesNotThrow(p::validateDeployment);
            assertFalse(p.hasIdentityConfiguration(),url);assertFalse(p.hasMerchantConfiguration(),url);assertFalse(p.paidConfigured(),url);
            assertTrue(p.unavailableReason().contains("base-url"));
        }
    }
    @Test void selfDestinationConfigurationIsIndependentOfMerchantCredentialsAndNewOrderBudget() {
        ExternalBillingProperties p=new ExternalBillingProperties();p.setBaseUrl("https://billing.example.test/billing");
        assertTrue(p.hasIdentityConfiguration());assertNull(p.identityConfigurationReason());
        assertFalse(p.hasMerchantConfiguration());assertFalse(p.paidConfigured());assertTrue(p.merchantConfigurationReason().contains("merchant-id"));
        p.setMerchantId("fixture");assertTrue(p.hasIdentityConfiguration());assertTrue(p.merchantConfigurationReason().contains("api-key"));
        p.setMerchantId("invalid/merchant");p.setApiKey("bad\r\nInjected: value");
        assertTrue(p.hasIdentityConfiguration());assertFalse(p.hasMerchantConfiguration());
    }
    @Test void merchantIdentityUsesTheSameBoundedCanonicalSyntaxAsFundingRequests() {
        String sixtyFour=new String(new char[64]).replace('\0','a');
        for(String merchant:new String[]{"vls","fixture-1","x_1",sixtyFour}) {
            ExternalBillingProperties p=configured();p.setMerchantId(merchant);assertTrue(p.hasMerchantConfiguration());assertTrue(p.paidConfigured());
        }
        for(String merchant:new String[]{"fixture id"," fixture","fixture ","fixture/id","fixture:id","商户",sixtyFour+"a"}) {
            ExternalBillingProperties p=configured();p.setMerchantId(merchant);assertFalse(p.hasMerchantConfiguration());assertFalse(p.paidConfigured());
            assertTrue(p.merchantConfigurationReason().contains("merchant-id"));assertDoesNotThrow(p::validateDeployment);
        }
    }
    @Test void eachRequiredMoneyRuntimeAndStopBoundRejectsOnlyPaidAdmission() {
        for(int invalid:new int[]{0,-1,86401}) {
            ExternalBillingProperties p=configured();p.setMaximumRunSeconds(invalid);assertPolicyUnavailable(p,"maximum-run-seconds");
        }
        for(String invalid:new String[]{null,"0","-1","1.5","9223372036854775808"}) {
            ExternalBillingProperties p=configured();p.setMaximumHoldQuota(invalid);assertPolicyUnavailable(p,"maximum-hold-quota");
        }
        for(Integer invalid:new Integer[]{0,-1}) {
            ExternalBillingProperties p=configured();p.setVerifiedProviderStopSeconds(invalid);assertPolicyUnavailable(p,"verified-provider-stop-seconds");
        }
        for(int invalid:new int[]{0,29,3601}) {
            ExternalBillingProperties p=configured();p.setHoldStopLeadSeconds(invalid);assertPolicyUnavailable(p,"hold-stop-lead-seconds");
        }
        for(int invalid:new int[]{0,-1,3601}) {
            ExternalBillingProperties p=configured();p.setSettlementIntervalSeconds(invalid);assertPolicyUnavailable(p,"settlement-interval-seconds");
        }
    }
    private void assertPolicyUnavailable(ExternalBillingProperties p,String field) {
        assertDoesNotThrow(p::validateDeployment);assertTrue(p.hasMerchantConfiguration());assertFalse(p.paidConfigured());
        assertTrue(p.unavailableReason().contains(field));assertFalse(p.unavailableReason().contains("尚未启用"));
    }
    @Test void removedActivationFieldsHaveNoCompatibilitySettersOrGetters() {
        for(java.lang.reflect.Method method:ExternalBillingProperties.class.getDeclaredMethods()) {
            assertFalse(java.util.Arrays.asList("getMode","setMode","isEnabled","setEnabled","isAllowWrites","setAllowWrites",
                "isPaidAdmissionEnabled","setPaidAdmissionEnabled","isSafetyStopEnabled","setSafetyStopEnabled").contains(method.getName()),method.getName());
        }
    }
    @Configuration(proxyBeanMethods=false)
    @EnableConfigurationProperties
    @Import({ExternalBillingProperties.class,NewApiBillingClient.class})
    static class Config {}
    private final ApplicationContextRunner context=new ApplicationContextRunner().withUserConfiguration(Config.class).withBean(ObjectMapper.class,ObjectMapper::new);
    private final String[] merchantProperties={"vlstream.compute.external-billing.base-url=https://billing.example.test",
        "vlstream.compute.external-billing.merchant-id=fixture","vlstream.compute.external-billing.api-key=unit-test-placeholder-not-real"};
    @Test void missingConfigurationKeepsApplicationAndClientAvailableWithoutTransportPermission() {
        context.run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);
            assertFalse(c.getBean(ExternalBillingProperties.class).hasMerchantConfiguration());
            assertThrows(IllegalStateException.class,()->c.getBean(NewApiBillingClient.class).config());});
    }
    @Test void invalidConfigurationDoesNotPreventUnrelatedApplicationStartup() {
        context.withPropertyValues(merchantProperties).withPropertyValues("vlstream.compute.external-billing.base-url=http://billing.example.test")
            .run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);assertFalse(c.getBean(ExternalBillingProperties.class).paidConfigured());});
    }
    @Test void configuredMerchantAlwaysHasTheClientWhileMissingNewOrderPolicyRemainsUnavailable() {
        context.withPropertyValues(merchantProperties).run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);
            ExternalBillingProperties p=c.getBean(ExternalBillingProperties.class);assertTrue(p.hasMerchantConfiguration());assertFalse(p.paidConfigured());});
    }
    @Test void completeConfigurationNeedsNoModeOrBooleanActivation() {
        context.withPropertyValues(merchantProperties).withPropertyValues("vlstream.compute.external-billing.maximum-run-seconds=60",
            "vlstream.compute.external-billing.maximum-hold-quota=100","vlstream.compute.external-billing.verified-provider-stop-seconds=30")
            .run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);
                assertTrue(c.getBean(ExternalBillingProperties.class).paidConfigured());assertTrue(c.getBean(NewApiBillingClient.class).supportsAtomicCapture());});
    }
}
