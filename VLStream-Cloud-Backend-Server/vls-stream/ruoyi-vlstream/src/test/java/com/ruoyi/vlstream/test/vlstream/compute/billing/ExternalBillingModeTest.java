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

@Tag("dev")
class ExternalBillingModeTest {
    static ExternalBillingProperties configured(ExternalBillingProperties.Mode mode) {
        ExternalBillingProperties p=new ExternalBillingProperties();p.setMode(mode);
        p.setBaseUrl("https://billing.example.test");p.setMerchantId("fixture");p.setApiKey("unit-test-placeholder-not-real");
        p.setMaximumRunSeconds(60);p.setMaximumHoldQuota("100");p.setVerifiedProviderStopSeconds(30);return p;
    }
    @Test void absentConfigurationNeverEnablesMoney() {
        ExternalBillingProperties p=new ExternalBillingProperties();p.validateDeployment();
        assertFalse(p.isEnabled());assertFalse(p.paidConfigured());assertNotNull(p.unavailableReason());
        p.setApiKey("unit-test-placeholder-not-real");assertFalse(p.isEnabled());
    }
    @Test void readonlyModeDoesNotEnableAnyWriteOrSafetyControl() {
        ExternalBillingProperties p=configured(ExternalBillingProperties.Mode.READ_ONLY);p.validateDeployment();
        assertTrue(p.isEnabled());assertFalse(p.isAllowWrites());assertFalse(p.isPaidAdmissionEnabled());assertFalse(p.isSafetyStopEnabled());assertNotNull(p.unavailableReason());
    }
    @Test void paidModeEnablesWholeLifecycleTogether() {
        ExternalBillingProperties p=configured(ExternalBillingProperties.Mode.PAID);p.validateDeployment();assertTrue(p.paidConfigured());assertNull(p.unavailableReason());assertFalse(p.getTrial().isEnabled());
    }
    @Test void legacyReadonlyAndPaidSemanticsRemainSeparate() {
        ExternalBillingProperties p=new ExternalBillingProperties();p.setEnabled(true);p.validateDeployment();assertTrue(p.isEnabled());assertFalse(p.paidConfigured());
        p.setAllowWrites(true);p.setPaidAdmissionEnabled(true);assertFalse(p.paidConfigured());p.setSafetyStopEnabled(true);assertTrue(p.paidConfigured());
    }
    @Test void explicitContradictoryOldFlagFailsRatherThanOpeningMoney() {
        ExternalBillingProperties p=configured(ExternalBillingProperties.Mode.PAID);p.setAllowWrites(false);assertThrows(IllegalStateException.class,p::validateDeployment);
        p=configured(ExternalBillingProperties.Mode.OFF);p.setEnabled(true);assertThrows(IllegalStateException.class,p::validateDeployment);
    }
    @Test void enabledModeMissingCredentialsFailsWithoutPrintingSecret() {
        ExternalBillingProperties p=configured(ExternalBillingProperties.Mode.PAID);p.setMerchantId(null);
        assertFalse(assertThrows(IllegalStateException.class,p::validateDeployment).getMessage().contains(p.getApiKey()));
    }
    @Test void modeRejectsUnsafeUrlAndUnboundedPaidDeployment() {
        ExternalBillingProperties p=configured(ExternalBillingProperties.Mode.READ_ONLY);p.setBaseUrl("http://billing.example.test");assertThrows(IllegalStateException.class,p::validateDeployment);
        p=configured(ExternalBillingProperties.Mode.PAID);p.setMaximumRunSeconds(0);assertThrows(IllegalStateException.class,p::validateDeployment);
        p=configured(ExternalBillingProperties.Mode.PAID);p.setMaximumHoldQuota(null);assertThrows(IllegalStateException.class,p::validateDeployment);
        p=configured(ExternalBillingProperties.Mode.PAID);p.setHoldStopLeadSeconds(1);assertThrows(IllegalStateException.class,p::validateDeployment);
    }
    @Configuration(proxyBeanMethods=false)
    @EnableConfigurationProperties
    @Import({ExternalBillingProperties.class,NewApiBillingClient.class})
    static class Config {}
    private final ApplicationContextRunner context=new ApplicationContextRunner().withUserConfiguration(Config.class).withBean(ObjectMapper.class,ObjectMapper::new);
    @Test void contextOffDoesNotCreateFinancialClient() {
        context.withPropertyValues("vlstream.compute.external-billing.mode=OFF").run(c->{assertThat(c).hasNotFailed().doesNotHaveBean(NewApiBillingClient.class);assertFalse(c.getBean(ExternalBillingProperties.class).isEnabled());});
    }
    @Test void contextReadonlyBindsAndCreatesOnlyReadonlyClient() {
        context.withPropertyValues("vlstream.compute.external-billing.mode=READ_ONLY","vlstream.compute.external-billing.base-url=https://billing.example.test","vlstream.compute.external-billing.merchant-id=fixture","vlstream.compute.external-billing.api-key=unit-test-placeholder-not-real")
            .run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);assertFalse(c.getBean(NewApiBillingClient.class).supportsAtomicCapture());});
    }
    @Test void contextConflictingEnvironmentFailsAtStartup() {
        context.withPropertyValues("vlstream.compute.external-billing.mode=OFF","vlstream.compute.external-billing.enabled=true").run(c->{assertThat(c).hasFailed();assertThat(c.getStartupFailure()).hasRootCauseMessage("external-billing.mode 与旧开关 enabled 冲突；请移除旧开关，不能静默改变部署语义");});
    }
    @Test void contextLegacyEnvironmentStillBindsWithoutModeOrImplicitWrites() {
        context.withPropertyValues("vlstream.compute.external-billing.enabled=true","vlstream.compute.external-billing.allow-writes=false")
            .run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);ExternalBillingProperties p=c.getBean(ExternalBillingProperties.class);assertNull(p.getMode());assertTrue(p.isEnabled());assertFalse(p.paidConfigured());});
    }
    @Test void contextPaidModeRequiresNoOldBooleanFlags() {
        context.withPropertyValues("vlstream.compute.external-billing.mode=PAID","vlstream.compute.external-billing.base-url=https://billing.example.test","vlstream.compute.external-billing.merchant-id=fixture","vlstream.compute.external-billing.api-key=unit-test-placeholder-not-real",
            "vlstream.compute.external-billing.maximum-run-seconds=60","vlstream.compute.external-billing.maximum-hold-quota=100","vlstream.compute.external-billing.verified-provider-stop-seconds=30")
            .run(c->{assertThat(c).hasNotFailed().hasSingleBean(NewApiBillingClient.class);assertTrue(c.getBean(ExternalBillingProperties.class).paidConfigured());assertTrue(c.getBean(NewApiBillingClient.class).supportsAtomicCapture());});
    }
}
