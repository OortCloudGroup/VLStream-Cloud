package com.ruoyi.vlstream.test.vlstream.compute;

import com.aliyuncs.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@Tag("dev")
class ComputeProviderAdapterTest {
    private final ComputeCredentialCipher cipher = new ComputeCredentialCipher("0123456789abcdef");
    private final ObjectMapper json = new ObjectMapper();
    private ComputeNode node(String provider) {
        ComputeNode n = new ComputeNode(); n.setId(1L); n.setProvider(provider); n.setControlMode("API");
        n.setProviderProduct("autodl".equals(provider) ? "AUTODL_PRO" : "ECS"); n.setProviderInstanceId("autodl".equals(provider) ? "pro-test-only" : "i-test-only");
        n.setProviderRegion("cn-test"); n.setRequestedStoppedMode("StopCharging");
        n.setProviderCredentialCipher(cipher.encrypt("autodl".equals(provider) ? "{\"token\":\"test-only-token\"}" : "{\"accessKeyId\":\"test-only-key\",\"accessKeySecret\":\"test-only-secret\"}")); return n;
    }
    @Test void controlsAreDisabledByDefaultAndOrdinaryAutodlCanNeverUsePro() {
        ComputeNode n = node("autodl"); RestTemplate http = mock(RestTemplate.class);
        AutoDlProPowerProvider disabled = new AutoDlProPowerProvider(cipher,false,http);
        assertFalse(disabled.supports(n)); assertThrows(ComputeProviderException.class, () -> disabled.submit(n,"start","op"));
        n.setProviderProduct(null); assertFalse(new AutoDlProPowerProvider(cipher,true,http).supports(n)); verifyNoInteractions(http);
        AliyunEcsPowerProvider.ClientFactory clients = mock(AliyunEcsPowerProvider.ClientFactory.class);
        assertFalse(new AliyunEcsPowerProvider(cipher,false,clients).supports(node("aliyun"))); verifyNoInteractions(clients);
    }
    @Test void proUsesRawAuthorizationAndBoundedListWithoutFetchingSecretSnapshotFields() throws Exception {
        RestTemplate http = AutoDlProPowerProvider.transport(); MockRestServiceServer mock = MockRestServiceServer.bindTo(http).build();
        ComputeNode n = node("autodl"); AutoDlProPowerProvider provider = new AutoDlProPowerProvider(cipher,true,http);
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on")).andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","test-only-token"))
            .andExpect(content().json("{\"instance_uuid\":\"pro-test-only\",\"payload\":\"gpu\"}"))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":null,\"request_id\":\"test-id\"}",MediaType.APPLICATION_JSON));
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"status")).andExpect(method(HttpMethod.GET)).andExpect(content().json("{\"instance_uuid\":\"pro-test-only\"}"))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":\"running\"}",MediaType.APPLICATION_JSON));
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"list")).andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"page_index\":1,\"page_size\":20}"))
            .andRespond(withSuccess("{\"code\":\"Success\",\"data\":{\"page_index\":1,\"max_page\":1,\"result_total\":1,\"list\":[{\"uuid\":\"pro-test-only\",\"status\":\"running\",\"req_gpu_amount\":2,\"charge_type\":\"fixed\",\"root_password\":\"discard-test-password\",\"jupyter_token\":\"discard-test-token\",\"untrusted\":\"anything\"}]}}",MediaType.APPLICATION_JSON));
        provider.submit(n,"start","stable-op"); assertNull(n.getPowerState()); // acceptance didn't set RUNNING
        ComputePowerProvider.Snapshot state = provider.status(n); assertEquals("RUNNING",state.power); assertEquals("UNKNOWN",state.capacity); assertEquals(2,state.gpuAmount);
        String exported = json.writeValueAsString(state); assertFalse(exported.contains("discard-test")); assertFalse(exported.contains("untrusted")); mock.verify();
    }
    @Test void actualApacheTransportSendsGetWithJsonBodyWithoutTurningItIntoPost() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        AtomicReference<String> method = new AtomicReference<>(), body = new AtomicReference<>();
        server.createContext("/state", exchange -> {
            method.set(exchange.getRequestMethod()); body.set(new String(org.springframework.util.StreamUtils.copyToByteArray(exchange.getRequestBody()),StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200,response.length); exchange.getResponseBody().write(response); exchange.close();
        }); server.start();
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
            AutoDlProPowerProvider.transport().exchange("http://127.0.0.1:"+server.getAddress().getPort()+"/state",HttpMethod.GET,new org.springframework.http.HttpEntity<>("{\"instance_uuid\":\"test\"}",headers),String.class);
            assertEquals("GET",method.get()); assertEquals("{\"instance_uuid\":\"test\"}",body.get());
        } finally { server.stop(0); }
    }
    @Test void undocumentedProErrorsDoNotBecomeGpuShortageOrExposeVendorMessages() {
        RestTemplate http = AutoDlProPowerProvider.transport(); MockRestServiceServer mock = MockRestServiceServer.bindTo(http).build();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on")).andRespond(withSuccess("{\"code\":\"undocumented\",\"msg\":\"test-only-token GPU stock?\"}",MediaType.APPLICATION_JSON));
        ComputeProviderException e = assertThrows(ComputeProviderException.class,()->new AutoDlProPowerProvider(cipher,true,http).submit(node("autodl"),"start","op"));
        assertEquals(ComputeProviderException.Kind.UNKNOWN,e.getKind()); assertFalse(e.getMessage().contains("test-only-token")); assertNull(e.getCause()); mock.verify();
    }
    @Test void ecsSdkRequestsUseExactPurchasedInstanceRegionSafeStopModeAndAsyncAcceptance() throws Exception {
        IAcsClient client = mock(IAcsClient.class); when(client.getCommonResponse(any())).thenReturn(response("{\"RequestId\":\"test-request\"}"));
        AliyunEcsPowerProvider provider = new AliyunEcsPowerProvider(cipher,true,(region,key,secret)->client); ComputeNode n = node("aliyun");
        provider.submit(n,"start","op"); provider.submit(n,"stop","op2");
        ArgumentCaptor<CommonRequest> requests = ArgumentCaptor.forClass(CommonRequest.class); verify(client,times(2)).getCommonResponse(requests.capture());
        CommonRequest start=requests.getAllValues().get(0), stop=requests.getAllValues().get(1);
        assertEquals("ecs.aliyuncs.com",start.getSysDomain()); assertEquals("2014-05-26",start.getSysVersion()); assertEquals(ProtocolType.HTTPS,start.getSysProtocol());
        assertEquals("StartInstance",start.getSysAction()); assertEquals("i-test-only",start.getSysQueryParameters().get("InstanceId")); assertEquals("cn-test",start.getSysQueryParameters().get("RegionId"));
        assertEquals("false",start.getSysQueryParameters().get("InitLocalDisk")); assertFalse(start.getSysQueryParameters().containsKey("ClientToken"));
        assertEquals("StopInstance",stop.getSysAction()); assertEquals("false",stop.getSysQueryParameters().get("ForceStop")); assertEquals("StopCharging",stop.getSysQueryParameters().get("StoppedMode"));
        assertNull(n.getPowerState()); verify(client,times(2)).shutdown();
    }
    @Test void ecsSpecificationIsNotCapacityOrFreeCardsAndUnknownEnumsStayUnknown() throws Exception {
        IAcsClient client=mock(IAcsClient.class); when(client.getCommonResponse(any())).thenReturn(response("{\"Instances\":{\"Instance\":[{\"InstanceId\":\"i-test-only\",\"RegionId\":\"cn-test\",\"Status\":\"Stopped\",\"GPUAmount\":4,\"StoppedMode\":\"KeepCharging\",\"OperationLocks\":{\"LockReason\":[{\"LockMsg\":\"discard-test-secret\"}]},\"PrivateIpAddress\":\"discard-test-ip\"}]}}"));
        AliyunEcsPowerProvider provider=new AliyunEcsPowerProvider(cipher,true,(r,k,s)->client); ComputePowerProvider.Snapshot state=provider.status(node("aliyun"));
        assertEquals("STOPPED",state.power); assertEquals("UNKNOWN",state.capacity); assertEquals(4,state.gpuAmount); assertEquals("KeepCharging",state.stoppedMode); assertTrue(state.locked);
        assertTrue(state.message.contains("计算费继续"));
        assertFalse(json.writeValueAsString(state).contains("discard-test"));
        ArgumentCaptor<CommonRequest> request=ArgumentCaptor.forClass(CommonRequest.class); verify(client).getCommonResponse(request.capture()); assertEquals("DescribeInstances",request.getValue().getSysAction()); assertEquals("[\"i-test-only\"]",request.getValue().getSysQueryParameters().get("InstanceIds"));
        when(client.getCommonResponse(any())).thenReturn(response("{\"Instances\":{\"Instance\":[{\"InstanceId\":\"i-test-only\",\"RegionId\":\"cn-test\",\"Status\":\"future-status\"}]}}")); assertEquals("UNKNOWN",provider.status(node("aliyun")).power);
    }
    @Test void ecsActualStopModeIsCheckedInsteadOfInferringSavingsFromTheRequestedMode() throws Exception {
        IAcsClient client = mock(IAcsClient.class);
        AliyunEcsPowerProvider provider = new AliyunEcsPowerProvider(cipher,true,(r,k,s)->client);
        ComputeNode node = node("aliyun"); node.setRequestedStoppedMode("StopCharging");
        for (String mode : new String[]{"KeepCharging", "future-mode", "StopCharging"}) {
            when(client.getCommonResponse(any())).thenReturn(response("{\"Instances\":{\"Instance\":[{\"InstanceId\":\"i-test-only\",\"RegionId\":\"cn-test\",\"Status\":\"Stopped\",\"StoppedMode\":\""+mode+"\",\"InstanceChargeType\":\"PostPaid\"}]}}"));
            ComputePowerProvider.Snapshot snapshot = provider.status(node);
            assertEquals("STOPPED",snapshot.power);
            assertEquals("StopCharging".equals(mode),ComputeBillingPolicy.computeStopConfirmed(node,snapshot));
            assertTrue(snapshot.message.contains("StopCharging".equals(mode) ? "磁盘" : "KeepCharging".equals(mode) ? "计算费继续" : "模式未知"));
        }
    }
    @Test void ecsErrorsDistinguishNoStockBalanceAuthorizationAndUnknownWithoutRetainingPayload() throws Exception {
        for (String code:new String[]{"OperationDenied.NoStock","InsufficientBalance","InvalidAccessKeyId.NotFound","LackResource"}) {
            IAcsClient client=mock(IAcsClient.class); when(client.getCommonResponse(any())).thenThrow(new ClientException(code,"discard-test-key"));
            ComputeProviderException e=assertThrows(ComputeProviderException.class,()->new AliyunEcsPowerProvider(cipher,true,(r,k,s)->client).submit(node("aliyun"),"start","op"));
            assertEquals(ComputeProviderException.aliyunKind(code),e.getKind()); assertFalse(e.getMessage().contains("discard-test")); assertNull(e.getCause());
        }
    }
    @Test void credentialDtoIsWriteOnlyAndEncryptedNodeNeverExportsIt() throws Exception {
        ComputeRequests.ProviderSettings settings=json.readValue("{\"token\":\"test-token\",\"accessKeyId\":\"test-key\",\"accessKeySecret\":\"test-secret\"}",ComputeRequests.ProviderSettings.class);
        assertEquals("test-token",settings.getToken()); assertFalse(json.writeValueAsString(settings).contains("test-token")); assertFalse(settings.toString().contains("test-secret"));
        String node=json.writeValueAsString(node("aliyun")); assertFalse(node.contains("providerCredentialCipher")); assertFalse(node.contains("test-only-secret"));
    }
    @Test void proPriceScalingAndCardScopeRemainUnknownInsteadOfGuessingTheExampleDivisor() {
        RestTemplate http=AutoDlProPowerProvider.transport(); MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"snapshot")).andRespond(withSuccess("{\"code\":\"Success\",\"data\":{\"payg_price\":1970,\"origin_pay_price\":2500,\"root_password\":\"test-secret\"}}",MediaType.APPLICATION_JSON));
        ComputeCostProvider.Observation cost=new AutoDlProPowerProvider(cipher,true,http).cost(node("autodl"));
        assertEquals(new java.math.BigDecimal("1970"),cost.getAmount()); assertEquals("UNKNOWN",cost.getAmountUnit()); assertEquals("UNKNOWN",cost.getResourceScope()); assertFalse(cost.isBillableCost()); mock.verify();
    }
    @Test void ecsHourlyQuoteKeepsComponentsSeparateAndCannotBeActivatedAsActualPurchasedCost() throws Exception {
        IAcsClient client=mock(IAcsClient.class);
        when(client.getCommonResponse(any())).thenReturn(response("{\"Instances\":{\"Instance\":[{\"InstanceId\":\"i-test-only\",\"RegionId\":\"cn-test\",\"InstanceType\":\"ecs.test\"}]}}"),response("{\"PriceInfo\":{\"Price\":{\"Currency\":\"CNY\",\"TradePrice\":3,\"OriginalPrice\":4,\"DetailInfos\":{\"DetailInfo\":[{\"Resource\":\"instanceType\",\"TradePrice\":2},{\"Resource\":\"systemDisk\",\"TradePrice\":1}]}}}}"));
        ComputeCostProvider.Observation cost=new AliyunEcsPowerProvider(cipher,true,(r,k,s)->client).cost(node("aliyun"));
        assertFalse(cost.isBillableCost()); assertEquals("REFERENCE_QUOTE",cost.getPriceKind()); assertEquals(new java.math.BigDecimal("2"),cost.getComponents().get("instanceType"));
        ArgumentCaptor<CommonRequest> requests=ArgumentCaptor.forClass(CommonRequest.class); verify(client,times(2)).getCommonResponse(requests.capture()); CommonRequest quote=requests.getAllValues().get(1);
        assertEquals("DescribePrice",quote.getSysAction()); assertEquals("Hour",quote.getSysQueryParameters().get("PriceUnit")); assertEquals("1",quote.getSysQueryParameters().get("Period")); assertEquals("instance",quote.getSysQueryParameters().get("ResourceType"));
    }
    private CommonResponse response(String value) { CommonResponse r=new CommonResponse(); r.setHttpStatus(200); r.setData(value); return r; }
}
