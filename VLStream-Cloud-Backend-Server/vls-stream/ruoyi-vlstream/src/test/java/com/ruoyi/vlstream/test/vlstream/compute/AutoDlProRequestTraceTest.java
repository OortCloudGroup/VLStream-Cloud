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
    static final String SECRET="SYNTHETIC_AUTHORIZATION_NEVER_LOG";
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
    @ParameterizedTest @ValueSource(strings={"BadRequest","UndocumentedSupplierError"})
    void onlyDocumentedSafeRequestIdSurvivesBusinessRejection(String code) throws Exception {
        RestTemplate http=AutoDlProPowerProvider.transport();MockRestServiceServer mock=MockRestServiceServer.bindTo(http).build();
        String body=json.createObjectNode().put("code",code).put("msg",SECRET).put("request_id",REQUEST_ID)
            .set("data",json.createObjectNode().put("root_password",SECRET).put("token",SECRET)).toString();
        mock.expect(requestTo(AutoDlProPowerProvider.BASE+"power_on")).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
        AutoDlProPowerProvider provider=provider(http,SECRET);
        ComputeProviderException failure=assertThrows(ComputeProviderException.class,()->provider.submit(node(),"start","original-op"));
        assertEquals(ComputeProviderException.Kind.UNKNOWN,failure.getKind());assertEquals(Integer.valueOf(200),failure.getHttpStatus());
        assertEquals(REQUEST_ID,failure.getProviderRequestId());assertNull(failure.getCause());
        assertFalse(json.writeValueAsString(failure).contains(SECRET));assertFalse(failure.getMessage().contains(code));
        mock.verify();
    }
    static Stream<String> invalidRequestIds(){return Stream.of("null","123","{}","[]","\"\"","\"abc\"","\""+String.join("",Collections.nCopies(65,"a"))+"\"","\"https://fixture.invalid/token\"","\"SYNTHETIC_AUTHORIZATION_NEVER_LOG\"","\"983edb8c8c8553db1115d51fb2758ae8\\n\"");}
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
            assertEquals(status==200?REQUEST_ID:null,failure.getProviderRequestId());assertFalse(json.writeValueAsString(failure).contains(SECRET));
        }finally{server.stop(0);}
    }
    @Test void durablePowerOutboxLogsRequestIdWithoutChangingUnknownOrResending() throws Exception {
        ConfiguredOfficialRunIntegrationTest fixture=new ConfiguredOfficialRunIntegrationTest();fixture.setup();
        ch.qos.logback.classic.Logger logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(ConfiguredPaidPowerService.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender=new ch.qos.logback.core.read.ListAppender<>();appender.start();logger.addAppender(appender);
        try{
            doThrow(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN,200,"BUSINESS_OTHER",REQUEST_ID)).when(fixture.f.supplier).submit(any(),eq("start"),anyString());
            String run=fixture.accepted();fixture.queued(run);fixture.f.runs.tick(run);fixture.f.runs.tick(run);
            assertEquals("UNKNOWN",fixture.jdbc.queryForObject("SELECT power_start_state FROM vls_compute_paid_run WHERE run_id=?",String.class,run));
            assertEquals(0,fixture.f.wallet.capturePosts);assertEquals(0,fixture.f.wallet.releasePosts);
            verify(fixture.f.supplier,times(1)).submit(any(),eq("start"),anyString());
            String log=appender.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).collect(java.util.stream.Collectors.joining("\n"));
            assertTrue(log.contains("providerRequestId="+REQUEST_ID));assertTrue(log.contains("kind=UNKNOWN"));assertFalse(log.contains(SECRET));
        }finally{logger.detachAppender(appender);appender.stop();fixture.cleanup();}
    }
}
