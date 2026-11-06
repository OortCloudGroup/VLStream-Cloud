package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class AutoDlBillingModeTest {
    @Test void existingOfficialIdentityWorksWithoutBillingModeOrGlobalCloudControl() {
        ConfiguredComputeRegistry registry=registry("SYNTHETIC_OFFICIAL_API");
        RestTemplate http=mock(RestTemplate.class);AutoDlProPowerProvider provider=provider(registry,false,http);
        assertTrue(provider.supports(registry.resolve(-6000L)));
        verifyNoInteractions(http);
    }

    @Test void ordinaryNodesRetainTheirExistingGlobalCloudControlGate() {
        ComputeNode node=new ComputeNode();node.setProvider("autodl");node.setProviderProduct("AUTODL_PRO");
        node.setControlMode("API");node.setProviderInstanceId("pro-0123456789ab");node.setProviderCredentialCipher("fixture-cipher");
        RestTemplate http=mock(RestTemplate.class);
        assertFalse(provider(null,false,http).supports(node));assertTrue(provider(null,true,http).supports(node));
        node.setProviderProduct("AUTODL_STANDARD");assertFalse(provider(null,true,http).supports(node));
        verifyNoInteractions(http);
    }

    @Test void globalCloudControlCannotAdmitAnotherConfiguredIdentity() {
        ConfiguredComputeRegistry registry=registry("SYNTHETIC_OFFICIAL_API");RestTemplate http=mock(RestTemplate.class);
        AutoDlProPowerProvider provider=provider(registry,true,http);ComputeNode changedInstance=registry.resolve(-6000L);
        changedInstance.setProviderInstanceId("pro-abcdef123456");assertFalse(provider.supports(changedInstance));
        assertThrows(ComputeProviderException.class,()->provider.submit(changedInstance,"start","not-an-order"));
        assertThrows(RuntimeException.class,()->provider.reconciliationStatus(changedInstance));
        ComputeNode changedId=registry.resolve(-6000L);changedId.setId(-6001L);assertFalse(provider.supports(changedId));
        ComputeNode changedCode=registry.resolve(-6000L);changedCode.setConfigurationCode("another-official");assertFalse(provider.supports(changedCode));
        verifyNoInteractions(http);
    }

    @Test void missingControlConfigurationDoesNotCallTheSupplier() {
        ConfiguredComputeRegistry registry=registry(null);RestTemplate http=mock(RestTemplate.class);
        AutoDlProPowerProvider provider=provider(registry,true,http);ComputeNode node=registry.resolve(-6000L);
        assertFalse(provider.supports(node));assertThrows(ComputeProviderException.class,()->provider.status(node));
        assertFalse(provider(null,true,http).supports(node));assertFalse(provider.supports(null));
        verifyNoInteractions(http);
    }

    @Test void originalStatusAndStopRemainAvailableWithoutNewAdmissionConfiguration() {
        // No merchant, fixed-price, quota or new-run budget configuration is supplied.
        // Recovery uses the existing supplier target and original operation identity.
        ConfiguredComputeRegistry registry=registry("SYNTHETIC_OFFICIAL_API");RestTemplate http=mock(RestTemplate.class);
        AutoDlProPowerProvider provider=provider(registry,false,http);ComputeNode node=registry.resolve(-6000L);
        when(http.exchange(anyString(),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class)))
            .thenReturn(ResponseEntity.ok("{\"code\":\"Success\",\"data\":\"shutdown\"}"));
        when(http.exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(String.class)))
            .thenReturn(ResponseEntity.ok("{\"code\":\"Success\",\"data\":null}"));
        assertEquals("STOPPED",provider.reconciliationStatus(node).power);provider.submit(node,"stop","original-stop-operation");
        verify(http).exchange(eq(AutoDlProPowerProvider.BASE+"status?instance_uuid=pro-0123456789ab"),eq(HttpMethod.GET),any(HttpEntity.class),eq(String.class));
        verify(http).exchange(eq(AutoDlProPowerProvider.BASE+"power_off"),eq(HttpMethod.POST),any(HttpEntity.class),eq(String.class));
        verifyNoMoreInteractions(http);
    }

    @Test void ordinaryPowerEntryStillRejectsOfficialStartsBeforeAnyFundingOrSupplierCall() {
        ConfiguredComputeRegistry registry=registry("SYNTHETIC_OFFICIAL_API");RestTemplate http=mock(RestTemplate.class);
        AutoDlProPowerProvider provider=provider(registry,false,http);assertTrue(provider.supports(registry.resolve(-6000L)));
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        ComputeNodeService nodes=new ComputeNodeService(jdbc,mock(ComputeCredentialCipher.class),mock(ComputeSsh.class),new ObjectMapper());
        ReflectionTestUtils.setField(nodes,"configured",registry);
        ComputeMarketService market=new ComputeMarketService(jdbc,nodes);ReflectionTestUtils.setField(market,"powerProviders",Collections.singletonList(provider));
        ServiceException denied=assertThrows(ServiceException.class,()->market.power(-6000L,"start"));
        assertEquals(Integer.valueOf(403),denied.getCode());verifyNoInteractions(jdbc,http);
    }

    @Test void marketOffersPaidAdmissionFromCompleteConfigurationWithoutStaticSshOrActivationFlags() {
        MarketFixture fixture=new MarketFixture("SYNTHETIC_OFFICIAL_API",true);
        ConfiguredComputeProperties.Resource definition=fixture.registry.definition(fixture.registry.resolve(-6000L));
        assertNull(definition.getHost());assertNull(definition.getHostKey());assertNull(definition.getPrivateKeyFile());
        ComputeMarketService.Resource offer=fixture.offer();
        assertTrue(offer.isPaidAdmissionEnabled());assertNull(offer.getPaidUnavailableReason());
        assertTrue(offer.isApiControl());assertEquals("13.00",offer.getUserHourlyPrice());
        fixture.noSupplierOperation();
    }

    @Test void marketReportsMissingMerchantConfigurationWithoutSuppressingTheOfferOrCallingSupplier() {
        MarketFixture fixture=new MarketFixture("SYNTHETIC_OFFICIAL_API",true);fixture.billing.setApiKey(null);
        assertDoesNotThrow(fixture.billing::validateDeployment);
        ComputeMarketService.Resource offer=fixture.offer();
        assertFalse(offer.isPaidAdmissionEnabled());assertTrue(offer.getPaidUnavailableReason().contains("缺配置"));
        assertTrue(offer.getPaidUnavailableReason().contains("api-key"));assertFalse(offer.getPaidUnavailableReason().contains("尚未启用"));
        fixture.noSupplierOperation();
    }

    @Test void marketReportsMissingFixedPriceBeforeOfferingNewPaidAdmission() {
        MarketFixture fixture=new MarketFixture("SYNTHETIC_OFFICIAL_API",false);
        ComputeMarketService.Resource offer=fixture.offer();
        assertFalse(offer.isPaidAdmissionEnabled());assertEquals("官方算力用户固定价格未配置",offer.getPaidUnavailableReason());
        fixture.noSupplierOperation();
    }

    @Test void marketReportsMissingSupplierIdentityBeforeOfferingNewPaidAdmission() {
        MarketFixture fixture=new MarketFixture(null,true);
        ComputeMarketService.Resource offer=fixture.offer();
        assertFalse(offer.isPaidAdmissionEnabled());assertEquals("官方算力供应商调用身份未配置",offer.getPaidUnavailableReason());
        assertFalse(offer.isApiControl());fixture.noSupplierOperation();
    }

    @Test void marketReportsMissingPowerSupportBeforeOfferingNewPaidAdmission() {
        MarketFixture fixture=new MarketFixture("SYNTHETIC_OFFICIAL_API",true);
        ReflectionTestUtils.setField(fixture.market,"powerProviders",Collections.emptyList());
        ComputeMarketService.Resource offer=fixture.offer();
        assertFalse(offer.isPaidAdmissionEnabled());assertEquals("实例电源控制配置不完整",offer.getPaidUnavailableReason());
        assertFalse(offer.isApiControl());fixture.noSupplierOperation();
    }

    @Test void marketKeepsInvalidStopPolicyVisibleWithoutStartupFailureOrSupplierOperation() {
        MarketFixture fixture=new MarketFixture("SYNTHETIC_OFFICIAL_API",true);fixture.policy.setStopBudgetSeconds(200);
        assertDoesNotThrow(fixture.billing::validateDeployment);
        ComputeMarketService.Resource offer=assertDoesNotThrow(fixture::offer);
        assertFalse(offer.isPaidAdmissionEnabled());assertEquals("算力预留和余额提醒/停机安全阈值配置无效",offer.getPaidUnavailableReason());
        fixture.noSupplierOperation();
    }

    @Test void marketRequiresHoldLeadToCoverTheValidatedCrossPolicyStopBudget() {
        MarketFixture fixture=new MarketFixture("SYNTHETIC_OFFICIAL_API",true);fixture.policy.setStopBudgetSeconds(360);
        assertTrue(fixture.billing.paidConfigured());assertDoesNotThrow(fixture.billing::validateDeployment);
        ComputeMarketService.Resource offer=fixture.offer();
        assertFalse(offer.isPaidAdmissionEnabled());assertEquals("余额到期停机提前量未覆盖可靠停机预算",offer.getPaidUnavailableReason());
        fixture.noSupplierOperation();
    }

    private static final class MarketFixture {
        final ConfiguredComputeRegistry registry;
        final ExternalBillingProperties billing=new ExternalBillingProperties();
        final ComputeBillingPolicy policy=new ComputeBillingPolicy();
        final RestTemplate http=mock(RestTemplate.class);
        final ComputeSsh ssh=mock(ComputeSsh.class);
        final AutoDlProPowerProvider power;
        final ComputeMarketService market;
        MarketFixture(String token,boolean fixedPrice) {
            registry=registry(token,fixedPrice);power=spy(provider(registry,false,http));
            JdbcTemplate jdbc=mock(JdbcTemplate.class,call->"query".equals(call.getMethod().getName())
                ?new ArrayList<ComputeNode>():org.mockito.Answers.RETURNS_DEFAULTS.answer(call));
            ComputeNodeService nodes=new ComputeNodeService(jdbc,mock(ComputeCredentialCipher.class),ssh,new ObjectMapper());
            ReflectionTestUtils.setField(nodes,"configured",registry);
            market=new ComputeMarketService(jdbc,nodes);ReflectionTestUtils.setField(market,"configured",registry);
            ReflectionTestUtils.setField(market,"billingProperties",billing);ReflectionTestUtils.setField(market,"billingPolicy",policy);
            ReflectionTestUtils.setField(market,"powerProviders",Collections.singletonList(power));
            billing.setBaseUrl("https://billing.example.test");billing.setMerchantId("synthetic-merchant");billing.setApiKey("SYNTHETIC_MERCHANT_API");
            billing.setMaximumRunSeconds(600);billing.setMaximumHoldQuota("500000");billing.setVerifiedProviderStopSeconds(240);
            billing.setHoldStopLeadSeconds(300);billing.setSettlementIntervalSeconds(60);
        }
        ComputeMarketService.Resource offer() {
            java.util.List<ComputeMarketService.Resource> offers=market.list(false);
            assertEquals(1,offers.size());return offers.get(0);
        }
        void noSupplierOperation() {
            verify(power,never()).status(any());verify(power,never()).submit(any(),anyString(),anyString());
            verifyNoInteractions(http,ssh);
        }
    }

    private static AutoDlProPowerProvider provider(ConfiguredComputeRegistry registry,boolean globalControl,RestTemplate http) {
        AutoDlProPowerProvider provider=new AutoDlProPowerProvider(mock(ComputeCredentialCipher.class),globalControl,http);
        ReflectionTestUtils.setField(provider,"configured",registry);return provider;
    }

    private static ConfiguredComputeRegistry registry(String token) {
        return registry(token,false);
    }

    private static ConfiguredComputeRegistry registry(String token,boolean fixedPrice) {
        ConfiguredComputeProperties.Resource resource=new ConfiguredComputeProperties.Resource();
        resource.setNodeId(-6000L);resource.setCode("official-autodl");resource.setProvider("autodl");
        resource.setName("Synthetic official instance");resource.setGpuModel("SYNTHETIC_GPU");
        resource.setInstanceId("pro-0123456789ab");resource.setAccountScope("synthetic-official");resource.setTenantId("synthetic-tenant");
        resource.setPythonPath("/fixture/python");resource.setWorkDir("/fixture/work");resource.setProviderApiToken(token);
        if(fixedPrice) {
            ConfiguredComputeProperties.FixedPrice price=new ConfiguredComputeProperties.FixedPrice();
            price.setPerInstanceHour(new BigDecimal("13.00"));resource.setFixedPrice(price);
        }
        ConfiguredComputeProperties properties=new ConfiguredComputeProperties();properties.setResources(Collections.singletonList(resource));
        ConfiguredComputeRegistry registry=new ConfiguredComputeRegistry(properties,new ComputeTrustedSelfAssetsProperties(),new ObjectMapper(),mock(JdbcTemplate.class));
        registry.initialize();return registry;
    }
}
