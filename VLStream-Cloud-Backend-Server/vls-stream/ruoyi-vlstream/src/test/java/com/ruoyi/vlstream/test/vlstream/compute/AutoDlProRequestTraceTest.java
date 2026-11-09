package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.StreamUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Synthetic credentials and loopback-only HTTP; never submit a live GPU or funding request. */
@Tag("dev")
class AutoDlProRequestTraceTest {
    static final String REQUEST_ID="983edb8c8c8553db1115d51fb2758ae8";
    static final String SECRET="TEST_ONLY_TOKEN_NEVER_LOG";
    static final String NO_STOCK_MESSAGE="当前算力规格暂无库存, 请修改配置或稍等再试";
    final ObjectMapper json=new ObjectMapper();

    ComputeNode node(){
        ComputeNode node=new ComputeNode();node.setId(1L);node.setProvider("autodl");node.setProviderProduct("AUTODL_PRO");
        node.setControlMode("API");node.setProviderInstanceId("pro-fixture");node.setProviderCredentialCipher("fixture-cipher");return node;
    }
    AutoDlProPowerProvider provider(RestTemplate http,String token) throws Exception {
        ComputeCredentialCipher cipher=mock(ComputeCredentialCipher.class);
        when(cipher.decrypt("fixture-cipher")).thenReturn(json.createObjectNode().put("token",token).toString());
        return new AutoDlProPowerProvider(cipher,true,http);
    }
    ComputeProviderException businessFailure(JsonNode code,JsonNode message,String token,String requestId) throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        com.fasterxml.jackson.databind.node.ObjectNode body=json.createObjectNode().put("request_id",requestId);
        if(code!=null)body.set("code",code);
        if(message!=null)body.set("msg",message);
        body.putObject("data").put("token",token);
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on")).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(body.toString(),MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,token);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
        assertNull(failure.getCause());assertFalse(json.writeValueAsString(failure).contains(token));mock.verify();return failure;
    }
    static Stream<String> recognizedCapacityMessages(){return Stream.of(NO_STOCK_MESSAGE,
        "当前算力规格暂无库存,请修改配置或稍等再试","当前算力规格暂无库存， 请修改配置或稍等再试",
        "当前算力规格暂无库存，请修改配置或稍等再试"," \t\r\n\u3000   "+NO_STOCK_MESSAGE+"   \u3000\n\r\t ");}
    @ParameterizedTest @MethodSource("recognizedCapacityMessages")
    void exactCapacityMessageWithBoundedWhitespaceIsTrustedWithoutLeakingMessage(String message) throws Exception {
        ComputeProviderException failure=businessFailure(json.getNodeFactory().textNode("UndocumentedSupplierError"),json.getNodeFactory().textNode(message),SECRET,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.NO_STOCK,failure.getKind());assertEquals(Integer.valueOf(200),failure.getHttpStatus());
        assertEquals("BUSINESS_OTHER",failure.getDiagnostic());assertEquals("UndocumentedSupplierError",failure.getProviderCode());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());assertFalse(json.writeValueAsString(failure).contains("当前算力规格"));
    }
    @Test void exactBadRequestCapacityMessageDoesNotDependOnAValidRequestId() throws Exception {
        ComputeProviderException failure=businessFailure(json.getNodeFactory().textNode("BadRequest"),json.getNodeFactory().textNode(NO_STOCK_MESSAGE),SECRET,"not-a-request-id");
        assertEquals(ComputeProviderException.Kind.NO_STOCK,failure.getKind());assertEquals("BUSINESS_BAD_REQUEST",failure.getDiagnostic());
        assertEquals("BadRequest",failure.getProviderCode());assertNull(failure.getProviderRequestId());
    }
    static Stream<JsonNode> unrecognizedCapacityMessages(){
        com.fasterxml.jackson.databind.node.JsonNodeFactory fields=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
        return Stream.concat(Stream.<JsonNode>of(fields.nullNode(),fields.numberNode(123),fields.objectNode(),fields.arrayNode(),fields.booleanNode(true)),
            Stream.of("","当前资源被占用","当前算力规格没有库存, 请修改配置或稍等再试","说明："+NO_STOCK_MESSAGE,
                NO_STOCK_MESSAGE+"。",NO_STOCK_MESSAGE+" 这是正常情况",NO_STOCK_MESSAGE+SECRET,
                "当前算力规格暂无库存 请修改配置或稍等再试","当前算力规格暂无库存,  请修改配置或稍等再试",
                "当前算力规格暂无库存,\t请修改配置或稍等再试","当前算力规格暂无库存,\n请修改配置或稍等再试",
                "         "+NO_STOCK_MESSAGE,NO_STOCK_MESSAGE+"         ","\u00a0"+NO_STOCK_MESSAGE,
                "当前算力规格暂无库存, 请修改配置或稍等\u200b再试").map(fields::textNode));
    }
    @ParameterizedTest @MethodSource("unrecognizedCapacityMessages")
    void approximateExtendedMalformedOrSecretBearingCapacityMessagesStayUnknown(JsonNode message) throws Exception {
        ComputeProviderException failure=businessFailure(json.getNodeFactory().textNode("BadRequest"),message,SECRET,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertEquals("BadRequest",failure.getProviderCode());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());assertFalse(json.writeValueAsString(failure).contains("当前算力规格"));
    }
    @ParameterizedTest @MethodSource("invalidProviderCodes")
    void exactCapacityMessageCannotTurnMalformedProviderCodeIntoTrustedRejection(String codeJson) throws Exception {
        ComputeProviderException failure=businessFailure(json.readTree(codeJson),json.getNodeFactory().textNode(NO_STOCK_MESSAGE),SECRET,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertNull(failure.getProviderCode());assertEquals(REQUEST_ID,failure.getProviderRequestId());
    }
    @Test void exactCapacityMessageWithoutProviderCodeStaysUnknown() throws Exception {
        ComputeProviderException failure=businessFailure(null,json.getNodeFactory().textNode(NO_STOCK_MESSAGE),SECRET,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertNull(failure.getProviderCode());
    }
    @ParameterizedTest @ValueSource(strings={"same","lowercase","prefix","suffix","contains","containsLowercase"})
    void exactCapacityMessageCannotTrustAProviderCodeThatEchoesTheCredential(String variant) throws Exception {
        String token="TEST_ONLY_TOKEN_NEVER_PERSIST",code=token;
        if("lowercase".equals(variant))code=token.toLowerCase(java.util.Locale.ROOT);
        if("prefix".equals(variant))code=token.substring(0,14);
        if("suffix".equals(variant))code=token.substring(10);
        if("contains".equals(variant))code="X"+token+"X";
        if("containsLowercase".equals(variant))code="X"+token.toLowerCase(java.util.Locale.ROOT)+"X";
        ComputeProviderException failure=businessFailure(json.getNodeFactory().textNode(code),json.getNodeFactory().textNode(NO_STOCK_MESSAGE),token,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertNull(failure.getProviderCode());assertEquals(REQUEST_ID,failure.getProviderRequestId());
        assertFalse(json.writeValueAsString(failure).contains(code));
    }
    @ParameterizedTest @ValueSource(strings={"same","credentialContainsMessage","messageContainsCredential","whitespaceCredentialContainsMessage"})
    void exactCapacityMessageThatEchoesAllOrPartOfCredentialStaysUnknown(String variant) throws Exception {
        String token=NO_STOCK_MESSAGE,message=NO_STOCK_MESSAGE;
        if("credentialContainsMessage".equals(variant)||"whitespaceCredentialContainsMessage".equals(variant))token="TEST_ONLY_"+NO_STOCK_MESSAGE+"_TOKEN";
        if("messageContainsCredential".equals(variant))token="当前算力规格暂无库存";
        if("whitespaceCredentialContainsMessage".equals(variant))message=" \t"+NO_STOCK_MESSAGE+"\u3000";
        ComputeProviderException failure=businessFailure(json.getNodeFactory().textNode("BadRequest"),json.getNodeFactory().textNode(message),token,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertEquals("BadRequest",failure.getProviderCode());assertEquals(REQUEST_ID,failure.getProviderRequestId());
    }
    @ParameterizedTest @ValueSource(strings={"power_off","status","list","snapshot"})
    void capacityTextFromOtherProviderEndpointsCannotBecomeTrustedStartRejection(String endpoint) throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        boolean get="status".equals(endpoint)||"snapshot".equals(endpoint);
        String body=json.createObjectNode().put("code","BadRequest").put("msg",NO_STOCK_MESSAGE).put("request_id",REQUEST_ID).toString();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+endpoint+(get?"?instance_uuid=pro-fixture":"")))
            .andExpect(method(get?HttpMethod.GET:HttpMethod.POST)).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,SECRET);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->{
            if("power_off".equals(endpoint))provider.submit(node(),"stop","original-op");
            else if("status".equals(endpoint))provider.status(node());
            else if("list".equals(endpoint))provider.inventoryPage(node(),1,20);
            else provider.cost(node());
        });
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertEquals("BadRequest",failure.getProviderCode());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());mock.verify();
    }
    @Test void successEnvelopeWithCapacityTextStillReturnsItsDataWithoutRejecting() throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        com.fasterxml.jackson.databind.node.ObjectNode body=json.createObjectNode().put("code","Success").put("msg",NO_STOCK_MESSAGE);
        JsonNode data=json.createObjectNode().put("fixture","TEST_ONLY_SUCCESS_DATA");body.set("data",data);
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"snapshot?instance_uuid=pro-fixture")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(body.toString(),MediaType.APPLICATION_JSON));
        assertEquals(data,provider(http,SECRET).connectionSnapshot(node()));mock.verify();
    }
    @Test void exactAdapterCapacityRejectionUsesExistingOriginalHoldReleaseOnce() throws Exception {
        ComputeProviderException rejection=businessFailure(json.getNodeFactory().textNode("BadRequest"),json.getNodeFactory().textNode(NO_STOCK_MESSAGE),SECRET,REQUEST_ID);
        assertEquals(ComputeProviderException.Kind.NO_STOCK,rejection.getKind());
        ConfiguredOfficialRunIntegrationTest fixture=new ConfiguredOfficialRunIntegrationTest();fixture.setup();
        try {
            String run=fixture.accepted();fixture.queued(run);
            String hold=fixture.jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run WHERE run_id=?",String.class,run);
            String operation=fixture.jdbc.queryForObject("SELECT start_operation_id FROM vls_compute_paid_run WHERE run_id=?",String.class,run);
            fixture.rejectStart(run,rejection);
            assertEquals("REJECTED_NO_STOCK",fixture.jdbc.queryForObject("SELECT power_start_state FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
            assertEquals("NO_STOCK",fixture.f.runs.get(run,fixture.f.payer).getStartFailureCode());
            assertEquals(0,fixture.f.wallet.releasePosts);fixture.assertOriginalClaimsHeld(run);
            fixture.registry.initialize();fixture.installPower();
            for(int i=0;i<4;i++)fixture.f.runs.tick(run);
            assertEquals("CANCELLED",fixture.f.runs.get(run,fixture.f.payer).getState());
            assertEquals("NO_STOCK",fixture.f.runs.get(run,fixture.f.payer).getStartFailureCode());
            assertEquals(1,fixture.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
            assertEquals(hold,fixture.jdbc.queryForObject("SELECT parent_intent_id FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",String.class));
            assertEquals(1,fixture.f.wallet.holdPosts);assertEquals(1,fixture.f.wallet.releasePosts);assertEquals(1,fixture.f.wallet.releaseEffects);
            assertEquals(0,fixture.f.wallet.capturePosts);assertEquals(0,fixture.f.wallet.remaining());
            for(int i=0;i<3;i++)fixture.f.runs.tick(run);
            assertEquals(1,fixture.f.wallet.releasePosts);assertEquals(1,fixture.f.wallet.releaseEffects);assertEquals(0,fixture.f.wallet.capturePosts);
            verify(fixture.f.supplier,times(1)).submit(any(),eq("start"),eq(operation));
            verify(fixture.f.supplier,never()).submit(any(),eq("stop"),anyString());
        } finally {fixture.cleanup();}
    }
    @ParameterizedTest @ValueSource(strings={"BadRequest","UndocumentedSupplierError","NoStock","NO_STOCK"})
    void boundedProviderCodeAndRequestIdSurviveWithoutClassifyingBusinessRejection(String code) throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        String body=json.createObjectNode().put("code",code).put("msg",SECRET).put("request_id",REQUEST_ID)
            .set("data",json.createObjectNode().put("root_password",SECRET).put("token",SECRET)).toString();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on")).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,SECRET);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertEquals(Integer.valueOf(200),failure.getHttpStatus());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());assertEquals(code,failure.getProviderCode());assertNull(failure.getCause());
        assertEquals("BadRequest".equals(code)?"BUSINESS_BAD_REQUEST":"BUSINESS_OTHER",failure.getDiagnostic());
        assertFalse(json.writeValueAsString(failure).contains(SECRET));assertFalse(failure.getMessage().contains(code));
        mock.verify();
    }
    static Stream<String> invalidProviderCodes(){return Stream.of("null","123","{}","[]","true","\"\"","\"1BadRequest\"","\"Bad.Request\"","\"Bad-Request\"","\"Bad Request\"","\"Bad\\nRequest\"","\"Bad\\rRequest\"","\"请求失败\"","\""+String.join("",Collections.nCopies(65,"a"))+"\"");}
    @ParameterizedTest @MethodSource("invalidProviderCodes")
    void nonTextualUnboundedOrNonIdentifierProviderCodesAreDiscarded(String codeJson) throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on"))
            .andRespond(withSuccess("{\"code\":"+codeJson+",\"msg\":\""+SECRET+"\",\"request_id\":\""+REQUEST_ID+"\"}",MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,SECRET);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertNull(failure.getProviderCode());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());assertFalse(json.writeValueAsString(failure).contains(SECRET));mock.verify();
    }
    @ParameterizedTest @ValueSource(strings={"same","lowercase","prefix","suffix","contains","containsLowercase"})
    void credentialOrItsPartialEchoCannotMasqueradeAsProviderCode(String variant) throws Exception {
        String token="TEST_ONLY_TOKEN_NEVER_PERSIST",code=token;
        if("lowercase".equals(variant))code=token.toLowerCase(java.util.Locale.ROOT);
        if("prefix".equals(variant))code=token.substring(0,14);
        if("suffix".equals(variant))code=token.substring(10);
        if("contains".equals(variant))code="X"+token+"X";
        if("containsLowercase".equals(variant))code="X"+token.toLowerCase(java.util.Locale.ROOT)+"X";
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on"))
            .andRespond(withSuccess(json.createObjectNode().put("code",code).put("request_id",REQUEST_ID).toString(),MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,token);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertNull(failure.getProviderCode());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());assertFalse(json.writeValueAsString(failure).contains(code));mock.verify();
    }
    @Test void legacyExceptionConstructorsKeepProviderCodeAbsent() {
        assertNull(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN).getProviderCode());
        assertNull(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,200,"BUSINESS_OTHER").getProviderCode());
        assertNull(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,200,"BUSINESS_OTHER",REQUEST_ID).getProviderCode());
    }
    static Stream<String> invalidRequestIds(){return Stream.of("null","123","{}","[]","\"\"","\"abc\"","\""+String.join("",Collections.nCopies(65,"a"))+"\"","\"https://fixture.invalid/token\"","\""+SECRET+"\"","\"983edb8c8c8553db1115d51fb2758ae8\\n\"");}
    @ParameterizedTest @MethodSource("invalidRequestIds")
    void malformedOrSecretBearingCorrelationFieldsAreDiscarded(String requestIdJson) throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on"))
            .andRespond(withSuccess("{\"code\":\"BadRequest\",\"msg\":\""+SECRET+"\",\"request_id\":"+requestIdJson+"}",MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,SECRET);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
        assertNull(failure.getProviderRequestId());assertFalse(json.writeValueAsString(failure).contains(SECRET));mock.verify();
    }
    @ParameterizedTest @ValueSource(strings={"same","uppercase","prefix","suffix","contains"})
    void hexadecimalCredentialOrItsSubstringsCannotMasqueradeAsRequestId(String variant) throws Exception {
        String token="0123456789abcdef0123456789abcdef",id=token;
        if("uppercase".equals(variant))id=token.toUpperCase(java.util.Locale.ROOT);
        if("prefix".equals(variant))id=token.substring(0,16);
        if("suffix".equals(variant))id=token.substring(16);
        if("contains".equals(variant))id="a"+token+"b";
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on"))
            .andRespond(withSuccess(json.createObjectNode().put("code","BadRequest").put("request_id",id).toString(),MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,token);
        assertNull(assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op")).getProviderRequestId());mock.verify();
    }
    @ParameterizedTest @ValueSource(ints={200,503})
    void actualApachePostUsesCanonicalJsonAndNeverRetriesRejection(int status) throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicInteger calls=new AtomicInteger();AtomicReference<String> method=new AtomicReference<>(),path=new AtomicReference<>(),body=new AtomicReference<>(),authorization=new AtomicReference<>(),contentType=new AtomicReference<>();
        server.createContext("/api/v1/dev/instance/pro/power_on",exchange->{
            calls.incrementAndGet();method.set(exchange.getRequestMethod());path.set(exchange.getRequestURI().toString());
            body.set(new String(StreamUtils.copyToByteArray(exchange.getRequestBody()),StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] reply=("{\"code\":\"UndocumentedSupplierError\",\"msg\":\""+SECRET+"\",\"request_id\":\""+REQUEST_ID+"\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,reply.length);exchange.getResponseBody().write(reply);exchange.close();
        });server.start();
        try{
            RestTemplate http=AutoDlProPowerProvider.transport();
            http.setInterceptors(Collections.singletonList((request,bytes,execution)->{
                assertEquals("api.autodl.com",request.getURI().getHost());
                return execution.execute(new HttpRequestWrapper(request){@Override public URI getURI(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+request.getURI().getRawPath());}},bytes);
            }));
            AutoDlProPowerProvider provider=provider(http,SECRET);
            ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
            assertEquals(1,calls.get());assertEquals("POST",method.get());assertEquals("/api/v1/dev/instance/pro/power_on",path.get());
            assertEquals(json.readTree("{\"instance_uuid\":\"pro-fixture\",\"payload\":\"gpu\"}"),json.readTree(body.get()));
            assertEquals(SECRET,authorization.get());assertTrue(contentType.get().startsWith("application/json"));
            assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertEquals(Integer.valueOf(status),failure.getHttpStatus());
            assertEquals(status==200?REQUEST_ID:null,failure.getProviderRequestId());assertEquals(status==200?"UndocumentedSupplierError":null,failure.getProviderCode());assertFalse(json.writeValueAsString(failure).contains(SECRET));
        }finally{server.stop(0);}
    }
    @Test void durablePowerOutboxLogsSafeCodeAndRequestIdWithoutChangingUnknownOrResending() throws Exception {
        ConfiguredOfficialRunIntegrationTest fixture=new ConfiguredOfficialRunIntegrationTest();fixture.setup();
        ch.qos.logback.classic.Logger logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(ConfiguredPaidPowerService.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender=new ch.qos.logback.core.read.ListAppender<>();appender.start();logger.addAppender(appender);
        try{
            doThrow(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,200,"BUSINESS_OTHER",REQUEST_ID,"UndocumentedSupplierError")).when(fixture.f.supplier).submit(any(),eq("start"),anyString());
            String run=fixture.accepted();fixture.queued(run);fixture.f.runs.tick(run);fixture.f.runs.tick(run);
            assertEquals("UNKNOWN",fixture.jdbc.queryForObject("SELECT power_start_state FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
            assertEquals(0,fixture.f.wallet.capturePosts);assertEquals(0,fixture.f.wallet.releasePosts);
            verify(fixture.f.supplier,times(1)).submit(any(),eq("start"),anyString());
            String log=appender.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).collect(java.util.stream.Collectors.joining("\n"));
            assertTrue(log.contains("providerRequestId="+REQUEST_ID));assertTrue(log.contains("providerCode=UndocumentedSupplierError"));assertTrue(log.contains("kind=UNKNOWN"));assertFalse(log.contains(SECRET));
        }finally{logger.detachAppender(appender);appender.stop();fixture.cleanup();}
    }
}
