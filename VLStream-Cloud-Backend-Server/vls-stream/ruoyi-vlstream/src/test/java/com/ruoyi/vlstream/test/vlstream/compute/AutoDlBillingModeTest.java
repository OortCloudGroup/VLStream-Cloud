package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class AutoDlBillingModeTest {
    @Test void administrativeStatusReadDoesNotEnablePowerAndCannotReadAnotherInstance() {
        ConfiguredComputeRegistry registry=mock(ConfiguredComputeRegistry.class);ComputeNode node=node();
        RestTemplate http=mock(RestTemplate.class);
        when(registry.providerToken(node)).thenReturn("SYNTHETIC_READ_FIXTURE");
        when(http.exchange(anyString(),eq(org.springframework.http.HttpMethod.GET),any(org.springframework.http.HttpEntity.class),eq(String.class)))
            .thenReturn(org.springframework.http.ResponseEntity.ok("{\"code\":\"Success\",\"data\":\"shutdown\"}"));
        AutoDlProPowerProvider p=new AutoDlProPowerProvider(mock(ComputeCredentialCipher.class),false,http);
        ExternalBillingProperties billing=new ExternalBillingProperties();billing.setMode(ExternalBillingProperties.Mode.READ_ONLY);
        ReflectionTestUtils.setField(p,"billing",billing);ReflectionTestUtils.setField(p,"configured",registry);
        assertFalse(p.supports(node));assertEquals("STOPPED",p.reconciliationStatus(node).power);
        assertThrows(ComputeProviderException.class,()->p.submit(node,"start","operation"));
        assertThrows(ComputeProviderException.class,()->p.submit(node,"stop","operation"));
        ComputeNode other=node();other.setId(-6001L);assertThrows(ComputeProviderException.class,()->p.reconciliationStatus(other));
        verify(http).exchange(eq(AutoDlProPowerProvider.BASE+"status?instance_uuid=pro-0123456789ab"),eq(org.springframework.http.HttpMethod.GET),any(org.springframework.http.HttpEntity.class),eq(String.class));verifyNoMoreInteractions(http);
    }
    @Test void onePaidModeEnablesOnlyConfiguredOfficialInstanceWithoutGlobalCloudControl() {
        ConfiguredComputeRegistry registry=mock(ConfiguredComputeRegistry.class);ComputeNode node=node();
        when(registry.official(node)).thenReturn(true);when(registry.providerTokenPresent(node)).thenReturn(true);when(registry.published()).thenReturn(Collections.singletonList(node));
        AutoDlProPowerProvider p=new AutoDlProPowerProvider(mock(ComputeCredentialCipher.class),false,mock(RestTemplate.class));
        ExternalBillingProperties billing=new ExternalBillingProperties();billing.setMode(ExternalBillingProperties.Mode.PAID);
        ReflectionTestUtils.setField(p,"billing",billing);ReflectionTestUtils.setField(p,"configured",registry);
        p.validateOfficialMode();assertTrue(p.supports(node));
        ComputeNode other=node();other.setId(-6001L);assertFalse(p.supports(other));
        billing.setMode(ExternalBillingProperties.Mode.READ_ONLY);assertFalse(p.supports(node));
    }
    @Test void missingControlMaterialOrExplicitOldDenialFailsInsteadOfSilentDisable() {
        ConfiguredComputeRegistry registry=mock(ConfiguredComputeRegistry.class);ComputeNode node=node();when(registry.official(node)).thenReturn(true);when(registry.published()).thenReturn(Collections.singletonList(node));
        AutoDlProPowerProvider p=new AutoDlProPowerProvider(mock(ComputeCredentialCipher.class),false,mock(RestTemplate.class));ExternalBillingProperties billing=new ExternalBillingProperties();billing.setMode(ExternalBillingProperties.Mode.PAID);
        ReflectionTestUtils.setField(p,"billing",billing);ReflectionTestUtils.setField(p,"configured",registry);
        assertThrows(IllegalStateException.class,p::validateOfficialMode);
        ReflectionTestUtils.setField(p,"officialControl",false);assertThrows(IllegalStateException.class,p::validateOfficialMode);
        assertFalse(p.supports(node));
    }
    private static ComputeNode node(){ComputeNode n=new ComputeNode();n.setId(-6000L);n.setConfigurationManaged(true);n.setConfigurationCode("official-autodl");n.setProvider("autodl");n.setProviderProduct("AUTODL_PRO");n.setControlMode("API");n.setProviderInstanceId("pro-0123456789ab");n.setPricePolicy("VLS_FIXED");n.setUserHourlyPrice("13.00");return n;}
    @Test void invalidStopBudgetFailsBeforeAnyProviderCall() {
        RestTemplate http=mock(RestTemplate.class);AutoDlProPowerProvider p=new AutoDlProPowerProvider(mock(ComputeCredentialCipher.class),false,http);
        ExternalBillingProperties b=new ExternalBillingProperties();b.setMode(ExternalBillingProperties.Mode.PAID);b.setVerifiedProviderStopSeconds(240);b.setHoldStopLeadSeconds(60);
        ReflectionTestUtils.setField(p,"billing",b);ReflectionTestUtils.setField(p,"billingPolicy",new ComputeBillingPolicy());
        assertThrows(IllegalStateException.class,p::validateOfficialMode);verifyNoInteractions(http);
        b.setHoldStopLeadSeconds(300);assertDoesNotThrow(p::validateOfficialMode);
    }
}
